package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BR 段第 357 条 357-R3（BSM:1673「通票分账、单店门票结付、游乐域内结算、餐饮与零售的券结算，一律沿用与积分结算相同的账期：自然月计费、次月出账；
 * 付款落在三个付账日上。」；BSM:1683「每一笔应付都要指定落在哪一个付账日结清」「该笔款项的待付状态须在系统中对收款方可见」；BSM:755「收支两条线，不得轧差」；
 * ENUM:190「非最终快照的数据一律不得进入结算」；第十八批裁决 35「游乐等按综合体逐日查」、69「付款方是平台主体的应收，登记收款前也须付款确认」）。
 *
 * <p>夹具随测试事务回滚：综合体 P 建档北京 2001-01-15，游乐商户 M 经营 P 的游乐项目；域内结算行（357-R2 入场时固化）由夹具直写在 2001 年的营业日上，
 * 入账作业以当前时刻前一小时直调（该月已结束，付款时点按当下推算）；台账经平台财务与商户真实接口查看与登记。修复前作业不入账：各用例第一条断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AmusementDomainSettlementPostingJdbcTests {

    private static final String ROUTE = "amusement_domain_settlement";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AmusementDomainSettlementJob job;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "p357_" + s;
    private final String merchant = "m357_" + s;
    private int seq;
    // 商户专区按 visible_at <= 数据库 now()（测试事务开始时刻）取：入账时刻取早一小时，付款时点仍落在当下（夹具工作日历覆盖今天前后）。
    private final Instant runAt = Instant.now().minusSeconds(3600);

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2001-01-15 00:00:00+08', now())", project, "游乐结算综合体" + s);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'amusement', ?, 'active', now(), now())", merchant, "游乐结算商户" + s, project);
    }

    /**
     * ①⑥⑦：3 月两笔入场各 3000、同月撤销其中一笔，两类数据已标最终：挂一笔游乐公司对 M 的应付 3000（来源与钱路 amusement_domain_settlement、会计期间
     * 2001-03、付款时点已固化、入账留痕一条），三行都标已入；再跑一轮不重复挂。平台财务直接登记付款被付款确认闸拦下；M 的账号在商户专区看到这笔待付。
     */
    @Test
    void monthlyNetPostsOnePayableVisibleToTheMerchantAndGatedByPaymentConfirmation() throws Exception {
        routeRule();
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 31));
        String a = settled(3000, LocalDate.of(2001, 3, 5));
        String b = settled(3000, LocalDate.of(2001, 3, 10));
        reversed(b, -3000, LocalDate.of(2001, 3, 20));

        job.run(runAt);

        String item = itemOf(a);
        assertThat(item).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_domain_settlement_lines WHERE verification_id IN (?, ?)"
                + " AND ar_ap_item_id = ? AND posted_at IS NOT NULL", Integer.class, a, b, item)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT direction||'/'||counterparty_id||'/'||source_type||'/'||amount_cent||'/'||accounting_period||'/'||project_id"
                + "||'/'||(merchant_id IS NULL) FROM ar_ap_items WHERE id = ?", String.class, item))
                .isEqualTo("payable/" + merchant + "/" + ROUTE + "/3000/2001-03/" + project + "/true");
        assertThat(jdbc.queryForObject("SELECT route FROM internal_payable_schedules WHERE ar_ap_item_id = ?", String.class, item)).isEqualTo(ROUTE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'settlement.amusement_domain.posted'"
                + " AND business_type = 'ar_ap_item' AND business_id = ?", Integer.class, item)).isEqualTo(1);

        job.run(runAt);
        assertThat(arApCount()).isEqualTo(1);

        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_357_fin_" + s, "platform_admin", null, null,
                "finance.ar_ap.view", "finance.ar_ap.manage", "finance.payment.register");
        String itemNo = jdbc.queryForObject("SELECT item_no FROM ar_ap_items WHERE id = ?", String.class, item);
        mockMvc.perform(get("/api/v1/finance/ar-ap-items").param("keyword", itemNo).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + item + "')].source_type", contains(ROUTE)));
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + item + "/payments").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"357-pay-" + s + "\",\"amount_cent\":3000,\"payment_method\":\"bank_transfer\","
                                + "\"bank_flow_ref\":\"flow_357_" + s + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));

        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_357_shop_" + s, "staff", null, merchant,
                "merchant.portal.settlement.view");
        mockMvc.perform(get("/api/v1/merchant-portal/ticket-settlements").with(shop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ar_ap_item_id", hasItem(item)));
    }

    /** ②：该综合体 4 月有一天两类数据未标最终，整组不入；标齐后下一轮入账。 */
    @Test
    void monthWithNonFinalDayWaitsUntilMarkedFinal() {
        routeRule();
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 4, 1), LocalDate.of(2001, 4, 14));
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 4, 16), LocalDate.of(2001, 4, 30));
        String a = settled(3000, LocalDate.of(2001, 4, 3));

        job.run(runAt);
        assertThat(arApCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT posted_at IS NULL FROM amusement_domain_settlement_lines WHERE verification_id = ?", Boolean.class, a))
                .isTrue();

        DailyCloseFixture.closeWithMarks(jdbc, "project", project, LocalDate.of(2001, 4, 15), List.of("orders_verifications", "refunds_reversals"));
        job.run(runAt);
        assertThat(arApCount()).isEqualTo(1);
        assertThat(itemOf(a)).isNotNull();
    }

    /** ③：财务还没给该钱路定付账日规则时不入账、行仍未入；定好规则后下一轮入账。 */
    @Test
    void withoutPayDayRuleNothingPostsUntilTheRuleExists() {
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 31));
        String a = settled(3000, LocalDate.of(2001, 3, 8));

        job.run(runAt);
        assertThat(arApCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT posted_at IS NULL FROM amusement_domain_settlement_lines WHERE verification_id = ?", Boolean.class, a))
                .isTrue();

        routeRule();
        job.run(runAt);
        assertThat(arApCount()).isEqualTo(1);
    }

    /** ④⑤：3 月核销、4 月撤销：3 月挂应付 3000，4 月另挂追收应收 3000，不与应付轧差；同在 5 月的核销与撤销净额为 0，不挂台账、行标已入。 */
    @Test
    void crossMonthReversalIsSeparateRecoveryAndSameMonthFullReversalPostsNothing() {
        routeRule();
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 3, 1), LocalDate.of(2001, 5, 31));
        String a = settled(3000, LocalDate.of(2001, 3, 25));
        String recovery = reversed(a, -3000, LocalDate.of(2001, 4, 2));
        String c = settled(2000, LocalDate.of(2001, 5, 6));
        reversed(c, -2000, LocalDate.of(2001, 5, 7));

        job.run(runAt);

        assertThat(itemOf(a)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT direction||'/'||amount_cent||'/'||accounting_period FROM ar_ap_items WHERE id = ?", String.class,
                itemOf(a))).isEqualTo("payable/3000/2001-03");
        String recoveryItem = jdbc.queryForObject("SELECT ar_ap_item_id FROM amusement_domain_settlement_lines WHERE id = ?", String.class, recovery);
        assertThat(jdbc.queryForObject("SELECT direction||'/'||counterparty_id||'/'||amount_cent||'/'||accounting_period||'/'||status FROM ar_ap_items"
                + " WHERE id = ?", String.class, recoveryItem)).isEqualTo("receivable/" + merchant + "/3000/2001-04/pending_collection");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM amusement_domain_settlement_lines WHERE verification_id = ?"
                + " AND ar_ap_item_id IS NULL AND posted_at IS NOT NULL", Integer.class, c)).isEqualTo(2);
        assertThat(arApCount()).isEqualTo(2);
    }

    // ---------- 夹具 ----------

    /** 一行 settled（357-R2 入场时固化），返回票务核销号。 */
    private String settled(long amount, LocalDate businessDate) {
        String verification = "ver357_" + (++seq) + "_" + s;
        line(verification, "settled", amount, businessDate);
        return verification;
    }

    /** 该核销的 reversed 行，返回结算行号。 */
    private String reversed(String verification, long amount, LocalDate businessDate) {
        return line(verification, "reversed", amount, businessDate);
    }

    private String line(String verification, String kind, long amount, LocalDate businessDate) {
        String id = "adsl357_" + kind + "_" + verification;
        jdbc.update("""
                INSERT INTO amusement_domain_settlement_lines (id, verification_id, kind, amusement_project_id, project_id, provider_merchant_id,
                    payer_entity_id, amount_cent, price_catalog_entry_id, business_at, business_date)
                VALUES (?, ?, ?, ?, ?, ?, 'be_amusement', ?, ?, now(), ?)
                """, id, verification, kind, "ap357_" + s, project, merchant, amount, "ipc357_" + s, Date.valueOf(businessDate));
        return id;
    }

    private String itemOf(String verification) {
        return jdbc.queryForObject("SELECT ar_ap_item_id FROM amusement_domain_settlement_lines WHERE verification_id = ? AND kind = 'settled'",
                String.class, verification);
    }

    private int arApCount() {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type = ? AND project_id = ?", Integer.class, ROUTE, project);
    }

    /** 财务给该钱路定一条早已生效的已批规则（次月 1 日出账、指定第 1 个付账日）。 */
    private void routeRule() {
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', ?, 1, 1, DATE '2000-01-01', 'approved', '测试夹具', 'fixture-finance', 'fixture-approver', now())
                """, "ipdr_357_" + s, ROUTE);
    }
}
