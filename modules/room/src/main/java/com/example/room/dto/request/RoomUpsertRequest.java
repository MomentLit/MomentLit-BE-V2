package com.example.room.dto.request;

import com.example.room.entity.RoomSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 호스트가 만든 3D 방 저장.
 *
 * @param sceneId      이 방을 만든 360도 장면 (파노라마 방). 그 공간의 장면이어야 한다
 * @param imageUrl     벽·바닥에 입힐 사진 — 파노라마 방은 추론 작업이 돌려준 "수평을 맞춘" 사진 URL
 * @param cameraHeight 사진을 찍은 높이(m). 파노라마 방은 필수
 */
public record RoomUpsertRequest(
        @JsonProperty("scene_id")
        Long sceneId,

        @NotNull(message = "방을 만든 방법(source)은 필수입니다.")
        RoomSource source,

        @JsonProperty("image_url")
        String imageUrl,

        @NotNull(message = "천장 높이는 필수입니다.")
        @DecimalMin(value = "1.8", message = "천장 높이는 1.8m 이상이어야 합니다.")
        @DecimalMax(value = "10.0", message = "천장 높이는 10m 이하여야 합니다.")
        @JsonProperty("ceiling_height")
        Double ceilingHeight,

        @DecimalMin(value = "0.3", message = "촬영 높이는 0.3m 이상이어야 합니다.")
        @DecimalMax(value = "3.0", message = "촬영 높이는 3m 이하여야 합니다.")
        @JsonProperty("camera_height")
        Double cameraHeight,

        @NotNull(message = "방 구조(geometry)는 필수입니다.")
        @Valid
        RoomGeometry geometry
) {
}
