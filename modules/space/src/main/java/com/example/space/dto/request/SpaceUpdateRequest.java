package com.example.space.dto.request;

import com.example.space.entity.BookingType;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpaceUpdateRequest(
        String name,

        String description,

        AddressRequest address,

        @JsonProperty("ai_summary")
        String aiSummary,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        // null이면 변경 없음, 빈 문자열이면 360도 사진 삭제
        @JsonProperty("panorama_url")
        String panoramaUrl,

        @Positive
        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        SpaceCategory category,

        // 선택 입력. 입력하면 공간 상세에 공개된다. 수정 시 빈 문자열이면 삭제
        @Size(max = 20)
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,20}$", message = "연락처는 숫자, +, -, 괄호, 공백만 7~20자로 입력해 주세요.")
        String phone,

        @PositiveOrZero
        Double area,

        @PositiveOrZero
        Integer capacity,

        // 최소 이용 인원. 수정 시 null이면 변경 없음, 0이면 삭제
        @PositiveOrZero
        @JsonProperty("min_capacity")
        Integer minCapacity,

        // 문의·주의·이용사항. 수정 시 null이면 변경 없음, 빈 문자열이면 삭제
        @Size(max = 5000)
        @JsonProperty("usage_notice")
        String usageNotice,

        String floor,

        @JsonProperty("parking_info")
        String parkingInfo,

        @JsonProperty("usage_unit")
        UsageUnit usageUnit,

        @JsonProperty("booking_type")
        BookingType bookingType,

        // EXTERNAL일 때 필수. 수정 시 빈 문자열이면 링크 삭제
        @Size(max = 2048)
        @JsonProperty("external_booking_url")
        String externalBookingUrl,

        @JsonProperty("is_draft")
        Boolean isDraft
) {
    /** 소개 문구의 근거가 되는 필드가 하나라도 바뀌면 비동기 재생성한다. */
    public boolean hasAiSummarySourceChanges() {
        return name != null
                || description != null
                || address != null
                || pricePerHour != null
                || category != null
                || area != null
                || capacity != null
                || minCapacity != null
                || floor != null
                || parkingInfo != null
                || usageUnit != null;
    }
}
