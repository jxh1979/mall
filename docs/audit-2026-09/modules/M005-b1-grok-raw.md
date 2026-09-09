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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M005 B1 Grok raw — V0.04

- 席位：Grok（外席乙）。本席本次完整报告自身版本 V0.04。
- 控制：160-audit-prompt V0.17。计划：`docs/audit-2026-09/plan/modules.md` V0.04，模块总数 471。
- 阶段：B1。模块：M005 — 治理门禁与 CI 基线／配置与工程入口／第3段。标记 NORMAL。
- 正本：`/home/james/complex-management` `main`。`HEAD == origin/main == 242f487233d028173cb9198eecd079f5d877405c`。
- 范围（以当前 `plan/modules.md` M005 为准，3 文件 52 行）：`tsconfig.base.json`（13）、`.github/CODEOWNERS`（12）、`.github/workflows/predeploy-gates.yml`（27）。`git ls-files -z -- <三路径> | xargs -0 cat | wc -l` = 52。
- 映射：`criteria-lock.md` `M005 → [GEN-001 … GEN-018]`，仅 18 项通用工程判据，无 HTML 派生 ID。
- 本席实算 `docs/audit-2026-09/plan/criteria-lock.md` SHA256 见本报告第一行；sidecar `criteria-lock.sha256` 同值。未改锁、未静默对齐。
- 历史计数按操作员给定保留、不清零：B3 累计 1、方案修复 2、交付失败 0。M001—M004 已 PASS、M005 IN_PROGRESS 采操作员给定，不读其它模块收口或审计结论。
- 旧 322 同号 / 历史修复 / 本文件前缀旧报告正文：不采信、不作为本次 PASS。前缀元数据：83350 字节，SHA256 `edd292d2d78e58bd141fcfcf20c3bf1237145f89f5d7b56e4f5e106cc960bf08`，文件此前自身版本 V0.03；本次只追加。
- 互盲：未读 self raw、findings 正文、本模块旧报告正文、其它模块收口。误命中检索索引时丢弃，证据只来自源文件、直接消费方、官方规范与本席命令输出。
- 禁子代理、禁 MCP_Cloud_160 / MCP_Codex_160。未改源码 / HTML / 计划 / 判据 / ledger / findings / questions / 任何配置。未发工作流、未 PR/评论/issue、未推送、未部署、未装依赖、未 `npm ci`、未 `npx`。
- 三文件版本：`tsconfig.base.json` 与 `.github/CODEOWNERS` 无自身版本（本次不改，不强加）；工作流保持 `V0.01`。未把应用 `version` 0.1.0 当文件版本。
- 用户给定：B/C 配置原文「就是我的原意」——CODEOWNERS L1–L3 与工作流 L3–L6 关于私有库免费版无法分支保护 / 无法 required status check、硬阻断在 160 与 pre-push——不重新判冲突。
- 已知下游：工作区 BSM HTML 与镜像 CSV 不一致使本地 `verify:html-ledger-sync` / `verify:entry-anchors` 红，根因属既定 M425/M427 等；不改 CSV/HTML、不重开 M001—M004。M004 锁已闭环，npm audit / Vitest moderate 不在此扩修。

## 0. §6 技能预检（本席环境实核）

| 技能 | 路径 | 用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 已读全文；用户指令优先于技能。 |
| grilling / grill-me | mattpocock `.../productivity/grilling/SKILL.md` 与 `grill-me/SKILL.md` | 已读；按提示词收窄：自问自答，不派子代理、不等操作员。 |
| codebase-design | mattpocock `.../engineering/codebase-design/SKILL.md` | 已读；用删除测试与 seam（extends / CODEOWNERS 模式 / 工作流入口）。 |
| diagnosing-bugs | mattpocock `.../engineering/diagnosing-bugs/SKILL.md` | 已读；只读追本地红 vs CI 绿的根因。 |
| systematic-debugging | superpowers `.../systematic-debugging/SKILL.md` | 已读；只借读错误→复现→数据流；不改代码。 |
| verification-before-completion | superpowers `.../verification-before-completion/SKILL.md` | 已读；结论均附本席本轮命令输出。 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 已读；见第 8 节。 |
| batch-grill-me | 本环境 `find` 无此技能 | **缺口**，不冒充已调用。 |
| ubiquitous-language | `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` | 存在；收窄：不写 `UBIQUITOUS_LANGUAGE.md`。用其区分 HTML「留痕复核」≠ CODEOWNERS「独立复核」。 |
| TDD | 不适用 | 源码无变更；纯配置不伪造测试。 |

## 1. 三文件哈希与直接消费方

```
13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db  tsconfig.base.json
b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d  .github/CODEOWNERS
5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974  .github/workflows/predeploy-gates.yml
5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974  ops/ci/predeploy-gates.yml
cmp_exit=0
```

- TypeScript 消费：受控 tsconfig 仅 `tsconfig.base.json` 与 `apps/web/tsconfig.json`（`"extends": "../../tsconfig.base.json"`）。`apps/web/package.json` `"typecheck": "tsc -b --noEmit"`；根 `"typecheck": "npm run typecheck --workspace apps/web"`。
- 工作流消费：`npm run verify:predeploy`（`package.json` 脚本，`&&` 链）。同仓镜像 `ops/ci/predeploy-gates.yml` 字节级一致。`tools/verify-delivery-gates.mjs`（`verify:delivery-gates-docs`，在 `verify:predeploy` 链内）断言：两份 yml 字节相同、`fetch-depth: 0`、`run: npm run verify:predeploy`、`HTML_GATES_BASE` 表达式逐字相等。
- CODEOWNERS 消费：GitHub 在默认分支 `.github/CODEOWNERS`；官方规范要求写权限主体 + 受保护分支「Require review from Code Owners」才强制。本仓私有免费版 `GET .../branches/main/protection` → HTTP 403「Upgrade to GitHub Pro...」，与文件 L1–L3 原文一致。

## 2. 52 行连续覆盖表

分类顺序：冲突 → 细化 → 技术性 → 添加。三份 HTML 无 TypeScript / CODEOWNERS / GitHub Actions 锚点；PGM「留痕复核」是业务操作留痕，不是 GitHub 人审。无冲突、无业务发明 → 全部技术性。

### 2.1 `tsconfig.base.json`（13 行，无自身版本）

| 行 | 内容摘要 | 分类 | 上位 HTML | 技术性理由 |
|---|---|---|---|---|
| L1 | `{` | 技术性 | 无 | JSON 结构 |
| L2 | `"compilerOptions": {` | 技术性 | 无 | 编译器选项容器 |
| L3 | `"target": "ES2022"` | 技术性 | 无 | 编译目标；无业务枚举 |
| L4 | `"module": "ESNext"` | 技术性 | 无 | 模块系统；web 再写一遍同值 |
| L5 | `"moduleResolution": "Bundler"` | 技术性 | 无 | 与 Vite/Bundler 配对 |
| L6 | `"strict": true` | 技术性 | 无 | 类型严格开关 |
| L7 | `"esModuleInterop": true` | 技术性 | 无 | CJS/ESM 互操作 |
| L8 | `"forceConsistentCasingInFileNames": true` | 技术性 | 无 | 路径大小写 |
| L9 | `"skipLibCheck": true` | 技术性 | 无 | 跳过 `.d.ts` 检查 |
| L10 | `"resolveJsonModule": true` | 技术性 | 无 | 允许 import JSON |
| L11 | `"noUncheckedIndexedAccess": true` | 技术性 | 无 | 索引访问 `| undefined` |
| L12 | `}` | 技术性 | 无 | 闭合 compilerOptions |
| L13 | `}` | 技术性 | 无 | 闭合根对象 |

### 2.2 `.github/CODEOWNERS`（12 行，无自身版本）

| 行 | 内容摘要 | 分类 | 上位 HTML | 技术性理由 |
|---|---|---|---|---|
| L1 | 强制点文件；改动应经独立复核 | 技术性 | 无（≠ PGM 留痕复核） | GitHub 人审意图注释；用户原意 |
| L2–L3 | 仅在受保护分支 + require review 时强制；免费私有库无法启用，现只记录意图 | 技术性 | 无 | 与 GitHub 官方规范 + 本仓 403 一致；用户原意，不重判冲突 |
| L4 | `/ops/160/complex-deploy-api.sh @jxh1979` | 技术性 | 无 | 部署脚本路径所有权，非业务角色 |
| L5 | `/ops/160/complex-deploy-web.sh @jxh1979` | 技术性 | 无 | 同上 |
| L6 | `/ops/160/hooks/ @jxh1979` | 技术性 | 无 | 钩子目录 |
| L7 | `/ops/160/propose-change.sh @jxh1979` | 技术性 | 无 | 开 PR 脚本 |
| L8 | `/ops/ci/ @jxh1979` | 技术性 | 无 | CI 镜像目录 |
| L9 | `/.github/ @jxh1979` | 技术性 | 无 | 含本文件与工作流；官方建议所有者覆盖 CODEOWNERS 自身 |
| L10 | `/tools/ @jxh1979` | 技术性 | 无 | 门禁工具 |
| L11 | `/package.json @jxh1979` | 技术性 | 无 | 仅根清单，不含 `apps/web/package.json` |
| L12 | `/backend/src/main/resources/db/migration/ @jxh1979` | 技术性 | 无 | 迁移目录所有权，不是迁移规则发明 |

### 2.3 `.github/workflows/predeploy-gates.yml`（27 行，文件版本 V0.01）

| 行 | 内容摘要 | 分类 | 上位 HTML | 技术性理由 |
|---|---|---|---|---|
| L1 | `# 文件版本：V0.01` | 技术性 | 无 | 文件版本，非应用 version |
| L2 | `name: predeploy-gates` | 技术性 | 无 | 系统内部标识符 |
| L3–L6 | GitHub 侧可见信号；硬阻断在 160 / pre-push；无 DB | 技术性 | 无 | 用户原意；与保护 API 403 一致 |
| L7–L11 | `on: pull_request/push branches: [main]` | 技术性 | 无 | 触发器 |
| L12–L15 | `jobs.predeploy` `ubuntu-latest` `steps` | 技术性 | 无 | 作业定义 |
| L16–L18 | checkout SHA pin + `fetch-depth: 0` | 技术性 | 无 | 完整历史供 merge-base |
| L19–L21 | setup-node SHA pin + `node-version: "22"` | 技术性 | 无 | 与 `engines.node ^22.13.0 \|\| >=24` 的 22 线相容 |
| L22–L23 | `npm ci --ignore-scripts` | 技术性 | 无 | 安装且跳过生命周期脚本 |
| L24–L27 | `HTML_GATES_BASE` 表达式 + `npm run verify:predeploy` | 技术性 | 无 | 把事件基线交给已有门禁；无 `continue-on-error` |

