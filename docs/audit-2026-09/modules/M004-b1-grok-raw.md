CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M004 B1 Grok 独立审计报告

- 报告版本：V0.01（指定路径原先不存在，本次创建，非追加）
- 席位：Grok（审计席乙）
- 步骤：B1 修复前全量审计
- 模块：M004 `01-root-config-and-guidance-02`
- 控制：V0.17；ledger 状态 IN_PROGRESS；B3 累计 0
- 范围：仅 `package-lock.json`（`OVERSIZE-SINGLE-FILE`）
- 主工作区 HEAD（实算）：`9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b`
- 审计结论：**PASS**
- 临时 finding：无（未分配 GR-01）
- 本席未修复、未改方案、未派子代理、未读 `M004-b1-self-raw.md`、未读 Codex 分析

## 0. 技能预检（本席自核）

本席在本机技能目录核到并可按 §6 收窄口径使用：

| 技能 | 路径事实 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前读取 |
| `grilling` / `grill-me` | mattpocock `.../skills/productivity/grilling/SKILL.md` 与 `grill-me/SKILL.md` | 对抗式自问自答；**不等操作员、不派子代理**（本体这两条作废） |
| `verification-before-completion` | superpowers 同目录 | PASS 前只采用已跑命令输出 |
| `systematic-debugging` | superpowers 同目录 | 只借读错误→复现→追数据流；本模块无待修缺陷，未改代码 |
| `diagnosing-bugs` | mattpocock engineering | 用 parse / ls / dry-run / 完整性回路；无红灯缺陷 |
| `codebase-design` | mattpocock engineering | 锁文件接口=npm ci 输入；删除测试：删锁则 `npm ci` 无法复现树 |
| `tdd` / `test-driven-development` | 均存在 | B1 禁止修复，未落测试 |
| `ponytail-review` | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删项；见 §8 |
| `writing-for-agents` | mattpocock productivity | 报告结构按完成标准写 |
| `i-have-adhd` | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 输出形态：先结论、编号、无寒暄 |
| `resolving-merge-conflicts` | mattpocock engineering | **已核存在**；本模块无合并冲突，未调用本体 |
| `receiving-code-review` | superpowers | B1 不适用收修复意见 |

缺口（不冒充已调用）：

1. `batch-grill-me`：mattpocock 文档写明已并入 `grilling`，无独立技能可装。
2. `git-guardrails-claude-code`、`ubiquitous-language`：在其他宿主目录存在（如 `~/.qwen/skills/`），**本席未把其 SKILL.md 当本次强制步骤执行**。
3. 未调用 `research`、未派 background agent。外部规范用本机 `npm help package-lock-json` 与 registry 实测。

明确未用（§6 划掉）：`dispatching-parallel-agents`、`subagent-driven-development`、`using-git-worktrees`、`ponytail-audit`、`ponytail-debt`。

## 1. 输入 manifest（只读）

| 输入 | 用途 | 实算 |
|---|---|---|
| `docs/audit-2026-09/plan/criteria-lock.md` | 独立 SHA256；M004 映射 18GEN | 本席 `sha256sum ... \| awk '{print $1}'` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| `docs/audit-2026-09/plan/criteria-lock.sha256` | 旁路对照（非本席填写） | 同值 |
| `docs/audit-2026-09/ledger.md` 顶部 | 对照基准，不写入 | `CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`；`LOCK-AMEND-COUNT: 0`；M004 `IN_PROGRESS` B3=0 |
| `docs/audit-2026-09/plan/modules.md` M004 | 文件清单与 OVERSIZE 标记 | 1 文件 `package-lock.json`；计划有效行 3320 |
| `docs/audit-2026-09/plan/runbook.md` | OVERSIZE 须全文件覆盖表；配置用原生解析+无副作用入口 | 已按此取证 |
| `docs/audit-2026-09/plan/scope.txt` | 纳入核对 | 第 2816 行 `package-lock.json` |
| `docs/audit-2026-09/plan/module-union.txt` | 重叠核对 | `package-lock.json` 出现 1 次 |
| `package-lock.json` | **本模块唯一审计对象** | 见 §2 |
| `package.json`（根，M005 文件，只作锁对照） | 直系声明 | name/version/workspaces/devDependencies/engines 与锁根记录一致 |
| `apps/web/package.json`（工作区清单，只作锁对照） | 直系声明 | 与 `packages["apps/web"]` 一致 |
| `.github/workflows/predeploy-gates.yml:22`、`ops/ci/predeploy-gates.yml:20` | 锁的使用入口 | `npm ci --ignore-scripts` |
| `tools/verify-no-legacy-api-prototype.mjs:65-73` | 现有只读门禁读锁 | 禁止 Fastify/Prisma/SQLite/`apps/api` 标记 |
| 本机 `npm help package-lock-json` | 外部规范原文（类型 3） | lockfileVersion 3；link 记录无其它字段；integrity 为 SRI |
| 未读 | `M004-b1-self-raw.md`、本次 Codex 分析、其他席 raw | 遵守互盲 |

冻结映射（`criteria-lock.md:6384`）：

`M004 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]`

无 HTML 派生 ID。不得因三份 HTML 未点名 npm 包而删除或提问。

## 2. 目标文件实算

命令与输出：

```
git -C /home/james/complex-management rev-parse HEAD
9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b

git ls-files -z -- package-lock.json | xargs -0 cat | wc -l
3320

sha256sum package-lock.json
0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d  package-lock.json

stat: size=114212 bytes; LF 结尾；无 BOM；无 CRLF
```

与 `modules.md` 计划 3320 行一致。`OVERSIZE-SINGLE-FILE` 合法（单文件 >3000 行不可再切）。

环境（记录，不是锁内容）：`node v24.20.0`；宿主 `npm 11.19.0`；根 `package.json` `packageManager` 为 `npm@10.9.8`（该字段在 M005 的 package.json，不在本锁内）。官方 `npm help`：lockfileVersion 3 为 npm v9+ 使用、对 npm v7 向后兼容。本锁 `lockfileVersion` 为 3。

禁止项遵守：未跑 `npm install` / `npm ci`（非 dry-run）/ `npm audit fix`；未改数据库或部署；锁文件 mtime 在只读命令前后均为 `2026-06-23 09:13:35`，SHA 未变。

## 3. 结构段覆盖（全文，非抽样）

覆盖方法（并用，缺一不可）：

1. 全文 `JSON.parse` 成功（逐字节）。
2. `packages` 220 个键全部列入下表，每键给出起始行；键扫描无重复。
3. 依赖边 438 条全部可解析到锁内记录，缺失边 0。
4. 人工按结构段阅读：顶层、根工作区、`apps/web`、嵌套 workspace 包、link、平台 optional、fsevents install script、playwright/vite/antd/vitest、文件闭合。

结构段：

| 段 | 行 | 内容 | 覆盖状态 |
|---|---|---|---|
| S0 顶层 | 1–5 | `name` `version` `lockfileVersion` `requires` | 已覆盖 |
| S1 `packages[""]` | 7–20 | 根工作区、devDependencies、engines | 已覆盖 |
| S2 `packages["apps/web"]` | 21–43 | 工作区依赖与开发依赖声明 | 已覆盖 |
| S3 工作区嵌套 | 44–76 | `@rolldown/pluginutils` `@vitejs/plugin-react` | 已覆盖 |
| S4 `packages` 其余 | 77–3318 | 217 个 registry 包 + 1 个 workspace link | 已覆盖（下表 220 键） |
| S5 闭合 | 3319–3320 | `packages` 与根对象结束 | 已覆盖 |

顶层键仅 `name, version, lockfileVersion, requires, packages`。无遗留 `dependencies` 树（lockfileVersion 3 官方以 `packages` 为权威）。无 `npm-shrinkwrap.json`。

### 3.1 packages 全键覆盖表（220/220）

| # | 行 | packages 键 | version | resolved 主机 | integrity | optional | dev | peer | hasInstallScript | os | cpu |
|---:|---:|---|---|---|---|---|---|---|---|---|---|
| 1 | 7 | `""` | 0.1.0 |  |  | false | false | false | false |  |  |
| 2 | 21 | `apps/web` | 0.1.0 |  |  | false | false | false | false |  |  |
| 3 | 44 | `apps/web/node_modules/@rolldown/pluginutils` | 1.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 4 | 51 | `apps/web/node_modules/@vitejs/plugin-react` | 6.0.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 5 | 77 | `node_modules/@adobe/css-tools` | 4.5.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 6 | 84 | `node_modules/@ant-design/colors` | 7.2.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 7 | 93 | `node_modules/@ant-design/cssinjs` | 1.24.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 8 | 112 | `node_modules/@ant-design/cssinjs-utils` | 1.1.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 9 | 127 | `node_modules/@ant-design/fast-color` | 2.0.6 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 10 | 139 | `node_modules/@ant-design/icons` | 5.6.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 11 | 159 | `node_modules/@ant-design/icons-svg` | 4.4.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 12 | 165 | `node_modules/@ant-design/react-slick` | 1.1.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 13 | 181 | `node_modules/@asamuzakjp/css-color` | 5.1.11 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 14 | 198 | `node_modules/@asamuzakjp/dom-selector` | 7.1.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 15 | 215 | `node_modules/@asamuzakjp/generational-cache` | 1.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 16 | 225 | `node_modules/@asamuzakjp/nwsapi` | 2.3.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 17 | 232 | `node_modules/@babel/code-frame` | 7.29.7 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 18 | 248 | `node_modules/@babel/helper-validator-identifier` | 7.29.7 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 19 | 259 | `node_modules/@babel/runtime` | 7.29.7 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 20 | 268 | `node_modules/@bramus/specificity` | 2.4.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 21 | 281 | `node_modules/@csstools/color-helpers` | 6.0.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 22 | 301 | `node_modules/@csstools/css-calc` | 3.2.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 23 | 325 | `node_modules/@csstools/css-color-parser` | 4.1.7 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 24 | 353 | `node_modules/@csstools/css-parser-algorithms` | 4.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 25 | 376 | `node_modules/@csstools/css-syntax-patches-for-csstree` | 1.1.5 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 26 | 401 | `node_modules/@csstools/css-tokenizer` | 4.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 27 | 421 | `node_modules/@emnapi/core` | 1.10.0 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 28 | 433 | `node_modules/@emnapi/runtime` | 1.10.0 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 29 | 444 | `node_modules/@emnapi/wasi-threads` | 1.2.1 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 30 | 455 | `node_modules/@emotion/hash` | 0.8.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 31 | 461 | `node_modules/@emotion/unitless` | 0.7.5 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 32 | 467 | `node_modules/@exodus/bytes` | 1.15.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 33 | 485 | `node_modules/@jridgewell/sourcemap-codec` | 1.5.5 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 34 | 492 | `node_modules/@napi-rs/wasm-runtime` | 1.1.5 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 35 | 511 | `node_modules/@oxc-project/types` | 0.133.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 36 | 521 | `node_modules/@playwright/test` | 1.61.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 37 | 537 | `node_modules/@rc-component/async-validator` | 5.1.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 38 | 549 | `node_modules/@rc-component/color-picker` | 2.0.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 39 | 565 | `node_modules/@rc-component/context` | 1.4.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 40 | 579 | `node_modules/@rc-component/mini-decimal` | 1.1.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 41 | 591 | `node_modules/@rc-component/mutate-observer` | 1.1.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 42 | 609 | `node_modules/@rc-component/portal` | 1.1.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 43 | 627 | `node_modules/@rc-component/qrcode` | 1.1.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 44 | 643 | `node_modules/@rc-component/tour` | 1.15.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 45 | 663 | `node_modules/@rc-component/trigger` | 2.3.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 46 | 684 | `node_modules/@rolldown/binding-android-arm64` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | android | arm64 |
| 47 | 701 | `node_modules/@rolldown/binding-darwin-arm64` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | darwin | arm64 |
| 48 | 718 | `node_modules/@rolldown/binding-darwin-x64` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | darwin | x64 |
| 49 | 735 | `node_modules/@rolldown/binding-freebsd-x64` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | freebsd | x64 |
| 50 | 752 | `node_modules/@rolldown/binding-linux-arm-gnueabihf` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm |
| 51 | 769 | `node_modules/@rolldown/binding-linux-arm64-gnu` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm64 |
| 52 | 786 | `node_modules/@rolldown/binding-linux-arm64-musl` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm64 |
| 53 | 803 | `node_modules/@rolldown/binding-linux-ppc64-gnu` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | ppc64 |
| 54 | 820 | `node_modules/@rolldown/binding-linux-s390x-gnu` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | s390x |
| 55 | 837 | `node_modules/@rolldown/binding-linux-x64-gnu` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | x64 |
| 56 | 854 | `node_modules/@rolldown/binding-linux-x64-musl` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | linux | x64 |
| 57 | 871 | `node_modules/@rolldown/binding-openharmony-arm64` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | openharmony | arm64 |
| 58 | 888 | `node_modules/@rolldown/binding-wasm32-wasi` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false |  | wasm32 |
| 59 | 907 | `node_modules/@rolldown/binding-win32-arm64-msvc` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | win32 | arm64 |
| 60 | 924 | `node_modules/@rolldown/binding-win32-x64-msvc` | 1.0.3 | registry.npmjs.org | sha512 | true | true | false | false | win32 | x64 |
| 61 | 941 | `node_modules/@standard-schema/spec` | 1.1.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 62 | 948 | `node_modules/@testing-library/dom` | 10.4.1 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 63 | 969 | `node_modules/@testing-library/jest-dom` | 6.9.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 64 | 989 | `node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api` | 0.6.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 65 | 996 | `node_modules/@testing-library/react` | 16.3.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 66 | 1024 | `node_modules/@testing-library/user-event` | 14.6.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 67 | 1038 | `node_modules/@tybys/wasm-util` | 0.10.2 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 68 | 1049 | `node_modules/@types/aria-query` | 5.0.4 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 69 | 1057 | `node_modules/@types/chai` | 5.2.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 70 | 1068 | `node_modules/@types/deep-eql` | 4.0.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 71 | 1075 | `node_modules/@types/estree` | 1.0.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 72 | 1082 | `node_modules/@types/react` | 19.2.16 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 73 | 1092 | `node_modules/@types/react-dom` | 19.2.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 74 | 1102 | `node_modules/@vitest/expect` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 75 | 1120 | `node_modules/@vitest/mocker` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 76 | 1147 | `node_modules/@vitest/pretty-format` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 77 | 1160 | `node_modules/@vitest/runner` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 78 | 1174 | `node_modules/@vitest/snapshot` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 79 | 1190 | `node_modules/@vitest/spy` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 80 | 1200 | `node_modules/@vitest/utils` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 81 | 1215 | `node_modules/ansi-regex` | 5.0.1 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 82 | 1226 | `node_modules/ansi-styles` | 5.2.0 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 83 | 1240 | `node_modules/antd` | 5.29.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 84 | 1305 | `node_modules/apps-web` |  | apps/web |  | false | false | false | false |  |  |
| 85 | 1309 | `node_modules/aria-query` | 5.3.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 86 | 1319 | `node_modules/assertion-error` | 2.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 87 | 1329 | `node_modules/bidi-js` | 1.0.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 88 | 1339 | `node_modules/chai` | 6.2.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 89 | 1349 | `node_modules/classnames` | 2.5.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 90 | 1355 | `node_modules/compute-scroll-into-view` | 3.1.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 91 | 1361 | `node_modules/convert-source-map` | 2.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 92 | 1368 | `node_modules/copy-to-clipboard` | 3.3.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 93 | 1377 | `node_modules/css-tree` | 3.2.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 94 | 1391 | `node_modules/css.escape` | 1.5.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 95 | 1398 | `node_modules/csstype` | 3.2.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 96 | 1404 | `node_modules/data-urls` | 7.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 97 | 1418 | `node_modules/dayjs` | 1.11.21 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 98 | 1424 | `node_modules/decimal.js` | 10.6.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 99 | 1431 | `node_modules/dequal` | 2.0.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 100 | 1441 | `node_modules/detect-libc` | 2.1.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 101 | 1451 | `node_modules/dom-accessibility-api` | 0.5.16 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 102 | 1459 | `node_modules/entities` | 8.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 103 | 1472 | `node_modules/es-module-lexer` | 2.1.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 104 | 1479 | `node_modules/estree-walker` | 3.0.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 105 | 1489 | `node_modules/expect-type` | 1.3.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 106 | 1499 | `node_modules/fdir` | 6.5.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 107 | 1517 | `node_modules/fsevents` | 2.3.3 | registry.npmjs.org | sha512 | true | true | false | true | darwin |  |
| 108 | 1532 | `node_modules/html-encoding-sniffer` | 6.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 109 | 1545 | `node_modules/indent-string` | 4.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 110 | 1555 | `node_modules/is-potential-custom-element-name` | 1.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 111 | 1562 | `node_modules/js-tokens` | 4.0.0 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 112 | 1570 | `node_modules/jsdom` | 29.1.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 113 | 1611 | `node_modules/json2mq` | 0.2.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 114 | 1620 | `node_modules/lightningcss` | 1.32.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 115 | 1650 | `node_modules/lightningcss-android-arm64` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | android | arm64 |
| 116 | 1671 | `node_modules/lightningcss-darwin-arm64` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | darwin | arm64 |
| 117 | 1692 | `node_modules/lightningcss-darwin-x64` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | darwin | x64 |
| 118 | 1713 | `node_modules/lightningcss-freebsd-x64` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | freebsd | x64 |
| 119 | 1734 | `node_modules/lightningcss-linux-arm-gnueabihf` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm |
| 120 | 1755 | `node_modules/lightningcss-linux-arm64-gnu` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm64 |
| 121 | 1776 | `node_modules/lightningcss-linux-arm64-musl` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | linux | arm64 |
| 122 | 1797 | `node_modules/lightningcss-linux-x64-gnu` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | linux | x64 |
| 123 | 1818 | `node_modules/lightningcss-linux-x64-musl` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | linux | x64 |
| 124 | 1839 | `node_modules/lightningcss-win32-arm64-msvc` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | win32 | arm64 |
| 125 | 1860 | `node_modules/lightningcss-win32-x64-msvc` | 1.32.0 | registry.npmjs.org | sha512 | true | true | false | false | win32 | x64 |
| 126 | 1881 | `node_modules/lru-cache` | 11.5.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 127 | 1891 | `node_modules/lz-string` | 1.5.0 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 128 | 1902 | `node_modules/magic-string` | 0.30.21 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 129 | 1912 | `node_modules/mdn-data` | 2.27.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 130 | 1919 | `node_modules/min-indent` | 1.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 131 | 1929 | `node_modules/nanoid` | 3.3.12 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 132 | 1948 | `node_modules/obug` | 2.1.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 133 | 1962 | `node_modules/parse5` | 8.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 134 | 1975 | `node_modules/pathe` | 2.0.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 135 | 1982 | `node_modules/picocolors` | 1.1.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 136 | 1989 | `node_modules/picomatch` | 4.0.4 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 137 | 2002 | `node_modules/playwright` | 1.61.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 138 | 2021 | `node_modules/playwright-core` | 1.61.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 139 | 2034 | `node_modules/playwright/node_modules/fsevents` | 2.3.2 | registry.npmjs.org | sha512 | true | true | false | true | darwin |  |
| 140 | 2049 | `node_modules/postcss` | 8.5.15 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 141 | 2078 | `node_modules/pretty-format` | 27.5.1 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 142 | 2094 | `node_modules/pretty-format/node_modules/react-is` | 17.0.2 | registry.npmjs.org | sha512 | false | true | true | false |  |  |
| 143 | 2102 | `node_modules/punycode` | 2.3.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 144 | 2112 | `node_modules/qrcode.react` | 4.2.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 145 | 2121 | `node_modules/rc-cascader` | 3.34.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 146 | 2138 | `node_modules/rc-checkbox` | 3.5.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 147 | 2153 | `node_modules/rc-collapse` | 3.9.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 148 | 2169 | `node_modules/rc-dialog` | 9.6.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 149 | 2186 | `node_modules/rc-drawer` | 7.3.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 150 | 2203 | `node_modules/rc-dropdown` | 4.2.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 151 | 2219 | `node_modules/rc-field-form` | 2.7.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 152 | 2237 | `node_modules/rc-image` | 7.12.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 153 | 2255 | `node_modules/rc-input` | 1.8.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 154 | 2270 | `node_modules/rc-input-number` | 9.5.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 155 | 2287 | `node_modules/rc-mentions` | 2.20.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 156 | 2306 | `node_modules/rc-menu` | 9.16.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 157 | 2324 | `node_modules/rc-motion` | 2.9.5 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 158 | 2339 | `node_modules/rc-notification` | 5.6.4 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 159 | 2358 | `node_modules/rc-overflow` | 1.5.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 160 | 2374 | `node_modules/rc-pagination` | 5.1.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 161 | 2389 | `node_modules/rc-picker` | 4.11.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 162 | 2428 | `node_modules/rc-progress` | 4.0.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 163 | 2443 | `node_modules/rc-rate` | 2.13.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 164 | 2461 | `node_modules/rc-resize-observer` | 1.4.3 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 165 | 2477 | `node_modules/rc-segmented` | 2.7.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 166 | 2493 | `node_modules/rc-select` | 14.16.8 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 167 | 2515 | `node_modules/rc-slider` | 11.1.9 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 168 | 2533 | `node_modules/rc-steps` | 6.0.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 169 | 2551 | `node_modules/rc-switch` | 4.1.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 170 | 2566 | `node_modules/rc-table` | 7.54.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 171 | 2587 | `node_modules/rc-tabs` | 15.7.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 172 | 2609 | `node_modules/rc-textarea` | 1.10.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 173 | 2626 | `node_modules/rc-tooltip` | 6.4.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 174 | 2642 | `node_modules/rc-tree` | 5.13.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 175 | 2662 | `node_modules/rc-tree-select` | 5.27.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 176 | 2679 | `node_modules/rc-upload` | 4.11.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 177 | 2694 | `node_modules/rc-util` | 5.44.4 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 178 | 2708 | `node_modules/rc-virtual-list` | 3.19.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 179 | 2727 | `node_modules/react` | 19.2.7 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 180 | 2736 | `node_modules/react-dom` | 19.2.7 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 181 | 2748 | `node_modules/react-is` | 18.3.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 182 | 2754 | `node_modules/redent` | 3.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 183 | 2768 | `node_modules/require-from-string` | 2.0.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 184 | 2778 | `node_modules/resize-observer-polyfill` | 1.5.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 185 | 2784 | `node_modules/rolldown` | 1.0.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 186 | 2818 | `node_modules/rolldown/node_modules/@rolldown/pluginutils` | 1.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 187 | 2825 | `node_modules/saxes` | 6.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 188 | 2838 | `node_modules/scheduler` | 0.27.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 189 | 2844 | `node_modules/scroll-into-view-if-needed` | 3.1.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 190 | 2853 | `node_modules/siginfo` | 2.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 191 | 2860 | `node_modules/source-map-js` | 1.2.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 192 | 2870 | `node_modules/stackback` | 0.0.2 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 193 | 2877 | `node_modules/std-env` | 4.1.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 194 | 2884 | `node_modules/string-convert` | 0.2.1 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 195 | 2890 | `node_modules/strip-indent` | 3.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 196 | 2903 | `node_modules/stylis` | 4.4.0 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 197 | 2909 | `node_modules/symbol-tree` | 3.2.4 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 198 | 2916 | `node_modules/throttle-debounce` | 5.0.2 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 199 | 2925 | `node_modules/tinybench` | 2.9.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 200 | 2932 | `node_modules/tinyexec` | 1.2.4 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 201 | 2942 | `node_modules/tinyglobby` | 0.2.17 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 202 | 2959 | `node_modules/tinyrainbow` | 3.1.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 203 | 2969 | `node_modules/tldts` | 7.4.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 204 | 2982 | `node_modules/tldts-core` | 7.4.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 205 | 2989 | `node_modules/toggle-selection` | 1.0.6 | registry.npmjs.org | sha512 | false | false | false | false |  |  |
| 206 | 2995 | `node_modules/tough-cookie` | 6.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 207 | 3008 | `node_modules/tr46` | 6.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 208 | 3021 | `node_modules/tslib` | 2.8.1 | registry.npmjs.org | sha512 | true | true | false | false |  |  |
| 209 | 3029 | `node_modules/typescript` | 5.9.3 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 210 | 3043 | `node_modules/undici` | 7.28.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 211 | 3053 | `node_modules/vite` | 8.0.16 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 212 | 3131 | `node_modules/vitest` | 4.1.9 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 213 | 3221 | `node_modules/w3c-xmlserializer` | 5.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 214 | 3234 | `node_modules/webidl-conversions` | 8.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 215 | 3244 | `node_modules/whatwg-mimetype` | 5.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 216 | 3254 | `node_modules/whatwg-url` | 16.0.1 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 217 | 3269 | `node_modules/why-is-node-running` | 2.3.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 218 | 3286 | `node_modules/xml-name-validator` | 5.0.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 219 | 3296 | `node_modules/xmlchars` | 2.2.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |
| 220 | 3303 | `node_modules/yaml` | 2.9.0 | registry.npmjs.org | sha512 | false | true | false | false |  |  |

