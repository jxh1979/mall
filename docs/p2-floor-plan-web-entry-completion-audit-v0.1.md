# P2 平面图 Web 管理入口完成审计 V0.1

更新时间：2026-06-15
执行机器：192.168.88.160
范围：P2 商场平面图图片底图 + 多边形坐标 Web 管理入口

## 1. 本次完成内容

- 在 Web 后台新增 `floorplan` 模块入口，并挂入“平台治理”导航。
- 支持创建平面图，读取平面图列表、正式区域、导入任务、候选明细和错误明细。
- 支持发起 SVG/标准 JSON 导入任务；导入只生成候选，不直接写入正式 shape。
- 支持人工选择候选 ID 后发布，并通过 `confirmHighImpactAction` 二次确认。
- 为 `/api/v1/floor-plans*` 补齐 OpenAPI 路径、请求体 schema、权限、数据范围、终端范围、幂等和审计事件元数据。
- 重新生成 OpenAPI 操作验收矩阵、契约闭环附录、正式安全基线和发现登记表。
- 新增专项验证脚本 `verify:p2-floor-plan-web-entry`，覆盖导航入口、Web API 引用、发布确认和 OpenAPI 覆盖。

## 2. 明确未做内容

- 未新增后端业务规则，沿用既有 floor-plan 后端接口。
- 未实现 CAD 原生解析、CAD 文件上传、自动识别商铺/设备/公共区。
- 未实现自动发布候选；发布必须人工确认。
- 未把第三方 CAD/SVG 供应商 SDK 固化进平台，仍等待后续供应商资料。

## 3. 对抗式审计结论

- 边界清晰：Web 入口只消费和编排既有正式 API，没有绕过 `/api/v1` 契约，也没有引入临时 TypeScript/SQLite 原型痕迹。
- 发布风险已压住：候选发布走人工确认、幂等键和后端权限校验，避免导入后直接污染正式区域。
- 接口契约已收口：新增客户端路由已进入 OpenAPI、操作矩阵、闭环附录和安全基线，客户端覆盖校验通过。
- 文案损坏风险已处理：修复了 Windows 到 SSH 管道导致的中文问号占位，并用专项脚本检查真实中文标题和发布确认标题。
- 安全扫描未发现本次新增阻塞问题。

## 4. Semgrep 结果

扫描文件：`security-scans/p2-floor-plan-web-entry-semgrep-20260615.json`

- `apps/android-terminal/android-app/app/src/main/AndroidManifest.xml:9`：`exported_activity`。结论：既有 Android 启动 Activity，带 `MAIN/LAUNCHER` intent-filter，属于系统要求的导出入口，不是本次变更引入。
- `ops/160/complex-runtime-config-check.sh:54`：`request-host-used`。结论：误报，脚本中的 `host` 是 JDBC 连接串解析变量，不是 Nginx 请求头 `$host`。
- `ops/160/complex-runtime-config-check.sh:58`：`request-host-used`。结论：误报，同上。

## 5. 验证证据

- `npm run verify:p2-floor-plan-web-entry`：通过。
- `npm run verify:p0-contract`：通过，198 个 OpenAPI operation 同步。
- `npm run build`：通过，Web 生产构建成功。
- `npm run test:backend`：通过，555 个后端测试成功。
- `npm run verify:security-baseline`：通过。
- `git diff --check`：通过。
- `semgrep --config p/owasp-top-ten --config p/secrets`：扫描完成，3 条非本次阻塞告警，已在本审计记录说明。

## 6. 回滚方式

- 本切片可通过单独回滚提交撤销。
- 若 Web 入口出现生产问题，可先隐藏 `floorplan` 导航入口并保留后端接口与 OpenAPI，不影响 P0/P1 主链路。
- 若 OpenAPI 派生文档冲突，重新执行 `npm run docs:operation-matrix`、`npm run docs:openapi-closure`、`npm run docs:security-baseline` 后再跑 `npm run verify:p0-contract` 和 `npm run verify:security-baseline`。