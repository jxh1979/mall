package com.complex.platform.facility;

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
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.evidence.EvidenceEventRepository;
import com.complex.platform.facility.dto.FacilityEquipmentScrapRequests;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * HP-F03 楼宇设备报废（BSM「楼宇机电设备」L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理，记录批准人、处置方式及实际处置凭证；
 * 残值或损失由财务记录并关联原设备。标记报废不等于实物处置或款项完成」；批准 13｜Q19「报废状态、实际处置、款项分别记」）。
 * 报废单分别记三项事实：审批（统一审批模型批准后设备才置「报废」）、实物处置（凭证）、款项（残值应收／损失内部核算，收款走应收应付登记收付款）。
 */
@Service
public class FacilityEquipmentScrapService {

    static final String ACTION = "facility.equipment.scrap";
    static final String OBJECT = "facility_equipment_scrap";
    private static final String PERM = "facility.equipment.manage";
    private static final String FINANCE_PERM = "finance.ar_ap.manage";
    private static final String SELECT = """
            SELECT s.*, e.name AS equipment_name, a.unpaid_amount_cent AS residual_unpaid_amount_cent
              FROM facility_equipment_scraps s JOIN facility_equipment e ON e.id = s.equipment_id
              LEFT JOIN ar_ap_items a ON a.id = s.residual_ar_ap_item_id
            """;

    /** 报废单视图：approvalStatus/disposalStatus/moneyStatus 三项事实分开，completed＝实物已处置且款项已结清。 */
    public record Scrap(String id, String equipmentId, String equipmentName, String projectId, String reason,
            String disposalMethod, String reviewId, String approvalStatus, String requestedBy, Instant requestedAt,
            String approvedBy, Instant approvedAt, String disposalStatus, String disposalCompletedBy,
            Instant disposalCompletedAt, List<String> disposalEvidenceAttachmentIds, String disposalNote,
            Long residualAmountCent, String residualCounterpartyId, String residualArApItemId,
            Long residualUnpaidAmountCent, Long lossAmountCent, String lossArApItemId, String financeRecordedBy,
            Instant financeRecordedAt, String moneyStatus, boolean completed) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final SupervisorReviewService reviews;
    private final FinanceService finance;
    private final EvidenceEventRepository evidence;
    private final EvidenceAttachmentValidator files;
    private final AuditEventService audit;

