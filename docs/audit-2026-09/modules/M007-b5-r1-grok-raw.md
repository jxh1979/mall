CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M007 B5-r1 Grok 乙席完整审计报告

- 文件版本：V0.01（本路径原先不存在，本次新建）
- 席位：Grok 独立审计席乙
- 步骤：B5-r1（B3 累计 1 之后的修复后整模块全量审计；不可用 B4-r1 PASS 替代）
- 计划：当前 471 模块计划；`docs/audit-2026-09/plan/modules.md` V0.04；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`
- 判据锁：`criteria-lock.md` V0.11；本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；旁路 `plan/criteria-lock.sha256` 同值
- 本席约束：只读业务代码与计划；不调用 MCP；不派子代理；不修源/HTML/台账/主索引；不执行真实 Git hooks 安装、push、开 PR、合并、部署
- 技能：已读并按提示词 §6 收窄口径使用 `ponytail`、`verification-before-completion`、`grilling`。未调用 `grill-me`/`batch-grill-me` 可执行体（本环境无独立 CLI），把追问结构自行落到反例命令。不冒充未核技能。不调用第三席
- 既有台账只作上下文：findings V1.09 记 001–004 已修出队、005–011 仅登记。本席不把那些票当成本席事实，下列行为均重新实读与实跑

## 1. 范围核对（来自 modules.md 与 criteria-lock.md，不是印象）

`plan/modules.md` M007 FILES-BEGIN 冻结原清单 5 文件、有效内容行数 156（修前口径）：

1. `ops/160/install-git-hooks.sh`
2. `ops/160/merge-pr.sh`
3. `ops/160/propose-change.sh`
4. `ops/160/hooks/pre-push`
5. `ops/ci/predeploy-gates.yml`

本轮实际另须完整审直接新增 `tools/gate-hardening-tests/m007-regression.mjs`（102 行 V0.01）。当前 5 文件 163 行（16/73/30/17/27）+ 回归 102 行。全部逐行实读，不抽样、不只读 diff、不复述上轮。

`criteria-lock.md` 映射（79 个，去重后仍 79）：

BEQR-085..096, BEQR-118, BEQR-146, BEQR-155, BEQR-160..181,
BSM-020, BSM-028, BSM-081..083, BSM-111..119,
GEN-001..018,
PGM-015, PGM-018, PGM-026, PGM-039, PGM-052..056, PGM-066。

三份 HTML 对应锚点已按锁内行号回读，关键词均落在引用行（mismatch_count=0）。37 条 BEQR 的 `<tr><td class="k">…</td></tr>` 与 `business-enum-quick-reference-v0.1.html` 原文一致。

## 2. 源指纹核对（独立 sha256sum，与指令给定值对照）

| 文件 | 版本头 | 行数 | 本席 SHA256 |
|---|---|---|---|
| ops/160/install-git-hooks.sh | V0.01 | 16 | 8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b |
| ops/160/merge-pr.sh | V0.02 | 73 | 3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06 |
| ops/160/propose-change.sh | V0.01 | 30 | 88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b |
| ops/160/hooks/pre-push | V0.01 | 17 | e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85 |
| ops/ci/predeploy-gates.yml | V0.02 | 27 | 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d |
| tools/gate-hardening-tests/m007-regression.mjs | V0.01 | 102 | 10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8 |

与指令给定指纹全部一致。环境：git 2.43.0，bash 5.2.21，node v24.20.0。主仓 `core.hooksPath` 为空；`git-common-dir=.git`。

## 3. 每文件三线

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

### 3.1 `ops/160/install-git-hooks.sh`（16 行）

**第一性原理**

1. 为什么存在：Git 只执行 `core.hooksPath` 或 `$GIT_DIR/hooks` 里的钩子。源码树 `ops/160/hooks/pre-push` 自己不会生效。删掉本脚本，开发者无法把禁止直推 main 的钩子装到 Git 实际读取的目录；与产品 HTML 无直接锚点，属于仓库变更闸门的技术入口（2.1 技术性，不是业务发明）。
2. 声称是否真执行：脚本 `cd` 到仓根，`git config --path --get core.hooksPath`，空则 `git rev-parse --git-common-dir`/hooks，`install -m 0755` 复制钩子。隔离 Git/HOME 实跑（类型 2/4）：默认 → `.git/hooks/pre-push` 且内容等于源钩子、可执行；`~/custom-hooks` → `$HOME/custom-hooks/pre-push`（回归 M007-002）；绝对路径 → 该绝对目录；相对 `rel-hooks` → 工作树 `rel-hooks/pre-push`，与 `git rev-parse --path-format=absolute --git-path hooks` 同一目录；从 worktree 执行 → 装到 common `.git/hooks`。本席**没有**对主仓执行该脚本。
3. 未经验证前提：GitHub/本机是否已对每位开发者跑过安装——主仓运行时钩子仍是旧副本（见 §8），不能把源码修复说成运行时已发布。`git config --path` 对相对路径在本 Git 2.43 返回字面 `rel-hooks`，但安装 cwd 是仓根，与 Git 实际 hooks 目录一致，不是 002 类错装。

**顺从式**：映射 79 条见 §5。本文件无业务枚举/状态机。GEN-002/009/013 由上述安装路径覆盖。英文成功句 `installed pre-push hook ->`（L16）见 PGM-066/GEN-015 不适用说明，不新开票。

**对抗式最小反例**

- 自定义 `~/`：回归 PASS，产物在 Git 使用的 HOME 路径。
- 相对/绝对/默认/worktree：本席隔离复跑，安装点 = `git-path hooks`。
- 未对主仓安装：有意。主仓 `.git/hooks/pre-push` 时间戳 2026-07-05，SHA `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b`，仍用整行 `*" refs/heads/main "*`。

分类：技术性；无业务发明；无 HTML 冲突。

### 3.2 `ops/160/merge-pr.sh`（73 行）

**第一性原理**

1. 为什么存在：注释与 AGENTS 铁律写明私有库免费版设不了 required check，`gh pr merge` 不看红灯。本脚本把「等 predeploy-gates 对本 commit 变绿」变成机器动作。删掉则回到手敲合并。HTML 无 GitHub PR 流程锚点 → 技术性。
2. 声称是否真执行（隔离 PATH：假 git/gh/sleep，禁止真网）：
   - 非法方法 `--ff` → exit 1，「只能是 --squash|--merge|--rebase」
   - 在 main → exit 1
   - 脏工作区 → exit 1，未 merge
   - origin 无该分支（ls-remote 空）→ exit 1，「先跑 propose-change.sh」
   - HEAD ≠ origin → exit 1
   - 无 PR → exit 1
   - `gh run list` exit 2 且 stderr `AUDIT_QUERY_AUTH_FAILURE` → 立即非 0，queries=1，无 merge（回归 M007-004）
   - list 空且 exit 0 → 30 次重试（sleep stub），然后「仍没有」exit 1，无 merge
   - 有 run 且 watch 失败 → 「未通过」exit 1，无 merge
   - watch 绿但 PR head 漂移 → 「被更新」exit 1，无 merge
   - 正常：watch 绿且 SHA 一致 → `gh pr merge <branch> --squash --match-head-commit <40位sha>`
   - `SKIP_CI_WAIT=1`：打印醒目警告，仍做 SHA 钉住后 merge；漂移则拒绝
3. 未经验证前提：GitHub Actions 是否对该仓启用、真实 `gh run list --workflow predeploy-gates.yml` 在线上的检索语义、action pin 是否等于注释标签（§8）。`seq`/`gh`/`git` 依赖本机 PATH，隔离测试提供了 stub。

**顺从式**：GEN-002/004/007/009/011/013 有命令证据。SKIP_CI_WAIT / 多 run 全 AND / 不读 baseRefName 见 §6 已核项，不新策略。无业务状态写入，GEN-006 以「钉住所验 SHA」覆盖合并对象完整性，不是金额事务。

**对抗式**：见上列拒绝/错误/覆盖路径。构造「查询失败当空列表」的反例时，当前源码因 `set -euo pipefail` 且无 `|| true`/`2>/dev/null` 而立刻失败并保留 stderr，与 004 修复声称一致。

分类：技术性。

### 3.3 `ops/160/propose-change.sh`（30 行）

**第一性原理**

1. 为什么存在：把「本地门禁 → 推 feature → 开/更新 PR」收成一条，并写明下一步必须 `merge-pr.sh`。删掉则开发者更容易未跑 `verify:predeploy` 就推，或手敲 `gh pr merge`。
2. 声称是否真执行：
   - 无参数：`${1:?需要 PR 标题}` → exit 1（类型 4）
   - 在 main → exit 1
   - 脏工作区 → exit 1，无 push（回归 M007-003）
   - npm `verify:predeploy` 非 0 → exit 1，无 push
   - 干净 + npm 0 + 无 PR → push 然后 `gh pr create --base main`
   - 已有 PR → push，不 create
3. 未经验证前提：未跑真实 `npm run verify:predeploy`（全仓会扩到其它模块）。已核 package.json 该键存在且 15 个子脚本均有定义；本脚本 L18 调用的就是这个名字。

**顺从式**：`--base main` 写死，对应 010「正道首跑不缺基线」的独立复验，不加新闸。用户可见 echo 为中文。

**对抗式**：脏树/npm 红/main/缺标题均在推送前失败。未发明「gh pr view 认证失败」组合理论票。

分类：技术性。

### 3.4 `ops/160/hooks/pre-push`（17 行）

**第一性原理**

1. 为什么存在：私有库无分支保护时，阻止 `git push` 直达 `refs/heads/main`。注释自承「开发者提示，非安全边界」。删掉则只靠自觉。
2. 声称是否真执行（对**源文件**喂 Git 四字段 stdin）：
   - local main → remote feature，ALLOW=0 → 0（回归；001 修复点）
   - feature → main，ALLOW=0 → 1，中文拒绝
   - feature → main，ALLOW=1 → 0，覆盖提醒
   - feature → mainish → 0
   - 空 stdin → 0
   - 删除 main（40 个 0）→ 1
   - 两行中第二行目标 main → 1
   - `refs/heads/Main` / `main-prod` → 0（精确匹配，不是子串）
3. 未经验证前提：主仓正在用的 hooks 仍是旧整行匹配实现（§8）。Git 官方 pre-push stdin 格式（类型 3，Git 钩子约定）与源码 `read -r local_ref local_sha remote_ref remote_sha` 一致。无 `set -euo`：本席用常规四字段输入未复现误放行，不把 echo 失败等理论 I/O 写成新缺陷。

**顺从式**：只按第三字段判断目标。与 merge/propose 的 main 禁令一致（GEN-008）。

**对抗式**：见上。旧运行时副本仍会误拦 main→feature，那是未重装，不是源码未修。

分类：技术性。

### 3.5 `ops/ci/predeploy-gates.yml`（27 行）

**第一性原理**

1. 为什么存在：GitHub 侧可见的静态治理门禁；与 `.160` 部署机隔离。`tools/verify-delivery-gates.mjs:284–289`（只作链接证据，不审 M008）要求本文件与 `.github/workflows/predeploy-gates.yml` 在 CRLF 归一后字节相同，只改一侧会在下次门禁红。
2. 声称是否真执行：`cmp` 与 sha256 双重相等（两文件皆 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`，1335 字节，无 CRLF）。结构：`name: predeploy-gates`；`on.pull_request/push.branches: [main]`；`fetch-depth: 0`；`npm ci --ignore-scripts`；`run: npm run verify:predeploy`；`HTML_GATES_BASE` 表达式与 verify-delivery-gates 期望字符串一致。package.json `verify:predeploy` 拆 15 段，名称全部有 scripts 定义。
3. 未经验证前提：未向 GitHub 查询 workflow 是否 enabled；未核对 action commit 是否等于注释 `v4.3.1`/`v4.4.0`（§8）。不因此编造缺陷。

