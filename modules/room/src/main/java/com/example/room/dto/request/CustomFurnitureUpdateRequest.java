package com.example.room.dto.request;

import com.example.room.entity.CustomFurnitureMount;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 내 가구 이름·놓는 위치 바꾸기. 둘 다 선택(없으면 그대로). */
public record CustomFurnitureUpdateRequest(
        @Size(min = 1, max = 50, message = "가구 이름은 1~50자로 입력해 주세요.")
        @Pattern(regexp = ".*\\S.*", message = "가구 이름을 입력해 주세요.")
        String name,

        CustomFurnitureMount mount
) {
}
