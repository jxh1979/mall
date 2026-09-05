CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M005 B1 Grok raw — V0.01

- 席位：Grok（审计席乙）
- 控制：V0.17
- 模块：M005 `01-root-config-and-guidance-03` / 共 322
- ledger：IN_PROGRESS；B3=0；运行失败重跑=0；LOCK-AMEND-COUNT=0
- HEAD：`134d75d158ca6ff1343f616cce47676ecfc83e02`（main）
- 步骤：B1 修复前全模块三线独立审计；不修源、不问人、不进 B2
- 判据映射：`M005 → [GEN-001 … GEN-018]`（criteria-lock.md L6385）
- 本席实算 lock SHA256：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与 sidecar / ledger 顶部一致）
- 审计结论：**FAIL**

---

## 0. 技能预检（§6）

本席目录实核，不冒充未核技能：

| 技能 | 路径实核 | 本步用法 |
|---|---|---|
| using-superpowers | 存在 | 开工技能路由 |
| brainstorming / writing-plans | 存在 | B1 不改方案，不调用其收尾闸 |
| systematic-debugging | 存在 | 只借读错误→复现→追数据流；不改生产代码 |
| test-driven-development / tdd | 存在 | B1 禁止修复，不落测试 |
| requesting/receiving-code-review | 存在 | B1 不请修、不实施 |
| verification-before-completion | 存在 | PASS/FAIL 前用命令输出 |
| grilling / grill-me | 存在 | 对抗式追问结构；不等操作员、不派子代理 |
| batch-grill-me | **缺口** | 目录不存在 |
| ubiquitous-language | **缺口** | 目录不存在 |
| git-guardrails-claude-code | 存在于 `mattpocock-skills/.../misc/` | 本步无 git 写操作 |
| codebase-design | 存在 | 配置入口=interface，实现在被调脚本 |
| diagnosing-bugs | 存在 | 构造 skip-all 反馈环 |
| resolving-merge-conflicts | 存在 | 本步无合并 |
| writing-for-agents | 存在 | 本 raw 结构 |
| ponytail / ponytail-review | 存在 | 只列可删项，不修 |
| i-have-adhd | 存在 | 输出形态；本文件按 B1 完整证据合同 |

未派子代理。未读 `M005-b1-self-raw.md` 正文；检索 `smoke:p0` 时工具索引命中该路径三行，立即停用该命中，不采信其裁决。未读 M004 raw / findings 过程当证据。

---

## 1. 四文件身份（223 行，终局重算 SHA）

| 文件 | wc -l | SHA256 |
|---|---:|---|
| package.json | 147 | `f59711d7579178d2772ec7456592d616d41ac3d5980e58864aeaf66c2e60b7db` |
| playwright.config.ts | 25 | `f389c55d86716b5bd921ea60320c74936a72e72b4e54a8df6fae9eca6db86e19` |
| playwright.real-backend.config.ts | 38 | `cf7b020c030dbd8ad4aeba54fe331528cd0743c59c22cbd03bdb884fe187afa2` |
| tsconfig.base.json | 13 | `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db` |
| **合计** | **223** | 与派发冻结映射一致 |

`git status --porcelain` 对上述四文件为空。只审当前 main 字节。

---

## 2. 覆盖表

### 2.1 package.json 全部顶层字段

`JSON.parse` 成功。键闭集：`name, private, version, type, workspaces, scripts, packageManager, engines, devDependencies`。无其它顶层键。

| 字段 | 值 | 覆盖 |
|---|---|---|
| name | `entertainment-complex-management` | 系统内部标识符，非展示文案 |
| private | `true` | 禁止误发布 |
| version | `0.1.0` | 与 lock 根记录同号（窄读 lock 根，不审锁图） |
| type | `module` | ESM；两份 Playwright TS 用 `import`，`node --input-type=module` 能加载默认配置 |
| workspaces | `["apps/web"]` | `apps/web/package.json` 存在；`verify-no-legacy-api-prototype` 断言不得含 `apps/api` |
| packageManager | `npm@10.9.8` | `corepack npm -v` → `10.9.8`；宿主 `npm -v` → `11.19.0`（环境差，见局限） |
| engines.node | `>=22` | 本机 `v24.20.0` 满足；CI `setup-node` node-version `"22"` |
| devDependencies.@playwright/test | `^1.61.0` | 已装 `1.61.0` |
| devDependencies.yaml | `^2.9.0` | 已装 `2.9.0`；被 `tools/verify-openapi-contract.mjs` 与 `tools/apply-openapi-p0-examples.mjs` import |

### 2.2 package.json 全部 128 个 scripts

计数：`Object.keys(scripts).length = 128`。所有 `node tools/*.mjs` 落点 **75 个文件均存在**（MISSING=NONE）。所有 `npm run <key>` 引用均指向本对象已有键（DANGLING=NONE）。`apps/web` 被调脚本 `dev/build/typecheck/test:unit` 均存在。

带 `--check/--strict/--verify/--require-android-sdk/--project/--update-snapshots` 的调用，对应工具源或 Playwright `--help` 均含该标志（FLAG_NOT_IN_TOOL_SOURCE=NONE）。

重复命令体（别名，不是断链）：

- `dev` == `dev:api` == `npm run dev:backend`（`backend/README.md` 要求 `dev`/`dev:api`/`smoke:p0` 继续指向 Java 后端，属防回流入口）
- `smoke:p0` == `smoke:p0-ops` == `npm run test:backend`（`smoke:p0-ops` 无仓库内调用方，见 GR-03）
- `test:ui` == `verify:ui` == `playwright test`
- `test:unit` == `verify:unit` == workspace vitest