**顺从式**：无业务规则。GEN-001：镜像不是死重复，有机检强制。GEN-013：pin 完整 SHA、ignore-scripts、node 22。

**对抗式**：若内容漂移，verify-delivery-gates 会推 `CI-LIVE-MATCH` 失败——这是现有闸，本模块不新加闸。本席确认当前未漂移。

分类：技术性。

### 3.6 `tools/gate-hardening-tests/m007-regression.mjs`（102 行，B3 直接新增）

**第一性原理**

1. 为什么存在：B3 对可执行行为修复要求先有能失败的最小回归，绑定 001–004。不在 modules.md 原 FILES 内，但是本轮明确纳入。
2. 声称是否真执行：`node --check` exit 0；`node tools/gate-hardening-tests/m007-regression.mjs` exit 0，七条 PASS（001 四组、002、003、004）。测试只 stub GitHub/推送边界，真实执行源脚本。隔离目录在 `.scratch/m007-regression-*`，finally 删除。
3. 未经验证前提：该文件未写入 package.json 任何 script（`m007 in package.json scripts False`）。按 ponytail/不新增闸，不把「未接入 verify:predeploy」写成缺陷。本审计已直接执行。

**顺从式 / 对抗式 / GEN-003**：断言是 `assert.equal/notEqual/ok/deepEqual`，能失败。本席把「目标 main 应放行」的反断言跑成 `1 !== 0`，exit 1，证明测试不是恒真。覆盖 001–004 关键修复分支；其余分支由本席隔离命令补证据，不要求再添测试工程。

