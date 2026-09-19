CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M013 B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 只读独立审计席乙（grok-4.6/xhigh）
计划：160-audit-prompt.md V0.17（本席读 B4 / §5；不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/M013-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B2 双 PASS 票替代本次实际 diff 审计。席甲自审票不得覆盖本席；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

未读取：任何 `M013-b4-r2-self-raw.md`、其发现/票。可读：V0.17 B4/§5、findings.md V2.25 顶端 B2-r2 定稿、旧 B1/B2 定稿文字（仅作队列边界，不作本次 diff 证据）。正本 `/home/james/complex-management` 分支 `main`。

本批队列：011–016＝是；001–010、017＝否。双方 B2 V0.04 已 PASS，不替代本次。B3 本批已完成，累计 2/3。文档类：可复现命令+输出，不造执行测试；只读源码/HTML 为实现证据，未跑 Java/HTTP/数据库。

---

## 1. 输入、独立锁实算、tree 与 diff 指纹

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
cd /home/james/complex-management
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。文件 889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 正文一行同值。ledger 顶部 `CRITERIA-LOCK-SHA256:` 同值（对照，非本席抄写来源）。`LOCK-AMEND-COUNT: 1`。criteria-lock 版本仍 V0.11。两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（本 diff 未改锁）。

| 项 | 值 |
|---|---|
| 模块 | M013 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段（`plan/modules.md` 4 文件；映射 84 条判据，去重 84） |
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身 |
| 正式队列「是」（6） | M013-011、012、013、014、015、016 |
| 正式队列「否」 | M013-001–010、M013-017（017 跨模块 XM→M348；AuthorizationService 依赖 M032） |
| B3 | 已执行 2 次（本轮为第 2 次），模块仍 IN_PROGRESS；上限 3 |
| before（对象类型 tree） | `75941c7d111b1a88313417197a581a48d917915c` |
| after（对象类型 tree） | `ad9c27c34fcf3032cf42e330821e14f26d5e510e` |
| `git cat-file -t` | 两者均为 `tree`；size 各 954 |
| 完整 diff 命令 | `git diff --binary 75941c7d111b1a88313417197a581a48d917915c ad9c27c34fcf3032cf42e330821e14f26d5e510e`（无路径过滤） |
| 实际差异 | 33677 bytes；SHA256 `6b70b54e324d51022c3fbc988d7172548f1114907fb710111f9b4214631c2055`；4 文件；+24 / −20；unified 180 行 |
| `git diff --check` | exit 0 |
| 两树路径清单 | before 3771 / after 3771；`comm -3` 空（无增删路径） |
| HEAD（对照，非本 diff 基线） | `e456ba96c84185c8d39f18f548011daac48a6466`；`HEAD^{tree}`=`ee1833d2672e5372ef7b056db4f92d48e6de875a` ≠ after。本席只审派发 before/after，不以 HEAD 或事后提交替换 |
| 三 HTML 两树 blob | BEQR `86a717930f9303d4f131ee2864478baa14450e6d` / BSM `7002bb80afb0ef6060e01a440daa361a99bcf040` / PGM `8eca4668cfd42a4c6628ae5a66d60ab8e5c8ebb3`，完全相同 |
| 锁/sidecar 两树 blob | 相同：lock `8db3fc02ce8a6dc998863a9784e7e88af511b960` / sidecar `41f575026b693af74d2a36e6faf86d260ef6f6ca` |
| findings/ledger 两树 blob | 相同（本 diff 未改）：findings `7749bf794550998eda200e74abd82d3c262500c4`，ledger `6fd26850b1710b6fcb9fd412152c9bf9bd20de10` |
| 源码/迁移/OpenAPI | `git diff --name-only` 仅下列 4 md；backend/ 与三份业务 HTML 不在 diff |
| 测试面 | 文档类；本席只读对照测试源，未执行 Maven/Java |
| 合并状态 | 无 MERGE_HEAD / rebase / cherry-pick |
| questions/M013* | 无 |
| 添加项 / HTML 申请 | 0 / 0 |

`git diff --stat --binary`：

```
 ...iness-entity-binding-development-design-v0.1.md | 12 ++++++------
 docs/group-governance-rebuild-prd-v1.0.md          | 22 ++++++++++++----------
 docs/platform-governance-acceptance-pack-v0.2.md   |  5 +++--
 docs/platform-governance-prd-v0.1.md               |  5 +++--
 4 files changed, 24 insertions(+), 20 deletions(-)
```

`git diff --numstat`：`6 6` / `12 10` / `3 2` / `3 2`。`git diff --name-status` 四处 `M`。

after 树四文件 vs 派发期望（`git cat-file -p <after>:<path> | sha256sum` + `wc -l`）：

| 文件 | 行 | SHA256 | 与派发 |
|---|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 153 | `04e4111bc47ad2c41d4f1188996f7e56b8a2dd875b1b0128b4468d7efcf8e674` | 全等 |
| docs/group-governance-rebuild-prd-v1.0.md | 115 | `3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5` | 全等 |
| docs/platform-governance-acceptance-pack-v0.2.md | 290 | `6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603` | 全等 |
| docs/platform-governance-prd-v0.1.md | 970 | `c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333` | 全等 |

工作区四文件 SHA256/行数与 after 树全等。git blob：

| 文件 | before blob | after blob |
|---|---|---|
| business-entity-binding-development-design-v0.1.md | `ad294981d1fca52b35f7986adb1505aafcecd333` | `c55e3120acd874376f66e6ade1735ebc04677d7d` |
| group-governance-rebuild-prd-v1.0.md | `e1dfa2e9434ac96a40249eba901997bb59da627c` | `784858240ec30f575849794d2e627c3006fa2210` |
| platform-governance-acceptance-pack-v0.2.md | `dbdfc24501028d0e1cbbf130d33c461c467b8bfc` | `c5b1f63c2b23d69e5e2b0afcdf53bec9114d3939` |
| platform-governance-prd-v0.1.md | `29d77349ae3eb2d49e950be7f4d17c274e771da4` | `ead26b294687c2c03feb386141958a0a2f7efe31` |

M013 映射 84 判据（本席从 `criteria-lock.md` M013 行拆分计数，未新造）：BEQR 21 + BSM 12 + GEN 18 + PGM 33 = 84。本轮只用定稿已引用的 BEQR-143/152、BSM-007、PGM-026/041、GEN-002/005/008/016/018。不 LOCK-AMEND。四分类不重开。`plan/modules.md` `^## M[0-9][0-9][0-9] —` 计数 471。

