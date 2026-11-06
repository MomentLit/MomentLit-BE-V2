package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        String sido,

        String sigungu,

        @JsonProperty("eup_myeon_dong")
        String eupMyeonDong,

        @JsonProperty("road_address")
        String roadAddress,

        @JsonProperty("jibun_address")
        String jibunAddress,

        @Size(max = 1000, message = "상세 주소는 1000자 이하로 입력해 주세요.")
        @JsonProperty("detail_address")
        String detailAddress,

        @JsonProperty("postal_code")
        String postalCode
) {
}
