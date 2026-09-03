package com.example.admin.dto.response;

import java.util.List;

public record AdminSpaceListResponses(
        List<AdminSpaceListResponse> spaces
) {
    public static AdminSpaceListResponses from(com.example.space.dto.response.AdminSpaceListResponses spaces){
        return new AdminSpaceListResponses(
                spaces.spaces().stream()
                        .map(AdminSpaceListResponse::from)
                        .toList()
        );
    }
}
