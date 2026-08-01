# 会员、积分与商户积分结算重构审计范围基线（2026-07-28）

## 1. 文档目的

本文冻结会员、积分与商户积分结算重构的起始证据、审计对象和预期处置方式。本文只决定“哪些文件必须被审计、为什么纳入、预期由哪份权威文档承接”，不提前代替 Phase 3 的逐条正式审计结论。

## 2. 基线与最终版本边界

| 项目 | 冻结值 |
|---|---|
| 唯一最终工作区 | `/home/james/complex-management` |
| 唯一最终分支 | `main` |
| Phase 1 起始提交 | `169cdf258d5360d27182184635565fd2e9fcb569` |
| 起始时 `main` 与 `origin/main` | 一致 |
| 已批准设计 | `docs/superpowers/specs/2026-07-28-member-points-settlement-redesign-design.md` |
| 已批准设计 SHA-256 | `426e0fa7253042b266899a1e8d9863716d78ba24a20bc42017fd69ce7a417ee7` |
| 计划文件 | `docs/superpowers/plans/2026-07-28-member-points-settlement-redesign.md` |

三个业务 HTML 在用户批准逐条替代清单之前保持只读。Phase 1 冻结值如下：

| HTML 文件 | SHA-256 |
|---|---|
| `docs/business-system-map-for-operations-v0.1.html` | `0704db9a80ac8a3235caf4cbf13024109257d3e04c5a0ffce55b83cf68d09e21` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `56edf0e902730ec2387e58dbb3e2fde4560bb043318cb2b250be5d36d3744ad8` |
| `docs/business-enum-quick-reference-v0.1.html` | `0cb85d605b04f7d933d7d27eec6f3c7f98f3389e9dc11c6ee7e3686cd9da71d4` |

## 3. 权威结构与范围约束

本次建立以下三份领域权威文档：

1. `docs/member-and-tier-product-prd-v1.0.md`：会员身份、成长值、等级与等级倍率。
2. `docs/points-and-voucher-product-prd-v1.0.md`：积分产生、冻结、确认、退款、有效期、积分商城与虚拟券。
3. `docs/merchant-points-settlement-rules-v1.0.md`：价格参数、兑换下限、商户补偿、服务费、营销成本、账期、冲正与税务。

范围约束：

- 本轮只审计和修改 PRD 及其直接相关的文档入口、决策记录、范围清单与覆盖矩阵。
- 不修改代码、OpenAPI、数据库、测试、开发设计、运行时配置或部署资产。
- `/home/james/codex-backups/` 只提供差异线索，不直接覆盖 `main`。
- 历史文档、备份内容与现行内容发生冲突时逐项裁决，不设置统一优先级。
- 三个业务 HTML 只读到 Phase 4；Phase 4 只形成逐条替代清单，未获批准不得改写。
- 所有参数、规则、审批、版本与运行依据必须由系统管理；线下表格不得成为运行时真源。

## 4. 发现方法

以 `docs/prd-audit/document-scope-manifest.csv` 中 `track=MAIN` 且 `document_type=prd` 的 22 份文件为全集，以“会员、等级、成长值、积分、冻结、退款、兑换、虚拟券、结算、商户、营销费用、内部结算价、零售参考价”进行全文候选发现。

结果：22 份现行 PRD 均至少命中一个候选词。候选词命中只证明“需要人工判读”，不自动证明存在冲突；正式状态统一先记为 `NOT_AUDITED_PHASE1`，由 Phase 3 改为正式审计状态。

处置方式定义：

| 处置方式 | 含义 |
|---|---|
| `CREATE_AUTHORITY` | 新建领域权威文档。 |
| `REPLACE_WITH_REFERENCE` | 移除旧文件承担的现行规则权威职责，保留历史说明或本域剩余职责，并引用新权威。 |
| `PRECISE_SYNC` | 已有直接相关段落，正式审计确认后仅精准同步该段。 |
| `AUDIT_ONLY` | 必须逐条审计；若没有实质冲突则不修改。 |
| `OUT_OF_SCOPE_WITH_REASON` | 有候选词但业务语义与本次领域无关，正式报告须写明理由。 |

