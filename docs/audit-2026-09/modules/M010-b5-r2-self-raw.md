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