表计数 220，与 `Object.keys(packages).length` 相等；无键缺行号。

## 4. 锁的使用入口（只读，不开新模块）

1. CI：`.github/workflows/predeploy-gates.yml` 与 `ops/ci/predeploy-gates.yml` 在 `setup-node` `node-version: "22"` 之后执行 `npm ci --ignore-scripts`。`npm ci` 的契约是：有锁、与 `package.json` 一致、按锁安装。`--ignore-scripts` 跳过生命周期脚本。本模块只确认锁能作为该入口的输入，不把 workflow 当 M004 文件审。
2. 根 `package.json` `packageManager: npm@10.9.8`、`engines.node: ">=22"`、`workspaces: ["apps/web"]`。锁根记录复制了 name/version/workspaces/devDependencies/engines。
3. `tools/verify-no-legacy-api-prototype.mjs` 读锁文本，禁止 `/apps\/api/i` `/legacy-apps-api/i` `/@fastify\//i` `/\bfastify\b/i` `/@prisma\//i` `/\bprisma\b/i` `/\bsqlite\b/i`。
4. README 写 `npm install`（其他模块）。此处只记：锁也会被 `npm install` 读取；`npm install` 相对 `npm ci` 可改锁。本席未跑 `npm install`。

## 5. 可复现命令与真实输出（类型 4；规范类型 3）

### 5.1 JSON 与图

- `JSON.parse`：OK。
- 顶层：`name=entertainment-complex-management` `version=0.1.0` `lockfileVersion=3` `requires=true`。
- `packages` 220；registry `https://registry.npmjs.org` 217；integrity 算法 sha512 217；缺失 resolved（registry 包）0；缺失 integrity 的 `node_modules/` 键仅 `node_modules/apps-web`（`link: true`，官方规定 link 记录不带其它字段）。
- 无 `http:` / `git+` / `file:` tarball；无 deprecated；无 bundled；packages 键重复 0。
- 根/工作区与两个 `package.json` 的 dependencies/devDependencies/workspaces/name/version/engines **零 diff**。
- 声明范围 semver.satisfies 全部 SAT（含 `antd@^5.22.5` → `5.29.3`、`react@^19.0.0` → `19.2.7`、`typescript@^5.7.2` → `5.9.3`）。
- 依赖边 438，缺失 0。lightningcss 11 个 optional、rolldown 15 个 optional 均有对应 packages 记录。
- 多版本（npm 嵌套，非冲突）：`dom-accessibility-api` 0.5.16/0.6.3；`fsevents` 2.3.2/2.3.3；`react-is` 17.0.2/18.3.1。

### 5.2 `npm ls --package-lock-only`

```
npm ls --package-lock-only --depth=0
entertainment-complex-management@0.1.0
├── @playwright/test@1.61.0
├─┬ apps-web@0.1.0 -> ./apps/web
│ ├── …（与 apps/web/package.json 声明集合同名同钉死版本）
└── yaml@2.9.0
EXIT=0

npm ls --package-lock-only --all
EXIT=0
```

stderr 仅 `NO_COLOR`/`FORCE_COLOR` 警告。`UNMET` 命中全部为 `UNMET OPTIONAL DEPENDENCY`（vitest/vite/jsdom/plugin-react 的 optional peer，如 `happy-dom`、`esbuild`、`canvas`）。无 invalid / extraneous / 非 optional UNMET。

### 5.3 `npm ci --dry-run --ignore-scripts`

`EXIT=0`。输出以 `add …` / `remove …` 描述相对**当前本机 `node_modules`** 的差异（`added 188 packages, and removed 186 packages`）。`node_modules/` 按范围排除，该 diff **不是锁自洽失败**。锁文件 SHA 与 mtime 未变。  
**未执行真实 `npm ci`，不得把安装/测试写成 PASS。** dry-run 只证明 npm 11 能解析此 lockfileVersion 3 且不因 package.json 漂移拒绝。

### 5.4 现有门禁

```
node tools/verify-no-legacy-api-prototype.mjs
NO_LEGACY_API_PROTOTYPE_PASS
EXIT=0
```

锁内无 Fastify/Prisma/SQLite/`apps/api` 标记。

### 5.5 完整性 vs 官方 registry（类型 3+4）

网络可用：`https://registry.npmjs.org/yaml/-/yaml-2.9.0.tgz` HTTP 200。

对锁内全部 217 个带 `resolved`+`integrity` 的 https tarball：`fetch` 后算 sha512-base64，与 SRI 比较。

```
JOBS 217
DONE ok 217 fail 0
EXIT=0
```

未猜测 CVE；未跑 `npm audit`。

### 5.6 官方 lock 语义（`npm help package-lock-json`）

- lockfileVersion 3：npm v9+；有 `packages` 时 v7 忽略遗留 `dependencies`。
- `link: true`：只给 link 目标，其它字段不出现 → `node_modules/apps-web` 无 integrity 符合规范，不是缺陷。
- `hasInstallScript`：表示存在 preinstall/install/postinstall。
- `optional`+`os`/`cpu`：平台可选依赖。
- integrity：sha512 或 sha1 的 SRI。本锁全 sha512。

## 6. 四分类（冲突 → 细化 → 技术性 → 添加）

整文件是 npm 锁，不包含票种、订单状态、角色、价格、审批等业务名词。

- **冲突**：无。三份 HTML 未规定 npm 依赖树；锁也未改写 HTML 口径。
- **细化**：不能把「前端要能构建」直接锚到某一 `td.k`/`h2` 作为本文件上位需求。锁不实现业务规则。
- **技术性**：整文件。无业务发明：只冻结构建/测试工具与 UI 库版本、完整性与平台可选包，使 `npm ci` 可复现。不因 HTML 未点名 `antd`/`vite`/`playwright` 而删除或提问。
- **添加**：无。不把依赖名当成业务发明。

## 7. 技术性逐项登记

每项：无业务发明理由 + 源码行 + 证据类型。工程缺陷栏：本次均否。

