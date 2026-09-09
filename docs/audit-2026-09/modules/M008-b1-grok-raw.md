CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M008 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M008 — 治理门禁与 CI 基线／检查及生成工具／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` FILES 17 文件，冻结有效内容行 1530
- 冻结判据：GEN-001–018 + PGM-036，共 19 条
- 结论：**FAIL**
- 工程缺陷成立、须进 B2 的临时编号：GR-M008-001、GR-M008-002
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装主仓 hook、未派子代理、未调 MCP

## 0. 技能自核（V0.17 §6）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问 |
| grill-me | 同上套件 `grill-me/SKILL.md` | 内部 sharpen，不写文件、不问事实 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 模块/接口/深度/删除测试 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环再下结论 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改生产代码 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，但 V0.17 明确不用，且会改 CONTEXT.md）

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；映射行 `M008 → [GEN-001 … GEN-018, PGM-036]` 与开工口径一致
- `plan/modules.md` M008 FILES 17 条与用户清单逐字相同
- 17 文件 `splitlines`/`\n` 计数之和 = **1530**，与冻结有效内容行一致

HEAD：`main` = `242f487233d028173cb9198eecd079f5d877405c` = `origin/main` merge-base。本席未动主索引（`GIT_INDEX_FILE` 未设置）。

## 2. 17 文件覆盖表（全文已读；版本/行数/指纹为当前实算）

| # | 路径 | 声称版本 | 字节 | 行（\\n） | SHA256 |
|---|---|---|---:|---:|---|
| 1 | tools/html-param-tier-baseline.json | 无版本头 | 596 | 10 | 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8 |
| 2 | tools/refresh-entry-doc-baseline.mjs | 无文件版本头 | 7209 | 122 | 76744e9a6285e6683beec1e2f7ec30105cb53cf878b6a5ec7d29ae6932647d8f |
| 3 | tools/verify-delivery-gates.mjs | V0.02 | 15949 | 325 | d75d32a0b5586e35e66e758ae95ff44e72dbb030eef2e8de1188b4878b399cac |
| 4 | tools/verify-entry-anchors.mjs | V0.02 | 16185 | 275 | 89a0d40c03b6d8c659575f432e84e0fc3df7cffdf6d1704916dddc9e9994ec2c |
| 5 | tools/verify-html-ledger-sync.mjs | V0.01 | 3804 | 68 | f325b4ccd9a4948b1eb7cc196fed4b6767c2a68d10f84005bf69943ddcb21944 |
| 6 | tools/verify-html-param-tier.mjs | V0.01 | 10942 | 198 | f0010963d1d004f124da724cd52f990704b7b6466735fccd92edd653c67fb43e |
| 7 | tools/gate-hardening-tests/99-predeploy-final.log | 无 | 7027 | 138 | 61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421 |
| 8 | tools/gate-hardening-tests/A-worktree-uncommitted.log | 无 | 5776 | 42 | 14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4 |
| 9 | tools/gate-hardening-tests/B-committed.log | 无 | 1692 | 17 | 3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339 |
| 10 | tools/gate-hardening-tests/C-no-baseline.log | 无 | 2462 | 39 | 5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67 |
| 11 | tools/gate-hardening-tests/D-baseline-legacy-pass.log | 无 | 1100 | 10 | 7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164 |
| 12 | tools/gate-hardening-tests/E-ignore-marker.log | 无 | 803 | 5 | 0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633 |
| 13 | tools/gate-hardening-tests/F-ci-baseline-evidence.log | 无 | 926 | 7 | 5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f |
| 14 | tools/gate-hardening-tests/G-merge-pr-guards.log | 无 | 1108 | 24 | 59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae |
| 15 | tools/gate-hardening-tests/I1-baseline-ratchet.log | 无 | 1055 | 13 | fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483 |
| 16 | tools/gate-hardening-tests/I2-ignore-granularity.log | 无 | 4264 | 21 | 75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b |
| 17 | tools/gate-hardening-tests/m001-regression.mjs | V0.05 | 11967 | 216 | edb313704c4efbfae3946a9898e2c94771ed5a7990feed7bcccecc959415889b |

10 份 `.log` **只作历史证据**，不冒称本席当前实跑。路径内可见当时 worktree（如 `99-predeploy-final.log` 写 `/home/james/wt-gates/`）、当时基线 6 条存量句、当时裸豁免放行。现行代码与 `m001-regression.mjs` 已改裸豁免为阻断；现行 `html-param-tier-baseline.json` 的 `entries` 为空数组。

同目录未跟踪文件 `tools/gate-hardening-tests/m007-regression.mjs` **不在本模块 FILES**，不扩审。

## 3. 用户 B/C 原意（不误报、不延伸）

操作员已确认 B/C 是业务配置原意。本席独立对照现行 BSM 原文，不采信其它模块 raw：

- B：`docs/business-system-map-for-operations-v0.1.html:1020`「来源枚举项为业务参数，按项目可配。」
- C：同文件 `:1068`「现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）」

本模块 TIER 正则含 `(?<!不)可配` 与 `业务参数`。对上述两行 `TIER=True`，不会当未标层级句。**不判冲突。** 该授权只覆盖这两处业务配置句，**不延伸到 CLI 的 PASS/FAIL/DRIFT 等内部标识符**，也不当作 CI 特批。

## 4. PGM-036 原文与适用

锁定义来源 `docs/platform-governance-map-for-operations-v0.1.html:611`，HTML 原文：

> 设备管理：谁领了什么设备，设备能不能用
> 设备包括工作手机、平板、一体机、扫码设备、综合体入场三道环节设备（安检环节的 X 光设备、年龄核验环节的人脸扫描设备、检票环节的检票闸机）和其他现场设备。设备要绑定员工、班次、点位和状态。

本 17 文件是 HTML/CI 治理门禁与回归，不建设备台账、领用、点位绑定或闸机/人脸可用性。无设备业务枚举发明，与 PGM-036 无明文冲突。**判据适用结论：不适用（无实现面）。** 映射因旧「治理门禁」文件继承而存在，不能用本模块票替代设备域后续模块。

## 5. 第一性原理（逐文件）

删除测试：删掉这些工具，三份 HTML 的解释权声明仍写在 HTML 里，但「改 HTML 必须同批同步台账 / 带数值句必须标层级 / 引用不得用行号」没有机械闸，历史已两次靠人记性失败（脚本头注释）。答得上的是治理保护，不是 PGM-036 设备功能。2.1：技术性（CI 脚本，提示词明文「不算业务发明」），上位若要指 HTML 只能指解释权声明，该锚点不在本模块 19 条锁内，故不升为细化项去改 HTML。

| 文件 | 为何存在 | 声称是否真执行 | 未证前提 |
|---|---|---|---|
| html-param-tier-baseline.json | 棘轮只缩不增的存量清单 | 现行 `entries: []`。三份 HTML 用脚本同款 scan 全量：未标层级 0、裸豁免 0。空基线与现行 HTML 一致 | 空基线不等于「以后不用扫」；未改 HTML 时门禁跳过扫描（设计如此） |
| refresh-entry-doc-baseline.mjs | 入口文档/README 数字快照回填 | `--check` 对 README 真红；隔离写回填真改 README 与 L26。但 L126 现行「事实基线」句正则 0 命中，写完 `--check` 仍绿（见 GR-M008-001） | 正则仍匹配已改写的 00-entry 句——**已证伪** |
| verify-delivery-gates.mjs | 文档标记 + workflow 镜像 + HTML 门禁接入 | `node tools/verify-delivery-gates.mjs` EXIT 0，`DELIVERY_GATES_DOCS_PASS`；回归「一致的 CI 定义/镜像分叉/缺 fetch-depth/缺 HTML 门禁」可红 | `requiredDeliveryCommands` 用 `includes` 子串，不是精确集合 |
| verify-entry-anchors.mjs | 条目名/CSV/账本/矩阵/镜像列逐字 | 现行仓 EXIT 1、6 处不合格，断言能失败。隔离 CSV 引号/列数/BOM 回归 12 条全过 | 6 红来自 BSM 工作区改动 vs 注册表/账本（父链 CSV，不扩修） |
| verify-html-ledger-sync.mjs | 改 HTML 必须同批出现台账文件 | 脏 HTML 无台账 EXIT 1；补台账 EXIT 0；缺基线 EXIT 1 | 只检文件是否出现在 diff，不检台账是否写了对应口径 |
| verify-html-param-tier.mjs | 数值句标层级 + 基线棘轮 | 假句「iso 30 天」EXIT 1；法定/业务层级回归绿；空豁免现码红（与旧 E-log 相反） | `git()` 把 argv/env 拼进 shell（见 GR-M008-002） |
| 10× .log | 当时门禁升级批取证 | 不执行。内容与现行代码/基线条数部分相反 | 若当当前绿证 → 违反 GEN-016；本席不当 |
| m001-regression.mjs | 门禁/CSV/CI/merge-pr 可失败回归 | 本席 `node tools/gate-hardening-tests/m001-regression.mjs` EXIT 0，49 条 PASS + `M001_GOVERNANCE_REGRESSION_PASS`，全程临时目录 | 未挂 package.json；不覆盖 refresh 与检查⑦ |

## 6. 顺从式（19 条逐 ID）

| ID | 覆盖结论 | 2.1 | 工程缺陷 |
|---|---|---|---|
| GEN-001 | 17 文件均有用途。`requiredDeliveryCommands` 50 项/44 唯一，5 个名字重复（includes 幂等）。重复不改判定 | 技术性观察 | 否（删重复后检查语义不变） |
| GEN-002 | 门禁主路径/错误路径已实跑。refresh 声称回填「当前事实基线」却漏 L126，且回填后 `--check` 伪绿 | 技术性 | **是 → GR-M008-001** |
| GEN-003 | `m001-regression.mjs` 有可失败断言并本席实跑。不覆盖 refresh 与检查⑦；检查⑦被现行仓 6 红中的镜像列失配实际打红 | 技术性观察 | 否（行为已由类型 4 打红，不把测试文件归属当遗漏） |
| GEN-004 | HTML 路径写死。基线 ref 来自 argv/`HTML_GATES_BASE`，未当不信任输入校验即进 shell | 技术性 | **是 → GR-M008-002**（与 GEN-010 同条） |
| GEN-005 | 无主体/对象授权面，本地/CI 工具 | 不适用 | 否 |
| GEN-006 | 无库表金额。台账闸是同 diff 出现文件，不是内容原子 | 技术性观察 | 否（与脚本自称的文件级同步一致） |
| GEN-007 | 门禁非业务重试账。merge-pr 三态（stable/watch/late）回归覆盖等待期间 tip 变化 | 技术性 | 否 |
| GEN-008 | 两份 workflow 字节相同（`cmp` 0）；`verify:predeploy` 含三 HTML 闸；package.json `verify:delivery-gates-docs` 精确等于本脚本 | 技术性 | 否 |
| GEN-009 | 缺基线有设计 FAIL 文案。注入后 `execSync` 抛未捕获 Error，不是同一文案 | 技术性 | 并入 GR-M008-002 |
| GEN-010 | `git(\`merge-base ${base} HEAD\`)` 走 `execSync` 字符串/shell。隔离探针 `origin/main; echo INJECTED_LEDGER` 使 echo 侧命令被执行 | 技术性 | **是 → GR-M008-002** |
| GEN-011 | 失败打印范围/文件/数值。旧 log 不应当当前证据 | 技术性 | 否 |
| GEN-012 | docs 树 529 md、实跑秒级。回归本地 clone，本席 2.6s 内结束 | 技术性 | 否 |
| GEN-013 | `yaml` 在 package.json devDependencies。缺基线红。回归未进 npm script，真门禁已在 `verify:predeploy` | 技术性观察 | 否 |
| GEN-014 | 无迁移文件 | 不适用 | 否 |
| GEN-015 | 无十一类终端 UI。CLI 中英混排属内部标识，不把 B/C 当文案特批，也不把 CLI 当用户界面冲突 | 技术性观察 | 否 |
| GEN-016 | 旧 log 不当当前。refresh `--check` 在 L126 仍写 V385/无 surefire 时可以 PASS | 技术性 | **是 → GR-M008-001** |
| GEN-017 | 本批无 HTML 冲突项、无添加项。工具本身技术性 | — | 否 |
| GEN-018 | 结论落到 HTML 原文、隔离/现行命令输出 | — | 否 |
| PGM-036 | 无设备实现面 | 不适用 | 否 |

## 7. 对抗式最小反例（本席自构，未问操作员）

1. 未提交 HTML 无台账 → ledger-sync EXIT 1（隔离 TMP 与现行脏工作区均红）。
2. 同改加台账任意一行 → ledger-sync EXIT 0（文件级，内容不必描述口径）。
3. 基线外「30 天」无层级 → param-tier EXIT 1。
4. 「（法定值，不可配）」「（业务参数，按项目可配）」→ EXIT 0（回归）。
5. 「该天数不可配」无「法定值」→ EXIT 1（否定词不够）。
6. 裸 `param-tier:ignore` → 现行 EXIT 1（回归「空豁免」）；**旧 E-log EXIT 0 已过时**。
7. 部分豁免只列 30 天留下 3 次 → EXIT 1。
8. 本地基线多 key → EXIT 1。
9. `HTML_GATES_BASE=absent-ref` → EXIT 1。
10. `origin/main; echo INJECTED_LEDGER` → echo 被 shell 执行，随后 `git diff` 抛错（GR-M008-002）。
11. CSV 列数/未闭合引号 → EXIT 1；BOM/CRLF/跨行字段 → EXIT 0。
12. CI 镜像改 fetch-depth 1 / 删 HTML_GATES_BASE / 不跑 predeploy → delivery-gates EXIT 1。
13. merge-pr stable 0；watch/late 1（不碰真 PR）。
14. 隔离 refresh 写完 `--check` PASS，同时 L126 仍 V385/无 surefire（GR-M008-001）。

B/C 两句有「业务参数，按项目可配」，对抗扫描不报。不把该结果说成 CLI 文案已获授权。

## 8. Findings（整批收齐；不修）

### GR-M008-001

- 文件:行号：`tools/refresh-entry-doc-baseline.mjs:56-73`（正则 B/C/D 对现行 00-entry 0 命中）；被声称维护的句在仓内 `docs/00-current-effective-document-entry-v0.1.md:126`（该文件不在 FILES，缺陷在本模块生成器）
- 2.1：技术性（文档快照数字，无业务实体/枚举/价格/流程发明）
- 判据：GEN-002、GEN-016、GEN-009
- 2.3 类型：4 可复现命令
- 工程缺陷：**是**
- 证据：
  - 脚本头声称改「本轮/当前事实基线/取证」快照行。
  - 现行 L126：「本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`」。
  - 正则命中：A L26=1，B=0，C=0，D=0，E L205=1，F L420=1，G L388=1。
  - CMD-REFRESH-CHECK：`node tools/refresh-entry-doc-baseline.mjs --check` → EXIT 1，只报 `README.md`（应为 `720 ops / V399（377 迁移） / 55 tests`），**不报 00-entry**。
  - 隔离目录写（未写正本）：EXIT 0，L26 日期改为 2026-09-09，README 改为 V399/55 tests；**L126 仍 V385 且仍写无 surefire**。随即 `--check` → **EXIT 0 PASS**。
  - 同时 `backend/target/surefire-reports` 存在，xml tests 属性合计 55；迁移 377 个、最新 V399。
- 无业务发明理由：OpenAPI/Flyway/surefire 计数是工程快照。
- 删除？不能删整个脚本——README `baseline:auto` 块仍靠它回填。应修正则对准现行 L126 句，或停止声称维护「当前事实基线」。正常用途（README 块）在修正则后仍满足。

### GR-M008-002

- 文件:行号：`tools/verify-html-ledger-sync.mjs:12,30-36,50`；`tools/verify-html-param-tier.mjs:22,122-131`（`const git = (cmd) => execSync(\`git ${cmd}\`)` 拼接 `base`）
- 2.1：技术性（进程调用，无业务发明）
- 判据：GEN-010、GEN-004、GEN-009
- 2.3 类型：4
- 工程缺陷：**是**
- 证据：隔离 TMP、cwd 非主仓，`node tools/verify-html-ledger-sync.mjs 'origin/main; echo INJECTED_LEDGER'`。shell 把分号后的 echo 当独立命令；`merge-base` 误用后 `echo` 成功导致后续 `git diff --name-only INJECTED_LEDGER HEAD` 抛未捕获 `Error: Command failed`，不是「取不到基线」设计 FAIL。同文件对 `git show` 已用 argv 数组以避免 Windows 重定向，口径不一致。
- CI 里 `HTML_GATES_BASE` 来自 GitHub sha 表达式，实际利用面窄，但判据写的是注入本身。
- 删除？不能删门禁。把 ref 当 `execFile` 参数即可。正常用途（比 origin/main 或 PR base sha）仍满足。

无添加项。无 HTML 自身问题申请。无 LOCK-AMEND。B/C 不进修复队列。

## 9. 无缺陷技术性观察（无业务发明）

1. `requiredDeliveryCommands` 重复 5 个脚本名（propagation-fixtures×3 等）。`includes` 幂等。ponytail：`verify-delivery-gates.mjs:L123-131 shrink: 50→44`。删重复不改变绿/红。
2. 台账闸只要求 `business-decision-ledger.md` 出现在同一 diff。空白行可通过。与脚本「同 PR 同步台账」的文件级自称一致。多条件理论边界，不添规则。
3. 未改 HTML 时 param-tier 不扫描。现行三份 HTML 全量 scan 未标层级=0，空基线不是藏污。
4. `E-ignore-marker.log` 当时裸豁免绿；现行回归「空豁免」红。历史证据，不当当前完成。删 log 不影响回归覆盖的正常用途。
5. `A/D/I2` log 当时基线 6 条；现行 `entries: []`。那 6 句已 ignore 或标「业务参数」或改写（如 51% 已不在 HTML）。
6. `99-predeploy-final.log` PASS 文案仍写「未登记条目不在保护范围」，现行脚本 L275 已含整条正文与九列镜像。旧跑。
7. `verify-entry-anchors` EXCLUDE 把文件名含 `-report` 的现行报告一并排除。本席抽查 `nas-diagnostic-report`/`00-entry` 无「实测 L」；现存「实测 L」多在 changelog（排除合理）。过宽排除仅登记。
8. 检查⑦无夹具；现行仓镜像列失配已打红。不另立测试工程。
9. CLI 中英混排。不把 B/C 当授权，也不当用户界面冲突。
10. 回归未进 `package.json`。`docs/deploy-and-change-flow-v0.1.md` 写可丢弃副本运行。真门禁已在 `verify:predeploy`。
11. `toISOString()` 日期是 UTC。隔离写出现 2026-09-09。理论边界。
12. PGM-036 不适用，见 §4。

ponytail-review（只对缺陷/重复，不改代码）：

- `verify-delivery-gates.mjs:L123-131: shrink: requiredDeliveryCommands 重复 6 次字符串。unique 即可。`
- `refresh-entry-doc-baseline.mjs:L56-67: yagni: 三条已 0 命中的正则。对准现行句或删。`
- `verify-html-ledger-sync.mjs:L12: native: execSync 字符串改 argv。`
- net: 约 −15 行可能（重复项+死正则），以修正确性为先。

## 10. 本席实跑命令（非锁行）

均在主仓只读或 `/tmp/m008-*` 隔离；正本 17 文件 `git diff --stat` 空。baseline SHA 事后仍为 `9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8`。

| 标签 | 命令 | EXIT | 要点 |
|---|---|---:|---|
| CMD-NODE-CHECK | `node --check` 六份 mjs | 0 | 语法过 |
| CMD-LEDGER | `node tools/verify-html-ledger-sync.mjs` | 1 | 工作区 BSM 脏、台账未同批（工具成立，不扩修 HTML） |
| CMD-PARAM | `node tools/verify-html-param-tier.mjs` | 0 | 改了 BSM，全文件扫无基线外未标句（存量 0） |
| CMD-DELIVERY | `node tools/verify-delivery-gates.mjs` | 0 | `DELIVERY_GATES_DOCS_PASS` |
| CMD-REFRESH-CHECK | `node tools/refresh-entry-doc-baseline.mjs --check` | 1 | 只 README |
| CMD-ANCHORS | `node tools/verify-entry-anchors.mjs` | 1 | 6 处，注册表/账本 vs 现行 BSM（父链 CSV） |
| CMD-REGRESSION | `node tools/gate-hardening-tests/m001-regression.mjs` | 0 | 49 PASS |
| CMD-SCAN3 | 同款 scan 三 HTML | 0 命中 | 未写文件 |
| CMD-ISO-REFRESH | `/tmp/m008-refresh-*` 写再 `--check` | 写 0 / check 0 | L126 仍 V385 且伪绿 |
| CMD-ISO-INJECT | TMP `… 'origin/main; echo INJECTED_LEDGER'` | 1（抛错） | echo 被执行 |

未跑：`npm run verify:delivery-gates` 全链、`verify:predeploy` 全链、真 push/PR/merge/部署、正本 `--write-baseline` 有效变更（曾误从主仓 cwd 调 `--write-baseline`，因扫描 0 条写出与正本空 entries 同 SHA，status 干净；此后只在 TMP cwd 复跑）。

## 11. 限制（如实，不是有条件 PASS）

- 共享主工作区 `docs/business-system-map-for-operations-v0.1.html` 已脏；ledger-sync/entry-anchors 现行红证明闸在工作，问题在父链 HTML/CSV/台账，本模块不修、不重开 M001/M002。
- 未把旧 M001/M007 票当本 17 文件已审。
- 未创建主项目 worktree。
- `batch-grill-me` / `ubiquitous-language` 缺口见 §0。
- 同目录未跟踪 `m007-regression.mjs` 不在范围。
- 三 HTML 只读；未发现需按铁律申请改 HTML 的本模块冲突句。

## 12. 结论

**FAIL**

全部 17 文件已全文覆盖，19 条判据已逐条给适用/不适用理由，三线已跑。存在未定稿工程缺陷 GR-M008-001、GR-M008-002。无添加项、无未答 questions、无条件 PASS。B2 再分配永久 ID 与队列归属。

## M008 B2-r1 整批候选复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 全文 21297 bytes 原样保留，未改写首个本次完整 B1 边界）
- 席位：Grok（审计席乙，独立）。步骤：B2。模块：M008 — 治理门禁与 CI 基线／检查及生成工具／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；B3=0，无源修复
- 本席 B2：**FAIL**
- 本追加段不写独占锁哈希字段名。观察值（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。同值见于 sidecar 全文一行、`ledger.md` 顶行。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席。未实际 `git push`/`merge`/`commit`（业务仓）、未 `gh pr create/merge` 或 PUT、未覆盖真实 hook、未部署。未读其它模块历史票当依据。未用 Codex 本批 B2 追加段作证据。

此票只判断 findings.md 顶部 V1.10「当前 M008 B2-r1整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、修复队列与最小修法。不是独立 B1/B5 PASS，不是模块 PASS，不是源已修，不是全链绿。必须另有 Codex 本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

本票依据只落到：当前 17 文件原文、冻结判据 GEN-001–018 与 PGM-036 原文、HTML 原句、隔离目录可复现命令输出。不把本席原 B1 意见、Codex 候选措辞或席位身份当正确性证明。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.10，全文件 379232 bytes，SHA256 `d1b4c899c1771290e18a981639e187f072d118cbfed4df4c456698895bee8f35`。完整读取最上「当前 M008 B2-r1整批候选（尚未定稿）」001–016 段至该节结束；下方他模块历史原文未改写、不重审。
- Codex B1：`docs/audit-2026-09/modules/M008-b1-self-raw.md` 原 V0.01 前缀 143625 bytes，SHA256 `98dcb89462c00b3065764642043f61d08da478d742a7b99961bd9369b9aa27a3`，结论 FAIL。只读该前缀；其后本批 B2 追加不作为本席依据。
- Grok B1：本文件 V0.01 前缀 21297 bytes，SHA256 `8dd0fb2ef8b803c49b745f67e2852afe1f5109dec0271319b26112f0690f5af3`，结论 FAIL。该前缀本票追加前实算仍全等，保留。
- ledger 只读顶部：版本 V1.91；`LOCK-AMEND-COUNT: 1`；M008 `IN_PROGRESS`，B3=0。主索引文件 SHA256 `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa`；`git diff --cached --raw` 空。不凭索引文件字节变化推断暂存内容，不擅自还原。
- 当前 17 源文件指纹与 B1 表全等，`splitlines` 行数之和 **1530**；工作区相对 HEAD 这 17 路径无已跟踪改动。未跟踪 `tools/gate-hardening-tests/m007-regression.mjs` 仍不在 FILES，不扩审。

| 路径 | 行 | SHA256 |
|---|---:|---|
| tools/html-param-tier-baseline.json | 10 | 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8 |
| tools/refresh-entry-doc-baseline.mjs | 122 | 76744e9a6285e6683beec1e2f7ec30105cb53cf878b6a5ec7d29ae6932647d8f |
| tools/verify-delivery-gates.mjs | 325 | d75d32a0b5586e35e66e758ae95ff44e72dbb030eef2e8de1188b4878b399cac |
| tools/verify-entry-anchors.mjs | 275 | 89a0d40c03b6d8c659575f432e84e0fc3df7cffdf6d1704916dddc9e9994ec2c |
| tools/verify-html-ledger-sync.mjs | 68 | f325b4ccd9a4948b1eb7cc196fed4b6767c2a68d10f84005bf69943ddcb21944 |
| tools/verify-html-param-tier.mjs | 198 | f0010963d1d004f124da724cd52f990704b7b6466735fccd92edd653c67fb43e |
| tools/gate-hardening-tests/99-predeploy-final.log | 138 | 61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421 |
| tools/gate-hardening-tests/A-worktree-uncommitted.log | 42 | 14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4 |
| tools/gate-hardening-tests/B-committed.log | 17 | 3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339 |
| tools/gate-hardening-tests/C-no-baseline.log | 39 | 5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67 |
| tools/gate-hardening-tests/D-baseline-legacy-pass.log | 10 | 7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164 |
| tools/gate-hardening-tests/E-ignore-marker.log | 5 | 0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633 |
| tools/gate-hardening-tests/F-ci-baseline-evidence.log | 7 | 5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f |
| tools/gate-hardening-tests/G-merge-pr-guards.log | 24 | 59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae |
| tools/gate-hardening-tests/I1-baseline-ratchet.log | 13 | fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483 |
| tools/gate-hardening-tests/I2-ignore-granularity.log | 21 | 75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b |
| tools/gate-hardening-tests/m001-regression.mjs | 216 | edb313704c4efbfae3946a9898e2c94771ed5a7990feed7bcccecc959415889b |

映射行 `M008 → [GEN-001 … GEN-018, PGM-036]`，19/19 定义区存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 开工技能优先；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问结构，事实自查；不等操作员、不派子代理 |
| verification-before-completion | 声称前先跑只读/隔离命令 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码、不加诊断插桩 |
| codebase-design | 删除测试：17 文件仍有治理闸用途；本票不扩接口 |
| ponytail-review | 005 的重复常量属 shrink 观察，不升格为正确性缺陷 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本体要写 hook 配置 → **未用** |
| resolving-merge-conflicts | 本节点无合并冲突 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到 batch-grill-me SKILL.md**；ubiquitous-language 会另写超 §7 文件 → 均未冒充调用 |

未用 MCP。未派子代理。`grill-me` 仅转发 grilling。

### 上轮误写承认（不得再说成未写）

B1 §10 已记：曾从主仓 cwd 调用 `verify-html-param-tier.mjs --write-baseline`。当时扫描 0 条，写出字节与原空 `entries` 相同，SHA 仍 `9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8`，`git status` 干净。同字节不是「没有写尝试」。本票未再对主仓执行 `--write-baseline`、refresh 默认写、hook 安装或任何其它写模式。写模式复现只在 `.scratch/m008-b2-r1-grok-iso/` 嵌套 Git/资料副本；结束后已删除该目录。

### 完整差集（CX01–10、GR-M008-001/002、观察1–12）

Codex 本次临时项：CX-01–06 工程缺陷候选；CX-07–10 观察。Grok 本次临时项：GR-M008-001、GR-M008-002、观察1–12。漏并：无。篡改原意见：无（005 队列归属除外，见下，此即本批 FAIL 原因）。无锁外新判据、无新强制门、无第三主体、无添加项、无 questions、无 HTML 自身问题上报。

| 正式ID | 全部本次来源 | 四分类 | 工程缺陷 | 修复队列 | 本席票 | 差集结论 |
|---|---|---|---|---|---|---|
| M008-001 | CX-01；Grok §5 includes 子串观察 | 技术性 | 是 | 是 | **是/是** | 全并。echo 反例为本票独立复现；不把 B1「不是精确集合」改写成当时已立缺陷 |
| M008-002 | CX-02 | 技术性 | 是 | 是 | **是/是** | 全并。Grok B1 未覆盖空 body；不得用现行 1228 行无空值覆盖失败 |
| M008-003 | CX-03；与观察2/012 文件级区分 | 技术性 | 是 | 是 | **是/是** | 全并。删除台账 ≠ 012 任意追加一行 |
| M008-004 | CX-04 | 技术性 | 是 | 是 | **是/是** | 全并。物理行当句；换行误拒、同行两 p 漏拒 |
| M008-005 | CX-05 + Grok观察1 | 技术性 | 否 | 否 | **否/否** | 来源全并，**队列不同意候选「是」**。见专节 |
| M008-006 | CX-06 + GR-M008-002 | 技术性 | 是 | 是 | **是/是** | 全并。原 B1「git show 已用 argv」不成立，原文保留，本票按源码更正 |
| M008-007 | GR-M008-001；Codex 收报后补验，纠正 CX-07 漏查 | 技术性 | 是 | 是 | **是/是** | 全并。合成样例不能冒称原先已发现 L126 |
| M008-008 | CX-07 其余生成器观察 | 技术性 | 否 | 否 | **否/否** | 全并。007 单列漏匹配；不加自动版本管理 |
| M008-009 | CX-08 + Grok 现行数据红 | 技术性 | 否（本模块源码） | 否 | **否/否** | 全并。6 红是正确拒绝；父链红须保留 |
| M008-010 | CX-09 + Grok观察4/5/6 | 技术性 | 否 | 否 | **否/否** | 全并。历史 log 不当当前绿证，不删不改 |
| M008-011 | CX-10 + Grok观察8/10 | 技术性 | 否 | 否 | **否/否** | 全并。49 PASS；不另立测试工程 |
| M008-012 | Grok观察2/3 | 技术性 | 否 | 否 | **否/否** | 全并。不与 003 混同；不把「三 HTML 均 0」说成 Codex 已全量实跑 |
| M008-013 | Grok观察7 | 技术性 | 否 | 否 | **否/否** | 全并。-report 排除偏宽仅登记 |
| M008-014 | Grok观察9 | 技术性 | 否 | 否 | **否/否** | 全并。B/C 不延伸为 CLI 授权，不顺手翻译 |
| M008-015 | Grok观察11 | 技术性 | 否 | 否 | **否/否** | 全并。UTC 取证日，不增时区参数 |
| M008-016 | Grok观察12 + 全 17 文件分类 | 技术性 | 否 | 否 | **否/否** | 全并。PGM-036 无实现面；不代替后续设备模块 |

冲突 0。添加 0。细化 0。技术性 16。无业务发明总理由：提示词 2.1 明文 CI/检查脚本不算业务发明；17 文件不引入票种/状态/角色/价格/审批链/商户/设备台账等 HTML 实体。PGM-036 原文是设备绑定员工/班次/点位/状态，不是本模块上位，故不进细化。B/C 两句有「业务参数，按项目可配」，TIER 命中，不判冲突，亦不把该确认扩成 CLI PASS/FAIL/DRIFT 授权。

### 001 独立裁定（includes 把 echo 当必跑命令）

源 `tools/verify-delivery-gates.mjs:153–156、312–315`：`deliveryScript.includes(command)` 与 `predeploy.includes(gate)`。隔离复制真实被读输入：原样 EXIT 0 `DELIVERY_GATES_DOCS_PASS`。只把 `verify:predeploy` 中 `npm run verify:html-param-tier` 换成 `echo verify:html-param-tier`，实际必跑命令消失，仍 EXIT 0。铁律 6：该替换后第一次跑门禁即伪绿，不需要并发或第二事件。GEN-002/008/009/013。Grok B1 只记「includes 不是精确集合」，未构造 echo 反例；本票不把那句改写成当时已立缺陷。最小修法同意候选：识别已有命令的实际执行位置，不能以打印/相似名冒充，不新增门禁项目。

### 002 独立裁定（空 entry_body 跳过全文一致）

源 `:113–116`：`if (entryBody && !flat[code].includes(entryBody))`。隔离 1 行有效 CSV 与 HTML 匹配 → EXIT 0，PASS 文案含「注册表条目正文逐字与 HTML 现文一致」。只清空该行 `entry_body` → 仍 EXIT 0，同一句 PASS。现行注册表 1228 行非空 body=0，不能拿存量无空值证明分支安全。脚本头声称整条正文保护。GEN-002/004/009/016。最小修法同意：必查正文为空须失败，不重造 CSV 框架。

### 003 独立裁定（删除台账即 PASS；≠012）

源 `:50–59`：`changed.includes(LEDGER)` 不区分增改删。隔离：改 HTML、台账不动 → EXIT 1「未同步」；删除台账文件 → EXIT 0「决策台账已同步」。删除是一次合法 Git 改动，第一次就会把完整性说成已同步。GEN-002/006/009/011。012 同树「台账任意追加一行」EXIT 0，与脚本自称的文件级同批规则一致，不得并进本 ID。最小修法同意：已同步至少要求台账仍存在；保持文件级同批，不添加自然语言语义审查。

### 004 独立裁定（物理换行误拒、同行两 p 漏拒）

源 `:54–85` 按物理行 `masked.split("\n")`，整行命中 TIER 则跳过。隔离同一句「测试期限30天（业务参数，按项目可配）」：整行 EXIT 0；仅在 30 天后换行 EXIT 1（误拒「测试期限30天」）；两个独立 p 同一物理行、首段有标注、次段「保质期30天，冷却3次，上限100分」无标注 → EXIT 0 漏拒。排版不是两三件不巧。GEN-002/008/009/016。不改业务 HTML、不改层级含义/精确数值豁免/棘轮。最小修法同意：按现有逻辑句/块识别，排版不改变结论。

### 005 独立裁定（保留 B1 原观点：非工程缺陷；本批因此 FAIL）

**B1 原文保留：** GEN-001 表「50 项/44 唯一，includes 幂等，重复不改判定，工程缺陷否」；观察1 与 ponytail `shrink: 50→44`。该 FAIL/观察原文不改。

独立核实的事实（同意 Codex 实证，不同意其队列归属）：

- 常量数组 50 项、44 唯一、6 个额外重复：`positive-propagation-fixtures`×3，另四类各×2。
- 隔离去掉 `verify:delivery-gates` 脚本字符串里那一条实际命令后，EXIT 1，完全相同错误行打印 **3** 次。
- `includes` 对 PASS/FAIL 幂等：有命令仍绿，无命令仍红；只是红的时候多喊两声。

GEN-011：三行相同的真实失败不是「相互矛盾或不可验证描述」，判据不成立。GEN-001 字面「重复应删除」可支撑 ponytail 收窄，但本模块工程缺陷门槛是声称未执行/伪成功/伪失败。缺命令第一次正常跑仍然正确为红；重复打印不把门禁跑错。铁律 6 的「第一次就会撞上」在这里撞上的是噪声，不是错误结论。

**本席票：工程缺陷否，修复队列否。** 与候选「是（Codex候选；有分歧）／是（待双席裁定）」的队列归属冲突。不得为合取改口。最小候选改法：**005 工程缺陷改为否，候选修复队列改为否**；来源归并句保留「CX-05 + Grok观察1」及「同一缺失命令报 3 次」实证；B3 不得借本 ID 删常量。可选删除留给日后无 ID 的工程整理，不是本轮修复队列。

因 005 队列不同意，本批 B2 **FAIL**。001–004/006–007 即使本席同意是，在候选 005 改否并再经双席 B2 之前不得定稿、不得进 B3。

### 006 独立裁定（shell 拼接基线；git show 并非 argv）

两脚本 `const git = (cmd) => execSync(\`git ${cmd}\`)`，基线进字符串。`verify-html-param-tier.mjs:141` 现文为 `execSync(\`git show ${mergeBase}:${BASELINE}\`, { stdio: ["ignore","pipe","ignore"] })`——用 stdio 避开 `2>/dev/null` 的 Windows 路径问题，**仍是 shell 字符串，不是 argv / execFileSync**。Grok B1「同文件对 git show 已用 argv」不符合源码；**原句保留，本票更正**。

隔离（嵌套 Git，cwd 非主仓）：

| 探针 | 工作区 HTML | ledger / param-tier |
|---|---|---|
| `absent-ref` | 干净 | 各 EXIT 1，设计 FAIL 文案 |
| `HEAD;echo M008_BASE_ARG` | 干净 | 各 EXIT 0 伪 PASS，「比对范围：提交区 HEAD;echo …」，stderr 有 merge-base 用法 |
| `origin/main; echo INJECTED_LEDGER` | 干净 | 各 EXIT 0 伪 PASS（echo 成功遮住 merge-base 失败） |
| `origin/main; echo INJECTED_LEDGER` | 脏 | 各 EXIT 1，未捕获 `git diff --name-only INJECTED_LEDGER HEAD` |
| `HEAD;echo M008_BASE_ARG` | 脏 | ledger EXIT 1，未捕获 `git diff --name-only M008_BASE_ARG HEAD` |

干净树上 Codex 无害 echo 得伪 PASS；脏树上 Grok 同类探针后续非 0。两条都是同一拼接缺陷的不同后续，不是两套规则。GEN-002/004/009/010/018。最小修法同意：两脚本 Git 调用改 `execFileSync` 参数数组，**含 git show**；合法基线行为保持。不是新增业务规则。

### 007 独立裁定（00-entry:126 漏匹配；不改 M435/M436 正本）

源 `:56–73`。现行 `docs/00-current-effective-document-entry-v0.1.md:126`：「本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`」。正则 A 命中 L26；B/C/D 对 L126 为 0；E/F/G 在其它行。主仓 `--check` EXIT 1 **只报 README**（720 ops / V399（377 迁移） / 55 tests），不报 00-entry。

隔离复制实际入口/README/OpenAPI/迁移/surefire：write EXIT 0（回填 README 与入口其它快照句），随即 `--check` EXIT 0 PASS；L126 仍 V385、仍写无 surefire。正本 L126 未动。Codex B1 CX-07 用合成「本轮（日期）核验为 N operations」样例，覆盖的是 A 类句，未覆盖 126；不能冒称原先已发现。GEN-002/009/016。最小修法同意：修生成器对当前活跃快照句的匹配，保留历史变更句；本阶段只改工具及最小验证，**不改 M435 README 或 M436 入口正本**。

### 008–016（仅登记）

- 008：生成器 README 自动块/其它有效快照行 check-write-check 正常；真实 README 55/862 漂移属 M435，入口资料属 M436；未承诺自动版本管理。007 单列漏匹配。不加自动版本管理。实际修文件版本头仍由 Codex 遵用户规则推进。
- 009：本席现行 `verify-entry-anchors` EXIT 1，同 6 红（registry BSM-E0433/E0736；authority BSM-0283 source/preconditions/state_transition 与 BSM-0530 source）。`verify-html-ledger-sync` EXIT 1：工作区 BSM 改动、台账未同步。工具正确拒绝。registry→M427、authority→M425、coverage→M428、decision ledger→M426。不改 HTML、不豁免为了当前绿；不声称关联 `verify-entry-anchors.test.mjs` 通过。当前父链红必须保留，模块源码通过 ≠ 全链绿。
- 010：10 份 .log 是历史证据。E 旧裸 ignore 放行 vs 现行拒绝；A/D/I2 旧 6 条 vs 现行 `entries: []`；99 旧全链 PASS 不是当前全链。不删不改，不冒充本轮验证。
- 011：本席 `m001-regression.mjs` EXIT 0，49 PASS + `M001_GOVERNANCE_REGRESSION_PASS`。未挂 npm。不另立测试工程。B3 只为定稿缺陷写最小可失败回归。
- 012：文件级台账接受任意新增行（本票 EXIT 0）；未改 HTML 时 param-tier 跳过。Grok B1 同款 scan 三 HTML 未标层级/裸豁免均 0；Codex 只实跑当前改动 BSM 为 0。不把三份全 0 写成 Codex 已全量实跑。不与 003 删除混同。
- 013：EXCLUDE 含 `-report`，偏宽；无真实误排导致错误通过的本票证据。不加新排除策略。
- 014：CLI PASS/FAIL/DRIFT 是内部标识，不是 PGM-036 设备或产品 UI。不顺手翻译。用户 B/C 只确认 BSM:1020 / :1068 两处业务配置原意。
- 015：`toISOString()` UTC。本票隔离回填日期 2026-09-09。不增时区参数。
- 016：PGM-036 原文 611 为设备绑定员工/班次/点位/状态。17 文件是治理检查、计数、测试及历史证据，无实现面。不用本模块票代替后续设备功能审计，不新增判据。

### 分类 / 队列 / 允许的最小修复边界

全批 001–016 分类技术性成立。本席队列：001–004、006–007 是；005、008–016 否。候选把 005 写成拟修「是」，本席不同意 → 本批不能定稿。

若候选按上节改 005 为否、且此后双席 B2 均 PASS，一次 B3 只修当时仍为「是」的 ID，并按各 ID 归因最小可失败测试（GEN-003）。允许边界：

- 001：识别已有命令实际执行位置，禁 echo/注释/相似名冒充；不新增门禁项目。
- 002：必查 `entry_body` 为空则失败；不重造 CSV 框架。
- 003：已同步至少要求台账文件仍存在；保持文件级同批，不加语义审查。
- 004：按逻辑句/块识别；排版不改变结论；保留精确数值豁免与棘轮；不改业务 HTML。
- 006：两脚本 Git 改 `execFileSync` argv，含 `git show`；合法基线保持。
- 007：生成器匹配当前活跃快照句；保留历史句；最小验证；不改 README/00-entry 正本。

禁止：改三份业务 HTML；改 005/008–016；锁外新判据；新强制门；第三主体；把模块通过理解成全链绿；本轮改 M435/M436/M425–M428/M427 正本资料。全部改动文件自身版本递增。

### 本票命令（主仓只读；写模式仅已删除的隔离副本）

主仓：六份 `node --check` 0；`verify-delivery-gates` 0；`verify-html-param-tier`（无 `--write-baseline`）0；`refresh --check` 1 只 README；`verify-html-ledger-sync` 1（BSM 脏、台账未同步）；`verify-entry-anchors` 1（上述 6 红）；`m001-regression` 0 / 49 PASS。未跑全仓 `verify:predeploy`。未对正本 `--write-baseline` / refresh 默认写。

隔离 `.scratch/m008-b2-r1-grok-iso/`（已删）：001 echo 伪绿、002 空 body 伪绿、003 删除台账伪绿、004 换行误拒/同行两 p 漏拒、005 缺命令 3 次同文、006 两探针、007 真资料 write0→check0 而 L126 仍 V385。

### 结论

差集无遗漏、无把观察升格为业务添加。001–004、006–007 独立复验成立，同意入队。008–016 仅登记。**005 本席维持 B1：工程缺陷否、队列否**；候选当前「是」不得合取。本席 B2 **FAIL**。最小修正：005 两列改为否，保留双方原文与 3 次打印实证。未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M008 `IN_PROGRESS`，B3=0。当前真实父链红保留。


## M008 B2-r2 整批候选复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 全文 21297 bytes 与 V0.02 B2-r1 追加合计 42982 bytes 原样保留，未改写、未覆盖）
- 席位：Grok（审计席乙，独立）。步骤：B2-r2。不是 B1 重跑，不是 B3 修复。
- 模块：M008 — 治理门禁与 CI 基线／检查及生成工具／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）；冻结 17 文件 / 1530 行 / 19 判据（GEN-001–018 + PGM-036）
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；B3=0，无源修复
- 本席本批 B2：**PASS**
- 本追加段不写独占锁哈希字段名。观察值（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。同值见于 sidecar 全文一行、`ledger.md` 顶行、本文件既有 B1 首行。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席。未实际 `git push`/`merge`/`commit`（业务仓）、未 `gh pr create/merge` 或 PUT、未覆盖真实 hook、未部署。未读其它模块历史票当依据。未用 Codex 本批 B2-r2 追加段作证据（self-raw 仅核 B1 前缀 143625 bytes）。不问操作员。

此票只判断 findings.md 顶部 V1.11「当前 M008 B2-r2整批候选」相对本次两份完整 B1 raw 与上批 B2-r1 候选的全批差集、GEN-017 四分类、修复队列与最小修法。不是独立 B1/B5 PASS，不是模块 PASS，不是源已修，不是全链绿。必须另有 Codex 本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 与 V0.02 B2-r1 **FAIL** 原文。

本票依据只落到：当前 17 文件原文、冻结判据 GEN-001–018 与 PGM-036 原文、HTML 原句、只读哈希/计数、以及上批已证明且源码指纹未变的反例。不把本席原 B1/B2-r1 意见、Codex 候选措辞或席位身份当正确性证明。

### 本批输入（完整指定文件）

- 候选：`docs/audit-2026-09/findings.md` 版本 V1.11，全文件 387743 bytes，SHA256 `947406075f723a0adb103aa9985930538f958741aaa2c454ac004cf1d39044ee`。完整读取最上「当前 M008 B2-r2整批候选（尚未定稿）」001–016 段至该节结束；下方 B2-r1 候选原文与他模块历史未改写、不重审。
- Codex B1：`docs/audit-2026-09/modules/M008-b1-self-raw.md` 原 V0.01 前缀 143625 bytes，SHA256 `98dcb89462c00b3065764642043f61d08da478d742a7b99961bd9369b9aa27a3`，结论 FAIL。只读该前缀。
- Grok B1：本文件 V0.01 前缀 21297 bytes，SHA256 `8dd0fb2ef8b803c49b745f67e2852afe1f5109dec0271319b26112f0690f5af3`，结论 FAIL。该前缀本票追加前实算仍全等。
- Grok B2-r1：本文件 V0.02 合计 42982 bytes，SHA256 `49a890bb74e88f32f8e3fbf9b8c053c2991de1a7a5006db0dbd79edea5600866`，结论 FAIL（仅因 005 队列为是）。本票追加前实算仍全等，保留。
- ledger 只读顶部：版本 V1.92；`LOCK-AMEND-COUNT: 1`；M008 `IN_PROGRESS`，B3=0。主索引文件 SHA256 `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa`；`git diff --cached --raw` 空。不凭索引文件字节变化推断暂存内容，不擅自还原。
- 当前 17 源文件指纹与 B1/B2-r1 表全等，`splitlines` 行数之和 **1530**；工作区相对 HEAD 这 17 路径无已跟踪改动。未跟踪 `tools/gate-hardening-tests/m007-regression.mjs` 仍不在 FILES，不扩审。
- `questions/` 无 M008 文件。无 HTML 修改申请、无 LOCK-AMEND、无新 ID。

锁文件独立实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 bytes，版本 V0.11）。工作树相对 git HEAD 仍显示 V0.06→V0.11 历史差，属既有 COUNT=1 冻结锁，不是本批改锁；GEN-001/011 定义区现文未改。映射行 `M008 → [GEN-001 … GEN-018, PGM-036]`，19/19 定义区存在。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 开工技能优先；用户/提示词覆盖技能本体。本任务为指定 B2 复核，不进入 brainstorming/plan |
| receiving-code-review | 先核源/哈希/差集再接受或反驳；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问树自己取证；不等操作员、不派子代理 |
| verification-before-completion | 声称前先跑本批只读核验；用户禁止重跑已证明反例，故 001–004/006–007 以源码指纹未变 + 上批隔离反例为据，本批另做哈希/差集/005 计数/002 空 body 计数 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码、不加诊断插桩 |
| codebase-design | 删除测试：17 文件仍有治理闸用途；本票不扩接口 |
| ponytail-review | 005 重复常量仍属 shrink 观察，不升格为正确性缺陷，也不写成新判据 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本体要写 hook 配置 → **未用** |
| resolving-merge-conflicts | 本节点无合并冲突 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到 batch-grill-me SKILL.md**；ubiquitous-language 会另写超 §7 文件 → 均未冒充调用 |

未用 MCP。未派子代理。`grill-me` 仅转发 grilling。

### 上轮误写承认与本批写约束

B1 §10 / B2-r1 已记：曾从主仓 cwd 调用 `verify-html-param-tier.mjs --write-baseline`，写出字节与原空 `entries` 相同，SHA 仍 `9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8`。同字节不是「没有写尝试」。本票未再对主仓执行 `--write-baseline`、refresh 默认写、hook 安装或任何其它写模式。本批禁止主仓写模式；写模式反例不重跑。

### 候选仅按 005 收窄：独立差集

程序对照 findings 顶部 V1.11 r2 表与下方完整保留的 V1.10/r1 表，16 行 ID 集合全等：`M008-001`–`M008-016`。除 005 外 15 行七列逐字相同。005 仅下列列变化，其余 ID 无增删、无改分类、无改队列、无改修法边界。

| 列 | r1（B2-r1 候选） | r2（本批候选） | 本席判定 |
|---|---|---|---|
| 来源归并 | CX-05 + Grok观察1 | CX-05 + Grok观察1；B2-r1外审否决后本方收窄 | 来源仍 CX-05+观察1；后半为过程注，不是新 finding |
| 分类 | 技术性 | 技术性 | 不变，同意 |
| 工程缺陷 | 是（Codex候选；有分歧） | 否（本批仅登记） | 同意收窄为否 |
| 队列 | 是（待双席裁定） | 否 | 同意收窄为否；此即上批唯一 FAIL 点 |
| 位置事实 | 50常量44唯一，positive-propagation×3及另四类×2；移除后同错打印3次 | 50项44唯一、6额外重复；同错打印3次且仍正确 exit1，有命令仍 PASS。双方对事实一致，原候选分歧完整保留 | 事实收窄表述与本席实算一致；细节仍在 r1 表 |
| 判据处置 | Codex 拟删 6 重复；Grok 判无缺陷；两票保留待裁定 | 本批仅登记；B3 不得删这 6 项；GEN 定义不改；不外推「所有工程缺陷只能影响红绿」 | 同意；见 005 专节 |

完整映射（沿用 r1 公式，r2 声明全部保留在这 16 ID，本席核对无漏并）：

- CX-01→001、02→002、03→003、04→004、05→005、06→006、07→007/008、08→009、09→010、10→011
- GR-M008-001→007、GR-M008-002→006
- Grok 观察1→005、2/3→012、4/5/6→010、7→013、8/10→011、9→014、11→015、12→016

无新 ID、无添加、无冲突、无 questions、无 HTML 自身问题上报、无锁外新判据、无新强制门。

### 完整差集与本批逐 ID 票

| 正式ID | 全部本次来源 | 四分类 | 工程缺陷 | 修复队列 | 本席本批票 | 差集结论 |
|---|---|---|---|---|---|---|
| M008-001 | CX-01；Grok §5 includes 子串观察 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。源仍 `includes`。上批 echo 反例已证，本批不重跑 |
| M008-002 | CX-02 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。源仍 `if (entryBody && …)`。现行 1228 行空 body=0，见笔误更正 |
| M008-003 | CX-03；与观察2/012 文件级区分 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。源仍 `changed.includes(LEDGER)`。删除≠012 |
| M008-004 | CX-04 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。源仍按物理行 `split("\n")` |
| M008-005 | CX-05 + Grok观察1 | 技术性 | 否 | 否 | **PASS 否/否** | 来源全并；队列已按上批最小修正改否。见专节 |
| M008-006 | CX-06 + GR-M008-002 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。`git show` 现文仍 shell 字符串，不是 argv |
| M008-007 | GR-M008-001；Codex 收报后补验 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r1 全等。L126 仍 V385/无 surefire；正则仍不覆盖该句。不改 M435/M436 正本 |
| M008-008 | CX-07 其余生成器观察 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。007 单列漏匹配；不加自动版本管理 |
| M008-009 | CX-08 + Grok 现行数据红 | 技术性 | 否（本模块源码） | 否 | **PASS 否/否** | 与 r1 全等。父链红须保留 |
| M008-010 | CX-09 + Grok观察4/5/6 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。历史 log 不删不改 |
| M008-011 | CX-10 + Grok观察8/10 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。不另立测试工程 |
| M008-012 | Grok观察2/3 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。不与 003 混同 |
| M008-013 | Grok观察7 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。-report 排除偏宽仅登记 |
| M008-014 | Grok观察9 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。B/C 不延伸为 CLI 授权 |
| M008-015 | Grok观察11 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。不增时区参数 |
| M008-016 | Grok观察12 + 全 17 文件分类 | 技术性 | 否 | 否 | **PASS 否/否** | 与 r1 全等。PGM-036 无实现面 |

冲突 0。添加 0。细化 0。技术性 16。无业务发明总理由不变：提示词 2.1 明文 CI/检查脚本不算业务发明；17 文件不引入票种/状态/角色/价格/审批链/商户/设备台账等 HTML 实体。PGM-036 原文（治理地图 :611）是设备绑定员工/班次/点位/状态，不是本模块上位。B/C 两句有「业务参数，按项目可配」，TIER 命中，不判冲突。

### 002 笔误更正（追加，不覆盖 B2-r1 原文）

B2-r1「002 独立裁定」有一句「现行注册表 1228 行非空 body=0」。该句与同段「不能拿存量无空值证明分支安全」及候选原实算相反，属笔误，**原段不改写**。

本批只读核验：`docs/prd-audit/entry-registry.csv` 有效数据行 **1228**；`entry_body` 去空白后为空的行 **0**。正确表述是：**空 body=0**（现行无空正文行），不是「非空 body=0」。空值分支仍须修，因为源码 `if (entryBody && …)` 在空正文时跳过「全文一致」声称；存量碰巧无空值不能证明该分支安全。

### 005 独立裁定（本批唯一收窄项）

**B1 / B2-r1 原文保留：** GEN-001 表「50 项/44 唯一，includes 幂等，重复不改判定，工程缺陷否」；观察1 与 ponytail `shrink: 50→44`。该 FAIL/观察原文不改。Codex r1「建议只删除 6 项重复常量」原文保留在 findings 下方 B2-r1 表。

本批只读复算（不重跑隔离删命令）：

- `requiredDeliveryCommands` 字面 50 项、去重 44、额外重复槽 6：`positive-propagation-fixtures`×3，另四类各×2（`ready-no-go` / `blocker-register-no-go` / `ready-positive` / `no-go-command-center`）。
- 判定仍走 `deliveryScript.includes(command)`，对 PASS/FAIL 幂等。
- 上批已证：去掉一条实际命令后 EXIT 1，完全相同错误行打印 3 次；有命令仍绿。本批采信该已证明反例，不重跑写模式隔离。

GEN-011 现文：「日志、指标和审计记录不得相互矛盾或依赖不可验证描述」。三行相同的真实失败不是相互矛盾，也不是不可验证描述，**本项不成立缺陷**。GEN-001 现文仍写「重复……应删除或收窄」，可支撑 ponytail 精简观察，但**本个案**第一次正常跑的红绿结论正确，重复打印是噪声不是伪成功/伪失败。B3 **不得**借本 ID 删这 6 个重复常量。

**明确边界（不是新判据）：** 上述只约束 M008-005 这一条重复真诊断。不把本个案外推为「所有工程缺陷必须影响红绿」的锁外新门槛。GEN-001/011 原文与锁哈希保持不动。001 echo 伪绿、002 空 body 伪绿、003 删台账伪绿、004 排版误拒/漏拒、006 拼接基线、007 漏匹配伪绿，各依其已证行为与既有 GEN ID 入队，不因 005 收窄而动摇。

**本席票：工程缺陷否，修复队列否。** 与本批候选两列一致。上批因候选为「是」而 FAIL 的唯一异议已消除。

### 001 / 003 / 004 / 006 / 007（队列是；源码指纹未变，不重跑反例）

- 001：`verify-delivery-gates.mjs:153–156、312–315` 仍 `includes`。最小修法不变：识别已有命令实际执行位置，禁 echo/相似名冒充，不新增门禁项目。
- 003：`verify-html-ledger-sync.mjs` 仍 `changed.includes`。最小修法不变：已同步至少要求台账仍存在；保持文件级同批，不加语义审查。≠012。
- 004：`verify-html-param-tier.mjs` 仍按物理行扫描。最小修法不变：按逻辑句/块识别；不改业务 HTML、精确数值豁免与棘轮。
- 006：两脚本 `const git = (cmd) => execSync(\`git ${cmd}\`)`；`verify-html-param-tier.mjs` 的 `git show ${mergeBase}:${BASELINE}` 现文仍是 shell 字符串。B1「git show 已用 argv」原文保留，B2-r1 已按源码更正。最小修法不变：改 `execFileSync` 参数数组，含 `git show`；合法基线保持。
- 007：现行 `docs/00-current-effective-document-entry-v0.1.md:126` 仍写 V385 / 无 surefire。生成器 `:56–73` 正则仍不覆盖「本轮 YYYY-MM-DD 实测 OpenAPI … Flyway 最新 V…」句。最小修法不变：只修生成器对当前活跃快照句的匹配及最小验证，**不改 M435 README 或 M436 入口正本**。

### 008–016（仅登记，与 r1 全等）

008 不加自动版本管理。009 现行父链 6 红与台账未同步是正确拒绝，归 M425–M428/M427，不豁免求绿。010 历史 log 不删。011 不另立测试工程；B3 只为定稿缺陷写最小可失败回归。012 文件级追加 ≠ 003 删除。013 不加新排除策略。014 不顺手翻译 CLI。015 不增时区参数。016 不用本模块票代替后续设备功能审计。

### 分类 / 队列 / 允许的最小修复边界

全批 001–016 分类技术性成立。本席队列：**001–004、006–007 是（6 项）；005、008–016 否（10 项）**。与本批候选一致。

若 Codex 本批亦 PASS，一次 B3 只修当时仍为「是」的 6 个 ID，并按各 ID 归因最小可失败测试（GEN-003）。允许边界与 B2-r1 相同：

- 001：识别已有命令实际执行位置，禁 echo/注释/相似名冒充；不新增门禁项目。
- 002：必查 `entry_body` 为空则失败；不重造 CSV 框架。
- 003：已同步至少要求台账文件仍存在；保持文件级同批，不加语义审查。
- 004：按逻辑句/块识别；排版不改变结论；保留精确数值豁免与棘轮；不改业务 HTML。
- 006：两脚本 Git 改 `execFileSync` argv，含 `git show`；合法基线保持。
- 007：生成器匹配当前活跃快照句；保留历史句；最小验证；不改 README/00-entry 正本。

禁止：改三份业务 HTML；改 005/008–016（含不得删 005 的 6 个重复常量）；锁外新判据（含不得把「必须影响红绿」写入锁）；新强制门；第三主体；把模块通过理解成全链绿；本轮改 M435/M436/M425–M428/M427 正本资料。全部改动文件自身版本递增。

无添加项，不写 questions。无本批最小修订要求。

### 本票命令（主仓只读；不重跑已证明反例）

- 锁/候选/两 raw 前缀/17 文件 SHA256 与行数：见上，全等。
- `requiredDeliveryCommands` 计数：50/44/6。
- 注册表：1228 行，空 `entry_body`=0。
- 源码点读：001 `includes`、002 `if (entryBody &&`、003 `changed.includes`、004 `split("\n")`、006 `execSync(\`git ${cmd}\`)` 与 `git show ${mergeBase}`、007 L126 与正则区。
- 未跑：六份 `node --check`、各 verify 入口、隔离写模式、全仓 `verify:predeploy`、正本 `--write-baseline` / refresh 默认写。上批已跑结果不在本段复述为新跑。

工作区 `docs/business-system-map-for-operations-v0.1.html` 相对 HEAD 仍脏，属既有父链资料问题，本模块不修、不重开 M001/M002。

### 结论

候选仅按 005 工程缺陷/修复队列两列收窄为否，16 ID 无漏并、无篡改，CX01–10 / GR-M008-001/002 / 观察1–12 映射不变。001–004、006–007 同意入队；005、008–016 仅登记。GEN-001/011 及锁原文不改，不外推新判据。002 笔误在本追加更正为「空 body=0」，原句保留。B1 误写 baseline 同字节事实保留。

本席 B2-r2 **PASS**。未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M008 `IN_PROGRESS`，B3=0。当前真实父链红保留。


## M008 B2-r3 整批候选复核（Grok 乙席）

- 文件自身版本：V0.04（本段追加；上方 V0.01 B1 全文 21297 bytes、V0.02 B2-r1、V0.03 B2-r2 合计前缀 **60730 bytes** SHA256 `27b70f22fa0a610a37f0c013c73df5d98200ef3bb0396f8ba481836fe7716281` 原样保留，未改写、未覆盖）
- 席位：固定 Grok 外审席乙。步骤：B2-r3。输入是触发回退的两份完整 B4-r1 raw + V1.13 候选，不是 B1 全模块重审，不是 B3 修复，不是 B5。
- 模块：M008 — 治理门禁与 CI 基线／检查及生成工具／第1段
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块）；冻结判据仍 GEN-001–018 + PGM-036 共 19 条
- 正本：`/home/james/complex-management`；`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`；M008 `IN_PROGRESS`；B3 累计 **1**（本批不修源，不做出队）
- 本席本批 B2：**PASS**
- 本追加段不写独占锁哈希字段名。观察值（命令标签，非独占行）：`LOCKFILE-SHA-CMD: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。同值见于 sidecar 全文一行、`ledger.md` 顶行、本文件既有 B1 首行。未改锁/sidecar/ledger/findings/HTML/源/主索引。
- 无 MCP、无子代理、无第三席。未实际 `git push`/`merge`/`commit`（业务仓）、未 `gh pr create/merge` 或 PUT、未覆盖真实 hook、未部署。未读其它模块历史票当依据。未用 Codex 本批 B2-r3 追加段作证据。不问操作员。

此票只判断 findings.md 顶部 V1.13「当前 M008 B2-r3整批候选」相对本次两份完整 B4-r1 raw、既有 16 个永久 ID 与上批 B2-r2 定稿表的全批差集、GEN-017 四分类、修复队列与最小修法。不是独立 B1/B5 PASS，不是模块 PASS，不是源已修完，不是全链绿。必须另有 Codex 本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **FAIL**、V0.03 B2-r2 **PASS** 与 B4-r1 席乙 **PASS** 原文。

本票依据只落到：两份指定 B4 全文、V1.13 候选、当前工具源、入口 126/160 现文、冻结 19 条、以及本席本批在 `.scratch` 隔离目录独立复现的 001 换行边界与 007 写后 160 残留。不把本席原 B1/B2/B4 意见、Codex 候选措辞或席位身份当正确性证明。B4 席乙曾对 007 的 160 观察投「仅登记 008」；本批按新证据重审，不因上一票 PASS 跳过。

### 本批输入（完整指定文件）

| 项 | 版本 | 字节 | SHA256 | 本席实算 |
|---|---|---:|---|---|
| Codex B4-r1 | V0.01 | 52322 | `583370935346bbe77bfc25efb8e8efea3d6fb68130ad2d4a5549a8f702c11068` | 全等 |
| Grok B4-r1 | V0.01 | 41557 | `01904baab6ca22f9b74fc6eababfb0a293879fa2c08796eda33df837b78c0012` | 全等 |
| 候选 findings.md | V1.13 | 409554 | `0d6aa46b6d7db8ded0596afcd3a75daa5cb8e53788a3d3298c734cd6c9dd3a0f` | 全等 |
| 本文件追加前 | V0.03 | 60730 | `27b70f22fa0a610a37f0c013c73df5d98200ef3bb0396f8ba481836fe7716281` | 全等 |
| ledger.md | V1.95 | — | 只读顶部 | `LOCK-AMEND-COUNT: 1`；M008 `IN_PROGRESS`，B3=1；B4-r1 合取 FAIL 回 B2-r3 |
| criteria-lock.md | V0.11 | 889020 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` | 与 sidecar 同行 |

B4 合取（用户给定、本席核对两文结论段一致）：归因双 PASS；修复内容 Codex FAIL / Grok PASS；合取 FAIL。没有 B5、没有队列出队、没有回滚。完整 diff 21637 bytes SHA `996b9954f7b4782aafca8ef5904a556dcee90067c0919311651b85139c8089d2` 已在 B4 两席各出现一次，本批不重取树。

`questions/` 无 M008 文件。无 HTML 修改申请、无 LOCK-AMEND、无第 17 个 ID。主索引 SHA256 `eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa`；`git diff --cached --raw` 空。工作区六工具 blob 仍等于 B3 after（delivery `73bcf494…` / refresh `2ba460a5…` / anchors `7e62c6a1…` / ledger `1c9f974f…` / param `c79cdb39…` / regression `d7479d0d…`）。正本入口 SHA `34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314`、README SHA `28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5`、基线 SHA `9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8` 本批均未写。

### 技能（V0.17 §6 收窄；复用已核意图，不假称本轮重核不存在项）

本节点实际重读：`using-superpowers`、`using-arc`、Arc `audit`（只读其流程约束）、`grilling`、`receiving-code-review`、`verification-before-completion`、`systematic-debugging`、`diagnosing-bugs`、`ponytail-review`、`codebase-design`。

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 开工技能优先；用户/提示词覆盖技能本体。本任务为指定 B2 复核，不进入 brainstorming/plan |
| using-arc / Arc audit | 架构/质量审查可路由到 audit；用户禁止子代理、禁止改 findings/源、唯一落盘本文件。故 **不执行** 机械全仓扫描、不派 reviewer、不写 `docs/arc/audits/`、不打分。缺口：Arc 默认审计产物与本轮 §7 指定路径冲突，按用户指令停在意图层 |
| receiving-code-review | 先核源/隔离命令再接受或反驳 Codex B4 的 001/007；禁止表演性同意；B2 不修复。本体逐条 IMPLEMENT 作废 |
| grilling | 只借追问树自己取证；不等操作员、不派子代理。本批内部树：160 是不是 changelog、008「入口资料属 M436」是不是已裁定漏匹配无缺陷、001 的 127 是不是生产退出码 |
| verification-before-completion | 声称前先跑本批必要隔离命令（001 四探针 × 两入口；007 隔离 check/写/check 并读 126 与 160） |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码、不加诊断插桩。001 根因：`npmCommands` 的 `\\s` 含 LF。007 根因：现行正则只吃「实测」句，不吃「取证为」句，check 在 126 对齐后对入口报绿 |
| codebase-design | 删除测试：17 文件仍有治理闸用途；本票不扩通用 shell 解析器、不加自动版本管理 |
| ponytail-review | 001 不扩解析器；005 重复常量仍 shrink 观察不入队；007 不把漏匹配推成新功能 |
| writing-for-agents | **复用**上批已核意图（报告结构）；本轮未重读全文；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本体要写 hook 配置 → **未用** |
| resolving-merge-conflicts | 本节点无合并冲突 |
| batch-grill-me / ubiquitous-language | **缺口（沿用 B1 已核，本轮未假称重找到）**：本环境未找到 `batch-grill-me` SKILL.md；`ubiquitous-language` 会另写超 §7 文件 → 均未冒充调用 |
| grill-me | 仅转发 grilling，不写文件、不问事实 |

未用 MCP。未派子代理。

### 上轮误写承认与本批写约束

B1 §10 / B2-r1 / B2-r2 已记：曾从主仓 cwd 调用 `verify-html-param-tier.mjs --write-baseline`，写出字节与原空 `entries` 相同。同字节不是「没有写尝试」。本票未再对主仓执行 `--write-baseline`、refresh 默认写、hook 安装。001/007 写模式与 shell 桩仅在 `.scratch/m008-b2-r3-*` 临时目录；结束后已删除，主仓入口/README/基线 SHA 未变。

### B4 两席意见并入 16 ID（差集）

程序对照 V1.13 顶部表与下方完整保留的 V1.12/B2-r2 定稿表，16 行 ID 集合全等：`M008-001`–`M008-016`。无新 ID。分类 16 条仍全部技术性。队列集合与 r2 定稿相同：**001–004、006–007 是；005、008–016 否**。列变化仅 001/007/008 的来源与事实/处置（并入 B4 新证据），其余 ID 七列与 r2 定稿同义保留原票。

| 列变化 | r2 定稿 | r3 候选 | 本席判定 |
|---|---|---|---|
| 001 来源 | CX-01；Grok includes 观察 | 增加 Codex B4-r1 换行证据 | 同意并入原 ID，不另发 |
| 001 位置 | `includes` + echo 假绿 | 保留原 echo 票，并入 helper 把 npm/run 间 LF、&& 前 LF 当空白；&& 后 LF 合法 | 同意；现码已无 `includes` 主路径，未修完的是 helper `\\s` |
| 007 来源 | GR-M008-001；Codex 收报后补验 | 增加 GR-M008-B4-OBS-01 同根因并入 | 同意并入 007，见争议二 |
| 007 位置/处置 | 钉 126 漏匹配；修「当前活跃快照句」；不改 M435/M436 正本 | 126 已修仍保留原票；新增 160「取证为」写后残留、check 绿；处置明确含 126 与 160 | 同意；不回填正本、不改历史句 |
| 008 处置 | 007 单列漏匹配；入口资料属 M436 | 写明 007 含 B4 新增 160 证据；008 只登记有效匹配部分与资料漂移；不把生成器假绿排除到 M436 | 同意两层拆分 |

映射（无漏并、无把观察升格为新 ID）：

- CX-01→001（含 B4 LF）、02→002、03→003、04→004、05→005、06→006、07→007/008、08→009、09→010、10→011
- GR-M008-001→007、GR-M008-002→006、GR-M008-B4-OBS-01→**007**（本批改判，原 B4 008 归类原文保留在 B4 raw）
- Grok 观察1→005、2/3→012、4/5/6→010、7→013、8/10→011、9→014、11→015、12→016
- B4 现行 6 红与 ledger 红→009；历史 log→010；71 回归与未另立测试工程→011；三 HTML 隔离扫描 entries=0→012（不改写 B1 只扫 BSM 的历史事实）；UTC→015；CLI/PGM→014/016；005 常量 50/44→005

无添加、无冲突、无 questions、无 HTML 自身问题上报、无锁外新判据、无新强制门。B4 归因双 PASS，本批不要求回滚已归因的 002–004/006 合规修复。

冲突 0。添加 0。细化 0。技术性 16。无业务发明总理由不变：提示词 2.1 明文 CI/检查脚本不算业务发明；17 文件不引入票种/状态/角色/价格/审批链/商户/设备台账等 HTML 实体。

### 完整差集与本批逐 ID 票

| 正式ID | 全部本次来源 | 四分类 | 工程缺陷 | 修复队列 | 本席本批票 | 差集结论 |
|---|---|---|---|---|---|---|
| M008-001 | CX-01；Grok includes；Codex B4 LF | 技术性 | 是 | 是 | **PASS 是/是** | 原 echo 票保留；B3 helper 未闭合。见争议一 |
| M008-002 | CX-02 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r2 全等。B4 两席内容通过，因整批 B4 未双 PASS 仍留队；不要求无意义再编辑 |
| M008-003 | CX-03；与 012 文件级区分 | 技术性 | 是 | 是 | **PASS 是/是** | 同上 |
| M008-004 | CX-04 | 技术性 | 是 | 是 | **PASS 是/是** | 同上 |
| M008-005 | CX-05 + Grok观察1 | 技术性 | 否 | 否 | **PASS 否/否** | 现码仍 50/44/6。不准删。不外推新判据 |
| M008-006 | CX-06 + GR-M008-002 | 技术性 | 是 | 是 | **PASS 是/是** | 与 r2 全等。B4 内容通过仍留队 |
| M008-007 | GR-M008-001；Codex 补验；B4-OBS-01 并入 | 技术性 | 是 | 是 | **PASS 是/是** | 126 原票未闭合意义上仍是；160 同根因并入。见争议二 |
| M008-008 | CX-07 其余生成器观察 | 技术性 | 否 | 否 | **PASS 否/否** | 有效匹配部分 + M435/M436 资料漂移。不加自动版本管理。生成器假绿不归 M436 |
| M008-009 | CX-08 + Grok 现行数据红 | 技术性 | 否（本模块源码） | 否 | **PASS 否/否** | B4 仍 6 红 + ledger 红，正确拒绝，仅登记原事实 |
| M008-010 | CX-09 + Grok观察4/5/6 | 技术性 | 否 | 否 | **PASS 否/否** | 历史 log 不删不改 |
| M008-011 | CX-10 + Grok观察8/10 | 技术性 | 否 | 否 | **PASS 否/否** | 原票 49 PASS 保留；B4 现 71 PASS 是最小回归增量，不另立测试工程、不提前当 001/007 已闭合 |
| M008-012 | Grok观察2/3 | 技术性 | 否 | 否 | **PASS 否/否** | 文件级追加 ≠ 003。三 HTML 扫描已有 ID，仅登记 |
| M008-013 | Grok观察7 | 技术性 | 否 | 否 | **PASS 否/否** | 不加新排除策略 |
| M008-014 | Grok观察9 | 技术性 | 否 | 否 | **PASS 否/否** | B/C 不延伸为 CLI 授权 |
| M008-015 | Grok观察11 | 技术性 | 否 | 否 | **PASS 否/否** | 不增时区参数。本批隔离写日期仍为 UTC 2026-09-09 |
| M008-016 | Grok观察12 + 全 17 文件分类 | 技术性 | 否 | 否 | **PASS 否/否** | PGM-036 无实现面 |

### 争议一：001 换行边界（独立复验；改判 B4 席乙「内容 PASS」中的 001 已修完）

**B4 席乙原文保留：** 001 当时按 echo/相似名/整串打印/注释 8 组红→绿投修复内容 PASS；`npmCommands` 只认当前仓纯 `npm run &&`。该段不改写。

**B4 席甲新证据（本席不自动接受，本批自构同构探针）：** helper `:7–10`：

```javascript
const parts = script.trim().split(/\s*&&\s*/);
if (!parts.every(command => /^npm\s+run\s+[\w:-]+$/.test(command))) return new Set();
return new Set(parts.map(command => command.split(/\s+/)[2]));
```

JavaScript `\\s` 含 LF。只读探针：`npm\\nrun verify:html-param-tier` 对 `^npm\\s+run\\s+[\\w:-]+$` 为 true，第三字段仍是脚本名。

隔离 `.scratch`（复制真实被读输入；`PATH` 前置桩：`npm` 只打印 `NPM:%s`，`run` 打印 `RUN_NOT_NPM:%s` 并 **显式 exit 127**；cwd 非主仓；结束后删除）：

| 入口 | 变体 | GATE | SHELL | 目标 npm 调用发生 |
|---|---|---:|---:|---|
| verify:delivery-gates / verify:predeploy | 原链 | 0 | 0 | 是（`NPM:run <gate>`） |
| 同上 | ` &&\\n`（&& 后 LF） | 0 | 0 | 是 |
| 同上 | `npm\\nrun <gate>` | **0** | **127** | **否**（`NPM:` + `RUN_NOT_NPM:<gate>`） |
| 同上 | 第一处 `\\n&& ` | **0** | **2** | **否**（`/bin/sh: Syntax error: "&&" unexpected`） |

127 是隔离 `run` 桩的显式返回，**不冒称生产全链退出码**。桩未执行真实 `verify:unit` / `verify:html-param-tier`。关键事实是命令边界：LF 把 `npm` 与 `run` 拆成两个命令，门禁仍 `DELIVERY_GATES_DOCS_PASS`。&& 后 LF 为 POSIX 合法续行，须保持通过。格式化时一次插入换行即触发，不需并发。

原 001 最小修法「识别已有命令实际执行位置，不能以打印/相似名冒充，不新增门禁项目」覆盖本缺口：打印冒充已修，**行内空白与命令换行未区分**。仍是同一 ID，不另发。GEN-002/004/008/009/013，类型 2/4，技术性，工程缺陷是，队列是。最小方向同意候选：修共享 helper 区分行内空白与命令换行，仅保留 && 后合法续行；不引入通用 shell 解析器，不新增必跑项。源码本批不修。

**本席票：001 工程缺陷是，修复队列是。** 此即对 B4 席乙「001 已按表修好」的更正；B4 raw 原文保留。

### 争议二：GR-M008-B4-OBS-01 归 007 还是 008（独立审查；不因 B4 席乙 PASS 跳过）

**B4 席乙原文（保留，不改写）：**

> 正式 007 钉的是 :126 活跃「实测」句；:160 是另一套「取证为」句，B1/B2 放在 008/M436 入口资料，本轮明确不回填正本。扩正则会超出定稿六项。工程缺陷：否（本批）。修复队列：否。

**冻结/原裁定原句检索（本文件 B1/B2 前缀 + 定稿 007/008 表；找不到「160 漏匹配已裁定 008 无缺陷」）：**

1. B1 GR-M008-001：「脚本头声称改『本轮/当前事实基线/取证』快照行。」证据块举现行 L126「实测…V385；无 surefire」。处置建议：「应修正则对准现行 L126 句，或停止声称维护『当前事实基线』。」——这是当时 0 命中句的举证，**不是**「职责永久限成 126」。
2. B2-r1/r2 007 处置原文：「修生成器对**当前活跃快照句**的匹配，保留历史变更句；本阶段只改工具及最小验证，**不改 M435 README 或 M436 入口正本**。」位置列举 126，处置未写「仅 126」。
3. B2-r1/r2 008：「生成器 README 自动块/**其它有效快照行**的 check-write-check 正常；真实 README …属 M435，**入口资料属 M436**；未承诺自动版本管理。**007 单列漏匹配**。」——「入口资料属 M436」是正本文件模块归属；「其它有效快照行」指当时正则已经吃到、check-write-check 正常的行。没有一句把「未匹配的 160 取证句」判成 008 无缺陷。
4. 工具头现行第 7 行：「只改带『本轮/当前事实基线/**取证**』快照语义的行与 README baseline:auto 标记块；历史变更记录（§changelog 旧快照数字）不动。」

**本席本批事实（隔离副本，正本未写）：**

- 正本 126：`本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 … surefire-reports`。
- 正本 160：位于 `## 2.3 … 本会话新功能落地登记`，HTML 注释写「刷新事实基线」。正文：`本轮 2026-09-01 **取证为** Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告`。下一小节才是「历史新功能已落地」。160 不是 changelog 旧数字。
- 现行 refresh 有「实测」正则，**无**「取证为」正则（旧「取证 Flyway…现有报告 N tests」已在 B3 被替换掉）。
- 隔离复制真实入口/README/OpenAPI，按真实 SQL 文件名建 377 个迁移（最新 V399），surefire tests 属性合计 55：`--check` exit 1 报入口+README → 默认写 exit 0「已回填 … V399 / 55 tests」→ `--check` exit 0「快照与实际一致」。写后 **126 变为 V399/55 tests**；**160 仍 V385 / 无 surefire / 2026-09-01**。正本入口/README SHA 未变。
- 声称「一致」时，职责范围内另一条「本轮/取证」快照句仍旧。这与原 007（126 未匹配时 check 不报入口、写后对 126 假绿）同一生成器漏匹配根因，只是 B3 把 126 补上之后暴露 160。

**两层（同意候选，纠正 B4 席乙把两层压成一层）：**

- 层 1 工具：生成器声称维护「本轮/当前/取证」活跃快照句，却漏 160，写后 check 绿 → **007**，工程缺陷是，队列是。本阶段只改工具及最小验证。
- 层 2 正本文件：00-entry 属 M436、README 属 M435；本轮 **仍不回填正本、不动历史句**。这是 008 的资料漂移登记，不是把工具假绿搬到 M436。

扩 007 匹配范围没有超出原裁定「当前活跃快照句」，也没有新判据/新 ID。B4「扩正则会超出定稿六项」是当时按 V1.12 表做内容审计的范围约束；本批 B2 在合取 FAIL 回退之后，用原 007 语言把未修完的同根因收进同一 ID，不是 B4 当时越权扩队列。

**本席票：OBS-01 并入 007，工程缺陷是，修复队列是；008 仍否。** 此为对 B4 席乙「160 仅登记 008」的更正；B4 raw 原文保留。找不到可引用的冻结句来维持原异议。

### 005 / 008–016（仅登记，不准删 005）

- 005：现码 `requiredDeliveryCommands` 字面 50、去重 44、额外重复 6。B4 diff 未改该数组。双方事实一致。队列否。B3 不得删。不外推「所有工程缺陷只能影响红绿」。
- 008：见争议二。不加自动版本管理。
- 009：B4 现行 anchors 6 红（BSM-E0433/E0736 正文；BSM-0283 source/preconditions/state_transition；BSM-0530 source）与 ledger-sync 台账未同步红，正确拒绝。归 M425–M428/M427。不豁免求绿。
- 010：10 份 .log 未改。
- 011：不另立测试工程。B4 71 PASS 不代替 001/007 未闭合证据。
- 012：文件级追加 ≠ 003 删除。三 HTML 扫描已有本 ID。
- 013–016：排除策略/CLI/UTC/PGM-036 边界不变，仅登记原事实。

### 分类 / 队列 / 允许的最小修复边界

全批 001–016 分类技术性成立。本席队列：**001–004、006–007 是（6 项）；005、008–016 否（10 项）**。与本批候选一致。

若 Codex 本批亦 PASS，下一次 B3 只修当时仍为「是」的 6 个 ID，并按各 ID 归因最小可失败测试（GEN-003）。002–004/006 虽 B4 内容通过，因整批未双 PASS **不得出队**，也 **不要求** 对已按表修好的块做无意义再编辑；下一轮 B3 仅补未闭合行为与回归。允许边界：

- 001：识别已有命令实际执行位置；禁 echo/注释/相似名冒充；**区分行内空白与命令换行，保留 && 后合法续行**；不新增门禁项目；不引入通用 shell 解析器。
- 002：必查 `entry_body` 为空则失败；不重造 CSV 框架。（B4 已体现的修复可保留）
- 003：已同步至少要求台账文件仍存在；保持文件级同批，不加语义审查。≠012。
- 004：按逻辑句/块识别；排版不改变结论；保留精确数值豁免与棘轮；不改业务 HTML。
- 006：两脚本 Git 保持 `execFileSync` argv，含 `git show`；合法基线保持。
- 007：生成器匹配**当前活跃快照句（明确含 126「实测」与 160「取证为」）**；保留历史句；最小验证；**不改 README/00-entry 正本**。

禁止：改三份业务 HTML；改 005/008–016（含不得删 005 的 6 个重复常量）；锁外新判据；新强制门；第三主体；把模块通过理解成全链绿；本轮改 M435/M436/M425–M428/M427 正本资料。全部改动文件自身版本递增。

无添加项，不写 questions。无本批最小修订要求（B2 不修源）。

### 本票命令（主仓只读；写/桩仅已删除的 .scratch 副本）

- 锁/候选/两 B4 raw/本文件前缀 SHA256：见上，全等。
- `requiredDeliveryCommands`：50/44/6。
- 点读：`npmCommands` `\\s`；refresh 第 7 行职责 + 「实测」正则、无「取证为」；入口 126/160 现文；§2.3 注释「刷新事实基线」。
- 只读 `node -e`：`npm\\nrun …` 对 helper 同类正则命中。
- 隔离 001：两入口 × 四变体，结果见争议一。
- 隔离 007：check1 → 写0 → check0；126 更新、160 仍旧；正本 SHA 未变。
- 未跑：主仓 refresh 默认写、`--write-baseline`、全仓 `verify:predeploy`、真实 npm 全链、安装 hook。未重跑 002–004/006 已由 B4 双席内容通过的反例。未跑三 HTML `--write-baseline`（012 已有 ID，仅登记原事实）。

工作区 `docs/business-system-map-for-operations-v0.1.html` 相对 HEAD 仍脏，属既有父链资料问题（009），本模块不修、不重开 M001/M002。

### 结论

候选把 B4 全部意见并入原 16 ID：001 换行未闭合仍是；007 吸收 OBS-01 仍是；002–004/006 因整批 B4 未双 PASS 留队且不要求无意义再编辑；005/008–016 否，005 不准删。无漏并、无新 ID、无新判据、无添加项。B4 席乙对 001「已修好」与 160「归 008」两处按本批新证据更正，原 B4 raw 保留。

两项争议本席票：① 001 是/是；② OBS-01 并入 007 是/是，008 仍否。找不到「160 漏匹配已裁定 008 无缺陷」的冻结原句。

本席 B2-r3 **PASS**。未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M008 `IN_PROGRESS`，B3=1。当前真实父链红保留。
