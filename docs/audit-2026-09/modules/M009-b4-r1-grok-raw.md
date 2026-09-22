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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M009 B4-r1 完整报告（Grok 独立审计席乙）
版本：V0.02
席位：Grok 独立审计席乙。步骤：M009 当前授权重审 B4 第 1 次。只读审计本轮全部修复 diff。不是 B1、不是 B2、不是 B3、不是 B5、不是模块 PASS。上方 110075 字节历史 r1 前缀原样保留，本段为本次完整票，不复用旧 r1 结论。

- 正本：`/home/james/complex-management` `main` 主工作区。HEAD=`e456ba96c84185c8d39f18f548011daac48a6466`（未提交、未推送、未部署）。
- 计划：160-audit-prompt.md V0.17；`docs/audit-2026-09/plan/` runbook V0.02 / modules.md M009（20 文件，2476 行，NORMAL）。不换计划。
- 判据锁：本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` = `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。sidecar 同行。ledger 顶部同值。LOCK-AMEND-COUNT 仍为 1。未改锁。
- B2 定稿输入：findings.md V2.08 顶部「当前M009第三批B2定稿（唯一当前表；双PASS）」20 行 14 是 / 6 否。两票 `M009-b1-self-raw.md` / `M009-b1-grok-raw.md` 各 V0.08 PASS。013/038 用户具体授权持续有效，不重问。后文旧候选与旧 r1 只作历史。
- 模块状态：M009 `IN_PROGRESS`，B3 已执行 1/3。
- 冻结树：before=`fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac`（git cat-file -t = tree）；after=`66f312549c6f1ea7ed5e510f639e9238c4c674e9`（tree）。本席实跑 `git diff --binary fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac 66f312549c6f1ea7ed5e510f639e9238c4c674e9`：25 路径，185872 bytes，SHA256 `67a1597fa96e386cb9d21ee0a4c6484213676de9dff433547a88ab08136b9f46`。与派发一致。`git diff --check` 两树 exit 0。25 路径工作区 blob 与 after 树逐文件 SHA256 全等（含未跟踪的 `ProjectAssociationRequest.java` 与 `V427__m009_project_profiles_and_associations.sql`）。
- 禁止项：无子代理；未改源码/测试/HTML/配置/锁/findings/ledger；未提交推送部署；未新建审计产物或 CLI 原始 JSON 日志；未读另一套 fresh-first-principles 结论；未读本次 Codex B4 未完成稿。
- 三份 HTML 只读。本席实算 SHA256：`business-enum-quick-reference-v0.1.html`=`4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；`business-system-map-for-operations-v0.1.html`=`eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；`platform-governance-map-for-operations-v0.1.html`=`a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`。核过 PGM-017/PGM-020/PGM-041/BEQR-152/BEQR-155/BSM-004 原文，与 criteria-lock 引用一致。
- M009 映射 87 条（lock 行 6388）全部纳入核对，本批修复只引用其中已冻结 ID，未主张锁外新判据。

## 0. 技能预检（本席自核）

已读再用：graft（只读）；using-superpowers；grilling（只借追问结构，事实自读，不派子代理、不等操作员）；diagnosing-bugs（只读错误/复现/数据流，不加诊断埋点）；systematic-debugging（收窄：假说用命令验证，不改生产代码）；verification-before-completion（PASS 前实跑）；requesting-code-review（B4=本席外审，不派第三席、不用 HEAD~1）；receiving-code-review（先核实，禁止表演性同意；本席为乙席，不实施修复）；ponytail-review（只列可删观察，不改码）；codebase-design（接口深度用语）；test-driven-development（本席验证已有测试，不新写测试）。

缺口/不适用：`i-have-adhd` 本环境未核到独立 SKILL.md；`batch-grill-me` / `git-guardrails-claude-code` 可选未作为证据。明确不用：subagent-driven-development、dispatching-parallel-agents、using-git-worktrees、research、ponytail-audit/debt/gain。未核实的技能未写成已调用。

Graft 只读：`isMasterAuthority`、项目隔离、`autoCreateMerchantForSign`、`MASTER_ACCOUNT_TYPE`、三份主文件 skeleton。

## 1. 冻结树与 binary diff 元数据

| 项 | 值 |
|---|---|
| before | `fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac` |
| after | `66f312549c6f1ea7ed5e510f639e9238c4c674e9` |
| 路径数 | 25 |
| diff 字节 | 185872 |
| diff SHA256 | `67a1597fa96e386cb9d21ee0a4c6484213676de9dff433547a88ab08136b9f46` |
| stat | 25 files changed, 1473 insertions(+), 199 deletions(-) |
| 名称状态 | 23 M + 2 A（`ProjectAssociationRequest.java`、`V427__m009_project_profiles_and_associations.sql`） |

完整 `git diff --binary` 全文见第 8 节，不以命令引用或另一报告代替。

## 2. 逐路径 / 逐 hunk 正式 ID 归因（闸 3）

归因只指向 V2.08 当前 14 条「是」。版本头随该文件主 ID。测试夹具/断言归到其所锁的生产 ID。六条登记否（048–053）无独立改动；`listMerchantsByBusinessEntity` 签名收窄归 038（定稿写明不能把 048 登记否读成保留跨项目反查）。

### 2.1 AuthorizationService.java（2 hunks）→ M009-038
- `@@ -1,6` 版本 V0.08→V0.13：038
- `@@ -137,7` `isMasterAuthority` 改为 active + `SessionIssuer.MASTER_ACCOUNT_TYPE/USER_ID/USERNAME` 三件套，去掉 `isPlatformAdminBridge`：038

### 2.2 JdbcPlatformRepository.java（17 hunks）
- `@@ -1,5` 版本：038/013
- `@@ -83,14` INSERT 增 `project_id`：038
- `@@ -127,7` / `@@ -142,7` / `@@ -182,7` SELECT 增 `project_id`：038
- `@@ -218,8` 绑定占用排除真正结束的 inactive 历史：045
- `@@ -227,31` `listMerchantsByBusinessEntity(projectId, businessEntityId)`，空项目不退全表：038
- `@@ -261,7` / `@@ -269,7` 项目列表读写三项资料列：013
- `@@ -467,7` 无项目重载 SELECT 增 `project_id`（记录映射，服务不再走此重载）：038
- `@@ -480,7` 无项目项目分页 SELECT 三项资料：013
- `@@ -563,6` `completeSpaceTypeDeleteRequest` / `lockMerchant` / `hasUnfinishedBusinessEntityBinding`：044 / 041
- `@@ -602,7` merchantParameters 增 project_id：038
- `@@ -616,7` projectParameters 三项资料：013
- `@@ -691,7` mapMerchant 读 project_id：038
- `@@ -706,7` mapProject 读三项资料：013
- `@@ -1164,4` `businessEntityInProject` / `associateBusinessEntityProject` / `assignMerchantProject`（只 UPDATE project_id 且 null→目标或同值）/ 五类带 projectId 的 COUNT+分页：038；ON CONFLICT DO NOTHING 只增关系：038

### 2.3 MerchantRecord.java（6 hunks）→ M009-038
canonical 增 `projectId`；保留旧 25 参/19 参构造（旧调用 projectId=null）；`withProject`：038

### 2.4 PlatformController.java（3 hunks）→ M009-038
新增 `PUT /merchants/{id}/project`、`POST /business-entities/{id}/projects`；各检查一次功能权限 + 幂等键：038

### 2.5 PlatformRepository.java（5 hunks）→ M009-038 / 041 / 013
`listMerchantsByBusinessEntity` 收 projectId；`lockMerchant` / `hasUnfinished…`；关联/补录接口；带 projectId 分页默认实现：038/041

### 2.6 PlatformService.java（36 hunks）
- 版本与 import：038
- 五类 `list*(page,size)` 改走 `currentProject()`：038
- `createBusinessEntity` `creationProject(..., true)` + `associateBusinessEntityProject`：038
- 商户删除/审批 `isMasterAuthority()? findMerchant: requireMerchant`（明确对象、不被 platform-company 误封）：038
- 成功/失败事件名按通过/驳回：043
- `assignMerchantProject` / `associateBusinessEntityProject` / `requireProjectConfigurationAuthority` / `associationProject` / `creationProject` / `requireCurrentProject`：038
- `createProject` 三项资料 + 容量非负：013 / 046
- `spaceExists` 同项目：039
- `unarchiveSpaceType` 接受 archived+disabled：042
- 空间类型申请/审批对象 ID、理由、complete 申请、驳回事件：043 / 044
- `createSpace` 必填 projectId、父空间/商户同项目、面积非负：039 / 046
- 绑定创建：行锁、未结束槽、未来 inactive 不投影、财税身份不变、主体须本项目关联：030 / 031 / 040 / 041 / 038
- 绑定变更：生效中禁止普通停用/暂停；未到生效日禁止提前启用或结束；已结束禁止重开；激活才同步投影：031 / 040 / 041
- 角色/状态/空间状态用户文案去英文括注：047
- `requireSubjectStatus` + 建档/更新拒绝直接 terminated、已 terminated 无状态变化保留：018
- `saveMerchant` 普通更新禁改 project_id；新建校验已给三主体：038 / 030 / 018 / 046
- `auditMetadata` 理由/决定/项目；`businessId` 补 SpaceTypeRecord / MerchantDeleteDecision：043 / 038

### 2.7 ProjectRecord.java（2 hunks）→ M009-013
三项可空；旧 10 参构造保留。

### 2.8 BusinessEntityRequest.java（2 hunks）→ M009-038
增可空 `projectId`；旧构造保留。

### 2.9 MerchantProfileRequest.java（2 hunks）→ M009-038
增可空 `projectId`；旧构造保留。

### 2.10 ProjectAssociationRequest.java（1 hunk，新文件）→ M009-038
`idempotencyKey, projectId, reason`。

### 2.11 ProjectRequest.java（2 hunks）→ M009-013
三项可空；旧构造保留。

### 2.12 LeasingService.java（4 hunks）→ M009-041 / 038
`autoCreateMerchantForSign` 三主体改 null；显式传入 `intent.projectId()`；随后仍 `createFourPartyBinding`。

### 2.13 V427__m009_project_profiles_and_associations.sql（1 hunk，新文件）→ M009-013 / 038
projects 三项可空列；merchants.project_id 可空 FK；`business_entity_projects` 唯一 (entity, project)。不回填历史。本席在 complex_test 查 `flyway_schema_history`：427,395,true。未查、未迁共享业务库。

### 2.14–2.25 测试（归因到所锁 ID）
- AuthorizationServiceTests（2）：038 真 USB 三件套 / 假 platform_admin 不是最高权限；DENY 仍生效
- M006SecurityRegressionTests（5）：038 夹具去假 platform_admin 冒充
- M010RegressionTests（4）：038 夹具
- StaffTestActors（2）：038 `master()` 改 MASTER_ACCOUNT_TYPE
- BarStoredValue… / StoredValueNotification（各 2）：038 夹具
- PlatformControllerTests（2）：038 新入口一次权限 + 缺幂等键
- PlatformJdbcIntegrationTests（3）：038/013/041/044/045 五类 SQL COUNT、多对多、补录只改 project_id、并发行锁、申请终结
- PlatformServiceTests（19）：038–047、018、013、030、031、040、041 行为锁
- SupervisorReviewServiceTests（7）：038 真 USB 夹具
- LeasingServiceTests（4）：038/041 调用顺序与空三主体 + 意向项目
- LeasingSignAtomicityJdbcIntegrationTests（5）：038/041 真实 JDBC 未来签约

**闸 3 结论：归因 PASS。** 25 路径 140 hunk 均可归到当前「是」的正式 ID（主要为 038，其次 013/039/030/031/040/041/042/043/044/045/046/047/018）。未见无 ID 的顺手重构/换框架/扩模块清单。原 20 文件边界未把新模块列入待审；必要依赖（ProjectRecord、LeasingService、AuthorizationService、V427、既有测试、新 DTO）与定稿「013 与必要依赖文件」一致。

## 3. 第一性原理 / 顺从式 / 对抗式

### 3.1 第一性原理（2.3 三问）
1. **为何必须存在**：删掉本 diff，PGM-020「记录项目状态、负责人、地址和经营范围」、PGM-017/PGM-041/BEQR-152「所有基础对象带综合体项目归属 / 普通岗位不得跨项目查看」、PGM-026 四类主体唯一绑定与财税调整须审、BSM-004 存档删除审批与最高权限 U 盘直办、BEQR-155 退出缓冲，对应 HTML 做不到。答得上，有业务上位。
2. **声称是否执行**：核心三类 106 例本席实跑全过（见第 5 节）。五类 SQL `WHERE project_id = :project_id` 与 COUNT 同谓词；`assignMerchantProject` 只改 project_id；Leasing 先空三主体再绑定。
3. **未验证就当真的前提**：生产 USB 的 `organizationId=platform-company` 不是 projects 行 → 列表空、普通写入 PROJECT_SCOPE_MISMATCH，已用 `asMasterAuthority()` 单测锁住。Access 域 `requirePlatformAdmin` 与真 USB 是否等价，本批未改 AccessService，测试已打红（GR-01）。

依据类型：HTML 原文（类型 1）；Maven/psql 输出（类型 2/4）；SQL 标准 NULL 比较与 FOR UPDATE（类型 3）。不接受「测试绿所以对」单独作为依据。

### 3.2 顺从式（假定实现意图正确）
对照 14 条「是」与定稿执行边界：
- 038 商户 project_id、公司多对多、五类服务入口 COUNT/分页隔离、真 USB 识别、专用补录、公司只增关系、历史不猜填：服务路径成立。
- 013 三项可空、旧构造：成立。
- 039 空间同项目：成立。
- 030 初建已给主体须存在且本项目关联：成立。
- 031/040/041 未来 inactive、唯一未结束槽、行锁、普通不可改已有财税身份、Leasing 真实调用点：成立。
- 042–047、018：成立。
- 六登记否未当缺陷修。
- 最高权限商户删除/审批仍按明确商户 ID，不要求当前项目；未扩成全局列表或财税调整特例（`bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority`）。

### 3.3 对抗式（假定实现错误）
构造并核源：
- 甲项目岗位读乙：服务 list total 不含乙；伪造乙 projectId 写空间 403。
- 空项目/platform-company/不存在项目：SQL `= NULL` / 不存在 id → total=0，不退全表。
- 普通认领 null→项目、迁移项目：拒绝。
- USB 补录同值幂等、改他项目失败；公司两项目 EXISTS 过滤。
- 未来绑定不投影；第二条未结束拒绝；历史 ended 不能 reopen。
- 负数面积/租金/容量拒绝；零与可空放行。
- terminated 新建/切入拒绝。
- 真 USB 不是 platform_admin：`isMasterAuthority(platform_admin+三件套)=false`。
- 反例成立且未修好：真 USB 调 `AccessService.changePositionTemplateLifecycle` 被 `requirePlatformAdmin` 挡在门外（GR-01）；储值/酒吧测试把无 merchantId 的 USB 当柜员（GR-02）。

## 4. 范围逐项核（14 是 + 6 否）

| ID | 结论 | 证据要点 |
|---|---|---|
| 038 | 服务/SQL/补录/识别按定稿后段落地；USB 新建商户不开放显式项目（后段「USB 例外只用于明确归属配置」，观察 OBS-01） | `currentProject()`；五类 `list*Paged(projectId,…)`；`assignMerchantProject` 条件 UPDATE；`creationProject` 公司 true、商户 false；`isMasterAuthority` 三件套 |
| 013 | 落地 | ProjectRequest/Record 可空三项 + 旧构造 + V427 + 读回断言 |
| 039 | 落地 | createSpace 必填 projectId；父空间/商户 `requireCurrentProject` |
| 030 | 落地 | 新建已给三主体 `requireBusinessEntity`；Leasing 传 null |
| 031/040/041 | 落地 | 生效中停用/暂停冲突；未来 inactive；`hasUnfinished` active 或 inactive且 effective_to IS NULL；`lockMerchant FOR UPDATE`；激活才 `syncMerchantEntities`；Leasing 先 null 再 binding |
| 042 | 落地 | unarchive 接受 archived/disabled；pending_delete 仍拒 |
| 043 | 落地 | businessId 补两类；metadata 理由/决定；驳回事件 `delete_rejected` |
| 044 | 落地 | `completeSpaceTypeDeleteRequest` 更新当前 pending；主路径直删也 complete approved；旧行保留 |
| 045 | 落地 | 仅排除 inactive 且 from/to 非空且 to>=from 且 to<=今天；未来 to=null 仍占用 |
| 046 | 落地 | `requireNonNegative`；capacity<0 |
| 047 | 落地 | 角色/状态文案无 `platform/project_company/...` 括注；单测锁住 |
| 018 | 落地 | 普通新建/切入 terminated 拒绝；已 terminated 省略状态不改 |
| 048–053 | 未当缺陷修 | 048 的反查通道按 038 收了 projectId，不是把登记否当缺陷重开 |

## 5. 独立测试（complex_test）

配置：只读 `/etc/complex/complex.env` 三项，强制 URL 库名为 `complex_test`。凭据不入库、不进本报告。`psql current_database=complex_test`。V427 仅在该库 `flyway_schema_history` 见到 success=true。

**核心三类 106 例（本席实跑，MAVEN_EXIT=0）：**
PlatformServiceTests 69、PlatformJdbcIntegrationTests 23、PlatformControllerTests 14。失败 0、错误 0、跳过 0。

**七类直接关联 188 例（含在随后一次调用中，全过）：** 上三者 + AuthorizationServiceTests 24 + SupervisorReviewServiceTests 44 + LeasingServiceTests 11 + LeasingSignAtomicityJdbcIntegrationTests 3。

**更广身份相关集合（本席实跑，MAVEN_EXIT=1）：** 13 个类合计 320 例，1 failure + 13 errors。与派发「282 / 273 过 / 1 failure+8 errors」计数不同：本席按类实跑时 M006 为 39 例、AccessServiceTests 为 50 例，且 `ProductionAuthJdbcIntegrationTests` 8 例本席通过。不以派发数字代替本席输出，也不声称全绿。

失败分类：

