package com.example.space.service;

import com.example.space.dto.request.SpaceAvailabilitySlotRequest;
import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.entity.ApprovalStatus;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.global.exception.ForbiddenException;
import com.example.space.repository.AiSummaryOutboxRepository;
import com.example.space.repository.SpaceAvailabilityRepository;
import com.example.space.repository.SpaceImageRepository;
import com.example.space.repository.SpacePanoramaSceneLinkRepository;
import com.example.space.repository.SpacePanoramaSceneRepository;
import com.example.space.repository.SpaceRepository;
import com.example.space.repository.SpaceScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 관리자는 남의 공간 정보·일정을 고칠 수 있지만, 삭제와 임시저장/검토 요청 전환은 호스트 본인만 한다.
 */
@ExtendWith(MockitoExtension.class)
class SpaceServiceAdminEditTest {

    private static final Long SPACE_ID = 1L;
    private static final String HOST_ID = "host-1";
    private static final String ADMIN_ID = "admin-1";
    private static final String OTHER_USER_ID = "user-2";

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceImageRepository spaceImageRepository;

    @Mock
    private SpacePanoramaSceneRepository spacePanoramaSceneRepository;

    @Mock
    private SpacePanoramaSceneLinkRepository spacePanoramaSceneLinkRepository;

    @Mock
    private SpaceScheduleRepository spaceScheduleRepository;

    @Mock
    private SpaceAvailabilityRepository spaceAvailabilityRepository;

    @Mock
    private AiSummaryOutboxRepository aiSummaryOutboxRepository;

    @InjectMocks
    private SpaceService spaceService;

    private Space space;

    @BeforeEach
    void setUp() {
        // 수정 흐름이 커밋 후 챗봇 동기화를 등록하므로 트랜잭션 동기화를 켜 둔다.
        TransactionSynchronizationManager.initSynchronization();

        space = Space.create(
                HOST_ID, "공간", "소개", null, 10L, null, 10000, false,
                SpaceCategory.POPUP_STORE, null, null, null, null, null, null, false
        );
        ReflectionTestUtils.setField(space, "id", SPACE_ID);
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private SpaceUpdateRequest updateRequest(String parkingInfo, Boolean isDraft) {
        return new SpaceUpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, parkingInfo, null, null, null, isDraft,
                null, null, null, null
        );
    }

    private List<SpaceAvailabilitySlotRequest> slots() {
        return List.of(new SpaceAvailabilitySlotRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), true));
    }

    @Test
    void 관리자는_남의_공간을_수정할_수_있다() {
        spaceService.updateSpace(ADMIN_ID, "ADMIN", SPACE_ID, updateRequest("건물 내 2대 무료", null));

        assertThat(space.getParkingInfo()).isEqualTo("건물 내 2대 무료");
        assertThat(space.getHostId()).isEqualTo(HOST_ID);
    }

    @Test
    void 호스트가_아닌_일반_사용자는_수정할_수_없다() {
        assertThatThrownBy(() -> spaceService.updateSpace(OTHER_USER_ID, "USER", SPACE_ID, updateRequest("주차", null)))
                .isInstanceOf(ForbiddenException.class);

        assertThat(space.getParkingInfo()).isNull();
    }

    @Test
    void 관리자가_수정해도_임시저장_검토_상태는_바뀌지_않는다() {
        ApprovalStatus before = space.getAdminStatus();

        spaceService.updateSpace(ADMIN_ID, "ADMIN", SPACE_ID, updateRequest(null, true));

        assertThat(space.getAdminStatus()).isEqualTo(before);
    }

    @Test
    void 호스트는_여전히_임시저장으로_돌릴_수_있다() {
        spaceService.updateSpace(HOST_ID, "USER", SPACE_ID, updateRequest(null, true));

        assertThat(space.getAdminStatus()).isEqualTo(ApprovalStatus.DRAFT);
    }

    @Test
    void 관리자는_남의_공간_일정을_수정할_수_있다() {
        spaceService.updateAvailability(ADMIN_ID, "ADMIN", SPACE_ID, slots());

        verify(spaceAvailabilityRepository).deleteAllBySpaceId(SPACE_ID);
        verify(spaceAvailabilityRepository).saveAll(anyList());
    }

    @Test
    void 호스트가_아닌_일반_사용자는_일정을_수정할_수_없다() {
        assertThatThrownBy(() -> spaceService.updateAvailability(OTHER_USER_ID, "USER", SPACE_ID, slots()))
                .isInstanceOf(ForbiddenException.class);

        verify(spaceAvailabilityRepository, never()).deleteAllBySpaceId(anyLong());
    }

    @Test
    void 관리자라도_남의_공간은_삭제할_수_없다() {
        assertThatThrownBy(() -> spaceService.deleteSpace(ADMIN_ID, SPACE_ID))
                .isInstanceOf(ForbiddenException.class);

        assertThat(space.getDeletedAt()).isNull();
    }
}
