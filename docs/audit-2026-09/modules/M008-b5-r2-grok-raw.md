CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M008 B5-r2 Grok 乙席完整审计报告

- 文件版本：V0.01（指定路径原先不存在，本次新建）
- 席位：固定 Grok 外审席乙（grok-4.6 xhigh；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r2（B3 累计 2 之后的修复后整模块全量三线审计；不可用 B4-r2 PASS、B3 自报或历史 B1 替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04（471 模块）；`runbook.md` V0.02；控制提示词 `160-audit-prompt.md` V0.17
- 模块：M008 — 治理门禁与 CI 基线／检查及生成工具／第1段
- 模块生命周期（台账上下文，非本席改写）：`IN_PROGRESS`；B3 累计 2/3；LOCK-AMEND-COUNT 1
- 判据锁：`criteria-lock.md` V0.11；本席独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；旁路 `plan/criteria-lock.sha256` 同行
- 映射：`M008 → [GEN-001 … GEN-018, PGM-036]`，共 19 条；定义与 HTML 出处均从锁自读，不新造判据
- 本席约束：只读业务代码与计划；不修源/三 HTML/正本资料/findings/ledger/锁；不执行真实 Git hooks 安装、`--write-baseline`、refresh 默认写、push、开 PR、合并、部署
- 未读取：`M008-b5-r2-self-raw`（当前亦不存在）。B4-r2 raw 只作修复线索，不作本 B5 通过依据
- 技能（V0.17 收窄，按实际存在与已读执行适用意图；未核到的不称已调用）：
  - 已读并按意图使用：`using-superpowers`、`using-arc`、Arc `audit`（机械核对 + 三线 + 证据；**不**派评审子代理、**不**写 `docs/arc/audits/`、**不**向操作员提问）、`ponytail` / `ponytail-review`（只找可删，不改源）、`verification-before-completion`、`grilling`（自问自答）、`grill-me`（存在但 `disable-model-invocation`，只借 grilling 追问结构）、`requesting-code-review` / `receiving-code-review`（本席自行审完整模块，禁止 IMPLEMENT）、`test-driven-development` / `tdd`（只核已落测试红绿，不写新测试）、`diagnosing-bugs` / `systematic-debugging`（读错误→复现→追数据流；不插桩、不改生产代码）、`codebase-design`（CLI 缝与深度词汇）、`writing-for-agents`（本 raw 给下游席读）
  - 缺口：`batch-grill-me` 本环境技能树中不存在。`git-guardrails-claude-code` 存在，本体要装 hook，本席只读不安装。阶段 A 的 `brainstorming` / `writing-plans` 不适用 B5。`resolving-merge-conflicts` 无冲突未用。划掉的 `dispatching-parallel-agents` / `using-git-worktrees` / `research` 等不调用
- 既有台账只作上下文：findings V1.15、ledger V1.98。本席不把那些票当成本席事实；下列行为均重新实读与实跑

本次审计结论：**PASS**。无新增工程缺陷、无添加项、无待问、无锁外判据。001–004/006–007 修复后行为独立复验成立；005/008–016 维持仅登记、队列否。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。

---

## 1. 范围核对（来自 modules.md 与 criteria-lock.md，不是印象）

`plan/modules.md` M008 FILES-BEGIN 冻结 17 文件；计划有效内容行数 1530（不改计划凑数）。本席 `git ls-files` 口径 `cat | wc -l` 实算当前 1605 行，与指令给定一致。

1. `tools/html-param-tier-baseline.json`
2. `tools/refresh-entry-doc-baseline.mjs`
3. `tools/verify-delivery-gates.mjs`
4. `tools/verify-entry-anchors.mjs`
5. `tools/verify-html-ledger-sync.mjs`
6. `tools/verify-html-param-tier.mjs`
7. `tools/gate-hardening-tests/99-predeploy-final.log`
8. `tools/gate-hardening-tests/A-worktree-uncommitted.log`
9. `tools/gate-hardening-tests/B-committed.log`
10. `tools/gate-hardening-tests/C-no-baseline.log`
11. `tools/gate-hardening-tests/D-baseline-legacy-pass.log`
12. `tools/gate-hardening-tests/E-ignore-marker.log`
13. `tools/gate-hardening-tests/F-ci-baseline-evidence.log`
14. `tools/gate-hardening-tests/G-merge-pr-guards.log`
15. `tools/gate-hardening-tests/I1-baseline-ratchet.log`
16. `tools/gate-hardening-tests/I2-ignore-granularity.log`
17. `tools/gate-hardening-tests/m001-regression.mjs`

