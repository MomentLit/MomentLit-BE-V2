package com.example.room.service;

import java.util.List;

/**
 * 방 모서리 추론 작업 하나. Redis에 JSON으로 저장해서 BE 인스턴스가 여러 대여도 어느 쪽에서든 조회된다.
 *
 * @param createdAt 생성 시각(epoch ms)
 */
public record LayoutJob(
        String id,
        String ownerId,
        Status status,
        Result result,
        String error,
        long createdAt
) {

    public enum Status {
        QUEUED,
        RUNNING,
        SUCCEEDED,
        FAILED
    }

    /**
     * @param imageUrl 수평을 맞춘 파노라마(S3). uv 좌표는 이 사진 기준이다
     * @param uv       (천장, 바닥) 쌍의 0~1 좌표
     * @param z0       카메라→천장 거리 (임의 단위)
     * @param z1       카메라→바닥 거리 (같은 단위, 음수)
     */
    public record Result(
            String imageUrl,
            List<List<Double>> uv,
            double z0,
            double z1,
            boolean aligned
    ) {
    }

    public static LayoutJob queued(String id, String ownerId, long now) {
        return new LayoutJob(id, ownerId, Status.QUEUED, null, null, now);
    }

    public LayoutJob running() {
        return new LayoutJob(id, ownerId, Status.RUNNING, null, null, createdAt);
    }

    public LayoutJob succeeded(Result result) {
        return new LayoutJob(id, ownerId, Status.SUCCEEDED, result, null, createdAt);
    }

    public LayoutJob failed(String error) {
        return new LayoutJob(id, ownerId, Status.FAILED, null, error, createdAt);
    }
}
