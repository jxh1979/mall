CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M005 B5-r1 修复后全模块审计（本方）
版本：V0.01

本席结论：PASS。当前完整4文件232行、127根scripts、9个基础编译选项、两份Playwright配置及18个冻结GEN均完成第一性原理、顺从式、对抗式审计。本次新finding无。此时仅是本席票，不是模块双PASS；未读Grok本轮报告。

## 当前输入与独立性

控制V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。M005/322—01-root-config-and-guidance-03，NORMAL；main HEAD134d75d158ca6ff1343f616cce47676ecfc83e02，B3=1、运行失败0、ledger IN_PROGRESS。B4完成后先派Grok B5（15:18:20 UTC/session11912），再独立读取本模块全部232行及general定义，实际运行下列命令后写本报告；截至写入未读本次Grok raw/原始JSON/分析。未用任何语义索引或当前全工作区diff接触对方raw。

当前findings V0.50/173339字节/SHA4ea0f32c8c84f3fd384c69e235901864d2e086ff36bf86c16cefea4e964b571e，12ID队列全否。读取它只确认现有登记与队列，以下结论以当前源码和实际验证为据，不以B4 PASS代替全量复审。当前source四项SHA复算一致，35既有scratch保持；三HTML未修改。

必要直接消费者：apps/web/package.json、tsconfig.json、vite.config.ts，tests/e2e/support/global-setup.ts，tools/run-real-backend-ui-e2e.mjs，真实选择器/flag所在的既有spec，npm/TypeScript/Playwright/Vite已安装实现。只验证本模块接线/入口，不审未来模块业务正文。package-lock根engine镜像按已定稿001纳入一致性复核，不重开M004；实际JSON规范化回退两字段后与134d75d1锁逐字段相同。

技能实际使用：本模块已核using-superpowers/grilling/codebase-design/ponytail-review（全量三线）、diagnosing/systematic-debugging（原生反例）、tdd/test-driven-development（配置按类型4）、verification-before-completion（本次写PASS前读取并执行验证）、writing-for-agents（完整范围和完成标准）。requesting-code-review在B4实际读取且仅派Grok。无第三席/新判据/新产物；不把其他技能引入的审批、子代理或tracker写入当要求。无业务添加、HTML申请或待答问题。

## 1. 全文件与全字段覆盖
| 文件 | 行数 | 字节 | SHA256 |
|---|---:|---:|---|
| package.json | 147 | 15583 | 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f |
| playwright.config.ts | 26 | 816 | c1c72998765d8aa42b83f11c557a1e8ea886eb1c557207aab1ded8d5d5bd4700 |
| playwright.real-backend.config.ts | 46 | 1639 | 956e9484517c7620833962e3ec45d63e940e8de452a201ab9a9fd148e5355132 |
| tsconfig.base.json | 13 | 303 | 13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db |

### package.json L1—147

L1—9/L139—147：name是内部包名；private=true限制发布；version0.1.0为既有npm应用语义版本，documentVersion V0.01为用户批准的文件版本；type=module与真实mjs/配置import一致；唯一工作区apps/web真实存在；packageManager npm@10.9.8保留，实际主环境/bin/npm11.19.0如实记录、不激活或冒称相同；engines与lock根均^22.13.0或>=24.0.0，真实npm checkEngine矩阵如下；Playwright根CLI及yaml的OpenAPI使用均有真实入口。npm pkg get实际正确读取全部这些元数据，JSON重复键检查通过。

下面为全部127scripts逐项证据登记。每个node目标都真实存在并node --check；每个npm run按当前工作区作用域建立边并检查目标/环；Playwright/tsc/Vite/Vitest命令在实际node_modules/.bin存在。含docs/prepare的写操作只检查入口，未执行它们改业务文档；设备/DB/部署动作未运行，未据语法检查宣称它们的业务执行通过。脚本名只是已有命令入口，未发明同名业务规则。

