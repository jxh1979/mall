package com.complex.platform.finance;

// 文件版本：V0.01

import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 第十八批 AV 段第 305 条 305-R1（PGM:528「④看不到别的商户和别的项目的数据」；PGM:664「单店层面的财务工作——……差异认定……由该酒吧对应的财务 BP 负责」；
 * ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定」）：登记对账差异时取业务单号在平台侧的归属，供核对所选批次的日结范围。
 * 取数次序与过滤条件同 {@link JdbcFinanceRepository#aggregatePlatformExpectedForBusinessRef}：收款记录、已付票务订单、酒吧账单、已核验经销到账。
 */
@Repository
class JdbcReconciliationBusinessRefRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcReconciliationBusinessRefRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 业务单号的平台侧归属；平台侧查不到（缺失差异的平台侧缺失、未支付票务、待核验经销到账）为空。 */
    Optional<Owner> findOwner(String businessRef) {
        return jdbcTemplate.query("""
                SELECT x.project_id, x.merchant_id, x.bound_bar_id,
                       EXISTS (SELECT 1 FROM merchants bm WHERE bm.id IN (x.merchant_id, x.bound_bar_id) AND bm.merchant_type = 'bar') AS under_bar
                  FROM (
                        SELECT 1 AS ord, COALESCE(i.project_id, im.project_id) AS project_id, i.merchant_id,
                               CAST(NULL AS VARCHAR) AS bound_bar_id
                          FROM ar_ap_payments p
                          JOIN ar_ap_items i ON i.id = p.ar_ap_item_id
                          LEFT JOIN merchants im ON im.id = i.merchant_id
                         WHERE p.id = :business_ref
                        UNION ALL
                        SELECT 2, o.project_id, o.merchant_id, o.bar_id
                          FROM ticket_orders o
                         WHERE (o.id = :business_ref OR o.order_no = :business_ref) AND o.pay_status = 'paid'
                        UNION ALL
                        SELECT 3, bbm.project_id, COALESCE(b.merchant_id, b.bar_id), CAST(NULL AS VARCHAR)
                          FROM bar_bills b
                          LEFT JOIN merchants bbm ON bbm.id = COALESCE(b.merchant_id, b.bar_id)
                         WHERE b.id = :business_ref OR b.bill_no = :business_ref
                        UNION ALL
                        SELECT 4, d.project_id, CAST(NULL AS VARCHAR), CAST(NULL AS VARCHAR)
                          FROM dealer_order_payments dp
                          JOIN dealer_orders dor ON dor.id = dp.order_id
                          JOIN dealers d ON d.id = dor.dealer_id
                         WHERE dp.id = :business_ref AND dp.status = 'verified'
                       ) x
                 ORDER BY x.ord
                 LIMIT 1
                """, Map.of("business_ref", businessRef), (rs, rowNumber) -> new Owner(rs.getString("project_id"),
                rs.getString("merchant_id"), rs.getString("bound_bar_id"), rs.getBoolean("under_bar"))).stream().findFirst();
    }

    /**
     * 归属：项目（收款记录取所属台账的项目，台账无项目取商户所属项目）、商户、单店票绑定酒吧（V288）、是否酒吧的单。
     * 口径同 161-R2 已落地的日结范围划分（{@code JdbcFinanceRepository} 的 ticketOrderInScope、openDifferenceAmounts）。
     */
    record Owner(String projectId, String merchantId, String boundBarId, boolean underBar) {

        /** merchant、bar 批次认该商户的单（含单店票的绑定酒吧）；project 批次认该综合体里酒吧以外的单。 */
        boolean within(String scopeType, String scopeId) {
            if ("project".equals(scopeType)) {
                return scopeId.equals(projectId) && !underBar;
            }
            return scopeId.equals(merchantId) || scopeId.equals(boundBarId);
        }
    }
}
