package com.complex.platform.governance;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.dto.MerchantOperatingProjectRequest;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段第 20 条 20-R1 商户加经营项目（治理地图第 659 行「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目，
 * 要经过审批才生效。」）。申请进统一审批（第 528 行：发起人沿直接上级到其指定的最终审批人逐级审批；第 529 行：本人审批由统一审批拒绝），
 * 批准才写入 merchant_projects；驳回不写，可重新申请。范围闸在幂等之前：商户的经营项目里须有一个在本人该权限码可触及的范围内。
 */
@Service
public class MerchantOperatingProjectService {

    static final String ACTION = "merchant.project.add";
    static final String OBJECT = "merchant_project_request";
    static final String MANAGE = "merchant.profile.manage";
    static final String VIEW = "merchant.profile.view";
    private static final String ENTRY = "/api/v1/platform/merchants/{merchant_id}/operating-projects";
    private static final String SELECT = """
            SELECT r.*, p.name AS project_name FROM merchant_project_requests r JOIN projects p ON p.id = r.project_id
            """;

    public record ProjectOption(String projectId, String projectName) {}

    /** 加经营项目申请：申请人、理由、申请时间、复核单与结论（审批人、时间）。 */
    public record ProjectRequestRow(String id, String merchantId, String projectId, String projectName, String status, String reason,
            String requestedBy, Instant requestedAt, String reviewId, String decidedBy, Instant decidedAt) {}

    /** 商户的主项目、已生效的经营项目、申请记录与可申请的项目。 */
    public record OperatingProjects(String merchantId, String primaryProjectId, String primaryProjectName,
            List<ProjectOption> operatingProjects, List<ProjectRequestRow> requests, List<ProjectOption> projectOptions) {}

    private final JdbcTemplate jdbc;
    private final PlatformRepository platform;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final SupervisorReviewService reviews;
    private final AuditEventService audit;

    public MerchantOperatingProjectService(JdbcTemplate jdbc, PlatformRepository platform, AuthorizationService auth,
            IdempotencyService idempotency, SupervisorReviewService reviews, AuditEventService audit) {
        this.jdbc = jdbc;
        this.platform = platform;
        this.auth = auth;
        this.idempotency = idempotency;
        this.reviews = reviews;
        this.audit = audit;
    }