| 行 | script | 全部命令 | 四分类与本模块验证 |
|---:|---|---|---|
| 11 | dev | npm run dev:backend | 技术性；npm转发解析/目标存在/无环；backend/README.md:66保留的既定迁移兼容入口 |
| 12 | dev:backend | node tools/run-backend-maven.mjs spring-boot:run | 技术性；node目标存在/语法检查0 |
| 13 | test:backend | node tools/run-backend-maven.mjs test | 技术性；node目标存在/语法检查0 |
| 14 | verify:backend-env | node tools/run-backend-maven.mjs --check | 技术性；node目标存在/语法检查0 |
| 15 | dev:api | npm run dev:backend | 技术性；npm转发解析/目标存在/无环；backend/README.md:66保留的既定迁移兼容入口 |
| 16 | dev:web | npm run dev --workspace apps/web | 技术性；npm转发解析/目标存在/无环 |
| 17 | verify:openapi | node tools/verify-openapi-contract.mjs | 技术性；node目标存在/语法检查0 |
| 18 | audit:client-routes | node tools/audit-client-api-routes.mjs | 技术性；node目标存在/语法检查0 |
| 19 | verify:client-routes | node tools/audit-client-api-routes.mjs --strict | 技术性；node目标存在/语法检查0 |
| 20 | audit:client-openapi | node tools/audit-client-openapi-coverage.mjs | 技术性；node目标存在/语法检查0 |
| 21 | verify:client-openapi | node tools/audit-client-openapi-coverage.mjs --strict | 技术性；node目标存在/语法检查0 |
| 22 | audit:client-payload | node tools/verify-client-payload-policy.mjs | 技术性；node目标存在/语法检查0 |
| 23 | verify:client-payload | node tools/verify-client-payload-policy.mjs --strict | 技术性；node目标存在/语法检查0 |
| 24 | docs:openapi-examples | node tools/apply-openapi-p0-examples.mjs | 技术性；node目标存在/语法检查0 |
| 25 | verify:openapi-examples | node tools/verify-openapi-p0-examples.mjs | 技术性；node目标存在/语法检查0 |
| 26 | docs:operation-matrix | node tools/generate-openapi-operation-matrix.mjs | 技术性；node目标存在/语法检查0 |
| 27 | verify:operation-matrix | node tools/generate-openapi-operation-matrix.mjs --check | 技术性；node目标存在/语法检查0 |
| 28 | docs:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs | 技术性；node目标存在/语法检查0 |
| 29 | verify:openapi-closure | node tools/generate-openapi-contract-closure-appendix.mjs --check | 技术性；node目标存在/语法检查0 |
| 30 | docs:security-baseline | node tools/generate-formal-security-baseline.mjs | 技术性；node目标存在/语法检查0 |
| 31 | docs:entry-baseline | node tools/refresh-entry-doc-baseline.mjs | 技术性；node目标存在/语法检查0 |
| 32 | verify:html-ledger-sync | node tools/verify-html-ledger-sync.mjs | 技术性；node目标存在/语法检查0 |
| 33 | verify:entry-anchors | node tools/verify-entry-anchors.mjs | 技术性；node目标存在/语法检查0 |
| 34 | verify:html-param-tier | node tools/verify-html-param-tier.mjs | 技术性；node目标存在/语法检查0 |
| 35 | verify:entry-doc-baseline | node tools/refresh-entry-doc-baseline.mjs --check | 技术性；node目标存在/语法检查0 |
| 36 | verify:security-baseline | npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict | 技术性；node目标存在/语法检查0 |
| 37 | verify:prod-docs-access | node tools/verify-prod-docs-access-policy.mjs | 技术性；node目标存在/语法检查0 |
| 38 | verify:ops160-execution-policy | node tools/verify-ops160-execution-policy.mjs | 技术性；node目标存在/语法检查0 |
| 39 | verify:ops160-backup-restore | node tools/verify-160-backup-restore-scripts.mjs | 技术性；node目标存在/语法检查0 |
| 40 | verify:production-credentials | node tools/verify-production-credential-rotation-ledger.mjs | 技术性；node目标存在/语法检查0 |
| 41 | verify:p0a-ops-hardening | node tools/verify-p0a-ops-hardening.mjs | 技术性；node目标存在/语法检查0 |
| 42 | verify:ops160-recovery | npm run verify:coding-host-protection && npm run verify:ops160-execution-policy && npm run verify:ops160-backup-restore && npm run verify:production-credentials && npm run verify:p0a-ops-hardening && npm run verify:nas-smb-mount-evidence | 技术性；npm转发解析/目标存在/无环 |
| 43 | verify:p0-contract | npm run verify:no-legacy-api-prototype && npm run verify:openapi && npm run verify:openapi-examples && npm run verify:operation-matrix && npm run verify:openapi-closure && npm run verify:client-routes && npm run verify:client-openapi && npm run verify:client-payload | 技术性；npm转发解析/目标存在/无环 |
| 44 | verify:p0-web | npm run verify:p0-contract && npm run build | 技术性；npm转发解析/目标存在/无环 |
| 45 | verify:p0-env | npm run verify:backend-env && npm run verify:android-env | 技术性；npm转发解析/目标存在/无环 |
| 46 | build | npm run build --workspace apps/web | 技术性；npm转发解析/目标存在/无环 |
| 47 | typecheck | npm run typecheck --workspace apps/web | 技术性；npm转发解析/目标存在/无环 |
| 48 | smoke:p0 | npm run test:backend | 技术性；npm转发解析/目标存在/无环；backend/README.md:66保留的既定迁移兼容入口 |
| 49 | verify:android-env | node tools/run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app | 技术性；node目标存在/语法检查0 |
| 50 | android:p0:build | node tools/run-android-gradle.mjs --project apps/android-terminal clean build | 技术性；node目标存在/语法检查0 |
| 51 | android:p0:smoke | node tools/run-android-p0-smoke.mjs | 技术性；node目标存在/语法检查0 |
| 52 | android:p0:apk | node tools/run-android-gradle.mjs --project apps/android-terminal/android-app --require-android-sdk :app:assembleDebug | 技术性；node目标存在/语法检查0 |
| 53 | verify:p0 | npm run verify:coding-host-protection && npm run verify:p0-contract && npm run verify:security-baseline && npm run test:backend && npm run build && npm run verify:android-env && npm run android:p0:build && npm run verify:android-p0-smoke-wrapper && npm run android:p0:smoke && npm run android:p0:apk | 技术性；npm转发解析/目标存在/无环 |
| 54 | verify:no-legacy-api-prototype | node tools/verify-no-legacy-api-prototype.mjs | 技术性；node目标存在/语法检查0 |
| 55 | verify:coding-host-protection | node tools/verify-coding-host-protection.mjs | 技术性；node目标存在/语法检查0 |
| 56 | verify:t3-coding-package | node tools/verify-t3-coding-package.mjs | 技术性；node目标存在/语法检查0 |
| 57 | verify:t3b-supplier-mock-kit | node tools/verify-t3b-supplier-mock-kit.mjs | 技术性；node目标存在/语法检查0 |
| 58 | verify:t3d-local-llm-ai-quality | node tools/verify-t3d-local-llm-ai-quality.mjs | 技术性；node目标存在/语法检查0 |
| 59 | verify:external-dependency-gates | node tools/verify-external-dependency-gates.mjs | 技术性；node目标存在/语法检查0 |
| 60 | verify:delivery-gates-docs | node tools/verify-delivery-gates.mjs | 技术性；node目标存在/语法检查0 |
| 61 | verify:delivery-gates | npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:migration-version-order && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:unit && npm run verify:coding-host-protection && npm run verify:external-dependency-gates && npm run verify:integration-capability-switches && npm run verify:external-evidence-requirement-traceability && npm run verify:partial-requirement-mock-coverage && npm run verify:t3-coding-package && npm run verify:t3b-supplier-mock-kit && npm run verify:t3d-local-llm-ai-quality && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:final-closed-loop-audit && npm run verify:closed-loop-current-state && npm run verify:runtime-acceptance-evidence && npm run verify:ui-experience-acceptance && npm run verify:production-readiness-evidence && npm run verify:nas-current-state-drift && npm run verify:nas-diagnostics && npm run verify:nas-it-handoff && npm run verify:production-blocker-register-no-go-fixtures && npm run verify:production-blockers && npm run verify:production-closure-gap-reports && npm run verify:production-evidence-backfill-execution-ledger && npm run verify:production-evidence-backfill-priority && npm run verify:production-evidence-crosslinks && npm run verify:production-evidence-draft-prep && npm run verify:production-evidence-draft-prep-fixtures && npm run verify:production-evidence-intake && npm run verify:production-evidence-manifest-templates && npm run verify:production-evidence-negative-fixtures && npm run verify:production-evidence-owner-execution-packets && npm run verify:production-evidence-owner-guides && npm run verify:production-evidence-p0-quickstart && npm run verify:production-evidence-positive-fixtures && npm run verify:production-evidence-positive-propagation-fixtures && npm run verify:production-evidence-submission-kit && npm run verify:production-evidence-submissions-guard && npm run verify:production-evidence-submission-status && npm run verify:production-no-go-command-center && npm run verify:production-owner-actions && npm run verify:production-ready-no-go-fixtures && npm run verify:production-ready-positive-fixtures | 技术性；npm转发解析/目标存在/无环 |
| 62 | verify:nas-smb-mount-evidence | node tools/verify-nas-smb-mount-evidence.mjs | 技术性；node目标存在/语法检查0 |
| 63 | verify:production-readiness-evidence | node tools/verify-production-readiness-evidence.mjs | 技术性；node目标存在/语法检查0 |
| 64 | docs:production-blockers | node tools/generate-production-blocker-register.mjs | 技术性；node目标存在/语法检查0 |
| 65 | verify:production-blockers | node tools/generate-production-blocker-register.mjs --check | 技术性；node目标存在/语法检查0 |
| 66 | verify:production-evidence-intake | node tools/verify-production-evidence-intake-templates.mjs | 技术性；node目标存在/语法检查0 |
| 67 | docs:production-owner-actions | node tools/generate-production-owner-action-packs.mjs | 技术性；node目标存在/语法检查0 |
| 68 | verify:production-owner-actions | node tools/generate-production-owner-action-packs.mjs --check | 技术性；node目标存在/语法检查0 |
| 69 | docs:production-evidence-submission-kit | node tools/generate-production-evidence-submission-kit.mjs | 技术性；node目标存在/语法检查0 |
| 70 | verify:production-evidence-submission-kit | node tools/generate-production-evidence-submission-kit.mjs --check | 技术性；node目标存在/语法检查0 |
| 71 | docs:production-evidence-submission-status | node tools/generate-production-evidence-submission-status.mjs | 技术性；node目标存在/语法检查0 |
| 72 | verify:production-evidence-submission-status | node tools/generate-production-evidence-submission-status.mjs --check | 技术性；node目标存在/语法检查0 |
| 73 | docs:production-evidence-manifest-templates | node tools/generate-production-evidence-manifest-templates.mjs | 技术性；node目标存在/语法检查0 |
| 74 | verify:production-evidence-manifest-templates | node tools/generate-production-evidence-manifest-templates.mjs --check | 技术性；node目标存在/语法检查0 |
| 75 | docs:production-evidence-owner-guides | node tools/generate-production-evidence-owner-guides.mjs | 技术性；node目标存在/语法检查0 |
| 76 | verify:production-evidence-owner-guides | node tools/generate-production-evidence-owner-guides.mjs --check | 技术性；node目标存在/语法检查0 |
| 77 | verify:production-evidence-submissions-guard | node tools/verify-production-evidence-submissions-guard.mjs | 技术性；node目标存在/语法检查0 |
| 78 | docs:production-evidence-backfill-priority | node tools/generate-production-evidence-backfill-priority.mjs | 技术性；node目标存在/语法检查0 |
| 79 | verify:production-evidence-backfill-priority | node tools/generate-production-evidence-backfill-priority.mjs --check | 技术性；node目标存在/语法检查0 |
| 80 | docs:production-evidence-backfill-execution-ledger | node tools/generate-production-evidence-backfill-execution-ledger.mjs | 技术性；node目标存在/语法检查0 |
| 81 | verify:production-evidence-backfill-execution-ledger | node tools/generate-production-evidence-backfill-execution-ledger.mjs --check | 技术性；node目标存在/语法检查0 |
| 82 | test:ui | playwright test | 技术性；原生Playwright入口/配置可加载；实际npm入口--list=32/6 |
| 83 | verify:ui | playwright test | 技术性；原生Playwright入口/配置可加载 |
| 84 | test:ui:update | playwright test --update-snapshots | 技术性；原生Playwright入口/配置可加载 |
| 85 | verify:ui:real-backend | REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts | 技术性；原生Playwright入口/配置可加载；实际npm入口--list=18/4 |
| 86 | verify:ui:real-backend:seeded | node tools/run-real-backend-ui-e2e.mjs | 技术性；node目标存在/语法检查0 |
| 87 | verify:android-p0-smoke-wrapper | node tools/verify-android-p0-smoke-wrapper.mjs | 技术性；node目标存在/语法检查0 |
| 88 | verify:full-admin-matrix | node tools/verify-full-admin-page-matrix.mjs | 技术性；node目标存在/语法检查0 |
| 89 | test:unit | npm run test:unit --workspace apps/web | 技术性；npm转发解析/目标存在/无环 |
| 90 | verify:unit | npm run test:unit --workspace apps/web | 技术性；npm转发解析/目标存在/无环 |
| 91 | docs:domain-ownership-scope | node tools/generate-domain-ownership-scope-audit.mjs | 技术性；node目标存在/语法检查0 |
| 92 | verify:domain-ownership-scope | node tools/generate-domain-ownership-scope-audit.mjs --check | 技术性；node目标存在/语法检查0 |
| 93 | verify:matrix-vs-migration | node tools/verify-matrix-vs-migration.mjs | 技术性；node目标存在/语法检查0 |
| 94 | verify:migration-version-order | node tools/verify-migration-version-order.mjs | 技术性；node目标存在/语法检查0 |
| 95 | verify:tenant-isolation | node tools/verify-tenant-isolation.mjs | 技术性；node目标存在/语法检查0 |
| 96 | verify:predeploy | npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation | 技术性；npm转发解析/目标存在/无环 |
| 97 | verify:pii-encryption | node tools/verify-pii-encryption.mjs | 技术性；node目标存在/语法检查0 |
| 98 | verify:test-db-isolation | node tools/verify-test-db-isolation.mjs | 技术性；node目标存在/语法检查0 |
| 99 | docs:production-closure-gap-reports | node tools/generate-production-closure-gap-report.mjs | 技术性；node目标存在/语法检查0 |
| 100 | verify:production-closure-gap-reports | node tools/generate-production-closure-gap-report.mjs --check && node tools/verify-production-closure-gap-reports.mjs | 技术性；node目标存在/语法检查0 |
| 101 | verify:final-closed-loop-audit | node tools/verify-final-closed-loop-audit.mjs | 技术性；node目标存在/语法检查0 |
| 102 | verify:production-evidence-negative-fixtures | node tools/verify-production-evidence-negative-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 103 | docs:nas-current-state-drift | node tools/generate-nas-current-state-drift-report.mjs | 技术性；node目标存在/语法检查0 |
| 104 | verify:nas-current-state-drift | node tools/generate-nas-current-state-drift-report.mjs --check | 技术性；node目标存在/语法检查0 |
| 105 | docs:closed-loop-current-state | node tools/generate-closed-loop-current-state.mjs | 技术性；node目标存在/语法检查0 |
| 106 | verify:closed-loop-current-state | node tools/generate-closed-loop-current-state.mjs --verify | 技术性；node目标存在/语法检查0 |
| 107 | verify:closed-loop-runtime | npm run test:backend && npm run typecheck --workspace apps/web && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:real-backend:seeded | 技术性；npm转发解析/目标存在/无环 |
| 108 | verify:nas-it-handoff | node tools/verify-nas-it-handoff.mjs | 技术性；node目标存在/语法检查0 |
| 109 | docs:external-evidence-requirement-traceability | node tools/generate-external-evidence-requirement-traceability.mjs | 技术性；node目标存在/语法检查0 |
| 110 | verify:external-evidence-requirement-traceability | node tools/generate-external-evidence-requirement-traceability.mjs --check | 技术性；node目标存在/语法检查0 |
| 111 | docs:nas-diagnostics | node tools/generate-nas-diagnostic-report.mjs | 技术性；node目标存在/语法检查0 |
| 112 | verify:nas-diagnostics | node tools/generate-nas-diagnostic-report.mjs --check | 技术性；node目标存在/语法检查0 |
| 113 | docs:production-evidence-p0-quickstart | node tools/generate-production-evidence-p0-quickstart.mjs | 技术性；node目标存在/语法检查0 |
| 114 | verify:production-evidence-p0-quickstart | node tools/generate-production-evidence-p0-quickstart.mjs --check | 技术性；node目标存在/语法检查0 |
| 115 | docs:runtime-acceptance-evidence | node tools/generate-runtime-acceptance-evidence.mjs | 技术性；node目标存在/语法检查0 |
| 116 | verify:runtime-acceptance-evidence | node tools/generate-runtime-acceptance-evidence.mjs --check | 技术性；node目标存在/语法检查0 |
| 117 | docs:ui-experience-acceptance | node tools/generate-ui-experience-acceptance-report.mjs | 技术性；node目标存在/语法检查0 |
| 118 | verify:ui-experience-acceptance | node tools/generate-ui-experience-acceptance-report.mjs --check | 技术性；node目标存在/语法检查0 |
| 119 | docs:production-evidence-owner-execution-packets | node tools/generate-production-evidence-owner-execution-packets.mjs | 技术性；node目标存在/语法检查0 |
| 120 | verify:production-evidence-owner-execution-packets | node tools/generate-production-evidence-owner-execution-packets.mjs --check | 技术性；node目标存在/语法检查0 |
| 121 | prepare:production-evidence-drafts | node tools/prepare-production-evidence-manifest-drafts.mjs | 技术性；node目标存在/语法检查0 |
| 122 | verify:production-evidence-draft-prep | node tools/prepare-production-evidence-manifest-drafts.mjs --check | 技术性；node目标存在/语法检查0 |
| 123 | verify:production-evidence-draft-prep-fixtures | node tools/verify-production-evidence-draft-prep-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 124 | verify:production-evidence-positive-fixtures | node tools/verify-production-evidence-positive-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 125 | verify:production-evidence-positive-propagation-fixtures | node tools/verify-production-evidence-positive-propagation-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 126 | verify:production-ready-no-go-fixtures | node tools/verify-production-ready-no-go-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 127 | verify:production-blocker-register-no-go-fixtures | node tools/verify-production-blocker-register-no-go-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 128 | verify:production-ready-positive-fixtures | node tools/verify-production-ready-positive-fixtures.mjs | 技术性；node目标存在/语法检查0 |
| 129 | verify:production-evidence-crosslinks | node tools/verify-production-evidence-crosslinks.mjs | 技术性；node目标存在/语法检查0 |
| 130 | docs:partial-requirement-mock-coverage | node tools/generate-partial-requirement-mock-coverage.mjs | 技术性；node目标存在/语法检查0 |
| 131 | verify:partial-requirement-mock-coverage | node tools/generate-partial-requirement-mock-coverage.mjs --check | 技术性；node目标存在/语法检查0 |
| 132 | docs:production-no-go-command-center | node tools/generate-production-no-go-resolution-command-center.mjs | 技术性；node目标存在/语法检查0 |
| 133 | verify:production-no-go-command-center | node tools/generate-production-no-go-resolution-command-center.mjs --check | 技术性；node目标存在/语法检查0 |
| 134 | docs:integration-capability-switches | node tools/generate-integration-capability-switches.mjs | 技术性；node目标存在/语法检查0 |
| 135 | verify:integration-capability-switches | node tools/generate-integration-capability-switches.mjs --check | 技术性；node目标存在/语法检查0 |
| 136 | docs:data-isolation-spec | node tools/generate-data-isolation-spec.mjs | 技术性；node目标存在/语法检查0 |
| 137 | verify:data-isolation-spec | node tools/generate-data-isolation-spec.mjs --check | 技术性；node目标存在/语法检查0 |

