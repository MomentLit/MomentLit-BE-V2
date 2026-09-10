package com.example.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "popup_reviews",
        schema = "reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_popup_reviews_popup_id_user_id",
                columnNames = {"popup_id", "user_id"}
        )
)
public class PopupReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "popup_id")
    private Long popupId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "verification_type")
    private VerificationType verificationType;

    @Column(nullable = false, name = "verification_payload", columnDefinition = "text")
    private String verificationPayload;

    @Column(nullable = false, name = "is_verified")
    private Boolean isVerified;

    @Column(nullable = false, name = "like_count")
    private Integer likeCount;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public static PopupReview create(
            Long popupId,
            String userId,
            Integer rating,
            String content,
            VerificationType verificationType,
            String verificationPayload
    ) {
        return PopupReview.builder()
                .popupId(popupId)
                .userId(userId)
                .rating(rating)
                .content(content)
                .verificationType(verificationType)
                .verificationPayload(verificationPayload)
                .isVerified(false)
                .likeCount(0)
                .build();
    }

    public void update(String content) {
        this.content = content;
    }

    public boolean isWrittenBy(String userId) {
        return this.userId.equals(userId);
    }
}
