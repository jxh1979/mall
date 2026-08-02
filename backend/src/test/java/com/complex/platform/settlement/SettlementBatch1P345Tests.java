package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算重构批1 第3步(对账)+第4步(未核销归项目)集成验收。
 *
 * <p>直接对拍真实业务表(非走核销主链):seed issued_entitlements(+第3步 seed settlement_transactions),
 * 调 {@link SettlementReconciliationJob#reconcile}/{@link SettlementExpiryJob#expireUnverified} 可调方法,
 * 断言 settlement_reconciliation_status / settlement_transactions(unverified_expired)结果。
 *
 * <p>非 @Transactional(与 SettlementDoubleWriteEndToEndTests 同):expireUnverified 内部 @Transactional
 * 真提交,@AfterEach 按 suffix 清理落地行,避免测试事务回滚遮蔽。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementBatch1P345Tests {

    @Autowired
    private SettlementReconciliationJob reconciliationJob;

    @Autowired
    private SettlementExpiryJob expiryJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> entIds = new ArrayList<>();
    private final List<LocalDate> reconDates = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String ent : entIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", ent);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", ent);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + ent);
        }
        for (LocalDate d : reconDates) {
            jdbcTemplate.update("DELETE FROM settlement_reconciliation_status WHERE reconciliation_date = ?",
                    java.sql.Date.valueOf(d));
        }
    }

    // ---- 第3步:对账 ----

    @Test
    void reconcileWritesStatusRowWithNewApproxOld() {
        String suffix = Long.toString(System.nanoTime());
        // 用一个远离今天的确定日,避开并发会话/真实数据落同日窗口。
        LocalDate day = LocalDate.of(2011, 3, 7);
        reconDates.add(day);
        Instant noon = day.atTime(12, 0).toInstant(ZoneOffset.UTC);

        // 旧口径:issued.allocated=10001 落当日;新口径:settlement verified+unverified=10001 落当日。
        String ent = seedIssuedEntitlement(suffix, 2, 1, 10001L, noon);
        seedVerificationSettledTxn(ent, 10001L, 5001L, 5000L, noon);

        reconciliationJob.reconcile(day);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_reconciliation_status WHERE reconciliation_date = ?",
                java.sql.Date.valueOf(day));
        long oldCent = ((Number) row.get("old_ticket_allocated_cent")).longValue();
        long newCent = ((Number) row.get("new_settlement_cent")).longValue();
        long variance = ((Number) row.get("variance_cent")).longValue();
        assertThat(oldCent).isEqualTo(10001L);
        assertThat(newCent).isEqualTo(10001L);   // verified+unverified=allocated -> new≈old
        assertThat(variance).isEqualTo(0L);      // variance = old - new
    }

    // ---- 第4步:未核销款分流(S8:默认归娱乐平台公司,游乐业态归游乐公司)----

    @Test
    void expireUnverifiedAllocatesUnverifiedPartToPlatformEntity() {
        String suffix = Long.toString(System.nanoTime());
        Instant now = Instant.now();
        // valid_to 已过、total=4/used=1 -> 未核销 3/4:unverified = 8000×3/4 = 6000,verified=2000。
        String ent = seedIssuedEntitlement(suffix, 4, 1, 8000L, now.minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                java.sql.Timestamp.from(now.minus(1, ChronoUnit.HOURS)), ent);

        int written = expiryJob.expireUnverified(now);
        assertThat(written).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type = 'unverified_expired'",
                ent);
        // S8(业务 HTML 逐字「没有被核销的权益,那部分钱归娱乐平台公司留存」):归主体而非项目公司;
        //   提供方商户(merchant_a)无 merchants 行 → 业态兜底 bar → all_pass_ticket 流 → 娱乐平台公司。
        assertThat(row.get("beneficiary_type")).isEqualTo("entity");
        assertThat(row.get("beneficiary_entity_id")).isEqualTo("be_ent_platform");
        assertThat(row.get("settlement_status")).isEqualTo("confirmed");
        assertThat(row.get("transaction_no")).isEqualTo("ST-UE-" + ent);
        assertThat(row.get("verification_id")).isNull();
        assertThat(((Number) row.get("verified_count")).intValue()).isEqualTo(1);
        long allocated = ((Number) row.get("allocated_revenue_cent")).longValue();
        long verified = ((Number) row.get("verified_amount_cent")).longValue();
        long unverified = ((Number) row.get("unverified_amount_cent")).longValue();
        assertThat(allocated).isEqualTo(8000L);
        assertThat(unverified).isEqualTo(6000L);          // 8000 × 3/4
        assertThat(verified).isEqualTo(2000L);
        assertThat(verified + unverified).isEqualTo(allocated);
    }

    @Test
    void expireUnverifiedIsIdempotentOnRepeatRun() {
        String suffix = Long.toString(System.nanoTime());
        Instant now = Instant.now();
        String ent = seedIssuedEntitlement(suffix, 4, 1, 8000L, now.minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                java.sql.Timestamp.from(now.minus(1, ChronoUnit.HOURS)), ent);

        expiryJob.expireUnverified(now);
        expiryJob.expireUnverified(now);   // 第二次:ON CONFLICT DO NOTHING

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ? AND transaction_type = 'unverified_expired'",
                Integer.class, ent);
        assertThat(count).isEqualTo(1);   // 仍 1 条(幂等)
    }

    @Test
    void expireSkipsDirectIssuedWithNullAllocated() {
        String suffix = Long.toString(System.nanoTime());
        Instant now = Instant.now();
        // allocated=null(direct 出票)-> 查询 WHERE allocated IS NOT NULL 排除,不归项目。
        String ent = seedIssuedEntitlement(suffix, 4, 1, null, now.minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                java.sql.Timestamp.from(now.minus(1, ChronoUnit.HOURS)), ent);

        expiryJob.expireUnverified(now);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ?",
                Integer.class, ent);
        assertThat(count).isEqualTo(0);   // 本次没把这张 direct 票归主体
    }

    /** S8 分流(业务 HTML 逐字「游乐通票的未核销权益款归游乐公司」):提供方是游乐业态商户 → be_amusement。 */
    @Test
    void expireUnverifiedRoutesAmusementProviderToAmusementEntity() {
        String suffix = Long.toString(System.nanoTime());
        Instant now = Instant.now();
        String amuMerchant = "m_ue_amu_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, '游乐测试商户', 'amusement', 'active', now(), now())
                ON CONFLICT (id) DO UPDATE SET merchant_type = EXCLUDED.merchant_type
                """, amuMerchant);
        try {
            String ent = seedIssuedEntitlement(suffix, 4, 1, 8000L, now.minus(2, ChronoUnit.DAYS));
            jdbcTemplate.update("UPDATE issued_entitlements SET valid_to = ?, merchant_id = ? WHERE id = ?",
                    java.sql.Timestamp.from(now.minus(1, ChronoUnit.HOURS)), amuMerchant, ent);

            expiryJob.expireUnverified(now);

            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT * FROM settlement_transactions WHERE entitlement_id = ?"
                            + " AND transaction_type = 'unverified_expired'", ent);
            // 业态判定源 = merchants.merchant_type(与 S11 池基过滤同一套),流 amusement_ticket → V375 种子。
            assertThat(row.get("beneficiary_type")).isEqualTo("entity");
            assertThat(row.get("beneficiary_entity_id")).isEqualTo("be_amusement");
            assertThat(row.get("merchant_id")).as("溯源商户保留").isEqualTo(amuMerchant);
        } finally {
            cleanup(); // 先清引用该商户的流水/权益,再删商户(FK 顺序)
            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", amuMerchant);
        }
    }

    /** S8 fail-loud:归属种子被删(有金额没有收款方=配置坏)→ 拒绝落账,零行落地,宁可阻断不出错账。 */
    @Test
    void expireUnverifiedFailsLoudWhenAttributionSeedMissing() {
        String suffix = Long.toString(System.nanoTime());
        Instant now = Instant.now();
        String ent = seedIssuedEntitlement(suffix, 4, 1, 8000L, now.minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update("UPDATE issued_entitlements SET valid_to = ? WHERE id = ?",
                java.sql.Timestamp.from(now.minus(1, ChronoUnit.HOURS)), ent);
        // 临时停用全局 all_pass_ticket 归属种子(模拟配置缺失);finally 还原,不泄漏给后续用例。
        jdbcTemplate.update("UPDATE revenue_stream_attribution SET status = 'inactive'"
                + " WHERE revenue_stream = 'all_pass_ticket' AND project_id IS NULL AND is_draft = FALSE");
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> expiryJob.expireUnverified(now))
                    .isInstanceOf(com.complex.platform.common.api.ApiException.class)
                    .hasMessageContaining("未核销权益款无法落账");
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM settlement_transactions WHERE entitlement_id = ?",
                    Integer.class, ent);
            assertThat(count).isZero(); // 事务整体回滚,一分钱没落
        } finally {
            jdbcTemplate.update("UPDATE revenue_stream_attribution SET status = 'active'"
                    + " WHERE revenue_stream = 'all_pass_ticket' AND project_id IS NULL AND is_draft = FALSE");
        }
    }

    // ---- seed helpers(直插业务表,不依赖 ticketing 服务) ----

    /** 直插 issued_entitlements 一行(created_at 可控,用于对账日窗)。返回 entitlement id。 */
    private String seedIssuedEntitlement(String suffix, int totalTimes, int usedTimes,
                                         Long allocatedCent, Instant createdAt) {
        String ent = "ent_b1p345_" + suffix;
        entIds.add(ent);
        String typeId = "tt_b1p345_" + suffix;
        String templateId = "et_b1p345_" + suffix;
        java.sql.Timestamp ct = java.sql.Timestamp.from(createdAt);
        java.sql.Timestamp validFrom = java.sql.Timestamp.from(createdAt.minusSeconds(60));
        java.sql.Timestamp validTo = java.sql.Timestamp.from(createdAt.plusSeconds(3600));
        // ticket_types / entitlement_templates 有外键,需先建。
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'B1P345 Type', 'fixed', FALSE, 'active', ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, typeId, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, 'Entry', 'entry', ?, 0, 'active', NULL, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, templateId, totalTimes, ct, ct);
        String orderId = "order_" + ent;
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, visit_date,
                    sales_channel, amount_cent, currency, pay_status, issue_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, NULL, 'web', 0, 'CNY', 'paid', 'issued', 'created', ?, ?)
                ON CONFLICT (id) DO NOTHING
                """, orderId, "ON-" + ent, typeId, totalTimes, ct, ct);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                    entitlement_code, entitlement_type, merchant_id, venue_id, point_id,
                    total_times, used_times, value_amount_cent, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent)
                VALUES (?, ?, ?, ?, ?, 'entry', 'merchant_a', NULL, ?, ?, ?, 0, 'active', ?, ?, ?, ?, 1, ?)
                """, ent, typeId, orderId, templateId, "code_" + suffix,
                "point_" + suffix, totalTimes, usedTimes, validFrom, validTo, ct, ct, allocatedCent);
        return ent;
    }

    /** 直插一条 verification_settled settlement_transactions(created_at 可控,用于对账新口径)。 */
    private void seedVerificationSettledTxn(String entId, long allocated, long verified, long unverified,
                                            Instant createdAt) {
        java.sql.Timestamp ct = java.sql.Timestamp.from(createdAt);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id,
                    settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, 'entry', 1, 2, 1, ?, ?, ?, 'merchant', 'merchant_a',
                    'draft', ?, ?, ?)
                """, "st_vs_" + entId, "ST-VS-" + entId, entId, allocated, verified, unverified,
                ct, ct, ct);
    }
}
