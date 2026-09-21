CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；规格文档现行手写段改动为席甲 B3 所写，且席甲 B1 漏过 5 条，以 Grok 票为准）。2026-09-21T01:52:58Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`；B3-r1 after tree `c08263bda933e7769db142af135ab204deae164e`。
- 模块 M028 2 文件（§1 全读），33 判据；B3 累计 1。环境前提同 B1（D-028）。
- 顺序：Grok 已先派（modules/M028-b5-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。
- 本轮补的方法（B1 漏了 5 条）：文中每个「现在时清单/现行事实」去现库与后续迁移对一遍（不只对它所引的那个迁移）；每个交叉引用（节号、ID、路径）实际打开所指对象；对 HTML 的每个转述单元格逐格对原文。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 372 | 42137 | e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9 | 全读 |
| docs/domain-ownership-scope-audit-v0.1.csv | 336 | 60192 | d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m028_b5r1_run.log；B1 同一取证脚本修后重跑，含一次性 worktree 里的生成器 --check）

```
START=2026-09-21T01:52:41Z HEAD=e456ba96 origin/main=b99a9e5d
== 0. files (lines/bytes/sha256) + three states
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 372 42137 e51251665bc8489dc137972245045753225ada6b4d4677c9cfd59b234dc330e9
  vs HEAD:  1 file changed, 19 insertions(+), 18 deletions(-) | vs origin/main:  1 file changed, 18 insertions(+), 17 deletions(-)[empty=same]
docs/domain-ownership-scope-audit-v0.1.csv 336 60192 d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa
  vs HEAD:  1 file changed, 1 insertion(+) | vs origin/main: [empty=same]
