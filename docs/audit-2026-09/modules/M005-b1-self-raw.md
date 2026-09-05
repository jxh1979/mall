CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M005 B1 修复前全模块审计（本方）
版本：V0.01

本席结论：FAIL。全4文件223行/128根scripts/18GEN三线已经完整审完，发现CX-01、CX-02两项工程缺陷；不在B1修复、发问或提前转B2。B3=0，运行失败重跑0。本报告只构成本席独立票，不是模块结论。

## 输入和独立性

控制V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21，HEAD 134d75d158ca6ff1343f616cce47676ecfc83e02，M005—01-root-config-and-guidance-03，NORMAL，ledger IN_PROGRESS。M004已发布PR413并ff同步后才进入M005。先派Grok B1（14:00:29 UTC/session5968），随后独立做本报告中的完整检查；截至本文件写入未读M005 Grok raw正文/原始JSON/对方分析，未创建当前全tree/diff混入raw。源4文件全部字节保持，三HTML未动。

首次上下文读取已全显四文件223行；B1检查逐项引用当前源并在写入时复算全部SHA，未把此前M004意见当独立审计。必要直接引用：apps/web package/tsconfig/Vite、Playwright globalSetup和真实后端测试入口、run-real-backend-ui-e2e.mjs、当前安装npm/Playwright实现。引用只为验证本模块调用，不开始未来模块的业务审计。

## 全文件覆盖
| 文件 | 行数 | 字节 | SHA256 |
|---|---:|---:|---|
| package.json | 147 | 15581 | f59711d7579178d2772ec7456592d616d41ac3d5980e58864aeaf66c2e60b7db |
| playwright.config.ts | 25 | 732 | f389c55d86716b5bd921ea60320c74936a72e72b4e54a8df6fae9eca6db86e19 |
| playwright.real-backend.config.ts | 38 | 1268 | cf7b020c030dbd8ad4aeba54fe331528cd0743c59c22cbd03bdb884fe187afa2 |
| tsconfig.base.json | 13 | 303 | 13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db |

### package.json 全部字段及128脚本

L1—9/L138—147：JSON重复键检查通过；name内部包名、private禁止误发包、version0.1.0应用semver、type module、workspaces apps/web、packageManager npm@10.9.8、engines.node、devDependencies @playwright/test及yaml逐字段审查。Playwright来自根CLI实际使用；yaml被tools/apply-openapi-p0-examples.mjs:3及tools/verify-openapi-contract.mjs:3实际import。两个依赖都有当前锁记录；本模块未变版本/依赖。engines范围的错误归CX-01。未将这些内部标识和测试名当面向用户展示文案。

每个scripts值已完整阅读；下表保留原完整命令并登记实际目标存在性、npm转发及用途。脚本是命令入口，指向已有工具，不等于添加同名业务；不因纯命令别名就在无调用证据时删除兼容入口。带docs/prepare的生成动作只核入口，不在B1运行改业务文档的命令；数据库/设备/部署全栈脚本不在非隔离环境执行。已执行所有75去重node入口的node --check，各exit0；npm根128和工作区6个脚本的workspace-aware解析得到108条npm run边，缺失0、环0。

