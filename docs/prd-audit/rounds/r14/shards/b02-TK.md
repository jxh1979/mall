R14-b02 · Shard **TK**（`docs/ticketing-entitlement-checkin-product-prd-v0.1.md`）Auditor 报告 r01

---

## 1. 覆盖声明

**冻结变量**：`AUDIT_BASE_SHA = ff2037a1c1e40a97e5979a5517451c7bfe409d16`；取证仓 `/home/james/complex-management`；一切被审文本经 `git show "$AUDIT_BASE_SHA":<path>`，**未读任何工作区业务文件**（工作区 HEAD 实测 `e3770efa`，与基线不同，故禁读这一条本轮真实生效 —— EV-TK-01）。

**目标文件**：643 行 / 78176 字节，blob `2eebe2319bea09979ef420bba94a4b938a41c308`，与任务包声明逐字相符（EV-TK-01）。全文 643 行**全量读过**（1–180 / 180–330 / 330–470 / 470–643 四段）。

**任务包前提纠正（2 条）**

1. 任务包 TK-S3 与 `b01-SR.md:379` 均称「ticketing **§12A** 的收款方镜像句」。实测：**§12A（12A.1/12A.2）全节不含任何积分发行成本或收款方语句**；本文件的积分发行成本落点只有 **§10 `:502`** 与 **§12B.2 `:596`** 两处，收款方在全文零命中（EV-TK-08、EV-TK-14）。后续引用请改指 §12B.2。
2. 任务包称 BSM「票务权益」section 共 118 承接行、本文件占 56。按我自己的 counting contract 复算得 **56**，与之一致；但**本文件的承接面不止该 section**，全量为 **102 行**（见下）。

**Counting contract（承接行）**：计数对象＝`html-prd-coverage.csv` 物理数据行；纳入规则＝`prd_path` 按 `;` 切分后**包含**目标文件路径的行（含多文件共享行）；时点＝`@ff2037a1`；解析＝RFC4180 + `utf-8-sig`。结果：**102 行**（`FULL 85 / STALE 14 / PARTIAL 3`）。按 HTML section 归属：BSM「票务权益」53 + 3（entry_name 在 ENUM 一与 BSM 票务权益重名）＝**56**；ENUM「一、票务与权益」13＋3＋1；BSM「一笔业务怎么走」7；BSM「酒吧管理」5＋1；其余 5 行分散；**14 行 entry_name＝`ORPHANED`，在 `entry-registry.csv` 无对应条目**（EV-TK-03、EV-TK-04）。

**三关完成情况**

| 关 | skill | 核过的面 | 结论 |
|---|---|---|---|
| CONFORMANCE | `mattpocock-skills:domain-modeling`（域模型比对法） | 102 承接行逐行过；3 条 PARTIAL 全部逐分句复核；14 条 STALE 全部回 `authority-rule-ledger.csv` 查 `decision_status`（**14 条全为 `SUPERSEDED`**，其被取代口径已并入本关其余检查，EV-TK-05）；FULL 行抽验 BSM-0028/0034/0036/0037/0056/0064/0070/0600–0608/0631–0651/0690–0695 与 BEQ-0001–0015/0125/0126；G-3 条目名可解析性关：PRD 中 **29 个**「BSM/PGM/ENUM「××」条」引用**逐个回三份 HTML 现文验证，29/29 存在**（EV-TK-06） | 12 条 finding |
| ADVERSARIAL | `mattpocock-skills:grilling` | 高风险域全覆盖：售票/退款原则、核销六态与入场三环节、通票权益 fan-out、赠酒下限与出资方、库存/超卖、渠道费两层与积分发行成本、跨批镜像句（settlement-rebuild / finance / bar / customer-app / amusement / points）；不可证伪措辞逐条过；跨条算术代实数串（100→10→54/36；场次—有效期—多次票次数；退款期→PENDING→AVAILABLE） | 5 条 finding |
| FIRST_PRINCIPLES | `ponytail:ponytail-audit` | 逐条问「为什么存在 / 拿掉会怎样」：§5.1a 退款原则、§11 P1 表、§4/§7.1/§7.2 三处实现缺口登记块、§12B 全节、§7.5 引用状态表 | 2 条 finding + 3 条 CODE-FOLLOWUP |

**G-1（判零承接前反查全表）实际执行**：本关**未产出任何「本文件零承接」型 finding 而未反查**。四处候选缺口逐一做了全仓反查，其中 **2 处判为落点分工、不登缺失**：①BSM「权益转赠」条「卡座开卡人可在邀请函中将该陌生人移出、收回其名额；该人一经核销门票权益即不得再移出」→ 落点在 `customer-app-miniapp-product-prd-v0.1.md:60-62`；②ENUM「项目内部结算价」条「任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券」→ 落点在 `settlement-rebuild-prd-v1.0.md:762`、`finance-settlement-product-prd-v0.1.md:184-188`、`merchant-points-settlement-rules-v1.0.md:177`（EV-TK-07）。

**本轮禁止 `RUNTIME_OBSERVED`**：本报告零条 `RUNTIME_OBSERVED`。三条 CODE-FOLLOWUP 均标 `IMPLEMENTATION_INERT` / `BASELINE_STATE`。

