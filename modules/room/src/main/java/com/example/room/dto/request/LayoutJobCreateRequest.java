package com.example.room.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 360도 사진에서 방 모서리 찾기 요청.
 *
 * @param imageUrl 이 서비스에 올린 2:1 파노라마 URL (합성/업로드 API가 돌려준 값)
 * @param align    소실점으로 사진 수평 맞추기. null이면 true
 * @param cuboid   벽 4개짜리 직육면체 방으로 강제. null이면 false
 */
public record LayoutJobCreateRequest(
        @NotBlank(message = "360도 사진 URL은 필수입니다.")
        @JsonProperty("image_url")
        String imageUrl,

        Boolean align,

        Boolean cuboid
) {
    public boolean alignOrDefault() {
        return align == null || align;
    }

    public boolean cuboidOrDefault() {
        return cuboid != null && cuboid;
    }
}
