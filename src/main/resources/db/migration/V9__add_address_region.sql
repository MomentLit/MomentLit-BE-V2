-- ============================================================
-- ADDRESS REGION (8 권역 그룹핑) + 기존 데이터 백필
-- ============================================================
ALTER TABLE spaces.addresses
    ADD COLUMN region VARCHAR(255);

ALTER TABLE spaces.addresses
    ADD CONSTRAINT chk_addresses_region CHECK (region IN (
        'SEOUL',
        'GYEONGGI_INCHEON',
        'BUSAN_GYEONGNAM',
        'DAEGU_GYEONGBUK',
        'DAEJEON_CHUNGCHEONG',
        'GWANGJU_JEOLLA',
        'GANGWON',
        'JEJU'
    ));

UPDATE spaces.addresses
SET region = CASE
    WHEN sido LIKE '%서울%' THEN 'SEOUL'
    WHEN sido LIKE '%인천%' OR sido LIKE '%경기%' THEN 'GYEONGGI_INCHEON'
    WHEN sido LIKE '%부산%' OR sido LIKE '%울산%' OR sido LIKE '%경남%' THEN 'BUSAN_GYEONGNAM'
    WHEN sido LIKE '%대구%' OR sido LIKE '%경북%' THEN 'DAEGU_GYEONGBUK'
    WHEN sido LIKE '%대전%' OR sido LIKE '%세종%' OR sido LIKE '%충남%' OR sido LIKE '%충북%' OR sido LIKE '%충청%' THEN 'DAEJEON_CHUNGCHEONG'
    WHEN sido LIKE '%광주%' OR sido LIKE '%전남%' OR sido LIKE '%전북%' OR sido LIKE '%전라%' THEN 'GWANGJU_JEOLLA'
    WHEN sido LIKE '%강원%' THEN 'GANGWON'
    WHEN sido LIKE '%제주%' THEN 'JEJU'
    ELSE NULL
END
WHERE region IS NULL;