    /** 申请加经营项目：项目与理由必填，最终审批人可不指定（按直接上级）；已在经营或已有待审批的同项目申请 409。 */
    @Transactional
    public ProjectRequestRow request(String merchantId, MerchantOperatingProjectRequest r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            auth.requirePlatformScope();
            auth.requirePermission(MANAGE);
            // 范围闸在幂等之前（同一幂等键重放也要再过）
            MerchantRecord merchant = requireMerchantInReach(merchantId, MANAGE);
            if (merchant.archivedAt() != null || "approved".equals(merchant.deleteStatus())) {
                throw ApiException.badRequest("MERCHANT_ARCHIVED", "商户已存档或已删除，不能再加经营项目");
            }
            return idempotency.execute("merchantOperatingProjectRequest:" + merchant.id(), key, r, () -> {
                String projectId = requireText(r.projectId(), "project_id");
                if (!platform.projectExists(projectId)) {
                    throw ApiException.notFound("PROJECT_NOT_FOUND", "项目不存在");
                }
                String reason = requireText(r.reason(), "reason");
                if (platform.listMerchantOperatingProjectIds(merchant.id()).contains(projectId)) {
                    throw ApiException.conflict("MERCHANT_PROJECT_ALREADY_OPERATING", "该商户已在这个项目经营");
                }
                if (!jdbc.queryForList("SELECT id FROM merchant_project_requests WHERE merchant_id=? AND project_id=? AND status='pending_approval'",
                        String.class, merchant.id(), projectId).isEmpty()) {
                    throw ApiException.conflict("MERCHANT_PROJECT_REQUEST_PENDING", "该商户加这个经营项目的申请正在审批中");
                }
                String id = "mpr_" + UUID.randomUUID();
                var decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", reason, List.of(),
                        ACTION, OBJECT, id, null, "商户 " + merchant.merchantName() + " 加经营项目 " + projectName(projectId),
                        trimToNull(r.finalApproverId()), List.of()));
                jdbc.update("""
                        INSERT INTO merchant_project_requests(id, merchant_id, project_id, reason, requested_by, review_id, status)
                        VALUES (?,?,?,?,?,?,'pending_approval')
                        """, id, merchant.id(), projectId, reason, actor(), decision.reviewId());
                audit.recordSuccess("merchant.operating_project.requested", MANAGE, OBJECT, id, null, key,
                        Map.of("merchant_id", merchant.id(), "project_id", projectId, "review_id", decision.reviewId(), "reason", reason));
                return find(id);
            }, ProjectRequestRow.class);
        } catch (RuntimeException error) {
            try {
                audit.recordFailure("merchant.operating_project.request_failed", MANAGE, OBJECT, "new", null, key,
                        error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            } catch (RuntimeException auditError) {
                error.addSuppressed(auditError);
            }
            throw error;
        }
    }

    /** 审批结论回写：批准写入 merchant_projects 并记审批人与时间；驳回只记结论，可重新申请。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!ACTION.equals(d.actionType()) || !OBJECT.equals(d.objectType())
                || !List.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        var rows = jdbc.queryForList(
                "SELECT id, merchant_id, project_id FROM merchant_project_requests WHERE review_id=? AND status='pending_approval' FOR UPDATE",
                d.reviewId());
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.getFirst();
        String id = (String) row.get("id");
        boolean approved = "approved".equals(d.reviewStatus());
        if (approved) {
            jdbc.update("INSERT INTO merchant_projects(merchant_id, project_id) VALUES (?,?) ON CONFLICT DO NOTHING",
                    row.get("merchant_id"), row.get("project_id"));
        }
        String reviewer = actor();
        jdbc.update("UPDATE merchant_project_requests SET status=?, decided_by=?, decided_at=now() WHERE id=?", d.reviewStatus(), reviewer, id);
        audit.recordSuccess("merchant.operating_project." + d.reviewStatus(), MANAGE, OBJECT, id, null, null,
                Map.of("review_id", d.reviewId(), "merchant_id", row.get("merchant_id"), "project_id", row.get("project_id"),
                        "decided_by", reviewer));
    }

    /** 商户档案里的经营项目：主项目、已生效的经营项目、申请记录（新的在前）、可申请的项目（去掉已经营的）。 */
    public OperatingProjects view(String merchantId) {
        try {
            auth.requirePlatformScope();
            auth.requirePermission(VIEW);
            MerchantRecord merchant = requireMerchantInReach(merchantId, VIEW);
            String id = merchant.id();
            List<ProjectOption> operating = jdbc.query("""
                    SELECT p.id, p.name FROM merchant_projects mp JOIN projects p ON p.id = mp.project_id
                     WHERE mp.merchant_id=? ORDER BY p.name, p.id""", this::option, id);
            List<ProjectRequestRow> requests = jdbc.query(SELECT + " WHERE r.merchant_id=? ORDER BY r.requested_at DESC, r.id", this::map, id);
            List<ProjectOption> options = jdbc.query("""
                    SELECT p.id, p.name FROM projects p
                     WHERE p.id IS DISTINCT FROM ? AND NOT EXISTS (SELECT 1 FROM merchant_projects mp WHERE mp.merchant_id=? AND mp.project_id=p.id)
                     ORDER BY p.name, p.id""", this::option, merchant.projectId(), id);
            OperatingProjects result = new OperatingProjects(id, merchant.projectId(),
                    merchant.projectId() == null ? null : projectName(merchant.projectId()), operating, requests, options);
            audit.recordSuccess("merchant.viewed", VIEW, "merchant", id, null, null, Map.of("entry", ENTRY));
            return result;
        } catch (RuntimeException error) {
            audit.recordFailure("merchant.view_failed", VIEW, "merchant", merchantId, null, null,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            throw error;
        }
    }

    /** 商户须存在；平台管理员不限，其余须该商户的经营项目（主项目加已批准追加的）里任一在该权限码可触及的范围内。 */
    private MerchantRecord requireMerchantInReach(String merchantId, String code) {
        MerchantRecord merchant = platform.findMerchantById(requireText(merchantId, "merchant_id"))
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "商户不存在或已被删除"));
        AccessActor actor = auth.currentActor();
        if (!auth.isPlatformAdmin(actor)) {
            var reach = auth.projectReach(actor, code);
            if (platform.listMerchantOperatingProjectIds(merchant.id()).stream().noneMatch(reach::covers)) {
                throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "无权访问该项目的数据");
            }
        }
        return merchant;
    }

    private String projectName(String projectId) {
        return jdbc.queryForList("SELECT name FROM projects WHERE id=?", String.class, projectId).stream().findFirst().orElse(projectId);
    }

    private ProjectRequestRow find(String id) {
        return jdbc.query(SELECT + " WHERE r.id=?", this::map, id).getFirst();
    }

    private ProjectOption option(ResultSet rs, int n) throws SQLException {
        return new ProjectOption(rs.getString("id"), rs.getString("name"));
    }

    private ProjectRequestRow map(ResultSet rs, int n) throws SQLException {
        return new ProjectRequestRow(rs.getString("id"), rs.getString("merchant_id"), rs.getString("project_id"),
                rs.getString("project_name"), rs.getString("status"), rs.getString("reason"), rs.getString("requested_by"),
                nullableInstant(rs, "requested_at"), rs.getString("review_id"), rs.getString("decided_by"), nullableInstant(rs, "decided_at"));
    }

    private String actor() {
        return auth.currentActor().id();
    }
}