| # | script | 命令（原文） | 落点/组合 | 2.1 分类 |
|---|---|---|---|---|
| 001 | dev | npm run dev:backend | 组合 | 技术性 |
| 002 | dev:backend | node tools/run-backend-maven.mjs spring-boot:run | 文件存在 | 技术性 |
| 003 | test:backend | node tools/run-backend-maven.mjs test | 文件存在 | 技术性 |
| 004 | verify:backend-env | node tools/run-backend-maven.mjs --check | `--check` 在工具内被解析 | 技术性 |
| 005 | dev:api | npm run dev:backend | 别名 | 技术性 |
| 006 | dev:web | npm run dev --workspace apps/web | web `dev` 存在 | 技术性 |
| 007 | verify:openapi | node tools/verify-openapi-contract.mjs | 文件存在 | 技术性 |
| 008 | audit:client-routes | node tools/audit-client-api-routes.mjs | 文件存在 | 技术性 |
| 009 | verify:client-routes | 同上 --strict | 标志在源 | 技术性 |
| 010 | audit:client-openapi | node tools/audit-client-openapi-coverage.mjs | 文件存在 | 技术性 |
| 011 | verify:client-openapi | 同上 --strict | 标志在源 | 技术性 |
| 012 | audit:client-payload | node tools/verify-client-payload-policy.mjs | 文件存在 | 技术性 |
| 013 | verify:client-payload | 同上 --strict | 标志在源 | 技术性 |
| 014 | docs:openapi-examples | node tools/apply-openapi-p0-examples.mjs | 生成入口；本席未跑（有副作用写 docs） | 技术性 |
| 015 | verify:openapi-examples | node tools/verify-openapi-p0-examples.mjs | 文件存在 | 技术性 |
| 016 | docs:operation-matrix | node tools/generate-openapi-operation-matrix.mjs | 未跑生成 | 技术性 |
| 017 | verify:operation-matrix | 同上 --check | 标志在源 | 技术性 |
| 018 | docs:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs | 未跑生成 | 技术性 |
| 019 | verify:openapi-closure | 同上 --check | 标志在源 | 技术性 |
| 020 | docs:security-baseline | node tools/generate-formal-security-baseline.mjs | 未跑生成 | 技术性 |
| 021 | docs:entry-baseline | node tools/refresh-entry-doc-baseline.mjs | 未跑生成 | 技术性 |
| 022 | verify:html-ledger-sync | node tools/verify-html-ledger-sync.mjs | 文件存在 | 技术性 |
| 023 | verify:entry-anchors | node tools/verify-entry-anchors.mjs | 文件存在 | 技术性 |
| 024 | verify:html-param-tier | node tools/verify-html-param-tier.mjs | 文件存在 | 技术性 |
| 025 | verify:entry-doc-baseline | node tools/refresh-entry-doc-baseline.mjs --check | 标志在源 | 技术性 |
| 026 | verify:security-baseline | npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict | 组合+标志 | 技术性 |
| 027 | verify:prod-docs-access | node tools/verify-prod-docs-access-policy.mjs | 文件存在 | 技术性 |
| 028 | verify:ops160-execution-policy | node tools/verify-ops160-execution-policy.mjs | 文件存在 | 技术性 |
| 029 | verify:ops160-backup-restore | node tools/verify-160-backup-restore-scripts.mjs | 文件存在 | 技术性 |
| 030 | verify:production-credentials | node tools/verify-production-credential-rotation-ledger.mjs | 文件存在 | 技术性 |
| 031 | verify:p0a-ops-hardening | node tools/verify-p0a-ops-hardening.mjs | 文件存在 | 技术性 |
| 032 | verify:ops160-recovery | 五个 verify 串联 | 组合键均存在 | 技术性 |
| 033 | verify:p0-contract | 七个契约 verify 串联 | 组合键均存在 | 技术性 |
| 034 | verify:p0-web | verify:p0-contract && build | 组合 | 技术性 |
| 035 | verify:p0-env | verify:backend-env && verify:android-env | 组合 | 技术性 |
| 036 | build | npm run build --workspace apps/web | web `build` 存在 | 技术性 |
| 037 | typecheck | npm run typecheck --workspace apps/web | web `typecheck` 存在 | 技术性 |
| 038 | smoke:p0 | npm run test:backend | 文档要求的防回流名 | 技术性 |
| 039 | smoke:p0-ops | npm run test:backend | 无调用方重复名 | 技术性；见 GR-03 |
| 040 | verify:android-env | run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app | 目录存在；标志在源 | 技术性 |
| 041 | android:p0:build | run-android-gradle.mjs --project apps/android-terminal clean build | 指向 Kotlin 核心工程（有 settings.gradle.kts） | 技术性 |
| 042 | android:p0:smoke | node tools/run-android-p0-smoke.mjs | 文件存在 | 技术性 |
| 043 | android:p0:apk | --project apps/android-terminal/android-app --require-android-sdk :app:assembleDebug | 与 env 检查同工程 | 技术性 |
| 044 | verify:p0 | coding-host + p0-contract + security + test:backend + build + android 链 | 组合键均存在 | 技术性 |
| 045 | verify:no-legacy-api-prototype | node tools/verify-no-legacy-api-prototype.mjs | 本席实跑 PASS | 技术性 |
| 046 | verify:coding-host-protection | node tools/verify-coding-host-protection.mjs | 文件存在 | 技术性 |
| 047 | verify:t3-coding-package | node tools/verify-t3-coding-package.mjs | 文件存在 | 技术性 |
| 048 | verify:t3b-supplier-mock-kit | node tools/verify-t3b-supplier-mock-kit.mjs | 文件存在 | 技术性 |
| 049 | verify:t3d-local-llm-ai-quality | node tools/verify-t3d-local-llm-ai-quality.mjs | 文件存在 | 技术性 |
| 050 | verify:external-dependency-gates | node tools/verify-external-dependency-gates.mjs | 文件存在 | 技术性 |
| 051 | verify:delivery-gates-docs | node tools/verify-delivery-gates.mjs | 文件存在 | 技术性 |
| 052 | verify:delivery-gates | 长串 verify（含 verify:migration-version-order、verify:unit 等） | 组合键均存在；未跑全量 | 技术性 |
| 053 | verify:nas-smb-mount-evidence | node tools/verify-nas-smb-mount-evidence.mjs | 文件存在 | 技术性 |
| 054 | verify:production-readiness-evidence | node tools/verify-production-readiness-evidence.mjs | 文件存在 | 技术性 |
| 055 | docs:production-blockers | generate-production-blocker-register.mjs | 未跑生成 | 技术性 |
| 056 | verify:production-blockers | 同上 --check | 标志在源 | 技术性 |
| 057 | verify:production-evidence-intake | verify-production-evidence-intake-templates.mjs | 文件存在 | 技术性 |
| 058 | docs:production-owner-actions | generate-production-owner-action-packs.mjs | 未跑生成 | 技术性 |
| 059 | verify:production-owner-actions | 同上 --check | 标志在源 | 技术性 |
| 060 | docs:production-evidence-submission-kit | generate-production-evidence-submission-kit.mjs | 未跑生成 | 技术性 |
| 061 | verify:production-evidence-submission-kit | 同上 --check | 标志在源 | 技术性 |
| 062 | docs:production-evidence-submission-status | generate-production-evidence-submission-status.mjs | 未跑生成 | 技术性 |
| 063 | verify:production-evidence-submission-status | 同上 --check | 标志在源 | 技术性 |
| 064 | docs:production-evidence-manifest-templates | generate-production-evidence-manifest-templates.mjs | 未跑生成 | 技术性 |
| 065 | verify:production-evidence-manifest-templates | 同上 --check | 标志在源 | 技术性 |
| 066 | docs:production-evidence-owner-guides | generate-production-evidence-owner-guides.mjs | 未跑生成 | 技术性 |
| 067 | verify:production-evidence-owner-guides | 同上 --check | 标志在源 | 技术性 |
| 068 | verify:production-evidence-submissions-guard | verify-production-evidence-submissions-guard.mjs | 文件存在 | 技术性 |
| 069 | docs:production-evidence-backfill-priority | generate-production-evidence-backfill-priority.mjs | 未跑生成 | 技术性 |
| 070 | verify:production-evidence-backfill-priority | 同上 --check | 标志在源 | 技术性 |
| 071 | docs:production-evidence-backfill-execution-ledger | generate-production-evidence-backfill-execution-ledger.mjs | 未跑生成 | 技术性 |
| 072 | verify:production-evidence-backfill-execution-ledger | 同上 --check | 标志在源 | 技术性 |
| 073 | test:ui | playwright test | 默认配置；--list 47/9 | 技术性 |
| 074 | verify:ui | playwright test | 与 073 同体；被 closed-loop-runtime 引用 | 技术性 |
| 075 | test:ui:update | playwright test --update-snapshots | Playwright --help 含该标志 | 技术性 |
| 076 | verify:ui:real-backend | REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts | 官方路径关 skip；配置单独调用见 GR-01 | 技术性 |
| 077 | verify:ui:real-backend:seeded | node tools/run-real-backend-ui-e2e.mjs | 包装器读 json 计数；本席未跑（会起后端/测库） | 技术性 |
| 078 | verify:android-p0-smoke-wrapper | verify-android-p0-smoke-wrapper.mjs | 文件存在 | 技术性 |
| 079 | verify:full-admin-matrix | verify-full-admin-page-matrix.mjs | 文件存在 | 技术性 |
| 080 | test:unit | npm run test:unit --workspace apps/web | web 脚本存在 | 技术性 |
| 081 | verify:unit | 同 080 | 被 delivery-gates 引用 | 技术性 |
| 082 | docs:domain-ownership-scope | generate-domain-ownership-scope-audit.mjs | 未跑生成 | 技术性 |
| 083 | verify:domain-ownership-scope | 同上 --check | 标志在源 | 技术性 |
| 084 | verify:matrix-vs-migration | verify-matrix-vs-migration.mjs | 文件存在 | 技术性 |
| 085 | verify:migration-version-order | verify-migration-version-order.mjs | 在 delivery-gates 链内，不在 predeploy 链；与 AGENTS.md 一致 | 技术性 |
| 086 | verify:tenant-isolation | verify-tenant-isolation.mjs | 工具入口名，非新业务实体 | 技术性 |
| 087 | verify:predeploy | html/ledger/矩阵/契约/安全/pii/test-db 等串联 | CI 与 ops/160/propose-change.sh 调用；本席未跑全链 | 技术性 |
| 088 | verify:pii-encryption | verify-pii-encryption.mjs | 工具入口名 | 技术性 |
| 089 | verify:test-db-isolation | verify-test-db-isolation.mjs | 工具入口名 | 技术性 |
| 090 | docs:production-closure-gap-reports | generate-production-closure-gap-report.mjs | 未跑生成 | 技术性 |
| 091 | verify:production-closure-gap-reports | --check && verify-production-closure-gap-reports.mjs | 双部落点存在 | 技术性 |
| 092 | verify:final-closed-loop-audit | verify-final-closed-loop-audit.mjs | 文件存在 | 技术性 |
| 093 | verify:production-evidence-negative-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 094 | docs:nas-current-state-drift | generate-nas-current-state-drift-report.mjs | 未跑生成 | 技术性 |
| 095 | verify:nas-current-state-drift | 同上 --check | 标志在源 | 技术性 |
| 096 | docs:closed-loop-current-state | generate-closed-loop-current-state.mjs | 未跑生成 | 技术性 |
| 097 | verify:closed-loop-current-state | 同上 --verify | 标志在源 | 技术性 |
| 098 | verify:closed-loop-runtime | test:backend && typecheck && build && verify:ui && verify:ui:real-backend:seeded | 含完整 UI/真后端 E2E；未跑 | 技术性 |
| 099 | verify:nas-it-handoff | verify-nas-it-handoff.mjs | 文件存在 | 技术性 |
| 100 | docs:external-evidence-requirement-traceability | generate-…mjs | 未跑生成 | 技术性 |
| 101 | verify:external-evidence-requirement-traceability | 同上 --check | 标志在源 | 技术性 |
| 102 | docs:nas-diagnostics | generate-nas-diagnostic-report.mjs | 未跑生成 | 技术性 |
| 103 | verify:nas-diagnostics | 同上 --check | 标志在源 | 技术性 |
| 104 | docs:production-evidence-p0-quickstart | generate-production-evidence-p0-quickstart.mjs | 未跑生成 | 技术性 |
| 105 | verify:production-evidence-p0-quickstart | 同上 --check | 标志在源 | 技术性 |
| 106 | docs:runtime-acceptance-evidence | generate-runtime-acceptance-evidence.mjs | 未跑生成 | 技术性 |
| 107 | verify:runtime-acceptance-evidence | 同上 --check | 标志在源 | 技术性 |
| 108 | docs:ui-experience-acceptance | generate-ui-experience-acceptance-report.mjs | 未跑生成 | 技术性 |
| 109 | verify:ui-experience-acceptance | 同上 --check | 标志在源 | 技术性 |
| 110 | docs:production-evidence-owner-execution-packets | generate-…mjs | 未跑生成 | 技术性 |
| 111 | verify:production-evidence-owner-execution-packets | 同上 --check | 标志在源 | 技术性 |
| 112 | prepare:production-evidence-drafts | prepare-production-evidence-manifest-drafts.mjs | 未跑（写草稿） | 技术性 |
| 113 | verify:production-evidence-draft-prep | 同上 --check | 标志在源 | 技术性 |
| 114 | verify:production-evidence-draft-prep-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 115 | verify:production-evidence-positive-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 116 | verify:production-evidence-positive-propagation-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 117 | verify:production-ready-no-go-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 118 | verify:production-blocker-register-no-go-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 119 | verify:production-ready-positive-fixtures | 对应 mjs | 文件存在 | 技术性 |
| 120 | verify:production-evidence-crosslinks | 对应 mjs | 文件存在 | 技术性 |
| 121 | docs:partial-requirement-mock-coverage | generate-partial-requirement-mock-coverage.mjs | 未跑生成 | 技术性 |
| 122 | verify:partial-requirement-mock-coverage | 同上 --check | 标志在源 | 技术性 |
| 123 | docs:production-no-go-command-center | generate-production-no-go-resolution-command-center.mjs | 未跑生成 | 技术性 |
| 124 | verify:production-no-go-command-center | 同上 --check | 标志在源 | 技术性 |
| 125 | docs:integration-capability-switches | generate-integration-capability-switches.mjs | 未跑生成 | 技术性 |
| 126 | verify:integration-capability-switches | 同上 --check | 标志在源 | 技术性 |
| 127 | docs:data-isolation-spec | generate-data-isolation-spec.mjs | 未跑生成 | 技术性 |
| 128 | verify:data-isolation-spec | 同上 --check | 标志在源 | 技术性 |

调用闭包（窄读，不等于审那些模块）：`.github/workflows/predeploy-gates.yml` 跑 `npm ci --ignore-scripts` + `npm run verify:predeploy`；`ops/160/propose-change.sh` 跑 `verify:predeploy`；`ops/160/complex-verify-p0.sh` / `complex-deploy-*.sh` / `complex-verify-be000.sh` 引用本文件已有脚本名。

### 2.3 playwright.config.ts 全部 25 行

| 行 | 字段/语句 | 实核 |
|---|---|---|
| 1 | `import { defineConfig, devices } from "@playwright/test"` | 包 1.61.0 存在 |
| 3–25 | `export default defineConfig({...})` | `node` ESM import 得到对象，exit 0 |
| 4 | testDir `./tests/e2e` | 目录存在，10 个 spec |
| 5 | testIgnore `web-ui-real-backend.spec.ts` | `--list` 9 files / 47 tests，该文件不在列表 |
| 6 | globalSetup `./tests/e2e/support/global-setup.ts` | 文件存在；内部 catch 吞掉 warmup 失败（他模块，见未验证前提） |
| 7 | timeout 30_000 | import 值为 30000 |
| 8 | expect.timeout 8_000 | import 值为 8000 |
| 9 | fullyParallel false | import 值为 false |
| 10 | reporter list + html `playwright-report` open never | import 一致 |
| 12 | baseURL `http://127.0.0.1:5173` | 与 web `dev` 端口 5173 一致 |
| 13–14 | trace/screenshot retain/only-on-failure | import 一致 |
| 17 | webServer.command `npm run dev:web` | 展开为 `vite --host 0.0.0.0 --port 5173`（见 GR-02） |
| 18 | webServer.url `http://127.0.0.1:5173` | 与 baseURL 同 |
| 19 | reuseExistingServer `!process.env.CI` | 本会话 `CI=true` 时 import 值为 false |
| 20 | webServer.timeout 120_000 | import 120000 |
| 22–24 | projects chromium / devices["Desktop Chrome"] | import projects=`["chromium"]`；Playwright 1.61 设备表含 Desktop Chrome |

`--list` 收集到的 9 个 spec：`customer-account-lifecycle-live-evidence`、`customer-journey`、`full-admin-mvp`、`full-admin-remaining`、`full-admin-ui-hardening`、`full-admin-visual`、`leasing-contract-live-evidence`、`order-tickets-live-evidence`、`property-fee-evidence`。其中 3 个 live-evidence 在 `REAL_BACKEND_E2E!=="1"` 时 skip；其余 mock 用例无该 skip。默认套件不是全 skip。

### 2.4 playwright.real-backend.config.ts 全部 38 行

