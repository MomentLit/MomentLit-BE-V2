package com.example.room.service;

import com.example.common.security.Role;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 공간의 3D 방(360도 장면마다 하나)과, 사용자가 방마다 저장한 가구 배치.
 * 방은 누구나 보고, 만들기·고치기·지우기는 호스트 본인과 관리자만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    static final int MAX_ROOMS_PER_SPACE = 20;
    static final int MAX_LAYOUTS_PER_ROOM = 20;

    private static final TypeReference<List<FurnitureLayoutRequest.Item>> FURNITURE_TYPE = new TypeReference<>() {
    };

    private final SpaceRoomRepository roomRepository;
    private final SpaceRoomLayoutRepository layoutRepository;
    private final SpaceRepository spaceRepository;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;

    /** 공간의 3D 방들, 순서대로. 없으면 빈 목록. */
    public List<RoomResponse> getRooms(Long spaceId) {
        getActiveSpace(spaceId);
        return roomRepository.findAllBySpaceIdOrderBySortOrderAscIdAsc(spaceId).stream().map(this::toResponse).toList();
    }

    public RoomResponse getRoom(Long spaceId, Long roomId) {
        getActiveSpace(spaceId);
        return toResponse(getRoomEntity(spaceId, roomId));
    }

    @Transactional
    public RoomResponse createRoom(String userId, String role, Long spaceId, RoomUpsertRequest request) {
        Space space = getActiveSpace(spaceId);
        validateOwnerOrAdmin(space, userId, role);
        if (roomRepository.countBySpaceId(spaceId) >= MAX_ROOMS_PER_SPACE) {
            throw new InvalidRoomException("3D 방은 공간마다 " + MAX_ROOMS_PER_SPACE + "개까지 만들 수 있습니다.");
        }
        return save(SpaceRoom.create(spaceId), request);
    }

    @Transactional
    public RoomResponse updateRoom(String userId, String role, Long spaceId, Long roomId, RoomUpsertRequest request) {
        Space space = getActiveSpace(spaceId);
        validateOwnerOrAdmin(space, userId, role);
        return save(getRoomEntity(spaceId, roomId), request);
    }

    /** 방을 지우면 그 방에 사용자들이 저장한 가구 배치도 함께 지운다(돌아갈 방이 없다). */
    @Transactional
    public void deleteRoom(String userId, String role, Long spaceId, Long roomId) {
        Space space = getActiveSpace(spaceId);
        validateOwnerOrAdmin(space, userId, role);
        SpaceRoom room = getRoomEntity(spaceId, roomId);
        layoutRepository.deleteAllByRoomId(room.getId());
        roomRepository.delete(room);
    }

    /** 내가 이 방에 저장한 배치들, 최근에 고친 것부터. */
    public List<FurnitureLayoutResponse> getMyLayouts(String userId, Long spaceId, Long roomId) {
        getActiveSpace(spaceId);
        getRoomEntity(spaceId, roomId);
        return layoutRepository.findAllByRoomIdAndUserIdOrderByUpdatedAtDesc(roomId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 새 배치로 저장. 방마다 사용자당 MAX_LAYOUTS_PER_ROOM개까지. */
    @Transactional
    public FurnitureLayoutResponse createMyLayout(String userId, Long spaceId, Long roomId, FurnitureLayoutRequest request) {
        getActiveSpace(spaceId);
        SpaceRoom room = getRoomEntity(spaceId, roomId);
        if (layoutRepository.countByRoomIdAndUserId(roomId, userId) >= MAX_LAYOUTS_PER_ROOM) {
            throw new InvalidRoomException("배치는 방마다 " + MAX_LAYOUTS_PER_ROOM + "개까지 저장할 수 있습니다. 안 쓰는 배치를 지워 주세요.");
        }

        SpaceRoomLayout layout = SpaceRoomLayout.create(
                room.getId(), spaceId, userId, request.name(), objectMapper.writeValueAsString(request.items()));
        return toResponse(layoutRepository.saveAndFlush(layout));
    }

    /** 내 배치 덮어쓰기(이름·가구). 다른 사람 배치는 없는 것으로 본다. */
    @Transactional
    public FurnitureLayoutResponse updateMyLayout(String userId, Long spaceId, Long roomId, Long layoutId, FurnitureLayoutRequest request) {
        SpaceRoomLayout layout = getMyLayoutEntity(userId, spaceId, roomId, layoutId);
        layout.update(request.name(), objectMapper.writeValueAsString(request.items()));
        return toResponse(layoutRepository.saveAndFlush(layout));
    }

    @Transactional
    public void deleteMyLayout(String userId, Long spaceId, Long roomId, Long layoutId) {
        layoutRepository.delete(getMyLayoutEntity(userId, spaceId, roomId, layoutId));
    }

    private RoomResponse save(SpaceRoom room, RoomUpsertRequest request) {
        validatePanoramaRoom(request);
        double area = request.geometry().validateAndComputeArea();
        room.update(
                request.name(),
                request.sortOrderOrDefault(),
                request.sceneImageUrl(),
                request.source(),
                request.imageUrl(),
                request.ceilingHeight(),
                request.cameraHeight(),
                Math.round(area * 100) / 100.0,
                objectMapper.writeValueAsString(request.geometry())
        );
        // 저장 후 updated_at을 응답에 담기 위해 flush
        return toResponse(roomRepository.saveAndFlush(room));
    }

    private SpaceRoomLayout getMyLayoutEntity(String userId, Long spaceId, Long roomId, Long layoutId) {
        getRoomEntity(spaceId, roomId);
        return layoutRepository.findByIdAndRoomIdAndUserId(layoutId, roomId, userId)
                .orElseThrow(() -> new FurnitureLayoutNotFoundException("가구 배치를 찾을 수 없습니다."));
    }

    private void validatePanoramaRoom(RoomUpsertRequest request) {
        if (request.source() != RoomSource.PANORAMA) {
            throw new InvalidRoomException("도면으로 만든 방은 아직 지원하지 않습니다.");
        }
        if (request.imageUrl() == null || !imageService.isOwnImageUrl(request.imageUrl())) {
            throw new InvalidRoomException("파노라마 방에는 이 서비스에 올린 사진 URL이 필요합니다.");
        }
        if (request.sceneImageUrl() != null && !imageService.isOwnImageUrl(request.sceneImageUrl())) {
            throw new InvalidRoomException("360도 장면 사진 URL이 올바르지 않습니다.");
        }
        if (request.cameraHeight() == null) {
            throw new InvalidRoomException("파노라마 방에는 촬영 높이가 필요합니다.");
        }
        if (request.cameraHeight() >= request.ceilingHeight()) {
            throw new InvalidRoomException("촬영 높이는 천장 높이보다 낮아야 합니다.");
        }
        if (request.geometry().camera() == null) {
            throw new InvalidRoomException("파노라마 방에는 촬영 위치(camera)가 필요합니다.");
        }
    }

    private void validateOwnerOrAdmin(Space space, String userId, String role) {
        if (Role.ADMIN.name().equals(role) || space.isOwner(userId)) {
            return;
        }
        throw new ForbiddenException("해당 공간에 대한 권한이 없습니다.");
    }

    private Space getActiveSpace(Long spaceId) {
        return spaceRepository.findByIdAndDeletedAtIsNull(spaceId)
                .orElseThrow(() -> new SpaceNotFoundException("공간을 찾을 수 없습니다."));
    }

    private SpaceRoom getRoomEntity(Long spaceId, Long roomId) {
        return roomRepository.findByIdAndSpaceId(roomId, spaceId)
                .orElseThrow(() -> new RoomNotFoundException("3D 방을 찾을 수 없습니다."));
    }

    private RoomResponse toResponse(SpaceRoom room) {
        return RoomResponse.of(room, objectMapper.readValue(room.getGeometry(), RoomGeometry.class));
    }

    private FurnitureLayoutResponse toResponse(SpaceRoomLayout layout) {
        return new FurnitureLayoutResponse(
                layout.getId(),
                layout.getRoomId(),
                layout.getSpaceId(),
                layout.getName(),
                objectMapper.readValue(layout.getFurniture(), FURNITURE_TYPE),
                layout.getUpdatedAt()
        );
    }
}
