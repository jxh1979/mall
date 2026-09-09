CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M009 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M009-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲票仍是作者自审票；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M009-b4-r1-self-raw.md`（当前亦不存在）、席甲本次 B4 raw、其它模块旧票正文。定稿队列以 findings.md V1.19 顶部「当前 M009 B2-r2 双PASS定稿，15项待修」为准，并与本席 B2-r2（`M009-b1-grok-raw.md` V0.04「最小修复边界」）对照。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff、15 个定稿「是」ID 是否按最小边界修好、每一行能否归因到这 15 个 ID 之一、是否越界改 HTML/锁/sidecar/ledger 基准/权限种子/迁移/013/017–025/其它模块业务逻辑。未新造判据/角色，未询问操作员。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 顶行同值。criteria-lock 版本仍 V0.11。`LOCK-AMEND-COUNT: 1`。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段 |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 正式队列「是」（15） | M009-001、002、003、004、005、006（仅存在性）、007、008（写侧投影）、009、010、011、012（仅删 M009 面 parentEntityId）、014、015、016 |
| 正式队列「否」（10） | 013、017、018、019、020、021、022、023、024、025 |
| B3 | 已执行 1 次，模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `c5e66bd5e156c01319a3ceece584906716659999` |
| after（对象类型 tree，可解析） | `80b11c54422e5f54873e1fc8bd4232322babf844` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary c5e66bd5e156c01319a3ceece584906716659999 80b11c54422e5f54873e1fc8bd4232322babf844`（无路径过滤，不只看 HEAD 已提交） |
| 实际差异 | 89605 bytes；SHA256 `6fd3527a9fe7e74696924d5ae59628291ec30efbfb03dd4a97a89a4eed83470e`；UTF-8；16 文件；527 插入 / 124 删除；96 hunk；name-status 全为 M |
| 与 `/tmp/m009_b3_r1.diff` | `cmp` 全等（本席自算为准，副本仅对照） |
| `git diff --check` | exit 0 |
| 两树路径清单 | 各 7233 条，差集空；无新增/删除路径 |
| HEAD | `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` |
| 工作区 16 文件 blob | 均等于 after；HEAD 16 文件 blob 均等于 before（修复尚未提交） |
| findings.md | V1.19；SHA256 `490010c5b5c36f41d32f726957408da2907542b3b8e388a56a80641623564810` |
| ledger.md | V2.05；顶部锁同行；`LOCK-AMEND-COUNT: 1` |
| 三 HTML 两树 blob | 完全相同，diff 未改 HTML |
| 锁/sidecar 两树 blob | 相同（未改） |
| 迁移 / 权限种子 | 两树 diff 无名 |

`git diff --stat --binary`：

```
 .../admin/pages/OrganizationMerchantPage.test.tsx  |   6 +-
 .../src/admin/pages/OrganizationMerchantPage.tsx   |  24 +-
 .../platform/governance/BusinessEntityRecord.java  |   4 +-
 .../governance/JdbcPlatformRepository.java         |  43 ++--
 .../governance/MerchantDeleteDecision.java         |   3 +-
 .../platform/governance/MerchantRecord.java        |  11 +
 .../platform/governance/PlatformController.java    |   9 +
 .../platform/governance/PlatformRepository.java    |  16 +-
 .../platform/governance/PlatformService.java       | 235 +++++++++++++++-----
 .../governance/dto/BusinessEntityRequest.java      |   2 +-
 .../governance/dto/MerchantArchiveRequest.java     |   3 +-
 .../complex/platform/leasing/LeasingService.java   |   4 +-
 .../governance/PlatformControllerTests.java        |  15 ++
 .../governance/PlatformJdbcIntegrationTests.java   |  28 ++-
 .../platform/governance/PlatformServiceTests.java  | 243 +++++++++++++++++++--
 .../LeasingSignAtomicityJdbcIntegrationTests.java  |   5 +
 16 files changed, 527 insertions(+), 124 deletions(-)
```

工作区相对 HEAD 另有既有父链脏文件（审计产物、工具、一份业务 HTML 等）。那些路径在 before/after 两树 blob 相同，不在本轮 diff 内，不按本轮修复内容审计。本席未改任何源文件，只新建本 raw。

---

## 2. 锁、分类与正式队列（既有，不新造）

适用冻结判据仍是 criteria-lock V0.11 已映射到 M009 的集合，本轮只用定稿 15 项已引用的 ID：BSM-004/007/028/072/074，BEQR-143/144/148/154/155，PGM-001/005/018/019/026/027/041，GEN-001/002/003/004/005/006/008/009/016。不 LOCK-AMEND。

B3 边界（与本席 V0.04「最小修复边界」一致）：只修上述 15 个 ID 及可归因最小行为测试；003 含 LeasingService `"leasing_auto"`→null 与 OrganizationMerchantPage 六类中文 option 及其测试、M011 夹具随 003/005 改；006 存在性落地后 LeasingSignAtomicityJdbcIntegrationTests 夹具插入/清理是必要伴生；不加迁移、不改 M015 ProjectRecord、不改权限种子、不改 013/017–025。

---

## 3. 完整未过滤 binary diff（全文，非链接、非摘要）

命令：`git diff --binary c5e66bd5e156c01319a3ceece584906716659999 80b11c54422e5f54873e1fc8bd4232322babf844`

before tree = `c5e66bd5e156c01319a3ceece584906716659999`
after tree = `80b11c54422e5f54873e1fc8bd4232322babf844`

字节 89605；SHA256 `6fd3527a9fe7e74696924d5ae59628291ec30efbfb03dd4a97a89a4eed83470e`；UTF-8 完整原文如下。

````diff
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
index 98ef3f22..5592efd6 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import { fireEvent, render, screen, waitFor } from "@testing-library/react";
 import { afterEach, describe, expect, it, vi } from "vitest";
 
@@ -164,7 +165,10 @@ describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", ()
     expect(typeText).not.toContain("catering");
     // 单屏模式：经营与租赁（合作形式下拉）与基本信息同屏，无需步进。
     const coopText = screen.getByTestId("organization-merchant-cooperation").textContent ?? "";
-    expect(coopText).toContain("固定租金");
+    // M009-003：合作形式下拉只提供 HTML 六类（自营/租赁/联营/代运营/服务商接入/其他），不再是收费模板码。
+    expect(coopText).toContain("租赁");
+    expect(coopText).toContain("服务商接入");
+    expect(coopText).not.toContain("固定租金");
     expect(coopText).not.toContain("fixed_rent");
     expect(coopText).not.toContain("revenue_share");
   });
diff --git a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
index 43271407..64b9cc0c 100644
--- a/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
+++ b/apps/web/src/admin/pages/OrganizationMerchantPage.tsx
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 import React from "react";
 import { TechDetails } from "../components/TechDetails";
 import { AdminDataState } from "../components/AdminDataState";
@@ -29,7 +30,18 @@ const TEXT = {
 // 业态是通票分账按业态拆份额与分流结算的依据。娱乐平台公司对应的即酒吧业态，不另设「娱乐」业态；
 // 传媒＝演出提供方（HTML「演出票 → 传媒公司」）。
 const MERCHANT_TYPES = ["bar", "catering", "retail", "amusement", "media", "other"];
-const COOPERATION_FORMS = ["fixed_rent_plus_commission", "revenue_share", "fixed_rent"];
+// M009-003：合作方式六类（BEQR-148/BSM-028/PGM-026：自营/租赁/联营/代运营/服务商接入/其他）；代码与后端 PlatformService.MERCHANT_COOPERATION_FORMS 同词表。
+const COOPERATION_FORMS: ReadonlyArray<{ code: string; label: string }> = [
+  { code: "self_operated", label: "自营" },
+  { code: "lease", label: "租赁" },
+  { code: "joint_operation", label: "联营" },
+  { code: "managed_operation", label: "代运营" },
+  { code: "service_provider", label: "服务商接入" },
+  { code: "other", label: "其他" },
+];
+function cooperationLabel(code: string | null | undefined) {
+  return COOPERATION_FORMS.find((form) => form.code === code)?.label ?? formatBusinessStatus(code);
+}
 
 function display(value: unknown) {
   if (value === null || value === undefined || value === "") return "-";
@@ -69,7 +81,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
   const [merchantType, setMerchantType] = React.useState("");
   const [contactName, setContactName] = React.useState("运营联系人");
   const [contactPhone, setContactPhone] = React.useState("13800000000");
-  const [cooperationForm, setCooperationForm] = React.useState("fixed_rent_plus_commission");
+  const [cooperationForm, setCooperationForm] = React.useState("lease");
   const [formError, setFormError] = React.useState("");
   // 引导式创建流（一期范式）：主按钮 → 分步弹窗 → 确认创建。
   const [createOpen, setCreateOpen] = React.useState(false);
@@ -263,7 +275,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         { label: "联系电话", value: display(row.contactPhone) },
         { label: "区块", value: display(row.mallBlockCode) },
         { label: "面积", value: display(row.leasedAreaSquareMeter ?? row.areaSquareMeter) },
-        { label: "合作形式", value: formatBusinessStatus(row.cooperationForm) },
+        { label: "合作形式", value: cooperationLabel(row.cooperationForm) },
         { label: "状态", value: formatBusinessStatus(row.status) },
         auditTraceField(navigateTo, "merchant", row.id),
       ],
@@ -336,7 +348,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
         <table className="admin-table" data-testid="organization-merchants-table">
           {/* 后端无“公司名/风险/留痕证据”字段（原按臆造字段读致整表“-”），列对齐真实字段：联系人/联系电话替换。 */}
           <thead><tr><th>商户</th><th>业态</th><th>联系人</th><th>联系电话</th><th>区块</th><th>面积</th><th>合作形式</th><th>状态</th><th>存档状态</th><th>操作</th><th>详情</th></tr></thead>
-          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{formatBusinessStatus(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
+          <tbody>{visibleMerchants.map((row) => <tr key={row.id} data-testid={`organization-merchant-row-${row.id}`} className={`admin-row-clickable${row.id === focusObjectId ? " admin-row-selected" : ""}`} tabIndex={0} onClick={() => openMerchantDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMerchantDetail(row); } }}><td>{display(row.merchantName)}</td><td>{row.merchantType ? formatBusinessStatus(row.merchantType) : "-"}</td><td>{display(row.contactName)}</td><td>{display(row.contactPhone)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.leasedAreaSquareMeter ?? row.areaSquareMeter)}</td><td>{cooperationLabel(row.cooperationForm)}</td><td>{formatBusinessStatus(row.status)}</td><td>{merchantArchiveLabel(row)}</td><td onClick={(event) => event.stopPropagation()}>{merchantActions(row)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看商户档案详情" onClick={(event) => { event.stopPropagation(); openMerchantDetail(row); }}>查看详情</button></td></tr>)}</tbody>
         </table>
       </div>
     </>
@@ -450,7 +462,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
             title: "经营与租赁",
             description: "选择与商户约定的合作形式。",
             content: (
-              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form} value={form}>{formatBusinessStatus(form)}</option>)}</select></label>
+              <label>合作形式<select data-testid="organization-merchant-cooperation" value={cooperationForm} onChange={(event) => setCooperationForm(event.target.value)}>{COOPERATION_FORMS.map((form) => <option key={form.code} value={form.code}>{form.label}</option>)}</select></label>
             ),
           },
         ]}
