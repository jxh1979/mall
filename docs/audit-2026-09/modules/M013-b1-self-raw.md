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


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M013 B1 Codex 独立全量审计 V0.03
2026-09-15T00:05:16.566937+00:00
结论：FAIL。6项本模块文档缺陷，1冲突/5细化；无添加、无HTML自身新增问题、无待答questions。正式ID等B2整批分配；未修任何业务文件。历史B3=1，本次0。M013/471，ledger IN_PROGRESS。
独立性：先派Grok（grok-4.6/xhigh，直接SSH只读席）；本方完成四文件全文、84定义/相关HTML及直接依赖后封存本报告，尚未读取本次Grok raw或CLI JSON。历史M013记录仅用于防重复，不替代本次全审。输出只追加指定self raw，保留原31826bytes。
## 范围与指纹
| 文件 | 版本/行数 | SHA256 |
|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | V0.09 / 1–153 全文已读 | f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f |
| docs/group-governance-rebuild-prd-v1.0.md | V1.01 / 1–113 全文已读 | e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b |
| docs/platform-governance-acceptance-pack-v0.2.md | V0.04 / 1–289 全文已读 | ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56 |
| docs/platform-governance-prd-v0.1.md | V0.04 / 1–969 全文已读 | aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413 |
计划冻结1517行与当前1524行分别保留，不伪改冻结范围；主PRD本次分段1–140/141–285/286–395/396–520/521–620/621–720/721–835/836–969全覆盖。其余3份亦逐行全读。
## 第一性原理（codebase-design + grilling）
用途：这四份文档是产品需求/验收承接和实现映射，应帮助开发者理解对象/权限/主体责任，不能创造HTML外新业务。
实际：绑定设计和集团PRD混含当前代码状态；验收包及主PRD以产品目标为主，部分段明确宣称实现已落地。须逐个追源码，不能用文件互引或旧PASS自证。
未经验证前提：创建永远active、平台scope即集团scope、临时申请必须含高危、平台管理员wildcard、所有模板权限整单先拒、URL声明的阈值参数可用——均由实际分支反证（下列N01–N06）。
最小性：只修当前文档失真处。财税完整审批/usages/下游闭环的既有缺口不在文档轮伪造实现；跨模块后台问题保留观察。
## 完整发现
### CX-M013-N01 — 主体绑定创建与状态变更现状说明滞后
位置：docs/business-entity-binding-development-design-v0.1.md:80,90,117,118
原文与上下文：当前创建接口直接写 active，已有 active 时拒绝；状态变更接口只改启用、停用或暂停。§4：当前差距：POST /api/v1/platform/business-entity-bindings 直接创建 active 记录，没有草稿、审批和复核状态。端点表：直接创建 active 绑定；已有 active 时返回 ACTIVE_BINDING_EXISTS；将状态改为 active、inactive 或 suspended，并填写原因。
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：PlatformService:756–866：创建先锁商户，requireUnfinishedBindingSlot、requireUnchangedFinancialSubjects；未来 effective_from 写 inactive 且不投影商户三主体，已到日期写 active 才同步。945–960：已有非空三主体不得经此接口替换；ACTIVE_BINDING_EXISTS 包括 active 或 inactive+effective_to为空。change 828–837：BINDING_PERIOD_ENDED / BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW / BINDING_NOT_EFFECTIVE。JdbcPlatformRepository:585–601 对应SQL；PlatformServiceTests:860–942现有反例（本次只读，未冒称重跑）。
分类：细化；工程缺陷：是；建议本模块队列：是。冻结ID：BEQR-143、PGM-026、GEN-002、GEN-008、GEN-016、GEN-018
顺序判定：排除与HTML明文相反的新规则；属于已有主体/权限条目的实现说明，先归细化；不旁路改判技术性/添加。
最小拟处置：仅同步上述四处当前实现说明：未来日期先记 inactive、不会立即改商户主体；已有生效或未结束待生效绑定拒绝重复；状态变更受未生效、已结束、财税主体变更守卫限制。保留尚无完整审批/草稿/历史版本/usages闭环的差距，不把到日可调用启用写成自动启用。
正常触发与影响：调用方不会再按无条件启用或任意状态切换接入；不更改日期规则、不新增后端能力。
### CX-M013-N02 — 集团总览错误宣称项目级调用已按本项目过滤
位置：docs/group-governance-rebuild-prd-v1.0.md:14,95,110（连带验收包G7与主PRD承接落地表述需明确闭合对象）
原文与上下文：平台/集团级角色（requirePlatformScope() 放行者）可跨项目公司汇总下钻；项目公司管理员只看本项目公司；…集团下钻只对平台/集团级角色开放，对项目公司/商户仍按其 scope 收敛。§4：项目公司管理员即便持有该权限，也因 scope 收敛到本项目、无法跨项目公司下钻。§4末：requirePlatformScope() 只收敛可见范围、不替代授权审批。
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：AnalyticsController:119–125仅 requirePermission、requirePlatformScope，再 groupOverview(groupBy)。AuthorizationService:482–498只查平台管理员桥、外包、merchantId，不检查project/organization；AnalyticsService:774–830读取全部project快照/平台回退，未接调用者project。无商户归属的项目员工获analytics.group.view后，当前链没有本项目过滤。AnalyticsServiceTests:286–300明确无actor输入时返回project_1及project_2。
分类：细化；工程缺陷：是；建议本模块队列：是。冻结ID：BEQR-152、PGM-041、GEN-005、GEN-008、GEN-016、GEN-018
顺序判定：排除与HTML明文相反的新规则；属于已有主体/权限条目的实现说明，先归细化；不旁路改判技术性/添加。
最小拟处置：保留HTML要求的项目隔离；将现状改为商户/外包守卫存在，但此端点尚未按项目级调用者收敛，不能把requirePlatformScope放行者等同已认定的集团角色。V394只闭合analytics.group.view授出前审批属性，不等于项目隔离已闭环。必要时在验收包G7及主PRD已落地引用旁限定为基本聚合/审批属性，登记项目范围缺口；本模块只修四文件内说明。
正常触发与影响：避免按已隔离状态验收；后台缺口仅cross_module_observations→M348（AnalyticsController/Service），AuthorizationService证据归M032，不扩本轮修复。
### CX-M013-N03 — 日常权限的临时申请被文档误写为拒绝
位置：docs/group-governance-rebuild-prd-v1.0.md:71
原文与上下文：申请时服务先解析目标授权会授出的权限码，只允许包含 requires_approval=true 的授权申请；若没有审批必需权限，返回 GRANT_APPROVAL_NOT_REQUIRED，应走直接授权。
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：AccessService:879–901明确dailyTemporary=approvalRequired.isEmpty() && temporary_permission；仅非日常临时且无审批必需码才拒绝，日常临时使用permission_grant_l1。AccessServiceTests:1430–1453已有dailyTemporaryPermissionApplicationEntersSupervisorChain，断言pending与一级链。PGM-041：员工可申请临时权限，按层级对应主管审批。
分类：细化；工程缺陷：是；建议本模块队列：是。冻结ID：PGM-041、GEN-002、GEN-008、GEN-016
顺序判定：排除与HTML明文相反的新规则；属于已有主体/权限条目的实现说明，先归细化；不旁路改判技术性/添加。
最小拟处置：将拒绝条件限定为角色/岗位授权申请；补清临时权限不论日常或高危均进审批链，日常临时按现有一级主管策略。保持过期/单次有效约束及不绕过审批。
正常触发与影响：接入方不会把合法日常临时申请改走直接授予；只修当前说明，不改变后台权限策略。
### CX-M013-N04 — 岗位模板仍描述全量持权检查与平台管理员wildcard例外
位置：docs/group-governance-rebuild-prd-v1.0.md:34,38
原文与上下文：创建/更新职位模板必须是平台管理员，并且复用 requireActorCanGrant：模板里角色权限和权限模板条目都不能超出操作者自身有效权限，平台管理员 wildcard 例外。…角色装配只要包含五类强制高危权限或其他 requires_approval=true 权限，就必须被审批闸拦截并走 §3 申请。
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：BSM-007平台管理员仅自动拥有日常功能；高危等逐项授予。AuthorizationService:178–206实际返回wildcard=false，daily集合扣denies；AccessService:2060–2076使用requireActorCanGrantImmediate，808–812只对立即生效普通码检查持权，待批高危不要求配置人先持有。assignUserPositionDirect:1464–1527混合模板保存普通部分，高危交holdHighRiskPositionPermissions，而非整单拒绝。
分类：冲突；工程缺陷：是；建议本模块队列：是。冻结ID：BSM-007、PGM-041、GEN-005、GEN-008、GEN-016
顺序判定：平台管理员wildcard例外先与BSM-007明文冲突；应删除该例外，相关现状收窄为真实执行分支。
最小拟处置：删除平台管理员wildcard例外；如实说明岗位映射校验立即生效的普通权限，须审批高危保持待批。同范围定岗普通部分生效、高危挂起；直接分配角色仍按自己的授予校验和审批，不将岗位特例推广到所有授权入口。保留配置模板的平台管理员条件与所有已有审批要求。
正常触发与影响：防止把日常管理员解释成全权限，或因一个待批高危码把整份正常定岗拒绝。改现状说明，不授予任何新权限。
### CX-M013-N05 — 授权申请权限风险档仍引用V170旧值
位置：docs/group-governance-rebuild-prd-v1.0.md:90
原文与上下文：权限：access.grant_request.view（medium）、access.grant_request.apply（high）、access.grant_request.review（critical、requires approval）、既有 governance.supervisor_review.approve。
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：本文:7称迁移/代码/接口为当前实现证据。V170:100确为历史high；V394:13明确UPDATE apply风险档为low，:14授予staff_self；当前所有迁移中此码无后续反改。原初值可保留作历史，不能未标注地当现行值。
分类：细化；工程缺陷：是；建议本模块队列：是。冻结ID：BSM-007、GEN-008、GEN-016
顺序判定：排除与HTML明文相反的新规则；属于已有主体/权限条目的实现说明，先归细化；不旁路改判技术性/添加。
最小拟处置：仅将apply写为low（V170原high，V394已调整），其它权限属性保持。
正常触发与影响：避免集成者按旧高危档判断谁能发起申请；申请归类为日常不等于授权授出免审。
### CX-M013-N06 — 智能总结端点示例携带已被拒绝的查询策略参数
位置：docs/group-governance-rebuild-prd-v1.0.md:77,83
原文与上下文：智能总结：grantSummary 统计窗口内临时权限申请…关键端点 GET /api/v1/access/grant-requests/summary?window_days=...&position_threshold=...&user_threshold=...
证据（类型2：当前可执行源码；类型4：本次直接SSH读取/检索输出）：AccessController:158–165仍接收这些参数但透传；AccessService:1044–1055任一threshold非null或window非后台值即QUERY_POLICY_OVERRIDE_FORBIDDEN；实际阈值/窗口从grantPolicy读取。正常按文档示例填写数字即被拒绝，不是极端情形。
分类：细化；工程缺陷：是；建议本模块队列：是。冻结ID：PGM-041、GEN-002、GEN-008、GEN-016
顺序判定：排除与HTML明文相反的新规则；属于已有主体/权限条目的实现说明，先归细化；不旁路改判技术性/添加。
最小拟处置：示例收窄为不带阈值的GET /api/v1/access/grant-requests/summary；现状说明窗口和频繁阈值取后台政策，查询不得覆盖；保留兼容窗口参数仅等于政策值可接受的事实，勿改Controller。
正常触发与影响：可直接按文档调用；不放宽参数覆盖，不新增配置。
## 顺从式 84/84 完整适用矩阵
各条定义已读全文，PGM-066按下一一至三级标题截断，未把模块映射当作定义。以下为本次逐项适用结果；“未见新增缺陷”不是声明所有业务已实现。对应文档内已诚实标注的待实现目标不是本模块实施任务。
| 冻结ID | 本次核对及证据 |
|---|---|
| BEQR-063 | PRD §2A.4、§12-14A；授权流程文档缺陷 N03/N04，高危不得因模板豁免。 |
| BEQR-074 | PRD §12-20、§15A、验收包权限/隐私边界；隐私桌台仅受权功能，不将集团权限等同敏感价权限。 |
| BEQR-143 | 绑定设计§1–7、PRD459–475/661，四主体唯一有效关联；当前创建行为N01。 |
| BEQR-144 | 绑定设计§1/3.1，group5–15，PRD191–223；无固定四层父链，V410删父列实证。 |
| BEQR-145 | PRD191–198，绑定设计职责段；项目公司资产/物业责任与域主管公司分开。 |
| BEQR-146 | PRD198/223、绑定设计职责；域运营职责不反推固定项目隶属。 |
| BEQR-147 | PRD223、318–328/769–778，收入/股权/牌照/结算分别承接；未见把酒吧单票收入给平台。 |
| BEQR-148 | PRD295/364/671、绑定设计；六合作模式，自营复用self_operated，旧003未复发。 |
| BEQR-149 | PRD406/640、group角色装配；M1–M10上下中为默认模板/薪资范围，不替代功能授权。 |
| BEQR-150 | PRD38/658、验收包开关条；十一端齐，客人无Web，技术代码不新造终端。 |
| BEQR-151 | PRD402/523/653、group15/76；加角色/基础/有效临时/单独授权减显拒，N04纠正wildcard。 |
| BEQR-152 | PRD218/396/525/662；group14/95隔离现状N02，不将应有规则当已落实。 |
| BEQR-153 | PRD51/658、验收包82/119；高层关优先、缺配置默认关、钱规则不得灰度。 |
| BEQR-154 | PRD232/430–431/604；六状态记录不额外创造项目绑定门槛。 |
| BEQR-155 | PRD281/460–465/607，六态与退出缓冲；绑定设计已标未完整实现，不假装后台全可用。 |
| BEQR-156 | PRD287–316/474/608，八态及新建/变更/终止审批。 |
| BEQR-157 | PRD500/610/673，员工五态；注册两路径与临时/外包有效期分清。 |
| BEQR-158 | PRD511/612、设备流程351–358；设备七态不混私人手机技术状态。 |
| BEQR-159 | PRD522/614，权限五态；group1实现表态明确不取代业务态。 |
| BEQR-166 | PRD297–298/660，主线与业务财务法务平行线逐级补全、驳回退起草。 |
| BEQR-189 | PRD138/295/475，绑定设计四主体费用归属；未新增管理费算法或收款方。 |
| BSM-004 | PRD15C、group44–45，存档/申请删除/最高权限/占用检查；AccessService481–517入口吻合，旧005未复发。 |
| BSM-005 | 验收包业务验收步骤与PRD6章分步录入；产品设计非页面已上线声明，无新增向导能力。 |
| BSM-006 | 验收包全局二维码边界、PRD设备核销上下文；动态码不被新定义静态身份，不为M013增二维码实现。 |
| BSM-007 | PRD527–537及group34；N04管理员wildcard错误，N05申请权限旧风险档。 |
| BSM-009 | PRD2.1、5–7章及验收包对象范围；基础对象关系与权限边界均承接。 |
| BSM-028 | 绑定设计全文、PRD5/7.1/7.4；多对多/四主体/项目归属；N01当前接口说明。 |
| BSM-029 | PRD11章/验收包系统连接；合同结算权限共用归属，而非各系统重造主体。 |
| BSM-072 | PRD275–339/453–491、绑定设计；商户仅授权范围内完整系统，不给跨商户开关。 |
| BSM-074 | PRD11/§15C、绑定设计§5–7；商户同合同/空间/财务关联，当前usages缺口诚实。 |
| BSM-114 | PRD211/377已按批准HTML为查看考勤；与冻结旧摘录差别属既知观察O01，不反改已批文字。 |
| BSM-115 | PRD11/12-19，验收包连接清单；全链可追责任/归属，未以子系统替代平台。 |
| BSM-120 | PRD12及验收包7–11章；后续事项有边界，冻结页脚时间差仅O01；不把未来完整系统判完成。 |
| GEN-001 | 4文件承接既有业务/实施边界，最小修正文档，不新建能力。 |
| GEN-002 | N01/N03/N06有按文档正常操作即相反结果；证据为实际源码读数，不称运行HTTP。 |
| GEN-003 | 文档审计按§6豁免新测试；读现有反例未把历史测试成绩当本次运行。 |
| GEN-004 | 项目/商户/参数信任边界声明检查，N02/N06；未改校验。 |
| GEN-005 | N02/N04实际授权与scope声明不符，其余显拒及最高权限边界保留。 |
| GEN-006 | 绑定唯一有效/历史主体不覆盖，当前守卫N01；不新增数据库约束。 |
| GEN-007 | PRD§2A.3幂等与先成功原则；本模块无可执行并发代码，不造压力测试。 |
| GEN-008 | N01/N03/N04/N05/N06实现字段/入口参数与文字对应核验；旧OpenAPI009归M392。 |
| GEN-009 | 文档失败码应表述真实拒绝条件N01/N03/N06，不能用404/403等替代应有描述。 |
| GEN-010 | 权限/个人信息/留痕边界有HTML；文档未含凭据，不重写已批准业务。 |
| GEN-011 | PRD6.11/7.9/15A/15C有留痕与可查来源；源码行为引用可定位。 |
| GEN-012 | 本模块无新增执行资源/查询；不设额外压测/容量门槛。 |
| GEN-013 | 现有接口/迁移仅证据；文档修复不引依赖，不强制整仓发布检查。 |
| GEN-014 | V170/V184/V394/V410只读，以迁移叠层辨认当前值N05；不改已应用迁移。 |
| GEN-015 | 全文表/流程可读，PRD无可执行UI新增；业务全中文，代码/内部标识不误当产品英文。 |
| GEN-016 | 全1524行完整读；当前实现声明N01–N06；历史日志和已废止字样不重判当新政策。 |
| GEN-017 | 依序冲突→细化→技术性→添加；6项无添加，无HTML改动申请。 |
| GEN-018 | 每项至少类型2可执行源码或类型4实际只读输出；必要性/实际行为/前提三问见正文。 |
| PGM-002 | PRD57–65固定业务五步，与接口拦截顺序分层。 |
| PGM-003 | PRD2.1/9及验收包模块范围；入口草案不自称运行页面。 |
| PGM-004 | PRD7.1/12-15、group4；N02分清集团聚合已具备与项目范围未收敛。 |
| PGM-005 | PRD6.2/7.2、验收包空间；字典活码与OpenAPI快照分别说明，旧004未复发。 |
| PGM-006 | 绑定设计全文、PRD7.4；四主体职责与商户单一业态分开。 |
| PGM-007 | PRD6.5/7.5及验收包合同；三类审批/当地日期/AI只建议。 |
| PGM-008 | PRD6.9/7.6/7.7，员工手机可信及私人/公司设备边界；当前查看考勤尊重已批原文。 |
| PGM-009 | PRD6.10/7.8，group1–3；N03/N04/N05/N06为现状说明。 |
| PGM-010 | PRD38/658、验收包；十一端、高层关闭优先，参数粒度不等同灰度。 |
| PGM-011 | PRD6.8/7.7；设备状态与人员/点位责任链，未引入任意设备状态表。 |
| PGM-012 | PRD商户流程/权限、验收包角色验收；本商户功能全开放仍受合法范围。 |
| PGM-013 | PRD2.2/11、绑定设计5.3；外部接入引用同绑定、usages未实现诚实。 |
| PGM-014 | PRD6.11/7.9/15C；日志永久、作废冲正非删除，事件七态。 |
| PGM-015 | PRD12A及空间合同验收；楼宇运营职责明确，不扩写现场新流程。 |
| PGM-016 | PRD12A/15A/15B/15C；应急、隐私与最高权限三红线不可越。 |
| PGM-017 | PRD229–234/429–431/635及验收包项目；资料目标含负责人/地址/经营范围，M009已补后端不冒称UI齐。 |
| PGM-018 | 绑定设计1/3、PRD191–223，V410直接证据：多对多不父链。 |
| PGM-019 | PRD191–223/769–778、绑定设计职责，城市项目/娱乐平台/游乐传媒/餐饮分工。 |
| PGM-020 | PRD项目商户对象与group下钻；N02项目隔离现状不能仅靠组权限。 |
| PGM-021 | PRD13/11及验收包业务意义：责任与归属层不替代各域业务。 |
| PGM-026 | 绑定设计3–7、PRD7.4/12-19；N01接口生效时间与四主体投影，完整财税审核尚无。 |
| PGM-027 | 绑定设计变更/引用、PRD6.5；经营对象与公司/合同主体独立，历史资金归属不自动改。 |
| PGM-031 | PRD318–328/484–491、验收包外经销；主体审核/合同目录/到账出库/职责分离/销售收入。 |
| PGM-035 | PRD378–386/12B；四条钱独立，代录仅留痕、销售归属与业绩同行。 |
| PGM-040 | PRD649–653、group角色模型；单一功能粒度，不以职级替代权限。 |
| PGM-041 | PRD6.10/7.8/12-14A；N02/N03/N04/N05/N06与当前授权文档漂移，业务规则保留。 |
| PGM-042 | PRD406/640/650与group角色装配；职级三十档、具体薪资独填，不折算实际组织审批链。 |
| PGM-043 | PRD166–185、验收包角色，外包仅指派范围/审计只读；供应商门户明确待落地。 |
| PGM-044 | PRD378–386/661–663及验收包业务权限；桌台/挂单/经销不因通用权限绕过审批。 |
| PGM-048 | PRD6.4/7.4/15A.1，商户范围与本商户客户例外，跨商户无例外。 |
| PGM-060 | PRD129–138/15A及验收包会员边界；积分成长分别取已批规则、用途边界不自造同意必填。 |
| PGM-065 | PRD11/12-19与绑定设计5；六种下游共用归属，不把usages写成已实现。 |
| PGM-066 | PRD12/12A/12B/15A–15E、验收包已决/废止记录；本轮不补HTML外业务分类。 |
## 对抗式反例与排除
1. 明年才生效是否现在换商户主体？代码写inactive且不同步，文档无条件active错误N01。当天或过期是否任意重开？已结束/未来/财税变更各有守卫，不能写任意三态切换。
2. 无merchantId而organizationId为项目的员工，获group权限是否会被裁成单项目？守卫未读取组织/项目，Service无调用者scope，N02成立。只有一个项目时看不出来不能证明已隔离。
3. 申请普通码temporary_permission是否走直接授予？dailyTemporary专门送主管链，N03。模板中有待批高危码是否要求配置者预先持有/整份拒绝？Immediate过滤及高危挂起证明N04。
4. 按文档请求summary填position_threshold/user_threshold是否成功？任一非null即拒，N06。申请权限是否仍high？V394覆写low，N05；只把V170当当前来源会漏掉后续迁移。
5. “源码/契约存在=业务已完成”？否。主PRD211/377查看考勤有原批准，不反改；OpenAPI角色六值仍旧009；完整绑定审批/usages缺口继续诚实登记；不补外部机构门户或对标公式。
## 观察与历史边界
O01：66条HTML来源冻结摘录与当前文本64条完全一致；BSM-114仍含旧“考勤打卡”，实际BSM1865已按用户2026-09-10“以L1445为准：只在工作手机打卡”及两处“同意按此修改”改为查看考勤；PGM741亦获批，ledger原记录含PR#433。BSM-120仅页脚2026-09-08→2026-09-10。M009此前已登记同两差别。本轮没有修改lock/sidecar/HTML，没有新判据，按同一既有条目及已批业务原文核文档；不重问，不把旧摘录恢复为业务规则。
O02：历史001–008修复仍在；009 OpenAPI角色六值→M392保持跨模块，010历史观察保留。010旧时创建active与当时现状一致不等于当前仍正确；N01是当前新证据，B2须明列继承关系避免冒称旧修复回归。
O03：PRD606“控制业态是否可被商户和合同引用”表述偏宽，但同格明确本PRD不定义状态，269–270/447/449及968已明示废止绑定开关，未提供新状态、规则或可执行入口。本次作非阻断文字观察，不创造未授权业态开关、不扩大修复。
O04：历史日志、已废止段、文件名旧版本、技术码与产品UI区别明确；PRD38字段字典引用最终受658/三HTML约束；不重报旧010。
O05：N02关联后台实现归M348，AuthorizationService依赖归M032，均未全审/未分配未来模块ID/未修。M013只消除文档虚假闭合，业务隔离本身仍需相应模块实现。
O06：group23的DB四类与公开applyGrant三类是V170/公开入口描述；当前内部scope_change与position_high_risk_release不是对外可填写申请类型，不据此擅自放宽枚举。现有对标算法/缺主体回退已明确不取业务效力，不在本轮批准新算法。
## 实际命令与验证边界
直接SSH运行Python读取指定UTF-8文档/定义，并以subprocess.run rg查直接依赖。核心实跑：rg -n requireUnfinishedBindingSlot|requireUnchangedFinancialSubjects PlatformService.java；读取PlatformService756–866/945–967与JdbcPlatformRepository585–601；读AnalyticsController119–125、AuthorizationService482–498、AnalyticsService774–830；读AccessService879–901/1044–1055/1464–1527/2060–2076、AuthorizationService178–206及V394全文。
修正一次证据命令错误：最初StaffApprovalService路径少access/、V394*作为字面路径均rg exit2；找到实际路径后读取成功。最初只读probe正则转义错误exit1，改为普通子串判断后exit0；均未改业务文件/未拿失败输出当通过。
本次静态文档声明probe输出：creation_doc_direct_active=true；code_future_inactive=true；group_doc_project_only=true；guard_reads_project=false；endpoint_passes_only_groupBy=true；doc_only_requires_approval=true；code_accepts_daily_temporary=true。
未运行HTTP/浏览器/数据库修改/Java测试；上述测试只作当前源码反例，文档型审计按runbook§6不强制造测试。结论限于M013四份文档。未发布/提交/推送/部署。
技能：本轮已实际读取using-superpowers、grilling、codebase-design、systematic-debugging、diagnosing-bugs及receiving-code-review/verification-before-completion。按V0.17收窄到自问、自读、静态复现；禁止第三席、修改instrumentation与额外交付物。先前技能缺口不冒称补齐。
## 封存指纹（SHA256）
- docs/business-entity-binding-development-design-v0.1.md: f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f
- docs/group-governance-rebuild-prd-v1.0.md: e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b
- docs/platform-governance-acceptance-pack-v0.2.md: ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56
- docs/platform-governance-prd-v0.1.md: aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413
- docs/business-enum-quick-reference-v0.1.html: 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
- docs/business-system-map-for-operations-v0.1.html: eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66
- docs/platform-governance-map-for-operations-v0.1.html: a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1
- backend/src/main/java/com/complex/platform/governance/PlatformService.java: 984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1
- backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java: 2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a
- backend/src/main/java/com/complex/platform/access/AccessService.java: f38bff0b90f42703756a4069fafe560a16b834e8c30b34b9bb1b6a57d3e580bb
- backend/src/main/java/com/complex/platform/access/AuthorizationService.java: 990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062
- backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java: 4f99e01947eae1fc8aef3a538fc7c3f5562eea8542f591d986ad4578d967a47e
- backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java: 3abccd0cf6275f1ab3e691e0bcf7b4924b013536e07718a56c57f7d771b83f38
- backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java: 1bbeee272f0615fa5bc143d00650ca6ae17e0c412730214e9ae24fe036a300d4
- backend/src/main/resources/db/migration/V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql: acdeef24cb2e653981919b47eb0421e96a713f9fd7ce7cb14a450538ad7ceba7
- backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql: 85c914d7d711b8d8b3eb1bbfad5799a13a9c1a40a9b4145bf200bf88cff12ef1
HEAD=e456ba96c84185c8d39f18f548011daac48a6466；branch=main；real-index SHA256=1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2。
B1自席最终结论：FAIL；全量完成，等待Grok完整交付后整批B2。


