package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.support.TestSecurityActors;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算重构批2-4：月度分账结算单过账到 finance 应付台账集成验收。
 *
 * <p>直插 settlement_bills(confirmed) + settlement_items(draft，含 final=0 一条) → 调
 * {@link SettlementPostingService#postBill} → 断言 ar_ap_items(payable/ticket_distribution/金额)、
 * 明细回填 ar_ap_item_id + status=posted、final=0 跳过、结算单 status=settled；幂等；draft 单拒绝。
 *
 * <p>非 @Transactional（postBill 内部 @Transactional 真提交）；@AfterEach 按 bill 前缀清理落地行。
 * 每个用例用独立 bill id（含 nanoTime）隔离。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementBatch2PostingTests {

    @Autowired
    private SettlementPostingService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> billIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 过账走 FinanceService.createArApItem(@GuardedAction)——需登录主体过审计/鉴权。
        // 平台管理员:merchant scope 空(与 bar 域过账测试同口径),无终端开关/准入策略拦截。
        TestSecurityActors.asPlatformAdmin();
        // FS-021：过账到期日按财务指定付账日推算，缺规则/日历即拒绝过账——种已批规则与工作日历。
        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate);
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String billId : billIds) {
            // ar_ap_items 为不可变台账(DB 触发器禁 DELETE),不清理;source_id=nanoTime 明细 id 唯一,
            // 遗留行不与后续用例冲突。仅清结算单/明细即可。
            jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id = ?", billId);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE id = ?", billId);
        }
    }

    @Test
    void postConfirmedBillCreatesPayablesBacksfillsItemsAndSettlesBill() {
        String billId = seedBill("confirmed");
        String i1 = seedItem(billId, "m1", 12800L);
        String i2 = seedItem(billId, "m2", 9600L);
        String iZero = seedItem(billId, "m3", 0L); // final=0 → 跳过，不过账

        int posted = service.postBill(billId);
        assertThat(posted).isEqualTo(2); // 只过 final>0 两条

        // ar_ap 生成:payable/ticket_distribution/金额对/counterparty=merchant。
        Map<String, Object> a1 = arApBySource(i1);
        assertThat(a1.get("direction")).isEqualTo("payable");
        assertThat(a1.get("source_type")).isEqualTo("ticket_distribution");
        assertThat(((Number) a1.get("amount_cent")).longValue()).isEqualTo(12800L);
        assertThat(a1.get("counterparty_id")).isEqualTo("m1");
        assertThat(((Number) arApBySource(i2).get("amount_cent")).longValue()).isEqualTo(9600L);

        // 明细回填 + posted;final=0 那条仍 draft、无 ar_ap。
        assertThat(itemStatus(i1)).isEqualTo("posted");
        assertThat(itemArApId(i1)).isNotNull();
        assertThat(itemStatus(i2)).isEqualTo("posted");
        assertThat(itemStatus(iZero)).isEqualTo("draft");
        assertThat(itemArApId(iZero)).isNull();
        assertThat(arApCountBySource(iZero)).isZero(); // final=0 不过账

        // 全过 → 单 settled + settled_at 落。
        assertThat(billStatus(billId)).isEqualTo("settled");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT settled_at FROM settlement_bills WHERE id = ?", java.sql.Timestamp.class, billId)).isNotNull();
    }

    @Test
    void repeatedPostIsIdempotent() {
        String billId = seedBill("confirmed");
        String i1 = seedItem(billId, "m1", 5000L);

        service.postBill(billId);
        String firstArAp = itemArApId(i1);
        // 已 settled，重复 postBill 被拒(非 confirmed)——先断言拒绝，再断言不产生第二条 ar_ap。
        assertThatThrownBy(() -> service.postBill(billId))
                .hasMessageContaining("只有已确认的账单才能过账");

        assertThat(arApCountBySource(i1)).isEqualTo(1); // 仍 1 条,不重复
        assertThat(itemArApId(i1)).isEqualTo(firstArAp);
    }

    @Test
    void draftBillIsRejected() {
        String billId = seedBill("draft"); // 超总池待特批
        String i1 = seedItem(billId, "m1", 9999L);

        assertThatThrownBy(() -> service.postBill(billId))
                .hasMessageContaining("只有已确认的账单才能过账");

        assertThat(itemStatus(i1)).isEqualTo("draft");     // 未过账
        assertThat(arApCountBySource(i1)).isZero();          // 无 ar_ap
        assertThat(billStatus(billId)).isEqualTo("draft"); // 单不变
    }

    /**
     * FS-021-R1（BSM L755「一律按总额收付、收支两条线，不得轧差」；BSM L804「每一段冲回都生成反向结算记录」）：
     * 负额明细（向商户追回）过账为该商户的单独追收应收，与他家/本家应付分别收付，不再拒绝过账、也不从应付里扣。
     */
    @Test
    void negativeItemPostsAsSeparateRecoveryReceivable() {
        String billId = seedBill("confirmed");
        String pos = seedItem(billId, "m1", 5000L);
        String neg = seedItem(billId, "m2", -3000L); // 向 m2 追回 30 元

        java.util.concurrent.atomic.AtomicInteger posted = new java.util.concurrent.atomic.AtomicInteger();
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> posted.set(service.postBill(billId))))
                .as("负额明细不再拒绝过账").isNull();
        assertThat(posted.get()).isEqualTo(2);

        Map<String, Object> payable = arApBySource(pos);
        assertThat(payable.get("direction")).isEqualTo("payable");
        assertThat(((Number) payable.get("amount_cent")).longValue()).isEqualTo(5000L);
        Map<String, Object> recovery = jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = 'ticket_distribution_recovery' AND source_id = ?", neg);
        assertThat(recovery.get("direction")).isEqualTo("receivable");
        assertThat(((Number) recovery.get("amount_cent")).longValue()).isEqualTo(3000L);
        assertThat(recovery.get("counterparty_id")).isEqualTo("m2");
        assertThat(itemStatus(pos)).isEqualTo("posted");
        assertThat(itemStatus(neg)).isEqualTo("posted");
        assertThat(itemArApId(neg)).isEqualTo(recovery.get("id"));
        assertThat(billStatus(billId)).isEqualTo("settled");

        // 重放：已 settled 被拒，不新增第二笔追收。
        assertThatThrownBy(() -> service.postBill(billId)).hasMessageContaining("只有已确认的账单才能过账");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = 'ticket_distribution_recovery' AND source_id = ?",
                Integer.class, neg)).isEqualTo(1);
    }

    // ---- helpers ----

    private String seedBill(String status) {
        String billId = "sb_b2p_" + System.nanoTime();
        billIds.add(billId);
        jdbcTemplate.update(
                "INSERT INTO settlement_bills (id, project_id, period_start, period_end,"
                + " ticket_revenue_cent, available_pool_cent, distributed_cent,"
                + " distribution_method, status)"
                + " VALUES (?, ?, DATE '2013-05-01', DATE '2013-05-31', 100000, 80000, 22400,"
                + " 'by_rank_tier', ?)",
                billId, "proj_b2p_" + System.nanoTime(), status);
        return billId;
    }

    private String seedItem(String billId, String merchantId, long finalCent) {
        String itemId = "si_b2p_" + System.nanoTime() + "_" + merchantId;
        jdbcTemplate.update(
                "INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method,"
                + " calculated_amount_cent, final_amount_cent, status)"
                + " VALUES (?, ?, ?, 'by_rank_tier', ?, ?, 'draft')",
                itemId, billId, merchantId, finalCent, finalCent);
        return itemId;
    }

    private Map<String, Object> arApBySource(String itemId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = 'ticket_distribution' AND source_id = ?", itemId);
    }

    private int arApCountBySource(String itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = 'ticket_distribution' AND source_id = ?",
                Integer.class, itemId);
    }

    private String itemStatus(String itemId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_items WHERE id = ?", String.class, itemId);
    }

    private String itemArApId(String itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT ar_ap_item_id FROM settlement_items WHERE id = ?", String.class, itemId);
    }

    private String billStatus(String billId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_bills WHERE id = ?", String.class, billId);
    }
}
