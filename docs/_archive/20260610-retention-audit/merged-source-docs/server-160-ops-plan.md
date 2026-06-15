# 160服务器运维专项方案

本文件负责 192.168.88.160 的部署、备份、恢复、监控和安全加固。

## 1. 当前运维目标

160 服务器当前定位为：

- 开发/预生产一体环境
- Java 后端部署目标
- Web 管理后台部署目标
- PostgreSQL、Redis、NATS、MinIO 基础设施承载环境
- 后续 CI/CD 或半自动部署目标

当前已完成：

- 系统级 Java 21、Maven、Gradle、Nginx、UFW、fail2ban
- Docker Compose 运行 PostgreSQL、Redis、NATS、MinIO
- `/opt/complex`、`/etc/complex`、`/var/log/complex` 等目录
- `complex-api.service` systemd 模板
- `complex-check-env` 系统检查命令

## 2. 部署流水线设计

### 2.1 目录约定

```text
/opt/complex
├── api
│   ├── releases
│   │   └── 20260605-120000
│   │       └── complex-api.jar
│   ├── current -> releases/20260605-120000
│   └── complex-api.jar -> current/complex-api.jar
├── web
│   ├── releases
│   │   └── 20260605-120000
│   │       └── dist
│   └── current -> releases/20260605-120000/dist
└── backups
```

### 2.2 后端部署流程

输入：一个已经构建好的 Java 后端 jar。

流程：

1. 检查 jar 是否存在。
2. 创建 release 目录。
3. 复制 jar 到 release。
4. 更新 `/opt/complex/api/current` 软链接。
5. 更新 `/opt/complex/api/complex-api.jar` 软链接。
6. 提示重启 `complex-api.service`。
7. 重启后检查 `/api/health`。

注意：重启 systemd 服务需要 sudo。

### 2.3 前端部署流程

输入：前端构建产物目录，例如 `dist`。

流程：

1. 检查 dist 目录是否存在。
2. 创建 release 目录。
3. 复制静态文件。
4. 更新 `/opt/complex/web/current` 软链接。
5. Nginx 指向 `/opt/complex/web/current`。
6. reload Nginx。

注意：修改 Nginx 配置和 reload 需要 sudo。

### 2.4 回滚流程

后端：

1. 查看 `/opt/complex/api/releases`。
2. 将 `current` 指回上一版本。
3. 重启 `complex-api.service`。

前端：

1. 查看 `/opt/complex/web/releases`。
2. 将 `current` 指回上一版本。
3. reload Nginx。

## 3. 备份策略

### 3.1 备份对象

| 对象 | 方式 | 频率 |
|---|---|---|
| PostgreSQL | `pg_dump` | 每日 |
| MinIO 数据 | 文件归档 | 每日 |
| NATS 数据 | 文件归档 | 每日 |
| Redis 数据 | RDB/AOF 文件归档 | 每日 |
| `/etc/complex` | 文件归档 | 每次部署前 |
| Docker Compose 配置 | 文件归档 | 每次部署前 |
| Nginx 配置 | 文件归档 | 每次变更前 |

### 3.2 备份目录

```text
/opt/complex/backups
├── daily
├── before-deploy
└── manual
```

### 3.3 保留策略

开发/预生产阶段建议：

- 每日备份保留 14 天
- 部署前备份保留 30 天
- 手动备份不自动删除

正式生产阶段建议：

- 每日备份保留 30 天
- 每周备份保留 12 周
- 每月备份保留 12 个月
- 异地备份至少一份

### 3.4 恢复原则

恢复必须遵守：

1. 先停止相关应用写入。
2. 先备份当前状态。
3. 恢复到临时库验证。
4. 验证无误后再切正式库。
5. 恢复操作必须记录时间、操作者、原因、备份文件。

## 4. 生产安全加固清单

### 4.1 账号与登录

- 禁止 root SSH 登录。
- SSH 只允许密钥登录。
- 保留 fail2ban。
- 运维用户按人创建，不多人共用一个账号。
- sudo 权限最小化，临时授权用完即删。

### 4.2 网络与端口

- 对公网只开放 80、443。
- SSH 限制为固定管理IP或VPN。
- PostgreSQL、Redis、NATS、MinIO 不直接暴露公网。
- 容器端口正式环境建议绑定 `127.0.0.1` 或内网地址。
- Nginx 统一作为外部入口。

### 4.3 密码与密钥

- 替换所有开发密码。
- 数据库密码使用强密码。
- MinIO root 密码使用强密码。
- 第三方接口密钥定期轮换。
- `.env` 和密钥文件不进入 Git。

### 4.4 数据安全

- PostgreSQL 每日备份。
- MinIO 每日备份。
- 备份文件加密。
- 备份恢复每月至少演练一次。
- 敏感日志脱敏。

### 4.5 应用安全

- 160 P0 先不绑定域名和 HTTPS；正式生产或外部第三方联调前必须启用 HTTPS。
- API 鉴权全量开启。
- 第三方调用必须使用动态令牌和签名。
- 后台管理端启用强密码策略。
- 高危操作写入事件中心。

### 4.6 监控告警

第一阶段建议先监控：

- CPU、内存、磁盘
- Docker 容器状态
- PostgreSQL 可用性
- Redis 可用性
- NATS 可用性
- MinIO 可用性
- Nginx 5xx 错误
- `complex-api.service` 状态

## 5. 第一阶段脚本

建议在 160 上提供以下用户级脚本：

| 脚本 | 用途 |
|---|---|
| `complex-deploy-api` | 部署后端 jar |
| `complex-deploy-web` | 部署前端 dist |
| `complex-backup` | 执行备份 |
| `complex-rollback-api` | 回滚后端 |
| `complex-rollback-web` | 回滚前端 |
| `complex-check-env` | 检查环境 |

## 6. 下一步

建议下一步继续：

1. 把部署脚本同步到 160。
2. 配置 PostgreSQL 自动备份。
3. 配置 MinIO 和 Docker 数据归档。
4. 把 Nginx 改成静态前端 + API 反向代理的正式结构。
5. 等 Java 后端代码出现后，完成 `complex-api.service` 启动验证。

实施级设计补充见：`docs/server-160-ops-implementation-design.md`。
