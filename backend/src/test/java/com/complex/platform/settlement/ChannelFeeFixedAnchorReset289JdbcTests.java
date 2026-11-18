package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批 AW 段 289-R1（ENUM:51「任何票种经第三方渠道售出时，销售渠道费一律先从整单扣除且只扣一次」）：固定额销售渠道费的票数按「本行发生时这张票
 * 是否还有别的仍有效核销」逐行判定——首笔核销被撤销后客人再核销，这张票重新计一张；跨月冲正由原月重算之差追收，合计仍只扣一次。
 *
 * <p>美团售出的游乐通票（每张固定 300），核销与撤销走真实结算消费者；按项目、按月取销售渠道汇总。非 @Transactional（消费者 REQUIRES_NEW），@AfterEach 清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class ChannelFeeFixedAnchorReset289JdbcTests {

    private static final YearMonth MONTH = YearMonth.of(2014, 5); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(10).atTime(12, 0).toInstant(ZoneOffset.UTC);

    @Autowired private SettlementSettledConsumer settled;
    @Autowired private SettlementReversedConsumer reversed;
    @Autowired private JdbcSettlementBillRepository bills;
    @Autowired private JdbcTemplate jdbc;

    private final String key = Long.toHexString(System.nanoTime());
    private final String project = "prj_c289_" + key;
    private final String ticketType = "tt_c289_" + key;
    private final String park = "m_c289_" + key;
    private final String order = "o_c289_" + key;
    private final String ticket = "tk_c289_" + key;
    private int seq;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "渠道费锚点综合体" + key);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'amusement', ?, 'active', now(), now())", park, "渠道费锚点游乐" + key, project);
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '渠道费锚点通票', 'fixed_price', FALSE, 'published', now(), now())""", ticketType);
        Timestamp sold = Timestamp.from(AT.minusSeconds(86_400L));
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id, paid_at)
                VALUES (?, ?, ?, 1, 'meituan', 2000, 'CNY', 'paid', 'issued', 'created', ?, ?, ?, ?)""",
                order, order, ticketType, sold, sold, project, sold);
        jdbc.update("""
                INSERT INTO ticket_order_channel_fees (order_id, project_id, sales_channel, sales_fee_config_id, sales_fee_bps,
                    sales_fixed_amount_cent, sales_fixed_at) VALUES (?, ?, 'meituan', ?, 0, 300, ?)""",
                order, project, "cfc_c289_" + key, sold);
    }

    @AfterEach
    void cleanup() {
        AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_order_channel_fees WHERE order_id = ?", order);
        jdbc.update("DELETE FROM ticket_orders WHERE id = ?", order);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", ticketType);
        jdbc.update("DELETE FROM merchants WHERE id = ?", park);
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    /** ① 同月：首笔核销 A 撤销后客人再核销 B——这张票计一张，固定额扣一道。 */
    @Test
    void reverifyAfterTheOnlyVerificationIsReversedCountsTheTicketAgain() {
        String a = settle(AT, 1, 1);
        reverse(a);
        settle(AT.plusSeconds(3_600L), 1, 1);

        assertThat(ticketCount(MONTH, null)).as("撤销后再核销，这张票的固定额照扣一道").isEqualTo(1L);
    }

    /** ② 跨月：A 在上月，撤销与 B 在本月——上月按原月重算净 0，本月计 1，合计只扣一次。 */
    @Test
    void crossMonthReversalMovesTheTicketToTheMonthOfTheNewVerification() {
        String a = settle(AT, 1, 1);
        reverse(a);
        Instant next = MONTH.plusMonths(1).atDay(5).atTime(12, 0).toInstant(ZoneOffset.UTC);
        AppendOnlyTestBypass.update(jdbc, "UPDATE settlement_transactions SET business_at = ? WHERE verification_id = ?"
                + " AND transaction_type = 'verification_reversed'", Timestamp.from(next), a);
        settle(next.plusSeconds(3_600L), 1, 1);

        Instant cutoff = MONTH.plusMonths(2).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        assertThat(ticketCount(MONTH, cutoff)).as("原月重算：核销与其撤销抵为 0").isZero();
        assertThat(ticketCount(MONTH.plusMonths(1), null)).as("本月再核销计一张").isEqualTo(1L);
    }

    /** ③ 两次票 A、B 都有效时撤销 A：这张票仍有有效核销，票数不减。 */
    @Test
    void reversingOneOfTwoLiveVerificationsKeepsTheTicketCounted() {
        String a = settle(AT, 2, 1);
        settle(AT.plusSeconds(3_600L), 2, 2);
        reverse(a);

        assertThat(ticketCount(MONTH, null)).as("B 仍有效，固定额不退").isEqualTo(1L);
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    /** 游乐入场核销走结算消费者真路径，返回核销 id。 */
    private String settle(Instant at, int total, int used) {
        String verification = "v_c289_" + (++seq) + "_" + key;
        settled.onSettlementSettled(new SettlementSettledEvent(verification, at, "ent_c289_" + key, order, ticketType,
                "amusement", park, total, used, 1, 1000L, at.plusSeconds(86_400L * 30), null, null, null, project, ticket));
        return verification;
    }

    /** 撤销核销走结算消费者真路径（原月未过账 → 冲正行落原核销时刻）。 */
    private void reverse(String verification) {
        reversed.onVerificationReversed(new VerificationReversedEvent(verification, "r_c289_" + (++seq) + "_" + key,
                "ent_c289_" + key, order, park, 1));
    }

    private long ticketCount(YearMonth month, Instant recoveryCutoff) {
        Instant start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<JdbcSettlementBillRepository.ChannelRevenue> rows = bills.aggregateChannelRevenue(project, start, end, recoveryCutoff);
        return rows.stream().filter(r -> "meituan".equals(r.salesChannel()))
                .mapToLong(JdbcSettlementBillRepository.ChannelRevenue::ticketCount).sum();
    }
}
