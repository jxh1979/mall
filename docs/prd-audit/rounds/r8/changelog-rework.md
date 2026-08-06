# R8 返工变更台账（技术审查 NEEDS_FIX 返工·实施代理 F2）

- 日期：**2026-08-05**
- 触发：R8 技术审查 `TECHNICAL_VERDICT: NEEDS_FIX`，返工项 R-1 ~ R-8（含 Advisory A-1/A-2/A-4/A-5）
- 三份业务 HTML：**一字未动**（收工 sha256 前 8 位 `085918bd` / `5081f029` / `a4c02dab`，与开工基线逐字节相同）
- 未执行任何 git 写操作（`add` / `commit` / `push` 均未执行）

格式：`位置 | 修改前 | 修改后 | 依据`

---

## R-1 §16.1 恢复 HTML 现行付款时点（Blocking·钱路）

文件：`docs/merchant-points-settlement-rules-v1.0.md`

| 位置 | 修改前 | 修改后 | 依据 |
|---|---|---|---|
| §16.1 付款时点条（L328） | 「上月款项按**后台指定的付账日**完成收付——平台内部各主体每月有**三个付账日：5 日、15 日、25 日**……原「初始在每月 15 日统一完成上月净额收付款」**作废**」 | 「初始在**每月十五日**完成上月收付款，遇法定节假日或非银行工作日顺延至下一工作日……**一律按各自总额分别收付、不得轧差**；原表述中的**净额**收付**作废**」 | ENUM「积分结算账期」条·实测 L77；BSM L816 / L1577 现行口径；DEC-R4-2026-08-04-91 |
| §16.1 作废宣告 | 作废对象＝「每月 15 日统一完成上月净额收付款」整句 | 作废对象**只保留「净额」**，不再对「每月 15 日」作废 | ENUM L77 明文「每月十五日完成上月收付款」仍为现行 |
| §16.1 R8-C01 登记块（L330） | 只写「算术矛盾已登记 + 方向」 | 三个付账日 5/15/25、月结定格→出账→异议期届满→付账日顺序校验、异议期未届满顺延，**整段移入登记块**，冠以「**用户已定方向（待三份 HTML 修订草案批准后生效，现阶段不作为现行口径）**」 | BSM 结算规则条·实测 L1496、BSM「三个付账日」条·实测 L1506；DEC-R4-35 / -36 / -40 |
| §16.2 | （不动） | （不动，仍为「按各自总额分别列示，不得轧差」） | ENUM L77 |
| §16.1 异议期条（L327） | （不动） | （不动，仍为十个工作日） | ENUM L77；DEC-R3-2026-08-04-19 |

## R-2 锚点错行（改前逐行 `sed -n 'Np'` 实测 HTML 验证）

实测依据：`BSM L1420 = 「给商户：房租、水电费、物业费的账单与催缴……」`；`BSM L1718 = 「注册、提交资料审批、收公告通知……等可离场办理的事」`；`BSM L1112 = 「客人存酒是客户资产、不算可售库存；可转让给他人」`；`BSM L1110 = 「高价值物品」`。

| 文件·位置 | 修改前 | 修改后 |
|---|---|---|
| `third-party-business-access-product-prd-v0.1.md` §11A 标题（L232） | 章 L1417–L1419，「给商户」条 L1418 | 章 **L1417–L1421**，「给商户」条 **L1420**、「不可关闭清单」条 **L1421** |
| `real-estate-full-chain-domain-prd-v0.1.md` L39 | 「给商户」条 L1417 | **L1420** |
| `merchant-portal-zone-prd-v0.1.md` L62（异议期口径） | 「给商户」条 L1417 | **L1420** |
| `merchant-portal-zone-prd-v0.1.md` L96（我的分红）**〔同源补修〕** | 「给商户」条 L1417 | **L1420** |
| `platform-governance-prd-v0.1.md` L312（可离场办理的事） | L1715 | **L1718** |
| `platform-governance-prd-v0.1.md` L170（同条引用）**〔同源补修〕** | L1715 | **L1718** |
| `supply-chain-inventory-product-prd-v0.1.md` L81（客人存酒·转让术语） | L1110 | **L1112** |
| `supply-chain-inventory-product-prd-v0.1.md` L349（A-4 锚点沿革尾巴） | 「重建为 ENUM L131／BSM L1108」 | 「……／**BSM L1110**」 |

> **偏差说明**：技术审查指定 §11A 章区间尾端为 L1420；改前实测 `sed -n '1417,1422p'` 显示该 `info-block` 内容行为 **L1417（h3）/1418（ul）/1419（给客人）/1420（给商户）/1421（不可关闭清单）**，`</ul>` 在 L1422。PRD §11A.3 正文承接的正是 L1421「不可关闭清单」，故区间取 **L1417–L1421**（审查值的超集），并在锚点里同时点名 L1420 与 L1421 两条。

