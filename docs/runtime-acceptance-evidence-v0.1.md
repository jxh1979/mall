# 运行时验收证据快照 V0.1

> 本文由 `tools/generate-runtime-acceptance-evidence.mjs` 生成，用于把后端测试、Web 构建、Playwright UI、真实后端 UI、Semgrep 和生产边界汇总成可追溯证据。它不替代 `npm run verify:closed-loop-runtime`，也不关闭任何生产 manifest。

机器识别标记：

- RUNTIME_ACCEPTANCE_EVIDENCE_READY
- RUNTIME_EVIDENCE_FROM_SUREFIRE_REPORTS
- RUNTIME_EVIDENCE_FROM_PLAYWRIGHT_REPORTS
- RUNTIME_EVIDENCE_FROM_WEB_DIST
- RUNTIME_EVIDENCE_FROM_SEMGREP_LATEST_SUMMARY
- RUNTIME_EVIDENCE_BOUND_TO_SOURCE_FINGERPRINT
- RUNTIME_EVIDENCE_DOES_NOT_CLOSE_PRODUCTION_MANIFEST

## 0. 证据绑定（防陈旧）

| 绑定项 | 当前值 |
|---|---|
| 生成时的 Git HEAD | `898bcae5f949dca27d491102669c8290cdf8effb`（feature/business-code-batch56-20261106） |
| 生成时工作树 | 干净 |
| 源码指纹 sha256 | `f6b102db3f2be9f0f4d3e33e7e50979cf48cc224d40aa2e0af014e7ea08e1df3` |
| 指纹覆盖文件数 | 3406 |
| 生成时间 | 2026-11-12T11:36:09.173Z |
| 最大有效天数 | 30 |

> 校验规则（`--check`）：若当前源码指纹与本文记录不一致（源码已变更），或本证据超过最大有效天数，则立即 FAIL；陈旧证据不再自欺 PASS。

## 1. 结论

| 判断项 | 当前值 |
|---|---|
| 运行时证据支持进入联调/业务验收 | 否 |
| 可以宣称生产上线就绪 | 否 |
| 结论说明 | Runtime evidence is incomplete or stale; rerun the closed-loop runtime command and regenerate this evidence. |

刷新规则：After backend, web, Playwright, OpenAPI, or runtime script changes, rerun npm run verify:closed-loop-runtime, then npm run docs:runtime-acceptance-evidence.

## 2. 必须能复跑的命令

| 命令 | 当前脚本 |
|---|---|
| 闭环运行态总验收 | `npm run test:backend && npm run typecheck --workspace apps/web && npm run build --workspace apps/web && npm run verify:ui && npm run verify:ui:real-backend:seeded` |
| 后端测试 | `node tools/run-backend-maven.mjs test` |
| Web 构建 | `npm run build --workspaces --if-present && npm run verify:customer-shell-separation` |
| 常规 UI 验收 | `playwright test` |
| 真实后端 UI 验收 | `node tools/run-real-backend-ui-e2e.mjs` |
| 本证据校验 | `node tools/generate-runtime-acceptance-evidence.mjs --check` |

## 3. 后端测试证据

| 指标 | 当前值 |
|---|---:|
| Surefire suites | 587 |
| tests | 6721 |
| failures | 0 |
| errors | 0 |
| skipped | 0 |
| latest_mtime | 2026-11-12T11:34:18.377Z |

## 4. Web 与 UI 验收证据

| 证据 | 当前值 |
|---|---|
| Web dist index | 存在 |
| Web main JS | `apps/web/dist/assets/main-BdoO04-f.js` 4337 bytes |
| 最大静态资源 | `apps/web/dist/assets/vendor-antd-CzvkKKQu.js` 251112 bytes |
| 主包过大 warning 预期 | 无 |
| 前端单元测试源文件 | 332 个文件，1735 条测试定义 |
| Playwright 常规 UI | 报告存在，47 条测试定义 |
| Playwright 真实后端 UI | 报告缺失，3 条测试定义 |
| Playwright last-run | passed |

## 5. 安全扫描与生产边界

| 证据 | 当前值 |
|---|---|
| Semgrep | scan=semgrep-20260703-delivery-gates-clean，results=0，errors=0，paths_scanned=0 |
| 生产证据 | awaiting=15，P0 awaiting=13 |
| NAS 当前状态 | failed |
| 本报告是否关闭生产 manifest | 否 |

## 6. 机器可读证据

对应 JSON：`docs/runtime-acceptance-evidence-latest.json`。
