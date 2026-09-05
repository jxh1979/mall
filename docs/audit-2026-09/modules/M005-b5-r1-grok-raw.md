CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M005 B5-r1 Grok 整模块只读审计报告

- 版本：V0.01（本文件此前不存在，本次创建）
- 控制：V0.17
- 模块：M005 / 322（`01-root-config-and-guidance-03`，NORMAL）
- 步骤：B5-r1（B3=1，ledger 保持 IN_PROGRESS；本席不修、不进 M006、不写 ledger PASS）
- 席位：Grok（审计席乙）
- 范围：当前四文件共 232 行，全量逐字段 / 127 根 scripts / 9 个编译选项 / 两份 Playwright 配置
- 冻结映射：`criteria-lock.md:6385` `M005 → [GEN-001 … GEN-018]`
- 互盲：未读任何 M005 B1/B2/B4/B5 raw、其他模块报告、findings 历史意见正文。仅抽取 `findings.md` 中 12 行 M005 队列表（当前修复队列全部为「否」），不沿用其结论代替独立全量检查。新问题若有则用 GR 临时编号。

## 0. 技能预检（本席环境实核，按合同收窄）

| 技能 | 路径存在 | 本席用法 |
|---|---|---|
| `using-superpowers` | 是 `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检 |
| `verification-before-completion` | 是（同 superpowers 6.3.0） | 凡声称均附本轮命令与退出码 |
| `grilling` | 是 `mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只借追问结构，自问自答；不派子代理、不等操作员 |
| `grill-me` | 是（同 grilling 目录） | 同上收窄 |
| `batch-grill-me` | **缺口** 未找到 SKILL.md | 不冒充已调用 |
| `ubiquitous-language` | **缺口** | 不冒充已调用 |
| `diagnosing-bugs` | 是 | 只借读错误→复现→追数据流；不改源、不加诊断桩 |
| `codebase-design` | 是 | 用模块/接口/缝/适配器词汇审两份 Playwright 配置 |
| `systematic-debugging` | 是 | 收窄：不改生产代码 |
| `receiving-code-review` | 是 | B5 禁止修复；只核不实施 |
| `tdd` / `test-driven-development` | 是 | B5 不落测试 |
| `ponytail-review` | 是 | 过工程化扫描，不改源 |
| `ponytail` | 是 | B5 不用（修复默认强度属 B3） |
| `writing-for-agents` | 是 | 本报告给下游席阅读 |
| `i-have-adhd` | 是 | 合同要求本报告首行必须是独占 `CRITERIA-LOCK-SHA256:`，覆盖该技能「首行即下一步动作」 |
| `git-guardrails-claude-code` | 是（可选） | 本席无 git 写操作，未启用 |
| `using-arc` | 是 | 本任务非 Arc 生命周期，不走 ideate/implement |

未调用子代理。未 `npm install`/`npm ci`、未 `corepack prepare/activate`、未启动生产/DB/部署、未跑真实业务用例。`--list` 只证明收集。

## 1. 范围、哈希、分类

### 1.1 四文件实算（与派发声明一致）

命令：`sha256sum package.json playwright.config.ts playwright.real-backend.config.ts tsconfig.base.json package-lock.json` 与 `wc -l`。

| 路径 | 行数 | SHA256 |
|---|---|---|
| `package.json` | 147 | `350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f` |
| `playwright.config.ts` | 26 | `c1c72998765d8aa42b83f11c557a1e8ea886eb1c557207aab1ded8d5d5bd4700` |
| `playwright.real-backend.config.ts` | 46 | `956e9484517c7620833962e3ec45d63e940e8de452a201ab9a9fd148e5355132` |
| `tsconfig.base.json` | 13 | `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db` |
| **合计** | **232** | — |
| `package-lock.json`（M005-001 既定根 engine 镜像，不重开 M004） | — | `d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef` |

判据锁：本席独立 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与 sidecar `plan/criteria-lock.sha256` 全文一行相同。

`git ls-files -z` 列出后剔除 `docs/audit-2026-09/`、`docs/_archive/`、`node_modules/`、`test-results/`、`playwright-report*`、`.scratch/`、`audit/`、`*-raw.md`：纳入清单 2939 条。本模块仅上述四源文件。

### 1.2 2.1 四分类（冲突→细化→技术性→添加）

三份最高 HTML 中检索 `Node`/`npm`/`Playwright`/`package.json`/`tsconfig`/`engines`/`Vite`/`构建配置`/`包管理`：BEQR、BSM 均为 0；PGM 无这些技术栈词。

PGM 有「界面全中文（所有终端零英文）」强制规则（后台/客人端/现场设备/安卓端展示给用户的文字须中文；可例外仅单据号、设备编号、二维码内容、系统内部标识符）。本模块：

- 无菜单/页面/枚举/按钮等用户界面文案
- Playwright 配置对操作者抛出的中文错误属于 CLI，不是 PGM 所列四类终端 UI；且文案已是中文
- scripts 名、JSON 键、编译选项名为系统内部标识符（PGM 明文例外）

**整模块归类：技术性。** 无与 HTML 明文冲突；无业务实体/枚举/价格/权限发明；构建、脚本、测试配置、编译选项服务于能构建/运行/被测试。不进入添加、不问操作员。技术性仍按 GEN 正常工程审计。

### 1.3 findings.md 队列表（仅 12 行，不作结论）

