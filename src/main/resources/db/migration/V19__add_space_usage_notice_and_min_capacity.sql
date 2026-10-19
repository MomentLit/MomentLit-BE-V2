-- ============================================================
-- SPACE USAGE NOTICE / MIN CAPACITY
-- usage_notice: 문의 방법·주의사항·이용 규칙 등 호스트가 자유롭게 쓰는 안내 (공간 공통)
-- min_capacity: 최소 이용 인원. capacity(최대 인원)와 함께 "최소 ~ 최대" 범위를 이룬다.
-- ============================================================
ALTER TABLE spaces.spaces
    ADD COLUMN usage_notice TEXT,
    ADD COLUMN min_capacity INTEGER;

ALTER TABLE spaces.spaces
    ADD CONSTRAINT chk_spaces_min_capacity_positive CHECK (min_capacity IS NULL OR min_capacity > 0);