分类：技术性。

## 4. 语法与链接命令（类型 4）

```
bash -n ops/160/install-git-hooks.sh  → 0
bash -n ops/160/merge-pr.sh           → 0
bash -n ops/160/propose-change.sh     → 0
bash -n ops/160/hooks/pre-push        → 0
node --check tools/gate-hardening-tests/m007-regression.mjs → 0
node tools/gate-hardening-tests/m007-regression.mjs → 0
```

门禁命令链：YAML `npm run verify:predeploy` → package.json 15 子入口均 DEFINED。生效 workflow 镜像字节相等。

## 5. 79 条冻结判据覆盖表

判定顺序：冲突 → 细化 → 技术性 → 添加。技术性 = 无业务发明 + 无 HTML 冲突，不要求每行 HTML 锚。工程问题仍引用 GEN，不因无业务冲突直接放行。三 HTML 只读；B/C 已确认原意不误报冲突。

业务词扫描（六文件）：`票种|营业日|商户状态|铺位|能耗|巡检|破窗|U 盘|赠酒|抽成|工单|看板|楼宇|核销|通票` 无命中。

### 5.1 HTML 派生（61）— 适用结论：本模块不适用（无实现、无冲突）

对下列 ID，锁内判据均为「映射模块内实现/测试/文档须与该锚点原文一致」。本模块是 Git/CI 闸门，不实现这些业务对象。不适用理由不是「HTML 没写所以可删」，而是「本文件集合未承载该锚点，也未发明相反规则」。