第一次只按根名搜索的简化图把--workspace目标错当根自身，产生虚假循环；随后使用逐&&分段、shlex及--workspace作用域重算，9个假循环全部消失。旧辅助输出保留事实但不作为finding，未修改源去迎合错误检查器。
| 行 | script | 完整命令 | 四分类/验证 |
|---:|---|---|---|
| 10 | dev | npm run dev:backend | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 11 | dev:backend | node tools/run-backend-maven.mjs spring-boot:run | 技术性；现有node入口/语法通过 |
| 12 | test:backend | node tools/run-backend-maven.mjs test | 技术性；现有node入口/语法通过 |
| 13 | verify:backend-env | node tools/run-backend-maven.mjs --check | 技术性；现有node入口/语法通过 |
| 14 | dev:api | npm run dev:backend | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 15 | dev:web | npm run dev --workspace apps/web | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 16 | verify:openapi | node tools/verify-openapi-contract.mjs | 技术性；现有node入口/语法通过 |
| 17 | audit:client-routes | node tools/audit-client-api-routes.mjs | 技术性；现有node入口/语法通过 |
| 18 | verify:client-routes | node tools/audit-client-api-routes.mjs --strict | 技术性；现有node入口/语法通过 |
| 19 | audit:client-openapi | node tools/audit-client-openapi-coverage.mjs | 技术性；现有node入口/语法通过 |
| 20 | verify:client-openapi | node tools/audit-client-openapi-coverage.mjs --strict | 技术性；现有node入口/语法通过 |
| 21 | audit:client-payload | node tools/verify-client-payload-policy.mjs | 技术性；现有node入口/语法通过 |
| 22 | verify:client-payload | node tools/verify-client-payload-policy.mjs --strict | 技术性；现有node入口/语法通过 |
| 23 | docs:openapi-examples | node tools/apply-openapi-p0-examples.mjs | 技术性；现有node入口/语法通过 |
| 24 | verify:openapi-examples | node tools/verify-openapi-p0-examples.mjs | 技术性；现有node入口/语法通过 |
| 25 | docs:operation-matrix | node tools/generate-openapi-operation-matrix.mjs | 技术性；现有node入口/语法通过 |
| 26 | verify:operation-matrix | node tools/generate-openapi-operation-matrix.mjs --check | 技术性；现有node入口/语法通过 |
| 27 | docs:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs | 技术性；现有node入口/语法通过 |
| 28 | verify:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs --check | 技术性；现有node入口/语法通过 |
| 29 | docs:security-baseline | node tools/generate-formal-security-baseline.mjs | 技术性；现有node入口/语法通过 |
| 30 | docs:entry-baseline | node tools/refresh-entry-doc-baseline.mjs | 技术性；现有node入口/语法通过 |
| 31 | verify:html-ledger-sync | node tools/verify-html-ledger-sync.mjs | 技术性；现有node入口/语法通过 |
| 32 | verify:entry-anchors | node tools/verify-entry-anchors.mjs | 技术性；现有node入口/语法通过 |
| 33 | verify:html-param-tier | node tools/verify-html-param-tier.mjs | 技术性；现有node入口/语法通过 |
| 34 | verify:entry-doc-baseline | node tools/refresh-entry-doc-baseline.mjs --check | 技术性；现有node入口/语法通过 |
| 35 | verify:security-baseline | npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict | 技术性；现有node入口/语法通过 |
| 36 | verify:prod-docs-access | node tools/verify-prod-docs-access-policy.mjs | 技术性；现有node入口/语法通过 |
| 37 | verify:ops160-execution-policy | node tools/verify-ops160-execution-policy.mjs | 技术性；现有node入口/语法通过 |
| 38 | verify:ops160-backup-restore | node tools/verify-160-backup-restore-scripts.mjs | 技术性；现有node入口/语法通过 |
| 39 | verify:production-credentials | node tools/verify-production-credential-rotation-ledger.mjs | 技术性；现有node入口/语法通过 |
| 40 | verify:p0a-ops-hardening | node tools/verify-p0a-ops-hardening.mjs | 技术性；现有node入口/语法通过 |
| 41 | verify:ops160-recovery | npm run verify:coding-host-protection && npm run verify:ops160-execution-policy && npm run verify:ops160-backup-restore && npm run verify:production-credentials && npm run verify:p0a-ops-hardening && npm run verify:nas-smb-mount-evidence | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 42 | verify:p0-contract | npm run verify:no-legacy-api-prototype && npm run verify:openapi && npm run verify:openapi-examples && npm run verify:operation-matrix && npm run verify:openapi-closure && npm run verify:client-routes && npm run verify:client-openapi && npm run verify:client-payload | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 43 | verify:p0-web | npm run verify:p0-contract && npm run build | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 44 | verify:p0-env | npm run verify:backend-env && npm run verify:android-env | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 45 | build | npm run build --workspace apps/web | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 46 | typecheck | npm run typecheck --workspace apps/web | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 47 | smoke:p0 | npm run test:backend | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 48 | smoke:p0-ops | npm run test:backend | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 49 | verify:android-env | node tools/run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app | 技术性；现有node入口/语法通过 |
| 50 | android:p0:build | node tools/run-android-gradle.mjs --project apps/android-terminal clean build | 技术性；现有node入口/语法通过 |
| 51 | android:p0:smoke | node tools/run-android-p0-smoke.mjs | 技术性；现有node入口/语法通过 |
| 52 | android:p0:apk | node tools/run-android-gradle.mjs --project apps/android-terminal/android-app --require-android-sdk :app:assembleDebug | 技术性；现有node入口/语法通过 |
| 53 | verify:p0 | npm run verify:coding-host-protection && npm run verify:p0-contract && npm run verify:security-baseline && npm run test:backend && npm run build && npm run verify:android-env && npm run android:p0:build && npm run verify:android-p0-smoke-wrapper && npm run android:p0:smoke && npm run android:p0:apk | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 54 | verify:no-legacy-api-prototype | node tools/verify-no-legacy-api-prototype.mjs | 技术性；现有node入口/语法通过 |
| 55 | verify:coding-host-protection | node tools/verify-coding-host-protection.mjs | 技术性；现有node入口/语法通过 |
| 56 | verify:t3-coding-package | node tools/verify-t3-coding-package.mjs | 技术性；现有node入口/语法通过 |
| 57 | verify:t3b-supplier-mock-kit | node tools/verify-t3b-supplier-mock-kit.mjs | 技术性；现有node入口/语法通过 |
| 58 | verify:t3d-local-llm-ai-quality | node tools/verify-t3d-local-llm-ai-quality.mjs | 技术性；现有node入口/语法通过 |
| 59 | verify:external-dependency-gates | node tools/verify-external-dependency-gates.mjs | 技术性；现有node入口/语法通过 |
| 60 | verify:delivery-gates-docs | node tools/verify-delivery-gates.mjs | 技术性；现有node入口/语法通过 |
| 61 | verify:delivery-gates | npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:migration-version-order && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:unit && npm run verify:coding-host-protection && npm run verify:external-dependency-gates && npm run verify:integration-capability-switches && npm run verify:external-evidence-requirement-traceability && npm run verify:partial-requirement-mock-coverage && npm run verify:t3-coding-package && npm run verify:t3b-supplier-mock-kit && npm run verify:t3d-local-llm-ai-quality && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:final-closed-loop-audit && npm run verify:closed-loop-current-state && npm run verify:runtime-acceptance-evidence && npm run verify:ui-experience-acceptance && npm run verify:production-readiness-evidence && npm run verify:nas-current-state-drift && npm run verify:nas-diagnostics && npm run verify:nas-it-handoff && npm run verify:production-blocker-register-no-go-fixtures && npm run verify:production-blockers && npm run verify:production-closure-gap-reports && npm run verify:production-evidence-backfill-execution-ledger && npm run verify:production-evidence-backfill-priority && npm run verify:production-evidence-crosslinks && npm run verify:production-evidence-draft-prep && npm run verify:production-evidence-draft-prep-fixtures && npm run verify:production-evidence-intake && npm run verify:production-evidence-manifest-templates && npm run verify:production-evidence-negative-fixtures && npm run verify:production-evidence-owner-execution-packets && npm run verify:production-evidence-owner-guides && npm run verify:production-evidence-p0-quickstart && npm run verify:production-evidence-positive-fixtures && npm run verify:production-evidence-positive-propagation-fixtures && npm run verify:production-evidence-submission-kit && npm run verify:production-evidence-submissions-guard && npm run verify:production-evidence-submission-status && npm run verify:production-no-go-command-center && npm run verify:production-owner-actions && npm run verify:production-ready-no-go-fixtures && npm run verify:production-ready-positive-fixtures | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 62 | verify:nas-smb-mount-evidence | node tools/verify-nas-smb-mount-evidence.mjs | 技术性；现有node入口/语法通过 |
| 63 | verify:production-readiness-evidence | node tools/verify-production-readiness-evidence.mjs | 技术性；现有node入口/语法通过 |
| 64 | docs:production-blockers | node tools/generate-production-blocker-register.mjs | 技术性；现有node入口/语法通过 |
| 65 | verify:production-blockers | node tools/generate-production-blocker-register.mjs --check | 技术性；现有node入口/语法通过 |
| 66 | verify:production-evidence-intake | node tools/verify-production-evidence-intake-templates.mjs | 技术性；现有node入口/语法通过 |
| 67 | docs:production-owner-actions | node tools/generate-production-owner-action-packs.mjs | 技术性；现有node入口/语法通过 |
| 68 | verify:production-owner-actions | node tools/generate-production-owner-action-packs.mjs --check | 技术性；现有node入口/语法通过 |
| 69 | docs:production-evidence-submission-kit | node tools/generate-production-evidence-submission-kit.mjs | 技术性；现有node入口/语法通过 |
| 70 | verify:production-evidence-submission-kit | node tools/generate-production-evidence-submission-kit.mjs --check | 技术性；现有node入口/语法通过 |
| 71 | docs:production-evidence-submission-status | node tools/generate-production-evidence-submission-status.mjs | 技术性；现有node入口/语法通过 |
| 72 | verify:production-evidence-submission-status | node tools/generate-production-evidence-submission-status.mjs --check | 技术性；现有node入口/语法通过 |
| 73 | docs:production-evidence-manifest-templates | node tools/generate-production-evidence-manifest-templates.mjs | 技术性；现有node入口/语法通过 |
| 74 | verify:production-evidence-manifest-templates | node tools/generate-production-evidence-manifest-templates.mjs --check | 技术性；现有node入口/语法通过 |
| 75 | docs:production-evidence-owner-guides | node tools/generate-production-evidence-owner-guides.mjs | 技术性；现有node入口/语法通过 |
| 76 | verify:production-evidence-owner-guides | node tools/generate-production-evidence-owner-guides.mjs --check | 技术性；现有node入口/语法通过 |
| 77 | verify:production-evidence-submissions-guard | node tools/verify-production-evidence-submissions-guard.mjs | 技术性；现有node入口/语法通过 |
| 78 | docs:production-evidence-backfill-priority | node tools/generate-production-evidence-backfill-priority.mjs | 技术性；现有node入口/语法通过 |
| 79 | verify:production-evidence-backfill-priority | node tools/generate-production-evidence-backfill-priority.mjs --check | 技术性；现有node入口/语法通过 |
| 80 | docs:production-evidence-backfill-execution-ledger | node tools/generate-production-evidence-backfill-execution-ledger.mjs | 技术性；现有node入口/语法通过 |
| 81 | verify:production-evidence-backfill-execution-ledger | node tools/generate-production-evidence-backfill-execution-ledger.mjs --check | 技术性；现有node入口/语法通过 |
| 82 | test:ui | playwright test | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 83 | verify:ui | playwright test | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 84 | test:ui:update | playwright test --update-snapshots | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 85 | verify:ui:real-backend | REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts | 技术性；已有npm工作区/脚本/Playwright入口解析通过；POSIX环境赋值在本160 Linux有效，未声称Windows执行已验证 |
| 86 | verify:ui:real-backend:seeded | node tools/run-real-backend-ui-e2e.mjs | 技术性；现有node入口/语法通过 |
| 87 | verify:android-p0-smoke-wrapper | node tools/verify-android-p0-smoke-wrapper.mjs | 技术性；现有node入口/语法通过 |
| 88 | verify:full-admin-matrix | node tools/verify-full-admin-page-matrix.mjs | 技术性；现有node入口/语法通过 |
| 89 | test:unit | npm run test:unit --workspace apps/web | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 90 | verify:unit | npm run test:unit --workspace apps/web | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 91 | docs:domain-ownership-scope | node tools/generate-domain-ownership-scope-audit.mjs | 技术性；现有node入口/语法通过 |
| 92 | verify:domain-ownership-scope | node tools/generate-domain-ownership-scope-audit.mjs --check | 技术性；现有node入口/语法通过 |
| 93 | verify:matrix-vs-migration | node tools/verify-matrix-vs-migration.mjs | 技术性；现有node入口/语法通过 |
| 94 | verify:migration-version-order | node tools/verify-migration-version-order.mjs | 技术性；现有node入口/语法通过 |
| 95 | verify:tenant-isolation | node tools/verify-tenant-isolation.mjs | 技术性；现有node入口/语法通过 |
| 96 | verify:predeploy | npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 97 | verify:pii-encryption | node tools/verify-pii-encryption.mjs | 技术性；现有node入口/语法通过 |
| 98 | verify:test-db-isolation | node tools/verify-test-db-isolation.mjs | 技术性；现有node入口/语法通过 |
| 99 | docs:production-closure-gap-reports | node tools/generate-production-closure-gap-report.mjs | 技术性；现有node入口/语法通过 |
| 100 | verify:production-closure-gap-reports | node tools/generate-production-closure-gap-report.mjs --check && node tools/verify-production-closure-gap-reports.mjs | 技术性；现有node入口/语法通过 |
| 101 | verify:final-closed-loop-audit | node tools/verify-final-closed-loop-audit.mjs | 技术性；现有node入口/语法通过 |
| 102 | verify:production-evidence-negative-fixtures | node tools/verify-production-evidence-negative-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 103 | docs:nas-current-state-drift | node tools/generate-nas-current-state-drift-report.mjs | 技术性；现有node入口/语法通过 |
| 104 | verify:nas-current-state-drift | node tools/generate-nas-current-state-drift-report.mjs --check | 技术性；现有node入口/语法通过 |
| 105 | docs:closed-loop-current-state | node tools/generate-closed-loop-current-state.mjs | 技术性；现有node入口/语法通过 |
| 106 | verify:closed-loop-current-state | node tools/generate-closed-loop-current-state.mjs --verify | 技术性；现有node入口/语法通过 |
| 107 | verify:closed-loop-runtime | npm run test:backend && npm run typecheck --workspace apps/web && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:real-backend:seeded | 技术性；已有npm工作区/脚本/Playwright入口解析通过 |
| 108 | verify:nas-it-handoff | node tools/verify-nas-it-handoff.mjs | 技术性；现有node入口/语法通过 |
| 109 | docs:external-evidence-requirement-traceability | node tools/generate-external-evidence-requirement-traceability.mjs | 技术性；现有node入口/语法通过 |
| 110 | verify:external-evidence-requirement-traceability | node tools/generate-external-evidence-requirement-traceability.mjs --check | 技术性；现有node入口/语法通过 |
| 111 | docs:nas-diagnostics | node tools/generate-nas-diagnostic-report.mjs | 技术性；现有node入口/语法通过 |
| 112 | verify:nas-diagnostics | node tools/generate-nas-diagnostic-report.mjs --check | 技术性；现有node入口/语法通过 |
| 113 | docs:production-evidence-p0-quickstart | node tools/generate-production-evidence-p0-quickstart.mjs | 技术性；现有node入口/语法通过 |
| 114 | verify:production-evidence-p0-quickstart | node tools/generate-production-evidence-p0-quickstart.mjs --check | 技术性；现有node入口/语法通过 |
| 115 | docs:runtime-acceptance-evidence | node tools/generate-runtime-acceptance-evidence.mjs | 技术性；现有node入口/语法通过 |
| 116 | verify:runtime-acceptance-evidence | node tools/generate-runtime-acceptance-evidence.mjs --check | 技术性；现有node入口/语法通过 |
| 117 | docs:ui-experience-acceptance | node tools/generate-ui-experience-acceptance-report.mjs | 技术性；现有node入口/语法通过 |
| 118 | verify:ui-experience-acceptance | node tools/generate-ui-experience-acceptance-report.mjs --check | 技术性；现有node入口/语法通过 |
| 119 | docs:production-evidence-owner-execution-packets | node tools/generate-production-evidence-owner-execution-packets.mjs | 技术性；现有node入口/语法通过 |
| 120 | verify:production-evidence-owner-execution-packets | node tools/generate-production-evidence-owner-execution-packets.mjs --check | 技术性；现有node入口/语法通过 |
| 121 | prepare:production-evidence-drafts | node tools/prepare-production-evidence-manifest-drafts.mjs | 技术性；现有node入口/语法通过 |
| 122 | verify:production-evidence-draft-prep | node tools/prepare-production-evidence-manifest-drafts.mjs --check | 技术性；现有node入口/语法通过 |
| 123 | verify:production-evidence-draft-prep-fixtures | node tools/verify-production-evidence-draft-prep-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 124 | verify:production-evidence-positive-fixtures | node tools/verify-production-evidence-positive-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 125 | verify:production-evidence-positive-propagation-fixtures | node tools/verify-production-evidence-positive-propagation-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 126 | verify:production-ready-no-go-fixtures | node tools/verify-production-ready-no-go-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 127 | verify:production-blocker-register-no-go-fixtures | node tools/verify-production-blocker-register-no-go-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 128 | verify:production-ready-positive-fixtures | node tools/verify-production-ready-positive-fixtures.mjs | 技术性；现有node入口/语法通过 |
| 129 | verify:production-evidence-crosslinks | node tools/verify-production-evidence-crosslinks.mjs | 技术性；现有node入口/语法通过 |
| 130 | docs:partial-requirement-mock-coverage | node tools/generate-partial-requirement-mock-coverage.mjs | 技术性；现有node入口/语法通过 |
| 131 | verify:partial-requirement-mock-coverage | node tools/generate-partial-requirement-mock-coverage.mjs --check | 技术性；现有node入口/语法通过 |
| 132 | docs:production-no-go-command-center | node tools/generate-production-no-go-resolution-command-center.mjs | 技术性；现有node入口/语法通过 |
| 133 | verify:production-no-go-command-center | node tools/generate-production-no-go-resolution-command-center.mjs --check | 技术性；现有node入口/语法通过 |
| 134 | docs:integration-capability-switches | node tools/generate-integration-capability-switches.mjs | 技术性；现有node入口/语法通过 |
| 135 | verify:integration-capability-switches | node tools/generate-integration-capability-switches.mjs --check | 技术性；现有node入口/语法通过 |
| 136 | docs:data-isolation-spec | node tools/generate-data-isolation-spec.mjs | 技术性；现有node入口/语法通过 |
| 137 | verify:data-isolation-spec | node tools/generate-data-isolation-spec.mjs --check | 技术性；现有node入口/语法通过 |

