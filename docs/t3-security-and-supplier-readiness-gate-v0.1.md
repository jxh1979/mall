# T3 安全与供应商就绪闸口 V0.1

机器标记：T3_SECURITY_GATE_READY
机器标记：robot_platform=blocked_until_supplier_api
机器标记：video_platform=blocked_until_supplier_api
机器标记：device_vendor=blocked_until_supplier_api
机器标记：python_ai_service=blocked_until_model_sla_data_review

## 1. 闸口结论

T3 不允许把供应商未确定、AI 未治理、视频/设备未合规的能力伪装成已完成生产闭环。当前只允许 T3-A 只读经营分析作为下一步编码候选。

## 2. 供应商就绪矩阵

| 能力 | 当前状态 | 缺口 | 未到位时允许做 | 未到位时禁止做 |
|---|---|---|---|---|
| 机器人平台 | blocked | API/SDK、签名、回调、任务状态、错误码、额度、沙箱 | 字段映射、mock、SDK 示例草案 | 真实任务指令和生产控制 |
| 视频平台 | blocked | 播放鉴权、录像索引、片段截取、保留期限、调阅审计 | 视频证据字段、mock 链接、权限设计 | 真实视频调阅和自动证据绑定 |
| 设备厂商/NFC/闸机 | blocked | 型号、SDK、签名、离线策略、现场网络、失败回退 | 设备能力字典和 mock 状态 | 真实开闸、锁定、放行和远程控制 |
| 企业微信深化 | pending_info | 会话存档、回调、授权范围、同步失败样例 | 基础同步和规则质检 | AI 深度质检自动处罚 |
| Python AI 服务 | blocked | 模型、样本、SLA、数据边界、部署、监控、安全审查 | Java 规则处理器、只读摘要 | 替换生产路径或直接做决策 |
| 支付/渠道 SDK 深化 | pending_info | 渠道 SDK、对账文件、退款权限、签名 | 抽象接口和 mock | 真实资金动作 |

## 3. 数据治理

| 数据 | T3-A 允许 | T3-B 允许 | T3-C/T3-D 前置 |
|---|---|---|---|
| 客户数据 | 聚合指标、脱敏摘要 | mock 样本 | PIPL 最小化、保留期限、访问审批 |
| 员工行为 | 评分摘要、服务趋势 | 脱敏样例 | HR/法务确认用途和申诉机制 |
| 视频证据 | 不直接播放真实视频 | mock URL | 调阅审批、播放 token、留存和删除策略 |
| AI 输出 | 只读辅助说明 | 离线评估样例 | 模型审查、SLA、回退、人审和误伤处理 |
| 跨主体经营数据 | 按授权主体聚合 | mock 集团视图 | 主体绑定、数据范围和审计确认 |

## 4. 安全闸口

T3 任一编码切片必须满足：

1. 仍在 160 仓库执行，先通过 `verify:coding-host-protection`。
2. 新增接口必须进入 OpenAPI、权限、终端、审计事件和验收矩阵。
3. 写接口必须有幂等、权限、审计事件、失败事件和回滚策略。
4. 供应商回调必须有签名、时间戳、nonce、重放保护和验签失败事件。
5. SDK/mock 示例不得包含真实密钥、真实客户身份数据或真实生产账号。
6. AI 输出不得直接影响支付、退款、核销、处罚、调价、绩效和合同状态。
7. 视频/设备/机器人真实控制必须有人工审批、权限、审计和失败回退。

## 5. 验证命令

T3-0 文档阶段至少运行：

- `npm run verify:t3-coding-package`
- `npm run verify:p0-contract`
- `npm run test:backend`
- `npm run build`
- `npm run verify:security-baseline`
- Semgrep `p/owasp-top-ten` + `p/secrets`

T3 功能编码阶段还必须按实际新增接口补充后端测试、Web 测试或供应商 mock 测试。

## 6. 回滚原则

- T3-A 只读分析可通过隐藏入口或回滚 Web/只读接口恢复。
- T3-B SDK/mock 可停用 mock endpoint 或撤销示例包。
- T3-C/T3-D 一旦进入真实生产控制，必须有供应商侧和平台侧双向回滚 Runbook；当前尚未满足。

## 7. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-06-15 | V0.1 | 新增 T3 安全与供应商就绪闸口，阻断真实供应商控制和独立 Python AI 服务的过早编码。 |
