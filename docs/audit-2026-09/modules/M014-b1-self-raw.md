CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M014 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理。2026-09-11T04:37:18Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `685dca4e1088ed59e68196c3c5b45787fde9db02`；工作区 tree `1c216eaa89684cc7bd772af11ba91db34cecdb2b`。三份业务 HTML 只读。
- 模块 M014「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段」，1 文件（计划冻结 3494 行），映射 84 判据（BEQR 21、BSM 12、GEN 18、PGM 33）。
- 本模块为 OVERSIZE-SINGLE-FILE：按 runbook V0.02 分段阅读，§3 列出七段全文覆盖表；本报告对象为 main HEAD 685dca4e（PR #434 已合并）的 V0.05 单文件。
- 顺序：Grok 先派、真实退出后本席才写本报告；本报告落盘前未读取 `M014-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/platform-governance-product-spec-v0.2.md | 3494 | 221816 | b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31 | 全读 |

## 2. 可执行证据原文（/tmp/m014_b1_run.log）

```
START=2026-09-11T04:01:13Z HEAD=5692a895 origin/main=5692a895 porcelain=10
== 0. spec fingerprint
3494 221816 b3ee635aa1763d1f5b1e6e7c7e381fe78bc71057c162caad39ae8cd3b8bbcd31
# 平台治理产品规格 V0.05
== 1. approval levels in code (spec §8.1/§24.3 say code still uses level_rank arithmetic + specific_role)
backend/src/main/java/com/complex/platform/customer/CustomerMemberLevelRecord.java:15:        int levelRank,
backend/src/main/java/com/complex/platform/customer/LoyaltyService.java:401:        if (upgradeTarget != null && upgradeTarget.levelRank() > rankOf(current)) {
backend/src/main/java/com/complex/platform/customer/LoyaltyService.java:434:        boolean down = target != null && target.levelRank() < rankOf(current);
backend/src/main/java/com/complex/platform/customer/LoyaltyService.java:660:            if (progress >= level.upgradeThreshold() && (best == null || level.levelRank() > b
backend/src/main/java/com/complex/platform/customer/LoyaltyService.java:668:        return level == null ? -1 : level.levelRank();
backend/src/main/java/com/complex/platform/customer/dto/MemberLevelConfigRequest.java:13:        Integer levelRank,
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:825:                    id, project_id, level_code, level_name, level_rank, upgrade_thresh
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:830:                    :id, :project_id, :level_code, :level_name, :level_rank, :upgrade_
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:838:                    level_rank = EXCLUDED.level_rank,
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:851:                .addValue("level_rank", level.levelRank())
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:869:                 ORDER BY level_rank ASC, id ASC
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:990:                 ORDER BY level_rank ASC, id ASC
backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java:1033:                rs.getInt("level_rank"),
backend/src/main/java/com/complex/platform/customer/CustomerRepository.java:85:    /** 按 project 升序排（level_rank）列出生效等级配置。projectId 可空（
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java:15:public record SupervisorReviewPolicyRecord(
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java-16-        String policyCode,
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java-17-        String actionType,
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewPolicyRecord.java-18-        long minAmountCent,
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:2:-- 背景：R3 确认 admission_audits/approval_level 零命中�
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:4:--   approval_level         = 升级类型枚举（上一级/上
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:9:    ADD COLUMN IF NOT EXISTS approval_level VARCHAR(32);
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:15:    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 
V057__governance_supervisor_review_approval_level.sql
V068__action_gate_switches_admission_policies.sql
== 2. field projection / sensitive view (spec §24.2)
fieldprojection_files=0
backend/src/main/java/com/complex/platform/compliance/WecomConversationService.java:42: *       返回原文并写 raw 审计 + 事件 wecom.raw_viewe
backend/src/main/java/com/complex/platform/compliance/WecomConversationService.java:353:                    "sensitive.viewed",
backend/src/main/resources/db/migration/V251__permission_names_i18n_zh.sql:146:UPDATE permissions SET permission_name = '查看敏感证据字段' WHE
backend/src/main/java/com/complex/platform/common/PiiTextRedactor.java
backend/src/main/java/com/complex/platform/common/PhoneMask.java
== 3. effective-permissions (spec §24.1)
backend/src/main/java/com/complex/platform/access/AccessController.java:244:    @GetMapping("/effective-permissions")
backend/src/main/java/com/complex/platform/access/AccessController.java:250:    @GetMapping("/users/{id}/effective-permissions")
backend/src/main/java/com/complex/platform/access/AuthorizationService.java:151:        return computeEffectivePermissions(actor.id(), isPlatformAdmin
backend/src/main/java/com/complex/platform/access/AuthorizationService.java:174:        return computeEffectivePermissions(userId, wildcard);
backend/src/main/java/com/complex/platform/access/AuthorizationService.java:177:    private EffectivePermissions computeEffectivePermissions(String us
backend/src/main/java/com/complex/platform/access/AuthorizationService.java:360:     * <p>语义对齐 {@link #computeEffectivePermissions}：DENY 优
apps/web/src/admin/AdminApp.tsx
== 4. terminal type codes (spec §3.1 dealer_portal vs 补录 distributor_web)
backend/src/main/java/com/complex/platform/amusement/AmusementMaintenanceService.java:32:            "staff_private_phone", "merchant_portal", "merchant_app", "
backend/src/main/java/com/complex/platform/terminal/TerminalCanaryService.java:37:            "customer_app", "customer_miniapp", "web_admin", "mobile_admin",
== 5. 业态 codes (spec §6 bar/catering/retail/amusement/media/other)
46:            Set.of("bar", "catering", "retail", "amusement", "media", "other");
backend/src/main/resources/db/migration/V348__merchant_business_format.sql:18:    CHECK (merchant_type IN ('bar', 'catering', 'retail', 'amusement', 'other'));
backend/src/main/resources/db/migration/V384__merchant_business_format_media.sql:19:    CHECK (merchant_type IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
== 6. tables referenced by spec
merchant_credentials: 
contract_key_dates: V028__p1_contract_compliance_expiry.sql 
staff_levels: V055__access_staff_levels_and_salary.sql 
staff_level_assignments: V055__access_staff_levels_and_salary.sql 
supervisor_review_decisions: V050__governance_supervisor_review_persistence.sql 
organizations: 
contract_admission_rules: 
admission_audits: 
space_type_dictionary: V184__space_type_dictionary.sql 
employee_notices: V086__staff_tables.sql 
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:2:-- 背景：R3 确认 admission_audits/appro
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:4:--   approval_level         = 升级类型�
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:9:    ADD COLUMN IF NOT EXISTS approval_level 
backend/src/main/resources/db/migration/V057__governance_supervisor_review_approval_level.sql:15:    IF NOT EXISTS (SELECT 1 FROM pg_constra
== 7. mall root node (spec L1291: 技术根节点为 mall)
psql=skip (read-only file probes only)
== 8. admin routes: 合同准入规则页 / AdmissionAudits
23:  { pageId: "ADM-GOV-005", label: "商户准入审批流水", module: "治理", group: "governance", route: "/admin/governance/admission-audits", requiredPer
AdmissionAuditsPage.guidedcreate.test.tsx
AdmissionAuditsPage.test.tsx
AdmissionAuditsPage.tsx
== 9. HTML: 缓冲期天数 / 终端开关优先级 / 审批转发 / 十一端
docs/business-enum-quick-reference-v0.1.html:238:    商户/运营主体状态待准入、正常、暂停、终止、归档、黑名单（试运营是商家自己的经营状态，系统不管、不记录）。暂停与列入黑名单由招商部门与运营�
docs/business-enum-quick-reference-v0.1.html:238:    商户/运营主体状态待准入、正常、暂停、终止、归档、黑名单（试运营是商家自己的经营状态，系统不管、不�
docs/platform-governance-map-for-operations-v0.1.html:529:                全平台审批通用时限：任一业务审批节点超过后台配置的时限（业务参数，按审批类型可配，出厂默认 3 个工�
docs/business-enum-quick-reference-v0.1.html:236:    终端开关优先级全局 &gt; 综合体 &gt; 商户 &gt; 业务系统 &gt; 模块 &gt; 版本/设备 &gt; 灰度；拉取失败按“兜底关�
docs/platform-governance-map-for-operations-v0.1.html:734:                开关优先级：全局 &gt; 综合体 &gt; 商户 &gt; 业务系统 &gt; 模块 &gt; 版本/设备 &gt; 灰度，高层级�
== 10. decision ledger: 缓冲期 rulings
1181:| DEC-R4-2026-08-04-71 | **删除商户状态「试运营」**（商家自己的经营状态，系统不管不记录）；**暂停与列入黑名单由招商部门与运营部门决定**，退出
2258:| **DEC-P1A-10** | 2026-08-25 | ENUM 商户退出缓冲期括注改「上下限出厂为三十至六十天，出厂默认三十天」（原文误把上下限区间写进出厂默认；默认单�
== 11. spec internal refs / typos
48:> **名词四层（业务口径）**：文中「综合体项目 / 商户（运营主体）/ 项目公司 / 组织主�
335:> **2026-07-04 逻辑一致性修订（准入落库口径收口）**：本页「准入规则 / 拦截记录 / 准入
docs/merchant-space-contract-model-v0.1.md
docs/operation-traceability-and-terminal-switch-prd-v0.1.md
docs/scope-wide-coding-rule-canonical-V0.01.md
== 12. analytics.group.view stale sentences in spec (M013-002 same root)
761:11. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归�
2721:12. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归�
== 13. evaluate / AdmissionGuard wiring (spec header L8 R5 note)
backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewController.java:47:        return ApiResponse.ok(serv
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:44:        var review = reviews.evaluate(new Sup
backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:61:        var review = reviews.evaluate(new Sup
backend/src/main/java/com/complex/platform/amusement/AmusementProjectSpaceAssignmentRequest.java
backend/src/main/java/com/complex/platform/bar/BarTableSpaceAssignmentRequest.java
backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java
backend/src/main/java/com/complex/platform/gate/JdbcAdmissionFactRepository.java
backend/src/main/java/com/complex/platform/gate/AdmissionGuard.java
== 14. terminal switch effective/expire fields in code (spec §14A/§3.1)
== 15. HTML sha (read-only)
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-
END=2026-09-11T04:01:13Z
```

## 3. 三线审计方法与覆盖（OVERSIZE-SINGLE-FILE 分段覆盖表）

| 段 | 行区间 | 内容 | 覆盖 |
|---|---|---|---|
| 头部与 §0A | L1–L33 | 版本、R5 标注、合并说明、现行 HTML 增量承接 | 全读 |
| 页面清单 | L35–L786 | §1–§20（含 §9.6 准入规则页、§10.6 注册审批页、§14A 终端开关页） | 全读 |
| 字段字典 | L788–L1328 | §1–§19（含 §3.1 终端开关字段、§8.1 审批级别、§14 状态字典） | 全读 |
| 关键流程图 | L1330–L2244 | F1–F11 十一条流程与验收 | 全读 |
| 低保真原型 | L2246–L2740 | §1–§23 | 全读 |
| §24 深度补齐 | L2742–L2852 | 有效权限集接口、字段级权限、审批层级映射 | 全读 |
| 继续合并补录 | L2854–L3494 | 三份源文件历史原文补录（已废止标注） | 全读 |

- 对象口径：main HEAD 5692a895 的单文件 3494 行（V0.05，221816 bytes，SHA b3ee635a…）；三份业务 HTML 取 main 正本（BSM V0.14、PGM V0.12、BEQR V0.07）；判据锁 V0.11，M014 映射 84 项（与 M013 同集）。
- 第一性原理线：规格的页面/字段/流程/状态/终端/审批层级陈述是否与三份 HTML 及 main 实现事实一致；凡把「当前实现/物理实现」写成过期或不存在的对象即文档缺陷（GEN-016/GEN-008）；内部四份原文件与补录之间是否自相矛盾（GEN-016）。
- 顺从式线：84 项判据逐族对照（§5）。对抗式线：假定开发者按规格实现——按 §3.1 终端代码表接线会用到不存在的代码值、按 L1291 建 `mall` 根节点会被字典硬校验拒绝、按 §17A.2 加生效/失效校验会与 F11 已废止规则冲突、按 §18/§21 继续把 `analytics.group.view` 当缺口会重复修复。
- 只读依赖追流：`PlatformService`（业态六码、createSpace 字典校验）、V184、V394、V057、V028、V055、`AccessController`/`AuthorizationService`（effective-permissions）、`SupervisorReviewService`（审批层级）、`gate` 包、`AdmissionAuditsPage`/`adminRoutes.ts`、OpenAPI `x-terminal-allowed` 值集、三份 HTML 现文（§2 探针原文）。
- 本席禁止修复；只读命令见 §2。

## 4. 与 main 实现事实 / HTML 现文的分叉对照（要害）

| 规格陈述 | main / HTML 事实（§2） | 结论 |
|---|---|---|
| L761（页面清单 §18 第 11 条）、L2721（低保真 §21 第 12 条）「`analytics.group.view` 审批属性的当前实现缺口按该 PRD 登记」 | V394 已置 requires_approval=TRUE；集团 PRD/验收包/PRD 已于 M013-002 改为已闭合 | 过期（CX-001） |
| L1291「物理实现为扁平 `spaces`：`space_type` 标识上述九类空间对象，技术根节点为 `mall`，通过 `parent_space_id` 串联」 | `createSpace` 只接受 `space_type_dictionary` active 码（V184：building/floor/zone/shop/public_area/parking/booth/other），governance 包 0 处 `"mall"`；`mall` 仅存在于 OpenAPI 枚举快照 | 过期/失实（CX-002） |
| 字段字典 §3.1 L865 终端代码 `dealer_portal(独立经销商 Web 门户)`；补录 L3245 同表写 `distributor_web` | OpenAPI `x-terminal-allowed` 值集：app/miniapp/web/tablet/staff_work_phone/staff_private_phone/merchant_portal/merchant_app/kiosk/mobile_admin（十值）；迁移/Java/OpenAPI 0 处 dealer_portal 或 distributor_web | 内部不一致 + 实现无该值（CX-003） |
| 字段字典 §6 L923「业态编号 必填 选择 bar/catering/retail/amusement/media/other」 | 补录 L3287 已把「业态编号」标【已废止/不得回灌】（ENUM 只有六值名称）；main `PlatformService:46` 与 V384 CHECK 六码同值 | 内部矛盾（CX-004） |
| 终端开关页三处：低保真 §17A.2 L2643「生效时间不能晚于失效时间」；页面清单 §14A.3 L626 开关列表含「状态」列；低保真 §17A.1 L2634 列表含生效/失效时间列 | 流程图 §12A.7 L2146 已把「生效时间晚于失效时间阻止保存」标【已废止/不得回灌】；§14A.5/§14.13 终端开关状态不定义；L626/L2969 列表口径为「是否启用」 | 内部矛盾（CX-005） |
| 字段字典 §7 L955「证照/资质/保险到期字段按 `merchant_credentials` 扩展或通用到期事项台账承载」 | 迁移与 Java 0 处 `merchant_credentials`；到期事项实际由 V028 `compliance_expiry_items`/`compliance_reminders`（`compliance` 包）承载 | 引用不存在对象（CX-006） |
| 页面清单 §9.6 L335「另见 §2999、§24.3」 | 全文无 §2999 节（L2999 为补录历史行「复杂审批页进入 P1」） | 悬空引用（CX-007） |
| §24.1 有效权限集接口 | `AccessController:244/250` 两端点、`AuthorizationService.computeEffectivePermissions`、`AdminApp.tsx` 引用均存在 | 一致 |
| §24.2 补记「FieldProjection 一组类已移除」 | governance/access 包 0 处 FieldProjection/FieldVisibility；`PiiTextRedactor`/`PhoneMask` 固定脱敏存在 | 一致 |
| §24.3 代码核验（2026-06-19）「现网仍按职级算术与 specific_role 实现」 | `SupervisorReviewService:350-415` 仍有 `ApprovalLevel.SPECIFIC_ROLE` 与「按 approval_level + 发起人 level_rank 计算所需审批人 rank」；V057 CHECK 含 specific_role；同时 `generateChain(...finalApproverId, parallelLines...)` 已有指定最终审批人+平行线 | 注记仍为真（实现缺口，非本模块文件；观察） |
| §8.1/§24.3.4 审批转发规则 | PGM L529 逐字同旨（逐单转发、原审批人保留、任一人审批、无常设代理、最高权限逐单转发） | 一致 |
| §12A.6 涉钱禁灰度/营业时段禁变更/兜底关闭 | PGM L734/L737/L738、ENUM L236 同旨 | 一致 |
| §14.2 商户终止缓冲期「上下限出厂为三十至六十天，出厂默认三十天」 | 决策台账 DEC-P1A-10（2026-08-25）已把 ENUM 括注改为同句 | 一致 |
| 十一端列表、状态闭集（项目六态/商户六态/合同八态/员工五态/设备七态/权限五态/事件七态）、五类高危、合作模式六类、业态六值 | ENUM/PGM 逐字同向（M013 已核） | 一致 |
| 表引用 `contract_key_dates`（V028）、`staff_levels`/`staff_level_assignments`（V055）、`supervisor_review_decisions.approval_level`（V050/V057）、无 `organizations`/`admission_audits`/`contract_admission_rules` 表 | 均与 L48/L335/§24.3.3 陈述一致 | 一致 |

## 5. 84 项判据逐族核对

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143/144/145/146/147/148（四类主体、多对多、城市项目公司、运营公司、收入归属、合作模式） | 通过（登记） | L48 名词四层与 `business_entities` 一表多角色同向；§7 主体绑定关系编号/四类主体、合作模式六类与 ENUM 同 |
| BEQR-149/151/152/157/159、PGM-040/041/042 | 通过 | 职级 M1–M10 上中下、最终权限五源减显式拒绝（§24.1.2）、五类高危（F9 L1981）、员工五态、权限五态与 HTML 同 |
| BEQR-150/153、PGM-010 | 通过（登记） | 十一端名称列表与 ENUM 同；七档优先级与 ENUM L236/PGM L734 同；代码映射表见 CX-003 |
| BEQR-154/155/156/158 | 通过 | 项目六态、商户六态（含缓冲期 DEC-P1A-10）、合同八态、设备七态与 ENUM 同 |
| BEQR-063/166/189、PGM-007/026/027/031/035 | 通过 | 合同主线+三平行线（§9.4/F3）、§0A 经销商治理、五类高危同向；管理费方式本文件无实现面 |
| BEQR-074 | 无实现面 | 全文无隐私桌台 |
| BSM-004/005/006/007/009 | 通过（登记） | 留痕不可删只作废/冲正、U 盘密钥清理、分步表单结构；动态二维码无本文件展开 |
| BSM-028/029/072/074/114/115/120 | 通过（登记） | 私人手机 App 边界（L211/L2531/L2675 不承接现场业务）与 HTML 同向；私人手机功能列表本文件未逐项列举 |
| PGM-002…021、043/044/048/060/065/066 | 通过（登记） | 治理顺序、空间点位九类（L1291 物理实现句见 CX-002）、集团视图已承接（缺口句见 CX-001）、外包/审计导出脱敏（§2.3）同向 |
| GEN-001 必要性 | 通过 | 四份原文件均为 P0 页面/字段/流程/原型的现行入口 |
| GEN-002 可执行正确性 | 不通过 | CX-002/003/006：按规格实现会用到不存在的根节点码、终端代码值与表名 |
| GEN-003/004/005/006/007/009/010/011/012/014 | 不适用或无实现面 | 文档模块 |
| GEN-008 接口一致性 | 不通过 | CX-002/003（枚举/代码映射与实现不一致） |
| GEN-013 构建 | 通过 | 引用文档（merchant-space-contract-model、operation-traceability PRD、canonical）与迁移 V050/V055/V057/V068/V184 均存在 |
| GEN-015 中文 | 通过 | 正文中文，代码值仅作标识 |
| GEN-016 文档 | 不通过 | CX-001…007（过期陈述、内部矛盾、悬空引用） |
| GEN-017 分类 | 通过 | 无添加项；未把 HTML 沉默判冲突 |
| GEN-018 证据 | 通过 | §2 命令原文 |

## 6. 本次 Findings（临时 ID CX-M014-00N；正式 ID 由 B2 分配）

### CX-M014-001 · 细化 · 集团视图审批属性「当前实现缺口」两处过期（与 M013-002 同根因）

- 位置：`docs/platform-governance-product-spec-v0.2.md:761`（页面清单 §18 后续细化第 11 条）、`:2721`（低保真 §21 后续细化第 12 条）。
- 事实：V394 `UPDATE permissions SET requires_approval = TRUE … 'analytics.group.view'`；M013 B3 已把集团 PRD/验收包/PRD 同句改为已闭合。
- 判据：GEN-016/GEN-002/PGM-041；依据类型 4。工程缺陷：是。建议入队：是。最小处置：两处改为「审批属性已由 V394 置 requires_approval=true（集团治理 PRD V1.01 §4）」。

### CX-M014-002 · 技术性 · L1291「技术根节点为 mall」与字典硬校验不符

- 位置：`:1291`（字段字典 §17 后续细化第 2 条括注）。
- 事实：`PlatformService:474-478` createSpace 以 `space_type_dictionary` active 码硬校验；V184 八码无 `mall`；governance 包 0 处 `"mall"`；`mall` 仅在 OpenAPI 枚举快照（M013-004 已在 PRD §6.2.1 登记为非活码）。
- 判据：GEN-016/GEN-008/GEN-002/PGM-005；依据类型 4。工程缺陷：是。建议入队：是。最小处置：括注改为「物理实现为扁平 `spaces`：`space_type` 以空间类型字典（V184：building/floor/zone/shop/public_area/parking/booth/other，后台可维护）为活校验，通过 `parent_space_id` 串联；OpenAPI 枚举中的 `mall` 仅为契约快照、不是活码」。

### CX-M014-003 · 技术性 · 终端代码映射表内部不一致且第十一端无实现代码值

- 位置：`:865`（字段字典 §3.1 校验口径列 `dealer_portal(独立经销商 Web 门户)`）与 `:3245`（补录同表 `distributor_web(独立经销商 Web 门户)`）。
- 事实：V111 `chk_tcs_terminal_type` CHECK 为 app/miniapp/web/tablet/staff_work_phone/staff_private_phone/merchant_portal/merchant_app/kiosk/mobile_admin 十值 + `*`；OpenAPI `x-terminal-allowed` 值集同十值；迁移、Java、OpenAPI 0 处 `dealer_portal`/`distributor_web`。业务十一端名称与 ENUM 一致，仅代码映射失实。
- 判据：GEN-008/GEN-016/BEQR-150；依据类型 4。工程缺陷：是。建议入队：是。最小处置：§3.1 该单元格改为「独立经销商 Web 门户（代码值待实现：当前 OpenAPI/迁移无对应终端代码，以实现批次落定为准）」，补录 L3245 加订正括注与 §3.1 统一；不在本模块改 OpenAPI/迁移。

### CX-M014-004 · 细化 · 「业态编号」必填字段与补录已废止标注矛盾

- 位置：`:923`（字段字典 §6 业态编号行）；对照 `:3287`（补录：业态编号【已废止/不得回灌】，ENUM 只有六值名称）。
- 事实：ENUM 业态只有六值名称；main `PlatformService:46`/V384 六码 bar/catering/retail/amusement/media/other 是实现标识。
- 判据：GEN-016/GEN-017/BSM-072；依据类型 1+4。工程缺陷：是。建议入队：是。最小处置：§6 该行改为「业态代码值（实现映射，系统只读，非业务必填字段）」，校验口径保留六码以便对照，不再写「必填」。

### CX-M014-005 · 细化 · 终端开关页三处内部不一致（已废止校验句回灌、未定义的「状态」列）

- 位置：`:2643`（低保真 §17A.2「生效时间不能晚于失效时间」）与 `:2146`（流程图 §12A.7 已废止同一校验）；`:626`（页面清单 §14A.3 开关列表含「状态」）与 `:643`/`:1236`（终端开关状态不定义）；`:2634`（低保真 §17A.1 列表含生效时间/失效时间）与 `:626`/`:2969`（列表口径为是否启用+技术字段不作开关控制）。
- 判据：GEN-016/PGM-010/BEQR-153；依据类型 1。工程缺陷：是。建议入队：是。最小处置：L2643 删「生效时间不能晚于失效时间。」或标【已废止/不得回灌】；L626「状态」改「是否启用」（去掉未定义状态列）；L2634 列表与 L626 对齐（生效/失效时间标为技术字段）。

### CX-M014-006 · 技术性 · 引用不存在的 `merchant_credentials` 表

- 位置：`:955`（字段字典 §7 资质信息行）。
- 事实：迁移与 Java 0 处 `merchant_credentials`；到期事项由 V028 `contract_key_dates`/`compliance_expiry_items`/`compliance_reminders` 与 `compliance` 包承载。
- 判据：GEN-016/GEN-008；依据类型 4。工程缺陷：是。建议入队：是。最小处置：改为「证照/资质/保险到期事项由合规到期台账（V028 `compliance_expiry_items`/`compliance_reminders`）承载」。

### CX-M014-007 · 技术性 · §9.6 悬空引用「§2999」

- 位置：`:335`「另见 §2999、§24.3」。
- 事实：全文无 §2999 节；L2999 为补录历史行「复杂审批页进入 P1」。
- 判据：GEN-016；依据类型 4。工程缺陷：是。建议入队：是。最小处置：改为「另见 §24.3 与补录段合同管理页历史行」或删去「§2999、」。

## 7. 观察（登记、不入队）

- OBS-1：头部 R5 标注（L6–L11，2026-06-18 日期化只读审计记录）所称「evaluate 零调用、闸口不触发、AdmissionAuditsPage 伪闭环」已部分被超越：`StaffApprovalService` 两处调用 `evaluate`；`gate` 包 `AdmissionGuard`/`ActionGateAspect`/`@GuardedAction` 已在 ticketing(2)/bar(2)/amusement(2)/settlement(1)/inventory(5) 等包接线（§2 第 e 项）；`adminRoutes.ts` ADM-GOV-005「商户准入审批流水」/`AdmissionAuditsPage` 存在。属日期化历史标注（自称「只读审计后回填，未改代码」），是否升为现状订正交 B2 裁定。
- OBS-2：§24.3 代码核验（2026-06-19）「现网仍按职级算术与 specific_role 实现」仍为真：`SupervisorReviewService:350-415`、V057 CHECK 含 specific_role，与 PGM 三档口径不一致——实现缺口，归 governance/review 实现模块（跨模块观察，本模块文档陈述正确）。
- OBS-3：版本号仅在标题（V0.05），四份原文件各带独立变更日志，合并文件无总变更日志；B3 版本递增 V0.05→V0.06 并在头部加一行变更记录即可。
- OBS-4：§11/§17 权限模板 basic/temporary 为产品设计，main 仅有 position_role_templates（V169）与 temporary_permission_grants（V170）——规格未自称「当前实现」，不升缺陷。
- OBS-5：多处修订说明（L425/L1022/L1721/L2514）仍复述「设备公钥/不可逆设备指纹 device_key_fingerprint」旧定义，但同节已加【已废止/不得回灌】与「未立法实现证据」标注；U01-007 已按 Keystore 指纹实现，与「不得升为已确认规则」口径不冲突。
- 技能：`using-superpowers`、`verification-before-completion`（探针原文附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。单文件 3494 行按七段全读并逐段核对、84 项判据逐族核对；7 项候选（细化 3、技术性 4）建议入 B2 修复队列；无添加项；三份 HTML 无需人闸修改；未修源、未提交/推送。本报告落盘前未读取 `M014-b1-grok-raw.md` 任何正文。


## M014 B2-r1 整批候选复核（席甲票）
版本：V0.02（本文件自身；B1 正文 V0.01 原字节 29222 bytes 保留，本段追加）

- 输入：`modules/M014-b1-self-raw.md` V0.01 前缀 29222 bytes SHA256 08a613b24236b34d9c2013e99d683d950b6bc6904839080f098d8c48783d6881；`modules/M014-b1-grok-raw.md` V0.01 前缀 53179 bytes SHA256 ebadd23d29e84e2553381b28aa75b732302e26a717df04df92e2b36a3990e584（Grok B1：FAIL，GR-M014-001…016，OBS-01…12）；候选 `findings.md` V1.87 747935 bytes SHA256 a43263fa2753d713b2d527e09bf4c5ab1d2243c5615221caf9649a5a02cf0b8d「当前 M014 B2-r1 整批候选」M014-001…020。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。

### 差集与 Grok 独有项核实（receiving-code-review，§2 探针原文）

- GR-001：PGM L469「维护楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位」九类无「项目」；L896 伪称 HTML 并集含项目、L903 把项目列为 space_type 选项。接受，001 冲突。
- GR-003：L2675 与 PGM V0.12 L741 / BSM V0.14 L930/L1865 逐字对照，规格把低风险功能收成「通知和状态查看」。接受，003 冲突。
- GR-005：`ActionGateAspect.gate`（terminalSwitchGuard.check→admissionGuard.check→requirePermission）、`AdmissionGuard.blockToReview`→`SupervisorReviewService.blockForAdmission`、`StaffApprovalService:44,61`。席甲 B1 只记 OBS-1，接受升队，005 细化（只加现状行，不删历史）。
- GR-006：`merchant-space-contract-model-v0.1.md:64-66` 仍把 `parent_entity_id` 与旧白名单写成当前代码结构；该文归 M261。接受，006 冲突 + 019 XM。
- GR-007：BEQR L239 合同状态只给八成员与当地时间，无迁移边；同文件其它对象均不定义边。接受，007 细化。
- GR-008/012/013/014/015：L2748 标题路径、总览图无 F11、L1541 非法 mermaid 行、L3343 未标废止、§3.1 一级导航与 §21 相反——逐行核对成立。接受。
- GR-016：四处修订说明仍写 fingerprint 为现行改写结果；与席甲 OBS-5 同源，接受升队。
- 席甲独有 017（`merchant_credentials` 0 命中，V028 合规表为实际承载）、018（§2999 悬空）以类型 4 依据成立。
- 差集：GR-001…016 → 001…016（+019）；OBS-01…12 → 020；CX-001…007 → 004/002/010/009/011/017/018。无遗漏、无改写 Grok 原意。

### 逐 ID 票

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（冲突／是） | 伪称 HTML 并集；改 L896/L903 并标注树首项即可，不删「项目根」措辞 |
| 002 | PASS（细化／是） | 与 M013-004 同口径 |
| 003 | PASS（冲突／是） | 按 HTML 现文列低风险功能闭集，禁止项不变 |
| 004 | PASS（细化／是） | V394 唯一根因，两处改 |
| 005 | PASS（细化／是） | 日期化标注加一行现状最懒，不改历史 |
| 006 | PASS（冲突／是） | 去掉「以该文为准」，权威回到 HTML+§0A |
| 007 | PASS（细化／是） | 删图改一句；合同审批链另由 §9.4/F3 承接 |
| 008 | PASS（技术性／是） | 标题一处 |
| 009 | PASS（细化／是） | 只读实现映射，不立法字段 |
| 010 | PASS（技术性／是） | 统一一码并标未实现 |
| 011 | PASS（细化／是） | 三处收口 |
| 012 | PASS（技术性／是） | 加节点与边 |
| 013 | PASS（技术性／是） | 改注释行 |
| 014 | PASS（细化／是） | 加废止前缀 |
| 015 | PASS（细化／是） | 表头改分组 |
| 016 | PASS（细化／是） | 四处修订说明改现行定义 |
| 017 | PASS（技术性／是） | 表名一句 |
| 018 | PASS（技术性／是） | 引用一处 |
| 019 | PASS（否，XM→M261） | 非本模块文件 |
| 020 | PASS（否） | 与两席观察一致 |

本席 B2-r1 票：**PASS**（队列是＝001–018；否＝019、020）。B3-r1 边界：只改本模块单文件（V0.05→V0.06，头部一行变更记录 + 三张原文件变更日志各一行），不改源码/迁移/OpenAPI/三份 HTML/锁/其它文档；补录历史原文只加订正括注或废止前缀，不删。无测试面，B4 以 diff 逐块归因复审。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。


## M014 B2-r2 修订候选复核（席甲票）
版本：V0.03（本文件自身；前 33645 bytes 原字节保留，本段追加）

- 输入：`modules/M014-b1-grok-raw.md` V0.02 前缀 78928 bytes SHA256 8518cb715242c96a6362df6a7c56389d1bb67e0d137078b0c1f7d3965617a531（Grok B2-r1：FAIL，否决点 001/009，其余 PASS）；r2 候选 `findings.md` V1.88 750964 bytes SHA256 557fd7bfa6e2d6175381bf2c871289ee3e21225f517596859fdff6a71542e907。
- 本段不写独占锁行；锁实算仍为 b012328f…6695a。席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r2 真实退出后、读取其追加段之前落盘。

### 对 Grok B2-r1 否决点的核实与修订（receiving-code-review）

- 001：本席复核 L892「## 5. 空间对象字段（项目、楼层、…）」与 L900「识别项目、楼层、…」——确为同根现行活句，r1 候选漏收；r2 已补（节标题注归属根、空间名称去项目），L56/L174/L2276/L902 保留。接受。
- 009：本席 r1「业态代码值 只读实现映射」仍是一条 P0 字段行，与 GR-009「不得立法另外五码」相悖；r2 改为跟随补录标【已废止/不得回灌】并只在业态名称行括注 media。接受（本席 CX-004 原处置作废）。
- 非阻断：003 禁止项对齐 BSM L930 现场句；005 现状行紧接 L9 并声明 L7–L9 为历史；011 L626 删「、状态」、L2643 只废止前半句；020 事实栏 V217 句为邻模块口径误抄，作废。均并入 B3 边界。

### 逐 ID 票（r2）

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（冲突／是） | 六处同根活句收全，归属根措辞保留 |
| 009 | PASS（细化／是） | 跟随补录废止，不新建字段 |
| 002–008、010–020 | PASS（r2 未变） | 与 r1 票相同；020 事实栏勘误已登记 |

本席 B2-r2 票：**PASS**（队列是＝001–018；否＝019、020）。B3-r1 边界以 findings V1.88 r2 节 + r1 原表为准。不以本席票覆盖外审。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M014 B1 席甲（Claude）整模块三线全量审计（预检发现问题后重开）
文件版本：V0.04（本文件自身；前 35645 bytes 原字节保留，本段追加；上行锁哈希为本席在 160 对 plan/criteria-lock.md 用 sha256sum 实算，与 sidecar、ledger 顶行同值）
审计结论：FAIL
当前步骤：B1（重开）；M014/471；ledger 为 IN_PROGRESS；历史 B3 累计 1（2026-09-11 B3-r1）保留，本次尚未 B3。本票只代表席甲；Grok 本次 B1 报告落盘并各自封存后才互读、进 B2。

## 1. 范围、身份与独立性
- 席甲执行者：Claude（claude-fable-5-1），按操作员「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」续作；M013 已 B5-r5 双 PASS 收口后进入 M014，预检发现问题重开。只读审计，不改业务文件、HTML、锁、findings、ledger；不提交、不推送。
- 正本 /home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466；审计对象为主工作区当前内容（含已暂存未提交修复）。
- 范围：plan/modules.md V0.04 M014 单文件 docs/platform-governance-product-spec-v0.2.md（V0.06，3491 行，SHA256 dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396；计划冻结 3494 行不改；OVERSIZE-SINGLE-FILE）。本席分七段全文通读：1–500、500–1000、1000–1500、1500–2000、2000–2500、2500–3000、3000–3491（合并文件含页面清单、字段字典、流程图、低保真、§24 与 2026-08-28 补录）。
- 独立性声明：本报告落盘前未读取 modules/M014-b1-grok-raw.md 本次追加段（V0.04）正文。可读旧 B1/B2/B4/B5（V0.01–V0.03）与 findings 历史定稿（M014-001–020）。未派子代理、未用 MCP、未问操作员、未边审边修。
- 判据锁：criteria-lock.md V0.11，M014 映射 84 项（BEQR 21、BSM 12、GEN 18、PGM 33，与 M013 同集）；未扩判据，LOCK-AMEND-COUNT=1。三份 HTML 当前哈希：ENUM 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66、BSM eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66、PGM a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1。
- 技能如实记录：未经技能工具调用 grilling / verification-before-completion；按提示词 §6 收窄口径的结构自行执行（按意图执行）。

## 2. 第一性原理三问
1. 为什么存在：本文件是治理 HTML「基础管理」「权限规则」「合同准入」等条目到页面/字段/流程/低保真的产品细化载体（PGM-002/003/005–014、PGM-040–044、BEQR-150–159）；其「当前实现/代码核验」栏是给实现方读的事实陈述，须与源码、迁移一致。
2. 声称是否被实际验证：文档类不强制跑测试（runbook §5）。本席对文中实现声称逐条追到当前源码/迁移（类型 4，只读）：V111 终端 CHECK 十值+`*`、迁移/OpenAPI/Java 均无 dealer_portal/distributor_web（L869/L3242 一致）；AccessController L244 `GET /effective-permissions`、L250 `GET /users/{id}/effective-permissions`，AdminApp.tsx 引用该接口（L2755–2778 一致）；V028 建 contract_key_dates / compliance_expiry_items / compliance_reminders，compliance 包存在（L959/L998 一致）；V055 staff_levels 含 main_level/sub_level/level_rank（L2825 一致）；V057 supervisor_review_decisions.approval_level 与 CHECK 四值含 specific_role（L2826/L338 一致）；gate 包 ActionGateAspect 持 TerminalSwitchGuard/AdmissionGuard（L12 一致）；V184 字典八码（L1295 一致）；SupervisorReviewService L117–121「M087 起审批人身份由组织层级节点决定，rank 只作档位下限」、L56–59 specific_role 策略仍在、L351–372 R-003 角色闸（L2848 为 2026-06-19 带日期核验记录，见观察）。
3. 未经核实就当真的前提：①「职级变化不触发重算」——L1979 与 PGM L658「定完系统自动适配」、L669「职级影响默认权限模板」相反，且 AccessService:1305–1313 改职级走 assignUserPositionDirect 重算（M013-019 已在四文档消除，本文件同根句未随）；②「所有对象继续携带 project_id」——business_entities / business_entity_bindings 无该列（V427 用 business_entity_projects 关联表；M013-021 已在验收包消除，本文件 L764/L2717 同根句未随）。

## 3. 顺从式与对抗式全审
顺从式：页面清单 §2–§20、字段字典 §1–§19、流程图 §1–§17、低保真 §1–§23、§24、补录段逐节对照 84 项：终端十一端（L122/L648/L869/L2085/L2638）、七档开关与兜底关闭（§12A.6）、商户六态与缓冲期（§14.2/§5.5）、合同八态与审批主线+平行线（§9.4/§14.3/§5.4）、员工五态、设备七态、权限五态、事件七态、七类红线与权限不足不设放行通道（§9.6.5/§7.4）、脱敏七项封闭清单（§2.3）、最终权限五源与拒绝优先（§18#7a/§24.1）、平台管理员≠最高权限（§24.1.2）、审批三档按组织汇报链（§8.1/§24.3）、逐单转发（L1017/L2831–2833）、宽限期（L763/L1304/L2219）、空间九类与归属根（§5/§6.3）、业态六值（§7）、私人手机低风险功能闭集（L2671）、两条注册路径（§8.1/L426）均与三 HTML 现文一致；M014-001–018 修复内容抽核未回归；废止框保持【已废止/不得回灌】。
对抗式（grilling 结构自问自答）：
- 「接入方按文档实现会不会做错？」→ L1979：HR 调级不触发重算，与 HTML 与现行代码相反（CX-M014-B1-01）。L764/L2717：验收集团视图时认定项目归属已全覆盖，主体/绑定表无该列（CX-M014-B1-02）。
- 「完成声明能否复现？」→ 除上述二处外，实现声称均可在源码/迁移复现（§2.2）。
- 「M013 收敛口径是否已同步到本文件？」→ 019/021 同根句未同步（两条新发现）；020（request_type 六类）、022（V427 关联）、023（status 四值）本文件零命中、无相反句，不需同步。
- 「凡写端点/入口的句子是否按对象核过 Controller？」→ §24.1 两端点已 grep AccessController 映射；§9.6/§24.3 只写落库表与服务名，未写不存在端点；L121 类「不存在的端点」句本文件无。
- 「是否把观察升级成缺陷或重开已批口径？」→ 否。M014-019（XM→M261）、020（§24.3 代码核验观察）保持；本席对 L2848 只记观察（见 §5）。
- 「同一文档内是否自相矛盾？」→ L1979 与同文 §11.5 异常分支表「保存岗位、职责或…变更 → 立即重算」并列不矛盾（后者未提职级）；L764/L2717 与 §18#8/§21#8「保证所有对象带综合体项目归属」（要求句）时态相反（一个是要求、一个写成继续携带的事实）。

## 4. 本轮新发现（阻断）
### CX-M014-B1-01 §11.4 强制补充规则第 4 条把「仅职级变化、责任范围变化」排除出重算，与 PGM L658/L669 及现行代码相反
- 位置：docs/platform-governance-product-spec-v0.2.md:1979 末句「本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件。」（本句无 DEC-R8-91 括注）。
- HTML 原文（类型 1）：PGM L658「HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板」；L669「职级影响默认权限模板和审批层级，但不能替代功能权限点。」
- 可复现证据（类型 4）：AccessService.java:1305–1313 已定岗员工改职级走 assignUserPositionDirect 按新映射重算（M006-G-02）；docs/prd-audit/business-decision-ledger.md:1936 DEC-R8-91 原文无「责任范围」；M013-019（2026-09-19 B4-r4/B5-r5 双 PASS）已在集团 PRD:76、PRD:407/523/544/656 删同一排除。
- 正常触发：HR 给在岗员工调级，实现方或测试按本文件认定不应重算。
- 分类：冲突（与 PGM L658/L669 相反；先于细化）。判据：PGM-041、PGM-042、BEQR-151、GEN-016、GEN-017。工程缺陷：成立。
- 最小处置：:1979 末句改为「本规则不把任意手工模板应用列为触发条件。」；同根句在本文件仅此一处（「仅职级变化」「责任范围变化」全文各 1 次）。版本 V0.06→V0.07 并在文首 2026-09-11 修订说明之后加一行本次说明、四张原文件变更日志各加一行（沿 B3-r1 先例）。不改 HTML、代码、决策台账。
### CX-M014-B1-02 §18 后续细化 11 与 §21 后续细化 12「所有对象继续携带 `project_id` 并按项目归属聚合」把要求写成已完成事实
- 位置：:764「集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归属聚合。`analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD V1.01 §4），不反向改写业务口径。」；:2717 同句。同文对照：:748/:2700「未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属」为要求句；:2992 补录行已标「历史摘录」。
- HTML 原文（类型 1）：PGM L412–413「当前先按一个综合体落地，但所有基础对象都要带“综合体项目归属”」——要求，非完成陈述。
- 可复现证据（类型 4）：business_entities（V005+V354+V410）、business_entity_bindings（V002+V010）、roles、users、permission_templates 均无 project_id 列；V427 以 business_entity_projects 关联表承载主体项目归属（绑定设计 V0.13 §3.1）。M013-021 已在验收包 G7 删同一句。
- 正常触发：按本文件验收集团视图的人会认定项目归属已全覆盖。
- 分类：细化（PGM-017 项目归属的现状陈述）。判据：PGM-017、PGM-004、BEQR-152、GEN-016。工程缺陷：成立。
- 最小处置（二选一交 B2）：A. 两处删去「；所有对象继续携带 `project_id` 并按项目归属聚合」——同 021 先例，要求句已在 :748/:2700 与 PRD §12 第 15 条；B. 两处改为「；所有对象须带综合体项目归属并按该归属聚合（要求，见 PRD §12 第 15 条）」。本席建议 A。随 CX-M014-B1-01 同一次版本变更。

## 5. 非阻断观察（只登记）
- O-M014-B1-01：:2848「代码核验（2026-06-19）：现网仍按职级算术与 `specific_role` 实现」为带日期核验记录；当前 SupervisorReviewService:117–121 自 M087 起审批人身份由组织层级节点决定、rank 只作档位下限留痕，:56–59 specific_role 策略仍在并按 R-003「指定最终审批人承担角色闸」解释。实现缺口归 governance/review 实现模块（沿 M014-020 观察），本文件按当时记录保留、不改。
- O-M014-B1-02：:1990 异常分支「职级与敏感权限不匹配 | 阻止保存或要求高级审批」——PGM L669 只写职级影响默认模板与审批层级，未写「职级不匹配即阻止」；历史双 PASS 未立案，本席不据此立案，交 B2 知悉。
- 020（§24.3）、019（XM→M261 merchant-space-contract-model）保持登记。

## 6. 84 项判据矩阵
|判据|本模块承接与证据|结论|
|---|---|---|
|BEQR-063|§24.2 敏感字段、§17 敏感权限留痕，无沽清相反陈述|PASS|
|BEQR-074|无承接，无冲突陈述|PASS|
|BEQR-143|字段字典 §7 主体绑定关系编号、§8 合同引用同一绑定|PASS|
|BEQR-144|名词四层脚注权威回到三 HTML+§0A（006 修复保持）|PASS|
|BEQR-145|无相反陈述|PASS|
|BEQR-146|无相反陈述|PASS|
|BEQR-147|无相反陈述|PASS|
|BEQR-148|合作模式六值（§7/§8/§17#4）|PASS|
|BEQR-149|职级 M1–M10 上中下、只绑薪资大范围（§18#5/§17#4/§8.1）|PASS|
|BEQR-150|十一端逐字一致（L122/L648/L869/L2085/L2638）；dealer_portal 标未实现与 V111 一致|PASS|
|BEQR-151|最终权限五源拒绝优先（§18#7a/§24.1）；:1979 职级排除相反|FAIL（CX-M014-B1-01）|
|BEQR-152|项目隔离不提供关闭开关（§14.3/§11.3）；:764/:2717 完成声明相反|FAIL（CX-M014-B1-02）|
|BEQR-153|七档开关、兜底关闭、留痕例外（§12A.6）|PASS|
|BEQR-154|项目六态、废止门控（§5.5/§14.1）|PASS|
|BEQR-155|商户六态、缓冲期 30–60/默认 30、客人三选一（§14.2/§5.5）|PASS|
|BEQR-156|合同八态、审批主线+平行线（§14.3/§5.4）|PASS|
|BEQR-157|员工五态（§14.7）|PASS|
|BEQR-158|设备七态（§14.4）|PASS|
|BEQR-159|权限五态（§14.9）|PASS|
|BEQR-166|合同审批链（§9.4/§5.4/§14.3 待确认）|PASS|
|BEQR-189|无承接|PASS|
|BSM-004|存档删除只在留痕页作废/冲正、密钥清理（§15.5）；无相反|PASS|
|BSM-005|无相反|PASS|
|BSM-006|无承接|PASS|
|BSM-007|§24.1.2 平台管理员/最高权限两角色|PASS|
|BSM-009|无相反|PASS|
|BSM-028|七类红线（§9.6.5/§7.4）|PASS|
|BSM-029|无相反|PASS|
|BSM-072|业态六值+media 码（§6/§7）|PASS|
|BSM-074|商户隔离一致|PASS|
|BSM-114|私人手机低风险功能含查看考勤（L2671）|PASS|
|BSM-115|§13 跨流程依赖|PASS|
|BSM-120|集团视图已承接（时态）；:764/:2717 完成声明见 CX-02|PASS（观察归 CX-02）|
|GEN-001|文件用途明确；:764/:2717 与 :748/:2700 时态相反|FAIL（CX-M014-B1-02）|
|GEN-002|端点/表/服务声称逐条核实一致|PASS|
|GEN-003|文档类不强制测试|PASS|
|GEN-004|校验口径与代码无相反|PASS|
|GEN-005|授权陈述一致|PASS|
|GEN-006|表/CHECK 陈述与 V028/V055/V057/V111/V184 一致|PASS|
|GEN-007|无相反|PASS|
|GEN-008|枚举陈述与 CHECK 一致（十端/四档/八码）|PASS|
|GEN-009|无相反|PASS|
|GEN-010|脱敏七项、密钥导出（§2.3）|PASS|
|GEN-011|留痕七态、作废为动作|PASS|
|GEN-012|无资源边界陈述|PASS|
|GEN-013|无构建内容|PASS|
|GEN-014|迁移号（V028/V055/V057/V068/V111/V184/V394）实查存在|PASS|
|GEN-015|无界面实现|PASS|
|GEN-016|两处当前事实/规则句不可复现|FAIL（CX-M014-B1-01/02）|
|GEN-017|:1979 与 HTML 相反被写成规则|FAIL（CX-M014-B1-01）|
|GEN-018|结论落到 HTML 原文或命令输出|PASS|
|PGM-002|治理顺序（§7.4 先阻断）|PASS|
|PGM-003|模块入口分组（§2/§3.1）|PASS|
|PGM-004|集团视图承接时态；:764/:2717 见 CX-02|FAIL（CX-M014-B1-02）|
|PGM-005|九类空间、归属根（§5/§6.3）|PASS|
|PGM-006|商户/主体字段（§7）|PASS|
|PGM-007|准入先卡后放行、权限不足不设放行通道|PASS|
|PGM-008|私人手机/工作手机、设备凭证（§9.1/§8）|PASS|
|PGM-009|单一功能权限点（§18#6–7）|PASS|
|PGM-010|终端开关（§14A/§12A/§3.1）|PASS|
|PGM-011|设备台账（§10/§12–13）|PASS|
|PGM-012|商户侧边界|PASS|
|PGM-013|无相反|PASS|
|PGM-014|留痕复核（§15/§12）|PASS|
|PGM-015|无相反|PASS|
|PGM-016|无相反|PASS|
|PGM-017|「所有基础对象带项目归属」为要求；:764/:2717 写成已完成|FAIL（CX-M014-B1-02）|
|PGM-018|多对多、名词脚注|PASS|
|PGM-019|无相反|PASS|
|PGM-020|集团下钻由职位模板决定、数据范围无设置项（§14.3/§11.3）|PASS|
|PGM-021|无相反|PASS|
|PGM-026|主体绑定关系编号、变更留痕（§7/§8）|PASS|
|PGM-027|无相反|PASS|
|PGM-031|§0A 经销商行|PASS|
|PGM-035|无相反|PASS|
|PGM-040|单一功能授权|PASS|
|PGM-041|「HR 定层级/职位即自动适配」被 :1979 排除抵触|FAIL（CX-M014-B1-01）|
|PGM-042|「职级影响默认权限模板」被 :1979 排除抵触|FAIL（CX-M014-B1-01）|
|PGM-043|外包/审计人员无相反；脱敏一致|PASS|
|PGM-044|跨范围每次审批（§11.5）|PASS|
|PGM-048|无相反|PASS|
|PGM-060|无相反|PASS|
|PGM-065|§13 依赖表|PASS|
|PGM-066|已拍板项一致（集团视图时态见 CX-02）|PASS|

## 7. 结论
三线完成、3491 行全覆盖、84 项逐项核对。新发现 2 条阻断（CX-M014-B1-01 冲突、CX-M014-B1-02 细化），均为 M013 收敛口径在本文件的同根句，无定稿 ID；须经 B2 与 Grok 本次 B1 归并后分配新 ID（M014-021 起）。历史 001–020 保持。无添加项、无 HTML 申请、无 questions。

结论：FAIL。


## M014 重开B2 Claude席甲整批归并复核

版本：V0.05（本文件自身；V0.04 及以前 51926 bytes 前缀完整保留，本段追加；B2 不写独占锁行）

结论：PASS（本批 B2 归并候选；不是 B4/B5/模块 PASS）。
输入：findings.md V2.39 顶部「当前 M014 重开B2整批候选」段（全文件 SHA256 见 ledger V4.31）；两份 B1 完整报告：modules/M014-b1-self-raw.md V0.04 追加段（全文件 SHA256 0dbcb356eddf5e3f0d640226b71e73dc3135b9fb1ffb8a15cd4f05d5a3b40e83）；modules/M014-b1-grok-raw.md V0.04 追加段（全文件 SHA256 2c8d9ffc56ecda4499ec37517c38d777eecc33539538e32df24e91277733dfd7，适配器实际 EXIT_CODE=0）。本票封存前未读 Grok 本次 B2 正文。

### 差集：两席 B1 全部条目 → 候选归属
| 来源条目 | 候选归属 | 本席复核 |
|---|---|---|
| CX-M014-B1-01 + GR-M014-B1-01 | 新 M014-021，冲突，是 | 同根；Grok 并入 :1990，席甲原 O-M014-B1-02 改接受（L669「不能替代功能权限点」）。处置差异：整句删（Grok）vs 保留手工模板排除（席甲，依 DEC-R8-91 原文与 M013-019 同根一致），候选按席甲写、交 Grok 复核 |
| CX-M014-B1-02 + GR-M014-B1-02 | 新 M014-022，细化，是 | 同根；处置差异：删句 A（席甲）vs 改写 B（Grok）；候选按 A（同 M013-021 先例，不扩写） |
| GR-M014-B1-03 | 新 M014-023，细化，是 | 席甲 B1 漏审；B2 核实 :232/:2415 各 1 处、:927 已废止行、ENUM 六值名称；接受 |
| O-M014-B1-01、OBS-B1-01…14 | M014-020 续记，否 | 均为既有边界/观察/实现模块缺口，不立案 |
无漏并、无改写对方 finding。

### 分类、队列与最小边界
- 021 冲突/是：PGM L658/L669 明文，AccessService:1305–1313 现行重算；:1979 只删两项排除、保留手工模板（DEC-R8-91 原文与四文档同根处置一致）；:1990 改为不以职级替代功能权限点、敏感授出按五类先审批（PGM L660/L669），不发明新规则。
- 022 细化/是：删句最小；要求句仍在 :748/:2700；不写「每张表有 project_id」也不在页面清单立法关联表实现。
- 023 细化/是：删「编号」列，与 :927 废止行、ENUM 六值名称一致；不重开 009。
- 020 续记否：观察不进队列。
- 版本 V0.06→V0.07；文首说明一行；页面清单 §20 / 流程图 §17 / 低保真 §23 各加一行（字段字典本批未改，不加）。

### 执行一致性与添加判定
- 补丁脚本 /home/james/audit-tmp/m014_b3r2_patch.py check 在当前工作区输出 CHECK_OK 11 edits：11 处原句各恰好 1 次（:232/:2415 以其后一行区分）；候选逐句段由同一 EDITS 导出。
- 同根活句已 grep 收全：「仅职级变化」「责任范围变化」各 1 处（:1979）；「继续携带」3 处（:764、:2717 活句；:2992 历史摘录不改）；「业态名称、编号、说明」2 处。
- 本批无添加项、无 HTML 申请、无 questions、无锁外判据；001–020 不动。

下一步：Grok 本批 B2 报告交付后核其完整票；双 PASS 才定稿并进 B3（第 2 次），任一 FAIL 按其意见改候选后重复②。


---

## M014 B2-r3 Claude席甲整批归并复核

版本：V0.06（本文件自身；V0.05 及以前 55102 bytes 前缀完整保留，本段追加；B2 不写独占锁行）

结论：PASS（本批 B2-r3 归并候选；不是 B4/B5/模块 PASS）。
输入：findings.md V2.42 顶部「当前 M014 B2-r3整批候选」段（全文件 SHA256 80d1e142630654dbe804200b80d43ada0121f7c00f10dd62242da51d493208f2）；两份 B5-r2 完整报告：modules/M014-b5-r2-grok-raw.md V0.01（33526bytes SHA256 0213bb82f1e26aefbda852fd3bf84d043dc0b452476ebc9727bee9a39420fa2a，FAIL，适配器实际 EXIT_CODE=0）；modules/M014-b5-r2-self-raw.md V0.01（11970bytes SHA256 681c122bf8f67e7c2ced620611fc3fd9032242e03584995f21d458d43788105d，PASS）。本票封存前未读 Grok 本次 B2-r3 正文。

### 差集：B5-r2 两席全部条目 → 候选归属
| 来源条目 | 候选归属 | 本席复核 |
|---|---|---|
| GR-M014-B5r2-01 | 新 M014-024，细化，是 | 席甲 B5-r2 漏审；B2 核原文 :1036–1075、:1845–1856、:3126–3132、:3371 与 ENUM :232/:240/:242、治理 HTML :363：同文已废止岗位状态门控（:1040/:1061/:3371），:1058/:1072/:1851 仍作现行校验，三 HTML 无岗位启用闭集/可分配字段、职级无启用状态；按班次/F7 实现即回灌岗位状态机。接受，工程缺陷成立 |
| Grok B5-r2 观察（020 续记、:1852/:1853 重复行、BSM-114 锁内原文 vs 现行 HTML） | M014-020 续记，否 | 既有登记/观察，不立案、不 LOCK-AMEND |
| 席甲 B5-r2 观察 | M014-020 续记，否 | 同上 |
无漏并、无改写对方 finding。

### 范围、分类与最小边界
- 同根活句 grep 全文件「启用岗位|启用职级|不可分配员工|启用角色|岗位状态|岗位停用」命中 :1040/:1058/:1060/:1061/:1072/:1851/:3129/:3371：写入集合只有 :1058/:1072/:1851；:1060「应选择启用角色」权限状态含「已启用」（ENUM :242、BEQR-159）不改；:3129 属补录历史摘录载体不改；:1040/:1061/:3371 已是废止行。
- 分类细化（非冲突：HTML 无「启用岗位」明文可矛盾；上位 PGM-008 / BSM-028 / BEQR-157 指得出条目）；判据 GEN-001/008/016。
- 处置：:1058/:1072 按同文 :1040 同根写法「选择已有…。【已废止/不得回灌】「…启用…」」；:1851 整行标【已废止/不得回灌】并注岗位状态本规格不定义、停岗/离职按员工状态处理（:1850 已有员工停岗或离职行）。与 Grok B5-r2 建议方向（去掉「启用」/与 :1040 对齐/删除或改员工状态五值）同向，取同文既有废止写法以防回灌、不新造规则；不改 HTML/AccessService、不重开 021–023、不新写岗位状态字段。
- 版本 V0.07→V0.08；文首说明一行；字段字典 §19、流程图 §17 各加一行（页面清单 §20、低保真 §23 本批未改，不加）。

### 执行一致性与添加判定
- 补丁脚本 /home/james/audit-tmp/m014_b3r3_patch.py check 在当前工作区输出 CHECK_OK 7 edits：7 处原句各恰好 1 次；候选逐句段由同一 EDITS 导出。
- 本批无添加项、无 HTML 申请、无 questions、无锁外判据；001–023 不动。B3 累计 2 不清零，本次通过后为第 3 次（上限内，无需操作员批）。

下一步：Grok 本批 B2-r3 报告交付后核其完整票；双 PASS 才定稿并进 B3-r3，任一 FAIL 按其意见改候选后重复②。


---

## M014 B2-r4 Claude席甲整批归并复核（操作员获批追加一次修复）

版本：V0.07（本文件自身；V0.06 及以前 58526 bytes 前缀完整保留，本段追加；B2 不写独占锁行）

结论：PASS（本批 B2-r4 归并候选；不是 B4/B5/模块 PASS）。
输入：findings.md V2.46 顶部「当前 M014 B2-r4整批候选」段（全文件 SHA256 25527282d85b60fd7d02a1b1def0b53de8f35da6a7712fae565bcc2deebad21e）；两份 B5-r3 完整报告：modules/M014-b5-r3-grok-raw.md V0.01（33768bytes SHA256 682e21dbd16f4353451536b65ed8db79963ccc4a461fe7bd13981a66ac991082，FAIL，适配器实际 EXIT_CODE=0）；modules/M014-b5-r3-self-raw.md V0.01（18292bytes SHA256 e04ab20c925b5e05e2d4ed63781798b6f9bfa5acf531bf56f19649ea147b9146，FAIL）。操作员 2026-09-19 弹窗批准追加一次修复。本票封存前未读 Grok 本次 B2-r4 正文。

### 差集：B5-r3 两席全部条目 → 候选归属
| 来源条目 | 候选归属 | 本席复核 |
|---|---|---|
| CX-M014-B5r3-01（席甲：:1078/:2573） | 新 M014-025，细化，是 | 同根成立（补录 :2959/:3135/:3451 已废止；:501/:1077/:1211/:1265/:1844 不定义；三 HTML 零命中） |
| GR-M014-B5r3-01（Grok：+:496、:484/:2565） | 并入 025 | 席甲 B2 核实接受：:496「记录异常班次」把班次标为异常即是状态门控，与 :2573 同句根；:484/:2565「状态」列与 :1265 废止「班次状态作为页面核心字段」直接并立，r1 M014-011 已按同样理由去开关列表「状态」列。席甲 B5-r3 把 :496 当记录动作、未核 :484/:2565，属席甲漏审，接受 |
| 两席观察（020 续记、:1855/:1856 重复行、BSM-114 锁文 vs 现行、即将到期/待审核/草稿用语） | M014-020 续记，否 | 既有登记/观察，不立案、不 LOCK-AMEND |
无漏并、无改写对方 finding。

### 范围、分类与最小边界
- 写入集合五处 :484/:496/:1078/:2565/:2573；排除 :485/:2566/:2567/:2574（记录项与提示，不是门控）、:1836 mermaid（流程异常分支）、:1099/:2606 设备异常说明（BEQR-158 有明文）、补录废止句、变更日志引述。
- 分类细化（非冲突：三 HTML 对班次状态沉默；上位 BSM-028 / PGM-008 / BEQR-157 指得出条目）；判据 GEN-001/008/016。
- 处置全部取同文既有废止/去列写法：:213（关键操作）、:2414（关键交互）、:1077/:1063（字段行）、r1 M014-011（去未定义状态列）；不新写班次状态机、不改 HTML、不重开 024。:1078 取 Grok「整行随班次状态废止」支而非席甲原拟「改选填」——前者不改 P0 要求语义、与同文体例一致。
- 版本 V0.08→V0.09；文首说明一行；页面清单 §20、字段字典 §19、低保真 §23 各加一行（流程图 §17 本批未改，不加）。

### 执行一致性与添加判定
- 补丁脚本 /home/james/audit-tmp/m014_b3r4_patch.py check 在当前工作区输出 CHECK_OK 10 edits：10 处原句各恰好 1 次；候选逐句段由同一 EDITS 导出。
- 本批无添加项、无 HTML 申请、无 questions、无锁外判据；001–024 不动。B3 累计 3 不清零，本次通过后为第 4 次（获批）。

下一步：Grok 本批 B2-r4 报告交付后核其完整票；双 PASS 才定稿并进 B3-r4，任一 FAIL 按其意见改候选后重复②。
