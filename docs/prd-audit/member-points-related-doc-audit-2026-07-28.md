# 会员、积分与商户积分结算相关 PRD 正式审计报告（2026-07-28）

执行日期：2026-08-01

基线：`origin/main` @ `a7a4e22a2b2101ca006247911eb5b1fff4448d54`

## 1. 审计目的与权威依据

本文对 `docs/prd-audit/document-scope-manifest.csv` 中全部 22 份 `MAIN,prd` 文件逐一执行顺从性审计与对抗性审计。

唯一完整领域权威：

- `docs/member-and-tier-product-prd-v1.0.md`
- `docs/points-and-voucher-product-prd-v1.0.md`
- `docs/merchant-points-settlement-rules-v1.0.md`

候选词命中只用于发现，不自动证明缺陷。历史文档和 `/home/james/codex-backups/` 只作为证据，冲突逐项裁决，不设统一覆盖优先级。

## 2. 状态与动作

正式审计状态只使用：`FULL`、`PARTIAL`、`CONFLICT`、`MISSING`、`AMBIGUOUS`、`STALE`、`NOT_APPLICABLE`。

处置动作只使用：`NO_CHANGE`、`SYNC_REQUIRED`、`SUPERSEDED`、`BLOCKED_BY_USER`。

## 3. 逐文件正式审计