---

## 2. 强制技能预检与按意图执行（铁律 3）

环境实核（列目录 / 读 SKILL.md），缺则记缺口，不冒充已调用。

| 技能 | 存在 | 本轮用法 |
|---|---|---|
| using-superpowers | 是 | 开工技能自检 |
| using-arc | 是 | 路由：用户指令优先于 Arc 全量审计/评审工作流。Arc audit/review 本体要派子代理、写 `docs/arc/audits/`、向人提问 → **不执行本体**；只借「尊重已定范围、不偷加需求」 |
| graft | 是 | `graft ask`/`graft grep` 定位绑定/总览/授权实现后再读 `file:line` |
| grilling / grill-me | 是 | 对抗式自问结构；**不等操作员、不派子代理**（提示词收窄） |
| batch-grill-me | **缺口** | 技能目录无此名 |
| ubiquitous-language | **缺口** | 技能目录无此名 |
| requesting-code-review | 是 | B4＝本席即审；**不派** reviewer 子代理、不用 `HEAD~1`、不即刻修 |
| receiving-code-review | 是 | 本席是外审不是收票方；只借「先核实再下票、禁止表演性同意」；**不 IMPLEMENT** |
| verification-before-completion | 是 | PASS 前实跑 sha256/wc/`git diff --binary`/`git cat-file`/`git diff --check` |
| systematic-debugging | 是 | 只借读错误→追数据流；**不加** diagnostic instrumentation（无 ID 改动） |
| diagnosing-bugs | 是 | 只读追链；文档类无红循环命令，不造 harness |
| test-driven-development / tdd | 是 | 文档类**不强制**落测试；未造假测试 |
| codebase-design | 是 | 删除测试：本 diff 无新模块接口；无加深/抽缝 |
| ponytail / ponytail-review | 是 | B3 已完成；本席审「可删的多余行」。结论：Lean already. net: -0 |
| i-have-adhd | 是 | 编号、复述「模块 M013 / 共 471，B3 第 2 次」；本 raw 承载正文 |
| writing-for-agents | 是 | 本 raw 给席甲/操作员读：完成条件可检查；不另写 §7 外产物 |
| resolving-merge-conflicts | 是 | 核无进行中 merge/rebase；无冲突可解 |
| git-guardrails-claude-code | 是（可选） | 本 harness 未装 Claude Code hook；**未安装 hook**（会写仓库外/内设置）。等价禁力：本席未 fetch/commit/push/merge/deploy/`reset --hard` |
| brainstorming / writing-plans | 是 | 阶段 A 技能，本轮 B4 **不适用** |
| ~~dispatching-parallel-agents / subagent-driven-development / executing-plans / using-git-worktrees / finishing-a-development-branch / research / ponytail-audit~~ | 划掉或禁第三席 | **未调用** |

未用 MCP。未派任何子代理/第三席。未提问。未改 findings/ledger。未新建脚本或临时日志文件。

---

## 3. 第一性原理三问（2.3）

1. **这段修复为什么必须存在？** 删掉它，B2-r2 定稿 011–016 所对应的 HTML/实现错述会继续把调用方导向错误接入（直接当 active、把 scope 守卫当项目隔离、把日常临时当拒绝、把平台管理员当 wildcard 全权、按 V170 high 判断申请码、按示例传阈值被拒）。上位：BEQR-143/PGM-026（四类主体绑定与财税权责）、BEQR-152/PGM-041（项目隔离与临时权限/岗位装配）、BSM-007（日常功能≠全权限）。答得上 → 有业务上位；分类保持定稿（011/012/013/015/016 细化，014 冲突已按删除错误「wildcard 例外」收窄）。
2. **声称的行为有没有被实际执行验证过？** 文档类不强制跑 Java。证据类型 1（HTML 现文 `sed`/`read`）+ 类型 4（`git diff --binary`、`sha256sum`、`git cat-file`）+ 只读源码对照（非类型 2 可执行产物）。测试源存在但**未执行**，不把测试通过当证据。
3. **哪一条前提没验证就当真？** （a）未跑 JVM，故「运行时 UTC 当天」以源码 `LocalDate.now(ZoneOffset.UTC)` 为只读证据，不是本次时钟输出；（b）V394 之后 23 个迁移文件名检索 `access.grant_request.apply` 零命中，是仓库内迁移文本检索，不是数据库当前行；（c）HEAD 晚于 after 树，不以 HEAD 四文件旧 blob 当修复结果。

不接受的理由未使用：未写「B2 已 PASS 所以 B4 通过」「测试绿所以对」「上一轮自己这么判」。

---

## 4. 逐 ID 修复内容审计（先假定对 → 再构造反例）

### M013-011 — 主体绑定创建与状态变更现状

定稿 5 处 + 该文件标题版本。对照 `PlatformService` 756–801 / 803–867 / 945–960、`JdbcPlatformRepository` 591–602。

| 声称 | 只读源码 | 判定 |
|---|---|---|
| 未来 `effective_from` 写 `inactive` 且不提前同步 | `effective = !effectiveFrom.isAfter(today)`；`effective ? "active" : "inactive"`；仅 `if (effective) syncMerchantEntities` | 成立 |
| 已到日期写 `active` 并同步 | 同上；`effectiveFrom == null` 视为 today | 成立 |
| UTC 当天 | `LocalDate.now(ZoneOffset.UTC)` | 成立 |
| 已有 active 或 inactive 且 `effective_to` 为空 → `ACTIVE_BINDING_EXISTS` | `hasUnfinishedBusinessEntityBinding` SQL：`status='active' OR (status='inactive' AND effective_to IS NULL)` | 成立 |
| 替换既有三主体 → `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` | `requireUnchangedFinancialSubjects`：三列皆空才允许；否则冲突 | 成立 |
| 已结束 → `BINDING_PERIOD_ENDED` | `effectiveTo() != null` | 成立 |
| 生效中停用/暂停 → 财税守卫 | `active` 且新状态非 `active` | 成立 |
| 未到生效日 → `BINDING_NOT_EFFECTIVE`（启用或结束） | `effectiveFrom.isAfter(today)` 在写状态前抛出 | 成立 |
| 启用还检查商户、三主体、槽位、财税 | `requireActiveMerchant` / `requireBusinessEntity`×3 / `requireUnfinishedBindingSlot` / `requireUnchangedFinancialSubjects` | 成立 |
| 待生效不是审批草稿；完整审批链仍未实现 | 创建直接 insert；无 draft 状态机 | 成立（未冒称完成） |
| 到期后可调用启用，系统不会自动启用 | `graft grep effectiveFrom` 在 governance 无 Job/Scheduler 调 `create`/`change`；`BINDING_NOT_EFFECTIVE` 仅 change 路径 | 成立 |
| 「见 §5」 | 本文 `## 5. 当前 API` 即该表，含上述错误码 | 内链成立 |

