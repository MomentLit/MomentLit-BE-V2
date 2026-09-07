package com.example.space.service;

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
import com.example.space.dto.response.MySpaceListResponses;
import com.example.space.dto.response.ScheduleCreateResponse;
import com.example.space.dto.response.ScheduleListResponses;
import com.example.space.dto.response.SpaceAdminStatusResponse;
import com.example.space.dto.response.SpaceCreateResponse;
import com.example.space.dto.response.SpaceDetailResponse;
import com.example.space.dto.response.SpaceListResponses;
import com.example.space.dto.response.SpaceMatchingContextResponse;
import com.example.space.entity.Address;
import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.SpaceImage;
import com.example.space.entity.SpaceSchedule;
import com.example.space.global.client.ChatbotSyncClient;
import com.example.space.global.exception.BadRequestException;
import com.example.space.global.exception.ForbiddenException;
import com.example.space.global.exception.ScheduleNotFoundException;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.SpaceImageRepository;
import com.example.space.repository.SpaceRepository;
import com.example.space.repository.SpaceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService implements SpaceInternalApi {

    private final SpaceRepository spaceRepository;
    private final SpaceImageRepository spaceImageRepository;
    private final SpaceScheduleRepository spaceScheduleRepository;
    private final AddressRepository addressRepository;
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
                request.phone()
        );

        Space savedSpace = spaceRepository.save(space);

        saveImages(savedSpace.getId(), request.imageUrls());

        syncToChatbotAfterCommit(savedSpace.getId());

        return SpaceCreateResponse.from(savedSpace);
    }

    public SpaceListResponses getSpaces(
            String name,
            SpaceCategory category
    ) {
        List<Space> spaces = findSpaces(name, category);

        return SpaceListResponses.from(spaces, findAddressesBySpaces(spaces));
    }

    @Override
    public SpaceDetailResponse getSpace(
            Long spaceId
    ) {
        Space space = getActiveSpace(spaceId);
        Address address = getAddress(space.getAddressId());
        List<SpaceImage> images = spaceImageRepository.findAllBySpaceId(spaceId);

        return SpaceDetailResponse.from(space, AddressResponse.from(address), images);
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
                request.phone()
        );

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

    public MySpaceListResponses getMySpaces(
            String hostId,
            String name,
            SpaceCategory category
    ) {
        List<Space> spaces = findMySpaces(hostId, name, category);

        return MySpaceListResponses.from(spaces, findAddressesBySpaces(spaces));
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

        return new SpaceMatchingContextResponse(
                space.getId(),
                space.getHostId(),
                space.getAdminStatus() == ApprovalStatus.APPROVED,
                Boolean.TRUE.equals(space.getIsActive()),
                available
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

    private List<Space> findSpaces(
            String name,
            SpaceCategory category
    ) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasCategory = category != null;

        if (hasName && hasCategory) {
            return spaceRepository.findAllByNameContainingAndCategoryAndDeletedAtIsNull(
                    name,
                    category
            );
        }

        if (hasName) {
            return spaceRepository.findAllByNameContainingAndDeletedAtIsNull(name);
        }

        if (hasCategory) {
            return spaceRepository.findAllByCategoryAndDeletedAtIsNull(category);
        }

        return spaceRepository.findAllByDeletedAtIsNull();
    }

    private List<Space> findMySpaces(
            String hostId,
            String name,
            SpaceCategory category
    ) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasCategory = category != null;

        if (hasName && hasCategory) {
            return spaceRepository.findAllByHostIdAndNameContainingAndCategoryAndDeletedAtIsNull(
                    hostId,
                    name,
                    category
            );
        }

        if (hasName) {
            return spaceRepository.findAllByHostIdAndNameContainingAndDeletedAtIsNull(
                    hostId,
                    name
            );
        }

        if (hasCategory) {
            return spaceRepository.findAllByHostIdAndCategoryAndDeletedAtIsNull(
                    hostId,
                    category
            );
        }

        return spaceRepository.findAllByHostIdAndDeletedAtIsNull(hostId);
    }
}
