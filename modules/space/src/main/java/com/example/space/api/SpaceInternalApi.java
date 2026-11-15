package com.example.space.api;

import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.response.AddressResponse;
import com.example.space.dto.response.AdminSpaceDetailResponse;
import com.example.space.dto.response.AdminSpaceListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceMatchingContextResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SpaceInternalApi {

    AdminSpaceListResponses getAdminSpaces(String role);

    AdminSpaceDetailResponse getAdminSpace(String role, Long spaceId);

    SpaceAdminStatusResponse getAdminStatus(String role, Long spaceId);

    void updateAdminStatus(String role, Long spaceId, SpaceAdminStatusUpdateRequest request);

    SpaceMatchingContextResponse getMatchingContext(Long spaceId, LocalDateTime startTime, LocalDateTime endTime);

    /** 예약 생성·승인을 공간 단위로 직렬화한다. 호출자의 트랜잭션에서 사용한다. */
    void lockSpaceForMatching(Long spaceId);

    SpaceDetailResponse getSpace(Long spaceId);

    /** 삭제되지 않은 공간인지 — 예외 없이 확인해야 할 때(호출자 트랜잭션을 rollback-only로 만들지 않도록) 쓴다. */
    boolean isActiveSpace(Long spaceId);

    /** 소프트 삭제된 공간 id 전체 — 공간이 지워진 팝업 등을 목록 조회 단계에서 빼는 데 쓴다. */
    Set<Long> getDeletedSpaceIds();

    /**
     * 삭제되지 않은 공간들의 주소를 한 번에 조회한다(공간 id → 주소).
     * 삭제됐거나 주소가 없는 공간은 결과에서 빠지며, 예외를 던지지 않는다.
     */
    Map<Long, AddressResponse> getActiveSpaceAddresses(Collection<Long> spaceIds);

    /** 매칭 승인 시 matching 모듈이 호출 — 그 날짜를 예약 완료로 기록해 날짜 검색에서 걸러지게 한다. */
    void markSpaceBooked(Long spaceId, LocalDate date, Long matchingId);
}
