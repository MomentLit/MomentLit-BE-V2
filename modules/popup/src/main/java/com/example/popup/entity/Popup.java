package com.example.popup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Table(
        name = "popups",
        schema = "popups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_popups_matching_id", columnNames = "matching_id")
        }
)
public class Popup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "matching_id", unique = true)
    private Long matchingId;

    @Column(nullable = false, name = "space_id")
    private Long spaceId;

    @Column(nullable = false, name = "seller_id")
    private String sellerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "ai_brand_summary", columnDefinition = "text")
    private String aiBrandSummary;

    @Column(nullable = false, name = "start_time")
    private LocalDateTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, name = "view_count")
    private Integer viewCount;

    @Column(nullable = false, name = "like_count")
    private Integer likeCount;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    public static Popup create(
            Long matchingId,
            Long spaceId,
            String sellerId,
            String title,
            String description,
            String thumbnailUrl,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return Popup.builder()
                .matchingId(matchingId)
                .spaceId(spaceId)
                .sellerId(sellerId)
                .title(title)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .startTime(startTime)
                .endTime(endTime)
                .viewCount(0)
                .likeCount(0)
                .build();
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
