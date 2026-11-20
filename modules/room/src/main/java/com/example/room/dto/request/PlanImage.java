package com.example.room.dto.request;

import com.example.room.global.exception.InvalidRoomException;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 도면 방의 도면 이미지 놓는 자리 — 이미지 픽셀을 바닥 평면(m)에 맞춘다. 바닥에 도면을 깔고, 다시 편집할 때 쓴다.
 *
 * @param metersPerPixel 축척: 이미지 1px = 몇 m
 * @param offset         이미지 왼쪽 위 모서리의 바닥 좌표 [x, z] (m)
 * @param widthPx        이미지 가로(px)
 * @param heightPx       이미지 세로(px)
 * @param showFloor      3D 바닥에 도면을 깔지
 */
public record PlanImage(
        @JsonProperty("meters_per_pixel")
        Double metersPerPixel,

        List<Double> offset,

        @JsonProperty("width_px")
        Integer widthPx,

        @JsonProperty("height_px")
        Integer heightPx,

        @JsonProperty("show_floor")
        Boolean showFloor
) {

    static final int MAX_IMAGE_PX = 20_000;
    /** 1px이 1m를 넘거나 0.1mm보다 작으면 축척을 잘못 맞춘 것으로 본다. */
    static final double MIN_METERS_PER_PIXEL = 0.0001;
    static final double MAX_METERS_PER_PIXEL = 1.0;
    static final double MAX_OFFSET = 1_000.0;

    public void validate() {
        if (metersPerPixel == null || !(metersPerPixel >= MIN_METERS_PER_PIXEL && metersPerPixel <= MAX_METERS_PER_PIXEL)) {
            throw new InvalidRoomException("도면 축척이 올바르지 않습니다. 기준 길이를 다시 맞춰 주세요.");
        }
        if (widthPx == null || heightPx == null || widthPx < 1 || heightPx < 1 || widthPx > MAX_IMAGE_PX || heightPx > MAX_IMAGE_PX) {
            throw new InvalidRoomException("도면 이미지 크기가 올바르지 않습니다.");
        }
        if (offset == null || offset.size() != 2
                || offset.stream().anyMatch(v -> v == null || !Double.isFinite(v) || Math.abs(v) > MAX_OFFSET)) {
            throw new InvalidRoomException("도면 위치가 올바르지 않습니다.");
        }
    }
}
