// 文件版本：V0.04
package com.complex.platform.support;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FS-021 测试夹具：结算过账按「财务指定付账日 + 异议期 + 银行工作日」推算到期日，缺规则或日历即拒绝过账。
 * 夹具给结算过账涉及的钱路各种一条早已生效的已批规则（次月 1 日出账、指定第 1 个付账日），并把今天前后一段日期记为
 * 已审批的工作日与银行工作日（与已有测试同一张日历表，冲突即沿用已有行）。精确日期用例另用远期年份自行种日历。
 */
public final class InternalPayDayFixtures {

    public static final String[] SETTLEMENT_ROUTES = {
            "ticket_distribution", "ticket_distribution_recovery", "platform_incentive_subsidy", "single_bar_service_fee",
            "compensation_fulfillment", "compensation_fulfillment_recovery", "parking_credit_cost",
            "pass_domain_share", "pass_domain_direct_settlement", "pass_unverified_attribution"};

    private InternalPayDayFixtures() {
    }

    public static void seed(JdbcTemplate jdbc) {
        for (String route : SETTLEMENT_ROUTES) {
            jdbc.update("""
                    INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                        submitted_by, decided_by, decided_at)
                    VALUES (?, 'route', ?, 1, 1, DATE '2000-01-01', 'approved', '测试夹具', 'fixture-finance', 'fixture-approver', now())
                    ON CONFLICT (id) DO NOTHING
                    """, "ipdr_fixture_" + route, route);
        }
        LocalDate today = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        allWorking(jdbc, today.minusDays(10), today.plusDays(150));
    }

    /** 把 [from, to] 记为已审批的工作日与银行工作日（已有行不改）。 */
    public static void allWorking(JdbcTemplate jdbc, LocalDate from, LocalDate to) {
        jdbc.update("""
                INSERT INTO ticket_points_calendar_cases (id, days, source_note, status, created_by, created_at, approved_at)
                VALUES ('tpcc_fixture_internal_pay_days', '[]', '测试夹具：给定每日事实', 'approved', 'fixture', now(), now())
                ON CONFLICT (id) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO ticket_points_calendar_days (calendar_date, working_day, bank_working_day, case_id)
                SELECT d::date, TRUE, TRUE, 'tpcc_fixture_internal_pay_days' FROM generate_series(?::date, ?::date, interval '1 day') d
                ON CONFLICT (calendar_date) DO NOTHING
                """, Date.valueOf(from), Date.valueOf(to));
    }
}
