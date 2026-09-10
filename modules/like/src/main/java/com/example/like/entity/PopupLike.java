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
        name = "popup_likes",
        schema = "likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_popup_likes_popup_id_user_id",
                columnNames = {"popup_id", "user_id"}
        )
)
public class PopupLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "popup_id")
    private Long popupId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    public static PopupLike create(Long popupId, String userId) {
        return PopupLike.builder()
                .popupId(popupId)
                .userId(userId)
                .build();
    }
}
