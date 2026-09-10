package com.example.like.repository;

import com.example.like.entity.PopupLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PopupLikeRepository extends JpaRepository<PopupLike, Long> {

    boolean existsByPopupIdAndUserId(Long popupId, String userId);

    Optional<PopupLike> findByPopupIdAndUserId(Long popupId, String userId);
}
