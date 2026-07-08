package com.complex.platform.bar;

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
        "spring.datasource.password=complex_dev_pwd"
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
    private com.complex.platform.gate.AdmissionFactRepository admissionFactRepository;

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

        // 窗口内退款（10:00）计入 refund_cent；窗口外退款（03:00，归前一经营日）不计入；waiver 永不计入。
        saveBarAdjustment("refund_in", "bill_refund_window_jdbc", "refund", 5000L,
                Instant.parse("2026-06-18T10:00:00Z"));
        saveBarAdjustment("refund_out", "bill_refund_window_jdbc", "refund", 9999L,
                Instant.parse("2026-06-18T03:00:00Z"));
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
                "bar.table_session.renew"
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
                "deposit_intent_id", "deposit_paid_at", "deposit_settlement");
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
        ));
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
}