### playwright.config.ts 全25行

L1：defineConfig/devices实际导入成功。L3—6：tests/e2e目录、排除web-ui-real-backend.spec.ts、global-setup.ts路径实存；list实得47测试/9文件。其中live-evidence类有REAL_BACKEND_E2E标志前置skip，默认未设置不运行真实库；默认列表包含它们本身不证明已跑真实行为，不把它们误报为默认数据库写入。L7—10：30s测试、8s期待、非fullyParallel、list+html且open never。L11—15：loopback baseURL、失败trace/screenshot。L16—21：npm dev:web可解析到apps/web vite，目标端口5173一致，CI不复用、普通开发可复用，120s启动边界。L22—25：单Chromium桌面项目实际被list读取。各行技术性，无业务发明；设备预设不是业务端口或新增客人Web授权。

必要引用global-setup.ts已完整读36行：浏览器关闭在finally，暖机异常有warning且测试仍须正常执行断言；launch在try外失败会显式失败。其mock API登录只为编译预热，不是后端授权实现。本模块不要求把开发服务器默认复用机制改成部署验收流程。

### playwright.real-backend.config.ts 全38行

L1：原生Node加载TS配置成功。L3—5：backend URL与webPort覆盖，默认8080/5174；port未经校验见CX-02。L7—12：同testDir只选web-ui-real-backend.spec.ts，45s/10s、非fullyParallel。L13—19：list/html/json记者与注释所指消费工具路径一致，json目标test-results/real-backend-results.json；wrapper确实读取stats并要求expected>=1且skip/fail/flaky0。L20—24：baseURL与webServer同值、失败trace/screenshot。L25—34：npm workspace命令、loopback host、strictPort、禁止复用、120s、env继承和VITE_API_PROXY_TARGET透传；无持久环境变更。L35—38：Chromium设备及闭合。除CX-02外字段用途均可验证技术性，未从报告目录和测试选择器发明业务流程。