# M013 B2-r2 Codex 整批归并复核 V0.04
2026-09-15T00:09:19.140309+00:00
B2自席结论：PASS。此票只通过候选归并，不是模块PASS；尚待Grok B2票。候选findings V2.24 SHA256=608151f69052d71804ddbbe058eb286d111b1225a526fe1fdf6b9343be086c6e。B3累计1；业务未修改。
输入仅本次B1：modules/M013-b1-self-raw.md V0.03 SHA256 d2222ac3a96165f0f17438191c2758de779e9ff1eeacc5f6f6968da63f70023f；modules/M013-b1-grok-raw.md V0.03 SHA256 d638a7134bdb7396534deb764548c3796c5c301761b42af97bbc36c0c323312a，真实exit0。已完整读取Grok353行，包括唯一GR011与12观察，不取历史独占锁段当当前。
## 逐ID复核
| ID | 判断 | 理由 |
|---|---|---|
| M013-001–008 | 保持否（已修） | 当前原修复仍在；不会因本轮新证据覆盖旧事实 |
| M013-009 | 保持否（XM→M392） | 当前OpenAPI角色六值仍在，绑定文档诚实登记 |
| M013-010 | 保持否（观察续记） | Grok OBS01/04/05/07/08/09/10/11/12和Codex非阻断观察完整入账 |
| M013-011 | 细化/工程是/队列是，PASS | 双方同一创建与change说明漂移合一；新增L99与待生效槽位来自Grok，原文逐处核对，保留完整审批缺口 |
| M013-012 | 细化/工程是/队列是，PASS | group-overview链没有project参数/过滤；三份文档对审批属性已闭合与隔离未闭合需分开，HTML应有隔离保留 |
| M013-013 | 细化/工程是/队列是，PASS | dailyTemporary分支与现有1433测试，非高危临时申请也进主管链；仅修错误排除条件 |
| M013-014 | 冲突/工程是/队列是，PASS | 平台管理员全权限例外与BSM-007冲突且actual wildcard=false；Immediate只验证立即生效普通码，待批高危不授出。候选未放宽任何生产入口 |
| M013-015 | 细化/工程是/队列是，PASS | V170 high为真实历史，V394改low为当前；写明两时点避免误用旧权限风险档 |
| M013-016 | 细化/工程是/队列是，PASS | Controller存在参数不代表接受覆盖；实际Service任一threshold非null拒绝；给可正常调用的无参数示例 |
| M013-017 | 跨模块细化/工程是/本模块队列否，PASS | 后台代码范围在M348/M032；本轮只修012说明，017持续未修，不能以文档关闭代码缺口 |
## 全量差集与处置
Grok GR011→011；Grok OBS02→009，OBS03/06→011，其他10观察→010。Codex N01–06依次011–016，O05→017，其余O01–04/06→009/010，零遗漏。正式编号新分配，不改Grok原编号或原票。旧010对旧实现创建active的历史观察保持，新实现使文字过期以011记录。
无添加项、无新HTML问题、无questions；原批准查看考勤的业务规则不重问不反改。PRD606观察不升格，不补业态开关。
候选16条原句在现文件对应行逐项断言且唯一匹配；4文件版本变化及现有日志说明均能归因011/012/013–016。只文档变更、无新代码/迁移/接口/开关；文档验证不强制TDD。
本方对全部候选独立核读实际代码后认可，未因Grok B1 group章节抽核绿而取消有新证据条目；最终以B2双席合取为准，不单方覆盖Grok。尚未读取Grok本批B2新增内容。
下一步：GrokB2交付后核完整票，双PASS才把findings定稿；否则整批修改候选再复核，不边复核边修业务。


