CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B4-r1 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B4-r1（B3 第 1 次整批修复的完整 diff 复审）。2026-09-11T03:55:09Z
- before tree：`30fa8121f4f1bdfc79201aa516ecaa7233edee0e`；after tree：`dcfc3a8cc70a23e43ef3b47c668411f2c05af1b4`；`git diff --binary 30fa8121f4f1 dcfc3a8cc70a` 全文 42391 bytes SHA256 559063ead76569775cfaaea9de88a72621161c892c8a3bc7f72950fdc81afd09（全文附于本报告末尾）。
- 定稿依据 findings V1.85「当前 M013 B2-r1 双PASS定稿，8项待修（B3-r1）」：是＝001（绑定设计按 V410 刷新 + 登记 OpenAPI 契约缺口）、002（analytics.group.view / risk_level CHECK 按 V394 已闭合，五处）、003（自营标注＝合作方式 self_operated）、004（§6.2.1 字典硬校验口径 + parking）、005（端点表 DELETE 恒拒绝 + lifecycle）、006（税费登记句按 V217/SupervisorReview）、007（准入闸口现状：员工域已接线）、008（「考勤打卡」→「查看考勤」两处）；否＝009（XM→M392）、010。B3 累计 1/3。文档模块无测试面：验证＝改写后陈述逐字对照 main 事实与 HTML 现文（/tmp/m013_b3r1_run.log）。
- 锁四值：实算/sidecar/ledger/Grok 均 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。

## 1. diffstat

```
...iness-entity-binding-development-design-v0.1.md | 13 ++++++-------
 docs/group-governance-rebuild-prd-v1.0.md          | 13 ++++++++-----
 docs/platform-governance-acceptance-pack-v0.2.md   |  7 ++++---
 docs/platform-governance-prd-v0.1.md               | 22 +++++++++++++---------
 4 files changed, 31 insertions(+), 24 deletions(-)
```

## 2. 逐块归因（每一行改动都归到定稿 ID；「版本铁律」行随其所在文件的 ID）

| 文件 | 块 | 位置 | 块内标记 ID | 归因 |
|---|---|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 1 | `@@ -1,6 +1,6 @@` | — | 001（§3.1 当前迁移/字段表/差距段、§8 依据）；版本行随本文件 |
| docs/business-entity-binding-development-design-v0.1.md | 2 | `@@ -36,14 +36,13 @@` | — | 001（§3.1 当前迁移/字段表/差距段、§8 依据）；版本行随本文件 |
| docs/business-entity-binding-development-design-v0.1.md | 3 | `@@ -53,7 +52,7 @@` | M013-009 | 001（§3.1 当前迁移/字段表/差距段、§8 依据）；版本行随本文件 |
| docs/business-entity-binding-development-design-v0.1.md | 4 | `@@ -151,4 +150,4 @@` | — | 001（§3.1 当前迁移/字段表/差距段、§8 依据）；版本行随本文件 |
| docs/group-governance-rebuild-prd-v1.0.md | 1 | `@@ -1,4 +1,4 @@` | — | 002（§1 risk_level、§4 analytics.group.view）、005（§2 端点表）、006（§3 税费登记句）；标题版本与文末变更记录随本文件 |
| docs/group-governance-rebuild-prd-v1.0.md | 2 | `@@ -23,7 +23,7 @@` | — | 002（§1 risk_level、§4 analytics.group.view）、005（§2 端点表）、006（§3 税费登记句）；标题版本与文末变更记录随本文件 |
| docs/group-governance-rebuild-prd-v1.0.md | 3 | `@@ -41,7 +41,8 @@` | — | 002（§1 risk_level、§4 analytics.group.view）、005（§2 端点表）、006（§3 税费登记句）；标题版本与文末变更记录随本文件 |
| docs/group-governance-rebuild-prd-v1.0.md | 4 | `@@ -65,7 +66,7 @@` | — | 002（§1 risk_level、§4 analytics.group.view）、005（§2 端点表）、006（§3 税费登记句）；标题版本与文末变更记录随本文件 |
| docs/group-governance-rebuild-prd-v1.0.md | 5 | `@@ -106,5 +107,7 @@` | M013-002/005/006 | 002（§1 risk_level、§4 analytics.group.view）、005（§2 端点表）、006（§3 税费登记句）；标题版本与文末变更记录随本文件 |
| docs/platform-governance-acceptance-pack-v0.2.md | 1 | `@@ -1,4 +1,4 @@` | — | 002（§10A G7、§10 修订说明）；标题版本与 §11 变更日志行随本文件 |
| docs/platform-governance-acceptance-pack-v0.2.md | 2 | `@@ -178,7 +178,7 @@` | — | 002（§10A G7、§10 修订说明）；标题版本与 §11 变更日志行随本文件 |
| docs/platform-governance-acceptance-pack-v0.2.md | 3 | `@@ -190,12 +190,13 @@` | M013-002 | 002（§10A G7、§10 修订说明）；标题版本与 §11 变更日志行随本文件 |
| docs/platform-governance-prd-v0.1.md | 1 | `@@ -1,4 +1,4 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 2 | `@@ -208,7 +208,7 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 3 | `@@ -244,9 +244,9 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 4 | `@@ -256,8 +256,11 @@` | M013-004 | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 5 | `@@ -358,7 +361,7 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 6 | `@@ -371,7 +374,7 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 7 | `@@ -636,7 +639,7 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 8 | `@@ -651,7 +654,7 @@` | — | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |
| docs/platform-governance-prd-v0.1.md | 9 | `@@ -959,7 +962,8 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责` | M013-002/003/004/007/008 | 002（§12#15 GAP-20）、003（§6.9 导语、变更日志 2026-08-07/08 行订正）、004（§6.2.1 导语 + parking 行）、007（§12#8 现状句）、008（§5 终端表、§6.9 第 8 条）；标题版本与变更日志新行随本文件 |

## 3. 红绿证据（B3-r1 运行记录 /tmp/m013_b3r1_run.log 原文）

