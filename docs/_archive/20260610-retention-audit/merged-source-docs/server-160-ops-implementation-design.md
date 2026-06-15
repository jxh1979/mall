# 160服务器与基础设施实施级运维设计

本文档面向 `192.168.88.160` 服务器的后续开发、部署和运维落地，目标是把现有环境配置升级为可执行、可回滚、可备份、可监控、可审计的运维方案。

本次只写文档，不修改服务器。

参考文档：

- `docs/system-development-spec.md`
- `docs/project-progress-dashboard.md`
- `docs/server-160-environment.md`
- `docs/server-160-runbook-v0.2.md`

## 1. 当前定位

160服务器当前定位为：

- 开发/预生产一体环境
- Java Spring Boot 后端发布目标
- React 管理后台发布目标
- PostgreSQL、Redis、NATS、MinIO 基础设施承载环境
- 后续半自动部署、备份、恢复、监控演练环境

当前基础：

- 系统：Ubuntu 24.04.4 LTS
- 系统级工具：Java 21、Maven 3.9.11、Gradle 8.14.3、Nginx、UFW、fail2ban
- 容器基础设施：PostgreSQL、Redis、NATS、MinIO
- 标准目录：`/opt/complex`、`/etc/complex`、`/var/log/complex`、`/srv/complex`
- 应用环境文件：`/etc/complex/complex.env`
- 后端 systemd 模板：`complex-api.service`
- 系统检查命令：`complex-check-env`

## 2. 正式目录结构

建议后续以以下结构作为唯一部署标准。

```text
/opt/complex
├── api
│   ├── releases
│   │   └── 20260605-120000
│   │       ├── complex-api.jar
│   │       ├── build-info.json
│   │       └── checksums.sha256
│   ├── current -> releases/20260605-120000
│   └── complex-api.jar -> current/complex-api.jar
├── web
│   ├── releases
│   │   └── 20260605-120000
│   │       ├── dist
│   │       ├── build-info.json
│   │       └── checksums.sha256
│   └── current -> releases/20260605-120000/dist
├── backups
│   ├── postgres
│   ├── docker-data
│   ├── minio
│   ├── config
│   └── before-deploy
├── scripts
│   ├── complex-deploy-api
│   ├── complex-deploy-web
│   ├── complex-rollback-api
│   ├── complex-rollback-web
│   ├── complex-backup-postgres
│   ├── complex-backup-docker-data
│   ├── complex-backup-config
│   ├── complex-restore-postgres
│   └── complex-health-check
└── tmp
```

配置目录：

```text
/etc/complex
├── complex.env
├── backup.env
├── alert.env
└── nginx
    └── complex.conf.template
```

日志目录：

```text
/var/log/complex
├── api.log
├── api-error.log
├── deploy.log
├── backup.log
├── restore.log
├── monitor.log
└── nginx
```

## 3. 部署脚本设计

### 3.1 通用发布原则

所有发布都必须遵守：

1. 发布前自动备份配置和当前版本信息。
2. 新版本进入独立 release 目录。
3. 使用软链接切换版本，不直接覆盖旧版本。
4. 切换后执行健康检查。
5. 健康检查失败自动回滚到上一版本。
6. 发布、验证、回滚结果写入 `/var/log/complex/deploy.log`。
7. 发布脚本不保存明文密码，不把敏感配置写入 release 目录。

### 3.2 后端发布脚本：`complex-deploy-api`

输入：

```bash
complex-deploy-api /path/to/complex-api.jar
```

脚本职责：

| 步骤 | 动作 |
|---|---|
| 1 | 校验 jar 文件存在 |
| 2 | 校验 `/etc/complex/complex.env` 存在 |
| 3 | 获取当前版本软链接，记录为 previous |
| 4 | 创建 release 目录，例如 `/opt/complex/api/releases/20260605-120000` |
| 5 | 复制 jar 到 release 目录 |
| 6 | 生成 `build-info.json`，记录发布时间、操作者、git commit、jar大小 |
| 7 | 生成 `checksums.sha256` |
| 8 | 更新 `/opt/complex/api/current` |
| 9 | 更新 `/opt/complex/api/complex-api.jar` |
| 10 | `sudo systemctl restart complex-api` |
| 11 | 等待服务启动 |
| 12 | 调用健康检查 `/api/health` |
| 13 | 健康检查失败则回滚 previous |
| 14 | 清理过旧 release |

健康检查要求：

```text
GET http://127.0.0.1:8080/api/health
```

预期返回：

