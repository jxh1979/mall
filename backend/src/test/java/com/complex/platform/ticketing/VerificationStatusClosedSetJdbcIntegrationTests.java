package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.TerminalSyncTaskItemRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import java.sql.Timestamp;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-05 核销记录状态闭集（ENUM「核销记录状态」：「成功、拒绝（规则不允许）、失败（设备或网络原因）、已冲正、异常，共五态，不设「预检通过」态……
 * 系统不提供人工放行」）：规则不允许 → rejected；设备、网络、凭证技术原因 → failed；库 CHECK 接受 anomaly、拒绝其他值。
 *
 * <p>真实 Flyway 迁移库，{@code @Transactional} 回滚；走终端离线同步正路 {@code TicketingService.syncTerminalTasks}，
 * 追 {@code ticket_verifications.status / result_reason} 与 {@code terminal_sync_tasks.status}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class VerificationStatusClosedSetJdbcIntegrationTests {

    @Autowired private TicketingService ticketingService;
    @Autowired private TicketingRepository repository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private String suffix;
    private String deviceCode;
    private String deviceId;
    private String usedUpCode;
    private String usedUpId;
    private String activeCode;
    private String activeId;

    @BeforeEach
    void seed() {
        TestSecurityActors.asPlatformAdmin();
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Instant now = Instant.now();
        String ticketTypeId = "tt_cg05_" + suffix;
        repository.saveTicketType(new TicketTypeRecord(ticketTypeId, "状态闭集测试票", "flat", 5000L, 5000L, 5000L, false,
                "published", 1, 60, now, now, null, 0, null, false, null, TicketTypeRecord.NO_COMBO_DISCOUNT));
        String templateId = "tpl_cg05_" + suffix;
        repository.saveEntitlementTemplate(new EntitlementTemplateRecord(templateId, "酒水权益", "bar_drink", 1, 3300L, "active",
                null, null, null, null, now, now, "virtual", null, "per_use", null, false));
        String orderId = "ord_cg05_" + suffix;
        repository.saveTicketOrder(new TicketOrderRecord(orderId, "ON-" + suffix, "cus_cg05_" + suffix, ticketTypeId, null, 1,
                LocalDate.now(), "web", null, 5000L, null, 5000L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        usedUpCode = "CG05-USED-" + suffix;
        usedUpId = "ent_cg05_used_" + suffix;
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(usedUpId, usedUpCode, ticketTypeId, orderId, templateId,
                "bar_drink", "mch_cg05_" + suffix, null, null, 1, 1, 3300L, "active", now.minusSeconds(3600), now.plusSeconds(86400),
                now, now, 1, 0L, "virtual", null, "per_use", null, null, 0, null, null));
        activeCode = "CG05-ACTIVE-" + suffix;
        activeId = "ent_cg05_active_" + suffix;
        repository.saveIssuedEntitlement(new IssuedEntitlementRecord(activeId, activeCode, ticketTypeId, orderId, templateId,
                "bar_drink", "mch_cg05_" + suffix, null, null, 1, 0, 3300L, "active", now.minusSeconds(3600), now.plusSeconds(86400),
                now, now, 1, 0L, "virtual", null, "per_use", null, null, 0, null, null));
        deviceCode = "device_cg05_" + suffix;
        deviceId = "dev-" + deviceCode;
        jdbcTemplate.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)",
                deviceCode, deviceId, "DC-" + deviceCode, Timestamp.from(now), Timestamp.from(now));
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    /** 规则不允许（次数用尽）：离线同步的核销落 rejected 记录，任务结果 rejected，权益不被消费；同批其他任务照常成功。 */
    @Test
    void ruleRejectionIsRecordedAsRejectedAndBatchContinues() {
        SyncTasksResponse response = ticketingService.syncTerminalTasks(new TerminalSyncTasksRequest(
                "cg05-sync-" + suffix, null, null, deviceCode, "staff_" + suffix, List.of(
                        new TerminalSyncTaskItemRequest("task-used-" + suffix, "offline_verification", "2026-10-17T10:00:00Z",
                                Map.of("entitlementCode", usedUpCode, "pointId", "point_cg05_" + suffix)),
                        new TerminalSyncTaskItemRequest("task-ok-" + suffix, "offline_verification", "2026-10-17T10:01:00Z",
                                Map.of("entitlementCode", activeCode, "pointId", "point_cg05_" + suffix)))));
        assertThat(response.results()).hasSize(2);
        SyncTaskResult rejected = response.results().get(0);
        assertThat(rejected.status()).isEqualTo("rejected");
        assertThat(rejected.reason()).isEqualTo("entitlement_used_up");
        assertThat(rejected.verificationId()).isNotBlank();
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT status, result_reason, source_type, client_task_id, device_id"
                + " FROM ticket_verifications WHERE id = ?", rejected.verificationId());
        assertThat(row.get("status")).isEqualTo("rejected");
        assertThat(row.get("result_reason")).isEqualTo("entitlement_used_up");
        assertThat(row.get("source_type")).isEqualTo("android_terminal_offline");
        assertThat(row.get("client_task_id")).isEqualTo("task-used-" + suffix);
        assertThat(row.get("device_id")).isEqualTo(deviceCode);
        assertThat(jdbcTemplate.queryForMap("SELECT used_times FROM issued_entitlements WHERE id = ?", usedUpId))
                .containsEntry("used_times", 1);
        assertThat(jdbcTemplate.queryForMap("SELECT status, reason FROM terminal_sync_tasks WHERE device_code = ? AND client_task_id = ?",
                deviceCode, "task-used-" + suffix)).containsEntry("status", "rejected").containsEntry("reason", "entitlement_used_up");
        assertThat(response.results().get(1).status()).isEqualTo("accepted");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Integer.class, activeId)).isEqualTo(1);
    }

    /** 设备 / 网络 / 凭证技术原因（凭证不存在）：任务结果 failed（无法定位权益则不落核销记录）。 */
    @Test
    void technicalFailureIsRecordedAsFailed() {
        SyncTasksResponse response = ticketingService.syncTerminalTasks(new TerminalSyncTasksRequest(
                "cg05-sync-tech-" + suffix, null, null, deviceCode, "staff_" + suffix, List.of(
                        new TerminalSyncTaskItemRequest("task-cred-" + suffix, "offline_verification", "2026-10-17T10:00:00Z",
                                Map.of("entitlementCode", "cred_missing_" + suffix, "pointId", "point_cg05_" + suffix)))));
        SyncTaskResult failed = response.results().get(0);
        assertThat(failed.status()).isEqualTo("failed");
        assertThat(failed.reason()).isEqualTo("credential_not_found");
        assertThat(failed.verificationId()).isNull();
        assertThat(jdbcTemplate.queryForMap("SELECT status FROM terminal_sync_tasks WHERE device_code = ? AND client_task_id = ?",
                deviceCode, "task-cred-" + suffix)).containsEntry("status", "failed");
    }

    /** 库 CHECK：接受 anomaly；拒绝闭集之外的值（如 pending_review / exception）。 */
    @Test
    void statusCheckAcceptsAnomalyAndRejectsOtherValues() {
        insertVerification("ver_cg05_anomaly_" + suffix, "anomaly");
        assertThat(jdbcTemplate.queryForMap("SELECT status FROM ticket_verifications WHERE id = ?", "ver_cg05_anomaly_" + suffix))
                .containsEntry("status", "anomaly");
        // 每个非法值各用一个保存点（嵌套事务），避免首个 CHECK 失败后外层事务进入中止态
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        for (String illegal : List.of("pending_review", "exception", "preview_passed")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(
                    status -> insertVerification("ver_cg05_bad_" + illegal + "_" + suffix, illegal)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("chk_ticket_verifications_status");
        }
    }

    private void insertVerification(String id, String status) {
        jdbcTemplate.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, result_reason, source_type, created_at, verified_people)"
                + " VALUES (?, ?, ?, ?, 'qr', 'payload', ?, 'point', ?, 'cg05_test', 'android_terminal_online', now(), 1)",
                id, "V" + id, activeId, activeCode, deviceCode, status);
    }
}
