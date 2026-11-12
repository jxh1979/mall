package com.complex.platform.ticketing;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AJ 段 236-R3 跨域组合通票票种规则版本（第十八批裁决 52 ②、裁决 61）。
 *
 * <p>依据：ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754「每次发布时把凑整差额金额及各域分摊数额记入票种规则版本、
 * 发布后不得再改」；PGM:528「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点……任一节点驳回则整单
 * 退回起草人」；决策台账第 75 条 18C 对同一原话「项目之上的平台权限」的落法（审批权限＋能看全部项目、不得自批）。
 *
 * <p>现表即当前生效版，草稿版与待审版只存本表；每票种至多 1 份生效＋1 份未结。锁序统一为「复核单→票种行」：审批回写在 approve 锁复核单之后才锁票种行，
 * 其余写由调用方（TicketingService 的 @Transactional 写方法）先锁票种行。
 */
@Service
public class TicketTypeRuleVersionService {

    static final String ACTION_TYPE = "ticket.type.rule.publish";
    static final String OBJECT_TYPE = "ticket_type_rule_version";
    static final String APPROVE_PERMISSION = "ticket.product.publish.approve";
    private static final String MANAGE_PERMISSION = "ticket.product.manage";
    private static final TypeReference<List<TicketTypeRuleVersionView.Entitlement>> ENTITLEMENTS = new TypeReference<>() { };
    private static final TypeReference<List<TicketTypeRuleVersionView.DomainAllocation>> DOMAINS = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TicketingRepository repository;
    private final SupervisorReviewService reviews;
    private final AuditEventService audit;
    private final AuthorizationService auth;
    private final AccessRepository access;

    public TicketTypeRuleVersionService(JdbcTemplate jdbc, ObjectMapper json, TicketingRepository repository,
            SupervisorReviewService reviews, AuditEventService audit, AuthorizationService auth, AccessRepository access) {
        this.jdbc = jdbc;
        // 版本里的权益组成与各域分摊按接口同名蛇形键存 JSONB。
        this.json = json.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.repository = repository;
        this.reviews = reviews;
        this.audit = audit;
        this.auth = auth;
        this.access = access;
    }

    static ApiException pendingConflict() {
        return ApiException.conflict("TICKET_RULE_VERSION_PENDING", "这张票的规则已提交审批，须先撤回提交再改");
    }

    /** 锁票种行（调用方事务内持有到提交）：同一票种的改动、提交与审批回写串行。 */
    void lockTicketType(String ticketTypeId) {
        jdbc.query("SELECT 1 FROM ticket_types WHERE id = ? FOR UPDATE", rs -> { }, ticketTypeId);
    }

    Optional<TicketTypeRuleVersionView> findOpen(String ticketTypeId) {
        return jdbc.query("SELECT * FROM ticket_type_rule_versions WHERE ticket_type_id = ? AND status IN ('draft', 'pending_review')",
                this::map, ticketTypeId).stream().findFirst();
    }

    /** 该票种全部版本，新版在前。 */
    List<TicketTypeRuleVersionView> list(String ticketTypeId) {
        return jdbc.query("SELECT * FROM ticket_type_rule_versions WHERE ticket_type_id = ? ORDER BY version_no DESC", this::map, ticketTypeId);
    }

    /** 存进该票种唯一一份未结版本（草稿版）：调用方已持票种锁且未结版本不是待审；没有就建第 N+1 版，有就覆盖内容。 */
    TicketTypeRuleVersionView saveOpen(String ticketTypeId, TicketCatalogService.RuleSnapshot snapshot) {
        String id = writeOpen(ticketTypeId, snapshot, "draft", null);
        audit.recordSuccess("ticket_type.rule_version.draft_saved", MANAGE_PERMISSION, OBJECT_TYPE, id, null, null,
                Map.of("ticket_type_id", ticketTypeId));
        return find(id);
    }