| ID | 修复队列 |
|---|---|
| M005-001 … M005-006 | 否 |
| M005-007 … M005-012 | 否 |

本席不以该表代替检查。下列审计按当前四文件独立完成。

## 2. `package.json` 逐字段

| 字段 | 值 | 审计 |
|---|---|---|
| `name` | `entertainment-complex-management` | 与 lock 根 `name` / `packages[""].name` 相同 |
| `private` | `true` | 阻止误发布；技术性 |
| `version` | `0.1.0` | 应用 semver，非业务规则 |
| `documentVersion` | `V0.01` | 仅出现在本文件；元数据戳，不是「已完成」声明。ponytail-review 可删 1 行，不构成首次运行缺陷 |
| `type` | `module` | 与 Playwright TS `import`、tools `*.mjs` 一致。lock 根未镜像该字段（npm 惯例），不判缺陷 |
| `workspaces` | `["apps/web"]` | 与 lock 根相同；`build`/`typecheck`/`dev:web`/`test:unit` 转发到该工作区 |
| `scripts` | 127 条 | 见 §3。`Object.keys` 计数 127，名称均 `[A-Za-z0-9:_-]+` |
| `packageManager` | `npm@10.9.8` | 声明 Corepack 包管理器。本席**未** prepare/activate。当前 PATH：`npm -v` → `11.19.0`，`node -v` → `v24.20.0`，`corepack 0.35.0` 且 `COREPACK_ROOT` 未设。不得声称 npm 10.9.8 已激活，也不得猜回滚 |
| `engines.node` | `^22.13.0 \|\| >=24.0.0` | 与 `package-lock.json` `packages[""].engines` **逐字相等**（M005-001 镜像边界）。已装 `@playwright/test@1.61.0` engines `node>=18`；`vite@8.0.16` engines `^20.19.0 \|\| >=22.12.0`。当前 Node 24.20.0 落在声明内。无第二套 Node 二进制，**不写跨版本构建通过**。`>=24.0.0` 上界未测，不据此重开 M004 |
| `devDependencies` | `@playwright/test ^1.61.0`，`yaml ^2.9.0` | 与 lock 根 devDependencies 相等。yaml 的明确消费者：`tools/apply-openapi-p0-examples.mjs`、`tools/verify-openapi-contract.mjs`（均被根 scripts 调用） |

无 `dependencies`、无 `overrides`、无 `os`/`cpu`。CI 消费者 `.github/workflows/predeploy-gates.yml` 使用 `node-version: "22"` + `npm ci --ignore-scripts` + `npm run verify:predeploy`（本模块脚本图内，见 86）。

## 3. 127 根 scripts 逐项表（可复核）

计数命令：解析 `package.json` `scripts` 键。分类：`node` 直接 `node tools/…`；`npm` 以 `npm run` 开头（含混列 `&& node`）；`playwright`；`env-playwright` 为 `REAL_BACKEND_E2E=1 playwright …`。

`node tools/…` 用 `findall` 得 **75** 个唯一目标文件：全部 `git ls-files` 已跟踪，全部 `node --check` 退出 0。多目标仅 90：`generate-production-closure-gap-report.mjs` + `verify-production-closure-gap-reports.mjs`。根脚本互引 `npm run <name>` 在排除 `--workspace` 后 **unknown=[]**。