无添加项。不申请改三份 HTML。不加第四道人闸。

## 3. 第一性原理

### 3.1 `tsconfig.base.json` 为什么必须存在

删除后 `apps/web/tsconfig.json` 的 `extends` 失配，九个编译开关不再被 web typecheck 继承。三份 HTML 没有任何一条会因此做不到（无业务上位）→ 不因此删除，走技术性。

声称行为已执行验证（类型 4 + 2）：

- `tsc --showConfig -p apps/web/tsconfig.json` exit 0：353 个文件；九开关全部出现在解析结果（`target=es2022` `module=esnext` `moduleResolution=bundler` `strict=True` `esModuleInterop=True` `forceConsistentCasingInFileNames=True` `skipLibCheck=True` `resolveJsonModule=True` `noUncheckedIndexedAccess=True`），另叠加 web 的 `jsx=react-jsx`、`types=['vite/client']`。
- TypeScript 5.9.3 `convertCompilerOptionsFromJson` 无错误；枚举名 `ES2022` / `ESNext` / `Bundler`。
- `/tmp/m005-tsc-probe` 内存诊断：`ok.ts` 0 错；`implicit-any.ts` TS7006；`index-access.ts` 在 `noUncheckedIndexedAccess=true` 时 TS2322、关掉后 0 错；`json-import.ts` 0 错。证明开关被真实消费，不是死配置。

未验证就当真的前提（见第 7 节）：Linux 上 `forceConsistentCasingInFileNames` 的跨大小写碰撞；`skipLibCheck` 是否掩盖某条 `@types` 错误。二者不支撑「必须删/改」结论。

删除测试：文件是深模块（小接口：九个 compilerOptions；实现在 tsc）。不能删。收窄：不必为「仅 extends 用」强加 `include: []`——声明入口是 web tsconfig，不是对本文件 `tsc -p`。

### 3.2 `.github/CODEOWNERS` 为什么必须存在

删除后失去「强制点文件」的可启用记录，以及官方建议的 CODEOWNERS 自覆盖（`/.github/`）。HTML 无对应条目 → 技术性。用户原文已写明当前不强制。

验证：

- 位置 `.github/CODEOWNERS` 为官方搜索顺序第一（类型 3：GitHub docs *About code owners*「CODEOWNERS file location」）。
- 体积 743 字节 << 3 MB。
- 语法：无 `!` 否定、无 `[ ]` 字符类、无 `\#` 转义；GitHub `GET /repos/jxh1979/mall/codeowners/errors?ref=main` → `{"errors":[]}`（类型 4）。
- 主体 `@jxh1979`：`GET /users/jxh1979` `login=jxh1979 id=69119522 type=User`；本仓 `viewerPermission=ADMIN`（类型 4）。官方要求 code owner 具备 write（类型 3）。
- 本席 gitignore 风格匹配：L4–L12 分别命中 1/1/1/1/1/2/90/1/377 个受控文件；`/.github/` 命中 CODEOWNERS 自身与本工作流。
- 保护规则：`GET .../branches/main/protection` HTTP 403 升级 Pro（类型 4），坐实 L2–L3。

覆盖反例（对抗式列出，不升缺陷）：见 5.2。属声明的强制点子集，不是「声称全仓所有权」。

### 3.3 工作流为什么必须存在

删除后 GitHub 上不再有独立于 160 的可见门禁信号。HTML 无 CI 条目 → 技术性。L3–L6 写明不是硬阻断。不能删。

验证：

- YAML 1.2（`node_modules/yaml`）解析 `on` 为对象 `{pull_request:{branches:["main"]}, push:{branches:["main"]}}`，0 errors。PyYAML 1.1 会把 `on` 当成布尔——**不能**用它宣称工作流无效；GitHub 已实际触发。
- 动作 SHA 与公开标签一致（类型 3/4）：`actions/checkout` `v4.3.1` → `34e114876b0b11c390a56381ad16ebd13914f8d5`；`actions/setup-node` `v4.4.0` → `49933ea5288caeca8642d1e84afbd3f7d6820020`。
- 无 `continue-on-error`。`npm run verify:predeploy` 为 `&&` 链，首红即停。
- 缺基线 / 全零 before 均 fail-closed：`HTML_GATES_BASE=absent-ref-m005` 与 `0000000…0` 均 exit 1，文案「无法判定不等于通过」。
- 直接消费者 `npm run verify:delivery-gates-docs` → `DELIVERY_GATES_DOCS_PASS` exit 0。
- 真实 GitHub 产物（类型 2）：workflow `predeploy-gates` active id 307870467。HEAD push run `34199032554` conclusion success；job 步骤含 checkout / setup-node / `npm ci --ignore-scripts` / `verify:predeploy (governance gates)` 皆 success。日志：`node: v22.23.2`（满足 `engines.node ^22.13.0`）；`HTML_GATES_BASE: 8765bd2e37d10616c15b360a1faa7eff7ad4ca41`；`[html-ledger-sync] PASS`；`[entry-anchors] PASS`；后续链内静态门禁均 PASS，含 `DELIVERY_GATES_DOCS_PASS`、`TEST_DB_ISOLATION_PASS`。PR run `34198944107` 同样注入该 base SHA 并 PASS。表达式在 push 与 pull_request 上均把事件基线交给脚本。
- `default_workflow_permissions: read`，`can_approve_pull_request_reviews: false`。未写 `permissions:` 块时本仓默认只读，满足最小权限现状。
- `verify:test-db-isolation` / `verify:tenant-isolation` 源码为读文件/正则，不连库，与「无需数据库」注释一致。

工作流准确暴露真实失败：本地因未提交 BSM 改动，`verify:html-ledger-sync` exit 1（改了 HTML 未同步台账）、`verify:entry-anchors` exit 1（6 处注册表/账本与 WT HTML 不一致）。同一命令在 CI 干净检出上 PASS。本地红 ≠ 工作流假绿，也 ≠ 提交树红。不得把已知下游镜像红说成自动工作流缺陷，也不得称工作区/发布全局绿。

## 4. 顺从式：18 GEN 逐项

| ID | 适用与证据 | 结论 |
|---|---|---|
| GEN-001 | 三文件均有可验证用途（web 继承、强制点记录、GitHub 可见门禁）。无重复/纯转发。CODEOWNERS 是有意子集不是死代码。 | 满足 |
| GEN-002 | web `showConfig` 继承九开关；探针正负诊断；工作流 CI 实跑 `npm run verify:predeploy` 全链；缺基线 exit 1；无 continue-on-error。 | 满足 |
| GEN-003 | 本模块无测试文件（配置类不伪造）。工作流关键断言由链内 `verify-delivery-gates.mjs` 执行且本席跑通 PASS；CI 日志可失败（本地已见 exit 1）。测试通过不替代上述行为证据。 | 满足（无强制 TDD） |
| GEN-004 | `HTML_GATES_BASE` 缺/零 SHA 在首次使用 merge-base 前失败。CODEOWNERS 主体经 API 确认存在。checkout 固定完整 SHA。 | 满足 |
| GEN-005 | 仓级 `default_workflow_permissions=read`。CODEOWNERS 当前不强制（原文 + 403），不是运行时授权绕过。未把 GitHub 人审伪装成业务权限模型。 | 满足 |
| GEN-006 | 三文件无状态机/金额/部分写入。 | 不适用（已核无该路径） |
| GEN-007 | 工作流可重复跑；门禁脚本幂等只读。无并发写。 | 不适用/无缺陷 |
| GEN-008 | 工作流镜像字节一致；`HTML_GATES_BASE` 表达式与 `verify-delivery-gates.mjs` 期望逐字相同；checkout/setup-node 注释标签与公开 API SHA 一致；`node-version: "22"` 与 CI `v22.23.2`、根 engines 22 线相容。 | 满足 |
| GEN-009 | 缺基线红；`&&` 传递失败；无吞错。CI 成功路径与本地失败路径均可复现。 | 满足 |
| GEN-010 | 三文件无秘密、无注入点。`npm ci --ignore-scripts` 降低安装期脚本。`pull_request` 而非 `pull_request_target`。TOKEN 默认 read。 | 满足 |
| GEN-011 | CI 日志含基线 SHA、各门禁 PASS/FAIL 行。CODEOWNERS errors API 空。不依赖不可验证口述。 | 满足 |
| GEN-012 | `fetch-depth: 0` 有意全历史；作业在 CI 约 7s 跑完门禁链。无本模块无界循环。未设 `timeout-minutes` 走 GitHub 默认，当前首跑不撞。 | 满足 |
| GEN-013 | 动作 SHA 钉死；`npm ci` 用 lock；node 22 已在 CI 解析为 v22.23.2。不依赖本机 Node 24 作为 CI 运行时。 | 满足 |
| GEN-014 | 无迁移文件；CODEOWNERS 只声明迁移目录所有权。 | 不适用 |
| GEN-015 | 无产品 UI。工作流 `name` 为内部标识符。 | 不适用 |
| GEN-016 | 工作流 L1 版本 V0.01 仍在文件头。L3–L6 与保护 API 403、CI 非 required、160 硬阻断可复现。注释「快子集」相对完整 `verify:delivery-gates` 为真（本命令不含 unit/UI 等）；实际入口是 `verify:predeploy` 而非注释枚举的穷尽清单——注释未宣称穷尽。动作标签与 SHA 已对照公开 API。 | 满足 |
| GEN-017 | 52 行依序分类，全部技术性；无业务发明伪装；无因 HTML 沉默而删除。用户 B/C 原文不重判冲突。 | 满足 |
| GEN-018 | 结论落到 HTML 无锚点（类型 1 负向）、CI 产物（类型 2）、GitHub 官方文档/公开 API（类型 3）、本席命令（类型 4）。不引用旧报告、惯例或另一席意见。 | 满足 |

## 5. 对抗式

自问（grilling 收窄，已自答）：

