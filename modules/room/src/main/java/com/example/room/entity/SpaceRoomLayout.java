package com.example.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 로그인한 사용자가 이름을 붙여 저장한 가구 배치(공간마다 여러 개). furniture는 가구 목록 JSON 배열. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "space_room_layouts",
        schema = "spaces"
)
public class SpaceRoomLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String furniture;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SpaceRoomLayout create(Long spaceId, String userId, String name, String furniture) {
        SpaceRoomLayout layout = new SpaceRoomLayout();
        layout.spaceId = spaceId;
        layout.userId = userId;
        layout.update(name, furniture);
        return layout;
    }

    public void update(String name, String furniture) {
        this.name = name.trim();
        this.furniture = furniture;
    }
}
