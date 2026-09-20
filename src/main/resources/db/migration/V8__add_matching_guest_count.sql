-- ============================================================
-- MATCHING GUEST COUNT
-- ============================================================
ALTER TABLE matchings.matchings
    ADD COLUMN guest_count INTEGER;
