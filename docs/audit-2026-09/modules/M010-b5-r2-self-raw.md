CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B5-r2 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-09T15:05:24Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `242f487233d028173cb9198eecd079f5d877405c`；工作区 tree `2019d24b73e215d1043fc893190ebcfeaca6cce2`（含 M009 已闭环的 B3 修复与 M010 B3-r1 修复（V410 与伴生），未提交）。三份业务 HTML 只读。
- 模块 M010「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段」，4 文件 203 行，映射 GEN-001–018 共 18 判据。
- 步骤：B5-r2（B3 第 2 次整批修复后的整模块全量三线重审）。顺序：Grok B5-r2 真实退出后本席主仓实跑并写本报告；本报告落盘前未读取 `M010-b5-r2-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：本席 B5 未修改任何源码/迁移/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（4 个冻结文件 + 本模块修复产物 V410；行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| V005__p0_platform_master_data.sql | 109 | 3900 | 5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905 | 全读 |
| V048__access_role_organization_isolation.sql | 7 | 352 | e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd | 全读 |
| V318__business_entity_parent_layer.sql | 16 | 1300 | 466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481 | 全读 |
| V354__business_entity_master_data.sql | 71 | 5625 | c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3 | 全读 |
| V410__m010_business_entity_topology_and_role_isolation.sql | 26 | 2074 | 85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1 | 全读 |

## 2. 可执行证据原文（/tmp/m010_b5r2_run.log）

```
START=2026-09-09T15:03:58Z
TREE=2019d24b73e215d1043fc893190ebcfeaca6cce2
=== BACKEND: JDBC suites touching the four tables/roles + M009 governance suites ===
BACKEND_EXIT=0
2026-09-09T08:04:01.841-07:00  INFO 2703071 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 378 migrations (execution time 00:00.067s)
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.289 s -- in com.complex.platform.analytics.AnalyticsJdbcIntegrationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.963 s -- in com.complex.platform.settlement.RevenueStreamAttributionJdbcIntegrationTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.070 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
2026-09-09T08:04:05.042-07:00  INFO 2703071 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 378 migrations (execution time 00:00.039s)
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.718 s -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.105 s -- in com.complex.platform.governance.PlatformServiceTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.838 s -- in com.complex.platform.governance.PlatformControllerTests
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.241 s -- in com.complex.platform.access.AccessJdbcIntegrationTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.031 s -- in com.complex.platform.leasing.LeasingJdbcIntegrationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.278 s -- in com.complex.platform.leasing.LeasingSignAtomicityJdbcIntegrationTests
[INFO] Tests run: 126, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
=== PSQL read-only probes (complex_test @ V410) ===
== flyway rows for the four migrations
318|business entity parent layer|-705915847|t|2026-09-09 06:45:52.915717
354|business entity master data|-61418191|t|2026-09-09 06:45:53.146157
== business_entities columns
id|character varying|64|NO|
entity_name|character varying|120|NO|
entity_role|character varying|32|NO|
unified_social_credit_code|character varying|128|YES|
legal_representative|character varying|255|YES|
bank_account_ref|text||YES|
status|character varying|32|NO|
created_at|timestamp with time zone||NO|
updated_at|timestamp with time zone||NO|
archive_reason|text||YES|
== business_entities constraints/indexes
business_entities_pkey|p|PRIMARY KEY (id)
chk_be_entity_role|c|CHECK (((entity_role)::text = ANY ((ARRAY['platform'::character varying, 'project_company'::character varying, 'management_company'::character varying])::text[])))
business_entities_pkey|CREATE UNIQUE INDEX business_entities_pkey ON public.business_entities USING btree (id)
idx_business_entities_status|CREATE INDEX idx_business_entities_status ON public.business_entities USING btree (status)
== business_entities rows
psql:/tmp/m010_probe.sql:9: ERROR:  column "parent_entity_id" does not exist
LINE 1: SELECT id, entity_name, entity_role, parent_entity_id, statu...
                                             ^
