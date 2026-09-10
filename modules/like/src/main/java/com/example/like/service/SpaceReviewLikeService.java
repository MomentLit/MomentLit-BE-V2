package com.example.like.service;

import com.example.like.dto.response.SpaceReviewLikeResponse;
import com.example.like.entity.SpaceReviewLike;
import com.example.like.global.exception.DuplicateReviewLikeException;
import com.example.like.global.exception.ReviewLikeNotFoundException;
import com.example.like.repository.SpaceReviewLikeRepository;
import com.example.review.entity.SpaceReview;
import com.example.review.global.exception.ReviewNotFoundException;
import com.example.review.repository.SpaceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceReviewLikeService {

    private final SpaceReviewLikeRepository spaceReviewLikeRepository;
    private final SpaceReviewRepository spaceReviewRepository;

    @Transactional
    public SpaceReviewLikeResponse like(String userId, Long reviewId) {
        SpaceReview review = getReviewWithLock(reviewId);
        if (spaceReviewLikeRepository.existsBySpaceReviewIdAndUserId(reviewId, userId)) {
            throw new DuplicateReviewLikeException("이미 좋아요한 공간 리뷰입니다.");
        }

        spaceReviewLikeRepository.save(SpaceReviewLike.create(reviewId, userId));
        review.increaseLikeCount();

        return SpaceReviewLikeResponse.liked(reviewId, review.getLikeCount());
    }

    @Transactional
    public SpaceReviewLikeResponse unlike(String userId, Long reviewId) {
        SpaceReview review = getReviewWithLock(reviewId);
        SpaceReviewLike reviewLike = spaceReviewLikeRepository.findBySpaceReviewIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewLikeNotFoundException("공간 리뷰 좋아요 기록을 찾을 수 없습니다."));

        spaceReviewLikeRepository.delete(reviewLike);
        review.decreaseLikeCount();

        return SpaceReviewLikeResponse.unliked(reviewId, review.getLikeCount());
    }

    public SpaceReviewLikeResponse getStatus(String userId, Long reviewId) {
        SpaceReview review = getReview(reviewId);
        boolean liked = spaceReviewLikeRepository.existsBySpaceReviewIdAndUserId(reviewId, userId);

        return liked
                ? SpaceReviewLikeResponse.liked(reviewId, review.getLikeCount())
                : SpaceReviewLikeResponse.unliked(reviewId, review.getLikeCount());
    }

    private SpaceReview getReview(Long reviewId) {
        return spaceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("공간 리뷰를 찾을 수 없습니다."));
    }

    private SpaceReview getReviewWithLock(Long reviewId) {
        return spaceReviewRepository.findByIdWithLock(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("공간 리뷰를 찾을 수 없습니다."));
    }
}
