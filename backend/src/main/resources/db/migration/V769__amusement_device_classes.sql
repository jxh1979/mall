-- 文件版本：V0.01
-- V769：第十八批 CF 段第 407 条 407-R1、407-R2（游乐设备关键／辅助分级与依赖关系、分级变更历史、辅助设备停用后能否继续运行的判定留痕）。
-- 依据：BSM:1271「每个游乐项目可以关联多台设备，并区分「关键安全设备」与「辅助设备」……辅助设备只停用该台设备本身，项目是否可以在其余条件下继续运行，
-- 由工程岗位或安全岗位判断……判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件，没有留痕的一律按停止运行处理；关键安全设备……
-- 每台关键安全设备属于哪一种，须在设备台账中登记……登记内容变更须经工程或安全审批。项目上线后要把某台关键安全设备改为辅助设备，必须重新经过工程或
-- 安全审批，并且只能在该项目停运后生效」；ENUM:147；决策台账 DEC-R3B-2026-08-04-22、「项目首次上线前，由游乐公司提出设备关键／辅助分类，工程或安全岗位
-- 复核通过后生效」「设备分类和每次变更必须保留申请人、复核人、理由、生效时间和历史版本」。研发阶段无存量（UD-07），不回填。

-- ① 设备分级（生效值；复核通过才写入，两列同时为空＝分级还没生效）
ALTER TABLE amusement_devices ADD COLUMN safety_class VARCHAR(16);
ALTER TABLE amusement_devices ADD COLUMN dependency VARCHAR(16);
ALTER TABLE amusement_devices ADD CONSTRAINT chk_amusement_devices_safety_class
    CHECK ((safety_class IS NULL AND dependency IS NULL)
        OR (safety_class = 'auxiliary' AND dependency IS NULL)
        OR (safety_class = 'critical' AND dependency IN ('dependent', 'independent')));
COMMENT ON COLUMN amusement_devices.safety_class IS '设备分级：critical 关键安全设备、auxiliary 辅助设备；为空表示分级还没经工程或安全复核生效';
COMMENT ON COLUMN amusement_devices.dependency IS '关键安全设备的依赖关系：dependent 依赖（必经环节）、independent 不依赖（可顶替或可绕过）；辅助设备为空';

-- ② 分级申请与历史（首次分级与每次变更各一行，经工程或安全复核；同一设备同时只许一条待复核）
CREATE TABLE IF NOT EXISTS amusement_device_class_changes (
    id VARCHAR(64) PRIMARY KEY,
    device_id VARCHAR(64) NOT NULL REFERENCES amusement_devices (id) ON DELETE CASCADE,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects (id) ON DELETE CASCADE,
    from_class VARCHAR(16),
    from_dependency VARCHAR(16),
    to_class VARCHAR(16) NOT NULL,
    to_dependency VARCHAR(16),
    reason TEXT NOT NULL,
    attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL,
    review_id VARCHAR(64),
    requested_by VARCHAR(128) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    decided_by VARCHAR(128),
    decided_at TIMESTAMPTZ,
    CONSTRAINT chk_amusement_device_class_changes_to
        CHECK ((to_class = 'auxiliary' AND to_dependency IS NULL)
            OR (to_class = 'critical' AND to_dependency IN ('dependent', 'independent'))),
    CONSTRAINT chk_amusement_device_class_changes_status CHECK (status IN ('pending_review', 'approved', 'rejected'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_device_class_changes_pending
    ON amusement_device_class_changes (device_id) WHERE status = 'pending_review';
CREATE INDEX IF NOT EXISTS idx_amusement_device_class_changes_project ON amusement_device_class_changes (project_id, requested_at);
COMMENT ON TABLE amusement_device_class_changes IS '游乐设备关键／辅助分级申请与历史：申请人、复核人、理由、生效时间；复核通过才写入设备分级';
COMMENT ON COLUMN amusement_device_class_changes.status IS 'pending_review 待工程或安全复核、approved 已生效、rejected 已驳回';
COMMENT ON COLUMN amusement_device_class_changes.decided_at IS '复核结案时刻（approved 即生效时间）';

-- ③ 辅助设备停用后项目能否继续运行的判定（谁判的、什么时间判的、依据了哪几项条件；只认本次停用起点之后的判定）
CREATE TABLE IF NOT EXISTS amusement_device_continue_judgements (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects (id) ON DELETE CASCADE,
    device_id VARCHAR(64) NOT NULL REFERENCES amusement_devices (id) ON DELETE CASCADE,
    conclusion VARCHAR(16) NOT NULL,
    conditions JSONB NOT NULL,
    judged_by VARCHAR(128) NOT NULL,
    judged_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_device_continue_judgements_conclusion CHECK (conclusion IN ('continue', 'stop')),
    CONSTRAINT chk_amusement_device_continue_judgements_conditions
        CHECK (jsonb_typeof(conditions) = 'array' AND jsonb_array_length(conditions) > 0)
);
CREATE INDEX IF NOT EXISTS idx_amusement_device_continue_judgements_device ON amusement_device_continue_judgements (device_id, judged_at);
COMMENT ON TABLE amusement_device_continue_judgements IS '辅助设备停用后项目能否继续运行的判定（工程或安全岗位）：continue 可继续运行、stop 停止运行';
COMMENT ON COLUMN amusement_device_continue_judgements.conditions IS '判定依据的条件（逐项文字）';

-- ④ 权限码（范式同 V570）：工程或安全岗位复核分级、判定辅助设备停用后能否继续运行（由运营分配给工程或安全岗位角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('amusement.device.class.approve', '复核游乐设备关键／辅助分级（工程或安全岗位）', 'amusement', 'high', FALSE, 'active'),
       ('amusement.device.continue_judge', '判定辅助设备停用后项目能否继续运行（工程或安全岗位）', 'amusement', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'amusement.device.class.approve'),
       ('role_platform_admin', 'amusement.device.continue_judge')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON amusement_device_class_changes TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON amusement_device_continue_judgements TO complex_app';
    END IF;
END $$;
