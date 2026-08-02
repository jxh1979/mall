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
 * 结算重构批3-2(PRD §6.2) 酒吧入场资格「场次规则」端到端验收(资金/消耗铁律，含证伪)。
 *
 * <p>用例：
 * <ol>
 *   <li>营业时段内首次入场 → 消耗一次(used_times 0→1) + 建入场记录 + 落 settlement_price。</li>
 *   <li>同场次二次核销 → 放行但 used_times 仍=1、不新增结算收入(入场记录仍 1 条、无第二条带 settlement_price 的 success)。</li>
 *   <li>该权益已在旧场次入过场,新场次再核销 → ENTRY_PASS_PAST_SESSION_VOID 拒绝、used_times 不变。</li>
 *   <li>非营业时段(venue 无营业时间表) → VENUE_NOT_IN_SESSION。</li>
 *   <li>consumption_window=per_use 老权益 → 行为与改动前一致(逐次扣,回归)。</li>
 * </ol>
 *
 * <p>营业时间表播 7×(00:00→23:59) 覆盖任意"当前时刻",使场次解析稳定命中(不依赖跑测时间)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SessionEntryPassJdbcIntegrationTests {

    @Autowired
    private TicketingService ticketingService;

    @Autowired
    private JdbcTicketingRepository ticketingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> seededCodes = new ArrayList<>();
    private final List<String> seededVenues = new ArrayList<>();
    private String deviceCode;

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String code : seededCodes) {
            jdbcTemplate.update("DELETE FROM entitlement_entry_records WHERE entitlement_id = ?", "ent_" + code);
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_verifications WHERE entitlement_id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id = ?", "ent_" + code);
            jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", "order_" + code);
            jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", "et_" + code);
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", "tt_" + code);
        }
        for (String venue : seededVenues) {
            jdbcTemplate.update("DELETE FROM entitlement_entry_records WHERE session_id IN (SELECT id FROM venue_sessions WHERE venue_id = ?)", venue);
            jdbcTemplate.update("DELETE FROM venue_sessions WHERE venue_id = ?", venue);
            jdbcTemplate.update("DELETE FROM venue_business_hours WHERE venue_id = ?", venue);
        }
        if (deviceCode != null) {
            jdbcTemplate.update("DELETE FROM terminal_devices WHERE id = ?", deviceCode);
        }
    }

    @Test
    void firstEntryConsumesOnceAndRecordsEntryAndSettlementPrice() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String venueId = seedOpenVenue(suffix);
        String code = seedEntitlement(suffix, "per_session", venueId, 5500L);

        var resp = verify(suffix, code);

        assertThat(resp.status()).isEqualTo("success");
        assertThat(usedTimes(code)).isEqualTo(1);            // 消耗一次
        assertThat(entryRecordCount(code)).isEqualTo(1);     // 建入场记录
        assertThat(successSettlementPrices(code)).containsExactly(5500L); // 落 settlement_price(virtual→签约价 5500)
    }

    @Test
    void reentrySameSessionPassesWithoutConsumingOrNewRevenue() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String venueId = seedOpenVenue(suffix);
        String code = seedEntitlement(suffix, "per_session", venueId, 5500L);

        verify(suffix, code);                 // 首入
        var reentry = verify(suffix, code);   // 同场次二次核销

        assertThat(reentry.status()).isEqualTo("success");     // 放行
        assertThat(usedTimes(code)).isEqualTo(1);              // used_times 仍=1(不重复消耗)
        assertThat(entryRecordCount(code)).isEqualTo(1);       // 入场记录仍 1 条
        // 结算铁律：仅首入那条 success 带 settlement_price；二次入场 success 不新增结算收入(settlement_price 为 null)。
        assertThat(successSettlementPrices(code)).containsExactly(5500L);
        assertThat(successCount(code)).isEqualTo(2);           // 两条 success(首入+二次放行),但只一条带价
    }

    @Test
    void entryInNewSessionAfterPastSessionIsVoided() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String venueId = seedOpenVenue(suffix);
        String code = seedEntitlement(suffix, "per_session", venueId, 5500L);
        // 预置：该权益曾在"昨天那场"入过场(手工插一条旧场次+入场记录)。
        seedPastSessionEntry(suffix, venueId, code);

        // 今天(营业中)再核销 → 今日场次 != 旧场次 → 过场作废。
        assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("入场凭证已失效");
        assertThat(usedTimes(code)).isEqualTo(0);  // 未消耗
    }

    @Test
    void nonBusinessHoursRejectsVenueNotInSession() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String venueId = "venue_closed_" + suffix; // 不播营业时间表 → 任何时刻都非营业
        seededVenues.add(venueId);
        String code = seedEntitlement(suffix, "per_session", venueId, 5500L);

        assertThatThrownBy(() -> verify(suffix, code))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("营业场次");
        assertThat(usedTimes(code)).isEqualTo(0);
    }

    @Test
    void perUseLegacyEntitlementUnchanged() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        String suffix = Long.toString(System.nanoTime());
        seedDevice(suffix);
        String venueId = "venue_closed_" + suffix; // 即便无营业表,per_use 也不该走场次逻辑
        seededVenues.add(venueId);
        String code = seedEntitlement(suffix, "per_use", venueId, 5500L);

        var resp = verify(suffix, code);

        assertThat(resp.status()).isEqualTo("success");
        assertThat(usedTimes(code)).isEqualTo(1);            // 逐次扣,行为不变
        assertThat(entryRecordCount(code)).isEqualTo(0);     // per_use 不建入场记录
    }

    // ===== helpers =====

    private VerificationResponse verify(String suffix, String code) {
        return ticketingService.createVerification(new VerificationRequest(
                "idem-" + suffix + "-" + System.nanoTime(), "qr", code, deviceCode, "point_" + suffix, Map.of()));
    }

    private int usedTimes(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT used_times FROM issued_entitlements WHERE id = ?", Integer.class, "ent_" + code);
    }

    private int entryRecordCount(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM entitlement_entry_records WHERE entitlement_id = ?", Integer.class, "ent_" + code);
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
        deviceCode = "device_ses_" + suffix;
        java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', ?, ?)
                """, deviceCode, "dev-" + deviceCode, "DC-" + deviceCode, ts, ts);
    }

    /** 播一个 7×(00:00→23:59) 全天营业的 venue，保证任意跑测时刻都在营业窗口内。 */
    private String seedOpenVenue(String suffix) {
        String venueId = "venue_open_" + suffix;
        seededVenues.add(venueId);
        for (int dow = 0; dow <= 6; dow++) {
            jdbcTemplate.update("""
                    INSERT INTO venue_business_hours (id, venue_id, day_of_week, open_time, close_time, is_draft)
                    VALUES (?, ?, ?, '00:00:00', '23:59:59', FALSE)
                    """, "vbh_" + venueId + "_" + dow, venueId, dow);
        }
        return venueId;
    }

    /** 手工插一条"旧场次"(昨日)+该权益在旧场次的入场记录,模拟"曾在别的场次入过场"。 */
    private void seedPastSessionEntry(String suffix, String venueId, String code) {
        String oldSession = "vses_old_" + suffix;
        java.time.OffsetDateTime yesterday = Instant.now().minusSeconds(86400).atOffset(java.time.ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO venue_sessions (id, venue_id, session_date, opened_at, status)
                VALUES (?, ?, ?, ?, 'closed')
                """, oldSession, venueId, LocalDate.now().minusDays(2), yesterday);
        jdbcTemplate.update("""
                INSERT INTO entitlement_entry_records (id, entitlement_id, session_id, first_entry_at)
                VALUES (?, ?, ?, ?)
                """, "eer_old_" + suffix, "ent_" + code, oldSession, yesterday);
    }

    /**
     * 播一个单次入场资格权益：consumption_window 由参数控。venue_id 决定场次归属。
     * kind=virtual + agreement_price=agreementPrice(=首入结算价，避免与 value_amount 混淆)。
     * entitlement_type='entry'(非 gift_drink，避免赠饮 bar 联动)。
     */
    private String seedEntitlement(String suffix, String window, String venueId, Long agreementPrice) {
        String code = "ses-code-" + suffix;
        seededCodes.add(code);
        Instant now = Instant.now();
        java.sql.Timestamp ts = java.sql.Timestamp.from(now);
        String typeId = "tt_" + code;
        String templateId = "et_" + code;
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'SES Type', 'fixed', FALSE, 'active', ?, ?)
                """, typeId, ts, ts);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, venue_id, entitlement_kind, agreement_price_cent,
                    consumption_window, created_at, updated_at)
                VALUES (?, 'Entry', 'entry', 1, 0, 'active', ?, 'virtual', ?, ?, ?, ?)
                """, templateId, venueId, agreementPrice, window, ts, ts);
        ticketingRepository.saveTicketOrder(new TicketOrderRecord(
                "order_" + code, "ON-" + code, "cust_" + code, typeId, null, 1,
                LocalDate.parse("2026-06-18"), "web", null, 0L, "CNY", "paid", "issued", "created", now, now));
        // total_times=1(单次)；value_amount=9999(回退基准,virtual 应取 agreement_price 而非它)；venue_id 决定场次。
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(
                "ent_" + code, code, typeId, "order_" + code, templateId, "entry",
                "merchant_a", venueId, "point_" + suffix, 1, 0, 9999L, "active",
                now.minusSeconds(60), now.plusSeconds(3600), now, now, 1, null,
                "virtual", agreementPrice, window));
        return code;
    }
}
