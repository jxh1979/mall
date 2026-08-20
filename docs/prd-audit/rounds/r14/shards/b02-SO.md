已完成 R14-b02 · SO 分片审计。以下为报告正文。

---

# R14-b02 Auditor 分片报告 · SO（service-order） r01

## 1. 覆盖声明

**目标文件**：`docs/service-order-product-prd-v0.1.md` @ `ff2037a1`
blob `449ffe80e65d62963cf71f3bf2729c177756b3a4` / 32022 字节 / 315 行 —— 与任务包声明及 `rounds/r14/prd-scope-manifest.txt` 逐字一致（EV-SO-01）。全部取证走 `git show ff2037a1:<path>`，零工作区读取。

**基线漂移**：`git diff --name-only ff2037a1 origin/main` 对目标 PRD、三份 HTML、四张台账、三份跨引 PRD（bar / pgm / finance）**全部为空**；`origin/main` 已推进到 `08a30921`，唯一新增提交是 R14-b01 自身审计产物 PR #381，属计划 §七③ 的自身合并例外（EV-SO-02）。本分片结论不受漂移影响。

| 关 | Skill | 核过的面 | 结果 |
|---|---|---|---|
| CONFORMANCE | `mattpocock-skills:domain-modeling` | coverage 全表过滤 `prd_path` 含 service-order 得 **22 行全为 FULL**，逐行回读（FULL 亦抽验）；BSM 服务点单章 15 条 entry × entry-registry × authority-rule-ledger 三源比对；ENUM 第十二章 2 条；G-1 反查全表确认 19 条规则各自**唯一落点**即本 PRD，无落点分工；G-3 条目名 21 处逐个回三份 HTML 现文 | 7 条 finding |
| ADVERSARIAL | `mattpocock-skills:grilling` | 不可证伪措辞宽正则 12 词 **0 命中**；跨条算术（管理费闭区间×分账触发×退款边界）串实数；跨文件镜像句 5 组（finance §4A.1 / pgm §12B.5 / bar §9·§11-5b~5g / ENUM 服务费·小费 / PGM 解释权声明）逐字比对 | 4 条 finding |
| FIRST_PRINCIPLES | `ponytail:ponytail-audit` | 15 条硬约束逐条追 business rationale 出处；专查不可能场景兜底、同一事多口径、只有实现理由的约束 | 2 条 finding |

**HTML 定向取节**：BSM L1114–1140（服务点单章全章）、L580–582（分步引导条）、L1447–1449、L1673；ENUM L225、L279–284、L297；PGM L970。**未整读任何 HTML。**

## 2. Findings 总表

| ID | lens | sev | disp | 文件:条目名 | 摘要 | EV |
|---|---|---|---|---|---|---|
| R14-SO-01 | CONFORMANCE | P1 | A | `authority-rule-ledger.csv`:BSM-0191 / BSM-0196 / BEQ-0114 | 三条规则 `source_text` 与 `value_flow` 未随 DEC-R8-90（08-10）与 DEC-R8-97-07（08-19）的 HTML 改动同步，钱路约束在账本侧整段缺失 | EV-SO-03~05 |
| R14-SO-02 | ADVERSARIAL | P1 | A | `platform-governance-prd-v0.1.md`:§12B.5 ＋ coverage BSM-0312 / BSM-0314 | 两行 coverage 判 FULL@pgm-prd，但该文件「服务点单」「劳务报酬」**双零命中**；薪资单只列小费与服务费两栏 | EV-SO-08~09 |
| R14-SO-03 | ADVERSARIAL | P1 | A | `finance-settlement-product-prd-v0.1.md`:§4A.1 :240 | 同一句内两个取数时点：尾句「一律以开台或下单时点」vs 括注「按服务完成时点」 | EV-SO-10 |
| R14-SO-04 | FIRST_PRINCIPLES | P1 | B（change_target=HTML） | `service-order-product-prd-v0.1.md`:§3.3 第 6 项 | 退款冲回在本状态机下**无可达路径**；「管理费随之冲回」是对不可能场景的兜底 | EV-SO-11 |
| R14-SO-05 | ADVERSARIAL | P1 | B（change_target=HTML） | `service-order-product-prd-v0.1.md`:§3.3 第 5 项 | 「变更前向受影响方通告」的比照对象**为空**——销售提成全部权威落点无任何通告规则 | EV-SO-12~13 |
| R14-SO-13 | CONFORMANCE | P1 | CODE-FOLLOWUP | `backend/.../ServiceOrderService.java`:77-78 / 302 | 固定金额管理费**无上限校验** + 分账时 `Math.min` **静默封顶**，与 DEC-R8-90「保存时直接拒绝，不得静默截断、封顶或改写」直接相反 | EV-SO-20~22 |
| R14-SO-06 | CONFORMANCE | P2 | A | `service-order-product-prd-v0.1.md`:§3.2 引文块 :52 | 标为「ENUM 原文口径」的引文漏 DEC-R8-80 落地的互认括注 | EV-SO-06 |
| R14-SO-07 | CONFORMANCE | P2 | A | `service-order-product-prd-v0.1.md`:§4.4-2 :122 ＋ `entry-registry.csv`:ENUM-E0117 | 引用的 ENUM「终端十端」条在三份 HTML **零命中**，现名「终端十一端」；注册表同样停在旧名（R14-CON-02 同族第 2 例） | EV-SO-14 |
| R14-SO-08 | CONFORMANCE | P2 | A | `service-order-product-prd-v0.1.md`:§13 | §3.3 六项比照规则在 P0 验收标准中零承接 | EV-SO-15 |
| R14-SO-09 | ADVERSARIAL | P2 | A | `service-order-product-prd-v0.1.md`:§16 变更日志 | 日期倒序错乱（08-06 → 08-19 → 08-10）；同批 ff2037a1 的 A-05 已修 finance/dividend，本文件漏修 | EV-SO-16 |
| R14-SO-10 | CONFORMANCE | P2 | A | `entry-registry.csv`:PGM-E0307 / BSM-E0736 vs ENUM-E0167 | 同一 footer 块在 ENUM 登「解释权声明」、在 PGM/BSM 登「文档版本」，PRD :3 的引用按注册表不可解析 | EV-SO-17 |
| R14-SO-11 | FIRST_PRINCIPLES | P2 | B | `service-order-product-prd-v0.1.md`:§3.1 / §3.2-4 / §13-15 | 术语未闭合：字段名「固定价格」，上限判定的被除量写「服务售价」，DEC-R8-90 原文第三个说法「服务价格」 | EV-SO-18 |
| R14-SO-12 | CONFORMANCE | P2 | A | `entry-registry.csv`:BSM-E0048 | `entry_body` 只截到 h3 标题，含「新增服务目录」的 19 项清单正文未登 | EV-SO-19 |