## R-3 区间锚点尾端

| 文件·位置 | 修改前 | 修改后 | 依据 |
|---|---|---|---|
| `service-order-product-prd-v0.1.md` L5（唯一事实源） | 章区间 L1035–L1057 | **L1035–L1059** | BSM `section#service-order` 实测：h2 L1035、末条 li L1059、`</ul>` L1060 |
| `service-order-product-prd-v0.1.md` L72 | 章导语 L1034 | **L1036** | BSM L1036 = `<p class="lead">`（L1034 为 `<section>` 标签行） |
| `service-order-product-prd-v0.1.md` L282（覆盖条目清单）**〔同源补修〕** | BSM L1033/L1034/L1039–L1044/L1050–L1057 | **L1035/L1036/L1041–L1046/L1052–L1059** | 同上；两个 `info-block` 的 li 实测区间 |
| `platform-governance-prd-v0.1.md` L312「员工手机端」条区间 L1293–L1326 | （未改） | （未改） | 实测 `sed -n '1291,1332p'`：`section#employee-phone` = L1292–L1330，h2 = L1293，末条内容行 = L1326（其后 L1327 `</ul>`、L1330 `</section>`）。**原锚点属实，无需修正** |

## R-4 CRLF 行尾恢复

三份文件在上一轮被整文件由 CRLF 转为 LF，制造 7242 行伪 diff。

| 文件 | HEAD 行尾实测 | 处置 | 处置前后实质 diff（`--ignore-cr-at-eol --numstat`） |
|---|---|---|---|
| `business-operations-layer-spec-v0.2.md` | 1965/1965 全 CRLF | LF→CRLF 整文件还原 | 处置前 25/23 → 处置后 25/23（未变） |
| `evidence-risk-layer-spec-v0.2.md` | 743/743 全 CRLF | 同上 | 处置前 6/4 → 处置后 6/4（未变） |
| `terminal-display-layer-spec-v0.2.md` | 910/910 全 CRLF | 同上 | 处置前 3/1 → 处置后 3/1（未变） |

`git diff --stat` 三文件合计由 **7242 行 → 62 行**（R-4 完成即刻实测，R-5 内容修改在其后进行）。

## R-5 第二向漏项收尾

### 5.1 「场次容量」11 处（先 R-4 后 R-5，避免二次噪声）

三份文件各在文首新增一段口径块（照抄 BSM L1162 / PGM L478 原句 + DEC-R5-2026-08-04-04 + R8 用户裁决 D-02），正文 11 处一律改「**项目容量（实时在玩人数）**」：

| 文件 | 处数 | 位置 |
|---|---|---|
| `business-operations-layer-spec-v0.2.md` | 8 | 原 L79/144/275/584/1115/1256/1447/1629（含两处章标题：`8.3 开放计划与项目容量（实时在玩人数）`、`8.1 项目容量页（实时在玩人数）`） |
| `terminal-display-layer-spec-v0.2.md` | 2 | 原 L80 / L111 |
| `data-analysis-layer-spec-v0.2.md` | 1 | 原 L79 |

> **未改并已在文首口径块声明**：低保真页面字段清单中仍按场次/时段罗列的字段（如「日期、场次、开始结束时间、容量」）只作历史结构参考，容量口径以文首口径块为准；`business-operations-layer-spec-v0.2.md` 同一表格单元内并列的口语化「开放、场次、容量」措辞未逐词改写，已登记为 `R8-U-05`。

### 5.2 其余漏项