| # | 类型 | 名称 | 命令 |
|---|---|---|---|
| 1 | npm | `dev` | `npm run dev:backend` |
| 2 | node | `dev:backend` | `node tools/run-backend-maven.mjs spring-boot:run` |
| 3 | node | `test:backend` | `node tools/run-backend-maven.mjs test` |
| 4 | node | `verify:backend-env` | `node tools/run-backend-maven.mjs --check` |
| 5 | npm | `dev:api` | `npm run dev:backend` |
| 6 | npm | `dev:web` | `npm run dev --workspace apps/web` |
| 7 | node | `verify:openapi` | `node tools/verify-openapi-contract.mjs` |
| 8 | node | `audit:client-routes` | `node tools/audit-client-api-routes.mjs` |
| 9 | node | `verify:client-routes` | `node tools/audit-client-api-routes.mjs --strict` |
| 10 | node | `audit:client-openapi` | `node tools/audit-client-openapi-coverage.mjs` |
| 11 | node | `verify:client-openapi` | `node tools/audit-client-openapi-coverage.mjs --strict` |
| 12 | node | `audit:client-payload` | `node tools/verify-client-payload-policy.mjs` |
| 13 | node | `verify:client-payload` | `node tools/verify-client-payload-policy.mjs --strict` |
| 14 | node | `docs:openapi-examples` | `node tools/apply-openapi-p0-examples.mjs` |
| 15 | node | `verify:openapi-examples` | `node tools/verify-openapi-p0-examples.mjs` |
| 16 | node | `docs:operation-matrix` | `node tools/generate-openapi-operation-matrix.mjs` |
| 17 | node | `verify:operation-matrix` | `node tools/generate-openapi-operation-matrix.mjs --check` |
| 18 | node | `docs:openapi-closure` | `node tools/generate-openapi-contract-closure-appendix.mjs` |
| 19 | node | `verify:openapi-closure` | `node tools/generate-openapi-contract-closure-appendix.mjs --check` |
| 20 | node | `docs:security-baseline` | `node tools/generate-formal-security-baseline.mjs` |
| 21 | node | `docs:entry-baseline` | `node tools/refresh-entry-doc-baseline.mjs` |
| 22 | node | `verify:html-ledger-sync` | `node tools/verify-html-ledger-sync.mjs` |
| 23 | node | `verify:entry-anchors` | `node tools/verify-entry-anchors.mjs` |
| 24 | node | `verify:html-param-tier` | `node tools/verify-html-param-tier.mjs` |
| 25 | node | `verify:entry-doc-baseline` | `node tools/refresh-entry-doc-baseline.mjs --check` |
| 26 | npm | `verify:security-baseline` | `npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict` |
| 27 | node | `verify:prod-docs-access` | `node tools/verify-prod-docs-access-policy.mjs` |
| 28 | node | `verify:ops160-execution-policy` | `node tools/verify-ops160-execution-policy.mjs` |
| 29 | node | `verify:ops160-backup-restore` | `node tools/verify-160-backup-restore-scripts.mjs` |
| 30 | node | `verify:production-credentials` | `node tools/verify-production-credential-rotation-ledger.mjs` |
| 31 | node | `verify:p0a-ops-hardening` | `node tools/verify-p0a-ops-hardening.mjs` |
| 32 | npm | `verify:ops160-recovery` | `npm run verify:coding-host-protection && npm run verify:ops160-execution-policy && npm run verify:ops160-backup-restore && npm run verify:production-credentials && npm run verify:p0a-ops-hardening && npm run verify:nas-smb-mount-evidence` |
| 33 | npm | `verify:p0-contract` | `npm run verify:no-legacy-api-prototype && npm run verify:openapi && npm run verify:openapi-examples && npm run verify:operation-matrix && npm run verify:openapi-closure && npm run verify:client-routes && npm run verify:client-openapi && npm run verify:client-payload` |
| 34 | npm | `verify:p0-web` | `npm run verify:p0-contract && npm run build` |
| 35 | npm | `verify:p0-env` | `npm run verify:backend-env && npm run verify:android-env` |
| 36 | npm | `build` | `npm run build --workspace apps/web` |
| 37 | npm | `typecheck` | `npm run typecheck --workspace apps/web` |
| 38 | npm | `smoke:p0` | `npm run test:backend` |
| 39 | node | `verify:android-env` | `node tools/run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app` |
| 40 | node | `android:p0:build` | `node tools/run-android-gradle.mjs --project apps/android-terminal clean build` |
| 41 | node | `android:p0:smoke` | `node tools/run-android-p0-smoke.mjs` |
| 42 | node | `android:p0:apk` | `node tools/run-android-gradle.mjs --project apps/android-terminal/android-app --require-android-sdk :app:assembleDebug` |
| 43 | npm | `verify:p0` | `npm run verify:coding-host-protection && npm run verify:p0-contract && npm run verify:security-baseline && npm run test:backend && npm run build && npm run verify:android-env && npm run android:p0:build && npm run verify:android-p0-smoke-wrapper && npm run android:p0:smoke && npm run android:p0:apk` |
| 44 | node | `verify:no-legacy-api-prototype` | `node tools/verify-no-legacy-api-prototype.mjs` |
| 45 | node | `verify:coding-host-protection` | `node tools/verify-coding-host-protection.mjs` |
| 46 | node | `verify:t3-coding-package` | `node tools/verify-t3-coding-package.mjs` |
| 47 | node | `verify:t3b-supplier-mock-kit` | `node tools/verify-t3b-supplier-mock-kit.mjs` |
| 48 | node | `verify:t3d-local-llm-ai-quality` | `node tools/verify-t3d-local-llm-ai-quality.mjs` |
| 49 | node | `verify:external-dependency-gates` | `node tools/verify-external-dependency-gates.mjs` |
| 50 | node | `verify:delivery-gates-docs` | `node tools/verify-delivery-gates.mjs` |
| 51 | npm | `verify:delivery-gates` | `npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:migration-version-order && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:unit && npm run verify:coding-host-protection && npm run verify:external-dependency-gates && npm run verify:integration-capability-switches && npm run verify:external-evidence-requirement-traceability && npm run verify:partial-requirement-mock-coverage && npm run verify:t3-coding-package && npm run verify:t3b-supplier-mock-kit && npm run verify:t3d-local-llm-ai-quality && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:final-closed-loop-audit && npm run verify:closed-loop-current-state && npm run verify:runtime-acceptance-evidence && npm run verify:ui-experience-acceptance && npm run verify:production-readiness-evidence && npm run verify:nas-current-state-drift && npm run verify:nas-diagnostics && npm run verify:nas-it-handoff && npm run verify:production-blocker-register-no-go-fixtures && npm run verify:production-blockers && npm run verify:production-closure-gap-reports && npm run verify:production-evidence-backfill-execution-ledger && npm run verify:production-evidence-backfill-priority && npm run verify:production-evidence-crosslinks && npm run verify:production-evidence-draft-prep && npm run verify:production-evidence-draft-prep-fixtures && npm run verify:production-evidence-intake && npm run verify:production-evidence-manifest-templates && npm run verify:production-evidence-negative-fixtures && npm run verify:production-evidence-owner-execution-packets && npm run verify:production-evidence-owner-guides && npm run verify:production-evidence-p0-quickstart && npm run verify:production-evidence-positive-fixtures && npm run verify:production-evidence-positive-propagation-fixtures && npm run verify:production-evidence-submission-kit && npm run verify:production-evidence-submissions-guard && npm run verify:production-evidence-submission-status && npm run verify:production-no-go-command-center && npm run verify:production-owner-actions && npm run verify:production-ready-no-go-fixtures && npm run verify:production-ready-positive-fixtures` |
| 52 | node | `verify:nas-smb-mount-evidence` | `node tools/verify-nas-smb-mount-evidence.mjs` |
| 53 | node | `verify:production-readiness-evidence` | `node tools/verify-production-readiness-evidence.mjs` |
| 54 | node | `docs:production-blockers` | `node tools/generate-production-blocker-register.mjs` |
| 55 | node | `verify:production-blockers` | `node tools/generate-production-blocker-register.mjs --check` |
| 56 | node | `verify:production-evidence-intake` | `node tools/verify-production-evidence-intake-templates.mjs` |
| 57 | node | `docs:production-owner-actions` | `node tools/generate-production-owner-action-packs.mjs` |
| 58 | node | `verify:production-owner-actions` | `node tools/generate-production-owner-action-packs.mjs --check` |
| 59 | node | `docs:production-evidence-submission-kit` | `node tools/generate-production-evidence-submission-kit.mjs` |
| 60 | node | `verify:production-evidence-submission-kit` | `node tools/generate-production-evidence-submission-kit.mjs --check` |
| 61 | node | `docs:production-evidence-submission-status` | `node tools/generate-production-evidence-submission-status.mjs` |
| 62 | node | `verify:production-evidence-submission-status` | `node tools/generate-production-evidence-submission-status.mjs --check` |
| 63 | node | `docs:production-evidence-manifest-templates` | `node tools/generate-production-evidence-manifest-templates.mjs` |
| 64 | node | `verify:production-evidence-manifest-templates` | `node tools/generate-production-evidence-manifest-templates.mjs --check` |
| 65 | node | `docs:production-evidence-owner-guides` | `node tools/generate-production-evidence-owner-guides.mjs` |
| 66 | node | `verify:production-evidence-owner-guides` | `node tools/generate-production-evidence-owner-guides.mjs --check` |
| 67 | node | `verify:production-evidence-submissions-guard` | `node tools/verify-production-evidence-submissions-guard.mjs` |
| 68 | node | `docs:production-evidence-backfill-priority` | `node tools/generate-production-evidence-backfill-priority.mjs` |
| 69 | node | `verify:production-evidence-backfill-priority` | `node tools/generate-production-evidence-backfill-priority.mjs --check` |
| 70 | node | `docs:production-evidence-backfill-execution-ledger` | `node tools/generate-production-evidence-backfill-execution-ledger.mjs` |
| 71 | node | `verify:production-evidence-backfill-execution-ledger` | `node tools/generate-production-evidence-backfill-execution-ledger.mjs --check` |
| 72 | playwright | `test:ui` | `playwright test` |
| 73 | playwright | `verify:ui` | `playwright test` |
| 74 | playwright | `test:ui:update` | `playwright test --update-snapshots` |
| 75 | env-playwright | `verify:ui:real-backend` | `REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts` |
| 76 | node | `verify:ui:real-backend:seeded` | `node tools/run-real-backend-ui-e2e.mjs` |
| 77 | node | `verify:android-p0-smoke-wrapper` | `node tools/verify-android-p0-smoke-wrapper.mjs` |
| 78 | node | `verify:full-admin-matrix` | `node tools/verify-full-admin-page-matrix.mjs` |
| 79 | npm | `test:unit` | `npm run test:unit --workspace apps/web` |
| 80 | npm | `verify:unit` | `npm run test:unit --workspace apps/web` |
| 81 | node | `docs:domain-ownership-scope` | `node tools/generate-domain-ownership-scope-audit.mjs` |
| 82 | node | `verify:domain-ownership-scope` | `node tools/generate-domain-ownership-scope-audit.mjs --check` |
| 83 | node | `verify:matrix-vs-migration` | `node tools/verify-matrix-vs-migration.mjs` |
| 84 | node | `verify:migration-version-order` | `node tools/verify-migration-version-order.mjs` |
| 85 | node | `verify:tenant-isolation` | `node tools/verify-tenant-isolation.mjs` |
| 86 | npm | `verify:predeploy` | `npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation` |
| 87 | node | `verify:pii-encryption` | `node tools/verify-pii-encryption.mjs` |
| 88 | node | `verify:test-db-isolation` | `node tools/verify-test-db-isolation.mjs` |
| 89 | node | `docs:production-closure-gap-reports` | `node tools/generate-production-closure-gap-report.mjs` |
| 90 | node | `verify:production-closure-gap-reports` | `node tools/generate-production-closure-gap-report.mjs --check && node tools/verify-production-closure-gap-reports.mjs` |
| 91 | node | `verify:final-closed-loop-audit` | `node tools/verify-final-closed-loop-audit.mjs` |
| 92 | node | `verify:production-evidence-negative-fixtures` | `node tools/verify-production-evidence-negative-fixtures.mjs` |
| 93 | node | `docs:nas-current-state-drift` | `node tools/generate-nas-current-state-drift-report.mjs` |
| 94 | node | `verify:nas-current-state-drift` | `node tools/generate-nas-current-state-drift-report.mjs --check` |
| 95 | node | `docs:closed-loop-current-state` | `node tools/generate-closed-loop-current-state.mjs` |
| 96 | node | `verify:closed-loop-current-state` | `node tools/generate-closed-loop-current-state.mjs --verify` |
| 97 | npm | `verify:closed-loop-runtime` | `npm run test:backend && npm run typecheck --workspace apps/web && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:real-backend:seeded` |
| 98 | node | `verify:nas-it-handoff` | `node tools/verify-nas-it-handoff.mjs` |
| 99 | node | `docs:external-evidence-requirement-traceability` | `node tools/generate-external-evidence-requirement-traceability.mjs` |
| 100 | node | `verify:external-evidence-requirement-traceability` | `node tools/generate-external-evidence-requirement-traceability.mjs --check` |
| 101 | node | `docs:nas-diagnostics` | `node tools/generate-nas-diagnostic-report.mjs` |
| 102 | node | `verify:nas-diagnostics` | `node tools/generate-nas-diagnostic-report.mjs --check` |
| 103 | node | `docs:production-evidence-p0-quickstart` | `node tools/generate-production-evidence-p0-quickstart.mjs` |
| 104 | node | `verify:production-evidence-p0-quickstart` | `node tools/generate-production-evidence-p0-quickstart.mjs --check` |
| 105 | node | `docs:runtime-acceptance-evidence` | `node tools/generate-runtime-acceptance-evidence.mjs` |
| 106 | node | `verify:runtime-acceptance-evidence` | `node tools/generate-runtime-acceptance-evidence.mjs --check` |
| 107 | node | `docs:ui-experience-acceptance` | `node tools/generate-ui-experience-acceptance-report.mjs` |
| 108 | node | `verify:ui-experience-acceptance` | `node tools/generate-ui-experience-acceptance-report.mjs --check` |
| 109 | node | `docs:production-evidence-owner-execution-packets` | `node tools/generate-production-evidence-owner-execution-packets.mjs` |
| 110 | node | `verify:production-evidence-owner-execution-packets` | `node tools/generate-production-evidence-owner-execution-packets.mjs --check` |
| 111 | node | `prepare:production-evidence-drafts` | `node tools/prepare-production-evidence-manifest-drafts.mjs` |
| 112 | node | `verify:production-evidence-draft-prep` | `node tools/prepare-production-evidence-manifest-drafts.mjs --check` |
| 113 | node | `verify:production-evidence-draft-prep-fixtures` | `node tools/verify-production-evidence-draft-prep-fixtures.mjs` |
| 114 | node | `verify:production-evidence-positive-fixtures` | `node tools/verify-production-evidence-positive-fixtures.mjs` |
| 115 | node | `verify:production-evidence-positive-propagation-fixtures` | `node tools/verify-production-evidence-positive-propagation-fixtures.mjs` |
| 116 | node | `verify:production-ready-no-go-fixtures` | `node tools/verify-production-ready-no-go-fixtures.mjs` |
| 117 | node | `verify:production-blocker-register-no-go-fixtures` | `node tools/verify-production-blocker-register-no-go-fixtures.mjs` |
| 118 | node | `verify:production-ready-positive-fixtures` | `node tools/verify-production-ready-positive-fixtures.mjs` |
| 119 | node | `verify:production-evidence-crosslinks` | `node tools/verify-production-evidence-crosslinks.mjs` |
| 120 | node | `docs:partial-requirement-mock-coverage` | `node tools/generate-partial-requirement-mock-coverage.mjs` |
| 121 | node | `verify:partial-requirement-mock-coverage` | `node tools/generate-partial-requirement-mock-coverage.mjs --check` |
| 122 | node | `docs:production-no-go-command-center` | `node tools/generate-production-no-go-resolution-command-center.mjs` |
| 123 | node | `verify:production-no-go-command-center` | `node tools/generate-production-no-go-resolution-command-center.mjs --check` |
| 124 | node | `docs:integration-capability-switches` | `node tools/generate-integration-capability-switches.mjs` |
| 125 | node | `verify:integration-capability-switches` | `node tools/generate-integration-capability-switches.mjs --check` |
| 126 | node | `docs:data-isolation-spec` | `node tools/generate-data-isolation-spec.mjs` |
| 127 | node | `verify:data-isolation-spec` | `node tools/generate-data-isolation-spec.mjs --check` |