图全量结果：127根+6工作区=133节点，107条npm run边、75个去重node目标，悬空0、循环0、语法失败0；10个原生命令片段均有安装的本地bin。特意按workspace解析build/typecheck，未把同名工作区脚本误判根自循环。

纯转发dev/dev:api/smoke:p0在backend/README.md:66明确要求持续指向正式Java后端，当前展开到run-backend-maven.mjs的spring-boot:run/test；不是无需求重复。初次按“npm run dev”做字符串搜索会匹配dev:web等前缀，仅是查找线索，不作精确调用数量；smoke:p0不带npm前缀的文档约定也被补查确认。smoke:p0-ops已无定义/受控引用，不因其与其它三个都转发就一律删除。以上兼容是技术入口事实，不是第四业务来源。

### playwright.config.ts 全26行

L1文件版本、L2真实依赖导入；L4—7 defineConfig、testDir存在、两项ignore与真实套件互补，globalSetup路径存在；L8—10测试/期望超时与非fullyParallel服务当前本地UI测试；L11 list/html输出与never自动开页；L12—16 loopback baseURL及失败trace/screenshot；L17—22现有workspace dev命令、就绪URL、!CI复用、启动超时；L23—26单Chromium Desktop Chrome项目和闭合结构。全部字段被原生配置加载消费，未写无用途额外开关。

