CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M006 B5-r1 席乙完整模块审计（修复后全量）

版本：V0.01  
席位：Grok（审计席乙）  
步骤：B5-r1  
模块：M006 — 治理门禁与 CI 基线／文档与接口定义／第1段  
计划：当前 471 模块计划  
生命周期：IN_PROGRESS（本席不改 ledger）  
B3 已执行次数：1  
控制提示词：160-audit-prompt.md V0.17  
runbook：docs/audit-2026-09/plan/runbook.md V0.02  
结论：**PASS**

独立实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（64 位小写 hex）。sidecar `plan/criteria-lock.sha256` 与 ledger 顶部同值。本席未使用派发指令中的期望哈希。

## 1. 范围、源码指纹、独立性

- 文件仅 `plan/modules.md` M006 FILES：`docs/delivery-readiness-gates-v0.1.md`（当前 158 行，标题 V0.11）、`docs/deploy-and-change-flow-v0.1.md`（当前 48 行，标题 V0.07）。合计 206 行。冻结计划 207 为修前计量，断行修复少 1 行，不改冻结计划/scope/lock。
- git HEAD：`242f487233d028173cb9198eecd079f5d877405c`。两文件工作区已改（B3-r1 修 039–043，未提交，符合阶段 B 不提交）。
  - delivery 工作区 SHA256 `7c120e8ed537189c8e4abf898be075d81ff3d3c6bc6a245b46380f0e54f37089`，blob `88a00e9a004e25656cf1b294fa48efc56aada458`（HEAD blob `0fbd20a021ad22ab561ad547b414df1212a75151`）
  - deploy 工作区 SHA256 `543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1`，blob `febbed4b0387288609fdd801a634e13404a910f7`（HEAD blob `c57db4573890015bb6b5de4e8dbd5f788783c009`）
- 全文阅读上述两文档；完整覆盖冻结映射 79 项（GEN-001–018 + 37 BEQR + 14 BSM + 10 PGM），不抽样、不只核刚修 5 项或 diff。
- 可读：`findings.md` V1.03 顶部当前 M006 定稿/出队段（037/038/044/045/046 队列否；039–043 历史缺陷是、已修出队）。禁止并以本次未读：本次 B5 self、旧同号 backend 报告 `M006-10-backend-main-access-01.md`、其它历史 raw/模块收口。旧 001–036 不回退、不复用。
- 未派子代理、未用 MCP、未修源/HTML/台账、未跑 docs 生成器、未 GitHub PUT、未发布、未 git 提交/推送、未跑 M007/M008 整模块或 `verify:delivery-gates` 全链。
- 本 raw 先前不存在，本次创建。除本路径外未写其它文件。

## 2. 技能预检（V0.17 §6，本节点实核）

按意图执行；未核到的不写已调用。B5 禁止修复，TDD 本体落测试作废。

| 技能 | 本席环境 | 本步用法 |
|---|---|---|
| using-superpowers | 已读 | 开工技能路由 |
| verification-before-completion | 已读 | PASS 前只认本步命令输出 |
| grilling / grill-me | 已读 | 只借追问结构，事实自读，不等操作员、不派子代理 |
| ponytail-review | 已读 | 找可删；本模块无死目录/无消费标记 |
| codebase-design | 已读 | 两文档是深工具上的浅接口目录 |
| diagnosing-bugs | 已读 | 无现行缺陷可建红环；用于核旧修是否仍红 |
| writing-for-agents | 已读 | 报告结构：步骤+完成条件 |
| systematic-debugging | 目录存在 | 只借读错→复现→数据流；不改源 |
| test-driven-development / tdd | 已读 | 文档/配置修复不强制测（类型 4）；本步不修 |
| requesting-code-review | 已读 | 只派本席；不派子代理、不用 HEAD~1 |
| receiving-code-review | 已读 | 只借先核实再接受；本步不 IMPLEMENT |
| resolving-merge-conflicts | 目录存在 | 无合并，未用 |
| git-guardrails-claude-code | 存在 | 可选；会写 hook，本步不用 |
| batch-grill-me / ubiquitous-language | **缺口** | 本环境未找到，不冒充已调用 |
| Arc audit 专席 | 存在但要派子代理 | 用户禁子代理，记缺口，本席本地三线代替 |
| ponytail-audit/debt/gain、research、executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch | V0.17 划掉 | 不用 |