`criteria-lock.md` 映射 19 条：GEN-001–018、PGM-036。PGM-036 锁内 HTML 原文与 `docs/platform-governance-map-for-operations-v0.1.html:611` 现文一致（h2「设备管理：谁领了什么设备，设备能不能用」+ lead「设备要绑定员工、班次、点位和状态。」）。

三 HTML 工作区 SHA（本席独立）：

| 文件 | SHA256 |
|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

ENUM / PGM 工作区相对 HEAD 干净。BSM 工作区相对 HEAD 已脏：`git diff --stat` 为 8 行、4 插入/4 删除（版本头 V0.11→V0.13、导航锚文案、物业工单回访句、页脚日期）。决策台账 `docs/prd-audit/business-decision-ledger.md` 未同步。本席**未**改三 HTML；该脏状态是本轮门禁实跑输入，见 §5.009。

---

## 2. 源指纹核对（独立 sha256sum，与指令给定值对照）

正本：`HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c`。`git status -sb` 显示工作区大量已修改（含本模块 6 个 mjs 的 B3 修复未提交，属主工作区就地修复约定）。本席核对的是工作区当前字节，不是 HEAD blob。

| 文件 | 版本头 | 行 | 字节 | 本席 SHA256 | 与指令 |
|---|---|---:|---:|---|---|
| tools/html-param-tier-baseline.json | （JSON，无版本头） | 10 | 596 | 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8 | 一致 |
| tools/refresh-entry-doc-baseline.mjs | V0.02 | 126 | 7807 | 8bd1ba5ac141ef47dbcd409616fcacc3303546b06cceba998852895fac4f8cbb | 一致 |
| tools/verify-delivery-gates.mjs | V0.04 | 333 | 16444 | cbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f | 一致 |
| tools/verify-entry-anchors.mjs | V0.03 | 275 | 16186 | 7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82 | 一致 |
| tools/verify-html-ledger-sync.mjs | V0.02 | 68 | 3894 | 1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d | 一致 |
| tools/verify-html-param-tier.mjs | V0.02 | 199 | 11193 | c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9 | 一致 |
| tools/gate-hardening-tests/99-predeploy-final.log | （历史 log） | 138 | 7027 | 61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421 | 一致 |
| tools/gate-hardening-tests/A-worktree-uncommitted.log | （历史 log） | 42 | 5776 | 14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4 | 一致 |
| tools/gate-hardening-tests/B-committed.log | （历史 log） | 17 | 1692 | 3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339 | 一致 |
| tools/gate-hardening-tests/C-no-baseline.log | （历史 log） | 39 | 2462 | 5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67 | 一致 |
| tools/gate-hardening-tests/D-baseline-legacy-pass.log | （历史 log） | 10 | 1100 | 7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164 | 一致 |
| tools/gate-hardening-tests/E-ignore-marker.log | （历史 log） | 5 | 803 | 0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633 | 一致 |
| tools/gate-hardening-tests/F-ci-baseline-evidence.log | （历史 log） | 7 | 926 | 5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f | 一致 |
| tools/gate-hardening-tests/G-merge-pr-guards.log | （历史 log） | 24 | 1108 | 59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae | 一致 |
| tools/gate-hardening-tests/I1-baseline-ratchet.log | （历史 log） | 13 | 1055 | fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483 | 一致 |
| tools/gate-hardening-tests/I2-ignore-granularity.log | （历史 log） | 21 | 4264 | 75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b | 一致 |
| tools/gate-hardening-tests/m001-regression.mjs | V0.07 | 278 | 17155 | c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6 | 一致 |

合计 `wc -l` 1605。10 份历史 log 316 行。ledger 385663 bytes SHA `932956a009eb60472cc0b4739eb5d05bb54dfe11e51e6995ea1f5486cacdc89d`；findings 427553 bytes SHA `3aff9c00f391e5288b158da9a99ce1201042ec7dcd5d68577e1963ee77b038d4`。审计结束后复算 17 文件/三 HTML/锁/台账 SHA 与上表全等。环境：git 2.43.0，node v24.20.0。`node --check` 六个 mjs 均退出 0。

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。

---

## 3. 每文件三线

### 3.1 `tools/html-param-tier-baseline.json`（10 行）

**第一性原理**

