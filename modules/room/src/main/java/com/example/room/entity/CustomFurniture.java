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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자가 만든 가구("내 가구"). 만든 사람만 보고 쓴다. 3D 방 배치에서는 type "u-{id}"로 가리킨다.
 * 모델(GLB)은 바닥 가운데가 원점, 앞이 +z, 단위 m — FE가 그대로 놓는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "custom_furniture", schema = "spaces")
public class CustomFurniture {

    public static final String TYPE_PREFIX = "u-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CustomFurnitureMount mount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CustomFurnitureSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CustomFurnitureStatus status;

    private Double width;

    private Double depth;

    @Column(nullable = false)
    private Double height;

    @Column(name = "model_url")
    private String modelUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 사진으로 만들기 시작 — 치수는 사용자가 잰 높이(폭·깊이는 선택)만 알고, 생성이 끝나면 실제 크기로 바뀐다. */
    public static CustomFurniture processing(String userId, String name, CustomFurnitureMount mount, double height, Double width, Double depth) {
        CustomFurniture furniture = new CustomFurniture();
        furniture.userId = userId;
        furniture.name = name.trim();
        furniture.mount = mount;
        furniture.source = CustomFurnitureSource.PHOTO;
        furniture.status = CustomFurnitureStatus.PROCESSING;
        furniture.height = height;
        furniture.width = width;
        furniture.depth = depth;
        return furniture;
    }

    /** 파일로 올린 가구 — 바로 쓸 수 있다. */
    public static CustomFurniture uploaded(String userId, String name, CustomFurnitureMount mount,
                                           double width, double depth, double height, String modelUrl, String thumbnailUrl) {
        CustomFurniture furniture = new CustomFurniture();
        furniture.userId = userId;
        furniture.name = name.trim();
        furniture.mount = mount;
        furniture.source = CustomFurnitureSource.FILE;
        furniture.status = CustomFurnitureStatus.READY;
        furniture.width = width;
        furniture.depth = depth;
        furniture.height = height;
        furniture.modelUrl = modelUrl;
        furniture.thumbnailUrl = thumbnailUrl;
        return furniture;
    }

    public void ready(double width, double depth, double height, String modelUrl, String thumbnailUrl) {
        this.status = CustomFurnitureStatus.READY;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.modelUrl = modelUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.failureReason = null;
    }

    public void fail(String reason) {
        this.status = CustomFurnitureStatus.FAILED;
        this.failureReason = reason.length() > 255 ? reason.substring(0, 255) : reason;
    }

    public void update(String name, CustomFurnitureMount mount) {
        if (name != null) this.name = name.trim();
        if (mount != null) this.mount = mount;
    }

    public String type() {
        return TYPE_PREFIX + id;
    }
}
