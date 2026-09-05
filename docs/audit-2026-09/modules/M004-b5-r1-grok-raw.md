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
