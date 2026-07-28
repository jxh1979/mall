package com.complex.platform.settlement;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业态收入归属读写:把 9 类收入流映射到收款方 —— 或某个经营主体(L2 项目公司 / L3 业态主体),
 * 或「产生这笔收入的各商户自己」(V355 beneficiary_kind)。
 *
 * <p>读(结算用):{@link #resolveManagementEntity} 项目特例优先→平台默认→未配置返 null,
 * 范式照 {@code JdbcSettlementBillRepository.findRetentionBps}(COALESCE(project_id,'__DEFAULT__'))。
 * <p>写(后台可配):{@link #upsert} 覆盖同 (项目, 业态流) 的 active 归属;{@link #list} 全量读。
 */
// ponytail: 单表读写仓储,无接口(one impl);REST/OpenAPI 管理面延后,先打通层级+归属+结算标注。
@Repository
public class RevenueStreamAttributionRepository {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(RevenueStreamAttributionRepository.class);

    /**
     * 合法业态收入流(与 V355 CHECK 一致);写入前先校验,避免落库时才被 DB 拒。
     * 口径逐条来自业务速查表「六业态收入归属」+ 系统地图「通票收入分账全流程/通票结算口径」。
     */
    public static final Set<String> REVENUE_STREAMS = Set.of(
            "all_pass_ticket", "table_dining", "bar_service_fee",
            "amusement_ticket", "performance_ticket", "venue_rent",
            "ticket_service_fee", "pass_gift_cost", "supply_goods");

    /** 收款方类型:归某个经营主体 / 归产生该笔收入的各商户自己(与 V355 chk_rsa_beneficiary_kind 一致)。 */
    public static final Set<String> BENEFICIARY_KINDS = Set.of("entity", "per_merchant");

    /** 解析结果:收款方类型 + 主体 id(per_merchant 时主体 id 为 null,表示归各商户自己,不是「未配置」)。 */
    public record Attribution(String beneficiaryKind, String managementEntityId) {
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 归属缺失时是否阻断(fail-loud)。
     *
     * <p>新瀑布里赠送权益预估成本(G)与门票综合服务费(F)的收款方就是这个解析结果,
     * 返 null 意味着「有金额没有收款方」——对账时业务人员看不出这笔钱该找谁要,
     * 按 DEC-DATA-BACKEND-ACCURATE-001「宁可阻断不可近似」应当直接拒绝结算。
     *
     * <p>🔴 本刀(S1)默认 <b>false</b>:只观测告警、不阻断,行为与 V355 之前逐字一致
     * (未配置返 null),引擎逐分不变。等运营把 9 条归属在结算配置中心配齐、
     * S2/S6 真正用到收款方时再由用户拍板翻开关。
     */
    private final boolean failLoudOnMissing;

    public RevenueStreamAttributionRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            @org.springframework.beans.factory.annotation.Value(
                    "${settlement.attribution.fail-loud.enabled:false}") boolean failLoudOnMissing) {
        this.jdbcTemplate = jdbcTemplate;
        this.failLoudOnMissing = failLoudOnMissing;
    }

    /**
     * 解析某(项目, 业态收入流)的收入归属主体 id:项目特例优先,否则平台默认,否则 null(未配置)。
     *
     * @param projectId     项目(综合体维度);null=只查平台默认
     * @param revenueStream 业态收入流(见 {@link #REVENUE_STREAMS})
     * @return L3 管理公司 business_entities.id;未配置返 null
     */
    public String resolveManagementEntity(String projectId, String revenueStream) {
        Attribution hit = resolveAttribution(projectId, revenueStream);
        return hit == null ? null : hit.managementEntityId();
    }

    /**
     * 解析某(项目, 业态收入流)的收款方:项目特例优先,否则平台默认,否则未配置。
     *
     * <p>与 {@link #resolveManagementEntity} 的区别:本方法能区分「归各商户自己(per_merchant)」
     * 与「压根没配」——前者主体 id 本就为 null 且完全合法,后者是配置缺口。
     *
     * @return 未配置返 null(阻断开关关时);开关开时未配置直接抛 REVENUE_STREAM_ATTRIBUTION_MISSING
     */
    public Attribution resolveAttribution(String projectId, String revenueStream) {
        // V329 结算配置中心批1:草案(is_draft=TRUE)不进结算读侧,只读正式生效归属(与 6 张单行 scope 表 finder 一致)。
        // 现存行 V329 前无 is_draft、迁移默认 FALSE=视同已批准,故本过滤对现存数据零行为变更。
        Attribution hit = jdbcTemplate.query("""
                SELECT beneficiary_kind, management_entity_id FROM revenue_stream_attribution
                 WHERE status = 'active' AND is_draft = FALSE
                   AND revenue_stream = :revenue_stream
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """,
                new MapSqlParameterSource()
                        .addValue("project_id", projectId)
                        .addValue("revenue_stream", revenueStream),
                rs -> rs.next()
                        ? new Attribution(rs.getString("beneficiary_kind"), rs.getString("management_entity_id"))
                        : null);
        if (hit == null) {
            String scope = projectId == null ? "平台默认" : projectId;
            if (failLoudOnMissing) {
                throw com.complex.platform.common.api.ApiException.badRequest(
                        "REVENUE_STREAM_ATTRIBUTION_MISSING",
                        "收入归属未配置:业态收入流「" + revenueStream + "」(范围:" + scope
                                + ")尚未在结算配置中心指定收款方,拒绝结算。请先配置归属并复核转正。");
            }
            log.warn("收入归属未配置(阻断开关关闭·按现状返空不阻断):revenue_stream={} scope={}", revenueStream, scope);
        }
        return hit;
    }

    /**
     * 配置(upsert)某(项目, 业态流)的收入归属主体。同键已有 active 行则覆盖 management_entity_id。
     * 幂等落点 = 唯一键 (COALESCE(project_id,'__DEFAULT__'), revenue_stream) WHERE status='active'。
     */
    public void upsert(RevenueStreamAttributionRecord record) {
        MapSqlParameterSource params = bindParams(record);
        jdbcTemplate.update("""
                INSERT INTO revenue_stream_attribution (
                    id, project_id, revenue_stream, management_entity_id, beneficiary_kind,
                    status, updated_by, updated_at
                )
                VALUES (
                    :id, :project_id, :revenue_stream, :management_entity_id, :beneficiary_kind,
                    'active', :updated_by, :updated_at
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), revenue_stream, is_draft) WHERE status = 'active'
                DO UPDATE SET
                    management_entity_id = EXCLUDED.management_entity_id,
                    beneficiary_kind     = EXCLUDED.beneficiary_kind,
                    updated_by           = EXCLUDED.updated_by,
                    updated_at           = EXCLUDED.updated_at
                """, params);
    }

    /** 写入前先校验流/收款方类型/主体列组合,避免落库时才被 DB CHECK 拒(错误码对业务人员不可读)。 */
    private MapSqlParameterSource bindParams(RevenueStreamAttributionRecord record) {
        if (!REVENUE_STREAMS.contains(record.revenueStream())) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "INVALID_REVENUE_STREAM", "未知业态收入流:" + record.revenueStream());
        }
        String kind = record.beneficiaryKind() == null ? "entity" : record.beneficiaryKind();
        if (!BENEFICIARY_KINDS.contains(kind)) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "INVALID_BENEFICIARY_KIND", "未知收款方类型:" + kind);
        }
        boolean hasEntity = record.managementEntityId() != null && !record.managementEntityId().isBlank();
        if ("entity".equals(kind) && !hasEntity) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "VALIDATION_FAILED", "收款方为「某个经营主体」时必须指定主体");
        }
        if ("per_merchant".equals(kind) && hasEntity) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "VALIDATION_FAILED", "收款方为「各商户自己」时不得再指定单一主体");
        }
        return new MapSqlParameterSource()
                .addValue("id", record.id())
                .addValue("project_id", record.projectId())
                .addValue("revenue_stream", record.revenueStream())
                .addValue("management_entity_id", hasEntity ? record.managementEntityId() : null)
                .addValue("beneficiary_kind", kind)
                .addValue("updated_by", record.updatedBy())
                .addValue("updated_at",
                        java.sql.Timestamp.from(record.updatedAt() == null ? Instant.now() : record.updatedAt()));
    }

    /**
     * V329(复核修订):落草案归属(is_draft=TRUE)走 <b>Path 2</b> —— INSERT 草案行 ON CONFLICT 到「同 scope 的草案槽」
     * ((scope, revenue_stream, is_draft) WHERE status='active'),<b>不动生效行</b>(is_draft=FALSE),编辑期归属不回落 null(保值)。
     * RETURNING id 返回真实落地行 id(草案槽命中保留首行 id),供审计 business_id 用。
     */
    public String upsertDraft(RevenueStreamAttributionRecord record) {
        MapSqlParameterSource params = bindParams(record);
        return jdbcTemplate.queryForObject("""
                INSERT INTO revenue_stream_attribution (
                    id, project_id, revenue_stream, management_entity_id, beneficiary_kind,
                    status, is_draft, updated_by, updated_at
                )
                VALUES (
                    :id, :project_id, :revenue_stream, :management_entity_id, :beneficiary_kind,
                    'active', TRUE, :updated_by, :updated_at
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), revenue_stream, is_draft) WHERE status = 'active'
                DO UPDATE SET
                    management_entity_id = EXCLUDED.management_entity_id,
                    beneficiary_kind     = EXCLUDED.beneficiary_kind,
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
                SELECT id, project_id, revenue_stream, management_entity_id, beneficiary_kind,
                       status, updated_by, updated_at
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
                        rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant(),
                        rs.getString("beneficiary_kind")));
    }
}
