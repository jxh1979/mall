CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r3 席乙（Grok）整模块三线全量重审 raw

- 报告版本：V0.01（本路径原先不存在，本次创建）
- 席位：审计席乙 Grok（grok-4.6 / xhigh）
- 步骤：B5-r3（B3 累计 3/3 后的修复后整模块重审，不是 B4 diff 复审）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 正本：`/home/james/complex-management` `main` 工作区
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11
- 独立实算命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- 独立实算结果：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（64 位小写 hex；未抄 sidecar/ledger/派发期望值。事后对照 sidecar 同行，仅作交叉，不作为本席取值来源）
- 提示词：`160-audit-prompt.md` V0.17 B1/B5 及三线；runbook V0.02
- 互盲：未读 `M013-b5-r3-self-raw.md` 及其票；未派子代理/MCP/第三席；未问人；未改业务/HTML/锁/findings/ledger；未 fetch/commit/push/merge/deploy/写库；未建脚本/日志/临时产物
- 证据纪律：只读。文档模块不强制跑测试。未冒称 Java/HTTP/数据库实跑。可执行声称用类型 1（三份 HTML 逐字）或类型 4（可复现只读命令：sha256sum / wc / git cat-file / rg / graft）
- 历史已批口径（独立复核后保留，不替代发现）：私人手机查看考勤、打卡仅工作手机；BSM-114 锁旧摘录与 BSM-120 页脚日期为 M009 已登记观察，不恢复旧句、不申请改 HTML、不新增锁；PRD 业态不再维护/配置引用开关，历史日志按当时文字解释；公示 grant 三请求类型与 DB 预留 `role_permission` 分清；绑定未来 `inactive` 不提前同步、完整审批仍缺；项目聚合 ≠ 项目结果隔离；同范围岗位普通立即/高危待批，普通角色授权不免审；summary 参数使用后台政策；M013-017 归 M348/依赖 M032；M013-009 归 M392。findings V2.28 的 001–018 当前全部为「否」≠ 功能全实现

---

## 0. 技能预检（§6，按本环境实核）

本席已读并按收窄口径使用：

| 技能 | 本环境 | 本步用法 |
|---|---|---|
| `using-superpowers` | 已读全文 | 开工技能自检；用户 B5 只读指令优先于技能本体提问/实施闸 |
| `grilling` | 已读全文 | 对抗式主武器；自问自答，事实自读，不派子代理、不问操作员 |
| `grill-me` | 已读（正文仅“Run a /grilling session”） | 按意图转发 `grilling`，不另起第三席 |
| `verification-before-completion` | 已读全文 | 任何 PASS 前用本次命令核哈希/行数/after-tree/源码声称 |
| `codebase-design` | 已读前部 | 文档模块用接口/实现深度语言：本模块接口是四份文档对 HTML 与当前实现的声称 |
| `diagnosing-bugs` | 已读前部 | 声称与源码不一致时按读代码→最小命令定位；本轮无运行时故障环可建 |
| `ponytail-review` | 已读全文 | 对抗式找可删；废止框已标【已废止/不得回灌】，不另提删 HTML/历史日志 |
| `graft` | 已读全文并调用 | 定位 `BUSINESS_ENTITY_ROLES`、`entity.binding`、grant/overview 符号 |
| `using-arc` | 已读 | 用户任务是 160 审计，不路由 Arc `audit` |

已核存在、本步不适用（B5 只读、不修、不提交、不写收口）：`brainstorming`、`writing-plans`、`systematic-debugging`、`test-driven-development`、`tdd`、`requesting-code-review`、`receiving-code-review`、`ponytail`、`resolving-merge-conflicts`（无合并冲突）、`writing-for-agents`（收口文件仅双 PASS 后由席甲写）。

明确不用（提示词已划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`domain-modeling`、`code-review`（mattpocock）、`ponytail-audit/debt/gain`。

缺口（未冒充已调用）：
- `batch-grill-me`：本环境 mattpocock `skills/productivity/` 无此目录
- `ubiquitous-language`、`git-guardrails-claude-code`：未出现在本席本次技能根；仅其他 agent 树有副本，不记已核实对本席可用

i-have-adhd 输出模式：本 raw 首行即哈希与下一步结构，无开场白。

---

## 1. 范围、哈希、after-tree（类型 4）

模块文件（`plan/modules.md` FILES 段，4 个，计划冻结有效行 1517 不改）：

| 路径 | wc -l | 独立 sha256sum | 与派发声明 |
|---|---|---|---|
| `docs/business-entity-binding-development-design-v0.1.md` | 153 | `21c704562c25af0987d781a91d8269a64caae1fd48dd5461b3561077d9018d65` | 相符 |
| `docs/group-governance-rebuild-prd-v1.0.md` | 115 | `3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5` | 相符 |
| `docs/platform-governance-acceptance-pack-v0.2.md` | 290 | `6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603` | 相符 |
| `docs/platform-governance-prd-v0.1.md` | 970 | `c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333` | 相符 |
| 合计 | 1528 | — | 计划冻结 1517；差额为既有 B3 修文，不改计划数字 |

`git diff --stat 393297499c9a4a70630d6eda6396a42642ae526d -- <上述四路径>` 为空。四文件与 after tree 相同。全文阅读：绑定 1–153、集团 PRD 1–115、验收包 1–290、治理 PRD 1–970。未抽样。

映射：`criteria-lock.md:6392` M013 → 84 ID = BEQR21 + BSM12 + GEN18 + PGM33。定义区已全文阅读对应条目（含 HTML 原文块）。三份 HTML 相关锚点按现行文件核对，不只信锁内行号。

命令：
```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
wc -l <四文档>
sha256sum <四文档>
git cat-file -t 393297499c9a4a70630d6eda6396a42642ae526d  → tree
git diff --stat 393297499c9a4a70630d6eda6396a42642ae526d -- <四文档>  → 空
```

---

## 2. 第一性原理线（每文件三问）

### 2.1 `business-entity-binding-development-design-v0.1.md`（V0.11，现行开发设计）

1. **为何必须存在**：删除则 PGM-026「四类主体唯一绑定」、BEQR-143、PGM-018 多对多协作、BSM-004 存档删除在实现侧没有当前模型/API/差距说明书。答得上，有业务上位。
2. **声称是否被验证**：文档自称 Implementation。类型 4 对照当前源码/迁移（见 §4），不是 HTTP/DB 实跑。
3. **未验证前提**：未跑绑定接口、未连库看 CHECK。以迁移文件与 Java 源码为当前实现证据。完整审批链、下游强制引用、存档删除闭环、退出缓冲，文档已标差距，不把差距当已实现。

### 2.2 `group-governance-rebuild-prd-v1.0.md`（V1.02）

1. **为何必须存在**：承接 PGM-017/020 集团视图、PGM-041 角色装配与五类高危。删除则 V169/V170/V115 与 HTML 的产品对齐说明书消失。
2. **声称是否被验证**：端点/表/权限/审批属性用类型 4 对 AccessController/AccessService/AnalyticsController/AnalyticsService/V170/V394/V217。
3. **未验证前提**：未调 `GET /group-overview`。项目级结果过滤缺口按文档与源码（`buildGroupOverview` 遍历全部 ETL 项目、无调用者项目收敛）记为未实现，归已登记跨模块口径，不写成已隔离。

### 2.3 `platform-governance-acceptance-pack-v0.2.md`（V0.05）

1. **为何必须存在**：产品设计验收清单，覆盖 PGM 模块入口与 BEQR 状态闭集。删除则 P0 设计是否可进入下一阶段失去对照表。
2. **声称是否被验证**：本包声明不是接口/库验收。类型 1 对照 HTML 状态/十一端/七类红线；G7 实现时态用类型 4 对照集团 PRD 与 V394。
3. **未验证前提**：未验收五份设计文档是否物理齐全（页面清单等在其他模块）。本包 §10A 已列已知缺口，不把「必须通过」列当代码已完成。

### 2.4 `platform-governance-prd-v0.1.md`（V0.05）

1. **为何必须存在**：平台治理层产品需求正本（产品细化，解释权仍归三 HTML）。删除则治理顺序、四类主体、权限公式、终端十一端、红线、私人手机边界在 PRD 层无承载。
2. **声称是否被验证**：业务口径用类型 1 对照现行 HTML；「当前实现」句用类型 4 对照源码（员工域 evaluate、V394、V384、字典 vs OpenAPI、私人手机「查看考勤」）。
3. **未验证前提**：未跑四域准入闸。文档已写员工域已接、票务/酒吧/游乐/结算未接。外包自助门户标明代码待落地。不把 P0 验收标准写成当前已实现。

