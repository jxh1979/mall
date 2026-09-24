CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M007 B1 修前全量审计（Grok 乙席 raw）

- 文件自身版本：V0.01（本路径此前不存在，新建；未追加、未覆盖前缀）
- 席位：Grok（审计席乙，独立）。步骤：B1。模块：M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）
- 正本：`/home/james/complex-management` 分支 `main`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`
- ledger 本模块行（只读顶部两行 + 本行，未用历史 raw/收口代替实审）：`M007 … | IN_PROGRESS | 0 | 2 | - | 0 | 当前471计划B1开工；5文件156行；B3为0；只用SSH及Codex/Grok两席`
- 锁旁路 sidecar 与 ledger 顶行与本席独算值相同（正文不重复独占锁字段名；命令标签见 §8）
- 只审不修。无 MCP、无子代理、无第三席。未改业务文件、HTML、判据、台账。未实际 `git push`/`merge`/`commit`（业务仓）、未 `gh pr create/merge` 或 PUT、未覆盖真实 hook、未部署、未生成源码
- 不重审 M001–M006、不抢审后续模块；调用者/消费者只读到足够建立本范围证据
- 最终步骤结论：**FAIL**

## 0. 范围与文件指纹

`plan/modules.md` FILES（5 文件，`git ls-files -z -- <paths> | xargs -0 cat | wc -l` = **156**，与模块声明一致）：

| 路径 | 物理行 | 内容 SHA256 | git hash-object（工作区） | 相对 HEAD |
|---|---|---|---|---|
| `ops/160/install-git-hooks.sh` | 15 | `badf746b715a52239361a80671cf863a8aeadaf66b52bc579741a88138ea25d3` | `2362788d8303e2404dfb159440f5d8801fb2b0ba` | 与 HEAD 相同 |
| `ops/160/merge-pr.sh` | 73 | `8688f7dc7e9b688c2d8583ff1942137089df55dbb9e5cfda95f69f344edbd514` | `e2952ca8d4848af74ac3c610eef98844421d062a` | 与 HEAD 相同 |
| `ops/160/propose-change.sh` | 24 | `913a12cae30d4448ac546d56ed54c9b25e7687c966a3181bd9324ebd194865e1` | `eb7198de8ae620705c0ad4b40e6f672728c547bb` | 与 HEAD 相同 |
| `ops/160/hooks/pre-push` | 17 | `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b` | `45966c01610c92bb624b921b8333314f08238fed` | 与 HEAD 相同 |
| `ops/ci/predeploy-gates.yml` | 27 | `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d` | `3857c0f9b3ca98e4d2e8d94771e91be606e0e732` | 工作区相对 HEAD 注释 2 行（L1 V0.01→V0.02，L6 职责说明）；**本席审工作区当前字节** |

工作区 `ops/ci/predeploy-gates.yml` 与 `.github/workflows/predeploy-gates.yml`（M005，只作消费者对照）`cmp` exit 0，字节全等。无 BOM/CRLF/NUL。

映射：`criteria-lock.md` 行 `M007 → […]` **79 个唯一 ID**（18 GEN + 37 BEQR + 14 BSM + 10 PGM），定义区 79/79 均存在。

## 1. §6 技能自核（本席 Linux 环境实读）

按 V0.17 收窄：只借结构，不派子代理、不问操作员、不写 §7 以外产物、B1 不修复。

| 技能 | 本环境 | 本节点用法 |
|---|---|---|
| `using-superpowers` | 已读 `…/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能优先；用户/提示词覆盖技能 |
| `verification-before-completion` | 已读 | 结论前必须有本轮命令输出 |
| `systematic-debugging` | 已读 | 只借读错误→复现→追数据流；不改生产代码、不加诊断插桩 |
| `diagnosing-bugs` | 已读 | 隔离 harness 作红/绿信号；B1 不进入修复相 |
| `grilling` / `grill-me` | 已读；`grill-me` 仅转发 grilling | 内用追问树；不等用户、不派探事实子代理 |
| `codebase-design` | 已读 | 模块/接口/缝/删除测试词汇用于第一性原理 |
| `ponytail-review` | 已读 | 只找可删复杂度；不把正确性当 ponytail 项 |
| `ponytail` | 已读 | B3 才用；B1 不修 |
| `tdd` / `test-driven-development` | 存在 | B1 禁止修复，未落测试文件 |
| `writing-for-agents` | 已读 | 报告结构；未改 AGENTS.md |
| `i-have-adhd` | 已读 `…/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号与直述；**报告首行被提示词锁行覆盖**（技能自身：harness 约束优先） |
| `git-guardrails-claude-code` | 存在（mattpocock misc） | 本体要问安装范围并写 hook 配置 → 第四闸 + 写仓；**未用** |
| `ubiquitous-language` | 存在（多份副本，非 mattpocock 主树必核项） | 会另写 `UBIQUITOUS_LANGUAGE.md` 超 §7；**未用** |
| `batch-grill-me` | **缺口：本环境未找到 SKILL.md** | 记缺口，未冒充调用 |
| `brainstorming` / `writing-plans` | 存在 | 阶段 A 技能，B1 未用 |
| `requesting-code-review` / `receiving-code-review` | 存在 | B4/收意见；B1 未用 |
| `resolving-merge-conflicts` | 存在 | 本节点无合并冲突 |
| 划掉项（executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、research、ponytail-audit 等） | 存在或不论 | **按 V0.17 不用** |

未用 MCP_Codex_160。未派子代理。

## 2. 第一性原理（逐文件三问）

依据类型只许：① HTML 逐字 ② 可执行产物 ③ 官方规范原文 ④ 可复现命令输出。

### 2.1 `ops/160/install-git-hooks.sh`（15 行）

1. **为何必须存在 / 删除代价**  
   把受控 `ops/160/hooks/pre-push` 装到 Git 实际生效的 hooks 目录。删除后新克隆/设了 `core.hooksPath` 的仓库会静默没有拦截（脚本头 L2–3 自述）。三份 HTML **没有**「必须装 git hook」句。最接近的交付章是 BSM-116《系统交付与上线》（`business-system-map-for-operations-v0.1.html` L1928–1952）——原文是生产换弱口令、U 盘密钥、安装包，**不是** git hook。答不上 HTML 哪一条会做不到 ≠ 该删。按 2.1：**无冲突 → 指不出必须实现的 HTML 上位 → 技术性（CI/工程脚本，无业务发明）**。
2. **声称是否执行**  
   隔离仓实跑原始脚本（§8）：默认装到 `$GIT_COMMON_DIR/hooks/pre-push`，mode `0755`，字节等于源；`core.hooksPath` 绝对路径与相对 `.githooks` 均装到配置值；从 worktree 执行装到 **common** `.git/hooks`（worktree 共用，与注释一致）。未在业务仓跑安装器。
3. **未验证前提**  
   未在 macOS/`install` 非 GNU 环境跑；未验证所有 Git 版本对相对 `hooksPath` 的解析（本机 `git 2.43.0` + `man git-config`：相对路径相对 hook **运行目录**；非裸仓 pre-push 运行于工作树根，与脚本 `cd` 到仓库根后 `mkdir/install` 一致）。

### 2.2 `ops/160/hooks/pre-push`（17 行）

1. **为何必须存在**  
   补私有库免费版无分支保护时「直推 main」缺口（L1–3）。删除后只靠纪律。HTML 无此句。BSM-114 十一端表（L1827–1893）不含 git CLI。技术性。文件头写明「开发者提示,非安全边界」——`git push --no-verify` 可绕，本席不把「可绕」升成未声明的安全边界。
2. **声称是否执行**  
   用官方 stdin 四元组喂原始脚本（未 `git push`）：推 `refs/heads/main` 且 `ALLOW_MAIN_PUSH≠1` → exit 1 中文拒绝；feature、`refs/heads/mainish`、`refs/heads/main/topic`、tag `refs/tags/main` → 0；`ALLOW_MAIN_PUSH=1` → 0 且「覆盖」；`ALLOW_MAIN_PUSH=true` 不放行；删除 main（`(delete)` + zeros）拦截；多 ref 中含 main 拦截；本地 ref 含空格仍能命中远端 main。`man githooks` 原文格式与实现匹配。
3. **未验证前提**  
   未对真实 `origin` 做 push。整行子串 `refs/heads/main` 会把「本地 main 推到非 main 远端」一并拒绝（对抗式 §4），属偏严、非常用路径。

### 2.3 `ops/160/propose-change.sh`（24 行）

1. **为何必须存在**  
   声明的正道：本地 `verify:predeploy` → `git push` → 开/更新 PR。删除后易手敲直推或手开 PR 绕过本地闸。HTML 无 PR 流程句。技术性。
2. **声称是否执行**  
   假 `npm`/`git`/`gh` 跑原始脚本：无标题非 0；在 main 拒绝；`npm` 非 0 不 push；成功路径 `npm run verify:predeploy` 后 `git push -u origin <branch>`，再 `gh pr create --base main --head <branch> --title … --body …`；已有 PR 则不 create。未对 GitHub 做 create/push。
3. **未验证前提 / 已证裂缝**  
   脚本**从不**读 `git status --porcelain`，本地门禁跑的是工作区文件，push 的是已提交对象。与自身 L11「本地跑 predeploy 门禁(红则不推)」及 merge-pr L17–21 对「CI/远端是 commit 不是工作区」的同一风险类不一致。见 **GR-01**。未跑真实 `verify:predeploy` 全链（提示词允许不跑全仓父链）。

### 2.4 `ops/160/merge-pr.sh`（73 行）

1. **为何必须存在**  
   私有库免费版设不了 required check（workflow GET `state=active` 仍只是可见信号）。删除后 `gh pr merge` 可在红灯合并。技术性。删除代价：失去「等 predeploy-gates + `--match-head-commit`」机器闸。
2. **声称是否执行**  
   假 git/gh + 空操作 `sleep` 跑原始脚本：非法 method、在 main、脏工作区、无远端分支、HEAD≠origin、无 PR、无 run（30 次空列表）、`gh run list` 失败被当成无 run、watch 失败、多 run 中一个失败、等待期间 SHA 漂移、SKIP_CI_WAIT 仍核对 SHA 再 merge——行为与源码一致。绿灯路径实际 argv：`pr merge <branch> --squash|--rebase --match-head-commit <40位SHA>`。与 `gh pr merge --help` 的 `--match-head-commit SHA`、`--squash`/`--rebase` 原文一致。只读 `gh run list --commit <feature-headSha> --workflow predeploy-gates.yml` 能命中 `event=pull_request` 的那条（例：`dc7b735d…` → run `34198944107`）。
3. **未验证前提 / 已证裂缝**  
   `run_ids="$(gh run list … 2>/dev/null || true)"` 把 gh 失败吞成「没有 run」。见 **GR-02**。未调用真实 `gh pr merge`。未验证 `gh run watch` 在 GitHub 侧对同一 `databaseId` 重跑 attempt 的语义（官方 help 只保证 watch 到完成且 `--exit-status` 失败非 0）。

### 2.5 `ops/ci/predeploy-gates.yml`（27 行，V0.02）

1. **为何必须存在**  
   与生效定义 `.github/workflows/predeploy-gates.yml` 的字节镜像；`tools/verify-delivery-gates.mjs`（M008，只读断言段）要求二者 LF 归一后全等，且 `fetch-depth: 0`、`run: npm run verify:predeploy`、`HTML_GATES_BASE` 表达式逐字相等。删除镜像会让该机检失败。技术性（CI 配置，提示词 2.1 明文「不算业务发明」）。
2. **声称是否执行**  
   `yaml` 包 `parse`：`on.pull_request.branches=[main]`、`on.push.branches=[main]`、job `predeploy`、`ubuntu-latest`、checkout SHA 钉死 + `fetch-depth: 0`、setup-node `22`、`npm ci --ignore-scripts`、`npm run verify:predeploy` + `HTML_GATES_BASE` 表达式。只读 `gh api …/actions/workflows/predeploy-gates.yml`：`name=predeploy-gates`，`path=.github/workflows/predeploy-gates.yml`，`state=active`。L6「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main」：部署脚本 `REQUIRED_GATES` 15 项与 `package.json` `verify:predeploy` 链 **逐项相等**（只读 api/web 两文件数组，不审部署模块）；pre-push 实跑不含 `npm`/`verify:predeploy`。
3. **未验证前提**  
   checkout/setup-node 完整 SHA 是否等于注释中的 v4.3.1 / v4.4.0：本席禁外发核对 action 发行页，**未验证**。`node-version: "22"` 与 engines `^22.13.0 \|\| >=24.0.0`：setup-node 装最新 22.x 的官方行为未在本机跑 GitHub runner 证实。`github.event.before` 在新建分支/force-push 的 40 个零：未构造。workflow `permissions:` 缺省值未查 GitHub 当前默认文档。

**删除测试（codebase-design）**：删这 5 个文件，复杂度不会消失，而会散到「人记得看 CI / 记得不推 main / 记得两份 yml 一致」。它们赚到了存在价值。不是无需求纯转发。

## 3. 顺从式：79 判据全覆盖

判定顺序每条都走完：冲突 → 细化 → 技术性 → 添加。GEN-017：读了 HTML 原句上下文，**不因字面词未出现就判添加**，也不把「本模块未实现游乐/地产业务」写成遗漏——那些锚点的实现模块不在本 FILES。

### 3.1 GEN-001 … GEN-018

| ID | 适用与证据 | 分类 | 缺陷？ |
|---|---|---|---|
| GEN-001 | 五文件均有可验证用途（装钩、拦 main、提议、等 CI 再合、CI 镜像）。镜像重复是机检约束，不是死代码。逃生口 `SKIP_CI_WAIT`/`ALLOW_MAIN_PUSH` 有注释与警告，不是为假设场景加的抽象层 | 技术性 | 否（存在性） |
| GEN-002 | 入口/主路径/错误路径已用原始脚本+假适配器执行。绿灯 merge argv 与 `gh` 官方 help 一致。PR 的 `headSha` 可被 `--commit` 查到。**GR-01** 使 propose 的「本地门禁代表将推送的树」声称不成立 | 技术性 | **是 GR-01** |
| GEN-003 | 本模块 FILES 无测试。仓内 `tools/gate-hardening-tests/m001-regression.mjs`（M008）含 merge-pr 三态；`verify-delivery-gates.mjs` 含镜像/fetch-depth/表达式。propose/install/pre-push 无仓内可失败断言。本席隔离实跑补证。不把「测试文件属于 M008」升成本模块修复项 | 技术性 | 否（归属外模块，§仅登记 R1） |
| GEN-004 | 标题/正文以独立 argv 传给 `gh`（`--title "$title"`），不经 shell 二次展开。覆盖开关只认字面 `1`。分支名来自 `git rev-parse`。无把未校验网络输入当权威 | 技术性 | 否 |
| GEN-005 | 无业务对象授权。合并/推送依赖操作者已有的 `gh`/`git` 凭证。默认拒绝直推 main、默认等 CI。不是界面隐藏式授权 | 技术性 | 否 |
| GEN-006 | 无业务库写入。合并钉 SHA 防止等 CI 期间 tip 漂移（已实跑漂移中止） | 技术性 | 否 |
| GEN-007 | 等待后再核 `ls-remote` 与 `headRefOid`。重复跑 merge 若已合，由 `gh` 失败 + `set -e` 中止。无并发写同一业务实体 | 技术性 | 否 |
| GEN-008 | workflow `name`/文件名与 `gh run list --workflow predeploy-gates.yml` 都能列出 run。`verify:predeploy` 字符串与 CI `run:` 一致。两份 yml 字节一致。无 DTO/枚举 | 技术性 | 否 |
| GEN-009 | 多数失败路径 exit 1 且中文原因。**GR-02**：`gh run list` 失败被 `2>/dev/null \|\| true` 变成「没有 run」 | 技术性 | **是 GR-02** |
| GEN-010 | 五文件无明文秘密。`npm ci --ignore-scripts` 与 `npm help ci` ignore-scripts 原文一致（仍允许随后 `npm run`）。hook 非安全边界已声明。未发现注入/路径穿越 | 技术性 | 否 |
| GEN-011 | SKIP_CI_WAIT/ALLOW_MAIN_PUSH 只打 stderr「请在 PR/说明记录原因」，脚本不强制写 PR 正文。属工程提示而非业务留痕（PGM-066 留痕原文是业务动作） | 技术性 | 否（§仅登记 R2） |
| GEN-012 | CI 等待 `seq 1 30` × `sleep 10` 有界。无无界文件/连接。本席测试用空操作 sleep，未等满 5 分钟真睡眠 | 技术性 | 否 |
| GEN-013 | 依赖 bash、git、gh、npm、`install`、`seq`（160 Linux 均在 PATH）。CI 钉 action SHA、`node-version: "22"`。workflow `state=active`。action 标签与 SHA 对应关系未验证 | 技术性 | 否 |
| GEN-014 | 无迁移文件、无 SQL | 不适用 | 否 |
| GEN-015 | 无产品 UI/键盘焦点。PGM-066 原文「后台/客人端/现场设备/安卓端」。YAML 键名受 GitHub Actions 工作流语法约束（类型 3，必须英文键）。CLI 用户可见文案以中文为主；install L15 一句英文见仅登记 R3 | 技术性 | 否（不升修复） |
| GEN-016 | 脚本头/yml 注释与实跑、只读 Actions API、部署数组对照可复现。L6 现文句与 hook/部署行为一致（审的是工作区 V0.02，不是 HEAD V0.01 旧句） | 技术性 | 否 |
| GEN-017 | 见本节与 §3.2：先冲突后细化；CI/hook/PR 脚本无业务发明；不因 HTML 无「git hook」字样判添加或删除 | — | 本条为分类过程，见各条 |
| GEN-018 | 本报告结论落到 HTML 原文、可执行产物、`man`/`gh --help`/`npm help`、可复现命令。不用历史 raw 当依据 | — | 否 |

技术性「无业务发明」总理由（适用于本模块五文件整体）：提示词 2.1 明文「构建配置、依赖声明、日志与埋点、**CI 脚本**、格式化配置」不算业务发明；五文件不引入票种/状态/角色/价格/审批链/商户等 HTML 实体。

### 3.2 HTML 派生 61 条（均已读锁内摘录 + HTML 原句上下文）

**读法（写死）**：锚点正文是游乐/地产/看板/主体职责等经营规则。本 FILES 是 git/CI 工程入口。顺从式检查「有没有在这些脚本里另写或写反那些规则」。没有实现 ≠ 遗漏（遗漏要由承载该业务的模块承担）。没有字面词 ≠ 添加。

| ID | HTML 锚点（来源行） | 本模块对照 | 分类 | 缺陷 |
|---|---|---|---|---|
| BEQR-085 | 项目开放状态 L140 | 无枚举、无状态机 | 无冲突；无发明；技术性（脚本不承载该业务） | 否 |
| BEQR-086 | 场次状态 L142 | 同上 | 同上 | 否 |
| BEQR-087 | 游乐当天/每天 L144 | 无营业日计算 | 同上 | 否 |
| BEQR-088 | 容量双阈值 L145 | 无容量 | 同上 | 否 |
| BEQR-089 | 园区容量 L146 | 无 | 同上 | 否 |
| BEQR-090 | 设备关键/辅助 L147 | 无游乐设备台账 | 同上 | 否 |
| BEQR-091 | 全园故障与补偿通票 L148 | 无票务补偿 | 同上 | 否 |
| BEQR-092 | 安全严重度 L149 | 无 | 同上 | 否 |
| BEQR-093 | 巡检任务状态 L150 | 无游乐巡检 | 同上 | 否 |
| BEQR-094 | 越界类型 L151 | 无空间/合同越界 | 同上 | 否 |
| BEQR-095 | 团体/包场 L153 | 无 | 同上 | 否 |
| BEQR-096 | 游乐增强阶段 L154 | 无 | 同上 | 否 |
| BEQR-118 | 运营营业日 L187 | 无账期/10:00 切日 | 同上 | 否 |
| BEQR-146 | 运营公司职责 L229 | 无娱乐平台/游乐/传媒公司分工 | 同上 | 否 |
| BEQR-155 | 商户/运营主体状态 L238 | 无待准入/黑名单/缓冲期 | 同上 | 否 |
| BEQR-160 | 招商线索状态 L248 | 无 | 同上 | 否 |
| BEQR-161 | 招商意向阶段 L249 | 无 | 同上 | 否 |
| BEQR-162 | 品牌/客户库类型 L250 | 无 | 同上 | 否 |
| BEQR-163 | 合同变更类型 L251 | 无 | 同上 | 否 |
| BEQR-164 | 退租结算状态 L252 | 无 | 同上 | 否 |
| BEQR-165 | 内部应付执行状态 L253 | 无 | 同上 | 否 |
| BEQR-166 | 合同审批链 L255 | 无合同审批；SKIP_CI_WAIT 不是合同审批豁免 | 同上 | 否 |
| BEQR-167 | 租金/账单类型 L256 | 无 | 同上 | 否 |
| BEQR-168 | 营业额抽成模式 L257 | 无 | 同上 | 否 |
| BEQR-169 | 公区能源费 L258 | 无 | 同上 | 否 |
| BEQR-170 | 铺位租赁状态 L259 | 无 | 同上 | 否 |
| BEQR-171 | 物业工单类型 L260 | 无 | 同上 | 否 |
| BEQR-172 | 物业工单状态 L262 | 无 | 同上 | 否 |
| BEQR-173 | 商户服务工单状态 L264 | 无 | 同上 | 否 |
| BEQR-174 | 工单时效 L265 | 无 | 同上 | 否 |
| BEQR-175 | 楼宇机电设备状态 L266 | 无 | 同上 | 否 |
| BEQR-176 | 设备维保类型 L267 | 无 | 同上 | 否 |
| BEQR-177 | 能耗表计类型 L268 | 无 | 同上 | 否 |
| BEQR-178 | 抄表状态 L269 | 无 | 同上 | 否 |
| BEQR-179 | 能耗归集对象 L270 | 无 | 同上 | 否 |
| BEQR-180 | 楼宇巡检任务状态 L271 | 无 | 同上 | 否 |
| BEQR-181 | 楼宇巡检类型 L273 | 无；未把能耗抄表写进巡检 | 同上 | 否 |
| BSM-020 | 楼宇运营入口卡 L656 | 无楼宇子系统 | 同上 | 否 |
| BSM-028 | 基础管理·主要功能 L698（含七类红线） | 脚本不提供关闭①–⑦校验的开关，也不发明放行 | 无冲突；技术性 | 否 |
| BSM-081 | 楼宇运营 h2 L1401 | 无机电/能耗/巡检实现 | 同上 | 否 |
| BSM-082 | 主要功能 L1405 | 无 | 同上 | 否 |
| BSM-083 | 和谁有关 L1414 | 无 | 同上 | 否 |
| BSM-111 | 经营看板 h2 L1781 | 无看板指标 | 同上 | 否 |
| BSM-112 | 主要指标 L1785 | 无 | 同上 | 否 |
| BSM-113 | 谁会看 L1813 | 无 | 同上 | 否 |
| BSM-114 | 各类入口 L1827 | 十一端无 git CLI；本模块不是那些入口的实现 | 同上 | 否 |
| BSM-115 | 相互关系 L1897 | 无子系统连接实现 | 同上 | 否 |
| BSM-116 | 系统交付与上线 L1928 | 原文：生产换弱口令、U 盘密钥、安装包+教程。本模块是仓库变更/CI 可见信号，**不实现也不改写**换钥规则。不把 hook/CI 硬套成该章细化上位 | 无冲突；技术性（CI 脚本） | 否 |
| BSM-117 | 正式上线时的安全加固 L1932 | 无 U 盘/最高权限口令 | 同上 | 否 |
| BSM-118 | 安装包+教程 L1940 | 无安装包 | 同上 | 否 |
| BSM-119 | 和谁有关 L1947 | 无 | 同上 | 否 |
| PGM-015 | 楼宇运营入口 L399 | 无 | 同上 | 否 |
| PGM-018 | 物理项目与运营公司多对多 L415 | 无主体拓扑 | 同上 | 否 |
| PGM-026 | 商户和主体·主要功能 L492 | 无商户档案/四类主体 | 同上 | 否 |
| PGM-039 | 智能设备场景联动范围 L636 | 无设备编排；未发明计时产品 | 同上 | 否 |
| PGM-052 | 楼宇运营 h2 L806 | 无 | 同上 | 否 |
| PGM-053 | 楼宇机电设备 L810 | 无 | 同上 | 否 |
| PGM-054 | 能耗计量 L821 | 无 | 同上 | 否 |
| PGM-055 | 楼宇巡检 L832 | 无 | 同上 | 否 |
| PGM-056 | 和谁有关 L844 | 无 | 同上 | 否 |
| PGM-066 | 已拍板规则 L951；含「界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端…」L965 | 本模块无这些终端的界面。YAML 键名受 GitHub 工作流规范。CLI 提示以中文为主。未发明日结/企微/下单双时间等拍板规则 | 无冲突；技术性 | 否（英文 L15 见仅登记 R3） |

**添加项：无。** 不把 git/CI 流程登记为业务发明。

## 4. 对抗式（假定实现错误）

内用 grilling，事实自读。

1. **直推 main 是否真拦？** 喂 `refs/heads/main` → exit 1。`mainish`/`main/topic` 不误伤。删除 main 拦。`--no-verify` 可绕：文件头已写非安全边界，不升未声明安全缺陷。  
2. **本地 main 推到 feature？** 整行含 `refs/heads/main` 被拦（偏严）。非常用。仅登记 R4。  
3. **propose 是否推了未提交内容？** 不会 push 工作区；但本地闸读工作区。未提交的「本该进 PR 的改动」会被丢掉，门禁却可能因工作区而绿。**GR-01**。  
4. **merge 是否合错 SHA？** 等待后再比 origin 与 `headRefOid`；漂移中止。`--match-head-commit` 写入真实 argv。  
5. **CI 红能否合？** watch 非 0 中止且不调 merge。SKIP_CI_WAIT=1 仍 merge，但打印警告——与注释逃生口一致，不升。  
6. **gh 挂了当「没 run」？** 是。**GR-02**。  
7. **同一 SHA 多个 run？** 源码 `for rid in $run_ids` 全 watch；一个失败即拦。只读列表见同一 `headSha` 两个 `pull_request` success id。未观察到同 SHA 一红一绿。重开 PR 可能留下旧失败 id。要两件同时发生。仅登记 R5。  
8. **PR 基线不是 main？** 源码不读 `baseRefName`（`gh pr view --help` JSON 有该字段），警告文案写「到 main」。本仓正道 `propose-change` 固定 `--base main`。第一次按正道跑不撞。仅登记 R6。  
9. **workflow 是否对 feature push 触发？** `on.push.branches:[main]` 不触发 feature。`on.pull_request` 在开 PR 后触发。propose 先 push 再 create，create 后才有 PR run。merge-pr 等的是该 commit 的 run。只读列表证实 feature SHA 上有 `pull_request` run。  
10. **镜像分叉？** 当前工作区两份 yml `cmp` 0。HEAD 旧注释若只提交一份会红（M008 机检）——属工作区未提交状态，不另开本模块缺陷。  
11. **hook 未安装？** 本仓 `.git/hooks/pre-push` 与源字节相同、可执行、无 `core.hooksPath`。新克隆须跑 install：脚本头已写。  
12. **伪成功？** 未发现 exit 0 掩盖失败的主路径（GR-02 是失败被换了理由，仍非 0）。

## 5. Findings（工程缺陷，进 B2 队列候选）

### GR-01

- **位置**：`ops/160/propose-change.sh:11-15`（对照 `ops/160/merge-pr.sh:17-21`）
- **冻结判据**：GEN-002（声称：本地 predeploy 红则不推；入口应对将推送的 commit 树）
- **2.1 分类**：技术性（CI/变更脚本；无业务发明：不引入 HTML 实体）
- **2.3 依据类型**：④ 可复现命令 + ② 可执行产物（原始脚本）
- **证据**：  
  - 源码在 `npm run verify:predeploy` 与 `git push` 之间无 `git status --porcelain`。  
  - 隔离：假 npm 成功、假 git 不实现 status 仍被调用 push（`propose:no-dirty-check-in-source` PASS；成功路径写出 `propose.pushed`）。  
  - merge-pr 在同类风险上明确拒绝脏树并说明「CI 跑的是远端 commit」。  
- **业务含义**：无票务/商户含义。工程含义：本地闸看工作区，远端/PR 看 commit，两棵树可以不是同一份；未提交的修复不会进 PR，未提交的「让门禁绿」的改动也不能代表将合并的代码。  
- **根因**：propose 入口只拦「在 main / 无标题 / 本地闸红」，不拦「工作区与 HEAD 不一致」。  
- **最小修法**：与 merge-pr 同样：`git status --porcelain` 非空则 exit 1，提示先提交或 stash。不改 HTML、不加新闸类型。  
- **工程缺陷是否成立**：**是**

### GR-02

- **位置**：`ops/160/merge-pr.sh:44-53`
- **冻结判据**：GEN-009（错误须可检测、可定位；不得吞错后用另一故事掩盖）
- **2.1 分类**：技术性（无业务发明）
- **2.3 依据类型**：④ + ②
- **证据**：  
  ```
  run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' 2>/dev/null || true)"
  ```  
  隔离：假 `gh run list` exit 1 写 stderr「fail」→ 脚本仍走满空列表分支，stderr 为「等了 5 分钟仍没有 … run」，**不含** gh 原错误（`merge:run-list-error-swallowed-as-no-run`）。官方 `gh run list --help` 列出的失败模式（未登录、workflow 名错、网络）因此不可定位。  
- **业务含义**：无。工程含义：CI 查询失败被说成「没有 run」，操作者可能误用 `SKIP_CI_WAIT=1`。仍 fail-closed（不合），但是错误语义假。  
- **根因**：`2>/dev/null || true` 把 gh 非 0 与「确实还没有 run」合成空字符串。  
- **最小修法**：去掉 `|| true`；gh 非 0 则把 stderr 打出并 exit 1。空 stdout 且 gh 0 才进入「还没有 run」重试。  
- **工程缺陷是否成立**：**是**

## 6. 仅登记（不进修复队列）

| 编号 | 位置 | 判据 | 分类 | 为何不修 |
|---|---|---|---|---|
| R1 | 本模块无测试文件 | GEN-003 | 技术性 | 关键回归在 M008；提示词禁止本席扩 FILES 或开下一模块 |
| R2 | SKIP_CI_WAIT/ALLOW_MAIN_PUSH 只 stderr 提「写原因」 | GEN-011 | 技术性 | 已声明逃生口；不是 HTML「业务动作必须留痕」 |
| R3 | `install-git-hooks.sh:15` `installed pre-push hook ->` | GEN-015 / PGM-066 | 技术性 | PGM-066 原文列举的是后台/客人端/设备/安卓，不是 git CLI；一句英文不构成业务发明。可在有 ID 时顺手改中文，不单独立项 |
| R4 | pre-push 整行匹配 `refs/heads/main`，本地 main→非 main 也拦 | GEN-002 | 技术性 | 偏严 fail-closed；正道不用这条 push |
| R5 | 同一 SHA 多个 run 全 AND | GEN-002/009 | 技术性 | 要「旧失败 id 仍出现在 --commit 列表」才伤；样本 30 条未见同 SHA 一红一绿。铁律 6：两三件不巧才成立 → 不修 |
| R6 | merge-pr 不读 `baseRefName` | GEN-002 | 技术性 | 正道 propose 固定 `--base main`；第一次按脚本跑不撞 |
| R7 | pre-push 无 `set -euo pipefail`，异常时落到 L17 `exit 0` | GEN-009 | 技术性 | echo/[[ 失败不是第一次正常跑会撞的路径 |
| R8 | action SHA↔版本标签 | GEN-013 | 技术性 | 本席未读 GitHub 发行页，未验证前提，不编缺陷 |
| R9 | `ops/ci` 与 `.github/workflows` 双份 | GEN-001 | 技术性 | M008 机检要求字节一致；删一份会红。不是死重复 |

**冲突类：0。添加类：0。**

## 7. 分类汇总

| 类 | 处置 | 本模块 |
|---|---|---|
| 冲突 | 删除/修复队列 | 无 |
| 细化 | 照常审工程 | 无必须实现的 HTML 下位（BSM-116 原文不是 git/CI） |
| 技术性 | 不删不问；有工程缺陷仍进队列 | 五文件整体；GR-01、GR-02 |
| 添加 | B2 问操作员 | 无 |

## 8. 验证命令、输出与限制

独立锁哈希（正文用不同标签，避免第二处独占字段）：

```
LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
SIDECAR:         b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
LEDGER-TOP:      b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（本席独立实算，非派发值）。