| 行 | 字段/语句 | 实核 |
|---|---|---|
| 1 | 同 Playwright import | 同上 |
| 3–5 | REAL_BACKEND_BASE_URL 默认 `http://127.0.0.1:8080`；WEB_PORT 默认 `5174`；webBaseUrl 拼接 | 源码字面量 |
| 8 | testDir `./tests/e2e` | 同默认 |
| 9 | testMatch `web-ui-real-backend.spec.ts` | `--list --config=...`：3 tests in 1 file |
| 10–11 | timeout 45_000 / expect 10_000 | 源码 |
| 12 | fullyParallel false | 单文件 3 用例串行 |
| 13–18 | 注释 AUD-022 + reporter list/html/json | json `outputFile: test-results/real-backend-results.json`；**Playwright 退出码不读该文件** |
| 20–24 | use.baseURL=webBaseUrl；trace/screenshot | 默认端口 5174 |
| 26–34 | webServer：`--host 127.0.0.1 --port ${webPort} --strictPort`；reuseExistingServer false；VITE_API_PROXY_TARGET=backendBaseUrl | 与默认配置的 0.0.0.0 不同；不启动 Java 后端 |
| 35–37 | projects chromium | `--list` 仅 chromium |

`tests/e2e/web-ui-real-backend.spec.ts` L40：`test.skip(process.env.REAL_BACKEND_E2E !== "1", ...)`。`package.json` 的 `verify:ui:real-backend` 会设该变量，skip 关闭；包装器再读 json 要求 expected≥1 且 skipped=0。

### 2.5 tsconfig.base.json 全部 13 行 / 全部 compilerOptions

| 键 | 值 | 实核 |
|---|---|---|
| target | ES2022 | `tsc -p tsconfig.base.json --showConfig` → es2022 |
| module | ESNext | → esnext |
| moduleResolution | Bundler | → bundler |
| strict | true | 展开 noImplicitAny/strictNullChecks 等为 true |
| esModuleInterop | true | showConfig 真 |
| forceConsistentCasingInFileNames | true | showConfig 真 |
| skipLibCheck | true | showConfig 真；TS 手册：跳过 .d.ts 检查 |
| resolveJsonModule | true | showConfig 真 |
| noUncheckedIndexedAccess | true | showConfig 真 |

无 `files`/`include`/`exclude`：单独 `tsc -p tsconfig.base.json --showConfig` 会按默认 glob 把根目录 TS（含两份 Playwright 配置与 `apps/web` 源）列入 `files`。约定消费者是 `apps/web/tsconfig.json`：`"extends": "../../tsconfig.base.json"`，自带 `include: ["src/**/*.ts","src/**/*.tsx"]` 与 `jsx: react-jsx`。`tsc -p apps/web/tsconfig.json --showConfig` exit 0，继承上述 9 项。

---

## 3. GEN-001 … GEN-018 逐项裁决

| ID | 裁决 | 证据 | 依据类型 |
|---|---|---|---|
| GEN-001 | **FAIL**（GR-03） | `smoke:p0-ops` 与 `smoke:p0`/`test:backend` 同体且无调用方；其余别名 `dev`/`dev:api`/`smoke:p0` 被 backend README 当作防回流入口，保留 | 4 |
| GEN-002 | **FAIL**（GR-01） | 声明入口 `verify:ui:real-backend` 设了 env，会真正跑 3 条；**配置文件作为 Playwright 入口**在未设 env 时 3 skip + exit 0 + last-run `passed`。`--list` 两配置均可加载。128 脚本落点存在。`verify:backend-env` 因 JAVA_HOME 失效显式 exit 1（环境，非脚本撒谎） | 2+4 |
| GEN-003 | **FAIL**（GR-01） | 真后端 spec 的 skip 与本配置 `testMatch` 叠成「整次运行全 skip 仍绿」。json reporter 不改退出码。包装器才断言计数。默认配置 47/9，不是全 skip | 2+3+4 |
| GEN-004 | **FAIL**（GR-02） | 默认 e2e webServer 继承 `vite --host 0.0.0.0`；同模块真后端配置已把 host 钉死 127.0.0.1。环境变量 URL/端口有默认值，无格式校验（默认字面量，非用户输入解析器） | 4 |
| GEN-005 | PASS（本模块范围内） | 四文件不含授权逻辑。真后端 REAL-UI-03 在他模块 spec 内断言 401/403；配置未把受保护操作改成仅前端隐藏 | 4 |
| GEN-006 | N/A→PASS | 无状态机/金额写入；脚本只调度 | — |
| GEN-007 | N/A→PASS | 无并发写路径；`fullyParallel: false` | 4 |
| GEN-008 | PASS | 5173=web dev 端口；5174 与 5173 错开；`VITE_API_PROXY_TARGET` 传入 Vite 已有 `server.proxy["/api"]`；workspaces 仅 apps/web；type=module 与 TS import 一致 | 4 |
| GEN-009 | **FAIL**（GR-01） | 全 skip 被记 `passed`/`failedTests:[]`，属伪成功。`verify:backend-env` 对坏 JAVA_HOME 显式失败，此项不记缺陷 | 2+4 |
| GEN-010 | **FAIL**（GR-02） | 默认 e2e 把带默认口令的管理端开发服务器绑到 0.0.0.0。四文件无密钥明文。`private: true` | 4 |
| GEN-011 | 部分 FAIL | 真后端配置写出 json 供包装器核对（设计正确）；配置自身退出码与 last-run 仍报 passed（GR-01） | 4 |
| GEN-012 | PASS | timeout/expect/webServer timeout 有上限；无本模块内无界循环 | 4 |
| GEN-013 | PASS（环境失败单列） | engines/packageManager/CI node 22 声明清楚；corepack 能给出 npm 10.9.8；`verify:backend-env` 失败显式。宿主 npm 11.19.0 与 JAVA_HOME 指向不存在目录属环境，不改脚本字节 | 2+4 |
| GEN-014 | N/A→PASS | 无迁移 SQL；仅有 `verify:migration-version-order` 工具入口且文件存在 | 4 |
| GEN-015 | N/A→PASS | 无 UI 文案；仅提供 chromium E2E 入口。展示中文在 spec/页面（他模块） | 4 |
| GEN-016 | PASS | 配置注释声称 json 供 `run-real-backend-ui-e2e.mjs` 核对：包装器 L68–93、L170–178 确实读取该路径并断言 skipped=0。注释未声称 Playwright 自身会因 skip 非 0 | 4 |
| GEN-017 | PASS | 四文件按冲突→细化→技术性→添加排序后全部技术性；无业务发明（见 §6） | 1+4 |
| GEN-018 | PASS（本席过程） | 结论落到命令输出/官方行为/HTML 无矛盾；不把测试绿当唯一证据 | 4 |

---

## 4. 三线（每条覆盖全部四文件）

### 4.1 第一性原理

**为什么必须存在？**

1. `package.json`：npm workspaces 根清单、脚本调度、engines/packageManager、Playwright/yaml 依赖。删掉则 CI `npm ci`、`verify:predeploy`、web 工作区与全部 `npm run verify:*` 无入口。三份 HTML 无「必须有根 package.json」原文；删掉也不使某一 HTML 业务句失效，但构建/测试/观测无法运行 → **技术性**（无业务发明）。
2. `playwright.config.ts`：默认 mock UI E2E 入口。HTML 无 Playwright 锚点。服务「能被测试」。
3. `playwright.real-backend.config.ts`：与 mock 套件隔离的真后端 UI 入口（独立端口、不 reuse、json 计数）。服务测试有效性（AUD-022 意图）。
4. `tsconfig.base.json`：web `tsc -b` 的共享 compilerOptions。删掉则 `apps/web/tsconfig.json` extends 断裂。

**声称行为是否执行？**

- 脚本落点 75/75 存在；标志能在工具源找到；`verify:no-legacy-api-prototype` **PASS**。
- Playwright 两配置 `--list` 均可收集测试。
- **未**执行默认 47 条 UI E2E、未执行 seeded 真后端、未执行全量 docs 生成/部署/DB E2E。
- `verify:backend-env` **exit 1**（JAVA_HOME 目录不存在）；环境失败如实记录。
- 真后端配置在未设 `REAL_BACKEND_E2E` 时：3 skipped / 0 expected / **exit 0** / last-run `passed`。

**未验证就当真的前提：**

- `global-setup.ts` 吞掉 warmup 失败后，默认套件仍能靠后续用例红/绿（他文件）。
- `verify:ui:real-backend:seeded` 包装器在官方路径上堵住 skip（本席未跑包装器，只读源码 L68–178）。
- 宿主 npm 11 与声明 npm 10.9.8 对根清单解析相容（不把 M004 锁审计当证据）。
- `CI=true` 在本会话为真，影响 `reuseExistingServer`。

### 4.2 顺从式

假定实现意图正确，按冻结 18 条核对：

- 输入：env 默认值、workspaces、脚本 argv。
- 状态：无业务状态机。
- 输出：verify 脚本非 0 失败；Playwright 对 skip 不失败（与官方文档一致，故真后端官方路径才外挂计数器）。
- 错误：legacy 门禁 PASS；backend-env 失败显式。
- 权限：本模块无授权实现。
- 持久化：无。
- 测试：默认配置能列出 47 条；真后端列出 3 条；配置层不能单独保证 3 条被执行。
- 文档：配置注释与包装器行为一致。

顺从式在 GEN-001/002/003/004/009/010 上不能给 PASS。

### 4.3 对抗式（grilling 自问，不派子代理）

1. **全 skip 是否仍绿？** 对真后端配置：是。命令见 §7。对默认配置：否（9 文件中多数不 skip）。
2. **官方 npm 脚本是否同一洞？** `verify:ui:real-backend` 前缀 `REAL_BACKEND_E2E=1`，走 assert 而非 skip；无后端应变红。`verify:ui:real-backend:seeded` 再读 json。洞在「只跑配置、不设 env」。
3. **json reporter 能否当闸？** 不能。本席加 `--reporter=json` 时甚至覆盖配置里的 file reporter，stdout 仍给出 skipped=3 且进程 0。包装器删旧 json 再读文件，是另一入口。
4. **0.0.0.0 是否第一次跑就发生？** `npm run verify:ui` → `npm run dev:web` → vite 0.0.0.0:5173。160 共享机上第一次跑就会绑所有接口。真后端配置已示范 127.0.0.1。
5. **脚本名是否发明业务？** 否。tenant/pii/android 等是已有工具文件的 npm 键。
6. **与 HTML 冲突？** 三份 HTML 无 package.json/playwright/tsconfig 锚点；enum 里「测试」指交易样本排除，不涉及本模块。无冲突。
7. **android:p0:build 是否指错目录？** `--project apps/android-terminal` 对 Kotlin 核心；apk/env 对 `android-app`。两树都有 `settings.gradle.kts`。不是断链。
8. **Windows 环境变量前缀？** 正本在 160 Linux；不记缺陷。

ponytail-review：`package.json:48: yagni: smoke:p0-ops 与 smoke:p0 同体且无调用方. 删除该键.` `net: -1 lines possible.`

---

## 5. 临时 findings

### GR-01 — 真后端 Playwright 配置在未设 REAL_BACKEND_E2E 时全 skip 仍 passed

- 文件:行号：`playwright.real-backend.config.ts:9-18` 叠 `tests/e2e/web-ui-real-backend.spec.ts:40`（窄读 skip 条件）
- 2.1 分类：**技术性**（测试配置；无业务发明）
- 工程缺陷：**成立**
- GEN：GEN-002、GEN-003、GEN-009、GEN-011
- 依据类型：2（可执行产物）+ 3（Playwright：全 skip 退出 0）+ 4（命令）
- 证据：

```
env -u REAL_BACKEND_E2E -u CI npx playwright test --config=playwright.real-backend.config.ts --reporter=json
EXIT=0
stats: expected=0, skipped=3, unexpected=0, flaky=0, duration≈784ms
test-results/.last-run.json: {"status":"passed","failedTests":[]}
```

