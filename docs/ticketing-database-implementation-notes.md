# 票务与权益数据库落地说明 V0.09

对应迁移文件（正式 Flyway，权威源）：

`backend/src/main/resources/db/migration/` —— 票务/权益相关主迁移：`V006__p0_ticketing_verification.sql`、`V009__p0_ticketing_customer_orders_credentials.sql`、`V031__p1_ticket_refund_line_ledger.sql` 等。

> 修订说明（2026-06-28 收口）：早期原型 SQL `database/migrations/002_ticketing_entitlement_settlement.sql` **不在 Flyway 路径**（`backend/.../application.yml: locations=classpath:db/migration`）、非权威，**已删除**。票务/权益的表、字段一律以上述正式 Flyway 迁移为准；下文"已落地表清单"同样按正式 Flyway 迁移为准。

## 1. 落地策略

当前仓库已采用 Java Spring Boot + PostgreSQL + Flyway；本文件只记录已落地数据库事实与尚未落地的明确缺口，不把逻辑目标表写成现行物理表。

执行原则：

1. 物理表、字段与约束只认正式 Flyway；Java 当前使用 JDBC Repository，不宣称尚未存在的 JPA Entity。
2. `merchant_id`、`venue_id`、`open_app_id` 等关系按现有迁移决定是否使用外键，不从旧原型推断。
3. 逻辑目标与当前实现分栏记录；缺迁移或缺代码时明确标为缺口。

## 2. 当前正式 Flyway 已落地表

| 表 | 作用 |
|---|---|
| `ticket_types` | 票种／票务产品 |
| `ticket_price_rules` | 价格规则 |
| `ticket_dynamic_pricing_rules` | 动态价格规则 |
| `entitlement_templates` | 权益模板 |
| `ticket_type_entitlements` | 票种与权益模板关系 |
| `ticket_orders` | 票务订单 |
| `ticket_payment_intents` | 支付意图 |
| `ticket_instances` | 完整票实例 |
| `issued_entitlements` | 已发放权益实例 |
| `ticket_credentials` | 票码/凭证绑定与防伪核验 |
| `ticket_verifications` | 核销记录 |
| `ticket_refunds` | 退款单 |
| `ticket_refund_lines` | 退款明细 |
| `settlement_transactions` | 结算交易流水 |
| `settlement_items` | 结算明细 |
| `settlement_bills` | 结算单 |
| `settlement_reconciliation_status` | 结算对账状态 |

> 上表只列当前仓库可由正式迁移定位的物理表。`ticket_eligibility_rules`、`student_credential_audits` 等目标表尚无正式迁移时只能列为缺口，不得写成已落地。
>
> 当前票务库存由 `ticket_types.stock_quantity/sold_quantity`（V133）及 Java 守卫式原子更新承载；仓库没有 `ticket_inventories` 物理表。
>
> 当前退款物理表为 `ticket_refunds` / `ticket_refund_lines`。请求通过 `ticket_instance_ids` 选择 `ticket_instances`，再按 `issued_entitlements.parent_ticket_id` 展开整票全部权益；当前代码仍缺订单内任一票已核销即整单拒绝，以及分级自助路径申请审批人分离，须按 HTML 修复。
>
> 当前 `ticket_verifications.status` 的 V067 约束仍为 `success/failed/rejected/reversed/manual_overridden`，缺少业务六态中的 `pending_review/exception`，并保留 HTML 禁止的人工放行技术态；Java 仍有核销 `preview`、`manual-override` 和终端 `offline_verification` 入口。上述均为待修实现缺口，不得反向定义“扫码即核销、无人工放行、必须在线核验”的业务规则。
>
> 当前 `TicketOrderExpiryJob` 的未支付超时 Java 缺省值仍为 30 分钟，生产配置未覆盖为 HTML 规定的出厂默认 5 分钟，也未承载 1 至 30 分钟配置边界校验，须作为实现缺口修复。

## 3. 关键设计说明

### 3.1 通票不是一张大票

通票在系统里不是一个不可拆分对象，而是：

```text
ticket_types
  -> ticket_type_entitlements
  -> entitlement_templates
```

游客购买后生成：

```text
ticket_orders
  -> ticket_instances
  -> issued_entitlements（parent_ticket_id）
```

这样一张票里可以同时包含酒吧、游乐、餐饮、零售等不同经营域权益；每项按域主管公司和实际提供商户记录归属。

### 3.2 核销必须核销权益，不直接核销订单

核销记录绑定：

```text
ticket_verifications.entitlement_id
issued_entitlements.parent_ticket_id -> ticket_instances.id
```

