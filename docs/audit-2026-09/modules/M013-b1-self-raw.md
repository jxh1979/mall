CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-11T03:16:48Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `ec735cac3806283a6c6ea7d452f3e7c9a180fa09`；工作区 tree `34902d732180b2e56e9c970d2785c3465ac53669`。三份业务 HTML 只读。
- 模块 M013「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段」，4 文件（计划冻结 1517 行），映射 84 判据（BEQR 21、BSM 12、GEN 18、PGM 33）。
- 本模块期间的 HTML 裁决：治理地图 L741「考勤打卡」→「查看考勤」已由操作员逐句批准，在独立工作树成 PR #433（治理地图 V0.12，CI 合并中）；本报告对象仍是 main HEAD ec735cac 的治理地图 V0.11（SHA256 c4238afd…），CX-M013-006 引用该现行裁决。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M013-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 154 | 12560 | fd715410a760c94d7cfabb976b4bab29c80e4e535401dafa75954f86e84ed9b4 | 全读 |
| docs/group-governance-rebuild-prd-v1.0.md | 110 | 18123 | f933b0fa6e0d9927c6b6498cfc3a69f61d7e5c8bbd54a0f869b4c6c32ee07666 | 全读 |
| docs/platform-governance-acceptance-pack-v0.2.md | 288 | 35023 | 6b7214c1e181f2467c178d19341cc75eabf8d7a7b335cdb32403f9eea0abb64a | 全读 |
| docs/platform-governance-prd-v0.1.md | 965 | 144100 | e5bd6e18b2173b37919b358554be6c11ba13eaad56bb36321fbb774291785990 | 全读 |

## 2. 可执行证据原文（/tmp/m013_b1_run.log）

