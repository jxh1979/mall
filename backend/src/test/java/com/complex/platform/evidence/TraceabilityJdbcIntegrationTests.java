package com.complex.platform.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TraceabilityJdbcIntegrationTests {

    @Autowired
    private JdbcTraceabilityRepository repository;

    @Autowired
    private TraceabilityService traceabilityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearActor() {
        TestSecurityActors.clear();
    }

    @Test
    void migrationCreatesThreeLayerTraceabilityTables() {
        assertThat(regclass("business_process_instances")).isEqualTo("business_process_instances");
        assertThat(regclass("business_process_step_records")).isEqualTo("business_process_step_records");
        assertThat(regclass("service_responsibility_records")).isEqualTo("service_responsibility_records");
    }

    @Test
    void processInstanceAndStepPersistAndReadBack() {
        TestSecurityActors.asPlatformAdmin();
        TraceabilityProcessInstanceRecord instance = traceabilityService.recordProcessInstance(
                new TraceabilityProcessInstanceRecord(null, "table_service", "table", "table_1", "prj_1",
                        null, null, null, null, null, "opened", null, null, null));
        traceabilityService.recordStep(new TraceabilityStepRecord(null, instance.id(), "serve", "上酒",
                "staff", "staff_1", "tablet", "dev_1", null, null, true, "qr_scan", false, null, null));

        assertThat(repository.findProcessInstanceById(instance.id())).isPresent();
        assertThat(repository.listStepsByInstance(instance.id()))
                .singleElement()
                .satisfies(step -> {
                    assertThat(step.captureSource()).isEqualTo("qr_scan");
                    assertThat(step.autoCaptured()).isTrue();
                    assertThat(step.result()).isEqualTo("success");
                });
    }

    @Test
    void recordResponsibilityInjectsMerchantFromTokenAndKeepsFinancialResponsibilitySegment() {
        TestSecurityActors.asMerchantUser("mch_A");
        TraceabilityResponsibilityRecord saved = traceabilityService.recordResponsibility(
                new TraceabilityResponsibilityRecord(null, "order_1", "table_1", "cust_1", null,
                        "staff_primary", List.of("staff_assist_1", "staff_assist_2"), "staff_sales", "staff_first",
                        "staff_refund", "staff_waive", "staff_approve", "staff_assigner", null, null,
                        "mch_FORGED", null, null, null));

        // 隔离：落库 merchant 必须是 token 的 mch_A，请求体伪造的 mch_FORGED 被忽略
        assertThat(saved.merchantId()).isEqualTo("mch_A");
        TraceabilityResponsibilityRecord read = repository.listResponsibilities("mch_A").stream()
                .filter(r -> saved.id().equals(r.id())).findFirst().orElseThrow();
        assertThat(read.merchantId()).isEqualTo("mch_A");
        assertThat(read.assistStaffIds()).containsExactly("staff_assist_1", "staff_assist_2");
        assertThat(read.refundedBy()).isEqualTo("staff_refund");
        assertThat(read.waivedBy()).isEqualTo("staff_waive");
        assertThat(read.approvedBy()).isEqualTo("staff_approve");
        assertThat(read.status()).isEqualTo("active");
    }

    @Test
    void listResponsibilitiesIsMerchantScoped() {
        TestSecurityActors.asMerchantUser("mch_A");
        TraceabilityResponsibilityRecord a = traceabilityService.recordResponsibility(responsibilityFor("order_a"));
        TestSecurityActors.asMerchantUser("mch_B");
        TraceabilityResponsibilityRecord b = traceabilityService.recordResponsibility(responsibilityFor("order_b"));

        TestSecurityActors.asMerchantUser("mch_A");
        assertThat(traceabilityService.listResponsibilities())
                .extracting(TraceabilityResponsibilityRecord::id)
                .contains(a.id())
                .doesNotContain(b.id());
    }

    @Test
    void autoCaptureRateCountsScopedStepsAgainstThreshold() {
        TestSecurityActors.asMerchantUser("mch_A");
        TraceabilityProcessInstanceRecord a = traceabilityService.recordProcessInstance(
                new TraceabilityProcessInstanceRecord(null, "saved_bottle_lifecycle", "saved_bottle", "saved_1", "prj_1",
                        null, null, null, null, null, "registered", null, null, null));
        traceabilityService.recordStep(new TraceabilityStepRecord(null, a.id(), "scan", "扫码", "staff", "staff_1",
                "staff_work_phone", "dev_1", null, null, true, "qr_scan", false, null, null));
        traceabilityService.recordStep(new TraceabilityStepRecord(null, a.id(), "manual_note", "人工备注", "staff", "staff_1",
                "web", null, null, null, false, "manual_input", true, null, null));

        TestSecurityActors.asMerchantUser("mch_B");
        TraceabilityProcessInstanceRecord b = traceabilityService.recordProcessInstance(
                new TraceabilityProcessInstanceRecord(null, "table_service", "table", "table_2", "prj_1",
                        null, null, null, null, null, "opened", null, null, null));
        traceabilityService.recordStep(new TraceabilityStepRecord(null, b.id(), "scan", "扫码", "staff", "staff_2",
                "staff_work_phone", "dev_2", null, null, true, "qr_scan", false, null, null));

        TestSecurityActors.asMerchantUser("mch_A");
        TraceabilityAutoCaptureRate rate = traceabilityService.autoCaptureRate(80);

        assertThat(rate.totalSteps()).isEqualTo(2);
        assertThat(rate.autoCapturedSteps()).isEqualTo(1);
        assertThat(rate.autoCaptureRatePercent()).isEqualTo(50.0);
        assertThat(rate.thresholdPercent()).isEqualTo(80);
        assertThat(rate.meetsThreshold()).isFalse();
    }

    private TraceabilityResponsibilityRecord responsibilityFor(String orderId) {
        return new TraceabilityResponsibilityRecord(null, orderId, null, null, null, "staff_p", List.of(),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "')::text", String.class);
    }

    // ===== XM-M031-B：失败留痕不随调用方事务回滚丢失（audit_events 只追加，V140；按本次唯一键断言，不清理） =====

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private com.complex.platform.access.AccessService accessService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private int auditRows(String eventName, String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE event_name = ? AND idempotency_key = ?",
                Integer.class, eventName, idempotencyKey);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failureEventOfATransactionalWriteSurvivesItsRollback() {
        TestSecurityActors.asPlatformAdmin("usr_xm031b");
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String plainKey = "xm031b-plain-" + suffix;
        String transactionalKey = "xm031b-tx-" + suffix;
        // 不带事务的赋角色与带 @Transactional 的定岗，对同一个不存在的用户各失败一次。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accessService.assignUserRole(
                        new com.complex.platform.access.dto.RoleAssignmentRequest(plainKey, null, "usr_xm031b_missing", "role_xm031b_missing")))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accessService.assignUserPosition(
                        new com.complex.platform.access.dto.UserPositionAssignmentRequest(transactionalKey, null, "usr_xm031b_missing",
                                "ops_clerk", "lvl_m1_lower", null, null, null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        assertThat(auditRows("access.role.assign_failed", plainKey)).isEqualTo(1);
        assertThat(auditRows("access.user_position.assign_failed", transactionalKey)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failureEventIsKeptAndTheOriginalErrorSurvivesWhenTheCallerTransactionIsAborted() {
        String key = "xm031b-abort-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        // 与生产 catch 同形：recordFailure(...); throw original; 不包内层 try。
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                            try {
                                jdbcTemplate.queryForObject("SELECT 1/0", Integer.class);
                            } catch (org.springframework.dao.DataAccessException original) {
                                auditEventService.recordFailure("xm031b.aborted_write_failed", "xm031b.probe", "xm031b_probe",
                                        key, null, key, "DIVISION_BY_ZERO");
                                throw original;
                            }
                        }))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(auditRows("xm031b.aborted_write_failed", key)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void successEventOfARolledBackTransactionIsNotKept() {
        String key = "xm031b-success-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                            auditEventService.recordSuccess("xm031b.rolled_back_write", "xm031b.probe", "xm031b_probe",
                                    key, null, key, java.util.Map.of());
                            throw new IllegalStateException("业务回滚");
                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(auditRows("xm031b.rolled_back_write", key)).isZero();
    }
}