未把实现限制写成 HTML 新规则：仍写「完整主体权责配置审批…仍未实现」。PGM-026 财税须审批在 HTML；本接口拒绝替换是实现守卫，文档标为当前能力/差距，不是新业务立法。

反例：`uq_business_entity_bindings_active_merchant` 只约束 active，服务槽位更宽。正文仍保留唯一约束句，并另写服务拒绝待生效重复——不矛盾。

**011 内容：够。**

### M013-012 — 集团总览项目过滤缺口 vs 审批属性已闭合

三文件同口径。HTML 原文（BEQR-152 L235 / PGM-041 L653）：「所有数据默认按项目隔离…只有项目之上的获授权岗位可以跨项目汇总。」

实现：`AnalyticsController` 119–125 仅 `requirePermission("analytics.group.view")` + `requirePlatformScope()` 再 `groupOverview(groupBy)`。`requirePlatformScope` 482–498：平台管理员桥放行；外包拒；`merchantId` 非空拒；**无 merchantId 则通过，不查 project/organization**。`AnalyticsService.buildGroupOverview` 774–830：读全部 project 快照，无 actor 项目过滤。`AnalyticsServiceTests` 286–300：无 actor 输入返回 `project_1` 与 `project_2`（只读源，未跑）。

修法：业务边界用 HTML「项目之上的获授权岗位」；实现标「待对齐」；「不得把通过该守卫等同于已认定的平台/集团级角色」；审批属性 V394 `requires_approval=true` 单独称闭合。验收包 G7、主 PRD §12#15 同步。017 不在本 diff（正确：后台缺口不在本模块修）。`modules.md` M348 含 `AnalyticsController`/`AnalyticsService`；M032 含 `AuthorizationService`。

未把 `requirePlatformScope` 立成 HTML 角色。未把隔离写成已完成。

未改行 `Controller 同时校验…商户用户因 merchant scope 被拒绝` 仍真，与「项目级无商户调用者不过滤」并存，不互相覆盖。

**012 内容：够。三文档一致。**

### M013-013 — 日常临时权限仍审批

`AccessService.applyGrant` 889–901：`dailyTemporary = approvalRequired.isEmpty() && REQUEST_TEMPORARY_PERMISSION`；非日常且无审批码才 `GRANT_APPROVAL_NOT_REQUIRED`；日常临时 `permission_grant_l1`。测试源 `dailyTemporaryPermissionApplicationEntersSupervisorChain` 断言 pending + l1（未跑）。PGM-041：「员工可申请临时权限，按层级对应主管审批。」

**013 内容：够。**

### M013-014 — 删平台管理员 wildcard 全权限例外；普通立即/高危待批不扩大到普通角色授权

BSM-007：平台管理员拥有**日常功能**；高危逐项授予。`computeEffectivePermissions`：wildcard 参数为真时只把 **daily 集或最高权限全码** 并入，随后 `effective.removeAll(denies)`；返回记录 `wildcard=false`（L206）。平台管理员桥走 daily 并入，不是全权限例外。`savePositionRoleTemplate` 2073–2076 用 `requireActorCanGrantImmediate`（808–812：从持权检查中剔除须审批码）。`assignUserPositionDirect` 1464–1527：普通保存，高危 `holdHighRiskPositionPermissions`。`unassignUserRole` 等仍 `requireActorCanGrant`（378）。显式授予路径 1413 仍 `requireActorCanGrant`。新句「岗位规则不推广为其他授权入口的免审例外」与源码一致，未扩大到普通角色授权。

**014 内容：够。冲突点（wildcard 全权例外）已删。**

### M013-015 — apply 风险档 V394 low，V170 为历史

V170:100 种子 `access.grant_request.apply` `high`。V394:13 `UPDATE … risk_level='low'`；:14 授 `role_staff_self`。V394 之后 23 个迁移文件文本检索该码零命中。新句保留「V170 原 high，V394 已调整」，未把申请日常等同授出免审。

**015 内容：够。**

### M013-016 — summary 禁阈值覆盖

`AccessController` 158–165 仍接收三参数并透传。`grantSummary` 1044–1049：`positionThreshold!=null || userThreshold!=null || (windowDays!=null && windowDays!=policy.windowDays())` → `QUERY_POLICY_OVERRIDE_FORBIDDEN`；阈值/窗从 `grantPolicy()` 读。文档示例改为不传阈值；`window_days` 兼容等于后台值。未放宽覆盖，未新增配置。

**016 内容：够。**

### 001–010 / 017

diff 无这些 ID 的修复行。017 后台未改，文档未把隔离写成已完成。001–008 旧闭合陈述未被本 diff 回写成「未修」。

---

## 5. 文档自洽 / 可照做 / 未立法 / 未假完成

- 四文件标题版本：绑定 V0.10、集团 V1.02、验收包 V0.05、主 PRD V0.05，与 B3 预定一致。
- 三份现有变更记录各追加一行：集团归 012–016；验收包/主 PRD 归 012。绑定无专用日志，未新建章节。
- 集团/验收包/主 PRD 对「审批属性闭合 ≠ 项目隔离完成」同向。
- 实现限制（错误码、UTC、政策参数）写在「当前实现/当前能力」，带「不得立法」既有框的章节未把这些升成 HTML。
- 旧缺口（绑定审批链、项目过滤、税费 finance.tax_* 权限码）仍标待对齐/未实现。

观察（不构成 B4 FAIL，不新分配 ID，不进本轮越界）：绑定 H1 已升 V0.10，文首 `> 版本：V0.09 ｜ 最后更新：2026-09-11` 未改。B2 对绑定写明「无专用日志则不新建章节，仅改标题」。第一次按 §5 API 接入不依赖该 banner。铁律 6：非「第一次正常跑必撞」路径。GEN-016 的权威版本声明以本轮授权递增的标题为准。不把更新 banner 立成锁外新需求。

