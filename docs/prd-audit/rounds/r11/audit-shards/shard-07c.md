# shard-07c · 仓外 scratch 未登记三类项复核

基线 `1c5c0c7d` ｜ 只读取证 ｜ 来源：本机 `D:\AI\ccwork\prd-audit-r8\`（从未进仓）

> 取证方法说明：`apps/web` 下 TSX 的 `TEXT` 常量块把中文存成 `\uXXXX` 转义，
> 而同文件的 `//` 注释是原文中文。**只按原文中文 grep 会漏掉全部用户可见串**，
> 本 shard 两种形态都跑了（见 EV-07c-04 的计数订正）。

---

## 类一 · 废词批 B 逐条实测

R10（2026-08-06）已对三个词出正式废止口径，各开发设计文档文首横幅均写
「**待代码轮下线**」。代码轮至今未做 —— 下列全部仍在。

### 1.1 人工放行（3 处转述，实测 3/3 仍在）

```
EV-07c-01 | code_ui_string | apps/web/src/admin/pages/SupervisorReviewPage.tsx:255 | @1c5c0c7d
关键节选：
  <option value="manual_release">人工放行</option>
（同 select 内并列 合同违规 / 基础定义违规 / 账单折扣，主管复核页动作类型下拉）
---
仍成立（用户可见串，非注释）
拟登记为 finding：并入既有「人工放行未下线」项，见 1.5 归属
severity: P1
```

```
EV-07c-02 | code_ui_string | apps/web/src/customer/i18n/statusLabels.ts:166 | @1c5c0c7d
关键节选：
  manual_release: "人工放行",
---
仍成立（用户可见串，非注释）
severity: P1
```

```
EV-07c-03 | db_seed | backend/src/main/resources/db/migration/V270__ticket_manual_override_permission.sql:10 | @1c5c0c7d
关键节选：
  ('verification.manual_override', '人工放行票务核销', 'ticketing', 'high', TRUE, 'active')
---
仍成立（权限种子，permission_name 会在权限管理页展示）
severity: P1
```

**转述漏计**：除上述 3 处「词」以外，**功能本体**也仍在，且不是废词问题而是口径冲突：
`TicketVerificationService.java:208/218/243`、`TicketingController.java:483`、
`TicketingService.java:918`、`dto/VerificationManualOverrideRequest.java:7`、
`V067__ticket_multiperson_reverse.sql:6` 均为 javadoc/注释，但它们描述的
`status=manual_overridden` 落库路径与端点是**真实可调用的**。
设计文档文首原话：「**系统不提供人工放行：核销要么成功、要么拒绝或失败**」。
→ 文档说"不提供"、代码真实提供，属**口径与实现相反**，比废词严重一档。

### 1.2 核销预检（转述 8 处，实测 8/8 仍在 + **漏计 4 处**）

```
EV-07c-04 | code_ui_string | apps/web/src/admin/pages/TicketOrdersPage.tsx:23,24,28,36,38,52 | @1c5c0c7d
关键节选（已解转义）：
  23 | description: "...权益和核销预检，用于定位未出票和异常核销。"
  24 | evidence:    "...；核销预检 POST ..."
  28 | entitlementRequired: "请填写权益码后再做核销预检。"
  36 | preview: "核销预检",
  38 | boundary: "本页不绕过支付状态；出票和核销预检均保留幂等键和审计事件。"
  52 | previewNote: "完整后台验收核销预检"
---
仍成立；且**转述只列了 36 / 52 两处，实为 6 处**，全部是用户可见串
（漏计原因即文首所述转义 grep 盲区）
severity: P1
```

```
EV-07c-05 | code_ui_string | apps/android-terminal/.../terminal/MainActivity.kt:290,293,297,301,302,304 | @1c5c0c7d
关键节选：
  290 | val status = statusText("先预检再核销")
  293 | val previewButton = button("预检") {
  297 | status.text = "正在预检…"
  301 | status.text = if (r.allowed) "预检通过：可核销（...）"
  302 | else "预检不通过：${r.reason.ifBlank { "denied" }}"
  304 | onError = { status.text = "预检失败：${friendly(it)}" },
---
仍成立（6/6，原文中文，用户可见）；两步流按钮本体仍在端上
severity: P1
```