1. 为什么必须存在：param-tier 棘轮把「存量未标层级句」登记为可警告放行的闭集；没有这份文件，脚本无法区分存量与新违规。删掉它，三 HTML 里没有任何业务句做不到——它不实现设备/价格/审批，只服务门禁（2.1 技术性，无业务发明）。当前 `entries: []`：存量清单已空，新数值句必须当场标层级或带数值豁免。
2. 声称是否真执行：`JSON.parse` 成功；`entries` 确为数组且长度 0。param-tier 在 BSM 脏时全量重扫，输出「存量 0 处已登记放行」、exit 0（类型 2/4）。`--write-baseline` 本席未对正本执行。
3. 未经验证前提：历史 log D/A/I2 曾记录 6 或 9 条存量，与当前空集不同——那些是当时基线，不是当前事实（010）。

**顺从式**：GEN-001 空数组不是死代码，是棘轮终态。GEN-006/011 稳定键约定写在 `_说明`。PGM-036 无设备字段。

**对抗式**：手工往 entries 加 key 会被棘轮与 merge-base 比较拦住（回归「基线新增 key」PASS；I1 log 为旧证据）。本席未改正本基线。

分类：技术性；无业务发明；无 HTML 冲突。

### 3.2 `tools/refresh-entry-doc-baseline.mjs`（126 行，V0.02）

**第一性原理**

1. 为什么必须存在：00-entry / README 里的 operation/迁移/测试快照会过期。删掉生成器，三 HTML 业务规则仍在；它只回填工程取证句（技术性）。
2. 声称是否真执行（隔离副本，正本 SHA 审计前后均为 entry `34e9c9f76578b61eb659dc4e369b274f62adb110b5f4dd3895bc0bb08f83e314`、README `28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5`）：
   - 正本 `node tools/refresh-entry-doc-baseline.mjs --check` → exit 1，`DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）`。
   - 隔离复制真实入口/README/OpenAPI/迁移文件名/surefire 头后 write exit 0：`已回填 … 720 ops / V399（377 迁移） / 55 tests（2026-09-09）`；随后 `--check` exit 0。
   - 隔离后第 126 行现为「本轮 2026-09-09 实测 OpenAPI 720 operations、Flyway 最新 V399；后端 surefire 现有报告 55 tests。」；第 160 行现为「本轮 2026-09-09 取证为 Flyway 最新 V399、OpenAPI 720 operations；后端 surefire 现有报告 55 tests。」（007 两处当前快照均匹配）。
   - 无 surefire 目录的第二隔离副本：126/160 回填 V399/720 并**保留**「当前工作树无 … 不声明当前测试数量」陈述。
3. 未经验证前提：正本 00-entry/README 仍是旧快照（V385/720 字面与当前 V399/55 不一致）——这是输入资料漂移（008/M435/M436），不是生成器漏匹配。日期用 `toISOString` UTC，本机本地日 2026-09-08、UTC 2026-09-09（015，仅登记）。

**顺从式**：GEN-002/009/016 隔离 write→check 闭环成立。GEN-001 不把自动版本管理做成新功能（008）。正则只改快照语义行，changelog「2026-06-23 … 386→419」仍在。

**对抗式**：回归 M008-007 三条 PASS（回填、回填后校验、无报告仍回填且保留「历史记录：V385 / 1606 tests」）。故意把断言改成 IMPOSSIBLE 后，回填本身仍写出正确 2/V12/3，随后裸 `assert.match` 抛错、exit 1（见 011）。

分类：技术性；无业务发明。007 已修；008 资料漂移正确报红。

### 3.3 `tools/verify-delivery-gates.mjs`（333 行，V0.04）

**第一性原理**

1. 为什么必须存在：把 delivery/predeploy 脚本声明、文档标记、CI 双份字节一致、部署 REQUIRED_GATES 收到机器闸。删掉则只剩人口头对 package.json。无 HTML 业务锚点 → 技术性。
2. 声称是否真执行：正本 `node tools/verify-delivery-gates.mjs` → `DELIVERY_GATES_DOCS_PASS` exit 0。`npmCommands` 现收窄：`split(/[ \t]*&&[ \t\n]*/)` 后每段必须 `^npm[ \t]+run[ \t]+[\w:-]+$` 且**不含换行**。本席用真实 `verify:predeploy` 串复刻：合法链解析出含三 HTML 门禁；`npm\nrun verify:html-param-tier` 与 `&&` 前换行得到空集合（会红）；`&&` 后换行仍含 `verify:html-param-tier`（合法续行）。工作区 `.github/workflows/predeploy-gates.yml` 与 `ops/ci/predeploy-gates.yml` LF 归一化后字节相等（1335）。unique 44 条 required 命令在 `package.json` 的 `verify:delivery-gates` 中均有 `npm run …`，missing=[]。
3. 未经验证前提：本席未跑完整 `npm run verify:delivery-gates` 重链（含后端单测等，超出本模块 17 文件且会碰测试库）。本文件只检查声明与文档标记。99.log 旧全链绿不是本次绿（010）。

