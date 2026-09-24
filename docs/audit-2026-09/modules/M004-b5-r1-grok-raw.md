CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M004 B5-r1 外席全量审计报告（Grok）

版本：V0.01  
模块：M004 `01-root-config-and-guidance-02` / 共以 `plan/modules.md` 实切为准  
步骤：B5-r1（修复后整模块重审，不是 diff 审）  
模块生命周期（ledger 只读状态行，不作收口写入）：`IN_PROGRESS`；B3 已执行 1 次；运行失败重跑 0  
本席结论：**PASS**

## 1. 角色、技能预检与独立性

本席 = Grok（审计席乙）。只审、不修源、不改方案、不派子代理、不新增判据/角色/闸/产物。审完本模块即退出。

技能自检（本机实际路径，缺则记缺口，不冒充已调用）：

- 已核存在并按收窄口径使用：`using-superpowers`、`grilling`/`grill-me`（只借追问结构，事实自读，不等操作员、不派子代理）、`codebase-design`、`diagnosing-bugs`、`systematic-debugging`（只读复现，不改生产代码）、`verification-before-completion`、`ponytail-review`（可删除性；本模块结论为 Lean already）、`writing-for-agents`（报告给下游席/适配器读）、`i-have-adhd`（输出形状）、`git-guardrails-claude-code`（可选，存在未当闸使用）。
- 缺口：`batch-grill-me` 不存在；`ubiquitous-language` 不存在。对抗式用 `grilling` 结构自问自答，不伪造这两项调用。
- 明确不用：subagent / worktree / ponytail-audit / ponytail-debt / research 等，与提示词 §6 一致。

独立性（本次实际遵守）：

- 未读取任何 `M004-b5-*-self-raw`、`M004-b4` raw、其他审计席本次新分析。
- 未读取先前 B1/B4 原报告正文。
- `findings.md` 只用当前六条 M004 队列表及 001–006 已定稿分类/命题/最小修复边界，以及用户给定的版本铁律口径。打开该文件时首段含 B4 接收过程说明，**不采信为当前证据**。
- `ledger.md` 只取顶部 `CRITERIA-LOCK-SHA256` / `LOCK-AMEND-COUNT: 0` 与 M004 行的状态、B3 次数、失败重跑次数；不读备注/接收说明。
- 搜索均显式列源路径：`package-lock.json`、`package.json`、`apps/web/package.json`、`.github/workflows/predeploy-gates.yml`、`ops/ci/predeploy-gates.yml`、三份业务 HTML、`plan/criteria-lock.md`、`plan/modules.md`、`plan/runbook.md`、`findings.md`、`ledger.md`、宿主 `/usr/lib/node_modules/npm/.../shrinkwrap.js`。未对全仓 `rg` 以免误命中 `docs/audit-2026-09` 里的 self。
- 未做当前工作区临时 tree、未做全量 `git diff`。B5 不是差异审。
- 允许只读引用根/`apps/web` 的 `package.json`、CI 消费方式、依赖源码与 npm 实现；不等于开始其他模块。
- 临时验证写在仓库 `.scratch/m004-b5-r1-grok-*`，审完已删除。审计命令 cwd 保持正本 `/home/james/complex-management`。未对主环境 `install`/`reify`，未改源。

## 2. 源与冻结输入（本席实算）

| 对象 | 实算 |
|---|---|
| `criteria-lock.md` SHA256 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar、ledger 顶部同值；COUNT=0；不抄派发预期，命令 `sha256sum docs/audit-2026-09/plan/criteria-lock.md`） |
| 模块映射 | `M004 → [GEN-001 … GEN-018]`（`plan/criteria-lock.md` 映射区） |
| 模块文件 | 仅 `package-lock.json`，`OVERSIZE-SINGLE-FILE`；`modules.md` 记 3320 行是切分时值，**当前实跑 3321 行 / 114242 字节** |
| 源 SHA256 | `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc` |
| 记录数 | packages 对象 220 = ROOT 1 + WEB 1 + LINK 1 + registry 217；registry 去重 name/version = 216（`@rolldown/pluginutils@1.0.1` 两处同版） |
| JSON | 原生解析成功；自定义扫描 **0 个重复键**；文件以换行结束 |
| findings | 当前 V0.46；六 ID 修复队列全否：001 nanoid、002 postcss、003 undici（已修命题）、004 格式根/web/link、005 图与平台/peer/engines/嵌套、006 完整性。只作已注册命题，B5 仍可发现新问题 |

顶层键实读：`name`、`version`（应用 semver `0.1.0`）、`lockfileVersion`（协议 `3`）、`documentVersion`（`V0.01`，非 npm 协议字段）、`requires`、`packages`。闭合：3320 `  }`、3321 `}`。

## 3. 第一性原理线

### 3.1 为什么必须存在

删掉 `package-lock.json`，三份业务 HTML **没有任何一条**做不到（HTML 里出现的「依赖」是游乐关键安全设备互相依赖，不是 npm）。但它是 **GEN-013 安装接口**：CI `.github/workflows/predeploy-gates.yml` 与 `ops/ci/predeploy-gates.yml` 均 `npm ci --ignore-scripts`，没有锁则无法复现依赖树。  
答不上 HTML 上位需求 **不等于该删**。按 2.1 顺序：与 HTML 无冲突 → 不是某条业务锚点的下位细化 → **无业务发明** → **技术性**。不因缺 HTML 锚点改判添加。

依据类型：1（HTML 无 npm 条目，对照设备「依赖」原文不是本文件）；4（CI 文件实读 + `npm ci --dry-run --ignore-scripts` exit 0）。

### 3.2 声称行为是否被执行验证

锁声称：lockfileVersion 3 工作区闭包可被 npm 解析、完整性可核、图可安装。

| 命令（cwd=正本） | 结果 |
|---|---|
| `npm ls --package-lock-only --ignore-scripts --depth=0` | exit 0；根 `entertainment-complex-management@0.1.0`，工作区 `apps-web@0.1.0 -> ./apps/web` |
| `npm ls --package-lock-only --ignore-scripts --all` | exit 0；仅 **UNMET OPTIONAL** 的 peer（`@vitest/ui`、`happy-dom` 等），与 `peerDependenciesMeta.optional=true` 一致 |
| `npm ci --dry-run --ignore-scripts` | exit 0；未 reify。输出中的 added/removed 是相对**当前磁盘 node_modules** 的 dry-run 对照，不是锁自相矛盾 |
| `npm audit --package-lock-only --ignore-scripts --json` | exit 0；high/critical/total = 0 |
| 同上 `--omit=dev` | exit 0；告警 0 |
| 自定义图闭包 | dependencies 398 边、optionalDependencies 28 边 **0 缺失**；peer 115 命中 + 29 缺失且 **29/29 均 optional meta**，必选 peer 缺失 = 0 |
| semver.satisfies（npm 自带 semver） | 426 条 dep+optional 边全部满足；`postcss→nanoid ^3.3.16` 锁 3.3.18；`vite→postcss ^8.5.15` 锁 8.5.23；`jsdom→undici ^7.25.0` 锁 7.29.0 |

宿主：node v24.20.0，npm 11.19.0。`package.json` 声明 `packageManager: npm@10.9.8`。两者 shrinkwrap 均 `this.data = { ...data }` 保留未知顶层键。

### 3.3 未经验证就当真的前提（本次实际拆掉）

1. **SRI 正确 ≠ 无漏洞。** 217 条 registry integrity 格式均为 `sha512-` + 64 字节；216 去重与官方 `dist.integrity`/`dist.tarball` 全等（1 条 `@rc-component/async-validator@5.1.0` 首次握手超时，重试匹配）。nanoid/postcss/undici **三个 tarball 实下重算 sha512 均匹配**。这只证明拿到的是声称的包，不证明包无缺陷。  
2. **dev 缺陷 ≠ 生产可利用。** 001–003 针对的库当前均 `dev: true`；`--omit=dev` audit 为 0。未声称线上跨用户可利用。  
3. **documentVersion 被 npm 拒绝。** 不成立。见 §6。  
4. **测试通过所以锁对。** 本模块没有测试文件；未把仓库其它测试绿当作本文件证据。  
5. **磁盘 node_modules 即锁。** dry-run 显示工作区 node_modules 与锁不完全同构；本席以锁文件 + `--package-lock-only` / dry-run 为准，不把脏 node_modules 写成锁缺陷。

## 4. 顺从式：GEN-001–018 逐项

每条均对照当前完整 3321 行，不是抽样。

- **GEN-001 必要性与最小性**：锁是 npm ci 的最小复现物。220 记录均在图中有位置（根/web/link 或被边引用/optional 平台包）。对抗式删 `node_modules/react` 后 `npm ls --package-lock-only` **exit 1 / ELSPROBLEMS missing react@^19.0.0**，证明记录不是死重量。未发现可删的额外包或重复抽象。ponytail-review：Lean already。
- **GEN-002 可执行正确性**：见 §3.2。声明的工作区、链接、registry 包可被 npm 解析。
- **GEN-003 测试有效性**：本模块源不是测试；没有把「测试绿」当证据。用可失败命令：删 react 必红、负 size nanoid 在 3.3.18 不再挂死。
- **GEN-004 信任边界**：resolved 217 条全 `https://registry.npmjs.org/`，无 http、无 yarn 镜像、无 file: 外链。integrity 全 sha512。SRI 匹配不能当无漏洞证明。
- **GEN-005 授权与最小权限**：锁不含角色/权限业务。`hasInstallScript` 仅 darwin `fsevents` 两处且 optional；CI 使用 `--ignore-scripts`。
- **GEN-006 数据完整性**：JSON 无重键；根/web 与对应 package.json 的 name/version/workspaces/engines/dependencies/devDependencies **全等**；link `resolved: apps/web` 且目录存在。失败不得留下半棵树由 npm ci 原子替换保证，本席未 reify。
- **GEN-007 并发与幂等**：锁本身无运行时并发逻辑；npm ci 对同一锁幂等。无缺陷。
- **GEN-008 接口与模式一致性**：根锁 name/version 与根清单一致；web 锁与 web 清单一致；`lockfileVersion` 3 与 npm 10/11 锁格式一致。`documentVersion` 不参与解析，不与 semver/lockfileVersion 抢义。
- **GEN-009 错误处理**：npm 在图缺失时显式 ELSPROBLEMS，不伪成功。当前完整锁 ls/ci dry-run 成功。
- **GEN-010 安全**：无明文秘密、无 token。audit 当前 0。三个已登记修补版本的库级反例见 §6，现行为符合修补。dev 缺陷未当成生产高危。
- **GEN-011 可观测**：integrity/resolved 即安装审计证据；本席命令输出可复现。
- **GEN-012 资源边界**：nanoid 3.3.18 `non-secure` 对 `size=-1` / `customAlphabet(...,0)` **1.5s 内返回空串**（`while (i-- > 0)`），不再无限循环。postcss 正常 `nanoid(6)` 长度 6。
- **GEN-013 构建配置与依赖**：CI `npm ci --ignore-scripts` + Node 22。锁与清单相容。26 个 dev 包 engines 比根 `>=22` 更严（`>=22.12`/`>=22.13`），**锁只是如实抄上游 engines**，改锁字段会撒谎；根 engines 过宽属 `package.json`（其他模块），不在本文件造 overrides。当前宿主 24.20.0，命令可跑。
- **GEN-014 数据库**：不适用。未查业务库，未把结构当 SRI。
- **GEN-015 界面**：不适用。无展示文案。
- **GEN-016 文档**：本文件不是说明文档。`documentVersion` 不冒充 npm 文档字段。
- **GEN-017 业务来源分类**：全部 220 记录 + 顶层/闭合按冲突→细化→技术性→添加判定，见 §8。无添加项。
- **GEN-018 第一性原理证据**：结论落到 HTML 原文、npm/Arborist 源码、可复现命令。未用「历史如此」「Grok 说过」「上一轮 PASS」。

## 5. 对抗式（grilling 结构，自问自答）

Q1. 缺 HTML 锚点是否就是添加？  
否。技术性判据是无业务发明 + 无冲突。全文无中文、无票务/酒吧/游乐/门禁等域词（对 `package-lock.json` 扫描 hits=0）。三份 HTML 的「依赖」是设备互依，不能当 npm 上位条目，也不能反过来把锁判成业务发明。

Q2. 图是否真闭合，还是解析器宽容？  
删 react 后必失败（exit 1，ELSPROBLEMS）。完整锁 dep/opt 0 缺失。29 个 unmet peer 全部 optional meta；必选 peer 0 缺失。

Q3. 可选平台包是否在 Linux CI 构成洞？  
`fsevents` 两处 `os: darwin` + optional；rolldown/lightningcss 各平台 optional+cpu/os。Linux 上 npm ls 仍 0。不扩平台、不删包。

Q4. 嵌套副本是否损坏完整性？  
4 处嵌套：`jest-dom` 下 `dom-accessibility-api@0.6.3`、`playwright` 下 `fsevents@2.3.2`、`pretty-format` 下 `react-is@17.0.2`、`rolldown` 下 `@rolldown/pluginutils@1.0.1`（与 `apps/web/node_modules/@rolldown/pluginutils@1.0.1` 同 SRI）。同 name/version 两份是路径隔离，不是脏重复。

Q5. documentVersion 是否破坏 npm 10.9.8 / 11.19.0？  
npm 11.19.0 Arborist `loadVirtual`→`toJSON`/`toString` **保留 `V0.01`**，packages 220，排序后 deepEqual 为真（键序上 documentVersion 会排到 requires 之后，因为 `swKeyOrder` 是优先列表不是拒绝闭集；`json-stringify-nice` 对其余键按字母排）。npm 10.9.8 官方 tarball SRI 核过，`shrinkwrap.js:490` 同样 `...data`。`npm ls` / `ci --dry-run` / `audit` 均接受当前文件。

Q6. 001–003 是否未修好或引入新洞？  
当前锁：nanoid 3.3.18、postcss 8.5.23（dependencies.nanoid `^3.3.16`）、undici 7.29.0。隔离 tarball：负 size 空串不挂；postcss.parse 绝对 `outside.map` **MAP_NULL / hasMarker=false**（未把合成 map 读进 AST）；undici `parseCacheControlHeader('private=')` 与 `'private, private=hdr'` 均 `{private:true}` 不抛 TypeError。audit 0。无新版本回退。

Q7. 生产 audit 安静是否掩盖 dev 问题？  
`--omit=dev` 与全量均为 0。即使将来 dev 再出公告，也要单独证明可利用路径，不能把 advisory 标签写成系统线上高危。

Q8. 锁能否当数据库/SRI 混用？  
不能。integrity 是包字节哈希，与业务库结构无关。本席未查 `complex`/`complex_test`。

## 6. 已登记六命题的当前核验（不是复制旧结论）

- **M004-001** 队列否。`node_modules/nanoid` 行 1930–1948 version 3.3.18，SRI 实下匹配。non-secure 负/零 size 空串。仍为 dev。无新缺陷。
- **M004-002** 队列否。`node_modules/postcss` 行 2050–2078 version 8.5.23，nanoid 约束 `^3.3.16`。绝对 map 反例未读入。仍为 dev。
- **M004-003** 队列否。`node_modules/undici` 行 3044–3053 version 7.29.0。空 private / 混合 private 不抛。仍为 dev，父 jsdom `^7.25.0` 满足。
- **M004-004** 队列否。顶层 1–7、根 8–21、web 22–44、link 1306–1309、闭合 3320–3321。根/web 与清单字段全等。`documentVersion` 必要性来自用户版本铁律（被改文件自身 Vx.xx），不能占用 `version`/`lockfileVersion`；兼容性本次用 npm 源码 + Arborist 往返 + ls/ci/audit 重做，不抄已过票。
- **M004-005** 队列否。220 记录图/optional/peer/engines/嵌套均核。不删包、不扩平台。
- **M004-006** 队列否。217 HTTPS + sha512 格式；216 去重官方元数据全等；3 个修补包 tarball 重哈希全等。**不把 SRI 当无漏洞证明。**

B5 未发现新的工程缺陷，无新临时 GR。

## 7. 四分类与类型 1/2/3/4

判定顺序：冲突 → 细化 → 技术性 → 添加。

- **冲突**：无。锁不改业务枚举/流程，与三份 HTML 无矛盾。
- **细化**：不能指到具体 HTML 锚点作为 npm 锁的上位需求（设备「依赖」不是它）。
- **技术性**：顶层、闭合、全部 220 记录。无业务发明理由见覆盖表。
- **添加**：无。不发 2.2 问询（B5 也禁止中途问人）。

依据类型：

- 类型 1：三份 HTML 无 npm/lockfile 条目；「依赖」原文属设备互依。
- 类型 2：隔离执行 nanoid/postcss/undici 修补后行为。
- 类型 3：npm 10.9.8 / 11.19.0 `shrinkwrap.js` 原文；lockfile v3 字段；`json-stringify-nice` 对未知键保留。
- 类型 4：sha256、npm ls/ci dry-run/audit、图闭包、semver、registry 元数据、删 react 反例。

## 8. 覆盖表：顶层 / 220 记录 / 闭合

顶层行 1–7：`name`/`version=0.1.0`/`lockfileVersion=3`/`documentVersion=V0.01`/`requires`/`packages` 开。技术性。  
闭合行 3320–3321：packages 对象与根对象结束。技术性。  
中间 3312 行 = 220 条 packages 记录，无间隙、无重叠（第一条 8–21，末条 yaml 3304–3319）。

