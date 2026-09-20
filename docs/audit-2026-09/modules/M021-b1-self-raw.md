CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理）。2026-09-20T17:21:31Z
- 控制合同 V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本主工作区 HEAD `e456ba96`，origin/main `a60ba660`；工作区 tree `17976ffbba72adb367348c4367298f8eb48a3a8d`。三份业务 HTML 只读。
- 模块 M021「商户档案、商户类型与商户侧门户／数据库迁移／第1段」，5 个迁移文件（计划有效内容行 103；§1 实算），映射 18 判据（GEN-001…018）。历史未审。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M021-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/迁移/文档/HTML/锁；命令全部只读（`git write-tree` 用临时索引；psql 只 SELECT complex_test）。已应用迁移不可改写（Flyway 校验和，complex_test 五条 checksum 见 §2 探针 2）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V147__merchants_owner_person.sql | 6 | 537 | 70a00f5d8a1936fac2e8bdf54d61150a5e30257a2e80deaff5dbd9049bb6734a | 全读 |
| backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql | 9 | 926 | 2b162b399a1f84aa419f90977dbe8c29947dd006c66aa8f40f76d74f4489d6ff | 全读 |
| backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql | 18 | 1211 | b082220f5bde8aba00f3ac590c4604badb1d660c641fae7d8a9961f517214e81 | 全读 |
| backend/src/main/resources/db/migration/V348__merchant_business_format.sql | 33 | 2521 | b01046851bb04ac5de83fba558ca052220f415f80e561fb5eace2888b142df4e | 全读 |
| backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql | 37 | 2775 | 36a8fc281957253926c07be557b3ab6e960a06f941312af14f6732422e588e2f | 全读 |

只读依赖（追流不审）：V002/V197/V198/V251/V354/V366/V375/V427 迁移、`governance/PlatformService`（MERCHANT_BUSINESS_FORMATS）、`leasing/LeasingService`、`settlement/TicketDistributionSettlementService` + `JdbcSettlementBillRepository`、`gate/GateRequestContext`、`merchantportal/MerchantPortalService`（M020 已修）、`apps/web/src/admin/adminRoutes.ts`、三份 HTML 被迁移注释点名的原句。

## 2. 可执行证据原文（/home/james/audit-tmp/m021_b1_run.log）

