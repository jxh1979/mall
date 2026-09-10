CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U04 未发布材料第 4 段（员工网页和组织网页）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T20:12:48Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `10a3e4aec4de0d7a78580c26ecba6d49c97678ea`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 4 段，1 文件 1301 行（apps/web/src/admin/types.ts），main 主归属 M365；适用判据＝M365 映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单 `unpublished-03acdb98-input.json` 核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。类型检查在 `/tmp/u-overlay`（发布基线 242f4872 `git archive` + 76 个未发布文件覆盖，76/76 哈希核对；node_modules 符号链接只读复用）内进行，不触主仓。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U04-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环（M001–M010 已合并 PR #428、M011/M012 已合并 PR #429，HEAD=origin/main=445a49f4）；U01–U03 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U04-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：未发布副本内 `OrganizationMerchantPage.tsx`（第 3 段，selfOperated 消费方）、`backend/.../governance/MerchantRecord.java`、`JdbcPlatformRepository.java`、相关迁移（第 8/10 段）；main 当前同名文件。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/types.ts | 1301 | 34127 | 0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c | 全读 |

## 2. 可执行证据原文（/tmp/u04_b1_run.log）

```
START=2026-09-09T19:55:27Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
apps/web/src/admin/types.ts | 1301 | 34127 | 0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c
apps/web/src/admin/types.ts 1301 34127 0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c
=== baseline 242f4872 -> unpublished tree e28dad05 (full diff, this file) ===
diff --git a/apps/web/src/admin/types.ts b/apps/web/src/admin/types.ts
index 8d26e821..5bcc6876 100644
--- a/apps/web/src/admin/types.ts
+++ b/apps/web/src/admin/types.ts
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 export type AdminUser = {
   id: string;
   username: string;
@@ -1117,6 +1118,7 @@ export type GovernanceSpaceRow = {
 
 export type MerchantAdmissionRow = {
   id: string;
+  selfOperated?: boolean;
   merchantName?: string;
   merchantType?: string;
   businessEntityId?: string;
=== main vs baseline vs unpublished (blob ids) ===
baseline=8d26e82118d6f9e607fe85e18ec638a81adc5dd4 head=8d26e82118d6f9e607fe85e18ec638a81adc5dd4 worktree=8d26e82118d6f9e607fe85e18ec638a81adc5dd4 unpublished=5bcc6876879e1f3fda9c0a9b35033732696f1281 main_unchanged_since_baseline=yes worktree_dirty=no
main_sha=0d41af630f71ad250f699a83d546e7aecc190d6e3ec4a21fa478acaf3372c71b unpublished_sha=0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c diff_lines=2
=== consumers of selfOperated in unpublished web copy / main web ===
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/types.ts:1121:  selfOperated?: boolean;
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:268:        { label: "自营商户", value: row.selfOperated ? "是（员工本人注册，�
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:320:    if (!row.archivedAt && row.deleteStatus !== "pending") actions.push({ key: "self-ope
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:325:          await api.post("/api/v1/platform/merchants/" + encodeURIComponent(row.id) + "/
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/pages/OrganizationMerchantPage.tsx:326:          setActionResult({ 结果: row.selfOperated ? "已取消自营标记" : "已标
=== backend field (unpublished copy) ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:36:        boolean selfOperated
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:42:    public MerchantRecord withSelfOperated(boolean selfOperated) {
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:43:        return new MerchantRecord(id, merchantName, merchantType, bus
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:66:                archivedAt, archivedBy, deleteStatus, deleteRequested
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:79:                archivedAt, archivedBy, deleteStatus, deleteRequested
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java:88:                archivedAt, archivedBy, deleteStatus, deleteRequested
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:90:                    archived_at, archived_by, delete_status, 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:97:                    :archived_at, :archived_by, :delete_statu
V022__p1_merchant_service_work_orders.sql
V026__p1_merchant_billing_portal.sql
V042__merchant_profile_extended_fields.sql
V047__bar_security_merchant_isolation.sql
V052__floorplan_merchant_isolation.sql
V065__amusement_merchant_isolation_indexes.sql
V075__entitlement_merchant_and_ticket_validity.sql
V120__merchant_service_work_order_sla_escalation.sql
V134__entitlement_sessions_merchant_id_nullable.sql
V135__ticket_orders_merchant_id.sql
V139__inventory_trace_handover_merchant_id.sql
V147__merchants_owner_person.sql
V175__stored_value_merchant_scoped_cards.sql
V176__stored_value_merchant_refund_rules.sql
V182__merchant_fee_notification_recipient.sql
V199__merchant_portal_zone_module_switch.sql
V200__merchant_portal_zone_permissions.sql
V208__merchant_summaries_real_pipeline.sql
V220__ticket_refunds_merchant_id.sql
V221__merchant_revenue_attribution_rules.sql
V228__entitlement_exclusive_merchant_batch3.sql
V259__merchant_archive_delete.sql
V267__bar_is_merchant_drop_bar_venue.sql
V289__stored_value_merchant_card_fund_mode.sql
V297__merchant_expense_ledger.sql
V334__integration_order_sync_merchant_attribution.sql
V348__merchant_business_format.sql
V374__ticket_distribution_merchant_rate_rule_type.sql
V384__merchant_business_format_media.sql
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/resources/db/migration/V408__staff_registration_company_routes.sql
=== pre-existing 臆造字段 remarks (main M365 scope) ===
1074:// 旧的臆造字段（name/code/orgType、displayName/companyName 等）后端不存在，保留 optional 供他页过渡，勿再新增读取。
1110:  // 旧臆造字段（保留 optional 防他页引用报错）
=== tsc in /tmp/u-overlay apps/web (baseline + 76 unpublished files) ===
START=2026-09-09T18:14:04Z
TSC_EXIT=0
END=2026-09-09T18:14:09Z
END=2026-09-09T19:55:27Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 4 段（1 文件 `apps/web/src/admin/types.ts` 1301 行），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- apps/web/src/admin/types.ts`（§2 全文 diff：+1 版本行、+1 字段 `selfOperated?: boolean`），main 当前版本仅作分叉对照。
- 第一性原理线：HTML PGM L578/BSM L1433「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」→ 商户档案行需要承载「自营」标注；类型层只需一个可选布尔字段，与后端 `MerchantRecord.selfOperated`（副本第 8 段）同名同义。
- 顺从式线：21 项判据逐项（§5）。
- 对抗式线：字段名与后端 JSON（snake `self_operated` → adminClient camel）是否一致；是否有消费方（第 3 段页面）；发布是否覆盖 main 已修项（M009/M012 未改 types.ts，§2 blob 对照）；整文件既有「臆造字段」注释是否被本材料加重；隔离 tsc 是否绿。
- 只读依赖追流（不审）：副本 `OrganizationMerchantPage.tsx`（第 3 段消费方）、`backend/.../governance/MerchantRecord.java`、`JdbcPlatformRepository.java`、相关迁移（第 8/10 段）；main 当前 `types.ts`（与基线同 blob）。
- 1 文件 1301 行全文逐行读完（§1）；diff 逐块读完（§2）。可执行验证：`/tmp/u-overlay`（基线 + 76 未发布文件，不触主仓）内 `apps/web` `npx tsc --noEmit` 退出码见 §2。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- main `types.ts` 自基线以来未改（§2 blob baseline=head=worktree），M009–M012 均未触及；未发布版＝基线 + 2 行 → 快进合并、无冲突。

