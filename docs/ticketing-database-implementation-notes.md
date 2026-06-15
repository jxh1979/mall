# 票务与权益数据库落地说明

对应迁移文件：

[database/migrations/002_ticketing_entitlement_settlement.sql](../database/migrations/002_ticketing_entitlement_settlement.sql)

> 修订说明（2026-06-08 复核修复）：该迁移实体文件已随当前交付包存在；本轮已补入 `ticket_credentials`、`ticket_inventories.project_id/version`、`refund_orders.refund_direction/original_payment_channel/refunded_by/project_id`。下文"已落地表清单"按修复后的迁移实体文件为准，链接可作为已交付 SQL 的索引。依据 finding overclaim-6 的判后修复。

## 1. 落地策略

本次先使用 PostgreSQL DDL 落地票务专项表，不直接写 Java JPA 实体。

原因：

1. 票务、权益、核销、结算是强交易模型，先用 SQL 明确边界更稳。
2. 地基系统表还在独立线程维护，当前迁移只通过 `merchant_id`、`venue_id`、`open_app_id` 等字段建立逻辑关系，暂不强制外键依赖。
3. 后续 Java Spring Boot 项目建立后，可以直接基于本迁移生成 JPA Entity、MyBatis Mapper 或 jOOQ 代码。

## 2. 已落地表清单

| 表 | 作用 |
|---|---|
| `ticket_products` | 票务产品 |
| `entitlement_templates` | 权益模板 |
| `product_entitlement_templates` | 票务产品与权益模板关系 |
| `ticket_inventories` | 票库存 |
| `sales_channels` | 销售渠道 |
| `ticket_orders` | 票务订单 |
| `order_items` | 订单明细 |
| `issued_tickets` | 已出票实例 |
| `issued_entitlements` | 已发放权益实例 |
| `ticket_credentials` | 票码/凭证绑定与防伪核验 |
| `verification_records` | 核销记录 |
| `payment_records` | 支付流水 |
| `refund_orders` | 退款单 |
| `settlement_rules` | 结算规则 |
| `settlement_items` | 结算明细 |
| `settlement_bills` | 结算单 |
| `settlement_bill_items` | 结算单与明细关系 |
| `reconciliation_records` | 对账记录 |

> 修订说明（2026-06-13 收口）：上表仅表示历史迁移 `002_ticketing_entitlement_settlement.sql` 已落地表，不等于正式 Java/PostgreSQL/Flyway 首批业务迁移完整边界。按《统一口径基准》D9（门票资格 P0），`ticket_eligibility_rules` 与 `student_credential_audits` 已在开发设计 §6.2.1.1/§6.2.1.2 定为执行侧权威字段，必须纳入正式 Flyway 首批业务迁移或其等价迁移，不再作为开放待决策项。依据 finding ticket-1/2/3 与本轮对抗式审计收口。
>
> 修订说明（2026-06-08 三轮收口）：`ticket_inventories`（票库存）为**票务库存唯一权威**（本迁移 002），foundation §6B.6 的 `ticket_inventory`(单数)仅作"指针：以本表为权威，不重复建表"。本表已补 `version`（乐观锁版本号）承载库存桶并发口径，完整字段以 settlement-breakdown §8.4 为准。依据 R1、finding newtable-fk-1。
>
> 修订说明（2026-06-08 三轮收口）：`refund_orders`（退款单）为**退款唯一权威**（p0-task-list DB-014 的 refund_records 统一改指本表）；已补退款资金流向字段 `refund_direction`/`refunded_by`/`original_payment_channel`，完整字段以 settlement-breakdown §8.12 为准。依据 R2、finding fix2-conflict-2。

## 3. 关键设计说明

### 3.1 通票不是一张大票

通票在系统里不是一个不可拆分对象，而是：

```text
ticket_products
  -> product_entitlement_templates
  -> entitlement_templates
```

游客购买后生成：