```
START=2026-09-11T03:02:49Z HEAD=ec735cac origin/main=ec735cac porcelain=1
== 1. business_entities current model (V005 + V354 + V410)
backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql:4:    entity_role VARCHAR(32) NOT NULL,
backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql:57:    project_id VARCHAR(64),
backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql:71:    ON spaces (project_id);
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:1:-- V318 组织四层落库:business_entities 增 parent_entity_id,把「管理公司/业态主体(L
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:4:--   L1/L2/L3 均落在 business_entities(entity_role=platform / project_company / management_compa
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:6:--   「一个项目公司下有多个管理公司主体」= 多行 business_entities(parent_entity
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:7:--   parent_entity_id 松引用 business_entities(id)(与 merchants.business_entity_id 一致,本�
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:8:--   business_entities 已在 V005 建表并 GRANT 给 complex_app;ALTER ADD COLUMN 继承既有授
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:10:    ADD COLUMN IF NOT EXISTS parent_entity_id VARCHAR(64);
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:12:COMMENT ON COLUMN business_entities.parent_entity_id IS
backend/src/main/resources/db/migration/V318__business_entity_parent_layer.sql:16:    ON business_entities (parent_entity_id);
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:12:--   而这些收款方在库里一行都不存在(现有 32 行全是 entity_role='operation' 的�
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:24:--   ④ entity_role 加白名单 CHECK(V005 建表至今该列无任何约束,打错字不会被�
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:32:    ADD COLUMN IF NOT EXISTS archive_reason TEXT;
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:38:--    判据:entity_role='operation' 且当前 active —— 开发库这 32 行全部是
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:46:   AND entity_role = 'operation';
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:53:INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, 
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:63:-- ④ entity_role 白名单(四层组织 + 历史 operation)
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:66:ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:67:ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:68:    CHECK (entity_role IN ('platform', 'project_company', 'management_company', 'spv', 'operation')
backend/src/main/resources/db/migration/V354__business_entity_master_data.sql:70:COMMENT ON COLUMN business_entities.entity_role IS
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:9:ALTER TABLE business_entities DROP COLUMN IF EXISTS parent_entity_id;
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:12:-- 把 entity_role 白名单收为 HTML 三角色；不删除任何行。
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:14:   SET entity_role = 'management_company',
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:16: WHERE entity_role IN ('operation', 'spv');
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:17:ALTER TABLE business_entities DROP CONSTRAINT IF EXISTS chk_be_entity_role;
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:18:ALTER TABLE business_entities ADD CONSTRAINT chk_be_entity_role
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:19:    CHECK (entity_role IN ('platform', 'project_company', 'management_company'
backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:20:COMMENT ON COLUMN business_entities.entity_role IS
business_entities ADD COLUMN in any migration:

BusinessEntityRecord fields:
String id String entityName String entityRole String unifiedSocialCreditCode String legalRepresentative String bankAccountRef String status Instant createdAt Instant updatedAt 
entity_role whitelist in PlatformService:
61:    public static final Set<String> BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
117:                    String entityRole = requireBusinessEntityRole(request == null ? null : request.entityRole());
123:                            entityRole,
699:        String value = requireText(raw, "entity_role");
== 2. binding statuses / endpoints
603:                    if (!Set.of("active", "inactive", "suspended").contains(status)) {
3
business-entities archive/delete endpoints:
== 3. V394 analytics.group.view requires_approval
3:--   V126/V127/V144 等把 11 条只读/日常作业码种成 risk_level='normal'，既不在平台管理员日常桥（low/medium），
10:--   BSM-1.3.4「敏感数据查看与导出（…企业微信会话原文、客人与员工个人信息）」）：落在五类内、仍为 requires_approval=FALSE 的高危码补 TRUE。
12:UPDATE permissions SET risk_level = 'low' WHERE risk_level = 'normal';
13:UPDATE permissions SET risk_level = 'low' WHERE permission_code = 'access.grant_request.apply';
15:ALTER TABLE permissions ADD CONSTRAINT chk_permissions_risk_level CHECK (risk_level IN ('low', 'medium', 'high', 'critical'));
16:UPDATE permissions SET requires_approval = TRUE
17: WHERE permission_code IN ('analytics.group.view', 'customer.voucher.manage', 'finance.merchant_expense.manage',
V180 line:
       AND permission_code <> 'analytics.group.view'
== 4. tax permission codes (finance.tax / tax_rule)
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:49:    static final String TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust";
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java:65:            new SupervisorReviewPolicyRecord("tax_rule_adjustment_l3", TAX_RULE_ADJUST_ACTION, 0, 3, true, 
== 5. access.grant_approval.enabled removed?
backend/src/main/java/com/complex/platform/access/AccessService.java:79:                // 原 @Value("${access.grant_approval.enabled:true}") 一票否决四�
== 6. group PRD endpoints exist (AccessController / AnalyticsController)
position-role-templates: 1
users/{id}/position: 1
role-assignments: 1
role-assignments/unassign: 1
staff-levels: 1
users/{id}/staff-level: 1
grant-requests: 1
grant-requests/{id}/review: 1
grant-requests/summary: 1
temporary-grants: 1
temporary-grants/{id}/revoke: 1
backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java:119:    @GetMapping("/group-overview")
backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java:50:        authorizationService.requirePlatformScope();
backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java:57:        authorizationService.requirePlatformScope();
== 7. V170 CHECKs
68:    CHECK (request_type IN ('user_role', 'user_position', 'temporary_permission', 'role_permission'));
72:    CHECK (status IN ('pending', 'applied', 'rejected'));
== 8. space types (PRD §6.2.1)
27418:        "x-audit-event-success": "space_type.viewed",
27419:        "x-audit-event-failure": "space_type.view_failed",
== 9. merchant/project statuses
    public static final Set<String> SUBJECT_STATUSES =
            Set.of("pending_admission", "active", "suspended", "terminated", "blacklisted");

    /** M009-004：BEQR-154 综合体项目状态六态（草稿/待启用/已启用/暂停/终止/归档）。 */
    public static final Set<String> PROJECT_STATUSES =
            Set.of("draft", "pending_activation", "active", "suspended", "terminated", "archived");

== 10. referenced docs exist
docs/platform-governance-product-spec-v0.2.md: exists
docs/operation-traceability-and-terminal-switch-prd-v0.1.md: exists
docs/evidence-risk-control-product-prd-v0.1.md: exists
docs/r5-spec-T2-T3-pre-action-gates-dev-design-20260618.md: exists
docs/scope-wide-coding-rule-canonical-V0.01.md: exists
docs/_archive/20260610-retention-audit/merged-layer-spec-sources/platform-governance-acceptance-checklist-v0.1.md: exists
docs/p0-operation-permission-matrix-v0.1.md: exists
== 11. HTML: 考勤打卡 in private-phone lists (PGM / BSM current)
741:                <li>同一个员工 
1445:                <li>考勤打卡从
741:                <li>同一个员工 App，装在公司�
                同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发，员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、考勤打卡、
                酒吧和游乐/乐园未来独立运营时，可以只打开对应商户和对应模块。
== 12. 自营 in HTML (PGM 员工注册分两条路径 / BSM)
docs/platform-governance-map-for-operations-v0.1.html:578:  
docs/business-system-map-for-operations-v0.1.html:1433:     
backend/src/main/java/com/complex/platform/governance/PlatformService.java:50:            Set.of("self_operated", "lease", "joint_operation", "managed_operation
backend/src/main/java/com/complex/platform/governance/PlatformService.java:660:                            + "(self_operated/lease/joint_operation/managed_opera
== 13. docs versions/headers
docs/business-entity-binding-development-design-v0.1.md: # 四类主体唯一绑定开发设计 V0.08 | > 版本：V0.08 ｜ 最后更新：2026-08-31 ｜ 状态：现行开发设计
docs/group-governance-rebuild-prd-v1.0.md: # 集团治理重建 PRD（V1.0·补断点） | > **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份�
docs/platform-governance-acceptance-pack-v0.2.md: # 平台治理验收包 V0.03 | > 合并说明：本文按 2026-06-10 全量文档收口与合并归档审计执
docs/platform-governance-prd-v0.1.md: # 平台治理层产品需求 PRD V0.03 | > 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终�
== 14. V349 is_sales_position / V384 media / V057 supervisor_review_decisions / V080 V162 contract scope
V057__governance_supervisor_review_approval_level.sql V080__contract_scope_and_spaces.sql V162__contract_lifecycle.sql V349__staff_sales_position.sql V384__merchant_business_format_media.sql 
== 15. is there a business_entities archive/delete in PlatformService?
196:    public MerchantRecord archiveMerchant(String merchantId, MerchantArchiveRequest request) {
200:                "archiveMerchant:" + id,
220:    public MerchantRecord unarchiveMerchant(String merchantId, MerchantArchiveRequest request) {
224:                "unarchiveMerchant:" + id,
== 16. HTML sha (only-read)
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-
c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c  docs/platform-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-
END=2026-09-11T03:02:49Z
```