## 3. 第一性原理

### 3.1 `docs/delivery-readiness-gates-v0.1.md`

1. **为何必须存在 / 删除代价**  
   删除后：BSM-116/117/118 要求的「正式上线安全加固 + 安装交付」会与「仓库最低交付准入」混成一件事，操作员无法区分工程闸口与 HTML 上线必做；PGM-066「第三方接口资料当前未确定」与外部依赖不得伪实现的机器入口会失去本文件这份目录。答得上业务上位（上线/外部未定/不得伪闭环），故不是无主文档。删除代价：`tools/verify-delivery-gates.mjs` 的 `requireIncludes` 对本文 65 个标记/命令字符串会立即失败，父脚本 `verify:delivery-gates` 与部署 REQUIRED_GATES 中的 `verify:delivery-gates-docs` 会红。

2. **声称行为是否实际执行**  
   文档类型 4。本席独立：真实 Markdown 解析两表（T1 38×2 列全对齐；T2 7×2 列全对齐）；102 处 npm 引用全部存在于 `package.json.scripts`（68 个不重复名）；T1 全部命令均在父串 `verify:delivery-gates` 内；父串多出的 `verify:unit` 等由 L63「完整清单以 package.json 为准」收口，且 `verify:partial-requirement-mock-coverage` / `verify:production-no-go-command-center` 在后文/附录出现。直接消费者 `npm run verify:delivery-gates-docs` exit 0，输出 `DELIVERY_GATES_DOCS_PASS`。**不把该 PASS 当作表格正确**：机检只做 `includes`，表格行列由本席解析器另证。当前 JSON：`partial_requirements=8` 且 `requirement_mock_coverage` 长度 8；追踪 JSON 同样 8 条，`traceable_partial_requirements=8`。runtime 默认 30 天与 `tools/generate-runtime-acceptance-evidence.mjs` `Number(process.env.RUNTIME_EVIDENCE_MAX_AGE_DAYS ?? 30)` 一致。t3d 机检字面量含 `loopback-only`。编码主机本机 `hostname -I` 含 `192.168.88.160`，与 `verify-coding-host-protection.mjs` 默认 IP 一致（未跑该脚本全链）。

3. **未验证前提**  
   - 未跑 `verify:delivery-gates` 全链、未跑各 `docs:*` 生成器。  
   - 追踪快照 8 条当前 `mapped_item_ids`/`owner_groups`/`target_manifest_paths` 为空，走 `switch_disabled_item_ids` 替代（生成器明确 `mapped 空 + switch_disabled 非空则跳过 manifest 路径失败`）。L96 目的句与生成器自述同口径，L149–150 同时写明不关闭 blocker、mock 不得当生产闭环。不把空 manifest 写成已回填。  
   - 未查询生产库，不验证「complex_test 与生产同实例」；该句在 deploy 文是仓外待办，不是已闭合声明。

### 3.2 `docs/deploy-and-change-flow-v0.1.md`

1. **为何必须存在 / 删除代价**  
   删除后：BSM-116 上线动作与仓内「部署前硬闸 / 禁直推 main / CI 可见信号 / 合并钉 SHA」四道工程门禁的诚实边界会回到「AI 直推 main、红着也能合」的已发生失败模式。上位是交付/变更控制，不是新业务实体。删除代价：部署脚本注释与 `propose-change.sh`/`merge-pr.sh` 失去操作说明书；CODEOWNERS/workflow 镜像约定无人维护。

