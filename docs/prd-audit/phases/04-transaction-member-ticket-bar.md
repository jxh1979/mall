# 阶段 4：交易、会员、票务与酒吧审计

状态：**阶段 4 完成（文档审计）** — 覆盖+对抗已归档；积分域已由会员积分主 PRD/P0/P1 收口；残余非积分 GAP 见下表 OPEN/DEFERRED
日期：2026-07-28
基线提交：`b433fda2`
分支：`audit/prd-phase4-transaction`
工作树：`/home/james/wt-prd-phase4`

## 范围

- 本阶段新审计规则：**299** 条（已排除 phase1–3 coverage 中规则）。
- 领域：bar / ticketing / member / finance / customer / dividend 及带交易权益关键词的 cross-domain 等。
- 权威：三份业务 HTML；MAIN PRD 承接。
- 已知决策：`BAR-VOID-A1`（文档已定，编码未改）。

### 领域分布
| domain | count |
|---|---:|
| bar | 83 |
| ticketing | 74 |
| finance | 54 |
| dividend | 26 |
| customer | 22 |
| terminal | 14 |
| member | 11 |
| service | 8 |
| merchant | 7 |

## 覆盖统计（本阶段新增）
| status | count |
|---|---:|
| FULL | 258 |
| PARTIAL | 35 |
| MISSING | 6 |

| severity | count |
|---|---:|
| P1 | 20 |
| P0 | 14 |
| P2 | 7 |

coverage 总行数（含历史）：**601**

## 价值对象守恒式

形式：`期初 + 合法增加 - 合法减少 = 期末 + 冻结/在途`

| 对象 | 确认时点 | 逆向 | 备注 |
|---|---|---|---|
| 支付实收 | 支付成功+对账 | 退款/冲正/原路 | 责任/结算主体合同绑定 |
| 储值本金/赠金 | 充值/发放确认 | 本金可退；赠金按规则 | 混退顺序见 ADV-TX-003 |
| 积分 | 业务确认入账 | 退款回退/过期/兑换 | 清算未决见 ADV-TX-004 |
| 券/票/权益 | 领取出票/核销 | 退回过期撤销 | 状态机见 ADV-TX-005/006 |
| 卡座低消名额提成 | 开台转续确认 | 重算冲提成 | ADV-TX-007 |
| 酒水库存 | 出品扣减 | 退酒回库/损耗 | A1 高价值先审后行 |
| 分账在途 | 结算批次 | 失败回滚 | ADV-TX-009 守恒 |

## 强制对抗场景
| ID | 场景 | 严重度 | 结果 | 缺口摘要 | 建议 |
|---|---|---|---|---|---|
| ADV-TX-001 | 重复支付、重复退款、部分退款、跨支付方式退款 | P0 | COVERED |  | 在票务/财务 PRD 明确重复支付解控、重复退款拒绝、部分退对象粒度、跨方式退款顺序与原路优先 |
| ADV-TX-002 | 退款发起人与复核人为同一人 | P0 | COVERED |  |  |
| ADV-TX-003 | 储值本金与赠送金混退、账户注销和余额迁移 | P0 | GAP | 本金/赠送金区分不足; 混退顺序与赠送金可否退未明确;  | 固定退款顺序、赠送金是否可退、注销/迁移时本金赠金处理 |
| ADV-TX-004 | 积分冻结/确认/兑换/退款/过期/商户退出/跨主体清算 | P0 | PARTIAL | 缺清算; 缺商户退出;  | 补齐积分全生命周期与跨主体清算、商户退出结清 |
| ADV-TX-005 | 券已领取/冻结/核销/退回/过期 | P1 | COVERED |  | 建券状态机与非法迁移拒绝表 |
| ADV-TX-006 | 门票复制/截图/动态码过期/部分核销/撤销 | P0 | COVERED |  | 明确防伪与动态码TTL、截图重放拒绝、部分核销粒度、撤销条件与权限 |
| ADV-TX-007 | 卡座转卡/合并/续卡后的低消/名额/销售归属/提成 | P1 | GAP | 缺转卡; 缺合并; 缺续卡;  | 固定转卡合并续卡后的低消继承、名额重算、销售与提成归属 |
| ADV-TX-008 | 订单跨营业日/跨月/跨结算周期 | P1 | PARTIAL | 缺营业日;  | 统一营业日切日点、跨日订单归属、跨月退款进入哪期结算 |
| ADV-TX-009 | 分账与清算总额/费用/责任主体不一致 | P0 | COVERED |  | 写守恒式：订单实收=分账之和+费用；主体绑定失败阻断 |
| ADV-TX-010 | 酒吧出品/库存/退单/赠酒不守恒 | P0 | PARTIAL | 实现仍可能先生效后审(代码未改);  | 文档已定A1；编码另开。补出品-库存-赠酒-退单守恒式与对账抽检 |
| ADV-TX-011 | 高价值退酒申请人与审批人同一人 | P0 | COVERED |  | 保持void_decide与自批禁令；实现强制 |

