package com.example.room.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * 평면 도면에서 벽 찾기 요청.
 *
 * @param imageUrl       이 서비스에 올린 도면 이미지 URL
 * @param metersPerPixel 사용자가 맞춘 축척(원본 1px = 몇 m). 주면 문 틈·최소 벽 길이를 실제 크기로 잡는다. 없으면 이미지 크기로 짐작
 */
public record PlanDetectRequest(
        @NotBlank(message = "도면 이미지 URL은 필수입니다.")
        @JsonProperty("image_url")
        String imageUrl,

        @DecimalMin(value = "0.0001", message = "축척이 너무 작습니다.")
        @DecimalMax(value = "1", message = "축척이 너무 큽니다.")
        @JsonProperty("meters_per_pixel")
        Double metersPerPixel
) {
}