**已裁不重报**：DEC-R7-11（收款确认冲突，§4.3/§5/§15 承接忠实，逐字复核通过）、DEC-R8-70/71（「公司」＝该服务发生地所属酒吧商户，§4.1/§8.1 承接忠实）、SYNC1-02（ENUM「服务订单状态」条互认括注遗留，引用不重列）、canonical「商户服务工单七态」GAP（已确认与本 PRD 的服务订单四态是不同对象，BSM L576 属数据存档章，无冲突，引用不重列）。

## 3. A 类明细（fix-ready）

### R14-SO-01 · 账本三条规则 source_text 整段漏登（P1）

定量比对（ledger `source_text` 长度 vs registry `entry_body` 长度，同一 SHA）：17 条规则中 14 条等长且逐字一致，**3 条短缺**：

| rule_id | src | body | 缺失内容 |
|---|---|---|---|
| BSM-0191 | 48 | 229 | 缺「按比例时配置值必须在 0% 至 100%（含边界）之间；固定金额必须在 0 至服务售价（含边界）之间；任一越界值在保存时直接拒绝，不得静默截断、封顶或改写。」＋「服务点单管理费的配置主体与审批…一律比照销售提成的对应规则执行；上下限约束按本条前述区间。」 |
| BSM-0196 | 50 | 80 | 缺「服务点单份额与管理费的归属月份按服务完成时点所在营业日确定。」 |
| BEQ-0114 | 91 | 303 | 缺上述三段全部 |

**现行原文逐字（BSM-0191 `source_text`）**：`后台维护服务目录：每项服务定固定价格、时长、上下架；每项可配管理费——不收、按比例、或固定金额。`
**应改成什么**：照 HTML 现文（BSM L1120 / ENUM L282）补齐 `source_text` 全文，并同步派生的 `value_flow` 列——三条的 `value_flow` 同样只到旧句为止，上下限与归属月份两项**涉钱约束在账本侧完全不可见**。
**依据**：BSM「后台维护服务目录」条、BSM「服务完成后系统自动分账」条、ENUM「管理费方式」条现文；DEC-R8-90、DEC-R8-97-07。
**这正是任务书 §1 第 3 问的实证**：`entry-registry.csv` 已同步（entry_body 含新内容），`authority-rule-ledger.csv` 未同步。门禁只检一致不检登全，故未响。本轮 O-1 已将该台账列 PROTECTED，只登记不落地。

### R14-SO-02 · 服务点单份额在薪资域零承接，两行 coverage 误判 FULL（P1）

`docs/platform-governance-prd-v0.1.md` 全文：`服务点单` = 0 命中（`grep -c`，exit=1）、`劳务报酬` = 0 命中（exit=1）、对照 `点单|提成` = 12 命中（exit=0，证明检索面与编码正常）。

- BSM-0312「薪资看得清」现文含「提成（酒水提成和**服务点单份额自动归集**）」→ coverage 判 FULL@pgm-prd
- BSM-0314「月结时按**劳务报酬**法定税率表对**服务点单份额**代扣个税」→ coverage 判 FULL@pgm-prd
- pgm §12B.5 现行原文：「**薪资单另含小费归集与服务费归集两栏**，两栏各自独立列示，全额发放给对应服务提供人并代扣个税。」——**无服务点单份额栏，无劳务报酬税目**。