| ID | 内容 | 行 | 无业务发明理由 | 证据类型 | 缺陷 |
|---|---|---|---|---|---|
| T01 | 顶层 name/version/lockfileVersion/requires | 1–5 | 锁格式元数据，对应官方 File Format | 3+4：`npm help`；JSON.parse | 否 |
| T02 | `packages[""]` 根工作区、yaml/@playwright/test、engines `node>=22`、workspaces `apps/web` | 7–20 | 复制根 package.json 的构建声明 | 4：与 package.json 零 diff | 否 |
| T03 | `packages["apps/web"]` 运行时与开发依赖范围 | 21–43 | 复制工作区 package.json | 4：零 diff | 否 |
| T04 | workspace link `node_modules/apps-web` | 1305–1308 | 官方 link 记录；目标 `apps/web` | 3+4 | 否 |
| T05 | 声明依赖钉死：`@playwright/test@1.61.0` L521；`yaml@2.9.0` L3303；`@ant-design/icons@5.6.1` L139；`antd@5.29.3` L1240；`qrcode.react@4.2.0` L2112；`react@19.2.7` L2727；`react-dom@19.2.7` L2736；`@testing-library/*`；`@types/react*`；`@vitejs/plugin-react@6.0.2` L51；`jsdom@29.1.1` L1570；`typescript@5.9.3` L3029；`vite@8.0.16` L3053；`vitest@4.1.9` L3131 | 见表 | 将 package.json 的 caret 范围钉成可复现版本，不是业务规则 | 4：semver SAT；npm ls EXIT 0 | 否 |
| T06 | 传递依赖（antd→rc-*、jsdom→whatwg-*、vitest→@vitest/* 等） | 覆盖表其余键 | 安装闭包，无域名词 | 4：边 438 全解析 | 否 |
| T07 | 平台 optional：rolldown 15 binding、lightningcss 11 native、fsevents darwin、wasm32-wasi | 684–940、1650–1879、1517、2034、888 | 跨 OS/CPU 复现安装；linux x64 需要 gnu/musl 记录在锁里 | 3+4：optionalDeps 清单与 packages 键一一对应 | 否 |
| T08 | 217× `resolved`+`sha512` integrity | 各 registry 包 | 信任边界：registry tarball 校验 | 3+4：217/217 与现网 tarball 一致 | 否 |
| T09 | `hasInstallScript`：`fsevents@2.3.3` L1522、`playwright/node_modules/fsevents@2.3.2` L2039 | 1517–2047 | 官方字段，标记原生模块安装脚本；os=darwin 且 optional | 3+4 | 否。CI 使用 `--ignore-scripts`（入口在 CI 文件，非本模块缺陷） |
| T10 | 各包 `engines`（含 vite/rolldown `^20.19.0 \|\| >=22.12.0`，vitest `^20 \|\| ^22 \|\| >=24`） | 多处，根 L17–19 | 从上游 package.json 拷入，非本仓库业务约束 | 3 | 否。仓库未设 `.npmrc`；`npm config get engine-strict` = false；本席节点 24.20.0 dry-run 0。CI `node-version: "22"` 属其他文件 |
| T11 | peer / peerDependenciesMeta（含未安装的 optional peer） | vitest L3168–3218 等 | 记录可选 peer，不强制闭包 | 4：UNMET 均为 OPTIONAL | 否 |
| T12 | 嵌套副本 pluginutils / fsevents / react-is / dom-accessibility-api | L44 与 L2818；L1517 与 L2034 等 | npm 嵌套树的正常形态，版本不同或路径不同 | 4 | 否。ponytail：删嵌套键会让 npm ci 重写锁 |

## 8. 三线

### 8.1 第一性原理

1. **为何必须存在？删了 HTML 哪条做不到？**  
   三份 HTML 没有「必须提交 package-lock.json」原文。删锁不会让某一 `td.k` 立刻失败，但 `npm ci`（CI 实入口）按官方说明需要锁来得到同一棵树。答不上 HTML 锚点 **≠ 该删**。分类=技术性（GEN-017）。证据：类型 3 `npm help`「intended to be committed」；类型 4 CI 脚本 `npm ci --ignore-scripts`。

2. **声称行为是否执行过？**  
   锁声称：描述可复现依赖树、带完整性。已验证：JSON 合法、与两个 package.json 声明一致、`npm ls --package-lock-only --all` 0、`npm ci --dry-run --ignore-scripts` 0、217 integrity 对现网 tarball 全匹配。  
   **未**跑写入式 `npm ci`，**未**跑前端单测/e2e，不得声称「安装后应用行为 PASS」。

3. **哪些前提未验证就当真？**  
   - 本机 `node_modules` 与锁可能漂移（dry-run 的 add/remove）；`node_modules` 不在范围，不以它为锁对错依据。  
   - 宿主 npm 11.19.0 ≠ package.json 声明的 npm@10.9.8；lockfileVersion 3 官方兼容，但未用 corepack 拉起 10.9.8 再 dry-run。  
   - `engine-strict` 为 false，engines 字段不会让 npm 失败。  
   - 未做漏洞数据库比对。

### 8.2 顺从式（假定锁意图正确）

按 18GEN 核对输入/输出/错误/权限/持久化/测试/文档。本文件无运行时权限模型、无 DB、无 UI、无模块内测试。顺从结论：它作为 npm v9+ lockfileVersion 3 的 `packages` 图，与清单一致，失败会在 `npm ci` 显式退出（dry-run 未触发失败）。legacy 门禁 PASS。

### 8.3 对抗式（grilling 结构，自答）

| # | 反例 | 结果 |
|---|---|---|
| Q1 | JSON 重复键覆盖攻击 | packages 220 唯一，DUPS 0 |
| Q2 | `http://` 或带 token 的 resolved | 无命中 |
| Q3 | registry 包缺 integrity | 仅 link 缺，符合规范 |
| Q4 | integrity 与现网 tarball 不符 | 217 全匹配 |
| Q5 | 与 package.json 漂移导致 npm ci 拒绝 | 声明零 diff；dry-run 0 |
| Q6 | 依赖边指向不存在的包 | 缺失边 0 |
| Q7 | 残留 Fastify/Prisma/apps/api | 门禁 PASS |
| Q8 | shrinkwrap 抢优先级 | 文件不存在 |
| Q9 | 必选 peer 未锁 | UNMET 全 optional；vitest 的 vite peer `optional: false` 且 vite 已锁 8.0.16 |
| Q10 | 当前 linux x64 缺 native optional | `lightningcss-linux-x64-gnu/musl`、`@rolldown/binding-linux-x64-gnu/musl` 均在 |
| Q11 | install 脚本在 CI 静默执行 | 锁标明 hasInstallScript；CI `--ignore-scripts`；darwin-only fsevents |
| Q12 | 业务发明藏在包名 | 包名为构建/UI/测试库，无票务枚举 |
| Q13 | 只因 HTML 未写 antd 就该删 | 禁止；技术性保留 |

ponytail-review：`Lean already. Ship.` 平台 optional 与嵌套副本不能从锁里手删，否则下一轮 npm 会写回。`net: -0`。

## 9. 18GEN 逐条

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | PASS | 锁是 `npm ci` 的最小输入。无死键、无 packages/\* 或 apps/api 残留。嵌套双份是 npm 树，不是可删死代码 |
| GEN-002 可执行正确性 | 适用（入口=npm 读锁） | PASS（入口解析） | JSON.parse、npm ls --package-lock-only 0、npm ci --dry-run 0。**未**声称真实安装 PASS |
| GEN-003 测试有效性 | **不适用** | N/A | 本文件不是测试，不含断言。外部 `verify-no-legacy-api-prototype` 已跑 PASS，属工具模块，不当作本文件测试套件 |
| GEN-004 信任边界校验 | 适用 | PASS | registry 包均有 sha512；217/217 对现网 tarball 校验。link 无 integrity 为规范。无环境变量/用户输入 |
| GEN-005 授权与最小权限 | **不适用** | N/A | 无主体-对象-动作；不授予业务权限。CI 身份不在本文件 |
| GEN-006 数据完整性 | 适用（锁作为安装输入的完整性） | PASS | SRI 全匹配；无部分写入语义。真实 `npm ci` 事务性属 npm，未跑写入 |
| GEN-007 并发与幂等 | **不适用** | N/A | 锁是不可变输入，不含可重试写路径。安装并发是 npm 运行时 |
| GEN-008 接口与模式一致性 | 适用 | PASS | 与根/工作区 package.json 的 name/version/workspaces/依赖范围一致。无 OpenAPI/DTO/枚举 |
| GEN-009 错误处理与恢复 | 适用（损坏锁应失败） | PASS | 合法 JSON；npm 解析未吞错。无伪成功字段 |
| GEN-010 安全与敏感信息 | 适用 | PASS（就已验证事实） | 无明文秘密、无 http tarball、无 git 依赖。fsevents install script 已标记且 optional darwin。完整性 217 匹配。未跑漏洞库；不猜测 CVE |
| GEN-011 可观测与审计证据 | **不适用**（作为运行时日志） | N/A | 无 log/metric。文件本身是依赖证据；本报告命令输出可复核 |
| GEN-012 资源与性能边界 | 适用（作为有界数据集） | PASS | 220 包、114212 字节、图有限。无循环放大。平台 optional 为有限闭集 |
| GEN-013 构建配置与依赖 | **主判据** | PASS | 可复现钉死版本；与清单相容；CI 入口 `npm ci --ignore-scripts`；失败会显式非 0（本次 dry-run 为 0）。不依赖未声明本机状态来解释锁内容。未把陈旧 node_modules 当锁的一部分 |
| GEN-014 数据库迁移 | **不适用** | N/A | 无 SQL/迁移 |
| GEN-015 界面可用性 | **不适用** | N/A | 无 UI。包名属于系统内部标识符，不是对客人展示文案 |
| GEN-016 文档可验证性 | **不适用** | N/A | 本文件不是文档；不含完成声明或命令说明书 |
| GEN-017 业务来源分类 | 适用 | PASS | 已走冲突→细化→技术性→添加；整文件技术性；无添加项 |
| GEN-018 第一性原理证据 | 适用 | PASS | 结论落到 HTML 无相关条目（类型 1 对照为「无」）、官方 npm 手册（类型 3）、可复现命令（类型 4）。未用「锁是自动生成的所以免审」 |

## 10. Findings

无。未开 GR-01。无添加项，B1 不发问。

不把下列观察写成缺陷：

- caret 范围钉到范围内较新版本（antd 5.29.3 等）——这是锁的职责。
- optional peer UNMET。
- 本机 node_modules 与锁的 dry-run 数量差。
- 宿主 npm 11 vs packageManager 10.9.8（字段不在本文件）。
- 上游 engines 拷贝。

## 11. 结论

**PASS**

条件核对：

- 模块内唯一文件全文覆盖（3320 行 / 220 packages）
- 映射 18 个 GEN 已逐条登记
- 三线已跑
- 声称均有类型 3 或 4 证据；未验证项已标明，未当 PASS
- 无未答 questions
- 无工程缺陷 finding
- 本报告第一行独立 CRITERIA-LOCK-SHA256 为 64 位小写 hex

B1 结束。不进入 B2/B3，不修复。


# M004 / B2 整批候选复核（Grok 席乙）

版本：V0.02

席位：Grok（审计席乙）。步骤：B2 整批候选定稿审查。模块：M004 / 322，`01-root-config-and-guidance-02`。控制 V0.17。ledger 现值：IN_PROGRESS，B3=0。本席 B1 为 PASS、无 GR；本方 B1 为 FAIL、CX-01—03。本席不重开 B1、不改写原 V0.01 PASS、不修源码、不推进其他模块、不写 findings/ledger/三份 HTML/锁/计划、不派子代理、不进入 B3/B5、不新写独占 `CRITERIA-LOCK-SHA256:` 字段行。

**B2 FAIL。**

否决点唯一：候选「整批候选修复边界」把非 npm 协议字段 `documentVersion: V0.01` 写入将要改的 `package-lock.json`。六项正式 ID 的归并、四分类、T01—T12 覆盖与 001—003 的同主版钉死本身可定稿；在该字段从修复边界删除之前，本席不得给整批候选 PASS。不是模块 PASS，不是 B3 已执行，不是有条件通过。

## 1. 输入（只采信完整原始 B1；不以随后本方 B2 当 B1）

| 输入 | 派发 | 本席实算 | 采信范围 |
|---|---|---|---|
| `docs/audit-2026-09/modules/M004-b1-self-raw.md` | V0.02，64989 字节，SHA256=`cde9d4c51416b2122e90051759c05c9be3a770d126102db64c28405e10be3f32` | 本轮开始后该路径已增长（现 70453 字节）。前 64989 字节 SHA256 与派发逐字相同，后缀止于「B1结论继续FAIL……原63804字节完整保留，末尾追加不是新一轮B1。」 | **只采信这 64989 字节 V0.01+V0.02 B1。** 其后新追加按指令不读、不引用、不采信。 |
| `docs/audit-2026-09/modules/M004-b1-grok-raw.md` | 原 V0.01，49550 字节，SHA256=`f1d03742777444c383fc857265779a4bd92f52209251ba2f462fb264e3c14adb` | 追加前全文件恰 49550 字节，SHA256 与派发逐字相同 | 本文件追加前全部 V0.01 B1；本追加不是 B1，不改写原 PASS |
| `docs/audit-2026-09/findings.md` | V0.43，155585 字节，SHA256=`cdeb13e34549eecdc985f0d3644f22877ea750ad0762480cd0048f0ba343019f` | `sha256sum` 实算同一值，155585 字节 | 只审最前「M004 B1完整双席归并候选（V0.43当前，待B2双PASS）」至「以上为候选，不是双席定稿」；M001—M003 历史只保留不重审 |

本席本轮实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文 SHA256 为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与 sidecar 文本、ledger 顶部基准相同；ledger `LOCK-AMEND-COUNT: 0`。B2 按控制不新写独占锁哈希字段行。M004 映射 `GEN-001`…`GEN-018` 共 18 条，无 HTML 派生 ID。

`docs/audit-2026-09/questions/` 无 M004 文件。无添加项、无未答问题、无三份 HTML 申请。

`HEAD`=`origin/main`=`9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b`。`package-lock.json` 仍 114212 字节 / 3320 行 / SHA256=`0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d`，与两席 B1 一致。顶层键仅 `name, version, lockfileVersion, requires, packages`，无 `documentVersion`。

## 2. 本席本轮只读核验（类型 3/4；库级类型 2）

命令均在 `/home/james/complex-management`，未 `source` 环境文件，未写源、未 `npm ci`/`npm install`/`npm audit fix`。

| 命令 | 退出码 | 本席读到的结果 |
|---|---|---|
| `npm audit --package-lock-only --ignore-scripts --json` | 1 | `metadata.vulnerabilities.high=3,total=3`；键仅 `nanoid`/`postcss`/`undici`。nanoid 两条 GHSA（`<3.3.16` 与 `<3.3.18`）；postcss 两条（`<=8.5.22` moderate 与 `<=8.5.17` high）；undici 五条均 `>=7.0.0 <7.29.0`。`fixAvailable` 均为 true。 |
| `npm audit --omit=dev --package-lock-only --ignore-scripts --json` | 0 | `vulnerabilities` 空；high/total=0 |
| `npm view nanoid@3.3.18 … --json` | 0 | tarball `https://registry.npmjs.org/nanoid/-/nanoid-3.3.18.tgz`；integrity `sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w==`；engines 与锁内 3.3.12 相同 |
| `npm view postcss@8.5.23 … --json` | 0 | tarball `…/postcss-8.5.23.tgz`；integrity `sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg==`；`dependencies.nanoid`=`^3.3.16` |
| `npm view undici@7.29.0 … --json` | 0 | tarball `…/undici-7.29.0.tgz`；integrity `sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw==`；engines `>=20.18.1` 与锁内 7.28.0 相同 |
| 锁内父范围 | — | postcss `nanoid` `^3.3.12`（L2070）；vite `postcss` `^8.5.15`（L3062）；jsdom `undici` `^7.25.0`（L1592）。caret SAT：3.3.18∈`^3.3.12` 与 `^3.3.16`；8.5.23∈`^8.5.15`；7.29.0∈`^7.25.0` |
| 已安装版本（只读，不以 node_modules 为锁对错依据） | — | nanoid 3.3.12、postcss 8.5.15、undici 7.28.0 |
| `nanoid/non-secure.nanoid(-1)` 与 `customAlphabet('abc',0)()`，子进程 timeout 1000ms | 子进程 `ETIMEDOUT` | 库级无界循环成立 |
| `nanoid(6)` | 0 | `NANOID_SIX_LEN 6` |
| `/tmp` 合成 CSS 绝对 `sourceMappingURL` → `postcss.parse`（不设 `from`） | 0 | `HAS_MARKER true`；临时目录随后删除；未读仓库秘密 |
| `parseCacheControlHeader` | 0 / throw | `private=""` → `{"private":[""]}`；`private, private=hdr` → `TypeError output[key].concat is not a function`。`public, max-age=300, private` 无值形式解析为 `private:true`，与「空串数组」不是同一输入 |
| 应用源 `rg` `from/require` nanoid\|postcss\|undici（排除 node_modules / 本轮产出 / `_archive` / `target`） | 0 命中 | 无业务直接调用 |
| `node_modules/vite/dist/node/chunks/node.js` `runPostCSS` | — | L21167–21169：`from: source` 且 `to: source` |
| `apps/web/vitest.config.ts` | — | `environment: "jsdom"`，无 `resources.interceptors` |
| `node_modules/jsdom/lib/api.js` | — | 默认 `userInterceptors = []`，再追加 `createDecompressInterceptor()`，无 cache interceptor |
| `npm help package-lock-json` File Format | 0 | 顶层枚举 `name` / `version` / `lockfileVersion` / `packages` / `dependencies`。无 `documentVersion` |
| Arborist `shrinkwrap.js` | — | `swKeyOrder` L57–65 八键，无 `documentVersion`。L520–526 `...data` 会在 load 时保留未知顶层键；L1009–1011 格式 3 只剔除 `dependencies`；`json-stringify-nice` 不丢未知键。**保留 ≠ 协议字段。** |

## 3. 全差集与 T01—T12 覆盖（不漏并证据）

Grok B1 临时 finding：**无**（未开 GR-01）。技术登记 T01—T12，缺陷栏均为否。

Codex B1 临时号：CX-01、CX-02、CX-03。其余 220 记录为无缺陷技术登记。

| 原临时号 / 登记 | 候选正式 ID | 漏并？ | 改写原发现？ |
|---|---|---|---|
| CX-01；Grok 无 GR | M004-001 | 否 | 否。Grok B1 §9 GEN-010 原文仍是「未跑漏洞库；不猜测 CVE」+ PASS；候选把本项标为差集，未把 B1 改写成「Grok 当时已同意修复」 |
| CX-02；Grok 无 GR | M004-002 | 否 | 否。同上 |
| CX-03；Grok 无 GR | M004-003 | 否 | 否。undici 五条 advisory 同包同修复版本，不另拆 ID |
| T01—T04 | M004-004 | 否 | 否。只登记；缺陷否、队列否 |
| T05—T07、T09—T12 | M004-005 | 否 | 否。220 键覆盖、平台 optional、peer、嵌套、engines、hasInstallScript 均在；安全版本问题不在本条重复 |
| T08 | M004-006 | 否 | 否。217 SRI 与「SRI 匹配 ≠ 无漏洞」均保留 |

T01—T12 逐条落入：

| 本席 B1 | 候选 ID | 原缺陷栏 | 候选队列 |
|---|---|---|---|
| T01 顶层 name/version/lockfileVersion/requires | 004 | 否 | 否 |
| T02 `packages[""]` | 004 | 否 | 否 |
| T03 `packages["apps/web"]` | 004 | 否 | 否 |
| T04 workspace link `node_modules/apps-web` | 004 | 否 | 否 |
| T05 声明依赖钉死 | 005 | 否 | 否 |
| T06 传递依赖 | 005 | 否 | 否 |
| T07 平台 optional | 005 | 否 | 否 |
| T08 217× resolved+sha512 | 006 | 否 | 否 |
| T09 hasInstallScript | 005 | 否 | 否 |
| T10 engines | 005 | 否 | 否 |
| T11 peer / peerDependenciesMeta | 005 | 否 | 否 |
| T12 嵌套副本 | 005 | 否 | 否 |

无第三套临时号、无重复正式 ID、无把 T08 完整性写成「已无漏洞」。Grok 438 边与 Codex 582 边（含 peer）口径不同，候选已写明非冲突；本席不另开 ID。B1 观察（optional peer UNMET、宿主 npm 11 vs packageManager 10.9.8、dry-run add/remove、上游 engines 拷贝）仍不升级为缺陷。

## 4. 六项逐一归并（2.1 顺序：冲突 → 细化 → 技术性 → 添加）

判定：三份 HTML 无 npm 依赖树条文；锁未改写 HTML 口径 → 非冲突。不能把「前端要能构建」锚到某一 `td.k`/`h2` 当本文件上位需求 → 非细化。无票种/角色/价格/审批等业务发明 → 非添加。六项均为**技术性**。

队列口径：冲突 / 有工程缺陷 / 添加且答「删除」→ 是；只登记、以及添加答「保留」且无工程缺陷 → 否。本批无添加项。

### M004-001 nanoid 3.3.12（CX-01）

- **分类**：技术性。无业务发明：传递开发依赖的已有软件钉死，不新增接口、价格、流程或权限。
- **真实应用可达性**：`dev: true`；`--omit=dev` audit 0；应用源无 nanoid 引用；postcss 实调用 `nanoid(6)` 长度为 6。**不支持**写成生产客人可利用或线上 DoS。
- **锁携带已知缺陷**：成立。GEN-012「不制造无界放大」；本席对已锁 3.3.12 复现负 size / 零长 1 秒挂起。GEN-010 未单列无限循环，但配置把该版本钉进 `npm ci` 树，属于锁作为安装配置的工程问题，不是 CVE 猜测。audit 两条 high 的 patched 上界分别为 3.3.16 与 3.3.18；候选取同主版 3.3.18 覆盖两者，仍在父 `^3.3.12` 及修后 postcss `^3.3.16` 内。
- **工程缺陷**：是。**修复队列**：是。
- **最小处置**：只改本包 `version`/`resolved`/`integrity` 为官方 3.3.18 元数据。不升 5.x，不在业务代码加参数闸，不 `npm audit fix`。

### M004-002 postcss 8.5.15（CX-02）

- **分类**：技术性。无业务发明：构建期 CSS 工具钉死。
- **真实应用可达性**：`dev: true`；生产 audit 0。Vite `runPostCSS` 明确 `from: source`。GHSA-fxqj-rqcc-2cmp 原文条件含「`from` 未设」——**不得**把该条件写成当前 Vite 主路径已可利用。
- **锁携带已知缺陷**：成立。GEN-010 明文禁止配置引入**路径穿越**。8.5.15 ∈ `<=8.5.17`（GHSA-r28c-9q8g-f849 high）且 ∈ `<=8.5.22`（incomplete fix）。本席 `postcss.parse` 不设 `from`、绝对 `sourceMappingURL` 命中合成 `AUDIT_SYNTHETIC_MARKER`（临时目录已删）。这是库级路径读取，不是业务 CSS 上传利用证明。
- **工程缺陷**：是。**修复队列**：是。
- **最小处置**：本包 `version`/`resolved`/`integrity` → 8.5.23 官方值，并把该记录 `dependencies.nanoid` `^3.3.12`→`^3.3.16`（与官方 8.5.23 元数据一致）。不升 vite，不改业务 CSS 流程。

### M004-003 undici 7.28.0（CX-03）

- **分类**：技术性。无业务发明：jsdom 测试传递依赖钉死。
- **真实应用可达性**：`dev: true`；生产 audit 0。vitest 只用 jsdom 环境；jsdom 默认 interceptor 不含 cache；本仓库 `vitest.config.ts` 未设 `resources.interceptors`。**未证明**测试或生产启用共享 cache / retry interceptor，**不支持**写成跨用户泄漏已发生。
- **锁携带已知缺陷**：成立。同一 7.28.0 被 audit 标 5 条 via，修复上界均为 7.29.0。本席对 `parseCacheControlHeader('private, private=hdr')` 得到与候选相同的 `TypeError output[key].concat is not a function`；`private=""` 得到 `private:[""]`。GEN-010 敏感信息泄露与 GEN-009 解析期崩溃针对的是锁引入的该库版本，不是已证的 jsdom 默认路径。
- **工程缺陷**：是。**修复队列**：是。
- **最小处置**：本包 `version`/`resolved`/`integrity` → 7.29.0 官方值。不升 jsdom，不改业务缓存设计。五条 advisory 保持一条 ID。

### M004-004（T01—T04）

技术性，工程缺陷否，队列否。顶层格式、根/web 清单镜像、workspace link 只登记。不改应用 `version=0.1.0` 或清单。接受。

### M004-005（T05—T07、T09—T12 + 本方 220 记录图）

技术性，工程缺陷否，队列否。结构闭包、平台 optional、peer、嵌套、engines、install script 只登记。三个安全版本问题只归 001—003，不在本条重复待修。接受。

### M004-006（T08）

技术性，工程缺陷否，队列否。217 HTTPS+SRI 与 Codex 216 去重 registry 比对是来源完整性，**不能**当 GEN-010 已满足的无漏洞证明。接受。

队列表实数：6 行，队列是 3、否 3，与候选表一致。无添加项人闸。

## 5. `documentVersion` — 不接受（整批 FAIL 的原因）

候选原文（findings V0.43 修复边界）：

> 版本处理：操作员版本铁律要求改动文件具有自身Vx.xx版本；npm顶层version=0.1.0和lockfileVersion=3分别是应用版本与协议格式，不能改成Vx.xx。候选拟在顶层增加documentVersion: V0.01，作为本文件自身版本标记，仅一次，不另造产物或审计计数器，归本批001的同文件版本维护；未来内容更新递增该字段。

可核事实：

1. `npm help package-lock-json`「File Format」未列出 `documentVersion`。官方顶层为 name / version / lockfileVersion / packages / dependencies。
2. 当前锁顶层键恰为 `name, version, lockfileVersion, requires, packages`。`version` 是应用 0.1.0，`lockfileVersion` 是协议 3。候选「这两项不能改成 Vx.xx」本席同意。
3. Arborist `swKeyOrder` 闭集八键，无 `documentVersion`。load 的 `...data` 与格式 3 `toJSON` 剔除 `dependencies` 的行为，只能说明**未知顶层键可能被 round-trip 留下**，不能把它提升为协议字段。
4. GEN-001：不为假设场景增加抽象。第一次正常 `npm ci` 不读该键。铁律 6：要「文档版本铁律」与 npm 协议同时成立才觉得缺字段 → 不在源里发明键。
5. 既有 Vx.xx 落点是 HTML `<!-- 文件版本 -->` 与 JS `// 文件版本` 注释。JSON 锁不能写注释，正确推论是**不要用协议外字段去迁就文档铁律**，不是「必须造一个键」。
6. 把该键归 001「同文件版本维护」会让安全钉死 diff 混入非缺陷行，B4 归因被污染。

**最小处置（收窄候选，不改源、不另造产物）：**

- 从修复边界**删除** `documentVersion` 及其「未来递增」句。
- B3 若在收窄后双 PASS：只改三个已有包记录的 `version`/`resolved`/`integrity` 与 postcss 内 `dependencies.nanoid`（10 个已有值）。其它 217 记录、根/web 镜像、`name`/`version`/`lockfileVersion`、业务 HTML 不动。
- 本批审计文档版本继续由 `findings.md` 与两席 raw 的 Vx.xx 承担。
- 若操作员坚持 JSON 锁必须有文件内 Vx.xx：停下另给指示，本席不得发明字段、不得改 npm `version`。

此项使整批候选不能定稿。001—003 的钉死本身不因该项被推翻。

## 6. 对抗式自问（grilling，自答，不等操作员）

| # | 问题 | 本席答案 |
|---|---|---|
| Q1 | 生产 omit=dev=0，是否因此队列否？ | 否。队列依据是锁携带已复现库缺陷 + 冻结 GEN-010/012，不是客人已能打。候选已把严重度限定为工具链，本席接受该限定。 |
| Q2 | 是否要把 B1 PASS 改成 FAIL？ | 否。B1 当时未跑漏洞库，PASS 对当时已验证事实成立。本 B2 补跑后接受差集，不回溯改写 V0.01。 |
| Q3 | 是否新判据「锁必须无 GHSA」？ | 否。只用已冻结 GEN-010 路径穿越 / 敏感信息泄露、GEN-012 无界循环、GEN-013 范围内相容修补。不 LOCK-AMEND。 |
| Q4 | 广泛 `npm audit fix`？ | 禁止。与候选一致。 |
| Q5 | documentVersion 既可能被 Arborist 保留，是否就该加？ | 否。保留能力不是必要性。见 §5。 |
| Q6 | 漏并了 undici 另外 4 条 advisory？ | 否。同包同 7.29.0，一条 003 足够。 |

## 7. 技能与禁止项

本席本轮按收窄口径使用：`using-superpowers`（开工读）、`grilling`（自问自答、不等待、不派子代理）、`verification-before-completion`（上表命令）、`receiving-code-review`（先核实再接受或反驳；B2 禁止 IMPLEMENT）、`ponytail-review`（删掉 documentVersion，留下三包同主版钉死）。未派子代理，未改源码/HTML/lock/台账，未新建产物/问题/角色，未进 B3/B5。

## 8. 结论

**B2 FAIL。**

六项归并本身：001—003 技术性 / 工程缺陷是 / 队列是 / 同主版钉死；004—006 技术性 / 缺陷否 / 队列否；T01—T12 全覆盖、CX 差集不漏并；无添加项。原 B1 PASS 原文保留。

整批候选因修复边界含 `documentVersion` 不能定稿。收窄该字段后须改候选台账并再派本席 B2（控制 ④）。在此之前禁止 B3。

B2 结束。不修复。

# M004 / B2 修订候选整批复核（Grok 席乙）

版本：V0.03

席位：Grok（审计席乙）。步骤：B2 整批候选定稿审查（修订后第二次整批）。模块：M004 / 322，`01-root-config-and-guidance-02`。控制 V0.17。ledger 现值：IN_PROGRESS，B3=0。本席原 B1 V0.01 PASS、无 GR 全文保留；本席首批 B2 V0.02 FAIL 全文保留，不被本方 PASS 覆盖。本席不重开 B1、不修源码、不推进其他模块、不写 findings/ledger/三份 HTML/锁/计划、不派子代理、不进入 B3/B5、不新写独占 `CRITERIA-LOCK-SHA256:` 字段行。

**B2 PASS。**

不是模块 PASS，不是 B3 已执行，不是有条件通过。本票只覆盖本批候选定稿。主锁仍未写。

## 1. 输入（只采信指定完整原始证据）

| 输入 | 派发 | 本席实算 | 采信范围 |
|---|---|---|---|
| `docs/audit-2026-09/findings.md` | V0.44，157999 字节，SHA256=`209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2` | `sha256sum` 同值，157999 字节 | 只审最前「M004 B1完整双席归并候选（V0.44当前，B2修订候选待双PASS）」至「B3=0保持。」；M001—M003 历史只保留不重审 |
| `docs/audit-2026-09/modules/M004-b1-self-raw.md` | 当前 V0.03 前 70453 字节，SHA256=`f600e1b16b80084d6a72ec8f2cbaf57c8e5ee00c6c0f484885c3093c21d62e27` | 该路径已增长（本轮开始后全文件 76213 字节）。前 70453 字节 SHA256 与派发逐字相同 | **只采信这 70453 字节**（B1 V0.01+V0.02 + 本方对 V0.43 的首批 B2）。其后新追加按指令不读、不引用、不采信 |
| `docs/audit-2026-09/modules/M004-b1-grok-raw.md` | 当前 V0.02 前 66531 字节，SHA256=`888887e239ab1b3bfc75aa6158ed7755e9fdd48d41d7f7e696dfacde15568a7f` | 追加前全文件恰 66531 字节，SHA256 与派发逐字相同 | 追加前全部 V0.01 B1 + V0.02 首批 B2 FAIL；本追加不是 B1，不改写原 PASS/FAIL |

本席本轮实算 `docs/audit-2026-09/plan/criteria-lock.md` 全文 SHA256 为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与 sidecar 文本、ledger 顶部基准相同；ledger `LOCK-AMEND-COUNT: 0`。B2 按控制不新写独占锁哈希字段行。M004 映射 `GEN-001`…`GEN-018` 共 18 条，无 HTML 派生 ID。

`docs/audit-2026-09/questions/` 无 M004 文件。无添加项、无未答问题、无三份 HTML 申请。

`HEAD`=`origin/main`=`9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b`。`package-lock.json` 仍 114212 字节 / 3320 行 / SHA256=`0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d`，与两席 B1 及本席首批 B2 一致。顶层键仅 `name, version, lockfileVersion, requires, packages`，磁盘上仍无 `documentVersion`。隔离验证未写主锁。

## 2. 技能预检（本席自核）

按 §6 收窄口径实际使用：`using-superpowers`（开工读）、`receiving-code-review`（先核实再接受或反驳；B2 禁止 IMPLEMENT）、`grilling`（自问自答、不等待、不派子代理）、`verification-before-completion`（下列命令均本轮实跑）、`ponytail-review`（只评是否可删；本批 001—003 不可删，`documentVersion` 在用户原文下亦不可删）。

已核存在、本轮未调用本体：`systematic-debugging`、`diagnosing-bugs`、`codebase-design`、`tdd`/`test-driven-development`、`resolving-merge-conflicts`、`writing-for-agents`、`i-have-adhd`、`ponytail`、`git-guardrails-claude-code`（可选）。

缺口（不冒充已调用）：`batch-grill-me` 无独立技能（已并入 grilling）；`ubiquitous-language` 本席运行环境无独立 SKILL.md。

未派子代理。未改源码/HTML/lock/台账。未新建产物/问题/角色/判据/闸。

## 3. 本席本轮只读核验（类型 3/4；库级类型 2）

命令均在 `/home/james/complex-management` 或 `/tmp/m004-b2-v044-verify`（隔离），未 `source` 环境文件，未写源、未 `npm ci`/`npm install`/`npm audit fix`、未改主工作区 `node_modules`。

### 3.1 锁与漏洞库（与首批 B2 同口径复跑）

| 命令 | 退出码 | 本席读到的结果 |
|---|---|---|
| `npm audit --package-lock-only --ignore-scripts --json` | 1 | `high=3,total=3`；键仅 `nanoid`/`postcss`/`undici`。nanoid 两条 GHSA（range `<=3.3.17`）；postcss 两条（range `<=8.5.22`）；undici 五条（range `7.0.0 - 7.28.0`）。`fixAvailable` 均为 true |
| `npm audit --omit=dev --package-lock-only --ignore-scripts --json` | 0 | high/total=0 |
| `npm view nanoid@3.3.18 dist` | 0 | tarball `https://registry.npmjs.org/nanoid/-/nanoid-3.3.18.tgz`；integrity `sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w==` |
| `npm view postcss@8.5.23 dist/dependencies` | 0 | tarball `…/postcss-8.5.23.tgz`；integrity `sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg==`；`dependencies.nanoid`=`^3.3.16`；`picocolors`/`source-map-js` 与锁内 8.5.15 相同 |
| `npm view undici@7.29.0 dist` | 0 | tarball `…/undici-7.29.0.tgz`；integrity `sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw==`；engines `>=20.18.1` 与锁内 7.28.0 相同 |
| 锁内父范围 | — | postcss `nanoid` `^3.3.12`（L2070）；vite `postcss` `^8.5.15`（L3062）；jsdom `undici` `^7.25.0`（L1592）。caret SAT 仍成立 |

当前锁内待改 10 值（未实施）：

- nanoid `version`/`resolved`/`integrity`：`3.3.12` / `…/nanoid-3.3.12.tgz` / `sha512-ZB9RH/39qpq5Vu6Y+NmUaFhQR6pp+M2Xt76XBnEwDaGcVAqhlvxrl3B2bKS5D3NH3QR76v3aSrKaF/Kiy7lEtQ==`
- postcss `version`/`resolved`/`integrity`/`dependencies.nanoid`：`8.5.15` / `…/postcss-8.5.15.tgz` / `sha512-FfR8sjd4em2T6fb3I2MwAJU7HWVMr9zba+enmQeeWFfCbm+UOC/0X4DS8XtpUTMwWMGbjKYP7xjfNekzyGmB3A==` / `^3.3.12`
- undici `version`/`resolved`/`integrity`：`7.28.0` / `…/undici-7.28.0.tgz` / `sha512-cRZYrTDwWznlnRiPjggAGxZXanty6M8RV1ff8Wm4LWXBp7/IG8v5DnOm74DtUBp9OONpK75YlPnIjQqX0dBDtA==`

### 3.2 `documentVersion` 与 npm 协议（本席独立，不采信本方叙述）

**协议字段（类型 3）**：`npm help package-lock-json` File Format 顶层枚举 `name` / `version` / `lockfileVersion` / `packages` / `dependencies`。无 `documentVersion`。候选明确写「此字段不声称是 npm 标准协议字段」——与手册一致，不构成把扩展升格为协议。

当前锁 `version=0.1.0` 是应用 semver，`lockfileVersion=3` 是协议格式。二者均须保留。把它们改成 `Vx.xx` 会破坏 npm 语义，本席不同意改。raw/findings 的 Vx.xx 是审计产物版本，不是被改文件自身版本。

**`swKeyOrder` 不是拒绝 schema（类型 3，源码）**：

宿主 npm 11.19.0 `shrinkwrap.js` L56–65 与官方 `npm@10.9.8` tarball（本席 `npm pack`，SRI `sha512-fYwb6ODSmHkqrJQQaCxY3M2lPf/mpgC7ik0HSzzIwG5CGtabRp4bNqikatvCoT42b5INQSqudVH0R7yVmC9hVg==` 与 registry 一致）`node_modules/@npmcli/arborist/lib/shrinkwrap.js` L56–65 同为八键优先列表。`toString` 调用 `stringify(data, swKeyOrder, indent)`。`json-stringify-nice` 把该数组当 `prefKeys`：在 `prefKeys` 中的键按列表序，不在列表中的键仍经 `Object.entries` 进入结果，按 `localeCompare` 排在优先键之后。本席用该实现对 `{name,version,lockfileVersion,requires,packages,documentVersion}` 序列化，输出含 `"documentVersion": "V0.01"`，位于 `requires` 之后、`packages` 之前。首批 B2 把 `swKeyOrder` 写成「闭集」不成立，本轮按源码收回该依据。

**load / toJSON 保留扩展（类型 3）**：npm 10.9.8 同文件 L490–495 `this.data = { ...data, lockfileVersion, requires: true, packages, dependencies }` 展开原顶层键；L959–961 格式 3 只剔除 `dependencies`。宿主 npm 11 对应 L520–526 / L1009–1011，语义相同。`commit()` 重建的是 `this.data.packages`，不重置其它顶层键。

**内存往返（类型 4，未写盘）**：宿主 Arborist `loadVirtual` → 克隆 `toJSON()` 为 baseline → `t.meta.data.documentVersion='V0.01'` → 再 `toJSON()`。结果：`out.documentVersion==='V0.01'`；删该键后与 baseline `deepEqual`；全部 inventory `edgesOut` 签名 582 条不变；`packages` 220；`toString()` 含该键；磁盘锁 SHA 仍 `0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d`。与候选声称的 582 边 / 220 记录 / 未写盘一致。本席独立复现，不引用本方未读后缀。

**「允许保留」≠ 必要性。** 必要性见 §5。兼容性由上述源码 + 实跑支持。

### 3.3 官方修补包隔离反例（类型 2；≠ B3）

本席将 `nanoid-3.3.18.tgz` / `postcss-8.5.23.tgz` / `undici-7.29.0.tgz` 下到 `/tmp`，SRI 与 §3.1 官方 integrity 逐字 MATCH。在自动删除的临时目录加载，不改主锁、不改主 `node_modules`。

| 包 | 本席隔离结果 | 与候选 |
|---|---|---|
| nanoid 3.3.18 | `customAlphabet('abc',0)()` → `""`；`nanoid(6)` 长度 6；`non-secure.nanoid(-1)` → `""`（B1 无限循环路径，不再挂起）。默认 CJS `nanoid(-1)` 抛 `Wrong ID size`（不是空串，也不是挂起） | 候选写「负size返回空串」与 **non-secure** 路径及 customAlphabet 零长一致；未写 CJS 抛错。缺陷（无界循环）已不在 3.3.18。此为隔离结果措辞精度，不改 001 归并 |
| postcss 8.5.23 | 合成 CSS 绝对 `sourceMappingURL=/tmp/…/outside.map`，`postcss.parse` 不设 `from`：`MAP_NULL true`，map 对象中无 `AUDIT_SYNTHETIC_MARKER`；磁盘 marker 文件仍在（未读入）。临时上下文随后删除 | 与候选「未读合成绝对 outside.map（MAP null）」一致 |
| undici 7.29.0 `parseCacheControlHeader` | `private=` / `private=""` / `private, private=hdr` / `public, max-age=300, private` 均 `private:true`，不抛 `TypeError` | 与候选一致 |

以上只证明候选修补版能消掉本席已复现的库级反例，**不等于 B3 已修**。主锁 SHA 未变。

## 4. 全差集与 T01—T12 覆盖（不只看争议点）

Grok B1 临时 finding：**无**。技术登记 T01—T12，缺陷栏均为否。Codex B1：CX-01、CX-02、CX-03。其余 220 记录无缺陷技术登记。

| 原临时号 / 登记 | 候选正式 ID | 漏并？ | 改写原发现？ |
|---|---|---|---|
| CX-01；Grok 无 GR | M004-001 | 否 | 否。仍标差集，未把 B1 改写成「Grok 当时已同意修复」 |
| CX-02；Grok 无 GR | M004-002 | 否 | 否 |
| CX-03；Grok 无 GR | M004-003 | 否 | 否。undici 五条 advisory 同包同 7.29.0，不另拆 ID |
| T01—T04 | M004-004 | 否 | 否。只登记；缺陷否、队列否 |
| T05—T07、T09—T12 | M004-005 | 否 | 否。安全版本问题不在本条重复待修 |
| T08 | M004-006 | 否 | 否。完整性 ≠ 无漏洞 |

T01—T12 逐条落入与首批 B2 相同：004←T01–T04；005←T05–T07、T09–T12；006←T08。无第三套临时号、无重复正式 ID。Grok 438 边与 Codex/本席 Arborist 582 边（含 peer）口径不同，候选已写明非冲突。B1 观察（optional peer UNMET、宿主 npm 11 vs `packageManager` 10.9.8、dry-run 数量差、上游 engines 拷贝）仍不升级为缺陷。

六项分类（2.1 顺序）：三份 HTML 无 npm 依赖树条文 → 非冲突；不能把「前端要能构建」锚到某一 `td.k`/`h2` → 非细化；无票种/角色/价格/审批等业务发明 → 非添加。六项均为**技术性**。`documentVersion` 同判：无业务发明、无 HTML 冲突，不是 2.2 添加项。

队列表：6 行，队列是 3、否 3，与候选表及首批已接受的三是三否一致。10 个既有依赖字段修补边界不变。无添加项人闸。

### M004-001 nanoid 3.3.12

技术性；工程缺陷是；队列是。`dev: true`；omit=dev audit 0；应用源无 nanoid 引用。锁携带已复现无界循环（GEN-012）与 GHSA。候选取同主版 3.3.18，覆盖 audit 上界，仍在父 `^3.3.12` 及修后 postcss `^3.3.16` 内。最小处置：本包 `version`/`resolved`/`integrity` → 官方 3.3.18。不升 5.x，不加业务参数闸，不 `npm audit fix`。接受。

### M004-002 postcss 8.5.15

技术性；工程缺陷是；队列是。Vite `runPostCSS` 设 `from`，不得写成当前主路径已可利用。锁携带 `<=8.5.22` 路径读取缺陷（GEN-010）。最小处置：本包三字段 → 8.5.23，且 `dependencies.nanoid` `^3.3.12`→`^3.3.16`（官方 8.5.23 元数据；其余依赖字段相同）。不升 vite。接受。

### M004-003 undici 7.28.0

技术性；工程缺陷是；队列是。未证明 jsdom 默认启用 cache interceptor。锁携带 7.x `<7.29.0` 已知缺陷（GEN-009/010）。最小处置：本包三字段 → 7.29.0。不升 jsdom。五条 advisory 一条 ID。接受。

### M004-004 / 005 / 006

技术性，缺陷否，队列否。与首批 B2 接受口径相同。接受。

## 5. `documentVersion` — 首批否决点的独立重核（receiving-code-review）

首批 B2 FAIL 的唯一否决点是：修复边界把非协议字段写入 `package-lock.json`。当时最小处置是「从边界删除该字段，版本由 findings/raw 承担；若操作员坚持文件内 Vx.xx 再另给指示」。

本轮核实：

1. **用户原文存在且不是调度者自造规则。** 候选逐字引用：「版本铁律：所有版本号统一使用 Vx.xx 格式；任何对话中只要更新文件内容，该文件自身版本号末位必须前进一位（如 V0.02 → V0.03），发布日期或文件名不能代替版本号递增。」本轮派发确认该原文由操作员直接给出、未限定文档/HTML/JS、不需重新授权、不能由总调度或外审默默豁免。仓库内可独立对照的既有用户裁决 `docs/prd-audit/business-decision-ledger.md` DEC-R8-96：「此例外仅适用于三份业务 HTML；其他文件仍执行 `Vx.xx` 版本递增规则。」射程第 3 条：「除三份业务 HTML 外，其他被更新文件仍须按版本铁律递增自身版本号。」`package-lock.json` 不是三份业务 HTML。本席 M003 B4 已按同一铁律接受无自身版本文件首次写入 `V0.01`（`.env.example` 注释、Markdown/JS 文件头）。JSON 不能写注释，不能把该要求缩成「仅审计文档」。

2. **与 npm 协议的区别成立。** 见 §3.2。`documentVersion` 是用户指定的文件自身版本元数据扩展，不是 npm 协议字段；`version` 与 `lockfileVersion` 保留。审计产物版本 ≠ 被改文件自身版本。把版本只写在 findings/raw **不满足**「该文件自身版本号」。改应用 `version` 或另造产物更大，且不满足「该文件自身」。

3. **兼容性独立成立。** npm11 内存 `loadVirtual`/`toJSON` 全部规范化字段相等、582 边不变、220 记录不变、未写盘；npm 10.9.8 官方包源码 `...data` 保留未知顶层字段、格式 3 只剔除 `dependencies`；`swKeyOrder` 为排序优先列表。未在 npm 文档列名只说明扩展不参与安装，不能推断 npm 禁止该元数据。

4. **GEN-001 不再支持否决。** 「不为假设场景增加抽象」针对要两三件不巧才发生的路径。本文件一旦 B3 更新内容，版本铁律**第一次正常跑就会撞上**。字段服务于已有用户规则，不是为 `npm ci` 发明的安装抽象。`npm ci` 不读该键不是删除它的理由。

5. **B4 归因。** 把该键归 001「同文件版本维护」与 M001/M003 已接受口径相同：一次内容更新、一文件一个自身版本步进，归因到本批导致该文件被写的队列「是」ID。001—003 同文件同批写入，只建一次 `V0.01`。不另开 ID，不新增产物。

6. **首批「删除该字段 / 只写审计文档」与现存用户原文不相容**，本轮不再作为最小处置。本席也不提出铁律 5 已排除的日常再批准闸。

结论：在候选已写明「非协议字段、不改 npm `version`/`lockfileVersion`、必要性来自用户原文、兼容性有源码和实跑」的前提下，顶层增加 `documentVersion: V0.01` 是与用户原文和 npm 协议**同时相容**的最小 JSON 合法落点。本席接受该边界。首批 V0.02 FAIL 原文保留作当时证据，不回溯改写。

## 6. 对抗式自问（grilling，自答，不等操作员）

| # | 问题 | 本席答案 |
|---|---|---|
| Q1 | 是否因本方补充要求而盲从？ | 否。独立复跑锁哈希、audit、官方元数据、Arborist 往返、npm 10.9.8 源码、`json-stringify-nice`、三包隔离。源码证明首批「闭集」依据不成立后才改判。 |
| Q2 | 生产 omit=dev=0，是否因此队列否？ | 否。同首批：队列依据是锁携带已复现库缺陷 + GEN-010/012，不是客人已能打。 |
| Q3 | 是否要把 B1 PASS 改成 FAIL？ | 否。B1 当时未跑漏洞库。 |
| Q4 | 是否新判据「锁必须无 GHSA」或「锁必须有 documentVersion」？ | 否。001—003 用已冻结 GEN-010/012/013。`documentVersion` 执行的是既有用户版本铁律 + DEC-R8-96，不 LOCK-AMEND。 |
| Q5 | 广泛 `npm audit fix`？ | 禁止。 |
| Q6 | 隔离反例是否等于已修？ | 否。主锁 SHA 未变，B3=0。 |
| Q7 | 漏并 undici 另外 4 条 advisory？ | 否。同包同 7.29.0。 |
| Q8 | 键名 `documentVersion` 用户未逐字指定，是否因此 FAIL？ | 否。用户指定的是「该文件自身版本号 / Vx.xx」，未指定 JSON 键名。键名只要不占用协议字段、并在候选中声明非协议即可；与 `.env` 用 `# 版本`、Markdown 用 `版本：` 同属文件类型语法。改 `version`/`lockfileVersion` 或另造文件更差。 |

## 7. 结论

**B2 PASS。**

输入 SHA：findings V0.44 `209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2`（157999）；self 前 70453 `f600e1b16b80084d6a72ec8f2cbaf57c8e5ee00c6c0f484885c3093c21d62e27`；本文件追加前 66531 `888887e239ab1b3bfc75aa6158ed7755e9fdd48d41d7f7e696dfacde15568a7f`。

六项：001—003 技术性 / 工程缺陷是 / 队列是 / 同主版钉死（10 个既有依赖字段）；004—006 技术性 / 缺陷否 / 队列否。T01—T12 全覆盖、CX 差集不漏并。无添加项。原 B1 PASS 与首批 B2 FAIL 原文保留。

修复边界接受：三包官方元数据 + postcss 内 nanoid 范围，以及顶层 `documentVersion: V0.01`（非 npm 协议字段；`version`/`lockfileVersion` 不动）。隔离反例已核，不等于已修。禁止 B3 前再扩队列。未双席定稿前不得实施。

B2 结束。不修复。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M004 B1 Grok 独立审计报告（当前 471 计划）

- 报告版本：V0.04（本文件既有 V0.01–V0.03 全部字节保留，本次完整 B1 报告追加；自身版本 V0.03→V0.04）
- 席位：Grok（唯一外审）。不派 subagent，不用 MCP_Codex_160 / MCP_Cloud_160。
- 步骤：B1 修复前全量审计。本席不修代码、不改方案、不写 findings/ledger/计划/锁/HTML。
- 模块：M004 — 治理门禁与 CI 基线／配置与工程入口／第2段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md 当前切分
- 唯一正本：`/home/james/complex-management` `main`
- 唯一受审文件：`package-lock.json`（3321 行 / 114258 字节 / OVERSIZE-SINGLE-FILE / 自身 `documentVersion` V0.02）
- 冻结映射：`criteria-lock.md` L6383 `M004 → [GEN-001 … GEN-018]`，无 HTML 派生 ID
- ledger 本模块行（只读顶部基准与该行）：`IN_PROGRESS`；历史 B3 累计 1、方案修复 2、交付失败 0，不清零
- 旧 322 同号报告与本文件 V0.01–V0.03 只作历史，**不继承旧 PASS**
- 互盲：未读 `M004-b1-self-raw.md`、findings、其他模块收口、本模块旧报告正文；旧 raw 仅取版本元数据以便追加
- HEAD / origin/main：`242f487233d028173cb9198eecd079f5d877405c`；锁文件 git 干净
- 锁 SHA256：`d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef`

## 0. 技能预检（V0.17 §6，收窄口径）

| 技能 | 本环境 | 本次用法 |
|---|---|---|
| `using-superpowers` | 已读 SKILL.md | 开工技能自检 |
| `grilling` / `grill-me` | 已读 | 自问自答追问结构；不等操作员、不派子代理（本体提问/第三席条款作废） |
| `batch-grill-me` | **缺口**：本机 `.claude`/`.agents`/`.codex`/`.grok` 未找到该技能目录 | 记缺口，未冒充调用 |
| `ubiquitous-language` | 存在（如 `~/.qwen/skills/ubiquitous-language`） | 只收窄术语：锁/packages/integrity/link/optional；**不写** `UBIQUITOUS_LANGUAGE.md` |
| `codebase-design` | 已读 | 只冻结 general 词表（模块/接口/缝），不改架构、不写额外设计文件 |
| `diagnosing-bugs` / `systematic-debugging` | 已读 | 只读复现：解析→虚拟树→边有效性；不改代码、不加 instrumentation |
| `ponytail-review` | 已读 | 只评可删除价值，不实施 |
| `verification-before-completion` | 已读 | PASS 前只采本轮实跑命令 |
| `tdd` / `test-driven-development` | 已读 | 纯锁配置、无行为改动，不新增测试文件 |
| `git-guardrails-claude-code` | 存在 | 本步无 git 写，未启用 |
| `resolving-merge-conflicts` | 必核清单中 | 无合并冲突，未用 |

明确未用（提示词划掉或本步不适用）：brainstorming/writing-plans（阶段 A）、subagent 类、worktree、research、domain-modeling、ponytail-audit/debt/gain。

## 1. 范围与只读对照（不扩大审计）

`plan/modules.md` L47–56：本模块文件列表仅 `package-lock.json`，有效内容 3321 行，标记 OVERSIZE-SINGLE-FILE。
为核实锁，只读对照（验证直接依赖，不当作其他模块审计）：

- 根 `package.json`（工作区/引擎/devDependencies/name/version）
- `apps/web/package.json`（工作区包声明）
- 本机 `node v24.20.0` / `npm 11.19.0` / linux x64 / glibc 2.39
- 宿主 Arborist：`/usr/lib/node_modules/npm/node_modules/@npmcli/arborist`（`npm help package-lock-json` + `shrinkwrap.js`）
- CI 锁入口（只确认消费方式）：`.github/workflows/predeploy-gates.yml` L23 与 `ops/ci/predeploy-gates.yml` L23 `npm ci --ignore-scripts`（文件属 M005/M007，**不审那些文件**）
- 既有锁扫描工具：`tools/verify-no-legacy-api-prototype.mjs`（读锁文本）

未改任何受审或对照文件。未 `npm install` / `npm ci` 实装 / `npx` / `npm exec`。`npm ls --package-lock-only --offline` 与 Arborist `loadVirtual` 经帮助文本确认只读锁、不写盘；运行前后 `package-lock.json` inode/size/mtime 不变。计算副本仅 `/tmp/m004-lock-analysis.txt`、`/tmp/m004-arborist.txt`、`/tmp/m004-npm-ls.json`。

三份权威 HTML 中「依赖」出现处均为设备依赖/拆分部署/密钥托管等业务语义，**不是 npm 锁**。未能把本文件锚到某一 `td.k`/`h2`/`h3` 作为上位需求，故整文件按 2.1 落到**技术性**（无业务发明 + 无 HTML 冲突）。不把包名推断成新业务需求。已知 BSM1020/1068 原意澄清与本锁无关。全局 html-ledger-sync/entry-anchors 红灯归 M425/M427，本模块不为刷绿改 HTML/CSV，不重开 M001–M003。

## 2. 第一性原理

### 2.1 锁为什么必须存在

`npm help package-lock-json`（类型 3）原文要点：锁是清单的显现；描述恰好生成的树，使同事、部署与 CI 安装同一组依赖；避免把 `node_modules` 提交进库；npm v7+ 的 `packages` 含完整树信息。

删掉本文件：

- 三份 HTML 没有任何一条「必须有 package-lock.json」的明文，因此**答不上 HTML 哪一条会做不到**。
- 按 2.1 不因此删除：它承载前端工作区的可复现安装（GEN-013），属构建/依赖元数据，无票种/角色/价格/审批等业务发明。
- CI 已写 `npm ci --ignore-scripts`，无锁则该入口按 npm 文档会失败。这是工程消费事实，不是 HTML 发明。

### 2.2 每类元数据 / 包条目用途

| 字段/类 | 用途（规范原文 + 本文件） | 无效重复？ | 未验证前提 |
|---|---|---|---|
| `name` / `version` | 与根 `package.json` 对齐的包标识与应用 SemVer `0.1.0` | 否 | 无 |
| `lockfileVersion: 3` | npm v9+ 格式，仅 `packages`、无遗留顶层 `dependencies` | 否 | 无 |
| `documentVersion: V0.02` | **非 npm 协议字段**；本文件自身 Vx.xx。Arborist `this.data = { ...data, ...}` 加载保留；`toJSON` 在 lfv≥3 剔除的是 `dependencies` 不是未知键 | 否 | 不声称它参与安装解析 |
| `requires: true` | 锁格式兼容字段，Arborist 加载时写回 | 否 | 无 |
| `packages[""]` / `packages["apps/web"]` | 根与工作区清单镜像（workspaces/engines/deps） | 否 | 无 |
| `packages["node_modules/apps-web"]` | `link: true` + `resolved: "apps/web"`；规范：link 记录无其它字段 | 否 | 无 |
| 217 个 registry 记录 | `version` + `resolved`（https://registry.npmjs.org/…tgz）+ `integrity` sha512-SRI | 同名嵌套 4 组是不同路径/版本的合法树，不是无效重复 | **未下载 tarball，未证明远端字节等于 SRI** |
| `dev` / `optional` / `peer` / `os` / `cpu` | 树角色与平台可选分支 | 否 | 本机未安装非 linux-x64 的 optional，属预期 |
| `hasInstallScript`（两处 fsevents） | 规范字段；CI 使用 `--ignore-scripts` | 否 | 未在 darwin 实跑安装脚本 |

### 2.3 声称行为是否被执行验证

锁声称：给定此文件，npm 能得到确定虚拟树。

- **已验证（类型 3+4）**：JSON 可解析；lfv=3；220 `packages` 键与源文件分段一一对应；根/工作区声明与两份 `package.json` 全等；Arborist `loadVirtual` 库存 220、出边 582、无 required missing、无 invalid；内存 `toJSON` 仍 220 包且保留 `documentVersion=V0.02`；`npm ls --package-lock-only --offline --all` exit 0、582 节点、`problems=null`；声明范围 semver.satisfies 全通过（根/工作区 17 条直依 + 锁内 438 条 deps/optional/devDep 边）。
- **未验证、不得伪称**：registry 可达、远端 tarball 字节、`npm ci`/`npm install` 全安装、`npm audit`、Vite/业务测试、`verify:predeploy` 全绿。

### 2.4 未经验证就当真的前提（明示）

1. 安装时 registry.npmjs.org 可返回与 `resolved` 相同的对象 — **本次无网络证明**。
2. 对象字节的 sha512 等于 `integrity` — **未取回 tarball，只验证了 SRI 语法与 64 字节解码长度（217/217）**。
3. 磁盘 `node_modules` 等于锁 — **明确不等于**：本机缺 darwin/win/android 等 optional 属正常；另有 `.vite` 缓存与 hidden lock，不作为本锁缺陷。
4. 未跑构建，不能把虚拟树 PASS 写成构建 PASS。

## 3. 顺从式（18 GEN 逐项）

判据全文以 `criteria-lock.md` L10–100 `## general` 为准。

| ID | 对本锁的适用 | 证据 | 结论 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 见 §2.1；平台 optional 是 rolldown/lightningcss/fsevents 的声明分支，不是为假设场景另造抽象。ponytail：生成锁无可删业务层；嵌套同版 `@rolldown/pluginutils` 是 workspace 隔离，删会破边 | 满足 |
| GEN-002 可执行正确性 | 配置类：验证解析与虚拟树，不强制全构建 | Arborist loadVirtual 25ms exit 0；npm ls lock-only exit 0；与 package.json 无漂移 | 满足（边界：非构建） |
| GEN-003 测试有效性 | 锁无测试面；不新造 TDD 文件 | 既有消费断言 `node tools/verify-no-legacy-api-prototype.mjs` → `NO_LEGACY_API_PROTOTYPE_PASS` exit 0。该命令不是安装/构建 PASS | 适用为消费扫描，无缺口 |
| GEN-004 信任边界 | 适用到完整性字段 | 217 registry 均有 sha512 SRI；无 http resolved；无 git/file 除 workspace link。**未验证远端字节** | 字段满足；字节未证 |
| GEN-005 授权与最小权限 | 基本不适用 | 锁不含账号/权限模型；无秘密（「token」11 处均为 css-tokenizer/js-tokens 包名） | 不适用/无缺陷 |
| GEN-006 数据完整性 | 适用到引用完整性 | 依赖边 426 条均可在树内解析；required peer 0 缺失；link 仅指向 `apps/web` | 满足 |
| GEN-007 并发与幂等 | 锁为静态快照 | npm ci 语义是冻结安装；文件本身无竞态逻辑 | 不适用/无缺陷 |
| GEN-008 接口与模式一致性 | 适用到清单↔锁 | name/version/workspaces/engines/devDependencies 与根清单一致；apps/web 的 dependencies/devDependencies 与工作区清单一致；lfv3 无遗留 `dependencies` 块 | 满足 |
| GEN-009 错误处理与恢复 | 锁不实现错误路径 | 清单漂移本应让 `npm ci` 失败；本轮直依与锁一致，**未实跑 npm ci**，故未演示失败路径，只证明当前不构成漂移失败输入 | 无缺陷 |
| GEN-010 安全与敏感信息 | 适用 | 无私钥/口令；integrity 全在；hasInstallScript 仅 darwin fsevents；CI 忽略脚本。不把「无 GHSA」做成新判据，**未跑 npm audit** | 满足（边界：无 audit） |
| GEN-011 可观测与审计证据 | 锁即树的结构化证据 | 220 记录、SRI、resolved 可复核；本报告命令可复现 | 满足 |
| GEN-012 资源与性能边界 | 适用到树规模 | 220 包、34 optional，有界；无锁内循环放大 | 满足 |
| GEN-013 构建配置与依赖 | **主判据** | 见 §2–§4：可复现虚拟树、版本相容（当前 node 24.20.0 满足根 `^22.13.0 \|\| >=24.0.0` 及锁内 116 条 engines）、失败显式依赖 npm 实现而非本文件逻辑。`packageManager: npm@10.9.8` 在根 package.json（他模块），锁以 lfv3 自描述，npm 10/11 均读 lfv3 | 满足 |
| GEN-014 数据库迁移 | 不适用 | 无 SQL | 不适用 |
| GEN-015 界面可用性 | 不适用 | 无 UI。包名/协议字段属系统内部标识符，不与「展示全中文」冲突 | 不适用 |
| GEN-016 文档可验证性 | 弱适用 | 本文件不是说明文档；`documentVersion` V0.02 与派发声称一致，可用 JSON 读出 | 满足 |
| GEN-017 业务来源分类 | 适用 | 冲突→细化→技术性→添加：无 HTML 冲突；不能指向上位锚点故非细化；无业务发明故非添加；整文件与每包为技术性并登记理由 T-* | 满足 |
| GEN-018 第一性原理证据 | 适用 | 结论落到规范原文、可复现命令、结构解析；不引用旧席意见或「历史 PASS」 | 满足 |

## 4. 对抗式（grilling 自问，事实已自读）

❓ Q1 首次 `npm ci` 会因锁解析失败吗？
➡️ 虚拟树与清单一致、无缺边/错版本/坏 SRI 语法。解析层未见会在「正常首次安装」立刻炸掉的输入。**未实装，不能声称 ci 已绿。** registry 不可达是环境，不是本文件已证缺陷。

❓ Q2 清单漂移？
➡️ 根与 `apps/web` 的 name/version/workspaces/engines/deps 与锁镜像逐字段相等。`private`/`scripts`/`packageManager`/`type` 按规范不必写入 packages 记录。

❓ Q3 工作区链接错了吗？
➡️ 唯一 link：`node_modules/apps-web` → `apps/web`，无多余字段。工作区键 `apps/web` 存在。

❓ Q4 required peer 缺失？
➡️ 0。29 个缺失 peer 全部 `peerDependenciesMeta.optional=true`（canvas/esbuild/sass/msw/@types/node 等），属可选未装，不是安装错误。`yaml` 因根 devDependency 已满足 Vite 的 optional peer。

❓ Q5 本机 linux-x64 缺原生绑定？
➡️ 锁内同时有 `@rolldown/binding-linux-x64-gnu|musl` 与 `lightningcss-linux-x64-gnu|musl`。本机 glibc 2.39。磁盘缺 fsevents/win/android/darwin 等 optional 与锁声明一致。

❓ Q6 engines 与当前 Node 冲突？
➡️ `semver.satisfies("24.20.0", range)` 对锁内全部 engines.node 为真，含 `>=v12.22.7` 与 `20 \|\| >=22`。CI 的 `node-version: "22"` 在 M005 工作流，不把那份 YAML 扩进本模块缺陷。

❓ Q7 `documentVersion` 会让 npm 拒读吗？
➡️ `Shrinkwrap.load` + `toJSON` 保留该键；inventory 仍 220。不改应用 SemVer、不改 lockfileVersion、不强制补/删该字段。

❓ Q8 要把漏洞库当缺陷吗？
➡️ 否。禁止为极端或可设想场景新增门禁。未跑 npm audit。

❓ Q9 嵌套重复能否 ponytail 删？
➡️ 四组同名：`@rolldown/pluginutils@1.0.1`（两路径）、`dom-accessibility-api` 0.6.3/0.5.16、`fsevents` 2.3.3/2.3.2、`react-is` 18.3.1/17.0.2。均为 npm 嵌套合法树。`net: -0 lines possible.` Lean already. Ship.

❓ Q10 本机 hidden lock / 旧 node_modules 是否证明锁坏？
➡️ 否。hidden lock 97528 字节 ≠ 受审文件 114258 字节。磁盘树缺 optional 平台包是安装结果，不是锁缺记录。

## 5. 连续覆盖表（3321 行，0 行缺口）

分段阅读：L1–900、L901–1800、L1801–2700、L2701–3321 均完整读过，不是抽样。源文件扫描 220 键 = JSON `packages` 220 键；行覆盖 3321/3321。

| 行 | 条目 | 结构 | 分类理由 |
|---|---|---|---|
| 1–7 | 文件头 | name / version / lockfileVersion / documentVersion / requires / packages 开括号 | T-HDR + T-DOCVER |
| 8–21 | (root `""`) | workspace（14 行） | T-WS |
| 22–44 | `apps/web` | workspace（23 行） | T-WS |
| 45–51 | `apps/web/node_modules/@rolldown/pluginutils` | dev-registry（7 行） | T-DEV |
| 52–77 | `apps/web/node_modules/@vitejs/plugin-react` | dev-registry（26 行） | T-DEV |
| 78–84 | `node_modules/@adobe/css-tools` | dev-registry（7 行） | T-DEV |
| 85–93 | `node_modules/@ant-design/colors` | prod-registry（9 行） | T-PROD |
| 94–112 | `node_modules/@ant-design/cssinjs` | prod-registry（19 行） | T-PROD |
| 113–127 | `node_modules/@ant-design/cssinjs-utils` | prod-registry（15 行） | T-PROD |
| 128–139 | `node_modules/@ant-design/fast-color` | prod-registry（12 行） | T-PROD |
| 140–159 | `node_modules/@ant-design/icons` | prod-registry（20 行） | T-PROD |
| 160–165 | `node_modules/@ant-design/icons-svg` | prod-registry（6 行） | T-PROD |
| 166–181 | `node_modules/@ant-design/react-slick` | prod-registry（16 行） | T-PROD |
| 182–198 | `node_modules/@asamuzakjp/css-color` | dev-registry（17 行） | T-DEV |
| 199–215 | `node_modules/@asamuzakjp/dom-selector` | dev-registry（17 行） | T-DEV |
| 216–225 | `node_modules/@asamuzakjp/generational-cache` | dev-registry（10 行） | T-DEV |
| 226–232 | `node_modules/@asamuzakjp/nwsapi` | dev-registry（7 行） | T-DEV |
| 233–248 | `node_modules/@babel/code-frame` | peer-registry（16 行） | T-PEER |
| 249–259 | `node_modules/@babel/helper-validator-identifier` | peer-registry（11 行） | T-PEER |
| 260–268 | `node_modules/@babel/runtime` | prod-registry（9 行） | T-PROD |
| 269–281 | `node_modules/@bramus/specificity` | dev-registry（13 行） | T-DEV |
| 282–301 | `node_modules/@csstools/color-helpers` | dev-registry（20 行） | T-DEV |
| 302–325 | `node_modules/@csstools/css-calc` | dev-registry（24 行） | T-DEV |
| 326–353 | `node_modules/@csstools/css-color-parser` | dev-registry（28 行） | T-DEV |
| 354–376 | `node_modules/@csstools/css-parser-algorithms` | dev-registry（23 行） | T-DEV |
| 377–401 | `node_modules/@csstools/css-syntax-patches-for-csstree` | dev-registry（25 行） | T-DEV |
| 402–421 | `node_modules/@csstools/css-tokenizer` | dev-registry（20 行） | T-DEV |
| 422–433 | `node_modules/@emnapi/core` | optional-registry（12 行） | T-OPT |
| 434–444 | `node_modules/@emnapi/runtime` | optional-registry（11 行） | T-OPT |
| 445–455 | `node_modules/@emnapi/wasi-threads` | optional-registry（11 行） | T-OPT |
| 456–461 | `node_modules/@emotion/hash` | prod-registry（6 行） | T-PROD |
| 462–467 | `node_modules/@emotion/unitless` | prod-registry（6 行） | T-PROD |
| 468–485 | `node_modules/@exodus/bytes` | dev-registry（18 行） | T-DEV |
| 486–492 | `node_modules/@jridgewell/sourcemap-codec` | dev-registry（7 行） | T-DEV |
| 493–511 | `node_modules/@napi-rs/wasm-runtime` | optional-registry（19 行） | T-OPT |
| 512–521 | `node_modules/@oxc-project/types` | dev-registry（10 行） | T-DEV |
| 522–537 | `node_modules/@playwright/test` | dev-registry（16 行） | T-DEV |
| 538–549 | `node_modules/@rc-component/async-validator` | prod-registry（12 行） | T-PROD |
| 550–565 | `node_modules/@rc-component/color-picker` | prod-registry（16 行） | T-PROD |
| 566–579 | `node_modules/@rc-component/context` | prod-registry（14 行） | T-PROD |
| 580–591 | `node_modules/@rc-component/mini-decimal` | prod-registry（12 行） | T-PROD |
| 592–609 | `node_modules/@rc-component/mutate-observer` | prod-registry（18 行） | T-PROD |
| 610–627 | `node_modules/@rc-component/portal` | prod-registry（18 行） | T-PROD |
| 628–643 | `node_modules/@rc-component/qrcode` | prod-registry（16 行） | T-PROD |
| 644–663 | `node_modules/@rc-component/tour` | prod-registry（20 行） | T-PROD |
| 664–684 | `node_modules/@rc-component/trigger` | prod-registry（21 行） | T-PROD |
| 685–701 | `node_modules/@rolldown/binding-android-arm64` | optional-registry（17 行） | T-OPT |
| 702–718 | `node_modules/@rolldown/binding-darwin-arm64` | optional-registry（17 行） | T-OPT |
| 719–735 | `node_modules/@rolldown/binding-darwin-x64` | optional-registry（17 行） | T-OPT |
| 736–752 | `node_modules/@rolldown/binding-freebsd-x64` | optional-registry（17 行） | T-OPT |
| 753–769 | `node_modules/@rolldown/binding-linux-arm-gnueabihf` | optional-registry（17 行） | T-OPT |
| 770–786 | `node_modules/@rolldown/binding-linux-arm64-gnu` | optional-registry（17 行） | T-OPT |
| 787–803 | `node_modules/@rolldown/binding-linux-arm64-musl` | optional-registry（17 行） | T-OPT |
| 804–820 | `node_modules/@rolldown/binding-linux-ppc64-gnu` | optional-registry（17 行） | T-OPT |
| 821–837 | `node_modules/@rolldown/binding-linux-s390x-gnu` | optional-registry（17 行） | T-OPT |
| 838–854 | `node_modules/@rolldown/binding-linux-x64-gnu` | optional-registry（17 行） | T-OPT |
| 855–871 | `node_modules/@rolldown/binding-linux-x64-musl` | optional-registry（17 行） | T-OPT |
| 872–888 | `node_modules/@rolldown/binding-openharmony-arm64` | optional-registry（17 行） | T-OPT |
| 889–907 | `node_modules/@rolldown/binding-wasm32-wasi` | optional-registry（19 行） | T-OPT |
| 908–924 | `node_modules/@rolldown/binding-win32-arm64-msvc` | optional-registry（17 行） | T-OPT |
| 925–941 | `node_modules/@rolldown/binding-win32-x64-msvc` | optional-registry（17 行） | T-OPT |
| 942–948 | `node_modules/@standard-schema/spec` | dev-registry（7 行） | T-DEV |
| 949–969 | `node_modules/@testing-library/dom` | peer-registry（21 行） | T-PEER |
| 970–989 | `node_modules/@testing-library/jest-dom` | dev-registry（20 行） | T-DEV |
| 990–996 | `node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api` | dev-registry（7 行） | T-DEV |
| 997–1024 | `node_modules/@testing-library/react` | dev-registry（28 行） | T-DEV |
| 1025–1038 | `node_modules/@testing-library/user-event` | dev-registry（14 行） | T-DEV |
| 1039–1049 | `node_modules/@tybys/wasm-util` | optional-registry（11 行） | T-OPT |
| 1050–1057 | `node_modules/@types/aria-query` | peer-registry（8 行） | T-PEER |
| 1058–1068 | `node_modules/@types/chai` | dev-registry（11 行） | T-DEV |
| 1069–1075 | `node_modules/@types/deep-eql` | dev-registry（7 行） | T-DEV |
| 1076–1082 | `node_modules/@types/estree` | dev-registry（7 行） | T-DEV |
| 1083–1092 | `node_modules/@types/react` | dev-registry（10 行） | T-DEV |
| 1093–1102 | `node_modules/@types/react-dom` | dev-registry（10 行） | T-DEV |
| 1103–1120 | `node_modules/@vitest/expect` | dev-registry（18 行） | T-DEV |
| 1121–1147 | `node_modules/@vitest/mocker` | dev-registry（27 行） | T-DEV |
| 1148–1160 | `node_modules/@vitest/pretty-format` | dev-registry（13 行） | T-DEV |
| 1161–1174 | `node_modules/@vitest/runner` | dev-registry（14 行） | T-DEV |
| 1175–1190 | `node_modules/@vitest/snapshot` | dev-registry（16 行） | T-DEV |
| 1191–1200 | `node_modules/@vitest/spy` | dev-registry（10 行） | T-DEV |
| 1201–1215 | `node_modules/@vitest/utils` | dev-registry（15 行） | T-DEV |
| 1216–1226 | `node_modules/ansi-regex` | peer-registry（11 行） | T-PEER |
| 1227–1240 | `node_modules/ansi-styles` | peer-registry（14 行） | T-PEER |
| 1241–1305 | `node_modules/antd` | prod-registry（65 行） | T-PROD |
| 1306–1309 | `node_modules/apps-web` | link（4 行） | T-LINK |
| 1310–1319 | `node_modules/aria-query` | dev-registry（10 行） | T-DEV |
| 1320–1329 | `node_modules/assertion-error` | dev-registry（10 行） | T-DEV |
| 1330–1339 | `node_modules/bidi-js` | dev-registry（10 行） | T-DEV |
| 1340–1349 | `node_modules/chai` | dev-registry（10 行） | T-DEV |
| 1350–1355 | `node_modules/classnames` | prod-registry（6 行） | T-PROD |
| 1356–1361 | `node_modules/compute-scroll-into-view` | prod-registry（6 行） | T-PROD |
| 1362–1368 | `node_modules/convert-source-map` | dev-registry（7 行） | T-DEV |
| 1369–1377 | `node_modules/copy-to-clipboard` | prod-registry（9 行） | T-PROD |
| 1378–1391 | `node_modules/css-tree` | dev-registry（14 行） | T-DEV |
| 1392–1398 | `node_modules/css.escape` | dev-registry（7 行） | T-DEV |
| 1399–1404 | `node_modules/csstype` | prod-registry（6 行） | T-PROD |
| 1405–1418 | `node_modules/data-urls` | dev-registry（14 行） | T-DEV |
| 1419–1424 | `node_modules/dayjs` | prod-registry（6 行） | T-PROD |
| 1425–1431 | `node_modules/decimal.js` | dev-registry（7 行） | T-DEV |
| 1432–1441 | `node_modules/dequal` | dev-registry（10 行） | T-DEV |
| 1442–1451 | `node_modules/detect-libc` | dev-registry（10 行） | T-DEV |
| 1452–1459 | `node_modules/dom-accessibility-api` | peer-registry（8 行） | T-PEER |
| 1460–1472 | `node_modules/entities` | dev-registry（13 行） | T-DEV |
| 1473–1479 | `node_modules/es-module-lexer` | dev-registry（7 行） | T-DEV |
| 1480–1489 | `node_modules/estree-walker` | dev-registry（10 行） | T-DEV |
| 1490–1499 | `node_modules/expect-type` | dev-registry（10 行） | T-DEV |
| 1500–1517 | `node_modules/fdir` | dev-registry（18 行） | T-DEV |
| 1518–1532 | `node_modules/fsevents` | optional-registry（15 行） | T-OPT |
| 1533–1545 | `node_modules/html-encoding-sniffer` | dev-registry（13 行） | T-DEV |
| 1546–1555 | `node_modules/indent-string` | dev-registry（10 行） | T-DEV |
| 1556–1562 | `node_modules/is-potential-custom-element-name` | dev-registry（7 行） | T-DEV |
| 1563–1570 | `node_modules/js-tokens` | peer-registry（8 行） | T-PEER |
| 1571–1611 | `node_modules/jsdom` | dev-registry（41 行） | T-DEV |
| 1612–1620 | `node_modules/json2mq` | prod-registry（9 行） | T-PROD |
| 1621–1650 | `node_modules/lightningcss` | dev-registry（30 行） | T-DEV |
| 1651–1671 | `node_modules/lightningcss-android-arm64` | optional-registry（21 行） | T-OPT |
| 1672–1692 | `node_modules/lightningcss-darwin-arm64` | optional-registry（21 行） | T-OPT |
| 1693–1713 | `node_modules/lightningcss-darwin-x64` | optional-registry（21 行） | T-OPT |
| 1714–1734 | `node_modules/lightningcss-freebsd-x64` | optional-registry（21 行） | T-OPT |
| 1735–1755 | `node_modules/lightningcss-linux-arm-gnueabihf` | optional-registry（21 行） | T-OPT |
| 1756–1776 | `node_modules/lightningcss-linux-arm64-gnu` | optional-registry（21 行） | T-OPT |
| 1777–1797 | `node_modules/lightningcss-linux-arm64-musl` | optional-registry（21 行） | T-OPT |
| 1798–1818 | `node_modules/lightningcss-linux-x64-gnu` | optional-registry（21 行） | T-OPT |
| 1819–1839 | `node_modules/lightningcss-linux-x64-musl` | optional-registry（21 行） | T-OPT |
| 1840–1860 | `node_modules/lightningcss-win32-arm64-msvc` | optional-registry（21 行） | T-OPT |
| 1861–1881 | `node_modules/lightningcss-win32-x64-msvc` | optional-registry（21 行） | T-OPT |
| 1882–1891 | `node_modules/lru-cache` | dev-registry（10 行） | T-DEV |
| 1892–1902 | `node_modules/lz-string` | peer-registry（11 行） | T-PEER |
| 1903–1912 | `node_modules/magic-string` | dev-registry（10 行） | T-DEV |
| 1913–1919 | `node_modules/mdn-data` | dev-registry（7 行） | T-DEV |
| 1920–1929 | `node_modules/min-indent` | dev-registry（10 行） | T-DEV |
| 1930–1948 | `node_modules/nanoid` | dev-registry（19 行） | T-DEV |
| 1949–1962 | `node_modules/obug` | dev-registry（14 行） | T-DEV |
| 1963–1975 | `node_modules/parse5` | dev-registry（13 行） | T-DEV |
| 1976–1982 | `node_modules/pathe` | dev-registry（7 行） | T-DEV |
| 1983–1989 | `node_modules/picocolors` | dev-registry（7 行） | T-DEV |
| 1990–2002 | `node_modules/picomatch` | dev-registry（13 行） | T-DEV |
| 2003–2021 | `node_modules/playwright` | dev-registry（19 行） | T-DEV |
| 2022–2034 | `node_modules/playwright-core` | dev-registry（13 行） | T-DEV |
| 2035–2049 | `node_modules/playwright/node_modules/fsevents` | optional-registry（15 行） | T-OPT |
| 2050–2078 | `node_modules/postcss` | dev-registry（29 行） | T-DEV |
| 2079–2094 | `node_modules/pretty-format` | peer-registry（16 行） | T-PEER |
| 2095–2102 | `node_modules/pretty-format/node_modules/react-is` | peer-registry（8 行） | T-PEER |
| 2103–2112 | `node_modules/punycode` | dev-registry（10 行） | T-DEV |
| 2113–2121 | `node_modules/qrcode.react` | prod-registry（9 行） | T-PROD |
| 2122–2138 | `node_modules/rc-cascader` | prod-registry（17 行） | T-PROD |
| 2139–2153 | `node_modules/rc-checkbox` | prod-registry（15 行） | T-PROD |
| 2154–2169 | `node_modules/rc-collapse` | prod-registry（16 行） | T-PROD |
| 2170–2186 | `node_modules/rc-dialog` | prod-registry（17 行） | T-PROD |
| 2187–2203 | `node_modules/rc-drawer` | prod-registry（17 行） | T-PROD |
| 2204–2219 | `node_modules/rc-dropdown` | prod-registry（16 行） | T-PROD |
| 2220–2237 | `node_modules/rc-field-form` | prod-registry（18 行） | T-PROD |
| 2238–2255 | `node_modules/rc-image` | prod-registry（18 行） | T-PROD |
| 2256–2270 | `node_modules/rc-input` | prod-registry（15 行） | T-PROD |
| 2271–2287 | `node_modules/rc-input-number` | prod-registry（17 行） | T-PROD |
| 2288–2306 | `node_modules/rc-mentions` | prod-registry（19 行） | T-PROD |
| 2307–2324 | `node_modules/rc-menu` | prod-registry（18 行） | T-PROD |
| 2325–2339 | `node_modules/rc-motion` | prod-registry（15 行） | T-PROD |
| 2340–2358 | `node_modules/rc-notification` | prod-registry（19 行） | T-PROD |
| 2359–2374 | `node_modules/rc-overflow` | prod-registry（16 行） | T-PROD |
| 2375–2389 | `node_modules/rc-pagination` | prod-registry（15 行） | T-PROD |
| 2390–2428 | `node_modules/rc-picker` | prod-registry（39 行） | T-PROD |
| 2429–2443 | `node_modules/rc-progress` | prod-registry（15 行） | T-PROD |
| 2444–2461 | `node_modules/rc-rate` | prod-registry（18 行） | T-PROD |
| 2462–2477 | `node_modules/rc-resize-observer` | prod-registry（16 行） | T-PROD |
| 2478–2493 | `node_modules/rc-segmented` | prod-registry（16 行） | T-PROD |
| 2494–2515 | `node_modules/rc-select` | prod-registry（22 行） | T-PROD |
| 2516–2533 | `node_modules/rc-slider` | prod-registry（18 行） | T-PROD |
| 2534–2551 | `node_modules/rc-steps` | prod-registry（18 行） | T-PROD |
| 2552–2566 | `node_modules/rc-switch` | prod-registry（15 行） | T-PROD |
| 2567–2587 | `node_modules/rc-table` | prod-registry（21 行） | T-PROD |
| 2588–2609 | `node_modules/rc-tabs` | prod-registry（22 行） | T-PROD |
| 2610–2626 | `node_modules/rc-textarea` | prod-registry（17 行） | T-PROD |
| 2627–2642 | `node_modules/rc-tooltip` | prod-registry（16 行） | T-PROD |
| 2643–2662 | `node_modules/rc-tree` | prod-registry（20 行） | T-PROD |
| 2663–2679 | `node_modules/rc-tree-select` | prod-registry（17 行） | T-PROD |
| 2680–2694 | `node_modules/rc-upload` | prod-registry（15 行） | T-PROD |
| 2695–2708 | `node_modules/rc-util` | prod-registry（14 行） | T-PROD |
| 2709–2727 | `node_modules/rc-virtual-list` | prod-registry（19 行） | T-PROD |
| 2728–2736 | `node_modules/react` | prod-registry（9 行） | T-PROD |
| 2737–2748 | `node_modules/react-dom` | prod-registry（12 行） | T-PROD |
| 2749–2754 | `node_modules/react-is` | prod-registry（6 行） | T-PROD |
| 2755–2768 | `node_modules/redent` | dev-registry（14 行） | T-DEV |
| 2769–2778 | `node_modules/require-from-string` | dev-registry（10 行） | T-DEV |
| 2779–2784 | `node_modules/resize-observer-polyfill` | prod-registry（6 行） | T-PROD |
| 2785–2818 | `node_modules/rolldown` | dev-registry（34 行） | T-DEV |
| 2819–2825 | `node_modules/rolldown/node_modules/@rolldown/pluginutils` | dev-registry（7 行） | T-DEV |
| 2826–2838 | `node_modules/saxes` | dev-registry（13 行） | T-DEV |
| 2839–2844 | `node_modules/scheduler` | prod-registry（6 行） | T-PROD |
| 2845–2853 | `node_modules/scroll-into-view-if-needed` | prod-registry（9 行） | T-PROD |
| 2854–2860 | `node_modules/siginfo` | dev-registry（7 行） | T-DEV |
| 2861–2870 | `node_modules/source-map-js` | dev-registry（10 行） | T-DEV |
| 2871–2877 | `node_modules/stackback` | dev-registry（7 行） | T-DEV |
| 2878–2884 | `node_modules/std-env` | dev-registry（7 行） | T-DEV |
| 2885–2890 | `node_modules/string-convert` | prod-registry（6 行） | T-PROD |
| 2891–2903 | `node_modules/strip-indent` | dev-registry（13 行） | T-DEV |
| 2904–2909 | `node_modules/stylis` | prod-registry（6 行） | T-PROD |
| 2910–2916 | `node_modules/symbol-tree` | dev-registry（7 行） | T-DEV |
| 2917–2925 | `node_modules/throttle-debounce` | prod-registry（9 行） | T-PROD |
| 2926–2932 | `node_modules/tinybench` | dev-registry（7 行） | T-DEV |
| 2933–2942 | `node_modules/tinyexec` | dev-registry（10 行） | T-DEV |
| 2943–2959 | `node_modules/tinyglobby` | dev-registry（17 行） | T-DEV |
| 2960–2969 | `node_modules/tinyrainbow` | dev-registry（10 行） | T-DEV |
| 2970–2982 | `node_modules/tldts` | dev-registry（13 行） | T-DEV |
| 2983–2989 | `node_modules/tldts-core` | dev-registry（7 行） | T-DEV |
| 2990–2995 | `node_modules/toggle-selection` | prod-registry（6 行） | T-PROD |
| 2996–3008 | `node_modules/tough-cookie` | dev-registry（13 行） | T-DEV |
| 3009–3021 | `node_modules/tr46` | dev-registry（13 行） | T-DEV |
| 3022–3029 | `node_modules/tslib` | optional-registry（8 行） | T-OPT |
| 3030–3043 | `node_modules/typescript` | dev-registry（14 行） | T-DEV |
| 3044–3053 | `node_modules/undici` | dev-registry（10 行） | T-DEV |
| 3054–3131 | `node_modules/vite` | dev-registry（78 行） | T-DEV |
| 3132–3221 | `node_modules/vitest` | dev-registry（90 行） | T-DEV |
| 3222–3234 | `node_modules/w3c-xmlserializer` | dev-registry（13 行） | T-DEV |
| 3235–3244 | `node_modules/webidl-conversions` | dev-registry（10 行） | T-DEV |
| 3245–3254 | `node_modules/whatwg-mimetype` | dev-registry（10 行） | T-DEV |
| 3255–3269 | `node_modules/whatwg-url` | dev-registry（15 行） | T-DEV |
| 3270–3286 | `node_modules/why-is-node-running` | dev-registry（17 行） | T-DEV |
| 3287–3296 | `node_modules/xml-name-validator` | dev-registry（10 行） | T-DEV |
| 3297–3303 | `node_modules/xmlchars` | dev-registry（7 行） | T-DEV |
| 3304–3319 | `node_modules/yaml` | dev-registry（16 行） | T-DEV |
| 3320–3321 | 文件尾 | packages 闭括号 + JSON 闭括号 | T-HDR |

行 7（`"packages": {`）计入 1–7 文件头。包记录 L8–3319 连续无重叠。

## 6. 每包 / 条目技术性分类

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **冲突**：锁内容与三份 HTML 明文无矛盾（HTML「依赖」不是 npm）。
- **细化**：不能指向具体锚点原文作为上位，故不归细化。
- **添加**：无业务实体/枚举/状态/角色/价格/流程发明。
- **技术性**：无业务发明 + 无 HTML 冲突。

理由库（每包引用其一，避免用包名发明业务）：

- **T-HDR**：npm lockfileVersion 3 协议元数据，只服务安装器读取。
- **T-DOCVER**：文件自身 Vx.xx 元数据扩展，不参与解析算法；不是业务规则。
- **T-WS**：工作区清单在锁内的镜像，使 `npm ci` 不必猜 package.json 之外的树。
- **T-LINK**：规范要求的 symbolic link 记录，无其它字段。
- **T-PROD**：工作区运行时依赖及其传递闭包（React/antd 等 UI 库），是前端能构建运行的技术依赖，不引入 HTML 未写的票务/商户规则。
- **T-DEV**：测试/构建工具链（vite/vitest/playwright/jsdom/typescript）。
- **T-OPT**：`optionalDependencies`/`os`/`cpu` 平台可选工件，安装器按本机选择。
- **T-PEER**：已安装的 peer 记录（如 `@testing-library/dom`），不是新业务权限模型。

种类计数：workspace 2、link 1、prod-registry 71、dev-registry 101、optional-registry 34、peer-registry 11，合计 220。

| 行 | 路径 | 版本 | 种类 | 标志 | 2.1 | 理由 |
|---|---|---|---|---|---|---|
| 8–21 | (root `""`) | 0.1.0 | workspace | — | 技术性 | T-WS |
| 22–44 | `apps/web` | 0.1.0 | workspace | — | 技术性 | T-WS |
| 45–51 | `apps/web/node_modules/@rolldown/pluginutils` | 1.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 52–77 | `apps/web/node_modules/@vitejs/plugin-react` | 6.0.2 | dev-registry | dev | 技术性 | T-DEV |
| 78–84 | `node_modules/@adobe/css-tools` | 4.5.0 | dev-registry | dev | 技术性 | T-DEV |
| 85–93 | `node_modules/@ant-design/colors` | 7.2.1 | prod-registry | — | 技术性 | T-PROD |
| 94–112 | `node_modules/@ant-design/cssinjs` | 1.24.0 | prod-registry | — | 技术性 | T-PROD |
| 113–127 | `node_modules/@ant-design/cssinjs-utils` | 1.1.3 | prod-registry | — | 技术性 | T-PROD |
| 128–139 | `node_modules/@ant-design/fast-color` | 2.0.6 | prod-registry | — | 技术性 | T-PROD |
| 140–159 | `node_modules/@ant-design/icons` | 5.6.1 | prod-registry | — | 技术性 | T-PROD |
| 160–165 | `node_modules/@ant-design/icons-svg` | 4.4.2 | prod-registry | — | 技术性 | T-PROD |
| 166–181 | `node_modules/@ant-design/react-slick` | 1.1.2 | prod-registry | — | 技术性 | T-PROD |
| 182–198 | `node_modules/@asamuzakjp/css-color` | 5.1.11 | dev-registry | dev | 技术性 | T-DEV |
| 199–215 | `node_modules/@asamuzakjp/dom-selector` | 7.1.1 | dev-registry | dev | 技术性 | T-DEV |
| 216–225 | `node_modules/@asamuzakjp/generational-cache` | 1.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 226–232 | `node_modules/@asamuzakjp/nwsapi` | 2.3.9 | dev-registry | dev | 技术性 | T-DEV |
| 233–248 | `node_modules/@babel/code-frame` | 7.29.7 | peer-registry | dev,peer | 技术性 | T-PEER |
| 249–259 | `node_modules/@babel/helper-validator-identifier` | 7.29.7 | peer-registry | dev,peer | 技术性 | T-PEER |
| 260–268 | `node_modules/@babel/runtime` | 7.29.7 | prod-registry | — | 技术性 | T-PROD |
| 269–281 | `node_modules/@bramus/specificity` | 2.4.2 | dev-registry | dev | 技术性 | T-DEV |
| 282–301 | `node_modules/@csstools/color-helpers` | 6.0.2 | dev-registry | dev | 技术性 | T-DEV |
| 302–325 | `node_modules/@csstools/css-calc` | 3.2.1 | dev-registry | dev | 技术性 | T-DEV |
| 326–353 | `node_modules/@csstools/css-color-parser` | 4.1.7 | dev-registry | dev | 技术性 | T-DEV |
| 354–376 | `node_modules/@csstools/css-parser-algorithms` | 4.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 377–401 | `node_modules/@csstools/css-syntax-patches-for-csstree` | 1.1.5 | dev-registry | dev | 技术性 | T-DEV |
| 402–421 | `node_modules/@csstools/css-tokenizer` | 4.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 422–433 | `node_modules/@emnapi/core` | 1.10.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 434–444 | `node_modules/@emnapi/runtime` | 1.10.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 445–455 | `node_modules/@emnapi/wasi-threads` | 1.2.1 | optional-registry | dev,optional | 技术性 | T-OPT |
| 456–461 | `node_modules/@emotion/hash` | 0.8.0 | prod-registry | — | 技术性 | T-PROD |
| 462–467 | `node_modules/@emotion/unitless` | 0.7.5 | prod-registry | — | 技术性 | T-PROD |
| 468–485 | `node_modules/@exodus/bytes` | 1.15.1 | dev-registry | dev | 技术性 | T-DEV |
| 486–492 | `node_modules/@jridgewell/sourcemap-codec` | 1.5.5 | dev-registry | dev | 技术性 | T-DEV |
| 493–511 | `node_modules/@napi-rs/wasm-runtime` | 1.1.5 | optional-registry | dev,optional | 技术性 | T-OPT |
| 512–521 | `node_modules/@oxc-project/types` | 0.133.0 | dev-registry | dev | 技术性 | T-DEV |
| 522–537 | `node_modules/@playwright/test` | 1.61.0 | dev-registry | dev | 技术性 | T-DEV |
| 538–549 | `node_modules/@rc-component/async-validator` | 5.1.0 | prod-registry | — | 技术性 | T-PROD |
| 550–565 | `node_modules/@rc-component/color-picker` | 2.0.1 | prod-registry | — | 技术性 | T-PROD |
| 566–579 | `node_modules/@rc-component/context` | 1.4.0 | prod-registry | — | 技术性 | T-PROD |
| 580–591 | `node_modules/@rc-component/mini-decimal` | 1.1.3 | prod-registry | — | 技术性 | T-PROD |
| 592–609 | `node_modules/@rc-component/mutate-observer` | 1.1.0 | prod-registry | — | 技术性 | T-PROD |
| 610–627 | `node_modules/@rc-component/portal` | 1.1.2 | prod-registry | — | 技术性 | T-PROD |
| 628–643 | `node_modules/@rc-component/qrcode` | 1.1.1 | prod-registry | — | 技术性 | T-PROD |
| 644–663 | `node_modules/@rc-component/tour` | 1.15.1 | prod-registry | — | 技术性 | T-PROD |
| 664–684 | `node_modules/@rc-component/trigger` | 2.3.1 | prod-registry | — | 技术性 | T-PROD |
| 685–701 | `node_modules/@rolldown/binding-android-arm64` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 702–718 | `node_modules/@rolldown/binding-darwin-arm64` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 719–735 | `node_modules/@rolldown/binding-darwin-x64` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 736–752 | `node_modules/@rolldown/binding-freebsd-x64` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 753–769 | `node_modules/@rolldown/binding-linux-arm-gnueabihf` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 770–786 | `node_modules/@rolldown/binding-linux-arm64-gnu` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 787–803 | `node_modules/@rolldown/binding-linux-arm64-musl` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 804–820 | `node_modules/@rolldown/binding-linux-ppc64-gnu` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 821–837 | `node_modules/@rolldown/binding-linux-s390x-gnu` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 838–854 | `node_modules/@rolldown/binding-linux-x64-gnu` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 855–871 | `node_modules/@rolldown/binding-linux-x64-musl` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 872–888 | `node_modules/@rolldown/binding-openharmony-arm64` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 889–907 | `node_modules/@rolldown/binding-wasm32-wasi` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 908–924 | `node_modules/@rolldown/binding-win32-arm64-msvc` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 925–941 | `node_modules/@rolldown/binding-win32-x64-msvc` | 1.0.3 | optional-registry | dev,optional | 技术性 | T-OPT |
| 942–948 | `node_modules/@standard-schema/spec` | 1.1.0 | dev-registry | dev | 技术性 | T-DEV |
| 949–969 | `node_modules/@testing-library/dom` | 10.4.1 | peer-registry | dev,peer | 技术性 | T-PEER |
| 970–989 | `node_modules/@testing-library/jest-dom` | 6.9.1 | dev-registry | dev | 技术性 | T-DEV |
| 990–996 | `node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api` | 0.6.3 | dev-registry | dev | 技术性 | T-DEV |
| 997–1024 | `node_modules/@testing-library/react` | 16.3.2 | dev-registry | dev | 技术性 | T-DEV |
| 1025–1038 | `node_modules/@testing-library/user-event` | 14.6.1 | dev-registry | dev | 技术性 | T-DEV |
| 1039–1049 | `node_modules/@tybys/wasm-util` | 0.10.2 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1050–1057 | `node_modules/@types/aria-query` | 5.0.4 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1058–1068 | `node_modules/@types/chai` | 5.2.3 | dev-registry | dev | 技术性 | T-DEV |
| 1069–1075 | `node_modules/@types/deep-eql` | 4.0.2 | dev-registry | dev | 技术性 | T-DEV |
| 1076–1082 | `node_modules/@types/estree` | 1.0.9 | dev-registry | dev | 技术性 | T-DEV |
| 1083–1092 | `node_modules/@types/react` | 19.2.16 | dev-registry | dev | 技术性 | T-DEV |
| 1093–1102 | `node_modules/@types/react-dom` | 19.2.3 | dev-registry | dev | 技术性 | T-DEV |
| 1103–1120 | `node_modules/@vitest/expect` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1121–1147 | `node_modules/@vitest/mocker` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1148–1160 | `node_modules/@vitest/pretty-format` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1161–1174 | `node_modules/@vitest/runner` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1175–1190 | `node_modules/@vitest/snapshot` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1191–1200 | `node_modules/@vitest/spy` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1201–1215 | `node_modules/@vitest/utils` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 1216–1226 | `node_modules/ansi-regex` | 5.0.1 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1227–1240 | `node_modules/ansi-styles` | 5.2.0 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1241–1305 | `node_modules/antd` | 5.29.3 | prod-registry | — | 技术性 | T-PROD |
| 1306–1309 | `node_modules/apps-web` | link→apps/web | link | — | 技术性 | T-LINK |
| 1310–1319 | `node_modules/aria-query` | 5.3.0 | dev-registry | dev | 技术性 | T-DEV |
| 1320–1329 | `node_modules/assertion-error` | 2.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 1330–1339 | `node_modules/bidi-js` | 1.0.3 | dev-registry | dev | 技术性 | T-DEV |
| 1340–1349 | `node_modules/chai` | 6.2.2 | dev-registry | dev | 技术性 | T-DEV |
| 1350–1355 | `node_modules/classnames` | 2.5.1 | prod-registry | — | 技术性 | T-PROD |
| 1356–1361 | `node_modules/compute-scroll-into-view` | 3.1.1 | prod-registry | — | 技术性 | T-PROD |
| 1362–1368 | `node_modules/convert-source-map` | 2.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 1369–1377 | `node_modules/copy-to-clipboard` | 3.3.3 | prod-registry | — | 技术性 | T-PROD |
| 1378–1391 | `node_modules/css-tree` | 3.2.1 | dev-registry | dev | 技术性 | T-DEV |
| 1392–1398 | `node_modules/css.escape` | 1.5.1 | dev-registry | dev | 技术性 | T-DEV |
| 1399–1404 | `node_modules/csstype` | 3.2.3 | prod-registry | — | 技术性 | T-PROD |
| 1405–1418 | `node_modules/data-urls` | 7.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 1419–1424 | `node_modules/dayjs` | 1.11.21 | prod-registry | — | 技术性 | T-PROD |
| 1425–1431 | `node_modules/decimal.js` | 10.6.0 | dev-registry | dev | 技术性 | T-DEV |
| 1432–1441 | `node_modules/dequal` | 2.0.3 | dev-registry | dev | 技术性 | T-DEV |
| 1442–1451 | `node_modules/detect-libc` | 2.1.2 | dev-registry | dev | 技术性 | T-DEV |
| 1452–1459 | `node_modules/dom-accessibility-api` | 0.5.16 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1460–1472 | `node_modules/entities` | 8.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 1473–1479 | `node_modules/es-module-lexer` | 2.1.0 | dev-registry | dev | 技术性 | T-DEV |
| 1480–1489 | `node_modules/estree-walker` | 3.0.3 | dev-registry | dev | 技术性 | T-DEV |
| 1490–1499 | `node_modules/expect-type` | 1.3.0 | dev-registry | dev | 技术性 | T-DEV |
| 1500–1517 | `node_modules/fdir` | 6.5.0 | dev-registry | dev | 技术性 | T-DEV |
| 1518–1532 | `node_modules/fsevents` | 2.3.3 | optional-registry | dev,optional,hasInstallScript | 技术性 | T-OPT |
| 1533–1545 | `node_modules/html-encoding-sniffer` | 6.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 1546–1555 | `node_modules/indent-string` | 4.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 1556–1562 | `node_modules/is-potential-custom-element-name` | 1.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 1563–1570 | `node_modules/js-tokens` | 4.0.0 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1571–1611 | `node_modules/jsdom` | 29.1.1 | dev-registry | dev | 技术性 | T-DEV |
| 1612–1620 | `node_modules/json2mq` | 0.2.0 | prod-registry | — | 技术性 | T-PROD |
| 1621–1650 | `node_modules/lightningcss` | 1.32.0 | dev-registry | dev | 技术性 | T-DEV |
| 1651–1671 | `node_modules/lightningcss-android-arm64` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1672–1692 | `node_modules/lightningcss-darwin-arm64` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1693–1713 | `node_modules/lightningcss-darwin-x64` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1714–1734 | `node_modules/lightningcss-freebsd-x64` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1735–1755 | `node_modules/lightningcss-linux-arm-gnueabihf` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1756–1776 | `node_modules/lightningcss-linux-arm64-gnu` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1777–1797 | `node_modules/lightningcss-linux-arm64-musl` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1798–1818 | `node_modules/lightningcss-linux-x64-gnu` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1819–1839 | `node_modules/lightningcss-linux-x64-musl` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1840–1860 | `node_modules/lightningcss-win32-arm64-msvc` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1861–1881 | `node_modules/lightningcss-win32-x64-msvc` | 1.32.0 | optional-registry | dev,optional | 技术性 | T-OPT |
| 1882–1891 | `node_modules/lru-cache` | 11.5.1 | dev-registry | dev | 技术性 | T-DEV |
| 1892–1902 | `node_modules/lz-string` | 1.5.0 | peer-registry | dev,peer | 技术性 | T-PEER |
| 1903–1912 | `node_modules/magic-string` | 0.30.21 | dev-registry | dev | 技术性 | T-DEV |
| 1913–1919 | `node_modules/mdn-data` | 2.27.1 | dev-registry | dev | 技术性 | T-DEV |
| 1920–1929 | `node_modules/min-indent` | 1.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 1930–1948 | `node_modules/nanoid` | 3.3.18 | dev-registry | dev | 技术性 | T-DEV |
| 1949–1962 | `node_modules/obug` | 2.1.3 | dev-registry | dev | 技术性 | T-DEV |
| 1963–1975 | `node_modules/parse5` | 8.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 1976–1982 | `node_modules/pathe` | 2.0.3 | dev-registry | dev | 技术性 | T-DEV |
| 1983–1989 | `node_modules/picocolors` | 1.1.1 | dev-registry | dev | 技术性 | T-DEV |
| 1990–2002 | `node_modules/picomatch` | 4.0.4 | dev-registry | dev | 技术性 | T-DEV |
| 2003–2021 | `node_modules/playwright` | 1.61.0 | dev-registry | dev | 技术性 | T-DEV |
| 2022–2034 | `node_modules/playwright-core` | 1.61.0 | dev-registry | dev | 技术性 | T-DEV |
| 2035–2049 | `node_modules/playwright/node_modules/fsevents` | 2.3.2 | optional-registry | dev,optional,hasInstallScript | 技术性 | T-OPT |
| 2050–2078 | `node_modules/postcss` | 8.5.23 | dev-registry | dev | 技术性 | T-DEV |
| 2079–2094 | `node_modules/pretty-format` | 27.5.1 | peer-registry | dev,peer | 技术性 | T-PEER |
| 2095–2102 | `node_modules/pretty-format/node_modules/react-is` | 17.0.2 | peer-registry | dev,peer | 技术性 | T-PEER |
| 2103–2112 | `node_modules/punycode` | 2.3.1 | dev-registry | dev | 技术性 | T-DEV |
| 2113–2121 | `node_modules/qrcode.react` | 4.2.0 | prod-registry | — | 技术性 | T-PROD |
| 2122–2138 | `node_modules/rc-cascader` | 3.34.0 | prod-registry | — | 技术性 | T-PROD |
| 2139–2153 | `node_modules/rc-checkbox` | 3.5.0 | prod-registry | — | 技术性 | T-PROD |
| 2154–2169 | `node_modules/rc-collapse` | 3.9.0 | prod-registry | — | 技术性 | T-PROD |
| 2170–2186 | `node_modules/rc-dialog` | 9.6.0 | prod-registry | — | 技术性 | T-PROD |
| 2187–2203 | `node_modules/rc-drawer` | 7.3.0 | prod-registry | — | 技术性 | T-PROD |
| 2204–2219 | `node_modules/rc-dropdown` | 4.2.1 | prod-registry | — | 技术性 | T-PROD |
| 2220–2237 | `node_modules/rc-field-form` | 2.7.1 | prod-registry | — | 技术性 | T-PROD |
| 2238–2255 | `node_modules/rc-image` | 7.12.0 | prod-registry | — | 技术性 | T-PROD |
| 2256–2270 | `node_modules/rc-input` | 1.8.0 | prod-registry | — | 技术性 | T-PROD |
| 2271–2287 | `node_modules/rc-input-number` | 9.5.0 | prod-registry | — | 技术性 | T-PROD |
| 2288–2306 | `node_modules/rc-mentions` | 2.20.0 | prod-registry | — | 技术性 | T-PROD |
| 2307–2324 | `node_modules/rc-menu` | 9.16.1 | prod-registry | — | 技术性 | T-PROD |
| 2325–2339 | `node_modules/rc-motion` | 2.9.5 | prod-registry | — | 技术性 | T-PROD |
| 2340–2358 | `node_modules/rc-notification` | 5.6.4 | prod-registry | — | 技术性 | T-PROD |
| 2359–2374 | `node_modules/rc-overflow` | 1.5.0 | prod-registry | — | 技术性 | T-PROD |
| 2375–2389 | `node_modules/rc-pagination` | 5.1.0 | prod-registry | — | 技术性 | T-PROD |
| 2390–2428 | `node_modules/rc-picker` | 4.11.3 | prod-registry | — | 技术性 | T-PROD |
| 2429–2443 | `node_modules/rc-progress` | 4.0.0 | prod-registry | — | 技术性 | T-PROD |
| 2444–2461 | `node_modules/rc-rate` | 2.13.1 | prod-registry | — | 技术性 | T-PROD |
| 2462–2477 | `node_modules/rc-resize-observer` | 1.4.3 | prod-registry | — | 技术性 | T-PROD |
| 2478–2493 | `node_modules/rc-segmented` | 2.7.1 | prod-registry | — | 技术性 | T-PROD |
| 2494–2515 | `node_modules/rc-select` | 14.16.8 | prod-registry | — | 技术性 | T-PROD |
| 2516–2533 | `node_modules/rc-slider` | 11.1.9 | prod-registry | — | 技术性 | T-PROD |
| 2534–2551 | `node_modules/rc-steps` | 6.0.1 | prod-registry | — | 技术性 | T-PROD |
| 2552–2566 | `node_modules/rc-switch` | 4.1.0 | prod-registry | — | 技术性 | T-PROD |
| 2567–2587 | `node_modules/rc-table` | 7.54.0 | prod-registry | — | 技术性 | T-PROD |
| 2588–2609 | `node_modules/rc-tabs` | 15.7.0 | prod-registry | — | 技术性 | T-PROD |
| 2610–2626 | `node_modules/rc-textarea` | 1.10.2 | prod-registry | — | 技术性 | T-PROD |
| 2627–2642 | `node_modules/rc-tooltip` | 6.4.0 | prod-registry | — | 技术性 | T-PROD |
| 2643–2662 | `node_modules/rc-tree` | 5.13.1 | prod-registry | — | 技术性 | T-PROD |
| 2663–2679 | `node_modules/rc-tree-select` | 5.27.0 | prod-registry | — | 技术性 | T-PROD |
| 2680–2694 | `node_modules/rc-upload` | 4.11.0 | prod-registry | — | 技术性 | T-PROD |
| 2695–2708 | `node_modules/rc-util` | 5.44.4 | prod-registry | — | 技术性 | T-PROD |
| 2709–2727 | `node_modules/rc-virtual-list` | 3.19.2 | prod-registry | — | 技术性 | T-PROD |
| 2728–2736 | `node_modules/react` | 19.2.7 | prod-registry | — | 技术性 | T-PROD |
| 2737–2748 | `node_modules/react-dom` | 19.2.7 | prod-registry | — | 技术性 | T-PROD |
| 2749–2754 | `node_modules/react-is` | 18.3.1 | prod-registry | — | 技术性 | T-PROD |
| 2755–2768 | `node_modules/redent` | 3.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 2769–2778 | `node_modules/require-from-string` | 2.0.2 | dev-registry | dev | 技术性 | T-DEV |
| 2779–2784 | `node_modules/resize-observer-polyfill` | 1.5.1 | prod-registry | — | 技术性 | T-PROD |
| 2785–2818 | `node_modules/rolldown` | 1.0.3 | dev-registry | dev | 技术性 | T-DEV |
| 2819–2825 | `node_modules/rolldown/node_modules/@rolldown/pluginutils` | 1.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 2826–2838 | `node_modules/saxes` | 6.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 2839–2844 | `node_modules/scheduler` | 0.27.0 | prod-registry | — | 技术性 | T-PROD |
| 2845–2853 | `node_modules/scroll-into-view-if-needed` | 3.1.0 | prod-registry | — | 技术性 | T-PROD |
| 2854–2860 | `node_modules/siginfo` | 2.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 2861–2870 | `node_modules/source-map-js` | 1.2.1 | dev-registry | dev | 技术性 | T-DEV |
| 2871–2877 | `node_modules/stackback` | 0.0.2 | dev-registry | dev | 技术性 | T-DEV |
| 2878–2884 | `node_modules/std-env` | 4.1.0 | dev-registry | dev | 技术性 | T-DEV |
| 2885–2890 | `node_modules/string-convert` | 0.2.1 | prod-registry | — | 技术性 | T-PROD |
| 2891–2903 | `node_modules/strip-indent` | 3.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 2904–2909 | `node_modules/stylis` | 4.4.0 | prod-registry | — | 技术性 | T-PROD |
| 2910–2916 | `node_modules/symbol-tree` | 3.2.4 | dev-registry | dev | 技术性 | T-DEV |
| 2917–2925 | `node_modules/throttle-debounce` | 5.0.2 | prod-registry | — | 技术性 | T-PROD |
| 2926–2932 | `node_modules/tinybench` | 2.9.0 | dev-registry | dev | 技术性 | T-DEV |
| 2933–2942 | `node_modules/tinyexec` | 1.2.4 | dev-registry | dev | 技术性 | T-DEV |
| 2943–2959 | `node_modules/tinyglobby` | 0.2.17 | dev-registry | dev | 技术性 | T-DEV |
| 2960–2969 | `node_modules/tinyrainbow` | 3.1.0 | dev-registry | dev | 技术性 | T-DEV |
| 2970–2982 | `node_modules/tldts` | 7.4.3 | dev-registry | dev | 技术性 | T-DEV |
| 2983–2989 | `node_modules/tldts-core` | 7.4.3 | dev-registry | dev | 技术性 | T-DEV |
| 2990–2995 | `node_modules/toggle-selection` | 1.0.6 | prod-registry | — | 技术性 | T-PROD |
| 2996–3008 | `node_modules/tough-cookie` | 6.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 3009–3021 | `node_modules/tr46` | 6.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 3022–3029 | `node_modules/tslib` | 2.8.1 | optional-registry | dev,optional | 技术性 | T-OPT |
| 3030–3043 | `node_modules/typescript` | 5.9.3 | dev-registry | dev | 技术性 | T-DEV |
| 3044–3053 | `node_modules/undici` | 7.29.0 | dev-registry | dev | 技术性 | T-DEV |
| 3054–3131 | `node_modules/vite` | 8.0.16 | dev-registry | dev | 技术性 | T-DEV |
| 3132–3221 | `node_modules/vitest` | 4.1.9 | dev-registry | dev | 技术性 | T-DEV |
| 3222–3234 | `node_modules/w3c-xmlserializer` | 5.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 3235–3244 | `node_modules/webidl-conversions` | 8.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 3245–3254 | `node_modules/whatwg-mimetype` | 5.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 3255–3269 | `node_modules/whatwg-url` | 16.0.1 | dev-registry | dev | 技术性 | T-DEV |
| 3270–3286 | `node_modules/why-is-node-running` | 2.3.0 | dev-registry | dev | 技术性 | T-DEV |
| 3287–3296 | `node_modules/xml-name-validator` | 5.0.0 | dev-registry | dev | 技术性 | T-DEV |
| 3297–3303 | `node_modules/xmlchars` | 2.2.0 | dev-registry | dev | 技术性 | T-DEV |
| 3304–3319 | `node_modules/yaml` | 2.9.0 | dev-registry | dev | 技术性 | T-DEV |

顶层键单独登记：`name`/`version`/`lockfileVersion`/`requires`/`packages` → T-HDR；`documentVersion` → T-DOCVER。均技术性、无业务发明、无工程缺陷、本次不改。

## 7. 命令、exit、输出边界（本轮实跑）

所有命令在 `/home/james/complex-management`；未 source 全量 env；未改源码。

| # | 命令 | exit | 关键输出 | 边界 |
|---|---|---|---|---|
| C1 | `python3` / `openssl dgst -sha256` / `sha256sum` 独立哈希 `criteria-lock.md` | 0 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节） | 三工具同值；**正文不再重复独占字段行** |
| C2 | `wc -l package-lock.json` + SHA256 | 0 | 3321 行；`d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef` | 与派发一致 |
| C3 | `git rev-parse HEAD origin/main` | 0 | 二者 `242f487233d028173cb9198eecd079f5d877405c` | 正本 main |
| C4 | `npm help package-lock-json` | 0 | File Format：name/version/lockfileVersion/packages；link 无其它字段；integrity 为 SRI | 类型 3 |
| C5 | JSON 解析 + 分段覆盖 + SRI 长度 + 边解析（python，stdout `/tmp/m004-lock-analysis.txt`） | 0 | 220 包；SRI 217 ok；缺 integrity/resolved 0；坏 SRI 0；http resolved 0；依赖边 426 全找到；required peer 缺失 0；legacy 标记 0 | 非下载 |
| C6 | Arborist `loadVirtual` + Shrinkwrap.load + semver（node，`/tmp/m004-arborist.txt`） | 0 | inventory 220；edges 582；MISSING_REQUIRED []；INVALID_EDGES []；RANGE_FAIL []；ENGINE_FAIL []；URL_MISMATCHES []；toJSON 保留 documentVersion V0.02；锁文件 mtime/inode 不变 | 未 reify、未写盘 |
| C7 | `npm ls --package-lock-only --offline --all --json` | 0 | name/version 匹配；dep 节点 582；problems/error 空；锁未写 | 不是安装；`--offline` 不证明 registry |
| C8 | `node tools/verify-no-legacy-api-prototype.mjs` | 0 | `NO_LEGACY_API_PROTOTYPE_PASS` | 文本扫描，非构建 |
| C9 | `node -v` / `npm -v` / `getconf GNU_LIBC_VERSION` | 0 | v24.20.0 / 11.19.0 / glibc 2.39 | 环境 |

明确未跑：`npm ci`、`npm install`、`npm audit`、`npx`、`npm exec`、Vite build、Playwright、`verify:predeploy`。

## 8. 新 findings

**空集。** 无 GR 临时编号。无工程缺陷。无添加项。无 HTML 自身问题需上报暂停。

不把下列观察写成缺陷或新闸：磁盘 optional 未装、hidden lock 并存、根 package.json `documentVersion` V0.01 与锁 V0.02 为**不同文件**自身版本、`packageManager` 不在锁内、CI Node 22 写在他模块工作流、未做 tarball 字节校对、未做 audit。

## 9. 本席结论

- 本模块全部 3321 行有覆盖记录。
- 映射 18 项 GEN 已逐项核对。
- 三线均执行并落证据。
- 声称均落到类型 3/4（及 HTML 无上位的类型 1 反证）。
- 无未答 questions。无未定稿缺陷。
- **本席 B1：PASS**
- **是否 HTML 问题：否**
- 不是模块生命周期 PASS（须 B5 双席 + 四值）。不是有条件通过。
- 历史 B3=1 / 方案修复=2 / 交付失败=0 保持。不继承旧 322 PASS。

B1 结束。不修复、不提问、不改锁。

## 当前 M004 整批 B2 独立复核（Grok）

版本：V0.05（本文件既有 V0.01–V0.04 全部字节保留，本次完整 B2 归并结论追加；自身版本 V0.04→V0.05）

席位：Grok（唯一外审）。节点：160-audit-prompt.md V0.17 阶段 B / M004 / B2（整批归并独立复核，不是 B1 重跑）。计划 471 模块；本模块仅 `package-lock.json`（3321 行 / 220 条目 / 18 GEN / OVERSIZE-SINGLE-FILE）。ledger 现值：M004 `IN_PROGRESS`，历史 B3 累计 1、方案修复 2、失败重跑 0；M001—M003 保持 PASS，本席不重审它们。本席不修依赖/源码/HTML/计划/锁/ledger/findings/questions，不派子代理，不用 MCP_Codex_160 / MCP_Cloud_160，不提交、不推送、不部署、不新 worktree、不另建 b2 文件。本追加段不写独占 `CRITERIA-LOCK-SHA256:` 字段行。

**本席 B2：PASS。不是模块 PASS。不是有条件通过。**

审核同一 findings 候选 V0.93：`docs/audit-2026-09/findings.md` 316366 字节，SHA256 `2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78`。只审该文件最上方当前 M004 B2 候选及相关旧 M004 历史 ID；其它模块段落不变，不重新审 findings 本身。

### 0. 技能（本节点实核；V0.17 §6 收窄：只审不修、不额外问人、不加门槛/状态/产物）

| 技能 | 本节点用法 |
|---|---|
| `receiving-code-review` | 先对照两席本次 B1、现锁/配置/公告/audit 输出再接受或反驳候选；禁止因根席 PASS 而表演性同意；本体「逐条 IMPLEMENT / 条目不清就问人」作废 |
| `grilling` / `grill-me` | 差集/分类/007 边界自问自答；不等操作员、不派子代理（本体提问条款作废） |
| `systematic-debugging` / `diagnosing-bugs` | 只借「读错误→复现→追数据流」核 audit exit1 与公告通道；不改代码、不加探针 |
| `codebase-design` | 锁接口=npm ci 输入；删除测试：删锁则确定安装入口做不到；不新造缝 |
| `verification-before-completion` | 任何 PASS/FAIL 前用本追加内本次命令输出 |
| `ponytail-review` | 只查重复永久 ID / 凭 CVE 标题自动入队的膨胀；不修 |

缺口，不得冒充已调用：`batch-grill-me` 本机技能目录未找到。`tdd` / `resolving-merge-conflicts` / `ponytail` 本体本节点无对应动作。控制已划掉的技能未调用。grilling 的「向人发问」与 receiving-code-review 的 IMPLEMENT 均按 §6 作废。

未安装依赖、未 `npm audit fix`、未改任何源文件。对照路径仅用于核实锁的直接消费（root/`apps/web` 的 `package.json`、`apps/web/vite.config.ts`、`apps/web/vitest.config.ts`）；不宣布这些文件所属其它模块已审完。

### 1. 两份当前 raw 输入（完整读取后实算）

均在写入本追加前独立计算。根随后的 self V0.06 是 B2 补核与根席票，**不是**独立 B1 证据。

| 对象 | 派发声称 | 本席实算 |
|---|---|---|
| 候选 `docs/audit-2026-09/findings.md` | V0.93，316366 字节，SHA256=`2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78` | `sha256sum` 同值；版本行 `V0.93`；最上方唯一当前 M004 队列表七行 001—007 |
| 输入一 根独立 B1 | `modules/M004-b1-self-raw.md` 当时 V0.05，L540—1129，完整文件 132363 字节、SHA256=`079da8b31892d00232e26f4097fac609b270df926149d4fc15fd872f20e66a4f` | 当前全文 141623 字节 / SHA256=`460c7f02e679e5631cf30d410af8e6b3d2f90f3ab5959c391bf3235a3ad068f3`。**前 132363 字节** SHA256=`079da8b31892d00232e26f4097fac609b270df926149d4fc15fd872f20e66a4f`，与独立 B1 声称相等。L540 为本次 B1 独占锁行，L1129 为 B1 结论空行。L1132 起 V0.06 为 B2 补核，不采信为独立 B1 |
| 输入二 本席独立 B1 | `modules/M004-b1-grok-raw.md` V0.04，L873—1536，完整文件 145659 字节、SHA256=`d2707c6c5aa9e4a6e585f079f88aa1f6a0615f2f196525b514c79b6db4160607` | 本追加前全文同值。L873 为本席本次 B1 独占锁行，L1536 为 B1 结束句。旧 322 前缀 V0.01–V0.03 完整保留，不继承旧 PASS |

两席本次 B1 均为完整三线 PASS，当时 finding 空集。派发顺序：先本席结束、根独立保存后才读本席全文。B1 后四值已相等（实算 / sidecar / ledger 顶行 / 本席当时最后独占字段行均为 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`，锁 V0.11 / COUNT 1）。本 B2 段不再写独占字段行。

源文件：`package-lock.json` 3321 行 / 114258 字节 / 220 `packages` / `documentVersion` V0.02 / SHA256=`d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef`。HEAD=`origin/main`=`242f487233d028173cb9198eecd079f5d877405c`。冻结映射：`criteria-lock.md` L6383 `M004 → [GEN-001 … GEN-018]`，无 HTML 派生 ID。

### 2. 全部差集（本次临时号 → 永久 ID）

本次临时号：

- 本席 V0.04 B1 §8：finding **空集**。明确未跑 `npm audit`。未把磁盘 optional 未装、hidden lock、未下载 tarball、未做 audit 写成缺陷。
- 根独立 B1 V0.05（前 132363 字节）：finding **空集**。同样记录当时未跑 `npm audit`。
- 根 B2 V0.06 / 候选 CX-B2-01：新增 **一条** 观察，两包同一 GHSA，归 **M004-007**。

| 本次信号 | 候选归并 | 漏并？ | 改写本席 finding？ |
|---|---|---|---|
| 本席 B1 空集 | 无新 GR 行 | 否 | 否。未把空集改写成缺陷，也未把本席原「未跑 audit」回写成当时已跑 |
| 根 B1 空集 | 无新 CX 行进入 001—006 的当前缺陷栏 | 否 | 否 |
| 根 B2 CX-B2-01（audit 一条公告，两包） | 新正式 **M004-007**，不拆两项 | 否 | 否。本席 B1 没有公告查询，**不把原 B1 PASS 冒称已同意 007** |
| 历史 nanoid / postcss / undici | 继续 **M004-001 / 002 / 003** | 否 | 否。旧工程缺陷与已修记录保留；当前缺陷/队列为否，不改写为「从未存在」 |
| 元数据 / 220 条图 / HTTPS+SRI | 继续 **M004-004 / 005 / 006** | 否 | 否。本席 T-HDR/T-DOCVER/T-WS/T-LINK、全 220 T-PROD/DEV/OPT/PEER、T 完整性登记分别落入三 ID，不复制 220 个新 ID |

无添加项。无铁律 4 新 HTML 申请。无未答 questions。旧 322 整体源 SHA `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc` 与当前 `d3676e89…` 不同，**不继承旧整体 PASS**。

### 3. 依序分类、工程缺陷、队列（冲突 → 细化 → 技术性 → 添加）

唯一当前 M004 表七行，本席逐项独立判断。三份 HTML 无 npm 锁上位锚点，整文件及各 ID 停不住冲突/细化，无业务发明，故均为 **技术性**。

#### M004-001

- 当前来源：旧 CX-01；本次两席完整锁/版本核对；根 B2 公告查询未再列入
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现锁：`package-lock.json` L1930—1948，`node_modules/nanoid` **3.3.18**。历史缺陷（3.3.12 无限循环）及已修出队记录保留在 findings 旧段。本席本次 `npm audit --package-lock-only --ignore-scripts --json` 结果不含 nanoid。不称所有未来公告为空
- 判定：同意队列否

#### M004-002

- 当前来源：旧 CX-02；本次两席完整锁/版本核对；根 B2 公告查询未再列入
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现锁：L2050—2078，`node_modules/postcss` **8.5.23**，内部 `dependencies.nanoid` **^3.3.16**。历史 map 路径缺陷及已修记录保留。当前 audit 不含 postcss
- 判定：同意队列否

#### M004-003

- 当前来源：旧 CX-03；本次两席完整锁/版本核对；根 B2 公告查询未再列入
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现锁：L3044—3053，`node_modules/undici` **7.29.0**。历史缓存头缺陷及已修记录保留。当前 audit 不含 undici
- 判定：同意队列否

#### M004-004

- 当前来源：根顶层/根/web/link 表；本席 T-HDR / T-DOCVER / T-WS / T-LINK
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现核：顶层 `name`/`version`/`lockfileVersion:3`/`documentVersion:V0.02`/`requires`/`packages`；唯一 link `node_modules/apps-web` → `apps/web`；根与 `apps/web` 记录存在。不改应用 SemVer、锁协议或文件自身版本
- 判定：同意队列否

#### M004-005

- 当前来源：根 220 条 / 582 边 / 116 engines / 28 平台约束；本席全 220 条 T-PROD/DEV/OPT/PEER
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现核：`packages` 220；optional 34，其中带 `os`/`cpu` 28、不带 6（`@emnapi/core|runtime|wasi-threads`、`@napi-rs/wasm-runtime`、`@tybys/wasm-util`、`tslib`）；含 `engines` 记录 116。根 582 边与本席普通 426 / 含其它字段 438 / 含 peer 582 为不同统计口径，不是冲突。可选缺失、嵌套同版属于有效依赖图。不为去重重排锁
- 判定：同意队列否

#### M004-006

- 当前来源：两席 217 条 HTTPS/SRI；根 ssri 错 payload 反例
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 现核：217 条 `resolved` 为 HTTPS、217 条 `integrity` 为 sha512。本次 B2 **未下载**远端 tarball，不借旧下载记录称本次已验证全部供应链或无漏洞
- 判定：同意队列否

#### M004-007

见下一节。本席独立结论：分类 **技术性**；当前工程缺陷 **否**；修复队列 **否**。不是跟随根席 PASS。

### 4. M004-007 事实、通道与铁律 6 边界

#### 4.1 本席独立复跑（类型 4）

命令：`npm audit --package-lock-only --ignore-scripts --json`（工作目录 `/home/james/complex-management`）。

- 真实 **exit 1**
- `metadata.vulnerabilities`：info 0 / low 0 / **moderate 2** / high 0 / critical 0 / total 2
- 仅 `@vitest/mocker` 与 `vitest` 两条；via 均为同一 URL `https://github.com/advisories/GHSA-82fw-gwwq-j7x9`；标题同一；range `>=2.1.0 <4.1.11`
- nanoid / postcss / undici **未列入**本次结果
- 未执行 `npm audit fix`。audit 前后三份 SHA 仍为：锁 `d3676e89…`、根清单 `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f`、web 清单 `b8017d9782b4c00eac1c992204dad2b009e23db7c4c804737bdf8fcbc90b8f1f`

两包同一 GHSA，归 **一个** 正式 ID，不分两项。不能把版本命中否定：现锁 `@vitest/mocker` L1121—1147 **4.1.9**，`vitest` L3132—3221 **4.1.9**，均在受影响范围。修补版公告为 4.1.11。

#### 4.2 上游公告（类型 3，本席直接读取）

主来源：`https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9`（本席 `curl` HTML exit 0；GitHub API `https://api.github.com/advisories/GHSA-82fw-gwwq-j7x9` exit 0）。GHSA-82fw-gwwq-j7x9；CVE-2026-84373；severity medium；CVSS 5.9 `AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N`。

公告原文要点（不另造判据）：

- 缺陷在 `@vitest/mocker` 对 redirect mock 目标路径未按 dev server 文件服务 allowlist 校验；`load` 钩子对 `mock.type === 'redirect'` 执行 `readFile(mock.redirect)`。
- **无认证可利用**仅通过公开导出的 `mockerPlugin` / 独立 `interceptorPlugin`（给第三方 dev server 用），它们把处理函数挂到 Vite 未做 token 校验的 HMR WebSocket。
- Vitest **自身 browser 模式**走带 per-run token 的 RPC，默认不是远程无认证可达。
- 这是 **development-server** 问题。默认绑 localhost；暴露到网络需要 `server.host` / `0.0.0.0` 等。host 暴露是可达性前提，不是「已经注册了公开 mock 插件」。
- 受影响：`@vitest/mocker` >= 2.1.0 至 4.1.x；**Fixed: 4.1.11**。4.1.9 命中属实。

公告只解释该软件行为。本席不借它新增业务要求、门禁、判据、技能或文件。

#### 4.3 当前项目接线（类型 4，只读对照配置）

已读全文：

- `apps/web/vite.config.ts`：`plugins: [react()]` only；无 mocker/interceptor/browser。
- `apps/web/vitest.config.ts`：`plugins: [react()]`；`test.environment: "jsdom"`；无 `browser` 配置。
- `apps/web/package.json`：`test:unit`=`vitest run`；`test:unit:watch`=`vitest`；`dev`/`preview` 为 `vite --host 0.0.0.0 --port 5173`（及 preview 同 host）。
- 精确检索：`rg -n --glob '!node_modules/**' --glob '!package-lock.json' '@vitest/(mocker|browser)|mockerPlugin|interceptorPlugin' apps/web` **exit 1、无匹配**。
- 锁内无 `@vitest/browser*` 记录。vitest 的 `@vitest/browser-playwright|preview|webdriverio` 为 optional peer，当前树未装。

已安装副本（只读，未改、未升级）进一步核对默认路径：

- `node_modules/vitest` **4.1.9** 源码检索 `mockerPlugin` / `interceptorPlugin` / `vitest:interceptor:register` **0 处**。
- vitest 实际从 `@vitest/mocker/node` 引入的是 `hoistMocksPlugin` 与 `automockPlugin`，不是公开 `mockerPlugin`/`interceptorPlugin`。
- `@vitest/mocker/dist/node.js` 确有 `interceptorPlugin`：`configureServer` 注册 `server.ws.on("vitest:interceptor:register", …)`，redirect 分支 `readFile(mock.redirect)`，与公告一致。该函数是公开导出，**当前 vite/vitest 配置未调用它**。

`dev` 的 host `0.0.0.0` ≠ 注册 `mockerPlugin`。`jsdom` ≠ Vitest browser 模式。`vitest run` / 默认 watch 走 jsdom 单元测试，不是公告的无认证 HMR 注册通道。

#### 4.4 GEN-004 / 010 / 013 与铁律 6（为何不入队）

grilling 自问，本席自答：

❓ Q1 仅凭 CVE 标题或 npm 两个 moderate 包计数，是否必须把当前工程缺陷/队列打成「是」？
➡️ 否。冻结 GEN 要求可观察引入与首次可信使用；V0.17 铁律 6 要求「第一次正常跑就会撞上」才修。两包计数是同一公告的依赖闭包，不是两个独立缺陷。

❓ Q2 001—003 曾以「锁携带已知库缺陷 + 同主版修补」入队，007 是否必须同样入队？
➡️ 否，口径不同。001—003 的库级反例在调用该包**自身公开 API** 时即可复现，不需要项目再注册一条对外通道。007 的 sink 按公告与当前 4.1.9 源码，需公开插件注册（或 browser 模式那条已鉴权 RPC）。当前配置与 vitest 默认 jsdom 路径都未注册该通道。不能把「包在树里」自动等同「通道已开」。

❓ Q3 GEN-010（路径穿越）是否已被当前配置引入？
➡️ 依赖版本命中属实，公告描述的穿越 sink 存在于 `@vitest/mocker` 公开插件实现中。当前可观察入口（`vitest run` / jsdom、`vite --host 0.0.0.0` 且 plugins 仅 react）**没有**把该 sink 接到首次正常跑的输入面。GEN-010 不把「树里有受影响版本」单独升级成必须立刻改锁。

❓ Q4 GEN-004（信任边界）？
➡️ 缺校验发生在未接线的 interceptor 注册路径。本项目当前配置未把该 WebSocket 事件当成输入面。不在 B2 发明新校验门禁。

❓ Q5 GEN-013（依赖须可复现、失败显式）？
➡️ 锁仍可被原生解析；`npm audit` **非 0** 是显式告警，必须保留。audit 非 0 ≠ 安装/构建入口在首次正常跑失败，也不等于当前工程缺陷成立。不把 audit 当新强制门禁。

❓ Q6 铁律 6：这条路径第一次正常跑就会撞上吗？
➡️ 不会。要撞上无认证读取，至少还要：把 `mockerPlugin`/`interceptorPlugin` 挂进实际 Vite server，或改走 Vitest browser 模式，再加上可达的 HMR/RPC 与构造好的 redirect 注册。这是「两三件不巧同时发生」，按铁律 6 **不修**，只登记。host `0.0.0.0` 只满足公告里的可达性前提之一，单独不够。

❓ Q7 应否升级到 4.1.11？
➡️ 本席认为 **不应**在本批入队升级。支持入队的可观察事实不足：当前入口未启用公告注册通道。凭标题/两包计数自动排队会违反铁律 6，并扩大 vitest 闭包（不「偷偷升级整套」）。将来若接线改动，不在本次批准范围。

本席因此 **认同仅登记**：007 技术性 / 当前工程缺陷否 / 队列否。明确保留：

- `npm audit --package-lock-only --ignore-scripts --json` **exit 1**
- moderate **2** / high 0 / critical 0
- 4.1.9 **命中** GHSA-82fw-gwwq-j7x9，修补在 4.1.11

**不写**：audit 全绿；公告误报；该 sink 永远不可触发。

ponytail：不把 007 拆成两个 ID，不因「看起来专业」升级整套 vitest。`net: -0 lines possible.` Lean already. Ship.（仅针对本批队列膨胀，不是声称锁无告警。）

### 5. 遗漏 / 分歧 / 人闸

- **遗漏**：无。七个 ID 均复核；001—006 历史与当前版本/队列均核对；007 为 B2 差集唯一新增。
- **分歧**：无。本席独立同意候选七行的分类/当前缺陷/队列。本席 B1 空集与 007 并存：007 是 B1 之后按历史修复核对补跑 audit 的新观察，不是改写 B1。
- **人闸**：无添加项，无 HTML 自身问题，无未答 questions。不向人提问。
- 全局 html-ledger-sync / entry-anchors 红灯仍按既定归属保留，不是本模块锁缺陷，本席不宣布其它模块审完。

### 6. 整批结论与下一步

**B2 PASS。** 七项当前修复队列全部为否。按 V0.17 §4：跳过 B3/B4（B3 次数保持 1，不增加），**必须完整 B5-r1**（跳过 B3 时文件名仍用历史 B3=1 的 r1 口径，由调度按既有命名执行；本席不改 ledger）。**现在不得标模块 PASS。**

未双席定稿前不得实施任何升级或改锁。本席不代改 findings 或 self。任一 FAIL 则整批继续归并、不定稿、不修，原票保留。

B2 结束。不修复。