@@ -459,7 +471,7 @@ export function OrganizationMerchantPage({ api, setActionResult, setGlobalError,
           { label: "业态", value: merchantType ? formatBusinessStatus(merchantType) : "未选择" },
           { label: "联系人", value: contactName },
           { label: "联系电话", value: contactPhone },
-          { label: "合作形式", value: formatBusinessStatus(cooperationForm) },
+          { label: "合作形式", value: cooperationLabel(cooperationForm) },
         ]}
       />
       <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
diff --git a/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java b/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
index 6a856656..41ef294b 100644
--- a/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/BusinessEntityRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 
@@ -6,9 +7,6 @@ public record BusinessEntityRecord(
         String id,
         String entityName,
         String entityRole,
-        // 四层组织(terminology-canonical §一):L3 管理公司/业态主体 的 parent = L2 项目公司;L2 的 parent = L1 平台。
-        // null = 顶层(平台)或未挂接。用于「一个项目公司下有多个管理公司主体」的层级落库。
-        String parentEntityId,
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index 51f12a0e..6536bda2 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -25,8 +26,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     private static final TypeReference<List<Map<String, Object>>> POLYGON_POINTS_TYPE = new TypeReference<>() {
     };
-    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
-    };
     private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
     };
 
@@ -46,17 +45,16 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     public BusinessEntityRecord saveBusinessEntity(BusinessEntityRecord businessEntity) {
         jdbcTemplate.update("""
                 INSERT INTO business_entities (
-                    id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                    id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                     bank_account_ref, status, created_at, updated_at
                 )
                 VALUES (
-                    :id, :entity_name, :entity_role, :parent_entity_id, :unified_social_credit_code, :legal_representative,
+                    :id, :entity_name, :entity_role, :unified_social_credit_code, :legal_representative,
                     :bank_account_ref, :status, :created_at, :updated_at
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     entity_name = EXCLUDED.entity_name,
                     entity_role = EXCLUDED.entity_role,
-                    parent_entity_id = EXCLUDED.parent_entity_id,
                     unified_social_credit_code = EXCLUDED.unified_social_credit_code,
                     legal_representative = EXCLUDED.legal_representative,
                     bank_account_ref = EXCLUDED.bank_account_ref,
@@ -69,7 +67,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     @Override
     public List<BusinessEntityRecord> listBusinessEntities() {
         return jdbcTemplate.query("""
-                SELECT id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                        bank_account_ref, status, created_at, updated_at
                   FROM business_entities
                  ORDER BY created_at DESC, id
@@ -138,7 +136,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
     @Override
     public List<MerchantRecord> listMerchants(boolean includeArchived) {
-        // V260：默认列表隐藏已存档 + 已审批删除；includeArchived=true 时全量（后台「显示已存档」开关）。
+        // V260：默认列表隐藏已存档 + 已审批删除；M009-010：includeArchived=true 时全量（含已审批删除行，可查可恢复）。
         return jdbcTemplate.query("""
                 SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
@@ -146,13 +144,26 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
                        archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
                   FROM merchants
-                 WHERE (:include_archived OR archived_at IS NULL)
-                   AND delete_status <> 'approved'
+                 WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))
                  ORDER BY created_at DESC, id
                  LIMIT 500
                 """, Map.of("include_archived", includeArchived), this::mapMerchant);
     }
 
+    @Override
+    public boolean businessEntityExists(String businessEntityId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM business_entities WHERE id = :id",
+                Map.of("id", businessEntityId), Long.class);
+        return count != null && count > 0;
+    }
+
+    @Override
+    public boolean projectExists(String projectId) {
+        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = :id",
+                Map.of("id", projectId), Long.class);
+        return count != null && count > 0;
+    }
+
     @Override
     public java.util.Optional<MerchantRecord> findMerchantById(String merchantId) {
         return jdbcTemplate.query("""
@@ -365,7 +376,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities", params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
         List<BusinessEntityRecord> items = jdbcTemplate.query("""
-                SELECT id, entity_name, entity_role, parent_entity_id, unified_social_credit_code, legal_representative,
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
                        bank_account_ref, status, created_at, updated_at
                   FROM business_entities
                  ORDER BY created_at DESC, id
@@ -382,7 +393,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         MapSqlParameterSource params = new MapSqlParameterSource().addValue("include_archived", includeArchived)
                 .addValue("business_format", businessFormat);
         // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
-        String where = " WHERE (:include_archived OR archived_at IS NULL) AND delete_status <> 'approved'"
+        String where = " WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
                 + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
@@ -491,7 +502,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("id", businessEntity.id())
                 .addValue("entity_name", businessEntity.entityName())
                 .addValue("entity_role", businessEntity.entityRole())
-                .addValue("parent_entity_id", businessEntity.parentEntityId())
                 .addValue("unified_social_credit_code", businessEntity.unifiedSocialCreditCode())
                 .addValue("legal_representative", businessEntity.legalRepresentative())
                 .addValue("bank_account_ref", businessEntity.bankAccountRef())
@@ -580,7 +590,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 resultSet.getString("id"),
                 resultSet.getString("entity_name"),
                 resultSet.getString("entity_role"),
-                resultSet.getString("parent_entity_id"),
                 resultSet.getString("unified_social_credit_code"),
                 resultSet.getString("legal_representative"),
                 resultSet.getString("bank_account_ref"),
@@ -1076,14 +1085,6 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         }
     }
 
-    private Map<String, Object> readObjectMap(String value) {
-        try {
-            return value == null ? Map.of() : objectMapper.readValue(value, OBJECT_MAP_TYPE);
-        } catch (JsonProcessingException exception) {
-            throw new IllegalStateException("contract JSON object is not readable", exception);
-        }
-    }
-
     private List<String> readStringList(String value) {
         try {
             return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java b/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
index 1e0cb4de..82d78234 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantDeleteDecision.java
@@ -1,7 +1,8 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 /**
- * 商户删除审批结果。approved 时商户已物理删除（merchantId 仅供留痕），deleted=true；
+ * 商户删除审批结果。approved 时商户已按软删除标记（delete_status=approved，行保留、默认列表不显示、可查可恢复，BSM-004），deleted=true；
  * rejected 时商户回到 none 态、未删除。
  */
 public record MerchantDeleteDecision(String merchantId, String decision, boolean deleted) {
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
index afbf6fb5..595fea23 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.math.BigDecimal;
 import java.time.Instant;
@@ -68,6 +69,16 @@ public record MerchantRecord(
                 archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
     }
 
+    /** M009-008：三个主体列以生效的四类主体绑定为准（PGM-026），绑定激活时由 PlatformService 同步投影。 */
+    public MerchantRecord withEntities(String businessEntityId, String contractEntityId, String settlementEntityId,
+            Instant updatedAt) {
+        return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
+                settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
+                leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
+                status, createdAt, updatedAt,
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+    }
+
     public MerchantRecord withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
             String deleteReason, Instant updatedAt) {
         return new MerchantRecord(id, merchantName, merchantType, businessEntityId, contractEntityId,
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformController.java b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
index 93f260fc..567dc33c 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformController.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -41,6 +42,8 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("entity.binding.view");
+        // M009-015：治理读端与写端同法 fail-closed（BSM-072/BSM-074/PGM-027：商户不能看平台全局和其他商户数据）。
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listBusinessEntities(page, size));
     }
 
@@ -59,6 +62,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("merchant.profile.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(maskPhonesForUnauthorized(service.listMerchants(includeArchived, merchantType, page, size)));
     }
 
@@ -122,6 +126,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("governance.project.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listProjects(page, size));
     }
 
@@ -137,12 +142,14 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listSpaces(page, size));
     }
 
     @GetMapping("/space-types")
     public ApiResponse<PageResponse<SpaceTypeRecord>> listSpaceTypes() {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listSpaceTypes());
     }
 