真实后端spec L39—40在REAL_BACKEND_E2E不为1时跳过；package脚本L85设置1，wrapper又显式传1，保持一致。原生list为3测试/1文件，不是测试执行通过。wrapper先删旧json后执行、检查统计，配置输出位置与其resultsPath一致；没有复用旧json假绿的当前配置缺口。

### tsconfig.base.json 全13行

L1—2/L12—13合法JSON对象闭合；L3 target ES2022、L4 ESNext、L5 Bundler用于Vite消费者；L6 strict、L7 esModuleInterop、L8路径大小写、L9 skipLibCheck、L10resolveJsonModule、L11noUncheckedIndexedAccess均实际由apps/web extends解析。tsc --showConfig -p apps/web/tsconfig.json exit0确认全部生效，目标src/**/*.ts和tsx来自消费者。本文件没有include是可复用base正常形式，不把消费者源码错误算成配置发明。skipLibCheck不关闭应用类型检查。

实际TypeScript createProgram用该文件解析出的选项，noEmit、types=[]、虚拟单文件，未写入工作区：合法number代码诊断0；数组索引赋给string及隐式any参数得到2322与7006，能失败的反例生效。四分类技术性，未定义业务枚举或权限。

## 冻结18GEN逐项结果

| GEN | 结果 | 本次实据/边界 |
|---|---|---|
| 001 | PASS | 四配置各有安装/脚本/测试/编译消费；128入口指向现有工具；没有为单次测试造新框架，原有别名用途可识别 |
| 002 | FAIL | JSON/TS原生解析、75 node入口语法、实际verify:openapi通过；CX-01声明相容性错误、CX-02非法端口进入shell |
| 003 | PASS | TypeScript正负编译断言真实运行；两套Playwright list准确区分47/9与3/1；未把列出测试当测试绿 |
| 004 | FAIL | CX-02环境端口进入可信命令前没有数字/范围校验 |
| 005 | PASS | 本模块无业务主体授权实现或数据库访问；已有mock/真实后端工具入口不替代服务器授权 |
| 006 | PASS | 根工作区目标/脚本引用/依赖均存在，配置baseURL与serverURL同值、结果json与读取端一致；不声称npm安装原子回滚 |
| 007 | PASS | 测试未开fullyParallel；真实服务不复用且strictPort；此文件无业务幂等写路径，端口冲突已有显式失败 |
| 008 | PASS | workspace、tsconfig继承、Playwright reporter与wrapper接口匹配；没有新增前后端业务schema |
| 009 | FAIL | CX-02错误端口未在首次使用前拒绝且能产生命令副作用；现有工具&&链接正确传播失败 |
| 010 | FAIL | CX-02可让控制端口环境的人在本地shell执行额外命令；无远端网络攻击者前提，未夸称线上RCE |
| 011 | PASS | reporter保存失败证据、真实json可被wrapper计数校验；本次记录真实exit，未把配置验证当全E2E通过 |
| 012 | PASS | 测试/expect/server均有有限timeout，单Chromium；静态脚本图无环；本模块未引入无界任务 |
| 013 | FAIL | CX-01根>=22宽于已锁jsdom等支持范围；CX-02端口配置未收窄到可执行参数 |
| 014 | PASS | 本模块无SQL迁移正文；migration-version-order接入既有delivery-gates且原生入口存在，不臆加到predeploy |
| 015 | PASS | 本模块无产品UI模板，内部script/project标识不需面向用户中文翻译；UI用例选择器已实际加载 |
| 016 | PASS | 两配置注释和路径/目标输出与真实消费者一致；没有把计划或旧报告写当前全业务完成 |
| 017 | PASS | 四文件全部内容按顺序判为技术性，无业务添加/HTML冲突；CX两项为工程缺陷，后续整批B2定稿 |
| 018 | PASS | 原生JSON/TS/npm/Playwright执行、可失败反例、冻结哈希及完整源覆盖；意见/注释本身未当证据 |