---

## 3. 顺从式：84 项全部适用/证据

判定：映射 ID 全部进入本表。文档模块「适用」= 不得与该锚点原文冲突、遗漏或把未实现写成已实现。实现缺口已诚实标注的，记「适用·差距已写」，不升新缺陷。

### 3.1 BEQR（21）

| ID | 适用 | 证据（类型1/4） |
|---|---|---|
| BEQR-063 | 适用 | 锁原文高危动作（反结/沽清/免单/账单调整/存酒作废）。PRD §12 14A、集团 PRD §3 高危业务举例与之同向；未把酒吧高危写成已在本模块落地。 |
| BEQR-074 | 适用 | 锁原文隐私桌台三权分离。PRD/验收包未另造相反规则；PGM-044 桌台权限由治理 PRD 权限章承接，无冲突。 |
| BEQR-143 | 适用 | HTML L226 四类主体。绑定设计 §1 表与 PGM-026 第四条一致；唯一生效绑定+下游同源为规则，§4/§7 标未闭环。 |
| BEQR-144 | 适用 | HTML L227 多对多非行政链。绑定 §1 规则2、PRD §5、集团 PRD 不写死四层链。V410 删 `parent_entity_id`（类型4：`V410__m010_…sql` L9）。 |
| BEQR-145 | 适用 | HTML L228 城市项目公司资产职责与排除清单。PRD §5 REGX-17 照抄；验收包未把资产业态另立主体。 |
| BEQR-146 | 适用 | HTML L229 娱乐平台/游乐/传媒/中央餐饮/独立酒吧职责。PRD §5 娱乐平台公司段照抄 PGM-019。 |
| BEQR-147 | 适用 | HTML L230 收入归属。PRD 承接娱乐平台不因顶层取得收入；未在本模块发明新名目。 |
| BEQR-148 | 适用 | HTML L231 六类+「其他」须指定收费模板。PRD §6.5.1 第2步、§12 后续细化第3条；业态不再维护新类。锁锚点是 L231 而非 L196 短条。 |
| BEQR-149 | 适用 | HTML L232 M1–M10 上中下。PRD §6.10/§7.8/§12 第6条一致。 |
| BEQR-150 | 适用 | HTML L233 十一端含经销商门户。PRD §2.1/§12 第16条、验收包 §6 终端开关页 enumeration 一致。 |
| BEQR-151 | 适用 | HTML L234 最终权限公式。PRD §6.10 第4条、§7.8、验收包 §4 角色权限行逐字同构。 |
| BEQR-152 | 适用 | HTML L235 项目隔离+敏感数据分权。PRD §7.8/§12 第20条；集团 PRD §4 诚实写守卫≠本项目过滤（M013-017 已登记，文档未再宣称已隔离）。 |
| BEQR-153 | 适用 | HTML L236 优先级与兜底关闭。PRD §12 第16条、验收包终端开关行一致。 |
| BEQR-154 | 适用 | HTML L237 项目六态。PRD §7.1/§10、验收包 §7 一致；【已废止】状态门控未回灌。 |
| BEQR-155 | 适用 | HTML L238 商户六态+试运营不管+缓冲期。绑定 §3.1、PRD §7.4、验收包 §7 一致。类型4：`SUBJECT_STATUSES` 五码无归档值、归档走存档；缓冲期闭环文档标未实现。 |
| BEQR-156 | 适用 | HTML L239 合同八态+当地时间。PRD §7.5/验收包 §2A/§7 一致。 |
| BEQR-157 | 适用 | HTML L240 员工五态。PRD §7.6/§10、验收包 §7 一致。 |
| BEQR-158 | 适用 | HTML L241 设备七态。PRD §7.7/§10、验收包 §7 一致。 |
| BEQR-159 | 适用 | HTML L242 权限五态。PRD §7.8/§10、验收包 §7 一致。 |
| BEQR-166 | 适用 | HTML L255 三类合同审批链。PRD §6.5/§12 第18条、验收包 G5 已收敛为同一模型。 |
| BEQR-189 | 适用 | 锁原文管理费方式。本模块未另定管理费规则；PRD 不把服务点单管理费写成治理层已实现。 |

### 3.2 BSM（12）

| ID | 适用 | 证据 |
|---|---|---|
| BSM-004 | 适用 | 锁原文存档/审批删除/占用检查/U盘免审。绑定 §2.5、PRD §15C、集团 PRD 模板 DELETE 恒 409+lifecycle。类型4：`AccessService.deletePositionRoleTemplate` 抛 `ARCHIVE_APPROVAL_REQUIRED`（`ApiException.conflict`→409）。主体档案完整存档链绑定标未落地。 |
| BSM-005 | 适用 | 分步引导新建。验收包/PRD 未改回「一张大表一次填完」为强制相反规则。 |
| BSM-006 | 适用 | 动态二维码。本模块未发明固定核销码规则。 |
| BSM-007 | 适用 | 平台管理员日常权≠最高权限；资金/敏感/删除/终审不自动获得。PRD §7.8.1、集团 PRD 无 wildcard 例外。 |
| BSM-009 | 适用 | 基础管理入口。四份文档正是该子系统的文档段。 |
| BSM-028 | 适用 | 主要功能：项目/商户六类合作/多对多/城市项目公司排除/合同八类变更/七类红线。PRD §2.2/§2A.4/§6 与验收包 §4/§5 对齐。 |
| BSM-029 | 适用 | 和谁有关。PRD §11 与后续系统关系表同向。 |
| BSM-072 | 适用 | 商户必须归入一业态；商户侧授权范围内全开。PRD 开篇 2026-08-01 口径、§7.3/§7.4；验收包业态行废止第三方接入当业态。 |
| BSM-074 | 适用 | 商户不能看平台全局。集团 PRD `requirePlatformScope()` 拦商户；绑定/PRD 未提供关闭隔离的后台项。 |
| BSM-114 | 适用 | **现行 HTML** L1865 私人手机为「查看考勤」；L1445 打卡从领取工作手机开始。PRD §5/§6.9 第8条为查看考勤。锁摘录表仍写「考勤打卡」——现行 HTML 已改，属锁旧摘录/M009 观察，不恢复旧句、不申请改 HTML。 |
| BSM-115 | 适用 | 基础管理连所有业务。PRD §11、验收包边界与之同向。 |
| BSM-120 | 适用 | 已拍板规则。PRD §12 已确认条承接集团视图/单一功能权限/开关/留痕等。锁摘录页脚「2026-09-08」；**现行 HTML** 注释 V0.14 与 footer L2003 为 2026-09-10。页脚日期漂移属 M009 观察，不申请改 HTML。 |