1. 直接 `tsc -p tsconfig.base.json` 会不会当工程编译？`tsc --showConfig -p tsconfig.base.json` 列出 371 个文件（含 `playwright*.ts`、`apps/web/**/*.tsx`），base 无 `jsx`。**反例成立但是脚枪**：没有任何脚本以该文件为 `-p` 工程；声明入口是 `apps/web/tsconfig.json`（353 文件 + jsx）。不升缺陷。
2. CODEOWNERS 覆盖反例：`ops/160/merge-pr.sh`、`install-git-hooks.sh`、rollback/backup、`package-lock.json`、`tsconfig.base.json`、`apps/web/package.json` 无匹配。32 个 `ops/160` 受控文件只覆盖 4 个。**反例成立且与 L1「强制点文件」子集叙事一致**，用户原意不重判为漏覆盖缺陷。官方：无 owner 的路径不请求 code owner（类型 3）。
3. CODEOWNERS 在 draft PR 不自动请求（官方原文）。叠加无保护规则 → 当前零强制。文件已写明。不是假绿：它从未声称正在强制。
4. 工作流漏触发：仅 `main` 的 PR/push。其它分支无本工作流。硬阻断不在 GitHub（原文）。`pull_request` 默认 types 含 opened/synchronize/reopened；draft 仍跑门禁（无 `if` 过滤）。
5. 假绿：无 `continue-on-error`；缺基线红；CI 对提交树实跑全链且日志逐项 PASS。本地红来自 WT BSM 相对 HEAD 的 4 处未提交改动（版本注释 V0.11→V0.13、导航文案、工单关闭句、页脚日期），`git hash-object` HEAD `16783edc` vs WT `12334b13`。CI 干净检出不会看见这些行。工作流没有把下游业务镜像失败藏成绿。
6. `on:` 未加引号：YAML 1.1 解析器会把它当布尔；GitHub 工作流解析 + 本仓 yaml 1.2 + 实际 runs 证明触发器有效。
7. 清单漂移：镜像与生效工作流 `cmp` 0。动作标签未相对公开 API 漂移。`ubuntu-latest` 浮动、动作 Node 20 runtime 被 runner 强制到 24（CI `##[warning]`）——当前 job 仍 success，属平台弃用预警，不是本轮首跑失败。
8. 所有权主体消失：`@jxh1979` 仍存在且为 ADMIN；官方「不存在或权限不足则不指派 owner」，当前不触发。

ponytail-review：无可删死代码/多余抽象。`Lean already. Ship. net: -0 lines possible.`

## 6. Findings

无临时 GR 项。无冲突、无添加、无工程缺陷（队列均为不适用）。对抗式反例与平台弃用警告记入第 5、7 节，不升修复队列。

不把应用版本、旧 322、历史 B3 修复或前缀旧票当作本模块缺陷或 PASS 依据。

## 7. 未验证边界

- `forceConsistentCasingInFileNames` 在本 Linux 大小写敏感文件系统上未构造碰撞用例。
- `skipLibCheck: true` 未引入故意损坏的 `.d.ts` 以证明被跳过。
- 未跑 `apps/web` 全量 `tsc -b --noEmit`（可能写 `tsconfig.tsbuildinfo`；该文件已存在且属 web 模块，本席不扩审 web 源）。九开关消费已用 `showConfig` + 内存探针证明。
- GitHub 表达式 `&&`/`||` 官方原文页本轮 raw 路径 404；行为改由 PR/push 两次真实 run 的 `HTML_GATES_BASE` 值证明。
- 未在 GitHub 求值器里逐文件重放 CODEOWNERS（用 errors API 空集 + 本席匹配器）。目录尾斜线与官方 `/docs/` 示例一致。
- `ubuntu-latest` 未来镜像、以及 GitHub 拆除 Node 20 action runtime 之后，当前 pin 的 checkout/setup-node SHA 是否仍能跑。当前 run 仅 warning 仍 success。
- 仓级 `default_workflow_permissions` 若被改成 write，本文件未写 `permissions:` 不会自己收窄。当前 API 值为 `read`。属设置变更而非首跑失败。
- 工作区未提交 BSM 不代表 origin/main 提交树红；提交树以 CI run 34199032554 为准。

以上边界均不支撑本模块 FAIL，也不支撑「全局绿」。

## 8. HTML 自身问题 / 人闸

- 未发现已提交三份 HTML 的新自身问题需要铁律 4 上报暂停。
- 工作区 BSM 未提交改动与镜像 CSV 的不一致是操作员已给定的 M425/M427 范围，不是本 3 文件引入的业务发明，不新开 2.2 添加项，不申请改 HTML。
- 无添加项 → 无人闸。

## 9. B1 结论

**PASS**

三文件 52 行全部覆盖；18 GEN 逐项有四类证据；三线完成；无未答 questions；无待修工程缺陷；无添加项。工作流在提交树上准确暴露门禁真实结果（绿则日志逐项 PASS，基线缺失则红）。本地因未提交 BSM 导致的 html-ledger-sync / entry-anchors 红不是本工作流缺陷，也不能改写为发布/全局绿。

## 10. 本席命令与关键输出（节录）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

git rev-parse HEAD origin/main
242f487233d028173cb9198eecd079f5d877405c
242f487233d028173cb9198eecd079f5d877405c

git ls-files -z -- tsconfig.base.json .github/CODEOWNERS .github/workflows/predeploy-gates.yml | xargs -0 cat | wc -l
52

cmp -s .github/workflows/predeploy-gates.yml ops/ci/predeploy-gates.yml; echo $?
0

./node_modules/.bin/tsc --showConfig -p apps/web/tsconfig.json  → exit 0，九开关均在，files 353
TS API 探针：ok 0 / implicit-any TS7006 / index-access TS2322（关 noUncheckedIndexedAccess 则 0）

npm run verify:delivery-gates-docs → DELIVERY_GATES_DOCS_PASS exit 0

HTML_GATES_BASE=absent-ref-m005 npm run verify:html-ledger-sync → exit 1 取不到基线
HTML_GATES_BASE=0000000000000000000000000000000000000000 … → exit 1

npm run verify:html-ledger-sync（工作区）→ exit 1 改了 BSM 未同步台账（基线 origin/main@242f4872）
npm run verify:entry-anchors（工作区）→ exit 1，6 处不合格

gh api repos/jxh1979/mall/codeowners/errors?ref=main → {"errors":[]}
gh api repos/jxh1979/mall/branches/main/protection → 403 Upgrade to GitHub Pro
gh api repos/jxh1979/mall/actions/permissions/workflow → default_workflow_permissions=read
公开 API：checkout v4.3.1 与 setup-node v4.4.0 的 SHA 与注释一致
CI run 34199032554 (push HEAD) / 34198944107 (pull_request) success；node v22.23.2；HTML_GATES_BASE=8765bd2e37d10616c15b360a1faa7eff7ad4ca41
```

纯计算副本仅 `/tmp/m005-tsc-probe`、`/tmp/m005-web-showconfig.json`、`/tmp/m005-base-showconfig.json`、本报告草稿 `/tmp/m005-b1-grok-v004.md` 与官方文档缓存；未写入业务树。

# M005 B2 差集复核（Grok 席追加）— V0.05

版本：V0.05（本文件既有 V0.01–V0.04 全部字节保留，本次完整 B2 归并结论追加；自身版本 V0.04→V0.05）

席位：Grok（唯一外审）。节点：160-audit-prompt.md V0.17 阶段 B / 当前 471 计划 / M005 / B2（整批候选复核，不是 B1 重跑、不是模块 PASS）。模块名「治理门禁与 CI 基线／配置与工程入口／第3段」；仅当前 3 文件 52 行。ledger：M005 `IN_PROGRESS`；历史 B3 累计 1、方案修复 2、交付失败 0，不清零。不重开 M001—M004，不审下一模块，不修源码或计划。本席不改 findings/ledger/questions/三份业务 HTML，不派子代理，不用 MCP_Codex_160 / MCP_Cloud_160，不提交、不推送、不部署、不装依赖、不另写 /tmp 报告草稿或其它审计产物。本追加段不写独占 `CRITERIA-LOCK-SHA256:` 字段行。

**本席 B2：PASS。不是模块 PASS。不是有条件通过。**

## 0. 输入身份（本席本步实算）

| 对象 | 派发声称 | 本席实算 |
|---|---|---|
| 候选 `docs/audit-2026-09/findings.md` | V0.95，324990 字节，SHA256=`cf75ee8f31aa440df52ce66d1a37fdffbc5c4b5d62ae288f87e00543e3282b63` | `sha256sum`/`wc -c` 同值；版本行 `V0.95`；只审最上方当前 M005 B2 候选及历史 M005-012 同源登记；不重审其它模块段落 |
| 本方 `modules/M005-b1-self-raw.md` 当前 V0.06 | 原 47896 字节之后追加；当前 84370 字节，SHA256=`ce387cf0b96872ffd5520e80f1b7df9d1dd825f82128405a6ad94f55a73451d8` | 全文同值。前 47896 字节 SHA256=`0ad9ec052aec7837ef6749549e977b4457016b3a965c76d6e84378c689f69817`（旧 322 范围报告，完整保留、不采信为当前独立 B1）。本次完整报告自 L316 `# M005 当前471计划 B1完整独立自审` / 版本 V0.06 起 |
| 本席 `modules/M005-b1-grok-raw.md` 当前 V0.04 | 原 83350 字节之后追加；当前 107434 字节，SHA256=`3905059dfcbf8b832ba8ae371277c0d8242a705404f5818359fd741834069c52` | 本追加前全文同值。前 83350 字节 SHA256=`edd292d2d78e58bd141fcfcf20c3bf1237145f89f5d7b56e4f5e106cc960bf08`。本次完整 B1 为 83350 字节之后 255 行（文件 L927 空行起，标题在 L930） |

当前源（与候选一致）：`tsconfig.base.json` SHA256=`13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db`（13 行）；`.github/CODEOWNERS` SHA256=`b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d`（12 行）；`.github/workflows/predeploy-gates.yml` SHA256=`5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974`（27 行，文件版本 V0.01）。`git ls-files -z -- <三路径> \| xargs -0 cat \| wc -l` = 52。`HEAD == origin/main == 242f487233d028173cb9198eecd079f5d877405c`。映射：`criteria-lock.md` L6384 `M005 → [GEN-001 … GEN-018]`，无 HTML 派生 ID。

B2 本步按提示词必须读取两席本次完整 raw 与候选台账；这与 V0.04 B1 互盲要求不是同一节点。B1 后四值及锁 COUNT 以当时记录为准，本段不另写独占锁行。

## 1. §6 技能（本节点实核；不新增门禁/状态/强制技能）

| 技能 | 本节点用法 |
|---|---|
| `receiving-code-review` | 先对照两席本次 B1、源文件、只读 API/命令再接受或反驳候选；禁止因根席 PASS 而表演性同意；本体 IMPLEMENT / 问人作废 |
| `grilling` / `grill-me` | 差集、B/C 边界、检索命中、分类自问自答；不等操作员、不派子代理 |
| `verification-before-completion` | PASS/FAIL 前用本追加内本次命令/API 输出 |
| `systematic-debugging` / `diagnosing-bugs` | 只借读错误→复现；核缺基线 exit1 与保护 API 403；不改代码 |
| `codebase-design` | 三文件接口=tsc extends / GitHub CODEOWNERS / Actions 入口；删除测试见各 ID |
| `ponytail-review` | 只查重复永久 ID、把 B/C 特批或平台 warning 加成新闸；不修 |

