# P2 Baseline Protection And Web Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 先把 160 上当前 P2 后端和文档差异固化成可回滚保护基线，再补 P2 Web 管理入口 MVP。

**Architecture:** 候选 A 只做审计、验证、入口文档和保护性提交；候选 B 只在 Web 管理端接入已存在的 P2-B/C/D 后端能力，不扩写第三方真实接入、Python AI 服务或 P3/T3 功能。Web 端调用正式 `/api/v1/*` 路径，所有业务裁决以后端权限、幂等和审计事件为准。

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, Maven, React, TypeScript, Vite, Ant Design, Semgrep.

---

## Scope

### In Scope

- 候选 A：当前 P2 差异审计、`00-current-effective-document-entry-v0.1.md` P2 入口补齐、Semgrep/测试/构建验证、保护性提交。
- 候选 B：后台 Web P2 管理入口 MVP，覆盖游乐团体/包场、票务动态定价、经营指标快照的查看和必要受控操作。

### Out Of Scope

- 第三方供应商真实 SDK/API 适配。
- 独立 Python AI 服务实现。
- 动态定价真实影响订单实收。
- P3/T3 预测分析、集团视图、开放平台 SDK 深化。
- 无关前端重构、拆包优化和旧 TypeScript API 物理清理。

---

## File Map

### 候选 A

- Modify: `docs/00-current-effective-document-entry-v0.1.md` - 补齐 P2-B/C/D/E/F 与 P2 全量审计入口。
- Create: `docs/p2-current-diff-adversarial-review-v0.1.md` - 记录当前差异、风险、验证证据和保护性基线建议。
- Create: `docs/superpowers/plans/2026-06-15-p2-baseline-protection-and-web-entry.md` - 本执行计划。
- Output: `security-scans/latest-semgrep-output.json` - Semgrep JSON 证据。

### 候选 B

- Modify: `apps/web/src/main.tsx` - 按现有单文件 Web 架构补 P2 管理入口、类型、数据读取、操作表单和页面区域。
- Modify if required: `apps/web/src/api/*` or existing API helper file - 若项目已有集中 API helper，则在现有位置添加 P2 API 封装；若没有，不新增横切抽象。
- Test if existing: `apps/web` typecheck/build - 用 TypeScript 和 Vite 构建作为前端验收证据。
- Backend tests only if backend contract changes: `npm run test:backend`。

---

## Task 1: Candidate A Baseline Protection

- [x] **Step 1: Inspect current 160 diff**

Run: `git status --short && git diff --stat`

Expected: P2-B/C/D backend, migration, tests and docs are visible as current diff.

- [x] **Step 2: Run whitespace/diff check**

Run: `git diff --check`

Expected: exit 0, no whitespace errors.

- [x] **Step 3: Run backend regression tests**

Run: `npm run test:backend`

Expected: Maven test suite passes with 0 failures and 0 errors.

- [x] **Step 4: Run P0 contract gate**

Run: `npm run verify:p0-contract`

Expected: OpenAPI contract, examples, operation matrix, closure appendix, client routes, client OpenAPI coverage and payload policy all pass.

- [x] **Step 5: Run security scan**

Run: `~/.local/bin/semgrep scan --config p/owasp-top-ten --config p/secrets --metrics=off --json --output security-scans/latest-semgrep-output.json backend apps/web packages tools`

Expected: scan completes with 0 findings.

- [x] **Step 6: Run Web baseline checks**

Run: `npm run typecheck && npm run build`

Expected: TypeScript and Vite build pass. Large chunk warning is non-blocking for this baseline.

- [x] **Step 7: Update current document entry**

Update `docs/00-current-effective-document-entry-v0.1.md` to include P2-B/C/D/E/F and this audit report.

- [x] **Step 8: Write adversarial review report**

Create `docs/p2-current-diff-adversarial-review-v0.1.md` with risk findings and fresh verification evidence.

- [x] **Step 9: Commit protected baseline**

Run: `git add backend docs security-scans/latest-semgrep-output.json && git commit -m "feat: complete p2 backend baseline"`

Expected: one protected commit on 160 with P2 backend, migrations, tests, docs and security evidence.

---

## Task 2: Candidate B P2 Web Management Entry MVP

- [ ] **Step 1: Characterize current Web structure before editing**

Read `apps/web/src/main.tsx` and existing API helper patterns. Identify where ticketing, amusement and analytics pages currently render.

- [ ] **Step 2: Write or strengthen failing frontend checks when practical**

If the Web app already has component tests, add tests for P2-B/C/D route rendering and API field mapping first. If no frontend test harness exists, use TypeScript compile checks and Vite build as the minimum verification gate, and do not add a new test framework in this task.

- [ ] **Step 3: Implement P2-B Web entry**

Add a management section for amusement group/private bookings: list, create request, approve, release. The UI must show project/session, booking type, party size, reserved capacity, status, evidence ids and operator notes. Approval/release must call backend APIs and rely on backend capacity outcome.

- [ ] **Step 4: Implement P2-C Web entry**

Add a management section for ticket dynamic pricing rules: list, create draft, publish, simulate. The UI must clearly label simulation as non-production pricing and must not alter ticket order price。

- [ ] **Step 5: Implement P2-D Web entry**

Add a read-only section for business metric snapshots. The UI must label the data as analytics snapshot, not financial settlement ledger.

- [ ] **Step 6: Verify Candidate B**

Run: `npm run typecheck && npm run build && npm run verify:p0-contract`

Expected: all pass.

- [ ] **Step 7: Security and differential review**

Run Semgrep again and review the Web diff for XSS, overbroad data exposure, unauthorized operations, stale API paths and accidental dependency on legacy `apps/api`.

---

## Acceptance Criteria

- 候选 A 有保护性提交，且测试、契约、安全扫描、前端基线构建均有新鲜证据。
- 当前有效文档入口能直接找到 P2-B/C/D/E/F、P2 全量完成审计和当前差异审计。
- 候选 B 完成后，运营角色不需要直接调用接口即可查看和操作 P2-B/C，并能查看 P2-D。
- Web 不使用旧 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*` 或 `apps/api` 历史后端。
- P2-C 动态定价仍只做模拟，不影响正式订单实收。

## Risks And Rollback

- 风险：候选 B 修改 `apps/web/src/main.tsx` 这个大文件，容易引入无关变化。回滚方式：候选 A 保护提交后，候选 B 独立提交；如 Web 出错，只回滚候选 B 提交。
- 风险：P2-B 容量锁显示被前端误判。回滚方式：前端只展示后端返回结果，不缓存“可锁定”裁决。
- 风险：P2-C 被误解为生产定价。回滚方式：UI 文案和接口命名保留“simulation/模拟”，后端订单定价逻辑不接入动态规则。
- 风险：Semgrep PATH 在非登录 SSH 中不可见。回滚方式：使用 `~/.local/bin/semgrep` 完整路径执行。