## M013 B2-r3 Codex整批归并复核

版本：V0.05

结论：PASS（归并候选，不是B4/B5/模块PASS）。输入：findings.md V2.26 SHA256 39fd20cc554ccf352febd2b063f978b13f786aee2de020ca62e662138db02c44；本次完整M013-b4-r2-self-raw.md V0.01 SHA c7d26b9aeca2ce9abd64b523162b9e48083f9583465118f89befd395ae3fa15a；M013-b4-r2-grok-raw.md V0.01 SHA72aefb6fd603c46fb66003bbf56f6555115367f5056b19d4156a3902887ea45d，实际exit0。均全diff33677bytes/16hunk，双方归因PASS，修复内容本方FAIL/外审PASS，按合取回B2。

两席差集仅CX-B4-01与Grok§5/§9同一版本标注。Grok原观察原义保留；候选说明本方反驳，不把其原PASS改成FAIL。正式新增M013-018；没有重复编新后台问题。

| ID | 当前候选 | 复核 |
|---|---|---|
| M013-001–008 | 否 | 旧修复保留，现批没改这些已关闭事项 |
| M013-009 | 否 | 原OpenAPI entity_role跨模块M392登记，未修 |
| M013-010 | 否 | 既有观察续记，不把历史日期/已批查看考勤重新开队 |
| M013-011–016 | 是 | 两席新文内容均接受；B4合取FAIL按规则尚不能改否，下一B3核16处保持即可，不为了重复执行再制造差异 |
| M013-017 | 否 | 项目结果过滤后端缺口归M348，依赖M032，仍未修，不混入当前两行修改 |
| M013-018 | 是 | 新技术性工程缺陷，候选两行准确最小，GEN-016已冻结要求文档版本可验证；GEN-018命令输出same=false |