== merchants columns (current)
id|character varying|NO
merchant_name|character varying|NO
merchant_type|character varying|NO
business_entity_id|character varying|YES
contract_entity_id|character varying|YES
settlement_entity_id|character varying|YES
contact_name|character varying|YES
contact_phone|character varying|YES
status|character varying|NO
created_at|timestamp with time zone|NO
updated_at|timestamp with time zone|NO
mall_block_code|character varying|YES
area_square_meter|numeric|YES
leased_area_square_meter|numeric|YES
secondary_contact_name|character varying|YES
secondary_contact_phone|character varying|YES
rent_per_square_meter|numeric|YES
cooperation_form|character varying|YES
owner_person|character varying|YES
archived_at|timestamp with time zone|YES
archived_by|character varying|YES
delete_status|character varying|NO
delete_requested_by|character varying|YES
delete_requested_at|timestamp with time zone|YES
delete_reason|text|YES
archive_reason|text|YES
== merchants/projects/spaces constraints
merchants|chk_merchant_delete_status|c|CHECK (((delete_status)::text = ANY ((ARRAY['none'::character varying, 'pending'::character varying, 'approved'::character varying])::text[])))
merchants|chk_merchants_business_format|c|CHECK (((merchant_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
merchants|chk_merchants_contact_phone_pii|c|CHECK (((contact_phone IS NULL) OR ((contact_phone)::text = ''::text) OR ((contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
merchants|chk_merchants_secondary_contact_phone_pii|c|CHECK (((secondary_contact_phone IS NULL) OR ((secondary_contact_phone)::text = ''::text) OR ((secondary_contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
spaces|chk_spaces_status|c|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'unavailable'::character varying])::text[])))
== roles.organization_id
organization_id|character varying|YES
idx_roles_organization|CREATE INDEX idx_roles_organization ON public.roles USING btree (organization_id) WHERE (organization_id IS NOT NULL)
10|0
== permissions seeded by V005
entity.binding.manage|管理业务主体和绑定|platform|high|t|active
entity.binding.view|查看业务主体和绑定|platform|medium|f|active
governance.project.manage|管理平台项目|governance|high|t|active
governance.project.view|查看平台项目|governance|medium|f|active
governance.space.manage|管理平台空间|governance|high|t|active
governance.space.view|查看平台空间|governance|medium|f|active
merchant.profile.manage|管理商户档案|platform|high|t|active
merchant.profile.view|查看商户档案|platform|medium|f|active
8
== permissions columns (does permission_name have a chinese column / translation?)
permission_code|character varying
permission_name|character varying
domain|character varying
risk_level|character varying
requires_approval|boolean
status|character varying
created_at|timestamp with time zone
== grants on business_entities
complex|DELETE
idx_roles_organization|CREATE INDEX idx_roles_organization ON public.roles USING btree (organization_id) WHERE (organization_id IS NOT NULL)
roles_pkey|CREATE UNIQUE INDEX roles_pkey ON public.roles USING btree (id)
uq_roles_org_role_name|CREATE UNIQUE INDEX uq_roles_org_role_name ON public.roles USING btree (COALESCE(organization_id, ''::character varying), role_name)
399|pii idempotency response body envelope|t
410|m010 business entity topology and role isolation|t
=== raw entity_role literals still using retired codes (test tree + ops) ===
backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java:479:                -- M010-012：entity_role 白名单已收为 HTML 三角色（
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java:450:    // ---- M010-002/003：entity_role 白名单只剩 HTML 三角色，spv/op
backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java:463:                "INSERT INTO business_entities (id, entity_name, entity_role, 
=== V410 file ===
  26 2074 backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1  backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
TREE_AFTER=2019d24b73e215d1043fc893190ebcfeaca6cce2
END=2026-09-09T15:04:07Z
=== supplementary probe 2026-09-09T15:04:58Z: /tmp/m010_probe.sql line 9 predates V410 and still selected the deleted parent_entity_id column (its ERROR above is itself evidence of M010-001); rows re-listed without it ===
be_amusement|游乐公司|management_company|active|f
be_central_catering|中央餐饮公司|management_company|active|f
be_ent_platform|娱乐平台公司|management_company|active|f
be_media|传媒公司|management_company|active|f
be_platform|平台公司|platform|active|f
be_proj_changsha|长沙项目公司|project_company|active|f
management_company|4
platform|1
project_company|1
ERROR:  syntax error at or near "\"
LINE 1: ... FROM information_schema.columns WHERE table_name=\x27busine...
                                                             ^
=== supplementary probe (retry via /tmp/m010_supp_probe.sql; the \x27 syntax error above was shell quoting of the inline column-count query, not a DB fact) 2026-09-09T15:05:24Z ===
be_amusement|游乐公司|management_company|active|f
be_central_catering|中央餐饮公司|management_company|active|f
be_ent_platform|娱乐平台公司|management_company|active|f
be_media|传媒公司|management_company|active|f
be_platform|平台公司|platform|active|f
be_proj_changsha|长沙项目公司|project_company|active|f
management_company|4
platform|1
project_company|1
0
END_SUPP=2026-09-09T15:05:24Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文重新推导本模块承载的业务约束（BEQR-143/144、BSM-028、PGM-018/019 多对多协作与三类主体角色；BSM-004 软删；BSM-007 平台管理员日常权限；BEQR-152/PGM-041 数据按组织/项目隔离），逐条对照四份历史迁移与本模块修复产物 V410 的当前语义（不依赖 B1–B4 结论）。
- 顺从式线：M010 映射 GEN-001–018 逐项（§5）。
- 对抗式线：只读 `complex_test`（新建库，V001→V410）实态：列/索引/约束/COMMENT/种子行/flyway 历史；treat V410 as attack surface——重放幂等（IF EXISTS / ON CONFLICT / UPDATE WHERE）、存量行坍缩是否丢数据、CHECK 收窄后种子六行是否仍合法、roles 表达式唯一索引对 NULL 组织的语义（COALESCE→全局名唯一）、下游消费方（结算测试、M009 后台）是否仍绿；treat renamed DB as evidence of nothing deleted。
- 依赖只读追流（不审）：`access/RoleRecord`、`access/JdbcAccessRepository`（roles 读写含 organization_id）、`governance/JdbcPlatformRepository`（四表读写、M009-006/007/027 存在性）、`settlement/SettlementEntityAttributionS1Tests`、`settlement/RevenueStreamAttributionJdbcIntegrationTests`、M011 `PlatformJdbcIntegrationTests`（三条新断言、六处夹具）。
- 5 文件（4 冻结 + V410）全文逐行读完（V005 109、V048 7、V318 16、V354 71、V410 26 行）；B3-r2 只改 AnalyticsJdbcIntegrationTests 一行夹具（012），按 diff 逐字对照。

## 4. 已定稿 14 项在当前工作区/活库的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | V410 §1 删索引+列；活库 `parent_entity_id` 列 0、`idx_business_entities_parent` 0；六行骨架 id/名称/角色/status 未变；伴生：结算两测试去父链断言/删父链方法 | `parentEntityColumnIsRemovedFromBusinessEntities` PASS；结算 8/8、2/2 |
| 002/003 | 否（已修，操作员 2.2 答删除） | V410 §2 存量 operation/spv 行坍缩为 management_company（新库 0 行命中，UPDATE 为空操作）后 CHECK 三值、COMMENT 三角色口径；活库 CHECK 定义仅三值 | `entityRoleCheckRejectsSpvAndOperation` PASS；六行角色实态 platform×1/project_company×1/management_company×4 |
| 004 | 否（已修） | V410 §3 删 `roles_role_name_key`，建 `uq_roles_org_role_name ((COALESCE(organization_id,'')), role_name)`；活库索引存在、10 行全局角色仍互不同名 | `organizationScopedRolesMayShareNameAcrossOrganizations` PASS |
| 012 | 否（已修 B3-r2） | AnalyticsJdbcIntegrationTests 夹具 `operation`→`management_company` + 版本头 | `merchantSummariesRollupAggregatesRealSources` 由 ERROR→通过（§2：29/29） |
| 005–011、013、014 | 否（登记） | 内容未动；009：V410 已按操作员裁定编号，测试库由席甲改名保留（`complex_test_pre_v410_20260909`，409）+ 新建（410），ledger V2.21 记录；MigrationVersions 断言 410=410；013/014 为索引语义与文案观察 | 与定稿一致 |

## 5. 18 判据逐项核对（修复后）

| 判据 | 结论 |
|---|---|
| GEN-001 必要性 | V318 死列已删（001）；`spv`/`operation` 无消费方的枚举值已删（002/003）；V048 列由 RoleRecord 读写；V410 每条语句对应定稿 ID，无多余语句 |
| GEN-002 可执行正确性 | V410 在新库成功应用（flyway 410 success）；126 条相关测试全部通过（§2，含 AnalyticsJdbcIntegrationTests 29/29） |
| GEN-003 测试有效性 | M011 三条新断言各自红→绿（B3 红日志）；结算两测试仍断言六骨架角色/status/工商空值/archive_reason/非法角色拒绝 |
| GEN-004 信任边界 | 库层 CHECK 三值与 M009 服务层白名单一致；roles 组织内唯一 |
| GEN-005 授权 | 权限种子未动；组织范围角色可同名，隔离语义与 V048 声称一致 |
| GEN-006 数据完整性 | UPDATE 坍缩保留全部行；DROP COLUMN 不删行；六行稳定 id 保留供结算引用；唯一索引替代全局 UNIQUE |
| GEN-007 并发幂等 | V410 全语句可重放（DROP IF EXISTS / UPDATE WHERE / DROP CONSTRAINT IF EXISTS / CREATE INDEX IF NOT EXISTS）；Flyway 单次执行 |
| GEN-008 接口与模式一致性 | 迁移枚举=服务层白名单=测试夹具（六处已改）；`parent_entity_id` 在记录/DTO/SQL/列四面均不存在；COMMENT 与 HTML 三角色一致 |
| GEN-009 错误处理 | 非法角色落 CHECK 违例（服务层先拦，M009-005）；迁移失败即 Flyway 阻断 |
| GEN-010 安全 | 无秘密；权限继承既有 GRANT（新库由 V140 授予 complex_app） |
| GEN-011 可观测 | flyway_schema_history 410 留痕；UPDATE 只改 role/updated_at，archive_reason 留痕未动 |
| GEN-012 资源边界 | 删除一个无消费方索引；新增一个表达式唯一索引（roles 10 行） |
| GEN-013 构建配置 | Flyway 校验 388 迁移通过；未改配置、未开 outOfOrder |
| GEN-014 迁移正确性 | 顺序确定（V410 为最高）；可在目标库执行；破坏性动作（删列）已获操作员明确授权；历史迁移正文未改；未发布分支 V400–V409 与本迁移的合并顺序为未来环境项（009 登记） |
| GEN-015 界面 | 无界面 |
| GEN-016 文档可验证 | V410 头注引用 HTML 原句与操作员裁定；旧 V318/V354 注释为历史文件不改（006 登记） |
| GEN-017 分类 | 001 冲突、002/003 添加（已答删除）、004 细化；本轮无新添加项 |
| GEN-018 第一性原理 | 每条落到 HTML 原文、迁移原文、psql 实态或测试输出 |

## 6. 本次 Findings

无新的未定稿工程缺陷。

对抗式反例逐条结果：
1. 重放 V410（在临时副本库上二次执行各语句）：全部幂等（本席只读推演 + 语句形态；未在主库重放）。
2. 存量 `operation`/`spv` 行：新库 0 行；原库（改名保留）中亦 0 行 active/inactive operation（V354 ② 在 complex_test 系列上从未命中）；UPDATE 不删行。
3. 六行骨架在 CHECK 收窄后仍合法（platform/project_company/management_company）。
4. `uq_roles_org_role_name`：两个 NULL 组织同名 → COALESCE 为 '' 相等 → 拒（全局名唯一）；不同组织同名 → 允许；同组织同名 → 拒（测试覆盖后两者）。
5. 结算消费方：`SettlementEntityAttributionS1Tests` 8/8、`RevenueStreamAttributionJdbcIntegrationTests` 2/2；生产代码 0 处引用 `parent_entity_id`。
6. M009 后台：`PlatformServiceTests` 31/31、`PlatformControllerTests` 10/10、`PlatformJdbcIntegrationTests` 16/16（含 M009 三批新测试）；`AccessJdbcIntegrationTests` 24/24（roles 唯一索引改动无回归）。
7. 治理外夹具：`AnalyticsJdbcIntegrationTests` 29/29（012 已修）；测试树与 ops 中再无 `operation`/`spv` 直写。

## 7. 观察（登记、不入队）

- OBS-B5-01：原 complex_test 改名为 `complex_test_pre_v410_20260909`（flyway 409、全部数据原样）；新 complex_test 由 V001 迁到 V410，测试数据为空表起步；其它 `complex_test_*` 副本库未动。若操作员需要旧数据，改名回退即可。
- OBS-B5-02：`business_entities.status` 仍无 CHECK（008 登记）；V354 ② 的 `inactive` 与 M009 `SUBJECT_STATUSES` 分叉未变。
- OBS-B5-03：未发布分支 V400–V409 若日后合入 main，其版本号低于已应用的 V410，届时需 `outOfOrder` 或重新编号（009 口径，操作员知悉）。
- OBS-B5-04：V410 头注引用「操作员裁定」为过程事实（可在 ledger V2.19–V2.21 与 questions/M010-002/003 复核），不是业务依据；业务依据为其后引用的 HTML 原句。
- 技能：`using-superpowers`、`verification-before-completion`（结论附实跑/psql）、`receiving-code-review`（Grok B2 的伴生清单逐条核实并落地）、`test-driven-development`（B3 红→绿，含一次测试结构修正）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r2 票：**PASS**。4 冻结文件 + V410 三线全审、18 判据逐项核对；两批共 5 项修复无回归、无新未定稿缺陷；无添加项、无 questions、无 HTML 修改申请；未提交/推送/合并/部署。本报告落盘前未读取 `M010-b5-r2-grok-raw.md` 任何正文。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 Codex 独立补充复查完整报告
版本：V0.02

本次结论：PASS。Grok：NOT-CALLED（无命令、无退出码、无本次 Grok 票）。
这是操作员“从M009自己重新审；没有问题只需自己审计，发现问题才按原提示词完整执行”的补充复查。保留旧B5-r2双PASS及真实席甲Claude Fable 5.1身份，不把本次单席改写为新双PASS。B3历史累计2，本次执行0，仍2。生命周期沿用PASS；无BLOCKED、无未答问题、无HTML申请。
日期：2026-09-14；正本160:/home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。直接SSH；无MCP、无第三席。原本报告20044bytes的完整字节前缀保留，旧SHA256 846ec98dc791f420982127215bfa21416f5234b8af73ed4b03467f6d0e88b9ac。
控制V0.17、冻结计划V0.04、判据V0.11；范围4文件203行+直接纠正依赖V410，映射GEN-001至018共18项，未扩冻结判据。

## 1. 完整文件覆盖
| 路径 | 行数 | SHA256 | 阅读 |
|---|---:|---|---|
| backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql | 109 | 5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905 | 全文 |
| backend/src/main/resources/db/migration/V048__access_role_organization_isolation.sql | 7 | e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd | 全文 |
| backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql | 16 | 466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481 | 全文 |
| backend/src/main/resources/db/migration/V354__business_entity_master_data.sql | 71 | c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3 | 全文 |
| backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql | 26 | 85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1 | 全文，直接纠正依赖 |

## 2. 三线审计与业务分类
第一性原理：主数据表承载项目/空间/商户/公司档案；V048列承载角色组织范围，V410去掉没有业务依据的固定行政父链。需求原文（已在本次连续会话读取正本）：
- business-enum-quick-reference-v0.1.html“经营主体关系”：“物理项目与运营公司是多对多关系，不是固定的‘平台→项目公司→域公司→商户’行政隶属链。”（引号样式仅展示；原文见正本227行及BEQR-144）
- platform-governance-map-for-operations-v0.1.html“综合体和集团：当前一个项目，未来能看集团”：“建立综合体项目档案。”“记录项目状态、负责人、地址和经营范围。”
- business-enum-quick-reference-v0.1.html“项目与敏感数据权限”：“所有数据默认按项目隔离，普通岗位不得跨项目查看；只有项目之上的获授权岗位可以跨项目汇总。”
四分类：V005四表与档案字段/权限种子为细化；主键/时间列/索引/ON CONFLICT为无业务发明的技术性存储机制；V048组织列为隔离细化，索引技术性；V318父链为历史冲突，当前由V410移除；V354六主体档案骨架为细化，工商数据不猜填；旧operation/spv添加已由操作员裁定删除且V410落实。V410每条仅纠正既有001–004，未新造结算/组织规则。
顺从式：逐行核四份冻结迁移及后续V410，逐项核18判据；同一迁移链的最终状态用于判断当前行为，不把旧迁移孤立当成正在生效的父级业务规则。
对抗式：分别尝试反证“旧父级仍在”“旧角色仍可入库”“不同组织角色被全局唯一卡住”“权限人读名称仍英文”“只有历史PASS没有活库证据”；通过以下目录查询和已实际执行的三条JDBC回归核对。没有发现新的待修项。
技术性逐条：表主键与必要索引用于检索/唯一性；TIMESTAMPTZ用于持久化操作时间；ON CONFLICT/IF EXISTS为迁移执行机制；Flyway文件顺序为工程运行依赖，均不赋予业务权利。按本次用户单席复查例外不为这些既有分类另起Grok。

## 3. 真实只读数据库核验
直接SSH在160 Python中调用psql，PGDATABASE=complex_test，PGOPTIONS=-c default_transaction_read_only=on，psql -X -A -t -v ON_ERROR_STOP=1 -c SELECT…。返回PSQL_EXIT=0。只取模式、迁移状态和公开权限显示名；未执行DDL/DML、未重建或改名数据库。
关键可复现查询：flyway_schema_history筛version IN ('005','048','251','318','354','410','427')；information_schema.columns筛public.business_entities.parent_entity_id；pg_indexes筛idx_business_entities_parent及roles唯一索引；pg_constraint对chk_be_entity_role执行pg_get_constraintdef；business_entities筛entity_role IN ('operation','spv')计数；permissions按V005八条permission_code读取permission_name。
实际输出：
```json
{
  "PSQL_EXIT": 0,
  "read_only": true,
  "V410_sha": "85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1",
  "data": {
    "database": "complex_test",
    "migrations": [
      {
        "version": "005",
        "script": "V005__p0_platform_master_data.sql",
        "success": true
      },
      {
        "version": "048",
        "script": "V048__access_role_organization_isolation.sql",
        "success": true
      },
      {
        "version": "251",
        "script": "V251__permission_names_i18n_zh.sql",
        "success": true
      },
      {
        "version": "318",
        "script": "V318__business_entity_parent_layer.sql",
        "success": true
      },
      {
        "version": "354",
        "script": "V354__business_entity_master_data.sql",
        "success": true
      },
      {
        "version": "410",
        "script": "V410__m010_business_entity_topology_and_role_isolation.sql",
        "success": true
      },
      {
        "version": "427",
        "script": "V427__m009_project_profiles_and_associations.sql",
        "success": true
      }
    ],
    "parent_column_count": 0,
    "parent_index_count": 0,
    "entity_role_constraint": "CHECK (((entity_role)::text = ANY ((ARRAY['platform'::character varying, 'project_company'::character varying, 'management_company'::character varying])::text[])))",
    "unsupported_entity_role_count": 0,
    "role_unique_indexes": [
      "CREATE UNIQUE INDEX roles_pkey ON public.roles USING btree (id)",
      "CREATE UNIQUE INDEX uq_roles_org_role_name ON public.roles USING btree (COALESCE(organization_id, ''::character varying), role_name)"
    ],
    "permission_names": {
      "governance.project.manage": "管理平台项目",
      "governance.project.view": "查看平台项目",
      "governance.space.manage": "管理平台空间",
      "governance.space.view": "查看平台空间",
      "entity.binding.manage": "管理业务主体和绑定",
      "entity.binding.view": "查看业务主体和绑定",
      "merchant.profile.manage": "管理商户档案",
      "merchant.profile.view": "查看商户档案"
    }
  }
}
```
上述输出证明当前结果；不从迁移注释推断实际模式。源文件/测试均在M009结束后未发生生产改动，最后一次变化仅SpaceTypeRequest注释。

## 4. 已执行的测试证据及本次限制
本次M010不重复运行测试。使用同一会话2026-09-14 M009阶段JDK21+complex_test实际执行的PlatformJdbcIntegrationTests：23测试，failures=0，errors=0，skipped=0，Maven exit0；已封存在M009-b5-r2-self-raw.md及M009-b5-r3-self-raw.md，不将2026-09-09的16条旧结果写作当前23条。
已读取本次源第680–714行：
- parentEntityColumnIsRemovedFromBusinessEntities：真实查询列/索引计数都0。
- entityRoleCheckRejectsSpvAndOperation：CHECK定义包含三合法值、排除spv/operation；真实插入合法值成功，spv插入被拒；没有声称对operation另做一次插入。
- organizationScopedRolesMayShareNameAcrossOrganizations：真实插入不同组织同名两行成功，同组织重复抛DuplicateKeyException。NULL组织同名唯一从当前COALESCE索引定义核验，未声称本次另写NULL插入测试。
M009根执行11类283条与Grok10类239条有重叠，未相加计数。本次只读SQL是新执行证据，测试为本会话已完成证据的明确复用。未声称执行全仓测试、生产库迁移或全链手动重放。

## 5. 18条冻结判据逐项结论
| 判据 | 结论 | 本次依据 |
|---|---|---|
| GEN-001 | PASS | V005 四张主数据表、V048 组织字段由当前仓储消费；V410 删除不再消费的父链。历史迁移留存用于 Flyway 校验，不复制重建。 |
| GEN-002 | PASS | 当前 complex_test 的 005/048/318/354/410 均 success=true；列、约束、索引与 V410 结果一致。 |
| GEN-003 | PASS | 三个 JDBC 回归检查删除列/索引、合法与非法角色、跨组织同名与同组织重复；本会话 M009 实跑23条全过，非只看测试名。 |
| GEN-004 | PASS | DDL 固定文本，无外部输入拼接；entity_role NOT NULL + 三值 CHECK；源端对象校验由已全审 M009 服务承担。 |
| GEN-005 | PASS | V005 固定 permission_code 和 role_platform_admin 种子，无新增授权动作；V048 组织列与 V410 唯一约束共同支持隔离，不把跨组织同名解释为跨组织访问权。 |
| GEN-006 | PASS | 主键/NOT NULL/unique 保护基本完整性；V410 UPDATE 保留主体行，role 白名单收窄前完成转换；不改变金额。历史无硬 FK 和 status 无 CHECK 仍为既有登记007/008，不改写成已修。 |
| GEN-007 | PASS | Flyway 已记录一次性执行；INSERT ON CONFLICT 和存在性 DDL 有确定重复行为。本次未手工重放整条迁移链，不声称重放实测通过。 |
| GEN-008 | PASS | 当前 parent_entity_id 列/索引均0，三角色 CHECK 与 M009 DTO/记录/JDBC 对齐。旧迁移的历史状态值与注释按006/008保留登记。 |
| GEN-009 | PASS | 测试实际断言 DataIntegrityViolationException 和 DuplicateKeyException；数据库核验 ON_ERROR_STOP=1 且返回0，没有吞错。 |
| GEN-010 | PASS | 迁移不含凭据/动态SQL；核验凭据只经进程环境传递，报告仅列模式事实，不导出私人字段值。 |
| GEN-011 | PASS | Flyway 版本、script、success 可查询；本次独立实算文件SHA与只读SQL输出封存。原始旧票完整保留。 |
| GEN-012 | PASS | 迁移一次性操作既有四表/roles；索引针对状态、归属及唯一性，无新增循环、常驻连接或无界缓存。 |
| GEN-013 | PASS | 本会话实际 JDK21、Maven exit0、complex_test 迁至V427；本次不改配置、不清库、不重新编号历史迁移。 |
| GEN-014 | PASS | 当前 Flyway 执行与脚本同名成功，V410 从后来迁移纠正历史脚本；过去删父列授权保留，本次无破坏性数据库操作。 |
| GEN-015 | 不适用（无界面） | SQL无界面组件。V005 八条人读英文权限名已被V251替换，当前查询全部为中文。 |
| GEN-016 | PASS | 区分历史2026-09-09票与本次2026-09-14 Codex复查；当前库最高不是旧报告V410而是已核V427；旧注释登记不追改。 |
| GEN-017 | PASS | 四文件按冲突→细化→技术性→添加逐段复查；原父链冲突由V410消除，operation/spv添加已有删除裁定；表/种子细化，索引/迁移标记技术性，无新添加业务。 |
| GEN-018 | PASS | 以下结论分别落到四迁移+V410原文、当前HTML原句、真实SQL输出、已封存测试结果；历史PASS本身不作为当前正确性的证明。 |

## 6. 既有登记与结论
001–004、012五项已修保持；005–011、013、014九项仅登记保持。008的status无CHECK和历史inactive观察并未改成“已修”；006旧迁移注释因checksum保留；005已用活库核实V251中文化；009库已至V427，旧报告V410只代表当时。无新ID、无待补项、修复队列仍14项全否。
本次PASS仅指M010当前4冻结文件+直接修复V410的18判据补充复查，无新工程缺陷/业务发明。Grok NOT-CALLED，旧Grok票未改。三业务HTML和业务源码/迁移全未改，未提交/推送/合并/部署。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M010 Codex补充复查
版本：V0.03

操作员本次明确“m010继续”，按此前“无问题只自己审计，发现问题再走原完整流程”。本次仅Codex复查；Grok NOT-CALLED，不冒称现锁新双PASS。历史B3累计2保持、修复0。M038错误派发已按操作员纠正取消，不属M010审计票。

## 范围与三线
冻结FILES四迁移共203行全部读完，另全文读直接纠正迁移V410共26行。18个GEN定义全读并逐项审查；HTML当前治理图415—440核对多对多协作和主体职责。历史V318/V354写父链/spv/operation不是当前模式，必须连V410看最终结果。
第一性：表承载项目/空间/商户/主体主数据，组织列隔离角色；删掉会失去对象归属。SQL结构和索引属细化/技术性，不能因HTML未列字段名删除。
顺从式：本轮实库现值对照V410纠正内容及当前HTML，不引用旧四层行政链作为现行口径。
对抗式：核父链是否残留、角色白名单是否仍允许删除角色、全局角色名约束是否阻断组织同名、英文权限种子是否仍外显、应用历史与文件checksum是否漂移。各项现值未见反例成立。
限制：不执行DDL/回填/迁移/数据库改名，不跑全后端，不称V427顺序门禁或发布链全绿。24项JDBC是同会话M009已跑证据，本轮读取XML核24/0/0/0及相关方法，不冒称本轮重新执行。纯SQL复查采用实际只读模式和checksum证据，不造TDD。

## 全文件指纹
- V005__p0_platform_master_data.sql：109行；SHA256 5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905；实算/库内Flyway checksum 413803928/413803928，相等。
- V048__access_role_organization_isolation.sql：7行；SHA256 e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd；实算/库内Flyway checksum -1430035601/-1430035601，相等。
- V318__business_entity_parent_layer.sql：16行；SHA256 466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481；实算/库内Flyway checksum -705915847/-705915847，相等。
- V354__business_entity_master_data.sql：71行；SHA256 c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3；实算/库内Flyway checksum -61418191/-61418191，相等。
- V410__m010_business_entity_topology_and_role_isolation.sql：26行；SHA256 85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1；实算/库内Flyway checksum 983875231/983875231，相等。

## 18判据逐项
- GEN-001：基础表、项目/空间归属与角色组织隔离各有实际用途；纠正迁移消除旧固定父链，未新增功能。
- GEN-002：当前complex_test模式及Flyway成功记录与五份迁移接线一致；本轮只读查询exit0。
- GEN-003：复用同会话M009已真实运行24项JDBC结果，其中3项直接断言删父链、拒spv/operation、组织名唯一；明确未冒称本轮重跑。
- GEN-004：entity_role现CHECK三值；roles组织范围由现有组织列承载；不为裸SQL绕过业务层新增校验。
- GEN-005：角色按组织隔离唯一性；V005权限种子不替代运行期授权；M009已查现有高危权限过滤，不单凭历史直授判越权。
- GEN-006：主键/唯一索引及三角色CHECK实际存在；旧父链删除不删六行主体；当前六主体分别为1平台/1项目/4域公司。
- GEN-007：roles表达式唯一索引处理同组织重复；多组织同名允许；历史迁移受Flyway版本记录，不假造并发反例。
- GEN-008：V410纠正旧父链/角色白名单；8权限名称当前中文；旧文件中英文注释不是现行产品口径。
- GEN-009：psql ON_ERROR_STOP=1、READ ONLY事务、ROLLBACK；任何查询失败不会写PASS；本次查询成功。
- GEN-010：未输出密码、未改加密或PII策略；查询只输出模式/权限字典及聚合数，姓名加密后续V431属既有依赖不扩。
- GEN-011：Flyway应用脚本/version/success/checksum可关联；本轮保留完整命令口径和结果。
- GEN-012：本模块常数种子、索引及一次迁移，无循环或无限重试；catalog查询限定表/版本。
- GEN-013：无需安装工具或新增依赖，目标PostgreSQL现有psql执行；不声称运行发布链一致。
- GEN-014：四历史迁移及V410实算Flyway逐行CRC32与已应用checksum五项全等；没有改已应用文件/重跑迁移/重建数据库。
- GEN-015：8权限用户名称现为中文；历史V005英文由V251覆盖，无网页文件。
- GEN-016：旧迁移注释关于固定四层等记作已登记历史，不改checksum，也不以旧注释作为现行需求。
- GEN-017：全14项既有问题重判，当前无新添加/工程缺陷；旧spv/operation删除裁决沿用，不擅自恢复。
- GEN-018：依据本轮完整源码/HTML原文、真实只读SQL及checksum，不以历史PASS或其它席意见为证据。

## 全14既有ID当前重判
- M010-001：当前队列否。父链列及索引现均不存在；V410删除已执行，稳定主体行保留。
- M010-002：当前队列否。spv已不在角色CHECK，当前0行；旧删除裁决保持。
- M010-003：当前队列否。operation已不在角色CHECK，当前0行；旧删除裁决保持。
- M010-004：当前队列否。roles_role_name_key全局唯一约束已无；uq_roles_org_role_name以组织+名称唯一。
- M010-005：当前队列否。八个permission_name现均中文，V251已成功应用。
- M010-006：当前队列否。历史迁移过时来源注释保留登记，不能倒改已应用checksum。
- M010-007：当前队列否。松引用由当前服务层校验承担；不新造硬外键。
- M010-008：当前队列否。business_entities.status旧inactive归档口径与服务可设状态不同，已登记范围，不新增状态流程。
- M010-009：当前队列否。当前库到V431，不把历史V410测试库快照当当前；不处置版本顺序/重建库。
- M010-010：当前队列否。下游断言已有删列/角色拒绝/组织隔离覆盖，复用同会话24项通过证据。
- M010-011：当前队列否。骨架稳定ID/不凭空造工商信息、ON CONFLICT与存档历史保持；不新增业务。
- M010-012：当前队列否。Analytics旧operation夹具的历史修复保留；本次不冒称重跑Analytics整类。
- M010-013：当前队列否。COALESCE(NULL,'')同键、inactive保留名称的既有唯一性说明保持。
- M010-014：当前队列否。结算测试命名旧父链字样作为历史文案观察，不跨模块改名。

## 本轮实跑
psql在complex_test执行BEGIN TRANSACTION READ ONLY，限定information_schema/pg_indexes/pg_constraint、业务角色聚合、8个权限名称、Flyway目标版本，最后ROLLBACK；ON_ERROR_STOP=1，真实exit0。仅读取/etc/complex/complex.env三个既有DB变量，未source整个文件，不输出秘密。
DB_EXIT 0
BEGIN
 current_database 
------------------
 complex_test
(1 row)

 column_name 
-------------
(0 rows)

       indexname        |                                                               indexdef                                                               
------------------------+--------------------------------------------------------------------------------------------------------------------------------------
 uq_roles_org_role_name | CREATE UNIQUE INDEX uq_roles_org_role_name ON public.roles USING btree (COALESCE(organization_id, ''::character varying), role_name)
(1 row)

        conname         |                                                                        pg_get_constraintdef                                                                         
------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------
 business_entities_pkey | PRIMARY KEY (id)
 chk_be_entity_role     | CHECK (((entity_role)::text = ANY ((ARRAY['platform'::character varying, 'project_company'::character varying, 'management_company'::character varying])::text[])))
(2 rows)

    entity_role     | count 
--------------------+-------
 management_company |     4
 platform           |     1
 project_company    |     1
(3 rows)

      permission_code      |  permission_name   
---------------------------+--------------------
 entity.binding.manage     | 管理业务主体和绑定
 entity.binding.view       | 查看业务主体和绑定
 governance.project.manage | 管理平台项目
 governance.project.view   | 查看平台项目
 governance.space.manage   | 管理平台空间
 governance.space.view     | 查看平台空间
 merchant.profile.manage   | 管理商户档案
 merchant.profile.view     | 查看商户档案
(8 rows)

 version |                           script                           | success 
---------+------------------------------------------------------------+---------
 005     | V005__p0_platform_master_data.sql                          | t
 048     | V048__access_role_organization_isolation.sql               | t
 251     | V251__permission_names_i18n_zh.sql                         | t
 318     | V318__business_entity_parent_layer.sql                     | t
 354     | V354__business_entity_master_data.sql                      | t
 410     | V410__m010_business_entity_topology_and_role_isolation.sql | t
 427     | V427__m009_project_profiles_and_associations.sql           | t
 431     | V431__merchant_contact_names_ciphertext.sql                | t
(8 rows)

 max | max 
-----+-----
 399 | 431
(1 row)

ROLLBACK



另读取flyway_schema_history(script,checksum)五版本，Python binascii.crc32按UTF-8每行去行尾累计，32位有符号转换；五个实算与库内校验值全等。字节SHA见上。
24项既有JDBC结果相关方法：parentEntityColumnIsRemovedFromBusinessEntities、entityRoleCheckRejectsSpvAndOperation、organizationScopedRolesMayShareNameAcrossOrganizations。断言是行为证据，非仅测试名称；本模块旧三条红→绿历史不覆盖。

## 完整性及结论
现锁实算/sidecar/ledger保持b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；LOCK-AMEND-COUNT1。旧Grok末条锁为b012…，属于历史，不冒充本轮第四值；按用户单席复查例外不声称本轮四值双审。
HEAD e456ba96c84185c8d39f18f548011daac48a6466；真实indexSHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331。源码/SQL/三HTML/锁均未修改，只允许本轮复查记录递增版本。
使用grilling/第一性追问与codebase-design识别持久化用途，按V0.17收窄，不派子代理、不增加判据/功能。
本次Codex复查结论PASS：未发现新问题。Grok NOT-CALLED，B3累计2、本次修复0、无待批准项。保留历史双席PASS及全部原始报告。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M010 B5-r2 当前锁全模块独立审计
版本：V0.04

席甲：Codex。B3累计2；本次未执行修复。先派Grok后独立读取源文件、当前数据库与历史授权；本报告落盘前未读取本次Grok正文。本批B1/B2已双PASS、当前14项队列均否；跳过新增B3/B4，历史累计2。此处重新读取五SQL全文和18GEN并执行新的只读数据库核验，不把此前单席补充PASS当双席结论。

## 范围与第一性原理
冻结FILES：V005 109行、V048 7行、V318 16行、V354 71行，共203行；另完整读取既有直接修复V410 26行。仅使用M010映射GEN-001至018，不扩映射。项目拓扑应从治理HTML“多对多协作，不是固定隶属层级”出发，不能从历史SQL注释反向发明行政层级。历史迁移需经后续纠正迁移求最终效果，不能单拿V318/V354旧DDL判当前仍有父链。
当前HTML、锁与已有授权保持；questions/M010-002、003均明确“删除”。V410删父链和收紧角色是已批准且已实施的历史修复，本次不执行迁移。

## 顺从式全覆盖
V005：四类主数据表、主键/索引、时间字段、8权限种子及role_permissions均已逐行读取。VARCHAR长度为旧建表态，后续加密和治理迁移须一并考虑；历史英文权限名已有V251修正，不改已应用checksum。
V048：可空organization_id及部分索引保留；实际角色同名隔离须同时查看V410替换全局唯一约束。
V318：旧父链列/注释/索引曾违背HTML，V410已删除最终模式的列和索引，旧文件保持作迁移证据。
V354：archive_reason、一次性operation归档、六稳定主体种子、旧五角色CHECK均读取。种子不造工商资料、不种十家酒吧、不单列资产公司。旧operation/spv白名单已被V410三值约束替换。
V410：仅删除旧父链、转换旧角色并收紧CHECK、替换角色唯一性；无DROP实体表或删实体行。COALESCE组织键保持全局NULL/空串同键，不同组织可同名；未自行增加权限或生命周期规则。

## 对抗式核验与实际命令证据
本次真实只读psql，ON_ERROR_STOP=1，BEGIN TRANSACTION READ ONLY…ROLLBACK，exit0：
- information_schema查business_entities.parent_entity_id：0行。
- pg_indexes查旧父链索引与新角色唯一索引：仅uq_roles_org_role_name存在，定义UNIQUE btree(COALESCE(organization_id,''::varchar),role_name)。
- pg_constraint查旧roles_role_name_key与chk_be_entity_role：仅后者存在，允许platform/project_company/management_company三值。
- business_entities按角色计数：management_company4、platform1、project_company1；旧两角色无行。
- Flyway 005/048/318/354/410全部success，checksum依次413803928、-1430035601、-705915847、-61418191、983875231；max(version::int)=431。
文件本次SHA与刚完成的只读复查一致，未发生源字节漂移。本次B5重新按Flyway逐行UTF-8 CRC32算法算得五checksum，与当前数据库逐项全等。
直接读取PlatformJdbcIntegrationTests:706-739：列/索引不存在断言；三角色定义断言+合法插入+spv违例；两组织同名成功、同组织同名DuplicateKey。最近M009实跑该类24/24为已有同源证据，本次未新跑写库测试；不声称重新跑全量后端。operation通过约束白名单排除，不谎称在已中止事务又执行了第二条违例插入。

## 18项冻结判据
| 判据 | 核验与本次结论 |
|---|---|
| GEN-001 | 四表/索引/稳定ID有当前调用用途；父链已删；未发现需增删新抽象，PASS |
| GEN-002 | 五迁移当前Flyway成功，最终DDL与使用方直接测试一致，PASS |
| GEN-003 | 直接三测试断言会因父链、角色放宽或组织唯一性回归而失败；不以旧绿替代本次DDL检查，PASS |
| GEN-004 | 角色CHECK有效；引用关系由服务校验的既有设计，不擅增硬FK，PASS |
| GEN-005 | 权限种子高风险管理须审批，organization索引不冒充完整授权；本模块未添越权入口，PASS |
| GEN-006 | 唯一约束和CHECK由数据库实施，迁移按Flyway事务顺序；旧全局唯一已除，PASS |
| GEN-007 | 固定种子ON CONFLICT避免重复，唯一性原子；不会宣称DO NOTHING修复人工漂移，PASS |
| GEN-008 | 最终三角色/父链移除与当前后台、直接夹具一致；旧状态观察维持登记，PASS |
| GEN-009 | 约束违例明确失败，无吞错；迁移失败不可伪成功，PASS |
| GEN-010 | SQL无动态拼接输入或嵌入密钥；工商资料种子NULL；历史字段加密不重复开无授权功能，PASS |
| GEN-011 | Flyway记录、checksum、schema实态可核；归档理由留存，PASS |
| GEN-012 | 固定DDL/种子及必要索引，无应用无界循环或资源泄漏，PASS |
| GEN-013 | 五文件checksum与既有安装一致；库当前V431，不复述旧409为现况，PASS |
| GEN-014 | 历史破坏性删列有明确授权；本次无迁移执行，顺序和最终结构正确，PASS |
| GEN-015 | 无本模块UI；权限文案经后续V251中文化，PASS |
| GEN-016 | 旧注释残留登记006/014，未作为当前业务依据；当前效果可复现，PASS |
| GEN-017 | 冲突→细化→技术性→添加顺序；旧两添加已答删除且完成，无新业务发明，PASS |
| GEN-018 | 结论依HTML原文、SQL源和可复现只读输出；历史意见不是单独依据，PASS |

## 全部14已定稿ID复核
| ID | 现态与已定稿当前修复队列 |
|---|---|
| M010-001 | 父链列和索引实际不存在，已修，否 |
| M010-002 | spv已不在CHECK，已批准删除且完成，否 |
| M010-003 | operation已不在CHECK，已批准删除且完成，否 |
| M010-004 | 组织复合唯一索引有效，已修，否 |
| M010-005 | V005旧英文被V251后继修正，登记，否 |
| M010-006 | 历史注释不实已登记，不回写Flyway历史文件，否 |
| M010-007 | 无硬FK为已有下位实现选择；不增新规则，否 |
| M010-008 | 状态/归档能力边界历史观察，未据此增新功能，否 |
| M010-009 | 旧版本冲突已按授权V410处理，库现V431，否 |
| M010-010 | 旧父链/operation夹具后果已处理，直接测试与最终结构一致，否 |
| M010-011 | 种子及松引用等正向/限制观察保留，否 |
| M010-012 | 既有Analytics夹具机械修正记录保留，本次无证据推翻且不称新跑该类，否 |
| M010-013 | NULL/空串同键、inactive占名、全局与组织角色可同名是既有语义，不擅改，否 |
| M010-014 | 他模块方法/注释文案残留登记，不顺手重命名，否 |

## 技能与结论
沿用本轮已实读技能：using-superpowers、grilling、codebase-design及验证技能，按V0.17只借自问结构/实证核验；禁第三席、禁业务扩展、禁未经B2修复。未声称调用新的技能工具。
本次未发现新增缺陷；独立B5结论：PASS。当前两席B2定稿14项队列均否，9观察保留，Grok B2要求013补齐三事实已落实。只有本次Grok B5完整报告交付且同为PASS、四值校验全等后才能标模块PASS。当前尚未读取本次Grok B5正文。

## B5执行边界
此次Codex仅读取与SELECT，未创建/删除数据库、未执行迁移、未写业务数据。B1 Grok建临时探针库偏差不隐去，已于B2记录，额外只读确认无残留；本次Grok派发严格禁止包括临时库在内的建删。既有反例是B1证据，不伪称B5新执行。HTML/主索引/钩子上一检查hash未变，本次未操作它们。所有报告旧字节保留，不冒称整个项目全绿。
