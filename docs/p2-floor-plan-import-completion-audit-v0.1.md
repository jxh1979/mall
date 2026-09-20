# P2-A 平面图导入编码完成审计 V0.2

> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：人工发布规则按现行实现改写（M019-004）；矢量图导入范围补闭合 `<polyline>`（M019-006）；§5 数字标为编码完成时快照（M019-007）；候选 `shape_type` 八值 CHECK 指向 V423（M019-009）。

## 1. 结论

P2-A 商场平面图 SVG/标准 JSON 导入能力已完成当前可编码范围，并已在 160 机器通过聚焦测试、全量后端测试、P0 契约闸口和 Semgrep 聚焦安全扫描。

本次完成的是“导入任务 -> 解析候选 -> 错误明细 -> 人工发布 -> 正式 shape”闭环，不包含 CAD/DWG/DXF 原生解析、AI/OCR 自动识别、机器人/视频/设备深度绑定。

## 2. 已完成范围

| 范围 | 结论 |
|---|---|
| 矢量图导入 | 支持 `<polygon>` 与首尾闭合的 `<polyline>`；未闭合 `<polyline>` 记录错误，不生成候选 |
| 标准数据导入 | 支持 `shapes` 数组和 `polygon_points` |
| 候选种类 | 只发布铺位、摊位、公共区、区域、入口、设施、风险区、路径八种 |
| 候选区 | 导入结果先进入 `floor_plan_import_candidates`，不直接覆盖正式 shape |
| 错误明细 | 非法点位、未闭合 polyline、JSON 错误等进入错误明细 |
| 人工发布 | `parsed`/`published` 任务的 `pending` 候选允许分批发布；`failed`/`cancelled` 任务拒绝 |
| 正式 shape | 发布后写入既有 `floor_plan_shapes` |
| 权限 | 新增 `floor_plan.import.view`、`floor_plan.import.manage` |
| 留痕 | 覆盖导入创建、查看候选、发布、失败事件 |
| 幂等 | 创建导入和发布导入均接入 idempotency |
| 数据库 | 新增 V034 迁移，包含导入任务、候选、错误表和权限种子；候选 `shape_type` 八值 CHECK 见 V423（V034 原六值已被其后迁移替换） |

## 3. 新增接口

| 方法 | 路径 | 权限 |
|---|---|---|
| `GET` | `/api/v1/floor-plans/{id}/imports` | `floor_plan.import.view` |
| `POST` | `/api/v1/floor-plans/{id}/imports` | `floor_plan.import.manage` |
| `GET` | `/api/v1/floor-plans/{id}/imports/{importId}/candidates` | `floor_plan.import.view` |
| `POST` | `/api/v1/floor-plans/{id}/imports/{importId}/publish` | `floor_plan.import.manage` |

## 4. 关键文件

| 类型 | 文件 |
|---|---|
| 迁移 | `backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql` |
| Controller | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java` |
| Service | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` |
| Parser | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java` |
| Repository | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java`、`JdbcFloorPlanRepository.java` |
| 测试 | `FloorPlanImportParserTests`、`FloorPlanServiceTests`、`FloorPlanControllerTests`、`FloorPlanJdbcIntegrationTests` |
| 接口契约 | `docs/p2-floor-plan-import-api-contract-v0.1.md` |
| 开发计划 | `docs/superpowers/plans/2026-06-15-p2-floor-plan-svg-json-import.md` |

## 5. 验证证据

> 下表为 2026-06 编码完成时的 160 记录快照，非当前门禁；当前验证以上述四个测试类与 `npm run verify:predeploy` 门禁为准。

| 验证项 | 160 结果 |
|---|---|
| 聚焦测试 | `FloorPlanImportParserTests,FloorPlanServiceTests,FloorPlanControllerTests,FloorPlanJdbcIntegrationTests` 共 33 个测试通过 |
| 全量后端测试 | `cd /home/james/complex-management/backend && mvn test`，528 个测试通过，0 失败，0 错误 |
| P0 契约闸口 | `npm run verify:p0-contract` 通过，181 个 operation 仍同步 |
| Semgrep | `~/.local/bin/semgrep --metrics=off --config p/java --error backend/src/main/java/com/complex/platform/floorplan backend/src/main/resources/db/migration/V034__p2_floor_plan_import_tasks.sql`，17 个目标、60 条规则、0 findings |

## 6. 未纳入范围

- DWG/DXF/CAD 原生解析。
- SVG `path d` 复杂曲线解析。
- AI/OCR 自动识别铺位号、商户名、设备点位。
- 自动绑定商户合同、机器人地图、视频监控点位。
- 多楼层连通路径、导航、应急疏散算法。
- 前端可视化批量编辑器。

## 7. 审计结论

当前 P2-A 可以作为下一步前端/实施工具对接的后端基础。后续若进入 CAD 原生导入或 AI 识别，必须另建 P2-B/P2-C 计划，不应直接扩大本次导入接口范围。
