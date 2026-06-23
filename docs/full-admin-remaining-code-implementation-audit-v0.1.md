# 完整后台非首批页面实现审计 V0.1

## 结论

本轮完成 full-admin-page-permission-field-matrix-v0.1.csv 中 22 个非首批后台页面的下一批 MVP 实现。ADM-TKT-002 动态定价已升级为专用页面，覆盖规则草稿、价格仿真、发布和 P2/P0 边界；ADM-TKT-003 出票与核销追踪已升级为专用页面，覆盖订单查询、出票、已发权益、核销预览和留痕边界；ADM-BAR-002 订单/出品/账单已升级为专用页面，覆盖生成账单、标记出品、账单结算和销售提成口径；ADM-BAR-003 存酒已升级为专用页面，覆盖存酒资产台账、在线领取核销、核验码、claim_items 请求体和证据附件留痕；ADM-BAR-004 销售提成已升级为专用页面，覆盖销售 15%/非销售 10%、不含通票和提成计算请求体；ADM-INV-001 库存台账已升级为专用页面，覆盖物料、余额、流水、负库存禁止、高价值酒水在线核销和 stock movement OpenAPI 请求体；ADM-FIN-002 对账差异已升级为专用页面，覆盖批次、差异、原因/附件人工复核留痕，并明确正式关闭接口未声明；ADM-MER-001 商户服务工单已升级为专用页面，覆盖读/创建/派单/处理/关闭、幂等键、附件证据和正式 /api/v1/merchant-service/work-orders OpenAPI；ADM-AMU-003 安全巡检/事故已升级为专用页面，覆盖巡检创建、提交、整改、复检、重开审批、幂等键、附件证据和事故深度闭环边界；ADM-GOV-006 主管复核与违规阻断已升级为专用页面，覆盖策略读取、违规动作默认阻断评估、主管审批放行、幂等键和附件证据；ADM-GOV-004 合同审批与附件归档已升级为专用页面，覆盖合同读取、草稿创建、附件归档、主体绑定展示、审批通过、幂等键和证据附件；ADM-RISK-001 风控预警已升级为专用页面，覆盖预警读取、创建、处理为已解决、幂等键、原因和证据附件；ADM-INT-002 回调/轮询/补偿已升级为专用页面，覆盖四类事件归一展示、立即重试、补偿处理、轮询校验留痕和生产执行边界；ADM-EVD-001 审计事件已升级为专用页面，覆盖搜索筛选、详情、读取错误态和导出自身留痕；ADM-ANA-001 经营分析已升级为专用页面，覆盖指标快照、指标定义、异常指标、业务域筛选、空/错状态和复盘快照锁定留痕；ADM-MER-002 商户授权工作台已升级为专用页面，覆盖授权工单、授权账单、商户范围过滤、授权确认和越权边界；ADM-GOV-003 四类主体绑定已升级为专用页面，覆盖唯一绑定摘要、运营/合同/结算主体、变更原因、幂等键和证据附件；其余页面接入当前正式 /api/v1 接口或本地留痕动作，并通过 UI 自动化验收。

## 代码证据

- 路由与权限入口：apps/web/src/admin/adminRoutes.ts
- 页面配置与接口证据：apps/web/src/admin/adminPageConfigs.ts（已于 2026-06-23 删除：全部页面已专用化，该配置成为永不渲染的死配置）
- 通用后台页面渲染、空/错/加载状态、操作执行：apps/web/src/admin/pages/GenericAdminPage.tsx（已于 2026-06-23 删除：永不渲染，未知路由由 AdminApp default 内联占位兜底）
- 票务动态定价专用页：apps/web/src/admin/pages/DynamicPricingPage.tsx
- 票务出票与核销追踪专用页：apps/web/src/admin/pages/TicketOrdersPage.tsx
- 酒吧订单/出品/账单专用页：apps/web/src/admin/pages/BarOrdersPage.tsx
- 存酒专用页：apps/web/src/admin/pages/StoredWinePage.tsx
- 销售提成专用页：apps/web/src/admin/pages/BarCommissionsPage.tsx
- 库存台账专用页：apps/web/src/admin/pages/InventoryLedgerPage.tsx
- 对账差异专用页：apps/web/src/admin/pages/ReconciliationPage.tsx
- 商户服务工单专用页：apps/web/src/admin/pages/MerchantWorkOrdersPage.tsx
- 商户授权工作台专用页：apps/web/src/admin/pages/MerchantScopePage.tsx
- 游乐安全巡检/事故专用页：apps/web/src/admin/pages/AmusementSafetyPage.tsx
- 主管复核与违规阻断专用页：apps/web/src/admin/pages/SupervisorReviewPage.tsx
- 四类主体绑定专用页：apps/web/src/admin/pages/SubjectBindingsPage.tsx
- 合同审批与附件归档专用页：apps/web/src/admin/pages/ContractApprovalPage.tsx
- 风控预警专用页：apps/web/src/admin/pages/RiskAlertsPage.tsx
- 回调/轮询/补偿专用页：apps/web/src/admin/pages/IntegrationEventsPage.tsx
- 审计事件专用页：apps/web/src/admin/pages/AuditEventsPage.tsx
- 经营分析专用页：apps/web/src/admin/pages/BusinessAnalyticsPage.tsx
- UI 自动化验收：tests/e2e/full-admin-remaining.spec.ts
- Mock 成功/失败/空状态：tests/e2e/support/mock-api.ts

