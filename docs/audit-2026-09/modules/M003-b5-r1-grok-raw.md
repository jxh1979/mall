CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B5-r1 Grok 全量独立审计报告

自身版本：V0.01
席位：Grok（审计席乙）
步骤：B5-r1（修复后整模块重审，不只看 diff）
模块：M003 `01-root-config-and-guidance-01`
控制：V0.17；ledger 状态 `IN_PROGRESS`；B3 累计 1
本次结论：**FAIL**

本席独立实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 如上。未使用派发期望锁值。sidecar 与 ledger 顶部同为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，`LOCK-AMEND-COUNT: 0`。本席不改 lock/ledger/findings/源码。

未读本次 self raw、其他席分析正文、B4 结论正文。未派子代理。检索限定 8 源文件、三份 HTML 及 README/AGENTS/PROGRESS/.env.example 点名的直系引用路径。发现只记录，不修。

---

## 1. §6 技能预检（本席自核）

实际读取并按收窄口径使用：

| 技能 | 路径 | 本次用法 |
|---|---|---|
| `using-superpowers` | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `verification-before-completion` | 同套件 | PASS/FAIL 前先跑命令 |
| `systematic-debugging` | 同套件 | 只借读错误→复现→追数据流；不改码 |
| `grilling` / `grill-me` | mattpocock productivity | 只借追问结构，自问自答，不派子代理、不等操作员 |
| `codebase-design` | mattpocock engineering | 根配置/文档接口深度与删除测试 |
| `diagnosing-bugs` | 同 | 文档假完成与配置漂移的可失败信号 |
| `writing-for-agents` | mattpocock productivity | 审 `AGENTS.md` 指针与完成标准 |
| `ponytail-review` | ponytail 4.9.0 | 找可删/应收窄；不改码 |
| `i-have-adhd` | cache 0.1.0 | 输出形态；本文件首行被锁哈希格式覆盖 |
| `receiving-code-review` | superpowers | 只借先核实再判定；B5 禁止修复 |
| `tdd` / `test-driven-development` | 存在 | 文档/配置不强制落测试；未写测试 |
| `resolving-merge-conflicts` | 存在 | 无合并冲突，未调用本体步骤 |
| `git-guardrails-claude-code` | 存在 | Claude Code hook 安装技能；本席无 git 写，未安装 |
| `ponytail` | 存在 | B5 非修复，不启用 ultra 改码 |

缺口（不得冒充已调用）：

- `batch-grill-me`：本环境未找到技能目录
- `ubiquitous-language`：本环境未找到技能目录
- `grill-with-docs`：目录存在但不在 §6 必核表，未调用

