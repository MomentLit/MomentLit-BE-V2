package com.example.space.dto.request;

import com.example.space.entity.ApprovalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceAdminStatusUpdateRequest(
        @JsonProperty("admin_status")
        ApprovalStatus adminStatus
) {
}
