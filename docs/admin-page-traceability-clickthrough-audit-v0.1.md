# 后台页面「全链可点击可溯源」盘点与整改清单 v0.1

> **✅ 2026-07-05 已完成落地**：本清单的整改在 2026-07-05 大轮前端建设中已全站铺完并 push origin/main（tip=7502a08）：
> - **P1/P2 引用下钻全站铺完**：引用字段（merchant/contract/order/project/space/处理人/审计对象）全站可点下钻，走统一 `<TraceLink>` 组件 + navigate 带参 + 目标页按参定位（§4 推荐路径已按此实现）；P2 商户门户内链+来源合同已接（43d9bae）。
> - **KPI 数字可点**：KPI 数字可点（本页聚焦对应表/区块高亮，或跳源业务页），消数据孤岛。
> - **整行明细抽屉**：全站 ~60 个数据表页整行可点→右侧明细抽屉（`RowDetailDrawer` 组件），W/R/F 分批铺完。
> - **P3 域内引用**：基本铺完，剩个别装饰性引用为机械收尾。
> - 反查入口（§4-4 审计时间线）是否全站到位、full-admin-matrix e2e 纳入（§5）为剩余尾项，见 `pending-development-backlog-v0.1.md` 0b-A3/A5/A6。
>
> 下方 §2「现状结论（不达标）」及 §3–§6 为 2026-06-30 原始盘点，**保留作历史对照**；现状已改观，以本框说明为准。

> 创建：2026-06-30。依据业务 HTML 新增全局原则 **「数据可溯源、全链可点击（无孤岛数据）」**（platform-governance-map「整体定位」区）：
> 平台内展示的所有数据、单据、对象与涉及的人员/角色都必须可点击、可追溯——从任意一处都能点进去看到它的来源、归属和上下游关联，可正查也可反查，不允许出现无法点击溯源的孤立数据。
> 本文为该原则对现有 44 个后台页的只读盘点 + 整改清单（不在本轮逐页改造，先出清单与统一实现路径）。

## 1. 审计方法与基线

- 范围：`apps/web/src/admin/pages/*.tsx` 共 **44** 个后台页（与 `full-admin-page-permission-field-matrix` / `adminRoutes.ts` 对齐）。
- 方法：扫描各页对「外键/引用类字段」（merchant_id、project_id、space_id、contract_id、order_id、assignee_id、actor、audit/evidence 引用等）的渲染方式——是纯文本平铺，还是可点击下钻到来源对象。
- 基线提交：`6fa8c87`（条目3 收口后）。

## 2. 现状结论（不达标）——⚠️ 此为 2026-06-30 盘点时的现状，2026-07-05 已改观（见顶部说明）

- **44 页普遍为孤立平铺**：引用类字段以纯文本 `<td>` 展示，**几乎无跨页下钻**（全量扫描仅命中一处测试文件，无生产下钻链接）。
- AdminApp 已有 `navigate(route)` + `history.pushState`（左侧导航用），但**不支持「带对象参数的深链」**（无法"点 merchant_id → 跳商户页并定位该商户"）。
- 后端**承接能力已具备**：对象时间线 `GET /api/v1/evidence/timelines/{object_type}/{object_id}`（已 UNION audit_events，条目4 另有跨对象审计查询）、各域 list/detail 端点齐全——缺的是前端把引用渲染成可点链接 + 目标页按参定位。

> 本段规则已归口至 `docs/scope-wide-coding-rule-canonical-V0.01.md` §B-16，本文不再重复记载。
## 5. 建议排期

- **批次 P1（资金/履约/审计链）**：contract_id、order_id、merchant_id、audit/evidence/actor 下钻 + `<TraceLink>` 组件 + navigate 带参 + 反查时间线入口。先落这批，因为审计与对账最依赖「点得动、查得回」。
- **批次 P2**：project_id、space_id、处理人身份下钻。
- **批次 P3**：域内装饰性引用（equipment_id、ticket_type_id 等）。

## 6. 备注

本轮（条目5）仅交付盘点与清单 + 统一实现路径，**未改页面**。逐批实现为独立工程切片；落地时每批补 Playwright「点击引用→定位目标」验收，确保「无孤岛数据」可机审。