| 文件·位置 | 修改前 | 修改后 | 依据 |
|---|---|---|---|
| `member-points-p1-ops-parameter-tables-v0.1.md` §3 H 行（L35） | 「项目、SKU、**滚动 30 天**成交快照、新 SKU 首发公开价审批」 | 「项目、商品规格、**后台配置的滚动取价窗口**（业务参数，按项目可配，出厂默认三十天，须同时配置上下限）内成交快照、新商品规格首发公开价审批」 | BSM 零售参考价条·实测 L1498；DEC-R3-31 |
| 同上（L29/L30/L36 及 L52） | 「SKU」共 6 处 | 「商品规格」（并清理中英夹写空格） | ENUM「一律叫商品规格」；DEC-R3B-15 |
| `compliance-and-personal-information-protection-requirements-v0.1.md` L166 | 「★GAP-07 / SEC-04 / P0-A」 | 「★SEC-04 / P0-A」 | GAP-07 编号归还 §7.4「高价值阈值」，与 PII 加密撞号 |
| `amusement-business-operations-product-prd-v0.1.md` L287（A-2） | 「涉及身高、年龄、健康提示、安全告知、**人工放行**或拒绝的……」 | 删除「人工放行」并加注「（原含「人工放行」，**已废止**——ENUM「核销记录状态」条·实测 L41……；DEC-CFG-2026-08-03-12）」 | ENUM L41；DEC-CFG-2026-08-03-12 |
| `business-operations-layer-spec-v0.2.md` 流程图节点（原 L1156，A-1） | `F["拒绝"]`（上一轮直删「或人工放行」无标注） | `F["拒绝（原含「人工放行」，已废止·见文首口径）"]` | 同上（与其余 30 处标注统一） |
| `evidence-risk-layer-spec-v0.2.md` L575（A-1） | 「1. 主管复核：高风险事件关闭……」（上一轮直删「人工放行、」无标注） | 句末加注「（原含「人工放行」，已废止·见文首口径）」 | 同上 |
| `pending-development-backlog-v0.1.md` L87（A-5） | 「② **票面原价** `component_price_cent`」 | 「② **各业态价格**（原称「票面原价」，2026-08-05 DEC-R7-16 更名）`component_price_cent`」 | DEC-R7-16；DB 列名等机读标识不动 |
| `ticketing-entitlement-settlement-breakdown.md` L286（A-5，2 处） | 「按各业态**票面原价**占比拆份额」／「**票面原价**用于定价与拆份额」 | 「按各业态**价格**占比拆份额（「各业态价格」原称「票面原价」……）」／「**各业态价格**用于定价与拆份额」 | 同上 |

## R-6 findings.csv 补产出

新建 `docs/prd-audit/rounds/r8/findings.csv`（148 行数据 + 表头，8 列，UTF-8 BOM + CRLF，`python csv` 可解析、`finding_id` 全局唯一）。

| 来源 | 编号段 | 条数 | status |
|---|---|---|---|
| `rounds/r8/changelog-imp1.md` | `R8-A-01` ~ `R8-A-60` | 60 | fixed |
| `rounds/r8/changelog-imp2.md`（账本/矩阵改动） | `R8-M-01` ~ `R8-M-10` | 10 | fixed |
| coverage.csv 既有 finding 编号 | `R8-F01` ~ `R8-F06` | 6 | fixed |
| `.scratch/prd-audit-r8/issues/01..10` | `R8-B-01` ~ `R8-B-10` | 10 | pending-user |
| `HTML修改建议草案.md` | `H-01` ~ `H-08` | 8 | draft-pending-approval |
| 本次技术审查返工项 | `R-1` ~ `R-8` | 8 | fixed-in-rework |
| `prd-audit-fix-change-log-20260804.md` §7.4A | `GAP-01` ~ `GAP-41` | 41 | open ×39 / closed-doc-layer ×2 |
| `changelog-imp2.md` §七 遗留 + 本轮新增遗留 | `R8-U-01` ~ `R8-U-05` | 5 | open |

## R-7 账本两条错锚改 ORPHANED

文件：`docs/prd-audit/authority-rule-ledger.csv`（`source_text` 未动）

| rule_id | 修改前 anchor | 修改后 anchor | 旧值来源 |
|---|---|---|---|
| BSM-0033 | `1809` | `ORPHANED(旧记 726)` | `rounds/r8/reanchor-manual-review.csv` L3 `old_anchor=726` |
| BSM-0063 | `731` | `ORPHANED(旧记 771)` | `rounds/r8/reanchor-manual-review.csv` L12 `old_anchor=771` |

改后 `python csv` 复核：**902 行、列宽全为 17**，与改前一致。

## R-8 乙方案·predeploy 修复（用户 2026-08-05 授权扩界，仅限此一处）

根因：`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` L319 与 `docs/p0-openapi-contract-closure-appendix-v0.1.md` L2457 的 L-19 作废批注是**手工追加进生成物**的，源 `openapi-p0.json` 无对应内容，`verify:operation-matrix` / `verify:openapi-closure` 判定生成物与源不同步 → `verify:predeploy` 红。

两份生成器（`tools/generate-openapi-operation-matrix.mjs` L70、`tools/generate-openapi-contract-closure-appendix.mjs` L77）取的是 **`operation.summary`**，不取 `description`。因此批注合入 `summary`（否则重生成即抹掉批注），`description` 同步合入以免契约源仍单独宣告已作废语义。

