# P2 商场平面图 SVG/标准 JSON 导入开发计划

> **执行要求：** 编码前先按本计划做 TDD。先写失败测试并确认失败，再做最小实现，再运行聚焦测试、全量测试、契约校验和安全扫描。

## 1. 目标

在 P1 “图片 + 多边形坐标”能力基础上，新增 P2-A 平面图导入能力：运营或实施人员可以上传/提交 SVG 或标准化 JSON，系统解析为候选区域，人工审核后发布为正式 `floor_plan_shapes`。

本期不做 DWG/DXF/CAD 原生解析，不做自动识别商户合同，不做机器人导航，不做视频点位深度联动。CAD 文件必须先由实施工具转为 SVG 或标准 JSON 后再导入。

## 2. 当前项目理解

- 现有模块：`com.complex.platform.floorplan`。
- 现有表：`mall_floor_plans`、`floor_plan_shapes`。
- 现有权限：`floor_plan.view`、`floor_plan.manage`。
- 现有接口：
  - `GET /api/v1/floor-plans`
  - `POST /api/v1/floor-plans`
  - `GET /api/v1/floor-plans/{id}/shapes`
  - `POST /api/v1/floor-plans/{id}/shapes`
- 现有校验：多边形至少 3 点，坐标使用 0 到 1 的归一化坐标。
- 现有写操作已经接入幂等与审计。

## 3. 推荐边界

### 3.1 MVP

- 支持两类导入源：
  - `svg`：解析 `<polygon>` 和 `<polyline>`，从 `points` 属性提取坐标。
  - `standard_json`：解析平台定义的候选 shape 数组。
- 导入先进入“候选区”，不直接覆盖正式平面图。
- 人工发布后，候选区写入 `floor_plan_shapes`。
- 支持导入错误明细，便于实施人员修图或修数据。
- 发布操作必须幂等、必须留痕。

### 3.2 暂不进入本次编码

- DWG/DXF/CAD 原生解析。
- SVG 中复杂路径 `path d` 的贝塞尔曲线自动转 polygon。
- OCR/AI 自动识别铺位号、商户名。
- 自动绑定商户合同、机器人地图、视频监控点位。
- 多楼层连通路径、导航和应急疏散算法。

## 4. 文件变更

### 4.1 新增文件

- `backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java`
- `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java`
- `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java`
- `backend/src/test/java/com/complex/platform/floorplan/FloorPlanImportParserTests.java`

### 4.2 修改文件

- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java`
- `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java`
- `backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java`
- `backend/src/test/java/com/complex/platform/floorplan/FloorPlanServiceTests.java`
- `backend/src/test/java/com/complex/platform/floorplan/FloorPlanControllerTests.java`
- `backend/src/test/java/com/complex/platform/floorplan/FloorPlanJdbcIntegrationTests.java`
- `docs/p2-floor-plan-import-api-contract-v0.1.md`
- `docs/00-current-effective-document-entry-v0.1.md`

## 5. 数据结构变化

新增 `floor_plan_import_tasks`：

- `id varchar(64) primary key`
- `floor_plan_id varchar(64) not null references mall_floor_plans(id)`
- `source_format varchar(32) not null`
- `source_name varchar(160) not null`
- `source_hash varchar(128) not null`
- `source_payload jsonb not null`
- `status varchar(32) not null`
- `candidate_count integer not null default 0`
- `error_count integer not null default 0`
- `operator_note varchar(500)`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

新增 `floor_plan_import_candidates`：

- `id varchar(64) primary key`
- `import_task_id varchar(64) not null references floor_plan_import_tasks(id)`
- `candidate_no integer not null`
- `shape_type varchar(32) not null`
- `ref_type varchar(32) not null`
- `ref_id varchar(64)`
- `label varchar(160) not null`
- `polygon_points jsonb not null`
- `z_index integer not null default 0`
- `status varchar(32) not null`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`

新增 `floor_plan_import_errors`：

- `id varchar(64) primary key`
- `import_task_id varchar(64) not null references floor_plan_import_tasks(id)`
- `line_no integer`
- `field_name varchar(120)`
- `error_code varchar(80) not null`
- `error_message varchar(500) not null`
- `created_at timestamptz not null`

权限新增：

- `floor_plan.import.view`
- `floor_plan.import.manage`

状态约束：

- import task：`parsed`、`failed`、`published`、`cancelled`
- candidate：`pending`、`published`、`rejected`

## 6. 接口变化

### 6.1 创建导入任务

`POST /api/v1/floor-plans/{id}/imports`

权限：`floor_plan.import.manage`

请求体：

```json
{
  "idempotency_key": "floor-import-1f-svg-001",
  "operator_note": "import 1F svg",
  "source_format": "svg",
  "source_name": "1F.svg",
  "source_payload": "<svg viewBox=\"0 0 1000 800\"><polygon id=\"A101\" points=\"10,10 100,10 100,80 10,80\" /></svg>"
}
```

返回：导入任务、候选数量、错误数量和状态。

### 6.2 查询导入任务

`GET /api/v1/floor-plans/{id}/imports`

权限：`floor_plan.import.view`

返回：导入任务分页列表。

### 6.3 查询导入候选

`GET /api/v1/floor-plans/{id}/imports/{importId}/candidates`

权限：`floor_plan.import.view`

返回：候选 shape 列表和错误明细。

### 6.4 发布导入候选

`POST /api/v1/floor-plans/{id}/imports/{importId}/publish`

权限：`floor_plan.import.manage`

请求体：

