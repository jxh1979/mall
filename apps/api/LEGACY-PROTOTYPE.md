# Legacy Prototype Notice

本目录是历史 TypeScript / Fastify / Prisma / SQLite 预研原型，只能作为流程和字段草稿参考。`package.json` 中的运行、构建、Prisma、seed 和 smoke 脚本均已改为阻断提示；本目录已从根 `package.json` 的 npm workspace 中摘除；不得再通过本目录启动旧后端。

正式 P0 后端基线已经收口为：

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Maven
- springdoc-openapi

强制规则：

1. 不得把 `apps/api` 作为正式后端启动、部署、联调或安全审计入口。
2. 不得从 `dev.db`、`prisma/schema.prisma` 或旧 TypeScript 路由直接生成生产数据库、Flyway migration 或正式 OpenAPI。
3. 如需复用旧原型中的业务想法，必须先回写到当前有效 PRD、SRS、数据库设计、权限矩阵、OpenAPI 和验收标准。
4. P0 编码必须以 `docs/openapi/openapi-p0.yaml`、`docs/openapi/openapi-p0.json`、`docs/p0-api-contract-pack-v0.1.md`、`docs/p0-operation-permission-matrix-v0.1.md` 为准。
5. 正式后端入口为 `backend/`；如发现根脚本或子脚本重新指向本目录，应视为 P0 入口回退缺陷并立即修正。
6. 根 `package-lock.json` 不得重新出现 `apps/api`、Fastify、Prisma 或 SQLite 原型依赖。