`--list --config=playwright.real-backend.config.ts` 列出 REAL-UI-01/02/03 共 3 条。配置 json reporter 注释把计数责任交给包装器；**配置自己的进程退出码不消费该 json**。`npm run verify:ui:real-backend` 设 env 后不再走 skip；`verify:ui:real-backend:seeded` 另有 `assertRealBackendE2eActuallyRan`。本 finding 针对本模块配置作为独立 Playwright 入口时的伪成功。

反例产物已删（`.last-run.json`、json 文件、html 报告）。未改源。

### GR-02 — 默认 E2E webServer 绑定 0.0.0.0

- 文件:行号：`playwright.config.ts:17-18`；展开调用 `apps/web/package.json` scripts.dev = `vite --host 0.0.0.0 --port 5173`（窄读）
- 对照：`playwright.real-backend.config.ts:27` 使用 `--host 127.0.0.1 --strictPort`
- 2.1 分类：**技术性**
- 工程缺陷：**成立**
- GEN：GEN-004、GEN-010
- 依据类型：4
- 证据：两配置源码 + web 脚本字面量。第一次执行 `npm run verify:ui`/`test:ui` 即按 0.0.0.0 起管理端开发服务器（默认测试账号在 spec/global-setup，他文件）。不是「要多件巧合」。

### GR-03 — smoke:p0-ops 纯转发重复键

- 文件:行号：`package.json:48`（及 47 同体 `smoke:p0`）
- 2.1 分类：**技术性**
- 工程缺陷：**成立**（GEN-001 重复/纯转发；无调用方）
- GEN：GEN-001
- 依据类型：4
- 证据：脚本体均为 `npm run test:backend`。源文件检索 `smoke:p0-ops` 仅出现在 `package.json`。`smoke:p0`/`dev:api` 被 backend README 点名为防回流入口，不列入本条。

无其它 GR。无添加项，不写 questions。无冲突项。

---

## 6. 四分类与无业务发明

判定顺序：冲突 → 细化 → 技术性 → 添加。

- **冲突**：无。三份 HTML 检索无 Playwright / 根 package.json / tsconfig.base 条目；enum「测试」指交易样本排除，与 npm 脚本无关。
- **细化**：无。不能把「工具入口名叫租户隔离」指到治理 HTML「商户和主体」当下属实现——那会把调度键发明成业务功能。
- **技术性**：四文件全部字段与 128 个 scripts。纯 npm 脚本 / Playwright 配置 / compilerOptions **不因缺 HTML 锚点变成添加**。
- **添加**：无。未推定新业务实体、枚举、状态、权限、价格、流程。`verify:tenant-isolation`、`verify:pii-encryption`、`android:p0:*` 等是已有 `tools/*.mjs` / Gradle 工程的入口名。

无业务发明说明：根清单只声明如何安装、调度验证、启动开发服务器、共享 TS 选项。业务规则若存在，在被调工具与测试文件里，不在本模块四文件中新造。

---

## 7. 命令真实 exit / 输出（节选）

| 命令 | exit | 关键输出 |
|---|---:|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| 四文件 `sha256sum` + `wc -l` | 0 | 与 §1 表全等 |
| `git rev-parse HEAD` | 0 | `134d75d158ca6ff1343f616cce47676ecfc83e02` |
| `node` 解析 package.json / 脚本闭包 | 0 | SCRIPT_COUNT=128；TOOL_MISSING=NONE；DANGLING=NONE |
| `node tools/verify-no-legacy-api-prototype.mjs` | 0 | `NO_LEGACY_API_PROTOTYPE_PASS` |
| `npx playwright test --list` | 0 | `Total: 47 tests in 9 files` |
| `npx playwright test --list --config=playwright.real-backend.config.ts` | 0 | `Total: 3 tests in 1 file` |
| ESM import `playwright.config.ts` | 0 | 见 §2.3 对象 |
| `npx tsc -p tsconfig.base.json --showConfig` | 0 | 9 项 compilerOptions + 默认 files glob |
| `npx tsc -p apps/web/tsconfig.json --showConfig` | 0 | extends 生效 |
| `node -e require("@playwright/test/package.json")` | 0 | `1.61.0` |
| `node -e require("yaml/package.json")` | 0 | `2.9.0` |
| `corepack npm -v` | 0 | `10.9.8` |
| `node -v` / `npm -v` | 0 | `v24.20.0` / `11.19.0` |
| skip-all 真后端配置（§5 GR-01） | **0** | skipped=3 expected=0；last-run passed |
| `npm run verify:backend-env` | **1** | Maven：`JAVA_HOME environment variable is not defined correctly`；`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` **目录不存在**；`java -version` 仍为 21.0.12.1（PATH） |

未跑：`verify:predeploy` / `verify:delivery-gates` / `verify:p0` / `verify:ui` 实执行 / `verify:ui:real-backend:seeded` / 任何 `docs:*` 生成 / 部署 / 完整 DB E2E。未改主 `node_modules`、未改版本、未动生产进程。

---

## 8. 局限

- 本会话 `CI=true`，默认配置 import 时 `reuseExistingServer===false`。skip-all 反例使用了 `env -u CI`。
- JAVA_HOME 指向不存在路径，backend-env 失败；未为通过而改环境。
- 未跑默认 47 条 E2E（会起 0.0.0.0:5173 的完整 UI）。用 `--list` + 源码 skip 条件代替「套件已绿」。
- 未跑 seeded 真后端（会打包 jar、起 18080、写测库）。包装器断言只做源码阅读。
- 检索 `smoke:p0` 时工具列出 `M005-b1-self-raw.md` 三行索引，未打开该文件其余正文，不采信。
- 未把 M004 / findings 台账当证据。
- `batch-grill-me`、`ubiquitous-language` 技能缺口。

---

## 9. 结论

**FAIL**

三线已覆盖四文件 223 行与 128 个 scripts、18 条 GEN。存在 GR-01/02/03 工程缺陷。无添加项、无 HTML 冲突、无业务发明。B1 结束；不修源、不进 B2。
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M005 B1 Grok 独立审计报告

- 版本：V0.02（本席本次完整报告；前缀 36359 字节未读、不采信）
- 席位：Grok（审计席乙）
- 步骤：B1 修复前全模块三线审计
- 模块：M005 `01-root-config-and-guidance-03` / 共 322
- 控制：V0.17
- ledger 本模块（只取状态/计数）：IN_PROGRESS；B3=0；方案复核已修=2；子代理失败重跑=0
- ledger 全局基准：CRITERIA-LOCK-SHA256 与 sidecar、本席实算三值相等；LOCK-AMEND-COUNT=0
- HEAD：`134d75d158ca6ff1343f616cce47676ecfc83e02`（`main`）
- 结论：**FAIL**（GR-01 工程缺陷成立；无未定稿缺陷不得 PASS）
- 本席未修源、未问人、未进 B2、未派子代理、未读 self-raw / 历史 raw / findings / questions

## 0. 技能预检

已实际读取：`using-superpowers`、`grilling`、`grill-me`（指向 grilling）、`codebase-design`、`diagnosing-bugs`、`systematic-debugging`、`verification-before-completion`、`ponytail-review`。

按 §6 收窄：grilling / grill-me 只借追问结构，事实自读，不派子代理、不等操作员；systematic-debugging 不改生产代码；verification-before-completion 在宣称前先跑命令。

存在但本步不适用：`tdd` / `test-driven-development`（B1 禁止修复）、`resolving-merge-conflicts`、`writing-for-agents`（本报告即给下游席阅读）、`ponytail`（B3 才用）、`git-guardrails-claude-code`（未推送）、`i-have-adhd`（目录存在于 `/home/james/.claude/plugins/cache/i-have-adhd`）。

缺口（不冒充已调用）：`batch-grill-me` 不存在；`ubiquitous-language` 在本席 mattpocock 技能树中无独立目录。

## 1. 检索边界与曝光披露

实际使用的检索边界：

1. 终端 Python / `git` / `sha256` / `npx` 等价二进制，只读明确路径。
2. **未使用**仓库语义搜索、代码索引搜索、全库 search 工具、MCP 代码索引。
3. 调用闭包：`git ls-files` / `git ls-files -z` 只取文件名 → Python 过滤 `docs/audit-2026-09/`、`.scratch/`、`docs/_archive/`、`*-raw.md` → 对余下文本路径做子串扫描（等价逐文件 rg）。排除文件内容未进入命中。
4. 允许窄读的直接调用者/被调用者/消费者：`apps/web/{package.json,tsconfig.json,vite.config.ts,vitest.config.ts}`、`tests/e2e/*.ts`（含 skip 门与 fixture）、`tools/run-real-backend-ui-e2e.mjs`、`.github/workflows/predeploy-gates.yml`、`ops/ci/predeploy-gates.yml`、`ops/160` 已枚举脚本中含 `npm ` 的行、`package-lock.json` 的 playwright/yaml 版本、`node_modules/vite/dist/node/cli.js` 的 `filterDuplicateOptions`、已安装 `@playwright/test`/`yaml` 的 `package.json` version。
5. 三份业务 HTML 只抽 `h2`/`h3`/`td.k` 计数与「系统交付与上线」「系统通用能力」正文对照分类，不把 HTML 当 M005 实现。
6. `criteria-lock.md` 只取 `## general` GEN-001–018 与 `M005 → [GEN-001…GEN-018]` 映射行。`runbook.md` 全文 103 行用于三线口径。
7. ledger：第 1–2 行 + M005 表字段；长备注格曾因短于 80 字符被打印出来（见下条），**不作为 finding 证据**。

**曝光披露（不能以「不采信」假装没看见）：**

- **未读**任何 `*-raw.md` 正文（含本文件前缀 36359 字节只 stat + 末字节 `\n`）、未读 `M005-b1-self-raw`、未读 M004 任何 raw/findings、未读 `findings.md` / `questions/`。
- **未见**其他席 B1 分析正文或代码索引片段。
- **见过** ledger M005 行一个短备注格：`V0.17；B1外席检索暴露本方报告索引，原票不采信，原本方独立票保留；新进程重审同阶段`。这是台账过程句，不是源码证据，本席 findings 不引用它。
- 首次非 `-z` 的 `git ls-files` 对两个 `docs/proposals/` 中文路径产生引号转义 `READ_FAIL`（文件未打开）。随后 `git ls-files -z` 2940 条 kept 路径扫描成功。
- 误执行过 `corepack prepare npm@10.9.8 --activate`（本拟查询、命令写进了 `--activate`）。事后核验：`/bin/npm` 仍为 **11.19.0**，仓库 `node_modules` / lock 未改；corepack 缓存 `lastKnownGood.json` 现含 `"npm": "10.9.8"`。未再跑 corepack。本环境 npm 主路径未被切换。

临时反例：`.scratch/m005-b1-skip-probe/` 已删除；探测产生的 `test-results/.last-run.json`（`"status":"passed"`，实为 15 skipped）已删除，空目录已 rmdir。

## 2. 本席实算指纹

| 对象 | 值 |
|---|---|
| criteria-lock.md SHA256（本席） | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| criteria-lock.sha256 sidecar | 同上 |
| ledger 顶部 CRITERIA-LOCK-SHA256 | 同上 |
| LOCK-AMEND-COUNT | 0 |
| package.json | 147 行 / SHA `f59711d7579178d2772ec7456592d616d41ac3d5980e58864aeaf66c2e60b7db` |
| playwright.config.ts | 25 行 / SHA `f389c55d86716b5bd921ea60320c74936a72e72b4e54a8df6fae9eca6db86e19` |
| playwright.real-backend.config.ts | 38 行 / SHA `cf7b020c030dbd8ad4aeba54fe331528cd0743c59c22cbd03bdb884fe187afa2` |
| tsconfig.base.json | 13 行 / SHA `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db` |
| 四文件合计 | **223 行**（`wc -l` 换行计数，与任务给定一致） |
| 映射 | `M005 → [GEN-001 … GEN-018]`（criteria-lock.md L6385） |

