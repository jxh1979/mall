# 闭环实测验收审计 2026-06-18

> 审计性质：对抗式**实测**审计——不引用历史快照结论，所有"通过"均为本次在 `/home/james/complex-management` 当前工作树实际执行命令的输出。
> 执行环境：Node v22.22.3 / Java 21.0.11 (Temurin) / Maven 3.9.11 / Playwright 1.61.0 / Android SDK present / PostgreSQL 16.14 (本机 5432 可连接)。

## 1. 结论（Go / No-Go）

| 判断项 | 结论 | 依据 |
|---|---|---|
| 代码可稳定构建 | 是 | 后端 `BUILD SUCCESS`；web `build`/`typecheck` 通过 |
| 自动化测试全绿 | 是 | 后端 673 测试 0 失败 0 错误（含 A1 新增 ApiRequestsTests 3 条）；web 单测通过；Playwright 28 + 真后端 2 全通过 |
| 可联调 / 可业务验收 | 是 | 真后端 seeded E2E 端到端登录/仪表盘/核心业务页全部通过 |
| 聚合交付闸口通过 | 是 | `verify:delivery-gates` 退出码 0，无 fail/error |
| 可宣称生产上线就绪 | **否** | 12 项生产前置 + 15 份生产证据待 IT/运维/安全/Owner 线下回填（NAS 自动挂载/备份/告警/演练、生产凭证轮换），属外部阻断，非代码缺陷 |

## 2. 实测命令与证据

| 闸口 | 命令 | 结果 |
|---|---|---|
| 前端类型检查 | `npm run typecheck` | exit 0 |
| 前端单测 | `npm run test --workspace apps/web` | exit 0 |
| OpenAPI 契约 | `npm run verify:openapi` | PASS（207 operations）|
| 客户端路由/契约/payload | `verify:client-routes` / `client-openapi` / `client-payload` | PASS（97 路由模板、74 方法级调用匹配）|
| 后端全量测试 | `npm run test:backend` | **Tests run: 673, Failures: 0, Errors: 0, Skipped: 0；BUILD SUCCESS** |
| 前端 UI 验收 | `npm run verify:ui` | **28 passed** |
| 真后端 seeded E2E | `npm run verify:ui:real-backend:seeded` | **2 passed**（修复后自愈，见 §3）|
| 聚合交付闸口 | `npm run verify:delivery-gates` | **exit 0**，全部 *_PASS / *_READY |
| 安全扫描 | Semgrep 最新 summary | results=0, errors=0, paths_scanned≈1543 |

## 3. 本轮发现并修复的缺陷（P1）

**问题**：`npm run verify:ui:real-backend:seeded` 首跑失败（exit 1），后端启动即崩，根因为
`java.lang.IllegalArgumentException: 'url' must start with "jdbc"`。

**根因**：运行环境导出了 12-factor 风格的 `COMPLEX_DATABASE_URL=postgresql://complex:complex_dev_pwd@localhost:5432/complex`（psql / node `pg` 可用，但缺少 `jdbc:` 前缀），覆盖了 `application.yml` 中正确的默认值，导致 Hikari 拒绝。PostgreSQL 本身在 5432 正常可连，后端代码无缺陷。

**修复**：`tools/run-real-backend-ui-e2e.mjs` 新增 `normalizeDatabaseEnv()`——将 `postgresql://user:pass@host/db` 自动归一化为 `jdbc:postgresql://host/db` 并拆出账号密码（仅当对应变量未单独设置时填充），使 seeded 后端无需手动覆盖即可启动。

**验证**：
- 归一化逻辑单元自检覆盖：含凭证 URL、已是 jdbc、无凭证、已设 username、空值 5 种用例，输出正确。
- **不带任何手动覆盖、纯靠机器现有畸形 env 重跑** → 日志输出 `normalized COMPLEX_DATABASE_URL to JDBC form jdbc:postgresql://localhost:5432/complex`，**2 passed (exit 0)**，自愈生效。
- `git diff --check` 行尾空白通过；`node --check` 语法通过。

## 4. P0/P1/P2/P3 当前态

- **P0**：无。核心业务可构建、可测试、可联调。
- **P1（代码类）**：1 项，已修复并验证（真后端 runner DB URL 归一化）。
- **P1（外部阻断）**：8 项"部分实现"需求（REQ-17/21/23b/25/32/40/41/44）卡真实供应商资料/生产证据，按 mock+adapter+台账保留补齐路径，**未伪标完成**。
- **P2/P3**：项目已自行完成大文件瘦身与平台治理页补齐；本轮未发现新增阻塞性质量问题。

## 5. 不可宣称生产上线就绪的诚实边界

以下为 **IT/运维/安全/Owner 线下职责**，按项目 `no-system-write` 边界，编码侧不可代为完成、不得伪造证据：

- NAS 开机自动挂载 / 定时备份 / 容量与失败告警 / 周期性恢复演练证据回填（PRD-NAS-*）。
- 生产凭证轮换台账（数据库、JWT、OPS、NAS 账号）责任人/到期日/credential_ref/轮换证据（PRD-CRED-*）。
- 生产证据 manifest 的 owner/reviewer 评审签署（15 份待回填，含 13 P0）。
- 12 项 production blocker register（6 readiness + 4 credential + 2 security findings）关闭。

## 6. 给用户的环境建议（非阻塞）

机器 shell 当前导出的 `COMPLEX_DATABASE_URL` 缺少 `jdbc:` 前缀，会同样影响 `npm run dev:backend`（直接启动后端）。建议在 `.env` 或 shell 中改为：
`COMPLEX_DATABASE_URL="jdbc:postgresql://127.0.0.1:5432/complex"`，并将账号密码用 `COMPLEX_DATABASE_USERNAME/PASSWORD` 单独提供（与 `.env.example` 一致）。真后端 E2E 已通过 runner 归一化自愈，此项仅为彻底消除隐患。
