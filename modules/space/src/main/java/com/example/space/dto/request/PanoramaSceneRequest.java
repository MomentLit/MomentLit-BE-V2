package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 360도 장면 한 장. 목록의 첫 장면이 시작 장면이고, 장면 사이 이동은 links로 정한다.
 */
public record PanoramaSceneRequest(
        @JsonProperty("image_url")
        String imageUrl,

        String name,

        // null이거나 빈 목록이면 이 장면에서 나가는 화살표가 없다.
        List<PanoramaSceneLinkRequest> links
) {
}
