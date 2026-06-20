# 160 服务器运维 Runbook V0.2

## 1. 文档定位

本文合并 160 环境记录、运维实施设计和早期运维计划的执行口径，作为 160 服务器后续部署、回滚、备份、恢复、监控和安全加固的统一入口。

原 160 环境记录、运维实施设计和早期运维计划已归档至：

`_archive/20260610-retention-audit/merged-source-docs/`

## 2. 基础信息

| 项 | 值 |
|---|---|
| SSH 别名 | `codex-160` |
| 地址 | `192.168.88.160` |
| 用户 | `james` |
| 项目目录 | `/home/james/complex-management` |
| 系统 | Ubuntu 24.04.4 LTS |
| 正式技术栈 | Java 21 + Spring Boot 4.x + PostgreSQL + Flyway + Maven |
| NAS 服务器 | `192.168.88.113` |

## 3. 运行环境

已安装并作为后续实施基础：

- Java 21、Maven、Python、Docker、Docker Compose、Nginx。
- PostgreSQL、Redis、NATS、MinIO。
- Android SDK 与构建工具。
- 标准目录：`/opt/complex`、`/etc/complex`、`/var/log/complex`、`/srv/complex`。

环境细节以后续实施复核记录和本文 Runbook 为准。

## 3A. 2026-06-13 实机更新快照

本节记录 160 机器按最新正式技术栈完成的维护快照，用于防止旧配置继续被误认为当前口径。

| 项 | 当前状态 |
|---|---|
| 旧项目备份 | 已备份到 `~/complex-management-backups/20260613-101406/`，包含项目目录和 `/opt/complex/scripts` 脚本归档 |
| 旧项目回滚入口 | 原 `/home/james/complex-management` 已保留为 `/home/james/complex-management.previous-20260613-101439` |
| 当前项目目录 | `/home/james/complex-management` 已同步为最新正式工程，包含 `backend/pom.xml`、Spring Boot 4.x、PostgreSQL/Flyway 和 OpenAPI 契约 |
| 运维脚本 | `/opt/complex/scripts` 已按本仓库 `ops/160/*.sh` 同步，脚本口径为 Java/Spring Boot/PostgreSQL/Flyway |
| Node 依赖 | 已在 160 执行 `npm ci`；`npm audit` 当前提示 3 个 high 严重度依赖风险，需后续安全维护评估，不得自动 `--force` 升级破坏构建 |
| 环境校验 | `npm run verify:p0-env` 已通过，160 具备 Java 21、Maven 3.9+、Gradle/Android SDK 前置能力 |
| 契约校验 | `npm run verify:p0-contract` 已通过，181 个 OpenAPI operation、示例、验收矩阵、追溯附录和客户端闸口已闭合 |
| Web 构建 | `npm run verify:p0-web` 已通过，Web typecheck 与生产构建可执行 |
| 运行配置 | `/etc/complex/complex.env` 仍保留旧配置且当前无免密 sudo 写入权限；已新增用户级覆盖配置 `~/.complex/complex.env`，将旧数据库 URL 转为 `jdbc:postgresql://127.0.0.1:5432/complex`，并把数据库账号/密码拆为独立变量。该覆盖只作为 160 开发/预生产实跑兜底；正式运维仍应由 IT/运维把同口径写入 `/etc/complex/complex.env` |
| 运行配置预检 | `complex-runtime-config-check.sh` 已通过，确认 PostgreSQL、Redis、NATS、MinIO 均运行，且 profile/API port/数据库 URL 口径符合 Spring Boot 4.x |
| API 运行态 | `complex-deploy-api.sh` 已发布 release `20260613-193142`。因 sudo 需要密码，systemd 写入未完成，脚本已回退为非 systemd 方式启动；当前 API 进程监听 `127.0.0.1:8080`，`/actuator/health`、`/api/v1/system/health` 和 `/docs/openapi` 均可访问；运行时 OpenAPI 描述已确认包含 `Java 21 + Spring Boot 4.x` |
| Web 运行态 | `complex-deploy-web.sh` 已发布 release `20260613-180329`，以 `vite preview` 方式监听 `127.0.0.1:3000`；Vite chunk size 仅为构建优化告警，不作为 P0 阻断 |
| BE-000 验收 | `complex-verify-be000.sh` 已在运行态通过，覆盖 `verify:backend-env`、`npm run test:backend`、Maven package、jar 产物检查、API 健康检查和运行时 OpenAPI 输出 |
| 整体健康检查 | `complex-health-check.sh` 已通过，覆盖 API actuator、系统健康接口、OpenAPI、Web 和基础容器 |
| 完整 P0 验收 | `complex-verify-p0.sh` 已通过，并于 2026-06-13 19:32 复核通过。覆盖契约、后端测试、BE-000 运行态、Web 构建、Android 环境、Android 核心构建、Android smoke、APK 和整体健康检查；Android smoke 输出 `health/login/device_bound/online_verification/sync/return` 等关键断言均为通过 |

