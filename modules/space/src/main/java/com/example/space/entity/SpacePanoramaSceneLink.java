package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 360도 장면 사이 이동 화살표 하나. 출발 장면의 yaw(수평 방향, 라디안) 위치에 도착 장면으로 가는 화살표를 둔다.
 */
@Getter
@Entity
@Table(name = "space_panorama_scene_links", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePanoramaSceneLink {

    private static final double FULL_TURN = Math.PI * 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "from_scene_id", nullable = false)
    private Long fromSceneId;

    @Column(name = "to_scene_id", nullable = false)
    private Long toSceneId;

    @Column(nullable = false)
    private Double yaw;

    @Builder(access = AccessLevel.PRIVATE)
    private SpacePanoramaSceneLink(
            Long spaceId,
            Long fromSceneId,
            Long toSceneId,
            Double yaw
    ) {
        this.spaceId = spaceId;
        this.fromSceneId = fromSceneId;
        this.toSceneId = toSceneId;
        this.yaw = yaw;
    }

    public static SpacePanoramaSceneLink create(
            Long spaceId,
            Long fromSceneId,
            Long toSceneId,
            Double yaw
    ) {
        if (fromSceneId.equals(toSceneId)) {
            throw new BadRequestException("360도 장면은 자기 자신으로 이동할 수 없습니다.");
        }

        if (yaw == null || yaw.isNaN() || yaw < 0 || yaw >= FULL_TURN) {
            throw new BadRequestException("이동 화살표 방향은 0 이상 2π 미만이어야 합니다.");
        }

        return SpacePanoramaSceneLink.builder()
                .spaceId(spaceId)
                .fromSceneId(fromSceneId)
                .toSceneId(toSceneId)
                .yaw(yaw)
                .build();
    }
}