## 5. 计划新增的领域权威文档

| 路径 | 当前角色 | 命中主题 | 正式审计状态 | 目标权威文档 | 处置方式 | 证据 |
|---|---|---|---|---|---|---|
| `docs/member-and-tier-product-prd-v1.0.md` | 尚未创建 | 会员、成长值、等级 | `NOT_AUDITED_PHASE1` | 本文件 | `CREATE_AUTHORITY` | 已批准设计第 3、4、10、11、12 节 |
| `docs/points-and-voucher-product-prd-v1.0.md` | 尚未创建 | 积分、冻结、退款、兑换、虚拟券 | `NOT_AUDITED_PHASE1` | 本文件 | `CREATE_AUTHORITY` | 已批准设计第 3、5、8、10、11、12 节 |
| `docs/merchant-points-settlement-rules-v1.0.md` | 尚未创建 | 商户、结算、价格、费用、营销、税务 | `NOT_AUDITED_PHASE1` | 本文件 | `CREATE_AUTHORITY` | 已批准设计第 6 至 12 节 |

## 6. 22 份现行 PRD 的候选范围

下表逐行覆盖范围清单中的全部 `MAIN,prd` 文件。

| 路径 | 当前角色 | 命中主题 | 正式审计状态 | 目标权威文档 | 处置方式 | 证据 |
|---|---|---|---|---|---|---|
| `docs/amusement-business-operations-product-prd-v0.1.md` | 游乐业务运营 PRD | 结算、退款 | `NOT_AUDITED_PHASE1` | 积分与虚拟券；商户积分结算 | `PRECISE_SYNC` | 全文候选词命中；需判读消费积分、退款与结算接口 |
| `docs/bar-business-operations-product-prd-v0.1.md` | 酒吧业务运营 PRD | 会员、积分、兑换、内部结算价、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 全文候选词命中；存在直接会员积分与价格表述 |
| `docs/building-operations-prd-v1.0.md` | 楼宇运营 PRD | 商户、积分、结算 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；先区分楼宇通用结算与积分结算 |
| `docs/business-dashboard-metrics-product-prd-v0.1.md` | 经营看板指标 PRD | 会员、等级、积分、兑换、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 全文候选词命中；指标定义必须引用新的领域口径 |
| `docs/customer-app-miniapp-product-prd-v0.1.md` | 会员端与小程序 PRD | 会员、等级、积分、兑换、商户、退款 | `NOT_AUDITED_PHASE1` | 会员与等级；积分与虚拟券 | `PRECISE_SYNC` | 全文候选词命中；直接承接会员可见状态与兑换流程 |
| `docs/dividend-engine-prd-v0.1.md` | 分红引擎 PRD | 冻结、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需区分分红冻结与积分冻结 |
| `docs/evidence-risk-control-product-prd-v0.1.md` | 证据与风控 PRD | 冻结、等级、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档的审计证据要求 | `AUDIT_ONLY` | 全文候选词命中；可能只属于通用证据语义 |
| `docs/finance-settlement-product-prd-v0.1.md` | 财务经营与结算 PRD | 兑换、内部结算价、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `PRECISE_SYNC` | 存在内部结算 SKU、结算期、退款税会归属等直接章节 |
| `docs/group-governance-rebuild-prd-v1.0.md` | 集团治理 PRD | 内部结算价、商户、等级、退款 | `NOT_AUDITED_PHASE1` | 会员与等级；商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需区分组织等级与会员等级 |
| `docs/leasing-acquisition-prd-v1.0.md` | 招商租赁 PRD | 商户、结算 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；可能仅为合同或租赁结算 |
| `docs/member-loyalty-and-stored-value-product-prd-v0.1.md` | 会员忠诚度与储值混合 PRD | 会员、等级、成长值、积分、冻结、退款、兑换、虚拟券、结算、内部结算价 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `REPLACE_WITH_REFERENCE` | §3 至 §6 及 §10 直接承载现行会员积分规则；后续只保留储值职责 |
| `docs/merchant-portal-zone-prd-v0.1.md` | 商户门户 PRD | 商户、结算 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需确认是否存在积分账单或仅通用商户结算 |
| `docs/platform-governance-prd-v0.1.md` | 平台治理 PRD | 商户、等级、结算、退款 | `NOT_AUDITED_PHASE1` | 会员与等级；商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需区分组织等级与会员等级 |
| `docs/product-requirements-v0.1.md` | 产品总纲 PRD | 会员、等级、积分、兑换、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 产品总纲存在跨域规则入口，必须避免成为第二规则真源 |
| `docs/real-estate-full-chain-domain-prd-v0.1.md` | 不动产全链路 PRD | 冻结、商户、结算 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需区分资产冻结与积分冻结 |
| `docs/settlement-rebuild-prd-v1.0.md` | 通票权益结算 PRD | 会员、积分、兑换、冻结、内部结算价、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 积分与虚拟券；商户积分结算 | `PRECISE_SYNC` | §5.2、§5.3、§6 等直接涉及核销结算与赠送权益 |
| `docs/stored-value-membership-checkin-birthday-prd-v0.1.md` | 储值、付费会员、签到、生日 PRD | 会员、等级、积分、冻结、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 会员与等级；积分与虚拟券 | `PRECISE_SYNC` | 签到和生日奖励必须明确不产生成长值、不刷新积分有效期 |
| `docs/supply-chain-inventory-product-prd-v0.1.md` | 供应链库存 PRD | 会员、积分、兑换、冻结、商户、结算 | `NOT_AUDITED_PHASE1` | 积分与虚拟券；商户积分结算 | `PRECISE_SYNC` | 兑换券发放预占库存、核销结算与库存状态直接相关 |
| `docs/terminal-page-product-prd-v0.1.md` | 后台终端页面 PRD | 会员、等级、积分、兑换、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 页面入口与系统配置必须承接新权威规则 |
| `docs/third-party-business-access-product-prd-v0.1.md` | 第三方业务接入 PRD | 商户、结算、退款 | `NOT_AUDITED_PHASE1` | 商户积分结算 | `AUDIT_ONLY` | 全文候选词命中；需确认是否暴露会员积分事件接口 |
| `docs/ticketing-entitlement-checkin-product-prd-v0.1.md` | 票务权益核销 PRD | 会员、等级、积分、兑换、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 积分与虚拟券；商户积分结算 | `PRECISE_SYNC` | 虚拟券核销、退款和商户补偿时点与本域直接相交 |
| `docs/web-mobile-app-terminal-extension-product-prd-v0.1.md` | 多终端扩展 PRD | 会员、等级、积分、兑换、商户、结算、退款 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 多端展示与操作必须引用同一会员积分状态和权限口径 |

