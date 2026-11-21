-- 文件版本：V0.01
-- V728：第十八批 BS 段第 373 条 373-R2（游乐营业中抽检最低频次与当班未抽检判定）。
-- 依据：BSM:1300「营业中抽检有最低频次要求，每隔多久至少抽检一次由后台配置（安全阈值，按项目可配，须同时配置上限与下限，
-- 出厂默认每 2 小时一次），达不到最低频次的视为当班未抽检」；决策台账 DEC-R3B-2026-08-04-30；DEC-CFG-2026-08-03-01「安全阈值可配但须
-- 最高权限审批并留痕」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」
-- 「按项目可配」取综合体项目（同 V447、V570），'__global__' 为出厂回落行（每 2 小时，上下限留空，项目行三项同填）。
-- 判定：每笔成功入场找它之前最近一次已提交巡检作锚点，起算点取锚点与该入场所属营业日起点较晚者，超过间隔仍有成功入场即记一条。

-- ① 抽检间隔（分钟）
CREATE TABLE IF NOT EXISTS amusement_spot_check_policies (
    project_id VARCHAR(64) PRIMARY KEY,
    interval_minutes INTEGER NOT NULL,
    min_minutes INTEGER,
    max_minutes INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_spot_check_policies_bounds CHECK (
        (min_minutes IS NULL AND max_minutes IS NULL AND interval_minutes >= 1)
        OR (min_minutes >= 1 AND min_minutes <= interval_minutes AND interval_minutes <= max_minutes))
);
INSERT INTO amusement_spot_check_policies (project_id, interval_minutes, min_minutes, max_minutes)
VALUES ('__global__', 120, NULL, NULL)
ON CONFLICT (project_id) DO NOTHING;
COMMENT ON TABLE amusement_spot_check_policies IS
    '游乐营业中抽检间隔（安全阈值，按综合体项目，调整须最高权限本人办理；无自有行用 __global__ 出厂行：每 2 小时一次）';
COMMENT ON COLUMN amusement_spot_check_policies.project_id IS '综合体项目；__global__ 为出厂回落行';
COMMENT ON COLUMN amusement_spot_check_policies.interval_minutes IS '每隔多少分钟至少抽检一次';

-- ② 当班未抽检记录（同一项目同一锚点只记最早那笔超间隔入场）
CREATE TABLE IF NOT EXISTS amusement_spot_check_misses (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects (id) ON DELETE CASCADE,
    complex_id VARCHAR(64) NOT NULL REFERENCES projects (id),
    anchor_at TIMESTAMPTZ NOT NULL,
    interval_minutes INTEGER NOT NULL,
    first_admission_at TIMESTAMPTZ NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_amusement_spot_check_misses_anchor UNIQUE (project_id, anchor_at)
);
COMMENT ON TABLE amusement_spot_check_misses IS '游乐当班未抽检记录（超过抽检间隔仍有客人成功入场即记一条，只判定留痕）';
COMMENT ON COLUMN amusement_spot_check_misses.complex_id IS '所属综合体项目（取该入场核销时锁定的综合体）';
COMMENT ON COLUMN amusement_spot_check_misses.anchor_at IS '上次巡检时间（该入场之前最近一次已提交巡检的生效时刻）';
COMMENT ON COLUMN amusement_spot_check_misses.interval_minutes IS '判定时所用抽检间隔（分钟）';
COMMENT ON COLUMN amusement_spot_check_misses.first_admission_at IS '超过间隔后首笔成功入场时刻';
COMMENT ON COLUMN amusement_spot_check_misses.detected_at IS '判定时刻';

-- ③ 判定只扫成功入场
CREATE INDEX IF NOT EXISTS idx_amusement_verifications_success_entry
    ON amusement_verifications (project_id, created_at)
    WHERE direction = 'entry' AND verification_result = 'success';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON amusement_spot_check_policies TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON amusement_spot_check_misses TO complex_app';
    END IF;
END $$;
