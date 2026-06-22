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
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
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
        TraceabilityResponsibilityRecord read = repository.findResponsibilityById(saved.id()).orElseThrow();
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
}