判定规则：160 当前可标记为“API/Web/BE-000/完整 P0 自动化 smoke 已通过”。但当前 P0 smoke 后端业务接口仍是 `local/dev/test` profile 下的内存态验收切片，只用于证明正式路径、统一响应、鉴权、Android 端流程和 160 环境可跑通；不得据此把平台、票务、权益、核销、证据、设备等业务域标记为生产级实现完成。生产级完成仍必须落 PostgreSQL + Flyway + Service/Repository + 权限/事件/审计。

## 4. 标准目录

```text
/opt/complex
├── api
│   ├── releases
│   ├── current -> releases/<release-id>
│   └── complex-api.jar -> current/complex-api.jar
├── web
│   ├── releases
│   └── current -> releases/<release-id>/dist
├── backups
├── scripts
└── tmp

/etc/complex
├── complex.env
├── backup.env
├── alert.env
└── nginx

/var/log/complex
├── api.log
├── deploy.log
├── backup.log
├── restore.log
└── monitor.log
```

## 5. 发布原则

1. 发布前备份配置和当前版本信息。
2. 新版本进入独立 release 目录。
3. 使用软链接切换版本，不直接覆盖旧版本。
4. 切换后执行健康检查。
5. 健康检查失败则回滚上一版本。
6. 发布、验证、回滚结果写入 `/var/log/complex/deploy.log`。
7. 发布脚本不保存明文密码，不把敏感配置写入 release 目录。

## 5A. AI-ready 四件套执行入口

本文是 160 服务器运维 Runbook，只定义发布、备份、恢复、监控和安全操作口径，不替代正式工程、接口契约或数据库迁移。进入实施前必须满足下表。

| 四件套 | 本文执行要求 | 权威引用 |
|---|---|---|
| 反模式 | 不得在 160 上延续 TypeScript/SQLite 旧原型作为正式后端；不得把明文密码写入 release 目录、脚本日志或文档；不得跳过备份直接发布 | `prototype-deprecation-and-technology-baseline-v0.1.md`、`code-contract-difference-and-migration-audit-v0.1.md`、本文 §5/§8 |
| 测试用例 | 发布、回滚、健康检查、备份、恢复演练、密钥轮换和告警必须有可重复执行步骤和验收结果 | `p0-implementation-task-list.md`、`project-progress-dashboard.md`、本文 §6/§7/§9 |
| 错误处理 | 必须覆盖健康检查失败、发布失败、回滚失败、备份失败、NAS 不可用、磁盘不足、密钥过期和服务不可用告警 | 本文 §6/§7/§9、`p0-end-to-end-acceptance-and-edge-cases-v0.1.md` |
| 精确引用 | 任何脚本、目录、环境变量和服务名必须写明完整路径或正式名称；不得用“服务器上那个脚本”“相关配置”等模糊表述 | 本文 §2/§4/§6、`00-current-effective-document-entry-v0.1.md` |

## 6. 常用操作

健康检查：

