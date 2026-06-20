## 概述

本 PR 汇总自 `main` 以来同一轮迭代的全部工作，共 **15 个提交**，按四个阶段递进：业务 HTML 缺口审计编码闭环 → 稳定化 goal → 文档体系 goal → 代码实现 goal（含最终收口验收的文档一致性修正）。所有编码改动均以 **TDD（先红→实现→再绿）** 推进，每步合入前完成中央验证（`mvn -o test`、`semgrep p/java`、`git diff --check`、`verify:tenant-isolation`、`verify:delivery-gates` 全链路）。

## 阶段一 · 业务 HTML 缺口审计编码闭环

| 提交 | 内容 |
|---|---|
| `0663343` feat | 业务 HTML 缺口审计闭环 + P0/P1/P2 全量编码落地（对照业务人员 HTML + 市场标杆 + 需求/开发文档做差异审计后，逐项补齐/闭环编码） |

## 阶段二 · 稳定化 goal（可稳定运行 + 具备验收条件）

| 提交 | 内容 |
|---|---|
| `32b72d9` chore(stabilization) | goal 模式稳定化与交付验收闸口收口 |
| `0c8a356` fix(gate) | 修正 t3d 闸口 PII 脱敏标记校验路径（随 T8 重构上移到 PiiTextRedactor） |

## 阶段三 · 文档体系 goal（支撑产品/开发/测试/运营/第三方供应商）

| 提交 | 内容 |
|---|---|
| `d92588c` docs | 补根 README + 文档地图 + V099–V124 新功能开发验收规格，修正反向 PRD 与短信开关 |
| `72b473b` docs(P1) | 刷新数据表/事件字典/需求矩阵/域 PRD，补第三方-运营-合规文档至 V124 |
| `43b4027` docs(P2/P3) | web-mobile PRD 去重 + 术语基线 + 告警步骤化 + dev-design 版本头 + 决策版本回填 |
| `ee28876` docs | 补 README 英文版与架构图集（Mermaid：系统上下文/域模块/分层/时序/前端） |

## 阶段四 · 代码实现 goal（需求→代码一致性审计 + 逐项 TDD 闭合）

### 缺陷/一致性/架构修复

| 提交 | 内容 |
|---|---|
| `96dba6f` fix(consistency+security) | 财务并发锁（预存 FOR UPDATE、盘亏差异条件 UPDATE、退款 TOCTOU 锁）；酒吧 void 行锁 + 权限分离(V125) + 修复预存 facade 未委派导致审批决策查不到流水的接线 bug；wecom → 统一 risk_alerts 双写；积分 earnPoints 审计补齐 |
| `a11ebee` fix(consistency) | 终端设备状态机条件 UPDATE（防并发丢更新/非法跃迁）；MDM 违规处置 `AND handled=FALSE` 防重复；工单 SLA 条件升级 + `FOR UPDATE SKIP LOCKED`；tenant-isolation 校验器行锁子句关键字解析健壮性 |
| `c84c043` fix(architecture) | 补齐「业务异常统一进 risk_alerts 责任链」不变量：inventory/analytics/amusement 三域风险写入双写统一表（复刻 wecom fail-safe 先例，含 riskLevel 映射防 medium 被静默吞数据） |

### A 类功能补全（扩 P0 契约面）

| 提交 | 内容 |
|---|---|
| `ec7ea8d` feat(inventory) | **V108 盘点录入**端到端（建单/录差异/复核/查询，5 端点 + V126 权限）。解锁 finance 入账依赖的 `review_status='approved'`，闭合此前断裂的入账链路 |
| `aafde6e` feat(amusement) | **V104 超容策略配置**（upsert/读取，2 端点 + V127 权限）。让既有软上限/warn_only 读路径真正可被触发 |
| `d492937` feat(inventory) | **REQ-39 溯源码主数据**（贴码/打印/责任链查询，4 端点 + 新建表 V128 + 权限）。DRIFT 闭合：矩阵声称有 `inventory_trace_codes` 表但从未建 |
| `ac64ad7` feat(amusement) | **REQ-53 权益会话**（扫码发起/一体机展示/防错领核销，4 端点 + 新建表 V129 + 权限）。DRIFT 闭合：会话表 + 会话时效 + 防错领条件 UPDATE |

### 最终收口验收（文档一致性）

| 提交 | 内容 |
|---|---|
| `docs(consistency)` | 追溯矩阵收口 REQ-39/REQ-53：核验横幅扩至 V129，第 134/168 行覆盖状态由"部分覆盖(未落地)"上修为"已覆盖"，与已落库 V128/V129 及 claims.json(present) 对齐（收口验收发现的唯一文档-代码不一致，已修复）。最终验收：后端 1341 测试、前端 vitest 36、Playwright E2E 42、semgrep java+ts 0 ERROR、约 50 道 delivery-gates 全 PASS、tenant-isolation strict-0、0 P0/P1 代码缺陷 |

## 基线变化（实测）

| 指标 | 变化 |
|---|---|
| 后端测试 | → **1341 全绿**（代码实现 goal 阶段 +82 个 TDD 测试，0 失败 0 错误） |
| OpenAPI operation | 386 → **401**（+15，17 个新端点） |
| 库表 | 213 → **215**（+2 新表，均带直接 merchant_id → 自动归类 direct scope） |
| matrix-vs-migration | 143 present-claims confirmed、7 absent（REQ-39/REQ-53 两个 DRIFT 翻 present） |
| semgrep p/java | 0 ERROR |
| verify:delivery-gates | 约 50 子闸口全链路 PASS |

## 契约级联处置

新增端点/表触发的契约级联已全部消化并验证：`add-<feature>-openapi.mjs` 改 JSON 源并重生成 yaml → 重生成 operation 矩阵 / 契约附录 / security-baseline / domain-ownership-scope → 手改 production-readiness 快照 operation 计数 → `matrix-migration-claims.json` 翻 present。

## 不在本 PR 范围（非缺陷）

- 外部依赖项（人脸识别 Port、REQ-06 HMAC 签名）：生产前置，当前开关 off + mock 为正确状态，未到位前不伪造对账数据。
- P3 事件名漂移 `order.sales_attributed`（统计口径微调），可后续单独处理。

## 验证方式

```bash
npm run test:backend            # 后端 1341 测试
npm run verify:delivery-gates   # 交付总闸口全链路
git diff --check                # 空白检查
```

🤖 Generated with [Claude Code](https://claude.com/claude-code)
