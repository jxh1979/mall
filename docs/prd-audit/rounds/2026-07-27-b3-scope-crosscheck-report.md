# 综合体 PRD 文档审计本轮报告（范围 B+3 / 完成线 1）

状态：**本轮完成线已达成（只读）**；全量 PRD 审计仍进行中（phase4–7 未做）。
生成：2026-07-27
输出目录：`D:\ai\codex\outputs\2026-07-27\prd-audit-scope-b3-crosscheck`

## 0. 执行边界

- 只读远端，未改 `/home/james/complex-management`
- 未改三份权威 HTML
- 未合并/提交任何 Git 变更

## 1. 锚定

```
HOST=y9000p-ai
USER=james
PWD=/home/james/complex-management
BRANCH=main
HEAD=b4290638dc591ccc4683a30e07fd0550eb8ece39
STATUS=## main...origin/main
```

### 权威 HTML SHA-256

```
0492b9112fec7ac03366f8c69a7a948f7706e9483b70370396359197f2ca087e  docs/business-system-map-for-operations-v0.1.html
a18cfff21c9cfbaf68d48ec6a0d68eeaa9afd3ae462dff76093c0a56dc31572b  docs/platform-governance-map-for-operations-v0.1.html
3e2071d8b740aea43df8a4685871c307a4185591ced4f1ced279ffa202e39648  docs/business-enum-quick-reference-v0.1.html
```

### 既有远端审计进度（保留）

- phase1–3：已完成并在 `main`
- phase4–7：未开始
- 规则账本：844
- phase3 覆盖：302（FULL 300 + NA 2）

## 2. 本轮做了什么

1. 固化 grilling 决策为 B+3 方法
2. 重建三轨范围清单
3. 快速确认备份 vs 现行（枚举 HTML 已变）
4. 对 phase3 治理主题做 APPENDIX 交叉复核
5. 给出 phase4–7 backlog 与远端写回变更清单（待批）

## 3. 范围返工结论

| 轨 | 数量 |
|---|---:|
| MAIN | 34 |
| APPENDIX | 265 |
| EXCLUDED（含 archive 扫描） | 694 |

- 旧 33 个 INCLUDED 全部保留在 MAIN
- 新增 MAIN：`docs/system-capability-layers-product-index-v0.1.md`
- 新纳入 APPENDIX：265（含 openapi/ops/设计/合同/矩阵/审计报告/交付等）

详见：`01-scope-rework-report.md`、`document-scope-manifest-b3.csv`

## 4. 备份结论（快过）

- 备份不是分支，是 2026-07-23 续接包
- 38 快照：37 不变，1 变（枚举 HTML）
- 历史积分清算 2 问仍未裁决
- 详情见 `existing/backup-current-diff.md`

## 5. phase3 交叉复核结论

| 判定 | 数量 | 含义 |
|---|---:|---|
| ALIGNED_OR_SUPPORTED | 10 | 附录未反向打脸，且有承接迹象 |
| KNOWN_DRIFT_RECORDED_IN_APPENDIX | 1 | 附录记录实现层先生效后审漂移（P1） |
| NEEDS_DOMAIN_PHASE_COVERAGE | 1 | 留痕/证据多为后续业务域主轨工作（P2） |

### P1（保留风险，不回滚 phase3）

- 规则 `PGM-0089`：高危先审后生效（phase3 FULL）
- 附录 `docs/dev-plan-v7-audit-20260722.md` 记录酒吧高价值退酒“先生效后审批”实现问题，并建议改为先审后行
- **不是** HTML/PRD 被设计文档推翻，而是实现仍可能漂移；属附录轨残余风险

### P2

- “留痕/证据”主题词命中远大于 phase3 覆盖数：应在 phase4–6 按域覆盖，不宜算 phase3 失败

详见：`02-phase3-crosscheck-findings.md`

## 6. 对抗性 + 顺从性（本轮适用方式）

- 顺从性：MAIN 仍以 HTML→PRD 全量规则覆盖为将来主线；本轮只重定范围并复核治理主题是否被 APPENDIX 支持/打脸
- 对抗性：对治理关键不变量做“若 APPENDIX/实现说明主张相反会怎样”的打脸检查
- 第一性原理：先定权威层与分轨，再谈文件多寡；禁止用 OpenAPI/设计给业务立法

## 7. 明确未完成（下轮）

- phase4 交易/会员/票务/酒吧主轨
- phase5 地产链
- phase6 游乐/终端/第三方/数据
- phase7 收口与最终报告
- APPENDIX 全量逐份深审（本轮不做）
- OpenAPI 全量 operation 逐条审计（本轮不做）

## 8. 本机产物索引

| 文件 | 用途 |
|---|---|
| `00-method-b3.md` | 方法固化 |
| `01-scope-rework-report.md` | 范围返工说明 |
| `document-scope-manifest-b3.csv` | 三轨清单 |
| `manifest-summary.json` | 计数摘要 |
| `02-phase3-crosscheck-findings.md` | 交叉复核结论 |
| `phase3-crosscheck-findings.csv/json` | 机器可读发现 |
| `conflict-excerpts.csv` | 冲突词摘录 |
| `03-phase4-to-7-backlog.md` | 后续清单 |
| `05-remote-writeback-change-list.md` | 写回远端待批清单 |
| `04-round-report.md` | 本报告 |
| `existing/**` | 远端既有审计副本 |
| `authority-hash.txt` / `identity.txt` | 锚定证据 |

## 9. 请你决定的下一步

1. 批准/调整三轨清单后，是否写回远端 `docs/prd-audit/`（见 `05-...`）
2. 是否立即开始 phase4 主轨（仍可先只读出发现）
3. P1 实现漂移是否单开代码修复任务（超出本轮文档审计完成线）
