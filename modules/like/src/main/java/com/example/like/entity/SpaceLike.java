package com.example.like.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "space_likes",
        schema = "likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_space_likes_space_id_user_id",
                columnNames = {"space_id", "user_id"}
        )
)
public class SpaceLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "space_id")
    private Long spaceId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    public static SpaceLike create(Long spaceId, String userId) {
        return SpaceLike.builder()
                .spaceId(spaceId)
                .userId(userId)
                .build();
    }
}
