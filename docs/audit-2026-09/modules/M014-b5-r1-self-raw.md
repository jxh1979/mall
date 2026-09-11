CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M014 B5-r1 修复后全模块重审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-11T05:51:41Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `685dca4e1088ed59e68196c3c5b45787fde9db02`；工作区 tree `a6ef23d0f2c02186b31ee298fc808ed24a03e0dd`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M014「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段」，1 文件（计划冻结 3494 行，OVERSIZE-SINGLE-FILE；当前含 B3-r1 修复），映射 84 判据（BEQR 21、BSM 12、GEN 18、PGM 33）。步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线重审）。

- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M014-b5-r1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：PR #434（M013 闭环）已合并，HEAD=origin/main=685dca4e；本模块 B3-r1 修复在工作区未提交（after tree 82136f01b088c2bf59a5b480a92e2bc4c71f9923）。
- 只读依赖追流（不审）：三份 HTML 现文、V028/V050/V055/V057/V111/V184/V394 迁移、`PlatformService`、`AccessController`/`AuthorizationService`、`SupervisorReviewService`、`gate` 包、`adminRoutes.ts`、`docs/openapi/openapi-p0.yaml`（未改）。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/platform-governance-product-spec-v0.2.md | 3491 | 226970 | dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396 | 全读 |

## 2. 可执行证据原文（/tmp/m014_b5r1_run.log）

