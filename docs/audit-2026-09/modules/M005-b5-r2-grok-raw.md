CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M005 B5-r2 Grok席乙全模块审计 V0.01（首次r2）

席位：Grok 乙。步骤：B5-r2。模块：M005 治理门禁与 CI 基线／配置与工程入口／第3段。计划 471 模块、本模块 3 文件 52 行、NORMAL。控制提示词 V0.17；runbook V0.02。ledger 仅用顶部两行与本模块当前行：锁基准见本报告第一行实算；`LOCK-AMEND-COUNT: 1`；`M005 … | IN_PROGRESS | 2 | 2 | - | 0 | B4-r2双PASS…进行B5-r2`。未读 findings/历史 raw/收口/索引。正本 `/home/james/complex-management` 分支 `main`，`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`。禁止修复/MCP/子代理/外发。本报告不表示全仓或发布门禁通过。

## 1 技能自核（V0.17 收窄，不派子代理、不加第四闸）

本席环境实列并实读：`using-superpowers`、`verification-before-completion`、`grilling`、`grill-me`（仅转发 grilling）、`codebase-design`、`diagnosing-bugs`、`ponytail-review`、`git-guardrails-claude-code`（存在；本体要写 hook 并问安装范围，本步不执行）、`i-have-adhd`（输出模式；本文件首行被 V0.17 锁哈希行覆盖）。`batch-grill-me`、`ubiquitous-language`：本机 mattpocock 树缺失，记缺口，未冒充调用。B5 禁止修复：`tdd`/`test-driven-development`/`ponytail`/`systematic-debugging` 本体不启用。`requesting-code-review`/`receiving-code-review` 会引入第三席或无 ID 改码，不用。`writing-for-agents` 用于收口文件，本席本步只写本 raw。

对抗式按 grilling 追问结构自问自答，事实自读，不等操作员。

## 2 范围与当前对象

`plan/modules.md` M005 FILES：`tsconfig.base.json`、`.github/CODEOWNERS`、`.github/workflows/predeploy-gates.yml`。

```
git ls-files -z -- tsconfig.base.json .github/CODEOWNERS .github/workflows/predeploy-gates.yml | xargs -0 cat | wc -l
→ 52
```

| 文件 | 行 | 字节 | SHA256 |
|---|---|---|---|
| tsconfig.base.json | 13 | 303 | 13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db |
| .github/CODEOWNERS | 12 | 743 | b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d |
| .github/workflows/predeploy-gates.yml | 27 | 1335 | 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d |

UTF-8、无 BOM、无 CRLF。工作区相对 HEAD：前两文件未改；两份 YAML（本模块 + 镜像 `ops/ci/predeploy-gates.yml`）各 4 行注释/版本差异，`cmp` 仍字节全等。审计对象是工作区当前字节（YAML 自身版本 V0.02），不是 HEAD V0.01 注释。

判据映射（锁定义区）：仅 GEN-001–018。独立实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → 第一行值。sidecar 同行。

## 3 第一性原理（三问 × 三文件）

### 3.1 tsconfig.base.json

1. **为何存在 / 删除影响**：给 `apps/web/tsconfig.json` 提供共享 `compilerOptions`。删除后 `extends: "../../tsconfig.base.json"` 解析失败，web `typecheck`/`build` 失去这 9 项。三份 HTML 无 TypeScript 编译器条目；答不上 HTML 上位 ≠ 该删，走 2.1 → 技术性。
2. **声称是否执行**：`tsc -p apps/web --showConfig` 合并出全部 9 项，且 `strict:true` 展开 `noImplicitAny`/`strictNullChecks` 等；`npx tsc -v` → `Version 5.9.3`。内存程序：`noUncheckedIndexedAccess` 开 → TS2322 `number | undefined`；关 → 空；仅 `strict:true` 不开该旗 → 空（旗非 strict 子集，必须单列）。`strict:true` → TS7006 隐式 any；关 → 空。`skipLibCheck` 开跳过坏 `.d.ts`，关 → TS2322+TS1039。`resolveJsonModule` 关 → TS2732。
3. **未验证前提**：未跑完整 `apps/web` 业务 `typecheck`（353 个纳入文件属后续模块）。`forceConsistentCasingInFileNames` 在 `useCaseSensitiveFileNames=true` 的 Linux 上未构造大小写折叠 FS 反例。本工作流 YAML 不调用 `tsc`。

**删除测试（codebase-design）**：删掉后复杂度出现在每个 web tsconfig，不是空转发。接口小（一份 JSON options），实现由 tsc 承担 → 深模块。

### 3.2 .github/CODEOWNERS

1. **为何存在**：把高风险工程路径的复核意图写给 GitHub。删除后 `/.github/` 等不再有 owners 记录；因文件自述当前不强制，删除不改变现网合并物理能力。无 HTML 上位 → 技术性。
2. **声称是否执行**：L2–3 自述「仅在受保护分支 + require review 启用时被 GitHub 自动强制；当前私有库免费版无法启用分支保护，故仅记录意图」。本席禁外发，未查 GitHub 分支保护 API。本地：9 条规则均可解析；`@jxh1979` 与 `origin=https://github.com/jxh1979/mall.git` 主体一致。前缀匹配：三文件中仅 `.github/*` 命中 L9；`tsconfig.base.json` 无 owner（文件未声称覆盖全仓）。`ops/160/hooks/pre-push` 不读 CODEOWNERS。
3. **未验证前提**：GitHub 是否已能在该私有库启用规则保护；`@jxh1979` 是否仍有写权限；CODEOWNERS 语法引擎与本席前缀匹配器对通配的完全等价性（本文件无 `*`/`**`，路径均根锚，歧义空）。

### 3.3 .github/workflows/predeploy-gates.yml

1. **为何存在**：在 GitHub 上对 `main` 的 PR/push 跑 `npm run verify:predeploy`，作可见信号。删除后 `ops/160/merge-pr.sh` 按文件名等 `predeploy-gates.yml` run，无 run 则 5 分钟后拒绝合并；`.160` 部署脚本仍按 `REQUIRED_GATES` 硬拦。无 HTML 上位 → 技术性。
2. **声称是否执行**：
   - `on.pull_request/push.branches: [main]`：yaml 包解析确认。
   - `fetch-depth: 0`（number 0）+ `HTML_GATES_BASE` 表达式：`verify-delivery-gates.mjs` 断言；`npm run verify:delivery-gates-docs` → `DELIVERY_GATES_DOCS_PASS` exit 0。
   - 取不到基线必须失败：`HTML_GATES_BASE=definitely-missing-ref-m005-b5` 与全 0 SHA 均 `verify:html-ledger-sync` exit 1，文案要求 checkout `fetch-depth: 0`。
   - 镜像：`cmp` exit 0，两文件 SHA 均为 `4c20c029…049d`。
   - 职责：L6「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main」与 `complex-deploy-api.sh`/`web.sh` 的 `REQUIRED_GATES` 循环、`pre-push` 只匹配 `refs/heads/main` 一致。`package.json` `verify:predeploy` 16 项与两部署脚本数组逐项相等。
   - 固定 SHA：`actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5`、`actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020`，均为 40 位小写 hex。
   - `npm ci --ignore-scripts`：本机 `npm ci --help`「If true, npm does not run scripts specified in package.json files」；随后 `npm run verify:predeploy` 仍会跑目标 script（help 原文：`npm run` 仍执行 intended script）。
3. **未验证前提**：未在 GitHub runner 实跑本 workflow；注释中 v4.3.1/v4.4.0 与 SHA 的标签对应未从官方仓库核对（禁外发）；`ubuntu-latest` 与 `node-version: "22"` 解析出的精确镜像/精确 Node 补丁版未知；`package.json` engines 为 `^22.13.0 \|\| >=24.0.0`，本机 Node v24.20.0，CI 的 `22` 是否 ≥22.13.0 未在 runner 证实。

