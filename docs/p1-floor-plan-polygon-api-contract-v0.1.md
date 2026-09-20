# P1 商场平面图图片与多边形坐标 API 契约 V0.2

文档类型：Implementation / API Contract

> 2026-09-20 160 全量审计 M019（B3-r1，V0.1→V0.2）：删去现行 DTO/OpenAPI 中不存在的 `evidence_attachment_ids`、`operator_user_id` 字段行，`z_index` 改为选填缺省 0（M019-001）；错误矩阵按现行实现改写——他图 `shape_id` 为 400 `VALIDATION_FAILED`、未知 `shape_id` 按该 id 新建，补 `PROJECT_NOT_FOUND`/`CROSS_PROJECT_FORBIDDEN`/`CROSS_MERCHANT_FORBIDDEN`（M019-002）；§9 引用改为现行迁移文件与真实标题（M019-003）。

本文档固化 P1-FLOOR-PLAN-01「商场平面图先做图片 + 多边形坐标」的最小可编码契约。P0 空间档案中的轻量 `polygon_points` 保持兼容；本 P1 模块作为后续商户、游乐项目、设备、视频点位、机器人点位和 CAD/SVG 导入的标准地图数据源。

## 1. 范围

| 能力 | P1 本次包含 | 本次不包含 |
|---|---|---|
| 平面图管理 | 创建、查询图片平面图，记录项目、楼栋、楼层、图片 URL、图片尺寸、版本和状态 | 图片上传、对象存储、图片裁剪 |
| 多边形标注 | 为平面图保存铺位、摊位、公共区、区域、入口、设施、风险区、路径多边形 | 拖拽编辑器、自动识别、批量绘制 |
| 坐标标准 | 使用 0-1 归一化坐标，结合图片宽高还原像素位置 | 直接存储屏幕像素坐标作为主口径 |
| 引用关系 | 支持引用空间、商户、项目、游乐项目、设备或自定义对象 | 强制校验外部对象存在性 |
| CAD/SVG | 预留后续标准化导入口径 | 本次不实现 CAD/SVG 解析和导入任务 |

## 2. 坐标规则

| 规则 | 说明 |
|---|---|
| 坐标范围 | `x`、`y` 必须在 `[0, 1]` 之间 |
| 原点 | 图片左上角为 `(0, 0)` |
| 右下角 | 图片右下角为 `(1, 1)` |
| 最小点数 | 一个多边形至少 3 个点 |
| 图片尺寸 | `image_width`、`image_height` 必须大于 0，用于前端和第三方转换 |
| 版本 | `version_no` 用于区分同楼层多版本图片，不在本次做自动版本发布流 |

## 3. 接口

| Method | Path | Permission | 幂等 | 说明 |
|---|---|---|---|---|
| `GET` | `/api/v1/floor-plans` | `floor_plan.view` | 否 | 查询平面图列表 |
| `POST` | `/api/v1/floor-plans` | `floor_plan.manage` | 是 | 创建平面图 |
| `GET` | `/api/v1/floor-plans/{id}/shapes` | `floor_plan.view` | 否 | 查询指定平面图的多边形标注 |
| `POST` | `/api/v1/floor-plans/{id}/shapes` | `floor_plan.manage` | 是 | 创建或更新多边形标注 |

## 4. 字段

### 4.1 创建平面图

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一创建请求幂等 |
| `operator_note` | 否 | string | 审计说明，默认 `created` |
| `project_id` | 是 | string | 所属综合体/项目 |
| `building_code` | 否 | string | 楼栋编码 |
| `floor_no` | 是 | string | 楼层，如 `B1`、`1F`、`3F` |
| `plan_name` | 是 | string | 1-160 字符 |
| `image_url` | 是 | string | 图片访问地址或对象存储引用 |
| `image_width` | 是 | integer | 必须大于 0 |
| `image_height` | 是 | integer | 必须大于 0 |
| `version_no` | 是 | string | 图片版本号 |
| `status` | 是 | enum | `active`/`inactive`/`archived` |

### 4.2 保存多边形标注

| 字段 | 必填 | 类型 | 规则 |
|---|---|---|---|
| `idempotency_key` | 是 | string | 同一标注写入请求幂等 |
| `operator_note` | 否 | string | 审计说明，默认 `shape_saved` |
| `shape_id` | 否 | string | 为空时创建；不为空时更新该标注 |
| `shape_type` | 是 | enum | `shop` 铺位 / `booth` 摊位 / `public_area` 公共区 / `area` 区域 / `entrance` 入口 / `facility` 设施 / `risk_zone` 风险区 / `route` 路径。给人看只用这八种中文，无走道 |
| `ref_type` | 是 | enum | `space`/`merchant`/`project`/`amusement_project`/`device`/`custom` |
| `ref_id` | 否 | string | 被引用对象 ID；`custom` 可为空 |
| `label` | 是 | string | 标注名称 |
| `polygon_points` | 是 | point[] | 至少 3 个点，每个点包含 `x`、`y` |
| `z_index` | 否 | integer | 前端渲染层级，缺省 0 |
| `status` | 是 | enum | `active`/`inactive`/`archived` |