018四分类：HTML业务无冲突，版本元数据无业务上位细化；只为交接辨认当前文档版本，无业务概念/枚举/状态/权限/校验/价格/流程/默认值发明，归技术性。不是添加、无需新问答。必要性：同页打开即互斥的两处当前版本，L3有“现行开发设计/最后更新”，不能解释为历史日志；API正确不豁免GEN016文档版本。只改标题与L3，不加流程/测试/新日志，用户明确版本+.01所以V0.10→V0.11而非在V0.10偷偷补改。日期采用当前操作员America/Los_Angeles 2026-09-14，保留业务内容。

重核实际after tree ad9c27c34fcf3032cf42e330821e14f26d5e510e的L1/L3；B4静态probe输出title V0.10/metadata V0.09/same false，修改方案标题和metadata均V0.11可直接消除。时间字段同步只是该元数据同一行现行事实，不改历史记录。候选原句/拟改句/影响与实际文件逐字相符。

整批无遗漏、无新添加、人闸、HTML修改或锁外判据。原011–016保留业务修复＋018元数据2行构成本批全部YES。待Grok本批B2追加报告；其FAIL不可由本票覆盖。当前M013 IN_PROGRESS/B3累计2，本批B3尚未执行；收到双B2PASS才第3次整批修复。


## M013 获批追加修复B2 Claude席甲整批归并复核