## 4 顺从式：GEN-001–018

| ID | 适用与证据 | 结果 |
|---|---|---|
| GEN-001 | 9 个 TS 选项均有 tsc 官方描述且被 web extends 消费；CODEOWNERS 9 条路径均有对应受控文件/目录；YAML 4 步均被 docs 门禁或部署脚本消费。无空转发。`noUncheckedIndexedAccess` 非 strict 隐含（strict-only 诊断空）。 | 满足 |
| GEN-002 | JSON/YAML 可解析；showConfig 无 errors；docs 门禁绿；基线缺失红。`verify:predeploy` 本机 exit 1 停在 html-ledger-sync（工作区 `M docs/business-system-map-for-operations-v0.1.html`，台账未同步）——属下游 HTML/台账，不是本三文件缺陷，也不刷绿。 | 满足 |
| GEN-003 | 本模块无测试文件。配置/CI 按 2.3 类型 3/4：tsc 内存可失败断言 + 现有 `verify:delivery-gates-docs`。未新写测试。 | 满足（豁免+等价证据） |
| GEN-004 | `HTML_GATES_BASE` 来自 GitHub 事件上下文，作 git ref；非法 ref 失败闭合。非用户表单输入。 | 满足 |
| GEN-005 | CODEOWNERS 不是服务端授权；文件已写明未启用分支保护。pre-push 自称「非安全边界」。默认拒绝不由本三文件承担。 | 满足（声明边界） |
| GEN-006 | 无持久化写入。门禁失败不改生产（部署脚本先闸后打包，本文件只触发闸）。 | N/A 有据 |
| GEN-007 | 单 job、无 matrix。静态门禁可重跑。无并发写。 | N/A 有据 |
| GEN-008 | `module: ESNext` + `moduleResolution: Bundler` 与 web Vite/`tsc -b` 一致；workflow `name`/`filename` 与 `merge-pr.sh --workflow predeploy-gates.yml` 一致；16 闸列表三处相等。 | 满足 |
| GEN-009 | 基线不可用 → exit 1（非跳过）。`set -e` 在部署脚本。YAML 无 `continue-on-error`。 | 满足 |
| GEN-010 | 三文件无秘密。`--ignore-scripts` 降 postinstall。action 钉完整 SHA。CODEOWNERS 无 token。 | 满足 |
| GEN-011 | CI 日志即证据；docs 门禁比较两 YAML 字节。注释职责可对照 hook/部署脚本原文。 | 满足 |
| GEN-012 | `fetch-depth: 0` 为全历史，服务于 merge-base，代价与仓大小绑定，非无界循环。 | 满足 |
| GEN-013 | `npm ci` 要求 lock（`npm help ci` 原文）。node 22 / engines 见限制。镜像字节锁死。 | 满足（精确 Node 补丁见限制） |
| GEN-014 | 无迁移 SQL。L12 仅把 migration 目录标为复核意图，不定义迁移语义。 | N/A 有据 |
| GEN-015 | 无 UI。 | N/A 有据 |
| GEN-016 | 当前 L6 与 hook/部署脚本职责一致。L4「无需数据库」：抽查 `verify:html-ledger-sync`/`entry-anchors`/`pii-encryption`/`formal-security-baseline`/`production-readiness-evidence` 无 DB 连接 API；`verify:tenant-isolation`/`verify:test-db-isolation` 源码为静态扫描。未把其余下游脚本当本模块展开。 | 满足 |
| GEN-017 | 全文走冲突→细化→技术性→添加：无 HTML 明文冲突、无业务实体/枚举/价格/权限发明 → 全部技术性。见 §6。 | 满足 |
| GEN-018 | 证据为 tsc/npm 官方文案、命令输出、文件字节；不用历史 PASS 或另一席结论。 | 满足 |

## 5 对抗式（grilling 结构，自答）

- Q：CI `npm run verify:predeploy` 被抽空是否本 YAML 假绿？A：会。文件 L3–6 已把 CI 定为可见信号；硬拦在部署 `REQUIRED_GATES`（不解析 package.json 脚本串）。`verify:delivery-gates-docs` 校验 `run === npm run verify:predeploy` 与 html 三闸子串。首次正常跑的部署路径不依赖 YAML 当硬边界。不新加闸。
- Q：`github.event.before` 全 0 SHA？A：实跑全 0 → html-ledger-sync exit 1。失败闭合。
- Q：`[skip ci]` 跳过 workflow？A：merge-pr 等不到 run 则拒绝（消费方 M007）。本文件未声称自己是 required check。
- Q：CODEOWNERS 漏 `tsconfig.base.json` 是否缺陷？A：文件未声称全仓覆盖。铁律 6：首次正常跑不因缺 owner 失败。不加规则。
- Q：`skipLibCheck` 藏坏 .d.ts？A：官方语义即跳过；内存正负已证实。不为一般加固改选项。
- Q：YAML 注释「快子集」列举不全 16 项？A：括号是例示，实际入口是 `verify:predeploy` 全串，与部署数组相等。不构成假完成。
- ponytail-review：无可删死代码/多余抽象。`Lean already. Ship.` net: -0。

## 6 技术性逐条分类（2.1 顺序：冲突→细化→技术性→添加）

无冲突、无细化锚点、无添加。下列均为技术性，无业务发明（构建/CI/复核路由，无票务酒吧游乐等域名词作为规则），工程缺陷否。

**tsconfig.base.json**

| 行 | 内容 | 用途 |
|---|---|---|
| 1–13 | JSON 对象 | 可被 extends 的共享编译配置 |
| 2–12 | compilerOptions | 传给 tsc |
| 3 | target ES2022 | 官方：「Set the JavaScript language version for emitted JavaScript and include compatible library declarations.」枚举含 es2022 |
| 4 | module ESNext | 官方：「Specify what module code is generated.」 |
| 5 | moduleResolution Bundler | 官方：「Specify how TypeScript looks up a file from a given module specifier.」type 含 bundler；showConfig 与 Vite web 一致 |
| 6 | strict true | 官方：「Enable all strict type-checking options.」showConfig 展开 noImplicitAny 等；不含必须单列的 noUncheckedIndexedAccess |
| 7 | esModuleInterop true | 官方：向 CJS 互操作并启用 allowSyntheticDefaultImports；showConfig 出现该隐含项 |
| 8 | forceConsistentCasingInFileNames true | 官方：「Ensure that casing is correct in imports.」 |
| 9 | skipLibCheck true | 官方：「Skip type checking all .d.ts files.」正负诊断已跑 |
| 10 | resolveJsonModule true | 官方：「Enable importing .json files.」正负诊断已跑 |
| 11 | noUncheckedIndexedAccess true | 官方：「Add 'undefined' to a type when accessed using an index.」正负诊断已跑 |

**CODEOWNERS**

| 行 | 内容 | 用途 |
|---|---|---|
| 1 | 强制点文件日期注释 | 记录意图，非执行 |
| 2–3 | 启用前提与当前仅记录 | 防止把未保护仓库写成已强制；与 hook/merge 口径同向 |
| 4–5 | 两部署脚本 @jxh1979 | 复核路由 |
| 6 | /ops/160/hooks/ | 覆盖 pre-push 等 |
| 7 | propose-change.sh | 复核路由 |
| 8 | /ops/ci/ | 覆盖 YAML 镜像 |
| 9 | /.github/ | 覆盖本 workflow 与本 CODEOWNERS |
| 10 | /tools/ | 覆盖门禁实现 |
| 11 | /package.json | 覆盖脚本串 |
| 12 | db/migration/ | 复核路由，不定义迁移业务 |

**predeploy-gates.yml**