**顺从式**：GEN-002/004/008/013：不能用 echo/相似名/注释冒充 npm run。GEN-001：`requiredDeliveryCommands` 仍 50 项 44 唯一、6 次额外重复（005，仅登记，不重开精简）。

**对抗式**：回归 14 条 M008-001（两入口 × 打印/相似名/整串打印/注释/命令内换行/&&前换行 → 1；&&后续行 → 0）全部 PASS。CI 分叉/缺 fetch-depth/缺事件基线/注释冒充/未跑 predeploy/部署缺三门禁均 1。

分类：技术性；无业务发明。001 已修；005 不重开。

### 3.4 `tools/verify-entry-anchors.mjs`（275 行，V0.03）

**第一性原理**

1. 为什么必须存在：三 HTML 改了而注册表/账本/矩阵不跟，引用会漂。删掉则回到人眼对 CSV。上位是「三 HTML 为最终解释权」的工程保护，不是 PGM-036 设备台账。
2. 声称是否真执行：正本 `node tools/verify-entry-anchors.mjs` exit 1，6 处：
   - 注册表正文找不到：`BSM-E0433`「处理后回访客人或商户，验收合格才关闭」；`BSM-E0736`「文档版本」
   - 账本 source_text 对不上：`BSM-0283` 同上回访句；`BSM-0530`「文档版本」
   - 账本镜像列：`BSM-0283` preconditions / state_transition 片段同一回访句
   统计行：`注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 360 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。`
   空 `entry_body`：`if (!entryBody || !flat[code].includes(entryBody))` 报错；回归 `M008-002 空条目正文` PASS。
3. 未经验证前提：这 6 红来自工作区 BSM 已改、registry/authority 未重建，属 M425–M428 资料，不是本工具故障（009）。EXCLUDE 含 `-report` 等，见 013。

**顺从式**：GEN-002/004/009/016：空正文不再跳过。检查①–⑦ 仍在。trigger 列整列豁免、阈值 12、哨兵 NOT_* 仍是既有口径。

**对抗式**：回归 CSV 引号逗号跨行/列数/未闭合引号/BOM+CRLF PASS。本席未为全链绿改 CSV 或 HTML。

分类：技术性；无业务发明。002 已修；009 正确拒绝。

### 3.5 `tools/verify-html-ledger-sync.mjs`（68 行，V0.02）

**第一性原理**

1. 为什么必须存在：用户 2026-08-03 拍板「改 HTML 必须同批同步决策台账」。删掉则该拍板只写在病史注释里。文件级同批，不做自然语言语义审查（012）。
2. 声称是否真执行：正本 exit 1，`改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。` 已改 `docs/business-system-map-for-operations-v0.1.html`。Git 一律 `execFileSync("git", args)`。`HEAD;echo M008_BASE_ARG` 作为单一 argv → `fatal: Not a valid object name HEAD;echo M008_BASE_ARG`，exit 1，echo 未另起命令（错误文案里出现该字符串是因为把整段当 ref 名打印，不是注入成功）。`--diff-filter=d`（小写）排除删除：删除台账不算同步。
3. 未经验证前提：文件级同步接受台账任意新增行（012）。未改 HTML 时本门禁不扫 param 句。

**顺从式**：GEN-002/006/009/011：已同步至少要求台账仍存在。GEN-004/010：基线走 argv 数组。

**对抗式**（隔离 git 仓）：脏 HTML 无台账改 exit 1；写入台账行 exit 0；删除台账文件 exit 1。回归「M008-003 删除台账」「已提交删除不被未跟踪副本掩盖」PASS。缺失基线 / 注入基线均 1。

分类：技术性；无业务发明。003/006 已修；009 当前红是输入未同步。

### 3.6 `tools/verify-html-param-tier.mjs`（199 行，V0.02）

**第一性原理**

1. 为什么必须存在：把「具体数值必须在原句标层级」变成机器闸。删掉则三层口径只靠人记。扫描对象是三 HTML 数值句，不发明新业务参数。
2. 声称是否真执行：正本因 BSM 脏而全量重扫该文件，`PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）` exit 0。本席用与脚本相同的 VALUE/TIER/IGNORE/按 `p|li|tr|div|h1-6` 分块算法扫工作区与 HEAD 三 HTML：未标层级 0、裸豁免 0。`--write-baseline` 未对正本执行。Git 调用同样是 argv；`git show ${mergeBase}:${BASELINE}` 用 `execFileSync`+stdio 忽略 stderr。
3. 未经验证前提：未改动的 HTML 本门禁跳过（012 职责）。历史 E.log 裸豁免曾 PASS，现行裸豁免回归期望 1。