ponytail-review：无多余抽象/无 ID 顺手优化。`Lean already. net: -0`。

---

## 6. 顺从式（冻结判据）

在 011–016 改写范围内：与 BEQR-143/152、PGM-026/041、BSM-007 现文不冲突；实现声称与只读源码/迁移文本一致（GEN-002/008/016）；授权路径未把日常管理员写成全权（GEN-005/BSM-007）。未启用锁外判据。

---

## 7. grilling 对抗（自问，不等人）

1. **是否用 B2 票代替 diff 审计？** 否。逐块对照 after 树正文与 Java/SQL/迁移。B2 只提供队列边界与拟改字节。
2. **是否把 `requirePlatformScope` 写成 HTML 角色？** 否。新文禁止该等同，并保留 HTML 业务边界与实现待对齐分离。
3. **012 是否把隔离假完成？** 否。三处「尚未按本项目过滤 / 待对齐」。
4. **014 是否把定岗拆单推广到 `requireActorCanGrant` 角色入口？** 否。角色路径仍全量 `requireActorCanGrant`。
5. **016 是否暗示 Controller 已删除参数因而放宽？** 否。文档要求调用不传阈值；源码传入非空阈值仍拒绝。
6. **有无无 ID 行？** 见 §8。无。
7. **有无改三 HTML/锁/ledger/源码？** `git diff --name-only` 仅 4 md；HTML/锁 blob 两树相同。
8. **标题版本与 3 条变更记录能否归因？** 能：各文件版本递增归该文件本批「是」ID；变更记录按 B3「分别归 011、012、013–016」（绑定仅标题→011）。

---

## 8. 逐块 / 逐行归因（§5 闸 3）

每一变更行归 findings.md 已定稿且队列为「是」的 ID。版本行与既有变更记录更新一并列出。

### docs/business-entity-binding-development-design-v0.1.md（+6/−6）

| 行（after） | 变更 | ID |
|---|---|---|
| 1 | 标题 V0.09→V0.10 | M013-011（B3 该文件标题版本） |
| 80 | 创建/槽位/财税/§5 守卫现状 | M013-011 |
| 90 | POST 创建 active/待生效 inactive | M013-011 |
| 99 | change 期间守卫；不自动启用 | M013-011 |
| 117 | POST 绑定错误码表 | M013-011 |
| 118 | POST change 错误码与启用检查 | M013-011 |

### docs/group-governance-rebuild-prd-v1.0.md（+12/−10）

| 行（after） | 变更 | ID |
|---|---|---|
| 1 | 标题 V1.01→V1.02 | M013-012–016（该文件本批全部「是」ID 的版本协议） |
| 14 | 集团下钻业务边界 vs 实现待对齐 | M013-012 |
| 34 | `requireActorCanGrantImmediate`；无 wildcard 全权 | M013-014 |
| 38 | 角色仍 `requireActorCanGrant`；定岗拆普通/高危；不推广 | M013-014 |
| 71 | 日常临时进审批链 | M013-013 |
| 77 | summary 政策窗/阈值，禁覆盖 | M013-016 |
| 83 | summary 端点不带阈值示例 | M013-016 |
| 90 | apply 风险 low；V170 历史 | M013-015 |
| 95 | 项目过滤未做；待后台模块 | M013-012 |
| 110 | 审批属性闭合 ≠ 结果过滤 | M013-012 |
| 115 | 新增变更记录一行 | M013-012–016 |

### docs/platform-governance-acceptance-pack-v0.2.md（+3/−2）

| 行（after） | 变更 | ID |
|---|---|---|
| 1 | 标题 V0.04→V0.05 | M013-012 |
| 181 | G7 审批闭合 / 隔离待对齐 | M013-012 |
| 200 | 变更日志一行 | M013-012 |

### docs/platform-governance-prd-v0.1.md（+3/−2）

| 行（after） | 变更 | ID |
|---|---|---|
| 1 | 标题 V0.04→V0.05 | M013-012 |
| 657 | §12#15 基本聚合 vs 项目过滤 | M013-012 |
| 970 | 变更日志一行 | M013-012 |

无行可归 001–010 或 017。无行无 ID。无测试/源码/HTML/锁行。越界判定：不成立。

---

## 9. 本席临时编号（本轮无新工程缺陷）

无 GR-M013-B4-* 入队项。观察见 §5 banner，队列否，不修。

添加项：0。HTML 修改申请：0。questions：0。

---

## 10. 完整原样 diff 与两个 tree id

before tree：`75941c7d111b1a88313417197a581a48d917915c`  
after tree：`ad9c27c34fcf3032cf42e330821e14f26d5e510e`  

命令（未按路径过滤）：

```
git diff --binary 75941c7d111b1a88313417197a581a48d917915c ad9c27c34fcf3032cf42e330821e14f26d5e510e
```

33677 bytes；SHA256 `6b70b54e324d51022c3fbc988d7172548f1114907fb710111f9b4214631c2055`。

全文原样：

```
diff --git a/docs/business-entity-binding-development-design-v0.1.md b/docs/business-entity-binding-development-design-v0.1.md
index ad294981..c55e3120 100644
--- a/docs/business-entity-binding-development-design-v0.1.md
+++ b/docs/business-entity-binding-development-design-v0.1.md
@@ -1,4 +1,4 @@
-# 四类主体唯一绑定开发设计 V0.09
+# 四类主体唯一绑定开发设计 V0.10
 
 > 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
 > 业务最终解释权：`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。代码、迁移和接口只说明当前实现，不得覆盖三份 HTML。
@@ -77,7 +77,7 @@
 
 唯一约束 `uq_business_entity_bindings_active_merchant` 保证每个商户至多一条 `active` 记录。
 
-当前没有 `project_id`、`binding_no`、`version`、`created_by`、`reviewed_by`、`reviewed_at`，也没有 `business_entity_binding_usages` 表。当前创建接口直接写 `active`，已有 active 时拒绝；状态变更接口只改启用、停用或暂停，尚未实现 HTML 要求的主体权责配置审批、新旧关系切换和下游统一引用闭环。
+当前没有 `project_id`、`binding_no`、`version`、`created_by`、`reviewed_by`、`reviewed_at`，也没有 `business_entity_binding_usages` 表。当前创建按 `effective_from` 相对 UTC 当天判断：未来日期写 `inactive`，不提前同步商户三主体；已到日期写 `active` 并同步。已有生效或未结束待生效绑定时拒绝重复；已有商户三主体不得经此接口替换。状态变更还受生效期间及财税变更守卫限制（见 §5）。完整主体权责配置审批、新旧关系切换和下游统一引用闭环仍未实现。
 
 ## 4. 目标流程与当前差距
 
