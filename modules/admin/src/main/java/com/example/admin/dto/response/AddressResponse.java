package com.example.admin.dto.response;

public record AddressResponse(
        String sido,
        String sigungu,
        String eupMyeonDong,
        String roadAddress,
        String jibunAddress,
        String detailAddress,
        String postalCode
) {
    public static AddressResponse from(com.example.space.dto.response.AddressResponse address){
        return new AddressResponse(
                address.sido(),
                address.sigungu(),
                address.eupMyeonDong(),
                address.roadAddress(),
                address.jibunAddress(),
                address.detailAddress(),
                address.postalCode()
        );
    }
}
