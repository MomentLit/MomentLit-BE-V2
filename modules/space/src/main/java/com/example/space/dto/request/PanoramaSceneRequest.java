package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 360도 장면 한 장. 목록의 첫 장면이 시작 장면이고, 장면 사이 이동은 links로 정한다.
 */
public record PanoramaSceneRequest(
        @JsonProperty("image_url")
        String imageUrl,

        @Size(max = 1000, message = "360도 장면 이름은 1000자 이하로 입력해 주세요.")
        String name,

        // null이거나 빈 목록이면 이 장면에서 나가는 화살표가 없다.
        List<PanoramaSceneLinkRequest> links
) {
}