同命令体别名（入口用途可核，不单独开缺陷）：`dev`≡`dev:api`；`test:ui`≡`verify:ui`（97 调用后者）；`test:unit`≡`verify:unit`（51 调用后者）；`smoke:p0`→`test:backend`。不存在第三名 `smoke:p0-ops`。

## 4. `playwright.config.ts` 逐字段（mock 默认套件）

源码 26 行。`node --experimental-strip-types` 导入退出 0。本进程 `CI=true`，故求值 `reuseExistingServer === false`；源码为 `!process.env.CI`，开发机无 CI 时为 true。不得把本进程 CI 当成仓库默认开发行为。

| 字段 | 值 | 审计 |
|---|---|---|
| `testDir` | `./tests/e2e` | 与目录内已跟踪 spec 一致 |
| `testIgnore` | `web-ui-real-backend.spec.ts`, `*-live-evidence.spec.ts` | `--list` 未收集这 4 个真实文件 |
| `globalSetup` | `./tests/e2e/support/global-setup.ts` | 文件存在。`--list` 不跑 globalSetup（Playwright `listMode` 任务无 `createGlobalSetupTasks`） |
| `timeout` | 30000 | 有界 |
| `expect.timeout` | 8000 | 有界 |
| `fullyParallel` | false | 单文件串行，降低共享 vite 竞争 |
| `reporter` | list + html `playwright-report` `open:never` | 不自动打开浏览器 |
| `use.baseURL` | `http://127.0.0.1:5173` | 环回 |
| `use.trace` | `retain-on-failure` | |
| `use.screenshot` | `only-on-failure` | |
| `webServer.command` | `npm --workspace apps/web run dev -- --host 127.0.0.1` | 追加在 `apps/web` 的 `vite --host 0.0.0.0 --port 5173` 之后 |
| `webServer.url` | `http://127.0.0.1:5173` | 与 apps/web `--port 5173` 一致 |
| `webServer.timeout` | 120000 | 有界 |
| `projects` | `chromium` + `devices["Desktop Chrome"]` | 导入后 `defaultBrowserType=chromium`，viewport 1280×720 |