版本：V0.06（本文件自身；V0.05及以前66673bytes前缀完整保留，本段追加；B2不写独占锁行）

结论：PASS（本批B2归并候选；不是B4/B5/模块PASS）。
输入：findings.md V2.31 全文件SHA256 70cfc3ba2c6016763f924f3a9dcc3ebac043200e2f2bc1038ef8d192a15b32c7，顶部「当前 M013 获批追加一次修复：B2整批候选（尚未定稿）」段；触发回退的两份完整B5-r3：modules/M013-b5-r3-self-raw.md V0.01 SHA256 bd610a579af0d1d91dd1dd52fe68b7c0f7e4a5b90a20b17932dce814e2f9b916；modules/M013-b5-r3-grok-raw.md 文件内第二个CRITERIA-LOCK行（L359）起本次完整报告V0.02，全文件SHA256 e4748561d14d8b6508dd8329a8a51a68601be6fac3b5e2a8a7a410efd355b808，适配器实际EXIT_CODE=0。本票封存前未读Grok本次B2正文。

### 差集：两份B5-r3完整报告全部条目 → 候选归属

| 来源条目 | 候选归属 | 本席复核 |
|---|---|---|
| CX-B5-01 | 新M013-019，冲突，是 | 见下「019」 |
| CX-B5-02 | 新M013-020，细化，是 | 见下「020」 |
| CX-B5-03 | 新M013-021，细化，是 | 见下「021」 |
| GR-B5R3-01 + 本席O-B5-02 | 新M013-022，细化，是 | 本席B5-r3曾以「表没有project_id仍为真」维持观察；B2核Grok证据后改认：绑定设计§3.1自称当前数据模型、§5自称当前API，漏已落地的V427项目关联表与关联端点，GEN-002/GEN-014/GEN-016要求入口与迁移声称与当前源一致，工程缺陷成立。两条同一事实并为一条，Grok原义保留 |
| GR-B5R3-02 | 新M013-023，细化，是 | 见下「023」 |
| O-B5-01、O-B5-03 | M013-010续记，技术性/观察，否 | 只登记，理由见候选行 |
| Grok「历史已批口径」 | M013-010续记，否 | 均为既有已登记观察或跨模块归属（M009观察、017→M348、009→M392），不重开 |

