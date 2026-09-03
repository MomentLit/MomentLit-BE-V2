package com.example.space.dto.response;

import com.example.space.entity.Address;
import com.example.space.entity.Space;

import java.util.List;
import java.util.Map;

public record SpaceListResponses(
        List<SpaceListResponse> spaces
) {

    public static SpaceListResponses from(
            List<Space> spaces,
            Map<Long, Address> addresses
    ) {
        return new SpaceListResponses(
                spaces.stream()
                        .map(space -> SpaceListResponse.from(
                                space,
                                AddressResponse.from(addresses.get(space.getAddressId()))
                        ))
                        .toList()
        );
    }
}
