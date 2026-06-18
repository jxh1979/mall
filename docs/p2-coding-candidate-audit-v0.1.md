# P2 编码候选审计 V0.1

## 1. 结论

P0/P1 当前可编码范围已经收口。下一阶段不应直接把所有 P2 一起开工，应先选择外部依赖少、不会破坏 P0/P1 主闭环、能继续复用现有 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 架构的切片。

推荐下一步优先级：

1. P2-A 商场平面图 CAD/SVG 标准化导入。
2. P2-B 游乐团体/包场预订与容量锁定。
3. P2-C 票务动态定价规则容器。
4. P2-D 经营分析/预测看板的指标沉淀。
5. P2-E 供应商深度联动、机器人、视频、设备、人脸正式接入。
6. P2-F 独立 Python AI 服务。

## 2. 可直接进入编码计划的候选项

| 编号 | 候选项 | 推荐度 | 原因 | 主要风险 |
| --- | --- | --- | --- | --- |
| P2-A | 商场平面图 CAD/SVG 标准化导入 | 高 | 已有 P1 图片 + 多边形坐标数据模型，新增导入任务、解析结果、错误报告即可扩展 | CAD 格式复杂；建议 MVP 先支持 SVG 和标准化 JSON，CAD 转换作为导入前置工具 |
| P2-B | 游乐团体/包场预订与容量锁定 | 中高 | 已有游乐项目、场次、预约/排队轻闭环，可增加团体订单和容量占用 | 价格、退款、改期和销售归属规则复杂；需先限制为后台创建、人工审批 |
| P2-C | 票务动态定价规则容器 | 中 | 已有平日/周末/节假日基础价和会员/学生/新人/多人/渠道价变体 | 动态定价若直接影响已售票，风险高；MVP 只能做模拟和人工发布 |
| P2-D | 经营分析/预测看板指标沉淀 | 中 | P0/P1 已有交易、核销、库存、应收应付、商户账单、游乐事故等数据 | 预测模型不能先上；先做指标快照和趋势表 |

## 3. 暂不建议直接编码的候选项

| 候选项 | 暂缓原因 | 进入条件 |
| --- | --- | --- |
| 机器人平台深度联动 | 缺少真实厂商统一 API、签名、回调、任务状态和失败码 | 厂商 API/SDK、沙箱、回调样例到位 |
| 视频平台内播放和自动证据绑定 | 依赖播放鉴权、片段截取、保存期限和调阅审计协议 | 视频平台 API、播放 token、录像片段索引、权限样例到位 |
| 设备厂商/NFC/闸机深度联动 | 依赖硬件型号、SDK、签名、离线策略和现场网络条件 | 设备型号、SDK、接口文档、测试设备到位 |
| 酒吧人脸/年龄正式核验 | 依赖供应商选型、合规材料和生产授权 | 供应商合同、接口、合规审查和联调环境到位 |
| 独立 Python AI 服务 | 会引入新运行时、部署、监控、模型权限和数据合规成本 | Java 规则处理器无法满足明确场景，且已有数据集、模型、SLA 和安全边界 |

## 4. 推荐先做 P2-A 的理由

- 与 P1 平面图能力天然衔接，不会重写核心交易、票务、酒吧或财务闭环。
- 外部依赖最低：MVP 可先支持 SVG 和标准化 JSON，CAD 文件由人工或工具预转换。
- 价值明确：减少运营手工画多边形，提高商场区域、楼层、商户点位和后续可视化的配置效率。
- 风险可控：导入失败只生成错误报告，不影响已发布平面图；必须人工确认后才发布。

## 5. P2-A 建议 MVP 边界