明确不用（§6 划掉）：`executing-plans`、`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`finishing-a-development-branch`、`research`、`ponytail-audit`、`ponytail-debt`、`ponytail-gain`、`domain-modeling`、`code-review`(mattpocock)、`wait-what`。

---

## 2. 输入清单（本次实算）

权威有效行数命令：

```
git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
```

输出：`621`

`modules.md` 仍写 625（修复前切分值）。本次以 621 为准。

| 路径 | 字节 | 行 | SHA256 |
|---|---:|---:|---|
| `.env.example` | 893 | 22 | e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049 |
| `.gitignore` | 1199 | 55 | 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b |
| `.semgrepignore` | 253 | 6 | 45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba |
| `AGENTS.md` | 3662 | 74 | 0635c31192e2bb20338e8091fbd40399f854fe722009683b2f0298298a81f14c |
| `CONTEXT.md` | 10197 | 163 | d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7 |
| `PROGRESS.md` | 3246 | 36 | 91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d |
| `README.en.md` | 7492 | 116 | e3efb3f26bb3785ad9a31adb214515f5df6f5ff08ed79a90b4ed9224abb64355 |
| `README.md` | 10871 | 149 | 7687e5c1f376ac623243652de20b82c664d0fb694558f6402067700919ef0e4e |
| **合计** | **37813** | **621** | — |

`git status --porcelain`：6 文件 `M`（`.env.example` `AGENTS.md` `CONTEXT.md` `PROGRESS.md` `README.en.md` `README.md`）；`.gitignore` `.semgrepignore` 未改。HEAD=`df580198172b2c497ef3cb7e25d2e8e5dc058920`=`origin/main`。

criteria-lock 映射（自读，未扩 HTML 派生 ID 到本模块）：

`M003 → [GEN-001 … GEN-018]` 共 18 条。本模块不映射 BEQR/BSM/PGM 判据 ID；三份 HTML 只作 2.1 上位对照。

findings V0.37 当前 24 ID 修复队列全部为「否」。questions：`M003-023` 答复首行 `删除`；`M003-024` 答复首行 `保留`。无待批问题。

---

## 3. 18GEN 逐项覆盖

| ID | 本模块落点 | 证据 | 结论 |
|---|---|---|---|
| GEN-001 | 8 文件存在必要性见 §5 | 删除测试 + HTML/工程入口对照 | 8 文件均有用途；PROGRESS 为历史台账，时态已标明 |
| GEN-002 | npm 脚本键、backend-env、基线 --check | 脚本键全在；`verify:backend-env` 在 Temurin JAVA_HOME 下 exit 0；未跑全量 mvn/E2E | 入口声明与键一致；全量测试未跑，不拿未跑当过 |
| GEN-003 | 本模块无测试文件 | 文档/配置按 runbook 不强制造测试 | 不适用强制测试门槛；README 测试数字走快照口径 |
| GEN-004 | `.env.example` 输入样例 | 本地口令标明勿用于生产；`.env` 被 ignore | 样例密钥属本地夹具，未当生产秘密入库 |
| GEN-005 | AGENTS 铁律 6；CONTEXT 权限/高危/存档 | HTML 逐字 | 存档/删除/高危主体与 HTML 对齐；铁律 7 见 GR-01 |
| GEN-006 | 无本模块持久化写入 | PROGRESS 幂等为历史描述 | 无本文件可执行完整性路径 |
| GEN-007 | 同上 | 未跑并发 | 无本文件可执行幂等路径 |
| GEN-008 | README 栈版本 vs pom/package | Java 21 / Boot 4.1.0 / springdoc 3.0.3 / React 19 / Vite 8 / antd 5 / TS 5.7 / Vitest 4 / Playwright 1.61 | 一致 |
| GEN-009 | PII 空值 vs 未设置；backend-env JAVA_HOME | 002 已删空赋值；本机错误 JAVA_HOME 两次失败后改 Temurin 成功 | 002 未复发；环境失败不记缺陷 |
| GEN-010 | gitignore 秘密与构建物 | `git check-ignore`：`.env` 忽略、`.env.example` 不忽略 | 无漏忽略的受控凭据文件 |
| GEN-011 | README surefire 快照；PROGRESS 时态 | 无 `backend/target/surefire-reports`；表外说明已写快照 | 021/022 未复发 |
| GEN-012 | 无循环/连接实现 | — | 不适用 |
| GEN-013 | env/ignore/脚本/CI 入口 | 脚本键存在；migration-order 在 delivery-gates、不在 predeploy，与 AGENTS 一致 | 配置可复现；Wrapper 缺失已由 backend README 写明回退 |
| GEN-014 | AGENTS 铁律 9 | 未执行迁移 | 只核文档声明，不扩审迁移模块 |
| GEN-015 | AGENTS 铁律 7 vs PGM 界面全中文 | HTML 原文 | **GR-01 冲突** |
| GEN-016 | 路径/命令/链接/版本/完成声明 | §6 命令；链接 0 缺失 | 除 GR-01 外，声明可复现 |
| GEN-017 | 全文件四分类 | 冲突优先 | 见 §7；HTML 沉默不造缺陷 |
| GEN-018 | 本报告依据 | 类型 1/4；类型 2 仅 backend-env | 未把席位旧结论当依据 |

---

## 4. 四分类总则（本席独立）

顺序：冲突 → 细化 → 技术性 → 添加。能指 HTML 原文者不进技术性。HTML 无词不等于该删。业务发明才是添加。M003-023 已删 L4；M003-024 操作员保留，按细化继续审，未证新工程缺陷。

---

## 5. 每文件第一性原理 + 621 行覆盖

### 5.1 `.env.example`（1–22）

**必要性**：本地/测试启动需要可复制的环境变量样例。删掉后三份 HTML 无一条做不到；属构建运行配置，技术性。

**行为证据（类型 4）**：变量消费者在直系 `backend/src/main/resources/application.yml` / `application-local.yml`：`COMPLEX_DATABASE_*`、`COMPLEX_API_PORT/HOST`、`POOL_SIZE`/`MIN_IDLE`、`API_VERSION`/`BUILD_TIME` 有绑定；`P0_ADMIN_*` 仅 local 种子。`git check-ignore --no-index -- .env.example` exit 1（不被忽略）。

**未验证**：未启动 Spring、未连 `complex` 库、未跑 AuthService 种子。PII 生产 fail-closed 未在 prod profile 实跑。

| 行 | 内容 | 分类 | 依据 |
|---|---|---|---|
| 1 | 版本头 | 技术性 | 无业务发明 |
| 2–4 | local profile / API host-port | 技术性 | 运行配置 |
| 6–10 | JDBC URL 指向 `complex` 非 `complex_test` | 技术性 | 与 AGENTS「测试用 complex_test、勿 source 全文件」不冲突：样例是本地库 |
| 12–13 | API version/build-time | 技术性 | yml 有绑定 |
| 15–16 | PII AES-256-GCM / U 盘 provision / 未设置用 dev 默认 | 技术性 | 直系 `application.yml:104-109` 同口径；变量名在 yml 为 `COMPLEX_KEYVAULT_PII_KEY`。样例未写出变量名，本地路径仍是「不设置」。不因 HTML 沉默要求样例穷尽全部密钥。无新缺陷 |
| 18–22 | P0_ADMIN 本地种子、留空不创建、勿用于生产 | 细化 | 上位：PGM 平台管理员角色。`AuthService:46-54` 空白则不 `createUser`。`application-local.yml` 写明仅 local、绑 `role_platform_admin` |

对抗：显式空 PII 赋值已不存在（002 未复发）。对抗「平台管理员权限」是否发明新角色：HTML 已有平台管理员，种子是本地夹具。

### 5.2 `.gitignore`（1–55）

**必要性**：阻止构建物与本机秘密入库。技术性。

**证据（类型 4）**：`git check-ignore --no-index -v --stdin` 命中 `.env` `.env.local` `node_modules` `dist` `backend/target` `coverage` `.vite` Gradle/Kotlin/`build`/`*.class`/`*.apk` `local.properties` playwright 报告目录、`.claude/settings.json`、`*.tsbuildinfo`、`security-scans/semgrep-current/`、`docs/_archive/**/*.log`、`.loop-engineering/`、根调试 mjs、`backend/data/bar-images|evidence-files`。`.env.example` 与 `docs/audit-2026-09/findings.md` 未被忽略。

**未验证**：未证明历史上从未误提交匹配文件。

分类：全部模式为构建/机器/运行产物，无业务状态发明。对应 M003-016，无新缺陷。

### 5.3 `.semgrepignore`（1–6）

**必要性**：把扫描器无法解析的输入排除，避免假阳性当漏洞。技术性。

**证据（类型 4）**：`docs/openapi/openapi-p0.yaml|json`、`docs/_archive/`、`ops/keyvault/` 均存在。未跑 semgrep AST（不强造扫描门槛）。

**未验证**：未复现 yaml/bash 解析器退出码。对应 M003-017，无新缺陷。不因「能被扫描」另造必须扫描判据。

### 5.4 `AGENTS.md`（1–74）

**必要性**：多会话代理的仓库铁律。删掉后 HTML 仍在，但代理会违反 worktree/禁直推/三份 HTML 只读等工程约束。技术性为主；铁律 5–8 有 HTML 上位，属细化，其中铁律 7 例外清单与 HTML 冲突（GR-01）。

**证据**：`docs/agents/issue-tracker.md` `triage-labels.md` `domain.md` 存在；三份 HTML 路径存在；`ops/160/propose-change.sh` `merge-pr.sh` 存在；`package.json`：`verify:migration-version-order` 在 `verify:delivery-gates` 字符串内，不在 `verify:predeploy` 字符串内。

**未验证**：未调用 `gh` 核实 `jxh1979/mall`；未执行 `propose-change.sh`/`merge-pr.sh`；未跑迁移闸。

| 行 | 分类 | 说明 |
|---|---|---|
| 1–17 | 技术性 | 技能指针，目标文件存在 |
| 25–40 | 技术性 | worktree/禁直推/fetch；本审计提示词另规定 B3 在主工作区，不使本文件变假 |
| 42–50 | 细化 | 三份 HTML 最终解释权。上位 PGM footer 解释权声明 |
| 52–53 | 细化 | 配置主数据删除审批/软删除；账户注销指向 HTML 专项。003 未复发 |
| **55–57** | **冲突** | **GR-01** |
| 58–59 | 技术性 | 不为存量兼容：工程约束，HTML 无该句但不构成业务发明 |
| 63–67 | 技术性 | 迁移 V 号串行；浅克隆理由已删（004 未复发） |
| 69–74 | 技术性 | 测试只 export 三变量；部署 skipTests |

### 5.5 `CONTEXT.md`（1–163）

**必要性**：词表给代理/实现用。删掉后 HTML 仍是权威；本文件声明自身从属 HTML。细化（术语）+ 技术性（结构）。

**证据**：三份 HTML 逐字对照（类型 1）。L4 已不存在（023 删除已落地）。

**未验证**：未跑会员/价格/权限后端；术语不要求 E2E。

| 行 | 分类 | HTML 上位（文件:行） | 缺陷 |
|---|---|---|---|
| 1–12 | 细化 | PGM footer 解释权；不再把会员 Markdown 置于 HTML 之上（005 未复发） | 无 |
| 14–19 | 细化 | PGM:417 顶层平台；集团视图 PGM:343/413/445 是预留视图，括号限制「仅多项目汇总视图」作别名，不另造主体 | 无 |
| 21–26 | 细化 | PGM:418/420 物理项目隔离、城市项目公司职责；已去掉固定隶属与越权售票（006 未复发） | 无 |
| 28–38 | 细化 | PGM:428–433 娱乐平台公司/独立酒吧商户/中央餐饮/城市项目兼任资产；服务费代收代付 PGM:429（007 未复发）；无 L4 | 无。2026-07-28 日期三份 HTML 无，属拍板时间戳，不因沉默造缺陷 |
| 40–45 | 细化 | 商户数据范围，PGM 商户侧/隔离 | 无 |
| 47–51 | 细化 | BEQR:65–66 短信验证手机号、自动注册。HTML 无「免费会员」「付费订阅」逐字；注册条件无付款步骤，不另开添加。019 已登记 | 无 |
| 53–57 | 细化 | BEQR:70 成长值与积分两套账 | 无 |
| 59–63 | 细化 | BEQR:82 积分余额不可转赠；BEQR:78 只兑换虚拟券 → 「不可收银抵现」；跨城复用 + 每笔留项目/成本主体为下位 | 无 |
| 65–69 | 细化 | BEQR:75 待追回积分 | 无 |
| 71–75 | 细化 | BEQR:80 可配窗口、默认三十天、无成交前延（009 未复发） | 无 |
| 77–81 | 细化 | BEQR:54 每项目目录、核销/结算用价；已去掉仅兑换 SKU（020 未复发）。「每个项目独立管理、审批」读作目录隔离，未写成项目公司审批，不与「域主管公司审批」字面相撞 | 无 |
| 83–87 | 细化 | BEQR:75/85 功能主体；非独立公司时顶层平台收款（010 未复发） | 无 |
| 89–93 | 细化 | BSM:1725 营销费用。操作员保留。024 未复发。「项目发起平台」是承担方法人的概括，不另造主体 | 无 |
| 95–100 | 细化 | PGM:497 四类主体绑定 | 无 |
| 102–107 | 细化 | PGM:644/669/655 功能权限点、显式拒绝 | 无 |
| 109–114 | 细化 | PGM:656/659 临时授权 | 无 |
| 116–121 | 细化 | PGM:660 五类高危；652 上一级/上两级/最高。CONTEXT 未写「发起人自选档位」，与 660「不由发起人选择档位」不构成明文相反 | 无 |
| 123–128 | 细化 | PGM:675 外包 | 无 |
| 130–135 | 细化 | PGM:676–677 审计人员 | 无 |
| 137–142 | 细化 | PGM:731–738 开关层级、兜底关闭、留痕例外（011 未复发） | 无 |
| 144–149 | 细化 | PGM:739 十一类终端原文（011 未复发） | 无 |
| 151–156 | 细化 | PGM 留痕/责任链 | 无 |
| 158–163 | 细化 | BSM:566/569/570 逐条存档不需批、批量/整店须批、删除最高权限（012 未复发） | 无 |

对抗：内部结算「审批」是否越权给项目公司——原文无「城市项目公司审批内部结算价」全称，不升冲突。会员「免费」——HTML 沉默，019 已按自动注册细化登记，不新开添加。

### 5.6 `PROGRESS.md`（1–36）

**必要性**：一条已合入能力的历史建设台账。删掉不影响 HTML。技术性。

**证据**：所列 Java/TS 路径均存在；`ContractService` 含 `settleTerminationIfNeeded`。标题与 L35–36 标明历史时态。当前 `TerminationSettlementMathTests.java` `@Test` 计数 8，与文内「纯单测 7」并存，但文已声明数字不代表当前工作区。

**未验证**：未跑 32/15 绿、未跑 verify:predeploy。对应 022 未复发。

### 5.7 `README.md`（1–149）

**必要性**：仓库第一入口。技术性；§1 业务域列举是 HTML 模块入口的下位概述。

**证据（类型 4）**：见 §8。链接抽查 0 缺失。`apps/api` 不存在。基线 `--check` PASS：720 ops / V385 / 363 迁移。surefire 目录不存在，与表外快照说明一致。`npx playwright test --list`：47 tests / 9 files；README 不写死该数。

**未验证**：未跑 `verify:unit`、`test:backend`、`verify:ui`、`verify:delivery-gates`、`verify:p0-contract`。

| 行 | 分类 | 缺陷 |
|---|---|---|
| 1–11 | 细化/技术性 | 单综合体起步+集团预留，上位 PGM:413 |
| 13–27 | 技术性 | 栈版本与 pom/package 一致 |
| 29–51 | 技术性 | 目录树路径存在；无 `database/migrations`、无写死 79 脚本（013 未复发） |
| 53–94 | 技术性 | 所列 npm 键均在 package.json |
| 96–110 | 技术性 | 自动基线块 + 快照脚注（021 未复发） |
| 112–127 | 技术性 | 读者路径文件存在 |
| 129–143 | 技术性 | mermaid 为 C4 系统上下文，不是十一类终端清单；终端清单在 CONTEXT。`Guest --> WEB (admin/customer)` 不升格为「客人网页终端」发明。015 已把 GuardedAction 收窄为受保护业务写动作典型路径 |
| 145–149 | 细化 | 第三方未到位+开关默认 off，上位治理/外部依赖文档存在 |

### 5.8 `README.en.md`（1–116）

**必要性**：国际开发者入口。非产品终端，不全文中文化。技术性。

**证据**：§5 转引中文自动基线，无 V124/386 当前数字（014 未复发）。脚本键与中文同一组。GuardedAction 同样收窄（015 未复发）。

**未验证**：同 README，未跑全量门禁。

覆盖行合计 22+55+6+74+163+36+116+149=**621**。无未读行。

---

## 6. 旧 24 ID 重验

| ID | 原缺陷是否仍在当前 8 文件 | 队列 | 复发 |
|---|---|---|---|
| M003-001 | `.env.example` 无 ACCESS_TOKEN/JWT/CORS 等死变量 | 否 | 未复发 |
| M003-002 | 无显式空 PII 赋值；注释为未设置用默认 | 否 | 未复发 |
| M003-003 | AGENTS 已收窄配置主数据+账户注销指向 HTML；无「一律/唯一例外人脸/role_platform_admin」 | 否 | 未复发 |
| M003-004 | 无浅克隆理由 | 否 | 未复发 |
| M003-005 | CONTEXT 以三份 HTML 为解释权 | 否 | 未复发 |
| M003-006 | 多对多、非固定隶属 | 否 | 未复发 |
| M003-007 | 酒吧域+代收代付 | 否 | 未复发 |
| M003-008 | 短信验证手机号，无自然人唯一 | 否 | 未复发 |
| M003-009 | 可配窗口+前延 | 否 | 未复发 |
| M003-010 | 功能主体+非独立公司情形 | 否 | 未复发 |
| M003-011 | 十一类+留痕例外 | 否 | 未复发 |
| M003-012 | 逐条/批量存档分写 | 否 | 未复发 |
| M003-013 | 目录树无死路径、无 79 脚本 | 否 | 未复发 |
| M003-014 | EN 不再把 V124/386 当当前基线 | 否 | 未复发 |
| M003-015 | 「典型路径」+认证入口例外 | 否 | 未复发 |
| M003-016 | 只登记；ignore 实跑仍成立 | 否 | 未复发 |
| M003-017 | 只登记；排除路径存在 | 否 | 未复发 |
| M003-018 | 只登记；命令/版本/链接本次复核仍成立 | 否 | 未复发 |
| M003-019 | 只登记；剩余术语仍有 HTML 上位 | 否 | 未复发 |
| M003-020 | 内部结算价扩到商品或服务 | 否 | 未复发 |
| M003-021 | 快照脚注存在；surefire 目录确无 | 否 | 未复发 |
| M003-022 | 历史时态标明 | 否 | 未复发 |
| M003-023 | CONTEXT 无 `L4`；question 首行删除已落地 | 否 | 未复发 |
| M003-024 | 词条仍在；BSM:1725 支持；question 首行保留 | 否 | 未复发 |

---

## 7. 本次完整发现清单

### GR-01（新）

- 文件:行：`AGENTS.md:55-57`
- 冻结 ID：GEN-015, GEN-016, GEN-017
- 分类：**冲突**
- 依据类型：1（HTML 逐字）+ 4（当前文件原文）
- 现有正式 ID：无
- 当前队列：无 ID
- 工程缺陷：是（须删除/收窄与 HTML 相反的例外清单）

**事实**：

AGENTS.md：

> 所有展示文字全中文，仅机读标识（枚举值、字段名、日志键）保留原文。后台、客人端、设备端、安卓端一律适用。

PGM `docs/platform-governance-map-for-operations-v0.1.html:287`：

> 平台所有终端——后台管理端、客人手机端、现场平板与设备端、安卓终端——展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错、导出文件表头与内容、短信与通知、日期时间等，必须全部为中文，不得出现任何英文……可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。

同文件 :965 已拍板复述封闭四样。

**判定**：铁律 7 的「全中文」有上位，属细化；但其例外把「枚举值、字段名」列为可保留原文的机读标识，与 HTML「字段名、枚举取值必须中文」且「没有第五样 / 不得以这是标识为由」明文相反。冲突优先于细化。日志键可落入「系统内部标识符」，不能把字段名/枚举取值一并放开。

对抗式最小反例：代理按 AGENTS 把 UI 枚举/字段名写成英文，将直接违反 PGM 强制规则。

顺从式：假定作者只想豁免 Java 枚举常量——则原文未把「展示给人看的」与「系统内部标识符」分开，仍与封闭四样不一致。

### 无其他新冲突 / 无新添加项

添加项 023/024 已答。未因 HTML 沉默新开添加。

---

## 8. 技术性逐条理由（无业务发明）

| 项 | 位置 | 无业务发明理由 | 工程缺陷 |
|---|---|---|---|
| 忽略 node_modules/dist/target/coverage/vite/gradle/class/apk | `.gitignore:1-21` | 构建产物 | 否 |
| 忽略 .env/.env.local、不忽略 .env.example | `.gitignore:4-5` | 本机秘密 vs 样例 | 否 |
| playwright/test-results/tsbuildinfo/claude settings | `.gitignore:23-32` | 机器/会话/报告 | 否 |
| semgrep-current 与 archive log | `.gitignore:34-39` | 扫描滚动输出 | 否 |
| 根调试 mjs 与 bar-images/evidence-files | `.gitignore:44-55` | 运行态/探针 | 否 |
| 排除 openapi yaml/json、_archive、ops/keyvault | `.semgrepignore:1-6` | 扫描器输入 | 否 |
| Spring/DB/API 运行变量 | `.env.example:2-13` | 连接与进程配置 | 否 |
| PII 注释（未设置用默认） | `.env.example:15-16` | 密钥安装；变量名在 application.yml | 否 |
| P0_ADMIN 本地种子 | `.env.example:18-22` | local profile 夹具 | 否 |
| 技能指针、worktree、禁直推、fetch | `AGENTS.md:1-40` | 代理工程规程 | 否 |
| 存量不兼容、迁移 V 串行、三变量测试、skipTests | `AGENTS.md:58-74` | 工程操作 | 否 |
| README/EN 栈、目录、脚本、基线、架构、边界 | 对应段落 | 入口与门禁 | 否 |
| PROGRESS 历史建设记录 | 全文 | 工程台账 | 否 |

---

## 9. 实际命令输出（节录）

```
# lock
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# 有效行
git ls-files -z -- .env.example .gitignore .semgrepignore AGENTS.md CONTEXT.md PROGRESS.md README.en.md README.md | xargs -0 cat | wc -l
621

