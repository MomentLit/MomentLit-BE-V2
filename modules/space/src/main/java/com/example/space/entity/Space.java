package com.example.space.entity;


import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.net.URI;
import java.net.URISyntaxException;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_summary_status", nullable = false)
    private AiSummaryStatus aiSummaryStatus;

    @Column(name = "ai_summary_version", nullable = false)
    private Long aiSummaryVersion;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "panorama_url")
    private String panoramaUrl;

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

    @Column(name = "min_capacity")
    private Integer minCapacity;

    @Column(name = "usage_notice", columnDefinition = "TEXT")
    private String usageNotice;

    private String floor;

    @Column(name = "parking_info")
    private String parkingInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_unit")
    private UsageUnit usageUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    private BookingType bookingType;

    @Column(name = "external_booking_url", length = 2048)
    private String externalBookingUrl;

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
        this.aiSummaryStatus = AiSummaryStatus.PENDING;
        this.aiSummaryVersion = 1L;
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
        this.bookingType = BookingType.INTERNAL;
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
                .phone(phone == null || phone.isBlank() ? null : phone.strip())
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
            this.phone = phone.isBlank() ? null : phone.strip();
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

    /**
     * 360도 사진 변경.
     * null이면 변경하지 않고, 빈 문자열이면 삭제하며, 그 외에는 새 URL로 교체한다.
     */
    public void updatePanoramaUrl(String panoramaUrl) {
        if (panoramaUrl == null) {
            return;
        }

        this.panoramaUrl = panoramaUrl.isBlank() ? null : panoramaUrl;
    }

    /**
     * 예약 방식/외부 예약 링크 변경.
     * 둘 다 null이면 변경하지 않는다.
     * 타입 없이 URL을 빈 문자열로 보내면 링크를 지우고 모먼트릿 예약(INTERNAL)으로 되돌린다.
     * 결과가 EXTERNAL이면 http(s) 링크가 반드시 있어야 하고, INTERNAL이면 링크를 비운다.
     */
    public void updateBooking(BookingType bookingType, String externalBookingUrl) {
        if (bookingType == null && externalBookingUrl == null) {
            return;
        }

        boolean clearsUrl = externalBookingUrl != null && externalBookingUrl.isBlank();
        BookingType nextType = bookingType != null
                ? bookingType
                : (clearsUrl ? BookingType.INTERNAL : this.bookingType);
        String nextUrl = externalBookingUrl != null
                ? (externalBookingUrl.isBlank() ? null : externalBookingUrl.trim())
                : this.externalBookingUrl;

        if (nextType == BookingType.INTERNAL) {
            this.bookingType = BookingType.INTERNAL;
            this.externalBookingUrl = null;
            return;
        }

        if (nextUrl == null) {
            throw new BadRequestException("외부 예약 공간은 예약 링크를 입력해야 합니다.");
        }
        validateExternalBookingUrl(nextUrl);

        this.bookingType = BookingType.EXTERNAL;
        this.externalBookingUrl = nextUrl;
    }

    public boolean isExternalBooking() {
        return this.bookingType == BookingType.EXTERNAL;
    }

    private static void validateExternalBookingUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new BadRequestException("예약 링크는 http:// 또는 https://로 시작하는 주소여야 합니다.");
            }
        } catch (URISyntaxException e) {
            throw new BadRequestException("예약 링크 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 이용 안내(문의·주의·이용사항) 변경. null이면 변경하지 않고, 빈 문자열이면 지운다.
     */
    public void updateUsageNotice(String usageNotice) {
        if (usageNotice == null) {
            return;
        }

        this.usageNotice = usageNotice.isBlank() ? null : usageNotice.strip();
    }

    /**
     * 최소 이용 인원 변경. null이면 변경하지 않고, 0이면 지운다.
     * 최대 인원(capacity)보다 크면 거부한다 — capacity를 먼저 반영한 뒤 호출해야 한다.
     */
    public void updateMinCapacity(Integer minCapacity) {
        if (minCapacity != null) {
            this.minCapacity = minCapacity == 0 ? null : minCapacity;
        }

        if (this.minCapacity != null && this.capacity != null && this.minCapacity > this.capacity) {
            throw new BadRequestException("최소 인원은 최대 인원보다 많을 수 없습니다.");
        }
    }

    public void updateAdminStatus(ApprovalStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    /** 요약 입력이 바뀌면 새 버전을 발급한다. 늦게 도착한 이전 AI 응답은 이 버전을 통과할 수 없다. */
    public void requestAiSummary() {
        this.aiSummary = null;
        this.aiSummaryStatus = AiSummaryStatus.PENDING;
        this.aiSummaryVersion++;
    }

    public boolean completeAiSummary(Long version, String summary) {
        if (!this.aiSummaryVersion.equals(version)) {
            return false;
        }
        this.aiSummary = summary;
        this.aiSummaryStatus = AiSummaryStatus.COMPLETED;
        return true;
    }

    public void failAiSummary(Long version) {
        if (this.aiSummaryVersion.equals(version)) {
            this.aiSummaryStatus = AiSummaryStatus.FAILED;
        }
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
