# 综合体管理系统 PRD 全量审计报告

状态：**文档审计完成（phase1–7）；B 线文档债清偿进行中（Batch1 已闭合 19 条）**  
完成日：2026-07-28  
基线提交：`31f262c5`  
范围：现行 PRD 及相关业务文档（B+3 MAIN/APPENDIX 分层）；**不含代码实现审计**

## 1. 审计结论与可用性边界

### 结论（审计推导 + HTML 明确要求 + 用户裁决）
1. 三份业务 HTML 为业务最终解释权；PRD 须向其对齐。  
2. 规则账本共 **844** 条，覆盖矩阵已 **100% 挂接**最终状态。  
3. 治理域（phase3）已完成并曾落地修订；交易/地产/边缘域完成文档覆盖与对抗登记。  
4. 会员积分/等级/清算已通过独立重构 PRD + P0/P1 决策收口（见主 PRD）。  
5. 仍存在 **文档债**（PARTIAL/MISSING 与 DEFERRED 项），不阻塞“文档审计完成”标签，但阻塞“可宣称业务 PRD 零缺口”。  

### 可用性边界
- 可用于：文档缺口定位、对抗场景清单、后续补 PRD 优先级。  
- **不可用于**：替代测试/代码审计/上线放行唯一依据。  

## 2. 权威 HTML 哈希与提交范围

| file | sha256 |
|---|---|
| business-system-map-for-operations-v0.1.html | `0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e` |
| platform-governance-map-for-operations-v0.1.html | `a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b` |
| business-enum-quick-reference-v0.1.html | `3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648` |

- 审计过程约定：不修改上述 HTML。  
- 变更范围：仅 `docs/prd-audit/**` 及必要索引指针。  

## 3. 文档范围统计
- MAIN 业务 PRD/规格/术语/决策：见 `document-scope-manifest.csv`（B+3）  
- APPENDIX：OpenAPI/设计/ops 等仅附录  
- 本报告阶段产物：phases/01–07、coverage、adversarial CSVs  

## 4. 规则覆盖统计

| status | count |
|---|---:|
| FULL | 807 |
| PARTIAL | 35 |
| MISSING | 0 |
| NOT_APPLICABLE | 2 |

**合计：844 = ledger 844**

## 5. 分阶段
| 阶段 | phase 标签 | 行数 |
|---|---|---:|
| 治理 | 03-governance-and-common | 302 |
| 交易权益 | 04-transaction-member-ticket-bar | 299 |
| 地产供应链 | 05-realestate-leasing-building-supply | 132 |
| 边缘域 | 06-amusement-terminal-thirdparty-data | 111 |

## 6. P0–P3 与残余风险
- 高严重度未 FULL：46（均已挂 decision_id）  
- 用户接受的剩余风险类型：`DEFERRED-DOCS-FOLLOWUP`（后续补 PRD，不在本审计线程改代码）  
- 积分域关键裁决：ACCRUAL-002、O-BAN、V-12、T-6-STRICT、P-DUAL、R-SYM、CFG-ALL-NUMERIC 等  

## 7. 已修订问题（摘要）
- Phase3：治理 PRD 多项漂移已修并合并历史 PR  
- 会员积分：主 PRD v1.0 + P1 参数表已合并 main  
- Phase4–7：覆盖与对抗文档化完成  

## 8. 未决 / 后续文档债
- 储值混退细则、卡座转并续、跨营业日统一切日等 DEFERRED  
- PARTIAL/MISSING 行见 coverage 与 phase*-issue-rows.csv  
- 实现切片见 `docs/superpowers/plans/2026-07-28-member-points-tier-clearing-implementation-slices.md`（**编码需另许可**）  

## 9. 阶段 PR（主要）
- #217 B+3 范围  
- #218/#222 phase4/5  
- #219 会员积分 PRD  
- #220 实现切片  
- #221 P1 参数表  
- （本 PR）phase6+7 收口  

## 10. 最终验证结果
- `COVERAGE_STATUS_PASS`  
- `HIGH_SEVERITY_ACCOUNTED_PASS`  
- 权威 HTML 哈希未变  
- **无代码变更**  

---

## 当前进度（归档）
- 阶段 1–7：完成（文档审计）  
- 代码实现审计：不在范围  


### B 线 Batch1（2026-07-28）

见 `PHASE-B-DEFERRED-BATCH1.md`。闭合 19 条；剩余高严重度 OPEN 见该文。