终审前重新实算四文件 SHA 与 lock SHA，与开工时一致。

## 3. 四文件全量覆盖表

### 3.1 package.json（L1–147，全部顶层字段 + 全部 128 个 scripts）

顶层字段（无未知键、无重复键；`json.loads` 成功）：

| 行 | 字段 | 值 | 三线覆盖 |
|---|---|---|---|
| L2 | name | entertainment-complex-management | 技术性项目名，非业务发明 |
| L3 | private | true | 阻止误发布 |
| L4 | version | 0.1.0 | 包版本 |
| L5 | type | module | ESM；tools 均为 `.mjs` |
| L6–8 | workspaces | ["apps/web"] | 唯一 npm workspace |
| L9–138 | scripts | 128 条，见下表 | 逐条存在性已核 |
| L139 | packageManager | npm@10.9.8 | 声明钉；本机 `/bin/npm` 为 11.19.0（未强制） |
| L140–142 | engines.node | >=22 | 本机 node v24.20.0 满足；CI `node-version: "22"` |
| L143–146 | devDependencies | @playwright/test ^1.61.0；yaml ^2.9.0 | lock 钉 1.61.0 / 2.9.0；已安装同版本 |

scripts 全表（S001–S128；命令值为 package.json 原文；被调 `tools/*.mjs` 75 个全部存在且 git 跟踪；`npm run X` 引用全部能在本 scripts 对象解析）：

| # | name | command |
|---|---|---|
| 1 | dev | npm run dev:backend |
| 2 | dev:backend | node tools/run-backend-maven.mjs spring-boot:run |
| 3 | test:backend | node tools/run-backend-maven.mjs test |
| 4 | verify:backend-env | node tools/run-backend-maven.mjs --check |
| 5 | dev:api | npm run dev:backend |
| 6 | dev:web | npm run dev --workspace apps/web |
| 7 | verify:openapi | node tools/verify-openapi-contract.mjs |
| 8 | audit:client-routes | node tools/audit-client-api-routes.mjs |
| 9 | verify:client-routes | node tools/audit-client-api-routes.mjs --strict |
| 10 | audit:client-openapi | node tools/audit-client-openapi-coverage.mjs |
| 11 | verify:client-openapi | node tools/audit-client-openapi-coverage.mjs --strict |
| 12 | audit:client-payload | node tools/verify-client-payload-policy.mjs |
| 13 | verify:client-payload | node tools/verify-client-payload-policy.mjs --strict |
| 14 | docs:openapi-examples | node tools/apply-openapi-p0-examples.mjs |
| 15 | verify:openapi-examples | node tools/verify-openapi-p0-examples.mjs |
| 16 | docs:operation-matrix | node tools/generate-openapi-operation-matrix.mjs |
| 17 | verify:operation-matrix | node tools/generate-openapi-operation-matrix.mjs --check |
| 18 | docs:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs |
| 19 | verify:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs --check |
| 20 | docs:security-baseline | node tools/generate-formal-security-baseline.mjs |
| 21 | docs:entry-baseline | node tools/refresh-entry-doc-baseline.mjs |
| 22 | verify:html-ledger-sync | node tools/verify-html-ledger-sync.mjs |
| 23 | verify:entry-anchors | node tools/verify-entry-anchors.mjs |
| 24 | verify:html-param-tier | node tools/verify-html-param-tier.mjs |
| 25 | verify:entry-doc-baseline | node tools/refresh-entry-doc-baseline.mjs --check |
| 26 | verify:security-baseline | npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict |
| 27 | verify:prod-docs-access | node tools/verify-prod-docs-access-policy.mjs |
| 28 | verify:ops160-execution-policy | node tools/verify-ops160-execution-policy.mjs |
| 29 | verify:ops160-backup-restore | node tools/verify-160-backup-restore-scripts.mjs |
| 30 | verify:production-credentials | node tools/verify-production-credential-rotation-ledger.mjs |
| 31 | verify:p0a-ops-hardening | node tools/verify-p0a-ops-hardening.mjs |
| 32 | verify:ops160-recovery | 串联 coding-host-protection、ops160-execution-policy、ops160-backup-restore、production-credentials、p0a-ops-hardening、nas-smb-mount-evidence |
| 33 | verify:p0-contract | 串联 no-legacy-api-prototype、openapi、openapi-examples、operation-matrix、openapi-closure、client-routes、client-openapi、client-payload |
| 34 | verify:p0-web | verify:p0-contract && build |
| 35 | verify:p0-env | verify:backend-env && verify:android-env |
| 36 | build | npm run build --workspace apps/web |
| 37 | typecheck | npm run typecheck --workspace apps/web |
| 38 | smoke:p0 | npm run test:backend |
| 39 | smoke:p0-ops | npm run test:backend |
| 40 | verify:android-env | node tools/run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app |
| 41 | android:p0:build | node tools/run-android-gradle.mjs --project apps/android-terminal clean build |
| 42 | android:p0:smoke | node tools/run-android-p0-smoke.mjs |
| 43 | android:p0:apk | node tools/run-android-gradle.mjs --project apps/android-terminal/android-app --require-android-sdk :app:assembleDebug |
| 44 | verify:p0 | coding-host-protection + p0-contract + security-baseline + test:backend + build + android-env + android:p0:build + android-p0-smoke-wrapper + android:p0:smoke + android:p0:apk |
| 45 | verify:no-legacy-api-prototype | node tools/verify-no-legacy-api-prototype.mjs |
| 46 | verify:coding-host-protection | node tools/verify-coding-host-protection.mjs |
| 47 | verify:t3-coding-package | node tools/verify-t3-coding-package.mjs |
| 48 | verify:t3b-supplier-mock-kit | node tools/verify-t3b-supplier-mock-kit.mjs |
| 49 | verify:t3d-local-llm-ai-quality | node tools/verify-t3d-local-llm-ai-quality.mjs |
| 50 | verify:external-dependency-gates | node tools/verify-external-dependency-gates.mjs |
| 51 | verify:delivery-gates-docs | node tools/verify-delivery-gates.mjs |
| 52 | verify:delivery-gates | 长串联（含 migration-version-order、tenant-isolation、unit、p0-contract、security-baseline、大量 production-evidence:* 与 nas:*）；L61 单行 JSON，无断行 |
| 53 | verify:nas-smb-mount-evidence | node tools/verify-nas-smb-mount-evidence.mjs |
| 54 | verify:production-readiness-evidence | node tools/verify-production-readiness-evidence.mjs |
| 55 | docs:production-blockers | node tools/generate-production-blocker-register.mjs |
| 56 | verify:production-blockers | 同上 --check |
| 57 | verify:production-evidence-intake | node tools/verify-production-evidence-intake-templates.mjs |
| 58 | docs:production-owner-actions | node tools/generate-production-owner-action-packs.mjs |
| 59 | verify:production-owner-actions | 同上 --check |
| 60 | docs:production-evidence-submission-kit | node tools/generate-production-evidence-submission-kit.mjs |
| 61 | verify:production-evidence-submission-kit | 同上 --check |
| 62 | docs:production-evidence-submission-status | node tools/generate-production-evidence-submission-status.mjs |
| 63 | verify:production-evidence-submission-status | 同上 --check |
| 64 | docs:production-evidence-manifest-templates | node tools/generate-production-evidence-manifest-templates.mjs |
| 65 | verify:production-evidence-manifest-templates | 同上 --check |
| 66 | docs:production-evidence-owner-guides | node tools/generate-production-evidence-owner-guides.mjs |
| 67 | verify:production-evidence-owner-guides | 同上 --check |
| 68 | verify:production-evidence-submissions-guard | node tools/verify-production-evidence-submissions-guard.mjs |
| 69 | docs:production-evidence-backfill-priority | node tools/generate-production-evidence-backfill-priority.mjs |
| 70 | verify:production-evidence-backfill-priority | 同上 --check |
| 71 | docs:production-evidence-backfill-execution-ledger | node tools/generate-production-evidence-backfill-execution-ledger.mjs |
| 72 | verify:production-evidence-backfill-execution-ledger | 同上 --check |
| 73 | test:ui | playwright test |
| 74 | verify:ui | playwright test |
| 75 | test:ui:update | playwright test --update-snapshots |
| 76 | verify:ui:real-backend | REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts |
| 77 | verify:ui:real-backend:seeded | node tools/run-real-backend-ui-e2e.mjs |
| 78 | verify:android-p0-smoke-wrapper | node tools/verify-android-p0-smoke-wrapper.mjs |
| 79 | verify:full-admin-matrix | node tools/verify-full-admin-page-matrix.mjs |
| 80 | test:unit | npm run test:unit --workspace apps/web |
| 81 | verify:unit | npm run test:unit --workspace apps/web |
| 82 | docs:domain-ownership-scope | node tools/generate-domain-ownership-scope-audit.mjs |
| 83 | verify:domain-ownership-scope | 同上 --check |
| 84 | verify:matrix-vs-migration | node tools/verify-matrix-vs-migration.mjs |
| 85 | verify:migration-version-order | node tools/verify-migration-version-order.mjs |
| 86 | verify:tenant-isolation | node tools/verify-tenant-isolation.mjs |
| 87 | verify:predeploy | html-ledger-sync + entry-anchors + html-param-tier + delivery-gates-docs + full-admin-matrix + domain-ownership-scope + matrix-vs-migration + data-isolation-spec + tenant-isolation + p0-contract + security-baseline + production-readiness-evidence + production-blockers + pii-encryption + test-db-isolation（**不含** UI E2E / 后端单测 / migration-version-order） |
| 88 | verify:pii-encryption | node tools/verify-pii-encryption.mjs |
| 89 | verify:test-db-isolation | node tools/verify-test-db-isolation.mjs |
| 90 | docs:production-closure-gap-reports | node tools/generate-production-closure-gap-report.mjs |
| 91 | verify:production-closure-gap-reports | generate --check && verify-production-closure-gap-reports.mjs |
| 92 | verify:final-closed-loop-audit | node tools/verify-final-closed-loop-audit.mjs |
| 93 | verify:production-evidence-negative-fixtures | node tools/verify-production-evidence-negative-fixtures.mjs |
| 94 | docs:nas-current-state-drift | node tools/generate-nas-current-state-drift-report.mjs |
| 95 | verify:nas-current-state-drift | 同上 --check |
| 96 | docs:closed-loop-current-state | node tools/generate-closed-loop-current-state.mjs |
| 97 | verify:closed-loop-current-state | 同上 --verify |
| 98 | verify:closed-loop-runtime | test:backend && typecheck --workspace apps/web && build --workspace apps/web && verify:ui && verify:ui:real-backend:seeded |
| 99 | verify:nas-it-handoff | node tools/verify-nas-it-handoff.mjs |
| 100 | docs:external-evidence-requirement-traceability | node tools/generate-external-evidence-requirement-traceability.mjs |
| 101 | verify:external-evidence-requirement-traceability | 同上 --check |
| 102 | docs:nas-diagnostics | node tools/generate-nas-diagnostic-report.mjs |
| 103 | verify:nas-diagnostics | 同上 --check |
| 104 | docs:production-evidence-p0-quickstart | node tools/generate-production-evidence-p0-quickstart.mjs |
| 105 | verify:production-evidence-p0-quickstart | 同上 --check |
| 106 | docs:runtime-acceptance-evidence | node tools/generate-runtime-acceptance-evidence.mjs |
| 107 | verify:runtime-acceptance-evidence | 同上 --check |
| 108 | docs:ui-experience-acceptance | node tools/generate-ui-experience-acceptance-report.mjs |
| 109 | verify:ui-experience-acceptance | 同上 --check |
| 110 | docs:production-evidence-owner-execution-packets | node tools/generate-production-evidence-owner-execution-packets.mjs |
| 111 | verify:production-evidence-owner-execution-packets | 同上 --check |
| 112 | prepare:production-evidence-drafts | node tools/prepare-production-evidence-manifest-drafts.mjs |
| 113 | verify:production-evidence-draft-prep | 同上 --check |
| 114 | verify:production-evidence-draft-prep-fixtures | node tools/verify-production-evidence-draft-prep-fixtures.mjs |
| 115 | verify:production-evidence-positive-fixtures | node tools/verify-production-evidence-positive-fixtures.mjs |
| 116 | verify:production-evidence-positive-propagation-fixtures | node tools/verify-production-evidence-positive-propagation-fixtures.mjs |
| 117 | verify:production-ready-no-go-fixtures | node tools/verify-production-ready-no-go-fixtures.mjs |
| 118 | verify:production-blocker-register-no-go-fixtures | node tools/verify-production-blocker-register-no-go-fixtures.mjs |
| 119 | verify:production-ready-positive-fixtures | node tools/verify-production-ready-positive-fixtures.mjs |
| 120 | verify:production-evidence-crosslinks | node tools/verify-production-evidence-crosslinks.mjs |
| 121 | docs:partial-requirement-mock-coverage | node tools/generate-partial-requirement-mock-coverage.mjs |
| 122 | verify:partial-requirement-mock-coverage | 同上 --check |
| 123 | docs:production-no-go-command-center | node tools/generate-production-no-go-resolution-command-center.mjs |
| 124 | verify:production-no-go-command-center | 同上 --check |
| 125 | docs:integration-capability-switches | node tools/generate-integration-capability-switches.mjs |
| 126 | verify:integration-capability-switches | 同上 --check |
| 127 | docs:data-isolation-spec | node tools/generate-data-isolation-spec.mjs |
| 128 | verify:data-isolation-spec | 同上 --check |