| 行 | 内容 | 用途 |
|---|---|---|
| 1 | 文件版本 V0.02 | 文档版本，非 Actions 字段 |
| 2 | name: predeploy-gates | workflow 显示名 |
| 3–6 | 职责/免费版/无 DB/可见信号 | 与 hook、部署脚本、docs 门禁对账 |
| 7–11 | on PR/push → main | 事件 |
| 12–15 | job predeploy / ubuntu-latest / steps | 单闸门 job |
| 16–18 | checkout@40hex + fetch-depth 0 | 全历史；docs 门禁断言 |
| 19–21 | setup-node@40hex + node 22 | CI Node 主版本 |
| 22–23 | npm ci --ignore-scripts | 冻结安装且不跑生命周期脚本 |
| 24–27 | verify:predeploy + HTML_GATES_BASE 表达式 | 业务校验入口与基线传播 |

## 7 验证命令（节选，exit 以实跑为准）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
→ b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  exit 0

cmp .github/workflows/predeploy-gates.yml ops/ci/predeploy-gates.yml
→ cmp_exit=0

npx tsc -p apps/web --showConfig
→ 合并 9 项 + jsx react-jsx + strict 家族；web.errors []

内存 DIAG INDEX_ON → TS2322；INDEX_OFF → []；strict-only 无 index 旗 → []
IMPLICIT_ANY strict on → TS7006；off → []
skipLibCheck false → TS2322+TS1039；true → []
resolveJson false → TS2732；true → []

npm run verify:delivery-gates-docs
→ DELIVERY_GATES_DOCS_PASS  exit 0

HTML_GATES_BASE=definitely-missing-ref-m005-b5 npm run verify:html-ledger-sync
→ FAIL 取不到基线  exit 1

HTML_GATES_BASE=0000000000000000000000000000000000000000 npm run verify:html-ledger-sync
→ FAIL 取不到基线  exit 1

npm run verify:predeploy
→ 在 verify:html-ledger-sync 红：工作区改了 BSM HTML、决策台账未同步；比对 origin/main@242f4872。exit 1
  不记作本模块缺陷，也不称为发布通过。
```

未写仓库临时文件；未安装依赖；未部署；未真实 push。`git status` 确认未产生 `bad.d.ts` 等诊断残留。

## 8 新 finding

空集。无 GR 临时号。无添加项、无未答 questions、无锁外判据。

## 9 验证限制（不得当成已证实）

- 未调用 GitHub API/Actions runner：分支保护、CODEOWNERS 强制、required check、workflow 实跑、`ubuntu-latest` 镜像哈希、`node-version: "22"` 精确补丁、action SHA 与注释标签 v4.3.1/v4.4.0 的官方对应。
- 未读 GitHub Docs 原文（禁外发）；CODEOWNERS 匹配用根锚前缀规则，本文件无通配。
- 未跑完整 web `typecheck`/构建；未在大小写不敏感 FS 上验证 casing。
- `verify:predeploy` 未跑完 16 闸（第一闸因工作区 HTML 脏退出）；其余闸对 DB/网络的需求未在本次全部执行中观察。
- `ts.getStrictOptionValue` 把任意缺省旗当成 strict 隐含，与真实 checker 不符；本席以诊断输出为准，不以该 API 为规范。

## 10 结论

**PASS**

本模块当前三文件相对冻结 GEN-001–018 无未定稿工程缺陷。这不是全仓通过，也不是发布门禁通过。
CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M005 B5-r2 Grok席乙全模块审计 V0.02（现锁独立重审）

- 报告自身版本：V0.02（追加；保留本文件既有 V0.01 全部 16689 字节，SHA256 `0bd729eebbd18bb266a9137f1170e367af347a2e023c93f047682004ef1b8271`，不覆盖、不改 V0.01 原文）
- 席位：Grok（乙）。步骤：B5-r2。模块：M005 治理门禁与 CI 基线／配置与工程入口／第3段
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02。本席不另扩流程
- 计划：`plan/modules.md` V0.04；标记 NORMAL；FILES 仅三路径；有效行 52
- 映射：`plan/criteria-lock.md` 定义区 L10–100 + 映射行 `M005 → [GEN-001 … GEN-018]`（18 条；无 HTML 派生 ID）
- 锁：本席 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` 写在本次报告第一行；sidecar `cat` 同值；ledger 顶部同值；`LOCK-AMEND-COUNT: 1`；锁版本 V0.14；判据总数 401。正文不再重复锁字段名
- ledger（只读顶部两行与本模块当前行，不写）：M005 `IN_PROGRESS`；B3 累计 **2**；方案复核已修 2；子代理失败重跑 0；PASS 证据栏仍指向本 B5-r2 两 raw。不把表备注里的旧「全量B5双PASS」当成本次结论
- findings 输入（只读，不写）：版本 V3.40；当前段「已双PASS定稿」；012/013/014 当前工程缺陷否、队列否；001—011 历史原范围保留；queue 空。跳新增 B3/B4
- 正本：`/home/james/complex-management` 分支 `main`。`HEAD=e456ba96c84185c8d39f18f548011daac48a6466`，`origin/main=ebb1eb99c610df330c07d24278798c2d3611299a`（ahead 49 / behind 54）。本席未 `git fetch`。三受审文件相对 HEAD porcelain 空；`git hash-object` = `HEAD:` = `origin/main:` blob
- 互盲：未读 `M005-b5-r2-self-raw.md`（本次 self B5）。不继承本文件 V0.01（当时锁 `b012328f…`）的 PASS，不继承旧 B5-r1，不继承模块收口 V0.02
- 源阅读：三文件 SHA 与本席 B1 V0.08 全等，故明确复用该次全文源阅读；本票命令、反例与 18 GEN / 52 行判断全部本轮独立重跑重判，**不继承 B1 PASS**
- 禁止：修复、改 HTML／源码／台账／锁／ledger／findings／收口、MCP、子代理、fetch、commit、push、安装、部署、worktree、触发 CI
- 本票结论：**PASS**（仅本模块当前 52 行／18 GEN 的 B5-r2 席乙判断，不是全仓或发布门禁通过）

---

## 0. §6 技能自核（本席实际环境）

开工前列目录并读 `SKILL.md`。用户本轮指令覆盖 skill 默认：无 MCP、无子代理、不等操作员、不问人、不写 findings/ledger/收口。

