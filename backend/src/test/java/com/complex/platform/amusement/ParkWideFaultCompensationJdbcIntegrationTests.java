package com.complex.platform.amusement;

// 文件版本：V0.06

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.dto.CompensationBatchConfirmRequest;
import com.complex.platform.amusement.dto.CompensationBatchInitiateRequest;
import com.complex.platform.amusement.dto.CompensationPolicyRequest;
import com.complex.platform.amusement.dto.ParkWideFaultConfirmRequest;
import com.complex.platform.amusement.dto.ParkWideFaultInitiateRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.InternalPayableExecutionService;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.CompensationFulfillmentPort;
import com.complex.platform.ticketing.EntitlementTemplateRecord;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.dto.VerificationReverseRequest;
import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationResponse;
import com.complex.platform.settlement.SettlementSettledConsumer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-08b 全园故障补偿通票（ENUM「全园故障与补偿通票」）：补偿对象快照（只取故障时已入场核销且本场次受影响的人份；
 * 2026-10-21 操作员裁决补充第 3 条「没来的一律不延期」：故障期间未入场的有效未核票不建延期项）、
 * 仍在原票下另补一次完整票面权益（TK-10：按票面绑定份数与单次份次数）、同一故障同一人份只补一次、批量发放前双岗位确认、
 * 补偿有效期参数（出厂默认一年，上下限须同时配置）、补偿机会用一次即用尽且履约锁定到补偿记录。
 * FS-012：补偿新增实际履约逐笔记录并锁定履约时目录生效价与版本、按承担主体对实际提供方形成补偿应付、不进普通结算事实与赠饮应付、
 * 承担主体须为在用法律主体、冲正时对冲承担方应付。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
@AutoConfigureMockMvc
@RecordApplicationEvents
class ParkWideFaultCompensationJdbcIntegrationTests {

    private static final String HEAD = AmusementParkWideFaultService.ROLE_HEAD;
    private static final String ENG = AmusementParkWideFaultService.ROLE_ENGINEERING_SAFETY;
    /** 承担主体：游乐公司（V354 主体主数据种子，active）。 */
    private static final String BEARER = "be_amusement";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEvents events;
    @Autowired private SettlementSettledConsumer settlementSettledConsumer;
    @Autowired private com.complex.platform.settlement.JdbcSettlementTransactionRepository settlementTransactions;

    @Autowired private AmusementFaultCompensationService compensationService;
    @Autowired private AmusementParkWideFaultService faultService;
    @Autowired private AmusementService amusementService;
    @Autowired private TicketingRepository ticketingRepository;
    @Autowired private TicketingService ticketingService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AmusementBusinessDayService businessDays;
    @Autowired private AmusementSessionLifecycleService lifecycle;
    @Autowired private AmusementRepository amusementRepository;
    @Autowired private JdbcAmusementFaultCompensationRepository compensationRepository;
    @Autowired private com.complex.platform.governance.review.SupervisorReviewService reviews;
    @Autowired private FinanceService financeService;
    @Autowired private InternalPayableExecutionService internalPayableExecution;
    @Autowired private CompensationFulfillmentPort compensationFulfillmentPort;
    @Autowired private CustomerRepository customerRepository;

