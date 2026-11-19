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
import com.example.room.global.exception.InvalidRoomException;
import com.example.room.global.exception.RoomNotFoundException;
import com.example.room.repository.SpaceRoomLayoutRepository;
import com.example.room.repository.SpaceRoomRepository;
import com.example.space.entity.Space;
import com.example.space.global.exception.ForbiddenException;
import com.example.space.global.exception.SpaceNotFoundException;
import com.example.space.repository.SpacePanoramaSceneRepository;
import com.example.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final TypeReference<List<FurnitureLayoutRequest.Item>> FURNITURE_TYPE = new TypeReference<>() {
    };

    private final SpaceRoomRepository roomRepository;
    private final SpaceRoomLayoutRepository layoutRepository;
    private final SpaceRepository spaceRepository;
    private final SpacePanoramaSceneRepository sceneRepository;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;

    public RoomResponse getRoom(Long spaceId) {
        getActiveSpace(spaceId);
        return toResponse(getRoomEntity(spaceId));
    }

    /** 호스트 본인과 관리자만 만들거나 고칠 수 있다. 공간당 하나라서 있으면 덮어쓴다. */
    @Transactional
    public RoomResponse saveRoom(String userId, String role, Long spaceId, RoomUpsertRequest request) {
        Space space = getActiveSpace(spaceId);
        validateOwnerOrAdmin(space, userId, role);
        validatePanoramaRoom(spaceId, request);

        double area = request.geometry().validateAndComputeArea();

        SpaceRoom room = roomRepository.findBySpaceId(spaceId).orElseGet(() -> SpaceRoom.create(spaceId));
        room.update(
                request.sceneId(),
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

    /** 방을 지워도 사용자들이 저장한 가구 배치는 남긴다 — 호스트가 방을 다시 만들면 그대로 이어서 볼 수 있다. */
    @Transactional
    public void deleteRoom(String userId, String role, Long spaceId) {
        Space space = getActiveSpace(spaceId);
        validateOwnerOrAdmin(space, userId, role);
        roomRepository.delete(getRoomEntity(spaceId));
    }

    /** 저장한 배치가 없으면 비어 있음(Optional.empty)을 돌려준다 — 처음 보는 사용자에게는 정상 상태라 404로 보내지 않는다. */
    public Optional<FurnitureLayoutResponse> getMyLayout(String userId, Long spaceId) {
        getActiveSpace(spaceId);
        return layoutRepository.findBySpaceIdAndUserId(spaceId, userId).map(this::toResponse);
    }

    @Transactional
    public FurnitureLayoutResponse saveMyLayout(String userId, Long spaceId, FurnitureLayoutRequest request) {
        getActiveSpace(spaceId);
        if (!roomRepository.existsBySpaceId(spaceId)) {
            throw new RoomNotFoundException("이 공간에는 아직 3D 방이 없습니다.");
        }

        SpaceRoomLayout layout = layoutRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseGet(() -> SpaceRoomLayout.create(spaceId, userId));
        layout.updateFurniture(objectMapper.writeValueAsString(request.items()));
        return toResponse(layoutRepository.saveAndFlush(layout));
    }

    @Transactional
    public void deleteMyLayout(String userId, Long spaceId) {
        layoutRepository.findBySpaceIdAndUserId(spaceId, userId).ifPresent(layoutRepository::delete);
    }

    private void validatePanoramaRoom(Long spaceId, RoomUpsertRequest request) {
        if (request.source() != RoomSource.PANORAMA) {
            throw new InvalidRoomException("도면으로 만든 방은 아직 지원하지 않습니다.");
        }
        if (request.imageUrl() == null || !imageService.isOwnImageUrl(request.imageUrl())) {
            throw new InvalidRoomException("파노라마 방에는 이 서비스에 올린 사진 URL이 필요합니다.");
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
        if (request.sceneId() != null) {
            boolean sceneOfThisSpace = sceneRepository.findById(request.sceneId())
                    .map(scene -> scene.getSpaceId().equals(spaceId))
                    .orElse(false);
            if (!sceneOfThisSpace) {
                throw new InvalidRoomException("이 공간의 360도 장면이 아닙니다.");
            }
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

    private SpaceRoom getRoomEntity(Long spaceId) {
        return roomRepository.findBySpaceId(spaceId)
                .orElseThrow(() -> new RoomNotFoundException("이 공간에는 아직 3D 방이 없습니다."));
    }

    private RoomResponse toResponse(SpaceRoom room) {
        return RoomResponse.of(room, objectMapper.readValue(room.getGeometry(), RoomGeometry.class));
    }

    private FurnitureLayoutResponse toResponse(SpaceRoomLayout layout) {
        return new FurnitureLayoutResponse(
                layout.getSpaceId(),
                objectMapper.readValue(layout.getFurniture(), FURNITURE_TYPE),
                layout.getUpdatedAt()
        );
    }
}
