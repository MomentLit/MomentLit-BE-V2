package com.example.like.service;

import com.example.like.dto.response.PopupReviewLikeResponse;
import com.example.like.entity.PopupReviewLike;
import com.example.like.global.exception.DuplicateReviewLikeException;
import com.example.like.global.exception.ReviewLikeNotFoundException;
import com.example.like.repository.PopupReviewLikeRepository;
import com.example.review.entity.PopupReview;
import com.example.review.global.exception.ReviewNotFoundException;
import com.example.review.repository.PopupReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupReviewLikeService {

    private final PopupReviewLikeRepository popupReviewLikeRepository;
    private final PopupReviewRepository popupReviewRepository;

    @Transactional
    public PopupReviewLikeResponse like(String userId, Long reviewId) {
        PopupReview review = getReviewWithLock(reviewId);
        if (popupReviewLikeRepository.existsByPopupReviewIdAndUserId(reviewId, userId)) {
            throw new DuplicateReviewLikeException("이미 좋아요한 팝업 리뷰입니다.");
        }

        popupReviewLikeRepository.save(PopupReviewLike.create(reviewId, userId));
        review.increaseLikeCount();

        return PopupReviewLikeResponse.liked(reviewId, review.getLikeCount());
    }

    @Transactional
    public PopupReviewLikeResponse unlike(String userId, Long reviewId) {
        PopupReview review = getReviewWithLock(reviewId);
        PopupReviewLike reviewLike = popupReviewLikeRepository.findByPopupReviewIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewLikeNotFoundException("팝업 리뷰 좋아요 기록을 찾을 수 없습니다."));

        popupReviewLikeRepository.delete(reviewLike);
        review.decreaseLikeCount();

        return PopupReviewLikeResponse.unliked(reviewId, review.getLikeCount());
    }

    public PopupReviewLikeResponse getStatus(String userId, Long reviewId) {
        PopupReview review = getReview(reviewId);
        boolean liked = popupReviewLikeRepository.existsByPopupReviewIdAndUserId(reviewId, userId);

        return liked
                ? PopupReviewLikeResponse.liked(reviewId, review.getLikeCount())
                : PopupReviewLikeResponse.unliked(reviewId, review.getLikeCount());
    }

    private PopupReview getReview(Long reviewId) {
        return popupReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("팝업 리뷰를 찾을 수 없습니다."));
    }

    private PopupReview getReviewWithLock(Long reviewId) {
        return popupReviewRepository.findByIdWithLock(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("팝업 리뷰를 찾을 수 없습니다."));
    }
}
