# T3-0 编码入口包 V0.1

## 1. 文档定位

本文档是 T3 阶段进入编码前的统一入口包。它不直接启动 T3 功能编码，而是定义 T3 的可做范围、禁止事项、供应商就绪条件、安全闸口、验收方式和下一步切片。

机器标记：T3_CODING_PACKAGE_READY
机器标记：T3_ALLOWED_FIRST_SLICE=T3-A_READONLY_ANALYTICS
机器标记：NO_T3_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY
机器标记：NO_PYTHON_AI_SERVICE_WITHOUT_T3D_GATE
验证命令：`npm run verify:t3-coding-package`

## 2. 总控结论

当前不直接进入 T3-C 机器人/视频/设备真实控制闭环，也不直接进入 T3-D 独立 Python AI 服务。下一步若继续推进 T3，只允许先进入 `T3-A 经营分析只读深化`。

T3-A 的边界是只读分析、集团级趋势、经营指标解释和内部管理看板，不发生产控制指令，不改变支付、退款、核销、票价、库存、员工绩效、商户账单和合同状态。

## 3. T3 切片准入

| 切片 | 当前状态 | 可做范围 | 禁止范围 | 进入条件 |
|---|---|---|---|---|
| T3-A 经营分析只读深化 | 可作为下一步 | 集团级看板、趋势对比、商户健康度解释、员工服务评分摘要、客户意向摘要、机器人经营分析只读结果 | 预测自动决策、自动处罚、自动调价、自动退款、自动核销 | 复用现有 P2-D 指标快照和 analytics 只读接口，不引入真实供应商控制 |
| T3-B 开放平台 SDK 和 mock 深化 | 有条件可做 | SDK 示例、mock 回调、签名样例、重试和补偿样例、供应商联调脚手架 | 使用真实生产密钥、伪造真实供应商成功、绕过平台标准 OpenAPI/Webhook | 外部供应商即将联调，至少有 API 草案或沙箱约束 |
| T3-C 机器人/视频/设备真实控制闭环 | 阻断 | 暂不编码真实控制，只保留边界 | 机器人任务指令、视频调阅控制、设备指令、闸机/NFC 控制、厂商生产回调 | 供应商 API/SDK、签名、回调、错误码、沙箱、额度和验收证据到位 |
| T3-D 独立 Python AI 服务 | 阻断 | 暂不替换 Java 内置规则处理器 | 生产路径 AI 决策、异步模型服务、AI 直接影响处罚/退款/绩效/准入 | 模型方案、样本集、SLA、合规审查、降级 Runbook 和数据边界到位 |

## 4. 必读顺序

| 角色 | 必读 |
|---|---|
| 产品负责人 | 本文档 -> `t3-user-story-map-and-sprint-slices-v0.1.md` -> `t3-entry-assessment-and-coding-package-audit-v0.1.md` |
| 后端 | 本文档 -> `t3-security-and-supplier-readiness-gate-v0.1.md` -> `analytics`/`integration` 现有代码 -> OpenAPI |
| Web | 本文档 -> `t3-user-story-map-and-sprint-slices-v0.1.md` -> `apps/web/src/main.tsx` |
| 测试 | 本文档 -> T3 验收标准 -> 现有 `verify:p0`、`verify:security-baseline`、`verify:t3-coding-package` |
| 安全/运维 | `t3-security-and-supplier-readiness-gate-v0.1.md` -> `coding-host-protection-baseline-v0.1.md` -> `production-readiness-implementation-ledger-template-v0.1.md` |
| 第三方供应商 | `third-party-supplier-integration-kit-v0.1.md` -> 本文档 -> `t3-security-and-supplier-readiness-gate-v0.1.md` |

## 5. 反模式

| 反模式 | 处理 |
|---|---|
| 没有 T3-0 包就直接写 T3 Controller、表或页面 | 阻断 |
| 把 T3-A 只读分析写成可执行控制指令 | 阻断 |
| 在供应商 API/SDK 未到位前实现机器人、视频、设备真实控制 | 阻断 |
| 在没有模型/SLA/合规材料前引入独立 Python AI 服务 | 阻断 |
| AI 输出直接影响付款、退款、核销、处罚、调价、绩效或合同状态 | 阻断 |
| 用真实生产密钥做 mock 或 SDK 示例 | 阻断 |
| 跨运营主体、合同主体、结算主体汇总时丢失主体边界 | 阻断 |

## 6. 开发计划模板

T3 任一切片开工前必须有独立计划，至少包含：

1. 修改文件和新增文件。
2. 数据结构和接口变化。
3. 只读/写入/控制边界。
4. 权限、审计事件、数据范围和主体边界。
5. 测试点、验收标准和边界情况。
6. 安全扫描、隐私检查、回滚方式。
7. 是否依赖供应商资料；若依赖，必须引用就绪证据。

## 7. T3-A 最小可编码建议

若继续编码，推荐只做 T3-A：

- 在现有 Web 管理端增加只读 T3 经营分析入口。
- 复用 P2-D 指标快照、analytics 风控、商户健康度、员工服务评分、客户意向、机器人经营分析只读数据。
- 后端如需补口径，先补测试，再补只读查询或聚合，不新增生产控制写接口。
- 不引入 Python 服务，不接真实机器人/视频/设备控制。

## 8. 验收标准

| 场景 | Given | When | Then |
|---|---|---|---|
| T3 包完整 | 接手人准备进入 T3 | 执行 `npm run verify:t3-coding-package` | 必须输出 `T3_CODING_PACKAGE_PASS` |
| T3-A 只读 | 运营查看集团级分析 | 打开 T3-A 页面或接口 | 只能读取指标和解释，不产生生产控制动作 |
| 供应商未就绪 | 缺少机器人/视频/设备 API | 尝试启动 T3-C | 必须被文档和计划阻断 |
| AI 服务未就绪 | 缺少模型、样本、SLA、合规审查 | 尝试启动 T3-D | 必须被文档和计划阻断 |
| 完整验证 | T3-0 文档变更 | 执行质量门 | 至少通过 `verify:t3-coding-package`、`verify:p0-contract`、`test:backend`、`build`、`verify:security-baseline` 和 Semgrep |

## 9. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-15 | V0.1 | 新增 T3-0 编码入口包，裁决下一步只允许优先进入 T3-A 只读经营分析；T3-C/T3-D 在供应商与 AI 治理资料到位前阻断。 |
