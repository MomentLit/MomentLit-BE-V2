package com.example.room.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 사용자가 3D 방에 놓아 본 가구 배치 저장. 빈 목록이면 "아무것도 안 놓은 배치"로 저장된다. */
public record FurnitureLayoutRequest(
        @NotBlank(message = "배치 이름을 입력해 주세요.")
        @Size(max = 50, message = "배치 이름은 50자 이하로 입력해 주세요.")
        String name,

        @NotNull(message = "가구 목록(items)은 필수입니다.")
        @Size(max = 200, message = "가구는 200개까지 놓을 수 있습니다.")
        List<@NotNull @Valid Item> items
) {

    /**
     * @param type     가구 종류 — 프론트 가구 목록의 id (예: sofa-3, bed-queen). 목록은 프론트가 관리한다
     * @param x        바닥 평면 x(m), 가구 중심
     * @param z        바닥 평면 z(m), 가구 중심
     * @param rotation y축 회전(라디안)
     * @param y        바닥에서 가구 밑면까지 높이(m). 가구 위 소품·벽에 건 액자·천장 조명에 쓴다.
     *                 없으면(null) 바닥 — 이 필드가 생기기 전에 저장한 배치도 그대로 읽힌다
     */
    public record Item(
            @NotNull(message = "가구 종류는 필수입니다.")
            @Pattern(regexp = "[a-z0-9-]{1,40}", message = "가구 종류는 영문 소문자·숫자·하이픈 40자 이하입니다.")
            String type,

            @NotNull @DecimalMin("-100") @DecimalMax("100")
            Double x,

            @NotNull @DecimalMin("-100") @DecimalMax("100")
            Double z,

            @NotNull @DecimalMin("-7") @DecimalMax("7")
            Double rotation,

            @DecimalMin("0") @DecimalMax("10")
            Double y
    ) {
    }
}