**只读远端（GET，无 create/merge/PUT/push）**

- `gh auth status`：登录 `jxh1979`，scope 含 `repo`/`workflow`
- `gh api repos/jxh1979/mall/actions/workflows/predeploy-gates.yml`：`state=active`，`path=.github/workflows/predeploy-gates.yml`，`name=predeploy-gates`
- `gh run list --commit dc7b735de4dd5a7a1d9a2ea4c3d0dfe8c9b5a7f4 --workflow predeploy-gates.yml`：一条 `pull_request` success，`databaseId=34198944107`
- `gh run list --commit 242f487233d028173cb9198eecd079f5d877405c --workflow predeploy-gates.yml`：一条 `push` success（main）
- `--workflow predeploy-gates.yml` 与 `--workflow predeploy-gates` 都能列出

**官方规范（本机 man / --help / npm help）**

- `man githooks` pre-push 四元组；删除时 local ref=`(delete)`、local sha=zeros
- `man git-config` `core.hooksPath` 相对路径相对 hook 运行目录
- `gh pr merge --help`：`--match-head-commit SHA`、`--squash`/`--merge`/`--rebase`
- `gh run watch --help`：`--exit-status`、`--compact`
- `gh run list --help`：`--commit`、`--workflow`；并提示 PR 关联可用 `gh pr checks`
- `npm help ci`：`ignore-scripts` 仍允许随后 `npm run`

