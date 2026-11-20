package com.example.room.dto.request;

import com.example.room.global.exception.InvalidRoomException;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 방의 평면 구조. 좌표는 모두 미터, 바닥 평면의 [x, z]. 프론트는 이 값으로 3D 방을 다시 만든다.
 *
 * @param outer   바깥벽 다각형 꼭짓점 (순서대로, 닫는 점 반복 없음)
 * @param inner   안쪽 벽 선분 [[x1, z1], [x2, z2]] 목록 (파노라마 방은 보통 비어 있음)
 * @param camera  파노라마를 찍은 위치 [x, z]. 사진을 벽에 입히는 기준점 (도면 방은 null)
 * @param yaw     파노라마 사진의 가로 회전 보정(라디안). null이면 0
 * @param corners 모서리 추론/보정 결과 uv 쌍 [[u, v], ...] (0~1). 다시 편집할 때 쓰는 원본 값
 */
public record RoomGeometry(
        @NotNull List<List<Double>> outer,
        List<List<List<Double>>> inner,
        List<Double> camera,
        Double yaw,
        List<List<Double>> corners
) {

    static final int MAX_OUTER_POINTS = 64;
    static final int MAX_INNER_WALLS = 200;
    static final int MAX_CORNERS = 128;
    // 좌표 절댓값 상한(m) — 실내 공간으로 볼 수 없는 값과 잘못된 단위(mm 등)를 거른다
    static final double MAX_COORDINATE = 100.0;
    static final double MIN_AREA_M2 = 0.5;

    /** 구조가 올바른지 확인하고 바닥 면적(㎡)을 돌려준다. */
    public double validateAndComputeArea() {
        if (outer == null || outer.size() < 3) {
            throw new InvalidRoomException("바깥벽 꼭짓점은 3개 이상이어야 합니다.");
        }
        if (outer.size() > MAX_OUTER_POINTS) {
            throw new InvalidRoomException("바깥벽 꼭짓점은 " + MAX_OUTER_POINTS + "개까지입니다.");
        }
        outer.forEach(p -> requirePoint(p, "바깥벽 꼭짓점"));

        if (inner != null) {
            if (inner.size() > MAX_INNER_WALLS) {
                throw new InvalidRoomException("안쪽 벽은 " + MAX_INNER_WALLS + "개까지입니다.");
            }
            for (List<List<Double>> wall : inner) {
                if (wall == null || wall.size() != 2) {
                    throw new InvalidRoomException("안쪽 벽은 시작점과 끝점 두 개로 이루어져야 합니다.");
                }
                wall.forEach(p -> requirePoint(p, "안쪽 벽 끝점"));
            }
        }

        if (camera != null) {
            requirePoint(camera, "촬영 위치");
        }
        if (yaw != null && !Double.isFinite(yaw)) {
            throw new InvalidRoomException("사진 회전값이 올바르지 않습니다.");
        }

        if (corners != null) {
            if (corners.size() > MAX_CORNERS) {
                throw new InvalidRoomException("모서리 좌표가 너무 많습니다.");
            }
            for (List<Double> uv : corners) {
                if (uv == null || uv.size() != 2 || uv.stream().anyMatch(v -> v == null || !(v >= 0 && v <= 1))) {
                    throw new InvalidRoomException("모서리 좌표는 0~1 사이의 [u, v]여야 합니다.");
                }
            }
        }

        double area = Math.abs(signedArea());
        if (area < MIN_AREA_M2) {
            throw new InvalidRoomException("방 면적이 너무 작습니다. 꼭짓점이 한 줄로 놓였는지 확인해 주세요.");
        }
        return area;
    }

    /** 신발끈 공식. 꼭짓점 순서(시계/반시계)에 따라 부호가 바뀐다. */
    private double signedArea() {
        double sum = 0;
        for (int i = 0; i < outer.size(); i++) {
            List<Double> p = outer.get(i);
            List<Double> q = outer.get((i + 1) % outer.size());
            sum += p.get(0) * q.get(1) - q.get(0) * p.get(1);
        }
        return sum / 2;
    }

    private static void requirePoint(List<Double> point, String name) {
        if (point == null || point.size() != 2) {
            throw new InvalidRoomException(name + "은 [x, z] 두 값이어야 합니다.");
        }
        for (Double v : point) {
            if (v == null || !Double.isFinite(v) || Math.abs(v) > MAX_COORDINATE) {
                throw new InvalidRoomException(name + " 좌표는 ±" + (int) MAX_COORDINATE + "m 안의 숫자여야 합니다.");
            }
        }
    }
}
