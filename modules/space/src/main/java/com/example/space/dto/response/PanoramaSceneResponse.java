package com.example.space.dto.response;

import com.example.space.entity.SpacePanoramaScene;
import com.example.space.entity.SpacePanoramaSceneLink;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PanoramaSceneResponse(
        @JsonProperty("scene_id")
        Long sceneId,

        @JsonProperty("image_url")
        String imageUrl,

        String name,

        @JsonProperty("sort_order")
        Integer sortOrder,

        List<PanoramaSceneLinkResponse> links
) {

    public static PanoramaSceneResponse from(
            SpacePanoramaScene scene,
            List<SpacePanoramaSceneLink> links
    ) {
        return new PanoramaSceneResponse(
                scene.getId(),
                scene.getImageUrl(),
                scene.getName(),
                scene.getSortOrder(),
                links.stream()
                        .map(PanoramaSceneLinkResponse::from)
                        .toList()
        );
    }

    /**
     * 장면마다 그 장면에서 나가는 화살표를 묶어 응답으로 바꾼다.
     */
    public static List<PanoramaSceneResponse> from(
            List<SpacePanoramaScene> scenes,
            List<SpacePanoramaSceneLink> links
    ) {
        return scenes.stream()
                .map(scene -> from(
                        scene,
                        links.stream()
                                .filter(link -> link.getFromSceneId().equals(scene.getId()))
                                .toList()
                ))
                .toList();
    }
}
