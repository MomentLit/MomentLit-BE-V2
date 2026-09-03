package com.example.matching.controller;

import com.example.matching.dto.response.InternalMatchingResponse;
import com.example.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/matchings")
public class InternalMatchingController {

    private final MatchingService matchingService;

    @GetMapping("/{matching-id}")
    public ResponseEntity<InternalMatchingResponse> getMatching(
            @PathVariable("matching-id") Long matchingId
    ) {
        return ResponseEntity.ok(matchingService.getMatchingForInternal(matchingId));
    }
}
