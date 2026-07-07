package com.complex.platform.bar;

import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** 酒吧门店主数据仓储（V263）。按 project scope 过滤（direct 表，租户隔离 §6A.1）。 */
@Repository
class JdbcBarVenueRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    JdbcBarVenueRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<BarVenueRecord> list(Optional<String> projectScope, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bars WHERE delete_status <> 'approved'");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (!includeArchived) {
            sql.append(" AND archived_at IS NULL");
        }
        // 项目维度隔离：项目员工只见本项目酒吧；平台管理员传 empty 见全量。
        // 回填的历史酒吧 project_id 为空，平台视角可见，项目视角不误入他项目。
        if (projectScope.isPresent()) {
            sql.append(" AND project_id = :project_id");
            p.addValue("project_id", projectScope.get());
        }
        sql.append(" ORDER BY created_at DESC, id");
        return jdbcTemplate.query(sql.toString(), p, this::map);
    }

    Optional<BarVenueRecord> find(String id) {
        return jdbcTemplate.query("SELECT * FROM bars WHERE id = :id", Map.of("id", id), this::map)
                .stream().findFirst();
    }

    BarVenueRecord save(BarVenueRecord bar) {
        jdbcTemplate.update("""
                INSERT INTO bars (
                    id, project_id, merchant_id, space_id, bar_name, bar_code, minimum_spend_cent, status,
                    archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason,
                    created_at, updated_at
                ) VALUES (
                    :id, :project_id, :merchant_id, :space_id, :bar_name, :bar_code, :minimum_spend_cent, :status,
                    :archived_at, :archived_by, :delete_status, :delete_requested_by, :delete_requested_at, :delete_reason,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    project_id = EXCLUDED.project_id,
                    merchant_id = EXCLUDED.merchant_id,
                    space_id = EXCLUDED.space_id,
                    bar_name = EXCLUDED.bar_name,
                    bar_code = EXCLUDED.bar_code,
                    minimum_spend_cent = EXCLUDED.minimum_spend_cent,
                    status = EXCLUDED.status,
                    archived_at = EXCLUDED.archived_at,
                    archived_by = EXCLUDED.archived_by,
                    delete_status = EXCLUDED.delete_status,
                    delete_requested_by = EXCLUDED.delete_requested_by,
                    delete_requested_at = EXCLUDED.delete_requested_at,
                    delete_reason = EXCLUDED.delete_reason,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", bar.id())
                .addValue("project_id", bar.projectId())
                .addValue("merchant_id", bar.merchantId())
                .addValue("space_id", bar.spaceId())
                .addValue("bar_name", bar.barName())
                .addValue("bar_code", bar.barCode())
                .addValue("minimum_spend_cent", bar.minimumSpendCent())
                .addValue("status", bar.status())
                .addValue("archived_at", timestampWithTimeZone(bar.archivedAt()))
                .addValue("archived_by", bar.archivedBy())
                .addValue("delete_status", bar.deleteStatus() == null ? "none" : bar.deleteStatus())
                .addValue("delete_requested_by", bar.deleteRequestedBy())
                .addValue("delete_requested_at", timestampWithTimeZone(bar.deleteRequestedAt()))
                .addValue("delete_reason", bar.deleteReason())
                .addValue("created_at", timestampWithTimeZone(bar.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(bar.updatedAt())));
        return bar;
    }

    void delete(String id) {
        jdbcTemplate.update("DELETE FROM bars WHERE id = :id", Map.of("id", id));
    }

    /**
     * 酒吧门店活引用：所有以 bar_id 引用本门店的运营表行数之和。>0 拒删（BAR_VENUE_IN_USE）。
     * 显式清单（非元数据驱动）：bar_id 是通用列名，元数据驱动会把别域同名列误算；此处按 2026-07-07 实查
     * complex 库 public schema 带 bar_id 列的 18 张表（含 service_orders 服务点单），全属酒吧运营范畴。
     * 新增带 bar_id 的酒吧运营表时须同步补此清单。
     */
    long countReferences(String barId) {
        Long count = jdbcTemplate.query("""
                SELECT (SELECT COUNT(1) FROM bar_tables WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_products WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_product_categories WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_combos WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_reservations WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_orders WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_bills WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_table_sessions WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_table_session_adjustments WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_shifts WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_deposit_rules WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_gift_redemptions WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_order_item_voids WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_production_tickets WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_production_ticket_events WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_reservation_invitation_members WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM bar_table_guest_checkins WHERE bar_id = :id)
                     + (SELECT COUNT(1) FROM service_orders WHERE bar_id = :id) AS c
                """, Map.of("id", barId), (rs, i) -> rs.getLong("c")).stream().findFirst().orElse(0L);
        return count == null ? 0L : count;
    }

    private BarVenueRecord map(ResultSet rs, int rowNum) throws SQLException {
        Long minimumSpend = rs.getObject("minimum_spend_cent") == null ? null : rs.getLong("minimum_spend_cent");
        return new BarVenueRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("merchant_id"),
                rs.getString("space_id"),
                rs.getString("bar_name"),
                rs.getString("bar_code"),
                minimumSpend,
                rs.getString("status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"),
                instant(rs, "archived_at"),
                rs.getString("archived_by"),
                rs.getString("delete_status"),
                rs.getString("delete_requested_by"),
                instant(rs, "delete_requested_at"),
                rs.getString("delete_reason"));
    }

    private static java.time.Instant instant(ResultSet rs, String col) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toInstant();
    }
}