废止依据（DEC-R6-08）：`docs/ticketing-pass-entitlement-development-design.md:212`
`preview_passed`「已废止…该取值停用、不得新写入，**待代码轮下线**」；
同文件 `:883` / `:929` 与 `docs/android-device-terminal-development-design.md:1213`
把 `POST .../verifications/preview` 标为已废止；`:1305` 把事件
`verification.previewed` 标为停用。**接口、事件、端上按钮三者都还活着。**

### 1.3 场次容量／三维度（转述 5~6 处，实测 6/6 仍在）

```
EV-07c-06 | code_ui_string | apps/web/src/admin/pages/AmusementSessionsPage.tsx:25 | @1c5c0c7d
关键节选：description: "管理游乐项目与场次容量，后续串联预约、核销和安全规则。"
---
仍成立（用户可见）
severity: P1
```

```
EV-07c-07 | code_ui_string | apps/web/src/admin/pages/AmusementVerificationRecordsPage.tsx:43-45 | @1c5c0c7d
关键节选：
  43 | point_device: "自由进出（并发在场）",
  44 | project: "项目容量",
  45 | session: "场次容量",
---
仍成立；此处是**三维度整组标签**，不止 :45 一行
severity: P1
```

```
EV-07c-08 | code_error_message | backend/.../amusement/AmusementBookingService.java:272 | @1c5c0c7d
关键节选：
  throw ApiException.badRequest("VALIDATION_FAILED", "预留容量不能超过场次容量");
---
仍成立（面向用户的报错文案）
severity: P1
```

```
EV-07c-09 | code_error_message | backend/.../amusement/AmusementService.java:1339 | @1c5c0c7d
关键节选：
  throw ApiException.conflict("SESSION_CAPACITY_EXHAUSTED",
          "场次容量已满（受限维度：" + claim.exhaustedDimension() + "）");
---
仍成立；错误码 SESSION_CAPACITY_EXHAUSTED 与「受限维度」措辞同为废止口径
severity: P1
```

```
EV-07c-10 | metric_definition | backend/.../analytics/MetricRegistry.java:376,379 | @1c5c0c7d
关键节选：
  376 | "游乐场次总容量",
  379 | "Σ场次容量=全部游乐场次 capacity 直加（与项目与场次页「总容量」卡一致）。..."
配套：apps/web/src/admin/pages/MetricDetailPage.tsx:32
  { key: "amusement.session_capacity", name: "游乐场次总容量", sourcePageId: "ADM-AMU-001", ... }
---
仍成立；指标 key + 中文名 + 可加口径三处一致地沿用废止口径
severity: P1（指标口径错 → BI 数字含义错，比纯文案高）
```

废止依据：`docs/amusement-operations-development-design.md:5`
「「场次容量／三维度容量」已废止 —— BSM「容量控制」条 明确
『**只按单个游乐项目的实时在玩人数控制容量，不再按场次…**』」；
同文 `:17`「三维度容量已落地（V083）（**口径已废止**…）」。
实现侧 `V083__amusement_three_dim_capacity.sql`、`CapacityClaimContext/Result`、
`AmusementRepository.java:174`「三维度容量取最小」仍是活代码路径。

### 1.4 对账「已忽略」（转述 2 处，实测 2/2 仍在 —— 但**不是废词，是标签撞车**）

```
EV-07c-11 | code_ui_string | apps/web/src/customer/i18n/statusLabels.ts:235-240 | @1c5c0c7d
关键节选：
  235 | // 对账 / 集成事件
  236 | processed: "已处理",
  237 | matched: "已匹配",
  238 | ignored: "已忽略",
  239 | // 风险预警处置
  240 | dismissed: "已忽略",
---
仍成立（2/2）
但性质与前三批不同：**未检索到任何「已忽略」废止口径**（docs/ 与三份 HTML 均无）。
真实问题是 `ignored`（对账差异）与 `dismissed`（风险预警处置）两个**不同业务状态
渲染成同一个中文词**，业务人员在界面上无法区分「这条差异不用管」和
「这条预警被驳回」。
severity: P2
```

### 1.5 类一小结：这批到底"做没做"

`docs/prd-audit/business-decision-ledger.md:1564`（R10 本轮落地自证）原文：

> **P1 深层残留清扫**：「人工放行」「保证金→押金」「场次容量→项目容量（实时在玩人数）」「核销预检两步流」…

