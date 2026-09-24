CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r4 席乙（Grok）整模块三线全量重审 raw

- 报告版本：V0.01（本路径原先不存在，本次创建）
- 席位：审计席乙 Grok（grok-4.6 / xhigh）
- 步骤：B5-r4（操作员 2026-09-19 批准追加一次修复后的整模块重审；B3 累计 4/4；不是 B4 diff 复审，不复用 B2/B4 票）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 正本：`/home/james/complex-management` `main` 工作区
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11
- 独立实算命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- 独立实算结果：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（64 位小写 hex；未抄 sidecar/ledger/派发期望值。事后对照 sidecar 与 ledger 同行，仅作交叉）
- 提示词：`160-audit-prompt.md` V0.17 B1/B5 及三线；runbook V0.02
- 互盲：未读 `M013-b5-r4-self-raw.md` 及其票；可读旧 B1/B4/B5；未派子代理/MCP/第三席；未问人；未改业务/HTML/锁/findings/ledger；未 fetch/commit/push/merge/deploy/写库；未建脚本/日志（本席未再为验收新建临时索引）
- 证据纪律：只读。文档模块不强制跑测试。未冒称 Java/HTTP/数据库实跑。可执行声称用类型 1（三份 HTML 逐字）或类型 4（sha256sum / wc / git cat-file / git hash-object / git diff --stat / rg / graft）
- 历史已批口径（独立复核后保留，不替代发现）：私人手机查看考勤、打卡仅工作手机；BSM-114 锁旧摘录与 BSM-120 页脚日期为 M009 已登记观察，不恢复旧句、不申请改 HTML、不新增锁；PRD 业态不再维护/配置引用开关，历史日志按当时文字解释，不把废止框当新规则；集团 PRD 第 5 行修订说明、PRD 第 966 行变更日志及本次新增变更记录行引述被删词属历史/日志记录；公示 grant 三请求类型、V392 六类 CHECK 与服务内生成两类分清；绑定未来 inactive 不提前同步但完整审批仍缺；项目聚合 ≠ 项目结果隔离；同范围岗位普通立即/高危待批，普通角色授权不免审；summary 参数使用后台政策。001–023 当前全部「否」（20 已修 + 009/010/017 登记）≠ 功能全实现。017 项目过滤归 M348/依赖 M032；009 OpenAPI 旧 entity_role 与 023 所披露 status 四值归 M392；019 同根句在 M014/M421/M439 只作跨模块观察，不在本模块改。

---

## 0. 技能预检（§6，按本环境实核）

本席已读并按收窄口径使用：

| 技能 | 本环境 | 本步用法 |
|---|---|---|
| `graft` | 已读全文并调用 | 定位 `BUSINESS_ENTITY_ROLES`、`business_entity_projects`、绑定错误码 |
| `using-superpowers` | 已读；本任务为派定席乙，`SUBAGENT-STOP` 适用 | 开工技能自检；用户 B5 只读指令优先于技能本体提问/实施闸 |
| `using-arc` | 已读 | 用户任务是 160 审计，不路由 Arc `audit` |
| `grilling` | 已读全文 | 对抗式主武器；自问自答，事实自读，不派子代理、不问操作员 |
| `grill-me` | 已读（正文仅“Run a /grilling session”） | 按意图转发 `grilling`，不另起第三席 |
| `verification-before-completion` | 已读全文 | 任何 PASS 前用本次命令核哈希/行数/after-tree 四文件 blob/源码声称 |
| `codebase-design` | 已读前部 | 文档模块用接口/实现深度语言：本模块接口是四份文档对 HTML 与当前实现的声称 |
| `diagnosing-bugs` | 已读前部 | 声称与源码不一致时按读代码→最小命令定位；本轮无运行时故障环可建 |
| `ponytail-review` | 已读全文 | 对抗式找可删；废止框与历史日志不删 |
| `long-running-background-tasks` | 已读 | 本步无长任务，不启动 monitor |

已核存在、本步不适用（B5 只读、不修、不提交、不写收口）：`brainstorming`、`writing-plans`、`systematic-debugging`、`test-driven-development`、`tdd`、`requesting-code-review`、`receiving-code-review`、`ponytail`、`resolving-merge-conflicts`、`writing-for-agents`（收口文件仅双 PASS 后由席甲写）。

