package com.example.alarm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AlarmCreateRequest(
        @NotBlank
        String description
) {
}