```
START=2026-09-20T17:19:16Z HEAD=e456ba96 origin/main=a60ba660
== 0. five files (lines/bytes/sha256)
backend/src/main/resources/db/migration/V147__merchants_owner_person.sql 6 537 70a00f5d8a1936fac2e8bdf54d61150a5e30257a2e80deaff5dbd9049bb6734a
backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql 9 926 2b162b399a1f84aa419f90977dbe8c29947dd006c66aa8f40f76d74f4489d6ff
backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql 18 1211 b082220f5bde8aba00f3ac590c4604badb1d660c641fae7d8a9961f517214e81
backend/src/main/resources/db/migration/V348__merchant_business_format.sql 33 2521 b01046851bb04ac5de83fba558ca052220f415f80e561fb5eace2888b142df4e
backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql 37 2775 36a8fc281957253926c07be557b3ab6e960a06f941312af14f6732422e588e2f
== 1. vs HEAD / vs origin/main (expect empty) / porcelain
== 2. flyway history (complex_test, SELECT only)
147|merchants owner person|t|235932490
199|merchant portal zone module switch|t|-1183753096
200|merchant portal zone permissions|t|1214979897
348|merchant business format|t|448719403
384|merchant business format media|t|-663687440
flyway top: 427
== 3. V147 owner_person: column actual / other owner_person columns / code use / HTML
merchants|owner_person|character varying|120|YES
production_readiness_items|owner_person|character varying|128|YES
projects|owner_person|text||YES
code use: ProjectRecord.java MerchantProfileRequest.java ProjectRequest.java MerchantRecord.java PlatformService.java JdbcPlatformRepository.java 
V147 cites governance map :429; actual line: 494
== 4. V199 module switch: row / master switch / V198 paradigm / frontend group / backend enforcement
tcs_mp_mod_merchant|merchant_portal|terminal.enabled|module|merchant|t
tcs_term_merchant_portal|merchant_portal|terminal.enabled|global|global|t
V197 master row line: 12; V198 module rows: 13
adminRoutes group merchant routes: 6; X-Module-Code enforcement files: GateRequestContext.java 
== 5. V200 permissions: rows actual / V251 i18n / grants / comment L10 vs service
merchant.portal.fee_recipient.manage|管理商户端自有费用通知接收人|merchant_portal|medium|f|active
merchant.portal.settlement.dispute|发起商户端自有未结结算争议|merchant_portal|medium|f|active
merchant.portal.settlement.view|查看商户端自有结算|merchant_portal|medium|f|active
merchant.portal.fee_recipient.manage|role_platform_admin
merchant.portal.settlement.dispute|role_platform_admin
merchant.portal.settlement.view|role_platform_admin
V251 lines: 255 256 257 
V200 L10: -- 平台管理员默认可监督商户自助层(与其它域 role_platform_admin 全授范式一致)。
service scope (post M020): 24: * fail-closed 抛 MERCHANT_SCOPE_REQUIRED(红线④:不因平台管理员身份整类关掉商户隔离;平台监督走各域
merchant roles seeded: 0
== 6. V348/V384 vocabulary: CHECK actual / same-vocabulary constraints / code sets / HTML
merchants|chk_merchants_business_format|CHECK (((merchant_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
leasing_leads|chk_leasing_leads_business_format|CHECK (((intended_business_type IS NULL) OR ((intended_business_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[]))))
settlement_bill_format_shares|chk_sbfs_format|CHECK (((business_format)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'other'::character varying])::text[])))
merchant_type column: character varying(32) nullable=NO
index: idx_merchants_business_format
code set: 46: public static final Set<String> MERCHANT_BUSINESS_FORMATS = 47- Set.of("bar", "catering", "retail", "amusement", "media", "other"); 
migrations carrying the five/six-value vocabulary: V015__p0_open_platform_integration_adapter_boundary.sql V030__p1_integration_retry_compensation_reconciliation.sql V223__settlement_distribution_configs_batch2.sql V348__merchant_business_format.sql V362__settlement_invoice_list.sql V366__settlement_bill_format_shares.sql V382__thirdparty_pass_ticket_channel.sql V384__merchant_business_format_media.sql 
V366 CHECK line: 37:    CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'other')),
later migration touching chk_sbfs_format: V366__settlement_bill_format_shares.sql 
media share write path: 151:            formatShares.put(fr.businessFormat(),; 365:            repository.upsertFormatShare(new SettlementBillFormatShareRecord(
upsert SQL: 832:                INSERT INTO settlement_bill_format_shares (
ENTITY_BENEFICIARY_FORMATS media: 90:            FORMAT_MEDIA, "performance_ticket");
tests exercising media share on real DB: 
HTML enum L195: 酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他
HTML business map: 1309
V384 cited sentences present: 演出归传媒公司=1; 演出票 → 传媒公司=3
V348 cited five-value sentence present now: docs/business-system-map-for-operations-v0.1.html:0 docs/business-enum-quick-reference-v0.1.html:0 
== 7. V384 seed: row actual / V354 be_media / V375 paradigm / leasing autoCreate
rsa_performance_ticket_default|NULL|performance_ticket|be_media|entity|active|f|migration:V384
V354 be_media line:     ('be_media',            '传媒公司',     'management_company', 'be_proj_changsha', 'active', now(), now()); V375 file: V375__settlement_pool_undistributed_and_unverified_attribution.sql
LeasingService.autoCreateMerchantForSign: 272
== 8. migration numbering state
origin/main max: V426__space_type_legacy_disabled_to_archived.sql; HEAD max: V426__space_type_legacy_disabled_to_archived.sql; worktree max: V427__m009_project_profiles_and_associations.sql
A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
== 9. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=17976ffbba72adb367348c4367298f8eb48a3a8d
END=2026-09-20T17:19:17Z
```

## 3. 三线审计方法与覆盖

