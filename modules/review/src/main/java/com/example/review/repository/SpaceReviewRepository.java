package com.example.review.repository;

import com.example.review.entity.SpaceReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpaceReviewRepository extends JpaRepository<SpaceReview, Long> {

    boolean existsByMatchingId(Long matchingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SpaceReview r where r.id = :reviewId")
    Optional<SpaceReview> findByIdWithLock(@Param("reviewId") Long reviewId);

    List<SpaceReview> findAllBySpaceIdOrderByCreatedAtDesc(Long spaceId);
}