```bash
/opt/complex/scripts/complex-health-check.sh
```

BE-000 正式后端工程验收：

```bash
cd /home/james/complex-management
/opt/complex/scripts/complex-verify-be000.sh
```

该脚本会先执行 `npm run verify:backend-env`，确认 Java 21+、Maven Wrapper 或 Maven 3.9+、`backend/pom.xml` 可用，再执行后端测试、打包、健康检查和 OpenAPI 输出验收。

后端 Java 测试和 BE-000/P0 验收默认在 160 机器执行。本机 Codex 可执行 `verify:p0-contract`、文档生成、静态脚本和任务拆解；若本机缺少 Java 21，不得把本机 `npm run test:backend` 失败解读为后端代码失败。需要跑后端测试时使用：

```bash
ssh codex-160 "cd /home/james/complex-management && npm run test:backend"
ssh codex-160 "cd /home/james/complex-management && /opt/complex/scripts/complex-verify-be000.sh"
ssh codex-160 "cd /home/james/complex-management && /opt/complex/scripts/complex-verify-p0.sh"
```

P0 验收：

```bash
/opt/complex/scripts/complex-verify-p0.sh
```

该脚本会先执行 `npm run verify:p0-contract`，覆盖 OpenAPI、示例、operation 矩阵、追溯附录、客户端路径/方法/字段闸口，再通过 BE-000 脚本执行 `verify:backend-env`、后端测试、Web 构建、`verify:android-env`、Android 核心构建、smoke、APK 和整体健康检查。Android 不得因 Gradle 或 SDK 缺失被静默跳过。脚本会读取 `/etc/complex/complex.env` 和 `~/.complex/complex.env`，并在 Android smoke 前要求 `P0_ADMIN_USERNAME`、`P0_ADMIN_PASSWORD`；该账号必须是专用种子测试账号，不得使用真实生产管理员密码。

发布：

```bash
/opt/complex/scripts/complex-runtime-config-check.sh
/opt/complex/scripts/complex-deploy-api.sh
/opt/complex/scripts/complex-deploy-web.sh
```

`complex-runtime-config-check.sh` 只输出非敏感状态，不打印数据库密码或对象存储密钥。它会强制检查 `COMPLEX_DATABASE_URL`、`COMPLEX_DATABASE_USERNAME`、`COMPLEX_DATABASE_PASSWORD`、PostgreSQL 可连接性和基础容器状态；`COMPLEX_DATABASE_URL` 必须使用 `jdbc:postgresql://host:port/database` 格式，不得使用旧 `postgresql://user:password@host:port/database` 形态，也不得把数据库账号密码塞进 URL；`COMPLEX_DATABASE_PASSWORD` 不得依赖后端默认值隐式兜底。若该脚本失败，不得执行 API 发布或 API 回滚。

160 运行配置模板见 `ops/160/complex.env.example`。复制到 `/etc/complex/complex.env` 时，`COMPLEX_DATABASE_USERNAME`、`COMPLEX_DATABASE_PASSWORD`、MinIO 密钥、`JWT_SECRET` 和 Android smoke 专用账号密码必须来自账号密钥台账或 IT/运维配置，不得写入真实密钥到仓库文档。若无 sudo 写入权限，160 开发/预生产可临时使用 `~/.complex/complex.env` 覆盖，但正式运维仍应统一落入 `/etc/complex/complex.env`。

### 5.1 P0-A no-system-write boundary

本轮 P0-A 收口确认：Codex 不允许直接写入 160 的系统级生产配置，不安装服务或定时器，不修改开机挂载配置，不写 root-owned secret 文件。Codex 只能交付用户级预检脚本、非安装模板、校验器、Runbook 和证据占位。

可由 Codex 生成并验证：

