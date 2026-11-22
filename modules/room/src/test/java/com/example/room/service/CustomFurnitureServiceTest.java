package com.example.room.service;

import com.example.image.dto.response.ImageUploadResponse;
import com.example.image.service.ImageService;
import com.example.room.dto.request.CustomFurnitureFileRequest;
import com.example.room.dto.request.CustomFurniturePhotoRequest;
import com.example.room.dto.request.CustomFurnitureUpdateRequest;
import com.example.room.dto.response.CustomFurnitureResponse;
import com.example.room.entity.CustomFurniture;
import com.example.room.entity.CustomFurnitureMount;
import com.example.room.entity.CustomFurnitureSource;
import com.example.room.entity.CustomFurnitureStatus;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.client.LayoutClient.FurnitureResult;
import com.example.room.global.exception.CustomFurnitureNotFoundException;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutBusyException;
import com.example.room.global.exception.LayoutRejectedException;
import com.example.room.global.exception.LayoutUnavailableException;
import com.example.room.repository.CustomFurnitureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomFurnitureServiceTest {

    private static final String USER_ID = "guest-1";
    private static final String MODEL_URL = "furniture/11111111-1111-4111-8111-111111111111.glb";
    private static final String THUMB_URL = "furniture/22222222-2222-4222-8222-222222222222.png";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};

    @Mock
    private CustomFurnitureRepository repository;
    @Mock
    private LayoutClient layoutClient;
    @Mock
    private ImageService imageService;
    @Mock
    private ExecutorService executor;

    private CustomFurnitureService service;

    @BeforeEach
    void setUp() {
        service = new CustomFurnitureService(repository, layoutClient, imageService, executor);
    }

    private static CustomFurniturePhotoRequest photoRequest() {
        return new CustomFurniturePhotoRequest("우리 집 의자", CustomFurnitureMount.FLOOR, 0.9, null, null);
    }

    private static MockMultipartFile photo(String part) {
        return new MockMultipartFile(part, "chair.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    /** 최소 GLB: 12바이트 머리글 + 8바이트(길이 0 JSON 청크 흉내). 전체 길이를 머리글에 맞춘다. */
    static byte[] glb(int version, int declaredLength) {
        return ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x46546C67).putInt(version).putInt(declaredLength).putInt(0).putInt(0x4E4F534A).array();
    }

    private void givenSaveAssignsId(long id) {
        when(repository.save(any(CustomFurniture.class))).thenAnswer(inv -> {
            CustomFurniture furniture = inv.getArgument(0);
            if (furniture.getId() == null) ReflectionTestUtils.setField(furniture, "id", id);
            return furniture;
        });
    }

    private Runnable capturedJob() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(captor.capture());
        return captor.getValue();
    }

    @Test
    void 사진으로_만들면_PROCESSING으로_저장하고_작업을_예약한다() {
        givenSaveAssignsId(7L);

        CustomFurnitureResponse response = service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null);

        assertThat(response.status()).isEqualTo(CustomFurnitureStatus.PROCESSING);
        assertThat(response.type()).isEqualTo("u-7");
        assertThat(response.source()).isEqualTo(CustomFurnitureSource.PHOTO);
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void 작업이_끝나면_모델과_썸네일을_올리고_READY로_바꾼다() {
        givenSaveAssignsId(7L);
        service.createFromPhoto(USER_ID, photoRequest(), photo("front"), photo("side"));
        CustomFurniture saved = CustomFurniture.processing(USER_ID, "우리 집 의자", CustomFurnitureMount.FLOOR, 0.9, null, null);
        when(repository.findById(7L)).thenReturn(Optional.of(saved));
        String b64 = Base64.getEncoder().encodeToString(new byte[]{9});
        when(layoutClient.generateFurniture(any(), any(), eq(0.9), eq(null), eq(null)))
                .thenReturn(new FurnitureResult(b64, b64, 0.82, 0.61, 0.9));
        when(imageService.uploadModel(any())).thenReturn(new ImageUploadResponse(MODEL_URL));
        when(imageService.uploadGeneratedPng(any())).thenReturn(new ImageUploadResponse(THUMB_URL));

        capturedJob().run();

        assertThat(saved.getStatus()).isEqualTo(CustomFurnitureStatus.READY);
        assertThat(saved.getModelUrl()).isEqualTo(MODEL_URL);
        assertThat(saved.getThumbnailUrl()).isEqualTo(THUMB_URL);
        assertThat(saved.getWidth()).isEqualTo(0.82);
        assertThat(saved.getDepth()).isEqualTo(0.61);
    }

    @Test
    void 가구를_못_찾으면_그_사유로_FAILED() {
        givenSaveAssignsId(7L);
        service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null);
        CustomFurniture saved = CustomFurniture.processing(USER_ID, "의자", CustomFurnitureMount.FLOOR, 0.9, null, null);
        when(repository.findById(7L)).thenReturn(Optional.of(saved));
        when(layoutClient.generateFurniture(any(), any(), anyDouble(), any(), any()))
                .thenThrow(new LayoutRejectedException("앞 사진에서 가구를 찾지 못했어요."));

        capturedJob().run();

        assertThat(saved.getStatus()).isEqualTo(CustomFurnitureStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("앞 사진에서 가구를 찾지 못했어요.");
    }

    @Test
    void 서비스_장애는_일반_문구로_FAILED() {
        givenSaveAssignsId(7L);
        service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null);
        CustomFurniture saved = CustomFurniture.processing(USER_ID, "의자", CustomFurnitureMount.FLOOR, 0.9, null, null);
        when(repository.findById(7L)).thenReturn(Optional.of(saved));
        when(layoutClient.generateFurniture(any(), any(), anyDouble(), any(), any()))
                .thenThrow(new LayoutUnavailableException("connection refused"));

        capturedJob().run();

        assertThat(saved.getStatus()).isEqualTo(CustomFurnitureStatus.FAILED);
        assertThat(saved.getFailureReason()).doesNotContain("connection");
    }

    @Test
    void 생성_중에_지웠으면_올린_파일도_지운다() {
        givenSaveAssignsId(7L);
        service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null);
        when(repository.findById(7L)).thenReturn(Optional.empty());
        String b64 = Base64.getEncoder().encodeToString(new byte[]{9});
        when(layoutClient.generateFurniture(any(), any(), anyDouble(), any(), any())).thenReturn(new FurnitureResult(b64, b64, 1, 1, 1));
        when(imageService.uploadModel(any())).thenReturn(new ImageUploadResponse(MODEL_URL));
        when(imageService.uploadGeneratedPng(any())).thenReturn(new ImageUploadResponse(THUMB_URL));

        capturedJob().run();

        verify(imageService).deleteOwnFile(MODEL_URL);
        verify(imageService).deleteOwnFile(THUMB_URL);
    }

    @Test
    void 대기열이_가득하면_FAILED로_남기고_Busy() {
        givenSaveAssignsId(7L);
        doThrow(new RejectedExecutionException()).when(executor).execute(any());

        assertThatThrownBy(() -> service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null))
                .isInstanceOf(LayoutBusyException.class);
    }

    @Test
    void 개수와_하루_사진_생성_횟수를_넘으면_막는다() {
        when(repository.countByUserId(USER_ID)).thenReturn(20L);
        assertThatThrownBy(() -> service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null))
                .isInstanceOf(InvalidRoomException.class).hasMessageContaining("20개");

        when(repository.countByUserId(USER_ID)).thenReturn(3L);
        when(repository.countByUserIdAndSourceAndCreatedAtAfter(eq(USER_ID), eq(CustomFurnitureSource.PHOTO), any())).thenReturn(5L);
        assertThatThrownBy(() -> service.createFromPhoto(USER_ID, photoRequest(), photo("front"), null))
                .isInstanceOf(InvalidRoomException.class).hasMessageContaining("하루");
        verify(executor, never()).execute(any());
    }

    @Test
    void 사진이_아니면_막는다() {
        MockMultipartFile pdf = new MockMultipartFile("front", "a.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> service.createFromPhoto(USER_ID, photoRequest(), pdf, null)).isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void GLB_파일을_올리면_바로_READY() {
        givenSaveAssignsId(9L);
        when(imageService.uploadModel(any())).thenReturn(new ImageUploadResponse(MODEL_URL));
        when(imageService.uploadGeneratedPng(any())).thenReturn(new ImageUploadResponse(THUMB_URL));
        var request = new CustomFurnitureFileRequest("스캔한 소파", CustomFurnitureMount.FLOOR, 1.8, 0.9, 0.8);

        CustomFurnitureResponse response = service.createFromFile(USER_ID, request,
                new MockMultipartFile("model", "sofa.glb", "model/gltf-binary", glb(2, 20)),
                new MockMultipartFile("thumbnail", "t.png", "image/png", PNG));

        assertThat(response.status()).isEqualTo(CustomFurnitureStatus.READY);
        assertThat(response.source()).isEqualTo(CustomFurnitureSource.FILE);
        assertThat(response.type()).isEqualTo("u-9");
        assertThat(response.modelUrl()).isEqualTo("/rooms/furniture/9/model");
        assertThat(response.thumbnailUrl()).isEqualTo("/rooms/furniture/9/thumbnail");
    }

    @Test
    void GLB가_아니거나_깨졌으면_막는다() {
        assertThatThrownBy(() -> CustomFurnitureService.validateGlb("not a glb file at all".getBytes())).isInstanceOf(InvalidRoomException.class);
        assertThatThrownBy(() -> CustomFurnitureService.validateGlb(glb(1, 20))).hasMessageContaining("2.0");
        assertThatThrownBy(() -> CustomFurnitureService.validateGlb(glb(2, 999))).hasMessageContaining("깨졌");
        CustomFurnitureService.validateGlb(glb(2, 20));
    }

    @Test
    void 썸네일이_PNG가_아니면_막는다() {
        var request = new CustomFurnitureFileRequest("소파", CustomFurnitureMount.FLOOR, 1.0, 1.0, 1.0);
        assertThatThrownBy(() -> service.createFromFile(USER_ID, request,
                new MockMultipartFile("model", "s.glb", "model/gltf-binary", glb(2, 20)),
                new MockMultipartFile("thumbnail", "t.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2, 3, 4, 5, 6, 7})))
                .isInstanceOf(InvalidRoomException.class);
        verify(imageService, never()).uploadModel(any());
    }

    @Test
    void 목록을_볼_때_오래된_PROCESSING은_FAILED로() {
        CustomFurniture old = CustomFurniture.processing(USER_ID, "의자", CustomFurnitureMount.FLOOR, 0.9, null, null);
        ReflectionTestUtils.setField(old, "id", 1L);
        ReflectionTestUtils.setField(old, "createdAt", LocalDateTime.now().minusMinutes(30));
        CustomFurniture fresh = CustomFurniture.processing(USER_ID, "탁자", CustomFurnitureMount.FLOOR, 0.7, null, null);
        ReflectionTestUtils.setField(fresh, "id", 2L);
        ReflectionTestUtils.setField(fresh, "createdAt", LocalDateTime.now().minusMinutes(1));
        when(repository.findAllByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(fresh, old));

        List<CustomFurnitureResponse> mine = service.getMine(USER_ID);

        assertThat(mine).extracting(CustomFurnitureResponse::status)
                .containsExactly(CustomFurnitureStatus.PROCESSING, CustomFurnitureStatus.FAILED);
    }

    @Test
    void 남의_가구는_보거나_고치거나_지울_수_없다() {
        when(repository.findByIdAndUserId(5L, "someone-else")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("someone-else", 5L)).isInstanceOf(CustomFurnitureNotFoundException.class);
        assertThatThrownBy(() -> service.update("someone-else", 5L, new CustomFurnitureUpdateRequest("x", null)))
                .isInstanceOf(CustomFurnitureNotFoundException.class);
        assertThatThrownBy(() -> service.delete("someone-else", 5L)).isInstanceOf(CustomFurnitureNotFoundException.class);
        assertThatThrownBy(() -> service.downloadAsset("someone-else", 5L, false)).isInstanceOf(CustomFurnitureNotFoundException.class);
        assertThatThrownBy(() -> service.downloadAsset("someone-else", 5L, true)).isInstanceOf(CustomFurnitureNotFoundException.class);
        verify(repository, never()).delete(any());
        verify(imageService, never()).downloadFurnitureFile(any());
    }

    @Test
    void 소유자는_모델과_썸네일을_받는다() {
        CustomFurniture furniture = CustomFurniture.uploaded(USER_ID, "소파", CustomFurnitureMount.FLOOR, 1, 1, 1, MODEL_URL, THUMB_URL);
        byte[] model = glb(2, 20);
        when(repository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(furniture));
        when(imageService.downloadFurnitureFile(MODEL_URL)).thenReturn(model);
        when(imageService.downloadFurnitureFile(THUMB_URL)).thenReturn(PNG);

        assertThat(service.downloadAsset(USER_ID, 3L, false)).isEqualTo(model);
        assertThat(service.downloadAsset(USER_ID, 3L, true)).isEqualTo(PNG);
    }

    @Test
    void 지우면_S3_파일도_지운다() {
        CustomFurniture furniture = CustomFurniture.uploaded(USER_ID, "소파", CustomFurnitureMount.FLOOR, 1, 1, 1, MODEL_URL, THUMB_URL);
        when(repository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(furniture));

        service.delete(USER_ID, 3L);

        verify(repository).delete(furniture);
        verify(imageService).deleteOwnFile(MODEL_URL);
        verify(imageService).deleteOwnFile(THUMB_URL);
    }

    @Test
    void 이름과_놓는_위치를_바꾼다() {
        CustomFurniture furniture = CustomFurniture.uploaded(USER_ID, "소파", CustomFurnitureMount.FLOOR, 1, 1, 1, MODEL_URL, THUMB_URL);
        when(repository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(furniture));

        CustomFurnitureResponse response = service.update(USER_ID, 3L, new CustomFurnitureUpdateRequest(" 거실 소파 ", CustomFurnitureMount.WALL));

        assertThat(response.name()).isEqualTo("거실 소파");
        assertThat(response.mount()).isEqualTo(CustomFurnitureMount.WALL);
    }
}
