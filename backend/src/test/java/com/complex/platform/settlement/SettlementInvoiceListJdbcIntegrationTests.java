package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 批 4b 开票清单(V362)验收:结算单 → 按主体/税率/税额拆出的开票清单。
 *
 * <p>覆盖业务 HTML 逐字口径「开票金额≤已收−已退−已开,退款后先红冲再重开」与
 * 底稿 23_开票清单模板 / PRD 附录 D 的主体×发票项目×税率映射。
 * 走真实 Flyway 迁移库(complex_test);须主会话串行跑(勿与其它 Flyway 集成测试并发)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementInvoiceListJdbcIntegrationTests {

    private static final String PROJECT = "proj_inv_it";
    private static final String BILL = "sb_inv_it_202605";
    private static final String ITEM_A = "si_inv_it_bar_a";
    private static final String ITEM_B = "si_inv_it_bar_b";
    private static final String MERCHANT_A = "mch_inv_it_a";
    private static final String MERCHANT_B = "mch_inv_it_b";
    private static final String ENT_PLATFORM = "be_inv_it_platform";   // 娱乐平台公司
    private static final String ENT_BAR_A = "be_inv_it_bar_a";         // A 酒吧商户结算主体
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 5, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 5, 31);

    /** 门票综合服务费(归娱乐平台公司)。6% 增值税 + 3% 文建费。 */
    private static final long COMP_SERVICE_FEE = 10_000L;
    private static final long SHARE_A = 53_000L;
    private static final long SHARE_B = 17_777L;

    @Autowired
    private SettlementInvoiceService service;

    @Autowired
    private RevenueStreamAttributionRepository attributionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanup();
        seed();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TestSecurityActors.clear();
    }

    // ------------------------------------------------------------------
    // ① 清单按主体拆出来,税率/税额/发票项目名称/凭证要件齐全
    // ------------------------------------------------------------------

    @Test
    void generatesInvoiceListPerSubjectWithRateAndTax() {
        assertThat(service.generateForBill(BILL, "tester")).isEqualTo(3);

        var page = service.list(PROJECT, PERIOD_START, null, null, 1, 50);
        assertThat(page.total()).isEqualTo(3);

        var ticketLine = page.items().stream()
                .filter(i -> "ticket".equals(i.incomeType())).findFirst().orElseThrow();
        // 门票综合服务费 → 娱乐平台公司 / 综合体验服务费 / 6% + 文建费(PRD 附录 D + 底稿 23 第 1 行)
        assertThat(ticketLine.subjectEntityId()).isEqualTo(ENT_PLATFORM);
        assertThat(ticketLine.subjectName()).isEqualTo("娱乐平台公司");
        assertThat(ticketLine.invoiceItemName()).isEqualTo("综合体验服务费");
        assertThat(ticketLine.evidenceRequired()).contains("购票页截图");
        assertThat(ticketLine.amountCent()).isEqualTo(COMP_SERVICE_FEE);
        assertThat(ticketLine.vatRateBps()).isEqualTo(600);
        // 含税 100.00 元、6% → 税额 = 10000 × 600 ÷ 10600 = 566.03… → 566 分(四舍五入到分)
        assertThat(ticketLine.vatAmountCent()).isEqualTo(566L);
        assertThat(ticketLine.culturalFeeApplicable()).isTrue();
        assertThat(ticketLine.culturalFeeCent()).isEqualTo(300L);   // 含税额 × 3%
        assertThat(ticketLine.status()).isEqualTo("pending");

        var barLine = page.items().stream()
                .filter(i -> ITEM_A.equals(i.settlementItemId())).findFirst().orElseThrow();
        // 可分配池分账 → 各酒吧商户 / 运营服务费 / 6%,不重复计文建费(附录 A·C 「文建费不重复计」)
        assertThat(barLine.subjectEntityId()).isEqualTo(ENT_BAR_A);
        assertThat(barLine.subjectName()).isEqualTo("A 酒吧结算主体");
        assertThat(barLine.invoiceItemName()).isEqualTo("运营服务费");
        assertThat(barLine.amountCent()).isEqualTo(SHARE_A);
        assertThat(barLine.vatRateBps()).isEqualTo(600);
        assertThat(barLine.vatAmountCent()).isEqualTo(3_000L);      // 53000 × 600 ÷ 10600
        assertThat(barLine.culturalFeeApplicable()).isFalse();
        assertThat(barLine.culturalFeeCent()).isZero();

        // 主体无归属配置时不静默丢账:B 酒吧未配结算主体 → 主体留空、条目照出
        var orphan = page.items().stream()
                .filter(i -> ITEM_B.equals(i.settlementItemId())).findFirst().orElseThrow();
        assertThat(orphan.subjectEntityId()).isNull();
        assertThat(orphan.subjectName()).isEqualTo("B 酒吧");
        assertThat(orphan.amountCent()).isEqualTo(SHARE_B);

        // 按开票主体分组汇总:财务据此按主体开票
        var summary = service.summarize(PROJECT, PERIOD_START);
        assertThat(summary.subjects()).hasSize(3);
        var platform = summary.subjects().stream()
                .filter(s -> ENT_PLATFORM.equals(s.subjectEntityId())).findFirst().orElseThrow();
        assertThat(platform.amountCent()).isEqualTo(COMP_SERVICE_FEE);
        assertThat(platform.vatAmountCent()).isEqualTo(566L);
        assertThat(summary.overIssued()).isEmpty();

        // 内部转付 / 营销成本永不进对外清单(底稿 23 第 11、12 行)
        assertThat(summary.rules()).anySatisfy(r -> {
            assertThat(r.incomeType()).isEqualTo("giveaway_transfer");
            assertThat(r.invoiceable()).isFalse();
        });
        assertThat(summary.rules()).anySatisfy(r -> {
            assertThat(r.incomeType()).isEqualTo("marketing");
            assertThat(r.invoiceable()).isFalse();
        });
    }

    // ------------------------------------------------------------------
    // ② 开票金额 ≤ 已收 − 已退 − 已开
    // ------------------------------------------------------------------

    @Test
    void regenerateIsIdempotentBecauseInvoiceableAmountIsExhausted() {
        assertThat(service.generateForBill(BILL, "tester")).isEqualTo(3);
        // 已开满 → 可开额 = 应开 − 已开 = 0,再生成不产生任何条目
        assertThat(service.generateForBill(BILL, "tester")).isZero();
        assertThat(service.list(PROJECT, PERIOD_START, null, null, 1, 50).total()).isEqualTo(3);
    }

    @Test
    void refundedBillDoesNotIssueMoreAndIsReportedAsOverIssued() {
        service.generateForBill(BILL, "tester");
        String ticketItemId = ticketItemId();
        service.changeStatus(ticketItemId, "issued", "INV-0001", null, "tester");

        // 退款后月结重跑把综合服务费下调到 4000(已开 10000 > 应开 4000)
        jdbcTemplate.update("UPDATE settlement_bills SET comp_service_fee_exclusive_cent = 4000 WHERE id = ?", BILL);

        assertThat(service.generateForBill(BILL, "tester")).isZero();   // 绝不再多开
        var over = service.summarize(PROJECT, PERIOD_START).overIssued();
        assertThat(over).hasSize(1);
        assertThat(over.get(0).incomeType()).isEqualTo("ticket");
        assertThat(over.get(0).dueAmountCent()).isEqualTo(4_000L);
        assertThat(over.get(0).openAmountCent()).isEqualTo(COMP_SERVICE_FEE);
        assertThat(over.get(0).overIssuedCent()).isEqualTo(6_000L);
    }

    @Test
    void reverseThenReissueMatchesTheReducedAmount() {
        service.generateForBill(BILL, "tester");
        String ticketItemId = ticketItemId();
        service.changeStatus(ticketItemId, "issued", "INV-0001", null, "tester");
        jdbcTemplate.update("UPDATE settlement_bills SET comp_service_fee_exclusive_cent = 4000 WHERE id = ?", BILL);

        // 先红冲(释放已开额度)
        service.changeStatus(ticketItemId, "reversed", null, "退款后红冲", "tester");
        // 再重开:新条目金额 = 下调后的应开额
        assertThat(service.generateForBill(BILL, "tester")).isEqualTo(1);

        var reissued = service.list(PROJECT, PERIOD_START, null, "pending", 1, 50).items().stream()
                .filter(i -> "ticket".equals(i.incomeType())).findFirst().orElseThrow();
        assertThat(reissued.amountCent()).isEqualTo(4_000L);
        assertThat(reissued.id()).isNotEqualTo(ticketItemId);
        assertThat(service.summarize(PROJECT, PERIOD_START).overIssued()).isEmpty();
    }

    // ------------------------------------------------------------------
    // ③ 状态机与前置条件
    // ------------------------------------------------------------------

    @Test
    void draftBillCannotBeInvoiced() {
        jdbcTemplate.update("UPDATE settlement_bills SET status = 'draft' WHERE id = ?", BILL);
        assertThatThrownBy(() -> service.generateForBill(BILL, "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("仅已确认/已结算的结算单可生成开票清单");
    }

    @Test
    void forbiddenStatusTransitionsAreRejected() {
        service.generateForBill(BILL, "tester");
        String ticketItemId = ticketItemId();
        // 待开票不能直接红冲(没开出去的票没什么可冲,只能作废)
        assertThatThrownBy(() -> service.changeStatus(ticketItemId, "reversed", null, null, "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不可从「待开票」流转到「已红冲」");

        service.changeStatus(ticketItemId, "issued", "INV-0002", null, "tester");
        service.changeStatus(ticketItemId, "reversed", null, "红冲", "tester");
        // 已红冲是终态
        assertThatThrownBy(() -> service.changeStatus(ticketItemId, "issued", null, null, "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不可从「已红冲」流转到「已开票」");
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private String ticketItemId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM settlement_invoice_items WHERE bill_id = ? AND income_type = 'ticket'"
                        + " AND status IN ('pending','issuing','issued')", String.class, BILL);
    }

    private void seed() {
        insertEntity(ENT_PLATFORM, "娱乐平台公司");
        insertEntity(ENT_BAR_A, "A 酒吧结算主体");
        insertMerchant(MERCHANT_A, "A 酒吧", ENT_BAR_A);
        insertMerchant(MERCHANT_B, "B 酒吧", null);   // 未配结算主体 → 主体留白

        // 门票综合服务费归属(V355 专列收入流):项目特例指向娱乐平台公司
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                "rsa_inv_it", PROJECT, "ticket_service_fee", ENT_PLATFORM, "active", "tester", Instant.now()));

        jdbcTemplate.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, comp_service_fee_exclusive_cent,
                        distribution_method, status)
                VALUES (?, ?, ?, ?, ?, 'by_rank_tier', 'confirmed')
                """, BILL, PROJECT, PERIOD_START, PERIOD_END, COMP_SERVICE_FEE);
        insertShare(ITEM_A, MERCHANT_A, SHARE_A);
        insertShare(ITEM_B, MERCHANT_B, SHARE_B);
    }

    private void insertEntity(String id, String name) {
        jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)
                VALUES (?, ?, 'management_company', 'active', now(), now()) ON CONFLICT (id) DO NOTHING
                """, id, name);
    }

    private void insertMerchant(String id, String name, String settlementEntityId) {
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, settlement_entity_id, status,
                        created_at, updated_at)
                VALUES (?, ?, 'bar', ?, 'active', now(), now()) ON CONFLICT (id) DO NOTHING
                """, id, name, settlementEntityId);
    }

    private void insertShare(String id, String merchantId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method,
                        calculated_amount_cent, final_amount_cent, status)
                VALUES (?, ?, ?, 'by_rank_tier', ?, ?, 'draft')
                """, id, BILL, merchantId, amountCent, amountCent);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM settlement_invoice_items WHERE bill_id = ?", BILL);
        jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id = ?", BILL);
        jdbcTemplate.update("DELETE FROM settlement_bills WHERE id = ?", BILL);
        jdbcTemplate.update("DELETE FROM revenue_stream_attribution WHERE id = 'rsa_inv_it'");
        jdbcTemplate.update("DELETE FROM merchants WHERE id IN (?, ?)", MERCHANT_A, MERCHANT_B);
        jdbcTemplate.update("DELETE FROM business_entities WHERE id IN (?, ?)", ENT_PLATFORM, ENT_BAR_A);
    }
}
