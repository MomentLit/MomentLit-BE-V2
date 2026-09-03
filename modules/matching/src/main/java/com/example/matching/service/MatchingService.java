package com.example.matching.service;

import com.example.matching.dto.request.MatchingCreateRequest;
import com.example.matching.dto.response.InternalMatchingResponse;
import com.example.matching.dto.response.MatchingCreateResponse;
import com.example.matching.dto.response.MatchingListResponse;
import com.example.matching.dto.response.MatchingSearchResponse;
import com.example.matching.api.MatchingInternalApi;
import com.example.matching.entity.Matching;
import com.example.matching.entity.MatchingStatus;
import com.example.matching.global.exception.BadRequestException;
import com.example.matching.global.exception.ForbiddenException;
import com.example.matching.global.exception.InvalidMatchingStateException;
import com.example.matching.global.exception.MatchingNotFoundException;
import com.example.matching.repository.MatchingRepository;
import com.example.space.api.SpaceInternalApi;
import com.example.space.dto.response.SpaceMatchingContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchingService implements MatchingInternalApi {

    private final MatchingRepository matchingRepository;
    private final SpaceInternalApi spaceApi;

    @Transactional
    public MatchingCreateResponse create(String userId, MatchingCreateRequest request) {
        LocalDateTime startTime = parseTime(request.startTime());
        LocalDateTime endTime = parseTime(request.endTime());

        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("종료 시간은 시작 시간 이후여야 함");
        }

        Integer totalPrice = parsePrice(request.totalPrice());

        SpaceMatchingContextResponse space =
                spaceApi.getMatchingContext(request.spaceId(), startTime, endTime);

        validateSpaceForMatching(userId, space);

        Matching matching = Matching.create(
                request.spaceId(),
                userId,
                space.hostId(),
                startTime,
                endTime,
                totalPrice
        );

        matchingRepository.save(matching);

        return MatchingCreateResponse.from(matching);
    }

    @Transactional(readOnly = true)
    public MatchingListResponse getReceivedMatchings(String userId) {
        return toListResponse(matchingRepository.findByHostIdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public MatchingListResponse getSentMatchings(String userId) {
        return toListResponse(matchingRepository.findBySellerIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalMatchingResponse getMatchingForInternal(Long matchingId) {
        return InternalMatchingResponse.from(getMatching(matchingId));
    }

    @Transactional
    public void approve(String userId, Long matchingId) {
        Matching matching = getMatching(matchingId);
        if (!matching.isHost(userId)) {
            throw new ForbiddenException("매칭 처리 권한이 없습니다.");
        }

        SpaceMatchingContextResponse space = spaceApi.getMatchingContext(
                matching.getSpaceId(),
                matching.getStartTime(),
                matching.getEndTime()
        );

        validateSpaceForMatching(matching.getSellerId(), space);
        if (!matching.getHostId().equals(space.hostId())) {
            throw new InvalidMatchingStateException("공간 소유자가 변경되어 매칭을 승인할 수 없습니다.");
        }

        List<Matching> spaceMatchings =
                matchingRepository.findAllBySpaceIdForUpdate(matching.getSpaceId());

        validateNoApprovedOverlap(matching, spaceMatchings);
        matching.approve(userId);
    }

    @Transactional
    public void reject(String userId, Long matchingId) {
        Matching matching = getMatching(matchingId);
        matching.reject(userId);
    }

    @Transactional
    public void cancel(String userId, Long matchingId) {
        Matching matching = getMatching(matchingId);
        matching.cancel(userId);
    }

    private MatchingListResponse toListResponse(List<Matching> matchings) {
        List<MatchingSearchResponse> responses = matchings.stream()
                .map(MatchingSearchResponse::from)
                .toList();

        return new MatchingListResponse(responses);
    }

    private Matching getMatching(Long matchingId) {
        return matchingRepository.findById(matchingId)
                .orElseThrow(() -> new MatchingNotFoundException("매칭 없음"));
    }

    private void validateSpaceForMatching(
            String sellerId,
            SpaceMatchingContextResponse space
    ) {
        if (!space.active()) {
            throw new InvalidMatchingStateException("비활성 공간에는 매칭을 요청할 수 없습니다.");
        }

        if (!space.approved()) {
            throw new InvalidMatchingStateException("승인되지 않은 공간에는 매칭을 요청할 수 없습니다.");
        }

        if (!space.available()) {
            throw new InvalidMatchingStateException("요청 시간이 공간의 예약 가능 일정에 포함되지 않습니다.");
        }

        if (sellerId.equals(space.hostId())) {
            throw new InvalidMatchingStateException("본인 공간에는 매칭을 요청할 수 없습니다.");
        }
    }

    private void validateNoApprovedOverlap(
            Matching target,
            List<Matching> spaceMatchings
    ) {
        boolean overlaps = spaceMatchings.stream()
                .filter(matching ->
                        matching != target
                                && (target.getId() == null
                                || !Objects.equals(matching.getId(), target.getId()))
                )
                .filter(matching -> matching.getStatus() == MatchingStatus.APPROVED)
                .anyMatch(matching ->
                        matching.getStartTime().isBefore(target.getEndTime())
                                && matching.getEndTime().isAfter(target.getStartTime())
                );

        if (overlaps) {
            throw new InvalidMatchingStateException("이미 승인된 매칭과 시간이 겹칩니다.");
        }
    }

    private LocalDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(value);
            } catch (Exception ignored) {
                throw new BadRequestException("시간 형식이 올바르지 않음");
            }
        }
    }

    private Integer parsePrice(String value) {
        try {
            Integer totalPrice = Integer.valueOf(value);
            if (totalPrice < 0) {
                throw new BadRequestException("총 금액은 음수일 수 없음");
            }
            return totalPrice;
        } catch (NumberFormatException e) {
            throw new BadRequestException("총 금액 형식이 올바르지 않음");
        }
    }
}
