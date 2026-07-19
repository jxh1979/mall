package com.complex.platform.settlement;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 六业态收入归属读写(terminology-canonical §三):把六类业态收入流映射到对应 L3 管理公司/业态主体。
 *
 * <p>读(结算用):{@link #resolveManagementEntity} 项目特例优先→平台默认→未配置返 null,
 * 范式照 {@code JdbcSettlementBillRepository.findRetentionBps}(COALESCE(project_id,'__DEFAULT__'))。
 * <p>写(后台可配):{@link #upsert} 覆盖同 (项目, 业态流) 的 active 归属;{@link #list} 全量读。
 */
// ponytail: 单表读写仓储,无接口(one impl);REST/OpenAPI 管理面延后,先打通层级+归属+结算标注。
@Repository
public class RevenueStreamAttributionRepository {

    /** 合法业态收入流(与 V319 CHECK 一致);写入前先校验,避免落库时才被 DB 拒。 */
    public static final Set<String> REVENUE_STREAMS = Set.of(
            "all_pass_ticket", "table_dining", "bar_service_fee",
            "amusement_ticket", "performance_ticket", "venue_rent");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RevenueStreamAttributionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 解析某(项目, 业态收入流)的收入归属主体 id:项目特例优先,否则平台默认,否则 null(未配置)。
     *
     * @param projectId     项目(综合体维度);null=只查平台默认
     * @param revenueStream 业态收入流(见 {@link #REVENUE_STREAMS})
     * @return L3 管理公司 business_entities.id;未配置返 null
     */
    public String resolveManagementEntity(String projectId, String revenueStream) {
        return jdbcTemplate.query("""
                SELECT management_entity_id FROM revenue_stream_attribution
                 WHERE status = 'active'
                   AND revenue_stream = :revenue_stream
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("project_id", projectId)
                        .addValue("revenue_stream", revenueStream),
                rs -> rs.next() ? rs.getString("management_entity_id") : null);
    }

    /**
     * 配置(upsert)某(项目, 业态流)的收入归属主体。同键已有 active 行则覆盖 management_entity_id。
     * 幂等落点 = 唯一键 (COALESCE(project_id,'__DEFAULT__'), revenue_stream) WHERE status='active'。
     */
    public void upsert(RevenueStreamAttributionRecord record) {
        if (!REVENUE_STREAMS.contains(record.revenueStream())) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "INVALID_REVENUE_STREAM", "未知业态收入流:" + record.revenueStream());
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("revenue_stream", record.revenueStream())
                .addValue("management_entity_id", record.managementEntityId())
                .addValue("updated_by", record.updatedBy())
                .addValue("updated_at",
                        java.sql.Timestamp.from(record.updatedAt() == null ? Instant.now() : record.updatedAt()));
        jdbcTemplate.update("""
                INSERT INTO revenue_stream_attribution (
                    id, project_id, revenue_stream, management_entity_id, status, updated_by, updated_at
                )
                VALUES (
                    :id, :project_id, :revenue_stream, :management_entity_id, 'active', :updated_by, :updated_at
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), revenue_stream) WHERE status = 'active'
                DO UPDATE SET
                    management_entity_id = EXCLUDED.management_entity_id,
                    updated_by           = EXCLUDED.updated_by,
                    updated_at           = EXCLUDED.updated_at
                """, params);
    }

    /** 全量读(后台展示/审阅用)。 */
    public List<RevenueStreamAttributionRecord> list() {
        return jdbcTemplate.query("""
                SELECT id, project_id, revenue_stream, management_entity_id, status, updated_by, updated_at
                  FROM revenue_stream_attribution
                 ORDER BY COALESCE(project_id, '__DEFAULT__'), revenue_stream
                """,
                (rs, i) -> new RevenueStreamAttributionRecord(
                        rs.getString("id"),
                        rs.getString("project_id"),
                        rs.getString("revenue_stream"),
                        rs.getString("management_entity_id"),
                        rs.getString("status"),
                        rs.getString("updated_by"),
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant()));
    }
}
