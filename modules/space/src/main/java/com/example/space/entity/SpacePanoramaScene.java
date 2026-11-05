package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공간의 360도 장면 한 장. 같은 공간의 장면끼리는 sortOrder 순서대로 앞뒤로 이어진다.
 */
@Getter
@Entity
@Table(name = "space_panorama_scenes", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePanoramaScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder(access = AccessLevel.PRIVATE)
    private SpacePanoramaScene(
            Long spaceId,
            String imageUrl,
            String name,
            Integer sortOrder
    ) {
        this.spaceId = spaceId;
        this.imageUrl = imageUrl;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public static SpacePanoramaScene create(
            Long spaceId,
            String imageUrl,
            String name,
            Integer sortOrder
    ) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("360도 장면의 사진 URL은 필수입니다.");
        }

        return SpacePanoramaScene.builder()
                .spaceId(spaceId)
                .imageUrl(imageUrl)
                .name((name == null || name.isBlank()) ? null : name.trim())
                .sortOrder(sortOrder)
                .build();
    }
}