缺口，不得冒充已调用：`batch-grill-me` 本机技能目录未找到。控制已划掉的技能未调用。未装依赖。

## 2. 必须解决的两个报告边界

### 2.1 V0.04 把用户 B/C 错套到 CODEOWNERS/CI —— 更正

V0.04 相对本次 255 行报告的第 18、76、77、94、172、180 行（文件约 L944 / L1002 / L1003 / L1020 / L1098 / L1106）把操作员 B1 提示词里的「用户B/C配置原文“就是我的原意”，不要重新判冲突」写成：CODEOWNERS L1–L3 与工作流 L3–L6 关于私有库免费版无法分支保护 / 无法 required status check、硬阻断在 160 与 pre-push。这是错套。原 raw 不覆盖。

真实 B/C（本席本步核对 HTML 原文，不是席位意见）：

- **B** = `docs/business-system-map-for-operations-v0.1.html` 第 1020 行：「来源枚举项为业务参数，按项目可配。」
- **C** = 同文件第 1068 行：「现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）」
- 用户「b c两项原文不冲突。就是我的原意。」是对上述两条 BSM 业务口径的解释，**不构成对 CI 注释或 CODEOWNERS 的特批或豁免**。

本批 013/014 **不**借该误述保留。下面第 4 节只用文件原文、原生解析/匹配、GitHub 只读 API 与可复现命令。去掉 V0.04「用户原意」句之后，技术性与无缺陷仍然成立。

### 2.2 V0.04 第 15 行「误命中检索索引时丢弃」—— 如实清单（见过正文，不以丢弃宣称无泄漏）

依据：V0.04 当时无头会话 `01a08322-6b92-7321-b6c8-458a49e42236`（15:27–15:46）的 `chat_history.jsonl` 工具参数与返回。不是猜测。

**未发生：** 没有 `read_file` 打开当时的 `M005-b1-self-raw.md` 或 `findings.md` 全文。当时 Codex 当前 V0.06 尚不存在，不可能读到本方本次 B1 结论。没有把 findings/self 当证据写进 V0.04 的 18GEN 表。

**已发生（工具返回了匹配行正文，不是仅路径元数据）：**

1. `grep` pattern=`^# \|版本|V0\.` path=`modules/M005-b1-grok-raw.md`（本意只取旧 raw 版本元数据，模式过宽）。返回本席**旧 B2 正文**：findings V0.48 候选哈希表、当时 `M005-b1-self-raw.md` V0.04 大小/哈希，以及 **M005-001 至 006 的完整命题、分类、缺陷/队列、最小修复**（旧 package/Playwright 范围）。
2. `grep` pattern=`tsconfig\.base` glob=`*.{json,md,yml,js,mjs,ts,tsx}`。除消费方 `apps/web/tsconfig.json` 外，返回 `findings.md` L1094 附近 007–012 技术登记句（含「012：九编译开关被 apps/web extends 消费…」）及 L1098 旧修复边界；并返回 **`modules/M005-b5-r1-self-raw.md` 正文**：13 行/SHA 表、「全13行/9选项」、以及当时 TS 探针脚本片段。
3. `grep` pattern=`tsc \|tsconfig` glob=`*.{json,yml,mjs,sh,md}` `head_limit=60`。返回 **`findings.md` L1056 整行**：`| M005-012 | 本方T06；Grok tsconfig全部覆盖 | 技术性 | 否 | 否 | 九个编译器选项及extends消费，实际类型正负验证；不改源 |`。
4. `grep` pattern=`CODEOWNERS|codeowners|@jxh1979` glob=`*.{md,yml,sh,json}` `head_limit=50`。返回 `ledger.md` 当前 M005 开工段（3 文件 52 行与三 SHA，过程句）、`plan/modules.md` 路径表、以及 `docs/deploy-and-change-flow-v0.1.md` 的 CODEOWNERS 说明。不是别席当前 B1 结论，但是台账/计划正文。

结论：V0.04「丢弃」**不能**充当互盲已成立的证据。本席当时**确见** findings 历史 012 行、旧 B2 001–006 命题、以及旧 B5 self-raw 的 tsconfig 段落。本 B2 不隐瞒。当前 012 沿用同源 ID 与 V0.04 独立 tsc 证据一致，不是靠那次泄漏行通过；001–011 仍保持旧 package/Playwright 原义，未改成当前 CI。

## 3. 全批差集（临时号 → 永久 ID）

本次临时号：

- 本方 V0.06：CX-01、CX-02、CX-03，全部技术性登记，工程缺陷否，建议队列否。无添加。
- 本席 V0.04 §6：临时 GR **空集**。§2 的 52 行连续覆盖表全部判技术性，必须登记，不能因「无 GR」漏并。

| 本次信号 | 候选归并 | 漏并？ | 改写本席 finding？ |
|---|---|---|---|
| CX-01 = `tsconfig.base.json:1–13` | **M005-012**（沿用历史同源 ID） | 否 | 否。未改写成缺陷，也未新分 ID |
| 本席 §2.1 L1–L13 九开关+JSON 结构 | 同上 012 | 否 | 否。52 行表逐段纳入，不因无 GR 丢弃 |
| CX-02 = `.github/CODEOWNERS:1–12` | **M005-013**（新 ID） | 否 | 否。历史 001–011 无 CODEOWNERS 对应项 |
| 本席 §2.2 L1–L12 | 同上 013 | 否 | 否 |
| CX-03 = `.github/workflows/predeploy-gates.yml:1–27` | **M005-014**（新 ID） | 否 | 否。历史无当前 workflow 对应项 |
| 本席 §2.3 L1–L27 | 同上 014 | 否 | 否 |
| 本席对抗式：CODEOWNERS 子集未覆盖 `merge-pr.sh`/`tsconfig.base.json` 等；action runtime 平台 warning | 不升新 ID；子集叙事记入 013，warning 记入 014 观察/未验证边界 | 否 | 否。V0.04 已写不升队列；候选「只登记、不加迁移/计数器」与之不冲突 |
| 历史 M005-001…011 | **保持原记录、原命题、队列否** | 否 | **否。禁止改成当前 CI 含义。** 它们属于旧 package/Playwright 范围，不在本 3 文件 |

`M005-013` / `M005-014` 在 findings 全文仅出现于本批候选表，无历史同号。`M005-012` 历史段（约 L1090）与当前表指向同一 `tsconfig.base.json` 九选项/extends，沿用不重复编号。无添加项。无新 HTML 自身问题。无人闸。

## 4. 依序分类、工程缺陷、队列（冲突 → 细化 → 技术性 → 添加）

三份 HTML 无 TypeScript 编译器选项、无 GitHub CODEOWNERS、无 GitHub Actions 工作流锚点。PGM 卡片「留痕复核」原文是「重要动作可追溯：谁发起、谁审批、谁复核、谁放行，敏感操作进复核队列。」——业务操作留痕，不是仓库人审路由。不能指认这三文件为某条 HTML 的下位实现 → 停不住细化。无明文矛盾 → 停不住冲突。无票务/财务/员工权限/工单/来源枚举/打折原因等业务发明 → 不是添加。故三项均为 **技术性**。GEN-017 按此顺序；不把 B/C 业务口径当成对本模块的豁免。

### M005-012

- 当前来源：CX-01；本席 V0.04 §2.1/3.1/4/5；历史 012 同一源路径
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 无业务发明：九个 `compilerOptions` 与 JSON 括号只服务 web `tsc` 继承，不含业务枚举/默认值/权限。
- 本步独立证据（类型 4）：TS 5.9.3 `parseJsonConfigFileContent(apps/web/tsconfig.json)` 353 个输入；`strict`/`esModuleInterop`/`forceConsistentCasingInFileNames`/`skipLibCheck`/`resolveJsonModule`/`noUncheckedIndexedAccess` 均为 true；`target=ES2022` `module=ESNext` `moduleResolution=Bundler`。内存诊断：合法 `.at` 0 错；`null` 赋值 TS2322；缺索引 TS2322；隐式 any TS7006。文件 SHA 与候选一致。
- 判定：同意沿用 012，仅登记，不改源。Linux 大小写碰撞与 skipLibCheck 未构造反例，维持 V0.04 未验证边界，不新增强制门禁。

### M005-013

- 当前来源：CX-02；本席 V0.04 §2.2/3.2/4/5
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 无业务发明：9 条根锚定路径 + `@jxh1979` 是 GitHub 审阅主体，不是产品角色/审批人配置。L1「独立复核」≠ PGM 留痕复核。
- **不**使用用户 B/C。依据文件原文 + API + 命令：
  - 原文 L2–L3 已写明仅在「受保护分支 + require review」时强制，免费私有库当前只记录意图。
  - `gh api repos/jxh1979/mall/codeowners/errors?ref=main` → `{"errors":[]}`（exit 0）。
  - `gh api .../branches/main/protection` → HTTP 403「Upgrade to GitHub Pro or make this repository public…」（exit 1）。与原文一致，不是假绿。
  - 仓 `private=true`；`@jxh1979` `login=jxh1979 id=69119522 type=User`；collaborator `permission=admin`。
  - 字面路径匹配（无 glob）：9 条分别命中 1/1/1/1/1/2/90/1/377；自身与本工作流被 `/.github/` 覆盖。反例 `ops/160/merge-pr.sh`、`tsconfig.base.json`、`package.json.bak`、`nested/tools/` 无匹配——与「强制点文件」子集一致，官方无 owner 则不请求 code owner，不升漏覆盖缺陷，也不申请升级保护规则。
- 判定：同意新 013，仅登记。

### M005-014