| # | 行 | 类 | 路径 | 名称@version | 字段 | 2.1分类 | 无业务发明理由 |
|---|---|---|---|---|---|---|---|
| 1 | 8–21 | ROOT | `(root)` | entertainment-complex-management@0.1.0 | name, version, workspaces, devDependencies, engines | 技术性 | 根工作区安装清单镜像（name/version/workspaces/devDependencies/engines），对应根 package.json 的安装接口字段；无业务实体/枚举/价格/权限/流程。删掉则 npm ci 失去工作区根约束。 |
| 2 | 22–44 | WEB | `apps/web` | apps-web@0.1.0 | name, version, dependencies, devDependencies | 技术性 | apps/web 工作区清单镜像（name/version/dependencies/devDependencies），对应 apps/web/package.json；客人端构建依赖声明，不是业务规则发明。 |
| 3 | 45–51 | REG | `apps/web/node_modules/@rolldown/pluginutils` | @rolldown/pluginutils@1.0.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 4 | 52–77 | REG | `apps/web/node_modules/@vitejs/plugin-react` | @vitejs/plugin-react@6.0.2 | version, resolved, integrity, dev, license, dependencies, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 5 | 78–84 | REG | `node_modules/@adobe/css-tools` | @adobe/css-tools@4.5.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 6 | 85–93 | REG | `node_modules/@ant-design/colors` | @ant-design/colors@7.2.1 | version, resolved, integrity, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 7 | 94–112 | REG | `node_modules/@ant-design/cssinjs` | @ant-design/cssinjs@1.24.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 8 | 113–127 | REG | `node_modules/@ant-design/cssinjs-utils` | @ant-design/cssinjs-utils@1.1.3 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 9 | 128–139 | REG | `node_modules/@ant-design/fast-color` | @ant-design/fast-color@2.0.6 | version, resolved, integrity, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 10 | 140–159 | REG | `node_modules/@ant-design/icons` | @ant-design/icons@5.6.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 11 | 160–165 | REG | `node_modules/@ant-design/icons-svg` | @ant-design/icons-svg@4.4.2 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 12 | 166–181 | REG | `node_modules/@ant-design/react-slick` | @ant-design/react-slick@1.1.2 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 13 | 182–198 | REG | `node_modules/@asamuzakjp/css-color` | @asamuzakjp/css-color@5.1.11 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 14 | 199–215 | REG | `node_modules/@asamuzakjp/dom-selector` | @asamuzakjp/dom-selector@7.1.1 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 15 | 216–225 | REG | `node_modules/@asamuzakjp/generational-cache` | @asamuzakjp/generational-cache@1.0.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 16 | 226–232 | REG | `node_modules/@asamuzakjp/nwsapi` | @asamuzakjp/nwsapi@2.3.9 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 17 | 233–248 | REG | `node_modules/@babel/code-frame` | @babel/code-frame@7.29.7 | version, resolved, integrity, dev, license, peer, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 18 | 249–259 | REG | `node_modules/@babel/helper-validator-identifier` | @babel/helper-validator-identifier@7.29.7 | version, resolved, integrity, dev, license, peer, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 19 | 260–268 | REG | `node_modules/@babel/runtime` | @babel/runtime@7.29.7 | version, resolved, integrity, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 20 | 269–281 | REG | `node_modules/@bramus/specificity` | @bramus/specificity@2.4.2 | version, resolved, integrity, dev, license, dependencies, bin | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 21 | 282–301 | REG | `node_modules/@csstools/color-helpers` | @csstools/color-helpers@6.0.2 | version, resolved, integrity, dev, funding, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 22 | 302–325 | REG | `node_modules/@csstools/css-calc` | @csstools/css-calc@3.2.1 | version, resolved, integrity, dev, funding, license, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 23 | 326–353 | REG | `node_modules/@csstools/css-color-parser` | @csstools/css-color-parser@4.1.7 | version, resolved, integrity, dev, funding, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 24 | 354–376 | REG | `node_modules/@csstools/css-parser-algorithms` | @csstools/css-parser-algorithms@4.0.0 | version, resolved, integrity, dev, funding, license, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 25 | 377–401 | REG | `node_modules/@csstools/css-syntax-patches-for-csstree` | @csstools/css-syntax-patches-for-csstree@1.1.5 | version, resolved, integrity, dev, funding, license, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 26 | 402–421 | REG | `node_modules/@csstools/css-tokenizer` | @csstools/css-tokenizer@4.0.0 | version, resolved, integrity, dev, funding, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 27 | 422–433 | REG | `node_modules/@emnapi/core` | @emnapi/core@1.10.0 | version, resolved, integrity, dev, license, optional, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 28 | 434–444 | REG | `node_modules/@emnapi/runtime` | @emnapi/runtime@1.10.0 | version, resolved, integrity, dev, license, optional, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 29 | 445–455 | REG | `node_modules/@emnapi/wasi-threads` | @emnapi/wasi-threads@1.2.1 | version, resolved, integrity, dev, license, optional, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 30 | 456–461 | REG | `node_modules/@emotion/hash` | @emotion/hash@0.8.0 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 31 | 462–467 | REG | `node_modules/@emotion/unitless` | @emotion/unitless@0.7.5 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 32 | 468–485 | REG | `node_modules/@exodus/bytes` | @exodus/bytes@1.15.1 | version, resolved, integrity, dev, license, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 33 | 486–492 | REG | `node_modules/@jridgewell/sourcemap-codec` | @jridgewell/sourcemap-codec@1.5.5 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 34 | 493–511 | REG | `node_modules/@napi-rs/wasm-runtime` | @napi-rs/wasm-runtime@1.1.5 | version, resolved, integrity, dev, license, optional, dependencies, funding, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 35 | 512–521 | REG | `node_modules/@oxc-project/types` | @oxc-project/types@0.133.0 | version, resolved, integrity, dev, license, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 36 | 522–537 | REG | `node_modules/@playwright/test` | @playwright/test@1.61.0 | version, resolved, integrity, dev, license, dependencies, bin, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 37 | 538–549 | REG | `node_modules/@rc-component/async-validator` | @rc-component/async-validator@5.1.0 | version, resolved, integrity, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 38 | 550–565 | REG | `node_modules/@rc-component/color-picker` | @rc-component/color-picker@2.0.1 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 39 | 566–579 | REG | `node_modules/@rc-component/context` | @rc-component/context@1.4.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 40 | 580–591 | REG | `node_modules/@rc-component/mini-decimal` | @rc-component/mini-decimal@1.1.3 | version, resolved, integrity, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 41 | 592–609 | REG | `node_modules/@rc-component/mutate-observer` | @rc-component/mutate-observer@1.1.0 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 42 | 610–627 | REG | `node_modules/@rc-component/portal` | @rc-component/portal@1.1.2 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 43 | 628–643 | REG | `node_modules/@rc-component/qrcode` | @rc-component/qrcode@1.1.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 44 | 644–663 | REG | `node_modules/@rc-component/tour` | @rc-component/tour@1.15.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 45 | 664–684 | REG | `node_modules/@rc-component/trigger` | @rc-component/trigger@2.3.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 46 | 685–701 | REG | `node_modules/@rolldown/binding-android-arm64` | @rolldown/binding-android-arm64@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=android、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 47 | 702–718 | REG | `node_modules/@rolldown/binding-darwin-arm64` | @rolldown/binding-darwin-arm64@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 48 | 719–735 | REG | `node_modules/@rolldown/binding-darwin-x64` | @rolldown/binding-darwin-x64@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 49 | 736–752 | REG | `node_modules/@rolldown/binding-freebsd-x64` | @rolldown/binding-freebsd-x64@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=freebsd、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 50 | 753–769 | REG | `node_modules/@rolldown/binding-linux-arm-gnueabihf` | @rolldown/binding-linux-arm-gnueabihf@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 51 | 770–786 | REG | `node_modules/@rolldown/binding-linux-arm64-gnu` | @rolldown/binding-linux-arm64-gnu@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 52 | 787–803 | REG | `node_modules/@rolldown/binding-linux-arm64-musl` | @rolldown/binding-linux-arm64-musl@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 53 | 804–820 | REG | `node_modules/@rolldown/binding-linux-ppc64-gnu` | @rolldown/binding-linux-ppc64-gnu@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=ppc64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 54 | 821–837 | REG | `node_modules/@rolldown/binding-linux-s390x-gnu` | @rolldown/binding-linux-s390x-gnu@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=s390x。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 55 | 838–854 | REG | `node_modules/@rolldown/binding-linux-x64-gnu` | @rolldown/binding-linux-x64-gnu@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 56 | 855–871 | REG | `node_modules/@rolldown/binding-linux-x64-musl` | @rolldown/binding-linux-x64-musl@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 57 | 872–888 | REG | `node_modules/@rolldown/binding-openharmony-arm64` | @rolldown/binding-openharmony-arm64@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=openharmony、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 58 | 889–907 | REG | `node_modules/@rolldown/binding-wasm32-wasi` | @rolldown/binding-wasm32-wasi@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、cpu=wasm32。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 59 | 908–924 | REG | `node_modules/@rolldown/binding-win32-arm64-msvc` | @rolldown/binding-win32-arm64-msvc@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=win32、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 60 | 925–941 | REG | `node_modules/@rolldown/binding-win32-x64-msvc` | @rolldown/binding-win32-x64-msvc@1.0.3 | version, resolved, integrity, cpu, dev, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=win32、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 61 | 942–948 | REG | `node_modules/@standard-schema/spec` | @standard-schema/spec@1.1.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 62 | 949–969 | REG | `node_modules/@testing-library/dom` | @testing-library/dom@10.4.1 | version, resolved, integrity, dev, license, peer, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 63 | 970–989 | REG | `node_modules/@testing-library/jest-dom` | @testing-library/jest-dom@6.9.1 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 64 | 990–996 | REG | `node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api` | dom-accessibility-api@0.6.3 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；嵌套副本。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 65 | 997–1024 | REG | `node_modules/@testing-library/react` | @testing-library/react@16.3.2 | version, resolved, integrity, dev, license, dependencies, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 66 | 1025–1038 | REG | `node_modules/@testing-library/user-event` | @testing-library/user-event@14.6.1 | version, resolved, integrity, dev, license, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 67 | 1039–1049 | REG | `node_modules/@tybys/wasm-util` | @tybys/wasm-util@0.10.2 | version, resolved, integrity, dev, license, optional, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 68 | 1050–1057 | REG | `node_modules/@types/aria-query` | @types/aria-query@5.0.4 | version, resolved, integrity, dev, license, peer | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 69 | 1058–1068 | REG | `node_modules/@types/chai` | @types/chai@5.2.3 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 70 | 1069–1075 | REG | `node_modules/@types/deep-eql` | @types/deep-eql@4.0.2 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 71 | 1076–1082 | REG | `node_modules/@types/estree` | @types/estree@1.0.9 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 72 | 1083–1092 | REG | `node_modules/@types/react` | @types/react@19.2.16 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 73 | 1093–1102 | REG | `node_modules/@types/react-dom` | @types/react-dom@19.2.3 | version, resolved, integrity, dev, license, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 74 | 1103–1120 | REG | `node_modules/@vitest/expect` | @vitest/expect@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 75 | 1121–1147 | REG | `node_modules/@vitest/mocker` | @vitest/mocker@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 76 | 1148–1160 | REG | `node_modules/@vitest/pretty-format` | @vitest/pretty-format@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 77 | 1161–1174 | REG | `node_modules/@vitest/runner` | @vitest/runner@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 78 | 1175–1190 | REG | `node_modules/@vitest/snapshot` | @vitest/snapshot@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 79 | 1191–1200 | REG | `node_modules/@vitest/spy` | @vitest/spy@4.1.9 | version, resolved, integrity, dev, license, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 80 | 1201–1215 | REG | `node_modules/@vitest/utils` | @vitest/utils@4.1.9 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 81 | 1216–1226 | REG | `node_modules/ansi-regex` | ansi-regex@5.0.1 | version, resolved, integrity, dev, license, peer, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 82 | 1227–1240 | REG | `node_modules/ansi-styles` | ansi-styles@5.2.0 | version, resolved, integrity, dev, license, peer, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 83 | 1241–1305 | REG | `node_modules/antd` | antd@5.29.3 | version, resolved, integrity, license, dependencies, funding, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 84 | 1306–1309 | LINK | `node_modules/apps-web` | apps-web@link | resolved, link | 技术性 | workspace 链接 node_modules/apps-web → apps/web；npm 工作区解析接口，目标目录存在。 |
| 85 | 1310–1319 | REG | `node_modules/aria-query` | aria-query@5.3.0 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 86 | 1320–1329 | REG | `node_modules/assertion-error` | assertion-error@2.0.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 87 | 1330–1339 | REG | `node_modules/bidi-js` | bidi-js@1.0.3 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 88 | 1340–1349 | REG | `node_modules/chai` | chai@6.2.2 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 89 | 1350–1355 | REG | `node_modules/classnames` | classnames@2.5.1 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 90 | 1356–1361 | REG | `node_modules/compute-scroll-into-view` | compute-scroll-into-view@3.1.1 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 91 | 1362–1368 | REG | `node_modules/convert-source-map` | convert-source-map@2.0.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 92 | 1369–1377 | REG | `node_modules/copy-to-clipboard` | copy-to-clipboard@3.3.3 | version, resolved, integrity, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 93 | 1378–1391 | REG | `node_modules/css-tree` | css-tree@3.2.1 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 94 | 1392–1398 | REG | `node_modules/css.escape` | css.escape@1.5.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 95 | 1399–1404 | REG | `node_modules/csstype` | csstype@3.2.3 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 96 | 1405–1418 | REG | `node_modules/data-urls` | data-urls@7.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 97 | 1419–1424 | REG | `node_modules/dayjs` | dayjs@1.11.21 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 98 | 1425–1431 | REG | `node_modules/decimal.js` | decimal.js@10.6.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 99 | 1432–1441 | REG | `node_modules/dequal` | dequal@2.0.3 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 100 | 1442–1451 | REG | `node_modules/detect-libc` | detect-libc@2.1.2 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 101 | 1452–1459 | REG | `node_modules/dom-accessibility-api` | dom-accessibility-api@0.5.16 | version, resolved, integrity, dev, license, peer | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 102 | 1460–1472 | REG | `node_modules/entities` | entities@8.0.0 | version, resolved, integrity, dev, license, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 103 | 1473–1479 | REG | `node_modules/es-module-lexer` | es-module-lexer@2.1.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 104 | 1480–1489 | REG | `node_modules/estree-walker` | estree-walker@3.0.3 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 105 | 1490–1499 | REG | `node_modules/expect-type` | expect-type@1.3.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 106 | 1500–1517 | REG | `node_modules/fdir` | fdir@6.5.0 | version, resolved, integrity, dev, license, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 107 | 1518–1532 | REG | `node_modules/fsevents` | fsevents@2.3.3 | version, resolved, integrity, dev, hasInstallScript, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、hasInstallScript。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 108 | 1533–1545 | REG | `node_modules/html-encoding-sniffer` | html-encoding-sniffer@6.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 109 | 1546–1555 | REG | `node_modules/indent-string` | indent-string@4.0.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 110 | 1556–1562 | REG | `node_modules/is-potential-custom-element-name` | is-potential-custom-element-name@1.0.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 111 | 1563–1570 | REG | `node_modules/js-tokens` | js-tokens@4.0.0 | version, resolved, integrity, dev, license, peer | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 112 | 1571–1611 | REG | `node_modules/jsdom` | jsdom@29.1.1 | version, resolved, integrity, dev, license, dependencies, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 113 | 1612–1620 | REG | `node_modules/json2mq` | json2mq@0.2.0 | version, resolved, integrity, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 114 | 1621–1650 | REG | `node_modules/lightningcss` | lightningcss@1.32.0 | version, resolved, integrity, dev, license, dependencies, engines, funding, optionalDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 115 | 1651–1671 | REG | `node_modules/lightningcss-android-arm64` | lightningcss-android-arm64@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=android、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 116 | 1672–1692 | REG | `node_modules/lightningcss-darwin-arm64` | lightningcss-darwin-arm64@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 117 | 1693–1713 | REG | `node_modules/lightningcss-darwin-x64` | lightningcss-darwin-x64@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 118 | 1714–1734 | REG | `node_modules/lightningcss-freebsd-x64` | lightningcss-freebsd-x64@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=freebsd、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 119 | 1735–1755 | REG | `node_modules/lightningcss-linux-arm-gnueabihf` | lightningcss-linux-arm-gnueabihf@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 120 | 1756–1776 | REG | `node_modules/lightningcss-linux-arm64-gnu` | lightningcss-linux-arm64-gnu@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 121 | 1777–1797 | REG | `node_modules/lightningcss-linux-arm64-musl` | lightningcss-linux-arm64-musl@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 122 | 1798–1818 | REG | `node_modules/lightningcss-linux-x64-gnu` | lightningcss-linux-x64-gnu@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 123 | 1819–1839 | REG | `node_modules/lightningcss-linux-x64-musl` | lightningcss-linux-x64-musl@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=linux、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 124 | 1840–1860 | REG | `node_modules/lightningcss-win32-arm64-msvc` | lightningcss-win32-arm64-msvc@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=win32、cpu=arm64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 125 | 1861–1881 | REG | `node_modules/lightningcss-win32-x64-msvc` | lightningcss-win32-x64-msvc@1.32.0 | version, resolved, integrity, cpu, dev, license, optional, os, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=win32、cpu=x64。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 126 | 1882–1891 | REG | `node_modules/lru-cache` | lru-cache@11.5.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 127 | 1892–1902 | REG | `node_modules/lz-string` | lz-string@1.5.0 | version, resolved, integrity, dev, license, peer, bin | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 128 | 1903–1912 | REG | `node_modules/magic-string` | magic-string@0.30.21 | version, resolved, integrity, dev, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 129 | 1913–1919 | REG | `node_modules/mdn-data` | mdn-data@2.27.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 130 | 1920–1929 | REG | `node_modules/min-indent` | min-indent@1.0.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 131 | 1930–1948 | REG | `node_modules/nanoid` | nanoid@3.3.18 | version, resolved, integrity, dev, funding, license, bin, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 132 | 1949–1962 | REG | `node_modules/obug` | obug@2.1.3 | version, resolved, integrity, dev, funding, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 133 | 1963–1975 | REG | `node_modules/parse5` | parse5@8.0.1 | version, resolved, integrity, dev, license, dependencies, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 134 | 1976–1982 | REG | `node_modules/pathe` | pathe@2.0.3 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 135 | 1983–1989 | REG | `node_modules/picocolors` | picocolors@1.1.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 136 | 1990–2002 | REG | `node_modules/picomatch` | picomatch@4.0.4 | version, resolved, integrity, dev, license, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 137 | 2003–2021 | REG | `node_modules/playwright` | playwright@1.61.0 | version, resolved, integrity, dev, license, dependencies, bin, engines, optionalDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 138 | 2022–2034 | REG | `node_modules/playwright-core` | playwright-core@1.61.0 | version, resolved, integrity, dev, license, bin, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 139 | 2035–2049 | REG | `node_modules/playwright/node_modules/fsevents` | fsevents@2.3.2 | version, resolved, integrity, dev, hasInstallScript, license, optional, os, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional、os=darwin、嵌套副本、hasInstallScript。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 140 | 2050–2078 | REG | `node_modules/postcss` | postcss@8.5.23 | version, resolved, integrity, dev, funding, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 141 | 2079–2094 | REG | `node_modules/pretty-format` | pretty-format@27.5.1 | version, resolved, integrity, dev, license, peer, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 142 | 2095–2102 | REG | `node_modules/pretty-format/node_modules/react-is` | react-is@17.0.2 | version, resolved, integrity, dev, license, peer | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；peer、嵌套副本。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 143 | 2103–2112 | REG | `node_modules/punycode` | punycode@2.3.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 144 | 2113–2121 | REG | `node_modules/qrcode.react` | qrcode.react@4.2.0 | version, resolved, integrity, license, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 145 | 2122–2138 | REG | `node_modules/rc-cascader` | rc-cascader@3.34.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 146 | 2139–2153 | REG | `node_modules/rc-checkbox` | rc-checkbox@3.5.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 147 | 2154–2169 | REG | `node_modules/rc-collapse` | rc-collapse@3.9.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 148 | 2170–2186 | REG | `node_modules/rc-dialog` | rc-dialog@9.6.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 149 | 2187–2203 | REG | `node_modules/rc-drawer` | rc-drawer@7.3.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 150 | 2204–2219 | REG | `node_modules/rc-dropdown` | rc-dropdown@4.2.1 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 151 | 2220–2237 | REG | `node_modules/rc-field-form` | rc-field-form@2.7.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 152 | 2238–2255 | REG | `node_modules/rc-image` | rc-image@7.12.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 153 | 2256–2270 | REG | `node_modules/rc-input` | rc-input@1.8.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 154 | 2271–2287 | REG | `node_modules/rc-input-number` | rc-input-number@9.5.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 155 | 2288–2306 | REG | `node_modules/rc-mentions` | rc-mentions@2.20.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 156 | 2307–2324 | REG | `node_modules/rc-menu` | rc-menu@9.16.1 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 157 | 2325–2339 | REG | `node_modules/rc-motion` | rc-motion@2.9.5 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 158 | 2340–2358 | REG | `node_modules/rc-notification` | rc-notification@5.6.4 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 159 | 2359–2374 | REG | `node_modules/rc-overflow` | rc-overflow@1.5.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 160 | 2375–2389 | REG | `node_modules/rc-pagination` | rc-pagination@5.1.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 161 | 2390–2428 | REG | `node_modules/rc-picker` | rc-picker@4.11.3 | version, resolved, integrity, license, dependencies, engines, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 162 | 2429–2443 | REG | `node_modules/rc-progress` | rc-progress@4.0.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 163 | 2444–2461 | REG | `node_modules/rc-rate` | rc-rate@2.13.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 164 | 2462–2477 | REG | `node_modules/rc-resize-observer` | rc-resize-observer@1.4.3 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 165 | 2478–2493 | REG | `node_modules/rc-segmented` | rc-segmented@2.7.1 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 166 | 2494–2515 | REG | `node_modules/rc-select` | rc-select@14.16.8 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 167 | 2516–2533 | REG | `node_modules/rc-slider` | rc-slider@11.1.9 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 168 | 2534–2551 | REG | `node_modules/rc-steps` | rc-steps@6.0.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 169 | 2552–2566 | REG | `node_modules/rc-switch` | rc-switch@4.1.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 170 | 2567–2587 | REG | `node_modules/rc-table` | rc-table@7.54.0 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 171 | 2588–2609 | REG | `node_modules/rc-tabs` | rc-tabs@15.7.0 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 172 | 2610–2626 | REG | `node_modules/rc-textarea` | rc-textarea@1.10.2 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 173 | 2627–2642 | REG | `node_modules/rc-tooltip` | rc-tooltip@6.4.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 174 | 2643–2662 | REG | `node_modules/rc-tree` | rc-tree@5.13.1 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 175 | 2663–2679 | REG | `node_modules/rc-tree-select` | rc-tree-select@5.27.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 176 | 2680–2694 | REG | `node_modules/rc-upload` | rc-upload@4.11.0 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 177 | 2695–2708 | REG | `node_modules/rc-util` | rc-util@5.44.4 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 178 | 2709–2727 | REG | `node_modules/rc-virtual-list` | rc-virtual-list@3.19.2 | version, resolved, integrity, license, dependencies, engines, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 179 | 2728–2736 | REG | `node_modules/react` | react@19.2.7 | version, resolved, integrity, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 180 | 2737–2748 | REG | `node_modules/react-dom` | react-dom@19.2.7 | version, resolved, integrity, license, dependencies, peerDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 181 | 2749–2754 | REG | `node_modules/react-is` | react-is@18.3.1 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 182 | 2755–2768 | REG | `node_modules/redent` | redent@3.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 183 | 2769–2778 | REG | `node_modules/require-from-string` | require-from-string@2.0.2 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 184 | 2779–2784 | REG | `node_modules/resize-observer-polyfill` | resize-observer-polyfill@1.5.1 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 185 | 2785–2818 | REG | `node_modules/rolldown` | rolldown@1.0.3 | version, resolved, integrity, dev, license, dependencies, bin, engines, optionalDependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 186 | 2819–2825 | REG | `node_modules/rolldown/node_modules/@rolldown/pluginutils` | @rolldown/pluginutils@1.0.1 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；嵌套副本。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 187 | 2826–2838 | REG | `node_modules/saxes` | saxes@6.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 188 | 2839–2844 | REG | `node_modules/scheduler` | scheduler@0.27.0 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 189 | 2845–2853 | REG | `node_modules/scroll-into-view-if-needed` | scroll-into-view-if-needed@3.1.0 | version, resolved, integrity, license, dependencies | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 190 | 2854–2860 | REG | `node_modules/siginfo` | siginfo@2.0.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 191 | 2861–2870 | REG | `node_modules/source-map-js` | source-map-js@1.2.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 192 | 2871–2877 | REG | `node_modules/stackback` | stackback@0.0.2 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 193 | 2878–2884 | REG | `node_modules/std-env` | std-env@4.1.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 194 | 2885–2890 | REG | `node_modules/string-convert` | string-convert@0.2.1 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 195 | 2891–2903 | REG | `node_modules/strip-indent` | strip-indent@3.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 196 | 2904–2909 | REG | `node_modules/stylis` | stylis@4.4.0 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 197 | 2910–2916 | REG | `node_modules/symbol-tree` | symbol-tree@3.2.4 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 198 | 2917–2925 | REG | `node_modules/throttle-debounce` | throttle-debounce@5.0.2 | version, resolved, integrity, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 199 | 2926–2932 | REG | `node_modules/tinybench` | tinybench@2.9.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 200 | 2933–2942 | REG | `node_modules/tinyexec` | tinyexec@1.2.4 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 201 | 2943–2959 | REG | `node_modules/tinyglobby` | tinyglobby@0.2.17 | version, resolved, integrity, dev, license, dependencies, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 202 | 2960–2969 | REG | `node_modules/tinyrainbow` | tinyrainbow@3.1.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 203 | 2970–2982 | REG | `node_modules/tldts` | tldts@7.4.3 | version, resolved, integrity, dev, license, dependencies, bin | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 204 | 2983–2989 | REG | `node_modules/tldts-core` | tldts-core@7.4.3 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 205 | 2990–2995 | REG | `node_modules/toggle-selection` | toggle-selection@1.0.6 | version, resolved, integrity, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 206 | 2996–3008 | REG | `node_modules/tough-cookie` | tough-cookie@6.0.1 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 207 | 3009–3021 | REG | `node_modules/tr46` | tr46@6.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 208 | 3022–3029 | REG | `node_modules/tslib` | tslib@2.8.1 | version, resolved, integrity, dev, license, optional | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity；optional。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 209 | 3030–3043 | REG | `node_modules/typescript` | typescript@5.9.3 | version, resolved, integrity, dev, license, bin, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 210 | 3044–3053 | REG | `node_modules/undici` | undici@7.29.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 211 | 3054–3131 | REG | `node_modules/vite` | vite@8.0.16 | version, resolved, integrity, dev, license, dependencies, bin, engines, funding, optionalDependencies, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 212 | 3132–3221 | REG | `node_modules/vitest` | vitest@4.1.9 | version, resolved, integrity, dev, license, dependencies, bin, engines, funding, peerDependencies, peerDependenciesMeta | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 213 | 3222–3234 | REG | `node_modules/w3c-xmlserializer` | w3c-xmlserializer@5.0.0 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 214 | 3235–3244 | REG | `node_modules/webidl-conversions` | webidl-conversions@8.0.1 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 215 | 3245–3254 | REG | `node_modules/whatwg-mimetype` | whatwg-mimetype@5.0.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 216 | 3255–3269 | REG | `node_modules/whatwg-url` | whatwg-url@16.0.1 | version, resolved, integrity, dev, license, dependencies, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 217 | 3270–3286 | REG | `node_modules/why-is-node-running` | why-is-node-running@2.3.0 | version, resolved, integrity, dev, license, dependencies, bin, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 218 | 3287–3296 | REG | `node_modules/xml-name-validator` | xml-name-validator@5.0.0 | version, resolved, integrity, dev, license, engines | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 219 | 3297–3303 | REG | `node_modules/xmlchars` | xmlchars@2.2.0 | version, resolved, integrity, dev, license | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |
| 220 | 3304–3319 | REG | `node_modules/yaml` | yaml@2.9.0 | version, resolved, integrity, dev, license, bin, engines, funding | 技术性 | 官方 registry 安装记录，HTTPS resolved + sha512 integrity。只服务构建/测试/运行闭包，无业务发明。三份 HTML 无 npm 锚点，不因此改判添加。 |

