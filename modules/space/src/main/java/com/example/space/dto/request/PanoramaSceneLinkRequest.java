package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 장면에서 다른 장면으로 가는 화살표 하나.
 * 수정할 때 장면을 모두 새로 저장해서 id가 바뀌므로, 도착 장면은 같은 요청의 panorama_scenes 안 위치로 가리킨다.
 */
public record PanoramaSceneLinkRequest(
        @JsonProperty("target_index")
        Integer targetIndex,

        // 출발 장면에서 화살표가 놓이는 수평 방향(라디안, 0 이상 2π 미만)
        Double yaw
) {
}