| 技能 | 路径核验 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工路由；用户禁子代理覆盖其默认 |
| `using-arc` | Arc `skills/using-arc/SKILL.md` | 仅路由；**未**调用 `arc:audit`（会写 §7 外产物） |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md` | 配置不在源码图内，直读三源；`graft grep HTML_GATES_BASE --in tools/` 只核 M008 消费点，不扩审 M007/M008 |
| `grilling` / `grill-me` | mattpocock `productivity/grilling`；grill-me 只转发 | 对抗式／第一性原理自问自答；不派子代理、不问操作员 |
| `codebase-design` | mattpocock `engineering/codebase-design` | 删除测试：base 是 web `extends` 的小接口；workflow 是 `verify:predeploy` 声明面；CODEOWNERS 是升级后才强制的评审路由 |
| `diagnosing-bugs` | mattpocock `engineering/diagnosing-bugs` | 只构造可红探针（非法 target／TS18048／零 SHA 基线／直推 main）；不加 instrumentation |
| `systematic-debugging` | superpowers | 只借读错误→复现；B5 **不改**生产代码 |
| `ponytail-review` | ponytail `4.9.0/skills/ponytail-review` | 找可删内容；不改文件 |
| `verification-before-completion` | superpowers | PASS/FAIL 前用本报告内本轮命令输出 |
| `writing-for-agents` | mattpocock | 本 raw 按下游可差集、可归因来写 |
| `tdd` / `test-driven-development` | 存在 | B5 禁止修复，未新增测试 |
| `i-have-adhd` | marketplace `skills/i-have-adhd/SKILL.md` | 输出形状；本报告第一行仍按闸 1 独占哈希 |
| `audit-context-building` | Trail of Bits 插件 | 用户禁子代理，**未**派 function-analyzer |

缺口（如实，不冒充已调用）：

- `batch-grill-me`：本环境技能目录**未找到**独立 SKILL.md
- `ubiquitous-language`：他处有目录（如 `~/.qwen/skills/ubiquitous-language`）；本节点未读全文、**未调用**（本模块 FILES 无词表文件）

明确不用：`brainstorming`／`writing-plans`（阶段 A）；`ponytail` 本体（B3）；`requesting-code-review`／`receiving-code-review`（会引入第三席）；`resolving-merge-conflicts`（无合并）；`git-guardrails-claude-code`（会写钩子）。未派子代理。未改三受审文件／镜像／HTML／findings／ledger／lock。未执行 `npm install`／真实 `npm ci`。计算与反例只在 `/tmp/m005-*`。三受审文件 inode/mtime 运行前后不变。

graft 本轮：`graft grep "HTML_GATES_BASE" --in tools/` 命中 `tools/verify-delivery-gates.mjs` L308、`tools/verify-html-ledger-sync.mjs` L30（默认 `"origin/main"`）、`tools/verify-html-param-tier.mjs` L123、以及 M001 回归夹具。只核本 YAML 如何被消费，**不扩审 M008**。

---

## 1. 输入哈希、行数、覆盖与复用声明

本席独立实算（未抄 sidecar／派发期望值）。

| 项 | 本轮实算 |
|---|---|
| 锁文件 | `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（与 sidecar、ledger 顶部、本席 B1 V0.08 首行全等） |
| `tsconfig.base.json` | 13 行／303 字节／SHA256 `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db`；LF、无 CRLF、无 BOM、以换行结束；inode `248256234` |
| `.github/CODEOWNERS` | 12 行／743 字节／SHA256 `b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d`；LF、无 CRLF、无 BOM；inode `10223657` |
| `.github/workflows/predeploy-gates.yml` | 27 行／1335 字节／SHA256 `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`；L1=`# 文件版本：V0.02`；inode `10223617` |
| 镜像 `ops/ci/predeploy-gates.yml`（只核消费，不审 M007） | `cmp` exit 0；同 1335／同 SHA；blob `3857c0f9b3ca98e4d2e8d94771e91be606e0e732` |
| `git ls-files -z -- <三路径> \| xargs -0 cat \| wc -l` | **52** |
| 三文件 blob | work = HEAD = origin/main：`60709a9c…` / `cf51fabd…` / `3857c0f9…` |
| 宿主 | node v24.21.0；npm 11.19.0；TypeScript 5.9.3；yaml 2.9.0；Linux x86_64。根 `packageManager=npm@10.9.8`、`engines.node=^22.13.0 \|\| >=24.0.0`（工具链差披露，不写成三文件缺陷） |
| 旧本文件前缀 | 16689 字节／`0bd729eebbd18bb266a9137f1170e367af347a2e023c93f047682004ef1b8271` |

**复用边界：** 锁 SHA 与三文件 SHA 均与本席 B1 V0.08 一致，故复用该次对 13+12+27 行原文的阅读，避免重读历史巨票。本 B5 的命令、GitHub 只读 API、反例、18 GEN 与 findings 重判全部本轮新跑。V0.01 正文不采信。

全行覆盖：tsconfig 1–13、CODEOWNERS 1–12、workflow 1–27 均进入三线，无抽样冒充整文件。

---

## 2. 第一性原理线（2.3 三问 × 三文件）

### 2.1 `tsconfig.base.json`

1. **为何必须存在；删除后哪条 HTML 做不到。** 仓内 `tsconfig*.json` 仅它与 `apps/web/tsconfig.json`；后者 L2 `"extends": "../../tsconfig.base.json"`。删除测试（codebase-design）：web 必须内联九项 `compilerOptions`，复杂性回到每个调用方，不是空转发。三份业务 HTML（`docs/business-enum-quick-reference-v0.1.html`、`docs/business-system-map-for-operations-v0.1.html`、`docs/platform-governance-map-for-operations-v0.1.html`）对 `tsconfig`／`CODEOWNERS`／`predeploy-gates`／`GitHub Actions` **零命中**（本轮 `rg -n -i`，NO_HITS）。答不上 HTML 上位 **≠ 该删**。四分类：**技术性**。

2. **声称是否执行。** `ts.parseConfigFileTextToJson` `jsonError=null`；顶层键闭集仅 `compilerOptions`，九选项。`node_modules/typescript/bin/tsc --showConfig -p apps/web/tsconfig.json` exit 0：`target es2022`／`module esnext`／`moduleResolution bundler`／`strict true`（展开 `strictNullChecks`／`noImplicitAny`）／`esModuleInterop true`（展开 `allowSyntheticDefaultImports`）／`forceConsistentCasingInFileNames true`／`skipLibCheck true`／`resolveJsonModule true`／`noUncheckedIndexedAccess true`；`files` **354** 全部 `./src/**`（32 `.ts` + 322 `.tsx`）。`tsc -b apps/web/tsconfig.json --dry` exit 0，文案 `A non-dry build would build project '…/apps/web/tsconfig.json'`。**不是** `tsc -b --noEmit` 业务编译通过。内存探针见 §4。`apps/web` script `typecheck=tsc -b --noEmit`。指纹列表 `tools/generate-runtime-acceptance-evidence.mjs` L23 含本文件名（M008 绑定，不扩审）。本 workflow YAML 不调用 `tsc`。

3. **未验证前提。** 未跑 apps/web 全量 `tsc -b --noEmit`／`vite build`。`forceConsistentCasingInFileNames` 在 Linux `useCaseSensitiveFileNames=true` 上未造双文件大小写冲突。单独 `tsc -p tsconfig.base.json` 会当根项目（B1 记 fileNames=372）；声明入口是 web 继承，本轮未再跑无 `--noEmit` 的根项目编译。

### 2.2 `.github/CODEOWNERS`

1. **为何必须存在。** 删除测试：九条强制点失去 owner 记录。官方启用条件是受保护分支 + Require review from Code Owners。本轮 `gh api …/branches/main/protection` 与 `…/rulesets` 均 HTTP 403「Upgrade to GitHub Pro or make this repository public」。当前不是运行时授权边界，是意图记录 + 升级后的接口。HTML 里的审批／留痕是业务单据，不是 Git 审阅。四分类：**技术性**。

2. **声称是否执行。** 仓库 `private: true`。`codeowners/errors` = `{"errors":[]}`。`users/jxh1979` type=User；collaborators permission=**admin**。九规则 `git ls-files` 均有命中（见 §8B）。大小写反例／lock／web `package.json`／`merge-pr.sh`／`tsconfig.base.json` 不命中。`ops/160/hooks/pre-push` 17 行不含 `CODEOWNERS`。文件 L2–3 与 403 事实一致，未把计划写成已强制。

3. **未验证前提。** 一旦可开 Require review from Code Owners，单一 `@jxh1979` 且其为 PR 作者时能否形成独立复核：官方 required 下 owner 不能批自己的 PR；第二 owner 属添加，用户明示不扩展配置，不立案。本席前缀匹配器对 `*`/`**` 的完全等价性：本文件无通配，歧义空。

### 2.3 `.github/workflows/predeploy-gates.yml`

1. **为何必须存在。** 删除测试：GitHub 上不再有名为 `predeploy-gates` 的可见检查（本轮 `actions/workflows`：path 该文件、`state: active`、id `307870467`）。`ops/160/merge-pr.sh` 按 `--workflow predeploy-gates.yml` 等 run，无 run 则约 5 分钟拒绝合并（消费方 M007，本席只核文件名，不审脚本其余）。免费私有库设不了 required check（保护 API 403），硬合并闸本就不在本文件。部署脚本仍按 `REQUIRED_GATES` 逐个 `npm run`。本文件用途：PR/push 到 main 的可见信号，并把 `fetch-depth: 0` 与 `HTML_GATES_BASE` 送到 `npm run verify:predeploy`。四分类：**技术性**。`verify:html-ledger-sync` 等对 HTML 解释权的机审是 **M008 工具 + M426 台账** 的下位实现，本 FILES 只声明调用。

