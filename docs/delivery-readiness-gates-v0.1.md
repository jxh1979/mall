# 交付总闸口与上线准入验证 V0.12

## 1. 定位

本文档把当前已经形成的分散验证命令收口为一个交付总闸口，用于在进入交付、外部评审、生产前置实施或供应商联调前做统一检查。

它不是新业务范围，也不是完整回归测试替代品；它只回答一个问题：当前仓库是否满足“只能在 160 正式编码、外部依赖不得伪实现、P0 契约仍闭合、安全基线仍可生成”的最低交付准入。

机器识别标记：

- DELIVERY_READINESS_GATES_READY
- VERIFY_DELIVERY_GATES_READY
- NO_DELIVERY_WITHOUT_EXTERNAL_DEPENDENCY_GATES
- NO_FORMAL_CODE_OUTSIDE_160
- NO_PRODUCTION_DEPLOYMENT_WITH_PENDING_EXTERNAL_BLOCKERS
- OWNER_ACTION_PACKS_FROM_PRODUCTION_BLOCKERS
- OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS
- PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY
- PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY
- PRODUCTION_EVIDENCE_OWNER_GUIDES_READY
- PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY
- PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY
- PRODUCTION_EVIDENCE_NEGATIVE_FIXTURES_READY
- PRODUCTION_EVIDENCE_POSITIVE_FIXTURES_READY
- PRODUCTION_EVIDENCE_POSITIVE_PROPAGATION_FIXTURES_READY
- PRODUCTION_READY_NO_GO_FIXTURES_READY
- PRODUCTION_BLOCKER_REGISTER_NO_GO_FIXTURES_READY
- PRODUCTION_READY_POSITIVE_FIXTURES_READY
- PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY
- PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_READY
- PRODUCTION_EVIDENCE_P0_QUICKSTART_READY
- PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_READY
- PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY
- PRODUCTION_EVIDENCE_CROSSLINKS_READY
- OWNER_PACKET_ONE_STOP_BACKFILL_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_READY
- PRODUCTION_EVIDENCE_DRAFT_PREP_FIXTURES_READY
- PRODUCTION_CLOSURE_GAP_REPORTS_READY
- PARTIAL_REQUIREMENTS_HAVE_OWNER_MANIFESTS
- EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_READY
- NO_FAKE_PRODUCTION_MANIFEST_CLOSURE
- FINAL_CLOSED_LOOP_AUDIT_READY
- NAS_IT_CONTROLLED_MOUNT_HANDOFF_READY
- VERIFY_NAS_IT_HANDOFF_READY
- NAS_DIAGNOSTIC_REPORT_READY
- VERIFY_NAS_DIAGNOSTICS_READY
- NAS_CURRENT_STATE_DRIFT_REPORT_READY
- VERIFY_NAS_CURRENT_STATE_DRIFT_READY
- HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE
- RUNTIME_ACCEPTANCE_EVIDENCE_READY
- VERIFY_RUNTIME_ACCEPTANCE_EVIDENCE_READY
- UI_EXPERIENCE_ACCEPTANCE_REPORT_READY
- VERIFY_UI_EXPERIENCE_ACCEPTANCE_READY

## 2. 必跑命令

交付前先运行：

```bash
npm run verify:delivery-gates
```

该命令应保持轻量，不直接包含后端完整测试、Web 构建、Android 构建或生产发布动作。主要闸口如下，完整清单与执行顺序以 `package.json` 的 `verify:delivery-gates` 脚本为准：

