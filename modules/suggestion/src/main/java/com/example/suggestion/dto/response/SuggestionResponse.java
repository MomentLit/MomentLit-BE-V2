package com.example.suggestion.dto.response;

import com.example.suggestion.entity.Suggestion;
import com.example.suggestion.entity.SuggestionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/** 내가 보낸 건의 목록/생성 응답 — 작성자 본인 조회이므로 `user_name`은 포함하지 않는다. */
public record SuggestionResponse(
        @JsonProperty("suggestion_id")
        Long suggestionId,

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

    public static SuggestionResponse from(Suggestion suggestion) {
        return new SuggestionResponse(
                suggestion.getId(),
                suggestion.getTitle(),
                suggestion.getContent(),
                suggestion.getStatus(),
                suggestion.getAnswerContent(),
                suggestion.getAnsweredAt(),
                suggestion.getCreatedAt()
        );
    }
}
