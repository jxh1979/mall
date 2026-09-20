CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M019 B4-r1 修复 diff 复审 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-20T09:56:10Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 对象：M019 B3-r1 整批修复 diff：before tree `ce3a76f543b56b781fe93887a824548e0569500c` → after tree `d14e6792b4f5f4f37de77ec61020f3099246fa3a`；冻结 diff `/home/james/audit-tmp/m019_b3_r1.diff`（13911 bytes，SHA256 3c37ef2df171937200fece80e55b6b2f2a66b1126232ddddc499c25941c1628a），全文见 §2。定稿 ID：M019-001…009（findings V2.60，队列「是」）。
- 顺序：Grok 已先派（modules/M019-b4-r1-grok-raw.md 存在=False），本报告落盘前未读其正文。

## 1. 逐块归因（§5 闸 3）

| 文件 | hunk | ±行数 | 归因 ID |
|---|---|---:|---|
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -1,7 +1,9 @@` | 4 | 001/002/003 |
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -42,7 +44,6 @@` | 1 | 001 |
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -52,7 +53,6 @@` | 1 | 001 |
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -60,16 +60,14 @@` | 4 | 001 |
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -90,8 +88,11 @@` | 5 | 002 |
| p1-floor-plan-polygon-api-contract-v0.1.md | `@@ -125,6 +126,6 @@` | 6 | 003 |
| p2-floor-plan-import-api-contract-v0.1.md | `@@ -1,4 +1,6 @@` | 4 | 004/005/006/007/008 |
| p2-floor-plan-import-api-contract-v0.1.md | `@@ -53,7 +55,7 @@` | 2 | 005 |
| p2-floor-plan-import-api-contract-v0.1.md | `@@ -66,8 +68,8 @@` | 4 | 005/006 |
| p2-floor-plan-import-api-contract-v0.1.md | `@@ -148,21 +150,22 @@` | 7 | 004/006/008 |
| p2-floor-plan-import-api-contract-v0.1.md | `@@ -208,4 +211,4 @@` | 2 | 007 |
| p2-floor-plan-import-completion-audit-v0.1.md | `@@ -1,4 +1,6 @@` | 4 | 004/006/007/009 |
| p2-floor-plan-import-completion-audit-v0.1.md | `@@ -10,17 +12,17 @@ P2-A 商场平面图 SVG/标准 JS` | 6 | 004/006/009 |
| p2-floor-plan-import-completion-audit-v0.1.md | `@@ -46,6 +48,8 @@ P2-A 商场平面图 SVG/标准 JSON` | 2 | 007 |
| 2026-06-15-p2-floor-plan-svg-json-import.md | `@@ -2,6 +2,8 @@` | 2 | 004 |
| 2026-06-15-p2-floor-plan-svg-json-import.md | `@@ -120,6 +122,8 @@` | 2 | 009 |
| 2026-06-15-p2-floor-plan-svg-json-import.md | `@@ -148,7 +152,7 @@` | 2 | 007 |
| 2026-06-15-p2-floor-plan-svg-json-import.md | `@@ -170,7 +174,7 @@` | 2 | 005 |
| 2026-06-15-p2-floor-plan-svg-json-import.md | `@@ -202,7 +206,7 @@` | 2 | 004 |

- 4 文档 19 个 hunk 全部落到 001–009（文首变更记录/版本号归各文件所含 ID）；未改源码/OpenAPI/迁移/三份 HTML/锁；归因票：**PASS**。

## 2. diff 全文

```diff
diff --git a/docs/p1-floor-plan-polygon-api-contract-v0.1.md b/docs/p1-floor-plan-polygon-api-contract-v0.1.md
index 9045ec95..b83cfa51 100644
--- a/docs/p1-floor-plan-polygon-api-contract-v0.1.md
+++ b/docs/p1-floor-plan-polygon-api-contract-v0.1.md
@@ -1,7 +1,9 @@
-# P1 商场平面图图片与多边形坐标 API 契约 V0.1
+# P1 商场平面图图片与多边形坐标 API 契约 V0.2
 
 文档类型：Implementation / API Contract
 
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：删去现行 DTO/OpenAPI 中不存在的 `evidence_attachment_ids`、`operator_user_id` 字段行，`z_index` 改为选填缺省 0（M019-001）；错误矩阵按现行实现改写——他图 `shape_id` 为 400 `VALIDATION_FAILED`、未知 `shape_id` 按该 id 新建，补 `PROJECT_NOT_FOUND`/`CROSS_PROJECT_FORBIDDEN`/`CROSS_MERCHANT_FORBIDDEN`（M019-002）；§9 引用改为现行迁移文件与真实标题（M019-003）。
+
 本文档固化 P1-FLOOR-PLAN-01「商场平面图先做图片 + 多边形坐标」的最小可编码契约。P0 空间档案中的轻量 `polygon_points` 保持兼容；本 P1 模块作为后续商户、游乐项目、设备、视频点位、机器人点位和 CAD/SVG 导入的标准地图数据源。
 
 ## 1. 范围