@@ -165,6 +172,7 @@ public class PlatformController {
     @GetMapping("/venue-business-hours")
     public ApiResponse<PageResponse<VenueBusinessHoursRecord>> listVenueBusinessHours() {
         authorizationService.requirePermission("governance.space.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(PageResponse.of(venueBusinessHoursService.list(), 1, 500));
     }
 
@@ -182,6 +190,7 @@ public class PlatformController {
             @org.springframework.web.bind.annotation.RequestParam(name = "page", defaultValue = "1") int page,
             @org.springframework.web.bind.annotation.RequestParam(name = "size", defaultValue = "500") int size) {
         authorizationService.requirePermission("entity.binding.view");
+        authorizationService.requirePlatformScope();
         return ApiResponse.ok(service.listBusinessEntityBindings(page, size));
     }
 
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
index f7eccbd8..6648d901 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.util.List;
 import java.util.Optional;
@@ -19,7 +20,8 @@ public interface PlatformRepository {
     }
 
     /**
-     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）。
+     * V260：列出商户，includeArchived=false 时排除已存档 + 已审批删除（默认列表）；
+     * M009-010：includeArchived=true 返回全量（含已审批删除行，供「已存档内容」查阅与恢复，BSM-004）。
      * 默认返回全量（内存 fake 用）；JdbcPlatformRepository 用 SQL 谓词过滤。
      */
     default List<MerchantRecord> listMerchants(boolean includeArchived) {
@@ -29,7 +31,7 @@ public interface PlatformRepository {
                 .toList();
     }
 
-    /** V260：物理删除商户（仅经删除审批通过后调用）。默认不支持，Jdbc/内存 fake 覆写。 */
+    /** V260/M009-010：软删除商户——标记 delete_status='approved'，行保留可查可恢复（BSM-004）；仅经删除审批通过或最高权限直接执行时调用。默认不支持，Jdbc/内存 fake 覆写。 */
     default void deleteMerchant(String merchantId) {
         throw new UnsupportedOperationException("deleteMerchant not supported by this repository");
     }
@@ -64,6 +66,16 @@ public interface PlatformRepository {
 
     List<SpaceRecord> listSpaces();
 
+    /** M009-006：主体是否存在（绑定引用校验）。默认基于 listBusinessEntities；Jdbc 覆写按主键。 */
+    default boolean businessEntityExists(String businessEntityId) {
+        return listBusinessEntities().stream().anyMatch(entity -> entity.id().equals(businessEntityId));
+    }
+
+    /** M009-007：项目是否存在（空间归属校验）。默认基于 listProjects；Jdbc 覆写按主键。 */
+    default boolean projectExists(String projectId) {
+        return listProjects().stream().anyMatch(project -> project.id().equals(projectId));
+    }
+
     /** 3.13：按 id 查空间（存在性校验用）。默认基于 listSpaces 过滤（空间量级小）；Jdbc 可按需覆写。 */
     default Optional<SpaceRecord> findSpaceById(String spaceId) {
         return listSpaces().stream().filter(space -> space.id().equals(spaceId)).findFirst();
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 0de37ac2..c29d5c8a 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -32,7 +33,6 @@ import static com.complex.platform.common.Strings.requireText;
 public class PlatformService {
 
     private static final int DEFAULT_PAGE = 1;
-    private static final int DEFAULT_PAGE_SIZE = 50;
     // AUD-005: 列表端点默认页大小(保留既有一次拉取上限, 供下拉/客户端分页); total 已取真值。
     private static final int LIST_MAX_PAGE_SIZE = 500;
 
@@ -45,38 +45,38 @@ public class PlatformService {
     public static final Set<String> MERCHANT_BUSINESS_FORMATS =
             Set.of("bar", "catering", "retail", "amusement", "media", "other");
 
+    /** M009-003：BEQR-148/BSM-028/PGM-026 合作方式六类（自营/租赁/联营/代运营/服务商接入/其他）的技术代码；空＝招商签约阶段尚未确定。 */
+    public static final Set<String> MERCHANT_COOPERATION_FORMS =
+            Set.of("self_operated", "lease", "joint_operation", "managed_operation", "service_provider", "other");
+
+    /** M009-004：BEQR-155 商户/运营主体可设状态（待准入/正常/暂停/终止/黑名单）；「归档」只经存档端点（archived_at 轴），不作可设代码。 */
+    public static final Set<String> SUBJECT_STATUSES =
+            Set.of("pending_admission", "active", "suspended", "terminated", "blacklisted");
+
+    /** M009-004：BEQR-154 综合体项目状态六态（草稿/待启用/已启用/暂停/终止/归档）。 */
+    public static final Set<String> PROJECT_STATUSES =
+            Set.of("draft", "pending_activation", "active", "suspended", "terminated", "archived");
+
+    /** M009-005：PGM-018/019 主体角色——顶层平台 / 城市项目公司 / 域主管公司（业态主体）。 */
+    public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
+
     private final PlatformRepository repository;
     private final AuditedOperationExecutor operations;
     private final AuditEventService auditEventService;
     // F-AUTHZ-02 治理写仅平台：治理写方法（建商户/法律实体绑定/合同/项目空间等）首行经
     // requirePlatformScope() fail-closed 拒 merchant-scoped 主体，防权限被下放给商户角色后越权治理。
     private final AuthorizationService authorizationService;
-    // AUD-076 撤场提前到期：商户存档=撤场；被存档商户为酒吧时其单店票已售权益提前到期（走方案2）。可为 null=兼容既有测试。
-    private final com.complex.platform.ticketing.TicketingService ticketingService;
 
-    @org.springframework.beans.factory.annotation.Autowired
     public PlatformService(
             PlatformRepository repository,
             AuditedOperationExecutor operations,
             AuditEventService auditEventService,
-            AuthorizationService authorizationService,
-            com.complex.platform.ticketing.TicketingService ticketingService
+            AuthorizationService authorizationService
     ) {
         this.repository = repository;
         this.operations = operations;
         this.auditEventService = auditEventService;
         this.authorizationService = authorizationService;
-        this.ticketingService = ticketingService;
-    }
-
-    // ponytail: 向后兼容构造——既有单测不接 ticketingService（不触发撤场提前到期）。
-    public PlatformService(
-            PlatformRepository repository,
-            AuditedOperationExecutor operations,
-            AuditEventService auditEventService,
-            AuthorizationService authorizationService
-    ) {
-        this(repository, operations, auditEventService, authorizationService, null);
     }
 
     public PageResponse<BusinessEntityRecord> listBusinessEntities() {
@@ -111,17 +111,17 @@ public class PlatformService {
                 "business_entity",
                 () -> {
                     String entityName = requireText(request == null ? null : request.entityName(), "entity_name");
-                    String entityRole = requireText(request == null ? null : request.entityRole(), "entity_role");
+                    String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
+                    String status = requireSubjectStatus(request.status());
                     Instant now = Instant.now();
                     return repository.saveBusinessEntity(new BusinessEntityRecord(
                             "be_" + UUID.randomUUID(),
                             entityName,
                             entityRole,
-                            trimToNull(request.parentEntityId()),
                             trimToNull(request.unifiedSocialCreditCode()),
                             trimToNull(request.legalRepresentative()),
                             trimToNull(request.bankAccountRef()),
-                            trimToDefault(request.status(), "active"),
+                            status,
                             now,
                             now
                     ));
@@ -159,16 +159,19 @@ public class PlatformService {
         return saveMerchant(
                 "createMerchant",
                 "mch_" + UUID.randomUUID(),
-                request
+                request,
+                false
         );
     }
 
+    /** M009-001/002：更新只覆盖档案字段；商户须已存在（404），存档/删除申请状态与 createdAt 原样保留。 */
     public MerchantRecord updateMerchant(String merchantId, MerchantProfileRequest request) {
         authorizationService.requirePlatformScope();
         return saveMerchant(
                 "updateMerchant:" + merchantId,
                 requireText(merchantId, "merchant_id"),
-                request
+                request,
+                true
         );
     }
 
@@ -203,13 +206,9 @@ public class PlatformService {
                     String r = requireText(request == null ? null : request.reason(), "reason");
                     MerchantRecord merchant = requireMerchant(id);
                     Instant now = Instant.now();
+                    // M009-011：存档＝可逆软下线（BSM-004），不是商户退出；退出缓冲期与停票不在此触发（BEQR-155）。
                     MerchantRecord saved = repository.saveMerchant(merchant.withArchive(now, actorId(), now));
                     repository.setMerchantArchiveReason(id, r);
-                    // AUD-076 撤场：被存档商户为酒吧时，其单店票停售（下单侧 TICKET_BAR_OFFLINE）+
-                    // 已售权益提前到该酒吧截止营业时刻到期（走方案2：可退未用自动退/不可退作废）。
-                    if (ticketingService != null && "bar".equals(merchant.merchantType())) {
-                        ticketingService.expireBarSingleStoreTickets(id, now);
-                    }
                     return saved;
                 }
         );
@@ -264,14 +263,27 @@ public class PlatformService {
                     }
                     Instant now = Instant.now();
                     String reason = requireText(request == null ? null : request.reason(), "reason");
-                    return repository.saveMerchant(merchant.withDelete("pending", actorId(), now, reason, now));
+                    String actor = actorId();
+                    // M009-009：BSM-004「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」——直接按软删执行，仍查占用并留痕。
+                    if (isMasterAuthority()) {
+                        long refs = repository.countMerchantReferences(id);
+                        if (refs > 0) {
+                            throw ApiException.conflict("MERCHANT_IN_USE",
+                                    "商户仍被 " + refs + " 条业务记录引用（合同/账单/工单/员工/酒吧等），不可删除");
+                        }
+                        MerchantRecord deleted = merchant.withDelete("approved", actor, now, reason, now);
+                        repository.saveMerchant(deleted);
+                        repository.deleteMerchant(id);
+                        return deleted;
+                    }
+                    return repository.saveMerchant(merchant.withDelete("pending", actor, now, reason, now));
                 }
         );
     }
 
     /**
-     * 审批删除申请。approved 且无活引用 → 物理删除；rejected → 回 none。
-     * 禁自审：审批人不得为删除申请发起人（照 supervisor_review 职责分离原则）。
+     * 审批删除申请。approved 且无活引用 → 软删除标记（delete_status=approved，行保留、默认列表不显示，BSM-004）；rejected → 回 none。
+     * M009-009：审批人须为最高权限（持授权 U 盘密钥者，BSM-004/BSM-007）；禁自审：审批人不得为删除申请发起人。
      */
     public MerchantDeleteDecision approveDeleteMerchant(String merchantId, MerchantDeleteApprovalRequest request) {
         authorizationService.requirePlatformScope();
@@ -296,6 +308,9 @@ public class PlatformService {
                     if (!"approved".equals(decision) && !"rejected".equals(decision)) {
                         throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
                     }
+                    if (!isMasterAuthority()) {
+                        throw ApiException.forbidden("MERCHANT_DELETE_APPROVER_NOT_MASTER", "商户删除只能由最高权限审批通过");
+                    }
                     // 职责分离：删除申请发起人不能审批自己的申请。actorId 为 null（无鉴权/测试）时跳过该闸。
                     String actor = actorId();
                     if (actor != null && actor.equals(merchant.deleteRequestedBy())) {
@@ -307,7 +322,7 @@ public class PlatformService {
                         repository.saveMerchant(merchant.withDelete("none", null, null, null, now));
                         return new MerchantDeleteDecision(id, "rejected", false);
                     }
-                    // approved：先校验商户无核心运营表活引用，有则拒绝硬删（防连锁孤儿）。
+                    // approved：先校验商户无核心运营表活引用，有则拒绝删除（防连锁孤儿）。
                     long refs = repository.countMerchantReferences(id);
                     if (refs > 0) {
                         throw ApiException.conflict("MERCHANT_IN_USE",
@@ -356,16 +371,21 @@ public class PlatformService {
                 () -> {
                     String name = requireText(request == null ? null : request.name(), "name");
                     String venueType = requireText(request == null ? null : request.venueType(), "venue_type");
+                    String merchantId = trimToNull(request.merchantId());
+                    if (merchantId != null) {
+                        requireMerchant(merchantId);
+                    }
+                    String status = requireProjectStatus(request.status());
                     Instant now = Instant.now();
                     return repository.saveProject(new ProjectRecord(
                             "prj_" + UUID.randomUUID(),
-                            trimToNull(request.merchantId()),
+                            merchantId,
                             venueType,
                             name,
                             trimToNull(request.code()),
                             trimToNull(request.floor()),
                             request.capacity(),
-                            trimToDefault(request.status(), "active"),
+                            status,
                             now,
                             now
                     ));
@@ -401,7 +421,7 @@ public class PlatformService {
         auditEventService.recordSuccess(
                 "space_type.viewed", "governance.space.view", "space_type", "all", null, null,
                 Map.of("count", records.size()));
-        return page(records);
+        return PageResponse.of(records, DEFAULT_PAGE, LIST_MAX_PAGE_SIZE);
     }
 
     public SpaceTypeRecord createSpaceType(com.complex.platform.governance.dto.SpaceTypeRequest request) {
@@ -445,12 +465,24 @@ public class PlatformService {
                         throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                 "铺位类型必须是铺位类型字典中的启用值");
                     }
+                    String projectId = trimToNull(request.projectId());
+                    if (projectId != null && !repository.projectExists(projectId)) {
+                        throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+                    }
+                    String merchantId = trimToNull(request.merchantId());
+                    if (merchantId != null) {
+                        requireMerchant(merchantId);
+                    }
+                    String parentSpaceId = trimToNull(request.parentSpaceId());
+                    if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
+                        throw ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在");
+                    }
                     Instant now = Instant.now();
                     return repository.saveSpace(new SpaceRecord(
                             "sp_" + UUID.randomUUID(),
-                            trimToNull(request.projectId()),
-                            trimToNull(request.merchantId()),
-                            trimToNull(request.parentSpaceId()),
+                            projectId,
+                            merchantId,
+                            parentSpaceId,
                             spaceName,
                             spaceType,
                             trimToNull(request.floorNo()),
@@ -510,6 +542,11 @@ public class PlatformService {
                 () -> {
                     BusinessEntityBindingRequest actual = requireRequest(request);
                     String merchantId = requireText(actual.merchantId(), "merchant_id");
+                    // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）。
+                    requireMerchant(merchantId);
+                    String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
+                    String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
+                    String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
                     repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
                             .ifPresent(existing -> {
                                 throw ApiException.conflict(
@@ -517,12 +554,12 @@ public class PlatformService {
                                         "该商户已存在生效中的主体绑定，不能重复绑定");
                             });
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             UUID.randomUUID().toString(),
                             merchantId,
-                            requireText(actual.operationEntityId(), "operation_entity_id"),
-                            requireText(actual.contractEntityId(), "contract_entity_id"),
-                            requireText(actual.settlementEntityId(), "settlement_entity_id"),
+                            operationEntityId,
+                            contractEntityId,
+                            settlementEntityId,
                             "active",
                             actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
                             null,
@@ -530,6 +567,8 @@ public class PlatformService {
                             now,
                             now
                     ));
+                    syncMerchantEntities(saved);
+                    return saved;
                 }
         );
     }
@@ -565,7 +604,7 @@ public class PlatformService {
                                 });
                     }
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             existing.id(),
                             existing.merchantId(),
                             existing.operationEntityId(),
@@ -578,6 +617,10 @@ public class PlatformService {
                             existing.createdAt(),
                             now
                     ));
+                    if ("active".equals(status)) {
+                        syncMerchantEntities(saved);
+                    }
+                    return saved;
                 }
         );
     }
@@ -596,10 +639,82 @@ public class PlatformService {
         return value;
     }
 
+    /** M009-003：合作方式非空时须为六类之一；空＝尚未确定（招商签约阶段）。 */
+    private static String requireCooperationForm(String raw) {
+        String value = trimToNull(raw);
+        if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
+            throw ApiException.badRequest("MERCHANT_COOPERATION_FORM_INVALID",
+                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一"
+                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
+        }
+        return value;
+    }
+
+    /** M009-004：商户/运营主体状态白名单（BEQR-155）；缺省正常（active）。 */
+    private String requireSubjectStatus(String raw) {
+        String value = trimToDefault(raw, "active");
+        if (!SUBJECT_STATUSES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "状态必须为 待准入/正常/暂停/终止/黑名单 之一"
+                            + "(pending_admission/active/suspended/terminated/blacklisted)；归档请使用存档操作");
+        }
+        return value;
+    }
+
+    /** M009-004：综合体项目状态白名单（BEQR-154）；缺省已启用（active）。 */
+    private String requireProjectStatus(String raw) {
+        String value = trimToDefault(raw, "active");
+        if (!PROJECT_STATUSES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一"
+                            + "(draft/pending_activation/active/suspended/terminated/archived)");
+        }
+        return value;
+    }
+
+    /** M009-005：主体角色白名单（PGM-018/019）；非法值在服务层以中文 400 拒绝，不落到数据库 CHECK。 */
+    private static String requireBusinessEntityRole(String raw) {
+        String value = requireText(raw, "entity_role");
+        if (!BUSINESS_ENTITY_ROLES.contains(value)) {
+            throw ApiException.badRequest("VALIDATION_FAILED",
+                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)");
+        }
+        return value;
+    }
+
+    /** M009-006：绑定引用的主体必须存在。 */
+    private String requireBusinessEntity(String raw, String fieldName) {
+        String id = requireText(raw, fieldName);
+        if (!repository.businessEntityExists(id)) {
+            throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
+        }
+        return id;
+    }
+
+    /** M009-008：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」——生效绑定是商户三个主体列的唯一来源，激活时同步投影。 */
+    private void syncMerchantEntities(BusinessEntityBindingRecord binding) {
+        repository.findMerchantById(binding.merchantId()).ifPresent(merchant -> repository.saveMerchant(
+                merchant.withEntities(binding.operationEntityId(), binding.contractEntityId(),
+                        binding.settlementEntityId(), Instant.now())));
+    }
+
+    /** M009-009：当前操作者是否为持授权 U 盘密钥的最高权限；无鉴权上下文回退 false。 */
+    private boolean isMasterAuthority() {
+        if (authorizationService == null) {
+            return false;
+        }
+        try {
+            return authorizationService.isMasterAuthority(authorizationService.currentActor());
+        } catch (RuntimeException unauthenticated) {
+            return false;
+        }
+    }
+
     private MerchantRecord saveMerchant(
             String operationId,
             String merchantId,
-            MerchantProfileRequest request
+            MerchantProfileRequest request,
+            boolean update
     ) {
         return executeWrite(
                 operationId,
@@ -613,14 +728,19 @@ public class PlatformService {
                 () -> {
                     String merchantName = requireText(request == null ? null : request.merchantName(), "merchant_name");
                     String merchantType = requireBusinessFormat(request == null ? null : request.merchantType());
+                    String cooperationForm = requireCooperationForm(request.cooperationForm());
+                    String status = requireSubjectStatus(request.status());
+                    // M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt；
+                    // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
+                    MerchantRecord existing = update ? requireMerchant(merchantId) : null;
                     Instant now = Instant.now();
                     return repository.saveMerchant(new MerchantRecord(
                             merchantId,
                             merchantName,
                             merchantType,
-                            trimToNull(request.businessEntityId()),
-                            trimToNull(request.contractEntityId()),
-                            trimToNull(request.settlementEntityId()),
+                            existing == null ? trimToNull(request.businessEntityId()) : existing.businessEntityId(),
+                            existing == null ? trimToNull(request.contractEntityId()) : existing.contractEntityId(),
+                            existing == null ? trimToNull(request.settlementEntityId()) : existing.settlementEntityId(),
                             trimToNull(request.contactName()),
                             trimToNull(request.contactPhone()),
                             trimToNull(request.ownerPerson()),
@@ -630,10 +750,16 @@ public class PlatformService {
                             trimToNull(request.secondaryContactName()),
                             trimToNull(request.secondaryContactPhone()),
                             request.rentPerSquareMeter(),
-                            trimToNull(request.cooperationForm()),
-                            trimToDefault(request.status(), "active"),
+                            cooperationForm,
+                            status,
+                            existing == null ? now : existing.createdAt(),
                             now,
-                            now
+                            existing == null ? null : existing.archivedAt(),
+                            existing == null ? null : existing.archivedBy(),
+                            existing == null ? "none" : existing.deleteStatus(),
+                            existing == null ? null : existing.deleteRequestedBy(),
+                            existing == null ? null : existing.deleteRequestedAt(),
+                            existing == null ? null : existing.deleteReason()
                     ));
                 }
         );
@@ -662,10 +788,6 @@ public class PlatformService {
         );
     }
 
-    private <T> PageResponse<T> page(List<T> items) {
-        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
-    }
-
     private String trimToDefault(String value, String defaultValue) {
         String trimmed = value == null ? "" : value.trim();
         return trimmed.isEmpty() ? defaultValue : trimmed;
@@ -678,13 +800,6 @@ public class PlatformService {
         return request;
     }
 
-    private LocalDate requireDate(LocalDate value, String fieldName) {
-        if (value == null) {
-            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
-        }
-        return value;
-    }
-
     private String idempotencyKey(BusinessEntityRequest request) {
         return request == null ? null : request.idempotencyKey();
     }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
index c7d2af05..fc6fa19f 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
 import java.util.List;
 
@@ -8,7 +9,6 @@ public record BusinessEntityRequest(
         List<String> evidenceAttachmentIds,
         String entityName,
         String entityRole,
-        String parentEntityId,
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
index b36d86eb..4e533295 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/MerchantArchiveRequest.java
@@ -1,5 +1,6 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
-/** 商户存档/取消存档请求。仅需幂等键（存档动作无额外入参）。 */
+/** 商户存档/取消存档请求。idempotencyKey 必填；reason 在存档时必填（BSM-004「存档及删除都需要填写理由」），取消存档可空。 */
 public record MerchantArchiveRequest(String idempotencyKey, String reason) {
 }
diff --git a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
index 325b9f70..eb6b15af 100644
--- a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
+++ b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.01
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -287,7 +288,8 @@ public class LeasingService {
                 contractSubjectId, contractSubjectId, contractSubjectId,
                 contactName, contactPhone, contactName,
                 spaceCode, area, area,
-                null, null, null, "leasing_auto", "active"));
+                // M009-003：合作方式只能是 HTML 六类之一，签约阶段未定＝空，不发明「leasing_auto」值。
+                null, null, null, null, "active"));
         return merchant.id();
     }
 
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index a2ef8f25..77301814 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -237,6 +238,20 @@ class PlatformControllerTests {
                 .andExpect(jsonPath("$.data.items[0].merchant_id").value("mch_1"));
 
         verify(authorizationService).requirePermission("entity.binding.view");
+        verify(authorizationService).requirePlatformScope(); // M009-015：读端与写端同法 fail-closed
+    }
+
+    // M009-015：治理读端必须带平台作用域守卫（BSM-072/BSM-074/PGM-027），不能只靠权限码。
+    @Test
+    void listingMerchantsRequiresPlatformScope() throws Exception {
+        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权查看平台级经营分析"))
+                .when(authorizationService)
+                .requirePlatformScope();
+
+        mockMvc.perform(get("/api/v1/platform/merchants")
+                        .with(user("ops").roles("P0_SMOKE")))
+                .andExpect(status().isForbidden())
+                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
     }
 
     @Test
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index d39ad848..f81d5c6d 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -62,7 +63,7 @@ class PlatformJdbcIntegrationTests {
         // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET(含 include_archived + delete_status 谓词), 第二页可达首页之后的商户。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pg", "PG Entity", "operation", null, null, null, null, "active", t, t));
+                "be_pg", "PG Entity", "operation", null, null, null, "active", t, t)); // M009-012：主体记录不再带 parent 链
         repository.saveMerchant(new MerchantRecord("mch_pg_1", "PG A", "other", "be_pg", "be_pg", "be_pg",
                 "C1", "18800000001", "Owner", "1F-A", new java.math.BigDecimal("10.00"), new java.math.BigDecimal("9.00"),
                 null, null, new java.math.BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -91,7 +92,6 @@ class PlatformJdbcIntegrationTests {
                 "be_jdbc_formal",
                 "  P0 Platform Entity  ",
                 "operation",
-                null,
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://bank/entity",
@@ -339,6 +339,24 @@ class PlatformJdbcIntegrationTests {
     }
 
 
+    // M009-010：软删除行保留可查（includeArchived=true 含已审批删除行），默认列表不显示（BSM-004）。
+    @Test
+    void approvedDeletedMerchantStaysQueryableWithIncludeArchived() {
+        Instant t = Instant.now();
+        repository.saveMerchant(new MerchantRecord("mch_soft_del", "软删商户", "other", null, null, null,
+                null, null, null, null, null, null, null, null, null, null, "active", t, t));
+        repository.deleteMerchant("mch_soft_del");
+
+        assertThat(repository.findMerchantById("mch_soft_del")).isPresent()
+                .get().extracting(MerchantRecord::deleteStatus).isEqualTo("approved");
+        assertThat(repository.listMerchants(true)).extracting(MerchantRecord::id).contains("mch_soft_del");
+        assertThat(repository.listMerchantsPaged(true, null, 1, 500).items()).extracting(MerchantRecord::id).contains("mch_soft_del");
+        assertThat(repository.listMerchants(false)).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
+        assertThat(repository.listMerchantsPaged(false, null, 1, 500).items()).extracting(MerchantRecord::id).doesNotContain("mch_soft_del");
+        // M009-006：主体存在性查询按主键。
+        assertThat(repository.businessEntityExists("be_definitely_missing")).isFalse();
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
@@ -356,7 +374,7 @@ class PlatformJdbcIntegrationTests {
     void merchantContactPhonesArePersistedAsCiphertextAndReadBackAsPlaintext() {
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii", "PII Entity", "operation", null, null, null, null, "active", t, t));
+                "be_pii", "PII Entity", "operation", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_1", "PII 商户", "other", "be_pii", "be_pii", "be_pii",
                 "第一联系人", "18800000021", "负责人", "1F-P", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 "第二联系人", "18800000022", new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -378,7 +396,7 @@ class PlatformJdbcIntegrationTests {
         // 防二次加密：把读回来的（明文）记录原样再存一次，落库仍是可解密的单层密文。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii2", "PII Entity 2", "operation", null, null, null, null, "active", t, t));
+                "be_pii2", "PII Entity 2", "operation", null, null, null, "active", t, t));
         repository.saveMerchant(new MerchantRecord("mch_pii_2", "PII 商户2", "other", "be_pii2", "be_pii2", "be_pii2",
                 "联系人", "18800000023", "负责人", "1F-Q", new BigDecimal("10.00"), new BigDecimal("9.00"),
                 null, null, new BigDecimal("1.00"), "fixed_rent_plus_commission", "draft", t, t));
@@ -392,7 +410,7 @@ class PlatformJdbcIntegrationTests {
         // DB 级边界（V249 思路）：任何绕开仓库的手写 SQL / 批量导入写明文，数据库直接拒。
         Instant t = Instant.now();
         repository.saveBusinessEntity(new BusinessEntityRecord(
-                "be_pii3", "PII Entity 3", "operation", null, null, null, null, "active", t, t));
+                "be_pii3", "PII Entity 3", "operation", null, null, null, "active", t, t));
         assertThatThrownBy(() -> jdbcTemplate.update(
                 "INSERT INTO merchants (id, merchant_name, merchant_type, contact_phone, status, created_at, updated_at) "
                         + "VALUES ('mch_pii_plain', 'X', 'other', '18800000024', 'draft', now(), now())"))
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index ef317152..4678ae92 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,8 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
+import com.complex.platform.access.AccessActor;
+import com.complex.platform.auth.SessionIssuer;
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
 import static org.assertj.core.api.Assertions.assertThat;
@@ -25,6 +28,7 @@ import com.complex.platform.governance.dto.StatusChangeRequest;
 import com.complex.platform.support.TestSecurityActors;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import java.math.BigDecimal;
+import java.time.Instant;
 import java.time.LocalDate;
 import java.util.ArrayList;
 import java.util.LinkedHashMap;
@@ -34,6 +38,8 @@ import java.util.Optional;
 import org.junit.jupiter.api.AfterEach;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
+import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
+import org.springframework.security.core.context.SecurityContextHolder;
 
 class PlatformServiceTests {
 
@@ -104,6 +110,183 @@ class PlatformServiceTests {
                 null, null, null, null, null, null, null, null, "active"));
     }
 
+    /** M009-006：绑定引用的主体须真实存在——直接落一条主体行（不经服务，避免干扰幂等计数断言）。 */
+    private void seedEntity(String id) {
+        platformRepository.saveBusinessEntity(new BusinessEntityRecord(
+                id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+    }
+
+    /** M009-009：最高权限（持授权 U 盘密钥者）身份——isMasterAuthority 要求 platform_admin + 固定 id/用户名（BSM-004/007）。 */
+    private static void asMasterAuthority() {
+        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                "platform_admin", "org_1", null, "active");
+        SecurityContextHolder.getContext().setAuthentication(
+                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+    }
+
+    // ---- M009-001/002：更新商户不得擦除存档/删除申请状态；更新不存在的商户须 404 ----
+    @Test
+    void updateMerchantKeepsArchiveAndPendingDeleteState() {
+        MerchantRecord merchant = seedMerchant("seed-keep-state");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("keep-archive", "测试存档理由"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("keep-delete-req", "清理"));
+
+        MerchantRecord updated = service.updateMerchant(merchant.id(), new MerchantProfileRequest(
+                "keep-update", "rename", List.of(), "改名后的商户", "other", "be_other",
+                "be_contract_x", "be_settlement_x", "Jane", "18800000011",
+                null, null, null, null, null, null, null, null, "active"));
+
+        assertThat(updated.merchantName()).isEqualTo("改名后的商户");
+        assertThat(updated.archivedAt()).isNotNull();
+        assertThat(updated.deleteStatus()).isEqualTo("pending");
+        assertThat(updated.deleteRequestedBy()).isEqualTo("usr_admin");
+        assertThat(updated.createdAt()).isEqualTo(merchant.createdAt());
+        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
+        assertThat(updated.businessEntityId()).isEqualTo("be_1");
+        assertThat(updated.contractEntityId()).isNull();
+        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
+    }
+
+    @Test
+    void updateMerchantRejectsUnknownMerchant() {
+        assertThatThrownBy(() -> service.updateMerchant("mch_missing", new MerchantProfileRequest(
+                "update-missing", "update", List.of(), "幽灵商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        assertThat(platformRepository.listMerchants()).noneMatch(m -> m.id().equals("mch_missing"));
+    }
+
+    // ---- M009-003：合作方式六类（BEQR-148）；空＝未定 ----
+    @Test
+    void createMerchantRejectsCooperationFormOutsideSixModes() {
+        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
+                "coop-bad", "seed", List.of(), "合作方式错的商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, "fixed_rent_plus_commission", "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_COOPERATION_FORM_INVALID"));
+        MerchantRecord lease = service.createMerchant(new MerchantProfileRequest(
+                "coop-lease", "seed", List.of(), "租赁商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, "lease", "active"));
+        assertThat(lease.cooperationForm()).isEqualTo("lease");
+        assertThat(seedMerchant("coop-null").cooperationForm()).isNull();
+    }
+
+    // ---- M009-004：商户/主体/项目状态只认速查表六态（BEQR-154/155） ----
+    @Test
+    void createMerchantRejectsStatusOutsideEnumeratedStates() {
+        assertThatThrownBy(() -> service.createMerchant(new MerchantProfileRequest(
+                "status-bad", "seed", List.of(), "状态错的商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        assertThat(service.createMerchant(new MerchantProfileRequest(
+                "status-pending", "seed", List.of(), "待准入商户", "other", null,
+                null, null, null, null, null, null, null, null, null, null, null, null, "pending_admission")).status())
+                .isEqualTo("pending_admission");
+    }
+
+    @Test
+    void createProjectRejectsUnknownStatusOrMerchant() {
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "project-bad-status", "create", List.of(), null, "bar", "P0 Bar", null, null, null, "zzz")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+        // M009-007：merchantId 非空时必须存在。
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "project-bad-merchant", "create", List.of(), "mch_missing", "bar", "P0 Bar", null, null, null, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+    }
+
+    // ---- M009-005：主体角色白名单（PGM-018/019） ----
+    @Test
+    void createBusinessEntityRejectsRoleOutsideWhitelist() {
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "entity-spv", "create", List.of(), "SPV 主体", "spv", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
+    }
+
+    // ---- M009-006：绑定的经营对象与三个主体须存在 ----
+    @Test
+    void createBusinessEntityBindingRejectsMissingMerchantOrEntity() {
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-ghost-merchant", "ghost", List.of(), "mch_missing", "be_x", "be_x", "be_x", null, null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        MerchantRecord merchant = seedMerchant("seed-ghost-entity");
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-ghost-entity", "ghost", List.of(), merchant.id(), "be_missing", "be_missing", "be_missing", null, null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("BUSINESS_ENTITY_NOT_FOUND"));
+        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
+    }
+
+    // ---- M009-007：空间归属链须存在 ----
+    @Test
+    void createSpaceRejectsUnknownProjectMerchantOrParent() {
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-prj", "create", List.of(), "prj_missing", null, null, "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_NOT_FOUND"));
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-mch", "create", List.of(), null, "mch_missing", null, "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
+        assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                "sp-bad-parent", "create", List.of(), null, null, "sp_missing", "A", "shop", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
+    }
+
+    // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
+    @Test
+    void createBusinessEntityBindingSyncsMerchantEntityColumns() {
+        MerchantRecord merchant = seedMerchant("seed-sync");
+        seedEntity("be_op_sync");
+        seedEntity("be_ct_sync");
+        seedEntity("be_st_sync");
+        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "binding-sync", "sync", List.of(), merchant.id(), "be_op_sync", "be_ct_sync", "be_st_sync", null, null));
+        MerchantRecord synced = platformRepository.findMerchantById(merchant.id()).orElseThrow();
+        assertThat(synced.businessEntityId()).isEqualTo("be_op_sync");
+        assertThat(synced.contractEntityId()).isEqualTo("be_ct_sync");
+        assertThat(synced.settlementEntityId()).isEqualTo("be_st_sync");
+    }
+
+    // ---- M009-009：删除审批只认最高权限；最高权限本人发起免审批直接执行（BSM-004/007） ----
+    @Test
+    void approveDeleteByNonMasterIsForbidden() {
+        MerchantRecord merchant = seedMerchant("seed-nonmaster");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-nm", "测试存档理由"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("delete-req-nm", "清理"));
+        TestSecurityActors.asPlatformAdmin("usr_admin2");
+        assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
+                new MerchantDeleteApprovalRequest("delete-approve-nm", "approved", null)))
+                .isInstanceOfSatisfying(ApiException.class, exception ->
+                        assertThat(exception.code()).isEqualTo("MERCHANT_DELETE_APPROVER_NOT_MASTER"));
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().deleteStatus()).isEqualTo("pending");
+    }
+
+    @Test
+    void requestDeleteByMasterAuthorityExecutesWithoutApproval() {
+        MerchantRecord merchant = seedMerchant("seed-master");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("archive-master", "测试存档理由"));
+        platformRepository.merchantReferenceCount = 0;
+        asMasterAuthority();
+        MerchantRecord deleted = service.requestDeleteMerchant(merchant.id(),
+                new MerchantDeleteRequest("delete-req-master", "最高权限直接清理"));
+        assertThat(deleted.deleteStatus()).isEqualTo("approved");
+        assertThat(deleted.deleteRequestedBy()).isEqualTo(SessionIssuer.MASTER_USER_ID);
+        assertThat(service.listMerchants(false).items()).noneMatch(m -> m.id().equals(merchant.id()));
+    }
+
+    // ---- M009-014：字典分页元数据须覆盖全部条目 ----
+    @Test
+    void listSpaceTypesPageSizeCoversAllItems() {
+        var page = service.listSpaceTypes();
+        assertThat(page.pageSize()).isGreaterThanOrEqualTo(page.items().size());
+        assertThat(page.pageSize()).isEqualTo(500);
+    }
+
     // V348 商户业态标准字段：五值之外的业态一律拒收（业态错＝通票分账份额拆给错的业态）。
     @Test
     void createMerchantRejectsBusinessFormatOutsideFiveValues() {
@@ -152,7 +335,7 @@ class PlatformServiceTests {
                 new MerchantDeleteRequest("delete-req", "录入错误，退场清理"));
 
         platformRepository.merchantReferenceCount = 0;
-        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人
+        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人
         MerchantDeleteDecision decision = service.approveDeleteMerchant(merchant.id(),
                 new MerchantDeleteApprovalRequest("delete-approve", "approved", null));
 
@@ -169,7 +352,7 @@ class PlatformServiceTests {
                 new MerchantDeleteRequest("delete-req-2", "尝试删除仍在用商户"));
 
         platformRepository.merchantReferenceCount = 3; // 模拟仍被酒吧商品/合同等引用
-        TestSecurityActors.asPlatformAdmin("usr_admin2"); // 禁自审:审批人≠申请发起人
+        asMasterAuthority(); // M009-009：删除只能由最高权限审批（BSM-004），且≠申请发起人
 
         assertThatThrownBy(() -> service.approveDeleteMerchant(merchant.id(),
                 new MerchantDeleteApprovalRequest("delete-approve-2", "approved", null)))
@@ -187,8 +370,7 @@ class PlatformServiceTests {
                 "create entity",
                 List.of("att_1"),
                 " P0 Entity ",
-                "operation",
-                null,
+                "management_company",
                 "91310000MA1P0",
                 "Jane Ops",
                 "vault://entity/bank",
@@ -209,11 +391,12 @@ class PlatformServiceTests {
 
     @Test
     void createProjectUsesOpenApiAuditEventNames() {
+        MerchantRecord owner = seedMerchant("seed-project-owner"); // M009-007：merchantId 须存在
         ProjectRecord created = service.createProject(new ProjectRequest(
                 "project-create",
                 "create project",
                 List.of(),
-                "mch_1",
+                owner.id(),
                 "bar",
                 "P0 Bar",
                 "BAR-01",
@@ -231,7 +414,8 @@ class PlatformServiceTests {
 
     @Test
     void updateMerchantWritesAuditAndPreservesId() {
-        MerchantRecord updated = service.updateMerchant("mch_existing", new MerchantProfileRequest(
+        MerchantRecord existing = seedMerchant("seed-existing"); // M009-002：更新须命中已有商户
+        MerchantRecord updated = service.updateMerchant(existing.id(), new MerchantProfileRequest(
                 "merchant-update",
                 "update merchant",
                 List.of(),
@@ -249,11 +433,11 @@ class PlatformServiceTests {
                 "Jack Contact",
                 "18800000013",
                 BigDecimal.valueOf(12.30),
-                "fixed_rent_plus_commission",
+                "lease",
                 "active"
         ));
 
-        assertThat(updated.id()).isEqualTo("mch_existing");
+        assertThat(updated.id()).isEqualTo(existing.id());
         assertThat(updated.merchantName()).isEqualTo("P0 Merchant");
         assertThat(updated.mallBlockCode()).isEqualTo("1F-A");
         assertThat(updated.ownerPerson()).isEqualTo("Owner Li");
@@ -262,7 +446,10 @@ class PlatformServiceTests {
         assertThat(updated.secondaryContactName()).isEqualTo("Jack Contact");
         assertThat(updated.secondaryContactPhone()).isEqualTo("18800000013");
         assertThat(updated.rentPerSquareMeter()).isEqualByComparingTo("12.30");
-        assertThat(updated.cooperationForm()).isEqualTo("fixed_rent_plus_commission");
+        assertThat(updated.cooperationForm()).isEqualTo("lease");
+        // M009-008：三个主体列以生效绑定为准，档案更新不覆盖（seed 时 businessEntityId=be_1、其余空）。
+        assertThat(updated.businessEntityId()).isEqualTo("be_1");
+        assertThat(updated.contractEntityId()).isNull();
         assertThat(auditEventRepository.events)
                 .extracting(AuditEvent::eventName)
                 .contains("merchant.updated");
@@ -296,11 +483,16 @@ class PlatformServiceTests {
 
     @Test
     void createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant() {
+        MerchantRecord merchant = seedMerchant("seed-binding"); // M009-006：经营对象与主体须存在
+        for (String id : List.of("be_operation_1", "be_contract_1", "be_settlement_1",
+                "be_operation_2", "be_contract_2", "be_settlement_2")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRequest first = new BusinessEntityBindingRequest(
                 "binding-first",
                 "first binding",
                 List.of("att_1"),
-                "mch_binding",
+                merchant.id(),
                 "be_operation_1",
                 "be_contract_1",
                 "be_settlement_1",
@@ -311,12 +503,12 @@ class PlatformServiceTests {
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(first);
 
         assertThat(created.status()).isEqualTo("active");
-        assertThat(created.merchantId()).isEqualTo("mch_binding");
+        assertThat(created.merchantId()).isEqualTo(merchant.id());
         assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-second",
                 "second binding",
                 List.of(),
-                "mch_binding",
+                merchant.id(),
                 "be_operation_2",
                 "be_contract_2",
                 "be_settlement_2",
@@ -333,11 +525,16 @@ class PlatformServiceTests {
 
     @Test
     void changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness() {
+        MerchantRecord merchant = seedMerchant("seed-change"); // M009-006
+        for (String id : List.of("be_operation", "be_contract", "be_settlement",
+                "be_operation_new", "be_contract_new", "be_settlement_new")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-change-create",
                 "create binding",
                 List.of(),
-                "mch_change",
+                merchant.id(),
                 "be_operation",
                 "be_contract",
                 "be_settlement",
@@ -353,7 +550,7 @@ class PlatformServiceTests {
                 "binding-change-replacement",
                 "replacement binding",
                 List.of(),
-                "mch_change",
+                merchant.id(),
                 "be_operation_new",
                 "be_contract_new",
                 "be_settlement_new",
@@ -371,11 +568,15 @@ class PlatformServiceTests {
     // P1-4 red→green: action 字段拒绝非法枚举值
     @Test
     void changeBusinessEntityBindingRejectsInvalidAction() {
+        MerchantRecord merchant = seedMerchant("seed-enum"); // M009-006
+        for (String id : List.of("be_op", "be_ctr", "be_stl")) {
+            seedEntity(id);
+        }
         BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-enum-create",
                 "create",
                 List.of(),
-                "mch_enum",
+                merchant.id(),
                 "be_op",
                 "be_ctr",
                 "be_stl",
@@ -397,8 +598,7 @@ class PlatformServiceTests {
                 "entity corp-A",
                 List.of(),
                 "Corp A",
-                "operation",
-                null,
+                "management_company",
                 null,
                 null,
                 null,
@@ -409,8 +609,7 @@ class PlatformServiceTests {
                 "entity corp_A",
                 List.of(),
                 "Corp B",
-                "operation",
-                null,
+                "management_company",
                 null,
                 null,
                 null,
@@ -431,7 +630,7 @@ class PlatformServiceTests {
         assertThat(service.listSpaceTypes().items()).extracting(SpaceTypeRecord::typeCode).contains("kiosk", "shop");
         // 新录入类型立即可用于 createSpace 硬校验。
         SpaceRecord space = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-kiosk", "create", List.of(), "prj1", "mch1", null, "K1", "kiosk", null, null, null, "active"));
+                "sp-kiosk", "create", List.of(), null, null, null, "K1", "kiosk", null, null, null, "active")); // M009-007：归属为空或须存在
         assertThat(space.spaceType()).isEqualTo("kiosk");
     }
 
@@ -439,7 +638,7 @@ class PlatformServiceTests {
     void createSpaceRejectsUnknownSpaceType() {
         // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                        "sp-bad", "create", List.of(), "prj1", "mch1", null, "Bad", "no_such_type", null, null, null, "active")))
+                        "sp-bad", "create", List.of(), null, null, null, "Bad", "no_such_type", null, null, null, "active")))
                 .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                         assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
     }
@@ -447,7 +646,7 @@ class PlatformServiceTests {
     @Test
     void createSpaceAcceptsDictionarySpaceType() {
         SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-ok", "create", List.of(), "prj1", "mch1", null, "Shop A", "shop", "1", null, null, "active"));
+                "sp-ok", "create", List.of(), null, null, null, "Shop A", "shop", "1", null, null, "active"));
         assertThat(saved.spaceType()).isEqualTo("shop");
     }
 
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
index dbd47b67..0e60c08e 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.01
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -51,6 +52,7 @@ class LeasingSignAtomicityJdbcIntegrationTests {
         jdbcTemplate.update("DELETE FROM contracts WHERE contract_subject_id = ?", subject);
         jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE contract_entity_id = ?", subject);
         jdbcTemplate.update("DELETE FROM merchants WHERE contract_entity_id = ?", subject);
+        jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", subject); // M009-006 夹具主体
         jdbcTemplate.update("DELETE FROM leasing_intents WHERE id = ?", intentId);
         jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
         jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
@@ -114,6 +116,9 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     private void seed(String intendedSpaceId) {
         TestSecurityActors.asPlatformAdmin();
         Instant now = Instant.now();
+        // M009-006：四类主体绑定要求主体真实存在（BEQR-143/PGM-026），合同主体须先在 business_entities 建档。
+        jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
+                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约测试合同主体");
         leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
                 "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, null, now, now));
         leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", intendedSpaceId,
````


---

## 4. 逐 ID 修复内容（是否修好 / 最懒可行 / 有无新缺陷或业务发明）

分类顺序仍是冲突→细化→技术性→添加。本轮无新添加项、无 questions、无 HTML 修改申请。

| ID | 分类 | 是否修好 | 最懒 | 新缺陷/发明 | 证据 |
|---|---|---|---|---|---|
| 001 | 冲突 | 是 | 是 | 无 | `saveMerchant(..., update=true)` 先 `requireMerchant`，25 参保留 `archived_*` / `delete_*` / `createdAt`；不改默认列表过滤。测试 `updateMerchantKeepsArchiveAndPendingDeleteState`：改名后仍 pending+archived，默认列表不出现 |
| 002 | 技术性 | 是 | 是 | 无 | 与 001 同处；不存在 → `MERCHANT_NOT_FOUND`。测试 `updateMerchantRejectsUnknownMerchant`；既有 `updateMerchantWritesAuditAndPreservesId` 改为先 seed |
| 003 | 冲突 | 是 | 是 | 无 | 服务层六类白名单（空=未定）；页面内中文 option，未改 `statusLabels.ts`；`LeasingService` `"leasing_auto"`→null。测试拒绝 `fixed_rent_plus_commission`、接受 `lease`。默认下拉初值改为 `lease`（六类之一，非第七类） |
| 004 | 冲突 | 是 | 是 | 无 | 商户/主体 `pending_admission/active/suspended/terminated/blacklisted`；项目六态；归档不作可设代码。测试拒 `draft`（商户）/`zzz`（项目）。不造状态机/缓冲期 |
| 005 | 技术性 | 是 | 是 | 无 | 三角色白名单，拒 `spv`（025 仍否）。服务层 400 中文。不含 spv/operation |
| 006 | 细化 | 是（仅存在性） | 是 | 无 | 绑定前 `requireMerchant`+三主体 `businessEntityExists`；未在 `createMerchant` 自动写绑定。仓库默认方法+Jdbc COUNT。签约夹具插入/清理 `business_entities` 是存在性落地后的必要伴生。未做「建档即绑定」 |
| 007 | 细化 | 是 | 是 | 无 | `createProject.merchantId`、`createSpace` 的 project/merchant/parent 非空则须存在。测试三条 404 |
| 008 | 细化 | 是 | 是 | 无 | 绑定创建/激活 `syncMerchantEntities`；档案更新走 `existing` 三列不覆盖。`withEntities` 沿用已有 `withArchive`/`withDelete` 形态。不改结算侧、不 DROP 列 |
| 009 | 冲突 | 是 | 是 | 无 | `approveDeleteMerchant` 增加 `isMasterAuthority` 闸（权限码检查仍在控制器）；最高权限 `requestDelete` 在已存档+无占用时直接软删并留痕。复用既有 `AuthorizationService.isMasterAuthority`（platform_admin + 固定 MASTER id/用户名）。同构六处未改（019） |
| 010 | 冲突 | 是 | 是 | 无 | 四处注释去掉「物理删除」/「仅需幂等键」；Jdbc `includeArchived=true` 谓词改为全量（含 approved）；`deleteMerchant` 仍是 UPDATE。测试 `approvedDeletedMerchantStaysQueryableWithIncludeArchived` 本席复跑 PASS。内存 fake 仍 `remove`，定稿已归 M011，本批不改 |
| 011 | 冲突 | 是 | 是 | 无 | 删除存档路径 `expireBarSingleStoreTickets` 调用及五参构造/字段。`TicketingService` 方法本身未删（票务模块）。before 树 1 处调用，after 树 0 处 |
| 012 | 冲突 | 是 | 是 | 无 | 仅从 Record/DTO/Service 写入/Jdbc INSERT·SELECT·map 去掉 `parentEntityId`。列仍在（V318 `VARCHAR(64)` 可空），未改 V318/V354、未 DROP、未新建协作表 |
| 014 | 技术性 | 是 | 是 | 无 | 删除 `page()` 与 `DEFAULT_PAGE_SIZE=50`；`listSpaceTypes` 改 `PageResponse.of(..., LIST_MAX_PAGE_SIZE)`。测试 `listSpaceTypesPageSizeCoversAllItems` pageSize=500 |
| 015 | 细化 | 是 | 是 | 无 | 七个治理 GET 均 `requirePlatformScope()`（写端仍走服务 fail-closed）。控制器 before 1 处（已有写端）、after 8 处。测试 `listingMerchantsRequiresPlatformScope` 403；既有绑定列表测试补 verify。未改权限种子 |
| 016 | 技术性 | 是 | 是 | 无 | 删除无调用的 `requireDate`、`readObjectMap` 及 `OBJECT_MAP_TYPE`。before 各 1 处，after 0 |

无业务发明：六类合作方式代码是既有中文口径的技术映射（BEQR-148/BSM-028/PGM-026）；三角色对应 PGM-018/019；状态白名单对应 BEQR-154/155 明文。`leasing_auto` 删除而非改成第七类。

最懒：能删则删（011 调用与兼容构造、012 字段、014 `page()`、016 死代码）；能改谓词就不加列；存在性用 COUNT/默认 list 扫描，不加 FK/迁移。

---

## 5. TDD 红绿（本席独立，不只信席甲日志）

席甲日志 `/tmp/m009_b3_red.log`、`/tmp/m009_b3_green.log`、`/tmp/m009_b3_green2.log`、`/tmp/m009_b3_web.log` 只作对照。本席不得在主仓回滚源码重放红阶段（B4 禁止写源），故红用 before 树源码对照新测试断言（类型 4），绿用当前 after 工作区实跑。

**红（类型 4，before 树 vs 新测试）：**

- before `PlatformService` 无 `requireCooperationForm` / 无更新 `requireMerchant` / 无主体角色与状态白名单 / 无绑定存在性 / 无空间项目存在性 / 无 `syncMerchantEntities` / 无 `isMasterAuthority` 闸 / `page()` 固定 50 / 存档仍调 `expireBarSingleStoreTickets`。新测试在这些点上必然失败。
- before 控制器 GET 不调 `requirePlatformScope`（仅 1 处写端已有）；`listingMerchantsRequiresPlatformScope` 在红阶段按席甲日志为 403 期望对 500 实得，属可失败。
- before Jdbc 谓词 `(:include_archived OR archived_at IS NULL) AND delete_status <> 'approved'`，`includeArchived=true` 仍排除 approved；新 JDBC 测试必红。
- 席甲红日志对照（非本席实跑）：治理三套件 51 条中 17 失败（13 服务 + 2 控制器 + 1 新 JDBC 010 + 1 既有 017 环境红），与上列断言集合一致。

结构改动允许进红阶段（编译级）：006/007 仓库默认方法、008 `withEntities`、012 去 parent 后测试构造函数 arity——否则新测试无法编译。这与闸 3「为某 ID 写的验证性测试归因得到它」及编译级伴生一致。

**绿（本席 2026-09-09 实跑，`COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test` 及用户名口令三变量，未 source 整份 env）：**

```
PlatformServiceTests 29/29
PlatformControllerTests 10/10
PlatformJdbcIntegrationTests 12 条中 11 PASS；唯一 FAIL = residualMigration… expected 399 but was 409（M009-017 环境，非本 diff）
  其中 approvedDeletedMerchantStaysQueryableWithIncludeArchived = PASS
LeasingServiceTests 11/11
LeasingSignAtomicityJdbcIntegrationTests 2/2
LeasingJdbcIntegrationTests 4 条中 3 PASS；唯一 FAIL = migrationCreates… 399≠409（同 017）
apps/web OrganizationMerchantPage.test.tsx 12/12（本席 npm run test:unit）
```

合计本席套件 68 后端测试：66 PASS + 2 环境红（017）。与 green2 形态相同。Flyway 日志：`Current version of schema "public": 409`，`newer than the latest available migration (399)`。

011 无单独存活单测：删除五参构造后无法再对 mock `TicketingService` 断言「未调用」。本席以 after 树 `expireBarSingleStoreTickets` 在 `PlatformService` 中 0 处、构造只剩四参为证。012/016 为签名/死代码删除，编译即证。不因此判内容 FAIL。

---

## 6. §5 闸 3 逐行归因（96 hunk）

判定：每一行都能归因到队列「是」的 15 个 ID 之一。验证性测试归因到它所证明的 ID；`// 文件版本：V0.01` 按操作员版本铁律随该文件所属 ID。混合 hunk 按下表拆到行。归因不到的行 = 0。

文件版本行（16 处，均无旧版本号，按铁律加 V0.01）所属：

| 文件 | 版本行归因 |
|---|---|
| OrganizationMerchantPage.test.tsx / .tsx | 003 |
| BusinessEntityRecord.java / BusinessEntityRequest.java | 012 |
| JdbcPlatformRepository.java | 010/006/007/012/016（该文件承载这些 ID） |
| MerchantDeleteDecision.java / MerchantArchiveRequest.java | 010 |
| MerchantRecord.java | 008 |
| PlatformController.java | 015 |
| PlatformRepository.java | 010/006/007 |
| PlatformService.java | 001–012、014、016（该文件主修复面） |
| LeasingService.java | 003 |
| PlatformControllerTests.java | 015 |
| PlatformJdbcIntegrationTests.java | 010/012/006 |
| PlatformServiceTests.java | 001–009、014 等最小测试 |
| LeasingSignAtomicityJdbcIntegrationTests.java | 006 |

Hunk 01–09 页面与测试：003。
Hunk 10–11 Record 去 parent：012。
Hunk 12 版本：见上。Hunk 13 `OBJECT_MAP_TYPE` 删除：016。Hunk 14–15、18、20–21 Jdbc 去 parent 列：012。Hunk 16 列表注释：010。Hunk 17 谓词：010；`businessEntityExists`：006；`projectExists`：007。Hunk 19 分页谓词：010。Hunk 22 `readObjectMap` 删除：016。
Hunk 23 删除决策注释：010。Hunk 24 版本、Hunk 25 `withEntities`：008。
Hunk 26 版本、Hunk 27–32 七个 GET `requirePlatformScope`：015。
Hunk 33 版本、Hunk 34 列表 javadoc：010。Hunk 35 `deleteMerchant` javadoc：010。Hunk 36 两个 exists 默认方法：006 与 007 各一块。
Hunk 37 版本。Hunk 38 删 `DEFAULT_PAGE_SIZE`：014。Hunk 39 常量 003/004/005 + 删除 ticketing 字段与五参构造：011。Hunk 40 主体创建：005+004+012。Hunk 41 更新标志：001/002。Hunk 42 删 expireBar：011。Hunk 43 最高权限直达软删：009。Hunk 44–45 审批闸与注释：009/010。Hunk 46 项目：007+004。Hunk 47 `PageResponse.of`：014。Hunk 48 空间存在性：007。Hunk 49–51 绑定创建存在性+同步：006+008。Hunk 52–53 激活同步：008。Hunk 54 私有校验/同步/`isMasterAuthority`：003/004/005/006/008/009。Hunk 55–56 `saveMerchant` 保留态与三列：001/002/003/004/008。Hunk 57 删 `page()`：014。Hunk 58 删 `requireDate`：016。
Hunk 59–60 DTO 去 parent：012。Hunk 61 存档请求注释：010。Hunk 62–63 `"leasing_auto"`→null：003。
Hunk 64–65 控制器测试：015。
Hunk 66 版本。Hunk 67–68、70–72 JDBC 构造去 parent 参数：012（夹具仍写 `operation`/`fixed_rent_plus_commission`/`draft` 是仓库直写，不经服务白名单，本 hunk 只改 arity）。Hunk 69 软删可查测试：010；同测试末行 `businessEntityExists`：006。
Hunk 73–75 测试 import（AccessActor/SessionIssuer/Instant/SecurityContext）：009/006。Hunk 76 新增用例块：001/002/003/004/005/006/007/008/009/014（`seedEntity` 归 006，`asMasterAuthority` 归 009）。Hunk 77–78 既有审批改最高权限身份：009。Hunk 79、89–90 `operation`→`management_company` 并去掉 parent 实参：005+012。Hunk 80 项目用例先 seed 商户：007。Hunk 81–83 更新用例改 seed/`lease`/三列不覆盖：002+003+008。Hunk 84–88 绑定用例补商户与主体：006。Hunk 91–93 空间用例归属改空：007。
Hunk 94–96 签约夹具插入/清理 `business_entities`：006。

无「顺手优化」「换测试框架」「补抽象」行。无归因到 013/017–025 的行。

---

## 7. 越界检查

| 禁止项 | 本轮两树 diff |
|---|---|
| 三份业务 HTML | blob 不变 |
| 锁 / sidecar / ledger 基准 | blob 不变；ledger 工作区有 B3 记事，但不在两树 diff 内 |
| 权限种子 | 无名 |
| 迁移文件 | 无名；无 V410 |
| M015 `ProjectRecord` | 未改 |
| 013/017–025 对应内容 | 无 |
| 其它模块业务逻辑 | `LeasingService` 仅 `"leasing_auto"`→null（003 定稿伴生）；`OrganizationMerchantPage` 六类 option+测试（003）；M011 三测试文件夹具/最小测试归因各 ID；`LeasingSignAtomicityJdbcIntegrationTests` 夹具（006 必要伴生）。`TicketingService.expireBarSingleStoreTickets` 方法体未删 |

`PlatformRepository.countMerchantReferences` javadoc 仍留「拒硬删」字样，不在 010 所列四处内，本批未扩改，不另开 ID、不判越界。

内存 fake `deleteMerchant` 仍 `remove`，定稿已写明归 M011，本 diff 未改它，不判越界。

---

## 8. 复跑命令（本席）

后端（只 export 三个 DB 变量，库名 `complex_test`）：

```
mvn -Dtest=PlatformServiceTests,PlatformControllerTests,PlatformJdbcIntegrationTests,LeasingServiceTests,LeasingSignAtomicityJdbcIntegrationTests,LeasingJdbcIntegrationTests test
```

前端：

```
cd apps/web && npm run test:unit -- src/admin/pages/OrganizationMerchantPage.test.tsx
```

未 commit / push / PR / merge / 部署。未在主仓写源。未跑写模式 SQL。

---

## 9. 问题二分（回退统一规则）

有 ID 且仍在修复队列、本轮未修好：无。
无 ID 或不在修复队列的新缺陷：无。

残留观察（不构成 FAIL、不新开 ID）：

- 011 无单独存活单测（删除依赖后无法对 mock 断言）；以源码 0 调用为证。
- 010 内存 fake 仍物理 remove（已定稿归 M011）。
- M011 JDBC 直写夹具仍出现 `operation` / `fixed_rent_plus_commission` / `draft`（不经服务层，红绿不依赖它们改值）。
- `LeasingServiceTests` mock 返回值仍带 `leasing_auto` 字符串（不经真实 `createMerchant`）。
- 017 环境红 399≠409 仍在，非本 diff。

---

## 10. 技能与缺口

已读并按收窄口径使用：`using-superpowers`（开工自检）、`verification-before-completion`（绿必须本席实跑）。B4 按提示词 §6：不派第三席、不 `HEAD~1`、不修。`grilling` 本轮按自问自答取证，未打开该 SKILL.md 全文——记缺口，不冒称已核实调用。未派 Arc audit 子代理。ponytail 只作「最懒可行」尺度，不落地代码。

---

## 11. 结论

修复内容：PASS（15 项均按定稿最小边界落地，TDD 红有 before 树对照、绿有本席复跑；017 环境红不是本轮引入）。
范围归因：PASS（16 文件 96 hunk 527+/124- 逐行可归到 001–012/014–016；无越界）。
总结论：PASS。

此票不是模块 PASS，也不是 B4 双 PASS。须席甲独立 B4 票且双 PASS 后才进 B5。本席未改 findings 队列归属。