1. `ops/160/complex-nas-preflight.sh`：检查 `/mnt/complex-nas-backup` 挂载、读写删除、容量和备份目标。
2. `ops/160/complex-backup-schedule.example`：给 IT 的定时备份模板，只描述命令、环境、频率、回滚和验收，不自动安装。
3. `ops/160/complex-backup-alert.example`：给 IT 的备份失败、容量告警和凭证到期提醒模板，只写 `credential_ref` 或 secret reference。
4. `docs/production-readiness-evidence/p0a-ops-hardening-latest.json`：记录 P0-A 无系统写入边界和最新可验证状态。

必须由 IT/运维在受控维护窗口执行并回填证据：

1. 开机自动挂载。
2. 定时备份。
3. 容量告警。
4. 失败告警。
5. 周期性恢复演练。

回滚：

```bash
/opt/complex/scripts/complex-rollback-api.sh <release-id>
/opt/complex/scripts/complex-rollback-web.sh <release-id>
```

备份：

```bash
/opt/complex/scripts/complex-backup.sh manual
```

Android APK：

```bash
cd /home/james/complex-management
npm run verify:android-env
npm run android:p0:apk
```

## 7. 备份策略

| 对象 | 方式 | 建议频率 |
|---|---|---|
| PostgreSQL | `pg_dump` | 每日 |
| MinIO 数据 | 文件归档或 NAS 同步 | 每日 |
| NATS 数据 | 文件归档 | 每日 |
| Redis 数据 | RDB/AOF 文件归档 | 每日 |
| `/etc/complex` | 文件归档 | 每次部署前 |
| Docker Compose 配置 | 文件归档 | 每次部署前 |
| Nginx 配置 | 文件归档 | 每次变更前 |

备份执行默认采用 NAS 方案，NAS 服务器为 `192.168.88.113`，挂载协议已确认为 SMB，共享为 `bn`，UNC 为 `//192.168.88.113/bn`，160 本地挂载目录为 `/mnt/complex-nas-backup`。当前 SMB 用户名为 `jxh1`，凭证引用为 `cred://prod/nas/smb/complex-backup-rw`，保留 30 天，容量上限 1TB；密码不得写入文档、聊天、脚本仓库或日志。当前总控口径为备份归档不加密，因此 160 运行配置必须显式设置 `ALLOW_UNENCRYPTED_BACKUPS=true`；备份脚本通过 `NAS_BACKUP_TARGET_DIR` 写入已挂载目录，生产恢复验收时设置 `REQUIRE_NAS_BACKUP_SYNC=true`。

备份脚本只备份正式 PostgreSQL、MinIO、NATS、Redis、配置和文档资产；旧 `apps/api` SQLite 原型库不得进入正式生产备份。

## 8. 安全与账号

1. 生产环境账号、密钥、数据库和对象存储密码采用人工台账 + 到期提醒 + Runbook 轮换。
2. 开发环境密码不得直接进入生产。
3. 生产前必须轮换数据库、MinIO、Redis、NATS、第三方接口和管理员账号密码。
4. 密钥轮换必须记录责任人、时间、影响范围、回滚方式和验证结果。

## 9. 告警与监控

P0 采用低成本可落地方案：健康检查脚本 + 定时任务 + 日志文件落地，并将关键失败事件接入企业微信或邮件告警。本节把原“建议”细化为可操作配置步骤，告警策略以 `ops/160/complex-backup-alert.example`（事件名、容量阈值、凭证引用、回退渠道占位）为模板，由 IT/运维在受控窗口落地。本 Runbook 只定义口径，不在仓库中写入真实 webhook、SMTP 密码、API token 或密钥；所有密钥一律以 `cred://...` 凭证引用占位。`文档goalP2(2026-06-20)`

### 9.1 监控对象与阈值

