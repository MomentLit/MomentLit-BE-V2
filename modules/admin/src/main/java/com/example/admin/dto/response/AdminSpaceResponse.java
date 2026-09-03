package com.example.admin.dto.response;

public record AdminSpaceResponse(
        Long spaceId
) {
    public static AdminSpaceResponse from(Long spaceId){
        return new AdminSpaceResponse(spaceId);
    }
}
