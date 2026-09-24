CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M031 B5-r1 席乙（Grok）修复后整模块全量审计 raw

- 席位：Grok 独立只读审计席乙（第一性原理 / 顺从式 / 对抗式）
- 模型：grok-4.6 / reasoning-effort xhigh
- 阶段：B5-r1（B3 累计 1 后的整模块重审；不只看 diff；不复用 B2/B4 票作结论）
- 报告版本：V0.01（指定路径此前不存在，本次创建）
- 模块：`plan/modules.md` V0.04 M031「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段」；标记 NORMAL；计划有效内容 2930 行（B1 开工口径，现已过期）
- 正本：`/home/james/complex-management` 分支 `main` 主工作区
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`
- origin/main：`508b2e87ca43a0e40f1fa81aaf1561aa9ba7def6`（M030 检查点 PR #471 已合并）
- after tree（B3-r1 冻结）：`f2b1cd10fc7a0fa71faec2e4f78a5f9ed95a4826`
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（未抄 sidecar / ledger / 其它报告）。sidecar 与 ledger 顶部同值，仅作交叉观察
- LOCK-AMEND-COUNT：1（ledger 顶部；本席未改锁）
- 映射 52 项：`M031 → [BEQR-157, BEQR-182…187, BSM-001, BSM-003, BSM-009, BSM-028, BSM-029, BSM-073, BSM-084…088, BSM-114, BSM-115, BSM-120, GEN-001…018, PGM-001, PGM-008, PGM-022, PGM-024, PGM-026, PGM-032…036, PGM-048, PGM-059, PGM-066]`
- findings 输入：V2.99；M031-001 / 002 / 003 修复队列「否（已修，B3-r1）」；当前队列「是」= 0。不把全部否当成模块已全对
- 独立性：未打开、未通读、未采信 `modules/M031-b5-r1-self-raw.md`。仓库路径检索曾命中该文件名（未 `Read` 正文）。读 ledger「D-028 / M031 B5 派发」段时见到席甲封存一行摘要，**不把该摘要当本席证据或结论**。旧 B1（含 B2-r1 追加段）与 B4-r1 可读，只作「B2 已定为只登记」的边界；结论以本席全文重审 + 当场实跑为准
- 未改计划 / findings / ledger / 业务代码 / 三 HTML / 锁；未 fetch / commit / push / merge / deploy；未用 MCP / 子代理 / 第三席；不对任何数据库执行写语句（跑既有测试类除外）；除 D-028 允许的一次性 worktree 外未建脚本、日志或临时审计产物
- `test -f .git/MERGE_HEAD` → **NO_MERGE**
- 本席步骤结论：**PASS**（001、002、003 修复落地、无回归、无新的须入队工程缺陷；B2 只登记项维持；添加 0；无 HTML 修改申请）

---

## 0. 环境实用强制技能（自核，铁律 3）

按 `160-audit-prompt.md` V0.17 §6 在本席环境实读 SKILL.md，不是提一嘴。用户指令禁止子代理 / 第三席 / MCP，优先于 Arc 默认全量派席。B5 禁止修复，故 TDD 落测试、ponytail 修复、receiving-code-review 的 IMPLEMENT 一律不执行。runbook「阶段 B 不创建 worktree」被 ledger「D-028 环境决定」与本轮用户指令覆盖：后端测试只在基于 origin/main 的一次性 worktree 里拷入本模块现行文件后跑。

本席环境技能根：`/home/james/.claude/skills/`、`/home/james/.claude/plugins/`、`/home/james/.grok/bundled/skills/`、`/home/james/.claude/plugins/cache/`。

| 技能 | 本机路径 | 本节点用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读；用户指令优先于技能 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 路由识别为质量审查；**用户禁子代理 / 禁改业务**，不跑 Arc 默认派评审、不写 `docs/arc/audits/` |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。用户已钉死三文件且要求全文读不抽样，未用 `graft ask` 替代全文 Read |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读。本轮 sha256 / psql / worktree add-remove / Maven 均前台完成，未挂 monitor、未派 loop 子代理 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗线自问自答；本体「等用户 / 派子代理」作废 |
| grill-me | 同目录 `grill-me/SKILL.md`（disable-model-invocation；正文只写 Run a /grilling session） | 已读头；并入 grilling，不另开会话 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | AccessService 深、Controller 浅门面、Repository 接口缝；删除测试：删三文件则权限写口与定岗计算消失 |
| diagnosing-bugs / systematic-debugging | 已读 | 只追根因与复现；B5 禁止修复、不加诊断桩。本轮反馈环 = worktree 三测试类 |
| tdd / test-driven-development | 已读 | 用来判断 001/002/003 断言能否因目标行为变坏而红；本席不写测试 |
| verification-before-completion | 已读 | PASS 前当场：独立 sha256、after tree blob、origin/main worktree 13＋54＋39、psql 只 SELECT |
| receiving-code-review | 已读 | 禁止表演性同意；本轮无对方 raw 可收（未读本次 self-raw） |
| requesting-code-review | 存在，B4 用 | B5 不适用（禁止派第三席） |
| writing-for-agents | 已读 | 本 raw 按下游席可消耗的步骤+证据写 |
| resolving-merge-conflicts | 已读 | `test -f .git/MERGE_HEAD` → NO_MERGE |
| git-guardrails-claude-code | `.../misc/git-guardrails-claude-code/SKILL.md` | 可选存在；只读审计不装 hook，记「存在但不适用」 |
| ponytail / ponytail-review | 已读 | B5 不修；review 口径扫可删项，见 §8 |
| brainstorming / writing-plans | 存在 | 阶段 A 用，B5 不适用 |
| i-have-adhd | marketplace / cache `skills/i-have-adhd/SKILL.md` | 存在；本席正文只落 raw；CLI 不回带结论或机器标记（适配器优先于该技能的「正文先写下一步」） |
| audit-context-building | Trail of Bits plugin `skills/audit-context-building/SKILL.md` | 要求派 function-analyzer 子代理：与「只允许两席」冲突，**不执行**，记缺口 |
| batch-grill-me | 本机无独立 SKILL.md | **缺口**；已并入 grilling 自问自答，不冒充调用 |
| ubiquitous-language | 本机 skill 目录无 | **缺口** |
| dispatching-parallel-agents / subagent-driven-development / research / using-git-worktrees | 存在 | 提示词划掉。一次性 `git worktree add --detach` 是 D-028 允许的只读核，不是 using-git-worktrees 技能本体；核完已 remove |

一次性 worktree：`git worktree add --detach /home/james/wt-m031-b5r1-grok-ro origin/main`（HEAD `508b2e87`）。拷入主工作区现行 3 个实现文件 + `AccessServiceTests.java` + `AccessControllerTests.java` + `/home/james/audit-tmp/m031_M006SecurityRegressionTests.publish.java`（覆盖 worktree 内 `M006SecurityRegressionTests.java`）。后端测试只导出三个 `COMPLEX_DATABASE_*`（URL 库名改为 `complex_test`），**未** `source /etc/complex/complex.env`。首次 Maven 因会话 `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`（该树无 `javac`）报 `release version 21 not supported`，属环境误指，**不是业务缺陷**；重开同一路径 worktree 并设 `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64` 后编译测试成功。`git worktree remove --force`；`test ! -d /home/james/wt-m031-b5r1-grok-ro` 成立。主工作区三文件 SHA 核后未变。

---

## 1. 模块覆盖（全文实读，行数/SHA 实算）

命令：`wc -l` + `sha256sum` + `git hash-object` + `git ls-tree f2b1cd10 -- <path>`。覆盖 **3/3 全文逐行，不抽样**。

| 路径 | 物理行 | 文件版本 | SHA256 | 工作区 git blob | after tree blob | origin/main blob |
|---|---:|---|---|---|---|---|
| `backend/src/main/java/com/complex/platform/access/AccessController.java` | 332 | V0.05 | `fbdb4667cf1a0811adae1e668dc299b42a07a82ba5abda879cc098bca0c60146` | `5524ed405227e0778aebc89257ed9995eff2b9d9` | 同左 | `ed1a0596c068fc75385fb96cf15ac7635ed8514c` |
| `backend/src/main/java/com/complex/platform/access/AccessRepository.java` | 298 | V0.09 | `6fc11453fee5aefbfe97bb270d4f6f88a294101daaab88c62abcf768338b6cca` | `a5978929f641d488aba54475873511261648ef85` | 同左 | `4f65efa058a74015dd9def01bdeda0c31fa26db3` |
| `backend/src/main/java/com/complex/platform/access/AccessService.java` | 2393 | V0.21 | `4556c02cf35206e8e5032855a0e9c6d8915beb3c43f2ae6f77fb1ce615dfeb0d` | `de7e6c4fc0bdc813b4bb59867bc7b5016603d32f` | 同左 | `be6c75f0df01ffbfcbb8835ed13bda82fc54c84e` |
| 合计 | **3023** | | | | | |

计划「有效内容 2930」是 B1 开工时口径（当时 AccessService V0.20 / 2361 行）。B3-r1 后实算 3023。三文件工作区 blob 与 after tree `f2b1cd10` 同路径 blob **逐字节一致**。

只读追证（非本模块文件，不扩大范围）：`JdbcAccessRepository.java` 的 `findPositionRoleTemplate` / `savePositionRoleTemplate` / `savePositionTemplateLifecycle`；`PositionRoleTemplate.withLifecycle`；`IdempotencyService.runAndComplete`；`V169` 部分唯一索引；`complex_test` 上 `pg_indexes`。AuthorizationService / OpenAPI / 测试类只作行为证据，不把别的模块缺陷入本模块队列。

相关三 HTML（只读，铁律 4 未改；本席 `sha256sum`：enum `4396bb71…`、业务地图 `eebc5b41…`、治理地图 `a7fe8c01…`）：

- 业务地图 `BSM-003` / 对照 `BSM-004`：「想恢复点"取消存档"即可」；存档隐藏、删除走最高权限审批、删前查占用
- 业务地图 `BSM-028`：「管理员工、岗位、班次、职级、权限」；红线⑤同一笔操作重复提交不重复生效
- 治理地图 `:655`：「最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝」
- 治理地图 `:658`：「保存岗位…系统立即按变更后的岗位权限模板重算有效权限」；跨项目变更须先审批；既有高危不得因处于岗位模板内而免除授出前审批
- 治理地图 `PGM-066`：「员工权限按单一功能最小颗粒度配置」；「所有有过程的业务动作都必须留痕」
- 枚举 `BEQR-157` 员工状态闭集；`BSM-120`「职级可按 M1-M10，并可分上中下；职级只给薪资大范围，具体薪资仍需单独填写」

---

## 2. 第一性原理三问（每文件）

### 2.1 AccessController.java（L1–L332）

1. **为什么必须存在？** 删除则三 HTML 里「管理员工、岗位、职级、权限」（BSM-028）与「单一功能权限」（PGM-066）没有 HTTP 写/读入口。`/api/v1/access` 把门禁码绑到服务方法。答得上 → 有业务上位。
2. **声称的行为是否实际执行？** 是。worktree `AccessControllerTests` **13** 条全绿（未认证、缺权限、缺幂等键、临时授权缺失效时间、员工覆盖门禁、职级列表/分配、self 有效权限、按人查看有效权限）。类型 2。
3. **未验证就当真的前提？** 控制器不重复实现业务规则，假定 Service 做组织闸与审批。本席已全文读 Service。6 个路径不在 openapi-p0.yaml → 已登记 XM-M031-C，不改判。

### 2.2 AccessRepository.java（L1–L298）

1. **为什么必须存在？** 删除则权限模板/角色/岗位映射/定岗/授权申请没有持久化缝。接口写明 `Null = platform admin (all templates)`（L19–28）是平台管理员全量列表的契约。答得上。
2. **声称的行为是否实际执行？** 默认方法在内存测试里跑通；Jdbc 覆盖只读追证：`findPositionRoleTemplate` 要求 `status='active' AND archived_at IS NULL AND deleted_at IS NULL`，组织优先、全局兜底。类型 4：`psql` 核 `uq_position_role_templates_active_scope … WHERE status='active'`。
3. **未验证前提？** Jdbc 未发布 `requireProjectScopeId(null)` 与本接口 Null＝全量冲突 → XM-M031-A，本模块检查点不带 Jdbc。默认 `findPositionRoleTemplate` 只滤 `status=active`、不滤 `archived_at`（L112–121）；生产走 Jdbc 覆盖。

### 2.3 AccessService.java（L1–L2393，V0.21）

1. **为什么必须存在？** 删除则定岗适配、岗位模板生命周期、授权申请/复核、范围变更、有效权限计算、职级薪资写入全部消失，BSM-028 / 治理地图 `:655–658` / PGM-066 做不到。
2. **声称的行为是否实际执行？** 是。worktree `AccessServiceTests` **54** 全绿（含 001 存档→恢复→可定岗、001 替代映射 409、002 重算两分支、003 两写口失败留痕）+ `M006SecurityRegressionTests` 发布变体 **39** 全绿（含删除后 restore 断言 `status=active`、状态机二次 archive/request_delete）。类型 2。
3. **未验证前提？** ① 带 `@Transactional` 的失败留痕真库丢失 → XM-M031-B，本席未重跑探针、不改判。② 保存映射 `organizationId` 恒 `null`（L2124）→ B2 只登记。③ restore 故意不走私有 `savePositionRoleTemplate`，故不触发重算——B2 实施边界，见 §7。

---

## 3. 顺从式：52 项逐项（不新增判据）

判定：适用 = 本三文件有对应实现面，核是否与锚点冲突/遗漏；无本面 = 锚点正文不落在这三文件，记录理由，不发明需求。

| ID | 适用/无本面 | 本席核对 |
|---|---|---|
| BEQR-157 | 无本面 | 员工状态闭集「待入职、在职、试岗、停岗、离职」。本文件账号状态是 `active/invited/disabled`（`requireAccountStatus` L1260–1265）。人事状态属员工档案模块。 |
| BEQR-182 | 无本面 | 考勤日结果。三文件无考勤。 |
| BEQR-183 | 无本面 | 打卡方式。无。 |
| BEQR-184 | 无本面 | 请假类型。无。 |
| BEQR-185 | 无本面 | 请假单状态。无。 |
| BEQR-186 | 适用（写入缝） | 「具体薪资仍需单独填写」的下位：`assignStaffLevel` / 定岗写入 `baseSalaryAmountCent`、币种仅 CNY、非负（L1318–1325、L1872–1878）。不实现薪资单公式/绩效档。无冲突。 |
| BEQR-187 | 无本面 | 提成发放进工资单。本文件不发薪。 |
| BSM-001 | 适用（弱） | 「谁能操作」。Controller+Service 权限门与组织闸。无另发明经营域。 |
| BSM-003 | 适用 | 「权限分岗」+ 配置可存档可删的下位实现（生命周期）。001 把 restore 做成可再定岗，与「取消存档即可」同向。BSM-004 原文作对照、不扩锁。 |
| BSM-009 | 适用 | 「管理…员工、设备、权限」的权限子系统入口。 |
| BSM-028 | 适用 | 「管理员工、岗位、班次、职级、权限」；红线⑤由 `IdempotencyService.execute` 覆盖写口。红线④跨组织/跨商户 `CROSS_ORG_FORBIDDEN` / `CROSS_MERCHANT_FORBIDDEN`。 |
| BSM-029 | 适用 | 「用户权限要知道…职级和审批状态」：职级列表/分配、授权申请 pending/applied。 |
| BSM-073 | 适用（弱） | 商户在授权范围内操作员工功能：`listUsersByMerchant`、商户 actor 不能跨商户。 |
| BSM-084 | 无本面 | 员工手机 App。无前端。 |
| BSM-085 | 适用（注册闸） | `STAFF_REGISTRATION_REQUIRED`（L1203–1206）挡住非外包账号直建。注册路径本身不在本三文件。 |
| BSM-086 | 无本面 | 排班考勤请假。 |
| BSM-087 | 无本面（展示） | 工资条展示不在此；薪资字段写入见 BEQR-186。 |
| BSM-088 | 无本面 | 提成/陪场禁止。无商品逻辑。 |
| BSM-114 | 适用 | 不同入口只做该做的：各端点 `requirePermission`；self 有效权限仅登录。 |
| BSM-115 | 适用（弱） | 「基础管理连所有业务」的权限底座。 |
| BSM-120 | 适用 | 「职级可按 M1-M10…具体薪资仍需单独填写」：`listStaffLevels` + `assignStaffLevel` 金额另填。 |
| GEN-001 | 适用 | 三文件均有用途。`slug()` L2345–2350 零调用 → B2 已登记死分支，不改判。 |
| GEN-002 | 适用 | 主路径与错误路径有测试。001 恢复后可定岗、002 重算、003 失败再抛，worktree 全绿。 |
| GEN-003 | 适用 | 001/002/003 断言能因实现变坏而红（B3 已记录 RED；本席 B5 跑 GREEN 54/54）。不把「测试通过」单独当正确性证明，与代码对照。 |
| GEN-004 | 适用 | `requireText` / `requireKnownPermissions` / 职级存在性 / 币种 / 账号类型闭集。 |
| GEN-005 | 适用 | 服务端 `requirePermission` + `requireActorCanGrant` / `requireGrantApprovalIfNeeded`；默认拒绝。 |
| GEN-006 | 适用 | 写口 `@Transactional`；restore 两步同在幂等 action 内；部分唯一索引防双 active。 |
| GEN-007 | 适用 | 写口 `requireIdempotencyKey` + `idempotencyService.execute`。409 在写前，不进两步。 |
| GEN-008 | 适用 | 状态 `active/inactive` 与表 CHECK 一致。6 个路径缺 OpenAPI → XM-M031-C。 |
| GEN-009 | 适用 | 失败 `recordFailure` 再抛；lifecycle / grant-policy 已补。真库丢失属 XM-M031-B。 |
| GEN-010 | 适用 | 不授超出自身权限；受益人/申请人不能自审。密码只经 provisioner。 |
| GEN-011 | 适用 | 写口成功/失败事件。003 补两缺口。读口无查看事件 → B2 只登记。 |
| GEN-012 | 适用 | `listUserIdsAssignedToPosition` 默认 N+1 → B2 只登记。`listPendingGrantRequests` 全表过滤同登记。 |
| GEN-013 | 无本面 | 无构建脚本。测试环境三变量由本席导出，未依赖未声明本机 source。 |
| GEN-014 | 无本面 | 无迁移文件。只读核 `uq_position_role_templates_active_scope` 部分唯一（仅 `status='active'`），与 restore 替代映射检查相容。 |
| GEN-015 | 无本面 | 无 UI。错误文案中文（PGM-001 全中文原则的 API 面）。 |
| GEN-016 | 无本面 | 三文件无完成声明文档。 |
| GEN-017 | 适用 | 本席四分类未把锁外业务发明当技术性留下。添加 0。 |
| GEN-018 | 适用 | 结论落到 HTML 原文、worktree 测试、psql SELECT。 |
| PGM-001 | 适用 | 资格/权限/责任底座；留痕与中文报错。 |
| PGM-008 | 适用（弱） | 员工账号创建闸；手机绑定不在此。 |
| PGM-022 | 无本面 | 空间点位。 |
| PGM-024 | 无本面 | 低消/容量/库位。 |
| PGM-026 | 无本面 | 商户档案/招商。权限引用合同范围不在本三文件展开。 |
| PGM-032 | 适用（弱） | 「审核是否通过、上岗时能做什么」：账号状态 + 定岗派生。 |
| PGM-033 | 适用（注册闸）/ 其余无本面 | 映射锚点是注册/换机/破解，不是权限模板段。注册直建被拒；设备绑定无本面。 |
| PGM-034 | 无本面 | 企微会话。 |
| PGM-035 | 无本面 | 提成发放。 |
| PGM-036 | 无本面 | 设备台账。 |
| PGM-048 | 适用 | 商户不能看平台全局和其他商户：`listUsersByMerchant`、`CROSS_MERCHANT_FORBIDDEN`。 |
| PGM-059 | 适用（弱） | 本文件不展示明文手机号；加密存放属别的模块。创建用户不把密码写入审计元数据（只记 `credential_provisioned` 布尔，L1268–1277）。 |
| PGM-066 | 适用 | 单一功能权限点、角色+员工覆盖、过程留痕、幂等、权限点预置（`permissionExists`）。界面全中文的 API 错误码配套中文消息。 |

52/52 已记录。无锁外主张。

---

## 4. grilling 对抗（自问自答；不等操作员、不派子代理）

焦点按派发：restore 两步顺序与事务、替代映射边界、恢复已软删除、幂等重放、失败留痕外壳、其余门控/提权/审批链/范围变更/状态机二次。

**Q1 两步写顺序在 Jdbc 下会不会把 status 写回 inactive？**
原句：`JdbcAccessRepository.savePositionTemplateLifecycle` `SET status = 'inactive'`；`savePositionRoleTemplate` `ON CONFLICT DO UPDATE` 写 `status`、**不写** `archived_at`/`deleted_at`。
Service restore（`AccessService.java:523–564`）：先 `savePositionTemplateLifecycle(withLifecycle(null,null,null,null,now))` 清存档/删除列（被 SQL 写成 inactive），再构造 `status="active"` 的同一 id 走 `repository.savePositionRoleTemplate`。
顺序不可反：后调 lifecycle 会盖掉 upsert 的 active。
事务：外层 `@Transactional`（L485）+ `idempotencyService.execute` 闭包内两步；`IdempotencyService.runAndComplete` L167–172 有 `TransactionTemplate` 时 **REQUIRED 并入**。单测无模板直跑。
证据：类型 4 读 Jdbc SQL；类型 2 `restoringAnArchivedPositionMappingMakesItAssignableAgain` 断言 `stored.status()==active` 且 `archivedAt()==null` 后定岗成功。

**Q2 组织行与全局行并存时会不会误 409？**
Jdbc `findPositionRoleTemplate`：`organization_id = :org OR organization_id IS NULL`，精确组织优先。
Service 额外 `.filter(active -> Objects.equals(active.organizationId(), current.organizationId()))`（L519–521）。
全局 active + 恢复组织行：`equals(null, "orgA")` 为假 → **不 409**，与部分唯一索引 `(COALESCE(organization_id,''), position_code, staff_level_id) WHERE status='active'` 相容（本席 `psql` 核 indexdef 含该 WHERE）。
本模块写口 `organizationId` 恒 null（L2124），组织行主要来自种子/别模块；过滤器仍是 B2 定稿边界。不升缺陷。

**Q3 双方组织皆空？**
`Objects.equals(null,null)` 为真 → 409。
首次正常触发：存档映射 A 后 `createPositionRoleTemplate` 再建同岗位同职级（写口也是 null）→ restore A。
证据：`restoringIsRejectedWhileAnActiveReplacementMappingExists`（AccessServiceTests L1529–1547）409 `POSITION_TEMPLATE_ACTIVE_EXISTS`，原行仍 `inactive` 且 `archivedAt!=null`。零写。

**Q4 恢复已软删除映射？**
`restore` 只要求 `archivedAt!=null`；`DELETE_REVIEW_PENDING` 仅 `deleteReviewId!=null && deletedAt==null`。批准删除后 `deletedAt` 已设，restore 走两步把删除列清空并 upsert active。
HTML「数据始终留在库里可查、可审计、可恢复」。M006 发布变体 L600–602：approve_delete 后 restore，断言 `deletedAt==null` 且 `status==active`。39 条里该用例绿。不升缺陷。

**Q5 幂等键重放？**
`execute("positionTemplateLifecycle:"+id, key, …)` 包住查找、409、两步写、成功事件。成功重放走幂等完成记录，不第二次写。失败走 `markRetryableFailure` 在闭包外（L174–176），可重试。
409 发生在任何 save 之前。未为「理论并发双 restore」加需求：部分唯一索引是最后一道；要两件不巧才撞。

**Q6 两个失败留痕外壳改没有改成功路径？异常是否原样上抛？**
```
changePositionTemplateLifecycle: try { return changePositionTemplateLifecycleAudited(...) }
  catch (RuntimeException e) { recordFailure(...lifecycle_failed...); throw e; }
