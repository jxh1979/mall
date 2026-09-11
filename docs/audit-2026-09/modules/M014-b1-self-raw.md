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