| 范围 | 说明 |
| --- | --- |
| 导入格式 | SVG、标准化 JSON；CAD/DWG/DXF 不直接解析，先转 SVG/JSON |
| 导入对象 | 楼层图、区域多边形、商户/铺位候选点位 |
| 导入结果 | 生成导入任务、解析摘要、候选区域、错误行、警告和证据附件 |
| 发布方式 | 仅管理员人工确认后发布为新版本 |
| 回滚方式 | 保留旧版本，导入失败或未发布不影响线上版本 |
| 权限 | `floor_plan.import.manage`、`floor_plan.import.view` |
| 审计 | 创建导入、解析完成、发布、撤销、失败均写事件 |

## 6. 下一步

若继续编码，已建立 P2-A 开发计划：`docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md`。执行时按该计划继续 TDD：

1. 新增 Flyway 迁移：导入任务、候选区域、错误明细。
2. 新增 DTO、Repository、Service、Controller。
3. 新增 SVG/JSON 解析器，限制文件大小、节点数量和坐标范围。
4. 新增权限种子和默认管理员授权。
5. 新增单元测试、Controller 测试和 JDBC 集成测试。
6. 在 160 上运行后端全量测试、P0 契约闸口和 Semgrep。

## 7. 当前执行状态

### P2-A 商场平面图 SVG/JSON 导入

状态：已完成；后端导入能力和 Web 管理入口均已完成。

交付物：

- `docs/p2-floor-plan-import-api-contract-v0.1.md`
- `docs/p2-floor-plan-import-completion-audit-v0.1.md`
- `docs/p2-floor-plan-web-entry-completion-audit-v0.1.md`
- `tools/verify-p2-floor-plan-web-entry.mjs`

审计结论：P2-A 只覆盖 SVG/标准 JSON 导入候选和 Web 人工发布入口；CAD/DWG/DXF 原生解析、自动发布和供应商特定导入适配仍不压入本切片。

### P2-B 游乐团体/包场预订与容量锁定

状态：已完成。

交付物：

- `docs/superpowers/plans/2026-06-15-p2-amusement-group-private-booking.md`
- `docs/p2-amusement-group-private-booking-api-contract-v0.1.md`
- `docs/p2-amusement-group-private-booking-completion-audit-v0.1.md`

审计结论：P2-B 只覆盖后台创建、人工审批、容量锁定和释放；价格、退款、改期和销售归属仍不压入本切片。

### P2-C 票务动态定价规则容器

状态：已完成。

交付物：

- `docs/superpowers/plans/2026-06-15-p2-ticket-dynamic-pricing.md`
- `docs/p2-ticket-dynamic-pricing-api-contract-v0.1.md`
- `docs/p2-ticket-dynamic-pricing-completion-audit-v0.1.md`

审计结论：P2-C 已升级为动态规则容器、价格模拟和“已发布规则影响正式订单金额”的闭环；草稿规则仍只参与模拟。

### P2-D 经营分析/预测看板指标沉淀

状态：已完成。

交付物：

- `docs/superpowers/plans/2026-06-15-p2-business-metric-snapshots.md`
- `docs/p2-business-metric-snapshots-api-contract-v0.1.md`
- `docs/p2-business-metric-snapshots-completion-audit-v0.1.md`

审计结论：P2-D 只做指标快照与查询接口，不做预测模型和自动经营建议。

### P2-E 供应商深度联动

状态：边界收口已完成；厂商特定实现待供应商 API/SDK 到位后进入正式联调。

交付物：

- `docs/p2-third-party-deep-integration-boundary-contract-v0.1.md`
- `docs/p2-third-party-deep-integration-boundary-completion-audit-v0.1.md`

审计结论：现有 integration 模块已经覆盖通用供应商适配器、开放平台、Webhook、轮询校验、重试、补偿和对账，不应伪造厂商 SDK 实现。

### P2-F 独立 Python AI 服务

状态：边界收口已完成；正式服务编码暂缓。

交付物：

- `docs/p2-python-ai-service-boundary-contract-v0.1.md`
- `docs/p2-python-ai-service-boundary-completion-audit-v0.1.md`

审计结论：当前 Java 内置规则处理器继续承担生产路径；独立 Python AI 服务只有在模型、SLA、数据集、安全和合规评审到位后再实施。