同节 `:1559` 又写「**三份 HTML 零字节改动**」、`:1566`「P2 PRD 同步」。
→ R10 的"清扫"**只清了 docs/PRD**，代码一行未动，且各文档自己写的是
「待代码轮下线」。**台账把它记成了已落地的一项，读起来像已完成。**
这正是任务书说的"做了没登记"的镜像："登记了但只做了一半"。

---

## 类二 · 引文门禁未立项

### 2.1 `tools/` 下没有任何引文/条名内容校验脚本

```
EV-07c-12 | tree_listing | tools/ | @1c5c0c7d
关键节选（`git ls-tree --name-only 1c5c0c7d tools/`，共 81 项，全量已核）：
  按 /quote|cite|引文|引用|attribut/i 筛选 → 命中 0 个文件名
  唯一与「引用」沾边的是 tools/verify-entry-anchors.mjs（见 2.2）
  其余 verify-* 42 个全部是 openapi / production-evidence / nas / pii /
  tenant-isolation / migration-order 等无关域
---
仍成立（引文门禁确实从未立项）
拟登记为 finding：**需新建**（tools/ 树下无对应物，无法归入任何已有条目）
severity: P1
```

### 2.2 `verify-entry-anchors.mjs` 只校验「条名能否唯一定位」，不校验引文归属

脚本自述的六项检查（`tools/verify-entry-anchors.mjs:7-18` 文件头注释）：

```
EV-07c-13 | gate_source | tools/verify-entry-anchors.mjs:7-18 | @1c5c0c7d
关键节选：
  ① 注册表内条目名在同一份 HTML 内唯一；首句指纹能在 HTML 现文命中
  ② 全库 Markdown 里的 文件代号「名」 引用，必须能在该 HTML 内唯一定位
  ③ 正文里不得再出现「实测 LNNN」式行号锚点
  ④ 账本与承接矩阵两份台账的 entry_name 同名同源
  ⑤ 对已登记进账本的条目，账本 source_text 须逐字仍存在于 HTML 现文
  ⑥ 账本每行须在承接矩阵有对应行
---
仍成立
severity: P1
```

关键实现证据 —— 检查②的判定只吃尖括号内的**条名**，完全不看引用它的那句话：

```
EV-07c-14 | gate_source | tools/verify-entry-anchors.mjs:170,180-193 | @1c5c0c7d
关键节选：
  170 | const REF = /(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」/g;
  182 |     const name = m[2];
  184 |     if (nameSeen[code + "|" + name]) { refByName++; continue; }   // ← 命中即放行
  188 |     const hits = byCode[code].filter(r => hitAll(norm(r.entry_name)));
  190 |     if (textHits.length === 1) { refByText++; continue; }
  192 |     errors.push(`...${code}「${name}」在该 HTML 中定位不到任何条目`);
  193 |     errors.push(`...${code}「${name}」在该 HTML 中命中 N 条，无法唯一定位`);
---
仍成立
severity: P1
```

**两条错误分支只有「定位不到」和「定位到多条」两种**，没有第三种
「定位到了，但 PRD 就着这条名写的话和 HTML 该条实际内容相反」。
`m[2]` 之外的正文一个字都没读进来。

结论 —— 现有保护面与缺口：

| 门禁能挡 | 门禁挡不住 |
|---|---|
| 引用了不存在的条名 | 引用了存在的条名，但转述内容与该条相反 |
| 引用命中多条、有歧义 | 把 A 条的规则挂在 B 条名下（张冠李戴） |
| HTML 改动后账本 source_text 失配（仅**已登记**条目，检查⑤自述"未登记不在保护范围"） | 未登记条目的引文（R9 遗留 233 条未登记） |
| 行号锚点复辟 | 引文断章取义（只抄前半句、丢掉限定词/例外句） |

反例可直接由类一构造，且**当前就是绿的**：任何 PRD 只要写
`BSM「容量控制」…按场次容量控量`，条名 `容量控制` 唯一命中 → 检查②放行，
而该条 HTML 现文写的是「**不再按场次**」。门禁全绿、口径全反。

---

## 类三 · delivery-gates 整链 CI 从不跑

### 3.1 全仓只有一个 workflow，只跑一条 npm script

```
EV-07c-15 | ci_config | .github/workflows/predeploy-gates.yml | @1c5c0c7d
关键节选（`git ls-tree -r --name-only 1c5c0c7d .github/` 全量输出）：
  .github/CODEOWNERS
  .github/workflows/predeploy-gates.yml      ← workflow 仅此 1 个
文件内唯一的 run 步骤：
  23 |       - name: verify:predeploy (governance gates)
  24 |         run: npm run verify:predeploy
触发面：on.pull_request.branches=[main]；on.push.branches=[main]
---
仍成立
severity: P1
```