## 9. 必要性（三问收口）

1. 必须存在：否则 CI `npm ci` 与工作区复现失败（GEN-013）。不是 HTML 业务条款。  
2. 行为已验证：§3.2 / §5。  
3. 前提已拆：SRI≠无漏洞；dev≠生产利用；documentVersion 非协议但兼容；磁盘 node_modules≠锁。

`documentVersion V0.01`：用户版本铁律作用于**被改文件自身**，不能改应用 semver 0.1.0，不能改 lockfileVersion 3。本次独立验证兼容（类型 3+4）与必要性（类型 4：铁律原文 + 无更小占位）。不是业务发明。

## 10. 命令与关键输出（节录）

- `sha256sum package-lock.json` → `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`；`wc -l -c` → 3321 114242  
- `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`  
- `npm ls --package-lock-only --ignore-scripts --depth=0` → exit 0  
- `npm ls --package-lock-only --ignore-scripts --all` → exit 0，仅 unmet optional peer  
- `npm ci --dry-run --ignore-scripts` → exit 0  
- `npm audit --package-lock-only --ignore-scripts` 全量与 `--omit=dev` → 均为 exit 0、total 0  
- 删 react 的隔离锁：`npm ls --package-lock-only --prefix <scratch>` → **exit 1**，`ELSPROBLEMS missing: react@^19.0.0`  
- Arborist 11.19.0 loadVirtual：`documentVersion_in_toJSON/toString = V0.01`，`sorted_deep_equal true`，packages 220  
- nanoid 3.3.18 tarball sri_match true；`nanoid(-1)` / `customAlphabet('abc',0)()` → `{"v":"","ms":0,"len":0}`；`nanoid(6)` 长度 6  
- postcss 8.5.23 tarball sri_match true；绝对 outside.map → MAP_NULL、hasMarker false  
- undici 7.29.0 tarball sri_match true；`private=` 与 `private, private=hdr` → `private:true`，无异常  

未在主环境 install/reify。scratch 已删。

## 11. 真实局限

- 未把其余 213 个非修补 tarball 再下载重哈希；完整性对这 213 个停在官方 dist.integrity 比对 + 格式校验。三个修补包做了字节级重算。  
- `npm audit` 依赖当时公告库，不是漏洞不存在的证明。  
- 未实际执行 npm 10.9.8 二进制 Arborist（只核官方 tarball SRI 并读 shrinkwrap 源）。宿主是 11.19.0。  
- `npm ci --dry-run` 不是一次真实 reify；未部署、未跑业务测试、未查数据库。  
- 26 个 dev 包 engines 严于根 `>=22`：记为跨模块观察，不在本锁造假 engines，不升格本模块 FAIL。  
- 打开 findings 时看见 B4 接收段，未当证据。

## 12. 临时 finding

**无 GR。** 无冲突、无添加、无未修好的 001–003、无新工程缺陷。覆盖表中的技术性登记不是修复队列。

## 13. 结论

整模块 3321 行 / 220 记录 / 18 GEN / 三线已审完。  
**PASS**

源码最终 SHA256：`65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`（与开工时相同，本席未改源）。  
无其他席曝光：未读 self/b4 raw、未读其他席本次分析、未把适配器缓冲当输入。

本席 B5 结束，不进入修复、发布或下一模块。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M004 B5-r1 外席全量审计报告（Grok）

版本：V0.02  
模块：M004 `治理门禁与 CI 基线／配置与工程入口／第2段`（`plan/modules.md` 实切）  
步骤：B5-r1（整模块重审；本批 B2 定稿后修复队列空，跳过 B3/B4，历史 B3=1 不变）  
模块生命周期（只读 ledger）：`IN_PROGRESS`；方案修复累计 2；交付失败 0  
范围文件：仅 `package-lock.json`（3321 行 / 220 条 / OVERSIZE-SINGLE-FILE / `documentVersion` V0.02）  
本席结论：**PASS**

旧 raw 元数据（只取版本/大小/hash，不采信旧正文结论）：V0.01、95358 字节、SHA256 `31f7bd9586520ad2ab89fd4b3bbc5a95fa596d081b68a2a343ce60af5ced264f`。本次完整报告追加于后，不覆盖。

---

## 1. 角色、技能、独立性

本席 = Grok（审计席乙）。只审不修；不派子代理；不调用 MCP_Codex_160 / MCP_Cloud_160；不改依赖/代码/HTML/计划/锁/ledger/findings/questions；不提交、不推送、不部署、不开 worktree、不 `npm audit fix`、不安装依赖。

正本：`/home/james/complex-management` `main`，`HEAD == origin/main` = `242f487233d028173cb9198eecd079f5d877405c`。

### 1.1 §6 技能实核

已读并按收窄口径使用：

| 技能 | 路径 | 本席用法 |
|---|---|---|
| using-superpowers | superpowers/6.3.0 | 开工前技能清单 |
| grilling / grill-me | mattpocock grilling + grill-me | 席内自问，不向人发问、不派第三席 |
| codebase-design | mattpocock codebase-design | 锁作为 npm ci 的深模块接口 |
| diagnosing-bugs / systematic-debugging | mattpocock + superpowers | 只读追 GHSA 到 interceptorPlugin.load，不改代码 |
| ponytail-review | ponytail 4.9.0 | 找可删；锁内无首跑失败的多余包可删 |
| verification-before-completion | superpowers | PASS 前实跑命令 |
| i-have-adhd | marketplace 存在 | 输出模式；本报告第一行仍服从独占哈希格式 |
| git-guardrails-claude-code | 可选存在 | **不调用**（会写钩子并问人） |
| tdd / test-driven-development | 存在 | 无源行为变更，不新增测试文件 |
| writing-for-agents / resolving-merge-conflicts / ponytail 修复模式 | 存在 | 本步不适用 |

缺口（不冒充已调用）：`batch-grill-me`、`ubiquitous-language` 无独立 SKILL.md。`domain-modeling` 存在但提示词划掉（会改 CONTEXT.md/ADR）。

### 1.2 独立性

未读本次或历史 self raw、其他模块审计/收口、本模块旧 B5 正文结论（仅 head 取 V0.01/大小/hash）。findings 只取当前 M004 定稿 ID 与队列值，不凭旧 322 同号 PASS、B1 或 B2 票替代本次 B5。对照读取限于根/`apps/web` 清单、npm 原生消费、CI 锁入口、vite/vitest 配置及公告调用路径。

---

## 2. 范围与冻结映射

`plan/modules.md`：文件数 1，有效行 3321，标记 OVERSIZE-SINGLE-FILE，唯一路径 `package-lock.json`。

`criteria-lock.md` 映射：`M004 → [GEN-001 … GEN-018]` 全部 18 条。无 HTML 派生 ID 映射到本模块。三份业务 HTML 检索 `package-lock`/`npm`：无命中。用户 B/C「就是我的原意」不重判冲突。

判据锁独立实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本报告第一行）。判据未扩锁、未静默对齐。

当前 findings V0.94 七项（均技术性、工程缺陷否、修复队列否）：M004-001 … M004-007。队列空故跳过 B3/B4。本席不把「队列否」自动写成模块 PASS。

---

## 3. 第一性原理

### 3.1 为何必须存在

`package-lock.json` 是 npm lockfile v3 的安装闭包。删掉它：CI `npm ci --ignore-scripts`（`.github/workflows/predeploy-gates.yml:23` 与 `ops/ci/predeploy-gates.yml:23`）与本地可复现安装会失去钉扎的 version/resolved/integrity。三份 HTML 没有「必须有锁文件」的业务句；归技术性（构建/安装/测试），不是添加。

最小用途（按字段，对照 npm Arborist 9.9.1 / lockfile v3）：

| 字段 | 最小用途 | 本文件事实 |
|---|---|---|
| name / version | 应用名与应用语义版本，不是文件自身版本 | `entertainment-complex-management` / `0.1.0` |
| lockfileVersion | 协议 | `3` |
| documentVersion | 非 npm 协议；文件自身版本 | `V0.02`；Arborist `loadVirtual` 与 `npm ci --dry-run` 仍消费该文件 |
| requires | v3 兼容旗标 | `true` |
| packages[""] | 根清单镜像 | name/version/workspaces/devDependencies/engines 与根 `package.json` 对应字段 `deepEqual` |
| packages["apps/web"] | 工作区清单镜像 | name/version/dependencies/devDependencies 与 `apps/web/package.json` 对应字段 `deepEqual` |
| packages["node_modules/apps-web"] | 工作区链接 | `{resolved:"apps/web", link:true}`，无 integrity |
| 其余 packages[*] | 钉扎 tarball | 均 `https://registry.npmjs.org/` + `sha512-` |

### 3.2 声称行为是否实际执行

| 命令 | 真实 exit | 关键输出 | 不得等同 |
|---|---|---|---|
| `npm ls --package-lock-only --depth=0` | 0 | 根 `@playwright/test@1.61.0`、`yaml@2.9.0`、`apps-web@0.1.0 -> ./apps/web`、web 直依含 `vitest@4.1.9` | 不等于已安装树等于锁 |
| `npm ls --package-lock-only --all --omit=optional` | 0 | 仅 UNMET OPTIONAL peer（canvas、msw、browser 包等），无 invalid/extraneous | 可选 peer 缺失不是硬缺陷 |
| Arborist `loadVirtual` | 成功 | inventory=220，invalid=0，extraneous=0，missing=0，edgesOut=563 | 不验证 SRI 字节、不验证远端 |
| `npm ci --dry-run --ignore-scripts --offline` | 0 | `added 188 packages, and removed 186 packages` | **不是**安装/构建/测试通过；证明计划可算且当前 `node_modules` ≠ 锁 |
| `npm ci --dry-run --ignore-scripts` | 0 | 同上 added 188 / removed 186 | 同上 |
| `npm audit --package-lock-only --ignore-scripts --json` | **1** | moderate=2 high=0 critical=0；仅 `@vitest/mocker` 与 `vitest`，同一 GHSA-82fw-gwwq-j7x9 | 不是 audit 绿 |
| `npm audit --package-lock-only --ignore-scripts --omit=dev --json` | 0 | total=0 | 只说明公告在 dev 闭包 |
| `ssri.parse` × 217 | 全部可解析 | 算法全为 sha512 | 未下载 tarball，不声称供应链已验证 |
| `ssri.checkData(dummy, vitest.integrity)` | matched=false | 错误字节不匹配 | 正向 tarball 核验未做 |

既有 `node_modules` 对照（排除范围，仅消费上下文，不等于锁）：vitest/@vitest/mocker/vite/jsdom 与锁同为 4.1.9/8.0.16/29.1.1；**nanoid 已装 3.3.12 vs 锁 3.3.18；postcss 已装 8.5.15 vs 锁 8.5.23；undici 已装 7.28.0 vs 锁 7.29.0**。

### 3.3 未验证就当真的前提

- 未跑真实 `npm ci`（会写盘安装）。
- 未跑 `vite build` / `vitest run` / 全量门禁（会扩到他模块并依赖陈旧 node_modules）。
- 未对 217 个 tarball 做下载后 SRI 比对。
- 本机 `npm 11.19.0` vs 根清单 `packageManager: npm@10.9.8`：属本机状态，锁协议 v3 仍被 npm 11 消费；不把本机工具链差写成锁缺陷，也不新造「必须 packageManager 一致」门禁。
- CI `node-version: "22"` 是否精确 ≥22.13.0：属 M005 工作流，本模块只核锁根 `engines.node` = `^22.13.0 \|\| >=24.0.0` 与根清单逐字相等。当前审计进程 node v24.20.0 落在声明内。

---

## 4. 顺从式：18 GEN 逐项

| ID | 适用 | 本次证据 | 新缺陷 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 锁是 npm ci 钉扎闭包；无重复 packages 键（分词器 DUP_KEYS=0）；嵌套 `@rolldown/pluginutils` 两处同版 1.0.1 是 npm 合法嵌套，Arborist 无 extraneous。ponytail：不删可选平台包或嵌套位。 | 无 |
| GEN-002 可执行正确性 | 适用 | `npm ls --package-lock-only` exit 0；`npm ci --dry-run` 能出计划 exit 0。删 vitest 条目后 `npm ci --dry-run --offline` **exit 1** `EUSAGE`「Missing: vitest@4.1.10 from lock file」。 | 无 |
| GEN-003 测试有效性 | 适用（本文件无测试） | 模块内无测试文件。用 npm 原生 ci/ls/audit/ssri 作可失败断言。不新增 TDD 文件。测试通过本身不替代锁证据。 | 无 |
| GEN-004 信任边界 | 适用 | 217 resolved 全 HTTPS npmjs；无 git/file/http。GHSA 信任边界见 §6：公开 mocker WS 未在当前入口注册。 | 无新队列项 |
| GEN-005 授权与最小权限 | 弱适用 | 锁不含主体/对象授权。`hasInstallScript` 仅 darwin `fsevents`；CI 已 `--ignore-scripts`。 | 无 |
| GEN-006 数据完整性 | 适用 | 217 sha512 可 parse；工作区链接无 SRI 符合 v3。根/web 清单镜像一致。 | 无 |
| GEN-007 并发与幂等 | 弱适用 | 静态 JSON。npm ci 是按锁重装，不是业务幂等。 | 无 |
| GEN-008 接口与模式一致性 | 适用 | 根/web 清单与锁镜像字段一致。应用 version 0.1.0 ≠ documentVersion V0.02。无第二套 lock。 | 无 |
| GEN-009 错误处理 | 适用 | 缺条目/错版本：npm ci EUSAGE exit 1。清单多 lodash：offline `ENOTCACHED` exit 1。失败显式。 | 无 |
| GEN-010 安全与敏感信息 | 适用 | 无明文秘密。当前 audit 仅 GHSA-82fw-gwwq-j7x9（CWE-22，medium，CVE-2026-84373）。nanoid/postcss/undici **不在**本次 audit 结果。锁内无 `apps/api`/`fastify`/`prisma`/`sqlite`。见 §6。 | 登记于既有 M004-007，不新开队列 |
| GEN-011 可观测 | 适用 | audit JSON、ls、Arborist 输出可复核。不把 dry-run 写成已安装。 | 无 |
| GEN-012 资源边界 | 适用 | 34 optional、28 带 os/cpu；本机 linux x64 glibc，锁含 `lightningcss-linux-x64-gnu` 与 `@rolldown/binding-linux-x64-gnu`。无 libc 字段。 | 无 |
| GEN-013 构建配置与依赖 | 主适用 | 锁可被 npm ci 计划；版本与直依范围 semver.satisfies（naive 解析曾误报 pluginutils，Arborist 与 `apps/web/node_modules/@rolldown/pluginutils@1.0.1` 证明边有效）。 | 无 |
| GEN-014 数据库迁移 | 不适用 | 无 SQL/迁移。 | 无 |
| GEN-015 界面可用性 | 不适用 | 无 UI 文案。antd/react 只作为依赖名。 | 无 |
| GEN-016 文档可验证性 | 适用 | `documentVersion` V0.02、3321 行、SHA `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef` 与 findings 当前源 SHA 一致。不把计划或旧 322 PASS 写成当前事实。 | 无 |
| GEN-017 业务来源分类 | 适用 | 全文件走冲突→细化→技术性→添加：三 HTML 无 npm 锁句且无矛盾 → 非冲突；无 HTML 上位条目 → 非细化；无业务实体/枚举/权限发明 → **技术性**。 | 无 |
| GEN-018 第一性原理证据 | 适用 | 结论落到命令输出、npm 规范行为、GHSA 原文。不引用旧席结论当依据。 | 无 |

---