| 命令 | 目的 |
|---|---|
| `npm run verify:delivery-gates-docs` | 校验本文档、入口文档、交付包清单和脚本是否互相引用 |
| `npm run verify:coding-host-protection` | 确认正式代码只在 192.168.88.160 的标准仓库执行 |
| `npm run verify:external-dependency-gates` | 阻断未确定供应商前的真实机器人、视频、设备控制和独立 Python AI 服务伪实现 |
| `npm run verify:t3-coding-package` | 校验 T3 编码包、供应商闸口和只读/后置边界仍闭合 |
| `npm run verify:t3b-supplier-mock-kit` | 校验供应商 mock kit 仍不包含真实密钥、生产控制或越权闭环 |
| `npm run verify:t3d-local-llm-ai-quality` | 校验 160 本地大模型质检仍为 loopback-only、脱敏、人工复核建议 |
| `npm run verify:p0-contract` | 校验 P0 OpenAPI、示例、operation 矩阵、闭环附录和客户端契约 |
| `npm run verify:security-baseline` | 校验正式安全基线输入和生产文档访问策略 |
| `npm run verify:production-readiness-evidence` | 校验生产前置证据、台账状态和安全基线 evidence 是否一致 |
| `npm run verify:production-blockers` | 校验生产阻断清单与台账、findings register 同步 |
| `npm run verify:production-owner-actions` | 校验生产阻断项已按责任人拆成可执行动作包，且动作包由阻断清单自动生成并关联证据 manifest 路径 |
| `npm run verify:production-evidence-submission-kit` | 校验证据提交包由责任人动作包自动生成，且仍保持等待真实证据状态 |
| `npm run verify:production-evidence-manifest-templates` | 校验 manifest 填写模板齐全，且默认 blocked、仅为模板不是真实证据 |
| `npm run verify:production-evidence-owner-guides` | 校验责任组回填说明包齐全，且只做分派说明不关闭阻断 |
| `npm run verify:production-evidence-submission-status` | 扫描实际 manifest 提交状态，缺失为 awaiting，格式错误或明文密钥为阻断 |
| `npm run verify:production-evidence-submissions-guard` | 校验证据提交目录只允许 manifest 入库，原始证据必须外部留链 |
| `npm run verify:production-evidence-negative-fixtures` | 使用临时负向 manifest 校验证据状态看板会拦截同人提交复核、本地路径、非生产 credential_ref、错绑 item、敏感 URL、明文密钥和非 passed 结果 |
| `npm run verify:production-evidence-positive-fixtures` | 使用临时目录正向 manifest 证明合规生产证据提交会被识别为 valid_submission，且不污染真实 submissions |
| `npm run verify:production-evidence-positive-propagation-fixtures` | 使用临时目录验证合规 manifest 能从提交状态传播到生产闭环缺口报告和闭环当前态，同时不误宣称生产上线就绪 |
| `npm run verify:production-ready-no-go-fixtures` | 使用临时目录验证 当前全部生产 manifest 全有效且 NAS 模拟 ready 时，外部证据 partial 需求仍必须阻断生产上线 ready 声明 |
| `npm run verify:production-blocker-register-no-go-fixtures` | 使用临时目录验证当生产 manifest、NAS 和外部证据都模拟通过时，生产 blocker register 未清零仍必须阻断生产上线 ready 声明 |
| `npm run verify:production-ready-positive-fixtures` | 使用临时目录验证当 当前全部生产 manifest、NAS、外部证据和 blocker register 全部满足时，闭环当前态必须能判定生产 ready=true |
| `npm run verify:production-evidence-backfill-priority` | 校验证据回填优先级看板齐全，按责任组和 P0 缺口排序且不关闭阻断 |
| `npm run verify:production-evidence-backfill-execution-ledger` | 校验证据回填执行台账齐全，一行一个待补 manifest 且不关闭阻断 |
| `npm run verify:production-evidence-p0-quickstart` | 校验 P0-only 生产证据快速回填快照齐全，当前 P0 manifest 已按责任组和第一动作可派发 |
| `npm run verify:production-evidence-owner-execution-packets`、`npm run verify:production-evidence-crosslinks` | 校验按责任组生成的一站式生产证据执行包齐全、证据交叉引用一致，模板复制命令不覆盖已提交 manifest，且不包含明文密钥或原始证据 |
| `npm run verify:production-evidence-draft-prep` | 校验生产证据 manifest 草稿准备器默认 dry-run、拒绝越界写入和覆盖已有 manifest，且不会关闭生产阻断 |
| `npm run verify:production-evidence-draft-prep-fixtures` | 在临时目录实测草稿准备器 dry-run 不写文件、execute 复制模板、再次 execute 不覆盖已有 manifest，并拒绝无选择器的 broad execute |
| `npm run verify:production-closure-gap-reports` | 从当前运行态、Semgrep、NAS 当前状态、生产证据提交状态和外部证据追踪校验生产闭环缺口总报告，防止旧报告或手改报告伪关闭生产证据 |
| `npm run verify:external-evidence-requirement-traceability` | 校验 8 条外部证据型 partial 需求已追踪到责任组、证据 item_id、模板和目标 manifest 路径，或已记录对应接入开关关闭，防止 mock/沙箱被误判为生产闭环 |
| `npm run verify:nas-it-handoff` | 校验 NAS IT 受控挂载交接包、默认 dry-run 脚本、manifest 映射和无明文密钥边界 |
| `npm run verify:nas-diagnostics` | 校验 NAS 只读诊断报告与当前网络、445 端口、CIFS 挂载和 preflight 根因一致，不关闭生产 manifest |
| `npm run verify:nas-current-state-drift` | 校验 NAS 历史通过证据没有被误当作当前生产自动挂载、定时备份、告警和恢复演练闭环 |
| `npm run verify:final-closed-loop-audit` | 校验最终闭环验收报告与需求矩阵、当前代码行数、Semgrep 摘要和生产证据状态一致，防止最终报告漂移 |
| `npm run verify:closed-loop-current-state` | 校验闭环当前态快照已纳入需求矩阵、安全扫描、生产证据、NAS 和运行时验收证据 |
| `npm run verify:runtime-acceptance-evidence` | 校验运行时验收证据快照齐全，覆盖后端测试、Web 构建、UI、真实后端 UI 和生产边界声明；并将证据绑定到生成时的源码指纹与生成时效——源码已变更或证据超期（默认 30 天，`RUNTIME_EVIDENCE_MAX_AGE_DAYS` 可调）即判为陈旧失败，杜绝陈旧证据自欺通过 |
| `npm run verify:ui-experience-acceptance` | 校验前端体验验收报告由运行态证据、Playwright spec 和截图基线生成，防止旧报告漂移 |
| `npm run verify:production-evidence-intake` | 校验生产前置证据回填模板齐全且不包含明文密钥 |

