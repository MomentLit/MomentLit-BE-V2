package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressRequest(
        String sido,

        String sigungu,

        @JsonProperty("eup_myeon_dong")
        String eupMyeonDong,

        @JsonProperty("road_address")
        String roadAddress,

        @JsonProperty("jibun_address")
        String jibunAddress,

        @JsonProperty("detail_address")
        String detailAddress,

        @JsonProperty("postal_code")
        String postalCode
) {
}