**红牌遵守**：`business-html-source-of-truth-acceptance-spec-v0.1.md` 全程**未作 BUSINESS_TRUTH 引用**；`business-decision-log-v0.1.md` 同样只作 `HISTORICAL`。

---

## 2. Findings 总表

| finding_id | audit_lens | sev | disp | 文件:条目名 | 一句话 | EV |
|---|---|---|---|---|---|---|
| R14-TK-01 | CONFORMANCE | P0 | A | ticketing:§7.4 核销记录状态前置块(:282)／§5.1 多人票行(:103) | BSM「核销单位」条「年龄核验环节按人逐一进行，多人票不得以一人核验代替全体」**全仓 PRD 零承接**，多人票一人代验的未成年人红线缺口 | EV-TK-09 |
| R14-TK-02 | CONFORMANCE | P1 | A | ticketing:§5 人群策略(:70)／§5.1 学生票行(:102) | ENUM「年龄限制」条分句 5「学生票不等于成年，学生票客人同样受 18 岁限制」全仓零命中 | EV-TK-10 |
| R14-TK-03 | CONFORMANCE | P1 | A | ticketing:§5.1a 退款原则(:120) | 「已核销部分不得**按未履约理由**退款」以理由限定收窄了 HTML 的无条件禁止，且与本文件 :424/:433 自相矛盾 | EV-TK-11 |
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-55，本文不再重复记载。
| R14-TK-05 | CONFORMANCE | P1 | A | ticketing:§5 票种发布上架闸(:89-93) | 上架闸自述穷举（「下列任一条」）却只列 3 条，缺 ENUM「门票综合服务费」条的第 4 条上架闸 | EV-TK-13 |
| R14-TK-06 | CONFORMANCE | P1 | A | ticketing:§8.3.2 营业兜底(:396-408) | 缺 BSM「核销单位」条「原票核销权按该两条规则延长，**不受本条场次终结的限制**」，与 §6:172／§8.3.1:390 场次终结制直接打架 | EV-TK-14 |
| R14-TK-07 | CONFORMANCE | P1 | A | ticketing:§12A.2 业务规则第 3 条(:562) | ENUM「动态价与会员价」条要求存「全部命中**资格价**」「**最终选中价**」，PRD 只写「全部命中资格」「最终采用价格」，比价过程不可复算（承接 coverage BEQ-0132 PARTIAL） | EV-TK-15 |
| R14-TK-08 | CONFORMANCE | P1 | B | ticketing:§7.1 票务策略状态(:205-212) | 六态（草稿/待启用/售卖中/暂停售卖/已结束/归档）在三份 HTML 与决策台账**全部零命中**，无权威出处 | EV-TK-16 |
| R14-TK-09 | CONFORMANCE | P2 | A | ticketing:§7.1 枚举权威注(:214) | 「本节状态值以速查表 §一 为最终口径」是错误出处声明——ENUM §一无「票务策略状态」条 | EV-TK-16 |
| R14-TK-10 | CONFORMANCE | P2 | A | ticketing:§9 页面清单·核销记录行(:473) | 术语契约不一致：「撤销/冲正」应为 §7.4／ENUM 六态的「已冲正」 | EV-TK-17 |
| R14-TK-11 | CONFORMANCE | P2 | A | docs/prd-audit/html-prd-coverage.csv:BEQ-0124／BSM-0070 | 落点登记不全（非内容缺失）：BEQ-0124 未登 `platform-governance-prd-v0.1.md:97`；BSM-0070 标 FULL 但「卡座移出」分句实落 customer-app | EV-TK-07、EV-TK-10 |
| R14-TK-12 | CONFORMANCE | P2 | B | ticketing:§4(:52)／§4 注(:61)／§8.2(:359)／§10(:488)／§12 第 6 条(:520)／§14(:624) | 「全场统一刷新间隔**默认 15**」在三份 HTML 零命中、决策台账零命中，6 处落点均无权威依据（引 canonical `FINAL-11`，open） | EV-TK-18 |
| R14-TK-13 | ADVERSARIAL | P1 | A | ticketing:§12B.2 渠道费用两层表·第二层行(:592) | 「承担方」格写成「按渠道合同，在通路费之上再扣一层」，与 BSM「域内通票不按经营域比例拆分」条「**两者**一律…由各域按分配份额自然共担、不由收款主体单独承担」冲突，且与同节 :598 自相矛盾 | EV-TK-19 |
| R14-TK-14 | ADVERSARIAL | P2 | A | ticketing:§12B.2 积分发行成本行(:596) | 保留「（基础积分与等级加成积分）」限定语——按裁决 15 广口径待删（TK-S1） | EV-TK-20、EV-TK-21 |
| R14-TK-15 | ADVERSARIAL | P1 | A | ticketing:§12B.1 第 2/3 条(:578/:579) | 只以「有效期」作权益终止条件，未承接 2026-08-04/08-06 才确立的场次终结制，与 §6:172／§7.1:224／§8.3.1:390 三处口径冲突 | EV-TK-22 |
| R14-TK-16 | ADVERSARIAL | P1 | B | ticketing:§12B.2(:596)／§10(:502) | 积分发行成本**取数时点缺失**：整单预扣 vs ENUM L69「按已核销的张（人次）计、未核销一律不计」不可能同时成立（本侧落点，引 `R14-FS-06`；裁决 5 已裁「预估扣＋差额冲正」，本文件沉默）（TK-S2） | EV-TK-08、EV-TK-21 |
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-55，本文不再重复记载。
| R14-TK-18 | FIRST_PRINCIPLES | P2 | A | ticketing:§5.1a(:120)／§8.4.1(:430-433)／§12 已按 D9 确认第 2 条(:536) | 同一件事三处三份全文：退款穷举五种＋U 盘放行写了三遍，且 :120 已漂移（见 R14-TK-03）——收敛为单一真身＋引用 | EV-TK-11 |
| R14-TK-19 | FIRST_PRINCIPLES | P2 | B | ticketing:§11 P1 表(:508)／§8.4 修订说明(:426) | 「复杂退款规则引擎」列 P1（2026-06-07 定），但 2026-08-04 收紧后退款只剩「五种穷举＋唯一例外＋U 盘逐笔」，该能力已无可规则化空间，business rationale 无出处 | EV-TK-23 |
| R14-TK-20 | FIRST_PRINCIPLES | P2 | CODE-FOLLOWUP | docs/openapi/openapi-p0.yaml:29137-29142 | E-25：`TicketTypeRequest.refresh_interval_seconds` 仍为票种级 `enum [10,15,30,60]`，落后于全场统一「5 秒为单位、5–60」口径 | EV-TK-24 |
| R14-TK-21 | FIRST_PRINCIPLES | P1 | CODE-FOLLOWUP | backend V009／V253（`ticket_orders`） | `pay_status`/`issue_status` 无 CHECK（V253 注释自证「不并入本约束」），`status` 仅 `created/cancelled`——§7.2 派生视图矩阵**无库层保障**，「已取消」与「已关闭」库内不可区分 | EV-TK-25 |
| R14-TK-22 | FIRST_PRINCIPLES | P2 | CODE-FOLLOWUP | docs/openapi/openapi-p0.yaml:29121-29129 | E-36：`TicketTypeRequest.status` 4 态（draft/active/paused/archived）与 §7.1 策略 6 态的对应关系未定 | EV-TK-26 |

