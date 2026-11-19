package com.example.room.dto.response;

import com.example.room.dto.request.RoomGeometry;
import com.example.room.entity.RoomSource;
import com.example.room.entity.SpaceRoom;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record RoomResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("scene_id")
        Long sceneId,

        RoomSource source,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("ceiling_height")
        Double ceilingHeight,

        @JsonProperty("camera_height")
        Double cameraHeight,

        @JsonProperty("area_m2")
        Double areaM2,

        // 평 = ㎡ ÷ 3.3058, 소수 첫째 자리
        @JsonProperty("area_pyeong")
        Double areaPyeong,

        RoomGeometry geometry,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
    private static final double M2_PER_PYEONG = 3.3058;

    public static RoomResponse of(SpaceRoom room, RoomGeometry geometry) {
        return new RoomResponse(
                room.getSpaceId(),
                room.getSceneId(),
                room.getSource(),
                room.getImageUrl(),
                room.getCeilingHeight(),
                room.getCameraHeight(),
                room.getAreaM2(),
                Math.round(room.getAreaM2() / M2_PER_PYEONG * 10) / 10.0,
                geometry,
                room.getUpdatedAt()
        );
    }
}