### 3.2 `verify:delivery-gates` 未被任何 workflow 引用

```
EV-07c-16 | ci_config | .github/ 全目录 | @1c5c0c7d
关键节选：
  $ git grep -nE 'delivery-gates|closed-loop' 1c5c0c7d -- .github/
  .github/workflows/predeploy-gates.yml:3:# delivery-gates 快子集(...)  ← 仅注释里提到
  （无任何 run: 行引用 verify:delivery-gates 或 closed-loop 系）
全库引用 verify:delivery-gates 的 20+ 处**全部在 README / AGENTS.md / docs/**，
无一处是可执行配置。
---
仍成立
severity: P1
```

### 3.3 两套闸口的成员差集：39 条子闸没有任何自动执行者

`package.json:61` 的 `verify:delivery-gates` 聚合 **49 条子闸**；
`package.json:96` 的 `verify:predeploy` 只有 **15 条**，两者交集 **10 条**
（delivery-gates-docs / full-admin-matrix / domain-ownership-scope /
matrix-vs-migration / data-isolation-spec / tenant-isolation / p0-contract /
security-baseline / production-readiness-evidence / production-blockers）。

```
EV-07c-17 | npm_script | package.json:61,96 | @1c5c0c7d
关键节选（差集中值得点名的）：
  仅在 delivery-gates、CI 与部署脚本都不跑：
    verify:migration-version-order      ← 迁移版本序（AGENTS.md:63 称"机器闸门"）
    verify:unit
    verify:final-closed-loop-audit
    verify:closed-loop-current-state
    verify:coding-host-protection / external-dependency-gates /
    integration-capability-switches / t3-coding-package / … 共 39 条
---
仍成立
severity: P1（含 migration-version-order，触及真实上线风险）
```

### 3.4 三条 closed-loop 门禁的实际执行者

| 门禁 | 在 delivery-gates | 在 predeploy | CI 跑 | 部署脚本跑 |
|---|---|---|---|---|
| `verify:final-closed-loop-audit` | 是（:61 第 37 项） | 否 | 否 | 否 |
| `verify:closed-loop-current-state` | 是（:61 第 39 项） | 否 | 否 | 否 |
| `verify:closed-loop-runtime` | **否** | 否 | 否 | 否 |

```
EV-07c-18 | npm_script | package.json:107 | @1c5c0c7d
关键节选：
  "verify:closed-loop-runtime": "npm run test:backend && npm run typecheck --workspace apps/web
     && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:re..."
全库引用处：README.md:93 / README.en.md:75 / docs/00-current-effective-document-entry-v0.1.md:65
  / docs/delivery-readiness-gates-v0.1.md:122 / docs/final-closed-loop-acceptance-audit-v0.1.md:104
  / docs/runtime-acceptance-evidence-latest.json:38（captured_command）
  / tools/verify-final-closed-loop-audit.mjs:122（只检"文档里有没有写这个名字"，不执行它）
---
仍成立：**它不在任何聚合闸口里**，只能靠人手敲
severity: P1
```

`tools/verify-final-closed-loop-audit.mjs:122,160` 的性质要点名：它把
`"verify:closed-loop-runtime"` 当**字符串**去 `requireRegex` 匹配验收文档正文，
校验的是「文档里写没写这条命令」，不是「这条命令跑没跑过、结果如何」。
而这个校验器自己又在 CI 之外。

### 3.5 定性修正：不是"疏忽漏配"，是"有意只跑子集 + 子集外无人接手"

workflow 文件头 `:2-5` 自述：

```
EV-07c-19 | ci_config | .github/workflows/predeploy-gates.yml:2-5 | @1c5c0c7d
关键节选：
  # 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
  # delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
  # 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
  # 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
---
部分成立
severity: P1
```