### 3.3 GEN（18）

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 | 适用 | 四文件各有 HTML 上位；废止框未当新规则；未为理论场景加闸。 |
| GEN-002 | 适用 | 文档声明的当前入口用类型4核对：绑定五条 API 在 `PlatformController` 存在；不存在 `/active`、`/{id}/usages`（对该文件 rg 零命中）；集团 PRD 所列 access/analytics 路径在对应 Controller 存在；DELETE 模板恒 conflict。 |
| GEN-003 | 不适用（本模块无测试文件） | 四文件皆文档。不把缺测试写成已测，也不为文档造假测试。 |
| GEN-004 | 适用 | 文档未把未校验输入写成已安全。绑定创建校验商户/三主体存在（源码 `requireActiveMerchant`/`requireBusinessEntity`）。 |
| GEN-005 | 适用 | 查看 `entity.binding.view`、管理 `entity.binding.manage`（V005 种子 high+requires_approval true）；集团总览 `analytics.group.view`+`requirePlatformScope`。文档未把平台 scope 写成项目隔离已完成。 |
| GEN-006 | 适用 | 绑定唯一：`uq_business_entity_bindings_active_merchant`（V010 L15-18）；文档写出。完整权责切换事务闭环标差距。 |
| GEN-007 | 适用 | 绑定创建对未结束 inactive 亦拒重复（`ACTIVE_BINDING_EXISTS`）；无自动到日启用调度（scheduler rg 零命中），与「不表示系统会自动启用」一致。 |
| GEN-008 | 适用 | OpenAPI `entity_role` 仍六旧值（`openapi-p0.yaml` L29208-29214）vs 服务三值——文档登记为契约缺口 M013-009→M392，不当活闭集。空间字典 vs OpenAPI 快照 PRD §6.2.1 已写。grant 三类服务 vs DB 四类 CHECK 已写。 |
| GEN-009 | 适用 | 绑定错误码 `BINDING_PERIOD_ENDED` / `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` / `BINDING_NOT_EFFECTIVE` 与 `PlatformService.changeBusinessEntityBinding` 分支一致。summary 覆盖政策→`QUERY_POLICY_OVERRIDE_FORBIDDEN`（`AccessService.grantSummary` L1044-1048）。 |
| GEN-010 | 适用 | 未要求改 HTML；敏感数据/导出/暗纹在 PRD §15A 承接 PGM-043。同意凭据标三 HTML 零命中、不升必填。 |
| GEN-011 | 适用 | 绑定/授权文档写留痕事件名与权限码；未把无审计的关闭闸写成仍存在（`grant_approval.enabled` 仅注释残留，`AccessService` L79）。 |
| GEN-012 | 适用 | 文档无无界批处理发明。分页 API 标明真分页。 |
| GEN-013 | 适用 | 引用迁移号可在 `backend/src/main/resources/db/migration/` 找到：V002 绑定表、V005 主体表（文件名 `V005__p0_platform_master_data.sql`）、V010、V115、V169 述、V170、V217、V384、V394、V410。 |
| GEN-014 | 适用 | 文档不执行迁移；类型4确认 V410 CHECK 三角色、V394 `analytics.group.view` requires_approval TRUE、V170 `request_type` 含四值、V384 merchant_type 含 media。 |
| GEN-015 | 部分适用 | 无本模块 UI 实现。PRD/验收包界面文案为中文产品需求；私人手机六项与现行 PGM L741 中文「查看考勤」一致。 |
| GEN-016 | 适用 | 版本头与最近变更日志：绑定 V0.11/2026-09-14；集团 V1.02；验收包 V0.05；PRD V0.05。打开文件不再出现互斥双版本（M013-018 已修）。当前实现句与源码对齐见 §4。历史 changelog 行保留当时文字（如 2026-06-07 单级确认），不回灌现行规则。 |
| GEN-017 | 适用 | 本席新问题按冲突→细化→技术性→添加。本轮无新缺陷项。跨模块缺口保持登记，不发明补齐。 |
| GEN-018 | 适用 | 结论落到 HTML 原文或可复现命令；不以历史席位意见为据。 |

### 3.4 PGM（33）

| ID | 适用 | 证据 |
|---|---|---|
| PGM-002 | 适用 | 治理五步。PRD §2.2 顺序固定；注明与实现闸口链分层不冲突。 |
| PGM-003 | 适用 | 模块入口。本模块文档覆盖综合体/商户/权限等入口的产品定义。 |
| PGM-004 | 适用 | 当前一个综合体、预留集团。四文件均带 project 归属要求；集团下钻已承接、隔离未闭环已写。 |
| PGM-005 | 适用 | 空间点位。PRD §6.2/§7.2；字典活校验 vs OpenAPI 快照已写（M013-004）。 |
| PGM-006 | 适用 | 商户和主体。绑定设计+PRD §7.4。 |
| PGM-007 | 适用 | 合同卡流程。PRD §6.5、验收包合同准入。 |
| PGM-008 | 适用 | 员工和手机。PRD §6.9 两条注册路径+查看考勤；打卡在工作手机（BSM L1445）。 |
| PGM-009 | 适用 | 单一功能授权。PRD §12 第10–14条、集团 PRD 角色装配。 |
| PGM-010 | 适用 | 十一端开关卡片。PRD/验收包名单与 BEQR-150 一致。 |
| PGM-011 | 适用 | 设备管理。PRD §6.8/§7.7。 |
| PGM-012 | 适用 | 商户侧边界。PRD/验收包/BSM-072。 |
| PGM-013 | 适用 | 开放接口。PRD 非范围+第三方未定先做自有接口（§12/PGM-066 已拍板）。 |
| PGM-014 | 适用 | 留痕复核。PRD §6.11/§7.9。 |
| PGM-015 | 适用 | 楼宇运营入口。PRD §12A.4 保安团队由城市项目公司管理。 |
| PGM-016 | 适用 | 应急与隐私。PRD §15B/§15A。 |
| PGM-017 | 适用 | 带综合体项目归属、未来集团视图。PRD §12 第15条：聚合已承接、项目级过滤待对齐。 |
| PGM-018 | 适用 | 多对多协作。绑定 §1、PRD §5、V410 删父链。 |
| PGM-019 | 适用 | 各主体职责+酒吧股权牌照场地资金分记。绑定 §1 末段、PRD §12C。 |
| PGM-020 | 适用 | 集团下钻按职位模板、数据范围无后台项。PRD §6.10、集团 PRD §4。 |
| PGM-021 | 适用 | 避免多综合体数据混。所有对象带 project 归属为强制；汇总过滤缺口诚实。 |
| PGM-026 | 适用 | 四类主体唯一绑定+财税三权+不追溯。绑定全文与 PRD §7.4/§12 第19条。完整审批仍标差距。 |
| PGM-027 | 适用 | 商户侧全开、不能看全局。同上。 |
| PGM-031 | 适用 | 经销商治理。PRD §6.5.4/§7.5.2、验收包 §2A。 |
| PGM-035 | 适用 | 提成进工资单月结、不许现金现抽。验收包 §8、PRD 边界。 |
| PGM-040 | 适用 | 单一功能权限点。PRD §12 第10条。 |
| PGM-041 | 适用 | 权限规则/角色装配两支/五类高危/税费四审。集团 PRD §2–§3、PRD §12 14A/14B。`finance.tax_*` 权限码仍缺、V217 个税档与 SupervisorReview `finance.tax_rule.adjust` 已登记待对齐。 |
| PGM-042 | 适用 | 职级规则。PRD §6.10 第8条。 |
| PGM-043 | 适用 | 外包/审计/导出脱敏。PRD §4.1 标代码待落地；§15A 脱敏闭集。不把待落地写成已上线。 |
| PGM-044 | 适用 | 隐私桌台/挂单/经销分权。PRD 未相反；BEQR-074 同向。 |
| PGM-048 | 适用 | 商户侧按类型开放表。验收包商户管理页、PRD 边界。 |
| PGM-060 | 适用 | 会员/营销/生物信息。PRD 未在治理层发明相反会员规则。 |
| PGM-065 | 适用 | 治理连接各域。PRD §11。 |
| PGM-066 | 适用 | 已拍板集团视图/商户侧/单一功能/开关/全中文等。PRD §12 已确认+后续细化「无方向性悬空」。 |

---

## 4. 文档「当前实现」声称 ↔ 源码/迁移/入口（类型 4，非实跑）

### 4.1 绑定设计

| 声称（文件:行） | 核对 |
|---|---|
| V0.11 / 2026-09-14（L1–L3） | 文内一致，无第二当前版本 |
| `BUSINESS_ENTITY_ROLES` 三值（L45） | `PlatformService.java:62` `platform/project_company/management_company` |
| V410 删父链+三角色（L39） | `V410__m010_…sql` L9 DROP `parent_entity_id`；L18-19 CHECK 三值 |
| OpenAPI 仍六旧值（L56） | `openapi-p0.yaml:29208-29214` operation/contract/settlement/invoice/payment/other。M013-009 |
| `status` 无 DB CHECK（L49） | `V005__p0_platform_master_data.sql` 有 status 列、无 chk_be_status |
| 绑定表 V002+V010、唯一索引（L61,L78） | `V002` CREATE `business_entity_bindings`；`V010:15-18` `uq_business_entity_bindings_active_merchant` WHERE status=active |
| 未来 `effective_from`→inactive 且不提前同步（L80,L117） | `createBusinessEntityBinding` L778-797：`effectiveFrom.isAfter(today)` 则 inactive；仅 `if (effective) syncMerchantEntities` |
| `ACTIVE_BINDING_EXISTS`（L117） | `requireUnfinishedBindingSlot` L957-959 |
| 替换三主体→`BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` | `requireUnchangedFinancialSubjects` + change 路径 L831-833 |
| change 只接受 active/inactive/suspended+原因；期间守卫（L118） | `changeBusinessEntityBinding` L818-837：`BINDING_PERIOD_ENDED` / `BINDING_NOT_EFFECTIVE` |
| 无 `/active`、`/{id}/usages`（L120） | `PlatformController.java` 对 usages 与 `/active` 零命中 |
| 无自动到日启用 | 无 scheduler 命中 syncMerchantEntities |
| `entity.binding.view` / `manage`（L126-127） | Controller L46/L54/L252/L261；V005 种子 manage=high+requires_approval true |
| 完整审批/usages/存档删除未落地 | 与「当前 API」表及差距段一致，未写成已完成 |

