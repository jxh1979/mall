CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M010 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-09T10:21:32Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `242f487233d028173cb9198eecd079f5d877405c`；工作区 tree `b565660e62131b162d24970c1ddc2e5d9912995b`（含 M009 已双 PASS 的 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M010「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段」，4 文件 203 行，映射 GEN-001–018 共 18 判据。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M010-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/迁移/HTML/锁/台账；数据库探针全部 SELECT 或 BEGIN…ROLLBACK。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| V005__p0_platform_master_data.sql | 109 | 3900 | 5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905 | 全读 |
| V048__access_role_organization_isolation.sql | 7 | 352 | e6a216f045479450b61766e54f80324122d3706099fc48f048a927d0094c83fd | 全读 |
| V318__business_entity_parent_layer.sql | 16 | 1300 | 466451ddb6e86f108f163e460282bedddc21f532d4289f80fc1c51e717d0b481 | 全读 |
| V354__business_entity_master_data.sql | 71 | 5625 | c32019a3457610df46e436f18572c0b71567779eddc12baf5fa59a837c22bbf3 | 全读 |

## 2. 可执行证据原文（/tmp/m010_b1_run.log）

```
START=2026-09-09T10:20:37Z
TREE=b565660e62131b162d24970c1ddc2e5d9912995b
=== BACKEND: PlatformJdbcIntegrationTests + SettlementEntityAttributionS1Tests ===
BACKEND_EXIT=1
2026-09-09T03:20:40.478-07:00  INFO 2543324 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 387 migrations (execution time 00:00.075s)
2026-09-09T03:20:40.488-07:00  WARN 2543324 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" has a version (409) that is newer than the latest available migration (399) !
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.425 s -- in com.complex.platform.settlement.SettlementEntityAttributionS1Tests
2026-09-09T03:20:43.811-07:00  INFO 2543324 --- [complex-api] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 387 migrations (execution time 00:00.044s)
2026-09-09T03:20:43.829-07:00  WARN 2543324 --- [complex-api] [           main] o.f.core.internal.command.DbMigrate      : Schema "public" has a version (409) that is newer than the latest available migration (399) !
[ERROR] Tests run: 13, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.868 s <<< FAILURE! -- in com.complex.platform.governance.PlatformJdbcIntegrationTests
[ERROR] com.complex.platform.governance.PlatformJdbcIntegrationTests.residualMigrationCreatesBindingContractTablesAndSeedsPermissions -- Time elapsed: 0.007 s <<< FAILURE!
[ERROR] Tests run: 21, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
=== PSQL read-only probes (complex_test) ===
== flyway rows for the four migrations
318|business entity parent layer|-705915847|t|2026-08-02 00:05:51.024461
354|business entity master data|-61418191|t|2026-08-02 00:05:51.213378
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
parent_entity_id|character varying|64|YES|
archive_reason|text||YES|
== business_entities constraints/indexes
business_entities_pkey|p|PRIMARY KEY (id)
chk_be_entity_role|c|CHECK (((entity_role)::text = ANY ((ARRAY['platform'::character varying, 'project_company'::character varying, 'management_company'::character varying, 'spv'::character varying, 'operation'::character varying])::text[])))
business_entities_pkey|CREATE UNIQUE INDEX business_entities_pkey ON public.business_entities USING btree (id)
idx_business_entities_parent|CREATE INDEX idx_business_entities_parent ON public.business_entities USING btree (parent_entity_id)
idx_business_entities_status|CREATE INDEX idx_business_entities_status ON public.business_entities USING btree (status)
== business_entities rows
be_amusement|游乐公司|management_company|be_proj_changsha|active|f
be_central_catering|中央餐饮公司|management_company|be_proj_changsha|active|f
be_ent_platform|娱乐平台公司|management_company|be_proj_changsha|active|f
be_media|传媒公司|management_company|be_proj_changsha|active|f
be_platform|平台公司|platform||active|f
be_proj_changsha|长沙项目公司|project_company|be_platform|active|f
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
is_self_operated|boolean|NO
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
=== static consumers ===
parent_entity_id in main: 7
spv/operation literals in main: 0
organization_id in RoleRecord: 1
TREE_AFTER=b565660e62131b162d24970c1ddc2e5d9912995b
END=2026-09-09T10:20:45Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文重新推导本模块承载的业务约束（BEQR-143/144、BSM-028、PGM-018/019 多对多协作、主体职责与四层名词；BSM-004 软删；BSM-007 平台管理员日常权限），逐条对照四份迁移 DDL/种子/注释；迁移是实现事实，不能当业务需求来源（GEN-018）。
- 顺从式线：M010 映射 GEN-001–018 逐项（§5）。
- 对抗式线：只读 `complex_test` 实态（表/列/约束/索引/种子行/flyway 历史/授权），治理外消费方追流（`JdbcAccessRepository`、`RoleRecord`、`JdbcPlatformRepository`、结算侧测试），可重放性与破坏性动作核对，版本序与环境漂移（V400–V409）核对。
- 依赖只读追流（不审）：`access/RoleRecord`、`access/JdbcAccessRepository`（roles 读写含 organization_id）、`governance/JdbcPlatformRepository`（四表读写）、`settlement/SettlementEntityAttributionS1Tests`（archive_reason）、M011 `PlatformJdbcIntegrationTests`（表/权限种子断言）、`V184`/`V203`/`V251`/`V259`/`V271`/`V339`/`V348` 等后续改动同表的迁移（只核对语义承接，不审）。
- 4 文件全文逐行读完（V005 109 行、V048 7 行、V318 16 行、V354 71 行）。

## 4. 可执行证据（本席实跑）

- E-1 现有测试：`PlatformJdbcIntegrationTests`（表/权限种子/绑定表断言）与 `SettlementEntityAttributionS1Tests`（archive_reason 列）实跑结果见 §2 原文；唯一红为 `residualMigration…` 399≠409（M009-017 环境）。
- E-2 psql 只读探针（§2 原文）：flyway_schema_history 005/048/318/354 均 success；`business_entities` 11 列（V005 9 列 + V318 parent_entity_id + V354 archive_reason），约束 `chk_be_entity_role`（5 值）、索引 parent/status；种子 6 行（be_platform / be_proj_changsha / 4 个 management_company，parent 链 platform←project←4 主体）；`roles.organization_id` 可空 + 部分索引，10 行角色全 NULL；8 个权限码现名为中文（V251 覆盖 V005 英文）且 role_platform_admin 8/8 授权；`merchants` 已被 V042/V147/V259/V271/V339/V348/V384 扩列且含未发布 `is_self_operated`（017）。
- E-3 静态：`parent_entity_id` 在 `backend/src/main/java` 0 处消费（运行记录中 7 处命中全在 resources/db/migration 的 V318/V354 本身）（M009-012 后仅存储与 V354 种子）；`archive_reason` 仅结算测试断言列存在；`organization_id` 由 `RoleRecord.organizationId`/`JdbcAccessRepository` 读写（非死列）；`'spv'`/`'operation'` 在 `backend/src/main/java` 0 处引用，仅 CHECK 白名单与 V354 注释。

## 5. 18 判据逐项核对

| 判据 | 结论 |
|---|---|
| GEN-001 必要性 | V005 四表/索引/权限种子均有消费方；V048 列由 RoleRecord/JdbcAccessRepository 读写；V318 列在 M009-012 后无代码消费方、仅 V354 种子写入（见 CX-M010-01）；V354 archive_reason 有测试消费；`spv`/`operation` 白名单值无任何消费方（见 CX-M010-02） |
| GEN-002 可执行正确性 | 四迁移在 complex_test 均 success；IF NOT EXISTS/ON CONFLICT/DROP IF EXISTS 可重放 |
| GEN-003 测试有效性 | M011 JDBC 断言表存在与 8 权限码/平台管理员授权；结算测试断言 archive_reason；父链与 CHECK 白名单无测试（登记 OBS-03） |
| GEN-004 信任边界 | 迁移无外部输入；status 列无 CHECK 由 M009 服务层白名单前置（004/036）；`parent_entity_id` 松引用无校验（M009-012 后无写入面） |
| GEN-005 授权 | V005 权限种子：8 码 domain/risk/requires_approval 与 V259/V204 后续一致；role_platform_admin 直接授予 manage（high）——BSM-007 日常功能自动同步、非四类高危，通过 |
| GEN-006 数据完整性 | 本库约定不加硬 FK（V318 注释）；引用完整性由 M009-006/007/027 服务层存在性校验等价保护；V354 ①②③④ 顺序保证种子不被 ② 归档；`spv/operation` 仍可经 SQL 直写（OBS-02） |
| GEN-007 并发幂等 | 全部语句可重放（IF NOT EXISTS / ON CONFLICT DO NOTHING/UPDATE / DROP CONSTRAINT IF EXISTS / UPDATE WHERE 幂等） |
| GEN-008 接口与模式一致性 | V005 英文 permission_name 已被 V251 中文覆盖（现态一致，OBS-01）；`chk_be_entity_role` 5 值 vs M009 服务层 3 值（CX-M010-02）；`parent_entity_id` 列 vs M009 无字段（CX-M010-01） |
| GEN-009 错误处理 | 迁移失败即 Flyway 阻断（显式失败）；无吞错 |
| GEN-010 安全 | 无秘密/明文口令；GRANT 由既有授权继承（V318 注释）；complex_app 四权限实态一致 |
| GEN-011 可观测 | flyway_schema_history 留痕；V354 ② 归档写 archive_reason 留痕 |
| GEN-012 资源边界 | 索引：status/parent/merchant/project/partial organization；无无界语句 |
| GEN-013 构建配置 | Flyway 校验通过（「Successfully validated 387 migrations」）；环境漂移见 017 |
| GEN-014 迁移正确性 | 顺序确定（V005<V048<V318<V354）；可在目标库执行；破坏性动作：V354 ② 仅 UPDATE 归档不 DELETE（软删铁律）；V354 ④ DROP CONSTRAINT IF EXISTS 后重建为加严——任何后续加严/删列（本模块可能的修复）属破坏性动作，须操作员授权（GEN-014 明文），且受 017 环境漂移阻塞（新迁移版本号 V400 已被未发布分支占用于 complex_test） |
| GEN-015 界面 | 无界面；permission_name 现态中文（V251） |
| GEN-016 文档可验证 | V318 头注与 `COMMENT ON COLUMN`、V354 头注「四层」把「平台→项目公司→管理公司→商户」固定隶属链写成当前事实，与 HTML 多对多协作原文相反（CX-M010-01）；V354 ④ 注释把 `spv`/`operation` 写成「历史通用运营主体」无 HTML 来源（CX-M010-02）；V354 引用 `terminology-canonical-v1.0.md` 非 HTML 正本（OBS-04） |
| GEN-017 分类 | 本报告按冲突→细化→技术性→添加；CX-M010-02 为添加项，须按 2.2 询问操作员 |
| GEN-018 第一性原理 | 每条落到 HTML 原文（BEQR-144/PGM-018/BSM-028 原句）、迁移原文、psql 输出或测试输出 |

## 6. Findings（临时编号 CX-M010-，正式 ID 在 B2 分配）

### CX-M010-01 V318/V354 把固定四层隶属链写入列注释、种子与迁移头注，与 HTML「多对多协作、不是固定隶属链」相反
- 位置：`V318__business_entity_parent_layer.sql:1-13`（头注「四层：平台(L1)→项目公司(L2)→管理公司(L3)→商户」、`COMMENT ON COLUMN business_entities.parent_entity_id IS '四层组织父级…'`）、`V354__business_entity_master_data.sql:48-61`（六主体种子 parent 链 be_proj_changsha→be_platform、四主体→be_proj_changsha）、`V354:70-71`（entity_role 注释「四层组织中的角色」）。
- HTML 原文：BEQR-144「物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链」；PGM-018「多对多协作，不是固定隶属层级…一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责」；BSM-028 同句。
- 现态：M009-012（B3-r1 已修）已从后台 API/记录/SQL 删除 parentEntityId；列在 complex_test 仍存在（可空），6 行种子 parent 链仍存在，DB 列注释仍宣称四层父级；`backend/src/main/java` 0 处消费方。
- 分类：冲突（迁移文档与种子数据把 HTML 否定的隶属链固化为当前事实）；依据类型 1（HTML 原文）+ 4（迁移原文、psql）；判据 GEN-016/GEN-008/GEN-001（列无消费方）；工程缺陷：是。
- 最小处置（B2 裁定，任一方案均为新迁移、不改历史迁移）：方案 A（最懒）新迁移改写 `COMMENT ON COLUMN` 为「历史列，不表示隶属关系；多对多协作以 business_entity_bindings 为准」并把 6 行种子 `parent_entity_id` 置 NULL；方案 B 直接 `DROP COLUMN parent_entity_id` 与索引（破坏性，GEN-014 须操作员明确授权）。两方案都需要新迁移版本号且受 017 阻塞：complex_test 已应用未发布 V400–V409，main 最新 V399，新迁移 V400 会在该库 Flyway 校验冲突——须操作员先处置测试库（回滚到 V399 或新建）并裁定版本号。建议入队：是（方案与版本号待操作员）。

### CX-M010-02 `chk_be_entity_role` 白名单含 HTML 没有的 `spv`、`operation` 两个角色码（添加项）
- 位置：`V354__business_entity_master_data.sql:63-71`（CHECK 五值；注释「spv / operation=历史通用运营主体」）；complex_test 实态同。
- HTML 原文：PGM-018/019、BEQR-145/146 只定义顶层平台、城市项目公司、域主管公司（业态主体）三类角色；M009-005 已把服务层白名单收为三值，025 登记转 M010。
- 现态：`spv`/`operation` 在 `backend/src/main/java` 0 处引用；complex_test 0 行；V354 ② 把开发库 32 行 `operation` 残留归档为 inactive 但仍保留其角色码，且 CHECK 仍允许新插入 `operation`/`spv`（经 SQL 直写）。
- 分类：添加（代码/迁移引入 HTML 没有的业务枚举值）；依据类型 1 + 4；判据 GEN-017/GEN-008/GEN-001；工程缺陷：是（服务层与库层枚举不一致、无来源的业务值）。
- 处置须经 2.2 询问操作员（本席不代答）：问题「`business_entities.entity_role` 是否保留 HTML 之外的 `spv`、`operation` 两个角色码？答保留→登记不修；答删除→新迁移把 CHECK 收为三值（历史 `operation` 行须先迁为 `management_company` 或以 NOT VALID 加约束，方案随答复裁定）」。同样受 017 版本号阻塞。

### CX-M010-03（观察级，见 OBS-01）V005 权限种子英文名
- V005:79-88 以英文 permission_name 落种子；V251 已全部改为中文（psql 现态 8 码中文）。现态无缺陷；历史迁移不改。分类：技术性；工程缺陷：否；登记。

## 7. 观察（登记、不入队）

- OBS-01：V005 permission_name 英文由 V251 覆盖（现态中文），符合 PGM-001 全中文；不重开历史迁移。
- OBS-02：V354 ② 归档 `operation` 行但 ④ 仍允许 `operation`/`spv` 插入；服务层（M009-005）已拦，SQL 直写仍可绕过——随 CX-M010-02 的操作员答复处置。
- OBS-03：父链/CHECK 白名单无 JDBC 测试（M011 只断言表与权限存在）；修复入队时按 TDD 补最小断言。
- OBS-04：V354 头注引用 `terminology-canonical-v1.0.md`（非三份 HTML）作口径来源；本审计以 HTML 为最终解释权，该文档不是依据（GEN-018）。
- OBS-05：本库约定「不加硬 FK」（V318 注释）；引用完整性由 M009 服务层存在性校验等价保护（006/007/027），登记不改。
- OBS-06（环境，M009-017 同源）：complex_test flyway 409 > main 399，含未发布 V400–V409 与 `merchants.is_self_operated`；任何 M010 新迁移都会与该库版本序冲突——须操作员处置后才能进入 B3。
- OBS-07：`roles.organization_id`（V048）10 行角色全 NULL（即全部平台全局角色），列与部分索引由 `RoleRecord`/`JdbcAccessRepository` 读写，非死列。

## 8. 结论

审计结论：**FAIL**（B1 修复前）。2 条 finding 有工程缺陷（CX-M010-01 冲突、CX-M010-02 添加）；1 条添加项须在 B2 按 2.2 弹窗询问操作员；两条处置均需新迁移且受 017 环境漂移阻塞（版本号与测试库须操作员裁定）；7 条观察登记。三份 HTML 本身未发现需上报的问题。本席 B1 未读取 Grok raw；本报告落盘后才读取并进入 B2 归并。


## M010 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M010-b1-self-raw.md` V0.01 前缀 20270 bytes SHA256 37f97e608bd5276184a2fea4d7be72d77daf1863e0ca790eeda06684083c52cc；`modules/M010-b1-grok-raw.md` V0.01 前缀 36902 bytes SHA256 d77a1da6c344fd76981be0200a79196f3043e109e18bf1683d6156491ad99e8b（Grok B1：FAIL，GR-M010-001/002/003，OBS-01…15）；候选 `findings.md` V1.28 490508 bytes SHA256 a55a9b5b7644ec40750d9cdb1d9b4caabcef645dc891b2cf0022ed5c833d0311「当前 M010 B2-r1 整批候选」M010-001…011 及 2.2 答复段；`questions/M010-002.md`（V0.02）/`M010-003.md`（V0.02）首行答复均「删除」。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。按 `receiving-code-review` 收窄：Grok GR-003（roles 全局 UNIQUE）经 psql 核实（`roles_role_name_key UNIQUE (role_name)` 来自 V003，V048 未替换）后接受；Grok OBS-04「V005 已 GRANT 不实、GRANT 来自 V140」经 grep 核实。
- 差集：Grok GR-001/002(A)/002(B)/003 → 001/002/003/004；OBS-01…15 → 005–011 或既有 M009-017；席甲 CX-01/02/03、OBS-01…07 → 001/002/003/005/006/007/009/011。无遗漏、无改写 Grok 原意。操作员答复与裁定已抄入候选（002/003 删除、001 方案 B、009 V410）。

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（冲突／是，方案 B 已授权） | 列/注释/种子固化 HTML 否定的隶属链；后台 0 消费方；DROP INDEX + DROP COLUMN 为最彻底且最懒的一次性处置（无需再维护列注释）；结算测试父链断言随批最小伴生（同 M009-006 夹具先例），不另开 ID |
| 002 | PASS（添加／是，操作员答删除） | 三份 HTML 无 SPV 角色；服务层已拒；0 行；新迁移收窄 CHECK |
| 003 | PASS（添加／是，操作员答删除） | 三份 HTML 无「通用运营主体」角色；存量 operation 行的过渡由 B2 定稿：本席建议先 `UPDATE … SET entity_role='management_company' WHERE entity_role='operation'`（这些行已被 V354 归档为 inactive，改角色不影响任何在用数据）再加严 CHECK，避免 NOT VALID 留下永久半有效约束；M011 :66 夹具 operation→management_company 为下游最小伴生 |
| 004 | PASS（细化／是） | V048 声称组织隔离但 V003 的全局 UNIQUE(role_name) 未替换，首轮正常（两个组织各建「店长」）即撞 23505；处置：同一新迁移删 `roles_role_name_key`，建 `UNIQUE (COALESCE(organization_id, ''), role_name)` 表达式唯一索引；失败测试放在 M011 `PlatformJdbcIntegrationTests`（本批可修文件，JDBC 直写 roles 两组织同名可共存/同组织同名仍拒），不新建 access 模块测试文件 |
| 005–008、010、011 | PASS（否） | 与 B1 两席观察一致，均无本模块可修的工程缺陷或属他模块/正向 |
| 009 | PASS（技术性环境／否，操作员已裁定 V410） | 新迁移在 complex_test 直接应用（409→410），两条 MigrationVersions 断言随之一致；main 库跨 400–409 空档由未发布分支合并时处理（操作员知悉）；不需要在迁移或配置上做额外动作 |