## 三线收口与全部临时findings

第一性原理：删除root package无法按现有npm入口启动/验证/安装；删除Playwright配置失去测试选择和真实后端对应证据出口；删除tsconfig base失去已运行证明的严格类型约束。三HTML没有npm/编译器逐字段要求，技术用途由实际消费验证，不套用“无HTML即添加”。全部脚本/配置的间接业务工具名字不在本模块创造新业务能力。

顺从式：对完整223行和18GEN逐项核对，入口存在、语法、workspace路由、Reporter消费、编译选项是可用的；根engine与锁有明确不一致，port却被宽容解析入shell，不能给全模块PASS。

对抗式：主动拒绝“root写>=22便支持所有22+”“端口环境只是字符串所以安全”“任何特殊字符都会被URL提前挡住”“--list exit0就是实际测试绿”“所有同名npm run都是递归”。前两项由以下反例证伪；普通分号直接嵌在port会被URL挡住，进一步带路径/的反例则通过真实URL解析及WebServerPlugin并执行无害标记，因此不只凭字符串推测漏洞。

### CX-01 — 根Node支持范围宽于既定构建/测试依赖

- 位置：package.json:140—141；必要引用package-lock.json已有jsdom/Vite/Vitest及其他engines，不重新审M004。
- 四分类：技术性；工程缺陷：是；严重度P2；冻结GEN-002/GEN-013；依据类型4，可复现命令输出（附类型2原生npm元数据）。
- 根声明>=22接受22.0.0/22.12.0/23.0.0；锁的jsdom 29.1.1要求^20.19.0 || ^22.13.0 || >=24.0.0，另Vite/部分依赖要求>=22.12。用当前npm实际semver逐217安装记录验证：22.0有26条不满足、22.12有1条不满足、23.0有9条不满足；22.13/24.0/24.20均0。当前Node24.20可运行不证明全部声明范围支持。
- 实际npm-install-checks.checkEngine对根和jsdom分别传相同nodeVersion：22.0根true/jsdom EBADENGINE；22.13均true；23根true/jsdom EBADENGINE；24均true。没有下载或运行其他版本Node二进制，不宣称在这些二进制上实测完整构建。
- 最小方向：在B2核准后收窄根engines到已声明依赖共同支持范围（候选^22.13.0 || >=24.0.0）；必要的根lock镜像同步须在B2明确边界，当前未改任何文件。不要把同一问题变成升级整个依赖树。
- 不是业务添加，不新增运行环境能力/业务参数。根应用semver与文件版本元数据按用户规则区分，实际修复时再整批定稿。

