package com.example.like.event;

import com.example.common.event.ReviewDeletedEvent;
import com.example.like.repository.PopupReviewLikeRepository;
import com.example.like.repository.SpaceReviewLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewLikeCleanupListener {

    private final SpaceReviewLikeRepository spaceReviewLikeRepository;
    private final PopupReviewLikeRepository popupReviewLikeRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void deleteReviewLikes(ReviewDeletedEvent event) {
        if ("SPACE".equals(event.reviewType())) {
            spaceReviewLikeRepository.deleteAllBySpaceReviewId(event.reviewId());
            return;
        }

        if ("POPUP".equals(event.reviewType())) {
            popupReviewLikeRepository.deleteAllByPopupReviewId(event.reviewId());
        }
    }
}