**隔离实跑原始脚本（`/tmp/m007-b1-*`，事后不作为交付物）**

pre-push 14+1 探针全按预期。install：默认/绝对 hooksPath/相对 `.githooks`/worktree→common dir。merge：method/main/dirty/无远端/SHA 不一致/无 PR/无 run/list 失败吞掉/watch 失败/绿灯 squash/rebase/多 run 一失败/等待漂移。propose：无标题/在 main/npm 红不推/create `--base main`/已有 PR 不 create/默认正文。`bash -n` 四脚本 0。两份 yml 字节相等。

第一次 harness 中 `install:worktree-common-dir`、`merge:dirty` 失败是 **harness 自己**（worktree 未提交 ops 文件；假 git `case` 被字符串替换写坏）。复跑后 install worktree exit 0 装到 common hooks；dirty merge exit 1 打印「工作区有未提交改动」。**不把 harness 失败记成产品缺陷。**

**明确没做**

- 业务仓 `git push`/`commit`/`merge`、`gh pr create`/`merge`、覆盖 `~/complex-management/.git/hooks`
- 全仓 `npm run verify:predeploy`、生产构建、部署
- 浏览器、数据库

范围外：部署脚本只读 `REQUIRED_GATES` 数组以核 L6；workflow 生效件在 M005；机检在 M008。故障不顺手修。

## 9. 独立性

- 未读 `M007-b1-self-raw.md`（不存在）。未用同号旧 backend M007、未用历史审计/收口结论代替本轮命令与原文。
- 调用者只读：`package.json` 的 `verify:predeploy` 字符串、`.github/workflows/predeploy-gates.yml`（对照镜像）、`tools/verify-delivery-gates.mjs` 284–304 行断言、`ops/160/complex-deploy-*.sh` 的 `REQUIRED_GATES`、`m001-regression.mjs` 的 merge 假适配器形态（未跑全文件以免克隆全仓）。未把那些模块审一遍。
- 三份 HTML 只读原句。铁律 4 未触发（HTML 自身问题未发现到要上报暂停的程度）。

## 10. 步骤结论

覆盖：5/5 文件，79/79 判据，三线均完整模块。有未定稿工程缺陷 GR-01、GR-02。无添加项、无 HTML 冲突、无未答 questions。

**FAIL**

B1 禁止修复。修复权在 Codex B3，且须 B2 定稿 ID。

## M007 B2-r1 整批候选复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 全文原样保留，未改写首个本次完整 B1 边界）
- 席位：Grok（审计席乙，独立）。步骤：B2。模块：M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；B3=0，无源修复
- 本席 B2：**PASS**
- 本追加段不写独占锁哈希字段名。观察值（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` 与 sidecar 同行、ledger 顶行相同。未改锁/sidecar/ledger/findings/HTML/源。
- 无 MCP、无子代理、无第三席。未实际 `git push`/`merge`/`commit`（业务仓）、未 `gh pr create/merge` 或 PUT、未覆盖真实 hook、未部署。

此票只判断 findings.md 顶部 V1.07「当前 M007 B2-r1 候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、修复队列与最小修法。不是独立 B1/B5 PASS，不是模块 PASS，不是源已修。必须另有 Codex 本批 B2 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。旧模块历史不重审。

本票依据只落到：当前五文件原文、`man githooks` / `git help config` 原文、隔离目录可复现命令输出。不把本席原 B1 意见、Codex 候选措辞或席位身份当正确性证明。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.07，全文件 362782 bytes，SHA256 `bae7d7a4e9b32ae4731fa980009b6cd51bfcb6dfc924ee4f6bc09878ef4a0f21`。完整读取最上「当前 M007 B2-r1 候选（尚未定稿）」001–011 段至该节结束；下方他模块历史原文未改写、不重审。
- Codex B1：`docs/audit-2026-09/modules/M007-b1-self-raw.md` V0.01，45354 bytes，SHA256 `74e5402de05dfa5154e4ae8bcb59de8ff441edd0baf7ee6fdf68fdc73a81dc03`，结论 FAIL。全文已读。
- Grok B1：本文件 V0.01 前缀 32337 bytes，SHA256 `8ffc35495fe4328d34b643ae7fd6686241ba89014193bb4e0aeb27667cd53504`，结论 FAIL。该前缀本票追加前实算仍全等，保留。
- 当前源（未执行候选修改；工作区 yml 相对 HEAD 仅既有注释 2 行，与 B1 相同）：
  - `ops/160/install-git-hooks.sh` 15 行 SHA256 `badf746b715a52239361a80671cf863a8aeadaf66b52bc579741a88138ea25d3`
  - `ops/160/merge-pr.sh` 73 行 SHA256 `8688f7dc7e9b688c2d8583ff1942137089df55dbb9e5cfda95f69f344edbd514`
  - `ops/160/propose-change.sh` 24 行 SHA256 `913a12cae30d4448ac546d56ed54c9b25e7687c966a3181bd9324ebd194865e1`
  - `ops/160/hooks/pre-push` 17 行 SHA256 `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b`
  - `ops/ci/predeploy-gates.yml` 27 行 SHA256 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`
  - 与 `.github/workflows/predeploy-gates.yml` `cmp` exit 0。`git ls-files` 五行 `wc -l` = 156。
- 主仓 `git config --get core.hooksPath` exit 1（未设）；`ops/160/hooks/pre-push` 与 `.git/hooks/pre-push` `cmp` 0。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 开工技能优先；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问结构，事实自查；不等操作员、不派子代理 |
| verification-before-completion | 声称前先跑只读/隔离命令 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码、不加诊断插桩 |
| codebase-design | 删除测试：五文件仍有存在价值；本票不扩接口 |
| ponytail-review | 接受候选最小修法（改已有字段/时间点，不新增闸/角色/文件类型） |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本体要写 hook 配置 → **未用** |
| resolving-merge-conflicts | 本节点无合并冲突 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到 batch-grill-me SKILL.md**；ubiquitous-language 会另写超 §7 文件 → 均未冒充调用 |

未用 MCP。未派子代理。`grill-me` 仅转发 grilling。

### 完整差集（CX01–05、GR01/02、R1–R9）

Codex 本次临时项：CX-01、CX-02、CX-03、CX-04（工程缺陷否）、CX-05（工程缺陷否）。Grok 本次临时项：GR-01、GR-02、R1–R9。漏并：无。篡改：无。无锁外新判据、无新强制门、无第三主体、无添加项、无 questions、无 HTML 自身问题上报。

| 正式ID | 全部本次来源 | 四分类 | 工程缺陷 | 修复队列 | 本席票 | 差集结论 |
|---|---|---|---|---|---|---|
| M007-001 | CX-01 + Grok R4（同一 `pre-push` 主源） | 技术性 | 是 | 是 | **是/是** | 全并。B1 将 R4 仅登记为偏严；本票独立改判，见下节。不是本方覆盖本席 |
| M007-002 | CX-02（Grok B1 未覆盖 tilde） | 技术性 | 是 | 是 | **是/是** | 全并。不得用本席默认/相对/绝对通过覆盖此失败 |
| M007-003 | CX-03 + GR-01 | 技术性 | 是 | 是 | **是/是** | 全并。两席同事实；self 真实 Git/npm 反例本票独立复现 |
| M007-004 | GR-02；CX-04 正常路径拆到 005 | 技术性 | 是 | 是 | **是/是** | 全并。拆分不是埋葬。本席 B1 已立 GR-02；Codex 收报后补验与本票 exit2 复验同向 |
| M007-005 | CX-04 + Grok R2；兼含 hook 既有覆盖提示 | 技术性 | 否 | 否 | **否/否** | 全并。不改 SKIP_CI_WAIT / ALLOW_MAIN_PUSH |
| M007-006 | CX-05 + Grok R8/R9 | 技术性 | 否 | 否 | **否/否** | 全并。R8 验证限制保持：未核 action SHA↔注释版本，不编缺陷。不删镜像 |
| M007-007 | Grok R1 | 技术性 | 否 | 否 | **否/否** | 全并。不把 FILES 无测试当遗漏 |
| M007-008 | Grok R3 | 技术性 | 否 | 否 | **否/否** | 全并。本席 B1「可顺手改中文」**不采纳**：无缺陷 ID 不改；不是篡改 R3 现状 |
| M007-009 | Grok R5 + CX-04 组合限制 | 技术性 | 否 | 否 | **否/否** | 全并。铁律 6：两三件不巧 → 不修 |
| M007-010 | Grok R6 | 技术性 | 否 | 否 | **否/否** | 全并。不增加 PR 基线门 |
| M007-011 | Grok R7 | 技术性 | 否 | 否 | **否/否** | 全并。不加 set -euo 兜底 |

冲突 0。添加 0。细化 0。技术性 11。无业务发明总理由：提示词 2.1 明文 CI 脚本不算业务发明；五文件不引入票种/状态/角色/价格/审批链/商户等 HTML 实体。BSM-116 原文是生产换钥/U 盘/安装包，不是 git/CI 上位，故不进细化。

### 001 独立裁定（本席 B1 R4 偏严 → 本票改判）

**B1 原文保留：**对抗式第 2 条与 R4 写「整行含 `refs/heads/main` 把本地 main 推到非 main 远端一并拒绝（偏严）。非常用。仅登记。正道不用这条 push」。该 FAIL/R4 原文不改。

**本票要裁定的不是「当时有没有看见整行匹配」，而是：该路径是否满足 V0.17 铁律 6「第一次正常跑就会撞上」的常规缺陷标准。** 候选不得覆盖本席；本席也不得用 B1 意见预定结论。

当前源 `ops/160/hooks/pre-push:5–8`：`while read -r line` 后 `[[ " $line " == *" refs/heads/main "* ]]`。文件头声称禁止直推 main；L6–7 自称「稳健取 remote ref」。

类型 3：`man githooks` pre-push 标准输入为 `<local ref> SP <local object name> SP <remote ref> SP <remote object name> LF`，**其唯一举例即** `git push origin master:foreign` → `refs/heads/master … refs/heads/foreign`。类型 4：

- 原脚本 stdin 四字段、无网络：`refs/heads/main <40a> refs/heads/feature/a <40b>` → exit 1，stderr「拒绝直推 main」。`feature→feature` exit 0；`feature→main` exit 1；`ALLOW_MAIN_PUSH=1` 放行；`mainish` / `main/topic` / `refs/tags/main` 不误伤；删除 main 仍拦。
- 隔离真实 Git、file 远端、原 hook：`git push origin main:refs/heads/feature/from-main` exit 1，同句拒绝；同仓 `checkout -b feature/ok && git push origin HEAD` exit 0。无 GitHub、无并发。
- `git check-ref-format 'refs/heads/with space'` exit 1。L7「源 ref 含空格」前提不成立。

