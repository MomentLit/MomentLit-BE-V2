package com.example.like.service;

import com.example.like.dto.response.PopupLikeResponse;
import com.example.like.entity.PopupLike;
import com.example.like.global.exception.DuplicatePopupLikeException;
import com.example.like.global.exception.PopupLikeNotFoundException;
import com.example.like.repository.PopupLikeRepository;
import com.example.popup.entity.Popup;
import com.example.popup.repository.PopupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopupLikeServiceTest {

    private static final Long POPUP_ID = 1L;
    private static final String USER_ID = "user-1";

    @Mock
    private PopupLikeRepository popupLikeRepository;

    @Mock
    private PopupRepository popupRepository;

    private PopupLikeService popupLikeService;

    @BeforeEach
    void setUp() {
        popupLikeService = new PopupLikeService(popupLikeRepository, popupRepository);
    }

    @Test
    void like_createsLikeAndIncreasesCount() {
        Popup popup = popup();
        when(popupRepository.findByIdWithLock(POPUP_ID)).thenReturn(Optional.of(popup));
        when(popupLikeRepository.existsByPopupIdAndUserId(POPUP_ID, USER_ID)).thenReturn(false);

        PopupLikeResponse response = popupLikeService.like(USER_ID, POPUP_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(popupLikeRepository).save(any(PopupLike.class));
    }

    @Test
    void like_rejectsDuplicateLike() {
        when(popupRepository.findByIdWithLock(POPUP_ID)).thenReturn(Optional.of(popup()));
        when(popupLikeRepository.existsByPopupIdAndUserId(POPUP_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> popupLikeService.like(USER_ID, POPUP_ID))
                .isInstanceOf(DuplicatePopupLikeException.class);

        verify(popupLikeRepository, never()).save(any());
    }

    @Test
    void unlike_deletesLikeAndDecreasesCount() {
        Popup popup = popup();
        popup.increaseLikeCount();
        PopupLike popupLike = PopupLike.create(POPUP_ID, USER_ID);
        when(popupRepository.findByIdWithLock(POPUP_ID)).thenReturn(Optional.of(popup));
        when(popupLikeRepository.findByPopupIdAndUserId(POPUP_ID, USER_ID)).thenReturn(Optional.of(popupLike));

        PopupLikeResponse response = popupLikeService.unlike(USER_ID, POPUP_ID);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(popupLikeRepository).delete(popupLike);
    }

    @Test
    void unlike_rejectsMissingLike() {
        when(popupRepository.findByIdWithLock(POPUP_ID)).thenReturn(Optional.of(popup()));
        when(popupLikeRepository.findByPopupIdAndUserId(POPUP_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> popupLikeService.unlike(USER_ID, POPUP_ID))
                .isInstanceOf(PopupLikeNotFoundException.class);
    }

    private Popup popup() {
        return Popup.create(
                2L,
                3L,
                "seller-1",
                "팝업",
                "설명",
                "https://example.com/thumbnail.jpg",
                LocalDateTime.of(2026, 9, 10, 10, 0),
                LocalDateTime.of(2026, 9, 10, 18, 0)
        );
    }
}