== 1. generator consistency, checked in a disposable worktree at origin/main (main worktree carries staged-unpublished V427, so it cannot be in sync by construction)
SPEC_CHECK_EXIT=0
CSV_CHECK_EXIT=0
worktree files identical to main-worktree copies: spec=NO csv=yes
scratch clean
scratch removed
data isolation spec is in sync with Flyway migration scope baseline.
DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
main-worktree check (expected red because of V427): spec=1 csv=1
== 2. gate wiring claims (spec 6A: verify:tenant-isolation in verify:delivery-gates; 3A: docs/verify scripts exist)
script verify:tenant-isolation: 1
script docs:data-isolation-spec: 1
script verify:data-isolation-spec: 1
script docs:domain-ownership-scope: 1
script verify:domain-ownership-scope: 1
script docs:security-baseline: 1
verify:delivery-gates includes tenant-isolation: 1 ; verify:predeploy includes data-isolation-spec: 1 domain-ownership-scope: 1
== 3. spec sec.2 reference implementation still exists?
FinanceService.listArApItems(: 1 listArApItemsByMerchant:  listArApItemsPaged: 2
currentMerchantScope impl (main worktree vs origin/main):
425:    public Optional<String> currentMerchantScope() {
426-        AccessActor actor = currentActor();
427-        String merchantId = actor.merchantId();
428-        if (merchantId != null && !merchantId.isBlank()) {
429-            return Optional.of(merchantId);
430-        }
431-        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
432-    }
433-
origin/main:
422:    public Optional<String> currentMerchantScope() {
423-        AccessActor actor = currentActor();
424-        if (isPlatformAdminBridge(actor)) {
425-            return Optional.empty();
426-        }
427-        String merchantId = actor.merchantId();
428-        if (merchantId != null && !merchantId.isBlank()) {
429-            return Optional.of(merchantId);
430-        }
== 4. spec sec.3 historical table facts vs immutable migrations
V051 width: merchant_id VARCHAR(80) | V049 width: merchant_id VARCHAR(64)  | V046 inventory_* tables: 9 + saved_bottles: 1
28:迁移 V044/V046 的 .sql 注释已确立、本规格正式提升为强制条款：
41:| V046 | inventory 9 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
44:| V049 | amusement 4 表 | merchant_id | VARCHAR(64) | 部分索引已补（V065，ISO-GAP-03 已关闭）|
45:| V051 | `compliance_reminders` | merchant_id | VARCHAR(80) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
53:| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（敏感字段 at-rest 加密已落地，见 §6 ISO-GAP-06） |
69:| `amusement_incidents` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
76:| `amusement_projects` | `merchant_id` + `space_id` | direct | V049, V084 | `V049__amusement_security_fixes.sql`<br>`V084__object_space_mapping.sql` |
80:| `amusement_safety_notices` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
== 5. line-number references inside the spec
csv line 131: facility_inspection_templates,direct,project_id | supervisor_review_decisions actually at line: 281
matrix 272-273:
| ticketing | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | `approveTicketRefund` | 退款审批 | `StatusChangeRequest` | `ticket.refund.approve` | `staff-token` | `project-order` | web | 是 | `ticket_refund.approved` | `ticket_refund.approve_failed` |
| ticketing | `GET` | `/api/v1/ticketing/qr-refresh-config` | `getTicketQrRefreshConfig` | 读全场统一动态码刷新间隔 | `-` | `ticket.product.view` | `staff-token` | `platform` | web | 否 | `ticket.qr_refresh_config.viewed` | `ticket.qr_refresh_config.view_failed` |
797:| 终端越权 | 员工端、平板、一体机、客人端调用不属于自己的接口 | 按 `x-terminal-allowed`、设备能力、登录主体和数据范围同时校验 |
804:| 附件归档 | 合同、审批、事故、整改附件缺失或 hash 不一致 | 附件上传、下载、病毒扫描、hash 校验、权限隔离和归档留痕必须可测 |
== 6. spec sec.9 V140 lists vs migration
V140__append_only_enforcement_and_runtime_role.sql
Traceback (most recent call last):
  File "<stdin>", line 14, in <module>
AttributeError: 'NoneType' object has no attribute 'group'
triggers in complex_test calling complex_block_mutation: 41
complex_app role referenced in V140: 15 ; application-prod.yml: 2
== 7. spec sec.10 claims: V178 / V180 / account type / roles / ADM-VND-004 / completion service
V178__outsourced_vendor_facility_assignee_permissions.sql V180__auditor_roles_readonly_seed.sql 
outsourced_vendor: migrations=2 java=3
OUTSOURCED_FACILITY: migrations=2 java=0
OUTSOURCED_SECURITY_CLEANING: migrations=2 java=0
OUTSOURCED_CONSTRUCTION: migrations=2 java=0
PartnerSubordinateCompletionService: 1 ; subordinates-completion endpoint: 0 ; ADM-VND-004 route: 1
== 8. spec sec.6 gap table facts
saved_bottles.customer_phone now encrypted? column checks: CHECK (((customer_phone IS NULL) OR ((customer_phone)::text = ''::text) OR ((customer_phone)::text ~~ 'enc:v1:%'::text))) CHECK (((transfer_to_phone IS NULL) OR ((transfer_to_phone)::text = ''::text) OR ((transfer_to_phone)::text ~~ 'enc:v1:%'::text))) 
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:10
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:12
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:13
security-scans files: security-scans/findings-register-v1.md security-scans/formal-security-baseline-v1.md 
SEC-P0-TENANT-ISOLATION-001 in register: 1
superpowers plan file: docs/superpowers/plans/2026-06-15-security-audit-fixes.md
== 9. CSV shape
rows 335 cols ['table_name', 'scope_type', 'scope_source', 'scope_path', 'multi_complex_note', 'file_version', 'status']
scope_type {'direct': 173, 'inherited': 79, 'global_reference': 7, 'single_complex_default': 76}
status {'covered': 335}
file_version {'V0.01': 335}
sorted True dups []
tables in complex_test not in CSV: business_entity_projects
CSV tables not in complex_test: 
== 10. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T01:52:55Z
```

说明：探针 3 的 bc 报错行已滤去（同 B1 说明）；探针 5 的两处行号引用在修后文档里已不存在，该段输出的「csv line 131 / matrix 272-273」只作对照。

## 2b. 十条定稿问题的只读检查（/home/james/audit-tmp/m028_b5r1_check.log）

```
title: # 多租户 / 商户数据隔离与越权防护 安全设计与验收规格 V0.03
auto block sha256: 81b83030b0bf22cbbd91d8c2e353e0d47d9f5f834b043792cf343d148265b335
problems=0
CHECK_EXIT=0
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。自动区块与 CSV 仍由生成器守住（一次性 worktree 里两个 --check 通过）；手写段的 10 条不可复现/自相矛盾/与 HTML 冲突的声称已消除。
- 顺从式线：33 判据逐项——B1 不通过的 GEN-002/005/008/010/014/016 与 PGM-059（文档侧）由 001–010 闭合；BSM-117、PGM-057、GEN-001/003（登记）/004/006/011/013/017/018 同 B1；其余 BEQR/BSM/PGM 与 GEN-007/009/012/015 无本面（理由同 B1 §4）。
- 对抗式线（grilling）：按 §2 去 `FinanceService.listArApItems()` 找参考实现→对得上；按 §4 去验收矩阵第 5 节找「数据范围穿透」行→在；按 §7 去 CSV 找 `supervisor_review_decisions` 行→在且标注一致；按 §6A.1 给业务表写读谓词→参数级、旧行 NULL 不可见，与 AC-ISO-4 一致；给配置默认槽表写→列级；§6 缺口表逐条：01/02/03 已关闭（脚本与 V065 存在）、04 仍真（无 V044–V061 统一回归 plan）、05 仍真（未核 dev-design，登记）、06 已关闭且承接属实；§9 清单标明 V140 初版且注明 cam 两表已删，读者不会再按它当现行闭集验收；§10 审计人员单元格与治理地图原文一致；§10 其余单元格（外包/检修、商户外包员工）与治理地图对应条目无缩小；文中其余交叉引用（`p0-operation-permission-matrix-v0.1.md`、`platform-governance-product-spec-v0.2.md`、`full-admin-page-permission-field-matrix`、security-scans 两文件）逐个打开存在。无新缺陷、无添加项、无 HTML 问题。

## 4. 已定稿 001–010 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。GR-06 与其余观察只登记。

## 5. 本次 Findings

无新增。

## 6. 观察（登记）

- 同 B2-r1 定稿的登记项（生成器分类差异归 M029；路径两种基准并存；§9/§10 简写名；两个生成器口径不同；CSV 恒值列；§5 回填与 V046 VALIDATE 待执行；`platform-governance-prd-v0.1.md` §4.1 标题过期；主工作区未发布改动联动项；D-028）。

## 7. 结论

席甲 B5-r1 票：**PASS**。2 文件全读、33 判据逐项、取证脚本与检查复核；未改任何文件；落盘前未读 Grok 本次正文。


---
## 席甲说明（V0.02，2026-09-21T01:53:21Z；不新增独占锁行，本段属上方 V0.01 报告的追加；落盘时未读 Grok 本次正文，Grok 仍在运行）

- §2 证据块里「worktree files identical to main-worktree copies: spec=NO」是预期结果：该探针的一次性 worktree 取自 origin/main，里面是未修的 V0.02 规格，而主工作区已是 B3 修后的 V0.03，二者自然不同；该处两个 --check 证明的是 origin/main 上的自动区块与 CSV 同步。带 **修后文件** 跑两个生成器 --check 与条目锚点门禁的证据在 B3 执行日志 /home/james/audit-tmp/m028_b3r1_run.log（SPEC_CHECK_EXIT=0、CSV_CHECK_EXIT=0、ANCHOR_GATE_EXIT=0，已全文收入 modules/M028-b4-r1-self-raw.md §4）；修后文件的自动区块 SHA256 与修前一致（§2b）。结论不变。
