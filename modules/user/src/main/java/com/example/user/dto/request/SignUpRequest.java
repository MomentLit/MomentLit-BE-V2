package com.example.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank
        String name,

        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 형식의 휴대폰 번호가 아닙니다. (예: 01012345678)")
        String phone
) {
}
