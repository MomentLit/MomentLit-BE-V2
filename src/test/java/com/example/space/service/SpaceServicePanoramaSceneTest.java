package com.example.space.service;

import com.example.space.dto.request.AddressRequest;
import com.example.space.dto.request.PanoramaSceneLinkRequest;
import com.example.space.dto.request.PanoramaSceneRequest;
import com.example.space.dto.request.SpaceCreateRequest;
import com.example.space.dto.request.SpaceUpdateRequest;
import com.example.space.entity.Space;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.SpacePanoramaScene;
import com.example.space.entity.SpacePanoramaSceneLink;
import com.example.space.global.exception.BadRequestException;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.AiSummaryOutboxRepository;
import com.example.space.repository.SpaceImageRepository;
import com.example.space.repository.SpacePanoramaSceneLinkRepository;
import com.example.space.repository.SpacePanoramaSceneRepository;
import com.example.space.repository.SpaceRepository;
import com.example.space.repository.SpaceScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 360도 장면(공간 안 로드뷰형 이동) 저장 규칙.
 * space 모듈에는 테스트 의존성이 없어서, 모든 모듈을 테스트 클래스패스에 두는 루트 프로젝트에 둔다.
 */
@ExtendWith(MockitoExtension.class)
class SpaceServicePanoramaSceneTest {

    private static final Long SPACE_ID = 1L;
    private static final String HOST_ID = "host-1";

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
    private AddressRepository addressRepository;

    @Mock
    private AiSummaryOutboxRepository aiSummaryOutboxRepository;