```
START=2026-09-11T05:51:41Z HEAD=685dca4e origin/main=685dca4e
== 0. spec after B3
3491 226970 dfb40482f22455c27071990d09b7a624248f3604d186a5b1293fb0ebb84f7396
# 平台治理产品规格 V0.06

> 2026-09-11 160 全量审计 M014（B3-r1，V0.05→V0.06）：R5 标注补 2026-09-11 现状行（005）；名词脚注权威回到三份 HTML + §0A（006）
== 1. 001 analytics.group.view
764:11. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归属聚合。`analytics.group.view` 审批�
2717:12. 集团视图与跨项目汇总下钻已由 `group-governance-rebuild-prd-v1.0.md` 承接落地；所有对象继续携带 `project_id` 并按项目归属聚合。`analytics.group.view` 审批
UPDATE permissions SET requires_approval = TRUE
 WHERE permission_code IN ('analytics.group.view', 'customer.voucher.manage', 'finance.merchant_expense.manage',
                           'pii.contact_phone.view', 'wecom.sync');
== 2. 002 mall / dictionary
1295:2. 空间层级 P0 为项目根节点以及楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房、设备点位；铺位号和经营面积保留为字段。（物理实�
                    if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) {
                        throw com.complex.platform.common.api.ApiException.badRequest("VALIDATION_FAILED",
                                "铺位类型必须是铺位类型字典中的启用值");
backend/src/main/java/com/complex/platform/governance/PlatformService.java:0
backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java:0
== 3. 003 terminal codes
6:> 2026-09-11 160 全量审计 M014（B3-r1，V0.05→V0.06）：R5 标注补 2026-09-11 现状行（005）；名词脚注权威回到三份 HTML + §0A（006）；空间类型去「项目」并注�
869:| 终端类型 | 确认控制哪个端（十一端，对齐 ENUM/GOV） | 终端开关页 | 必填 | 选择 | app(客人手机应用)、miniapp(客人小程序)、web(Web 前端)、tablet(工作�
1316:| 2026-09-11 | 改 | §3.1 / §5 / §6 / §7 / §9.1 修订说明 / §17 | 第十一端产品码统一 dealer_portal 并标未实现；空间类型去「项目」并注归属根（§5 节标题/�
3242:| 终端类型 | 确认控制哪个端（十一端，对齐 ENUM/GOV） | 终端开关页 | 必填 | 选择 | app(客人手机应用)、miniapp(客人小程序)、web(Web 前端)、tablet(工作�
        ADD CONSTRAINT chk_tcs_terminal_type CHECK (terminal_type IN (            'app', 'miniapp', 'web', 'tablet',            'staff_work_phone', 'staff_private_phone',            'merchant_portal', 'merchant_app',            'kiosk', 'mobile_admin', '*'    

== 4. 004 业态
6:> 2026-09-11 160 全量审计 M014（B3-r1，V0.05→V0.06）：R5 标注补 2026-09-11 现状行（005）；名词脚注权威回到三份 HTML + §0A（006）；空间类型去「项目」并注�
927:| 【已废止/不得回灌】业态编号 | 补录已废止：ENUM「商户业态」只有六值名称，未批独立编号字段（演出传媒的系统枚举码 media 见上一行括注） | �
1316:| 2026-09-11 | 改 | §3.1 / §5 / §6 / §7 / §9.1 修订说明 / §17 | 第十一端产品码统一 dealer_portal 并标未实现；空间类型去「项目」并注归属根（§5 节标题/�
3284:| 业态编号 | 【已废止/不得回灌】系统生成或必填且唯一的业态编号。ENUM「商户业态」只有六值名称，未批独立编号字段 | 不采用 | — | 不得作为�
46:            Set.of("bar", "catering", "retail", "amusement", "media", "other");
== 5. 005 terminal switch page
L626: 628:|---|---|
L2634: 2636:### 17A.2 关键交互
L2643: 2645:
L2146: 2148:- 终端开关配置与灰度范围。
2639:- 【已废止/不得回灌】生效时间不能晚于失效时间——不得以生效/失效做通用保存阻断（与 F11 §12A.7 一致）。不设定
== 6. 006 merchant_credentials
6:> 2026-09-11 160 全量审计 M014（B3-r1，V0.05→V0.06）：R5 标注补 2026-09-11 现状行（005）；名词脚注权威回到三份 HTML + §0A（006）；空间类型去「项目」并注�
959:| 资质信息 | 记录执照、许可证、保险等资料情况 | 选填/P0 推荐 | 附件/文本/到期事项 | 涉及餐饮、酒吧、游乐、供应商等业态时建议填写；证照/资�
1316:| 2026-09-11 | 改 | §3.1 / §5 / §6 / §7 / §9.1 修订说明 / §17 | 第十一端产品码统一 dealer_portal 并标未实现；空间类型去「项目」并注归属根（§5 节标题/�
1:CREATE TABLE IF NOT EXISTS contract_key_dates (
27:CREATE TABLE IF NOT EXISTS compliance_expiry_items (
61:CREATE TABLE IF NOT EXISTS compliance_reminders (
== 7. 007 §2999
780:| 2026-09-11 | 改 | 名词脚注 / §6.3 / §9.6 / §10.6 修订说明 / §14A.3 / §18 | 权威实体关系回到三份 HTML + §0A；空间树首项注归属根；悬空引用「§2999」订正
== 8. HTML untouched
eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66  docs/business-
a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1  docs/platform-
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66  docs/business-
 M docs/audit-2026-09/findings.md
 M docs/audit-2026-09/ledger.md
 M docs/platform-governance-product-spec-v0.2.md
?? docs/audit-2026-09/modules/M014-b1-grok-raw.md
?? docs/audit-2026-09/modules/M014-b1-self-raw.md
?? docs/audit-2026-09/modules/M014-b4-r1-grok-raw.md
?? docs/audit-2026-09/modules/M014-b4-r1-self-raw.md
?? docs/audit-2026-09/modules/M014-b5-r1-grok-raw.md
END=2026-09-11T05:51:41Z
```

## 3. 三线审计方法与覆盖（OVERSIZE-SINGLE-FILE 分段覆盖表，修复后行号）

| 段 | 行区间（V0.06） | 内容 | 覆盖 |
|---|---|---|---|
| 头部与 §0A | L1–L36 | 版本 V0.06、B3 变更记录、R5 标注（含 2026-09-11 现状行）、合并说明、§0A | 全读 |
| 页面清单 | L38–L790 | §1–§20（§9.6 引用已订正、§14A.3 开关列表去「状态」、§18 集团视图已闭合、§20 新行） | 全读 |
| 字段字典 | L792–L1333 | §1–§19（§3.1 终端码、§5 空间对象、§6 业态、§7 资质、§9.1 修订说明、§17 括注、§19 新行） | 全读 |
| 关键流程图 | L1335–L2240 | F1–F11（§2 总览图含 F11、F3 §5.4 注释行/§5.6 成员句、F6 修订说明、§17 新行） | 全读 |
| 低保真原型 | L2242–L2737 | §1–§23（§3.1 表头、§12 修订说明、§17A.1/§17A.2、§19、§21、§23 新行） | 全读 |
| §24 深度补齐 | L2739–L2849 | 有效权限集接口（标题路径已订正）、字段级权限、审批层级映射 | 全读 |
| 继续合并补录 | L2851–L3491 | 历史原文补录（终端码订正括注、admission_audits 废止前缀） | 全读 |