**三关均有 finding，无「本关无发现」节。**

---

## 3. A 类明细（fix-ready）

### R14-TK-01（P0）多人票年龄核验按人逐一
- **目标位置**：§7.4 核销记录状态 前置引用块（:282 之后），并在 §5.1 资格规则表「多人票」行（:103）加一句交叉引用。
- **现行原文逐字（:282，节选）**：「**综合体入场由三道环节依次分开完成，不是一次性完成：安检环节（X 光查验）→ 年龄核验环节（人脸扫描确认满 18 周岁）→ 检票环节（二维码刷票并核销）。三道环节须逐道通过，前一道未通过的不得进入下一道；前两道环节不产生票务核销记录，核销记录状态只适用于检票环节。**」——全段无「按人」「逐一」「多人票」任何限定。
- **应改成什么**：在该块末尾补 HTML 原句：「**年龄核验环节按人逐一进行，多人票不得以一人核验代替全体。**」并在 §5.1 多人票行「凭证/留痕」列补「各人份须各自留存本人年龄核验结果」。
- **依据**：BSM「核销单位」条（BSM L796）原句逐字；`business-decision-ledger.md:1512` `DEC-R8-31` 明列该句为 14b-11「推断补入，已逐项单列交批」。
- **EV**：EV-TK-09。

### R14-TK-02（P1）学生票不等于成年
- **目标位置**：§5 票务策略表「人群策略」行（:70）P0 处理方式列；§5.1 资格规则表「学生票」行（:102）。
- **现行原文逐字（:70）**：`| 人群策略 | 学生票、成人票 | 记录适用人群和现场核验要求；学生票 P0 人工审核（证件 + 学信网），凭证留痕。<!-- 2026-06-24 删儿童票：全场 18+，无儿童票（用户取消） -->` —— 18+ 只存在于 HTML 注释，注释不是规则正文。
- **应改成什么**：处理方式列补正文句「**学生票不等于成年，学生票客人同样受 18 岁入场限制**，仍须通过综合体入场第二道年龄核验环节」。
- **依据**：ENUM「年龄限制」条（ENUM L31）末句逐字。
- **EV**：EV-TK-10。**已按 G-1 反查全表**：该分句在 `docs/*.md`（排除 `_archive`/`prd-audit`）零命中，非落点分工。

### R14-TK-03（P1）§5.1a `:120` 退款禁止被理由限定收窄
- **现行原文逐字（:120 末句）**：「**已核销部分不得按未履约理由退款（唯一例外见 §8.4.1）。**」
- **问题**：HTML 是无条件禁止；PRD 的「按未履约理由」把禁止收窄成一类理由，可退五种中的「重复扣款」「法律法规要求」并非未履约理由，据此文可对已核销票放款。
- **应改成什么**：「**门票或权益一经开始核销（含部分核销）即不可退款**；除 §8.4.1 通票安全例外（免单退款）外，退款只允许发生在**完全未核销**的票上。」
- **依据**：ENUM「售票与退款原则」条（ENUM L44）；BSM「退款」条（BSM L798）同句；`DEC-R8-94-01`（ledger L1993）。
- **EV**：EV-TK-11。

