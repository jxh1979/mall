CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M034-AB B1 修复前全量审计（席乙）

版本：V0.01
阶段：B1。本文件是本次完整报告，不追加、不覆盖旧文（落盘前该路径不存在）。
席：Grok，独立只读。模型 grok-4.7，推理强度 xhigh。
结论：FAIL

锁：现行 `docs/audit-2026-09/plan/criteria-lock.md` 版本 V0.14。本席在本机执行 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`，第一行是该命令输出的 64 位小写十六进制，不是从 sidecar、ledger 或派发提示抄来的。算完后对照，sidecar 与 ledger 顶部同一字段的值与实算相同。LOCK-AMEND-COUNT 读 ledger 为 1。本席不改锁、不新增判据。

基线：正本 `/home/james/complex-management`。主工作区 HEAD 是 `e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main` 是 `daf6adfc0bada1e1d972aa8e4499eb117925e244`。代码与三份 HTML 以 `daf6adfc` 为准。开工段 33 个文件里，32 个工作树 blob 前 12 位与 `daf6adfc` 相同；`V429__retire_verification_manual_override.sql` 工作树与 `daf6adfc` 不一致（`git diff --stat daf6adfc --` 该文件为 17 行删除）。本席采用 `git show daf6adfc:` 的 17 行正文，不用工作树副本。三份 HTML 对 `daf6adfc` 的 `git diff --stat` 为空。

未读 `modules/XM-M034-AB-b1-self-raw.md`。未用 MCP、子代理或第三席。未改业务代码、三份 HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未对任何数据库执行语句，未建库。未创建一次性 worktree，因此也没有 `git worktree remove`。未把脚本或日志写入仓库。

## 1. 技能（按本环境实况，不把未读的写成已调用）

用户指令与 160-audit-prompt.md V0.17 优先于技能正文里的问人、派子代理、改代码和另写产物。

- `using-superpowers`：本环境文件存在，已读开头。其「任何动作前先调用技能、派子代理」与本轮「只允许两席、不问人、不修」冲突，按用户指令收窄，不派代理。
- `using-arc`：已读。Arc `audit` 会派评审员，本轮不走。
- `graft`：本仓库有该技能。工作树与 `origin/main` 分叉（HEAD 上 `AmusementService.java` 的 blob 与 `daf6adfc` 不同，V429 工作树也不同）。graft 索引的是当前工作树，不能当 `daf6adfc` 的证据。本席改用 `git grep` / `git show daf6adfc:`。这是故意不用 graft，不是忘了。
- `grilling`：已读全文。追问结构用于第 7 节；不等操作员，不派子代理查事实。
- `systematic-debugging`：已读。只沿「读代码、对 HTML、追调用」做根因。不加诊断日志，不改代码。本体要求先加探针，本轮作废。
- `diagnosing-bugs`：已读。本缺陷不是一条变红的测试：检查点日志是全绿，绿的断言正是被废止的成功路径。用户禁止对库写、禁止为了复现去建库。本席没有做一条会变红的可执行回路，这是限制，不是已复现。
- `verification-before-completion`：已读。下面凡是「测试全绿」只引用已读日志的原文，本席没有重跑 Maven。没有把未跑的测试写成通过。
- `ponytail-review`：已读。处置按「能删写入口就不留拒绝壳、不改已应用迁移、不为理论分支加抽象」。本阶段不改代码，所以没有行数净值。
- `codebase-design`：已读正文。其后续文档要求并行子代理做第二次设计，本轮不执行。删除测试只用在处置意见里：票务人工放行写方法和游乐 `release` 清零删掉之后，调用方不再承担这段行为。
- `ponytail` 本体：文件存在。B1 不修复，未按其强度改代码。
- `tdd` 与 superpowers `test-driven-development`：文件存在。B1 禁止修复，未写测试。
- `writing-for-agents`：文件存在，未读正文。本 raw 按 V0.17 与 runbook 第 3、4 节写，不写模块收口文件。
- `git-guardrails-claude-code`：文件存在。本轮无提交、无 reset、无 push，未跑该流程。
- `resolving-merge-conflicts`、`requesting-code-review`、`receiving-code-review`、`brainstorming`、`writing-plans`：本阶段不是合并、B4、阶段 A，未调用。
- `batch-grill-me`、`ubiquitous-language`：在 mattpocock 技能目录下未找到，记缺口，不冒充已调用。
- `i-have-adhd`：本席未打开对应 SKILL.md。V0.17 要求本文件第一行是锁哈希，故不把「下一步」放在第一行。
- 划掉不用的技能（executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、ponytail-audit、ponytail-debt、ponytail-gain、code-review、domain-modeling）未调用。

## 2. 范围与读法

判据并集实算 181。17 个模块映射都在锁内，无缺号。定义标题 181/181 都能对上 `### ID —`。只引用，不新增。

33 个文件以开工段清单为准。超长文件没有逐行通读：`openapi-p0.json`、`openapi-p0.yaml`、`TicketingServiceTests.java`、`AmusementServiceTests.java`、`AmusementService.java`、`JdbcAmusementRepository.java`。对这些文件用 `git grep` 把 `manual-override`、`manualOverride`、`manual_overridden`、`verification.override`、`override_type`、`clearProjectConcurrentCapacity` 的命中收齐，再读命中段和判断所需的上下方法。因此本报告不宣布这些文件的未读段落没有别的缺陷。FAIL 来自已经读到的冲突，不来自「整文件无事」。

`exitVerification`（`AmusementService.java:1401-1442`，控制器 `AmusementController.java:532`）读过。它按未释放入场记录减并发人数，同一入场重复出场不重复减，没有未释放入场就拒绝或返回已有出场。这与业务地图「出场自动扣减」同向，不是五种改判里的 `release`，不并入 GR-02。

前端：`git grep` `daf6adfc -- apps` 对 `manual-overrides`、`manual-override`、`manualOverride`、`manual_overridden` 无命中。`tests/e2e/support/mock-api.ts:262` 把游乐 GET 打成空列表，不是产品调用方。

检查点日志 `/home/james/audit-tmp/xm031b_ckpt_commit.log` 本席读到的头部原文：`MVN_EXIT=0`、`suites=263 tests=3539 red=0`、随后提交说明指向 XM-M031-B。本席没有重跑这套测试，也没有连库。全绿只说明现行实现（含人工放行成功断言、`allow` 记 approved、`release` 把在场人数打成 0）在空库上跑得通，不说明它符合 HTML。

## 3. 第一性三问

### 3.1 票务人工放行写入口

为什么必须存在：删掉 `POST /api/v1/ticketing/verifications/manual-override` 以及 `TicketingService.manualOverrideVerification`、`TicketVerificationService.manualOverride` 之后，枚举速查「核销记录状态」没有一条会做不到。该条要求的是成功、拒绝、失败、已冲正、待复核、异常，并写明系统不提供人工放行。这个入口的注释和测试把自己定义成人工放行。

声称的行为有没有被执行验证：服务方法在内存仓库测试里确实写入 `status=manual_overridden`（`TicketingServiceTests.java:490-498`）。控制器测试在打桩放行权限后期望 HTTP 200 和该状态（`TicketingControllerTests.java:282-302`）。权限停用由 `AccessJdbcIntegrationTests.java:650-658` 断言 `permissionExists` 为假且四处授予为 0。该集成测试在上述全绿日志所代表的那次空库全量里，没有被记成失败。本席没有单独重跑它。

未经验证就被当成前提的一点：注释写「放行不自动扣减权益（是否扣减为后续策略）」（`TicketVerificationService.java:209`）。代码路径里没有扣减调用。本席没有在库里插一行再数 `used_times`。结算侧 `JdbcSettlementTransactionRepository.java:137`（不在 33 文件内，只作调用链旁证）把 `manual_overridden` 排除在计入 `used_times` 的成功核销之外。所以「写出该状态」已被测试验证，「把它当成核销成功并扣次」没有被这条路径做出来。

### 3.2 游乐五种改判

为什么必须存在：五种类型共用一个 POST。删掉整个 POST 不会使「现场核销判断权益与容量」「出场自动扣减」「场次结束自动清空」「项目开放状态里的暂停」「主管确认后恢复营业」做不到。这些句子各自有别的实现或仍缺的实现，但不靠这五行类型。

`allow`、`deny`、`pause`、`resume` 的实际行为是插入 `amusement_manual_overrides` 一行。`release` 在插入之外把并发快照清零。核销主链不读这张表：`daf6adfc` 的 Java 里，`amusement_manual_overrides` 只出现在保存、列表、行映射和测试。

未经验证的前提：V234 注释和 `AmusementExitReleaseJdbcIntegrationTests` 把 `release` 写成 2026-07-05 用户拍板的主管异常清场。三份 HTML 里没有这一句授权。现行 HTML 有相反句（见 GR-02）。本席不把迁移注释当成需求。

### 3.3 权限码