@@ -42,7 +44,6 @@
 |---|---|---|---|
 | `idempotency_key` | 是 | string | 同一创建请求幂等 |
 | `operator_note` | 否 | string | 审计说明，默认 `created` |
-| `evidence_attachment_ids` | 否 | string[] | 仅存附件 ID 引用 |
 | `project_id` | 是 | string | 所属综合体/项目 |
 | `building_code` | 否 | string | 楼栋编码 |
 | `floor_no` | 是 | string | 楼层，如 `B1`、`1F`、`3F` |
@@ -52,7 +53,6 @@
 | `image_height` | 是 | integer | 必须大于 0 |
 | `version_no` | 是 | string | 图片版本号 |
 | `status` | 是 | enum | `active`/`inactive`/`archived` |
-| `operator_user_id` | 是 | string | 操作人 |
 
 ### 4.2 保存多边形标注
 
@@ -60,16 +60,14 @@
 |---|---|---|---|
 | `idempotency_key` | 是 | string | 同一标注写入请求幂等 |
 | `operator_note` | 否 | string | 审计说明，默认 `shape_saved` |
-| `evidence_attachment_ids` | 否 | string[] | 仅存附件 ID 引用 |
 | `shape_id` | 否 | string | 为空时创建；不为空时更新该标注 |
 | `shape_type` | 是 | enum | `shop` 铺位 / `booth` 摊位 / `public_area` 公共区 / `area` 区域 / `entrance` 入口 / `facility` 设施 / `risk_zone` 风险区 / `route` 路径。给人看只用这八种中文，无走道 |
 | `ref_type` | 是 | enum | `space`/`merchant`/`project`/`amusement_project`/`device`/`custom` |
 | `ref_id` | 否 | string | 被引用对象 ID；`custom` 可为空 |
 | `label` | 是 | string | 标注名称 |
 | `polygon_points` | 是 | point[] | 至少 3 个点，每个点包含 `x`、`y` |
-| `z_index` | 是 | integer | 前端渲染层级 |
+| `z_index` | 否 | integer | 前端渲染层级，缺省 0 |
 | `status` | 是 | enum | `active`/`inactive`/`archived` |
-| `operator_user_id` | 是 | string | 操作人 |
 
 ## 5. 事件
 
@@ -90,8 +88,11 @@
 | 状态非法 | 非 `active`/`inactive`/`archived` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
 | 多边形点数不足 | 少于 3 个点 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
 | 坐标越界 | 任一点 `x`/`y` 不在 `[0, 1]` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
+| 项目不存在 | 创建时 `project_id` 查询不到 | 404 | `PROJECT_NOT_FOUND` | 拒绝创建 |
+| 越项目 | 项目员工读写其他综合体项目的平面图 | 403 | `CROSS_PROJECT_FORBIDDEN` | 拒绝 |
+| 越商户 | 商户账号读写其他商户的平面图 | 403 | `CROSS_MERCHANT_FORBIDDEN` | 拒绝 |
 | 平面图不存在 | `floor_plan_id` 查询不到 | 404 | `FLOOR_PLAN_NOT_FOUND` | 拒绝保存标注 |
-| 标注不存在 | 更新时 `shape_id` 查询不到 | 404 | `FLOOR_PLAN_SHAPE_NOT_FOUND` | 拒绝更新 |
+| 标注属于其他平面图 | 更新时 `shape_id` 属于其他平面图 | 400 | `VALIDATION_FAILED` | 拒绝更新（文案「图形不属于该平面图」）；查询不到的 `shape_id` 按该 id 新建标注，不报错 |
 | 幂等冲突 | 同 key 不同请求体 | 409 | `IDEMPOTENCY_CONFLICT` | 要求换 key 或查询原请求 |
 
 ## 7. Anti-Patterns