2. **声称是否执行。**
   - YAML `parseDocument` errors/warnings 空；`on.pull_request/push.branches=[main]`；`fetch-depth` JSON number `0`；无 `continue-on-error`。
   - checkout SHA `34e114876b0b11c390a56381ad16ebd13914f8d5` = `gh api repos/actions/checkout/git/ref/tags/v4.3.1` object.sha；setup-node SHA `49933ea5288caeca8642d1e84afbd3f7d6820020` = tag `v4.4.0` object.sha。均为 40 位小写 hex。
   - `HTML_GATES_BASE` 字面量与 `tools/verify-delivery-gates.mjs` `eventBase` 全等。`npm run verify:delivery-gates-docs` → `DELIVERY_GATES_DOCS_PASS` exit 0。
   - 取不到基线必须失败：`HTML_GATES_BASE=0000…0`／`not-a-ref-m005-b5-r2`／`definitely-missing-ref-m005-b5` 均 `verify-html-ledger-sync` exit 1，文案要求 checkout `fetch-depth: 0`。`npm run verify:html-ledger-sync && echo SHOULD_NOT_PRINT` 前者红且无 SHOULD_NOT_PRINT。
   - L6「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main」：hook 17 行无 `npm`／`verify:predeploy`；三 probe exit 0／1／0；两部署脚本 `REQUIRED_GATES` 15 项与 `package.json` `verify:predeploy` 15 项逐名相等，失败 `exit 1`。
   - `npm ci --ignore-scripts`：`npm help ci`「If true, npm does not run scripts specified in package.json files」；「npm run will still run their intended script」。本机**未** `npm ci`。
   - 只读 CI：最新成功 run `35601416957` event=push、`head_sha=ebb1eb99…`（当前 `origin/main`）、日志 `node: v22.23.2`、`HTML_GATES_BASE: 40fcc8f1…`、展开 15 闸、`DELIVERY_GATES_DOCS_PASS`。失败 run `34865649772`：前步 success、`verify:predeploy (governance gates)` failure → job failure，Post setup-node skipped。**不得**把 origin 绿写成当前脏工作区绿。
   - `default_workflow_permissions: read`；`sha_pinning_required: false` 但本文件仍 pin。

3. **未验证前提。** 未在 runner 上触发本 workflow。未本机 `npm ci`。未跑 `verify:predeploy` 全 15 闸（第一闸已红且会滑出 M005）。`ubuntu-latest` 精确镜像未钉死。run 日志有 Node 20 deprecation warning，conclusion 仍 success，不把 warning 写成已红。本轮**未**再拉取 docs.github.com `workflow-syntax` 原文，故不把缺 `timeout-minutes` 写成已核「Default: 360」，也不立案为无上限（用户明示无配置功能扩展）。

---

## 3. 顺从式：GEN-001–018 逐项（锁定义区原文，本轮独立核对）

| ID | 适用 | 本轮证据（类型 3／4） | 本席 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | §2 删除测试。九选项均被 showConfig 继承，非空转。九条 CODEOWNERS 均有索引命中。workflow 四步均在 run `35601416957` 出现。ponytail：无可删业务层；不为假设场景补 `files:[]`／第二 owner／timeout／permissions | **满足** |
| GEN-002 可执行正确性 | 适用（配置：验证解析与声明入口，不强制全构建） | JSON/YAML 可解析；TS 正负诊断；docs 门禁绿；基线缺失红；L6 与 hook／部署循环一致。`verify:predeploy` 本机停在第一闸（PGM 台账，见 §7），属下游，不是本三文件缺陷 | **满足**（边界：非全构建） |
| GEN-003 测试有效性 | 部分适用 | 本模块无测试文件。用已装 typescript／yaml／tsc／gh／hook stdin 作可失败断言。不新造 TDD。不把 dry／help／CI 绿写成业务测试通过 | **满足**（配置豁免+等价证据） |
| GEN-004 信任边界校验 | 适用 | `HTML_GATES_BASE` 来自事件；消费者对 `0000…0`／非法 ref fail-closed。checkout 完整历史。workflow 本身不做字符串校验，失败交给 `git merge-base` | **满足**（fail-closed） |
| GEN-005 授权与最小权限 | 弱适用 | CODEOWNERS 当前因无 branch protection **不构成**强制授权。仓库 Actions 默认 token 只读。workflow 未提权。HTML 业务审批链不是本文件。pre-push 自称「非安全边界」 | **满足**（声明边界；无越权发明） |
| GEN-006 数据完整性 | 弱适用 | 无事务／金额／持久化写入。基线取不到则 html-ledger-sync 拒绝 PASS。部署脚本先闸后打包，本文件只触发闸 | **N/A 有据**／配置面满足 |
| GEN-007 并发与幂等 | 弱适用 | 单 job、无 matrix、无 `concurrency`。门禁脚本只读。不把缺 concurrency 写成缺陷。静态门禁可重跑 | **N/A 有据** |
| GEN-008 接口与模式一致性 | 适用 | live＝镜像字节。`HTML_GATES_BASE` 与 checker 期望字面量一致。`node-version: "22"` 与最近 CI v22.23.2 及 engines `^22.13.0` 相容。15 闸名与两份部署 `REQUIRED_GATES` 全等。tsconfig 选项名与 TS 5.9 解析器一致。`module: ESNext` + `moduleResolution: Bundler` 与 web Vite/`tsc -b` 一致。workflow `name`／filename 与 `merge-pr.sh --workflow predeploy-gates.yml` 一致 | **满足** |
| GEN-009 错误处理与恢复 | 适用 | 无 `continue-on-error`。零 SHA／非法 ref 本地 exit 1。`&&` 短路成立。历史 job `34865649772` 在 verify 步失败即失败。不吞错 | **满足** |
| GEN-010 安全与敏感信息 | 适用 | 三文件无秘密、无注入点。action 完整 SHA 且与官方 tag 对象一致；仓库 `sha_pinning_required=false` 但本文件仍 pin。`npm ci --ignore-scripts`。CODEOWNERS 无 `!`／`[]` | **满足** |
| GEN-011 可观测与审计证据 | 适用 | Actions 日志含 SHA、Node 版本、HTML_GATES_BASE、15 闸展开。CODEOWNERS errors API 可查。tsconfig 以 showConfig／探针为证据。不把 dry 写成已编译。不把 origin CI 绿写成脏工作区绿 | **满足** |
| GEN-012 资源与性能边界 | 适用 | `fetch-depth: 0` 为全历史，服务于 merge-base，代价与仓大小绑定，非无界循环。缺 `timeout-minutes`：本轮未拉官方 syntax 原文，不写成已核默认值，也不立案补字段。最近成功 run 约数十秒量级（12:45:44Z 起，verify 步 12:46:08Z 已见 warning），未见无界放大 | **满足**（已划未验证边界） |
| GEN-013 构建配置与依赖 | 主适用 | pin SHA 与 tag 对齐；Node 22 线 CI 实测 v22.23.2；typescript/yaml 已装可解析；`tsc --dry` 接受无 composite 的 web 工程。`npm ci` 要求 lock（`npm help ci` 原文）。本机未 npm ci。run 日志 Node 20 deprecation warning，conclusion 仍 success | **满足**（精确 Node 补丁见限制） |
| GEN-014 数据库迁移正确性 | 不适用执行 | 无 SQL。CODEOWNERS L12 只给迁移目录登记 owner，且当前不强制。tenant-isolation 头注释为静态读 CSV／Jdbc SQL；test-db-isolation 为测试源正则扫描，不连库。支持 L5「无需数据库」。不扩审 M008 其余逻辑 | **N/A 有据** |
| GEN-015 界面可用性与可访问性 | 不适用 | 无用户界面。注释中文。脚本／路径为系统内部标识符 | **N/A 有据** |
| GEN-016 文档可验证性 | 适用 | L2–5 与 protection 403、静态闸、Actions 记录相符。L6 **当前**可从 hook 17 行与部署 `REQUIRED_GATES` 复现（V0.02 已修，本轮核消费**不重修**）。CODEOWNERS L2–3 可复现。tsconfig 无虚假完成声明。默认 `verify-html-ledger-sync` 文案 `origin/main@88c7b465` 是工具 merge-base 显示，不是 `origin/main` 尖端 `ebb1eb99` 抄录（B1/B2 已澄清，本轮独立复现同一文案） | **满足** |
| GEN-017 业务来源分类 | 适用 | 见 §5。顺序冲突→细化→技术性→添加。无冲突、无添加。未因 HTML 沉默主张删除。未把补 CODEOWNERS 覆盖 tsconfig／补 permissions／timeout 写成业务需求 | **满足** |
| GEN-018 第一性原理证据 | 适用 | 结论落到 HTML 原文（零命中）、可执行产物（tsc/yaml/node 门禁/hook）、外部规范（GitHub API：tag object、protection 403、permissions read、CODEOWNERS errors=[]）、可复现命令。不引用旧 B5／惯例／席位意见当依据。本轮未拉 workflow-syntax 网页原文的事实已在 GEN-012 划界 | **满足** |