### CX-02 — 真实后端Web端口环境值未经校验拼入shell命令

- 位置：playwright.real-backend.config.ts:4—5、26；冻结GEN-004/GEN-009/GEN-010/GEN-013；四分类技术性；工程缺陷是；严重度P2；依据类型4实际反例执行。
- 原配置import接受0、70000、abc及含shell语法的值，原样放入url和command。不凭这一点单独称命令可执行：实际当前Playwright runner/index.js:799—805先getIsAvailableFunction(new URL)，:832—841以shell:true执行command，:952—955只解析URL而未将原始port限制为端口。
- 首次简化反例“5174; printf...”虽在noop npm shell中出标记，但URL可能先拒，不能当完整可达证据。修正输入为随机本机空闲端口后接“/; printf M005_ACTUAL_PLUGIN_MARKER; #”，URL合法，path承接分号，原命令仍是shell文本。
- 实际探针在内存加载已安装Playwright runner仅导出其原WebServerPlugin类，未改实现；调用原配置计算command/url；仅在命令前加noop npm函数，避免启动业务Vite，后续原命令完全保留。原Plugin实际setup→URL可用性检查→launchProcess(shell:true)执行标记，捕获stdout。干净重跑exit0，parsedPort39311、pathAccepted true、actualPluginShellMarker true、预期Process from config.webServer exited early；onlyStubNpm true。没有访问数据库/生产服务/敏感文件。
- 第一次私有API探针已输出marker true但Node加载器线程留存，退出未结束；精确核对本方node -e完整参数后仅终止该测试PID2735680，返回-15，不把它计成功。辅助器最后明确process.exit，原样行为重验得到上述exit0。这不是Grok交付失败，重跑计数不加。
- 攻击前提仅为可控制REAL_BACKEND_WEB_PORT的本地/CI配置输入，没有证明远端用户能设置它；这里报告输入校验和命令构造缺陷，不夸称线上未认证RCE。最小修复方向是使用前限定十进制整数1—65535；不增加依赖/工厂/通用命令框架。当前仅审计。

## 技术登记

T01 package协议字段和根workspace关系；T02 全128scripts及命令图（75node目标/108npm边）；T03 两项devDependencies真实用途及packageManager；T04 mock Playwright配置及消费；T05 real-backend除CX-02外配置/报告关联；T06 tsconfig全部编译字段。这六组覆盖所有无缺陷技术性内容，后续B2应和CX全部一起归并，登记不等于待修。

## 真实检查、局限与源码终检

实际成功：Node v24.20.0/npm11.19.0；75node目标node --check；workspace-aware脚本图134节点/108边、缺失0/循环0；两Playwright --list分别47测试9文件/3测试1文件；tsc --showConfig；实际TypeScript正/负虚拟编译诊断0 vs2322/7006；npm run verify:openapi；npm semver/checkEngine范围反例；原配置加载正常和非法端口；原WebServerPlugin实际安全marker探针。临时模块只在内存，不留永久辅助代码。

未执行：全128脚本的业务动作、全量E2E、真实后端/数据库/设备启动、部署、源码typecheck全项目、Node22/23二进制构建。--list、node --check、showConfig各只证明各自读取/语法/配置行为，不代替真实业务测试。本模块审的是四个配置的消费与声明，不把被调用未来模块所有行为一并宣称通过。

源四SHA在写本raw前全部实算等于输入，合计223行未改。实际/sidecar/ledger锁一致且COUNT0；B1后需等外席本次报告才能取第四值，当前不得先写B1双结论或模块PASS。无M005业务添加项/HTML修改申请，无未答问题。本席完整B1到此结束，等待对方完整报告后整批B2。

## V0.02 本席独立证据补录：实际端口探针完整命令

