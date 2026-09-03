package com.example.space.dto.response;


import com.example.space.entity.Address;
import com.example.space.entity.Space;

import java.util.List;
import java.util.Map;

public record MySpaceListResponses(
        List<MySpaceListResponse> spaces
) {

    public static MySpaceListResponses from(
            List<Space> spaces,
            Map<Long, Address> addresses
    ) {
        return new MySpaceListResponses(
                spaces.stream()
                        .map(space -> MySpaceListResponse.from(
                                space,
                                AddressResponse.from(addresses.get(space.getAddressId()))
                        ))
                        .toList()
        );
    }
}
