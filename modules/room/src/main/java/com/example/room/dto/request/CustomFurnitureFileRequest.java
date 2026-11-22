package com.example.room.dto.request;

import com.example.room.entity.CustomFurnitureMount;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * GLB 파일로 내 가구 올리기(multipart 폼 필드). 파일은 model(GLB)·thumbnail(PNG) 파트.
 * FE가 미리보기에서 바닥 가운데 원점·앞 +z·실제 크기(m)로 맞춘 GLB와 그 화면 썸네일을 보낸다. 치수는 그 모델의 크기.
 */
public record CustomFurnitureFileRequest(
        @NotBlank(message = "가구 이름을 입력해 주세요.")
        @Size(max = 50, message = "가구 이름은 50자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "놓는 위치(mount)는 필수입니다.")
        CustomFurnitureMount mount,

        @NotNull @DecimalMin("0.01") @DecimalMax("6")
        Double width,

        @NotNull @DecimalMin("0.01") @DecimalMax("6")
        Double depth,

        @NotNull @DecimalMin("0.01") @DecimalMax("3.5")
        Double height
) {
}