| 文件 | 相关条款 | 当前表述摘要 | 顺从性结论 | 对抗性反例 | 权威依据 | 状态 | 动作 | 修改证据或不修改原因 |
|---|---|---|---|---|---|---|---|---|
| `docs/amusement-business-operations-product-prd-v0.1.md` | 全文候选扫描 | 只命中通用结算、退款等游乐经营词，没有定义会员、成长值、积分获取、兑换券或积分结算规则 | 不承担本域权威，无直接冲突 | 将“退款”误判为积分退款会扩大修改范围 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 关键词上下文属于游乐订单和经营结算；备份 `DIFFERENT`，差异不涉及本域裁决 |
| `docs/bar-business-operations-product-prd-v0.1.md` | 抬头规则 4、7；§7.2 | 内部结算价和免费权益表述属于通票赠送权益与酒吧商户兑付，不定义积分商城兑换 | 与本域相邻但属于通票结算权威，不能用积分规则改写 | 把所有“内部结算价”都强制套用积分兑换公式会破坏通票分账 | 通票规则继续由 `settlement-rebuild-prd-v1.0.md` 承接 | `NOT_APPLICABLE` | `NO_CHANGE` | 只保留领域边界；备份 `DIFFERENT`，main 差异为酒吧经营演进 |
| `docs/building-operations-prd-v1.0.md` | 全文候选扫描 | “商户、积分、结算”命中均为楼宇运营通用语义，没有会员积分规则 | 不承担本域权威 | 把组织积分或运营评分误当会员积分 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 与备份 `IDENTICAL`；无实质会员积分条款 |
| `docs/business-dashboard-metrics-product-prd-v0.1.md` | §3 指标域表会员行；§9 会员汇总卡 | 只列“新增会员、积分发放、积分兑换、等级分布”，未声明指标必须引用三份新权威和分账来源 | 指标范围合理，但口径来源缺失 | 看板把冻结积分算作已发放、把发券算作已核销补偿 | 三份新权威文档 | `PARTIAL` | `SYNC_REQUIRED` | 增加引用式指标边界，不复制公式；备份 `DIFFERENT` |
| `docs/customer-app-miniapp-product-prd-v0.1.md` | 抬头说明；§5 客人端功能 | 仍把积分和会员细节指向旧 `member-loyalty-and-stored-value` 混合 PRD | 入口已过期 | 客人端按旧文档展示“成长积分”或错误券转赠默认值 | 会员与等级 PRD；积分与虚拟券 PRD | `STALE` | `SYNC_REQUIRED` | 替换旧链接并明确只展示权威状态；备份 `IDENTICAL`，但 Phase 2 新权威使原入口过期 |
| `docs/dividend-engine-prd-v0.1.md` | 全文候选扫描 | 冻结、商户、结算、退款均属于分红业务 | 不承担本域权威 | 把分红冻结状态套成积分冻结 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 业务对象不同；备份 `DIFFERENT` |
| `docs/evidence-risk-control-product-prd-v0.1.md` | 全文候选扫描 | 冻结、等级、退款属于通用证据或风险等级 | 不定义会员积分业务规则 | 把风险等级误当会员等级 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 与备份 `IDENTICAL`；仅通用控制语义 |
| `docs/finance-settlement-product-prd-v0.1.md` | §3B.2 至 §3B.4 | 写“集中一个目录、改一次全平台生效、全平台只在一个目录存在” | 与“每项目独立内部结算价管理”直接冲突 | 项目 A 改价污染项目 B 同 SKU，或项目 B 跨项目引用 | 商户积分结算规则 §9 | `CONFLICT` | `SYNC_REQUIRED` | 把目录范围改为项目级，同时保留项目内唯一编辑、复核和历史固化；备份 `DIFFERENT` |
| `docs/group-governance-rebuild-prd-v1.0.md` | §4 权限示例 | 只提内部结算价敏感权限和组织等级，不定义积分价格或会员等级 | 不承担本域权威 | 将组织等级当会员等级 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 权限示例与新规则不冲突；备份 `DIFFERENT` |
| `docs/leasing-acquisition-prd-v1.0.md` | 全文候选扫描 | 商户和结算均为招商租赁语义 | 不承担本域权威 | 把租赁结算当积分结算 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 业务对象不同；备份 `DIFFERENT` |
| `docs/member-loyalty-and-stored-value-product-prd-v0.1.md` | 全文 | Phase 2 已收缩为储值卡、本金、赠送金、扣款、退款和注销；会员积分只引用新权威 | 职责与边界完整 | 储值充值误产积分或成长值 | 储值 PRD §11；积分与虚拟券 PRD §6.2 | `FULL` | `NO_CHANGE` | 已在 Phase 2 精准收口；备份 `DIFFERENT` 属预重构历史 |
| `docs/merchant-portal-zone-prd-v0.1.md` | 全文候选扫描 | 商户和结算为商户门户通用功能，没有积分账单或兑换配置定义 | 不承担本域权威 | 因“商户结算”字样擅自增加积分价格页面 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 未发现直接会员积分条款；备份 `DIFFERENT` |
| `docs/platform-governance-prd-v0.1.md` | 全文候选扫描 | 商户、组织等级和退款属于平台治理、合同、权限语义 | 不承担会员积分领域权威 | 将组织等级或审批等级当会员等级 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 无直接规则；备份 `DIFFERENT` |
| `docs/product-requirements-v0.1.md` | §6 核心对象；§8 能力层；§14 已确认规则 | 把“会员积分账户”同时描述为余额、等级和成长记录，并只笼统说后台定义 | 边界不完整，可能继续形成混合对象真源 | 下游从总纲推导一个混合账本或发券即扣实际库存 | 三份新权威文档 | `PARTIAL` | `SYNC_REQUIRED` | 把混合对象拆成引用式边界并明确发券预占、核销实耗；备份 `DIFFERENT` |
| `docs/real-estate-full-chain-domain-prd-v0.1.md` | 全文候选扫描 | 冻结、商户、结算属于资产、合同或地产经营语义 | 不承担本域权威 | 把资产冻结套成积分冻结 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 与备份 `IDENTICAL`；业务对象不同 |
| `docs/settlement-rebuild-prd-v1.0.md` | 抬头规则 9；配置表 `points_accrual_rate_configs`；§9.7；附录 A5/B.4 | 写“全平台内部结算价一个目录”“项目公司×商户积分率”“消费 1 元=1 积分、1000 积分=10 元”等旧口径 | 与项目级内部结算价、全国业态积分规则及新兑换定价直接冲突 | 商户自选积分率、跨项目改价、把财务测算默认值当生产规则 | 三份新权威文档 | `CONFLICT` | `SYNC_REQUIRED` | 只改积分边界、配置行和项目目录范围；通票分账其他规则保持不变；备份 `DIFFERENT` |
| `docs/stored-value-membership-checkin-birthday-prd-v0.1.md` | 全文 | Phase 2 已明确签到、生日为营销权益，不产生成长值、不刷新积分有效期，成本和预算系统化 | 与新权威一致 | 签到同时增加成长值，或生日倍率改写基础公式 | 会员与等级 PRD；积分与虚拟券 PRD；商户积分结算规则 | `FULL` | `NO_CHANGE` | 已在 Phase 2 精准同步；备份 `DIFFERENT` 属预重构历史 |
| `docs/supply-chain-inventory-product-prd-v0.1.md` | §10 第 3 条 | 已有预占→领取实扣两阶段，但写“发起兑换不减实际可售库存”，且仍标注票务 PRD 待同步 | 时点基本正确，用词会允许重复销售，跨文档状态已过期 | 发券后可销售数量不减造成超卖，或领取前就扣账面库存 | 积分与虚拟券 PRD §10 | `PARTIAL` | `SYNC_REQUIRED` | 改为“不减账面实物库存、必须减少可销售/可承诺量”，更新上游引用；备份 `DIFFERENT` |
| `docs/terminal-page-product-prd-v0.1.md` | 客人端能力表；§13、§15 | 列出积分余额、等级、兑换与“兑换成功必须扣减库存”，未指向新权威且未区分预占和实耗 | 展示范围合理，库存时点含糊 | 发券直接扣账面库存并生成商户补偿 | 会员与等级 PRD；积分与虚拟券 PRD | `PARTIAL` | `SYNC_REQUIRED` | 增加领域链接并改为发券预占、核销实耗；备份 `DIFFERENT` |
| `docs/third-party-business-access-product-prd-v0.1.md` | 全文候选扫描 | 商户、结算和退款均为第三方接入通用语义，没有会员积分事件契约 | 不承担本域权威 | 因通用退款字样擅自引入积分追偿协议 | 无需引用 | `NOT_APPLICABLE` | `NO_CHANGE` | 未发现直接规则；备份 `DIFFERENT` |
| `docs/ticketing-entitlement-checkin-product-prd-v0.1.md` | §3 核心对象；§7.5；§8.5；§12A.1 | 自行定义积分规则、三态入账和订单生命周期，并以实现反推；兑换库存和虚拟券边界不完整 | 与新积分冻结、原版本退款重算及单向权威冲突 | 支付成功即按旧实现计算、确认后退款无追偿、积分兑换礼物直接扣账面库存 | 会员与等级 PRD；积分与虚拟券 PRD | `CONFLICT` | `SYNC_REQUIRED` | 票务域只提交消费、履约、退款和核销事实；积分状态与兑换全部改为引用；备份 `DIFFERENT` |
| `docs/web-mobile-app-terminal-extension-product-prd-v0.1.md` | §4、§6、§8；去重指针 | 只定义多端展示和 P0/P1 分工，并明确详细规则以终端总表为准 | 不自行定义积分计算、价格或结算，委托关系清晰 | 把“兑换成功扣库存”解释成发券即扣账面库存 | 终端总表本轮同步后承接；三份新权威文档 | `FULL` | `NO_CHANGE` | 保持去重指针，避免两端文档重复规则；备份 `IDENTICAL` |

