package com.example.space.service;

import com.example.common.dto.PageResponse;
import com.example.common.security.Role;
import com.example.space.api.SpaceInternalApi;
import com.example.space.dto.request.ScheduleCreateRequest;
import com.example.space.dto.request.ScheduleUpdateRequest;
import com.example.space.dto.request.SpaceAdminStatusUpdateRequest;
import com.example.space.dto.request.SpaceCreateRequest;
import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.dto.request.AddressRequest;
import com.example.space.dto.response.AddressResponse;
import com.example.space.dto.response.AdminSpaceDetailResponse;
import com.example.space.dto.response.AdminSpaceListResponses;
import com.example.space.dto.response.MySpaceListResponse;
import com.example.space.dto.response.ScheduleCreateResponse;
import com.example.space.dto.response.ScheduleListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceCategoryCountResponse;
import com.example.space.dto.response.SpaceCreateResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceListResponse;
import com.example.space.dto.response.SpaceAvailabilityListResponses;
import com.example.space.dto.response.SpaceBookedDatesResponse;
import com.example.space.dto.response.SpaceMatchingContextResponse;
import com.example.space.dto.response.SpaceRegionCountResponse;
import com.example.space.dto.request.SpaceAvailabilitySlotRequest;
import com.example.space.entity.Address;
import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Region;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceAvailability;
import com.example.space.entity.SpaceBookedDate;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.SpaceImage;
import com.example.space.entity.SpaceSchedule;
import com.example.space.entity.UsageUnit;
import com.example.space.global.client.ChatbotSyncClient;
import com.example.space.global.exception.BadRequestException;
import com.example.space.global.exception.ForbiddenException;
import com.example.space.global.exception.ScheduleNotFoundException;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.SpaceAvailabilityRepository;
import com.example.space.repository.SpaceBookedDateRepository;
import com.example.space.repository.SpaceImageRepository;
import com.example.space.repository.SpaceRepository;
import com.example.space.repository.SpaceScheduleRepository;
import com.example.user.api.UserInternalApi;
import com.example.user.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService implements SpaceInternalApi {

    private static final Set<String> ALLOWED_SPACE_SORT_PROPERTIES = Set.of("createdAt", "likeCount");

    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final SpaceScheduleRepository spaceScheduleRepository;
    private final SpaceAvailabilityRepository spaceAvailabilityRepository;
    private final SpaceBookedDateRepository spaceBookedDateRepository;
    private final AddressRepository addressRepository;
    private final UserInternalApi userApi;
    private final ChatbotSyncClient chatbotSyncClient;

    @Transactional
    public SpaceCreateResponse createSpace(
            String hostId,
            SpaceCreateRequest request
    ) {
        Address address = addressRepository.save(createAddress(request.address()));

        Space space = Space.create(
                hostId,
                request.name(),
                request.description(),
                null,
                address.getId(),
                request.thumbnailUrl(),
                request.pricePerHour(),
                request.category(),
                request.phone(),
                request.area(),
                request.capacity(),
                request.floor(),
                request.parkingInfo(),
                request.usageUnit(),
                request.isDraft()
        );

        Space savedSpace = spaceRepository.save(space);

        saveImages(savedSpace.getId(), request.imageUrls());

        syncToChatbotAfterCommit(savedSpace.getId());

        return SpaceCreateResponse.from(savedSpace);
    }

    public PageResponse<SpaceListResponse> getSpaces(
            String name,
            SpaceCategory category,
            Region region,
            UsageUnit usageUnit,
            Integer minCapacity,
            Integer maxCapacity,
            LocalDate date,
            Double lat,
            Double lng,
            Pageable pageable
    ) {
        DayOfWeek dayOfWeek = date != null ? date.getDayOfWeek() : null;

        Page<Space> spaces = (lat != null && lng != null)
                ? spaceRepository.searchSpacesByDistance(
                        toLikePattern(name),
                        category,
                        region,
                        usageUnit,
                        minCapacity,
                        maxCapacity,
                        dayOfWeek,
                        date != null,
                        date,
                        lat,
                        lng,
                        pageable
                )
                : spaceRepository.searchSpaces(
                        toLikePattern(name),
                        category,
                        region,
                        usageUnit,
                        minCapacity,
                        maxCapacity,
                        dayOfWeek,
                        date != null,
                        date,
                        sanitizeSpaceSort(pageable)
                );

        Map<Long, Address> addresses = findAddressesBySpaces(spaces.getContent());

        return PageResponse.from(
                spaces,
                space -> SpaceListResponse.from(space, AddressResponse.from(addresses.get(space.getAddressId())))
        );
    }

    @Override
    public SpaceDetailResponse getSpace(
            Long spaceId
    ) {
        Space space = getActiveSpace(spaceId);
        Address address = getAddress(space.getAddressId());
        List<SpaceImage> images = spaceImageRepository.findAllBySpaceId(spaceId);
        UserProfileResponse host = userApi.getUserProfile(space.getHostId());

        return SpaceDetailResponse.from(space, AddressResponse.from(address), images, host.name(), host.imageUrl());
    }

    @Transactional
    public void updateSpace(
            String userId,
            Long spaceId,
            SpaceUpdateRequest request
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        if (request.address() != null) {
            Address address = getAddress(space.getAddressId());
            updateAddress(address, request.address());
        }

        space.update(
                request.name(),
                request.description(),
                request.aiSummary(),
                request.thumbnailUrl(),
                request.pricePerHour(),
                request.category(),
                request.phone(),
                request.area(),
                request.capacity(),
                request.floor(),
                request.parkingInfo(),
                request.usageUnit()
        );

        space.applyDraftTransition(request.isDraft());

        if (request.imageUrls() != null) {
            spaceImageRepository.deleteAllBySpaceId(spaceId);
            saveImages(spaceId, request.imageUrls());
        }

        syncToChatbotAfterCommit(spaceId);
    }

    @Transactional
    public void deleteSpace(
            String userId,
            Long spaceId
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        space.delete();

        spaceImageRepository.deleteAllBySpaceId(spaceId);
        spaceScheduleRepository.deleteAllBySpaceId(spaceId);

        syncToChatbotAfterCommit(spaceId);
    }

    public PageResponse<MySpaceListResponse> getMySpaces(
            String hostId,
            String name,
            SpaceCategory category,
            Region region,
            UsageUnit usageUnit,
            Integer minCapacity,
            Integer maxCapacity,
            Pageable pageable
    ) {
        Page<Space> spaces = spaceRepository.searchMySpaces(
                hostId,
                toLikePattern(name),
                category,
                region,
                usageUnit,
                minCapacity,
                maxCapacity,
                sanitizeSpaceSort(pageable)
        );

        Map<Long, Address> addresses = findAddressesBySpaces(spaces.getContent());

        return PageResponse.from(
                spaces,
                space -> MySpaceListResponse.from(space, AddressResponse.from(addresses.get(space.getAddressId())))
        );
    }

    public List<SpaceCategoryCountResponse> getCountsByCategory() {
        return spaceRepository.countActiveSpacesByCategory(ApprovalStatus.APPROVED).stream()
                .map(projection -> new SpaceCategoryCountResponse(
                        projection.getCategory().name(),
                        projection.getCount()
                ))
                .toList();
    }

    public List<SpaceRegionCountResponse> getCountsByRegion() {
        return spaceRepository.countActiveSpacesByRegion(ApprovalStatus.APPROVED.name()).stream()
                .map(projection -> new SpaceRegionCountResponse(
                        projection.getRegion(),
                        projection.getCount()
                ))
                .toList();
    }

    @Transactional
    public ScheduleCreateResponse createSchedule(
            String userId,
            Long spaceId,
            ScheduleCreateRequest request
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        SpaceSchedule schedule = SpaceSchedule.create(
                spaceId,
                request.startTime(),
                request.endTime(),
                true
        );

        SpaceSchedule savedSchedule = spaceScheduleRepository.save(schedule);

        return ScheduleCreateResponse.from(savedSchedule);
    }

    public ScheduleListResponses getSchedules(
            Long spaceId
    ) {
        getActiveSpace(spaceId);

        List<SpaceSchedule> schedules =
                spaceScheduleRepository.findAllBySpaceIdOrderByStartTimeAsc(spaceId);

        return ScheduleListResponses.from(schedules);
    }

    @Override
    public SpaceMatchingContextResponse getMatchingContext(
            Long spaceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BadRequestException("매칭 요청 시간이 올바르지 않습니다.");
        }

        Space space = getActiveSpace(spaceId);
        boolean available =
                spaceScheduleRepository
                        .existsBySpaceIdAndIsBookableTrueAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                                spaceId,
                                startTime,
                                endTime
                        );

        // SpaceSchedule(날짜별 실제 예약 인스턴스)에 아직 해당 날짜 슬롯이 없어도,
        // 요청 구간이 하루 안에 들어오고 호스트가 열어둔 주간 반복 가용시간
        // (SpaceAvailability)에 포함되면 예약을 요청할 수 있게 한다. 두 모델을
        // 자동으로 맞물리게 하는 배치는 범위 밖이라 이 보조 체크로 대체한다.
        if (!available && startTime.toLocalDate().equals(endTime.toLocalDate())) {
            available = spaceAvailabilityRepository
                    .existsBySpaceIdAndDayOfWeekAndIsOpenTrueAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                            spaceId,
                            startTime.getDayOfWeek(),
                            startTime.toLocalTime(),
                            endTime.toLocalTime()
                    );
        }

        return new SpaceMatchingContextResponse(
                space.getId(),
                space.getHostId(),
                space.getAdminStatus() == ApprovalStatus.APPROVED,
                Boolean.TRUE.equals(space.getIsActive()),
                available,
                space.getCapacity()
        );
    }

    @Override
    public AdminSpaceListResponses getAdminSpaces(
            String role
    ) {
        validateAdmin(role);

        List<Space> spaces = spaceRepository.findAllByDeletedAtIsNull();

        return AdminSpaceListResponses.from(spaces, findAddressesBySpaces(spaces));
    }

    @Override
    public AdminSpaceDetailResponse getAdminSpace(
            String role,
            Long spaceId
    ) {
        validateAdmin(role);

        Space space = getActiveSpace(spaceId);
        Address address = getAddress(space.getAddressId());
        List<SpaceImage> images = spaceImageRepository.findAllBySpaceId(spaceId);

        return AdminSpaceDetailResponse.from(space, AddressResponse.from(address), images);
    }

    @Override
    @Transactional
    public void markSpaceBooked(Long spaceId, LocalDate date, Long matchingId) {
        if (spaceBookedDateRepository.existsBySpaceIdAndDate(spaceId, date)) {
            return;
        }
        spaceBookedDateRepository.save(new SpaceBookedDate(spaceId, date, matchingId));
    }

    @Override
    public SpaceAdminStatusResponse getAdminStatus(
            String role,
            Long spaceId
    ) {
        validateAdmin(role);

        Space space = getActiveSpace(spaceId);

        return SpaceAdminStatusResponse.from(space);
    }

    @Override
    @Transactional
    public void updateAdminStatus(
            String role,
            Long spaceId,
            SpaceAdminStatusUpdateRequest request
    ) {
        validateAdmin(role);

        if (request.adminStatus() == null) {
            throw new BadRequestException("승인 상태는 필수입니다.");
        }

        Space space = getActiveSpace(spaceId);

        if (space.getAdminStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("승인 대기 상태의 공간만 승인 상태를 변경할 수 있습니다.");
        }

        space.updateAdminStatus(request.adminStatus());

        syncToChatbotAfterCommit(spaceId);
    }

    @Transactional
    public void updateSchedule(
            String userId,
            Long spaceId,
            Long scheduleId,
            ScheduleUpdateRequest request
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        SpaceSchedule schedule = spaceScheduleRepository.findByIdAndSpaceId(scheduleId, spaceId)
                .orElseThrow(() -> new ScheduleNotFoundException("일정을 찾을 수 없습니다."));

        LocalDateTime startTime = request.startTime() != null
                ? request.startTime()
                : schedule.getStartTime();

        LocalDateTime endTime = request.endTime() != null
                ? request.endTime()
                : schedule.getEndTime();

        Boolean isBookable = request.isBookable() != null
                ? request.isBookable()
                : schedule.getIsBookable();

        schedule.update(
                startTime,
                endTime,
                isBookable
        );
    }

    @Transactional
    public void deleteSchedule(
            String userId,
            Long spaceId,
            Long scheduleId
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        SpaceSchedule schedule = spaceScheduleRepository.findByIdAndSpaceId(scheduleId, spaceId)
                .orElseThrow(() -> new ScheduleNotFoundException("일정을 찾을 수 없습니다."));

        spaceScheduleRepository.delete(schedule);
    }

    /**
     * 요일×시간대 반복 가용시간 템플릿을 통째로 덮어쓴다.
     * new_FE의 ScheduleGrid가 7일×3구간 전체 상태를 한번에 보내는 방식과 대응된다.
     */
    @Transactional
    public void updateAvailability(
            String userId,
            Long spaceId,
            List<SpaceAvailabilitySlotRequest> requests
    ) {
        Space space = getActiveSpace(spaceId);
        validateOwner(space, userId);

        if (requests == null) {
            throw new BadRequestException("가용시간 목록은 필수입니다.");
        }

        List<SpaceAvailability> availabilities = requests.stream()
                .map(request -> SpaceAvailability.create(
                        spaceId,
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime(),
                        request.isOpen()
                ))
                .toList();

        spaceAvailabilityRepository.deleteAllBySpaceId(spaceId);
        spaceAvailabilityRepository.saveAll(availabilities);
    }

    public SpaceAvailabilityListResponses getAvailability(
            Long spaceId
    ) {
        getActiveSpace(spaceId);

        List<SpaceAvailability> availabilities =
                spaceAvailabilityRepository.findAllBySpaceIdOrderByDayOfWeekAscStartTimeAsc(spaceId);

        return SpaceAvailabilityListResponses.from(availabilities);
    }

    /** 오늘 이후로 이미 승인된 예약이 찬 날짜 목록 — 공간 상세의 날짜 선택기가 미리 막아둘 수 있도록. */
    public SpaceBookedDatesResponse getBookedDates(Long spaceId) {
        getActiveSpace(spaceId);

        List<SpaceBookedDate> bookedDates =
                spaceBookedDateRepository.findAllBySpaceIdAndDateGreaterThanEqual(spaceId, LocalDate.now());

        return SpaceBookedDatesResponse.from(bookedDates);
    }

    private void syncToChatbotAfterCommit(Long spaceId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatbotSyncClient.syncSpace(spaceId);
            }
        });
    }

    private Space getActiveSpace(Long spaceId) {
        return spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("공간을 찾을 수 없습니다."));
    }

    private Address getAddress(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new SpaceNotFoundException("공간 주소를 찾을 수 없습니다."));
    }

    private Map<Long, Address> findAddressesBySpaces(List<Space> spaces) {
        List<Long> addressIds = spaces.stream()
                .map(Space::getAddressId)
                .distinct()
                .toList();

        Map<Long, Address> addresses = addressRepository.findAllById(addressIds).stream()
                .collect(Collectors.toMap(Address::getId, Function.identity()));

        if (addresses.size() != addressIds.size()) {
            throw new SpaceNotFoundException("공간 주소를 찾을 수 없습니다.");
        }

        return addresses;
    }

    private Address createAddress(AddressRequest request) {
        if (request == null) {
            throw new BadRequestException("공간 주소는 필수입니다.");
        }
        validateAddressRequiredForCreate(request);

        return Address.create(
                request.sido(),
                request.sigungu(),
                request.eupMyeonDong(),
                request.roadAddress(),
                request.jibunAddress(),
                request.detailAddress(),
                request.postalCode()
        );
    }

    private void updateAddress(
            Address address,
            AddressRequest request
    ) {
        validateAddressRequiredFieldsNotBlankForUpdate(request);

        address.update(
                request.sido(),
                request.sigungu(),
                request.eupMyeonDong(),
                request.roadAddress(),
                request.jibunAddress(),
                request.detailAddress(),
                request.postalCode()
        );
    }

    private void validateAddressRequiredForCreate(AddressRequest request) {
        if (isBlank(request.sido())) {
            throw new BadRequestException("시/도는 필수입니다.");
        }
        if (isBlank(request.sigungu())) {
            throw new BadRequestException("시/군/구는 필수입니다.");
        }
        if (isBlank(request.roadAddress())) {
            throw new BadRequestException("도로명 주소는 필수입니다.");
        }
    }

    private void validateAddressRequiredFieldsNotBlankForUpdate(AddressRequest request) {
        if (request.sido() != null && request.sido().isBlank()) {
            throw new BadRequestException("시/도는 비어 있을 수 없습니다.");
        }
        if (request.sigungu() != null && request.sigungu().isBlank()) {
            throw new BadRequestException("시/군/구는 비어 있을 수 없습니다.");
        }
        if (request.roadAddress() != null && request.roadAddress().isBlank()) {
            throw new BadRequestException("도로명 주소는 비어 있을 수 없습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateOwner(
            Space space,
            String userId
    ) {
        if (!space.isOwner(userId)) {
            throw new ForbiddenException("해당 공간에 대한 권한이 없습니다.");
        }
    }

    private void validateAdmin(String role) {
        if (!Role.ADMIN.name().equals(role)) {
            throw new ForbiddenException("관리자 권한이 없습니다.");
        }
    }

    private void saveImages(
            Long spaceId,
            List<String> imageUrls
    ) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<SpaceImage> images = imageUrls.stream()
                .map(imageUrl -> SpaceImage.create(spaceId, imageUrl))
                .toList();

        spaceImageRepository.saveAll(images);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * JPQL의 {@code CONCAT('%', :name, '%')}로 LIKE 패턴을 만들면, PostgreSQL이 바인드
     * 파라미터 타입을 추론하다 varchar 대신 bytea로 잘못 잡아 "operator does not exist:
     * character varying ~~ bytea" 에러가 난다(:name이 null일 때 특히 재현됨). 패턴 조합을
     * 애플리케이션 레이어에서 미리 끝내고 완성된 문자열을 그대로 LIKE에 바인딩하면 이 문제를 피할 수 있다.
     */
    private String toLikePattern(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : "%" + trimmed + "%";
    }

    /**
     * Space와 Address는 JPA 연관관계 없이 콤마 조인(theta join)으로 조회되므로,
     * 둘 다 갖고 있는 컬럼명(예: createdAt)으로 그냥 정렬을 위임하면 SQL이 모호(ambiguous)해질 수 있다.
     * 따라서 허용된 정렬 속성만 화이트리스트로 걸러 "s." 접두사를 붙인 JpaSort.unsafe로 안전하게 변환한다.
     * (좌표 데이터가 없어 거리순 정렬은 미지원)
     */
    private Pageable sanitizeSpaceSort(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));

        String property = ALLOWED_SPACE_SORT_PROPERTIES.contains(order.getProperty())
                ? order.getProperty()
                : "createdAt";

        Sort safeSort = JpaSort.unsafe(order.getDirection(), "s." + property);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }
}