```text
ticket_orders
  -> order_items
  -> issued_tickets
  -> issued_entitlements
```

这样一张票里可以同时包含平台、酒吧、游乐、餐饮、零售等不同商户权益。

### 3.2 核销必须核销权益，不直接核销订单

核销记录绑定：

```text
verification_records.issued_entitlement_id
verification_records.issued_ticket_id
```

这可以避免以下问题：

- 一个订单有多个权益时无法判断哪个权益被使用。
- 酒吧和游乐权益混在一起结算。
- 部分退款无法判断哪些权益可退。
- 第三方回传核销缺少归属。

### 3.3 结算明细来源可追溯

结算不是直接从订单金额粗暴分账，而是先生成：

```text
settlement_items
```

每条结算明细可以来自：

- 销售
- 核销
- 退款
- 调整
- 补贴

并可关联：

- 订单
- 权益
- 核销记录
- 退款单

之后再汇总生成：

```text
settlement_bills
settlement_bill_items
```

### 3.4 状态字段采用字符串 + check约束

当前迁移没有使用 PostgreSQL enum，而是使用 `varchar + check constraint`。

原因：

- 后续业务状态可能扩展。
- enum 修改成本较高。
- Java 服务里可以用枚举类统一约束。

## 4. 与其他系统关系

| 系统 | 关系 |
|---|---|
| 多商户系统 | `merchant_id`、`owner_merchant_id`、`venue_id`、`project_id` 逻辑关联 |
| 开放平台 | `open_app_id` 用于第三方渠道、第三方核销回传 |
| 事件中心 | 售票、支付、出票、权益发放、核销、退款、结算都要写入事件 |
| 权限系统 | 商户后台只能查自己 `merchant_id` 范围内的权益、核销、结算 |
| 酒吧系统 | 酒吧入场、酒水兑换通过权益项接入；卡座预约入场名额由酒吧预约/卡座系统校验，不作为票务低消抵扣权益 |
| 游乐系统 | 游乐次数、游乐通玩、快速通道通过权益项接入 |
| 餐饮/零售第三方 | 餐饮券、零售券通过开放平台回传核销 |

## 4A. AI-ready 四件套执行入口

本文是票务与权益数据库落地说明，不能替代正式 Java/Spring/PostgreSQL/Flyway 工程、OpenAPI 契约或验收矩阵。后续编码必须按下表执行。

| 四件套 | 本文执行要求 | 权威引用 |
|---|---|---|
| 反模式 | 不得绕过正式 Flyway 迁移直接改库；不得把通票当作一张大票；不得直接核销订单；不得启用 P0 离线核销；不得把卡座权益、最低消费和票务权益混为一体 | `ticketing-pass-entitlement-development-design.md` §3/§5/§6/§7、`00-current-effective-document-entry-v0.1.md` |
| 测试用例 | 必须覆盖出票拆权益、库存扣减、在线核销、退款冲正、结算明细、渠道回流、多人票同一时段全部核销和学生/新人资格留痕 | `p0-user-story-map-and-sprint-slices-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| 错误处理 | 必须覆盖库存不足、并发超卖、重复支付回调、重复核销、二维码过期/重放、退款状态冲突、第三方回传重复/漏发和结算差异 | `p0-api-contract-pack-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md` |
| 精确引用 | 表、字段和接口必须引用正式 Flyway 文件、OpenAPI exact path、operationId 或追溯附录条目；不得继续引用旧 TypeScript/SQLite/Prisma 原型结构 | `database/migrations/002_ticketing_entitlement_settlement.sql`、`docs/openapi/openapi-p0.yaml`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md` |

## 5. 后续开发顺序

建议 Java 后端按这个顺序开发：

1. 票务产品管理。
2. 权益模板管理。
3. 票务产品绑定权益。
4. 创建订单。
5. 模拟支付成功。
6. 出票并拆分权益。
7. 核销前校验。
8. 执行核销。
9. 生成结算明细。
10. 生成结算单。
11. 所有步骤写入事件中心。

