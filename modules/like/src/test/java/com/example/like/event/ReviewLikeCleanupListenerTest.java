package com.example.like.event;

import com.example.common.event.ReviewDeletedEvent;
import com.example.like.repository.PopupReviewLikeRepository;
import com.example.like.repository.SpaceReviewLikeRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReviewLikeCleanupListenerTest {

    @Test
    void deleteReviewLikes_removesLikesFromTheMatchingTable() {
        SpaceReviewLikeRepository spaceReviewLikeRepository = mock(SpaceReviewLikeRepository.class);
        PopupReviewLikeRepository popupReviewLikeRepository = mock(PopupReviewLikeRepository.class);
        ReviewLikeCleanupListener listener = new ReviewLikeCleanupListener(
                spaceReviewLikeRepository,
                popupReviewLikeRepository
        );

        listener.deleteReviewLikes(new ReviewDeletedEvent("POPUP", 3L));

        verify(popupReviewLikeRepository).deleteAllByPopupReviewId(3L);
    }
}
