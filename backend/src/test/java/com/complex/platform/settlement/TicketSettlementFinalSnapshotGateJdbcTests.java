package com.complex.platform.settlement;

// 文件版本：V0.01

import static com.complex.platform.support.DailyCloseFixture.markRangeFinal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.FinanceService;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
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
 * 第十八批 V 段第 159 条 159-R2 通票月度分账闸真库回归。依据：ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」
 * 「最迟不晚于该营业日闭店时点所在自然日的次日 24:00」；BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算」；
 * 第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」（单位口径先例）。
 *
 * <p>建档时刻随测试事务回滚（回填到 2001 年的在营单位留库会拦下平台口径结算 2013-09、2009 年的既有用例）。铺底：综合体 P 与 P 下酒吧 B
 * 建档北京 2001-01-15 00:00；P 下酒吧 C 建档北京 2001-03-20 12:00（晚于 3-19 营业日结束 3-20 10:00，3-19 及以前不查）。3 月 P 整月、
 * C 3-20 起五类标齐；B 除 3-10、3-15 外五类标齐，这两天由持 finance.daily_close.lock、负责范围覆盖 P 的平台侧财务 BP 经真实生成、标记接口
 * （蛇形键）生成日结：3-10 缺退款与冲正，3-15 缺出品与物品交接（分账只要订单与核销、退款与冲正两类，不拦）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TicketSettlementFinalSnapshotGateJdbcTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private TicketDistributionSettlementService service;
    @Autowired private JdbcSettlementBillRepository repository;
    @Autowired private SettlementPostingService postingService;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    private final String project = "p159r2_" + s;
    private final String barB = "b159r2_" + s;
    private final String barBName = "分账酒吧乙" + s;
    private final String barC = "c159r2_" + s;
    private RequestPostProcessor financeBp;

    // 修复前 settleMonth 不判标记：不抛，第一条断言即红。
    @Test
    void settleMonthRejectsMonthWithNonFinalVerificationOrRefundDayThenSettlesOnceMarked() throws Exception {
        String close10 = seedMarch();
        seedExclusiveFact("m", Instant.parse("2001-03-12T04:00:00Z"), 1200L);

        assertThatThrownBy(() -> service.settleMonth(project, YearMonth.of(2001, 3)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("SETTLEMENT_DATA_NOT_FINAL");
                    assertThat(e.getMessage()).isEqualTo("以下营业日数据尚未标记为最终，不能结算通票分账：2001-03-10 " + barBName
                            + "：当日退款与冲正记录");
                });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement_bills WHERE project_id = ? AND period_start = DATE '2001-03-01'",
                Integer.class, project)).isZero();

        finalMark(close10, "refunds_reversals", "m10");
        String billId = service.settleMonth(project, YearMonth.of(2001, 3));
        assertThat(exclusiveAmount(billId)).isEqualTo(1200L);
    }

    // 修复前作业逐月照结：2001-01～03 的平台口径单都出，第一条断言即红。
    @Test
    void monthlyJobSkipsMonthsWithNonFinalDaysAndSettlesFinalMonth() throws Exception {
        seedMarch();
        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 4, 1), LocalDate.of(2001, 4, 30));
        markRangeFinal(jdbc, "bar", barB, LocalDate.of(2001, 4, 1), LocalDate.of(2001, 4, 30));
        markRangeFinal(jdbc, "bar", barC, LocalDate.of(2001, 4, 1), LocalDate.of(2001, 4, 30));
        seedExclusiveFact("a", Instant.parse("2001-04-12T04:00:00Z"), 3400L);

        // UTC 2001-05-02 19:00＝北京 5-03 03:00：上月取 2001-04，回看 2000-11～2001-04；过账闸关。
        new SettlementMonthlyJob(service, repository, postingService,
                Clock.fixed(Instant.parse("2001-05-02T19:00:00Z"), ZoneOffset.UTC), true, false).scheduledSettle();

        // 1、2 月：P、B 建档后的营业日未标；3 月：B 3-10 缺退款与冲正——都跳过不出单。
        for (String start : new String[] {"2001-01-01", "2001-02-01", "2001-03-01"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM settlement_bills WHERE project_id IS NULL AND period_start = ?::date",
                    Integer.class, start)).as(start).isZero();
        }
        String april = jdbc.queryForObject("SELECT id FROM settlement_bills WHERE project_id IS NULL AND period_start = DATE '2001-04-01'",
                String.class);
        assertThat(exclusiveAmount(april)).isEqualTo(3400L);
    }

    /** 共用铺底；返回 B 在 3-10 缺退款与冲正那张日结的编号。 */
    private String seedMarch() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at)"
                + " VALUES (?, 'complex', ?, 'active', TIMESTAMPTZ '2001-01-15 00:00:00+08', now())", project, "分账综合体" + s);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2001-01-15 00:00:00+08', now())", barB, barBName, project);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', ?, 'active', TIMESTAMPTZ '2001-03-20 12:00:00+08', now())", barC, "分账酒吧丙" + s, project);
        financeBp = MediaPerformanceTestFixtures.staff(jdbc, "usr_159r2_bp_" + s, project, null, "finance.daily_close.lock");

        markRangeFinal(jdbc, "project", project, LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 31));
        markRangeFinal(jdbc, "bar", barB, LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 9));
        markRangeFinal(jdbc, "bar", barB, LocalDate.of(2001, 3, 11), LocalDate.of(2001, 3, 14));
        markRangeFinal(jdbc, "bar", barB, LocalDate.of(2001, 3, 16), LocalDate.of(2001, 3, 31));
        markRangeFinal(jdbc, "bar", barC, LocalDate.of(2001, 3, 20), LocalDate.of(2001, 3, 31));
        String close10 = closeAndMarkAllBut(LocalDate.of(2001, 3, 10), "refunds_reversals");
        closeAndMarkAllBut(LocalDate.of(2001, 3, 15), "production_handover");
        return close10;
    }

    /** B 在该营业日经真实生成接口生成日结，再经真实标记接口标除 missing 外的四类（蛇形键，每次 200）。 */
    private String closeAndMarkAllBut(LocalDate day, String missing) throws Exception {
        String closeId = JsonPath.read(mockMvc.perform(post("/api/v1/finance/daily-closes").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r2g-" + s + "-" + day + "\",\"business_date\":\"" + day
                                + "\",\"scope_type\":\"bar\",\"scope_id\":\"" + barB + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        for (String category : FinanceService.FINAL_MARK_CATEGORIES.keySet()) {
            if (!category.equals(missing)) {
                finalMark(closeId, category, day.getDayOfMonth() + category);
            }
        }
        return closeId;
    }

    private void finalMark(String closeId, String category, String tag) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes/" + closeId + "/final-marks").with(financeBp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"r2m-" + s + "-" + tag + "\",\"category\":\"" + category + "\"}"))
                .andExpect(status().isOk());
    }

    /** B 的一笔专属直付核销事实（写法同 SettlementStateMachineJdbcIntegrationTests.seedSettledFact）。 */
    private void seedExclusiveFact(String tag, Instant businessAt, long amount) {
        String ver = "v159r2_" + tag + "_" + s;
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        jdbc.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,
                    merchant_id, project_id, exclusive_merchant_id, settlement_status, entitlement_valid_to,
                    created_at, updated_at, business_at)
                VALUES (?, ?, 'verification_settled', ?, ?, 'entry', 1, 2, 1, ?, ?, 0, 'merchant',
                    ?, ?, ?, 'draft', ?, ?, ?, ?)
                """, "st_vs_" + ver, "ST-VS-" + ver, "ent_" + ver, ver, amount, amount, barB, project, barB, ts, ts, ts, ts);
    }

    private long exclusiveAmount(String billId) {
        return jdbc.queryForObject("SELECT final_amount_cent FROM settlement_items WHERE bill_id = ? AND merchant_id = ?"
                + " AND distribution_method = 'exclusive_full'", Long.class, billId, barB);
    }
}
