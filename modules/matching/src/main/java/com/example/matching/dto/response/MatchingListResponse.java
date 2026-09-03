package com.example.matching.dto.response;

import java.util.List;

public record MatchingListResponse(
        List<MatchingSearchResponse> matchings
) {
}
