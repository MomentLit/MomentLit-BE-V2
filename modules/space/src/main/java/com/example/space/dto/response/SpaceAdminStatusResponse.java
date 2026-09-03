package com.example.space.dto.response;

import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Space;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceAdminStatusResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("admin_status")
        ApprovalStatus adminStatus
) {

    public static SpaceAdminStatusResponse from(Space space) {
        return new SpaceAdminStatusResponse(space.getId(), space.getAdminStatus());
    }
}
