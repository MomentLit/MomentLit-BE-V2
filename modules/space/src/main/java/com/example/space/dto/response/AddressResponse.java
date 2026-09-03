package com.example.space.dto.response;

import com.example.space.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressResponse(
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

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getSido(),
                address.getSigungu(),
                address.getEupMyeonDong(),
                address.getRoadAddress(),
                address.getJibunAddress(),
                address.getDetailAddress(),
                address.getPostalCode()
        );
    }
}
