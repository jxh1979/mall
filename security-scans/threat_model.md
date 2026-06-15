# 综合体管理系统正式安全基线重建说明

> **状态：当前文件不再承载旧原型威胁模型。**
>
> 旧 TypeScript / Fastify / Prisma / SQLite 预研原型相关安全扫描结论已经废止，只能通过 `security-scans/LEGACY-SECURITY-SCANS.md` 作为历史问题追溯入口查看，不得作为当前正式系统的安全基线、接口范围、数据库边界或上线依据。

## 1. 当前正式安全基线口径

| 项目 | 正式口径 |
|---|---|
| 后端 | Java 21 + Spring Boot 4.x |
| 数据库 | PostgreSQL |
| 迁移 | Flyway |
| 接口契约 | `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json` |
| 权限与终端 | `docs/p0-operation-permission-matrix-v0.1.md`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` |
| 事件与证据 | `docs/p0-action-event-evidence-matrix-v0.1.md` |
| 160 运维 | `docs/server-160-runbook-v0.2.md`、`ops/160/*.sh` |

## 2. 禁止事项

1. 不得引用旧 Node/Fastify/SQLite 原型扫描结论作为正式 Java/Spring/PostgreSQL 系统风险结论。
2. 不得以旧 `/api/admin/*`、`/api/mobile/*`、`/api/customer/*`、`/api/bar/*`、`/api/integration/*` 路径作为正式安全测试范围。
3. 不得从旧 Prisma/SQLite 数据模型推导正式 PostgreSQL/Flyway 安全边界。
4. 不得把旧原型的内网演示漏洞、默认账号、历史密钥或旧部署拓扑继续带入当前上线审计。

## 3. 正式威胁模型重建输入

正式安全威胁模型必须在下列输入稳定后重新生成：

| 输入 | 最低要求 |
|---|---|
| 运行时后端 | `backend/` 已按 Spring Boot 4.x 启动，并输出运行时 OpenAPI |
| 数据库迁移 | Flyway 首批业务迁移已覆盖主体、权限、票务、权益、酒吧、游乐、结算、事件和证据基础表 |
| 鉴权授权 | 员工、设备、客人、开放应用、Webhook 签名五类身份上下文已可验证 |
| 支付与第三方 | 聚合支付、直连接口抽象、Webhook + 定时轮询校验、幂等和补偿框架已落地 |
| 运维部署 | 160 或等效环境完成配置预检、密钥台账、NAS 备份、告警和回滚演练 |

## 4. 重建后必须覆盖的安全审计域

| 审计域 | 必查内容 |
|---|---|
| 认证授权 | 登录、Token 生命周期、RBAC、数据范围、权限模板和单人覆盖 |
| 终端安全 | 工作手机、平板、一体机、设备凭证、MDM/Android Enterprise、离线查看边界 |
| 票务权益 | 出票、核销、权益领取、卡座入场、酒水领取、库存扣减、退款冲正 |
| 酒吧高风险 | 年龄/人脸核验、最低消费、扫码支付、高价值酒水在线核销、存酒与提成 |
| 第三方接入 | 开放 API、Webhook 签名、防重放、轮询校验、对账、异常补偿 |
| 财务结算 | 应收应付台账、主体绑定、结算单、退款、差异、凭证和审计留痕 |
| 数据保护 | 个人信息最小化、日志脱敏、证据留存、备份加密、密钥轮换 |
| 运维安全 | `/etc/complex/complex.env`、`~/.complex/complex.env`、systemd、NAS、告警、回滚权限 |

## 5. 验收标准

Given 安全审计人员打开 `security-scans/threat_model.md`，  
When 判断当前正式系统安全基线，  
Then 只能看到 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 的重建要求，不会继续看到旧 TypeScript/SQLite 原型威胁模型正文。

Given 正式后端和 Flyway 迁移进入生产级验收，  
When 执行 P0 上线安全审计，  
Then 必须基于运行时 OpenAPI、PostgreSQL/Flyway 表、权限矩阵、事件矩阵、160 Runbook 和真实部署配置重新生成威胁模型。
