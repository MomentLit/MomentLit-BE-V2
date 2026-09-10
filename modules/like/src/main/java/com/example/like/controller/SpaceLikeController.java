package com.example.like.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.like.dto.response.SpaceLikeResponse;
import com.example.like.service.SpaceLikeService;
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
public class SpaceLikeController {

    private final SpaceLikeService spaceLikeService;

    @PostMapping("/spaces/{space-id}/likes")
    public ResponseEntity<ApiResponse<SpaceLikeResponse>> like(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceLikeResponse response = spaceLikeService.like(principal.userId(), spaceId);
        return ResponseEntity.ok(ResponseUtil.success("like space", response));
    }

    @DeleteMapping("/spaces/{space-id}/likes")
    public ResponseEntity<ApiResponse<SpaceLikeResponse>> unlike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceLikeResponse response = spaceLikeService.unlike(principal.userId(), spaceId);
        return ResponseEntity.ok(ResponseUtil.success("unlike space", response));
    }

    @GetMapping("/spaces/{space-id}/likes/me")
    public ResponseEntity<ApiResponse<SpaceLikeResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-id") Long spaceId
    ) {
        SpaceLikeResponse response = spaceLikeService.getStatus(principal.userId(), spaceId);
        return ResponseEntity.ok(ResponseUtil.success("select space like status", response));
    }
}
