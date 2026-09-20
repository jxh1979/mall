CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M021 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T18:00:30Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `a5e4fadfe03ff042c00bd240719b76d9ab7341d6`。
- 模块 M021 5 迁移（已应用、零改写）+ B3 新增 V428（§1 全读），18 判据；B3 累计 1。
- 顺序：Grok 已先派（modules/M021-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；psql 只 SELECT；测试实跑经既有夹具清场。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/resources/db/migration/V147__merchants_owner_person.sql | 6 | 537 | 70a00f5d8a1936fac2e8bdf54d61150a5e30257a2e80deaff5dbd9049bb6734a | 全读 |
| backend/src/main/resources/db/migration/V199__merchant_portal_zone_module_switch.sql | 9 | 926 | 2b162b399a1f84aa419f90977dbe8c29947dd006c66aa8f40f76d74f4489d6ff | 全读 |
| backend/src/main/resources/db/migration/V200__merchant_portal_zone_permissions.sql | 18 | 1211 | b082220f5bde8aba00f3ac590c4604badb1d660c641fae7d8a9961f517214e81 | 全读 |
| backend/src/main/resources/db/migration/V348__merchant_business_format.sql | 33 | 2521 | b01046851bb04ac5de83fba558ca052220f415f80e561fb5eace2888b142df4e | 全读 |
| backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql | 37 | 2775 | 36a8fc281957253926c07be557b3ab6e960a06f941312af14f6732422e588e2f | 全读 |
| backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql（B3 新增） | 13 | 1215 | 239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m021_b5r1_run.log；B1 同一只读取证脚本修后重跑 + 真实库测试复跑）

```
START=2026-09-20T18:00:24Z HEAD=e456ba96 origin/main=a60ba660
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
flyway top: 428
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
settlement_bill_format_shares|chk_sbfs_format|CHECK (((business_format)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
merchant_type column: character varying(32) nullable=NO
index: idx_merchants_business_format
code set: 46: public static final Set<String> MERCHANT_BUSINESS_FORMATS = 47- Set.of("bar", "catering", "retail", "amusement", "media", "other"); 
migrations carrying the five/six-value vocabulary: V015__p0_open_platform_integration_adapter_boundary.sql V030__p1_integration_retry_compensation_reconciliation.sql V223__settlement_distribution_configs_batch2.sql V348__merchant_business_format.sql V362__settlement_invoice_list.sql V366__settlement_bill_format_shares.sql V382__thirdparty_pass_ticket_channel.sql V384__merchant_business_format_media.sql V428__m021_settlement_format_share_media.sql 
V366 CHECK line: 37:    CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'other')),
later migration touching chk_sbfs_format: V366__settlement_bill_format_shares.sql V428__m021_settlement_format_share_media.sql 
media share write path: 151:            formatShares.put(fr.businessFormat(),; 365:            repository.upsertFormatShare(new SettlementBillFormatShareRecord(
upsert SQL: 832:                INSERT INTO settlement_bill_format_shares (
ENTITY_BENEFICIARY_FORMATS media: 90:            FORMAT_MEDIA, "performance_ticket");
tests exercising media share on real DB: SettlementFormatShareS11Tests.java 
HTML enum L195: 酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他
HTML business map: 1309
V384 cited sentences present: 演出归传媒公司=1; 演出票 → 传媒公司=3
V348 cited five-value sentence present now: docs/business-system-map-for-operations-v0.1.html:0 docs/business-enum-quick-reference-v0.1.html:0 
== 7. V384 seed: row actual / V354 be_media / V375 paradigm / leasing autoCreate
rsa_performance_ticket_default|NULL|performance_ticket|be_media|entity|active|f|migration:V384
V354 be_media line:     ('be_media',            '传媒公司',     'management_company', 'be_proj_changsha', 'active', now(), now()); V375 file: V375__settlement_pool_undistributed_and_unverified_attribution.sql
LeasingService.autoCreateMerchantForSign: 272
== 8. migration numbering state
origin/main max: V426__space_type_legacy_disabled_to_archived.sql; HEAD max: V426__space_type_legacy_disabled_to_archived.sql; worktree max: V428__m021_settlement_format_share_media.sql
A  backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
?? backend/src/main/resources/db/migration/V428__m021_settlement_format_share_media.sql
== 9. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=9bf8e2aa55730b56294cae3b352b451198a2ce68
== 10. tests rerun (SettlementFormatShareS11Tests, complex_test, three vars only)
B5_MVN_EXIT=0
com.complex.platform.settlement.SettlementFormatShareS11Tests.txt: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.239 s -- in com.complex.platform.settlement.SettlementFormatShareS11Tests
== 11. fix-state (SELECT only)
428|m021 settlement format share media|t|1696678370
same-vocabulary CHECKs without media: 0
V428: 13 lines 239d44ecd771a1c849e5ad56aa96995e1201ab068d65c6f72a868c65b1909589
test leftovers: merchants=0 shares(media)=0
END=2026-09-20T18:00:30Z
```

## 3. 三线审计

- 第一性原理线：上位句同 B1（治理地图 L494 负责人句、业务地图 L1309/枚举 L195 六值业态、演出票→传媒公司、L1322 商户专区、L587-588 平台管理员全部日常功能权限）。修后逐条追实态（探针 2–8）：flyway 428 success；同词表 CHECK 三处（merchants、leasing_leads、settlement_bill_format_shares）均六值，与 `PlatformService.MERCHANT_BUSINESS_FORMATS` 与 HTML 逐值一致；V147/V199/V200/V348/V384 五条 checksum 未变（零改写）；开关行、权限行、归属种子行同 B1。
- 顺从式线：18 判据逐项——GEN-002/003/006/008/014 由 M021-001（V428 + 真实库回归）闭合；其余同 B1（通过/登记/无本面）；M021-002（V200 L10 注释）维持登记：授予为 L587-588 明文要求、注释不可改写。
- 对抗式线（grilling）：media 业态商户进月结 → 份额行写入、归 be_media、不进酒吧池（测试实跑）；同词表第四处封口——pg_constraint 全库按词表特征检索只有三处；五值/六值外的值仍被 CHECK 拒；V428 重复执行由 Flyway 版本控制与 DROP IF EXISTS 兜底；V427/V428 并入顺序风险已记台账报操作员（环境事实，非本模块缺陷）；把 V428 拿掉，回归用例即再红。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–002 现态

001 已修（B3-r1），B4-r1 双 PASS；002 只登记；修复队列「是」= 0。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B1 §7 与 B2 登记项（V147 行号漂移、V348/V366 历史注释、出厂无商户角色、LeasingService:285 注释、「其他须指定主管公司」规则在别模块、V427 遗留暂存与部署顺序、主工作区门禁产物既有失步——检查点在 origin/main worktree 内重生成）。

## 7. 结论

席甲 B5-r1 票：**PASS**。5 迁移 + V428 全读、18 判据逐项、只读探针与真实库测试复跑；未改任何文件；落盘前未读 Grok 本次正文。