| ID | 锁内标题（HTML 锚点原文关键词） | 锁来源行 | 本席回读 | 适用/证据 |
|---|---|---|---|---|
| BEQR-085 | 项目开放状态 | BEQR html:140 | 原文「未开放、准备中、开放中、暂停、检修中、已关闭、异常」 | 不适用。脚本无项目开放状态 |
| BEQR-086 | 场次状态 | :142 | 「未开始、入场中、已满员…」 | 不适用 |
| BEQR-087 | 游乐「当天／每天」口径 | :144 | 营业日 10:00–次日 10:00，禁服务器时区 | 不适用 |
| BEQR-088 | 容量双阈值 | :145 | 软/硬容量 | 不适用 |
| BEQR-089 | 园区容量 | :146 | 指向双阈值 | 不适用 |
| BEQR-090 | 设备关键/辅助分级 | :147 | 关键安全设备依赖 | 不适用 |
| BEQR-091 | 全园故障与补偿通票 | :148 | 全园故障才补偿 | 不适用 |
| BEQR-092 | 安全严重度 | :149 | 提醒/警告/阻断/重大 | 不适用 |
| BEQR-093 | 巡检任务状态 | :150 | 草稿…取消 | 不适用 |
| BEQR-094 | 越界类型 | :151 | 物理/经营范围 | 不适用 |
| BEQR-095 | 团体/包场 | :153 | 包场停对外核销 | 不适用 |
| BEQR-096 | 游乐增强阶段 | :154 | P1/P2 不得绕过核销 | 不适用 |
| BEQR-118 | 运营营业日 | :187 | 钱账按营业日 | 不适用 |
| BEQR-146 | 运营公司职责 | :229 | 娱乐平台/游乐/传媒/中央餐饮 | 不适用 |
| BEQR-155 | 商户/运营主体状态 | :238 | 待准入…黑名单 | 不适用 |
| BEQR-160 | 招商线索状态 | :248 | 待跟进…已搁置 | 不适用 |
| BEQR-161 | 招商意向阶段 | :249 | 初步接触…待签约 | 不适用 |
| BEQR-162 | 品牌/客户库类型 | :250 | 意向/储备/合作/黑名单品牌 | 不适用 |
| BEQR-163 | 合同变更类型 | :251 | 续租…不支持转租 | 不适用 |
| BEQR-164 | 退租结算状态 | :252 | 待结算…诉讼/仲裁 | 不适用 |
| BEQR-165 | 内部应付执行状态 | :253 | 待付/失败/已结清 | 不适用 |
| BEQR-166 | 合同审批链 | :255 | 主线+平行线 | 不适用 |
| BEQR-167 | 租金/账单类型 | :256 | 固定租金…违约金 | 不适用 |
| BEQR-168 | 营业额抽成模式 | :257 | 保底/比例/阶梯 | 不适用 |
| BEQR-169 | 公区能源费 | :258 | 并入物业费 | 不适用 |
| BEQR-170 | 铺位租赁状态 | :259 | 空置…不可租 | 不适用 |
| BEQR-171 | 物业工单类型 | :260 | 报修…其他 | 不适用 |
| BEQR-172 | 物业工单状态 | :262 | 待派单…已超时 | 不适用 |
| BEQR-173 | 商户服务工单状态 | :264 | 已提交…已取消 | 不适用 |
| BEQR-174 | 工单时效 | :265 | 响应/处理时限 | 不适用 |
| BEQR-175 | 楼宇机电设备状态 | :266 | 正常…报废 | 不适用 |
| BEQR-176 | 设备维保类型 | :267 | 定期保养…部件更换 | 不适用 |
| BEQR-177 | 能耗表计类型 | :268 | 电/水/燃气/冷热 | 不适用 |
| BEQR-178 | 抄表状态 | :269 | 待抄…异常 | 不适用 |
| BEQR-179 | 能耗归集对象 | :270 | 公区/商户/回路 | 不适用 |
| BEQR-180 | 楼宇巡检任务状态 | :271 | 待执行…已关闭 | 不适用 |
| BEQR-181 | 楼宇巡检类型 | :273 | 安防/消防/设施设备/环境卫生四类 | 不适用 |
| BSM-020 | 楼宇运营（子系统入口卡） | BSM html:656 | 机电/能耗/巡检转工单 | 不适用 |
| BSM-028 | 主要功能（基础管理） | :698 | 综合体/商户/多对多/合同 | 不适用 |
| BSM-081 | 楼宇运营：机电/能耗/巡检 | :1401 | 同域 h2 | 不适用 |
| BSM-082 | 主要功能（楼宇） | :1405 | 台账/抄表/四类巡检 | 不适用 |
| BSM-083 | 和谁有关（楼宇） | :1414 | 连物业/合同/财务/看板 | 不适用 |
| BSM-111 | 经营看板 | :1781 | 总览下钻 | 不适用 |
| BSM-112 | 主要指标 | :1785 | 收入/客流/核销… | 不适用 |
| BSM-113 | 谁会看 | :1813 | 管理层/运营/财务 | 不适用 |
| BSM-114 | 各类入口 | :1827 | 管理后台/Web/商户侧/设备等产品入口表 | 不适用。表中无 Git 钩子或 CI；本模块不冒充产品入口 |
| BSM-115 | 相互关系 | :1897 | 子系统必须相连 | 不适用 |
| BSM-116 | 系统交付与上线 | :1928 | 生产加固+现场安装 | 不适用。本模块不是生产换密/安装包 |
| BSM-117 | 正式上线时的安全加固 | :1932 | 弱口令→强密、U 盘密钥、破窗留痕 | 不适用。源文件无口令/U 盘实现，也未与原文相反 |
| BSM-118 | 能自动识别环境的安装包 | :1940 | 测试/生产自动配置+教程 | 不适用。`install-git-hooks.sh` 只装开发者 Git 钩子 |
| BSM-119 | 和谁有关（交付） | :1947 | 留痕+最高权限口令 | 不适用 |
| PGM-015 | 楼宇运营（治理入口卡） | PGM html:399 | 电梯暖通水电消防 | 不适用 |
| PGM-018 | 物理项目与运营公司多对多 | :415 | 顶层平台不自动取得收入 | 不适用 |
| PGM-026 | 主要功能（商户和主体） | :492 | 四类主体绑定/六类合作 | 不适用 |
| PGM-039 | 智能设备场景联动范围 | :636 | 不计时/不自动执行未立项场景 | 不适用。本模块无设备动作 |
| PGM-052 | 楼宇运营 h2 | :806 | 楼宇本体运营 | 不适用 |
| PGM-053 | 楼宇机电设备 | :810 | 台账状态正常…报废 | 不适用 |
| PGM-054 | 能耗计量 | :821 | 表计/异常不得直接出账 | 不适用 |
| PGM-055 | 楼宇巡检 | :832 | 四类；抄表不重复 | 不适用 |
| PGM-056 | 和谁有关（楼宇治理） | :844 | 物业/财务/供应链/留痕 | 不适用 |
| PGM-066 | 已拍板规则和待排期问题 | :951–970 | 见下节专条 | 见 5.2 |