```json
{
  "ok": true
}
```

建议伪代码：

```bash
set -euo pipefail
JAR="$1"
RELEASE_ID="$(date +%Y%m%d-%H%M%S)"
RELEASE_DIR="/opt/complex/api/releases/$RELEASE_ID"
PREVIOUS="$(readlink -f /opt/complex/api/current || true)"

mkdir -p "$RELEASE_DIR"
cp "$JAR" "$RELEASE_DIR/complex-api.jar"
sha256sum "$RELEASE_DIR/complex-api.jar" > "$RELEASE_DIR/checksums.sha256"

cat > "$RELEASE_DIR/build-info.json" <<EOF
{
  "releaseId": "$RELEASE_ID",
  "releasedAt": "$(date -Iseconds)",
  "releasedBy": "$(whoami)",
  "artifact": "complex-api.jar"
}
EOF

ln -sfn "$RELEASE_DIR" /opt/complex/api/current
ln -sfn /opt/complex/api/current/complex-api.jar /opt/complex/api/complex-api.jar

sudo systemctl restart complex-api

if ! /opt/complex/scripts/complex-health-check api; then
  if [ -n "$PREVIOUS" ]; then
    ln -sfn "$PREVIOUS" /opt/complex/api/current
    ln -sfn /opt/complex/api/current/complex-api.jar /opt/complex/api/complex-api.jar
    sudo systemctl restart complex-api
  fi
  exit 1
fi
```

### 3.3 前端发布脚本：`complex-deploy-web`

输入：

```bash
complex-deploy-web /path/to/dist
```

脚本职责：

| 步骤 | 动作 |
|---|---|
| 1 | 校验 dist 目录存在 |
| 2 | 校验 dist 中存在 `index.html` |
| 3 | 创建 release 目录 |
| 4 | 复制 dist 到 release |
| 5 | 生成 `build-info.json` 和 `checksums.sha256` |
| 6 | 更新 `/opt/complex/web/current` |
| 7 | 执行 `sudo nginx -t` |
| 8 | 执行 `sudo systemctl reload nginx` |
| 9 | 验证首页可访问 |
| 10 | 失败则回滚 previous |

健康检查：

```text
GET http://127.0.0.1/
```

要求：

- HTTP 状态为 200
- 响应内容包含前端应用入口
- 静态资源返回 200 或 304

### 3.4 发布前备份：`complex-backup-before-deploy`

每次部署前自动执行：

```bash
complex-backup-config before-deploy
complex-backup-postgres before-deploy
```

部署前备份目录：

```text
/opt/complex/backups/before-deploy/20260605-120000
├── etc-complex.tar.gz
├── nginx.tar.gz
├── docker-compose.tar.gz
└── postgres.dump
```

## 4. PostgreSQL 自动备份设计

### 4.1 备份目标

PostgreSQL 当前运行在 Docker 容器：

- 容器名：`complex-postgres`
- 数据库：`complex`
- 用户：`complex`
- 端口：`5432`

### 4.2 备份方式

采用 `pg_dump` 逻辑备份。

推荐命令：

```bash
docker exec complex-postgres pg_dump \
  -U complex \
  -d complex \
  --format=custom \
  --file=/tmp/complex.dump

docker cp complex-postgres:/tmp/complex.dump \
  /opt/complex/backups/postgres/complex-$(date +%Y%m%d-%H%M%S).dump
```

备份后生成校验：

```bash
sha256sum complex-YYYYMMDD-HHMMSS.dump > complex-YYYYMMDD-HHMMSS.dump.sha256
```

### 4.3 自动计划

建议使用 systemd timer，而不是 cron。

服务：

```text
/etc/systemd/system/complex-backup-postgres.service
```

定时器：

```text
/etc/systemd/system/complex-backup-postgres.timer
```

建议时间：

```text
每天 03:10
```

### 4.4 保留策略

开发/预生产阶段：

| 类型 | 保留 |
|---|---|
| 每日备份 | 14天 |
| 部署前备份 | 30天 |
| 手动备份 | 不自动删除 |

正式生产阶段：

| 类型 | 保留 |
|---|---|
| 每日备份 | 30天 |
| 每周备份 | 12周 |
| 每月备份 | 12个月 |
| 异地备份 | 至少1份 |

### 4.5 恢复流程

恢复必须先停写：

1. 停止 `complex-api.service`。
2. 记录当前数据库状态并先做一次临时备份。
3. 创建临时恢复库 `complex_restore_test`。
4. 在临时库恢复并校验。
5. 校验通过后再恢复正式库。
6. 恢复完成后启动 `complex-api.service`。
7. 写入恢复日志。

