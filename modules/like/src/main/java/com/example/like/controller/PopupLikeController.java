package com.example.like.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.like.dto.response.PopupLikeResponse;
import com.example.like.service.PopupLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PopupLikeController {

    private final PopupLikeService popupLikeService;

    @PostMapping("/popups/{popup-id}/likes")
    public ResponseEntity<ApiResponse<PopupLikeResponse>> like(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-id") Long popupId
    ) {
        PopupLikeResponse response = popupLikeService.like(principal.userId(), popupId);
        return ResponseEntity.ok(ResponseUtil.success("like popup", response));
    }

    @DeleteMapping("/popups/{popup-id}/likes")
    public ResponseEntity<ApiResponse<PopupLikeResponse>> unlike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-id") Long popupId
    ) {
        PopupLikeResponse response = popupLikeService.unlike(principal.userId(), popupId);
        return ResponseEntity.ok(ResponseUtil.success("unlike popup", response));
    }

    @GetMapping("/popups/{popup-id}/likes/me")
    public ResponseEntity<ApiResponse<PopupLikeResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-id") Long popupId
    ) {
        PopupLikeResponse response = popupLikeService.getStatus(principal.userId(), popupId);
        return ResponseEntity.ok(ResponseUtil.success("select popup like status", response));
    }
}