**跨文件矛盾**：service-order §8.2-2 要求「薪资单须列示该笔服务点单分账的应发额、代扣税额与实发额」，而 §14 边界表把薪资单构成推给 pgm §12B；pgm 侧的薪资单栏目集里没有这一栏。**两份 PRD 对同一张薪资单给出不同栏目集。**
**应改成什么**：pgm §12B.5 照 BSM-0312 / BSM-0314 现文补服务点单份额归集栏与劳务报酬税目（HTML 逐字可照抄，目标状态唯一）；coverage 两行由 FULL 改判并重指落点。
**依据**：BSM「薪资看得清」条（L1447）、BSM「月结时按劳务报酬法定税率表对服务点单份额代扣个税」条（L1449）。
**注**：pgm-prd 属 b05 分片目标，本条为跨批发现，按公共契约先登记。

### R14-SO-03 · finance §4A.1 同句两个取数时点（P1）

**现行原文逐字**（`finance-settlement-product-prd-v0.1.md`:240）：
`**一律按营业日归属的适用清单**：日结批次、…、销售提成月份、**服务点单份额与管理费的归属月份**（2026-08-19 补：该两项按**服务完成时点所在营业日**确定，依据 BSM「服务完成后系统自动分账」条 / ENUM「管理费方式」条现文；DEC-R8-97-07），**一律以开台或下单时点所属的营业日为准，不按自然日切分**。`

尾句「一律以开台或下单时点所属的营业日为准」的射程覆盖整张清单，与括注的「服务完成时点」对同一项给出相反口径。清单里「销售提成月份」按 BSM-0705 确实是下单时点，所以尾句本身没错——**错在没把服务点单两项从尾句射程里显式排除**。
**应改成什么**：把括注升为独立分句并显式排除，例如「…销售提成月份，一律以开台或下单时点所属的营业日为准；**服务点单份额与管理费的归属月份除外，按服务完成时点所在营业日确定**；均不按自然日切分。」
**依据**：BSM「服务完成后系统自动分账」条现文、ENUM「管理费方式」条现文（唯一口径＝服务完成时点）。service-order §8.1-5 侧承接**正确**，无需改。
**b01 未登记**：`rounds/r14/findings.csv` 53 行对 `营业日|归属月份|4A\.1|服务点单|管理费` 检索 0 命中（exit=1）。

### R14-SO-06 · §3.2 引文块漏互认括注（P2）

**现行原文逐字**（:52）：`> ENUM「管理费方式」条 原文口径：**不收、按比例、固定金额**（按服务目录项配置；服务费全额入公司账，管理费归公司收入，其余份额按薪资/提成名录进服务人员月结薪资）。`
**应改成什么**：`服务费全额入公司账` 后补 `（此处「公司」指该服务发生地所属酒吧的经营主体）`。
**依据**：ENUM「管理费方式」条现文（ENUM L282）；DEC-R8-80。
DEC-R8-71⑤「标明为 HTML 现行原文的 PRD 引文按原样保留不改写」是 2026-08-06 的纪律，而 ENUM 侧括注是 2026-08-07 的 DEC-R8-80 才落地——引文此后未跟进。同一括注在 BSM 侧写「所属酒吧**商户**的经营主体」、ENUM 侧写「所属酒吧的经营主体」，两份 HTML 措辞不一（实质同一主体，DEC-R8-70 已裁），一并在此备案，不单开条目。

### R14-SO-07 · ENUM「终端十端」条目名在 HTML 零命中（P2）

三份 HTML `grep -c 终端十端` 全为 **0**；ENUM `终端十一端` = 1（L225，正文列 11 项）。`entry-registry.csv` ENUM-E0117 的 `entry_name` 仍登「终端十端」。
**现行原文逐字**（PRD :122）：`…（终端十端见 **ENUM「终端十端」条**）…`
**应改成什么**：PRD 与注册表 `entry_name` 同步改为「终端十一端」。
**依据**：ENUM L225 现文。与 R14-CON-02 同族——**注册表自身 entry_name 会漂移且门禁不响，本轮第 2 例**。

### R14-SO-08 · §3.3 六项无验收标准（P2）

§13 共 15 条 P0 验收标准，其中第 15 条对应 §3.2-4 上下限。§3.3 的配置主体与审批、出厂默认值、版本化与生效日、下单锁版本、变更前通告、排除项与退款冲回**六项零对应验收条**。
**应改成什么**：按 §3.3 各项机械导出验收条（如「管理费方式与数值的费率版本在下单时锁定；重新月结不改已锁定版本，仅重算金额」），措辞可从 bar §9 对应销售提成验收条平移。出厂默认值的**数值**不得自拟（见推断清单第 4 项）。

### R14-SO-09 · §16 变更日志日期倒序（P2）

现行顺序：`2026-08-06`（:313）→ `2026-08-19`（:314）→ `2026-08-10`（:315）。同一提交 `ff2037a1` 的 commit message A-05 记「finance / dividend 变更日志行改为 4 列格式并**移到日期序表首**」，说明该批已识别日期序问题，本文件未同批处理。**应改成什么**：08-10 行上移至 08-19 行之前。