| # | 监控对象 | 采集方式（指向既有脚本/口径） | 触发阈值 | 事件名（对应 alert.example） |
|---|---|---|---|---|
| 1 | API/Web 服务存活与端口 | `ops/160/complex-health-check.sh`（actuator/system health、`/docs/openapi` 状态码、Web 200、容器存活），定时执行非 0 退出即告警 | 健康检查脚本退出码 ≠ 0，或连续 2 次探测失败 | `service_unavailable` |
| 2 | PostgreSQL 连接/可达性 | `ops/160/complex-runtime-config-check.sh` 运行配置预检 + `complex-health-check.sh` 的 `complex-postgres=running`；或 `pg_isready`/连接探测 | 容器非 running，或连接探测失败 | `service_unavailable` |
| 3 | 磁盘容量百分比（本地盘与 `/mnt/complex-nas-backup`） | 定时 `df -P` 采集已用百分比，对照 alert.example 的 `CAPACITY_WARNING_PERCENT`/`CAPACITY_CRITICAL_PERCENT` | ≥ 80% 预警（P1），≥ 90% 严重（P0） | `capacity_threshold_exceeded` |
| 4 | NAS 挂载状态 | `ops/160/complex-nas-preflight.sh`（挂载点可达/可写预检），定时执行 | 预检失败（未挂载或不可写） | `service_unavailable`（NAS 维度） |
| 5 | 备份任务成功率 | `ops/160/complex-backup.sh` 退出码 + `ops/160/complex-restore-drill.sh` 恢复演练结果 | 备份脚本非 0 退出，或当日无成功备份产物，或恢复演练失败 | `backup_failed` / `restore_drill_failed` |
| 6 | 关键 verify 失败 | `npm run verify:ops160-recovery`（备份/恢复/凭证/NAS 证据），及发布链路 `complex-verify-p0.sh` | 任一 verify 非 0 退出 | `backup_failed`（备份链）/ `service_unavailable`（发布链），按 9.4 分级 |
| 7 | 证书/密钥到期 | 人工台账 + 到期提醒（见 §8），到期前 N 天触发 | 距到期 ≤ 阈值天数 | `credential_expiring` |

说明：上述脚本均为既有 `ops/160/*.sh`，本节只新增“何时采集、何阈值告警”的运维口径，不修改脚本本身。

### 9.2 通知渠道接通（企微 webhook 或邮件 SMTP）

渠道配置位置统一在 `ops/160/complex-backup-alert.example`（告警策略模板）与 `/etc/complex/complex.env`（运行配置）。真实值由 IT/运维在受控窗口注入，**文档与仓库只保留占位**：

- 企业微信机器人 webhook
  - 凭证引用占位：`WECOM_WEBHOOK_REF="cred://prod/alert/wecom/complex-backup"`（已在 alert.example 中）。
  - 运行环境变量占位（写入 `/etc/complex/complex.env`，不入仓库）：`COMPLEX_ALERT_WECOM_WEBHOOK_REF=cred://prod/alert/wecom/complex-backup`。
  - 发送示例命令（占位，真实 URL 从凭证保管处注入，不写入文档/脚本/日志）：
    ```bash
    # WECOM_WEBHOOK 由 IT 从 cred://prod/alert/wecom/complex-backup 解析后注入到本进程环境，禁止落盘
    curl -fsS -H 'Content-Type: application/json' \
      -d '{"msgtype":"text","text":{"content":"[complex-160][P1] backup_failed: <detail>"}}' \
      "$WECOM_WEBHOOK"
    ```
- 邮件 SMTP（回退渠道，alert.example 中 `FALLBACK_CHANNEL="email"`）
  - 收件人占位：`EMAIL_RECIPIENTS_REF="cred://prod/alert/email/complex-ops"`。
  - 运行环境变量占位：`COMPLEX_ALERT_SMTP_HOST`、`COMPLEX_ALERT_SMTP_PORT`、`COMPLEX_ALERT_SMTP_USER`、`COMPLEX_ALERT_SMTP_PASSWORD_REF=cred://prod/alert/smtp/complex-ops`（密码只放凭证引用，不写明文）。
  - 发送示例命令（占位）：
    ```bash
    # SMTP 密码由 IT 从 cred://prod/alert/smtp/complex-ops 解析后注入，禁止写入文档/脚本/日志
    printf 'Subject: [complex-160][P0] capacity_threshold_exceeded\n\n<detail>\n' \
      | msmtp --host="$COMPLEX_ALERT_SMTP_HOST" --port="$COMPLEX_ALERT_SMTP_PORT" \
              --auth=on --user="$COMPLEX_ALERT_SMTP_USER" --passwordeval='<cred-resolver>' \
              "$(/* recipients from cred://prod/alert/email/complex-ops */)"
    ```

