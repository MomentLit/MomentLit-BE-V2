-- ============================================================
-- SPACE DETAIL FIELDS
-- ============================================================
ALTER TABLE spaces.spaces
    ADD COLUMN area           DOUBLE PRECISION,
    ADD COLUMN capacity       INTEGER,
    ADD COLUMN floor          VARCHAR(255),
    ADD COLUMN parking_info   VARCHAR(255),
    ADD COLUMN usage_unit     VARCHAR(255);

ALTER TABLE spaces.spaces
    ADD CONSTRAINT chk_spaces_usage_unit CHECK (usage_unit IN ('HOURLY', 'DAILY'));