**顺从式**：GEN-002/008/009：逻辑块而非物理行；精确数值豁免；棘轮只缩不增。TIER 含 `(?<!不)可配`，「不可配」不能当已标注（回归「否定词」期望 1）。

**对抗式**：回归空豁免/部分豁免/明确豁免/否定词/法定/业务/换行标注/同行两段/段后豁免/豁免不扩到后段均 PASS。基线新增 key 1、保持空 entries 0。

分类：技术性；无业务发明。004/006 已修。

### 3.7 `tools/gate-hardening-tests/99-predeploy-final.log`（138 行）

**第一性原理**：当时一次 `verify:predeploy` 全链绿的运行记录，路径含 `/home/james/wt-gates/`。删掉不影响现行门禁。声称的「当时 PASS」只对那次环境成立。本次正本 anchors/ledger-sync 为红，不得把本 log 当本轮绿（010）。**顺从式**：GEN-016 文档/证据可验证性——它是历史，不是当前声明。**对抗式**：log 内 entry-anchors 统计为注册表 1210/账本 1225，与本次 1228/1235 不同。分类：技术性历史证据；仅登记 010。

### 3.8 `A-worktree-uncommitted.log`（42 行）

当时工作区塞假句「到期前 30 天…3 次」、存量警告 6 条、新句红。旧脚本对照假绿。现行脚本在隔离回归中仍对未提交错误 exit 1。存量 6 条与当前空基线不同。分类：历史证据；010。

### 3.9 `B-committed.log`（17 行）

已提交假改动、台账未提交 → ledger-sync 红、param-tier 红。与现行「提交区也能看见 HTML 改动」一致。分类：历史证据；010。

### 3.10 `C-no-baseline.log`（39 行）

旧脚本取不到基线 skip exit 0；新脚本 exit 1。与现行 `merge-base` 失败即 1 一致。分类：历史证据；010。

### 3.11 `D-baseline-legacy-pass.log`（10 行）

命中当时 6 条存量基线则警告放行。当前 entries 空，该 6 条不再是现行基线。分类：历史证据；010。

### 3.12 `E-ignore-marker.log`（5 行）

当时裸 `param-tier:ignore` 整行放行 exit 0。现行回归「空豁免」期望 1，且错误文案要求改成带数值豁免。本 log 证明旧行为，不能覆盖现行。分类：历史证据；010。

### 3.13 `F-ci-baseline-evidence.log`（7 行）

CI run 31145223559 checkout 看到 origin/main，故「无基线转 exit 1」不会把当时 CI 误红。取证对象是历史 run，不是本次。分类：历史证据；010。

### 3.14 `G-merge-pr-guards.log`（24 行）

`merge-pr.sh` 守卫路径（非法方法/在 main/脏工作区/未 push）的历史实测。该脚本属 M007 文件，本模块只存 log。回归仍用假 git/gh 覆盖 stable/watch/late，未对真实 PR/merge/push。分类：历史证据 + 回归旁路；010。

### 3.15 `I1-baseline-ratchet.log`（13 行）

对照当时基线 commit 828796f5 的 9 条 entries：手工加 key 红、删 key 绿。当前基线 0 条，数字不可当现行。棘轮逻辑仍由回归「基线新增 key」覆盖。分类：历史证据；010。

### 3.16 `I2-ignore-granularity.log`（21 行）

带全量数值豁免绿、只列 30 天留下 3 次红。与现行「明确豁免 / 部分豁免」回归一致。存量 6 条警告是当时基线。分类：历史证据；010。

### 3.17 `tools/gate-hardening-tests/m001-regression.mjs`（278 行，V0.07）

**第一性原理**

1. 为什么必须存在：把 001–004/006–007 的可失败反例固定成可重复命令。删掉则修复只能靠下一次手工探针。不挂 npm 入口（011 仅登记）。
2. 声称是否真执行：`node tools/gate-hardening-tests/m001-regression.mjs` → 77 条 `PASS …` + `M001_GOVERNANCE_REGRESSION_PASS`，exit 0。输入与假 merge 均在 `os.tmpdir()/m001-gates-*`，finally `rmSync` 且断言路径仍在 tmp 下。本席未对真实 PR/merge/push。
3. 未经验证前提：007 之后的内容断言是裸 `assert.match`，失败会抛出、跳过后续 `expectExit`（回填后校验、无报告仍回填）。独立副本把期望改成 IMPOSSIBLE：前 75 条 PASS，随后 AssertionError，后两条 007 未打印，exit 1。错误可见、能失败，after 绿路径 77/77。此为 GR-M008-B4-R2-OBS-01 / 011，不升为必须改汇总风格的缺陷。

