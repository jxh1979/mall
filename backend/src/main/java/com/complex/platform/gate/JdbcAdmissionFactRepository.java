package com.complex.platform.gate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@link AdmissionFactRepository} 的 JDBC 只读实现：查 contracts / compliance_expiry_items
 * （V010/V028 既有表）+ contracts.allowed_business_scope / contract_spaces（V080）。
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

    @Override
    public boolean isOverContractScopeForMerchant(String merchantId, String businessDomain) {
        if (merchantId == null || merchantId.isBlank() || businessDomain == null || businessDomain.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 该商户当前所有生效合同（approved + 日期覆盖今天）。无生效合同 → 命中（与 expired 一致）。
        Integer activeContractCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contracts
                 WHERE merchant_id = :merchant_id
                   AND lifecycle_status = 'approved'
                   AND effective_date <= :today
                   AND expiry_date >= :today
                """, Map.of("merchant_id", merchantId, "today", today), Integer.class);
        if (activeContractCount == null || activeContractCount == 0) {
            return true;
        }
        // 任一生效合同 scope 含该业务域，或 scope 为空 [] (未配置=不限) → 放行。否则 → 越界命中。
        Integer scopeOkCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contracts
                 WHERE merchant_id = :merchant_id
                   AND lifecycle_status = 'approved'
                   AND effective_date <= :today
                   AND expiry_date >= :today
                   AND (
                         allowed_business_scope @> to_jsonb(:business_domain::text)
                      OR jsonb_array_length(allowed_business_scope) = 0
                   )
                """,
                Map.of("merchant_id", merchantId, "today", today, "business_domain", businessDomain),
                Integer.class);
        return scopeOkCount == null || scopeOkCount == 0;
    }

    @Override
    public boolean isOverSpaceForMerchant(String merchantId, String spaceId) {
        if (merchantId == null || merchantId.isBlank() || spaceId == null || spaceId.isBlank()) {
            return false;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        // 该商户存在一份生效合同经 contract_spaces 关联到该空间 → 在租赁内（放行）；否则越界命中。
        Integer leasedCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contract_spaces cs
                  JOIN contracts c ON c.id = cs.contract_id
                 WHERE cs.space_id = :space_id
                   AND c.merchant_id = :merchant_id
                   AND c.lifecycle_status = 'approved'
                   AND c.effective_date <= :today
                   AND c.expiry_date >= :today
                """,
                Map.of("merchant_id", merchantId, "space_id", spaceId, "today", today),
                Integer.class);
        return leasedCount == null || leasedCount == 0;
    }

    @Override
    public Optional<String> resolveSpaceId(String objectType, String objectId) {
        if (objectType == null || objectType.isBlank() || objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        // 仅支持有直连 space_id 列的对象类型；其余无 对象→空间 映射 → empty（放行）。
        String sql = switch (objectType) {
            case "merchant_service_work_order" ->
                    "SELECT space_id FROM merchant_service_work_orders WHERE id = :object_id";
            default -> null;
        };
        if (sql == null) {
            return Optional.empty();
        }
        List<String> rows = jdbcTemplate.queryForList(sql, Map.of("object_id", objectId), String.class);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        String spaceId = rows.get(0);
        return (spaceId == null || spaceId.isBlank()) ? Optional.empty() : Optional.of(spaceId);
    }
}
