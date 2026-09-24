CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r5 席乙（Grok）整模块三线全量重审 raw

- 报告版本：V0.01（本路径原先不存在，本次创建）
- 席位：审计席乙 Grok（grok-4.6 / xhigh）
- 步骤：B5-r5（操作员 2026-09-19 第二次批准追加一次修复后的整模块重审；B3 累计 5/5；不是 B4 diff 复审，不复用 B2/B4 票）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 正本：`/home/james/complex-management` `main` 工作区
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11
- 独立实算命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`；另用 Python `hashlib.sha256(path.read_bytes()).hexdigest()` 交叉，同值
- 独立实算结果：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（64 位小写 hex；未抄 sidecar/ledger/派发期望值。事后对照 sidecar 与 ledger 顶部同行，仅作交叉，不作为填写来源）
- 提示词：`160-audit-prompt.md` V0.17 B1/B5 及三线；runbook V0.02
- 互盲：未读 `M013-b5-r5-self-raw.md` 及其票；可读旧 B1/B4/B5；未派子代理/MCP/第三席；未问人；未改业务/HTML/锁/findings/ledger；未 fetch/commit/push/merge/deploy/写库；未建脚本/日志/临时产物（未再为验收新建临时索引）
- 证据纪律：只读。文档模块不强制跑测试。未冒称 Java/HTTP/数据库实跑。可执行声称用类型 1（三份 HTML 逐字）或类型 4（sha256sum / wc / git cat-file / git hash-object / git ls-tree / git diff --stat / rg / graft）
- 历史已批口径（独立复核后保留，不替代发现）：私人手机查看考勤、打卡仅工作手机；BSM-114 锁旧摘录与 BSM-120 页脚日期为 M009 已登记观察，不恢复旧句、不申请改 HTML、不新增锁；PRD 业态不再维护/配置引用开关，历史日志按当时文字解释，不把废止框当新规则；集团 PRD 第 5 行修订说明、PRD 第 966 行变更日志及各变更记录行引述被删词属历史/日志记录；公示 grant 三请求类型、V392 六类 CHECK 与服务内生成两类分清；绑定未来 inactive 不提前同步但完整审批仍缺；项目聚合 ≠ 项目结果隔离；同范围岗位普通立即/高危待批，普通角色授权不免审；summary 参数使用后台政策。001–024 当前全部「否」（21 已修 + 009/010/017 登记）≠ 功能全实现。017 项目过滤归 M348/依赖 M032；009 OpenAPI 旧 entity_role 与 023 所披露 status 四值归 M392；019 同根句在 M014/M421/M439 只作跨模块观察，不在本模块改。O-B5R4-01（`requireSubjectStatus` 提示语对主体调用方误导）归 `PlatformService` 所属后台模块观察，不在本模块发明入口。

---

## 0. 技能预检（§6，按本环境实核）

本席已读并按收窄口径使用：

| 技能 | 本环境 | 本步用法 |
|---|---|---|
| `graft` | 已读全文并多次调用 | 定位 Controller 映射、`BUSINESS_ENTITY_ROLES`、绑定错误码、grant 类型、lifecycle、`requirePlatformScope` |
| `using-superpowers` | 已读；用户指令优先 | 开工技能自检；B5 只读、不问人、不派第三席 |
| `using-arc` | 已读；`platform-tools.md` 本路径不存在 | 用户任务是 160 审计，不路由 Arc `audit`（其会派评审子代理并写 `docs/arc/audits/`，与双席/§7 冲突） |
| `grilling` | 已读全文 | 对抗式主武器；自问自答，事实自读，不派子代理、不问操作员 |
| `grill-me` | 已读（正文仅 “Run a /grilling session”） | 按意图转发 `grilling` |
| `verification-before-completion` | 已读全文 | PASS 前用本次命令核哈希/行数/after-tree blob/源码声称 |
| `codebase-design` | 已读前部 | 本模块接口是四份文档对 HTML 与当前实现的声称；实现在 M009–M012 等代码模块 |
| `diagnosing-bugs` | 已读前部 | 声称与源码不一致时按读代码→最小命令定位；本轮无运行时故障环，不造诊断插桩 |
| `systematic-debugging` | 已读前部 | 按意图：读声称→追 Controller/服务/迁移；不作无 ID 改码 |
| `ponytail-review` | 已读全文 | 对抗式找可删；废止框与历史日志不删 |
| `long-running-background-tasks` | 会话提醒适用 | 本步无长任务，不启动 monitor |
| `i-have-adhd` | 已核存在于 cache `0.1.0` | 本步产出是 raw 落盘而非对人对话；不按其「首行下一步」改写审计正文。编号与状态复述保留 |

已核存在、本步不适用（B5 只读、不修、不提交、不写收口）：`brainstorming`、`writing-plans`、`test-driven-development`、`tdd`、`requesting-code-review`（禁止第三席）、`receiving-code-review`（本席即外审，期间禁止修复）、`ponytail`（B3 修复强度）、`resolving-merge-conflicts`、`writing-for-agents`（收口文件仅双 PASS 后由席甲写）、`git-guardrails-claude-code`（可选，存在于 mattpocock `misc/`；无 Claude Code hooks，本步无 git 写）。

明确不用（提示词已划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`domain-modeling`、`code-review`（mattpocock）、`ponytail-audit/debt/gain`。

缺口（未冒充已调用）：
- `batch-grill-me`：mattpocock 技能树未找到该目录
- `ubiquitous-language`：本席本次技能根未核到可调用副本

graft 本席检索合计 tokens saved ≈ 359,864（21 次命中调用；migration / docs / openapi 路径 graft 未索引或不全，改 rg）。

---

## 1. 范围、哈希、after-tree（类型 4）

模块文件（`plan/modules.md` FILES 段，4 个，计划冻结有效行 1517 不改）：

| 路径 | wc -l | 独立 sha256sum | 工作区 `git hash-object` | after tree `2720a4e02b917ab6e88b5ba20d4912d59c007fae` blob |
|---|---|---|---|---|
| `docs/business-entity-binding-development-design-v0.1.md` | 154 | `b85d4d42a6bf7bfcdba197da90cf132ed07b8012080a311c3110f97506f25143` | `583106e516a7519e3eccfaf4789a56428a38ec63` | 同 |
| `docs/group-governance-rebuild-prd-v1.0.md` | 117 | `7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52` | `d252bdc30d89bc195f80cb8ed6eda835df60de84` | 同 |
| `docs/platform-governance-acceptance-pack-v0.2.md` | 291 | `56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0` | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` | 同 |
| `docs/platform-governance-prd-v0.1.md` | 971 | `e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c` | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` | 同 |
| 合计 | 1533 | — | — | 计划冻结 1517；差额为既有 B3 修文，不改计划数字 |

`git cat-file -t 2720a4e02b917ab6e88b5ba20d4912d59c007fae` → `tree`。`git diff --stat 2720a4e02b917ab6e88b5ba20d4912d59c007fae -- <四路径>` 为空。四文件与 after tree 相同。

全文阅读：绑定 1–154、集团 PRD 1–117、验收包 1–291、治理 PRD 1–971。未抽样。

映射：`criteria-lock.md:6392` M013 → 84 ID = BEQR 21 + BSM 12 + GEN 18 + PGM 33。定义区已读对应条目（含 HTML 原文块）。三份 HTML 相关锚点按现行文件核对，不只信锁内行号。

命令：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
wc -l / sha256sum / git hash-object <四文档>
git cat-file -t 2720a4e02b917ab6e88b5ba20d4912d59c007fae
git ls-tree 2720a4e02b917ab6e88b5ba20d4912d59c007fae -- <四文档>
git diff --stat 2720a4e02b917ab6e88b5ba20d4912d59c007fae -- <四文档>
```

