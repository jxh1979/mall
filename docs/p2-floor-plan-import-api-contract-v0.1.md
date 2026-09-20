# P2-A 平面图 SVG/标准 JSON 导入接口契约 V0.2

> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：发布前置改为 `parsed`/`published` 可继续发布剩余待处理候选（M019-004）；示例 id 改为派生形态并加说明（M019-005）；`source_format` 必填无默认、解析闭合 `<polyline>`、非八种整单拒绝（M019-006）；§6 验证口径以 verify:predeploy 为准（M019-007）；§3.4 补高危二次确认句（M019-008）。

## 1. 定位

本文档定义 P2-A 商场平面图导入能力的后端接口、权限、事件、数据边界和验收口径。该能力建立在 P1 `mall_floor_plans` 与 `floor_plan_shapes` 之上，用于把矢量图或标准数据解析为候选区域，再由管理员人工发布为正式 shape。机内码仍为 `svg` / `standard_json`。

本期不支持 DWG/DXF/CAD 原生解析、不支持 AI/OCR 自动识别铺位号、不支持机器人/视频/设备深度绑定。CAD 文件必须先由实施工具转换为 SVG 或标准 JSON。

## 2. 数据与权限

新增表：

| 表 | 用途 |
|---|---|
| `floor_plan_import_tasks` | 记录一次导入任务、来源格式、来源摘要、状态、候选数和错误数 |
| `floor_plan_import_candidates` | 记录解析出的候选 shape |
| `floor_plan_import_errors` | 记录导入错误明细 |

新增权限：

| 权限 | 用途 | 风险 |
|---|---|---|
| `floor_plan.import.view` | 查看导入任务、候选和错误 | 中 |
| `floor_plan.import.manage` | 创建导入任务、发布候选 shape | 高 |

## 3. 接口

### 3.1 创建导入任务

`POST /api/v1/floor-plans/{id}/imports`

权限：`floor_plan.import.manage`

成功事件：`floor_plan.import_created`

失败事件：`floor_plan.import_create_failed`

请求：

```json
{
  "idempotency_key": "floor-import-create-001",
  "operator_note": "import 1F svg",
  "source_format": "svg",
  "source_name": "1F.svg",
  "source_payload": "<svg viewBox=\"0 0 1000 800\"><polygon id=\"A101\" points=\"100,80 300,80 300,240 100,240\" /></svg>"
}
```

响应：

```json
{
  "code": "OK",
  "data": {
    "id": "fpi_0123456789abcdef0123456789abcdef",
    "floor_plan_id": "fp_1",
    "source_format": "svg",
    "source_name": "1F.svg",
    "status": "parsed",
    "candidate_count": 1,
    "error_count": 0
  }
}
```

规则：

- `idempotency_key` 必填；任务 id 为 `fpi_` + 由平面图 id 与幂等键派生的 SHA-256 前 32 位十六进制（共 36 字符），不直接拼接幂等键；本文示例 id 均为示意。
- `source_format` 必填，给人看只有两种：矢量图（`svg`）、标准数据（`standard_json`）。不接受其它格式，无默认值。
- `source_payload` 最大 1,000,000 字符。
- 导入只生成候选和错误，不直接改写正式 `floor_plan_shapes`。
- 若没有任何合法候选，任务状态为 `failed`。

> 已实现修复（finding F-CROSS-10，2026-06-22，已在 160 编译通过）：在已有 `source_payload ≤ 1,000,000 字符` 上限之外，新增解析阶段的资源上限以防 DoS（小体积 payload 仍可展开为海量节点/顶点）：
> - **节点（候选形状）数上限 2000**：单次导入解析出的候选形状数超过 2000 时抛 `FLOOR_PLAN_IMPORT_TOO_MANY_NODES`。
> - **单形状顶点数上限 1000**：任一形状的多边形顶点数超过 1000 时抛 `FLOOR_PLAN_IMPORT_TOO_MANY_POINTS`。
>
> 两项上限在解析候选时即校验并快速失败，与既有 1MB payload 上限互补，避免恶意/异常 SVG 或标准 JSON 撑爆解析与候选落库。

### 3.2 查询导入任务

`GET /api/v1/floor-plans/{id}/imports`