globalSetup全36行已读：加载installMockApi，暖机登录页/admin shell，失败仅warn并让真正测试承担断言，finally关闭browser。不是用暖机成功替代业务断言或给真实后端开权限。mock凭据为测试夹具，不将其称生产秘密。未执行浏览器/业务动作。

### playwright.real-backend.config.ts 全46行

L1—6文件版本/import/flag显式1护栏；L8—13操作员配置的backend目标、端口数值/整数范围校验、纯数值URL；L15—20目录、双模式选择器、超时与串行；L21—27可验证JSON注释、list/html/json三reporter；L28—32共用webBaseURL/失败trace与截图；L33—42 localhost服务命令、strictPort、同URL就绪、不复用、120秒启动上界、env继承且覆盖VITE_API_PROXY_TARGET；L43—46单Chromium项目。当前两条中文错误能被真正CLI打印，内部变量名不算面向业务用户非中文文案。

tools/run-real-backend-ui-e2e.mjs全189行作为接线消费已读：resultsPath第19行与配置json output同路径；第145行清旧结果；170—176通过原npm入口传flag/真实URL/用户凭据；178行真实检查stats期望>=1、零skip/fail/flaky。未在本审计启动该wrapper、后端或DB；上述为源码接线证据，不能说18条真实业务已执行通过。backendBaseUrl仅传给Vite proxy的环境参数，不拼入shell；端口模板只接受已归一化整数，已完成实际反例。

### tsconfig.base.json 全13行/9选项

target ES2022、module ESNext、moduleResolution Bundler支持当前Vite TS编译；strict包含noImplicitAny/strictNullChecks；esModuleInterop支持既有ESM互用；forceConsistentCasingInFileNames约束大小写；skipLibCheck只跳依赖声明检查不跳项目类型；resolveJsonModule支持JSON模块；noUncheckedIndexedAccess把未知索引纳入undefined。apps/web/tsconfig.json实际extends该文件，新增jsx/react-jsx和vite/client types；实际--showConfig显示九项全部生效（见输出），不是只读注释推断。原生TypeScript内存正反例为good=[]、bad=[2322,7006]，证明关键严格分支会失败，虚拟source未落磁盘。

## 2. 三线全模块复审

第一性原理：根包提供被README/ops/工具使用的执行入口与既有依赖；两个测试配置分别消费mock/real现有spec而非创造业务；基础TS配置被web extends实际消费。每项到真实文件、CLI/编译器或可复现输出停止，不用“大家都这样”作为依据。127脚本并非127个业务要求。

顺从式：逐字段覆盖如上；实际npm入口、native --list、原生TS、引擎、JSON镜像与所有脚本转发均与声明一致。四分类先查有无业务命题和HTML矛盾：本四文件全部为技术性，业务名称仅存在于工具/测试标识；没有钱账/资格/角色/生命周期规则，不属于细化或添加。既有M005-001—012技术登记继续适用，无新的业务内容。

对抗式：端口0/65536/NaN/小数/合法URL路径分号；flag缺失/空/0/true；CI本地复用分支；两套选择器遗漏/重复；实际npm转发是否吞参数；Vite重复host是否首值覆盖；Node22.12/23相容性；TS未知索引与隐式any；JSON重复键；workspace同名脚本误判循环；别名未检到npm前缀是否就是无用途。逐一用下列原生工具及精确文档消费者确认。无本模块未修缺陷。未将更远未执行业务场景当作正常首跑确定缺陷。

## 3. 冻结18GEN逐项结论

| GEN | 本模块应用及证据 | 结论 |
|---|---|---|
| 001 必要性最小性 | 全127脚本有真实目标，3迁移兼容别名有README:66约定；配置均原生消费，死别名已删除、无新抽象 | PASS |
| 002 可执行正确性 | 75入口语法、133节点/107边、两实际npm列表入口、原生配置/TypeScript和OpenAPI真实exit0；不扩称业务命令执行 | PASS |
| 003 测试有效性 | 同代码先红后绿证据B4保留，本B5再执行12断言0失败；TS正负0 vs2322/7006；当前mock32/real18完整分组 | PASS |
| 004 信任边界 | flag严格1；端口在URL/shell前number/整数/范围；后端目标按Vite代理参数消费，未拼shell；文件内容由npm/TS原生解析 | PASS |
| 005 授权最小权限 | 本模块无服务端授权处理；真实flag不得隐式开启，localhost绑定；不把fixtureUI隐藏当业务授权 | PASS（业务对象授权本模块无该路径） |
| 006 数据完整性 | 本模块无钱账/DB事务/业务状态写入；root-lock元数据一致，npm图无悬空；不空泛宣称业务事务正确 | PASS（业务事务本模块无该路径） |
| 007 并发幂等 | 两套输出目录分开、real strictPort且不复用，fullyParallel=false；无本模块业务重复投递/共享数据库写入实现 | PASS |
| 008 接口模式一致性 | module/extends/工作区解析一致；real reporter与wrapper字段/路径一致；OpenAPI入口实际验证0 | PASS |
| 009 错误恢复 | 缺flag、非法port立即非0且明确中文；TS反例可失败；warmup warning不会替代真测试；不把列表当全绿 | PASS |
| 010 安全敏感信息 | env端口先数值化、服务只loopback；本文件不含秘密，backend URL不进shell；不输出完整process.env | PASS |
| 011 证据可观测 | list/html/json和trace/screenshot有原生consumer；wrapper原代码核stats且清旧结果；本次命令和输出完整保留 | PASS |
| 012 资源性能边界 | 30/45秒测试、8/10秒expect、120秒webServer有界；globalSetup finally关browser；本模块无无限循环/缓存 | PASS |
| 013 构建依赖 | root engines与lock同步，npm原生相容检查，全部现有本地bin，TS继承实际生效，未依赖新全局激活 | PASS |
| 014 数据库迁移 | 模块不含SQL或迁移执行算法；对应verify:migration-version-order入口真实存在/语法通过，不因此认定全DB迁移绿 | PASS（数据库迁移实现不在本模块） |
| 015 界面可用性 | 本模块无产品UI；中文throw可读、内部标识保持协议；配置不会新增界面操作 | PASS（产品可访问性无该路径） |
| 016 文档可验证 | 路径/版本/命令/JSON注释逐一对当前消费核验；npm应用semver与文件版本明确；没有把--list或旧审计说成业务通过 | PASS |
| 017 业务来源分类 | 四文件逐项技术性，127入口无业务发明；未改三最高HTML/新增角色资格 | PASS |
| 018 第一性原理证据 | 当前可执行产物与本次命令输出，具体行/路径；无用席位意见替代依据 | PASS |