---

## 4. 对抗式（grilling 自问 + 原生反例）

席内问题与答案（不等操作员）：

1. 是否用旧 B5 V0.01／B1 PASS 代替本 B5？否。V0.01 锁哈希 `b012328f…` ≠ 现锁；本轮按当前字节重跑命令并重判 18 GEN 与 001—014。
2. 无效 TS target／未知选项是否被解析器拒绝？会。TS6046／TS5023。
3. `noUncheckedIndexedAccess` 会不会只写在 JSON 里不生效？会生效：`const b = a[0]; b.toUpperCase()` → **TS18048** `'b' is possibly 'undefined'`；关掉则 0 诊断。`const b: string = a[0]` → **TS2322** `string | undefined`。仅 `strict:true` 关掉该旗 → 0 诊断（旗非 strict 子集，必须单列）。
4. `strict` 是否空转？`const x: string = null` → TS2322；`string | null` 0 诊断；隐式 any → TS7006／关掉 strict 则 0。
5. CODEOWNERS 大小写反例是否误匹配？`OPS/ci/…`、`TOOLS/…`、`.GITHUB/…` 均不在索引且规则不命中。
6. `/package.json` 会不会吞掉 lock 或 workspace package.json？不会。二者 `matched=None`。
7. `HTML_GATES_BASE=0000…0` 或非法 ref 是否假绿？exit 1，「取不到基线」「无法判定不等于通过」。
8. 工作区改 PGM HTML 不改台账是否红？会。见 §7。这是闸按设计暴露，不是本 YAML 写错。
9. verify 失败是否让 job 失败？`34865649772`：verify 步 failure → job failure。
10. 现行 L6 是否仍把 pre-push 写成门禁承担者？否。当前句为部署脚本硬阻断 + pre-push 仅拦直推 main。三 probe 与部署循环支持该句。
11. 标签 SHA 是否漂移？本次 API 上 tag object.sha 与文件 pin 一致。
12. 缺 `permissions` 是否把 GITHUB_TOKEN 写成 write？仓库默认 workflow permissions = read。
13. 是否把 `tsc --dry`／help／origin CI 绿当业务编译／脏工作区通过？否。
14. 是否为刷绿改 HTML／CSV／台账？否。真实红按归属报告。
15. `skipLibCheck`／`resolveJsonModule` 是否空转？坏 `.d.ts` 开旗 0 诊断、关旗 TS2322+TS1039；JSON import 关旗 TS2732、开旗 0。

构造反例（`/tmp/m005-*` 与 hook stdin；正本 inode/mtime/SHA 不变）：

| 反例 | 工具 | 结果 |
|---|---|---|
| `target: NOT_A_REAL_TARGET` | `parseJsonConfigFileContent` | TS6046，列出含 `es2022` 的合法集合 |
| `notARealOption: true` | 同上 | TS5023 Unknown compiler option |
| `const x: string = null` + 继承选项 | 内存 program | TS2322 |
| `string \| null = null` | 内存 program | 0 诊断 |
| `a[0]` 赋给 `string` + `noUncheckedIndexedAccess` | 内存 program | TS2322 |
| `const b = a[0]; b.toUpperCase()` 开旗 | 内存 program | TS18048 |
| 同上关旗 | 内存 program | 0 诊断 |
| 仅 strict、关 index 旗 | 内存 program | 0 诊断（证明必须单列） |
| 隐式 any on/off | 内存 program | TS7006／空 |
| 坏 `bad.d.ts` skipLibCheck on/off | `/tmp/m005-libcheck` | 空／TS2322+TS1039 |
| `import data from "./data.json"` resolveJson on/off | `/tmp/m005-jsonmod` | 空／TS2732 |
| CODEOWNERS 大小写／lock／web package.json／merge-pr.sh／tsconfig.base.json | 规则匹配 | 不命中 |
| hook feature 分支 | `bash ops/160/hooks/pre-push` stdin | exit 0 |
| hook 直推 main | 同上 | exit 1，拒绝直推 |
| hook `ALLOW_MAIN_PUSH=1` | 同上 | exit 0，留痕放行；**未 git push** |
| `HTML_GATES_BASE` 零 SHA／非法 ref | `node tools/verify-html-ledger-sync.mjs` | exit 1 fail-closed |
| `verify:html-ledger-sync && echo SHOULD_NOT_PRINT` | npm | 前者 exit 1，无 SHOULD_NOT_PRINT |

ponytail-review：三文件无死代码、无第二套抽象、无多余依赖。不为假设加固加字段。`Lean already. Ship.` net: -0。

---

## 5. 四分类（冲突 → 细化 → 技术性 → 添加）

**冲突：** 无。三 HTML 无 tsconfig／CODEOWNERS／Actions 明文；三文件未发明票种、状态、价格、权限、删除审批。

**细化：** 无 HTML 上位条目落在本 52 行字段上。`verify:predeploy` 链中 html-ledger-sync／entry-anchors／html-param-tier 是解释权 HTML 的机审入口，**实现属 M008、台账属 M426、入口快照属 M435/M436**；本 YAML 只 `npm run` 调用。

**技术性：** 全部 52 行。编译选项、仓库评审路由、CI 工作流不是业务角色／枚举／审批。

**添加：** 无。`questions/` 无 M005 文件（目录 8 个条目，无 M005）。不把「应再覆盖 tsconfig.base.json」「应写 permissions/concurrency/timeout」当成现有业务添加项。

---

## 6. 全部 finding ID（独立重判，不继承 PASS）

findings V3.40 当前段与历史表只作输入命题。本席按**当前三文件**重新取证。无新 GR。