@@ -87,7 +87,7 @@
 2. 转商户时，经营对象、运营主体、合同主体、结算主体默认可指向同一公司。
 3. 经适用审批生效后，下游合同、结算、权限、看板和第三方接入引用同一绑定。
 
-当前差距：`POST /api/v1/platform/business-entity-bindings` 直接创建 active 记录，没有草稿、审批和复核状态。
+当前差距：`POST /api/v1/platform/business-entity-bindings` 按生效日期创建 `active` 或待生效 `inactive` 记录，只有生效时才同步商户三主体；待生效记录不是审批草稿，尚无完整草稿、审批和复核流程。
 
 ### 4.2 主体或权责调整
 
@@ -96,7 +96,7 @@
 3. 审批后只影响生效时间之后的新业务；历史合同、订单、票据、结算和快照保持原值。
 4. 下游新业务切到新绑定，历史引用继续可追溯。
 
-当前差距：`POST /api/v1/platform/business-entity-bindings/{id}/change` 只改变现有记录状态，不能提交新的三主体组合，也没有上述审批链和历史版本切换。
+当前差距：`POST /api/v1/platform/business-entity-bindings/{id}/change` 只接受状态动作并校验生效期间；生效中绑定不得直接停用或暂停，已结束期间不得重开，未到生效日不得启用或结束。该接口不能提交新的三主体组合，也没有上述审批链和历史版本切换；到达生效日期后可调用启用，不表示系统会自动启用。
 
 ### 4.3 准入与引用
 
@@ -114,8 +114,8 @@
 | `/api/v1/platform/business-entities` | GET | 真分页查询主体档案 |
 | `/api/v1/platform/business-entities` | POST | 创建主体档案 |
 | `/api/v1/platform/business-entity-bindings` | GET | 真分页查询四类主体绑定 |
-| `/api/v1/platform/business-entity-bindings` | POST | 直接创建 active 绑定；已有 active 时返回 `ACTIVE_BINDING_EXISTS` |
-| `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 将状态改为 active、inactive 或 suspended，并填写原因 |
+| `/api/v1/platform/business-entity-bindings` | POST | 未来 `effective_from` 写 `inactive` 且不提前同步商户三主体；已到日期写 `active` 并同步。已有 `active` 或 `inactive` 且 `effective_to` 为空的绑定返回 `ACTIVE_BINDING_EXISTS`；替换商户既有三主体返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` |
+| `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 接受 active、inactive、suspended 并必填原因；已结束期间返回 `BINDING_PERIOD_ENDED`，生效中绑定停用/暂停返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`，未到生效日返回 `BINDING_NOT_EFFECTIVE`。启用还检查商户、三主体、未完成绑定槽位及既有财税主体不被替换 |
 
 当前不存在 `/active?businessObjectId=` 和 `/{id}/usages` 两条接口，也不存在主体完整 CRUD。文档不得调用不存在的端点。
 
diff --git a/docs/group-governance-rebuild-prd-v1.0.md b/docs/group-governance-rebuild-prd-v1.0.md
index e1dfa2e9..78485824 100644
--- a/docs/group-governance-rebuild-prd-v1.0.md
+++ b/docs/group-governance-rebuild-prd-v1.0.md
@@ -1,4 +1,4 @@
-# 集团治理重建 PRD（V1.01·补断点）
+# 集团治理重建 PRD（V1.02·补断点）
 
 > **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。
 
@@ -11,7 +11,7 @@
 - 不重写 RBAC：继续复用 `permissions`、`roles`、`role_permissions`、`user_roles`、`permission_templates`、`permission_template_items`、`permission_template_applications`、`temporary_permission_grants`。
 - 不重写主管复核：高危授权审批复用 `SupervisorReviewService` 与 `supervisor_review_decisions`。敏感动作可要求上一级、上两级或最高权限，这是敏感度要求，不是发起人可选档位，也不等于指定角色档或合同单级。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现代码值 `one_level_up` / `two_levels_up` / `top_authority` 仅为实现证据，不取得业务效力。
 - 本轮新增的是治理闭环，不是新权限模型：平台管理员配置岗位模板，HR/平台按用户职位定岗，系统自动授予角色并套模板；高危权限授出前可被审批闸拦住。
-- 集团下钻与项目公司级隔离按角色分级共存，不冲突：**平台/集团级角色**（`requirePlatformScope()` 放行者）可跨项目公司汇总下钻；**项目公司管理员**只看本项目公司；**商户用户** scope 一律 fail-closed 403。`GET /api/v1/analytics/group-overview` 除校验 `analytics.group.view` 外，还调用 `requirePlatformScope()`，商户 scope 调用 fail-closed（返回 403）。即「集团下钻」只对平台/集团级角色开放，对项目公司/商户仍按其 scope 收敛。
+- 集团下钻的业务边界：只有项目之上的获授权岗位可跨项目汇总，项目公司管理员限本项目，商户用户不得查看集团总览。**当前实现待对齐**：`GET /api/v1/analytics/group-overview` 校验 `analytics.group.view` 和 `requirePlatformScope()`；后者拦截商户及外包范围，但未按项目级调用者过滤汇总。不得把通过该守卫等同于已认定的平台/集团级角色；项目范围隔离尚未闭环，见 §4。
 - 不做：多套审批流引擎、按业务线重造授权模型、把职位等同于权限。职位只驱动默认角色/模板，最终有效权限仍由 RBAC + 临时授权 + 显式拒绝计算。
 
 ## 1. 业务对象与状态枚举（当前实现证据；锚点为枚举 HTML §九平台治理，不是 ENUM §十地产运营）
@@ -31,11 +31,11 @@
 **业务规则**（GOV「角色装配（已确认）」两支与五类高危以 HTML 为准。本节表名、唯一约束、接口取值与回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
 - 平台管理员配置 `position_role_templates`：`position_code + staff_level_id` 绑定一个 `role_id` 与一个 `permission_template`。这对应业务口径“职位/层级对应哪个模板”。
 - 【实现证据/不取得效力】活跃模板唯一约束：`COALESCE(organization_id,'') + position_code + staff_level_id` 在 `status='active'` 下唯一——三份业务 HTML 零命中，不得立法。