@@ -125,6 +126,6 @@
 | Topic | Location |
 |---|---|
 | 本切片计划 | `docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md` |
-| P1 总路线 | `docs/master-roadmap.md#阶段-8-市场基准增强为-p1p2-演进` |
-| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md#第二期-p1` |
-| 空间档案 P0 轻量坐标 | `backend/src/main/resources/db/migration/V001__initial_schema.sql` |
+| P1 总路线 | `docs/master-roadmap.md`（「阶段 8：市场基准增强与 SaaS 化深度能力」） |
+| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md`（§1.1 版本分层「第二期 / P1」行） |
+| 空间档案 P0 轻量坐标 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` |
diff --git a/docs/p2-floor-plan-import-api-contract-v0.1.md b/docs/p2-floor-plan-import-api-contract-v0.1.md
index 60f38bb6..00797fe6 100644
--- a/docs/p2-floor-plan-import-api-contract-v0.1.md
+++ b/docs/p2-floor-plan-import-api-contract-v0.1.md
@@ -1,4 +1,6 @@
-# P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.1
+# P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.2
+
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：发布前置改为 `parsed`/`published` 可继续发布剩余待处理候选（M019-004）；示例 id 改为派生形态并加说明（M019-005）；`source_format` 必填无默认、解析闭合 `<polyline>`、非八种整单拒绝（M019-006）；§6 验证口径以 verify:predeploy 为准（M019-007）；§3.4 补高危二次确认句（M019-008）。
 
 ## 1. 定位
 
