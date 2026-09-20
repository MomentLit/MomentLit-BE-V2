package com.example.popup.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.dto.PageResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.popup.dto.request.PopupCreateRequest;
import com.example.popup.dto.response.PopupCreateResponse;
import com.example.popup.dto.response.PopupDetailResponse;
import com.example.popup.dto.response.PopupHistoryResponses;
import com.example.popup.dto.response.PopupListResponse;
import com.example.popup.dto.response.PopupListResponses;
import com.example.popup.service.PopupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    @PostMapping("/popups")
    public ResponseEntity<ApiResponse<PopupCreateResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PopupCreateRequest request
    ) {
        PopupCreateResponse response = popupService.create(principal.userId(), request);
        ApiResponse<PopupCreateResponse> apiResponse = ResponseUtil.success("create popup", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/popups")
    public ResponseEntity<ApiResponse<PageResponse<PopupListResponse>>> getPopups(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PopupListResponse> response = popupService.getPopups(pageable);
        ApiResponse<PageResponse<PopupListResponse>> apiResponse = ResponseUtil.success("select popups", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/popups/recommendations")
    public ResponseEntity<ApiResponse<PopupListResponses>> getRecommendations() {
        PopupListResponses response = popupService.getRecommendations();
        ApiResponse<PopupListResponses> apiResponse = ResponseUtil.success("select popup recommendations", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/popups/me")
    public ResponseEntity<ApiResponse<PopupListResponses>> getMyPopups(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PopupListResponses response = popupService.getMyPopups(principal.userId());
        ApiResponse<PopupListResponses> apiResponse = ResponseUtil.success("select my popups", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/popups/{popup-id}")
    public ResponseEntity<ApiResponse<PopupDetailResponse>> getPopup(
            @PathVariable("popup-id") Long popupId
    ) {
        PopupDetailResponse response = popupService.getPopup(popupId);
        ApiResponse<PopupDetailResponse> apiResponse = ResponseUtil.success("select popup", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/spaces/{space-id}/popups/history")
    public ResponseEntity<ApiResponse<PopupHistoryResponses>> getSpacePopupHistories(
            @PathVariable("space-id") Long spaceId
    ) {
        PopupHistoryResponses response = popupService.getSpacePopupHistories(spaceId);
        ApiResponse<PopupHistoryResponses> apiResponse = ResponseUtil.success("select space popup histories", response);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/users/{user-id}/popups/history")
    public ResponseEntity<ApiResponse<PopupListResponses>> getSellerPopupHistories(
            @PathVariable("user-id") String userId
    ) {
        PopupListResponses response = popupService.getSellerPopupHistories(userId);
        ApiResponse<PopupListResponses> apiResponse = ResponseUtil.success("select seller popup histories", response);

        return ResponseEntity.ok(apiResponse);
    }
}