### R14-TK-04（P1）§8.4.1 `:431` 缺「未核销票」限定
- **现行原文逐字（:431）**：「- **五种之外的放行**：确需在五种情形之外退款的，只能由持授权 U 盘密钥的最高权限逐笔放行，**每次只对当前这一笔生效**，不得作为常规通道或批量授权。」
- **应改成什么**：「确需在五种情形之外**对未核销票**退款的，只能由持授权 U 盘密钥的最高权限逐笔放行，每次只对当前这一笔生效，不得作为常规通道或批量授权；**最高权限逐笔放行不解除已开始核销后的退款禁止**。」
- **依据**：BSM「退款原则」条（BSM L784）；ENUM L44 同义；`DEC-R8-94-01`。
- **EV**：EV-TK-12（`不解除已开始核销|不解除.{0,8}退款禁止` 在 `docs/*.md` 全仓 **exit=1，零命中**）。

### R14-TK-05（P1）§5 发布上架闸缺第 4 条
- **现行原文逐字（:89）**：「**票种发布上架闸（P0，2026-08-04 补；依据 ENUM「通票赠酒下限」条、ENUM「收费与免费权益结算」条）**：票种建成后固定为草稿，须经发布闸方可上架。**下列任一条不满足的，发布闸一律拒绝上架**，不得以「先上架后补」方式绕过：」+ 3 条。
- **问题**：清单自述穷举，实施者据此只建 3 道校验；ENUM 另有一道明确的**票种级**上架闸未列。
- **应改成什么**：新增第 4 条「**单一酒吧门票未选定综合服务费方式（按比例／按固定金额）或未填入数值**（依据 ENUM「门票综合服务费」条；产品口径见 `finance-settlement-product-prd-v0.1.md:126`）」，并在标题依据处补该条目名。
- **依据**：ENUM「门票综合服务费」条（ENUM L54）「……**未选定方式或未填写数值的票种不得发布上架**」；BSM L787 同句。
- **EV**：EV-TK-13。

### R14-TK-06（P1）§8.3.2 缺场次终结制豁免
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-55，本文不再重复记载。
- **问题**：§6:172、§7.1:224、§8.3.1:390 三处均把「场次结束即终结、不顺延、不折现」写成无例外。缺豁免句时，「延长到补偿有效期」与「场次结束即终结」在同一文件内互斥。
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-55，本文不再重复记载。
- **依据**：BSM「核销单位」条（BSM L796）末段原句逐字。
- **EV**：EV-TK-14（`不受[^，。]{0,12}场次终结` 在 `docs/*.md` 仅命中审计产物，无任何 PRD 落点）。

### R14-TK-07（P1）§12A.2 `:562` 价格快照缺「资格价」维度
- **现行原文逐字（:562）**：「- 订单保存基础价、动态价、**全部命中资格**、最终采用价格和规则版本。」
- **应改成什么**：「- 订单必须保存基础价、动态价、**全部命中资格价**、**最终选中价**及对应规则版本。」
- **依据**：ENUM「动态价与会员价」条（ENUM L55）；BSM L756 同句。
- **业务后果**：只存资格名单不存各资格算出的价格，事后无法重算比价过程。
- **EV**：EV-TK-15。

### R14-TK-09（P2）§7.1 `:214` 错误出处声明
- **现行原文逐字**：「> 枚举权威（2026-06-24 对齐；2026-08-04 补全）：**本节状态值**以业务枚举速查表 …§一（票务与权益）为最终口径……」
- **应改成什么**：「本节状态值」改为「**§7.2/§7.3/§7.4/§7.6 的状态值**」；本节另按 R14-TK-08 裁决结果处理。
- **依据**：ENUM §一 全 25 条 entry_name 实测无「票务策略状态」（EV-TK-16）。

### R14-TK-10（P2）§9 `:473` 术语统一
- `撤销/冲正` → `已冲正`。依据：ENUM「核销记录状态」条六态；本 PRD §7.4:289。

### R14-TK-11（P2）覆盖台账落点登记不全（**非内容缺失**）
- **BEQ-0124**：补登 `platform-governance-prd-v0.1.md:97`（红线③「未满 18 岁一律拒入、拒售酒」）。
- **BSM-0070**：补登 `customer-app-miniapp-product-prd-v0.1.md:60-62`（卡座移出分句实落点）。
- **处置＝补登落点，不登缺失。** EV：EV-TK-07、EV-TK-10。