-- 创建/更新职位模板必须是平台管理员，并且复用 `requireActorCanGrant`：模板里角色权限和权限模板条目都不能超出操作者自身有效权限，平台管理员 wildcard 例外。
+- 创建/更新职位模板必须是平台管理员，服务使用 `requireActorCanGrantImmediate` 检查模板中会立即生效的普通权限不得超出操作者自身有效权限；须审批的高危权限不要求配置者预先持有，实际授出仍须审批。平台管理员没有 wildcard 全权限例外，显式拒绝继续有效。
 - 给用户定岗 `assignUserPosition` 时，系统按目标用户组织、职位、职级查模板，写三类结果：`staff_level_assignments`、`user_position_assignments`、`user_roles`，并套用 `permission_templates`。
 - 跨组织闸保持原 RBAC 规则：非平台管理员不得给其他组织用户定岗；角色或模板若带组织归属，也必须与目标用户组织一致。
 - `access.role.assign` 独立为高危权限，V169 设为 `risk_level=high`、`requires_approval=true`，避免把“配角色”混在普通 `access.role.manage` 里。
-- 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；角色装配只要包含五类强制高危权限或其他 `requires_approval=true` 权限，就必须被审批闸拦截并走 §3 申请，不受可关闭总开关影响。
+- 直接分配/取消角色继续使用 `requireActorCanGrant` 和跨组织闸；直接授出须审批的高危权限仍被拦截并走 §3 申请。同范围定岗按普通权限与待批高危权限分别处理：普通部分随保存生效，高危部分挂起进入审批链，通过后才释放，不因模板含高危权限而整单拒绝；岗位规则不推广为其他授权入口的免审例外。
 
 **关键端点（实际 Controller）**
 - `GET /api/v1/access/position-role-templates`
@@ -68,31 +68,31 @@
   - **第 5 类 税费规则与税费调整（2026-08-04 对齐平台治理地图/枚举速查表）**：所有税费规则和税费调整均须经过**财务审核、税务审核、法务审核**，再由**顶层平台公司的老板最终确认后生效**；**任一环节驳回均不得生效**；**提交人不得审批自己提交的调整**；全部审批意见和时间必须留痕。**税率、税目不属于普通后台配置项**，不得按普通配置项直接改动。
     - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档权限码 `staff.tax_bracket.manage`（V217，`risk_level=high`、`requires_approval=true`；属薪资个税档，不是本条第 5 类「税费规则与税费调整」）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 复核动作（策略 tax_rule_adjustment_l3，不是 `permissions` 行），但本条 HTML「税费规则与税费调整」在 `permissions` 侧尚无 `finance.tax_*` 权限码落点，**记为实现待对齐（范围收窄为该权限码）**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
 - 申请类型当前服务支持三类：`user_role`、`user_position`、`temporary_permission`。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支。
-- 申请时服务先解析目标授权会授出的权限码，只允许包含 `requires_approval=true` 的授权申请；若没有审批必需权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权。
+- 申请时服务先解析目标权限码。角色/岗位授权申请若不含 `requires_approval=true` 的权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权；临时权限申请无论日常或高危均进入主管审批链，其中仅含日常权限的申请使用当前一级主管策略，不直接授出。
 - 审批链不由发起人选择档位，也不按风险值自动映射成可选档或指定角色。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现把 `critical`/`high`/其他 写成 `permission_grant_l3`/`l2`/`l1` 与 `top_authority`/`two_levels_up`/`one_level_up`，属实现证据，不是业务可选档。
 - 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。
 - 【实现证据/不取得效力】流程：申请创建 `access_grant_requests(status=pending)` 并调用 `SupervisorReviewService.blockForAdmission` 生成 `review_id`；审批端调用 SupervisorReview `approve` 后，批准则应用角色/定岗/临时权限并写 `applied`，驳回写 `rejected`——三份业务 HTML 零命中上述表名、状态流程与服务名，不得立法。
 - 临时权限必须有 `expires_at`；可带 `valid_from`、`single_use`。V170 扩展 `temporary_permission_grants` 的 `status`、`valid_from`、撤销、单次消耗字段；有效权限查询按有效期和消耗状态过滤。
 - 角色装配按治理 HTML「角色装配（已确认）」两支执行。同商户同项目：保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。
-- 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。
+- 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。统计窗口和频繁阈值从后台授权政策读取，查询不得覆盖；兼容的 `window_days` 仅在等于后台值时接受，两个阈值参数只要传入即返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。
 
 **关键端点（实际 Controller）**
 - `POST /api/v1/access/grant-requests`
 - `GET /api/v1/access/grant-requests?window_days=...`
 - `POST /api/v1/access/grant-requests/{id}/review`
-- `GET /api/v1/access/grant-requests/summary?window_days=...&position_threshold=...&user_threshold=...`
+- `GET /api/v1/access/grant-requests/summary`（使用后台授权政策，不传阈值参数）
 - `POST /api/v1/access/temporary-grants`
 - `POST /api/v1/access/temporary-grants/{id}/revoke`
 
 **迁移 / 表 / 权限**
 - 迁移：`V170__access_grant_requests.sql`
 - 表：`access_grant_requests`、`temporary_permission_grants`；批量更新 `permissions.requires_approval`
-- 权限：`access.grant_request.view`（medium）、`access.grant_request.apply`（high）、`access.grant_request.review`（critical、requires approval）、既有 `governance.supervisor_review.approve`
+- 权限：`access.grant_request.view`（medium）、`access.grant_request.apply`（low；V170 原 high，V394 已调整）、`access.grant_request.review`（critical、requires approval）、既有 `governance.supervisor_review.approve`
 - UI：`ADM-ACC-003` 访问治理工作台
 
 ## 4. 集团下钻对标
 **业务规则**（可见范围与平台/集团级角色以 GOV 为准。本节 `groupBy`、缺主体归并与快照回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
