# 阶段 6：游乐、终端、第三方与数据分析审计

状态：**完成（文档审计）**
日期：2026-07-28
基线：`31f262c5`
约束：仅文档；不改代码；不批量改业务 PRD 正文

## 范围
- 本阶段覆盖剩余规则：**111** 条（应清空 ledger 缺口）
- 领域分布：terminal 11 / service 4 / amusement 32 / merchant 3 / thirdparty 35 / data 26

## 覆盖统计
| status | count |
|---|---:|
| FULL | 99 |
| PARTIAL | 12 |

## 强制对抗场景
| ID | 场景 | 严重度 | 结果 | 缺口 |
|---|---|---|---|---|
| ADV-ED-001 | 游乐开放前巡检缺项、严重项未关闭、异常关闭后越权重开 | P0 | COVERED |  |
| ADV-ED-002 | 团体或包场越界占用空间、超人数、无主管审批 | P1 | COVERED |  |
| ADV-ED-003 | 身高、年龄、健康、安全告知和监护要求互相冲突 | P1 | PARTIAL | 缺监护 |
| ADV-ED-004 | 终端模式错误、配置拉取失败、离线与开关关闭后的安全默认值 | P0 | COVERED |  |
| ADV-ED-005 | 第三方回调重复、乱序、失败、超时和供应商不可用 | P0 | COVERED |  |
| ADV-ED-006 | 第三方人工录入数据冒充系统自动来源 | P0 | COVERED |  |
| ADV-ED-007 | 指标口径分子、分母、时间窗、去重规则或主体不同 | P0 | PARTIAL | 缺去重;时间窗 |
| ADV-ED-008 | 无数据、延迟数据、回补数据和跨项目聚合被呈现为实时真实值 | P1 | PARTIAL | 缺回补;无数据;新鲜 |
| ADV-ED-009 | 通知开关层级冲突或重要通知无责任主体 | P1 | COVERED |  |

## 方法与局限
检索启发式 + 维度检查 + 对抗抬升；FULL 不代表实现已上线。

## 产物
- phase6-adversarial-findings.csv / phase6-issue-rows.csv
- html-prd-coverage.csv（补全剩余规则）

## 完成声明
阶段 6 文档审计完成；OPEN 项以 DEFERRED-DOCS-FOLLOWUP 登记，不阻塞 phase7 覆盖完整性。
