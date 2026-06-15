# P2 平面图 Web 管理入口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 把已完成的 P1/P2 平面图后端能力接入 Web 管理端，让实施/运营能创建平面图、导入 SVG/标准 JSON、查看候选/错误并人工发布。

**Architecture:** 不新增后端业务规则，Web 端只调用既有 `/api/v1/floor-plans` 接口并展示后端返回。OpenAPI 作为前端正式接口闸口同步补齐，避免前端绕过契约扫描。

**Tech Stack:** React 19、Ant Design 5、TypeScript、Spring Boot 4.x 后端既有接口、OpenAPI JSON/YAML、Node 验收脚本。

---

## File Structure

- Modify: `apps/web/src/main.tsx`
  - 新增 `floorplan` 管理入口、Snapshot 类型、加载逻辑、平面图导入表单、候选/错误查看、人工发布动作。
- Modify: `docs/openapi/openapi-p0.json`
  - 补齐 Web 会引用的平面图与导入接口路径、请求体 schema、操作元数据。
- Modify: `docs/openapi/openapi-p0.yaml`
  - 与 JSON 保持同源语义，供人工阅读。
- Modify: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`
  - 随 OpenAPI 新增操作重新生成验收矩阵。
- Modify: `docs/p0-openapi-contract-closure-appendix-v0.1.md`
  - 随 OpenAPI 新增操作重新生成契约闭环附录。
- Modify: `security-scans/formal-security-baseline-v1.md`
  - 随 OpenAPI 操作数重新生成正式安全基线。
- Modify: `security-scans/findings-register-v1.md`
  - 随 OpenAPI 操作数重新生成发现登记表。
- Modify: `package.json`
  - 新增 `verify:p2-floor-plan-web-entry` 验收命令。
- Create: `tools/verify-p2-floor-plan-web-entry.mjs`
  - 静态验收 Web 入口、导航、API 引用、OpenAPI 覆盖和发布确认边界。
- Create: `docs/p2-floor-plan-web-entry-completion-audit-v0.1.md`
  - 完成后记录边界、验收证据和剩余风险。

## Data / Interface Changes

- 前端读取：
  - `GET /api/v1/floor-plans`
  - `GET /api/v1/floor-plans/{id}/shapes`
  - `GET /api/v1/floor-plans/{id}/imports`
  - `GET /api/v1/floor-plans/{id}/imports/{importId}/candidates`
- 前端写入：
  - `POST /api/v1/floor-plans`
  - `POST /api/v1/floor-plans/{id}/imports`
  - `POST /api/v1/floor-plans/{id}/imports/{importId}/publish`
- 所有写入继续由现有 `normalizePostBody` 注入 `idempotency_key`。
- 浏览器不保存或扩展 `source_payload`，只提交给后端导入任务；展示以任务摘要、候选和错误为主。

## Tasks

### Task 1: Red Gate

- [x] 新增 `tools/verify-p2-floor-plan-web-entry.mjs`。
- [x] 在 `package.json` 加 `verify:p2-floor-plan-web-entry`。
- [x] 运行 `npm run verify:p2-floor-plan-web-entry`，确认初始失败并指出缺失 Web 入口。

### Task 2: Web Entry

- [x] 在 `apps/web/src/main.tsx` 新增 `floorplan` Section。
- [x] 新增平面图、shape、导入任务、候选、错误类型。
- [x] 登录后加载首个平面图的 shapes/imports/import details。
- [x] 新增创建平面图表单。
- [x] 新增 SVG/标准 JSON 导入表单。
- [x] 新增候选/错误表格和人工发布动作。
- [x] 发布动作必须经过 `confirmHighImpactAction`。

### Task 3: OpenAPI Contract

- [x] 在 `docs/openapi/openapi-p0.json` 补齐 floor-plan 路径。
- [x] 在 `docs/openapi/openapi-p0.json` 补齐请求体 schema。
- [x] 同步 `docs/openapi/openapi-p0.yaml` 可读契约。
- [x] 重新生成操作矩阵、契约闭环附录和安全基线派生文档。

### Task 4: Verification And Review

- [x] 运行 `npm run verify:p2-floor-plan-web-entry`。
- [x] 运行 `npm run verify:p0-contract`。
- [x] 运行 `npm run build`。
- [x] 运行 `npm run test:backend`。
- [x] 运行 `npm run verify:security-baseline`。
- [x] 运行 Semgrep OWASP + secrets。
- [x] 运行 `git diff --check`。
- [x] 写完成审计文档。

## Acceptance Criteria

- Web 左侧导航、模块地图和 URL 查询均支持 `floorplan`。
- 页面能创建平面图、查看平面图、shape、导入任务、候选和错误。
- 页面能发起 SVG/标准 JSON 导入，导入只生成候选，不直接发布正式 shape。
- 发布候选必须人工确认，并调用后端发布接口。
- 前端引用的所有 `/api/v1/floor-plans*` 路径都在 OpenAPI 中声明。
- 全量验证命令在 160 机器通过。

## Risks And Rollback

- 风险：`apps/web/src/main.tsx` 单文件较大，误伤现有 P0/P1/P2 入口。回滚：本切片独立提交，出错只回滚该提交。
- 风险：OpenAPI JSON/YAML 不一致。处理：已运行 `verify:p0-contract` 和专项验收脚本。
- 风险：误发布错误候选影响正式 shape。处理：Web 显示候选状态，发布必须确认，后端仍做状态和权限校验。