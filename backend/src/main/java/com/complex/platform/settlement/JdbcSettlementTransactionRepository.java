package com.complex.platform.settlement;

// 文件版本：V0.09

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
     * AUD-012/013/017：append 一条<b>不可变</b>结算事实（核销增量 / 冲正负向 / 未核销到期归项目共用）。
     * 幂等锚点=全局唯一 {@code transaction_no}（uq_st_transaction_no）→ ON CONFLICT DO NOTHING：
     * 同一业务事件重放不重复入账，且<b>永不 UPDATE</b>（账本 append-only，历史行不可变）。
     */
    public void insertImmutableFact(SettlementTransactionRecord record) {
        insertFact(record, null);
    }

    /**
     * FS-023：到期未核销事实另带立项分类与实际渠道费（ENUM L48「各项均只扣一次」、L49「两类各自单独立项留痕」、
     * L51「每笔分摊的结果与所用合同版本必须留痕」；PGM L302 尾差单列并写明承担主体）。幂等口径同 {@link #insertImmutableFact}。
     */
    public void insertUnverifiedFact(SettlementTransactionRecord record, UnverifiedExtras extras) {
        insertFact(record, extras);
    }

    private void insertFact(SettlementTransactionRecord record, UnverifiedExtras extras) {
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
                .addValue("exclusive_merchant_id", record.exclusiveMerchantId())
                .addValue("business_at", record.businessAt() == null ? null : Timestamp.from(record.businessAt()))
                .addValue("discount_amount_cent", record.discountAmountCent())
                .addValue("comp_service_fee_cent", record.compServiceFeeCent())
                .addValue("comp_service_fee_config_id", record.compServiceFeeConfigId())
                .addValue("beneficiary_entity_id", record.beneficiaryEntityId())
                .addValue("parent_ticket_id", record.parentTicketId())
                .addValue("gift_cost_cent", record.giftCostCent())
                .addValue("gift_cost_config_id", record.giftCostConfigId())
                .addValue("unverified_category", extras == null ? null : extras.category())
                .addValue("channel_fee_cent", extras == null ? 0L : extras.channelFeeCent())
                .addValue("channel_fee_config_id", extras == null ? null : extras.channelFeeConfigId())
                .addValue("channel_fee_residual_cent", extras == null ? 0L : extras.channelFeeResidualCent())
                .addValue("channel_fee_residual_entity_id", extras == null ? null : extras.channelFeeResidualEntityId())
                .addValue("route_fee_cent", record.routeFeeCent())
                .addValue("domain_share_cent", record.domainShareCent(), java.sql.Types.BIGINT);

        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (
                    id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent,
                    beneficiary_type, merchant_id, project_id, settlement_status, entitlement_valid_to,
                    exclusive_merchant_id, business_at, discount_amount_cent,
                    comp_service_fee_cent, comp_service_fee_config_id, beneficiary_entity_id,
                    parent_ticket_id, gift_cost_cent, gift_cost_config_id,
                    unverified_category, channel_fee_cent, channel_fee_config_id,
                    channel_fee_residual_cent, channel_fee_residual_entity_id, route_fee_cent, domain_share_cent
                )
                VALUES (
                    :id, :transaction_no, :transaction_type, :entitlement_id, :order_id, :ticket_type_id,
                    :verification_id, :entitlement_type, :verified_count, :total_times, :num_people,
                    :allocated_revenue_cent, :verified_amount_cent, :unverified_amount_cent,
                    :beneficiary_type, :merchant_id, :project_id, :settlement_status, :entitlement_valid_to,
                    :exclusive_merchant_id, :business_at, :discount_amount_cent,
                    :comp_service_fee_cent, :comp_service_fee_config_id, :beneficiary_entity_id,
                    :parent_ticket_id, :gift_cost_cent, :gift_cost_config_id,
                    :unverified_category, :channel_fee_cent, :channel_fee_config_id,
                    :channel_fee_residual_cent, :channel_fee_residual_entity_id, :route_fee_cent, :domain_share_cent
                )
                ON CONFLICT (transaction_no) DO NOTHING
                """, params);
    }

    /**
     * AUD-013：读原核销结算「可冲正的单笔核销金额」用于冲正取反。两条路径：
     * <ol>
     *   <li><b>新模型</b>：verification-keyed 增量行 ST-VS-&lt;verificationId&gt; → 直接取其 verified 金额；</li>
     *   <li><b>遗留兜底(Critical 复核)</b>：找不到新行时，落到该权益的遗留累计行(verification_id IS NULL)——
     *       历史核销此前只写「每权益一行」累计值、无 per-verification 行，若不兜底则历史核销冲正 no-op、遗留正向永不冲销
     *       (AUD-013 对历史失效、多付商户)。兜底按本次核销在该权益成功核销中的时序 k 反推<b>单笔增量</b>
     *       f(k)−f(k−1)(用遗留行 allocated/total，对专属定价 allocated=price×total 亦精确=price)，仅红冲这一笔、不动遗留累计行。</li>
     * </ol>
     * 两路都找不到（原结算事实确实缺失）返回 null → 冲正 no-op（无正向可冲，净额已正确）。
     */
    public SettledFactRow findVerificationSettledByVerificationId(String verificationId) {
        SettledFactRow modern = jdbcTemplate.query("""
                SELECT entitlement_id, order_id, ticket_type_id, entitlement_type, num_people, total_times,
                       verified_amount_cent, discount_amount_cent, comp_service_fee_cent, comp_service_fee_config_id,
                       beneficiary_type, beneficiary_entity_id, merchant_id, project_id, exclusive_merchant_id,
                       entitlement_valid_to, COALESCE(business_at, created_at) AS business_at, parent_ticket_id,
                       gift_cost_cent, gift_cost_config_id, route_fee_cent, domain_share_cent
                  FROM settlement_transactions
                 WHERE transaction_type = 'verification_settled'
                   AND transaction_no = :tno
                """, new MapSqlParameterSource().addValue("tno", "ST-VS-" + verificationId), this::mapSettledFactRow);
        if (modern != null) {
            return modern;
        }
        // 遗留兜底：反推本次核销的单笔增量金额，红冲对遗留累计行。
        return jdbcTemplate.query("""
                SELECT leg.entitlement_id, leg.order_id, leg.ticket_type_id, leg.entitlement_type,
                       v.verified_people AS num_people, leg.total_times,
                       (round(leg.allocated_revenue_cent::numeric * k.k       / NULLIF(leg.total_times, 0))
                      - round(leg.allocated_revenue_cent::numeric * (k.k - 1) / NULLIF(leg.total_times, 0)))::bigint
                            AS verified_amount_cent,
                       0::bigint AS discount_amount_cent,  -- 遗留累计行无满额价快照 → 折扣 0（红冲不动折扣，与新行配对天然净掉）
                       0::bigint AS comp_service_fee_cent,  -- P6 遗留累计行无 fee 快照 → 0（无正向可冲，且 legacy 已清理）
                       NULL::varchar AS comp_service_fee_config_id,
                       0::bigint AS gift_cost_cent,         -- S6 遗留累计行无 G 快照 → 0（无正向可冲）
                       NULL::varchar AS gift_cost_config_id,
                       0::bigint AS route_fee_cent,         -- 第十八批第 3 条：遗留累计行无通路费分摊 → 0
                       NULL::bigint AS domain_share_cent,   -- 176-R1：遗留累计行无域份额
                       leg.beneficiary_type, leg.beneficiary_entity_id, leg.merchant_id, leg.project_id,
                       leg.exclusive_merchant_id,
                       leg.entitlement_valid_to, COALESCE(leg.business_at, leg.created_at) AS business_at,
                       leg.parent_ticket_id
                  FROM ticket_verifications v
                  JOIN settlement_transactions leg
                    ON leg.entitlement_id = v.entitlement_id
                   AND leg.transaction_type = 'verification_settled'
                   AND leg.verification_id IS NULL
                  CROSS JOIN LATERAL (
                      SELECT COUNT(*) AS k FROM ticket_verifications f
                       WHERE f.entitlement_id = v.entitlement_id
                         AND f.reverses_verification_id IS NULL
                         -- k=本次核销在「计入 used_times 的成功核销」中的时序序号：只数 success/reversed
                         -- (原核销在冲正主链已先翻转成 reversed 再发事件,故本次被冲的核销自身也须计入),
                         -- 排除 failed/rejected/manual_overridden(不进 used_times、不构成遗留 f(used)),否则 k 被抬高、增量错配、净额差分。
                         AND f.status IN ('success', 'reversed')
                         AND (f.created_at, f.id) <= (v.created_at, v.id)
                  ) k
                 WHERE v.id = :verId
                """, new MapSqlParameterSource().addValue("verId", verificationId), this::mapSettledFactRow);
    }

    /**
     * TK-05-R4：已成功退款所退权益上的成功核销（按核销先后）——即该退款对应的、已形成分账的核销结算。
     * 退款未成功（requested/approved/rejected）返回空：BSM:804「门票实际退款成功后」才冲回。
     */
    public List<String> findSettledVerificationsOfSucceededRefund(String refundId) {
        return jdbcTemplate.queryForList("""
                SELECT v.id
                  FROM ticket_refunds r
                  JOIN ticket_refund_lines l ON l.refund_id = r.id
                  JOIN ticket_verifications v ON v.entitlement_id = l.entitlement_id
                 WHERE r.id = :refund_id AND r.status = 'success'
                   AND v.status = 'success' AND v.reverses_verification_id IS NULL
                 ORDER BY v.created_at, v.id
                """, new MapSqlParameterSource().addValue("refund_id", refundId), String.class);
    }

    private SettledFactRow mapSettledFactRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return rs.next() ? new SettledFactRow(
                rs.getString("entitlement_id"), rs.getString("order_id"), rs.getString("ticket_type_id"),
                rs.getString("entitlement_type"), rs.getInt("num_people"), rs.getInt("total_times"),
                rs.getLong("verified_amount_cent"), rs.getString("beneficiary_type"),
                rs.getString("merchant_id"), rs.getString("project_id"), rs.getString("exclusive_merchant_id"),
                rs.getTimestamp("entitlement_valid_to").toInstant(),
                rs.getTimestamp("business_at").toInstant(),
                rs.getLong("discount_amount_cent"),
                rs.getLong("comp_service_fee_cent"), rs.getString("comp_service_fee_config_id"),
                rs.getString("beneficiary_entity_id"), rs.getString("parent_ticket_id"),
                rs.getLong("gift_cost_cent"), rs.getString("gift_cost_config_id"), rs.getLong("route_fee_cent"),
                rs.getObject("domain_share_cent", Long.class)) : null;
    }

    /** AUD-013 冲正取反读投影：原核销结算行的金额与归属维度。D6：加 discountAmountCent，冲正时取原行反。 */
    public record SettledFactRow(
            String entitlementId, String orderId, String ticketTypeId, String entitlementType,
            int numPeople, int totalTimes, long verifiedAmountCent, String beneficiaryType,
            String merchantId, String projectId, String exclusiveMerchantId,
            Instant entitlementValidTo, Instant businessAt, long discountAmountCent,
            long compServiceFeeCent, String compServiceFeeConfigId,
            // V355(B-7):原行的收款主体,冲正行必须原样带上,否则 type=entity 的冲正会撞 chk_st_beneficiary。
            String beneficiaryEntityId,
            // S3:原行的父票锚点,冲正行必须原样带上 —— 否则该票的服务费净额抵不回 0,锚点永远复不了位,
            //   冲掉首笔核销之后这张票就再也收不到那一道综合服务费。
            String parentTicketId,
            // S6:原行的赠送权益预估成本 G 与规则 id,冲正行取反/沿用 —— 该票 G 净额抵回 0 后锚点复位可再计提。
            long giftCostCent, String giftCostConfigId,
            // 第十八批第 3 条：原行分摊的通路费，冲正行取反。
            long routeFeeCent,
            // 176-R1：原行的域份额（有价专属直付行），冲正行取反；null 仍 null。
            Long domainShareCent) {
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
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND COALESCE(business_at, created_at) >= :day_start AND COALESCE(business_at, created_at) < :day_end
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
     * AUD-019 到期归项目"可没收终态"白名单：扫已过期(valid_to &lt; :now)且存在未核销部分(total_times &gt; used_times)、
     * allocated 非空的权益，<b>并排除退款/转赠链</b>：
     * <ul>
     *   <li>{@code status IN ('active','forfeited')}：已成功退款(status='refunded')的权益天然排除——
     *       退款给客户的金额不能再以未使用到期名义归项目(AUD-019 根因)；到期作废(forfeited)是未退款的终结状态，须照常归属(FS-023-R2)；</li>
     *   <li>父票实例非 active(refund_pending/refunded/transferring/forfeited)一并排除——覆盖"退款申请中/已批准"
     *       尚未冻结权益的时间窗（此时权益仍 active 但实例已冻结）。</li>
     * </ul>
     * 退款失败驳回→实例恢复 active、权益仍 active → 重新可没收（下轮扫描并入）。仓库只取数，金额切分在 Job 内做。
     *
     * <p><b>business_format 不再 COALESCE 兜底 'bar'（2026-08-03）</b>：这一列是「未核销余额归谁」的判定源
     * （HTML §「到期未核销款」：酒吧相关归娱乐平台公司、纯游乐归游乐公司、演出归传媒公司）。
     * 兜底成酒吧＝有金额却定不出真正的收款方时默认记给娱乐平台公司，传媒/游乐公司的钱就这么悄悄没了。
     * 现改为如实返回 NULL，由 {@code SettlementExpiryJob} fail-loud 阻断落账。
     */
    // FS-023-R2/R4：作废(forfeited)终结状态同样要归属（TicketOrderExpiryJob 把到期且从未使用的权益置 forfeited，
    //   只取 active 会让恰恰从未使用的票永不落归属事实）；另取多人票首场次已开启、本人份从未检票的权益（未到期），
    //   由作业按首场次实际闭店时刻判定是否已可处理（ENUM L48「于该场次结束时按本条处理，不等票面有效期届满」）。
    //   人份汇总（是否检票、入场次数）按同父票同 slot 全部权益取；入场类权益口径同 TicketRewardService（ticket/entry/admission/amusement*）。
    //   第十八批第 2 条：另取该人份仍有效的入场类核销时刻（作业按营业日计已开启次数，与逐份分账同口径）；
    //   18AH 复核修复：另取同父票各人份仍有效的入场类核销时刻（作业按整张票判本场，与核销放行 ticketCheckedInThisSession 同口径）；
    //   第十八批第 3 条：销售渠道费取订单付款时固化的配置（ticket_order_channel_fees）。
    //   第十八批 AI 段 212-R1（ENUM:48「各项均只扣一次」）：固定额锚点另认单一酒吧门票核销行——月结已按其首笔核销扣过一道。
    public java.util.List<ExpiredUnverifiedRow> findExpiredUnverifiedEntitlements(Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        return jdbcTemplate.query("""
                SELECT c.*,
                       vs.session_date,
                       EXTRACT(EPOCH FROM h.open_time)::int  AS open_s,
                       EXTRACT(EPOCH FROM h.close_time)::int AS close_s,
                       sl.checked_in, sl.visits, sl.opened, ea.entry_ats, ea.ticket_entry_ats,
                       f.sales_fee_config_id, COALESCE(f.sales_fee_bps, 0) AS sales_fee_bps, f.sales_fixed_amount_cent,
                       (EXISTS (SELECT 1 FROM settlement_transactions a
                                 WHERE a.parent_ticket_id = c.parent_ticket_id
                                   AND a.transaction_type = 'verification_settled'
                                   AND a.verification_id IS NOT NULL
                                   AND (a.exclusive_merchant_id IS NULL OR
                """ + JdbcSettlementBillRepository.SINGLE_BAR_ROW.formatted("a") + """
                ))
                        -- 同票已有到期事实扣过渠道费（如多人票未到人份先于首场次结束时处理）：固定额不再扣第二次（ENUM L48「各项均只扣一次」）
                        OR EXISTS (SELECT 1 FROM settlement_transactions f
                                     JOIN issued_entitlements fe ON fe.id = f.entitlement_id
                                    WHERE fe.parent_ticket_id = c.parent_ticket_id
                                      AND f.transaction_type = 'unverified_expired'
                                      AND (f.channel_fee_cent <> 0 OR f.channel_fee_residual_cent <> 0))) AS anchored
                  FROM (
                      SELECT e.id, e.order_id, e.ticket_type_id, e.entitlement_type, e.merchant_id,
                             e.total_times, e.used_times, e.allocated_revenue_cent, e.valid_to,
                             e.parent_ticket_id, e.slot_index,
                             m.merchant_type AS business_format,
                             m.supervising_company_id,
                             o.project_id, o.sales_channel,
                             COALESCE(ti.num_people, 1) AS parent_num_people, ti.first_session_key,
                             (COALESCE(m.merchant_type, 'bar') = 'bar' AND e.exclusive_merchant_id IS NULL
                              AND EXISTS (SELECT 1 FROM pass_pool_snapshots pps WHERE pps.ticket_instance_id = e.parent_ticket_id))
                                 AS pooled_at_sale
                        FROM issued_entitlements e
                        LEFT JOIN merchants m ON m.id = e.merchant_id
                        LEFT JOIN ticket_orders o ON o.id = e.order_id
                        LEFT JOIN ticket_instances ti ON ti.id = e.parent_ticket_id
                       WHERE e.total_times > e.used_times
                         AND e.allocated_revenue_cent IS NOT NULL
                         AND e.status IN ('active', 'forfeited')
                         -- 父实例非 active(退款申请中/已退/转赠中)一并排除；无父实例的遗留权益照旧可没收。
                         AND (ti.id IS NULL OR ti.status = 'active')
                         -- 已落到期事实的权益（一权益一次处理即写全两类）不再重扫：免得历史行每日重算归属、重分固定渠道费。
                         AND NOT EXISTS (SELECT 1 FROM settlement_transactions x
                                          WHERE x.entitlement_id = e.id AND x.transaction_type = 'unverified_expired')
                         AND (e.valid_to < :now
                              OR (ti.num_people > 1 AND ti.first_session_key IS NOT NULL
                                  AND NOT EXISTS (SELECT 1 FROM issued_entitlements u
                                                   WHERE u.parent_ticket_id = e.parent_ticket_id
                                                     AND u.slot_index = e.slot_index AND u.used_times > 0)))
                  ) c
                  LEFT JOIN venue_sessions vs ON vs.id = c.first_session_key
                  LEFT JOIN venue_business_hours h ON h.venue_id = vs.venue_id AND h.is_draft = FALSE
                        AND h.day_of_week = EXTRACT(DOW FROM vs.session_date)::int
                  CROSS JOIN LATERAL (
                      SELECT BOOL_OR(s.used_times > 0) AS checked_in,
                             MAX(s.total_times) FILTER (WHERE s.entitlement_type IN ('ticket', 'entry', 'admission')
                                                           OR s.entitlement_type LIKE 'amusement%') AS visits,
                             MAX(s.used_times)  FILTER (WHERE s.entitlement_type IN ('ticket', 'entry', 'admission')
                                                           OR s.entitlement_type LIKE 'amusement%') AS opened
                        FROM issued_entitlements s
                       WHERE s.id = c.id
                          OR (s.parent_ticket_id = c.parent_ticket_id AND s.slot_index = c.slot_index)
                  ) sl
                  CROSS JOIN LATERAL (
                      SELECT COALESCE(ARRAY_AGG(COALESCE(st.business_at, st.created_at))
                                          FILTER (WHERE s.id = c.id OR s.slot_index = c.slot_index), ARRAY[]::timestamptz[]) AS entry_ats,
                             COALESCE(ARRAY_AGG(COALESCE(st.business_at, st.created_at)), ARRAY[]::timestamptz[]) AS ticket_entry_ats
                        FROM settlement_transactions st
                        JOIN issued_entitlements s ON s.id = st.entitlement_id
                       WHERE (s.id = c.id OR s.parent_ticket_id = c.parent_ticket_id)
                         AND st.transaction_type = 'verification_settled' AND st.verification_id IS NOT NULL
                         AND (st.entitlement_type IN ('ticket', 'entry', 'admission') OR st.entitlement_type LIKE 'amusement%')
                         AND NOT EXISTS (SELECT 1 FROM settlement_transactions r
                                          WHERE r.transaction_type = 'verification_reversed' AND r.verification_id = st.verification_id)
                  ) ea
                  LEFT JOIN ticket_order_channel_fees f ON f.order_id = c.order_id
                 ORDER BY c.parent_ticket_id NULLS LAST, c.id
                """, params, (rs, i) -> new ExpiredUnverifiedRow(
                        rs.getString("id"),
                        rs.getString("order_id"),
                        rs.getString("ticket_type_id"),
                        rs.getString("entitlement_type"),
                        rs.getString("merchant_id"),
                        rs.getInt("total_times"),
                        rs.getInt("used_times"),
                        rs.getLong("allocated_revenue_cent"),
                        rs.getTimestamp("valid_to").toInstant(),
                        rs.getString("business_format"),
                        rs.getString("supervising_company_id"),
                        rs.getString("project_id"),
                        rs.getString("sales_channel"),
                        rs.getString("parent_ticket_id"),
                        rs.getInt("parent_num_people"),
                        rs.getString("first_session_key"),
                        rs.getObject("session_date", java.time.LocalDate.class),
                        (Integer) rs.getObject("open_s"),
                        (Integer) rs.getObject("close_s"),
                        rs.getBoolean("checked_in"),
                        rs.getInt("visits"),
                        rs.getInt("opened"),
                        rs.getBoolean("anchored"),
                        rs.getBoolean("pooled_at_sale"),
                        rs.getInt("slot_index"),
                        java.util.Arrays.stream((Timestamp[]) rs.getArray("entry_ats").getArray()).map(Timestamp::toInstant).toList(),
                        java.util.Arrays.stream((Timestamp[]) rs.getArray("ticket_entry_ats").getArray()).map(Timestamp::toInstant).toList(),
                        rs.getString("sales_fee_config_id") == null ? JdbcSettlementBillRepository.NO_SALES_FEE
                                : new JdbcSettlementBillRepository.ChannelFeeConfig(rs.getString("sales_fee_config_id"),
                                        rs.getInt("sales_fee_bps"), (Long) rs.getObject("sales_fixed_amount_cent"))));
    }

    /** FS-023-R1：该项目唯一有效的城市项目公司（ENUM L49「餐饮域与零售域 → 城市项目公司」）；0 或多家返回全部供调用方阻断。 */
    public java.util.List<String> findProjectCompanies(String projectId) {
        return jdbcTemplate.queryForList("""
                SELECT e.id FROM business_entities e
                  JOIN business_entity_projects p ON p.business_entity_id = e.id
                 WHERE p.project_id = :project_id AND e.entity_role = 'project_company' AND e.status = 'active'
                 ORDER BY e.id
                """, new MapSqlParameterSource().addValue("project_id", projectId), String.class);
    }

    /** 订单所挂酒吧（单一酒吧门票）；通票或无订单为 null。核销结算事实据此把单店票按专属直付记到该酒吧（ENUM L32）。 */
    public String findOrderBarId(String orderId) {
        return jdbcTemplate.query("SELECT bar_id FROM ticket_orders WHERE id = :id",
                new MapSqlParameterSource("id", orderId), rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * AUD-017 可审计重建：按核销权威事实(ticket_verifications success)补建缺失的核销结算增量行。
     * 仅对<b>无冲正历史</b>的权益（used_times 单调等于成功核销数，k=时序序号可信）逐笔重建，
     * 每次核销一行 ST-VS-&lt;verificationId&gt;（与在线消费者同 key 同金额公式，幂等 ON CONFLICT DO NOTHING）。
     * 有冲正的权益跳过（净额已由在线正/负事实承载；交由对账观察，避免重建期序号错配）。
     *
     * <p><b>Critical(复核 AUD-017)：排除遗留 entitlement-keyed 累计行覆盖的权益。</b>
     * 旧消费者按「每权益一行」写累计行(transaction_no=ST-VS-&lt;entitlementId&gt;、verification_id 为 NULL、值=f(used))。
     * 若不排除，重建会给这些历史核销再建 per-verification 增量行 → 与遗留累计行叠加 → 账单聚合对历史核销≈翻倍多付。
     * 故仅重建<b>无任何遗留累计行</b>(NOT EXISTS verification_id IS NULL 的 verification_settled 行)的权益；
     * 遗留行承载的历史交由其自身，不重建、不双计。判别用 verification_id IS NULL(新行必带 verification_id)，无需 cutover 时间。
     */
    // FS-012：补偿补发权益的核销不进普通核销结算事实（在线消费者跳过），补建同样排除，不把它补回原票分账。
    // 第十八批 Q 段 154-R2（BSM:1689「通票权益核销后才按所属域的已生效规则与实际提供方结算；未核销不产生提供方结算」）：
    //   只取在线会发结算事件的成功核销——TicketVerificationService 普通扣次（result_reason 空）与游乐入场（amusement_verified）；
    //   放行不消耗的场次重入（session_reentry）与当天重入（day_replay）不发事件，补建也不补。k 按该权益这些核销（含已有结算行的）
    //   时序编号，等于在线消费者用的 used_times。
    // ponytail: 每次对全部成功核销开窗编号；量大到拖慢夜间任务时，先筛出有缺行的权益再编号。
    public java.util.List<MissingSettledFact> findMissingVerificationSettlementFacts(int limit) {
        return jdbcTemplate.query("""
                WITH consumed AS (
                    SELECT v.id, v.entitlement_id, v.created_at, v.settlement_price_cent,
                           ROW_NUMBER() OVER (PARTITION BY v.entitlement_id ORDER BY v.created_at, v.id) AS k
                      FROM ticket_verifications v
                     WHERE v.status = 'success'
                       AND (v.result_reason IS NULL OR v.result_reason = 'amusement_verified')
                )
                SELECT v.id AS verification_id, v.entitlement_id, v.created_at,
                       e.order_id, e.ticket_type_id, e.entitlement_type, e.merchant_id,
                       e.total_times, e.num_people, e.allocated_revenue_cent, e.valid_to, e.exclusive_merchant_id,
                       v.settlement_price_cent, e.component_full_price_cent, v.k
                  FROM consumed v
                  JOIN issued_entitlements e ON e.id = v.entitlement_id
                 WHERE e.compensation_item_id IS NULL
                   AND NOT EXISTS (SELECT 1 FROM settlement_transactions s WHERE s.transaction_no = 'ST-VS-' || v.id)
                   AND NOT EXISTS (SELECT 1 FROM ticket_verifications r WHERE r.entitlement_id = v.entitlement_id AND r.status = 'reversed')
                   AND NOT EXISTS (SELECT 1 FROM settlement_transactions leg
                                    WHERE leg.entitlement_id = v.entitlement_id
                                      AND leg.transaction_type = 'verification_settled'
                                      AND leg.verification_id IS NULL)
                 ORDER BY v.entitlement_id, v.k
                 LIMIT :limit
                """, new MapSqlParameterSource().addValue("limit", limit), (rs, i) -> new MissingSettledFact(
                        rs.getString("verification_id"), rs.getString("entitlement_id"),
                        rs.getTimestamp("created_at").toInstant(), rs.getString("order_id"),
                        rs.getString("ticket_type_id"), rs.getString("entitlement_type"), rs.getString("merchant_id"),
                        rs.getInt("total_times"), rs.getInt("num_people"),
                        rs.getObject("allocated_revenue_cent") == null ? null : rs.getLong("allocated_revenue_cent"),
                        rs.getTimestamp("valid_to").toInstant(), rs.getString("exclusive_merchant_id"),
                        rs.getObject("settlement_price_cent") == null ? null : rs.getLong("settlement_price_cent"),
                        rs.getObject("component_full_price_cent") == null ? null : rs.getLong("component_full_price_cent"),
                        rs.getInt("k")));
    }

    /** AUD-017 重建源投影：一条缺失核销结算的权威核销事实 + 其时序序号 k（用于算增量 f(k)−f(k−1)）。
     *  D6：加 componentFullPriceCent，重建时用同一增量公式算 verifiedFull 还原折扣（与在线消费者一致）。 */
    public record MissingSettledFact(
            String verificationId, String entitlementId, Instant verifiedAt, String orderId, String ticketTypeId,
            String entitlementType, String merchantId, int totalTimes, int numPeople, Long allocatedRevenueCent,
            Instant validTo, String exclusiveMerchantId, Long settlementPriceCent, Long componentFullPriceCent, int k) {
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
                       settlement_status, created_at,
                       (SELECT v.settlement_catalog_entry_id FROM ticket_verifications v
                         WHERE v.id = settlement_transactions.verification_id) AS settlement_catalog_entry_id
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
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("settlement_catalog_entry_id")));
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
            Instant createdAt,
            // 第十八批 AI 段 236-R1（BSM:1754「每项权益都必须绑定实际提供方和审批版本」；ENUM:54「核销或结算时锁定当时版本」）：
            // 该笔核销所用内部结算价目录行（＝审批版本），非按内部结算价结算的为 null。
            String settlementCatalogEntryId
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
            Instant validTo,
            // S8 未核销款分流:提供该权益的商户业态(merchants.merchant_type,与 S11 池基过滤同一套判定源)。
            String businessFormat,
            // FS-024(V511):「其他」业态商户在档案指定的主管公司;未指定为 null,作业对该商户单行记错、不落账。
            String supervisingCompanyId,
            // FS-023-R1:售票锁定的项目与销售渠道(订单快照,不从操作者反推)。
            String projectId,
            String salesChannel,
            // FS-023-R2/R4:父票、父票人数、首场次键及其营业日与营业时段(秒,可空);人份是否检票、入场次数。
            String parentTicketId,
            int parentNumPeople,
            String firstSessionKey,
            java.time.LocalDate sessionDate,
            Integer sessionOpenSecond,
            Integer sessionCloseSecond,
            boolean slotCheckedIn,
            int slotVisits,
            int slotOpened,
            // FS-023-R3:父票的固定金额渠道费是否已扣过(进池核销锚点已由月结按首笔核销计过,或同票到期事实已扣)。
            boolean parentAnchored,
            // 第十八批第 2 条:酒吧业态非专属权益且父票有池快照——已检票部分已在池内按通票未核销减除额处理。
            boolean pooledAtSale,
            // 第十八批第 2 条:人份序号与该人份仍有效的入场类核销时刻(按营业日计已开启次数)。
            int slotIndex,
            java.util.List<Instant> slotEntryAts,
            // 18AH 复核修复:同父票各人份仍有效的入场类核销时刻(按整张票判本场,与 ticketCheckedInThisSession 同口径)。
            java.util.List<Instant> ticketEntryAts,
            // 第十八批第 3 条:订单付款时固化的销售渠道费配置(官方渠道/存量订单=不扣)。
            JdbcSettlementBillRepository.ChannelFeeConfig salesFee
    ) {
    }

    /** FS-023 到期未核销事实附加列。 */
    public record UnverifiedExtras(String category, long channelFeeCent, String channelFeeConfigId,
                                   long channelFeeResidualCent, String channelFeeResidualEntityId) {
    }

}