这可以避免以下问题：

- 一个订单有多个权益时无法判断哪个权益被使用。
- 酒吧和游乐权益混在一起结算。
- 订单完全未核销时，无法判断被选中退票实例下哪些权益须一并冲正。
- 第三方回传核销缺少归属。

### 3.3 结算明细来源可追溯

结算不是直接从订单金额粗暴分账。票务售卖成功只形成收款、出票和价格快照；实际提供商户的结算从核销事实进入：

```text
ticket_verifications
  -> settlement_transactions
```

每条结算交易可以来自：

- 核销
- 到期未核销归属
- 退款冲正

并可关联：

- 订单
- 权益
- 核销记录
- 退款单

之后按账期与已生效的结算配置汇总生成：

```text
settlement_bills
settlement_items
```

### 3.4 状态字段采用字符串 + check约束

当前迁移没有使用 PostgreSQL enum，而是使用 `varchar + check constraint`；Java 服务与数据库约束必须保持同一值域，不得把内部工作流技术值对外扩成新的中文业务状态。

## 4. 与其他系统关系

| 系统 | 关系 |
|---|---|
| 多商户系统 | `merchant_id`、`owner_merchant_id`、`venue_id`、`project_id` 逻辑关联 |
| 开放平台 | `open_app_id` 用于第三方渠道、第三方核销回传 |
| 事件中心 | 售票、支付、出票、权益发放、核销、退款、结算都要写入事件 |
| 权限系统 | 商户后台只能查自己 `merchant_id` 范围内的权益、核销、结算 |
| 酒吧系统 | 酒吧入场、酒水兑换通过权益项接入；卡座预约入场名额由酒吧预约/卡座系统校验，不作为票务低消抵扣权益 |
| 游乐系统 | 游乐次数、游乐通玩通过权益项接入 |
| 餐饮/零售第三方 | 折扣型权益由平台完成核销并写平台核销记录，第三方 POS 只录入折扣率或扣减金额、不逐张回传；券核销型才由第三方 POS 通过开放平台逐张回传 |

## 4A. AI-ready 四件套执行入口

本文是票务与权益数据库落地说明，不能替代正式 Java/Spring/PostgreSQL/Flyway 工程、OpenAPI 契约或验收矩阵。后续编码必须按下表执行。

| 四件套 | 本文执行要求 | 权威引用 |
|---|---|---|
| 反模式 | 不得绕过正式 Flyway 迁移直接改库；不得把通票当作一张大票；不得直接核销订单；不得启用离线核销；不得把卡座权益、最低消费和票务权益混为一体 | `ticketing-pass-entitlement-development-design.md` §3/§5/§6/§7、`00-current-effective-document-entry-v0.1.md` |
| 测试用例 | 必须覆盖出票拆权益、库存扣减、在线核销、退款冲正、结算明细、渠道回流、多人票同一场次内分别核销和学生/新人资格留痕 | `p0-user-story-map-and-sprint-slices-v0.1.md`、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| 错误处理 | 必须覆盖库存不足、并发超卖、重复支付回调、重复核销、二维码过期/重放、退款状态冲突、第三方回传重复/漏发和结算差异 | `p0-api-contract-pack-v0.1.md`、`p0-action-event-evidence-matrix-v0.1.md`、`third-party-supplier-integration-kit-v0.1.md` |
| 精确引用 | 表、字段和接口必须引用正式 Flyway 文件、OpenAPI exact path、operationId 或追溯附录条目；不得继续引用旧 TypeScript/SQLite/Prisma 原型结构 | `backend/src/main/resources/db/migration/V009__p0_ticketing_customer_orders_credentials.sql`、`docs/openapi/openapi-p0.yaml`、`p0-openapi-contract-closure-appendix-v0.1.md`、`p0-task-api-permission-event-acceptance-map-v0.1.md` |

## 5. 当前数据库收口顺序

票种、权益、订单、支付意图、出票、动态凭证、在线核销、退款和结算主表均已落地，不再列为“后续建模”。本模块只按当前缺口收口：

1. 修正到期技术值 `forfeited` 与对客“已过期”的映射。
2. 在退款入口增加订单级核销守卫，并保证 `requested_by` 与 `approved_by` 职责分离。
3. 补齐演出票、大众点评及会员/渠道/动态等价格命中资格的可追溯快照。
4. 按冻结 HTML 补齐两段池、跨域份额、服务费、免费权益成本、尾差和收款/收入分离的持久化与金额守恒验证。

## 6. 当前落地与补充事项

已落地事实：

