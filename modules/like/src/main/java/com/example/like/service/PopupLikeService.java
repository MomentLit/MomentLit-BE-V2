package com.example.like.service;

import com.example.like.dto.response.PopupLikeResponse;
import com.example.like.entity.PopupLike;
import com.example.like.global.exception.DuplicatePopupLikeException;
import com.example.like.global.exception.PopupLikeNotFoundException;
import com.example.like.repository.PopupLikeRepository;
import com.example.popup.entity.Popup;
import com.example.popup.global.exception.PopupNotFoundException;
import com.example.popup.repository.PopupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupLikeService {

    private final PopupLikeRepository popupLikeRepository;
    private final PopupRepository popupRepository;

    @Transactional
    public PopupLikeResponse like(String userId, Long popupId) {
        Popup popup = getPopupWithLock(popupId);

        if (popupLikeRepository.existsByPopupIdAndUserId(popupId, userId)) {
            throw new DuplicatePopupLikeException("이미 좋아요한 팝업입니다.");
        }

        popupLikeRepository.save(PopupLike.create(popupId, userId));
        popup.increaseLikeCount();

        return PopupLikeResponse.liked(popupId, popup.getLikeCount());
    }

    @Transactional
    public PopupLikeResponse unlike(String userId, Long popupId) {
        Popup popup = getPopupWithLock(popupId);
        PopupLike popupLike = popupLikeRepository.findByPopupIdAndUserId(popupId, userId)
                .orElseThrow(() -> new PopupLikeNotFoundException("좋아요 기록을 찾을 수 없습니다."));

        popupLikeRepository.delete(popupLike);
        popup.decreaseLikeCount();

        return PopupLikeResponse.unliked(popupId, popup.getLikeCount());
    }

    public PopupLikeResponse getStatus(String userId, Long popupId) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new PopupNotFoundException("팝업을 찾을 수 없습니다."));
        boolean liked = popupLikeRepository.existsByPopupIdAndUserId(popupId, userId);

        return liked
                ? PopupLikeResponse.liked(popupId, popup.getLikeCount())
                : PopupLikeResponse.unliked(popupId, popup.getLikeCount());
    }

    private Popup getPopupWithLock(Long popupId) {
        return popupRepository.findByIdWithLock(popupId)
                .orElseThrow(() -> new PopupNotFoundException("팝업을 찾을 수 없습니다."));
    }
}