## 5. 21 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | `selfOperated` 有 HTML 上位（L578/L1433 标注为自营） |
| GEN-002 可执行正确性 | 通过（本段） | 类型层无逻辑；隔离 tsc 退出 0（§2）；功能失效点在第 3 段请求体（U03-001），不在类型 |
| GEN-003 测试有效性 | 不适用 | 类型定义无测试面；消费方用例属第 3 段 |
| GEN-004 信任边界 | 不适用 | 无输入处理 |
| GEN-005 授权 | 不适用 | 无授权面 |
| GEN-006 数据完整性 | 通过 | 可选布尔，缺省不改既有语义 |
| GEN-007 并发幂等 | 不适用 | 无写路径 |
| GEN-008 接口一致性 | 通过 | `selfOperated` ↔ 后端 `self_operated`（副本 `MerchantRecord.selfOperated`，§2 探针）经 adminClient 驼峰化一致 |
| GEN-009 错误处理 | 不适用 | 无逻辑 |
| GEN-010 安全 | 通过 | 无敏感字段新增 |
| GEN-011 审计证据 | 不适用 | 无实现面 |
| GEN-012 资源 | 不适用 | 无实现面 |
| GEN-013 构建 | 通过 | 隔离 tsc 退出 0（§2） |
| GEN-014 迁移 | 不适用 | 无迁移 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过（登记） | 版本行新增；既有「旧臆造字段保留 optional」注释为 main 既有（OBS-1） |
| GEN-017 分类 | 通过 | 本段无业务分类争议 |
| GEN-018 证据 | 通过 | 结论落到 diff、blob 对照、tsc |
| BSM-001 整体说明 | 通过 | 商户档案「谁在经营」信息补齐自营标注 |
| BSM-003 通用能力 | 通过 | 无冲突 |
| PGM-001 整体定位 | 不适用 | 无用户可见文字 |