### R14-SO-10 · 解释权声明条目名三源不一致（P2）

同一 footer 块：ENUM-E0167 `entry_name`＝「解释权声明」；PGM-E0307 与 BSM-E0736 `entry_name`＝「文档版本」（`entry_body` 含解释权全文）。coverage PGM-0192 / BSM-0530 亦用「文档版本」。PRD :3 引「PGM「解释权声明」条」——按 HTML 现文可解析（PGM L970 有 `<strong>解释权声明：</strong>` 标签），但**按 PRD 自己声明的锚点契约「条目名以 entry-registry.csv 为准」不可解析**。
**应改成什么**：注册表三行 `entry_name` 统一。PRD 抬头正文与 PGM L970 现文**逐字一致**（含 DEC-R7-39 的时间线口径），正文无需改。

### R14-SO-12 · BSM-E0048 entry_body 只登标题（P2）

`entry_body` = `后台的"新建/录入"改成分步引导`（仅重复标题），HTML L581 的 `<p>` 正文（19 项已改造清单）未登。
**副产品**：PRD §12 声称「清单含"新增服务目录"」——回 HTML L581 验证**成立**（`新增服务目录` 在 BSM 命中 1 次，ENUM/PGM 各 0 次），该 claim 有据，不登缺陷。但若只查注册表会误判为无据，属注册表侧的取证陷阱。

### R14-SO-13 · 管理费上下限的实现与已批规则相反（P1，CODE-FOLLOWUP）

`backend/src/main/java/com/complex/platform/serviceorder/ServiceOrderService.java`：

```java
// L76-79（目录项保存）
long feeValue = request.mgmtFeeValue() == null ? 0 : request.mgmtFeeValue();
if (feeValue < 0 || (feeMode.equals("percent") && feeValue > 10000)) {
    throw ApiException.badRequest("VALIDATION_FAILED", "管理费数值（mgmt_fee_value）不正确：百分比模式使用基点，不得超过 10000");
}
// L299-302（完成时分账）
long mgmtFee = switch (feeMode) {
    case "percent" -> MoneyRounding.bpsOf(current.priceCent(), feeValueFinal);
    case "fixed"   -> Math.min(feeValueFinal, current.priceCent());
```

- **percent** 上限 10000 基点＝100%，与 HTML 一致 ✓；下限 0 ✓
- **fixed 模式无任何上限校验** —— 保存时不拒绝超过服务售价的固定金额
- **L302 `Math.min` 就是静默封顶** —— DEC-R8-90 与 BSM/ENUM 现文明令「任一越界值在保存时直接拒绝，**不得静默截断、封顶或改写**」

**wiring**：`ServiceOrderController` `@RestController @RequestMapping("/api/v1/services")`；`POST /catalog` → `service.upsertCatalogItem`（L88-92）、`POST /orders/{id}/complete` → `service.complete`（L152-158）。两条路径均**当前可达**。
DEC-R8-90 补注 5 已预言此形态：「现有代码若静默封顶只能作为待修实现证据，不能反向升级为业务规则」。
**基线判定**：`rounds/r11/implementation-gaps.csv` 73 条对 `服务点单|管理费|service.order` 检索 **0 命中**（exit=1）→ 属 `CODE_FOLLOWUP_DELTA`，非既有条目。

## 4. B / C / D 明细

### R14-SO-04（B，P1，change_target=HTML）· 退款冲回无可达路径

**五条件自证**：①属业务政策与金额规则选择 ✓ ②取证已完成 ✓ ③现有权威源无唯一答案 ✓ ④存在多个合理方案 ✓ ⑤用户回答创造新业务事实（服务点单是否存在完成后退款情形）✓

**矛盾链（代实数串一遍）**：§8.1-1 分账**由「已完成」触发，无人工发起入口** → §6-2「进入「已完成」态即不可退款，不设人工放行、不设主管例外」 → 唯一可退场景是 §6-1「已收款后客人取消」，此时订单未完成、分账未发生、管理费未计取。故 §3.3-6 的「退款时的冲回与反向结算…按订单最终保留收入重新计算并冲回差额，生成一笔反向结算记录」在本 PRD 定义的四态机下**无任何可达状态**；其后半句「已收款但服务未完成而取消的，管理费随之冲回」冲的是一笔从未发生的计取。

**HTML 侧的根**：BSM-E0316 与 ENUM-E0157 现文均写「退款时的冲回与反向结算，一律比照销售提成的对应规则执行」，而 BSM-E0325 同章写「服务完成后不可退」、BSM-E0323 写完成时才分账。**权威源内部三条并存但组合不自洽**——不是「两条对同一对象给相反口径」的经典形态，是**悬空规则**，无唯一胜出证据（比照条为 2026-08-19 新增但并未 supersede「完成后不可退」）。按 §4.2 第 2 种情形第二分支 → B，`change_target=HTML`，不得择一写死。

