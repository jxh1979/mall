# 架构简化 / P2-P3 优化待办清单 2026-06-18

> 来源：本轮对抗式只读审计（后端 `com.complex.platform` + 前端 `apps/web/src`），均带 file:line 证据。
> 原则：交付前夕，优先**纯内部、不改对外行为、不碰契约/testid** 的简化；改变 ID 生成/异常/可见文案的项单列谨慎区。
> 状态：本轮已落地 1 项（见 §0），其余为建议批次，等待 Owner 决定是否在交付后单独 PR 推进。

## 0. 已实施并验证

### 第 1 轮（2026-06-18 上午）
- **`CustomerService.redeemPoints()` 拆分**（`backend/.../customer/CustomerService.java`）：把"取兑换目录项""取/建积分账户"两段抽为 `resolveActiveRedemptionItem()`、`loadOrCreatePointsAccountForUpdate()`。纯内部、行为/异常/审计事件/执行顺序不变。
  - 验证：customer 专项 23 测试通过；全量后端 670 测试 0 失败 0 错误 BUILD SUCCESS；`git diff --check` 通过。

### 第 2 轮（2026-06-18 下午）
- **A1 已完成** — `requireIdempotencyKey` 去重：新增 `common/api/ApiRequests.requireIdempotencyKey`，19 个 controller + `IdempotencyService` 共 20 个文件用 static import 替换私有副本（调用点不变）。新增 `ApiRequestsTests`（3 条）锁定 `VALIDATION_FAILED / "idempotency_key is required"` 契约。
  - 验证：**全量后端 673 测试（670+3）0 失败 0 错误 BUILD SUCCESS**；9 个锁定该契约的 controller 测试仍全过 → 行为字节级不变；`git diff --check` 通过。
- **① useAdminResource — 全面完成（29/29 页）** — 新增 `apps/web/src/admin/hooks/useAdminResource.ts`（ref 持有最新 fetcher/onError 避免重取死循环；增加可选 `onStart` 覆盖"load 开头清全局错误"的变体）。全部 29 个使用旧 `load` 样板的页面已迁移，保留全部 testid 与可见行为。
  - 特殊处理：`DynamicPricingPage`、`TicketOrdersPage` 的 `runAction` 已有名为 `reload` 的形参，迁移时把 hook 返回别名为 `reloadResource` 避免遮蔽。runAction 页的 `await load()` 统一改 `await reload()`。
  - 未纳入：`GenericAdminPage`（有意保留自有 loading/contractGap 与 testid，见谨慎区③）；`TicketingPoliciesPage` 的 `setError` 是表单校验非数据加载。
  - 验证：typecheck 0；web 单测 16 passed；**Playwright UI 28 passed**；`npm run build` 0；`verify:full-admin-matrix` 0（30 页齐全）；**`verify:delivery-gates` exit 0**。

## 1. 后端（按风险/收益比排序）

| # | 项 | 证据 | 改法 | 风险 | 类型 |
|---|---|---|---|---|---|
| A1 | 20 个 Controller 各自手写 `requireIdempotencyKey`（body 完全相同） | `bar/BarController.java:275`、`ticketing/TicketingController.java:251`、`amusement/AmusementController.java:261` …共 20 处 | 抽到 `common/api`/`ApiException` 静态方法，删除 20 份副本 | **零**（字节级同 code/message/status） | 纯内部 |
| A2 | 15 个 Service 重写 `readPage`/`executeWrite`/`codeOf`，而抽象 `evidence/AuditedOperationExecutor` 已存在 | 见下"A2 变体地图" | 各 service 注入 `AuditedOperationExecutor`，私有 helper 改为**瘦委托**（保留签名、调用点零改动），用 `AuditedOperation.of(...)` 包装原始字符串 | 低-中（**审计语义有差异，需逐服务核对**） | 纯内部 | **🔄 进行中：4/15（finance ✅、customer ✅、governance ✅、analytics ✅，均验证；terminal/evidence 复核后暂缓）** |

> **新发现（codeOf 保留规则）**：部分 service（如 `CustomerService`）在 readPage/executeWrite 之外的方法里也**直接调用 `codeOf`**（自定义 try/catch+recordFailure）。这些 service 委托 readPage/executeWrite 后 **`codeOf` 仍需保留**，不能删。删 codeOf 仅适用于 `codeOf` 只被两个 helper 使用的 service（如 finance）。迁移每个包前先 `grep -c codeOf` 确认调用点数。

### A2 变体地图（决定每个 service 能否安全委托 executeWrite）

执行器 `executeWrite` 失败路径硬编码 businessId=`null`、`codeOf`=`getSimpleName()`、`readPage` 与所有 service 逐字一致。据此：

- **`readPage` 委托：所有 15 个 service 均安全**（与执行器逐字一致）。
- **已完成（4/15，均验证零行为变化）**：`finance`✅、`customer`✅、`governance`✅、`analytics`✅（为 analytics 给执行器**新增了 `readSingle`**，向后兼容）。
- **⚠️ 复核后判定不可直接委托（需先增强执行器，已暂缓，私有方法保持不动）**：
  - `terminal/TerminalDeviceService`：executeWrite 用 `storedResponseTransformer`（`this::withoutDeviceToken`，幂等重放时抹设备 token，安全相关）；执行器不支持 transformer。
  - `evidence/EvidenceEventService`：executeWrite 用 **actor 维度 scopedOperationId**（`actorId+":"+op`）+ 请求(非响应)算 businessId + `metadata(response)` 自定义元数据 + 动态 `Function<T,String>` businessType/businessId 重载；与执行器签名不符。
  - 教训：是否"干净匹配"必须读**全 executeWrite body**，仅看失败 businessId+codeOf 不够。