## 7. 强制纳入的旧规则、入口与治理文档

以下文件不属于上述 22 份 `MAIN,prd` 全集，或承担的类型不是 PRD，但批准计划要求纳入。第 6 节已列出的四份直接 PRD不重复列出。

| 路径 | 当前角色 | 命中主题 | 正式审计状态 | 目标权威文档 | 处置方式 | 证据 |
|---|---|---|---|---|---|---|
| `docs/member-points-tier-and-clearing-prd-v1.0.md` | 旧会员积分、等级与清算混合 PRD；未列为 `MAIN,prd` | 会员、等级、成长值、积分、冻结、退款、结算 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `REPLACE_WITH_REFERENCE` | 文件仍直接定义账户、发分、有效期、等级和清算 |
| `docs/member-points-p1-ops-parameter-tables-v0.1.md` | 旧运营参数表；未列为 `MAIN,prd` | 业态发分、成长值、冻结、有效期、等级、双价清算、兑换 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 表 A 至 K 直接罗列运行参数；后续改为系统配置覆盖检查表 |
| `CONTEXT.md` | 项目领域上下文 | 术语、主体、职责 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 需要提供统一术语，但不得复制公式 |
| `docs/business-decision-log-v0.1.md` | `MAIN,decision` 决策日志 | 全部已批准裁决 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 需要登记用户已批准的新裁决与链接 |
| `docs/prd-audit/business-decision-ledger.md` | `APPENDIX,audit_report` 决策台账 | 全部已批准裁决 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 需要保持审计追溯 |
| `docs/00-current-effective-document-entry-v0.1.md` | `MAIN,index` 当前有效入口 | 当前权威入口 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 现有会员积分入口仍指向旧混合 PRD |
| `docs/product-documentation-index-v0.1.md` | `MAIN,index` 产品文档索引 | 文档导航 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 新权威文档必须成为现行入口 |
| `docs/system-capability-layers-product-index-v0.1.md` | `MAIN,index` 能力分层索引 | 能力归属 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 需要登记会员、积分、结算的单向权威关系 |
| `docs/documentation-map-v0.1.md` | `APPENDIX,other` 文档地图 | 文档导航 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | 需要同步文档职责和替代关系 |
| `docs/prd-audit/document-scope-manifest.csv` | `APPENDIX,audit_report` 范围清单 | 文档角色与状态 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | Phase 3 按实际角色更新 |
| `docs/prd-audit/html-prd-coverage.csv` | `APPENDIX,audit_report` HTML/PRD 覆盖矩阵 | HTML 与 PRD 覆盖关系 | `NOT_AUDITED_PHASE1` | 三份新权威文档 | `PRECISE_SYNC` | Phase 3 登记覆盖关系，Phase 4 只读 HTML 后形成替代清单 |

