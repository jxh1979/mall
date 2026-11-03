package com.complex.platform.ticketing;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.support.TestSecurityActors;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
 * <p>真实 Flyway 迁移库，{@code @Transactional} 回滚；验库 CHECK {@code chk_ticket_verifications_status}。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class VerificationStatusClosedSetJdbcIntegrationTests {

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
                null, null, null, null, now, now, "virtual", null, "per_use", null));
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

    /** 库 CHECK：接受 anomaly；拒绝闭集之外的值（如 pending_review / exception）。 */
    @Test
    void statusCheckAcceptsAnomalyAndRejectsOtherValues() {
        // TK-14（V524）：异常记录须指向一条原成功核销。
        insertVerification("ver_cg05_source_" + suffix, "success");
        jdbcTemplate.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, result_reason, source_type, created_at, verified_people,"
                + " anomaly_of_verification_id) VALUES (?, ?, ?, ?, 'qr', 'payload', ?, 'point', 'anomaly', 'cg05_test',"
                + " 'verification_anomaly', now(), 1, ?)",
                "ver_cg05_anomaly_" + suffix, "Vver_cg05_anomaly_" + suffix, activeId, activeCode, deviceCode, "ver_cg05_source_" + suffix);
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
