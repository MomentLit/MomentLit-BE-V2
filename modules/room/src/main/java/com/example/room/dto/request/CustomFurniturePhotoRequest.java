package com.example.room.dto.request;

import com.example.room.entity.CustomFurnitureMount;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사진으로 내 가구 만들기(multipart 폼 필드). 사진은 front(정면, 필수)·side(옆, 선택) 파트.
 *
 * @param height 사용자가 잰 실제 높이(m) — AI 모델은 크기 단위가 제각각이라 꼭 받는다
 * @param width  실제 폭(m, 선택). 주면 그대로 맞춘다
 * @param depth  실제 깊이(m, 선택)
 */
public record CustomFurniturePhotoRequest(
        @NotBlank(message = "가구 이름을 입력해 주세요.")
        @Size(max = 50, message = "가구 이름은 50자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "놓는 위치(mount)는 필수입니다.")
        CustomFurnitureMount mount,

        @NotNull(message = "높이를 입력해 주세요.")
        @DecimalMin(value = "0.05", message = "높이는 5cm 이상이어야 합니다.")
        @DecimalMax(value = "3.5", message = "높이는 3.5m 이하여야 합니다.")
        Double height,

        @DecimalMin("0.05") @DecimalMax("6")
        Double width,

        @DecimalMin("0.05") @DecimalMax("6")
        Double depth
) {
}
