package com.example.space.api;

import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.response.AdminSpaceDetailResponse;
import com.example.space.dto.response.AdminSpaceListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceMatchingContextResponse;

import java.time.LocalDateTime;

public interface SpaceInternalApi {

    AdminSpaceListResponses getAdminSpaces(String role);

    AdminSpaceDetailResponse getAdminSpace(String role, Long spaceId);

    SpaceAdminStatusResponse getAdminStatus(String role, Long spaceId);

    void updateAdminStatus(String role, Long spaceId, SpaceAdminStatusUpdateRequest request);

    SpaceMatchingContextResponse getMatchingContext(Long spaceId, LocalDateTime startTime, LocalDateTime endTime);

    SpaceDetailResponse getSpace(Long spaceId);
}