updateGrantPolicy: 同构 change_failed
```
成功路径仍只跑 Audited 方法；无额外状态改写。`throw exception` 原样上抛。
证据：001/M006 成功 restore 仍返回 active；003 `assertThatThrownBy` 仍拿到 `POSITION_ROLE_TEMPLATE_NOT_FOUND` / `VALIDATION_FAILED`，且失败事件名出现。类型 2。

**Q7 真库失败事件会不会随回滚丢？**
会。catch 在 `@Transactional` 方法体内，与 XM-M031-B 同构。B2 已登记转 M092。本席不改判、不摘注解（摘掉会拆 001 两步原子性）。

**Q8 其余端点门控 / 提权 / 审批链 / 范围变更 / 状态机第二次？**
- 门控：Controller 各写口 `requirePermission`；lifecycle 的 approve/reject 双码（L196–201）；staff 调 lifecycle → `PLATFORM_ADMIN_REQUIRED`（Service L500–502，master 放行）。
- 提权：`requireActorCanGrant` / `requireActorCanGrantImmediate`；模板 apply 高危 `GRANT_REQUIRES_APPROVAL`。
- 审批链：`reviewGrant` 非末节点返回原 pending（L995–996）；受益人/申请人自审禁止（L973–978）；指纹变化 `GRANT_SNAPSHOT_CHANGED`。
- 范围变更：旧派生撤销 + 新范围整单进链（L608–727）；同范围拒绝 `SCOPE_UNCHANGED`。
- 状态机二次：M006 发布变体 restore 后再 archive、最高权限本人 `request_delete` 直接软删且不增审批单（L603–607），39 绿。已 archive 再 archive → `INVALID_STATE`。DELETE 入口恒 `ARCHIVE_APPROVAL_REQUIRED`（L481–483）。

**Q9 停用→存档→恢复后在岗人员派生会不会自动回来？**
不会。restore 走仓储 upsert，**不**走私有 `savePositionRoleTemplate`（L2092 起会 `recomputeUsersOnPositionTemplate`）。B2 明文禁止走私有方法。首次正常路径是「启用态存档→恢复」：存档本身不收回派生，恢复后定岗查找恢复即可（001 测试）。停用是另一条 PUT，已由 002 覆盖收回。把两条拼成「停用后再存档再恢复必须重算」是新需求，铁律 6 不修。只记观察，不入队。

**Q10 四分类有没有添加项？**
001 细化（BSM-003 伞下；BSM-004 对照不扩锁）。002 细化（治理地图重算句 / GEN-003）。003 技术性（无业务发明：事件名沿同文件范式，GEN-011/PGM-001）。无添加、无 questions、无 HTML 申请。

---

## 5. 已定稿 ID 复核（不把「否」当全对）

### M031-001（细化，队列否/已修）

原缺陷：`withLifecycle` 恒 `"inactive"` + Jdbc lifecycle SQL 恒 inactive → 取消存档后不能定岗。
现码 `AccessService.java:515–564`：
- 先查替代 active；命中且 `organizationId` 相同 → 409「同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复」，零写
- 否则 lifecycle 清标记，再 `new PositionRoleTemplate(..., "active", ...)` + `repository.savePositionRoleTemplate`
- 返回启用态对象，不是 inactive 的 withLifecycle 结果
证据：AccessServiceTests L1507–1526、L1529–1547；M006 发布变体 L600–602 `status().isEqualTo("active")`。worktree 54+39 绿。
分类维持细化。**修复落地，无回归。**

### M031-002（细化，队列否/已修）

原缺陷：`recomputeUsersOnPositionTemplate` 两分支零测试。
现码 L2149–2172：active → 逐人 `assignUserPositionDirect(..., true)` 保留薪资；非 active → `replacePositionGrants(..., null, ...)`。
证据：`savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants` L1570–1616：改映射后 `positionRoles==role_recompute_new` 且薪资 500000 不变；停用后 key 仍在、值为 null。54 绿。本席 B5 未再做「注释掉重算」变异（那是 B3 证据）；当前断言绑定派生角色字面量，实现若不再调用重算会红。**测试面落地。**

### M031-003（技术性，队列否/已修）

原缺陷：lifecycle / grant-policy 无 `recordFailure`。
现码 L485–494、L1943–1952：`RuntimeException` → 指定失败事件名 → `throw exception`。
证据：`positionTemplateLifecycleAndGrantPolicyFailuresAreAudited` L1621–1631 含 `access.position_role_template.lifecycle_failed` 与 `access.grant_policy.change_failed`。成功路径测试未受影响。真库留得住仍依赖 XM-M031-B。**范式补齐，不改判跨模块项。**

---

## 6. B2 只登记项（无新证据，不改判）

| 项 | 维持 |
|---|---|
| XM-M031-A → M032 | `requireProjectScopeId` 在 origin/main Jdbc **0** 处；本接口 L19–28 Null＝全量；违约在未发布 Jdbc（M032） |
| XM-M031-B → M092 | 失败留痕与业务同事务；本席未重跑探针，代码结构未变 |
| XM-M031-C → M392 | Controller 6 路径仍不在本模块 OpenAPI 文件 |
| `organizationId` 恒 null | L2124 `null` 字面量仍在 |
| 读口无查看事件 | `grantSummary` / `effectivePermissionsForUser` 仍无独立查看失败事件 |
| N+1 | `listUserIdsAssignedToPosition` 默认方法仍按人查岗 |
| 字面量经办人 `"access-api"` | 多处仍在；审计另有 actor |
| `slug()` 死分支 | L2345 定义，无调用点 |
| `wildcard()` 短路 | `requireActorCanGrant` L812；构造侧恒 false 属 AuthorizationService |
| 保存映射重新挂起已获批高危 | `assignUserPositionDirect` 仍对 highRisk 调 `holdHighRiskPositionPermissions`；字面合 HTML「不得免除授出前审批」 |
| 管理员直发日常临时授权 | `createTemporaryGrant` 对非高危可直发；HTML 未禁止 |

---

## 7. 观察（不入队）

**O-1** 停用（PUT inactive，002 已收回派生）后再存档再恢复，restore 不重算、派生不会自动回来。这是 B2「禁止走私有 save」的直接后果，不是 001 回归。首次正常「启用态存档→恢复→可定岗」已绿。不为该拼路径加需求。

**O-2** 内存假仓 `findPositionRoleTemplate`（AccessServiceTests L1865–1881）只滤 `status=active`，不滤 `archived_at`/`deleted_at`（Jdbc 滤）。001 测试同时断言 `archivedAt()==null`，且必须先 lifecycle 再 upsert，测得到 Jdbc 语义缺口。不升 GEN-003。

**O-3** 本席 B5 实跑前一次 Maven 因错误 `JAVA_HOME`（无 javac）失败；改 Temurin 21 后 106 绿。记环境，不是 finding。

---

## 8. ponytail-review（只列可删，B5 不修）

- `AccessService.java:2345` `slug()`：delete: 零调用。B2 已登记，不新开 ID。
- 失败留痕外壳：不是可删——003 要的最小范式，与 `executeTemplateWrite` 同构。
- restore 两步：相对改 M032 `withLifecycle`/Jdbc SQL 更长，但是 **本模块最小且 Jdbc 成立** 的改法（B2 已核）。不标 yagni。

`Lean already. Ship.`（就本模块修复后的可删面；死代码一项已登记）

---

## 9. 问题清单与四分类

本席临时编号：**无须入队项。** GR 项 0。

添加 0。冲突 0。未答 questions 0。无 HTML 修改申请。无锁外主张。

---

## 10. 实跑证据（类型 2 / 4）

### 10.1 判据锁

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

### 10.2 complex_test 只 SELECT

```
SELECT indexdef FROM pg_indexes WHERE indexname='uq_position_role_templates_active_scope';
-- CREATE UNIQUE INDEX uq_position_role_templates_active_scope ON public.position_role_templates
-- USING btree (COALESCE(organization_id, ''::character varying), position_code, staff_level_id)
-- WHERE ((status)::text = 'active'::text)