## 5. 对抗式（grilling 自问 + 原生反例）

席内问题与答案：

1. 是否用 B1/B2 PASS 代替 B5？否。本席重读锁、重跑 audit/ls/ci-dry/Arborist/ssri，并独立取 GHSA。
2. 是否宣称 audit 绿/公告误报/永不可触发？否。audit exit 1 属实；公告版本命中 4.1.9 属实。
3. 是否凭标题自造「必须升级才 PASS」门禁？否。铁律 6：首跑是否撞上。
4. `vite --host 0.0.0.0` 是否单独等于触发通道？否。缺 mockerPlugin/interceptorPlugin 注册。
5. `node_modules` 旧 nanoid/postcss/undici 是否证明锁坏？否。那是已装树漂移，排除范围。

构造反例（`/tmp/m004-adv-run` 副本，正本未改）：

| 反例 | 工具 | 结果 |
|---|---|---|
| 删除 `packages["node_modules/vitest"]` | `npm ci --dry-run --offline` | exit 1，`EUSAGE`，`Missing: vitest@4.1.10 from lock file`（npm 按 `^4.1.9` 解析到 4.1.10 后发现锁不同步） |
| 将 vitest.version 改为 `0.0.0` | 同上 | exit 1（同样走 EUSAGE 同步失败） |
| `apps/web/package.json` 增加 `lodash` | 同上 | exit 1，`ENOTCACHED` 请求 registry.npmjs.org/lodash |
| 仅改锁内 `packages["apps/web"].dependencies.react` 为 `^18.0.0`（package.json 仍 ^19） | 同上 | **dry-run exit 0**。说明 npm ci 同步检查看的是清单与锁能否解出同一树，不是只比对该字符串；当前 `node_modules/react` 仍为 19.2.7。不把 dry-run 写成「范围字符串已校验」 |
| dummy 字节 vs vitest SRI | `ssri.checkData` | matched=false |
| `ssri.parse("not-an-sri")` | ssri 13.0.1 | 返回 null，不抛。不能只 parse  junk 当验证 |
| Arborist loadVirtual 对错版本/缺 SRI/http resolved | loadVirtual | **不标 invalid**。虚拟加载不是完整性门禁；完整性靠 ci 抽取与 ssri |

当前 vitest 发行包 `node_modules/vitest/dist/**/*.js` 对 `interceptorPlugin`/`mockerPlugin` **零引用**；只从 `@vitest/mocker/node` 引入 `hoistMocksPlugin`/`automockPlugin`。`apps/web` 源码对 `@vitest/mocker`、`mockerPlugin`、`interceptorPlugin`、browser 配置 **rg 无匹配**。`vite.config.ts` / `vitest.config.ts` 插件均为 `react()`，environment=`jsdom`。

---

## 6. GHSA-82fw-gwwq-j7x9 独立判断（GEN-004/010/013 + 铁律 6）

主来源已获取（HTTP 200）：

- HTML：`https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9` 标题「Path Traversal / Arbitrary File Read via @vitest/mocker Redirect Mock」
- API：`https://api.github.com/advisories/GHSA-82fw-gwwq-j7x9` → GHSA-82fw-gwwq-j7x9，CVE-2026-84373，severity medium，CVSS 5.9 `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N`，CWE-22
- 受影响：`@vitest/mocker` 与 `vitest` `>=2.1.0 <4.1.11`（另有 5.x 预发布区间）。修补 4.1.11
- 锁内版本：`node_modules/@vitest/mocker` 4.1.9（L1121–1147）、`node_modules/vitest` 4.1.9（L3132–3221）**命中**
- 公告原文要点：`interceptorPlugin` 的 `load` 对 `mock.type==='redirect'` 执行 `readFile(mock.redirect)`；公开 `mockerPlugin`/`interceptorPlugin` 把处理函数挂在 **未认证** Vite HMR WebSocket `vitest:interceptor:register`。Vitest browser 模式走 token RPC。开发服务器默认 localhost；需 `server.host`/`0.0.0.0` 才网络可达。这是开发服务器问题。

本仓库当前入口：

- `apps/web/package.json` `dev` = `vite --host 0.0.0.0 --port 5173`（会暴露 Vite，但插件只有 react）
- `test:unit` = `vitest run`（jsdom，无 browser）
- 测试用 `vi.mocked`，无 redirect mock 注册
- 已装 4.1.9 的 `node.js` 含公告所述 `readFile(mock.redirect)` 与 `server.ws.on("vitest:interceptor:register")`，但 **vitest CLI 未装配该插件**

铁律 6：首次正常跑会撞上吗？`npm ci`、`vitest run`、`vite --host 0.0.0.0`（现配置）都不会挂上该 WS 注册通道。要触发需再注册公开 mocker 插件或打开 browser 模式——不是首跑必撞。因此 **不把「必须升级到 4.1.11」写成工程缺陷=是/修复队列=是**。

同时：不能写 npm audit 全绿；不能写公告误报；不能写永远不可触发；将来若接线 mockerPlugin/browser，不在本次批准范围。与定稿 M004-007 处置一致，但是 **本次独立证据**，不是抄 B2。

`fixAvailable: true` 不构成必须本轮升级。升级会改锁内 vitest 家族多个包，属修复动作，B5 禁止。

---

## 7. 四分类

顺序：冲突 → 细化 → 技术性 → 添加。

- **冲突**：三 HTML 无 npm/锁表述，锁也未发明票种/权限/枚举。无冲突。用户 B/C 原意不重判。
- **细化**：无具体 HTML 上位条目。
- **技术性**：全 220 条均为安装/构建/测试/运行闭包，无业务发明。理由见覆盖表末列。不要求每行 HTML 锚点。
- **添加**：无。

全局 html-ledger-sync/entry-anchors 红属 M425/M427 等，不证明本锁坏，也不因本模块 PASS 称全仓/发布绿。本模块不为刷绿改 CSV/HTML，不重开 M001–M003。

---

## 8. 连续 3321 行覆盖

`wc -l` = 3321；`git ls-files -z -- package-lock.json | xargs -0 cat | wc -l` = 3321；文件以换行结束。SHA256 `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef`，114258 字节。

| 行号 | 内容 |
|---|---|
| 1–6 | `{` / name / version / lockfileVersion 3 / documentVersion V0.02 / requires |
| 7 | `"packages": {` |
| 8–21 | 根 `""` |
| 22–44 | `apps/web` |
| 45–3319 | 其余 218 条 packages（见下表起止） |
| 3320 | `packages` 闭 `}` |
| 3321 | 根闭 `}` |

下表 yaml 行曾把 3320–3321 划进末条止行（分词器「下一起始-1」），**yaml 正文实际 3304–3319**。全文件无未覆盖行。无重复键。顶层键仅 `name,version,lockfileVersion,documentVersion,requires,packages`。无顶层旧式 `dependencies` 树。

结构段已通读：头、根与 web 镜像、嵌套 plugin-react、rolldown 平台 optional、vitest/mocker、nanoid 3.3.18、postcss 8.5.23、undici 7.29.0、antd/react、jsdom、lightningcss 平台包、rc-*、收尾 yaml。其余条目由 JSON 全字段遍历 + 行号表覆盖，不是只计数。

---

## 9. 全部 220 条技术分类覆盖表

列：起、止、行数、类、路径、版本、标志、平台、许可证、声明边数、完整性、字段、技术性理由。  
完整性：217 个 tarball 为 sha512 可解析；ROOT/WORKSPACE_PACKAGE/WORKSPACE_LINK 协议不要求 SRI（链接行 resolved=`apps/web`）。  
类计数：ROOT 1 + WORKSPACE_PACKAGE 1 + WORKSPACE_LINK 1 + NESTED 6 + HOISTED 211 = 220。

| 起 | 止 | 行数 | 类 | 路径 | 版本 | 标志 | 平台 | 许可证 | 边 | 完整性 | 字段 | 技术性理由 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
|8|21|14|ROOT|(root)|0.1.0|-|-|-|2|SRI|name\|version\|workspaces\|devDependencies\|engines|npm 工作区根安装元数据镜像，供 npm ci 对齐根 package.json|
|22|44|23|WORKSPACE_PACKAGE|apps/web|0.1.0|-|-|-|15|SRI|name\|version\|dependencies\|devDependencies|apps/web 工作区清单镜像，供 npm ci 对齐 web package.json|
|45|51|7|NESTED|apps/web/node_modules/@rolldown/pluginutils|1.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|52|77|26|NESTED|apps/web/node_modules/@vitejs/plugin-react|6.0.2|dev|-|MIT|4|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines\|peerDependencies\|peerDependenciesMeta|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|78|84|7|HOISTED|node_modules/@adobe/css-tools|4.5.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|85|93|9|HOISTED|node_modules/@ant-design/colors|7.2.1|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|94|112|19|HOISTED|node_modules/@ant-design/cssinjs|1.24.0|-|-|MIT|9|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|113|127|15|HOISTED|node_modules/@ant-design/cssinjs-utils|1.1.3|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|128|139|12|HOISTED|node_modules/@ant-design/fast-color|2.0.6|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|140|159|20|HOISTED|node_modules/@ant-design/icons|5.6.1|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|160|165|6|HOISTED|node_modules/@ant-design/icons-svg|4.4.2|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|166|181|16|HOISTED|node_modules/@ant-design/react-slick|1.1.2|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|182|198|17|HOISTED|node_modules/@asamuzakjp/css-color|5.1.11|dev|-|MIT|5|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|199|215|17|HOISTED|node_modules/@asamuzakjp/dom-selector|7.1.1|dev|-|MIT|5|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|216|225|10|HOISTED|node_modules/@asamuzakjp/generational-cache|1.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|226|232|7|HOISTED|node_modules/@asamuzakjp/nwsapi|2.3.9|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|233|248|16|HOISTED|node_modules/@babel/code-frame|7.29.7|dev,peer|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|license\|peer\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|249|259|11|HOISTED|node_modules/@babel/helper-validator-identifier|7.29.7|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|260|268|9|HOISTED|node_modules/@babel/runtime|7.29.7|-|-|MIT|0|SRI|version\|resolved\|integrity\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|269|281|13|HOISTED|node_modules/@bramus/specificity|2.4.2|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin|registry 提升安装位，安装/构建/测试/运行闭包所需|
|282|301|20|HOISTED|node_modules/@csstools/color-helpers|6.0.2|dev|-|MIT-0|0|SRI|version\|resolved\|integrity\|dev\|funding\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|302|325|24|HOISTED|node_modules/@csstools/css-calc|3.2.1|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|funding\|license\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|326|353|28|HOISTED|node_modules/@csstools/css-color-parser|4.1.7|dev|-|MIT|4|SRI|version\|resolved\|integrity\|dev\|funding\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|354|376|23|HOISTED|node_modules/@csstools/css-parser-algorithms|4.0.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|funding\|license\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|377|401|25|HOISTED|node_modules/@csstools/css-syntax-patches-for-csstree|1.1.5|dev|-|MIT-0|1|SRI|version\|resolved\|integrity\|dev\|funding\|license\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|402|421|20|HOISTED|node_modules/@csstools/css-tokenizer|4.0.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|funding\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|422|433|12|HOISTED|node_modules/@emnapi/core|1.10.0|dev,opt|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|optional\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|434|444|11|HOISTED|node_modules/@emnapi/runtime|1.10.0|dev,opt|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|optional\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|445|455|11|HOISTED|node_modules/@emnapi/wasi-threads|1.2.1|dev,opt|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|optional\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|456|461|6|HOISTED|node_modules/@emotion/hash|0.8.0|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|462|467|6|HOISTED|node_modules/@emotion/unitless|0.7.5|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|468|485|18|HOISTED|node_modules/@exodus/bytes|1.15.1|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|engines\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|486|492|7|HOISTED|node_modules/@jridgewell/sourcemap-codec|1.5.5|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|493|511|19|HOISTED|node_modules/@napi-rs/wasm-runtime|1.1.5|dev,opt|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|license\|optional\|dependencies\|funding\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|512|521|10|HOISTED|node_modules/@oxc-project/types|0.133.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|522|537|16|HOISTED|node_modules/@playwright/test|1.61.0|dev|-|Apache-2.0|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|538|549|12|HOISTED|node_modules/@rc-component/async-validator|5.1.0|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|550|565|16|HOISTED|node_modules/@rc-component/color-picker|2.0.1|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|566|579|14|HOISTED|node_modules/@rc-component/context|1.4.0|-|-|MIT|4|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|580|591|12|HOISTED|node_modules/@rc-component/mini-decimal|1.1.3|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|592|609|18|HOISTED|node_modules/@rc-component/mutate-observer|1.1.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|610|627|18|HOISTED|node_modules/@rc-component/portal|1.1.2|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|628|643|16|HOISTED|node_modules/@rc-component/qrcode|1.1.1|-|-|MIT|3|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|644|663|20|HOISTED|node_modules/@rc-component/tour|1.15.1|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|664|684|21|HOISTED|node_modules/@rc-component/trigger|2.3.1|-|-|MIT|8|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|685|701|17|HOISTED|node_modules/@rolldown/binding-android-arm64|1.0.3|dev,opt|os=android;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|702|718|17|HOISTED|node_modules/@rolldown/binding-darwin-arm64|1.0.3|dev,opt|os=darwin;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|719|735|17|HOISTED|node_modules/@rolldown/binding-darwin-x64|1.0.3|dev,opt|os=darwin;cpu=x64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|736|752|17|HOISTED|node_modules/@rolldown/binding-freebsd-x64|1.0.3|dev,opt|os=freebsd;cpu=x64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|753|769|17|HOISTED|node_modules/@rolldown/binding-linux-arm-gnueabihf|1.0.3|dev,opt|os=linux;cpu=arm|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|770|786|17|HOISTED|node_modules/@rolldown/binding-linux-arm64-gnu|1.0.3|dev,opt|os=linux;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|787|803|17|HOISTED|node_modules/@rolldown/binding-linux-arm64-musl|1.0.3|dev,opt|os=linux;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|804|820|17|HOISTED|node_modules/@rolldown/binding-linux-ppc64-gnu|1.0.3|dev,opt|os=linux;cpu=ppc64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|821|837|17|HOISTED|node_modules/@rolldown/binding-linux-s390x-gnu|1.0.3|dev,opt|os=linux;cpu=s390x|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|838|854|17|HOISTED|node_modules/@rolldown/binding-linux-x64-gnu|1.0.3|dev,opt|os=linux;cpu=x64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|855|871|17|HOISTED|node_modules/@rolldown/binding-linux-x64-musl|1.0.3|dev,opt|os=linux;cpu=x64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|872|888|17|HOISTED|node_modules/@rolldown/binding-openharmony-arm64|1.0.3|dev,opt|os=openharmony;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|889|907|19|HOISTED|node_modules/@rolldown/binding-wasm32-wasi|1.0.3|dev,opt|cpu=wasm32|MIT|3|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|908|924|17|HOISTED|node_modules/@rolldown/binding-win32-arm64-msvc|1.0.3|dev,opt|os=win32;cpu=arm64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|925|941|17|HOISTED|node_modules/@rolldown/binding-win32-x64-msvc|1.0.3|dev,opt|os=win32;cpu=x64|MIT|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|942|948|7|HOISTED|node_modules/@standard-schema/spec|1.1.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|949|969|21|HOISTED|node_modules/@testing-library/dom|10.4.1|dev,peer|-|MIT|8|SRI|version\|resolved\|integrity\|dev\|license\|peer\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|970|989|20|HOISTED|node_modules/@testing-library/jest-dom|6.9.1|dev|-|MIT|6|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|990|996|7|NESTED|node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api|0.6.3|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|997|1024|28|HOISTED|node_modules/@testing-library/react|16.3.2|dev|-|MIT|6|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1025|1038|14|HOISTED|node_modules/@testing-library/user-event|14.6.1|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1039|1049|11|HOISTED|node_modules/@tybys/wasm-util|0.10.2|dev,opt|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|optional\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1050|1057|8|HOISTED|node_modules/@types/aria-query|5.0.4|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1058|1068|11|HOISTED|node_modules/@types/chai|5.2.3|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1069|1075|7|HOISTED|node_modules/@types/deep-eql|4.0.2|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1076|1082|7|HOISTED|node_modules/@types/estree|1.0.9|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1083|1092|10|HOISTED|node_modules/@types/react|19.2.16|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1093|1102|10|HOISTED|node_modules/@types/react-dom|19.2.3|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1103|1120|18|HOISTED|node_modules/@vitest/expect|4.1.9|dev|-|MIT|6|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1121|1147|27|HOISTED|node_modules/@vitest/mocker|4.1.9|dev|-|MIT|5|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1148|1160|13|HOISTED|node_modules/@vitest/pretty-format|4.1.9|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1161|1174|14|HOISTED|node_modules/@vitest/runner|4.1.9|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1175|1190|16|HOISTED|node_modules/@vitest/snapshot|4.1.9|dev|-|MIT|4|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1191|1200|10|HOISTED|node_modules/@vitest/spy|4.1.9|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1201|1215|15|HOISTED|node_modules/@vitest/utils|4.1.9|dev|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1216|1226|11|HOISTED|node_modules/ansi-regex|5.0.1|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1227|1240|14|HOISTED|node_modules/ansi-styles|5.2.0|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1241|1305|65|HOISTED|node_modules/antd|5.29.3|-|-|MIT|51|SRI|version\|resolved\|integrity\|license\|dependencies\|funding\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1306|1309|4|WORKSPACE_LINK|node_modules/apps-web|-|link|-|-|0|SRI|resolved\|link|npm 工作区符号链接记录，无 tarball/SRI 属 lockfile v3 协议|
|1310|1319|10|HOISTED|node_modules/aria-query|5.3.0|dev|-|Apache-2.0|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1320|1329|10|HOISTED|node_modules/assertion-error|2.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1330|1339|10|HOISTED|node_modules/bidi-js|1.0.3|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1340|1349|10|HOISTED|node_modules/chai|6.2.2|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1350|1355|6|HOISTED|node_modules/classnames|2.5.1|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1356|1361|6|HOISTED|node_modules/compute-scroll-into-view|3.1.1|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1362|1368|7|HOISTED|node_modules/convert-source-map|2.0.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1369|1377|9|HOISTED|node_modules/copy-to-clipboard|3.3.3|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1378|1391|14|HOISTED|node_modules/css-tree|3.2.1|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1392|1398|7|HOISTED|node_modules/css.escape|1.5.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1399|1404|6|HOISTED|node_modules/csstype|3.2.3|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1405|1418|14|HOISTED|node_modules/data-urls|7.0.0|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1419|1424|6|HOISTED|node_modules/dayjs|1.11.21|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1425|1431|7|HOISTED|node_modules/decimal.js|10.6.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1432|1441|10|HOISTED|node_modules/dequal|2.0.3|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1442|1451|10|HOISTED|node_modules/detect-libc|2.1.2|dev|-|Apache-2.0|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1452|1459|8|HOISTED|node_modules/dom-accessibility-api|0.5.16|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1460|1472|13|HOISTED|node_modules/entities|8.0.0|dev|-|BSD-2-Clause|0|SRI|version\|resolved\|integrity\|dev\|license\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1473|1479|7|HOISTED|node_modules/es-module-lexer|2.1.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1480|1489|10|HOISTED|node_modules/estree-walker|3.0.3|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1490|1499|10|HOISTED|node_modules/expect-type|1.3.0|dev|-|Apache-2.0|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1500|1517|18|HOISTED|node_modules/fdir|6.5.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|engines\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1518|1532|15|HOISTED|node_modules/fsevents|2.3.3|dev,opt,install|os=darwin|MIT|0|SRI|version\|resolved\|integrity\|dev\|hasInstallScript\|license\|optional\|os\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1533|1545|13|HOISTED|node_modules/html-encoding-sniffer|6.0.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1546|1555|10|HOISTED|node_modules/indent-string|4.0.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1556|1562|7|HOISTED|node_modules/is-potential-custom-element-name|1.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1563|1570|8|HOISTED|node_modules/js-tokens|4.0.0|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1571|1611|41|HOISTED|node_modules/jsdom|29.1.1|dev|-|MIT|22|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1612|1620|9|HOISTED|node_modules/json2mq|0.2.0|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1621|1650|30|HOISTED|node_modules/lightningcss|1.32.0|dev|-|MPL-2.0|12|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines\|funding\|optionalDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1651|1671|21|HOISTED|node_modules/lightningcss-android-arm64|1.32.0|dev,opt|os=android;cpu=arm64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1672|1692|21|HOISTED|node_modules/lightningcss-darwin-arm64|1.32.0|dev,opt|os=darwin;cpu=arm64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1693|1713|21|HOISTED|node_modules/lightningcss-darwin-x64|1.32.0|dev,opt|os=darwin;cpu=x64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1714|1734|21|HOISTED|node_modules/lightningcss-freebsd-x64|1.32.0|dev,opt|os=freebsd;cpu=x64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1735|1755|21|HOISTED|node_modules/lightningcss-linux-arm-gnueabihf|1.32.0|dev,opt|os=linux;cpu=arm|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1756|1776|21|HOISTED|node_modules/lightningcss-linux-arm64-gnu|1.32.0|dev,opt|os=linux;cpu=arm64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1777|1797|21|HOISTED|node_modules/lightningcss-linux-arm64-musl|1.32.0|dev,opt|os=linux;cpu=arm64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1798|1818|21|HOISTED|node_modules/lightningcss-linux-x64-gnu|1.32.0|dev,opt|os=linux;cpu=x64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1819|1839|21|HOISTED|node_modules/lightningcss-linux-x64-musl|1.32.0|dev,opt|os=linux;cpu=x64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1840|1860|21|HOISTED|node_modules/lightningcss-win32-arm64-msvc|1.32.0|dev,opt|os=win32;cpu=arm64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1861|1881|21|HOISTED|node_modules/lightningcss-win32-x64-msvc|1.32.0|dev,opt|os=win32;cpu=x64|MPL-2.0|0|SRI|version\|resolved\|integrity\|cpu\|dev\|license\|optional\|os\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1882|1891|10|HOISTED|node_modules/lru-cache|11.5.1|dev|-|BlueOak-1.0.0|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1892|1902|11|HOISTED|node_modules/lz-string|1.5.0|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer\|bin|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1903|1912|10|HOISTED|node_modules/magic-string|0.30.21|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1913|1919|7|HOISTED|node_modules/mdn-data|2.27.1|dev|-|CC0-1.0|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1920|1929|10|HOISTED|node_modules/min-indent|1.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1930|1948|19|HOISTED|node_modules/nanoid|3.3.18|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|funding\|license\|bin\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1949|1962|14|HOISTED|node_modules/obug|2.1.3|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|funding\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1963|1975|13|HOISTED|node_modules/parse5|8.0.1|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1976|1982|7|HOISTED|node_modules/pathe|2.0.3|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1983|1989|7|HOISTED|node_modules/picocolors|1.1.1|dev|-|ISC|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|1990|2002|13|HOISTED|node_modules/picomatch|4.0.4|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2003|2021|19|HOISTED|node_modules/playwright|1.61.0|dev|-|Apache-2.0|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines\|optionalDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2022|2034|13|HOISTED|node_modules/playwright-core|1.61.0|dev|-|Apache-2.0|0|SRI|version\|resolved\|integrity\|dev\|license\|bin\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2035|2049|15|NESTED|node_modules/playwright/node_modules/fsevents|2.3.2|dev,opt,install|os=darwin|MIT|0|SRI|version\|resolved\|integrity\|dev\|hasInstallScript\|license\|optional\|os\|engines|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|2050|2078|29|HOISTED|node_modules/postcss|8.5.23|dev|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|funding\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2079|2094|16|HOISTED|node_modules/pretty-format|27.5.1|dev,peer|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|license\|peer\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2095|2102|8|NESTED|node_modules/pretty-format/node_modules/react-is|17.0.2|dev,peer|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|peer|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|2103|2112|10|HOISTED|node_modules/punycode|2.3.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2113|2121|9|HOISTED|node_modules/qrcode.react|4.2.0|-|-|ISC|1|SRI|version\|resolved\|integrity\|license\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2122|2138|17|HOISTED|node_modules/rc-cascader|3.34.0|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2139|2153|15|HOISTED|node_modules/rc-checkbox|3.5.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2154|2169|16|HOISTED|node_modules/rc-collapse|3.9.0|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2170|2186|17|HOISTED|node_modules/rc-dialog|9.6.0|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2187|2203|17|HOISTED|node_modules/rc-drawer|7.3.0|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2204|2219|16|HOISTED|node_modules/rc-dropdown|4.2.1|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2220|2237|18|HOISTED|node_modules/rc-field-form|2.7.1|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2238|2255|18|HOISTED|node_modules/rc-image|7.12.0|-|-|MIT|8|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2256|2270|15|HOISTED|node_modules/rc-input|1.8.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2271|2287|17|HOISTED|node_modules/rc-input-number|9.5.0|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2288|2306|19|HOISTED|node_modules/rc-mentions|2.20.0|-|-|MIT|9|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2307|2324|18|HOISTED|node_modules/rc-menu|9.16.1|-|-|MIT|8|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2325|2339|15|HOISTED|node_modules/rc-motion|2.9.5|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2340|2358|19|HOISTED|node_modules/rc-notification|5.6.4|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2359|2374|16|HOISTED|node_modules/rc-overflow|1.5.0|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2375|2389|15|HOISTED|node_modules/rc-pagination|5.1.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2390|2428|39|HOISTED|node_modules/rc-picker|4.11.3|-|-|MIT|12|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2429|2443|15|HOISTED|node_modules/rc-progress|4.0.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2444|2461|18|HOISTED|node_modules/rc-rate|2.13.1|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2462|2477|16|HOISTED|node_modules/rc-resize-observer|1.4.3|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2478|2493|16|HOISTED|node_modules/rc-segmented|2.7.1|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2494|2515|22|HOISTED|node_modules/rc-select|14.16.8|-|-|MIT|9|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2516|2533|18|HOISTED|node_modules/rc-slider|11.1.9|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2534|2551|18|HOISTED|node_modules/rc-steps|6.0.1|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2552|2566|15|HOISTED|node_modules/rc-switch|4.1.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2567|2587|21|HOISTED|node_modules/rc-table|7.54.0|-|-|MIT|8|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2588|2609|22|HOISTED|node_modules/rc-tabs|15.7.0|-|-|MIT|9|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2610|2626|17|HOISTED|node_modules/rc-textarea|1.10.2|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2627|2642|16|HOISTED|node_modules/rc-tooltip|6.4.0|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2643|2662|20|HOISTED|node_modules/rc-tree|5.13.1|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2663|2679|17|HOISTED|node_modules/rc-tree-select|5.27.0|-|-|MIT|7|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2680|2694|15|HOISTED|node_modules/rc-upload|4.11.0|-|-|MIT|5|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2695|2708|14|HOISTED|node_modules/rc-util|5.44.4|-|-|MIT|4|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2709|2727|19|HOISTED|node_modules/rc-virtual-list|3.19.2|-|-|MIT|6|SRI|version\|resolved\|integrity\|license\|dependencies\|engines\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2728|2736|9|HOISTED|node_modules/react|19.2.7|-|-|MIT|0|SRI|version\|resolved\|integrity\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2737|2748|12|HOISTED|node_modules/react-dom|19.2.7|-|-|MIT|2|SRI|version\|resolved\|integrity\|license\|dependencies\|peerDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2749|2754|6|HOISTED|node_modules/react-is|18.3.1|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2755|2768|14|HOISTED|node_modules/redent|3.0.0|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2769|2778|10|HOISTED|node_modules/require-from-string|2.0.2|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2779|2784|6|HOISTED|node_modules/resize-observer-polyfill|1.5.1|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2785|2818|34|HOISTED|node_modules/rolldown|1.0.3|dev|-|MIT|17|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines\|optionalDependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2819|2825|7|NESTED|node_modules/rolldown/node_modules/@rolldown/pluginutils|1.0.1|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|嵌套安装位，避免版本冲突；安装/构建/测试闭包所需|
|2826|2838|13|HOISTED|node_modules/saxes|6.0.0|dev|-|ISC|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2839|2844|6|HOISTED|node_modules/scheduler|0.27.0|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2845|2853|9|HOISTED|node_modules/scroll-into-view-if-needed|3.1.0|-|-|MIT|1|SRI|version\|resolved\|integrity\|license\|dependencies|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2854|2860|7|HOISTED|node_modules/siginfo|2.0.0|dev|-|ISC|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2861|2870|10|HOISTED|node_modules/source-map-js|1.2.1|dev|-|BSD-3-Clause|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2871|2877|7|HOISTED|node_modules/stackback|0.0.2|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2878|2884|7|HOISTED|node_modules/std-env|4.1.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2885|2890|6|HOISTED|node_modules/string-convert|0.2.1|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2891|2903|13|HOISTED|node_modules/strip-indent|3.0.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2904|2909|6|HOISTED|node_modules/stylis|4.4.0|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2910|2916|7|HOISTED|node_modules/symbol-tree|3.2.4|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2917|2925|9|HOISTED|node_modules/throttle-debounce|5.0.2|-|-|MIT|0|SRI|version\|resolved\|integrity\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2926|2932|7|HOISTED|node_modules/tinybench|2.9.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2933|2942|10|HOISTED|node_modules/tinyexec|1.2.4|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2943|2959|17|HOISTED|node_modules/tinyglobby|0.2.17|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2960|2969|10|HOISTED|node_modules/tinyrainbow|3.1.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2970|2982|13|HOISTED|node_modules/tldts|7.4.3|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2983|2989|7|HOISTED|node_modules/tldts-core|7.4.3|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2990|2995|6|HOISTED|node_modules/toggle-selection|1.0.6|-|-|MIT|0|SRI|version\|resolved\|integrity\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|2996|3008|13|HOISTED|node_modules/tough-cookie|6.0.1|dev|-|BSD-3-Clause|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3009|3021|13|HOISTED|node_modules/tr46|6.0.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3022|3029|8|HOISTED|node_modules/tslib|2.8.1|dev,opt|-|0BSD|0|SRI|version\|resolved\|integrity\|dev\|license\|optional|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3030|3043|14|HOISTED|node_modules/typescript|5.9.3|dev|-|Apache-2.0|0|SRI|version\|resolved\|integrity\|dev\|license\|bin\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3044|3053|10|HOISTED|node_modules/undici|7.29.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3054|3131|78|HOISTED|node_modules/vite|8.0.16|dev|-|MIT|18|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines\|funding\|optionalDependencies\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3132|3221|90|HOISTED|node_modules/vitest|4.1.9|dev|-|MIT|32|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines\|funding\|peerDependencies\|peerDependenciesMeta|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3222|3234|13|HOISTED|node_modules/w3c-xmlserializer|5.0.0|dev|-|MIT|1|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3235|3244|10|HOISTED|node_modules/webidl-conversions|8.0.1|dev|-|BSD-2-Clause|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3245|3254|10|HOISTED|node_modules/whatwg-mimetype|5.0.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3255|3269|15|HOISTED|node_modules/whatwg-url|16.0.1|dev|-|MIT|3|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3270|3286|17|HOISTED|node_modules/why-is-node-running|2.3.0|dev|-|MIT|2|SRI|version\|resolved\|integrity\|dev\|license\|dependencies\|bin\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3287|3296|10|HOISTED|node_modules/xml-name-validator|5.0.0|dev|-|Apache-2.0|0|SRI|version\|resolved\|integrity\|dev\|license\|engines|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3297|3303|7|HOISTED|node_modules/xmlchars|2.2.0|dev|-|MIT|0|SRI|version\|resolved\|integrity\|dev\|license|registry 提升安装位，安装/构建/测试/运行闭包所需|
|3304|3321|18|HOISTED|node_modules/yaml|2.9.0|dev|-|ISC|0|SRI|version\|resolved\|integrity\|dev\|license\|bin\|engines\|funding|registry 提升安装位，安装/构建/测试/运行闭包所需|