恢复命令示例：

```bash
createdb -h 127.0.0.1 -U complex complex_restore_test
pg_restore -h 127.0.0.1 -U complex -d complex_restore_test backup.dump
```

正式库恢复前必须经总控确认。

## 5. MinIO 与 Docker 数据归档

### 5.1 Docker 数据目录

当前 Docker Compose 数据目录：

```text
~/complex-env/data
├── postgres
├── redis
├── nats
└── minio
```

### 5.2 归档对象

| 对象 | 方式 | 频率 |
|---|---|---|
| MinIO数据 | 文件归档 | 每日 |
| Redis AOF/RDB | 文件归档 | 每日 |
| NATS JetStream数据 | 文件归档 | 每日 |
| Docker Compose配置 | 文件归档 | 每次变更前 |
| PostgreSQL物理目录 | 不作为主备份 | 仅灾难归档 |

PostgreSQL 主备份以 `pg_dump` 为准，不建议依赖直接打包数据目录作为常规恢复手段。

### 5.3 归档方式

建议使用 `tar + zstd`，如果未安装 zstd，可先用 gzip。

```bash
tar -czf /opt/complex/backups/docker-data/docker-data-$(date +%Y%m%d-%H%M%S).tar.gz \
  -C /home/james/complex-env data infra
```

MinIO 单独归档：

```bash
tar -czf /opt/complex/backups/minio/minio-$(date +%Y%m%d-%H%M%S).tar.gz \
  -C /home/james/complex-env/data minio
```

### 5.4 归档注意事项

- Redis、NATS、MinIO 可做文件归档，但恢复前必须先停止对应容器。
- PostgreSQL 不建议在线直接打包物理目录作为主恢复方案。
- MinIO 未来正式环境建议使用 `mc mirror` 做对象级同步，而不是只靠目录打包。
- 归档文件必须生成 sha256 校验。
- 归档文件 P0 同步到 NAS；后续可再叠加第二台机器或云对象存储。

## 6. Nginx 正式结构

### 6.1 目标结构

正式结构建议：

- Nginx 直接服务 React 静态文件。
- `/api/` 反向代理到 Java 后端 `127.0.0.1:8080`。
- `/docs` 反向代理到 Java OpenAPI 文档。
- `/uploads/` 或 `/files/` 不直接暴露 MinIO，必须通过后端签名或授权访问。

### 6.2 正式配置结构

```text
/etc/nginx/sites-available/complex
/etc/nginx/sites-enabled/complex -> ../sites-available/complex
/opt/complex/web/current
```

建议 Nginx 配置：

```nginx
server {
    listen 80;
    server_name _;

    root /opt/complex/web/current;
    index index.html;

    client_max_body_size 50m;

    access_log /var/log/complex/nginx/access.log;
    error_log /var/log/complex/nginx/error.log;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /docs {
        proxy_pass http://127.0.0.1:8080/docs;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }
}
```

### 6.3 HTTPS 决策

当前 160 是局域网/受控访问环境，P0 已确认先不绑定域名和 HTTPS，HTTP 可用于开发、预生产和演示。

正式生产或外部第三方联调前必须重新决策：

- 是否绑定域名
- 是否使用公网证书
- 是否使用内网自签证书
- 是否由上层网关统一 TLS 终止

正式生产要求：

- 后台管理端强制 HTTPS
- 第三方开放平台强制 HTTPS
- 动态鉴权接口强制 HTTPS
- 下载、导出、证据访问强制 HTTPS

## 7. `complex-api.service` 发布验证

### 7.1 systemd 服务要求

服务文件：

```text
/etc/systemd/system/complex-api.service
```

关键要求：

- 使用 `james` 用户运行，不使用 root。
- 环境变量从 `/etc/complex/complex.env` 读取。
- 日志写入 `/var/log/complex/api.log` 和 `/var/log/complex/api-error.log`。
- 设置自动重启。
- 启动后必须通过健康检查。

### 7.2 验证清单

发布后执行：

```bash
systemctl is-active complex-api
curl -fsS http://127.0.0.1:8080/api/health
curl -fsS http://127.0.0.1:8080/docs
tail -n 100 /var/log/complex/api-error.log
```

验证项：

| 项 | 预期 |
|---|---|
| systemd状态 | active |
| 健康检查 | HTTP 200 |
| 数据库连接 | 正常 |
| Redis连接 | 正常 |
| NATS连接 | 正常 |
| MinIO连接 | 正常 |
| 错误日志 | 无启动异常 |
| Nginx `/api/health` | HTTP 200 |

