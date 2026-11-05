-- ============================================================
-- SPACE PRICE INQUIRY
-- 호스트가 가격 대신 "가격 문의"를 고를 수 있다. 문의 공간은 price_per_hour가 NULL이다.
-- 기존 공간은 모두 가격이 있으므로 false로 채운다.
-- ============================================================
ALTER TABLE spaces.spaces
    ADD COLUMN price_inquiry BOOLEAN NOT NULL DEFAULT false,
    ALTER COLUMN price_per_hour DROP NOT NULL;

ALTER TABLE spaces.spaces
    ADD CONSTRAINT chk_spaces_price_or_inquiry CHECK (price_inquiry OR price_per_hour IS NOT NULL);