Vite 8.0.16 `cli.js:650-651`：`filterDuplicateOptions` 对重复 CLI 选项取**最后一项**。因此 `--host 0.0.0.0 … --host 127.0.0.1` 生效主机为 `127.0.0.1`。依据类型：已安装实现原文 + 导入求值。

`--list`（默认配置）退出 0：**32 tests in 6 files**（customer-journey、full-admin-mvp/remaining/ui-hardening/visual、property-fee-evidence）。无 live-evidence、无 web-ui-real-backend。`property-fee-evidence.spec.ts` 使用 `installMockApi`，不属于 `*-live-evidence.spec.ts`，留在 mock 套件正确。

**不得**把 `--list` 写成业务断言通过。

## 5. `playwright.real-backend.config.ts` 逐字段

| 字段 | 值 | 审计 |
|---|---|---|
| 顶层闸 | `REAL_BACKEND_E2E !== "1"` 则 throw 中文 | `env -u REAL_BACKEND_E2E playwright test --list --config=…` **EXIT:1**，stderr：`真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。`（L5） |
| `backendBaseUrl` | env 或 `http://127.0.0.1:8080` | 写入 `webServer.env.VITE_API_PROXY_TARGET`，**不**插进 shell 命令 |
| `webPort` | `Number(REAL_BACKEND_WEB_PORT ?? "5174")`，须 `Number.isInteger` 且 1–65535 | `abc`/`0`/`65536`/`-1`/`1.5`/空/`5174;id` 均 EXIT:1，文案：`REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。`（L11）。合法 `80`/`5174` EXIT:0（仅 --list） |
| `webBaseUrl` | `http://127.0.0.1:${webPort}` | 导入默认 5174 |
| `testDir` | `./tests/e2e` | |
| `testMatch` | `web-ui-real-backend.spec.ts`, `*-live-evidence.spec.ts` | 与 mock `testIgnore` 成对 |
| `timeout` / `expect.timeout` | 45000 / 10000 | 有界 |
| `fullyParallel` | false | |
| `reporter` | list；html `playwright-report-real-backend` `open:never`；json `test-results/real-backend-results.json` | 路径与 wrapper `resultsPath` 相同 |
| `use.baseURL` | `webBaseUrl` | 默认 5174，避免与 mock 5173 抢端口 |
| `webServer.command` | ``npm --workspace apps/web run dev -- --host 127.0.0.1 --port ${webPort} --strictPort`` | `webPort` 已是校验后的 number，模板插值无 shell 注入面 |
| `webServer.reuseExistingServer` | false | 不复用脏服务器 |
| `webServer.env` | `{...process.env, VITE_API_PROXY_TARGET: backendBaseUrl}` | 覆盖同名键；子进程需要 PATH。对抗式见 GEN-010 |
| `projects` | chromium | |
| 无 `globalSetup` | — | 真实套件不走 mock 预热 |

