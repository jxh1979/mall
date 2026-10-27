package com.complex.platform.settlement;

// 文件版本：V0.01

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 第十八批第 2 条：酒吧通票逐份分账的读写——售票池快照、份内入场核销、方案档位行、暂挂与续算、逐份结果。
 * 引擎见 {@link TicketDistributionSettlementService}；快照写入见 {@link PassPoolSnapshotService}。
 */
@Repository
public class JdbcPassPoolRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPassPoolRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 份内一次仍有效的入场类核销；poolBarEntry＝进池的酒吧入场（非专属、酒吧业态、entry/admission）。 */
    record EntryUse(String ticketId, int slotIndex, String merchantId, boolean poolBarEntry, Instant at) {
    }

    /** 售票池快照（含售票时固化的第一段名单）。 */
    record Snapshot(String ticketId, String projectId, int slots, int visits, long barGrossCent, long platformBorneCent,
                    long routeFeeCent, long salesChannelFeeCent, String salesChannelFeeConfigId, long giftCostCent,
                    String giftCostConfigId, long serviceFeeCent, String schemeVersionId, Instant schemeEffectiveAt,
                    List<String> coveredBarIds, List<FirstSegment> firstSegment) {
    }

    record FirstSegment(String merchantId, String ruleType, long amountCent) {
    }

    /** 暂挂份的一家第二段参与者：续算沿用原月的当期入场人数与核销状态、原份第二段余额。 */
    record SuspendedRow(String ticketId, int slotIndex, int visitNo, String merchantId, int entryPeople, boolean verified,
                        long segmentPoolCent, String projectId, Instant schemeEffectiveAt) {
    }

    record UnitResult(String ticketId, int slotIndex, int visitNo, String merchantId, int segment, Integer entryPeople,
                      Integer rankNo, java.math.BigDecimal pct, Long segmentPoolCent, long amountCent, boolean verified,
                      String status, String schemeVersionId) {
    }

    /**
     * 本窗内有过核销的池快照父票，其全部仍有效的入场类核销（与到期作业「入场次数」同口径：ticket/entry/admission/amusement*），
     * 按父票、人份、商户、时刻排序。冲正只认业务时刻早于 cutoff 的（FS-021-R2 原月重算口径；当月结算 cutoff＝窗尾）。
     */
    List<EntryUse> findEntryUses(String projectId, Instant start, Instant end, Instant cutoff) {
        return jdbc.query("""
                SELECT st.parent_ticket_id, COALESCE(e.slot_index, 1) AS slot_index, st.merchant_id,
                       (st.exclusive_merchant_id IS NULL AND st.entitlement_type IN ('entry', 'admission')
                        AND COALESCE(m.merchant_type, 'bar') = 'bar') AS pool_bar_entry,
                       COALESCE(st.business_at, st.created_at) AS at
                  FROM settlement_transactions st
                  JOIN pass_pool_snapshots ps ON ps.ticket_instance_id = st.parent_ticket_id
                       AND (:project_id::text IS NULL OR ps.project_id = :project_id)
                  LEFT JOIN issued_entitlements e ON e.id = st.entitlement_id
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type = 'verification_settled'
                   AND st.verification_id IS NOT NULL
                   AND (st.entitlement_type IN ('ticket', 'entry', 'admission') OR st.entitlement_type LIKE 'amusement%')
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                   AND st.parent_ticket_id IN (SELECT x.parent_ticket_id FROM settlement_transactions x
                                                WHERE x.transaction_type = 'verification_settled'
                                                  AND x.parent_ticket_id IS NOT NULL
                                                  AND (:project_id::text IS NULL OR x.project_id = :project_id)
                                                  AND COALESCE(x.business_at, x.created_at) >= :start
                                                  AND COALESCE(x.business_at, x.created_at) < :end)
                   AND NOT EXISTS (SELECT 1 FROM settlement_transactions r
                                    WHERE r.transaction_type = 'verification_reversed'
                                      AND r.verification_id = st.verification_id
                                      AND COALESCE(r.business_at, r.created_at) < :cutoff)
                 ORDER BY 1, 2, 3, 5, st.id
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("start", Timestamp.from(start)).addValue("end", Timestamp.from(end))
                .addValue("cutoff", Timestamp.from(cutoff)),
                (rs, i) -> new EntryUse(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getBoolean(4),
                        rs.getTimestamp(5).toInstant()));
    }

    Map<String, Snapshot> findSnapshots(Collection<String> ticketIds) {
        Map<String, Snapshot> out = new LinkedHashMap<>();
        if (ticketIds.isEmpty()) {
            return out;
        }
        MapSqlParameterSource ids = new MapSqlParameterSource("ids", ticketIds);
        Map<String, List<FirstSegment>> firstSegment = new HashMap<>();
        jdbc.query("""
                SELECT ticket_instance_id, merchant_id, rule_type, amount_cent FROM pass_pool_snapshot_first_segment
                 WHERE ticket_instance_id IN (:ids) ORDER BY ticket_instance_id, merchant_id
                """, ids, rs -> {
            firstSegment.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                    .add(new FirstSegment(rs.getString(2), rs.getString(3), rs.getLong(4)));
        });
        jdbc.query("""
                SELECT s.*, v.effective_at AS scheme_effective_at FROM pass_pool_snapshots s
                  JOIN ticket_distribution_scheme_versions v ON v.id = s.scheme_version_id
                 WHERE s.ticket_instance_id IN (:ids)
                """, ids, rs -> {
            String id = rs.getString("ticket_instance_id");
            out.put(id, new Snapshot(id, rs.getString("project_id"), rs.getInt("slots"), rs.getInt("visits"),
                    rs.getLong("bar_gross_cent"), rs.getLong("platform_borne_cent"), rs.getLong("route_fee_cent"),
                    rs.getLong("sales_channel_fee_cent"), rs.getString("sales_channel_fee_config_id"),
                    rs.getLong("gift_cost_cent"), rs.getString("gift_cost_config_id"),
                    rs.getLong("service_fee_cent"), rs.getString("scheme_version_id"),
                    rs.getTimestamp("scheme_effective_at").toInstant(),
                    Arrays.asList((String[]) rs.getArray("covered_bar_ids").getArray()),
                    firstSegment.getOrDefault(id, List.of())));
        });
        return out;
    }

    /** 方案版本的参与家数档位表：家数 → 各名次比例（万分比，下标 0＝第 1 名）。 */
    Map<Integer, int[]> findTiers(String versionId) {
        Map<Integer, int[]> out = new HashMap<>();
        jdbc.query("""
                SELECT participant_count, rank_no, share_bps FROM ticket_distribution_participant_tiers WHERE version_id = :v
                """, new MapSqlParameterSource("v", versionId), rs -> {
            int n = rs.getInt(1);
            out.computeIfAbsent(n, k -> new int[n])[rs.getInt(2) - 1] = rs.getInt(3);
        });
        return out;
    }

    /**
     * 补配行：售票所用版本之后生效、at 之前已生效、含该家数行的最早一版（ENUM L52「补配生效后，因缺该行而暂挂的票按该行续算」）。
     * 只认后续补配，不就近取用其他家数的行，也不回头用更早的版本。
     */
    String findResolvingVersion(String projectId, int participantCount, Instant afterEffectiveAt, Instant at) {
        return jdbc.queryForList("""
                SELECT v.id FROM ticket_distribution_scheme_versions v
                 WHERE v.status = 'notified' AND v.effective_at > :after AND v.effective_at <= :at
                   AND (v.project_id IS NULL OR v.project_id = :project_id)
                   AND EXISTS (SELECT 1 FROM ticket_distribution_participant_tiers t
                                WHERE t.version_id = v.id AND t.participant_count = :n)
                 ORDER BY v.effective_at, v.notified_at
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("after", Timestamp.from(afterEffectiveAt))
                .addValue("at", Timestamp.from(at)).addValue("project_id", projectId).addValue("n", participantCount),
                String.class).stream().findFirst().orElse(null);
    }

    /** 本范围更早月份结算单里仍暂挂、尚未在更早结算单续算过的份（票已整张退款作废的不再续算；当月重跑时本月已续算的照常重算）。 */
    List<SuspendedRow> findSuspendedRows(String projectId, LocalDate periodStart) {
        return jdbc.query("""
                SELECT r.ticket_instance_id, r.slot_index, r.visit_no, r.merchant_id, COALESCE(r.entry_people, 0) AS entry_people,
                       r.verified, r.segment_pool_cent, ps.project_id, v.effective_at
                  FROM pass_pool_unit_results r
                  JOIN settlement_bills b ON b.id = r.bill_id
                  JOIN pass_pool_snapshots ps ON ps.ticket_instance_id = r.ticket_instance_id AND ps.status = 'active'
                  JOIN ticket_distribution_scheme_versions v ON v.id = ps.scheme_version_id
                 WHERE r.status = 'suspended'
                   AND b.period_start < :period_start
                   AND COALESCE(b.project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                   AND NOT EXISTS (SELECT 1 FROM pass_pool_unit_results x
                                     JOIN settlement_bills xb ON xb.id = x.bill_id
                                    WHERE x.status = 'resumed' AND x.ticket_instance_id = r.ticket_instance_id
                                      AND x.slot_index = r.slot_index AND x.visit_no = r.visit_no
                                      AND xb.period_start < :period_start)
                 ORDER BY 1, 2, 3, 4
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("period_start", java.sql.Date.valueOf(periodStart)),
                (rs, i) -> new SuspendedRow(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getInt(5),
                        rs.getBoolean(6), rs.getLong(7), rs.getString(8), rs.getTimestamp(9).toInstant()));
    }

    /** 逐份结果随结算单幂等覆盖（与明细同范式：先清后写）。 */
    void replaceUnitResults(String billId, List<UnitResult> results) {
        jdbc.update("DELETE FROM pass_pool_unit_results WHERE bill_id = :b", new MapSqlParameterSource("b", billId));
        for (UnitResult r : results) {
            jdbc.update("""
                    INSERT INTO pass_pool_unit_results (bill_id, ticket_instance_id, slot_index, visit_no, merchant_id, segment,
                        entry_people, rank_no, pct, segment_pool_cent, amount_cent, verified, status, scheme_version_id)
                    VALUES (:b, :t, :s, :k, :m, :seg, :people, :rank, :pct, :p2, :amount, :verified, :status, :v)
                    """, new MapSqlParameterSource().addValue("b", billId).addValue("t", r.ticketId())
                    .addValue("s", r.slotIndex()).addValue("k", r.visitNo()).addValue("m", r.merchantId())
                    .addValue("seg", r.segment()).addValue("people", r.entryPeople(), java.sql.Types.INTEGER)
                    .addValue("rank", r.rankNo(), java.sql.Types.INTEGER).addValue("pct", r.pct(), java.sql.Types.NUMERIC)
                    .addValue("p2", r.segmentPoolCent(), java.sql.Types.BIGINT).addValue("amount", r.amountCent())
                    .addValue("verified", r.verified()).addValue("status", r.status()).addValue("v", r.schemeVersionId()));
        }
    }
}