### 5.2 PGM-066 专条（HTML 原文已读）

`platform-governance-map-for-operations-v0.1.html:951` 起拍板列表，本席核对的与本模块可能挨着的句子：

- 「界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字……可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样」
- 「所有有过程的业务动作都必须留痕」针对酒水/可追责物品履约；安检例外
- 解释权声明：三 HTML 经用户批准的内容为准

`install-git-hooks.sh:16` 英文 `installed pre-push hook ->` 是开发者 CLI，不是上述四类产品终端的用户展示。按 B/C 用户确认原意，**不报冲突**。其余拍板（集团视图、日结补录、企微永久存储、下单/支付时间等）本模块无实现。SKIP_CI_WAIT 的 stderr 警告是开发者提示，不是业务审批留痕系统。

### 5.3 GEN-001..018（18）— 全部核对，不是跳过

| ID | 结论 | 证据 |
|---|---|---|
| GEN-001 | 通过 | 五脚本+镜像+回归均有可验证用途。镜像由现有机检强制，不是死重复。回归只绑 001–004，无额外抽象 |
| GEN-002 | 通过 | bash -n 四脚本 0；回归 0；§3 主路径/拒绝/错误均实跑，与注释声称一致 |
| GEN-003 | 通过 | 回归真实 spawn 源脚本，断言可失败（反断言 1!==0）。本席另跑未写入该文件的分支。未把「测试绿」当成唯一证据 |
| GEN-004 | 通过 | 方法枚举白名单；stdin 四字段；ALLOW/SKIP 环境变量显式比较；脏树在信任 GitHub 之前拒绝。钩子自承非安全边界 |
| GEN-005 | 通过 | 无产品授权模型。默认拒绝直推 main；覆盖项是开发者环境变量，不是伪造业务角色。不新造审批角色 |
| GEN-006 | 通过 | 无库表。合并对象用 `--match-head-commit` 钉住所验 SHA，等待期间 tip 变化则中止 |
| GEN-007 | 通过 | 空 run 列表有界 30 次；查询失败立即停（不再伪装成空列表空转）；重复跑 merge 在 SHA 变了会拒绝 |
| GEN-008 | 通过 | workflow 名 `predeploy-gates`、文件名 `predeploy-gates.yml`、脚本 `--workflow predeploy-gates.yml`、npm `verify:predeploy` 一致；双 YAML 字节相同 |
| GEN-009 | 通过 | 查询非 0 保留 stderr 并 exit 1；watch 失败不 merge；不再 `2>/dev/null`/`|| true` 吞 gh 错 |
| GEN-010 | 通过 | 无注入点拼进 sh -c；npm ci --ignore-scripts；无明文秘密。钩子覆盖不是安全边界（自承） |
| GEN-011 | 通过 | 拒绝/覆盖/SKIP 均有固定中文（或 SKIP 双语标记）stderr；回归断言 stderr 含 `AUDIT_QUERY_AUTH_FAILURE` |
| GEN-012 | 通过 | 重试上限 30；回归 finally 删临时目录；无无界读仓库 |
| GEN-013 | 通过 | 四脚本 bash -n；CI pin SHA、node 22、ignore-scripts；安装不依赖未声明全局 hooksPath 也能落到 common-dir |
| GEN-014 | 不适用 | 本模块无数据库迁移 |
| GEN-015 | 通过（产品 UI 不适用） | 无 GUI。用户可见提示以中文为主。L16 英文成功句按 PGM-066 原意不升为冲突 |
| GEN-016 | 通过 | 注释中的 `ops/160/propose-change.sh`、`ops/160/merge-pr.sh`、`.github/workflows/predeploy-gates.yml`、`npm run verify:predeploy` 路径/命令均存在。不把主仓旧 hook 写成已发布 |
| GEN-017 | 通过 | 本模块内容按序归技术性；HTML 派生项不适用而非删除；无添加项（无新业务实体/枚举/角色/闸/产物类型/强制技能） |
| GEN-018 | 通过 | 结论落到 HTML 原文、实跑输出或 Git 钩子/config 行为，不用上轮意见当依据 |