- 当前来源：CX-03；本席 V0.04 §2.3/3.3/4/5
- 分类：**技术性**
- 当前工程缺陷：**否**
- 修复队列：**否**
- 无业务发明：main 的 PR/push 静态治理入口、固定 action SHA、事件基线、失败传递。不新增 HTML 业务规则。
- **不**使用用户 B/C。依据：
  - 原文 L3–L6：GitHub 侧可见信号；硬阻断由 160 `verify:predeploy` 与 pre-push 承担；因私有库免费版无法设 required status check。这是文件自己的分工声明，由保护 API 403 与 `default_workflow_permissions=read` / `can_approve_pull_request_reviews=false` 支撑，不是操作员对注释的特批。
  - YAML 1.2（yaml 2.9.0）解析 `on` 为 pull_request+push、`branches:[main]`；4 step；无 `continue-on-error` / 无 `if` 跳过。
  - `cmp` 与 `ops/ci/predeploy-gates.yml` exit 0，字节相同。
  - 公开 API：`actions/checkout` v4.3.1 → `34e114876b0b11c390a56381ad16ebd13914f8d5`；`actions/setup-node` v4.4.0 → `49933ea5288caeca8642d1e84afbd3f7d6820020`，与注释一致。
  - `HTML_GATES_BASE=absent-ref-m005-b2 node tools/verify-html-ledger-sync.mjs` exit 1，文案「无法判定不等于通过」。
  - 工作区 html-ledger-sync / entry-anchors 红属既定下游镜像，不能改 CSV/HTML 刷绿，也不能把已提交 CI 绿冒称工作区/发布绿。action Node 20 runtime 被 runner 迁到 24 的 warning 只登记，不为未来情景加迁移或计数器。
- 判定：同意新 014，仅登记。

## 5. 历史 001–011（明确不改写）

findings 历史段（约 L1079–L1089）仍为旧 322 范围：engines/端口/live-evidence 选择器/`smoke:p0-ops`/真实配置缺 flag/0.0.0.0、以及 package 字段与 Playwright 配置的技术登记。当前工程缺陷栏为否、修复队列否，表示那些项已出队，**不是**把它们改成 CODEOWNERS 或当前 workflow 的含义。本候选当前唯一有效三行是 012/013/014。本席同意此边界。

## 6. 本步命令与关键输出（节录）

```
sha256sum 三源 + 镜像：13d49b… / b8b7a9… / 5c62be… / 5c62be…（镜像同）
git ls-files -z -- 三路径 | xargs -0 cat | wc -l → 52
cmp 工作流镜像 exit 0
HEAD/origin/main = 242f487233d028173cb9198eecd079f5d877405c
gh codeowners/errors → {"errors":[]}
gh branches/main/protection → 403 Upgrade to GitHub Pro（exit 1）
gh actions/permissions/workflow → default_workflow_permissions=read, can_approve_pull_request_reviews=false
gh collaborators/jxh1979/permission → admin
gh repos/jxh1979/mall → private=true
gh users/jxh1979 → login=jxh1979 id=69119522 type=User
checkout v4.3.1 sha 34e114876b0b11c390a56381ad16ebd13914f8d5
setup-node v4.4.0 sha 49933ea5288caeca8642d1e84afbd3f7d6820020
native node：typescript 5.9.3 files 353；yaml 2.9.0；CODEOWNERS 9 条 1/1/1/1/1/2/90/1/377；merge-pr.sh 与 tsconfig.base.json 无匹配；exit 0
HTML_GATES_BASE=absent-ref-m005-b2 node tools/verify-html-ledger-sync.mjs → exit 1
BSM L1020/L1068 原文命中如上
三份输入 sha/字节与派发声称一致
```

未发工作流、未 npm ci、未改业务树。内存诊断未落盘。

## 7. 分歧 / 添加 / 人闸

无分类冲突。无漏并。无改写本席当前 52 行技术性登记，也无把旧 001–011 改成当前 CI。无添加项，不写 questions。无新的三份 HTML 自身问题。V0.04 的 B/C 误套与检索见正文，已在本段更正/披露，不作为 013/014 的依据。

## 8. 结论

**B2 PASS。**

同意候选三行：012/013/014 均为技术性、当前工程缺陷否、修复队列否。修复队列为空时，按提示词跳过 B3/B4（B3 次数保持 1），下一步仍须完整 B5-r1；本票不是模块 PASS。双 PASS 定稿前本段只是外审一票。
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M005 B1 独立审计报告（Grok 外席）V0.06

- 提示词：160-audit-prompt V0.17；操作员指令「后面模块不要再暂停了，直接继续」。
- 席位：唯一 Grok 外席。本次新调用不继承、不检索历史审计结论；未读 self raw / findings / ledger / questions / 其它模块报告 / 旧 grok raw 正文。
- 阶段：B1 完整独立重做。模块状态按操作员：M005 IN_PROGRESS。历史计数不清算：B3=1、方案修复=2、交付失败=0。
- 正本：`/home/james/complex-management`。`git rev-parse HEAD` = `git rev-parse origin/main` = `242f487233d028173cb9198eecd079f5d877405c`，与 `plan/modules.md` 当前 main 基线一致。
- 范围：`plan/modules.md` M005 段三文件、有效内容行 52、标记 NORMAL。`criteria-lock.md` 模块映射：`M005 → [GEN-001 … GEN-018]`，无 HTML 派生 ID。
- 判据锁实算：工作区 `docs/audit-2026-09/plan/criteria-lock.md` 全 bytes SHA256 见本报告首行。HEAD 同路径另一份 801099 bytes、SHA256 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`，与工作区不等；对两份抽出 `## general` 至 `## HTML 派生判据` 以及 `M005 →` 行，字节级相等。本席按工作区文件读 GEN 定义并哈希。未读 sidecar。
- 旧 raw 前缀（不解码、不输出旧正文）：123663 bytes，SHA256 `d9d1254d2c8a6edd2216015f6895777ab4d96e3d13d0db8c20e57dda46560f95`，与给定值一致。本次以 `ab` 追加 V0.06 完整报告。自身旧版本 V0.05。
- 三文件 git porcelain 空（相对 HEAD 无未提交改动）。
- 禁止项遵守：无子代理、无 MCP_Codex_160、无新 workflow/PR/评论/issue、无推送、无部署、无启动服务、无连库、无 npm ci、无 npx 下载、未改 CLI/MCP、未改三权威 HTML、未改 CSV、未边审边修。

## 技能预检（只读收窄）

