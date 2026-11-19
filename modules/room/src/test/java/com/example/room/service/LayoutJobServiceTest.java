package com.example.room.service;

import com.example.image.dto.response.ImageUploadResponse;
import com.example.image.service.ImageService;
import com.example.room.dto.request.LayoutJobCreateRequest;
import com.example.room.dto.response.LayoutJobResponse;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutBusyException;
import com.example.room.global.exception.LayoutJobNotFoundException;
import com.example.room.global.exception.LayoutRejectedException;
import com.example.room.global.exception.LayoutUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LayoutJobServiceTest {

    private static final String USER_ID = "host-1";
    private static final String PANO_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/pano.jpg";
    private static final String ALIGNED_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/aligned.jpg";

    @Mock
    private LayoutJobStore jobStore;
    @Mock
    private LayoutClient layoutClient;
    @Mock
    private ImageService imageService;
    @Mock
    private ExecutorService executor;

    private LayoutJobService service;

    @BeforeEach
    void setUp() {
        service = new LayoutJobService(jobStore, layoutClient, imageService, executor);
    }

    private static LayoutJobCreateRequest request() {
        return new LayoutJobCreateRequest(PANO_URL, null, null);
    }

    @Test
    void 작업을_만들면_QUEUED로_저장하고_실행을_예약한다() {
        when(imageService.isOwnImageUrl(PANO_URL)).thenReturn(true);

        LayoutJobResponse response = service.create(USER_ID, request());

        assertThat(response.status()).isEqualTo(LayoutJob.Status.QUEUED);
        assertThat(response.jobId()).isNotBlank();
        verify(jobStore).save(any(LayoutJob.class));
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void 외부_URL로는_작업을_만들지_않는다() {
        when(imageService.isOwnImageUrl(PANO_URL)).thenReturn(false);

        assertThatThrownBy(() -> service.create(USER_ID, request())).isInstanceOf(InvalidRoomException.class);
        verify(executor, never()).execute(any());
    }

    @Test
    void 대기열이_가득_차면_503_예외와_함께_작업을_실패로_남긴다() {
        when(imageService.isOwnImageUrl(PANO_URL)).thenReturn(true);
        doThrow(new RejectedExecutionException()).when(executor).execute(any());

        assertThatThrownBy(() -> service.create(USER_ID, request())).isInstanceOf(LayoutBusyException.class);

        ArgumentCaptor<LayoutJob> captor = ArgumentCaptor.forClass(LayoutJob.class);
        verify(jobStore, times(2)).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(LayoutJob.Status.FAILED);
    }

    @Test
    void 추론에_성공하면_정렬된_사진을_올리고_결과를_저장한다() {
        byte[] pano = {1, 2, 3};
        byte[] aligned = {9, 8, 7};
        List<List<Double>> uv = List.of(List.of(0.1, 0.3), List.of(0.1, 0.7));
        when(imageService.downloadOwnImage(PANO_URL)).thenReturn(pano);
        when(layoutClient.infer(pano, true, false)).thenReturn(
                new LayoutClient.InferResult(Base64.getEncoder().encodeToString(aligned), uv, 50.0, -58.4, true));
        when(imageService.uploadGeneratedJpeg(aligned)).thenReturn(new ImageUploadResponse(ALIGNED_URL));

        service.run(LayoutJob.queued("job-1", USER_ID, 0L), request());

        LayoutJob last = lastSaved(2);
        assertThat(last.status()).isEqualTo(LayoutJob.Status.SUCCEEDED);
        assertThat(last.result().imageUrl()).isEqualTo(ALIGNED_URL);
        assertThat(last.result().uv()).isEqualTo(uv);
        assertThat(last.result().z1()).isEqualTo(-58.4);
    }

    @Test
    void 추론_서비스가_거절하면_그_메시지로_실패한다() {
        when(imageService.downloadOwnImage(PANO_URL)).thenReturn(new byte[]{1});
        when(layoutClient.infer(any(), any(Boolean.class), any(Boolean.class)))
                .thenThrow(new LayoutRejectedException("가로:세로 2:1인 360° 사진만 받아요"));

        service.run(LayoutJob.queued("job-1", USER_ID, 0L), request());

        LayoutJob last = lastSaved(2);
        assertThat(last.status()).isEqualTo(LayoutJob.Status.FAILED);
        assertThat(last.error()).isEqualTo("가로:세로 2:1인 360° 사진만 받아요");
    }

    @Test
    void 추론_서비스가_죽어_있으면_일반_메시지로_실패한다() {
        when(imageService.downloadOwnImage(PANO_URL)).thenReturn(new byte[]{1});
        when(layoutClient.infer(any(), any(Boolean.class), any(Boolean.class)))
                .thenThrow(new LayoutUnavailableException("connection refused"));

        service.run(LayoutJob.queued("job-1", USER_ID, 0L), request());

        LayoutJob last = lastSaved(2);
        assertThat(last.status()).isEqualTo(LayoutJob.Status.FAILED);
        assertThat(last.error()).doesNotContain("connection refused");
    }

    @Test
    void 다른_사람의_작업은_조회할_수_없다() {
        when(jobStore.find("job-1")).thenReturn(Optional.of(LayoutJob.queued("job-1", "someone-else", 0L)));

        assertThatThrownBy(() -> service.get(USER_ID, "job-1")).isInstanceOf(LayoutJobNotFoundException.class);
    }

    @Test
    void 내_작업은_조회된다() {
        when(jobStore.find("job-1")).thenReturn(Optional.of(LayoutJob.queued("job-1", USER_ID, 0L).running()));

        assertThat(service.get(USER_ID, "job-1").status()).isEqualTo(LayoutJob.Status.RUNNING);
    }

    private LayoutJob lastSaved(int expectedSaves) {
        ArgumentCaptor<LayoutJob> captor = ArgumentCaptor.forClass(LayoutJob.class);
        verify(jobStore, times(expectedSaves)).save(captor.capture());
        return captor.getValue();
    }
}
