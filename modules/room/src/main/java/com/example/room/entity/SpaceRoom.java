package com.example.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** 공간의 3D 방 하나(360도 장면마다 하나). geometry는 RoomGeometry를 직렬화한 JSON. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "space_rooms", schema = "spaces")
public class SpaceRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "scene_image_url")
    private String sceneImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomSource source;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "ceiling_height", nullable = false)
    private Double ceilingHeight;

    @Column(name = "camera_height")
    private Double cameraHeight;

    @Column(name = "area_m2", nullable = false)
    private Double areaM2;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String geometry;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SpaceRoom create(Long spaceId) {
        SpaceRoom room = new SpaceRoom();
        room.spaceId = spaceId;
        return room;
    }

    public void update(
            String name,
            Integer sortOrder,
            String sceneImageUrl,
            RoomSource source,
            String imageUrl,
            Double ceilingHeight,
            Double cameraHeight,
            Double areaM2,
            String geometry
    ) {
        this.name = name.trim();
        this.sortOrder = sortOrder;
        this.sceneImageUrl = sceneImageUrl;
        this.source = source;
        this.imageUrl = imageUrl;
        this.ceilingHeight = ceilingHeight;
        this.cameraHeight = cameraHeight;
        this.areaM2 = areaM2;
        this.geometry = geometry;
    }
}