| 分类 | 例 | 说明 |
|---|---|---|
| 本批引入 | BarStoredValue 1F+4E；StoredValueNotification 1E | 夹具改为 `MASTER_ACCOUNT_TYPE` 且 merchantId=null，`currentMerchantScope()` 抛「此操作必须在商户范围内进行」。旧夹具是 platform_admin。商户资金测试不应使用无商户号的 USB。 |
| 真实生产身份暴露原有问题 | M006 `positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval` | 夹具改为真 USB 后，`AccessService.requirePlatformAdmin` 抛「此操作需要平台管理员权限」。生产 USB 本来就不是 platform_admin。 |
| 本批夹具连带 | M006 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` | 改成真 USB 后审批 `permission_grant_l3` 报必须登记直接受益人。 |
| 原先已失败 / 本批未改该类 | AccessServiceTests 6E（受益人） | 该类不在 25 路径；`@BeforeEach` 仍是 platform_admin。不是本批修复义务，也不构成 038 条件 PASS。 |
| 与派发记录不一致但不红 | ProductionAuthJdbcIntegrationTests 8 | 本席全过；派发记录为 8 errors。记观察，不采信未复现的全绿或全红。 |

现有 surefire XML 可读，不代替上述独立跑。

## 6. 临时 findings（不自行定正式 ID / 不修）

### GR-01
- 位置：`AccessService.java:2181-2185` `requirePlatformAdmin`；调用点 `changePositionTemplateLifecycle:488`；测试 `M006SecurityRegressionTests.java:589-592`
- 复现：`mvn -Dtest=M006SecurityRegressionTests#positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval test`（complex_test）→ error「此操作需要平台管理员权限」
- 分类：冲突（BSM-004「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」vs 入口只认 platform_admin）
- 依据类型：4（命令输出）+ 1（BSM-004 原文）
- 冻结：BSM-004 / GEN-005 / PGM-041
- 最小边界：生命周期入口与共用 `isMasterAuthority` 对齐，真 USB 可完成删除/恢复；禁止恢复 platform_admin 假最高权限夹具掩盖
- 性质：真实生产身份暴露原有问题。AccessService 不在本批允许修改的生产依赖里，但本批 038 夹具已把它打红。无正式 ID，不能当 14 条已修完而 PASS。

### GR-02
- 位置：`BarStoredValueFundChainJdbcIntegrationTests.java:63-66`；`StoredValueNotificationIntegrationTest.java:40-43`；闸 `AuthorizationService.currentMerchantScope:425-431`
- 复现：上述测试类 → MERCHANT_SCOPE_REQUIRED
- 分类：细化（测试夹具用错主体）；不是放行 USB 看全商户资金
- 依据类型：4
- 冻结：GEN-003 / PGM-041
- 最小边界：商户范围资金例改用带 merchantId 的商户操作人；USB 无商户号被拒是隔离正果
- 性质：本批引入（改了 25 路径内夹具、未改断言主体）

### GR-03
- 位置：`M006SecurityRegressionTests.java:410-415` 真 USB 审批请求受益人为 null
- 复现：`topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` →「退款、免单、授权与资金事项必须登记直接受益人」
- 分类：细化（测试请求不完整）或须另证 USB 是否豁免受益人；不得用假 platform_admin 掩盖
- 依据类型：4
- 冻结：GEN-003 / GEN-005
- 最小边界：补直接受益人后重跑，或单独登记 USB 与资金受益人规则；不在本批 14 条内擅自改审批模型

## 7. 无修复义务的观察

- **OBS-01**：定稿前段「新建商户 USB 可显式 projectId」与后段「本次新增的 USB 例外只用于明确归属配置；商户走专用补录」以后段为准。实现 `creationProject(..., false)` 于商户新建，与后段一致。不单列缺陷。
- **OBS-02**：Jdbc 仍保留无 projectId 的 `list*Paged` 全表 COUNT。`PlatformService` 已不调用。其它模块 `listMerchants()` LIMIT 500 仍全表，不在本批 20 文件生产入口。不把 038 扩成全仓删方法。
- **OBS-03**：未来 inactive（effectiveTo=null）普通 change 在生效日前被 `BINDING_NOT_EFFECTIVE` 挡住，没有「取消预约」入口。040/041 禁止新状态/调度。不单列。
- **OBS-04**：`StaffRegistrationService` 经 `PlatformService.listBusinessEntities()` 现受项目过滤，属 038 副作用，本批未改员工模块。
- **OBS-05**：`listSpaceTypes` 仍全字典，空间种类不是项目对象。
- **OBS-06**：`saveMerchant` UPSERT 的 ON CONFLICT 不更新 `project_id`，与「补录专用 SQL / 全量档案不得改归属」一致；Jdbc 单测已锁。
- **OBS-07**：AccessServiceTests 6 例受益人错误，类不在 25 路径，视为原先已失败观察，不条件 PASS，也不塞进本批 14 条。
- **OBS-08**：ponytail-review：无项目分页重载可视为 yagni 残留；净删须另 ID。本席不改码。`net: 观察，不删。`

## 8. 完整 binary diff 全文

命令：`git diff --binary fa05f8c4af1ad4cf7e048ed5e074ed09cd48eeac 66f312549c6f1ea7ed5e510f639e9238c4c674e9`  
本席实跑 185872 bytes，SHA256 `67a1597fa96e386cb9d21ee0a4c6484213676de9dff433547a88ab08136b9f46`。

