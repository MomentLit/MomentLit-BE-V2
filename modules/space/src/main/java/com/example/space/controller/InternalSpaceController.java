package com.example.space.controller;

import com.example.common.security.UserPrincipal;
import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.response.AdminSpaceDetailResponse;
import com.example.space.dto.response.AdminSpaceListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceMatchingContextResponse;
import com.example.space.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/spaces")
public class InternalSpaceController {

    private final SpaceService spaceService;

    @GetMapping("/{space-id}/matching-context")
    public ResponseEntity<SpaceMatchingContextResponse> getMatchingContext(
            @PathVariable("space-id") Long spaceId,
            @RequestParam("start-time")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam("end-time")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime
    ) {
        return ResponseEntity.ok(
                spaceService.getMatchingContext(spaceId, startTime, endTime)
        );
    }

    @GetMapping
    public ResponseEntity<AdminSpaceListResponses> getAdminSpaces(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                spaceService.getAdminSpaces(getRole(principal))
        );
    }

    @GetMapping("/{space-id}")
    public ResponseEntity<AdminSpaceDetailResponse> getAdminSpace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        return ResponseEntity.ok(
                spaceService.getAdminSpace(getRole(principal), spaceId)
        );
    }

    @GetMapping("/{space-id}/admin-status")
    public ResponseEntity<SpaceAdminStatusResponse> getAdminStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        return ResponseEntity.ok(
                spaceService.getAdminStatus(getRole(principal), spaceId)
        );
    }

    @PatchMapping("/{space-id}/admin-status")
    public ResponseEntity<Void> updateAdminStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @RequestBody SpaceAdminStatusUpdateRequest request
    ) {
        spaceService.updateAdminStatus(getRole(principal), spaceId, request);

        return ResponseEntity.noContent().build();
    }

    private String getRole(UserPrincipal principal) {
        return principal != null ? principal.role().name() : null;
    }
}