| 文件 | 位置 | 修改前 | 修改后 |
|---|---|---|---|
| `docs/openapi/openapi-p0.json` | `POST /api/v1/compliance/wecom/retention-policies` `summary` | 「配置企微留存策略（默认180天）」 | 原文 + 空格 + L-19 批注全文（逐字照搬，未改一字） |
| 同上 | 同端点 `description` | 「R5 T8：新增/更新企微会话留存策略。retention_days 默认 180……」 | 原文 + 空格 + L-19 批注全文 |
| `docs/openapi/openapi-p0.yaml` | 同端点 `summary` / `description` | 同上 | 同上（`verify:openapi` 强制 json↔yaml 深等价，只改一边必红） |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | L319 | 批注挂在「失败事件」单元格尾部 | 重生成：批注随 `summary` 落入「摘要」单元格，失败事件单元格恢复纯净 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | L2457 | 批注挂在「operation 级补充」单元格尾部 | 同上 |

重生成前后 `diff` 逐份比对：两份生成物**各只有 1 行变化**，批注文字逐字仍在，**无其它手工批注被抹掉**。

> **偏差说明**：技术审查指令写「合入 description」「git diff 应只有 description 一处」。实读生成器后确认 description 不进生成物，只改 description 无法满足验收第 4 条（批注必须仍在且来自源），故改为 `summary` + `description` 双改；并因 `verify:openapi` 的 json↔yaml 深等价校验，`openapi-p0.yaml` 一并同步。

---

## 验证命令与结果

| 命令 | 结果 |
|---|---|
| `npm run verify:openapi` | PASS（OpenAPI contract verification passed） |
| `npm run verify:openapi-closure` | PASS（720 operations checked, in sync） |
| `npm run verify:operation-matrix` | PASS（720 operations checked, in sync） |
| `npm run verify:p0-contract` | PASS（全部子项 PASS） |
| `npm run verify:predeploy` | **EXIT=0 全绿**（含 `[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML`） |

## R-9 微返工 4 处（R8 轮 F3 代理，2026-08-06）

| # | 位置 | 修改前 | 修改后 | 依据 |
|---|---|---|---|---|
| I-A | `docs/third-party-access-layer-spec-v0.2.md` L367「处理状态」行 | `待处理、待复核、已关闭（原含「已忽略」，**对账差异状态已收为四态、该终态删除**，依据 DEC-R4-2026-08-04-43）` | `待处理、待复核、已关闭（**差异类记录**原含「已忽略」，已按 DEC-R4-2026-08-04-43 收为四态；**接入异常/回流侧的「已忽略」是否同受该裁决约束待 R8-B-09 用户裁决**，在此之前该侧维持五态）` | R8 复审 I-A：原标注把仅适用于差异类记录的 DEC-R4-2026-08-04-43 扩用到接入异常/回流侧，属过界。加限定语并把接入异常/回流侧挂到 R8-B-09 待裁。同文件 L392/L393 状态表未动 |
| I-B | `docs/prd-audit/rounds/r8/findings.csv` 末行追加 | 无 H-09 行（148 数据行） | 追加 `H-09,C,important,docs/business-system-map-for-operations-v0.1.html,1421,...,draft-pending-approval`（149 数据行） | R8 复审 I-B：BSM L1421「下列三类不在可关闭范围内」实列四项（商户应收应付变化/未核销票到期/客人金额变化/存酒到期），数量词与列举不符，需入台账走 HTML 修订草案 |
| Advisory-1 | `docs/service-order-product-prd-v0.1.md` L292 变更日志行 | `实测 L1035–L1057` | `实测 L1035–L1057（区间已于 R8 更正为 L1035–L1059，见 L5）` | R8 复审 Advisory-1：L5 唯一事实源已于 R-3 更正为 L1035–L1059，L292 历史行仍记旧区间，补溯源批注（历史行本体不改写） |
| Advisory-3 | `docs/prd-audit/rounds/r8/findings.csv` L103（R-8 行）`file` 字段 | `docs/openapi/openapi-p0.json;docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `docs/openapi/openapi-p0.json;docs/openapi/openapi-p0.yaml;docs/p0-openapi-operation-acceptance-matrix-v0.1.md;docs/p0-openapi-contract-closure-appendix-v0.1.md` | R8 复审 Advisory-3：R-8 实际改动含 `openapi-p0.yaml` 与 `p0-openapi-contract-closure-appendix-v0.1.md`（`git status` 可证），file 字段漏记两项 |

编码/行尾保持：`third-party-access-layer-spec-v0.2.md` = UTF-8 BOM + LF；`service-order-product-prd-v0.1.md` = UTF-8 无 BOM + LF；`findings.csv` = UTF-8 BOM + CRLF。三份业务最终解释权 HTML **未改动**（sha256 前 8 位仍为 `085918bd` / `5081f029` / `a4c02dab`）。

改后 `python csv` 复核 `findings.csv`：**149 数据行、finding_id 唯一、列宽全为 8**。