2. **声称行为是否实际执行**  
   - 部署：`complex-deploy-api.sh` / `complex-deploy-web.sh` 各 `REQUIRED_GATES=(...)` **15** 项硬编码，循环 `npm run "$gate"`，非 0 `exit 1`；数组内不含对 `package.json` 的 `verify:predeploy` 串解析。L6/L36「逐项执行固定的预部署门禁清单」与源码一致。  
   - pre-push：17 行，只拦 `refs/heads/main`，`ALLOW_MAIN_PUSH!=1` 则 exit 1；不跑 npm。与 L8/L24 一致。  
   - 合并：`merge-pr.sh` 有 `gh run watch --exit-status`、等待后再核 `remote_now`/`pr_head`、`gh pr merge --match-head-commit`、`SKIP_CI_WAIT=1` 醒目警告。与 L10/L17 一致。  
   - `propose-change.sh` 先 `npm run verify:predeploy`。与 L13 一致。  
   - workflow：`.github/workflows/predeploy-gates.yml` 与 `ops/ci/predeploy-gates.yml` `cmp` 字节一致；`fetch-depth: 0`；步骤 `npm run verify:predeploy`；`HTML_GATES_BASE` 为 PR base / push before。只读 `gh api GET repos/jxh1979/mall/actions/workflows/predeploy-gates.yml` → `state: active`。  
   - 分支保护：只读 `gh api GET repos/jxh1979/mall/branches/main/protection` 返回 HTTP 403，`Upgrade to GitHub Pro or make this repository public`。**不把 workflow active 或任何 loopback 假 token 捕获当成已开通 GitHub 保护。** 文档 L20/L43–45 与 403 一致。  
   - L45 `gh api -X PUT ...`：本席 `shlex` 解析，`-F` 用于 `strict=true`（JSON bool）、`enforce_admins=true`、`required_approving_review_count=1`（JSON int）、`restrictions=null`；`-f` 仅 `contexts[]=predeploy` 字符串。`gh api --help`：`-F/--field` typed，`-f/--raw-field` string。未执行 PUT。

3. **未验证前提**  
   - 未复跑 2026-07-06「首跑 SUCCESS」历史 run；当前只采信 workflow `state=active` + 仓内 YAML。L39 历史首跑不当作本次 PASS 依据。  
   - 未验证 CODEOWNERS 在 GitHub 上是否 enforced（私有免费版无 required review）；仅确认文件存在。文档已写 CODEOWNERS「仍非服务端强制」。

## 4. 四分类（冲突 → 细化 → 技术性 → 添加）

全文两文档无与三 HTML **明文矛盾** 的冲突项（§4 已显式指向「系统交付与上线」且声明工程闸不能替代；§3 外部 pending 不得宣称生产闭环；AI 负向约束不禁止 HTML 已有的非 AI 自动规则如超时升级）。

| 段 | 分类 | 理由（HTML 原文或无业务发明） | 工程缺陷 |
|---|---|---|---|
| delivery L1–8 定位 | 细化 | 上位 BSM-116 正式上线要专门把关；本文只做仓库最低准入，不替代上线加固 | 否 |
| delivery L9–53 机器标记 | 技术性 | 供 `requireIncludes`/其它 verify 消费的内部标识；无票务/价格/状态发明 | 否 |
| delivery L55–105 命令表 | 技术性 | npm 入口与夹具目录；L63 以 package.json 为完整清单 | 否 |
| delivery L107–113 阻断规则 | 细化 | PGM-066 第三方资料未定；BSM-116 上线必做；不得伪实现 | 否 |
| delivery L111 MDM | **细化** | 见 §6，HTML 企业级手机统一管控 | 否 |
| delivery L113 AI | **细化** | 见 §6，HTML 待人工确认建议 | 否 |
| delivery L115–127 与完整验证关系 | 细化 | BSM-116/117/118 强密码、U 盘、安装包+教程 | 否 |
| delivery L129–140 失败处理 | 技术性 | 恢复步骤指向已有 docs:/verify: 入口，本席不执行生成器 | 否 |
| delivery L141–151 partial 报告 | 细化 | 8 条 partial 与 JSON 一致；明确不关闭 blocker | 否 |
| delivery L151 对象存储 | **细化** | 见 §6，HTML 永久存储/长期保存/附件证据 | 否 |
| delivery L152–158 附录 | 技术性 | 指挥板标记另由 no-go 生成器/校验消费 | 否 |
| deploy 全文 L1–48 | 技术性 | 四道工程门禁、仓外限制、workflow 镜像；不改业务最高权限 | 否 |