## 3. 交付阻断规则

1. 未通过 `verify:coding-host-protection`：不允许宣称当前代码来自正式编码仓库。
2. 未通过 `verify:external-dependency-gates`：不允许进入供应商联调、生产前置签收或对外交付。
3. 生产前置台账仍存在 `pending_info` 的外部能力，例如 MDM、支付、企微、人脸、供应商 API、第三方密钥时，不允许宣称生产闭环完成。
4. 未拿到真实供应商 SDK/API、签名、回调、字段、SLA 和测试账号前，只允许保留 OpenAPI 边界、mock adapter、台账和 Runbook，不允许做真实控制闭环。
5. 独立 Python AI 服务在 T3-D2 前继续阻断；当前允许的是 160 本地大模型只读建议能力，不允许 AI 自动处罚、自动退款、自动调价、自动核销或自动影响绩效。

## 4. 与完整验证的关系

`npm run verify:delivery-gates` 是交付准入，不是完整上线验收。正式上线还必须按业务系统地图“系统交付与上线”完成强密码更换、U 盘密钥托管及相关留痕，并交付能自动识别环境的安装包和业务语言教程；以下工程验证不能替代这些要求。按场景补跑：

| 场景 | 追加验证 |
|---|---|
| 后端代码变更 | `npm run test:backend` |
| 闭环运行态证据变更 | `npm run verify:closed-loop-runtime`，再运行 `npm run docs:runtime-acceptance-evidence`、`npm run docs:ui-experience-acceptance`、`npm run verify:runtime-acceptance-evidence` 和 `npm run verify:ui-experience-acceptance` |
| Web 管理端变更 | `npm run build` 和对应功能校验 |
| Android/一体机变更 | `npm run verify:android-env`、`npm run android:p0:build`、`npm run android:p0:apk` |
| OpenAPI 变更 | 先跑 `npm run docs:openapi-closure`，再跑 `npm run verify:p0-contract` |
| 生产运维前置 | `npm run verify:ops160-recovery`、`npm run verify:nas-it-handoff`、`npm run verify:nas-diagnostics`、`npm run verify:nas-current-state-drift`，并回填真实证据 |
| 安全发布前 | Semgrep、差异审计、密钥扫描、人工 code review |

## 5. 失败处理

失败时不要跳过闸口，也不要改脚本掩盖问题。处理顺序如下：