    private String suffix;
    private String projectId;
    private String amusementProjectId;
    private String sessionId;
    private String ticketTypeId;
    private String orderId;
    private String verifiedEntitlementId;   // A：已入场核销（本场次受影响）
    private String unverifiedEntitlementId; // B：尚未核销、仍在有效期内
    private String expiredEntitlementId;    // C：尚未核销但已过期
    private String drinkEntitlementId;      // D：非游乐权益
    private String entryVerificationId;
    private String headUser;
    private String engineeringUser;
    private String bothRolesUser;
    private String financeUser;             // 登记收付款（finance.payment.register）
    private String deviceCode;
    private String providerMerchantId;      // 补发权益的实际提供方商户
    private String drinkItemCode;           // 酒水权益的内部结算价目录条目

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate); // 补偿应付按付账日规则与日历推算到期日
        projectId = "prj_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', '补偿测试综合体', 'active', now(), now())", projectId);
        Instant now = Instant.now();
        amusementProjectId = "amp_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, created_at, updated_at)"
                + " VALUES (?, '补偿测试项目', 100, 'open', ?, ?)", amusementProjectId, Timestamp.from(now), Timestamp.from(now));
        AmusementBusinessDayTestFixture.bind(jdbcTemplate, amusementProjectId, projectId);
        AmusementBusinessDayTestFixture.approve(jdbcTemplate, businessDays, reviews, projectId,
                "Asia/Shanghai", 10, now.minus(3, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "point_cg08b_" + suffix, amusementProjectId);
        sessionId = "ses_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, ?, 100, 1, 'open', ?, ?)", sessionId, amusementProjectId,
                Timestamp.from(now.minus(30, ChronoUnit.MINUTES)), Timestamp.from(now.plus(60, ChronoUnit.MINUTES)),
                Timestamp.from(now), Timestamp.from(now));
        lifecycle.lockSessionWindow(amusementRepository.findSessionById(sessionId).orElseThrow());
        ticketTypeId = "tt_cg08b_" + suffix;
        ticketingRepository.saveTicketType(new TicketTypeRecord(ticketTypeId, "补偿测试通票", "flat", 8800L, 8800L, 8800L, false,
                "published", 1, 60, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String amusementTemplate = template("amusement_pass", 5500L);
        String drinkTemplate = template("bar_drink", 3300L);
        // FS-012：酒水权益挂内部结算价目录（BSM L750「由对应域主管公司决定采用内部结算价……与实际提供方结算」）。
        providerMerchantId = "mch_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, '补偿测试提供方', 'bar', 'active', now(), now())", providerMerchantId);
        drinkItemCode = catalog("drink", 1200L);
        jdbcTemplate.update("UPDATE entitlement_templates SET catalog_item_code=? WHERE id=?", drinkItemCode, drinkTemplate);
        orderId = "ord_cg08b_" + suffix;
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(orderId, "ON-" + suffix, "cus_cg08b_" + suffix, ticketTypeId, null, 1,
                LocalDate.now(), "web", null, 8800L, null, 8800L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        jdbcTemplate.update("UPDATE ticket_orders SET project_id=? WHERE id=?", projectId, orderId);
        String parentTicket = "tkt_cg08b_" + suffix;
        ticketingRepository.saveTicketInstance(new com.complex.platform.ticketing.TicketInstanceRecord(parentTicket, "T-" + suffix, orderId,
                ticketTypeId, 1, 3, 1, "cus_cg08b_" + suffix, "cus_cg08b_" + suffix, "active", null, null, now, now, null));
        verifiedEntitlementId = entitlement("a", amusementTemplate, "amusement_pass", 1, 1, now.minus(2, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS), parentTicket, 1);
        unverifiedEntitlementId = entitlement("b", amusementTemplate, "amusement_pass", 1, 0, now.minus(2, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS), parentTicket, 2);
        expiredEntitlementId = entitlement("c", amusementTemplate, "amusement_pass", 1, 0, now.minus(10, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS), parentTicket, 3);
        drinkEntitlementId = entitlement("d", drinkTemplate, "bar_drink", 1, 0, now.minus(2, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS), parentTicket, 1);
        entryVerificationId = "amv_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_verifications (id, project_id, session_id, credential_payload, device_id, staff_id,"
                + " verification_result, failure_reason, evidence_attachment_ids, created_at, verified_people, direction, entitlement_id)"
                + " VALUES (?, ?, ?, 'payload', 'dev', NULL, 'success', NULL, '[]'::jsonb, ?, 1, 'entry', ?)",
                entryVerificationId, amusementProjectId, sessionId, Timestamp.from(now.minus(20, ChronoUnit.MINUTES)), verifiedEntitlementId);
        lifecycle.recordEntryWindow(entryVerificationId, businessDays.requireWindow(amusementProjectId, now.minus(20, ChronoUnit.MINUTES)));
        headUser = user("head", List.of("amusement.park_fault.confirm_as_head", "amusement.park_fault.view",
                "amusement.compensation_policy.manage"));
        engineeringUser = user("eng", List.of("amusement.park_fault.confirm_as_engineering_safety", "amusement.park_fault.view"));
        bothRolesUser = user("both", List.of("amusement.park_fault.confirm_as_head",
                "amusement.park_fault.confirm_as_engineering_safety", "amusement.park_fault.view"));
        financeUser = user("fin", List.of("finance.payment.register"));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /**
     * 快照：已入场核销人份 → 另补完整票面；过期票、非游乐权益不进；同一故障只补一次；双岗位确认。
     * TK-09（2026-10-21 操作员裁决补充第 3 条「没来的一律不延期」，ENUM L148「故障发生时持有效未核票不作为保留资格或延期的理由」）：
     * 故障期间未入场、持有效未核票的人份（B）不进补偿、原票照原有效期使用。
     */
    @Test
    void snapshotThenSecondRoleConfirmIssuesCompensationOnce() {
        Instant unverifiedValidTo = ticketingRepository.findIssuedEntitlementById(unverifiedEntitlementId).orElseThrow().validTo();
        AmusementParkWideFaultRecord fault = activateFault();
        actAs(bothRolesUser);
        AmusementFaultCompensationBatchRecord batch = compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("cg08b-init-" + suffix, HEAD, "供电故障，游乐园整体关闭", BEARER));
        assertThat(batch.status()).isEqualTo("pending_confirmation");
        assertThat(batch.bearerCompanyId()).isEqualTo(BEARER);
        assertThat(batch.validityDays()).isEqualTo(365);
        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "SELECT entitlement_id, kind, status, original_session_id, original_entry_verification_id, share_count"
                        + " FROM amusement_fault_compensation_items WHERE batch_id = ? ORDER BY kind", batch.id());
        assertThat(pending).extracting(r -> r.get("entitlement_id") + ":" + r.get("kind"))
                .as("没来的一律不延期：只补故障时已入场的人份")
                .containsExactly(verifiedEntitlementId + ":full_reissue");
        Map<String, Object> reissue = pending.stream().filter(r -> "full_reissue".equals(r.get("kind"))).findFirst().orElseThrow();
        assertThat(reissue.get("original_session_id")).isEqualTo(sessionId);
        assertThat(reissue.get("original_entry_verification_id")).isEqualTo(entryVerificationId);
        assertThat(reissue.get("status")).isEqualTo("pending");
        assertThat(reissue.get("share_count")).isEqualTo(1);

        // 发起人不得自行确认（即便同时持两岗位权限、换岗位）；另一人但同岗位不得确认。
        assertThatThrownBy(() -> compensationService.confirmBatch(batch.id(),
                new CompensationBatchConfirmRequest("cg08b-same-" + suffix, ENG, "本人换岗位确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_SAME_PERSON_FORBIDDEN");
        actAs(headUser);
        assertThatThrownBy(() -> compensationService.confirmBatch(batch.id(),
                new CompensationBatchConfirmRequest("cg08b-role-" + suffix, HEAD, "另一负责人确认")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_ROLE_PAIR_REQUIRED");

        actAs(engineeringUser);
        AmusementFaultCompensationBatchRecord issued = compensationService.confirmBatch(batch.id(),
                new CompensationBatchConfirmRequest("cg08b-ok-" + suffix, ENG, "工程岗确认发放"));
        assertThat(issued.status()).isEqualTo("issued");
        assertThat(issued.confirmedBy()).isEqualTo(engineeringUser);
        assertThat(issued.reissueCount()).isEqualTo(1);
        Instant validUntil = issued.validUntil();
        assertThat(validUntil).isCloseTo(Instant.now().plus(365, ChronoUnit.DAYS), within(1, ChronoUnit.HOURS));

        // B：原票照原有效期使用，不延期、不增加次数。
        Map<String, Object> b = jdbcTemplate.queryForMap("SELECT total_times, used_times, valid_to FROM issued_entitlements WHERE id = ?",
                unverifiedEntitlementId);
        assertThat(b.get("total_times")).isEqualTo(1);
        assertThat(b.get("used_times")).isEqualTo(0);
        assertThat(((Timestamp) b.get("valid_to")).toInstant()).isCloseTo(unverifiedValidTo, within(1, ChronoUnit.MILLIS));
        // A：原行不回改；原票下补发完整票面权益（游乐 + 酒水各一次），标记补偿记录。
        Map<String, Object> a = jdbcTemplate.queryForMap("SELECT total_times, used_times, valid_to FROM issued_entitlements WHERE id = ?",
                verifiedEntitlementId);
        assertThat(a.get("used_times")).isEqualTo(1);
        assertThat(((Timestamp) a.get("valid_to")).toInstant()).isBefore(validUntil);
        String itemId = (String) jdbcTemplate.queryForMap("SELECT id FROM amusement_fault_compensation_items WHERE batch_id = ? AND kind = 'full_reissue'",
                batch.id()).get("id");
        List<Map<String, Object>> reissued = jdbcTemplate.queryForList("SELECT entitlement_type, total_times, used_times, valid_to,"
                + " order_id, parent_ticket_id, slot_index, status FROM issued_entitlements WHERE compensation_item_id = ? ORDER BY entitlement_type", itemId);
        assertThat(reissued).hasSize(2);
        assertThat(reissued).extracting(r -> (String) r.get("entitlement_type")).containsExactly("amusement_pass", "bar_drink");
        for (Map<String, Object> r : reissued) {
            assertThat(r.get("total_times")).isEqualTo(1);
            assertThat(r.get("used_times")).isEqualTo(0);
            assertThat(r.get("status")).isEqualTo("active");
            assertThat(r.get("order_id")).isEqualTo(orderId);
            assertThat(r.get("parent_ticket_id")).isEqualTo("tkt_cg08b_" + suffix);
            assertThat(r.get("slot_index")).isEqualTo(1);
            assertThat(((Timestamp) r.get("valid_to")).toInstant()).isEqualTo(validUntil);
        }
        assertThat(jdbcTemplate.queryForMap("SELECT status FROM amusement_fault_compensation_items WHERE id = ?", itemId))
                .containsEntry("status", "issued");
        // C（过期）与 D（非游乐）未进补偿。
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM amusement_fault_compensation_items WHERE entitlement_id IN (?, ?)",
                Integer.class, expiredEntitlementId, drinkEntitlementId)).isZero();
        // 同一故障下同一人份只补偿一次：再次发起无可补偿人份。
        actAs(headUser);
        assertThatThrownBy(() -> compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("cg08b-again-" + suffix, HEAD, "重复发起", BEARER)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("COMPENSATION_NO_ELIGIBLE_SHARES");
    }

    /** 补偿有效期参数：上下限须同时配置、值须在上下限之间；项目行覆盖出厂行并用于发放。 */
    @Test
    void policyRequiresBoundsAndProjectValueDrivesValidity() {
        actAs(headUser);
        assertThatThrownBy(() -> compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("cg08b-pol-1-" + suffix, 400, null, 730)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("cg08b-pol-2-" + suffix, 10, 30, 730)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        AmusementCompensationPolicyRecord saved = compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("cg08b-pol-3-" + suffix, 400, 30, 730));
        assertThat(saved.validityDays()).isEqualTo(400);
        assertThat(jdbcTemplate.queryForMap("SELECT validity_days, min_days, max_days FROM amusement_compensation_policies WHERE project_id = ?",
                projectId)).containsEntry("validity_days", 400).containsEntry("min_days", 30).containsEntry("max_days", 730);

        AmusementParkWideFaultRecord fault = activateFault();
        actAs(headUser);
        AmusementFaultCompensationBatchRecord batch = compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("cg08b-init2-" + suffix, HEAD, "消防系统故障", BEARER));
        assertThat(batch.validityDays()).isEqualTo(400);
        actAs(engineeringUser);
        AmusementFaultCompensationBatchRecord issued = compensationService.confirmBatch(batch.id(),
                new CompensationBatchConfirmRequest("cg08b-ok2-" + suffix, ENG, "确认"));
        assertThat(issued.validUntil()).isCloseTo(Instant.now().plus(400, ChronoUnit.DAYS), within(1, ChronoUnit.HOURS));
    }

    /** 补偿机会核销一次即用尽：履约（核销记录、时点、结算价）锁定到补偿记录；原核销不回改；再次核销被拒。 */
    @Test
    void reissuedEntitlementFulfillmentIsLockedToCompensationOnce() {
        AmusementParkWideFaultRecord fault = activateFault();
        actAs(headUser);
        AmusementFaultCompensationBatchRecord batch = compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("cg08b-init3-" + suffix, HEAD, "入口系统故障", BEARER));
        actAs(engineeringUser);
        compensationService.confirmBatch(batch.id(), new CompensationBatchConfirmRequest("cg08b-ok3-" + suffix, ENG, "确认"));
        String itemId = (String) jdbcTemplate.queryForMap("SELECT id FROM amusement_fault_compensation_items WHERE batch_id = ? AND kind = 'full_reissue'",
                batch.id()).get("id");
        String drinkCode = (String) jdbcTemplate.queryForMap("SELECT entitlement_code FROM issued_entitlements WHERE compensation_item_id = ?"
                + " AND entitlement_type = 'bar_drink'", itemId).get("entitlement_code");

        TestSecurityActors.asPlatformAdmin();
        seedDevice();
        VerificationResponse response = ticketingService.createVerification(new VerificationRequest(
                "cg08b-ver-" + suffix, "qr", drinkCode, deviceCode, "point_cg08b_" + suffix, Map.of()));
        assertThat(response.status()).isEqualTo("success");
        Map<String, Object> item = jdbcTemplate.queryForMap("SELECT status, fulfilled_verification_id, fulfilled_at,"
                + " fulfilled_settlement_price_cent FROM amusement_fault_compensation_items WHERE id = ?", itemId);
        assertThat(item.get("status")).isEqualTo("used");
        assertThat(item.get("fulfilled_verification_id")).isEqualTo(response.id());
        assertThat(item.get("fulfilled_at")).isNotNull();
        assertThat(item.get("fulfilled_settlement_price_cent")).isNotNull();
        // 原入场核销与原权益不回改。
        assertThat(jdbcTemplate.queryForMap("SELECT used_times FROM issued_entitlements WHERE id = ?", verifiedEntitlementId))
                .containsEntry("used_times", 1);
        // 补偿机会用一次后即用尽。
        assertThatThrownBy(() -> ticketingService.createVerification(new VerificationRequest(
                "cg08b-ver2-" + suffix, "qr", drinkCode, deviceCode, "point_cg08b_" + suffix, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("ENTITLEMENT_USED_UP");
    }

    @Test
    void activeFaultRejectsOnlyAmusementVerificationAndDoesNotConsume() {
        activateFault();
        actAsVerifier();
        seedDevice();
        String amusementCode = ticketingRepository.findIssuedEntitlementById(unverifiedEntitlementId).orElseThrow().entitlementCode();
        assertThatThrownBy(() -> ticketingService.createVerification(new VerificationRequest(
                "blocked-entry-" + suffix, "qr", amusementCode, deviceCode, "point_cg08b_" + suffix, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_VERIFICATION_SUSPENDED");
        assertThat(ticketingRepository.findIssuedEntitlementById(unverifiedEntitlementId).orElseThrow().usedTimes()).isZero();
        TestSecurityActors.asPlatformAdmin();
        String drinkCode = ticketingRepository.findIssuedEntitlementById(drinkEntitlementId).orElseThrow().entitlementCode();
        assertThat(ticketingService.createVerification(new VerificationRequest(
                "normal-drink-" + suffix, "qr", drinkCode, deviceCode, "point_cg08b_" + suffix, Map.of())).status())
                .isEqualTo("success");
    }

    @Test
    void directAmusementEntryAlsoStopsWithoutChangingCapacityOrEntitlement() {
        activateFault();
        actAsVerifier();
        String code = ticketingRepository.findIssuedEntitlementById(unverifiedEntitlementId).orElseThrow().entitlementCode();
        assertThatThrownBy(() -> amusementService.createVerification(new VerificationRequest(
                "direct-blocked-" + suffix, "qr", code, "dev", "point", Map.of("project_id", amusementProjectId, "session_id", sessionId))))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("PARK_WIDE_FAULT_VERIFICATION_SUSPENDED");
        assertThat(ticketingRepository.findIssuedEntitlementById(unverifiedEntitlementId).orElseThrow().usedTimes()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT used_capacity FROM amusement_sessions WHERE id=?", Integer.class, sessionId)).isEqualTo(1);
    }

    @Test
    void compensationPolicyAcceptsInclusiveBusinessBoundsAndRejectsLargerLimit() {
        actAs(headUser);
        assertThat(compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("bound-low-" + suffix, 1, 1, 3650)).validityDays()).isEqualTo(1);
        assertThat(compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("bound-high-" + suffix, 3650, 1, 3650)).validityDays()).isEqualTo(3650);
        assertThatThrownBy(() -> compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("bound-over-" + suffix, 3651, 1, 3651)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> compensationService.configurePolicy(projectId,
                new CompensationPolicyRequest("bound-hidden-over-" + suffix, 365, 1, 3651)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("VALIDATION_FAILED");
        assertThat(compensationService.resolvePolicy(projectId).validityDays()).isEqualTo(3650);
    }

    @Test
    void missingSourceComplexIsNeverIncludedInFaultCompensation() {
        jdbcTemplate.update("UPDATE ticket_orders SET project_id=NULL WHERE id=?", orderId);
        Instant now = Instant.now();
        assertThat(compensationRepository.listVerifiedAffectedShares("missing-" + suffix, projectId, now.minusSeconds(60), now))
                .noneMatch(row -> verifiedEntitlementId.equals(row.get("entitlement_id")));
    }

    @Test
    void compensationUsesLockedEntryComplexAfterTheConfiguredSpaceChanges() {
        String other = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        jdbcTemplate.update("UPDATE spaces SET project_id=? WHERE id=(SELECT space_id FROM amusement_projects WHERE id=?)",
                other, amusementProjectId);
        Instant now = Instant.now();
        assertThat(compensationRepository.listVerifiedAffectedShares("moved-" + suffix, projectId, now.minusSeconds(60), now))
                .anyMatch(row -> verifiedEntitlementId.equals(row.get("entitlement_id")));
    }

    @Test
    void anotherComplexCannotClaimThisOrdersCompensationShares() {
        String other = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        Instant now = Instant.now();
        assertThat(compensationRepository.listVerifiedAffectedShares("other-" + suffix, other, now.minusSeconds(60), now))
                .noneMatch(row -> verifiedEntitlementId.equals(row.get("entitlement_id")));
    }

    /**
     * FS-012-R1/R2（BSM L800、ENUM L148「本次完整补偿新增实际履约所用的内部结算价及其规则版本，按补偿实际履约时生效的版本计算并锁定到该笔
     * 补偿履约记录」；台账 02｜Q15；CG 第 39 条）：同一补偿记录下补发的各项权益每次实际履约逐笔记录，锁定履约时目录生效价与条目版本，
     * 由批次承担主体对实际提供方形成补偿应付（普通内部结算：BSM L1673/L1683 按财务指定付账日、次月出账日起的异议期与银行工作日推算并固化，
     * 不挂提供方账本）；同一请求重放不重复挂账；履约后目录改价不动已锁记录。
     */
    @Test
    void everyReissuedEntitlementFulfillmentIsRecordedWithLockedCatalogVersion() {
        String giftItemCode = addGiftDrinkBinding(800L);
        String itemId = issueFullReissue();
        VerificationResponse drink = verify("fs012-drink-" + suffix, compensationEntitlement(itemId, "bar_drink"));
        assertThat(verify("fs012-drink-" + suffix, compensationEntitlement(itemId, "bar_drink")).id()).isEqualTo(drink.id());
        String giftVersion = promote(giftItemCode, 900L);
        VerificationResponse gift = verify("fs012-gift-" + suffix, compensationEntitlement(itemId, "gift_drink"));
        promote(drinkItemCode, 1500L);

        List<Map<String, Object>> payables = jdbcTemplate.queryForList("SELECT id, direction, counterparty_id, merchant_id, amount_cent,"
                + " unpaid_amount_cent, status, due_date, accounting_period FROM ar_ap_items"
                + " WHERE source_type = 'compensation_fulfillment' AND project_id = ? ORDER BY amount_cent DESC", projectId);
        assertThat(payables).extracting(r -> ((Number) r.get("amount_cent")).longValue()).containsExactly(1200L, 900L);
        for (Map<String, Object> r : payables) {
            assertThat(r).containsEntry("direction", "payable").containsEntry("counterparty_id", providerMerchantId)
                    .containsEntry("merchant_id", null).containsEntry("status", "pending_payment")
                    .containsEntry("accounting_period", YearMonth.now().toString());
            assertThat(r.get("unpaid_amount_cent")).isEqualTo(r.get("amount_cent"));
            Map<String, Object> s = jdbcTemplate.queryForMap("SELECT route, project_id, visible_at, objection_deadline, scheduled_due_date"
                    + " FROM internal_payable_schedules WHERE ar_ap_item_id = ?", r.get("id"));
            assertThat(s).containsEntry("route", "compensation_fulfillment").containsEntry("project_id", projectId);
            assertThat(r.get("due_date")).isEqualTo(s.get("scheduled_due_date"));
            assertThat(((Timestamp) s.get("visible_at")).toInstant()).as("次月出账后才可查看").isAfter(Instant.now());
            assertThat(((java.sql.Date) s.get("scheduled_due_date")).toLocalDate())
                    .as("付账日不早于异议期届满").isAfterOrEqualTo(((java.sql.Date) s.get("objection_deadline")).toLocalDate());
        }
        assertThat(jdbcTemplate.queryForObject("SELECT settlement_price_cent FROM ticket_verifications WHERE id = ?", Long.class,
                drink.id())).isEqualTo(1200L);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT verification_id, batch_id, bearer_company_id, provider_merchant_id,"
                + " settlement_price_cent, price_catalog_entry_id, payable_item_id, project_id FROM amusement_compensation_fulfillments"
                + " WHERE item_id = ? ORDER BY settlement_price_cent DESC", itemId);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("verification_id", drink.id()).containsEntry("settlement_price_cent", 1200L)
                .containsEntry("price_catalog_entry_id", drinkItemCode + "_v1").containsEntry("payable_item_id", payables.get(0).get("id"));
        assertThat(rows.get(1)).containsEntry("verification_id", gift.id()).containsEntry("settlement_price_cent", 900L)
                .containsEntry("price_catalog_entry_id", giftVersion).containsEntry("payable_item_id", payables.get(1).get("id"));
        for (Map<String, Object> r : rows) {
            assertThat(r).containsEntry("bearer_company_id", BEARER).containsEntry("provider_merchant_id", providerMerchantId)
                    .containsEntry("project_id", projectId);
        }
    }

    /** FS-012-R2（BSM L751「商户让利的零元免费权益不结算」）：履约时目录价为 0 → 只留补偿履约记录，不挂补偿应付。 */
    @Test
    void zeroPricedConcessionCompensationPostsNoPayable() {
        String zeroVersion = promote(drinkItemCode, 0L);
        String itemId = issueFullReissue();
        VerificationResponse drink = verify("fs012-zero-" + suffix, compensationEntitlement(itemId, "bar_drink"));
        assertThat(jdbcTemplate.queryForObject("SELECT fulfilled_settlement_price_cent FROM amusement_fault_compensation_items WHERE id = ?",
                Long.class, itemId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'compensation_fulfillment'"
                + " AND project_id = ?", Integer.class, projectId)).isZero();
        assertThat(jdbcTemplate.queryForMap("SELECT verification_id, settlement_price_cent, price_catalog_entry_id, payable_item_id"
                + " FROM amusement_compensation_fulfillments WHERE item_id = ?", itemId))
                .containsEntry("verification_id", drink.id()).containsEntry("settlement_price_cent", 0L)
                .containsEntry("price_catalog_entry_id", zeroVersion).containsEntry("payable_item_id", null);
    }

    /**
     * FS-012-R3（BSM L800「补偿履约与原履约分别记录，原核销和原结算不回改」；CG 第 39 条「防止重复记原票分账」）：
     * 补偿补发权益核销不写普通核销结算事实；普通核销照写。结算消费者是提交后另开事务，本类测试事务不提交，
     * 故把两次核销实际发布的事件交给消费者本体（绕过事务代理）在本事务内落库。按核销权威事实补建缺失结算（AUD-017）也不把补偿核销补回去。
     */
    @Test
    void compensationVerificationWritesNoOrdinarySettlementFact() {
        String itemId = issueFullReissue();
        VerificationResponse compensation = verify("fs012-st-comp-" + suffix, compensationEntitlement(itemId, "bar_drink"));
        VerificationResponse ordinary = verify("fs012-st-normal-" + suffix, drinkEntitlementId);
        SettlementSettledConsumer consumer = AopTestUtils.getUltimateTargetObject(settlementSettledConsumer);
        events.stream(SettlementSettledEvent.class).forEach(consumer::onSettlementSettled);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM settlement_transactions WHERE verification_id = ?", Integer.class,
                compensation.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM settlement_transactions WHERE verification_id = ?", Integer.class,
                ordinary.id())).isEqualTo(1);
        assertThat(settlementTransactions.findMissingVerificationSettlementFacts(Integer.MAX_VALUE))
                .noneMatch(fact -> compensation.id().equals(fact.verificationId()));
    }

    /** FS-012-R3：补偿赠饮照常出 0 元赠饮单，但不再挂「平台→酒吧」赠饮应付，改由承担主体对酒吧挂补偿应付。 */
    @Test
    void compensationGiftDrinkPostsNoPassDrinkPayable() {
        addGiftDrinkBinding(800L);
        String itemId = issueFullReissue();
        VerificationResponse gift = verify("fs012-gift-only-" + suffix, compensationEntitlement(itemId, "gift_drink"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'pass_drink_settlement'"
                + " AND source_id = ?", Integer.class, gift.id())).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM bar_gift_redemptions WHERE verification_id = ?", Integer.class,
                gift.id())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = 'compensation_fulfillment'"
                + " AND project_id = ? AND amount_cent = 800", Integer.class, projectId)).isEqualTo(1);
    }

    /** FS-012-R5（CG 第 39 条：形成的账款须与实际履约一致）：补偿核销被冲正 → 按 V344 范式追加严格取反的冲正行，承担方应付净额归零。 */
    @Test
    void reversingCompensationVerificationReversesBearerPayable() {
        String itemId = issueFullReissue();
        VerificationResponse drink = verify("fs012-rev-" + suffix, compensationEntitlement(itemId, "bar_drink"));
        ticketingService.reverseVerification(drink.id(), new VerificationReverseRequest("fs012-rev-do-" + suffix, "误核销冲正"),
                "staff_fs012");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, amount_cent, paid_amount_cent, unpaid_amount_cent, status,"
                + " origin_item_id FROM ar_ap_items WHERE source_type = 'compensation_fulfillment' AND project_id = ?"
                + " ORDER BY amount_cent DESC", projectId);
        assertThat(rows).extracting(r -> ((Number) r.get("amount_cent")).longValue()).containsExactly(1200L, -1200L);
        assertThat(rows.get(1)).containsEntry("origin_item_id", rows.get(0).get("id")).containsEntry("status", "reversed")
                .containsEntry("unpaid_amount_cent", -1200L).containsEntry("paid_amount_cent", 0L);
        assertThat(rows.get(0)).containsEntry("status", "pending_payment").containsEntry("origin_item_id", null);
        Map<String, Object> fulfillment = jdbcTemplate.queryForMap("SELECT reversed_at, reversal_item_id FROM amusement_compensation_fulfillments"
                + " WHERE verification_id = ?", drink.id());
        assertThat(fulfillment.get("reversed_at")).isNotNull();
        assertThat(fulfillment.get("reversal_item_id")).isEqualTo(rows.get(1).get("id"));
        // 操作员裁决 2026-10-19 第二轮第 5 条：全部未付时只冲正，不挂追收（上面 containsExactly 已断言无重述行）。
        assertThat(recoveryRows()).as("全部未付撤销不挂追收").isEmpty();
        // 冲正后原行不再是有效款项：承担方财务不得再对它登记已发起付款（与登记收付同码 409 AR_AP_ITEM_REVERSED），钱不会先汇出再被拦。
        actAs(financeUser);
        assertThatThrownBy(() -> internalPayableExecution.initiate((String) rows.get(0).get("id"),
                new InternalPayableExecutionService.InitiateRequest("fs012-rev-pay-" + suffix, "BANK-REV-" + suffix, null)))
                .isInstanceOf(ApiException.class).extracting(e -> ((ApiException) e).code()).isEqualTo("AR_AP_ITEM_REVERSED");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM internal_payable_payment_attempts WHERE ar_ap_item_id = ?",
                Integer.class, rows.get(0).get("id"))).isZero();
    }

    /**
     * 操作员裁决 2026-10-19 第二轮第 5 条「已付部分另挂追收」（比照门票分账裁决：冲正只取消未付部分；已付出的部分另挂一笔对提供方的追收应收，
     * 单独收回）：补偿应付 1200 已付 500 后该次核销被撤销 → 原行不动；V344 冲正行严格取反、重述行承接已付部分（三行净额＝已付 500、未付归零）；
     * 已付 500 对提供方另挂追收应收（source_type=compensation_fulfillment_recovery，幂等键取该次冲正 id），到期日按该钱路付账日规则推算并固化。
     */
    @Test
    void partiallyPaidCompensationPayableReversalRecoversPaidPartFromProvider() {
        String verificationId = reverseAfterPartialPayment(500L);

        List<Map<String, Object>> rows = compensationRows();
        assertThat(rows).extracting(r -> ((Number) r.get("amount_cent")).longValue()).containsExactly(1200L, 500L, -1200L);
        Map<String, Object> origin = rows.get(0);
        assertThat(origin).containsEntry("status", "partial_paid").containsEntry("paid_amount_cent", 500L)
                .containsEntry("unpaid_amount_cent", 700L).containsEntry("origin_item_id", null);
        assertThat(rows.get(1)).as("重述行承接已付部分").containsEntry("paid_amount_cent", 500L).containsEntry("unpaid_amount_cent", 0L)
                .containsEntry("status", "settled").containsEntry("origin_item_id", origin.get("id"));
        Map<String, Object> reversal = rows.get(2);
        assertThat(reversal).as("冲正行严格取反").containsEntry("paid_amount_cent", -500L).containsEntry("unpaid_amount_cent", -700L)
                .containsEntry("status", "reversed").containsEntry("origin_item_id", origin.get("id"));
        assertThat(jdbcTemplate.queryForMap("SELECT reversed_at, reversal_item_id FROM amusement_compensation_fulfillments"
                + " WHERE verification_id = ?", verificationId)).containsEntry("reversal_item_id", reversal.get("id"));

        List<Map<String, Object>> recoveries = recoveryRows();
        assertThat(recoveries).hasSize(1);
        Map<String, Object> recovery = recoveries.get(0);
        assertThat(recovery).containsEntry("direction", "receivable").containsEntry("counterparty_id", providerMerchantId)
                .containsEntry("merchant_id", null).containsEntry("project_id", projectId).containsEntry("source_id", reversal.get("id"))
                .containsEntry("amount_cent", 500L).containsEntry("paid_amount_cent", 0L).containsEntry("unpaid_amount_cent", 500L)
                .containsEntry("status", "pending_collection").containsEntry("accounting_period", YearMonth.now().toString())
                .containsEntry("origin_item_id", null);
        Map<String, Object> s = jdbcTemplate.queryForMap("SELECT route, project_id, counterparty_id, objection_deadline, scheduled_due_date"
                + " FROM internal_payable_schedules WHERE ar_ap_item_id = ?", recovery.get("id"));
        assertThat(s).containsEntry("route", "compensation_fulfillment_recovery").containsEntry("project_id", projectId)
                .containsEntry("counterparty_id", providerMerchantId);
        assertThat(recovery.get("due_date")).isEqualTo(s.get("scheduled_due_date"));
        assertThat(((java.sql.Date) s.get("scheduled_due_date")).toLocalDate())
                .as("付账日不早于异议期届满").isAfterOrEqualTo(((java.sql.Date) s.get("objection_deadline")).toLocalDate());
    }

    /** 裁决第 5 条「单独收回」：追收应收按前端真实请求形状（蛇形键）经登记收款接口（统一回写点 settleArApItem）收回。 */
    @Test
    void compensationRecoveryIsCollectedThroughArApPaymentEndpoint() throws Exception {
        reverseAfterPartialPayment(500L);
        List<Map<String, Object>> recoveries = recoveryRows();
        assertThat(recoveries).hasSize(1);
        String recoveryId = (String) recoveries.get(0).get("id");
        AccessActor admin = new AccessActor(financeUser, "u_" + financeUser, "platform_admin", "org_1", null, "active");
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + recoveryId + "/payments")
                        .with(request -> {
                            request.addHeader("X-Terminal-Type", "web");
                            return authentication(new UsernamePasswordAuthenticationToken(admin, "token", List.of()))
                                    .postProcessRequest(request);
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"fs012-cpr-pay-" + suffix + "\",\"amount_cent\":500,"
                                + "\"payment_method\":\"bank_transfer\",\"bank_flow_ref\":\"BANK-CPR-" + suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.status").value("settled"))
                .andExpect(jsonPath("$.data.item.unpaid_amount_cent").value(0));
        assertThat(jdbcTemplate.queryForMap("SELECT paid_amount_cent, unpaid_amount_cent, status FROM ar_ap_items WHERE id = ?", recoveryId))
                .containsEntry("paid_amount_cent", 500L).containsEntry("unpaid_amount_cent", 0L).containsEntry("status", "settled");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id = ?", Integer.class, recoveryId))
                .isEqualTo(1);
    }

    /** 裁决第 5 条：同一次冲正重放不重复冲正、不双挂追收（幂等键取该次冲正 id）。 */
    @Test
    void replayedCompensationReversalDoesNotDoubleRecover() {
        String verificationId = reverseAfterPartialPayment(500L);
        compensationFulfillmentPort.reverseFulfillment(verificationId, Instant.now());
        compensationFulfillmentPort.reverseFulfillment(verificationId, Instant.now().plusSeconds(1));
        assertThat(compensationRows()).extracting(r -> ((Number) r.get("amount_cent")).longValue()).containsExactly(1200L, 500L, -1200L);
        assertThat(recoveryRows()).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_compensation_fulfillments WHERE verification_id = ?",
                Integer.class, verificationId)).isEqualTo(1);
    }

    /** 补发酒水核销形成补偿应付 1200 → 承担方先付 paidCent → 撤销该次核销；返回该次核销 id。 */
    private String reverseAfterPartialPayment(long paidCent) {
        String itemId = issueFullReissue();
        VerificationResponse drink = verify("fs012-part-" + suffix, compensationEntitlement(itemId, "bar_drink"));
        String payableId = jdbcTemplate.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'compensation_fulfillment'"
                + " AND project_id = ?", String.class, projectId);
        actAs(financeUser);
        financeService.registerArApPayment(payableId, new ArApPaymentRequest("fs012-part-pay-" + suffix, null, List.of(), paidCent,
                "bank_transfer", null, "BANK-CPF-" + suffix));
        TestSecurityActors.asPlatformAdmin();
        ticketingService.reverseVerification(drink.id(), new VerificationReverseRequest("fs012-part-rev-" + suffix, "误核销冲正"),
                "staff_fs012");
        return drink.id();
    }

    private List<Map<String, Object>> compensationRows() {
        return jdbcTemplate.queryForList("SELECT id, amount_cent, paid_amount_cent, unpaid_amount_cent, status, origin_item_id FROM ar_ap_items"
                + " WHERE source_type = 'compensation_fulfillment' AND project_id = ? ORDER BY amount_cent DESC", projectId);
    }

    private List<Map<String, Object>> recoveryRows() {
        return jdbcTemplate.queryForList("SELECT id, direction, counterparty_id, merchant_id, project_id, source_id, amount_cent,"
                + " paid_amount_cent, unpaid_amount_cent, status, due_date, accounting_period, origin_item_id FROM ar_ap_items"
                + " WHERE source_type = 'compensation_fulfillment_recovery' AND project_id = ?", projectId);
    }

    /** FS-012-R4（ENUM L148「发放记录须明确该公司的具体法律主体」）：承担主体须为主体主数据中在用的法律主体。 */
    @Test
    void initiateBatchRejectsUnknownBearerEntity() {
        AmusementParkWideFaultRecord fault = activateFault();
        actAs(headUser);
        assertThatThrownBy(() -> compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("fs012-bearer-" + suffix, HEAD, "供电故障", "ent_not_exist")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("COMPENSATION_BEARER_ENTITY_INVALID");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_fault_compensation_batches WHERE fault_id = ?",
                Integer.class, fault.id())).isZero();
    }

    /** FS-012-R4：按前端真实请求形状（adminClient 把键转蛇形）发起补偿批次，承担主体为主体主数据编号。 */
    @Test
    void initiateBatchAcceptsFrontendSnakeCaseBody() throws Exception {
        AmusementParkWideFaultRecord fault = activateFault();
        AccessActor head = new AccessActor(headUser, "u_" + headUser, "platform_admin", "org_1", null, "active");
        mockMvc.perform(post("/api/v1/amusement/park-wide-faults/" + fault.id() + "/compensation-batches")
                        .with(request -> {
                            request.addHeader("X-Terminal-Type", "web");
                            return authentication(new UsernamePasswordAuthenticationToken(head, "token", List.of()))
                                    .postProcessRequest(request);
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"fs012-mvc-" + suffix + "\",\"role\":\"amusement_head\","
                                + "\"reason\":\"供电故障，游乐园整体关闭\",\"bearer_company_id\":\"be_amusement\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bearer_company_id").value(BEARER));
        assertThat(jdbcTemplate.queryForObject("SELECT bearer_company_id FROM amusement_fault_compensation_batches WHERE fault_id = ?",
                String.class, fault.id())).isEqualTo(BEARER);
    }

    /**
     * TK-10（ENUM L148「另补一次完整票面权益」「家庭票、多人票和多次票只对受影响的对应人份或次份办理补偿，不增加未受影响的剩余人数或次数」）：
     * 完整补偿按票面绑定的份数与次数补发受影响这一人份——酒水绑定 2 份×每份 2 次 → 补发 2 行各 2 次；
     * 多人票绑定（num_people=3）不按人数展开，只补受影响人份（原座次）1 行。
     */
    @Test
    void fullReissueHonoursBindingQuantityAndTimes() {
        jdbcTemplate.update("UPDATE ticket_type_entitlements SET quantity = 2, total_times = 2 WHERE id = ?",
                "bind_tpl_cg08b_bar_drink_" + suffix);
        jdbcTemplate.update("UPDATE ticket_type_entitlements SET num_people = 3 WHERE id = ?", "bind_tpl_cg08b_amusement_pass_" + suffix);
        String itemId = issueFullReissue();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT entitlement_type, total_times, slot_index FROM issued_entitlements"
                + " WHERE compensation_item_id = ? ORDER BY entitlement_type", itemId);
        assertThat(rows).extracting(r -> r.get("entitlement_type") + ":" + r.get("total_times") + ":" + r.get("slot_index"))
                .containsExactly("amusement_pass:1:1", "bar_drink:2:1", "bar_drink:2:1");
    }

    /**
     * TK-10（ENUM L46「多次票每完成一次检票环节消耗一次入场次数并开启一个独立场次窗口，该窗口内附带权益按完整一份配足」；
     * ENUM L148 只补受影响的对应次份）：3 次票只补 1 个次份——各绑定次数按单次份折算（3 次 ÷ 3 = 1）。
     */
    @Test
    void fullReissueForMultiUseTicketCoversOneShare() {
        jdbcTemplate.update("UPDATE ticket_type_entitlements SET total_times = 3 WHERE ticket_type_id = ?", ticketTypeId);
        jdbcTemplate.update("UPDATE issued_entitlements SET total_times = 3 WHERE id = ?", verifiedEntitlementId);
        String itemId = issueFullReissue();
        assertThat(jdbcTemplate.queryForList("SELECT total_times FROM issued_entitlements WHERE compensation_item_id = ?",
                Integer.class, itemId)).containsExactly(1, 1);
    }

    /**
     * TK-13（ENUM L148「“我的票”按对应人份或次份显示补偿标注及其实际有效期」；BSM L862）：客人按前端真实请求（客人 token、蛇形键响应）
     * 查看订单票——完整补发行带补偿标注 compensation_kind=full_reissue、所属人份座次，valid_to 即该人份实际补偿有效期；原票行不带标注。
     */
    @Test
    void customerOrderTicketsMarkCompensationPerShareWithActualValidity() throws Exception {
        String itemId = issueFullReissue();
        Instant validUntil = jdbcTemplate.queryForObject("SELECT new_valid_to FROM amusement_fault_compensation_items WHERE id = ?",
                Timestamp.class, itemId).toInstant();
        String customerId = "cus_cg08b_" + suffix;
        String phone = "139" + String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000_000));
        customerRepository.saveCustomer(new com.complex.platform.customer.CustomerRecord(customerId, phone,
                "139****" + phone.substring(7), "active", null, null, Instant.now(), Instant.now()));
        String token = "token_cg08b_" + suffix;
        customerRepository.saveSession(new CustomerSessionRecord("session_cg08b_" + suffix, customerId, token, "web",
                Instant.now(), Instant.now().plusSeconds(3600), "active"));
        TestSecurityActors.clear();
        String body = mockMvc.perform(get("/api/v1/customer/orders/" + orderId + "/tickets")
                        .header("X-Customer-Token", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> items = com.jayway.jsonpath.JsonPath.read(body, "$.data.items");
        assertThat(items).filteredOn(r -> "full_reissue".equals(r.get("compensation_kind")))
                .extracting(r -> r.get("entitlement_type") + ":" + r.get("slot_index") + ":" + Instant.parse((String) r.get("valid_to")))
                .containsExactlyInAnyOrder("amusement_pass:1:" + validUntil, "bar_drink:1:" + validUntil);
        assertThat(items).filteredOn(r -> r.get("compensation_kind") == null).extracting(r -> (String) r.get("id"))
                .containsExactlyInAnyOrder(verifiedEntitlementId, unverifiedEntitlementId, expiredEntitlementId, drinkEntitlementId);
    }

    // ---------- helpers ----------

    private void actAsVerifier() {
        String verifier = AmusementBusinessDayTestFixture.actor(jdbcTemplate, projectId,
                List.of("verification.execute", "amusement.verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(verifier, projectId);
    }

    private AmusementParkWideFaultRecord activateFault() {
        actAs(headUser);
        AmusementParkWideFaultRecord fault = faultService.initiate(new ParkWideFaultInitiateRequest(
                "cg08b-f-" + suffix + "-" + System.nanoTime(), projectId, HEAD, "供电故障，游乐园整体关闭"));
        actAs(engineeringUser);
        return faultService.confirmActivation(fault.id(), new ParkWideFaultConfirmRequest(
                "cg08b-f-ok-" + suffix + "-" + System.nanoTime(), ENG, "工程岗确认"));
    }

    private String template(String type, long valueCent) {
        Instant now = Instant.now();
        String id = "tpl_cg08b_" + type + "_" + suffix;
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(id, "权益-" + type, type, 1, valueCent, "active",
                null, null, null, null, now, now, "virtual", null, "per_use", null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("bind_" + id, ticketTypeId, id, 1, 1,
                valueCent, "default", 1, valueCent, now, now));
        return id;
    }

    private String entitlement(String key, String templateId, String type, int total, int used, Instant from, Instant to,
            String parentTicketId, int slot) {
        Instant now = Instant.now();
        String id = "ent_cg08b_" + key + "_" + suffix;
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(id, "CODE-" + key + "-" + suffix, ticketTypeId, orderId,
                templateId, type, "mch_cg08b_" + suffix, null, null, total, used, 5500L, "active", from, to, now, now, 1, 0L,
                "virtual", null, "per_use", null, parentTicketId, slot, null, null));
        return id;
    }

    private void seedDevice() {
        deviceCode = "device_cg08b_" + suffix;
        Timestamp ts = Timestamp.from(Instant.now());
        // 设备绑提供方商户：赠饮须在提供它的商户核销（S5 K16）。
        jdbcTemplate.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status,"
                + " merchant_id, created_at, updated_at) VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?, ?)",
                deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, providerMerchantId, ts, ts);
    }

    /** 负责人发起、工程岗确认全园故障补偿；返回已入场人份的完整补发补偿记录，随后以平台管理员在提供方设备上核销。 */
    private String issueFullReissue() {
        AmusementParkWideFaultRecord fault = activateFault();
        actAs(headUser);
        AmusementFaultCompensationBatchRecord batch = compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("fs012-init-" + suffix, HEAD, "供电故障，游乐园整体关闭", BEARER));
        actAs(engineeringUser);
        compensationService.confirmBatch(batch.id(), new CompensationBatchConfirmRequest("fs012-ok-" + suffix, ENG, "确认"));
        TestSecurityActors.asPlatformAdmin();
        seedDevice();
        return jdbcTemplate.queryForObject("SELECT id FROM amusement_fault_compensation_items WHERE batch_id = ? AND kind = 'full_reissue'",
                String.class, batch.id());
    }

    private String compensationEntitlement(String itemId, String type) {
        return jdbcTemplate.queryForObject("SELECT id FROM issued_entitlements WHERE compensation_item_id = ? AND entitlement_type = ?",
                String.class, itemId, type);
    }

    private VerificationResponse verify(String key, String entitlementId) {
        String code = jdbcTemplate.queryForObject("SELECT entitlement_code FROM issued_entitlements WHERE id = ?", String.class, entitlementId);
        return ticketingService.createVerification(new VerificationRequest(key, "qr", code, deviceCode, "point_cg08b_" + suffix, Map.of()));
    }

    /** 内部结算价目录生效条目（item_code 稳定，行 id＝价格版本）。 */
    private String catalog(String key, long priceCent) {
        String code = "sku_cg08b_" + key + "_" + suffix;
        catalogRow(code + "_v1", code, priceCent);
        return code;
    }

    /** 目录转正新价：旧生效行软替换为 inactive（留库即版本史），新行生效；返回新版本行 id。 */
    private String promote(String code, long priceCent) {
        jdbcTemplate.update("UPDATE internal_price_catalog SET status = 'inactive' WHERE item_code = ? AND is_draft = FALSE AND status = 'active'",
                code);
        String id = code + "_v" + System.nanoTime();
        catalogRow(id, code, priceCent);
        return id;
    }

    private void catalogRow(String id, String code, long priceCent) {
        jdbcTemplate.update("INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent, status,"
                + " is_draft, updated_by, created_at, updated_at) VALUES (?, ?, ?, 'beverage', '补偿测试条目', ?, 'active', FALSE,"
                + " 'seed_fs012', now(), now())", id, code, providerMerchantId, priceCent);
    }

    /** 票种追加一项平台出资型赠饮（提供方商品挂目录价、提供方有台位），完整补发时随票面一起补；返回目录条目编码。 */
    private String addGiftDrinkBinding(long priceCent) {
        String code = catalog("gift", priceCent);
        String productId = "barprod_cg08b_" + suffix;
        jdbcTemplate.update("INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency, inventory_item_id,"
                + " stock_location_id, stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code, created_at,"
                + " updated_at) VALUES (?, ?, '补偿赠饮', 'beverage', 3800, 'CNY', NULL, 'default', FALSE, 'active', ?, ?, now(), now())",
                productId, providerMerchantId, priceCent, code);
        jdbcTemplate.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, NULL, ?, '补偿赠饮台', 'standard', 4, 0, 0, 'CNY', 'active', now(), now())",
                "table_cg08b_" + suffix, providerMerchantId, "TC_cg08b_" + suffix);
        Instant now = Instant.now();
        String id = "tpl_cg08b_gift_" + suffix;
        ticketingRepository.saveEntitlementTemplate(new EntitlementTemplateRecord(id, "权益-赠饮", "gift_drink", 1, 0L, "active",
                null, null, null, productId, now, now, "physical", null, "per_use", null));
        ticketingRepository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("bind_" + id, ticketTypeId, id, 1, 1,
                0L, "default", 1, 0L, now, now));
        return code;
    }

    private String user(String key, List<String> permissions) {
        String userId = "usr_cg08b_" + key + "_" + suffix;
        String roleId = "role_cg08b_" + key + "_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_" + userId);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "全园故障补偿测试角色" + System.nanoTime());
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