- 对象口径：5 迁移 103 行逐行读完；18 判据逐项；每条迁移声称按对象追 complex_test 实态（flyway 427）、后续迁移与现行代码词表、三 HTML 原句（§2 探针 2–8）。
- 第一性原理线：V147 把治理地图「建立商户档案、运营主体、负责人、第一联系人和第二联系人」（现 L494）的「负责人」落成列；V199/V200 是商户专区（业务地图 L1322 自助五项）的终端模块开关与最小权限点；V348/V384 是「每个商户必须归入一个业态」（业务地图 L1309、枚举 L195，现行六值含 media）的数据库封口与「演出票 → 传媒公司」的归属种子。三问：用途均有上位；实际行为见 §4；未经验证前提——V384 自称「词表一次性放开」「同词表同步」，实际同词表的第三处 CHECK 未放开（CX-001）。
- 顺从式线：18 判据逐项（§5）。对抗式线（grilling）：重复执行是否安全（IF NOT EXISTS / ON CONFLICT / NOT EXISTS / DROP IF EXISTS+ADD 在 Flyway 单事务内）；顺序依赖是否成立（V197→V199、V198 范式、V251 中文化、V354 be_media→V384 种子、V375 范式）；封口词表与 HTML/代码是否一致（merchants、leasing_leads 六值一致；settlement_bill_format_shares 五值不一致）；录入一个 media 业态商户并让其权益进通票月结会怎样（探针 6：`aggregateFormatRevenue` 取 merchants.merchant_type → `upsertFormatShare` 写 business_format=media → 违反 `chk_sbfs_format` → 整张月结单生成失败）；注释点名的 HTML 句是否还在（V147 行号漂移、V348 五值句已被六值句取代，均为不可改写的历史注释）。
- 迁移模块本轮不写库、不跑测试；本席禁止修复。

## 4. 与现行事实的对照（要害）

| 迁移声称 | 现行事实（§2 探针） | 结论 |
|---|---|---|
| V384:13-19「词表一次性放开」、:21「同词表同步(V348 ④ 同因:词表不一致会…卡在 CHECK 上)」——只重建 merchants 与 leasing_leads 两处 CHECK | complex_test 同词表 CHECK 共三处：`chk_merchants_business_format`、`chk_leasing_leads_business_format` 六值；`settlement_bill_format_shares.chk_sbfs_format`（V366:37，早于 V384）仍五值、无后续迁移触及；`JdbcSettlementBillRepository.aggregateFormatRevenue:186` 业态取 `merchants.merchant_type`，`TicketDistributionSettlementService:151/:359-368` 每个业态 upsert 一行 `business_format`；`ENTITY_BENEFICIARY_FORMATS:90` 已含 media→performance_ticket；无任何测试在真实库上写 media 份额 | CX-001 |
| V147:1 引「platform-governance-map :429」 | 该句现在 L494（HTML 后续修订致行号漂移）；列 `merchants.owner_person varchar(120)` 已建、`MerchantRecord/MerchantProfileRequest/JdbcPlatformRepository` 在用 | 通过（登记 OBS-1） |
| V199 模块开关行 + 注释「V197 总开关 / V198 范式 / adminRoutes group=merchant / X-Module-Code 硬拒」 | 行 `tcs_mp_mod_merchant` enabled=t，总开关行在；V198 13 条 module 行同范式；adminRoutes group merchant 6 条；`GateRequestContext` 执行 X-Module-Code | 通过 |
| V200 三权限码 + 授 role_platform_admin；L10「平台管理员默认可监督商户自助层」 | 三码 active/medium/无需审批，名称已由 V251:255-257 中文化；仅 role_platform_admin 持有、出厂无商户角色（全部 merchant.* 码同此，商户角色运行时授出）；M020 已定稿 fail-closed → 平台管理员持码但过不了商户 scope，L10 注释过时（M020-007 转入） | 通过（登记 OBS-2/3） |
| V348 五值封口 + 旧词表归一到 other + leasing_leads 同步；注释引 HTML 五值句 | V384 已扩为六值，与 HTML（业务地图 L1309、枚举 L195）及 `PlatformService.MERCHANT_BUSINESS_FORMATS` 一致；五值句已不在 HTML（历史注释）；`merchant_type varchar(32) NOT NULL`、索引在；`LeasingService.autoCreateMerchantForSign:272` 存在 | 通过（登记 OBS-4） |
| V384 ③ 演出票归属种子（依赖 V354 be_media、V375 范式） | 行 `rsa_performance_ticket_default`（project NULL/be_media/entity/active/非草案）在；V354 有 be_media；V375 文件在；HTML 两处引句均在 | 通过 |