- 退款明细由 `ticket_refund_lines`（V031）承载；退款请求选择完整 `ticket_instances`，内部按 `parent_ticket_id` 展开全部权益。
- 动态二维码凭证表 `ticket_credentials` 由 V009/V038/V064/V066/V320 演进；数据库不持久化可复用展示 token 明文，生产 Java 已以 HMAC-SHA256 签发和校验，签名缺失或不匹配时拒绝核销。手环只记录当前场次内允许入场，不作为首次检票凭证。
- `settlement_transactions` 已承载核销与到期未核销交易，但现行 `unverified_expired + beneficiary_type=project` 无法按票种经营域区分娱乐平台公司、游乐公司、城市项目公司或传媒公司，须作为实现缺口收口。

尚未落地或尚未形成正式迁移的目标必须继续标为缺口：支付机构回调、第三方渠道订单映射、结算调整审批、对账文件导入，以及新人/学生/多人资格与学生审核留痕的执行侧载体。学生审核载体只保存凭证引用/哈希、审核结果、审核人与时间，不保存原始证件影像明文。

`ticket_type_entitlements.num_people`（V076）、`issued_entitlements.num_people` 与 `ticket_verifications.verified_people`（V067）已经落地；多人票各人须在同一场次内完成入场，但可在该场次内分别核销，不要求同一时刻。

数据库与约束必须承载下列冻结 HTML 现行规则：

- 派对票与活动票等价；票种包括单日票、夜场票、演出票；赠票作为票务产品类型。
- 票种目录与接口须覆盖平日票、周末票、节假日票、成人票、学生票、多人票、新人票、一人多次票、单日票、夜场票、酒吧票、游乐票、演出票、派对票/活动票、各类通票和赠票；当前 `ticket_types.ticket_category` 只覆盖 `standard/new_person/student`，不能冒充完整票种枚举。产品持久化状态只用草稿、已上架、暂停、已归档，售罄由 `stock_quantity/sold_quantity` 派生展示。
- 票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、暂停售卖（临时停止售卖）、已结束（售卖周期结束）、归档（历史策略）。
- `entitlement_templates.entitlement_type` 须覆盖入场、游乐、酒水、卡座免通票入场名额、餐饮券、零售券、服务权益、积分兑换虚拟券和酒吧简餐权益。简餐套餐作为餐饮券或酒吧简餐权益。
- 有效期只取永久、固定天数、按自然月、按自然年、按场次；有效期按日票、周票、月票、年票等票种规则配置，票务二维码按日票、周票、月票、年票等票种设置有效期；类型和值必须显式配置，缺失时拒绝发布或出票，不得回退 1 天。
- 综合体入场依次经过安检、年龄核验、检票三道环节；前两道不产生票务核销记录，检票环节扫码即核销。安检设备未安装或故障时改为人工开包、目视查验，数据库不得生成按人安检记录，只记录设备状态、故障事件和维修工单。年龄核验故障抽查不得写成已完成年龄核验；误拒放行的一次性凭据不含身份信息、核销即作废且不可复用。
- 一个场次是一次开门到关门，跨夜场次以实际闭店时间为准，不按自然日切分；完成检票后绑定当前场次，同一场次内可重复进出；有手环则当前场次内允许入场。场次结束时，已完成检票但未核销的附带权益按场次终结制处理；多人票中从未完成检票的人份按「到期未核销款」于该场次结束时处理，两者分别立项、不合并。
- 一人多次票每完成一次检票环节消耗一次入场次数并开启一个独立场次窗口；剩余入场次数跨场次保留至用尽或到期，前后场次之间不得挪用权益。
- 大楼门票检查或权益核验系统进入营业兜底时，记录免票入场、通票酒水权益禁兑和原票核销权补偿。游乐全园故障批量补偿前必须记录游乐公司负责人与工程或安全岗位共同确认；故障状态生效后，立即停止所有纯游乐门票和所有包含游乐权益通票的新销售。高频出入达到配置阈值时生成预警并关联出入录像核查，查实借票后关闭客人账户、作废账号并列入禁止再次进入的黑名单。
- 核销记录中文业务态仅为成功、拒绝、失败、已冲正、待复核、异常；逐级升级到最高一级仍超时的，转「异常」并按「票务异常队列」处置，不得自动通过、不得自动作废。结算中文业务态仅为草稿、待复核、已确认、已调整、已归档。
- 票/权益中文业务态仅为未使用、部分使用、已使用、已过期、已退款、已作废；退款、风控或争议期间的临时不可用使用独立阻断字段和业务单据，不新增冻结状态。
- 订单显示态必须区分已关闭和异常；已关闭只表示超过支付时限系统自动关闭。支付时限是全平台统一可配的业务参数，出厂默认 5 分钟，上下限出厂为 1 至 30 分钟。
- 系统不提供人工放行、不做离线核销；门票或权益开始核销后不得退款，通票安全例外除外。
- 人工申请、额度内自助和系统批量退款都进入同一授权与复核链，申请人不得审批自己的申请；退款选择粒度是 `ticket_instances`，内部对该票全部 `issued_entitlements` 做冲正和金额拆分。
- 酒吧门口或前台的售票型一体机只能售卖本酒吧单店门票，不得售卖综合体通票；门票为动态二维码，不提供取票功能，数据库或契约不得增加取票状态或纸票交付路径。
- 权益转赠按完整权益单位处理，并按确认领取竞态、逐人窗口和场次窗口约束落库；领取完成必须撤销原持有人全部现存凭证，新持有人重新取码。
- 通路费、销售渠道费、票务积分发行成本在域间拆分前按冻结 HTML 顺序逐项记录；门票综合服务费只适用于酒吧相关票务。

