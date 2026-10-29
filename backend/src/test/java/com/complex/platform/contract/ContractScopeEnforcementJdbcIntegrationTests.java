package com.complex.platform.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarOrderRequest;
import com.complex.platform.bar.BarProductRecord;
import com.complex.platform.bar.BarService;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.TicketTypeRecord;
import com.complex.platform.ticketing.TicketingRepository;
import com.complex.platform.ticketing.TicketingService;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同经营范围越界拦截 真 PostgreSQL 端到端集成测试（真合同 JSONB + 真下单路径 + Spring 真实装配
 * ContractScopeGuard 字段注入）：
 * ① 酒吧下单：生效合同 rent_terms.allowedCategories 只许 beer → 卖 spirit 整单拒、零落库；beer 照常成单。
 * ② 票务出票（单店票挂酒吧商户）：合同范围不含 ticketing → 下单拒且不扣票种库存（拒单置于库存扣减前）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class ContractScopeEnforcementJdbcIntegrationTests {

    private static final String MERCHANT = "mch_scope_jdbc_bar";

    @Autowired
    private JdbcContractRepository contractRepository;

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private BarService barService;

    @Autowired
    private TicketingRepository ticketingRepository;

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpActor() {
        TestSecurityActors.asMerchantUser(MERCHANT);
    }

    @AfterEach
    void clearActor() {
        TestSecurityActors.clear();
    }

    private void seedEffectiveContract(List<String> allowedCategories) {
        Instant now = Instant.now();
        contractRepository.saveContract(new ContractRecord(
                "ctr_scope_jdbc", MERCHANT, "subj_scope_jdbc", null, "subj_scope_jdbc",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                Map.of("businessScope", "酒吧经营", "allowedCategories", allowedCategories),
                null, "none", 0L, Map.of(), List.of(), null, null, List.of(),
                "effective", null, null, null, null, now, now));
    }

    @Test
    void barOrderBlockedWhenContractScopeExcludesCategoryAndAllowedCategorySellsThrough() {
        seedEffectiveContract(List.of("beer"));
        Instant now = Instant.now();
        barRepository.saveTable(new BarTableRecord(
                "table_scope_jdbc", "bar_scope_jdbc", MERCHANT, "T-SCOPE", "范围桌", "card_seat",
                6, 2, 0L, "CNY", "active", now, now));
        barRepository.saveProduct(new BarProductRecord(
                "product_scope_spirit", "bar_scope_jdbc", MERCHANT, "金酒", "spirit", 8800L,
                "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));
        barRepository.saveProduct(new BarProductRecord(
                "product_scope_beer", "bar_scope_jdbc", MERCHANT, "精酿", "beer", 3800L,
                "CNY", null, null, false, "active", now, now, null, null, null, false, null, null, null));

        // 越界品类 → 整单拒（CONTRACT_SCOPE_VIOLATION），bar_orders 零落库。
        assertThatThrownBy(() -> barService.createOrder(new BarOrderRequest(
                "scope-jdbc-x", null, List.of(), "bar_scope_jdbc", "table_scope_jdbc", null,
                List.of(Map.of("product_id", "product_scope_spirit", "quantity", 1)), List.of(), "offline")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION");
                    assertThat(e.getMessage()).contains("烈酒");
                });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_orders WHERE bar_id = 'bar_scope_jdbc'", Long.class)).isZero();

        // 范围内品类照常成单（真 JSONB 读回 → 拦截器不误伤）。
        BarOrderRecord ok = barService.createOrder(new BarOrderRequest(
                "scope-jdbc-ok", null, List.of(), "bar_scope_jdbc", "table_scope_jdbc", null,
                List.of(Map.of("product_id", "product_scope_beer", "quantity", 2)), List.of(), "offline"));
        assertThat(ok.totalAmountCent()).isEqualTo(7600L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM bar_orders WHERE id = ?", Long.class, ok.id())).isEqualTo(1L);
    }

    @Test
    void ticketOrderBlockedWhenContractScopeExcludesTicketingAndStockUntouched() {
        seedEffectiveContract(List.of("bar", "beer"));   // 不含 ticketing → 出票域级拒。
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at) "
                        + "VALUES (?, ?, 'bar', 'active', now(), now()) ON CONFLICT (id) DO NOTHING",
                MERCHANT, "范围测试酒吧");
        ticketingRepository.saveTicketType(new TicketTypeRecord(
                "tt_scope_jdbc", "范围测试单店票", "weekday_weekend_holiday", 9900L, 9900L, 9900L,
                false, "published", null, null, now, now, 10, 0, null, false, MERCHANT, TicketTypeRecord.NO_COMBO_DISCOUNT));

        assertThatThrownBy(() -> ticketingService.createTicketOrder(new TicketOrderRequest(
                "scope-jdbc-ticket-x", null, List.of(), "tt_scope_jdbc", null, 1,
                LocalDate.now(), "admin", null, null, Map.of(), MERCHANT)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("CONTRACT_SCOPE_VIOLATION"));
        // 拒单置于库存扣减前：sold_quantity 不动。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT sold_quantity FROM ticket_types WHERE id = 'tt_scope_jdbc'", Integer.class)).isZero();
    }
}