### R14-TK-13（P1）§12B.2 表「第二层·承担方」列错值
- **现行原文逐字（:589-592）**：第二层行「承担方」格＝「按渠道合同，在通路费之上**再扣一层**」。
- **问题**：填的是扣除方式而非承担方，且「按渠道合同」与同节 :598「渠道合同**只能**约定佣金费率、收付款方式和对账方式，**不得改变佣金的承担方式与分摊口径**」自相矛盾。
- **应改成什么**：第二层承担方格改为「**各域按分配份额自然共担，不由该笔业务的收款主体单独承担**；渠道合同不得改变承担方式与分摊口径」；「在通路费之上再扣一层」移入「扣除时点」格。
- **依据**：BSM「域内通票不按经营域比例拆分」条（BSM L748）「**两者**一律…由各域按分配份额自然共担、不由该笔业务的收款主体单独承担」；ENUM L48「共同成本，一律按上述默认方式处理」。
- **EV**：EV-TK-19。

### R14-TK-14（P2）§12B.2 `:596` 限定语待删（TK-S1）
- **现行原文逐字**：「- **票务订单产生的积分发行成本（DEC-R8-87）**（基础积分与等级加成积分）——在域间拆分之前从整单扣除且只扣一次……成本只扣一次。」
- **应改成什么**：删去「（基础积分与等级加成积分）」七字，其余一字不动。
- **依据**：`rulings-b01.md` 裁决 15（广口径）；I-11 勘误——限定语真身是 **ENUM L69**；ENUM L48 与 BSM L748 均无该限定（EV-TK-20 逐字比对）。§10:502 同规则无限定，删后内部一致。
- **补充（TK-S1 后半）**：积分成本相关句共 2 处（:502、:596）；与裁决 15：:502 已合、:596 待删；与裁决 5：两处均未写取数时点 → R14-TK-16；与裁决 6：两处均未写收款方 → R14-TK-17；§12A.1 的 PENDING→AVAILABLE 与裁决 5 相容且互为前提。
- **EV**：EV-TK-20、EV-TK-21。

### R14-TK-15（P1）§12B.1 未承接场次终结制
- **现行原文逐字（:578-579）**：「2. **未核销跨期**：权益有效期内跨自然月/结算期仍可核销…」「3. **过期未核销**：权益过期后不得再核销…」
- **问题**：§12B 写于 2026-07-28，场次终结制 2026-08-04/08-06 才确立；现文把「有效期」当唯一终止条件，跨期应付口径失真。
- **应改成什么**：第 2 条主语加限定「**未完成检票环节的票**…」；补「**已完成检票环节的票，其全部权益的可用窗口收窄为该场次，场次结束即终结，按未核销份额进入分账减除并按 ENUM「域归属对照」条归属（见 §8.3.1）**」；第 3 条补「或场次结束后」。
- **依据**：ENUM「有效期类型」条（L43）；BSM「核销单位」条（L796）；ENUM「到期未核销款」条（L45）；本 PRD §6:172、§8.3.1:390。
- **EV**：EV-TK-22。

### R14-TK-18（P2）退款口径三处三份全文
- 落点 :120 / :430-433 / :536。以 §8.4.1 为唯一真身；:120 改一句引用；:536 改「见 §8.4.1（完整口径）」保留 D9 出处。依据：CLAUDE.md §5「同一事实只完整陈述一次」。

---

## 4. B / C / D 明细

**C 类：无**（本文件相关两条 C 已在 b01 开立）。**D 类：无**（全部取证链跑完）。

### R14-TK-08（P1，B）§7.1 票务策略状态六态无权威出处
- 五条件自证通过。两处查证：台账 `待启用|票务策略状态` 零命中；canonical 该主题零命中（唯一相关是 FINAL-11 → R14-TK-12）。
- 最小决策面：三份 HTML `票务策略状态` exit=1 零命中；ENUM 全表唯一含「待启用」的是 L229「综合体项目状态」（另一对象）。候选：甲＝确认六态写入 ENUM §一（E-36 可定）；乙＝复用票种 4 态、「待启用/已结束」按生效时间派生；丙＝维持并标注「PRD 自定义、非 HTML 口径」。影响 R14-TK-09、R14-TK-22。

### R14-TK-12（P2，B）「刷新间隔默认 15」——引 canonical `FINAL-11`
- canonical `FINAL-11`（CONFLICT/important/open）已判「10/15/30/60、四档、15 秒三份 HTML 0 命中」；台账 `07-19|#172|V320|ticket_qr_refresh` 零命中——:624 声称的「用户 2026-07-19 拍板」无台账真身。
- 增量：FINAL-11 的 file 列指向红牌文件，**未登本 PRD 6 处落点**——请补登 `:52/:61/:488/:520/:359/:624`。
- 决策面：甲＝「默认 15」逐句批准写入 HTML；乙＝从 PRD 与契约删除、改「出厂默认由后台配置、须同时配置上下限」。

### R14-TK-16（P1，B）积分成本取数时点（TK-S2）——引 `R14-FS-06`，本侧落点
- 本文件同时持有矛盾两侧：:596 整单预扣 vs :521 按人次计＋ENUM L69 未核销不计＋§12A.1 PENDING 机制；`预估` 全文零命中（EV-TK-08）。
- 裁决 5 已定目标状态（预估扣＋差额冲正）→ 本条实为「待修复轮补」：:596 后新增「取数时点」子项 + §10:502 补验收。登 B 而非 A 的理由：**冲正触发点**（核销完成 vs AVAILABLE 转态）与**跨结算期归属**两项 HTML 未写明（INF-TK-01）。

