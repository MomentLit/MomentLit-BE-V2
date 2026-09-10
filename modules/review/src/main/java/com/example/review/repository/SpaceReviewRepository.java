package com.example.review.repository;

import com.example.review.entity.SpaceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceReviewRepository extends JpaRepository<SpaceReview, Long> {

    boolean existsByMatchingId(Long matchingId);

    List<SpaceReview> findAllBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
