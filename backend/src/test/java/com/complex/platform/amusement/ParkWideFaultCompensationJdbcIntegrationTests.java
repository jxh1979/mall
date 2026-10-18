package com.complex.platform.amusement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.dto.CompensationBatchConfirmRequest;
import com.complex.platform.amusement.dto.CompensationBatchInitiateRequest;
import com.complex.platform.amusement.dto.CompensationPolicyRequest;
import com.complex.platform.amusement.dto.ParkWideFaultConfirmRequest;
import com.complex.platform.amusement.dto.ParkWideFaultInitiateRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.EntitlementTemplateRecord;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.VerificationResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
 * CG-08b 全园故障补偿通票（ENUM「全园故障与补偿通票」）：补偿对象快照（已入场核销且本场次受影响 / 尚未核销但仍在有效期内）、
 * 仍在原票下办理（延长原核销权不增加次数 / 另补一次完整票面权益）、同一故障同一人份只补一次、批量发放前双岗位确认、
 * 补偿有效期参数（出厂默认一年，上下限须同时配置）、补偿机会用一次即用尽且履约锁定到补偿记录。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ParkWideFaultCompensationJdbcIntegrationTests {

    private static final String HEAD = AmusementParkWideFaultService.ROLE_HEAD;
    private static final String ENG = AmusementParkWideFaultService.ROLE_ENGINEERING_SAFETY;

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
    private String deviceCode;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
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
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 快照：已入场核销人份 → 另补完整票面；尚未核销且在有效期内 → 延长核销权；过期票、非游乐权益不进；同一故障只补一次；双岗位确认。 */
    @Test
    void snapshotThenSecondRoleConfirmIssuesCompensationOnce() {
        AmusementParkWideFaultRecord fault = activateFault();
        actAs(bothRolesUser);
        AmusementFaultCompensationBatchRecord batch = compensationService.initiateBatch(fault.id(),
                new CompensationBatchInitiateRequest("cg08b-init-" + suffix, HEAD, "供电故障，游乐园整体关闭", "ent_amusement_co"));
        assertThat(batch.status()).isEqualTo("pending_confirmation");
        assertThat(batch.bearerCompanyId()).isEqualTo("ent_amusement_co");
        assertThat(batch.validityDays()).isEqualTo(365);
        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "SELECT entitlement_id, kind, status, original_session_id, original_entry_verification_id, share_count"
                        + " FROM amusement_fault_compensation_items WHERE batch_id = ? ORDER BY kind", batch.id());
        assertThat(pending).hasSize(2);
        assertThat(pending).extracting(r -> r.get("entitlement_id") + ":" + r.get("kind"))
                .containsExactlyInAnyOrder(unverifiedEntitlementId + ":extend", verifiedEntitlementId + ":full_reissue");
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
        assertThat(issued.extendCount()).isEqualTo(1);
        assertThat(issued.reissueCount()).isEqualTo(1);
        Instant validUntil = issued.validUntil();
        assertThat(validUntil).isCloseTo(Instant.now().plus(365, ChronoUnit.DAYS), within(1, ChronoUnit.HOURS));

        // B：只延长原核销权到补偿有效期，不增加次数。
        Map<String, Object> b = jdbcTemplate.queryForMap("SELECT total_times, used_times, valid_to FROM issued_entitlements WHERE id = ?",
                unverifiedEntitlementId);
        assertThat(b.get("total_times")).isEqualTo(1);
        assertThat(b.get("used_times")).isEqualTo(0);
        assertThat(((Timestamp) b.get("valid_to")).toInstant()).isEqualTo(validUntil);
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
                new CompensationBatchInitiateRequest("cg08b-again-" + suffix, HEAD, "重复发起", "ent_amusement_co")))
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
                new CompensationBatchInitiateRequest("cg08b-init2-" + suffix, HEAD, "消防系统故障", "ent_amusement_co"));
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
                new CompensationBatchInitiateRequest("cg08b-init3-" + suffix, HEAD, "入口系统故障", "ent_amusement_co"));
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
        assertThat(compensationRepository.listUnverifiedAffectedShares("missing-" + suffix, projectId, now))
                .noneMatch(row -> unverifiedEntitlementId.equals(row.get("entitlement_id")));
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
        assertThat(compensationRepository.listUnverifiedAffectedShares("other-" + suffix, other, now))
                .noneMatch(row -> unverifiedEntitlementId.equals(row.get("entitlement_id")));
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
                null, null, null, null, now, now, "virtual", null, "per_use", null, false));
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
        jdbcTemplate.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)",
                deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
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
