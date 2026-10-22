package com.example.space.dto.response;

import com.example.space.entity.SpacePanoramaSceneLink;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PanoramaSceneLinkResponse(
        @JsonProperty("target_scene_id")
        Long targetSceneId,

        Double yaw
) {

    public static PanoramaSceneLinkResponse from(SpacePanoramaSceneLink link) {
        return new PanoramaSceneLinkResponse(
                link.getToSceneId(),
                link.getYaw()
        );
    }
}
