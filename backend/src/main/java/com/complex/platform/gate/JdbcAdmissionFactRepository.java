package com.complex.platform.gate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@link AdmissionFactRepository} 的 JDBC 只读实现：查 contracts / compliance_expiry_items（V005/V028 既有表），不新增迁移。
 * 任一查询抛异常 → 返回 false（fail-open，未命中=放行），异常向上由 {@link AdmissionGuard} 决定是否留痕。
 */
@Repository
class JdbcAdmissionFactRepository implements AdmissionFactRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcAdmissionFactRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isContractExpiredForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 存在一份生效合同（approved 且日期区间覆盖今天） → 未过期。
        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contracts
                 WHERE merchant_id = :merchant_id
                   AND lifecycle_status = 'approved'
                   AND effective_date <= :today
                   AND expiry_date >= :today
                """, Map.of("merchant_id", merchantId, "today", today), Integer.class);
        return activeCount == null || activeCount == 0;
    }

    @Override
    public boolean isQualificationAbnormalForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 存在一条 active 的合规资质项已过期（valid_to < today） → 资质异常。
        Integer expiredCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM compliance_expiry_items
                 WHERE merchant_id = :merchant_id
                   AND subject_type IN ('merchant', 'supplier')
                   AND status = 'active'
                   AND valid_to < :today
                """, Map.of("merchant_id", merchantId, "today", today), Integer.class);
        return expiredCount != null && expiredCount > 0;
    }
}