`REAL_BACKEND_E2E=1 playwright test --list --config=playwright.real-backend.config.ts` 退出 0：**18 tests in 4 files**（3 个 `*-live-evidence` + `web-ui-real-backend`）。四文件 `beforeEach` 仅在 `REAL_BACKEND_E2E !== "1"` 时 `test.skip`；本入口已设 `=1`，收集路径上 skip 条件为假。`--list` ≠ 跑通。

消费者 `tools/run-real-backend-ui-e2e.mjs`：`node --check` 0。调用 `npm run verify:ui:real-backend` 并读 json `stats`，要求 `expected>=1 && skipped===0 && unexpected===0 && flaky===0`。默认后端端口 18080 并设置 `REAL_BACKEND_BASE_URL`，与配置默认 8080 不冲突（wrapper 覆盖）。本席不启动 Java/DB。

## 6. `tsconfig.base.json` 九个编译选项

仅 `compilerOptions` 九键，无 `include`/`files`（由消费者扩展）。

`tsc --showConfig`（scratch 临时 tsconfig `extends` 本文件，已删除）析出：

```
target=es2022, module=esnext, moduleResolution=bundler, strict=true,
esModuleInterop=true, forceConsistentCasingInFileNames=true,
skipLibCheck=true, resolveJsonModule=true, noUncheckedIndexedAccess=true
```

