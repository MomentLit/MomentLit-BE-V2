package com.example.space.dto.response;

import com.example.space.entity.Address;
import com.example.space.entity.Space;

import java.util.List;
import java.util.Map;

public record AdminSpaceListResponses(
        List<AdminSpaceListResponse> spaces
) {

    public static AdminSpaceListResponses from(
            List<Space> spaces,
            Map<Long, Address> addresses
    ) {
        return new AdminSpaceListResponses(
                spaces.stream()
                        .map(space -> AdminSpaceListResponse.from(
                                space,
                                AddressResponse.from(addresses.get(space.getAddressId()))
                        ))
                        .toList()
        );
    }
}
