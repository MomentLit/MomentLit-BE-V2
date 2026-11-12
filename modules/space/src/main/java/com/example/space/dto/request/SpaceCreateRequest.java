package com.example.space.dto.request;

import com.example.space.entity.BookingType;
import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpaceCreateRequest(
        @Size(max = 1000, message = "공간 이름은 구역 이름을 포함해 1000자 이하로 입력해 주세요.")
        String name,

        String description,

        @Valid
        AddressRequest address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("panorama_url")
        String panoramaUrl,

        // 목록 순서대로 앞뒤 장면이 이어진다. 있으면 panorama_url 대신 첫 장면 URL을 저장한다.
        @JsonProperty("panorama_scenes")
        List<@Valid PanoramaSceneRequest> panoramaScenes,

        // price_inquiry가 true면 무시하고 비운다. 아니면 필수이며 양수여야 한다 (Space에서 검증)
        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        // 가격 대신 "가격 문의"로 노출. 등록 시 null이면 false
        @JsonProperty("price_inquiry")
        Boolean priceInquiry,

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

        @Size(max = 1000, message = "층 정보는 1000자 이하로 입력해 주세요.")
        String floor,

        @Size(max = 1000, message = "주차 정보는 1000자 이하로 입력해 주세요.")
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
        Boolean isDraft,

        // 이어진 예약 슬롯 여러 개를 한 번에 예약받을지. 등록 시 null이면 false, 수정 시 null이면 변경 없음
        @JsonProperty("allow_consecutive_booking")
        Boolean allowConsecutiveBooking,

        // 시간 단위 공간의 최소 이용 시간. 수정 시 null이면 변경 없음, 0이면 삭제
        @PositiveOrZero
        @JsonProperty("min_usage_hours")
        Integer minUsageHours,

        // 인원 초과 요금의 기준 인원. 처음 설정할 때는 extra_guest_fee와 함께 보낸다
        @PositiveOrZero
        @JsonProperty("base_guest_count")
        Integer baseGuestCount,

        // 기준 인원 초과 1인당 추가 요금(이용 단위당). 수정 시 null이면 변경 없음, 0이면 기준 인원과 함께 삭제
        @PositiveOrZero
        @JsonProperty("extra_guest_fee")
        Integer extraGuestFee
) {
}
