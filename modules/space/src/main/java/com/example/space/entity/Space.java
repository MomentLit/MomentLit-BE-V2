package com.example.space.entity;


import com.example.space.global.exception.BadRequestException;
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

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price_per_hour", nullable = false)
    private Integer pricePerHour;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false)
    private ApprovalStatus adminStatus;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceCategory category;

    private String phone;

    private Double area;

    private Integer capacity;

    private String floor;

    @Column(name = "parking_info")
    private String parkingInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_unit")
    private UsageUnit usageUnit;

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
            String phone,
            Double area,
            Integer capacity,
            String floor,
            String parkingInfo,
            UsageUnit usageUnit,
            ApprovalStatus adminStatus
    ) {
        this.hostId = hostId;
        this.name = name;
        this.description = description;
        this.aiSummary = aiSummary;
        this.addressId = addressId;
        this.thumbnailUrl = thumbnailUrl;
        this.pricePerHour = pricePerHour;
        this.likeCount = 0;
        this.category = category;
        this.adminStatus = adminStatus;
        this.isActive = true;
        this.phone = phone;
        this.area = area;
        this.capacity = capacity;
        this.floor = floor;
        this.parkingInfo = parkingInfo;
        this.usageUnit = usageUnit;
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
            String phone,
            Double area,
            Integer capacity,
            String floor,
            String parkingInfo,
            UsageUnit usageUnit,
            Boolean isDraft
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
                .area(area)
                .capacity(capacity)
                .floor(floor)
                .parkingInfo(parkingInfo)
                .usageUnit(usageUnit)
                .adminStatus(Boolean.TRUE.equals(isDraft) ? ApprovalStatus.DRAFT : ApprovalStatus.PENDING)
                .build();
    }

    public void update(
            String name,
            String description,
            String aiSummary,
            String thumbnailUrl,
            Integer pricePerHour,
            SpaceCategory category,
            String phone,
            Double area,
            Integer capacity,
            String floor,
            String parkingInfo,
            UsageUnit usageUnit
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
        if (area != null) {
            this.area = area;
        }
        if (capacity != null) {
            this.capacity = capacity;
        }
        if (floor != null) {
            this.floor = floor;
        }
        if (parkingInfo != null) {
            this.parkingInfo = parkingInfo;
        }
        if (usageUnit != null) {
            this.usageUnit = usageUnit;
        }
    }

    public void updateAdminStatus(ApprovalStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    /**
     * 임시저장(DRAFT) ↔ 승인 대기(PENDING) 전환.
     * isDraft=true면 임시저장으로 되돌리고, false면 정식 제출(승인 대기)한다.
     * 이미 관리자가 처리(APPROVED/REJECTED)한 공간은 이 흐름으로 되돌릴 수 없다.
     */
    public void applyDraftTransition(Boolean isDraft) {
        if (isDraft == null) {
            return;
        }

        if (this.adminStatus == ApprovalStatus.APPROVED || this.adminStatus == ApprovalStatus.REJECTED) {
            throw new BadRequestException("승인 또는 거절된 공간은 임시저장 상태로 전환할 수 없습니다.");
        }

        this.adminStatus = isDraft ? ApprovalStatus.DRAFT : ApprovalStatus.PENDING;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }

    public boolean isOwner(String userId) {
        return this.hostId.equals(userId);
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
