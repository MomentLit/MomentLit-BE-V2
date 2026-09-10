package com.example.like.service;

import com.example.like.dto.response.SpaceLikeResponse;
import com.example.like.entity.SpaceLike;
import com.example.like.global.exception.DuplicateSpaceLikeException;
import com.example.like.repository.SpaceLikeRepository;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceLikeServiceTest {

    private static final Long SPACE_ID = 1L;
    private static final String USER_ID = "user-1";

    @Mock
    private SpaceLikeRepository spaceLikeRepository;

    @Mock
    private SpaceRepository spaceRepository;

    private SpaceLikeService spaceLikeService;

    @BeforeEach
    void setUp() {
        spaceLikeService = new SpaceLikeService(spaceLikeRepository, spaceRepository);
    }

    @Test
    void like_createsLikeAndIncreasesCount() {
        Space space = space();
        when(spaceRepository.findByIdWithLock(SPACE_ID)).thenReturn(Optional.of(space));
        when(spaceLikeRepository.existsBySpaceIdAndUserId(SPACE_ID, USER_ID)).thenReturn(false);

        SpaceLikeResponse response = spaceLikeService.like(USER_ID, SPACE_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(spaceLikeRepository).save(any(SpaceLike.class));
    }

    @Test
    void like_rejectsDuplicateLike() {
        when(spaceRepository.findByIdWithLock(SPACE_ID)).thenReturn(Optional.of(space()));
        when(spaceLikeRepository.existsBySpaceIdAndUserId(SPACE_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> spaceLikeService.like(USER_ID, SPACE_ID))
                .isInstanceOf(DuplicateSpaceLikeException.class);
    }

    private Space space() {
        return Space.create(
                "host-1",
                "공간",
                "설명",
                null,
                2L,
                "https://example.com/thumbnail.jpg",
                10_000,
                SpaceCategory.CAFE,
                "010-1234-5678"
        );
    }
}