-- 集团视图能看什么由职位模板最终授出的权限决定；只有拥有 `analytics.group.view` **且通过 `requirePlatformScope()`（平台/集团级角色）**的职位/角色/临时授权可见集团总览。项目公司管理员即便持有该权限，也因 scope 收敛到本项目、无法跨项目公司下钻；商户用户 fail-closed 403。
+- 集团视图的业务可见范围仍按治理 HTML 的项目隔离与岗位授权执行。**当前实现**只校验 `analytics.group.view` 和 `requirePlatformScope()`，再按 `groupBy` 汇总全部项目快照；无商户归属的项目级调用者通过守卫后，结果没有按其项目收敛。该范围缺口待后台实现模块处理，不能据本 PRD 判为已经隔离。
 - 【实现证据/不取得效力】端点 `GET /api/v1/analytics/group-overview` 接受 `groupBy=project|entity`。`project` 按综合体项目聚合；`entity` 通过 `projects` 关联 `merchants.business_entity_id` 派生，缺主体归 `entity_unassigned`——三份业务 HTML 零命中 `groupBy` 与 `entity_unassigned`，不得立法。
 - Controller 同时校验 `analytics.group.view` 和 `requirePlatformScope()`；即使某商户用户误拿到权限，也会因 merchant scope 被拒绝。
 - 指标目录来自真实 ETL 快照：`revenue.net`、`revenue.ticket`、`revenue.bar`、`traffic.amusement`、`verify.done`、`risk.pending_review`；商户数由当日 merchant ETL 快照去重统计。
@@ -107,7 +107,9 @@
 **迁移 / 表 / 权限**
 - 迁移：权限来自 `V115__analytics_group_overview_permission.sql`；职位授权闭环来自 V169；高危授权审批来自 V170
 - 表：`analytics_business_metric_snapshots`、`projects`、`merchants.business_entity_id`
-- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**实现待对齐（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；§4 的 `requirePlatformScope()` 只收敛可见范围、不替代授权审批。
+- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**审批属性缺口（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；该结论只涉及审批属性；`requirePlatformScope()` 不替代授权审批，也尚未完成本节所述项目级结果过滤。
 - UI：`ADM-ANA-002` 集团总览；可通过 `ADM-ACC-003` 的职位模板授出或收回 `analytics.group.view`
 
 > 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` CHECK 与 §4 `analytics.group.view` 审批属性按 V394 更新为已闭合；§2 关键端点补 lifecycle 并标注 DELETE 恒拒绝；§3 税费权限码登记句按 V217 / SupervisorReview 现状改写。
+
+> 变更记录（2026-09-14，V1.02，M013-012–016）：集团项目隔离现状与审批属性闭合分开；岗位模板普通权限与待批高危、日常临时审批、申请权限风险档、总结查询参数按现有实现订正。
diff --git a/docs/platform-governance-acceptance-pack-v0.2.md b/docs/platform-governance-acceptance-pack-v0.2.md
index dbdfc245..c5b1f63c 100644
--- a/docs/platform-governance-acceptance-pack-v0.2.md
+++ b/docs/platform-governance-acceptance-pack-v0.2.md
@@ -1,4 +1,4 @@
-# 平台治理验收包 V0.04
+# 平台治理验收包 V0.05
 
 > 合并说明：本文按 2026-06-10 全量文档收口与合并归档审计执行，用于替代下列分散源文件。源文件已移入 `_archive/20260610-retention-audit/merged-layer-spec-sources/`，后续开发、审计和交接以本文为入口。
 
@@ -178,7 +178,7 @@
 | G4 | 空间/铺位建模 | 部分 | 空间对象须按 GOV 既有口径收敛 | P0 按楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位建模；铺位按 HTML 已定义口径管理，不另立区块层级 |
 | G5 | 合同审批边界 | 已收敛 | 原"P0 仅单级确认、多级审批 P1"与 GOV 多级模型冲突 | 合同新建、到期终止、变更三类各走各自的审批流程模板、模板分别可配（业务参数，按综合体可配，须同时配置上下限），各类阶段数与审批岗位可以不同；每类均按统一审批模型执行：草稿提交时指定这条主线的最终审批人（默认总经理），系统按组织层级逐级自动补全；业务、财务、法务为平行的相关部门线，各指定该部门最终审批人并补全至该部门预先设定的最低起点层级；主线与各条平行线全部通过合同才生效，任一节点驳回则整单退回起草。三类均不得录入即生效：新建未审批不生效、到期不得无审批自动终止、变更未审批不生效。产品细化见 PRD §12 已确认第 18 条 |
 | G6 | 高级审批级别阶梯 | 已补 | 原把"指定角色"与 level_rank 计算写成已建模缺口 | 收口为 GOV 三档：上一级、上两级、最高权限。最高权限是持有授权 U 盘密钥的那一个人，不按职级档位解析；不设指定角色档 |
-| G7 | 集团视图与跨项目汇总 | 已承接；实现缺口已闭合 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD §4 同步），不反向改写业务口径 |
+| G7 | 集团视图与跨项目汇总 | 已承接；审批属性已闭合，项目范围隔离待对齐 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接基本聚合能力，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`；项目级调用者的汇总结果尚未按本项目过滤（集团治理 PRD §4），不得把审批属性闭合当作隔离已完成 |
 
 > 待批准事项：员工数据处理与监控告知的同意凭据尚未写入三份 HTML，不作为本验收包必过项，也不得升为必填字段或注册流程，不另造替换 consent 字段。
 
@@ -197,6 +197,7 @@
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
 | 2026-09-11 | 改 | §10A G7 / §10 | 版本升 V0.04；集团视图审批属性缺口按 V394 改为已闭合 | 160 全量审计 M013-002 |
+| 2026-09-14 | 改 | §10A G7 | 版本升 V0.05；区分集团审批属性已闭合与项目范围隔离待对齐 | M013-012；集团治理 PRD §4 与当前集团总览调用链 |
 | 2026-09-01 | 增/改 | §2 / §2A / §10A G7 | 版本升 V0.03；补金额精度、合同本地 AI、合同当地时间和外部经销商全链验收；集团视图统一为已承接并保留实现缺口 | M01 全量复审 ADV-01/02/04/05/08；三份业务 HTML 现文 |
 | 2026-06-07 | 改 | §6 终端开关页验收 | 终端枚举改为 10 端，对齐字段字典 §3.1 | D5、govfam-3；本轮已再对齐十一端 |
 | 2026-06-07 | 改 | §7 终端开关状态 | 去掉无支撑的"定时启用/定时停用"独立态，改为派生过渡态 | govfam-4 |
diff --git a/docs/platform-governance-prd-v0.1.md b/docs/platform-governance-prd-v0.1.md
index 29d77349..ead26b29 100644
--- a/docs/platform-governance-prd-v0.1.md
+++ b/docs/platform-governance-prd-v0.1.md
@@ -1,4 +1,4 @@
-# 平台治理层产品需求 PRD V0.04
+# 平台治理层产品需求 PRD V0.05
 
 > 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。**三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决**（2026-08-05 R7 补，依据 PGM「解释权声明」条 / DIFF-A-051）。
 