铁律 6：「两三件不巧才成立 → 不修」。此处是**一次合法 Git 四字段输入、原脚本单次执行**即误拦，不需要网络、第二事件或罕见组合。官方 hook 规范的首个例子就是「本地名与远端名不同」。错误语义把非 main 目标说成「直推 main」（GEN-009），并诱导 `ALLOW_MAIN_PUSH=1`。GEN-002：声称取 remote ref / 拦直推 main，实际匹配任一场含该子串。

**本席 B2 改判：工程缺陷是，修复队列是。** 与候选 001 队列归属相同；与本席 B1「仅登记」**明示分歧**，以本票为准，不是 Codex 覆盖。最小修法同意候选：按第三字段 `remote_ref` 精确匹配 `refs/heads/main`，删「源 ref 含空格」误导注释，保留 ALLOW_MAIN_PUSH 及既有 feature→main / 删除 / 覆盖路径。不把 hook 升为安全边界，不改 `--no-verify` 声明。

### 002 独立裁定（Grok B1 未覆盖）

B1 只跑默认 / 绝对 / 相对 `.githooks` / worktree→common，**没有** `~/…` 分支。不得用那些通过覆盖失败。

当前源 `install-git-hooks.sh:6–14`：`custom="$(git config --get core.hooksPath || true)"` 后 `dest="$custom"`。`git help config`：pathname 值 `~/` / `~user/` 须 tilde 展开；`core.hooksPath` 相对路径相对 hook 运行目录。该脚本存在的职责就是处理 hooksPath。

类型 4（隔离真实 Git，HOME 改到临时目录，原安装脚本）：

| 模式 | 脚本 exit | Git 实际 hooks 路径有 hook？ | 备注 |
|---|---|---|---|
| 默认 | 0 | 是 `.git/hooks/pre-push` | 与 B1 同 |
| 相对 `.custom-hooks` | 0 | 是 | 与 B1 同 |
| 绝对 | 0 | 是 | 与 B1 同 |
| `core.hooksPath=~/custom-hooks` | 0 | **否**（Git 期望 `HOME/custom-hooks`） | 脚本写入仓内字面 `~/custom-hooks`，stdout「installed … -> ~/custom-hooks/pre-push」 |

`git config --path --get core.hooksPath`：tilde → `$HOME/custom-hooks`；相对仍 `.custom-hooks`；绝对不变；未设置则 exit 1（现有 else 分支 `git-common-dir/hooks` 仍可用）。`git rev-parse --git-path hooks` 对 tilde 同样展开到 HOME。worktree 默认 `git-path hooks` 仍指向 common `.git/hooks`，与现脚本 worktree 职责一致。

铁律 6：合法 Git 路径首次安装即撞上，不是两三件不巧。主仓当前未设 hooksPath、生效 hook 等于源，不夸大成主仓已失效。GEN-002/009/013/016。

**工程缺陷是，队列是。** `git config --path --get` 作为最小原生解析合适（本机 Git 2.43.0 不破坏相对/绝对/默认）；`git rev-parse --git-path hooks` 等价可接受。B3 必须维持现有安装职责与默认/相对/绝对/worktree-common，不加新配置键。

### 003 独立裁定（CX-03 + GR-01）

源 `propose-change.sh:8–15`：`npm run verify:predeploy` 后直接 `git push`，从不读 `git status --porcelain`。声称「本地跑 predeploy 门禁(红则不推)」。merge-pr:17–21 对同类风险已拒绝脏树。

类型 4（隔离真实 Git + 真实 npm，只 stub `git push`/`gh`）：已提交 `gate.txt=bad` 时 `npm run verify:predeploy` exit 1；改工作区为 `good` 后原 `propose-change.sh` exit 0；捕获的 push 对象仍是 HEAD 的 `bad`。单一未提交修改条件，无网络。与 self 工作树段及本席 B1 GR-01 同事实。

铁律 6：第一次正常跑 propose（工作区有未提交改动）即撞上。不声称绕过 merge 的 clean/SHA/CI。GEN-002/008/009/011。

**工程缺陷是，队列是。** 最小修法同意：验证前复用 merge 已有干净工作区前提。

### 004 独立裁定（GR-02；CX-04 拆分）

源 `merge-pr.sh:44`：`run_ids="$(gh run list … 2>/dev/null || true)"`。空则当「还没有 run」重试 30 次后谎报无 run。

类型 4：假 `gh run list` 每次 exit 2、stderr `AUDIT_QUERY_AUTH_FAILURE`，`sleep` 空操作。原脚本：30 次 list + 30 次 sleep 后 exit 1，stderr 为「等了 5 分钟仍没有 … run」，**不含** `AUDIT_QUERY_AUTH_FAILURE`；未调用 `pr merge`。仍 fail-closed，但是错误语义假（GEN-009）。`gh` 未登录/权限失败是等待 CI 路径的第一次正常错误，不是两三件不巧。

CX-04 对其它 merge 正常/失败/SHA 前移/match-head/SKIP 的通过记录拆到 005，不改写 GR-02。本席 B1 假 gh exit 1 已见同构；本票用 exit 2 再验，与候选「Codex 收报后补验」同向，不是把 FAIL 改 PASS，也不扩大等待。

**工程缺陷是，队列是。** 最小修法同意：查询非 0 保留错误并立即失败；真实空列表且 gh 0 才继续原 30 次。不改 SKIP_CI_WAIT 策略。

### 005–011（仅登记，明确不采纳无 ID 改动）

- 005：merge 正常路径与逃生口提示已核；原因留痕不是业务审批。队列否。不改 SKIP_CI_WAIT/ALLOW_MAIN_PUSH。hook 既有覆盖（feature→main 拦、覆盖开关、mainish 不误伤）保持；001 只改误拦 local main→remote 非 main，不是拆掉这些覆盖。
- 006：YAML 可解析、事件/job/SHA 钉死/`fetch-depth:0`/Node 22/`npm ci --ignore-scripts`/`verify:predeploy`、镜像字节相等、workflow active。R8：本席仍**未**读 GitHub action 发行页核对 SHA↔v4.3.1/v4.4.0，**不编缺陷**。R9：双份是机检约束不是死重复。无改动，不删镜像，不声称生产准入/全仓绿。
- 007：FILES 无测试；merge 回归在 M008。若 B3 修可执行行为，按定稿 ID 加最小有效回归，不另立测试工程。
- 008：`install-git-hooks.sh:15` 一句英文。PGM-066 原文列举后台/客人端/现场设备/安卓端，不是 git CLI。本席 B1「可在有 ID 时顺手改中文」**本票明确不采纳**——无缺陷 ID 不改。R3 现状仍是仅登记、队列否。
- 009：同 SHA 多 run 全 AND；需旧失败 id 仍出现在 `--commit` 列表才伤。铁律 6 不修。
- 010：不读 `baseRefName`；正道 propose 固定 `--base main`。不增加 PR 基线门。
- 011：无 `set -euo`；echo/`[[` 失败不是第一次正常跑。不加兜底。

### 分类 / 队列 / 允许的最小修复边界

全批 001–011 分类技术性成立。队列候选 001–004 是、005–011 否，与本席票全等。

若 B2 双 PASS 后一次 B3：只修定稿队列内行为，并按各 ID 归因最小可失败测试（GEN-003；TDD 节点到 B3 才触发）。允许边界：

- 001：`pre-push` 按第三字段匹配 `refs/heads/main`；删空格误导注释；文件加入/递增版本头。保留 ALLOW_MAIN_PUSH、删除 main 拦截、feature→main 拦截、mainish/tag 不误伤。
- 002：安装 dest 改用 Git 原生路径解析（`--path` 或 `--git-path hooks`，须保持默认/相对/绝对/worktree-common）。不加新配置。
- 003：`propose-change.sh` 在 `npm run verify:predeploy` 前拒绝非空 `git status --porcelain`，提示对齐 merge 现句。不改 merge 的 CI/SHA 逻辑，不声称绕过最终合并 CI。
- 004：去掉 list 查询的 `2>/dev/null || true` 吞错；gh 非 0 立即失败并保留 stderr。空列表且成功才重试。不把失败改成合并成功，不扩大 30×10s，不改 SKIP_CI_WAIT。L58 `run view` 的 `|| true` 不在本 ID 范围内。

禁止：改三份业务 HTML；改两份 CI 镜像（`ops/ci/predeploy-gates.yml` 与 `.github/workflows/predeploy-gates.yml`）；改既有显式覆盖开关；锁外新判据；新强制门；第三主体；无 ID 顺手改中文（008）；为 R5–R7 加过滤/基线门/set -euo。全部改动文件自身版本递增（此前无版本的加入 V0.01）。

### 本票隔离实验（临时目录，事后非交付物）

- `/tmp/m007-b2-001-*`：原 hook stdin + 真实 Git file 远端 push
- `/tmp/m007-b2-002-*`：真实 Git 四种 hooksPath + `--path` / `--git-path` / worktree
- `/tmp/m007-b2-003-*`：真实 Git/npm，只 stub push/PR
- `/tmp/m007-b2-004-*`：原 merge-pr.sh，gh list exit2 + 标记被吞，sleep stub，30 次后不合并

未对业务仓 push/PR/merge/部署。未跑全仓 `verify:predeploy`。

### 结论

全批差集成立，分类按上表，修复队列按上表。001 与本席 B1 R4 的分歧已明示并以本票铁律 6 + 规范原文 + 单次合法输入为准。002–004 独立复验成立。005–011 仅登记，R3 顺手中文不采纳，R8 未查不编。未源修、未改 findings。本席 B2 **PASS**。须 Codex 本批 B2 PASS 后才能定稿并一次修复 001–004；未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M007 IN_PROGRESS，B3=0。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M007 B1 Grok 独立审计报告（现锁 V0.14／当前五源 + 直接回归）

- 报告自身版本：V0.03（追加；保留本文件既有 V0.01 B1 + V0.02 B2 全部 **49424** 字节，SHA256 `09b192b4e7b4b358a6fc7e139e0196a4893d803aa9630be14043603285848155`，不覆盖。末版本核为 V0.02，按真实递增 V0.02→V0.03）
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（`docs/audit-2026-09/160-audit-prompt.md` 当前不存在）；`plan/runbook.md` V0.02 不另扩流程
- 席位：Grok（乙），现锁修复前全量独立审计（步骤 B1）。不修复、不改方案、不写 questions/findings/ledger/锁/HTML/源、不派子代理、不 MCP、不 fetch/commit/push/worktree/部署/主仓安装 hook/真实 GitHub PUT/DB；固定 CLI 参数未改
- 模块：M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段（`plan/modules.md` V0.04；标记 `NORMAL`；计划历史有效行 **156 不改**；当前 `git ls-files -z -- <五路径> | xargs -0 cat | wc -l` = **163**）
- 唯一受审 FILES（5）：`ops/160/install-git-hooks.sh`、`ops/160/merge-pr.sh`、`ops/160/propose-change.sh`、`ops/160/hooks/pre-push`、`ops/ci/predeploy-gates.yml`。**不是**旧同号权限后端模块
- 必要一并读且隔离执行：`tools/gate-hardening-tests/m007-regression.mjs`（102 行，不在 FILES，不扩范围）
- 映射：`plan/criteria-lock.md` V0.14 行 `M007 → [BEQR-085…096,118,146,155,160–181, BSM-020,028,081–083,111–119, GEN-001–018, PGM-015,018,026,039,052–056,066]`（**79** 唯一；定义区 79/79 自读存在；HTML 派生 61 条锁摘录与现文锚点行 **MISMATCH_N=0**）
- ledger（只读顶部两行 + 本模块行，不写）：顶部基准 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，`LOCK-AMEND-COUNT: 1`；M001—M006 现锁 PASS；M007 `IN_PROGRESS`，历史 B3 累计 **1**、方案修复 2、失败重跑 0（本席不清零、不改）。表行仍记旧 B5 链，**本票不继承 PASS**
- 正本：`/home/james/complex-management` 分支 `main`；工作区 `HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=ebb1eb99c610df330c07d24278798c2d3611299a`（ahead 49 / behind 54，本席未 fetch）。五 FILES + 回归相对 HEAD 与 origin/main **blob 全等**，porcelain 空
- 输入：旧 001–004 历史修复 / 005–011 登记仅作编号与原句线索，本轮对现行字节独立重判，不把历史 B2/B4/B5 PASS 当现行证据。可读本席旧票 V0.01/V0.02（同文件前缀）。**未读任何 `*-self-raw.md`，未读本次 selfB1**
- 本席 B1 结论：**PASS**
- HTML 自身问题（本模块范围内）：**无上报**。不扩审 M008 全量、不重开 M001—M006、不抢审后续。全局 PGM 台账红 / 入口快照漂移 / runtime 过期已登记后续所属模块，不刷绿

---

## 0. §6 技能自核（本席实际环境）

