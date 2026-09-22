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

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M007 B5-r1 Grok 乙席完整审计报告

- 文件版本：V0.02（追加；保留本路径既有 V0.01 全部 **26860** 字节，SHA256 `9e854ef37d9e8587c67721d8d48fe25e8b7655e4589c610c2314d3b45dcfb5e1`，不覆盖、不改 V0.01 原文；自身版本 V0.01→V0.02）
- 席位：Grok（审计席乙，独立）。步骤：B5-r1（整模块全量三线重审）。**不继承**本文件 V0.01 PASS、不继承本轮 B1/B2 PASS、不把队列空写成模块 PASS
- 计划：当前 471 模块计划；`docs/audit-2026-09/plan/modules.md` V0.04；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（`docs/audit-2026-09/160-audit-prompt.md` 当前不存在）
- 模块：M007 — 治理门禁与 CI 基线／运行和发布脚本／第1段（标记 `NORMAL`；计划历史有效行 156 不改；当前 `git ls-files -z -- <五路径> | xargs -0 cat | wc -l` = **163**）
- 范围同本轮 B1：唯一受审 FILES 五文件 + 必要直接回归 `tools/gate-hardening-tests/m007-regression.mjs`（102 行，不在 FILES，不扩范围）
- 映射：`plan/criteria-lock.md` **V0.14** 行 `M007 → [BEQR-085…096,118,146,155,160–181, BSM-020,028,081–083,111–119, GEN-001–018, PGM-015,018,026,039,052–056,066]`（**79** 唯一；定义区 79/79 自读存在；HTML 派生 61 条锁摘录与现文锚点行 **MISMATCH=0**）
- 输入：findings **V3.44** B2 双 PASS 定稿、十一行当前队列空、跳新增 B3/B4、累计 B3 **仍 1**。旧 001–011 只作编号与原句线索，本票对现行字节独立重判。**未读** `M007-b5-r1-self-raw.md` 及任何本次 selfB5
- ledger（只读顶部两行 + 本模块当前段，不写）：顶部基准 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`，`LOCK-AMEND-COUNT: 1`；M007 `IN_PROGRESS`。本席不改 ledger/findings/锁/sidecar/HTML/源/收口/主仓 hook
- 正本：`/home/james/complex-management` 分支 `main`；`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=ebb1eb99c610df330c07d24278798c2d3611299a`（ahead 49 / behind 54，本席未 fetch）。五 FILES + 回归相对 HEAD 与 origin/main **blob 全等**；该六路径 porcelain 空
- 约束：无 MCP、无子代理、无第三席、无真实 GitHub 写、无 fetch/push/开 PR/合并/部署/DB、固定 CLI 不变。隔离 fixture 仅临时 Git init/commit 与 file:// 远端，不得碰真实 origin
- 本席 B5-r1 结论：**PASS**

---

## 0. §6 技能自核（本席本票实际环境）

开工前按本机路径读 `SKILL.md`。用户本轮指令覆盖 skill 默认：无 MCP、无子代理、不等操作员、不问人、不写 findings/ledger/锁/源。grilling 本体「等用户回答 / 事实题派子代理」作废：只借追问结构，事实自读。

| 技能 | 本票核验 | 本次用法 |
|---|---|---|
| `graft` | `/home/james/.claude/skills/graft/SKILL.md` | `graft map` / `graft ask` 定位回归文件；`graft grep "M007-b5-r1"` 报告未索引，改直读五源全文 |
| `using-arc` | Arc `skills/using-arc/SKILL.md` | 仅路由；**未**调用会写 §7 外产物或派子代理的 Arc 工作流 |
| `verification-before-completion` | superpowers 6.3.0 | 任何 PASS/FAIL 前用本报告内本票命令输出 |
| `ponytail` | ponytail 4.9.0 | B5 只审不修；五文件已是最小 CI/hook 入口。用户明确要求完整报告，按技能「用户要的解释不是债」写全 |
| `grilling` | mattpocock `productivity/grilling` | 对抗式／第一性原理追问树；**自问自答**；不派子代理、不问操作员 |

缺口（如实，不冒充已调用）：

- `batch-grill-me`：本环境技能目录**未找到**独立 `SKILL.md`
- 本票未再通读 B1 列出的 `codebase-design` / `diagnosing-bugs` / `systematic-debugging` / `tdd` / `ponytail-review` / `i-have-adhd` 全文，不把那些名字写成「本票已核读」

明确不用：`dispatching-parallel-agents`、`using-git-worktrees`、`research`、`ponytail-audit`、`git-guardrails-claude-code`（会写 hook 配置）。未派子代理。未改 HTML／findings／ledger／lock／sidecar／五受审文件／回归文件。未执行 `npm install`／真实 `npm ci`／全仓 `verify:predeploy`／部署／`gh api -X PUT`／`gh pr merge`／业务仓 `git push`。临时 Git fixture 在 `/tmp/m007-b5-r1-20260922T122330Z` 与 `/tmp/m007-b5-r1-pushfix`。

---

## 1. 输入哈希、行数、覆盖

本席独立实算（未抄 sidecar／派发期望值）。正文不再重复锁字段名。

命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → 写在本次报告第一行。

| 项 | 值 |
|---|---|
| 工作区 `criteria-lock.md` | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`；sidecar 单行同值；ledger **顶部**同值。`HEAD` 该文件 blob 仍为旧 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（LOCK-AMEND 工作区已生效，本席不提交、不把脏工作区写成五文件缺陷） |
| 锁版本 | V0.14；判据总数 401；`LOCK-AMEND-COUNT: 1`；M007 **79** |
| `ops/160/install-git-hooks.sh` | 16 行／707 字节／SHA256 `8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b`；blob `c3dc157dbfd23e77c25a071e5bf628ebe289cef9`；标题 V0.01；LF、无 CRLF/BOM/NUL |
| `ops/160/merge-pr.sh` | 73 行／4180 字节／SHA256 `3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06`；blob `ef2a97b4f00bd13b26a01ecb2b74555dd0a38eaa`；标题 V0.02 |
| `ops/160/propose-change.sh` | 30 行／1626 字节／SHA256 `88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b`；blob `35a5139e2b176f88a5041cf4bbed4c0249898589`；标题 V0.01 |
| `ops/160/hooks/pre-push` | 17 行／922 字节／SHA256 `e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85`；blob `3ae3d237d26ba0f5bd937f36c7e8f856864f8b6f`；标题 V0.01 |
| `ops/ci/predeploy-gates.yml` | 27 行／1335 字节／SHA256 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`；blob `3857c0f9b3ca98e4d2e8d94771e91be606e0e732`；标题 V0.02。与 `.github/workflows/predeploy-gates.yml` `cmp` exit 0（字节全等） |
| 官方有效行 | **163** |
| 直接回归 | 102 行／5239 字节／SHA256 `10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8`；blob `6fc4720e3e704539e28a9d488b967a28a79548cf`；标题 V0.01 |
| 旧本文件前缀 | 26860 字节／`9e854ef37d9e8587c67721d8d48fe25e8b7655e4589c610c2314d3b45dcfb5e1` |
| 宿主 | node v24.21.0；npm 11.19.0；git 2.43.0；gh 2.100.0；bash 5.2.21。根清单 `packageManager=npm@10.9.8`（本机工具链差披露，不写成五文件缺陷） |

五源 SHA 与本文件 V0.01 所记及本轮 B1 所记**同字节**。校 SHA 后复用同字节完整阅读，并在本票再次逐行读完 16+73+30+17+27 与回归 1–102。`bash -n` 四脚本 exit 0。yml `yaml.safe_load` 成功。

三 HTML 只读 SHA：枚举 `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`／业务地图 `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`／治理地图 `5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f`。

---

## 2. 第一性原理线（2.3 三问）

依据类型只许：① HTML 逐字 ② 可执行产物 ③ 官方规范原文 ④ 可复现命令输出。本票命令见 §7，不引用历史 raw 当依据。

### 2.1 `ops/160/install-git-hooks.sh`（16 行，V0.01）

1. **为何必须存在 / 删除代价**  
   把受控 `ops/160/hooks/pre-push` 装到 Git 实际生效的 hooks 目录（默认 common dir，或 `core.hooksPath`）。删除后新克隆／改了 hooksPath 会静默没有拦截。三份 HTML **没有**「必须装 git hook」句。最接近交付章 BSM-116 L1928–1952 原文是生产换弱口令、U 盘密钥、安装包+教程，**不是** git hook。答不上 HTML 哪一条会做不到 ≠ 该删。按 2.1：**无冲突 → 指不出必须实现的 HTML 上位 → 技术性（CI/工程脚本，无业务发明）**。
2. **声称是否执行**  
   隔离真实 Git（`/tmp/m007-b5-r1-20260922T122330Z`，未把安装器指向主仓作为本票验收）：默认装到 `rev-parse --git-common-dir`/hooks，mode 可执行，字节等于源；`core.hooksPath=~/custom-hooks` 时 `git config --get` 仍为字面 `~/custom-hooks`，`git config --path --get` 展开为 `$HOME/custom-hooks`，产物在 `HOME/custom-hooks/pre-push`，**仓内不出现字面 `~` 目录**；绝对路径 hooksPath、相对 `.githooks`、worktree 执行装到 **common** hooks：均 exit 0。`git help config` pathname：`~/` 展开为 `$HOME`（类型 3）。
3. **未验证前提**  
   未在 macOS/`install` 非 GNU 环境跑。本机 `git 2.43.0`：相对 hooksPath `--path` 取值仍为 `.githooks`（未绝对化），脚本先 `cd` 仓库根再 `mkdir/install`，相对路径落在仓根 `.githooks`——与本席实跑一致。主仓运行时副本见 OBS-1，**不能**把源码修复说成已安装。

### 2.2 `ops/160/hooks/pre-push`（17 行，V0.01）

1. **为何必须存在**  
   补私有库免费版无分支保护时「直推 main」缺口（L2–4）。删除后只靠纪律。HTML 无此句。BSM-114 十一端表不含 git CLI。技术性。文件头写明「开发者提示,非安全边界」——`git push --no-verify` 可绕，本席不把「可绕」升成未声明的安全边界。
2. **声称是否执行**  
   `man githooks` 原文：stdin 行格式 `<local ref> SP <local object name> SP <remote ref> SP <remote object name> LF`；删除时 local ref=`(delete)`、local sha=zeros（类型 3）。现行源 **只按第三字段 `remote_ref == refs/heads/main`**。stdin 探针：feature→main 无覆盖 exit 1；`ALLOW_MAIN_PUSH=1` exit 0 且「覆盖」；`ALLOW_MAIN_PUSH=true` **不**放行；mainish、main/topic、tag `refs/tags/main` 不误伤；删除 main 拦；多 ref 含 main 拦；空 stdin exit 0；本地 main→远端非 main 放行。真实 `git push` 到 **file://** 远端（源 hook 装在 fixture **绝对** `.git/hooks`，非主仓）：推 origin main → exit 1；feature → 0；`main:refs/heads/from-main` → 0；`ALLOW_MAIN_PUSH=1` 推 main → 0。
3. **未验证前提**  
   未对真实 `origin`（GitHub）做 push。本工作区 `.git/hooks/pre-push` **不是**现行源副本（OBS-1）。本票审的是 FILES 源字节与 fixture 安装后的行为。

### 2.3 `ops/160/propose-change.sh`（30 行，V0.01）

1. **为何必须存在**  
   声明的正道：工作区干净 → 本地 `verify:predeploy` → `git push` → 开/更新 PR。删除后易手敲直推或手开 PR 绕过本地闸。HTML 无 PR 流程句。技术性。
2. **声称是否执行**  
   真实 Git fixture + 只 stub npm/gh：无标题非 0；在 main 拒绝「不要在 main 上提议」；**脏工作区** `git status --porcelain` 非空 → exit 1 中文「未提交改动」，**不调用 npm、不 push**；干净路径调用 `npm run verify:predeploy` 后 `git push -u origin feature/p`（file 远端出现该 ref），再 `gh pr create --base main --head feature/p --title …`；已有 PR（`gh pr view` 0）则不 create；npm 非 0 不推新 commit。未对 GitHub 做 create/push。
3. **未验证前提**  
   未跑真实全仓 `verify:predeploy`（全局 PGM 台账红 / 入口快照漂移已登记他模块，本席不刷绿、不把未跑全链写成五文件缺陷）。

### 2.4 `ops/160/merge-pr.sh`（73 行，V0.02）

1. **为何必须存在**  
   私有库免费版设不了 required check。只读 `gh api …/actions/workflows/predeploy-gates.yml`：`state=active`，`path=.github/workflows/predeploy-gates.yml`，仍只是可见信号。删除后 `gh pr merge` 可在红灯合并。技术性。
2. **声称是否执行**  
   真实 Git fixture + stub gh/sleep（cwd=fixture，非主仓）：非法 method `--ff` exit 1；在 main 拒；脏工作区拒；无远端分支拒；HEAD≠origin 拒；无 PR 拒；`gh run list` exit 2 立即失败且 stderr 含 `AUDIT_QUERY_AUTH_FAILURE`、查询 1 次、无 sleep、无 merge；空列表成功则重试（本席 2 次 sleep + 第 3 次有 id 后 watch/merge）；watch 非 0 中止且不 merge；多 run 全 AND（第二个失败不 merge）；`SKIP_CI_WAIT=1` 打警告、不查 run、仍核 SHA 再 merge；等待后 `headRefOid` 漂移中止；绿灯 argv：`pr merge <branch> --squash|--rebase --match-head-commit <40位SHA>`，与 `gh pr merge --help` 原文一致（类型 3）。查询失败时脚本因 `set -euo pipefail` 以 **gh 的 exit 2** 退出（不是脚本里另写的 `exit 1`），仍 fail-closed——见 004 重判。未调用真实 `gh pr merge`。
3. **未验证前提**  
   未验证 `gh run watch` 对同一 `databaseId` 重跑 attempt 的 GitHub 侧语义。L58 `gh run view … || true` 只在 watch 已失败后丢 URL，不影响中止。

### 2.5 `ops/ci/predeploy-gates.yml`（27 行，V0.02）

1. **为何必须存在**  
   与生效定义 `.github/workflows/predeploy-gates.yml` 的字节镜像。`tools/verify-delivery-gates.mjs` L290–308（M008，只读断言段）：LF 归一后全等，且 `fetch-depth: 0`、`run: npm run verify:predeploy`、`HTML_GATES_BASE` 表达式逐字相等。删除镜像会让该机检失败。技术性（提示词 2.1 明文「CI 脚本」不算业务发明）。
2. **声称是否执行**  
   `yaml` 解析：`on.pull_request.branches=[main]`、`on.push.branches=[main]`、job `predeploy`、`ubuntu-latest`、checkout SHA `34e114876b0b11c390a56381ad16ebd13914f8d5` + `fetch-depth: 0`、setup-node SHA `49933ea5288caeca8642d1e84afbd3f7d6820020` `node-version: "22"`、`npm ci --ignore-scripts`、`npm run verify:predeploy` + `HTML_GATES_BASE` 表达式。只读 Actions：workflow `id=307870467` `state=active`。近 5 次 run 均 `conclusion=success`（最新 `35601416957` event=push `headSha=ebb1eb99…`＝当前 origin/main）。L6「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main」：两部署脚本 `REQUIRED_GATES` 各 15 名 **等于** `package.json` `verify:predeploy` 链 15 名，且 15 名均 DEFINED；pre-push 实跑不含 npm。`npm help ci`：`--ignore-scripts` 仍允许随后 `npm run`（类型 3）。本票 GET `repos/actions/checkout/git/ref/tags/v4.3.1` object_sha＝YAML 钉死 SHA、type=commit；`setup-node` tag `v4.4.0` 同理相等。
3. **未验证前提**  
   `node-version: "22"` 在 GitHub runner 上装最新 22.x 的官方行为未在本机跑 runner 证实。`github.event.before` 在新建分支/force-push 的 40 个零：未构造。未跑 `npm ci`。

**删除测试**：删这 5 个文件，复杂度不会消失，而会散到「人记得看 CI / 记得不推 main / 记得两份 yml 一致」。它们赚到了存在价值。不是无需求纯转发。

---

## 3. 顺从式：79 判据全覆盖

判定顺序每条都走完：冲突 → 细化 → 技术性 → 添加。GEN-017：读了 HTML 原句上下文，**不因字面词未出现就判添加**，也不把「本模块未实现游乐/地产业务」写成遗漏。每 ID 本票独立核对。

### 3.1 GEN-001 … GEN-018

| ID | 适用与证据 | 分类 | 缺陷？ |
|---|---|---|---|
| GEN-001 | 五文件均有可验证用途（装钩、拦 main、提议、等 CI 再合、CI 镜像）。镜像重复是 M008 机检约束，不是死代码。逃生口 `SKIP_CI_WAIT`/`ALLOW_MAIN_PUSH` 有注释与警告，不是为假设场景加的抽象层 | 技术性 | 否 |
| GEN-002 | 入口/主路径/错误路径已用原始脚本+真实 Git fixture / stub 边界执行。绿灯 merge argv 与 `gh` 官方 help 一致。001–004 现行行为与声称一致（见 §5） | 技术性 | 否（现行） |
| GEN-003 | FILES 仍无测试文件。仓内直接回归 `m007-regression.mjs` 102 行覆盖 001–004 关键分支，本席确认只写 `.scratch/m007-regression-*` 后执行，7 项 PASS、exit 0、无残留。测试通过不替代上面的行为探针。不把「测试文件不在 FILES」升成本模块新修复项 | 技术性 | 否（007） |
| GEN-004 | 标题/正文以独立 argv 传给 `gh`（`--title "$title"`），不经 shell 二次展开。覆盖开关只认字面 `1`。分支名来自 `git rev-parse`。无把未校验网络输入当权威 | 技术性 | 否 |
| GEN-005 | 无业务对象授权。合并/推送依赖操作者已有的 `gh`/`git` 凭证。默认拒绝直推 main、默认等 CI。不是界面隐藏式授权 | 技术性 | 否 |
| GEN-006 | 无业务库写入。合并钉 SHA 防止等 CI 期间 tip 漂移（已实跑漂移中止） | 技术性 | 否 |
| GEN-007 | 等待后再核 `ls-remote` 与 `headRefOid`。重复跑 merge 若已合，由 `gh` 失败 + `set -e` 中止。无并发写同一业务实体 | 技术性 | 否 |
| GEN-008 | workflow `name`/文件名与 `gh run list --workflow predeploy-gates.yml` 都能列出 run。`verify:predeploy` 字符串与 CI `run:` 一致。两份 yml 字节一致。无 DTO/枚举 | 技术性 | 否 |
| GEN-009 | 多数失败路径非 0 且中文原因。查询失败不再吞成空列表（004）。L58 `|| true` 仅丢已失败 run 的 URL。pre-push 无 `set -euo` 见 011 | 技术性 | 否（现行主路径） |
| GEN-010 | 五文件无明文秘密。`npm ci --ignore-scripts` 与 `npm help ci` 原文一致。hook 非安全边界已声明。未发现注入/路径穿越 | 技术性 | 否 |
| GEN-011 | SKIP_CI_WAIT/ALLOW_MAIN_PUSH 只打 stderr「请在 PR/说明记录原因」，脚本不强制写 PR 正文。属工程提示而非业务留痕（PGM-066 留痕原文是业务动作） | 技术性 | 否（005） |
| GEN-012 | CI 等待 `seq 1 30` × `sleep 10` 有界。查询失败立即退出，不把 30 次用满。无无界文件/连接 | 技术性 | 否 |
| GEN-013 | 依赖 bash、git、gh、npm、`install`、`seq`（本机 PATH 均在）。CI 钉 action SHA 且本票 GET tag 对象与注释版本一致。workflow `state=active` | 技术性 | 否 |
| GEN-014 | 无迁移文件、无 SQL | 不适用 | 否 |
| GEN-015 | 无产品 UI/键盘焦点。PGM-066 L965 原文「后台/客人端/现场设备/安卓端」。YAML 键名受 GitHub Actions 工作流语法约束（类型 3，必须英文键）。CLI 用户可见文案以中文为主；install L16 一句英文见 008 | 技术性 | 否 |
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

1. **直推 main 是否真拦？** stdin + 真实 file-remote push（绝对 hooks 路径）：拦。mainish/main/topic/tag 不误伤。删除 main 拦。`--no-verify` 可绕：文件头已写非安全边界，不升未声明安全缺陷。
2. **本地 main 推到非 main 远端？** 现行按 `remote_ref` 判断，stdin 与 `git push origin main:refs/heads/from-main` 均放行。旧整行匹配缺陷不在现行源（001）。
3. **含空格的非法 local ref？** `read -r` 四字段会错位，本票 stdin `refs/heads/feat ure … refs/heads/main` **got exit 0**。`man githooks` 四字段以 SP 分隔；Git 标准 ref 不含空格。第一次正常 `git push` 不会发出该 stdin。**这是 harness 非法输入，不是产品缺陷**；也不伪称「若断言必须 deny 则该探针通过」。铁律 6：两三件不巧 → 不修、不立项。
4. **propose 是否推未提交内容？** 脏树在 npm 之前 exit 1；干净才 npm+push。003 现行成立。
5. **merge 是否合错 SHA？** 等待后再比 origin 与 `headRefOid`；漂移中止。`--match-head-commit` 写入真实 argv。
6. **CI 红能否合？** watch 非 0 中止且不调 merge。SKIP_CI_WAIT=1 仍 merge，但打印警告——与注释逃生口一致，不升（005）。
7. **gh 挂了当「没 run」？** 否。exit **2** + 原 stderr，1 次查询，无 sleep，不合（004）。**不把 exit 2 藏成成功**；也不是产品误放行。
8. **同一 SHA 多个 run？** 源码全 watch；一个失败即拦。本席 11+22 第二失败不合。第一次按正道通常一个 PR run。铁律 6 → 009 仍只登记。
9. **PR 基线不是 main？** 源码不读 `baseRefName`。正道 propose 固定 `--base main`。第一次按脚本跑不撞（010）。
10. **workflow 是否对 feature push 触发？** `on.push.branches:[main]` 不触发 feature。`on.pull_request` 在开 PR 后触发。
11. **镜像分叉？** 当前两份 yml `cmp` 0。
12. **tilde 安装装到仓内 `~`？** 否。`--path` 展开到 `$HOME/custom-hooks`（002）。
13. **伪成功？** 未发现 exit 0 掩盖失败的主路径。查询失败以非 0 退出。
14. **本机已安装 hook 是否等于源？** 不等于（OBS-1）。不把「未重装」写成源缺陷；本轮禁止主仓安装。

---

## 5. 新 GR 与旧 11 项独立重判

**新 GR：空集。** 无冲突、无添加、无未定稿工程缺陷。

旧 ID 只作输入。下列为**现行源**重判，不继承历史 PASS/队列操作、不继承 B1 结论。

### M007-001（历史已修出队）

- **位置（现行）**：`ops/160/hooks/pre-push:6–8`
- **冻结判据**：GEN-002/004/008/009/016/018
- **2.1 分类**：技术性（无业务发明）
- **2.3 依据类型**：③ `man githooks` 四字段 + ④ stdin / 真实 file-remote push
- **现行证据**：`if [[ "$remote_ref" == "refs/heads/main" ]]`。`local main → remote feature` stdin exit 0；`git push origin main:refs/heads/from-main` exit 0。feature→main 仍拦。
- **工程缺陷是否仍成立**：**否**（历史缺陷在现行源不在）。不新开 GR，不把已修 ID 拉回队列。

### M007-002（历史已修出队）

- **位置（现行）**：`ops/160/install-git-hooks.sh:7–15`
- **判据**：GEN-002/009/013/016/018
- **分类**：技术性
- **依据**：③ `git help config` pathname `~/`→`$HOME` + ④ 真实 Git
- **现行证据**：`git config --path --get core.hooksPath`。tilde 产物 `$HOME/custom-hooks/pre-push` 等于源、可执行；仓内无字面 `~` 目录。默认 / 绝对 / 相对 / worktree→common 均过。
- **缺陷仍成立？**：**否**。

### M007-003（历史已修出队）

- **位置（现行）**：`ops/160/propose-change.sh:12–15`
- **判据**：GEN-002/004/008/009/011/013/018
- **分类**：技术性
- **依据**：④ 真实 Git + stub npm/gh
- **现行证据**：脏树 exit 1「未提交改动」，无 npm、无远端 ref；干净路径 npm 后 file-remote 出现 `feature/p`。
- **缺陷仍成立？**：**否**。

### M007-004（历史已修出队）

- **位置（现行）**：`ops/160/merge-pr.sh:44–54`（已无 `2>/dev/null || true`）
- **判据**：GEN-009/002/011/018
- **分类**：技术性
- **依据**：④ stub gh exit 2 + 标记 `AUDIT_QUERY_AUTH_FAILURE`
- **现行证据**：stderr 含原错误；`queries` 1 行；无 sleep；无 merge。因 `set -euo pipefail`，进程退出码为 **gh 的 2** 而非脚本 `exit 1`。回归断言 `status != 0` 而非 `== 1`。空列表且 gh 0 仍走 30 次等待（本席第 3 次才给出 id）。
- **缺陷仍成立？**：**否**。退出码 2 vs 1 **不是产品缺陷**，也**不藏失败**：harness 若期望 `==1` 会红，产品路径仍非 0 且不合。不新开 GR。

### M007-005（历史仅登记）

- **位置**：`merge-pr.sh:35–39`；`pre-push:8–14`；hook 头「非安全边界」
- **判据**：GEN-002/004/005/007/009/011/017/018
- **分类**：技术性
- **依据**：④ SKIP_CI_WAIT 路径打警告、不查 run、仍核 SHA 后 merge；ALLOW_MAIN_PUSH 只认 `1`
- **重判**：原因留痕仍是开发者提示，非业务审批。显式覆盖保持。`--no-verify` 可绕已声明。
- **缺陷成立？**：**否**。保持仅登记，不改开关。

### M007-006（历史仅登记）

- **位置**：`ops/ci/predeploy-gates.yml` 全文；对照 `.github/workflows/predeploy-gates.yml`
- **判据**：GEN-001/002/013/016/017/018
- **分类**：技术性
- **依据**：④ `cmp` 0；yaml parse；GET workflow active；③+④ 本票 `gh api repos/actions/checkout/git/ref/tags/v4.3.1` object_sha＝`34e114876b0b11c390a56381ad16ebd13914f8d5`；setup-node `v4.4.0`＝`49933ea5288caeca8642d1e84afbd3f7d6820020`
- **重判**：官方 tag 与两 SHA **相等**，仍不编缺陷。双份是机检约束，删一份会红。不声称生产准入/全仓绿。
- **缺陷成立？**：**否**。保持仅登记。

### M007-007（历史仅登记）

- **位置**：FILES 无测试；直接回归 `tools/gate-hardening-tests/m007-regression.mjs:1–102`
- **判据**：GEN-003/018
- **分类**：技术性
- **依据**：④ 回归 exit 0、7 PASS；写入隔离已确认
- **重判**：关键行为已有能失败断言；文件归属仍非本 FILES。不另立测试工程、不扩 FILES。
- **缺陷成立？**：**否**。保持仅登记。

### M007-008（历史仅登记）

- **位置**：`install-git-hooks.sh:16` `installed pre-push hook ->`
- **判据**：GEN-015、PGM-066（L965 原文：后台/客人端/现场设备/安卓端）
- **分类**：技术性
- **依据**：① PGM-066 逐字
- **重判**：一句英文 CLI 不足以断言 Git 工具违背产品终端中文规则。无缺陷 ID 不顺手改中文。
- **缺陷成立？**：**否**。保持仅登记。

### M007-009（历史仅登记）

- **位置**：`merge-pr.sh:55–61` `for rid in $run_ids`
- **判据**：GEN-002/009
- **分类**：技术性
- **依据**：④ 两 id 第二失败 → 不合
- **重判**：AND 语义成立；「旧失败 run 仍出现在 --commit 列表」要组合条件。铁律 6 不新建过滤策略。
- **缺陷成立？**：**否**。保持仅登记。

### M007-010（历史仅登记）

- **位置**：`merge-pr.sh` 全文不读 `baseRefName`；`propose-change.sh:26` `--base main`
- **判据**：GEN-002
- **分类**：技术性
- **依据**：④ propose create 日志含 `--base main --head feature/p`
- **重判**：正道首跑不冲突。不增加 PR 基线门。
- **缺陷成立？**：**否**。保持仅登记。

### M007-011（历史仅登记）

- **位置**：`pre-push` 无 `set -euo pipefail`，异常时落到 L17 `exit 0`
- **判据**：GEN-009
- **分类**：技术性
- **依据**：源码阅读 + 正常 stdin 探针均按预期非 0/0；**非法含空格 ref** 字段错位 exit 0，属非正常 Git 输入（见 §4.3），不证明一次常规四字段会误放行
- **重判**：理论 I/O 兜底不加。铁律 6。
- **缺陷成立？**：**否**。保持仅登记。

### 观察（非 GR，不进队列）

- **OBS-1 运行时 hook 副本过期**：本工作区 `.git/hooks/pre-push` 存在、mode `0775`、997 字节、mtime 2026-07-05 23:19:28，内容 SHA256 `46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b`（整行匹配 `*" refs/heads/main "*` 旧逻辑），**不等于**现行源 `e38ccd33…`（922 字节）。`core.hooksPath` 未设置。源码修复与运行时安装分别据实记录。本轮禁止主仓安装 hook，**不宣称运行副本已更新**，也不把副本漂移写成 FILES 缺陷。
  **夹具事故（必须披露）**：本票第一轮 file-remote 安装误把相对路径 `.git/hooks` 解析到**主仓**，曾短暂把主仓副本覆盖为现行源字节。已用历史 blob `45966c01610c92bb624b921b8333314f08238fed` 精确恢复 SHA `46aabdf3…`、mode 775、mtime。恢复后复跑 file-remote（绝对 git dir）主仓副本保持 `46aabdf3…`。不以「曾短暂装上」宣称已发布。
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

**既有回归（先确认写入隔离）**

- 静态：`mkdtempSync(.scratch/m007-regression-*)`；`finally rmSync`；源中无主仓 `.git/hooks` 写入。
- 执行：`node tools/gate-hardening-tests/m007-regression.mjs` → **exit 0**；stdout 7 行 `PASS M007-001…004`；`.scratch` 无 `m007-regression-*` 残留；六路径 porcelain 仍空。

**只读远端（GET，无 create/merge/PUT/push）**

- `gh auth status`：登录 `jxh1979`，scope 含 `repo`/`workflow`
- `gh api repos/jxh1979/mall/actions/workflows/predeploy-gates.yml`：`state=active`，`path=.github/workflows/predeploy-gates.yml`，`name=predeploy-gates`，`id=307870467`
- `gh run list --workflow predeploy-gates.yml --limit 5 --json …`：均为 success；最新 push `35601416957` `headSha=ebb1eb99c610df330c07d24278798c2d3611299a`
- `gh api repos/actions/checkout/git/ref/tags/v4.3.1`：object_sha＝YAML checkout SHA，type=commit
- `gh api repos/actions/setup-node/git/ref/tags/v4.4.0`：object_sha＝YAML setup-node SHA，type=commit

**官方规范（本机 man / --help）**

- `man githooks` pre-push 四元组；删除时 local ref=`(delete)`、local sha=zeros
- `git help config` pathname：`~/` 展开 `$HOME`
- `gh pr merge --help`：`--match-head-commit SHA`、`--squash`/`--merge`/`--rebase`
- `gh run watch --help`：`--exit-status`、`--compact`
- `gh run list --help`：`--commit`、`--workflow`
- `npm help ci`：`ignore-scripts` 仍允许随后 `npm run`（不跑 pre/post）

**隔离实跑（事后非交付物）**

- `/tmp/m007-b5-r1-20260922T122330Z`：pre-push stdin 常规项 PASS；「local ref 含空格」got exit 0——**harness 非法 ref**，不记产品缺陷。install 四 dest + worktree-common PASS。propose 脏/干净/已有 PR/npm 红 PASS。merge：非法 method / main / dirty / 无远端 / SHA 不一致 / 无 PR / 查询 exit **2** / 空列表后合并 / watch 红 / AND / SKIP / rebase argv / 漂移 PASS。第一轮 file-remote 因相对 `.git` 装错目录出现「推 main 未被拦」——属夹具错误，见 OBS-1，**不写成产品 FAIL**。
- `/tmp/m007-b5-r1-pushfix`：绝对 `--absolute-git-dir` 安装源 hook 后：`git push origin main` exit 1；feature 0；`main:refs/heads/from-main` 0；`ALLOW_MAIN_PUSH=1` 0。事后主仓 hook 仍 `46aabdf3…`。
- `bash -n` 四脚本 0。两份 yml 字节相等。`REQUIRED_GATES` 两部署脚本各 15 名 = `verify:predeploy` 15 名且 DEFINED。

**明确没做**

- 业务仓 `git push`/`commit`/`merge`、`gh pr create`/`merge`、覆盖主仓 hook（除上述已恢复的夹具事故外）
- 全仓 `npm run verify:predeploy`、生产构建、部署、DB
- 读取本次 selfB5 / 任何 `*-self-raw.md`

范围外：部署脚本只读 `REQUIRED_GATES` 数组以核 L6；workflow 生效件对照属 M005 文件，本票只证镜像相等；机检在 M008。故障不顺手修。工作区另有 `criteria-lock.md`/`seed-dev-bars.sh` 脏文件，不在本 FILES，本席不改、不刷绿。

---

## 8. 独立性

- 未读 `M007-b5-r1-self-raw.md` 及任何本次 selfB5。未用同号旧 backend M007、未用历史审计/收口结论代替本轮命令与原文。旧 001–011 只作输入线索。不继承本文件 V0.01 PASS、不继承本轮 B1/B2 PASS。
- 调用者只读：`package.json` 的 `verify:predeploy` 字符串、`.github/workflows/predeploy-gates.yml`（对照镜像）、`tools/verify-delivery-gates.mjs` 290–308 行断言、`ops/160/complex-deploy-*.sh` 的 `REQUIRED_GATES`、回归 102 行。未把那些模块审一遍。
- 三份 HTML 只读原句。铁律 4 未触发。
- 五源与回归 SHA 与 V0.01 所记同字节，复用完整阅读并本票再读再判。

---

## 9. 步骤结论

覆盖：5/5 FILES（163 行）+ 直接回归 102 行，79/79 判据，三线均完整模块。新 GR 空集。旧 11 项均已独立重判：001–004 现行源不再呈现历史缺陷；005–011 仍仅登记。无添加项、无 HTML 冲突、无未答 questions。OBS-1 运行时 hook 旧副本已披露，**未安装、未刷绿**。harness 非法 ref 与查询 exit 2 **完整披露**，不写成产品 FAIL，也不伪称全部探针按错误期望通过。

锁行有效。声称均有类型 1/2/3/4 之一。

**PASS**

本票不是模块 PASS、不是发布 PASS。模块生命周期仍由调度写 ledger；B3 累计 1 不清零。跳过的新增 B3/B4 不能由本席补做。本结论也不把运行时旧钩子说成已发布。
