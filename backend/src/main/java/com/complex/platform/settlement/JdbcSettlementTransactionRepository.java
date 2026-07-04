package com.complex.platform.settlement;

import java.sql.Timestamp;
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
                .addValue("entitlement_valid_to", Timestamp.from(record.entitlementValidTo()));

        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (
                    id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent,
                    beneficiary_type, merchant_id, project_id, settlement_status, entitlement_valid_to
                )
                VALUES (
                    :id, :transaction_no, :transaction_type, :entitlement_id, :order_id, :ticket_type_id,
                    :verification_id, :entitlement_type, :verified_count, :total_times, :num_people,
                    :allocated_revenue_cent, :verified_amount_cent, :unverified_amount_cent,
                    :beneficiary_type, :merchant_id, :project_id, :settlement_status, :entitlement_valid_to
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
}
