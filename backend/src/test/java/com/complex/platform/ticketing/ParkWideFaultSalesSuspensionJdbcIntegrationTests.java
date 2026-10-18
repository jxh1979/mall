package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.AmusementParkWideFaultRecord;
import com.complex.platform.amusement.AmusementParkWideFaultService;
import com.complex.platform.amusement.dto.ParkWideFaultConfirmRequest;
import com.complex.platform.amusement.dto.ParkWideFaultInitiateRequest;
import com.complex.platform.amusement.dto.ParkWideFaultLiftRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-08a 全园故障状态（ENUM「全园故障与补偿通票」：「全园故障状态由游乐公司负责人和工程或安全岗位共同确认后生效，一经生效，必须立即
 * 停止所有纯游乐门票和所有包含游乐权益的通票的新销售；全园故障状态的解除同样由游乐公司负责人和工程或安全岗位共同确认，解除后恢复
 * 上述票种的新销售；局部设备或单个项目故障，才允许在客人付款前明确提示后继续销售」）。
 *
 * <p>真实 Flyway 迁移库，{@code @Transactional} 回滚；发起/确认走 {@code AmusementParkWideFaultService}，售票闸走
 * {@code TicketOrderService.createOrder}；追 {@code amusement_park_wide_faults} 状态列与留痕表。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ParkWideFaultSalesSuspensionJdbcIntegrationTests {

    private static final String HEAD = AmusementParkWideFaultService.ROLE_HEAD;
    private static final String ENG = AmusementParkWideFaultService.ROLE_ENGINEERING_SAFETY;

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketOrderService orderService;
    @Autowired private AmusementParkWideFaultService faultService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String suffix;
    private String projectId;
    private String barId;
    private String amusementTicketTypeId;
    private String passTicketTypeId;
    private String barTicketTypeId;
    private String headUser;
    private String engineeringUser;
    private String bothRolesUser;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "prj_cg08_" + suffix;
        barId = "mch_cg08_bar_" + suffix;
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '全园故障测试综合体', 'active', now(), now())", projectId);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id)"
                + " VALUES (?, '全园故障测试酒吧', 'bar', 'active', now(), now(), ?)", barId, projectId);
        amusementTicketTypeId = ticketType("amu", null, List.of("amusement"));
        passTicketTypeId = ticketType("pass", null, List.of("amusement", "bar_drink"));
        barTicketTypeId = ticketType("bar", barId, List.of("bar_drink"));
        headUser = user("head", List.of("amusement.park_fault.confirm_as_head", "amusement.park_fault.view"));
        engineeringUser = user("eng", List.of("amusement.park_fault.confirm_as_engineering_safety", "amusement.park_fault.view"));
        bothRolesUser = user("both", List.of("amusement.park_fault.confirm_as_head",
                "amusement.park_fault.confirm_as_engineering_safety", "amusement.park_fault.view"));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 同一人两次确认被拒（即便其同时持两个岗位权限）；两个不同人但同一岗位被拒；记录仍停在待确认。 */
    @Test
    void activationRequiresTwoDifferentPeopleFromTwoRoles() {
        actAs(bothRolesUser);
        AmusementParkWideFaultRecord fault = faultService.initiate(new ParkWideFaultInitiateRequest(
                "cg08-init-" + suffix, projectId, HEAD, "供电故障，游乐园整体关闭"));
        assertThat(fault.status()).isEqualTo("pending_activation");

        assertThatThrownBy(() -> faultService.confirmActivation(fault.id(),
                new ParkWideFaultConfirmRequest("cg08-same-" + suffix, ENG, "本人换岗位再确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SAME_PERSON_FORBIDDEN");

        actAs(headUser);
        assertThatThrownBy(() -> faultService.confirmActivation(fault.id(),
                new ParkWideFaultConfirmRequest("cg08-samerole-" + suffix, HEAD, "另一位负责人确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_ROLE_PAIR_REQUIRED");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, activation_confirmed_by, activated_at FROM amusement_park_wide_faults WHERE id = ?", fault.id());
        assertThat(row.get("status")).isEqualTo("pending_activation");
        assertThat(row.get("activation_confirmed_by")).isNull();
        assertThat(row.get("activated_at")).isNull();
    }

    /** 生效后：纯游乐票与含游乐权益通票下单被拒、酒吧票不受影响；两岗位确认解除后恢复下单。 */
    @Test
    void activeFaultSuspendsAmusementSalesOnlyAndLiftRestores() {
        AmusementParkWideFaultRecord fault = activate();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, activation_confirmed_by, activation_confirmed_role, activated_at FROM amusement_park_wide_faults WHERE id = ?",
                fault.id());
        assertThat(row.get("status")).isEqualTo("active");
        assertThat(row.get("activation_confirmed_by")).isEqualTo(engineeringUser);
        assertThat(row.get("activation_confirmed_role")).isEqualTo(ENG);
        assertThat(row.get("activated_at")).isNotNull();

        assertThatThrownBy(() -> orderService.createOrder(order(amusementTicketTypeId, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SALES_SUSPENDED");
        assertThatThrownBy(() -> orderService.createOrder(order(passTicketTypeId, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SALES_SUSPENDED");
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id = ?", Integer.class,
                amusementTicketTypeId)).isZero();
        TicketOrderRecord barOrder = orderService.createOrder(order(barTicketTypeId, barId));
        assertThat(barOrder.id()).isNotBlank();

        actAs(engineeringUser);
        AmusementParkWideFaultRecord pendingLift = faultService.initiateLift(fault.id(),
                new ParkWideFaultLiftRequest("cg08-lift-" + suffix, ENG, "供电恢复，全部项目复检通过"));
        assertThat(pendingLift.status()).isEqualTo("pending_lift");
        assertThatThrownBy(() -> orderService.createOrder(order(amusementTicketTypeId, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SALES_SUSPENDED");

        actAs(headUser);
        AmusementParkWideFaultRecord lifted = faultService.confirmLift(fault.id(),
                new ParkWideFaultConfirmRequest("cg08-liftok-" + suffix, HEAD, "负责人确认解除"));
        assertThat(lifted.status()).isEqualTo("lifted");
        assertThat(jdbcTemplate.queryForMap("SELECT status, lift_confirmed_by, lifted_at FROM amusement_park_wide_faults WHERE id = ?",
                fault.id())).containsEntry("status", "lifted").containsEntry("lift_confirmed_by", headUser);

        TicketOrderRecord restored = orderService.createOrder(order(amusementTicketTypeId, null));
        assertThat(restored.id()).isNotBlank();

        assertThat(jdbcTemplate.queryForList("SELECT action, actor_id, actor_role FROM amusement_park_wide_fault_actions"
                + " WHERE fault_id = ? ORDER BY created_at, id", fault.id()))
                .extracting(r -> r.get("action") + ":" + r.get("actor_id") + ":" + r.get("actor_role"))
                .containsExactly("initiate:" + headUser + ":" + HEAD, "confirm_activation:" + engineeringUser + ":" + ENG,
                        "initiate_lift:" + engineeringUser + ":" + ENG, "confirm_lift:" + headUser + ":" + HEAD);
    }

    /** 解除同样双岗位：解除发起人不得自行确认（即便换岗位），两个不同人但同一岗位不得确认。 */
    @Test
    void liftRequiresTwoDifferentPeopleFromTwoRoles() {
        AmusementParkWideFaultRecord fault = activate();
        actAs(bothRolesUser);
        faultService.initiateLift(fault.id(), new ParkWideFaultLiftRequest("cg08-lift2-" + suffix, ENG, "设施恢复"));
        assertThatThrownBy(() -> faultService.confirmLift(fault.id(),
                new ParkWideFaultConfirmRequest("cg08-lift2-same-" + suffix, HEAD, "本人换岗位确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SAME_PERSON_FORBIDDEN");
        actAs(engineeringUser);
        assertThatThrownBy(() -> faultService.confirmLift(fault.id(),
                new ParkWideFaultConfirmRequest("cg08-lift2-role-" + suffix, ENG, "另一位工程岗确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_ROLE_PAIR_REQUIRED");
        assertThat(jdbcTemplate.queryForMap("SELECT status FROM amusement_park_wide_faults WHERE id = ?", fault.id()))
                .containsEntry("status", "pending_lift");
        assertThatThrownBy(() -> orderService.createOrder(order(passTicketTypeId, null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SALES_SUSPENDED");
    }

    @Test
    void faultStopsOldUnpaidIntentAndKeepsAlreadyPaidReplay() {
        TicketOrderRecord oldOrder = orderService.createOrder(order(amusementTicketTypeId, null));
        TicketOrderRecord paid = orderService.createOrder(order(passTicketTypeId, null));
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(paid.id(), payment(paid, "before"));
        orderService.confirmPayment(paid.id(), new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                "confirmed-" + suffix, intent.id(), null));
        activate();
        assertThatThrownBy(() -> orderService.createPaymentIntent(oldOrder.id(), payment(oldOrder, "after")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SALES_SUSPENDED");
        assertThat(orderService.confirmPayment(paid.id(),
                new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest("replay-" + suffix, intent.id(), null))
                .payStatus()).isEqualTo("paid");
        assertThat(repository.findTicketOrderById(oldOrder.id()).orElseThrow().payStatus()).isEqualTo("unpaid");
    }

    @Test
    void lateConfirmedPaymentCancelsUnacceptedOrderAndCreatesOneOpenOriginalRouteRefund() {
        String type = ticketType("barpass", barId, List.of("amusement", "bar_drink"));
        TicketOrderRecord oldOrder = orderService.createOrder(order(type, barId));
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(oldOrder.id(), payment(oldOrder, "late"));
        activate();
        var confirm = new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                "late-confirm-" + suffix, intent.id(), "gateway-original-" + suffix);
        TicketOrderRecord rejected = orderService.confirmPayment(oldOrder.id(), confirm);
        assertThat(rejected.status()).isEqualTo("cancelled");
        assertThat(rejected.payStatus()).isEqualTo("paid");
        assertThat(rejected.issueStatus()).isEqualTo("not_issued");
        assertThat(repository.findTicketPaymentIntentById(intent.id()).orElseThrow().status()).isEqualTo("succeeded");
        Map<String, Object> refund = jdbcTemplate.queryForMap("SELECT * FROM finance_refunds"
                + " WHERE source_type='ticket_payment_rejection' AND target_id=?", intent.id());
        assertThat(refund).containsEntry("status", "requested").containsEntry("refund_amount_cent", oldOrder.amountCent())
                .containsEntry("refund_channel", "wechat").containsEntry("refund_direction", "original")
                .containsEntry("original_payment_ref", "gateway-original-" + suffix).containsEntry("project_id", projectId);
        assertThat(refund.get("succeeded_at")).isNull();
        assertThat(repository.listIssuedEntitlementsByOrderId(oldOrder.id())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id=?", Integer.class,
                type)).isZero();
        assertThat(orderService.confirmPayment(oldOrder.id(), confirm).status()).isEqualTo("cancelled");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_refunds"
                + " WHERE source_type='ticket_payment_rejection' AND target_id=?", Integer.class, intent.id())).isEqualTo(1);
        assertThatThrownBy(() -> orderService.issueOrder(oldOrder.id(),
                new com.complex.platform.ticketing.dto.StatusChangeRequest("no-issue-" + suffix, null, "拒收单不得出票", List.of())))
                .isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void cancelledUnpaidOrderStillRecordsLateMoneyDuringFaultWithoutReleasingStockTwice() {
        String type = ticketType("cancelled-late", barId, List.of("amusement"));
        TicketOrderRecord order = orderService.createOrder(order(type, barId));
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(order.id(), payment(order, "cancelled-late"));
        orderService.cancelOrder(order.id());
        activate();
        TicketOrderRecord result = orderService.confirmPayment(order.id(),
                new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                        "cancelled-late-confirm-" + suffix, intent.id(), "gateway-cancelled-" + suffix));
        assertThat(result.status()).isEqualTo("cancelled");
        assertThat(result.payStatus()).isEqualTo("paid");
        assertThat(repository.listIssuedEntitlementsByOrderId(order.id())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_refunds WHERE source_type='ticket_payment_rejection' AND target_id=?",
                Integer.class, intent.id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id=?", Integer.class, type)).isZero();
    }

    @Test
    void everyLatePaymentOnRejectedOrderKeepsItsOwnRefundAfterFaultLift() {
        String type = ticketType("two-late-payments", barId, List.of("amusement"));
        TicketOrderRecord order = orderService.createOrder(order(type, barId));
        TicketPaymentIntentRecord first = orderService.createPaymentIntent(order.id(), payment(order, "first-late"));
        TicketPaymentIntentRecord second = orderService.createPaymentIntent(order.id(), payment(order, "second-late"));
        AmusementParkWideFaultRecord fault = activate();
        var firstConfirmation = new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                "first-late-confirm-" + suffix, first.id(), "gateway-first-" + suffix);
        assertThat(orderService.confirmPayment(order.id(), firstConfirmation).status()).isEqualTo("cancelled");

        actAs(engineeringUser);
        faultService.initiateLift(fault.id(), new ParkWideFaultLiftRequest(
                "two-pay-lift-" + suffix, ENG, "故障解除"));
        actAs(headUser);
        faultService.confirmLift(fault.id(), new ParkWideFaultConfirmRequest(
                "two-pay-lift-ok-" + suffix, HEAD, "确认解除"));
        var secondConfirmation = new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                "second-late-confirm-" + suffix, second.id(), "gateway-second-" + suffix);
        for (int n = 0; n < 2; n++) {
            assertThat(orderService.confirmPayment(order.id(), secondConfirmation).status()).isEqualTo("cancelled");
            assertThat(orderService.confirmPayment(order.id(), firstConfirmation).status()).isEqualTo("cancelled");
        }
        var refunds = jdbcTemplate.queryForList("SELECT target_id, original_payment_ref, refund_amount_cent, status"
                + " FROM finance_refunds WHERE source_type='ticket_payment_rejection' AND target_id IN (?,?)",
                first.id(), second.id());
        assertThat(refunds).hasSize(2);
        assertThat(refunds).extracting(row -> row.get("target_id")).containsExactlyInAnyOrder(first.id(), second.id());
        assertThat(refunds).extracting(row -> row.get("original_payment_ref"))
                .containsExactlyInAnyOrder("gateway-first-" + suffix, "gateway-second-" + suffix);
        assertThat(refunds).allSatisfy(row -> assertThat(row)
                .containsEntry("refund_amount_cent", order.amountCent()).containsEntry("status", "requested"));
        assertThat(repository.findTicketPaymentIntentById(second.id()).orElseThrow().status()).isEqualTo("succeeded");
        assertThat(repository.listIssuedEntitlementsByOrderId(order.id())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id=?",
                Integer.class, type)).isZero();
    }

    @Test
    void amusementSaleWithoutTrustedProjectStopsBeforeStockOrPayment() {
        String type = ticketType("unresolved-project", null, List.of("amusement"));
        jdbcTemplate.update("UPDATE entitlement_templates SET merchant_id=NULL, point_id=NULL WHERE id IN"
                + " (SELECT entitlement_template_id FROM ticket_type_entitlements WHERE ticket_type_id=?)", type);
        assertThatThrownBy(() -> orderService.createOrder(order(type, null)))
                .isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("TICKET_PROJECT_UNRESOLVED");
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id=?", Integer.class, type)).isZero();
    }

    @Test
    void saleProjectSnapshotSurvivesMerchantMoveAndControlsLatePaymentRefund() {
        String type = ticketType("project-snapshot", null, List.of("amusement"));
        jdbcTemplate.update("UPDATE entitlement_templates SET merchant_id=? WHERE id IN"
                + " (SELECT entitlement_template_id FROM ticket_type_entitlements WHERE ticket_type_id=?)", barId, type);
        TicketOrderRecord order = orderService.createOrder(order(type, null));
        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM ticket_orders WHERE id=?", String.class, order.id()))
                .isEqualTo(projectId);
        TicketPaymentIntentRecord intent = orderService.createPaymentIntent(order.id(), payment(order, "project-snapshot"));
        String other = "prj_moved_" + suffix;
        jdbcTemplate.update("INSERT INTO projects (id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '迁往综合体', 'active', now(), now())", other);
        jdbcTemplate.update("UPDATE merchants SET project_id=? WHERE id=?", other, barId);
        activate();
        TicketOrderRecord rejected = orderService.confirmPayment(order.id(),
                new com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest(
                        "snapshot-confirm-" + suffix, intent.id(), "gateway-snapshot-" + suffix));
        assertThat(rejected.status()).isEqualTo("cancelled");
        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM ticket_orders WHERE id=?", String.class, order.id()))
                .isEqualTo(projectId);
        assertThat(jdbcTemplate.queryForObject("SELECT project_id FROM finance_refunds WHERE source_type='ticket_payment_rejection' AND target_id=?",
                String.class, intent.id())).isEqualTo(projectId);
    }

    @Test
    void amusementSaleWithConflictingProjectCandidatesIsRejected() {
        String type = ticketType("conflicting-project", null, List.of("amusement"));
        String other = "prj_conflict_" + suffix;
        jdbcTemplate.update("INSERT INTO projects (id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '冲突综合体', 'active', now(), now())", other);
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id,project_id) VALUES (?,?)", barId, other);
        assertThat(repository.listTicketTypeProjectIds(type)).containsExactlyInAnyOrder(projectId, other);
        assertThatThrownBy(() -> orderService.createOrder(order(type, null)))
                .isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("TICKET_PROJECT_UNRESOLVED");
        assertThat(jdbcTemplate.queryForObject("SELECT sold_quantity FROM ticket_types WHERE id=?", Integer.class, type)).isZero();
    }

    @Test
    void amusementSaleCanResolveProjectThroughItsConfiguredPoint() {
        String type = ticketType("point-project", null, List.of("amusement"));
        String point = "point_sale_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_projects (id,project_name,capacity,open_status,eligibility_rule_json,"
                + "location_point_id,created_at,updated_at,merchant_id) VALUES (?, '售票点位归属', 20, 'open', '{}'::jsonb, ?, now(), now(), ?)",
                "amu_sale_" + suffix, point, barId);
        jdbcTemplate.update("UPDATE entitlement_templates SET merchant_id=NULL, point_id=? WHERE id IN"
                + " (SELECT entitlement_template_id FROM ticket_type_entitlements WHERE ticket_type_id=?)", point, type);
        TicketOrderRecord order = orderService.createOrder(order(type, null));
        assertThat(order.projectId()).isEqualTo(projectId);
        assertThat(order.withMaskedPhone().projectId()).isEqualTo(projectId);
        assertThat(repository.findTicketOrderById(order.id()).orElseThrow().projectId()).isEqualTo(projectId);
    }

    private com.complex.platform.finance.dto.PaymentIntentRequest payment(TicketOrderRecord order, String key) {
        return new com.complex.platform.finance.dto.PaymentIntentRequest("pay-" + key + "-" + suffix, null, List.of(),
                "ticket_order", order.id(), order.amountCent(), "CNY", "wechat");
    }

    // ---------- helpers ----------

    private AmusementParkWideFaultRecord activate() {
        actAs(headUser);
        AmusementParkWideFaultRecord fault = faultService.initiate(new ParkWideFaultInitiateRequest(
                "cg08-act-" + suffix, projectId, HEAD, "消防系统故障，游乐园整体关闭"));
        actAs(engineeringUser);
        return faultService.confirmActivation(fault.id(), new ParkWideFaultConfirmRequest("cg08-act-ok-" + suffix, ENG, "工程岗确认"));
    }

    private TicketOrderRequest order(String ticketTypeId, String orderBarId) {
        return new TicketOrderRequest("cg08-ord-" + UUID.randomUUID(), null, List.of(), ticketTypeId, null, 1,
                LocalDate.now(), "admin", null, null, Map.of(), orderBarId, List.of(), null);
    }

    private String ticketType(String key, String ticketBarId, List<String> entitlementTypes) {
        Instant now = Instant.now();
        String id = "tt_cg08_" + key + "_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(id, "全园故障测试票-" + key, "flat", 5000L, 5000L, 5000L, false,
                "published", 1, 60, now, now, null, 0, null, false, ticketBarId, TicketTypeRecord.NO_COMBO_DISCOUNT));
        int i = 0;
        for (String type : entitlementTypes) {
            String templateId = "tpl_cg08_" + key + "_" + (i++) + "_" + suffix;
            repository.saveEntitlementTemplate(new EntitlementTemplateRecord(templateId, "权益-" + type, type, 1, 5000L, "active",
                    ticketBarId != null ? ticketBarId : barId, null, null, null, now, now, "virtual", null, "per_use", null, false));
            repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("bind_" + templateId, id, templateId, 1, 1,
                    5000L, "default", 1, 5000L, now, now));
        }
        return id;
    }

    private String user(String key, List<String> permissions) {
        String userId = "usr_cg08_" + key + "_" + suffix;
        String roleId = "role_cg08_" + key + "_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_" + userId);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "全园故障测试角色" + System.nanoTime());
        for (String code : permissions) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }

    private void actAs(String userId) {
        AccessActor actor = new AccessActor(userId, "u_" + userId, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