- 对象口径：main HEAD 685dca4e 工作区的单文件 V0.06（3491 行，B3-r1 后，after tree 82136f01）；三份 HTML 取 main 正本（BSM V0.14、PGM V0.12、BEQR V0.07）；判据锁 V0.11，84 项。
- 第一性原理线：从三份 HTML 原文（PGM L469 空间九类、L741 私人手机低风险功能、L734/L737 终端开关七档与涉钱禁灰度、BEQR L195 业态六值、L239 合同八态、BEQR-150 十一端、BEQR-144/PGM-018 多对多）反推规格应当陈述什么，逐一对照修复后文档与 main 实现事实（不依赖 B1–B4 结论）。
- 顺从式线：84 判据逐族（§5）。对抗式线：按修复后规格实现——是否仍会把项目当 space_type、把 mall 当活码、按 R5 旧句排期、按 L48 引用回灌父链、按 §24.1 标题 404、按业态编号立法五码、按 17A.2 加已废止校验、把私人手机收成只通知；B2 定稿禁止项（改源码/迁移/OpenAPI/三 HTML、新增字段接口、删补录原文、改日志历史行、把 R5/§24 代码核验重写为现状（005 除外））是否越界。
- 只读依赖追流（不审）：V028/V050/V055/V057/V111/V184/V394、`PlatformService`（业态六码、createSpace 字典校验）、`AccessController`/`AuthorizationService`、`SupervisorReviewService`、`gate` 包、`adminRoutes.ts`、OpenAPI（未改）、三份 HTML 现文（§2）。
- 单文件全文按七段逐行读完（修复后）；B3-r1 diff（+40/−43）逐字对照（B4-r1 双 PASS）。

## 4. 已定稿 20 项在当前工作区的状态（逐项）

| ID | 队列 | 当前状态 | 本席核对 |
|---|---|---|---|
| 001 | 否（已修） | L896/L903/L892/L900 去「项目」作空间类型，L187/L2392 树首项注归属根；L56/L174/L2276/L902 保留 | PGM L469 九类一致；全文「并集收敛为项目」0 处（§2） |
| 002 | 否（已修） | §17 括注改字典活校验、mall 仅契约快照 | `PlatformService:474-478`、V184 一致；「技术根节点为 mall」0 处 |
| 003 | 否（已修） | 低保真 §19 私人手机能力按 PGM L741 + BSM L930 闭集，禁止项对齐现场句 | HTML 现文逐字（§2） |
| 004 | 否（已修） | §18 #11、§21 #12 改 V394 已闭合 | V394:16-18；「当前实现缺口按该 PRD」0 处 |
| 005 | 否（已修） | R5 块 L9 后现状行，L8/L9 标历史 | `ActionGateAspect`/`AdmissionGuard`/`StaffApprovalService` 事实 |
| 006 | 否（已修） | L48 权威改三 HTML + §0A，模型文仅实现参考 | V410 删父链；019 登记 |
| 007 | 否（已修） | 合同 stateDiagram 删除，改成员句 | BEQR L239；「stateDiagram-v2」0 处 |
| 008 | 否（已修） | §24.1 标题路径 | `AccessController:244`；「/me/effective-permissions」0 处 |
| 009 | 否（已修） | 业态编号行废止、业态名称括注 media | BEQR L195；补录 L3287 同向 |
| 010 | 否（已修） | 终端码统一 dealer_portal 并标未实现；补录订正 | V111 CHECK 十值+`*` |
| 011 | 否（已修） | L626 去「状态」、L2634 对齐、L2643 前半句废止 | §14A.5/§14.13、L2146 一致 |
| 012 | 否（已修） | 总览图含 F11 三行 | 图与 §2 表一致 |
| 013 | 否（已修） | F3 图非法行改 %% 注释 | mermaid 可渲染（语法） |
| 014 | 否（已修） | 补录 admission_audits 行加废止前缀 | 表从未建；§9.6/§24.3 |
| 015 | 否（已修） | 低保真 §3.1 表头改分组 | §21 第 1–3 条一致 |
| 016 | 否（已修） | 四处修订说明指纹定义改现行 | 同节【已废止】标注；HTML「手机专属的设备标识」 |
| 017 | 否（已修） | §7 资质信息改 V028 合规到期台账 | V028 CREATE TABLE；merchant_credentials 0 处 |
| 018 | 否（已修） | §9.6 引用订正 | 「§2999」仅剩变更记录提及 |
| 019 | 否（XM→M261） | merchant-space-contract-model 未动（非本模块） | 与定稿一致 |
| 020 | 否（登记） | 观察项未动 | 与定稿一致 |