## 4. 审计汇总

| 结果 | 数量 |
|---|---:|
| `SYNC_REQUIRED` | 8 |
| `NO_CHANGE` | 14 |
| `SUPERSEDED` | 0 |
| `BLOCKED_BY_USER` | 0 |

状态分布：

| 状态 | 数量 |
|---|---:|
| `FULL` | 3 |
| `PARTIAL` | 4 |
| `CONFLICT` | 3 |
| `STALE` | 1 |
| `NOT_APPLICABLE` | 11 |

校验：3 + 4 + 3 + 1 + 11 = 22。

## 5. 备份一次性快速对照

对照源：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/work/source/docs/`。

| 对照结果 | 数量 |
|---|---:|
| `IDENTICAL` | 5 |
| `DIFFERENT` | 17 |
| `BACKUP_MISSING` | 0 |

字节一致：

- `docs/building-operations-prd-v1.0.md`
- `docs/customer-app-miniapp-product-prd-v0.1.md`
- `docs/evidence-risk-control-product-prd-v0.1.md`
- `docs/real-estate-full-chain-domain-prd-v0.1.md`
- `docs/web-mobile-app-terminal-extension-product-prd-v0.1.md`

其余 17 份不同。差异只说明备份与当前版本不同，不说明哪一方自动优先。本报告仍按每个具体条款、用户裁决和三份新权威文档重新判断。

备份中的旧报告曾写“积分/会员跳过细节，以 main 新文档为准”和“非积分差异默认以 main 为准”；本轮不沿用这种统一优先级，只把其差异统计和历史线索作为证据。

## 6. 精准同步授权范围

只允许修改以下 8 份 PRD：

1. `docs/business-dashboard-metrics-product-prd-v0.1.md`
2. `docs/customer-app-miniapp-product-prd-v0.1.md`
3. `docs/finance-settlement-product-prd-v0.1.md`
4. `docs/product-requirements-v0.1.md`
5. `docs/settlement-rebuild-prd-v1.0.md`
6. `docs/supply-chain-inventory-product-prd-v0.1.md`
7. `docs/terminal-page-product-prd-v0.1.md`
8. `docs/ticketing-entitlement-checkin-product-prd-v0.1.md`

每份文件只修改本报告列明的相关段落。出现额外文件或相邻业务规则变化时立即停止。

## 7. Phase 3 收口记录

- 阶段分支最终提交：`b093f26b2634970065c1b297589b3e8e92add1f3`。
- 合并到唯一最终工作区 `main` 的提交：`d5458563b8f17946f981eb331409552a4f4c6881`。
- 逐文件结果：审计时 22 份现行 PRD 全部有结论；8 份按本报告授权精准同步，14 份保持不变。
- 覆盖登记：三份新权威与三个业务 HTML 的 9 组关系已写入 `docs/prd-audit/html-prd-coverage.csv`，逐条 HTML 锚点与拟替代文本留给 Phase 4 只读审计。
- HTML 只读证明：三个文件的 SHA-256 仍分别为 `0704db9a80ac8a3235caf4cbf13024109257d3e04c5a0ffce55b83cf68d09e21`、`56edf0e902730ec2387e58dbb3e2fde4560bb043318cb2b250be5d36d3744ad8`、`0cb85d605b04f7d933d7d27eec6f3c7f98f3389e9dc11c6ee7e3686cd9da71d4`，与 Phase 1 基线一致。