约束：webhook URL、SMTP 密码、token、私钥不得出现在本 Runbook、`*.example`、脚本仓库或日志中；只允许 `cred://...` 引用，真实值由 IT 在运行机注入并落入生产准入证据台账。

### 9.3 接通验证（发测试告警确认渠道可达）

落地后必须验证渠道连通，再认定告警生效：

1. 企微：以测试内容发送一条 `text` 消息（见 9.2 示例命令），确认机器人所在群收到，并核对 HTTP 200 与返回体 `errcode=0`。
2. 邮件：发送一封测试主题邮件至 `cred://prod/alert/email/complex-ops` 解析出的收件组，确认收件箱可达且非垃圾箱拦截。
3. 注入式演练：临时制造可控失败以验证“事件 → 告警”链路，例如把 `complex-health-check.sh` 指向不可达端口、或对 `complex-backup.sh` 设置不可写的 `NAS_BACKUP_TARGET_DIR`，确认对应事件（`service_unavailable`/`backup_failed`）按渠道送达。演练后立即恢复正常配置。
4. 记录验收：把测试时间、渠道、收到截图/回执、责任人写入生产准入证据台账，作为“告警渠道可达”的证据。

### 9.4 告警分级（P0 立即 / P1 工作时间）

| 级别 | 含义 | 触发事件示例 | 响应时效 | 通知渠道 |
|---|---|---|---|---|
| P0 | 生产不可用或数据风险，需立即处置 | `service_unavailable`（API/Web/PostgreSQL/NAS 不可用）、磁盘 ≥ 90% `capacity_threshold_exceeded`、`backup_failed` 连续失败、`restore_drill_failed` | 立即（7×24，分钟级响应） | 企微 + 邮件双发，未确认升级到 `EXPECTED_ACK_OWNER=IT/ops` |
| P1 | 有降级风险但暂不影响在线业务 | 磁盘 80%–90% 容量预警、单次备份失败已自动重试、`credential_expiring`（距到期 > 紧急阈值）、单个非阻断 verify 失败 | 工作时间内处置 | 企微（必要时邮件回退） |

分级口径与 `complex-backup-alert.example` 的 `CAPACITY_WARNING_PERCENT=80`/`CAPACITY_CRITICAL_PERCENT=90`、`EXPECTED_ACK_OWNER=IT/ops`、`FALLBACK_CHANNEL=email` 保持一致；本节只规定“何级走何渠道、谁确认”，不改脚本。

## 10. 当前注意事项

