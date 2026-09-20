package com.example.suggestion.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.dto.PageResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.suggestion.dto.request.SuggestionAnswerRequest;
import com.example.suggestion.dto.request.SuggestionCreateRequest;
import com.example.suggestion.dto.response.SuggestionAdminResponse;
import com.example.suggestion.dto.response.SuggestionResponse;
import com.example.suggestion.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping("/suggestions")
    public ResponseEntity<ApiResponse<SuggestionResponse>> createSuggestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SuggestionCreateRequest request
    ) {
        SuggestionResponse response = suggestionService.createSuggestion(principal.userId(), request);
        return ResponseEntity.status(201).body(ResponseUtil.success("create suggestion", response));
    }

    @GetMapping("/suggestions/me")
    public ResponseEntity<ApiResponse<PageResponse<SuggestionResponse>>> getMySuggestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SuggestionResponse> response = suggestionService.getMySuggestions(principal.userId(), pageable);
        return ResponseEntity.ok(ResponseUtil.success("list my suggestions", response));
    }

    @GetMapping("/admin/suggestions")
    public ResponseEntity<ApiResponse<PageResponse<SuggestionAdminResponse>>> getAllSuggestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SuggestionAdminResponse> response = suggestionService.getAllSuggestions(principal.role(), pageable);
        return ResponseEntity.ok(ResponseUtil.success("list all suggestions", response));
    }

    @PatchMapping("/admin/suggestions/{suggestion-id}/answer")
    public ResponseEntity<ApiResponse<SuggestionAdminResponse>> answerSuggestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("suggestion-id") Long suggestionId,
            @Valid @RequestBody SuggestionAnswerRequest request
    ) {
        SuggestionAdminResponse response =
                suggestionService.answerSuggestion(principal.role(), suggestionId, request.answerContent());
        return ResponseEntity.ok(ResponseUtil.success("answer suggestion", response));
    }
}