## 3. 三线审计方法与覆盖

- 对象口径：main HEAD ec735cac（PR #430/#431/#432 已合并）的 4 份治理文档，共 1517 计划行（实算 154+110+288+965）；三份业务 HTML 取 main 正本（BSM V0.14 eebc5b41…、PGM V0.11 c4238afd…、BEQR V0.07 4396bb71…）；判据锁 V0.11，M013 映射 84 项。
- 第一性原理线：文档陈述的主体拓扑、四类主体绑定、自营口径、高危授权、集团下钻、准入闸口是否与三份 HTML 及 main 实现事实一致；凡文档把「当前实现」写成过期状态（V410 之前、V394 之前）即为文档缺陷（GEN-016/GEN-008）。
- 顺从式线：84 项判据逐族对照（§5）。对抗式线：假定文档被开发者当作现行合同——按其描述的字段/接口/待办去实现会产生什么错误（父链回灌、自营新字段、集团权限免审、私人手机打卡）。
- 只读依赖追流：backend governance/access/analytics/review 包、迁移 V005/V318/V354/V394/V410/V169/V170/V115、OpenAPI `SpaceRequest.space_type`、三份 HTML 现文。
- 4 文件全文逐行读完（§1）；可执行验证只读（§2）。本席禁止修复。

## 4. 与 main 实现事实的分叉对照（要害）