### 9.1 图与边（独立计数）

声明边合计 **582** = dependencies 398 + optionalDependencies 28 + peerDependencies 144 + devDependencies 12。  
peer 中 `peerDependenciesMeta.optional=true`：**37** 条（含 vitest browser/coverage/ui、vite 预处理器、rc-picker 日期库、canvas、msw 等）。其中 jsdom/vite/yaml/dayjs/`@types/react` 等由直依或他边满足；npm ls 仍列出 UNMET OPTIONAL，属有效图而非 37 项缺陷。  
optional 包 34：28 带 os/cpu；6 不带（`@emnapi/core|runtime|wasi-threads`、`@napi-rs/wasm-runtime`、`@tybys/wasm-util`、`tslib`）服务 wasm 可选链。  
`hasInstallScript`：仅 `fsevents` 与 `playwright/node_modules/fsevents`（os=darwin）。  
Arborist inventory 220 / invalid 0 / missing 0。

### 9.2 清单镜像

根：name/version/workspaces/devDependencies/engines 一致。根 `documentVersion` V0.01 不进 `packages[""]`；锁顶层 `documentVersion` V0.02。  
web：name/version/dependencies/devDependencies 一致。

---

## 10. 既有定稿 ID 对照（不凭票通过）

| ID | 本席当场核到的事实 | 工程缺陷 | 队列 |
|---|---|---|---|
| M004-001 | nanoid L1930–1948 = 3.3.18；本次 audit 不含 nanoid | 否（当前锁） | 否 |
| M004-002 | postcss L2050–2078 = 8.5.23，内部 nanoid `^3.3.16`；audit 不含 postcss | 否 | 否 |
| M004-003 | undici L3044–3053 = 7.29.0；audit 不含 undici | 否 | 否 |
| M004-004 | 顶层/根/web/link 与清单一致；documentVersion V0.02 | 否 | 否 |
| M004-005 | 220 条、582 边、116 engines、28 平台约束；Arborist 无 invalid | 否 | 否 |
| M004-006 | 217 HTTPS+sha512 parse；未下载 tarball | 否 | 否 |
| M004-007 | audit exit1 moderate2，同一 GHSA；见 §6 | 否（铁律 6） | 否 |

历史 001–003 曾是缺陷并已修出队：本席不改写为「从未存在」。

---

## 11. 本席新 finding

**空集。** 无新 GR 临时号。无未定稿缺陷。无添加项。无 HTML 自身问题需上报暂停。无未答 questions（`questions/` 无 M004 文件）。

不把 GHSA 另开新 ID：与 M004-007 同一公告、同一两包、同一判据。

---

## 12. 实际检查边界 / 未做

已做：锁全字段遍历、行覆盖、清单对照、Arborist loadVirtual、npm ls --package-lock-only、npm ci dry-run（含 offline）、npm audit --package-lock-only、ssri parse/checkData、GHSA HTML+API、vite/vitest 配置与 mocker 发行代码只读、CI yml 锁入口只读、`verify-no-legacy-api-prototype.mjs` 锁消费模式只读。

未做（明确）：真实 `npm ci` 写盘安装；`vite build`/`vitest run`/Maven/Playwright；217 tarball 下载验字节；用 npm 10.9.8 重放（本机 11.19.0）；生产部署。dry-run ≠ 安装/构建/测试通过。

---

## 13. HTML 问题 / 人闸

三 HTML 只读，本模块无 HTML 自身问题，无人闸（无添加询问、无 HTML 改申请、阶段 B 不跑 1.3 未跟踪闸）。

---

## 14. 源前后不变

开工与收工 `package-lock.json` SHA256 均为 `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef`。根/`apps/web` package.json SHA 亦不变。对抗只在 `/tmp/m004-adv-run`。本席未改仓库源。

---

## 15. 结论

单席 B5：**PASS**。

条件核对：本模块唯一文件有覆盖记录；18 GEN 已逐项核对；三线均执行；声称有命令/原文证据；无未答 questions；无未定稿缺陷；本报告第一行哈希为独立实算。不存在有条件通过。本结论不是模块生命周期 PASS，也不等于全仓/发布绿。

CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M004 B5-r1 外席全量审计报告（Grok）

版本：V0.03（仅本追加段。调用前本文件 **162406** 字节、SHA256 `951be0608835cd8ed9df191bee4646cb5aeafcc97dd07b96ec1514116f97ffd1` 的 V0.01+V0.02 前缀全部保留，不覆盖、不删、不改原字节。本次完整报告 = 本文件最后一个独占锁哈希行起至 EOF。）
席位：Grok（乙），只读外审
步骤：B5-r1（B2 定稿队列空，跳过新增 B3/B4；历史 B3 累计 **1**，本轮新增修复 **0**，次数不增不重置。文件名仍用历史 B3=1 的 r1 口径。）
模块：M004 — 治理门禁与 CI 基线／配置与工程入口／第2段（`plan/modules.md` V0.04；标记 `OVERSIZE-SINGLE-FILE`；计划有效行 3321）
控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02
正本：`/home/james/complex-management` `main`
唯一 FILES：`package-lock.json` **3321** 行 / **114258** 字节 / **220** `packages`
映射：锁模块映射行 `M004 → [GEN-001 … GEN-018]`，完整 18 GEN，无 HTML 派生 ID
ledger（只读顶部与本模块行，不写）：现文版本字段 V7.69；`LOCK-AMEND-COUNT: 1`；顶部段「当前M004：B2双PASS，B5-r1进行中」。M004 `IN_PROGRESS`，B3=1，方案修复 2，失败重跑 0。表行历史「旧 B5 双PASS」只作旧轮次文字，**不得继承旧 B5/B1/B2 PASS**，也不得把表行写成已收口。
B2 输入（只作范围与队列定位，不当成本次结论）：findings **V3.38** 顶部「当前 M004 新锁B2双PASS定稿」七行 001—007 全部技术性／当前工程缺陷否／队列否；self V0.09 / 本席 V0.07 双 PASS。本席 **不继承** 本席 B1 V0.06 PASS、B2 V0.07 PASS，也不继承本文件 V0.01/V0.02 PASS。
互盲：`ls` 见 `docs/audit-2026-09/modules/M004-b5-r1-self-raw.md` 存在、**190568** 字节、SHA256 `f8ee5cc230aa5dc736ba48d0644340e9d513071c3d3da84e56669838fa27a3d4`（存在性记录）。**未打开正文、不采信。** 未读本次 Codex 分析/原始 JSON。未派子代理；无 MCP；未 fetch/commit/push/worktree/部署/安装依赖/真实 `npm ci`/`npm install`/`npx`/`npm exec`/`audit fix`。未新 GET 公告页。未下载 tarball。
本席本票步骤结论：**PASS**
临时 GR 缺陷：**空集**（不分配 GR-01 起的 FAIL 号）
未答 questions：无（`questions/` 无 M004 文件）
新 HTML 自身问题：**无**（铁律 4 未触发；三份 HTML 对 `package-lock`/`npm ci`/`lockfileVersion` 无命中）
本席票不是模块生命周期 PASS，不是收口，不是全仓/预发布绿。

---

## 0. 技能预检（本环境实核 SKILL.md；V0.17 §6 收窄）

grilling / grill-me **自问自答**，不等操作员、不派子代理。Arc `audit` 会写 §7 外产物，本节点 **不调用**。i-have-adhd「列表≤5」与本票「18 GEN + 220 记录全表 + 首行独占哈希」冲突时，**任务优先**。