## 覆盖范围

- 治理：拓扑、组织商户档案、四类主体绑定、合同审批。
- 票务：动态定价、出票与核销追踪。
- 酒吧：订单/出品/账单、存酒、销售提成。
- 游乐：项目与场次、团体/包场、安全巡检/事故。
- 库存与财务：库存台账、对账差异。
- 商户服务：维修、投诉、整改和服务工单。
- 第三方与治理证据：回调/轮询/补偿、风控预警、审计事件、经营分析。

## 对抗式发现

- 已关闭：ADM-MER-001 商户服务工单已从 GenericAdminPage 升级为专用页面，正式 OpenAPI、数据表、权限码、读/创建/派单/处理/关闭写操作和测试均已补齐；生产附件、逾期提醒真实证据仍按 production evidence 台账回填。
- 已关闭：ADM-AMU-003 安全巡检/事故已从 GenericAdminPage 升级为专用页面，覆盖巡检创建、提交、整改、复检、重开审批、幂等键、附件证据和事故工单深度闭环边界；当前不伪装事故工单全闭环。
- 已关闭：ADM-GOV-006 主管复核与违规阻断已从 GenericAdminPage 升级为专用页面，覆盖策略读取、违规动作默认阻断评估、主管审批放行、幂等键和附件证据；不允许绕过 review_id 放行。
- 已关闭：ADM-GOV-001 集团/综合体拓扑已从 GenericAdminPage 升级为专用页面，覆盖经营主体、商户、项目、空间点位摘要，主体-商户-项目关系表和空间点位表；拓扑页只展示关系，不做主体变更。
- 已关闭：ADM-GOV-002 组织与商户档案已从 GenericAdminPage 升级为专用页面，覆盖经营主体、商户档案、项目档案、创建商户档案请求、幂等键和证据附件；不在本页替代四类主体绑定。
- 已关闭：ADM-GOV-003 四类主体绑定已从 GenericAdminPage 升级为专用页面，覆盖唯一绑定摘要、运营主体、合同主体、结算主体、变更原因、幂等键、证据附件和变更提交；历史合同和流程留痕不得覆盖。
- 已关闭：ADM-GOV-004 合同审批与附件归档已从 GenericAdminPage 升级为专用页面，覆盖合同读取、草稿创建、附件归档、主体绑定展示、审批通过、幂等键和证据附件；股东比例仅作为利润报表字段展示。
- 已关闭：ADM-GOV-005 商户准入审批流水已从 GenericAdminPage 升级为专用页面，覆盖商户档案、四类主体绑定、合同草稿/审批、准入闭环摘要、导出原因、/api/v1/evidence/events 审计写入和附件证据。
- 已关闭：ADM-RISK-001 风控预警已从 GenericAdminPage 升级为专用页面，覆盖预警读取、创建、处理为已解决、幂等键、原因和证据附件；忽略高风险预警仍需审批边界。
- 已关闭：ADM-INT-002 回调/轮询/补偿已从 GenericAdminPage 升级为专用页面，覆盖四类事件归一展示、立即重试、补偿处理、轮询校验留痕、幂等键和证据附件；立即重试/补偿处理只更新平台任务状态与审计，不伪造第三方生产补偿执行。
- 已关闭：ADM-EVD-001 审计事件已从 GenericAdminPage 升级为专用页面，覆盖审计搜索、trace 查询、详情前后值、读取失败态、导出自身留痕和 append-only 边界；不提供编辑、删除或覆盖历史事件能力。
- 已关闭：ADM-TKT-002 动态定价已从 GenericAdminPage 升级为专用页面，覆盖规则草稿、价格仿真、发布和 P2/P0 边界。
- 已关闭：ADM-BAR-002 订单/出品/账单已从 GenericAdminPage 升级为专用页面，覆盖账单、出品、结算和销售提成口径。
- 已关闭：ADM-TKT-003 出票与核销追踪已从 GenericAdminPage 升级为专用页面，覆盖订单、出票、权益凭证、核销预览和留痕边界。
- 已关闭：ADM-BAR-003 存酒已从 GenericAdminPage 升级为专用页面，覆盖存酒资产、在线领取核销、核验码、claim_items 请求体和证据附件留痕。
- 已关闭：ADM-BAR-004 销售提成已从 GenericAdminPage 升级为专用页面，覆盖销售 15%/非销售 10%、不含通票和提成计算请求体。
- 已关闭：ADM-INV-001 库存台账已从 GenericAdminPage 升级为专用页面，覆盖物料、余额、流水、负库存禁止、高价值酒水在线核销和 OpenAPI stock movement 请求体。
- 已关闭：ADM-FIN-002 对账差异已从 GenericAdminPage 升级为专用页面，覆盖批次、差异、原因/附件人工复核留痕；正式关闭写接口未在 OpenAPI 声明，未伪关闭。
- 已关闭：ADM-ANA-001 经营分析已从 GenericAdminPage 升级为专用页面，覆盖指标快照、指标定义、异常指标、业务域筛选、空状态、错误状态、窄屏和复盘快照锁定留痕；AI 预测和外部 BI 生产口径不在本页伪造。
- 已关闭：ADM-AMU-001 项目与场次已从 GenericAdminPage 升级为专用页面，覆盖项目、场次、容量摘要、新增场次、幂等键和证据附件；不替代安全巡检和事故闭环。
- 已关闭：ADM-AMU-002 团体/包场已从 GenericAdminPage 升级为专用页面，覆盖团体预约、容量占用、释放原因、释放容量、幂等键和证据附件；不得静默覆盖预约和核销证据。
- 已关闭：ADM-MER-002 商户授权工作台已从 GenericAdminPage 升级为专用页面，覆盖授权工单/账单分表展示、商户角色数据范围过滤、授权范围确认留痕和平台页越权拦截；后端数据范围策略仍是安全边界。
- 已关闭：完整后台 30 个页面均已具备专用页面或首批既有专用实现；`GenericAdminPage` 与 `adminPageConfigs` 已于 2026-06-23 删除（永不渲染的死配置），未知路由由 `AdminApp` default 内联占位兜底。
- P2：对账差异关闭当前为本地留痕动作，后续如需真实审批/导出任务，应补正式 API。经营分析复盘锁定只生成本地审计结果；集成事件页已接入 P1 内部重试/补偿写接口，但真实供应商执行仍需生产证据闭环。

