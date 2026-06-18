package com.complex.platform.bar;

import static org.assertj.core.api.Assertions.assertThat;
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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
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

    @Test
    void migrationCreatesBarTablesAndSeedsPermissions() {
        assertThat(regclass("bar_tables")).isEqualTo("bar_tables");
        assertThat(regclass("bar_reservations")).isEqualTo("bar_reservations");
        assertThat(regclass("bar_table_guest_checkins")).isEqualTo("bar_table_guest_checkins");
        assertThat(regclass("bar_age_verifications")).isEqualTo("bar_age_verifications");
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
                "bar.table_session.renew"
        );
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
                now
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
}