| 技能 | 实核 | 本次用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检；用户本批禁令优先 |
| using-arc | Arc `skills/using-arc/SKILL.md` | 仅路由；**未**跑 `arc:audit` |
| grilling / grill-me | mattpocock `productivity/grilling`；grill-me 仅转发 grilling | 对抗/第一性原理追问树；自问自答 |
| verification-before-completion | superpowers | 任何 PASS/FAIL 前用**本报告内本次命令**；不以 B1/B2 或本文件 V0.01/V0.02 结论替代 |
| codebase-design | mattpocock `engineering/codebase-design` | 锁作为 `npm ci --ignore-scripts` 的深模块接口（小接口=钉扎树；实现=220 包闭包）；删除测试：删锁则 CI 无法复现树 |
| diagnosing-bugs / systematic-debugging | 已读 | 只读源、建可失败命令；不加探针、不改码 |
| ponytail-review | ponytail `4.9.0` | 找可删；本步不修。嵌套同版 pluginutils 与平台 optional 不是死代码 |
| writing-for-agents | mattpocock | 本 raw 按下游可差集、可归因来写；**不写**模块收口 |
| graft | 仓库技能；本轮 `graft grep "package-lock.json"` 已跑 | 锁文件不在源码图内（配置）；命中仅 `tools/verify-no-legacy-api-prototype.mjs` L65–73（M008 消费入口，只核它读锁，不扩审该工具） |
| i-have-adhd | marketplace 路径存在 | 输出形态；首行仍按闸 1 独占哈希 |
| ubiquitous-language | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` 目录存在 | 本节点未读全文、**未调用**（FILES 无词表） |
| batch-grill-me | 本机技能树 **无独立 SKILL.md** | **缺口**，不冒充已调用 |

明确不用：`brainstorming`/`writing-plans`（阶段 A）；`ponytail` 本体（B3）；`requesting-code-review`/`receiving-code-review`（B4）；`resolving-merge-conflicts`；`git-guardrails-claude-code`（会写 hook）；§6 划掉的子代理 / worktree / research / ponytail-audit|debt|gain。未派子代理。未改 HTML / 源码 / 锁 / sidecar / ledger / findings / questions / 收口 / `package-lock.json`。

---

## 1. 本次实核 vs 同字节复用（必须分开写）

### 1.1 同字节复用（先自算确认，再复用阅读，不继承结论）

本席对唯一 FILES 独立 `sha256sum` / `wc` / `json` 头：

| 路径 | 换行行数 | 字节 | SHA256 |
|---|---:|---:|---|
| `package-lock.json` | 3321 | 114258 | `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef` |

`git ls-files -z -- package-lock.json | xargs -0 cat | wc -l` = **3321**。文件以换行结束。`git hash-object package-lock.json` = `HEAD:package-lock.json` = `63ecc802df8de76786dc69101c01abba6bb33d31`。对该文件 porcelain 空。inode=248255979 size=114258 mtime=1788623052，命令前后不变。

该 SHA/字节/行数与本席 **B1 V0.06** 所记录字节**逐字相同**。因此 **复用** V0.06 对 3321 行 / 220 记录的全文阅读（顶层键闭集、根/web 镜像字段、220 条路径与行跨、SRI 形态、声明边 582）。**复用的是阅读，不是 B1 PASS、不是 18 GEN 旧判断、不是 001—007 旧票。** 下列 §2–§8 全部按本 B5 重新判定，并在本会话重跑解析、原生图、SRI 形态、清单镜像、engine、workspace、optional、peer、错误传播与只读 audit。

对照只读（非本模块 FILES）：根 `package.json` SHA256 `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f`（147 行，与 B1 V0.06 同）；`apps/web/package.json` SHA256 `b8017d9782b4c00eac1c992204dad2b009e23db7c4c804737bdf8fcbc90b8f1f`（33 行，同）。

纳入：`plan/scope.txt` L2861 `package-lock.json`；`plan/module-union.txt` 出现 1 次（L6）。

旧本文件前缀：162406 字节 / `951be0608835cd8ed9df191bee4646cb5aeafcc97dd07b96ec1514116f97ffd1`，与派发声明逐字相同，全部保留。旧 V0.01/V0.02 所审源 SHA 为 `65254915…`、当时 `documentVersion` V0.01、当时 audit 绿——**与当前字节不同，结论不可继承**。

宿主：node v24.21.0；npm 11.19.0；Arborist 9.9.1；Linux x86_64。根清单 `packageManager=npm@10.9.8`（本机工具链差披露，不写成锁缺陷）。未 `corepack enable`。

JSON 顶层键闭集：`name, version, lockfileVersion, documentVersion, requires, packages`。`lockfileVersion=3`，`documentVersion=V0.02`（非 npm 协议字段），应用 `version=0.1.0`，`requires=true`。无顶层旧式 `dependencies` 树。packages 键 220，源文本分词 220 起行、重复键 0，与 `Object.keys` 顺序全等。

类计数：ROOT 1 + WS_PKG 1 + WS_LINK 1 + NESTED 6 + HOISTED 211 = **220**。行跨合计 3312（8–3319）+ 头 7 + 闭 2 = 3321。

### 1.2 锁全文哈希（本会话独立实算）

`python3 hashlib` 与 `sha256sum` 对 `docs/audit-2026-09/plan/criteria-lock.md`（**891182** 字节，文头版本 **V0.14**，判据总数 401）独立实算。结果**只写在本次报告首行独占字段**，正文不重复该独占字段名。sidecar 单行文本与之同值；sidecar **文件自身** SHA256（含换行）=`c4f8b2c53239206e0eb7b08c1a219049cf785bf9c233f93d66f7a8f0617c111f`（哈希的是 sidecar 文件，不是锁正文）。ledger 顶行与本席实算同值。`LOCK-AMEND-COUNT: 1`。工作区相对 `HEAD` 的 `criteria-lock.md` 是既有 LOCK-AMEND 未提交状态，本席未改该文件。本席不把四值校验写成已完成的模块收口动作。

```
git rev-parse --abbrev-ref HEAD / HEAD / origin/main
main
e456ba96c84185c8d39f18f548011daac48a6466
ebb1eb99c610df330c07d24278798c2d3611299a
```

分支 `main`。本席未 fetch。`## main...origin/main` 分叉（ahead 49 / behind 54，非本模块命题）。审计对象是主工作区**当前字节**。

### 1.3 本次命令实核（本会话跑出；不引用旧票 exit）

| 命令（cwd=正本，除非注明 `/tmp` 副本） | 真实 exit | 要点 | 不得等同 |
|---|---:|---|---|
| `sha256sum package-lock.json` | 0 | `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef` | — |
| JSON.parse + 分词 220 + 重复键 0 | 0 | 键序与对象键全等 | 不是安装 |
| 根/web 清单镜像 `deepEqual` | 0 | name/version/workspaces/devDependencies/engines 与根清单；name/version/dependencies/devDependencies 与 web 清单 | 不镜像 private/type/scripts/packageManager/documentVersion |
| 唯一 link | 0 | `packages["node_modules/apps-web"]={resolved:"apps/web",link:true}`，无 integrity | 规范：link 记录无其它字段 |
| Arborist `loadVirtual`（宿主 `@npmcli/arborist@9.9.1`） | 0 | inventory **220**；edgesOut **582**；invalid 0；extraneous 0；missing 0；必选缺失 0；invalidEdges 0；semver 范围失败 0；`toJSON().documentVersion==="V0.02"` 且 packages 仍 220 | **原生锁图 ≠ 磁盘安装态** |
| 声明边 | — | dep 398 + optional 28 + peer 144 + devDep 12 = **582** | 与 Arborist 出边同数 |
| SRI 形态 | 0 | 217 条 `sha512-` + base64 解码 **64** 字节；`ssri.parse` 217/217；`ssri.checkData(dummy, vitest.integrity)=false`；`ssri.parse("not-an-sri")=null` | **不是** tarball 字节等于 SRI；未下载 |
| `npm ls --package-lock-only --ignore-scripts --depth=0` | 0 | 根 `@playwright/test@1.61.0`、`yaml@2.9.0`、`apps-web@0.1.0 -> ./apps/web`，web 直依含 `vitest@4.1.9` | ≠ 已装树等于锁 |
| `npm ls --package-lock-only --ignore-scripts --all --json` | 0 | problems=null；walk 节点 583（含工作区展开，**不是** 220 包计数） | 不要把 583 写成锁坏了 220 |
| `npm ls --package-lock-only --ignore-scripts --all --omit=optional` | 0 | **29** 条 `UNMET OPTIONAL DEPENDENCY`（`@vitest/ui`、`happy-dom`、`@vitest/browser-*` 等） | omit 视图 ≠ 原生图缺陷；必选 peer 仍 0 |
| `npm ci --dry-run --ignore-scripts --offline` | 0 | added **188** / removed **186** | **不是**安装／构建／测试通过；相对**当前磁盘** `node_modules` 的计划 |
| `npm ci --dry-run --ignore-scripts` | 0 | 同上 | 同上 |
| `npm audit --package-lock-only --ignore-scripts --json` | **1** | moderate=2 high=0 critical=0 total=2；仅 GHSA-82fw-gwwq-j7x9；`@vitest/mocker`+`vitest` 均为 4.1.9 命中 `>=2.1.0 <4.1.11`；CWE-22；CVSS 5.9；fixAvailable true | **不是** audit 绿；fixAvailable **不是**本轮必须升级 |
| 同上 `--omit=dev` | 0 | total=0 | 只说明公告在 dev 闭包 |
| `node tools/verify-no-legacy-api-prototype.mjs` | 0 | `NO_LEGACY_API_PROTOTYPE_PASS` | 不是全仓门禁绿 |
| `npm help package-lock-json` | 0 | lockfileVersion 3；锁是清单的显现；integrity 为 SRI | **help ≠ 已安装** |
| `/tmp` NEG1 删 vitest 锁条目 | **1** | `EUSAGE` `Missing: vitest@4.1.10 from lock file` | 正本未改 |
| `/tmp` NEG2 vitest.version=`0.0.0` | **1** | `Invalid: lock file's vitest@0.0.0 does not satisfy vitest@4.1.10` | 正本未改 |
| `/tmp` NEG3 web 清单加 lodash | **1** | `ENOTCACHED` 请求 registry.npmjs.org/lodash | 正本未改 |
| `/tmp` NEG4 改坏 vitest integrity 后 dry-run | **0** | 虚拟计划不核字节 | dry-run **不是**完整性抽取 |
| `/tmp` NEG5 仅改锁内 `apps/web.dependencies.react` 为 `^18` | **0** | npm ci 同步看能否解出同一树，不是只比对工作区范围字符串 | 不把此绿写成范围已校验 |
| 跟踪 `apps/web` 上 `@vitest/(mocker\|browser)\|mockerPlugin\|interceptorPlugin` | rg **123** | 无匹配 | 发行 dist 里有 interceptorPlugin **≠** 项目注册了公开插件 |

engines 116；platform(os/cpu) 28；optional 旗标 34；`hasInstallScript` 仅 darwin `fsevents` 两处。optional 且无 os/cpu：**6**（`@emnapi/core|runtime|wasi-threads`、`@napi-rs/wasm-runtime`、`@tybys/wasm-util`、`tslib`）为 wasm 运行时可选。本机 linux x64 gnu：锁含 `lightningcss-linux-x64-gnu` 与 `@rolldown/binding-linux-x64-gnu`。当前 Node 24.21.0 对根 `engines.node=^22.13.0 \|\| >=24.0.0` 满足；对本锁 116 条 engines 字段 semver 失败 0。

217 resolved 全 `https://registry.npmjs.org/`；无 http／git／file（除 workspace link）。tarball name/version 去重 216（`@rolldown/pluginutils@1.0.1` 两处同版：工作区隔离 + rolldown 嵌套）。

### 1.4 原生锁图 vs 声明 vs 安装态（本 B5 强制分开）

| 层 | 本轮事实 | 不得写成 |
|---|---|---|
| 锁文件声明 | 220 记录；nanoid **3.3.18**；postcss **8.5.23**（`dependencies.nanoid=^3.3.16`）；undici **7.29.0**；vitest/mocker **4.1.9** | 「磁盘已是这些版本」 |
| 原生虚拟图 | Arborist loadVirtual 220/582/0 invalid/0 missing required | 「已经 npm ci 成功」 |
| 磁盘 `node_modules` | nanoid **3.3.12**；postcss **8.5.15**；undici **7.28.0**；vitest/mocker **4.1.9**（后二者与锁相同，**不是**三旧叶） | 「三旧叶已修且正在运行」 |
| dry-run 计划 | add nanoid 3.3.18 / remove 3.3.12；add postcss 8.5.23 / remove 8.5.15；add undici 7.29.0 / remove 7.28.0；added 188 / removed 186 | 「已经真正安装」或「锁自相矛盾」 |

三旧叶是**安装目录漂移**，排除范围。以锁 + `--package-lock-only` / dry-run 为准。dry-run 有计划更新，**不宣称已真正安装**。

范围外观察：`.scratch/module-recheck-20260908/unpublished-03acdb98/package-lock.json` 存在，**不是**本模块 FILES，不扩审，不把根锁当成双份。

CI 只读调用点（不扩审 M005/M007）：`.github/workflows/predeploy-gates.yml:23` 与 `ops/ci/predeploy-gates.yml:23` 均为 `npm ci --ignore-scripts`。工作流 `node-version: "22"` 属 M005。

---

## 2. 第一性原理线（2.3 三问；本 B5 重答）

### 2.1 这段东西为什么必须存在？

删掉 `package-lock.json`，三份业务 HTML **没有任何一条**立刻做不到——本轮对三 HTML 检索 `package-lock`/`npm ci`/`lockfileVersion` **无命中**。HTML 里的「依赖」是设备／拆分部署语义，不是 npm。`npm help package-lock-json`（类型 3）原文：锁是清单的显现，描述恰好生成的树，使同事、部署与 CI 安装同一组依赖。

本仓库 CI 实读入口（只核消费）：两份 predeploy 工作流均为 `npm ci --ignore-scripts`。无锁则该入口按 npm 文档无法做干净复现安装。

答不上 HTML 上位 **不等于该删**。按 2.1：与 HTML 无明文矛盾 → 不能指到某一 `td.k`/`h2`/`h3` 作为上位 → 无票种／角色／价格／审批等业务发明 → **技术性**。不因缺 HTML 锚点改判添加，也不新造「必须有锁」业务需求。

字段最小用途（对照 npm 11.19.0 Arborist：`this.data = { ...data, lockfileVersion, requires: true, packages, dependencies }` 保留未知顶层键；lfv≥3 的 `toJSON` 只剔除顶层 `dependencies`）：

| 字段 | 用途 | 本文件事实 |
|---|---|---|
| name / version | 应用标识与应用 SemVer | `entertainment-complex-management` / `0.1.0` |
| lockfileVersion | 协议 | `3` |
| documentVersion | **非协议**；文件自身 Vx.xx | `V0.02`；loadVirtual→toJSON 仍为 `V0.02`，packages 仍 220 |
| requires | v3 兼容旗标 | `true` |
| packages[""] | 根清单镜像 | 见 §1.3 |
| packages["apps/web"] | 工作区清单镜像 | 见 §1.3 |
| packages["node_modules/apps-web"] | 工作区链接 | `{resolved:"apps/web", link:true}` |
| 其余 217 | 钉扎 tarball | 全部 HTTPS npmjs + sha512 形态 |

### 2.2 声称的行为有没有被实际执行验证？

锁声称：给定此文件，npm 能得到确定虚拟树，且清单与锁可被 `npm ci` 判定为同步。

已验证：§1.3 全部命令。JSON 可解析；lfv=3；220 键与源分段一一对应；根／工作区声明镜像；唯一 link；Arborist 图闭合；`npm ls` lock-only 绿；`npm ci --dry-run` 可算出计划；缺／错版本负例 exit 1；SRI 形态与错误 payload 拒绝。

**未验证、不得伪称：** registry 可达与 tarball 字节等于 SRI（本轮未下载）；真实 `npm ci`／`npm install`；Vite／Vitest 业务测试；`verify:predeploy` 全绿。`npm ci --dry-run` 对故意改坏的 integrity 仍 exit 0——dry-run **不是**完整性抽取。help／dry 不当安装／编译／测试／SRI 抽取／业务验收。

### 2.3 它依赖的前提，哪一条是没验证过就当真的？

1. 安装时 registry.npmjs.org 返回与 `resolved` 相同对象 — **本次无网络证明 tarball 字节**。audit 只问公告库。
2. 对象 sha512 等于 `integrity` — 只验证了 SRI **形态**与错误 payload 拒绝，未取回 tarball。
3. 磁盘 `node_modules` 等于锁 — **明确不等于**（三旧叶）。以锁为准。
4. 本机 npm 11.19.0 与声明 npm@10.9.8 对全部语义逐字节相同 — 未 `corepack enable`。Arborist 11 已消费本锁；不新造 packageManager 门禁。
5. CI `node-version` 是否精确 ≥22.13.0 — 属 M005。本模块只核锁根 engines 与根清单逐字相等。当前进程 v24.21.0 满足。
6. `fixAvailable: true` 等于必须本轮升级 — **不成立**。见 §5 与铁律 6。
7. 未跑构建，不能把虚拟树 PASS 写成构建 PASS。
8. 发行包 dist 含 interceptorPlugin 等于项目已接线公开 mocker 通道 — **不成立**。跟踪源无注册。

---

## 3. 顺从式：18 GEN 逐项（本 B5 重判，不抄 B1 票）

判据全文以 `plan/criteria-lock.md` `## general` 为准。本轮独立核对。

| ID | 适用 | 证据（类型 3／4） | 本席 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 见 §2.1。重复键 0。嵌套 `@rolldown/pluginutils@1.0.1` 两处是工作区隔离，Arborist 无 extraneous。平台 optional 是 rolldown／lightningcss／fsevents 声明分支。ponytail：无可删业务层；不为假设场景另造抽象 | 满足 |
| GEN-002 可执行正确性 | 适用（配置：验证解析与虚拟树，不强制全构建） | loadVirtual 成功；`npm ls` lock-only exit 0；`npm ci --dry-run` exit 0；缺／错版本负例 exit 1 | 满足（边界：非构建） |
| GEN-003 测试有效性 | 部分适用 | 本模块无测试文件。用 npm 原生 ci/ls/audit/ssri 作可失败断言。不新造 TDD。不把「测试通过」替代锁证据。既有消费扫描 `verify-no-legacy-api-prototype` exit 0 不是安装 PASS | 满足（配置豁免） |
| GEN-004 信任边界 | 适用 | 217 resolved 全 HTTPS npmjs；无 http／git／file（除 workspace link）。SRI 形态 217/217。GHSA 见 §5：公开 mocker 通道未在当前入口注册。**未验证远端字节** | 字段满足；字节未证 |
| GEN-005 授权与最小权限 | 弱适用 | 锁不含主体／对象授权。无秘密（token 只在包名 css-tokenizer／js-tokens）。`hasInstallScript` 仅 darwin `fsevents` 两处；CI 已 `--ignore-scripts` | 不适用业务授权；无越权发明 |
| GEN-006 数据完整性 | 适用 | 引用边 582 可解析；必选缺失 0；link 仅 `apps/web`；根／web 镜像一致 | 满足 |
| GEN-007 并发与幂等 | 弱适用 | 静态 JSON。npm ci 按锁重装，不是业务幂等 | 不适用／无缺陷 |
| GEN-008 接口与模式一致性 | 适用 | 根／web 清单镜像字段一致。应用 version 0.1.0 ≠ documentVersion V0.02。模块范围内无第二套 lock。lfv3 无遗留顶层 dependencies | 满足 |
| GEN-009 错误处理与恢复 | 适用 | 缺条目／错版本：EUSAGE exit 1。清单多 lodash：offline ENOTCACHED exit 1。失败显式。dry-run 不核 SRI 已披露，不把 dry-run 绿写成完整性绿 | 满足（已划边界） |
| GEN-010 安全与敏感信息 | 适用 | 无明文密钥。audit 仅 GHSA-82fw-gwwq-j7x9（moderate×2）。nanoid／postcss／undici **不在**本次结果。锁无 apps/api／fastify／prisma／sqlite。见 §5 | 登记于既有 M004-007，不新开队列、不升级 |
| GEN-011 可观测与审计证据 | 适用 | audit JSON、ls、Arborist、ssri、负例、dry-run 计划可复核。不把 dry-run 写成已安装 | 满足 |
| GEN-012 资源与性能边界 | 适用 | optional 34，带 os／cpu 28；6 条 optional 无 os／cpu 为 wasm 运行时可选，不是无界放大。本机 linux x64 gnu 平台包在锁内 | 满足 |
| GEN-013 构建配置与依赖 | 主适用 | 锁可被 npm ci 计划；116 engines，当前 Node 24.21.0 对根范围满足，engineFail 0；工作区链接有效；CI 调用点只读 | 满足 |
| GEN-014 数据库迁移 | 不适用 | 无 SQL／迁移 | 无缺陷 |
| GEN-015 界面可用性 | 不适用 | 无 UI 文案。antd／react 只作为依赖名 | 无缺陷 |
| GEN-016 文档可验证性 | 适用 | documentVersion V0.02、3321 行、源 SHA 与 findings 当前源 SHA 一致。不把计划或旧 322／旧 B5 PASS 写成当前事实 | 满足 |
| GEN-017 业务来源分类 | 适用 | 全文件走冲突→细化→技术性→添加：三 HTML 无 npm 锁句且无矛盾 → 非冲突；无 HTML 上位条目 → 非细化；无业务实体／枚举／权限发明 → **技术性** | 满足 |
| GEN-018 第一性原理证据 | 适用 | 结论落到本轮命令输出、npm 规范原文、audit JSON。不引用旧席结论当依据，不以 B1/B2 PASS 代替本 B5 | 满足 |

18/18。无未核 ID。

---

## 4. 四分类（冲突 → 细化 → 技术性 → 添加；本 B5 重判）

**冲突：** 无。锁未改写票种、状态、价格、权限、删除审批等 HTML 明文。三 HTML 无 npm 锁句。

**细化：** 无具体 HTML 上位条目可把 220 条钉扎成业务规则。

**技术性：** 全 220 条均为安装／构建／测试／运行闭包，无业务发明。不要求每行 HTML 锚点。`documentVersion` 非协议字段，Arborist 保留，不参与安装解析。

