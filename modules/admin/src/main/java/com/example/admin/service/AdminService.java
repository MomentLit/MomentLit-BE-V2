package com.example.admin.service;

import com.example.admin.dto.response.AdminSpaceDetailResponse;
import com.example.admin.dto.response.AdminSpaceListResponses;
import com.example.admin.dto.response.AdminSpaceResponse;
import com.example.admin.global.exception.ForbiddenException;
import com.example.admin.global.exception.PreconditionFailedException;
import com.example.common.security.Role;
import com.example.space.api.SpaceInternalApi;
import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.entity.ApprovalStatus;
import org.springframework.stereotype.Service;
import lombok.*;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final SpaceInternalApi spaceApi;

    public AdminSpaceListResponses adminSpaceList(
            String authenticatedRole
    ){
        if (!Role.ADMIN.name().equals(authenticatedRole)){
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        return AdminSpaceListResponses.from(spaceApi.getAdminSpaces(authenticatedRole));
    }

    public AdminSpaceDetailResponse adminSpaceDetail(
            Long spaceId, String authenticatedRole
    ){
        if (!Role.ADMIN.name().equals(authenticatedRole)){
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        return AdminSpaceDetailResponse.from(spaceApi.getAdminSpace(authenticatedRole, spaceId));
    }

    public synchronized AdminSpaceResponse adminSpaceApprove(
            Long spaceId, String authenticatedRole
    ){
        if (!Role.ADMIN.name().equals(authenticatedRole)){
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        SpaceAdminStatusResponse space = spaceApi.getAdminStatus(authenticatedRole, spaceId);
        if (!space.adminStatus().equals(ApprovalStatus.PENDING)){
            throw new PreconditionFailedException("이미 승인됐거나 거절되었습니다");
        }
        spaceApi.updateAdminStatus(
                authenticatedRole,
                spaceId,
                new SpaceAdminStatusUpdateRequest(ApprovalStatus.APPROVED)
        );
        return AdminSpaceResponse.from(spaceId);
    }

    public synchronized AdminSpaceResponse adminSpaceReject(
            Long spaceId, String authenticatedRole
    ){
        if (!authenticatedRole.equals(Role.ADMIN.name())){
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        SpaceAdminStatusResponse space = spaceApi.getAdminStatus(authenticatedRole, spaceId);
        if (!space.adminStatus().equals(ApprovalStatus.PENDING)){
            throw new PreconditionFailedException("이미 승인됐거나 거절되었습니다");
        }
        spaceApi.updateAdminStatus(
                authenticatedRole,
                spaceId,
                new SpaceAdminStatusUpdateRequest(ApprovalStatus.REJECTED)
        );
        return AdminSpaceResponse.from(spaceId);
    }
}