## 7. 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-08-30 | 改 | §2/全文版本 | 版本升至 V0.09；登记核销状态值域、预检/人工放行/离线入口和支付超时默认值未对齐 HTML 的现行实现缺口 | M03 Grok Build 最终全文交叉审计 |
| 2026-08-30 | 改 | §6/全文版本 | 版本升至 V0.08；承接日票、周票、月票、年票及其二维码按票种设置有效期 | M03 第二次全文全量双审；冻结 HTML 现文 |
| 2026-08-30 | 改 | §6/全文版本 | 版本升至 V0.07；补齐票务策略六态及最高级复核超时转异常出口 | M03 Grok Build 修复切片审计 ADV-S-001/003；James 已判 J-032/J-033 |
| 2026-08-30 | 改 | 全文 | 版本升至 V0.06；按当前 Flyway 重建已落地表清单，修正票实例、库存、退款、凭证、结算与积分边界，补演出票和 HTML 相关落库约束 | M03 全量再审与 Grok 交叉审计 |
| 2026-08-30 | 改 | §2/§3/§6 | 删除原型迁移 002 的现行权威表述；修正票实例与权益实例退款关系；补完整票种/权益枚举、六态、支付超时、安检不留按人记录、一体机售票边界 | M03 全量交叉审计修复；三份业务 HTML 现文及 James 裁决 |
| 2026-06-13 | 改 | §2 表清单注 | 将历史资格表补充口径改为“正式 Flyway 首批业务迁移必须纳入”，并声明不再作为开放待决策项 | 防止开发把门票资格 P0 执行侧权威误判为未定；本轮对抗式审计收口 |
| 2026-06-07 | 增 | §6 正式工程补充事项 | 新增 `ticket_eligibility_rules`、`student_credential_audits` 两表，并提示 `num_people` 字段补充 | 落地 D9 资格与多人票；finding ticket-1/3/4 |
| 2026-08-30 复核修复 | 改 | 顶部迁移链接 | 明确早期原型 002 已删除、非 Flyway 权威；票务数据库只引用正式 `backend/src/main/resources/db/migration/` 迁移 | M03 全量交叉审计修复 |
| 2026-06-07；2026-08-30 复核修复 | 改 | §6 票码凭证表 | 历史逻辑字段已由本版改为正式 Flyway/Java 现状；首次检票使用在线动态二维码，手环只记录当前场次入场状态 | 现行字段以正式迁移为准 |
| 2026-06-08 | 注 | §2 表清单注 | 历史上曾提出 `ticket_inventories`；当前仓库没有该物理表，库存由 `ticket_types.stock_quantity/sold_quantity` 承载 | 历史记录，现行事实见 §2 |
| 2026-06-08 | 注 | §2 表清单注 | 历史上曾提出 `refund_orders`；当前退款物理表是 `ticket_refunds` / `ticket_refund_lines` | 历史记录，现行事实见 §2 |
| 2026-06-12 AI-ready 收口 | 增 | §4A | 新增数据库落地说明四件套执行入口，约束 Flyway、通票拆权益、在线核销、库存并发、退款冲正和精确引用 | 防止 SQL 说明脱离正式 Java/Flyway/OpenAPI 验收链路 |
| 2026-06-13 OpenAPI 追溯闸口 | 改 | §4A | 精确引用补入 `p0-openapi-contract-closure-appendix-v0.1.md`，表、字段和接口必须能追溯 operation、权限、事件、终端和样例准入 | 防止票务 SQL 说明脱离正式接口追溯链路 |
