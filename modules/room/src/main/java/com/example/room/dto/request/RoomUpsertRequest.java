package com.example.room.dto.request;

import com.example.room.entity.RoomSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 호스트가 만든 3D 방 저장(만들기·덮어쓰기 공통).
 *
 * @param name          방 이름 — 보통 360도 장면 이름(거실, 침실 …)
 * @param sortOrder     방 순서 — 보통 장면 순서. 3D 보기에서 이 순서로 탭이 놓인다
 * @param sceneImageUrl 이 방을 만든 360도 장면의 사진 URL(장면 id는 공간 수정 때 바뀔 수 있어 사진으로 기억한다)
 * @param imageUrl      벽·바닥에 입힐 사진 — 파노라마 방은 추론 작업이 돌려준 "수평을 맞춘" 사진 URL
 * @param cameraHeight  사진을 찍은 높이(m). 파노라마 방은 필수
 */
public record RoomUpsertRequest(
        @NotBlank(message = "방 이름을 입력해 주세요.")
        @Size(max = 100, message = "방 이름은 100자 이하로 입력해 주세요.")
        String name,

        @Min(value = 0, message = "방 순서는 0 이상이어야 합니다.")
        @JsonProperty("sort_order")
        Integer sortOrder,

        @JsonProperty("scene_image_url")
        String sceneImageUrl,

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
    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }
}