`verification.manual_override`：V270 种入为 active 并授给 `role_platform_admin`。V429 把它改为 `retired`，并删除角色权限、模板项、用户覆盖，把仍 active 的临时授权改成 revoked。`JdbcAccessRepository.permissionExists`（`JdbcAccessRepository.java:30-37`）只计 `status = 'active'`。`AuthorizationService.permissionGrant`（`AuthorizationService.java:72-74`）在最高权限短路之前先调用 `knownPermission`；不存在或非 active 就 `deny`。因此该码对包括最高权限在内的任何主体都进不了写方法。这是读代码得出的顺序，不是本席打出来的 HTTP 403。

`amusement.verification.override`：V014 种入 active，授给平台管理员；V170 再次把 `status` 写成 active；V251 中文名「覆盖游乐核验结果」；V254 改为「强制改判游乐核验结果」。其后没有迁移把它 retired。控制器 POST/GET 都 `requirePermission` 这个码。码仍 active 时，平台管理员和（在 `knownPermission` 通过之后）最高权限可以调用写方法。

## 4. 五种类型对三份 HTML

枚举速查第 43 行整格原文（`docs/business-enum-quick-reference-v0.1.html:43`，判据 BEQR-012）：

> 成功、拒绝（规则不允许）、失败（设备或网络原因）、已冲正、待复核、异常，共六态，不设「预检通过」态。……前两道环节不产生票务核销记录，本条的核销记录状态只适用于检票环节。……系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过。

业务地图游乐管理主要功能（`docs/business-system-map-for-operations-v0.1.html:1262-1263`，锚在 BSM-069，父锚 BSM-068）：

> 出场自动扣减：游乐项目出口设置闸机或摄像头感应器；发现有人走过出口、且该人在入场闸机或入场摄像头留有入场记录时，系统自动把该项目实时在玩人数减一；同一人重复经过不重复扣减；没有入场记录的经过不扣减。按场清场的场次型项目，散场时由系统按场次结束自动清空。

> 在玩人数不允许人工调整：任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数；计数只能由入场核销与出场自动扣减产生。

项目开放状态（枚举第 140 行，BEQR-085）：未开放、准备中、开放中、暂停、检修中、已关闭、异常。

同章另有「主管确认检修通过后，设施才能恢复营业」（业务地图约第 1269 行）。那是检修确认，不是 `override_type=resume`。

年龄限制条里的「人脸误拒经现场人工复核放行」是检票之前的年龄环节，本 33 文件的人工放行方法不实现它。准入「先卡住、再按更高级审核放行」写在治理地图和 V099 的 `over_contract_scope` 策略里。权限矩阵第 5 行写明那是准入阻断的升级放行，不得回读成人工放行一笔核销。矩阵不是三份 HTML，只作旁证。

| 类型 | 代码实际做了什么 | 与「不提供人工放行 / 不能让不满足规则的核销通过」 | 与其它 HTML 原句 | 判定 |
|---|---|---|---|---|
| 票务写入口（不是游乐类型，但是同一末句的票务面） | 新插入一条 `ticket_verifications.status=manual_overridden`，不扣权益 | 末句所在格写明状态只适用于检票环节。这条写的就是票务核销记录，落在格内。状态不在六态里。入口名称和注释就是人工放行 | 无另外的授权句 | 冲突。见 GR-01 |
| allow | 只插入一行，`override_type=allow`，`status=approved`。不改游乐核销结果，不扣权益 | 末句前半「系统不提供人工放行」：POST、权限中文名、OpenAPI 摘要「游乐人工放行」、测试把 approved 当成功，仍在提供。后半「让核销通过」：今天没有发生，核销主链不读该行 | 不是在玩人数清零，也不是项目暂停 | 冲突，只针对「提供」。见 GR-03 |
| deny | 只插入一行，`status=denied`。不把任何核销改成拒绝 | 不构成放行，也不构成「让通过」 | 没有一条 HTML 要求主管另记一笔否认 | 添加。见 GR-04 |
| pause | 只插入一行，`status=recorded`。不改 `open_status` | 不是放行 | 暂停是项目开放状态。这个类型没有去改那个状态，所以它不是该条的实现 | 添加。见 GR-04 |
| resume | 只插入一行，`status=recorded`。不调用检修恢复 | 不是放行 | 「主管确认检修通过后才能恢复营业」由巡检恢复路径承担，不是这个类型 | 添加。见 GR-04 |
| release | 调用 `clearProjectConcurrentCapacity`，把 `point_device` 与 `project` 两维 `occupied_count>0` 的行置 0；不写 `released_at`；不改 `amusement_sessions.used_capacity`；再插入 `status=released` | 不是「让一笔核销通过」。不要用末句一刀切掉它 | 与第 1263 行「不得手工清零」冲突。第 1262 行的合法清场是出场自动扣减，以及场次型由系统在场次结束时清空。V234 注释里的 2026-07-05 拍板不是三份 HTML | 冲突，依据是第 1263 行，不是末句。见 GR-02 |

`createManualOverrideInternal`（`AmusementService.java:1581-1616`）对五种类型的分支是：`release` 才清零；状态映射 `allow→approved`、`deny→denied`、`release→released`，其余（`pause`、`resume`）为 `recorded`。`OVERRIDE_TYPES` 在 `AmusementService.java:36`。

## 5. 问题

### GR-01 票务人工放行写入口仍在

- 文件:行：`TicketingController.java:495-507`；`TicketingService.java:934-937`；`TicketVerificationService.java:207-248`；`VerificationManualOverrideRequest.java:6-24`；`V067__ticket_multiperson_reverse.sql:17-25`；`V270__ticket_manual_override_permission.sql:8-16`；`V429__retire_verification_manual_override.sql:8-16`（`daf6adfc` 正文）；`TicketingControllerTests.java:237-328`；`TicketingServiceTests.java:488-498`；`docs/p0-operation-permission-matrix-v0.1.md:5` 与 `:63`。
- 原句：控制器注释「人工放行（主管）。主管对核销做人工放行决策，留痕 status=manual_overridden。」服务写入的状态字面量是 `manual_overridden`（`TicketVerificationService.java:229`）。HTML 六态没有这个词，末句禁止人工放行。
- 正常触发：已认证请求 `POST /api/v1/ticketing/verifications/manual-override`。按 `permissionExists` 只认 active，V429 之后该码不是 active，`requirePermission` 在最高权限判断之前拒绝。写方法因此从控制器进不去。方法本身仍是 public，测试不经过权限直接调用并期望成功。OpenAPI 的 json 与 yaml 没有这条路径（`git grep manual-override` 在两份契约里只命中游乐路径）。
- 四分类顺序：先冲突。与 BEQR-012 明文矛盾。不是细化，不是技术性，不是添加。
- 已有判据：BEQR-012、GEN-002、GEN-003、GEN-005、GEN-016、GEN-017。
- 证据类型：类型 1（HTML 第 43 行原文）加类型 4（`git show` / `git grep` 的路径与行）。不是本席新跑出来的 HTTP 响应。
- 工程缺陷：成立。权限停用没有去掉写入口。测试仍把废除行为断言为成功，所以全绿不能当符合 HTML。
- 附带、不单独立项：`manualOverrideVerification` 不走 `executeWrite` 幂等，控制器只检查幂等键非空（GEN-007）。当前被权限拒绝挡住，第一次正常调用到不了重复写入。DTO 注释写权限码 `verification.override`（`VerificationManualOverrideRequest.java:8`），运行时用的是 `verification.manual_override`。注释错误，不构成第二条缺陷。
- 最小处置：删除控制器映射、`TicketingService.manualOverrideVerification`、`TicketVerificationService.manualOverride`，以及只被该入口使用的 `VerificationManualOverrideRequest`。同步删或改 `TicketingControllerTests`、`TicketingServiceTests` 里期望 200 和 `manual_overridden` 的用例。不要留一个专门返回 403 的空映射；权限层已经会拒绝，再留路由仍然是在提供入口。不要把该路径补进 OpenAPI。不要改 V067 的 CHECK：历史行可以用 `manual_overridden`，范围外的 `TicketingJdbcIntegrationTests.java:281-282` 还在插入该状态。不要改 V270、V429。不要新增迁移去改状态或删行。权限码保持 retired，不要重新授予。权限矩阵第 63 行已写「停用，待代码轮下线」；代码删除后把这一格改成写入口已去掉，避免文档继续把下线说成未来时。`ticketing.special_release` 的准入策略在已应用的 V068（不在本 33 文件的改写范围内），V099 只是注释里点到它已被占用。删方法后生产代码不再引用该 action。不要改这两份已应用迁移。

### GR-02 游乐 release 清零在玩人数