本席 B2-r1 票：**PASS**（队列是＝001、002、003、004；否＝005–011）。B3 边界：一条新迁移 `V410`（按 ID 分段）、M011 `PlatformJdbcIntegrationTests` 最小断言、下游最小伴生（M011 :66 夹具、settlement 父链断言）；不改 V005/V048/V318/V354 正文、三份 HTML、锁、权限种子。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。

### 席甲 B2 票勘误（V0.03 追加，在读取 Grok B2 追加段之前落盘）

009 行本席原写「新迁移在 complex_test 直接应用（409→410）…不需要在迁移或配置上做额外动作」——**错误**。本席随后在临时副本实测（2.3 类型 4）：`CREATE DATABASE complex_probe_m010 TEMPLATE complex_test`，仓库临时副本加入空 `V410__probe.sql`，运行 `SettlementEntityAttributionS1Tests#threeNewRevenueStreamsAreAccepted`（`SETTLE_TEST_DB_URL` 指向探针库）→ Spring 上下文启动失败，Flyway 12.4.0 `validate-on-migrate: true` 报 `Detected applied migration not resolved locally: 400 … 409`（十条），探针库版本停在 409、V410 未应用；探针库与副本已删除，complex_test 未改（仍 409）。结论：只要 complex_test 仍含未发布 V400–V409 的 flyway 行，加入任何 ≥V410 的新迁移都会使全部 Spring 数据库测试在该库上失败；「V410 起编号」本身成立，但 B3 的绿阶段必须先由操作员处置 complex_test（回滚/重建到 main V399 或等价），或另行授权测试配置忽略缺失迁移。本勘误不改 001–011 分类与队列，仅更正 009 的环境表述与 B3 前置条件。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M010 B1 当前锁全模块独立审计
版本：V0.04

