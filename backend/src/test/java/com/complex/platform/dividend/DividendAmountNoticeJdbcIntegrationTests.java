package com.complex.platform.dividend;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批第 13 条（BSM 第 1597 行「……分红出单与发放，通知中写明金额、原因与异议截止日期」；第 1598 行不可关闭）：分红单审批通过（出单）、
 * 标记发放、留存释放各通知商户一次；驳回不发。通知在业务提交后才发，本类不包事务。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
class DividendAmountNoticeJdbcIntegrationTests {

    @Autowired DividendService service;
    @Autowired JdbcDividendRepository repository;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;
    @Autowired PiiCipher cipher;

    String suffix, merchant, plan, submitter, approver;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        merchant = "dan_m_" + suffix;
        plan = "dan_plan_" + suffix;
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'分红通知商户','bar','主理人',?,'active',now(),now())
                """, merchant, cipher.encrypt("136" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000))));
        Instant now = Instant.now();
        repository.insertPlan(new DividendRecords.DividendPlanRecord(plan, merchant, "usr_principal_" + suffix, "主理人甲",
                8000, 2000, "monthly", List.of("bar", "ticket"), true, List.of("labor"),
                null, "active", null, "usr_div_" + suffix, now, now, false, false, false, 0, 0L));
        jdbc.update("UPDATE dividend_plans SET payout_pay_slot = 1 WHERE id = ?", plan);   // 211-R2 发放付账日
        String role = "dan_role_" + suffix;
        access.createRole(new RoleRecord(role, "分红通知审批-" + Long.parseLong(suffix, 16), null, "platform", "active", now,
                List.of(DividendService.MANAGE_PERM, DividendService.VIEW_PERM), null, false, List.of()));
        submitter = user(role);
        approver = user(role);
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    @Test
    void approvePaidAndReserveReleaseNotifyMerchantRejectDoesNot() {
        String issued = statement("2026-07");
        String rejected = statement("2026-08");
        TestSecurityActors.asPlatformAdmin(submitter);
        service.submit(issued, new DividendActionRequest(key(), null));
        service.submit(rejected, new DividendActionRequest(key(), null));
        TestSecurityActors.asPlatformAdmin(approver);
        service.reject(rejected, new DividendActionRequest(key(), "利润口径待复核"));
        service.approve(issued, new DividendActionRequest(key(), null));
        service.markPaid(issued, new DividendActionRequest(key(), null));
        service.releaseReserve(issued, new DividendActionRequest(key(), "到期返还"));

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT request_id, params->>'amount_cent' AS amount, params->>'reason' AS reason, params->>'objection_deadline' AS deadline
                  FROM notification_send_log WHERE domain='merchant_amount' AND params->>'merchant_id'=? ORDER BY created_at, id
                """, merchant);
        assertThat(rows).extracting(r -> r.get("request_id") + "=" + r.get("amount")).containsExactly(
                "mamt:dividend_issued:" + issued + "=14400",
                "mamt:dividend_paid:" + issued + "=14400",
                "mamt:dividend_paid:" + issued + ":reserve=1600");
        assertThat((String) rows.get(0).get("reason")).contains("2026-07").contains("分红出单").contains("留存 16.00 元");
        assertThat((String) rows.get(2).get("reason")).contains("留存释放");
        assertThat(rows).allSatisfy(r -> assertThat(r.get("deadline")).isNotNull());
    }

    /** 净利 10 万：平台 8 万、主理人税前 2 万、税 4000、税后 1.6 万，留存 1600、实发 1.44 万；已登记依法可分配金额。 */
    private String statement(String month) {
        String id = "dan_s_" + month + "_" + suffix;
        java.time.YearMonth ym = java.time.YearMonth.parse(month);
        Instant now = Instant.now();
        repository.insertStatement(new DividendStatementRecord(id, merchant, plan, "official", month, ym.atDay(1), ym.atEndOfMonth(),
                "usr_principal_" + suffix, 8000, 2000, List.of("bar", "ticket"), true, List.of("labor"),
                100_000L, 0L, 0L, 100_000L, 100_000L, 80_000L, 20_000L, 4_000L, 16_000L,
                0L, "draft", null, null, "usr_div_" + suffix, null, null, null, null, null, null, now, now,
                false, 0L, 0L, 0L, 0L, 1_600L, 14_400L, "individual", 0L, 1_000_000_000L));
        return id;
    }

    private String user(String role) {
        String id = "dan_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "platform_admin", "org_1", null, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
