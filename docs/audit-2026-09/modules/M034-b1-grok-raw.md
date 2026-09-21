CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M034 B1 席乙（Grok）修复前全量审计

- 版本：V0.01（本路径此前不存在，本次创建）
- 席位：乙 / Grok 4.6 xhigh / 独立只读
- 模块：M034「功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移／第1段」
- 阶段：B1（PENDING→IN_PROGRESS 由调度席写入 ledger；本席不改 ledger/findings/plan）
- 计划：`plan/modules.md` V0.04；判据锁：`plan/criteria-lock.md` V0.11
- 映射：M034 → GEN-001 … GEN-018（18 项；无 HTML 派生 ID）
- 本席未读 `modules/M034-b1-self-raw.md`，未用 MCP / 子代理 / 第三席
- 结论：**FAIL**（1 条工程缺陷成立：GR-01）

## 0. 技能预检（环境实用，按意图执行）

本席运行环境实核（`ls` 技能 `SKILL.md`，缺则记缺口，不冒充已调用）：

| 技能 | 存在 | 本轮用法（§6 收窄） |
|---|---|---|
| superpowers `using-superpowers` | 是 | 开工技能自检 |
| superpowers `verification-before-completion` | 是 | 任何 PASS/FAIL 前用本轮命令输出，不靠印象 |
| superpowers `systematic-debugging` | 是 | 只借读错误→复现→追数据流；不加诊断插桩、不改代码 |
| mattpocock `grilling` / `grill-me` | 是 | 对抗式自问自答；不等操作员、不派子代理 |
| mattpocock `diagnosing-bugs` | 是 | 只读取证环（psql SELECT / git / sha256）；不写 harness |
| mattpocock `codebase-design` | 是 | 权限种子 vs 运行时过滤的 seam 判断（不改架构） |
| ponytail-review | 是 | 只找可删/可收窄；不扫全仓 |
| graft | 是 | 读 SKILL；对 Java 消费链跑了 1 次 `graft ask`；SQL/HTML/迁移全文以 ripgrep + 通读 + psql 为准（graft 图不以迁移为节点） |
| `batch-grill-me` / `ubiquitous-language` | **缺口** | 本环境无对应 SKILL 目录 |
| `git-guardrails-claude-code` | 是（可选） | 本轮无 git 写操作，未启用 |
| Arc `audit` 机械扫描/子代理评审 | 存在但 **不用** | 与「禁止第三席 / 禁止子代理」冲突 |
| `research` / `dispatching-parallel-agents` | 划掉 | 未调用 |