无添加项，无 HTML 自身问题，不生成 questions。037/038 无缺陷登记成立。044/045/046 本席独立裁定仍为细化、无工程缺陷，不因缩写未出现而改判添加，也不用「技术性」掩盖新业务。

## 5. 顺从式：79 映射逐项

判据全文已从 `criteria-lock.md` 定义区读取；HTML 原文另用三份 HTML 现文核对锁内摘录 61/61 命中。下列「适用」= 本模块两文档必须满足；「作用边界」= 本文不实现该业务状态机，只审不冲突、不发明。

### 5.1 GEN-001–018（18）

| ID | 适用 | 证据 | 结果 |
|---|---|---|---|
| GEN-001 | 适用 | 两文档删除代价见 §3；标记均有消费者；ponytail-review：无死命令/无未消费路径 | PASS |
| GEN-002 | 适用 | 表命令均在 scripts；部署 15 项数组实跑入口存在；gh 字段类型与 help 一致；doc gate 绿但不替代表解析 | PASS |
| GEN-003 | 作用边界 | 本模块无测试文件；文档类型 4；不把 doc gate 绿当表正确 | PASS |
| GEN-004 | 适用 | L45 示例为仓外操作员动作且标明未执行；本席只 GET。文档不把假 token 当已保护 | PASS |
| GEN-005 | 适用 | 文档诚实：hook 可 `--no-verify`；保护 403；SKIP_CI_WAIT 须写原因。未把可见信号写成硬阻断 | PASS |
| GEN-006 | 作用边界 | 无状态机写入；不声明账务守恒 | PASS |
| GEN-007 | 适用 | merge-pr 等待期间 SHA 变化即中止 + `--match-head-commit`；幂等合并钉已验提交 | PASS |
| GEN-008 | 适用 | workflow 双份字节一致；父串/表/REQUIRED_GATES 角色分离（delivery 父串 vs 部署 15 项快子集）已写明 | PASS |
| GEN-009 | 适用 | 失败节「不要跳过闸口、不要改脚本掩盖」；部署红 exit 1；CI 非成功中止并打 run URL | PASS |
| GEN-010 | 适用 | 明文密钥/原始证据不入库；t3d 脱敏+loopback；PUT 示例未执行 | PASS |
| GEN-011 | 适用 | 机器标记被 verify-delivery-gates 及其它 listed verify 消费；附录 NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE / NAS_REQUIRES_IT_SUDO_MAINTENANCE_WINDOW 由 no-go 工具消费 | PASS |
| GEN-012 | 作用边界 | 文档无无界批处理；runtime 超期默认 30 天有上限 | PASS |
| GEN-013 | 适用 | 脚本名均可在 package.json 复现；部署不解析可被抽空的父串 | PASS |
| GEN-014 | 作用边界 | 无迁移文件 | PASS |
| GEN-015 | 作用边界 | 两文不是后台/客人/设备/安卓界面；PGM-066 全中文约束的是终端展示。命令名/标记属系统内部标识。无 039 类混语残渣（`?requires` 已不存在） | PASS |
| GEN-016 | 适用 | 14 条引用路径均存在；版本标题 V0.11/V0.07；workflow active；保护 403；8 条 partial 与两份 JSON 一致；未把历史首跑当当前 PASS | PASS |
| GEN-017 | 适用 | §4 顺序分类；044/045/046 用 HTML 条目而非缩写缺席 | PASS |
| GEN-018 | 适用 | 本报告依据仅 HTML 原文、现文、官方/gh help、可复现命令。席位意见不当来源 | PASS |

### 5.2 BEQR（37）— 作用边界为主

本模块不实现游乐/地产/招商状态机。两文档检索：无票种、容量、场次、巡检任务状态、铺位、抽成、营业日配置等发明。delivery 仅在 AI 负向句与证据回填列举中出现「核销/退款/处罚/绩效/调价」，不定义其业务规则。

