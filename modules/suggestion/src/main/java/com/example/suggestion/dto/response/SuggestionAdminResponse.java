package com.example.suggestion.dto.response;

import com.example.suggestion.entity.Suggestion;
import com.example.suggestion.entity.SuggestionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** 관리자용 건의 목록/답변 응답 — 작성자를 식별할 수 있도록 `user_name`을 포함한다. */
public record SuggestionAdminResponse(
        @JsonProperty("suggestion_id")
        Long suggestionId,

        @JsonProperty("user_name")
        String userName,

        String title,
        String content,
        SuggestionStatus status,

        @JsonProperty("answer_content")
        String answerContent,

        @JsonProperty("answered_at")
        LocalDateTime answeredAt,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

    public static SuggestionAdminResponse from(Suggestion suggestion, String userName) {
        return new SuggestionAdminResponse(
                suggestion.getId(),
                userName,
                suggestion.getTitle(),
                suggestion.getContent(),
                suggestion.getStatus(),
                suggestion.getAnswerContent(),
                suggestion.getAnsweredAt(),
                suggestion.getCreatedAt()
        );
    }
}