## 5. 事件

| 动作 | 成功事件 | 失败事件 |
|---|---|---|
| 查询平面图 | `floor_plan.viewed` | 暂不单列 |
| 创建平面图 | `floor_plan.created` | `floor_plan.create_failed` |
| 查询标注 | `floor_plan.shape_viewed` | 暂不单列 |
| 保存标注 | `floor_plan.shape_saved` | `floor_plan.shape_save_failed` |

## 6. 错误处理矩阵

| 错误 | 检测 | 响应码 | 业务码 | 处理 |
|---|---|---|---|---|
| 缺少幂等键 | 写接口 `idempotency_key` 为空 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 平面图字段缺失 | 项目、楼层、名称、图片 URL、版本等为空 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 图片尺寸非法 | 宽高小于或等于 0 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 状态非法 | 非 `active`/`inactive`/`archived` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 多边形点数不足 | 少于 3 个点 | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 坐标越界 | 任一点 `x`/`y` 不在 `[0, 1]` | 400 | `VALIDATION_FAILED` | 拒绝写入 |
| 项目不存在 | 创建时 `project_id` 查询不到 | 404 | `PROJECT_NOT_FOUND` | 拒绝创建 |
| 越项目 | 项目员工读写其他综合体项目的平面图 | 403 | `CROSS_PROJECT_FORBIDDEN` | 拒绝 |
| 越商户 | 商户账号读写其他商户的平面图 | 403 | `CROSS_MERCHANT_FORBIDDEN` | 拒绝 |
| 平面图不存在 | `floor_plan_id` 查询不到 | 404 | `FLOOR_PLAN_NOT_FOUND` | 拒绝保存标注 |
| 标注属于其他平面图 | 更新时 `shape_id` 属于其他平面图 | 400 | `VALIDATION_FAILED` | 拒绝更新（文案「图形不属于该平面图」）；查询不到的 `shape_id` 按该 id 新建标注，不报错 |
| 幂等冲突 | 同 key 不同请求体 | 409 | `IDEMPOTENCY_CONFLICT` | 要求换 key 或查询原请求 |

## 7. Anti-Patterns

| Don't | Do Instead | Why |
|---|---|---|
| 用像素坐标作为唯一主口径 | 用归一化坐标，图片宽高用于转换 | 不同屏幕、缩放和图片重采样下仍可复用 |
| 把图片文件写进数据库 | 只保存 `image_url` 或对象存储引用 | 降低数据库膨胀和备份成本 |
| 标注直接绑定某个前端画布实现 | 存标准点位数组和引用类型 | 方便 Web、平板、一体机、第三方复用 |
| P1 直接实现 CAD/SVG 全量导入 | 本次留接口和数据模型，后续供应商/格式确定后再做导入器 | 避免把未知格式风险压入 P1 |
| 覆盖 P0 空间轻量坐标字段 | P0 字段保持兼容，P1 平面图作为更完整数据源 | 不破坏已完成 P0 契约 |

## 8. Test Case Specifications

| Test ID | 类型 | 验收点 |
|---|---|---|
| FP-UT-001 | Unit | 创建平面图缺少项目、楼层、图片 URL 或非法尺寸返回 `VALIDATION_FAILED` |
| FP-UT-002 | Unit | 创建平面图成功写入记录并产生 `floor_plan.created` 审计事件 |
| FP-UT-003 | Unit | 保存标注时平面图不存在返回 `FLOOR_PLAN_NOT_FOUND` |
| FP-UT-004 | Unit | 多边形少于 3 个点返回 `VALIDATION_FAILED` |
| FP-UT-005 | Unit | 坐标越界返回 `VALIDATION_FAILED` |
| FP-UT-006 | Unit | 同幂等键同请求返回原结果，不重复写记录和事件 |
| FP-MVC-001 | Controller | 无 `floor_plan.view` 权限不可查询 |
| FP-MVC-002 | Controller | 无 `floor_plan.manage` 权限不可创建或保存标注 |
| FP-IT-001 | Integration | V023 创建平面图表、标注表、索引、权限和平台管理员授权 |
| FP-IT-002 | Integration | JDBC 能保存并读取平面图与多边形 JSON 坐标 |
| FP-GATE-001 | Contract | P1 新接口不漂移 P0 OpenAPI 契约，`verify:p0-contract` 继续通过 |

## 9. References

| Topic | Location |
|---|---|
| 本切片计划 | `docs/superpowers/plans/2026-06-14-p1-floor-plan-polygons.md` |
| P1 总路线 | `docs/master-roadmap.md`（「阶段 8：市场基准增强与 SaaS 化深度能力」） |
| P0/P1 用户故事分层 | `docs/p0-user-story-map-and-sprint-slices-v0.1.md`（§1.1 版本分层「第二期 / P1」行） |
| 空间档案 P0 轻量坐标 | `backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql` |