### 7.3 启动失败处理

如果启动失败：

1. 查看 `journalctl -u complex-api -n 200`。
2. 查看 `/var/log/complex/api-error.log`。
3. 检查 `/etc/complex/complex.env`。
4. 检查 PostgreSQL、Redis、NATS、MinIO。
5. 回滚到上一 jar。

## 8. 日志策略

### 8.1 日志类型

| 日志 | 路径 | 内容 |
|---|---|---|
| API标准日志 | `/var/log/complex/api.log` | 启动、请求摘要、业务日志 |
| API错误日志 | `/var/log/complex/api-error.log` | 异常、启动失败、依赖失败 |
| 部署日志 | `/var/log/complex/deploy.log` | 发布、验证、回滚 |
| 备份日志 | `/var/log/complex/backup.log` | 备份开始、结束、文件、校验 |
| 恢复日志 | `/var/log/complex/restore.log` | 恢复操作记录 |
| 监控日志 | `/var/log/complex/monitor.log` | 健康检查结果 |
| Nginx日志 | `/var/log/complex/nginx/` | 访问日志、错误日志 |

### 8.2 日志轮转

建议新增：

```text
/etc/logrotate.d/complex
```

规则：

```text
/var/log/complex/*.log /var/log/complex/nginx/*.log {
    daily
    rotate 14
    compress
    missingok
    notifempty
    copytruncate
}
```

生产阶段可改为：

- 普通日志保留30天
- 安全审计日志保留180天
- 事件中心数据以数据库为准，不依赖日志文件保留

### 8.3 日志安全

- 不记录明文密码、Token、动态鉴权密钥。
- 手机号、证件号、银行卡号脱敏。
- 第三方调用日志只保留参数摘要和 requestId。
- 高风险操作以事件中心为准，日志只做辅助定位。

## 9. 监控告警设计

### 9.1 P0 监控项

| 监控项 | 方法 | 告警条件 |
|---|---|---|
| CPU | `top`/node exporter | 连续5分钟超过85% |
| 内存 | `free`/node exporter | 可用内存低于15% |
| 磁盘 | `df -h` | 使用率超过80%提醒，90%高危 |
| Docker容器 | `docker ps` | 关键容器退出 |
| PostgreSQL | `pg_isready` | 不可用 |
| Redis | `redis-cli ping` | 非PONG |
| NATS | `http://127.0.0.1:8222/healthz` | 非OK |
| MinIO | `/minio/health/live` | 非OK |
| Nginx | `systemctl is-active nginx` | 非active |
| complex-api | `systemctl is-active complex-api` | 非active |
| API健康 | `/api/health` | 非200 |
| 备份结果 | 备份文件和sha256 | 当日无备份或校验失败 |

### 9.2 第一阶段监控脚本

脚本：

```text
/opt/complex/scripts/complex-health-check
```

支持：

```bash
complex-health-check all
complex-health-check api
complex-health-check infra
complex-health-check backup
```

输出要求：

- 人类可读
- 退出码表达状态
- `0` 正常
- `1` 异常

### 9.3 告警渠道

第一阶段可选：

| 渠道 | 说明 |
|---|---|
| 企业微信群机器人 | 推荐 |
| 邮件 | 可选 |
| n8n Webhook | 160上已有 n8n，可考虑转发 |
| 短信 | 生产阶段再接 |

告警配置：

```text
/etc/complex/alert.env
```

字段：

```bash
ALERT_WEBHOOK_URL=
ALERT_ENABLED=true
ALERT_ENV=dev
```

### 9.4 生产监控升级

生产阶段建议引入：

- Prometheus
- node_exporter
- cAdvisor
- postgres_exporter
- redis_exporter
- Nginx exporter
- Grafana
- Loki 或 OpenSearch 日志系统

160 阶段先不强制上完整监控栈，避免运维复杂度过早上升。

## 10. 回滚流程

### 10.1 后端回滚：`complex-rollback-api`

输入：

```bash
complex-rollback-api
complex-rollback-api 20260605-120000
```

无参数时默认回滚到上一版本。

流程：

1. 列出当前版本和候选版本。
2. 切换 `current` 软链接。
3. 重启 `complex-api.service`。
4. 调用健康检查。
5. 失败则切回原版本。
6. 写入回滚日志。

### 10.2 前端回滚：`complex-rollback-web`

流程：

