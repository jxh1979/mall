# P1 Bar Bill Split Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add P1 bar bill split and merge operations as auditable POS enhancements without changing P0 order/payment semantics.

**Architecture:** Extend the existing `bar` Spring package. A new `bar_bill_operations` table records split/merge facts. Split creates a child bill and reduces the source bill amount. Merge combines an unpaid secondary bill into a primary bill and marks the secondary as `merged`.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, JUnit, MockMvc.

---

### Task 1: Request And Record Types

**Files:**
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillSplitRequest.java`
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillMergeRequest.java`
- Create: `backend/src/main/java/com/complex/platform/bar/BarBillOperationRecord.java`

- [ ] Add split request with idempotency key, split amount, reason, requester, and evidence attachments.
- [ ] Add merge request with idempotency key, merged bill id, reason, requester, and evidence attachments.
- [ ] Add operation record for audit/listing.

### Task 2: Service And Repository

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/bar/BarService.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/BarRepository.java`
- Modify: `backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java`
- Test: `backend/src/test/java/com/complex/platform/bar/BarServiceTests.java`

- [ ] Write failing tests for split success, merge success, invalid split amount, same-bill merge, and paid/settled bill rejection.
- [ ] Implement `splitBill`, `mergeBills`, `saveBillOperation`, and `listBillOperations`.
- [ ] Keep split/merge rejected for settled or already paid bills to avoid hidden payment reconciliation.

### Task 3: Controller And Migration

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/bar/BarController.java`
- Modify: `backend/src/test/java/com/complex/platform/bar/BarControllerTests.java`
- Modify: `backend/src/test/java/com/complex/platform/bar/BarJdbcIntegrationTests.java`
- Create: `backend/src/main/resources/db/migration/V033__p1_bar_bill_split_merge.sql`

- [ ] Add `POST /api/v1/bar/bills/{bill_id}/split`, `POST /api/v1/bar/bills/{bill_id}/merge`, and `GET /api/v1/bar/bills/{bill_id}/operations`.
- [ ] Seed `bar.bill.split_merge` and grant it to `role_platform_admin`.
- [ ] Update latest Flyway assertions from 032 to 033.

### Task 4: Verification

**Files:**
- Create: `docs/p1-bar-bill-split-merge-api-contract-v0.1.md`

- [ ] Document paths, permissions, request examples, validations, audit events, and non-goals.
- [ ] Verify on 160 with `mvn -Dtest=BarServiceTests,BarControllerTests,BarJdbcIntegrationTests test`.

**Acceptance Criteria:**
- Splitting an unpaid bill creates a child bill, lowers source payable/subtotal by split amount, and writes a `split` operation record.
- Merging an unpaid secondary bill into a primary bill increases primary payable/subtotal, zeroes the secondary, marks secondary `merged`, and writes a `merge` operation record.
- Split/merge endpoints require idempotency keys and `bar.bill.split_merge`.
- Settled or already paid bills are rejected for split/merge.
- Same-bill merge is rejected.

**Risks And Rollback:**
- Risk: Split/merge with partial payment can corrupt finance reconciliation. Mitigation: reject paid or settled bills in this P1 slice.
- Risk: Item-level split is more precise but requires order-item allocation. Mitigation: this slice is amount-level only; item-level split remains future enhancement.
- Rollback before deployment: remove V033, new classes, service/controller methods, tests, and doc.
- Rollback after deployment: forward migration can disable `bar.bill.split_merge` while preserving operation history.
