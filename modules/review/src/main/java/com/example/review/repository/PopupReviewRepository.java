package com.example.review.repository;

import com.example.review.entity.PopupReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PopupReviewRepository extends JpaRepository<PopupReview, Long> {

    boolean existsByPopupIdAndUserId(Long popupId, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PopupReview r where r.id = :reviewId")
    Optional<PopupReview> findByIdWithLock(@Param("reviewId") Long reviewId);

    List<PopupReview> findAllByPopupIdOrderByCreatedAtDesc(Long popupId);
}
