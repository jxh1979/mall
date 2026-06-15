# 历史安全扫描说明

本目录下现有安全扫描、威胁模型和报告均基于历史 TypeScript / Fastify / Prisma / SQLite 预研原型生成，只能作为旧问题追溯和安全审计方法参考。

当前正式安全基线必须以以下口径重新建立：

| 项 | 当前正式口径 |
|---|---|
| 后端 | Java 21 + Spring Boot 4.x |
| 数据库 | PostgreSQL |
| 迁移 | Flyway |
| 接口契约 | `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json` |
| 权限与终端 | `docs/p0-operation-permission-matrix-v0.1.md`、`docs/p0-openapi-operation-acceptance-matrix-v0.1.md` |

禁止事项：

1. 不得把本目录旧报告中的 Node/Fastify/SQLite 风险结论当作当前正式系统安全结论。
2. 不得以旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 路径作为正式安全测试范围。
3. 不得用旧 Prisma/SQLite 数据模型推导正式 PostgreSQL/Flyway 安全边界。

`security-scans/threat_model.md` 已替换为正式安全基线重建入口。正式 Java 21 + Spring Boot 4.x 后端、运行时 OpenAPI、Flyway 迁移和部署拓扑落地后，必须重新生成安全威胁模型、接口鉴权审计、依赖扫描和部署安全检查。
