package com.complex.platform.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.ticketing.dto.VerificationRequest;

/**
 * 结算重构批3-3(PRD §6.3) 游乐「当天无限」端到端验收(资金/消耗铁律,含证伪)。
 *
 * <p>口径(2026-07-04 用户拍板):每次核销激活「一个整天无限」——每自然日首刷即消耗一次(扣 total_times、落当天结算价);
 * 同一权益当天再刷判「已激活」放行、不重复扣次不重复计费;跨自然日=新的一天再消耗一次。
 * 单人票 total=1 跨日作废(次日 used 已尽);多次票 total=N 跨日保留(天天各扣一次)。
 *
 * <p>核销主链 now=Instant.now()(不可注入),故:
 * <ul>
 *   <li>「同一自然日再刷」用真实时钟两次核销(必同一自然日) 直接验证。</li>
 *   <li>「跨自然日」用手工预置——插一条过去自然日的 day_activation + 手动把 used_times 推进,模拟"昨天已消耗一天",
 *       再以真实时钟(今天)核销 → 新自然日无激活记录 → 再消耗一次(多次票)或 used_up(单人票)。</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class UnlimitedInDayJdbcIntegrationTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> seededCodes = new ArrayList<>();
    private String deviceCode;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String code : seededCodes) {
            jdbcTemplate.update("DELETE FROM entitlement_day_activations WHERE entitlement_id = ?", "ent_" + code);
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        }
        if (deviceCode != null) {
            jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        }
    }

    // ===== 单人票(total_times=1) =====

    /** ① day1 首刷 → used 0→1 + 落 settlement_price + 建 day_activation。 */
    @Test
    void singleTicketFirstSwipeConsumesOnceAndRecordsDayActivationAndSettlementPrice() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "unlimited_in_day", 1, 5500L);

        var resp = verify(suffix, code);

        assertThat(resp.status()).isEqualTo("success");
        assertThat(usedTimes(code)).isEqualTo(1);
        assertThat(dayActivationCount(code)).isEqualTo(1);
        assertThat(successSettlementPrices(code)).containsExactly(5500L); // virtual→签约价
    }

    /** ② day1 同日再刷 → 放行 used 仍=1、无新结算收入、day_activation 仍 1 条(证伪靶心)。 */
    @Test
    void singleTicketSameDayReswipePassesWithoutConsumingOrNewRevenue() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "unlimited_in_day", 1, 5500L);

        verify(suffix, code);               // day1 首刷
        var replay = verify(suffix, code);  // 同一自然日再刷

        assertThat(replay.status()).isEqualTo("success");   // 放行
        assertThat(usedTimes(code)).isEqualTo(1);           // 不重复扣次
        assertThat(dayActivationCount(code)).isEqualTo(1);  // 当天仅一条激活
        assertThat(successSettlementPrices(code)).containsExactly(5500L); // 仅首刷带价,再刷不新增结算收入
        assertThat(successCount(code)).isEqualTo(2);        // 两条 success(首刷+放行),只一条带价
    }

    /** ③ day2 核销 → ENTITLEMENT_USED_UP(跨日作废,total 已尽)、used 仍=1。 */
    @Test
    void singleTicketNextDayVoidedAsUsedUp() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "unlimited_in_day", 1, 5500L);
        // 模拟 day1 已消耗:used_times 推到 1 + 昨天已有 day_activation。
        seedPastDayConsumed(code, 1, LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("剩余次数已用完");
        assertThat(usedTimes(code)).isEqualTo(1);           // 未再消耗
        // 今天不应留孤儿激活(剩余为 0 时不插激活)。
        assertThat(dayActivationCountOnDate(code, LocalDate.now())).isEqualTo(0);
    }

    // ===== 多次票(total_times=3) =====

    /** ④ day1 首刷 used1 + 同日再刷放行 used 仍1。 */
    @Test
    void multiTicketDay1ConsumesOnceThenSameDayReswipePasses() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "unlimited_in_day", 3, 5500L);

        verify(suffix, code);               // day1 首刷
        assertThat(usedTimes(code)).isEqualTo(1);
        verify(suffix, code);               // day1 再刷
        assertThat(usedTimes(code)).isEqualTo(1);           // 当天不重复扣
        assertThat(dayActivationCountOnDate(code, LocalDate.now())).isEqualTo(1);
    }

    /** ⑤⑥ day2 used2、day3 used3(每天各消耗一次);⑦ day4 used_up。 */
    @Test
    void multiTicketConsumesOncePerDayAcrossDaysThenUsedUp() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "unlimited_in_day", 3, 5500L);
        // 模拟 day1+day2 已各消耗一次(used_times=2、前两天各有激活),今天=第三天。
        seedPastDayConsumed(code, 2, LocalDate.now().minusDays(2));
        jdbcTemplate.update("INSERT INTO entitlement_day_activations (id, entitlement_id, usage_date, first_activated_at) VALUES (?, ?, ?, now())",
                "eda_past2_" + suffix, "ent_" + code, LocalDate.now().minusDays(1));

        // 今天(第三天)核销 → 新自然日再消耗一次 used 2→3。
        var d3 = verify(suffix, code);
        assertThat(d3.status()).isEqualTo("success");
        assertThat(usedTimes(code)).isEqualTo(3);
        assertThat(dayActivationCountOnDate(code, LocalDate.now())).isEqualTo(1);
        // 今天再刷 → 放行不消耗。
        verify(suffix, code);
        assertThat(usedTimes(code)).isEqualTo(3);

        // 模拟到"第四天(新自然日,今天尚无激活)":清掉今天的激活(腾出今天=未激活的新自然日),此时 used=3=total →
        // 新日核销无激活记录 → fall-through → 剩余为 0 → used_up(多次票扣尽后作废)。
        jdbcTemplate.update("DELETE FROM entitlement_day_activations WHERE entitlement_id = ? AND usage_date = ?",
                "ent_" + code, LocalDate.now());
        assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("剩余次数已用完");
        assertThat(usedTimes(code)).isEqualTo(3);
    }

    // ===== 回归:per_use 权益行为不变 =====

    @Test
    void perUseLegacyEntitlementUnchanged() {
        asAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String code = seedEntitlement(suffix, "per_use", 3, 5500L);

        verify(suffix, code);
        verify(suffix, code);

        assertThat(usedTimes(code)).isEqualTo(2);           // 逐次扣,每次都消耗
        assertThat(dayActivationCount(code)).isEqualTo(0);  // per_use 不建激活记录
    }

    // ===== helpers =====

    private void asAdmin() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    private VerificationResponse verify(String suffix, String code) {
        return ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix + "-" + System.nanoTime(), "qr", code, deviceCode, "point_" + suffix, Map.of()));
    }

    private int usedTimes(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
    }

    private int dayActivationCount(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM entitlement_day_activations WHERE entitlement_id = ?", Integer.class, "ent_" + code);
    }

    private int dayActivationCountOnDate(String code, LocalDate date) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM entitlement_day_activations WHERE entitlement_id = ? AND usage_date = ?",
                Integer.class, "ent_" + code, date);
    }

    private int successCount(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ticket_verifications WHERE entitlement_id = ? AND status = 'success'",
                Integer.class, "ent_" + code);
    }

    private List<Long> successSettlementPrices(String code) {
        return jdbcTemplate.queryForList(
                "SELECT settlement_price_cent FROM ticket_verifications "
                        + "WHERE entitlement_id = ? AND status = 'success' AND settlement_price_cent IS NOT NULL "
                        + "ORDER BY created_at",
                Long.class, "ent_" + code);
    }

    private void seedDevice(String suffix) {
        deviceCode = "device_day_" + suffix;
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    /** 模拟"过去若干天已各消耗一次":把 used_times 推到 usedSoFar,并插一条过去自然日的 day_activation。 */
    private void seedPastDayConsumed(String code, int usedSoFar, LocalDate pastDate) {
        jdbcTemplate.update("UPDATE issued_entitlements SET used_times = ? WHERE id = ?", usedSoFar, "ent_" + code);
        jdbcTemplate.update("""
                INSERT INTO entitlement_day_activations (id, entitlement_id, usage_date, first_activated_at)
                VALUES (?, ?, ?, now())
                """, "eda_past_" + code, "ent_" + code, pastDate);
    }

    /**
     * 播游乐当天无限权益:consumption_window 由参数控,total_times 由参数控。
     * kind=virtual + agreement_price(=首刷结算价);entitlement_type='amusement'(非 gift_drink,避免赠饮联动);
     * value_amount=9999(回退基准,virtual 应取 agreement_price 而非它)。venue_id 对 unlimited_in_day 无关但仍设。
     */
    private String seedEntitlement(String suffix, String window, int totalTimes, Long agreementPrice) {
        String code = "day-code-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        String venueId = "venue_day_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'DAY Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, venue_id, entitlement_kind, agreement_price_cent,
                    consumption_window, created_at, updated_at)
                VALUES (?, 'Amuse', 'amusement', ?, 0, 'active', ?, 'virtual', ?, ?, ?, ?)
                """, templateId, totalTimes, venueId, agreementPrice, window, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        // valid 窗口足够宽(±很久),使跨日测试不因有效期先失败——聚焦"当天无限+跨日扣次"机制。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "amusement",
                "merchant_a", venueId, "point_" + suffix, totalTimes, 0, 9999L, "active",
                now.minusSeconds(864000), now.plusSeconds(864000), now, now, 1, null,
                "virtual", agreementPrice, window));
        return code;
    }
}