| 文档陈述 | main 事实（§2 探针） | 结论 |
|---|---|---|
| 绑定设计 §3.1「当前迁移 V005+V318+V354；`parent_entity_id` 为当前列；V354 白名单仍含 HTML 没有的代码值」 | V410 已 `DROP COLUMN parent_entity_id`，`chk_be_entity_role` 收为 platform/project_company/management_company；`BUSINESS_ENTITY_ROLES` 三值；`BusinessEntityRecord` 无 parent 字段 | 文档过期（CX-001） |
| 集团 PRD §4 / 验收包 §10A G7、§10 修订说明 / PRD §12 第 15 条「`analytics.group.view` requires_approval=false 待对齐（GAP-20）」 | V394 `UPDATE permissions SET requires_approval = TRUE WHERE permission_code IN ('analytics.group.view', …)` | 缺口已闭合而文档仍登记为未闭合（CX-002） |
| 集团 PRD §3「全库检索无 finance.tax / tax_rule 命中，无税费类权限码」 | `SupervisorReviewService` 有 `TAX_RULE_ADJUST_ACTION="finance.tax_rule.adjust"` 与策略 `tax_rule_adjustment_l3`；`permissions` 种子仍无税费权限码 | 登记事实过期（CX-003） |
| PRD §6.9「商户档案的「自营」标注字段属代码轮新增待办」 | 操作员 2026-09-09 已答删除独立布尔（questions/U04-001.md）；自营标注＝合作方式 `self_operated`（`PlatformService` 六类码） | 与已裁决口径冲突（CX-004） |
| PRD §12 第 8 条「当前 `SupervisorReviewService.evaluate` 为孤立评估接口，业务域动作前尚未调用」 | `StaffApprovalService` 已调用 `reviews.evaluate`（排班发布、处罚上限）；票务/酒吧/游乐/结算仍未调用 | 现状描述部分过期（CX-005） |
| PRD §5 对象表「员工私人手机 App」与 §6.9 第 8 条：私人手机六项低风险功能含「考勤打卡」 | BSM L1445 考勤打卡从领取工作手机开始算；操作员 2026-09-10 裁决「只在工作手机打卡」并批准 BSM L930/L1865、PGM L741 把「考勤打卡」改为「查看考勤」；契约 `punchStaffAttendance` 终端仅 staff_work_phone | 冲突（CX-006） |
| PRD §6.2.1 `space_type` 对照表 8 值 | OpenAPI `SpaceRequest.space_type` enum 9 值（多 `parking`） | 表不完整（CX-007） |
| 绑定设计 §5 API / §3.2 状态 active/inactive/suspended / 无 `/active`、`/usages` | `PlatformController` 三口、`Set.of("active","inactive","suspended")`、无该两口 | 一致 |
| 集团 PRD §2/§3 端点、V169/V170 CHECK、`access.grant_approval.enabled` 已移除 | 11 端点均在 `AccessController`；V170 CHECK 四类/三态；AccessService 注释确认移除 | 一致 |
| PRD §7.1/§7.4/§7.6 项目六态、商户六态、员工五态 | `PROJECT_STATUSES` 六值；`SUBJECT_STATUSES` 五值 + 存档由 M009 存档流承载；员工五态（U13-005） | 一致 |

## 5. 84 项判据逐族核对

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143/144/145/146/147/148（四类主体、主体关系、城市项目公司、运营公司职责、收入归属、合作模式） | 通过（登记） | 三份文档措辞与 ENUM 逐字同向；绑定设计 §1 三条规则即 BEQR-144；但 §3.1 仍把父链列写成现行（CX-001） |
| BEQR-149/151/152/157/159、PGM-040/041/042（职级、最终权限、敏感数据、员工状态、权限状态） | 通过 | PRD §6.10/§7.8/§12 与 PGM-041 逐句一致 |
| BEQR-150/153、PGM-010（终端十一端、开关优先级） | 通过 | PRD §2.1/§12 第 16 条十一端与优先级与 ENUM 一致 |
| BEQR-154/155/156/158（项目/商户/合同/设备状态） | 通过 | 六态/六态/八态/七态与 ENUM 一致；main 实现同向 |
| BEQR-063/166/189、PGM-007/026/027/031/035（高危动作、合同审批链、管理费、商户和主体、经销商、提成） | 通过 | PRD §2A/§6.5/§7.5 与 PGM-026 逐句同向 |
| BSM-004/005/006/007/009（存档删除、分步引导、动态二维码、平台管理员、基础管理） | 通过（登记） | PRD §15C/§7.8.1 同向；绑定设计 §4.3 如实标主体档案存档/删除未落地 |
| BSM-028/029/072/074/114/115/120 | 通过（登记） | 主体关系、注册两条路径与 HTML 同向；私人手机功能列表见 CX-006 |
| PGM-002…021（治理顺序、模块入口、综合体与集团、各主体职责、主要功能、业务意义） | 通过（登记） | PRD §2.2 治理顺序五步、§5 域主管公司职责逐字照抄 PGM-019；PGM-018 多对多与绑定设计 §1 一致；但集团下钻权限缺口登记过期（CX-002） |
| PGM-043/044/048/060/065/066 | 通过 | PRD §4.1 外包机构、§7.5.2 经销业务权限、§11 连接关系同向 |
| PGM-006/008（商户和主体、员工和手机） | 通过（登记） | 自营标注措辞见 CX-004 |
| GEN-001 必要性 | 通过 | 四份文档均为现行导航/验收入口 |
| GEN-002 可执行正确性 | 不通过 | CX-001/002/003/005：按文档描述实现会回灌父链、误判集团权限免审、误判评估接口未接线 |
| GEN-003 测试有效性 | 不适用 | 文档模块 |
| GEN-004/005/006/007/009/010/011/012/014 | 不适用（无实现面） | — |
| GEN-008 接口一致性 | 不通过 | CX-001/007 |
| GEN-013 构建 | 通过 | 引用文档/迁移/端点均存在（§2 第 10、14 项） |
| GEN-015 中文 | 通过 | 文档正文中文；代码值仅作标识 |
| GEN-016 文档 | 不通过 | CX-001…007 |
| GEN-017 分类 | 通过 | 无添加项 |
| GEN-018 证据 | 通过 | §2 命令原文 |

