-- 文件版本：V0.01
-- M736（XM-M001-D 可跨项目角色·批 1 权限治理；操作员 2026-10-01 说明：「权限配置表格当中有一个可以查看全部项目选项，
-- 只要有这个选项被勾选，那么对应的角色就可以看全部项目了。……那另外这个可以查看项目的选项，可以点击+打开。打开后，
-- 有每个独立的项目。那么可以逐个独立的项目去选那被选中的项目。对应的角色就可以看了，没被选中的对应的角色不能看。」）：
-- ① roles.view_all_projects：角色勾了「可以查看全部项目」。只存标记、不存清单，判定时取当时的项目集合
--    （操作员 2026-10-01 所选选项原文：「「全部项目」始终指所有项目，含以后新建的；只有展开后逐个勾选的，才只管勾中的那几个。」）。
--    默认值只表示未勾选：存量角色（含种子 role_platform_admin）一律未勾选，不按 data_scope 推断，不回填（AGENTS.md 第 8 条）。
-- ② role_viewable_projects：展开后逐个勾选的可查看项目，一行一条，随角色删除。viewable_id 取值落在两张表
--    （projects.id，或城市项目公司、在用的域主管公司的 business_entities.id；操作员 2026-10-03 所选选项原文：
--    「项目、城市项目公司、在用的域主管公司各列为独立条目，勾哪条只管哪条，互不连带。」），所以不加外键；
--    它是勾选的可查看目标、不是行归属（行归属随角色）。「勾了全部项目就不能再逐个勾选」跨两张表，由服务层在建角色时校验，不加检查约束。
-- ③ merchant_projects：一个商户对应多个项目的建模（操作员 2026-10-01 所选选项原文：「……并支持一个商户对应多个项目（新增建模）。」）。
--    结构同 V432 的 business_entity_projects。本迁移只建空表：不写行、不回填、不加级联，不改 merchants.project_id；
--    写入口、读取与审批另行落地。
-- ④ role_viewable_project_holds：待批条目——角色增加可查看项目时已持有该角色的人，对新增的这一条还没有重新审批通过
--    （操作员 2026-10-03 所选选项原文：「直接在原角色上勾 C；但张三等所有持有人要各自重新审批通过，才能看 C。」）。
--    以下为技术推导：存的是「还没批的」——改勾选时写、重新审批通过时删，改之后才得到角色的人没有行；
--    viewable_id 为 * 指「可以查看全部项目」这一勾；为 - 的是保留标记、不是可查看项目——角色原来一条勾选都没有、
--    第一次勾上项目时已持有它的人各记一条，重新审批通过之前这个角色对他仍只到本人项目（没有这条标记的人，
--    对他有效的勾选一条不剩时这个角色什么都不给）。只建空表，不回填（AGENTS.md 第 8 条）。
--    ponytail: 待批表按角色读写是小表扫描，行数上万时加 (role_id) 索引
-- ⑤ roles.view_all_removal_review_id、role_viewable_projects.removal_review_id：两个待减标记列，减少在待审时记那张主管复核单的编号
--    （操作员 2026-10-03 所选选项原文：「减少也要审批通过后才生效；审批期间持有人仍能看 C。」）。以下为技术推导：待减的行原样留着，
--    所以审批期间角色不变；不加外键到复核单表。授权申请类型（chk_access_grant_requests_type）加一个值 role_project_reapproval：
--    原持有人对新增项目的重新审批（④）。
ALTER TABLE roles
    ADD COLUMN view_all_projects BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN view_all_removal_review_id VARCHAR(64);

COMMENT ON COLUMN roles.view_all_projects IS
    '可以查看全部项目（含以后新建的项目）';

COMMENT ON COLUMN roles.view_all_removal_review_id IS
    '去掉「可以查看全部项目」的减少在待审时，记那张主管复核单的编号；空＝没有待审的减少';

CREATE TABLE role_viewable_projects (
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    viewable_id VARCHAR(64) NOT NULL,
    removal_review_id VARCHAR(64),
    PRIMARY KEY (role_id, viewable_id)
);

COMMENT ON TABLE role_viewable_projects IS
    '角色逐个勾选的可查看项目；viewable_id 取值是 projects.id，或城市项目公司、在用的域主管公司的 business_entities.id';

COMMENT ON COLUMN role_viewable_projects.removal_review_id IS
    '这一条的减少在待审时，记那张主管复核单的编号；空＝没有待审的减少。待审期间这一行照常生效';

CREATE TABLE role_viewable_project_holds (
    user_id VARCHAR(64) NOT NULL REFERENCES access_users(id) ON DELETE CASCADE,
    role_id VARCHAR(64) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    viewable_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role_id, viewable_id)
);

COMMENT ON TABLE role_viewable_project_holds IS
    '角色增加可查看项目时已持有该角色的人，对新增的这一条还没有重新审批通过；viewable_id 为 * 指「可以查看全部项目」这一勾，为 - 是保留标记（角色原来没有勾选、第一次勾上时已持有的人，重新审批通过前仍只到本人项目）';

ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_access_grant_requests_type;
ALTER TABLE access_grant_requests ADD CONSTRAINT chk_access_grant_requests_type
    CHECK (request_type IN ('user_role', 'user_position', 'temporary_permission', 'role_permission',
                            'user_scope_change', 'position_high_risk_release',
                            'user_permission_grant', 'user_permission_deny_removal',
                            'role_project_reapproval'));

CREATE TABLE merchant_projects (
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    PRIMARY KEY (merchant_id, project_id)
);

CREATE INDEX idx_merchant_projects_project_id
    ON merchant_projects(project_id);

COMMENT ON TABLE merchant_projects IS
    '商户经营项目＝merchants.project_id（主项目）加本表；本表只放主项目以外追加的项目';
