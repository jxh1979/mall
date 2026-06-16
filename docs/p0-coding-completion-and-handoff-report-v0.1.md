# P0 编码完成与交接报告 V0.1

## 1. 文档定位

本文档用于记录综合体管理系统 P0 首批编码包在 192.168.88.160 机器上的正式编码完成状态、验证证据、质量结论和后续交接边界。

它不是新的需求文档，也不扩大 P0 范围。若本文档与旧进度描述冲突，以本文档、`00-current-effective-document-entry-v0.1.md`、`project-progress-dashboard.md` 和 `p0-implementation-task-list.md` 的最新状态为准。

## 2. 当前结论

| 项 | 结论 |
|---|---|
| 编码环境 | 192.168.88.160，工程目录 `/home/james/complex-management` |
| 正式技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven |
| P0 API 覆盖 | OpenAPI 198 个 operation 与 Java Controller 198 个映射一致，缺失 0，多余 0 |
| 后端测试 | 557 个测试通过，0 失败 |
| 前端验证 | Web typecheck 与 Vite production build 通过 |
| Android 验证 | Android 环境、核心构建、smoke、debug APK 均通过 |
| 运行态验证 | API health、OpenAPI endpoint、Web、PostgreSQL、Redis、NATS、MinIO 均通过 |
| 总闸口 | `/opt/complex/scripts/complex-verify-p0.sh` 输出 `P0 verification PASS` |
| 交接状态 | P0 编码可收口；生产上线仍受生产前置台账和正式安全基线约束 |

## 3. 本轮最后修复项

| 缺口 | 修复结论 |
|---|---|
| OpenAPI 有 `/api/v1/auth/refresh`、`/api/v1/auth/logout`，Java Controller 缺失 | 已补齐两个接口 |
| 登录响应缺少 refresh token / session id | 已补齐并保留 access token / token type / user |
| refresh token 复用风险 | refresh 成功后轮换 access token 和 refresh token，旧 refresh token 失效 |
| logout 后 token 仍可用风险 | logout 成功后当前 access token 和 refresh token 失效 |
| refresh/logout 被误放行为免登录接口风险 | 安全配置只放行 `/api/v1/auth/login`，refresh/logout 仍要求 Bearer |
| `login_name` 与旧 `username` 兼容 | 正式字段为 `login_name`，后端通过 `@JsonAlias("username")` 兼容旧 payload |
| 空请求体或 null request 500 风险 | 已补测试并返回 4xx `VALIDATION_FAILED` |
| 权限仓储接口残留默认未实现抛错 | 已改为抽象接口方法，缺实现会在编译期暴露；测试替身已补齐最小实现 |
| 审计事件仓储残留未使用抛错映射函数 | 已移除未使用方法和相关导入，避免后续审计误判为 P0 留痕查询缺口 |
| 根目录 `npm run verify:p0` 直接执行缺 Android smoke 配置 | `tools/run-android-gradle.mjs` 已加载 `/etc/complex/complex.env` 和 `~/.complex/complex.env`，同时保留显式环境变量优先级 |
| 生产 seed admin 配置存在时可能覆盖已轮换密码 | 已改为仅首次插入凭证，已有凭证不覆盖；补 `ProductionAuthJdbcIntegrationTests` 防回归 |
| 幂等业务 action 失败后可能长期卡在 `IN_PROGRESS` | 已补 `FAILED_RETRYABLE` 状态，同 key 同请求可重试并在成功后覆盖为 `OK`；补 `IdempotencyServiceTests` 防回归 |

## 4. 验证证据

| 验证项 | 命令 | 结果 |
|---|---|---|
| 认证目标测试 | `cd backend && mvn -q -Dtest=AuthControllerTests,AuthServiceTests test` | 通过 |
| 契约验证 | `npm run verify:p0-contract` | 通过；198 operations checked |
| Controller 映射对比 | OpenAPI method+path vs `*Controller.java` | 198 operations，missing 0，extra 0 |
| 完整 P0 验证 | `/opt/complex/scripts/complex-verify-p0.sh` | 通过；最终输出 `P0 verification PASS` |
| 根目录完整 P0 验证 | `npm run verify:p0` | 通过；包含契约、安全基线、557 后端测试、Web build、Android 环境、Android smoke 和 debug APK |

## 5. 已实现能力边界

P0 编码验收已覆盖以下最小闭环：

1. 平台组织、商户、场馆、区域、点位、主体绑定和基础治理。
2. 员工账号、认证、权限、临时授权、权限模板和操作留痕。
3. 票务、票种、权益、订单、凭证、核销、客户订单和渠道/资格规则。
4. 卡座、酒吧订单、账单、赠酒、最低消费边界和支付抽象。
5. 库存、存酒、调拨、盘点、损耗、报损和可追责流水。
6. 游乐项目、场次、容量、安全巡检、暂停、整改复检、主管恢复和核销安全状态。
7. 完整应收应付台账、财务经营口径和结算相关记录。
8. 第三方双向开放、开放平台、供应商 adapter、Webhook/轮询校验和异常补偿框架。
9. Android 工作手机/平板/一体机 P0 smoke、设备绑定、在线核销、离线任务队列和返回阻断。
10. 生产前置资料台账、160 运行配置、健康检查和部署验证。

