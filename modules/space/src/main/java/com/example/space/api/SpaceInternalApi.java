package com.example.space.api;

import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.response.AdminSpaceDetailResponse;
import com.example.space.dto.response.AdminSpaceListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceMatchingContextResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SpaceInternalApi {

    AdminSpaceListResponses getAdminSpaces(String role);

    AdminSpaceDetailResponse getAdminSpace(String role, Long spaceId);

    SpaceAdminStatusResponse getAdminStatus(String role, Long spaceId);

    void updateAdminStatus(String role, Long spaceId, SpaceAdminStatusUpdateRequest request);

    SpaceMatchingContextResponse getMatchingContext(Long spaceId, LocalDateTime startTime, LocalDateTime endTime);

    SpaceDetailResponse getSpace(Long spaceId);

    /** 매칭 승인 시 matching 모듈이 호출 — 그 날짜를 예약 완료로 기록해 날짜 검색에서 걸러지게 한다. */
    void markSpaceBooked(Long spaceId, LocalDate date, Long matchingId);
}
