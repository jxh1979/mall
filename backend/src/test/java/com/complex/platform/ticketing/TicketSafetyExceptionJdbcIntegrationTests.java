package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TicketSafetyEventRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
 * TK-05-R2/R3 通票安全例外（BSM:804「通票安全例外是：客人出现重大情绪不稳定、安全问题或其他必要问题时，由夜班值班经理及以上批准处理，
 * 须记录原因、批准人与当时情形。多次通票……不退现金，只恢复本次入场已消耗的 1 次使用机会……作为一次完整补偿使用机会……同一事件对该次份只恢复一次」
 * 「其他非多次通票仍可免除其通票费用，即使已核销也予以退款」；BSM:806 申请人不得审批自己的申请；DEC-R4-2026-08-04-08 夜班值班经理按当日排班；
 * 2026-10-21 操作员裁决补充第 5 条「只退出事客人那一人份的票款（按该票人均分摊额），同票其他人照常使用」）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketSafetyExceptionJdbcIntegrationTests {

    @Autowired private JdbcTicketingRepository repository;
    @Autowired private TicketSafetyExceptionService safety;
    @Autowired private JdbcTemplate jdbc;

    private final Instant now = Instant.now();
    private String key;
    private String project;
    private String entity;
    private String nightUser;
    private String requester;
    private String device;

    @BeforeEach
    void seed() {
        key = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_tk05_" + key;
        entity = "be_tk05_" + key;
        nightUser = "usr_tk05_night_" + key;
        requester = "usr_tk05_req_" + key;
        device = "dev_tk05_" + key;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', '安全例外综合体', 'active', now(), now())", project);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '游乐公司', 'project_company', 'active', now(), now())", entity);
        // 本项目当日已发布排班的夜班值班经理（此刻在班）。
        jdbc.update("INSERT INTO staff_profiles (id, project_id, name, user_id, status, registration_status) VALUES (?, ?, '夜班经理', ?, 'active', 'approved')",
                "sp_tk05_" + key, project, nightUser);
        jdbc.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id, night_duty_project_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())""", "shift_tk05_" + key, nightUser,
                java.sql.Date.valueOf(java.time.LocalDate.now()), Timestamp.from(now.minusSeconds(3600)),
                Timestamp.from(now.plusSeconds(3600)), project, project);
        jdbc.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now())""", device, device, device);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 单人单次通票已核销：夜班值班经理批准即免除票款整张退（已用权益与核销记录保留，未用权益冻结）。 */
    @Test
    void verifiedSingleUsePassRefundedAfterNightDutyApproval() {
        Pass f = pass(1, 1, 10_000L, null);
        String entry = enter(f, 1);

        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 1));
        assertThat(event.outcome()).isEqualTo("refund");
        assertThat(event.status()).isEqualTo("pending");
        assertThat(event.entryVerificationId()).isEqualTo(entry);

        TestSecurityActors.asPlatformAdmin(nightUser);
        TicketSafetyEventRecord approved = safety.approve(event.id(), approve("a1"));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.approverBasis()).isEqualTo("night_duty");
        assertThat(approved.approvedBy()).isEqualTo(nightUser);
        assertThat(approved.refundId()).isNotNull();

        Map<String, Object> refund = jdbc.queryForMap(
                "SELECT status, reason_code, refund_amount_cent, responsible_entity_id, requested_by, approved_by FROM ticket_refunds WHERE id = ?",
                approved.refundId());
        assertThat(refund).containsEntry("status", "approved").containsEntry("reason_code", "safety_exception")
                .containsEntry("refund_amount_cent", 10_000L).containsEntry("responsible_entity_id", entity)
                .containsEntry("requested_by", requester).containsEntry("approved_by", nightUser);
        assertThat(status("ticket_instances", f.parent())).isEqualTo("refunded");
        assertThat(entitlement(f.entry(1))).containsEntry("status", "active").containsEntry("used_times", 1);
        assertThat(status("issued_entitlements", f.drink(1))).isEqualTo("refunded");
        assertThat(status("ticket_verifications", entry)).isEqualTo("success");
    }

    /** 多人通票只退出事那一人份（该票人均分摊额，尾差按既有均分工具），父票与同票他人份照常可用；最高权限亦可批准。 */
    @Test
    void multiPersonPassRefundsOnlyThatPersonShare() {
        Pass f = pass(3, 1, 30_001L, null);
        enter(f, 1);
        enter(f, 2);

        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 2));
        asMaster();
        TicketSafetyEventRecord approved = safety.approve(event.id(), approve("a2"));

        assertThat(approved.approverBasis()).isEqualTo("master");
        assertThat(jdbc.queryForObject("SELECT refund_amount_cent FROM ticket_refunds WHERE id = ?", Long.class, approved.refundId()))
                .isEqualTo(10_000L);
        assertThat(status("ticket_instances", f.parent())).isEqualTo("active");
        assertThat(status("issued_entitlements", f.drink(2))).isEqualTo("refunded");
        assertThat(entitlement(f.entry(2))).containsEntry("status", "active").containsEntry("used_times", 1);
        assertThat(status("issued_entitlements", f.drink(1))).isEqualTo("active");
        assertThat(status("issued_entitlements", f.entry(3))).isEqualTo("active");
        assertThat(status("issued_entitlements", f.drink(3))).isEqualTo("active");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_refund_lines WHERE refund_id = ?", Integer.class, approved.refundId()))
                .isEqualTo(2);
    }

    /** 单一酒吧门票不是通票，不适用通票安全例外。 */
    @Test
    void singleBarTicketNotEligible() {
        String bar = "mch_tk05_" + key;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) VALUES (?, '单店酒吧', 'bar', 'active', now(), now())", bar);
        Pass f = pass(1, 1, 10_000L, bar);
        enter(f, 1);
        TestSecurityActors.asPlatformAdmin(requester);
        assertThatThrownBy(() -> safety.request(request(f, 1)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("SAFETY_EXCEPTION_PASS_ONLY");
    }

    /** 非本项目当前夜班值班经理及以上（也非最高权限）不能批准。 */
    @Test
    void approverNotOnNightDutyRejected() {
        Pass f = pass(1, 1, 10_000L, null);
        enter(f, 1);
        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 1));

        TestSecurityActors.asPlatformAdmin("usr_tk05_other_" + key);
        assertThatThrownBy(() -> safety.approve(event.id(), approve("a3")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("NIGHT_DUTY_MANAGER_REQUIRED");
        assertThat(status("ticket_safety_events", event.id())).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Integer.class, f.order())).isZero();
    }

    /** 申请人不得审批自己的申请（即使本人就是当班夜班值班经理）。 */
    @Test
    void requesterCannotApproveOwnSafetyEvent() {
        Pass f = pass(1, 1, 10_000L, null);
        enter(f, 1);
        TestSecurityActors.asPlatformAdmin(nightUser);
        TicketSafetyEventRecord event = safety.request(request(f, 1));
        assertThatThrownBy(() -> safety.approve(event.id(), approve("a4")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("SELF_APPROVAL_FORBIDDEN");
    }

    /**
     * 多次通票：不退现金、不回改原核销与已用次数；恢复本次 1 次作为完整补偿使用机会——原票下补发完整票面一份（入场 + 酒水），
     * 关联本次事件、次份与原入场；自发放之日起按补偿有效期（不受原票有效期截断）；新增履约由所选公司承担，补发行带补偿标记（使用不发奖励）。
     */
    @Test
    void multiUsePassRestoresOneFullFaceUseNoCash() {
        Pass f = pass(1, 3, 30_000L, null);
        enter(f, 1);
        String second = enter(f, 1);

        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 1));
        assertThat(event.outcome()).isEqualTo("restore");
        assertThat(event.entryVerificationId()).isEqualTo(second);
        TestSecurityActors.asPlatformAdmin(nightUser);
        TicketSafetyEventRecord approved = safety.approve(event.id(), approve("a5"));

        assertThat(approved.refundId()).isNull();
        assertThat(approved.compensationBatchId()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_refunds WHERE target_id = ?", Integer.class, f.order())).isZero();
        assertThat(entitlement(f.entry(1))).containsEntry("used_times", 2).containsEntry("status", "active");
        assertThat(status("ticket_instances", f.parent())).isEqualTo("active");
        Map<String, Object> batch = jdbc.queryForMap(
                "SELECT event_type, status, bearer_company_id FROM amusement_fault_compensation_batches WHERE id = ?",
                approved.compensationBatchId());
        assertThat(batch).containsEntry("event_type", "safety_restore").containsEntry("status", "issued")
                .containsEntry("bearer_company_id", entity);
        Map<String, Object> item = jdbc.queryForMap("""
                SELECT id, kind, status, entitlement_id, parent_ticket_id, slot_index, original_entry_verification_id
                  FROM amusement_fault_compensation_items WHERE safety_event_id = ?""", event.id());
        assertThat(item).containsEntry("kind", "full_reissue").containsEntry("status", "issued")
                .containsEntry("entitlement_id", f.entry(1)).containsEntry("parent_ticket_id", f.parent())
                .containsEntry("slot_index", 1).containsEntry("original_entry_verification_id", second);
        List<Map<String, Object>> reissued = jdbc.queryForList("""
                SELECT entitlement_type, total_times, used_times, valid_to, parent_ticket_id, slot_index
                  FROM issued_entitlements WHERE compensation_item_id = ? ORDER BY entitlement_type""", item.get("id"));
        assertThat(reissued).extracting(r -> r.get("entitlement_type")).containsExactly("drink", "entry");
        assertThat(reissued).allSatisfy(r -> {
            assertThat(r.get("total_times")).isEqualTo(1);
            assertThat(r.get("used_times")).isEqualTo(0);
            assertThat(r.get("parent_ticket_id")).isEqualTo(f.parent());
            assertThat(((Timestamp) r.get("valid_to")).toInstant()).isAfter(now.plus(300, ChronoUnit.DAYS));
        });
    }

    /** 同一事件对该次份只恢复一次：同一次入场不能再登记，已批准的事件不能再批。 */
    @Test
    void restoreOnlyOncePerEventShare() {
        Pass f = pass(1, 3, 30_000L, null);
        enter(f, 1);
        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 1));
        TestSecurityActors.asPlatformAdmin(nightUser);
        safety.approve(event.id(), approve("a6"));

        assertThatThrownBy(() -> safety.approve(event.id(), approve("a7")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("TICKET_SAFETY_EVENT_NOT_PENDING");
        TestSecurityActors.asPlatformAdmin(requester);
        assertThatThrownBy(() -> safety.request(request(f, 1)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo("TICKET_SAFETY_EVENT_DUPLICATE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_fault_compensation_items WHERE parent_ticket_id = ?",
                Integer.class, f.parent())).isEqualTo(1);
    }

    /**
     * PGM:429 夜班值班经理按项目每天一名、由排班指定，是项目员工而非平台账号：不挂商户的项目员工（发起人与在班夜班经理）
     * 可登记并批准客人自购通票（订单无受理商户）的安全事件。
     */
    @Test
    void projectStaffNightDutyManagerApprovesGuestSelfPurchasedPass() {
        Pass f = pass(1, 1, 10_000L, null);
        enter(f, 1);
        AtomicReference<TicketSafetyEventRecord> event = new AtomicReference<>();
        asProjectStaff(requester);
        assertThatCode(() -> event.set(safety.request(request(f, 1)))).doesNotThrowAnyException();
        AtomicReference<TicketSafetyEventRecord> approved = new AtomicReference<>();
        asProjectStaff(nightUser);
        assertThatCode(() -> approved.set(safety.approve(event.get().id(), approve("s1")))).doesNotThrowAnyException();
        assertThat(approved.get().approverBasis()).isEqualTo("night_duty");
        assertThat(safety.list()).extracting(TicketSafetyEventRecord::id).contains(event.get().id());
    }

    /** BSM:804「夜班值班经理及以上」：组织层级中位于本项目夜班职责排班人之上的人员可批准（依据记 superior）。 */
    @Test
    void superiorOfNightDutyManagerApproves() {
        Pass f = pass(1, 1, 10_000L, null);
        enter(f, 1);
        String boss = "usr_tk05_boss_" + key;
        for (String user : List.of(nightUser, boss)) {
            jdbc.update("""
                    INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)
                    VALUES (?, ?, 'staff', ?, NULL, 'active', now())""", user, user, project);
        }
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", nightUser, boss);
        TestSecurityActors.asPlatformAdmin(requester);
        TicketSafetyEventRecord event = safety.request(request(f, 1));
        AtomicReference<TicketSafetyEventRecord> approved = new AtomicReference<>();
        asProjectStaff(boss);
        assertThatCode(() -> approved.set(safety.approve(event.id(), approve("s2")))).doesNotThrowAnyException();
        assertThat(approved.get().approverBasis()).isEqualTo("superior");
    }

    // ---------- 造数 ----------

    private record Pass(String type, String order, String parent, int people) {
        String entry(int slot) { return "ent_e_" + parent + "_" + slot; }
        String drink(int slot) { return "ent_d_" + parent + "_" + slot; }
    }

    /** 一张通票（barId 为空）或单店票：每人份一条入场类权益（次数=times）与一条酒水权益；订单实付即该票分摊额。 */
    private Pass pass(int people, int times, long amountCent, String barId) {
        String type = "tt_tk05_" + key;
        String order = "o_tk05_" + key;
        String parent = "ti_tk05_" + key;
        repository.saveTicketType(new TicketTypeRecord(type, "安全例外通票", "weekday_weekend_holiday", amountCent, amountCent,
                amountCent, false, "published", 30, null, now, now, null, 0, null, false, barId, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String entryTpl = "tpl_e_" + key;
        String drinkTpl = "tpl_d_" + key;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(entryTpl, "入场", "entry", times, 0L, "active",
                null, null, null, null, now, now, null, null, null, null));
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(drinkTpl, "酒水", "drink", times, 0L, "active",
                null, null, null, null, now, now, null, null, null, null));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("tte_e_" + key, type, entryTpl, 1, times, 0L,
                null, people, null, null, now, now));
        repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord("tte_d_" + key, type, drinkTpl, 1, times, 0L,
                null, people, null, null, now, now));
        repository.saveTicketOrder(new TicketOrderRecord(order, "TO-" + key, null, type, null, 1,
                java.time.LocalDate.now(), "miniapp", "13900000000", amountCent, null, amountCent, 0L, "CNY", "paid", "issued",
                "created", now, now, null, now, barId, project));
        repository.saveTicketInstance(new TicketInstanceRecord(parent, "TN-" + key, order, type, 1, people, times, null, null,
                "active", null, null, now, now));
        Pass f = new Pass(type, order, parent, people);
        long perPerson = amountCent / people;
        for (int slot = 1; slot <= people; slot++) {
            repository.saveIssuedEntitlement(new IssuedEntitlementRecord(f.entry(slot), "CE-" + slot + "-" + key, type, order,
                    entryTpl, "entry", null, null, null, times, 0, 0L, "active", now, now.plusSeconds(86_400), now, now, 1,
                    perPerson * 6 / 10, null, null, null, null, parent, slot, barId, null));
            repository.saveIssuedEntitlement(new IssuedEntitlementRecord(f.drink(slot), "CD-" + slot + "-" + key, type, order,
                    drinkTpl, "drink", null, null, null, times, 0, 0L, "active", now, now.plusSeconds(86_400), now, now, 1,
                    perPerson - perPerson * 6 / 10, null, null, null, null, parent, slot, barId, null));
        }
        return f;
    }

    /** 该人份入场一次：入场权益扣 1 次并写一条成功核销。 */
    private String enter(Pass f, int slot) {
        String id = "v_tk05_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        jdbc.update("UPDATE issued_entitlements SET used_times = used_times + 1 WHERE id = ?", f.entry(slot));
        jdbc.update("""
                INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,
                    credential_payload, device_id, point_id, status, source_type, created_at, verified_people)
                VALUES (?, ?, ?, ?, 'qr', 'x', ?, 'gate', 'success', 'admin', ?, 1)""",
                id, "VN-" + id, f.entry(slot), "CE-" + slot + "-" + key, device,
                Timestamp.from(Instant.now()));
        return id;
    }

    private TicketSafetyEventRequest request(Pass f, int slot) {
        return new TicketSafetyEventRequest("tse-" + UUID.randomUUID(), "安全例外", List.of(), f.parent(), slot,
                "客人突发重大情绪不稳定", "场内与他人冲突，安保介入后离场", entity);
    }

    private StatusChangeRequest approve(String tag) {
        return new StatusChangeRequest("tse-" + tag + "-" + key, "approve", "核实属实，批准", List.of());
    }

    /** 本项目员工（不挂商户、非平台账号）。 */
    private void asProjectStaff(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, null, "active"), "token", List.of()));
    }

    private void asMaster() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                        null, null, "active"), "token", List.of()));
    }

    private String status(String table, String id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id = ?", String.class, id);
    }

    private Map<String, Object> entitlement(String id) {
        return jdbc.queryForMap("SELECT status, used_times FROM issued_entitlements WHERE id = ?", id);
    }
}
