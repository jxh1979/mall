package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class BarJdbcIntegrationTests {

    @Autowired
    private JdbcBarRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private com.complex.platform.finance.FinanceRepository financeRepository;

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

    @Autowired
    private com.complex.platform.gate.JdbcAdmissionFactRepository admissionFactRepository;

    @Test
    void aggregateBarRefundCentSumsRefundAdjustmentsWithinBusinessDayWindowAndExcludesWaiver() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_refund_jdbc", "bar_jdbc", null, "T-REFUND", "Refund JDBC", "card_seat",
                6, 2, 50000L, "CNY", "active", now, now));
        repository.saveOrder(new BarOrderRecord(
                "order_refund_jdbc", "bar_jdbc", null, "table_refund_jdbc", null, "customer_jdbc",
                "staff", "created", 128800L, "CNY", "web", "device_1", "staff_sales", now, now));
        repository.saveBill(new BarBillRecord(
                "bill_refund_window_jdbc", "BILL-REFUND-WINDOW", "order_refund_jdbc", "bar_jdbc", null,
                "table_refund_jdbc", null, 128800L, 0L, 0L, 128800L, 0L, "CNY", "paid", "settled",
                "staff_sales", "sales", now, now));

        LocalDate businessDate = LocalDate.parse("2026-06-18");
        Instant from = businessDayResolver.startInstant(businessDate);
        Instant to = businessDayResolver.endInstant(businessDate);

        // 日切按所在地（北京）时间：经营日 06-18 窗口 = 北京 [06-18 04:00, 06-19 04:00) = [06-17T20:00Z, 06-18T20:00Z)。
        // 窗口内退款（北京 10:00 = 06-18T02:00Z）计入 refund_cent；窗口外退款（北京 03:00 = 06-17T19:00Z，未到日切归前一经营日）
        // 不计入；waiver 永不计入。
        saveBarAdjustment("refund_in", "bill_refund_window_jdbc", "refund", 5000L,
                Instant.parse("2026-06-18T02:00:00Z"));
        saveBarAdjustment("refund_out", "bill_refund_window_jdbc", "refund", 9999L,
                Instant.parse("2026-06-17T19:00:00Z"));
        saveBarAdjustment("waiver_in", "bill_refund_window_jdbc", "waiver", 7000L, null);

        com.complex.platform.finance.FinanceRepository.DailyCloseSourceTotals totals =
                financeRepository.aggregateBar(businessDate, from, to);
        assertThat(totals.refundCent()).isEqualTo(5000L);
    }

    private void saveBarAdjustment(String id, String billId, String type, long amount, Instant refundedAt) {
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adj_" + id, billId, type, amount, 128800L, 128800L - amount,
                type + " reason", "finance_1", true, "applied", "key_" + id,
                List.of(), null, "{}", Instant.parse("2026-06-18T10:00:00Z"), refundedAt));
    }

    @Test
    void barBillAdjustmentTypeCheckRejectsIllegalEnum() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_bad_enum_jdbc", "bar_jdbc", null, "T-BAD-ENUM", "Bad Enum JDBC", "card_seat",
                6, 2, 50000L, "CNY", "active", now, now));
        repository.saveOrder(new BarOrderRecord(
                "order_bad_enum_jdbc", "bar_jdbc", null, "table_bad_enum_jdbc", null, "customer_jdbc",
                "staff", "created", 128800L, "CNY", "web", "device_1", "staff_sales", now, now));
        repository.saveBill(new BarBillRecord(
                "bill_bad_enum_jdbc", "BILL-BAD-ENUM", "order_bad_enum_jdbc", "bar_jdbc", null,
                "table_bad_enum_jdbc", null, 128800L, 0L, 0L, 128800L, 0L, "CNY", "unpaid", "unsettled",
                "staff_sales", "sales", now, now));

        assertThatThrownBy(() -> repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adj_bad_enum", "bill_bad_enum_jdbc", "bogus_type", 1L, 128800L, 128799L,
                "illegal type", "finance_1", true, "applied", "key_bad_enum",
                List.of(), null, "{}", now, null)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void migrationCreatesBarTablesAndSeedsPermissions() {
        assertThat(regclass("bar_tables")).isEqualTo("bar_tables");
        assertThat(regclass("bar_reservations")).isEqualTo("bar_reservations");
        assertThat(regclass("bar_table_guest_checkins")).isEqualTo("bar_table_guest_checkins");
        assertThat(regclass("bar_products")).isEqualTo("bar_products");
        assertThat(regclass("bar_orders")).isEqualTo("bar_orders");
        assertThat(regclass("bar_order_items")).isEqualTo("bar_order_items");
        assertThat(regclass("bar_production_tickets")).isEqualTo("bar_production_tickets");
        assertThat(regclass("bar_production_ticket_events")).isEqualTo("bar_production_ticket_events");
        assertThat(regclass("bar_bills")).isEqualTo("bar_bills");
        assertThat(regclass("bar_commission_calculations")).isEqualTo("bar_commission_calculations");
        assertThat(regclass("bar_table_sessions")).isEqualTo("bar_table_sessions");
        assertThat(regclass("bar_table_session_adjustments")).isEqualTo("bar_table_session_adjustments");
        assertThat(regclass("bar_bill_adjustments")).isEqualTo("bar_bill_adjustments");
        assertThat(regclass("bar_bill_operations")).isEqualTo("bar_bill_operations");
        assertThat(columnNames("bar_bill_adjustments")).contains("ar_ap_item_id");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "bar.entry.age_verify",
                "bar.table_commission.calculate",
                "bar.order.view",
                "bar.order.create",
                "bar.production_ticket.view",
                "bar.production_ticket.manage",
                "bar.reservation.view",
                "bar.reservation.create",
                "bar.table.guest_checkin",
                "bar.table.view",
                "bar.table.manage",
                "bar.table.open_close",
                "bar.bill.adjust",
                "bar.bill.split_merge",
                "bar.table_session.view",
                "bar.table_session.create",
                "bar.table_session.transfer",
                "bar.table_session.merge",
                "bar.table_session.renew",
                "bar.table_session.split"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "bar.entry.age_verify",
                "bar.table_commission.calculate",
                "bar.order.create",
                "bar.production_ticket.view",
                "bar.production_ticket.manage",
                "bar.reservation.create",
                "bar.table.guest_checkin",
                "bar.table.manage",
                "bar.table.open_close",
                "bar.bill.adjust",
                "bar.bill.split_merge",
                "bar.table_session.create",
                "bar.table_session.transfer",
                "bar.table_session.merge",
                "bar.table_session.renew",
                "bar.reservation.manage",
                "bar.deposit_rule.manage"
        );
    }

    @Test
    void migrationCreatesDepositRuleTableAndReservationDepositColumns() {
        assertThat(regclass("bar_deposit_rules")).isEqualTo("bar_deposit_rules");
        assertThat(columnNames("bar_reservations")).contains(
                "deposit_intent_id", "deposit_paid_at", "deposit_settlement", "deposit_channel", "deposit_settled_at");
        assertThat(permissionCodes()).contains("bar.reservation.manage", "bar.deposit_rule.manage");
    }

    @Test
    void depositRuleRoundTripAndReservationDepositStatusCheckEnforced() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveDepositRule(new BarDepositRuleRecord(
                "deposit_rule_jdbc", "bar_jdbc_d", null, "bar", "bar_jdbc_d",
                "min_consumption_ratio", null, 3000, 60, true, now, now));
        assertThat(repository.listDepositRules("bar_jdbc_d"))
                .extracting(BarDepositRuleRecord::id, BarDepositRuleRecord::mode, BarDepositRuleRecord::ratioBps)
                .contains(tuple("deposit_rule_jdbc", "min_consumption_ratio", 3000));

        repository.saveTable(new BarTableRecord(
                "table_jdbc_d", "bar_jdbc_d", null, "T-D", "VIP D", "card_seat",
                6, 2, 100000L, "CNY", "active", now, now));
        // 非法 deposit_status 必须被 CHECK 约束拒绝。
        assertThatThrownBy(() -> repository.saveReservation(new BarReservationRecord(
                "reservation_jdbc_bad", "bar_jdbc_d", null, "table_jdbc_d", null,
                now.plusSeconds(3600), 2, 2, 100L, "bogus_status", "staff", null,
                "reserved", "web", null, List.of(), now, now, null)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void saveAndListFlowsPersistReservationOrderBillCommissionAndGuestCheckinRows() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_jdbc",
                "bar_jdbc",
                null,
                "T-JDBC",
                "VIP JDBC",
                "card_seat",
                6,
                2,
                50000L,
                "CNY",
                "active",
                now,
                now
        ));
        repository.saveReservation(new BarReservationRecord(
                "reservation_jdbc",
                "bar_jdbc",
                null,
                "table_jdbc",
                "customer_jdbc",
                now.plusSeconds(3600),
                2,
                2,
                10000L,
                "paid",
                "staff",
                "staff_sales",
                "reserved",
                "web",
                "device_1",
                List.of("att_reservation"),
                now,
                now,
                null
        ));
        repository.saveGuestCheckin(new BarTableGuestCheckinRecord(
                "checkin_jdbc",
                "reservation_jdbc",
                "bar_jdbc",
                "table_jdbc",
                "guest_1",
                "accepted",
                1,
                "device_1",
                "staff_1",
                List.of("att_checkin"),
                now
        ));
        repository.saveProduct(new BarProductRecord(
                "product_jdbc",
                "bar_jdbc",
                null,
                "Whisky",
                "spirit",
                128800L,
                "CNY",
                null,
                null,
                false,
                "active",
                now,
                now,
                30,
                "ml",
                10
        , false, null, null, null));
        repository.saveOrder(new BarOrderRecord(
                "order_jdbc",
                "bar_jdbc",
                null,
                "table_jdbc",
                "reservation_jdbc",
                "customer_jdbc",
                "staff",
                "created",
                128800L,
                "CNY",
                "web",
                "device_1",
                "staff_sales",
                now,
                now
        ));
        repository.saveOrderItem(new BarOrderItemRecord(
                "order_item_jdbc",
                "order_jdbc",
                "product_jdbc",
                "Whisky",
                1,
                128800L,
                128800L,
                "pending",
                now,
                now,
                null,
                0L
        ));
        // V232 补时间第2批：已送达明细带 delivered_at；未送达明细留 null（证伪：service 层不写 delivered_at 或迁移漏列则本断言红）。
        repository.saveOrderItem(new BarOrderItemRecord(
                "order_item_delivered_jdbc",
                "order_jdbc",
                "product_jdbc",
                "Whisky",
                1,
                128800L,
                128800L,
                "delivered",
                now,
                now,
                200,
                0L,
                false,
                null,
                null,
                null,
                false,
                false,
                now
        ));
        repository.saveProductionTicket(new BarProductionTicketRecord(
                "ticket_jdbc",
                "order_jdbc",
                "bar_jdbc",
                null,
                "created",
                now,
                now
        ));
        repository.saveProductionTicketEvent(new BarProductionTicketEventRecord(
                "ticket_event_jdbc",
                "ticket_jdbc",
                "order_jdbc",
                "bar_jdbc",
                "accept",
                "created",
                "accepted",
                "bar-counter-1",
                "staff_bar_1",
                "accept order",
                "ticket-event-jdbc",
                List.of("att_accept"),
                now
        ));
        repository.saveBill(new BarBillRecord(
                "bill_jdbc",
                "BILL-JDBC-001",
                "order_jdbc",
                "bar_jdbc",
                null,
                "table_jdbc",
                "reservation_jdbc",
                128800L,
                0L,
                10000L,
                118800L,
                0L,
                "CNY",
                "unpaid",
                "unsettled",
                "staff_sales",
                "sales",
                now,
                now
        ));
        repository.saveCommissionCalculation(new BarCommissionCalculationRecord(
                "commission_jdbc",
                "bill_jdbc",
                "staff_sales",
                "sales",
                128800L,
                1500,
                19320L,
                "calculated",
                now
        ));

        // 订单列表内联账单摘要（幽灵字段修复）：按订单批量取回账单，供后台账单列/结算/提成定位账单。
        assertThat(repository.listBillsByOrderIds(java.util.List.of("order_jdbc")))
                .extracting(BarBillRecord::id, BarBillRecord::billNo, BarBillRecord::paymentStatus)
                .contains(tuple("bill_jdbc", "BILL-JDBC-001", "unpaid"));

        assertThat(repository.listTables(Optional.empty()))
                .extracting(BarTableRecord::id, BarTableRecord::allowedPassFreeGuestCount, BarTableRecord::minConsumptionCent)
                .contains(tuple("table_jdbc", 2, 50000L));
        assertThat(repository.listReservations(Optional.empty()))
                .extracting(BarReservationRecord::id, BarReservationRecord::depositAmountCent, BarReservationRecord::salesStaffId)
                .contains(tuple("reservation_jdbc", 10000L, "staff_sales"));
        assertThat(repository.countGuestCheckins("reservation_jdbc")).isEqualTo(1);
        assertThat(repository.listOrderItems("order_jdbc"))
                .extracting(BarOrderItemRecord::quantity, BarOrderItemRecord::amountCent, BarOrderItemRecord::deliveryStatus)
                .contains(tuple(1, 128800L, "pending"));
        // V232：未送达明细 delivered_at 为空，已送达明细 delivered_at≈送达当时（round-trip 证明列/mapper/param/INSERT 通链）。
        assertThat(repository.findOrderItemById("order_item_jdbc"))
                .get().extracting(BarOrderItemRecord::deliveredAt).isNull();
        assertThat(repository.findOrderItemById("order_item_delivered_jdbc"))
                .get().extracting(BarOrderItemRecord::deliveredAt).isEqualTo(now);
        assertThat(repository.findBillById("bill_jdbc"))
                .get()
                .extracting(BarBillRecord::payableAmountCent, BarBillRecord::salesRoleType)
                .containsExactly(118800L, "sales");
        assertThat(repository.findProductionTicketById("ticket_jdbc"))
                .get()
                .extracting(BarProductionTicketRecord::ticketStatus)
                .isEqualTo("created");
        assertThat(repository.findProductionTicketByOrderId("order_jdbc"))
                .get()
                .extracting(BarProductionTicketRecord::id)
                .isEqualTo("ticket_jdbc");
        assertThat(repository.listProductionTicketEvents("ticket_jdbc"))
                .extracting(
                        BarProductionTicketEventRecord::id,
                        BarProductionTicketEventRecord::actionType,
                        BarProductionTicketEventRecord::fromStatus,
                        BarProductionTicketEventRecord::toStatus,
                        BarProductionTicketEventRecord::operatorUserId
                )
                .contains(tuple("ticket_event_jdbc", "accept", "created", "accepted", "staff_bar_1"));
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_adjustment_jdbc",
                "bill_jdbc",
                "service_fee",
                12880L,
                118800L,
                131680L,
                "10 percent service fee",
                "supervisor_1",
                true,
                "applied",
                "bill-adjust-jdbc",
                List.of("att_service_fee"),
                "arap_jdbc",
                "{\"payable_before\":118800,\"payable_after\":131680}",
                now,
                null
        ));
        assertThat(repository.listBillAdjustments("bill_jdbc"))
                .extracting(
                        BarBillAdjustmentRecord::id,
                        BarBillAdjustmentRecord::adjustmentType,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::afterPayableAmountCent,
                        BarBillAdjustmentRecord::arApItemId
                )
                .contains(tuple("bill_adjustment_jdbc", "service_fee", 12880L, 131680L, "arap_jdbc"));
        // 待确认#2：CHECK 接受 waiver（减项，无 refunded_at）与 refund（减项，带 refunded_at）。
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_waiver_jdbc",
                "bill_jdbc",
                "waiver",
                10000L,
                131680L,
                121680L,
                "goodwill waiver",
                "supervisor_1",
                true,
                "applied",
                "bill-waiver-jdbc",
                List.of("att_waiver"),
                null,
                "{}",
                now,
                null
        ));
        Instant refundedAt = Instant.parse("2026-06-14T05:00:00Z");
        repository.saveBillAdjustment(new BarBillAdjustmentRecord(
                "bill_refund_jdbc",
                "bill_jdbc",
                "refund",
                20000L,
                121680L,
                101680L,
                "post-settlement refund",
                "finance_1",
                true,
                "applied",
                "bill-refund-jdbc",
                List.of("att_refund"),
                null,
                "{}",
                now,
                refundedAt
        ));
        assertThat(repository.listBillAdjustments("bill_jdbc"))
                .filteredOn(a -> "refund".equals(a.adjustmentType()))
                .extracting(
                        BarBillAdjustmentRecord::id,
                        BarBillAdjustmentRecord::amountCent,
                        BarBillAdjustmentRecord::refundedAt
                )
                .containsExactly(tuple("bill_refund_jdbc", 20000L, refundedAt));
        assertThat(repository.listBillAdjustments("bill_jdbc"))
                .filteredOn(a -> "waiver".equals(a.adjustmentType()))
                .extracting(BarBillAdjustmentRecord::refundedAt)
                .containsExactly((Instant) null);
        repository.saveBill(new BarBillRecord(
                "bill_split_jdbc",
                "BILL-JDBC-001-S1",
                "order_jdbc",
                "bar_jdbc",
                null,
                "table_jdbc",
                "reservation_jdbc",
                40000L,
                0L,
                0L,
                40000L,
                0L,
                "CNY",
                "unpaid",
                "unsettled",
                "staff_sales",
                "sales",
                now,
                now
        ));
        repository.saveBillOperation(new BarBillOperationRecord(
                "bill_operation_jdbc",
                "split",
                "bill_jdbc",
                "bill_split_jdbc",
                "bill_split_jdbc",
                40000L,
                "guest wants separate payment",
                "cashier_1",
                "applied",
                "split-jdbc",
                List.of("att_split"),
                "{\"payable_before\":118800,\"payable_after\":78800}",
                now
        ));
        assertThat(repository.listBillOperations("bill_jdbc"))
                .extracting(
                        BarBillOperationRecord::id,
                        BarBillOperationRecord::operationType,
                        BarBillOperationRecord::secondaryBillId,
                        BarBillOperationRecord::generatedBillId,
                        BarBillOperationRecord::amountCent
                )
                .contains(tuple("bill_operation_jdbc", "split", "bill_split_jdbc", "bill_split_jdbc", 40000L));
    }

    @Test
    void saveAndListTableSessionsAndAdjustments() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_session_jdbc",
                "bar_jdbc",
                null,
                "T-SESSION-JDBC",
                "VIP Session JDBC",
                "card_seat",
                6,
                2,
                50000L,
                "CNY",
                "opened",
                now,
                now
        ));
        repository.saveTableSession(new BarTableSessionRecord(
                "session_jdbc",
                "bar_jdbc",
                "merchant_jdbc",
                LocalDate.parse("2026-06-20"),
                "table_session_jdbc",
                null,
                "customer_jdbc",
                2,
                50000L,
                2,
                "staff_sales",
                "staff_service",
                "open",
                now,
                null,
                null,
                1L,
                now,
                now
        ));
        repository.saveTableSessionAdjustment(new BarTableSessionAdjustmentRecord(
                "adjustment_jdbc",
                "session_jdbc",
                "renew",
                null,
                null,
                "table_session_jdbc",
                "table_session_jdbc",
                "bar_jdbc",
                "merchant_jdbc",
                LocalDate.parse("2026-06-20"),
                50000L,
                50000L,
                Instant.parse("2026-06-21T02:00:00Z"),
                20000L,
                0L,
                1000L,
                "guest extends time",
                "supervisor_1",
                false,
                "applied",
                "renew-jdbc",
                List.of("att_renew"),
                "{\"session_version_before\":1}",
                now
        ));

        assertThat(repository.listTableSessions(Optional.empty()))
                .extracting(
                        BarTableSessionRecord::id,
                        BarTableSessionRecord::merchantId,
                        BarTableSessionRecord::businessDate,
                        BarTableSessionRecord::sessionVersion
                )
                .contains(tuple("session_jdbc", "merchant_jdbc", LocalDate.parse("2026-06-20"), 1L));
        assertThat(repository.listTableSessionAdjustments("session_jdbc"))
                .extracting(
                        BarTableSessionAdjustmentRecord::id,
                        BarTableSessionAdjustmentRecord::adjustmentType,
                        BarTableSessionAdjustmentRecord::additionalMinConsumptionCent,
                        BarTableSessionAdjustmentRecord::additionalServiceFeeCent
                )
                .contains(tuple("adjustment_jdbc", "renew", 20000L, 1000L));
    }

    @Test
    void assignTableSpaceBackfillDrivesOverSpaceResolutionEndToEnd() {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, polygon_points, status, created_at, updated_at)
                VALUES ('space_bar_8', NULL, 'merchant_bar_8', 'Bar Zone 8', 'bar_zone', '[]'::jsonb, 'active', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """);
        repository.saveTable(new BarTableRecord(
                "table_space_8", "bar_space_8", "merchant_bar_8", "T-SP8", "Space8", "card_seat",
                6, 2, 50000L, "CNY", "active", now, now));
        repository.saveOrder(new BarOrderRecord(
                "order_space_8", "bar_space_8", "merchant_bar_8", "table_space_8", null, "customer_8",
                "staff", "created", 10000L, "CNY", "web", "device_1", "staff_sales", now, now));
        repository.saveBill(new BarBillRecord(
                "bill_space_8", "BILL-SP8", "order_space_8", "bar_space_8", "merchant_bar_8",
                "table_space_8", null, 10000L, 0L, 0L, 10000L, 0L, "CNY", "unpaid", "open",
                "staff_sales", "sales", now, now, false, null, null, null, 0, 0L));

        // 回填前：桌台出现在「未回填空间」清单；bar_bill 无法解析到 space（over_space 保守放行）。
        assertThat(repository.listTablesWithoutSpace(Optional.of("merchant_bar_8")))
                .extracting(BarTableSpaceBacklogRecord::id)
                .contains("table_space_8");
        assertThat(admissionFactRepository.resolveSpaceId("bar_bill", "bill_space_8")).isEmpty();

        // 指派/回填 space_id。
        assertThat(repository.findSpaceMerchant("space_bar_8")).contains("merchant_bar_8");
        assertThat(repository.assignTableSpace("table_space_8", "space_bar_8")).isEqualTo(1);

        // 回填后：bar_bill 经 table_id → bar_tables.space_id 解析到该 space（over_space 真实生效）。
        assertThat(admissionFactRepository.resolveSpaceId("bar_bill", "bill_space_8"))
                .contains("space_bar_8");
        assertThat(repository.listTablesWithoutSpace(Optional.of("merchant_bar_8")))
                .extracting(BarTableSpaceBacklogRecord::id)
                .doesNotContain("table_space_8");
    }

    private String regclass(String tableName) {
        return namedParameterJdbcTemplate.queryForObject("""
                SELECT to_regclass(:qualified_name)::text
                """, Map.of("qualified_name", "public." + tableName), String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, String.class);
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = ?
                 ORDER BY ordinal_position
                """, String.class, tableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, String.class);
    }
    @Test
    void barTableSessionPersistsWithPlatformMerchantPlaceholder() {
        // 平台直营开台修复：会话 merchant_id 落 __platform__ 占位可通过 NOT NULL 约束真正入库；
        // 此前平台管理员开台（merchant 为 null）直插会触发 500（null value in column merchant_id）。
        Instant now = Instant.parse("2026-06-20T02:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_platform_open", "bar_jdbc", null, "T-PLAT", "Platform Direct", "card_seat",
                8, 2, 50000L, "CNY", "active", now, now));
        BarTableSessionRecord saved = repository.saveTableSession(new BarTableSessionRecord(
                "session_platform_open", "bar_jdbc", "__platform__", LocalDate.parse("2026-06-20"),
                "table_platform_open", null, null, 2, 50000L, 2, null, null, "open",
                now, null, null, 1L, now, now));
        assertThat(saved.merchantId()).isEqualTo("__platform__");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bar_table_sessions WHERE id = ? AND merchant_id = ?",
                Integer.class, "session_platform_open", "__platform__");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void barTableSessionMerchantIdRejectsNull() {
        // 约束回归：merchant_id 为 NOT NULL——正是 __platform__ 占位修复要绕过的坑，直插 null 必被拒。
        Instant now = Instant.parse("2026-06-20T02:00:00Z");
        repository.saveTable(new BarTableRecord(
                "table_null_merchant", "bar_jdbc", null, "T-NULLM", "Null Merchant", "card_seat",
                8, 2, 50000L, "CNY", "active", now, now));
        assertThatThrownBy(() -> repository.saveTableSession(new BarTableSessionRecord(
                "session_null_merchant", "bar_jdbc", null, LocalDate.parse("2026-06-20"),
                "table_null_merchant", null, null, 2, 50000L, 2, null, null, "open",
                now, null, null, 1L, now, now)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // ========================================================================
    // 第十批 TK-24-R2 / TK-25-R2：台组口径真库回归（bar_order_service_sources 归属 + 并台链递归）
    // ========================================================================

    /** 并台链真库夹具：projects/merchants/spaces 满足 bar_order_service_sources 外键。返回 merchant id。 */
    private String barMerchantFixture(String suffix) {
        String project = "b10p_" + suffix;
        String company = "b10e_" + suffix;
        String merchant = "b10m_" + suffix;
        jdbcTemplate.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','台组综合体','active',now(),now())", project);
        jdbcTemplate.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbcTemplate.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        jdbcTemplate.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at) VALUES(?,'台组酒吧','bar',?,?,?,?,'active',now(),now())", merchant, company, company, company, project);
        return merchant;
    }

    private BarTableSessionRecord familySession(String id, String merchant, String tableId, long minSnapshot,
            String status, String sourceSessionId) {
        Instant now = Instant.parse("2026-06-20T02:00:00Z");
        repository.saveTable(new BarTableRecord(tableId, merchant, merchant, "T-" + tableId, tableId, "card_seat",
                6, 2, minSnapshot, "CNY", "opened", now, now));
        return repository.saveTableSession(new BarTableSessionRecord(id, merchant, merchant,
                LocalDate.parse("2026-06-20"), tableId, null, null, 2, minSnapshot, 2, null, null, status,
                now, null, sourceSessionId, 1L, now, now));
    }

    /** 落一笔归属到 session 的订单（一条明细 amount），返回账单 id。 */
    private String familyOrder(String id, String merchant, BarTableSessionRecord session, long amountCent) {
        Instant now = Instant.parse("2026-06-20T03:00:00Z");
        repository.saveProduct(new BarProductRecord("product_" + id, merchant, merchant, "Drink " + id, "beer",
                amountCent, "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));
        repository.saveOrder(new BarOrderRecord("order_" + id, merchant, merchant, session.tableId(), null, null,
                "staff", "created", amountCent, "CNY", "web", null, null, now, now));
        repository.saveOrderItem(new BarOrderItemRecord("item_" + id, "order_" + id, "product_" + id, "Drink " + id,
                1, amountCent, amountCent, "pending", now, now, null, 0L));
        String project = jdbcTemplate.queryForObject("SELECT project_id FROM merchants WHERE id=?", String.class, merchant);
        jdbcTemplate.update("INSERT INTO bar_order_service_sources(order_id,session_id,table_id,project_id,merchant_id,business_date) VALUES(?,?,?,?,?,'2026-06-20')",
                "order_" + id, session.id(), session.tableId(), project, merchant);
        repository.saveBill(new BarBillRecord("bill_" + id, "BILL-" + id, "order_" + id, merchant, merchant,
                session.tableId(), null, amountCent, 0L, 0L, amountCent, 0L, "CNY", "unpaid", "unsettled",
                null, "non_sales", now, now));
        return "bill_" + id;
    }

    private void familyAdjustment(String id, String billId, String type, long amountCent) {
        Instant now = Instant.parse("2026-06-20T04:00:00Z");
        repository.saveBillAdjustment(new BarBillAdjustmentRecord("bill_adj_" + id, billId, type, amountCent,
                0L, 0L, type, "staff_1", false, "applied", "key_" + id, List.of(), null, "{}", now, null));
    }

    @Test
    void familyConsumedIncludesMergedChildOrdersAndTopUp() {
        // BSM L1079：A 单 300、B 单 400 并入 A、主台补足 100 → 进度 800；阈值 = 两侧低消快照之和。
        String merchant = barMerchantFixture("fam");
        BarTableSessionRecord a = familySession("sess_fam_a", merchant, "table_fam_a", 50000L, "open", null);
        BarTableSessionRecord b = familySession("sess_fam_b", merchant, "table_fam_b", 60000L, "merged", a.id());
        String billA = familyOrder("fam_a", merchant, a, 30000L);
        familyOrder("fam_b", merchant, b, 40000L);
        familyAdjustment("fam_topup", billA, "min_spend_top_up", 10000L);

        assertThat(repository.familySessionIds(a.id(), false)).containsExactly(a.id(), b.id());
        assertThat(repository.findSessionIdByOrderId("order_fam_b")).contains(b.id());
        assertThat(BarMinSpend.thresholdCent(repository, a.id())).isEqualTo(110000L);
        assertThat(BarMinSpend.consumedCent(repository, a.id())).isEqualTo(80000L);
    }

    @Test
    void retainedBalanceSurvivesMergeFamily() {
        // Q28「留款、抵用和未用分别留痕」：留款在 A（留 400、已抵 100），A 并入 B 后 B 的台组余额仍为 300。
        String merchant = barMerchantFixture("ret");
        BarTableSessionRecord b = familySession("sess_ret_b", merchant, "table_ret_b", 50000L, "open", null);
        BarTableSessionRecord a = familySession("sess_ret_a", merchant, "table_ret_a", 50000L, "merged", b.id());
        String billA = familyOrder("ret_a", merchant, a, 100000L);
        familyAdjustment("ret_keep", billA, "min_spend_retained", 40000L);
        familyAdjustment("ret_used", billA, "min_spend_retained_applied", 10000L);

        assertThat(BarMinSpend.retainedBalanceCent(repository, b.id())).isEqualTo(30000L);
    }

    // ========================================================================
    // 第十批 TK-27-R1：关台门槛（BSM L1058「关台指账单全部结清……除上述情形外，账单未结清（含挂起未结清）不得关台」；
    // DEC-R8-97-R1-03）+ 关台时处置未达标定金（BSM L1062「客人已到店但未满足该台位最低消费的，定金不退、予以没收」）
    // ========================================================================

    @Autowired
    private com.complex.platform.access.AccessRepository accessRepository;

    @Autowired
    private BarServiceResponsibilityService responsibilities;

    @org.junit.jupiter.api.AfterEach
    void clearActor() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** 关台真库夹具：商户 + 铺位（tableScope 需要）+ 持 bar.table.open_close 的在岗员工，并以其身份登录。 */
    private String closeFixture(String suffix) {
        String merchant = barMerchantFixture(suffix);
        String project = jdbcTemplate.queryForObject("SELECT project_id FROM merchants WHERE id=?", String.class, merchant);
        jdbcTemplate.update("INSERT INTO spaces(id,project_id,merchant_id,space_name,space_type,status,created_at,updated_at) VALUES(?,?,?,'关台卡座区','unit','active',now(),now())",
                "space_" + suffix, project, merchant);
        String user = "b10u_" + suffix;
        accessRepository.createUser(new com.complex.platform.access.AccessUser(user, user, "staff", project, merchant, "active",
                Instant.parse("2026-06-20T00:00:00Z")));
        String role = "b10r_" + suffix;
        jdbcTemplate.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,'关台测试','测试专用','platform','active',now())", role);
        jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'bar.table.open_close')", role);
        accessRepository.assignUserRole(user, role);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(user, user, "staff", project, merchant, "active"), "test", List.of()));
        return merchant;
    }

    private BarTableSessionRecord closeSession(String id, String merchant, String tableId, long minSnapshot, String status,
            String sourceSessionId) {
        BarTableSessionRecord session = familySession(id, merchant, tableId, minSnapshot, status, sourceSessionId);
        repository.assignTableSpace(tableId, "space_" + merchant.substring("b10m_".length()));
        return session;
    }

    private BarPayrollRequests.CloseSession closeRequest(String key, BarTableSessionRecord session) {
        return new BarPayrollRequests.CloseSession(key, session.sessionVersion(), "实际服务已结束");
    }

    private String tableStatus(String tableId) {
        return repository.findTableById(tableId).orElseThrow().status();
    }

    @Test
    void serviceCloseRejectsWhenAnyFamilyBillUnsettled() {
        String merchant = closeFixture("cls1");
        BarTableSessionRecord b = closeSession("sess_cls1_b", merchant, "table_cls1_b", 50000L, "open", null);
        BarTableSessionRecord a = closeSession("sess_cls1_a", merchant, "table_cls1_a", 50000L, "merged", b.id());
        familyOrder("cls1_a", merchant, a, 30000L); // A 账单 unsettled

        assertThatThrownBy(() -> responsibilities.close(b.id(), closeRequest("close-cls1", b)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("BAR_TABLE_SESSION_BILL_UNSETTLED"));
        assertThat(repository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("open");
        assertThat(tableStatus("table_cls1_b")).isEqualTo("opened");
    }

    @Test
    void serviceCloseRejectsOnAccountReceivableBill() {
        String merchant = closeFixture("cls2");
        BarTableSessionRecord b = closeSession("sess_cls2_b", merchant, "table_cls2_b", 50000L, "open", null);
        String bill = familyOrder("cls2_b", merchant, b, 30000L);
        jdbcTemplate.update("UPDATE bar_bills SET settlement_status='receivable' WHERE id=?", bill);

        assertThatThrownBy(() -> responsibilities.close(b.id(), closeRequest("close-cls2", b)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("BAR_TABLE_SESSION_BILL_UNSETTLED"));
        assertThat(repository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("open");
    }

    @Test
    void serviceCloseAllowsWhenAllBillsSettledOrMerged() {
        String merchant = closeFixture("cls3");
        BarTableSessionRecord b = closeSession("sess_cls3_b", merchant, "table_cls3_b", 50000L, "open", null);
        BarTableSessionRecord a = closeSession("sess_cls3_a", merchant, "table_cls3_a", 50000L, "merged", b.id());
        String billB = familyOrder("cls3_b", merchant, b, 60000L);
        String billA = familyOrder("cls3_a", merchant, a, 30000L);
        jdbcTemplate.update("UPDATE bar_bills SET settlement_status='settled',payment_status='paid',paid_amount_cent=60000 WHERE id=?", billB);
        jdbcTemplate.update("UPDATE bar_bills SET settlement_status='merged' WHERE id=?", billA);

        responsibilities.close(b.id(), closeRequest("close-cls3", b));

        assertThat(repository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("closed");
        assertThat(tableStatus("table_cls3_b")).isEqualTo("pending_clear");
    }

    @Test
    void serviceCloseAllowsEmptyTable() {
        String merchant = closeFixture("cls4");
        BarTableSessionRecord b = closeSession("sess_cls4_b", merchant, "table_cls4_b", 50000L, "open", null);

        responsibilities.close(b.id(), closeRequest("close-cls4", b));

        assertThat(repository.findTableSessionById(b.id())).get()
                .extracting(BarTableSessionRecord::status).isEqualTo("closed");
        assertThat(tableStatus("table_cls4_b")).isEqualTo("pending_clear");
    }

    @Test
    void serviceCloseForfeitsUnsettledDeposit() {
        // BSM L1062「客人已到店但未满足该台位最低消费的，定金不退、予以没收」：空台关台（进度 0 < 低消）→ 已付定金没收。
        String merchant = closeFixture("cls5");
        Instant now = Instant.parse("2026-06-20T02:00:00Z");
        BarTableSessionRecord open = closeSession("sess_cls5_tmp", merchant, "table_cls5_b", 50000L, "closed", null);
        repository.saveReservation(new BarReservationRecord("resv_cls5", merchant, merchant, "table_cls5_b", "customer_cls5",
                now, 2, 2, 20000L, "paid", "staff", null, "reserved", "web", null, List.of(), now, now, null));
        BarTableSessionRecord b = repository.saveTableSession(new BarTableSessionRecord("sess_cls5_b", merchant, merchant,
                LocalDate.parse("2026-06-20"), "table_cls5_b", "resv_cls5", "customer_cls5", 2, 50000L, 2, null, null, "open",
                now, null, null, 1L, now, now));
        assertThat(open.status()).isEqualTo("closed");

        responsibilities.close(b.id(), closeRequest("close-cls5", b));

        assertThat(repository.findReservationById("resv_cls5")).get()
                .extracting(BarReservationRecord::depositStatus, BarReservationRecord::depositSettlement)
                .containsExactly("settled", "forfeited");
    }

    @Test
    void listTablesFlagsPendingClearOverdue() {
        // BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）未恢复空闲的，系统提醒当班主管」。
        String merchant = barMerchantFixture("pcl");
        Instant created = Instant.parse("2026-06-20T02:00:00Z");
        // 真库 timestamptz 只到微秒，取整后再比较。
        Instant overdue = Instant.now().minusSeconds(30 * 60).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Instant fresh = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        repository.saveTable(new BarTableRecord("table_pcl_old", merchant, merchant, "T-PCL-OLD", "old", "card_seat",
                6, 2, 50000L, "CNY", "pending_clear", created, overdue));
        repository.saveTable(new BarTableRecord("table_pcl_new", merchant, merchant, "T-PCL-NEW", "new", "card_seat",
                6, 2, 50000L, "CNY", "pending_clear", created, fresh));
        repository.saveTable(new BarTableRecord("table_pcl_idle", merchant, merchant, "T-PCL-IDLE", "idle", "card_seat",
                6, 2, 50000L, "CNY", "active", created, overdue));

        var byId = repository.listTables(Optional.of(merchant)).stream()
                .collect(java.util.stream.Collectors.toMap(BarTableRecord::id, t -> t));
        assertThat(byId.get("table_pcl_old").pendingClearOverdue()).isTrue();
        assertThat(byId.get("table_pcl_old").pendingClearSince()).isEqualTo(overdue);
        assertThat(byId.get("table_pcl_new").pendingClearOverdue()).isFalse();
        assertThat(byId.get("table_pcl_idle").pendingClearOverdue()).isFalse();
        assertThat(byId.get("table_pcl_idle").pendingClearSince()).isNull();

        // 酒吧（商户）把时限配到 60 分钟（值与上下限同配）后，30 分钟前的待清台不再超时。
        jdbcTemplate.update("UPDATE merchants SET pending_clear_alert_minutes=60, pending_clear_alert_min_minutes=10, "
                + "pending_clear_alert_max_minutes=180 WHERE id=?", merchant);
        assertThat(repository.listTables(Optional.of(merchant)).stream()
                .filter(t -> t.id().equals("table_pcl_old")).findFirst().orElseThrow().pendingClearOverdue()).isFalse();
        // 值与上下限同配：只改值、清空上下限被 CHECK 拒绝。
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE merchants SET pending_clear_alert_min_minutes=NULL WHERE id=?", merchant))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // 复核 money B1（BSM L1058「关台成功后台位自动进入待清台」「并台期间全部原台位保持已开台并归属该合并台组」）：关台时组内每张原台都进入待清台。
    @Test
    void serviceCloseMovesEveryGroupTableToPendingClear() {
        String merchant = closeFixture("clg");
        BarTableSessionRecord b = closeSession("sess_clg_b", merchant, "table_clg_b", 50000L, "open", null);
        closeSession("sess_clg_a", merchant, "table_clg_a", 50000L, "merged", b.id());

        responsibilities.close(b.id(), closeRequest("close-clg", b));

        assertThat(tableStatus("table_clg_b")).isEqualTo("pending_clear");
        assertThat(tableStatus("table_clg_a")).isEqualTo("pending_clear");
    }

    // 复核 money B3：并台期间在被并台位点的单归属该台位的并台会话（当前有效链内），计入台组进度、关台门槛检查其账单。
    @Test
    void orderOnMergedTableBindsToGroupAndBlocksCloseWhileUnsettled() {
        String merchant = closeFixture("rob");
        BarTableSessionRecord b = closeSession("sess_rob_b", merchant, "table_rob_b", 50000L, "open", null);
        BarTableSessionRecord a = closeSession("sess_rob_a", merchant, "table_rob_a", 50000L, "merged", b.id());
        Instant now = Instant.parse("2026-06-20T03:00:00Z");
        BarOrderRecord order = repository.saveOrder(new BarOrderRecord("order_rob_a", merchant, merchant, "table_rob_a", null, null,
                "staff", "created", 30000L, "CNY", "web", null, null, now, now));
        responsibilities.recordOrder(order);
        assertThat(repository.findSessionIdByOrderId("order_rob_a")).contains(a.id());
        repository.saveBill(new BarBillRecord("bill_rob_a", "BILL-rob_a", "order_rob_a", merchant, merchant,
                "table_rob_a", null, 30000L, 0L, 0L, 30000L, 0L, "CNY", "unpaid", "unsettled", null, "non_sales", now, now));

        assertThatThrownBy(() -> responsibilities.close(b.id(), closeRequest("close-rob", b)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("BAR_TABLE_SESSION_BILL_UNSETTLED"));
    }

    // 复核 money B3：台组台位自开台以来无会话归属的订单，其未结账单同样阻断关台。
    @Test
    void orphanUnsettledBillOnGroupTableBlocksClose() {
        String merchant = closeFixture("orp");
        BarTableSessionRecord b = closeSession("sess_orp_b", merchant, "table_orp_b", 50000L, "open", null);
        closeSession("sess_orp_a", merchant, "table_orp_a", 50000L, "merged", b.id());
        Instant now = Instant.parse("2026-06-20T03:00:00Z");
        repository.saveOrder(new BarOrderRecord("order_orp_a", merchant, merchant, "table_orp_a", null, null,
                "staff", "created", 30000L, "CNY", "web", null, null, now, now));
        repository.saveBill(new BarBillRecord("bill_orp_a", "BILL-orp_a", "order_orp_a", merchant, merchant,
                "table_orp_a", null, 30000L, 0L, 0L, 30000L, 0L, "CNY", "unpaid", "unsettled", null, "non_sales", now, now));

        assertThatThrownBy(() -> responsibilities.close(b.id(), closeRequest("close-orp", b)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("BAR_TABLE_SESSION_BILL_UNSETTLED"));
    }

    // 复核 money I1：从被并子会话取低消口径须先上溯到当前有效并台链根，阈值与进度按整个台组。
    @Test
    void minSpendFromMergedChildUsesWholeGroup() {
        String merchant = barMerchantFixture("mch");
        BarTableSessionRecord b = familySession("sess_mch_b", merchant, "table_mch_b", 50000L, "open", null);
        BarTableSessionRecord a = familySession("sess_mch_a", merchant, "table_mch_a", 50000L, "merged", b.id());
        familyOrder("mch_a", merchant, a, 30000L);
        familyOrder("mch_b", merchant, b, 60000L);

        assertThat(BarMinSpend.thresholdCent(repository, a.id())).isEqualTo(100000L);
        assertThat(BarMinSpend.consumedCent(repository, a.id())).isEqualTo(90000L);
    }

    // 复核 money m4：待清台起点单独留痕，编辑台位资料不重置计时。
    @Test
    void pendingClearSinceSurvivesTableEdit() {
        String merchant = barMerchantFixture("pcs");
        Instant created = Instant.parse("2026-06-20T02:00:00Z");
        Instant since = Instant.now().minusSeconds(30 * 60).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        repository.saveTable(new BarTableRecord("table_pcs", merchant, merchant, "T-PCS", "pcs", "card_seat",
                6, 2, 50000L, "CNY", "pending_clear", created, since));
        repository.saveTable(new BarTableRecord("table_pcs", merchant, merchant, "T-PCS", "pcs 改名", "card_seat",
                6, 2, 50000L, "CNY", "pending_clear", created, Instant.now()));

        BarTableRecord listed = repository.listTables(Optional.of(merchant)).stream()
                .filter(t -> t.id().equals("table_pcs")).findFirst().orElseThrow();
        assertThat(listed.pendingClearSince()).isEqualTo(since);
        assertThat(listed.pendingClearOverdue()).isTrue();
    }

    @Autowired
    private BarReceivableLedgerService barLedger;

    @Autowired
    private com.complex.platform.finance.FinanceService financeService;

    // 复核 money I2（BSM L1062「只有实际退款成功才记为已退」）：定金退款应付经财务登记实付结清后，定金才置「达标已原路退回」。
    @Test
    void depositRefundMarkedRefundedOnlyAfterPayableSettled() {
        String merchant = barMerchantFixture("dps");
        Instant now = Instant.parse("2026-06-20T02:00:00Z");
        familySession("sess_dps", merchant, "table_dps", 50000L, "open", null);
        BarReservationRecord reservation = repository.saveReservation(new BarReservationRecord("resv_dps", merchant, merchant,
                "table_dps", "customer_dps", now, 2, 2, 20000L, "paid", "staff", null, "reserved", "web", null, List.of(), now, now, null));
        String payer = "b10u_dps_fin";
        accessRepository.createUser(new com.complex.platform.access.AccessUser(payer, payer, "platform_admin", "org_1", null, "active", now));
        accessRepository.saveUserPermissionOverrides(payer, List.of("finance.payment.register"),
                com.complex.platform.access.AccessRepository.Effect.GRANT, "定金退款实付测试授权", payer);
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin(payer);
        String itemId = barLedger.createDepositRefundPayable(reservation);
        assertThat(barLedger.hasDepositRefundPayable("resv_dps")).isTrue();
        assertThat(repository.findReservationById("resv_dps")).get()
                .extracting(BarReservationRecord::depositStatus).isEqualTo("paid");

        financeService.registerArApPayment(itemId, new com.complex.platform.finance.dto.ArApPaymentRequest(
                "dps-pay", "原路退回定金", List.of(), 20000L, "aggregate_pay", Instant.now(), "flow_dps", null));

        assertThat(repository.findReservationById("resv_dps")).get()
                .extracting(BarReservationRecord::depositStatus, BarReservationRecord::depositSettlement)
                .containsExactly("settled", "refunded_on_threshold");
    }

    @Test
    void splitAttributionPrimitivesListOrphanOrdersAndPersistManualAttribution() {
        // 第十批 TK-27-R3（BSM L1079「无法确认订单归属时，须先人工明确归属并留痕」）：真库原语——
        // 开台期间台位上没有 bar_order_service_sources 的订单被列为待归属；人工归属写入后即有会话归属。
        String merchant = closeFixture("spa");
        BarTableSessionRecord b = closeSession("sess_spa_b", merchant, "table_spa_b", 50000L, "open", null);
        Instant now = Instant.parse("2026-06-20T03:00:00Z");
        repository.saveOrder(new BarOrderRecord("order_spa_orphan", merchant, merchant, "table_spa_b", null, null,
                "staff", "created", 30000L, "CNY", "web", null, null, now, now));
        repository.saveOrder(new BarOrderRecord("order_spa_old", merchant, merchant, "table_spa_b", null, null,
                "staff", "created", 30000L, "CNY", "web", null, null, now.minusSeconds(7200), now.minusSeconds(7200)));

        assertThat(repository.listOrdersWithoutSession(List.of("table_spa_b"), b.openedAt()))
                .extracting(BarOrderRecord::id).containsExactly("order_spa_orphan");
        assertThat(repository.findSessionIdByOrderId("order_spa_orphan")).isEmpty();

        repository.saveOrderServiceSource("order_spa_orphan", b.id());

        assertThat(repository.findSessionIdByOrderId("order_spa_orphan")).contains(b.id());
        assertThat(repository.listOrdersWithoutSession(List.of("table_spa_b"), b.openedAt())).isEmpty();
        assertThat(BarMinSpend.consumedCent(repository, b.id())).isEqualTo(0L); // 无明细，只验归属不改进度口径
    }
}