## 1. 环境事实（本席实算，非抄期待值）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
sidecar plan/criteria-lock.sha256 同值
ledger 顶部 CRITERIA-LOCK-SHA256 同值
LOCK-AMEND-COUNT: 1（只读，未改）
```

```
git rev-parse HEAD        → e456ba96c84185c8d39f18f548011daac48a6466
git rev-parse origin/main → 4f6ef48bfc8d0b761f16172027d667630f955a0e
```

- 主工作区 HEAD 落后 origin/main，工作区有未推提交与暂存改动（与派发所述一致）。
- 本模块 13 个文件：`git show origin/main:<path> | sha256sum` 与工作区 `sha256sum` **逐文件相同**；`git ls-files -z -- <13 paths> | xargs -0 cat | wc -l` = **690**。
- Flyway（complex_test，只 SELECT）：本模块 13 版均 `success=t`（库内 version 为 `003`/`092`/… 非去零）。库内最新 `flyway_schema_history` 为 **428**（`m021 settlement format share media`）。派发写「含未发布 V427 属预期」；现值已到 428，记事实，不升格为本模块缺陷。
- 口令：只从 `/etc/complex/complex.env` 的 `COMPLEX_DATABASE_PASSWORD` 行读取，未 source、未回显；`psql -h 127.0.0.1 -U complex -d complex_test` 仅 SELECT。
- 已应用迁移不可改：本席不提议回写这 13 个文件；缺陷只允许前向迁移或登记。
- 未读 findings 定稿以外的相邻模块 raw 正文作发现来源；ledger M027/M031/M032/M033/D-029 只作「勿无新证据重复立项」边界。

## 2. 范围覆盖（13/13 全文，不抽样）

| 文件 | 行 | SHA256 | 通读 |
|---|---:|---|---|
| V003__p0_access_permission_authz_slice.sql | 181 | fb1b158cfe1d15ec3396396105f25941ceba8a56b7a04cb7e7c800a4e4a1b638 | 是 |
| V092__amusement_operational_config_permissions.sql | 23 | eabcc470c5cfb3e6d4aa59023bae90753afb381e9550181bdeb8bcfdf543203b | 是 |
| V158__refund_authority_admin_permissions.sql | 23 | 4a5d2728b52f2513e32f37e019f278c2c657ff1f300db9344de30080189b15a2 | 是 |
| V169__access_role_assignment_closure.sql | 53 | a25316489af69c5a5318b743f1220e5deca75adee2e7d8c887b0ddd68d3a5ea7 | 是 |
| V180__auditor_roles_readonly_seed.sql | 268 | 8af881c9af09eb3ef23fdd8dce36d404997d299d0c2c8cbb3f918d20e374c551 | 是 |
| V204__seed_runtime_checked_permissions.sql | 16 | 55e98c86950bcef5b8e608b7af60c2bf8a0e17659d43b285715ae72a314edcf5 | 是 |
| V261__platform_admin_full_permissions.sql | 9 | 5a6a982a2e0d7c8c9d61e53938552273bfc083e30839fcf603d54c7614f938dc | 是 |
| V270__ticket_manual_override_permission.sql | 16 | 45927d048b1caa9c64116a1bfe9ee99ec1633b3dbb832f50fee8904ddc4a1fcb | 是 |
| V321__staff_self_profile_permission.sql | 12 | 49b40908456a0fac3ef23759ad08e6631f542ec35f7a9d471a336abc2621f1e1 | 是 |
| V322__staff_self_service_role.sql | 32 | 3bd069d3e69773c9d00041cb8ec5deb6db667c7df6e7899f49964e18196cb8fe | 是 |
| V387__access_display_names_chinese.sql | 31 | 7f89a0e39aaa4a161400f351e1ceae5e227358783e713d49c61c77ffd2eab511 | 是 |
| V393__sensitive_business_data_permissions_require_approval.sql | 8 | 40aeb2b412ee4a07ed968be6582b4c7cbfbad0658bfcc55abecb789ba25e445f | 是 |
| V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql | 18 | acdeef24cb2e653981919b47eb0421e96a713f9fd7ce7cb14a450538ad7ceba7 | 是 |

合计 690 行。只读追证（非本模块 FILES）：三 HTML 权限/职级/日常/外包审计/员工自助/枚举九；`AuthorizationService.java` 日常桥与 `isExplicitOrDailyGrant`；`JdbcAccessRepository.findPermissionEffects` / `listPermissions`；`TicketingController.java:498-507`；后续迁移 V181/V187/V385/V388/V410（终态叠加）。

## 3. 三 HTML 本面原文（业务最终解释权；迁移不是依据）

**治理地图 `docs/platform-governance-map-for-operations-v0.1.html`**

- L287 界面全中文：展示给人看的文字必须全部为中文；例外闭集仅单据号、设备编号、二维码内容、系统内部标识符。L532 正文自用「AI」。
- L643–660「权限规则」：单一功能最小颗粒；角色级模板；员工级自定义；最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝（拒绝优先）；临时授权可有效期或单次；高危五类授出前须审批——资金/核销/退款/调账/分红发放类、提权或跨商户跨项目、敏感经营数据的查看/导出/修改、高危业务（沽清/反结/存酒作废）、税费规则与税费调整；内部结算价/企微完整原文/导出个人信息与结算价格不因「平台管理员」身份自动获得；未归类先按高危。
- L664–669「职级规则」：M1–M10，每级上/中/下；职级只绑薪资大范围，影响默认模板和审批层级，不能替代功能权限点。
- L673–677「外包与审计」：外包只能处理被指派工单/区域/任务，不看平台全局、不跨出本综合体；内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定范围与时间段；两类都只能查看、不能修改；查看或导出敏感信息（资金流水、人脸与身份等个人信息、完整操作记录导出）须先审批并留痕。

**业务地图 `docs/business-system-map-for-operations-v0.1.html`**

- L587–588「平台管理员拥有全部日常功能权限」：与最高权限是两个角色；日常功能自动同步；资金操作（退款、调账、分红发放、税费调整）、敏感数据查看与导出、删除、最终业务审批不因该身份自动获得。
- L1428 员工自助：审批通过的员工自动获得「员工自助」——本人（且仅本人）工资条（含提成明细）、基础资料、待办通知与排班/考勤/请假。

**枚举九 `docs/business-enum-quick-reference-v0.1.html`**

- L232 职级：M1–M10，每级上/中/下（共 30 档）。
- L234 最终权限计算：角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝（拒绝优先）。
- L235 项目与敏感数据权限。
- L242 权限状态：草稿、已启用、暂停、已停用、归档。
- L43 核销记录状态：**「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过。」**

## 4. 库内终态（后续迁移叠加后；陈旧 SQL 不当现状）

`psql` 实算（complex_test）：

- `permissions`：active 376 / inactive 2 / retired 3；`risk_level` 仅 low/medium/high/critical；`chk_permissions_risk_level` 存在。
- 风险×审批：critical/t 16；high/t 192；high/f 10；medium/t 1（`bar.combo.view`）；medium/f 129；low/f 33。
- V393/V394 点名码终态与脚本一致：`bar.combo.view` medium+TRUE；`bar.combo.manage` / `inventory.cost.view` / `voucher.internal_price.view` high+TRUE；`access.grant_request.apply` low+FALSE 且 `role_staff_self` 持有；`analytics.group.view` / `customer.voucher.manage` / `finance.merchant_expense.manage` / `pii.contact_phone.view` / `wecom.sync` high+TRUE。
- high+FALSE 现存 10 条（指标异常、审核快照锁定、邀请、客户账户生命周期、视频审批、供货主体、跨主体调拨、预警升级/转派、批准删除服务目录）——与 V394 注释「不在五类命名范围内，登记不改」同向（注释写 11，现值 10，见观察）。
- `role_platform_admin` 持有 **全部 376 条 active**（0 缺失）。Java `AuthorizationService.dailyPermissions` 只把 active ∧ ¬requires_approval ∧ (low|medium) 当日常桥；`isExplicitOrDailyGrant` 把来源为 `role_platform_admin` 的高危条目剔出，治理本职 9 码除外。此过滤在已闭环 M031，本席不重复立项。
- 角色显示名 10/10 中文、无拉丁。模板名 5/5 中文。`permission_name` 无纯非汉字行；含拉丁字母仅 2 条且含「AI」（治理地图 L532 自身用词）。
- 审计：`role_auditor_internal` 81 条，全部 `\.(view|read)$`，无写动词。`role_auditor_external` **0** 条 `role_permissions`（V181 删除直授，改 TemporaryGrant；非 V180 终态）。模板 `tpl_auditor_internal` 81 grant / `tpl_auditor_external` 51 grant，皆只读。
- 外包三角色 `role_permissions` 为 0（同 V181）；`role_outsourced_vendor_admin` 2 条。不在本模块 FILES 播种范围内。
- `role_staff_self` 8 条：V322 的 6 码 + V394 的 `access.grant_request.apply` + 后续迁移的 `staff.device.bind`。
- `staff_levels` 30 行，`level_rank` 1–30（表由 V055 建，V169/V180 FK 引用；本模块未改档位数）。
- `verification.manual_override`：**active / high / requires_approval=t / 名「人工放行票务核销」**；`role_permissions` 仅 `role_platform_admin`；模板项与用户覆盖 0 行。后续迁移 **无** 再写该码。

后续叠加（不得把 V180/V003 原文当现状）：

- V181：清空外审与外包三角色的 `role_permissions`。
- V187：去掉审计角色的 `audit.operation_log.export`。
- V385：权限展示名中文化（本模块 V387 只补角色/模板名与 5 条含 MDM 的权限名）。
- V388：另 4 个资金/导出码 `requires_approval=TRUE`。
- V410：`roles_role_name_key` 已 DROP，现唯一为 `uq_roles_org_role_name (COALESCE(organization_id,''), role_name)`。

## 5. 逐文件审计要点

### V003（181）
建 `permissions`/`access_users`/`roles`/`role_permissions`/`user_roles`/`permission_template_items`（effect CHECK grant/deny）/`permission_template_applications`/`usage_logs`/`user_permission_overrides`；给 `permission_templates`/`temporary_permission_grants` 加列；种子 14 个 access/ops 码（ON CONFLICT DO UPDATE）；`role_platform_admin` 并授当时全部 active。`CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS` / `ON CONFLICT` 对 Flyway 首次应用幂等。DO UPDATE 若被手工重放会盖掉后来的中文名——Flyway 不重放已应用版本，不按理论重放立项。risk CHECK 由 V394 补，终态有。`roles.role_name` 原 UNIQUE 已被 V410 替换，终态不冲突。

### V092 / V158 / V169 / V204
种子 + 授 `role_platform_admin`，ON CONFLICT 幂等。V092/V158 高危写码 high+TRUE，与「未归类先高危 / 退款配置」同向。V169 建岗位模板（active 部分唯一索引 `uq_position_role_templates_active_scope` 终态仍在）与 `access.role.assign` high+TRUE（提权）。V204 三码 ON CONFLICT DO NOTHING；终态 `contract.lifecycle.activate`/`bar.entitlement.redeem` high+TRUE，`bar.entitlement.read` low+FALSE，与注释兄弟码口径一致（展示名后经 V385 中文化）。

### V180（268）
内/外审角色+模板+岗位映射；只读正则 `\.(view|read)$` 且排除 manage/export/…。外审排除 `analytics.group.view`。INSERT/DELETE/ON CONFLICT 对当时字典是可重放的「收敛到只读闭集」。**终态**外审角色直授已被 V181 清空，不得把 V180 INSERT 当现状。内审 81 条仍为只读 view/read。模板 `valid_to` 为空；外审时间盒依赖 TemporaryGrant（V180 注释 + V181），属后继迁移，不在此重复立项。

### V261（9）
`INSERT … SELECT` 全部 active 给 `role_platform_admin`，ON CONFLICT DO NOTHING。注释写「平台管理员=全部权限」。SQL 与注释一致；与 HTML「仅日常功能自动同步」不一致的是**角色行上的 376 码**。运行时日常桥+`isExplicitOrDailyGrant` 已在 M031 实现 HTML 口径。首次 `requirePermission` 对管理员高危码不会因该角色直授而放行。不重复立项（见观察 O-1）。

### V270（16）→ **GR-01**
见第 8 节。无后续迁移退役该码。

### V321 / V322
`staff.profile.self` low+FALSE；`role_staff_self` 授工资条/排班/打卡/请假/通知/本人档案，对应该 HTML 员工自助清单。V394 再授 `access.grant_request.apply`，对应「员工可申请临时权限」。`data_scope='self'` 注释写明 AuthorizationService 不读该列、自锁靠 actor.id()——注释与实现指向一致（实现在 M031，不在此重复）。

### V387（31）
角色 10 条、模板 5 条按旧英文名 UPDATE（重放因 WHERE 旧名而不再命中，安全）；5 条权限名无 WHERE 旧名，写的是中文。终态显示名满足 GEN-015/零英文例外。`template_code`/`id` 仍为拉丁机读标识，属四样例外。

### V393 / V394
把敏感经营数据码打上须审批；`normal`→`low`；申请码降为 low 并授员工自助；CHECK 四档与 `AccessGrantPolicy` 的 `low/medium/high/critical` 键集一致。ADD CONSTRAINT 无 IF NOT EXISTS——Flyway 不重放；终态约束已在。UPDATE 无 WHERE 已是目标值时仍为同值赋值，安全。

## 6. 顺从式：18 项逐项

| ID | 适用? | 证据 | 满足? |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 权限字典/角色/模板/职级映射是 L643–669、L587、L1428 的数据承载。V270 种子的「人工放行」码无 HTML 上位且与 L43 相反 → GR-01 | **不满足**（GR-01） |
| GEN-002 可执行正确性 | 适用 | 13 版均已在 complex_test 成功应用；点名码终态与 V393/V394/V321/V322 一致；V270 声称的权限码在库内为 active 且端点仍按该码鉴权 | 种子可执行；V270 声称与 HTML 不一致（GR-01） |
| GEN-003 测试有效性 | **无本面** | FILES 无测试。不把别模块测试绿当本模块证据 | 无本面 |
| GEN-004 信任边界 | 部分 | 本模块是种子/DDL，无用户输入面。CHECK/UNIQUE/FK 见 GEN-006/008 | 无独立输入校验面 |
| GEN-005 授权与最小权限 | 适用 | 日常桥只认 low/medium 且 ¬requires_approval；高危五类点名码已 TRUE。V270 把 HTML 禁止的核销人工放行做成可勾选、可审批授出的权限点 | **不满足**（GR-01） |
| GEN-006 数据完整性 | 适用 | PK/FK/ON CONFLICT/部分唯一索引终态存在；effect CHECK grant/deny 与 Java `Effect.GRANT/DENY` 一致；risk CHECK 四档与 Java 政策键一致 | 满足（终态） |
| GEN-007 并发与幂等 | 适用 | 种子 INSERT 均 ON CONFLICT；V180 DELETE+INSERT 收敛只读集；V387 条件 UPDATE。已应用文件不被 Flyway 重放 | 满足（应用语义） |
| GEN-008 接口与模式一致性 | 适用 | risk 四档 Java/库一致；effect 闭集一致；权限状态库内 active/inactive/retired（无 CHECK），与枚举九中文五态是机读标识 vs 展示，不在本模块做中文闭集。`usage_mode` 默认 merge 与 M033 已登记的服务识别差不在此 13 文件，不重复 | 本面模式一致；V270 码与 HTML 禁止项不一致见 GR-01 |
| GEN-009 错误处理 | **无本面** | 纯 DDL/DML 种子，无运行时错误分支 | 无本面 |
| GEN-010 安全与敏感 | 适用 | 不引入注入（静态 SQL）；敏感码 V393/V394 须审批；审计角色无 export。V270 提供禁止的放行能力 | **不满足**（GR-01） |
| GEN-011 可观测 | 部分 | V003 建 usage_logs / 临时授权留痕列，本身不写业务日志。无矛盾声称需本模块单独证伪 | 部分；无缺陷 |
| GEN-012 资源边界 | **无本面** | 无循环/批处理/连接放大 | 无本面 |
| GEN-013 构建配置 | **无本面** | 非构建/CI | 无本面 |
| GEN-014 迁移正确性 | 适用 | 顺序已应用；破坏性（V410 改 roles 唯一）不在本模块。发现问题只允许前向迁移。V270 已应用且仍 active | 顺序正确；内容缺陷 GR-01（前向退役，不回写 V270） |
| GEN-015 界面与语言 | 适用 | `permission_name`/`role_name`/`template_name` 上屏（V385 注释：模板页无翻译层）。终态无缺汉字名；拉丁仅 HTML 自用「AI」 | 满足 |
| GEN-016 文档可验证 | 适用 | V270 注释以权限矩阵为「权威」且称人工放行高风险须审批；三 HTML 才是权威，且 L43 禁止该动作。V261 注释「全部权限」与 HTML 日常口径不同，由运行时过滤消化（O-1） | V270 注释/SQL 与 HTML 不可对上（GR-01） |
| GEN-017 四分类 | 适用 | 本批按冲突→细化→技术性→添加。GR-01 冲突。无添加项，不写 questions | 分类已走 |
| GEN-018 第一性证据 | 适用 | 结论落到 HTML 原文、psql 输出、Java 源码行，不用「历史审过/注释说」 | 本报告 |

## 7. 第一性原理三问（整模块）

1. **为何必须存在？** 删掉这 13 个迁移，治理地图「单一功能权限点 / 角色模板 / 最终权限公式 / 职级三十档 / 外包与审计只读 / 高危授出审批」和业务地图「平台管理员日常权限 / 员工自助」在库内没有字典、角色、模板和职级 FK 可承载。答得上 → 有业务上位。V270 删掉后，L43「系统不提供人工放行」反而成立，故 V270 **不是**必须存在。
2. **声称是否被执行验证？** Flyway success=t + 终态 SELECT 验证种子效果。V270 声称「人工放行」权限存在：库内 active，且 `TicketingController.java:498-502` `POST /ticketing/verifications/manual-override` 仍 `requirePermission("verification.manual_override")`。HTML 声称「不提供」未被实现。
3. **未验证前提？** （a）不得把 V180 当时 INSERT 当外审现状（V181 已清空）。（b）不得把 `role_platform_admin` 的 376 行当有效权限（Java 过滤，M031）。（c）Flyway 不重放，故「手工重放会盖中文名 / ADD CONSTRAINT 会失败」不是首次正常路径。

## 8. 对抗式（grilling 结构，自问自答）

❓ **Q1** — V270 是否首次正常可复现，还是理论？
➡️ 权限字典 `listPermissions` 只出 active，该码在列；模板页可勾选；`requires_approval=TRUE` 走高危授出审批；审批通过后 `findPermissionEffects` 可含该码（非 `role_platform_admin` 来源时 `isExplicitOrDailyGrant` 为真）；端点仍按该码放行。这是配置权限的正常路径，不是要两三件不巧才成立。**立项。**

❓ **Q2** — 管理员角色持有该码，会不会已经自动放行？
➡️ 否。来源 ROLE + `role_platform_admin` + 非日常 + 非治理 9 码 → 过滤。当前 `role_permissions` 仅管理员持有、模板/覆盖为 0，故**此刻**没有人因默认种子而有效持有。缺陷是「系统提供了可授出的禁止能力」，不是「默认管理员已经能放行」。仍冲突。

❓ **Q3** — 是否与 M031/M032/D-029 重复？
➡️ findings 检索 `verification.manual_override`/`V270` 无本轮台账 ID。D-029 是未发布工作流，与这 13 个已发布迁移无关。**新证据，不重复。**

❓ **Q4** — 内审持有 `analytics.group.view`（集团跨项目）是否超出「本综合体」？
➡️ HTML 对内审写「可查看本综合体全部数据」，「可」是下限不是「只能」。禁止跨项目的句子针对「普通岗位」。不把「可」反推成上限。**不立项**（O-3）。

❓ **Q5** — 内审 `finance.ar_ap.view` 是否就是须审批的「资金流水」？
➡️ 展示名为「查看应收应付台账项」，不是「资金流水」。HTML 点名资金流水/人脸原文/完整操作记录**导出**。内审无 export。不拉伸立项（O-3）。

❓ **Q6** — V261 授全部 active 是否冲突「日常才自动同步」？
➡️ 角色行确实 376=全部 active。有效集由已闭环 M031 过滤。首次鉴权路径不因该种子自动得到退款/敏感/删除。无新的可观察越权。**不立项**（O-1）。

❓ **Q7** — V394 留下的 10 条 high+FALSE 是否漏了五类？
➡️ 与 V394 注释排除的删除/最终审批/预警/调拨/供货/邀请/客户账户同向。「跨主体调拨」是否算「跨商户跨项目」：五类里的「提权或跨商户跨项目」读的是授出范围，不是业务单据是否跨主体。不把业务名词套进授出五类。**不立项**（O-2）。

❓ **Q8** — 外审 0 条 role_permissions 是否 V180 失败？
➡️ V181 有意删除。终态靠模板+TemporaryGrant。把 V180 INSERT 当现状是陈旧事实。**不立项**（O-4）。

❓ **Q9** — 显示名拉丁？
➡️ 仅「AI」，治理地图 L532 自用。**不立项。**

❓ **Q10** — 有无添加项（业务发明）？
➡️ 权限码/角色/模板均能指到 HTML 上位（或与 HTML 冲突）。无「指不出上位」的发明。**添加项 0。**

## 9. Findings

### GR-01 — `verification.manual_override` 种子与枚举九「系统不提供人工放行」冲突

- **文件:行：** `backend/src/main/resources/db/migration/V270__ticket_manual_override_permission.sql:8-16`
- **原文：**
```
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('verification.manual_override', '人工放行票务核销', 'ticketing', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'verification.manual_override')
ON CONFLICT (role_id, permission_code) DO NOTHING;
```
  更长上下文：同文件 L1–7 注释称 C1/REQ-05 人工放行、以 `p0-operation-permission-matrix-v0.1.md` 为权威（高风险、须审批）。该矩阵不是三 HTML。
- **正常触发 / 影响：** 权限字典列出「人工放行票务核销」。运营在模板/角色里勾选该码，走高危授出审批通过后，调用 `POST /api/v1/ticketing/verifications/manual-override`（`TicketingController.java:498-502` 先 `requirePermission("verification.manual_override")` 再 `manualOverrideVerification`）即可让不满足规则的核销以人工方式通过。与枚举 L43「任何岗位、任何权限都不能」相反。
- **终态命令：**
```
SELECT permission_code, permission_name, risk_level, requires_approval, status
FROM permissions WHERE permission_code='verification.manual_override';
-- verification.manual_override | 人工放行票务核销 | high | t | active
SELECT role_id FROM role_permissions WHERE permission_code='verification.manual_override';
-- role_platform_admin
```
  后续 `V*.sql` 无再写该码。
- **四分类（顺序）：** **冲突**（与 BEQR「核销记录状态」明文矛盾）。能指到 HTML 上位且相反 → 停在冲突，不进细化/技术性/添加。
- **工程缺陷：** **是**
- **冻结 ID：** GEN-001、GEN-005、GEN-010、GEN-014、GEN-017、GEN-018（本模块仅映射 general；HTML 作 2.3 类型 1 证据，不新增锁外判据）
- **依据类型：** 1 HTML 原文（枚举 L43）+ 4 psql 输出 + 可执行入口源码行（Controller:498-502）
- **最小拟处置（不得回写已应用的 V270）：** 新前向迁移：将该码 `status` 置 `retired`（或等价停用），`DELETE` 其 `role_permissions` / `permission_template_items` / 用户覆盖。仅改 status 不够：`findPermissionEffects` 不 join `permissions.status`，残留 role_permissions 仍会授出。端点删除/410 属票务模块，登记跨模块，**不改本模块 13 文件、不改 HTML**。
- **修复队列建议（B2 定稿）：** 是（冲突→删除该能力）

## 10. 观察（不立项）

- **O-1** V261 把全部 active 授给 `role_platform_admin`（终态 376/376）。HTML 只要日常自动同步。运行时过滤在 M031，无新越权证据。
- **O-2** high∧¬approval 现存 10 条，与 V394「五类外不改」同向；注释写 11、现值 10。
- **O-3** 内审 81 只读含 `analytics.group.view`、`finance.ar_ap.view`、`wecom.view`（脱敏）。不把「可查看本综合体」写成「不得看集团」；资金流水/导出未直授。
- **O-4** 外审/外包三角色 `role_permissions` 为空是 V181 终态，不是 V180 失败。
- **O-5** `role_staff_self` 第 8 码 `staff.device.bind` 来自后续迁移，非 V322。
- **O-6** `roles.description` 仍有英文；治理页用 `roleName` 上屏，描述非本轮用户可见面。
- **O-7** `permission.status` 无 CHECK，取值 active/inactive/retired；与枚举九中文五态是机读 vs 展示。Java `listPermissions` 只读 active。
- **O-8** V003 `ON CONFLICT DO UPDATE`、V394 `ADD CONSTRAINT` 无 IF NOT EXISTS：仅手工重放有意义，Flyway 不重放。
- **O-9** V180 内审 DELETE 段声明了未使用的 `external_project_permissions` CTE。死 SQL，不影响结果。
- **O-10** complex_test Flyway 最高 428（派发写到 V427）。记录，不升格。
- **O-11** M027 同类已应用迁移模块的「观察只登记、不改已应用文件」口径适用于本模块其余幂等/注释漂移项。

## 11. 相邻台账（不重复立项）

- M027：已应用迁移不可改、终态以 SELECT+后续迁移为准。
- M031/M032：日常桥、`isExplicitOrDailyGrant`、授权政策四档、有效权限公式实现。本席只追证消费，不重开其 ID。
- M033：DTO/契约 mode 与 `usage_mode` 默认 merge 的差，XM 至 M392/M031。
- D-029：未发布工作流。这 13 个文件与 origin/main 逐字节相同，不适用。

## 12. 添加项 / HTML 申请

- 添加项：0（不写 questions，不停 B2 闸）
- 三 HTML 自身：L43 与实现冲突，问题在种子/端点，不申请改 HTML

## 13. 步骤结论

**FAIL**

理由：13/13 全文覆盖、18 项逐项、三线完成、哈希独立实算；存在未定稿工程缺陷 GR-01（冲突，缺陷是）。无未答 questions。不存在有条件通过。

B2 应把 GR-01 纳入归并；已应用的 V270 只允许前向迁移或登记，禁止回写。

## M034 B2-r1 整批归并复核（席乙票，V0.02）

- 时间：2026-09-21T08:22:29Z。席位：乙 / Grok 4.6 xhigh / 只读外审，正本 `/home/james/complex-management` main 主工作区。
- 输入：`modules/M034-b1-grok-raw.md` V0.01（27617 bytes，SHA256 `41a77fb7292ae28cd0785a2031b590ab7cc5d79f610a3d29a19401e3083ff496`，本席 B1：FAIL，GR-01，观察 O-1…O-11）；`modules/M034-b1-self-raw.md` V0.01 封存前缀（16438 bytes，SHA256 `575cd2a9c3d188e953a052b4e34339e91a5986495f3a933671ac8055381031c7`，席甲 B1：PASS，OBS-1…5）。席甲 raw 现文件更长（17889 bytes，全文 SHA `1202bf0301a54e378bf87615cab5f538073617386ee0e2d4b55e2913d666d55c`），本票只读该 16438 字节前缀，未通读、未采信其后追加的 B2 结论段。
- 候选：`docs/audit-2026-09/findings.md` V3.05 顶部「当前 M034 B2-r1 整批候选」段（全文件 SHA256 `02291884d9341baf26dbb91c28eb97209e4aa048b63c282b2057159e34ba785d`）。B1 后四值仍全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席本步只读复核，不写新的独占 CRITERIA-LOCK-SHA256 行）。
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句（complex_test 仅 SELECT）；未用 MCP 或子代理。
- 本席 B2-r1 票：**PASS**（整批候选同意）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M034-001 | 本席 GR-01（席甲 B1 漏：只核种子风险档/审批位/终态，未拿「人工放行」能力本身对 HTML） | 冲突 / 是 / 是 | **同意入本模块队列**；席甲 B1 PASS 撤回后合取 FAIL，与候选一致 |
| XM-M034-A | GR-01 端点/服务/契约/前端/测试下线 | →票务后台实现与接口定义所在模块（落地文件分属 M132 `TicketingController`、M133 `TicketingService`、M134 `TicketingControllerTests` 及契约/前端模块） | **同意登记**；不在本模块 13 文件改 |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **同意；无漏并、无应升队项** |

冲突入队 1（001）。细化入队 0。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 1。本模块写入集合＝新增 `backend/src/main/resources/db/migration/V429__retire_verification_manual_override.sql` ＋ `AccessJdbcIntegrationTests.java`（M035，PENDING）；已应用的 13 个迁移不动。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、席甲补的旁证（矩阵 L5 / L63）。票：加强冲突定性，不改分类、不加判据。**

独立核源（不重审模块）：

1. `docs/p0-operation-permission-matrix-v0.1.md:5` 文首口径修订（2026-08-06 R10）逐字引用枚举「核销记录状态」：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」；并写「本文原有的『人工放行』取值、端点、按钮、指标、权限点与复核类型一律废止」「已实现的端点、权限点与状态取值待代码轮下线，期间不得新增调用、不得新写入」。
2. 同文件 `:63` 矩阵行：「人工放行（**已废止·见文首口径**）」／`verification.manual_override`（停用，待代码轮下线）／说明「该动作不再提供，任何岗位、任何权限都不得放行不满足规则的核销」。
3. 本席 B1 已核：V270 L1–7 注释把该矩阵当作「权威」，种入 active 高危须审批码。矩阵不是三 HTML，不能替代枚举 L43 作类型 1；但它是 V270 自己引用的项目文档，且早已按 HTML 口径标废。字典 `permissions.status='active'`（complex_test 实查 1 行）与文档「停用，待代码轮下线」并存，说明冲突不是「HTML 一句 vs 一处种子」的孤证，而是项目文档层已经收口、权限字典从未跟上。
4. 「待代码轮下线」与本批切分同向：本模块只退役字典与授予行；端点删除留 XM-M034-A。不把矩阵升成锁外判据，不申请改三 HTML。

**明确同意**：旁证加强冲突定性；分类仍停在冲突（能指到 HTML 上位且相反）；不进细化/技术性/添加。

**二、最小处置 V429。票：同意四张授予表同时清；无更小且同样完整的改法；无遗漏的有效授予来源。**

独立核源：

1. 版本号。`git ls-tree origin/main` 迁移最高 `V428__m021_settlement_format_share_media.sql`（无 V427）。主工作区磁盘最高同为 428；另有未发布 `V427__m009_project_profiles_and_associations.sql`（origin/main 无此路径；complex_test `flyway_schema_history` 已 success=t 应用到 428，含 427）。V429 不读 V427 对象，无依赖。下号 429 正确。
2. `AuthorizationService.permissionGrant` L72–84：先 `if (!"active".equals(actor.status()) || !knownPermission(code)) deny(code)`，其后才 `isMasterAuthority` / 日常桥 / 客人自助桥。`knownPermission` L257–259 调 `permissionExists`。`JdbcAccessRepository.permissionExists` L30–37 `WHERE permission_code = :permission_code AND status = 'active'`。该码 `retired` 后，任何主体（含最高权限）走 `requirePermission("verification.manual_override")` 均 `PERMISSION_DENIED`（`deny` L 抛 `ApiException.forbidden("PERMISSION_DENIED", …)`）。候选因果链成立。
3. `findPermissionEffects` L857–903 五段 SQL **均不联** `permissions.status`。有效授予来源闭集：① `user_roles`⋈`role_permissions`；② `user_position_assignments`⋈`role_permissions`（岗位派生，已覆盖）；③ `permission_template_applications`⋈`permission_template_items`（模板应用，已覆盖）；④ `temporary_permission_grants`（`status='active'`）；⑤ `user_permission_overrides`。另：`attachPermissionCodes` L325–329 读 `role_permissions` 同样不联 status——只改 status 则平台管理员角色详情仍列出该码。故「仅 retired」不够。
4. `listPermissions` L50–56 `WHERE status = 'active'`。模板页 `PermissionTemplatesPage` 经 `GET /api/v1/access/permissions` 取该清单。retired 后字典与模板勾选面不再出现该码。`AccessService.requireKnownPermissions` L2326 同样走 `permissionExists`，新授予/新写入角色或模板会 `UNKNOWN_PERMISSION`。
5. 库内终态（本票 SELECT）：该码 `active/high/requires_approval=t`；`role_permissions` 1 行（`role_platform_admin`）；`permission_template_items` / `user_permission_overrides` / `temporary_permission_grants` 皆 0（含 active）。`permissions.status` 现值 `active` 376 / `inactive` 2 / `retired` 3；CHECK 仅 `chk_permissions_risk_level`，**无** status CHECK。retired 先例：V290 三条 `customer.membership.*`（注释「permissions 只增不删，留痕」），现库仍为 retired。`temporary_permission_grants.status` 无 CHECK，置 `revoked` 合法。
6. 含 `permission_code` 的其它表：`audit_events`、`permission_template_usage_logs` 为留痕，禁止清；`access_grant_requests` 该码 0 行（`permission_code` 与 `permission_codes` 皆空库），不是 `findPermissionEffects` 来源，retired 后新申请会被 `requireKnownPermissions` 挡住，不必进本迁移；`permission_template_permissions` 全表 0 行，仅 V002 建表、V003 L84–87 一次性拷入 `permission_template_items`，Java 零引用，不是有效授予来源。
7. 更小改法对照：①只 `UPDATE status='retired'`——入口会 DENY，但角色页仍列出、effects 仍含残留行，`computeEffectivePermissions` / `holdsPermissionViaRole` 亦不联 status；非管理员来源一旦存在会进入有效集展示。不够。②只 retired＋`DELETE role_permissions`（V290 写法）——对 **complex_test 现值**够用（仅 1 行角色授予），但留下模板项/覆盖/临时授权三处 effects 源的确定性缺口；本席 B1 已要求清模板项与覆盖；候选把仍 active 的临时授权置 revoked（现 0 行，为确定性而写）补上第四源，与 `findPermissionEffects` 闭集对齐，不是扩大范围。③`DELETE FROM permissions`——违反「只增不删」先例，且 FK 指向该码。不是更小。

**明确同意**：V429 ①retired ②DELETE 三张行表 ③active 临时授权→revoked；已应用 V270 不改。无更小且同样完整的改法。无遗漏的有效授予来源。

**三、测试与 Flyway 升到 429。票：同意。**

1. `backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java` 工作区 SHA256 `ba1d17c374af1bb0693937b20a78b776f4c2db70c52a66c0a1196d69f163e282` ＝ `git show origin/main:…` 同值。属 M035（PENDING）。补一条真库断言「该码非 active 且四张授予表 0 行」钉住 V429 终态，层级正确（迁移模块用库态，不在本模块写 Controller 403）。
2. M021 先例：主工作区落前向迁移，测试启动 Flyway 把 complex_test 从当时最高版升到新号（M021-b4 写明升到 428）。本批同形，升到 429 可接受。complex_test 现最高 428，V427 已在该库（未发布、与 V429 无依赖），不构成否决。

**明确同意**。

**四、端点／服务／契约／前端／TicketingControllerTests 下线。票：同意登记 XM-M034-A，不在本模块改。**

1. 入口仍在：`TicketingController.java:498-504` `POST /ticketing/verifications/manual-override` 先 `requirePermission("verification.manual_override")` 再 `TicketingService.manualOverrideVerification`（`:936`）。`TicketingControllerTests` 两例（无权限 DENY／有权限走放行）属 M134。控制器属 M132，服务属 M133。契约与前端不在本 13 文件。
2. V429 之后该入口对任何主体 403，HTML「不能以人工方式通过」在鉴权层已成立；删除/410 路由、服务方法、契约字段、前端按钮与相关用例是票务面下线，本模块 B3 禁止改这些文件。候选 XM-M034-A 与本席 B1「端点删除/410 属票务模块，登记跨模块」同向。`docs/business-decision-log-v0.1.md:417` 2026-07-07 增项已被矩阵 R10 取代，随 XM 登记即可，不在本模块改决策日志。

**明确同意**。

**五、其余观察。票：全部只登记；无漏并须升队项。**

候选已覆盖：`role_platform_admin` 376 条 active、运行时 M006 过滤（O-1＝OBS-1）；high 且免审批 10 条、V394 注释写 11（O-2 / OBS-2）；V261「累计漏 25 条」现状不可复现（OBS-2）；「新权限须同步授平台管理员」无门禁、现状缺 0（OBS-3）；种子 `DO NOTHING`/`DO UPDATE` 并存（OBS-4）；外审与外包三角色 `role_permissions` 空是 V181（O-4＝OBS-5）；内审 81 只读含 `analytics.group.view` / `finance.ar_ap.view` / `wecom.view`（O-3）；`role_staff_self` 第 8 码后续迁移（O-5）；`roles.description` 英文非上屏（O-6）；`permissions.status` 无 CHECK（O-7，与 V429 用 retired 同向）；V180 未用 CTE（O-9）；V003/V394 仅手工重放才有幂等/IF NOT EXISTS 问题（O-8）。

本席 O-10（派发写到 V427、库已 428）与 O-11（M027「观察只登记」口径）是环境/流程事实，不进台账观察段，正确。不把其它模块的 `amusement.verification.override`（V014，非本 13 文件）拉进本批。无应升队项。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 `M034*`。不申请改枚举 L43。不 BLOCKED-ASK。001 是删除已禁止能力，不是发明新能力。

---

### 二、分类理由

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **001 冲突。** 枚举 L43「系统不提供人工放行…任何岗位、任何权限都不能」与 V270 种入可勾选、可审批授出、入口仍按该码鉴权的 `verification.manual_override` 不能同时成立。能指到 HTML 上位且相反 → 停在冲突。矩阵 L5/L63 是加强旁证，不是改判细化的理由。不是添加（不是「HTML 没说、实现发明」）。同意采本席 B1 分类。

工程缺陷「是」。队列「是」（本模块写入集合内存在「改完后首次正常使用可观察行为变对」的改法：字典不再列出、任何主体调该入口 PERMISSION_DENIED、四张授予表 0 行；且不触碰已闭环模块、不回写已应用 V270）。

冻结支撑：候选列 GEN-005 / GEN-017，HTML 原文为类型 1。本席 B1 另列 GEN-001 / GEN-010 / GEN-014 / GEN-018 仍适用、不扩锁、不作为否决条件。

---

### 三、最小修复边界

本模块写入集合＝新增 `V429__retire_verification_manual_override.sql` ＋ `AccessJdbcIntegrationTests.java`（M035，PENDING）一条真库断言。一次 B3 禁止改已应用的 13 个迁移、禁止改 `AuthorizationService` / `JdbcAccessRepository` / `TicketingController` / `TicketingService` / `TicketingControllerTests` / OpenAPI / 三 HTML / 锁。XM-M034-A 不在本模块 B3 执行。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | 新前向迁移 V429：该码 `permissions.status='retired'`；`DELETE` 该码在 `role_permissions`、`permission_template_items`、`user_permission_overrides` 的行；该码仍 `active` 的 `temporary_permission_grants` 置 `revoked`。测试类补「非 active 且四表 0 行」。若发布门禁因本迁移触发生成物检查，只机械重生成生成器拥有区域 | 回写 V270；`DELETE FROM permissions`；改鉴权 Java「补 join status」冒充本模块修复；在本模块删端点/契约/前端/Ticketing 测试；清 `audit_events` / 用量日志；动 V427 或其它未发布迁移 |

B3 实施非否决提示（不改变本票）：临时授权置 revoked 时写 `status='revoked'` 即满足 effects 过滤；`revoked_at`/`revoked_by` 有列可顺带填，不是入队条件。

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止开工 B3。一次 B3 只加 V429 并在 M035 测试文件补一条真库断言。

- 001：冲突／是／是。矩阵 L5/L63 加强定性、不改分类。V429 retired＋清四张授予表是相对 `findPermissionEffects` 闭集的最小完整改法；只改 status 不够（角色页与 effects 不联 status）。最高权限不能绕过 `knownPermission`。
- XM-M034-A 同意登记，本模块不改票务文件。
- 观察全部只登记，无升队。无添加、无 HTML 申请、无 questions。
- 测试升 429 按 M021 先例可接受。

### 结论

全批差集无原意改写。GR-01→001，分类采本席冲突，缺陷「是」，队列「是」。席甲 B1 PASS 因漏对 HTML 否定句而撤回，合取 FAIL，与候选一致。旁证加强冲突定性。四张授予表必须清，无遗漏的有效授予来源，无更小完整改法。本席 B2-r1 **PASS**。本票不是模块 PASS，不是授权在双 PASS 前开工 B3。步骤仍 B2；M034 IN_PROGRESS；B3=0。