## 4. 全量脚本与入口验证

### 脚本图/全部目标/别名查找/实际OpenAPI

160根目录python3 -，实际exit0。
```python
from pathlib import Path
import json,subprocess,shlex,re,collections
def unique_pairs(pairs):
 d={}
 for k,v in pairs:
  assert k not in d,('duplicate',k);d[k]=v
 return d
pkgs={s:json.loads(Path(f).read_text(),object_pairs_hook=unique_pairs) for s,f in [('root','package.json'),('apps/web','apps/web/package.json')]}
graph={(s,n):[] for s,p in pkgs.items() for n in p['scripts']}
targets=set();external=set();missing=[];kinds=collections.Counter()
for (scope,name),edges in graph.items():
 cmd=pkgs[scope]['scripts'][name]
 for part in cmd.split('&&'):
  words=shlex.split(part)
  while words and re.match(r'^\w+=',words[0]):words.pop(0)
  if words[0]=='npm':
   wi=next((i for i,x in enumerate(words) if x in ['--workspace','-w']),None)
   ws=words[wi+1] if wi is not None else next((x.split('=',1)[1] for x in words if x.startswith('--workspace=')),scope)
   ni=words.index('run');n=words[ni+1];dest=(ws,n);edges.append(dest)
   if dest not in graph:missing.append((scope,name,dest))
   kinds['npm']+=1
  elif words[0]=='node':
   f=str((Path('.') if scope=='root' else Path(scope))/words[1]);targets.add(f);kinds['node']+=1
   if not Path(f).is_file():missing.append((scope,name,f))
  else:
   external.add((scope,words[0]));kinds['native']+=1
cycles=[]
def visit(x,stack,done):
 if x in stack:cycles.append(stack[stack.index(x):]+[x]);return
 if x in done or x not in graph:return
 for y in graph[x]:visit(y,stack+[x],done)
 done.add(x)
done=set()
for x in graph:visit(x,[],done)
bad=[]
for f in sorted(targets):
 p=subprocess.run(['node','--check',f],capture_output=True,text=True)
 if p.returncode:bad.append((f,p.stderr))
for scope,exe in sorted(external):
 bins=[Path('node_modules/.bin')/exe,Path(scope)/'node_modules/.bin'/exe]
 assert any(p.exists() for p in bins),(scope,exe)
print(json.dumps({'rootScripts':len(pkgs['root']['scripts']),'workspaceScripts':len(pkgs['apps/web']['scripts']),'graphNodes':len(graph),'npmEdges':sum(map(len,graph.values())),'nodeTargets':len(targets),'missing':missing,'cycles':cycles,'nodeSyntaxFailures':bad,'nativeCommands':sorted(external),'segmentKinds':kinds},ensure_ascii=False))
assert not missing and not cycles and not bad
for name,cmd in pkgs['root']['scripts'].items():
 if re.fullmatch(r'npm run [\w:-]+',cmd):
  p=subprocess.run(['git','grep','-n','-F','npm run '+name,'--',':(exclude)docs/audit-2026-09/**',':(exclude)docs/_archive/**',':(exclude)package.json'],capture_output=True,text=True)
  print('FORWARD',name,'REFS',p.returncode,p.stdout[:1600].replace('\n',' | '))
print('ROOT_LOCK_ENGINE',pkgs['root']['engines']==json.loads(Path('package-lock.json').read_text())['packages']['']['engines'])
p=subprocess.run(['npm','run','verify:openapi'],capture_output=True,text=True);print('OPENAPI',p.returncode,p.stdout,p.stderr);assert p.returncode==0
```

```text
{"rootScripts": 127, "workspaceScripts": 6, "graphNodes": 133, "npmEdges": 107, "nodeTargets": 75, "missing": [], "cycles": [], "nodeSyntaxFailures": [], "nativeCommands": [["apps/web", "tsc"], ["apps/web", "vite"], ["apps/web", "vitest"], ["root", "playwright"]], "segmentKinds": {"npm": 107, "node": 108, "native": 10}}
FORWARD dev REFS 0 README.en.md:50:npm run dev:web             # start admin web (vite, :5173) | README.en.md:60:npm run dev:backend         # start backend (spring-boot:run, :8080) | README.md:61:npm run dev:web             # 启动 Web 管理端（vite，:5173） | README.md:72:npm run dev:backend         # 启动后端（spring-boot:run，默认 :8080） | backend/README.md:29:npm run dev:backend | docs/closed-loop-live-acceptance-audit-20260618.md:62:机器 shell 当前导出的 `COMPLEX_DATABASE_URL` 缺少 `jdbc:` 前缀，会同样影响 `npm run dev:backend`（直接启动后端）。建议在 `.env` 或 shell 中改为： | docs/code-contract-difference-and-migration-audit-v0.1.md:128:When 执行 `npm run dev:api`、`npm run prisma:migrate`、`npm run seed:p0` 或 `npm run dev --workspace apps/api` 等命令，   | docs/superpowers/plans/2026-06-14-p0-runtime-service-scripts.md:35:Start API from `backend/target/complex-api-0.1.0-SNAPSHOT.jar` on `COMPLEX_API_PORT`, building the jar if missing. Start Web on `WEB_PORT` by preferring `npm run dev --workspace apps/web -- --host 127.0.0.1 --port 3000 --strictPort`. | ops/160/complex-lan-prod.sh:127:    npm run dev:backend >"$API_LOG" 2>&1 & | ops/160/complex-runtime.sh:133:  setsid npm run dev --workspace apps/web -- --host "$WEB_HOST" --port "$WEB_PORT" --strictPort > "$LOG_DIR/web.log" 2>&1 < /dev/null & | tools/run-backend-maven.mjs:106:      "当前 `npm run test:backend`、`npm run dev:backend` 和 `verify:p0` 都会先经过该前置校验。", | tools/run-backend-maven.mjs:147:    "示例：`npm run test:backend` 或 `npm run dev:backend`。", | 
FORWARD dev:api REFS 0 docs/code-contract-difference-and-migration-audit-v0.1.md:128:When 执行 `npm run dev:api`、`npm run prisma:migrate`、`npm run seed:p0` 或 `npm run dev --workspace apps/api` 等命令，   | 
FORWARD smoke:p0 REFS 1 
ROOT_LOCK_ENGINE True
OPENAPI 0 
> entertainment-complex-management@0.1.0 verify:openapi
> node tools/verify-openapi-contract.mjs

OpenAPI contract verification passed.
 
```

### 原生TypeScript及flag/CI矩阵

