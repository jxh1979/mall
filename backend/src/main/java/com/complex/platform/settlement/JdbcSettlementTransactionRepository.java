package com.complex.platform.settlement;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 结算重构批1：settlement_transactions 写入仓储。
 *
 * <p>批1仅一个方法（核销结算双写 upsert），故用单一具体 {@code @Repository}、不引入接口
 * （one implementation → no interface；后续过期归项目/查询能力出现时再拆）。
 */
// ponytail: single-method repo, no interface until a 2nd impl or query surface appears.
@Repository
public class JdbcSettlementTransactionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSettlementTransactionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 核销结算双写：按 entitlement upsert 一条 verification_settled 流水。
     *
     * <p>命中部分唯一索引 {@code uq_st_entitlement_verified}（entitlement_id WHERE transaction_type='verification_settled'）
     * 则 DO UPDATE 累积——同一权益再次核销（used_times 变）只更新已核销次数/金额三段与 updated_at，不重复插入。
     * transaction_no/beneficiary/merchant/status 等首插即定，冲突时不覆盖（保持首次身份）。
     */
    public void upsertVerificationSettled(SettlementTransactionRecord record) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("transaction_no", record.transactionNo())
                .addValue("transaction_type", record.transactionType())
                .addValue("entitlement_id", record.entitlementId())
                .addValue("order_id", record.orderId())
                .addValue("ticket_type_id", record.ticketTypeId())
                .addValue("verification_id", record.verificationId())
                .addValue("entitlement_type", record.entitlementType())
                .addValue("verified_count", record.verifiedCount())
                .addValue("total_times", record.totalTimes())
                .addValue("num_people", record.numPeople())
                .addValue("allocated_revenue_cent", record.allocatedRevenueCent())
                .addValue("verified_amount_cent", record.verifiedAmountCent())
                .addValue("unverified_amount_cent", record.unverifiedAmountCent())
                .addValue("beneficiary_type", record.beneficiaryType())
                .addValue("merchant_id", record.merchantId())
                .addValue("project_id", record.projectId())
                .addValue("settlement_status", record.settlementStatus())
                .addValue("entitlement_valid_to", Timestamp.from(record.entitlementValidTo()))
                .addValue("exclusive_merchant_id", record.exclusiveMerchantId());

        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (
                    id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent,
                    beneficiary_type, merchant_id, project_id, settlement_status, entitlement_valid_to,
                    exclusive_merchant_id
                )
                VALUES (
                    :id, :transaction_no, :transaction_type, :entitlement_id, :order_id, :ticket_type_id,
                    :verification_id, :entitlement_type, :verified_count, :total_times, :num_people,
                    :allocated_revenue_cent, :verified_amount_cent, :unverified_amount_cent,
                    :beneficiary_type, :merchant_id, :project_id, :settlement_status, :entitlement_valid_to,
                    :exclusive_merchant_id
                )
                ON CONFLICT (entitlement_id) WHERE transaction_type = 'verification_settled'
                DO UPDATE SET
                    verified_count         = EXCLUDED.verified_count,
                    verified_amount_cent   = EXCLUDED.verified_amount_cent,
                    unverified_amount_cent = EXCLUDED.unverified_amount_cent,
                    verification_id        = EXCLUDED.verification_id,
                    updated_at             = now()
                """, params);
    }

    // ---- 结算重构批1 第3步:双写观察期对账(观察用途,不改现有 net 聚合口径) ----

    /**
     * 旧口径当日:issued_entitlements.allocated_revenue_cent 当日(created_at 落 [dayStart,dayEnd))聚合。
     * allocated 为 null(direct 出票无分摊快照)跳过——与 analytics net 口径一致。
     */
    public long sumOldTicketAllocatedCent(Instant dayStart, Instant dayEnd) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("day_start", Timestamp.from(dayStart))
                .addValue("day_end", Timestamp.from(dayEnd));
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(allocated_revenue_cent), 0)
                  FROM issued_entitlements
                 WHERE allocated_revenue_cent IS NOT NULL
                   AND created_at >= :day_start AND created_at < :day_end
                """, params, Long.class);
        return sum == null ? 0L : sum;
    }

    /**
     * 新口径当日:settlement_transactions(verification_settled)当日 verified+unverified 聚合。
     * verified+unverified=allocated 恒成立,故新旧总额应基本一致——差异=direct 出票(旧跳过)与时序。
     */
    public long sumNewSettlementCent(Instant dayStart, Instant dayEnd) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("day_start", Timestamp.from(dayStart))
                .addValue("day_end", Timestamp.from(dayEnd));
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(verified_amount_cent + unverified_amount_cent), 0)
                  FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                   AND created_at >= :day_start AND created_at < :day_end
                """, params, Long.class);
        return sum == null ? 0L : sum;
    }

    /** 对账结果 upsert settlement_reconciliation_status(按 reconciliation_date 主键幂等,可重入)。 */
    public void upsertReconciliationStatus(java.time.LocalDate reconciliationDate,
                                           long oldTicketAllocatedCent, long newSettlementCent, long varianceCent) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reconciliation_date", java.sql.Date.valueOf(reconciliationDate))
                .addValue("old_ticket_allocated_cent", oldTicketAllocatedCent)
                .addValue("new_settlement_cent", newSettlementCent)
                .addValue("variance_cent", varianceCent);
        jdbcTemplate.update("""
                INSERT INTO settlement_reconciliation_status (
                    reconciliation_date, old_ticket_allocated_cent, new_settlement_cent, variance_cent
                )
                VALUES (:reconciliation_date, :old_ticket_allocated_cent, :new_settlement_cent, :variance_cent)
                ON CONFLICT (reconciliation_date) DO UPDATE SET
                    old_ticket_allocated_cent = EXCLUDED.old_ticket_allocated_cent,
                    new_settlement_cent       = EXCLUDED.new_settlement_cent,
                    variance_cent             = EXCLUDED.variance_cent
                """, params);
    }

    // ---- 结算重构批1 第4步:未核销权益到期归项目 ----

    /**
     * 扫已过期(valid_to < :now)且存在未核销部分(total_times > used_times)的权益:
     * allocated 非空、状态非终止(active/expired 皆可,只看有无未核销与是否已过期)。
     * 返回未核销归项目所需字段——金额切分/记账在 Job 内做,仓库只取数。
     */
    public java.util.List<ExpiredUnverifiedRow> findExpiredUnverifiedEntitlements(Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        return jdbcTemplate.query("""
                SELECT id, order_id, ticket_type_id, entitlement_type, merchant_id,
                       total_times, used_times, allocated_revenue_cent, valid_to
                  FROM issued_entitlements
                 WHERE valid_to < :now
                   AND total_times > used_times
                   AND allocated_revenue_cent IS NOT NULL
                """, params, (rs, i) -> new ExpiredUnverifiedRow(
                        rs.getString("id"),
                        rs.getString("order_id"),
                        rs.getString("ticket_type_id"),
                        rs.getString("entitlement_type"),
                        rs.getString("merchant_id"),
                        rs.getInt("total_times"),
                        rs.getInt("used_times"),
                        rs.getLong("allocated_revenue_cent"),
                        rs.getTimestamp("valid_to").toInstant()));
    }

    /**
     * 未核销到期归项目:插一条 unverified_expired(beneficiary_type=project,settlement_status=confirmed)。
     * transaction_no 唯一(uq_st_transaction_no)→ ON CONFLICT DO NOTHING 幂等:同权益重复扫不重复归。
     */
    public void insertUnverifiedExpiredIgnoreConflict(SettlementTransactionRecord record) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("transaction_no", record.transactionNo())
                .addValue("transaction_type", record.transactionType())
                .addValue("entitlement_id", record.entitlementId())
                .addValue("order_id", record.orderId())
                .addValue("ticket_type_id", record.ticketTypeId())
                .addValue("verification_id", record.verificationId())
                .addValue("entitlement_type", record.entitlementType())
                .addValue("verified_count", record.verifiedCount())
                .addValue("total_times", record.totalTimes())
                .addValue("num_people", record.numPeople())
                .addValue("allocated_revenue_cent", record.allocatedRevenueCent())
                .addValue("verified_amount_cent", record.verifiedAmountCent())
                .addValue("unverified_amount_cent", record.unverifiedAmountCent())
                .addValue("beneficiary_type", record.beneficiaryType())
                .addValue("merchant_id", record.merchantId())
                .addValue("project_id", record.projectId())
                .addValue("settlement_status", record.settlementStatus())
                .addValue("entitlement_valid_to", Timestamp.from(record.entitlementValidTo()))
                .addValue("exclusive_merchant_id", record.exclusiveMerchantId());
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (
                    id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent,
                    beneficiary_type, merchant_id, project_id, settlement_status, entitlement_valid_to,
                    exclusive_merchant_id
                )
                VALUES (
                    :id, :transaction_no, :transaction_type, :entitlement_id, :order_id, :ticket_type_id,
                    :verification_id, :entitlement_type, :verified_count, :total_times, :num_people,
                    :allocated_revenue_cent, :verified_amount_cent, :unverified_amount_cent,
                    :beneficiary_type, :merchant_id, :project_id, :settlement_status, :entitlement_valid_to,
                    :exclusive_merchant_id
                )
                ON CONFLICT (transaction_no) DO NOTHING
                """, params);
    }

    /**
     * 平台侧结算审阅(2026-07-05 审计断层②):分页只读列出核销结算流水
     * (transaction_type=verification_settled)。可按 project/merchant 过滤(均可空)。
     * 供平台操作员查看项目/商户/核销张数/金额/状态——补此前只有 DB+月度 Job、无查看页的缺口。
     */
    public List<SettlementTransactionView> listVerificationSettled(
            String projectId, String merchantId, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbcTemplate.query("""
                SELECT id, transaction_no, project_id, merchant_id, exclusive_merchant_id,
                       ticket_type_id, entitlement_type, beneficiary_type,
                       verified_count, num_people, verified_amount_cent,
                       settlement_status, created_at
                  FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                   AND (:project_id::text  IS NULL OR project_id  = :project_id)
                   AND (:merchant_id::text IS NULL OR merchant_id = :merchant_id)
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, (rs, i) -> new SettlementTransactionView(
                        rs.getString("id"),
                        rs.getString("transaction_no"),
                        rs.getString("project_id"),
                        rs.getString("merchant_id"),
                        rs.getString("exclusive_merchant_id"),
                        rs.getString("ticket_type_id"),
                        rs.getString("entitlement_type"),
                        rs.getString("beneficiary_type"),
                        rs.getInt("verified_count"),
                        rs.getInt("num_people"),
                        rs.getLong("verified_amount_cent"),
                        rs.getString("settlement_status"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    /** 平台侧结算审阅:同筛选条件下的总条数(分页 total)。 */
    public long countVerificationSettled(String projectId, String merchantId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId);
        Long n = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                   AND (:project_id::text  IS NULL OR project_id  = :project_id)
                   AND (:merchant_id::text IS NULL OR merchant_id = :merchant_id)
                """, params, Long.class);
        return n == null ? 0L : n;
    }

    /** 平台侧结算审阅只读投影:列表页展示字段。 */
    public record SettlementTransactionView(
            String id,
            String transactionNo,
            String projectId,
            String merchantId,
            String exclusiveMerchantId,
            String ticketTypeId,
            String entitlementType,
            String beneficiaryType,
            int verifiedCount,
            int numPeople,
            long verifiedAmountCent,
            String settlementStatus,
            Instant createdAt
    ) {
    }

    /** 第4步取数投影:已过期未核销权益归项目所需字段。 */
    public record ExpiredUnverifiedRow(
            String entitlementId,
            String orderId,
            String ticketTypeId,
            String entitlementType,
            String merchantId,
            int totalTimes,
            int usedTimes,
            long allocatedRevenueCent,
            Instant validTo
    ) {
    }

}
