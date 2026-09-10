package com.example.like.repository;

import com.example.like.entity.SpaceReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceReviewLikeRepository extends JpaRepository<SpaceReviewLike, Long> {

    boolean existsBySpaceReviewIdAndUserId(Long spaceReviewId, String userId);

    Optional<SpaceReviewLike> findBySpaceReviewIdAndUserId(Long spaceReviewId, String userId);

    void deleteAllBySpaceReviewId(Long spaceReviewId);
}