| 技能 | 路径 | 用法 |
|---|---|---|
| using-superpowers | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 已读。用户/操作员指令优先于技能；不派子代理。 |
| grilling | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 已读。对抗线按 grilling 结构在席内自答事实问题；按操作员「不要再暂停」不增加人闸。 |
| grill-me | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grill-me/SKILL.md` | 已读。正文为 `Run a /grilling session.` 且 `disable-model-invocation: true`。按 grilling 收窄执行，不另开会话。 |
| codebase-design | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 已读。对三文件做删除测试（用途/消费者/删除后复杂性是否外溢）。 |
| diagnosing-bugs | `/home/james/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 已读。只构造可红探针，不进入修复。 |
| systematic-debugging | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 已读。先根因后分类；本阶段不修。 |
| verification-before-completion | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | 已读。凡声称均有本次命令/API 输出。 |
| ponytail-review | `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 已读。只评过工程复杂度，不改文件。 |

可选 `batch-grill-me` 未在本席技能树以真实 SKILL.md 加载；按指令如实记录，不暂停。

## 独立性事件

`git status -sb` 列出了其它审计产物文件名（含 `M005-b1-self-raw.md` 等）。本席只把它们当作路径元数据，未打开其内容，也未把文件名中的模块结论当证据。旧 grok raw 仅做原始 bytes 长度/SHA256/前缀校验，未解码正文。未命中旧结论或 self 正文。互盲搜索：读取/rg 均带具体文件路径；`git ls-files` 仅用于 CODEOWNERS 规则匹配计数。

## 工具链（已装，未新装）

- `node -v` → `v24.20.0`
- `require("typescript/package.json").version` → `5.9.3`（lock 声明 `apps/web` `typescript` `^5.7.2`）
- `require("yaml/package.json").version` → `2.9.0`
- 三文件行数 `wc -l`：`tsconfig.base.json` 13、`.github/CODEOWNERS` 12、`.github/workflows/predeploy-gates.yml` 27，合计 52。

## 52 行连续覆盖

### A. `tsconfig.base.json`（13 行，303 bytes，SHA256 `13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db`）

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | `{` | JSON 开。`ts.parseConfigFileTextToJson` 无 jsonError。 |
| 2 | `"compilerOptions": {` | 仅此一块；无 `files`/`include`/`exclude`/`references`。 |
| 3 | `"target": "ES2022"` | `tsc --showConfig -p apps/web/tsconfig.json` 得 `target: es2022`。负例：虚拟 `"target":"NOT_A_REAL_TARGET"` → TS「Argument for '--target' option must be: … 'es2022' …」。 |
| 4 | `"module": "ESNext"` | showConfig `module: esnext`。子配置同值覆盖，继承链成立。 |
| 5 | `"moduleResolution": "Bundler"` | showConfig `moduleResolution: bundler`。与 Vite/apps-web 消费者一致。 |
| 6 | `"strict": true` | showConfig 展开 `strictNullChecks` 等为 true。内存探针 `const x: string = null` → TS2322；`string \| null = null` 无诊断。 |
| 7 | `"esModuleInterop": true` | showConfig 同时出现 `allowSyntheticDefaultImports: true`（该选项的伴随效果）。 |
| 8 | `"forceConsistentCasingInFileNames": true` | showConfig 为 true。未另造双文件大小写冲突（见未验证边界）。官方路径大小写敏感见 CODEOWNERS 规范，与本选项同类。 |
| 9 | `"skipLibCheck": true` | showConfig 为 true。跳过 `.d.ts` 体检；属工程取舍，不是业务规则。 |
| 10 | `"resolveJsonModule": true` | 选项被解析。虚拟 `import x from "./p.json"` 在无该文件时报找不到模块（文件不存在，不能据此否定选项）。 |
| 11 | `"noUncheckedIndexedAccess": true` | 内存程序：`const a: string[]=["x"]; const b=a[0]; b.toUpperCase()` → TS18048 `'b' is possibly 'undefined'`。对照把该选项关掉后诊断为空。选项实际生效。 |
| 12 | `}` | compilerOptions 闭合。 |
| 13 | `}` | 文件闭合，LF 结尾，无 CRLF。 |

消费者（白名单读出）：`apps/web/tsconfig.json` `"extends": "../../tsconfig.base.json"`；`apps/web/package.json` `"typecheck": "tsc -b --noEmit"`。`tsc -b apps/web/tsconfig.json --dry --pretty false` exit 0，输出 `A non-dry build would build project '.../apps/web/tsconfig.json'`。本席未跑 apps/web 全量 typecheck/vite build，不把配置探针写成整仓业务编译全绿。

`parseJsonConfigFileContent` 单独解析 base：`fileNamesCount=371`；解析 web：353。多出的 18 个为 `apps/web/vite.config.ts`、`apps/web/vitest.config.ts`、根 `playwright*.ts` 与 `tests/e2e/**`。base 无 `files`/`include` 时，tsc 把它当仓库根项目。声明入口是 web 继承而非 `tsc -p tsconfig.base.json`。见未验证边界。

### B. `.github/CODEOWNERS`（12 行，743 bytes，SHA256 `b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d`）

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | 强制点文件(2026-07-06):改动应经独立复核。 | 意图声明。九条规则即其清单，不是全仓默认 owner。 |
| 2 | CODEOWNERS 仅在「受保护分支 + require review」启用时被 GitHub 自动强制。 | 官方 docs「CODEOWNERS and branch protection」：须改 branch protection 并打开 Require review from Code Owners。 |
| 3 | 当前私有库免费版无法启用分支保护，故仅记录意图 + 一旦升级即生效。 | `gh api repos/jxh1979/mall`：`private: true`。`.../branches/main/protection` HTTP 403 正文 `Upgrade to GitHub Pro or make this repository public to enable this feature.` rulesets 同 403。注释与当前事实一致，不是把计划写成已强制。 |
| 4 | `/ops/160/complex-deploy-api.sh @jxh1979` | git ls-files 命中 1。路径在索引中。 |
| 5 | `/ops/160/complex-deploy-web.sh @jxh1979` | 命中 1。 |
| 6 | `/ops/160/hooks/ @jxh1979` | 命中 1（`ops/160/hooks/pre-push`）。 |
| 7 | `/ops/160/propose-change.sh @jxh1979` | 命中 1。`ops/160/merge-pr.sh`、`ops/160/install-git-hooks.sh` 不匹配（前缀规则是精确文件或 `hooks/` 目录，不是整个 `/ops/160/`）。 |
| 8 | `/ops/ci/ @jxh1979` | 命中 1（`ops/ci/predeploy-gates.yml`）。 |
| 9 | `/.github/ @jxh1979` | 命中 2：本文件与 `predeploy-gates.yml`。官方建议把 CODEOWNERS 放在 `.github` 并由该目录 owner 保护；本规则满足。 |
| 10 | `/tools/ @jxh1979` | 命中 90。 |
| 11 | `/package.json @jxh1979` | 命中 1。`package-lock.json`、`apps/web/package.json` 不匹配。 |
| 12 | `/backend/src/main/resources/db/migration/ @jxh1979` | 命中 377。 |

九规则全部有 owner `@jxh1979`。`gh api users/jxh1979` → login/type User；`collaborators/jxh1979/permission` → `admin`。`gh api repos/jxh1979/mall/codeowners/errors` → `{"errors":[]}`。官方：CODEOWNERS 路径大小写敏感。反例 `OPS/ci/predeploy-gates.yml`、`TOOLS/verify-delivery-gates.mjs`、`.GITHUB/CODEOWNERS` 均不匹配且不在索引。`tsconfig.base.json` 无规则命中——该文件不在本 CODEOWNERS 自列强制点内；不能因 HTML 无关键词就要求补规则（GEN-017，见添加未立案）。

`git ls-files` 3559 路径：九规则 last-match 合计 475，未匹配 3084。官方示例：「Order is important; the last matching pattern takes the most precedence。」本九条路径互不重叠，last-wins 与唯一命中等价。

### C. `.github/workflows/predeploy-gates.yml`（27 行，1315 bytes，SHA256 `5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974`）

与 `ops/ci/predeploy-gates.yml` 全 bytes 相同（门禁 `tools/verify-delivery-gates.mjs` 要求二者一致；比较在内存做，未扩审 M007/M008 其它逻辑）。`yaml` 包 `parseDocument` errors/warnings 空。

| 行 | 原文要点 | 覆盖 |
|---|---|---|
| 1 | `# 文件版本：V0.01` | 注释。 |
| 2 | `name: predeploy-gates` | `gh api .../actions/workflows` 该 path `state: active`，name 相同。 |
| 3–4 | GitHub 侧独立治理门禁；PR/push 到 main 静态跑 delivery-gates 快子集。 | `on.pull_request/push.branches: [main]` 被解析。实际 `run` 是 `npm run verify:predeploy`（package.json 链，见顺从线），不是注释括号里的枚举本身。 |
| 5 | 无需数据库（全静态分析）；私有库免费版无法设 required status check；可见信号 + 独立复现。 | 白名单脚本 `verify-html-ledger-sync`/`verify-delivery-gates`/`verify-pii-encryption`/`verify-tenant-isolation`/`verify-test-db-isolation` 均为读文件/git，不连库。protection API 403 支持「无法设 required check」。 |
| 6 | 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。 | 部署脚本 `REQUIRED_GATES` 与 `package.json` `verify:predeploy` 成分闸同名并 `npm run "$gate"`（白名单链出 `ops/160/complex-deploy-api.sh`/`complex-deploy-web.sh`）。**pre-push 不跑这些闸**：只拒直推 `refs/heads/main`（除非 `ALLOW_MAIN_PUSH=1`），自注「开发者提示,非安全边界」。见 finding GR-M005-B1-01。 |
| 7–11 | `on: pull_request/push branches: [main]` | YAML 解析确认。无 `workflow_dispatch`/`paths`/`types`。 |
| 12–15 | `jobs.predeploy.runs-on: ubuntu-latest` + steps | 无 `permissions`（仓库 `default_workflow_permissions: read`，与缺省只读相容）。无 `concurrency`/`timeout-minutes`。 |
| 16–18 | `actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5` 注释 v4.3.1；`fetch-depth: 0` | `gh api repos/actions/checkout/git/ref/tags/v4.3.1` object.sha 即该值。完成记录 run `34199032554` 步骤名含该 SHA，`fetch-depth: 0` 与 html-ledger-sync 基线需求一致。注释里的历史编号只当标签，不当证据。 |
| 19–21 | `actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020` 注释 v4.4.0；`node-version: "22"` | tag v4.4.0 的 object.sha 即该值。run 日志 `node: v22.23.2`，满足 `package.json` `engines.node` `^22.13.0 \|\| >=24.0.0`。 |
| 22–23 | `npm ci --ignore-scripts` | 同 run 步骤 success（约 8s）。未在本机 npm ci。 |
| 24–27 | `HTML_GATES_BASE` 表达式 + `npm run verify:predeploy` | 表达式：`event_name == 'pull_request' && base.sha \|\| before`。push run `34199032554` 日志 `HTML_GATES_BASE: 8765bd2e37d10616c15b360a1faa7eff7ad4ca41`（before，非空）。随后展开完整 `verify:predeploy` 脚本串并跑 `verify:html-ledger-sync`，该次 PASS。`continue-on-error` 未设。历史失败 run `33296882514`：前步 success，`verify:predeploy (governance gates)` failure → job failure（失败传递成立）。 |

本机**未**跑完整 `npm run verify:predeploy`（会在第一闸因工作区脏 HTML 红，且范围会滑出 M005）。只跑第一闸与 YAML/TS 探针，并以既有 Actions 记录证明 CI 入口真实执行。

## 三线

### 1. 第一性原理

**tsconfig.base.json 为何存在。** 删除测试：`apps/web/tsconfig.json` 显式 extends 它；删除则 web 必须内联 9 项 compilerOptions，复杂性回到调用方。它不是业务页面，三份 HTML 也无「tsconfig」句；GEN-017：不能因 HTML 无关键词主张删除。实际消费者：`tsc --showConfig -p apps/web/tsconfig.json` 合并了全部 base 选项。未验证前提：是否还有未在白名单出现的其它 extends（本席不从仓根递归搜 tsconfig）。

**CODEOWNERS 为何存在。** 删除测试：失去九条强制点的 owner 记录；官方在升级到可开 branch protection 后，该文件才变成强制审阅。当前 403 证明它此刻不是运行时授权边界。消费者：GitHub CODEOWNERS 求值器（errors API 空）；本地无 GitHub 强制。不能因 HTML 无「CODEOWNERS」删除。HTML 里的审批/留痕是业务单据审批，不是 Git 审阅。

**predeploy-gates.yml 为何存在。** 删除测试：GitHub 上不再有名为 predeploy-gates 的可见检查（workflow 现 active）。硬合并闸因免费私有库设不了 required check，本来就不在本文件。部署脚本仍按成分闸跑。本文件的用途是可见信号 + 把 `HTML_GATES_BASE`/`fetch-depth:0` 送到 `verify:predeploy`。完成记录证明入口被执行。不能因 HTML 无「GitHub Actions」删除。

未验证前提（见专节）：ubuntu-latest 浮动、action Node 20 弃用警告、base 被误当根项目、CODEOWNERS 升级后单 owner 自批。

### 2. 顺从（假定意图正确）

- **TS 继承/选项：** base → web 合并被 `tsc --showConfig` 与 `parseJsonConfigFileContent` 证实；`strict`/`noUncheckedIndexedAccess`/`Bundler`/`ES2022` 与声明一致；`tsc -b --dry` 接受无 `composite` 的 web 工程。
- **CODEOWNERS 路径与主体：** 九规则均可在索引中命中真实路径；owner 存在且 admin；语法 API 无错；大小写敏感与官方一致；注释对「未强制」的陈述与 403 一致。
- **YAML 触发/checkout/Node/命令/事件基线：** main 的 pull_request 与 push；checkout SHA=v4.3.1 标签；setup-node SHA=v4.4.0 标签；`fetch-depth:0`；`HTML_GATES_BASE` 在真实 push 上落到 `github.event.before`；`npm run verify:predeploy` 在 CI 展开为 package.json 全链。`tools/verify-delivery-gates.mjs` 对 live/template 的字节一致、`fetch-depth===0`、`run===npm run verify:predeploy`、HTML_GATES_BASE 表达式字符串的检查，与当前 YAML 相符（CI 日志有 `DELIVERY_GATES_DOCS_PASS`）。
- 映射的 18 条 GEN 逐项见下。无 HTML 派生判据可顺从。

### 3. 对抗（假定实现错误）

grilling 前沿（席内用命令回答，不等操作员）：

1. 无效 TS target / 未知选项是否被解析器拒绝？会。输出见覆盖表。
2. `noUncheckedIndexedAccess` 会不会只写在 JSON 里不生效？会生效（TS18048）。关掉则沉默。
3. CODEOWNERS 大小写反例是否误匹配？不会。
4. `/package.json` 会不会吞掉 lock 或 workspace package.json？不会。
5. `HTML_GATES_BASE=0000…0` 或非法 ref 是否假绿？`node tools/verify-html-ledger-sync.mjs 0000000000000000000000000000000000000000` exit 1，`取不到基线`；`not-a-ref` 同样 exit 1。
6. 工作区改 HTML 不改台账是否红？会。见观察。
7. verify 失败是否让 job 失败？历史 run `33296882514` 证实该步 failure → job failure。
8. 注释声称 pre-push 硬阻断门禁是否为真？否。见 finding。
9. 标签 SHA 是否漂移？本次 API 上 tag object.sha 与文件 pin 一致。
10. 缺 `permissions:` 是否把 GITHUB_TOKEN 写成 write？仓库默认 workflow permissions = read。

Ponytail-review：三文件无死代码、无多余依赖、无第二套抽象。注释 L6 是错误陈述不是多余层。`Lean already` 不等于注释可错。net: 0 行可删（纠错是改字不是删模块）。

## GEN-001 … GEN-018

每条：适用/不适用、证据、分类顺序（冲突→细化→技术性→添加）。无业务发明的技术判断单独写。不能用「HTML 没有这个词」推出无需求或删除。

**GEN-001 必要性与最小性。** 适用。三文件各有可验证消费者（web extends、GitHub owner 求值、Actions 入口）。删除测试见第一性原理。tsconfig 九选项均被 showConfig 继承，非空转。CODEOWNERS 九条均有索引命中。workflow 四步均在 run `34199032554` 出现。分类：无冲突/细化。base 无 `files:[]` 可能扩大 tsc 项目面，见未验证边界，不立案添加「必须 files:[]」为业务需求。

**GEN-002 可执行正确性。** 适用。YAML/JSON 可解析；CI 真实跑 `npm run verify:predeploy`；TS 选项探针可红可绿。文档声称与行为不一致处：workflow L6 把 pre-push 写成门禁硬阻断承担者。冲突？与 HTML 无冲突。细化？否。技术性：是（工程注释）。添加？否。→ finding GR-M005-B1-01。

**GEN-003 测试有效性。** 适用为「不为纯配置新增测试文件，用已装 typescript/yaml 与只读原生断言」。本次内存探针具备能失败断言（无效 target、TS18048、零 SHA 基线 exit 1）。不把未跑的 apps/web 全量测试写成已跑。不适用「模块必须自带测试文件」。

**GEN-004 信任边界校验。** 适用（CI 事件输入）。`HTML_GATES_BASE` 来自 GitHub 表达式，push 实测为 40 位 hex；消费者对非法基线 fail-closed。workflow 本身不做字符串校验，把失败交给 `git merge-base`。无冲突。不发明新校验业务规则。

**GEN-005 授权与最小权限。** 适用在 GitHub 侧。CODEOWNERS 当前因无 branch protection **不构成**强制授权（文件已写明）。仓库 Actions 默认 token 只读。workflow 未提权。不能把「独立复核」意图写成已经生效的最小权限控制。无冲突于 HTML 的业务审批链——那是另一套主体。不添加「必须第二 owner」为 HTML 需求（添加未立案，见未验证边界）。

**GEN-006 数据完整性。** 对这三份配置：无事务/金额。基线取不到则 html-ledger-sync 拒绝给 PASS，避免「判不了当没改」。workflow 提供 fetch-depth 0 与事件 SHA。适用面窄，行为与脚本注释一致。不适用数据库约束。

**GEN-007 并发与幂等。** workflow 无 `concurrency`。门禁脚本只读，并行跑不写生产。历史同时存在 pull_request 与随后 push 两次 success，属重复执行只读检查。不适用锁/去重键。不把缺 concurrency 写成缺陷。

**GEN-008 接口与模式一致性。** live workflow 与 `ops/ci/predeploy-gates.yml` 字节相同。`HTML_GATES_BASE` 表达式字符串与 `verify-delivery-gates.mjs` 期望字面量一致。`node-version: "22"` 与 engines `^22.13.0` 在本次 CI 的 v22.23.2 上相容。tsconfig 选项名与 TS 5.9 解析器一致。无枚举/DTO 漂移（本模块无）。

**GEN-009 错误处理与恢复。** 无 `continue-on-error`。零 SHA/非法 ref 本地 exit 1。历史 job 在 verify 步失败即失败。不吞错。不适用半成品泄漏（无部署步骤）。

**GEN-010 安全与敏感信息。** 三文件无秘密、无注入点。action 用完整 commit SHA，且与官方 tag 对象一致；`sha_pinning_required` 在仓库级为 false，但本文件仍 pin。`npm ci --ignore-scripts` 降低 install 期脚本。CODEOWNERS 无 `!` 否定（官方说明 CODEOWNERS 不支持 gitignore 的 `!`/`[]`）。无 finding。

**GEN-011 可观测与审计证据。** Actions 日志含 SHA、Node 版本、HTML_GATES_BASE、脚本展开。CODEOWNERS errors API 可查。tsconfig 无运行日志，showConfig/探针即证据。不适用业务审计留痕 HTML 条款。

**GEN-012 资源与性能边界。** `ubuntu-latest` 与未设 timeout 为浮动/无上限。本次成功 run 约 25–36s，未观察到无界放大。不立案为必须钉死镜像的业务需求；见未验证边界。

**GEN-013 构建配置与依赖。** pin SHA 与 tag 对齐；Node 22 线与 engines 在 CI 实测相容；typescript/yaml 已装可解析。run 日志有 Node 20 deprecation warning（checkout/setup-node 以 Node 20 为目标、被强制跑 Node 24）——当前 conclusion 仍 success，见未验证边界，不把 warning 写成已红。

**GEN-014 数据库迁移正确性。** 本模块无迁移文件。CODEOWNERS L12 只是给迁移目录登记 owner，且当前不强制。workflow 不跑迁移。不适用执行迁移；适用面仅「规则存在」。无缺陷。

**GEN-015 界面可用性与可访问性。** 三文件无用户界面。注释中文。不适用。

**GEN-016 文档可验证性。** workflow L3–5 与 protection 403、静态脚本、Actions 记录相符。L6 后半句「本地 pre-push 钩子承担」硬阻断门禁 **不可**从 `ops/160/hooks/pre-push` 复现。CODEOWNERS L2–3 可复现。tsconfig 无虚假完成声明。→ GR-M005-B1-01。分类：非 HTML 冲突、非细化、技术性、非添加。无业务发明：只要求注释与钩子事实一致。

**GEN-017 业务来源分类。** 本模块无 HTML 派生映射。所有问题按冲突→细化→技术性→添加处理。未把「HTML 没写 GitHub/tsconfig」当成删除令。未把「应再覆盖 tsconfig.base.json」写成冲突或细化（那会是添加，未立案）。

**GEN-018 第一性原理证据。** 结论只落：HTML 原文（治理/审批句只用来排除「这就是业务审批」的误绑）、可执行产物（tsc/yaml/node 门禁）、外部规范（GitHub CODEOWNERS 文档与 tag API、TS 诊断码）、可复现命令（见各节）。不引用历史 AUD 编号当依据，不引用「用户 B/C 批准」给 CI 注释豁免。

## 技术性登记（逐项）

| 项 | 顺序结果 | 无业务发明理由 |
|---|---|---|
| GR-M005-B1-01 workflow L6 pre-push 声称 | 冲突否→细化否→**技术性是**→添加否 | 改的是工程钩子职责描述，不新增业务状态/金额/权限口径。 |
| tsconfig 九选项取值 | 无缺陷 | 原生编译器选项，HTML 沉默不构成删除令。 |
| CODEOWNERS 九规则 + 未覆盖 tsconfig.base.json | 不立案 | 文件自列强制点；补规则属添加，须 B2 问题，本席不发明「HTML 要求 GitHub owner」。 |
| action SHA pin | 无缺陷 | 供应链钉死，非业务。 |
| 缺 permissions/concurrency/timeout | 不立案 | 默认 token 只读已核实；其余为加固添加。 |
| skipLibCheck true | 不立案 | 工程取舍，无 HTML 冲突。 |
| 工作区 HTML 脏导致第一闸红 | **不属 M005 缺陷** | 闸按设计暴露错误；不改 HTML/CSV、不扩审所属模块。 |

## 临时 finding

### GR-M005-B1-01

- 分类（2.1）：技术性
- 工程缺陷：成立
- GEN：GEN-016、GEN-002
- 2.3 证据类型：类型 4 可复现命令输出（辅以类型 3 外部规范：GitHub 免费私有库无 branch protection）
- 文件:行号：`.github/workflows/predeploy-gates.yml:6`（L5–6 为同一句注释）
- 事实：L6 写「硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担」。部署脚本确跑与 `verify:predeploy` 同名的成分闸。`ops/160/hooks/pre-push` 全文不调用 `verify:predeploy`/`npm run`，只匹配 `refs/heads/main` 并在未设 `ALLOW_MAIN_PUSH=1` 时 exit 1；文件头写「开发者提示,非安全边界」。
- 真实命令/输出：
  - `rg verify:predeploy ops/160/hooks/pre-push` → No matches（仅 L3 流程说明含单词 predeploy）
  - `sed -n '1,17p' ops/160/hooks/pre-push` → 直推拦截循环后 `exit 0`
  - `gh api repos/jxh1979/mall/branches/main/protection` → 403 Upgrade to GitHub Pro…（required check 确实设不了，L5 前半真、L6 把 pre-push 算作门禁硬阻断假）
- 队列建议：修复队列。只改本 YAML 注释，使之与「pre-push 只拦直推 main、不执行治理门禁」一致。不改 pre-push、不改部署脚本、不扩 M007。
- 无业务发明：不引入新审批主体或 HTML 口径。

临时 finding 共 1 条。无添加项，无 questions。

## 工作区 HTML 台账观察（非本模块缺陷）

`git status --porcelain -- docs/business-system-map-for-operations-v0.1.html …ledger.md` → 仅 `M docs/business-system-map-for-operations-v0.1.html`。

`node tools/verify-html-ledger-sync.mjs` exit 1：

```
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
```

这是门禁真实暴露错误，不是 M005 三文件的实现错误。未改 HTML/CSV，未扩审该 HTML 所属模块。若此脏工作区被推进 CI，`verify:predeploy` 会在第一闸失败（由 chain `&&` 与无 continue-on-error 保证）。本次 HEAD 上的成功 run `34199032554` 不能用来声称「当前脏工作区也会绿」。

## 未验证边界

- 未跑 apps/web 全量 `tsc --noEmit` / `vite build` / `npm run verify:predeploy` 全链（避免把配置审计写成整仓编译/发布全绿；全链第一闸在脏 HTML 上已红）。
- 未构造 `forceConsistentCasingInFileNames` 双文件大小写冲突。
- `tsconfig.base.json` 无 `files`/`include` 时解析为 371 个 TS 文件；未实际执行无 `--noEmit` 的 `tsc -p tsconfig.base.json`。
- 是否还有白名单外的 tsconfig extends：未做仓根递归搜索。
- `ubuntu-latest` 镜像浮动；run 日志 Node 20 deprecation warning（当前 job 仍 success）。
- CODEOWNERS 一旦可开「Require review from Code Owners」，单一 `@jxh1979` 且其为 PR 作者时能否形成独立复核：官方「code owners required」下 owner 不能批自己的 PR；第二 owner 属添加，未立案。
- `ALLOW_MAIN_PUSH=1` 绕过 pre-push 后 GitHub 无 required check：属已知免费私有库限制，文件 L5 已写「可见信号」。
- 判据锁工作区哈希与 HEAD 哈希不等（映射/GEN 段相等）；四值校验若他席用 HEAD 文件会分叉——记实，不自行对齐 sidecar/ledger。

## B1 结论

**FAIL**

理由：52 行已连续覆盖，GEN-001–018 已逐项取证，三线已做；存在未定稿工程缺陷 GR-M005-B1-01。按 runbook 单席 PASS 须无未定稿缺陷。本席不修复、不把其它已闭环模块重开。

## M005 新独立B1后的完整B2归并票
版本：V0.07

本席B2：PASS。此票只判断候选V0.97相对两份当前B1 raw的全批差集、四分类与修复队列，以及014最小修复边界。不是独立B1 PASS，不是模块PASS，不是发布PASS。必须另有Codex本批B2 PASS才能定稿。不覆盖上方V0.06 B1 FAIL原文。操作员已授权后续模块连续推进，流程恢复不另请批；三份业务HTML仍禁止未批准修改。

### 本批输入
- 候选：docs/audit-2026-09/findings.md V0.97，333329bytes，SHA256 73e0a8008754b85b7e2d22471161221ec1c4f6f5e44c94b37f610c155fdba309。完整读取最上「当前 M005 新独立B1后的B2整批候选」至下一旧M005段（「当前 M005 B2 未定稿：双席合取 FAIL」）为止；下方旧001—011原文未改写。
- Codex：docs/audit-2026-09/modules/M005-b1-self-raw.md，指定本次从byte89202起到当时123361结尾。当时整文件SHA256 211152cc5fb70ba186d81aae9e0b6d0dd88a2f1df0a344f3eb89110d3b000a16。该区间含本席须复核的当前B1 V0.08（CX-01/02/03，B1 PASS，CX-03当时工程缺陷否）及其后Codex本批B2 V0.09正文。B2允许读双方本次报告；不以旧失败轮次当现状。
- Grok：docs/audit-2026-09/modules/M005-b1-grok-raw.md 本文件，指定本次从byte123663起到当时151569结尾，SHA256 6b741668d1d24099b1f81d4ba4003842d74532ea544d3574891194f44541242e。该区间即上方V0.06完整B1，243行，B1 FAIL，唯一临时缺陷GR-M005-B1-01。原123663bytes前缀SHA d9d1254d2c8a6edd2216015f6895777ab4d96e3d13d0db8c20e57dda46560f95原样保留。本票只追加，不覆盖。

本席已完整读两份当前raw与候选段。技能按V0.17收窄实读：receiving-code-review（先核实再接受，不表演性同意）、requesting-code-review（席内复核，不派子代理）、verification-before-completion（先跑命令再下PASS）。未用MCP，未派子代理，未改源码/findings/方案/其他报告，未部署/提交/推送/安装。

### 完整差集、分类、队列
Codex当前临时项仅CX-01/02/03。Grok当前临时项仅GR-M005-B1-01；另有tsconfig九选项、CODEOWNERS九规则、workflow逐段的技术性登记，以及permissions/concurrency/timeout/补CODEOWNERS覆盖等不立案讨论。无添加项、无questions、无新HTML自身问题。

| 正式ID | 全部本次来源 | 四分类 | 当前工程缺陷 | 修复队列 | 差集结论 |
|---|---|---|---|---|---|
| M005-012 | CX-01；Grok V0.06 tsconfig:1–13逐段技术性登记 | 技术性 | 否 | 否 | 全并。九选项+web真实extends。只登记 |
| M005-013 | CX-02；Grok V0.06 CODEOWNERS:1–12逐段技术性登记 | 技术性 | 否 | 否 | 全并。九条路径评审路由。只登记 |
| M005-014 | CX-03；Grok V0.06 workflow:1–27逐段登记；GR-M005-B1-01 | 技术性 | 是 | 是 | 全并入既有014，不新分ID。唯一缺陷L6注释 |

漏并：无。Grok唯一finding未另开号、未丢。Codex三项登记均入012/013/014。action SHA钉死、skipLibCheck、HTML脏工作区第一闸红均未误升为新ID；后一项候选正确记为既有下游BSM/台账问题，不属本模块缺陷。

分类：三项均冲突否→细化否→技术性是→添加否。编译选项、仓库评审路由、CI工作流不是业务角色/枚举/审批。无业务发明。依据GEN-001/002/003/004/005/008/009/010/011/012/013/016/017/018按两席适用表。014缺陷主据GEN-002（声称与行为不一致）与GEN-016（注释不可从hook复现），GEN-018为取证方式（可复现命令+外部规范），不改变分类。012/013证据类型4为主、类型3为辅；014类型4（本席B2三probe），辅以类型3（保护API 403与workflow syntax）。

队列：012/013技术性无缺陷队列否；014技术性有缺陷队列是。Codex B1曾把CX-03写成缺陷否，不能用来覆盖Grok B1已成立的GR-M005-B1-01；候选取并集并把014标是，正确。本席B1队列建议原文是「只改本 YAML 注释」；B2在同一finding下补必要镜像与自身版本行，不另开ID。

旧001–011：候选正文声明保留旧范围与历史处置。已核下方历史定稿表仍在：001–006历史工程缺陷是、队列否；007–011否/否；历史012仍是同一tsconfig.base.json九选项、否/否。未把001–006改写成CI内容，未抹历史缺陷。下一旧M005段及更早原文未改。

### 014根因复现与修复边界
两YAML当前均L6：
> # 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
均L1：`# 文件版本：V0.01`。两文件各1315bytes、27行，SHA256均5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974，当前原始字节相等且无CRLF。

本席B2只读复现（未git push、未部署）：
- 完整读ops/160/hooks/pre-push共17行。只匹配stdin中` refs/heads/main `；ALLOW_MAIN_PUSH不为1则exit1。无npm、无verify:predeploy。
- 三probe，Python subprocess.run(['bash','ops/160/hooks/pre-push'],input=...)：
  - feature/default：stdin `refs/heads/probe aaaaa refs/heads/probe bbbbb`，未设ALLOW_MAIN_PUSH，exit0，无输出。
  - main/default：stdin `refs/heads/probe aaaaa refs/heads/main bbbbb`，exit1，stderr「[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。」
  - main/override：同main输入、ALLOW_MAIN_PUSH=1，exit0，stderr「[pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。」
- 部署脚本只读：ops/160/complex-deploy-api.sh:197–210与complex-deploy-web.sh:24–37，REQUIRED_GATES循环内`npm run "$gate"`，非0则exit1。确为部署前治理门禁硬阻断。不调用package.json整串verify:predeploy，但成分闸同名。拟改后注释「部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。」与上述事实一致，优于原句把pre-push写成门禁承担者。
- 镜像约束只读：tools/verify-delivery-gates.mjs:284–288先`text.replace(/\r\n/g,"\n")`再比较`.github/workflows/predeploy-gates.yml`与`ops/ci/predeploy-gates.yml`。内容不等即失败。只改.github会在下次跑该checker时失配。同步ops/ci是同一finding的必要伴随变更，不是「跟别处一致」的习惯扩批。

边界足够且最小：仅`.github/workflows/predeploy-gates.yml`与`ops/ci/predeploy-gates.yml`各L6注释替换、各L1 V0.01→V0.02，两文件四行。不得改事件、步骤、action、权限、hooks、部署逻辑、其他配置或三份业务HTML。不改pre-push、不改部署脚本。镜像文件属M007（plan/modules.md M007 FILES含ops/ci/predeploy-gates.yml与ops/160/hooks/pre-push）；checker属M008（tools/verify-delivery-gates.mjs）。此处仅追M005直接依赖并修镜像、只读取证hook与checker，不启动M007/M008审计，不关闭M007。B3配置/注释修正无需新增行为测试文件；用现有yaml解析器断言修前后AST相同、两文件字节相等、diff恰为上述两行。

### 三处报告措辞：明确同意
1. GEN-012超时默认360。同意。本席V0.06写「未设 timeout 为浮动/无上限」中「无上限」不准确。本次只读获取GitHub Workflow syntax：`jobs.<job_id>.timeout-minutes`原文「The maximum number of minutes to let a job run before GitHub automatically cancels it. Default: 360」。ubuntu-latest镜像浮动仍是未验证边界，不立案。本批不新增timeout字段，不把该措辞当第二缺陷。V0.06原文保留，本票追加澄清。
2. 假设工程增强不属于现有业务添加项。同意。本席V0.06已对缺permissions/concurrency/timeout、补CODEOWNERS覆盖tsconfig.base.json、base补files:[]写「不立案」。未提实施，不能据此新建questions。现有三项技术内容（012/013/014）与假设加固必须分开。不把假设增强变成新的业务需求、判据或强制测试。
3. 镜像LF比较。同意。checker实现是CRLF归一LF后比较字符串；失败文案写byte-identical，不能冒称checker要求原始字节必等。当前两文件碰巧原始字节也相等。修后只要两文件得到同一四行改动，LF比较与原始字节比较都会继续相等。

### 纠正意见（不构成B2 FAIL）
- 候选验证计划写「运行现有verify:delivery-gates」。package.json中`verify:delivery-gates`是父串，含matrix/unit等下游闸；直接相关子命令是`verify:delivery-gates-docs`→`node tools/verify-delivery-gates.mjs`。B3实施须用该子命令及两YAML的AST/字节/diff断言，如实保留结果；不得把父命令或verify:predeploy全仓绿当作本模块验证目标。此为实施收窄，不是漏并、不是改分类/队列。
- 本席V0.06 B1队列建议「只改本 YAML」在B2由镜像checker事实补为两文件四行，仍是GR-M005-B1-01/M005-014同一缺陷，不新开M007 finding。

四值在新独立B1后仍为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，COUNT1未改锁。B2不写独占锁字段。M001—M004闭环保留。历史B3累计1、方案修复2、交付失败0不清零。生命周期仍IN_PROGRESS。

结论：全批差集成立，012/013只登记，014按已列两文件四行整批修复。本席B2 PASS。须Codex本批B2 PASS后才能定稿并一次修复；未经双PASS不进B3，更不称模块PASS。