### R14-TK-17（P1，B）积分成本收款方零落点（TK-S3）
- 实查：`结算中心|收款方` 全文零命中；`收款主体` 5 处全为否定式——与裁决 6 **不冲突，是沉默**。
- 落点建议（待 HTML 修订轮落地后补）：:596 后新增「收款方」子项。**不得在 HTML 落地前先写进 PRD。**

### R14-TK-19（P2，B）§11 P1「复杂退款规则引擎」
> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §A-55，本文不再重复记载。

---

## 5. 推断补写清单

**INF-TK-01（P1）票务侧「退款期」时长无定义** —— §12A.1 :547 的 PENDING→AVAILABLE 依赖「退款期结束」，但票务无通用退款窗口；本文件、HTML、台账均未定义票务退款期长度/起算点。后果：AVAILABLE 转态不可判定 → 裁决 5 的差额冲正无锚。最小面：①退款期＝场次结束/有效期届满/独立可配时长？②多人票按人份还是整票？③退款期后 U 盘放行发生时已转 AVAILABLE 的积分按追偿还是冲正（:548 只写「进入追偿流程」未接回）。

**INF-TK-02（P2）域内通票的积分发行成本承担口径** —— ENUM L48/BSM L748 绑定「域间分配份额」共担，而域内通票不做域间拆分；销售渠道费 HTML 已显式补洞（「域内通票同样适用」），积分成本无对应补句。:594 承接了渠道费半句、:596 落空。需裁：全额由该域主管公司承担，还是另设分摊。

**INF-TK-03（P2）通票赠酒下限的参数上下限** —— :175「须同时配置上下限」在 ENUM「通票赠酒下限」条（L32）无对应文字（其他条目确有），疑为平台通例外溢。需确认：①通例是否适用；②上下限具体值（台账 L1040 第 2 条至今未了，不得自拟数字）。

---

## 6. R13 追溯表

`git log --oneline 2bc7cb0f..ff2037a1 -- <本文件>` → **仅 1 个 commit**（EV-TK-02）：`32b63708 (#370)`，经 PR 非直推，+4/−1。

| commit | hunk | 变更 | 追溯 | 结论 |
|---|---|---|---|---|
| `32b63708` | @@ -499 §10 | 新增 :502 积分成本验收句（含 100/10/60:40/54:36 算例） | 能。DEC-R8-87（ledger L1932 含同算例）＋同 commit 的 ENUM L48/BSM L748 现文 | **追得到，算术自洽**（100−10=90；54+36=90 ✓） |
| `32b63708` | @@ -591 §12B.2 | 改写 :596（**含删除类**）。删前原文尾部：「…（**与通路费同机制，照 ENUM「跨域通票分配」条**）。」删后：「（DEC-R8-87）」标注＋算例 | 主体可追溯 DEC-R8-87；**被删内容＝HTML 出处引用** | **删除不造成口径错误但削弱可追溯性**（该口径恰已写进同 commit 的 ENUM L48/BSM L748，删引用后只剩 DEC 编号）。建议修复轮随 R14-TK-14 补回出处。**另一处未删干净**：保留「（基础积分与等级加成积分）」而同批把 ENUM L48/BSM L748 改成无限定广口径——**本 commit 自身即 I-11「ENUM 内部矛盾」的产生现场**，也是 R14-TK-14 的直接成因 |
| `32b63708` | @@ -638 §14 | 变更日志行 | 能，依据列填 DEC 编号 | 无异议 |

**同 commit 对权威源的旁证**（据实登记）：ENUM L48 积分成本句扩写为共担现文（**始终无限定**）；ENUM L69 由「由售票收款主体承担」改为「共担」但**限定语原样保留**——两条自相矛盾即由本次同步产生。另 ENUM「积分结算账期」条删「（账单在商户专区不可查看的期间不计入异议期）」改写为 DEC-R8-88 口径——属 b03/b06 面，只登记观察。

**批准链**：DEC-R8-87 批准真身问题已由 b01 R14-FS-16/DE-17 登记并经**裁决 1「甲：全部追认」**结案；本 shard 不重复登记，按裁决 1 视其为有效 DEC。

---

## 7. CODE-FOLLOWUP 候选

73 条基线中票务相关既有条目：FINAL-02、R11-P0-3、R11-P0-4、EV-07c-03/04——**均已登记不重列**。下列三条不在基线、不在 canonical，为 DELTA 候选：

| 候选 | PRD 证据 | 实施证据（层级如实标） | 侧 |
|---|---|---|---|
| **R14-TK-20**（E-25，P2） | :61/:488「全场统一、5 秒为单位、最长 60 秒」 | `IMPLEMENTATION_INERT`：openapi-p0.yaml:29137-29142 `refresh_interval_seconds enum [10,15,30,60]`；同文件 :8388-8392 配置端点 `minimum:5/maximum:60`——同一契约两套取值域 | contract |
| **R14-TK-21**（P1） | :246-260 §7.2 派生视图矩阵「矩阵之外一律非法」 | `BASELINE_STATE`：V253:5-10 只 CHECK `status IN ('created','cancelled')`，注释明写 pay_status/issue_status「不并入本约束」→ 非法组合库层不拦；「已取消」与「已关闭」库内不可区分 | db-migration |
| **R14-TK-22**（E-36，P2） | :226 策略 6 态与票种 4 态对应关系属待对齐缺口 | `IMPLEMENTATION_INERT`：openapi-p0.yaml:29121-29129 `status enum [draft,active,paused,archived]` | contract |