- **`executeWrite` 不可直接委托（会悄改审计数据，本批保持私有方法不动）**：
  - 失败 businessId=`"unknown"`：`amusement`、`amusementInspection`、`amusementBooking`、`inventory`、`inventoryStockMovement`
  - codeOf 非 ApiException 返回 `"INTERNAL_ERROR"`：`inventory`、`inventoryStockMovement`
  - 失败路径用 `businessId(...)` 而非 null：`merchantbilling`、`compliance`
  - 待逐个核对：`ticketing/TicketOfflinePackage`、`ticketing/TicketCredential`、`bar/BarAgeVerification`、`access`

**实现要点（试点已验证）**：给 `AuditedOperation` 加 `static of(...)` 工厂（含本地 record）；service 构造器把 `IdempotencyService` 替换为 `AuditedOperationExecutor`（保留仍直接使用的 `AuditEventService`）；删私有 `codeOf`；**测试构造点**需 `new AuditedOperationExecutor(idempotencyService, auditEventService)` 并与 service 共享同一 `AuditEventService` 实例。
| A3 | `requireText/requireDate/requirePositive/requireEnum/optionalEnum/trimToNull` 跨 20-24 文件复制 | `finance/FinanceService.java:465-512`、`amusement/AmusementService.java:646` … | 建 `common/api/Validation` 统一；**逐字保留 `"X is required"` 文案**（对外契约） | 中（文案是契约） | 纯内部(需保文案) |
| B | 长方法 | `customer/CustomerService.java:296` redeemPoints(已做)、`inventory/InventoryService.java:213` claimSavedBottle(67)、`terminal/TerminalDeviceService.java:265` bind(59) | 内部抽私有方法 | 低 | 纯内部 |
| A5a | `timestampWithTimeZone`/`nullableInstant` 跨多个 Jdbc repo 逐字复制 | `finance/JdbcFinanceRepository.java`、`amusement/JdbcAmusementRepository.java` … | 抽到 `common/jdbc/JdbcTimestamps`（static import） | 低 | 纯内部 | **✅ 已完成** |
| C | `businessId(Object)` instanceof 派发链分散（13/11/7… 分支） | `inventory/InventoryService.java`、`amusement/AmusementService.java` … | record 实现 `Identifiable{String id();}`，派发退化为一行 | 中（机械改多 record） | 纯内部 |

**谨慎区（会改变行为/数据，勿盲目合并）**：
- **A4 `slug()`** 复制 20 份且**已分叉**（amusement 用 UUID-hash，其余用 regex）；用于生成**业务主键 ID**（`finance:191 "arap_"+slug`、`customer:343 "redemption_"+slug`）。合并会改变 ID/idempotency 推导。仅可在确认生成 ID 完全一致的前提下合并同算法的 19 份。
- **A5b `writeJson`** 空值回退/异常类型已分叉（amusement `Map.of()`+IllegalArgument vs finance `List.of()`+IllegalState）。

## 2. 前端（按风险/收益比排序）

> 关键事实：`AdminDataState` **不输出任何 data-testid**，testid 由各页面自挂。因此把"加载/动作样板"抽成 hook 对 Playwright **完全透明**——只要不动现有 testid 字符串（共 308 个）。

| # | 项 | 证据 | 改法 | 风险 | 类型 |
|---|---|---|---|---|---|
| ① | 26 页逐字重复 loading/error + `load` useCallback + `useEffect(()=>{void load()},[load])` + catch `setError+setGlobalError` | `TicketOrdersPage.tsx:81`、`BarOrdersPage.tsx:102`、`AuditEventsPage.tsx:90` …共 26 处 | 抽 `admin/hooks/useAdminResource.ts` | 低 | 纯内部 |
| ④ | ~~10~~ **11 页**重复"列表变更收敛选中行"useEffect（含审计漏统计的 MerchantWorkOrdersPage）| `TicketOrdersPage.tsx`、`RiskAlertsPage.tsx` …共 11 处 | 抽 `useSelectedRow.ts` | 低 | 纯内部 | **✅ 已完成** |
| ② | `runAction` 函数体 4 文件字节级相同 + SupervisorReview 近似 | `AmusementSafetyPage`、`RiskAlertsPage`、`MerchantWorkOrdersPage`、`ContractApprovalPage`、`SupervisorReviewPage` | 抽 `useAdminAction`（`makeResult` 各页注入；SupervisorReview 用可选 `onSuccess` 处理 reviewId） | 低-中 | 纯内部 | **✅ 已完成（5 页，verify:ui 28 仍绿，actionResult 断言通过）** |
| ⑤ | 22 页各自 `const TEXT={…}`，转义风格不一 | `TicketOrdersPage.tsx:11`(\u) vs `AuditEventsPage.tsx:11`(明文) | 仅统一转义风格，**不合并文案值** | 中（可见文案/可能被断言） | 半内部 |

**谨慎区**：
- **③ `GenericAdminPage` 套用 `AdminDataState`**：会丢 `admin-generic-error/empty/contract-gap` testid（`GenericAdminPage.tsx:104,106,110`），**几乎确定破坏验收**，不建议简单替换。此处不一致是**有意**的。
- **⑥ `LegacyConsoleApp.tsx`(313)/`shell.tsx`(448) god-file 拆分**：结构性重构，legacy 表单联动复杂，需独立 PR + 全量 e2e。

## 3. 建议推进顺序（交付后）

1. 后端 A1（零风险）→ A5a → B 其余长方法。
2. 前端 ① → ④ → ②（每批 2-3 页起步，跑 `verify:ui` 护航）。
3. 后端 A2 + A3（同批迁移，逐包回归）。
4. C（Identifiable 接口，机械改动）。
5. 谨慎区（A4/A5b/③/⑥）单独评估，不在常规简化批次内。
