package com.example.review.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.review.dto.request.PopupReviewCreateRequest;
import com.example.review.dto.request.PopupReviewUpdateRequest;
import com.example.review.dto.request.SpaceReviewCreateRequest;
import com.example.review.dto.request.SpaceReviewUpdateRequest;
import com.example.review.dto.response.PopupReviewResponses;
import com.example.review.dto.response.PopupReviewCreateResponse;
import com.example.review.dto.response.SpaceReviewCreateResponse;
import com.example.review.dto.response.SpaceReviewResponses;
import com.example.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/matchings/{matching-id}/reviews")
    public ResponseEntity<ApiResponse<SpaceReviewCreateResponse>> createSpaceReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("matching-id") Long matchingId,
            @RequestBody SpaceReviewCreateRequest request
    ) {
        SpaceReviewCreateResponse response = reviewService.createSpaceReview(principal.userId(), matchingId, request);
        return ResponseEntity.ok(ResponseUtil.success("create space review", response));
    }

    @GetMapping("/spaces/{space-id}/reviews")
    public ResponseEntity<ApiResponse<SpaceReviewResponses>> getSpaceReviews(
            @PathVariable("space-id") Long spaceId
    ) {
        return ResponseEntity.ok(ResponseUtil.success("select space reviews", reviewService.getSpaceReviews(spaceId)));
    }

    @PatchMapping("/space-reviews/{space-review-id}")
    public ResponseEntity<Void> updateSpaceReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-review-id") Long reviewId,
            @RequestBody SpaceReviewUpdateRequest request
    ) {
        reviewService.updateSpaceReview(principal.userId(), reviewId, request);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/space-reviews/{space-review-id}")
    public ResponseEntity<Void> deleteSpaceReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("space-review-id") Long reviewId
    ) {
        reviewService.deleteSpaceReview(principal.userId(), reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/popups/{popup-id}/reviews")
    public ResponseEntity<ApiResponse<PopupReviewCreateResponse>> createPopupReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-id") Long popupId,
            @RequestBody PopupReviewCreateRequest request
    ) {
        PopupReviewCreateResponse response = reviewService.createPopupReview(principal.userId(), popupId, request);
        return ResponseEntity.ok(ResponseUtil.success("create popup review", response));
    }

    @GetMapping("/popups/{popup-id}/reviews")
    public ResponseEntity<ApiResponse<PopupReviewResponses>> getPopupReviews(
            @PathVariable("popup-id") Long popupId
    ) {
        return ResponseEntity.ok(ResponseUtil.success("select popup reviews", reviewService.getPopupReviews(popupId)));
    }

    @PatchMapping("/popup-reviews/{popup-review-id}")
    public ResponseEntity<Void> updatePopupReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-review-id") Long reviewId,
            @RequestBody PopupReviewUpdateRequest request
    ) {
        reviewService.updatePopupReview(principal.userId(), reviewId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/popup-reviews/{popup-review-id}")
    public ResponseEntity<Void> deletePopupReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("popup-review-id") Long reviewId
    ) {
        reviewService.deletePopupReview(principal.userId(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