- 文件:行：`AmusementService.java:1594-1599` 与 `:1600-1603`；`JdbcAmusementRepository.java:1109-1125`；`V234__amusement_exit_release.sql:44-49`；`AmusementExitReleaseJdbcIntegrationTests.java:147-164` 与 `:243-248`；OpenAPI `override_type` 枚举不含 `release`（`openapi-p0.yaml:31154-31162`，json 同构 `43463-43472`）。
- 原句：业务地图第 1263 行「任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数；计数只能由入场核销与出场自动扣减产生。」代码注释写「异常清场——清零本项目并发在场快照」。测试断言 `projectSnapshotOccupied` 从 1 变成 0。
- 正常触发：持有 active 的 `amusement.verification.override` 的主体（V014 授给平台管理员；码仍 active）调用 `POST /api/v1/amusement/manual-overrides`，`override_type=release`。这不是两件偶然同时发生才出现的路径。
- 四分类顺序：冲突。与 BSM-068/BSM-069 里的第 1263 行矛盾，也与 BEQR-088、BEQR-089 所依赖的「计数只来自入场与出场自动扣减」矛盾。不是末句那种人工放行。
- 已有判据：BSM-068、BSM-069、BEQR-088、BEQR-089、GEN-006、GEN-008。
- 证据类型：类型 1（第 1262-1263 行）加类型 4（源码与测试断言的行）。集成测试本席未重跑；全绿日志说明这条断言当时是绿的，也就是清零当时成功。
- 工程缺陷：成立。另外，清零只改快照，不把对应入场行的 `released_at` 写上。快照已是 0，未释放入场记录仍在。这是同一次操作的完整性裂缝，并进本条，不另编号。OpenAPI 枚举没有 `release`，服务器仍接受，契约与实现不一致，并进本条。
- 最小处置：`createManualOverrideInternal` 拒绝 `release`，删除对 `clearProjectConcurrentCapacity` 的调用。若此后没有其它生产调用方，该方法可以留在仓储里供出场路径之外不再使用，但不要再从改判入口调用；更小的做法是改判入口直接拒绝该类型，仓储方法若无调用再删。本席看到的生产调用就是这一处。历史 `amusement_manual_overrides` 行保留。不改 V014、V234 的 CHECK，否则已有 `release` 行会违反约束。不写前向迁移去改历史快照或删行。`AmusementExitReleaseJdbcIntegrationTests` 里两处「主管释放后占用为 0」改成期望拒绝。`exitVerification` 不动。权限码先不 retired：`deny`/`pause`/`resume` 仍走同一码，见 GR-04。OpenAPI 本来就没有 `release`，拒绝该类型之后与枚举对齐，不必把 `release` 补进契约。

### GR-03 游乐 allow 仍作为人工放行被接受

- 文件:行：`AmusementController.java:45-51`；`AmusementService.java:120-133` 与 `:1600-1616`；`AmusementServiceTests.java:1618-1650`；`AmusementControllerTests.java:116-117`、`:236-238`、`:391-392`；`openapi-p0.yaml:574-583`；`docs/p0-openapi-operation-acceptance-matrix-v0.1.md:44`；`V254__permission_names_p3_polish.sql:17`。
- 原句：OpenAPI 摘要「游乐人工放行（已废止，不得新增调用）」，描述引用末句。测试期望 `overrideType=allow` 且 `status=approved`。HTML 末句「系统不提供人工放行」。
- 正常触发：与 GR-02 同一 POST，`override_type=allow`，权限码仍 active。`AmusementServiceTests` 在不连库的服务测试里把这次写入当成功。
- 四分类顺序：冲突。末句前半是否定「提供」。后半「让核销通过」没有被这段代码做出来，处置不得去核销主链里找一条不存在的放行分支来删。
- 已有判据：BEQR-012、GEN-001、GEN-003、GEN-005。
- 证据类型：类型 1 加类型 4。
- 工程缺陷：成立。提供了名为放行、状态为 approved 的写入。效果止于一行记录。
- 最小处置：POST 拒绝 `allow`。OpenAPI 两份契约的 `override_type` 枚举去掉 `allow`。控制器测试和 `AmusementServiceTests.manualOverrideRequiresReasonAndRecordsOverride` 不再把 `allow/approved` 当成功。不改核销执行方法。不删历史行。不因此 retired 整个权限码（GR-04 未答复前 POST 还要接待其它类型）。

### GR-04 deny、pause、resume 没有 HTML 上位，且 pause/resume 不改变开放状态

- 文件:行：`AmusementService.java:36` 与 `:1592-1604`；V014 原 CHECK 含这三型（`V014__p0_amusement_safety_session_verification.sql:133`）；V234 扩成五型后仍含它们。
- 原句：三份 HTML 没有「主管写入 deny/pause/resume 改判记录」这一条。项目开放状态条有「暂停」。游乐章有「记录巡检、暂停、关闭」和「主管确认检修通过后，设施才能恢复营业」。本类型的 pause/resume 不写项目 `open_status`，也不调用检修恢复。
- 正常触发：同一 POST，类型为三者之一，权限码仍 active。会成功插入一行。
- 四分类顺序：先看冲突：它们不让核销通过，也不清零在玩人数，对不上末句和第 1263 行。再看细化：指不出哪一句是它们的上位，因为它们没有实现「把项目改为暂停」或「检修确认后恢复」。不是纯技术性：类型名是业务词。落到添加。
- 已有判据：GEN-001、GEN-017。不能用锁外新判据。
- 证据类型：类型 1（相关句存在，但管的是另一条实现）加类型 4（分支只插入）。
- 工程缺陷：pause/resume 与名字不符。在操作员对添加项答复之前，不把这个名实不符单独送进修复队列。若答复保留，再按名实不符修，那是答复之后的事。
- 最小处置意见（供 B2 询问，本席不代答）：建议删除这三型的新写入，并在 allow 与 release 都已拒绝、POST 不再需要该权限时，新增一份前向迁移，按 V429 的方式把 `amusement.verification.override` 标为 retired 并清掉四处授予。GET 列表若仍要读历史，改用已有的 `amusement.verification.view`，或随 POST 一起去掉、历史只留在表里。不删表，不改 V014/V234 的类型 CHECK。保留的代价：一个高风险、须审批、中文名仍是「强制改判游乐核验结果」的码继续活着，只为了写三行不会改变核销或开放状态的记录；OpenAPI 仍把操作摘要写成已废止的人工放行。

添加项未在本 B1 写 `questions/`。询问时机在 B2。未答复前本模块不能当 PASS。

## 6. 下线或收窄（汇总，不执行）

票务：删写入口，不留拒绝壳。权限码已经 retired，不再做第二份迁移。CHECK 与历史行保持。契约里本就没有该路径。

游乐：不要五型一起删。`release` 按第 1263 行拒绝。`allow` 按末句「不提供」拒绝。`deny`、`pause`、`resume` 等操作员在添加项上选保留或删除。权限码在仍有被允许的新写入时保持 active；只在写入都去掉之后做前向停用。表、历史行、已应用迁移不动。不需要为了停用写入而去改 V014 或 V234。GET 在添加项答复前保持，避免把「能不能查历史」当成已经决定的删除。

`clearProjectConcurrentCapacity` 若只被 release 使用，拒绝 release 后成为无调用方，可以一并删方法。出场扣减走的是 `releaseEntryConcurrentCapacity`，不要删错。

## 7. 对抗（grilling，自问自答，不等候）

问 1：第 43 行写了「核销记录状态只适用于检票环节」，游乐 allow 是不是根本不受末句约束？

答：限制句的宾语是「核销记录状态」。它把六态从安检、年龄核验上拿开，也把六态限定在检票。末句用的是「人工放行」和「核销」，并写「任何岗位、任何权限」。票务写入口落在检票核销记录上，无论怎么读限制句都在格内。游乐 allow 不把核销结果改成成功，所以末句后半的「通过」没有发生；末句前半的「不提供」仍被这个 POST 打破。不把游乐核销主链说成已经放行。

问 2：2026-07-05 的主管清场是不是合法现场操作，所以 release 必须留下？

答：不是。该拍板现在只存在于 V234 注释、测试名和设计文档。三份 HTML 的现行句禁止主管和最高权限清零。合法的清空是第 1262 行的自动扣减和场次结束自动清空。

问 3：票务入口已经 403，是不是可以不算缺陷？

答：不算已符合「不提供」。路由、服务方法和成功测试都还在。V429 自己的注释写明端点下线另行登记。这就是被登记后仍在的那一项。

问 4：为了稳妥，是否把 POST 改成永远 403，五种都留下？

答：不。拒绝壳仍是入口。票务侧权限已经会 403，再留映射是重复。游乐侧若整段 403，会在添加项答复前删掉尚未决定的 deny/pause/resume。冲突的两类先拒绝，添加的三类先问。

问 5：是否改 V067，把 `manual_overridden` 从 CHECK 去掉，以对齐六态？

答：不。已应用迁移不改。去掉该值而又不改写历史行，已有行会违反约束。改写历史状态等于改历史。六态里的待复核、异常本文件的 CHECK 也没有，那是票务状态机的另一面，本席没有把它升成第 5 条缺陷：本轮没有沿核销主链证明「待复核」写入点缺失的全部调用方，不把未追完的状态机缺口写成已证实缺陷。