| ID | 锚点摘要 | 作用边界 | 结果 |
|---|---|---|---|
| BEQR-085 项目开放状态 | 未开放…异常 | 不实现开放状态；无冲突 | PASS |
| BEQR-086 场次状态 | 未开始…异常 | 同上 | PASS |
| BEQR-087 当天/每天口径 | 按营业日 | 无营业日改写 | PASS |
| BEQR-088 容量双阈值 | 软/硬上限 | 无容量规则 | PASS |
| BEQR-089 园区容量 | 指向双阈值 | 同上 | PASS |
| BEQR-090 设备关键/辅助 | 故障停机范围 | 无设备分级 | PASS |
| BEQR-091 全园故障与补偿通票 | 补偿规则 | 无票务补偿 | PASS |
| BEQR-092 安全严重度 | 提醒…重大 | 无该枚举 | PASS |
| BEQR-093 巡检任务状态 | 草稿…取消 | 无游乐巡检 | PASS |
| BEQR-094 越界类型 | 物理/经营范围 | 无该枚举 | PASS |
| BEQR-095 团体/包场 | 批量/整场 | 无该枚举 | PASS |
| BEQR-096 游乐增强阶段 | P2 不得绕过核销审批 | AI 负向句不授权 P2 自动核销，与「不得绕过」同向 | PASS |
| BEQR-118 运营营业日 | 10:00 切日 | 无切日规则 | PASS |
| BEQR-146 运营公司职责 | 域主管公司 | 无收入归属 | PASS |
| BEQR-155 商户/运营主体状态 | 待准入…黑名单 | 无商户状态 | PASS |
| BEQR-160–162 招商线索/意向/品牌库 | 待跟进…黑名单品牌 | 无招商状态机 | PASS |
| BEQR-163 合同变更类型 | 续租…位置变更 | 无合同变更 | PASS |
| BEQR-164 退租结算状态 | 待结算…诉讼 | **锁 ID 不是 HTML:164**。HTML:164 是「高价值判定」含「录像与分析结果长期保存」，作 045 上位，见 §6。本 ID 业务状态机本模块不实现 | PASS |
| BEQR-165 内部应付执行 | 待付…已结清 | 无付款状态 | PASS |
| BEQR-166 合同审批链 | 主线+平行线 | 无审批链配置 | PASS |
| BEQR-167–169 租金/抽成/公区能源 | 账单类型 | 无计费 | PASS |
| BEQR-170 铺位租赁状态 | 空置…不可租 | 无铺位 | PASS |
| BEQR-171–174 物业/商户工单与时效 | 工单枚举 | 无工单 | PASS |
| BEQR-175–181 楼宇机电/维保/表计/抄表/归集/巡检 | 楼宇运营枚举 | 无楼宇台账；NAS/备份是运维手段不是机电状态 | PASS |

### 5.3 BSM（14）

| ID | 适用 | 证据 | 结果 |
|---|---|---|---|
| BSM-020 楼宇运营入口 | 作用边界 | 卡片「机电/能耗/巡检」；本文 NAS 是 IT 挂载而非楼宇机电 | PASS |
| BSM-028 基础管理主要功能 | 作用边界 | 七类红线/合同/主体；本文不改红线、不发明审批档位 | PASS |
| BSM-081–083 楼宇运营专章 | 作用边界 | 不实现抄表/巡检状态 | PASS |
| BSM-111–113 经营看板 | 作用边界 | 无指标定义 | PASS |
| BSM-114 各类入口 | 作用边界 | 本文不是这些 UI | PASS |
| BSM-115 相互关系 | 部分适用 | 「工作手机连设备管控：正式发放的受管工作手机…企业级手机统一管控」是 044 上位；本文只把未落实管控列为生产待证，不新造设备权 | PASS |
| BSM-116 系统交付与上线 | **适用** | 现文 h2 L1928 与 delivery L117 逐字对应「系统交付与上线」 | PASS |
| BSM-117 正式上线安全加固 | **适用** | HTML：换强密码、U 盘密钥托管、留痕。L117 列出且声明工程闸不能替代 | PASS |
| BSM-118 安装包+教程 | **适用** | HTML：自动识别环境的安装包 + 业务语言教程。L117 列出，未声称已交付 | PASS |
| BSM-119 和谁有关 | 适用 | 上线动作连留痕；本文不关闭留痕义务 | PASS |

### 5.4 PGM（10）

