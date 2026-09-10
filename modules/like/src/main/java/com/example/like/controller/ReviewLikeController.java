package com.example.like.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.like.dto.response.PopupReviewLikeResponse;
import com.example.like.dto.response.SpaceReviewLikeResponse;
import com.example.like.service.PopupReviewLikeService;
import com.example.like.service.SpaceReviewLikeService;
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
public class ReviewLikeController {

    private final SpaceReviewLikeService spaceReviewLikeService;
    private final PopupReviewLikeService popupReviewLikeService;

    @PostMapping("/space-reviews/{space-review-id}/likes")
    public ResponseEntity<ApiResponse<SpaceReviewLikeResponse>> likeSpaceReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-review-id") Long reviewId
    ) {
        SpaceReviewLikeResponse response = spaceReviewLikeService.like(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("like space review", response));
    }

    @DeleteMapping("/space-reviews/{space-review-id}/likes")
    public ResponseEntity<ApiResponse<SpaceReviewLikeResponse>> unlikeSpaceReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-review-id") Long reviewId
    ) {
        SpaceReviewLikeResponse response = spaceReviewLikeService.unlike(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("unlike space review", response));
    }

    @GetMapping("/space-reviews/{space-review-id}/likes/me")
    public ResponseEntity<ApiResponse<SpaceReviewLikeResponse>> getSpaceReviewStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-review-id") Long reviewId
    ) {
        SpaceReviewLikeResponse response = spaceReviewLikeService.getStatus(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("select space review like status", response));
    }

    @PostMapping("/popup-reviews/{popup-review-id}/likes")
    public ResponseEntity<ApiResponse<PopupReviewLikeResponse>> likePopupReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-review-id") Long reviewId
    ) {
        PopupReviewLikeResponse response = popupReviewLikeService.like(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("like popup review", response));
    }

    @DeleteMapping("/popup-reviews/{popup-review-id}/likes")
    public ResponseEntity<ApiResponse<PopupReviewLikeResponse>> unlikePopupReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-review-id") Long reviewId
    ) {
        PopupReviewLikeResponse response = popupReviewLikeService.unlike(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("unlike popup review", response));
    }

    @GetMapping("/popup-reviews/{popup-review-id}/likes/me")
    public ResponseEntity<ApiResponse<PopupReviewLikeResponse>> getPopupReviewStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-review-id") Long reviewId
    ) {
        PopupReviewLikeResponse response = popupReviewLikeService.getStatus(principal.userId(), reviewId);
        return ResponseEntity.ok(ResponseUtil.success("select popup review like status", response));
    }
}