**顺从式**：GEN-003 关键修复分支有能失败断言。GEN-013 不另立测试工程。clone 用工作区脚本 + HEAD 仓，验证的是当前 mjs。

**对抗式**：见各 ID 回归名。合并测只碰假 git/gh。ponytail-review：测试体量服务已定缺陷，不是多余抽象。

分类：技术性；无业务发明。011 仅登记。

---

## 4. 十九冻结判据逐项

| ID | 锁内定义（自读摘要） | HTML 出处 | 本模块实现面 | 本席结论 |
|---|---|---|---|---|
| GEN-001 | 必要性与最小性 | 通用 | 17 文件均有可验证用途；005 重复常量 50/44 仍在、不影响红绿；010 历史 log 不删 | 适用。005/010 仅登记，不重开精简、不删 log |
| GEN-002 | 可执行正确性 | 通用 | 六脚本 `node --check` 0；delivery 正本 PASS；ledger/anchors 正本按脏输入正确红；param 正本 PASS；refresh --check 正本 DRIFT；回归 77 PASS | 适用且满足。009/008 的红是输入，不是脚本假绿 |
| GEN-003 | 测试有效性 | 通用 | 回归执行真实脚本；011 证明断言能红；after 77 绿 | 适用。011 仅登记汇总风格 |
| GEN-004 | 信任边界 | 通用 | 基线 ref 走 argv；CSV 校验列数/引号；npm 命令必须是 npm run 形态 | 适用。006 注入 exit 1 |
| GEN-005 | 授权与最小权限 | 通用 | 本 17 文件无主体/对象/动作授权面，无登录、无权限码、无界面隐藏替代服务端授权 | **无对应实现面**。不因此删除工具，也不发明授权模型 |
| GEN-006 | 数据完整性 | 通用 | 台账删除≠同步；棘轮 key 只缩不增；CSV 行与矩阵一一对应 | 适用。003/棘轮已验 |
| GEN-007 | 并发与幂等 | 通用 | 本地 CLI，无重试窗口、无多写者协议；`--write-baseline` 非默认可重入路径 | **无对应实现面**（首次正常跑不撞并发窗）。不为假想双进程加锁 |
| GEN-008 | 接口与模式一致 | 通用 | CI 双份 LF 归一化相等；predeploy 含三 HTML 门禁；部署脚本 REQUIRED_GATES 含三门禁 | 适用。正本 delivery PASS |
| GEN-009 | 错误处理 | 通用 | 无基线/注入/空正文/删除台账/非法 npm 均 exit 1 且有中文说明；不吞错 | 适用 |
| GEN-010 | 安全与敏感信息 | 通用 | argv 阻断 `;echo`；log/脚本无密钥。walk 不含 `docs/_archive` | 适用。006 已修 |
| GEN-011 | 可观测 | 通用 | PASS/FAIL 行可关联到范围与文件；历史 log 保留当时输出 | 适用。010 不把旧输出当本轮 |
| GEN-012 | 资源与性能 | 通用 | `walk(docs)` 有界；git maxBuffer 32MB；回归临时目录有上限清理 | 适用，未见无界放大。不为极端仓体加新闸 |
| GEN-013 | 构建配置与依赖 | 通用 | 只依赖 node/git/yaml；`yaml` 已在仓内；失败显式。回归不挂 npm | 适用。011 不强制 npm 入口 |
| GEN-014 | 数据库迁移正确性 | 通用 | 无 Flyway/SQL。refresh 只**统计**迁移文件名 | **无对应实现面**。不审 V 号顺序（那是其它模块的 verify:migration-version-order） |
| GEN-015 | 界面可用性与语言 | 通用 | 无产品 UI。CLI 标识 DELIVERY_GATES_* / PASS/FAIL/DRIFT 为内部工程标识 | 适用为「无产品界面」。014：不翻译 CLI；B/C 原意不延伸为 CLI 授权 |
| GEN-016 | 文档可验证 | 通用 | 历史 log≠当前；00-entry 正本漂移被 --check 抓住；隔离 write 后 check 0 | 适用。008 资料模块回填，不改本模块正本 |
| GEN-017 | 四分类 | 通用 | 17 文件均技术性；无业务发明伪装；不因 HTML 沉默删除门禁 | 适用。本席无添加项 |
| GEN-018 | 第一性原理证据 | 通用 | 本票结论落到 HTML 原文、脚本输出、git 规范行为、可复现命令；不以 B4 PASS 替代 | 适用 |
| PGM-036 | 设备管理：谁领了什么设备，设备能不能用 | `platform-governance-map-for-operations-v0.1.html:611` h2；原文 lead「设备要绑定员工、班次、点位和状态。」 | 17 文件无设备台账、领用、点位、班次绑定、工作手机白名单。`verify:html-*` 的「gate」与现场闸机同名不等同 | **无对应业务实现面**。016 仅登记。不把 CI gate 解释成设备模块，不新增设备判据 |