### 摘要
- 文档锚点较好：BAR-VOID-A1、部分退款/核销/库存关键词。
- 需补强：部分退与重复支付退款、储值混退注销迁移、券状态机、票防伪/部分核销、卡座转并续、分账守恒。
- **阻塞未决**：积分兑换券是否先冻结至核销、净积分计价（ADV-TX-004）——需单问题确认。

## 方法局限
检索启发式映射 + 维度检查 + 对抗抬升；`FULL` 不代表实现已上线。

## 非 FULL 样例
| rule_id | domain | status | sev | finding | PRD | summary |
|---|---|---|---|---|---|---|
| BSM-0130 | terminal | MISSING | P1 | TX-BSM-0130-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| BSM-0132 | terminal | MISSING | P1 | TX-BSM-0132-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| BSM-0135 | terminal | MISSING | P1 | TX-BSM-0135-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| BSM-0142 | terminal | MISSING | P1 | TX-BSM-0142-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| PGM-0075 | terminal | MISSING | P1 | TX-PGM-0075-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| PGM-0077 | terminal | MISSING | P1 | TX-PGM-0077-MISS | docs/product-requirements-v0.1.md | MAIN PRD 中未检索到足够承接表述 |
| BSM-0027 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |
| BSM-0029 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |
| BSM-0032 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |
| BSM-0033 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |
| BSM-0039 | ticketing | PARTIAL | P0 | ADV-TX-004 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 积分冻结/确认/兑换/退款/过期/商户退出/跨主体清算 => GAP: 缺清算; 缺商户退出;  |
| BSM-0041 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |
| BSM-0042 | ticketing | PARTIAL | P2 | TX-BSM-0042-DIM | docs/ticketing-entitlement-checkin-product-prd-v0.1.md | 已有部分承接，但缺少: cross_period |
| BSM-0043 | ticketing | PARTIAL | P0 | ADV-TX-010 | docs/settlement-rebuild-prd-v1.0.md | 酒吧出品/库存/退单/赠酒不守恒 => PARTIAL: 实现仍可能先生效后审(代码未改);  |

## 待批准 PRD 修订清单（未改正文）
| finding | sev | 目标文档 | 建议变更 | 需先裁决 |
|---|---|---|---|---|
| ADV-TX-003 | P0 | member; stored-value | 固定退款顺序、赠送金是否可退、注销/迁移时本金赠金处理 | no |
| ADV-TX-004 | P0 | member; decision-log | 补齐积分全生命周期与跨主体清算、商户退出结清 | no |
| ADV-TX-007 | P1 | bar | 固定转卡合并续卡后的低消继承、名额重算、销售与提成归属 | no |
| ADV-TX-008 | P1 | finance; bar; ticketing | 统一营业日切日点、跨日订单归属、跨月退款进入哪期结算 | no |
| ADV-TX-010 | P0 | bar (code later) | 文档已定A1；编码另开。补出品-库存-赠酒-退单守恒式与对账抽检 | no |

