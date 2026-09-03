package com.example.admin.controller;

import com.example.admin.dto.response.AdminSpaceDetailResponse;
import com.example.admin.dto.response.AdminSpaceListResponses;
import com.example.admin.dto.response.AdminSpaceResponse;
import com.example.admin.service.AdminService;
import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/spaces")
public class AdminController {
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminSpaceListResponses>> spaceList(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminSpaceListResponses response = adminService.adminSpaceList(principal.role().name());
        ApiResponse<AdminSpaceListResponses> apiResponse =
                ResponseUtil.success("space list", response);
        return ResponseEntity.status(200).body(apiResponse);

    }
    @GetMapping("/{space-id}")
    public ResponseEntity<ApiResponse<AdminSpaceDetailResponse>> spaceDetail(
            @PathVariable("space-id") Long spaceId, @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminSpaceDetailResponse response = adminService.adminSpaceDetail(spaceId, principal.role().name());
        ApiResponse<AdminSpaceDetailResponse> apiResponse =
                ResponseUtil.success("space detail", response);
        return ResponseEntity.status(200).body(apiResponse);

    }
    @PatchMapping("/{space-id}/approve")
    public ResponseEntity<ApiResponse<AdminSpaceResponse>> spaceApprove(
            @PathVariable("space-id") Long spaceId, @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminSpaceResponse response = adminService.adminSpaceApprove(spaceId, principal.role().name());
        ApiResponse<AdminSpaceResponse> apiResponse =
                ResponseUtil.success("space approve", response);
        return ResponseEntity.status(202).body(apiResponse);

    }
    @PatchMapping("/{space-id}/reject")
    public ResponseEntity<ApiResponse<AdminSpaceResponse>> spaceReject(
            @PathVariable("space-id") Long spaceId, @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminSpaceResponse response = adminService.adminSpaceReject(spaceId, principal.role().name());
        ApiResponse<AdminSpaceResponse> apiResponse =
                ResponseUtil.success("space reject", response);
        return ResponseEntity.status(202).body(apiResponse);

    }
}
