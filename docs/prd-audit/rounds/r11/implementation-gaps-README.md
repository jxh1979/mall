# `implementation-gaps.csv` 读法说明

本文件是 `docs/prd-audit/rounds/r11/implementation-gaps.csv` 的伴随说明。

**为什么单独开一份 README，而不在 CSV 里加注释头**：`tools/verify-entry-anchors.mjs` 把 CSV 首行
**无条件当表头**。一旦给 CSV 加 `#` 开头的注释行，`head` 退化为单元素，
`rows.filter(r => r.length === head.length)` 会**滤光全部数据行**，账本被解析成 0 行，
检查空转而门禁**照样 PASS**。这比门禁变红危险得多。因此任何说明一律走本文件，
**禁止向该 CSV 追加注释头**。

---

## 1 · 行数与定性：`rounds/r11/verdict.md:17` 的数字有误

`verdict.md:17` 原文：

> - 代码侧缺陷 71 条(implementation-gaps.csv)—— 范围排除,非规模 deferred

该句在**数量**与**定性**两方面都不准：

| 项 | verdict.md 的说法 | 实测 |
|---|---|---|
| 条数 | 71 | **73**（`csv.DictReader` 计数；文件 74 行 = 1 表头 + 73 数据行） |
| 定性 | 「代码侧缺陷」 | 73 条中 **39 条是文档侧**（`side=docs`），代码侧仅 **34 条** |

**71 的来历（可复算）**：`rounds/r11/worklog.md:294` 与同文件「构成与已知限制」一节把本表记作
46（shard-07a）＋ 6（`R11-P0-1~6`）＋ 19（`EV-07c-01~19`）＝ 71 行。
该清单**漏计了后加入的 `GAP-1C-01` / `GAP-1C-02` 两行**——这两条在 `verdict.md` 里
是另起一条 bullet 单列的。46 ＋ 6 ＋ 19 ＋ 2 ＝ **73**，与实测一致。

**处置**：`verdict.md` 与 `worklog.md` 是已验收的历史凭证，**一律不改**，错误数字只在此登记。
引用本表规模时以本文件为准。

---

## 2 · `severity` 值域已归一（R12-batch3）

归一前本表混用三套词表（07a 沿用 `findings.csv` 词表、07b 用 `blocking-P0`、07c 用 `P1`/`P2`），
且有 1 条把中文说明写进了 `severity` 字段，导致无法机读聚合。
现已全部归一到 `rounds/r8/findings.csv` 的既有 5 值域，**未新增第六个值**：

`critical` / `blocking` / `important` / `advisory` / `info`

### 原值 → 新值对照表

| 原值 | 条数 | 新值 | 依据 |
|---|---:|---|---|
| `blocking-P0` | 5 | `blocking` | 编排指定 |
| `blocking-P0（销毁执行面降 INERT）` | 1 | `blocking` | 同上；括注另存，见下 |
| `P1` | 20 | `important` | 编排指定 |
| `P2` | 1 | `advisory` | 见下「P2 定档依据」 |
| `important` | 11 | `important` | 不变 |
| `advisory` | 2 | `advisory` | 不变 |
| `info` | 33 | `info` | 不变 |

归一后分布：`info` 33 / `important` 31 / `blocking` 6 / `advisory` 3，合计 73。

**未改变任何一条的实际严重度**：阻塞级归并后恰为 6 条（5 ＋ 1 脏值），无降级；`info` 33 条无升级。

### `P2` 定档依据（`EV-07c-11`）

该条 `implementation_evidence` 为
`apps/web/src/customer/i18n/statusLabels.ts:235-240（ignored 与 dismissed 双键同渲染为「已忽略」）`，
`prd_evidence` 为 `DEC-R4-2026-08-04-43 / DEC-R8-08 / DEC-R8-46 三度裁定删「已忽略」`。

定为 `advisory` 而非 `info`，两条理由：

1. 本表的 `info` 全部是 37 条 `GAP-xx` 那类**证据指针占位行**（`prd_evidence` 仅「见 §7.4A 状态依据列」，
   无任何可执行的修复动作）。`EV-07c-11` 有确切 `文件:行号` 与三度裁决，性质不同。
2. 它是**客人端可见文案**，且已有明确裁决要求删除，属可立即执行的改进项——
   正是 `advisory`（改进建议）的语义，而 `info`（纯记录）会低估它。

同时它低于 `important`：与同批 `EV-07c-01~10` 的 `P1`（人工放行 / 场次容量等口径错误）相比，
本条只影响一处状态标签渲染，不改变钱路、权限或核销判定。

### 已知遗留：`critical` 与 `blocking` 的界线