sidecar `plan/criteria-lock.sha256` 与 ledger 顶部 `CRITERIA-LOCK-SHA256:` 均为同值。本席第一行仍只写独立实算，不把 sidecar 当填写来源。

---

## 2. 第一性原理线（每文件三问）

### 2.1 `docs/business-entity-binding-development-design-v0.1.md`（V0.13，1–154）

1. **为何必须存在？** 删掉则 BEQR-143「四类主体」唯一绑定、BEQR-144/PGM-018 多对多协作、PGM-026 四类绑定与主体权责、BSM-004 配置存档删除在本模块没有实现层说明书。答得上上位。
2. **声称是否验证？** 文档模块豁免类型 2。入口/表/状态/版本用类型 4：`PlatformController` `@RequestMapping("/api/v1/platform")` 下 GET/POST `/business-entities`、POST `/business-entities/{entity_id}/projects`、GET/POST `/business-entity-bindings`、POST `/business-entity-bindings/{id}/change` 均存在；无 PUT/DELETE/GET-by-id 主体 CRUD，无 `/active`、无 `/{id}/usages`。`BUSINESS_ENTITY_ROLES` 三值；`SUBJECT_STATUSES` 五值无 archived；V410 CHECK 三角色；V427 `business_entity_projects`；V354 `archive_reason` 列存在但 `BusinessEntityRecord` 不映射、列表 SQL 不选该列；创建绑定未来 `effective_from` 写 `inactive` 且仅生效时 `syncMerchantEntities`。类型 1：§1–§2 与 HTML 四类主体/多对多/存档删除同向；§3–§5 把未落地标为差距。
3. **未核实前提？** 不以服务注释/`requireSubjectStatus` 提示语代替入口。L49 现行原句是「当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`」，无「归档走存档端点」。L108 写「完整存档/删除流程未在当前接口落地」。L56「持授权 U 盘密钥的最高权限可指定已存在项目」对得上 `creationProject(..., true)`；L116 POST projects「须平台范围且由持授权 U 盘密钥的最高权限执行」对得上 `requireProjectConfigurationAuthority()`。

### 2.2 `docs/group-governance-rebuild-prd-v1.0.md`（V1.03，1–117）

1. **为何必须存在？** 删掉则 PGM-017/020/041 集团下钻、角色装配两支、五类高危授权在本模块没有实现层对标。答得上。
2. **声称是否验证？** `AccessController` `@RequestMapping("/api/v1/access")`：职位模板 GET/POST/PUT、DELETE、lifecycle；`PUT /users/{id}/position`；role-assignments 及 unassign；staff-levels GET 与 `PUT /users/{id}/staff-level`；grant-requests POST/GET/review/summary；temporary-grants POST 与 revoke。`AnalyticsController` `@GetMapping("/group-overview")` 校验 `analytics.group.view` + `requirePlatformScope()`。DELETE 进 `deletePositionRoleTemplate` 抛 409 `ARCHIVE_APPROVAL_REQUIRED`。lifecycle switch 五动作与文一致。`normalizeGrantRequestType` 只放行三类；V392 CHECK 六类；`user_scope_change` / `position_high_risk_release` 为服务常量。V394 将 `analytics.group.view` 置 `requires_approval=TRUE`，`access.grant_request.apply` 置 `low`。`grantSummary` 对阈值参数或不等的 `window_days` 抛 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。`requirePlatformScope` 拦商户与外包，不按项目过滤汇总——与 §0/§4 待对齐句一致。
3. **未核实前提？** 不把 `requirePlatformScope` 通过当成项目隔离完成；不把 DB 六类当成申请入口六类；不把 `role_permission` 写成开放入口。L5 修订说明④与 L117 变更记录引述被删词按历史/日志，不升为现行排除规则。现行 L76 角色装配句已无「仅职级变化/责任范围变化」排除，只留任意手工模板应用不触发，与 PGM L658/L669 同向。

### 2.3 `docs/platform-governance-acceptance-pack-v0.2.md`（V0.06，1–291）

1. **为何必须存在？** 删掉则治理层 P0 产品设计验收清单、§2A HTML 增量、状态闭集与 G7 集团缺口在本模块无载体。答得上。
2. **声称是否验证？** 本文自称产品设计验收、非接口/库验收（L18–L19）。§2A/§4–§7 与 PGM 七类红线、十一端、开关优先级、ENUM 六态/商户六态/合同八态一致。G7 现行句：审批属性已由 V394 闭合；项目级汇总过滤待对齐；已删「所有对象继续携带 project_id」。L201 变更日志引述被删短语属日志。补录区废止框未回灌为现行规则。
3. **未核实前提？** 不把「五件套有」写成编码完成；不把 G7 审批属性闭合写成项目隔离完成。

### 2.4 `docs/platform-governance-prd-v0.1.md`（V0.06，1–971）

1. **为何必须存在？** 删掉则 PGM 治理顺序、红线、四类主体、权限计算、终端开关、私人手机低风险功能、存档删除在本模块没有产品细化。答得上。
2. **声称是否验证？** 私人手机六项写「查看考勤」，与现行 PGM L741、BSM L930/L1865 一致；§12B.2 打卡仍从领取工作手机开始，与 BSM L1445 一致。§6.10.9 / §7.8.2 / §12 14B 现行句不再排除职级/责任范围，只保留任意手工模板应用不触发。§6.2.1 字典八码与 V184 种子一致；OpenAPI `SpaceRequest.space_type` 枚举为契约快照（含 mall/bar_table/facility，缺 building/public_area），文内已标明非活闭集。`createSpace` 校验 `listActiveSpaceTypeCodes()`。§12#8 员工域 `StaffApprovalService` 两处 `evaluate`（排班发布、处罚上限）存在；票务/酒吧/游乐/结算包内未检索到对等 `.evaluate(`，文内已写待对齐。§12#15 集团审批属性按 V394 闭合，项目级过滤待对齐。V349 `is_sales_position`、ADM-STF-004 路由与 `staff.profile.sales_position_changed` 提示存在。自营标注即 `self_operated`。§15C 存档删除与 BSM-004 同向。
3. **未核实前提？** 不把 `project_id` 治理要求写成主体表当前列（绑定设计已披露 V427 多对多、表无 `project_id` 列；验收包 G7 已删「所有对象继续携带 project_id」）。不把变更日志 L966/L971 当现行排除规则。不把废止框当新规则。

---

## 3. 顺从式：84 项逐条（适用 / 证据 / 本席缺陷）

证据类型：1 = HTML 逐字；4 = 可复现命令/源码只读。无类型 2（未跑 JVM/HTTP/DB）。不新增判据。

| ID | 适用 | 本模块证据（摘要） | 缺陷 |
|---|---|---|---|
| BEQR-063 | 是 | 验收包/PRD 七类红线与酒吧高危（沽清/反结）承接 PGM，不另造酒吧动作闭集 | 否 |
| BEQR-074 | 是 | PRD/验收包不把隐私桌台权限改写成可配置数据范围 | 否 |
| BEQR-143 | 是 | 绑定 §1 四类名称+唯一生效组合；HTML L226 逐字同向 | 否 |
| BEQR-144 | 是 | 绑定 L22、PRD §5、集团 §0：多对多，非固定行政链 | 否 |
| BEQR-145 | 是 | PRD L191–193 城市项目公司资产职责与排除清单 | 否 |
| BEQR-146 | 是 | PRD 娱乐平台公司段照抄 PGM；绑定牌照/股权分记 | 否 |
| BEQR-147 | 是 | PRD 收入归属/通票拆分承接；不在本模块发明新名目 | 否 |
| BEQR-148 | 是 | PRD §6.5.1 / 验收包合作模式六类+「其他」须指定收费模板 | 否 |
| BEQR-149 | 是 | PRD 职级 M1–M10 上中下；集团模板按 staff_level 绑定 | 否 |
| BEQR-150 | 是 | 验收包/PRD 十一端含独立经销商门户 | 否 |
| BEQR-151 | 是 | PRD §6.10 / §12#13 最终权限公式与 HTML 一致 | 否 |
| BEQR-152 | 是 | PRD/集团：默认项目隔离；敏感数据分权；跨项目仅获授权岗位 | 否 |
| BEQR-153 | 是 | 验收包/PRD 开关优先级、兜底关闭、留痕例外 | 否 |
| BEQR-154 | 是 | 验收包 §7 / PRD §10 综合体六态；废止状态门控 | 否 |
| BEQR-155 | 是 | 绑定 L58、PRD §7.4/§10 商户六态+试运营不记录+退出缓冲；服务五值标为未实现完整词表 | 否 |
| BEQR-156 | 是 | 验收包/PRD 合同八态+当地时间 | 否 |
| BEQR-157 | 是 | PRD/验收包员工五态 | 否 |
| BEQR-158 | 是 | PRD/验收包设备七态 | 否 |
| BEQR-159 | 是 | PRD/验收包权限五态 | 否 |
| BEQR-166 | 是 | PRD §6.5 / 验收包 G5：三类模板+主线/平行线+不得录入即生效 | 否 |
| BEQR-189 | 是 | 验收包边界：治理层不做酒吧点单细节；PRD 不把管理费方式改写成治理可配闭集 | 否 |
| BSM-004 | 是 | 绑定 §2.5 / PRD §15C 存档隐藏、删前占用、软删、USB 免审；绑定 L108 标接口未落地 | 否 |
| BSM-005 | 是 | 本模块无「一次填完 vs 分步」相反发明 | 否 |
| BSM-006 | 是 | 本模块不把固定核销码写成治理规则 | 否 |
| BSM-007 | 是 | PRD §7.8.1 平台管理员 vs 最高权限；高危不自动获得 | 否 |
| BSM-009 | 是 | 四文档覆盖综合体/空间/商户/合同/员工/设备/权限基础管理 | 否 |
| BSM-028 | 是 | 绑定/PRD/验收包承接多对多、合作六类、城市项目公司边界 | 否 |
| BSM-029 | 是 | 验收包/PRD 连接票务酒吧游乐财务权限 | 否 |
| BSM-072 | 是 | 验收包/PRD 商户业态六值单选；商户侧授权范围内开放 | 否 |
| BSM-074 | 是 | 绑定同源引用；未获价格权不得看内部结算价 | 否 |
| BSM-114 | 是 | PRD 私人手机查看考勤、现场动作在工作手机；入口表不恢复「考勤打卡」为私人手机功能。HTML 锁旧摘录/页脚日期归 M009 观察 | 否 |
| BSM-115 | 是 | 验收包/PRD 基础管理卡住后续流程 | 否 |
| BSM-120 | 是 | 已拍板集团视图/项目归属；页脚日期观察归 M009 | 否 |
| GEN-001 | 是 | 四文档均有 HTML 或实现证据用途；差距段不发明已完成能力 | 否 |
| GEN-002 | 是 | 文中端点均有对应 Controller 映射；不存在的端点已声明不存在 | 否 |
| GEN-003 | 不适用 | 本模块无测试文件；不把缺测试当本模块缺陷 | 否 |
| GEN-004 | 是 | 文档要求校验白名单/必填理由；不把未校验写成已校验 | 否 |
| GEN-005 | 是 | 查看/管理权限码、平台范围、USB 最高权限、grant 审批分离均有对应实现或待对齐 | 否 |
| GEN-006 | 是 | 唯一 active 索引 + 未结束 inactive 槽位拒绝；不回写历史快照 | 否 |
| GEN-007 | 是 | 绑定创建幂等键；未来 inactive 不提前同步 | 否 |
| GEN-008 | 是 | OpenAPI entity_role 六值与 status 四值已标契约缺口（009/023→M392）；空间类型字典 vs OpenAPI 已标明快照 | 否 |
| GEN-009 | 是 | 绑定/grant 错误码与失败路径有中文冲突码，无伪成功声称 | 否 |
| GEN-010 | 是 | PRD §15A 脱敏/导出密码/敏感页；不在本模块发明明文秘密 | 否 |
| GEN-011 | 是 | 关键动作留痕、导出记录永久；集团审批属性有 V394 证据 | 否 |
| GEN-012 | 是 | 真分页 LIMIT；summary 窗口由政策封顶 | 否 |
| GEN-013 | 是 | 引用的迁移文件名在 `db/migration` 存在 | 否 |
| GEN-014 | 是 | V410/V427/V392/V394/V184/V349/V217 只读核存在；未跑 migrate | 否 |
| GEN-015 | 不适用 | 无本模块 UI 文件 | 否 |
| GEN-016 | 是 | 路径/接口/表/状态/版本/完成声明已对当前源码；024 括注已删 | 否 |
| GEN-017 | 是 | 差距与实现证据已标，未把业务发明伪装成已完成 | 否 |
| GEN-018 | 是 | 本席结论落到 HTML 原文或可复现命令，不靠上一轮意见 | 否 |
| PGM-002 | 是 | PRD §2.2 治理五步顺序 | 否 |
| PGM-003 | 是 | 验收包/PRD 模块入口覆盖 | 否 |
| PGM-004 | 是 | 综合体和集团章 | 否 |
| PGM-005 | 是 | 空间对象九类；不另立楼栋区块为业务对象 | 否 |
| PGM-006 | 是 | 业态六值 | 否 |
| PGM-007 | 是 | 商户/主体/四类绑定 | 否 |
| PGM-008 | 是 | 合同准入/红线 | 否 |
| PGM-009 | 是 | 员工/私人手机/工作手机 | 否 |
| PGM-010 | 是 | 权限职级 | 否 |
| PGM-011 | 是 | 设备管理 | 否 |
| PGM-012 | 是 | 商户侧边界 | 否 |
| PGM-013 | 是 | 开放接口非范围/待排期 | 否 |
| PGM-014 | 是 | 留痕复核 | 否 |
| PGM-015 | 是 | 楼宇运营边界 | 否 |
| PGM-016 | 是 | 破窗/隐私 §15B/15A | 否 |
| PGM-017 | 是 | 项目归属要求；实现多对多已披露 | 否 |
| PGM-018 | 是 | 多对多协作；V410 删父链 | 否 |
| PGM-019 | 是 | 各主体职责表 | 否 |
| PGM-020 | 是 | 集团下钻细则：模板决定层级、无数据范围设置项；聚合已承接、隔离待对齐 | 否 |
| PGM-021 | 是 | 避免多综合体数据混 | 否 |
| PGM-026 | 是 | 四类唯一绑定+主体权责六类财务主体；绑定标审批链未落地 | 否 |
| PGM-027 | 是 | 商户侧全开放/不看全局 | 否 |
| PGM-031 | 是 | 验收包 §2A / PRD §6.5.4 经销商全链 | 否 |
| PGM-035 | 是 | 验收包边界：提成进工资单月结、禁止现金现抽 | 否 |
| PGM-040 | 是 | 单一功能权限点 | 否 |
| PGM-041 | 是 | 角色装配两支+HR 定层级/职位即适配；本模块现行句已去掉相反排除 | 否 |
| PGM-042 | 是 | 职级影响默认模板；现行规则不再排除职级变化 | 否 |
| PGM-043 | 是 | 外包/审计/导出脱敏 | 否 |
| PGM-044 | 是 | 隐私桌台/经销职责分离 | 否 |
| PGM-048 | 是 | 商户侧授权范围 | 否 |
| PGM-060 | 是 | 会员治理不在本模块发明后台导入 | 否 |
| PGM-065 | 是 | 治理连接各业务 | 否 |
| PGM-066 | 是 | 已拍板集团视图与项目归属；不把未入 HTML 的阈值立法 | 否 |

84/84 已记录。无锁外主张。

---

## 4. 对抗式（grilling 自问，不派子代理）

Q1 绑定 L49 是否仍写「归档走存档端点」？  
否。现行 L49 只列服务五值与无 DB CHECK。`rg "归档走存档|存档端点"` 绑定设计仅 L121 声明不存在 `/active` 与 usages。`PlatformController` 对 business-entities 只有 GET、POST、POST `{entity_id}/projects`。

Q2 `requireSubjectStatus`「归档请使用存档操作」是否构成本模块文档缺陷？  
否。该句在 `PlatformService.java:893-898`，由商户与主体创建共用。主体无存档入口。这是后台模块观察（O-B5R4-01 口径），本模块文档已不再把该提示语写成主体存档端点存在。

Q3 职级变化是否仍被本模块现行规则排除出重算？  
否。集团 L76、PRD L407/L541/L656 现行句无「仅职级变化」排除。L5/L966/L971/L117 为历史或本次删除记录。他模块同根句不在本单元集。

Q4 grant 入口是否被写成六类都可申请？  
否。L23/L70：入口三类；`role_permission` 未开放；两类服务内生成。`normalizeGrantRequestType` 只放行三类。

Q5 集团总览是否被写成已按项目过滤？  
否。L14/L95/L110 与验收包 G7 明确：审批属性闭合 ≠ 项目级结果隔离。`requirePlatformScope` 只拦商户/外包。

Q6 未来绑定是否被写成自动启用或提前同步商户三主体？  
否。创建：`effective ? "active" : "inactive"`，仅 `effective` 时 `syncMerchantEntities`。变更：未到日一律 `BINDING_NOT_EFFECTIVE`。文 L80/L90/L99 同向。

Q7 POST projects / 指定项目是否假称 USB 最高权限？  
否。`associateBusinessEntityProject` → `requireProjectConfigurationAuthority`（平台范围 + `isMasterAuthority`）。`creationProject(..., true)` 仅最高权限可指定已存在项目。

Q8 主体表是否仍被写成带 `project_id` 列？  
否。绑定 L39/L56：无列，V427 多对多。验收包 G7 已删「所有对象继续携带 project_id」。PRD L569/L657 的 `project_id` 是治理要求用语，并与「项目级过滤仍待对齐」并列，不构成当前图式完成声明。

Q9 私人手机是否仍写「考勤打卡」为可离场功能？  
否。PRD L211/L377 为「查看考勤」。§12B.2 标题「考勤打卡」对应 BSM L1445 领取工作手机，不是私人手机功能列表。

Q10 空间类型是否把 OpenAPI 枚举写成活闭集？  
否。PRD L246-262：活校验是字典；OpenAPI 是契约快照；mall/bar_table/facility 不在 V184 八码中。`createSpace` 走 `listActiveSpaceTypeCodes()`。

Q11 DELETE 职位模板是否被写成删除入口？  
否。文写恒 409；代码抛 `ARCHIVE_APPROVAL_REQUIRED`。真实路径是 lifecycle。

Q12 税费第 5 类是否被写成 `permissions` 已有 `finance.tax_*`？  
否。集团 L69：薪资个税档 `staff.tax_bracket.manage`（V217）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 动作不是 permissions 行；`rg finance\.tax_` 于 migration 无命中。待对齐范围收窄为该权限码。

Q13 能否为「archive_reason 列存在但 Java 记录不映射」新开缺陷？  
否。绑定 L50 把它列为当前表字段（V354 确有列），L108 同时写完整存档流程未落地。第一次正常阅读不会把该列当成已开放存档端点。不为理论场景加需求。

Q14 绑定 GET 列表经 merchant.project_id 过滤，文未逐字写「绑定列表按项目过滤」，是否遗漏到冲突？  
否。文未声称未过滤全库；L117 只称真分页。服务 `listBusinessEntityBindingsPaged(currentProject())` 属实现细节，不构成与 HTML 相反或假完成。

Q15 验收包补录/变更日志是否把废止闭集升回现行？  
否。废止框保留；§7 现行状态表与 ENUM 一致。

---

## 5. 入口 / 表 / 状态 / 版本核对（类型 4，非实跑）

Controller 前缀：

- `PlatformController` `@RequestMapping("/api/v1/platform")`
- `AccessController` `@RequestMapping("/api/v1/access")`
- `AnalyticsController` `@RequestMapping("/api/v1/analytics")`

| 文档声称 | 映射证据 | 结果 |
|---|---|---|
| GET/POST `/business-entities` | `PlatformController` L42 / L52 | 存在 |
| POST `/business-entities/{entity_id}/projects` | L97 | 存在；服务 L363 最高权限 |
| GET/POST `/business-entity-bindings` | L248 / L257 | 存在；真分页+当前项目（经商户） |
| POST `/business-entity-bindings/{id}/change` | L266 | 存在 |
| 主体完整 CRUD / `/active` / usages | 无对应 Get/Put/Delete by id；无 `/active` | 文档「不存在」为真 |
| 职位模板 CRUD + DELETE 409 + lifecycle 五动作 | AccessController L168-209；AccessService L481-541 | 真 |
| `PUT /users/{id}/position` `/staff-level` | L281 / L262 | 真 |
| role-assignments / unassign | L105 / L112 | 真 |
| grant-requests POST/GET/review/summary | L132-166 | 真 |
| temporary-grants POST/revoke | L211 / L221 | 真 |
| GET `/analytics/group-overview` | AnalyticsController L119 | 真 |
| ADM-ACC-003 / ADM-ANA-002 / ADM-STF-004 | `adminRoutes.ts` | 真 |

表/迁移：V410 删 `parent_entity_id` 并将 `entity_role` CHECK 收为三值；V427 建 `business_entity_projects`（主体表无 `project_id`）；V354 加 `archive_reason`；V010 部分唯一索引仅 `status='active'`；服务另拦 `inactive AND effective_to IS NULL`；V392 六类 CHECK；V394 `analytics.group.view` 审批属性与 `access.grant_request.apply` 风险档；V184 八码；V349 `is_sales_position`；V217 `staff.tax_bracket.manage`。`business_entities.status` 无 DB CHECK。

状态：主体服务五值 vs HTML 商户六态（含归档）——文已标未实现完整词表。绑定服务 `active/inactive/suspended`。OpenAPI `BusinessEntityRequest.entity_role` 仍为 operation/contract/settlement/invoice/payment/other（L29208-29214）；`status` 仍为 draft/active/suspended/archived（L29231-29235）——与 009/023 披露一致，不在本模块假装已闭合。

版本：绑定 V0.13、集团 V1.03、验收包 V0.06、PRD V0.06 与文首/变更记录一致。

---

## 6. 024 / B5-r4 FAIL 残留

B5-r4 席甲 FAIL 项（派发称已定稿为 024、B3 第 5 次删绑定设计 L49 括注「归档走存档端点」）：

- 现行 L49 原句：「当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`」
- 无括注、无「存档端点」
- 与 `SUBJECT_STATUSES`（`PlatformService.java:54-55`）及无 DB CHECK 一致
- 本席不把「全部否」写成功能全实现

---

## 7. 观察（不进本模块修复队列，不新增判据）

1. **O-B5R5-01 = O-B5R4-01 续**：`PlatformService.requireSubjectStatus` 错误文案「归档请使用存档操作」对主体创建调用方仍可能误导。入口在 M009 `PlatformService`，本模块文档已切断「主体归档走存档端点」的完成声称。
2. **009/023**：OpenAPI 旧枚举归 M392。
3. **017**：集团项目级结果过滤归 M348/依赖 M032。
4. **019 同根句**：`platform-governance-product-spec-v0.2.md`、`business-html-source-of-truth-acceptance-spec-v0.1.md`、`product-requirements-v0.1.md` 仍可能含职级/责任范围排除，只作跨模块观察。
5. **BSM-114/BSM-120 HTML 自身**：锁旧摘录/页脚日期，M009 已登记，不申请改 HTML。

---

## 8. 覆盖表

| 文件 | 行 | 覆盖 |
|---|---|---|
| 绑定设计 | 1–154 | 全文 |
| 集团 PRD | 1–117 | 全文 |
| 验收包 | 1–291 | 全文（含补录废止框） |
| 治理 PRD | 1–971 | 全文（含 §15–§16） |
| 判据定义 | 84 ID | 定义区+映射区+现行 HTML 锚点抽核 |
| 三份 HTML | 相关锚点 | BEQR 平台治理表、BSM 存档/入口/私人手机、PGM 项目/权限/角色装配/职级 |

未覆盖：无。未跑 Java/HTTP/psql。

---

## 9. 四分类与 findings

本席临时编号：无新 GR 项。

审完全文后：无冲突、无未披露的假完成、无锁外添加。已登记缺口保持登记，不因「全部否」改写成已实现。不为理论场景加需求。不因获批追加或额度用尽降标准。

---

## 10. 结论

**PASS**

依据：四文件全文覆盖；84 项均有适用/不适用记录与类型 1 或 4 证据；三线已跑；B5-r4 的 L49 括注已不在现行文；当前实现声称已对 Controller/服务/迁移只读核验；无未答 questions；无本席新缺陷。
