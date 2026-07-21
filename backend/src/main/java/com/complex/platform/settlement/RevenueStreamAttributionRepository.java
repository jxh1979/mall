package com.complex.platform.settlement;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
        // V329 结算配置中心批1:草案(is_draft=TRUE)不进结算读侧,只读正式生效归属(与 6 张单行 scope 表 finder 一致)。
        // 现存行 V329 前无 is_draft、迁移默认 FALSE=视同已批准,故本过滤对现存数据零行为变更。
        return jdbcTemplate.query("""
                SELECT management_entity_id FROM revenue_stream_attribution
                 WHERE status = 'active' AND is_draft = FALSE
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
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), revenue_stream, is_draft) WHERE status = 'active'
                DO UPDATE SET
                    management_entity_id = EXCLUDED.management_entity_id,
                    updated_by           = EXCLUDED.updated_by,
                    updated_at           = EXCLUDED.updated_at
                """, params);
    }

    /**
     * V329(复核修订):落草案归属(is_draft=TRUE)走 <b>Path 2</b> —— INSERT 草案行 ON CONFLICT 到「同 scope 的草案槽」
     * ((scope, revenue_stream, is_draft) WHERE status='active'),<b>不动生效行</b>(is_draft=FALSE),编辑期归属不回落 null(保值)。
     * RETURNING id 返回真实落地行 id(草案槽命中保留首行 id),供审计 business_id 用。
     */
    public String upsertDraft(RevenueStreamAttributionRecord record) {
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
        return jdbcTemplate.queryForObject("""
                INSERT INTO revenue_stream_attribution (
                    id, project_id, revenue_stream, management_entity_id, status, is_draft, updated_by, updated_at
                )
                VALUES (
                    :id, :project_id, :revenue_stream, :management_entity_id, 'active', TRUE, :updated_by, :updated_at
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), revenue_stream, is_draft) WHERE status = 'active'
                DO UPDATE SET
                    management_entity_id = EXCLUDED.management_entity_id,
                    updated_by           = EXCLUDED.updated_by,
                    updated_at           = EXCLUDED.updated_at
                RETURNING id
                """, params, String.class);
    }

    /**
     * V329(复核修订):复核转正走 <b>Path 2 换行</b>(@Transactional):① 旧生效归属行软替换 status→inactive(留库不物理删);
     * ② 翻草案为生效。返回②行数(0=不存在/非草案)。scope「同一」= 同 (COALESCE(project_id),revenue_stream)。
     */
    @Transactional
    public int activateDraft(String id, String actorId, Instant now) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", id).addValue("updated_by", actorId)
                .addValue("now", java.sql.Timestamp.from(now));
        // ① 旧生效归属行软替换 → inactive
        jdbcTemplate.update("""
                UPDATE revenue_stream_attribution l
                   SET status = 'inactive', updated_by = :updated_by, updated_at = :now
                  FROM revenue_stream_attribution d
                 WHERE d.id = :id AND d.is_draft = TRUE AND d.status = 'active'
                   AND l.is_draft = FALSE AND l.status = 'active' AND l.id <> d.id
                   AND COALESCE(l.project_id, '__DEFAULT__') = COALESCE(d.project_id, '__DEFAULT__')
                   AND l.revenue_stream = d.revenue_stream
                """, p);
        // ② 翻草案为生效
        return jdbcTemplate.update("""
                UPDATE revenue_stream_attribution
                   SET is_draft = FALSE, updated_by = :updated_by, updated_at = :now
                 WHERE id = :id AND is_draft = TRUE AND status = 'active'
                """, p);
    }

    /** V329 软删铁律:归属条目下线 = status→inactive + archive_reason(不物理删,仅 active 行可下线)。返回更新行数。 */
    public int softDelete(String id, String archiveReason, String actorId, Instant now) {
        return jdbcTemplate.update("""
                UPDATE revenue_stream_attribution
                   SET status = 'inactive', archive_reason = :archive_reason,
                       updated_by = :updated_by, updated_at = :now
                 WHERE id = :id AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("archive_reason", archiveReason)
                .addValue("updated_by", actorId)
                .addValue("now", java.sql.Timestamp.from(now)));
    }

    /** 草案归属起草人(供差人转正守卫;不存在/非草案返 null)。 */
    public String draftUpdatedBy(String id) {
        return jdbcTemplate.query(
                "SELECT updated_by FROM revenue_stream_attribution WHERE id = :id AND is_draft = TRUE AND status = 'active'",
                new MapSqlParameterSource().addValue("id", id),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    /** active 归属行最后编辑人(供软删差人守卫;不存在返 null)。 */
    public String activeUpdatedBy(String id) {
        return jdbcTemplate.query(
                "SELECT updated_by FROM revenue_stream_attribution WHERE id = :id AND status = 'active'",
                new MapSqlParameterSource().addValue("id", id),
                rs -> rs.next() ? rs.getString(1) : null);
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
