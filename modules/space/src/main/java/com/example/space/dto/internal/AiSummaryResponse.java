package com.example.space.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryResponse(@JsonProperty("ai_summary") String aiSummary) {
}