## 请你确认的下一个业务问题（仅 1 个）

> 每个**净积分**对应的费用金额/计价方式如何确定？
>
> 请直接给可执行规则（示例方向，非选项限制）：
> - 固定单价（请给出金额与币种/单位）；
> - 按活动/商户/积分类型差异化（请说明维度与默认值）；
> - 由清算中心按账期结算价（请说明谁定价、何时生效）；
> - 或其他（请写明公式）。


## PR #218 收口说明（2026-07-28）

### 本 PR 保留内容
- `docs/prd-audit/html-prd-coverage.csv` phase4 覆盖行
- `docs/prd-audit/phase4-*.csv/json`
- `docs/prd-audit/phases/04-transaction-member-ticket-bar.md`
- `docs/prd-audit/prd-audit-report.md` 进度更新

### 与 main 会员积分重构的关系（docs-only，已在 main）
- 主 PRD：`member-points-tier-and-clearing-prd-v1.0.md`
- P1 参数表：`member-points-p1-ops-parameter-tables-v0.1.md`（A–K 已裁）
- 实现切片（规划文档）：`superpowers/plans/2026-07-28-member-points-tier-clearing-implementation-slices.md`
- **POINTS-VOUCHER-ACCRUAL-002**：到店核销才记商户承接（废止本分支曾写的 ACCRUAL-001）
- **DEC-CFG-ALL-NUMERIC-001**：时间/数字一律可配
- 本审计线程约定：**不改代码**，仅文档

### 阶段 4 结论（收口）
- 交易/会员/票务/酒吧规则覆盖初评已完成并写入 coverage
- 强制对抗场景矩阵已归档
- 积分域结构性未决已通过 R3/P0/P1 决策与主 PRD 收口；残余为运营填数与**未来编码**（另许可）
- 非积分 GAP（如储值混退 ADV-TX-003、卡座转并续 ADV-TX-007 等）仍可在后续文档迭代处理，不阻塞本 PR 合并

### 下一步（文档或编码均需另指令）
- 阶段 5+ 审计 或 积分实现切片编码（编码需明确许可）


## 阶段 4 完成清单（文档）

| 计划步骤 | 状态 |
|---|---|
| 覆盖矩阵 | 完成（299 条） |
| 价值守恒式 | 完成（见上文） |
| 强制对抗场景 | 完成（11 项，见 phase4-adversarial-findings.csv） |
| 业务单问题 | 积分承接已裁决（ACCRUAL-002）；其余非阻塞 GAP 登记 |
| PRD 正文大规模修订 | **不做自动批改**（审计约定 docs-only；积分重构已另 PR 合并） |
| 代码 | **不在范围** |

### 阶段 4 仍 OPEN / DEFERRED（文档债，不阻塞标完成）

| ID | 主题 | 处置 |
|---|---|---|
| ADV-TX-003 | 储值本金/赠金混退与注销迁移 | DEFERRED：写入后续会员/储值文档迭代，需业务补规则后改 PRD |
| ADV-TX-007 | 卡座转并续后低消/提成 | DEFERRED：酒吧 PRD 补生命周期规则 |
| ADV-TX-008 | 跨营业日/周期 | DEFERRED：财务/票务/酒吧切日统一 |
| ADV-TX-010 | 酒吧守恒/A1 编码 | DOC 已定 BAR-VOID-A1；**编码另许可** |
| MISSING 行 | phase4-issue-rows | 保持 PARTIAL/MISSING 跟踪，后续精准补文 |

### 阶段 4 完成声明
- 完成日：2026-07-28
- 分支收口依据：main 已含 phase4 产物 + 会员积分重构文档
- 本文件随 phase4+5 收口提交再次确认 **COMPLETE**