## 6. 本次 Findings（临时 ID CX-M013-00N；正式 ID 由 B2 分配）

### CX-M013-001 · 技术性 · 主体拓扑「当前实现」段过期（V410 未反映）

- 位置：`docs/business-entity-binding-development-design-v0.1.md:39`「当前迁移：V005 基表 + V318 `parent_entity_id` + V354」、`:46`「`parent_entity_id` | 松引用 | 当前代码中的父级关系」、`:56`「V318/V354 把 parent_entity_id 用作固定层级，且 V354 的角色白名单仍含 HTML 没有的代码值」、`:154`「当前实现：V002、V005、V010、V318、V354」。
- 事实：V410（M010）已删列并把 `entity_role` 白名单收为 HTML 三角色；后台 `BUSINESS_ENTITY_ROLES` 三值；`BusinessEntityRecord` 无父链字段。
- 判据：GEN-016、GEN-008、BEQR-144、PGM-018；依据类型：4（命令）+ 2（迁移产物）。
- 工程缺陷：是（按本文实现会回灌父链）。建议入队：是。最小处置：§3.1 改为「V005 基表 + V354 archive_reason/角色 CHECK + V410 删父链并收窄角色」，表中去掉 `parent_entity_id` 行，差距段改为「父链已删；`status` 仍无 DB CHECK；无 `project_id`」，§8 补 V410；不改业务规则。

### CX-M013-002 · 技术性 · `analytics.group.view` 审批属性缺口已由 V394 闭合，三处仍登记为待对齐

- 位置：`docs/group-governance-rebuild-prd-v1.0.md:109`（「实现待对齐…当前实现为 false…属实现缺口」）；`docs/platform-governance-acceptance-pack-v0.2.md:181`（G7）、`:193`（修订说明）；`docs/platform-governance-prd-v0.1.md:654`（「GAP-20：requires_approval=false 待对齐」）。
- 事实：V394 `UPDATE permissions SET requires_approval = TRUE WHERE permission_code IN ('analytics.group.view', …)`。
- 判据：GEN-016、GEN-002；依据类型：4。
- 工程缺陷：是（错误的缺口登记会引发重复修复或误判审计）。建议入队：是。最小处置：三处改为「已由 V394 置 requires_approval=true 闭合」，保留登记痕迹。

### CX-M013-003 · 技术性 · 税费审批「全库无命中」登记过期