**添加：** 无。无 2.2 人闸。无新 questions。

全局门禁（html-ledger-sync／entry-doc-baseline 等）属 M426 台账 + M008 工具 + M435／M436 入口文档，**不**证明本锁坏，也不因本模块 B5 步骤 PASS 称全仓／发布绿。本模块不为刷绿改 CSV／HTML，不重开 M001—M003。

ponytail-review：`Lean already.` 嵌套同版 pluginutils 与平台 optional 不是死代码。net: -0。

---

## 5. 既有 001—007 作 B5 输入（独立重判，不继承队列否）

findings V3.38 七行只作输入命题。本席按当前锁重新取证。不重开、不复用 ID、不拆 007。

| ID | 当前实证 | 四分类 | 工程缺陷 | 修复队列 | 本席处置 |
|---|---|---|---|---|---|
| M004-001 | 锁 `node_modules/nanoid` L1930–1948 版本 **3.3.18**；不在本次 audit。磁盘仍 3.3.12 | 技术性 | 否 | 否 | 历史已修记录保留；锁版本未回退。**不称安装目录已更新／已修运行** |
| M004-002 | 锁 `node_modules/postcss` L2050–2078 版本 **8.5.23**；`dependencies.nanoid=^3.3.16`，锁内 nanoid 3.3.18 满足。磁盘仍 8.5.15 | 技术性 | 否 | 否 | 同上 |
| M004-003 | 锁 `node_modules/undici` L3044–3053 版本 **7.29.0**；不在本次 audit。磁盘仍 7.28.0 | 技术性 | 否 | 否 | 同上 |
| M004-004 | 顶层／根／web／唯一 link 与两份清单镜像一致；documentVersion V0.02 非协议且 Arborist 保留 | 技术性 | 否 | 否 | 不改应用 SemVer、锁协议或文件自身版本 |
| M004-005 | 220 条／582 边／116 engine／28 平台／34 optional；原生图无 invalid／extraneous／必选缺失。`npm ls --omit=optional` 的 29 条 UNMET OPTIONAL 是 omit 视图 | 技术性 | 否 | 否 | 可选缺失、嵌套同版属于有效依赖图；不为去重重排锁 |
| M004-006 | 217 HTTPS + sha512 形态；ssri 错误 payload 拒绝；**未下载 tarball** | 技术性 | 否 | 否 | 只证明字段形态与校验行为，不借旧下载记录称供应链已验证 |
| M004-007 | 见下。audit **仍红**；当前入口未接公开插件 | 技术性 | 否 | 否 | 只登记，不升级 |

### 5.1 007 当前公告／入口（本轮实证）

命令：`npm audit --package-lock-only --ignore-scripts --json`（正本 cwd，锁 inode/mtime/SHA 运行前后不变）。

| 项 | 本轮事实 |
|---|---|
| 真实 exit | **1** |
| metadata | moderate=2 high=0 critical=0 total=2 |
| 包 | `@vitest/mocker`（indirect）与 `vitest`（direct），同一 GHSA-82fw-gwwq-j7x9 |
| 标题（audit JSON） | Vitest: Path Traversal / Arbitrary File Read via @vitest/mocker Redirect Mock |
| CWE / CVSS | CWE-22；5.9 `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N` |
| range | `>=2.1.0 <4.1.11`（vitest 另写 2.1.0-beta.1–4.1.10） |
| 锁内版本 | mocker L1121–1147 **4.1.9**；vitest L3132–3221 **4.1.9** — **命中** |
| 磁盘版本 | vitest 4.1.9、mocker 4.1.9（与锁相同；**不是**三旧叶） |
| fixAvailable | true（audit 字段；**不是**本轮必须升级的判据） |
| `--omit=dev` | exit 0，total=0 — 公告在 dev 闭包 |
| nanoid/postcss/undici | 未列入本次结果 |
| 本轮未 GET GitHub 公告页 | 证据停在 audit JSON（类型 4） |

入口（当前跟踪文件）：`apps/web/vite.config.ts` `plugins: [react()]`；`apps/web/vitest.config.ts` `environment: "jsdom"`、`plugins: [react()]`，无 `browser` 键。`git ls-files -- apps/web` 上 mockerPlugin／interceptorPlugin／`@vitest/browser` **无匹配**（rg 退出 123）。`apps/web/package.json` `dev`=`vite --host 0.0.0.0 --port 5173`，`test:unit`=`vitest run`。host 开关单独不等于公告触发通道。磁盘 `@vitest/mocker/dist/node.js` 含 `interceptorPlugin`、vitest dist 含 `hoistMocksPlugin`/`automockPlugin`：这是**库能力**，不是项目注册了公开插件。

铁律 6：首次正常跑会撞上吗？`npm ci`、`vitest run`（jsdom）、现配置下的 `vite --host 0.0.0.0` 都不会挂上该公开 WS 注册通道。要触发需再注册公开 mocker 插件或打开 browser 模式——不是首跑必撞。因此 **不把「必须升级到 4.1.11」写成工程缺陷=是／修复队列=是**，不执行任何依赖升级。

同时：不能写 npm audit 全绿；不能写公告误报；不能写永远不可触发；将来若接线 mockerPlugin／browser，不在本次批准范围。不新增强制门禁或提醒任务。

---

## 6. 对抗式线（grilling 自问自答）

**Q1** 是否用旧 B5／B1／B2 PASS 代替本 B5？  
➡️ 否。同源只复用阅读。本轮重跑命令并重判 18 GEN 与 001—007。旧 V0.01/V0.02 源 SHA 不同，不可继承。

**Q2** 是否宣称 audit 绿／公告误报／永不可触发？  
➡️ 否。audit exit 1 属实；4.1.9 命中属实。

**Q3** 是否凭标题自造「必须升级才 PASS」门禁？  
➡️ 否。铁律 6：首跑是否撞上。不擅升依赖。

**Q4** `vite --host 0.0.0.0` 是否单独等于触发通道？  
➡️ 否。缺 mockerPlugin／interceptorPlugin 注册，vitest 为 jsdom、无 browser。

**Q5** disk 旧 nanoid／postcss／undici 是否证明锁坏，或证明「已修运行」？  
➡️ 否。那是已装树漂移。锁版本未回退。dry-run 计划更新 ≠ 已安装。不称三旧叶已修运行。

**Q6** 原生图 220/582/0 invalid 是否等于磁盘已同步？  
➡️ 否。loadVirtual 不读安装目录版本。`npm ls --all --json` problems=null 仍是锁视图。

**Q7** `npm ls --omit=optional` 29 条 UNMET OPTIONAL 是否锁缺陷？  
➡️ 否。omit 视图把可选 peer 显示为 unmet；原生图必选缺失 0。`@vitest/ui`／`happy-dom`／browser 包均为 optional peer。

**Q8** dry-run 绿是否等于 SRI／业务测试通过？  
➡️ 否。NEG4 改坏 integrity 后 dry-run 仍 0。help 只解释格式。

**Q9** 只改锁内工作区 react 范围字符串 dry-run 仍 0，是否范围已校验？  
➡️ 否。npm ci 看的是能否解出同一树。

**Q10** 会不会把 B2「队列空」当成已经整模块 PASS？  
➡️ 不会。队列空只跳过新增 B3/B4。本票是 B5 全量重审。当前仍 `IN_PROGRESS`。本席 PASS 不是生命周期 PASS。

---

## 7. 连续 3321 行覆盖 + 全部 220 条

`wc -l` = 3321；`git ls-files -z -- package-lock.json | xargs -0 cat | wc -l` = 3321；文件以换行结束。

| 行号 | 内容 |
|---|---|
| 1–6 | `{` / name / version / lockfileVersion 3 / documentVersion V0.02 / requires |
| 7 | `"packages": {` |
| 8–21 | 根 `""` |
| 22–44 | `apps/web` |
| 45–3319 | 其余 218 条（下表；行数合计 3312，含根与 web） |
| 3320 | `packages` 闭 `}` |
| 3321 | 根闭 `}` |

7 + 3312 + 2 = 3321。全文件无未覆盖行。无抽样冒充整文件。四分类：下表每条均为技术性（安装闭包，无业务发明）。

列：起、止、行数、类、路径、版本、标志、平台、许可证、声明边数、完整性、字段。完整性：217 个 tarball 为 sha512 可解析；ROOT／WS_PKG／WS_LINK 协议不要求 SRI。