```
START=2026-09-11T03:39:04Z HEAD=5692a895 origin/main=5692a895
== 0. 4 docs after B3 (lines/bytes/sha)
docs/business-entity-binding-development-design-v0.1.md 153 13095 f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f
docs/group-governance-rebuild-prd-v1.0.md 113 19023 e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b
docs/platform-governance-acceptance-pack-v0.2.md 289 35245 ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56
docs/platform-governance-prd-v0.1.md 969 145630 aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413
== 1. versions
# 四类主体唯一绑定开发设计 V0.09
> 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
# 集团治理重建 PRD（V1.01·补断点）
# 平台治理验收包 V0.04
# 平台治理层产品需求 PRD V0.04
== 2. 001: binding design vs V410 / service
39:当前迁移：V005 基表 + V354 `archive_reason` 与 `entity_role` CHECK + V410 删除 `parent_entity_id` 并把 `e
55:- V318 曾把 `parent_entity_id` 用作固定层级、V354 的角色白名单曾含 HTML 没有的代码值；V410 �
9:ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;
17:ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
18:ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
19:    CHECK (entity_role IN ('platform', 'project_company', 'management_company'));
61:    public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
0
entity_role: type:string description:BusinessEntityRequest.entity_role字段；必须按本schema的类型、必填项、枚举、长度、单位和对应PRD规则校验。 enum: -operation -contract -settlement -invoice -payment -other maxLength:255 
== 3. 002: V394 facts vs 5 rewritten lines
ALTER TABLE permissions ADD CONSTRAINT chk_permissions_risk_level CHECK (risk_level IN ('low', 'medium', 'high', 'critical'));
UPDATE permissions SET requires_approval = TRUE
 WHERE permission_code IN ('analytics.group.view', 'customer.voucher.manage', 'finance.merchant_expense.manage',
                           'pii.contact_phone.view', 'wecom.sync');
docs/group-governance-rebuild-prd-v1.0.md:26:| 权限定义 `permissions` | `risk_level` | V394 已加 CHECK（low / medium / high / critica
docs/group-governance-rebuild-prd-v1.0.md:110:- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approva
docs/group-governance-rebuild-prd-v1.0.md:113:> 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` 
docs/platform-governance-acceptance-pack-v0.2.md:181:| G7 | 集团视图与跨项目汇总 | 已承接；实现缺口已闭合 | 已由 `gro
docs/platform-governance-acceptance-pack-v0.2.md:193:> 修订说明（2026-09-01 覆盖 2026-06-08 排期时态）：集团视图与跨项�
docs/platform-governance-acceptance-pack-v0.2.md:199:| 2026-09-11 | 改 | §10A G7 / §10 | 版本升 V0.04；集团视图审批属性缺口
docs/platform-governance-prd-v0.1.md:657:15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归�
docs/platform-governance-prd-v0.1.md:969:| 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；�
== 4. 003: self_operated
49:    public static final Set<String> MERCHANT_COOPERATION_FORMS =
0
364:> **两条注册路径（2026-08-05 R7 第三批用户裁决落地，依据 PGM「员工注册分两条路径」条�
965:| 2026-08-07/08 | 改 | §6.9 | 「公司持股超过 51% 的商户视同公司自有」整体废除，改为「在�
969:| 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风�
== 5. 004: space_type dictionary vs §6.2.1
                    String spaceType = requireText(request == null ? null : request.spaceType(), "space_type");
                    // 条目3 硬校验：space_type 必须是空间类型字典中处于 active 的取值（前端走下拉、后台可维护字典�
                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                "铺位类型必须是铺位类型字典中的启用值");
