package com.example.space.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.dto.PageResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.space.dto.request.ScheduleCreateRequest;
import com.example.space.dto.request.ScheduleUpdateRequest;
import com.example.space.dto.request.SpaceAvailabilitySlotRequest;
import com.example.space.dto.request.SpaceCreateRequest;
import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.dto.response.MySpaceListResponse;
import com.example.space.dto.response.ScheduleCreateResponse;
import com.example.space.dto.response.ScheduleListResponses;
import com.example.space.dto.response.SpaceAvailabilityListResponses;
import com.example.space.dto.response.SpaceBookedDatesResponse;
import com.example.space.dto.response.SpaceCategoryCountResponse;
import com.example.space.dto.response.SpaceCreateResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceListResponse;
import com.example.space.dto.response.SpaceRegionCountResponse;
import com.example.space.entity.Region;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import com.example.space.service.SpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<ApiResponse<PageResponse<SpaceListResponse>>> getSpaces(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SpaceCategory category,
            @RequestParam(required = false) Region region,
            @RequestParam(name = "usage-unit", required = false) UsageUnit usageUnit,
            @RequestParam(name = "min-capacity", required = false) Integer minCapacity,
            @RequestParam(name = "max-capacity", required = false) Integer maxCapacity,
            // 특정 날짜 검색 — 그 요일에 정기적으로 열려 있으면서, 그 날짜에 이미 승인된 예약으로 차지 않은 공간만 남긴다.
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            // lat/lng를 같이 주면 "가까운순" — 좌표는 브라우저 Geolocation에서 온다.
            // 둘 다 있을 때만 거리순 정렬로 전환되고, pageable의 sort는 이때 무시된다.
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            // 정렬은 createdAt(기본)/likeCount만 지원한다(거리순은 위 lat/lng로 별도 처리).
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<SpaceListResponse> response = spaceService.getSpaces(
                name,
                category,
                region,
                usageUnit,
                minCapacity,
                maxCapacity,
                date,
                lat,
                lng,
                pageable
        );

        ApiResponse<PageResponse<SpaceListResponse>> apiResponse =
                ResponseUtil.success("select spaces", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/counts/by-category")
    public ResponseEntity<ApiResponse<List<SpaceCategoryCountResponse>>> getCountsByCategory() {
        List<SpaceCategoryCountResponse> response = spaceService.getCountsByCategory();

        ApiResponse<List<SpaceCategoryCountResponse>> apiResponse =
                ResponseUtil.success("select space counts by category", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/counts/by-region")
    public ResponseEntity<ApiResponse<List<SpaceRegionCountResponse>>> getCountsByRegion() {
        List<SpaceRegionCountResponse> response = spaceService.getCountsByRegion();

        ApiResponse<List<SpaceRegionCountResponse>> apiResponse =
                ResponseUtil.success("select space counts by region", response);

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
            @Valid @RequestBody SpaceUpdateRequest request
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
    public ResponseEntity<ApiResponse<PageResponse<MySpaceListResponse>>> getMySpaces(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SpaceCategory category,
            @RequestParam(required = false) Region region,
            @RequestParam(name = "usage-unit", required = false) UsageUnit usageUnit,
            @RequestParam(name = "min-capacity", required = false) Integer minCapacity,
            @RequestParam(name = "max-capacity", required = false) Integer maxCapacity,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<MySpaceListResponse> response = spaceService.getMySpaces(
                principal.userId(),
                name,
                category,
                region,
                usageUnit,
                minCapacity,
                maxCapacity,
                pageable
        );

        ApiResponse<PageResponse<MySpaceListResponse>> apiResponse =
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

    @PutMapping("/{space-id}/availability")
    public ResponseEntity<Void> updateAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId,
            @RequestBody List<SpaceAvailabilitySlotRequest> request
    ) {
        spaceService.updateAvailability(
                principal.userId(),
                spaceId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{space-id}/availability")
    public ResponseEntity<ApiResponse<SpaceAvailabilityListResponses>> getAvailability(
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceAvailabilityListResponses response = spaceService.getAvailability(spaceId);

        ApiResponse<SpaceAvailabilityListResponses> apiResponse =
                ResponseUtil.success("select space availability", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{space-id}/booked-dates")
    public ResponseEntity<ApiResponse<SpaceBookedDatesResponse>> getBookedDates(
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceBookedDatesResponse response = spaceService.getBookedDates(spaceId);

        ApiResponse<SpaceBookedDatesResponse> apiResponse =
                ResponseUtil.success("select space booked dates", response);

        return ResponseEntity.ok(apiResponse);
    }
}