| ID | 当前实证 | 四分类 | 工程缺陷 | 修复队列 | 本席处置 |
|---|---|---|---|---|---|
| M005-001 | 旧计划 package.json engines 宽于构建依赖 | 技术性（历史） | 历史是 | 否 | **历史原范围保留**；不在本 3 文件；不改写成 CI／tsconfig 含义；不重开 |
| M005-002 | 旧计划 REAL_BACKEND_WEB_PORT 未校验 | 技术性（历史） | 历史是 | 否 | 同上 |
| M005-003 | 旧计划 15 条 live-evidence 默认套件跳过 | 技术性（历史） | 历史是 | 否 | 同上 |
| M005-004 | 旧计划 smoke:p0-ops 重复别名 | 技术性（历史） | 历史是 | 否 | 同上 |
| M005-005 | 旧计划不设 REAL_BACKEND_E2E 全 skip 却 exit 0 | 技术性（历史） | 历史是 | 否 | 同上 |
| M005-006 | 旧计划默认 E2E 绑定所有接口 | 技术性（历史） | 历史是 | 否 | 同上 |
| M005-007 | 旧计划包协议字段／工作区 | 技术性（历史） | 否 | 否 | 历史保留；不属于本次 52 行 |
| M005-008 | 旧计划 scripts 图 | 技术性（历史） | 否 | 否 | 同上 |
| M005-009 | 旧计划依赖及 packageManager | 技术性（历史） | 否 | 否 | 同上 |
| M005-010 | 旧计划 Playwright mock 配置（历史表） | 技术性（历史） | 否 | 否 | 同上 |
| M005-011 | 旧计划 Playwright real 配置（历史表） | 技术性（历史） | 否 | 否 | 同上 |
| M005-012 | 当前 tsconfig 九选项 + web 真实 extends；原生 354 输入、正负诊断 | 技术性 | **否** | **否** | **只登记**，无缺陷；不新分 ID |
| M005-013 | 当前九条 CODEOWNERS + 保护 403 + errors=[] + 主体 admin | 技术性 | **否** | **否** | **只登记**，无缺陷；不新分 ID |
| M005-014 | 两 YAML 现 V0.02；L6 与 hook／部署循环一致；镜像字节相等；docs 门禁 exit 0 | 技术性 | 历史是、**当前已修** | **否** | **核现代码／镜像消费，不重复修**；不把已修注释再当现行缺陷 |

漏并：无。本轮无新临时 finding。假设第二 owner／扩大 CODEOWNERS 覆盖 tsconfig／permissions／concurrency／timeout／base `files:[]`：**不立案**。

### GR 显式 PASS/FAIL

| GR | 本轮 |
|---|---|
| 本轮新临时 finding | **无** |
| GR-M005-B1-01（旧 L6 把 pre-push 写成门禁硬阻断） | **现行缺陷不成立**。源已 V0.02，L6 可从 hook 与 `REQUIRED_GATES` 复现。历史记录保留，不重开、不重修 |
| 假设补字段／补覆盖 | **不立案** |

---

## 7. 真实红按归属（不刷绿、不升本模块缺陷）

| 观察 | 命令／出口 | 归属 | 与 M005 |
|---|---|---|---|
| PGM HTML 工作区已改、决策台账未同步 | `npm run verify:html-ledger-sync` **exit 1**；范围「工作区↔基线 origin/main@88c7b465」；已改 `docs/platform-governance-map-for-operations-v0.1.html` | 台账 **M426**；工具 **M008** | 第一闸按设计红；YAML 无 `continue-on-error`。**不改 HTML／台账刷绿**。`88c7b465` 是 merge-base 显示，不是 `origin/main` 尖端 `ebb1eb99` |
| 入口文档快照漂移 | `node tools/refresh-entry-doc-baseline.mjs --check` **exit 1**（`--check` 在 writeFile 之前 exit，未写业务资料）：`00-current-effective-document-entry-v0.1.md + README.md`，应为 `732 ops / V431（399 迁移） / 147 tests` | **M435／M436** | **不在** `verify:predeploy` 15 闸内；按指令另跑观察。不升 M005 缺陷 |
| `npm run verify:delivery-gates-docs` | `DELIVERY_GATES_DOCS_PASS` **exit 0** | M008 消费本 YAML | 镜像／fetch-depth／HTML_GATES_BASE／run 断言通过 |
| origin/main CI `35601416957` success | 只读 run／log 摘录 | 该 commit 工作区当时相对基线可过第一闸 | **不得**冒称当前脏工作区也会绿 |

`verify:predeploy` 15 闸（本轮独立计数）：html-ledger-sync、entry-anchors、html-param-tier、delivery-gates-docs、full-admin-matrix、domain-ownership-scope、matrix-vs-migration、data-isolation-spec、tenant-isolation、p0-contract、security-baseline、production-readiness-evidence、production-blockers、pii-encryption、test-db-isolation。与 api／web 部署 `REQUIRED_GATES` 全等（`api==web==pkg True`）。本席**未**跑全链（第一闸已红且会滑出 M005）。

---

## 8. 52 行连续覆盖

### A. `tsconfig.base.json`（13 行）

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | `{` | JSON 开。`parseConfigFileTextToJson` `jsonError=null`。顶层键闭集仅 `compilerOptions` |
| 2 | `"compilerOptions": {` | 仅此一块；无 `files`/`include`/`exclude`/`references`。声明入口不是 `tsc -p tsconfig.base.json` |
| 3 | `"target": "ES2022"` | showConfig `target: es2022`；`ScriptTarget.ES2022 === 9`。负例非法 target → TS6046 |
| 4 | `"module": "ESNext"` | showConfig `module: esnext`；converted `module=99` |
| 5 | `"moduleResolution": "Bundler"` | showConfig `moduleResolution: bundler`；converted `100`；web 消费者同值覆盖 |
| 6 | `"strict": true` | showConfig 展开 `strictNullChecks`／`noImplicitAny`。探针 TS2322／TS7006 |
| 7 | `"esModuleInterop": true` | showConfig 同时 `allowSyntheticDefaultImports: true` |
| 8 | `"forceConsistentCasingInFileNames": true` | showConfig true。未另造双文件大小写冲突（未验证边界） |
| 9 | `"skipLibCheck": true` | showConfig true。`/tmp` 坏 d.ts：开旗 0 诊断，关旗 TS2322+TS1039 |
| 10 | `"resolveJsonModule": true` | showConfig true。`/tmp` JSON import：开旗 0，关旗 TS2732 |
| 11 | `"noUncheckedIndexedAccess": true` | TS18048／TS2322；关掉则 0；仅 strict 不开该旗则 0 |
| 12 | `}` | compilerOptions 闭合。选项计数 **9** |
| 13 | `}` | 文件闭合，LF 结尾 |

消费者：`apps/web/tsconfig.json` extends + 覆盖 jsx／module／moduleResolution／types；`include: src/**/*.ts(x)`。`tsc --showConfig` files=**354** 全 `./src`。`tsc -b --dry` exit 0。仓内无第三份 tsconfig（排除 node_modules／graft／build）。

### B. `.github/CODEOWNERS`（12 行）

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | 强制点文件(2026-07-06):改动应经独立复核 | 意图。九条规则即清单，不是 `*` 全仓 owner |
| 2 | 仅在受保护分支 + require review 启用时被 GitHub 自动强制 | 本轮 protection **403** Upgrade to GitHub Pro |
| 3 | 当前私有库免费版无法启用分支保护，仅记录意图 + 一旦升级即生效 | `private: true`；rulesets **403**。注释与事实一致 |
| 4 | `/ops/160/complex-deploy-api.sh @jxh1979` | ls-files 命中 1 |
| 5 | `/ops/160/complex-deploy-web.sh @jxh1979` | 命中 1 |
| 6 | `/ops/160/hooks/ @jxh1979` | 命中 1（`pre-push`）。`merge-pr.sh`／`install-git-hooks.sh` 不匹配 |
| 7 | `/ops/160/propose-change.sh @jxh1979` | 命中 1 |
| 8 | `/ops/ci/ @jxh1979` | 命中 1（镜像 YAML） |
| 9 | `/.github/ @jxh1979` | 命中 2：本文件 + workflow。`git ls-files .github` 恰这两份 |
| 10 | `/tools/ @jxh1979` | 命中 91＝`git ls-files tools/` |
| 11 | `/package.json @jxh1979` | 命中 1。lock／`apps/web/package.json` 不匹配 |
| 12 | `/backend/src/main/resources/db/migration/ @jxh1979` | 命中 395＝该目录 ls-files |