问 6：项目开放状态代码是 `open/paused/closed/maintenance`（`AmusementService.java:34`），HTML 是七个中文态。要不要立项？

答：看到了常量不一致。本席没有追完创建、更新和界面映射，不能说第一次正常保存会拒绝「准备中」或写出 HTML 没有的态。记为观察，不进 GR。场次状态 `scheduled/open/closed/cancelled` 对枚举「未开始、入场中、已满员、进行中、已结束、取消、异常」同样只记观察。

问 7：出场扫凭证减人是不是和第 1263 行一起该删？

答：不。`exitVerification` 要求有未释放入场，减的是这一笔的人数，重复出场不重复减。这是第 1262 行的扣减，不是一键清零。

问 8：年龄复核放行、合同越界后的升级放行，要不要跟着人工放行删掉？

答：不。两句都在 HTML 或矩阵里被写成另一件事。本模块文件不实现年龄复核放行。V099 的 `amusement_manual_override` 策略是越界先 block。删改判写入之后，这条已应用策略可以留着，没有新的放行效果。

## 8. 观察（不进修复队列）

- O-01：`PROJECT_STATUSES` 与 BEQR-085 七态不完全同形。未追完，不立项。
- O-02：`SESSION_STATUSES` 与 BEQR-086 不完全同形。未追完，不立项。
- O-03：DTO 注释中的权限码 `verification.override` 与运行时 `verification.manual_override` 不一致。随 GR-01 删除 DTO 即消失。
- O-04：工作树 V429 与 `daf6adfc` 不一致。审计对象用的是 `daf6adfc` 上已停用该码的 17 行。若有人按工作树缺这段的副本审查，会误判该码仍只由 V270 保持 active。

## 9. 181 项

有缺陷，对应 GR：BEQR-012（GR-01、GR-03），BEQR-088（GR-02），BEQR-089（GR-02），BSM-068（GR-02），BSM-069（GR-02），GEN-001（GR-03、GR-04），GEN-002（GR-01），GEN-003（GR-01、GR-03），GEN-005（GR-01、GR-03），GEN-006（GR-02），GEN-008（GR-02），GEN-016（GR-01），GEN-017（四条的分类）。共 13。

本路径已核对、除上述 GR 外未见新的违背：

- GEN-004：改判类型、原因、项目存在性有校验；缺的不是校验，而是不该接受的业务类型。
- GEN-007：游乐写入走幂等执行。票务写路径不走幂等执行，但控制器侧被已停用权限挡住，不单独立项。
- GEN-009：非法类型与缺原因抛出校验错误。未见吞掉异常后仍当成功。
- GEN-010：本路径未见把证据附件内容或密钥写进响应。无额外缺陷。
- GEN-011：游乐写入经 `executeWrite` 记成功或失败事件。票务人工放行方法本身没有这层审计包裹；入口按 GR-01 删除后该缺口随之消失，不单独立项。
- GEN-014：V429 是前向停用，符合不改已应用迁移。游乐码仍 active 是权限事实，不是某份迁移语法错误。
- GEN-018：本报告的依据只使用 HTML 原文、已读日志和可复现的 git 命令输出。
- PGM-028：V099 对 `amusement.manual_override` 是越界 block、上一级，不是核销放行。与矩阵第 5 行的区分一致。
- PGM-040：两个权限码是分开的功能码。票务码已停用，游乐码仍 active 的问题已在 GR-03/GR-02，不另说成「不能单独授权」。

共 9。另有未升格观察对应 BEQR-085、BEQR-086，不记通过，也不进队列。共 2。

其余 157 项：这些文件没有该锚点的业务对象，或者该锚点落在权限矩阵、OpenAPI、票务服务的其它段落里，本席没有逐句核对。不记通过。名单如下，每项都算进 181：

BEQR-001、BEQR-002、BEQR-003、BEQR-004、BEQR-005、BEQR-006、BEQR-007、BEQR-008、BEQR-009、BEQR-010、BEQR-011、BEQR-013、BEQR-014、BEQR-015、BEQR-016、BEQR-017、BEQR-018、BEQR-019、BEQR-020、BEQR-021、BEQR-022、BEQR-023、BEQR-024、BEQR-025、BEQR-026、BEQR-027、BEQR-032、BEQR-034、BEQR-040、BEQR-063、BEQR-074、BEQR-087、BEQR-090、BEQR-091、BEQR-092、BEQR-093、BEQR-094、BEQR-095、BEQR-096、BEQR-097、BEQR-124、BEQR-143、BEQR-144、BEQR-145、BEQR-146、BEQR-147、BEQR-148、BEQR-149、BEQR-150、BEQR-151、BEQR-152、BEQR-153、BEQR-154、BEQR-155、BEQR-156、BEQR-157、BEQR-158、BEQR-159、BEQR-163、BEQR-166、BEQR-182、BEQR-183、BEQR-184、BEQR-185、BEQR-186、BEQR-187、BEQR-189。

BSM-001、BSM-002、BSM-003、BSM-004、BSM-005、BSM-006、BSM-007、BSM-009、BSM-010、BSM-016、BSM-027、BSM-028、BSM-029、BSM-030、BSM-031、BSM-032、BSM-033、BSM-034、BSM-035、BSM-036、BSM-044、BSM-045、BSM-046、BSM-047、BSM-048、BSM-049、BSM-050、BSM-070、BSM-071、BSM-072、BSM-073、BSM-074、BSM-075、BSM-076、BSM-077、BSM-084、BSM-085、BSM-086、BSM-087、BSM-088、BSM-114、BSM-115、BSM-120。

PGM-001、PGM-002、PGM-003、PGM-004、PGM-005、PGM-006、PGM-007、PGM-008、PGM-009、PGM-010、PGM-011、PGM-012、PGM-013、PGM-014、PGM-015、PGM-016、PGM-017、PGM-018、PGM-019、PGM-020、PGM-021、PGM-022、PGM-023、PGM-024、PGM-026、PGM-027、PGM-029、PGM-030、PGM-031、PGM-032、PGM-033、PGM-034、PGM-035、PGM-036、PGM-038、PGM-041、PGM-042、PGM-043、PGM-044、PGM-048、PGM-059、PGM-060、PGM-065、PGM-066。

GEN-012、GEN-013、GEN-015：无界面、无依赖变更、无本路径上的资源上限问题。并入上面的「不记通过」计数前已单列在这三句；它们属于 157 的一部分（GEN-012、GEN-013、GEN-015 共 3，已含在 157 的算法里，不在上面三段字母名单里重复出现）。

计数：13 + 9 + 2 + 157 = 181。157 = 上列 BEQR 67 项 + BSM 43 项 + PGM 44 项 + GEN-012、GEN-013、GEN-015 共 3 项。67+43+44+3=157。这三则 GEN 不在上面三段名单里重复出现。下一段对 BEQR-004、BSM-070、BSM-071 的说明是同一项的读法，不另计。

BSM-070、BSM-071 放在未逐句核对：本席读了游乐章里和清场、暂停、恢复直接相关的列表项，没有把「和谁有关」「游乐类型与规则」整表逐句对完代码。BEQR-004 的年龄人工复核不在本入口，不记成已核对通过。

## 10. 33 个文件覆盖

| 文件 | 本席实际用到的部分 |
|---|---|
| AmusementController.java | 人工改判 GET/POST（39-51）；出场方法仅定位（532） |
| AmusementManualOverrideRecord.java | 全文 |
| AmusementManualOverrideRequest.java | 全文 |
| AmusementRepository.java | `save/list` 与 `clearProjectConcurrentCapacity` 声明 |
| AmusementService.java | 常量 34-36；列表与创建 109-133；改判内部 1581-1616；出场 1395-1442。其余未逐行 |
| JdbcAmusementRepository.java | 清零 1109-1125；插入与列表 1371-1409。其余未逐行 |
| JdbcAmusementRowMapper.java | `mapManualOverride` 203-214 |
| JdbcAdmissionFactRepository.java | 人工改判对象无空间解析的注释 135 行一带 |
| VerificationManualOverrideRequest.java | 全文 |
| TicketingController.java | 人工放行映射 495-507 |
| TicketingService.java | `manualOverrideVerification` 934-937 |
| TicketVerificationService.java | `manualOverride` 207-248 |
| V014 | 表与类型 CHECK 123-134；权限种入 182-206；角色授予 208-229 |
| V067 | 状态 CHECK 17-25 |
| V099 | 全文 60 行；人工改判越界策略在 50-51 |
| V170 | 高风险列表含游乐改判码并写回 active，约 173-187 |
| V234 | 全文；类型 CHECK 扩到五型在 44-49 |
| V251 | 该码中文名第 67 行 |
| V254 | 该码中文名第 17 行 |
| V270 | 全文 |
| V429 | `git show daf6adfc` 全文 |
| AccessJdbcIntegrationTests.java | 停用断言 647-659 |
| AmusementControllerTests.java | 路径、权限码、allow 请求体与样例记录 |
| AmusementJdbcIntegrationTests.java | 只确认表名断言仍在；未重跑 |
| AmusementServiceTests.java | `manualOverrideRequiresReasonAndRecordsOverride` 1618-1650 |
| AmusementExitReleaseJdbcIntegrationTests.java | 主管释放两则 147-164、243-248；出场扣减作对照 |
| TicketingControllerTests.java | 人工放行四则 237-328 |
| TicketingServiceTests.java | 人工放行一则 488-498 |
| openapi-p0.yaml | 游乐 manual-overrides 574-625；枚举 31122-31166。无票务该路径 |
| openapi-p0.json | 与 yaml 同一操作与枚举；无票务该路径 |
| p0-openapi-contract-closure-appendix-v0.1.md | 该操作与事件行（84、446-447、516-517、1854、1879） |
| p0-openapi-operation-acceptance-matrix-v0.1.md | 第 43-44 行 |
| p0-operation-permission-matrix-v0.1.md | 文首第 5 行；第 63、78、251、269 行。全文 301 行未逐段当 181 锚点核对 |