1. 先看失败命令对应的文档或工具提示。
2. 如果是 OpenAPI 附录漂移，先生成闭环附录，再复跑契约闸口。
3. 如果是外部依赖阻断，回到供应商资料、生产前置台账和外部依赖闸口，不写假实现。
4. 如果是 160 主机保护失败，先确认是否在正式仓库 `/home/james/complex-management` 和主机 `192.168.88.160` 上运行。
5. 如果是责任人动作包、证据提交包、manifest 模板、责任组说明包、提交状态看板、提交目录护栏、负向样例校验、回填优先级看板、回填执行台账、生产闭环缺口报告、外部证据需求追踪报告或已有证据差距审计漂移，先运行 `npm run docs:production-blockers`，再运行 `npm run docs:production-owner-actions`、`npm run docs:production-evidence-submission-kit`、`npm run docs:production-evidence-manifest-templates`、`npm run docs:production-evidence-owner-guides`、`npm run docs:production-evidence-submission-status`、`npm run docs:production-evidence-backfill-priority`、`npm run docs:production-evidence-backfill-execution-ledger`、`npm run docs:production-evidence-p0-quickstart`、`npm run docs:production-evidence-owner-execution-packets`、`npm run prepare:production-evidence-drafts -- --owner <owner_slug> --p0`、`npm run docs:external-evidence-requirement-traceability` 和 `npm run docs:production-closure-gap-reports`，并复跑 `npm run verify:production-evidence-submissions-guard`、`npm run verify:production-evidence-negative-fixtures`、`npm run verify:production-evidence-positive-fixtures`、`npm run verify:production-evidence-p0-quickstart`、`npm run verify:production-evidence-owner-execution-packets`、`npm run verify:production-evidence-draft-prep`、`npm run verify:production-evidence-draft-prep-fixtures`、`npm run verify:production-closure-gap-reports`、`npm run verify:external-evidence-requirement-traceability` 与 `npm run verify:final-closed-loop-audit`。
6. 如果是 NAS IT 受控挂载交接包、NAS 诊断或 NAS 当前状态漂移失败，先检查 `docs/nas-it-controlled-mount-handoff-v0.1.md`、`ops/160/complex-nas-system-mount.example.sh`、`docs/nas-diagnostic-report-v0.1.md`、`docs/nas-current-state-drift-report-v0.1.md`、`npm run verify:nas-it-handoff`、`npm run verify:nas-diagnostics` 和 `npm run verify:nas-current-state-drift`，不得通过仓库脚本写入 SMB 明文密钥、伪造生产挂载通过，或把历史 passed evidence 当作当前生产闭环。
7. 如果是运行时验收证据或前端体验验收报告漂移，先运行 `npm run verify:closed-loop-runtime`，再运行 `npm run docs:runtime-acceptance-evidence`、`npm run docs:ui-experience-acceptance`、`npm run verify:runtime-acceptance-evidence`、`npm run verify:ui-experience-acceptance` 和 `npm run verify:closed-loop-current-state`。
8. 修复后必须重新运行 `npm run verify:delivery-gates` 并保留输出证据。

## Partial 需求 Mock/沙箱验收覆盖报告 V0.1

Markers: PARTIAL_REQUIREMENT_MOCK_COVERAGE_READY / PARTIAL_REQUIREMENTS_HAVE_MOCK_ACCEPTANCE_PATHS.

- Generate: `npm run docs:partial-requirement-mock-coverage`
- Verify: `npm run verify:partial-requirement-mock-coverage`
- Report: `docs/partial-requirement-mock-coverage-v0.1.md`
- Snapshot: `docs/partial-requirement-mock-coverage-latest.json`
- Boundary: 本文由 `tools/generate-partial-requirement-mock-coverage.mjs` 从 `docs/requirements-code-implementation-matrix-v0.1.csv` 和 `docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json` 生成，用于证明 8 条 partial 需求虽然不能宣称生产闭环，但已有 mock、adapter、页面、后端测试或 E2E 验收替身，可以支撑联调和业务验收。
- Production boundary: 本文不关闭任何生产 blocker；真实支付/POS、人脸、视频、机器人、设备、对象存储、开放平台凭证和 MDM/NAS 证据仍必须按 production evidence manifest 回填。

## 附录：生产 No-Go 关闭指挥板

- `npm run docs:production-no-go-command-center`
- `npm run verify:production-no-go-command-center`
- `PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY`
- `NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE`
- `NAS_REQUIRES_IT_SUDO_MAINTENANCE_WINDOW`