1. 正式实现以 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 为准。
2. 域名和 HTTPS 暂不绑定。
3. 第三方真实接入仍需供应商 SDK/API 到位后实施。
4. 本 Runbook 不直接修改服务器，只定义执行口径。
5. `complex-deploy-api.sh`、`complex-rollback-api.sh`、`complex-health-check.sh`、`complex-verify-be000.sh` 和 `complex-verify-p0.sh` 已切换为正式 Java 后端口径；其中 API 发布和 BE-000 验收统一复用 `verify:backend-env` 与统一 Maven 运行器，不再硬编码只认系统 `mvn`，也不再启动旧 `apps/api`。
6. `complex-rollback-api.sh` 必须先执行 `complex-runtime-config-check.sh` 或等价运行配置预检；回滚不是绕过数据库账号、密码、PostgreSQL 可达性和基础容器状态检查的后门。若预检失败，不得把回滚标记为成功。
7. `complex-verify-p0.sh` 已纳入 OpenAPI 示例闸口；若高风险写接口或外部接入接口缺机器样例，不得进入供应商 mock、接口自动化或现场联调。
8. `complex-verify-p0.sh` 已纳入 OpenAPI 权限/事件/任务追溯附录闸口；若 `p0-openapi-contract-closure-appendix-v0.1.md` 未随 OpenAPI 更新，不得进入 P0 联调和验收。
9. `complex-verify-p0.sh` 已纳入 Android 环境、核心构建、smoke 和 APK 闸口；若 Java 21、Gradle/Wrapper、Android SDK、P0 种子测试管理员账号或 Android smoke 依赖的 P0 业务接口不可用，不得把完整 P0 验收标记为通过。
10. `verify:p0-contract` 可在 160 环境不具备 Java/Android 前先执行，用于确认文档契约、客户端路径和供应商 mock 输入是否已经可联调；它通过不代表 BE-000、Android smoke 或 Android APK 已验收。
11. 当前 160 API/Web/BE-000/完整 P0 自动化 smoke 已通过，但 API 仍处于 dev profile 且使用非 systemd 回退启动；生产或长期预生产前，IT/运维必须完成 `/etc/complex/complex.env` 正式配置、systemd 服务写入、账号密钥轮换和告警接入。
12. 当前用于跑通 Android smoke 的后端业务接口为 `local/dev/test` profile 下的 P0 smoke 切片，数据为进程内存态；正式业务开发不得在该切片上继续堆功能，必须按 OpenAPI、Flyway、权限矩阵和事件矩阵迁移到正式业务包、表和服务。
13. 后续 Java/后端集成测试、`npm run test:backend`、BE-000 和完整 P0 验收以 160 机器或等效 Java 21 环境结果为准；本机缺少 Java 21 时只记录为本机环境前置缺失，不作为代码失败结论。

## 11. 变更日志

