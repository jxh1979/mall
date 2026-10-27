package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.support.InternalPayDayFixtures;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批第 13 条（BSM 第 1597 行「结算金额出账……通知中写明金额、原因与异议截止日期……异议期自该账单在商户专区可查看之日的次日起算」）：
 * 普通内部结算往来到出账日（商户专区可查看）逐笔通知对方商户，异议截止日取往来固化值；未到出账日、停车抵免（另有出账短信）、对方非商户不发；
 * 重跑不重发。操作员裁决第十八批第 2 条：通票赠饮应付不逐笔发，在门票分销出账短信里一并写明本期净额。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
class MerchantSettlementStatementNoticeJobTests {

    @Autowired MerchantSettlementStatementNoticeJob job;
    @Autowired JdbcTemplate jdbc;
    @Autowired PiiCipher cipher;

    String suffix, project, merchant, phone;
    LocalDate period;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        project = "mss_p_" + suffix;
        merchant = "mss_m_" + suffix;
        phone = "137" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        period = LocalDate.now(LocalZone.ID).withDayOfMonth(1).minusMonths(1);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','出账通知项目','active',now(),now())", project);
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'出账通知酒吧','bar',?,'主要联系人',?,'active',now(),now())
                """, merchant, project, cipher.encrypt(phone));
    }

    @Test
    void visibleMerchantStatementNotifiedOnceWithDeadlineAndPassDrinkTotal() {
        Instant past = Instant.now().minusSeconds(3600);
        LocalDate deadline = LocalDate.now(LocalZone.ID).plusDays(12);
        String statement = schedule("ticket_distribution", merchant, "payable", 5_000L, past, deadline);
        String future = schedule("ticket_distribution", merchant, "payable", 6_000L, Instant.now().plusSeconds(86_400), deadline);
        String parking = schedule("parking_credit_cost", merchant, "receivable", 700L, past, deadline);
        String entity = schedule("ticket_distribution", "mss_entity_" + suffix, "payable", 800L, past, deadline);
        String drink1 = drink("payable", "v1");
        String drink2 = drink("payable", "v2");
        String drinkReversal = drink("receivable", "v2");

        job.notifyVisibleStatements(Instant.now());
        job.notifyVisibleStatements(Instant.now());

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT request_id, event_key, template_code, recipient, params->>'amount_cent' AS amount, params->>'reason' AS reason,
                       params->>'objection_deadline' AS deadline
                  FROM notification_send_log WHERE domain='merchant_amount' AND params->>'merchant_id'=?
                """, merchant);
        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.get("request_id")).isEqualTo("mamt:settlement_statement:" + statement);
            assertThat(r.get("event_key")).isEqualTo("settlement_statement");
            assertThat(r.get("template_code")).isEqualTo("sms.merchant_amount.settlement_statement");
            assertThat(cipher.decrypt((String) r.get("recipient"))).isEqualTo(phone);
            assertThat(r.get("amount")).isEqualTo("5000");
            assertThat(r.get("deadline")).isEqualTo(deadline.toString());
            assertThat((String) r.get("reason")).contains("门票分销出账：应付商户").contains("另含本期通票赠饮应付 30.00 元");
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id LIKE 'mamt:%' AND (request_id LIKE ? "
                        + "OR request_id LIKE ? OR request_id LIKE ? OR request_id LIKE ? OR request_id LIKE ? OR request_id LIKE ?)",
                Integer.class, "%" + future, "%" + parking, "%" + entity, "%" + drink1, "%" + drink2, "%" + drinkReversal)).isZero();
    }

    /**
     * 操作员裁决第十八批第 2 条「不逐笔发：只在结算账单出账时发一条，里面已含这些赠饮应付」：同一酒吧同期两条门票分销往来（如专属直付与
     * 第二段池分账）另加一条追收——赠饮应付净额只写进其中一条门票分销出账短信，另一条与追收短信都不带。
     */
    @Test
    void passDrinkTotalWrittenIntoOnlyOneStatementPerMerchantAndPeriod() {
        Instant past = Instant.now().minusSeconds(3600);
        LocalDate deadline = LocalDate.now(LocalZone.ID).plusDays(12);
        schedule("ticket_distribution", merchant, "payable", 5_000L, past, deadline);
        schedule("ticket_distribution", merchant, "payable", 2_000L, past, deadline);
        schedule("ticket_distribution_recovery", merchant, "receivable", 300L, past, deadline);
        drink("payable", "v1");

        job.notifyVisibleStatements(Instant.now());

        List<String> reasons = jdbc.queryForList("""
                SELECT params->>'reason' FROM notification_send_log WHERE domain='merchant_amount' AND params->>'merchant_id'=?
                """, String.class, merchant);
        assertThat(reasons).hasSize(3);
        assertThat(reasons).filteredOn(r -> r.contains("另含本期通票赠饮应付 30.00 元")).hasSize(1);
    }

    private String schedule(String route, String counterparty, String direction, long amount, Instant visibleAt, LocalDate deadline) {
        String item = "mss_i_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,?,?,?,?,?,0,?,'CNY',?,?,?,now(),now(),NULL,?)
                """, item, "MSS-" + item, direction, counterparty, route, item, amount, amount, deadline, deadline.toString().substring(0, 7),
                "payable".equals(direction) ? "pending_payment" : "pending_collection", project);
        jdbc.update("""
                INSERT INTO internal_payable_schedules(ar_ap_item_id,route,project_id,counterparty_id,billing_period_start,visible_at,
                    objection_deadline,designated_pay_day,scheduled_due_date,pay_days_rule_id,route_rule_id,objection_working_days)
                VALUES(?,?,?,?,?,?,?,?,?,'ipdr_initial_pay_days',?,10)
                """, item, route, project, counterparty, period, Timestamp.from(visibleAt), deadline, deadline, deadline,
                "ipdr_fixture_" + route);
        return item;
    }

    /** 赠饮核销按内部结算价逐杯挂给出品酒吧的应付（冲正为同来源反向应收），记账期＝核销所在月。 */
    private String drink(String direction, String verification) {
        String item = "mss_d_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,?,?,'pass_drink_settlement',?,3000,0,3000,'CNY',?,?,?,now(),now(),?,NULL)
                """, item, "MSS-" + item, direction, merchant, "mss_v_" + suffix + "_" + verification, period.plusDays(30),
                period.toString().substring(0, 7), "payable".equals(direction) ? "pending_payment" : "pending_collection", merchant);
        return item;
    }
}