yaml 的直接消费者（窄读）：`tools/verify-openapi-contract.mjs`、`tools/apply-openapi-p0-examples.mjs` 的 `import YAML from "yaml"`。依赖非死。

`docs:*` 脚本会写文档，本席 **未执行**（有副作用全量 docs 生成，禁）。存在性与 `--check` 入口成对出现者已在表中登记。

### 3.2 playwright.config.ts（L1–25 逐行）

| 行 | 内容 | 覆盖 |
|---|---|---|
| L1 | import defineConfig, devices from @playwright/test | 已安装 1.61.0 |
| L2 | 空行 | — |
| L3–4 | testDir `./tests/e2e` | 目录存在，10 个 spec + support |
| L5 | testIgnore `web-ui-real-backend.spec.ts` | `--list` 确认该文件不在默认 47 条内 |
| L6 | globalSetup `./tests/e2e/support/global-setup.ts` | 文件存在；warmup mock 登录；失败只 warn |
| L7 | timeout 30000 | 有界 |
| L8 | expect.timeout 8000 | 有界 |
| L9 | fullyParallel false | 同文件串行；**未**设 workers=1，跨文件仍可并行 |
| L10 | reporter list + html `playwright-report` open never | .gitignore 有 playwright-report |
| L11–15 | use.baseURL 127.0.0.1:5173；trace/screenshot 仅失败 | 与 webServer.url 一致 |
| L16–21 | webServer: `npm run dev:web`，url 5173，reuseExistingServer=!CI，timeout 120s | apps/web `dev` 为 vite --host 0.0.0.0 --port 5173 |
| L22–24 | projects chromium Desktop Chrome | 唯一项目 |
| L25 | 结束 | — |

### 3.3 playwright.real-backend.config.ts（L1–38 逐行）

| 行 | 内容 | 覆盖 |
|---|---|---|
| L1 | 同 playwright import | |
| L3 | REAL_BACKEND_BASE_URL 默认 127.0.0.1:8080 | 与 vite 默认代理目标同形 |
| L4 | REAL_BACKEND_WEB_PORT 默认 5174 | 避开 5173 |
| L5 | webBaseUrl `http://127.0.0.1:${webPort}` | |
| L8 | testDir `./tests/e2e` | |
| L9 | testMatch **仅** `web-ui-real-backend.spec.ts` | `--list` Total: 3 tests in 1 file |
| L10–12 | timeout 45s / expect 10s / fullyParallel false | |
| L13–14 | 注释：json reporter 堵住全部 skip 仍退出 0（AUD-022） | 意图在注释；**本 config 的 npm 脚本不读 json**，读 json 的是 seeded runner |
| L15–19 | reporter list + html `playwright-report-real-backend` + json `test-results/real-backend-results.json` | |
| L20–24 | use.baseURL=webBaseUrl；trace/screenshot 仅失败 | |
| L25–34 | webServer: `npm --workspace apps/web run dev -- --host 127.0.0.1 --port ${webPort} --strictPort`；reuseExistingServer **false**；env VITE_API_PROXY_TARGET=backendBaseUrl | vite CLI `filterDuplicateOptions` 取数组最后一项 → host/port 以后追加值为准（见命令证据） |
| L35–37 | chromium Desktop Chrome | |
| L38 | 结束 | 无 globalSetup（与 mock 暖机分离，合理） |

### 3.4 tsconfig.base.json（L1–13 逐行）

| 行 | 字段 | 值 | 覆盖 |
|---|---|---|---|
| L2–12 | compilerOptions | target ES2022, module ESNext, moduleResolution Bundler, strict true, esModuleInterop, forceConsistentCasingInFileNames, skipLibCheck true, resolveJsonModule, noUncheckedIndexedAccess true | `tsc --showConfig -p tsconfig.base.json` exit 0，上述键均出现 |
| 消费者 | apps/web/tsconfig.json L2 | `"extends": "../../tsconfig.base.json"`，include 仅 `src/**/*.ts(x)` | `tsc --showConfig -p apps/web/tsconfig.json` exit 0，合并 jsx react-jsx + types vite/client |
| 注意 | 单独以 base 为项目根时 | showConfig 的 files 会扫到根下全部 TS（含 e2e）。`package.json` `typecheck` 走 workspace `tsc -b`，不以 base 为入口 | 前提：base 只被 extends，不是编译入口 |

## 4. 四分类（顺序：冲突 → 细化 → 技术性 → 添加）

四文件均为 **npm 脚本 / Playwright 测试配置 / TypeScript 编译器基配置**。

- **冲突**：与三份 HTML 明文无矛盾。HTML 无「必须用 Playwright」「必须叫 smoke:p0」之类条目。`name`/`scripts` 中的票务/酒吧/商户等字样只出现在**已有工具文件名与脚本入口**（如 `verify:tenant-isolation` → `tools/verify-tenant-isolation.mjs`），本席不把入口改判为新业务规则。
- **细化**：不能指向某一 `td.k` / 具体 `<h2>`/`<h3>` 作为「这些 npm 字段」的上位实现。最接近对照是业务系统图「系统交付与上线」（测试环境 vs 生产加固、安装包）和「系统通用能力」（配置存档删除）——均不定义根目录构建工具。故不归细化。
- **技术性**：无业务发明 + 无 HTML 冲突。归为技术性。**不因缺 HTML 锚点改判添加。**
- **添加**：无。未推定新业务实体/枚举/权限/价格/流程。

无业务发明说明：128 个 script 是对已跟踪 `tools/*.mjs` 与 workspace 脚本的别名/串联；Playwright 配置选择 tests/e2e；tsconfig.base 只含编译器开关。

## 5. GEN-001–018 逐项裁决

| ID | 裁决 | 证据类型 | 证据 |
|---|---|---|---|
| GEN-001 | **FAIL（GR-01、GR-02）** | 4 | 默认 Playwright 选择器纳入 15 条永不经声明入口执行的真实后端用例；`smoke:p0-ops` 与 `smoke:p0`/`test:backend` 三名一事且零调用方 |
| GEN-002 | **FAIL（GR-01）** | 2+4 | 声明入口 `verify:ui` / `verify:ui:real-backend` 的选择器与 `REAL_BACKEND_E2E` 门不对齐；`--list` + skip 探测 |
| GEN-003 | **FAIL（GR-01）** | 2+4 | 15 条 live-evidence 在默认套件 skip 且 exit 0；真实后端 config 不收集它们；测试通过/列出 ≠ 那些用例被执行 |
| GEN-004 | 适用但本模块无输入校验面 | 4 | 四文件不解析用户输入；环境变量当操作员配置。未把缺校验升为缺陷 |
| GEN-005 | 不适用 | 4 | 四文件无授权逻辑；REAL-UI-03 在其他 spec |
| GEN-006 | 不适用 | 4 | 无状态写入 |
| GEN-007 | 不适用 | 4 | 无并发写路径；fullyParallel false 仅为测试调度 |
| GEN-008 | PASS 于配置内部一致 | 4 | 5173 与 apps/web port 一致；5174/VITE_API_PROXY_TARGET 与 vite.config 默认 8080 同形；vite 重复 flag last-wins 已核 CLI 源码 |
| GEN-009 | **FAIL（GR-01）** | 2 | skip 被当成进程成功（exit 0）；默认 config 无 json skip 闸；real-backend 的 json 闸只在 seeded 包装器读取 |
| GEN-010 | PASS 于本四文件 | 4 | 无密钥/口令字段；凭证在 spec/global-setup（非本模块） |
| GEN-011 | 部分 | 4 | real-backend json reporter 存在；默认 list/html。json 不经 `verify:ui:real-backend` 脚本消费 |
| GEN-012 | PASS | 4 | timeout/webServer timeout 有上界 |
| GEN-013 | 有未验证前提，不单独升 finding | 4 | engines.node 满足；packageManager 声明 10.9.8 vs 本机 npm 11.19.0，无 engines.npm、CI 未 pin npm。未证明因此行为错误。75 个 tool 路径均存在。lockfileVersion 3，playwright/yaml 已钉 |
| GEN-014 | 不适用 | 4 | 无迁移 SQL；`verify:migration-version-order` 只是入口，工具在其他模块 |
| GEN-015 | 不适用 | 4 | 无界面文案；仅 Desktop Chrome 项目，移动视口由其他 spec 自设 |
| GEN-016 | 不适用本四文件正文 | 1+4 | 四文件不是文档。外部 README 声称 Playwright 1.61 与 lock 1.61.0 相符，不在本模块修文档 |
| GEN-017 | PASS 分类 | 1 | 见 §4 |
| GEN-018 | PASS 证据链 | 1/2/3/4 | 见各 finding；本报告不引用席位意见或历史审计 |

## 6. 三线（每条都审完四文件）

### 6.1 第一性原理

1. **为何必须存在？** 删掉 `package.json`，workspace 脚本、CI `npm ci`/`npm run verify:predeploy`、ops 部署门禁入口全部消失——这是工程载体，不是某条 HTML 业务句。删掉两个 Playwright config，声明的 UI 验收入口无配置。删掉 `tsconfig.base.json`，`apps/web/tsconfig.json` extends 断裂。答不上 HTML 业务句 ≠ 该删 → 技术性。
2. **声称是否执行？** JSON 可解析、script 目标存在、`playwright test --list` 两套配置均可加载（exit 0）、`tsc --showConfig` 两处 exit 0。**未**跑完整 UI E2E / 后端测试 / 部署 / docs 生成。
3. **未验证前提：** 本机 npm 11 vs 声明 10.9.8；`typecheck`/`build`/`verify:ui` 全量未跑；vite 重复参数依赖当前 vite 8.0.16 的 `filterDuplicateOptions`；corepack 未强制 packageManager。

### 6.2 顺从式