无漏并、无改写对方finding。首版候选V2.30漏并O-B5-01/O-B5-03，本席在对方运行约65秒时中止派发（目标raw未变），V2.31补齐；019–023及逐句原句/拟改在两版间未改一字。

### 逐项分类、队列与最小边界

- 019 冲突/是：PGM L658角色装配原文「HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板」、L669「职级影响默认权限模板和审批层级」；AccessService:1305–1313改职级按新职级映射经assignUserPositionDirect重算（M006-G-02）。business-decision-ledger.md:1936 DEC-R8-91原文「本裁决不扩展到未点名的“职级”或任意手工模板应用」是射程限定，不是「职级变化不得重算」的裁决；「责任范围变化」在DEC原文中不存在。四文件把「不扩展」写成「排除」并多加「责任范围变化」，与HTML相反，按2.1冲突删相反内容。边界：只删两处排除词，保留「任意手工模板应用」不触发（DEC原文与第6条免审射程均有）；集团PRD:5修订说明与PRD:966变更日志是当时记录，不改。删后五句语义完整（已逐句读L76/407/523/544/656）。同根句在M014/M421/M439单元内，超出本模块单元集，只登记跨模块观察，符合收窄以单元集为界。
- 020 细化/是：集团PRD:23表头标「当前实现 CHECK / 服务校验」却写4类，V392已把CHECK扩为六类，现库一致；申请入口:1580–1585只收三类、两类服务内生成（:56–57、:1792–1802）。拟改只陈述已发布迁移与代码现状，不新增类型或流程。
- 021 细化/是：G7把PGM L412–413要求写成「所有对象继续携带project_id」的完成事实，主体/绑定/角色/用户/模板表均无该列。选删除而非改写：改写会在验收包新增一条要求句，删除即可消除错误事实陈述，是最小处置。
- 022 细化/是：补V427多对多关联、创建关联当前项目（最高权限可指定已存在项目，creationProject:419–429）、列表按当前项目过滤（:92）、关联端点（PlatformController:97；:363–381须平台范围且最高权限）；保留「主体表本身无project_id列」事实。「持授权 U 盘密钥的最高权限」取BSM-004原话（PlatformService:277/296/970注释同），非自造。
- 023 细化/是：OpenAPI BusinessEntityRequest.status四值与服务SUBJECT_STATUSES五值不一致，按契约首次请求即400；本模块只在绑定设计披露，OpenAPI改动归M392（同009）。
- 010续记 技术性/观察/否：O-B5-01「建议」一词HTML未写明职级如何影响审批层级且无可复现相反实现；O-B5-03变更日志行序不影响可验证性。