    public FacilityEquipmentScrapService(JdbcTemplate jdbc, ObjectMapper mapper, AuthorizationService auth,
            IdempotencyService idempotency, SupervisorReviewService reviews, FinanceService finance,
            EvidenceEventRepository evidence, EvidenceAttachmentValidator files, AuditEventService audit) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.auth = auth;
        this.idempotency = idempotency;
        this.reviews = reviews;
        this.finance = finance;
        this.evidence = evidence;
        this.files = files;
        this.audit = audit;
    }

    /** 报废申请：记原因与处置方式，进统一审批（发起人沿直接上级到其指定的最终审批人）；设备状态此时不动。 */
    @Transactional
    public Scrap request(String equipmentId, FacilityEquipmentScrapRequests.Create r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            auth.requirePlatformScope();
            if (r == null) {
                throw invalid("请填写报废申请");
            }
            return idempotency.execute("facilityEquipmentScrap:" + equipmentId, key, r, () -> {
                Map<String, Object> e = jdbc.queryForList(
                        "SELECT name, status, project_id, merchant_id FROM facility_equipment WHERE id=? FOR UPDATE", equipmentId)
                        .stream().findFirst()
                        .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "设施设备不存在或已被删除"));
                String project = (String) e.get("project_id");
                requireProject(project, PERM);
                if ("retired".equals(e.get("status"))) {
                    throw ApiException.conflict("FACILITY_EQUIPMENT_RETIRED", "设备已报废");
                }
                if (jdbc.queryForObject("SELECT count(*) FROM facility_equipment_scraps WHERE equipment_id=? AND status='pending_approval'",
                        Integer.class, equipmentId) > 0) {
                    throw ApiException.conflict("FACILITY_EQUIPMENT_SCRAP_PENDING", "该设备已有待审批的报废申请");
                }
                String reason = requireText(r.reason(), "reason");
                String method = requireText(r.disposalMethod(), "disposal_method");
                String id = "fes_" + UUID.randomUUID();
                var decision = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", reason, List.of(),
                        ACTION, OBJECT, id, null, "楼宇设备 " + e.get("name") + " 报废，处置方式：" + method,
                        trimToNull(r.finalApproverId()), List.of()));
                jdbc.update("""
                        INSERT INTO facility_equipment_scraps(id, equipment_id, project_id, merchant_id, reason, disposal_method,
                            review_id, status, requested_by) VALUES (?,?,?,?,?,?,?,'pending_approval',?)
                        """, id, equipmentId, project, e.get("merchant_id"), reason, method, decision.reviewId(), actor());
                audit.recordSuccess("facility.equipment_scrap_requested", PERM, OBJECT, id, null, key,
                        Map.of("equipment_id", equipmentId, "review_id", decision.reviewId()));
                return find(id);
            }, Scrap.class);
        } catch (RuntimeException error) {
            recordFailure("facility.equipment_scrap_request_failed", PERM, equipmentId, key, error);
            throw error;
        }
    }

    /** 审批结论回写：批准才把设备置「报废」并记批准人（终审人）；驳回设备照常在用，可重新申请。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!ACTION.equals(d.actionType()) || !OBJECT.equals(d.objectType())
                || !List.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        var rows = jdbc.queryForList("SELECT id, equipment_id FROM facility_equipment_scraps WHERE review_id=? AND status='pending_approval' FOR UPDATE",
                d.reviewId());
        if (rows.isEmpty()) {
            return;
        }
        String id = (String) rows.getFirst().get("id");
        boolean approved = "approved".equals(d.reviewStatus());
        jdbc.update("UPDATE facility_equipment_scraps SET status=?, approved_by=?, approved_at=?, updated_at=now() WHERE id=?",
                d.reviewStatus(), approved ? actor() : null, approved ? java.sql.Timestamp.from(Instant.now()) : null, id);
        if (approved) {
            jdbc.update("""
                    UPDATE facility_equipment SET status='retired', alarm_source=NULL, alarm_note=NULL, alarm_raised_at=NULL, updated_at=now()
                     WHERE id=?""", rows.getFirst().get("equipment_id"));
        }
        audit.recordSuccess("facility.equipment_scrap_" + d.reviewStatus(), PERM, OBJECT, id, null, null,
                Map.of("review_id", d.reviewId()));
    }

    /** 实物处置单独登记：须已批准、附已上传且绑定本报废单的凭证；不碰款项。 */
    @Transactional
    public Scrap completeDisposal(String scrapId, FacilityEquipmentScrapRequests.Disposal r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            scoped(scrapId, PERM);
            if (r == null) {
                throw invalid("请填写实际处置登记");
            }
            return idempotency.execute("facilityEquipmentScrapDisposal:" + scrapId, key, r, () -> {
                Scrap s = locked(scrapId);
                if (!"approved".equals(s.approvalStatus()) || s.disposalCompletedAt() != null) {
                    throw ApiException.conflict("FACILITY_EQUIPMENT_SCRAP_STATUS_CONFLICT", "报废须先经批准，实际处置只登记一次");
                }
                List<String> ids = r.evidenceAttachmentIds() == null ? List.of() : r.evidenceAttachmentIds();
                if (ids.isEmpty()) {
                    throw ApiException.badRequest("EVIDENCE_REQUIRED", "实际处置须附处置凭证");
                }
                for (String attachmentId : ids) {
                    var attachment = evidence.findAttachmentById(attachmentId)
                            .filter(a -> OBJECT.equals(a.businessType()) && scrapId.equals(a.businessId()))
                            .orElseThrow(() -> ApiException.badRequest("EVIDENCE_BINDING_MISMATCH", "凭证不存在或未绑定本报废单"));
                    files.requireUploaded(attachment);
                }
                jdbc.update("""
                        UPDATE facility_equipment_scraps SET disposal_completed_by=?, disposal_completed_at=now(),
                            disposal_evidence_attachment_ids=?::jsonb, disposal_note=?, updated_at=now() WHERE id=?
                        """, actor(), json(ids), trimToNull(r.disposalNote()), scrapId);
                audit.recordSuccess("facility.equipment_scrap_disposal_completed", PERM, OBJECT, scrapId, null, key,
                        Map.of("evidence_count", ids.size()));
                return find(scrapId);
            }, Scrap.class);
        } catch (RuntimeException error) {
            recordFailure("facility.equipment_scrap_disposal_complete_failed", PERM, scrapId, key, error);
            throw error;
        }
    }

    /** 财务登记残值（对回收方应收）与损失（内部核算），均关联本报废单；每单一次，与实物处置先后无关。 */
    @Transactional
    public Scrap recordFinance(String scrapId, FacilityEquipmentScrapRequests.Finance r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            scoped(scrapId, FINANCE_PERM);
            if (r == null) {
                throw invalid("请填写残值或损失");
            }
            return idempotency.execute("facilityEquipmentScrapFinance:" + scrapId, key, r, () -> {
                Scrap s = locked(scrapId);
                if (!"approved".equals(s.approvalStatus())) {
                    throw ApiException.conflict("FACILITY_EQUIPMENT_SCRAP_STATUS_CONFLICT", "报废未批准，不能登记残值或损失");
                }
                if (s.financeRecordedAt() != null) {
                    throw ApiException.conflict("FACILITY_EQUIPMENT_SCRAP_FINANCE_RECORDED", "该报废单已做财务登记");
                }
                long residual = r.residualAmountCent() == null ? 0 : r.residualAmountCent();
                long loss = r.lossAmountCent() == null ? 0 : r.lossAmountCent();
                String counterparty = trimToNull(r.residualCounterpartyId());
                if (residual < 0 || loss < 0) {
                    throw invalid("残值与损失金额不能为负");
                }
                if (residual > 0 && counterparty == null) {
                    throw invalid("有残值须填写回收方");
                }
                String residualItem = residual > 0 ? finance.postFacilityEquipmentScrapResidual(scrapId, counterparty,
                        s.projectId(), residual, key + ":residual").id() : null;
                String lossItem = loss > 0 ? finance.postFacilityEquipmentScrapLoss(scrapId,
                        s.projectId() == null ? "platform" : s.projectId(), s.projectId(), loss, key + ":loss").id() : null;
                jdbc.update("""
                        UPDATE facility_equipment_scraps SET residual_amount_cent=?, residual_counterparty_id=?, residual_ar_ap_item_id=?,
                            loss_amount_cent=?, loss_ar_ap_item_id=?, finance_recorded_by=?, finance_recorded_at=now(), updated_at=now()
                         WHERE id=?""", residual, counterparty, residualItem, loss, lossItem, actor(), scrapId);
                audit.recordSuccess("facility.equipment_scrap_finance_recorded", FINANCE_PERM, OBJECT, scrapId, null, key,
                        Map.of("residual_amount_cent", residual, "loss_amount_cent", loss));
                return find(scrapId);
            }, Scrap.class);
        } catch (RuntimeException error) {
            recordFailure("facility.equipment_scrap_finance_record_failed", FINANCE_PERM, scrapId, key, error);
            throw error;
        }
    }

    /** 报废处置清单：设备管理或应收应付管理任一权限，平台口径，只列可达项目。 */
    public PageResponse<Scrap> list() {
        String perm = permissionHeld();
        auth.requirePlatformScope();
        AccessActor actor = auth.currentActor();
        List<Scrap> rows = jdbc.query(SELECT + " ORDER BY s.requested_at DESC, s.id", this::map).stream()
                .filter(s -> s.projectId() == null || auth.canReachProject(actor, s.projectId(), perm)).toList();
        audit.recordSuccess("facility.equipment_scraps_viewed", perm, OBJECT, "all", null, null, Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, 500, rows.size());
    }

    /** 证据附件定界：上传须设备管理权限；查看设备管理或应收应付管理任一权限；均须项目可达。 */
    public void requireEvidenceScope(String scrapId, String evidencePermission) {
        boolean reading = "evidence.timeline.view".equals(evidencePermission);
        Scrap s = scoped(scrapId, reading ? permissionHeld() : PERM);
        auth.requirePermission(evidencePermission);
        if (s.projectId() != null && !auth.canReachProject(auth.currentActor(), s.projectId(), evidencePermission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "报废单不在授权项目范围内");
        }
    }

    private String permissionHeld() {
        if (auth.currentActorHasPermission(PERM)) {
            return PERM;
        }
        auth.requirePermission(FINANCE_PERM);
        return FINANCE_PERM;
    }

    private Scrap scoped(String scrapId, String permission) {
        auth.requirePermission(permission);
        auth.requirePlatformScope();
        Scrap s = find(requireText(scrapId, "scrap_id"));
        requireProject(s.projectId(), permission);
        return s;
    }

    private void requireProject(String project, String permission) {
        if (project != null && !auth.canReachProject(auth.currentActor(), project, permission)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "设备不在授权项目范围内");
        }
    }

    private Scrap locked(String id) {
        jdbc.queryForObject("SELECT id FROM facility_equipment_scraps WHERE id=? FOR UPDATE", String.class, id);
        return find(id);
    }

    private Scrap find(String id) {
        return jdbc.query(SELECT + " WHERE s.id=?", this::map, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("FACILITY_EQUIPMENT_SCRAP_NOT_FOUND", "报废单不存在"));
    }

    private Scrap map(ResultSet rs, int n) throws SQLException {
        Instant disposedAt = nullableInstant(rs, "disposal_completed_at");
        Instant financeAt = nullableInstant(rs, "finance_recorded_at");
        Long unpaid = nullableLong(rs, "residual_unpaid_amount_cent");
        String disposal = disposedAt == null ? "pending" : "completed";
        String money = financeAt == null ? "pending_finance"
                : unpaid != null && unpaid > 0 ? "residual_pending_collection" : "settled";
        return new Scrap(rs.getString("id"), rs.getString("equipment_id"), rs.getString("equipment_name"),
                rs.getString("project_id"), rs.getString("reason"), rs.getString("disposal_method"),
                rs.getString("review_id"), rs.getString("status"), rs.getString("requested_by"),
                nullableInstant(rs, "requested_at"), rs.getString("approved_by"), nullableInstant(rs, "approved_at"),
                disposal, rs.getString("disposal_completed_by"), disposedAt,
                strings(rs.getString("disposal_evidence_attachment_ids")), rs.getString("disposal_note"),
                nullableLong(rs, "residual_amount_cent"), rs.getString("residual_counterparty_id"),
                rs.getString("residual_ar_ap_item_id"), unpaid, nullableLong(rs, "loss_amount_cent"),
                rs.getString("loss_ar_ap_item_id"), rs.getString("finance_recorded_by"), financeAt, money,
                "completed".equals(disposal) && "settled".equals(money));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String actor() {
        return auth.currentActor().id();
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> strings(String value) {
        try {
            return value == null ? List.of() : mapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }

    private void recordFailure(String event, String permission, String id, String key, RuntimeException error) {
        try {
            audit.recordFailure(event, permission, OBJECT, id, null, key,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
        } catch (RuntimeException auditError) {
            error.addSuppressed(auditError);
        }
    }
}