即：跑子集是**有意的**（无 DB、要快、私有库免费版设不了 required check）。
但文件里点名的两个"硬阻断"承担者 —— `ops/160/complex-deploy-api.sh`、
`ops/160/complex-deploy-web.sh` —— 经 `git grep -l 'verify:predeploy'` 确认，
**跑的也是 `verify:predeploy` 同一个 15 条子集**。
→ 三个执行点（CI / 部署脚本 / pre-push）跑的是同一份子集，
**delivery-gates 独有的 39 条在整个工程里没有任何自动执行者**，
而 README.md:84 与 docs/architecture-overview-v0.1.md:145 仍把它写作
「交付总闸口（约 50 子闸聚合）」。多份验收文档（如
`docs/closed-loop-live-acceptance-audit-20260618.md:13,27`）记的
「`verify:delivery-gates` exit 0 通过」是 2026-06-18 的**一次性人工跑**，
此后代码改了两个月，无任何机制复验。

---

## 建议新登记的最小集

### 必须新建 finding（3 条）

| 建议 ID | 标题 | 理由 | severity |
|---|---|---|---|
| **R11-07c-A** | 引文门禁缺位：条名可定位≠转述属实 | 类二。`tools/` 81 项无一物覆盖；`verify-entry-anchors.mjs` 检查②只吃 `m[2]` 条名、两条错误分支无"内容相反"一类。这是 R8~R10 反复出现的「PRD 转述与 HTML 相反」类问题的**唯一无门禁缺口**，且可构造当前就绿的反例（见类二末）。 | P1 |
| **R11-07c-B** | delivery-gates 独有 39 条子闸无任何自动执行者 | 类三。CI / 部署脚本 / pre-push 三点同跑 15 条子集；差集含 `verify:migration-version-order`（AGENTS.md 自称机器闸门）与三条 closed-loop。README 与架构文档仍称其为"交付总闸口"，与实际执行面不符 —— **文档承诺的保护面 ≈ 实际保护面的 3 倍**。 | P1 |
| **R11-07c-C** | 废词/口径核查漏 `\uXXXX` 转义串 | 方法论缺陷，非单点。scratch 把 `TicketOrdersPage.tsx` 的 6 处点成 2 处，根因是只按原文中文 grep。**凡后续轮次做"某词是否清干净"的核查，不双形态 grep 就必然低估**。此条不修具体文案，只订正核查方法并建议进门禁（若 R11-07c-A 落地，顺带覆盖）。 | P1 |

### 已有登记、不必重复（4 项）

| 项 | 已登记在哪 | 说明 |
|---|---|---|
| 「人工放行」代码未下线 | `docs/prd-audit/business-decision-ledger.md:1564`「P1 深层残留清扫」；各设计文档文首 R10 口径横幅 + 「待代码轮下线」标注 | **但登记状态误导**：同节 `:1559` 写"三份 HTML 零字节改动"、`:1566` 写"PRD 同步"，通读像已完成，实际代码零改动。建议**订正既有条目状态为 open/仅文档侧**，而非新建。 |
| 「核销预检」两步流未下线 | DEC-R6-08；`ticketing-pass-entitlement-development-design.md:212,883,929`、`android-device-terminal-development-design.md:1213,1305`、`openapi-p0.json:10954` 均已标"已废止·待代码轮下线" | 同上，登记齐备、执行未做。**不新建**，建议并入代码轮待办清单并给出本 shard 的 12 处实测坐标。 |
| 「场次容量／三维度」未下线 | `amusement-operations-development-design.md:5,17` R10 废止横幅；ledger:1564 | 同上。唯一需要升格提示的是 `MetricRegistry.java:376/379` + `MetricDetailPage.tsx:32` —— 指标口径错会直接让 BI 数字含义错，建议在既有条目下**单列为代码轮优先项**。 |
| 未登记条目不受门禁⑤保护（233 条） | R9-B/R8-U-02 已挂账，`verify-entry-anchors.mjs:15-16` 注释自认 | 已有登记，本 shard 只做旁证。 |

### 需要裁决、本 shard 不自裁（1 项）

- **`ignored` / `dismissed` 双键同标签「已忽略」**（EV-07c-11）：未检索到任何废止或合并口径，
  属新发现的界面歧义。两个状态分属对账差异与风险预警处置两条业务线，
  业务人员在界面上无法区分。建议登记为 **P2 待裁**（改标签需动用户可见文案，
  按铁律须先出草案），不建议本轮直接归入废词批。

---

## 本 shard 未覆盖（划界）

- 不做 `findings.csv` 提取、不碰 6 项 P0 —— 属 shard-07a / 07b。
- 类一各词的**功能下线方案**（接口/事件/权限码怎么退役）不在本 shard，
  本 shard 只出实测坐标与"做没做"的判定。
