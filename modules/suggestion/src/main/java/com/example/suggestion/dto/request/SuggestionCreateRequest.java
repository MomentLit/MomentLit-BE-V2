package com.example.suggestion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SuggestionCreateRequest(
        @NotBlank
        String title,

        @NotBlank
        String content
) {
}