`business_entities` 表仍无 `project_id` 列（V005 建表列集）。after tree 另有 `business_entity_projects`（V427）及 `POST /business-entities/{entity_id}/projects`、创建时 `associateBusinessEntityProject`。此为 M009 拓扑实现，列级「当前表没有 project_id」仍真；见 §6 观察，不升本模块新需求。

### 4.2 集团治理 PRD

| 声称 | 核对 |
|---|---|
| V1.02 变更记录（L115） | 与标题 V1.02 一致 |
| DB CHECK 四类、服务三类（L23,L70） | `V170:67-68` 含 `role_permission`；`normalizeGrantRequestType` L2046 只接受 user_role/user_position/temporary_permission |
| DELETE 模板恒 409 `ARCHIVE_APPROVAL_REQUIRED`（L44） | `deletePositionRoleTemplate` L481-482 + `ApiException.conflict`→HTTP 409 |
| lifecycle 五动作（L45） | `AccessController:192-201` |
| `grant_approval.enabled` 已移除、生产恒开（L61） | 仅注释（AccessService L79），无配置开关命中 |
| summary 阈值不可覆盖（L77） | `grantSummary` L1046-1048：阈值非空或 windowDays≠政策值 → `QUERY_POLICY_OVERRIDE_FORBIDDEN`；兼容 window_days 仅等于后台值 |
| 列表 `?window_days=`（L81） | `listGrantRequests` 用 `policy.windowDays().checked(windowDays)`（越界 `GRANT_POLICY_OUT_OF_BOUNDS`），与 summary 禁覆盖是两条入口 |
| `analytics.group.view` + `requirePlatformScope`（L14,L95,L110） | `AnalyticsController:122-124` |
| 未按项目级调用者过滤（L14,L95） | `AnalyticsService.buildGroupOverview` L782-798 遍历全部 `projectIds`，无 actor 项目收敛 |
| `groupBy=project\|entity`（L29,L96） | Controller 入参 `groupBy`；非法校验在服务 `normalizeGroupBy`（文档称 VALIDATION_FAILED） |
| V394 审批属性闭合（L110） | `V394:16-18` UPDATE IN 列表含 `analytics.group.view` |
| `staff.tax_bracket.manage` ≠ 第5类税费码（L69） | V217 种子；全库无 `finance.tax_*` permissions 行；有 `SupervisorReviewService.TAX_RULE_ADJUST_ACTION = finance.tax_rule.adjust` |
| 岗位普通立即/高危待批（L38） | 文档已写；不把模板含高危当整单免审 |

### 4.3 验收包

| 声称 | 核对 |
|---|---|
| 标题 V0.05（L1）与 changelog 2026-09-14 升 V0.05（L200） | 一致 |
| 十一端、项目六态、商户六态、合同八态 | 与 BEQR 及现行 HTML 一致 |
| G7：审批属性已闭合、项目范围隔离待对齐（L181） | 与集团 PRD §4、V394、AnalyticsService 未过滤一致 |
| 历史 10 端/单级确认等在补录与 changelog | 标【已废止/不得回灌】或作为当时记录，不回灌 |

### 4.4 治理 PRD

| 声称 | 核对 |
|---|---|
| 标题 V0.05（L1）与 L970 升 V0.05 | 一致 |
| 私人手机六项含「查看考勤」（L211,L377） | 现行 PGM L741、BSM L930/L1865；打卡 BSM L1445 工作手机 |
| 自营=`self_operated`（L364） | `MERCHANT_COOPERATION_FORMS` 含 `self_operated` |
| 业态六值含演出传媒/V384（L451） | `MERCHANT_BUSINESS_FORMATS` 含 media；`V384__merchant_business_format_media.sql` |
| 空间活校验=字典（L247,L262） | 文档已写与 V184 差异 |
| 员工域 evaluate 已接、四域未接（L643） | StaffApprovalService 非本模块文件；对 Ticket/Bar/Amusement/Settlement `*Service` 的 `blockForAdmission/evaluate` 零命中，与「四域未接」同向 |
| §12 第15条聚合≠过滤（L657） | 与集团 PRD/验收包 G7 一致 |
| applyPermissionTemplate 高危 fail-closed（L654） | `AccessServiceTests`/`AccessService` `GRANT_REQUIRES_APPROVAL` |

---

## 5. 对抗式（grilling 结构，自问自答）

❓ **Q1 是否把 001–018 全否当成功能已完成？**  
➡️ 否。009/010/017 仍是跨模块或观察；绑定审批链、下游引用、存档删除、退出缓冲、税费权限码、集团项目过滤、外包自助门户，文档仍标待对齐。全否只表示本模块修复队列空。

❓ **Q2 私人手机「考勤打卡」会否从锁摘录回灌？**  
➡️ 现行 HTML 已是「查看考勤」。PRD 已改。锁 BSM-114 摘录旧句不作为改回文档的理由。

❓ **Q3 集团 overview 有 `groupBy=project` 是否等于项目隔离？**  
➡️ 否。那是聚合维度。`requirePlatformScope` 只挡商户/外包。`buildGroupOverview` 仍汇总全部 ETL 项目。文档已分开写。第一次正常读 §4 不会把聚合当成过滤。

❓ **Q4 OpenAPI 六值是否应在本模块修？**  
➡️ 否。已登记 M013-009→M392。文档不当活闭集。不申请改 OpenAPI。

❓ **Q5 `role_permission` DB 预留是否业务发明？**  
➡️ 文档明确服务不开放。不把预留写成第四种公示申请类型。

❓ **Q6 未来 inactive 是否被写成已审批草稿？**  
➡️ 否。§4.1「待生效记录不是审批草稿」。

❓ **Q7 版本头是否再分裂？**  
➡️ 绑定 L1=L3=V0.11；集团标题=changelog V1.02；验收包/PRD 标题=最近日志 V0.05。打开即见单一当前版本。

❓ **Q8 历史 changelog「P0 单级确认」是否现行规则？**  
➡️ 否。按当时记录解释；现行 §6.5/§12 第18条为统一多线审批。不把废止史当新规则。

❓ **Q9 创建主体现要求项目关联，§3.1「无 project_id」是否假？**  
➡️ 表 `business_entities` 仍无该列（V005）。关联在 `business_entity_projects`（M009/V427）。M013-001 定稿差距即列级无 project_id。不把 M009 关联表升格为本模块新修项。见观察 OBS-1。

❓ **Q10 理论并发/金额/越权是否要给文档加闸？**  
➡️ 否。文档模块不因此加需求。第一次正常读已诚实的差距不会因未写理论场景而错。

❓ **Q11 验收包「必须通过」是否声称编码完成？**  
➡️ 否。§1 写明不是开发测试/接口/库验收。§10A 有缺口。

对抗构造（文档层）：越权——未把 view 写成含导出；重复绑定——唯一索引+ACTIVE_BINDING_EXISTS；状态跳转——change 期间守卫；跨层枚举——OpenAPI 旧枚举已点名；下线绕过——存档闭环标未落地；文档假完成——实现诚实表存在。未发现新的、第一次正常读就会撞上的未登记假完成。

ponytail-review：废止框与补录是历史载体，不是可删活规则。`Lean already. Ship.`（就本模块四份现行正文相对 HTML 而言，无新的应删活规则。）

---

## 6. 本轮问题

无新的冲突/细化/技术性/添加缺陷项进入修复队列。

观察（不进队列、不申请 HTML、不新增锁）：

- **OBS-1** `docs/business-entity-binding-development-design-v0.1.md:56` 原句：「当前表没有 `project_id`、主体联系电话、证照附件或证照有效期字段。」类型4：V005 建表无该列仍真；after tree 另有 `V427` `business_entity_projects` 与 `PlatformController` `POST /business-entities/{entity_id}/projects`。属 M009 拓扑。列级陈述不假。不要求本模块补写为「项目归属已完成」。
- **OBS-2** 锁 BSM-114 摘录私人手机格仍含「考勤打卡」；现行 `business-system-map-for-operations-v0.1.html:1865` 为「查看考勤」。M009 锁旧摘录观察。
- **OBS-3** 锁 BSM-120 摘录页脚「更新日期：2026-09-08」；现行 HTML L2 与 L2003 为 2026-09-10。行号/页脚漂移。M009 观察。