调用方（范围外，只读）：`AuthorizationService` 与 `JdbcAccessRepository.permissionExists`；结算排除 `manual_overridden` 的一行注释；`AdmissionGuard` 对 `ticketing.special_release` 的测试引用。前端产品代码无调用。

## 11. 结论

FAIL。

修复队列建议为是的：GR-01、GR-02、GR-03。建议为否、须先按添加项询问的：GR-04。观察 O-01 至 O-04 不进队列。

没有未写完的 GR。没有把「有条件通过」写成 PASS。B1 期间没有改任何业务文件。

## XM-M034-AB B2-r1 整批复核（Grok席乙，V0.02）

版本：V0.02。前文 B1 报告保持 V0.01，本段起把文件版本记为 V0.02。不改前文。本段不写独占锁行，不重算四值。

### 0. 输入、边界、技能

时间：2026-09-24T07:59:47Z。正本 `/home/james/complex-management`。代码、迁移、契约与测试以 `origin/main` `daf6adfc0bada1e1d972aa8e4499eb117925e244` 为准。工作区 HEAD 仍是 `e456ba96c84185c8d39f18f548011daac48a6466`，相对 HEAD 有未提交改动。本段点名的生产、测试、契约和门禁脚本都用 `git show daf6adfc:` 与 `git grep daf6adfc`。追加前本文件 36451 字节，SHA-256 `df7bea7d80f9347af26e913a0ded98c527a139f69113516b289adfbdf36feeb2`，与候选所记 V0.01 一致。

输入：本席 B1（GR-01…04，FAIL）；席甲 B1 只取 `modules/XM-M034-AB-b1-self-raw.md` 前 49028 字节（SHA-256 `991fc776eb2999e12ed0234d7a7b44ebfaa70708f67a8447e57ac1b2a1b2620a`，与候选所记一致，CX-01…04，FAIL）。候选是工作区 `findings.md` 顶部「当前 XM-M034-AB B2-r1 整批候选（按 D-033「删除」重写…）」（该段不在 `daf6adfc` 上）。D-033 最终答复读 ledger「当前 D-033 改判为「删除」」段：deny、pause、resume 一并删除，两端点整体下线，权限码前向停用，历史行保留。本段只核这批的归并、分类、队列和最小修复边界，不重审模块。

按字节截取席甲 raw 时，49028 字节之后的标题开头进入了预览（「XM-M034-AB B2-r1 整批候选复核（席甲票，V0.02）」及时间戳前缀 `2026-09-24T06:57`）。本席停止续读，下列结论不引用该段，也不引用 V0.03。ledger 派发段写有席甲 V0.03 已 PASS 的一句；不把该句当本席证据。未用 MCP、子代理、第三席。未对人发问。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未对数据库执行语句。未建脚本、日志或临时文件。未跑 Maven，未跑 `npm run verify:predeploy`。下面凡是「门禁会红」都是读 `daf6adfc` 上的脚本后的判断，不是本席刚跑出来的退出码。

| 技能 | 本轮 |
|---|---|
| using-arc | 已读开头。用户限定两席、不派代理、不修。不走 Arc `audit`，不写 `docs/arc/audits/`。 |
| graft | 已读全文。未跑 graft。它索引的是脏工作区，不是 `daf6adfc`。用户要求本席自行 `git grep`。无节省数。 |
| ponytail-review | 已读。用来核最小边界：入口删除后不再留 403 壳，也不为已删除的 POST 再写一套拒绝分支。本段不改代码，没有行数净值。 |
| grilling | 已读。其「问操作员、派子代理查事实」与本轮冲突。事实用 `git grep daf6adfc` 查完。第 7 节是自问自答，不等候。 |
| verification-before-completion、using-superpowers、systematic-debugging、diagnosing-bugs | 本轮未重读这些文件。未把未跑的门禁写成已通过。 |

锁文件本轮 `sha256sum` 仍是 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，与 B1 所记相同。本段不改锁。

### 1. 差集

四条都有去处：GR-01＋CX-01 → 001，GR-02＋CX-03 → 002，GR-03＋CX-02 → 003，GR-04＋CX-04 → 004。没有第五条队列项。

席甲 B1 有两处原判，候选前言已改口，本席在 `daf6adfc` 上复核后采同一改口：

- 两份 OpenAPI 没有 `ticketing/verifications/manual-override`（`git grep` 该字面量在 `docs/openapi` 为零）。CX-01「契约仍有票务路径、附录要改成已下线」不成立。001 不改 OpenAPI，也不把这条路径补进契约。
- 票务入口分类用冲突。路由、服务方法和把 `manual_overridden` 断言为成功的测试还在。V429 只是让权限层拒绝。这仍是在提供该能力。

CX-04 原文分类是细化，最小处置已经是整接口下线。GR-04 分类是添加，处置是先问。操作员按添加项答复「删除」之后，004 保持添加，队列改为「是」，处置落到 CX-04 那套删除，并写上 D-033。这是答复之后收口，没有把「无 HTML 上位」改写成另一条冲突句。

GR-02、GR-03 在 B1 的最小处置是拒绝 `release` / `allow` 并留着其余类型。D-033 把其余类型也删除之后，拒绝分支没有入口可挂。002、003 改为随 004 删除。事实格仍保留清零快照、不写 `released_at`、OpenAPI 枚举无 `release` 却被服务端接受、`allow` 落 `approved`。缺陷没有换。

判据编号有归并：001 写了 BEQR-012、GEN-002、GEN-003、GEN-016；003 写了 BEQR-012、GEN-001、GEN-003；004 写了 GEN-001、GEN-005、GEN-008、GEN-014、GEN-016 与 D-033。GR-01 的 GEN-005、两席的 GEN-017 没有单列。GEN-017 是分类顺序本身，004 的添加分类已经用了这个顺序。没有一条 GR 或 CX 的事实没有去处。

观察：O-01、O-02、O-04 在「只登记」。O-03（DTO 注释里的权限码笔误）没有单独点名；001 删除 `VerificationManualOverrideRequest` 之后该注释没有残留。席甲第 5 节的 `JdbcAdmissionFactRepository:135` 与 V099 惰性策略行在「只登记」。权限矩阵四行分别进了 001（63、251）和 004（78、269）。漏并：无。

### 2. 001 票务写入口

同意。`daf6adfc` 上映射在 `TicketingController.java:498-507`，import 在第 24 行，只被该映射使用。`TicketingService.manualOverrideVerification` 在第 934–937 行，只转给 `TicketVerificationService.manualOverride`（第 207 行起的注释，方法在第 211 行）。`git grep` 这两个方法名，生产侧只有这一条链，测试侧是 `TicketingControllerTests` 四则（第 240、263、282、308 行）和 `TicketingServiceTests` 第 490 行。候选写的五条就是这五则。删方法、删 DTO、删这五则，不留返回 403 的空映射。

V067 第 25 行的 CHECK 仍含 `manual_overridden`。`TicketingJdbcIntegrationTests.java:282` 仍插入该状态。结算仓储第 137 行的排除子句仍点这个词。这三处保持原样。V270、V429 保持原样，不新增票务迁移，历史行保留。OpenAPI 本就没有这条路径，001 不补。

权限矩阵：第 63 行权限格是 `` `verification.manual_override`（停用，待代码轮下线） ``。代码删除后这一格改为写入口已下线，同行其余列保持。第 251 行现在是 `` `verification.reverse`（`verification.manual_override` 停用） ``，句中没有「待代码轮下线」。定稿按括号改：`` `verification.reverse`（`verification.manual_override` 写入口已下线） ``。`verification.reverse` 留在这一格。第 5 行文首是对一批已实现入口的总注，本批不改那一段。

### 3. 002 与 003 是否还要单独拒绝

不需要。D-033 删除 GET 与 POST 之后，没有剩下的改判入口可以返回「类型被拒」。再写 `OVERRIDE_TYPES` 去掉 `allow`/`release` 的校验，是在已删除的方法上留逻辑。002、003 的冲突由整段删除覆盖：`allow` 不能再落 `approved`，`release` 不能再清零。核销执行方法不动。`amusement_manual_overrides` 的历史行、V014 与 V234 的类型 CHECK 不动。