**两处查证结果**：决策台账宽正则检索 `服务点单|管理费` 全部落点（DEC-R8-70/71/80/90/94-40/97-07、§17）——无任何一条讨论服务点单的完成后退款；canonical 与 `rounds/r14/findings.csv` 检索 `管理费|冲回|反向结算` 无同源条目。

**最小决策面**
- **现有证据**：HTML 三条并存；PRD 忠实承接了比照条，矛盾不是 PRD 引入的
- **HTML 明确到哪**：完成后不可退（明确）；完成时才分账（明确）；退款冲回比照销售提成（明确要求存在，但未给触发条件）
- **未明确什么**：服务点单是否存在任何完成后退款/撤销情形；若无，「冲回与反向结算」这一比照要素是否应从 HTML 删除
- **候选方案**：㈠ 认定服务点单确无完成后退款 → 从 BSM-E0316 / ENUM-E0157 删除该比照要素，PRD §3.3-6 后半段随删（影响：与销售提成的比照不再逐项对齐，需在 HTML 注明例外）；㈡ 认定存在有限的完成后撤销情形（如误点完成、渠道回传后被撤单）→ 须新增第五态或撤销动作，与 §5「四态为全集」「已完成为终态」两条硬约束冲突，影响面覆盖状态机、留痕、薪资冲正（影响：本 PRD 状态机与 §13-7 验收标准需重写）

### R14-SO-05（B，P1，change_target=HTML）· 通告规则的比照对象为空

**检索面与检索式（NEGATIVE_SEARCH）**：
- `authority-rule-ledger.csv` 全表按 `entry_name` 含「销售提成」取出 **9 条**（BSM-0380 / BSM-0701~0708 / BEQ-0035），逐条读 `source_text`：无任何「通告 / 告知 / 公示 / 通知受影响方」要素
- `bar-business-operations-product-prd-v0.1.md` 全文 `通告|告知|公示|通知受影响` → **1 命中**，位于 :429 存酒登记 PIPL 隐私告知，与提成无关

HTML BSM-E0316 / ENUM-E0157 明确要求「变更前向受影响方通告…一律比照销售提成的对应规则执行」，而被比照的销售提成规则里**这一要素不存在**。PRD §3.3-5 因此自行推断出「变更生效前须向受影响的**服务提供人与商户**通告，通告与生效日一并留痕」——**通告对象、方式、提前时长、留痕要求全部无权威出处**。

按 §5.1 硬边界，不得登 A。

**两处查证结果**：决策台账 DEC-R8-97 全系列与 DEC-R8-98 同步明细无通告口径；canonical findings 与 b01 findings 无同源条目。

**最小决策面**
- **HTML 明确到哪**：要求「变更前向受影响方通告」这件事必须存在
- **未明确什么**：受影响方是谁（服务提供人？商户？两者？）、提前多久、以什么渠道、通告失败是否阻断生效日
- **候选方案**：㈠ 在 BSM/ENUM 的销售提成条补通告规则，服务点单自动继承（影响面最大，同时改变酒吧销售提成域）；㈡ 在服务点单条直接写明通告口径，不再走比照（影响面局限本域，但与「一律比照」的表述不符）；㈢ 认定该要素为误写并删除（影响：PRD §3.3-5 随删）

### R14-SO-11（B，P2）· 价格术语未闭合

同一个量三处三名：§3.1 目录项字段「**固定价格**」；§3.2-4 与 §13-15 的上限被除量「**服务售价**」；DEC-R8-90 原文「**服务价格**」。HTML 侧亦然（BSM L1120 同句内先写「每项服务定固定价格」再写「0 至服务售价」）。§3.1「不设按人、按台阶浮动的现场议价」使实际只有一个可能读法，但同一性声明全文缺失，而这个量正是固定金额管理费上限判定的分母。
**候选方案**：㈠ 在 §3.1 字段表加一行同一性声明（「服务售价＝本表固定价格」）；㈡ 全文统一为「服务售价」。二者均需先确认 HTML 是否同批统一 → 涉 HTML，故不入 A。

### D 类

**无。** 本分片全部条目取证链已跑到底：`git log` 三提交逐 hunk、宽正则跨库扫描（三份 HTML + 四张台账 + 7 份关联 PRD + backend/apps）、相关目录检索均已完成，无「证据不足需继续调查」的悬置项。

### C 类 HTML 草案

本分片**不出 C 类逐句草案**。R14-SO-04 与 R14-SO-05 虽最终须改 HTML，但按 §4.2 第 2 种情形第二分支——无唯一胜出证据、须用户创造业务事实——登 B 并标 `change_target=HTML`，**不得先择一写成草案**。已裁的 SYNC1-02 引用不重列。

## 5. 推断补写清单

HTML 未写明、PRD 需补的判定方法 / 边界 / 兜底 / 时限 / 留痕，逐条列出（均属 B，不入 A）：