SELECT status, (organization_id IS NULL) AS org_null, count(*) FROM position_role_templates GROUP BY 1,2;
-- active | t | 5

SELECT COUNT(*) FILTER (WHERE archived_at IS NOT NULL) AS archived,
       COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS deleted,
       COUNT(*) FILTER (WHERE status='active') AS active FROM position_role_templates;
-- 0 | 0 | 5
```

PSQL_EXIT=0。未 source env；口令从 `COMPLEX_DATABASE_PASSWORD` 行读取不回显。

### 10.3 一次性 worktree 测试

- 基线：`origin/main` `508b2e87`
- 拷入 SHA 与主工作区一致：Controller `fbdb4667…`、Repository `6fc11453…`、Service `4556c02c…`；AccessServiceTests `077ce9e6…`（blob `1c34f772`＝after tree）；AccessControllerTests `466b7800…`（blob `1b4ea7d7`＝after tree）；M006 发布变体 `c2d8b8a6…`
- `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`
- `COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test`（另两变量导出，未打印口令）
- `mvn --batch-mode test -Dtest=AccessControllerTests,AccessServiceTests,M006SecurityRegressionTests`
- **MVN_EXIT=0**
  - AccessControllerTests tests=13 errors=0 skipped=0 failures=0
  - AccessServiceTests tests=54 errors=0 skipped=0 failures=0
  - M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
  - Results: Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
- worktree 已 `git worktree remove --force`；主工作区 AccessService SHA 仍 `4556c02c…`

主工作区直接跑 AccessServiceTests 会因别的模块未发布改动报错（D-028 / M031 B1 环境事实）；本席未把主工作区红当本模块缺陷。

---

## 11. 结论

三文件全文覆盖（332+298+2393=3023）。52 项全部适用或无本面已记录。三线已跑。001/002/003 修复在 Jdbc 语义与测试上落地，成功路径与状态机二次、门控、提权、审批链、范围变更未见回归。无新的须入队工程缺陷。B2 只登记项维持。添加 0。

**本席 B5-r1：PASS。**

本票不是合取结论。合取须席甲本次完整 raw 落盘后按 §4 双 PASS 且 B5 后四值全等。