## 6. 对 001–004 的独立复验（不是台账转述）

| 原 ID | 本席复跑 | 结果 |
|---|---|---|
| 001 按 remote_ref 拦目标 main | 回归四组 + 本席多行/删除/大小写/后缀 | 源码行为正确。运行时旧钩子仍整行匹配，见 §8 |
| 002 `git config --path` 展开 ~ | 回归：HOME/custom-hooks 存在且内容一致、可执行 | 通过。相对/绝对/默认/worktree 本席另验，与 `git-path hooks` 一致 |
| 003 脏工作区不推送 | 回归 + 本席再跑 | exit≠0，无 pushed |
| 004 gh 查询失败立即退出并留原因 | 回归：stderr 含 AUDIT_QUERY_AUTH_FAILURE，queries=1，无 merged | 通过。真·空列表仍 30 次后失败 |

## 7. 与 005–011 同类、本席独立复判为「不新开」的已核行为

不把台账当事实；下列是本席自己跑完后的分类。不重造「多条件同时才成立」的理论缺陷。

| 主题 | 本席证据 | 分类 | 是否新 finding |
|---|---|---|---|
| SKIP_CI_WAIT=1 / ALLOW_MAIN_PUSH=1 | 均实跑：SKIP 仍钉 SHA 并打警告；ALLOW 放行并打覆盖句。开发者提示，非业务审批 | 技术性保持 | 否 |
| CI 镜像 + 15 子入口 | 字节相等；15 名均 DEFINED | 技术性保持 | 否 |
| 回归文件归属 | 现已直接新增并本席执行 | 技术性 | 否（不另立测试工程） |
| install 英文成功句 | L16 仍英文；PGM-066 终端范围不含 Git CLI | 技术性保持 | 否 |
| 同 SHA 多 run 全 AND | 代码 `for rid in $run_ids`；单 id watch 失败已拒合并。不新增过滤策略 | 技术性保持 | 否 |
| merge 不读 baseRefName | propose `--base main` 已实跑 create 路径 | 技术性保持 | 否 |
| hook 无 set -euo | 常规四字段未误放行 | 技术性保持 | 否 |