> R14-TK-22 定性依赖 R14-TK-08 裁决；裁决前只登记不定目标状态。
> 澄清一处避免误报：openapi 中 `TicketTypeRequest:` 两次出现（L26166 requestBodies / L29065 schemas），非重复键，不构成 finding。

---

## 8. Evidence 附录

> 全部命令在 `/home/james/complex-management` 下执行，`@ff2037a1`。管道场景已按 lesson 单独取退出码。无 artifact 落盘能力，超长输出写明可复现命令。

| EV | 类型 | audit_lens | 命令 / file:line | exit @SHA | 关键节选（≤10 行） |
|---|---|---|---|---|---|
| EV-TK-01 | `BASELINE_STATE` | — | `git rev-parse HEAD; git cat-file -t ff2037a1…; git show ff2037a1:docs/ticketing-…md \| wc -lc; git rev-parse ff2037a1:docs/ticketing-…md` | 0 | `e3770efab6b2…`(工作区 HEAD，≠基线) / `commit` / `643 78176` / `2eebe2319bea…` |
| EV-TK-02 | `BASELINE_STATE` | CONFORMANCE | `git log --oneline 2bc7cb0f..ff2037a1 -- docs/ticketing-…md` | 0 | `32b63708 (#370)`（唯一命中） |
| EV-TK-03 | `BASELINE_STATE` | CONFORMANCE | coverage RFC4180 过滤 prd_path 含目标文件 | 0 | `TOTAL 1234`；`TK 102`；`FULL 85 / STALE 14 / PARTIAL 3` |
| EV-TK-04 | `BASELINE_STATE` | CONFORMANCE | 同上 + entry-registry 按 section 归属统计 | 0 | BSM 票务权益 53+3=56（与任务包一致）；14 行 `ORPHANED` 无注册表条目 |
| EV-TK-05 | `HISTORICAL` | CONFORMANCE | arl.csv 取 14 个 ORPHANED rule_id 的 decision_status | 0 | 14/14 `SUPERSEDED`；例 BSM-0063 旧文「通票只能由项目公司售卖…」 |
| EV-TK-06 | `NEGATIVE_SEARCH`＋`BUSINESS_AUTHORITY` | CONFORMANCE | 本 PRD 全文抽 `(BSM\|PGM\|ENUM\|BEQ)「…」条` 29 个引用，逐个回 registry+HTML | 0 | **29/29 存在**；`PGM「解释权声明」`registry 只登 ENUM 侧但 PGM L970 实存，PRD:3 承接无误 |
| EV-TK-07 | `NEGATIVE_SEARCH` | CONFORMANCE | 检索面＝docs/*.md（排 _archive/prd-audit）；式＝`收回其名额\|移出` 与 `没有获批内部结算价\|不得为其开通` | 0（有命中） | 卡座移出→customer-app:60-62；内部结算价闸→settlement-rebuild:762、finance:184/188、merchant-points:177。**判落点分工** |
| EV-TK-08 | `NEGATIVE_SEARCH` | ADVERSARIAL | 本 PRD 全文：`结算中心`/`收款方`/`预估`/`笔数` | 各 1（零命中） | 四式全零；`收款主体` 5 处全为否定式 |
| EV-TK-09 | `NEGATIVE_SEARCH`＋`BUSINESS_AUTHORITY` | CONFORMANCE | `git grep -nE "逐一进行\|一人核验\|代替全体\|按人逐一" ff2037a1 -- 'docs/*.md'` | 0（命中面全为非 PRD） | 仅台账 L1512（DEC-R8-31 列为 14b-11 已交批）与 rounds/r8 changelog；**零 PRD 落点**。HTML 真身 BSM L796 |
| EV-TK-10 | `NEGATIVE_SEARCH` | CONFORMANCE | ①`学生票不等于\|学生票客人同样受` ②`学生`×`成年\|18\|年龄` ③`18 ?(周)?岁\|十八(周)?岁` | ①0 ②0 ③0 | 活文档零命中（与 r12-final retrieval-audit 记录一致，@ff2037a1 复核仍成立）；正面口径落点 pgm-prd:97 |
| EV-TK-11 | `BUSINESS_AUTHORITY` | CONFORMANCE | PRD sed :120;:424;:433;:536；ENUM L44；BSM L784/L798 | 0 | PRD:120「按未履约理由」vs ENUM L44 无条件禁止 vs PRD:424「一律拒绝退款」——同文件两处不一致 |
| EV-TK-12 | `NEGATIVE_SEARCH` | CONFORMANCE | `不解除已开始核销\|不解除.{0,8}退款禁止` @docs/*.md | **1（零命中）** | 全仓零落点；HTML 真身 BSM L784/L798 |
| EV-TK-13 | `BUSINESS_AUTHORITY` | CONFORMANCE | `不得发布上架\|不得上架` @docs/*.md；ENUM L54；PRD:89-93 | 0 | finance:126 已承接「未选定方式或未填写数值的票种不得发布上架」；PRD:89 上架闸只列 3 条 |
| EV-TK-14 | `NEGATIVE_SEARCH` | CONFORMANCE | `不受[^，。]{0,12}场次终结` @docs/*.md；旁证 `补偿通票` | 0（唯一命中为 rounds/r8 changelog） | 零 PRD 落点（amusement:353-362 承接补偿规则但无豁免句）；HTML 真身 BSM L796 末段 |
| EV-TK-15 | `BUSINESS_AUTHORITY` | CONFORMANCE | ENUM L55；BSM L756；PRD:562；coverage BEQ-0132 | 0 | 「全部命中资格价」「最终选中价」vs「全部命中资格」「最终采用价格」 |
| EV-TK-16 | `NEGATIVE_SEARCH` | CONFORMANCE | ①`票务策略状态` @三份 HTML ②ledger `待启用\|票务策略状态` ③ENUM §一 25 条枚举 | ①1（零）②零输出 ③0 | ENUM 唯一含「待启用」的是 L229「综合体项目状态」＝另一对象 |
| EV-TK-17 | `BUSINESS_AUTHORITY` | CONFORMANCE | PRD:473 vs :289 vs ENUM L41 | 0 | 「撤销/冲正」vs「已冲正」 |
| EV-TK-18 | `NEGATIVE_SEARCH`＋`HISTORICAL` | CONFORMANCE | ①三份 HTML `刷新间隔[^<]{0,120}` ②ledger `07-19\|#172\|V320\|ticket_qr_refresh` ③canonical FINAL-11 | ①0（4 命中均无「默认/15」）②零 ③0 | HTML 四处一律止于「5 秒为单位、最长 60 秒」；FINAL-11 open |
| EV-TK-19 | `BUSINESS_AUTHORITY` | ADVERSARIAL | PRD:589-598；BSM L748；ENUM L48 | 0 | BSM「**两者**一律…自然共担」vs PRD 第二层承担方格「按渠道合同…」 |
| EV-TK-20 | `BUSINESS_AUTHORITY`＋`EVIDENCE_CONFLICT` | ADVERSARIAL | ENUM sed 48p;69p；BSM sed 748p；`git show 32b63708 -- <ENUM>` | 0 | L48/BSM L748 无限定语；L69 有限定语。两侧并列不选边——已由裁决 15 消解（广口径） |
| EV-TK-21 | `BASELINE_STATE` | ADVERSARIAL | `git show 32b63708 -- docs/ticketing-…md` | 0 | 3 hunk（+4/−1）；`-…（与通路费同机制，照 ENUM「跨域通票分配」条）` / `+…（DEC-R8-87）（基础积分与等级加成积分）——…` |
| EV-TK-22 | `BUSINESS_AUTHORITY` | ADVERSARIAL | PRD :172;:224;:390;:578;:579 并置 | 0 | :390「场次结束即终结」vs :578「有效期内跨期仍可核销」 |
| EV-TK-23 | `HISTORICAL` | FIRST_PRINCIPLES | PRD:426;:508;:430-433；arl BSM-0067（SUPERSEDED） | 0 | :508 P1 表仍列「复杂退款规则引擎」；退款已穷举五种 |
| EV-TK-24 | `IMPLEMENTATION_INERT` | FIRST_PRINCIPLES | openapi sed 29130-29148 与 8385-8396 | 0 | `enum [10,15,30,60]` vs `minimum:5/maximum:60` |
| EV-TK-25 | `BASELINE_STATE` | FIRST_PRINCIPLES | V253__status_check_constraints.sql | 0 | 「pay_status/issue_status…不并入本约束」；`CHECK (status IN ('created','cancelled'))` |
| EV-TK-26 | `IMPLEMENTATION_INERT` | FIRST_PRINCIPLES | openapi `TicketTypeRequest.status` | 0 | `enum draft/active/paused/archived`；「AUD-015 起服务端不再采信该字段」 |

---

### 交 Consolidator 的三条事项

1. **`R14-FS-10` 已按裁决 15 撤销**，其「ticketing:596 保留限定」侧不再作镜像漂移，按 `R14-TK-14` 作**待删 A 项**处理。三落点齐：finance:76、settlement-rebuild:100（丢限定＝符合广口径，无需改）、ticketing:596（保留限定＝待删）。
2. `R14-TK-16` 与 `R14-TK-17` 请与 `R14-FS-06`、`R14-SR-09` 合并成条，本 shard 只提供 ticketing 侧落点与零命中证据（EV-TK-08）。b01-SR 的「ticketing §12A」指向需更正为 **§12B.2**。
3. `R14-TK-11`（BEQ-0124、BSM-0070）与 `R14-TK-12`（FINAL-11 落点补登）都**不新增缺陷本体**，请按「补登落点」处置，勿与内容缺失合并计数。

---

*编排者落盘备注：TK 子代理返回报告的原样落盘（HTML 实体已还原）。落盘时间 2026-08-20。*