@@ -654,7 +654,7 @@
 > **（2026-07-20 安全修复 AUD-011，补记于第 14 条）权限模板应用不是高危授权审批的例外**：`applyPermissionTemplate` 现与角色/岗位/临时授权走同一政策引擎 `requireGrantApprovalIfNeeded`——模板含任一 `requires_approval` 权限时，经模板入口批量应用一律 fail-closed 拒绝（`GRANT_REQUIRES_APPROVAL` 403），不得绕过职责分离/主管复核。高风险授权须改走 `applyGrant → reviewGrant` 主管复核流（该流按权限快照建待审请求、批准后再原子应用）；模板入口对高风险直接拒绝、无待审窗口，故审批期间模板内容变化亦无从偷换。
 14A. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、**税费规则与税费调整**（共“五类”）——上述权限授出前必须先审批才生效；配置只允许新增高危项或提高审批等级，不得关闭这些强制闸门。授权申请人与审批人必须职责分离，申请人不能审批自己的授权申请。**持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不得审批自己的单据。税费规则与税费调整的审批链固定为：财务审核 → 税务审核 → 法务审核 → 顶层平台公司老板最终确认后生效**；任一环节驳回均不得生效，提交人不得审批自己提交的调整，全部审批意见和时间必须留痕；税率、税目不属于普通后台配置项。（依据 PGM「高危授权审批（已确认）」条、ENUM「税费配置」条；与 §7.8.1 及 `group-governance-rebuild-prd-v1.0.md` §3 口径一致）
 14B. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更后必须立即按变更后的岗位权限模板重算有效权限并使旧岗位或旧范围派生权限失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效直至审批通过。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。本条不把仅职级变化或任意手工模板应用列为触发条件（DEC-R8-91）。
-15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**不再是本 PRD 的未来待办**。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
+15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**基本聚合能力不再是本 PRD 的未来待办**；上述闭环仅指审批属性，项目级调用者的汇总结果尚未按本项目过滤，仍须按集团治理 PRD §4 对齐项目隔离。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
 16. 所有端都必须有开关接口，支持按全局、综合体、商户、业务系统、模块、版本/设备、灰度控制，高层级关闭压倒低层级打开；关闭后该端或模块不可用，打开后仍受权限、数据范围和设备状态控制；与钱有关的规则（分账比例、结算口径、综合服务费、租金与抽成规则）一律不得使用灰度、不得按商户或设备部分开放，只能按综合体整体统一切换，切换日期由后台配置（业务参数，按综合体统一可配，须同时配置上下限），切换前后各自适用完整的一套规则；灰度只用于不影响金额计算的功能开放。同一套规则之下的参数取值按三份业务 HTML 为该参数写明的粒度配置，不属于灰度或部分开放。开关缺配置行一律默认关闭（opt-in），不得由代码或部署脚本视同打开；开关服务、支付通路、核销与入场等营业必需的能力不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败按兜底关闭处理、写操作一律拒绝，只有留痕不受兜底关闭影响。终端类型为受控十一类（客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户），产品细化见 `platform-governance-product-spec-v0.2.md`「字段字典」部分 §3.1「终端开关字段」，最终业务边界仍以三份业务 HTML 为准。
 17. 所有有过程的业务动作必须留痕，订单获取、销售归属、卡座服务、异常第一处理人，以及酒水和可追责物品从库房到客人的交接过程必须能追溯责任人、设备、地点、时间和结果。已拍板例外：安检环节不产生按人记录，留痕与溯源以设备侧记录为终点。
 18. 合同生命周期统一为「信息录入/发起 → 可配置审批流 → 审批通过后生效」：新建合同审批通过后才生效；到期或终止审批通过后才终止；变更（续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权、位置变更）审批通过后才变更或终止落地。审批流模板可配，一律按全平台统一的审批模型执行：指定主线最终审批人（默认总经理）＋系统逐级自动补全，业务/财务/法务为平行的相关部门线、各指定该部门最终审批人并补全至该部门预设的最低起点层级，主线与各平行线都通过才生效；任一节点驳回则整单退回起草；超时未处理自动提醒并升级到上一级、不得默认通过；全程保留原因、版本快照与审计。（2026-08-04 用户裁决改写）
@@ -967,3 +967,4 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责
 | 2026-08-29 | 改 | §7.3 / §12#10 / §12#5 / §15.2 / §15.4 | 业态去掉是否核心/第三方接入/统计分类/状态；废止多类对象统一待复核；告知/同意不得升为必填或注册流程，不另造 consent_* | M01 ADV-017 / ADV-003 |
 | 2026-08-29 | 改 | §6.3 / §7.2 / §7.3 | 业态收回 ENUM 六值+单一归属，废止维护/绑定开关/复合业态；废止空间「是否可留痕」开关 | M01 R-007 / R-008 |
 | 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风险功能列表「考勤打卡」改「查看考勤」（治理地图 V0.12 L741、业务地图 V0.14 L930/L1865/L1445）；§6.2.1 改为字典硬校验口径并补 `parking`；自营标注改为合作方式 `self_operated` 既有字段；准入闸口现状改为员工域已接线、四域未接；GAP-20 按 V394 闭合 | 160 全量审计 M013-002/003/004/007/008；PGM L741、BSM L930/L1865/L1445 现文 |
+| 2026-09-14 | 改 | §12 已确认第15条 | 版本升 V0.05；集团基本聚合与审批属性已具备，项目级结果过滤仍待对齐 | M013-012；PGM 权限规则及集团治理 PRD §4 |
```

---

## 11. 统一结论（审完整 diff 后一次给出）

- 修复内容：PASS（011–016 均纠正原错述；文档可按 §5/集团 §3–4 照做；未把实现限制立成 HTML 规则；旧实现缺口未冒称完成；017 未混进本 diff）。
- 归因：PASS（每一变更行可归 011–016；四标题版本与三处变更记录已逐行归因；无无 ID 行；无 001–010/017 行）。
- 总结论：PASS。
- 本票不是模块 PASS，不是 B5，不是单票收口。B4 须双 PASS。
- graft 本轮约 605504 tokens（10 calls）。

模块 M013 / 共 471，B3 第 2 次。本席 B4-r2 完整报告已落盘。