---

## 5. 既有 16 项与本轮观察（独立复验，不复述）

判定顺序：冲突 → 细化 → 技术性 → 添加。下列均为**技术性**。工程缺陷列按 findings V1.15 顶部表；当前修复队列全否。本席复验后不新增永久 ID，不重开已闭环处置。

| ID | 本席复验 | 工程缺陷 | 队列 | 依据 |
|---|---|---|---|---|
| M008-001 | `npmCommands` 已拒命令内 LF 与 `&&` 前 LF，保留 `&&` 后 LF；回归 14 条 + 复刻真实 predeploy 串 | 是（已修） | 否 | GEN-002/004/008/009/013；类型 2/4 |
| M008-002 | 空 `entry_body` 报「注册表条目正文在 HTML 现文中找不到」；回归 PASS | 是（已修） | 否 | GEN-002/004/009/016；2/4 |
| M008-003 | 隔离：改 HTML 无台账 1；有台账行 0；删除台账文件 1 | 是（已修） | 否 | GEN-002/006/009/011；2/4 |
| M008-004 | 回归换行标注 0、同行两段 1、段后豁免 0、豁免不扩后段 1 | 是（已修） | 否 | GEN-002/008/009/016；2/4 |
| M008-005 | 源码仍 50 项/44 唯一/6 额外重复；缺命令仍会 exit 1，有命令 delivery PASS。不重开可选精简 | 否（仅登记） | 否 | GEN-001/011；2/4 |
| M008-006 | 两脚本 Git 全部 argv；`HEAD;echo …` 与 env 同等 exit 1，git 报「Not a valid object name」整段 | 是（已修） | 否 | GEN-002/004/009/010/018；2/4 |
| M008-007 | 隔离真实入口：write 同时改 126 实测句与 160 取证句为 V399/720/55；check 0；无 surefire 保留「不声明测试数量」；正本未写 | 是（已修） | 否 | GEN-002/009/016；2/4 |
| M008-008 | 正本 `--check` 仍 DRIFT 入口+README；生成器在隔离中能回填。属 M435/M436 资料。无自动版本管理新功能 | 否 | 否 | GEN-001/002/016 |
| M008-009 | 正本 anchors 恰 6 红（E0433/E0736/0283 source+preconditions+state_transition/0530）；ledger-sync 因 BSM 脏无台账 exit 1。工具正确拒绝。不为全链绿改 M425–M428/M435/M436 或三 HTML | 否（本模块源码） | 否 | GEN-002/008/016；2/4 |
| M008-010 | 10 份 log 已逐份读完。E 旧裸 ignore 放行≠现行；A/D/I2 旧 6/9 条≠当前空基线；99 旧全链绿≠本次 ledger/anchors 红。无非审计 docs 把它们写成现行承诺 | 否 | 否 | GEN-001/011/016 |
| M008-011 | 正本回归 77 PASS。独立副本打断 007 内容断言：75 PASS 后 AssertionError，后续 007 两条未跑，exit 1。能失败、after 绿、无报告支路另有回归第三条 | 否 | 否 | GEN-003/013/018 |
| M008-012 | 台账任意新增行即可过文件级同步（隔离已演示）。未改的 PGM/ENUM 本轮 param-tier 跳过；本席另用同算法全量扫三 HTML，未标层级/裸豁免均 0 | 否 | 否 | GEN-002/006/009 |
| M008-013 | EXCLUDE 命中 changelog/rounds/`-report`。抽查：现行 nas/r1 等 `*-report*.md` 0 条「」引用、0 条行号锚点；有行号锚点的排除文件落在 changelog/rounds/本审计 raw。无「误排导致错误通过」实证。不加新排除策略 | 否 | 否 | GEN-002/016 |
| M008-014 | CLI 英文标识不是产品 UI，也不是 PGM-036 设备。B/C 原文不扩大为 CLI 授权 | 否 | 否 | GEN-015/017 |
| M008-015 | `toISOString().slice(0,10)` 在本机给出 2026-09-09，本地日历 2026-09-08。工程取证日，非营业日。不增时区参数 | 否 | 否 | GEN-002/016 |
| M008-016 | PGM-036 原文已回读。17 文件无员工/班次/点位/设备状态实现 | 否 | 否 | PGM-036/GEN-017/018 |

