package com.example.review.service;

import com.example.common.event.ReviewDeletedEvent;
import com.example.matching.entity.Matching;
import com.example.matching.entity.MatchingStatus;
import com.example.matching.repository.MatchingRepository;
import com.example.popup.entity.Popup;
import com.example.popup.repository.PopupRepository;
import com.example.review.dto.request.PopupReviewCreateRequest;
import com.example.review.dto.request.PopupReviewUpdateRequest;
import com.example.review.dto.request.SpaceReviewCreateRequest;
import com.example.review.dto.request.SpaceReviewUpdateRequest;
import com.example.review.dto.response.PopupReviewResponse;
import com.example.review.dto.response.PopupReviewResponses;
import com.example.review.dto.response.PopupReviewCreateResponse;
import com.example.review.dto.response.SpaceReviewCreateResponse;
import com.example.review.dto.response.SpaceReviewResponse;
import com.example.review.dto.response.SpaceReviewResponses;
import com.example.review.entity.PopupReview;
import com.example.review.entity.SpaceReview;
import com.example.review.global.exception.BadRequestException;
import com.example.review.global.exception.DuplicateReviewException;
import com.example.review.global.exception.ForbiddenException;
import com.example.review.global.exception.ReviewNotFoundException;
import com.example.review.repository.PopupReviewRepository;
import com.example.review.repository.SpaceReviewRepository;
import com.example.user.api.UserInternalApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final SpaceReviewRepository spaceReviewRepository;
    private final PopupReviewRepository popupReviewRepository;
    private final MatchingRepository matchingRepository;
    private final PopupRepository popupRepository;
    private final UserInternalApi userApi;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SpaceReviewCreateResponse createSpaceReview(
            String userId,
            Long matchingId,
            SpaceReviewCreateRequest request
    ) {
        validateRatingAndContent(request.rating(), request.content());

        if (spaceReviewRepository.existsByMatchingId(matchingId)) {
            throw new DuplicateReviewException("매칭당 공간 리뷰는 한 건만 작성할 수 있습니다.");
        }

        Matching matching = getMatching(matchingId);
        if (!matching.getSellerId().equals(userId)) {
            throw new ForbiddenException("공간 리뷰 작성 권한이 없습니다.");
        }
        if (matching.getStatus() != MatchingStatus.APPROVED) {
            throw new BadRequestException("승인된 매칭에만 공간 리뷰를 작성할 수 있습니다.");
        }

        SpaceReview review = spaceReviewRepository.save(SpaceReview.create(
                matchingId,
                matching.getSpaceId(),
                userId,
                request.rating(),
                request.content()
        ));
        return SpaceReviewCreateResponse.from(review);
    }

    public SpaceReviewResponses getSpaceReviews(Long spaceId) {
        return new SpaceReviewResponses(
                spaceReviewRepository.findAllBySpaceIdOrderByCreatedAtDesc(spaceId)
                        .stream()
                        .map(review -> SpaceReviewResponse.from(
                                review,
                                userApi.getUserName(review.getUserId()).name()
                        ))
                        .toList()
        );
    }

    @Transactional
    public void updateSpaceReview(String userId, Long reviewId, SpaceReviewUpdateRequest request) {
        SpaceReview review = getSpaceReview(reviewId);
        validateWriter(review.isWrittenBy(userId));
        validateContent(request.content());
        review.update(request.content());
    }

    @Transactional
    public void deleteSpaceReview(String userId, Long reviewId) {
        SpaceReview review = getSpaceReview(reviewId);
        validateWriter(review.isWrittenBy(userId));
        spaceReviewRepository.delete(review);
        eventPublisher.publishEvent(new ReviewDeletedEvent("SPACE", reviewId));
    }

    @Transactional
    public PopupReviewCreateResponse createPopupReview(
            String userId,
            Long popupId,
            PopupReviewCreateRequest request
    ) {
        validateRatingAndContent(request.rating(), request.content());
        if (request.verificationType() == null || isBlank(request.verificationPayload())) {
            throw new BadRequestException("리뷰 인증 정보는 필수입니다.");
        }
        if (popupReviewRepository.existsByPopupIdAndUserId(popupId, userId)) {
            throw new DuplicateReviewException("팝업당 리뷰는 한 건만 작성할 수 있습니다.");
        }

        Popup popup = getPopup(popupId);
        if (popup.getSellerId().equals(userId)) {
            throw new ForbiddenException("본인이 등록한 팝업에는 리뷰를 작성할 수 없습니다.");
        }

        PopupReview review = popupReviewRepository.save(PopupReview.create(
                popupId,
                userId,
                request.rating(),
                request.content(),
                request.verificationType(),
                request.verificationPayload()
        ));
        return PopupReviewCreateResponse.from(review);
    }

    public PopupReviewResponses getPopupReviews(Long popupId) {
        getPopup(popupId);
        return new PopupReviewResponses(
                popupReviewRepository.findAllByPopupIdOrderByCreatedAtDesc(popupId)
                        .stream()
                        .map(review -> PopupReviewResponse.from(
                                review,
                                userApi.getUserName(review.getUserId()).name()
                        ))
                        .toList()
        );
    }

    @Transactional
    public void updatePopupReview(String userId, Long reviewId, PopupReviewUpdateRequest request) {
        PopupReview review = getPopupReview(reviewId);
        validateWriter(review.isWrittenBy(userId));
        validateContent(request.content());
        review.update(request.content());
    }

    @Transactional
    public void deletePopupReview(String userId, Long reviewId) {
        PopupReview review = getPopupReview(reviewId);
        validateWriter(review.isWrittenBy(userId));
        popupReviewRepository.delete(review);
        eventPublisher.publishEvent(new ReviewDeletedEvent("POPUP", reviewId));
    }

    private Matching getMatching(Long matchingId) {
        return matchingRepository.findById(matchingId)
                .orElseThrow(() -> new BadRequestException("매칭을 찾을 수 없습니다."));
    }

    private Popup getPopup(Long popupId) {
        return popupRepository.findById(popupId)
                .orElseThrow(() -> new BadRequestException("팝업을 찾을 수 없습니다."));
    }

    private SpaceReview getSpaceReview(Long reviewId) {
        return spaceReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("공간 리뷰를 찾을 수 없습니다."));
    }

    private PopupReview getPopupReview(Long reviewId) {
        return popupReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("팝업 리뷰를 찾을 수 없습니다."));
    }

    private void validateRatingAndContent(Integer rating, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("평점은 1점에서 5점 사이여야 합니다.");
        }
        validateContent(content);
    }

    private void validateContent(String content) {
        if (isBlank(content)) {
            throw new BadRequestException("리뷰 내용은 필수입니다.");
        }
    }

    private void validateWriter(boolean isWriter) {
        if (!isWriter) {
            throw new ForbiddenException("리뷰 수정 또는 삭제 권한이 없습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
