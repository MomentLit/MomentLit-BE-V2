package com.example.like.repository;

import com.example.like.entity.PopupReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PopupReviewLikeRepository extends JpaRepository<PopupReviewLike, Long> {

    boolean existsByPopupReviewIdAndUserId(Long popupReviewId, String userId);

    Optional<PopupReviewLike> findByPopupReviewIdAndUserId(Long popupReviewId, String userId);

    void deleteAllByPopupReviewId(Long popupReviewId);
}
