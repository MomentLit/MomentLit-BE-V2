-- ============================================================
-- ADDRESS LATITUDE/LONGITUDE ("가까운순" 정렬용) + 기존 데이터 백필
-- 실제 지오코딩 API 연동이 없어서 권역(region)별 대표 좌표로 근사한다
-- (RegionCoordinates.java와 값을 반드시 맞출 것).
-- ============================================================
ALTER TABLE spaces.addresses
    ADD COLUMN latitude DOUBLE PRECISION;

ALTER TABLE spaces.addresses
    ADD COLUMN longitude DOUBLE PRECISION;

UPDATE spaces.addresses
SET latitude = CASE region
        WHEN 'SEOUL' THEN 37.5665
        WHEN 'GYEONGGI_INCHEON' THEN 37.4563
        WHEN 'BUSAN_GYEONGNAM' THEN 35.1796
        WHEN 'DAEGU_GYEONGBUK' THEN 35.8714
        WHEN 'DAEJEON_CHUNGCHEONG' THEN 36.3504
        WHEN 'GWANGJU_JEOLLA' THEN 35.1595
        WHEN 'GANGWON' THEN 37.8228
        WHEN 'JEJU' THEN 33.4996
        ELSE 36.5
    END,
    longitude = CASE region
        WHEN 'SEOUL' THEN 126.9780
        WHEN 'GYEONGGI_INCHEON' THEN 126.7052
        WHEN 'BUSAN_GYEONGNAM' THEN 129.0756
        WHEN 'DAEGU_GYEONGBUK' THEN 128.6014
        WHEN 'DAEJEON_CHUNGCHEONG' THEN 127.3845
        WHEN 'GWANGJU_JEOLLA' THEN 126.8526
        WHEN 'GANGWON' THEN 128.1555
        WHEN 'JEJU' THEN 126.5312
        ELSE 127.8
    END
WHERE latitude IS NULL;