## 6. 本次 Findings

无新的工程缺陷。本段唯一新增字段与 HTML 及后端一致；功能层缺陷（自营标记请求体缺幂等键）已在第 3 段登记为 U03-001，不在本段重复登记。

## 7. 观察（登记、不入队）

- OBS-1：整文件保留多组 main 既有「旧臆造字段」（`BusinessEntityRow` L1084–1086、`GovernanceSpaceRow` L1111–1116、`MerchantAdmissionRow` L1141–1144、`SubjectBindingRow` L1153–1154），与 M012 观察 OBS-B5-02 同根，归 M365；本材料未加重。
- OBS-2：`AttendanceRow`/`ShiftRow` 等排班字段在页面内定义（第 3 段）而非 `types.ts`，类型集中度不一，登记不入队。
- 技能：`using-superpowers`、`verification-before-completion`（tsc 实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**PASS**。1 文件全文三线审计、21 项判据逐项核对；无新增工程缺陷、无添加项、无 HTML 修改申请；隔离 tsc 绿；未修源、未提交/推送。本报告落盘前未读取 `U04-b1-grok-raw.md` 任何正文。
---

# U04 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.50「当前 U04 B2-r1 整批候选」U04-001…003；modules/U04-b1-grok-raw.md V0.01（36941bytes SHAaed41cddef4d1116672f535e77abfd9d74399426fd67c9139776fa5a970eb2b5）；questions/U04-001.md V0.01（操作员答复首行「删除」）；本文件 B1 段（13393bytes SHA72baf5349285aef599d65307b71e7639f523a154f8fce79ddb6a7525b5eadf48）。2026-09-09T20:40:45Z
- 席甲 B2 核实（只读，见 ledger V2.53 U04 节）：三份 HTML `grep 自营` 共 7 处（BSM L701/L1016/L1433/L1717、PGM L495/L498/L578），无一句允许自营标注独立于合作方式或规定同步；Grok 类型探针（lease+true、self_operated+false 均 exit 0）事实成立；main `types.ts` blob＝基线，删除该字段后本段相对 main 无净改动。

## 勘误（席甲 B1 段）

- 本文件 §5 GEN-001/GEN-017 与 §8 票 PASS 有误：席甲只对照 BSM L1433「标注为自营」把 `selfOperated` 判为细化，未按 GEN-017 判定顺序对照 BSM L701/PGM L495/L498 六类合作方式已含「自营」这一轴；该字段可与合作方式分叉，是 DTO 层业务开关，属添加。B2 接受 Grok 分类并已按 2.2 弹窗，操作员答「删除」。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 添加分类经 HTML 逐字与类型探针核实；操作员已答「删除」；处置＝删字段、自营标注＝合作方式 `self_operated`、U05 分流改读合作方式——「派生」是 HTML 六类既有口径的读法，不是新规则；跨段项只登记处置、不改 U03 定稿文本 |
| 002 | PASS | 四件分处四段、TEST-B 4× TS2339 与 main 无路由均有实跑证据；随 001 删除消失；保留分支只作发布者违背 001 时的兜底约束，不发明业务 |
| 003 | PASS | 观察归并未改写双方原意；OBS-06/07/11/15 随 001 失效、OBS-12 HEAD 勘误已改提示词模板、OBS-13 以树为准均如实登记 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 001 处置「自营标注＝合作方式『自营』／派生」只表示删除第二轴后的读路径落在既有六类码 `self_operated`，不是新规则；禁止另写同步函数或第七类。
2. 定稿 U04 后在 findings 的 U03 定稿节末尾追加一行指向 questions/U04-001.md（不改 U03-001…015 单元格），避免只读 U03 的发布者仍去迁自营按钮。
3. 003 席甲 OBS-1 中 `BusinessEntityRow` 臆造字段本体是 L1084–1086（`name/code/orgType`），L1071–1073 是同块注释；同条不另开 ID。
4. 「不再弹窗」只覆盖本自营布尔发明；U05/U08/U10 其它添加项仍走 2.2。
5. 002 的「四件」＝类型/页面/路由/列（U04/U03/U08/U10），不含 U05；U05 读路径属 001。

## 结论

席甲 B2-r1 票：**PASS**（候选 U04-001…003 成立；发布前须修「是」2 / 「否」1；添加 1 已答「删除」；questions 1 已答复）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 53643bytes SHA496a683a1857b0f23dffafe29bc8797234616658b3521cca583711bd1f79d8a4，前 36941bytes 原字节不变）。
