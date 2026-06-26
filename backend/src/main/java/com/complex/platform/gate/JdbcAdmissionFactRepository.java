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
 * 任一查询抛异常 → 向上抛出，由 {@link AdmissionGuard} 决定 fail-open（非越界类）或
 * <b>fail-closed 进复核</b>（over_contract_scope / over_space 越界类，P0）。
 * {@link #resolveSpaceId} 返回 empty 是"不可解析"的明确信号，上层据此对 over_space 保守进复核。
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
        // 存在一份生效合同（effective 且日期区间覆盖今天） → 未过期。
        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contracts
                 WHERE merchant_id = :merchant_id
                   AND lifecycle_status = 'effective'
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
        // 该商户当前所有生效合同（effective + 日期覆盖今天）。无生效合同 → 命中（与 expired 一致）。
        Integer activeContractCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                  FROM contracts
                 WHERE merchant_id = :merchant_id
                   AND lifecycle_status = 'effective'
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
                   AND lifecycle_status = 'effective'
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
                   AND c.lifecycle_status = 'effective'
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
        // 已支持解析的受控 objectType（解析到 space → over_space 真实判定）：
        //   - merchant_service_work_order：直连 merchant_service_work_orders.space_id（V022）。
        //   - bar_bill：objectId=bill_id → bar_bills.table_id → bar_tables.space_id（V084 多级解析）。
        //   - inspection / amusement_maintenance_record / amusement_incident / amusement_group_booking：
        //     objectId → 各表 project_id → amusement_projects.space_id（V084）。
        // P0 fail-closed 契约变更：empty 不再等于"over_space 静默放行"。AdmissionGuard 对配置了
        //   over_space 策略却返回 empty 的场景一律保守进复核（admission.space_unresolved_closed）。
        //   因此 finance_*（ar_ap_item/finance_refund/...）、ticketing_*（ticket_refund/verification/...）、
        //   inventory_*（saved_bottle/inventory_stock_adjustment/inventory_purchase_request）等业务对象
        //   表无空间归属列（仅 target_id/location_id/bar_project_id 等，非 spaces.id），无法解析到 space；
        //   按 V099 设计这些域改走 over_contract_scope（基于业务域，不需 space），故不在此补 over_space 解析。
        //   若后续给某域配置了 over_space 策略，default→empty 将令其在 AdmissionGuard 走 fail-closed 进复核
        //   （明确"不可解析"信号 → 保守而非放行），不会再 silent pass。
        //   amusement_manual_override（写接口首参非 String，objectId=null）同理走经营范围。
        String sql = switch (objectType) {
            case "merchant_service_work_order" ->
                    "SELECT space_id FROM merchant_service_work_orders WHERE id = :object_id";
            case "bar_bill" -> """
                    SELECT t.space_id
                      FROM bar_bills b
                      JOIN bar_tables t ON t.id = b.table_id
                     WHERE b.id = :object_id
                    """;
            case "inspection" -> """
                    SELECT p.space_id
                      FROM amusement_safety_inspections i
                      JOIN amusement_projects p ON p.id = i.project_id
                     WHERE i.id = :object_id
                    """;
            case "amusement_maintenance_record" -> """
                    SELECT p.space_id
                      FROM amusement_maintenance_record m
                      JOIN amusement_projects p ON p.id = m.project_id
                     WHERE m.id = :object_id
                    """;
            case "amusement_incident" -> """
                    SELECT p.space_id
                      FROM amusement_incidents inc
                      JOIN amusement_projects p ON p.id = inc.project_id
                     WHERE inc.id = :object_id
                    """;
            case "amusement_group_booking" -> """
                    SELECT p.space_id
                      FROM amusement_group_bookings g
                      JOIN amusement_projects p ON p.id = g.project_id
                     WHERE g.id = :object_id
                    """;
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