`findings.csv` 同时使用 `critical`（9 条）与 `blocking`（7 条），两者的判别标准无既有裁决。
本表 6 条阻塞级一律落 `blocking`，**未擅自区分**。如需拆分需另行裁决。

---

## 3 · `severity` 里的括注去了哪里

原脏值 `blocking-P0（销毁执行面降 INERT）` 的括注内容承载真实信息（`R11-P0-2` 的销毁执行面为 INERT）。
本表 8 列无 `notes` 列，故该括注已**原文追加到该行 `implementation_evidence` 列末尾**：

```
…｜IMPLEMENTATION_INERT；严重度补注:销毁执行面降 INERT。
```

`severity` 列只留纯值 `blocking`。信息未丢失。

---

## 4 · 新增 `side` 列

位置：`affected_component` 之后、`severity` 之前。表头共 9 列。

值域：`docs` / `code` / `mixed`。

### 判据

按**修复动作要落在哪里**判定，不按 `prd_evidence` 引的是不是文档
（本表每行的 `prd_evidence` 都指向文档，那是缺口的需求侧，不是修复侧）：

- **`docs`** —— 修复只需改 Markdown 正文，不触及任何可编译 / 可运行 / 可迁移的文件。
- **`code`** —— 修复需改源码、SQL 迁移、脚本或 CI 配置（含 `tools/*.mjs` 这类生成器源文件；
  其重生成的文档是派生产物，不算手改文档）。
- **`mixed`** —— 同一条同时要求手改文档正文与源码，两边缺一不可。

### `affected_component` → `side` 映射（全 17 个取值）

| `affected_component` | 条数 | `side` |
|---|---:|---|
| `docs` | 39 | `docs` |
| `tools` | 6 | `code` |
| `ci` | 5 | `code` |
| `web-admin` | 4 | `code` |
| `settlement` | 3 | `code` |
| `backend` | 2 | `code` |
| `finance` | 2 | `code` |
| `web-customer` | 2 | `code` |
| `amusement` | 2 | `code` |
| `compliance-wecom` | 1 | `code` |
| `ticketing+governance+amusement` | 1 | `code` |
| `gate` | 1 | `code` |
| `db-migration` | 1 | `code` |
| `android-terminal` | 1 | `code` |
| `analytics` | 1 | `code` |
| `tools/docs-gen` | 1 | `code` |
| `backend/web/migration` | 1 | `code` |

分布：`docs` **39** / `code` **34** / `mixed` **0**，合计 73。

### 两个复合 component 的逐条判定

- **`ticketing+governance+amusement`（`R11-P0-3`）→ `code`**：三条链证据全是
  `TicketingController.java` / `V270__*.sql` / `SupervisorReviewPage.tsx` / `AmusementService.java` 等代码位置，
  无文档待改项。
- **`backend/web/migration`（`GAP-1C-02`）→ `code`**：证据为
  `FaceVerificationRouter.java` / `DisabledFaceVerificationGate.java` / `V146__*.sql` /
  `IntegrationSwitchesPage.tsx:24`；该行自述「文档措辞已领先代码……留待代码轮统一改口径」，
  即文档侧已正确，只有代码滞后。
- 附带说明 **`tools/docs-gen`（`GAP-1C-01`）→ `code`**：待改的是
  `tools/integration-capability-switches.mjs:49` 这个源文件；三份生成物是跑
  `npm run docs:*` 自动重出的派生产物，不是手改文档，故不算 `mixed`。

`mixed` 当前 0 行——保留在值域里，是为了以后真出现「文档与代码都得手改」的条目时不必再改列语义。

---

## 5 · 文件格式（改动时必须保持）

本表与姊妹表 `rounds/r8/findings.csv`、`docs/prd-audit/html-prd-coverage.csv` **格式不同**，
后两者是 BOM ＋ CRLF，本表是：

- **无 BOM**（首三字节 `66 69 6e`，即 `fin`）
- **行尾全 LF**，无 CR
- UTF-8，`csv` 模块 `QUOTE_MINIMAL` 引号规则（全表仅 `FINAL-10` 一行因字段含 ASCII 逗号被引号包裹）

R12-batch3 的归一**原样保持**了上述格式，未做归一化到姊妹表的样式。
后续脚本读本表用 `encoding='utf-8-sig'` 可同时兼容有无 BOM。

## 6 · 消费方

截至 R12-batch3，**没有任何脚本或 CI 读取本表**
（`git grep implementation-gaps` 在 `.github/`、`tools/`、`package.json` 下零命中，
仅有文档正文引用）。因此本次加列不影响任何门禁。