`clearProjectConcurrentCapacity` 的生产调用只有 `AmusementService.java:1598`，位于 `override_type=release` 分支内。接口声明在 `AmusementRepository.java:248`，Jdbc 实现在 `JdbcAmusementRepository.java:1109-1125`（方法体第 1115 行），内存仓在 `AmusementServiceTests.java:3095`。实现者只有 `JdbcAmusementRepository` 与该类内存仓。删除接口方法时这两处实现一并删除，否则 `@Override` 对不上。

`releaseEntryConcurrentCapacity` 是另一方法：接口第 244 行，Jdbc 第 1085 行，出场调用在 `AmusementService.java:1437`，内存仓第 3078 行。002 不动这四处。

出场测试改法恰当，定稿写死范围：

- 删除 `supervisorReleaseClearsConcurrentCapacityWithAudit`（第 147–164 行）。该方法只断言主管 `release` 把占用从 1 打成 0。入口删除后没有「被拒」可断言，保留该方法会继续调用已删除的 `createManualOverride`。
- `sessionBatchCapacityUnaffectedByExitAndSupervisorRelease`（第 223–249 行）只去掉第 243–248 行主管释放段，并改名、改方法注释（第 214–221 行）。第 237–241 行留下：出场后项目维占用为 0、场次 `used_capacity` 仍为 1。类注释第 35、38、42 行里点名主管释放和 `clearProjectConcurrentCapacity` 的句子同步改掉。`freeFlowEntryThenExitReleasesConcurrentCapacity`、`repeatExitReleasesOnlyOnce`、`exitWithoutActiveEntryRejects` 不动。
- `@AfterEach` 第 76 行 `DELETE FROM amusement_manual_overrides` 和第 7 行 `AmusementManualOverrideRequest` 的 import 删除。本类不再插入改判行。`releaseEntryConcurrentCapacity` 的出场覆盖仍在第 99 行与第 237 行两条路径上。

### 4. 004 迁移、删除面、测试与回归

同意整体下线。`daf6adfc` 最新迁移是 V432，V433 这个版本号空着，工作区 `backend/src/main/resources/db/migration` 下也没有 V433。V170 第 180–186 行把 `amusement.verification.override` 又写成 `status='active'`。V433 排在 V170 与 V432 之后，最终状态由 V433 决定。V430–V432 不改这个权限码。

照 V429 第 8–16 行写一条即可，权限码换成 `amusement.verification.override`：`permissions.status='retired'`；删除 `role_permissions`、`permission_template_items`、`user_permission_overrides` 中该码；`temporary_permission_grants` 里 `status='active'` 的行改为 `revoked`。这四处就是 V429 注释说的四处授予。种子里实际有字典行（V014 第 185 行）和平台管理员角色授予（V014 第 213 行）。模板项、用户覆盖和临时授权没有该码的 INSERT，删除与 UPDATE 仍然照写，回归才能守住「以后再授也清掉」。不 `DELETE FROM permissions`，不改 V014、V234、V251、V254、V099。

`git grep daf6adfc` 该权限码、`manual-overrides`、`ManualOverride`、`clearProjectConcurrentCapacity`、`amusement.manual_override`：

- 两端点：`AmusementController.java:39-51`。
- 服务：`OVERRIDE_TYPES` 第 36 行；`listManualOverrides` 第 109–118 行；`createManualOverride` 第 120–133 行；`createManualOverrideInternal` 第 1581–1617 行；返回值 id 分支第 1850–1852 行。该分支删掉，其所在方法保留。
- 仓储：`saveManualOverride`、`listManualOverrides`、`listManualOverridesByMerchant`（第 1371–1409 行）与 `manualOverrideParameters`（第 1996 行）。行映射 `JdbcAmusementRowMapper.mapManualOverride` 第 203 行。
- 两个记录类型文件。Java 引用落在上述生产文件和四个测试：`AmusementControllerTests`、`AmusementJdbcIntegrationTests`、`AmusementServiceTests`、`AmusementExitReleaseJdbcIntegrationTests`。`apps/` 与 `*.kt` 无命中。
- 准入动作字符串只在被删的 `listManualOverrides` / `createManualOverride` 和 V099 第 51 行。V099 保持。`JdbcAdmissionFactRepository.java:135` 只是注释，候选已登记，不进队列。

测试：

- `AmusementServiceTests` 删 `createManualOverrideRejectsCrossMerchantProject`（第 104 行）与 `manualOverrideRequiresReasonAndRecordsOverride`（第 1618 行），以及内存仓第 2311 行的 Map 和第 3095、3105、3111、3243 行的四个实现。
- `AmusementJdbcIntegrationTests`：`permissionCodes()`（第 496–501 行）读全表 `permissions`，不过滤 status。停用后行还在，第 55 行的 `contains` 保持。`platformAdminPermissionCodes()`（第 504–510 行）读 `role_permissions`，第 80 行必须去掉该码，否则 V433 删掉角色授予后这条变红。第 46 行表存在断言保持。第 194–204 行的 `saveManualOverride` 与第 291–293 行的 `listManualOverrides` 断言从 `saveAndListFlowsPersistSafetySessionVerificationAndReadonlyRecords` 里删掉，该方法其余保存与断言保持。第 51 行 Flyway 版本对 `MigrationVersions.latest()`，后者扫描迁移目录，V433 不需要改这个助手。
- 真库回归放在 `AmusementJdbcIntegrationTests` 足够：该类已是 `@SpringBootTest` 并注入 `JdbcTemplate`。断言该码 `status='retired'`，并且四处计数之和为 0，SQL 与 `AccessJdbcIntegrationTests.java:650-658` 同一形状，权限码换成游乐码。`AccessJdbcIntegrationTests` 现有那条只断言 `verification.manual_override`，本批不改。
- `AmusementControllerTests` 候选点了第 64–65 行的桩、第 116–117 行两行、`bodyFor` 第 236–239 行和样例第 391 行。同一文件还有一处没写进候选：`writeRoutesRejectMissingIdempotencyKey` 的 CsvSource 第 195 行仍 POST `/api/v1/amusement/manual-overrides`，期望 400。路径删除后该请求落到未映射，得到 404。定稿补上：删第 195 行。补上之后，本类的改判引用就清完了。

OpenAPI：`openapi-p0.yaml:574-625` 是同一路径上的 POST `createAmusementManualOverride` 与 GET `listAmusementManualOverrides`。响应用的是通用 `Success` / `Page`。`AmusementManualOverrideRequest` 在该文件出现于请求体和第 27211、31122 行的组件，没有第二个记录类 schema。两份契约删路径和这两个组件；`verify:openapi` 要求 json 与 yaml 的 paths/components 深相等，所以两份一起改。`operationId:` 在 yaml 里现为 732。删这两个操作后为 730。

### 5. 开工段外 4 个文件，以及还会被门禁碰到的生成物

四处都核过，都该进写入集合：

- 新迁移 V433：D-033 要求前向停用。必要。
- `docs/p0-api-contract-pack-v0.1.md:334` 只有 POST 一行，写着待代码轮下线。GET 不在这张表里。`tools/` 与 `package.json` 不读这个文件，`verify:predeploy` 不因此变红。行本身仍把已删路径写成现行契约。删这一行，或改成接口已下线。留下「待代码轮下线」与本批事实相反。
- `docs/requirements-code-implementation-matrix-v0.1.csv` 第 49–52 行（REQ-45…48）的 API 列含 `createAmusementManualOverride` 与 `listAmusementManualOverrides`。去掉这两个操作引用，同行其他接口保留。表名 `amusement_manual_overrides` 保留，表还在。代码文件列里的 `AmusementManualOverrideRecord.java` 会变成指向已删文件的模块级定位；该 csv 第 2 行写明此列不是逐条证据。本批不为此重写整行。`verify:final-closed-loop-audit` 读这张表做行数和实现状态计数，不核对 operation 是否还在 OpenAPI，而且它不在 `verify:predeploy` 里。改 csv 是为了矩阵不再把已删接口写成 REQ 的实现依据。
- `tests/e2e/support/mock-api.ts:262` 是该路径的空返回。`apps/` 无调用。`tools/` 里只有 `generate-partial-requirement-mock-coverage.mjs` 点到 `mock-api`，该脚本不在 `verify:predeploy`。删第 262 行，避免模拟层继续发布这条路径。不删也不使 predeploy 变红。

