package com.example.room.service;

import com.example.image.service.ImageService;
import com.example.room.dto.request.FurnitureLayoutRequest;
import com.example.room.dto.request.RoomGeometry;
import com.example.room.dto.request.RoomUpsertRequest;
import com.example.room.dto.response.FurnitureLayoutResponse;
import com.example.room.dto.response.RoomResponse;
import com.example.room.entity.RoomSource;
import com.example.room.entity.SpaceRoom;
import com.example.room.entity.SpaceRoomLayout;
import com.example.room.global.exception.FurnitureLayoutNotFoundException;
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.RoomNotFoundException;
import com.example.room.repository.SpaceRoomLayoutRepository;
import com.example.room.repository.SpaceRoomRepository;
import com.example.space.entity.Space;
import com.example.space.global.exception.ForbiddenException;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final Long SPACE_ID = 1L;
    private static final Long ROOM_ID = 10L;
    private static final String HOST_ID = "host-1";
    private static final String GUEST_ID = "guest-1";
    private static final String IMAGE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/aligned.jpg";
    private static final String SCENE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/scene.jpg";

    @Mock
    private SpaceRoomRepository roomRepository;
    @Mock
    private SpaceRoomLayoutRepository layoutRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private ImageService imageService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private RoomService roomService;
    private Space space;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, layoutRepository, spaceRepository, imageService, objectMapper);
        space = mock(Space.class);
    }

    private static RoomGeometry geometry() {
        return new RoomGeometry(
                List.of(List.of(0.0, 0.0), List.of(4.0, 0.0), List.of(4.0, 3.0), List.of(0.0, 3.0)),
                List.of(), List.of(2.0, 1.5), 0.0, null);
    }

    private static RoomUpsertRequest request(Double cameraHeight) {
        return new RoomUpsertRequest("거실", 0, SCENE_URL, RoomSource.PANORAMA, IMAGE_URL, 2.4, cameraHeight, geometry());
    }

    private void givenActiveSpace() {
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.of(space));
    }

    private void givenValidImages() {
        when(imageService.isOwnImageUrl(IMAGE_URL)).thenReturn(true);
        when(imageService.isOwnImageUrl(SCENE_URL)).thenReturn(true);
    }

    private SpaceRoom givenRoom() {
        SpaceRoom room = SpaceRoom.create(SPACE_ID);
        when(roomRepository.findByIdAndSpaceId(ROOM_ID, SPACE_ID)).thenReturn(Optional.of(room));
        return room;
    }

    // ---------- 방 ----------

    @Test
    void 호스트가_장면마다_방을_만들면_이름_순서_면적이_저장된다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        givenValidImages();
        when(roomRepository.countBySpaceId(SPACE_ID)).thenReturn(2L);
        when(roomRepository.saveAndFlush(any(SpaceRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomResponse response = roomService.createRoom(HOST_ID, "USER", SPACE_ID,
                new RoomUpsertRequest(" 침실 ", 2, SCENE_URL, RoomSource.PANORAMA, IMAGE_URL, 2.4, 1.5, geometry()));

        assertThat(response.spaceId()).isEqualTo(SPACE_ID);
        assertThat(response.name()).isEqualTo("침실");
        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.sceneImageUrl()).isEqualTo(SCENE_URL);
        assertThat(response.areaM2()).isEqualTo(12.0);
        assertThat(response.areaPyeong()).isEqualTo(3.6);
        assertThat(response.geometry()).isEqualTo(geometry());
    }

    @Test
    void 방은_공간마다_20개까지다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        when(roomRepository.countBySpaceId(SPACE_ID)).thenReturn(20L);

        assertThatThrownBy(() -> roomService.createRoom(HOST_ID, "USER", SPACE_ID, request(1.5)))
                .isInstanceOf(InvalidRoomException.class)
                .hasMessageContaining("20개");
        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void 호스트가_아닌_사용자는_방을_만들_수_없다() {
        givenActiveSpace();
        when(space.isOwner(GUEST_ID)).thenReturn(false);

        assertThatThrownBy(() -> roomService.createRoom(GUEST_ID, "USER", SPACE_ID, request(1.5)))
                .isInstanceOf(ForbiddenException.class);
        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void 관리자는_다른_사람_공간의_방도_고칠_수_있다() {
        givenActiveSpace();
        givenValidImages();
        givenRoom();
        when(roomRepository.saveAndFlush(any(SpaceRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(roomService.updateRoom("admin-1", "ADMIN", SPACE_ID, ROOM_ID, request(1.5)).areaM2()).isEqualTo(12.0);
    }

    @Test
    void 다른_공간의_방은_고칠_수_없다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        when(roomRepository.findByIdAndSpaceId(ROOM_ID, SPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateRoom(HOST_ID, "USER", SPACE_ID, ROOM_ID, request(1.5)))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void 촬영_높이가_천장보다_높으면_거절한다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        givenValidImages();
        when(roomRepository.countBySpaceId(SPACE_ID)).thenReturn(0L);

        assertThatThrownBy(() -> roomService.createRoom(HOST_ID, "USER", SPACE_ID, request(2.5)))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 외부_이미지_URL은_거절한다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        when(roomRepository.countBySpaceId(SPACE_ID)).thenReturn(0L);
        when(imageService.isOwnImageUrl(IMAGE_URL)).thenReturn(false);

        assertThatThrownBy(() -> roomService.createRoom(HOST_ID, "USER", SPACE_ID, request(1.5)))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 도면_방은_아직_거절한다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        when(roomRepository.countBySpaceId(SPACE_ID)).thenReturn(0L);
        RoomUpsertRequest plan = new RoomUpsertRequest("도면", 0, null, RoomSource.PLAN, null, 2.4, null, geometry());

        assertThatThrownBy(() -> roomService.createRoom(HOST_ID, "USER", SPACE_ID, plan))
                .isInstanceOf(InvalidRoomException.class);
    }

    @Test
    void 방을_지우면_그_방의_가구_배치도_지운다() {
        givenActiveSpace();
        when(space.isOwner(HOST_ID)).thenReturn(true);
        SpaceRoom room = givenRoom();

        roomService.deleteRoom(HOST_ID, "USER", SPACE_ID, ROOM_ID);

        verify(layoutRepository).deleteAllByRoomId(room.getId());
        verify(roomRepository).delete(room);
    }

    @Test
    void 삭제된_공간의_방은_조회되지_않는다() {
        when(spaceRepository.findByIdAndDeletedAtIsNull(SPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRooms(SPACE_ID)).isInstanceOf(SpaceNotFoundException.class);
    }

    @Test
    void 방이_없으면_빈_목록() {
        givenActiveSpace();
        when(roomRepository.findAllBySpaceIdOrderBySortOrderAscIdAsc(SPACE_ID)).thenReturn(List.of());

        assertThat(roomService.getRooms(SPACE_ID)).isEmpty();
    }

    // ---------- 가구 배치 ----------

    private static final List<FurnitureLayoutRequest.Item> ITEMS = List.of(
            new FurnitureLayoutRequest.Item("sofa-3", 1.0, 2.0, Math.PI / 2),
            new FurnitureLayoutRequest.Item("plant", 3.5, 0.5, 0.0));

    @Test
    void 게스트가_방에_이름을_붙여_새_배치를_저장한다() {
        givenActiveSpace();
        givenRoom();
        when(layoutRepository.countByRoomIdAndUserId(ROOM_ID, GUEST_ID)).thenReturn(3L);
        when(layoutRepository.saveAndFlush(any(SpaceRoomLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        FurnitureLayoutResponse saved = roomService.createMyLayout(GUEST_ID, SPACE_ID, ROOM_ID, new FurnitureLayoutRequest(" 거실 A안 ", ITEMS));

        assertThat(saved.items()).isEqualTo(ITEMS);
        assertThat(saved.name()).isEqualTo("거실 A안");
        assertThat(saved.spaceId()).isEqualTo(SPACE_ID);
        ArgumentCaptor<SpaceRoomLayout> captor = ArgumentCaptor.forClass(SpaceRoomLayout.class);
        verify(layoutRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(GUEST_ID);
    }

    @Test
    void 배치는_방마다_20개까지다() {
        givenActiveSpace();
        givenRoom();
        when(layoutRepository.countByRoomIdAndUserId(ROOM_ID, GUEST_ID)).thenReturn(20L);

        assertThatThrownBy(() -> roomService.createMyLayout(GUEST_ID, SPACE_ID, ROOM_ID, new FurnitureLayoutRequest("B안", ITEMS)))
                .isInstanceOf(InvalidRoomException.class)
                .hasMessageContaining("20개");
        verify(layoutRepository, never()).saveAndFlush(any());
    }

    @Test
    void 없는_방에는_가구_배치를_저장할_수_없다() {
        givenActiveSpace();
        when(roomRepository.findByIdAndSpaceId(ROOM_ID, SPACE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createMyLayout(GUEST_ID, SPACE_ID, ROOM_ID, new FurnitureLayoutRequest("A안", List.of())))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    void 내_배치를_덮어쓴다() {
        givenRoom();
        SpaceRoomLayout layout = SpaceRoomLayout.create(ROOM_ID, SPACE_ID, GUEST_ID, "A안", "[]");
        when(layoutRepository.findByIdAndRoomIdAndUserId(5L, ROOM_ID, GUEST_ID)).thenReturn(Optional.of(layout));
        when(layoutRepository.saveAndFlush(layout)).thenReturn(layout);

        FurnitureLayoutResponse updated = roomService.updateMyLayout(GUEST_ID, SPACE_ID, ROOM_ID, 5L, new FurnitureLayoutRequest("A안 수정", ITEMS));

        assertThat(updated.name()).isEqualTo("A안 수정");
        assertThat(updated.items()).isEqualTo(ITEMS);
    }

    @Test
    void 다른_사람의_배치는_고치거나_지울_수_없다() {
        givenRoom();
        when(layoutRepository.findByIdAndRoomIdAndUserId(5L, ROOM_ID, "someone-else")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateMyLayout("someone-else", SPACE_ID, ROOM_ID, 5L, new FurnitureLayoutRequest("x", ITEMS)))
                .isInstanceOf(FurnitureLayoutNotFoundException.class);
        assertThatThrownBy(() -> roomService.deleteMyLayout("someone-else", SPACE_ID, ROOM_ID, 5L))
                .isInstanceOf(FurnitureLayoutNotFoundException.class);
        verify(layoutRepository, never()).delete(any());
    }

    @Test
    void 저장한_배치가_없으면_빈_목록() {
        givenActiveSpace();
        givenRoom();
        when(layoutRepository.findAllByRoomIdAndUserIdOrderByUpdatedAtDesc(ROOM_ID, GUEST_ID)).thenReturn(List.of());

        assertThat(roomService.getMyLayouts(GUEST_ID, SPACE_ID, ROOM_ID)).isEmpty();
    }
}