### 执行一致性与添加判定

- 补丁脚本/home/james/audit-tmp/m013_b3r4_patch.py check在HEAD e456ba96c84185c8d39f18f548011daac48a6466当前工作区输出CHECK_OK 22 edits：22处原句各在对应文件恰好出现1次；候选逐句段由同一EDITS导出，B3按此逐字执行。
- 四文件内同根活句已grep收全：「仅职级」「责任范围变化」「职级变化」仅命中集团PRD L5（历史）、L76与平台治理PRD L407、L523、L544、L656、L966（历史），绑定设计与验收包零命中；活句五处全在候选内。
- 版本：绑定设计V0.11→V0.12（本文件无变更日志段，只改标题与页首）、集团PRD V1.02→V1.03加变更记录、验收包V0.05→V0.06加§11行、PRD V0.05→V0.06加变更日志行；均+0.01。
- 本批无添加项：拟改只删相反内容或陈述已发布迁移/现行代码事实，无新业务概念、枚举、状态、权限、校验、价格、流程、默认值；无HTML申请、无questions、无锁外判据。

下一步：Grok本批B2报告交付后核其完整票；双PASS才定稿并进B3第4次，任一FAIL按其意见改候选后重复②。


## M013 第二次获批追加修复B2 Claude席甲整批归并复核