'sptype_building', 'building', 'active', 'sptype_floor', 'floor', 'active', 'sptype_zone', 'zone', 'active', 'sptype_shop', 'shop', 'active', 'sptype_public_area', 'public_area', 'active', 'sptype_parking', 'parking', 'active', 'sptype_booth', 'booth', 'active', 'sptype_other', 'other', 'active', 
247:业务空间对象为楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位，当前实现统一�
259:| 车位 / 车库 | `parking` | parent=楼层/区域 | OpenAPI 与字典均含；技术类型不新增业务空间对象 |
262:> 与字典差异（2026-09-11 M013-004）：V184 字典八码为 building / floor / zone / shop / public_area / parking / booth / other
969:| 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风险功能列表「考
== 6. 005: DELETE vs lifecycle
    @DeleteMapping("/position-role-templates/{id}")
    public ApiResponse<Void> deletePositionRoleTemplate(@PathVariable String id) {
        authorizationService.requirePermission("access.role.assign");
        service.deletePositionRoleTemplate(id);
        return ApiResponse.ok(null);
    public void deletePositionRoleTemplate(String id) {
        throw ApiException.conflict("ARCHIVE_APPROVAL_REQUIRED", "岗位模板须先存档，填写理由申请删除，经最高权限审�
    }
    @PostMapping("/position-role-templates/{id}/lifecycle")
495:                case "archive" -> {
499:                case "restore" -> {
504:                case "request_delete" -> {
517:                case "approve_delete", "reject_delete" -> {
1396:                            case "grant" -> AccessRepository.Effect.GRANT;
1397:                            case "deny" -> AccessRepository.Effect.DENY;
43:- `PUT /api/v1/access/position-role-templates/{id}`
44:- `DELETE /api/v1/access/position-role-templates/{id}`（恒返回 409 `ARCHIVE_APPROVAL_REQUIRED`，不是删除入口）
45:- `POST /api/v1/access/position-role-templates/{id}/lifecycle`（`action` = archive / restore / request_delete / approve_delete / reject_delete；存档→申请删除→最高权限审批的真实
== 7. 006: tax facts
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.tax_bracket.manage', 'Maintain payroll tax brackets (labor remuneration withholding table)', 'staff', 'high', TRUE, 'active')
49:    static final String TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust";
65:            new SupervisorReviewPolicyRecord("tax_rule_adjustment_l3", TAX_RULE_ADJUST_ACTION, 0, 3, true, List.of("reason", "before_afte
866:                || actionType.equals(TAX_RULE_ADJUST_ACTION);
0
69:    - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档�
== 8. 007: evaluate callers
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewController.java:47:        return ApiResponse.ok(serv
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:44:        var review = reviews.evaluate(new Sup
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:61:        var review = reviews.evaluate(new Sup
642:8. 合同或基础定义违规时，所有流程必须先卡住，但允许按后台配置的更高级审核层级�
== 9. 008: HTML current text vs PRD
同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发�
<!-- 文件版本：V0.12 -->
930:          <p cla 1865:                
211:| 员工私人手机 App | 员工自
377:8. 私人手机 App 只开放后台�
725:### 12B.2 考勤打卡
960:| 2026-08-04 | 改 | §12B.2 | 打�
969:| 2026-09-11 | 改 | §5 / §6.2.1 /
== 10. HTML untouched
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
 M docs/business-entity-binding-development-design-v0.1.md
 M docs/group-governance-rebuild-prd-v1.0.md
 M docs/platform-governance-acceptance-pack-v0.2.md
 M docs/platform-governance-prd-v0.1.md
?? docs/audit-2026-09/modules/M013-b1-grok-raw.md
?? docs/audit-2026-09/modules/M013-b1-self-raw.md
END=2026-09-11T03:39:05Z
```

## 4. 本席复审结论

见文末追加的判定段。

## 附：完整 diff（before→after，--binary）

```diff
diff --git a/docs/business-entity-binding-development-design-v0.1.md b/docs/business-entity-binding-development-design-v0.1.md
index c91bdf9e..ad294981 100644
--- a/docs/business-entity-binding-development-design-v0.1.md
+++ b/docs/business-entity-binding-development-design-v0.1.md
@@ -1,6 +1,6 @@
-# 四类主体唯一绑定开发设计 V0.08
+# 四类主体唯一绑定开发设计 V0.09
 
-> 版本：V0.08 ｜ 最后更新：2026-08-31 ｜ 状态：现行开发设计
+> 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
 > 业务最终解释权：`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。代码、迁移和接口只说明当前实现，不得覆盖三份 HTML。
 
 文档类型：Implementation  
@@ -36,14 +36,13 @@
 
 ### 3.1 `business_entities`
 
-当前迁移：V005 基表 + V318 `parent_entity_id` + V354 `archive_reason` 与 `entity_role` CHECK。
+当前迁移：V005 基表 + V354 `archive_reason` 与 `entity_role` CHECK + V410 删除 `parent_entity_id` 并把 `entity_role` 白名单收窄为 `platform` / `project_company` / `management_company`（V318 的父链列已由 V410 删除，只作历史）。
 
 | 当前字段 | 类型/约束 | 用途 |
 |---|---|---|
 | `id` | `VARCHAR(64)` PK | 工商主体标识 |
 | `entity_name` | `VARCHAR(120)` NOT NULL | 主体名称 |
-| `entity_role` | `VARCHAR(32)` + CHECK | 当前代码角色 |
-| `parent_entity_id` | `VARCHAR(64)`，松引用 | 当前代码中的父级关系 |
+| `entity_role` | `VARCHAR(32)` + CHECK（V410：platform / project_company / management_company） | 当前代码角色（`PlatformService.BUSINESS_ENTITY_ROLES` 同三值） |
 | `unified_social_credit_code` | `VARCHAR(128)` | 统一社会信用代码 |
 | `legal_representative` | `VARCHAR(255)` | 法定代表人 |
 | `bank_account_ref` | `TEXT` | 银行账户引用 |
@@ -53,7 +52,7 @@
 
 实现差距：
 
-- V318/V354 把 `parent_entity_id` 用作固定层级，且 V354 的角色白名单仍含 HTML 没有的代码值；业务读取不得据此推导固定行政隶属链，代码应按 HTML 的多对多协作关系收敛。
+- V318 曾把 `parent_entity_id` 用作固定层级、V354 的角色白名单曾含 HTML 没有的代码值；V410 已删除该列并把白名单收为 HTML 三角色，代码按 HTML 的多对多协作关系收敛，业务读取不得据历史列推导固定行政隶属链。OpenAPI `BusinessEntityRequest.entity_role` 枚举仍是旧六值（operation / contract / settlement / invoice / payment / other），与服务白名单不一致，属契约缺口（M013-009，归 OpenAPI 契约模块处理），不得当作活闭集。
 - 当前表没有 `project_id`、主体联系电话、证照附件或证照有效期字段。证照、资质和保险由合规到期台账承载，不得在本文虚构这些现行列。
 - HTML 的商户/运营主体业务状态为待准入、正常、暂停、终止、归档、黑名单；试运营是商户自己的经营状态，系统不记录。暂停与列入黑名单由招商部门与运营部门决定。退出不立即生效，须进入按项目可配的缓冲期（上下限出厂为 30 至 60 天，出厂默认 30 天）；缓冲期内向该商户客人说明情况，并给出转到其他酒吧、客人自行取走或退款三种处理方式。当前 `status` 未实现该完整词表和缓冲期闭环。
 
@@ -151,4 +150,4 @@
 - PGM「酒吧股权、牌照、场地和资金必须分别记录」
 - BSM「配置数据可以存档、也可以删除（删除要审批）」
 - ENUM「四类主体」「经营主体关系」「商户/运营主体状态」
-- 当前实现：V002、V005、V010、V318、V354；`PlatformController`、`PlatformService`、`JdbcPlatformRepository`
+- 当前实现：V002、V005、V010、V318、V354、V410（删父链、角色三值）；`PlatformController`、`PlatformService`、`JdbcPlatformRepository`
diff --git a/docs/group-governance-rebuild-prd-v1.0.md b/docs/group-governance-rebuild-prd-v1.0.md
index 49be5760..e1dfa2e9 100644
--- a/docs/group-governance-rebuild-prd-v1.0.md
+++ b/docs/group-governance-rebuild-prd-v1.0.md
@@ -1,4 +1,4 @@
-# 集团治理重建 PRD（V1.0·补断点）
+# 集团治理重建 PRD（V1.01·补断点）
 
 > **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。
 
@@ -23,7 +23,7 @@
 | 授权申请 `access_grant_requests` | `request_type` | `user_role` / `user_position` / `temporary_permission` / `role_permission` | DB CHECK 预留 4 类；当前 `AccessService` 实际支持前三类，`role_permission` 服务层未开放 |
 | 授权申请 `access_grant_requests` | `status` | `pending` / `applied` / `rejected` | 待审批 / 已应用 / 已驳回 |
 | 权限定义 `permissions` | `requires_approval` | `true` / `false` | 实现字段；五类 HTML 强制高危权限必须为 `true`，后台只能新增高危项或提高审批等级，不能关闭强制项 |
-| 权限定义 `permissions` | `risk_level` | 现有表无 CHECK；服务按 `critical` / `high` / 其他分档 | `critical` → l3 / 最高审批；`high` → l2 / 上两级；其他 → l1 / 上一级 |
+| 权限定义 `permissions` | `risk_level` | V394 已加 CHECK（low / medium / high / critical）；服务按 `critical` / `high` / 其他分档 | `critical` → l3 / 最高审批；`high` → l2 / 上两级；其他 → l1 / 上一级 |
 | 临时授权 `temporary_permission_grants` | `status` | 现有表无 CHECK；V170 默认写 `active` | 到期由有效权限查询按 `expires_at` 过滤；撤销写 `revoked_by/revoked_at`，单次授权写 `single_use/consumed/consumed_at` |
 | 集团总览 API 参数 | `groupBy` | `project` / `entity` | Controller/Service 校验；非法返回 `VALIDATION_FAILED` |
 
@@ -41,7 +41,8 @@
 - `GET /api/v1/access/position-role-templates`
 - `POST /api/v1/access/position-role-templates`
 - `PUT /api/v1/access/position-role-templates/{id}`
-- `DELETE /api/v1/access/position-role-templates/{id}`
+- `DELETE /api/v1/access/position-role-templates/{id}`（恒返回 409 `ARCHIVE_APPROVAL_REQUIRED`，不是删除入口）
+- `POST /api/v1/access/position-role-templates/{id}/lifecycle`（`action` = archive / restore / request_delete / approve_delete / reject_delete；存档→申请删除→最高权限审批的真实路径）
 - `PUT /api/v1/access/users/{id}/position`
 - `POST /api/v1/access/role-assignments`
 - `POST /api/v1/access/role-assignments/unassign`
@@ -65,7 +66,7 @@
   - 敏感经营数据：不限于内部结算价和成本价，还包括采购价、毛利、分账底价、域内补贴缺口、供应链价格及其他被标记为敏感的数据。查看、导出和修改分别授权；普通用户、会员、前线员工和无相应权限的商户人员不得查看。系统自动使用某价格不代表当前操作人获得查看或修改权限。数据默认按项目隔离，只有项目之上的获授权岗位才能跨项目查看。
   - 高危业务：如 `bar.product.sold_out`、`bar.bill.reopen`、`bar.order_item.void`、`bar.saved_bottle.manage`、`ticket.verify.reverse`、`amusement.verification.override`、`amusement.over_capacity_policy.configure`。
   - **第 5 类 税费规则与税费调整（2026-08-04 对齐平台治理地图/枚举速查表）**：所有税费规则和税费调整均须经过**财务审核、税务审核、法务审核**，再由**顶层平台公司的老板最终确认后生效**；**任一环节驳回均不得生效**；**提交人不得审批自己提交的调整**；全部审批意见和时间必须留痕。**税率、税目不属于普通后台配置项**，不得按普通配置项直接改动。
-    - **实现待对齐（2026-08-04 登记）**：当前实现**无税费类 `requires_approval=true` 权限码**（全库 `*.sql`/`*.java` 检索无 `finance.tax` / `tax_rule` 命中），本条 HTML 口径在代码侧尚无对应权限码落点，**记为实现待对齐**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
+    - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档权限码 `staff.tax_bracket.manage`（V217，`risk_level=high`、`requires_approval=true`；属薪资个税档，不是本条第 5 类「税费规则与税费调整」）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 复核动作（策略 tax_rule_adjustment_l3，不是 `permissions` 行），但本条 HTML「税费规则与税费调整」在 `permissions` 侧尚无 `finance.tax_*` 权限码落点，**记为实现待对齐（范围收窄为该权限码）**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
 - 申请类型当前服务支持三类：`user_role`、`user_position`、`temporary_permission`。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支。
 - 申请时服务先解析目标授权会授出的权限码，只允许包含 `requires_approval=true` 的授权申请；若没有审批必需权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权。
 - 审批链不由发起人选择档位，也不按风险值自动映射成可选档或指定角色。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现把 `critical`/`high`/其他 写成 `permission_grant_l3`/`l2`/`l1` 与 `top_authority`/`two_levels_up`/`one_level_up`，属实现证据，不是业务可选档。
@@ -106,5 +107,7 @@
 **迁移 / 表 / 权限**
 - 迁移：权限来自 `V115__analytics_group_overview_permission.sql`；职位授权闭环来自 V169；高危授权审批来自 V170
 - 表：`analytics_business_metric_snapshots`、`projects`、`merchants.business_entity_id`
-- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=false`）。**实现待对齐（2026-08-04 F1 登记）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；当前实现为 `false`，与 §3「不得把 HTML 明确的强制项降级为无需审批」直接冲突，**属实现缺口**。按事实源优先级不反向改写 PRD，留待实现批次补齐；在补齐前，§4 的 `requirePlatformScope()` 只收敛可见范围、不替代授权审批。
+- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**实现待对齐（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；§4 的 `requirePlatformScope()` 只收敛可见范围、不替代授权审批。
 - UI：`ADM-ANA-002` 集团总览；可通过 `ADM-ACC-003` 的职位模板授出或收回 `analytics.group.view`
+
+> 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` CHECK 与 §4 `analytics.group.view` 审批属性按 V394 更新为已闭合；§2 关键端点补 lifecycle 并标注 DELETE 恒拒绝；§3 税费权限码登记句按 V217 / SupervisorReview 现状改写。
diff --git a/docs/platform-governance-acceptance-pack-v0.2.md b/docs/platform-governance-acceptance-pack-v0.2.md
index 7185a6fc..dbdfc245 100644
--- a/docs/platform-governance-acceptance-pack-v0.2.md
+++ b/docs/platform-governance-acceptance-pack-v0.2.md
@@ -1,4 +1,4 @@
-# 平台治理验收包 V0.03
+# 平台治理验收包 V0.04
 
 > 合并说明：本文按 2026-06-10 全量文档收口与合并归档审计执行，用于替代下列分散源文件。源文件已移入 `_archive/20260610-retention-audit/merged-layer-spec-sources/`，后续开发、审计和交接以本文为入口。
 
@@ -178,7 +178,7 @@
 | G4 | 空间/铺位建模 | 部分 | 空间对象须按 GOV 既有口径收敛 | P0 按楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位建模；铺位按 HTML 已定义口径管理，不另立区块层级 |
 | G5 | 合同审批边界 | 已收敛 | 原"P0 仅单级确认、多级审批 P1"与 GOV 多级模型冲突 | 合同新建、到期终止、变更三类各走各自的审批流程模板、模板分别可配（业务参数，按综合体可配，须同时配置上下限），各类阶段数与审批岗位可以不同；每类均按统一审批模型执行：草稿提交时指定这条主线的最终审批人（默认总经理），系统按组织层级逐级自动补全；业务、财务、法务为平行的相关部门线，各指定该部门最终审批人并补全至该部门预先设定的最低起点层级；主线与各条平行线全部通过合同才生效，任一节点驳回则整单退回起草。三类均不得录入即生效：新建未审批不生效、到期不得无审批自动终止、变更未审批不生效。产品细化见 PRD §12 已确认第 18 条 |
 | G6 | 高级审批级别阶梯 | 已补 | 原把"指定角色"与 level_rank 计算写成已建模缺口 | 收口为 GOV 三档：上一级、上两级、最高权限。最高权限是持有授权 U 盘密钥的那一个人，不按职级档位解析；不设指定角色档 |
-| G7 | 集团视图与跨项目汇总 | 已承接；实现有已登记缺口 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性实现缺口按集团治理 PRD 登记，不反向改写业务口径 |
+| G7 | 集团视图与跨项目汇总 | 已承接；实现缺口已闭合 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD §4 同步），不反向改写业务口径 |
 
 > 待批准事项：员工数据处理与监控告知的同意凭据尚未写入三份 HTML，不作为本验收包必过项，也不得升为必填字段或注册流程，不另造替换 consent 字段。
 
@@ -190,12 +190,13 @@
 
 > 修订说明（2026-06-07，status-6）：本清单不再做"全绿即通过"的一次性自评。模块覆盖、状态一致性以第 4 节、第 7 节如实标注为准，未闭合项集中见第 10A 节。
 >
-> 修订说明（2026-09-01 覆盖 2026-06-08 排期时态）：集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接；当前 `analytics.group.view` 审批属性缺口继续如实登记，不把已承接能力写回 P2 待办。
+> 修订说明（2026-09-01 覆盖 2026-06-08 排期时态）：集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接；`analytics.group.view` 审批属性缺口已由 V394 闭合（`requires_approval=true`，2026-09-11 M013 核对），不把已承接能力写回 P2 待办。
 
 ## 11. 变更日志
 
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
+| 2026-09-11 | 改 | §10A G7 / §10 | 版本升 V0.04；集团视图审批属性缺口按 V394 改为已闭合 | 160 全量审计 M013-002 |
 | 2026-09-01 | 增/改 | §2 / §2A / §10A G7 | 版本升 V0.03；补金额精度、合同本地 AI、合同当地时间和外部经销商全链验收；集团视图统一为已承接并保留实现缺口 | M01 全量复审 ADV-01/02/04/05/08；三份业务 HTML 现文 |
 | 2026-06-07 | 改 | §6 终端开关页验收 | 终端枚举改为 10 端，对齐字段字典 §3.1 | D5、govfam-3；本轮已再对齐十一端 |
 | 2026-06-07 | 改 | §7 终端开关状态 | 去掉无支撑的"定时启用/定时停用"独立态，改为派生过渡态 | govfam-4 |
diff --git a/docs/platform-governance-prd-v0.1.md b/docs/platform-governance-prd-v0.1.md
index 3f3c1d73..29d77349 100644
--- a/docs/platform-governance-prd-v0.1.md
+++ b/docs/platform-governance-prd-v0.1.md
@@ -1,4 +1,4 @@
-# 平台治理层产品需求 PRD V0.03
+# 平台治理层产品需求 PRD V0.04
 
 > 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。**三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决**（2026-08-05 R7 补，依据 PGM「解释权声明」条 / DIFF-A-051）。
 
@@ -208,7 +208,7 @@
 | 合作模式 | 自营、租赁、联营、代运营、服务商接入、其他六类合作类型 | 影响合同、权限、结算和数据归属 |
 | 结算口径摘要 | 合同层记录的结算原则摘要 | 为后续财务系统提供依据，不负责具体结算计算 |
 | 员工 | 平台、商户或服务商下的工作人员 | 关联组织、岗位、班次、设备、权限、职级、薪资口径和留痕 |
-| 员工私人手机 App | 员工自有手机上的轻量 App | 用于注册、通知、状态查看、设备凭证绑定和审批结果查看，并开放六项低风险功能（含但不限于，PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条为开放列举；BSM「员工私人手机 App」条另含看排班）（公告、我的工单、帮客人订位、考勤打卡、请假、查自己薪资，见 §6.9 第 8 条）；不承担现场核销、点单、放行、结算、补录 |
+| 员工私人手机 App | 员工自有手机上的轻量 App | 用于注册、通知、状态查看、设备凭证绑定和审批结果查看，并开放六项低风险功能（含但不限于，PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条为开放列举；BSM「员工私人手机 App」条另含看排班）（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资，见 §6.9 第 8 条）；不承担现场核销、点单、放行、结算、补录 |
 | 设备凭证（设备公钥/不可逆设备指纹） | 手机专属的设备标识，不读取硬件识别码。Keystore 设备公钥/不可逆设备指纹属未立法实现证据，不得升为唯一可信标识或已确认规则 | 关联员工、私人手机 App、公司工作手机、解绑重绑记录和审批状态 |
 | 岗位 | 员工职责类型，如运营、财务、店长、核销员、服务员、巡检员 | 关联权限、职级、班次、空间、设备和薪资口径 |
 | 班次 | 员工在特定时间段内的工作安排 | 关联岗位、区域、设备领用和责任链 |
@@ -244,9 +244,9 @@
 
 ### 6.2.1 空间业务名词与 space_type 枚举对照
 
-业务空间对象为楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位，当前实现统一落在 `spaces` 自引用树及 `space_type` 字段上。OpenAPI `SpaceRequest.space_type` 是当前实现枚举，不拥有高于三份业务 HTML 的解释权，对照如下：
+业务空间对象为楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位，当前实现统一落在 `spaces` 自引用树及 `space_type` 字段上。当前实现的活校验是空间类型字典 `space_type_dictionary`（V184 种子 building / floor / zone / shop / public_area / parking / booth / other，后台可维护；`createSpace` 只接受字典中启用的 `type_code`）；OpenAPI `SpaceRequest.space_type` 枚举与下表只是契约快照、不是活闭集，也不拥有高于三份业务 HTML 的解释权，对照如下：
 
-| 业务名词（HTML / 本 PRD） | spaces.space_type | 层级与关键字段 | 说明 |
+| 业务名词（HTML / 本 PRD） | space_type（OpenAPI 契约快照，非活码） | 层级与关键字段 | 说明 |
 |---|---|---|---|
 | 综合体项目 | 非 spaces（`projects` 表） | `spaces.project_id` 指向 | 所有空间必须归属某综合体项目 |
 | 综合体空间树根（技术节点） | `mall` | 树根，`parent_space_id` 为空 | 不新增业务空间层级 |
@@ -256,8 +256,11 @@
 | 摊位 / 快闪位 | `booth` | parent=区域 | 技术类型不新增业务空间对象 |
 | 卡座 / 吧台位 | `bar_table` | parent=区域 | 物理点位口径，运营桌台见酒吧域 |
 | 游乐入口 / 库房 / 设备点位 | `facility` | parent=楼层/区域 | 供入口、库房和设备绑定 |
+| 车位 / 车库 | `parking` | parent=楼层/区域 | OpenAPI 与字典均含；技术类型不新增业务空间对象 |
 | 其他点位 | `other` | — | 入口、摄像头点位等未归类点位 |
 
+> 与字典差异（2026-09-11 M013-004）：V184 字典八码为 building / floor / zone / shop / public_area / parking / booth / other；表中 `mall` / `bar_table` / `facility` 不在字典中、不是 `createSpace` 活码，`building` / `public_area` 不在 OpenAPI 枚举中；实现以字典为准，本表只对照业务名词。
+
 字段补充：`polygon_points` 存平面图多边形，`area_square_meter` 存面积。
 合同绑定：合同经营范围与铺位绑定走 contract-space scope（迁移 `V080__contract_scope_and_spaces`、`V162`；空间出租状态见 `SpaceRentalStatusRecord`，用于防止同一铺位重复出租）。
 
@@ -358,7 +361,7 @@
 
 > 修订说明（2026-06-07 收口）：本节按 D8/A6 改写——废止"读取手机本机 NFC 唯一标识"，私人手机身份改为"安装期生成并存于设备 Keystore 的设备公钥/不可逆设备指纹（device_key_fingerprint）"；仅持久化不可逆哈希/公钥指纹，不存原始标识（PIPL 数据最小化，compliance-4/C4）。依据 finding compliance-land-9/decision-land-1、D8 条。
 
-> **两条注册路径（2026-08-05 R7 第三批用户裁决落地，依据 PGM「员工注册分两条路径」条、BSM「新员工注册分两条路径」条）**：本节 1–7 步描述的**自助注册路径只适用于公司自有员工**（本人在私人手机 App 注册、提交资料后由所属公司后台审批）；**商户员工不走自助注册**，由**商户代该员工提交注册申请**，经**对应域公司**批准后账号生效——城市项目公司、娱乐平台公司或游乐公司，按该商户所属经营域确定。**在商户档案上标注为自营的商户视同公司自有**，其员工按公司自有员工路径办理（2026-08-07/08 用户裁决：「持股超过 51% 视同公司自有」整体废除，改为按商户档案上的自营标注判定；51% 既非参数也非门槛。**商户档案的「自营」标注字段属代码轮新增待办**）。
+> **两条注册路径（2026-08-05 R7 第三批用户裁决落地，依据 PGM「员工注册分两条路径」条、BSM「新员工注册分两条路径」条）**：本节 1–7 步描述的**自助注册路径只适用于公司自有员工**（本人在私人手机 App 注册、提交资料后由所属公司后台审批）；**商户员工不走自助注册**，由**商户代该员工提交注册申请**，经**对应域公司**批准后账号生效——城市项目公司、娱乐平台公司或游乐公司，按该商户所属经营域确定。**在商户档案上标注为自营的商户视同公司自有**，其员工按公司自有员工路径办理（2026-08-07/08 用户裁决：「持股超过 51% 视同公司自有」整体废除，改为按商户档案上的自营标注判定；51% 既非参数也非门槛。**商户档案的「自营」标注即合作方式 `self_operated`（既有字段；U04-001 裁决不新增独立字段或接口）**）。
 
 1. 员工在私人手机上安装轻量 App。
 2. 员工提交姓名、手机号、身份证明、所属组织、岗位申请、紧急联系人等注册信息。
@@ -371,7 +374,7 @@
 6. 系统建立员工私人手机、设备凭证（手机专属的设备标识，不读取硬件识别码）、公司发放工作手机之间的一一对应关系。【已废止/不得回灌】「设备公钥/设备指纹」作凭证定义。
 7. 员工换手机时必须先解绑旧手机设备凭证，再绑定新手机，解绑和重绑都要留痕。
 7a. **同一账号多端同时登录（2026-08-19 补，依据 BSM“同一账号允许在多台设备、多个端同时登录”条现文；DEC-R8-97-19）**：**同一账号允许在多台设备、多个端同时登录**，不设同端挤下线；**收银、核销、收款、放行等现场动作，一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕**；**长短款补足、小费归属、提成归属等责任认定按动作发生时的会话记录判定，不因账号同时在别处登录而转移**。主条与全文口径见 `operation-traceability-and-terminal-switch-prd-v0.1.md` §2.2／§2.4。
-8. 私人手机 App 只开放后台按设备类型放权的低风险功能，共六项（含但不限于，PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条为开放列举；BSM「员工私人手机 App」条另含看排班）：**公告、我的工单、帮客人订位、考勤打卡、请假、查自己薪资**；此外可收发通知、查看班次、查看审批状态和个人状态。**不允许现场核销、点单、放行、结算、补录**。（依据 PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条「员工自有手机只开放低风险功能」、BSM「员工手机端」条）
+8. 私人手机 App 只开放后台按设备类型放权的低风险功能，共六项（含但不限于，PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条为开放列举；BSM「员工私人手机 App」条另含看排班）：**公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资**；此外可收发通知、查看班次、查看审批状态和个人状态。**不允许现场核销、点单、放行、结算、补录**。（依据 PGM「同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样」条「员工自有手机只开放低风险功能」、BSM「员工手机端」条）
 8A. 「帮客人订位」的业务口径（依据 BSM「帮客人订位」条·条目名主锚点；BSM 已于 2026-08-04 更正为“爽约”（DEC-R6-15），两侧用词一致，原「BSM 原文写作“爬约”」的差异说明已失效并删除）：
    - 员工代客预订卡座/散台**不收定金**；
    - 代订**必须填写最晚到店时间**；
@@ -636,7 +639,7 @@
 5. 私人手机寄存柜不固定，流动使用，先到先选，离开时全部拿走。
 6. 职级可按 M1-M10，并可分上、中、下；职级只绑定薪资大范围，具体薪资单独填写。
 7. 工作手机预装软件清单和网站白名单由后台维护，通过 Android Enterprise / Android Management API + 商业 MDM 阻止违规访问并留痕。
-8. 合同或基础定义违规时，所有流程必须先卡住，但允许按后台配置的更高级审核层级放行；其中权限不足不设放行通道，须换由有该权限的人办理。> 现状/目标（2026-06-18 收口，R5，本期 P0）：准入闸口（超范围→卡住→升级→放行）须在票务/酒吧/游乐/结算业务动作前内嵌校验；当前 `SupervisorReviewService.evaluate` 为孤立评估接口，业务域动作前尚未调用——目标为动作前强制校验。> 两个闸口相互独立（2026-07-04 澄清）：**准入闸口**校验的是「业务动作是否在合同/空间/资质允许范围内」（domain=合同/空间/资质准入），**高危权限审批闸口**校验的是「操作人是否有权发起此高危动作」（domain=操作人权限，即 group-governance §3 的 requires_approval 授权审批）。二者都以 `supervisor_review` 为统一审批载体、都可升级放行，但触发条件和 domain 不同、互不替代：一次高危业务动作可能同时经过「操作人有没有权」与「业务在不在准入范围」两道校验。本条以下逐域切入点只描述**准入闸口**。
+8. 合同或基础定义违规时，所有流程必须先卡住，但允许按后台配置的更高级审核层级放行；其中权限不足不设放行通道，须换由有该权限的人办理。> 现状/目标（2026-06-18 收口，R5，本期 P0）：准入闸口（超范围→卡住→升级→放行）须在票务/酒吧/游乐/结算业务动作前内嵌校验；当前 `SupervisorReviewService.evaluate` 已在员工域两处动作前调用（`StaffApprovalService`：排班发布、处罚上限申请），票务/酒吧/游乐/结算四域动作前尚未调用——目标为动作前强制校验。> 两个闸口相互独立（2026-07-04 澄清）：**准入闸口**校验的是「业务动作是否在合同/空间/资质允许范围内」（domain=合同/空间/资质准入），**高危权限审批闸口**校验的是「操作人是否有权发起此高危动作」（domain=操作人权限，即 group-governance §3 的 requires_approval 授权审批）。二者都以 `supervisor_review` 为统一审批载体、都可升级放行，但触发条件和 domain 不同、互不替代：一次高危业务动作可能同时经过「操作人有没有权」与「业务在不在准入范围」两道校验。本条以下逐域切入点只描述**准入闸口**。
 > 准入校验逐域切入点（2026-06-18 D2 逐域细化，本期 P0）：在上述总述基础上逐域明确强制校验插入位置，业务系统执行关键动作前必须先调用准入闸口（统一走 `supervisor_review`，不再引入 `admission_audits`/`contract_admission_rules` 旧表）；任一域校验不通过→阻断动作并写留痕→按上一级、上两级或最高权限升级审批→放行落 `supervisor_review_decisions`：
 >   - 票务：下单前 + 出票前两处内嵌校验（超合同、超经营范围、资质过期则卡住，升级审批通过后放行）；校验不通过→阻断并写留痕→按上一级、上两级或最高权限升级审批→放行落 `supervisor_review_decisions`。
 >   - 酒吧：开台前 + 点酒/结算前两处内嵌校验（超合同、超经营范围、资质过期则卡住，升级审批通过后放行）；校验不通过→阻断并写留痕→按上一级、上两级或最高权限升级审批→放行落 `supervisor_review_decisions`。
@@ -651,7 +654,7 @@
 > **（2026-07-20 安全修复 AUD-011，补记于第 14 条）权限模板应用不是高危授权审批的例外**：`applyPermissionTemplate` 现与角色/岗位/临时授权走同一政策引擎 `requireGrantApprovalIfNeeded`——模板含任一 `requires_approval` 权限时，经模板入口批量应用一律 fail-closed 拒绝（`GRANT_REQUIRES_APPROVAL` 403），不得绕过职责分离/主管复核。高风险授权须改走 `applyGrant → reviewGrant` 主管复核流（该流按权限快照建待审请求、批准后再原子应用）；模板入口对高风险直接拒绝、无待审窗口，故审批期间模板内容变化亦无从偷换。
 14A. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、**税费规则与税费调整**（共“五类”）——上述权限授出前必须先审批才生效；配置只允许新增高危项或提高审批等级，不得关闭这些强制闸门。授权申请人与审批人必须职责分离，申请人不能审批自己的授权申请。**持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不得审批自己的单据。税费规则与税费调整的审批链固定为：财务审核 → 税务审核 → 法务审核 → 顶层平台公司老板最终确认后生效**；任一环节驳回均不得生效，提交人不得审批自己提交的调整，全部审批意见和时间必须留痕；税率、税目不属于普通后台配置项。（依据 PGM「高危授权审批（已确认）」条、ENUM「税费配置」条；与 §7.8.1 及 `group-governance-rebuild-prd-v1.0.md` §3 口径一致）
 14B. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更后必须立即按变更后的岗位权限模板重算有效权限并使旧岗位或旧范围派生权限失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效直至审批通过。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。本条不把仅职级变化或任意手工模板应用列为触发条件（DEC-R8-91）。
-15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（该承接存在 GAP-20：`analytics.group.view` `requires_approval=false` 待对齐，未完全闭环），**不再是本 PRD 的未来待办**。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
+15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**不再是本 PRD 的未来待办**。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
 16. 所有端都必须有开关接口，支持按全局、综合体、商户、业务系统、模块、版本/设备、灰度控制，高层级关闭压倒低层级打开；关闭后该端或模块不可用，打开后仍受权限、数据范围和设备状态控制；与钱有关的规则（分账比例、结算口径、综合服务费、租金与抽成规则）一律不得使用灰度、不得按商户或设备部分开放，只能按综合体整体统一切换，切换日期由后台配置（业务参数，按综合体统一可配，须同时配置上下限），切换前后各自适用完整的一套规则；灰度只用于不影响金额计算的功能开放。同一套规则之下的参数取值按三份业务 HTML 为该参数写明的粒度配置，不属于灰度或部分开放。开关缺配置行一律默认关闭（opt-in），不得由代码或部署脚本视同打开；开关服务、支付通路、核销与入场等营业必需的能力不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败按兜底关闭处理、写操作一律拒绝，只有留痕不受兜底关闭影响。终端类型为受控十一类（客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户），产品细化见 `platform-governance-product-spec-v0.2.md`「字段字典」部分 §3.1「终端开关字段」，最终业务边界仍以三份业务 HTML 为准。
 17. 所有有过程的业务动作必须留痕，订单获取、销售归属、卡座服务、异常第一处理人，以及酒水和可追责物品从库房到客人的交接过程必须能追溯责任人、设备、地点、时间和结果。已拍板例外：安检环节不产生按人记录，留痕与溯源以设备侧记录为终点。
 18. 合同生命周期统一为「信息录入/发起 → 可配置审批流 → 审批通过后生效」：新建合同审批通过后才生效；到期或终止审批通过后才终止；变更（续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权、位置变更）审批通过后才变更或终止落地。审批流模板可配，一律按全平台统一的审批模型执行：指定主线最终审批人（默认总经理）＋系统逐级自动补全，业务/财务/法务为平行的相关部门线、各指定该部门最终审批人并补全至该部门预设的最低起点层级，主线与各平行线都通过才生效；任一节点驳回则整单退回起草；超时未处理自动提醒并升级到上一级、不得默认通过；全程保留原因、版本快照与审计。（2026-08-04 用户裁决改写）
@@ -959,7 +962,8 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责
 | 2026-08-04 | 改 | §6.5.1 第 4 步、§12 第 18 条 | 合同审批由「业务/财务/法务同阶段并行会签」改写为全平台统一审批模型：指定主线最终审批人（默认总经理）＋逐级自动补全；业务/财务/法务为平行相关部门线、各指定该部门最终审批人并补全至该部门预设的最低起点层级；主线与各平行线都通过才生效；任一节点驳回退回起草、超时升级、三类模板可配均保留 | 用户 2026-08-04 逐项批准（DEC-R5-2026-08-04-06）；PGM「审批链不由发起人选择档位」条、ENUM「合同审批链」条、BSM「审批：合同与合同变更走同一条全平台统一的审批链」条（CL-54；行号 2026-08-04 F1 复核更正，原记 PGM L520/L524、ENUM L182、BSM L1195） |
 | 2026-08-05 R7 第三批 | 改 | §15A.1 | 「客人的姓名与手机号一律脱敏，不设例外」改为「默认脱敏 + 商户查询与导出本商户客户信息不脱敏」例外口径；未脱敏原文导出边界收窄为跨商户/平台侧批量场景 | 2026-08-05 用户裁决；PGM「导出文件是否脱敏」条；来源判定按 BSM「账户注销」条 |
 | 2026-08-05 R7 第三批 | 增 | §6.9 | 补「两条注册路径」前置说明：自助注册仅适用公司自有员工，商户员工由商户代提交、经对应域公司批准；持股超 51% 的商户视同公司自有 | 2026-08-05 用户裁决；PGM「员工注册分两条路径」条、BSM「新员工注册分两条路径」条 |
-| 2026-08-07/08 | 改 | §6.9 | 「公司持股超过 51% 的商户视同公司自有」整体废除，改为「在商户档案上标注为自营的商户视同公司自有」；51% 既非业务参数也非判定门槛。商户档案的「自营」标注字段属代码轮新增待办 | 用户 2026-08-07/08 弹窗裁决（第六批审查后三裁之②）；DEC-R8-82；PGM「员工注册分两条路径」条、BSM「新员工注册分两条路径」条 |
+| 2026-08-07/08 | 改 | §6.9 | 「公司持股超过 51% 的商户视同公司自有」整体废除，改为「在商户档案上标注为自营的商户视同公司自有」；51% 既非业务参数也非判定门槛。商户档案的「自营」标注字段属代码轮新增待办（2026-09-11 订正：该待办作废，自营标注即合作方式 `self_operated`，U04-001） | 用户 2026-08-07/08 弹窗裁决（第六批审查后三裁之②）；DEC-R8-82；PGM「员工注册分两条路径」条、BSM「新员工注册分两条路径」条 |
 | 2026-08-10 | 改 | §6.10 / §7.8 / §12 | 保存岗位、职责、所属组织或所属商户变更后按新岗位模板立即重算；模板内权限免审生效、超模板增权待批；排除仅职级变化和任意手工模板应用 | DEC-R8-91（R13-049） |
 | 2026-08-29 | 改 | §7.3 / §12#10 / §12#5 / §15.2 / §15.4 | 业态去掉是否核心/第三方接入/统计分类/状态；废止多类对象统一待复核；告知/同意不得升为必填或注册流程，不另造 consent_* | M01 ADV-017 / ADV-003 |
 | 2026-08-29 | 改 | §6.3 / §7.2 / §7.3 | 业态收回 ENUM 六值+单一归属，废止维护/绑定开关/复合业态；废止空间「是否可留痕」开关 | M01 R-007 / R-008 |
+| 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风险功能列表「考勤打卡」改「查看考勤」（治理地图 V0.12 L741、业务地图 V0.14 L930/L1865/L1445）；§6.2.1 改为字典硬校验口径并补 `parking`；自营标注改为合作方式 `self_operated` 既有字段；准入闸口现状改为员工域已接线、四域未接；GAP-20 按 V394 闭合 | 160 全量审计 M013-002/003/004/007/008；PGM L741、BSM L930/L1865/L1445 现文 |

```


## 4. 本席复审结论（席甲，作者自审票）

本席逐块读完上述 diff 全文（4 文件），并以 §2 归因表核对每一行；本模块无测试面，§3 为只读事实对照记录（改写后的每条陈述对 main 源码/迁移/OpenAPI 与三份 HTML 现文逐字核对）。

### 4.1 修复内容（逐 ID）

| ID | 修复内容 | 证据（§3 原文） | 本席票 |
|---|---|---|---|
| 001 | 绑定设计 §3.1「当前迁移」改为 V005 + V354 + V410（删父链、角色三值）；字段表删 `parent_entity_id` 行、`entity_role` 行标 V410 三值与 `BUSINESS_ENTITY_ROLES`；差距段改为父链已删 + OpenAPI `entity_role` 旧六值登记为契约缺口（M013-009）；§8 当前实现补 V410 | V410 `DROP COLUMN IF EXISTS parent_entity_id` + CHECK 三值；`BUSINESS_ENTITY_ROLES` 三值；`BusinessEntityRecord` 0 处 parent；OpenAPI enum 六旧值原文 | PASS |
| 002 | 集团 PRD L26 `risk_level` 改「V394 已加 CHECK」、L109 `requires_approval=true`（V394 已置）且缺口句改已闭合；验收包 G7 与修订说明改已闭合；PRD §12#15 GAP-20 改已闭合 | V394:15 `chk_permissions_risk_level`、V394:16-18 `UPDATE … requires_approval = TRUE … 'analytics.group.view'`；V4* 无回写 | PASS |
| 003 | PRD §6.9 导语「自营标注字段属代码轮新增待办」→「自营标注即合作方式 `self_operated`（既有字段；U04-001 不新增）」；变更日志 2026-08-07/08 行加订正括注 | `MERCHANT_COOPERATION_FORMS` 含 self_operated；生产代码 selfOperated/is_self_operated 0 处；questions/U04-001.md 答「删除」 | PASS |
| 004 | PRD §6.2.1 导语改「活校验＝空间类型字典（V184 八码）；OpenAPI 与表只是契约快照」；表补 `parking` 行 | `PlatformService:474-478` `listActiveSpaceTypeCodes()` 硬校验；V184 八码；OpenAPI enum 含 parking | PASS |
| 005 | 集团 PRD §2 端点表 DELETE 标「恒返回 409 ARCHIVE_APPROVAL_REQUIRED，不是删除入口」，补 lifecycle 行（五个 action） | `AccessController:204-208` → `AccessService:479-481` 无条件 throw；`:192` lifecycle；`case` 分支 archive/restore/request_delete/approve_delete/reject_delete | PASS |
| 006 | 集团 PRD §3 税费登记句改为现状：V217 `staff.tax_bracket.manage`（high、requires_approval=TRUE）已有、`finance.tax_rule.adjust` 审批动作已落策略、`finance.tax_*` 权限码仍缺（范围收窄） | V217:43-45；`SupervisorReviewService:49` 与策略；迁移 `'finance.tax` 0 命中 | PASS |
| 007 | PRD §12#8 现状句改「员工域两处已接线（排班发布、处罚上限），票务/酒吧/游乐/结算四域未接」 | `StaffApprovalService.java:44,61` `reviews.evaluate`；其它域 0 处调用 | PASS |
| 008 | PRD §5 终端表与 §6.9 第 8 条「考勤打卡」→「查看考勤」 | 治理地图 V0.12 L741、业务地图 V0.14 L930/L1865 现文「查看考勤」；PRD 残留「考勤打卡」仅 §12B.2 标题与历史日志行 | PASS |

版本铁律：绑定设计 V0.08→V0.09（L1/L3 含日期）、集团 PRD 标题 V1.0→V1.01 + 文末一行变更记录、验收包 V0.03→V0.04 + §11 一行、PRD V0.03→V0.04 + 变更日志一行；各随所属文件 ID 归因。

### 4.2 范围归因（§5 闸 3）

- 全部 hunk 落到定稿 001–008；版本行/变更记录行随所属文件（版本铁律）；归因不到的行 0。
- 未改：三份业务 HTML（SHA 与 §3 第 10 项一致）、`plan/*`、锁、sidecar、ledger 基准、findings、源码、迁移、OpenAPI、其它文档。after tree 再次实取相同。
- 未提交/推送/合并/部署。

### 4.3 结论

内容修复：**PASS**。范围归因：**PASS**。总：**PASS**（作者自审票；须 Grok B4-r1 亦 PASS 才进 B5-r1；B3 累计 1/3）。本席在本报告落盘前未读取 `M013-b4-r1-grok-raw.md`。