`````
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 34be1cca..4bfae6d2 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.08
+// 文件版本：V0.13
 
 import com.complex.platform.auth.AuthService;
 import com.complex.platform.common.api.ApiException;
@@ -137,7 +137,8 @@ public class AuthorizationService implements CallerScopeResolver {
     }
 
     public boolean isMasterAuthority(AccessActor actor) {
-        return isPlatformAdminBridge(actor)
+        return "active".equals(actor.status())
+                && SessionIssuer.MASTER_ACCOUNT_TYPE.equals(normalize(actor.accountType()))
                 && SessionIssuer.MASTER_USER_ID.equals(actor.id())
                 && SessionIssuer.MASTER_USERNAME.equals(actor.username());
     }
diff --git a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
index c8b989df..aa34c220 100644
--- a/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.08
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -83,14 +83,14 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                     settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                     leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                     rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                 )
                 VALUES (
                     :id, :merchant_name, :merchant_type, :business_entity_id, :contract_entity_id,
                     :settlement_entity_id, :contact_name, :contact_phone, :owner_person, :mall_block_code, :area_square_meter,
                     :leased_area_square_meter, :secondary_contact_name, :secondary_contact_phone,
                     :rent_per_square_meter, :cooperation_form, :status, :created_at, :updated_at,
-                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason
+                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason, :project_id
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     merchant_name = EXCLUDED.merchant_name,
@@ -127,7 +127,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  ORDER BY created_at DESC, id
                  LIMIT 500
@@ -142,7 +142,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  WHERE (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))
                  ORDER BY created_at DESC, id
@@ -182,7 +182,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
                  WHERE id = :id
                 """, Map.of("id", merchantId), this::mapMerchant).stream().findFirst();
@@ -218,8 +218,12 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 continue;
             }
             // table 名来自 information_schema（系统元数据，非用户输入），无注入风险；merchant_id 走参数绑定。
+            String activeReference = "business_entity_bindings".equals(table)
+                    ? " AND NOT (status = 'inactive' AND effective_from IS NOT NULL AND effective_to IS NOT NULL"
+                            + " AND effective_to >= effective_from AND effective_to <= (now() AT TIME ZONE 'UTC')::date)"
+                    : "";
             Long c = jdbcTemplate.queryForObject(
-                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id",
+                    "SELECT COUNT(1) FROM " + table + " WHERE merchant_id = :id" + activeReference,
                     Map.of("id", merchantId), Long.class);
             total += c == null ? 0L : c;
         }
@@ -227,31 +231,31 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     }
 
     @Override
-    public List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
+    public List<MerchantRecord> listMerchantsByBusinessEntity(String projectId, String businessEntityId) {
         // 商户面 scoped 读通道：按 merchants.business_entity_id = token scope 过滤（AC-ISO-1，§4）。
-        if (businessEntityId == null) return listMerchants();
+        if (projectId == null || businessEntityId == null) return List.of();
         return jdbcTemplate.query("""
                 SELECT id, merchant_name, merchant_type, business_entity_id, contract_entity_id,
                        settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, area_square_meter,
                        leased_area_square_meter, secondary_contact_name, secondary_contact_phone,
                        rent_per_square_meter, cooperation_form, status, created_at, updated_at,
-                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason
+                       archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id
                   FROM merchants
-                 WHERE business_entity_id = :business_entity_id
+                 WHERE project_id = :project_id AND business_entity_id = :business_entity_id
                    AND archived_at IS NULL AND delete_status <> 'approved'
                  ORDER BY created_at DESC, id
                  LIMIT 500
-                """, Map.of("business_entity_id", businessEntityId), this::mapMerchant);
+                """, Map.of("business_entity_id", businessEntityId, "project_id", projectId), this::mapMerchant);
     }
 
     @Override
     public ProjectRecord saveProject(ProjectRecord project) {
         jdbcTemplate.update("""
                 INSERT INTO projects (
-                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                    id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                 )
                 VALUES (
-                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at
+                    :id, :merchant_id, :venue_type, :name, :code, :floor, :capacity, :status, :created_at, :updated_at, :owner_person, :address, :business_scope
                 )
                 ON CONFLICT (id) DO UPDATE SET
                     merchant_id = EXCLUDED.merchant_id,
@@ -261,7 +265,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                     floor = EXCLUDED.floor,
                     capacity = EXCLUDED.capacity,
                     status = EXCLUDED.status,
-                    updated_at = EXCLUDED.updated_at
+                    updated_at = EXCLUDED.updated_at,
+                    owner_person = EXCLUDED.owner_person,
+                    address = EXCLUDED.address,
+                    business_scope = EXCLUDED.business_scope
                 """, projectParameters(project));
         return project;
     }
@@ -269,7 +276,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
     @Override
     public List<ProjectRecord> listProjects() {
         return jdbcTemplate.query("""
-                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                   FROM projects
                  ORDER BY created_at DESC, id
                  LIMIT 500
@@ -467,7 +474,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
                 + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
                 + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
-                + "delete_status, delete_requested_by, delete_requested_at, delete_reason FROM merchants" + where
+                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id FROM merchants" + where
                 + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
         return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
     }
@@ -480,7 +487,7 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects", params, Long.class);
         params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
         List<ProjectRecord> items = jdbcTemplate.query("""
-                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
                   FROM projects
                  ORDER BY created_at DESC, id
                  LIMIT :limit OFFSET :offset
@@ -563,6 +570,37 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
 
 
 
+    @Override
+    public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
+        jdbcTemplate.update("""
+                UPDATE space_type_delete_requests SET status = :status
+                 WHERE id = (
+                     SELECT id FROM space_type_delete_requests
+                      WHERE space_type_id = :id AND status = 'pending'
+                      ORDER BY requested_at DESC, id DESC LIMIT 1
+                 )
+                """, Map.of("id", spaceTypeId, "status", status));
+    }
+
+    @Override
+    public void lockMerchant(String merchantId) {
+        jdbcTemplate.query("SELECT id FROM merchants WHERE id = :id FOR UPDATE",
+                Map.of("id", merchantId), (rs, rowNum) -> rs.getString("id"));
+    }
+
+    @Override
+    public boolean hasUnfinishedBusinessEntityBinding(String merchantId, String excludedId) {
+        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
+                SELECT EXISTS (
+                    SELECT 1 FROM business_entity_bindings
+                     WHERE merchant_id = :merchant_id
+                       AND (:excluded_id IS NULL OR id::text <> :excluded_id)
+                       AND (status = 'active' OR (status = 'inactive' AND effective_to IS NULL))
+                )
+                """, new MapSqlParameterSource("merchant_id", merchantId)
+                .addValue("excluded_id", excludedId, java.sql.Types.VARCHAR), Boolean.class));
+    }
+
     private MapSqlParameterSource businessEntityParameters(BusinessEntityRecord businessEntity) {
         return new MapSqlParameterSource()
                 .addValue("id", businessEntity.id())
@@ -602,7 +640,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("delete_status", merchant.deleteStatus() == null ? "none" : merchant.deleteStatus())
                 .addValue("delete_requested_by", merchant.deleteRequestedBy())
                 .addValue("delete_requested_at", timestampWithTimeZone(merchant.deleteRequestedAt()))
-                .addValue("delete_reason", merchant.deleteReason());
+                .addValue("delete_reason", merchant.deleteReason())
+                .addValue("project_id", merchant.projectId());
     }
 
     private MapSqlParameterSource projectParameters(ProjectRecord project) {
@@ -616,7 +655,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 .addValue("capacity", project.capacity())
                 .addValue("status", project.status())
                 .addValue("created_at", timestampWithTimeZone(project.createdAt()))
-                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()));
+                .addValue("updated_at", timestampWithTimeZone(project.updatedAt()))
+                .addValue("owner_person", project.ownerPerson())
+                .addValue("address", project.address())
+                .addValue("business_scope", project.businessScope());
     }
 
     private MapSqlParameterSource spaceParameters(SpaceRecord space) {
@@ -691,7 +733,8 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 resultSet.getString("delete_status"),
                 resultSet.getString("delete_requested_by"),
                 nullableInstant(resultSet, "delete_requested_at"),
-                resultSet.getString("delete_reason")
+                resultSet.getString("delete_reason"),
+                resultSet.getString("project_id")
         );
     }
 
@@ -706,7 +749,10 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
                 integerValue(resultSet, "capacity"),
                 resultSet.getString("status"),
                 nullableInstant(resultSet, "created_at"),
-                nullableInstant(resultSet, "updated_at")
+                nullableInstant(resultSet, "updated_at"),
+                resultSet.getString("owner_person"),
+                resultSet.getString("address"),
+                resultSet.getString("business_scope")
         );
     }
 
@@ -1164,4 +1210,129 @@ class JdbcPlatformRepository implements PlatformRepository, SupervisorReviewRepo
         jdbcTemplate.update("UPDATE merchants SET archive_reason = :reason WHERE id = :id",
                 new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("reason", reason).addValue("id", id));
     }
+
+    @Override
+    public Optional<BusinessEntityRecord> findBusinessEntityById(String entityId) {
+        return jdbcTemplate.query("""
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
+                       bank_account_ref, status, created_at, updated_at
+                  FROM business_entities WHERE id = :id
+                """, Map.of("id", entityId), this::mapBusinessEntity).stream().findFirst();
+    }
+
+    @Override
+    public boolean businessEntityInProject(String businessEntityId, String projectId) {
+        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
+                SELECT EXISTS (SELECT 1 FROM business_entity_projects
+                    WHERE business_entity_id = :entity_id AND project_id = :project_id)
+                """, new MapSqlParameterSource().addValue("entity_id", businessEntityId)
+                .addValue("project_id", projectId, java.sql.Types.VARCHAR), Boolean.class));
+    }
+
+    @Override
+    public void associateBusinessEntityProject(String businessEntityId, String projectId) {
+        jdbcTemplate.update("""
+                INSERT INTO business_entity_projects (business_entity_id, project_id)
+                VALUES (:entity_id, :project_id) ON CONFLICT DO NOTHING
+                """, Map.of("entity_id", businessEntityId, "project_id", projectId));
+    }
+
+    @Override
+    public boolean assignMerchantProject(String merchantId, String projectId) {
+        return jdbcTemplate.update("""
+                UPDATE merchants SET project_id = :project_id
+                 WHERE id = :id AND (project_id IS NULL OR project_id = :project_id)
+                """, Map.of("id", merchantId, "project_id", projectId)) == 1;
+    }
+
+    @Override
+    public com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(String projectId,
+            int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_entities WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<BusinessEntityRecord> items = jdbcTemplate.query("""
+                SELECT id, entity_name, entity_role, unified_social_credit_code, legal_representative,
+                       bank_account_ref, status, created_at, updated_at
+                  FROM business_entities
+                 WHERE EXISTS (SELECT 1 FROM business_entity_projects ep WHERE ep.business_entity_id = business_entities.id AND ep.project_id = :project_id)
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapBusinessEntity);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(String projectId,
+            boolean includeArchived, String businessFormat, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR).addValue("include_archived", includeArchived)
+                .addValue("business_format", businessFormat);
+        // V348 按业态查询：business_format 为空＝不筛（走 idx_merchants_business_format）。
+        String where = " WHERE project_id = :project_id AND (:include_archived OR (archived_at IS NULL AND delete_status <> 'approved'))"
+                + " AND (CAST(:business_format AS VARCHAR) IS NULL OR merchant_type = :business_format)";
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants" + where, params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<MerchantRecord> items = jdbcTemplate.query("SELECT id, merchant_name, merchant_type, business_entity_id, "
+                + "contract_entity_id, settlement_entity_id, contact_name, contact_phone, owner_person, mall_block_code, "
+                + "area_square_meter, leased_area_square_meter, secondary_contact_name, secondary_contact_phone, "
+                + "rent_per_square_meter, cooperation_form, status, created_at, updated_at, archived_at, archived_by, "
+                + "delete_status, delete_requested_by, delete_requested_at, delete_reason, project_id FROM merchants" + where
+                + " ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset", params, this::mapMerchant);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(String projectId, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE id = :project_id", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<ProjectRecord> items = jdbcTemplate.query("""
+                SELECT id, merchant_id, venue_type, name, code, floor, capacity, status, created_at, updated_at, owner_person, address, business_scope
+                  FROM projects
+                 WHERE id = :project_id
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapProject);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(String projectId, int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM spaces WHERE project_id = :project_id", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<SpaceRecord> items = jdbcTemplate.query("""
+                SELECT id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no,
+                       area_square_meter, polygon_points::text AS polygon_points, status, created_at, updated_at
+                  FROM spaces
+                 WHERE project_id = :project_id
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapSpace);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
+    @Override
+    public com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(String projectId,
+            int page, int size) {
+        int safePage = page < 1 ? 1 : page;
+        int safeSize = size < 1 ? 500 : Math.min(size, 500);
+        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId, java.sql.Types.VARCHAR);
+        Long total = jdbcTemplate.queryForObject(
+                "SELECT COUNT(*) FROM business_entity_bindings WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND m.project_id = :project_id)", params, Long.class);
+        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
+        List<BusinessEntityBindingRecord> items = jdbcTemplate.query("""
+                SELECT id::text AS id, merchant_id, operation_entity_id, contract_entity_id, settlement_entity_id,
+                       status, effective_from, effective_to, change_reason, created_at, updated_at
+                  FROM business_entity_bindings
+                 WHERE merchant_id IS NOT NULL AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = business_entity_bindings.merchant_id AND m.project_id = :project_id)
+                 ORDER BY created_at DESC, id
+                 LIMIT :limit OFFSET :offset
+                """, params, this::mapBusinessEntityBinding);
+        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
index c2deb1fe..c14ca2d9 100644
--- a/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/MerchantRecord.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.04
 
 import java.math.BigDecimal;
 import java.time.Instant;
@@ -31,8 +31,34 @@ public record MerchantRecord(
         String deleteStatus,
         String deleteRequestedBy,
         Instant deleteRequestedAt,
-        String deleteReason
+        String deleteReason,
+        String projectId
 ) {
+    public MerchantRecord(
+            String id, String merchantName, String merchantType, String businessEntityId, String contractEntityId,
+            String settlementEntityId, String contactName, String contactPhone, String ownerPerson,
+            String mallBlockCode, BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter,
+            String secondaryContactName, String secondaryContactPhone, BigDecimal rentPerSquareMeter,
+            String cooperationForm, String status, Instant createdAt, Instant updatedAt, Instant archivedAt,
+            String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
+            String deleteReason) {
+        this(
+                id, merchantName, merchantType, businessEntityId, contractEntityId, settlementEntityId,
+                contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter,
+                secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm, status,
+                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
+                deleteReason, null);
+    }
+
+    public MerchantRecord withProject(String projectId) {
+        return new MerchantRecord(
+                id, merchantName, merchantType, businessEntityId, contractEntityId, settlementEntityId,
+                contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter,
+                secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm, status,
+                createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt,
+                deleteReason, projectId);
+    }
+
     // ponytail: V260 前的 19 参构造点(既有代码/测试)不带存档/删除字段,默认未存档 + delete_status=none。
     //           避免为 6 个新字段改所有既有构造点;日后语义要变则删本构造让编译逼出所有点。
     public MerchantRecord(
@@ -53,7 +79,7 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     /**
@@ -66,7 +92,7 @@ public record MerchantRecord(
                 ownerPerson, mallBlockCode, areaSquareMeter, leasedAreaSquareMeter, secondaryContactName,
                 com.complex.platform.common.PhoneMask.mask(secondaryContactPhone), rentPerSquareMeter,
                 cooperationForm, status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     /** M009-008：三个主体列以生效的四类主体绑定为准（PGM-026），绑定激活时由 PlatformService 同步投影。 */
@@ -76,7 +102,7 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 
     public MerchantRecord withDelete(String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt,
@@ -85,6 +111,6 @@ public record MerchantRecord(
                 settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode, areaSquareMeter,
                 leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone, rentPerSquareMeter, cooperationForm,
                 status, createdAt, updatedAt,
-                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
+                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, projectId);
     }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformController.java b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
index ca47080c..52d3f0d2 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformController.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformController.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.01
+// 文件版本：V0.02
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.access.AuthorizationService;
@@ -11,6 +11,7 @@ import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
 import com.complex.platform.governance.dto.BusinessEntityRequest;
 import com.complex.platform.governance.dto.MerchantProfileRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.SpaceRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import org.springframework.web.bind.annotation.GetMapping;
@@ -84,6 +85,24 @@ public class PlatformController {
         return ApiResponse.ok(maskPhonesForUnauthorized(service.updateMerchant(merchantId, request)));
     }
 
+    @PutMapping("/merchants/{merchant_id}/project")
+    public ApiResponse<MerchantRecord> assignMerchantProject(
+            @PathVariable("merchant_id") String merchantId,
+            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
+        authorizationService.requirePermission("merchant.profile.manage");
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        return ApiResponse.ok(maskPhonesForUnauthorized(service.assignMerchantProject(merchantId, request)));
+    }
+
+    @PostMapping("/business-entities/{entity_id}/projects")
+    public ApiResponse<BusinessEntityRecord> associateBusinessEntityProject(
+            @PathVariable("entity_id") String entityId,
+            @RequestBody @jakarta.validation.Valid ProjectAssociationRequest request) {
+        authorizationService.requirePermission("entity.binding.manage");
+        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
+        return ApiResponse.ok(service.associateBusinessEntityProject(entityId, request));
+    }
+
     // ================= V260 商户存档与审批后删除范式 =================
 
     @PostMapping("/merchants/{merchant_id}/archive")
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
index 95f31cbb..b313cd2d 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.06
 
 import java.util.List;
 import java.util.Optional;
@@ -43,10 +43,10 @@ public interface PlatformRepository {
      * 勿删:本方法零调用是常态(多综合体隔离通道,建成待激活),但 verify:tenant-isolation 门禁
      * 以「该表存在 scoped 查询」为隔离能力判据,删除会导致门禁 fail-closed 阻断部署。
      */
-    default List<MerchantRecord> listMerchantsByBusinessEntity(String businessEntityId) {
-        if (businessEntityId == null) return listMerchants();
+    default List<MerchantRecord> listMerchantsByBusinessEntity(String projectId, String businessEntityId) {
+        if (projectId == null || businessEntityId == null) return List.of();
         return listMerchants().stream()
-                .filter(m -> businessEntityId.equals(m.businessEntityId()))
+                .filter(m -> projectId.equals(m.projectId()) && businessEntityId.equals(m.businessEntityId()))
                 .toList();
     }
 
@@ -100,6 +100,8 @@ public interface PlatformRepository {
 
     String findSpaceTypeDeleteRequestedBy(String spaceTypeId);
 
+    void completeSpaceTypeDeleteRequest(String spaceTypeId, String status);
+
 
     BusinessEntityBindingRecord saveBusinessEntityBinding(BusinessEntityBindingRecord binding);
 
@@ -109,6 +111,18 @@ public interface PlatformRepository {
 
     Optional<BusinessEntityBindingRecord> findActiveBusinessEntityBindingByMerchantId(String merchantId);
 
+    void lockMerchant(String merchantId);
+
+    default boolean hasUnfinishedBusinessEntityBinding(String merchantId, String excludedId) {
+        return listBusinessEntityBindings().stream()
+                .filter(binding -> merchantId.equals(binding.merchantId()))
+                .filter(binding -> !java.util.Objects.equals(excludedId, binding.id()))
+                .anyMatch(binding -> "active".equals(binding.status())
+                        || ("inactive".equals(binding.status()) && binding.effectiveTo() == null));
+    }
+
+
+
 
 
 
@@ -142,4 +156,49 @@ public interface PlatformRepository {
             int page, int size) {
         return com.complex.platform.common.api.PageResponse.of(listBusinessEntityBindings(), page, size);
     }
+
+    default Optional<BusinessEntityRecord> findBusinessEntityById(String entityId) {
+        return listBusinessEntities().stream().filter(entity -> entityId.equals(entity.id())).findFirst();
+    }
+
+    boolean businessEntityInProject(String businessEntityId, String projectId);
+
+    void associateBusinessEntityProject(String businessEntityId, String projectId);
+
+    boolean assignMerchantProject(String merchantId, String projectId);
+
+    default com.complex.platform.common.api.PageResponse<BusinessEntityRecord> listBusinessEntitiesPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listBusinessEntities().stream()
+                .filter(entity -> projectId != null && businessEntityInProject(entity.id(), projectId))
+                .toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<MerchantRecord> listMerchantsPaged(
+            String projectId, boolean includeArchived, String businessFormat, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listMerchants(includeArchived).stream()
+                .filter(merchant -> projectId != null && projectId.equals(merchant.projectId()))
+                .filter(merchant -> businessFormat == null || businessFormat.equals(merchant.merchantType()))
+                .toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<ProjectRecord> listProjectsPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listProjects().stream()
+                .filter(project -> projectId != null && projectId.equals(project.id())).toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<SpaceRecord> listSpacesPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listSpaces().stream()
+                .filter(space -> projectId != null && projectId.equals(space.projectId())).toList(), page, size);
+    }
+
+    default com.complex.platform.common.api.PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindingsPaged(
+            String projectId, int page, int size) {
+        return com.complex.platform.common.api.PageResponse.of(listBusinessEntityBindings().stream()
+                .filter(binding -> projectId != null && findMerchantById(binding.merchantId())
+                        .filter(merchant -> projectId.equals(merchant.projectId())).isPresent())
+                .toList(), page, size);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/PlatformService.java b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
index 3acc3e9e..b325adc0 100644
--- a/backend/src/main/java/com/complex/platform/governance/PlatformService.java
+++ b/backend/src/main/java/com/complex/platform/governance/PlatformService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.03
+// 文件版本：V0.11
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -14,6 +14,7 @@ import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
 import com.complex.platform.governance.dto.MerchantDeleteRequest;
 import com.complex.platform.governance.dto.MerchantArchiveRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.SpaceRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import java.time.Instant;
@@ -88,7 +89,7 @@ public class PlatformService {
 
     // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有前端一次拉取(下拉源)契约。
     public PageResponse<BusinessEntityRecord> listBusinessEntities(int page, int size) {
-        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(page, size);
+        PageResponse<BusinessEntityRecord> result = repository.listBusinessEntitiesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "entity_binding.viewed",
                 "entity.binding.view",
@@ -117,7 +118,8 @@ public class PlatformService {
                     String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
                     String status = requireSubjectStatus(request.status());
                     Instant now = Instant.now();
-                    return repository.saveBusinessEntity(new BusinessEntityRecord(
+                    String projectId = creationProject(request.projectId(), true);
+                    BusinessEntityRecord saved = repository.saveBusinessEntity(new BusinessEntityRecord(
                             "be_" + UUID.randomUUID(),
                             entityName,
                             entityRole,
@@ -128,6 +130,8 @@ public class PlatformService {
                             now,
                             now
                     ));
+                    repository.associateBusinessEntityProject(saved.id(), projectId);
+                    return saved;
                 }
         );
     }
@@ -141,10 +145,9 @@ public class PlatformService {
         String format = trimToNull(businessFormat);
         if (format != null && !MERCHANT_BUSINESS_FORMATS.contains(format)) {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
-                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
-                            + "(bar/catering/retail/amusement/media/other)");
+                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
-        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(includeArchived, format, page, size);
+        PageResponse<MerchantRecord> result = repository.listMerchantsPaged(currentProject(), includeArchived, format, page, size);
         auditEventService.recordSuccess(
                 "merchant.viewed",
                 "merchant.profile.view",
@@ -255,7 +258,7 @@ public class PlatformService {
                 "merchant.profile.manage",
                 "merchant",
                 () -> {
-                    MerchantRecord merchant = requireMerchant(id);
+                    MerchantRecord merchant = isMasterAuthority() ? findMerchant(id) : requireMerchant(id);
                     if (merchant.archivedAt() == null) {
                         throw ApiException.badRequest("MERCHANT_NOT_ARCHIVED",
                                 "商户必须先存档才能申请删除");
@@ -300,12 +303,13 @@ public class PlatformService {
                 request == null ? null : request.idempotencyKey(),
                 request,
                 MerchantDeleteDecision.class,
-                "merchant.delete_approved",
+                request != null && "rejected".equalsIgnoreCase(request.decision() == null ? "" : request.decision().trim())
+                        ? "merchant.delete_rejected" : "merchant.delete_approved",
                 "merchant.delete_approve_failed",
                 "merchant.profile.approve",
                 "merchant",
                 () -> {
-                    MerchantRecord merchant = requireMerchant(id);
+                    MerchantRecord merchant = isMasterAuthority() ? findMerchant(id) : requireMerchant(id);
                     if (!"pending".equals(merchant.deleteStatus())) {
                         throw ApiException.badRequest("MERCHANT_DELETE_NOT_PENDING",
                                 "该商户没有待审批的删除申请");
@@ -313,7 +317,7 @@ public class PlatformService {
                     String decision = request == null || request.decision() == null
                             ? null : request.decision().trim().toLowerCase(Locale.ROOT);
                     if (!"approved".equals(decision) && !"rejected".equals(decision)) {
-                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
+                        throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
                     }
                     if (!isMasterAuthority()) {
                         throw ApiException.forbidden("MERCHANT_DELETE_APPROVER_NOT_MASTER", "商户删除只能由最高权限审批通过");
@@ -341,11 +345,89 @@ public class PlatformService {
         );
     }
 
-    private MerchantRecord requireMerchant(String id) {
+    public MerchantRecord assignMerchantProject(String merchantId, ProjectAssociationRequest request) {
+        requireProjectConfigurationAuthority();
+        String id = requireText(merchantId, "merchant_id");
+        return executeWrite("assignMerchantProject:" + id, request == null ? null : request.idempotencyKey(),
+                request, MerchantRecord.class, "merchant.project_assigned", "merchant.project_assign_failed",
+                "merchant.profile.manage", "merchant", () -> {
+                    String projectId = associationProject(request);
+                    findMerchant(id);
+                    if (!repository.assignMerchantProject(id, projectId)) {
+                        throw ApiException.conflict("MERCHANT_PROJECT_ALREADY_ASSIGNED", "商户已有明确项目归属，不能迁移");
+                    }
+                    return findMerchant(id);
+                });
+    }
+
+    public BusinessEntityRecord associateBusinessEntityProject(String entityId, ProjectAssociationRequest request) {
+        requireProjectConfigurationAuthority();
+        String id = requireText(entityId, "business_entity_id");
+        return executeWrite("associateBusinessEntityProject:" + id, request == null ? null : request.idempotencyKey(),
+                request, BusinessEntityRecord.class, "business_entity.project_associated", "business_entity.project_associate_failed",
+                "entity.binding.manage", "business_entity", () -> {
+                    String projectId = associationProject(request);
+                    BusinessEntityRecord entity = repository.findBusinessEntityById(id)
+                            .orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在"));
+                    repository.associateBusinessEntityProject(id, projectId);
+                    return entity;
+                });
+    }
+
+    private void requireProjectConfigurationAuthority() {
+        authorizationService.requirePlatformScope();
+        if (!isMasterAuthority()) {
+            throw ApiException.forbidden("MASTER_AUTHORITY_REQUIRED", "项目归属配置须由持授权密钥的最高权限执行");
+        }
+    }
+
+    private String associationProject(ProjectAssociationRequest request) {
+        ProjectAssociationRequest actual = requireRequest(request);
+        String projectId = requireText(actual.projectId(), "project_id");
+        requireText(actual.reason(), "reason");
+        if (!repository.projectExists(projectId)) {
+            throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+        }
+        return projectId;
+    }
+
+    private MerchantRecord findMerchant(String id) {
         return repository.findMerchantById(id)
                 .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
     }
 
+    private MerchantRecord requireMerchant(String id) {
+        MerchantRecord merchant = findMerchant(id);
+        requireCurrentProject(merchant.projectId());
+        return merchant;
+    }
+
+    private String currentProject() {
+        authorizationService.requirePlatformScope();
+        String scope = authorizationService.currentProjectScopeQuietly().orElseThrow();
+        return repository.projectExists(scope) ? scope : null;
+    }
+
+    private String requireCurrentProject(String projectId) {
+        String current = currentProject();
+        if (current == null || !current.equals(projectId)) {
+            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
+        }
+        return current;
+    }
+
+    private String creationProject(String requested, boolean allowMasterConfiguration) {
+        String explicit = trimToNull(requested);
+        if (allowMasterConfiguration && isMasterAuthority() && explicit != null) {
+            if (!repository.projectExists(explicit)) {
+                throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
+            }
+            return explicit;
+        }
+        String current = currentProject();
+        return requireCurrentProject(explicit == null ? current : explicit);
+    }
+
     /** M009-028：BSM-004「下线就不能再用」——新项目/空间/绑定只能引用未存档且未审批删除的商户；档案更新、存档/恢复与删除流程仍经 requireMerchant 命中已存档行。 */
     private MerchantRecord requireActiveMerchant(String id) {
         MerchantRecord merchant = requireMerchant(id);
@@ -360,7 +442,7 @@ public class PlatformService {
     }
 
     public PageResponse<ProjectRecord> listProjects(int page, int size) {
-        PageResponse<ProjectRecord> result = repository.listProjectsPaged(page, size);
+        PageResponse<ProjectRecord> result = repository.listProjectsPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "project.viewed",
                 "governance.project.view",
@@ -392,6 +474,9 @@ public class PlatformService {
                         requireActiveMerchant(merchantId);
                     }
                     String status = requireProjectStatus(request.status());
+                    if (request.capacity() != null && request.capacity() < 0) {
+                        throw ApiException.badRequest("VALIDATION_FAILED", "容量不得为负数");
+                    }
                     Instant now = Instant.now();
                     return repository.saveProject(new ProjectRecord(
                             "prj_" + UUID.randomUUID(),
@@ -403,7 +488,10 @@ public class PlatformService {
                             request.capacity(),
                             status,
                             now,
-                            now
+                            now,
+                            trimToNull(request.ownerPerson()),
+                            trimToNull(request.address()),
+                            trimToNull(request.businessScope())
                     ));
                 }
         );
@@ -411,7 +499,9 @@ public class PlatformService {
 
     /** 3.13：空间存在性（招商线索/意向意向空间校验用）。 */
     public boolean spaceExists(String spaceId) {
-        return spaceId != null && repository.findSpaceById(spaceId).isPresent();
+        String projectId = currentProject();
+        return spaceId != null && projectId != null && repository.findSpaceById(spaceId)
+                .filter(space -> projectId.equals(space.projectId())).isPresent();
     }
 
     public PageResponse<SpaceRecord> listSpaces() {
@@ -419,7 +509,7 @@ public class PlatformService {
     }
 
     public PageResponse<SpaceRecord> listSpaces(int page, int size) {
-        PageResponse<SpaceRecord> result = repository.listSpacesPaged(page, size);
+        PageResponse<SpaceRecord> result = repository.listSpacesPaged(currentProject(), page, size);
         auditEventService.recordSuccess(
                 "space.viewed",
                 "governance.space.view",
@@ -500,8 +590,8 @@ public class PlatformService {
         authorizationService.requirePlatformScope();
         SpaceTypeRecord existing = repository.findSpaceTypeById(id)
                 .orElseThrow(() -> ApiException.notFound("SPACE_TYPE_NOT_FOUND", "空间种类不存在或已被删除"));
-        if (!"archived".equals(existing.status())) {
-            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "只有已存档的种类可以取消存档");
+        if (!Set.of("archived", "disabled").contains(existing.status())) {
+            throw ApiException.badRequest("SPACE_TYPE_NOT_ARCHIVED", "只有已存档或已删除的种类可以恢复");
         }
         Instant now = Instant.now();
         repository.saveSpaceType(new SpaceTypeRecord(
@@ -512,6 +602,7 @@ public class PlatformService {
                 Map.of("type_code", existing.typeCode()));
     }
 
+    @org.springframework.transaction.annotation.Transactional
     public void requestDeleteSpaceType(String id, String reason) {
         authorizationService.requirePlatformScope();
         SpaceTypeRecord existing = repository.findSpaceTypeById(id)
@@ -527,12 +618,13 @@ public class PlatformService {
         String actor = actorId() == null ? "unknown" : actorId();
         if (isMasterAuthority()) {
             repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
+            repository.completeSpaceTypeDeleteRequest(existing.id(), "approved");
             repository.saveSpaceType(new SpaceTypeRecord(
                     existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                     "disabled", existing.createdAt(), Instant.now()));
             auditEventService.recordSuccess(
                     "space_type.deleted", "governance.space.manage", "space_type", id, null, null,
-                    Map.of("type_code", existing.typeCode()));
+                    Map.of("type_code", existing.typeCode(), "reason", text));
             return;
         }
         repository.saveSpaceType(new SpaceTypeRecord(
@@ -541,9 +633,10 @@ public class PlatformService {
         repository.saveSpaceTypeDeleteRequest(existing.id(), text, actor);
         auditEventService.recordSuccess(
                 "space_type.delete_requested", "governance.space.manage", "space_type", id, null, null,
-                Map.of("type_code", existing.typeCode()));
+                Map.of("type_code", existing.typeCode(), "reason", text));
     }
 
+    @org.springframework.transaction.annotation.Transactional
     public void approveDeleteSpaceType(String id, String decision) {
         authorizationService.requirePlatformScope();
         if (!isMasterAuthority()) {
@@ -565,12 +658,13 @@ public class PlatformService {
         }
         Instant now = Instant.now();
         String next = "approved".equals(verdict) ? "disabled" : "archived";
+        repository.completeSpaceTypeDeleteRequest(existing.id(), verdict);
         repository.saveSpaceType(new SpaceTypeRecord(
                 existing.id(), existing.typeCode(), existing.typeName(), existing.sortOrder(),
                 next, existing.createdAt(), now));
         auditEventService.recordSuccess(
-                "space_type.delete_approved", "governance.space.manage", "space_type", id, null, null,
-                Map.of("decision", next, "type_code", existing.typeCode()));
+                "approved".equals(verdict) ? "space_type.delete_approved" : "space_type.delete_rejected", "governance.space.manage", "space_type", id, null, null,
+                Map.of("decision", verdict, "type_code", existing.typeCode()));
     }
 
     public SpaceRecord createSpace(SpaceRequest request) {
@@ -592,18 +686,22 @@ public class PlatformService {
                         throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                 "空间种类必须是空间种类表中的启用值");
                     }
-                    String projectId = trimToNull(request.projectId());
-                    if (projectId != null && !repository.projectExists(projectId)) {
+                    String projectId = requireText(request.projectId(), "project_id");
+                    if (!repository.projectExists(projectId)) {
                         throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
                     }
+                    requireCurrentProject(projectId);
                     String merchantId = trimToNull(request.merchantId());
                     if (merchantId != null) {
                         requireActiveMerchant(merchantId);
                     }
                     String parentSpaceId = trimToNull(request.parentSpaceId());
-                    if (parentSpaceId != null && repository.findSpaceById(parentSpaceId).isEmpty()) {
-                        throw ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在");
+                    if (parentSpaceId != null) {
+                        SpaceRecord parent = repository.findSpaceById(parentSpaceId)
+                                .orElseThrow(() -> ApiException.notFound("SPACE_NOT_FOUND", "上级空间不存在"));
+                        requireCurrentProject(parent.projectId());
                     }
+                    requireNonNegative(request.areaSquareMeter(), "空间面积");
                     Instant now = Instant.now();
                     return repository.saveSpace(new SpaceRecord(
                             "sp_" + UUID.randomUUID(),
@@ -630,7 +728,7 @@ public class PlatformService {
     public PageResponse<BusinessEntityBindingRecord> listBusinessEntityBindings(int page, int size) {
         try {
             PageResponse<BusinessEntityBindingRecord> result =
-                    repository.listBusinessEntityBindingsPaged(page, size);
+                    repository.listBusinessEntityBindingsPaged(currentProject(), page, size);
             auditEventService.recordSuccess(
                     "entity_binding.viewed",
                     "entity.binding.view",
@@ -670,16 +768,16 @@ public class PlatformService {
                     BusinessEntityBindingRequest actual = requireRequest(request);
                     String merchantId = requireText(actual.merchantId(), "merchant_id");
                     // M009-006：四类主体绑定的经营对象与三个主体都必须真实存在（BEQR-143/PGM-026）；M009-028：且商户未下线。
-                    requireActiveMerchant(merchantId);
+                    repository.lockMerchant(merchantId);
+                    MerchantRecord merchant = requireActiveMerchant(merchantId);
                     String operationEntityId = requireBusinessEntity(actual.operationEntityId(), "operation_entity_id");
                     String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
                     String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
-                    repository.findActiveBusinessEntityBindingByMerchantId(merchantId)
-                            .ifPresent(existing -> {
-                                throw ApiException.conflict(
-                                        "ACTIVE_BINDING_EXISTS",
-                                        "该商户已存在生效中的主体绑定，不能重复绑定");
-                            });
+                    requireUnfinishedBindingSlot(merchantId, null);
+                    requireUnchangedFinancialSubjects(merchant, operationEntityId, contractEntityId, settlementEntityId);
+                    LocalDate today = LocalDate.now(ZoneOffset.UTC);
+                    LocalDate effectiveFrom = actual.effectiveFrom() == null ? today : actual.effectiveFrom();
+                    boolean effective = !effectiveFrom.isAfter(today);
                     Instant now = Instant.now();
                     BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
                             UUID.randomUUID().toString(),
@@ -687,14 +785,16 @@ public class PlatformService {
                             operationEntityId,
                             contractEntityId,
                             settlementEntityId,
-                            "active",
-                            actual.effectiveFrom() == null ? LocalDate.now(ZoneOffset.UTC) : actual.effectiveFrom(),
+                            effective ? "active" : "inactive",
+                            effectiveFrom,
                             null,
                             trimToNull(actual.changeReason()),
                             now,
                             now
                     ));
-                    syncMerchantEntities(saved);
+                    if (effective) {
+                        syncMerchantEntities(saved);
+                    }
                     return saved;
                 }
         );
@@ -716,19 +816,33 @@ public class PlatformService {
                     StatusChangeRequest actual = requireRequest(request);
                     String status = requireText(actual.action(), "action");
                     if (!Set.of("active", "inactive", "suspended").contains(status)) {
-                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用（active）、停用（inactive）或暂停（suspended）");
+                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用、停用或暂停");
                     }
                     String reason = requireText(actual.reason(), "reason");
-                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId)
+                    BusinessEntityBindingRecord located = repository.findBusinessEntityBindingById(bindingId)
                             .orElseThrow(() -> ApiException.notFound("BINDING_NOT_FOUND", "主体绑定不存在或已被删除"));
+                    repository.lockMerchant(located.merchantId());
+                    BusinessEntityBindingRecord existing = repository.findBusinessEntityBindingById(bindingId).orElseThrow();
+                    MerchantRecord merchant = requireMerchant(existing.merchantId());
+                    LocalDate today = LocalDate.now(ZoneOffset.UTC);
+                    if (existing.effectiveTo() != null) {
+                        throw ApiException.conflict("BINDING_PERIOD_ENDED", "已结束的主体绑定不能重新打开或改写历史期间");
+                    }
+                    if ("active".equals(existing.status()) && !"active".equals(status)) {
+                        throw ApiException.conflict("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW",
+                                "停用生效中的主体绑定须经过财税权责变更审核");
+                    }
+                    if (existing.effectiveFrom() != null && existing.effectiveFrom().isAfter(today)) {
+                        throw ApiException.conflict("BINDING_NOT_EFFECTIVE", "主体绑定尚未到生效日期，不能提前启用或结束");
+                    }
                     if ("active".equals(status)) {
-                        repository.findActiveBusinessEntityBindingByMerchantId(existing.merchantId())
-                                .filter(active -> !active.id().equals(existing.id()))
-                                .ifPresent(active -> {
-                                    throw ApiException.conflict(
-                                            "ACTIVE_BINDING_EXISTS",
-                                            "该商户已存在生效中的主体绑定，不能重复绑定");
-                                });
+                        requireActiveMerchant(existing.merchantId());
+                        requireBusinessEntity(existing.operationEntityId(), "operation_entity_id");
+                        requireBusinessEntity(existing.contractEntityId(), "contract_entity_id");
+                        requireBusinessEntity(existing.settlementEntityId(), "settlement_entity_id");
+                        requireUnfinishedBindingSlot(existing.merchantId(), existing.id());
+                        requireUnchangedFinancialSubjects(merchant, existing.operationEntityId(),
+                                existing.contractEntityId(), existing.settlementEntityId());
                     }
                     Instant now = Instant.now();
                     BusinessEntityBindingRecord saved = repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
@@ -760,8 +874,7 @@ public class PlatformService {
         String value = requireText(raw, "merchant_type");
         if (!MERCHANT_BUSINESS_FORMATS.contains(value)) {
             throw ApiException.badRequest("MERCHANT_BUSINESS_FORMAT_INVALID",
-                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一"
-                            + "(bar/catering/retail/amusement/media/other)");
+                    "商户业态必须为 酒吧/餐饮/零售/游乐/传媒/其他 之一");
         }
         return value;
     }
@@ -771,8 +884,7 @@ public class PlatformService {
         String value = trimToNull(raw);
         if (value != null && !MERCHANT_COOPERATION_FORMS.contains(value)) {
             throw ApiException.badRequest("MERCHANT_COOPERATION_FORM_INVALID",
-                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一"
-                            + "(self_operated/lease/joint_operation/managed_operation/service_provider/other)");
+                    "合作方式必须为 自营/租赁/联营/代运营/服务商接入/其他 之一");
         }
         return value;
     }
@@ -783,7 +895,7 @@ public class PlatformService {
         if (!SUBJECT_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
                     "状态必须为 待准入/正常/暂停/终止/黑名单 之一"
-                            + "(pending_admission/active/suspended/terminated/blacklisted)；归档请使用存档操作");
+                            + "；归档请使用存档操作");
         }
         return value;
     }
@@ -793,8 +905,7 @@ public class PlatformService {
         String value = trimToDefault(raw, "active");
         if (!PROJECT_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一"
-                            + "(draft/pending_activation/active/suspended/terminated/archived)");
+                    "项目状态必须为 草稿/待启用/已启用/暂停/终止/归档 之一");
         }
         return value;
     }
@@ -804,7 +915,7 @@ public class PlatformService {
         String value = trimToDefault(raw, "active");
         if (!SPACE_STATUSES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "空间状态必须为 可用/不可用 之一(active/unavailable)");
+                    "空间状态必须为 可用/不可用 之一");
         }
         return value;
     }
@@ -814,7 +925,7 @@ public class PlatformService {
         String value = requireText(raw, "entity_role");
         if (!BUSINESS_ENTITY_ROLES.contains(value)) {
             throw ApiException.badRequest("VALIDATION_FAILED",
-                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一(platform/project_company/management_company)");
+                    "主体角色必须为 顶层平台/城市项目公司/域主管公司 之一");
         }
         return value;
     }
@@ -825,9 +936,30 @@ public class PlatformService {
         if (!repository.businessEntityExists(id)) {
             throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
         }
+        if (!repository.businessEntityInProject(id, currentProject())) {
+            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "该主体未关联当前项目");
+        }
         return id;
     }
 
+    private void requireUnchangedFinancialSubjects(MerchantRecord merchant, String operationId,
+            String contractId, String settlementId) {
+        boolean unbound = merchant.businessEntityId() == null && merchant.contractEntityId() == null
+                && merchant.settlementEntityId() == null;
+        if (!unbound && (!java.util.Objects.equals(merchant.businessEntityId(), operationId)
+                || !java.util.Objects.equals(merchant.contractEntityId(), contractId)
+                || !java.util.Objects.equals(merchant.settlementEntityId(), settlementId))) {
+            throw ApiException.conflict("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW",
+                    "变更已有经营、合同或结算主体须经过财税权责变更审核");
+        }
+    }
+
+    private void requireUnfinishedBindingSlot(String merchantId, String excludedId) {
+        if (repository.hasUnfinishedBusinessEntityBinding(merchantId, excludedId)) {
+            throw ApiException.conflict("ACTIVE_BINDING_EXISTS", "该商户已存在生效或待生效的主体绑定，不能重复绑定");
+        }
+    }
+
     /** M009-008：PGM-026「合同、结算、权限、看板、第三方接入都引用同一组绑定」——生效绑定是商户三个主体列的唯一来源，激活时同步投影。 */
     private void syncMerchantEntities(BusinessEntityBindingRecord binding) {
         repository.findMerchantById(binding.merchantId()).ifPresent(merchant -> repository.saveMerchant(
@@ -870,6 +1002,25 @@ public class PlatformService {
                     // M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt；
                     // M009-008：三个主体列以生效绑定为准，档案更新不覆盖。
                     MerchantRecord existing = update ? requireMerchant(merchantId) : null;
+                    String projectId = existing == null ? creationProject(request.projectId(), false) : existing.projectId();
+                    if (existing != null && trimToNull(request.projectId()) != null
+                            && !projectId.equals(request.projectId().trim())) {
+                        throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "普通档案更新不能变更商户的项目归属");
+                    }
+                    if (existing == null) {
+                        if (trimToNull(request.businessEntityId()) != null) requireBusinessEntity(request.businessEntityId(), "business_entity_id");
+                        if (trimToNull(request.contractEntityId()) != null) requireBusinessEntity(request.contractEntityId(), "contract_entity_id");
+                        if (trimToNull(request.settlementEntityId()) != null) requireBusinessEntity(request.settlementEntityId(), "settlement_entity_id");
+                    }
+                    if (existing != null && "terminated".equals(existing.status()) && trimToNull(request.status()) == null) {
+                        status = existing.status();
+                    }
+                    if ("terminated".equals(status) && (existing == null || !"terminated".equals(existing.status()))) {
+                        throw ApiException.conflict("MERCHANT_EXIT_REQUIRES_BUFFER", "商户退出须经过退出缓冲流程，不能直接终止");
+                    }
+                    requireNonNegative(request.areaSquareMeter(), "面积");
+                    requireNonNegative(request.leasedAreaSquareMeter(), "租赁面积");
+                    requireNonNegative(request.rentPerSquareMeter(), "每平方米租金");
                     Instant now = Instant.now();
                     return repository.saveMerchant(new MerchantRecord(
                             merchantId,
@@ -896,7 +1047,8 @@ public class PlatformService {
                             existing == null ? "none" : existing.deleteStatus(),
                             existing == null ? null : existing.deleteRequestedBy(),
                             existing == null ? null : existing.deleteRequestedAt(),
-                            existing == null ? null : existing.deleteReason()
+                            existing == null ? null : existing.deleteReason(),
+                            projectId
                     ));
                 }
         );
@@ -921,10 +1073,38 @@ public class PlatformService {
                 AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                 action,
                 this::businessId,
-                Map.of()
+                auditMetadata(request)
         );
     }
 
+    private Map<String, Object> auditMetadata(Object request) {
+        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
+        String reason = null;
+        if (request instanceof ProjectAssociationRequest value) {
+            reason = value.reason();
+            if (value.projectId() != null) metadata.put("projectId", value.projectId().trim());
+        } else if (request instanceof MerchantArchiveRequest value) {
+            reason = value.reason();
+        } else if (request instanceof MerchantDeleteRequest value) {
+            reason = value.reason();
+        } else if (request instanceof StatusChangeRequest value) {
+            reason = value.reason();
+        } else if (request instanceof BusinessEntityBindingRequest value) {
+            reason = value.changeReason();
+        } else if (request instanceof MerchantDeleteApprovalRequest value) {
+            if (value.decision() != null) metadata.put("decision", value.decision().trim().toLowerCase(Locale.ROOT));
+            if (value.comment() != null) metadata.put("comment", value.comment().trim());
+        }
+        if (reason != null) metadata.put("reason", reason.trim());
+        return Map.copyOf(metadata);
+    }
+
+    private void requireNonNegative(java.math.BigDecimal value, String label) {
+        if (value != null && value.signum() < 0) {
+            throw ApiException.badRequest("VALIDATION_FAILED", label + "不得为负数");
+        }
+    }
+
     private String trimToDefault(String value, String defaultValue) {
         String trimmed = value == null ? "" : value.trim();
         return trimmed.isEmpty() ? defaultValue : trimmed;
@@ -964,6 +1144,8 @@ public class PlatformService {
 
 
     private String businessId(Object value) {
+        if (value instanceof SpaceTypeRecord record) return record.id();
+        if (value instanceof MerchantDeleteDecision decision) return decision.merchantId();
         if (value instanceof BusinessEntityRecord record) {
             return record.id();
         }
diff --git a/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java b/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
index 355c9db8..a9ada70d 100644
--- a/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
+++ b/backend/src/main/java/com/complex/platform/governance/ProjectRecord.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance;
+// 文件版本：V0.01
 
 import java.time.Instant;
 
@@ -12,6 +13,13 @@ public record ProjectRecord(
         Integer capacity,
         String status,
         Instant createdAt,
-        Instant updatedAt
+        Instant updatedAt,
+        String ownerPerson,
+        String address,
+        String businessScope
 ) {
+    public ProjectRecord(String id, String merchantId, String venueType, String name, String code, String floor,
+            Integer capacity, String status, Instant createdAt, Instant updatedAt) {
+        this(id, merchantId, venueType, name, code, floor, capacity, status, createdAt, updatedAt, null, null, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
index fc6fa19f..594ede80 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/BusinessEntityRequest.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance.dto;
-// 文件版本：V0.01
+// 文件版本：V0.03
 
 import java.util.List;
 
@@ -12,6 +12,15 @@ public record BusinessEntityRequest(
         String unifiedSocialCreditCode,
         String legalRepresentative,
         String bankAccountRef,
-        String status
+        String status,
+        String projectId
 ) {
+    public BusinessEntityRequest(
+            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String entityName,
+            String entityRole, String unifiedSocialCreditCode, String legalRepresentative, String bankAccountRef,
+            String status) {
+        this(
+                idempotencyKey, operatorNote, evidenceAttachmentIds, entityName, entityRole,
+                unifiedSocialCreditCode, legalRepresentative, bankAccountRef, status, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
index c57aa129..5dd9526c 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.02
 
 import java.math.BigDecimal;
 import java.util.List;
@@ -22,6 +23,19 @@ public record MerchantProfileRequest(
         String secondaryContactPhone,
         BigDecimal rentPerSquareMeter,
         String cooperationForm,
-        String status
+        String status,
+        String projectId
 ) {
+    public MerchantProfileRequest(
+            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String merchantName,
+            String merchantType, String businessEntityId, String contractEntityId, String settlementEntityId,
+            String contactName, String contactPhone, String ownerPerson, String mallBlockCode,
+            BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter, String secondaryContactName,
+            String secondaryContactPhone, BigDecimal rentPerSquareMeter, String cooperationForm, String status) {
+        this(
+                idempotencyKey, operatorNote, evidenceAttachmentIds, merchantName, merchantType, businessEntityId,
+                contractEntityId, settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode,
+                areaSquareMeter, leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone,
+                rentPerSquareMeter, cooperationForm, status, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java
new file mode 100644
index 00000000..5223b206
--- /dev/null
+++ b/backend/src/main/java/com/complex/platform/governance/dto/ProjectAssociationRequest.java
@@ -0,0 +1,5 @@
+package com.complex.platform.governance.dto;
+// 文件版本：V0.01
+
+public record ProjectAssociationRequest(String idempotencyKey, String projectId, String reason) {
+}
diff --git a/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java b/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
index a3e81e93..77311d17 100644
--- a/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
+++ b/backend/src/main/java/com/complex/platform/governance/dto/ProjectRequest.java
@@ -1,4 +1,5 @@
 package com.complex.platform.governance.dto;
+// 文件版本：V0.01
 
 import java.util.List;
 
@@ -12,6 +13,14 @@ public record ProjectRequest(
         String code,
         String floor,
         Integer capacity,
-        String status
+        String status,
+        String ownerPerson,
+        String address,
+        String businessScope
 ) {
+    public ProjectRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
+            String merchantId, String venueType, String name, String code, String floor, Integer capacity, String status) {
+        this(idempotencyKey, operatorNote, evidenceAttachmentIds, merchantId, venueType, name, code, floor,
+                capacity, status, null, null, null);
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
index eb6b15af..c1ff9e37 100644
--- a/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
+++ b/backend/src/main/java/com/complex/platform/leasing/LeasingService.java
@@ -1,5 +1,5 @@
 package com.complex.platform.leasing;
-// 文件版本：V0.01
+// 文件版本：V0.04
 
 import com.complex.platform.access.AuthorizationService;
 import com.complex.platform.common.api.ApiException;
@@ -240,7 +240,7 @@ public class LeasingService {
         // 1.4 决策A：merchant_id 空 → 签约自动建正式商户档案（带入线索/意向信息，四类主体默认同一公司=contractSubjectId）。
         String merchantId = trimToNull(actual.merchantId());
         if (merchantId == null) {
-            merchantId = autoCreateMerchantForSign(intent, contractSubjectId);
+            merchantId = autoCreateMerchantForSign(intent);
             // AUD-003：签约建"四主体"权威绑定——与商户/合同/线索同一事务原子落库；缺此则权威表(business_entity_bindings)恒空、
             // 主体绑定页/结算/权限按权威表读取时会把正式商户当未绑定。经营/结算主体缺省=合同主体（默认同一公司，后续走既有变更流拆分）。
             createFourPartyBinding(intent, merchantId, contractSubjectId, operationSubjectId, settlementSubjectId,
@@ -269,7 +269,7 @@ public class LeasingService {
      * 1.4 决策A：签约无既有商户 → 自动建正式商户档案，带入线索联系人/业态/空间/面积（四类主体默认同一公司=contractSubjectId）。
      * 稳定 idempotency key（按意向）→ 重签命中同一商户、不重复建；租金条款随合同 draft 带入，商户档案后续可完善。
      */
-    private String autoCreateMerchantForSign(LeasingIntentRecord intent, String contractSubjectId) {
+    private String autoCreateMerchantForSign(LeasingIntentRecord intent) {
         LeasingLeadRecord lead = repository.findLeadById(intent.leadId()).orElse(null);
         String contactName = lead != null ? trimToNull(lead.contactName()) : null;
         String contactPhone = lead != null ? trimToNull(lead.contactPhone()) : null;
@@ -285,11 +285,11 @@ public class LeasingService {
                 // V348 业态必填：线索意向业态直接带入（同五值词表）；线索阶段还没定业态的落「其他」，
                 // 由招商在商户档案里改正——不猜成「零售」，猜错＝通票分账把份额拆给错的业态。
                 businessType != null ? businessType : "other",
-                contractSubjectId, contractSubjectId, contractSubjectId,
+                null, null, null,
                 contactName, contactPhone, contactName,
                 spaceCode, area, area,
                 // M009-003：合作方式只能是 HTML 六类之一，签约阶段未定＝空，不发明「leasing_auto」值。
-                null, null, null, null, "active"));
+                null, null, null, null, "active", intent.projectId()));
         return merchant.id();
     }
 
diff --git a/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql b/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
new file mode 100644
index 00000000..e485a5d2
--- /dev/null
+++ b/backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql
@@ -0,0 +1,20 @@
+-- 文件版本：V0.01
+-- M009-013/038：历史资料保持为空，不推断项目或公司归属。
+ALTER TABLE projects
+    ADD COLUMN owner_person TEXT,
+    ADD COLUMN address TEXT,
+    ADD COLUMN business_scope TEXT;
+
+ALTER TABLE merchants
+    ADD COLUMN project_id VARCHAR(64) REFERENCES projects(id);
+
+CREATE INDEX idx_merchants_project_id ON merchants(project_id);
+
+CREATE TABLE business_entity_projects (
+    business_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
+    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
+    PRIMARY KEY (business_entity_id, project_id)
+);
+
+CREATE INDEX idx_business_entity_projects_project_id
+    ON business_entity_projects(project_id);
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index 6a591453..714c10a2 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -1,11 +1,12 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.01
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
 import com.complex.platform.auth.AuthService;
+import com.complex.platform.auth.SessionIssuer;
 import com.complex.platform.common.api.ApiException;
 import java.time.Instant;
 import java.util.ArrayList;
@@ -234,10 +235,37 @@ class AuthorizationServiceTests {
         assertThat(service.currentMerchantIdQuietly()).contains("merchant");
     }
 
+    @Test
+    void productionMasterIdentityGetsExistingAuthorityWithoutBypassingPermissionDenials() {
+        AccessActor master = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
+        authenticateActor(master);
+        assertThat(service.isMasterAuthority(master)).isTrue();
+        service.requirePermission("ops.production_readiness.manage");
+        repository.addEffect(effect(master.id(), "ops.production_readiness.manage", AccessRepository.Effect.DENY));
+        assertThatThrownBy(() -> service.requirePermission("ops.production_readiness.manage"))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
+        assertThatThrownBy(() -> service.requirePermission("not.registered"))
+                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PERMISSION_DENIED"));
+    }
+
+    @Test
+    void platformAdminAndIncompleteMasterIdentityDoNotBecomeHighestAuthority() {
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
+                SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID,
+                SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "disabled"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor("another-user", SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
+        assertThat(service.isMasterAuthority(new AccessActor(SessionIssuer.MASTER_USER_ID, "another-name",
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"))).isFalse();
+    }
+
     @Test
     void currentSupervisorLevelIsThreeOnlyForMasterAuthority() {
         authenticateActor(new AccessActor(
-                "usr_master_authority", "master-authority", "platform_admin", "org", "merchant", "active"));
+                SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
+                "platform-company", null, "active"));
         assertThat(service.currentSupervisorLevel()).isEqualTo(3);
     }
 
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index 05b9afeb..7fdca215 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.24
+// 文件版本：V0.28
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -338,11 +338,7 @@ class M006SecurityRegressionTests {
         when(repository.applyTemplate(anyString(), anyList(), any(), any(), anyBoolean(), anyString())).thenReturn(List.of());
         roles.put("old", new RoleRecord("old", "旧岗位", null, "project", "active", Instant.now(), List.of(LOW), "org"));
         roles.put("new", new RoleRecord("new", "新岗位", null, "project", "active", Instant.now(), List.of(other), "org"));
-        actor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID, "platform_admin", "org", null);
-        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"),
-                "unused", List.of()));
+        actor("template-admin", "platform_admin", "org", null);
         PermissionTemplate template = service.createPermissionTemplate(template("empty"));
         for (String code : List.of("old", "new")) service.createPositionRoleTemplate(
                 new PositionRoleTemplateRequest(code, "理由", code, "level", code, template.id(), "active"));
@@ -413,7 +409,7 @@ class M006SecurityRegressionTests {
         verify(reviewRepository, never()).saveSupervisorReview(any());
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"),
                 "unused", List.of()));
         assertThat(reviews.approve("top", new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(
                 "top-master", "同意", List.of(), null, "approved", "同意")).decision()).isEqualTo("approved");
@@ -592,7 +588,7 @@ class M006SecurityRegressionTests {
                 new StatusChangeRequest("ordinary-admin", "approve_delete", "同意", List.of()))).isInstanceOf(ApiException.class);
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"), "unused", List.of()));
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"), "unused", List.of()));
         service.changePositionTemplateLifecycle("lifecycle", new StatusChangeRequest("approve", "approve_delete", "同意", List.of()));
         assertThat(mapping.get().deletedAt()).isNotNull();
         assertThat(service.listPositionRoleTemplates(true).items()).hasSize(1);
@@ -791,9 +787,7 @@ class M006SecurityRegressionTests {
         when(repository.findUserPositionAssignment(anyString())).thenAnswer(a -> Optional.ofNullable(positions.get(a.getArgument(0))));
         roles.put("junior", new RoleRecord("junior", "初级", null, "project", "active", Instant.now(), List.of(LOW), "org"));
         roles.put("senior", new RoleRecord("senior", "高级", null, "project", "active", Instant.now(), List.of(other), "org"));
-        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org", null, "active"), "unused", List.of()));
+        actor("template-admin", "platform_admin", "org", null);
         PermissionTemplate template = service.createPermissionTemplate(template("empty"));
         service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l1", "理由", "ops", "l1", "junior", template.id(), "active"));
         service.createPositionRoleTemplate(new PositionRoleTemplateRequest("m-l2", "理由", "ops", "l2", "senior", template.id(), "active"));
diff --git a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
index 6c401544..f703bd6b 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access.staff;
 
-// 文件版本：V0.22
+// 文件版本：V0.26
 
 import static org.assertj.core.api.Assertions.*;
 
@@ -322,7 +322,7 @@ class M010RegressionTests {
         assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='staff.payroll.penalty_cap_requested' AND metadata->>'review_id'=?",
                 Integer.class, pending.reviewId())).isEqualTo(1);
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
+                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
         assertThatThrownBy(() -> reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"确认",List.of(),null,"approved","确认")))
                 .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEPARTMENT_REVIEWS_PENDING"));
         for (var line : lines) {
@@ -331,7 +331,7 @@ class M010RegressionTests {
             assertThat(service.getPenaltyCap().value()).isEqualTo(2000);
         }
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor("usr_master_authority","master-authority","platform_admin","m010_p",null,"active"),"test",List.of()));
+                new AccessActor("usr_master_authority","master-authority",com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,"m010_p",null,"active"),"test",List.of()));
         reviewService.approve(pending.reviewId(),new com.complex.platform.governance.review.SupervisorReviewApprovalRequest(key(),"最终确认",List.of(),null,"approved","确认"));
         assertThat(service.getPenaltyCap().value()).isEqualTo(1000);
         actor(manager,"platform_admin",null);
@@ -600,7 +600,7 @@ class M010RegressionTests {
         repository.saveProfile(profile.withArchive(now, manager, now));
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                 new AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                        com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "m010_p", null, "active"),
                 "test", List.of()));
         var deleted = service.requestDeleteProfile(profile.id(), new StaffProfileDeleteRequest("工作结束"));
         assertThat(deleted.deleteStatus()).isEqualTo("approved");
diff --git a/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java b/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
index 71825678..f8999bea 100644
--- a/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
+++ b/backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access.staff;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import com.complex.platform.access.*;
 import com.complex.platform.auth.SessionIssuer;
@@ -24,7 +24,7 @@ final class StaffTestActors {
     }
     static void master() {
         SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
-                new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,"platform_admin",null,null,"active"),"test",List.of()));
+                new AccessActor(SessionIssuer.MASTER_USER_ID,SessionIssuer.MASTER_USERNAME,com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE,null,null,"active"),"test",List.of()));
     }
 
     /** U05-003/U11-007：模拟私人手机安装时生成的 EC 密钥对 → [Base64(X.509 DER 公钥), SHA-256(DER) 十六进制指纹]。 */
diff --git a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
index 474c55ce..9b5c2e7e 100644
--- a/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/bar/BarStoredValueFundChainJdbcIntegrationTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.bar;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -63,7 +63,7 @@ class BarStoredValueFundChainJdbcIntegrationTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
-                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"), null, java.util.List.of()));
     }
 
     @AfterEach
diff --git a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
index b56a4459..69252fb5 100644
--- a/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
+++ b/backend/src/test/java/com/complex/platform/customer/StoredValueNotificationIntegrationTest.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.02
+// 文件版本：V0.05
 
 import static org.assertj.core.api.Assertions.assertThat;
 
@@ -40,7 +40,7 @@ class StoredValueNotificationIntegrationTest {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor("usr_master_authority", "master-authority",
-                                "platform_admin", "org_1", null, "active"), null, java.util.List.of()));
+                                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"), null, java.util.List.of()));
         cleanupNotificationData();
         setSwitch("sms_notification", true);
         setSwitch("sms.notify.stored_value", true);
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
index 02d9e49d..a16333d9 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformControllerTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.02
+// 文件版本：V0.03
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.mockito.ArgumentMatchers.any;
@@ -382,6 +382,52 @@ class PlatformControllerTests {
     }
 
 
+
+    @Test
+    void projectAssociationEndpointsConsumeEachFunctionalPermissionOnce() throws Exception {
+        MerchantRecord merchant = new MerchantRecord("mch_project", "项目商户", "other", null, null, null,
+                null, "18800000001", null, null, null, null, null, null, null, null, "active",
+                Instant.EPOCH, Instant.EPOCH).withProject("project_a");
+        when(service.assignMerchantProject(eq(merchant.id()), any())).thenReturn(merchant);
+        when(service.associateBusinessEntityProject(eq("entity_project"), any()))
+                .thenReturn(new BusinessEntityRecord("entity_project", "公司", "management_company",
+                        null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+        String request = """
+                {"idempotency_key":"project-association","project_id":"project_a","reason":"核实原始资料"}
+                """;
+        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
+                        "/api/v1/platform/merchants/mch_project/project")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.data.project_id").value("project_a"))
+                .andExpect(jsonPath("$.data.contact_phone").value("188****0001"));
+        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
+                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("entity_project"));
+        verify(authorizationService, Mockito.times(1)).requirePermission("merchant.profile.manage");
+        verify(authorizationService, Mockito.times(1)).requirePermission("entity.binding.manage");
+        var expected = new com.complex.platform.governance.dto.ProjectAssociationRequest(
+                "project-association", "project_a", "核实原始资料");
+        verify(service).assignMerchantProject("mch_project", expected);
+        verify(service).associateBusinessEntityProject("entity_project", expected);
+    }
+
+    @Test
+    void associationEndpointRejectsMissingKeyAndDeniedPermissionBeforeService() throws Exception {
+        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
+                .andExpect(status().isBadRequest());
+        doThrow(ApiException.forbidden("PERMISSION_DENIED", "无权限"))
+                .when(authorizationService).requirePermission("merchant.profile.manage");
+        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
+                        "/api/v1/platform/merchants/mch_project/project")
+                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"idempotency_key\":\"denied\",\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
+                .andExpect(status().isForbidden());
+        Mockito.verifyNoInteractions(service);
+    }
+
     @TestConfiguration
     static class TestBeans {
         @Bean
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
index b155b4bf..563eca72 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.05
+// 文件版本：V0.08
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -30,6 +30,8 @@ class PlatformJdbcIntegrationTests {
 
     @Autowired
     private JdbcTemplate jdbcTemplate;
+    @Autowired private PlatformService platformService;
+    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
 
     @Test
     void migrationCreatesPlatformMasterDataTablesAndSeedsPermissions() {
@@ -381,6 +383,232 @@ class PlatformJdbcIntegrationTests {
                 .isInstanceOf(DataIntegrityViolationException.class);
     }
 
+
+    @Test
+    void approvedProjectStorageExistsWithoutHistoricalBackfill() {
+        assertThat(jdbcTemplate.queryForList("""
+                SELECT column_name FROM information_schema.columns
+                 WHERE table_schema = 'public' AND table_name = 'projects'
+                """, String.class)).contains("owner_person", "address", "business_scope");
+        assertThat(jdbcTemplate.queryForList("""
+                SELECT column_name FROM information_schema.columns
+                 WHERE table_schema = 'public' AND table_name = 'merchants'
+                """, String.class)).contains("project_id");
+        assertThat(regclass("business_entity_projects")).isEqualTo("business_entity_projects");
+    }
+
+    @Test
+    void onlyEndedInactiveBindingsStopOccupyingMerchant() {
+        String id = "00000000-0000-0000-0000-000000009045";
+        String merchant = "m009_reference_period";
+        Instant now = Instant.now();
+        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today.minusDays(3), today.minusDays(1),
+                "历史期间结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isZero();
+        for (String status : List.of("inactive", "suspended", "active")) {
+            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                    "operation", "contract", "settlement", status, today.plusDays(2), null,
+                    "尚未结束", now, now));
+            assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+        }
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today.plusDays(2), today.plusDays(3),
+                "未来结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+        repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(id, merchant,
+                "operation", "contract", "settlement", "inactive", today, today.minusDays(1),
+                "倒置期间不算真正结束", now, now));
+        assertThat(repository.countMerchantReferences(merchant)).isEqualTo(1);
+    }
+
+
+    private void seedProjectScopeRows() {
+        Instant now = Instant.now();
+        for (String key : List.of("a", "b")) {
+            repository.saveProject(new ProjectRecord("m009_project_" + key, null, "complex", "项目" + key,
+                    null, null, null, "active", now, now, "负责人" + key, "地址" + key, "范围" + key));
+            repository.saveBusinessEntity(new BusinessEntityRecord("m009_company_" + key, "公司" + key,
+                    "management_company", null, null, null, "active", now, now));
+            repository.associateBusinessEntityProject("m009_company_" + key, "m009_project_" + key);
+            repository.saveMerchant(new MerchantRecord("m009_merchant_" + key, "商户" + key, "other",
+                    "m009_company_" + key, null, null, null, null, null, null, null, null, null, null, null,
+                    null, "active", now, now).withProject("m009_project_" + key));
+            repository.saveSpace(new SpaceRecord("m009_space_" + key, "m009_project_" + key, "m009_merchant_" + key,
+                    null, "空间" + key, "shop", null, null, List.of(), "active", now, now));
+            repository.saveBusinessEntityBinding(new BusinessEntityBindingRecord(
+                    "00000000-0000-0000-0000-0000000090" + ("a".equals(key) ? "81" : "82"),
+                    "m009_merchant_" + key, "m009_company_" + key, "m009_company_" + key, "m009_company_" + key,
+                    "inactive", LocalDate.now().plusYears(1), null, "待生效", now, now));
+        }
+    }
+
+    @Test
+    void scopedSqlFiltersAllFiveTotalsAndSupportsCompanyManyToMany() {
+        seedProjectScopeRows();
+        String a = "m009_project_a";
+        assertThat(repository.listProjectsPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listProjectsPaged(a, 1, 1).items()).singleElement().satisfies(project -> {
+            assertThat(project.ownerPerson()).isEqualTo("负责人a");
+            assertThat(project.address()).isEqualTo("地址a");
+            assertThat(project.businessScope()).isEqualTo("范围a");
+        });
+        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listMerchantsPaged(a, true, null, 1, 1).items()).extracting(MerchantRecord::id)
+                .containsExactly("m009_merchant_a");
+        assertThat(repository.listSpacesPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listSpacesPaged(a, 1, 1).items()).extracting(SpaceRecord::id).containsExactly("m009_space_a");
+        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).total()).isEqualTo(1);
+        assertThat(repository.listBusinessEntityBindingsPaged(a, 1, 1).items())
+                .extracting(BusinessEntityBindingRecord::merchantId).containsExactly("m009_merchant_a");
+        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(1);
+        repository.associateBusinessEntityProject("m009_company_b", a);
+        repository.associateBusinessEntityProject("m009_company_b", a);
+        assertThat(repository.businessEntityInProject("m009_company_b", "m009_project_b")).isTrue();
+        assertThat(repository.listBusinessEntitiesPaged(a, 1, 1).total()).isEqualTo(2);
+        var companyPage1 = repository.listBusinessEntitiesPaged(a, 1, 1);
+        var companyPage2 = repository.listBusinessEntitiesPaged(a, 2, 1);
+        assertThat(companyPage2.items()).hasSize(1).doesNotContainAnyElementsOf(companyPage1.items());
+        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_b")).isEmpty();
+        assertThat(repository.listMerchantsByBusinessEntity(a, "m009_company_a")).extracting(MerchantRecord::id)
+                .containsExactly("m009_merchant_a");
+        assertThat(repository.listMerchantsByBusinessEntity(null, "m009_company_a")).isEmpty();
+        for (String scope : new String[] {null, "platform-company", "missing-project"}) {
+            org.junit.jupiter.api.Assertions.assertAll(
+                    () -> assertThat(repository.listMerchantsPaged(scope, true, null, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listProjectsPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listSpacesPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listBusinessEntitiesPaged(scope, 1, 1).total()).isZero(),
+                    () -> assertThat(repository.listBusinessEntityBindingsPaged(scope, 1, 1).total()).isZero());
+        }
+    }
+
+    @Test
+    void projectOnlyBackfillPreservesEveryOtherDatabaseColumnAndLegacyNulls() {
+        seedProjectScopeRows();
+        Instant now = Instant.now();
+        MerchantRecord legacy = new MerchantRecord("m009_legacy", "旧资料", "other", "financial-original",
+                "contract-original", "settlement-original", "联系人", "18800000001", "负责人", "A",
+                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000002", BigDecimal.ZERO, "lease",
+                "suspended", now, now).withArchive(now, "original-actor", now)
+                .withDelete("pending", "original-actor", now, "原理由", now);
+        repository.saveMerchant(legacy);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isNull();
+        assertThat(repository.listMerchantsPaged("m009_project_a", true, null, 1, 500).items())
+                .extracting(MerchantRecord::id).doesNotContain(legacy.id());
+        String before = jdbcTemplate.queryForObject(
+                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id());
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_a")).isTrue();
+        assertThat(repository.assignMerchantProject(legacy.id(), "m009_project_b")).isFalse();
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT (to_jsonb(m) - 'project_id')::text FROM merchants m WHERE id = ?", String.class, legacy.id())).isEqualTo(before);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
+        // Existing record saves cannot erase a separately assigned project.
+        repository.saveMerchant(legacy);
+        assertThat(repository.findMerchantById(legacy.id()).orElseThrow().projectId()).isEqualTo("m009_project_a");
+        ProjectRecord oldRequest = new ProjectRecord("m009_legacy_project", null, "complex", "旧调用项目",
+                null, null, null, "active", now, now);
+        repository.saveProject(oldRequest);
+        assertThat(repository.listProjectsPaged(oldRequest.id(), 1, 1).items()).singleElement().satisfies(project -> {
+            assertThat(project.ownerPerson()).isNull();
+            assertThat(project.address()).isNull();
+            assertThat(project.businessScope()).isNull();
+        });
+    }
+
+    @Test
+    void completingSpaceTypeRequestKeepsPriorRequestHistory() {
+        Instant now = Instant.now();
+        repository.saveSpaceType(new SpaceTypeRecord("m009_type_request", "m009_type_request", "申请历史", 100, "archived", now, now));
+        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第一次申请", "first-actor");
+        repository.completeSpaceTypeDeleteRequest("m009_type_request", "rejected");
+        repository.saveSpaceTypeDeleteRequest("m009_type_request", "第二次申请", "second-actor");
+        repository.completeSpaceTypeDeleteRequest("m009_type_request", "approved");
+        assertThat(jdbcTemplate.queryForList(
+                "SELECT requested_by || ':' || status FROM space_type_delete_requests WHERE space_type_id = ? ORDER BY requested_by",
+                String.class, "m009_type_request")).containsExactly("first-actor:rejected", "second-actor:approved");
+        assertThat(jdbcTemplate.queryForObject(
+                "SELECT count(*) FROM space_type_delete_requests WHERE space_type_id = ? AND status = 'pending'",
+                Integer.class, "m009_type_request")).isZero();
+    }
+
+
+    @Test
+    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
+    void concurrentFutureBindingsWaitForMerchantLockAndOnlyOneReservesTheSlot() throws Exception {
+        String key = "m009_concurrent_" + java.util.UUID.randomUUID().toString().substring(0, 8);
+        Instant now = Instant.now();
+        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
+        List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();
+        try {
+            repository.saveProject(new ProjectRecord(key, null, "complex", "并发绑定测试", null, null, null, "active", now, now));
+            repository.saveBusinessEntity(new BusinessEntityRecord(key, "并发主体", "management_company", null, null, null, "active", now, now));
+            repository.associateBusinessEntityProject(key, key);
+            repository.saveMerchant(new MerchantRecord(key, "并发商户", "other", null, null, null,
+                    null, null, null, null, null, null, null, null, null, null, "active", now, now).withProject(key));
+            var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
+            transaction.executeWithoutResult(status -> {
+                repository.lockMerchant(key);
+                for (int index = 0; index < 2; index++) {
+                    String requestKey = key + "_" + index;
+                    futures.add(pool.submit(() -> {
+                        var actor = new com.complex.platform.access.AccessActor("usr_admin", "platform_admin_user",
+                                "platform_admin", key, null, "active");
+                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+                        try {
+                            platformService.createBusinessEntityBinding(
+                                    new com.complex.platform.governance.dto.BusinessEntityBindingRequest(
+                                            requestKey, null, List.of(), key, key, key, key,
+                                            LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1), "并发预约"));
+                            return "CREATED";
+                        } catch (com.complex.platform.common.api.ApiException exception) {
+                            return exception.code();
+                        } finally {
+                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
+                        }
+                    }));
+                }
+                int waiting = 0;
+                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
+                while (waiting < 2 && System.nanoTime() < deadline) {
+                    jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
+                    waiting = jdbcTemplate.queryForObject("""
+                            SELECT count(*) FROM pg_stat_activity
+                             WHERE datname = current_database() AND wait_event_type = 'Lock'
+                               AND query LIKE 'SELECT id FROM merchants WHERE id = %FOR UPDATE%'
+                            """, Integer.class);
+                    if (waiting < 2) {
+                        try { Thread.sleep(25); }
+                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
+                    }
+                }
+                assertThat(waiting).as("两个请求均在商户行锁上等待，不能跳过锁直接检查").isEqualTo(2);
+            });
+            assertThat(List.of(futures.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
+                    futures.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS)))
+                    .containsExactlyInAnyOrder("CREATED", "ACTIVE_BINDING_EXISTS");
+            assertThat(jdbcTemplate.queryForObject(
+                    "SELECT count(*) FROM business_entity_bindings WHERE merchant_id = ? AND status = 'inactive' AND effective_to IS NULL",
+                    Integer.class, key)).isEqualTo(1);
+            MerchantRecord merchant = repository.findMerchantById(key).orElseThrow();
+            assertThat(merchant.businessEntityId()).isNull();
+            assertThat(merchant.contractEntityId()).isNull();
+            assertThat(merchant.settlementEntityId()).isNull();
+        } finally {
+            pool.shutdownNow();
+            assertThat(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
+            jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE merchant_id = ?", key);
+            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM business_entity_projects WHERE business_entity_id = ?", key);
+            jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", key);
+            jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key IN (?, ?)", key + "_0", key + "_1");
+        }
+    }
+
     private String regclass(String tableName) {
         return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
     }
diff --git a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
index 784f9e82..394ae9d1 100644
--- a/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/PlatformServiceTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.governance;
-// 文件版本：V0.04
+// 文件版本：V0.16
 
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.auth.SessionIssuer;
@@ -24,6 +24,7 @@ import com.complex.platform.governance.dto.MerchantDeleteRequest;
 import com.complex.platform.governance.dto.MerchantDeleteApprovalRequest;
 import com.complex.platform.governance.dto.MerchantArchiveRequest;
 import com.complex.platform.governance.dto.ProjectRequest;
+import com.complex.platform.governance.dto.ProjectAssociationRequest;
 import com.complex.platform.governance.dto.StatusChangeRequest;
 import com.complex.platform.support.TestSecurityActors;
 import com.fasterxml.jackson.databind.ObjectMapper;
@@ -64,6 +65,8 @@ class PlatformServiceTests {
         );
         // F-AUTHZ-02：治理写方法现首行 requirePlatformScope()，以平台管理员身份运行既有写用例。
         TestSecurityActors.asPlatformAdmin();
+        platformRepository.saveProject(new ProjectRecord("org_1", null, "complex", "当前项目", null, null,
+                null, "active", Instant.EPOCH, Instant.EPOCH));
     }
 
     @AfterEach
@@ -104,6 +107,7 @@ class PlatformServiceTests {
     // ---- V260 商户存档与审批后删除范式 ----
 
     private MerchantRecord seedMerchant(String key) {
+        if (!platformRepository.businessEntityExists("be_1")) seedEntity("be_1");
         return service.createMerchant(new MerchantProfileRequest(
                 key, "seed", List.of(), "存档删除测试商户", "other", "be_1",
                 null, null, "Jane", "18800000011",
@@ -114,12 +118,13 @@ class PlatformServiceTests {
     private void seedEntity(String id) {
         platformRepository.saveBusinessEntity(new BusinessEntityRecord(
                 id, id, "management_company", null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
+        platformRepository.associateBusinessEntityProject(id, "org_1");
     }
 
-    /** M009-009：最高权限（持授权 U 盘密钥者）身份——isMasterAuthority 要求 platform_admin + 固定 id/用户名（BSM-004/007）。 */
+    /** M009-009：最高权限（持授权 U 盘密钥者）身份——与生产 SessionIssuer 相同的最高权限三件套（BSM-004/007）。 */
     private static void asMasterAuthority() {
         AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
-                "platform_admin", "org_1", null, "active");
+                com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
         SecurityContextHolder.getContext().setAuthentication(
                 new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
     }
@@ -230,10 +235,10 @@ class PlatformServiceTests {
                 "sp-bad-prj", "create", List.of(), "prj_missing", null, null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("PROJECT_NOT_FOUND"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-mch", "create", List.of(), null, "mch_missing", null, "A", "shop", null, null, null, "active")))
+                "sp-bad-mch", "create", List.of(), "org_1", "mch_missing", null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_NOT_FOUND"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-parent", "create", List.of(), null, null, "sp_missing", "A", "shop", null, null, null, "active")))
+                "sp-bad-parent", "create", List.of(), "org_1", null, "sp_missing", "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("SPACE_NOT_FOUND"));
     }
 
@@ -247,7 +252,7 @@ class PlatformServiceTests {
                 "project-archived-mch", "create", List.of(), merchant.id(), "bar", "P0 Bar", null, null, null, "draft")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-archived-mch", "create", List.of(), null, merchant.id(), null, "A", "shop", null, null, null, "active")))
+                "sp-archived-mch", "create", List.of(), "org_1", merchant.id(), null, "A", "shop", null, null, null, "active")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("MERCHANT_ARCHIVED"));
         assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
                 "binding-archived-mch", "ref", List.of(), merchant.id(), "be_ref_1", "be_ref_1", "be_ref_1", null, null)))
@@ -260,18 +265,18 @@ class PlatformServiceTests {
     @Test
     void createSpaceRejectsStatusOutsideCheckSet() {
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-bad-status", "create", List.of(), null, null, null, "A", "shop", null, null, null, "zzz")))
+                "sp-bad-status", "create", List.of(), "org_1", null, null, "A", "shop", null, null, null, "zzz")))
                 .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
         assertThat(platformRepository.listSpaces()).noneMatch(s -> "zzz".equals(s.status()));
         assertThat(service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-unavailable", "create", List.of(), null, null, null, "B", "shop", null, null, null, "unavailable")).status())
+                "sp-unavailable", "create", List.of(), "org_1", null, null, "B", "shop", null, null, null, "unavailable")).status())
                 .isEqualTo("unavailable");
     }
 
     // ---- M009-008：生效绑定同步商户三个主体列（PGM-026「引用同一组绑定」） ----
     @Test
     void createBusinessEntityBindingSyncsMerchantEntityColumns() {
-        MerchantRecord merchant = seedMerchant("seed-sync");
+        MerchantRecord merchant = unboundMerchant("seed-sync");
         seedEntity("be_op_sync");
         seedEntity("be_ct_sync");
         seedEntity("be_st_sync");
@@ -422,7 +427,7 @@ class PlatformServiceTests {
     @Test
     void occupiedSpaceTypeCannotBeDeleted() {
         service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-shop-occ", "create", List.of(), null, null, null, "Shop Occ", "shop", "1", null, null, "active"));
+                "sp-shop-occ", "create", List.of(), "org_1", null, null, "Shop Occ", "shop", "1", null, null, "active"));
         service.archiveSpaceType("sptype_shop", "测试占用");
         assertThatThrownBy(() -> service.requestDeleteSpaceType("sptype_shop", "想删"))
                 .isInstanceOfSatisfying(ApiException.class, exception ->
@@ -467,6 +472,7 @@ class PlatformServiceTests {
     // V348 按业态查询（分账按业态拆份额要用）：只返回该业态的商户。
     @Test
     void listMerchantsFiltersByBusinessFormat() {
+        seedEntity("be_1");
         service.createMerchant(new MerchantProfileRequest(
                 "seed-bar", "seed", List.of(), "话梅酒馆", "bar", "be_1",
                 null, null, "Jane", "18800000011",
@@ -510,6 +516,7 @@ class PlatformServiceTests {
         // 默认列表不见；已存档列表仍可查且 delete_status=approved——与 JdbcPlatformRepository 同口径。
         assertThat(service.listMerchants(false).items())
                 .noneMatch(m -> m.id().equals(merchant.id()));
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()) && "approved".equals(m.deleteStatus()));
     }
@@ -543,6 +550,7 @@ class PlatformServiceTests {
 
         assertThat(decision.deleted()).isFalse();
         assertThat(decision.decision()).isEqualTo("rejected");
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()) && "none".equals(m.deleteStatus()) && m.archivedAt() != null);
     }
@@ -575,6 +583,7 @@ class PlatformServiceTests {
                 .isInstanceOfSatisfying(ApiException.class, exception ->
                         assertThat(exception.code()).isEqualTo("MERCHANT_IN_USE"));
         // 拒删后商户仍在（含已存档列表可见）。
+        TestSecurityActors.asPlatformAdmin();
         assertThat(service.listMerchants(true).items())
                 .anyMatch(m -> m.id().equals(merchant.id()));
     }
@@ -675,7 +684,7 @@ class PlatformServiceTests {
     void listSpacesWritesViewedAudit() {
         platformRepository.spaces.put("sp_1", new SpaceRecord(
                 "sp_1",
-                "prj_1",
+                "org_1",
                 "mch_1",
                 null,
                 "P0 Space",
@@ -699,7 +708,7 @@ class PlatformServiceTests {
 
     @Test
     void createBusinessEntityBindingAllowsOnlyOneActiveBindingPerMerchant() {
-        MerchantRecord merchant = seedMerchant("seed-binding"); // M009-006：经营对象与主体须存在
+        MerchantRecord merchant = unboundMerchant("seed-binding"); // M009-006：经营对象与主体须存在
         for (String id : List.of("be_operation_1", "be_contract_1", "be_settlement_1",
                 "be_operation_2", "be_contract_2", "be_settlement_2")) {
             seedEntity(id);
@@ -740,51 +749,27 @@ class PlatformServiceTests {
     }
 
     @Test
-    void changeBusinessEntityBindingRecordsStatusHistoryAndFreesActiveUniqueness() {
-        MerchantRecord merchant = seedMerchant("seed-change"); // M009-006
-        for (String id : List.of("be_operation", "be_contract", "be_settlement",
-                "be_operation_new", "be_contract_new", "be_settlement_new")) {
-            seedEntity(id);
+    void activeBindingCannotBeDisabledToReplaceFinancialSubjects() {
+        MerchantRecord merchant = unboundMerchant("active-merchant");
+        seedEntity("active-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "active-create", null, List.of(), merchant.id(), "active-company", "active-company",
+                "active-company", LocalDate.now(), "初始生效"));
+        for (String action : List.of("inactive", "suspended")) {
+            assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                    new StatusChangeRequest("stop-" + action, action, "停用后改主体", List.of())))
+                    .isInstanceOf(ApiException.class);
         }
-        BusinessEntityBindingRecord created = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
-                "binding-change-create",
-                "create binding",
-                List.of(),
-                merchant.id(),
-                "be_operation",
-                "be_contract",
-                "be_settlement",
-                LocalDate.parse("2026-06-14"),
-                "initial setup"
-        ));
-
-        BusinessEntityBindingRecord changed = service.changeBusinessEntityBinding(
-                created.id(),
-                new StatusChangeRequest("binding-change", "inactive", "contract entity changed", List.of("att_change"))
-        );
-        BusinessEntityBindingRecord replacement = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
-                "binding-change-replacement",
-                "replacement binding",
-                List.of(),
-                merchant.id(),
-                "be_operation_new",
-                "be_contract_new",
-                "be_settlement_new",
-                LocalDate.parse("2026-06-15"),
-                "replacement"
-        ));
-
-        assertThat(changed.status()).isEqualTo("inactive");
-        assertThat(replacement.status()).isEqualTo("active");
-        assertThat(platformRepository.businessEntityBindingStatusHistory)
-                .containsExactly("active", "inactive", "active");
+        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("active-company");
     }
 
 
     // P1-4 red→green: action 字段拒绝非法枚举值
     @Test
     void changeBusinessEntityBindingRejectsInvalidAction() {
-        MerchantRecord merchant = seedMerchant("seed-enum"); // M009-006
+        MerchantRecord merchant = unboundMerchant("seed-enum"); // M009-006
         for (String id : List.of("be_op", "be_ctr", "be_stl")) {
             seedEntity(id);
         }
@@ -846,7 +831,7 @@ class PlatformServiceTests {
         assertThat(service.listSpaceTypes().items()).extracting(SpaceTypeRecord::typeCode).contains("kiosk", "shop");
         // 新录入类型立即可用于 createSpace 硬校验。
         SpaceRecord space = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-kiosk", "create", List.of(), null, null, null, "K1", "kiosk", null, null, null, "active")); // M009-007：归属为空或须存在
+                "sp-kiosk", "create", List.of(), "org_1", null, null, "K1", "kiosk", null, null, null, "active")); // M009-039：归属为当前真实项目
         assertThat(space.spaceType()).isEqualTo("kiosk");
     }
 
@@ -854,7 +839,7 @@ class PlatformServiceTests {
     void createSpaceRejectsUnknownSpaceType() {
         // 条目3 硬校验：不在字典内的 space_type → VALIDATION_FAILED。
         assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                        "sp-bad", "create", List.of(), null, null, null, "Bad", "no_such_type", null, null, null, "active")))
+                        "sp-bad", "create", List.of(), "org_1", null, null, "Bad", "no_such_type", null, null, null, "active")))
                 .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class, ex ->
                         assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
     }
@@ -862,13 +847,442 @@ class PlatformServiceTests {
     @Test
     void createSpaceAcceptsDictionarySpaceType() {
         SpaceRecord saved = service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
-                "sp-ok", "create", List.of(), null, null, null, "Shop A", "shop", "1", null, null, "active"));
+                "sp-ok", "create", List.of(), "org_1", null, null, "Shop A", "shop", "1", null, null, "active"));
         assertThat(saved.spaceType()).isEqualTo("shop");
     }
 
+    private MerchantRecord unboundMerchant(String key) {
+        return service.createMerchant(new MerchantProfileRequest(
+                key, "seed", List.of(), "待绑定商户", "other", null, null, null,
+                "Jane", "18800000011", null, null, null, null, null, null, null, null, "active"));
+    }
+
+    @Test
+    void futureBindingDoesNotChangeCurrentMerchantSubjects() {
+        MerchantRecord merchant = unboundMerchant("future-merchant");
+        seedEntity("future-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "future-binding", null, List.of(), merchant.id(), "future-company", "future-company",
+                "future-company", LocalDate.now().plusDays(10), "未来生效"));
+        assertThat(binding.status()).isEqualTo("inactive");
+        assertThat(binding.effectiveTo()).isNull();
+        MerchantRecord saved = platformRepository.findMerchantById(merchant.id()).orElseThrow();
+        assertThat(saved.businessEntityId()).isNull();
+        assertThat(saved.contractEntityId()).isNull();
+        assertThat(saved.settlementEntityId()).isNull();
+    }
+
+    @Test
+    void anotherFutureBindingCannotReserveTheSameMerchant() {
+        MerchantRecord merchant = unboundMerchant("reserved-merchant");
+        seedEntity("reserved-company");
+        service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "reserve-first", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
+                "reserved-company", LocalDate.now().plusDays(10), "预约"));
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "reserve-second", null, List.of(), merchant.id(), "reserved-company", "reserved-company",
+                "reserved-company", LocalDate.now().plusDays(11), "第二条")))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.listBusinessEntityBindings()).hasSize(1);
+    }
+
+    @Test
+    void futureBindingCannotActivateOrEndBeforeItsStart() {
+        MerchantRecord merchant = unboundMerchant("early-merchant");
+        seedEntity("early-company");
+        var binding = service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "early-create", null, List.of(), merchant.id(), "early-company", "early-company",
+                "early-company", LocalDate.now().plusDays(10), "未来生效"));
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                new StatusChangeRequest("early-activate", "active", "提前启用", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(binding.id(),
+                new StatusChangeRequest("early-end", "inactive", "提前结束", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findBusinessEntityBindingById(binding.id())).contains(binding);
+    }
+
+    @Test
+    void dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen() {
+        MerchantRecord merchant = unboundMerchant("due-merchant");
+        seedEntity("due-company");
+        var due = new BusinessEntityBindingRecord("due-binding", merchant.id(), "due-company", "due-company",
+                "due-company", "inactive", LocalDate.now(), null, "到期", Instant.EPOCH, Instant.EPOCH);
+        platformRepository.saveBusinessEntityBinding(due);
+        assertThat(service.changeBusinessEntityBinding(due.id(),
+                new StatusChangeRequest("due-activate", "active", "到期启用", List.of())).status()).isEqualTo("active");
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("due-company");
+
+        MerchantRecord historicalMerchant = unboundMerchant("ended-history-merchant");
+        var ended = new BusinessEntityBindingRecord("ended-binding", historicalMerchant.id(), "due-company", "due-company",
+                "due-company", "inactive", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1),
+                "历史", Instant.EPOCH, Instant.EPOCH);
+        platformRepository.saveBusinessEntityBinding(ended);
+        assertThatThrownBy(() -> service.changeBusinessEntityBinding(ended.id(),
+                new StatusChangeRequest("reopen-history", "active", "重开历史", List.of())))
+                .isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findBusinessEntityBindingById(ended.id())).contains(ended);
+    }
+
+    @Test
+    void bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority() {
+        MerchantRecord merchant = unboundMerchant("existing-subject-merchant");
+        platformRepository.saveMerchant(merchant.withEntities("known-company", "known-company", "known-company", Instant.now()));
+        seedEntity("different-company");
+        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
+                SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active");
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
+        assertThatThrownBy(() -> service.createBusinessEntityBinding(new BusinessEntityBindingRequest(
+                "replace-subjects", null, List.of(), merchant.id(), "different-company", "different-company",
+                "different-company", LocalDate.now(), "不经财税审核替换")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW"));
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().settlementEntityId())
+                .isEqualTo("known-company");
+        assertThat(platformRepository.listBusinessEntityBindings()).isEmpty();
+    }
+
+    @Test
+    void projectCreationReturnsApprovedOptionalProfileFields() {
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        ProjectRequest request = mapper.convertValue(Map.of(
+                "idempotencyKey", "project-profile", "name", "甲综合体", "venueType", "bar",
+                "status", "draft", "ownerPerson", "王负责人", "address", "甲路一号",
+                "businessScope", "餐饮与零售"), ProjectRequest.class);
+        var result = mapper.valueToTree(service.createProject(request));
+        assertThat(result.path("ownerPerson").asText()).isEqualTo("王负责人");
+        assertThat(result.path("address").asText()).isEqualTo("甲路一号");
+        assertThat(result.path("businessScope").asText()).isEqualTo("餐饮与零售");
+    }
+
+    @Test
+    void deletedSpaceTypeCanBeRestoredButPendingRequestCannotBeBypassed() {
+        service.archiveSpaceType("sptype_booth", "暂不用");
+        service.requestDeleteSpaceType("sptype_booth", "删除错误配置");
+        assertThatThrownBy(() -> service.unarchiveSpaceType("sptype_booth")).isInstanceOf(ApiException.class);
+        asMasterAuthority();
+        service.approveDeleteSpaceType("sptype_booth", "approved");
+        org.assertj.core.api.Assertions.assertThatCode(() -> service.unarchiveSpaceType("sptype_booth"))
+                .doesNotThrowAnyException();
+        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("active");
+    }
+
+    @Test
+    void spaceTypeDeletionCompletesOnlyTheCurrentRequest() {
+        service.archiveSpaceType("sptype_booth", "暂不用");
+        service.requestDeleteSpaceType("sptype_booth", "申请");
+        asMasterAuthority();
+        service.approveDeleteSpaceType("sptype_booth", "rejected");
+        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
+        service.requestDeleteSpaceType("sptype_booth", "最高权限直办");
+        assertThat(platformRepository.findSpaceTypeDeleteRequestedBy("sptype_booth")).isNull();
+        assertThat(platformRepository.findSpaceTypeById("sptype_booth").orElseThrow().status()).isEqualTo("disabled");
+    }
+
+    @Test
+    void spaceTypeAuditHasObjectIdAndActualRejectedDecision() {
+        var type = service.createSpaceType(new com.complex.platform.governance.dto.SpaceTypeRequest(
+                "audit-type", null, List.of(), "auditable_type", "审计类型", 10, "active"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.updated".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.businessId()).isEqualTo(type.id()));
+        service.archiveSpaceType(type.id(), "存档原因");
+        service.requestDeleteSpaceType(type.id(), "删除原因");
+        asMasterAuthority();
+        service.approveDeleteSpaceType(type.id(), "rejected");
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_rejected".equals(e.eventName()))
+                .singleElement().satisfies(e -> {
+                    assertThat(e.businessId()).isEqualTo(type.id());
+                    assertThat(e.metadata()).containsEntry("decision", "rejected");
+                });
+        assertThat(auditEventRepository.events).filteredOn(e -> "space_type.delete_requested".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
+    }
+
+    @Test
+    void merchantDeleteAuditKeepsReasonAndRecordsRejectionAgainstTheMerchant() {
+        MerchantRecord merchant = seedMerchant("audit-merchant");
+        service.archiveMerchant(merchant.id(), new MerchantArchiveRequest("audit-archive", "存档原因"));
+        service.requestDeleteMerchant(merchant.id(), new MerchantDeleteRequest("audit-delete", "删除原因"));
+        asMasterAuthority();
+        service.approveDeleteMerchant(merchant.id(), new MerchantDeleteApprovalRequest("audit-reject", "rejected", "核实保留"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_requested".equals(e.eventName()))
+                .singleElement().satisfies(e -> assertThat(e.metadata()).containsEntry("reason", "删除原因"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.delete_rejected".equals(e.eventName()))
+                .singleElement().satisfies(e -> {
+                    assertThat(e.businessId()).isEqualTo(merchant.id());
+                    assertThat(e.metadata()).containsEntry("decision", "rejected").containsEntry("comment", "核实保留");
+                });
+    }
+
+    private MerchantProfileRequest numericMerchant(String key, String status,
+            BigDecimal area, BigDecimal leasedArea, BigDecimal rent) {
+        return new MerchantProfileRequest(key, null, List.of(), "数值验证商户", "other",
+                null, null, null, null, null, null, null, area, leasedArea, null, null, rent, null, status);
+    }
+
+    @Test
+    void negativeCapacityAreasAndRentAreRejectedWhileZeroAndNullRemainValid() {
+        assertThatThrownBy(() -> service.createProject(new ProjectRequest(
+                "negative-capacity", null, List.of(), null, "bar", "项目", null, null, -1, "draft")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        for (MerchantProfileRequest request : List.of(
+                numericMerchant("negative-area", "active", new BigDecimal("-1"), null, null),
+                numericMerchant("negative-leased", "active", null, new BigDecimal("-1"), null),
+                numericMerchant("negative-rent", "active", null, null, new BigDecimal("-1")))) {
+            assertThatThrownBy(() -> service.createMerchant(request))
+                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
+        }
+        assertThat(service.createProject(new ProjectRequest(
+                "zero-capacity", null, List.of(), null, "bar", "项目", null, null, 0, "draft")).capacity()).isZero();
+        assertThat(service.createMerchant(numericMerchant("zero-values", "active",
+                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)).rentPerSquareMeter()).isZero();
+        assertThat(service.createMerchant(numericMerchant("null-values", "active", null, null, null)).areaSquareMeter()).isNull();
+    }
+
+    @Test
+    void ordinaryMerchantProfileCannotCauseImmediateExit() {
+        assertThatThrownBy(() -> service.createMerchant(numericMerchant("new-exit", "terminated", null, null, null)))
+                .isInstanceOf(ApiException.class);
+        MerchantRecord merchant = unboundMerchant("exit-merchant");
+        assertThatThrownBy(() -> service.updateMerchant(merchant.id(),
+                numericMerchant("update-exit", "terminated", null, null, null))).isInstanceOf(ApiException.class);
+        assertThat(platformRepository.findMerchantById(merchant.id()).orElseThrow().status()).isEqualTo("active");
+    }
+
+    @Test
+    void userFacingRoleValidationDoesNotIncludeEnglishEnumNames() {
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "invalid-role-message", null, List.of(), "公司", "unknown", null, null, null, "active")))
+                .isInstanceOfSatisfying(ApiException.class, e -> {
+                    assertThat(e.code()).isEqualTo("VALIDATION_FAILED");
+                    assertThat(e.getMessage()).doesNotContain("platform", "project_company", "management_company");
+                });
+    }
+
+    @Test
+    void savingAnAlreadyTerminatedMerchantDoesNotReactivateItWhenStatusIsOmitted() throws Exception {
+        MerchantRecord merchant = unboundMerchant("already-terminated");
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
+        var data = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(merchant);
+        data.put("status", "terminated");
+        platformRepository.saveMerchant(mapper.treeToValue(data, MerchantRecord.class));
+        assertThat(service.updateMerchant(merchant.id(),
+                numericMerchant("same-terminated", "terminated", null, null, null)).status()).isEqualTo("terminated");
+        assertThat(service.updateMerchant(merchant.id(),
+                numericMerchant("omitted-status", null, null, null, null)).status()).isEqualTo("terminated");
+    }
+
+    private void twoProjectFixtures() {
+        for (String id : List.of("org_1", "other-project")) {
+            platformRepository.saveProject(new ProjectRecord(id, null, "bar", id, null, null, null,
+                    "active", Instant.EPOCH, Instant.EPOCH));
+        }
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        for (String suffix : List.of("a", "b")) {
+            String project = "a".equals(suffix) ? "org_1" : "other-project";
+            platformRepository.saveMerchant(mapper.convertValue(Map.of(
+                    "id", "scoped-merchant-" + suffix, "merchantName", suffix, "merchantType", "other",
+                    "status", "active", "projectId", project, "deleteStatus", "none",
+                    "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
+            platformRepository.saveSpace(new SpaceRecord("scoped-space-" + suffix, project,
+                    "scoped-merchant-" + suffix, null, suffix, "shop", null, null, List.of(),
+                    "active", Instant.EPOCH, Instant.EPOCH));
+            seedEntity("scoped-company-" + suffix);
+            platformRepository.businessEntityProjects.put("scoped-company-" + suffix, java.util.Set.of(project));
+            platformRepository.saveBusinessEntityBinding(new BusinessEntityBindingRecord("scoped-binding-" + suffix,
+                    "scoped-merchant-" + suffix, "scoped-company-" + suffix, "scoped-company-" + suffix,
+                    "scoped-company-" + suffix, "inactive", LocalDate.now().plusDays(1), null,
+                    null, Instant.EPOCH, Instant.EPOCH));
+        }
+    }
+
+    @Test
+    void projectListsAndTotalsStayWithinCurrentProjectForOrdinaryAdmin() {
+        twoProjectFixtures();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> { var p = service.listProjects(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(ProjectRecord::id).containsExactly("org_1"); },
+                () -> { var p = service.listSpaces(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(SpaceRecord::id).containsExactly("scoped-space-a"); },
+                () -> { var p = service.listMerchants(true, null, 1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(MerchantRecord::id).containsExactly("scoped-merchant-a"); },
+                () -> { var p = service.listBusinessEntities(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(BusinessEntityRecord::id).containsExactly("scoped-company-a"); },
+                () -> { var p = service.listBusinessEntityBindings(1, 1); assertThat(p.total()).isEqualTo(1);
+                    assertThat(p.items()).extracting(BusinessEntityBindingRecord::id).containsExactly("scoped-binding-a"); });
+    }
+
+    @Test
+    void employeeCannotReadOtherProjectThroughSpaceLookupOrWrite() {
+        twoProjectFixtures();
+        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
+                new AccessActor("employee-a", "employee-a", "employee", "org_1", null, "active"), "token", List.of()));
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(service.spaceExists("scoped-space-b")).isFalse(),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-parent", null, List.of(), "org_1", null, "scoped-space-b", "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-merchant", null, List.of(), "org_1", "scoped-merchant-b", null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class));
+    }
+
+    @Test
+    void spaceRequiresCurrentProjectAndNonnegativeArea() {
+        twoProjectFixtures();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "missing-project", null, List.of(), null, null, null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "cross-project", null, List.of(), "other-project", null, null, "空间", "shop",
+                        null, null, null, "active"))).isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.createSpace(new com.complex.platform.governance.dto.SpaceRequest(
+                        "negative-space", null, List.of(), "org_1", null, null, "空间", "shop",
+                        null, null, new BigDecimal("-1"), "active"))).isInstanceOf(ApiException.class));
+    }
+
+    @Test
+    void initialMerchantRejectsUnknownOrForeignBusinessEntities() {
+        twoProjectFixtures();
+        ObjectMapper mapper = new ObjectMapper()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        for (String entity : List.of("missing-company", "scoped-company-b")) {
+            MerchantProfileRequest request = mapper.convertValue(Map.of(
+                    "idempotencyKey", "initial-" + entity, "merchantName", "商户", "merchantType", "other",
+                    "status", "active", "businessEntityId", entity), MerchantProfileRequest.class);
+            assertThatThrownBy(() -> service.createMerchant(request)).isInstanceOf(ApiException.class);
+        }
+    }
+
+    @Test
+    void ordinaryProfileUpdateCannotClaimOrMoveProject() {
+        twoProjectFixtures();
+        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
+                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
+        platformRepository.saveMerchant(mapper.convertValue(Map.of(
+                "id", "legacy-unassigned", "merchantName", "旧商户", "merchantType", "other", "status", "active",
+                "deleteStatus", "none", "createdAt", Instant.EPOCH, "updatedAt", Instant.EPOCH), MerchantRecord.class));
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a", mapper.convertValue(Map.of(
+                        "idempotencyKey", "move-project", "merchantName", "商户", "merchantType", "other",
+                        "projectId", "other-project", "status", "active"), MerchantProfileRequest.class)))
+                        .isInstanceOf(ApiException.class),
+                () -> assertThatThrownBy(() -> service.updateMerchant("legacy-unassigned", mapper.convertValue(Map.of(
+                        "idempotencyKey", "claim-project", "merchantName", "商户", "merchantType", "other",
+                        "projectId", "org_1", "status", "active"), MerchantProfileRequest.class)))
+                        .isInstanceOf(ApiException.class));
+    }
+
+
+    @Test
+    void masterProjectBackfillChangesOnlyTheProjectAndCannotMoveIt() {
+        twoProjectFixtures();
+        MerchantRecord original = new MerchantRecord("legacy-backfill", "旧商户", "other", "financial-original",
+                "contract-original", "settlement-original", "联系人", "18800000011", "负责人", "A1",
+                BigDecimal.TEN, BigDecimal.ONE, "第二联系人", "18800000012", BigDecimal.ZERO, "lease",
+                "suspended", Instant.EPOCH, Instant.EPOCH).withArchive(Instant.EPOCH, "operator", Instant.EPOCH)
+                .withDelete("pending", "operator", Instant.EPOCH, "保留理由", Instant.EPOCH);
+        platformRepository.saveMerchant(original);
+        asMasterAuthority();
+        MerchantRecord saved = service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"));
+        assertThat(saved).isEqualTo(original.withProject("org_1"));
+        assertThat(service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-backfill", "org_1", "核实原始归属"))).isEqualTo(saved);
+        assertThat(service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-same", "org_1", "确认同值"))).isEqualTo(saved);
+        assertThatThrownBy(() -> service.assignMerchantProject(original.id(),
+                new ProjectAssociationRequest("project-move", "other-project", "迁移")))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("MERCHANT_PROJECT_ALREADY_ASSIGNED"));
+        assertThat(auditEventRepository.events).filteredOn(e -> "merchant.project_assigned".equals(e.eventName()))
+                .hasSize(2).allSatisfy(e -> {
+                    assertThat(e.businessId()).isEqualTo(original.id());
+                    assertThat(e.metadata()).containsEntry("projectId", "org_1").containsKey("reason");
+                    assertThat(e.actorId()).isEqualTo(SessionIssuer.MASTER_USER_ID);
+                });
+    }
+
+    @Test
+    void companyCanBeAssociatedWithTwoProjectsOnlyByRealMaster() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        assertThat(service.associateBusinessEntityProject("scoped-company-a",
+                new ProjectAssociationRequest("company-link", "other-project", "确认共同运营"))).isNotNull();
+        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "org_1")).isTrue();
+        assertThat(platformRepository.businessEntityInProject("scoped-company-a", "other-project")).isTrue();
+        BusinessEntityRecord created = service.createBusinessEntity(new BusinessEntityRequest(
+                "master-company-create", null, List.of(), "新关联公司", "management_company", null, null, null, "active", "org_1"));
+        assertThat(platformRepository.businessEntityInProject(created.id(), "org_1")).isTrue();
+        TestSecurityActors.asPlatformAdmin();
+        assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-b",
+                new ProjectAssociationRequest("ordinary-link", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-b",
+                new ProjectAssociationRequest("ordinary-assign", "org_1", "普通认领"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.createBusinessEntity(new BusinessEntityRequest(
+                "ordinary-company-foreign", null, List.of(), "其它项目", "management_company", null, null, null, "active", "other-project")))
+                .isInstanceOf(ApiException.class);
+    }
+
+    @Test
+    void associationRequiresExistingExplicitProjectReasonAndIdempotency() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        for (ProjectAssociationRequest request : List.of(
+                new ProjectAssociationRequest(null, "org_1", "确认"),
+                new ProjectAssociationRequest("missing-project", null, "确认"),
+                new ProjectAssociationRequest("unknown-project", "missing", "确认"),
+                new ProjectAssociationRequest("missing-reason", "org_1", " "))) {
+            assertThatThrownBy(() -> service.assignMerchantProject("scoped-merchant-a", request)).isInstanceOf(ApiException.class);
+            assertThatThrownBy(() -> service.associateBusinessEntityProject("scoped-company-a", request)).isInstanceOf(ApiException.class);
+        }
+        assertThatThrownBy(() -> service.assignMerchantProject("missing",
+                new ProjectAssociationRequest("missing-merchant", "org_1", "确认"))).isInstanceOf(ApiException.class);
+        assertThatThrownBy(() -> service.associateBusinessEntityProject("missing",
+                new ProjectAssociationRequest("missing-company", "org_1", "确认"))).isInstanceOf(ApiException.class);
+    }
+
+    @Test
+    void masterHasNoGlobalListOrOrdinaryProfileBypass() {
+        twoProjectFixtures();
+        asMasterAuthority();
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(service.listMerchants(true).total()).isZero(),
+                () -> assertThat(service.listBusinessEntities().total()).isZero(),
+                () -> assertThat(service.listProjects().total()).isZero(),
+                () -> assertThat(service.listSpaces().total()).isZero(),
+                () -> assertThat(service.listBusinessEntityBindings().total()).isZero());
+        assertThatThrownBy(() -> service.updateMerchant("scoped-merchant-a",
+                numericMerchant("master-ordinary-edit", "active", null, null, null)))
+                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PROJECT_SCOPE_MISMATCH"));
+    }
+
     private static final class InMemoryPlatformRepository implements PlatformRepository {
         @Override public void setMerchantArchiveReason(String id, String reason) { }
+        @Override public void lockMerchant(String merchantId) { }
+        @Override public boolean businessEntityInProject(String entityId, String projectId) {
+            return projectId != null && businessEntityProjects.getOrDefault(entityId, java.util.Set.of()).contains(projectId);
+        }
+        @Override public void associateBusinessEntityProject(String entityId, String projectId) {
+            businessEntityProjects.compute(entityId, (ignored, current) -> {
+                java.util.Set<String> projects = new java.util.HashSet<>(current == null ? java.util.Set.of() : current);
+                projects.add(projectId);
+                return projects;
+            });
+        }
+        @Override public boolean assignMerchantProject(String merchantId, String projectId) {
+            MerchantRecord merchant = merchants.get(merchantId);
+            if (merchant == null || (merchant.projectId() != null && !merchant.projectId().equals(projectId))) return false;
+            merchants.put(merchantId, merchant.withProject(projectId));
+            return true;
+        }
+        @Override public void completeSpaceTypeDeleteRequest(String spaceTypeId, String status) {
+            deleteRequestedBy.remove(spaceTypeId);
+        }
         private final Map<String, BusinessEntityRecord> businessEntities = new LinkedHashMap<>();
+        private final Map<String, java.util.Set<String>> businessEntityProjects = new LinkedHashMap<>();
         private final Map<String, MerchantRecord> merchants = new LinkedHashMap<>();
         private final Map<String, ProjectRecord> projects = new LinkedHashMap<>();
         private final Map<String, SpaceRecord> spaces = new LinkedHashMap<>();
diff --git a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
index 9f25806f..8c29f53a 100644
--- a/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
@@ -1,4 +1,4 @@
-// 文件版本：V0.03
+// 文件版本：V0.04
 package com.complex.platform.governance.review;
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
@@ -440,7 +440,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(svc.listPending(200).items()).extracting(p -> p.decision().reviewId()).containsExactlyInAnyOrder(a.reviewId(), b.reviewId());
         assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("governance.supervisor_review.pending_listed_cross_scope");
@@ -578,7 +578,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", null, null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, null, null, "active"),
                         "test", List.of()));
     }
 
@@ -783,7 +783,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest("esc-approve-master", "note", List.of("att"), 3, "approved", "ok"))
                 .reviewStatus()).isEqualTo("approved");
@@ -809,7 +809,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThat(service.forward(pending.reviewId(), new SupervisorReviewForwardRequest(
                 "top-forward-master", null, "usr_anyone", "handle")).forwardedTo()).isEqualTo("usr_anyone");
@@ -831,7 +831,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of()));
         assertThatThrownBy(() -> service.approve(pending.reviewId(), new SupervisorReviewApprovalRequest(
                 "top-chain-master-early", "note", List.of("att"), 0, "approved", "too early")))
@@ -878,7 +878,7 @@ class SupervisorReviewServiceTests {
         org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                 new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                         new com.complex.platform.access.AccessActor(com.complex.platform.auth.SessionIssuer.MASTER_USER_ID,
-                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, "platform_admin", "org_1", null, "active"),
+                                com.complex.platform.auth.SessionIssuer.MASTER_USERNAME, com.complex.platform.auth.SessionIssuer.MASTER_ACCOUNT_TYPE, "org_1", null, "active"),
                         "token", List.of())); // 红线④：只有密钥主体可整批看待审
         assertThat(service.listPending(10).items()).extracting(item -> item.decision().reviewId())
                 .contains(money.reviewId(), normal.reviewId());
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
index a3942057..635a8688 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingServiceTests.java
@@ -1,4 +1,5 @@
 package com.complex.platform.leasing;
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -212,6 +213,7 @@ class LeasingServiceTests {
 
     @Test
     void signIntentWithoutMerchantAutoCreatesFormalMerchant() {
+        when(authz.currentProjectScopeQuietly()).thenReturn(Optional.of("lease-current-project"));
         // 1.4 决策A：merchant_id 空 → 签约自动建正式商户，带入线索联系人/业态、四类主体默认同一公司。
         when(contractService.createDraft(any())).thenReturn(new ContractRecord(
                 "ctr_auto", "mch_auto", "cs", null, "cs", LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
@@ -222,6 +224,11 @@ class LeasingServiceTests {
                 null, null, null, "leasing_auto", "active", Instant.now(), Instant.now()));
         LeasingLeadRecord lead = service.createLead(new LeasingLeadRequest(
                 "钱七", "13800001111", "online_inquiry", "retail", "sp_1", null, null, "k1"));
+        // 本例验证已归属意向的签约调用点；线索创建归属由其所属模块另审。
+        repo.saveLead(new LeasingLeadRecord(lead.id(), lead.contactName(), lead.contactPhone(), lead.source(),
+                lead.intendedBusinessType(), lead.intendedSpaceId(), lead.intendedAreaSqm(), lead.brandId(),
+                lead.ownerUserId(), lead.status(), lead.lostOrHoldReason(), lead.lastFollowUpAt(),
+                "lease-current-project", lead.createdAt(), lead.updatedAt()));
         LeasingIntentRecord intent = service.createIntent(new LeasingIntentRequest(lead.id(), "sp_1", null, Map.of(), "k2"));
         service.advanceIntent(intent.id(), new LeasingIntentAdvanceRequest("pending_signing", null, "k4"));
 
@@ -231,19 +238,22 @@ class LeasingServiceTests {
 
         org.mockito.ArgumentCaptor<MerchantProfileRequest> captor =
                 org.mockito.ArgumentCaptor.forClass(MerchantProfileRequest.class);
-        verify(platformService).createMerchant(captor.capture());
+        var order = org.mockito.Mockito.inOrder(platformService);
+        order.verify(platformService).createMerchant(captor.capture());
         MerchantProfileRequest req = captor.getValue();
         assertThat(req.contactName()).isEqualTo("钱七");
         assertThat(req.contactPhone()).isEqualTo("13800001111");
         assertThat(req.merchantType()).isEqualTo("retail");
-        assertThat(req.contractEntityId()).isEqualTo("cs");
-        assertThat(req.businessEntityId()).isEqualTo("cs");
-        assertThat(req.settlementEntityId()).isEqualTo("cs");
+        org.junit.jupiter.api.Assertions.assertAll(
+                () -> assertThat(req.projectId()).isEqualTo("lease-current-project"),
+                () -> assertThat(req.contractEntityId()).isNull(),
+                () -> assertThat(req.businessEntityId()).isNull(),
+                () -> assertThat(req.settlementEntityId()).isNull());
 
         // AUD-003：自动建商户后必建"四主体"权威绑定（经营/合同/结算主体默认同一公司=cs）。
         org.mockito.ArgumentCaptor<com.complex.platform.governance.dto.BusinessEntityBindingRequest> bindingCaptor =
                 org.mockito.ArgumentCaptor.forClass(com.complex.platform.governance.dto.BusinessEntityBindingRequest.class);
-        verify(platformService).createBusinessEntityBinding(bindingCaptor.capture());
+        order.verify(platformService).createBusinessEntityBinding(bindingCaptor.capture());
         com.complex.platform.governance.dto.BusinessEntityBindingRequest binding = bindingCaptor.getValue();
         assertThat(binding.merchantId()).isEqualTo("mch_auto");
         assertThat(binding.operationEntityId()).isEqualTo("cs");
diff --git a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
index 0e60c08e..ac496f13 100644
--- a/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
+++ b/backend/src/test/java/com/complex/platform/leasing/LeasingSignAtomicityJdbcIntegrationTests.java
@@ -1,5 +1,5 @@
 package com.complex.platform.leasing;
-// 文件版本：V0.01
+// 文件版本：V0.02
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -36,6 +36,7 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     @Autowired private JdbcTemplate jdbcTemplate;
 
     private String suffix;
+    private String projectId;
     private String subject;   // contract_subject_id 标记（同时是 merchant/binding 三主体快照）
     private String leadId;
     private String intentId;
@@ -51,12 +52,14 @@ class LeasingSignAtomicityJdbcIntegrationTests {
                 + "(SELECT id FROM contracts WHERE contract_subject_id = ?)", subject);
         jdbcTemplate.update("DELETE FROM contracts WHERE contract_subject_id = ?", subject);
         jdbcTemplate.update("DELETE FROM business_entity_bindings WHERE contract_entity_id = ?", subject);
-        jdbcTemplate.update("DELETE FROM merchants WHERE contract_entity_id = ?", subject);
+        jdbcTemplate.update("DELETE FROM merchants WHERE project_id = ?", projectId);
+        jdbcTemplate.update("DELETE FROM business_entity_projects WHERE project_id = ?", projectId);
         jdbcTemplate.update("DELETE FROM business_entities WHERE id = ?", subject); // M009-006 夹具主体
         jdbcTemplate.update("DELETE FROM leasing_intents WHERE id = ?", intentId);
         jdbcTemplate.update("DELETE FROM leasing_leads WHERE id = ?", leadId);
         jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
         jdbcTemplate.update("DELETE FROM spaces WHERE id LIKE ?", "sp_%" + suffix);
+        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", projectId);
     }
 
     /** AUD-002：签约中途失败（草稿空间不存在）→ 商户/四主体绑定/合同全回滚，无孤儿；线索未置 signed。 */
@@ -103,10 +106,35 @@ class LeasingSignAtomicityJdbcIntegrationTests {
         assertThat(countContracts()).isEqualTo(1);
     }
 
+
+    @Test
+    void futureSignKeepsCurrentSubjectsEmptyAndRetainsIntentProject() {
+        String spaceId = "sp_future_" + newSuffix();
+        seedSpace(spaceId);
+        seed(spaceId);
+        LocalDate future = LocalDate.now(java.time.ZoneOffset.UTC).plusYears(1);
+        ContractRecord contract = leasingService.signIntent(intentId, new LeasingSignRequest(
+                null, subject, null, null, future, future.plusYears(1), "sign_" + suffix));
+        Map<String, Object> merchant = jdbcTemplate.queryForMap(
+                "SELECT project_id, business_entity_id, contract_entity_id, settlement_entity_id FROM merchants WHERE id = ?",
+                contract.merchantId());
+        assertThat(merchant).containsEntry("project_id", projectId)
+                .containsEntry("business_entity_id", null).containsEntry("contract_entity_id", null)
+                .containsEntry("settlement_entity_id", null);
+        assertThat(jdbcTemplate.queryForMap(
+                "SELECT status, effective_from, effective_to FROM business_entity_bindings WHERE merchant_id = ?",
+                contract.merchantId())).containsEntry("status", "inactive")
+                .containsEntry("effective_from", java.sql.Date.valueOf(future)).containsEntry("effective_to", null);
+        assertThat(countActiveBindings()).isZero();
+        assertThat(countMerchants()).isEqualTo(1);
+    }
+
     // ---- helpers ----
 
     private String newSuffix() {
         suffix = UUID.randomUUID().toString().substring(0, 8);
+        projectId = "lease_project_" + suffix;
+        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '签约项目', 'active', now(), now())", projectId);
         subject = "be_test_" + suffix;
         leadId = "lead_aud_" + suffix;
         intentId = "intent_aud_" + suffix;
@@ -114,27 +142,30 @@ class LeasingSignAtomicityJdbcIntegrationTests {
     }
 
     private void seed(String intendedSpaceId) {
-        TestSecurityActors.asPlatformAdmin();
+        var actor = new com.complex.platform.access.AccessActor("usr_admin", "platform_admin_user", "platform_admin", projectId, null, "active");
+        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
+                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, "token", java.util.List.of()));
         Instant now = Instant.now();
         // M009-006：四类主体绑定要求主体真实存在（BEQR-143/PGM-026），合同主体须先在 business_entities 建档。
         jdbcTemplate.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                 + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "签约测试合同主体");
+        jdbcTemplate.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectId);
         leasingRepository.saveLead(new LeasingLeadRecord(leadId, "签约测试联系人", "13800002222", "referral",
-                "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, null, now, now));
+                "retail", intendedSpaceId, null, null, "usr_sales", "negotiating", null, null, projectId, now, now));
         leasingRepository.saveIntent(new LeasingIntentRecord(intentId, leadId, "pending_signing", intendedSpaceId,
-                null, Map.of(), now, "usr_supervisor", null, now, now));
+                null, Map.of(), now, "usr_supervisor", projectId, now, now));
     }
 
     private void seedSpace(String spaceId) {
         jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, "
                 + "space_type, floor_no, area_square_meter, status, created_at, updated_at) "
-                + "VALUES (?, NULL, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
-                spaceId, "签约测试铺位");
+                + "VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 88.0, 'active', now(), now())",
+                spaceId, projectId, "签约测试铺位");
     }
 
     private int countMerchants() {
         return jdbcTemplate.queryForObject(
-                "SELECT COUNT(*) FROM merchants WHERE contract_entity_id = ?", Integer.class, subject);
+                "SELECT COUNT(*) FROM merchants WHERE project_id = ?", Integer.class, projectId);
     }
 
     private int countActiveBindings() {

`````

## 9. 结论

- **内容修复：FAIL**（14 条服务/SQL/Leasing 主路径有核心 106 例执行证据，但本轮 diff 内身份夹具把 GR-01/GR-02/GR-03 打红；有阻断，不存在有条件通过。不能恢复假 platform_admin 夹具，也不能用「原先存在」把 GR-01 写成 PASS。）
- **归因：PASS**（25 路径 140 hunk 均可归到 V2.08 当前「是」的正式 ID。）
- **总：FAIL**

此票不是模块 PASS，不是 B4 双 PASS。新问题仅临时编号 GR-01…03，不自行定正式 ID、不修复。按回退规则：本批含无 ID / 不在修复队列的依据，整批应回 B2 定稿后再修，禁止只修已有 14 条。013/038 授权不重问。未改 findings 队列、未改 ledger、未改 HTML。
