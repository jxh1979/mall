package com.complex.platform.bar;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBarSessionRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;

    JdbcBarSessionRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
    }

    public BarTableSessionRecord saveTableSession(BarTableSessionRecord session) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_sessions (
                    id, bar_id, merchant_id, business_date, table_id, reservation_id,
                    customer_id, guest_count, min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                    status, opened_at, closed_at, source_session_id, session_version,
                    created_at, updated_at
                )
                VALUES (
                    :id, :bar_id, :merchant_id, :business_date, :table_id, :reservation_id,
                    :customer_id, :guest_count, :min_consumption_snapshot_cent,
                    :allowed_pass_free_guest_count_snapshot, :sales_staff_id, :service_staff_id,
                    :status, :opened_at, :closed_at, :source_session_id, :session_version,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    bar_id = EXCLUDED.bar_id,
                    merchant_id = EXCLUDED.merchant_id,
                    business_date = EXCLUDED.business_date,
                    table_id = EXCLUDED.table_id,
                    reservation_id = EXCLUDED.reservation_id,
                    customer_id = EXCLUDED.customer_id,
                    guest_count = EXCLUDED.guest_count,
                    min_consumption_snapshot_cent = EXCLUDED.min_consumption_snapshot_cent,
                    allowed_pass_free_guest_count_snapshot = EXCLUDED.allowed_pass_free_guest_count_snapshot,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    service_staff_id = EXCLUDED.service_staff_id,
                    status = EXCLUDED.status,
                    opened_at = EXCLUDED.opened_at,
                    closed_at = EXCLUDED.closed_at,
                    source_session_id = EXCLUDED.source_session_id,
                    session_version = EXCLUDED.session_version,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.tableSessionParameters(session));
        return session;
    }

    public List<BarTableSessionRecord> listTableSessions(Optional<String> merchantScope) {
        if (merchantScope.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                           customer_id, guest_count, min_consumption_snapshot_cent,
                           allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                           status, opened_at, closed_at, source_session_id, session_version,
                           created_at, updated_at
                      FROM bar_table_sessions
                     ORDER BY created_at DESC, id
                    """, rowMapper::mapTableSession);
        }
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapTableSession);
    }

    public Optional<BarTableSessionRecord> findTableSessionById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bar_id, merchant_id, business_date, table_id, reservation_id,
                       customer_id, guest_count, min_consumption_snapshot_cent,
                       allowed_pass_free_guest_count_snapshot, sales_staff_id, service_staff_id,
                       status, opened_at, closed_at, source_session_id, session_version,
                       created_at, updated_at
                  FROM bar_table_sessions
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapTableSession).stream().findFirst();
    }

    public BarTableSessionAdjustmentRecord saveTableSessionAdjustment(BarTableSessionAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_table_session_adjustments (
                    id, session_id, adjustment_type, source_session_id, target_session_id,
                    from_table_id, to_table_id, bar_id, merchant_id, business_date,
                    min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                    additional_min_consumption_cent, additional_deposit_amount_cent,
                    additional_service_fee_cent, reason, requested_by_user_id,
                    approval_required, status, idempotency_key, evidence_attachment_ids,
                    snapshot_json, created_at
                )
                VALUES (
                    :id, :session_id, :adjustment_type, :source_session_id, :target_session_id,
                    :from_table_id, :to_table_id, :bar_id, :merchant_id, :business_date,
                    :min_consumption_before_cent, :min_consumption_after_cent, :renewed_until,
                    :additional_min_consumption_cent, :additional_deposit_amount_cent,
                    :additional_service_fee_cent, :reason, :requested_by_user_id,
                    :approval_required, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb),
                    :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    snapshot_json = EXCLUDED.snapshot_json
                """, parameterSource.tableSessionAdjustmentParameters(adjustment));
        return adjustment;
    }

    public List<BarTableSessionAdjustmentRecord> listTableSessionAdjustments(String sessionId) {
        return jdbcTemplate.query("""
                SELECT id, session_id, adjustment_type, source_session_id, target_session_id,
                       from_table_id, to_table_id, bar_id, merchant_id, business_date,
                       min_consumption_before_cent, min_consumption_after_cent, renewed_until,
                       additional_min_consumption_cent, additional_deposit_amount_cent,
                       additional_service_fee_cent, reason, requested_by_user_id,
                       approval_required, status, idempotency_key, evidence_attachment_ids,
                       snapshot_json, created_at
                  FROM bar_table_session_adjustments
                 WHERE session_id = :session_id
                 ORDER BY created_at DESC, id
                """, Map.of("session_id", sessionId), rowMapper::mapTableSessionAdjustment);
    }
}
