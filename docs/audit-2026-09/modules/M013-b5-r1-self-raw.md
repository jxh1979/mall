CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-11T04:08:41Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `5692a89502833a423d465b3e2cb8834c9ad91b49`；工作区 tree `d5276bc2c47e9da5aaad2fadc5a9bbcb629d1e82`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M013「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段」，4 文件（计划冻结 1517 行；当前含 B3-r1 修复），映射 84 判据（BEQR 21、BSM 12、GEN 18、PGM 33）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审）。

- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M013-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：PR #430/#431/#432/#433 已合并（HEAD=origin/main=5692a895，治理地图 V0.12）；本模块 B3-r1 修复在工作区未提交（after tree dcfc3a8cc70a23e43ef3b47c668411f2c05af1b4）。
- 只读依赖追流（不审）：三份 HTML 现文、V005/V318/V354/V410/V394/V217/V184 迁移、`PlatformService`/`BusinessEntityRecord`/`JdbcPlatformRepository`、`AccessController`/`AccessService`、`SupervisorReviewService`、`access/staff/StaffApprovalService`、`docs/openapi/openapi-p0.yaml`（未改）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 153 | 13095 | f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f | 全读 |
| docs/group-governance-rebuild-prd-v1.0.md | 113 | 19023 | e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b | 全读 |
| docs/platform-governance-acceptance-pack-v0.2.md | 289 | 35245 | ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56 | 全读 |
| docs/platform-governance-prd-v0.1.md | 969 | 145630 | aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413 | 全读 |

## 2. 可执行证据原文（/tmp/m013_b5r1_run.log）

```
START=2026-09-11T04:08:41Z HEAD=5692a895 origin/main=5692a895
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
?? docs/audit-2026-09/modules/M013-b4-r1-grok-raw.md
?? docs/audit-2026-09/modules/M013-b4-r1-self-raw.md
?? docs/audit-2026-09/modules/M013-b5-r1-grok-raw.md
END=2026-09-11T04:08:41Z
```

## 3. 三线审计方法与覆盖

- 第一性原理线：从三份 HTML 原文（BEQR-143/144/148/155、PGM-018/019/026/041、BSM-004/114/120、BSM L1445）反推四份文档应当陈述什么「现行合同」，逐一对照修复后文档与 main 实现事实（不依赖 B1–B4 结论）。
- 顺从式线：84 冻结判据逐族（§5；无实现面的判据明确原因）。
- 对抗式线：假定开发者把修复后文档当现行合同——按 §3.1 新表建模是否还会回灌父链、按 §6.2.1 新导语编码是否会漏 parking 或把 OpenAPI 当活闭集、按端点表集成是否还会撞 409、按 §6.9 是否还会新建自营布尔、按 §12#8 是否会误判评估接口未接线、私人手机列表是否仍与 HTML 冲突；B2 定稿禁止项（改 OpenAPI/V184/迁移/源码、新增字段或接口、删对照表、改 HTML）是否被越界引入。
- 依赖只读追流（不审）：V410/V394/V217/V184/V318/V354 迁移、`PlatformService`（BUSINESS_ENTITY_ROLES / MERCHANT_COOPERATION_FORMS / createSpace）、`AccessController`/`AccessService`（DELETE 与 lifecycle）、`SupervisorReviewService`、`access/staff/StaffApprovalService`、`openapi-p0.yaml`（未改）、三份 HTML 现文（§2）。
- 4 文件全文逐行读完（修复后行数见 §1）；B3-r1 diff 逐字对照（B4-r1 双 PASS）。

