# 阶段 A：APPENDIX 深审（冲突 / 过时 / 沉默）
状态：**完成（文档审计）**  
日期：2026-07-28  
基线：`main`（B 线高严重度清偿后）  
约束：仅文档；APPENDIX **不得反向立法**；不改代码与权威 HTML  

## 1. 方法
- 范围：`document-scope-manifest.csv` 中 track=APPENDIX 的 **265** 份清单项
- 深度：主题探针（10 个现行 MAIN 裁决）全文检索 + 历史桶分流
- **不是** 265 份逐句文学审，也不是 OpenAPI 全量契约证明
- 历史审计/交付/快照命中过时词 → `HISTORICAL_*`（保留证据，不要求改历史正文）
- 现行设计/OpenAPI/ops/合同模型等命中过时词 → 进入 **行动队列**

## 2. 扫描统计
| 项 | 值 |
|---|---:|
| APPENDIX 清单 | 265 |
| 文本已扫描 | 265 |
| 路径缺失 | 0 |
| 探针命中记录 | 78 |
| 行动队列（需处理/确认） | 9 |

### 判定分布
| verdict | count |
|---|---:|
| SUPPORTS_MAIN | 58 |
| POTENTIAL_STALE_OR_REVERSE | 7 |
| HISTORICAL_STALE_OK | 6 |
| HISTORICAL_MIXED | 5 |
| MIXED_STALE_AND_SUPPORT | 2 |

### 行动队列严重度
| severity | count |
|---|---:|
| P1 | 8 |
| P2 | 1 |

### 行动队列按探针
| probe | count |
|---|---:|
| APX-SEAT-TM | 7 |
| APX-SV-LEDGER | 1 |
| APX-DATA-FRESH | 1 |

## 3. 重点现行附录（行动队列 Top 路径）
| path | 命中次数 |
|---|---:|
| `docs/development-ready-spec-draft-v0.1.md` | 1 |
| `docs/full-admin-mvp-coding-task-breakdown-v0.1.csv` | 1 |
| `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv` | 1 |
| `docs/full-admin-page-permission-field-matrix-v0.1.csv` | 1 |
| `docs/full-admin-srs-v0.1.md` | 1 |
| `docs/full-admin-ui-acceptance-matrix-v0.1.md` | 1 |
| `docs/member-loyalty-and-stored-value-development-design-v0.1.md` | 1 |
| `docs/p1-bar-table-session-api-contract-v0.1.md` | 1 |
| `docs/data-analysis-layer-spec-v0.2.md` | 1 |

## 4. 高优先级发现摘要（抽样）
（无 P0 行动项）

## 5. 与 phase3 附录交叉复核关系
- 保留 `appendix-crosscheck-findings.csv`（治理主题启发式）
- 本阶段新增全 APPENDIX 主题探针与行动队列，覆盖积分/酒吧/储值/营业日税法等 B 线新裁决
- 已知：实现层「先生效后审」类漂移仍可能存在于历史审计报告；**以 MAIN BAR-VOID-A1 为准**，编码另开

## 6. 明确未做
1. 未批量改写 265 份附录正文（避免未经逐条批准的大爆炸）
2. 未改 OpenAPI 契约生成/代码
3. 未把历史审计报告「现代化」重写
4. 未宣称 APPENDIX 与实现对齐

## 7. 建议下一步（需你批准）
1. **A1**：对行动队列 P0 现行设计/runbook 加「废止/以 MAIN 为准」横幅或最小补丁  
2. **A2**：OpenAPI/开发设计中与 ACCRUAL-002、BAR-VOID-A1、T-INVOICE-LINK 冲突处定点修订  
3. **A3**：仅维持本清单，附录正文暂不动  

## 8. 产物
- `appendix-deep-findings.csv`
- `appendix-deep-action-queue.csv`
- `appendix-deep-summary.json`
- 本报告 `phases/08-appendix-deep-audit.md`

## 9. 复核后行动队列（去误报）

扫描后人工语境复核：
- data-analysis-layer 对「过期隐藏」的命中为 §F 非目标表述 → FALSE_POSITIVE。
- 非历史附录 P0 主题（ACCRUAL-001 / 先生效后审 / 积分抵现 / 中心代收）未命中现行设计正文。
- 真实附录债：仍把「续卡」写进现行能力的 Admin/SRS/矩阵/P1 卡座契约，与 MAIN DEC-SEAT-NO-RENEW-001 冲突。
- 储值开发设计为 2026-06 代码回填稿，早于 DEC-SV-*，需横幅或改版对齐 MAIN 储值 §7。

### 行动队列（7）

| sev | probe | path |
|---|---|---|
| P1 | APX-SEAT-TM | `docs/development-ready-spec-draft-v0.1.md` |
| P1 | APX-SEAT-TM | `docs/full-admin-mvp-coding-task-breakdown-v0.1.csv` |
| P1 | APX-SEAT-TM | `docs/full-admin-mvp-requirements-code-matrix-v0.1.csv` |
| P1 | APX-SEAT-TM | `docs/full-admin-page-permission-field-matrix-v0.1.csv` |
| P1 | APX-SEAT-TM | `docs/full-admin-srs-v0.1.md` |
| P1 | APX-SEAT-TM | `docs/full-admin-ui-acceptance-matrix-v0.1.md` |
| P1 | APX-SEAT-TM | `docs/p1-bar-table-session-api-contract-v0.1.md` |

### 推荐处理（待批）

| 选项 | 内容 |
|---|---|
| **A1（推荐）** | 对上表路径加「以 MAIN 为准 / 续卡废止 / 储值见 PRD」废止横幅，最小补丁 |
| **A2** | 同步改 P1 卡座契约与 Admin 矩阵，删除续卡接口与验收项 |
| **A3** | 仅保留本深审清单，正文暂不动 |

**本 PR 只落地审计产物，不改附录业务正文。**

