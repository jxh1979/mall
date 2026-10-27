package com.complex.platform.support;

// 文件版本：V0.01

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批第 2 条：测试夹具——售票时逐张固化的酒吧通票可分配池（pass_pool_snapshots）。
 *
 * <p>引擎只按池快照逐份分账；直插核销流水的用例给父票补一行快照，毛额即池（各扣项为 0）、单人单次，第一段名单按固定金额。
 * 售票时固化本身见 PassPoolSnapshotJdbcIntegrationTests。
 */
public final class PassPoolFixtures {

    /** 出厂分账方案（ENUM L52 参与家数档位表 1～10 家，第一段名单为空）。 */
    public static final String FACTORY_SCHEME = "tdsv_factory";

    private PassPoolFixtures() {
    }

    /** 一张单人单次池快照票，按出厂方案、无第一段，覆盖给定酒吧。 */
    public static void snapshot(JdbcTemplate jdbc, String ticketId, String projectId, long poolCent, String... coveredBars) {
        snapshot(jdbc, ticketId, projectId, poolCent, FACTORY_SCHEME, List.of(coveredBars), Map.of());
    }

    /** 一张单人单次池快照票：按给定方案版本，覆盖给定酒吧，firstSegmentFixed 为快照里的第一段名单（每份固定金额）。 */
    public static void snapshot(JdbcTemplate jdbc, String ticketId, String projectId, long poolCent, String schemeVersionId,
                                List<String> coveredBars, Map<String, Long> firstSegmentFixed) {
        jdbc.update("""
                INSERT INTO pass_pool_snapshots (ticket_instance_id, order_id, project_id, slots, visits, bar_gross_cent,
                    route_fee_cent, sales_channel_fee_cent, gift_cost_cent, service_fee_cent, pool_cent, scheme_version_id,
                    covered_bar_ids)
                VALUES (?, ?, ?, 1, 1, ?, 0, 0, 0, 0, ?, ?, CAST(? AS text[]))""",
                ticketId, "o_" + ticketId, projectId, poolCent, poolCent, schemeVersionId,
                "{" + String.join(",", coveredBars) + "}");
        firstSegmentFixed.forEach((merchant, amount) -> jdbc.update("""
                INSERT INTO pass_pool_snapshot_first_segment (ticket_instance_id, merchant_id, rule_type, amount_cent)
                VALUES (?, ?, 'fixed', ?)""", ticketId, merchant, amount));
    }

    /** 一张单人单次池快照票：酒吧份额 grossCent、门票综合服务费 serviceFeeCent，池＝两者之差，出厂方案、无第一段，覆盖给定酒吧。 */
    public static void snapshotWithServiceFee(JdbcTemplate jdbc, String ticketId, String projectId, long grossCent,
                                              long serviceFeeCent, String... coveredBars) {
        jdbc.update("""
                INSERT INTO pass_pool_snapshots (ticket_instance_id, order_id, project_id, slots, visits, bar_gross_cent,
                    route_fee_cent, sales_channel_fee_cent, gift_cost_cent, service_fee_cent, pool_cent, scheme_version_id,
                    covered_bar_ids)
                VALUES (?, ?, ?, 1, 1, ?, 0, 0, 0, ?, ?, ?, CAST(? AS text[]))""",
                ticketId, "o_" + ticketId, projectId, grossCent, serviceFeeCent, grossCent - serviceFeeCent, FACTORY_SCHEME,
                "{" + String.join(",", coveredBars) + "}");
    }

    /** 某结算单酒吧业态份额行的净份额（无池快照的核销按 ENUM L52 末句另整笔单列未分配池余额）。 */
    public static long barNetCent(JdbcTemplate jdbc, String billId) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(net_revenue_cent), 0) FROM settlement_bill_format_shares"
                + " WHERE bill_id = ? AND business_format = 'bar'", Long.class, billId);
    }

    /** 本项目一版已生效（2000 年起）的分账方案：参与家数行按给定比例（万分比，下标 0＝第 1 名）。 */
    public static String scheme(JdbcTemplate jdbc, String id, String projectId, Map<Integer, int[]> rows) {
        jdbc.update("""
                INSERT INTO ticket_distribution_scheme_versions (id, project_id, status, notified_at, effective_at)
                VALUES (?, ?, 'notified', TIMESTAMPTZ '2000-01-01 00:00:00+00', TIMESTAMPTZ '2000-01-02 00:00:00+00')""",
                id, projectId);
        rows.forEach((n, shares) -> {
            for (int r = 0; r < shares.length; r++) {
                jdbc.update("""
                        INSERT INTO ticket_distribution_participant_tiers (version_id, participant_count, rank_no, share_bps)
                        VALUES (?, ?, ?, ?)""", id, n, r + 1, shares[r]);
            }
        });
        return id;
    }

    /** 清掉本项目的池快照（连同第一段名单）与分账方案版本（连同档位行）；逐份结果随结算单删除。 */
    public static void deleteByProject(JdbcTemplate jdbc, String projectId) {
        jdbc.update("DELETE FROM pass_pool_snapshots WHERE project_id = ?", projectId);
        jdbc.update("DELETE FROM ticket_distribution_scheme_versions WHERE project_id = ?", projectId);
    }
}
