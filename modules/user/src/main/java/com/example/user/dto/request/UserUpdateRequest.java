package com.example.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
        String name,

        @JsonProperty("image_url")
        String imageUrl,

        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 형식의 휴대폰 번호가 아닙니다. (예: 01012345678)")
        String phone,

        String intro
) {
}