# 基线
node tools/refresh-entry-doc-baseline.mjs --check
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
EXIT:0

# Flyway / OpenAPI（python 实算）
count 363
latest V385__permission_names_chinese.sql
ops 720

# surefire
ls: cannot access 'backend/target/surefire-reports': No such file or directory

# .env.example 不被 ignore
git check-ignore -v --no-index -- .env.example
exit:1

# 脚本键：dev:web typecheck build verify:unit verify:backend-env test:backend dev:backend
# verify:openapi verify:p0-contract verify:tenant-isolation verify:delivery-gates
# verify:production-blockers verify:ui verify:ui:real-backend:seeded verify:closed-loop-runtime
# verify:migration-version-order verify:predeploy
全部 OK
engines {'node': '>=22'}
predeploy_has_migration False
delivery_has_migration True

# 版本
java.version 21
boot.parent 4.1.0
springdoc.version 3.0.3
react ^19.0.0 vite ^8.0.16 antd ^5.22.5 typescript ^5.7.2 vitest ^4.1.9
playwright ^1.61.0
node v24.20.0

# verify:backend-env
# 1) 默认 JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64（该路径无 bin/java）EXIT:1
# 2) 同 JAVA_HOME 原样重跑 EXIT:1
# 3) JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
[backend-env] 后端环境前置校验通过。
- system Maven: Apache Maven 3.9.11
EXIT:0
Java: OpenJDK 21.0.12.1 Temurin

