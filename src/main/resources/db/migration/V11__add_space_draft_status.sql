-- ============================================================
-- SPACE DRAFT STATUS (공간 등록 임시저장)
-- ============================================================
ALTER TABLE spaces.spaces
    DROP CONSTRAINT chk_spaces_admin_status;

ALTER TABLE spaces.spaces
    ADD CONSTRAINT chk_spaces_admin_status CHECK (admin_status IN (
        'DRAFT',
        'PENDING',
        'APPROVED',
        'REJECTED'
    ));
