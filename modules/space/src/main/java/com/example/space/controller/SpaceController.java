package com.example.space.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.space.dto.request.ScheduleCreateRequest;
import com.example.space.dto.request.ScheduleUpdateRequest;
import com.example.space.dto.request.SpaceCreateRequest;
import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.dto.response.MySpaceListResponses;
import com.example.space.dto.response.ScheduleCreateResponse;
import com.example.space.dto.response.ScheduleListResponses;
import com.example.space.dto.response.SpaceCreateResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceListResponses;
import com.example.space.entity.SpaceCategory;
import com.example.space.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<SpaceCreateResponse>> createSpace(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SpaceCreateRequest request
    ) {
        SpaceCreateResponse response = spaceService.createSpace(
                principal.userId(),
                request
        );

        ApiResponse<SpaceCreateResponse> apiResponse =
                ResponseUtil.success("create space", response);

        return ResponseEntity.status(201).body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SpaceListResponses>> getSpaces(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SpaceCategory category
    ) {
        SpaceListResponses response = spaceService.getSpaces(
                name,
                category
        );

        ApiResponse<SpaceListResponses> apiResponse =
                ResponseUtil.success("select spaces", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{space-id}")
    public ResponseEntity<ApiResponse<SpaceDetailResponse>> getSpace(
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceDetailResponse response = spaceService.getSpace(spaceId);

        ApiResponse<SpaceDetailResponse> apiResponse =
                ResponseUtil.success("select space", response);

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{space-id}")
    public ResponseEntity<Void> updateSpace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @RequestBody SpaceUpdateRequest request
    ) {
        spaceService.updateSpace(
                principal.userId(),
                spaceId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{space-id}")
    public ResponseEntity<Void> deleteSpace(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        spaceService.deleteSpace(
                principal.userId(),
                spaceId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MySpaceListResponses>> getMySpaces(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SpaceCategory category
    ) {
        MySpaceListResponses response = spaceService.getMySpaces(
                principal.userId(),
                name,
                category
        );

        ApiResponse<MySpaceListResponses> apiResponse =
                ResponseUtil.success("select my spaces", response);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{space-id}/schedule")
    public ResponseEntity<ApiResponse<ScheduleCreateResponse>> createSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @Valid @RequestBody ScheduleCreateRequest request
    ) {
        ScheduleCreateResponse response = spaceService.createSchedule(
                principal.userId(),
                spaceId,
                request
        );

        ApiResponse<ScheduleCreateResponse> apiResponse =
                ResponseUtil.success("create schedule", response);

        return ResponseEntity.status(201).body(apiResponse);
    }

    @GetMapping("/{space-id}/schedule")
    public ResponseEntity<ApiResponse<ScheduleListResponses>> getSchedules(
            @PathVariable("space-id") Long spaceId
    ) {
        ScheduleListResponses response = spaceService.getSchedules(spaceId);

        ApiResponse<ScheduleListResponses> apiResponse =
                ResponseUtil.success("select schedules", response);

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{space-id}/schedule/{schedule-id}")
    public ResponseEntity<Void> updateSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @PathVariable("schedule-id") Long scheduleId,
            @RequestBody ScheduleUpdateRequest request
    ) {
        spaceService.updateSchedule(
                principal.userId(),
                spaceId,
                scheduleId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{space-id}/schedule/{schedule-id}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @PathVariable("schedule-id") Long scheduleId
    ) {
        spaceService.deleteSchedule(
                principal.userId(),
                spaceId,
                scheduleId
        );

        return ResponseEntity.noContent().build();
    }
}
