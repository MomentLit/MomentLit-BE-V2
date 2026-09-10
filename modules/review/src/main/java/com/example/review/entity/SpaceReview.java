package com.example.review.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "space_reviews",
        schema = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_space_reviews_matching_id", columnNames = "matching_id")
)
public class SpaceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "matching_id", unique = true)
    private Long matchingId;

    @Column(nullable = false, name = "space_id")
    private Long spaceId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, name = "like_count")
    private Integer likeCount;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public static SpaceReview create(Long matchingId, Long spaceId, String userId, Integer rating, String content) {
        return SpaceReview.builder()
                .matchingId(matchingId)
                .spaceId(spaceId)
                .userId(userId)
                .rating(rating)
                .content(content)
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