按接口名、操作名、权限码再扫过、本批不改的文件：游乐与票务设计文档、`docs/prd-audit/`、`docs/superpowers/plans/2026-06-14-p0-amusement-safety-session-verification-slice.md`、`docs/p0-action-event-evidence-matrix-v0.1.md:73`、`docs/business-systems-breakdown.md:501`、`docs/operation-traceability-and-terminal-switch-prd-v0.1.md`、`docs/frontend-admin-development-design.md`、`docs/group-governance-rebuild-prd-v1.0.md:69`。这些句子已经写废止或待下线，或是历史方案。闭包生成器只用人工矩阵做「OpenAPI 里的权限/事件是否在正文出现过」的包含判断，人工矩阵多出来的废止行不会让 `--check` 失败。候选把设计文档留在登记里，本席接受。

两份附录用生成器重建是对的。`verify:operation-matrix` 与 `verify:openapi-closure` 都是对应脚本的 `--check`。手改附录会和 OpenAPI 漂移。生成器会去掉这两个 operation 以及 `amusement.verification_overridden`、`amusement.verification_override_failed`、`amusement.manual_override.viewed`、`amusement.manual_override.view_failed` 的接口行。

`verify:predeploy` 在本批改动后的风险点，按脚本原文：

- `verify:matrix-vs-migration` 用迁移里的 CREATE/ALTER ADD COLUMN 对 `docs/matrix-migration-claims.json`。该清单没有 `amusement_manual_overrides`。V433 只有 UPDATE 与 DELETE，不拆列。这一关不因 V433 变红。
- `verify:domain-ownership-scope` 只收集建表、改表和 DROP TABLE。V433 不改变表集合。`docs/domain-ownership-scope-audit-v0.1.csv` 保持，含 `amusement_manual_overrides` 那一行。
- `verify:data-isolation-spec` 会红。`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:59` 写着最新迁移 **V432**、172 张表。生成器把最新版本号写进该句。V433 即使不新增 scope 列，这一句也会从 V432 变成 V433。候选里的「若迁移影响」对域归属清单成立，对这份隔离规格不成立：戳一定会变。跑 `npm run docs:data-isolation-spec`，把该文件带进写入集合。表清单仍含 `amusement_manual_overrides`。
- `verify:p0-contract`：两份 OpenAPI 深相等、示例随路径一起删掉（没有另立的 examples 文件点这两个 operationId）、两份附录 `--check` 通过之前必须重生成。`verify:client-routes` 与 `verify:client-openapi` 扫的是正式客户端；`apps/` 无该路径。mock 那一行不在这两条脚本里。
- `verify:security-baseline` 在 predeploy 里以 `--strict` 运行。它要求 `security-scans/formal-security-baseline-v1.md` 与 `security-scans/findings-register-v1.md` 含有与当前 OpenAPI 相同的 `OpenAPI operation count:`。现在两份第 6 行都是 732。删两个操作后必须是 730。用 `npm run docs:security-baseline` 重生成这两份。只改第 6 行会留下生成器写在后文的计数句。
- `verify:production-blockers` 的生成器不枚举这条路径。`verify:entry-doc-baseline` 不在 `verify:predeploy` 里；`daf6adfc` 的 README 快照仍是 V385/720，本批不把那份快照收进修复。

候选写了「门禁要求的生成物如有变化一并带入」和「跑全链 verify:predeploy」。上面三份（隔离规格、安全基线、findings register）就是会变、并且会卡住 predeploy 的生成物。定稿点名它们。不为此新开 ID。

### 6. 分类与观察

接受。001、002、003 是冲突：001 与 003 对枚举速查第 43 行「系统不提供人工放行」，002 对业务地图第 1263 行不得手工清零。004 保持添加。操作员答复删除，所以工程缺陷为是、修复队列为是，不再留 questions。CX-04 的细化分类不覆盖这个答复路径。

观察登记接受：O-01、O-02 未追完，不进队列；O-04 只说明审计对象用 `daf6adfc` 的 V429；O-03 随 001 删除 DTO 消失；Jdbc 注释与 V099 惰性行不改；权限中文名不改、不为此加迁移；设计文档不改。队列「是」4 条，队列「否」0。

### 7. 添加项与要不要再问

没有新的添加项。删除的是 D-033 已经点名的 deny、pause、resume、两端点、权限码前向停用和历史行保留。GET 不再提供历史查询，历史留在表里。这是「整体下线」那句本身，不再问一次。

不再问操作员的口径：票务 CHECK 与 `manual_overridden` 历史值保留；出场扣减保留；V099 策略行保留；权限中文名保留；设计文档保留。这些都已在两席 B1 和本批候选里定过，本轮事实没有出现新的业务分支。

自问：第 78 行事件列仍有 `amusement.verification_overridden`，而该事件只在被删的 `createManualOverride` 里发出。人工矩阵多一个废止事件名不会让闭包 `--check` 失败。定稿可以在改第 78 行括号时从事件列去掉这一项，留下 `amusement.verification_succeeded` / `amusement.verification_failed`。不做也没有新的写入入口。不单独立项，不问人。

### 8. 对候选 ID

同意 XM-M034-AB-001。分类冲突，工程缺陷，修复队列「是」。五则成功或权限测试删除，不留 403 壳，不改 V067、V270、V429。矩阵第 63 行改写入口已下线；第 251 行只改括号，`verification.reverse` 留下。

同意 XM-M034-AB-002。分类冲突，工程缺陷，修复队列「是」。随整接口删除，不再单写 release 拒绝。`clearProjectConcurrentCapacity` 的删除面只到 release 这一条调用链。`releaseEntryConcurrentCapacity` 不动。出场测试按第 3 节的切分改。

同意 XM-M034-AB-003。分类冲突，工程缺陷，修复队列「是」。`allow`/`approved` 随路径、枚举组件和测试一起删除。核销执行方法不动，历史行不删。

同意 XM-M034-AB-004。分类添加，D-033 删除，工程缺陷，修复队列「是」。V433 照 V429 停用并清四处授予。调用方已按第 4 节列完。真库回归放在 `AmusementJdbcIntegrationTests`，全量权限码清单第 55 行保持，平台管理员角色清单第 80 行去掉该码。定稿补三句，不另开 ID：控制器测试删第 195 行；隔离规格最新迁移戳从 V432 改为 V433；两份安全扫描里的 operation count 从 732 改为 730。第 78、269 行只改 `amusement.verification.override` 的括号，`amusement.verification.execute` 留在格内。

修复队列「是」：4（XM-M034-AB-001…004）。修复队列「否」：0。

## 结论

PASS。

本席 B2-r1 票：PASS。同意候选 XM-M034-AB-001、002、003、004 入修复队列。无新的添加项，无 questions。定稿吸收第 3、5、8 节点名的边界。此票只覆盖本批归并、分类、队列和最小修复边界。B1 的 FAIL 仍在上文，不因本段 PASS 变成模块 PASS，也不代替 B3。

## XM-M034-AB B2-r2 复核（Grok席乙，V0.03）

本文件版本：V0.03。本段追加在文末。文首「版本：V0.01」是 B1 原文，保持不动。本段没有锁哈希字段。

席：Grok，固定只读外审席乙。模型 grok-4.7，推理强度 xhigh。步骤：只复核 findings.md 文首「当前 XM-M034-AB B2-r2 候选」这一批的归并、分类、最小处置和是否还要问人。不重审模块，不代替 B3/B4/B5，不把上文 B1 的 FAIL 或 B2-r1 的 PASS 改写成别的结论。

正本：`/home/james/complex-management`，分支 main。本轮 `git rev-parse HEAD` 为 `e456ba96c84185c8d39f18f548011daac48a6466`。三份已删 Java 在工作区不存在，相对索引为未提交删除（`git status --short` 为 `D`）：`backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java`、`backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRequest.java`、`backend/src/main/java/com/complex/platform/ticketing/dto/VerificationManualOverrideRequest.java`。矩阵 csv 为工作区已修改文件。本段以下路径存在性与点名次数都对这个工作区。

未打开 `modules/XM-M034-AB-b1-self-raw.md`。该文件当前 52786 字节，大于所给 V0.03 边界 51981 字节；不读其后席甲 B2-r2 段，也不引用该段。未用 MCP、子代理或第三席。未问人。未改业务、HTML、锁、findings、ledger。未 fetch、commit、push、merge、deploy。未建脚本、日志或临时文件。

### 技能（只记本轮实际读过或执行过的）

- `using-arc`：本轮已读。用户指令优先于 Arc 流程。本任务是指定的两席只读复核，不转入 Arc `audit` / `review`（那些流程会派评审员）。
- `using-superpowers`：本轮已读开头至 “User Instructions”。用户指令优先。未派子代理，未进入计划模式。
- `graft`：本轮已读并执行 `graft grep "AmusementManualOverrideRecord" --fixed`。1940 个索引文件无命中，输出没有 `[graft] tokens saved` 行。该类已不在索引代码里；矩阵、扫描快照和文档不由这次索引命中覆盖。随后按本轮要求用 `git grep`。
- 本轮未读、也不记成已调用：`grilling`、`systematic-debugging`、`verification-before-completion`、`ponytail`、160 提示词全文。判据原文取自本轮读过的 `docs/audit-2026-09/plan/criteria-lock.md` 中 GEN-002、GEN-016、GEN-017。

### 一、归并与分类

