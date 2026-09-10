package com.example.like.service;

import com.example.like.dto.response.PopupReviewLikeResponse;
import com.example.like.entity.PopupReviewLike;
import com.example.like.repository.PopupReviewLikeRepository;
import com.example.review.entity.PopupReview;
import com.example.review.entity.VerificationType;
import com.example.review.repository.PopupReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopupReviewLikeServiceTest {

    private static final Long REVIEW_ID = 1L;
    private static final String USER_ID = "user-1";

    @Mock
    private PopupReviewLikeRepository popupReviewLikeRepository;

    @Mock
    private PopupReviewRepository popupReviewRepository;

    private PopupReviewLikeService popupReviewLikeService;

    @BeforeEach
    void setUp() {
        popupReviewLikeService = new PopupReviewLikeService(popupReviewLikeRepository, popupReviewRepository);
    }

    @Test
    void like_createsPopupReviewLikeAndIncreasesCount() {
        PopupReview review = PopupReview.create(
                3L,
                "reviewer-1",
                5,
                "좋아요",
                VerificationType.QR,
                "verification"
        );
        when(popupReviewRepository.findByIdWithLock(REVIEW_ID)).thenReturn(Optional.of(review));
        when(popupReviewLikeRepository.existsByPopupReviewIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(false);

        PopupReviewLikeResponse response = popupReviewLikeService.like(USER_ID, REVIEW_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(popupReviewLikeRepository).save(any(PopupReviewLike.class));
    }
}
