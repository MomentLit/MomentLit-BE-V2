package com.example.space.entity;

import java.util.Map;

/**
 * 권역(Region)별 대표 좌표 — 실제 지오코딩 API(카카오/네이버 등) 연동 없이 "가까운순"
 * 정렬을 동작시키기 위한 근사치다. 정확한 건물 단위 좌표가 아니라 각 권역의 대표 도시
 * 좌표라서, 같은 권역 안에서는 거리 차이가 반영되지 않는다 — 그래도 "서울에서 검색하면
 * 서울/경기 공간이 제주 공간보다 먼저 뜬다" 수준의 정렬은 충분히 의미가 있다.
 */
public final class RegionCoordinates {

    private RegionCoordinates() {
    }

    private static final Map<Region, double[]> COORDINATES = Map.of(
            Region.SEOUL, new double[]{37.5665, 126.9780},
            Region.GYEONGGI_INCHEON, new double[]{37.4563, 126.7052},
            Region.BUSAN_GYEONGNAM, new double[]{35.1796, 129.0756},
            Region.DAEGU_GYEONGBUK, new double[]{35.8714, 128.6014},
            Region.DAEJEON_CHUNGCHEONG, new double[]{36.3504, 127.3845},
            Region.GWANGJU_JEOLLA, new double[]{35.1595, 126.8526},
            Region.GANGWON, new double[]{37.8228, 128.1555},
            Region.JEJU, new double[]{33.4996, 126.5312}
    );

    /** 대한민국 대략 중심(대전 인근) — region이 없거나 매칭 안 될 때의 fallback. */
    private static final double[] FALLBACK = new double[]{36.5, 127.8};

    public static double latitudeOf(Region region) {
        return COORDINATES.getOrDefault(region, FALLBACK)[0];
    }

    public static double longitudeOf(Region region) {
        return COORDINATES.getOrDefault(region, FALLBACK)[1];
    }
}