## 6. 正式工程补充事项

正式 Java/PostgreSQL/Flyway 首批业务迁移必须按当前权威口径补齐以下事项；这些不是新的产品拍板问题，而是 P0 编码前置口径：

- 退款权益明细表。
- 支付机构回调表。
- 第三方渠道订单映射表。
- 离线核销同步表（P1 预留，P0 不启用离线核销）。
- 票码/NFC绑定表 `ticket_credentials` 已在本迁移中补入；后续仅需在正式 Java/Flyway 工程中复核字段顺序、索引命名与权限约束。
- 结算调整审批表。
- 对账文件导入表。
- 资格规则表 `ticket_eligibility_rules`（新人/学生/多人资格判定，D9，字段见开发设计 §6.2.1.1），P0 必须落入正式 Flyway 迁移；不得继续只停留在历史 SQL 说明或策略侧聚合视图。
- 学生凭证留痕表 `student_credential_audits`（`customer_id`/`verify_method`/`credential_ref`/`verified_by`/`result`/`verified_at`，D9，字段见开发设计 §6.2.1.2），P0 必须落入正式 Flyway 迁移；只存凭证引用/哈希和审核结果，不存原始证件影像明文。

此外，`product_entitlement_templates` 与 `issued_entitlements` 需补 `num_people` 字段（多人票核销人数，默认 1），核销记录需支持记录放行人数，落地 D9"多人票同一时段全部核销"。

## 7. 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-06-13 | 改 | §2 表清单注 | 将历史资格表补充口径改为“正式 Flyway 首批业务迁移必须纳入”，并声明不再作为开放待决策项 | 防止开发把门票资格 P0 执行侧权威误判为未定；本轮对抗式审计收口 |
| 2026-06-07 | 增 | §6 正式工程补充事项 | 新增 `ticket_eligibility_rules`、`student_credential_audits` 两表，并提示 `num_people` 字段补充 | 落地 D9 资格与多人票；finding ticket-1/3/4 |
| 2026-06-07；2026-06-08 复核修复 | 注/改 | 顶部迁移链接 | 原加注迁移实体 002_*.sql 缺少随包交付；本轮复核后实体 SQL 已随包存在并补齐关键字段，链接改为已交付 SQL 索引 | 缺口诚实度与判后修复闭环；finding overclaim-6 |
| 2026-06-07；2026-06-08 复核修复 | 改 | §6 票码/NFC绑定表 | 原明确 `ticket_credentials` 字段以开发设计 §6.2.3 为准；本轮已补入迁移实体，后续仅需正式工程复核 | 权威源落空收口与判后修复；finding dup-tables-6、overclaim-6 |
| 2026-06-08 | 注 | §2 表清单注 | `ticket_inventories` 为票务库存唯一权威(foundation §6B.6 仅指针)，已补 `version` 乐观锁；完整字段见 settlement-breakdown §8.4 | 票务库存唯一权威；R1、finding newtable-fk-1 |
| 2026-06-08 | 注 | §2 表清单注 | `refund_orders` 为退款唯一权威(DB-014 refund_records 改指本表)，已补 refund_direction/refunded_by/original_payment_channel | 退款唯一权威+资金流向字段；R2、finding fix2-conflict-2 |
| 2026-06-12 AI-ready 收口 | 增 | §4A | 新增数据库落地说明四件套执行入口，约束 Flyway、通票拆权益、在线核销、库存并发、退款冲正和精确引用 | 防止 SQL 说明脱离正式 Java/Flyway/OpenAPI 验收链路 |
| 2026-06-13 OpenAPI 追溯闸口 | 改 | §4A | 精确引用补入 `p0-openapi-contract-closure-appendix-v0.1.md`，表、字段和接口必须能追溯 operation、权限、事件、终端和样例准入 | 防止票务 SQL 说明脱离正式接口追溯链路 |