160根目录python3 -，实际exit0。
```python
import os,subprocess,json
checks=[]
js="""const ts=require('typescript'),path=require('node:path'); const raw=ts.readConfigFile('tsconfig.base.json',ts.sys.readFile);if(raw.error)throw Error('config load');const result=ts.convertCompilerOptionsFromJson(raw.config.compilerOptions,process.cwd());if(result.errors.length)throw Error('config parse');const opts={...result.options,noEmit:true};
function check(source){const f=path.resolve('.scratch/m005-b5-type-probe.ts');const host=ts.createCompilerHost(opts);const orig=host.getSourceFile.bind(host);host.getSourceFile=(name,languageVersion,...rest)=>path.resolve(name)===f?ts.createSourceFile(f,source,languageVersion,true):orig(name,languageVersion,...rest);const program=ts.createProgram([f],opts,host);return ts.getPreEmitDiagnostics(program).map(d=>d.code);}
const good=check('const rows: Record<string, number> = {a: 1}; const n: number | undefined = rows.a; export {n};');
const bad=check('const rows: Record<string, number> = {}; const n: number = rows.missing; function echo(x) { return x; } export {n, echo};');console.log(JSON.stringify({baseOptionCount:Object.keys(raw.config.compilerOptions).length,good,bad}));if(good.length||!bad.includes(2322)||!bad.includes(7006))process.exitCode=1;
"""
p=subprocess.run(['node','-e',js],capture_output=True,text=True);print('TYPESCRIPT_POS_NEG',p.returncode,p.stdout,p.stderr);assert p.returncode==0
p=subprocess.run(['node','node_modules/typescript/bin/tsc','--showConfig','-p','apps/web/tsconfig.json'],capture_output=True,text=True);assert p.returncode==0
t=json.loads(p.stdout);print('TSC_EFFECTIVE_OPTIONS',json.dumps(t['compilerOptions'],ensure_ascii=False))
for k in ['strict','esModuleInterop','forceConsistentCasingInFileNames','skipLibCheck','resolveJsonModule','noUncheckedIndexedAccess']:assert t['compilerOptions'][k]
print('TSC_INHERITANCE_PASS')
for flag in [None,'','0','true','1']:
 for ci in [None,'true']:
  env=os.environ.copy()
  if flag is None:env.pop('REAL_BACKEND_E2E',None)
  else:env['REAL_BACKEND_E2E']=flag
  if ci is None:env.pop('CI',None)
  else:env['CI']=ci
  env.pop('REAL_BACKEND_WEB_PORT',None)
  cjs="Promise.all([import('./playwright.config.ts'),import('./playwright.real-backend.config.ts')]).then(([a,b])=>console.log(JSON.stringify({mockReuse:a.default.webServer.reuseExistingServer,realReuse:b.default.webServer.reuseExistingServer,realReports:b.default.reporter,proxy:b.default.webServer.env.VITE_API_PROXY_TARGET})))"
  p=subprocess.run(['node','--input-type=module','-e',cjs],env=env,capture_output=True,text=True)
  print('FLAG_CI',repr(flag),repr(ci),'EXIT',p.returncode,'OUT',p.stdout.strip())
  assert (p.returncode==0)==(flag=='1')
  if flag=='1':
   v=json.loads(p.stdout);assert v['mockReuse']==(ci is None) and v['realReuse']==False
```

```text
TYPESCRIPT_POS_NEG 0 {"baseOptionCount":9,"good":[],"bad":[2322,7006]}
 
TSC_EFFECTIVE_OPTIONS {"target": "es2022", "module": "esnext", "moduleResolution": "bundler", "strict": true, "esModuleInterop": true, "forceConsistentCasingInFileNames": true, "skipLibCheck": true, "resolveJsonModule": true, "noUncheckedIndexedAccess": true, "jsx": "react-jsx", "types": ["vite/client"], "allowSyntheticDefaultImports": true, "resolvePackageJsonExports": true, "resolvePackageJsonImports": true, "useDefineForClassFields": true, "noImplicitAny": true, "noImplicitThis": true, "strictNullChecks": true, "strictFunctionTypes": true, "strictBindCallApply": true, "strictPropertyInitialization": true, "strictBuiltinIteratorReturn": true, "alwaysStrict": true, "useUnknownInCatchVariables": true}
TSC_INHERITANCE_PASS
FLAG_CI None None EXIT 1 OUT 
FLAG_CI None 'true' EXIT 1 OUT 
FLAG_CI '' None EXIT 1 OUT 
FLAG_CI '' 'true' EXIT 1 OUT 
FLAG_CI '0' None EXIT 1 OUT 
FLAG_CI '0' 'true' EXIT 1 OUT 
FLAG_CI 'true' None EXIT 1 OUT 
FLAG_CI 'true' 'true' EXIT 1 OUT 
FLAG_CI '1' None EXIT 0 OUT {"mockReuse":true,"realReuse":false,"realReports":[["list"],["html",{"outputFolder":"playwright-report-real-backend","open":"never"}],["json",{"outputFile":"test-results/real-backend-results.json"}]],"proxy":"http://127.0.0.1:8080"}
FLAG_CI '1' 'true' EXIT 0 OUT {"mockReuse":false,"realReuse":false,"realReports":[["list"],["html",{"outputFolder":"playwright-report-real-backend","open":"never"}],["json",{"outputFile":"test-results/real-backend-results.json"}]],"proxy":"http://127.0.0.1:8080"}
```

### 同份配置回归检查

160根目录python3 -，实际exit0。
```python
from pathlib import Path
import os,subprocess,json,re,sys
checks=[]
js="import('./playwright.real-backend.config.ts').then(({default:c})=>console.log(JSON.stringify({url:c.use.baseURL})))"
for port,want in [('1',True),('5174',True),('65535',True),('0',False),('65536',False),('abc',False),('5174.5',False),('5174/; printf M005_NO_EXEC; #',False)]:
 env=os.environ.copy();env['REAL_BACKEND_E2E']='1';env['REAL_BACKEND_WEB_PORT']=port
 p=subprocess.run(['node','--input-type=module','-e',js],env=env,capture_output=True,text=True)
 checks.append({'case':'port '+port,'ok':(p.returncode==0)==want,'exit':p.returncode})
env=os.environ.copy();env.pop('REAL_BACKEND_E2E',None);env.pop('REAL_BACKEND_WEB_PORT',None)
p=subprocess.run(['node','--input-type=module','-e',js],env=env,capture_output=True,text=True)
checks.append({'case':'missing real flag rejects before starting services','ok':p.returncode!=0,'exit':p.returncode})
files=[]
for config,want in [('playwright.config.ts',(32,6)),('playwright.real-backend.config.ts',(18,4))]:
 env=os.environ.copy();env['REAL_BACKEND_E2E']='1';env.pop('REAL_BACKEND_WEB_PORT',None)
 p=subprocess.run(['node','node_modules/@playwright/test/cli.js','test','--config='+config,'--list','--reporter=list'],env=env,capture_output=True,text=True)
 hit=re.search(r'Total: (\d+) tests in (\d+) files?',p.stdout);got=tuple(map(int,hit.groups())) if hit else None
 selected=set(re.findall(r'› ([^\s:]+\.spec\.ts):',p.stdout));files.append(selected)
 checks.append({'case':config+' selects intended suite','ok':p.returncode==0 and got==want,'exit':p.returncode,'got':got,'want':want})
checks.append({'case':'mock and real partition all existing spec files','ok':not files[0]&files[1] and len(files[0]|files[1])==10,'intersection':sorted(files[0]&files[1]),'union':len(files[0]|files[1])})
print(json.dumps({'checks':checks,'failures':sum(not r['ok'] for r in checks)},ensure_ascii=False))
sys.exit(1 if any(not r['ok'] for r in checks) else 0)
```

```text
{"checks": [{"case": "port 1", "ok": true, "exit": 0}, {"case": "port 5174", "ok": true, "exit": 0}, {"case": "port 65535", "ok": true, "exit": 0}, {"case": "port 0", "ok": true, "exit": 1}, {"case": "port 65536", "ok": true, "exit": 1}, {"case": "port abc", "ok": true, "exit": 1}, {"case": "port 5174.5", "ok": true, "exit": 1}, {"case": "port 5174/; printf M005_NO_EXEC; #", "ok": true, "exit": 1}, {"case": "missing real flag rejects before starting services", "ok": true, "exit": 1}, {"case": "playwright.config.ts selects intended suite", "ok": true, "exit": 0, "got": [32, 6], "want": [32, 6]}, {"case": "playwright.real-backend.config.ts selects intended suite", "ok": true, "exit": 0, "got": [18, 4], "want": [18, 4]}, {"case": "mock and real partition all existing spec files", "ok": true, "intersection": [], "union": 10}], "failures": 0}
```

### 原生引擎与实际npm/Vite命令