- 位置：`docs/group-governance-rebuild-prd-v1.0.md:68`「当前实现无税费类 requires_approval=true 权限码（全库 *.sql/*.java 检索无 finance.tax / tax_rule 命中）」。
- 事实：`SupervisorReviewService:49/65` 有 `finance.tax_rule.adjust` 动作与 `tax_rule_adjustment_l3` 策略；`permissions` 种子仍无税费权限码。
- 判据：GEN-016；依据类型：4。
- 工程缺陷：是。建议入队：是。最小处置：改写为「审批动作已落 SupervisorReview 策略 tax_rule_adjustment_l3；`permissions` 仍无税费权限码，待对齐范围收窄为权限码」。

### CX-M013-004 · 技术性 · 「自营标注字段属代码轮新增待办」与操作员裁决冲突

- 位置：`docs/platform-governance-prd-v0.1.md:361`（§6.9 前置说明末句）、`:962`（变更日志同句）。
- 事实：questions/U04-001.md 操作员答「删除」独立布尔；U-FIX 已按合作方式 `self_operated` 分流（PlatformService 六类码；StaffRegistrationService）。
- 判据：GEN-016、GEN-017（不得把已否决的添加当待办）、PGM-026；依据类型：1 + 4。
- 工程缺陷：是。建议入队：是。最小处置：改为「自营标注即商户档案合作方式「自营」（cooperation_form=self_operated），不新增字段」；变更日志追加一行说明。

### CX-M013-005 · 技术性 · 准入闸口「评估接口孤立、尚未调用」现状过期

- 位置：`docs/platform-governance-prd-v0.1.md:639`「当前 `SupervisorReviewService.evaluate` 为孤立评估接口，业务域动作前尚未调用」。
- 事实：`StaffApprovalService` 已在排班发布与处罚上限申请前调用 `reviews.evaluate`；票务/酒吧/游乐/结算四域仍未调用。
- 判据：GEN-016；依据类型：4。
- 工程缺陷：是（低估已接线范围、高估缺口）。建议入队：是。最小处置：现状句改为「员工域（排班发布、处罚上限）已接线；票务/酒吧/游乐/结算四域动作前仍未调用」。

### CX-M013-006 · 冲突 · 私人手机功能列表含「考勤打卡」

- 位置：`docs/platform-governance-prd-v0.1.md:211`（§5「员工私人手机 App」行）、`:374`（§6.9 第 8 条）。
- HTML：BSM L1445「考勤打卡从领取工作手机开始算」；操作员 2026-09-10 裁决「以 L1445 为准：只在工作手机打卡」，并逐句批准 BSM L930/L1865 与 PGM L741 把「考勤打卡」改为「查看考勤」（PGM 改动随本模块同批落地）。
- 判据：BSM-114/BSM-120、PGM-008；依据类型：1。
- 工程缺陷：是。建议入队：是。最小处置：两处「考勤打卡」改「查看考勤」，引用条随 HTML 现文；不改其它功能项。

### CX-M013-007 · 技术性 · `space_type` 对照表漏 `parking`

- 位置：`docs/platform-governance-prd-v0.1.md:249-259`。
- 事实：OpenAPI `SpaceRequest.space_type` enum＝mall/floor/zone/shop/booth/bar_table/facility/parking/other。
- 判据：GEN-008、GEN-016；依据类型：4。
- 工程缺陷：是。建议入队：是。最小处置：表补 `parking` 行（停车位/车位点位，技术类型，不新增业务空间对象）。

## 7. 观察（登记、不入队）

- OBS-1：验收包文件名 `-v0.2.md` 而标题/变更日志为 V0.03（PRD 同类 `-v0.1.md`/V0.03）——命名惯例，不改文件名。
- OBS-2：绑定设计 §4.3「主体档案存档/删除未落地」对 `business_entities` 成立；商户档案已由 M009 落地存档/删除流（PlatformController archive/unarchive/delete-request/delete-approve），文档范围不含商户，不立项。
- OBS-3：验收包 §2A/§10A 与 PRD §2A.7 金额精度、§6.5.4 经销商治理为 M01 复审同步内容，与 PGM 现文同向。
- OBS-4：集团 PRD §1 表把 `position_role_templates` 等实现表名标为「三份 HTML 零命中、不取得效力」，措辞合规。
- 技能：`using-superpowers`、`verification-before-completion`（探针原文附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。4 文件全文三线审计、84 项判据逐族核对；7 项候选（技术性 6、冲突 1）建议入 B2 修复队列；无添加项；HTML 自身张力（PGM L741）已按人闸由操作员裁决并批准改句；未修源、未提交/推送。本报告落盘前未读取 `M013-b1-grok-raw.md` 任何正文。


## M013 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节 27544 bytes 保留，本段追加）

- 输入：`modules/M013-b1-self-raw.md` V0.01 前缀 27544 bytes SHA256 4be118bafd89ce483dc21ba233af01a515f2a248e1d2f4f0b8e7447b22b3285b；`modules/M013-b1-grok-raw.md` V0.01 前缀 41273 bytes SHA256 f6e9f5adf5c5ff8a47552ee7958cda5cff697cad53a2db5825c580e9c21db61e（Grok B1：FAIL，GR-M013-001…005，OBS-01…12）；候选 `findings.md` V1.84 730800 bytes SHA256 fd94dbd724bb8a1ab8a34314940da9516a3ba6ffb2ffecd9500b6bb9b288c193「当前 M013 B2-r1 整批候选」M013-001…010。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。

### 差集与 Grok 独有项核实（receiving-code-review）

- GR-005（席甲 B1 漏报）：`AccessController.java:204-208` DELETE 调 `AccessService.deletePositionRoleTemplate:479-481`，方法体只有 `throw ApiException.conflict("ARCHIVE_APPROVAL_REQUIRED", …)`；`:192` lifecycle 端点分支 archive/restore/request_delete/approve_delete/reject_delete（`AccessService:495-524`）。接受，入 005。
- GR-004 字典侧（席甲 B1 只报漏 parking）：`PlatformService.java:474-478` createSpace 以 `repository.listActiveSpaceTypeCodes()` 硬校验；V184 种子 building/floor/zone/shop/public_area/parking/booth/other，与 OpenAPI 九值词表不同。接受，004 处置从「补 parking」扩为「导语改字典硬校验 + 补 parking + 注明差异」。
- GR-001 契约侧：`openapi-p0.yaml:29205-29215` `BusinessEntityRequest.entity_role` enum 六旧值。事实成立；OpenAPI 不在本模块 4 文件内，拆为 009「否（XM→M392）」，001 只在文档差距段登记。
- Grok OBS-05：V394:15 `chk_permissions_risk_level` 存在，集团 PRD L26「现有表无 CHECK」同源过期，并入 002。
- Grok OBS-04（税费检索句只记观察）：本席复核 V217:43-45 `staff.tax_bracket.manage` risk_level=high、requires_approval=TRUE——L68「当前实现无税费类 requires_approval=true 权限码」与检索句均已不实，仅「税费规则权限码落点缺」仍真；一句改写即可，本席升为 006 技术性「是」，交 Grok 裁定。
- 席甲独有 007（`StaffApprovalService.java:44,61` 调 `reviews.evaluate`）、008（治理地图 V0.12 L741 / 业务地图 V0.14 L930/L1865 现文「查看考勤」）事实以类型 4/1 依据成立。
- 差集：GR-001…005 → 001/002/003/004/005（+009）；OBS-01…12 → 002（05）、006（04）、010；CX-001…007 → 001/002/003/006/007/008/004。无遗漏、无改写 Grok 原意。

### 逐 ID 票

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（细化／是） | 文档自称「当前实现」却写已删列与旧白名单；按 V410 刷新三处 + 差距段登记契约缺口为最懒；不改迁移/OpenAPI |
| 002 | PASS（细化／是） | V394 事实唯一根因，五处过期句一并改，登记痕迹保留 |
| 003 | PASS（细化／是） | 与 U04-001「删除」及 BSM L1433 一致；两句改写 + 日志一行 |
| 004 | PASS（细化／是） | 字典才是活校验；保留表作契约快照并补 parking 比删表更小 |
| 005 | PASS（细化／是） | 标注恒拒绝 + 补 lifecycle 一行 |
| 006 | PASS（技术性／是） | 可验证陈述已假，一句改写；不发明权限码 |
| 007 | PASS（技术性／是） | 现状句改一句，目标不变 |
| 008 | PASS（冲突／是） | HTML 现文逐字「查看考勤」，PRD 两处引用同条却写「考勤打卡」 |
| 009 | PASS（否，XM→M392） | OpenAPI 非本模块文件；不顺手改 |
| 010 | PASS（否） | 与两席观察一致 |

本席 B2-r1 票：**PASS**（队列是＝001–008；否＝009、010）。B3-r1 边界：只改本模块 4 文件（绑定设计 V0.08→V0.09、验收包 V0.03→V0.04、PRD V0.03→V0.04、集团 PRD 标题版本 V1.0→V1.01 并加日志），不改源码/迁移/OpenAPI/三份 HTML/锁；无测试面（文档模块），B4 以 diff 逐块归因复审。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。
