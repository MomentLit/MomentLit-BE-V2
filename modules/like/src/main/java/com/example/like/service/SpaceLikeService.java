package com.example.like.service;

import com.example.like.dto.response.SpaceLikeResponse;
import com.example.like.entity.SpaceLike;
import com.example.like.global.exception.DuplicateSpaceLikeException;
import com.example.like.global.exception.SpaceLikeNotFoundException;
import com.example.like.repository.SpaceLikeRepository;
import com.example.space.entity.Space;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceLikeService {

    private final SpaceLikeRepository spaceLikeRepository;
    private final SpaceRepository spaceRepository;

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

    private Space getSpaceWithLock(Long spaceId) {
        return spaceRepository.findByIdWithLock(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("공간을 찾을 수 없습니다."));
    }
}