## 5. 18 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性最小性 | 通过 | 五迁移各有 HTML 上位与现行消费方（列/开关/权限/CHECK/种子均在用） |
| GEN-002 可执行正确性 | 不通过 | CX-001（media 业态首次进通票月结即违反 CHECK） |
| GEN-003 测试有效性 | 不通过（并入 CX-001） | 无测试在真实库写 media 业态份额；V384 的放开只被 merchants 侧覆盖 |
| GEN-004 信任边界校验 | 通过 | 词表由数据库 CHECK 封口（merchants/leasing_leads） |
| GEN-005 授权与最小权限 | 通过（登记） | 三码最小权限点、无需审批的自助动作；平台管理员全授为全仓范式，商户角色运行时授出（OBS-3） |
| GEN-006 数据完整性 | 不通过 | CX-001（同一词表三处封口不一致，合法业态写不进留痕表） |
| GEN-007 并发与幂等 | 通过 | ON CONFLICT DO NOTHING / IF NOT EXISTS / NOT EXISTS；Flyway 单事务 |
| GEN-008 接口与模式一致性 | 不通过 | CX-001（DB 封口 vs 代码词表 vs HTML 六值） |
| GEN-009 错误处理与恢复 | 通过（登记） | 迁移失败即整体回滚；V384 注释「fail-loud」口径与种子一致 |
| GEN-010 安全与敏感信息 | 通过 | 无秘密、无 PII 种子 |
| GEN-011 可观测与审计证据 | 通过 | 种子 updated_by=migration:V384 可溯源；开关 reason 有中文说明 |
| GEN-012 资源与性能边界 | 通过 | V348 索引 idx_merchants_business_format 在；无全表重写风险（UPDATE 仅命中旧词表行） |
| GEN-013 构建配置与依赖 | 无本面 | 无构建文件 |
| GEN-014 数据库迁移正确性 | 不通过 | CX-001；其余：版本顺序与依赖成立（V197→V199、V354→V384），可重复安全，complex_test 五条 success |
| GEN-015 界面可用性 | 无本面 | 权限名称已中文（V251） |
| GEN-016 文档可验证性 | 通过（登记） | OBS-1/2/4：历史注释的行号/引句/口径漂移，已应用迁移不可改写 |
| GEN-017 业务来源分类 | 通过 | CX-001 细化（HTML 六值业态 + 演出票→传媒公司的下位实现），无添加项 |
| GEN-018 第一性原理证据 | 通过 | §2 命令原文（complex_test 只读 SELECT、git、grep）+ 迁移/源码原文 + HTML 逐字 |

## 6. 本次 Findings（临时 ID CX-M021-00N；正式 ID 由 B2 分配）

### CX-M021-001 · 细化 · V384 放开业态词表时漏同步 `settlement_bill_format_shares.chk_sbfs_format`，media 业态份额写不进留痕表

- 位置：`V384__merchant_business_format_media.sql:13-26`（「词表一次性放开」「同词表同步」，只重建 `chk_merchants_business_format` 与 `chk_leasing_leads_business_format`）；同词表第三处 `V366__settlement_bill_format_shares.sql:37` `CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar','catering','retail','amusement','other'))`（V366 早于 V384，属 M319，当时五值正确）。
- 事实：complex_test（flyway 427）`chk_sbfs_format` 仍五值、无后续迁移触及；`aggregateFormatRevenue:186` 以 `merchants.merchant_type` 为业态，`TicketDistributionSettlementService:151` 逐业态建 FormatShare、`:359-368` 逐业态 `upsertFormatShare`（`JdbcSettlementBillRepository:832-851` INSERT business_format）；`ENTITY_BENEFICIARY_FORMATS:88-90` 已把 media 归 performance_ticket。首次正常流：招商录入 media 业态商户（V384 放开后合法）→ 其权益进联合通票并核销 → 月结生成 → INSERT business_format=media 违反 CHECK → 整张结算单生成失败（其它业态一并出不了单）。V384 头注自述要修的正是「演出提供方…结算按业态分流」，其放开不完整使该目的落空。
- 依据类型：4（complex_test pg_constraint 原文、grep）+ 2（迁移/源码原文）。HTML 上位：枚举 L195「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他」、业务地图 L1309 同句与「演出票 → 传媒公司」。判据：GEN-014 / GEN-006 / GEN-008 / GEN-002 / GEN-003。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：已应用迁移不可改写 → 新增一条前向迁移（编号取主工作区现有最大号之后，V427 为 M009 遗留暂存）：`ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format; ADD CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar','catering','retail','amusement','media','other'))`；TDD：先落一条真实库断言（media 业态份额行可写入；五值外的仍被拒），RED 后加迁移转 GREEN；随迁移重生成 data-isolation spec 等门禁产物并跑全链 verify:predeploy。不改 V366/V384 原文件、不改代码词表、不改 HTML。