明确不用（提示词已划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`domain-modeling`、`code-review`（mattpocock）、`ponytail-audit/debt/gain`。

缺口（未冒充已调用）：
- `batch-grill-me`：本环境 mattpocock `skills/productivity/` 无此目录
- `ubiquitous-language`：本席本次技能根未核到可调用副本
- `git-guardrails-claude-code`：可选；无 Claude Code hooks，记缺口，本步无 git 写操作

graft 本席检索合计 tokens saved ≈ 87,886（3 次命中调用；migration 路径 graft 未索引，改 rg）。

---

## 1. 范围、哈希、after-tree（类型 4）

模块文件（`plan/modules.md` FILES 段，4 个，计划冻结有效行 1517 不改）：

| 路径 | wc -l | 独立 sha256sum | 工作区 blob | after tree `ddff52dad5669cf17a598b75490df0bcfb454429` blob |
|---|---|---|---|---|
| `docs/business-entity-binding-development-design-v0.1.md` | 154 | `9cdec650f49335a42a8769aaea0fbfa0881f15b77994171b0d381538edf001b8` | `bad5f736e4a4cf2c6d5657753c965b953568e784` | 同 |
| `docs/group-governance-rebuild-prd-v1.0.md` | 117 | `7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52` | `d252bdc30d89bc195f80cb8ed6eda835df60de84` | 同 |
| `docs/platform-governance-acceptance-pack-v0.2.md` | 291 | `56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0` | `cb89c73ada4d6e36fbcc257aae10609a407ec1ed` | 同 |
| `docs/platform-governance-prd-v0.1.md` | 971 | `e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c` | `1b21047eb64406f523826ccbf1fdc8025f8c6fa0` | 同 |
| 合计 | 1533 | — | — | 计划冻结 1517；差额为既有 B3 修文，不改计划数字 |

`git cat-file -t ddff52dad5669cf17a598b75490df0bcfb454429` → `tree`。`git diff --stat ddff52dad5669cf17a598b75490df0bcfb454429 -- <四路径>` 为空。四文件与 after tree 相同。HEAD blob 较旧，工作区即本次审本。

全文阅读：绑定 1–154、集团 PRD 1–117、验收包 1–291、治理 PRD 1–971。未抽样。

映射：`criteria-lock.md:6392` M013 → 84 ID = BEQR 21 + BSM 12 + GEN 18 + PGM 33。定义区已全文阅读对应条目（含 HTML 原文块）。三份 HTML 相关锚点按现行文件核对，不只信锁内行号。