此段在未读Grok本次正文前追加，保留原36825字节独立报告，不是B2定稿或模块PASS。上述CX-02干净exit0对应的Node -e正文如下，cwd为160:/home/james/complex-management；只为探针导出已安装私有类，不改磁盘源码、业务环境或数据库。

```javascript
const fs=require('fs'),path=require('path'),Module=require('module'),net=require('net');
(async()=>{
 const p=path.resolve('node_modules/playwright/lib/runner/index.js');
 const m=new Module(p,module);m.filename=p;m.paths=Module._nodeModulePaths(path.dirname(p));
 m._compile(fs.readFileSync(p,'utf8')+'\nmodule.exports.__M005Probe=WebServerPlugin;\n',p);
 const server=net.createServer();await new Promise(r=>server.listen(0,'127.0.0.1',r));const port=server.address().port;await new Promise(r=>server.close(r));
 process.env.REAL_BACKEND_WEB_PORT=String(port)+'/; printf M005_ACTUAL_PLUGIN_MARKER; #';
 const {default:c}=await import('./playwright.real-backend.config.ts');
 const u=new URL(c.webServer.url);if(u.port!==String(port))throw Error('URL port unexpected');
 const lines=[];const plugin=new m.exports.__M005Probe({...c.webServer,command:'npm() { :; }\n'+c.webServer.command,stdout:'pipe',timeout:2000},false);
 let failure='';try{await plugin.setup({},process.cwd(),{onStdOut:x=>lines.push(String(x)),onStdErr:x=>lines.push(String(x))});}catch(e){failure=e.message;}finally{await plugin.teardown();}
 const marker=lines.join('').includes('M005_ACTUAL_PLUGIN_MARKER');console.log(JSON.stringify({parsedPort:u.port,pathAccepted:true,actualPluginShellMarker:marker,error:failure,onlyStubNpm:true}));
 process.exit(marker ? 0 : 2);
})().catch(e=>{console.error(e.message);process.exit(1);});
```

执行方式：Python subprocess.run(['node','-e',上述字符串],capture_output=True,text=True,timeout=30)。实际exit0输出parsedPort39311/pathAccepted true/actualPluginShellMarker true/onlyStubNpm true。先前终止的辅助进程未计成功；本段为干净重跑证据命令。

CX-01的npm原生校验可在相同cwd复现：

```javascript
const fs=require('fs'),cp=require('child_process'),m=require('module'); const n=fs.realpathSync(cp.execFileSync('which',['npm'],{encoding:'utf8'}).trim()); const {checkEngine}=m.createRequire(n)('npm-install-checks'); const l=JSON.parse(fs.readFileSync('package-lock.json','utf8')); for(const v of ['22.0.0','22.13.0','23.0.0','24.0.0']){let root=true,jsdom=true;try{checkEngine({engines:{node:'>=22'}},'11.19.0',v)}catch{root=false};try{checkEngine({engines:l.packages['node_modules/jsdom'].engines},'11.19.0',v)}catch(e){jsdom=e.code};console.log(JSON.stringify({node:v,root,jsdom}));}
```

本次node -e上述正文exit0，22.0/23.0根通过而jsdom报EBADENGINE；22.13/24均通过。没有把当下Node24的成功当成完整>=22支持证明。

## V0.03 外席接收：独立性异常处理（不新增盲审票）

本方原V0.01加V0.02独立证据共39710字节/SHA44961cabea4ca790edf19d0960d001aa34161c7c47eb35aa1d27b028cbc1fca3，完整写入后才读外席本次报告。Grok首次真实exit0、36359字节/460行/SHA7ee402a51c04c4493f78308f356875e6ac95244fb1863b81d97f982763a7671f已全部读完，给出FAIL/GR01—03；报告L40/L450自述工具检索smoke:p0时看到了本方raw三行索引。对应本方报告含分析/分类，无法将该票当独立票；“未采信/未打开其余正文”不能恢复互盲。原字节完整保留，问题内容也不静默丢弃，本段不改写原外席FAIL。

启动新无继承Grok进程从相同四源223行完整重审同一B1，在同名grok raw只追加完整报告，禁止仓库语义/索引搜索，所有命令按显式源码白名单，报告目录排除。不得读本方任何raw或自身旧报告，ledger只读基准和M005状态，不读这段接收意见。本方原独立报告保留，本接收段不是新盲审票。CLI原交付四条件满足，不记运行失败，不改B3=0，不改源，不进B2或下个模块。此前GR01—03作为未采信票中的观察保留待完整归并核查，不以独立性问题将实质疑点假称已解决。

## V0.04 完整外席新报告接收及补核（不是新盲审票）

有效Grok本次追加33320字节/385行/SHA d30dab7380be95bfe1cdfe293a7e19f71c851825516a1da7937f075014c65ac7，从全raw byte36359起到EOF，已经1—385行全部读完。原受影响36359字节完整保留；全69679字节/SHA bdcd2b52b3970093c2d1f4b5817e869f69ff7beae75521601b1603e63f52abc2。新报告FAIL/GR-01选择器漏15条、GR-02死别名。明确未使用索引/语义搜索、未读任何raw/self；看到的ledger过程短备注不含本方分析，不是新的分析泄漏。两席B1现在完整，不能提前PASS。

新外席误执行corepack prepare npm@10.9.8 --activate，属于超出只读审计意图的环境操作；事后本方实核/bin/npm仍11.19.0，核心4源及package-lock.json对HEAD无diff，corepack lastKnownGood.json当前npm10.9.8（另pnpm11.6.0/yarn4.18.0）。没有事前该缓存值证据，因此不能声称缓存未变，也不猜测旧值回滚。后续明确禁止corepack prepare/activate及任何全局包管理器配置动作；记录事实，不把它变成项目源finding或伪称所有环境全程只读。

