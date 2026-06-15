# P1 酒吧挂账应收台账联动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 酒吧账单执行 `on_account` 挂账时，自动生成财务应收台账，并把 `ar_ap_item_id` 回写到挂账流水，避免账单状态与财务台账脱节。

**Architecture:** 酒吧域继续负责账单状态、挂账流水和审计；财务域继续负责 AR/AP 台账创建、幂等和财务审计。新增一个很薄的酒吧到财务桥接服务，调用现有 `FinanceService.createArApItem`，不在酒吧域复制财务规则。

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, JUnit 5, AssertJ.

---

### Task 1: 写失败测试锁定挂账应收联动

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`
- Modify: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
- Modify: `backend/src/test/java/com/complex/platform/finance/FinanceServiceTests.java`

- [ ] **Step 1: BarService 单元测试**

新增测试：`onAccountCreatesReceivableLedgerAndStoresArApItemId`。

验收点：
- `on_account` 金额必须仍为 0。
- 账单状态变为 `payment_status=on_account`、`settlement_status=receivable`。
- 桥接服务收到一条应收创建请求。
- 挂账流水记录带 `arApItemId`。

- [ ] **Step 2: 重复挂账保护测试**

新增测试：`onAccountRejectsAlreadyReceivableBillToAvoidDuplicateArApItem`。

验收点：
- 同一账单已进入挂账应收状态后，换一个幂等键再次挂账必须失败。
- 失败码为 `BAR_BILL_ALREADY_ON_ACCOUNT`。
- 不重复写应收台账。

- [ ] **Step 3: JDBC 测试**

在 `BarJdbcIntegrationTests` 中验证：
- `bar_bill_adjustments` 新增 `ar_ap_item_id` 列。
- 保存和读取挂账流水时能持久化 `ar_ap_item_id`。
- Flyway 当前版本提升到 `021`。

- [ ] **Step 4: FinanceService 来源类型测试**

在 `FinanceServiceTests` 中验证：
- `source_type=bar_bill` 可以创建应收台账。
- `source_id` 保存酒吧账单 ID。

- [ ] **Step 5: 运行失败测试**

在 160 上运行：

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests,BarJdbcIntegrationTests,FinanceServiceTests test
```

预期：失败，原因包括缺少 `arApItemId` 字段、缺少 `bar_bill` 来源类型、缺少数据库列或桥接服务。

### Task 2: 实现最小桥接

**Files:**
- Create: `backend/src/main/java/com/complex/platform/bar/BarReceivableLedgerService.java`
- Create: `backend/src/main/java/com/complex/platform/bar/FinanceBarReceivableLedgerService.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarService.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarBillAdjustmentRecord.java`
- Modify: `backend/src/main/java/com/complex/platform/finance/FinanceService.java`

- [ ] **Step 1: 新增桥接接口**

接口方法：

```java
String createReceivableForOnAccountBill(
        BarBillRecord bill,
        BarOrderRecord order,
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds
);
```

- [ ] **Step 2: 新增桥接实现**

实现策略：
- `direction=receivable`
- `source_type=bar_bill`
- `source_id=bill.id()`
- `amount_cent=bill.payableAmountCent()`
- `due_date=LocalDate.now().plusDays(30)`
- `accounting_period=yyyy-MM`
- `counterparty_id` 优先 `order.customerId()`，否则 `bill.reservationId()`，否则 `bill.tableId()`。
- 幂等键为 `bar-on-account-` + `bill.id()` + `-` + `idempotencyKey`。

- [ ] **Step 3: BarService 调用桥接**

在 `applyBillAdjustmentInternal` 的 `on_account` 分支：
- 如果账单已是 `payment_status=on_account` 或 `settlement_status=receivable`，抛 `BAR_BILL_ALREADY_ON_ACCOUNT`。
- 查询订单，用于桥接服务确定 `counterparty_id`。
- 先创建应收台账，取得 `arApItemId`。
- 保存账单状态和挂账流水。
- 挂账流水的 `snapshot_json` 包含 `ar_ap_item_id`。

- [ ] **Step 4: FinanceService 接受 `bar_bill` 来源**

把 `SOURCE_TYPES` 增加 `bar_bill`，保持现有 `order` 来源兼容。

### Task 3: 数据库迁移与 JDBC 映射

**Files:**
- Create: `backend/src/main/resources/db/migration/V021__p1_bar_on_account_receivable_link.sql`
- Modify: `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- Modify: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`

- [ ] **Step 1: 新增迁移**

```sql
ALTER TABLE bar_bill_adjustments
    ADD COLUMN IF NOT EXISTS ar_ap_item_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_bar_bill_adjustments_ar_ap_item
    ON bar_bill_adjustments (ar_ap_item_id)
    WHERE ar_ap_item_id IS NOT NULL;
```

- [ ] **Step 2: JDBC 保存与读取**

`saveBillAdjustment` 的 INSERT/SELECT/参数/mapper 全部包含 `ar_ap_item_id`。

- [ ] **Step 3: Flyway 版本断言**

`BarJdbcIntegrationTests.currentFlywayVersion()` 期望改为 `021`。

### Task 4: 文档同步

**Files:**
- Modify: `docs/p1-bar-bill-adjustment-api-contract-v0.1.md`

- [ ] **Step 1: 删除旧边界说明**

删除“本切片不生成完整财务应收台账”的说明。

- [ ] **Step 2: 补充新契约**

说明：
- `on_account` 会创建 `direction=receivable`、`source_type=bar_bill` 的 AR/AP 台账。
- 响应中的挂账流水包含 `ar_ap_item_id`。
- 重复挂账返回 `BAR_BILL_ALREADY_ON_ACCOUNT`。
- 同幂等键重复提交仍返回同一结果。

### Task 5: 验证与审查

**Files:**
- Create: `docs/superpowers/reviews/2026-06-14-p1-bar-on-account-ar-ledger-link-review.md`

- [ ] **Step 1: 目标测试**

```bash
cd /home/james/complex-management/backend
mvn -Dtest=BarServiceTests,BarJdbcIntegrationTests,FinanceServiceTests,FinanceJdbcIntegrationTests test
```

- [ ] **Step 2: P0 合同闸口**

```bash
cd /home/james/complex-management
npm run verify:p0-contract
```

- [ ] **Step 3: 全量后端测试**

```bash
cd /home/james/complex-management/backend
mvn test
```

- [ ] **Step 4: 安全扫描**

```bash
cd /home/james/complex-management
semgrep scan --metrics=off --severity WARNING --severity ERROR --config p/java --config p/findsecbugs --config p/secrets backend/src/main/java/com/complex/platform/bar backend/src/main/java/com/complex/platform/finance
```

### Risks and Rollback

- 风险：挂账桥接依赖财务服务，若 FinanceService 幂等策略异常，可能阻断挂账。缓解：桥接使用专属幂等键，并用测试覆盖重复提交。
- 风险：历史已挂账账单没有 `ar_ap_item_id`。缓解：新增列允许为空；只要求新挂账流水可验证。
- 回滚方式：删除 `BarReceivableLedgerService` 与实现，恢复 `BarService` 构造函数和 `on_account` 分支，保留 nullable 数据库列不影响运行。
