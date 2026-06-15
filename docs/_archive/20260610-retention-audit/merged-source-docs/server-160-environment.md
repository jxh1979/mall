# 160 服务器环境配置记录

更新时间：2026-06-05

## 1. 基本信息

| 项 | 值 |
|---|---|
| SSH 别名 | `codex-160` |
| 地址 | `192.168.88.160` |
| 用户 | `james` |
| 项目目录 | `/home/james/complex-management` |
| 系统 | Ubuntu 24.04.4 LTS |
| 架构 | x86_64 |

## 2. 已安装环境

| 类型 | 版本/路径 |
|---|---|
| Node.js | v22.22.2 |
| npm | 10.9.7 |
| Java | OpenJDK 21.0.11 |
| Maven | 3.9.11 |
| Gradle | 8.14.3 |
| Python | 3.12.3 |
| Docker | 29.5.2 |
| Docker Compose | v5.1.4 |
| Nginx | 1.24.0 |
| Android SDK | `/home/james/android-sdk` |
| Android cmdline-tools | 20.0 |
| Android platform-tools | 37.0.0 |
| Android platform | `platforms;android-36` |
| Android build-tools | `36.1.0` |

Android 环境脚本：

```bash
source /opt/complex/scripts/android-sdk-env.sh
```

## 3. 基础设施服务

| 服务 | 容器名 | 端口 | 状态 |
|---|---|---|---|
| PostgreSQL | `complex-postgres` | `5432` | running |
| Redis | `complex-redis` | `6379` | running |
| NATS | `complex-nats` | `4222`, `8222` | running |
| MinIO | `complex-minio` | `9000`, `9001` | running |

## 4. 当前应用服务

| 服务 | 地址/路径 | 状态 |
|---|---|---|
| API | `http://192.168.88.160:8080` | 运行中 |
| API 文档 | `http://192.168.88.160:8080/docs` | 运行中 |
| Web | `http://192.168.88.160/` | 运行中 |
| Android debug APK | `/home/james/complex-management/apps/android-terminal/android-app/app/build/outputs/apk/debug/app-debug.apk` | 可构建 |

当前 release：

- API：`/opt/complex/api/releases/20260605-063525`
- Web：`/opt/complex/web/releases/20260605-065155`

说明：外部入口统一由 80 端口提供，后续正式发布通过 Nginx 静态服务和反向代理承载。

## 5. 运维脚本

脚本安装位置：`/opt/complex/scripts`

| 脚本 | 用途 | 状态 |
|---|---|---|
| `complex-deploy-api.sh` | 构建并按 release 发布 API | 已验证 |
| `complex-deploy-web.sh` | 构建并按 release 发布 Web | 已验证 |
| `complex-rollback-api.sh` | API release 回滚并重启 | 已验证 |
| `complex-rollback-web.sh` | Web release 回滚并重启 | 已验证 |
| `complex-health-check.sh` | API、Web、容器健康检查 | 已验证 |
| `complex-verify-p0.sh` | P0 一键验收 | 已验证 |
| `complex-backup.sh` | PostgreSQL、对象存储、docs、容器配置和基础设施数据备份 | 已验证 |
| `android-sdk-env.sh` | Android SDK 环境变量 | 已验证 |

## 6. 常用命令

健康检查：

```bash
/opt/complex/scripts/complex-health-check.sh
```

P0 验收：

```bash
cd /home/james/complex-management
SPRING_PROFILES_ACTIVE=prod ./mvnw test
curl -fsS http://127.0.0.1:8080/actuator/health
```

发布：

```bash
/opt/complex/scripts/complex-deploy-api.sh
/opt/complex/scripts/complex-deploy-web.sh
```

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
npm run android:p0:apk
```

## 7. 最终备份

最新 P0 干净备份：

```text
/opt/complex/backups/p0-final-clean/20260605-063819
```

备份包含：

- PostgreSQL dump。
- Docker Compose 配置。
- docs 压缩包。
- Redis、NATS、MinIO 数据。

## 8. 注意事项

- 正式编码与生产实现已拍板使用 Java 21 + Spring Boot + PostgreSQL + Flyway。
- Web 正式生产采用 Nginx 静态服务或统一反向代理。
- 当前开发账号、MinIO、数据库密码均为开发环境密码，生产前必须轮换。
- 第三方真实接入仍在 P1/P2，不应在 P0 环境直接对接真实供应商。