# playwright --list（未执行测试）
Total: 47 tests in 9 files
EXIT:0

# 链接/路径抽查 MISSING_COUNT=0；apps/api ABSENT
# MathTests @Test = 8
# questions 首行：M003-023 删除；M003-024 保留
```

未跑：`mvn test`、`npm run verify:unit`、`verify:ui`、`verify:delivery-gates`、semgrep、真实库写入。

---

## 10. ponytail-review（只列可删，不改）

`AGENTS.md:55-57: shrink: 「枚举值、字段名、日志键」换成 PGM 封闭四样。`
`Lean already` 于其余 7 文件相对本模块已修冲突。
`net: -约一行例外清单。`

---

## 11. grilling 记录（自问自答，不等操作员）

1. 24 ID 队列全否能否代替 B5？不能。已整模块重读 621 行。
2. B4 diff PASS 能否代替？不能。
3. README mermaid Guest→Web 是否冲突「客人无网页端」？CONTEXT 持有十一类终端原文；README mermaid 是 C4 上下文且目录树写「管理端」。不升冲突，不因测试文件名扩审 web 模块。
4. 「项目发起平台」是否添加？024 已保留；BSM:1725 有承担方法人闭集，概括不另造主体。
5. PII 未写变量名是否缺陷？yml 直系可解析；本地行为是不设置。不新造「样例必须列出全部密钥」判据。
6. 铁律 7 例外是否可当系统内部标识符细化？HTML 禁止「以这是标识为由」且字段名/枚举取值在展示清单内 → 冲突，不能细化消化。

---

## 12. 本次 B5 结论

**FAIL**

理由：存在无正式 ID 的新冲突 GR-01（`AGENTS.md:55-57` 与 PGM:287/965 展示中文封闭四样相反）。按回退规则，任一条无 ID 或不在修复队列 → 整批回 B2，不得直接 B3。

旧 24 ID 未复发。023 删除与 024 保留均已体现在当前 CONTEXT。无待批 questions。未改任何源码/HTML/lock/ledger/findings。