权限：`floor_plan.import.view`

事件：`floor_plan.import_viewed`

返回该平面图下导入任务列表。

### 3.3 查询导入候选和错误

`GET /api/v1/floor-plans/{id}/imports/{importId}/candidates`

权限：`floor_plan.import.view`

事件：`floor_plan.import_candidate_viewed`

返回：

```json
{
  "code": "OK",
  "data": {
    "task": {
      "id": "fpi_1",
      "status": "parsed"
    },
    "candidates": [
      {
        "id": "fpic_1",
        "shape_type": "shop",
        "ref_type": "space",
        "ref_id": "sp_101",
        "label": "A101",
        "polygon_points": [
          {"x": 0.1, "y": 0.1},
          {"x": 0.3, "y": 0.1},
          {"x": 0.3, "y": 0.3}
        ],
        "status": "pending"
      }
    ],
    "errors": []
  }
}
```

### 3.4 发布导入候选

`POST /api/v1/floor-plans/{id}/imports/{importId}/publish`

权限：`floor_plan.import.manage`

成功事件：`floor_plan.import_published`

失败事件：`floor_plan.import_publish_failed`

请求：

```json
{
  "idempotency_key": "floor-import-publish-001",
  "operator_note": "publish reviewed candidates",
  "candidate_ids": ["fpic_1"]
}
```

规则：

- `parsed` 或 `published` 状态的导入任务允许继续发布剩余待处理候选；`failed`/`cancelled` 拒绝。
- 仅 `pending` 候选允许发布。
- 发布后生成正式 `floor_plan_shapes`，候选状态变为 `published`，任务状态变为 `published`。
- 发布必须幂等，同一请求重复提交不得重复创建正式 shape。
- 发布为高危动作，须二次确认（治理地图「空间和点位」）；服务端确认字段与 OpenAPI/前端同批落地（M015-011），本文尚未列入请求体。

## 4. 解析规则

### 4.1 SVG

- 解析 `<polygon>` 与首尾闭合的 `<polyline>`。
- `<polyline>` 必须已闭合；未闭合时记录 `POLYLINE_NOT_CLOSED`，不生成候选。
- 必须存在正向 `viewBox`，用于把点位归一化到 0 到 1。
- 默认 `shape_type=shop`（铺位），默认 `ref_type=custom`。
- `shape_type` 只允许八种：铺位 `shop`、摊位 `booth`、公共区 `public_area`、区域 `area`、入口 `entrance`、设施 `facility`、风险区 `risk_zone`、路径 `route`。无走道。
- 可通过 `data-shape-type`、`data-ref-type`、`data-ref-id` 覆盖；不在八种内的整单拒绝（400 `VALIDATION_FAILED`），不生成任务、候选与错误明细。
- 候选 label 优先取 `data-label`，其次取 `id`。

### 4.2 标准 JSON

支持以下结构：

```json
{
  "shapes": [
    {
      "label": "Shop 102",
      "shape_type": "shop",
      "ref_type": "space",
      "ref_id": "sp_102",
      "z_index": 5,
      "polygon_points": [
        {"x": 0.2, "y": 0.2},
        {"x": 0.4, "y": 0.2},
        {"x": 0.4, "y": 0.4}
      ]
    }
  ]
}
```

## 5. 验收标准

- 合法 SVG polygon 能生成候选 shape，坐标归一化到 0 到 1。
- 未闭合 polyline 不生成候选，必须生成错误明细。
- 合法标准 JSON 能生成候选 shape。
- 少于 3 个点、越界点、空 payload、未知格式必须被拒绝或记录错误。
- 导入任务不会直接覆盖正式 shape。
- 发布成功后，正式 shape 数量与发布候选数量一致。
- 权限、幂等和审计事件必须覆盖所有新增写操作。
- Flyway V034 必须创建表、索引、约束、权限和平台管理员授权。

## 6. 当前验证

当前验证以 160 机器为准：

- `FloorPlanImportParserTests`
- `FloorPlanServiceTests`
- `FloorPlanControllerTests`
- `FloorPlanJdbcIntegrationTests`

全量后端测试、P0 契约校验与安全扫描以 `npm run verify:predeploy` 门禁为准（完成审计 §5 的数字为编码完成时快照）。
