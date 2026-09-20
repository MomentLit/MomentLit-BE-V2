package com.example.space.entity;

public enum Region {
    SEOUL,
    GYEONGGI_INCHEON,
    BUSAN_GYEONGNAM,
    DAEGU_GYEONGBUK,
    DAEJEON_CHUNGCHEONG,
    GWANGJU_JEOLLA,
    GANGWON,
    JEJU;

    /**
     * 자유 텍스트인 {@code sido} 값으로부터 권역(Region)을 유추한다.
     * 포함 관계(contains)로 판단하며, 공백은 무시하고 매칭한다.
     * 어떤 규칙에도 매칭되지 않으면 {@code null}을 반환한다.
     */
    public static Region fromSido(String sido) {
        if (sido == null) {
            return null;
        }

        String normalized = sido.replaceAll("\\s+", "");

        if (normalized.contains("서울")) {
            return SEOUL;
        }
        if (normalized.contains("인천") || normalized.contains("경기")) {
            return GYEONGGI_INCHEON;
        }
        if (normalized.contains("부산") || normalized.contains("울산") || normalized.contains("경남")) {
            return BUSAN_GYEONGNAM;
        }
        if (normalized.contains("대구") || normalized.contains("경북")) {
            return DAEGU_GYEONGBUK;
        }
        if (normalized.contains("대전")
                || normalized.contains("세종")
                || normalized.contains("충남")
                || normalized.contains("충북")
                || normalized.contains("충청")) {
            return DAEJEON_CHUNGCHEONG;
        }
        if (normalized.contains("광주")
                || normalized.contains("전남")
                || normalized.contains("전북")
                || normalized.contains("전라")) {
            return GWANGJU_JEOLLA;
        }
        if (normalized.contains("강원")) {
            return GANGWON;
        }
        if (normalized.contains("제주")) {
            return JEJU;
        }

        return null;
    }
}
