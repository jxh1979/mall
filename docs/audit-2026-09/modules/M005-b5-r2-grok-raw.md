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
