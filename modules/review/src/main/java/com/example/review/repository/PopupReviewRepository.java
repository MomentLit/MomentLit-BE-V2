package com.example.review.repository;

import com.example.review.entity.PopupReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PopupReviewRepository extends JpaRepository<PopupReview, Long> {

    boolean existsByPopupIdAndUserId(Long popupId, String userId);

    List<PopupReview> findAllByPopupIdOrderByCreatedAtDesc(Long popupId);
}
