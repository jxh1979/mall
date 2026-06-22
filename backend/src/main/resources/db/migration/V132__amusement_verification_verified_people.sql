-- R1-LOOP-12: 游乐多人票核销必须能审计实际放行人数；容量扣减按该人数整体占用。
ALTER TABLE amusement_verifications
    ADD COLUMN IF NOT EXISTS verified_people INTEGER NOT NULL DEFAULT 1;

ALTER TABLE amusement_verifications
    DROP CONSTRAINT IF EXISTS chk_amusement_verifications_verified_people;
ALTER TABLE amusement_verifications
    ADD CONSTRAINT chk_amusement_verifications_verified_people CHECK (verified_people > 0);