假定实现意图正确后逐文件核对 GEN 映射：输入（无运行时用户输入）、状态（无）、输出（脚本串联、测试选择、编译选项）、错误（real-backend 注释承认 skip-as-success 并加 json，但默认套件没有对等闸）、权限（无）、持久化（无）、测试（选择器是本模块的测试面）、文档（脚本名是入口不是文档）。**无 finding ≠ 未审**：四文件均有覆盖记录。顺从式仍撞上选择器与 env 门不一致（GR-01）。

### 6.3 对抗式（grilling 自问，不等操作员）

- 若 `verify:ui` 绿，是否执行了所有 `tests/e2e` 里标「真实后端」的用例？**否。** `--list` 47 条含 15 条 live-evidence；无 `REAL_BACKEND_E2E` 时探测 15 skipped / exit 0。
- 若 `verify:ui:real-backend` 绿，是否覆盖那些 live-evidence？**否。** testMatch 单文件，`--list` 仅 3 条 REAL-UI-*。
- 有无第三入口设置 `REAL_BACKEND_E2E=1` 且用默认 config？package.json **没有**。手工 `REAL_BACKEND_E2E=1 playwright test` 不是声明入口。
- webServer 重复 `--host/--port` 会不会绑错端口？vite 8.0.16 `filterDuplicateOptions` last-wins，后写的 127.0.0.1:5174 生效。
- `smoke:p0` 是否冒充 UI smoke？命令是 `test:backend`；backend/README 要求它指向 Java 测试。`smoke:p0-ops` 同命令且无调用方（GR-02）。
- 默认 reuseExistingServer=!CI 会不会挂到真后端？可能，属本地假设；CI=1 时 false。未升独立 finding。
- tsconfig.base 单独当入口会不会误编译全仓 TS？showConfig 显示会。声明入口是 workspace typecheck。未升独立 finding。

## 7. Findings

### GR-01 — 真实后端用例选择器与 REAL_BACKEND_E2E 门错位（skip 假绿 / 声明入口不可达）

- 文件:行号：`playwright.config.ts:4-5`；`playwright.real-backend.config.ts:8-9`；`package.json:82-86`（test:ui / verify:ui / verify:ui:real-backend / seeded）
- 2.1 分类：**技术性**（测试配置；无业务发明）
- 工程缺陷：**成立**
- 冻结判据：GEN-001、GEN-002、GEN-003、GEN-009
- 2.3 依据：类型 2 + 4
- 事实：
  1. 默认 config `testIgnore` 只有 `web-ui-real-backend.spec.ts`。`./node_modules/.bin/playwright test --list --reporter=line` **exit 0**，`Total: 47 tests in 9 files`，其中包括 15 条 live-evidence（customer-account-lifecycle 2 + leasing-contract 12 + order-tickets 1）。
  2. 这些 spec 的 `beforeEach` 为 `test.skip(process.env.REAL_BACKEND_E2E !== "1", …)`（例：`tests/e2e/order-tickets-live-evidence.spec.ts:30`、`leasing-contract-live-evidence.spec.ts:31`、`customer-account-lifecycle-live-evidence.spec.ts:44`）。
  3. 唯一在 package.json 里设置 `REAL_BACKEND_E2E=1` 的脚本是 `verify:ui:real-backend`，且 `--config=playwright.real-backend.config.ts`。该 config `testMatch` 仅 `web-ui-real-backend.spec.ts`。`--list` **exit 0**，`Total: 3 tests in 1 file`（REAL-UI-01/02/03）。
  4. 临时 scratch 配置（无 webServer、无 DB、testMatch 三个 live-evidence 文件）跑 `playwright test`：**15 skipped，exit 0**。进程成功 ≠ 用例执行。已删除 scratch。
  5. `verify:ui:real-backend:seeded` → `tools/run-real-backend-ui-e2e.mjs:170` 仍调用 `npm run verify:ui:real-backend`，因此同样只跑那 3 条。包装器的 json 计数闸（AUD-022）挡的是「3 条被 skip」，挡不住「另外 15 条根本没进 suite」。
- 配置边界：M005 拥有选择器与 npm 入口；skip 语句在其他 spec，但**使它们不可达的是本模块的 testIgnore/testMatch/script 组合**。
- 不把「应新增业务验收」发明进去；只要求声明的测试配置能选中它已经纳入 testDir 且用同一 env 门的文件，或显式 testIgnore 它们并提供能打开该 env 的入口。

### GR-02 — `smoke:p0-ops` 死别名

- 文件:行号：`package.json:48`（`smoke:p0-ops` = `npm run test:backend`）；对照 L47 `smoke:p0` 同命令、L12 `test:backend`
- 2.1 分类：**技术性**
- 工程缺陷：**成立**（GEN-001 重复 / 不可达）
- 2.3 依据：类型 4
- 在过滤后的跟踪文本中，`smoke:p0-ops` **只出现在 package.json**。`smoke:p0` 另有 `backend/README.md` 等引用。ponytail-review：`package.json:L48: delete: 与 smoke:p0/test:backend 同命令且无调用方。删除该键。` net: -1 script。

## 8. 四类型依据索引

| 类型 | 本报告使用 |
|---|---|
| 1 HTML 原文 | 业务系统图 h2「系统交付与上线」「系统通用能力」正文（分类对照，非冲突） |
| 2 可执行产物 | playwright --list 两套；scratch 15 skipped exit 0；json.loads package.json；tsc --showConfig |
| 3 外部规范原文 | 未把惯例当依据。Vite 8.0.16 已安装 CLI 源码 `filterDuplicateOptions`（`value[value.length-1]`）当作当前工具实现，不是 RFC |
| 4 可复现命令 | 见 §9 |

## 9. 命令真实 exit / 关键输出

| 命令 | exit | 关键输出 |
|---|---|---|
| 本席 sha256 criteria-lock.md | 0 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| `git rev-parse HEAD` | 0 | `134d75d158ca6ff1343f616cce47676ecfc83e02` |
| `node -v` / `npm -v` | 0 | v24.20.0 / 11.19.0 |
| python json.loads package.json | 0 | 9 顶层键，128 scripts，无重复 |
| 75 个 `node tools/…` 目标 exists+tracked | 0 | ALL_TOOL_FILES_EXIST True；ALL_SCRIPT_REFS_RESOLVED True |
| `./node_modules/.bin/playwright test --list --reporter=line` | **0** | Total: 47 tests in 9 files（含 15 live-evidence，不含 web-ui-real-backend） |
| `./node_modules/.bin/playwright test --config=playwright.real-backend.config.ts --list --reporter=line` | **0** | Total: 3 tests in 1 file |
| scratch live-evidence 无 webServer 实跑 | **0** | `15 skipped` |
| `./node_modules/.bin/tsc --showConfig -p tsconfig.base.json` | 0 | compilerOptions 含 strict/bundler/noUncheckedIndexedAccess |
| `./node_modules/.bin/tsc --showConfig -p apps/web/tsconfig.json` | 0 | extends 合并成功 |
| 读 vite cli.js filterDuplicateOptions | 0 | L650–652 last-wins |
| lock 中 @playwright/test / yaml | 0 | 1.61.0 / 2.9.0；node_modules 同版本 |
| 未跑 | — | `verify:ui` 全量、`test:backend`、任何 docs: 生成、部署、完整 DB E2E、`npm ci`、改 node_modules |

## 10. 局限

- 未执行完整 mock E2E 与真实后端 seeded E2E（禁完整 DB E2E / 不启生产进程）。GR-01 用 `--list` + 无服务器 skip 探测，不依赖后端。
- 未执行 `npm run typecheck`/`build` 全量编译，只 showConfig。
- `docs:*` 生成类脚本只核入口存在，不跑。
- packageManager 与本机 npm 主版本差未做行为对照实验（避免再动 corepack）。
- 不审未来模块的 spec 业务断言对错；只审它们如何被本模块选择器加载。

## 11. 结论

**FAIL**

理由：存在已落盘的工程缺陷 GR-01（及 GR-02）。runbook §9：无未定稿缺陷才能单席 PASS。本席停止于 B1，不修复、不发问、不进 B2。

# M005 B2 差集复核（Grok 席追加）— V0.03

本段只是 B2 整批候选复核结论，**不是**新的完整 B1。上方原 B1 正文必须继续作为「本次 B1 报告」定位：byte 0–36358 为旧受影响票（完整保留、不采信为独立合取），byte 36359 起为有效新独立 B1。本追加不得覆盖、不得改写前述任何一行。

步骤：B2。模块：M005 / `01-root-config-and-guidance-03`。控制 V0.17。ledger：IN_PROGRESS。B3=0。本席未修源、未改 findings/ledger/计划、未写 questions、未申请改三份业务 HTML。

## 输入身份（本轮实算）

| 输入 | 字节 | SHA256 |
|---|---:|---|
| findings.md V0.48 候选 | 171669 | `927eb2ced3766ac3c54f2bc944ba0da3f5578e300384777d52e7e3d1d5375e22` |
| M005-b1-self-raw.md 当前 V0.04 | 43563 | `552242925e80e266a7dd5b7aaf3c46eba2beb0d871edf50b30879bded93be783` |
| 其中本方独立票前缀 V0.02 | 39710 | `44961cabea4ca790edf19d0960d001aa34161c7c47eb35aa1d27b028cbc1fca3` |
| 本文件追加前全文 | 69679 | `bdcd2b52b3970093c2d1f4b5817e869f69ff7beae75521601b1603e63f52abc2` |
| 有效新 B1（byte 36359–EOF） | 33320 | `d30dab7380be95bfe1cdfe293a7e19f71c851825516a1da7937f075014c65ac7` |
| 旧 Grok 前缀（追溯用，非独立票） | 36359 | `7ee402a51c04c4493f78308f356875e6ac95244fb1863b81d97f982763a7671f` |

self 39710 字节之后的 V0.03/V0.04 为收后核实/接收，不作为新盲审票。旧 Grok 前缀因读到本方索引不参与合取；候选 005/006 保留其观察，本席本轮按源与可执行产物复验，不把旧/新两个不同的 GR-01 混号。