同意。GR-01 单列为 XM-M034-AB-005。来源只记 Grok B5-r1 GR-01。001…004 维持 B2-r1 定稿和已修，队列保持「否」，不重开，不并进 005。

本席 B5-r1（`modules/XM-M034-AB-b5-r1-grok-raw.md`）把第 49–52 行仍指向已删类写成 GR-01，并写明 001–004 的下线复核成立、不因此重开。本轮在工作区重新数过这四行，事实仍在。席甲 B5 的 PASS 被候选推翻；本席未读席甲 B5 raw，矩阵里的假路径是本轮自己数的。

四分类按 GEN-017 的顺序走完：

- 冲突：三份类名在当前产品文档和 HTML 里没有点名要求矩阵必须留下这个类。`git grep` 全仓跟踪文件里，`VerificationManualOverrideRequest` 只出现在 `docs/audit-2026-09/` 与 `docs/prd-audit/`。另外两个类名在 HTML 中无命中。没有一条现行 HTML 句子与「这个 java 文件已经不在树上」相抵。
- 细化：指不出三份 HTML 里哪一格是「相关代码文件」这一列的上位句。这一列是索引。
- 技术性：索引路径失真。不新造业务实体、枚举、状态或流程。无业务发明。技术性仍要过正确性：按第 49 行去打开该类，工作区没有这个路径。
- 添加：不到。D-033 已经决定整体删除。本项是删掉一个已经不存在的路径记号，不是补一条 HTML 没有的业务行为。

工程缺陷：是。冻结判据用 GEN-016 作主判据（文档里的路径须能从当前树打开，不得把旧路径写成当前事实），GEN-002 作旁证（文档把该文件写成当前相关代码，树里没有它）。修复队列：是。这个归并和分类成立。

### 二、最小处置与全仓点名

同意最小处置，四行这一处删完即完整。写入集合只 `docs/requirements-code-implementation-matrix-v0.1.csv`。

工作区该 csv（utf-8-sig，98 行物理行）里，字符串 `AmusementManualOverrideRecord.java` 出现 4 次，都在第 49–52 行，对应 REQ-45、REQ-46、REQ-47、REQ-48。四行「相关代码文件」单元格彼此相同，各 8 个路径，该类都是最后一项，各含恰好一次 `"; backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java"`。全文件该带分号前缀的字符串也是 4 次。`AmusementManualOverrideRequest` 与 `VerificationManualOverrideRequest` 在这张 csv 里是 0 次。

同列其余 7 个路径在工作区都存在：`AmusementOpenEligibilityResponse.java`、`AmusementGroupBookingApprovalRequest.java`、`JdbcAmusementRepository.java`、`AmusementController.java`、`AmusementVerificationRecord.java`、`AmusementSafetyActionRecord.java`、`AmusementSafetyService.java`。缺失的只有待删的这一项。删掉候选写的那一处之后，前一项 `AmusementSafetyService.java` 仍是完整路径，不会留下多余分号。

API 列已经没有 `createAmusementManualOverride` 和 `listAmusementManualOverrides`。数据表列仍有 `amusement_manual_overrides`。该表仍由 `V014__p0_amusement_safety_session_verification.sql` 第 123 行 `CREATE TABLE IF NOT EXISTS` 建立；迁移目录里没有 `DROP TABLE` 这张表（`V234` 第 46 行只 `DROP CONSTRAINT` 旧检查约束）。表名不动是对的。OpenAPI 与 V433 不在本项写入集合里，与本席 B5 的最小处置一致。

证据列仍写「OpenAPI 10 ops; 代码文件 42」。代码文件单元格在删之前是 8 条路径，API 列是 6 个操作，这两个数字本来就不是单元格条目的个数。csv 第 2 行已写明该列是模块级定位、不是逐条证据，证据也是模块级粗判。本项不改这些列。这不构成漏删：它们不点名三个已删类。

全仓点名（本轮自己跑的，不是沿用席甲名单）：

- `git grep -l -E 'AmusementManualOverrideRecord|AmusementManualOverrideRequest|VerificationManualOverrideRequest'` 覆盖跟踪文件。随后按文件计次数。排除审计文档（`docs/audit-2026-09/`）和历史目录（`docs/_archive/`、`docs/prd-audit/`）。
- 排除之后，现行索引里点到已删类、并且是在声称「当前相关代码」的，只有矩阵上述 4 处，且只点 `AmusementManualOverrideRecord`。
- `docs/production-evidence-closure-gap-report-v0.1.md` 第 45 行是一行 144711 字符的表行，开头为 `| Semgrep | results=0, errors=0, paths_scanned=`，后面是逗号拼接的扫描路径清单。其中 `AmusementManualOverrideRecord` 1 次、`AmusementManualOverrideRequest` 1 次、`VerificationManualOverrideRequest` 0 次。这是把 `security-scans/semgrep-latest-summary.json` 的 `paths_scanned` 嵌进报告。该 summary 的 `scan_id` 为 `semgrep-20260703-delivery-gates-clean`。同意只登记、不改。手改这一行会和生成器脱开；不重跑扫描就重生成，清单还是那次扫描的路径。
- 同一次扫描原文 `security-scans/semgrep-20260703-delivery-gates-clean/semgrep.json`，以及 `security-scans/semgrep-latest-summary.json`，两个游乐类名各 1 次，票务类名 0 次。更早的 `security-scans/semgrep-20260623-180915-manual-request/semgrep.json` 同样是两个游乐类名各 1 次。这些是历史扫描目录里的路径清单，不是需求索引。不进入写入集合，不另开 ID。候选用「security-scans 历史扫描」把它们排除出「只此四处」，与本轮归类一致。
- `AmusementManualOverrideRequest` 在排除归档、prd-audit、审计文档和上述扫描之后，只剩缺口报告第 45 行这一处快照。`VerificationManualOverrideRequest` 的跟踪文件命中全部落在 `docs/audit-2026-09/`（findings、ledger、plan 下四份清单）和 `docs/prd-audit/rounds/r11/`。现行矩阵、缺口报告和三份扫描都不点它。
- 未跟踪且未被 exclude-standard 忽略的文件有 269 个。其中点到这三个类名的只有 `docs/audit-2026-09/modules/` 下六份 raw（含本文件与席甲 self-raw）。属审计文档。`grep -l` 只返回路径，本席没有打印 self-raw 的匹配行，也没有打开该文件。

所以：没有漏在现行索引里、还要改的点名。四处删路径覆盖了全部现行假路径。其他文件不改。

### 三、与 B2-r1「本批不为此重写整行」

不构成否决冲突。上文 B2-r1 第 5 节原句是：代码文件列里的 `AmusementManualOverrideRecord.java` 会变成指向已删文件的模块级定位；csv 第 2 行写明此列不是逐条证据；本批不为此重写整行。当时「本批」是 001…004，文件还在树上，删除还是 004 的预定动作。那句拒绝的是为了这个预期悬空去重写第 49–52 整行。那句没有进入当时的 findings 登记，也没有把「文件已经缺失」写成已验收的现状。

本候选不做整行重写。四行只各删末项那一处路径记号，API 列、数据表列、证据列、测试列和表名保持原样。这比 B2-r1 拒绝的动作窄，并且仍然用第 2 行的模块级读法：不把这一列改写成逐条证据。

B5 之后的新事实是：004 已经把文件从工作区删掉，而这四行在去掉两个 operation 时把该类留在了「相关代码文件」末项。现在按文档打开路径会落空。GEN-016 管的是当前路径。第 2 行允许这一列只做模块级定位，不允许模块级定位指向树上没有的文件。005 因此单独立项，001…004 不重开。这是删除落地之后的新事实，不是拿同一份旧证据把 001…004 改判。

### 四、添加项与操作员口径

无添加项。无 questions。不问操作员。

三条已删类里，现行「相关代码」点名只剩矩阵这 4 处 Record 路径，处置已写死。Request 与票务 DTO 没有第二条现行索引要改。缺口报告第 45 行和 security-scans 里的扫描清单保持历史快照。证据列的「OpenAPI 10 ops; 代码文件 42」不点名已删类，也不随这 4 处路径的个数变化，不升成新 ID，不据此去改其他列。表保留、历史行保留、不改 OpenAPI、不改 V433，都沿用已定稿的 001…004，本轮没有出现新的业务分支。

### 对 XM-M034-AB-005

同意。分类技术性，无业务发明。工程缺陷是。修复队列是。最小处置就是四行各删「; backend/src/main/java/com/complex/platform/amusement/AmusementManualOverrideRecord.java」一处，只写这一张 csv，其余列、表名和其他文件不动。GEN-016、GEN-002 用得对。

## 结论

PASS。

本席 B2-r2 票：PASS。同意 XM-M034-AB-005 入修复队列。001…004 维持已修、队列「否」、不重开。无添加项，无 questions。此票只覆盖本批归并、分类、队列和最小处置。上文 B1 的 FAIL 与 B2-r1 的 PASS 都保持原样。