九规则 owner 均为 `@jxh1979`（User／admin）。`codeowners/errors=[]`。无 `!`／`[]`。无第二份 CODEOWNERS。last-wins 合计 **494**，未匹配 **3281**／总 **3775**。`tsconfig.base.json` 无规则——文件自列强制点外；补规则属添加，不立案。

### C. `.github/workflows/predeploy-gates.yml`（27 行）

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | `# 文件版本：V0.02` | 注释。镜像同句。相对历史 V0.01 已递增 |
| 2 | `name: predeploy-gates` | AST `name`；GitHub workflow `state: active` 同名 id `307870467` |
| 3–4 | GitHub 侧独立治理门禁；PR/push 到 main 静态跑 delivery-gates 快子集 | `on.pull_request/push.branches:[main]`。实际 `run` 是 `npm run verify:predeploy`（15 闸），括号是主题枚举不是命令本身 |
| 5 | 无需数据库；私有库免费版无法设 required check；可见信号 + 独立复现 | tenant-isolation／test-db-isolation 为静态读文件／测试源，不连库。protection 403 支持「无法设 required check」 |
| 6 | 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main | **与 hook／REQUIRED_GATES 一致**。历史错误句已不在当前源。**不重修 014** |
| 7–11 | `on:` PR／push `main` | YAML 解析确认。无 `workflow_dispatch`／`paths`／`types` |
| 12–15 | `jobs.predeploy.runs-on: ubuntu-latest` + `steps` | 无 permissions／concurrency／timeout-minutes。默认 token read。不立案补字段。`ubuntu-latest` 浮动未验证 |
| 16–18 | checkout `@34e114876b0b11c390a56381ad16ebd13914f8d5` 注释 v4.3.1；`fetch-depth: 0` | tag v4.3.1 object.sha **即该值**。40 位 hex。checker 要求 fetch-depth 0（JSON number 0） |
| 19–21 | setup-node `@49933ea5288caeca8642d1e84afbd3f7d6820020` 注释 v4.4.0；`node-version: "22"` | tag v4.4.0 object.sha **即该值**。最近 run `node: v22.23.2` |
| 22–23 | `npm ci --ignore-scripts` | 同成功 run 步骤 success。本机未 npm ci。help 原文见 §2.3 |
| 24–27 | `HTML_GATES_BASE` 表达式 + `npm run verify:predeploy` | 与 checker `eventBase` 逐字相等。push run 落到 `github.event.before`=`40fcc8f1…`。无 continue-on-error。nSteps=4 |

镜像：`ops/ci/predeploy-gates.yml` 原始字节＝live。checker L292–308：LF 归一后比较、fetch-depth、run、HTML_GATES_BASE。`verify:delivery-gates-docs` exit 0。只追该直接依赖，不审 M007 其余脚本、不关闭 M007。

---

## 9. 未验证边界（诚实限制）

- 未跑 apps/web 全量 `tsc -b --noEmit`／`vite build`／真实 `npm ci`／`verify:predeploy` 全 15 闸／未触发 CI
- 未构造 `forceConsistentCasingInFileNames` 双文件冲突
- `ubuntu-latest` 镜像浮动；run 日志 Node 20 deprecation warning（checkout／setup-node 仍 success）
- 本轮未再拉取 docs.github.com workflow-syntax 原文，故不把缺 timeout 写成已核默认 360
- CODEOWNERS 一旦可开独立复核、单一 owner 自批：未在可启用保护的仓库上实证
- `ALLOW_MAIN_PUSH=1` 绕过 pre-push 后 GitHub 无 required check：已知免费私有库限制，L5 已写「可见信号」
- 工作区 HEAD 与 origin/main 分叉（ahead 49／behind 54）；**三受审文件**两侧 blob 仍全等。未 fetch
- 未把其余 12 个下游 `verify:*` 脚本当本模块展开

---

## 10. 命令与真实出口（本轮新跑）

下列命令只读或只写 `/tmp`：sha256sum／git hash-object／gh GET／tsc --showConfig|--dry／内存探针／hook stdin／`--check` 门禁。未 `git push`、未 `workflow_dispatch`、未改 settings、未 npm ci、未写 findings／ledger／三源。三受审文件 inode 运行前后相同（248256234／10223657／10223617）。

| 命令 | 出口／要点 |
|---|---|
| `sha256sum` 三文件＋锁 | tsconfig `13d49b94…`；CODEOWNERS `b8b7a9f1…`；YAML `4c20c029…`；锁＝sidecar＝ledger 首行 `b0863b95…` |
| `git ls-files -z -- <三路径> \| xargs -0 cat \| wc -l` | **52** |
| `git status --porcelain --` 三文件 | 空 |
| `git rev-list --left-right --count HEAD...origin/main` | 49／54 |
| `cmp` 两 YAML | exit 0 |
| `ts.parseConfigFileTextToJson` | jsonError null；9 选项 |
| `tsc --showConfig -p apps/web/tsconfig.json` | exit 0；es2022／esnext／bundler／strict／noUncheckedIndexedAccess 等；files 354 全 `./src` |
| `tsc -b apps/web/tsconfig.json --dry` | exit 0；non-dry would build |
| 内存／`/tmp` TS 探针 | TS2322／TS18048／TS7006／TS6046／TS5023／TS2732／TS1039 见 §4 |
| `yaml.parseDocument` | errors/warnings 空；fetch-depth type number 0 |
| `gh api …/codeowners/errors` | `{"errors":[]}` |
| `gh api …/branches/main/protection` | HTTP 403 Upgrade to GitHub Pro |
| `gh api …/actions/permissions/workflow` | `default_workflow_permissions: read` |
| `gh api repos/actions/checkout/git/ref/tags/v4.3.1` | object.sha=`34e11487…` |
| `gh api repos/actions/setup-node/git/ref/tags/v4.4.0` | object.sha=`49933ea5…` |
| hook 三 probe | 0／1／0 |
| `node tools/verify-html-ledger-sync.mjs` | **exit 1**（PGM／台账，M426；文案 origin/main@88c7b465） |
| 零 SHA／非法 ref | **exit 1** fail-closed |
| `npm run verify:html-ledger-sync && echo SHOULD_NOT_PRINT` | 无 SHOULD_NOT_PRINT；CHAIN_EXIT=1 |
| `npm run verify:delivery-gates-docs` | `DELIVERY_GATES_DOCS_PASS` **exit 0** |
| `node tools/refresh-entry-doc-baseline.mjs --check` | **exit 1** DRIFT（M435／M436；未写文件） |
| 只读 run `35601416957` | success；node v22.23.2；HTML_GATES_BASE=`40fcc8f1…`；15 闸展开 |
| 只读 run `34865649772` | verify 步 failure → job failure |
| 三 HTML `rg -i` | NO_HITS |
| `questions/` | 无 M005 文件 |

---

## 11. B5-r2 结论

**PASS**

52 行已连续覆盖，GEN-001–018 已逐项取证，三线已做。本轮无新未定稿工程缺陷。012／013 维持技术性无缺陷登记；014 现行源与镜像消费与职责说明一致，**不重复修**。历史 001—011 保留旧范围。全局 PGM 台账同步红与入口快照漂移按归属保留，不刷绿，不称全仓／发布通过。未全业务构建／安装／触发 CI。

本票不是模块收口（须两席 B5 双 PASS 且四值全等后由根写 ledger PASS 再写收口），不是 B2 定稿，不是发布 PASS。B3 累计 2 不清零。M001—M004 现锁闭环不重开。三份业务 HTML 未改。未代写 ledger／findings／收口。
