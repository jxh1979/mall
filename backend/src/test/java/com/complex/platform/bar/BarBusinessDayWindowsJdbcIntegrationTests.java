package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.support.TestSecurityActors;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 1 条真库回归：ENUM:187「营业日从当日上午 10:00 起算……按每个项目、每家酒吧可配……日结批次……一律以开台或下单时点所属的
 * 营业日为准」；BSM:1682「酒吧仍按每家酒吧配置」。跨酒吧汇总逐家按各自营业日窗取数，开台营业日由服务端按该酒吧营业日确定
 * （请求体按前端 adminClient 蛇形键、不带 business_date）。已批准版本直接落库以固定窗口，审批链见 BarBusinessDayJdbcIntegrationTests。
 * 第十八批 O 段第 126 条 126-R1：日结订台定金源（bar_deposit）同酒吧源逐家按各自营业日窗取数。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBusinessDayWindowsJdbcIntegrationTests {

    /** 营业日 D 与北京 D 11:00：出厂 10:00 起算的酒吧归 D，12:00 起算的酒吧仍归 D−1。 */
    private static final LocalDate D = LocalDate.parse("2015-03-14");
    private static final Instant BEIJING_ELEVEN = Instant.parse("2015-03-14T03:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository bars;
    @Autowired private FinanceService finance;
    @Autowired private BarReceivableLedgerService ledger;
    @Autowired private AnalyticsRepository analytics;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "bbw_prj_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','酒吧营业日综合体','active',now(),now())",
                project);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 同一综合体两家酒吧北京 11:00 各结一笔：无版本的 A（出厂 10:00）归 D，已批准 12:00 的 B 归 D−1。 */
    @Test
    void dailyCloseBarSourceUsesEachBarsOwnBusinessDay() {
        String a = bar("a", null);
        String b = bar("b", 12);
        settledBill(a, 10_000L, BEIJING_ELEVEN);
        settledBill(b, 20_000L, BEIJING_ELEVEN);
        TestSecurityActors.asPlatformAdmin();
        assertThat(barSource(close(D, merchant(a)))).containsExactly(10_000L, 1L);
        assertThat(barSource(close(D, merchant(b)))).containsExactly(0L, 0L);
        assertThat(barSource(close(D.minusDays(1), merchant(b)))).containsExactly(20_000L, 1L);
    }

    /** 订台定金源同酒吧源：两家酒吧北京 11:00 各到账一笔定金，无版本的 A（出厂 10:00）归 D，已批准 12:00 的 B 归 D−1。 */
    @Test
    void dailyCloseBarDepositSourceUsesEachBarsOwnBusinessDay() {
        String a = bar("depa", null);
        String b = bar("depb", 12);
        paidDeposit(a, 20_000L, BEIJING_ELEVEN);
        paidDeposit(b, 30_000L, BEIJING_ELEVEN);
        TestSecurityActors.asPlatformAdmin();
        assertThat(depositSource(close(D, merchant(a)))).containsExactly(20_000L, 1L);
        assertThat(depositSource(close(D, merchant(b)))).containsExactly(0L, 0L);
        assertThat(depositSource(close(D.minusDays(1), merchant(b)))).containsExactly(30_000L, 1L);
    }

    /** B 在北京 11:00 有一张异常账单：只拦 B 的 D−1，不拦 D。 */
    @Test
    void abnormalBillOnlyBlocksItsOwnBarsBusinessDay() {
        String b = bar("b", 12);
        String bill = settledBill(b, 20_000L, BEIJING_ELEVEN);
        jdbc.update("UPDATE bar_bills SET payment_status='abnormal', abnormal_prior_status='paid' WHERE id=?", bill);
        TestSecurityActors.asPlatformAdmin();
        assertThat(catchThrowable(() -> finance.createDailyClose(dailyClose(D, merchant(b))))).isNull();
        assertThatThrownBy(() -> finance.createDailyClose(dailyClose(D.minusDays(1), merchant(b))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_ANOMALY_PENDING"));
    }

    /** 开台不带 business_date：落库营业日按该酒吧已批准版本（起点晚于此刻 → 仍是前一营业日）。 */
    @Test
    void openTableBusinessDateIsDecidedByTheBarsBusinessDay() throws Exception {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23, "北京 23 点后没有更晚的整点起点可配");
        String bar = bar("open", local.getHour() + 1, Instant.now().minus(Duration.ofDays(3)));
        String table = table(bar);
        mockMvc.perform(post("/api/v1/bar/table-sessions").with(barStaff(bar, "bar.table_session.create"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"bbw-open-" + suffix + "\",\"bar_id\":\"" + bar + "\",\"table_id\":\""
                                + table + "\",\"guest_count\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.business_date").value(local.toLocalDate().minusDays(1).toString()));
    }

    /**
     * 储值商户应付（发卡商户是酒吧，BSM:1682「酒吧仍按每家酒吧配置」）：落账日（due_date）与判锁按该酒吧此刻的营业日，
     * 不按所属综合体（无版本＝出厂 10:00）；该营业日已锁日结后再记被拒。
     */
    @Test
    void storedValueMerchantPayableUsesTheBarsBusinessDay() {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23 && local.getMinute() < 58, "北京 23 点后没有更晚的整点起点可配，整点前两分钟内不跑");
        String bar = bar("sv", cutoffDifferingFromFactory(local), Instant.now().minus(Duration.ofDays(3)));
        LocalDate expected = barDayNow(local);
        assertThat(finance.recordStoredValueMerchantPayable(bar, "bbw_svtxn_" + suffix, 1_200L, "bbw_svk_" + suffix).dueDate())
                .isEqualTo(expected);
        TestSecurityActors.asPlatformAdmin();
        close(expected, merchant(bar));
        assertThatThrownBy(() -> finance.recordStoredValueMerchantPayable(bar, "bbw_svtxn2_" + suffix, 1_200L, "bbw_svk2_" + suffix))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_LOCKED"));
    }

    /** 酒吧挂账应收：到期日与账期按该酒吧此刻的营业日起算（营业日＋30 天）。 */
    @Test
    void onAccountReceivableDueDateCountsFromTheBarsBusinessDay() {
        ZonedDateTime local = Instant.now().atZone(LocalZone.ID);
        assumeTrue(local.getHour() < 23 && local.getMinute() < 58, "北京 23 点后没有更晚的整点起点可配，整点前两分钟内不跑");
        String bar = bar("oa", cutoffDifferingFromFactory(local), Instant.now().minus(Duration.ofDays(3)));
        LocalDate expected = barDayNow(local);
        String bill = settledBill(bar, 9_000L, Instant.now());
        BarBillRecord record = bars.findBillById(bill).orElseThrow();
        TestSecurityActors.asPlatformAdmin();
        String item = ledger.createReceivableForOnAccountBill(record, bars.findOrderById(record.orderId()).orElseThrow(),
                "bbw-oa-" + suffix, "挂账", List.of());
        assertThat(jdbc.queryForMap("SELECT due_date::text AS due, accounting_period FROM ar_ap_items WHERE id=?", item))
                .containsEntry("due", expected.plusDays(30).toString())
                .containsEntry("accounting_period", java.time.YearMonth.from(expected).toString());
    }

    /** 看板 ETL 酒吧收入逐商户按各自营业日窗：B（12:00 起算）北京 D 11:00 结的一笔计入 D−1，不计入 D。 */
    @Test
    void dashboardBarRevenueUsesEachBarsOwnWindow() {
        String b = bar("dash", 12);
        String bill = settledBill(b, 20_000L, BEIJING_ELEVEN);
        String space = "bbw_sp_" + suffix;
        jdbc.update("INSERT INTO spaces (id, project_id, space_name, space_type, status, created_at, updated_at)"
                + " VALUES (?, ?, '酒吧营业日空间', 'shop', 'active', now(), now())", space, project);
        jdbc.update("UPDATE bar_tables SET space_id=? WHERE id=(SELECT table_id FROM bar_bills WHERE id=?)", space, bill);
        assertThat(analytics.sumBarRevenueCentsByMerchant(D.minusDays(1), "bar")).containsEntry(b, BigDecimal.valueOf(20_000L));
        assertThat(analytics.sumBarRevenueCentsByMerchant(D, "bar")).doesNotContainKey(b);
    }

    /** 让酒吧此刻的营业日与出厂 10:00 不同的起点：北京 10 点后取下一整点（此刻仍属前一营业日），10 点前取 0 点（此刻已属当日）。 */
    private static int cutoffDifferingFromFactory(ZonedDateTime local) {
        return local.getHour() >= 10 ? local.getHour() + 1 : 0;
    }

    private static LocalDate barDayNow(ZonedDateTime local) {
        return local.getHour() >= 10 ? local.toLocalDate().minusDays(1) : local.toLocalDate();
    }

    private String bar(String tag, Integer cutoffHour) {
        return bar(tag, cutoffHour, Instant.parse("2015-01-01T00:00:00Z"));
    }

    /** 酒吧（业态为酒吧的商户，V267）；cutoffHour 非空时落一条已批准、已生效的营业日版本。 */
    private String bar(String tag, Integer cutoffHour, Instant effectiveAt) {
        String bar = "bbw_bar_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,?,'bar',?,'active',now(),now())",
                bar, "营业日酒吧" + tag, project);
        if (cutoffHour != null) {
            jdbc.update("""
                    INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                      close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                    VALUES(?,?,?,'V0.01',?,0,23,7,0,23,?,'approved','测试已批准版本','bbw_requester',?,'usr_master',?)
                    """, "bbd_" + tag + "_" + suffix, bar, project, cutoffHour, Timestamp.from(effectiveAt),
                    Timestamp.from(effectiveAt.minusSeconds(7200)), Timestamp.from(effectiveAt.minusSeconds(3600)));
        }
        return bar;
    }

    /** 酒吧即商户：账单、结算与日结范围的商户就是酒吧本身。 */
    private static String merchant(String bar) {
        return bar;
    }

    private String table(String bar) {
        String table = "bbw_tbl_" + suffix + "_" + seq.incrementAndGet();
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, merchant(bar), "T-" + table, "营业日卡座", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        return table;
    }

    private String settledBill(String bar, long amountCent, Instant at) {
        int n = seq.incrementAndGet();
        String table = table(bar);
        String order = "bbw_ord_" + suffix + "_" + n;
        String bill = "bbw_bill_" + suffix + "_" + n;
        bars.saveOrder(new BarOrderRecord(order, bar, merchant(bar), table, null, null, "staff", "billed", amountCent, "CNY", "web",
                null, null, at, at));
        bars.saveBill(new BarBillRecord(bill, "BILL-BBW-" + suffix + "-" + n, order, bar, merchant(bar), table, null,
                amountCent, 0L, 0L, amountCent, amountCent, "CNY", "paid", "settled", null, null, at, at));
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_" + bill, bill, "pos", null, "POS-" + bill,
                amountCent, merchant(bar), at));
        return bill;
    }

    /** 一笔在 at 由一体机登记到账的订台定金。 */
    private void paidDeposit(String bar, long amountCent, Instant at) {
        jdbc.update("INSERT INTO bar_reservations (id, bar_id, merchant_id, table_id, reserved_at, guest_count, deposit_amount_cent,"
                + " deposit_status, deposit_channel, deposit_paid_at, order_source, status, created_at, updated_at)"
                + " VALUES (?,?,?,?,?,2,?,'paid','cash',?,'kiosk','reserved',?,?)",
                "bbw_res_" + suffix + "_" + seq.incrementAndGet(), bar, merchant(bar), table(bar), Timestamp.from(at), amountCent,
                Timestamp.from(at), Timestamp.from(at), Timestamp.from(at));
    }

    private DailyCloseRequest dailyClose(LocalDate day, String merchantId) {
        return new DailyCloseRequest("bbw-close-" + suffix + "-" + seq.incrementAndGet(), "日结", List.of(), day, "merchant",
                merchantId, false);
    }

    private String close(LocalDate day, String merchantId) {
        return finance.createDailyClose(dailyClose(day, merchantId)).id();
    }

    private List<Long> barSource(String dailyCloseId) {
        return jdbc.queryForObject("SELECT amount_cent, order_count FROM finance_daily_close_summaries WHERE daily_close_id=? AND source_stream='bar'",
                (rs, n) -> List.of(rs.getLong(1), rs.getLong(2)), dailyCloseId);
    }

    /** 订台定金源（金额、笔数）；没有该源的汇总行时为空。 */
    private List<Long> depositSource(String dailyCloseId) {
        return jdbc.query("SELECT amount_cent, order_count FROM finance_daily_close_summaries WHERE daily_close_id=? AND source_stream='bar_deposit'",
                (rs, n) -> List.of(rs.getLong(1), rs.getLong(2)), dailyCloseId).stream().flatMap(List::stream).toList();
    }

    /** 酒吧员工（商户范围＝该酒吧）。 */
    private RequestPostProcessor barStaff(String bar, String... permissions) {
        String userId = "bbw_staff_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', ?, ?, 'active', now())", userId, userId, project, bar);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '酒吧营业日回归', 'merchant', 'active', now())", role, role);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "merchant_staff", project, bar, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
