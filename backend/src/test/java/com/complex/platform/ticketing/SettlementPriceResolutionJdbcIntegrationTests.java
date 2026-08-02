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
 * 结算重构批3-1(PRD §6.1) + 批10/E(V379/V380 内部结算 SKU 目录) 端到端验收：核销成功后
 * resolveSettlementPrice 判定结算价并固化落 ticket_verifications.settlement_price_cent。
 *
 * <p>批10/E 起提供方兑付类(实物等)取价统一收口目录：bar_product 须挂目录引用(catalog_item_code)、
 * 价取目录生效行；没挂引用 = 没录内部价 → fail-closed 拒核销。非提供方兑付类(入场等虚拟)行为不变
 * (发放快照/回退 value_amount)。
 *
 * <p>压轴用例 catalogPriceChangeDoesNotMoveAlreadyVerifiedMoney 是「历史不受改价影响」的资金证明：
 * 草案不生效 → 他人复核转正 → 已核销一分不动 → 之后的核销按新目录价固化 → 快照被传播。
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

    @Autowired
    private com.complex.platform.settlement.SettlementConfigWriteService configWriteService;

    @Autowired
    private InternalSettlementPriceResolver priceResolver;

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
            jdbcTemplate.update("DELETE FROM internal_price_catalog WHERE item_code = ?", "sku_bp_" + pid);
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

    /**
     * 批10/E「历史不受改价影响」资金证明（接替原批5 updateAgreementPrice 用例；该端点已随目录收编下线）。
     *
     * <p>目录口径（业务 HTML「内部结算价目录」节）四步全链：
     * <ol>
     *   <li>目录生效价 8000 的商品权益 A、B 各发一张；核销 A → 固化 8000（这笔已发生）。</li>
     *   <li>起草人把该条目改成 3000 —— <b>草案不生效</b>：解析器仍解析 8000；起草人自审转正 → 403。</li>
     *   <li>他人复核转正 → 已核销的 A 仍是 8000（改价倒不动历史账）；商品快照列被传播成 3000；
     *       旧生效行软替换留库 = 价格版本史。</li>
     *   <li>此后核销 B → 按<b>当时的目录价 3000</b> 固化（改价对后续核销生效——目录口径「核销才是固化时点」
     *       取代了旧「按发放时快照」口径）。</li>
     * </ol>
     */
    @Test
    void catalogPriceChangeDoesNotMoveAlreadyVerifiedMoney() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String barProductId = seedBarProduct(suffix, 8000L); // 目录生效价 8000
        String itemCode = "sku_bp_" + barProductId;
        String codeA = seedEntitlement(suffix, "physical", barProductId, null, 9999L);
        String templateId = "et_" + codeA;
        String codeB = seedSiblingEntitlement(suffix, codeA, templateId, "physical", null, 9999L);

        // ① 改价前核销 A —— 固化 8000，这笔钱已经发生。
        verify(suffix + "-a", codeA);
        assertThat(settlementPrice(codeA)).isEqualTo(8000L);

        // ② 起草改价 3000：落的是草案，不生效——解析器此刻仍是 8000。
        configWriteService.saveInternalPriceItem(
                new com.complex.platform.settlement.SettlementConfigWriteRequests.InternalPriceItemRequest(
                        "idem-sku-" + suffix, itemCode, "merchant_a", "spirit", "SP Drink", 3000L),
                "drafter_sp_it");
        assertThat(priceResolver.require(barProductId, "merchant_a", null)).isEqualTo(8000L);

        // ②b 起草人不得自审转正（差人守卫）。
        String draftId = jdbcTemplate.queryForObject(
                "SELECT id FROM internal_price_catalog WHERE item_code = ? AND is_draft = TRUE AND status = 'active'",
                String.class, itemCode);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                configWriteService.activateDraft("internal_price_item", draftId, "drafter_sp_it"))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("SELF_ACTIVATION_FORBIDDEN"));

        // ③ 他人复核转正：目录换生效价 + 快照传播；已核销的 A 一分不动。
        configWriteService.activateDraft("internal_price_item", draftId, "approver_sp_it");
        assertThat(settlementPrice(codeA)).isEqualTo(8000L); // 已发生的不可改
        assertThat(jdbcTemplate.queryForObject(
                "SELECT internal_settlement_price_cent FROM bar_products WHERE id = ?", Long.class, barProductId))
                .isEqualTo(3000L); // 快照传播（改一次，对后续生效）
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM internal_price_catalog WHERE item_code = ? AND status = 'inactive'",
                Integer.class, itemCode)).isEqualTo(1); // 旧生效行软替换留库 = 价格版本史

        // ④ 此后核销 B：按当时的目录价 3000 固化。
        verify(suffix + "-b", codeB);
        assertThat(settlementPrice(codeB)).isEqualTo(3000L);
    }

    /** 在既有模板/订单下再发一张权益（改价前已发放，用于验证改价后核销按目录当时价固化）。 */
    private String seedSiblingEntitlement(String suffix, String siblingCode, String templateId,
            String kind, Long agreementPrice, Long valueAmount) {
        String code = "sp-code-b-" + suffix;
        // 插队首：B 复用 A 的订单/模板，必须先清 B 的权益行，否则清 A 时删 ticket_orders 撞 FK。
        seededCodes.add(0, code);
        Instant now = Instant.now();
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, "tt_" + siblingCode, "order_" + siblingCode, templateId, "entry",
                "merchant_a", null, "point_" + suffix, 1, 0, valueAmount, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                kind, agreementPrice, null, null, null, 0, null, null));
        return code;
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

    /**
     * S5：商品归属可指定——用于验证「商品不属于本提供方则不解析」。
     * 批10/E：有内部价的商品按 V380 收编形态播种——目录生效条目(sku_bp_ + 商品id) + 商品挂引用 + 快照列同值；
     * 无内部价 = 不挂引用（解析 fail-closed，与旧 NULL 内部价同语义）。
     */
    private String seedBarProduct(String suffix, Long internalPrice, String barId) {
        String pid = "barprod_sp_" + suffix;
        seededBarProducts.add(pid);
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        String itemCode = internalPrice == null ? null : "sku_bp_" + pid;
        jdbcTemplate.update("""
                INSERT INTO bar_products (id, bar_id, product_name, product_type, price_cent, currency,
                    stock_deduction_required, status, internal_settlement_price_cent, catalog_item_code,
                    created_at, updated_at)
                VALUES (?, ?, 'SP Drink', 'drink', 12000, 'CNY', FALSE, 'active', ?, ?, ?, ?)
                """, pid, barId, internalPrice, itemCode, ts, ts);
        if (itemCode != null) {
            seedMerchant(barId);
            jdbcTemplate.update("""
                    INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name,
                        price_cent, status, is_draft, updated_by, created_at, updated_at)
                    VALUES (?, ?, ?, 'spirit', 'SP Drink', ?, 'active', FALSE, 'seed_sp_it', now(), now())
                    """, itemCode, itemCode, barId, internalPrice);
        }
        return pid;
    }

    /** 目录条目有 merchants FK：确保测试商户真实存在（共享 id，ON CONFLICT 幂等，不清理防撞他类 FK）。 */
    private void seedMerchant(String merchantId) {
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, 'bar', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, merchantId, "SP测试商户-" + merchantId);
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
                java.time.LocalDate.parse("2026-06-18"), "web", null, 0L, null, 0L, 0L, "CNY", "paid", "issued", "created", now, now, null, null, null));
        // allocated=null（settlement 双写跳过，本类只验 settlement_price 落库，与批1 双写解耦）。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "entry",
                "merchant_a", null, "point_" + suffix, 1, 0, valueAmount, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                kind, agreementPrice, null, null, null, 0, null, null));
        return code;
    }
}