    @InjectMocks
    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        // 수정 흐름이 커밋 후 챗봇 동기화를 등록하므로 트랜잭션 동기화를 켜 둔다.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void createSpace_savesPanoramaScenesInRequestOrderAndSyncsPanoramaUrl() {
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> {
            Space saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", SPACE_ID);
            return saved;
        });

        spaceService.createSpace(HOST_ID, createRequest(List.of(
                new PanoramaSceneRequest("https://s3/entrance.jpg", "입구", null),
                new PanoramaSceneRequest("https://s3/hall.jpg", " ", null),
                new PanoramaSceneRequest("https://s3/room.jpg", "룸 A", null)
        )));

        List<SpacePanoramaScene> scenes = captureSavedScenes();
        assertThat(scenes).extracting(SpacePanoramaScene::getImageUrl)
                .containsExactly("https://s3/entrance.jpg", "https://s3/hall.jpg", "https://s3/room.jpg");
        assertThat(scenes).extracting(SpacePanoramaScene::getSortOrder).containsExactly(0, 1, 2);
        assertThat(scenes).extracting(SpacePanoramaScene::getSpaceId).containsOnly(SPACE_ID);
        assertThat(scenes).extracting(SpacePanoramaScene::getName).containsExactly("입구", null, "룸 A");

        ArgumentCaptor<Space> spaceCaptor = ArgumentCaptor.forClass(Space.class);
        verify(spaceRepository).save(spaceCaptor.capture());
        assertThat(spaceCaptor.getValue().getPanoramaUrl()).isEqualTo("https://s3/entrance.jpg");
    }

    @Test
    void createSpace_keepsLegacyPanoramaUrlWhenScenesAreMissing() {
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));

        spaceService.createSpace(HOST_ID, createRequest("https://s3/legacy.jpg", null));

        ArgumentCaptor<Space> spaceCaptor = ArgumentCaptor.forClass(Space.class);
        verify(spaceRepository).save(spaceCaptor.capture());
        assertThat(spaceCaptor.getValue().getPanoramaUrl()).isEqualTo("https://s3/legacy.jpg");
        verify(spacePanoramaSceneRepository, never()).saveAll(anyList());
    }

    @Test
    void createSpace_rejectsSceneWithoutImageUrl() {
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.save(any(Space.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> spaceService.createSpace(HOST_ID, createRequest(List.of(
                new PanoramaSceneRequest("https://s3/entrance.jpg", "입구", null),
                new PanoramaSceneRequest(" ", "홀", null)
        )))).isInstanceOf(BadRequestException.class);

        verify(spacePanoramaSceneRepository, never()).saveAll(anyList());
    }

    @Test
    void updateSpace_replacesPanoramaScenesWhenProvided() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));

        spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(List.of(
                new PanoramaSceneRequest("https://s3/hall.jpg", "홀", null),
                new PanoramaSceneRequest("https://s3/room.jpg", "룸 A", null)
        )));

        verify(spacePanoramaSceneLinkRepository).deleteAllBySpaceId(SPACE_ID);
        verify(spacePanoramaSceneRepository).deleteAllBySpaceId(SPACE_ID);
        assertThat(captureSavedScenes()).extracting(SpacePanoramaScene::getImageUrl)
                .containsExactly("https://s3/hall.jpg", "https://s3/room.jpg");
        assertThat(space.getPanoramaUrl()).isEqualTo("https://s3/hall.jpg");
    }

    @Test
    void updateSpace_keepsPanoramaScenesWhenNotProvided() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));

        spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(null));

        verify(spacePanoramaSceneRepository, never()).deleteAllBySpaceId(anyLong());
        verify(spacePanoramaSceneRepository, never()).saveAll(anyList());
        verify(spacePanoramaSceneLinkRepository, never()).deleteAllBySpaceId(anyLong());
        verify(spacePanoramaSceneLinkRepository, never()).saveAll(anyList());
        assertThat(space.getPanoramaUrl()).isEqualTo("https://s3/old.jpg");
    }

    @Test
    void updateSpace_deletesAllPanoramaScenesWhenEmpty() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));

        spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(List.of()));

        verify(spacePanoramaSceneLinkRepository).deleteAllBySpaceId(SPACE_ID);
        verify(spacePanoramaSceneRepository).deleteAllBySpaceId(SPACE_ID);
        verify(spacePanoramaSceneRepository, never()).saveAll(anyList());
        verify(spacePanoramaSceneLinkRepository, never()).saveAll(anyList());
        assertThat(space.getPanoramaUrl()).isNull();
    }

    @Test
    void deleteSpace_deletesPanoramaScenes() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));

        spaceService.deleteSpace(HOST_ID, SPACE_ID);

        verify(spacePanoramaSceneLinkRepository).deleteAllBySpaceId(SPACE_ID);
        verify(spacePanoramaSceneRepository).deleteAllBySpaceId(SPACE_ID);
    }

    @Test
    void updateSpace_savesSceneLinksWithSavedSceneIds() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));
        assignSceneIdsOnSave();

        spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(List.of(
                new PanoramaSceneRequest("https://s3/entrance.jpg", "입구", List.of(
                        new PanoramaSceneLinkRequest(1, 0.0),
                        new PanoramaSceneLinkRequest(2, Math.PI / 2)
                )),
                new PanoramaSceneRequest("https://s3/hall.jpg", "홀", List.of(
                        new PanoramaSceneLinkRequest(0, Math.PI)
                )),
                new PanoramaSceneRequest("https://s3/room.jpg", "룸 A", List.of(
                        new PanoramaSceneLinkRequest(0, Math.PI * 3 / 2)
                ))
        )));

        List<SpacePanoramaSceneLink> links = captureSavedLinks();
        assertThat(links).extracting(SpacePanoramaSceneLink::getFromSceneId).containsExactly(100L, 100L, 101L, 102L);
        assertThat(links).extracting(SpacePanoramaSceneLink::getToSceneId).containsExactly(101L, 102L, 100L, 100L);
        assertThat(links).extracting(SpacePanoramaSceneLink::getYaw)
                .containsExactly(0.0, Math.PI / 2, Math.PI, Math.PI * 3 / 2);
        assertThat(links).extracting(SpacePanoramaSceneLink::getSpaceId).containsOnly(SPACE_ID);
    }

    @Test
    void updateSpace_skipsLinkSaveWhenScenesHaveNoLinks() {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));
        assignSceneIdsOnSave();

        spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(List.of(
                new PanoramaSceneRequest("https://s3/hall.jpg", "홀", null),
                new PanoramaSceneRequest("https://s3/room.jpg", "룸 A", List.of())
        )));

        verify(spacePanoramaSceneLinkRepository, never()).saveAll(anyList());
    }

    @Test
    void updateSpace_rejectsLinkToMissingScene() {
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(5, 0.0)));
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(-1, 0.0)));
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(null, 0.0)));
    }

    @Test
    void updateSpace_rejectsLinkToSelf() {
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(0, 0.0)));
    }

    @Test
    void updateSpace_rejectsDuplicateLinkToSameScene() {
        assertRejectsLinks(List.of(
                new PanoramaSceneLinkRequest(1, 0.0),
                new PanoramaSceneLinkRequest(1, Math.PI)
        ));
    }

    @Test
    void updateSpace_rejectsLinkWithInvalidYaw() {
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(1, null)));
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(1, -0.1)));
        assertRejectsLinks(List.of(new PanoramaSceneLinkRequest(1, Math.PI * 2)));
    }

    /** 첫 장면에 주어진 화살표를 붙여 두 장면으로 수정하면 거부되고, 화살표는 저장되지 않는다. */
    private void assertRejectsLinks(List<PanoramaSceneLinkRequest> links) {
        Space space = existingSpace("https://s3/old.jpg");
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));
        assignSceneIdsOnSave();

        assertThatThrownBy(() -> spaceService.updateSpace(HOST_ID, SPACE_ID, updateRequest(List.of(
                new PanoramaSceneRequest("https://s3/hall.jpg", "홀", links),
                new PanoramaSceneRequest("https://s3/room.jpg", "룸 A", null)
        )))).isInstanceOf(BadRequestException.class);

        verify(spacePanoramaSceneLinkRepository, never()).saveAll(anyList());
    }

    /** IDENTITY 저장처럼 saveAll에 넘긴 장면에 순서대로 100, 101, … id를 채운다. */
    private void assignSceneIdsOnSave() {
        when(spacePanoramaSceneRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<SpacePanoramaScene> scenes = invocation.getArgument(0);
            for (int index = 0; index < scenes.size(); index++) {
                ReflectionTestUtils.setField(scenes.get(index), "id", 100L + index);
            }
            return scenes;
        });
    }

    @SuppressWarnings("unchecked")
    private List<SpacePanoramaSceneLink> captureSavedLinks() {
        ArgumentCaptor<List<SpacePanoramaSceneLink>> captor = ArgumentCaptor.forClass(List.class);
        verify(spacePanoramaSceneLinkRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<SpacePanoramaScene> captureSavedScenes() {
        ArgumentCaptor<List<SpacePanoramaScene>> captor = ArgumentCaptor.forClass(List.class);
        verify(spacePanoramaSceneRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Space existingSpace(String panoramaUrl) {
        Space space = Space.create(
                HOST_ID, "공간", "소개", null, 10L, null, 10000,
                SpaceCategory.POPUP_STORE, null, null, null, null, null, null, false
        );
        ReflectionTestUtils.setField(space, "id", SPACE_ID);
        space.updatePanoramaUrl(panoramaUrl);
        return space;
    }

    private SpaceCreateRequest createRequest(List<PanoramaSceneRequest> panoramaScenes) {
        return createRequest(null, panoramaScenes);
    }

    private SpaceCreateRequest createRequest(String panoramaUrl, List<PanoramaSceneRequest> panoramaScenes) {
        return new SpaceCreateRequest(
                "공간",
                "소개",
                new AddressRequest("부산광역시", "강서구", "명지동", "가락대로 1", null, null, "46700"),
                null,
                null,
                panoramaUrl,
                panoramaScenes,
                10000,
                SpaceCategory.POPUP_STORE,
                null, null, null, null, null, null, null, null, null, null,
                false
        );
    }

    private SpaceUpdateRequest updateRequest(List<PanoramaSceneRequest> panoramaScenes) {
        return new SpaceUpdateRequest(
                null, null, null, null, null, null, null,
                panoramaScenes,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