160根目录python3 -，实际exit0。
```python
from pathlib import Path
import subprocess,json,os,base64,shlex
nodejs="""const {createRequire}=require('node:module'); const requireNpm=createRequire('/usr/lib/node_modules/npm/package.json');const semver=requireNpm('semver');const {checkEngine}=requireNpm('npm-install-checks'); const p=require('./package.json'),l=require('./package-lock.json');
for(const v of ['22.0.0','22.12.0','22.13.0','23.0.0','24.0.0','24.20.0']) {let code='PASS';try{checkEngine(p,process.versions.npm||'11.19.0',v)}catch(e){code=e.code};const bad=Object.entries(l.packages).filter(([k,p])=>p.engines?.node&&!semver.satisfies(v,p.engines.node)); console.log(JSON.stringify({node:v,root:code,incompatible:bad.length})); if((code==='PASS')!==(bad.length===0))process.exitCode=1}
"""
p=subprocess.run(['node','-e',nodejs],capture_output=True,text=True);print('ENGINE_CHECK',p.returncode,p.stdout);assert p.returncode==0
js="""import {registerHooks} from 'node:module';import {pathToFileURL} from 'node:url';
const target=pathToFileURL('/home/james/complex-management/node_modules/vite/dist/node/cli.js').href;
registerHooks({load(url,ctx,next){const r=next(url,ctx);if(url===target){const s=String(r.source);const needle='filterDuplicateOptions(options);';if(!s.includes(needle))throw Error('missing native anchor');return {...r,source:s.replace(needle,needle+' console.log("M005_PARSED="+JSON.stringify(options)); return;')}}return r}});
"""
env=os.environ.copy();env['NODE_OPTIONS']='--import=data:text/javascript;base64,'+base64.b64encode(js.encode()).decode()
config=json.loads(subprocess.check_output(['node','--input-type=module','-e',"import('./playwright.config.ts').then(({default:c})=>console.log(JSON.stringify(c.webServer)))"],text=True))
p=subprocess.run(shlex.split(config['command']),env=env,capture_output=True,text=True);print('ACTUAL_NPM_VITE_PARSER',p.returncode,p.stdout,p.stderr)
hit=next(x.split('=',1)[1] for x in p.stdout.splitlines() if x.startswith('M005_PARSED='))
opts=json.loads(hit);assert p.returncode==0 and opts['host']=='127.0.0.1' and int(opts['port'])==5173
print('NO_SERVER_STARTED native action returned after option parse')
p=subprocess.run(['git','grep','-n','-F','smoke:p0-ops','--',':(exclude)docs/audit-2026-09/**',':(exclude)docs/_archive/**',':(exclude).scratch/**'],capture_output=True,text=True)
print('DELETED_ALIAS_REFERENCE_SEARCH',p.returncode,p.stdout);assert p.returncode==1
```

```text
ENGINE_CHECK 0 {"node":"22.0.0","root":"EBADENGINE","incompatible":27}
{"node":"22.12.0","root":"EBADENGINE","incompatible":2}
{"node":"22.13.0","root":"PASS","incompatible":0}
{"node":"23.0.0","root":"EBADENGINE","incompatible":10}
{"node":"24.0.0","root":"PASS","incompatible":0}
{"node":"24.20.0","root":"PASS","incompatible":0}

ACTUAL_NPM_VITE_PARSER 0 
> apps-web@0.1.0 dev
> vite --host 0.0.0.0 --port 5173 --host 127.0.0.1

M005_PARSED={"host":"127.0.0.1","port":5173}
 
NO_SERVER_STARTED native action returned after option parse
DELETED_ALIAS_REFERENCE_SEARCH 1 
```

### 真实npm公开入口与元数据

160根目录python3 -，实际exit0。
```python
import subprocess,json
for args in [
 ['npm','pkg','get','name','private','version','documentVersion','type','workspaces','packageManager','engines','devDependencies'],
 ['npm','run','test:ui','--','--list','--reporter=list'],
 ['npm','run','verify:ui:real-backend','--','--list','--reporter=list']
]:
 p=subprocess.run(args,capture_output=True,text=True)
 if '--list' in args:
  lines=p.stdout.splitlines();summary=[x for x in lines if x.startswith('Total:') or x.startswith('>')]
  print('CMD',json.dumps(args),'EXIT',p.returncode,'OUTPUT',json.dumps(summary,ensure_ascii=False))
  assert any(x=='Total: '+('18 tests in 4 files' if 'verify:ui:real-backend' in args else '32 tests in 6 files') for x in lines)
 else:print('CMD',json.dumps(args),'EXIT',p.returncode,'OUTPUT',p.stdout)
 assert p.returncode==0
```

```text
CMD ["npm", "pkg", "get", "name", "private", "version", "documentVersion", "type", "workspaces", "packageManager", "engines", "devDependencies"] EXIT 0 OUTPUT {
  "name": "entertainment-complex-management",
  "private": true,
  "version": "0.1.0",
  "documentVersion": "V0.01",
  "type": "module",
  "workspaces": "apps/web",
  "packageManager": "npm@10.9.8",
  "engines": {
    "node": "^22.13.0 || >=24.0.0"
  },
  "devDependencies": {
    "@playwright/test": "^1.61.0",
    "yaml": "^2.9.0"
  }
}

CMD ["npm", "run", "test:ui", "--", "--list", "--reporter=list"] EXIT 0 OUTPUT ["> entertainment-complex-management@0.1.0 test:ui", "> playwright test --list --reporter=list", "Total: 32 tests in 6 files"]
CMD ["npm", "run", "verify:ui:real-backend", "--", "--list", "--reporter=list"] EXIT 0 OUTPUT ["> entertainment-complex-management@0.1.0 verify:ui:real-backend", "> REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts --list --reporter=list", "Total: 18 tests in 4 files"]
```

Vite验证hook只在真实CLI参数解析后打印并return，既有parser与npm转发不替换，未启动服务；TS检查仅覆盖进程内虚拟source，不写文件。npm pkg get把单个workspaces结果显示为字符串是命令输出形式，package.json真实字段仍是数组，未改其类型。

## 5. 边界与结论

已重新阅读完整232行并对127脚本、两配置每字段、9编译选项、18GEN完成三线。检查均有实际当前命令/返回值；本次不读取Grok B5 raw，不改源码/HTML/计划、不运行corepack激活、不装依赖、不触发生产/DB。B3仍1，失败重跑0，12正式ID队列全否，未答问题0。

本席PASS；本次新finding无。必须待Grok完整报告真实交付并审读、B5后四值全等才能写ledger PASS，再写模块收口并按已有授权发布；此报告不是先行收口。

## B5本方参数接口补核
版本：V0.02

在读取Grok本次正文前，对根scripts中带参数的36种node调用另核其目标源码的process.argv/对应flag解析行，解决“语法有效但传参没有消费者”的疑点。输出逐目标包含命令名、参数和真实行号；--check、--verify、--strict、--project及--require-android-sdk均有解析，Maven目标由args.slice(2)转发。未执行生成器/设备/数据库命令，故这里只证明声明参数被识别，不宣称它们的完整运行和只读语义已动态测试。无新增finding，原完整三线审计与结论保持。

复现方法：对package.json scripts各&&片段用shlex.split，筛node且有后续参数，按(file,args)去重；读取对应源文件，仅显示process.argv或显式参数字串所在行。仅为本模块的直接调用接口复核，不审这些工具的业务正文，不更改其他模块。

原独立完整前51912字节/SHA4a4d220c59f122d7170a44af9c2c41c0d1e4d19e0629b6e6440e39d08b8fb962完整保留。本节也在Grok正文读取前落盘，无互看。

