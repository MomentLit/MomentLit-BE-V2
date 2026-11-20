package com.example.room.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 도면에서 찾은 벽. 좌표는 모두 원본 이미지 픽셀 [x, y]. 못 찾았으면 ok=false와 사용자에게 보여 줄 reason.
 *
 * @param outer 바깥벽 중심선 다각형(수평·수직으로 정리됨)
 * @param inner 안쪽 벽 선분(문 틈은 끊긴 채로)
 * 인식 서비스의 나머지 필드(scale, ocr_numbers)는 쓰지 않는다(축척은 사용자가 맞춘다).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanDetectResponse(
        boolean ok,
        String reason,
        Integer width,
        Integer height,
        List<List<Double>> outer,
        List<List<List<Double>>> inner,
        @JsonProperty("wall_px")
        WallPx wallPx
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WallPx(double interior, double exterior) {
    }

    public static PlanDetectResponse failed(String reason) {
        return new PlanDetectResponse(false, reason, null, null, List.of(), List.of(), null);
    }
}
