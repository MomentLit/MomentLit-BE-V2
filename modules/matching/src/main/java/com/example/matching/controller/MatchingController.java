package com.example.matching.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.matching.dto.request.MatchingCreateRequest;
import com.example.matching.dto.response.HostStatsResponse;
import com.example.matching.dto.response.MatchingCreateResponse;
import com.example.matching.dto.response.MatchingListResponse;
import com.example.matching.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/matchings")
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping
    public ResponseEntity<ApiResponse<MatchingCreateResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MatchingCreateRequest request
    ) {
        MatchingCreateResponse response = matchingService.create(principal.userId(), request);
        ApiResponse<MatchingCreateResponse> apiResponse = ResponseUtil.success("create matching", response);

        return ResponseEntity.status(201).body(apiResponse);
    }

    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<MatchingListResponse>> getReceivedMatchings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MatchingListResponse response = matchingService.getReceivedMatchings(principal.userId());
        ApiResponse<MatchingListResponse> apiResponse = ResponseUtil.success("select received matchings", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MatchingListResponse>> getSentMatchings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MatchingListResponse response = matchingService.getSentMatchings(principal.userId());
        ApiResponse<MatchingListResponse> apiResponse = ResponseUtil.success("select sent matchings", response);

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{matching-id}/approve")
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("matching-id") Long matchingId
    ) {
        matchingService.approve(principal.userId(), matchingId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{matching-id}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("matching-id") Long matchingId
    ) {
        matchingService.reject(principal.userId(), matchingId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{matching-id}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("matching-id") Long matchingId
    ) {
        matchingService.cancel(principal.userId(), matchingId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/host-stats/{host-id}")
    public ResponseEntity<ApiResponse<HostStatsResponse>> getHostStats(
            @PathVariable("host-id") String hostId
    ) {
        HostStatsResponse response = matchingService.getHostStats(hostId);
        ApiResponse<HostStatsResponse> apiResponse = ResponseUtil.success("select host stats", response);

        return ResponseEntity.ok(apiResponse);
    }
}