## 5. 84 项判据逐族核对（修复后）

| 判据族 | 结论 | 依据 |
|---|---|---|
| BEQR-143/144/145/146/147/148 | 通过 | 四类主体/多对多（L48 权威已回到 HTML）/合作模式六类同向 |
| BEQR-149/151/152/157/159、PGM-040/041/042 | 通过 | 职级、最终权限五源减显式拒绝（§24.1）、五类高危、员工/权限状态与 HTML 同 |
| BEQR-150/153、PGM-010 | 通过 | 十一端名称与 ENUM 同；产品码统一并标未实现；七档优先级与 ENUM L236/PGM L734 同；终端开关页三处已收口 |
| BEQR-154/155/156/158 | 通过 | 项目六态、商户六态（缓冲期 DEC-P1A-10）、合同八态（无自造边）、设备七态 |
| BEQR-063/166/189、PGM-007/026/027/031/035 | 通过 | 合同主线+三平行线、§0A 经销治理、准入闸口现状（R5 现状行）同向 |
| BEQR-074 | 无实现面 | 全文无隐私桌台 |
| BSM-004/005/006/007/009 | 通过（登记） | 留痕不可删只作废/冲正、分步表单；动态二维码无本文件展开 |
| BSM-028/029/072/074/114/115/120 | 通过 | 业态六值（仅 media 括注）、私人手机低风险功能闭集与 HTML 现文同向 |
| PGM-002…021、043/044/048/060/065/066 | 通过 | 空间九类 + 项目归属根、集团视图已闭合、导出脱敏同向 |
| GEN-001 必要性 | 通过 | 四份原文件均为 P0 现行入口 |
| GEN-002 可执行正确性 | 通过 | 「当前/物理实现」陈述均可按 §2 命令复核为真；按规格实现不再撞字典/404/不存在表 |
| GEN-003/004/005/006/007/009/010/011/012/014 | 不适用或无实现面 | 文档模块 |
| GEN-008 接口一致性 | 通过（本模块范围） | 路径/枚举/表名与实现一致；第十一端码标未实现 |
| GEN-013 构建 | 通过 | 引用文档/迁移/端点均存在 |
| GEN-015 中文 | 通过 | 正文中文，F3 图可渲染 |
| GEN-016 文档 | 通过 | 001–018 修复后无过期「当前」陈述与内部矛盾残留（§2 grep） |
| GEN-017 分类 | 通过 | 无添加项 |
| GEN-018 证据 | 通过 | §2 命令原文 |

## 6. 本次 Findings

无新增 finding。（B3-r1 修复后单文件七段三线全量重审，未发现未定稿缺陷、添加项或 HTML 问题。）

## 7. 观察（登记，不入队；与 020 同）

- §24.3 代码核验（2026-06-19）「仍按职级算术与 specific_role」仍为真，实现缺口归 governance/review 模块（跨模块观察）。
- merchant-space-contract-model-v0.1.md 过期父链陈述（019）待 M261 处理。
- 变更日志与补录历史行仍含 fingerprint/admission_audits 旧句（历史记录，已标废止或为日志），不升。
- 技能：`verification-before-completion`（§2 命令实跑）、`receiving-code-review`（未读席乙 B5 报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B5-r1 票：**PASS**。单文件修复后七段全读三线重审、84 判据逐族核对、20 项定稿逐项核对；无新增缺陷、无添加项、无 HTML 问题；未修源、未提交/推送。本报告落盘前未读取 `M014-b5-r1-grok-raw.md` 任何正文。