版本：V0.07（本文件自身；V0.06及以前72465bytes前缀完整保留，本段追加；B2不写独占锁行）

结论：PASS（本批B2归并候选；不是B4/B5/模块PASS）。
输入：findings.md V2.35 顶部「当前 M013 第二次获批追加修复：B2整批候选（尚未定稿）」段（全文件SHA256见ledger V4.20）；触发回退的两份完整B5-r4：modules/M013-b5-r4-self-raw.md V0.01 SHA256 af260f344faee42bc7e3e8a705f6b51af2fc22f1454b7c9dc446e7c3a4388b77（FAIL，CX-B5R4-01）；modules/M013-b5-r4-grok-raw.md V0.01 SHA256 ec4a7b838abe2939ce52b396d2fdb21eeeadf23f5440dded3447182a019a8407（PASS，无新发现，适配器实际EXIT_CODE=0）。本票封存前未读Grok本次B2正文。

### 差集：两份B5-r4完整报告全部条目 → 候选归属
| 来源条目 | 候选归属 | 本席复核 |
|---|---|---|
| CX-B5R4-01（席甲） | 新M013-024，细化，是 | 见下 |
| O-B5R4-01（席甲观察） | 跨模块观察，不立案 | 服务提示语措辞归后台模块 |
| Grok B5-r4 84/84 否缺陷、无新发现 | 无条目可并 | Grok §5「主体status五码+归档走存档 :54–56、:893–899」是对照记录，不是finding；本席在候选表来源栏如实注明其依据 |
无漏并、无改写对方票。

### 024 分类、队列与最小边界
- 事实（类型4，只读）：PlatformController business-entities 映射仅 L42 GET、L52 POST、L97 POST …/projects；存档端点只有 merchants L108/L117 与 space-types L190/L199。PlatformService:893–900 `requireSubjectStatus` 提示「归档请使用存档操作」被 :119 createBusinessEntity 与 :1001 商户档案更新共用；:53 注释「归档只经存档端点（archived_at 轴）」针对商户（M009-004）。JdbcPlatformRepository 无 business_entities 存档写法。同文 :57「当前 status 未实现该完整词表和缓冲期闭环」、:108「完整存档/删除流程未在当前接口落地」、:121「也不存在主体完整 CRUD」。
- 四分类：HTML（BEQR-155 六态含归档）无与之相反的规则句→非冲突；括注是主体状态实现说明的当前事实误述→细化；无新业务概念/入口发明→非添加；工程缺陷成立（GEN-002 入口声称须与源码一致、GEN-016 当前事实可复现、GEN-001 同文自洽）。队列「是」。
- ID：CX-B5R4-01 是 B3-r4 按 023 拟改写入的新误述；023 原缺陷（OpenAPI status 四值裂口未披露）已落地无回归。按「无 ID 者分配新 ID」立 024 并注明来源，不重开 023，避免把已落地披露与新误述混为一项。
- 最小边界：方案A删括注最小——:57/:108 已登记归档词表与存档流程差距，删后 status 行只陈述服务可设五值，不重复也不扩写（同 021 先例）。方案B改写会在字段表内新增一句流程陈述，非必要。版本 V0.12→V0.13；L3 最后更新同日 2026-09-19 不变；本文件无变更日志段、不新造。不改代码/HTML/OpenAPI。
- 自省：该括注为本席在 B2-r4 候选中自拟，两席 B2/B4 四票均以服务提示语为据未核主体存档入口；本席在 B5-r4 全文重审时 grep Controller 才发现。已写入席甲工作记忆（B2 拟改凡写「走 X 端点」先按对象 grep Controller 映射）。

### 执行一致性与添加判定
- 补丁脚本 /home/james/audit-tmp/m013_b3r5_patch.py check 在当前工作区（B3-r4 after 态，HEAD e456ba96）输出 CHECK_OK 3 edits：3 处原句各恰好 1 次；候选逐句段由同一 EDITS 导出，B3 按此逐字执行。
- 001–023 不动；本批无添加项、无 HTML 申请、无 questions、无锁外判据。

下一步：Grok 本批 B2 报告交付后核其完整票；双 PASS 才定稿并进 B3 第 5 次，任一 FAIL 按其意见改候选后重复②。
