package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.complex.platform.ticketing.dto.VerificationRequest;

/**
 * 结算重构批3-1(PRD §6.1) 端到端验收：核销成功后 resolveSettlementPrice 按权益结算类别判定结算价并落
 * ticket_verifications.settlement_price_cent。
 *
 * <p>四场景（资金教训，断言真守着"取内部价/签约价"而非空转）：
 * <ol>
 *   <li>实物(physical) + bar_item_ref 指向内部价 5000 的 bar_product → settlement_price=5000（不是 value_amount 9999）。</li>
 *   <li>虚拟(virtual) + agreement_price=8000 → 8000（不是 value_amount 9999）。</li>
 *   <li>实物但 bar_product 内部价为 NULL → 回退 value_amount 7777。</li>
 *   <li>kind 为 NULL（旧数据）→ 回退 value_amount 6666。</li>
 * </ol>
 *
 * <p>走 {@link TicketingService#createVerification}（真提交，与批1 端到端同范式）；@AfterEach 按 suffix 清理落地行。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementPriceResolutionJdbcIntegrationTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> seededCodes = new ArrayList<>();
    private final List<String> seededBarProducts = new ArrayList<>();
    private String deviceCode;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String code : seededCodes) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        }
        for (String pid : seededBarProducts) {
            jdbcTemplate.update("DELETE FROM bar_products WHERE id = ?", pid);
        }
        if (deviceCode != null) {
            jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        }
    }

    @Test
    void physicalEntitlementSettlesAtBarProductInternalPrice() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String barProductId = seedBarProduct(suffix, 5000L); // 内部结算价 5000
        // 实物权益：value_amount=9999（回退基准，若判定空转会取到它），bar_item_ref → 内部价 5000。
        String code = seedEntitlement(suffix, "physical", barProductId, null, 9999L);

        verify(suffix, code);

        assertThat(settlementPrice(code)).isEqualTo(5000L); // 取内部价，不是 value_amount 9999
    }

    @Test
    void virtualEntitlementSettlesAtAgreementPrice() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        // 虚拟权益：agreement_price=8000，value_amount=9999（回退基准）。
        String code = seedEntitlement(suffix, "virtual", null, 8000L, 9999L);

        verify(suffix, code);

        assertThat(settlementPrice(code)).isEqualTo(8000L); // 取签约单价，不是 value_amount 9999
    }

    @Test
    void physicalEntitlementWithoutInternalPriceIsRejectedFailClosed() {
        // S5：商品没录内部结算价 → 拒绝核销，绝不回退票面价 7777
        //（票面原价与内部结算价不可混用；回退会让成本悄悄按售价计，账面平、钱全错）。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String barProductId = seedBarProduct(suffix, null); // 内部结算价 NULL
        String code = seedEntitlement(suffix, "physical", barProductId, null, 7777L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PRICE_UNRESOLVED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_verifications WHERE entitlement_id = ?", Integer.class, "ent_" + code))
                .isZero();
    }

    @Test
    void physicalEntitlementBoundToAnotherBarsProductIsRejectedFailClosed() {
        // S5：A 吧的权益绑到 B 吧的商品 → 成本记给谁全凭巧合 → 不解析、拒核销。
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String barProductId = seedBarProduct(suffix, 5000L, "merchant_other");
        String code = seedEntitlement(suffix, "physical", barProductId, null, 9999L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("ENTITLEMENT_PRICE_UNRESOLVED"));
    }

    @Test
    void nullKindLegacyEntitlementFallsBackToValueAmount() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, null, null, null, 6666L); // kind=NULL 旧数据

        verify(suffix, code);

        assertThat(settlementPrice(code)).isEqualTo(6666L); // 回退 value_amount
    }

    private void verify(String suffix, String code) {
        var response = ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix, "qr", code, deviceCode, "point_" + suffix, Map.of()));
        assertThat(response.status()).isEqualTo("success");
    }

    private Long settlementPrice(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT settlement_price_cent FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Long.class, "ent_" + code);
    }

    private void seedDevice(String suffix) {
        deviceCode = "device_sp_" + suffix;
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, merchant_id, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', 'merchant_a', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    /** 播一个 bar_product，internal_settlement_price_cent 可空。返回其 id（= 权益 bar_item_ref）。 */
    private String seedBarProduct(String suffix, Long internalPrice) {
        return seedBarProduct(suffix, internalPrice, "merchant_a");
    }

    /** S5：商品归属可指定——用于验证「商品不属于本提供方则不解析」。 */
    private String seedBarProduct(String suffix, Long internalPrice, String barId) {
        String pid = "barprod_sp_" + suffix;
        seededBarProducts.add(pid);
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency,
                    stock_deduction_required, status, internal_settlement_price_cent, created_at, updated_at)
                VALUES (?, ?, 'SP Drink', 'drink', 12000, 'CNY', FALSE, 'active', ?, ?, ?)
                """, pid, barId, internalPrice, ts, ts);
        return pid;
    }

    /**
     * 播一个单次商户权益：kind/bar_item_ref/agreement_price/value_amount 由参数控制，覆盖四判定分支。
     * entitlement_type 用 'entry'（非 gift_drink，避免触发赠饮消费者需要 bar 侧联动）。
     */
    private String seedEntitlement(String suffix, String kind, String barItemRef, Long agreementPrice, Long valueAmount) {
        String code = "sp-code-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'SP Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, entitlement_kind, agreement_price_cent,
                    created_at, updated_at)
                VALUES (?, 'Entry', 'entry', 1, 0, 'active', ?, ?, ?, ?, ?)
                """, templateId, barItemRef, kind, agreementPrice, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        // allocated=null（settlement 双写跳过，本类只验 settlement_price 落库，与批1 双写解耦）。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "entry",
                "merchant_a", null, "point_" + suffix, 1, 0, valueAmount, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                kind, agreementPrice));
        return code;
    }
}