本席本轮实算 `plan/criteria-lock.md` SHA256＝`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（叙述字段，非独占行）。HEAD＝`134d75d158ca6ff1343f616cce47676ecfc83e02`。四源当前 SHA 与 B1 冻结一致：package.json `f59711d7579178d2772ec7456592d616d41ac3d5980e58864aeaf66c2e60b7db`（147 行）、playwright.config.ts `f389c55d86716b5bd921ea60320c74936a72e72b4e54a8df6fae9eca6db86e19`（25 行）、playwright.real-backend.config.ts `cf7b020c030dbd8ad4aeba54fe331528cd0743c59c22cbd03bdb884fe187afa2`（38 行）、tsconfig.base.json `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db`（13 行）。package-lock.json 全文 `65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc`；顶层 `documentVersion` 已是 `V0.01`，`version` 0.1.0，`lockfileVersion` 3；`packages[""].engines.node` 现为 `>=22`，且锁内仅这一处根镜像等于该字面量。

## 技能预检（§6，本步）

目录实核：using-superpowers / verification-before-completion / receiving-code-review / systematic-debugging / diagnosing-bugs / codebase-design / grilling / grill-me / ponytail / ponytail-review / tdd / writing-for-agents / resolving-merge-conflicts / git-guardrails-claude-code / i-have-adhd 均存在。B2 禁止修复，故不落测试、不实施 review 的 IMPLEMENT。grilling 只借追问结构，事实自读，不等操作员、不派子代理。ponytail-review 只评最小删除/收窄，不改源。

缺口（不冒充已调用）：`batch-grill-me`、`ubiquitous-language` 在 mattpocock 技能目录中不存在。

## 本轮最窄反例（无主环境安装 / 无 corepack / scratch 已删）

1. 根引擎 vs 锁：宿主 `node -v`＝v24.20.0，`npm -v`＝11.19.0（`/bin/npm`）。用已装 `npm-install-checks.checkEngine`：根 `>=22` 在 22.0.0/22.12.0/23.0.0 为 true，jsdom 29.1.1 同点为 `EBADENGINE`；22.13.0 与 24.0.0 均 true。用 npm 自带 semver 扫锁：22.0.0 不相容 26、22.12.0 为 1（仅 jsdom）、23.0.0 为 9、22.13.0/24.0.0/24.20.0 为 0。候选范围 `^22.13.0 || >=24.0.0` 对 22.0.0/22.12.0/23.0.0 为 EBADENGINE，对 22.13.0/24.0.0 通过。
2. 选择器：`playwright test --list` 默认 **47/9**（含 live-evidence 2+12+1＝15，已 ignore `web-ui-real-backend.spec.ts`）；`--config=playwright.real-backend.config.ts --list` **3/1**。`property-fee-evidence.spec.ts` 走 mock、无 `REAL_BACKEND_E2E` 门。三份 `*-live-evidence.spec.ts` 与 `web-ui-real-backend.spec.ts` 的 skip 均为 `process.env.REAL_BACKEND_E2E !== "1"`。仓库 `.scratch` 临时配置（绝对 testDir、无 webServer）`--list`：默认 ignore 两模式后 **32/6**，真实 testMatch 同一数组后 **18/4**；文件集合不交，联合 10 文件 / 50 条＝现有 47+3。目录已删。
3. 005 缺 flag：scratch 引用原真实配置、去掉 webServer、重定向 outputDir，`env -u REAL_BACKEND_E2E` 实跑（不连后端）exit **0**，stats `expected=0 skipped=3 unexpected=0 flaky=0`，`.last-run.json` `status=passed`。临时目录已删。公开脚本 `verify:ui:real-backend` 已带 `REAL_BACKEND_E2E=1`，不与本条混为「官方入口全 skip」。
4. 002 端口进 shell：Playwright runner `WebServerPlugin` L832–841 `shell: true`；`getIsAvailableFunction` 只 `new URL`。`REAL_BACKEND_WEB_PORT=<空闲端口>/; printf M005_B2_PLUGIN_MARKER; #` 时 URL 合法，noop `npm()` 避免起 Vite，原 command 仍执行，stdout 含标记，`actualPluginShellMarker=true`，exit 0。`Number(该值)` 为 NaN，候选 `Number.isInteger && 1..65535` 会拒绝。合法 `5174`/`1`/`65535` 通过；`0`/`65536`/`abc`/`5174/;x`/`5174.5` 拒绝。未宣称公网越权。
5. 004：`git ls-files -z` 过滤 audit/.scratch/raw 后扫描，`smoke:p0-ops` **仅** `package.json:48`；`smoke:p0` 另有 `backend/README.md` 与一份 docs 引用。128 个根脚本对根键的 `npm run` 悬空为 0；`node tools/*.mjs` 去重 75 个文件均存在。
6. 006：`playwright.config.ts:17` 为 `npm run dev:web` → `apps/web` `vite --host 0.0.0.0 --port 5173`；消费者 URL 为 `127.0.0.1:5173`。真实配置已示范 `--host 127.0.0.1`。已装 Vite 8.0.16 `cli.js` L650–652 `filterDuplicateOptions` 取数组最后一项（last-wins）。本条只要求默认 E2E 命令覆盖 loopback，不改共享 `apps/web` `dev`，不把 mock 口令写成已证线上越权。
7. 012：`tsc --showConfig -p tsconfig.base.json` 含 strict / bundler / noUncheckedIndexedAccess；`apps/web/tsconfig.json` extends 合并成功（jsx react-jsx）。候选保持该文件不改。

未跑：`npm install`/`ci`、corepack prepare/activate、全量 UI/后端/DB/部署、改 node_modules。`--list` 与 skip 探测不等于业务用例绿。

## 逐十二 ID（来源 / 分类 / 缺陷 / 队列 / 边界）

分类一律按冲突 → 细化 → 技术性 → 添加。四文件与 128 脚本均无三份 HTML 明文矛盾，也不能指到某一 `td.k` / 具体 h2/h3 作为这些 npm/Playwright/TS 字段的上位实现，且无业务实体/权限/价格/流程发明 → 整批 **技术性**。无添加项，故无 2.2 发问。有工程缺陷 → 队列「是」；只登记 → 「否」。

**M005-001** — 来源：本方独立票 CX-01；新 Grok B1 GEN-013 只核了当前 Node 24 未升 finding。差集真实，不以本席 B1 未报否定。分类技术性，缺陷是，队列是。命题：根 `engines.node >=22` 宽于既定锁依赖（jsdom `^22.13.0 || >=24` 为 22 线最紧）。证据见上 checkEngine/semver。最小修复：`package.json` 改为 `^22.13.0 || >=24.0.0`，并只同步 `package-lock.json` 的 `packages[""].engines.node` 同值；锁内仅这一处根镜像为 `>=22`，不重开 M004、不改任何 `node_modules/*` 的 version/resolved/integrity/SRI/URL、不改 `apps/web` 记录、不改冻结模块映射。文件自身版本：package.json 首次加 `documentVersion: V0.01`；package-lock 已有 V0.01 升 V0.02。应用 `version` 0.1.0 与 `lockfileVersion` 3 保持。此为最小直接消费一致性边界，本席接受。

**M005-002** — 来源：本方 CX-02；新 Grok B1 把环境当操作员配置未升缺陷。差集真实。分类技术性，缺陷是，队列是。命题：`REAL_BACKEND_WEB_PORT` 未校验即进入 `shell:true` 命令。本席复现标记执行，前提仅为能设该环境的本地/CI，不声称线上 RCE。最小修复：`Number(...)` + `Number.isInteger` 且 1–65535，否则抛中文错误，其后模板只用 number。不增库/通用 shell 框架、不另加字符串格式规则（如 `080`→80 的残留不在本 ID 扩大）。同文件首次 `// 版本：V0.01`，005 共享、不重复递增。

**M005-003** — 来源：有效新 Grok GR-01（选择器漏 15 条）。分类技术性，缺陷是，队列是。命题：15 条 live-evidence 留在默认套件（无 flag 则 skip 仍绿），声明的 real-backend 入口 `testMatch` 又不收集它们；`verify:ui:real-backend:seeded` 仍调用该入口，json 闸挡不住「根本没进 suite」。与 005 原因不同，禁止混条。最小修复：默认 `testIgnore` 与真实 `testMatch` 同为 `["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"]`，只匹配当前已存在四文件，无新脚本、无改 fixture 业务断言。scratch `--list` 已得 32/6 与 18/4、不交、并为 50。沿用既有 `REAL_BACKEND_E2E=1` 入口；不把选择器修复宣称为 18 条业务已通过。默认配置首次 `// 版本：V0.01`，006 共享。

**M005-004** — 来源：新 Grok GR-02；旧票 GR-03 同观察；本席 ls-files 扫描仅定义处。分类技术性，缺陷是，队列是。精确删除 `smoke:p0-ops` 键，保留 `smoke:p0` / `test:backend` 及 README 防回流入口。无已证调用不是保留理由。package.json 版本与 001 共享一次。

**M005-005** — 来源：旧受影响 Grok GR-01 观察 + 本方收后复现 + 本席本轮隔离复验。分类技术性，缺陷是，队列是。命题：用户直接 `--config=playwright.real-backend.config.ts` 且未设 flag 时 3 skip / exit 0 / last-run passed。官方 npm 入口已设 flag，不污为同一 skip。最小修复：加载时 `REAL_BACKEND_E2E !== "1"` 则抛中文错误，提示设变量或用 `npm run verify:ui:real-backend`；不自动开真实后端。`--list` 审计须显式传 1。沿用既有 flag，不新增开关。版本共享 002。与 003 分条。

**M005-006** — 来源：旧 Grok GR-02 观察（绑定 0.0.0.0）+ 新 B1 覆盖表同一展开 + 本席源与 Vite CLI 核实。分类技术性，缺陷是，队列是。只认定默认 E2E webServer 超出 127.0.0.1 消费需求。**明确拒绝**旧票把默认 mock 口令外推为已证线上越权。最小修复：仅改 `playwright.config.ts` `webServer.command` 为 `npm --workspace apps/web run dev -- --host 127.0.0.1`，靠 Vite last-wins 覆盖脚本里的 0.0.0.0，保留 5173。不改共享 `apps/web` `dev`、不改 `reuseExistingServer`/超时/其它服务器参数。版本共享 003。

**M005-007** — 本方 T01 + 两份 Grok 根字段覆盖。技术性，缺陷否，队列否。name/private/version/type/workspaces/JSON 可解析、workspace 路径存在；无业务发明。应用 semver 0.1.0 不在本项改。

**M005-008** — 本方 T02 + 128 scripts 覆盖。技术性，缺陷否，队列否。本席核 128 键、75 个 node 工具文件存在、根 `npm run` 悬空 0、workspace 转发目标为 web 已有 `dev/build/typecheck/test:unit`。缺陷只归 001–006（含删 004 那一键）。不把工具文件名发明成业务流程。

**M005-009** — 本方 T03 + 依赖/packageManager 覆盖。技术性，缺陷否，队列否。Playwright/yaml 为已装 1.61.0 / 2.9.0 且被根 CLI / OpenAPI 工具实际消费；`packageManager` 声明 npm@10.9.8，本机为 11.19.0，如实登记，不自行加 `engines.npm`、不无谓升级依赖。新 B1 误执行 corepack `--activate` 记为环境事件：当前 `/bin/npm` 仍 11.19.0，四源与 lock 对 HEAD 无 diff；缺事前 lastKnownGood 证据，不猜测回滚、不单列 ID。

**M005-010** — 本方 T04 + mock 配置覆盖。技术性，缺陷否，队列否。超时/报告/globalSetup/Chromium/reuseExistingServer 能加载；选择器与绑定缺陷只归 003/006。

**M005-011** — 本方 T05 + real 配置覆盖。技术性，缺陷否，队列否。json reporter 路径与 `tools/run-real-backend-ui-e2e.mjs` L68–93 / L170–178 消费一致；strictPort/proxy/timeout 服务既有隔离。端口/选择器/缺 flag 假绿只归 002/003/005。不以 reporter 存在声称真实业务通过。

**M005-012** — 本方 T06 + tsconfig 全字段。技术性，缺陷否，队列否。九个 compilerOptions 被 extends 消费；本项不改 `tsconfig.base.json`。

漏并检查：本方 CX-01/02 与 T01–T06、新 GR-01/02、旧观察 GR-01/02/03 均已落入 001–012；无第三套临时号被丢弃，无改写对方 finding 命题。未把两个 GR-01 合成一条。

## 整批修复边界（候选，B2 通过前未实施）

只改：`package.json`、`package-lock.json` 根镜像（`packages[""].engines.node` + 顶层 `documentVersion`）、`playwright.config.ts`、`playwright.real-backend.config.ts`。不改：`tsconfig.base.json`、任何测试 spec 业务正文、三份业务 HTML、其它锁记录、`apps/web/package.json`、计划/模块映射。hunk 按 001–006 归因；同文件版本行按首次涉及的 ID 记账一次。验证：JSON/根 engine 相等；除根 engine 与顶层 documentVersion 外锁记录深相等；删键后脚本图；端口合法/非法在构造 URL 前拦截；缺真实 flag 非 0、flag=1 可列 18；默认 32 与真实 18 不交并为 50；tsc showConfig。临时反例仅 scratch 并删除。不凭 `--list` 宣称用例业务绿。

## 分歧

无。对来源、四分类、工程缺陷、队列是/否、001 根锁镜像边界、003/005 分条、006 不升级为线上越权、无添加/无 HTML 申请/无未答，全部同意。

## 结论

**PASS**