| 选项 | 正/负例（`.scratch/m005-b5-grok-tsc`，已 `rm -rf`） |
|---|---|
| 九键继承 | `--showConfig` 全出现 |
| `strict` | 负：`function f(x){return x.foo}` → TS7006 implicit any，exit 2 |
| `noUncheckedIndexedAccess` | 正：`const first: string\|undefined = rows[0]` exit 0；负：`const first: string = rows[0]` → TS2322，exit 2 |
| `resolveJsonModule` | 正：`import data from "./data.json"` exit 0 |
| `target ES2022` | 正：`Array.prototype.at` exit 0 |
| `forceConsistentCasingInFileNames` | 负：`import from "./casinglib"` 而文件为 `CasingLib.ts` → TS2307（linux 大小写敏感 FS），exit 2 |
| `module`/`moduleResolution` | Bundler+ESNext，与 Vite 8 / apps/web `tsc -b` 消费者一致 |
| `esModuleInterop` | 对 Bundler 冗余但无害 |
| `skipLibCheck` | 不吞项目错误（上列负例仍红） |

消费者 `apps/web/tsconfig.json` `extends` `../../tsconfig.base.json`，并加 `jsx: react-jsx`、`types: ["vite/client"]`。`--showConfig -p apps/web/tsconfig.json` 九键仍在。`tests/e2e` 不在该 include 内，由 Playwright 自带加载器处理，不是 base 的遗漏义务。

## 7. 三线全模块

### 7.1 第一性原理

1. **为何必须存在？** 三份 HTML 无 Node/npm/Playwright 条目。删掉这四文件，HTML 业务句不会少一个字，但仓库无法声明工作区、无法收集 UI 测试、apps/web 无法 extends 编译选项。2.1 归技术性：无 HTML 上位不等于该删。
2. **声称行为是否执行验证？** 配置导入、`--list` 收集、端口/环境闸、tsc 正负例、75 个 tool `node --check`、Vite 重复 flag 源码。未跑 e2e 用例、未跨 Node 版本构建、未 corepack 激活。
3. **未验证就当真的前提？** （a）Node 22.13 与 Node 25+ 未执行；（b）`packageManager npm@10.9.8` 本环境未激活；（c）`--list` ≠ 断言绿；（d）本进程 `CI=true` 影响 `reuseExistingServer` 求值，已按源码区分。

依据类型：HTML 原文（分类）+ 可复现命令输出 + 已装 Vite/Playwright 实现原文。

### 7.2 顺从式（冻结 GEN + HTML）

映射 18 条 GEN 均已落到本模块证据或明确 N/A（§8）。脚本图无悬空 `npm run`。真实/mock 收集集互补。engine 与 lock 镜像一致。中文错误满足「若把 CLI 报错当展示」也不与 PGM 冲突。

### 7.3 对抗式

| 攻击 | 结果 |
|---|---|
| 无 `REAL_BACKEND_E2E` 跑真实配置假绿 | 配置 throw，Playwright EXIT 1 |
| 无效/注入型 `REAL_BACKEND_WEB_PORT` 进 shell | `Number`+范围闸，EXIT 1，未拼进命令 |
| 默认套件收集 live-evidence 再 skip 仍 0 | `testIgnore`；`--list` 32/6，无那 4 文件 |
| 真实套件 skip-as-success | 入口已设 env=1，skip 条件假；seeded wrapper 另读 json 计数 |
| `--host 0.0.0.0` 未被覆盖 | Vite `filterDuplicateOptions` 取最后一项 → 127.0.0.1 |
| 两套 web 抢 5173 | 真实默认 5174 + `--strictPort` |
| yaml 无消费者 | 两个 OpenAPI 工具 import yaml |
| 75 个目标缺文件或语法错 | 全跟踪，`node --check` 全 0 |
| `process.env` 展开泄漏到浏览器 | 仅非 `VITE_` 留在 Vite 服务端；`VITE_API_PROXY_TARGET` 被显式覆盖。首次正常跑不撞客户端泄密。不升为缺陷 |
| 别名死代码 | `test:`/`verify:` 成对；51/97 使用 verify 名。`dev:api` 为入口别名。无第三名 `smoke:p0-ops` |

ponytail-review：`documentVersion` 与 `dev:api` 可删但非首次运行必撞。结论：`Lean enough. 不因别名 FAIL。`

## 8. GEN-001 … GEN-018（每条三线或 N/A 具体原因）

