-- 服务目录软删框架(对抗审计Part1缺口:服务点单配置本不能存档/删除)。
-- 仿酒吧商品:存档必填理由;删除走 申请→审批(禁自审·审批用独立高权限 service.catalog.approve);
-- delete_status='approved' 作墓碑(全查询过滤·不硬删·留痕)。
ALTER TABLE service_catalog_items
  ADD COLUMN IF NOT EXISTS archived_at timestamptz,
  ADD COLUMN IF NOT EXISTS archived_by varchar(64),
  ADD COLUMN IF NOT EXISTS archive_reason text,
  ADD COLUMN IF NOT EXISTS delete_status varchar(16) NOT NULL DEFAULT 'none',
  ADD COLUMN IF NOT EXISTS delete_requested_by varchar(64),
  ADD COLUMN IF NOT EXISTS delete_requested_at timestamptz,
  ADD COLUMN IF NOT EXISTS delete_reason text;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('service.catalog.approve', '批准删除服务目录', 'service', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 审批权限先随 manage 一并授(禁自审在服务层强制);最高权限口径由角色配置决定。
INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'service.catalog.approve' FROM role_permissions WHERE permission_code = 'service.catalog.manage'
ON CONFLICT DO NOTHING;