@@ -53,7 +55,7 @@
 {
   "code": "OK",
   "data": {
-    "id": "fpi_floor-import-create-001",
+    "id": "fpi_0123456789abcdef0123456789abcdef",
     "floor_plan_id": "fp_1",
     "source_format": "svg",
     "source_name": "1F.svg",
@@ -66,8 +68,8 @@
 
 规则：
 
-- `idempotency_key` 必填。
-- `source_format` 给人看只有两种：矢量图（`svg`，默认）、标准数据（`standard_json`）。不接受其它格式。
+- `idempotency_key` 必填；任务 id 为 `fpi_` + 由平面图 id 与幂等键派生的 SHA-256 前 32 位十六进制（共 36 字符），不直接拼接幂等键；本文示例 id 均为示意。
+- `source_format` 必填，给人看只有两种：矢量图（`svg`）、标准数据（`standard_json`）。不接受其它格式，无默认值。
 - `source_payload` 最大 1,000,000 字符。
 - 导入只生成候选和错误，不直接改写正式 `floor_plan_shapes`。
 - 若没有任何合法候选，任务状态为 `failed`。
@@ -148,21 +150,22 @@
 
 规则：
 
-- 仅 `parsed` 状态的导入任务允许发布。
+- `parsed` 或 `published` 状态的导入任务允许继续发布剩余待处理候选；`failed`/`cancelled` 拒绝。
 - 仅 `pending` 候选允许发布。
 - 发布后生成正式 `floor_plan_shapes`，候选状态变为 `published`，任务状态变为 `published`。
 - 发布必须幂等，同一请求重复提交不得重复创建正式 shape。
+- 发布为高危动作，须二次确认（治理地图「空间和点位」）；服务端确认字段与 OpenAPI/前端同批落地（M015-011），本文尚未列入请求体。
 
 ## 4. 解析规则
 
 ### 4.1 SVG
 
-- 仅解析 `<polygon>`。
+- 解析 `<polygon>` 与首尾闭合的 `<polyline>`。
 - `<polyline>` 必须已闭合；未闭合时记录 `POLYLINE_NOT_CLOSED`，不生成候选。
 - 必须存在正向 `viewBox`，用于把点位归一化到 0 到 1。
 - 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。
 - `shape_type` 只允许八种：铺位 `shop`、摊位 `booth`、公共区 `public_area`、区域 `area`、入口 `entrance`、设施 `facility`、风险区 `risk_zone`、路径 `route`。无走道。
-- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的记入错误明细，不生成候选。
+- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的整单拒绝（400 `VALIDATION_FAILED`），不生成任务、候选与错误明细。
 - 候选 label 优先取 `data-label`，其次取 `id`。
 
 ### 4.2 标准 JSON
@@ -208,4 +211,4 @@
 - `FloorPlanControllerTests`
 - `FloorPlanJdbcIntegrationTests`
 
-后续完成前仍需运行全量后端测试、P0 契约校验和 Semgrep 聚焦扫描。
+全量后端测试、P0 契约校验与安全扫描以 `npm run verify:predeploy` 门禁为准（完成审计 §5 的数字为编码完成时快照）。
diff --git a/docs/p2-floor-plan-import-completion-audit-v0.1.md b/docs/p2-floor-plan-import-completion-audit-v0.1.md
index aca8448a..f7eadfbb 100644
--- a/docs/p2-floor-plan-import-completion-audit-v0.1.md
+++ b/docs/p2-floor-plan-import-completion-audit-v0.1.md
@@ -1,4 +1,6 @@
-# P2-A 平面图导入编码完成审计 V0.1
+# P2-A 平面图导入编码完成审计 V0.2
+
+> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：人工发布规则按现行实现改写（M019-004）；矢量图导入范围补闭合 `<polyline>`（M019-006）；§5 数字标为编码完成时快照（M019-007）；候选 `shape_type` 八值 CHECK 指向 V423（M019-009）。
 
 ## 1. 结论
 
@@ -10,17 +12,17 @@ P2-A 商场平面图 SVG/标准 JSON 导入能力已完成当前可编码范围
 
 | 范围 | 结论 |
 |---|---|
-| 矢量图导入 | 支持 `<polygon>`；未闭合 `<polyline>` 记录错误，不生成候选 |
+| 矢量图导入 | 支持 `<polygon>` 与首尾闭合的 `<polyline>`；未闭合 `<polyline>` 记录错误，不生成候选 |
 | 标准数据导入 | 支持 `shapes` 数组和 `polygon_points` |
 | 候选种类 | 只发布铺位、摊位、公共区、区域、入口、设施、风险区、路径八种 |
 | 候选区 | 导入结果先进入 `floor_plan_import_candidates`，不直接覆盖正式 shape |
 | 错误明细 | 非法点位、未闭合 polyline、JSON 错误等进入错误明细 |
-| 人工发布 | 仅 `parsed` 任务和 `pending` 候选允许发布 |
+| 人工发布 | `parsed`/`published` 任务的 `pending` 候选允许分批发布；`failed`/`cancelled` 任务拒绝 |
 | 正式 shape | 发布后写入既有 `floor_plan_shapes` |
 | 权限 | 新增 `floor_plan.import.view`、`floor_plan.import.manage` |
 | 留痕 | 覆盖导入创建、查看候选、发布、失败事件 |
 | 幂等 | 创建导入和发布导入均接入 idempotency |
-| 数据库 | 新增 V034 迁移，包含导入任务、候选、错误表和权限种子 |
+| 数据库 | 新增 V034 迁移，包含导入任务、候选、错误表和权限种子；候选 `shape_type` 八值 CHECK 见 V423（V034 原六值已被其后迁移替换） |
 
 ## 3. 新增接口
 
@@ -46,6 +48,8 @@ P2-A 商场平面图 SVG/标准 JSON 导入能力已完成当前可编码范围
 
 ## 5. 验证证据
 
+> 下表为 2026-06 编码完成时的 160 记录快照，非当前门禁；当前验证以上述四个测试类与 `npm run verify:predeploy` 门禁为准。
+
 | 验证项 | 160 结果 |
 |---|---|
 | 聚焦测试 | `FloorPlanImportParserTests,FloorPlanServiceTests,FloorPlanControllerTests,FloorPlanJdbcIntegrationTests` 共 33 个测试通过 |
diff --git a/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md b/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
index 6f41b39f..6a89f9cc 100644
--- a/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
+++ b/docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md
@@ -2,6 +2,8 @@
 
 > **执行要求：** 编码前先按本计划做 TDD。先写失败测试并确认失败，再做最小实现，再运行聚焦测试、全量测试、契约校验和安全扫描。
 
+> 2026-09-20 160 全量审计 M019（B3-r1）：发布状态机、示例候选 id、列表信封与八值 CHECK 出处按现行实现改写（M019-004/005/007/009）；其余为历史编码计划原文。
+
 ## 1. 目标
 
 在 P1 “图片 + 多边形坐标”能力基础上，新增 P2-A 平面图导入能力：运营或实施人员可以上传/提交 SVG 或标准化 JSON，系统解析为候选区域，人工审核后发布为正式 `floor_plan_shapes`。
@@ -120,6 +122,8 @@
 - import task：`parsed`、`failed`、`published`、`cancelled`
 - candidate：`pending`、`published`、`rejected`
 
+候选 `shape_type` 八值 CHECK 见 `V423__m016_floor_plan_and_space_type_checks.sql`（V034 原六值已被其后迁移替换）。
+
 ## 6. 接口变化
 
 ### 6.1 创建导入任务
@@ -148,7 +152,7 @@
 
 权限：`floor_plan.import.view`
 
-返回：导入任务分页列表。
+返回：导入任务列表（PageResponse 信封，单页全量，不分页）。
 
 ### 6.3 查询导入候选
 
@@ -170,7 +174,7 @@
 {
   "idempotency_key": "floor-import-1f-publish-001",
   "operator_note": "publish reviewed candidates",
-  "candidate_ids": ["fpic_1", "fpic_2"]
+  "candidate_ids": ["<候选 id 1>", "<候选 id 2>"]
 }
 ```
 
@@ -202,7 +206,7 @@
 - [x] 写失败测试：导入到不存在的 floor plan 返回 `FLOOR_PLAN_NOT_FOUND`。
 - [x] 写失败测试：重复 idempotency key 同请求返回首个结果，不同请求返回冲突。
 - [x] 写失败测试：发布候选会写入正式 `floor_plan_shapes`，并将 task/candidate 标记为 published。
-- [x] 写失败测试：failed/cancelled/published task 不允许重复发布。
+- [x] 写失败测试：failed/cancelled task 不允许发布；published task 可继续发布剩余 pending 候选，已发布候选不得重复发布（M015-016 后口径）。
 - [x] 实现 service 最小逻辑。
 - [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanServiceTests test`。
```

## 3. 内容审计（对照定稿最小处置与现行实现）

- 001：删 `evidence_attachment_ids`/`operator_user_id` 各 2 行；`z_index` 改选填缺省 0（`FloorPlanService:277`）。
- 002：矩阵删不存在的 `FLOOR_PLAN_SHAPE_NOT_FOUND`，改为现行两种行为（他图 id → 400 VALIDATION_FAILED「图形不属于该平面图」；未知 id 按该 id 新建，`saveShapeOnce:260-270`）；补 `PROJECT_NOT_FOUND`（404）、`CROSS_PROJECT_FORBIDDEN`/`CROSS_MERCHANT_FORBIDDEN`（403）。
- 003：`V001__initial_schema.sql` → `V005__p0_platform_master_data.sql`（grep 实态）；两处 Markdown 锚点改真实标题描述。
- 004：p2 §3.4、完成审计 §2、计划 §8 三处改 parsed|published + failed/cancelled 拒（`FloorPlanService:367`）。
- 005：p2 §3.1 示例 id 改 36 字符派生形态 + 规则句说明「示例为示意」；计划 §6.4 候选 id 改占位。
- 006：`source_format` 必填无默认（`validateImportCreateRequest` normalizeAllowed）；解析闭合 `<polyline>`（解析器 :18/:79-84）；非八种整单拒绝（服务 :321）；完成审计 §2 同步。
- 007：完成审计 §5 加快照说明；p2 §6 末句改门禁口径；计划 §6.2 分页措辞改单页全量（M015-007）。
- 008：p2 §3.4 加高危二次确认句并注明 M015-011 同批。
- 009：完成审计 §2 与计划 §5 各加一句指向 V423。
- 改后探针摘要（/home/james/audit-tmp/m019_b3r1_post.log）：
```
p1 4.1 fields: idempotency_key operator_note project_id building_code floor_no plan_name image_url image_width image_height version_no status 
p1 4.2 fields: idempotency_key operator_note shape_id shape_type ref_type ref_id label polygon_points z_index status 
p1 6 codes: CROSS_MERCHANT_FORBIDDEN CROSS_PROJECT_FORBIDDEN FLOOR_PLAN_NOT_FOUND IDEMPOTENCY_CONFLICT PROJECT_NOT_FOUND VALIDATION_FAILED 
p2 3.4 publish rule line: 
audit 2 publish line: 
p2 4.1 non-eight rule: 168:- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的整单拒绝（400 `VALIDATION_FAILED`），�
p2 4.1 polygon-only line: ; parser polyline closed accepted: 18:    private static final Pattern SVG_SHAPE_PATTERN = Pattern.compile("(?is)<(polygon|polyline)\\b([^>]*)/?\\s*>");;79:            if ("polyline".equals(tag) && !samePoint(rawPoints.getFirst(), rawPoints.getLast())) {;80:                errors.add(error(shapeNo, "points", "POLYLINE_NOT_CLOSED", "折线（polyline）必须首尾闭合后才能导入"));;
p2 3.1 example id: ; service derivedId: 298:        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
p2 4.1 defaults: 166:- 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。; parser defaults:  defaultText(attributes.get("data-shape-type"), "shop"),; defaultText(attributes.get("data-ref-type"), "custom"),; blankToNull(attributes.get("data-ref-id")),; defaultText(firstNonBlank(attributes.get("data-label"), attributes.get("id")), "shape-" + shapeNo),;
```
- 内容票：**PASS**。

## 4. 可执行证据原文（/home/james/audit-tmp/m019_b3r1_run.log）

```
START=2026-09-20T09:56:04Z HEAD=e456ba96
CHECK OK
BEFORE_TREE=ce3a76f543b56b781fe93887a824548e0569500c
CHECK OK
APPLIED 26 edits
== post-fix probes
POST_PROBE_EXIT=0
grep: /home/james/audit-tmp/m019_b3r1_post.log: binary file matches
p1 4.1 fields: idempotency_key operator_note project_id building_code floor_no plan_name image_url image_width image_height version_no status 
p1 4.2 fields: idempotency_key operator_note shape_id shape_type ref_type ref_id label polygon_points z_index status 
operator_user_id in floorplan main: 0 files; evidence_attachment_ids in floorplan dto: 0 files
p1 6 codes: CROSS_MERCHANT_FORBIDDEN CROSS_PROJECT_FORBIDDEN FLOOR_PLAN_NOT_FOUND IDEMPOTENCY_CONFLICT PROJECT_NOT_FOUND VALIDATION_FAILED 
p2 3.4 publish rule line: 
audit 2 publish line: 
p2 4.1 polygon-only line: ; parser polyline closed accepted: 18:    private static final Pattern SVG_SHAPE_PATTERN = Pattern.compile("(?is)<(polygon|polyline)\\b([^>]*)/?\\s*>");;79:            if ("p
p2 3.1 example id: ; service derivedId: 298:        String taskId = derivedId("fpi_", floorPlanId + "_" + request.idempotencyKey());
p2 4.1 defaults: 166:- 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。; parser defaults:  defaultText(attributes.get("data-shape-type"), "shop"),; defaultText(attributes.get("data-r
ghost fields remaining: 1 ; stale publish rule remaining: docs/p2-floor-plan-import-api-contract-v0.1.md:0 docs/p2-floor-plan-import-completion-audit-v0.1.md:0 
markdown table rows with wrong column count (p1 §4/§6): 4
AFTER_TREE=d14e6792b4f5f4f37de77ec61020f3099246fa3a
DIFF bytes=13911 sha256=3c37ef2df171937200fece80e55b6b2f2a66b1126232ddddc499c25941c1628a
 docs/p1-floor-plan-polygon-api-contract-v0.1.md     | 21 +++++++++++----------
 docs/p2-floor-plan-import-api-contract-v0.1.md      | 19 +++++++++++--------
 docs/p2-floor-plan-import-completion-audit-v0.1.md  | 12 ++++++++----
 .../2026-06-15-p2-floor-plan-svg-json-import.md     | 10 +++++++---
 4 files changed, 37 insertions(+), 25 deletions(-)
hunks=19 files=4
docs/p1-floor-plan-polygon-api-contract-v0.1.md 131 9a5f5c9949022dc78553410e162191ecb19d95ca5af9ad22cd3a11c50aa8e7ce # P1 商场平面图图片与多边形坐标 API 契约 V0.2
docs/p2-floor-plan-import-api-contract-v0.1.md 214 e36e6160b33372c708ee2acc5c4d426167cca525ed48abca3be9b68f168de921 # P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.2
docs/p2-floor-plan-import-completion-audit-v0.1.md 71 54e364a7e151fcd7f84a04df1d2d96ff6e3aee9ce4566b76ed3432a82d429e2c # P2-A 平面图导入编码完成审计 V0.2
docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md 279 05f1296760679eee73a9491aa06e5bd1bd554a14fcafddbe571cab3529fac388 # P2 商场平面图 SVG/标准 JSON 导入开发计划
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-20T09:56:05Z
```

## 5. 结论

席甲 B4-r1 票：归因 PASS、内容 PASS。落盘前未读 Grok 本次正文；合取以 Grok 票为准。