明确纳入且已在第 6 节列出的直接 PRD：

- `docs/member-loyalty-and-stored-value-product-prd-v0.1.md`
- `docs/stored-value-membership-checkin-birthday-prd-v0.1.md`
- `docs/finance-settlement-product-prd-v0.1.md`
- `docs/settlement-rebuild-prd-v1.0.md`

## 8. Phase 3 正式审计要求

Phase 3 必须把 22 份现行 PRD 逐一写入正式报告，即使结论为不适用。正式状态只允许：

- `FULL`
- `PARTIAL`
- `CONFLICT`
- `MISSING`
- `AMBIGUOUS`
- `STALE`
- `NOT_APPLICABLE`

正式处置动作只允许：

- `NO_CHANGE`
- `SYNC_REQUIRED`
- `SUPERSEDED`
- `BLOCKED_BY_USER`

Phase 1 的 `PRECISE_SYNC` 和 `AUDIT_ONLY` 只是审计排程，不预先强迫 Phase 3 修改文件。Phase 3 必须依据具体原句、反例和新权威条款重新确认动作。

## 9. 既有未跟踪资料保护

主工作区起始时存在以下四项未跟踪审计资料，均属于既有状态：

- `docs/prd-audit/OTHER_15_FILES_FULL_SENTENCE_DIFF-2026-07-28.md`
- `docs/prd-audit/backup-diff-by-file-2026-07-28/`
- `docs/prd-audit/backup-vs-main-FULL-SENTENCE-DIFF-2026-07-28.md`
- `docs/prd-audit/bar-prd-backup-vs-main-FULL-DIFF.md`

本计划不删除、不移动、不修改、不提交上述资料。

## 10. Phase 1 完成判据

- 已冻结起始提交、设计文件和三个业务 HTML 的 SHA-256。
- 已逐行覆盖全部 22 份 `MAIN,prd` 文件。
- 已纳入两份未登记为现行 PRD、但直接承载旧会员积分规则的文件。
- 已纳入决策、入口、索引、范围与覆盖治理文档。
- 未把候选词命中自动判成缺陷。
- 未纳入代码、OpenAPI、数据库、测试、开发设计或运行时文件。
- 未修改三个业务 HTML。
- Phase 1 成果合并推送后，`main` 与 `origin/main` 必须一致。
