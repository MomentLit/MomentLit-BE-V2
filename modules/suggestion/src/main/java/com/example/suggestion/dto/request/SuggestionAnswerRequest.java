package com.example.suggestion.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SuggestionAnswerRequest(
        @NotBlank
        @JsonProperty("answer_content")
        String answerContent
) {
}
