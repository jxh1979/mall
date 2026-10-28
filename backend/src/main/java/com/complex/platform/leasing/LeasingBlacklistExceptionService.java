package com.complex.platform.leasing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.leasing.dto.LeasingBlacklistExceptionRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 30 条 招商黑名单例外放行（PGM L502「黑名单品牌拦截同品牌新线索，以及由拉黑前既有线索新建意向。确需继续的，
 * 须由具备单独授予的黑名单例外放行权限人员操作，按层级对应主管审批，申请人不得审批自己提交的申请，并记录操作人、审批人、理由、时间和关联线索或意向。」）。
 * 一单放行一件事：新建同品牌线索（lead_create），或由某条拉黑前既有线索新建意向（intent_create）。申请进统一审批（发起人沿直接上级到其指定的
 * 最终审批人，PGM L540；本人审批由统一审批拒绝）；批准后申请人本人在被拦时凭单放行一次，同一事务记已用与所建线索或意向并留痕。
 */
@Service
public class LeasingBlacklistExceptionService {

    static final String ACTION = "leasing.blacklist_exception";
    static final String OBJECT = "leasing_blacklist_exception";
    static final String PERM = "leasing.brand.blacklist_exception";
    static final String LEAD_CREATE = "lead_create";
    static final String INTENT_CREATE = "intent_create";
    private static final String SELECT = """
            SELECT x.*, b.brand_name FROM leasing_blacklist_exceptions x JOIN leasing_brands b ON b.id = x.brand_id
            """;

    /** 放行单视图：操作人（申请人）、审批人、理由、申请与批准时间、关联线索或意向。 */
    public record BlacklistException(String id, String projectId, String brandId, String brandName, String targetType,
            String leadId, String reason, String requestedBy, Instant requestedAt, String reviewId, String status,
            String approvedBy, Instant approvedAt, Instant usedAt, String usedLeadId, String usedIntentId) {}

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final SupervisorReviewService reviews;
    private final AuditEventService audit;

    public LeasingBlacklistExceptionService(JdbcTemplate jdbc, AuthorizationService auth, IdempotencyService idempotency,
            SupervisorReviewService reviews, AuditEventService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.idempotency = idempotency;
        this.reviews = reviews;
        this.audit = audit;
    }