## 6. 仍不得误判为已完成的事项

| 事项 | 当前状态 | 约束 |
|---|---|---|
| 生产 MDM/Android Enterprise | 生产资料待 IT/采购落地 | 未纳管设备不得作为生产工作终端 |
| 真实聚合支付生产商户 | 资料和密钥待财务/IT 提供 | 不得生产收款或退款上线 |
| 酒吧年龄/人脸核验供应商 | SDK/API/设备待采购 | 供应商未配置时只能 mock，不得生产通过 |
| NAS 备份与恢复演练 | NAS 为 `192.168.88.113`，协议 SMB，共享 `bn`，账号 `jxh1`，160 挂载 `/mnt/complex-nas-backup`，保留 30 天，容量上限 1TB；当前不启用备份归档加密；真实挂载、读写删除、NAS 同步备份和从 NAS 恢复演练已通过 | 生产前仍需固化开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练 |
| 生产账号、数据库、对象存储、JWT/第三方密钥轮换 | 台账模板、分类型 Runbook 和 `verify:production-credentials` 已建立；真实生产 credential_ref、到期日和轮换证据待运维录入 | 不得在文档或代码中写明文密钥 |
| 正式安全基线 | P0 编码完成后应生成 V1 | findings 未处理或未接受风险前不得上线 |
| 域名 HTTPS | 总控确认暂不绑定 | 不阻塞内网预生产，不代表公开生产就绪 |

以上事项不纳入 P0 编码完成范围；它们只阻塞真实生产、长期预生产或对应外部联调，不反向否定 160 上已经完成的 P0 编码验收结论。

## 7. 下一步交接动作

| 优先级 | 动作 | 负责人 |
|---|---|---|
| P0-A | 按 `formal-security-baseline-generation-task-v0.1.md` 生成正式安全基线 V1 和 findings register | 技术负责人 / 安全 |
| P0-A | 按 `production-readiness-implementation-ledger-template-v0.1.md` 初始化真实生产前置台账 | 运维 / IT / 采购 / 财务 |
| P0-A | 将 160 用户级运行配置迁移为长期预生产或生产配置：`/etc/complex/complex.env`、systemd、日志、告警 | 运维 / IT |
| P0-A | 固化备份恢复演练、真实 credential_ref、密钥到期提醒、轮换证据、支付回调和 NAS 证据归档 | 运维 / 财务 / 安全 |
| P0-B | 供应商 SDK/API 到位后补字段映射、签名、回调、对账和异常补偿实测记录 | 集成开发 / 采购 / 供应商 |
| P1 | 进入酒吧 SaaS 增强、商户服务工单、游乐电子免责/事故工单、商场平面图、合同审批附件归档等 P1 排期 | 产品 / 开发 / 测试 |

## 8. 交付结论

P0 首批编码包已经从“文档可编码”推进到“160 机器正式实现并通过完整自动化验收”。后续不得再把 `p0-first-coding-package-v0.1.md` 解读为待开始编码；它现在作为已实现范围、验收回归和风险边界的历史执行包使用。

生产上线前的主线工作不再是继续补 P0 业务代码，而是完成生产前置资料、正式安全基线、供应商真实资料接入、运维配置生产化和风险接受流程。

## 9. 变更日志

| 日期 | 类型 | 内容 |
|---|---|---|
| 2026-06-14 | 新增 | 记录 160 P0 编码完成、认证缺口修复、198 operations 对齐、557 后端测试通过和 `P0 verification PASS` 结论 |
| 2026-06-14 | 改 | 按总控口径明确外部采购/真实供应商/真实 NAS 账号不纳入 P0 编码完成范围；NAS 协议收口为 SMB |
| 2026-06-14 | 改 | 清理 P0 代码中残留未实现抛错痕迹，补齐根目录 `npm run verify:p0` 对 160 运行配置的加载能力，并重新通过完整 P0 验证 |
| 2026-06-14 | 改 | NAS 参数补齐为 `//192.168.88.113/bn`、账号 `jxh1`、160 挂载 `/mnt/complex-nas-backup`、30 天保留、1TB 容量上限；当前不启用备份归档加密 |
| 2026-06-14 | 验证 | 已完成真实 NAS 挂载读写、无加密 `.tar.gz` 备份同步、`.sha256` 校验和从 NAS 归档恢复演练；最新 evidence 为 `docs/production-readiness-evidence/nas-restore-drill-latest.json` |
| 2026-06-15 | 改 | 修复生产 seed admin 重启/重复初始化覆盖已轮换密码风险；补充红绿测试验证 |
| 2026-06-15 | 改 | 修复幂等 action 失败后 `IN_PROGRESS` 卡死风险；失败记录进入 `FAILED_RETRYABLE` 并允许同请求重试 |
| 2026-06-15 | 验证 | 当前 P0 契约口径更新为 198 operations，后端测试口径更新为 557 tests；生产前置真实证据仍按台账待回填，不因代码修复自动关闭 |