```json
{
  "idempotency_key": "floor-import-1f-publish-001",
  "operator_note": "publish reviewed candidates",
  "candidate_ids": ["fpic_1", "fpic_2"]
}
```

返回：发布后的正式 shape 数量和导入任务状态。

## 7. 审计事件

- `floor_plan.import_created`
- `floor_plan.import_create_failed`
- `floor_plan.import_viewed`
- `floor_plan.import_candidate_viewed`
- `floor_plan.import_published`
- `floor_plan.import_publish_failed`

## 8. TDD 任务

### Task 1: Parser

- [x] 写失败测试：SVG polygon 解析为候选 shape。
- [x] 写失败测试：SVG polyline 首尾未闭合时自动闭合或拒绝，默认推荐拒绝并记录错误。
- [x] 写失败测试：标准 JSON 候选解析。
- [x] 写失败测试：少于 3 点、越界坐标、空 payload、未知格式均失败并返回错误明细。
- [x] 实现 `FloorPlanImportParser`。
- [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanImportParserTests test`。

### Task 2: Service

- [x] 写失败测试：创建导入任务会生成 task、candidate、error，并记录审计。
- [x] 写失败测试：导入到不存在的 floor plan 返回 `FLOOR_PLAN_NOT_FOUND`。
- [x] 写失败测试：重复 idempotency key 同请求返回首个结果，不同请求返回冲突。
- [x] 写失败测试：发布候选会写入正式 `floor_plan_shapes`，并将 task/candidate 标记为 published。
- [x] 写失败测试：failed/cancelled/published task 不允许重复发布。
- [x] 实现 service 最小逻辑。
- [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanServiceTests test`。

### Task 3: Controller

- [x] 写失败测试：创建导入任务要求 `floor_plan.import.manage`。
- [x] 写失败测试：查询导入任务和候选要求 `floor_plan.import.view`。
- [x] 写失败测试：发布要求 `floor_plan.import.manage` 和 `idempotency_key`。
- [x] 实现 Controller 路由。
- [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanControllerTests test`。

### Task 4: JDBC/Flyway

- [x] 写失败测试：V034 表、索引、约束、权限和平台管理员授权存在。
- [x] 写失败测试：导入任务、候选和错误明细可通过 PostgreSQL 往返。
- [x] 实现 V034 和 JDBC repository。
- [x] 运行 `cd /home/james/complex-management/backend && mvn -Dtest=FloorPlanJdbcIntegrationTests test`。

### Task 5: 文档与闸口

- [x] 新增 `docs/p2-floor-plan-import-api-contract-v0.1.md`。
- [x] 更新 `docs/00-current-effective-document-entry-v0.1.md`。
- [x] 在 160 运行 `cd /home/james/complex-management/backend && mvn test`。
- [x] 在 160 运行 `cd /home/james/complex-management && npm run verify:p0-contract`。
- [x] 在 160 运行 Semgrep 聚焦扫描：`backend/src/main/java/com/complex/platform/floorplan` 与 `V034__p2_floor_plan_import_tasks.sql`。

## 9. 用户故事地图

### MVP

- 作为实施人员，我要导入 SVG 或标准 JSON，得到候选区域和错误明细。
- 作为运营管理员，我要查看候选区域，确认是否可以发布。
- 作为运营管理员，我要发布候选区域，使其成为正式平面图 shape。
- 作为审计人员，我要看到每次导入、失败、发布的审计记录。

### 第二期

- 作为实施人员，我要批量修改候选区域的标签、类型和绑定对象。
- 作为运营管理员，我要回滚某次导入发布。
- 作为供应商，我要按平台标准 JSON 生成可导入文件。

### 未来版本

- 作为实施人员，我要上传 CAD/DWG/DXF 并由工具自动转换。
- 作为运营人员，我要自动识别铺位号、商户名和设备点位。
- 作为调度系统，我要复用平面图作为机器人/视频/应急联动的空间底图。

## 10. 验收标准

- SVG 中合法 polygon 可生成候选 shape，坐标被归一化为 0 到 1。
- 标准 JSON 中合法候选可生成候选 shape。
- 非法候选不写入正式 shape，必须生成可查询错误明细。
- 导入任务不会直接覆盖 `floor_plan_shapes`。
- 发布成功后，正式 shape 数量与选中的候选数量一致。
- 发布必须幂等，重复提交不得重复创建正式 shape。
- 权限、幂等、审计事件必须覆盖所有新增写操作。
- V034 必须创建表、索引、约束、权限和平台管理员授权。
- 全量测试、P0 契约校验和 Semgrep 必须通过。

## 11. 风险与回滚

- 风险：SVG path/复杂 transform 无法完整解析。处理：MVP 只支持 polygon/polyline，复杂 SVG 由实施工具预处理。
- 风险：导入候选误发布影响正式平面图。处理：导入必须先入候选区，发布必须人工触发并留痕。
- 风险：大量 payload 写入数据库导致膨胀。处理：MVP 限制 payload 长度，正式文件仍应存对象存储或实施资料库。
- 风险：同一个候选重复发布。处理：发布操作用幂等键，候选状态从 `pending` 改为 `published` 后不得再次发布。
- 回滚：未上线环境可撤销 V034；已上线环境使用前向迁移停用 `floor_plan.import.*` 权限，并将导入任务标记为 `cancelled`，不得删除正式 shape 历史记录。

## 12. 开始前必须确认的问题

当前无必须阻塞编码的问题。默认方案为：本期只支持 SVG polygon/polyline 和标准 JSON；CAD 原生解析、AI 识别、供应商深度联动后置。
