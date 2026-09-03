package com.example.space.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Getter
@Entity
@Table(name = "spaces", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_id", nullable = false)
    private String hostId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "thumbnail_url", nullable = false)
    private String thumbnailUrl;

    @Column(name = "price_per_hour", nullable = false)
    private Integer pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false)
    private ApprovalStatus adminStatus;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceCategory category;

    private String phone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Space(
            String hostId,
            String name,
            String description,
            String aiSummary,
            Long addressId,
            String thumbnailUrl,
            Integer pricePerHour,
            SpaceCategory category,
            String phone
    ) {
        this.hostId = hostId;
        this.name = name;
        this.description = description;
        this.aiSummary = aiSummary;
        this.addressId = addressId;
        this.thumbnailUrl = thumbnailUrl;
        this.pricePerHour = pricePerHour;
        this.category = category;
        this.adminStatus = ApprovalStatus.PENDING;
        this.isActive = true;
        this.phone = phone;
    }

    public static Space create(
            String hostId,
            String name,
            String description,
            String aiSummary,
            Long addressId,
            String thumbnailUrl,
            Integer pricePerHour,
            SpaceCategory category,
            String phone
    ) {
        return Space.builder()
                .hostId(hostId)
                .name(name)
                .description(description)
                .aiSummary(aiSummary)
                .addressId(addressId)
                .thumbnailUrl(thumbnailUrl)
                .pricePerHour(pricePerHour)
                .category(category)
                .phone(phone)
                .build();
    }

    public void update(
            String name,
            String description,
            String aiSummary,
            String thumbnailUrl,
            Integer pricePerHour,
            SpaceCategory category,
            String phone
    ) {
        if (name != null) {
            this.name = name;
        }

        if (description != null) {
            this.description = description;
        }

        if (aiSummary != null) {
            this.aiSummary = aiSummary;
        }

        if (thumbnailUrl != null) {
            this.thumbnailUrl = thumbnailUrl;
        }

        if (pricePerHour != null) {
            this.pricePerHour = pricePerHour;
        }

        if (category != null) {
            this.category = category;
        }
        if (phone != null) {
            this.phone = phone;
        }
    }

    public void updateAdminStatus(ApprovalStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }

    public boolean isOwner(String userId) {
        return this.hostId.equals(userId);
    }
}
