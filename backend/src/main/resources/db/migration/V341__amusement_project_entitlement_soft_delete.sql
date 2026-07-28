-- V341 游乐项目↔权益类型绑定：物理删 → 软删（v7 审计 R-12 · P0 止血批）。
-- 证据：JdbcAmusementRepository.deleteProjectEntitlement 原为 `DELETE FROM amusement_project_entitlements`，
-- 解绑即抹行，违反删除铁律（删除=存库 + 前台不显 + 可恢复 + 分类 + 填理由 + 最高权限批准）。
--
-- 范式对齐 V255（试点）/ V260（票种）/ V271（archive_reason 11 表）/ V272（服务目录），字段名/类型/索引一字不改：
--   存档轴：archived_at 非空=已存档；archived_by 存档人；archive_reason 存档理由（服务层 requireText 强校验非空）。
--   删除轴：delete_status none -> pending（申请，须先存档、填理由）-> approved（墓碑）；
--           delete_requested_by / delete_requested_at / delete_reason 记申请人/时间/理由。
--   delete_status='approved' 作【墓碑】：行留库、全部读取路径过滤隐藏、可恢复，不物理删（PR#53 用户拍板口径）。
-- 分类：两轴即分类——「已存档（软下线可恢复）」与「已删除（审批后墓碑）」在库内分类留存，列表可分别筛出。
-- 最高权限批准：审批权限码 amusement.project_entitlement.approve（critical + requires_approval）仅授
--   role_platform_admin（系统内最高角色），发起权 amusement.project_entitlement.manage ≠ 审批权，申请人禁自审（服务层强制）。
--
-- 全列可空/带默认，向后兼容（既有行 delete_status 默认 'none' + archived_at NULL = 生效中），可独立回滚。

ALTER TABLE amusement_project_entitlements
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS archive_reason      TEXT,
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_amusement_project_entitlement_delete_status') THEN
        ALTER TABLE amusement_project_entitlements
            ADD CONSTRAINT chk_amusement_project_entitlement_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

-- 生效绑定的默认读取路径（核销绑定校验 / 管理列表默认视图）走此偏索引：未存档 + 未审批通过删除。
CREATE INDEX IF NOT EXISTS idx_amusement_project_entitlements_active_list
    ON amusement_project_entitlements (project_id, entitlement_type)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- 审批权限码（照 V255 ticket.entitlement.approve / V260 ticket.type.approve 口径：critical + requires_approval）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('amusement.project_entitlement.approve', '审批游乐项目权益绑定删除', 'amusement', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 最高权限批准：只授平台最高管理员（V261 口径：新权限迁移须同步授 role_platform_admin；此处也是唯一被授角色）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'amusement.project_entitlement.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;
