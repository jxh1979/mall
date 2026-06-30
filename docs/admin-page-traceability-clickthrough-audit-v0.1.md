# 后台页面「全链可点击可溯源」盘点与整改清单 v0.1

> 创建：2026-06-30。依据业务 HTML 新增全局原则 **「数据可溯源、全链可点击（无孤岛数据）」**（platform-governance-map「整体定位」区）：
> 平台内展示的所有数据、单据、对象与涉及的人员/角色都必须可点击、可追溯——从任意一处都能点进去看到它的来源、归属和上下游关联，可正查也可反查，不允许出现无法点击溯源的孤立数据。
> 本文为该原则对现有 44 个后台页的只读盘点 + 整改清单（不在本轮逐页改造，先出清单与统一实现路径）。

## 1. 审计方法与基线

- 范围：`apps/web/src/admin/pages/*.tsx` 共 **44** 个后台页（与 `full-admin-page-permission-field-matrix` / `adminRoutes.ts` 对齐）。
- 方法：扫描各页对「外键/引用类字段」（merchant_id、project_id、space_id、contract_id、order_id、assignee_id、actor、audit/evidence 引用等）的渲染方式——是纯文本平铺，还是可点击下钻到来源对象。
- 基线提交：`6fa8c87`（条目3 收口后）。

## 2. 现状结论（不达标）

- **44 页普遍为孤立平铺**：引用类字段以纯文本 `<td>` 展示，**几乎无跨页下钻**（全量扫描仅命中一处测试文件，无生产下钻链接）。
- AdminApp 已有 `navigate(route)` + `history.pushState`（左侧导航用），但**不支持「带对象参数的深链」**（无法"点 merchant_id → 跳商户页并定位该商户"）。
- 后端**承接能力已具备**：对象时间线 `GET /api/v1/evidence/timelines/{object_type}/{object_id}`（已 UNION audit_events，条目4 另有跨对象审计查询）、各域 list/detail 端点齐全——缺的是前端把引用渲染成可点链接 + 目标页按参定位。

## 3. 整改清单（按被引用对象归类：谁该点到哪个来源）

| 引用字段 | 出现页数 | 应可点到的来源 | 优先级 |
|---|---|---|---|
| `contract_id` | 3 | 合同生命周期工作台 ADM-CONT-001（定位该合同 + 审批/变更/结算链） | **P1**（资金/履约链） |
| `order_id` | 3 | 出票与核销追踪 ADM-TKT-003 / 账单（定位订单 + 退款/核销/积分流水） | **P1** |
| `merchant_id` | 9 | 商户档案/授权 ADM-GOV-002 / ADM-MER-002（商户主体 + 合同 + 工单） | **P1** |
| audit/evidence 引用、actor/operator | 多页 | 审计事件页 ADM-EVD-001 / 对象时间线（正查下游、反查源头） | **P1**（审计链） |
| `project_id` | 11 | 集团/综合体拓扑 ADM-GOV-001（综合体归属下钻） | P2 |
| `space_id` | 7 | 拓扑空间点位 / 空间类型字典 ADM-GOV-007（空间归属与类型） | P2 |
| `assignee_id`/`vendor_user_id`/`reporter_id` | 3+ | 员工/外包账号（处理人身份与所属机构） | P2 |
| `equipment_id`/`ticket_type_id` 等域内引用 | 2 | 设施台账 / 票种（域内对象定位） | P3 |

> 原则要求「**正查也可反查**」：不仅从源头能看到其下游记录（如合同→账单→收款），也要能从任一结果反查回源头（如审计事件→对象→合同）。当前两向都缺。

## 4. 推荐统一实现路径（避免 44 × 各自为政）

1. **导航扩展**：`AdminApp.navigate` 支持携带 `{ objectType, objectId }` 查询参数（URL query），目标页读参后过滤/高亮/打开详情；保留现有无参导航向后兼容。
2. **统一 `<TraceLink>` 组件**：渲染一个可点击的对象引用（显示名/编号），点击即跳目标页 + 参数；无权限或无目标页时降级为纯文本（不破坏现有展示）。一处实现、各页替换纯文本单元格即可，**不需逐页重写**。
3. **目标页按参定位**：各 list 页支持 `objectId` 入参做服务端/前端过滤（多数 list 端点已支持按 id 过滤或可加）。
4. **反查入口**：对象详情/时间线统一走 `evidence/timelines/{type}/{id}`（已 UNION audit_events），审计跨对象走条目4 `audit/operation-events`。
5. **验收纳入**：把「展示的对象引用必须可点击溯源」加入 `full-admin-ui-acceptance-matrix` 的逐页验收项与 Playwright（点击引用→落到目标页且定位），作为新页准入门槛。

## 5. 建议排期

- **批次 P1（资金/履约/审计链）**：contract_id、order_id、merchant_id、audit/evidence/actor 下钻 + `<TraceLink>` 组件 + navigate 带参 + 反查时间线入口。先落这批，因为审计与对账最依赖「点得动、查得回」。
- **批次 P2**：project_id、space_id、处理人身份下钻。
- **批次 P3**：域内装饰性引用（equipment_id、ticket_type_id 等）。

## 6. 备注

本轮（条目5）仅交付盘点与清单 + 统一实现路径，**未改页面**。逐批实现为独立工程切片；落地时每批补 Playwright「点击引用→定位目标」验收，确保「无孤岛数据」可机审。
