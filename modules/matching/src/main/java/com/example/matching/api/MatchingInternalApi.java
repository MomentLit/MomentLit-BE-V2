package com.example.matching.api;

import com.example.matching.dto.response.InternalMatchingResponse;

public interface MatchingInternalApi {

    InternalMatchingResponse getMatchingForInternal(Long matchingId);
}