| 日期 | 类型 | 内容 | 原因/依据 |
|---|---|---|---|
| 2026-06-12 | 增 | 新增 160 运维 Runbook 的 AI-ready 四件套执行入口，覆盖反模式、测试、错误处理和精确引用 | 防止部署、备份、密钥和告警操作脱离正式工程与运维验收链路 |
| 2026-06-12 | 改 | 160 API 部署、回滚、健康检查、BE-000/P0 验收和备份脚本改为 Java/Spring Boot/PostgreSQL/Flyway 口径 | 防止 160 运维脚本继续运行旧 TypeScript/Fastify/SQLite 原型 |
| 2026-06-12 | 增 | P0 验收脚本纳入 `npm run verify:openapi` 契约闸口 | 防止 OpenAPI 旧路径、缺权限、缺终端/调用通道或缺审计事件回退 |
| 2026-06-13 | 增 | P0 验收脚本纳入 `npm run verify:openapi-examples` 示例闸口 | 防止供应商 mock、接口自动化和 SDK 生成在缺少 request/response 示例时继续推进 |
| 2026-06-13 | 增 | P0 验收脚本纳入 `npm run verify:openapi-closure` 追溯附录闸口 | 防止 OpenAPI 已更新但权限、事件、任务映射和样例准入口径未同步 |
| 2026-06-13 | 改 | API 发布和 BE-000 验收脚本复用 `verify:backend-env` 与统一 Maven 运行器 | 防止 160 脚本硬依赖系统 Maven，降低后续 Maven Wrapper 接入和接手机器迁移成本 |
| 2026-06-13 | 改 | P0 验收脚本复用 `verify:android-env` 与统一 Android Gradle 运行器 | 防止 Android 构建或 APK 因 Gradle/SDK 缺失被静默跳过 |
| 2026-06-13 | 改 | P0 验收脚本复用 `verify:p0-contract` | 防止 OpenAPI、客户端和供应商 mock 契约闸口在本地脚本与 160 脚本之间漂移 |
| 2026-06-13 | 增 | 补充 160 实机更新快照，记录旧项目备份、最新工程同步、脚本同步、契约/环境/Web/后端构建通过和运行配置缺口 | 防止把旧 160 配置继续作为当前依据，也防止把同步完成误判为 API 已生产运行 |
| 2026-06-13 | 增 | 新增 `complex-runtime-config-check.sh` 运行配置预检和 `ops/160/complex.env.example` 模板，并要求 API 发布前强制执行 | 防止依赖默认数据库密码或半配置环境启动 API，降低 160 运行态验收误判 |
| 2026-06-13 | 改 | `complex-rollback-api.sh` 纳入 `complex-runtime-config-check.sh` 预检，Runbook 同步声明回滚不得绕过运行配置检查 | 防止缺数据库账号/密码或基础容器不可用时把回滚误判为运行态通过 |
| 2026-06-13 | 改 | 160 运行配置预检新增 JDBC URL 形态检查，并禁止在 `COMPLEX_DATABASE_URL` 中内嵌数据库账号密码 | 防止当前旧 `postgresql://...` 配置在补齐用户名后继续导致 Spring Boot 数据源启动失败 |
| 2026-06-13 | 改 | 记录 160 用户级运行配置覆盖、API release `20260613-180232`、Web release `20260613-180329`、BE-000 运行态和健康检查通过；当时完整 P0 处于阶段性待收口状态，后续已由 release `20260613-183652` 和完整 `complex-verify-p0.sh` 收口 | 防止把阶段性 BE-000/API/Web 运行态与完整 P0 混淆 |
| 2026-06-13 | 改 | API release `20260613-183652` 已发布；补齐 dev smoke 鉴权、统一响应解包和 P0 smoke 切片后，`complex-verify-p0.sh` 完整通过 | 证明 160 环境、正式路径、Android smoke、APK 和整体健康检查可跑通；同时声明该 smoke 切片不等同于生产级业务实现 |
| 2026-06-13 | 复核 | 重新执行 `complex-runtime-config-check.sh`、`verify:backend-env`、`verify:android-env`、`verify:p0-contract` 和完整 `complex-verify-p0.sh`，均通过；确认当前 160 编码环境为 JDK 21.0.11、Maven 3.9.11、Node 22.22.3、npm 10.9.8、Spring Boot 4.1.0 | 防止 160 机器旧配置被误判为当前环境，也防止只凭历史记录判断可用 |
| 2026-06-13 | 改 | 同步本轮安全基线文档与 OpenAPI 描述后，API release `20260613-193142` 已发布，运行时 `/docs/openapi` 已确认包含 `Java 21 + Spring Boot 4.x`，完整 `complex-verify-p0.sh` 再次通过 | 防止本地文档/代码与 160 运行态漂移 |
| 2026-06-14 | 改 | 明确 NAS 服务器为 `192.168.88.113`；后续 Java/后端集成测试、BE-000 和完整 P0 验收默认在 160 或等效 Java 21 环境执行 | 防止本机缺少 Java 21 时误判后端代码失败，并固定 160 到 NAS 的备份同步目标 |
| 2026-06-14 | 改 | NAS 挂载协议收口为 SMB；真实共享名、账号和挂载目录由总控/IT 提供 | 防止后续实施继续在 NFS/SMB 之间摇摆，也避免把缺少外部 NAS 账号误判为 P0 编码未完成 |
| 2026-06-14 | 改 | NAS 参数收口为 `//192.168.88.113/bn`、账号 `jxh1`、160 挂载 `/mnt/complex-nas-backup`、30 天保留、1TB 容量上限；当前不启用备份归档加密 | 防止脚本默认加密口径与总控选择冲突，并把真实密码继续留在凭证保管流程而非文档 |
| 2026-06-20 | 改 | §9 告警与监控由“建议”细化为可操作配置步骤：监控对象与阈值、企微/邮件渠道接通（占位凭证引用）、接通验证、P0/P1 告警分级；指向既有 `ops/160/*.sh` 与 `complex-backup-alert.example`，不改脚本（`文档goalP2(2026-06-20)`） | 防止告警停留在口号、缺少可执行落地步骤，同时避免把真实 webhook/SMTP 密钥写入文档或仓库 |