## 验收结果

- tests/e2e/full-admin-remaining.spec.ts：覆盖 22 个非首批页面入口、路由、接口证据、表格内容、ADM-TKT-002 规则草稿/价格仿真/发布专用操作、ADM-TKT-003 订单/出票/权益凭证/核销预览专用操作、ADM-BAR-002 账单/出品/结算专用操作、ADM-BAR-003 存酒领取核销专用操作、ADM-BAR-004 销售提成计算专用操作、ADM-INV-001 库存移动登记专用操作、ADM-FIN-002 对账人工复核专用操作、ADM-MER-001 商户工单创建/派单/处理/关闭专用操作、ADM-AMU-003 安全巡检创建/提交/整改/复检/重开审批专用操作、ADM-GOV-006 主管复核评估/审批放行专用操作、ADM-GOV-004 合同草稿归档/审批通过专用操作、ADM-RISK-001 风控预警创建/处理专用操作、ADM-INT-002 回调/轮询/补偿四类事件展示、立即重试、补偿处理和轮询校验专用操作、ADM-EVD-001 审计事件搜索/详情/导出留痕专用操作、ADM-ANA-001 经营分析三接口聚合/业务域筛选/复盘锁定专用操作、ADM-MER-002 授权范围查看/确认和越权拦截专用路径、ADM-GOV-003 主体绑定变更专用路径、关键业务动作、错误状态、空状态、移动端窄屏。
- 本轮 TDD：先跑红灯确认缺口，再实现后跑绿灯。
