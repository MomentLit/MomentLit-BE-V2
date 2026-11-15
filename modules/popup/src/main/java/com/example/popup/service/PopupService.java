package com.example.popup.service;

import com.example.common.dto.PageResponse;
import com.example.matching.api.MatchingInternalApi;
import com.example.matching.dto.response.InternalMatchingResponse;
import com.example.matching.entity.MatchingStatus;
import com.example.popup.dto.request.PopupCreateRequest;
import com.example.popup.dto.response.*;
import com.example.popup.entity.Popup;
import com.example.popup.global.exception.BadRequestException;
import com.example.popup.global.exception.ForbiddenException;
import com.example.popup.global.exception.InvalidPopupStateException;
import com.example.popup.global.exception.PopupNotFoundException;
import com.example.popup.repository.PopupRepository;
import com.example.space.api.SpaceInternalApi;
import com.example.space.dto.response.AddressResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupService {

    private final PopupRepository popupRepository;
    private final MatchingInternalApi matchingApi;
    private final SpaceInternalApi spaceApi;

    @Transactional
    public PopupCreateResponse create(
            String userId,
            PopupCreateRequest request
    ) {
        validateCreateRequest(request);

        LocalDateTime startTime = parseTime(request.startTime());
        LocalDateTime endTime = parseTime(request.endTime());
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        if (popupRepository.existsByMatchingId(request.matchingId())) {
            throw new InvalidPopupStateException("이미 팝업이 생성된 매칭입니다.");
        }

        InternalMatchingResponse matching = matchingApi.getMatchingForInternal(request.matchingId());
        validateMatching(userId, matching, startTime, endTime);

        Popup popup = Popup.create(
                matching.matchingId(),
                matching.spaceId(),
                matching.sellerId(),
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                startTime,
                endTime
        );

        Popup savedPopup = popupRepository.save(popup);

        return PopupCreateResponse.from(savedPopup);
    }

    /**
     * 공간이 (소프트) 삭제된 팝업은 목록에서 뺀다. 예전에는 팝업마다 공간 상세를 조회해서,
     * 삭제된 공간에 딸린 팝업이 한 건만 있어도 SpaceNotFoundException으로 목록 전체가 404가 됐다.
     */
    public PageResponse<PopupListResponse> getPopups(Pageable pageable) {
        Set<Long> deletedSpaceIds = spaceApi.getDeletedSpaceIds();
        Page<Popup> popups = deletedSpaceIds.isEmpty()
                ? popupRepository.findAll(pageable)
                : popupRepository.findAllBySpaceIdNotIn(deletedSpaceIds, pageable);
        Map<Long, AddressResponse> addresses = getAddresses(popups.getContent());

        return PageResponse.from(
                popups,
                popup -> PopupListResponse.from(popup, addresses.get(popup.getSpaceId()))
        );
    }

    public PopupListResponses getRecommendations() {
        Set<Long> deletedSpaceIds = spaceApi.getDeletedSpaceIds();
        return toListResponses(deletedSpaceIds.isEmpty()
                ? popupRepository.findTop10ByOrderByLikeCountDescViewCountDescCreatedAtDesc()
                : popupRepository.findTop10BySpaceIdNotInOrderByLikeCountDescViewCountDescCreatedAtDesc(deletedSpaceIds));
    }

    public PopupListResponses getMyPopups(String userId) {
        return toListResponses(popupRepository.findAllBySellerIdOrderByCreatedAtDesc(userId));
    }

    @Transactional
    public PopupDetailResponse getPopup(Long popupId) {
        Popup popup = getPopupEntity(popupId);
        // 공간이 삭제된 팝업은 없는 팝업으로 본다. getSpace의 예외에 맡기면 "공간을 찾을 수 없습니다"가
        // 나가고 이 트랜잭션도 rollback-only가 되므로, 조회수를 올리기 전에 예외 없이 먼저 확인한다.
        if (!spaceApi.isActiveSpace(popup.getSpaceId())) {
            throw new PopupNotFoundException("팝업을 찾을 수 없습니다.");
        }
        popup.increaseViewCount();

        SpaceDetailResponse space = spaceApi.getSpace(popup.getSpaceId());

        return PopupDetailResponse.from(popup, space);
    }

    public PopupHistoryResponses getSpacePopupHistories(Long spaceId) {
        List<PopupHistoryResponse> popups = popupRepository.findAllBySpaceIdOrderByCreatedAtDesc(spaceId)
                .stream()
                .map(PopupHistoryResponse::from)
                .toList();

        return new PopupHistoryResponses(popups);
    }

    public PopupListResponses getSellerPopupHistories(String sellerId) {
        return toListResponses(popupRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId));
    }

    /** 주소를 한 번에 조회하고, 그 사이 공간이 삭제돼 주소가 없는 팝업은 건너뛴다. */
    private PopupListResponses toListResponses(List<Popup> popups) {
        Map<Long, AddressResponse> addresses = getAddresses(popups);
        List<PopupListResponse> responses = popups.stream()
                .filter(popup -> addresses.containsKey(popup.getSpaceId()))
                .map(popup -> PopupListResponse.from(popup, addresses.get(popup.getSpaceId())))
                .toList();

        return new PopupListResponses(responses);
    }

    private Map<Long, AddressResponse> getAddresses(List<Popup> popups) {
        return spaceApi.getActiveSpaceAddresses(
                popups.stream().map(Popup::getSpaceId).filter(Objects::nonNull).distinct().toList()
        );
    }

    private Popup getPopupEntity(Long popupId) {
        return popupRepository.findById(popupId)
                .orElseThrow(() -> new PopupNotFoundException("팝업을 찾을 수 없습니다."));
    }

    private void validateCreateRequest(PopupCreateRequest request) {
        if (request.matchingId() == null) {
            throw new BadRequestException("매칭 ID는 필수입니다.");
        }
        if (isBlank(request.title())) {
            throw new BadRequestException("팝업 제목은 필수입니다.");
        }
        if (isBlank(request.thumbnailUrl())) {
            throw new BadRequestException("대표 이미지는 필수입니다.");
        }
        if (isBlank(request.startTime()) || isBlank(request.endTime())) {
            throw new BadRequestException("팝업 시작/종료 시간은 필수입니다.");
        }
    }

    private void validateMatching(
            String userId,
            InternalMatchingResponse matching,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (!userId.equals(matching.sellerId())) {
            throw new ForbiddenException("팝업 생성 권한이 없습니다.");
        }

        if (matching.status() != MatchingStatus.APPROVED) {
            throw new InvalidPopupStateException("승인된 매칭에만 팝업을 생성할 수 있습니다.");
        }

        if (startTime.isBefore(matching.startTime()) || endTime.isAfter(matching.endTime())) {
            throw new InvalidPopupStateException("팝업 운영 시간은 매칭 시간 안에 있어야 합니다.");
        }
    }

    private LocalDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception ignored) {
                throw new BadRequestException("시간 형식이 올바르지 않습니다.");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