1. 切换 `/opt/complex/web/current`。
2. 执行 `sudo nginx -t`。
3. reload Nginx。
4. 验证首页。
5. 失败则切回原版本。

### 10.3 数据库回滚原则

数据库不能简单回滚。

原则：

- Flyway 迁移必须先评估是否可逆。
- P0 阶段尽量避免破坏性迁移。
- 重大迁移前必须做 `before-deploy` 数据库备份。
- 如果迁移失败，优先恢复备份到临时库验证。
- 正式库恢复必须由总控确认。

### 10.4 回滚触发条件

| 条件 | 动作 |
|---|---|
| 后端启动失败 | 自动回滚 |
| `/api/health`失败 | 自动回滚 |
| Nginx配置失败 | 不切换或回滚 |
| 前端首页不可访问 | 自动回滚 |
| 数据库迁移失败 | 停止发布，等待人工确认 |
| 业务严重异常 | 人工触发回滚 |

## 11. 安全加固检查清单

### 11.1 账号和SSH

| 检查项 | 当前建议 |
|---|---|
| 禁止root SSH登录 | 必须 |
| SSH只允许密钥登录 | 必须 |
| SSH限制来源IP或VPN | 生产必须 |
| sudo最小权限 | 必须 |
| 临时sudo授权用完删除 | 必须 |
| 运维人员独立账号 | 生产必须 |

### 11.2 防火墙

当前建议：

| 端口 | 策略 |
|---|---|
| 22 | 管理IP或VPN |
| 80 | 开放 |
| 443 | 开放 |
| 5432 | 仅内网或本机 |
| 6379 | 仅内网或本机 |
| 4222 | 仅内网或本机 |
| 8222 | 仅内网或本机 |
| 9000/9001 | 仅内网或本机 |

生产建议：

- 数据库、Redis、NATS、MinIO 不直接暴露公网。
- 如无跨机器访问需求，全部绑定 `127.0.0.1`。
- 外部访问统一从 Nginx 或 API 后端进入。

### 11.3 密码和密钥

必须更换：

- PostgreSQL 密码
- MinIO root 密码
- 第三方开放平台密钥
- JWT/Token签名密钥
- 管理后台初始管理员密码

要求：

- 不进入 Git。
- 不写入 release 目录。
- 不在日志中输出。
- 按“人工台账 + 到期提醒 + Runbook”轮换；P0 不做自动轮换。

P0 密钥轮换台账至少记录：账号/密钥名称、用途、负责人、创建时间、到期时间、轮换步骤、回滚步骤、验证步骤、最近轮换时间和下一次提醒时间。数据库、MinIO、JWT/Token、开放平台、企业微信、第三方渠道和支付通道均必须入台账。

### 11.4 Nginx与HTTPS

P0 160 暂不绑定域名和 HTTPS；Nginx 先完成静态前端、API 反向代理、上传大小限制和安全响应头。正式生产或外部第三方联调前必须：

- 启用 HTTPS。
- HTTP 自动跳转 HTTPS。
- 设置安全响应头。
- 限制上传大小。
- 对管理后台启用更严格访问策略。

建议响应头：

```nginx
add_header X-Content-Type-Options "nosniff";
add_header X-Frame-Options "SAMEORIGIN";
add_header Referrer-Policy "strict-origin-when-cross-origin";
```

### 11.5 应用安全

| 检查项 | 要求 |
|---|---|
| 管理后台登录 | 强密码，后续接MFA |
| 第三方API | 动态Token、签名、nonce、防重放 |
| 高风险操作 | 写事件中心 |
| 敏感数据 | 脱敏展示 |
| 文件访问 | 授权后签名URL |
| 导出功能 | 权限控制和审计 |

### 11.6 备份安全

- 备份文件权限限制为运维用户可读。
- 生产备份必须加密。
- 备份恢复每月至少演练一次。
- 异地备份必须验证可下载、可校验。

## 12. P0 实施任务清单

### 12.1 先写脚本

| 任务 | 产物 |
|---|---|
| 后端发布脚本 | `/opt/complex/scripts/complex-deploy-api` |
| 前端发布脚本 | `/opt/complex/scripts/complex-deploy-web` |
| 后端回滚脚本 | `/opt/complex/scripts/complex-rollback-api` |
| 前端回滚脚本 | `/opt/complex/scripts/complex-rollback-web` |
| PostgreSQL备份脚本 | `/opt/complex/scripts/complex-backup-postgres` |
| Docker数据归档脚本 | `/opt/complex/scripts/complex-backup-docker-data` |
| 配置备份脚本 | `/opt/complex/scripts/complex-backup-config` |
| 健康检查脚本 | `/opt/complex/scripts/complex-health-check` |

