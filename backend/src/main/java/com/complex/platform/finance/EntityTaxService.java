package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.evidence.EvidenceAttachmentValidator;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 经营主体税费实际申报金额登记（第十八批 AC 段第 210 条 210-R2；第十八批裁决 42「财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、
 * 法务审核并由顶层平台公司老板最终确认后生效」）。依据 BSM:1760「平台只接收专业财税软件回传的数据，或保存经授权人员手工填写并通过审批的数据」
 * 「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整」。
 *
 * <p>登记即提交审批（不设草稿）；审批照 PayrollTaxService.submitReview：财务、税务、法务三条真实部门起点＋复核动作 finance.tax_rule.adjust
 * （tax_rule_adjustment_l3，终点最高权限确认；提交人自审由复核链拦），结果由 {@link #decided} 回写，审批通过才生效。分红扣税（210-R3）只取已生效行。</p>
 */
@Service
public class EntityTaxService {

    static final String VIEW = "finance.entity_tax.view";
    static final String MANAGE = "finance.entity_tax.manage";
    static final String ACTION = "finance.tax_rule.adjust";
    static final String TYPE = "entity_tax_declaration";
    public static final Map<String, String> TAX_LABELS = Map.of("vat", "增值税", "cultural_fee", "文化事业建设费",
            "surcharge", "附加税", "corporate_income", "企业所得税", "other", "其他税费");
    private static final Set<String> STATUSES = Set.of("pending_review", "approved", "rejected");
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final String SELECT = "SELECT d.*, e.entity_name FROM entity_tax_declarations d"
            + " LEFT JOIN business_entities e ON e.id = d.business_entity_id";

    private final JdbcTemplate jdbc;
    private final AccessRepository access;
    private final AuthorizationService auth;
    private final SupervisorReviewService reviews;
    private final AuditedOperationExecutor operations;
    private final EvidenceAttachmentValidator attachments;
    private final ObjectMapper json;

    public EntityTaxService(JdbcTemplate jdbc, AccessRepository access, AuthorizationService auth, SupervisorReviewService reviews,
            AuditedOperationExecutor operations, EvidenceAttachmentValidator attachments, ObjectMapper json) {
        this.jdbc = jdbc;
        this.access = access;
        this.auth = auth;
        this.reviews = reviews;
        this.operations = operations;
        this.attachments = attachments;
        this.json = json;
    }

    /** 列表：按经营主体、所属月（所属期覆盖该月）、税种、状态筛选，分页。平台口径（商户账号拒）。 */
    public PageResponse<EntityTaxDeclaration> list(String entityId, String month, String taxType, String status, int page, int pageSize) {
        return operations.readPageResult(AuditedOperation.of("entity_tax.listed", "entity_tax.list_failed", VIEW, TYPE), () -> {
            auth.requirePermission(VIEW);
            auth.requirePlatformScope();
            String entity = blank(entityId);
            String m = blank(month) == null ? null : requireMonth(month, "month");
            String tax = blank(taxType) == null ? null : requireTaxType(taxType);
            String st = blank(status);
            if (st != null && !STATUSES.contains(st)) throw invalid("无法识别的登记状态：" + st);
            String where = " WHERE (?::text IS NULL OR d.business_entity_id = ?)"
                    + " AND (?::text IS NULL OR (d.period_start_month <= ? AND d.period_end_month >= ?))"
                    + " AND (?::text IS NULL OR d.tax_type = ?) AND (?::text IS NULL OR d.status = ?)";
            Object[] args = {entity, entity, m, m, m, tax, tax, st, st};
            int size = Math.min(Math.max(pageSize, 1), 200);
            int safePage = Math.max(page, 1);
            long total = jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations d" + where, Long.class, args);
            List<Object> pageArgs = new ArrayList<>(Arrays.asList(args));
            pageArgs.add(size);
            pageArgs.add((safePage - 1) * size);
            var items = jdbc.query(SELECT + where + " ORDER BY d.period_end_month DESC, d.created_at DESC, d.id LIMIT ? OFFSET ?",
                    this::row, pageArgs.toArray());
            return new PageResponse<>(items, safePage, size, total);
        });
    }

    /** 登记并提交审批（一步）。同主体、同税种、同所属期已有待审批或已生效登记的 409，驳回后可重登。 */
    @Transactional
    public EntityTaxDeclaration declare(EntityTaxDeclaration.Request r) {
        auth.requirePermission(MANAGE);
        auth.requirePlatformScope();
        if (r == null) throw invalid("请求内容不能为空");
        return operations.executeWrite("declareEntityTax", r.idempotencyKey(), r, EntityTaxDeclaration.class,
                AuditedOperation.of("entity_tax.declared", "entity_tax.declare_failed", MANAGE, TYPE), () -> {
                    String entity = requireText(r.businessEntityId(), "business_entity_id");
                    String taxType = requireTaxType(requireText(r.taxType(), "tax_type"));
                    String start = requireMonth(r.periodStartMonth(), "period_start_month");
                    String end = requireMonth(r.periodEndMonth(), "period_end_month");
                    if (end.compareTo(start) < 0) throw invalid("所属期末月不能早于首月");
                    if (r.declaredAmountCent() == null || r.declaredAmountCent() < 0) throw invalid("实际申报金额须填写，且不能为负数");
                    String reason = requireText(r.reason(), "reason");
                    List<String> evidence = r.evidenceAttachmentIds() == null ? List.of() : List.copyOf(r.evidenceAttachmentIds());
                    attachments.requireExisting(evidence, "evidence_attachment_ids");
                    String entityName = jdbc.query("SELECT entity_name FROM business_entities WHERE id = ?", (rs, n) -> rs.getString(1), entity)
                            .stream().findFirst().orElseThrow(() -> ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "经营主体不存在：" + entity));
                    jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { },
                            "entity-tax:" + entity + ":" + taxType + ":" + start + ":" + end);
                    if (jdbc.queryForObject("SELECT count(*) FROM entity_tax_declarations WHERE business_entity_id = ? AND tax_type = ?"
                            + " AND period_start_month = ? AND period_end_month = ? AND status IN ('pending_review', 'approved')",
                            Integer.class, entity, taxType, start, end) > 0) {
                        throw ApiException.conflict("ENTITY_TAX_DECLARATION_DUPLICATE",
                                "该经营主体同一税种、同一所属期已有待审批或已生效的申报登记，驳回后才能重新登记");
                    }
                    String id = "etd_" + UUID.randomUUID().toString().replace("-", "");
                    long cent = r.declaredAmountCent();
                    String summary = entityName + " " + TAX_LABELS.get(taxType) + " 所属期 " + start + "～" + end + " 实际申报金额 "
                            + (cent / 100) + "." + String.format("%02d", cent % 100) + " 元（新登记，生效后计入该期分红应扣税费）";
                    var review = submitReview(r, id, reason, summary);
                    jdbc.update("INSERT INTO entity_tax_declarations(id, business_entity_id, tax_type, period_start_month, period_end_month,"
                                    + " declared_amount_cent, reason, evidence_attachment_ids, status, review_id, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, 'pending_review', ?, ?, clock_timestamp())",
                            id, entity, taxType, start, end, cent, reason, encode(evidence), review.reviewId(), auth.currentActor().id());
                    return find(id);
                }, saved -> ((EntityTaxDeclaration) saved).id(), Map.of());
    }

    /** 复核终态回写：只认本表待审批行、复核单号一致且复核表里确有该终态（伪造或重放的回调不改行）。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!ACTION.equals(d.actionType()) || !TYPE.equals(d.objectType()) || !Set.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        jdbc.update("UPDATE entity_tax_declarations SET status = ?, approved_at = CASE WHEN ?::text = 'approved' THEN clock_timestamp() END"
                        + " WHERE id = ? AND status = 'pending_review' AND review_id = ? AND EXISTS (SELECT 1 FROM supervisor_review_decisions"
                        + " WHERE review_id = ? AND action_type = ? AND object_type = ? AND object_id = ? AND review_status = ?)",
                d.reviewStatus(), d.reviewStatus(), d.objectId(), d.reviewId(), d.reviewId(), ACTION, TYPE, d.objectId(), d.reviewStatus());
    }

    /** 照 PayrollTaxService.submitReview：三条线须配真实部门起点（在用、具正式审批角色），再进 finance.tax_rule.adjust 复核链。 */
    private SupervisorReviewDecisionRecord submitReview(EntityTaxDeclaration.Request r, String id, String reason, String summary) {
        for (String code : List.of("finance_review", "tax_review", "legal_review")) {
            var entry = jdbc.query("SELECT entry_user_id FROM approval_lines WHERE line_code = ? AND status = 'active'", (rs, n) -> rs.getString(1), code);
            if (entry.isEmpty() || entry.getFirst() == null || entry.getFirst().isBlank()) {
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED", "须先配置财务、税务、法务真实部门审批起点：" + code);
            }
            var account = access.findUserById(entry.getFirst()).orElse(null);
            if (account == null || !"active".equals(account.status()) || !auth.holdsPermissionViaRole(account.id(), "governance.supervisor_review.approve")) {
                throw ApiException.conflict("TAX_DEPARTMENT_ROUTING_REQUIRED", "部门起点须为在用且具有正式审批角色的人员：" + code);
            }
        }
        var review = reviews.evaluate(new SupervisorReviewEvaluateRequest(r.idempotencyKey() + ":review", reason, r.evidenceAttachmentIds(),
                ACTION, TYPE, id, 0L, summary, null, r.parallelLines(), null));
        if (!"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("TAX_APPROVAL_REQUIRED", "税费事项必须进入财税法和最高确认审批");
        }
        return review;
    }

    private EntityTaxDeclaration find(String id) {
        return jdbc.query(SELECT + " WHERE d.id = ?", this::row, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("ENTITY_TAX_DECLARATION_NOT_FOUND", "税费申报登记不存在"));
    }

    private EntityTaxDeclaration row(ResultSet rs, int n) throws SQLException {
        Timestamp approved = rs.getTimestamp("approved_at");
        return new EntityTaxDeclaration(rs.getString("id"), rs.getString("business_entity_id"), rs.getString("entity_name"),
                rs.getString("tax_type"), rs.getString("period_start_month"), rs.getString("period_end_month"),
                rs.getLong("declared_amount_cent"), rs.getString("reason"), ids(rs.getString("evidence_attachment_ids")),
                rs.getString("status"), rs.getString("review_id"), rs.getString("created_by"),
                rs.getTimestamp("created_at").toInstant(), approved == null ? null : approved.toInstant());
    }

    private List<String> ids(String text) {
        try {
            return json.readValue(text, json.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            throw new IllegalStateException("申报登记附件清单无法解析", e);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("申报登记附件清单无法保存", e);
        }
    }

    private static String requireTaxType(String value) {
        String taxType = value.trim();
        if (!TAX_LABELS.containsKey(taxType)) throw invalid("无法识别的税种：" + taxType);
        return taxType;
    }

    private static String requireMonth(String value, String field) {
        String month = requireText(value, field);
        if (!MONTH.matcher(month).matches()) throw invalid("月份须为「年-月」格式（如 2026-01）：" + month);
        return month;
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException invalid(String message) {
        return ApiException.badRequest("VALIDATION_FAILED", message);
    }
}
