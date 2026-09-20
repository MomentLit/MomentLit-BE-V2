package com.example.alarm.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AlarmCreateRequest(
        @NotBlank
        @JsonProperty("user_id")
        String userId,

        @NotBlank
        String description
) {
}