| ID | 适用 | 证据 | 结果 |
|---|---|---|---|
| PGM-015 楼宇运营卡片 | 作用边界 | 同 BSM-020 | PASS |
| PGM-018 物理项目与运营公司 | 作用边界 | 无主体拓扑改写 | PASS |
| PGM-026 商户主要功能 | 作用边界 | 无招商/四类主体绑定 | PASS |
| PGM-039 智能设备场景联动范围 | 适用边界 | HTML：独立立项前不定义设备动作/自动执行。本文不定义设备动作；交付闸不把设备伪闭环 | PASS |
| PGM-052–056 楼宇专章 | 作用边界 | 不实现机电/能耗/巡检 | PASS |
| PGM-066 已拍板规则 | **适用** | HTML 现文：第三方接口资料未定；企微永久存储；界面全中文（终端）；留痕。本文外部 pending、对象存储证据回填、不把 mock 当生产闭环，与「资料未定先自有系统」同向 | PASS |

**79/79 已核对。无 FAIL 项。**

## 6. 044 / 045 / 046 独立裁定（核三 HTML 原文，不把席位意见当来源）

### M006-044 MDM — 细化，缺陷否

HTML 现文：

- `platform-governance-map-for-operations-v0.1.html:619–620`：「工作手机预装软件清单和网站白名单由后台维护。」「通过企业级手机统一管控手段阻止违规访问并留痕。」
- `business-system-map-for-operations-v0.1.html:949`：「正式发放的受管工作手机不允许安装除预装软件之外的任何软件，只允许访问白名单网站；清单由后台维护，通过企业级手机统一管控手段拦截违规访问。」
- 同文件约 L1978 已拍板同句。

BEQR「接入的外部系统」（约 L208）未写英文字母 MDM，但 GEN-017 允许用具体 HTML 条目核分类，**不能只因缩写没出现判添加**。delivery L111 把 MDM 列为 `pending_info` 外部能力之例，不新增手机使用权、设备类别或对接业务。**细化。** 工程上只是生产证据未闭合的诚实阻断，无缺陷。

### M006-045 对象存储 — 细化，缺陷否

三 HTML 无「对象存储」四字。上位现文：

- PGM `html:590–592`：企业微信工作会话永久保存；「平台使用企业微信已有会话能力，统一同步、存储、质检和审计。」
- PGM-066 锁摘录/现文：永久存储、不自动到期删除。
- BEQR 高价值判定（HTML **行** 164，不是锁 ID BEQR-164）：「录像与分析结果长期保存（业务参数…出厂默认至少五年…）」
- BSM 高价值物品（约 L1195）同句。
- BSM 留痕（约 L1613）：「必要时关联视频、企业微信会话、附件或备注作为证据。」

delivery L151 只要求真实存储证据按 production evidence manifest 回填，不定义新存储实体、保留期、清理权限或供应商。**细化（现有留存/附件的工程载体）。** 无缺陷。

### M006-046 AI 自动业务决策限制 — 细化，缺陷否

HTML 现文 PGM `html:532`：「P0 可引入本地 AI 大模型做辅助分析判断：对合同文本…给出提示；但 AI 结果只作为“待人工确认建议”，不得自动替代合同台账、审批、阻断或放行结论。」

另：BEQR「智能采购建议」不得自动向供应商下单；BEQR-096 P2 不得绕过核销/审批。

delivery L113 全句：「独立 Python AI 服务在 T3-D2 前继续阻断；当前允许的是 160 本地大模型只读建议能力，不允许 AI 自动处罚、自动退款、自动调价、自动核销或自动影响绩效。」

裁定：

- 合同域「建议待人工确认、不得自动替代台账/审批/阻断/放行」是上位。160 本地大模型只读建议是该原则在质检路径上的下位实现。独立 Python 服务阻断是供应商/工程闸（与 PGM-066 资料未定同族），不是新处罚规则。
- 列举处罚/退款/调价/核销/绩效是「不得自动替代业务决定」的副作用清单，**没有创设**这些业务的新枚举、价格或流程。HTML 已有退款五种情形、核销规则、运营人员填写处罚记录等，本文只禁止 **AI 自动执行**。
- 仓库「摄像内容须做人工智能分析…发现盗窃的开除」是分析取证 + 既有人事后果，并非授权 AI 自动处罚；L113 不禁止该分析义务，也不禁止非 AI 的确定性自动规则（超时升级等）。
- 因此不是添加，也不是把合同域句扩成全平台新禁令业务。**细化。** t3d 文档/机检含 `AI_OUTPUT_NO_AUTO_DECISION` / `loopback-only`，与只读建议一致。无工程缺陷。

