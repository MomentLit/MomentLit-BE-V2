package com.example.room.service;

import com.example.image.service.ImageService;
import com.example.room.dto.request.PlanDetectRequest;
import com.example.room.dto.response.PlanDetectResponse;
import com.example.room.global.client.LayoutClient;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.LayoutRejectedException;
import com.example.room.global.exception.LayoutUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanDetectServiceTest {

    private static final String PLAN_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/plan.png";
    private static final byte[] IMAGE = {1, 2, 3};

    @Mock
    private LayoutClient layoutClient;
    @Mock
    private ImageService imageService;

    private PlanDetectService service;

    @BeforeEach
    void setUp() {
        service = new PlanDetectService(layoutClient, imageService);
    }

    @Test
    void 우리_서비스에_올린_도면이_아니면_거절() {
        when(imageService.isOwnImageUrl(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.detect(new PlanDetectRequest("https://evil.example/plan.png", null)))
                .isInstanceOf(InvalidRoomException.class);
        verify(layoutClient, never()).detectPlan(any(), any());
    }

    @Test
    void 축척을_그대로_넘기고_인식_결과를_돌려준다() {
        PlanDetectResponse found = new PlanDetectResponse(true, null, 1000, 800,
                List.of(List.of(100.0, 100.0), List.of(900.0, 100.0), List.of(900.0, 700.0)),
                List.of(), new PlanDetectResponse.WallPx(12, 20));
        when(imageService.isOwnImageUrl(PLAN_URL)).thenReturn(true);
        when(imageService.downloadOwnImage(PLAN_URL)).thenReturn(IMAGE);
        when(layoutClient.detectPlan(IMAGE, 0.01)).thenReturn(found);

        assertThat(service.detect(new PlanDetectRequest(PLAN_URL, 0.01))).isSameAs(found);
    }

    @Test
    void 읽을_수_없는_이미지는_못_찾음으로_돌려준다() {
        when(imageService.isOwnImageUrl(PLAN_URL)).thenReturn(true);
        when(imageService.downloadOwnImage(PLAN_URL)).thenReturn(IMAGE);
        when(layoutClient.detectPlan(IMAGE, null)).thenThrow(new LayoutRejectedException("이미지를 읽을 수 없어요"));

        PlanDetectResponse response = service.detect(new PlanDetectRequest(PLAN_URL, null));

        assertThat(response.ok()).isFalse();
        assertThat(response.reason()).isEqualTo("이미지를 읽을 수 없어요");
    }

    @Test
    void 인식_서비스가_죽어_있으면_그대로_올린다() {
        when(imageService.isOwnImageUrl(PLAN_URL)).thenReturn(true);
        when(imageService.downloadOwnImage(PLAN_URL)).thenReturn(IMAGE);
        when(layoutClient.detectPlan(IMAGE, null)).thenThrow(new LayoutUnavailableException("down"));

        assertThatThrownBy(() -> service.detect(new PlanDetectRequest(PLAN_URL, null)))
                .isInstanceOf(LayoutUnavailableException.class);
    }
}