1. **渠道回传丢失或延迟的兜底（P1）**。§4.3-3 把「收到渠道回传到账信息」设为完成履约的硬门槛，并加写「不设人工放行、不设主管例外」（HTML 只写「没有收到渠道回传的到账信息不能点完成」，PRD 属同向更紧，coverage BSM-0532 已备案）。但**回传永久丢失时服务已实际提供却永远无法点完成 → 永远不触发分账 → 服务提供人永远拿不到该笔份额**。HTML 对这条死路零口径，PRD 也未给对账补录或异常路径。缺口在钱路上，**建议优先于其余各项裁决**。
2. **「已下单」态的超时处置（P2）**。§6-4 只说「未收款的取消不进入已收款待退，直接取消」，未定义谁取消、多久后取消、是否自动。HTML 沉默。
3. **零份额分账是否仍生成记录（P2）**。§3.2-4 允许比例＝100% 或固定金额＝服务售价（§13-15 明确「均可保存」）。此时服务提供人份额为 0：分账记录是否仍生成、§8.2 的 0 元劳务报酬代扣是否仍在薪资单列示一行、可追溯性如何保证——三处均无口径。
4. **§3.3-2 出厂默认值的具体数值（P2）**。PRD 写「必须给出出厂默认值」而未给数字，**这是正确处置**（§5.1 禁自拟阈值），但数值本身仍是待裁项。销售提成侧的对应值是 15%/10%（BSM-0702），服务点单侧无对应裁决。
5. **§7-4「分别统计、分别对账，不合并计数」（P2）**。HTML BSM-E0326 只写「独立单据不并入酒水账单；公司账上每笔全额入账、可追可查」。「分别统计、分别对账」是同向推断，风险低，登记备案。
6. **§6-2「不设密钥逐笔开放通道」的出处未标（P2）**。已核：「密钥」与「逐笔开放」在三份 HTML 均有落点（BSM 11/4、ENUM 2/1、PGM 4/2），属票务域既有机制，该否定式声明**有 business rationale**，非无据兜底；仅缺条目名标注，按 §4.4 FIRST_PRINCIPLES「须写明出处」备案。

## 6. R13 追溯表

`git log --oneline 2bc7cb0f..ff2037a1 -- docs/service-order-product-prd-v0.1.md` → **3 个提交**（任务包已知的 `32b63708` 在内）。

| commit | hunk | 改动 | 能否追溯 | 删除类？删对了吗 |
|---|---|---|---|---|
| `32b63708`（#370） | §3.2 +硬约束 4 | 管理费闭区间与拒绝保存 | ✓ DEC-R8-90（R13-041）；与 BSM L1120 / ENUM L282 现文逐字一致 | 无删除 |
| `32b63708` | §13 +第 15 条 | 0/边界/越界验收 | ✓ 同上 | 无删除 |
| `32b63708` | §16 +1 行 | 变更日志 | ✓ | 无删除。**副产品：日期序错乱，见 R14-SO-09** |
| `3f3e30cd` | 标题 V0.1→V0.2 | 版本号 | ✓ 决策台账 :2230 登记「service-order V0.1→V0.2」 | 改写。文件名仍 `-v0.1.md`，属全批同形态，非本文件独有 |
| `3f3e30cd` | +§3.3 全节 | 管理费比照条六项 | ✓ DEC-R8-97-07；HTML 引用块逐字核对通过 | 无删除。**但比照对象核验暴露 R14-SO-05** |
| `3f3e30cd` | §8.1 +第 5 条 | 归属月份按服务完成营业日 | ✓ DEC-R8-97-07；与 BSM-E0323 / ENUM-E0157 现文逐字一致 | 无删除 |
| `3f3e30cd` | §8.2-3 改写 | 小费税目措辞 | ✓ DEC-R8-97-10 | **删除类**：被删句为「小费…**按规定代扣个人所得税**（ENUM「小费」条）」。回 ENUM-E0062 现文验证——现文已是「代扣个人所得税，适用税目按发放时有效的国家法律、纳税主体身份和交易实质确定」，旧句确为过期原文。**删对了** |
| `ff2037a1`（I-04） | §3.3 引用块改写 | 「上下限约束按 §3.2 第 4 项的区间」→ 恢复 HTML 原文「按本条前述区间」，指路括注移出引号外 | ✓ 上一轮技术审查 I-04 | **删除类**：被删内容是**对 HTML 引文的擅自改写**，恢复原文是正确方向。**删对了** |

**三问结论**：8 个 hunk 全部可追溯到 HTML 条目名或有效 DEC；2 个删除类 hunk 均已从 diff 复原被删内容并回权威源验证，均判「删对了」；无「已经在 main 上所以默认它对」的放行。

## 7. CODE-FOLLOWUP 候选

**R14-SO-13**（见 §3）为本分片唯一 CODE-FOLLOWUP，属 `CODE_FOLLOWUP_DELTA`。

