-- V354 通票分账瀑布重建 S1(主体与归属层)·第 1 刀:business_entities 组织主数据落地。
--
-- 【业务依据】(最终解释权=三份业务 HTML,本迁移逐条对齐、不自造口径)
--   platform-governance-map-for-operations §「经营主体分四层：平台 → 项目公司 → 业态主体 → 商户」
--     与 §「六个业态主体各管什么」:娱乐平台公司 / 中央餐饮 / 游乐公司 / 传媒公司 /
--     资产公司(即长沙项目公司,资产业态不单设主体、由项目公司兼任);
--   business-enum-quick-reference §「六业态收入归属」:通票收入与门票综合服务费→娱乐平台公司;
--     向商户供货的货款→中央餐饮公司;游乐票→游乐公司;演出票→传媒公司;场地租金→长沙项目公司。
--
-- 【为什么现在做】新口径把「门票综合服务费 / 赠送权益预估成本 / 未核销权益款」的收款方
--   从「项目公司」改成「娱乐平台公司」,把「游乐票与游乐核销收入」归「游乐公司」——
--   而这些收款方在库里一行都不存在(现有 32 行全是 entity_role='operation' 的测试残留)。
--   对账时业务人员要能在后台看见「这笔钱归谁」,归属配置就必须能引用一个**稳定的主体 id**;
--   稳定 id 是后续一切归属配置(revenue_stream_attribution)能写死引用的前提。
--
-- 【零金额影响】business_entities 不参与任何金额计算,本迁移不改一分钱:
--   引擎只在结算单上标注「归属主体 id」(settlement_bills.attributed_entity_id),该列不进任何算式。
--
-- 【本迁移做四件事】
--   ① 补 archive_reason 列(V271 软删铁律覆盖的 11 张表不含本表);
--   ② 32 行 epoch 命名的测试残留按软删铁律归档(status→inactive + archive_reason,**不 DELETE**);
--   ③ seed 六个主体「骨架 + 稳定 id」——统一社会信用代码 / 法人 / 银行账户一律留 NULL,
--      由运营在后台补(工商信息不得由代码编造);
--   ④ entity_role 加白名单 CHECK(V005 建表至今该列无任何约束,打错字不会被拦)。
--
-- ⚠️【10 家酒吧不在本迁移,是刻意的】酒吧＝merchant_type='bar' 的**商户**(V267 已删独立 bars 表就是为这条),
--   必须走招商 / 商户准入正路录入;且用户明确「酒吧数量必须可配(可能七八家,也可能二三十家)」,
--   把 10 家固化进迁移链恰恰违背这条。开发环境要造数用 ops/160/seed-dev-bars.sh(家数是参数)。

-- ① 软删留痕列(软删铁律:删除=留库 + 前台不显 + 可恢复 + 填理由)
ALTER TABLE business_entities
    ADD COLUMN IF NOT EXISTS archive_reason TEXT;

COMMENT ON COLUMN business_entities.archive_reason IS
    '软删归档理由(软删铁律):status=inactive 时记录下线原因;主体一律不物理删除';

-- ② 测试残留归档(先归档、后 seed,顺序不可换:换了会把新骨架行一起归档掉)
--    判据:entity_role='operation' 且当前 active —— 开发库这 32 行全部是
--    「MZ活证据主体<epoch>」「P0 Android Org <epoch>」「S2主体<epoch>」这类 ops 证据脚本产物。
--    在全新测试库(complex_test*)上本语句命中 0 行,天然幂等。
UPDATE business_entities
   SET status         = 'inactive',
       archive_reason = '2026-07-28 主体主数据重建(S1):epoch 命名的测试残留主体统一归档,不参与任何收入归属配置',
       updated_at     = now()
 WHERE status = 'active'
   AND entity_role = 'operation';

-- ③ 六层主体骨架 seed(稳定 id;工商信息留空由运营后台补)
--    L1 平台公司                 —— 占位名,正式工商名称待运营确定后在后台改;
--    L2 长沙项目公司(= 资产公司,同一主体两个叫法)—— 场地租金归此主体;
--    L3 娱乐平台公司 / 中央餐饮公司 / 游乐公司 / 传媒公司。
--    资产公司**不建行**:governance 明写「资产业态不单设主体、由项目公司兼任」。
INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, updated_at)
VALUES
    ('be_platform',         '平台公司',     'platform',           NULL,               'active', now(), now()),
    ('be_proj_changsha',    '长沙项目公司', 'project_company',    'be_platform',      'active', now(), now()),
    ('be_ent_platform',     '娱乐平台公司', 'management_company', 'be_proj_changsha', 'active', now(), now()),
    ('be_central_catering', '中央餐饮公司', 'management_company', 'be_proj_changsha', 'active', now(), now()),
    ('be_amusement',        '游乐公司',     'management_company', 'be_proj_changsha', 'active', now(), now()),
    ('be_media',            '传媒公司',     'management_company', 'be_proj_changsha', 'active', now(), now())
ON CONFLICT (id) DO NOTHING;

-- ④ entity_role 白名单(四层组织 + 历史 operation)
--    实查:开发库现存值只有 'operation';测试夹具用 'project_company' / 'management_company';
--    ops 证据脚本用 'operation' —— 全部在白名单内,加约束零阻断。
ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
    CHECK (entity_role IN ('platform', 'project_company', 'management_company', 'spv', 'operation'));

COMMENT ON COLUMN business_entities.entity_role IS
    '主体在四层组织中的角色:platform=平台/project_company=项目公司/management_company=业态主体(管理公司)/spv/operation=历史通用运营主体';
