package com.complex.platform.amusement;

// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.amusement.dto.AmusementIncidentCreateRequest;
import com.complex.platform.amusement.dto.CompensationBatchConfirmRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * CG-02 硬安全容量自动停运后自动恢复开放（BSM「容量分两道」：「因达到硬安全容量而自动停运的项目，在玩人数回落到硬安全容量以下后
 * 由系统自动恢复开放」；BSM「容量控制」/ ENUM「容量双阈值」同句）。
 *
 * <p>走真实 Flyway 迁移库（V443 amusement_projects.auto_paused_reason）。本类【非事务】：并发两路出场用例要让两条
 * exitVerification 各自真提交；每用例唯一后缀隔离，末尾按后缀清理（amusement_* 随项目级联删除；audit_events 只增不删）。
 * 入场/出场都走正路（createVerification / exitVerification，真实权益 + 容量快照），逐步追 amusement_projects.open_status、
 * auto_paused_reason 与 amusement_capacity_snapshot.occupied_count 列。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class AmusementHardCapacityAutoReopenJdbcIntegrationTests {

    @Autowired private AmusementService amusementService;
    @Autowired private AmusementController controller;
    @Autowired private AmusementSessionLifecycleController lifecycleController;
    @Autowired private AmusementCountReleaseController releaseController;
    @Autowired private AmusementSafetyService safetyService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AmusementBusinessDayService businessDays;
    @Autowired private AmusementSessionLifecycleService lifecycle;
    @Autowired private AmusementFaultCompensationService compensationService;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private AmusementRepository repository;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private com.complex.platform.governance.review.SupervisorReviewService reviews;

    private String suffix;
    private String projectId;
    private String sessionId;
    private String templateId;
    private String userId;
    private String roleId;
    private final List<String> extraReadProjects = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
        if (suffix == null) {
            return;
        }
        // 清场原件不可删除，相关随机测试交易保留为测试证据。
        if (jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE project_id=?", Integer.class, projectId)>0) return;
        // amusement_sessions / verifications / capacity_snapshot / inspections / incidents 均 ON DELETE CASCADE 到项目；
        // risk_alerts/risk_alert_actions 为 append-only（V140），随机 id 留痕不清。
        for (String extra : extraReadProjects) jdbcTemplate.update("DELETE FROM amusement_projects WHERE id = ?", extra);
        extraReadProjects.clear();
        jdbcTemplate.update("DELETE FROM amusement_projects WHERE id = ?", projectId);
        jdbcTemplate.update("DELETE FROM amusement_safety_templates WHERE id = ?", templateId);
        jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id IN (SELECT id FROM issued_entitlements WHERE order_id = ?)", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM issued_entitlements WHERE order_id = ?", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM ticket_instances WHERE order_id = ?", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_cg02_" + suffix);
        jdbcTemplate.update("DELETE FROM idempotency_records WHERE idempotency_key LIKE ?", "%" + suffix + "%");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", roleId);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", userId);
    }

    /** 入场到硬上限 → paused（原因 hard_capacity）；一人出场后在玩人数 < 硬上限 → 系统自动恢复 open 并留痕。 */
    @Test
    void hardCapacityPauseIsLiftedBySystemWhenInPlayDropsBelowLimit() {
        seed(1, 2);
        enter(1);
        assertThatThrownBy(() -> enter(2))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("SESSION_CAPACITY_EXHAUSTED"));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(inPlayCount()).isEqualTo(1);

        exit(1);

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "open")
                .containsEntry("auto_paused_reason", null);
        assertThat(autoReopenEvents()).isEqualTo(1);
    }

    /** 高危事故停运（safety_incident）不被出场翻回：事故停运不是硬超容。 */
    @Test
    void safetyIncidentPauseIsNotLiftedByExit() {
        seed(5, 1);
        enter(1);
        safetyService.createIncident(new AmusementIncidentCreateRequest(
                "inc_" + suffix, null, List.of(), projectId, projectId, sessionId, null, null,
                "high", "高危安全事故", "测试：高危事故自动停运", "usr_reporter", "staff", Instant.now()));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");

        exit(1);

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");
        assertThat(autoReopenEvents()).isEqualTo(0);
    }

    /** 硬超容停运期间发生高危事故 → 原因升级为 safety_incident，人数回落后也不自动恢复。 */
    @Test
    void safetyIncidentDuringHardCapacityPauseBlocksAutoReopen() {
        seed(1, 2);
        enter(1);
        assertThatThrownBy(() -> enter(2)).isInstanceOf(ApiException.class);
        assertThat(projectRow()).containsEntry("auto_paused_reason", "hard_capacity");
        safetyService.createIncident(new AmusementIncidentCreateRequest(
                "inc_" + suffix, null, List.of(), projectId, projectId, sessionId, null, null,
                "critical", "停运期间严重事故", "测试：硬超容停运期间发生事故", "usr_reporter", "staff", Instant.now()));
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");

        exit(1);

        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "safety_incident");
        assertThat(autoReopenEvents()).isEqualTo(0);
    }

    /** 人工 maintenance / closed / 人工 paused（无系统原因）均不被出场翻回。 */
    @Test
    void manualStatesAreNotLiftedByExit() {
        for (String manual : List.of("maintenance", "closed", "paused")) {
            seed(5, 1);
            enter(1);
            jdbcTemplate.update("UPDATE amusement_projects SET open_status = ?, auto_paused_reason = NULL WHERE id = ?",
                    manual, projectId);

            exit(1);

            assertThat(inPlayCount()).isEqualTo(0);
            assertThat(projectRow()).containsEntry("open_status", manual)
                    .containsEntry("auto_paused_reason", null);
            assertThat(autoReopenEvents()).isEqualTo(0);
            tearDown();
        }
    }

    /** 并发两路出场：两路都真实释放，但项目只翻一次 open、只留一条自动恢复痕。 */
    @Test
    void concurrentExitsReopenExactlyOnce() throws Exception {
        seed(2, 3);
        enter(1);
        enter(2);
        assertThatThrownBy(() -> enter(3)).isInstanceOf(ApiException.class);
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(inPlayCount()).isEqualTo(2);

        String actorUser = userId;
        runBoth(() -> { authenticate(actorUser); exit(1); return null; },
                () -> { authenticate(actorUser); exit(2); return null; });

        assertThat(inPlayCount()).isEqualTo(0);
        assertThat(projectRow()).containsEntry("open_status", "open")
                .containsEntry("auto_paused_reason", null);
        assertThat(autoReopenEvents()).isEqualTo(1);
    }

    @Test
    void sessionDisplayCapacityDoesNotDuplicateProjectHardLimit() {
        seed(5, 2);
        jdbcTemplate.update("UPDATE amusement_sessions SET capacity = 1 WHERE id = ?", sessionId);

        enter(1);
        enter(2);

        assertThat(inPlayCount()).isEqualTo(2);
        assertThat(projectRow()).containsEntry("open_status", "open");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM amusement_verifications WHERE session_id = ? AND direction = 'entry'",
                Integer.class, sessionId)).isEqualTo(2);
    }

    @Test
    void pointDisplayCapacityDoesNotDuplicateProjectHardLimit() {
        seed(5, 2);
        jdbcTemplate.update("""
                INSERT INTO amusement_capacity_config (id, project_id, location_point_id, max_concurrent,
                    enabled, created_at, updated_at)
                VALUES (?, ?, ?, 1, true, now(), now())
                """, "cap_" + suffix, projectId, "point_" + suffix);
        for (int n : List.of(1, 2)) {
            amusementService.createVerification(new VerificationRequest(
                    "point_in_" + n + "_" + suffix, "entitlement_code", code(n), null, null,
                    Map.of("project_id", projectId, "session_id", sessionId,
                            "location_point_id", "point_" + suffix, "staff_id", "usr_gate")));
        }

        assertThat(inPlayCount()).isEqualTo(2);
        assertThat(projectRow()).containsEntry("open_status", "open");
    }

    @Test
    void directAdmissionKeepsRealAssetCodeWithoutUsingItAsTicketTerminalId() {
        seed(5, 1);
        String deviceCode = "asset_code_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO amusement_devices (id, project_id, device_code, status,
                    safety_check_interval_hours, last_pass_inspection_at, created_at, updated_at)
                VALUES (?, ?, ?, 'active', 1, now(), now(), now())
                """, "asset_" + suffix, projectId, deviceCode);

        AmusementVerificationRecord admission = amusementService.createVerification(new VerificationRequest(
                "asset_entry_" + suffix, "entitlement_code", code(1), deviceCode, null,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "usr_gate")));

        assertThat(admission.deviceId()).isEqualTo(deviceCode);
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT device_id, amusement_verification_id FROM ticket_verifications
                 WHERE amusement_verification_id = ?
                """, admission.id())).containsEntry("device_id", null)
                .containsEntry("amusement_verification_id", admission.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE entitlement_code = ?", Integer.class, code(1)))
                .isEqualTo(1);
    }

    @Test
    void directAdmissionRejectsUnknownNonemptyDeviceWithoutConsumingAnything() {
        seed(5, 1);

        assertThatThrownBy(() -> amusementService.createVerification(new VerificationRequest(
                "unknown_device_" + suffix, "entitlement_code", code(1), "unknown_asset_" + suffix, null,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "usr_gate"))))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("DEVICE_NOT_REGISTERED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE entitlement_code = ?", Integer.class, code(1)))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COALESCE(sum(occupied_count),0) FROM amusement_capacity_snapshot
                 WHERE project_id = ? AND dimension = 'project'
                """, Integer.class, projectId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM amusement_verifications WHERE project_id = ?", Integer.class, projectId)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM ticket_verifications
                 WHERE entitlement_id IN (SELECT id FROM issued_entitlements WHERE order_id = ?)
                """, Integer.class, "order_cg02_" + suffix)).isZero();
    }

    @Test
    void hardCapacityRecoveryDoesNotReopenWithExpiredActiveDeviceInspection() {
        seed(1, 2);
        jdbcTemplate.update("""
                INSERT INTO amusement_devices (id, project_id, device_code, status,
                    safety_check_interval_hours, last_pass_inspection_at, created_at, updated_at)
                VALUES (?, ?, ?, 'active', 1, now(), now(), now())
                """, "dev_" + suffix, projectId, "device_" + suffix);
        enter(1);
        assertThatThrownBy(() -> enter(2)).isInstanceOf(ApiException.class);
        jdbcTemplate.update("UPDATE amusement_devices SET last_pass_inspection_at = now() - interval '2 hours' WHERE project_id = ?", projectId);

        exit(1);

        assertThat(inPlayCount()).isZero();
        assertThat(projectRow()).containsEntry("open_status", "paused")
                .containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(autoReopenEvents()).isZero();
    }

    @Test
    void hardCapacityRecoveryDoesNotReopenWhenLatestInspectionIsUnsubmitted() {
        seed(1, 2);
        enter(1);
        assertThatThrownBy(() -> enter(2)).isInstanceOf(ApiException.class);
        jdbcTemplate.update("UPDATE amusement_safety_inspections SET inspection_status = 'draft' WHERE project_id = ?", projectId);

        exit(1);

        assertThat(inPlayCount()).isZero();
        assertThat(projectRow()).containsEntry("open_status", "paused");
        assertThat(autoReopenEvents()).isZero();
    }

    @Test
    void endingOldSessionMustNotClearVisitorsInAnotherSession() {
        seed(5, 2);
        enter(1);
        String originalSession = sessionId;
        sessionId = "next_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity,
                    status, created_at, updated_at)
                VALUES (?, ?, now() - interval '1 hour', now() + interval '1 hour', 100, 0, 'open', now(), now())
                """, sessionId, projectId);
        lifecycle.lockSessionWindow(repository.findSessionById(sessionId).orElseThrow());
        enter(2);
        jdbcTemplate.update("UPDATE amusement_sessions SET end_time = now() - interval '1 minute' WHERE id = ?",
                originalSession);

        amusementService.closeEndedSessionsAndClearCounts(Instant.now());

        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT released_at IS NOT NULL FROM amusement_verifications WHERE session_id = ? AND direction = 'entry'",
                Boolean.class, originalSession)).isTrue();
    }

    @Test
    void oldEntryExitAfterSessionEndMustNotDeductNewSessionVisitor() {
        seed(5, 2);
        enter(1);
        jdbcTemplate.update("UPDATE amusement_sessions SET end_time = now() - interval '1 minute' WHERE id = ?", sessionId);
        amusementService.closeEndedSessionsAndClearCounts(Instant.now());
        sessionId = "next_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity,
                    status, created_at, updated_at)
                VALUES (?, ?, now() - interval '1 hour', now() + interval '1 hour', 100, 0, 'open', now(), now())
                """, sessionId, projectId);
        lifecycle.lockSessionWindow(repository.findSessionById(sessionId).orElseThrow());
        enter(2);

        try {
            exit(1);
        } catch (ApiException failure) {
            assertThat(failure.code()).isEqualTo("ENTITLEMENT_NO_ACTIVE_ENTRY");
        }

        assertThat(inPlayCount()).isEqualTo(1);
    }


    @Test
    void earlyEndRequiresActualClearanceAndKeepsTicketFacts() {
        seed(5, 1);
        enter(1);
        Instant cleared=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        assertThatThrownBy(()->lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
                "early-no-"+suffix,false,cleared,"尚未清场",List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(inPlayCount()).isEqualTo(1);
        var request=new AmusementSessionLifecycleService.EndRequest("early-yes-"+suffix,true,cleared,"负责人现场清场完成",List.of());
        var ending=lifecycle.endEarly(sessionId,request);
        assertThat(lifecycle.endEarly(sessionId,request).id()).isEqualTo(ending.id());
        assertThat(inPlayCount()).isZero();
        assertThat(ending.clearedPeople()).isEqualTo(1);
        assertThat(ending.actualEndAt()).isEqualTo(cleared);
        assertThat(ending.plannedEndAt()).isAfter(cleared);
        assertThat(jdbcTemplate.queryForObject("SELECT release_source FROM amusement_verifications WHERE project_id=? AND direction='entry'",String.class,projectId))
                .isEqualTo("session_end");
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,"ie_cg02_"+suffix+"_1")).isEqualTo(1);
        assertThat(lifecycle.followups(sessionId)).hasSize(1);
        assertThat(lifecycle.followups(sessionId).getFirst()).containsEntry("status","pending");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",Integer.class,sessionId)).isEqualTo(1);
    }

    /** BSM L1263 / ENUM L142「已售但未履行的权益按既有取消、补偿规则处理并留痕」：既有预约取消的结果写回提前结束待办。 */
    @Test
    void earlyEndFollowupCompletesWhenReservationCancelledByExistingRule() {
        seed(5, 1);
        String reservation=seedReservation();
        lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-resv-"+suffix,true,Instant.now(),"现场清场完成",List.of()));
        assertThat(lifecycle.followups(sessionId)).singleElement().satisfies(row->assertThat(row)
                .containsEntry("source_type","reservation").containsEntry("source_id",reservation).containsEntry("status","pending"));
        amusementService.changeReservationStatus(reservation,new StatusChangeRequest("cancel-resv-"+suffix,"cancelled","客人取消",List.of()));
        Map<String,Object> followup=lifecycle.followups(sessionId).getFirst();
        assertThat((String)followup.get("status")).isEqualTo("completed");
        assertThat(followup).containsEntry("resolution_type","reservation_cancelled").containsEntry("resolution_id",reservation)
                .containsEntry("resolved_by",userId);
        assertThat(followup.get("resolved_at")).isNotNull();
    }

    @Test
    void earlyEndFollowupCompletesWhenGroupBookingReleased() {
        seed(5, 1);
        String booking="group_booking_cg02_"+suffix;
        jdbcTemplate.update("""
            INSERT INTO amusement_group_bookings(id,project_id,session_id,booking_type,customer_name,party_size,reserved_capacity,
                booking_status,approved_at,paid_at,created_at,updated_at) VALUES(?,?,?,'group','团体客',2,2,'approved',now(),now(),now(),now())
            """,booking,projectId,sessionId);
        lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-group-"+suffix,true,Instant.now(),"现场清场完成",List.of()));
        assertThat(lifecycle.followups(sessionId)).singleElement().satisfies(row->assertThat(row)
                .containsEntry("source_type","group_booking").containsEntry("source_id",booking).containsEntry("status","pending"));
        amusementService.releaseGroupBooking(booking,new AmusementGroupBookingReleaseRequest("release-group-"+suffix,"客人取消",List.of()));
        Map<String,Object> followup=lifecycle.followups(sessionId).getFirst();
        assertThat((String)followup.get("status")).isEqualTo("completed");
        assertThat(followup).containsEntry("resolution_type","group_booking_released").containsEntry("resolution_id",booking)
                .containsEntry("resolved_by",userId);
        assertThat(jdbcTemplate.queryForObject("SELECT booking_status FROM amusement_group_bookings WHERE id=?",String.class,booking)).isEqualTo("released");
    }

    /** 未提前结束的预约取消不造待办行（命中 0 行静默）。 */
    @Test
    void cancellingReservationWithoutEarlyEndWritesNoFollowup() {
        seed(5, 1);
        String reservation=seedReservation();
        amusementService.changeReservationStatus(reservation,new StatusChangeRequest("cancel-plain-"+suffix,"cancelled","客人取消",List.of()));
        assertThat(jdbcTemplate.queryForObject("SELECT reservation_status FROM amusement_reservations WHERE id=?",String.class,reservation)).isEqualTo("cancelled");
        assertThat(lifecycle.followups(sessionId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_followups WHERE source_id=?",Integer.class,reservation)).isZero();
    }

    private String seedReservation() {
        String reservation="reservation_cg02_"+suffix;
        jdbcTemplate.update("""
            INSERT INTO amusement_reservations(id,project_id,session_id,customer_id,reservation_status,created_at,updated_at)
            VALUES(?,?,?,'cust_cg02','reserved',now(),now())
            """,reservation,projectId,sessionId);
        return reservation;
    }

    /**
     * 2026-10-18 操作员裁决（TK-18，裁决补充第 3 条）：已入场（已扣次）的客人比照 ENUM L148「全园故障与补偿通票」补偿——
     * 复用 CG-08b 补偿记录（原票、原场次、原入场核销、费用游乐公司承担、补偿有效期），发放前须游乐公司负责人与工程或安全岗位
     * 共同确认（同一确认入口），发放后原票下恢复本次 1 次；未入场的票不补偿、不建补偿待办（可再次使用）。
     */
    @Test
    void earlyEndCompensatesAdmittedGuestsAfterJointConfirmationAndLeavesUnusedTicketsAlone() {
        seed(5, 2);
        enter(1);
        String entryId=jdbcTemplate.queryForObject("SELECT id FROM amusement_verifications WHERE session_id=? AND direction='entry'",String.class,sessionId);
        var ending=lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-comp-"+suffix,true,Instant.now(),"设备临时停机，现场清场完成",List.of()));
        // 已入场人份进补偿待发放记录（事件类型「场次提前结束」，关联场次与结束原件）；未入场的第 2 张票不进。
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_fault_compensation_items WHERE original_entry_verification_id=?",Integer.class,entryId)).isEqualTo(1);
        Map<String,Object> item=jdbcTemplate.queryForMap("SELECT id,batch_id,kind,status,original_session_id,ending_id,fault_id FROM amusement_fault_compensation_items WHERE original_entry_verification_id=?",entryId);
        assertThat(item).containsEntry("kind","full_reissue").containsEntry("status","pending").containsEntry("original_session_id",sessionId).containsEntry("ending_id",ending.id());
        assertThat(item.get("fault_id")).isNull();
        String batchId=(String)item.get("batch_id");
        Map<String,Object> batch=jdbcTemplate.queryForMap("SELECT event_type,status,ending_id,session_id,bearer_company_id,reissue_count,extend_count,initiated_role FROM amusement_fault_compensation_batches WHERE id=?",batchId);
        assertThat(batch).containsEntry("event_type","session_early_end").containsEntry("status","pending_confirmation").containsEntry("ending_id",ending.id())
                .containsEntry("session_id",sessionId).containsEntry("bearer_company_id","be_amusement").containsEntry("reissue_count",1).containsEntry("extend_count",0);
        assertThat(batch.get("initiated_role")).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_fault_compensation_items WHERE entitlement_id=?",Integer.class,"ie_cg02_"+suffix+"_2")).isZero();
        assertThat(lifecycle.followups(sessionId)).singleElement().satisfies(row->assertThat(row)
                .containsEntry("source_type","entry").containsEntry("source_id",entryId).containsEntry("status","pending"));

        // 发放前须两岗位共同确认：第一岗位确认后仍待第二岗位且未发放；同一人换岗位不算；另一人以另一岗位确认即发放。
        String complex=businessDays.complexId(projectId);
        String head=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("amusement.park_fault.confirm_as_head",
                "amusement.park_fault.confirm_as_engineering_safety","amusement.park_fault.view"));
        String engineer=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("amusement.park_fault.confirm_as_engineering_safety","amusement.park_fault.view"));
        AmusementBusinessDayTestFixture.authenticate(head,complex);
        var first=compensationService.confirmBatch(batchId,new CompensationBatchConfirmRequest("comp-head-"+suffix,AmusementParkWideFaultService.ROLE_HEAD,"负责人确认"));
        assertThat(first.status()).isEqualTo("pending_confirmation");
        assertThat(first.initiatedRole()).isEqualTo(AmusementParkWideFaultService.ROLE_HEAD);
        assertThat(first.initiatedBy()).isEqualTo(head);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM issued_entitlements WHERE compensation_item_id=?",Integer.class,item.get("id"))).isZero();
        assertThatThrownBy(()->compensationService.confirmBatch(batchId,new CompensationBatchConfirmRequest("comp-same-"+suffix,
                AmusementParkWideFaultService.ROLE_ENGINEERING_SAFETY,"本人换岗位")))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PARK_WIDE_FAULT_SAME_PERSON_FORBIDDEN"));
        AmusementBusinessDayTestFixture.authenticate(engineer,complex);
        var issued=compensationService.confirmBatch(batchId,new CompensationBatchConfirmRequest("comp-eng-"+suffix,AmusementParkWideFaultService.ROLE_ENGINEERING_SAFETY,"工程岗确认"));
        assertThat(issued.status()).isEqualTo("issued");
        assertThat(issued.confirmedBy()).isEqualTo(engineer);
        assertThat(issued.validUntil()).isNotNull();
        // 原票下恢复本次 1 次：原权益不回改（仍已扣 1 次），补发行在原票、原父票下 1 次未用，有效期至补偿有效期截止。
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,"ie_cg02_"+suffix+"_1")).isEqualTo(1);
        List<Map<String,Object>> restored=jdbcTemplate.queryForList("SELECT entitlement_type,total_times,used_times,order_id,parent_ticket_id,valid_to"
                +" FROM issued_entitlements WHERE compensation_item_id=?",item.get("id"));
        assertThat(restored).singleElement().satisfies(row->{
            assertThat(row).containsEntry("entitlement_type","amusement_pass").containsEntry("total_times",1).containsEntry("used_times",0)
                    .containsEntry("order_id","order_cg02_"+suffix).containsEntry("parent_ticket_id","parent_cg02_"+suffix+"_1");
            assertThat(((java.sql.Timestamp)row.get("valid_to")).toInstant()).isEqualTo(issued.validUntil());
        });
        // 待办留痕：入场待办写回补偿发放结果；未入场的第 2 张票仍 0 次已用、无任何补偿行。
        authenticate(userId);
        Map<String,Object> followup=lifecycle.followups(sessionId).getFirst();
        assertThat(followup).containsEntry("status","completed").containsEntry("resolution_type","compensation_issued")
                .containsEntry("resolution_id",batchId).containsEntry("resolved_by",engineer);
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,"ie_cg02_"+suffix+"_2")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM issued_entitlements WHERE order_id=? AND compensation_item_id IS NOT NULL",Integer.class,"order_cg02_"+suffix)).isEqualTo(1);
    }

    /** 无已入场客人的提前结束：不建补偿记录、不建入场待办（未入场的票可再次使用）。 */
    @Test
    void earlyEndWithoutAdmittedGuestsCreatesNoCompensation() {
        seed(5, 1);
        var ending=lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-none-"+suffix,true,Instant.now(),"无人入场，现场清场完成",List.of()));
        assertThat(ending.clearedPeople()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_fault_compensation_items WHERE original_session_id=?",Integer.class,sessionId)).isZero();
        assertThat(lifecycle.followups(sessionId)).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,"ie_cg02_"+suffix+"_1")).isZero();
    }

    @Test
    void earlyEndRejectsClearanceBeforeLaterAdmission() {
        seed(5, 1);
        Instant statedClearance=Instant.now().minusSeconds(60);
        enter(1);
        assertThatThrownBy(()->lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
                "early-stale-"+suffix,true,statedClearance,"旧时刻已清场",List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("SESSION_CLEARANCE_CONFLICT"));
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @Test
    void earlyEndReplayStillChecksCurrentProjectScope() {
        seed(5, 1);
        enter(1);
        var request=new AmusementSessionLifecycleService.EndRequest("early-scope-"+suffix,true,Instant.now(),"现场清场完成",List.of());
        lifecycle.endEarly(sessionId,request);
        String other=AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        String outsider=AmusementBusinessDayTestFixture.actor(jdbcTemplate,other,List.of("amusement.session.end","amusement.session.view"));
        AmusementBusinessDayTestFixture.authenticate(outsider,other);
        assertThatThrownBy(()->lifecycle.endEarly(sessionId,request)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->lifecycle.endings(sessionId)).isInstanceOf(ApiException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",Integer.class,sessionId)).isEqualTo(1);
    }

    /** TK18-R3（COVERAGE_ONLY）BSM L1263 / ENUM L142「可由获授权的现场负责人…提前结束」：本综合体只持 amusement.session.view 的账号能读、不能结束。 */
    @Test
    void earlyEndRejectsViewOnlyActor() {
        seed(5, 1);
        enter(1);
        String complex=businessDays.complexId(projectId);
        String viewer=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("amusement.session.view"));
        AmusementBusinessDayTestFixture.authenticate(viewer,complex);
        assertThat(lifecycle.endings(sessionId)).isEmpty();
        assertThatThrownBy(()->lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
                "early-viewer-"+suffix,true,Instant.now(),"只读账号尝试结束",List.of())))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",Integer.class,sessionId)).isZero();
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @Test
    void concurrentEarlyEndCreatesOnlyOneEndingAndOneRelease() throws Exception {
        seed(5, 1);
        enter(1);
        String actor=userId;
        Instant cleared=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        runBoth(()->{
            authenticate(actor);
            try { lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-a-"+suffix,true,cleared,"完成清场",List.of())); }
            catch(ApiException ex){assertThat(ex.code()).isEqualTo("SESSION_NOT_OPEN");}
            return null;
        },()->{
            authenticate(actor);
            try { lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest("early-b-"+suffix,true,cleared,"完成清场",List.of())); }
            catch(ApiException ex){assertThat(ex.code()).isEqualTo("SESSION_NOT_OPEN");}
            return null;
        });
        assertThat(inPlayCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",Integer.class,sessionId)).isEqualTo(1);
    }

    @Test
    void sameAdmissionReplayedAcrossEntryPathsKeepsOneOriginalFact() {
        seed(5,1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?","shared_gate_"+suffix,projectId);
        replayEntry(true,1,"shared_"+suffix);
        replayEntry(false,1,"shared_"+suffix);
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=?",
            Integer.class,projectId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_1")).isEqualTo(1);
    }

    @Test
    void sameAdmissionCrossEntryReplaySurvivesClosedSessionWithoutReadmitting() {
        seed(5, 1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "shared_gate_" + suffix, projectId);
        var first = replayEntry(true, 1, "closed_replay_" + suffix);
        lifecycle.endEarly(sessionId, new AmusementSessionLifecycleService.EndRequest(
                "closed_end_" + suffix, true, Instant.now(), "负责人实际清场完成", List.of()));

        var replay = replayEntry(false, 1, "closed_replay_" + suffix);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.createdAt()).isEqualTo(first.createdAt());
        assertThat(inPlayCount()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id=?", Integer.class, "ie_cg02_" + suffix + "_1")).isEqualTo(1);
        assertThatThrownBy(() -> replayEntry(false, 1, "closed_new_request_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("AMUSEMENT_NO_OPEN_SESSION"));
    }

    @Test
    void sameAdmissionCrossEntryReplaySurvivesProjectPauseWithoutReadmitting() {
        seed(5, 1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "shared_gate_" + suffix, projectId);
        var first = replayEntry(false, 1, "paused_replay_" + suffix);
        jdbcTemplate.update("UPDATE amusement_projects SET open_status='paused',auto_paused_reason=NULL WHERE id=?", projectId);

        var replay = replayEntry(true, 1, "paused_replay_" + suffix);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.createdAt()).isEqualTo(first.createdAt());
        assertThat(inPlayCount()).isEqualTo(1);
        assertThatThrownBy(() -> replayEntry(true, 1, "paused_new_request_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("PROJECT_NOT_OPEN"));
    }

    @Test
    void successfulCrossEntryReplayStillRejectsRevokedPermissionAfterClosure() {
        seed(5, 1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "shared_gate_" + suffix, projectId);
        replayEntry(true, 1, "revoked_replay_" + suffix);
        lifecycle.endEarly(sessionId, new AmusementSessionLifecycleService.EndRequest(
                "revoked_end_" + suffix, true, Instant.now(), "负责人实际清场完成", List.of()));
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id=? AND permission_code='verification.execute'", roleId);

        assertThatThrownBy(() -> replayEntry(false, 1, "revoked_replay_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("PERMISSION_DENIED"));
        assertThat(inPlayCount()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM amusement_verifications WHERE project_id=? AND direction='entry'", Integer.class, projectId)).isEqualTo(1);
    }

    @Test
    void sameAdmissionKeyCannotChangeExplicitSessionAcrossEntryPaths() {
        seed(5, 1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?", "shared_gate_" + suffix, projectId);
        replayEntry(false, 1, "session_replay_" + suffix);
        String otherSession = "other_session_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO amusement_sessions(id,project_id,start_time,end_time,capacity,used_capacity,status,created_at,updated_at)
                VALUES(?,?,now()-interval '1 minute',now()+interval '30 minutes',0,0,'open',now(),now())
                """, otherSession, projectId);
        lifecycle.lockSessionWindow(repository.findSessionById(otherSession).orElseThrow());

        assertThatThrownBy(() -> amusementService.createVerification(new VerificationRequest(
                "session_replay_" + suffix, "entitlement_code", code(1), null, null,
                Map.of("project_id", projectId, "session_id", otherSession,
                        "location_point_id", "shared_gate_" + suffix, "staff_id", "usr_gate"))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM amusement_verifications WHERE session_id=?", Integer.class, otherSession)).isZero();
    }

    @Test
    void sameAdmissionKeyWithDifferentEntitlementConflictsWithoutExtraConsumption() {
        seed(5,2);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?","shared_gate_"+suffix,projectId);
        replayEntry(true,1,"shared_"+suffix);
        assertThatThrownBy(()->replayEntry(false,2,"shared_"+suffix)).isInstanceOfSatisfying(ApiException.class,
            failure->assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_2")).isZero();
    }

    @Test
    void concurrentSameAdmissionAcrossEntryPathsOccupiesCapacityOnce() throws Exception {
        seed(5,1);
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?","shared_gate_"+suffix,projectId);
        String actor=userId;
        runBoth(()->{authenticate(actor);replayEntry(true,1,"shared_"+suffix);return null;},
            ()->{authenticate(actor);replayEntry(false,1,"shared_"+suffix);return null;});
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=?",
            Integer.class,projectId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_1")).isEqualTo(1);
    }

    private AmusementVerificationRecord replayEntry(boolean direct,int n,String key) {
        String point="shared_gate_"+suffix;
        if(direct) {
            return amusementService.createVerification(new VerificationRequest(key,"entitlement_code",code(n),null,null,
                Map.of("project_id",projectId,"session_id",sessionId,"location_point_id",point,"staff_id","usr_gate")));
        }
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status->
                amusementService.verifyAmusementEntry(new AmusementEntryRequest(
                    code(n),point,null,"usr_gate",key,Instant.now())).orElseThrow());
    }

    @Test
    void authorizedPlatformStaffCanUseDirectAmusementEntry() {
        seed(5,1);
        String complex=businessDays.complexId(projectId);
        String staff=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("amusement.verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(staff,complex);
        enter(1);
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @Test
    void authorizedPlatformStaffCanUseTicketAmusementEntry() {
        seed(5,1);
        String complex=businessDays.complexId(projectId);
        String staff=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(staff,complex);
        ticketEnter(1);
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @Test
    void bothEntryPathsRejectAnotherComplexDespitePermission() {
        seed(5,1);
        String other=AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        String staff=AmusementBusinessDayTestFixture.actor(jdbcTemplate,other,
            List.of("amusement.verification.execute","verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(staff,other);
        assertThatThrownBy(()->enter(1)).isInstanceOfSatisfying(ApiException.class,
            failure->assertThat(failure.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(()->ticketEnter(1)).isInstanceOfSatisfying(ApiException.class,
            failure->assertThat(failure.code()).isEqualTo("FORBIDDEN"));
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_1")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=?",
            Integer.class,projectId)).isZero();
    }

    @Test
    void permissionRevokedWhileAdmissionWaitsForProjectLockPreventsEntry() throws Exception {
        seed(5,1);
        String complex=businessDays.complexId(projectId);
        String staff=AmusementBusinessDayTestFixture.actor(jdbcTemplate,complex,List.of("amusement.verification.execute"));
        raceUnderProjectLock(()->repository.lockProjectForSafety(projectId),()->{
            AmusementBusinessDayTestFixture.authenticate(staff,complex);
            assertThatThrownBy(()->enter(1)).isInstanceOf(ApiException.class);
        },new java.util.concurrent.atomic.AtomicReference<>(),()->jdbcTemplate.update("""
            DELETE FROM role_permissions WHERE role_id IN (SELECT role_id FROM user_roles WHERE user_id=?)
                AND permission_code='amusement.verification.execute'
            """,staff));
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_1")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_verifications WHERE project_id=?",
            Integer.class,projectId)).isZero();
    }

    private void ticketEnter(int n) {
        String point="gate_"+suffix;
        jdbcTemplate.update("UPDATE amusement_projects SET location_point_id=? WHERE id=?",point,projectId);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status->
            assertThat(amusementService.verifyAmusementEntry(new AmusementEntryRequest(
                code(n),point,null,"usr_gate","ticket_in_"+n+"_"+suffix,Instant.now()))).isPresent());
    }

    @Test
    void missingSessionWindowDoesNotInferItFromCurrentConfiguration() {
        seed(5,1);
        jdbcTemplate.update("""
            UPDATE amusement_sessions SET complex_id=NULL,business_day_version_id=NULL,business_date=NULL,
                business_start_at=NULL,business_end_at=NULL WHERE id=?
            """,sessionId);
        assertThatThrownBy(()->enter(1)).isInstanceOfSatisfying(ApiException.class,
            failure->assertThat(failure.code()).isEqualTo("AMUSEMENT_SESSION_DAY_PENDING"));
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_1")).isZero();
    }

    @Test
    void exitWithMissingEntryWindowDoesNotDeductCurrentVisitors() {
        seed(5,1);
        enter(1);
        jdbcTemplate.update("""
            UPDATE amusement_verifications SET complex_id=NULL,business_day_version_id=NULL,business_date=NULL,
                business_start_at=NULL,business_end_at=NULL WHERE project_id=? AND direction='entry'
            """,projectId);
        assertThatThrownBy(()->exit(1)).isInstanceOfSatisfying(ApiException.class,
            failure->assertThat(failure.code()).isEqualTo("AMUSEMENT_ENTRY_DAY_PENDING"));
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @Test
    void earlyEndCommitsBeforeWaitingAdmissionAndLeavesItsEntitlementUnused() throws Exception {
        seed(5,2);
        enter(1);
        Instant cleared=Instant.now();
        raceUnderProjectLock(
            ()->lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
                "end-first-"+suffix,true,cleared,"现场清场完成",List.of())),
            ()->assertThatThrownBy(()->enter(2)).isInstanceOfSatisfying(ApiException.class,
                failure->assertThat(failure.code()).isEqualTo("SESSION_NOT_OPEN")));
        assertThat(inPlayCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT used_times FROM issued_entitlements WHERE id=?",Integer.class,
            "ie_cg02_"+suffix+"_2")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",
            Integer.class,sessionId)).isEqualTo(1);
    }

    @Test
    void admissionCommitsBeforeWaitingEndAndInvalidatesEarlierClearance() throws Exception {
        seed(5,1);
        Instant statedClearance=Instant.now().minusSeconds(60);
        raceUnderProjectLock(
            ()->enter(1),
            ()->assertThatThrownBy(()->lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
                "entry-first-"+suffix,true,statedClearance,"此前已清场",List.of())))
                    .isInstanceOfSatisfying(ApiException.class,
                        failure->assertThat(failure.code()).isEqualTo("SESSION_CLEARANCE_CONFLICT")));
        assertThat(inPlayCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM amusement_session_endings WHERE session_id=?",
            Integer.class,sessionId)).isZero();
    }

    @Test
    void waitingAdmissionLocksItsActualPostLockTime() throws Exception {
        seed(5,1);
        var releasedAt=new java.util.concurrent.atomic.AtomicReference<Instant>();
        raceUnderProjectLock(()->repository.lockProjectForSafety(projectId),()->{
            enter(1);
            Instant recorded=jdbcTemplate.queryForObject(
                "SELECT created_at FROM amusement_verifications WHERE project_id=? AND direction='entry'",
                java.sql.Timestamp.class,projectId).toInstant();
            assertThat(recorded).isAfterOrEqualTo(releasedAt.get());
        },releasedAt);
    }

    @Test
    void releaseHistoryRequiresPermissionAndRealComplexIntersection() {
        seed(5,1);
        enter(1);
        var ending=lifecycle.endEarly(sessionId,new AmusementSessionLifecycleService.EndRequest(
            "history-"+suffix,true,Instant.now(),"现场清场完成",List.of()));
        assertThat(lifecycle.releases(projectId)).anySatisfy(row->
            assertThat(row).containsEntry("source_id",ending.id()).containsEntry("cleared_people",1));
        String other=AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        String outsider=AmusementBusinessDayTestFixture.actor(jdbcTemplate,other,List.of("amusement.verification.view"));
        AmusementBusinessDayTestFixture.authenticate(outsider,other);
        assertThatThrownBy(() -> lifecycle.releases(projectId)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.code()).isEqualTo("FORBIDDEN"));
        assertThat(lifecycle.releases(null)).isEmpty();
    }

    /** 两个真实事务；查询数据库阻塞链确认第二路已等候第一路的项目锁后，才提交第一路。 */
    private void raceUnderProjectLock(Runnable first,Runnable second) throws Exception {
        raceUnderProjectLock(first,second,new java.util.concurrent.atomic.AtomicReference<>());
    }

    private void raceUnderProjectLock(Runnable first,Runnable second,
            java.util.concurrent.atomic.AtomicReference<Instant> releasedAt) throws Exception {
        raceUnderProjectLock(first,second,releasedAt,()->{});
    }

    private void raceUnderProjectLock(Runnable first,Runnable second,
            java.util.concurrent.atomic.AtomicReference<Instant> releasedAt,Runnable beforeRelease) throws Exception {
        var ready=new java.util.concurrent.CountDownLatch(1);
        var commit=new java.util.concurrent.CountDownLatch(1);
        var firstPid=new java.util.concurrent.CompletableFuture<Integer>();
        var secondPid=new java.util.concurrent.CompletableFuture<Integer>();
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        String actor=userId;
        try {
            Future<?> a=pool.submit(()->{
                authenticate(actor);
                try { tx.executeWithoutResult(status->{
                    firstPid.complete(jdbcTemplate.queryForObject("SELECT pg_backend_pid()",Integer.class));
                    first.run();
                    ready.countDown();
                    awaitLatch(commit);
                }); } finally { TestSecurityActors.clear(); }
            });
            assertThat(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            Future<?> b=pool.submit(()->{
                authenticate(actor);
                try { tx.executeWithoutResult(status->{
                    secondPid.complete(jdbcTemplate.queryForObject("SELECT pg_backend_pid()",Integer.class));
                    second.run();
                    status.setRollbackOnly();
                }); } finally { TestSecurityActors.clear(); }
            });
            int blocker=firstPid.get(10,java.util.concurrent.TimeUnit.SECONDS);
            int waiter=secondPid.get(10,java.util.concurrent.TimeUnit.SECONDS);
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            boolean blocked=false;
            while(System.nanoTime()<deadline) {
                blocked=Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                     WHERE pid=? AND ?=ANY(pg_blocking_pids(pid)) AND query ILIKE '%amusement_projects%')
                    """,Boolean.class,waiter,blocker));
                if(blocked)break;
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
            }
            assertThat(blocked).as("第二路确实等待同一项目的事务行锁").isTrue();
            beforeRelease.run();
            releasedAt.set(Instant.now());
            commit.countDown();
            a.get(20,java.util.concurrent.TimeUnit.SECONDS);
            b.get(20,java.util.concurrent.TimeUnit.SECONDS);
        } finally { commit.countDown(); pool.shutdownNow(); }
    }

    private static void awaitLatch(java.util.concurrent.CountDownLatch latch) {
        try {
            if(!latch.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("等待事务交接超时");
        } catch(InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private void approveBusinessDay() {
        String complex=AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        AmusementBusinessDayTestFixture.bind(jdbcTemplate,projectId,complex);
        jdbcTemplate.update("UPDATE ticket_orders SET project_id=? WHERE id=?", complex, "order_cg02_" + suffix);
        AmusementBusinessDayTestFixture.approve(jdbcTemplate,businessDays,reviews,complex,"Asia/Shanghai",4,
                Instant.now().minusSeconds(7*86400));
    }

    // ---- 正路驱动 ----

    private void enter(int n) {
        amusementService.createVerification(new VerificationRequest(
                "in_" + n + "_" + suffix, "entitlement_code", code(n), null, null,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "usr_gate")));
    }

    private void exit(int n) {
        amusementService.exitVerification(new AmusementExitRequest(
                "out_" + n + "_" + suffix, code(n), null, null, "usr_gate", List.of()));
    }

    private String code(int n) {
        return "code_cg02_" + suffix + "_" + n;
    }

    private Map<String, Object> projectRow() {
        return jdbcTemplate.queryForMap(
                "SELECT open_status, auto_paused_reason FROM amusement_projects WHERE id = ?", projectId);
    }

    private int inPlayCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT occupied_count FROM amusement_capacity_snapshot
                 WHERE project_id = ? AND dimension = 'project'
                   AND COALESCE(session_id, '') = '' AND COALESCE(location_point_id, '') = ''
                   AND COALESCE(device_id, '') = ''
                """, Integer.class, projectId);
        return count == null ? -1 : count;
    }

    private int autoReopenEvents() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name = 'amusement.project.auto_reopened' AND business_id = ?",
                Integer.class, projectId);
        return count == null ? 0 : count;
    }

    private void runBoth(Callable<?> a, Callable<?> b) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> fa = pool.submit(() -> { barrier.await(); return a.call(); });
            Future<?> fb = pool.submit(() -> { barrier.await(); return b.call(); });
            fa.get();
            fb.get();
        } finally {
            pool.shutdownNow();
        }
    }


    @ParameterizedTest
    @CsvSource({
        "projects,platform", "sessions,platform", "verifications,platform", "raw-verifications,platform", "capacity,platform",
        "projects,merchant", "sessions,merchant", "verifications,merchant", "raw-verifications,merchant", "capacity,merchant",
        "projects,merchant_admin", "sessions,merchant_admin", "verifications,merchant_admin", "raw-verifications,merchant_admin", "capacity,merchant_admin"
    })
    void readSurfacesUseRealComplexAndOptionalMerchantIntersection(String surface, String mode) {
        seed(5, 1);
        String complex = businessDays.complexId(projectId);
        String merchant = createReadMerchant(complex);
        jdbcTemplate.update("UPDATE amusement_projects SET merchant_id=? WHERE id=?", merchant, projectId);
        jdbcTemplate.update("UPDATE spaces SET merchant_id=? WHERE id=(SELECT space_id FROM amusement_projects WHERE id=?)", merchant, projectId);
        enter(1);
        String otherComplex = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        AmusementBusinessDayTestFixture.approve(jdbcTemplate, businessDays, reviews, otherComplex,
                "Asia/Shanghai", 4, Instant.now().minusSeconds(7 * 86400));
        String outside = seedReadProject(otherComplex, merchant);
        String sameComplexOtherMerchant = seedReadProject(complex, createReadMerchant(complex));
        String actor = AmusementBusinessDayTestFixture.actor(jdbcTemplate, complex,
                List.of("amusement.session.view", "amusement.verification.view"));
        if ("platform".equals(mode)) {
            AmusementBusinessDayTestFixture.authenticate(actor, complex);
        } else {
            String account = "merchant_admin".equals(mode) ? "merchant_admin" : "staff";
            jdbcTemplate.update("UPDATE access_users SET merchant_id=?,account_type=? WHERE id=?", merchant, account, actor);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new AccessActor(actor, actor, account, complex, merchant, "active"), "token", List.of()));
        }

        List<String> projects = switch (surface) {
            case "projects" -> controller.listProjects().data().items().stream().map(AmusementProjectRecord::id).toList();
            case "sessions" -> controller.listSessions().data().items().stream().map(AmusementSessionRecord::projectId).toList();
            case "verifications" -> controller.listVerifications(null, null, null, null, null).data().items().stream()
                    .map(AmusementVerificationListItem::projectId).toList();
            case "raw-verifications" -> amusementService.listVerifications().items().stream().map(AmusementVerificationRecord::projectId).toList();
            case "capacity" -> controller.listCapacitySnapshots().data().items().stream().map(AmusementCapacitySnapshotRecord::projectId).toList();
            default -> throw new AssertionError(surface);
        };
        assertThat(projects).doesNotContain(outside);
        assertThat(projects).containsExactlyInAnyOrderElementsOf("platform".equals(mode)
                ? List.of(projectId, sameComplexOtherMerchant) : List.of(projectId));
    }

    @Test
    void ordinaryPlatformStaffHardLimitPersistsIncidentUnderActualMerchant() {
        seed(1, 2);
        String complex = businessDays.complexId(projectId);
        String merchant = createReadMerchant(complex);
        jdbcTemplate.update("UPDATE amusement_projects SET merchant_id=? WHERE id=?", merchant, projectId);
        jdbcTemplate.update("UPDATE spaces SET merchant_id=? WHERE id=(SELECT space_id FROM amusement_projects WHERE id=?)", merchant, projectId);
        String actor = AmusementBusinessDayTestFixture.actor(jdbcTemplate, complex,
                List.of("amusement.verification.execute"));
        AmusementBusinessDayTestFixture.authenticate(actor, complex);
        enter(1);

        assertThatThrownBy(() -> enter(2)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.code()).isEqualTo("SESSION_CAPACITY_EXHAUSTED"));

        assertThat(projectRow()).containsEntry("open_status", "paused").containsEntry("auto_paused_reason", "hard_capacity");
        assertThat(jdbcTemplate.queryForList("SELECT merchant_id FROM amusement_incidents WHERE project_id=?", String.class, projectId))
                .containsExactly(merchant);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='amusement.incident.over_capacity' AND actor_id=?",
                Integer.class, actor)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='amusement.risk.unified_alert_failed' AND actor_id=?",
                Integer.class, actor)).isEqualTo(1);
    }

    @Test
    void lifecycleEndRecordsPromisedSuccessAuditWithSessionAndActor() {
        seed(5, 1);
        enter(1);
        endThroughController();
        assertThat(auditCount("amusement.session.end_completed", "success", sessionId, userId)).isEqualTo(1);
    }

    @Test
    void lifecycleEndRecordsPromisedFailureAuditWhenClearanceIsUnconfirmed() {
        seed(5, 1);
        enter(1);
        assertThatThrownBy(() -> lifecycleController.end(sessionId, new AmusementSessionLifecycleService.EndRequest(
                "audit-denied-" + suffix, false, Instant.now(), "尚未清场", List.of())))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(auditCount("amusement.session.end_failed", "failure", sessionId, userId)).isEqualTo(1);
        assertThat(inPlayCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"endings", "followups", "releases"})
    void lifecycleReadsRecordPromisedAuditWithRequestedObject(String surface) {
        seed(5, 1);
        enter(1);
        endThroughController();
        String object = "releases".equals(surface) ? projectId : sessionId;
        int before = auditCount("amusement.session.viewed", "success", object, userId);
        assertThat(readLifecycle(surface)).isNotEmpty();
        assertThat(auditCount("amusement.session.viewed", "success", object, userId)).isEqualTo(before + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"endings", "followups", "releases"})
    void lifecycleReadsRejectAnotherComplexAndRecordPromisedFailureAudit(String surface) {
        seed(5, 1);
        enter(1);
        endThroughController();
        String other = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        String actor = AmusementBusinessDayTestFixture.actor(jdbcTemplate, other,
                List.of("amusement.session.view", "amusement.verification.view"));
        AmusementBusinessDayTestFixture.authenticate(actor, other);
        assertThatThrownBy(() -> readLifecycle(surface)).isInstanceOf(ApiException.class);
        String object = "releases".equals(surface) ? projectId : sessionId;
        assertThat(auditCount("amusement.session.view_failed", "failure", object, actor)).isEqualTo(1);
    }


    @ParameterizedTest
    @ValueSource(strings = {"own_platform", "own_merchant", "multiple_projects", "no_projects", "platform_global_merchant"})
    void sessionRepositoryFiltersLockedComplexAndMerchantBeforeReturningRows(String scope) {
        seed(5, 1);
        String complex = businessDays.complexId(projectId);
        String merchant = createReadMerchant(complex);
        jdbcTemplate.update("UPDATE amusement_projects SET merchant_id=? WHERE id=?", merchant, projectId);
        jdbcTemplate.update("UPDATE spaces SET merchant_id=? WHERE id=(SELECT space_id FROM amusement_projects WHERE id=?)", merchant, projectId);
        String otherComplex = AmusementBusinessDayTestFixture.complex(jdbcTemplate);
        AmusementBusinessDayTestFixture.approve(jdbcTemplate, businessDays, reviews, otherComplex,
                "Asia/Shanghai", 4, Instant.now().minusSeconds(7 * 86400));
        String outside = seedReadProject(otherComplex, merchant);
        String otherMerchant = seedReadProject(complex, createReadMerchant(complex));
        boolean unrestricted = "platform_global_merchant".equals(scope);
        java.util.Set<String> complexes = switch (scope) {
            case "multiple_projects" -> java.util.Set.of(complex, otherComplex);
            case "no_projects", "platform_global_merchant" -> java.util.Set.of();
            default -> java.util.Set.of(complex);
        };
        String merchantScope = scope.endsWith("merchant") ? merchant : null;

        List<String> result = repository.listSessionsScoped(unrestricted, complexes, merchantScope).stream()
                .map(AmusementSessionRecord::projectId).toList();

        List<String> expected = switch (scope) {
            case "own_platform" -> List.of(projectId, otherMerchant);
            case "own_merchant" -> List.of(projectId);
            case "multiple_projects" -> List.of(projectId, outside, otherMerchant);
            case "platform_global_merchant" -> List.of(projectId, outside);
            case "no_projects" -> List.of();
            default -> throw new AssertionError(scope);
        };
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void sessionControllerPassesActualReachToRepositoryWithoutUnscopedRead() {
        seed(5, 1);
        String complex = businessDays.complexId(projectId);
        String actor = AmusementBusinessDayTestFixture.actor(jdbcTemplate, complex, List.of("amusement.session.view"));
        AmusementBusinessDayTestFixture.authenticate(actor, complex);
        org.mockito.Mockito.clearInvocations(repository);

        assertThat(controller.listSessions().data().items()).extracting(AmusementSessionRecord::id).containsExactly(sessionId);

        org.mockito.Mockito.verify(repository).listSessionsScoped(false, java.util.Set.of(complex), null);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).listSessions();
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).listSessionsByMerchant(org.mockito.ArgumentMatchers.anyString());
    }

    private String createReadMerchant(String complex) {
        String merchant = "read_m_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at)
                VALUES(?,'范围复核游乐商户','amusement',?,'active',now(),now())
                """, merchant, complex);
        return merchant;
    }

    /** 只读范围夹具：另一真实综合体、商户、场次及锁版核销原件，不能靠全局管理员掩盖读权限。 */
    private String seedReadProject(String complex, String merchant) {
        String ride = "read_p_" + UUID.randomUUID().toString().replace("-", "");
        String session = "read_s_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        repository.saveProject(new AmusementProjectRecord(ride, "读取范围测试项目", 5, "open", templateId,
                Map.of(), null, now, now, merchant));
        extraReadProjects.add(ride);
        AmusementBusinessDayTestFixture.bind(jdbcTemplate, ride, complex);
        AmusementSessionRecord source = repository.saveSession(new AmusementSessionRecord(session, ride,
                now.minusSeconds(60), now.plusSeconds(3600), 0, 0, 0, "open", List.of(), now, now));
        lifecycle.lockSessionWindow(source);
        repository.claimCapacityAcrossDimensions(new CapacityClaimContext(ride, session, null, null, 5, 1));
        String entry = "read_v_" + UUID.randomUUID().toString().replace("-", "");
        repository.saveVerification(AmusementVerificationRecord.entry(entry, ride, session, "范围夹具凭据",
                null, userId, "success", null, List.of(), now, 1, null, null));
        lifecycle.recordEntryWindow(entry, businessDays.requireWindow(ride, now));
        return ride;
    }

    private void endThroughController() {
        lifecycleController.end(sessionId, new AmusementSessionLifecycleService.EndRequest(
                "audit-end-" + suffix, true, Instant.now(), "负责人确认实际清场完成", List.of()));
    }

    private List<?> readLifecycle(String surface) {
        return switch (surface) {
            case "endings" -> lifecycleController.endings(sessionId).data();
            case "followups" -> lifecycleController.followups(sessionId).data();
            case "releases" -> releaseController.releases(projectId).data();
            default -> throw new AssertionError(surface);
        };
    }

    private int auditCount(String event, String result, String object, String actor) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE event_name=? AND event_result=? AND business_id=? AND actor_id=?
                """, Integer.class, event, result, object, actor);
    }

    // ---- 种子 ----

    /**
     * @param safetyCapacity 项目硬安全容量（amusement_projects.capacity，项目维快照上限）
     * @param entitlements   预发的游乐权益数（每张 1 人、1 次）
     */
    private void seed(int safetyCapacity, int entitlements) {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "amu_cg02_" + suffix;
        sessionId = "sess_cg02_" + suffix;
        templateId = "tpl_cg02_" + suffix;
        jdbcTemplate.update("INSERT INTO amusement_safety_templates (id, template_name, items_json, status, created_at, updated_at) "
                + "VALUES (?, '硬超容测试模板', '[{\"name\":\"restraint\"}]'::jsonb, 'active', now(), now())", templateId);
        jdbcTemplate.update("INSERT INTO amusement_projects (id, project_name, capacity, open_status, safety_template_id, "
                + "eligibility_rule_json, created_at, updated_at) VALUES (?, '硬超容测试项目', ?, 'open', ?, '{}'::jsonb, now(), now())",
                projectId, safetyCapacity, templateId);
        // 准入闸：最近一次巡检 pass + submitted 才放行核销（evaluateOpenEligibility）。
        jdbcTemplate.update("INSERT INTO amusement_safety_inspections (id, project_id, template_id, items_json, overall_result, "
                + "inspection_status, rectification_status, reopen_status, inspected_at, submitted_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, '[{\"name\":\"restraint\",\"result\":\"pass\"}]'::jsonb, 'pass', 'submitted', 'not_required', "
                + "'not_required', now(), now(), now(), now())", "insp_cg02_" + suffix, projectId, templateId);
        // 场次容量放大，确保耗尽的是项目硬安全容量维（project_safety），不是场次维。
        jdbcTemplate.update("INSERT INTO amusement_sessions (id, project_id, start_time, end_time, capacity, used_capacity, status, "
                + "created_at, updated_at) VALUES (?, ?, now() - interval '1 hour', now() + interval '1 hour', 100, 0, 'open', now(), now())",
                sessionId, projectId);
        // 真实票务权益（游乐类 amusement_*，1 人 1 次），经 TicketingAmusementEntitlementGateway 按权益码识别。
        jdbcTemplate.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at) "
                + "VALUES (?, '硬超容测试票', 'fixed', 'active', now(), now())", "tt_cg02_" + suffix);
        jdbcTemplate.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, "
                + "status, created_at, updated_at) VALUES (?, '硬超容测试权益', 'amusement_pass', 1, 0, 'active', now(), now())",
                "et_cg02_" + suffix);
        jdbcTemplate.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, "
                + "issue_status, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'web', 0, 'paid', 'issued', 'created', now(), now())",
                "order_cg02_" + suffix, "ONO_CG02_" + suffix, "tt_cg02_" + suffix, entitlements);
        for (int i = 1; i <= entitlements; i++) {
            String parent="parent_cg02_"+suffix+"_"+i;
            jdbcTemplate.update("""
                INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,num_people,total_times,
                    status,created_at,updated_at) VALUES(?,?,?,?,?,1,1,'active',now(),now())
                """,parent,"TNO_CG02_"+suffix+"_"+i,"order_cg02_"+suffix,"tt_cg02_"+suffix,i);
            jdbcTemplate.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, "
                    + "entitlement_type, total_times, used_times, status, valid_from, valid_to, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, 'amusement_pass', 1, 0, 'active', now() - interval '1 day', now() + interval '1 day', now(), now())",
                    "ie_cg02_" + suffix + "_" + i, "tt_cg02_" + suffix, "order_cg02_" + suffix, "et_cg02_" + suffix, code(i));
            jdbcTemplate.update("UPDATE issued_entitlements SET parent_ticket_id=?,slot_index=1 WHERE id=?",
                parent,"ie_cg02_"+suffix+"_"+i);
        }
        // 核销/出场/事故上报都走 requirePermission：amusement.verification.execute 为高危码，须给操作人显式角色来源。
        userId = "usr_cg02_" + suffix;
        roleId = "role_cg02_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) "
                + "VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_cg02_" + suffix);
        // 角色名全中文+数字：治理页上屏名称不得含拉丁字母（AccessJdbcIntegrationTests.seededDisplayNamesAreChinese 全库核查）。
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "硬超容测试角色" + System.nanoTime());
        for (String code : List.of("amusement.verification.execute", "amusement.incident.create", "amusement.session.end", "amusement.session.view", "amusement.verification.view", "verification.execute")) {
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", roleId, code);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        authenticate(userId);
        approveBusinessDay();
        lifecycle.lockSessionWindow(repository.findSessionById(sessionId).orElseThrow());
    }

    private void authenticate(String user) {
        AccessActor actor = new AccessActor(user, "u_cg02_" + suffix, "platform_admin", "org_1", null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }
}