命令：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
wc -l / sha256sum / git hash-object <四文档>
git cat-file -t ddff52dad5669cf17a598b75490df0bcfb454429
git ls-tree ddff52dad5669cf17a598b75490df0bcfb454429 <四文档>
git diff --stat ddff52dad5669cf17a598b75490df0bcfb454429 -- <四文档>
```

---

## 2. 第一性原理线（每文件三问）

### 2.1 `business-entity-binding-development-design-v0.1.md`（V0.12，现行开发设计）

1. **为何必须存在**：删除则 PGM-026「四类主体唯一绑定」、BEQR-143、PGM-018 多对多协作、BSM-004 存档删除在实现侧没有当前模型/API/差距说明书。答得上，有业务上位。
2. **声称行为是否被实际执行验证？** 本席只读对照源码/迁移/Controller，未跑 Java/HTTP/DB。§3.1/§5 与现行入口一致（见顺从表）：V410 三角色、V427 多对多、创建写关联、列表按 `currentProject()` 过滤、`POST .../business-entities/{entity_id}/projects`、未来 `effective_from` 写 `inactive` 且不提前同步、OpenAPI role 六值与 status 四值均披露为契约缺口。完整权责审批链、下游统一引用、退出缓冲——文内已标差距。
3. **未验证前提：** 「当前数据模型」节被当成完整现行模型时，须同时读差距段。未把「否」当已实现。

### 2.2 `group-governance-rebuild-prd-v1.0.md`（V1.03）

1. **为何必须存在**：承接 PGM-017/020 集团视图、PGM-041 角色装配与五类高危。删除则 V169/V170/V115 与 HTML 的产品对齐说明书消失。
2. **声称是否验证：** `GET /api/v1/analytics/group-overview` 同时 `requirePermission("analytics.group.view")` 与 `requirePlatformScope()`；`normalizeGroupBy` 仅 `project|entity`；`buildGroupOverview` 按全部 ETL 项目聚合，无项目级结果过滤。申请入口 `normalizeGrantRequestType` 仅三类；V392 CHECK 六类；`user_scope_change`/`position_high_risk_release` 服务内生成。`assignStaffLevel` 已定岗改职级走 `assignUserPositionDirect`。DELETE 模板抛 `ARCHIVE_APPROVAL_REQUIRED`。`grantSummary` 阈值覆盖返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。税费 `finance.tax_*` 权限码仍无，文内待对齐。生产 `@Autowired` 构造器把 `grantApprovalEnabled` 写死 `true`，配置项已移除，测试构造器保留入参。
3. **未验证前提：** `requirePlatformScope()` 不等于项目结果隔离。第 5 行 2026-07-27 修订说明是历史记录。

### 2.3 `platform-governance-acceptance-pack-v0.2.md`（V0.06）

1. **为何必须存在**：产品设计验收清单，承接 PGM 模块覆盖、状态闭集、经销商与金额精度。删除则 P0 设计收口无检查表。
2. **声称是否验证：** 本文自称不是接口/数据库验收。§7 状态与 ENUM 对照成立。G7 现文把审批属性闭合与项目范围隔离分开，未再写「所有对象继续携带 project_id」。§11 2026-06-08 行「P0 只预留 project_id」为历史日志。
3. **未验证前提：** 「五件套已补齐」指向规格/流程图，不在本模块四文件内，不把补齐写成编码完成。

### 2.4 `platform-governance-prd-v0.1.md`（V0.06）

1. **为何必须存在**：平台治理层产品需求。删除则 PGM-002 治理顺序、PGM-017–021、PGM-026、PGM-040–044、BEQR-150–159 在本模块无 PRD 载体。
2. **声称是否验证：** 私人手机六项写「查看考勤」，与现行 PGM L741、BSM L930/L1865 一致；§12B.2 打卡仍从领取工作手机开始，与 BSM L1445 一致。§6.10.9 / §7.8.2 / §12 14B 现行句不再排除职级/责任范围，只保留任意手工模板应用不触发。§6.2.1 字典八码与 V184 种子一致，OpenAPI 枚举为契约快照。§12#8 员工域 `StaffApprovalService` 两处 `evaluate`（排班发布、处罚上限）存在；四域动作前未在本席检索到对等接线，文内已写待对齐。§12#15 集团审批属性按 V394 闭合，项目级过滤待对齐。
3. **未验证前提：** 「所有对象必须带 `project_id` 归属」是产品强制要求（HTML 原文是「综合体项目归属」）；现行主体表无该列、用 V427 关联表，绑定设计已诚实写。不把列名当成当前主体表已有列。变更日志 966 行引述被删排除句属历史。

---

## 3. 顺从式（84 项逐条）

判定列：适用？证据（类型）？本模块工程缺陷？

| ID | 适用 | 证据 | 缺陷 |
|---|---|---|---|
| BEQR-063 | 是 | HTML L111 酒吧高危动作。PRD §12 14A / 集团 PRD §3 把沽清等列入五类高危。本模块无酒吧实现声称与原文相反 | 否 |
| BEQR-074 | 是 | HTML L125 隐私桌台三权。PGM-044 原文块与 PRD 权限章未发明相反规则 | 否 |
| BEQR-143 | 是 | HTML L226 四类主体。绑定 §1 表与三条规则、PRD §12#19 照抄唯一绑定 | 否 |
| BEQR-144 | 是 | HTML L227 多对多非行政链。绑定 §1.2、PRD 组织关系修订、集团 PRD 边界 | 否 |
| BEQR-145 | 是 | HTML L228 城市项目公司。PRD §5 REGX-17 排除清单与 ENUM 同口径 | 否 |
| BEQR-146 | 是 | HTML L229 娱乐平台/游乐/传媒/中央餐饮/独立酒吧。PRD §5 娱乐平台公司段照抄 PGM-019 | 否 |
| BEQR-147 | 是 | HTML L230 收入归属。PRD 未把顶层平台写成自动取得收入 | 否 |
| BEQR-148 | 是 | HTML L231 六类合作+其他须指定收费模板。PRD §6.5.1 / 验收包合作模式 | 否 |
| BEQR-149 | 是 | HTML L232 M1–M10 上中下。PRD §6.10.8 / §12#6 | 否 |
| BEQR-150 | 是 | HTML L233 十一端。验收包 §6 终端开关页、PRD §2.1 与现行十一类一致 | 否 |
| BEQR-151 | 是 | HTML L234 最终权限公式。PRD §6.10.4 / §12#13–14 | 否 |
| BEQR-152 | 是 | HTML L235 项目隔离+敏感数据分权。集团 PRD §3/§4、PRD §7.8 与 G7 隔离待对齐诚实 | 否 |
| BEQR-153 | 是 | HTML L236 开关优先级。验收包 §4/§6、PRD §12#16 | 否 |
| BEQR-154 | 是 | HTML L237 项目六态。验收包 §7、PRD §7.1/§10；废止框禁止当门控 | 否 |
| BEQR-155 | 是 | HTML L238 六态+试运营不记录+缓冲期。绑定 §3.1 差距、PRD §7.4、服务 `SUBJECT_STATUSES` 五码+归档走存档轴。OpenAPI status 四值已在绑定 §3.1 披露为 023/M392 | 否 |
| BEQR-156 | 是 | HTML L239 合同八态+当地时间。验收包 §2A/§7、PRD §6.5 | 否 |
| BEQR-157 | 是 | HTML L240 员工五态。验收包 §7、PRD §7.6/§10 | 否 |
| BEQR-158 | 是 | HTML L241 设备七态。验收包 §7、PRD §7.7/§10 | 否 |
| BEQR-159 | 是 | HTML L242 权限五态。验收包 §7、PRD §7.8/§10 | 否 |
| BEQR-166 | 是 | HTML L255 合同三类审批链。验收包 G5、PRD §6.5.1/§12#18 | 否 |
| BEQR-189 | 是 | HTML L291 服务点单管理费。本四文件未展开该细则、无相反完成声称 | 否 |
| BSM-004 | 是 | HTML 存档/删除/占用/密钥免审。绑定 §2.5/§7、PRD §15C。实体完整存档流文内标未落地 | 否 |
| BSM-005 | 是 | HTML 分步引导新建。验收包/PRD 未把「一张大表一次填完」写成现行必过 | 否 |
| BSM-006 | 是 | HTML 动态码。本模块无核销码实现声称 | 否 |
| BSM-007 | 是 | HTML 平台管理员≠最高权限。PRD §7.8.1 | 否 |
| BSM-009 | 是 | HTML 基础管理入口。四文件定位与此一致 | 否 |
| BSM-028 | 是 | HTML 基础管理主要功能（多对多、城市项目公司、七类红线、合同八类变更）。PRD/验收包承接，废止框不回灌 | 否 |
| BSM-029 | 是 | HTML 和谁有关。PRD §11 后续系统依赖 | 否 |
| BSM-072 | 是 | HTML 商户侧授权范围。PRD/验收包商户边界 | 否 |
| BSM-074 | 是 | HTML 商户连接基础/权限。绑定同源引用、集团 fail-closed 商户 scope | 否 |
| BSM-114 | 是 | 锁摘录仍含私人手机「考勤打卡」；现行 HTML L1865 为「查看考勤」（类型 1）。本模块 PRD 写查看考勤。锁旧摘录=M009 观察，不恢复、不申请 HTML | 否 |
| BSM-115 | 是 | HTML 子系统必须连。PRD §11 / PGM-065 连接表 | 否 |
| BSM-120 | 是 | HTML 已拍板清单。PRD §12 已确认与之对齐处已核；页脚日期观察不重开 | 否 |
| GEN-001 | 是 | 四文件均有 HTML 上位；无纯转发空章 | 否 |
| GEN-002 | 是 | 文档模块；入口声称与源码对照见 §2。当前 API 表含项目关联入口 | 否 |
| GEN-003 | 是 | 文档模块不强制测试；未把「测试通过」当证据 | 否 |
| GEN-004 | 是 | 无本模块可执行输入面；文档未把未校验输入写成已完成 | 否 |
| GEN-005 | 是 | 绑定权限表、集团 requirePlatformScope、模板 DELETE 走审批。隔离缺口诚实 | 否 |
| GEN-006 | 是 | 绑定唯一索引 `uq_business_entity_bindings_active_merchant`（V010 部分唯一）。文内未把未落地完整性写成已落地 | 否 |
| GEN-007 | 是 | PRD §2A.3 先成功为准；绑定未发明自动启用 | 否 |
| GEN-008 | 是 | entity_role 旧六值=009/M392；status 四值=023/M392，绑定已披露。空间 OpenAPI≠字典，PRD §6.2.1 已写明 | 否 |
| GEN-009 | 是 | 绑定列出 ACTIVE_BINDING_EXISTS 等冲突码，与 PlatformService 抛码一致（只读） | 否 |
| GEN-010 | 是 | 私人手机不读硬件识别码；导出/暗纹在 PRD §15A。未把未立法 Keystore 升规则 | 否 |
| GEN-011 | 是 | 留痕七态、开关变更四要素。文内未关闭留痕 | 否 |
| GEN-012 | 是 | 分页默认 500 有上限声称；无本模块性能完成声明 | 否 |
| GEN-013 | 是 | 无构建入口声称 | 否 |
| GEN-014 | 是 | 绑定列 V002/V005/V010/V318/V354/V410/V427；V318 父链已由 V410 删除只作历史 | 否 |
| GEN-015 | 是 | PRD §2A.2 全中文；无界面完成声称 | 否 |
| GEN-016 | 是 | 路径/版本/完成声明对照现行源。V427/项目关联 API/status 契约裂口均已写入当前句。G7 不再把「所有对象继续携带 project_id」写成现状 | 否 |
| GEN-017 | 是 | 本席新问题按冲突→细化→技术性→添加。本轮无新缺陷项。跨模块缺口保持观察 | 否 |
| GEN-018 | 是 | 结论落到 HTML 原文或只读命令，不引用席位意见当依据 | 否 |
| PGM-002 | 是 | HTML 治理五步。PRD §2.2 顺序固定；与实现闸口链分层说明 | 否 |
| PGM-003 | 是 | 模块入口卡片。验收包 §4 覆盖表 | 否 |
| PGM-004 | 是 | 综合体和集团卡片。集团 PRD / PRD §8.2 | 否 |
| PGM-005 | 是 | 空间和点位。PRD §6.2 / 验收包空间树 | 否 |
| PGM-006 | 是 | 商户和主体。绑定+PRD §6.4 | 否 |
| PGM-007 | 是 | 合同和准入。PRD §6.5 / 验收包准入 | 否 |
| PGM-008 | 是 | 员工和手机。PRD §6.9 查看考勤 | 否 |
| PGM-009 | 是 | 权限和职级卡片 | 否 |
| PGM-010 | 是 | 终端开关卡片。十一端 | 否 |
| PGM-011 | 是 | 设备管理卡片。PRD §6.8 | 否 |
| PGM-012 | 是 | 商户侧卡片 | 否 |
| PGM-013 | 是 | 开放接口卡片。本模块未发明已接第三方完成 | 否 |
| PGM-014 | 是 | 留痕复核卡片。PRD §7.9 | 否 |
| PGM-015 | 是 | 楼宇运营卡片。验收包范围含楼宇 | 否 |
| PGM-016 | 是 | 应急与隐私。PRD §15B | 否 |
| PGM-017 | 是 | 现行 HTML L412–413「所有基础对象都要带综合体项目归属」。绑定用 V427 关联表写归属；PRD 产品句用 `project_id` 指归属，未把主体表缺列写成「没有归属实现」 | 否 |
| PGM-018 | 是 | 多对多协作。绑定 §1.2、V410 删父链 | 否 |
| PGM-019 | 是 | 各主体职责+酒吧股权牌照场地资金。绑定 §1 末段、PRD §12C | 否 |
| PGM-020 | 是 | 集团下钻按职位模板、数据范围无后台项。PRD §6.10、集团 PRD §4。聚合≠隔离已分开 | 否 |
| PGM-021 | 是 | 避免多综合体数据混。无「已混在一起」的完成声称 | 否 |
| PGM-026 | 是 | 四类主体绑定+主体权责。绑定全文；审批链标差距 | 否 |
| PGM-027 | 是 | 商户侧全开放但不能看全局。集团 fail-closed 商户 | 否 |
| PGM-031 | 是 | 经销商治理。PRD §6.5.4 / 验收包 §2A | 否 |
| PGM-035 | 是 | 提成进工资单、不允许现金现抽。验收包 §8 / PRD 12B | 否 |
| PGM-040 | 是 | 单一功能授权。PRD §12#10 | 否 |
| PGM-041 | 是 | 现行 HTML L658 角色装配两支+HR 定层级/职位即适配。本模块现行句已去掉与之相反的职级/责任范围排除，只留手工模板不触发。AccessService:1305–1313 改职级重算（只读） | 否 |
| PGM-042 | 是 | HTML L669 职级影响默认权限模板。现行规则不再排除职级变化 | 否 |
| PGM-043 | 是 | 外包与审计。PRD §4.1 标代码待落地 | 否 |
| PGM-044 | 是 | 隐私桌台/挂单/经销权限。PRD 未写成已全实现 | 否 |
| PGM-048 | 是 | 商户侧全开放表。验收包商户管理 | 否 |
| PGM-060 | 是 | 会员/营销/生物信息。PRD 非范围不展开细节 | 否 |
| PGM-065 | 是 | 治理连接各业务。PRD §11 | 否 |
| PGM-066 | 是 | 已拍板：集团视图与项目归属。G7/§12#15 已承接聚合、隔离待对齐 | 否 |

84/84 均记录。无锁外新判据。

---

## 4. 对抗式（grilling 结构，自问自答）

Q1 私人手机是否仍被写成打卡入口？现行 PRD :211/:377 为查看考勤；§12B.2 打卡从领工作手机开始。现行 BSM L930/L1865、PGM L741 为查看考勤。锁 BSM-114 旧摘录不回灌。

Q2 职级变化是否仍被本模块现行规则排除出重算？否。PRD :407/:541/:656 现行句无「仅职级变化」排除；AccessService 已定岗改职级走定岗重算。:966 变更日志是历史。他模块 `platform-governance-product-spec-v0.2.md:1979`、`product-requirements-v0.1.md:601/650` 仍有同根排除句 → 跨模块观察，不在本模块改。

Q3 grant 申请类型是否把 DB 六类写成入口六类？否。集团 PRD :23/:70 入口三类、CHECK 六类、两类服务内生成、`role_permission` 未开放。`normalizeGrantRequestType` 只收三类。

Q4 集团总览是否被写成已按项目过滤结果？否。:14/:95/:110 明确 `requirePlatformScope()` 不替代项目级结果过滤。AnalyticsService.buildGroupOverview 遍历全部 ETL 项目。017 边界保持。

Q5 绑定是否仍把「无 project_id 列」写成没有项目归属落点？否。:39/:57 写明 V427 关联表、创建关联当前项目、列表按当前项目过滤；:116 列出 `POST .../projects`。Jdbc `listBusinessEntitiesPaged` EXISTS 关联表；创建 `associateBusinessEntityProject`。

Q6 OpenAPI status 四值是否仍不披露？否。绑定 :56 登记 draft/active/suspended/archived ≠ 服务五值，023/M392。

Q7 G7 是否仍写「所有对象继续携带 project_id」为现状？否。验收包 :182 只保留审批属性闭合与隔离待对齐。:201 变更记录说明删除该句。:208 历史行不回灌。

Q8 税费第 5 类是否被写成已有 `finance.tax_*` 权限码？否。集团 PRD :69 收窄为权限码待对齐；V217 是薪资个税档。

Q9 未来 inactive 是否被写成已自动启用或已有完整审批草稿？否。绑定 :80/:90/:99。

Q10 普通角色授权是否被写成岗位规则的免审例外？否。集团 PRD :38 岗位规则不推广。

Q11 为理论场景加需求？未新增判据、未申请改 HTML/锁、未发明缓冲期闭环或完整 CRUD。

Q12 获批追加或额度 4/4 是否降标准？否。全文+84+三线后统一结论。

ponytail-review：废止框与历史日志不删。`Lean already. Ship.`（本模块可删项不在 B5 权限内。）

---

## 5. 当前实现声称对照（类型 4，只读）

| 声称 | 对照 |
|---|---|
| `BUSINESS_ENTITY_ROLES` 三值 | PlatformService:62 |
| 主体 status 五码+归档走存档 | :54–56、:893–899 |
| V410 CHECK 三值、删 `parent_entity_id` | V410:9/:18–19 |
| V427 `business_entity_projects` | V427:13–17；创建 :133；列表 paged :1254–1260 |
| 未来生效 inactive 不同步 | PlatformService:778–797 |
| 绑定错误码 | :829–836/:952/:959 |
| 唯一索引 | V010:15–18 |
| OpenAPI entity_role 六值、status 四值 | openapi-p0.yaml:29208–29235 |
| `POST /business-entities/{entity_id}/projects` | PlatformController:97–104；须最高权限 :377–381 |
| grant 入口三类 | AccessService:2044–2049 |
| V392 六类 CHECK | V392:8–10 |
| group-overview 守卫+全项目聚合 | AnalyticsController:119–125；AnalyticsService:774–811 |
| `analytics.group.view` requires_approval | V394:16–18（相对 V115 初始 false） |
| DELETE 模板 409 | AccessService:482 |
| grantSummary 禁覆盖 | AccessService:1044–1048 |
| 改职级重算 | AccessService:1305–1313 |
| 生产审批闸恒开 | AccessService:67–83 写死 true |
| V184 字典八码 | V184:16–23 |
| 员工域 evaluate 两处 | StaffApprovalService:44–46/:61–64 |
| `staff.tax_bracket.manage` | V217:45 |
| `finance.tax_rule.adjust` 策略非 permissions 行 | SupervisorReviewService:49/:65 |

未执行迁移、单测、HTTP、psql。

---

## 6. 本次 findings

无新工程缺陷。

已有 001–023 保持「否」：已修内容（含 019 职级排除、020 申请类型、021 G7 project_id 现状句、023 status 披露，以及既有 001–018）未回归；009/010/017 不重开、不解释成功能已全实现。019 同根句在 `docs/platform-governance-product-spec-v0.2.md`、`docs/product-requirements-v0.1.md` 等仍在，只作跨模块观察。

四分类：无「添加」项，不发 2.2 人闸。无 HTML 修改申请。无锁外主张。

---

## 7. 覆盖表

| 文件 | 行 | 读法 | 三线 |
|---|---|---|---|
| 绑定设计 V0.12 | 1–154 | 全文 | 第一性 2.1；顺从绑定相关 ID；对抗 Q5/Q6/Q9 |
| 集团 PRD V1.03 | 1–117 | 全文 | 2.2；BEQR-152/PGM-041/GEN-005/008/016；Q2/Q3/Q4 |
| 验收包 V0.06 | 1–291（含补录） | 全文 | 2.3；状态/十一端/G7；Q7 |
| 治理 PRD V0.06 | 1–971 | 全文 | 2.4；PGM-002–021/026/031/035/040–044/066；Q1/Q2/Q8 |

OVERSIZE：否。未抽样宣称整文件已审。

---

## 8. 结论

**PASS**

同时满足单席 PASS：四文件全文覆盖；84 个映射 ID 均有适用结论与类型 1 或 4 证据；三线均执行；实现声称只读对照，未冒称实跑；无未答 questions；无未定稿新缺陷；本报告第一行独立实算哈希格式合法。获批追加与 B3 4/4 未降标准。不把 001–023 全否解释为功能全实现。不申请改 HTML/锁，不 LOCK-AMEND。不为理论场景加需求。

（CLI 不附机器标记。正文只在本 raw。）