席甲：Codex。B3累计2；本次未执行修复。先派Grok后独立读取源文件、当前数据库与历史授权；本报告落盘前未读取本次Grok正文。本次完整复核，不把此前单席补充PASS当双席结论。

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
文件本次SHA与刚完成的只读复查一致，未发生源字节漂移。此前按Flyway逐行CRC算法核验上述五checksum全等；不把此前算法执行称为本次重新运行。
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

## 全部14既有ID复核
| ID | 现态与候选当前修复队列 |
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
| M010-013 | NULL/空串同键、inactive占名、跨组织同名是既有语义，不擅改，否 |
| M010-014 | 他模块方法/注释文案残留登记，不顺手重命名，否 |

## 技能与结论
沿用本轮已实读技能：using-superpowers、grilling、codebase-design及验证技能，按V0.17只借自问结构/实证核验；禁第三席、禁业务扩展、禁未经B2修复。未声称调用新的技能工具。
本次未发现新增缺陷；独立B1结论：PASS。上述队列为席甲候选，须B2两席归并后定稿，不能据此直接宣称模块双PASS。


## M010 当前B2席甲票
版本：V0.05
输入两份M010-b1原始报告当前V0.04/self与V0.03/grok及findings V3.51当前候选整批14项与9观察。已读两席完整B1，差集0；新ID0、当前队列14项均否；分类与历史批准相符。候选完整保留Grok观察，不将旧模式当现态，不声称Grok未建临时库。席甲B2结论：PASS。等待Grok独立B2票后才定稿。

## M010 B2定稿措辞补齐
版本：V0.06
输入：本模块两份b1 raw（self B1 V0.04/B2 V0.05；Grok B1 V0.03/B2 V0.04）及findings V3.52当前M010段。接收Grok B2 PASS，013补齐NULL/空串同键、全局与组织可同名、inactive占名三事实；九观察路由不改宿主ID。只读确认临时库无残留。分类和队列无变，席甲B2 PASS保持；整批14项均否，双PASS定稿，跳过新增B3/B4，进入B5-r2。