补核首份报告的实质观察：本方git grep排除docs/audit-2026-09后，smoke:p0-ops仅package.json:48，确认新GR-02；本方先前笼统将全部别名都以便利性保留的说法收窄，该具体别名无已证用途，应按GEN001列队。此改判属于外席收到后的B2归并依据，不伪称最初盲审已发现。

旧GR-01专指不设REAL_BACKEND_E2E直接运行真实配置全skip；用.scratch TemporaryDirectory wrapper import原配置，仅移除webServer、绝对testDir并将outputDir重定向临时目录，运行原三用例（不连后端）得到exit0、expected0/skipped3/unexpected0/flaky0，.last-run.status passed；临时目录自动删除。该观察与新GR-01漏选15条并非同一原因，B2分别登记。

旧GR-02专指默认E2E通过dev:web继承0.0.0.0，当前源调用展开已证。这里只说明测试服务接口暴露超出127.0.0.1消费需求；默认mock口令不意味着后端被绕过，不接受旧报告可能暗示的已证线上越权。B2列候选最小host覆盖，需两席完整核准，不借受影响旧票直接修复。

B1后actual/sidecar/ledger/Grok最新完整报告四值本方实算均23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0，四源223行全部SHA保持。进入B2整批候选，B3仍0。旧GR编号只在对应报告内部有效，正式ID统一在findings分配，不能把两个报告不同GR-01混为一项。

## V0.05 M005 B2整批复核（本方）

结论：PASS。仅表示本方接受完整候选V0.48；Grok本次B2正文尚未读取，不能据此定稿/B3或模块PASS。该结论已在外席B2完成前于调度内存固定，待其进程完成后、本方读取外席B2正文前追加，以保持双方B1输入前缀固定。

候选输入：findings.md V0.48，171669字节/SHA927eb2ced3766ac3c54f2bc944ba0da3f5578e300384777d52e7e3d1d5375e22。原self输入前43563字节/V0.04/SHA552242925e80e266a7dd5b7aaf3c46eba2beb0d871edf50b30879bded93be783；其中39710字节为原独立B1报告。Grok输入前69679字节/SHAbdcd2b52b3970093c2d1f4b5817e869f69ff7beae75521601b1603e63f52abc2，其中有效新完整B1从byte36359起，SHAd30dab7380be95bfe1cdfe293a7e19f71c851825516a1da7937f075014c65ac7。旧Grok原票不参与独立合取，实质观察005/006由源与补核重新支撑。

全批12ID逐一复核：
- 001接受：实际npm checkEngine与逐依赖相容性输出支撑范围收窄；只改根engine和直接lock镜像，保留全部依赖记录，不重开M004审计。根package自身版本字段与lock自身版本递增服从用户版本铁律，不改应用semver/锁协议。
- 002接受：缺陷落到环境值→合法URL路径→实际WebServerPlugin shell:true完整链，不依赖仅静态字符串想象。Number+integer+1..65535校验并使用归一化number，已是最小原生机制；不夸称远端攻击者已获该环境控制。
- 003接受：四已存在真实spec共18条有相同现有flag，不增加业务测试或新npm入口；两匹配数组让默认32/6、真实18/4互斥并覆盖原50/10。这是配置调度缺口，不审未来spec业务断言。
- 004接受：git grep只定义、独立新Grok同证据；删除单个死别名保留有文档用途的smoke:p0。纠正本方初审把全部别名统称便利性的过宽结论。
- 005接受：直接以--config使用真实配置是其正常可执行入口，未设置既有flag时3skip/exit0已实际复现；候选选择抛错而非自动开启真实后端，保持无flag时不执行业务动作且不报假绿。与003漏选用例是独立原因。
- 006接受：现有测试消费者固定loopback而启动命令继承全接口，直接只覆盖host即可，不动共享web dev脚本。版本行与003共用一次；不额外加新服务器参数/隔离框架。仅按明确接口暴露事实收窄，不采用“mock口令意味着真实后端越权”的不成立推论。
- 007接受仅登记：package内部协议字段、workspaces、private/type和应用semver均有当前原生消费；无业务发明，队列否。
- 008接受仅登记：所有脚本与命令图完整覆盖；将确定缺陷归001—006，未对剩余入口无谓删除/重构；队列否。
- 009接受仅登记：两依赖实际import/CLI消费，npm版本声明本身合法；不借宿主npm差异扩依赖升级；队列否。
- 010接受仅登记：mock其余超时/报告/globalSetup/Chromium/复用均合法，当前问题由003/006单独归因；队列否。
- 011接受仅登记：real其余字段与wrapper结果文件和现有proxy一致，端口/选择器/缺flag由002/003/005归因；队列否。
- 012接受仅登记：tsconfig九项被真实extends消费，正负编译诊断已证；该文件不修；队列否。

分类全部技术性，工程缺陷6是/登记6否，队列6是6否，正式ID唯一、无漏并，无添加项/HTML变更/待问业务问题。有效新Grok的两个GR全部明确映射003/004；两项CX映射001/002；旧三GR分别映射005/006/004，没有用编号碰撞掩盖差集。未知外部脚本使用不是已证用途，不作假设性兼容分支。

候选修复只涉及四路径：package.json、package-lock.json根镜像、两个Playwright配置。tsconfig.base/三HTML/业务spec保持，所有版本维护具体归001/002/003。每条修复都有源码位置、冻结GEN、实际证据、最小候选与验证；无新判据/角色/闸/状态/计数器。B3必须整批6ID、前后临时索引tree，再双B4/全B5；本票不代表这些未来检查已经执行。

未执行候选源补丁，源仍为B1输入，B3=0。Corepack误激活作为运行记录保留，/bin/npm已实核仍11.19.0、事前缓存未知不猜测回滚；后续禁该命令，不把运行偏差隐藏或发明成业务源ID。