```text
ENTRY dev:backend TARGET tools/run-backend-maven.mjs ARGS ["spring-boot:run"]
9: const args = process.argv.slice(2);
ENTRY test:backend TARGET tools/run-backend-maven.mjs ARGS ["test"]
9: const args = process.argv.slice(2);
ENTRY verify:backend-env TARGET tools/run-backend-maven.mjs ARGS ["--check"]
9: const args = process.argv.slice(2);
10: const checkOnly = args.includes("--check");
11: const mavenGoals = args.filter((arg) => arg !== "--check");
ENTRY verify:client-routes TARGET tools/audit-client-api-routes.mjs ARGS ["--strict"]
5: const strict = process.argv.includes('--strict');
ENTRY verify:client-openapi TARGET tools/audit-client-openapi-coverage.mjs ARGS ["--strict"]
5: const strict = process.argv.includes('--strict');
ENTRY verify:client-payload TARGET tools/verify-client-payload-policy.mjs ARGS ["--strict"]
5: const strict = process.argv.includes('--strict');
ENTRY verify:operation-matrix TARGET tools/generate-openapi-operation-matrix.mjs ARGS ["--check"]
7: const checkOnly = process.argv.includes("--check");
ENTRY verify:openapi-closure TARGET tools/generate-openapi-contract-closure-appendix.mjs ARGS ["--check"]
11: const checkOnly = process.argv.includes("--check");
ENTRY verify:entry-doc-baseline TARGET tools/refresh-entry-doc-baseline.mjs ARGS ["--check"]
4: //       node tools/refresh-entry-doc-baseline.mjs --check —— 只校验漂移，漂移则退出码 1（可挂进可选链，
17: const checkOnly = process.argv.includes("--check");
100: // --check 只比数字不比日期：上面 A/C/D 三条替换会把 today 无条件戳进快照句，
ENTRY verify:security-baseline TARGET tools/verify-formal-security-baseline.mjs ARGS ["--strict"]
5: const strict = process.argv.includes("--strict");
ENTRY verify:android-env TARGET tools/run-android-gradle.mjs ARGS ["--check", "--require-android-sdk", "--project", "apps/android-terminal/android-app"]
43: const args = process.argv.slice(2);
44: const checkOnly = args.includes("--check");
45: const requireAndroidSdk = args.includes("--require-android-sdk");
51:   if (arg === "--check" || arg === "--require-android-sdk") continue;
52:   if (arg === "--project") {
ENTRY android:p0:build TARGET tools/run-android-gradle.mjs ARGS ["--project", "apps/android-terminal", "clean", "build"]
43: const args = process.argv.slice(2);
52:   if (arg === "--project") {
ENTRY android:p0:apk TARGET tools/run-android-gradle.mjs ARGS ["--project", "apps/android-terminal/android-app", "--require-android-sdk", ":app:assembleDebug"]
43: const args = process.argv.slice(2);
45: const requireAndroidSdk = args.includes("--require-android-sdk");
51:   if (arg === "--check" || arg === "--require-android-sdk") continue;
52:   if (arg === "--project") {
ENTRY verify:production-blockers TARGET tools/generate-production-blocker-register.mjs ARGS ["--check"]
6: const checkMode = process.argv.includes("--check");
ENTRY verify:production-owner-actions TARGET tools/generate-production-owner-action-packs.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-submission-kit TARGET tools/generate-production-evidence-submission-kit.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-submission-status TARGET tools/generate-production-evidence-submission-status.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-manifest-templates TARGET tools/generate-production-evidence-manifest-templates.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-owner-guides TARGET tools/generate-production-evidence-owner-guides.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-backfill-priority TARGET tools/generate-production-evidence-backfill-priority.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-backfill-execution-ledger TARGET tools/generate-production-evidence-backfill-execution-ledger.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:domain-ownership-scope TARGET tools/generate-domain-ownership-scope-audit.mjs ARGS ["--check"]
7: const check = process.argv.includes("--check");
ENTRY verify:production-closure-gap-reports TARGET tools/generate-production-closure-gap-report.mjs ARGS ["--check"]
6: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:nas-current-state-drift TARGET tools/generate-nas-current-state-drift-report.mjs ARGS ["--check"]
7: const checkMode = process.argv.includes("--check");
ENTRY verify:closed-loop-current-state TARGET tools/generate-closed-loop-current-state.mjs ARGS ["--verify"]
7: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify") || process.env.CLOSED_LOOP_CURRENT_STATE_CHECK === "1";
ENTRY verify:external-evidence-requirement-traceability TARGET tools/generate-external-evidence-requirement-traceability.mjs ARGS ["--check"]
6: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:nas-diagnostics TARGET tools/generate-nas-diagnostic-report.mjs ARGS ["--check"]
8: const checkMode = process.argv.includes("--check");
ENTRY verify:production-evidence-p0-quickstart TARGET tools/generate-production-evidence-p0-quickstart.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check");
ENTRY verify:runtime-acceptance-evidence TARGET tools/generate-runtime-acceptance-evidence.mjs ARGS ["--check"]
9: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
339: > 校验规则（\`--check\`）：若当前源码指纹与本文记录不一致（源码已变更），或本证据超过最大有效天数，则立即 FAIL；陈旧证据不再自欺 PASS。
ENTRY verify:ui-experience-acceptance TARGET tools/generate-ui-experience-acceptance-report.mjs ARGS ["--check"]
6: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:production-evidence-owner-execution-packets TARGET tools/generate-production-evidence-owner-execution-packets.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:production-evidence-draft-prep TARGET tools/prepare-production-evidence-manifest-drafts.mjs ARGS ["--check"]
9: const args = process.argv.slice(2);
11: const checkMode = args.includes("--check") || args.includes("--verify");
ENTRY verify:partial-requirement-mock-coverage TARGET tools/generate-partial-requirement-mock-coverage.mjs ARGS ["--check"]
5: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:production-no-go-command-center TARGET tools/generate-production-no-go-resolution-command-center.mjs ARGS ["--check"]
5: const args = process.argv.slice(2);
6: const checkMode = args.includes("--check") || args.includes("--verify");
ENTRY verify:integration-capability-switches TARGET tools/generate-integration-capability-switches.mjs ARGS ["--check"]
6: const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
ENTRY verify:data-isolation-spec TARGET tools/generate-data-isolation-spec.mjs ARGS ["--check"]
10: const check = process.argv.includes("--check");
ARGUMENT_ENTRY_VARIANTS 36
```


## B5-r1接收与合取回执
版本：V0.03

15:39:23 UTC取得Grok真实EXIT_CODE=0，modules/M005-b5-r1-grok-raw.md 39052字节/416行/SHA cf0fdfa8f179392f93161e64afed533cea92fd84da79a5d1eda478a4451652a3。全文416行完整读取；四文件232行、127脚本、9编译项、18GEN三线完整，审计PASS，无新GR/待补项/未答。报告明确未读本方raw或历史findings，未改源或corepack激活。本方独立完整报告及读取外席前补核共60471字节/SHA2dab6ab2974fa7dba20ee0e9246a3dfedd6376f69e4ca34dfdac5312cba21ec7保持；本节为收后核实，不是新独立票。

三点证据边界保留说明，不改写外席原文，也不制造修复项：
1. Grok的documentVersion/dev:api“可删”是明确不进队列的观察；documentVersion承担操作员文件版本铁律且B2已定稿，dev/dev:api/smoke:p0的保留用途在backend/README.md:66有原文。本次不采纳额外删除建议，Grok原生PASS和无finding结论保持。
2. Grok Linux错大小写导入的TS2307只证明当时文件解析失败，不能单凭该例断言forceConsistentCasingInFileNames开关的独立作用；九项继承有原生showConfig，本方严格/未知索引正负例另有2322/7006。未以TS2307扩称所有选项开关逐一做了A/B验证。
3. 两席都只对UI测试收集/配置验证，未跑50条业务断言；未跨Node二进制构建；npm当前11.19.0与声明10.9.8差异如实保留，不把它说成残留工程缺陷或有条件通过。环境展开也不作任意操作员VITE_变量安全性的全称保证。

B5后四值实算、sidecar、ledger、当前Grok最后独占哈希均为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。源四改动文件SHA与B3 after一致；tsconfig base/三HTML保持；35scratch保持；工作区tracked变化仅本模块四source和ledger/findings。控制本机SHA仍c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。

两席B5合取PASS，12项队列全否、B3=1、运行失败0。按顺序先ledger PASS再模块收口，随后按操作员已有逐模块发布授权提交/PR/CI/merge/ff-sync。发布结果另以真实回执为准，不在此预写完成。
