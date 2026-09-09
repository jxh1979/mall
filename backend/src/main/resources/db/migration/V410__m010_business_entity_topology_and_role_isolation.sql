-- 文件版本：V0.01
-- V410 审计 M010（2026-09-09）：主体拓扑与角色隔离修复。操作员裁定：新迁移自 V410 起编号（跳过未发布分支的 V400–V409）；
-- M010-001 删列已按 GEN-014 明确授权；M010-002/003 添加项按 2.2 答「删除」。历史迁移 V005/V048/V318/V354 正文不改。
-- 业务依据（三份 HTML 最终解释权）：BEQR-144 / PGM-018 / BSM-028「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链」；
-- PGM-018/019 主体角色只有顶层平台、城市项目公司、域主管公司（业态主体）三类。

-- M010-001：删除 V318 引入的固定父链列与索引（后台 M009-012 已不再读写；V354 六行主体骨架的稳定 id/名称/角色保留）。
DROP INDEX IF EXISTS idx_business_entities_parent;
ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;

-- M010-003 / M010-002：存量「operation」「spv」角色行（V354 已按软删铁律归档为 inactive）迁为域主管公司角色后，
-- 把 entity_role 白名单收为 HTML 三角色；不删除任何行。
UPDATE business_entities
   SET entity_role = 'management_company',
       updated_at  = now()
 WHERE entity_role IN ('operation', 'spv');
ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
    CHECK (entity_role IN ('platform', 'project_company', 'management_company'));
COMMENT ON COLUMN business_entities.entity_role IS
    '主体角色（PGM-018/019）:platform=顶层平台 / project_company=城市项目公司 / management_company=域主管公司（业态主体）';

-- M010-004：V048 组织隔离配套唯一性——全局角色（organization_id 为空）之间同名唯一、同一组织内同名唯一、不同组织可同名。
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_role_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_org_role_name
    ON roles ((COALESCE(organization_id, '')), role_name);
