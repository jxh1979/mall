# 综合体管理系统正式后端

本目录是正式后端工程入口，技术栈固定为 Java 21 + Spring Boot 4.x + Maven + PostgreSQL + Flyway。

## 本地启动

1. 安装 Java 21；推荐在项目根目录加入 Maven Wrapper（`mvnw.cmd`、`mvnw`、`.mvn/`），否则本机必须安装 Maven 3.9+。
2. 准备 PostgreSQL 数据库。
3. 配置环境变量：

```bash
COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex
COMPLEX_DATABASE_USERNAME=complex
COMPLEX_DATABASE_PASSWORD=complex_dev_pwd
COMPLEX_API_PORT=8080
COMPLEX_API_HOST=127.0.0.1
```

4. 在仓库根目录先执行后端环境前置校验：

```bash
npm run verify:backend-env
```

5. 执行：

```bash
npm run test:backend
npm run dev:backend
```

`test:backend` 与 `dev:backend` 会优先使用 Maven Wrapper；若项目根目录没有 Wrapper，则使用系统 Maven。

也可以在 `backend/` 目录直接执行：

```bash
mvn test
mvn spring-boot:run
```

## 160 后端测试口径

Java/后端集成测试、BE-000 和完整 P0 验收默认在 `192.168.88.160` 执行。本机 Codex 终端如果缺少 Java 21，只记录为本机环境前置缺失，不得判定为后端代码失败。

```bash
ssh codex-160 "cd /home/james/complex-management && npm run test:backend"
ssh codex-160 "cd /home/james/complex-management && /opt/complex/scripts/complex-verify-be000.sh"
ssh codex-160 "cd /home/james/complex-management && /opt/complex/scripts/complex-verify-p0.sh"
```

## 工程可移植性建议

P0 当前以 Maven 为唯一后端构建工具，允许使用系统 Maven 3.9+ 完成 BE-000 和 160 验收。为降低后续换机、CI 和供应商交接成本，建议在 P1 工程化增强中补齐 Maven Wrapper（`mvnw`、`mvnw.cmd`、`.mvn/`）；Android 同理补齐 Gradle Wrapper。Wrapper 未补齐前，`npm run verify:backend-env` 和 `npm run verify:android-env` 必须明确检查系统 Maven/Gradle，不得静默跳过。

## 基础接口

- `GET /api/v1/system/health`
- `GET /api/v1/system/version`
- `GET /actuator/health`
- `GET /docs/openapi`
- `GET /docs/swagger-ui`

## 开发边界

- `apps/api` 下的 TypeScript/Fastify/SQLite 内容不得作为正式后端入口。
- 根目录 `dev`、`dev:api`、`smoke:p0` 已改为指向本 Java 后端；若脚本重新指向 `apps/api`，视为入口回退缺陷。
- 业务域 Controller、Service、Repository 必须在 BE-000 启动和 Flyway baseline 可执行后再进入正式实现。
- 数据库结构必须通过 `src/main/resources/db/migration` 下的 Flyway 迁移文件管理，禁止依赖 Hibernate 自动建表。

## P0 smoke 切片边界

当前 `com.complex.platform.smoke` 与 `com.complex.platform.auth` 下的 P0 smoke 能力只在 `local/dev/test` profile 生效，用于 160 自动化验收、Android smoke、统一响应、Bearer token、设备绑定、权益预检/核销、离线任务同步和事件上报的最小闭环保活。

该切片使用进程内存数据，不是生产级业务实现。正式开发必须按 OpenAPI、Flyway、权限矩阵和事件矩阵，把平台、员工、票务、权益、核销、终端和证据接口迁移到正式业务包、PostgreSQL 表、Service/Repository 和审计事件；迁移过程中必须保持 `complex-verify-p0.sh` 继续通过。
