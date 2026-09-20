package com.example.like.service;

import com.example.common.dto.PageResponse;
import com.example.like.dto.response.SpaceLikeResponse;
import com.example.like.entity.SpaceLike;
import com.example.like.global.exception.DuplicateSpaceLikeException;
import com.example.like.global.exception.SpaceLikeNotFoundException;
import com.example.like.repository.SpaceLikeRepository;
import com.example.space.dto.response.AddressResponse;
import com.example.space.dto.response.SpaceListResponse;
import com.example.space.entity.Address;
import com.example.space.entity.Space;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceLikeService {

    private final SpaceLikeRepository spaceLikeRepository;
    private final SpaceRepository spaceRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public SpaceLikeResponse like(String userId, Long spaceId) {
        Space space = getSpaceWithLock(spaceId);

        if (spaceLikeRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new DuplicateSpaceLikeException("이미 좋아요한 공간입니다.");
        }

        spaceLikeRepository.save(SpaceLike.create(spaceId, userId));
        space.increaseLikeCount();

        return SpaceLikeResponse.liked(spaceId, space.getLikeCount());
    }

    @Transactional
    public SpaceLikeResponse unlike(String userId, Long spaceId) {
        Space space = getSpaceWithLock(spaceId);
        SpaceLike spaceLike = spaceLikeRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> new SpaceLikeNotFoundException("좋아요 기록을 찾을 수 없습니다."));

        spaceLikeRepository.delete(spaceLike);
        space.decreaseLikeCount();

        return SpaceLikeResponse.unliked(spaceId, space.getLikeCount());
    }

    public SpaceLikeResponse getStatus(String userId, Long spaceId) {
        Space space = spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("공간을 찾을 수 없습니다."));
        boolean liked = spaceLikeRepository.existsBySpaceIdAndUserId(spaceId, userId);

        return liked
                ? SpaceLikeResponse.liked(spaceId, space.getLikeCount())
                : SpaceLikeResponse.unliked(spaceId, space.getLikeCount());
    }

    /** 좋아요한 순서(최신순 기본)를 유지한 채 공간 목록을 반환한다 — 승인 취소 등으로 소프트 삭제된 공간은 조용히 제외한다. */
    public PageResponse<SpaceListResponse> getLikedSpaces(String userId, Pageable pageable) {
        Page<SpaceLike> likePage = spaceLikeRepository.findByUserId(userId, pageable);

        List<Long> spaceIds = likePage.getContent().stream().map(SpaceLike::getSpaceId).toList();
        Map<Long, Space> spaceById = spaceRepository.findByIdInAndDeletedAtIsNull(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, Function.identity()));

        List<Long> addressIds = spaceById.values().stream().map(Space::getAddressId).distinct().toList();
        Map<Long, Address> addressById = addressRepository.findAllById(addressIds).stream()
                .collect(Collectors.toMap(Address::getId, Function.identity()));

        List<SpaceListResponse> content = spaceIds.stream()
                .map(spaceById::get)
                .filter(Objects::nonNull)
                .map(space -> SpaceListResponse.from(space, AddressResponse.from(addressById.get(space.getAddressId()))))
                .toList();

        return new PageResponse<>(
                content,
                likePage.getNumber(),
                likePage.getSize(),
                likePage.getTotalElements(),
                likePage.getTotalPages()
        );
    }

    private Space getSpaceWithLock(Long spaceId) {
        return spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("공간을 찾을 수 없습니다."));
    }
}