| ID | 适用 | 第一性原理 | 顺从 | 对抗 | 依据类型 |
|---|---|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 四文件是构建/测试入口，非 HTML 业务句 | 127 脚本均有目标；75 tools 存在 | 别名有入口用途；无第三重复名 | 命令+源码 |
| GEN-002 可执行正确性 | 适用 | 导入/list/闸/tsc/`node --check` | 声明的 mock/real 入口与收集集一致 | 无 env 真实配置失败显式 | 可执行产物+命令 |
| GEN-003 测试有效性 | 适用（配置层） | `--list` 只证明收集 | mock 32/6、real 18/4；skip 条件与闸对齐 | 未跑用例，不声称通过 | 命令（收集） |
| GEN-004 信任边界 | 适用 | `WEB_PORT` 在插值前校验 | 非法端口 EXIT 1 | `BASE_URL` 不进 shell；恶意 URL 需操作员故意设置，非首次必撞 | 命令 |
| GEN-005 授权与最小权限 | **N/A** | 四文件不实现主体-对象-动作授权。真实用例 REAL-UI-03 在测试文件中，属其他模块 | — | 配置未设 bypass | 模块边界 |
| GEN-006 数据完整性 | **N/A** | 无状态转换/金额/约束 | — | — | 模块边界 |
| GEN-007 并发与幂等 | 适用（弱） | `fullyParallel:false` 收窄共享 vite 竞争 | 真实 `reuseExistingServer:false` | 未设 `workers` 上限；依赖 Playwright 默认。高核机靠 globalSetup 预热。非配置错误 | 源码 |
| GEN-008 接口一致性 | 适用 | engine 与 lock 相等；json 路径与 wrapper 相等；端口与 url 相等 | apps/web extends 九键 | mock url 5173 与 apps/web `--port 5173` 一致 | lock+导入 |
| GEN-009 错误处理 | 适用 | 缺 env/坏端口 throw 中文且 EXIT 1 | 不吞错、不假绿 | 见对抗表 | 命令 |
| GEN-010 安全 | 适用 | 端口不进未校验 shell；监听 127.0.0.1 | 无明文生产密钥在四文件中 | env 展开见 §7.3 | 源码+Vite 实现 |
| GEN-011 可观测 | 适用 | json reporter + wrapper 计数；html `open:never` | 真实路径可核对 skip/fail | `--list` 不写 json，不误用 | 源码 |
| GEN-012 资源边界 | 适用 | 超时均有限 | webServer 120s | 无无界循环在四文件 | 源码 |
| GEN-013 构建配置与依赖 | 适用 | engine=lock；当前 Node 24.20.0 落入声明 | yaml/playwright 已装且有消费者 | 未测 Node 22.13/25+；未激活 npm@10.9.8。不写跨版本通过 | lock+`node -v` |
| GEN-014 迁移 | **N/A** | 四文件无 SQL/Flyway/迁移脚本。`verify:migration-version-order` 只是根脚本指针，实现在 tools（他模块） | — | — | 模块边界 |
| GEN-015 界面可用 | **N/A（UI）/适用（文案例外）** | 无用户界面。CLI 中文错误；脚本名为内部标识符。PGM「全中文」针对四类终端展示 | 不与 PGM 冲突 | 测试 skip 英文在 spec 文件，非本四文件 | HTML 原文 |
| GEN-016 文档可验证 | 适用（弱） | `// 版本：V0.01` 与 `documentVersion` 为文件戳，未把计划写成已完成 | 路径/命令与现存 scripts 一致 | — | 源码 |
| GEN-017 业务来源分类 | 适用 | 已走冲突→细化→技术性 | 整模块技术性，无添加 | 脚本名含租户/票务等词仅为调用他模块工具的内部标识，不在本模块发明规则 | HTML 沉默+锁 |
| GEN-018 第一性原理证据 | 适用 | 本报告结论落到 HTML/命令/已装实现 | 不用惯例或他席意见 | 队列表「否」未当证据 | 本文件 |

## 9. 原生命令与真实输出（摘要）

```
sha256sum criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

wc -l 四文件 → 147+26+46+13=232
scripts 键数 → 127
node --check 75 tools → OK 75 FAIL 0
node -v → v24.20.0
npm -v → 11.19.0   # 未激活 packageManager
corepack → 0.35.0，COREPACK_ROOT unset，未 prepare/activate

playwright test --list
  Total: 32 tests in 6 files   EXIT 0

env -u REAL_BACKEND_E2E playwright test --list --config=playwright.real-backend.config.ts
  Error: 真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。
  EXIT 1

REAL_BACKEND_E2E=1 playwright test --list --config=playwright.real-backend.config.ts
  Total: 18 tests in 4 files   EXIT 0

REAL_BACKEND_E2E=1 REAL_BACKEND_WEB_PORT=abc|0|65536|-1|1.5|''|'5174;id'
  Error: REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。
  EXIT 1

tsc 正例 exit 0；负例 TS2322/TS7006/TS2307 exit 2
scratch 目录已删除
```

Playwright 1.61 `runAllTestsWithConfig`：`listMode` 任务仅为 load+reportBegin，不启动 webServer、不跑 globalSetup。本轮 `--list` 未拉起 vite。

## 10. Findings

**无新 GR。** 全量审完后未发现须进修复队列的冲突/工程缺陷/添加项。

观察（不进队列、不构成 FAIL）：

- `packageManager npm@10.9.8` 与本机 `npm 11.19.0` 不同；Corepack 未激活，相容性未测。
- `engines` 含 `>=24.0.0` 无上界；无第二 Node 二进制，不声称 22.13 或 25+ 构建通过。
- `documentVersion` 无其他已读消费者。
- 本进程 `CI=true` 使 mock `reuseExistingServer` 求值为 false。

## 11. 审计结论

**FAIL/PASS：PASS**

证据：四文件 232 行与声明 SHA 一致；127 脚本可复核；75 工具语法通过；两套 Playwright 收集集互补且闸有效；九编译选项经 showConfig 与正负例；engine 与 lock 镜像相等；18 条 GEN 均有三线证据或具体 N/A；无待补项、无未答添加项。

限制（不把限制写成有条件通过）：未执行 e2e 业务断言、未跨版本 Node、未激活所声明 npm。这些是证据边界，不是残留缺陷。

本席不写 ledger PASS、不写 `M005-<slug>.md`、不进入 M006、不改源。