### 12.2 再配置自动任务

| 任务 | 方式 |
|---|---|
| PostgreSQL每日备份 | systemd timer |
| Docker数据每日归档 | systemd timer |
| 配置变更前备份 | 部署脚本触发 |
| 健康检查 | systemd timer 或 cron |
| 日志轮转 | logrotate |

### 12.3 最后做演练

| 演练 | 验收 |
|---|---|
| 后端发布演练 | 服务可启动，健康检查通过 |
| 后端失败回滚演练 | 错误jar能自动回滚 |
| 前端发布演练 | 页面可访问，静态资源正常 |
| 前端回滚演练 | 可切换上一版本 |
| PostgreSQL备份演练 | 备份文件生成并校验 |
| PostgreSQL恢复演练 | 可恢复到临时库 |
| MinIO归档演练 | 归档生成并校验 |
| 监控告警演练 | 模拟服务停止能触发告警 |

## 13. 已确认执行口径与后续实施事项

以下事项已按总控口径收口，后续按实施条件落服务器：

1. ~~160 是否继续作为预生产环境，还是后续也承担正式生产？~~ 已确认：160 作为 P0 预生产/演示环境；正式生产另行规划。
2. ~~PostgreSQL、Redis、NATS、MinIO 是否继续使用 Docker Compose，还是改成独立服务器/云服务？~~ 已确认：P0/预生产继续 Docker Compose；正式生产前再评估独立服务或云服务。
3. ~~数据库、MinIO、JWT、开放平台密钥是否现在就更换为正式强密码？~~ 已确认：接真实数据前必须更换强密码并进入“人工台账 + 到期提醒 + Runbook”；演示环境也建议使用强密码。
4. ~~Nginx 是否现在切换为静态前端正式结构，而不是代理 `127.0.0.1:3000`？~~ 已确认：P0 采用静态前端 + API 反向代理结构。
5. ~~是否需要为 160 绑定域名并配置 HTTPS？~~ 已确认：P0 先不绑定域名和 HTTPS；160 以预生产/演示内网或受控访问为主，正式生产或外部第三方联调前再配置域名、HTTPS 和证书自动续期。
6. ~~告警渠道优先使用企业微信、邮件、n8n，还是其他方式？~~ 已确认：企业微信机器人 + 邮件兜底，n8n 作为 P1 自动化增强。
7. ~~备份是否需要同步到第二台机器、NAS或云对象存储？~~ 已确认：P0 同步到 NAS；后续可叠加第二台机器或云对象存储。
8. ~~160 脚本写入授权。~~ 已确认：允许写入 160 脚本，但必须先 dry-run、备份、Runbook、可回滚。
9. **Android APK 构建口径。** 已确认：P0 不要求在 160 上构建 Android APK；Android 先在开发/CI 环境构建，160 只保存发布包、版本记录和下载/分发入口。若后续要求 160 构建，再安装 Android SDK 并处理许可证。

## 14. 验收标准

本文档进入实施后，160 运维能力的验收标准：

1. `complex-deploy-api` 可发布 jar。
2. `complex-deploy-web` 可发布前端 dist。
3. `complex-rollback-api` 可回滚后端。
4. `complex-rollback-web` 可回滚前端。
5. PostgreSQL 每日自动备份成功。
6. 备份文件有 sha256 校验。
7. PostgreSQL 可恢复到临时库。
8. MinIO 和 Docker 数据可归档。
9. Nginx 使用静态前端 + API反向代理结构。
10. `complex-api.service` 可启动、可重启、可查看日志。
11. 日志自动轮转。
12. 健康检查脚本可识别基础服务异常。
13. 关键服务异常可触发告警。
14. UFW、fail2ban、SSH、密钥和端口策略符合安全清单。
15. 数据库、对象存储、配置归档能同步到 NAS，并能完成恢复演练。
16. 生产账号、密钥、数据库和对象存储密码有人工台账、到期提醒和 Runbook。

## 15. 变更日志

| 日期 | 类型 | 位置 | 内容 | 原因/依据 |
|---|---|---|---|---|
| 2026-06-10 总控拍板 | 改 | §5.4/§11.3/§13/§14 | 备份目标明确为 NAS；账号、密钥、数据库和对象存储密码轮换采用人工台账 + 到期提醒 + Runbook | 总控最新运维拍板 |