已有 001–018 保持「否」：15 条已修内容在现行文中仍在；009/010/017 仍按跨模块/观察登记。不重开、不把它们当未修功能已实现。

四分类：本轮零新项，无需走添加人闸。

---

## 7. 覆盖表

| 文件 | 行 | 读法 | 三线 |
|---|---|---|---|
| 绑定设计 V0.11 | 1–153 | 全文 | 第一性 §2.1；顺从 §3 绑定相关 ID；对抗 Q5/Q6/Q7/Q9 |
| 集团 PRD V1.02 | 1–115 | 全文 | §2.2；BEQR-152/PGM-041/GEN-005/008/016；Q3/Q5 |
| 验收包 V0.05 | 1–290（含补录） | 全文 | §2.3；状态/十一端/G7；Q8/Q11 |
| 治理 PRD V0.05 | 1–970 | 全文 | §2.4；PGM-002–021/026/031/035/040–044/066；Q2/Q7/Q8 |

OVERSIZE：否。未抽样宣称 PASS。

---

## 8. 结论

**PASS**

同时满足：四文件全覆盖；84 个映射 ID 均有适用结论与类型 1 或 4 证据；三线均执行；当前实现声称已用只读命令对照源码/迁移/Controller，未冒称实跑；无未答 questions；无本轮未定稿新缺陷；本报告第一行独立实算哈希格式合法。

不把 findings 001–018 全否解释为四类主体绑定、集团隔离、税费权限码或完整审批链已在系统落地。

（CLI 不附机器标记。正文只在本 raw。）

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r3 席乙（Grok）整模块三线全量重审 raw

- 报告版本：V0.02（指定路径已有 V0.01，本次完整报告追加末尾，未覆盖）
- 席位：审计席乙 Grok（grok-4.6 / xhigh）
- 步骤：B5-r3（B3 累计 3/3 后的修复后整模块重审，不是 B4 diff 复审；不因额度降标准）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 正本：`/home/james/complex-management` `main` 工作区
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；映射 84 = BEQR21 / BSM12 / GEN18 / PGM33
- 独立实算命令：`cd /home/james/complex-management && sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- 独立实算结果：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（64 位小写 hex。未抄 sidecar / ledger / 派发期望值 / 同文件 V0.01 行。事后对照 sidecar 同行，仅交叉，不作为取值来源）
- 提示词：`160-audit-prompt.md` V0.17 B1/B5 及三线；runbook V0.02
- 互盲：未读 `M013-b5-r3-self-raw.md` 及其票。同路径 V0.01 仅用于确认须追加及版本 +0.01，不采信其 PASS、不复用其 findings 表。可读旧 B1/B2/B4。未派子代理 / MCP / 第三席；未问人；未改业务 / HTML / 锁 / findings / ledger；未 fetch / commit / push / merge / deploy / 写库；未建脚本 / 日志 / 临时产物
- 证据纪律：只读。文档模块不强制跑测试。未冒称 Java / HTTP / 数据库实跑。可执行声称用类型 1（三份 HTML 逐字）或类型 4（sha256sum / wc / git cat-file / git ls-files / rg / graft / 读源码与迁移）
- 历史已批口径（独立复核后保留，不替代发现）：私人手机查看考勤、打卡仅工作手机；BSM-114 锁旧摘录与 BSM-120 页脚日期为 M009 已登记观察，不恢复旧句、不申请改 HTML、不新增锁；PRD 业态不再维护/配置引用开关，历史日志按当时文字解释；公示 grant 三请求类型与 DB 预留 `role_permission` 分清；绑定未来 `inactive` 不提前同步、完整审批仍缺；项目聚合 ≠ 项目结果隔离；同范围岗位普通立即/高危待批，普通角色授权不免审；summary 参数使用后台政策；M013-017 归 M348/依赖 M032；M013-009 归 M392。findings 001–018 当前全部为「否」≠ 功能全实现

---

## 0. 技能预检（§6，按本环境实核）

本席已读并按收窄口径使用：

| 技能 | 本环境 | 本步用法 |
|---|---|---|
| `using-superpowers` | 已读全文 | 开工技能自检；用户 B5 只读指令优先于技能本体提问/实施闸 |
| `using-arc` | 已读 | 用户任务是 160 审计，不路由 Arc `audit`（该技能要派评审子代理并写 `docs/arc/audits/`，与两席/只写本 raw 冲突） |
| `grilling` | 已读全文 | 对抗式主武器；自问自答，事实自读，不派子代理、不问操作员 |
| `grill-me` | 已读（正文仅“Run a /grilling session”，`disable-model-invocation: true`） | 按意图转发 `grilling`，不另起第三席 |
| `verification-before-completion` | 已读全文 | PASS/FAIL 前用本次命令核哈希/行数/after-tree/源码声称 |
| `codebase-design` | 已读前部 | 本模块接口是四份文档对 HTML 与当前实现的声称；深度=声称必须藏住真实入口/表/状态 |
| `diagnosing-bugs` | 已读前部 | 声称与源码不一致时按读代码→最小只读命令定位；本轮无运行时故障环，不造测试/探针 |
| `ponytail-review` | 已读全文 | 对抗式找可删；废止框已标【已废止/不得回灌】，不另提删 HTML/历史日志 |
| `graft` | 已读全文并调用 | 定位 `BUSINESS_ENTITY_ROLES`、`entity.binding`、`createBusinessEntityBinding`、`group-overview`、`grantSummary` |

已核存在、本步不适用（B5 只读、不修、不提交、不写收口）：`brainstorming`、`writing-plans`、`systematic-debugging`、`test-driven-development`、`tdd`、`requesting-code-review`、`receiving-code-review`、`ponytail`、`resolving-merge-conflicts`（无合并冲突）、`writing-for-agents`（收口文件仅双 PASS 后由席甲写）。

明确不用（提示词已划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`domain-modeling`、`code-review`（mattpocock）、`ponytail-audit/debt/gain`。

缺口（未冒充已调用）：本步未另读 `batch-grill-me` / `ubiquitous-language` / `git-guardrails-claude-code` / `i-have-adhd` 的 SKILL.md 全文（后三者可选；对抗式已用 `grilling`）。Arc `audit` 已读但按用户 160 流程作废其派席/落盘。

---

## 1. 范围、哈希、after tree（类型 4）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
sha256sum docs/business-entity-binding-development-design-v0.1.md \
  docs/group-governance-rebuild-prd-v1.0.md \
  docs/platform-governance-acceptance-pack-v0.2.md \
  docs/platform-governance-prd-v0.1.md
# 21c704562c25af0987d781a91d8269a64caae1fd48dd5461b3561077d9018d65
# 3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5
# 6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603
# c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333
wc -l <同上四文件>
# 153 + 115 + 290 + 970 = 1528
git cat-file -t 393297499c9a4a70630d6eda6396a42642ae526d  → tree
git diff --stat 393297499c9a4a70630d6eda6396a42642ae526d -- <四文件>  → 空
```

工作区四文件 SHA256/行数与派发 after tree 全等；与用户给出的四份 SHA/行数全等。计划冻结有效行 1517（`plan/modules.md` M013）不改；现行 1528 为历次 B3 增行后的工作区事实，不是本席扩范围。

`plan/modules.md` 本模块四文件与本次审读清单一致。判据映射一行 84 个 ID，去重 84。未新增判据。

三份 HTML 只读：BEQR 308 行、BSM 2032 行、PGM 979 行。锁内锚点行抽核：BEQR-063=`html:111` 现文仍为「高危动作（需审批/双证据）」；BEQR-143=`html:226` 现文「四类主体」；BSM-004=`html:566` 现文「配置数据可以存档…」；PGM-018=`html:415` 现文「物理项目与运营公司…」。BSM-114 锁摘录仍写私人手机「考勤打卡」，现行 HTML L1865/L930/PGM L741 为「查看考勤」；BSM-120 锁摘录页脚「2026-09-08」，现行 HTML L2003 为「2026-09-10」。两处按已批口径记 M009 观察，不恢复旧句、不申请改 HTML、不新增锁。

---

## 2. 第一性原理三问

### 2.1 绑定设计 `business-entity-binding-development-design-v0.1.md`（153 行，V0.11）

