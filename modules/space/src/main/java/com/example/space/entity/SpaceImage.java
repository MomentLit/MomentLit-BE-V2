package com.example.space.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "space_images", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Builder(access = AccessLevel.PRIVATE)
    private SpaceImage(
            Long spaceId,
            String imageUrl
    ) {
        this.spaceId = spaceId;
        this.imageUrl = imageUrl;
    }

    public static SpaceImage create(
            Long spaceId,
            String imageUrl
    ) {
        return SpaceImage.builder()
                .spaceId(spaceId)
                .imageUrl(imageUrl)
                .build();
    }
}
