package com.complex.platform.ops.productionreadiness;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProductionReadinessRepository implements ProductionReadinessRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcProductionReadinessRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProductionReadinessItem> findAllItems() {
        return jdbcTemplate.query("""
                SELECT id, category, item_name, default_solution, owner_department, owner_person,
                       supplier_name, status, target_date, evidence_link, last_test_time, blocking_scope,
                       fallback_mode, credential_ref, runbook_ref, next_action, updated_at, updated_by
                  FROM production_readiness_items
                 ORDER BY id
                """, this::mapItem);
    }

    @Override
    public List<ProductionReadinessItem> findItemsByCategory(String category) {
        return jdbcTemplate.query("""
                SELECT id, category, item_name, default_solution, owner_department, owner_person,
                       supplier_name, status, target_date, evidence_link, last_test_time, blocking_scope,
                       fallback_mode, credential_ref, runbook_ref, next_action, updated_at, updated_by
                  FROM production_readiness_items
                 WHERE category = :category
                 ORDER BY id
                """, Map.of("category", category), this::mapItem);
    }

    @Override
    public Optional<ProductionReadinessItem> findItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, category, item_name, default_solution, owner_department, owner_person,
                       supplier_name, status, target_date, evidence_link, last_test_time, blocking_scope,
                       fallback_mode, credential_ref, runbook_ref, next_action, updated_at, updated_by
                  FROM production_readiness_items
                 WHERE id = :id
                """, Map.of("id", id), this::mapItem).stream().findFirst();
    }

    @Override
    public void saveDefaultItems(List<ProductionReadinessItem> defaultItems) {
        String sql = """
                INSERT INTO production_readiness_items (
                    id, category, item_name, default_solution, owner_department, owner_person,
                    supplier_name, status, target_date, evidence_link, last_test_time, blocking_scope,
                    fallback_mode, credential_ref, runbook_ref, next_action, updated_at, updated_by
                )
                VALUES (
                    :id, :category, :item_name, :default_solution, :owner_department, :owner_person,
                    :supplier_name, :status, :target_date, :evidence_link, :last_test_time, :blocking_scope,
                    :fallback_mode, :credential_ref, :runbook_ref, :next_action, :updated_at, :updated_by
                )
                ON CONFLICT (id) DO NOTHING
                """;
        jdbcTemplate.batchUpdate(sql, defaultItems.stream()
                .map(this::parametersForItem)
                .toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public ProductionReadinessItem saveItem(ProductionReadinessItem item) {
        String sql = """
                INSERT INTO production_readiness_items (
                    id, category, item_name, default_solution, owner_department, owner_person,
                    supplier_name, status, target_date, evidence_link, last_test_time, blocking_scope,
                    fallback_mode, credential_ref, runbook_ref, next_action, updated_at, updated_by
                )
                VALUES (
                    :id, :category, :item_name, :default_solution, :owner_department, :owner_person,
                    :supplier_name, :status, :target_date, :evidence_link, :last_test_time, :blocking_scope,
                    :fallback_mode, :credential_ref, :runbook_ref, :next_action, :updated_at, :updated_by
                )
                ON CONFLICT (id) DO UPDATE SET
                    owner_department = EXCLUDED.owner_department,
                    owner_person = EXCLUDED.owner_person,
                    supplier_name = EXCLUDED.supplier_name,
                    status = EXCLUDED.status,
                    target_date = EXCLUDED.target_date,
                    evidence_link = EXCLUDED.evidence_link,
                    last_test_time = EXCLUDED.last_test_time,
                    blocking_scope = EXCLUDED.blocking_scope,
                    fallback_mode = EXCLUDED.fallback_mode,
                    credential_ref = EXCLUDED.credential_ref,
                    runbook_ref = EXCLUDED.runbook_ref,
                    next_action = EXCLUDED.next_action,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by
                """;
        jdbcTemplate.update(sql, parametersForItem(item));
        return item;
    }

    @Override
    public ProductionReadinessCheck saveCheck(ProductionReadinessCheck check, List<ProductionReadinessItem> blockers) {
        jdbcTemplate.update("""
                INSERT INTO production_readiness_checks (
                    id, check_scope, result, blocker_count, operator_note, checked_at, checked_by, idempotency_key
                )
                VALUES (
                    :id, :check_scope, :result, :blocker_count, :operator_note, :checked_at, :checked_by, :idempotency_key
                )
                """, new MapSqlParameterSource()
                .addValue("id", check.id())
                .addValue("check_scope", check.checkScope())
                .addValue("result", check.result())
                .addValue("blocker_count", check.blockerCount())
                .addValue("operator_note", check.operatorNote())
                .addValue("checked_at", timestampWithTimeZone(check.checkedAt()))
                .addValue("checked_by", check.checkedBy())
                .addValue("idempotency_key", check.idempotencyKey()));

        if (!blockers.isEmpty()) {
            jdbcTemplate.batchUpdate("""
                    INSERT INTO production_readiness_check_blockers (
                        check_id, item_id, status, blocking_scope
                    )
                    VALUES (
                        :check_id, :item_id, :status, :blocking_scope
                    )
                    """, blockers.stream()
                    .map(item -> new MapSqlParameterSource()
                            .addValue("check_id", check.id())
                            .addValue("item_id", item.id())
                            .addValue("status", item.status().code())
                            .addValue("blocking_scope", item.blockingScope()))
                    .toArray(MapSqlParameterSource[]::new));
        }
        return check;
    }

    private MapSqlParameterSource parametersForItem(ProductionReadinessItem item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("category", item.category())
                .addValue("item_name", item.itemName())
                .addValue("default_solution", item.defaultSolution())
                .addValue("owner_department", item.ownerDepartment())
                .addValue("owner_person", item.ownerPerson())
                .addValue("supplier_name", item.supplierName())
                .addValue("status", item.status().code())
                .addValue("target_date", item.targetDate())
                .addValue("evidence_link", item.evidenceLink())
                .addValue("last_test_time", timestampWithTimeZone(item.lastTestTime()))
                .addValue("blocking_scope", item.blockingScope())
                .addValue("fallback_mode", item.fallbackMode())
                .addValue("credential_ref", item.credentialRef())
                .addValue("runbook_ref", item.runbookRef())
                .addValue("next_action", item.nextAction())
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()))
                .addValue("updated_by", item.updatedBy());
    }

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private ProductionReadinessItem mapItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductionReadinessItem(
                resultSet.getString("id"),
                resultSet.getString("category"),
                resultSet.getString("item_name"),
                resultSet.getString("default_solution"),
                resultSet.getString("owner_department"),
                resultSet.getString("owner_person"),
                resultSet.getString("supplier_name"),
                ProductionReadinessStatus.fromCode(resultSet.getString("status")),
                resultSet.getObject("target_date", LocalDate.class),
                resultSet.getString("evidence_link"),
                nullableInstant(resultSet, "last_test_time"),
                resultSet.getString("blocking_scope"),
                resultSet.getString("fallback_mode"),
                resultSet.getString("credential_ref"),
                resultSet.getString("runbook_ref"),
                resultSet.getString("next_action"),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getString("updated_by")
        );
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        var timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