    /**
     * 提交项目之上的平台权限审批：调用方已持票种锁、已跑全部发布闸。最终审批人须持跨域组合通票票种规则审批权限（复核策略 specific_role 把关）、
     * 能看全部项目，并在提交人的直接上级链上（审批链补全时把关）；提交人不得自批（复核时把关）。
     */
    TicketTypeRuleVersionView submit(TicketTypeRecord ticketType, TicketCatalogService.RuleSnapshot snapshot, String reason,
            String finalApproverId, String idempotencyKey) {
        String approver = trimToNull(finalApproverId);
        if (approver == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "跨域组合通票须指定最终审批人（项目之上的平台权限）");
        }
        requirePlatformApprover(approver);
        if (findOpen(ticketType.id()).filter(open -> "pending_review".equals(open.status())).isPresent()) {
            throw pendingConflict();
        }
        String id = writeOpen(ticketType.id(), snapshot, "pending_review", reason);
        TicketTypeRuleVersionView version = find(id);
        SupervisorReviewDecisionRecord review = reviews.evaluate(new SupervisorReviewEvaluateRequest(idempotencyKey + ":review", reason,
                List.of(), ACTION_TYPE, OBJECT_TYPE, id, 0L, summary(ticketType, version), approver, List.of()));
        if (!"pending_review".equals(review.reviewStatus())) {
            throw ApiException.conflict("TICKET_RULE_APPROVAL_REQUIRED", "票种规则须进入审批，不得直接生效");
        }
        jdbc.update("UPDATE ticket_type_rule_versions SET review_id = ? WHERE id = ?", review.reviewId(), id);
        audit.recordSuccess("ticket_type.rule_version.submitted", MANAGE_PERMISSION, OBJECT_TYPE, id, null, idempotencyKey,
                Map.of("ticket_type_id", ticketType.id(), "version_no", version.versionNo(), "review_id", review.reviewId()));
        return find(id);
    }

    /**
     * 撤回（236-R7，第十八批裁决 61）：锁序「复核单→票种行」——待审的先作废复核单（不发决定事件），再锁票种行重读；期间已获批或本无未结版本即 409。
     * 版本置已撤回、理由记撤回理由；现表不变、旧版照卖，再改由现表另起新草稿（版本号续增）。
     */
    TicketTypeRuleVersionView withdraw(String ticketTypeId, String reason) {
        String actor = auth.currentActor().id();
        findOpen(ticketTypeId).filter(open -> "pending_review".equals(open.status()))
                .ifPresent(open -> reviews.cancelPendingReview(open.reviewId(), actor, "票种规则提交已撤回"));
        lockTicketType(ticketTypeId);
        TicketTypeRuleVersionView open = findOpen(ticketTypeId).orElseThrow(() ->
                ApiException.conflict("TICKET_RULE_VERSION_NOT_OPEN", "这张票没有可撤回的草稿或待审批版本"));
        jdbc.update("UPDATE ticket_type_rule_versions SET status = 'withdrawn', reason = ?, updated_by = ?, updated_at = ? WHERE id = ?",
                reason, actor, Timestamp.from(Instant.now()), open.id());
        return find(open.id());
    }

    /**
     * 审批回写（与 approve 同一事务，复核单已锁）：通过即把原生效版置已被取代、本版生效，并把本版套回现表（三档价、折扣、上架、补插现表没有的绑定、
     * 最低赠酒杯数）——首次发布即上架，已上架的即换成新版；驳回则本版回草稿、保留复核单号，现表不动。不重跑发布闸（提交时已跑，待审期间改动一律 409）。
     */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord decision) {
        if (!OBJECT_TYPE.equals(decision.objectType()) || !ACTION_TYPE.equals(decision.actionType())
                || !Set.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        List<String> types = jdbc.queryForList("SELECT ticket_type_id FROM ticket_type_rule_versions WHERE id = ?", String.class,
                decision.objectId());
        if (types.isEmpty()) {
            return;
        }
        String ticketTypeId = types.getFirst();
        lockTicketType(ticketTypeId);
        TicketTypeRuleVersionView version = find(decision.objectId());
        if (!"pending_review".equals(version.status()) || !Objects.equals(version.reviewId(), decision.reviewId())) {
            return;
        }
        Instant now = Instant.now();
        Timestamp at = Timestamp.from(now);
        Map<String, Object> trail = Map.of("ticket_type_id", ticketTypeId, "version_no", version.versionNo(), "review_id", decision.reviewId());
        if ("rejected".equals(decision.reviewStatus())) {
            jdbc.update("UPDATE ticket_type_rule_versions SET status = 'draft', updated_at = ? WHERE id = ?", at, version.id());
            audit.recordSuccess("ticket_type.rule_version.returned", APPROVE_PERMISSION, OBJECT_TYPE, version.id(), null, null, trail);
            return;
        }
        // 部分唯一索引不可延迟：先把原生效版降为已被取代，再把本版升为生效。
        jdbc.update("UPDATE ticket_type_rule_versions SET status = 'superseded', updated_at = ? WHERE ticket_type_id = ? AND status = 'effective'",
                at, ticketTypeId);
        jdbc.update("UPDATE ticket_type_rule_versions SET status = 'effective', decided_at = ?, updated_at = ? WHERE id = ?", at, at, version.id());
        jdbc.update("UPDATE ticket_types SET weekday_price_cent = ?, weekend_price_cent = ?, holiday_price_cent = ?, combo_discount_rate = ?,"
                + " status = 'published', updated_at = ? WHERE id = ?", version.weekdayPriceCent(), version.weekendPriceCent(),
                version.holidayPriceCent(), version.comboDiscountRate(), at, ticketTypeId);
        Set<String> bound = repository.listTicketTypeEntitlements(ticketTypeId).stream()
                .map(TicketTypeEntitlementRecord::id).collect(Collectors.toSet());
        for (TicketTypeRuleVersionView.Entitlement e : version.entitlements()) {
            if (!bound.contains(e.bindingId())) {
                // 绑定时间用版本所记原值，出票顺序不变。
                repository.saveTicketTypeEntitlement(new TicketTypeEntitlementRecord(e.bindingId(), ticketTypeId, e.entitlementTemplateId(),
                        e.quantity(), e.totalTimes(), e.valueAmountCent(), e.effectiveRule(), e.numPeople(), e.componentPriceCent(),
                        e.costBearer(), e.createdAt(), now));
            }
        }
        if (version.minGiftCups() != null) {
            repository.saveGiftFloor(new TicketTypeGiftFloor(ticketTypeId, version.minGiftCups(), version.minGiftCupsLower(),
                    version.minGiftCupsUpper(), version.submittedBy(), now));
        }
        audit.recordSuccess("ticket_type.rule_version.approved", APPROVE_PERMISSION, OBJECT_TYPE, version.id(), null, null, trail);
    }

    /** 未结版本写入（插入或覆盖内容）；提交时另记提交人、时间、理由与票种所属项目（恰属一个项目才填）。 */
    private String writeOpen(String ticketTypeId, TicketCatalogService.RuleSnapshot s, String status, String reason) {
        boolean submitting = "pending_review".equals(status);
        List<String> projects = submitting ? repository.listTicketTypeProjectIds(ticketTypeId) : List.of();
        String id = findOpen(ticketTypeId).map(TicketTypeRuleVersionView::id)
                .orElseGet(() -> "ttrv_" + UUID.randomUUID().toString().replace("-", ""));
        String actor = auth.currentActor().id();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO ticket_type_rule_versions (id, ticket_type_id, project_id, version_no, status, weekday_price_cent, weekend_price_cent,
                    holiday_price_cent, combo_discount_rate, min_gift_cups, min_gift_cups_lower, min_gift_cups_upper, entitlements,
                    component_total_cent, computed_price_cent, rounding_diff_cent, domain_allocations, reason, created_by, updated_by,
                    submitted_by, created_at, updated_at, submitted_at)
                VALUES (?, ?, ?, (SELECT COALESCE(MAX(version_no), 0) + 1 FROM ticket_type_rule_versions WHERE ticket_type_id = ?), ?, ?, ?,
                    ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET project_id = EXCLUDED.project_id, status = EXCLUDED.status,
                    weekday_price_cent = EXCLUDED.weekday_price_cent, weekend_price_cent = EXCLUDED.weekend_price_cent,
                    holiday_price_cent = EXCLUDED.holiday_price_cent, combo_discount_rate = EXCLUDED.combo_discount_rate,
                    min_gift_cups = EXCLUDED.min_gift_cups, min_gift_cups_lower = EXCLUDED.min_gift_cups_lower,
                    min_gift_cups_upper = EXCLUDED.min_gift_cups_upper, entitlements = EXCLUDED.entitlements,
                    component_total_cent = EXCLUDED.component_total_cent, computed_price_cent = EXCLUDED.computed_price_cent,
                    rounding_diff_cent = EXCLUDED.rounding_diff_cent, domain_allocations = EXCLUDED.domain_allocations,
                    reason = EXCLUDED.reason, updated_by = EXCLUDED.updated_by, updated_at = EXCLUDED.updated_at,
                    submitted_by = EXCLUDED.submitted_by, submitted_at = EXCLUDED.submitted_at
                """,
                id, ticketTypeId, projects.size() == 1 ? projects.getFirst() : null, ticketTypeId, status,
                s.weekdayPriceCent(), s.weekendPriceCent(), s.holidayPriceCent(), s.comboDiscountRate(),
                s.minGiftCups(), s.minGiftCupsLower(), s.minGiftCupsUpper(), encode(s.entitlements()),
                s.componentTotalCent(), s.computedPriceCent(), s.roundingDiffCent(), encode(s.domainAllocations()), reason,
                actor, actor, submitting ? actor : null, now, now, submitting ? now : null);
        return id;
    }

    /** 能看全部项目（平台管理员，或经勾了「可以查看全部项目」的角色持有审批权限码）；对同一原话的既有落法见决策台账第 75 条 18C。 */
    private void requirePlatformApprover(String approverId) {
        AccessUser user = access.findUserById(approverId).filter(found -> "active".equals(found.status()))
                .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "最终审批人不存在或已停用"));
        AccessActor actor = new AccessActor(user.id(), user.username(), user.accountType(), user.organizationId(), user.merchantId(),
                user.status());
        boolean allProjects;
        try {
            allProjects = auth.allowsPlatformScope(actor, APPROVE_PERMISSION);
        } catch (ApiException noProjectScope) {
            allProjects = false; // 本人没有项目归属的账号谈不上能看全部项目
        }
        if (!allProjects) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "跨域组合通票的定价与域间结算规则须由项目之上的平台权限审批，最终审批人须能看全部项目");
        }
    }

    /** 复核单摘要（复核单只存这段作理由）：首行发布理由，其后写全本版内容，审批人在主管复核页据此办理。 */
    private static String summary(TicketTypeRecord ticketType, TicketTypeRuleVersionView v) {
        StringBuilder text = new StringBuilder("发布理由：").append(v.reason())
                .append("\n票种：").append(ticketType.ticketName()).append("，第 ").append(v.versionNo()).append(" 版")
                .append("\n平日价 ").append(yuan(v.weekdayPriceCent())).append("，周末价 ").append(yuan(v.weekendPriceCent()))
                .append("，节假日价 ").append(yuan(v.holidayPriceCent()))
                .append("\n各业态价格合计 ").append(yuan(v.componentTotalCent())).append(" × ")
                .append(v.comboDiscountRate().stripTrailingZeros().toPlainString()).append("% 折扣 ＝ 算出 ").append(yuan(v.computedPriceCent()))
                .append("，填的平日价 ").append(yuan(v.weekdayPriceCent())).append("，凑整差额 ").append(yuan(v.roundingDiffCent()));
        for (TicketTypeRuleVersionView.DomainAllocation d : v.domainAllocations()) {
            text.append("\n").append(d.domainName()).append("：业态价格合计 ").append(yuan(d.componentTotalCent()))
                    .append("，折扣分摊 ").append(yuan(d.discountShareCent())).append("，凑整分摊 ").append(yuan(d.roundingShareCent()))
                    .append("，所得 ").append(yuan(d.shareCent()));
        }
        for (TicketTypeRuleVersionView.Entitlement e : v.entitlements()) {
            text.append("\n权益：").append(e.templateName()).append(" · ").append(e.providerMerchantName()).append(" · ")
                    .append(e.quantity()).append(" 份 · 业态价格 ").append(yuan(e.componentPriceCent()));
            if (e.costBearer() != null) {
                text.append(" · ").append(TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED.equals(e.costBearer()) ? "平台出资" : "商户让利");
            }
        }
        return text.append("\n最低赠酒杯数：").append(v.minGiftCups() == null ? "出厂 1 杯"
                : v.minGiftCups() + " 杯（下限 " + v.minGiftCupsLower() + "，上限 " + v.minGiftCupsUpper() + "）").toString();
    }

    private static String yuan(Long cent) {
        return cent == null ? "未填" : BigDecimal.valueOf(cent).movePointLeft(2).toPlainString() + " 元";
    }

    private TicketTypeRuleVersionView find(String id) {
        return jdbc.query("SELECT * FROM ticket_type_rule_versions WHERE id = ?", this::map, id).getFirst();
    }

    private TicketTypeRuleVersionView map(ResultSet rs, int row) throws SQLException {
        try {
            return new TicketTypeRuleVersionView(rs.getString("id"), rs.getString("ticket_type_id"), rs.getString("project_id"),
                    rs.getInt("version_no"), rs.getString("status"), rs.getObject("weekday_price_cent", Long.class),
                    rs.getObject("weekend_price_cent", Long.class), rs.getObject("holiday_price_cent", Long.class),
                    rs.getBigDecimal("combo_discount_rate"), rs.getObject("min_gift_cups", Integer.class),
                    rs.getObject("min_gift_cups_lower", Integer.class), rs.getObject("min_gift_cups_upper", Integer.class),
                    json.readValue(rs.getString("entitlements"), ENTITLEMENTS), rs.getLong("component_total_cent"),
                    rs.getLong("computed_price_cent"), rs.getObject("rounding_diff_cent", Long.class),
                    json.readValue(rs.getString("domain_allocations"), DOMAINS), rs.getString("reason"), rs.getString("review_id"),
                    rs.getString("created_by"), rs.getString("updated_by"), rs.getString("submitted_by"), instant(rs, "created_at"),
                    instant(rs, "updated_at"), instant(rs, "submitted_at"), instant(rs, "decided_at"), null);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("票种规则版本内容无法读取", e);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