本轮临时观察 **GR-M008-B5-R2-OBS-01**（`tools/gate-hardening-tests/m001-regression.mjs:261–270`）：与既有 011 / GR-M008-B4-R2-OBS-01 同根因。分类技术性；工程缺陷否；队列否；不新建永久 ID。四分类不是冲突/细化/添加。

无新增根因。无添加项。无 HTML 修改申请。

---

## 6. 真实命令与输出（判 PASS 前）

工作目录均为 `/home/james/complex-management`，除非注明隔离副本。

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
```

```
git rev-parse HEAD origin/main
242f487233d028173cb9198eecd079f5d877405c
242f487233d028173cb9198eecd079f5d877405c
```

```
node --check tools/verify-delivery-gates.mjs
node --check tools/verify-entry-anchors.mjs
node --check tools/verify-html-ledger-sync.mjs
node --check tools/verify-html-param-tier.mjs
node --check tools/refresh-entry-doc-baseline.mjs
node --check tools/gate-hardening-tests/m001-regression.mjs
# 六个均退出 0
```

```
node tools/gate-hardening-tests/m001-regression.mjs
# 77 行 PASS …（含 M008-001×14、M008-002、M008-003×2、M008-004×4、M008-006×4、M008-007×3 等）
M001_GOVERNANCE_REGRESSION_PASS
# exit 0
```

```
node tools/verify-delivery-gates.mjs
DELIVERY_GATES_DOCS_PASS
# exit 0
```

```
node tools/verify-html-param-tier.mjs
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872；扫描口径：改动文件全文，已剥除 <style> 与注释。
# exit 0
```

```
node tools/verify-html-ledger-sync.mjs
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
# exit 1
```

```
node tools/verify-entry-anchors.mjs
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 360 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] FAIL：6 处不合格。
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」——HTML 已改而注册表未重建
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」——HTML 已改而注册表未重建
  - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」——HTML 已改而账本 source_text 未重取
  - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」——HTML 已改而账本 source_text 未重取
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」preconditions 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」state_transition 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
# exit 1
```

```
node tools/refresh-entry-doc-baseline.mjs --check
[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。
# exit 1（正本未写）
```

隔离（已清理）：复制真实 00-entry/README 后 write exit 0、check exit 0，126/160 均为 V399/720/55；正本 SHA 不变。`HEAD;echo M008_BASE_ARG` 两脚本 exit 1。隔离删台账 exit 1。011 副本 exit 1、75 PASS 后 AssertionError。

未跑边界（不折算 PASS/FAIL）：完整 `npm run verify:delivery-gates` 重链、真实 `merge-pr`/`propose-change`/push/部署、正本 `--write-baseline`、正本 refresh 默认写、安装 hook。

---

## 7. ponytail-review（只列可删，不改源）

- `verify-delivery-gates.mjs` required 列表 6 处重复：`yagni:` 可删重复字面，不改变红绿。对应已定 005，**不重开**。
- 10 份 `.log`：`delete:` 会丢掉当时证据。010 已否。
- 回归 278 行：关键反例的最小固定，不是多余框架。`Lean already` 对修复闭包而言成立。

net: 不建议本 B5 删任何行。

---

## 8. 结论

- 17 文件全部整读并走第一性原理 / 顺从式 / 对抗式。
- 19 条冻结判据均有可复核结论；GEN-005/007/014 与 PGM-036 无对应实现面，已说明原因，不删工具、不加锁外门槛。
- 001–004/006–007 修复后行为本席独立复验成立。
- 005/008–016 维持仅登记、队列否；009 仍正确报红。
- 无新增工程缺陷，无添加项，无待问，无条件通过。
- 正本 17 文件 / 三 HTML / 锁 / findings / ledger 字节在本席进出前后不变。临时隔离已删。

**本次 B5-r2 席乙审计结论：PASS。**

本票不是双席合取，不是模块 PASS。模块仍 `IN_PROGRESS`。