1. **为何必须存在？** 删掉则 PGM「商户和主体」、PGM-026 四类主体唯一绑定、BEQR-143/144、BSM-004 存档删除在本模块无开发设计载体。有业务上位。
2. **声称行为是否被实际执行验证？** 本席只读对照源码/迁移/Controller，未跑 Java/HTTP/DB。部分声称与现行入口一致（见顺从表）；§3.1/§5 把项目归属写成「当前表没有 project_id」且 API 表不列已存在的关联入口，与现行实现第一次对照即失败（GR-B5R3-01）。
3. **未验证前提：** 「当前数据模型」节被当成完整现行模型；OpenAPI 仅披露了 `entity_role` 旧枚举，同一 schema 的 `status` 契约词表未披露（GR-B5R3-02）。完整主体权责审批链、下游统一引用、退出缓冲——文内已标差距，不把「否」当已实现。

### 2.2 集团治理 PRD `group-governance-rebuild-prd-v1.0.md`（115 行，V1.02）

1. **为何必须存在？** 删掉则 PGM-017/020 集团视图、PGM-041 角色装配两支与五类高危、BEQR-151/152 在本模块无 PRD 载体。
2. **声称是否验证？** `normalizeGrantRequestType` 只接受三类；`role_permission` 不在白名单（L2044–2049）。`grantSummary` 阈值覆盖返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`（L1044–1048）。`GET /group-overview` 校验 `analytics.group.view` + `requirePlatformScope()`，`buildGroupOverview` 不按调用者项目收敛（AnalyticsService L774–831；AuthorizationService L482–498 只拦商户/外包）。V394 将 `analytics.group.view` 置 `requires_approval=true`。与文内诚实差距一致。
3. **未验证前提：** `requirePlatformScope()` ≠ 项目结果隔离（文已写）。`finance.tax_*` 权限码仍无 `permissions` 行，仅有 `SupervisorReviewService.TAX_RULE_ADJUST_ACTION` 与 `staff.tax_bracket.manage`（V217）。对标 `DEFAULT_BENCHMARK_DEVIATION=0.20` 仍在 AnalyticsService L1167；PRD §4 已写「残留数值不得作为业务口径」，不另发明公式。

### 2.3 验收包 `platform-governance-acceptance-pack-v0.2.md`（290 行，V0.05）

1. **为何必须存在？** 删掉则 P0 设计验收与 G7 集团承接在本模块无清单载体。
2. **声称是否验证？** 本文自标「不是接口/数据库验收」。G7 区分审批属性已闭合与项目范围隔离待对齐，与集团 PRD §4、V394 只读对照一致。废止框与补录 `~~~~text` 不回灌。
3. **未验证前提：** 「五件套已补齐」指向 M014 规格/流程/低保真，本模块不把验收包自评写成编码完成。

### 2.4 平台治理 PRD `platform-governance-prd-v0.1.md`（970 行，V0.05）

1. **为何必须存在？** 删掉则 PGM-002 治理顺序、PGM-017–021 综合体集团、PGM-026 商户主体、PGM-040–044 权限、BEQR-150–159 状态闭集在本模块无产品需求载体。
2. **声称是否验证？** §6.9「查看考勤」对齐现行 PGM L741 / BSM L930/L1865；§12B.2 打卡对齐 BSM L1445。§6.2.1 `createSpace` 走 `listActiveSpaceTypeCodes()`（PlatformService L685–688）与 V184 八码种子一致。§12#8 `StaffApprovalService` 仍 `evaluate` 排班/处罚；票务目录无 `AdmissionGuard` 引用。§12#15 项目过滤待对齐与 AnalyticsService 一致。
3. **未验证前提：** 变更日志中 2026-06-07「P0 单级确认」是当时记录，现行 §6.5/§12#18 已改统一审批模型；不把历史日志当新规则。外包自助下属账号仍标「代码待落地」。

---

## 3. 顺从式：84 项逐条

判定：每条都适用本模块文档（映射闭集）。证据类型 1=HTML 原文对照，4=只读命令/源码。无新增 ID。

| ID | 适用 | 本模块证据 | 类型 | 缺陷？ |
|---|---|---|---|---|
| BEQR-063 | 是 | 治理 PRD §12#14A / 集团 PRD §3 列高危业务含沽清类权限码；未改写「临时沽清/长期下架」闭集 | 1 | 否 |
| BEQR-074 | 是 | 治理 PRD / 验收包未发明与「查看/操作/紧急接管分权」相反的规则；细表在 PGM-044 承接 | 1 | 否 |
| BEQR-143 | 是 | 绑定设计 §1 表与原文「经营对象、运营主体、合同主体、结算主体…唯一绑定」一致 | 1 | 否 |
| BEQR-144 | 是 | 绑定设计 §1 规则 2、治理 PRD §5 组织关系与「多对多、非固定隶属链」一致 | 1 | 否 |
| BEQR-145 | 是 | 治理 PRD §5 / 绑定设计未把城市项目公司写成管理酒吧/游乐 | 1 | 否 |
| BEQR-146 | 是 | 治理 PRD 娱乐平台/游乐/传媒/中央餐饮职责段照抄 PGM | 1 | 否 |
| BEQR-147 | 是 | 治理 PRD 未把顶层平台写成自动取得票款；收入表未与 HTML 对打 | 1 | 否 |
| BEQR-148 | 是 | 治理 PRD §6.5.1 / 绑定设计合作六类 +「其他」须指定收费模板 | 1 | 否 |
| BEQR-149 | 是 | 治理 PRD §7.8 / §12#6 M1–M10 上中下 | 1 | 否 |
| BEQR-150 | 是 | 验收包 §6 / 治理 PRD §2.1 十一端含独立经销商门户 | 1 | 否 |
| BEQR-151 | 是 | 治理 PRD §6.10 / 集团 PRD 最终权限公式与拒绝优先 | 1 | 否 |
| BEQR-152 | 是 | 治理 PRD §12#20 / 集团 PRD §3 敏感数据与项目隔离；隔离实现缺口已诚实写 | 1 | 否 |
| BEQR-153 | 是 | 治理 PRD §12#16 / 验收包终端开关优先级与兜底关闭+留痕例外 | 1 | 否 |
| BEQR-154 | 是 | 验收包 §7 / 治理 PRD §10 六态；废止状态门控 | 1 | 否 |
| BEQR-155 | 是 | 验收包 §7 / 绑定设计 §3.1 / 治理 PRD §7.4 六态+试运营不记录+缓冲期；服务 `SUBJECT_STATUSES` 五码+归档走存档轴。OpenAPI `status` 四值未在绑定设计披露 → GR-B5R3-02 | 1+4 | **是（02）** |
| BEQR-156 | 是 | 验收包 §7 / 治理 PRD §7.5 八态+当地时间 | 1 | 否 |
| BEQR-157 | 是 | 验收包 §7 / 治理 PRD §7.6 五态 | 1 | 否 |
| BEQR-158 | 是 | 验收包 §7 / 治理 PRD §7.7 七态 | 1 | 否 |
| BEQR-159 | 是 | 验收包 §7 / 治理 PRD §10 权限五态 | 1 | 否 |
| BEQR-166 | 是 | 治理 PRD §6.5 / 验收包 G5 三类模板+主线平行线+不得录入即生效 | 1 | 否 |
| BEQR-189 | 是 | 本模块未改写服务点单管理费区间；治理 PRD 未发明越界静默截断 | 1 | 否 |
| BSM-004 | 是 | 绑定设计 §2#5 / 治理 PRD §15C 存档先下线、删前占用、U 盘免审；绑定接口未落地完整存档链（文已标差距） | 1+4 | 否（诚实差距） |
| BSM-005 | 是 | 验收包/治理 PRD 未把分步引导改回一张大表；本模块无页面实现文件 | 1 | 否 |
| BSM-006 | 是 | 本模块无核销码实现声称 | 1 | 否 |
| BSM-007 | 是 | 治理 PRD §7.8.1 平台管理员≠最高权限；日常/高危分列 | 1 | 否 |
| BSM-009 | 是 | 四份文档整体即「基础管理」载体 | 1 | 否 |
| BSM-028 | 是 | 绑定设计/治理 PRD 承接多对多、城市项目公司边界、合同变更八类、七类红线 | 1 | 否 |
| BSM-029 | 是 | 治理 PRD §11 与后续系统关系表 | 1 | 否 |
| BSM-072 | 是 | 治理 PRD / 验收包 业态六值+商户侧授权范围；未写成只读小窗 | 1 | 否 |
| BSM-074 | 是 | 同 BSM-072；价格权限与隔离 | 1 | 否 |
| BSM-114 | 是 | 现行 HTML 私人手机「查看考勤」；治理 PRD §6.9 同文。锁摘录仍为「考勤打卡」= M009 观察，不恢复、不申请 HTML | 1 | 否 |
| BSM-115 | 是 | 治理 PRD / 验收包未拆开「基础管理连所有业务」 | 1 | 否 |
| BSM-120 | 是 | 已拍板集团视图/十一端开关/权限颗粒度等，治理 PRD §12 承接。锁页脚日期 ≠ 现行 HTML 日期 = M009 观察 | 1 | 否 |
| GEN-001 | 是 | 四文件均有可验证用途；废止框未当新规则 | 4 | 否 |
| GEN-002 | 是 | 文档模块；入口声称须与源码一致。绑定 §5 当前 API 漏项目关联入口 → GR-B5R3-01 | 4 | **是（01）** |
| GEN-003 | 是 | 本模块无测试文件；不把缺测写成已测 | 4 | 否 |
| GEN-004 | 是 | 文档无把未校验输入写成已校验；绑定创建仍标审批缺口 | 4 | 否 |
| GEN-005 | 是 | 绑定 §6 `entity.binding.view/manage`；集团 PRD 授权/自审分离。V005 manage=`high`+`requires_approval=true` | 4 | 否 |
| GEN-006 | 是 | 绑定唯一约束 `uq_business_entity_bindings_active_merchant`（V010 L15–18）与文一致 | 4 | 否 |
| GEN-007 | 是 | 绑定创建幂等键文内要求；未把未实现自动启用写成已自动 | 4 | 否 |
| GEN-008 | 是 | 文已登记 entity_role OpenAPI 六值≠服务三值（009/M392）。同 schema `status` 仍裂 → GR-B5R3-02 | 4 | **是（02）** |
| GEN-009 | 是 | 绑定 change 错误码与 PlatformService L834–848 一致；未把伪成功写成成功 | 4 | 否 |
| GEN-010 | 是 | 文档无明文秘密；导出/敏感页规则在治理 PRD §15A | 1 | 否 |
| GEN-011 | 是 | 绑定 §6 留痕权限；集团 PRD 申请/审批留痕为实现证据不立法 | 4 | 否 |
| GEN-012 | 是 | 文档模块无无界批处理声称 | 4 | 否 |
| GEN-013 | 是 | 未把本机状态写成可复现构建 | 4 | 否 |
| GEN-014 | 是 | 绑定列 V002/V005/V010/V318/V354/V410；漏 V427 项目关联表 → 并入 GR-B5R3-01 | 4 | **是（01）** |
| GEN-015 | 是 | 中文界面原则在治理 PRD §2A.2；本模块无 UI 文件 | 1 | 否 |
| GEN-016 | 是 | 路径/版本/完成声明须对应当前源。§3.1「当前表没有 project_id」在 V427 `business_entity_projects` 与 `POST .../business-entities/{entity_id}/projects` 已存在后仍当缺口 → GR-B5R3-01 | 4 | **是（01）** |
| GEN-017 | 是 | 本轮新项按冲突→细化→技术性→添加；01/02 为细化下的工程缺陷 | 4 | 否 |
| GEN-018 | 是 | 本报告结论落到 HTML 原文或只读命令；不引用席位意见当依据 | 4 | 否 |
| PGM-002 | 是 | 治理 PRD §2.2 五步顺序 | 1 | 否 |
| PGM-003 | 是 | 验收包模块表 / 治理 PRD 范围覆盖入口卡片主题 | 1 | 否 |
| PGM-004 | 是 | 治理 PRD / 验收包 当前一个综合体、预留集团 | 1 | 否 |
| PGM-005 | 是 | 验收包空间九类；铺位号非必过 | 1 | 否 |
| PGM-006 | 是 | 绑定设计 + 治理 PRD 商户/主体 | 1 | 否 |
| PGM-007 | 是 | 治理 PRD §6.5 合同卡住再放行 | 1 | 否 |
| PGM-008 | 是 | 治理 PRD §6.9 人/私人手机/工作手机 | 1 | 否 |
| PGM-009 | 是 | 治理 PRD §6.10 单一功能授权 | 1 | 否 |
| PGM-010 | 是 | 治理 PRD §2.1 / 验收包 十一端开关 | 1 | 否 |
| PGM-011 | 是 | 治理 PRD §7.7 设备 | 1 | 否 |
| PGM-012 | 是 | 验收包 / 治理 PRD 商户侧全开放+不看全局 | 1 | 否 |
| PGM-013 | 是 | 治理 PRD 非范围：先自有接口 | 1 | 否 |
| PGM-014 | 是 | 治理 PRD §7.9 留痕 | 1 | 否 |
| PGM-015 | 是 | 治理 PRD 范围含楼宇运营口径，未做成编码完成 | 1 | 否 |
| PGM-016 | 是 | 治理 PRD §15B 破窗 | 1 | 否 |
| PGM-017 | 是 | 四文件均写项目归属。绑定设计把「无 project_id 列」写成现行模型缺口，未记已落地的多对多关联表 → GR-B5R3-01 | 1+4 | **是（01）** |
| PGM-018 | 是 | 绑定设计 §1#2 / 治理 PRD §5 多对多；V410 删 `parent_entity_id` 与文一致 | 1+4 | 否 |
| PGM-019 | 是 | 治理 PRD 主体职责表 + 酒吧股权牌照资金分记 | 1 | 否 |
| PGM-020 | 是 | 集团 PRD + 验收包 G7 集团下钻；聚合≠隔离已分开写 | 1+4 | 否 |
| PGM-021 | 是 | 同 PGM-017 业务意义，无「已混在一起」的完成声称 | 1 | 否 |
| PGM-026 | 是 | 绑定设计 §2 / 治理 PRD 四类绑定+权责三类；审批链标未落地 | 1+4 | 否 |
| PGM-027 | 是 | 商户侧边界；未授权看其他商户 | 1 | 否 |
| PGM-031 | 是 | 验收包 §2A / 治理 PRD §6.5.4 经销商链 | 1 | 否 |
| PGM-035 | 是 | 验收包 §8 / 治理 PRD 提成进工资单、禁止现金现抽 | 1 | 否 |
| PGM-040 | 是 | 治理 PRD §12#10 单一功能 | 1 | 否 |
| PGM-041 | 是 | 集团 PRD §2–3 角色装配两支、同范围普通立即/高危待批、跨项目先审；`holdHighRiskPositionPermissions` 存在（AccessService L728 注释/调用链） | 1+4 | 否 |
| PGM-042 | 是 | 职级只绑薪资大范围 | 1 | 否 |
| PGM-043 | 是 | 治理 PRD §4 外包/审计；自助下属标待落地 | 1 | 否 |
| PGM-044 | 是 | 隐私桌台/挂单/经销权限组，治理 PRD 未对打 | 1 | 否 |
| PGM-048 | 是 | 验收包 / 治理 PRD 商户侧表 | 1 | 否 |
| PGM-060 | 是 | 治理 PRD 会员/营销/人脸；未把后台导入写成已允许 | 1 | 否 |
| PGM-065 | 是 | 治理 PRD §11 连接表 | 1 | 否 |
| PGM-066 | 是 | 治理 PRD §12 已拍板清单与现行 PGM 已拍板条同向；集团过滤仍待对齐 | 1 | 否 |

84/84 均有适用记录。有缺陷的冻结 ID：GEN-002/008/014/016、PGM-017、BEQR-155。不新增判据。

---

## 4. 对抗式（grilling 自问，不派子代理）

Q1 私人手机能否打卡？现行 HTML/PRD 为查看考勤；打卡 BSM L1445 + 契约 `staff_work_phone`。锁摘录旧句不回灌。不立 finding。

Q2 把「001–018 全否」读成功能全实现？文内仍有完整审批/下游引用/缓冲期/税费权限码/集团项目过滤缺口。不立新规则，但若文档把缺口写成已闭合则冲突。本次 G7/§4/绑定 §4 仍标差距。

Q3 `requirePlatformScope` 是否等于项目隔离？否。AuthorizationService L482–498 只拒商户/外包；AnalyticsService 汇总全部 project 快照。文已写。M013-017 跨模块，不重开。

Q4 grant 第四类 `role_permission`？DB CHECK 预留，服务 L2046 仅三类。文已分清。

Q5 未来生效绑定是否提前同步商户三主体？`createBusinessEntityBinding` L787–799：`effectiveFrom.isAfter(today)` → `inactive` 且不 `syncMerchantEntities`。与绑定 §4.1/§5 一致。

Q6 change 能否改三主体？只接受 active/inactive/suspended + 原因；生效中停用/暂停 → `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`。与 §4.2 一致。完整权责审批仍缺。

Q7 业态维护开关是否回灌？治理 PRD §6.3 【已废止】。历史日志 2026-06-07 单级确认不回灌。

Q8 验收包补录旧六态/10 端是否现行？在 `~~~~text` 且标【已废止/不得回灌】。

Q9 OpenAPI 是否被当成活闭集？绑定设计已禁止把 entity_role 六值当活闭集。同对象 `status` 四值（draft/active/suspended/archived）对服务五码+归档轴仍未登记 → GR-B5R3-02。

Q10 项目归属是否仍「表上没列就没实现」？否。V427 `business_entity_projects`、创建时 `associateBusinessEntityProject`、`GET` 按 `currentProject()` 过滤、`POST /business-entities/{entity_id}/projects` 均在当前工作区。绑定设计仍写「当前表没有 project_id」为差距 → GR-B5R3-01。第一次按该节实现/验收就会与现行入口对不上。

Q11 为理论场景加锁/加 HTML？否。不申请改三 HTML，不 LOCK-AMEND。

Q12 `DELETE position-role-templates` 是否删除入口？AccessService L481–482 恒 `ARCHIVE_APPROVAL_REQUIRED`。集团 PRD §2 已标。

---

## 5. 当前实现声称对照（只读，非实跑）

| 声称 | 对照 | 结果 |
|---|---|---|
| `BUSINESS_ENTITY_ROLES` = platform / project_company / management_company | PlatformService L62 | 一致 |
| V410 删 `parent_entity_id`、角色三值 CHECK | V410 L9–19 | 一致 |
| OpenAPI `entity_role` 六值 | openapi-p0.yaml L29208–29214 | 一致（009/M392） |
| 绑定 unique active merchant | V010 L15–18 | 一致 |
| POST bindings 未来 inactive 不同步 | PlatformService L787–799 | 一致 |
| POST change 状态/期间/财税守卫 | PlatformService L803–867 | 一致 |
| 无 `/active`、`/usages` | governance 包 rg 无匹配 | 一致 |
| `entity.binding.manage` high + requires_approval | V005 L82 | 一致 |
| grant 服务三类 | AccessService L2044–2049 | 一致 |
| summary 禁覆盖阈值 | AccessService L1044–1048；Controller 仍收参但服务拒绝覆盖 | 一致 |
| group-overview 权限+平台 scope、不按项目过滤 | AnalyticsController L119–125；AnalyticsService L774–831 | 一致（017 仍在） |
| V394 `analytics.group.view` requires_approval | V394 L16–17 | 一致 |
| 无 `finance.tax_*` permissions 行 | rg 仅 SupervisorReview 动作名 + staff.tax_bracket | 一致 |
| 审批闸配置项已撤 | AccessService L79–82 注释+生产恒 true | 一致 |
| **当前表没有 project_id（当成模型缺口）** | V427 `business_entity_projects`；create 必 `associateBusinessEntityProject`；list 按 currentProject；`POST /api/v1/platform/business-entities/{entity_id}/projects` | **不一致 → 01** |
| **当前代码 status 仅「尚无 DB CHECK」** | 无 DB CHECK 为真；服务 `SUBJECT_STATUSES` 五码；OpenAPI status 四值 draft/active/suspended/archived | **契约未披露 → 02** |

`git ls-files backend/src/main/resources/db/migration/V427__m009_project_profiles_and_associations.sql` 有路径（索引已纳）。未执行迁移/测试。

---

## 6. 本次 findings

### GR-B5R3-01

- **位置：** `docs/business-entity-binding-development-design-v0.1.md:57`（§3.1 实现差距第 2 子弹）、`:113–120`（§5 当前 API 表）、`:39`（当前迁移只列到 V410）、`:136`（§7 项目归属验收）
- **原句：** 「当前表没有 `project_id`、主体联系电话、证照附件或证照有效期字段。」「`/api/v1/platform/business-entities` GET 真分页查询主体档案 / POST 创建主体档案」。迁移句只到 V410。
- **正常触发：** 按该设计第 3.1/5 节理解「现行没有项目归属落点」去对接或验收；打开当前 `PlatformService.createBusinessEntity` / `listBusinessEntities` / `PlatformController` L97–104 / `V427`，第一次就会发现创建必写 `business_entity_projects`、列表按 `currentProject()` 过滤、另有 `POST /api/v1/platform/business-entities/{entity_id}/projects`。
- **四分类顺序：** 冲突？否（业务规则仍要项目归属，未与 HTML 对打）。细化？是（PGM-017/018 项目归属的实现细节）。技术性？否。添加？否。**分类=细化。**
- **冻结判据：** GEN-016、GEN-002、GEN-014、PGM-017
- **依据类型：** 4（读 V427、PlatformService L105–136/L91–92/L363–375/L405–408、PlatformController L42–57/L97–104、JdbcPlatformRepository L1224–1264）；1（PGM-017「所有基础对象都要带综合体项目归属」）
- **工程缺陷：** 是。把已落地的多对多项目关联写成「当前表没有 project_id」缺口，且当前 API 表漏实际入口。不是要求补业务规则，只要求文档与现行模型/入口一致。
- **不是：** M013-017（那是 group-overview 结果过滤，已登记 M348/M032）。不是要求改 HTML/锁。

### GR-B5R3-02

- **位置：** `docs/business-entity-binding-development-design-v0.1.md:49`（status 行）、`:57–58`（实现差距：status 未实现完整词表）、`:55`（只登记 OpenAPI `entity_role` 旧六值）
- **原句：** 「`status` \| `VARCHAR(32)` \| 当前代码状态，尚无 DB CHECK」；「OpenAPI `BusinessEntityRequest.entity_role` 枚举仍是旧六值…属契约缺口（M013-009…）」
- **正常触发：** 按 OpenAPI `BusinessEntityRequest.status` 枚举 `draft/active/suspended/archived`（openapi-p0.yaml L29228–29236）生成请求；服务 `requireSubjectStatus` 只接受 `pending_admission/active/suspended/terminated/blacklisted`（PlatformService L54–56/L893–900），`draft`/`archived` 第一次即 400。绑定设计对同一 schema 只披露了 role 裂口。
- **四分类顺序：** 冲突？文档本身未把 draft 写成 HTML 商户状态。细化？是（BEQR-155 词表的契约/服务对照）。技术性？否（状态词表是业务闭集）。添加？否。**分类=细化。**
- **冻结判据：** GEN-008、GEN-016、BEQR-155
- **依据类型：** 4（读 OpenAPI 与 PlatformService）；1（BEQR-155 六态）
- **工程缺陷：** 是。披露口径不完整。OpenAPI 文件本身归既有跨模块边界（同 009→M392），但本模块设计既已引用该 schema，就必须把 status 裂口写清，不得只写「尚无 DB CHECK」。
- **不是：** 要求本模块改 OpenAPI 或发明缓冲期闭环。

已有 001–018 保持「否」：已修内容未回归；009/010/017 不重开、不解释成功能已全实现。

四分类：无「添加」项，不发 2.2 人闸。无 HTML 修改申请。

---

## 7. 覆盖表

| 文件 | 行 | 读法 | 三线 |
|---|---|---|---|
| 绑定设计 V0.11 | 1–153 | 全文 | 第一性 2.1；顺从绑定相关 ID；对抗 Q5/Q6/Q9/Q10 |
| 集团 PRD V1.02 | 1–115 | 全文 | 2.2；BEQR-152/PGM-041/GEN-005/008/016；Q3/Q4/Q12 |
| 验收包 V0.05 | 1–290（含补录） | 全文 | 2.3；状态/十一端/G7；Q8 |
| 治理 PRD V0.05 | 1–970 | 全文 | 2.4；PGM-002–021/026/031/035/040–044/066；Q1/Q2/Q7 |

OVERSIZE：否。未抽样宣称整文件已审。

---

## 8. 结论

**FAIL**

未同时满足单席 PASS：出现未定稿新缺陷 GR-B5R3-01、GR-B5R3-02。四文件全文已覆盖；84 个映射 ID 均有适用结论与类型 1 或 4 证据；三线均执行；实现声称只读对照，未冒称实跑；无未答 questions；本报告第一行独立实算哈希格式合法。B3 3/3 未降标准。不把 001–018 全否解释为功能全实现。不申请改 HTML/锁，不 LOCK-AMEND。

（CLI 不附机器标记。正文只在本 raw。）