| 8 | 21 | 14 | ROOT | "" | 0.1.0 | - | - | - | 2 | - | name,version,workspaces,devDependencies,engines |
| 22 | 44 | 23 | WS_PKG | apps/web | 0.1.0 | - | - | - | 15 | - | name,version,dependencies,devDependencies |
| 45 | 51 | 7 | NESTED | apps/web/node_modules/@rolldown/pluginutils | 1.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 52 | 77 | 26 | NESTED | apps/web/node_modules/@vitejs/plugin-react | 6.0.2 | dev | - | MIT | 4 | sha512 | version,resolved,integrity,dev,license,dependencies,engines,peerDependencies,peerDependenciesMeta |
| 78 | 84 | 7 | HOISTED | node_modules/@adobe/css-tools | 4.5.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 85 | 93 | 9 | HOISTED | node_modules/@ant-design/colors | 7.2.1 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies |
| 94 | 112 | 19 | HOISTED | node_modules/@ant-design/cssinjs | 1.24.0 | - | - | MIT | 9 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 113 | 127 | 15 | HOISTED | node_modules/@ant-design/cssinjs-utils | 1.1.3 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 128 | 139 | 12 | HOISTED | node_modules/@ant-design/fast-color | 2.0.6 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies,engines |
| 140 | 159 | 20 | HOISTED | node_modules/@ant-design/icons | 5.6.1 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 160 | 165 | 6 | HOISTED | node_modules/@ant-design/icons-svg | 4.4.2 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 166 | 181 | 16 | HOISTED | node_modules/@ant-design/react-slick | 1.1.2 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 182 | 198 | 17 | HOISTED | node_modules/@asamuzakjp/css-color | 5.1.11 | dev | - | MIT | 5 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 199 | 215 | 17 | HOISTED | node_modules/@asamuzakjp/dom-selector | 7.1.1 | dev | - | MIT | 5 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 216 | 225 | 10 | HOISTED | node_modules/@asamuzakjp/generational-cache | 1.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 226 | 232 | 7 | HOISTED | node_modules/@asamuzakjp/nwsapi | 2.3.9 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 233 | 248 | 16 | HOISTED | node_modules/@babel/code-frame | 7.29.7 | dev+peer | - | MIT | 3 | sha512 | version,resolved,integrity,dev,license,peer,dependencies,engines |
| 249 | 259 | 11 | HOISTED | node_modules/@babel/helper-validator-identifier | 7.29.7 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer,engines |
| 260 | 268 | 9 | HOISTED | node_modules/@babel/runtime | 7.29.7 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license,engines |
| 269 | 281 | 13 | HOISTED | node_modules/@bramus/specificity | 2.4.2 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,bin |
| 282 | 301 | 20 | HOISTED | node_modules/@csstools/color-helpers | 6.0.2 | dev | - | MIT-0 | 0 | sha512 | version,resolved,integrity,dev,funding,license,engines |
| 302 | 325 | 24 | HOISTED | node_modules/@csstools/css-calc | 3.2.1 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,funding,license,engines,peerDependencies |
| 326 | 353 | 28 | HOISTED | node_modules/@csstools/css-color-parser | 4.1.7 | dev | - | MIT | 4 | sha512 | version,resolved,integrity,dev,funding,license,dependencies,engines,peerDependencies |
| 354 | 376 | 23 | HOISTED | node_modules/@csstools/css-parser-algorithms | 4.0.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,funding,license,engines,peerDependencies |
| 377 | 401 | 25 | HOISTED | node_modules/@csstools/css-syntax-patches-for-csstree | 1.1.5 | dev | - | MIT-0 | 1 | sha512 | version,resolved,integrity,dev,funding,license,peerDependencies,peerDependenciesMeta |
| 402 | 421 | 20 | HOISTED | node_modules/@csstools/css-tokenizer | 4.0.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,funding,license,engines |
| 422 | 433 | 12 | HOISTED | node_modules/@emnapi/core | 1.10.0 | dev+optional | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,optional,dependencies |
| 434 | 444 | 11 | HOISTED | node_modules/@emnapi/runtime | 1.10.0 | dev+optional | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,optional,dependencies |
| 445 | 455 | 11 | HOISTED | node_modules/@emnapi/wasi-threads | 1.2.1 | dev+optional | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,optional,dependencies |
| 456 | 461 | 6 | HOISTED | node_modules/@emotion/hash | 0.8.0 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 462 | 467 | 6 | HOISTED | node_modules/@emotion/unitless | 0.7.5 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 468 | 485 | 18 | HOISTED | node_modules/@exodus/bytes | 1.15.1 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,engines,peerDependencies,peerDependenciesMeta |
| 486 | 492 | 7 | HOISTED | node_modules/@jridgewell/sourcemap-codec | 1.5.5 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 493 | 511 | 19 | HOISTED | node_modules/@napi-rs/wasm-runtime | 1.1.5 | dev+optional | - | MIT | 3 | sha512 | version,resolved,integrity,dev,license,optional,dependencies,funding,peerDependencies |
| 512 | 521 | 10 | HOISTED | node_modules/@oxc-project/types | 0.133.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,funding |
| 522 | 537 | 16 | HOISTED | node_modules/@playwright/test | 1.61.0 | dev | - | Apache-2.0 | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines |
| 538 | 549 | 12 | HOISTED | node_modules/@rc-component/async-validator | 5.1.0 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies,engines |
| 550 | 565 | 16 | HOISTED | node_modules/@rc-component/color-picker | 2.0.1 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 566 | 579 | 14 | HOISTED | node_modules/@rc-component/context | 1.4.0 | - | - | MIT | 4 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 580 | 591 | 12 | HOISTED | node_modules/@rc-component/mini-decimal | 1.1.3 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies,engines |
| 592 | 609 | 18 | HOISTED | node_modules/@rc-component/mutate-observer | 1.1.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 610 | 627 | 18 | HOISTED | node_modules/@rc-component/portal | 1.1.2 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 628 | 643 | 16 | HOISTED | node_modules/@rc-component/qrcode | 1.1.1 | - | - | MIT | 3 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 644 | 663 | 20 | HOISTED | node_modules/@rc-component/tour | 1.15.1 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 664 | 684 | 21 | HOISTED | node_modules/@rc-component/trigger | 2.3.1 | - | - | MIT | 8 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 685 | 701 | 17 | HOISTED | node_modules/@rolldown/binding-android-arm64 | 1.0.3 | dev+optional | os=android;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 702 | 718 | 17 | HOISTED | node_modules/@rolldown/binding-darwin-arm64 | 1.0.3 | dev+optional | os=darwin;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 719 | 735 | 17 | HOISTED | node_modules/@rolldown/binding-darwin-x64 | 1.0.3 | dev+optional | os=darwin;cpu=x64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 736 | 752 | 17 | HOISTED | node_modules/@rolldown/binding-freebsd-x64 | 1.0.3 | dev+optional | os=freebsd;cpu=x64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 753 | 769 | 17 | HOISTED | node_modules/@rolldown/binding-linux-arm-gnueabihf | 1.0.3 | dev+optional | os=linux;cpu=arm | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 770 | 786 | 17 | HOISTED | node_modules/@rolldown/binding-linux-arm64-gnu | 1.0.3 | dev+optional | os=linux;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 787 | 803 | 17 | HOISTED | node_modules/@rolldown/binding-linux-arm64-musl | 1.0.3 | dev+optional | os=linux;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 804 | 820 | 17 | HOISTED | node_modules/@rolldown/binding-linux-ppc64-gnu | 1.0.3 | dev+optional | os=linux;cpu=ppc64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 821 | 837 | 17 | HOISTED | node_modules/@rolldown/binding-linux-s390x-gnu | 1.0.3 | dev+optional | os=linux;cpu=s390x | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 838 | 854 | 17 | HOISTED | node_modules/@rolldown/binding-linux-x64-gnu | 1.0.3 | dev+optional | os=linux;cpu=x64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 855 | 871 | 17 | HOISTED | node_modules/@rolldown/binding-linux-x64-musl | 1.0.3 | dev+optional | os=linux;cpu=x64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 872 | 888 | 17 | HOISTED | node_modules/@rolldown/binding-openharmony-arm64 | 1.0.3 | dev+optional | os=openharmony;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 889 | 907 | 19 | HOISTED | node_modules/@rolldown/binding-wasm32-wasi | 1.0.3 | dev+optional | cpu=wasm32 | MIT | 3 | sha512 | version,resolved,integrity,cpu,dev,license,optional,dependencies,engines |
| 908 | 924 | 17 | HOISTED | node_modules/@rolldown/binding-win32-arm64-msvc | 1.0.3 | dev+optional | os=win32;cpu=arm64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 925 | 941 | 17 | HOISTED | node_modules/@rolldown/binding-win32-x64-msvc | 1.0.3 | dev+optional | os=win32;cpu=x64 | MIT | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines |
| 942 | 948 | 7 | HOISTED | node_modules/@standard-schema/spec | 1.1.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 949 | 969 | 21 | HOISTED | node_modules/@testing-library/dom | 10.4.1 | dev+peer | - | MIT | 8 | sha512 | version,resolved,integrity,dev,license,peer,dependencies,engines |
| 970 | 989 | 20 | HOISTED | node_modules/@testing-library/jest-dom | 6.9.1 | dev | - | MIT | 6 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 990 | 996 | 7 | NESTED | node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api | 0.6.3 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 997 | 1024 | 28 | HOISTED | node_modules/@testing-library/react | 16.3.2 | dev | - | MIT | 6 | sha512 | version,resolved,integrity,dev,license,dependencies,engines,peerDependencies,peerDependenciesMeta |
| 1025 | 1038 | 14 | HOISTED | node_modules/@testing-library/user-event | 14.6.1 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,engines,peerDependencies |
| 1039 | 1049 | 11 | HOISTED | node_modules/@tybys/wasm-util | 0.10.2 | dev+optional | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,optional,dependencies |
| 1050 | 1057 | 8 | HOISTED | node_modules/@types/aria-query | 5.0.4 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer |
| 1058 | 1068 | 11 | HOISTED | node_modules/@types/chai | 5.2.3 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1069 | 1075 | 7 | HOISTED | node_modules/@types/deep-eql | 4.0.2 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1076 | 1082 | 7 | HOISTED | node_modules/@types/estree | 1.0.9 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1083 | 1092 | 10 | HOISTED | node_modules/@types/react | 19.2.16 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1093 | 1102 | 10 | HOISTED | node_modules/@types/react-dom | 19.2.3 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,peerDependencies |
| 1103 | 1120 | 18 | HOISTED | node_modules/@vitest/expect | 4.1.9 | dev | - | MIT | 6 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1121 | 1147 | 27 | HOISTED | node_modules/@vitest/mocker | 4.1.9 | dev | - | MIT | 5 | sha512 | version,resolved,integrity,dev,license,dependencies,funding,peerDependencies,peerDependenciesMeta |
| 1148 | 1160 | 13 | HOISTED | node_modules/@vitest/pretty-format | 4.1.9 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1161 | 1174 | 14 | HOISTED | node_modules/@vitest/runner | 4.1.9 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1175 | 1190 | 16 | HOISTED | node_modules/@vitest/snapshot | 4.1.9 | dev | - | MIT | 4 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1191 | 1200 | 10 | HOISTED | node_modules/@vitest/spy | 4.1.9 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,funding |
| 1201 | 1215 | 15 | HOISTED | node_modules/@vitest/utils | 4.1.9 | dev | - | MIT | 3 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1216 | 1226 | 11 | HOISTED | node_modules/ansi-regex | 5.0.1 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer,engines |
| 1227 | 1240 | 14 | HOISTED | node_modules/ansi-styles | 5.2.0 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer,engines,funding |
| 1241 | 1305 | 65 | HOISTED | node_modules/antd | 5.29.3 | - | - | MIT | 51 | sha512 | version,resolved,integrity,license,dependencies,funding,peerDependencies |
| 1306 | 1309 | 4 | WS_LINK | node_modules/apps-web | - | link | - | - | 0 | - | resolved,link |
| 1310 | 1319 | 10 | HOISTED | node_modules/aria-query | 5.3.0 | dev | - | Apache-2.0 | 1 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1320 | 1329 | 10 | HOISTED | node_modules/assertion-error | 2.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1330 | 1339 | 10 | HOISTED | node_modules/bidi-js | 1.0.3 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1340 | 1349 | 10 | HOISTED | node_modules/chai | 6.2.2 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1350 | 1355 | 6 | HOISTED | node_modules/classnames | 2.5.1 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 1356 | 1361 | 6 | HOISTED | node_modules/compute-scroll-into-view | 3.1.1 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 1362 | 1368 | 7 | HOISTED | node_modules/convert-source-map | 2.0.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1369 | 1377 | 9 | HOISTED | node_modules/copy-to-clipboard | 3.3.3 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies |
| 1378 | 1391 | 14 | HOISTED | node_modules/css-tree | 3.2.1 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 1392 | 1398 | 7 | HOISTED | node_modules/css.escape | 1.5.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1399 | 1404 | 6 | HOISTED | node_modules/csstype | 3.2.3 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 1405 | 1418 | 14 | HOISTED | node_modules/data-urls | 7.0.0 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 1419 | 1424 | 6 | HOISTED | node_modules/dayjs | 1.11.21 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 1425 | 1431 | 7 | HOISTED | node_modules/decimal.js | 10.6.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1432 | 1441 | 10 | HOISTED | node_modules/dequal | 2.0.3 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1442 | 1451 | 10 | HOISTED | node_modules/detect-libc | 2.1.2 | dev | - | Apache-2.0 | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1452 | 1459 | 8 | HOISTED | node_modules/dom-accessibility-api | 0.5.16 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer |
| 1460 | 1472 | 13 | HOISTED | node_modules/entities | 8.0.0 | dev | - | BSD-2-Clause | 0 | sha512 | version,resolved,integrity,dev,license,engines,funding |
| 1473 | 1479 | 7 | HOISTED | node_modules/es-module-lexer | 2.1.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1480 | 1489 | 10 | HOISTED | node_modules/estree-walker | 3.0.3 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1490 | 1499 | 10 | HOISTED | node_modules/expect-type | 1.3.0 | dev | - | Apache-2.0 | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1500 | 1517 | 18 | HOISTED | node_modules/fdir | 6.5.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,engines,peerDependencies,peerDependenciesMeta |
| 1518 | 1532 | 15 | HOISTED | node_modules/fsevents | 2.3.3 | dev+optional+hasInstallScript | os=darwin | MIT | 0 | sha512 | version,resolved,integrity,dev,hasInstallScript,license,optional,os,engines |
| 1533 | 1545 | 13 | HOISTED | node_modules/html-encoding-sniffer | 6.0.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 1546 | 1555 | 10 | HOISTED | node_modules/indent-string | 4.0.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1556 | 1562 | 7 | HOISTED | node_modules/is-potential-custom-element-name | 1.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1563 | 1570 | 8 | HOISTED | node_modules/js-tokens | 4.0.0 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer |
| 1571 | 1611 | 41 | HOISTED | node_modules/jsdom | 29.1.1 | dev | - | MIT | 22 | sha512 | version,resolved,integrity,dev,license,dependencies,engines,peerDependencies,peerDependenciesMeta |
| 1612 | 1620 | 9 | HOISTED | node_modules/json2mq | 0.2.0 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies |
| 1621 | 1650 | 30 | HOISTED | node_modules/lightningcss | 1.32.0 | dev | - | MPL-2.0 | 12 | sha512 | version,resolved,integrity,dev,license,dependencies,engines,funding,optionalDependencies |
| 1651 | 1671 | 21 | HOISTED | node_modules/lightningcss-android-arm64 | 1.32.0 | dev+optional | os=android;cpu=arm64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1672 | 1692 | 21 | HOISTED | node_modules/lightningcss-darwin-arm64 | 1.32.0 | dev+optional | os=darwin;cpu=arm64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1693 | 1713 | 21 | HOISTED | node_modules/lightningcss-darwin-x64 | 1.32.0 | dev+optional | os=darwin;cpu=x64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1714 | 1734 | 21 | HOISTED | node_modules/lightningcss-freebsd-x64 | 1.32.0 | dev+optional | os=freebsd;cpu=x64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1735 | 1755 | 21 | HOISTED | node_modules/lightningcss-linux-arm-gnueabihf | 1.32.0 | dev+optional | os=linux;cpu=arm | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1756 | 1776 | 21 | HOISTED | node_modules/lightningcss-linux-arm64-gnu | 1.32.0 | dev+optional | os=linux;cpu=arm64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1777 | 1797 | 21 | HOISTED | node_modules/lightningcss-linux-arm64-musl | 1.32.0 | dev+optional | os=linux;cpu=arm64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1798 | 1818 | 21 | HOISTED | node_modules/lightningcss-linux-x64-gnu | 1.32.0 | dev+optional | os=linux;cpu=x64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1819 | 1839 | 21 | HOISTED | node_modules/lightningcss-linux-x64-musl | 1.32.0 | dev+optional | os=linux;cpu=x64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1840 | 1860 | 21 | HOISTED | node_modules/lightningcss-win32-arm64-msvc | 1.32.0 | dev+optional | os=win32;cpu=arm64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1861 | 1881 | 21 | HOISTED | node_modules/lightningcss-win32-x64-msvc | 1.32.0 | dev+optional | os=win32;cpu=x64 | MPL-2.0 | 0 | sha512 | version,resolved,integrity,cpu,dev,license,optional,os,engines,funding |
| 1882 | 1891 | 10 | HOISTED | node_modules/lru-cache | 11.5.1 | dev | - | BlueOak-1.0.0 | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1892 | 1902 | 11 | HOISTED | node_modules/lz-string | 1.5.0 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer,bin |
| 1903 | 1912 | 10 | HOISTED | node_modules/magic-string | 0.30.21 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies |
| 1913 | 1919 | 7 | HOISTED | node_modules/mdn-data | 2.27.1 | dev | - | CC0-1.0 | 0 | sha512 | version,resolved,integrity,dev,license |
| 1920 | 1929 | 10 | HOISTED | node_modules/min-indent | 1.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 1930 | 1948 | 19 | HOISTED | node_modules/nanoid | 3.3.18 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,funding,license,bin,engines |
| 1949 | 1962 | 14 | HOISTED | node_modules/obug | 2.1.3 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,funding,license,engines |
| 1963 | 1975 | 13 | HOISTED | node_modules/parse5 | 8.0.1 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,funding |
| 1976 | 1982 | 7 | HOISTED | node_modules/pathe | 2.0.3 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 1983 | 1989 | 7 | HOISTED | node_modules/picocolors | 1.1.1 | dev | - | ISC | 0 | sha512 | version,resolved,integrity,dev,license |
| 1990 | 2002 | 13 | HOISTED | node_modules/picomatch | 4.0.4 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines,funding |
| 2003 | 2021 | 19 | HOISTED | node_modules/playwright | 1.61.0 | dev | - | Apache-2.0 | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines,optionalDependencies |
| 2022 | 2034 | 13 | HOISTED | node_modules/playwright-core | 1.61.0 | dev | - | Apache-2.0 | 0 | sha512 | version,resolved,integrity,dev,license,bin,engines |
| 2035 | 2049 | 15 | NESTED | node_modules/playwright/node_modules/fsevents | 2.3.2 | dev+optional+hasInstallScript | os=darwin | MIT | 0 | sha512 | version,resolved,integrity,dev,hasInstallScript,license,optional,os,engines |
| 2050 | 2078 | 29 | HOISTED | node_modules/postcss | 8.5.23 | dev | - | MIT | 3 | sha512 | version,resolved,integrity,dev,funding,license,dependencies,engines |
| 2079 | 2094 | 16 | HOISTED | node_modules/pretty-format | 27.5.1 | dev+peer | - | MIT | 3 | sha512 | version,resolved,integrity,dev,license,peer,dependencies,engines |
| 2095 | 2102 | 8 | NESTED | node_modules/pretty-format/node_modules/react-is | 17.0.2 | dev+peer | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,peer |
| 2103 | 2112 | 10 | HOISTED | node_modules/punycode | 2.3.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 2113 | 2121 | 9 | HOISTED | node_modules/qrcode.react | 4.2.0 | - | - | ISC | 1 | sha512 | version,resolved,integrity,license,peerDependencies |
| 2122 | 2138 | 17 | HOISTED | node_modules/rc-cascader | 3.34.0 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2139 | 2153 | 15 | HOISTED | node_modules/rc-checkbox | 3.5.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2154 | 2169 | 16 | HOISTED | node_modules/rc-collapse | 3.9.0 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2170 | 2186 | 17 | HOISTED | node_modules/rc-dialog | 9.6.0 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2187 | 2203 | 17 | HOISTED | node_modules/rc-drawer | 7.3.0 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2204 | 2219 | 16 | HOISTED | node_modules/rc-dropdown | 4.2.1 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2220 | 2237 | 18 | HOISTED | node_modules/rc-field-form | 2.7.1 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2238 | 2255 | 18 | HOISTED | node_modules/rc-image | 7.12.0 | - | - | MIT | 8 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2256 | 2270 | 15 | HOISTED | node_modules/rc-input | 1.8.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2271 | 2287 | 17 | HOISTED | node_modules/rc-input-number | 9.5.0 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2288 | 2306 | 19 | HOISTED | node_modules/rc-mentions | 2.20.0 | - | - | MIT | 9 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2307 | 2324 | 18 | HOISTED | node_modules/rc-menu | 9.16.1 | - | - | MIT | 8 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2325 | 2339 | 15 | HOISTED | node_modules/rc-motion | 2.9.5 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2340 | 2358 | 19 | HOISTED | node_modules/rc-notification | 5.6.4 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2359 | 2374 | 16 | HOISTED | node_modules/rc-overflow | 1.5.0 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2375 | 2389 | 15 | HOISTED | node_modules/rc-pagination | 5.1.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2390 | 2428 | 39 | HOISTED | node_modules/rc-picker | 4.11.3 | - | - | MIT | 12 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies,peerDependenciesMeta |
| 2429 | 2443 | 15 | HOISTED | node_modules/rc-progress | 4.0.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2444 | 2461 | 18 | HOISTED | node_modules/rc-rate | 2.13.1 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2462 | 2477 | 16 | HOISTED | node_modules/rc-resize-observer | 1.4.3 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2478 | 2493 | 16 | HOISTED | node_modules/rc-segmented | 2.7.1 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2494 | 2515 | 22 | HOISTED | node_modules/rc-select | 14.16.8 | - | - | MIT | 9 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2516 | 2533 | 18 | HOISTED | node_modules/rc-slider | 11.1.9 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2534 | 2551 | 18 | HOISTED | node_modules/rc-steps | 6.0.1 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2552 | 2566 | 15 | HOISTED | node_modules/rc-switch | 4.1.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2567 | 2587 | 21 | HOISTED | node_modules/rc-table | 7.54.0 | - | - | MIT | 8 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2588 | 2609 | 22 | HOISTED | node_modules/rc-tabs | 15.7.0 | - | - | MIT | 9 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2610 | 2626 | 17 | HOISTED | node_modules/rc-textarea | 1.10.2 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2627 | 2642 | 16 | HOISTED | node_modules/rc-tooltip | 6.4.0 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2643 | 2662 | 20 | HOISTED | node_modules/rc-tree | 5.13.1 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2663 | 2679 | 17 | HOISTED | node_modules/rc-tree-select | 5.27.0 | - | - | MIT | 7 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2680 | 2694 | 15 | HOISTED | node_modules/rc-upload | 4.11.0 | - | - | MIT | 5 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2695 | 2708 | 14 | HOISTED | node_modules/rc-util | 5.44.4 | - | - | MIT | 4 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2709 | 2727 | 19 | HOISTED | node_modules/rc-virtual-list | 3.19.2 | - | - | MIT | 6 | sha512 | version,resolved,integrity,license,dependencies,engines,peerDependencies |
| 2728 | 2736 | 9 | HOISTED | node_modules/react | 19.2.7 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license,engines |
| 2737 | 2748 | 12 | HOISTED | node_modules/react-dom | 19.2.7 | - | - | MIT | 2 | sha512 | version,resolved,integrity,license,dependencies,peerDependencies |
| 2749 | 2754 | 6 | HOISTED | node_modules/react-is | 18.3.1 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2755 | 2768 | 14 | HOISTED | node_modules/redent | 3.0.0 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 2769 | 2778 | 10 | HOISTED | node_modules/require-from-string | 2.0.2 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 2779 | 2784 | 6 | HOISTED | node_modules/resize-observer-polyfill | 1.5.1 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2785 | 2818 | 34 | HOISTED | node_modules/rolldown | 1.0.3 | dev | - | MIT | 17 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines,optionalDependencies |
| 2819 | 2825 | 7 | NESTED | node_modules/rolldown/node_modules/@rolldown/pluginutils | 1.0.1 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2826 | 2838 | 13 | HOISTED | node_modules/saxes | 6.0.0 | dev | - | ISC | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 2839 | 2844 | 6 | HOISTED | node_modules/scheduler | 0.27.0 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2845 | 2853 | 9 | HOISTED | node_modules/scroll-into-view-if-needed | 3.1.0 | - | - | MIT | 1 | sha512 | version,resolved,integrity,license,dependencies |
| 2854 | 2860 | 7 | HOISTED | node_modules/siginfo | 2.0.0 | dev | - | ISC | 0 | sha512 | version,resolved,integrity,dev,license |
| 2861 | 2870 | 10 | HOISTED | node_modules/source-map-js | 1.2.1 | dev | - | BSD-3-Clause | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 2871 | 2877 | 7 | HOISTED | node_modules/stackback | 0.0.2 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2878 | 2884 | 7 | HOISTED | node_modules/std-env | 4.1.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2885 | 2890 | 6 | HOISTED | node_modules/string-convert | 0.2.1 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2891 | 2903 | 13 | HOISTED | node_modules/strip-indent | 3.0.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 2904 | 2909 | 6 | HOISTED | node_modules/stylis | 4.4.0 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2910 | 2916 | 7 | HOISTED | node_modules/symbol-tree | 3.2.4 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2917 | 2925 | 9 | HOISTED | node_modules/throttle-debounce | 5.0.2 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license,engines |
| 2926 | 2932 | 7 | HOISTED | node_modules/tinybench | 2.9.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2933 | 2942 | 10 | HOISTED | node_modules/tinyexec | 1.2.4 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 2943 | 2959 | 17 | HOISTED | node_modules/tinyglobby | 0.2.17 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,engines,funding |
| 2960 | 2969 | 10 | HOISTED | node_modules/tinyrainbow | 3.1.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 2970 | 2982 | 13 | HOISTED | node_modules/tldts | 7.4.3 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,bin |
| 2983 | 2989 | 7 | HOISTED | node_modules/tldts-core | 7.4.3 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 2990 | 2995 | 6 | HOISTED | node_modules/toggle-selection | 1.0.6 | - | - | MIT | 0 | sha512 | version,resolved,integrity,license |
| 2996 | 3008 | 13 | HOISTED | node_modules/tough-cookie | 6.0.1 | dev | - | BSD-3-Clause | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 3009 | 3021 | 13 | HOISTED | node_modules/tr46 | 6.0.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 3022 | 3029 | 8 | HOISTED | node_modules/tslib | 2.8.1 | dev+optional | - | 0BSD | 0 | sha512 | version,resolved,integrity,dev,license,optional |
| 3030 | 3043 | 14 | HOISTED | node_modules/typescript | 5.9.3 | dev | - | Apache-2.0 | 0 | sha512 | version,resolved,integrity,dev,license,bin,engines |
| 3044 | 3053 | 10 | HOISTED | node_modules/undici | 7.29.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 3054 | 3131 | 78 | HOISTED | node_modules/vite | 8.0.16 | dev | - | MIT | 18 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines,funding,optionalDependencies,peerDependencies,peerDependenciesMeta |
| 3132 | 3221 | 90 | HOISTED | node_modules/vitest | 4.1.9 | dev | - | MIT | 32 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines,funding,peerDependencies,peerDependenciesMeta |
| 3222 | 3234 | 13 | HOISTED | node_modules/w3c-xmlserializer | 5.0.0 | dev | - | MIT | 1 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 3235 | 3244 | 10 | HOISTED | node_modules/webidl-conversions | 8.0.1 | dev | - | BSD-2-Clause | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 3245 | 3254 | 10 | HOISTED | node_modules/whatwg-mimetype | 5.0.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 3255 | 3269 | 15 | HOISTED | node_modules/whatwg-url | 16.0.1 | dev | - | MIT | 3 | sha512 | version,resolved,integrity,dev,license,dependencies,engines |
| 3270 | 3286 | 17 | HOISTED | node_modules/why-is-node-running | 2.3.0 | dev | - | MIT | 2 | sha512 | version,resolved,integrity,dev,license,dependencies,bin,engines |
| 3287 | 3296 | 10 | HOISTED | node_modules/xml-name-validator | 5.0.0 | dev | - | Apache-2.0 | 0 | sha512 | version,resolved,integrity,dev,license,engines |
| 3297 | 3303 | 7 | HOISTED | node_modules/xmlchars | 2.2.0 | dev | - | MIT | 0 | sha512 | version,resolved,integrity,dev,license |
| 3304 | 3319 | 16 | HOISTED | node_modules/yaml | 2.9.0 | dev | - | ISC | 0 | sha512 | version,resolved,integrity,dev,license,bin,engines,funding |

220/220。结构段已通读：头、根与 web 镜像、嵌套 plugin-react、rolldown 平台 optional、vitest／mocker、nanoid 3.3.18、postcss 8.5.23、undici 7.29.0、antd／react、jsdom、lightningcss 平台包、rc-*、收尾 yaml。其余条目由 JSON 全字段遍历 + 行号表覆盖。

---

## 8. 三线执行记录

| 线 | 状态 |
|---|---|
| 第一性原理 | §2 三问均落本轮类型 3/4 证据 |
| 顺从式 | §3 十八 GEN 逐项重判 |
| 对抗式 | §6 旧票/audit/升级/host/三旧叶/图与安装态/omit 视图/dry-SRI/范围字符串/不继承 PASS |

---

## 9. 执行边界（诚实）

**本轮做了：** 独立实算锁；确认 FILES 哈希后复用 V0.06 全文阅读；JSON 解析与重复键扫描；220 行跨表；根/web 镜像；Arborist loadVirtual；SRI 形态与负例；`npm ls` lock-only；`npm ci --dry-run`（含 offline）；只读 audit（含 omit=dev）；消费扫描 `verify-no-legacy-api-prototype`；`npm help package-lock-json`；007 入口配置与跟踪源检索；磁盘三叶与 vitest/mocker 版本；`/tmp` 五条负例（正本 inode/mtime/SHA 不变）；findings V3.38 七行只读定位；questions 目录无 M004。

**明确没做、因此不能声称：**

- 真实 `npm ci` / `npm install` / `npx` / `npm exec` / `npm audit fix`
- 217 个 tarball 下载与 sha512 重算
- GET GHSA 公告页
- Vite 构建、Vitest 用例、Playwright、Maven、Gradle
- `verify:predeploy` 全链
- 安装目录更新三旧叶
- fetch / commit / push / worktree / 部署
- 读本次 `M004-b5-r1-self-raw.md` 正文
- 升依赖、启长驻、连业务库、外下业务流量、删除仓内文件

**不得声称：** 全局预发布绿；全仓交付门禁绿；audit 全绿；供应链已验证；磁盘已等于锁；三旧叶已修运行；typecheck／UI／后端测试已通过；模块生命周期 PASS。

---

## 10. Findings（临时 GR）

**空集。** 不分配 GR-01 起的号。不预设 PASS 才扫到空集：本票对 3321 行、220 记录、18 GEN、七 ID 重判后，**没有**可复现的本模块当前工程缺陷或新添加项。007 公告仍红且未接公开入口，按铁律 6 保持登记、队列否。三旧叶保持「磁盘≠锁」观察，不升缺陷。

---

## 11. 最终结论

**本席 B5-r1：PASS。**

条件核对：本模块唯一文件有覆盖记录（3321/3321 与 220/220）；映射的 18 GEN 已逐项核对；三线均执行；声称有本轮命令/原文证据；无未答 questions；无未定稿缺陷；本报告第一行哈希为独立实算。不存在有条件通过。

七 ID 保持技术性／当前工程缺陷否／修复队列否。新 GR 空集。跳过的新增 B3/B4 不回写。累计 B3 仍 **1**。

**现在不得标模块 PASS。** 双席合取与 B5 后四值校验属根席收口动作，本席不写 ledger／收口。两席 B1/B2 PASS 不代替本 B5。本文件 V0.01/V0.02 旧票保留作历史，不覆盖。

不修 HTML／锁／源码／依赖。其后不进入 B3／B4／其他模块。

B5 结束。不修复。
