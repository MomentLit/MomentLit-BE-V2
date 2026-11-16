-- ============================================================
-- SPACE BOOKING RULES
-- 연속 예약(이어진 슬롯 여러 개를 한 번에 예약), 최소 이용 시간, 인원 초과 요금.
-- 기존 공간은 연속 예약 꺼짐 / 최소 시간 없음 / 초과 요금 없음으로 채워 지금과 똑같이 동작한다.
-- ============================================================
ALTER TABLE spaces.spaces
    ADD COLUMN allow_consecutive_booking BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN min_usage_hours  INTEGER,
    ADD COLUMN base_guest_count INTEGER,
    ADD COLUMN extra_guest_fee  INTEGER;

ALTER TABLE spaces.spaces
    ADD CONSTRAINT chk_spaces_min_usage_hours CHECK (min_usage_hours IS NULL OR min_usage_hours > 0),
    ADD CONSTRAINT chk_spaces_extra_guest_pair CHECK ((base_guest_count IS NULL) = (extra_guest_fee IS NULL)),
    ADD CONSTRAINT chk_spaces_extra_guest_values CHECK (
        base_guest_count IS NULL OR (base_guest_count > 0 AND extra_guest_fee > 0)
    );
