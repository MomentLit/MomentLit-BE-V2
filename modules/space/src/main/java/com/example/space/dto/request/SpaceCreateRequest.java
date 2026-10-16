package com.example.space.dto.request;

import com.example.space.entity.BookingType;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpaceCreateRequest(
        String name,

        String description,

        AddressRequest address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("panorama_url")
        String panoramaUrl,

        @Positive
        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        SpaceCategory category,

        String phone,

        @PositiveOrZero
        Double area,

        @PositiveOrZero
        Integer capacity,

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
}
