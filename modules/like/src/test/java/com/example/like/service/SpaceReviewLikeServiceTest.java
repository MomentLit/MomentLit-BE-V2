package com.example.like.service;

import com.example.like.dto.response.SpaceReviewLikeResponse;
import com.example.like.entity.SpaceReviewLike;
import com.example.like.global.exception.DuplicateReviewLikeException;
import com.example.like.repository.SpaceReviewLikeRepository;
import com.example.review.entity.SpaceReview;
import com.example.review.repository.SpaceReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceReviewLikeServiceTest {

    private static final Long REVIEW_ID = 1L;
    private static final String USER_ID = "user-1";

    @Mock
    private SpaceReviewLikeRepository spaceReviewLikeRepository;

    @Mock
    private SpaceReviewRepository spaceReviewRepository;

    private SpaceReviewLikeService spaceReviewLikeService;

    @BeforeEach
    void setUp() {
        spaceReviewLikeService = new SpaceReviewLikeService(spaceReviewLikeRepository, spaceReviewRepository);
    }

    @Test
    void like_createsSpaceReviewLikeAndIncreasesCount() {
        SpaceReview review = SpaceReview.create(2L, 3L, "seller-1", 5, "좋아요");
        when(spaceReviewRepository.findByIdWithLock(REVIEW_ID)).thenReturn(Optional.of(review));
        when(spaceReviewLikeRepository.existsBySpaceReviewIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(false);

        SpaceReviewLikeResponse response = spaceReviewLikeService.like(USER_ID, REVIEW_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(spaceReviewLikeRepository).save(any(SpaceReviewLike.class));
    }

    @Test
    void like_rejectsDuplicateLike() {
        when(spaceReviewRepository.findByIdWithLock(REVIEW_ID))
                .thenReturn(Optional.of(SpaceReview.create(2L, 3L, "seller-1", 5, "좋아요")));
        when(spaceReviewLikeRepository.existsBySpaceReviewIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> spaceReviewLikeService.like(USER_ID, REVIEW_ID))
                .isInstanceOf(DuplicateReviewLikeException.class);
    }
}