## 7. 观察（登记、不入队）

- OBS-1：V147:1 注释引治理地图「:429」，该句现位于 L494（HTML 修订致行号漂移）；历史注释不可改写，登记。
- OBS-2（M020-007 转入）：V200:10「平台管理员默认可监督商户自助层」与 M020 定稿的 fail-closed 不符；授给 role_platform_admin 的三码对其不可用（过不了商户 scope），属全仓「平台管理员全授」范式的惰性授予；迁移不可改写，登记。
- OBS-3：V200 只给 role_platform_admin 播种，出厂无商户角色（complex_test 0 个），全部 merchant.* 码同此——商户角色由平台运行时授出（前端 ADM-MER-003…006 按同码门控）；不立项。
- OBS-4：V348:2-4 注释引的 HTML 五值句已被六值句取代（V384 已跟进）；V366:27 列注释仍写「V348 五值」（M319）；历史注释，登记。
- OBS-5：V348 ①/④ 把旧词表一次性归一到 other（注释自述开发环境数据最后全删）；生产首发为空库，不触发；登记。
- OBS-6：主工作区有 M009 遗留暂存迁移 V427（complex_test 已应用、origin/main 最大 V426）；本模块新迁移编号须避开，属环境事实，B3 时记台账。
- 无添加项；无 HTML 修改申请；无 questions。

## 8. 结论

本席 B1 票：**FAIL**（CX-M021-001 为工程缺陷且建议入队；处置＝新增前向迁移 + 真实库回归）。5 迁移全文三线审完、18 判据逐项核对、complex_test 只读实态为证据；未写库、未改任何文件。本报告落盘前未读取 `M021-b1-grok-raw.md` 任何正文。


## M021 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T17:35:38Z。输入：modules/M021-b1-grok-raw.md V0.01（35139 bytes，SHA256 cd58fda5bcc82b083b5042972e946bdf6adfd93f33eb97b3456a2b59e08fdcfa）、modules/M021-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M021 B2-r1 整批候选」段（全文件 SHA256 522291065c922fb5576248c2772564cf635c30fd6b27684d528ebde4c1431659）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01/02 与席甲 CX-001 全部归并（GR-02+CX-001→001；GR-01+席甲 OBS-2/3→002）；双方观察全部登记；无漏并、无改写。
- 001 分类：席甲 B1 原判细化；按 2.1「冲突→细化→技术性→添加」顺序，现行 HTML 六值业态明文与拒绝 media 的五值封口不能同时成立，采 Grok「冲突」，队列是。处置只新增前向迁移 V428 + 既有真实库测试类加 media 一例（跨单元 TDD），不改 V366/V384/结算 Java/HTML。
- 002：席甲核源业务地图 L587-588「平台管理员拥有全部日常功能权限…不会出现"管理员反而没权限"」——三码为日常功能码，授予是 HTML 要求；Grok 拟 DELETE 授予会与该明文冲突，不采。剩余只有一句不可改写的历史注释，无可执行修复面；按 M010-006 / M016-004 先例 技术性/否/否 登记；M020-007 的 V200 部分就此闭合。
- 环境事实登记：V427（M009 遗留暂存）与 V428 的并入/部署顺序风险（validate-on-migrate、无 out-of-order、生产库 385）报操作员。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