开工前按本机路径列目录并读 `SKILL.md`。用户本轮指令覆盖 skill 默认：无 MCP、无子代理、不等操作员、不问人、不写 findings/ledger/锁/源。Arc 默认全量派席 **不作废本节点**。grilling 本体「等用户回答 / 事实题派子代理」作废：只借追问结构，事实自读。

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由；用户禁子代理／禁等人覆盖其默认 |
| `using-arc` | Arc `skills/using-arc/SKILL.md` | 仅路由；**未**调用 `arc:audit`（会写 §7 外产物并派子代理） |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md` | `graft ask` 定位回归文件；`graft grep "install-git-hooks.sh" --in ops/` 报告 ops/ 未索引，改直读五源全文 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`、`productivity/grill-me`（后者仅转发 grilling） | 对抗式／第一性原理追问树；**自问自答**；不派子代理、不问操作员 |
| `codebase-design` | mattpocock `engineering/codebase-design` | 删除测试：五文件赚到「人记得看 CI／不直推 main／两份 yml 一致」的复杂度 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 隔离 harness 作红/绿信号；B1 不进入修复相 |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；B1 **不改**生产代码、不加诊断插桩 |
| `ponytail-review` | ponytail `4.9.0/skills/ponytail-review` | 找可删复杂度；不把正确性当 ponytail 项。现行五文件：`Lean already. Ship.`（双份 yml 是机检约束，不是死重复） |
| `verification-before-completion` | superpowers | 任何 PASS/FAIL 前用本报告内命令输出 |
| `writing-for-agents` | mattpocock `productivity/writing-for-agents` | 本 raw 按下游 B2 可差集、可归因来写；未改 AGENTS.md |
| `receiving-code-review` | superpowers | 只借先核实再接受或反驳；B1 禁止修复 |
| `tdd` / `test-driven-development` | 存在 | B1 禁止修复，未落测试文件；只跑既有回归 |
| `i-have-adhd` | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号与直述；**报告首行被提示词锁行覆盖**（技能自身：harness 约束优先） |
| `ponytail` | 存在 | B3 才用；B1 不修 |
| `requesting-code-review` | 存在 | B4 才用 |
| `resolving-merge-conflicts` | 存在 | 本节点无进行中 merge/rebase |
| `git-guardrails-claude-code` | 多宿主副本存在（如 `~/.qwen/skills/…`） | 本体要问安装范围并写 hook 配置 → 第四闸 + 写仓；**未用** |
| `ubiquitous-language` | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` 等存在 | 会另写词表文件超 §7；本 FILES 无词表；**未调用** |

缺口（如实，不冒充已调用）：

- `batch-grill-me`：本环境技能目录**未找到**独立 `SKILL.md`

明确不用：`brainstorming`／`writing-plans`（阶段 A）；§6 划掉的子代理／worktree／research／ponytail-audit／debt／gain。未派子代理。未改 HTML／findings／ledger／lock／sidecar／五受审文件／回归文件。未执行 `npm install`／真实 `npm ci`／全仓 `verify:predeploy`／部署／`gh api -X PUT`／`gh pr merge`／业务仓 `git push`／主仓 hook 安装。临时 Git fixture init/commit 与模拟 gh/npm 仅在 `/tmp/m007-b1-*` 与 `.scratch/m007-regression-*`（后者 finally 删除）。

---

## 1. 输入哈希、行数、覆盖

本席独立实算（未抄 sidecar／派发期望值）。正文不再重复锁字段名。

| 项 | 值 |
|---|---|
| `plan/criteria-lock.md` | 本席 `sha256sum … \| awk '{print $1}'` 写在本次报告第一行；sidecar 单行同值；ledger **顶部**同值。ledger L7030 另有历史字段 `cf25d716…`，**不是**闸 1 基准，不取 |
| 锁版本 | V0.14；判据总数 401；`LOCK-AMEND-COUNT: 1`；M007 **79** |
| `ops/160/install-git-hooks.sh` | 16 行／707 字节／内容 SHA256 `8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b`；blob `c3dc157dbfd23e77c25a071e5bf628ebe289cef9`；标题 V0.01；LF、无 CRLF/BOM/NUL |
| `ops/160/merge-pr.sh` | 73 行／4180 字节／SHA256 `3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06`；blob `ef2a97b4f00bd13b26a01ecb2b74555dd0a38eaa`；标题 V0.02 |
| `ops/160/propose-change.sh` | 30 行／1626 字节／SHA256 `88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b`；blob `35a5139e2b176f88a5041cf4bbed4c0249898589`；标题 V0.01 |
| `ops/160/hooks/pre-push` | 17 行／922 字节／SHA256 `e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85`；blob `3ae3d237d26ba0f5bd937f36c7e8f856864f8b6f`；标题 V0.01 |
| `ops/ci/predeploy-gates.yml` | 27 行／1335 字节／SHA256 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`；blob `3857c0f9b3ca98e4d2e8d94771e91be606e0e732`；标题 V0.02。与 `.github/workflows/predeploy-gates.yml` `cmp` exit 0（字节全等）。内容 SHA 与本席旧票 V0.01 所记相同，**仍全文重读 27 行** |
| 官方有效行 | **163**（计划历史 156，冻结计量不改） |
| 直接回归 | 102 行／5239 字节／SHA256 `10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8`；blob `6fc4720e3e704539e28a9d488b967a28a79548cf`；标题 V0.01 |
| 旧本文件前缀 | 49424 字节／`09b192b4e7b4b358a6fc7e139e0196a4893d803aa9630be14043603285848155` |
| 宿主 | node v24.21.0；npm 11.19.0；git 2.43.0；gh 2.100.0；hostname `y9000p-ai`；IPv4 含 `192.168.88.160/24`。根清单 `packageManager=npm@10.9.8`（本机工具链差披露，不写成五文件缺陷） |

全行覆盖：install 1–16、merge 1–73、propose 1–30、pre-push 1–17、yml 1–27、回归 1–102 均进入三线，无抽样冒充整文件。`bash -n` 四脚本 exit 0。yml `yaml.parse` 成功。