pre-push 注释仍写正道以 `gh pr merge` 收尾，而 propose 正文禁止手敲。这是注释宽松，不是可执行主路径错误；可执行入口已指向 `merge-pr.sh`。不升 finding。

## 8. 验证限制（不是条件 PASS）

这些限制阻止本席做出「运行时已发布 / 线上 Actions 已绿 / action 标签已核」的声称，**不**构成源码未修或未知缺陷：

1. 未执行主仓 `ops/160/install-git-hooks.sh`。`.git/hooks/pre-push` 仍为 2026-07-05 旧安装副本，SHA `46aabdf3…`，逻辑仍是整行匹配 `refs/heads/main`。源码修复 ≠ 运行时发布。
2. 未真实 `git push`、未 `gh pr create/merge`、未部署。
3. 未用网络核验 GitHub Actions 是否对该私有库启用，也未核验 `actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5` 是否等于注释 v4.3.1、`actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020` 是否等于 v4.4.0。
4. 未跑全仓 `npm run verify:predeploy`（会扩到其它模块）。已核名字、15 子入口定义、CI 步骤与镜像。
5. 未读本轮 self raw 或其它模块 raw。
6. grilling 按结构自取事实，未向操作员发问（本阶段禁止中途发问）。

## 9. 新 finding

无。

本席临时编号未占用。不添加业务、判据、闸、产物类型、角色或强制技能。不扩大到父链其它模块已知问题。

## 10. 统一判定

范围六文件已整份实读；79 条锁 ID 均给出适用证据或不适用理由；三线均执行；声称均有类型 1/2/3/4 之一；无未答 questions；无未定稿新缺陷；锁行有效。

**结论：PASS**

（审计结论 PASS。模块生命周期仍由调度写 ledger；本席不改台账。B4-r1 内容/范围 PASS 不能替代本 B5 全量结论；本结论也不把运行时旧钩子说成已发布。）