## 7. 对抗式（grilling 结构，自问自答）

Q1 表是否仍损坏？→ 解析器 T1 38 行×2 列、T2 7 行×2 列全对齐；无「顺序」列；L92 两命令同一单元格仍两列。  
Q2 命令是否幽灵？→ 102 引用 0 缺失。  
Q3 doc gate 绿是否等于表对？→ 否。`requireIncludes` 只做子串。表正确靠解析器。  
Q4 父串多出的命令是否隐瞒？→ L63 写明完整清单以 package.json 为准；partial/no-go 在后文。  
Q5 GitHub 保护是否已开？→ GET 403。未 PUT。未把 loopback 当已保护。  
Q6 部署是否仍信 package.json 父串？→ 否，15 项数组。L36 动词已改。  
Q7 40/31 固定计数？→ 正则无命中。  
Q8 gh `-f` 是否仍全是字符串？→ `-F` typed / `-f` 仅 context 字符串。  
Q9 混语残渣 `?requires`？→ 无。  
Q10 是否把 mock 写成生产闭环？→ L149–150、§3.3 pending_info 明确禁止。  
Q11 是否发明 MDM/对象存储/AI 业务？→ §6 否。  
Q12 是否与上线 HTML 冲突？→ L117 显式要求强密码/U 盘/安装包，并声明工程闸不能替代。  
Q13 hook 是否被写成安全边界？→ 文档写明可绕过。  
Q14 历史首跑 SUCCESS？→ 不采信为本次 PASS；只采信当前 workflow active。  
Q15 可删内容？→ ponytail-review：Lean already. Ship. net: -0。标记均有消费者。

最小反例：越权（保护未开，文档未谎称已开）；重复合并（match-head-commit）；失败重试（SKIP_CI_WAIT 警告）；配置漂移（workflow 镜像机检）；文档假完成（上线 HTML 未声称已做；pending_info 阻断）。金额精度/状态跳转不适用这两份文档。

## 8. 039–043 修后核验（B5 全量的一部分，不是只审 diff）

| ID | 修前缺陷 | 当前实证 | 队列 |
|---|---|---|---|
| 039 | 坏表/顺序列/父命令/混语 | 两列表、L63 父清单、L96 只 verify、无 `?requires` | 否（已修） |
| 040 | 40/31 固定计数 | 无「40 份」「31 个」 | 否 |
| 041 | gh `-f` 类型 | `-F` bool/int/null，`-f` 仅 context | 否 |
| 042 | 部署「跑 verify:predeploy」 | 现文「固定清单」；脚本 15 项数组 | 否 |
| 043 | §4 未接 HTML 上线 | L117 接 BSM-116–118 | 否 |

## 9. Findings

无新临时 GR。无未定稿缺陷。无未答 questions。044/045/046 维持细化/缺陷否/队列否。

## 10. 验证限制

- 类型 4：Markdown 解析、path、package.json、只读脚本/API 消费。未部署、未合并、未写生成器。
- 命令先确认无源写；`verify:delivery-gates-docs` 只读 `fs.readFileSync` + 比较。
- `gh api` 仅 GET；403/active 为现文。
- 未扫 `docs/_archive`、`.scratch`、`docs/audit-2026-09` 作业务证据（控制提示词/本 raw 只读例外）。
- 直接消费者只读到能证本文声称为止，不审 M007/M008 整模块。
- 未跑后端测试/全父链。

## 11. 结论

**PASS**

本模块两文件均有覆盖记录；79 映射均已核对；三线均执行；声称均有 HTML 原文、现文产物、gh/官方 help 或可复现命令之一；无未答 questions；无未定稿缺陷；本报告第一行哈希有效。