- **PRD 证据**：§3.2-4、§13-15（BUSINESS_AUTHORITY 链：BSM L1120 / ENUM L282 / DEC-R8-90）
- **实施证据层级**：`WIRING_ACTIVE`（controller 两条端点映射到含缺陷的 service 方法，当前可达）+ `IMPLEMENTATION_INERT`（校验与封顶代码存在）。**本轮禁止 `RUNTIME_OBSERVED`**，故不声称生产环境已出现错误配置
- **基线核对**：73 条基线零命中，需登 DELTA 并同步 canonical findings（按 O-4 推迟到 R14-final）

## 8. Evidence 附录

`EV-<shard>-NN | 类型 | audit_lens | 命令或 file:line | exit @ SHA | 关键节选`
**全部命令在 `/home/james/complex-management` 执行，`SHA = ff2037a1`。无 artifact 落盘能力（纯只读角色），超长输出以可复现命令替代完整输出，如实标注。**

| EV | 类型 | lens | 命令 / 位置 | exit | 关键节选 |
|---|---|---|---|---|---|
| EV-SO-01 | BASELINE_STATE | — | `git rev-parse ff2037a1:docs/service-order-product-prd-v0.1.md`；`git cat-file -s`；`grep -E service-order rounds/r14/prd-scope-manifest.txt` | 0 | `449ffe80…` / `32022` / manifest 行逐字一致 |
| EV-SO-02 | BASELINE_STATE | — | `git diff --name-only ff2037a1 origin/main -- <目标 PRD+3 HTML+4 台账+bar/pgm/finance>` | 0 | 空输出；`git log --oneline ff2037a1..origin/main` → 仅 `08a30921 R14 b01 审计产物 (#381)` |
| EV-SO-03 | BUSINESS_AUTHORITY | CONFORMANCE | `git show ff2037a1:docs/business-system-map-for-operations-v0.1.html \| sed -n '1114,1140p'` | 0 | L1120 含「按比例时配置值必须在 0% 至 100%（含边界）之间…」「服务点单管理费的配置主体与审批…一律比照销售提成的对应规则执行」 |
| EV-SO-04 | BUSINESS_AUTHORITY | CONFORMANCE | `git show ff2037a1:docs/business-enum-quick-reference-v0.1.html \| sed -n '279,284p'` | 0 | L282 含上下限段 + 比照段 + 「服务点单份额与管理费的归属月份按服务完成时点所在营业日确定」 |
| EV-SO-05 | BASELINE_STATE | CONFORMANCE | python RFC4180 解析 `arl.csv` × `reg.csv`，逐条比 `len(source_text)` vs `len(entry_body)`（17 条规则） | 0 | `BSM-0191 src=48 body=229 SHORT` / `BSM-0196 src=50 body=80 SHORT` / `BEQ-0114 src=91 body=303 SHORT`；其余 14 条 OK |
| EV-SO-06 | BUSINESS_AUTHORITY | CONFORMANCE | PRD:52 vs ENUM L282 | 0 | PRD 引文无「（此处「公司」指该服务发生地所属酒吧的经营主体）」，ENUM 现文有 |
| EV-SO-07 | BASELINE_STATE | CONFORMANCE | python 过滤 `html-prd-coverage.csv`（1234 行）`prd_path` 含 service-order；再按 19 个 rule_id 反查全表落点（G-1） | 0 | 22 行全 FULL；19 条规则各自唯一落点即本 PRD，无落点分工 |
| EV-SO-08 | NEGATIVE_SEARCH | ADVERSARIAL | 检索面＝`git show ff2037a1:docs/platform-governance-prd-v0.1.md` 全文；检索式 `grep -cE "服务点单"` / `"劳务报酬"` / 对照式 `"点单\|提成"` | 1 / 1 / 0 | `0` / `0` / `12` —— 对照式命中证明检索面与编码正常，前两项为真零命中 |
| EV-SO-09 | BUSINESS_AUTHORITY | ADVERSARIAL | `reg.csv` BSM-E0471 / BSM-E0473；`arl.csv` BSM-0312 / BSM-0314；`cov.csv` 两行 | 0 | HTML L1447「提成（酒水提成和服务点单份额自动归集）」、L1449「月结时按劳务报酬法定税率表对服务点单份额代扣个税」；coverage 两行均 `FULL @ platform-governance-prd` |
| EV-SO-10 | BUSINESS_AUTHORITY | ADVERSARIAL | `git show ff2037a1:docs/finance-settlement-product-prd-v0.1.md \| sed -n '240p'` | 0 | 括注「按服务完成时点所在营业日确定」与尾句「一律以开台或下单时点所属的营业日为准」同句并存 |
| EV-SO-11 | BUSINESS_AUTHORITY | FIRST_PRINCIPLES | PRD:80（§3.3-6）/ :151（§6-2）/ :174（§8.1-1）；BSM-E0316 / E0323 / E0325 | 0 | 分账触发＝已完成；已完成不可退；未完成取消时管理费未计取 |
| EV-SO-12 | NEGATIVE_SEARCH | ADVERSARIAL | 检索面＝`arl.csv` 全表 `entry_name` 含「销售提成」的 9 条（BSM-0380 / BSM-0701~0708 / BEQ-0035）；逐条读 `source_text` | 0 | 9 条中「通告 / 告知 / 公示」要素 0 命中 |
| EV-SO-13 | NEGATIVE_SEARCH | ADVERSARIAL | 检索面＝`git show ff2037a1:docs/bar-business-operations-product-prd-v0.1.md` 全文；检索式 `grep -nE "通告\|告知\|公示\|通知受影响"` | 0 | 唯 1 命中 :429 存酒登记 PIPL 告知，与提成无关 |
| EV-SO-14 | NEGATIVE_SEARCH | CONFORMANCE | 检索面＝三份 HTML 全文；检索式 `grep -cE "终端十端"` / `"终端十一端"` | 0 | ENUM `终端十端=0 终端十一端=1`；BSM 与 PGM 两项均 0。ENUM L225 `<td class="k">终端十一端</td>`，正文 11 项 |
| EV-SO-15 | BASELINE_STATE | CONFORMANCE | PRD:262-278 逐条读 §13 15 条 vs §3.3 六项 | 0 | 仅第 15 条对应 §3.2-4；§3.3 六项零对应 |
| EV-SO-16 | BASELINE_STATE | ADVERSARIAL | PRD:313-315 | 0 | `2026-08-06` → `2026-08-19` → `2026-08-10` |
| EV-SO-17 | BASELINE_STATE | CONFORMANCE | `reg.csv` 检索 `entry_name` 或 `entry_body` 含「解释权」；`git show ff2037a1:docs/platform-governance-map-for-operations-v0.1.html \| sed -n '970p'` | 0 | ENUM-E0167＝「解释权声明」；PGM-E0307 与 BSM-E0736＝「文档版本」。PGM L970 正文与 PRD:3 抬头逐字一致 |
| EV-SO-18 | BASELINE_STATE | FIRST_PRINCIPLES | PRD:45 / :65 / :278；`dec.md`:1935 | 0 | 「固定价格」/「服务售价」/ DEC-R8-90「服务价格」 |
| EV-SO-19 | BASELINE_STATE | CONFORMANCE | `reg.csv` BSM-E0048；`git show ff2037a1:docs/business-system-map-for-operations-v0.1.html \| grep -nE "新增服务目录"` | 0 | `entry_body` 仅标题；HTML L581 清单确含「新增服务目录」（BSM 命中 1，ENUM/PGM 各 0）→ PRD §12 的 claim 成立 |
| EV-SO-20 | IMPLEMENTATION_INERT | CONFORMANCE | `git show ff2037a1:backend/src/main/java/com/complex/platform/serviceorder/ServiceOrderService.java`（460 行）L76-79 | 0 | `if (feeValue < 0 \|\| (feeMode.equals("percent") && feeValue > 10000))` —— fixed 模式无上限分支 |
| EV-SO-21 | IMPLEMENTATION_INERT | CONFORMANCE | 同上 L299-302 | 0 | `case "fixed" -> Math.min(feeValueFinal, current.priceCent());` |
| EV-SO-22 | WIRING_ACTIVE | CONFORMANCE | `git show ff2037a1:…/ServiceOrderController.java` L15-16 / L88-92 / L152-158 | 0 | `@RestController @RequestMapping("/api/v1/services")`；`@PostMapping("/catalog")` → `service.upsertCatalogItem`；`@PostMapping("/orders/{id}/complete")` → `service.complete` |
| EV-SO-23 | NEGATIVE_SEARCH | — | 检索面＝`rounds/r14/findings.csv`（53 行，b01 产物）；检索式 `grep -cE "营业日\|归属月份\|4A\.1\|服务点单\|管理费"` | 1 | `0` —— R14-SO-03 非重复登记 |
| EV-SO-24 | NEGATIVE_SEARCH | — | 检索面＝`rounds/r11/implementation-gaps.csv`（73 条基线）；检索式 `grep -cE "服务点单\|管理费\|service.order"` | 1 | `0` —— R14-SO-13 属 DELTA |
| EV-SO-25 | HISTORICAL | — | `git log --oneline 2bc7cb0f..ff2037a1 -- docs/service-order-product-prd-v0.1.md`；`git show <c> -- <file>` ×3 | 0 | 3 提交 / numstat `+3−0`、`+18−2`、`+1−1`；逐 hunk 结论见 §6 |

**计数纪律自查**：coverage 22 行（口径＝物理行，含 BSM-0761/0762 两行同 entry_name 拆句登记）；HTML 条目 15 条（口径＝BSM 服务点单章 `entry_kind=entry`，不含 3 个 section 行）+ ENUM 2 条；账本比对 17 条（口径＝有 rule_id 且落点为本 PRD 的规则行，`BSM-0194` 为 `ORPHANED/SUPERSEDED` 不纳入）。三个数口径不同，**不互换、不相减**。`BSM-0194`（旧「服务员现场确认收款」口径）已 SUPERSEDED，PRD §4.3 承接的是取代它的 BSM-0532，处置正确。

**本分片不输出 VERDICT 行。**

---

*编排者落盘备注：SO 子代理返回报告的原样落盘（HTML 实体已还原）。落盘时间 2026-08-20。*