    /** 申请例外放行：持单独授予的放行权限，写明理由，进统一审批；由线索新建意向的品牌取该线索的品牌。 */
    @Transactional
    public BlacklistException request(LeasingBlacklistExceptionRequest r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            auth.requirePlatformScope();
            auth.requirePermission(PERM);
            if (r == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写黑名单例外放行申请");
            }
            return idempotency.execute("leasingBlacklistException", key, r, () -> {
                String target = requireText(r.targetType(), "target_type");
                String leadId = null;
                String brandId;
                String project = null;
                String what;
                if (INTENT_CREATE.equals(target)) {
                    Map<String, Object> lead = jdbc.queryForList("SELECT id, brand_id, project_id FROM leasing_leads WHERE id=?",
                            requireText(r.leadId(), "lead_id")).stream().findFirst()
                            .orElseThrow(() -> ApiException.notFound("LEASING_LEAD_NOT_FOUND", "招商线索不存在或已被删除"));
                    leadId = (String) lead.get("id");
                    project = (String) lead.get("project_id");
                    AccessActor actor = auth.currentActor();
                    if (!auth.isPlatformAdmin(actor) && (project == null || !auth.canReachProject(actor, project, PERM))) {
                        throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "招商线索不在本权限可触及的项目范围内");
                    }
                    brandId = requireText((String) lead.get("brand_id"), "brand_id");
                    what = "由线索 " + leadId + " 新建意向";
                } else if (LEAD_CREATE.equals(target)) {
                    brandId = requireText(r.brandId(), "brand_id");
                    what = "新建线索";
                } else {
                    throw ApiException.badRequest("VALIDATION_FAILED", "target_type is invalid");
                }
                Map<String, Object> brand = jdbc.queryForList("SELECT brand_name, project_id FROM leasing_brands WHERE id=?", brandId)
                        .stream().findFirst()
                        .orElseThrow(() -> ApiException.notFound("LEASING_BRAND_NOT_FOUND", "品牌不存在或已被删除"));
                if (LEAD_CREATE.equals(target)) {
                    project = (String) brand.get("project_id");
                }
                String reason = requireText(r.reason(), "reason");
                String id = "lbe_" + UUID.randomUUID();
                var decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", reason, List.of(),
                        ACTION, OBJECT, id, null, "品牌 " + brand.get("brand_name") + " 黑名单例外放行：" + what,
                        trimToNull(r.finalApproverId()), List.of()));
                jdbc.update("""
                        INSERT INTO leasing_blacklist_exceptions(id, project_id, brand_id, target_type, lead_id, reason, requested_by,
                            review_id, status) VALUES (?,?,?,?,?,?,?,?,'pending_approval')
                        """, id, project, brandId, target, leadId, reason, actor(), decision.reviewId());
                audit.recordSuccess("leasing.blacklist_exception.requested", PERM, OBJECT, id, null, key,
                        Map.of("review_id", decision.reviewId(), "target_type", target, "brand_id", brandId,
                                "lead_id", leadId == null ? "" : leadId, "reason", reason));
                return find(id);
            }, BlacklistException.class);
        } catch (RuntimeException error) {
            try {
                audit.recordFailure("leasing.blacklist_exception.request_failed", PERM, OBJECT, "new", null, key,
                        error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            } catch (RuntimeException auditError) {
                error.addSuppressed(auditError);
            }
            throw error;
        }
    }

    /** 审批结论回写：批准记批准人（终审人）与时间；驳回不能再用，确需继续可重新申请。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!ACTION.equals(d.actionType()) || !OBJECT.equals(d.objectType())
                || !List.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        var rows = jdbc.queryForList(
                "SELECT id FROM leasing_blacklist_exceptions WHERE review_id=? AND status='pending_approval' FOR UPDATE", d.reviewId());
        if (rows.isEmpty()) {
            return;
        }
        String id = (String) rows.getFirst().get("id");
        boolean approved = "approved".equals(d.reviewStatus());
        String reviewer = actor();
        jdbc.update("UPDATE leasing_blacklist_exceptions SET status=?, approved_by=?, approved_at=?, updated_at=now() WHERE id=?",
                d.reviewStatus(), approved ? reviewer : null, approved ? java.sql.Timestamp.from(Instant.now()) : null, id);
        audit.recordSuccess("leasing.blacklist_exception." + d.reviewStatus(), PERM, OBJECT, id, null, null,
                Map.of("review_id", d.reviewId(), approved ? "approved_by" : "rejected_by", reviewer));
    }

    /**
     * 被拦时凭单放行（由 {@link LeasingService} 在新建线索／转意向的同一事务内调用）：须本人申请、已批准且未用、品牌与放行事项相符，
     * 由线索新建意向时线索也须相符；通过即记已用与所建线索或意向，并留痕操作人、审批人、理由、时间和关联线索或意向。
     */
    void consume(String exceptionId, String targetType, String brandId, String leadId, String intentId) {
        auth.requirePermission(PERM);
        Map<String, Object> row = jdbc.queryForList("SELECT * FROM leasing_blacklist_exceptions WHERE id=? FOR UPDATE", exceptionId)
                .stream().findFirst().orElse(null);
        if (row == null || !"approved".equals(row.get("status")) || !actor().equals(row.get("requested_by"))
                || !targetType.equals(row.get("target_type")) || !brandId.equals(row.get("brand_id"))
                || (INTENT_CREATE.equals(targetType) && !leadId.equals(row.get("lead_id")))) {
            throw ApiException.conflict("LEASING_BLACKLIST_EXCEPTION_INVALID",
                    "黑名单例外放行单不可用：须是本人申请、已批准且未使用、与本品牌及本线索相符的放行单");
        }
        jdbc.update("""
                UPDATE leasing_blacklist_exceptions SET status='used', used_at=now(), used_lead_id=?, used_intent_id=?, updated_at=now()
                 WHERE id=?""", leadId, intentId, exceptionId);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("exception_id", exceptionId);
        meta.put("review_id", row.get("review_id"));
        meta.put("target_type", targetType);
        meta.put("requested_by", row.get("requested_by"));
        meta.put("approved_by", row.get("approved_by"));
        meta.put("reason", row.get("reason"));
        meta.put("requested_at", String.valueOf(row.get("requested_at")));
        meta.put("approved_at", String.valueOf(row.get("approved_at")));
        meta.put("lead_id", leadId);
        if (intentId != null) {
            meta.put("intent_id", intentId);
        }
        audit.recordSuccess("leasing.blacklist_exception.used", PERM, OBJECT, exceptionId, null, null, meta);
    }

    /** 放行单清单：与线索清单同口径（项目员工只见本项目，平台管理员／全局平台员工全量）。 */
    public PageResponse<BlacklistException> list() {
        auth.requirePlatformScope();
        String scope = auth.currentProjectScopeQuietly().orElse(null);
        List<BlacklistException> rows = scope == null
                ? jdbc.query(SELECT + " ORDER BY x.requested_at DESC, x.id LIMIT 500", this::map)
                : jdbc.query(SELECT + " WHERE x.project_id=? ORDER BY x.requested_at DESC, x.id LIMIT 500", this::map, scope);
        audit.recordSuccess("leasing.blacklist_exception.viewed", "leasing.lead.manage", OBJECT, "all", null, null,
                Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, 500, rows.size());
    }

    private BlacklistException find(String id) {
        return jdbc.query(SELECT + " WHERE x.id=?", this::map, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("LEASING_BLACKLIST_EXCEPTION_NOT_FOUND", "黑名单例外放行单不存在"));
    }

    private BlacklistException map(ResultSet rs, int n) throws SQLException {
        return new BlacklistException(rs.getString("id"), rs.getString("project_id"), rs.getString("brand_id"),
                rs.getString("brand_name"), rs.getString("target_type"), rs.getString("lead_id"), rs.getString("reason"),
                rs.getString("requested_by"), nullableInstant(rs, "requested_at"), rs.getString("review_id"),
                rs.getString("status"), rs.getString("approved_by"), nullableInstant(rs, "approved_at"),
                nullableInstant(rs, "used_at"), rs.getString("used_lead_id"), rs.getString("used_intent_id"));
    }

    private String actor() {
        return auth.currentActor().id();
    }
}