三 HTML 只读 SHA：枚举 `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`／业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`／治理地图 `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`。

---

## 2. 第一性原理线（2.3 三问）

依据类型只许：① HTML 逐字 ② 可执行产物 ③ 官方规范原文 ④ 可复现命令输出。

### 2.1 `ops/160/install-git-hooks.sh`（16 行，V0.01）

1. **为何必须存在 / 删除代价**  
   把受控 `ops/160/hooks/pre-push` 装到 Git 实际生效的 hooks 目录（默认 common dir，或 `core.hooksPath`）。删除后新克隆／改了 hooksPath 会静默没有拦截。三份 HTML **没有**「必须装 git hook」句。最接近交付章 BSM-116 L1928–1952 原文是生产换弱口令、U 盘密钥、安装包+教程，**不是** git hook。答不上 HTML 哪一条会做不到 ≠ 该删。按 2.1：**无冲突 → 指不出必须实现的 HTML 上位 → 技术性（CI/工程脚本，无业务发明）**。
2. **声称是否执行**  
   隔离真实 Git（`/tmp/m007-b1-adv-*`，未装主仓）：默认装到 `rev-parse --git-common-dir`/hooks，mode 可执行，字节等于源；`core.hooksPath=~/custom-hooks` 时 `git config --get` 仍为字面 `~/custom-hooks`，`git config --path --get` 展开为 `$HOME/custom-hooks`，产物在 `HOME/custom-hooks/pre-push`，**仓内不出现字面 `~` 目录**；绝对路径 hooksPath、相对 `.githooks`、worktree 执行装到 **common** hooks：均 exit 0。`git help config` pathname：`~/` 展开为 `$HOME`（类型 3）。未在业务仓跑安装器。
3. **未验证前提**  
   未在 macOS/`install` 非 GNU 环境跑。本机 `git 2.43.0`：相对 hooksPath `--path` 取值仍为 `.githooks`（未绝对化），脚本先 `cd` 仓库根再 `mkdir/install`，相对路径落在仓根 `.githooks`——与本席实跑一致，不把「所有 Git 版本都把相对路径绝对化」当真。

### 2.2 `ops/160/hooks/pre-push`（17 行，V0.01）

1. **为何必须存在**  
   补私有库免费版无分支保护时「直推 main」缺口（L2–4）。删除后只靠纪律。HTML 无此句。BSM-114 十一端表（L1827 起）不含 git CLI。技术性。文件头写明「开发者提示,非安全边界」——`git push --no-verify` 可绕，本席不把「可绕」升成未声明的安全边界。
2. **声称是否执行**  
   `man githooks` 原文：stdin 行格式 `<local ref> SP <local object name> SP <remote ref> SP <remote object name> LF`；删除时 local ref=`(delete)`、local sha=zeros（类型 3）。现行源 **只按第三字段 `remote_ref == refs/heads/main`**。stdin 探针 + 真实 `git push` 到 **file://** 远端（源 hook 装在 fixture，非主仓）：推 origin main → exit 1；feature → 0；`main:refs/heads/from-main`（本地 main、远端非 main）→ **0**（001 现行行为）。stdin：feature→main 无覆盖 exit 1；`ALLOW_MAIN_PUSH=1` exit 0 且「覆盖」；`ALLOW_MAIN_PUSH=true` **不**放行；mainish、main/topic、tag `refs/tags/main` 不误伤；删除 main 拦；多 ref 含 main 拦；空 stdin exit 0。
3. **未验证前提**  
   未对真实 `origin`（GitHub）做 push。本工作区 `.git/hooks/pre-push` **不是**现行源副本（见观察 OBS-1）；本票审的是 FILES 源字节与 fixture 安装后的行为，不把本机过期安装副本写成源缺陷，也不伪称「在本工作区执行 git push 已是现行逻辑」。

### 2.3 `ops/160/propose-change.sh`（30 行，V0.01）

1. **为何必须存在**  
   声明的正道：工作区干净 → 本地 `verify:predeploy` → `git push` → 开/更新 PR。删除后易手敲直推或手开 PR 绕过本地闸。HTML 无 PR 流程句。技术性。
2. **声称是否执行**  
   真实 Git fixture + 只 stub npm/gh：无标题非 0；在 main 拒绝「不要在 main 上提议」；**脏工作区** `git status --porcelain` 非空 → exit 1 中文「未提交改动」，**不调用 npm、不 push**；干净路径调用 `npm run verify:predeploy` 后 `git push -u origin feature/p`（file 远端出现该 ref），再 `gh pr create --base main --head feature/p --title …`；已有 PR（`gh pr view` 0）则不 create；npm 非 0 不推新 commit。未对 GitHub 做 create/push。
3. **未验证前提**  
   未跑真实全仓 `verify:predeploy`（全局 PGM 台账红 / 入口快照漂移已登记他模块，本席不刷绿、不把未跑全链写成五文件缺陷）。

### 2.4 `ops/160/merge-pr.sh`（73 行，V0.02）

1. **为何必须存在**  
   私有库免费版设不了 required check。只读 `gh api …/actions/workflows/predeploy-gates.yml`：`state=active`，`path=.github/workflows/predeploy-gates.yml`，仍只是可见信号。删除后 `gh pr merge` 可在红灯合并。技术性。删除代价：失去「等 predeploy-gates + `--match-head-commit`」机器闸。
2. **声称是否执行**  
   真实 Git fixture + stub gh/sleep（cwd=fixture，非主仓）：非法 method、在 main、脏工作区、无远端分支、HEAD≠origin、无 PR、`gh run list` exit 2 立即失败且 stderr 含原错误、空列表成功则重试（本席 2 次 sleep + 第 3 次有 id 后 watch/merge）、watch 非 0 中止且不 merge、多 run 全 AND（第二个失败不 merge）、等待后 `ls-remote`/`headRefOid` 漂移中止、`SKIP_CI_WAIT=1` 打警告、不查 run、仍核 SHA 再 merge。绿灯 argv：`pr merge <branch> --squash|--rebase --match-head-commit <40位SHA>`，与 `gh pr merge --help` 原文一致（类型 3）。`gh run list --help` 有 `--commit`/`--workflow`；`gh run watch --help` 有 `--exit-status`/`--compact`。查询失败时脚本因 `set -euo pipefail` 以 **gh 的 exit 2** 退出（不是脚本里另写的 `exit 1`），仍 fail-closed、保留 stderr、不重试、不合——见 004 重判。未调用真实 `gh pr merge`。
3. **未验证前提**  
   未验证 `gh run watch` 对同一 `databaseId` 重跑 attempt 的 GitHub 侧语义（官方 help 只保证 watch 到完成且 `--exit-status` 失败非 0）。L58 `gh run view … || true` 只在 watch 已失败后丢 URL，不影响中止。

### 2.5 `ops/ci/predeploy-gates.yml`（27 行，V0.02）

1. **为何必须存在**  
   与生效定义 `.github/workflows/predeploy-gates.yml` 的字节镜像。`tools/verify-delivery-gates.mjs` L290–308（M008，只读断言段）：LF 归一后全等，且 `fetch-depth: 0`、`run: npm run verify:predeploy`、`HTML_GATES_BASE` 表达式逐字相等。删除镜像会让该机检失败。技术性（提示词 2.1 明文「CI 脚本」不算业务发明）。
2. **声称是否执行**  
   `yaml` 包 parse：`on.pull_request.branches=[main]`、`on.push.branches=[main]`、job `predeploy`、`ubuntu-latest`、checkout SHA `34e114876b0b11c390a56381ad16ebd13914f8d5` + `fetch-depth: 0`、setup-node SHA `49933ea5288caeca8642d1e84afbd3f7d6820020` `node-version: "22"`、`npm ci --ignore-scripts`、`npm run verify:predeploy` + `HTML_GATES_BASE` 表达式。只读 Actions：workflow `id=307870467` `state=active`。近 5 次 run 均 `conclusion=success`（最新 `35601416957` event=push `headSha=ebb1eb99…`＝当前 origin/main）。L6「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main」：两部署脚本 `REQUIRED_GATES` 各 15 名 **等于** `package.json` `verify:predeploy` 链 15 名（只读数组，不审部署模块）；pre-push 实跑不含 npm。`npm help ci`：`--ignore-scripts` 仍允许随后 `npm run`（类型 3）。**本轮 GET** `repos/actions/checkout/git/ref/tags/v4.3.1` object_sha＝YAML 钉死 SHA、type=commit；`setup-node` tag `v4.4.0` 同理相等（旧 R8「未核发行页」本轮已核，见 006 重判）。
3. **未验证前提**  
   `node-version: "22"` 在 GitHub runner 上装最新 22.x 的官方行为未在本机跑 runner 证实。`github.event.before` 在新建分支/force-push 的 40 个零：未构造。workflow 缺省 `permissions:` 未查当前默认文档。未跑 `npm ci`。

**删除测试（codebase-design）**：删这 5 个文件，复杂度不会消失，而会散到「人记得看 CI / 记得不推 main / 记得两份 yml 一致」。它们赚到了存在价值。不是无需求纯转发。

---

## 3. 顺从式：79 判据全覆盖

判定顺序每条都走完：冲突 → 细化 → 技术性 → 添加。GEN-017：读了 HTML 原句上下文，**不因字面词未出现就判添加**，也不把「本模块未实现游乐/地产业务」写成遗漏——那些锚点的实现模块不在本 FILES。每 ID 本轮独立核对，不抄旧票结论。

### 3.1 GEN-001 … GEN-018

| ID | 适用与证据 | 分类 | 缺陷？ |
|---|---|---|---|
| GEN-001 | 五文件均有可验证用途（装钩、拦 main、提议、等 CI 再合、CI 镜像）。镜像重复是 M008 机检约束，不是死代码。逃生口 `SKIP_CI_WAIT`/`ALLOW_MAIN_PUSH` 有注释与警告，不是为假设场景加的抽象层 | 技术性 | 否 |
| GEN-002 | 入口/主路径/错误路径已用原始脚本+真实 Git fixture / stub 边界执行。绿灯 merge argv 与 `gh` 官方 help 一致。001–004 现行行为与声称一致（见 §5 重判） | 技术性 | 否（现行） |
| GEN-003 | FILES 仍无测试文件。仓内直接回归 `m007-regression.mjs` 102 行覆盖 001–004 关键分支，本席确认只写 `.scratch/m007-regression-*` 后执行，7 项 PASS、exit 0、无残留。测试通过不替代上面的行为探针。不把「测试文件不在 FILES」升成本模块新修复项 | 技术性 | 否（007 重判） |
| GEN-004 | 标题/正文以独立 argv 传给 `gh`（`--title "$title"`），不经 shell 二次展开。覆盖开关只认字面 `1`。分支名来自 `git rev-parse`。无把未校验网络输入当权威 | 技术性 | 否 |
| GEN-005 | 无业务对象授权。合并/推送依赖操作者已有的 `gh`/`git` 凭证。默认拒绝直推 main、默认等 CI。不是界面隐藏式授权 | 技术性 | 否 |
| GEN-006 | 无业务库写入。合并钉 SHA 防止等 CI 期间 tip 漂移（已实跑漂移中止） | 技术性 | 否 |
| GEN-007 | 等待后再核 `ls-remote` 与 `headRefOid`。重复跑 merge 若已合，由 `gh` 失败 + `set -e` 中止。无并发写同一业务实体 | 技术性 | 否 |
| GEN-008 | workflow `name`/文件名与 `gh run list --workflow predeploy-gates.yml` 都能列出 run。`verify:predeploy` 字符串与 CI `run:` 一致。两份 yml 字节一致。无 DTO/枚举 | 技术性 | 否 |
| GEN-009 | 多数失败路径非 0 且中文原因。查询失败不再吞成空列表（004 重判）。L58 `|| true` 仅丢已失败 run 的 URL。pre-push 无 `set -euo` 见 011 | 技术性 | 否（现行主路径） |
| GEN-010 | 五文件无明文秘密。`npm ci --ignore-scripts` 与 `npm help ci` 原文一致。hook 非安全边界已声明。未发现注入/路径穿越 | 技术性 | 否 |
| GEN-011 | SKIP_CI_WAIT/ALLOW_MAIN_PUSH 只打 stderr「请在 PR/说明记录原因」，脚本不强制写 PR 正文。属工程提示而非业务留痕（PGM-066 留痕原文是业务动作） | 技术性 | 否（005） |
| GEN-012 | CI 等待 `seq 1 30` × `sleep 10` 有界。查询失败立即退出，不把 30 次用满。无无界文件/连接 | 技术性 | 否 |
| GEN-013 | 依赖 bash、git、gh、npm、`install`、`seq`（本机 PATH 均在）。CI 钉 action SHA 且本轮 GET tag 对象与注释版本一致。workflow `state=active` | 技术性 | 否 |
| GEN-014 | 无迁移文件、无 SQL | 不适用 | 否 |
| GEN-015 | 无产品 UI/键盘焦点。PGM-066 原文「后台/客人端/现场设备/安卓端」。YAML 键名受 GitHub Actions 工作流语法约束（类型 3，必须英文键）。CLI 用户可见文案以中文为主；install L16 一句英文见 008 | 技术性 | 否 |
| GEN-016 | 脚本头/yml 注释与实跑、只读 Actions API、部署数组对照可复现。L6 现文句与 hook/部署行为一致 | 技术性 | 否 |
| GEN-017 | 见本节与 §3.2：先冲突后细化；CI/hook/PR 脚本无业务发明；不因 HTML 无「git hook」字样判添加或删除 | — | 本条为分类过程 |
| GEN-018 | 本报告结论落到 HTML 原文、可执行产物、`man`/`gh --help`/`npm help`/`git help config`、可复现命令。不用历史 raw 当依据 | — | 否 |

技术性「无业务发明」总理由（适用于本模块五文件整体）：提示词 2.1 明文「构建配置、依赖声明、日志与埋点、**CI 脚本**、格式化配置」不算业务发明；五文件不引入票种/状态/角色/价格/审批链/商户等 HTML 实体。

### 3.2 HTML 派生 61 条（均已读锁内摘录 + HTML 原句上下文；摘录行 MISMATCH 0）

**读法**：锚点正文是游乐/地产/看板/主体职责等经营规则。本 FILES 是 git/CI 工程入口。顺从式检查「有没有在这些脚本里另写或写反那些规则」。没有实现 ≠ 遗漏。没有字面词 ≠ 添加。

| ID | HTML 锚点（锁来源行，现文匹配） | 本模块对照 | 分类 | 缺陷 |
|---|---|---|---|---|
| BEQR-085 | 项目开放状态 L140 | 无枚举、无状态机 | 无冲突；无发明；技术性 | 否 |
| BEQR-086 | 场次状态 L142 | 同上 | 同上 | 否 |
| BEQR-087 | 游乐当天/每天 L144 | 无营业日计算 | 同上 | 否 |
| BEQR-088 | 容量双阈值 L145 | 无容量 | 同上 | 否 |
| BEQR-089 | 园区容量 L146 | 无 | 同上 | 否 |
| BEQR-090 | 设备关键/辅助 L147 | 无游乐设备台账 | 同上 | 否 |
| BEQR-091 | 全园故障与补偿通票 L148 | 无票务补偿 | 同上 | 否 |
| BEQR-092 | 安全严重度 L149 | 无 | 同上 | 否 |
| BEQR-093 | 巡检任务状态 L150 | 无游乐巡检 | 同上 | 否 |
| BEQR-094 | 越界类型 L151 | 无空间/合同越界 | 同上 | 否 |
| BEQR-095 | 团体/包场 L153 | 无 | 同上 | 否 |
| BEQR-096 | 游乐增强阶段 L154 | 无 | 同上 | 否 |
| BEQR-118 | 运营营业日 L187 | 无账期/10:00 切日 | 同上 | 否 |
| BEQR-146 | 运营公司职责 L229 | 无娱乐平台/游乐/传媒公司分工 | 同上 | 否 |
| BEQR-155 | 商户/运营主体状态 L238 | 无待准入/黑名单/缓冲期 | 同上 | 否 |
| BEQR-160 | 招商线索状态 L248 | 无 | 同上 | 否 |
| BEQR-161 | 招商意向阶段 L249 | 无 | 同上 | 否 |
| BEQR-162 | 品牌/客户库类型 L250 | 无 | 同上 | 否 |
| BEQR-163 | 合同变更类型 L251 | 无 | 同上 | 否 |
| BEQR-164 | 退租结算状态 L252 | 无 | 同上 | 否 |
| BEQR-165 | 内部应付执行状态 L253 | 无 | 同上 | 否 |
| BEQR-166 | 合同审批链 L255 | 无合同审批；SKIP_CI_WAIT 不是合同审批豁免 | 同上 | 否 |
| BEQR-167 | 租金/账单类型 L256 | 无 | 同上 | 否 |
| BEQR-168 | 营业额抽成模式 L257 | 无 | 同上 | 否 |
| BEQR-169 | 公区能源费 L258 | 无 | 同上 | 否 |
| BEQR-170 | 铺位租赁状态 L259 | 无 | 同上 | 否 |
| BEQR-171 | 物业工单类型 L260 | 无 | 同上 | 否 |
| BEQR-172 | 物业工单状态 L262 | 无 | 同上 | 否 |
| BEQR-173 | 商户服务工单状态 L264 | 无 | 同上 | 否 |
| BEQR-174 | 工单时效 L265 | 无 | 同上 | 否 |
| BEQR-175 | 楼宇机电设备状态 L266 | 无 | 同上 | 否 |
| BEQR-176 | 设备维保类型 L267 | 无 | 同上 | 否 |
| BEQR-177 | 能耗表计类型 L268 | 无 | 同上 | 否 |
| BEQR-178 | 抄表状态 L269 | 无 | 同上 | 否 |
| BEQR-179 | 能耗归集对象 L270 | 无 | 同上 | 否 |
| BEQR-180 | 楼宇巡检任务状态 L271 | 无 | 同上 | 否 |
| BEQR-181 | 楼宇巡检类型 L273 | 无；未把能耗抄表写进巡检 | 同上 | 否 |
| BSM-020 | 楼宇运营 L656 | 无楼宇子系统 | 同上 | 否 |
| BSM-028 | 基础管理·主要功能 L698（含七类红线） | 脚本不提供关闭①–⑦校验的开关，也不发明放行 | 无冲突；技术性 | 否 |
| BSM-081 | 楼宇运营 h2 L1401 | 无机电/能耗/巡检实现 | 同上 | 否 |
| BSM-082 | 主要功能 L1405 | 无 | 同上 | 否 |
| BSM-083 | 和谁有关 L1414 | 无 | 同上 | 否 |
| BSM-111 | 经营看板 h2 L1781 | 无看板指标 | 同上 | 否 |
| BSM-112 | 主要指标 L1785 | 无 | 同上 | 否 |
| BSM-113 | 谁会看 L1813 | 无 | 同上 | 否 |
| BSM-114 | 各类入口 L1827 | 十一端无 git CLI；本模块不是那些入口的实现 | 同上 | 否 |
| BSM-115 | 相互关系 L1897 | 无子系统连接实现 | 同上 | 否 |
| BSM-116 | 系统交付与上线 L1928 | 原文：生产换弱口令、U 盘密钥、安装包+教程。本模块是仓库变更/CI 可见信号，**不实现也不改写**换钥规则。不把 hook/CI 硬套成该章细化上位 | 无冲突；技术性 | 否 |
| BSM-117 | 正式上线时的安全加固 L1932 | 无 U 盘/最高权限口令 | 同上 | 否 |
| BSM-118 | 安装包+教程 L1940 | 无安装包 | 同上 | 否 |
| BSM-119 | 和谁有关 L1947 | 无 | 同上 | 否 |
| PGM-015 | 楼宇运营入口 L399 | 无 | 同上 | 否 |
| PGM-018 | 物理项目与运营公司多对多 L415 | 无主体拓扑 | 同上 | 否 |
| PGM-026 | 商户和主体·主要功能 L492 | 无商户档案/四类主体 | 同上 | 否 |
| PGM-039 | 智能设备场景联动范围 L636 | 无设备编排；未发明计时产品 | 同上 | 否 |
| PGM-052 | 楼宇运营 h2 L806 | 无 | 同上 | 否 |
| PGM-053 | 楼宇机电设备 L810 | 无 | 同上 | 否 |
| PGM-054 | 能耗计量 L821 | 无 | 同上 | 否 |
| PGM-055 | 楼宇巡检 L832 | 无 | 同上 | 否 |
| PGM-056 | 和谁有关 L844 | 无 | 同上 | 否 |
| PGM-066 | 已拍板规则 L951；界面全中文 L965 原文列举后台/客人端/现场设备/安卓端 | 本模块无这些终端的界面。YAML 键名受 GitHub 工作流规范。CLI 提示以中文为主。未发明日结/企微/下单双时间等拍板规则 | 无冲突；技术性 | 否（英文 L16 见 008） |

**添加项：无。** 不把 git/CI 流程登记为业务发明。**冲突项：无。**

---

## 4. 对抗式（假定实现错误）

内用 grilling，事实自读。

1. **直推 main 是否真拦？** stdin + 真实 file-remote push：拦。mainish/main/topic/tag 不误伤。删除 main 拦。`--no-verify` 可绕：文件头已写非安全边界，不升未声明安全缺陷。
2. **本地 main 推到非 main 远端？** 现行按 `remote_ref` 判断，stdin 与 `git push origin main:refs/heads/from-main` 均放行。旧整行匹配缺陷不在现行源（001 重判）。
3. **含空格的非法 local ref？** `read -r` 四字段会错位，可能认不出 remote main。`man githooks` 四字段以 SP 分隔；Git 标准 ref 不含空格。第一次正常 `git push` 不会发出该 stdin。铁律 6：两三件不巧 → 不修、不立项。
4. **propose 是否推未提交内容？** 脏树在 npm 之前 exit 1；干净才 npm+push。003 现行成立。
5. **merge 是否合错 SHA？** 等待后再比 origin 与 `headRefOid`；漂移中止。`--match-head-commit` 写入真实 argv。
6. **CI 红能否合？** watch 非 0 中止且不调 merge。SKIP_CI_WAIT=1 仍 merge，但打印警告——与注释逃生口一致，不升（005）。
7. **gh 挂了当「没 run」？** 否。exit 2 + 原 stderr，1 次查询，无 sleep，不合（004）。
8. **同一 SHA 多个 run？** 源码全 watch；一个失败即拦。本席 11+22 第二失败不合。第一次按正道通常一个 PR run。铁律 6 → 009 仍只登记。
9. **PR 基线不是 main？** 源码不读 `baseRefName`。正道 propose 固定 `--base main`。第一次按脚本跑不撞（010）。
10. **workflow 是否对 feature push 触发？** `on.push.branches:[main]` 不触发 feature。`on.pull_request` 在开 PR 后触发。propose 先 push 再 create。只读列表见 feature SHA 上有 `pull_request` success。
11. **镜像分叉？** 当前两份 yml `cmp` 0。
12. **tilde 安装装到仓内 `~`？** 否。`--path` 展开到 `$HOME/custom-hooks`（002）。
13. **伪成功？** 未发现 exit 0 掩盖失败的主路径。查询失败以非 0 退出。
14. **本机已安装 hook 是否等于源？** 不等于（OBS-1）。不把「未重装」写成源缺陷；本轮禁止主仓安装。

---

## 5. 新 GR 与旧 11 项独立重判

**新 GR：空集。** 无冲突、无添加、无未定稿工程缺陷。

旧 ID 只作输入。下列为**现行源**重判，不继承历史 PASS/队列操作。

### M007-001（历史 CX-01 + 旧 R4；已修出队）

- **位置（现行）**：`ops/160/hooks/pre-push:6–8`
- **冻结判据**：GEN-002
- **2.1 分类**：技术性（无业务发明）
- **2.3 依据类型**：③ `man githooks` 四字段 + ④ stdin / 真实 file-remote push
- **现行证据**：`if [[ "$remote_ref" == "refs/heads/main" ]]`。`local main → remote feature` stdin exit 0；`git push origin main:refs/heads/from-main` exit 0。feature→main 仍拦。
- **工程缺陷是否仍成立**：**否**（历史缺陷在现行源不在）。不新开 GR，不把已修 ID 拉回队列。

### M007-002（历史 CX-02；已修出队）

- **位置（现行）**：`ops/160/install-git-hooks.sh:7–15`
- **判据**：GEN-002/009/013
- **分类**：技术性
- **依据**：③ `git help config` pathname `~/`→`$HOME` + ④ 真实 Git
- **现行证据**：`git config --path --get core.hooksPath`。tilde 产物 `$HOME/custom-hooks/pre-push` 等于源、可执行；仓内无字面 `~` 目录。默认 / 绝对 / 相对 / worktree→common 均过。
- **缺陷仍成立？**：**否**。

### M007-003（历史 CX-03 + 旧 GR-01；已修出队）

- **位置（现行）**：`ops/160/propose-change.sh:12–15`
- **判据**：GEN-002/004/008/009
- **分类**：技术性
- **依据**：④ 真实 Git + stub npm/gh
- **现行证据**：脏树 exit 1「未提交改动」，无 npm、无远端 ref；干净路径 npm 后 file-remote 出现 `feature/p`。
- **缺陷仍成立？**：**否**。

### M007-004（历史 GR-02；已修出队）

- **位置（现行）**：`ops/160/merge-pr.sh:44–54`（已无 `2>/dev/null || true`）
- **判据**：GEN-009/002/011
- **分类**：技术性
- **依据**：④ stub gh exit 2 + 标记 `AUDIT_QUERY_AUTH_FAILURE`
- **现行证据**：stderr 含原错误；`queries` 1 行；无 sleep；无 merge。因 `set -euo pipefail`，进程退出码为 **gh 的 2** 而非脚本 `exit 1`。回归断言 `status != 0` 而非 `== 1`。空列表且 gh 0 仍走 30 次等待（本席第 3 次才给出 id）。
- **缺陷仍成立？**：**否**。退出码 2 vs 1 不是第一次正常跑会误放行的路径，不新开 GR。

### M007-005（历史仅登记）

- **位置**：`merge-pr.sh:35–39`；`pre-push:8–14`；hook 头「非安全边界」
- **判据**：GEN-002/004/005/007/009/011/017/018
- **分类**：技术性
- **依据**：④ SKIP_CI_WAIT 路径打警告、不查 run、仍核 SHA 后 merge；ALLOW_MAIN_PUSH 只认 `1`
- **重判**：原因留痕仍是开发者提示，非业务审批。显式覆盖保持。`--no-verify` 可绕已声明。
- **缺陷成立？**：**否**。保持仅登记，不改开关。

### M007-006（历史仅登记；旧 R8/R9）

- **位置**：`ops/ci/predeploy-gates.yml` 全文；对照 `.github/workflows/predeploy-gates.yml`
- **判据**：GEN-001/002/013/016/017/018
- **分类**：技术性
- **依据**：④ `cmp` 0；yaml parse；GET workflow active；③+④ 本轮 `gh api repos/actions/checkout/git/ref/tags/v4.3.1` object_sha＝`34e114876b0b11c390a56381ad16ebd13914f8d5`；setup-node `v4.4.0`＝`49933ea5288caeca8642d1e84afbd3f7d6820020`
- **重判**：旧「未核 action SHA↔注释」限制本轮已解除且**相等**，仍不编缺陷。双份是机检约束，删一份会红。不声称生产准入/全仓绿。
- **缺陷成立？**：**否**。保持仅登记。

### M007-007（历史仅登记；旧 R1）

- **位置**：FILES 无测试；直接回归 `tools/gate-hardening-tests/m007-regression.mjs:1–102`
- **判据**：GEN-003/018
- **分类**：技术性
- **依据**：④ 回归 exit 0、7 PASS；写入隔离已确认
- **重判**：关键行为已有能失败断言；文件归属仍非本 FILES。不另立测试工程、不扩 FILES。
- **缺陷成立？**：**否**。保持仅登记。

### M007-008（历史仅登记；旧 R3）

- **位置**：`install-git-hooks.sh:16` `installed pre-push hook ->`
- **判据**：GEN-015、PGM-066（L965 原文：后台/客人端/现场设备/安卓端）
- **分类**：技术性
- **依据**：① PGM-066 逐字
- **重判**：一句英文 CLI 不足以断言 Git 工具违背产品终端中文规则。无缺陷 ID 不顺手改中文。
- **缺陷成立？**：**否**。保持仅登记。

### M007-009（历史仅登记；旧 R5）

- **位置**：`merge-pr.sh:55–61` `for rid in $run_ids`
- **判据**：GEN-002/009
- **分类**：技术性
- **依据**：④ 两 id 第二失败 → 不合
- **重判**：AND 语义成立；「旧失败 run 仍出现在 --commit 列表」要组合条件。铁律 6 不新建过滤策略。
- **缺陷成立？**：**否**。保持仅登记。

### M007-010（历史仅登记；旧 R6）

- **位置**：`merge-pr.sh` 全文不读 `baseRefName`；`propose-change.sh:26` `--base main`
- **判据**：GEN-002
- **分类**：技术性
- **依据**：④ propose create 日志含 `--base main --head feature/p`
- **重判**：正道首跑不冲突。不增加 PR 基线门。
- **缺陷成立？**：**否**。保持仅登记。

### M007-011（历史仅登记；旧 R7）

- **位置**：`pre-push` 无 `set -euo pipefail`，异常时落到 L17 `exit 0`
- **判据**：GEN-009
- **分类**：技术性
- **依据**：源码阅读 + 正常 stdin 探针均按预期非 0/0；未证明一次常规 Git 输入会因 echo/`[[` 失败而误放行
- **重判**：理论 I/O 兜底不加。铁律 6。
- **缺陷成立？**：**否**。保持仅登记。

### 观察（非 GR，不进队列）

- **OBS-1 运行时 hook 副本过期**：本工作区 `.git/hooks/pre-push` 存在、mode `0775`、17 行，内容 SHA256 `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b`（整行匹配旧逻辑），**不等于**现行源 `e38ccd33…`。`core.hooksPath` 未设置。源码修复与运行时安装分别据实记录。本轮禁止主仓安装 hook，不把副本漂移写成 FILES 缺陷，也不伪称本机 `git push` 已走现行 `remote_ref` 判断。
- **OBS-2 工作区与 origin 分叉**：HEAD≠origin/main（ahead 49/behind 54），但五 FILES+回归 blob 三者全等。未 fetch。
- **OBS-3 全局红灯**：`verify:predeploy` 链含 `verify:html-ledger-sync` / `verify:entry-anchors` 等；用户已指定 PGM 台账红、入口快照漂移、runtime 过期归后续模块。本席不跑全链刷绿，不把全局红写成 M007 新缺陷。

---

## 6. 分类汇总

| 类 | 处置 | 本模块本票 |
|---|---|---|
| 冲突 | 删除/修复队列 | 无 |
| 细化 | 照常审工程 | 无必须实现的 HTML 下位（BSM-116 原文不是 git/CI） |
| 技术性 | 不删不问；有工程缺陷仍进队列 | 五文件整体；**现行无新工程缺陷** |
| 添加 | B2 问操作员 | 无 |

---

## 7. 验证命令、输出与限制

独立锁哈希（正文用不同标签，避免第二处独占字段）：

```
LOCKFILE-SHA-CMD: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
SIDECAR:         b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
LEDGER-TOP:      b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
```

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（本席独立实算，非派发值）。

**既有回归（先确认写入隔离）**

- 静态：`mkdtempSync(.scratch/m007-regression-*)`；`finally rmSync`；源中无主仓 `.git/hooks` 写入。
- 执行：`node tools/gate-hardening-tests/m007-regression.mjs` → **exit 0**；stdout 7 行 `PASS M007-001…004`；`.scratch` 无 `m007-regression-*` 残留；五 FILES porcelain 仍空。

**只读远端（GET，无 create/merge/PUT/push）**

- `gh auth status`：登录 `jxh1979`，scope 含 `repo`/`workflow`
- `gh api repos/jxh1979/mall/actions/workflows/predeploy-gates.yml`：`state=active`，`path=.github/workflows/predeploy-gates.yml`，`name=predeploy-gates`，`id=307870467`
- `gh run list --workflow predeploy-gates.yml --limit 5`：均为 success；最新 push `35601416957` `headSha=ebb1eb99c610df330c07d24278798c2d3611299a`
- `gh api repos/actions/checkout/git/ref/tags/v4.3.1`：object_sha＝YAML checkout SHA，type=commit
- `gh api repos/actions/setup-node/git/ref/tags/v4.4.0`：object_sha＝YAML setup-node SHA，type=commit

**官方规范（本机 man / --help）**

- `man githooks` pre-push 四元组；删除时 local ref=`(delete)`、local sha=zeros
- `git help config` pathname：`~/` 展开 `$HOME`
- `gh pr merge --help`：`--match-head-commit SHA`、`--squash`/`--merge`/`--rebase`
- `gh run watch --help`：`--exit-status`、`--compact`
- `gh run list --help`：`--commit`、`--workflow`
- `npm help ci`：`ignore-scripts` 仍允许随后 `npm run`

**隔离实跑（事后非交付物）**

- `/tmp/m007-b1-adv-20260922T115357Z`：pre-push stdin 14 项中 13 PASS；「local ref 含空格」FAIL 为非法 ref 导致字段错位，**harness 误用**，不记产品缺陷。真实 file-remote：拦 main、放行 feature、放行 local main→remote 非 main。install 四 dest + worktree-common PASS。
- `/tmp/m007-b1-adv2-20260922T115538Z`：propose 14 PASS（须 `cd` fixture；首次误在主仓 cwd 被 main 守卫拦住，**未**触达真实 origin push）。merge 49 PASS / 1 FAIL：查询失败 got exit **2**（期望 1）——产品 fail-closed，见 004。漂移/watch/AND/rebase argv 均 PASS。
- `bash -n` 四脚本 0。两份 yml 字节相等。`REQUIRED_GATES` 两部署脚本各 15 名 = `verify:predeploy` 15 名。

**明确没做**

- 业务仓 `git push`/`commit`/`merge`、`gh pr create`/`merge`、覆盖 `~/complex-management/.git/hooks`
- 全仓 `npm run verify:predeploy`、生产构建、部署、DB
- 读取本次 selfB1 / 任何 `*-self-raw.md`

范围外：部署脚本只读 `REQUIRED_GATES` 数组以核 L6；workflow 生效件对照属 M005 文件，本票只证镜像相等；机检在 M008。故障不顺手修。

---

## 8. 独立性

- 未读 `M007-b1-self-raw.md` 及任何本次 selfB1。未用同号旧 backend M007、未用历史审计/收口结论代替本轮命令与原文。旧 001–011 只作输入线索。
- 调用者只读：`package.json` 的 `verify:predeploy` 字符串、`.github/workflows/predeploy-gates.yml`（对照镜像）、`tools/verify-delivery-gates.mjs` 290–308 行断言、`ops/160/complex-deploy-*.sh` 的 `REQUIRED_GATES`、回归 102 行。未把那些模块审一遍。
- 三份 HTML 只读原句。铁律 4 未触发。
- 可复用本席旧票对 **未改字节** 的 yml 之全文阅读记录，但本轮仍全文重读并重判。

---

## 9. 步骤结论

覆盖：5/5 FILES（163 行）+ 直接回归 102 行，79/79 判据，三线均完整模块。新 GR 空集。旧 11 项均已独立重判：001–004 现行源不再呈现历史缺陷；005–011 仍仅登记。无添加项、无 HTML 冲突、无未答 questions。OBS-1 运行时 hook 副本过期已披露，不升源缺陷。

**PASS**

B1 禁止修复。本票不是模块 PASS、不是 B2 定稿。须 Codex 本批 B1 落盘后才进入合取；修复队列是否为空由 B2 双席定稿。步骤仍 B1，M007 `IN_PROGRESS`，B3 累计 1 不清零。

## M007 现锁B2整批归并票（Grok 席追加）
版本：V0.04（本文件既有 V0.01 B1 + V0.02 B2 + V0.03 B1 全部 **89977** 字节原样保留，SHA256 `66466c551f9609a0e0cca9692ab46b45abe13c214f1317b5a5c665f0e1628779`，不覆盖、不改 V0.03 原文；自身版本 V0.03→V0.04）

本席B2：**PASS**。此票只判断 findings V3.43 顶部「当前M007现锁B2整批候选」相对两份指定完整 B1 raw 的全批差集、四分类、当前缺陷、修复队列与最小处置。不是独立 B1 PASS，不是模块 PASS，不是发布 PASS。不继承本文件旧 B2（V0.02，当时 001–004 入队）、不继承旧 B4/B5、不继承本席 V0.03 B1 PASS、不继承 self V0.03 B1 PASS。self-raw 在 70964 字节之后的 V0.04 Codex B2 追加段**不在本批输入、不采其结论**。根候选未把本席 V0.03 空 GR 改写成新缺陷。本段不写独占锁哈希字段行。

现锁由本席本节点独立实算（`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`），**不是**抄 sidecar／ledger／派发期望值后再回填：`b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`。对照：sidecar `plan/criteria-lock.sha256` 同值；ledger 顶部第 1 行同值；self V0.03 独占锁行同值；本文件 V0.03 之前最后独占锁行同值。四值全等。`LOCK-AMEND-COUNT: 1`。锁版本 V0.14；M007 映射 79。

### 本批输入（精确路径／版本／体积／SHA）

| 输入 | 路径 | 版本 | 体积 | SHA256 |
|---|---|---|---|---|
| 席甲最后完整 B1 | `docs/audit-2026-09/modules/M007-b1-self-raw.md` | V0.03（自最后独占锁行（值 `b0863b95…`）起至「模块仍IN_PROGRESS。无源码/HTML修改，无真实推送合并部署。」止；其后 V0.04 Codex B2 不在本批输入） | 封存前缀 **70964** 字节（当前磁盘整文件 75967，尾 5003 字节为 V0.04，本席只用封存前缀） | `40d4759cef73fe7d6a1afa14d4f597ae1081b31ecb94dd6f7a22e344b0a2d2f8` |
| 本席最后完整 B1 | `docs/audit-2026-09/modules/M007-b1-grok-raw.md` 本文件 | V0.03（自上一独占锁行起至 V0.03「步骤仍 B1，M007 `IN_PROGRESS`，B3 累计 1 不清零。」） | **89977** 字节（追加前整文件） | `66466c551f9609a0e0cca9692ab46b45abe13c214f1317b5a5c665f0e1628779` |
| 候选台账 | `docs/audit-2026-09/findings.md` 顶部「当前M007现锁B2整批候选」至「下方旧当前段为历史。」 | V3.43 | **1238199** 字节（整文件；用户/候选引用 SHA 即此整文件） | `648b5378c44a8b68631a91bb2799e81ffbe4eccb6e6f9347acd0055744c0b84e` |

候选段本身（L4 起至下一 `##` 前，3735 字节）另算 SHA256 `a65f245f7dd4555a8592ef9eca2965e0fb7b66e97201acf6e79bc9ff6ec2af86`，仅作切片披露；身份核验以整文件 `648b5378…` 为准。两份 B1 与候选段全文已读（self 只用 70964 前缀）。

当前范围只认五源 163 行、映射 79：`ops/160/install-git-hooks.sh` 16／707／`8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b`；`ops/160/merge-pr.sh` 73／4180／`3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06`；`ops/160/propose-change.sh` 30／1626／`88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b`；`ops/160/hooks/pre-push` 17／922／`e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85`；`ops/ci/predeploy-gates.yml` 27／1335／`4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`。`git ls-files -z -- <五路径> | xargs -0 cat | wc -l` = **163**（计划历史 156，冻结计量不改）。blob 工作区＝HEAD＝origin/main。五文件 + 回归 porcelain 空。**不是**旧同号权限后端模块。必要回归 `tools/gate-hardening-tests/m007-regression.mjs` 102／5239／`10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8` 只作行为证据，不扩 FILES。`questions/` 无 `M007*`，无本批未答添加项。

未派子代理、未用 MCP、未 fetch／commit／push／安装主仓 hook／部署／真实 PUT／DB，未改 findings／ledger／源码／HTML／锁／收口／五受审文件／回归文件。固定 CLI 形态未改。

### 全部 ID 差集（不继承旧 B2 队列）

席甲 V0.03 当前临时项 **CX01—CX06**（正文「当前完整finding登记」：CX01 hook→001/005/011；CX02 install→002/008；CX03 propose→003；CX04 merge→004/005/009/010；CX05 workflow→006；CX06 直接回归→007）。本席 V0.03 当前新 GR **空集**（§5 明文；本批 V0.03 正文无新 `GR-` 临时号。V0.01 前缀的历史 GR-01/GR-02 只作旧 003/004 线索，不是本批新 GR）。无添加项、无未答 questions、无新 HTML 自身问题、无 LOCK-AMEND 主张。

| 正式ID | 本批全部来源 | 四分类 | 当前工程缺陷 | 修复队列 | 本席独立核后的最小处置 |
|---|---|---|---|---|---|
| M007-001 | self CX01；Grok 旧001重判 | 技术性 | **否**（历史已修） | **否** | 不重修。现行 `pre-push:6–8` 只按第三字段 `remote_ref == refs/heads/main`。stdin：local main→feature exit 0；feature→main exit 1；`ALLOW_MAIN_PUSH=1` exit 0；`ALLOW_MAIN_PUSH=true` 仍拒。隔离真实 Git：新 commit 后 `git push origin main` exit 1；`main:refs/heads/from-main` exit 0。GEN-002/004/008/009/016/018。保持 |
| M007-002 | self CX02；Grok 旧002重判 | 技术性 | **否**（历史已修） | **否** | 不重修。现行 `install-git-hooks.sh:7–15` 用 `git config --path --get core.hooksPath`。隔离 HOME：字面 `~/custom-hooks`，`--path` 展开到 `$HOME/custom-hooks`，产物等于源且可执行，仓内无字面 `~` 目录。GEN-002/009/013/016/018。保持 |
| M007-003 | self CX03；Grok 旧003重判 | 技术性 | **否**（历史已修） | **否** | 不重修。现行 `propose-change.sh:12–15` dirty 早于 npm。隔离 stub：exit 1「未提交改动」，无 `npm_called`、无 `pushed`。GEN-002/004/008/009/011/013/018。保持 |
| M007-004 | self CX04；Grok 旧004重判 | 技术性 | **否**（历史已修） | **否** | 不重修。现行 `merge-pr.sh:44–54` 已无 `2>/dev/null \|\| true` 吞查询。隔离：gh `run list` exit **2** + stderr `AUDIT_QUERY_AUTH_FAILURE`，`queries` 1 行，无 sleep、无 merge。因 `set -euo pipefail` 进程退出码为 gh 的 **2**（不是脚本另写的 1）。回归断言 `status != 0`。不新开 GR。GEN-009/002/011/018。保持 |
| M007-005 | self CX01/CX04；Grok 旧005 | 技术性 | **否** | **否** | 只登记。SKIP_CI_WAIT／ALLOW_MAIN_PUSH 显式覆盖与 SHA／match-head 保护保持；原因留痕是开发者提示，非业务审批。不改开关。GEN-002/004/005/007/009/011/017/018。保持 |
| M007-006 | self CX05；Grok 旧006 | 技术性 | **否** | **否** | 只登记。两份 yml `cmp` 0；本轮 GET tag `v4.3.1` object_sha＝`34e114876b0b11c390a56381ad16ebd13914f8d5`，`v4.4.0`＝`49933ea5288caeca8642d1e84afbd3f7d6820020`，与 YAML 钉死 SHA 相等。workflow `state=active`。不删镜像，不声称生产准入／全仓绿。GEN-001/002/013/016/017/018。保持 |
| M007-007 | self CX06；Grok 旧007 | 技术性 | **否** | **否** | 只登记。FILES 仍无测试；直接回归 102 行本节点 `node tools/gate-hardening-tests/m007-regression.mjs` **exit 0**、7 PASS、无 `.scratch/m007-regression-*` 残留。不另立测试工程、不扩 FILES。GEN-003/018。保持 |
| M007-008 | self CX02；Grok 旧008 | 技术性 | **否** | **否** | 只登记。`install-git-hooks.sh:16` 一句英文维护 CLI。PGM-066 L965 原文列举后台／客人端／现场设备／安卓端，不是 Git 工具。无缺陷 ID 不顺手改中文。GEN-015/017、PGM-066。保持 |
| M007-009 | self CX04；Grok 旧009 | 技术性 | **否** | **否** | 只登记。`merge-pr.sh:55–61` 同 SHA 多个 run 全 AND。第一次按正道通常一个 PR run。铁律 6 不新建过滤策略。GEN-002/009。保持 |
| M007-010 | self CX04；Grok 旧010 | 技术性 | **否** | **否** | 只登记。merge 全文不读 `baseRefName`；正道 `propose-change.sh:26` 固定 `--base main`。不增加 PR 基线门。GEN-002。保持 |
| M007-011 | self CX01；Grok 旧011 | 技术性 | **否** | **否** | 只登记。hook 无 `set -euo`；正常 Git 四字段 stdin 行为成立。含空格非法 local ref 见下方 harness，不是第一次正常 `git push`。铁律 6 不加理论 I/O 兜底。GEN-009。保持 |

漏并：**无。** CX01—06 全部落入 001—011。本席 V0.03 无新 GR 可丢。001—011 均在候选声明中，未复用号、未改写成业务权限源码。

分类顺序冲突→细化→技术性→添加：当前十一项均冲突否、添加否。五源是仓库 Git／CI／安装脚本，提示词 2.1 明文「CI 脚本」不算业务发明；不引入票种、价格、角色权限或新合作模式。BSM-116 原文是生产换弱口令、U 盘密钥、安装包+教程，**不**把该章硬套为 Git 规则细化上位。self V0.03 对 BSM-116–119 写「上线工程支撑适用」只是关联范围，候选与本票最终十一项全部技术性，与本席 V0.03 分类一致。

队列「是」= **空集**。与两席本轮 B1 及候选十一行一致。旧 B2 曾把 001–004 标「是」——那是修前状态，**本票不继承**。历史 B3 累计 **1** 保留，不清零。

### 观察（非 finding，不进队列）

- **OBS1 运行时 hook 副本过期**：本工作区 `.git/hooks/pre-push` 存在、mode `0775`、内容 SHA256 `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b`，正文仍是「直接整行匹配」旧逻辑，**不等于**现行源 `e38ccd33…`（`remote_ref`）。`core.hooksPath` 未设。它属于 Git 运行配置，不在冻结 FILES。本模块验的是源字节和隔离安装后路径。**本批不安装主仓钩子，不宣称运行副本已更新，也不伪称本机 `git push` 已走现行 `remote_ref` 判断。** 真实运行同步是另行维护，本票不假称完成。
- **OBS2 工作区与 origin 分叉**：`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=ebb1eb99c610df330c07d24278798c2d3611299a`，`rev-list --left-right --count` = **49／54**。五 FILES+回归 blob 三者全等。未 fetch。
- **OBS3 全局红灯**：`verify:predeploy` 链含 html-ledger-sync／entry-anchors 等既有红。归属后续模块照旧。本席不跑全链刷绿，不把全局红写成 M007 新缺陷。

### harness 两个 FAIL（原 raw 保留；不藏失败、不写成产品 FAIL）

本席本节点隔离复验，结果与 V0.03 B1 同向：

1. **非法空格 local ref**：stdin `refs/heads/feat ure <40a> refs/heads/main <40b>` → 源 hook **exit 0**。`read -r` 四字段错位，`remote_ref` 落到 SHA 而非 `refs/heads/main`。`man githooks` 四字段以 SP 分隔；Git 标准 ref 不含空格。第一次正常 `git push` 不会发出该 stdin。这是 **harness 误用**，不是产品缺陷，也不因此把 011 改「是」。
2. **gh 查询退出码 2 对预设 1**：隔离 stub `gh run list` exit 2 时，原 `merge-pr.sh` **MERGE_EXIT=2**，stderr 含 `AUDIT_QUERY_AUTH_FAILURE`，查询 1 次，无 sleep、无 merge。产品 fail-closed。既有回归断言 `status != 0` 而非 `== 1`。不写成产品 FAIL，也**不伪称所有 harness 断言通过**。不改任何测试源。

另：隔离 fixture 在 hook 安装前已把 main 推到 file 远端，随后无新 commit 的 `git push origin main` 得到 `Everything up-to-date`／exit 0——Git 对空更新可不走 pre-push stdin。本席另作**新 commit 后再推**，源 hook exit 1 拒 main。不把「up-to-date」伪写成「现行 hook 已放行直推 main」。

`merge-pr.sh:58` 仅有的 `gh run view … \|\| true` 发生在 watch 已失败之后，只丢 URL，不影响中止，也不吞 L44 查询错误。

### 与候选的合取／根未覆盖本席意见

候选十一行分类／当前缺陷／队列与本席 V0.03 §5 表及本票独立复算一致；CX01—06→001—011 映射齐全；新 GR 空集未被改写成缺陷；OBS1–3 与 harness 两 FAIL 的披露口径与本席 V0.03 一致，未被改写成「主仓已装现行源」或「全部探针通过」。无 ID 遗漏，故不因此 FAIL。

证据层次差（不改三值、不另造 ID）：席甲 V0.03 另跑了 m001 merge 段与内存扩展 propose 三态；本席 B2 复跑 m007 回归 7 项 + 本节点隔离 hook／install／propose dirty／merge exit2／真实 file-remote。两套都支持现行 001–004 已修。席甲「工程支撑」措辞不把 BSM-116 变成细化上位。

### 本席 grilling（自答；禁止表演性同意）

1. 能否因两席 B1 都 PASS、或因 self V0.04 已写 PASS，就预设本 B2 PASS？**不能。** V0.04 不是输入。差集、三值、OBS、harness 均在本票独立核实后才下。
2. 空新 GR 是否漏并？**否。** V0.03 §5 明文空集；本批无新 `GR-` 号。
3. CX01 兼映射 001／005／011 是否丢 005／011？**否。** 候选 005 写 CX01/CX04，011 写 CX01，三行齐全。
4. 001–004 应否因历史缺陷再入队？**否。** 现行源与本节点命令均显示历史缺陷不在。重修会变成无缺陷改脚本。
5. OBS1 应否立源码缺陷并在本 B2 安装主仓 hook？**否。** 运行副本不在 FILES；本批禁止主仓安装。不能声称现源已装。
6. 含空格 ref／exit 2 vs 1 应否新开 GR？**否。** 前者非法输入；后者仍非 0 且无 merge。不藏失败，不升产品缺陷。
7. 英文 L16 应否顺手改中文？**否。** 无缺陷 ID。
8. 空队列能否直接模块 PASS？**不能。** 按 V0.17 只允许跳新增 B3／B4 进入 **B5-r1**。B3 累计仍 **1**。
9. 旧 B5-r1 历史 PASS 能否继承为本轮模块 PASS？**不能。** 现锁重开后须独立 B5。ledger 表行旧 PASS 只是历史。
10. BSM-116 生产上线章能否当 Git 脚本细化上位？**不能。** 原文不是 git hook／CI。

### 实跑证据（类型 4；本节点；只读 + 隔离 fixture）

```text
self_v003_prefix=70964/40d4759cef73fe7d6a1afa14d4f597ae1081b31ecb94dd6f7a22e344b0a2d2f8
self_disk=75967/d5478ebd77ed33c1a6ee02aa7b03bc3868c5ac88821f3834dc73f03ad183521d (V0.04 Codex B2 excluded)
grok_v003_before_append=89977/66466c551f9609a0e0cca9692ab46b45abe13c214f1317b5a5c665f0e1628779
findings_v343=1238199/648b5378c44a8b68631a91bb2799e81ffbe4eccb6e6f9347acd0055744c0b84e
candidate_section_L4_to_next_h2=3735/a65f245f7dd4555a8592ef9eca2965e0fb7b66e97201acf6e79bc9ff6ec2af86
lock_workspace=b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
sidecar_eq=true ledger_top_eq=true grok_last_lock_eq=true
LOCK-AMEND-COUNT=1
HEAD=e456ba96c84185c8d39f18f548011daac48a6466
origin/main=ebb1eb99c610df330c07d24278798c2d3611299a
ahead_behind=49/54
official_lines=163 mapped=79
html_enum=4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
html_bsm=eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66
html_pgm=5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f
five_source_porcelain=empty regression_porcelain=empty
bash_-n=install/merge/propose/pre-push exit0
yml_cmp=0 yaml_parse=on.pr/push.main checkout@34e11487 setup-node@49933ea5 node=22
REQUIRED_GATES_api=15 eq_web=true eq_predeploy=true
m007-regression=EXIT0 PASS M007-001×4 PASS M007-002/003/004 scratch_residue=none
OBS1_runtime_sha=46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b
OBS1_source_sha=e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85
OBS1_equal=false core.hooksPath=unset
hook_stdin: main->feature=0 feature->main=1 allow1=0 allow_true=1 mainish=0 empty=0 space_local_ref=0
file_remote: feature_push=0 main_to_from-main=0 main_new_commit_push=1
install_tilde: literal=~/custom-hooks expanded=$HOME/custom-hooks dest_eq_source=true mode=0755 repo_tilde_dir=false
propose_dirty: exit=1 npm_called=false pushed=false
merge_gh_exit2: MERGE_EXIT=2 queries=1 merged=false slept=false stderr=AUDIT_QUERY_AUTH_FAILURE
gh_workflow=id307870467 state=active path=.github/workflows/predeploy-gates.yml
gh_tag_checkout_v4.3.1=34e114876b0b11c390a56381ad16ebd13914f8d5 type=commit
gh_tag_setup_node_v4.4.0=49933ea5288caeca8642d1e84afbd3f7d6820020 type=commit
gh_run_latest=35601416957 success push headSha=ebb1eb99… (=origin/main)
cx_ids=CX01..CX06
grok_new_GR=empty
candidate_ids=M007-001..011
queue_yes=empty
adding=0 unanswered_questions_this_batch=0
B3_cumulative=1
harness_fail_kept=space_local_ref_exit0 merge_exit2_vs_preset1
main_repo_hook_installed_this_batch=false
```

隔离目录 `/tmp/m007-b2-grok-20260922T121030Z` 为事后非交付物。未真实 `npm ci`、未跑 15 门禁全链、未部署、未 merge、未 PUT、未连库、未改 `.git/hooks`。

### 最终结论

**本席 B2：PASS。**

实际差集：席甲 V0.03 当前临时项 CX01—06 → 001—011；本席 V0.03 新 GR 空集。候选覆盖上述全部 ID，无漏并、无改写本席空集为缺陷、无拆分或复用。001／002／003／004 技术性历史已修、当前否、队列否。005—011 技术性、当前否、队列否、仅登记。无添加、无新 HTML 申请、无本批未答 questions。

OBS1 主仓运行 hook 仍为旧整行匹配副本，不升源缺陷，不安装、不伪称现源已装。OBS2／OBS3 照旧。harness 两个 FAIL 原样保留。

修复队列「是」= 空集。按 V0.17：双席 B2 定稿后跳过新增 B3／B4（累计 B3 仍 **1**），必须再做独立 **B5-r1** 全模块重审。现在不得标模块 PASS。两席 B1 PASS 不代替 B2／B5。空队列只允许下一步 B5-r1，不能直接模块 PASS。

不修改候选。不修 HTML／锁／源码／配置。双席合取仍须根席本批 B2；本席不得被自审覆盖。本段未写独占锁哈希字段行。其后本节点不进入 B3／B4／B5／其他模块。

B2 结束。不修复。
