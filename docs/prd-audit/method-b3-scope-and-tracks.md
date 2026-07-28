# PRD 审计本轮方法固化（B+3）

日期：2026-07-27
目标机：`y9000p-ai` / `james@192.168.88.160`
仓库：`/home/james/complex-management`
基线：`main@b4290638`（= `origin/main`，干净）

## 1. 已确认决策

| # | 决策 | 选择 |
|---|---|---|
| 1 | 范围 | B：扩大纳入 openapi/ops/设计/合同/矩阵/审计报告/交付说明等 |
| 2 | 深度 | 3 分层混合 |
| 3 | 既有成果 | 保留 844 规则账本与 phase3 覆盖；并对已审主题做附录交叉复核 |
| 4 | 写入 | 先只读；本机 outputs 出稿；远端/PRD 改动分批批准 |
| 5 | 分轨 | MAIN / APPENDIX / EXCLUDED |
| 6 | 本轮完成线 | 方法固化 + 范围返工 + phase3 交叉复核；不做完 phase4–7 |

## 2. 权威层级

1. 现行三份业务 HTML（最终业务解释权）
2. 用户本轮新裁决
3. MAIN 文档（PRD/规格/术语/决策/业务追溯/入口索引）
4. APPENDIX 文档（OpenAPI/设计/ops/合同/实现矩阵/审计报告/交付说明等）

APPENDIX 与实现类文档**不得反向立法**。
排除目录内容仅在验证声明时按需读取。

## 3. 三轨定义

### MAIN
- 三份权威 HTML
- 现行总/领域 PRD、产品规格
- 术语、业务决策日志
- 业务需求追溯矩阵
- 当前有效文档入口、产品文档索引、能力层索引
- 业务 HTML 事实源验收规格

### APPENDIX
- `docs/openapi/**`
- `docs/ops/**` 与各类 runbook
- `*development-design*`、架构/库表设计、实现计划
- 合同模型、admin/实现/验收矩阵
- 审计报告、交付/handoff/编码完成说明
- 当前 `docs/prd-audit/**` 工作集

### EXCLUDED
- `docs/_archive/**`
- Playwright 报告、测试结果
- `production-readiness-evidence/**` 运行证据
- `*-latest.json/csv` 等机器快照
- 代码/数据库实现本体

## 4. 备份使用规则

路径：`/home/james/codex-backups/2026-07-23/complex-management-prd-audit-phase1/`

- 不是 Git 分支
- 37 份哈希未变可作线索
- 1 份变化：`business-enum-quick-reference-v0.1.html`（必须以现行版重建规则）
- 与现行冲突：以现行 HTML 为准

## 5. 本轮不做

- phase4–7 主轨全量规则覆盖
- 远端仓库写入 / PR
- 按代码反向改业务规则

## 6. 证据边界

交叉复核采用“主题词 + 摘录确认”启发式，用于发现**反向打脸/沉默缺口/已知漂移记录**；
**不是**对 APPENDIX 每个文件的逐句全量审计，也不是 phase3 每条规则的二次逐行证明。
