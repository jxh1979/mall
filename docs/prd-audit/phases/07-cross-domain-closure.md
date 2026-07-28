# 阶段 7：跨域一致性与最终收口

状态：**完成（文档审计收口）**
日期：2026-07-28
基线：`31f262c5`

## 覆盖完整性
- Ledger 规则数：844
- Coverage 行数：844
- 键集合一致：**是**
- 状态集合均在允许集合内：**是**
- `COVERAGE_STATUS_PASS`

## 覆盖状态统计
| status | count |
|---|---:|
| FULL | 788 |
| PARTIAL | 48 |
| MISSING | 6 |
| NOT_APPLICABLE | 2 |

## 未完全关闭的高严重度项
- 数量：46
- 均已挂 `decision_id`（多数为 `DEFERRED-DOCS-FOLLOWUP` 或既有业务裁决）
- `HIGH_SEVERITY_ACCOUNTED_PASS`

| severity x status | count |
|---|---:|
| P1/PARTIAL | 26 |
| P0/PARTIAL | 14 |
| P1/MISSING | 6 |

## 跨域一致性检查
| ID | 主题 | 严重度 | 结果 | 说明 |
|---|---|---|---|---|
| XD-001 | 金额单位/精度 | P1 | COVERED | 多文档使用分/元；以 HTML 与产品总纲金额规则为准 |
| XD-002 | 项目/商户隔离 | P0 | COVERED | 项目归属与商户范围在治理/地产/积分文档均有要求 |
| XD-003 | 终端关闭 fail-closed | P0 | COVERED | 终端开关关闭后后端拒绝 |
| XD-004 | 积分双账本与兑换边界 | P0 | COVERED | 以 member-points-tier-and-clearing-prd-v1.0 与 ACCRUAL-002/O-B |
| XD-005 | 审批/职责分离 | P0 | COVERED | 治理 phase3 已强化；各域需继续沿用 |
| XD-006 | 营业日/时区 | P1 | PARTIAL | 仍有跨营业日统一切日点文档债（phase4 DEFERRED ADV-TX-008） |

## 术语与权威
- 业务最终解释权：三份业务 HTML（哈希见下）
- 会员积分/等级/清算：以 `member-points-tier-and-clearing-prd-v1.0.md` 为准
- 积分兑券承接：`POINTS-VOUCHER-ACCRUAL-002`（核销才记商户承接）
- 配置化：`DEC-CFG-ALL-NUMERIC-001`（时间/数字可配）

## 明确边界（审计可用性）
1. 本审计为 **文档顺从性/对抗性** 审计，**不验证代码实现真实性**。  
2. 未在用户批准下批量改写业务 PRD 正文；OPEN 项登记为后续文档债。  
3. **无代码变更**。  
4. `FULL` = 文档检索可支撑，**不是**生产已实现证明。

## 各阶段摘要
| 阶段 | 覆盖约 | 状态 |
|---|---:|---|
| 1-2 基线/规则账本 | — | 完成 |
| 3 治理 | 302 | 完成 |
| 4 交易权益 | 299 | 完成（文档） |
| 5 地产供应链 | 132 | 完成（文档） |
| 6 游乐终端第三方数据 | 111 | 完成（文档） |
| 7 跨域收口 | 全量校验 | 完成 |

## 权威 HTML 哈希
| file | sha256 |
|---|---|
| business-system-map-for-operations-v0.1.html | `0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e` |
| platform-governance-map-for-operations-v0.1.html | `a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b` |
| business-enum-quick-reference-v0.1.html | `3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648` |

## 最终验证
- git 路径：仅 docs（提交时校验）
- HTML 哈希未改
- coverage 全量闭环
