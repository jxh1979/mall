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
     * AUD-012/013/017：append 一条<b>不可变</b>结算事实（核销增量 / 冲正负向 / 未核销到期归项目共用）。
     * 幂等锚点=全局唯一 {@code transaction_no}（uq_st_transaction_no）→ ON CONFLICT DO NOTHING：
     * 同一业务事件重放不重复入账，且<b>永不 UPDATE</b>（账本 append-only，历史行不可变）。
     */
    public void insertImmutableFact(SettlementTransactionRecord record) {
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
                .addValue("gift_cost_config_id", record.giftCostConfigId());

        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (
                    id, transaction_no, transaction_type, entitlement_id, order_id, ticket_type_id,
                    verification_id, entitlement_type, verified_count, total_times, num_people,
                    allocated_revenue_cent, verified_amount_cent, unverified_amount_cent,
                    beneficiary_type, merchant_id, project_id, settlement_status, entitlement_valid_to,
                    exclusive_merchant_id, business_at, discount_amount_cent,
                    comp_service_fee_cent, comp_service_fee_config_id, beneficiary_entity_id,
                    parent_ticket_id, gift_cost_cent, gift_cost_config_id
                )
                VALUES (
                    :id, :transaction_no, :transaction_type, :entitlement_id, :order_id, :ticket_type_id,
                    :verification_id, :entitlement_type, :verified_count, :total_times, :num_people,
                    :allocated_revenue_cent, :verified_amount_cent, :unverified_amount_cent,
                    :beneficiary_type, :merchant_id, :project_id, :settlement_status, :entitlement_valid_to,
                    :exclusive_merchant_id, :business_at, :discount_amount_cent,
                    :comp_service_fee_cent, :comp_service_fee_config_id, :beneficiary_entity_id,
                    :parent_ticket_id, :gift_cost_cent, :gift_cost_config_id
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
                       gift_cost_cent, gift_cost_config_id
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
                rs.getLong("gift_cost_cent"), rs.getString("gift_cost_config_id")) : null;
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
            long giftCostCent, String giftCostConfigId) {
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
     *   <li>{@code status='active'}：已成功退款(status='refunded')/已没收(forfeited)的权益天然排除——
     *       退款给客户的金额不能再以未使用到期名义归项目(AUD-019 根因)；</li>
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
    public java.util.List<ExpiredUnverifiedRow> findExpiredUnverifiedEntitlements(Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        return jdbcTemplate.query("""
                SELECT e.id, e.order_id, e.ticket_type_id, e.entitlement_type, e.merchant_id,
                       e.total_times, e.used_times, e.allocated_revenue_cent, e.valid_to,
                       m.merchant_type AS business_format
                  FROM issued_entitlements e
                  LEFT JOIN merchants m ON m.id = e.merchant_id
                 WHERE e.valid_to < :now
                   AND e.total_times > e.used_times
                   AND e.allocated_revenue_cent IS NOT NULL
                   AND e.status = 'active'
                   -- 已知边界(低危,复核提示)：V285 之前遗留权益 parent_ticket_id 可能为 NULL，此时下句 NOT EXISTS 恒真、
                   --   父实例次级守卫失效；但「退款申请中(refund_pending)」是靠父实例冻结实现的，无父实例的遗留权益不会进入
                   --   该窄窗；已成功退款仍由上句 e.status='active'(退款→refunded)主挡。故仅「遗留 NULL 父 + refund_pending」
                   --   这一不会发生的组合未被次级守卫覆盖，无实际敞口。
                   AND NOT EXISTS (
                       SELECT 1 FROM ticket_instances ti
                        WHERE ti.id = e.parent_ticket_id
                          AND ti.status <> 'active')
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
                        rs.getString("business_format")));
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
    public java.util.List<MissingSettledFact> findMissingVerificationSettlementFacts(int limit) {
        return jdbcTemplate.query("""
                SELECT v.id AS verification_id, v.entitlement_id, v.created_at,
                       e.order_id, e.ticket_type_id, e.entitlement_type, e.merchant_id,
                       e.total_times, e.num_people, e.allocated_revenue_cent, e.valid_to, e.exclusive_merchant_id,
                       v.settlement_price_cent, e.component_full_price_cent,
                       ROW_NUMBER() OVER (PARTITION BY v.entitlement_id ORDER BY v.created_at, v.id) AS k
                  FROM ticket_verifications v
                  JOIN issued_entitlements e ON e.id = v.entitlement_id
                 WHERE v.status = 'success'
                   AND NOT EXISTS (SELECT 1 FROM settlement_transactions s WHERE s.transaction_no = 'ST-VS-' || v.id)
                   AND NOT EXISTS (SELECT 1 FROM ticket_verifications r WHERE r.entitlement_id = v.entitlement_id AND r.status = 'reversed')
                   AND NOT EXISTS (SELECT 1 FROM settlement_transactions leg
                                    WHERE leg.entitlement_id = v.entitlement_id
                                      AND leg.transaction_type = 'verification_settled'
                                      AND leg.verification_id IS NULL)
                 ORDER BY v.entitlement_id, k
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
            Instant validTo,
            // S8 未核销款分流:提供该权益的商户业态(merchants.merchant_type,与 S11 池基过滤同一套判定源)。
            // amusement → 未核销款归游乐公司;其余 → 归娱乐平台公司(业务 HTML「通票结算口径」)。
            String businessFormat
    ) {
    }

}