## 4. 已定稿 10 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | 绑定设计 §3.1 当前迁移含 V410、字段表无父链行、`entity_role` 三值、差距段登记 OpenAPI 旧六值为契约缺口、§8 补 V410 | 文档 `parent_entity_id` 仅出现在「已删除/历史」语境；与 V410/`BUSINESS_ENTITY_ROLES` 一致（§2） |
| 002 | 否（已修） | 集团 PRD L26/L109、验收包 G7/修订说明、PRD §12#15 均写 V394 已闭合 | V394:15-18 原文一致；无残留「requires_approval=false」缺口句（§2） |
| 003 | 否（已修） | PRD §6.9 自营标注＝合作方式 `self_operated`；日志行有订正 | 生产代码无独立布尔；U04-001 答删除（§2） |
| 004 | 否（已修） | §6.2.1 导语字典硬校验口径；表含 parking | `PlatformService:474-478` 与 V184 一致；OpenAPI 未改 |
| 005 | 否（已修） | 端点表 DELETE 标恒拒绝 + lifecycle 行 | `AccessService:479-481`/`:495-524` 一致 |
| 006 | 否（已修） | 税费登记句为现状（V217 已有、finance.tax_* 仍缺） | V217:43-45；迁移 `'finance.tax` 0 命中 |
| 007 | 否（已修） | §12#8 现状：员工域已接线、四域未接 | `StaffApprovalService:44,61`；其它域 0 处 |
| 008 | 否（已修） | §5/§6.9 两处「查看考勤」 | 与治理地图 V0.12 L741、业务地图 V0.14 L930/L1865 逐字同向；残留「考勤打卡」仅 §12B.2 标题与历史日志 |
| 009 | 否（XM→M392） | OpenAPI `entity_role` 旧六值未动（非本模块） | 与定稿一致；已在 001 差距段登记 |
| 010 | 否（登记） | 观察项未动 | 与定稿一致 |

## 5. 84 项判据逐族核对（修复后）

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143/144/145/146/147/148 | 通过 | 四类主体/多对多/城市项目公司/运营公司/收入归属/合作模式措辞与 HTML 同向；§3.1 不再把父链写成现行；自营标注＝合作方式 |
| BEQR-149/151/152/157/159、PGM-040/041/042 | 通过 | 职级/最终权限/敏感数据/员工与权限状态与 PGM-041 逐句一致；`analytics.group.view` 审批属性登记与 V394 一致 |
| BEQR-150/153、PGM-010 | 通过 | 十一端与开关优先级一致 |
| BEQR-154/155/156/158 | 通过 | 状态闭集与 ENUM 一致 |
| BEQR-063/166/189、PGM-007/026/027/031/035 | 通过 | 高危动作、合同审批链、管理费、商户与主体、经销、提成同向 |
| BEQR-074 | 无实现面 | 4 文件不写隐私桌台 |
| BSM-004/005/006/007/009 | 通过 | 存档/审批软删：端点表已改为 lifecycle 真实路径；其余同向 |
| BSM-028/029/072/074/114/115/120 | 通过 | 私人手机功能列表与 HTML 现文同向；主体关系/注册路径一致 |
| PGM-002…021、043/044/048/060/065/066 | 通过 | 治理顺序、模块入口、多对多、主体职责、空间点位（字典口径）、集团下钻均同向 |
| GEN-001 必要性 | 通过 | 4 文件均有 HTML 上位 |
| GEN-002 可执行正确性 | 通过 | 「当前实现」陈述均可按 §2 命令复核为真；按文档实现不再回灌父链/误判审批/撞 409 |
| GEN-003/004/005/006/007/009/010/011/012/014 | 不适用或无实现面 | 文档模块（原因同 B1） |
| GEN-008 接口一致性 | 通过（本模块范围） | 文档与端点/枚举/字典一致；OpenAPI 旧六值为 009 跨模块登记 |
| GEN-013 构建 | 通过 | 引用文件/迁移/端点均存在 |
| GEN-015 中文 | 通过 | 正文中文，代码值仅作标识 |
| GEN-016 文档 | 通过 | 001–008 修复后无过期「当前」陈述残留（§2 grep） |
| GEN-017 分类 | 通过 | 无添加项；未把业务发明写成技术修复 |
| GEN-018 证据 | 通过 | §2 命令原文 |

## 6. 本次 Findings

无新增 finding。（B3-r1 修复后 4 文件三线全量重审，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队；与 010 同）

- 文件名版本与正文版本不一致为仓库惯例（绑定设计 v0.1/V0.09、验收包 v0.2/V0.04、PRD v0.1/V0.04、集团 PRD v1.0/V1.01）。
- OpenAPI `BusinessEntityRequest.entity_role` 旧六值（009）待 M392 处理；本模块文档已登记。
- 技能：`verification-before-completion`（§2 命令实跑）、`receiving-code-review`（未读席乙 B5 报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。4 文件全文三线重审、84 判据逐族核对、10 项定稿逐项核对；无新增缺陷、无添加项、无 HTML 问题；未修源、未提交/推送。本报告落盘前未读取 `M013-b5-r1-grok-raw.md` 任何正文。
