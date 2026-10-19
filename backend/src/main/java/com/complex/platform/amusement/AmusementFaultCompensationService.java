package com.complex.platform.amusement;

// 文件版本：V0.03

import static com.complex.platform.amusement.AmusementParkWideFaultService.roleLabel;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.amusement.dto.CompensationBatchConfirmRequest;
import com.complex.platform.amusement.dto.CompensationBatchInitiateRequest;
import com.complex.platform.amusement.dto.CompensationPolicyRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ticketing.EntitlementTemplateRecord;
import com.complex.platform.ticketing.IssuedEntitlementRecord;
import com.complex.platform.ticketing.TicketTypeEntitlementRecord;
import com.complex.platform.ticketing.TicketingRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-08b 全园故障补偿通票（ENUM「全园故障与补偿通票」）：
 * 发起＝按全园故障发生时间快照受影响人份/次份（已入场核销且本场次受影响 → 另补一次完整票面权益；尚未核销但仍在有效期内 → 只延长原核销权）；
 * 另一岗位、另一人确认＝实际发放。补偿不重新售票、仍在原票下办理；同一故障同一人份只补偿一次；全程留痕。
 * TK-18（2026-10-18 操作员裁决补充第 3 条）：场次提前结束时已入场（已扣次）的客人比照同一规则——结束事务生成 session_early_end
 * 批次（{@link #createSessionEndBatch}），发放前仍经 {@link #confirmBatch} 同一入口由两岗位先后确认；未入场的票不补偿。
 */
@Service
public class AmusementFaultCompensationService {

    static final String PERMISSION_POLICY_MANAGE = "amusement.compensation_policy.manage";
    static final String STATUS_PENDING = "pending_confirmation";
    static final String STATUS_ISSUED = "issued";
    private static final String TARGET_TYPE = "amusement_fault_compensation_batch";
    /** 场次提前结束补偿的费用承担方：游乐公司（V354 主体主数据 business_entities.id）。 */
    static final String BEARER_AMUSEMENT_COMPANY = "be_amusement";

    private final JdbcAmusementFaultCompensationRepository repository;
    private final JdbcAmusementParkWideFaultRepository faultRepository;
    private final TicketingRepository ticketingRepository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;
    private final AmusementParkWideFaultService faultService;

    public AmusementFaultCompensationService(JdbcAmusementFaultCompensationRepository repository,
            JdbcAmusementParkWideFaultRepository faultRepository, TicketingRepository ticketingRepository,
            AuthorizationService authorizationService, AuditEventService auditEventService,
            IdempotencyService idempotencyService, AmusementParkWideFaultService faultService) {
        this.repository = repository;
        this.faultRepository = faultRepository;
        this.ticketingRepository = ticketingRepository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
        this.faultService = faultService;
    }

    // ---------- 补偿有效期参数 ----------

    /** 该项目生效的补偿有效期参数：项目行 → 出厂回落行；均无则 fail-closed。 */
    public AmusementCompensationPolicyRecord resolvePolicy(String projectId) {
        return repository.findPolicy(projectId)
                .or(() -> repository.findPolicy(AmusementCompensationPolicyRecord.GLOBAL_SCOPE))
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "补偿有效期参数未配置，不得发放补偿"));
    }

    public AmusementCompensationPolicyRecord getPolicy(String projectId) {
        authorizationService.requirePermission(AmusementParkWideFaultService.PERMISSION_VIEW);
        return resolvePolicy(requireProject(projectId));
    }

    @Transactional
    public AmusementCompensationPolicyRecord configurePolicy(String projectId, CompensationPolicyRequest request) {
        try {
            authorizationService.requirePermission(PERMISSION_POLICY_MANAGE);
            String project = requireProject(projectId);
            if (request == null || request.validityDays() == null || request.minDays() == null || request.maxDays() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "补偿有效期须同时填写天数、下限与上限");
            }
            int value = request.validityDays();
            int min = request.minDays();
            int max = request.maxDays();
            if (min < 1 || min > value || value > max || max > 3650) {
                throw ApiException.badRequest("VALIDATION_FAILED", "补偿有效期须满足：1 ≤ 下限 ≤ 天数 ≤ 上限 ≤ 3650");
            }
            return idempotencyService.execute("configureCompensationPolicy", request.idempotencyKey(), request, () -> {
                AccessActor actor = authorizationService.currentActor();
                AmusementCompensationPolicyRecord saved = repository.upsertPolicy(new AmusementCompensationPolicyRecord(
                        project, value, min, max, actor.id(), Instant.now()));
                auditEventService.recordSuccess("amusement.fault_compensation.policy_configured", PERMISSION_POLICY_MANAGE,
                        "amusement_compensation_policy", project, null, request.idempotencyKey(),
                        Map.of("validity_days", value, "min_days", min, "max_days", max));
                return saved;
            }, AmusementCompensationPolicyRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.fault_compensation.policy_config_failed", PERMISSION_POLICY_MANAGE,
                    "amusement_compensation_policy", projectId, null, request == null ? null : request.idempotencyKey(),
                    codeOf(exception));
            throw exception;
        }
    }

    // ---------- 批次 ----------

    /** 发起（第一岗位）：快照受影响人份/次份为待确认批次；同故障已有待确认批次或无可补偿人份则拒绝。 */
    @Transactional
    public AmusementFaultCompensationBatchRecord initiateBatch(String faultId, CompensationBatchInitiateRequest request) {
        String permission = request == null ? AmusementParkWideFaultService.PERMISSION_VIEW
                : AmusementParkWideFaultService.permissionFor(trimToNull(request.role()));
        try {
            String role = faultService.requireRoleWithPermission(request == null ? null : request.role());
            String reason = requireText(request.reason(), "必须填写发放原因");
            String bearer = requireText(request.bearerCompanyId(), "必须指明决定发放补偿的域主管公司的具体法律主体");
            // FS-012-R4（ENUM L148「发放记录须明确该公司的具体法律主体」）：承担主体须为主体主数据中在用的法律主体。
            if (!repository.isActiveBusinessEntity(bearer)) {
                throw ApiException.badRequest("COMPENSATION_BEARER_ENTITY_INVALID", "承担主体须为主体主数据中在用的法律主体");
            }
            return idempotencyService.execute("initiateFaultCompensationBatch", request.idempotencyKey(), request, () -> {
                AmusementParkWideFaultRecord fault = requireFault(faultId);
                if (fault.activatedAt() == null) {
                    throw ApiException.conflict("PARK_WIDE_FAULT_NOT_ACTIVATED", "该全园故障状态尚未生效，不构成补偿事件");
                }
                repository.findPendingBatch(fault.id()).ifPresent(open -> {
                    throw ApiException.conflict("COMPENSATION_BATCH_ALREADY_PENDING", "该故障已有待确认的补偿批次，请先确认或等待处理");
                });
                AccessActor actor = authorizationService.currentActor();
                Instant now = Instant.now();
                Instant windowFrom = fault.activatedAt();
                Instant windowTo = fault.liftedAt() != null ? fault.liftedAt() : now;
                AmusementCompensationPolicyRecord policy = resolvePolicy(fault.projectId());
                String batchId = "afcb_" + UUID.randomUUID().toString().replace("-", "");
                List<AmusementFaultCompensationItemRecord> items = snapshotAffectedShares(batchId, fault, windowFrom, windowTo, now);
                if (items.isEmpty()) {
                    throw ApiException.conflict("COMPENSATION_NO_ELIGIBLE_SHARES",
                            "该故障下没有可补偿的人份或次份（已补偿、不在有效期内或无入场核销）");
                }
                int extend = (int) items.stream().filter(i -> AmusementFaultCompensationItemRecord.KIND_EXTEND.equals(i.kind())).count();
                AmusementFaultCompensationBatchRecord batch = repository.insertBatch(new AmusementFaultCompensationBatchRecord(
                        batchId, fault.id(), fault.projectId(), STATUS_PENDING, reason, bearer, policy.validityDays(),
                        windowFrom, windowTo, null, actor.id(), role, now, null, null, null,
                        items.size(), extend, items.size() - extend, now, now,
                        AmusementFaultCompensationBatchRecord.EVENT_PARK_WIDE_FAULT, null, null));
                items.forEach(repository::insertItem);
                auditEventService.recordSuccess("amusement.fault_compensation.batch_initiated", permission, TARGET_TYPE, batchId,
                        null, request.idempotencyKey(), Map.of("fault_id", fault.id(), "role", role, "items", items.size(),
                                "extend", extend, "full_reissue", items.size() - extend, "bearer_company_id", bearer));
                return batch;
            }, AmusementFaultCompensationBatchRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.fault_compensation.batch_initiate_failed", permission, TARGET_TYPE, faultId,
                    null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 第二岗位确认并发放：须与发起人不同人、另一岗位；延长核销权 / 另补完整票面权益，有效期自发放之日起算。 */
    @Transactional
    public AmusementFaultCompensationBatchRecord confirmBatch(String batchId, CompensationBatchConfirmRequest request) {
        String permission = request == null ? AmusementParkWideFaultService.PERMISSION_VIEW
                : AmusementParkWideFaultService.permissionFor(trimToNull(request.role()));
        try {
            String role = faultService.requireRoleWithPermission(request == null ? null : request.role());
            return idempotencyService.execute("confirmFaultCompensationBatch", request.idempotencyKey(), request, () -> {
                AmusementFaultCompensationBatchRecord batch = repository.findBatchById(batchId)
                        .orElseThrow(() -> ApiException.notFound("COMPENSATION_BATCH_NOT_FOUND", "补偿批次不存在"));
                requireProjectInScope(batch.projectId());
                if (!STATUS_PENDING.equals(batch.status())) {
                    throw ApiException.conflict("COMPENSATION_BATCH_NOT_PENDING", "该补偿批次不在待确认阶段");
                }
                AccessActor actor = authorizationService.currentActor();
                Instant now = Instant.now();
                if (batch.initiatedRole() == null) {
                    // TK-18 场次提前结束批次由系统生成、无发起岗位：第一次岗位确认记为发起方，仍待另一岗位、另一人确认后才发放。
                    AmusementFaultCompensationBatchRecord first = repository.updateBatch(batch.withFirstConfirmation(actor.id(), role, now));
                    auditEventService.recordSuccess("amusement.fault_compensation.batch_initiated", permission, TARGET_TYPE, batchId, null,
                            request.idempotencyKey(), details(first, Map.of("role", role, "items", first.itemCount())));
                    return first;
                }
                AmusementParkWideFaultService.requireSecondPerson(batch.initiatedBy(), batch.initiatedRole(), actor, role);
                // 截断到毫秒：与库内 timestamptz（微秒）往返一致，便于对账比对。
                Instant validUntil = now.plus(batch.validityDays(), ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
                int[] counts = issueItems(batch, validUntil, now);
                if (batch.endingId() != null) {
                    repository.completeSessionEndFollowups(batch.id(), actor.id(), now);
                }
                AmusementFaultCompensationBatchRecord issued = repository.updateBatch(
                        batch.withIssued(actor.id(), role, now, validUntil, counts[0], counts[1]));
                auditEventService.recordSuccess("amusement.fault_compensation.batch_issued", permission, TARGET_TYPE, batchId, null,
                        request.idempotencyKey(), details(issued, Map.of("role", role, "initiated_by", batch.initiatedBy(),
                                "initiated_role", batch.initiatedRole(), "extend", counts[0], "full_reissue", counts[1],
                                "valid_until", validUntil.toString())));
                return issued;
            }, AmusementFaultCompensationBatchRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("amusement.fault_compensation.batch_issue_failed", permission, TARGET_TYPE, batchId,
                    null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    public PageResponse<AmusementFaultCompensationBatchRecord> listBatches(String faultId) {
        authorizationService.requirePermission(AmusementParkWideFaultService.PERMISSION_VIEW);
        AmusementParkWideFaultRecord fault = requireFault(faultId);
        List<AmusementFaultCompensationBatchRecord> items = repository.listBatches(fault.id());
        auditEventService.recordSuccess("amusement.fault_compensation.viewed", AmusementParkWideFaultService.PERMISSION_VIEW,
                TARGET_TYPE, fault.id(), null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, Math.max(1, items.size()), items.size());
    }

    /** TK-18：按事件类型列出批次（场次提前结束批次无全园故障，不能按故障查）；有项目范围的账号只见本综合体。 */
    public PageResponse<AmusementFaultCompensationBatchRecord> listBatchesByEvent(String eventType, String projectId) {
        authorizationService.requirePermission(AmusementParkWideFaultService.PERMISSION_VIEW);
        String scope = authorizationService.currentProjectScopeQuietly().orElse(null);
        List<AmusementFaultCompensationBatchRecord> items = repository.listBatchesByEvent(trimToNull(eventType),
                scope != null ? scope : trimToNull(projectId));
        auditEventService.recordSuccess("amusement.fault_compensation.viewed", AmusementParkWideFaultService.PERMISSION_VIEW,
                TARGET_TYPE, "list", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, Math.max(1, items.size()), items.size());
    }

    /**
     * TK-18（2026-10-18 操作员裁决补充第 3 条）：场次提前结束时仍在场（已入场核销、已扣次）的客人比照 ENUM L148 全园故障补偿规则——
     * 在结束事务内为每个已入场人份生成 full_reissue 补偿待发放记录（原票、原场次、原入场核销；费用游乐公司承担；补偿有效期按项目参数），
     * 发放前仍须游乐公司负责人与工程或安全岗位经 {@link #confirmBatch} 同一入口先后确认。未入场的票不进（可再次使用）。
     * 无可补偿人份返回 null。须在入场记录被标记 released_at 之前调用。
     */
    public AmusementFaultCompensationBatchRecord createSessionEndBatch(String endingId, String sessionId, String complexId,
            String reason, Instant windowFrom, Instant windowTo, Instant now) {
        List<Map<String, Object>> rows = repository.listSessionEndAffectedShares(endingId, sessionId);
        if (rows.isEmpty()) {
            return null;
        }
        String batchId = "afcb_" + UUID.randomUUID().toString().replace("-", "");
        AmusementCompensationPolicyRecord policy = resolvePolicy(complexId);
        AmusementFaultCompensationBatchRecord batch = repository.insertBatch(new AmusementFaultCompensationBatchRecord(
                batchId, null, complexId, STATUS_PENDING, reason, BEARER_AMUSEMENT_COMPANY, policy.validityDays(),
                windowFrom, windowTo, null, null, null, null, null, null, null, rows.size(), 0, rows.size(), now, now,
                AmusementFaultCompensationBatchRecord.EVENT_SESSION_EARLY_END, endingId, sessionId));
        for (Map<String, Object> row : rows) {
            repository.insertItem(item(batchId, null, endingId, complexId, row, AmusementFaultCompensationItemRecord.KIND_FULL_REISSUE,
                    sessionId, (String) row.get("entry_verification_id"), now));
        }
        auditEventService.recordSuccess("amusement.fault_compensation.batch_initiated", AmusementSessionLifecycleService.END,
                TARGET_TYPE, batchId, null, null, details(batch, Map.of("items", rows.size(), "bearer_company_id", BEARER_AMUSEMENT_COMPANY)));
        return batch;
    }

    public PageResponse<AmusementFaultCompensationItemRecord> listItems(String batchId) {
        authorizationService.requirePermission(AmusementParkWideFaultService.PERMISSION_VIEW);
        AmusementFaultCompensationBatchRecord batch = repository.findBatchById(batchId)
                .orElseThrow(() -> ApiException.notFound("COMPENSATION_BATCH_NOT_FOUND", "补偿批次不存在"));
        requireProjectInScope(batch.projectId());
        List<AmusementFaultCompensationItemRecord> items = repository.listItems(batch.id());
        return new PageResponse<>(items, 1, Math.max(1, items.size()), items.size());
    }

    // ---------- 快照与发放 ----------

    /**
     * 受影响人份/次份快照（以全园故障发生时间与原票核销状态、有效期判定）：
     * ① 故障发生时已完成入场核销、且本场次履约受影响 → full_reissue（另补一次完整票面权益），记原场次与原入场核销；
     * ② 故障发生时尚未核销、但游乐权益仍在有效期内 → extend（只延长原核销权，不增加次数；多次票只补受影响的 1 次份）。
     * 排除补偿补发行与同故障已补偿的人份（同一故障同一人份只补偿一次）。
     */
    List<AmusementFaultCompensationItemRecord> snapshotAffectedShares(String batchId, AmusementParkWideFaultRecord fault,
            Instant windowFrom, Instant windowTo, Instant now) {
        List<AmusementFaultCompensationItemRecord> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : repository.listVerifiedAffectedShares(fault.id(), fault.projectId(), windowFrom, windowTo)) {
            String entitlementId = (String) row.get("entitlement_id");
            if (!seen.add(entitlementId)) {
                continue;
            }
            items.add(item(batchId, fault.id(), null, fault.projectId(), row, AmusementFaultCompensationItemRecord.KIND_FULL_REISSUE,
                    (String) row.get("session_id"), (String) row.get("entry_verification_id"), now));
        }
        for (Map<String, Object> row : repository.listUnverifiedAffectedShares(fault.id(), fault.projectId(), windowFrom)) {
            String entitlementId = (String) row.get("entitlement_id");
            if (!seen.add(entitlementId)) {
                continue;
            }
            items.add(item(batchId, fault.id(), null, fault.projectId(), row, AmusementFaultCompensationItemRecord.KIND_EXTEND, null, null, now));
        }
        return items;
    }

    private AmusementFaultCompensationItemRecord item(String batchId, String faultId, String endingId, String projectId,
            Map<String, Object> row, String kind, String sessionId, String entryVerificationId, Instant now) {
        Object slot = row.get("slot_index");
        Object validTo = row.get("valid_to");
        return new AmusementFaultCompensationItemRecord("afci_" + UUID.randomUUID().toString().replace("-", ""), batchId,
                faultId, projectId, (String) row.get("entitlement_id"), (String) row.get("order_id"),
                (String) row.get("ticket_type_id"), (String) row.get("parent_ticket_id"),
                slot == null ? null : ((Number) slot).intValue(), (String) row.get("customer_id"), kind, 1, sessionId,
                entryVerificationId, validTo == null ? null : ((java.sql.Timestamp) validTo).toInstant(), "pending", null,
                List.of(), null, null, null, now, now, endingId);
    }

    /** 审计明细：事件类型 + 全园故障编号或结束原件/场次编号（场次提前结束批次无 fault_id，不能进 Map.of）。 */
    private static Map<String, Object> details(AmusementFaultCompensationBatchRecord batch, Map<String, Object> extra) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("event_type", batch.eventType());
        if (batch.faultId() != null) {
            details.put("fault_id", batch.faultId());
        }
        if (batch.endingId() != null) {
            details.put("ending_id", batch.endingId());
            details.put("session_id", batch.sessionId());
        }
        details.putAll(extra);
        return details;
    }

    /**
     * 实际发放（第二岗位确认后）：extend → 原权益 valid_to 延长到补偿有效期截止（取较晚者），次数不变；
     * full_reissue → 原票下按票种完整票面（各权益模板各一次，含酒水、礼品）补发权益行，标记补偿记录，有效期自发放之日起算。
     * 返回 {extend 数, full_reissue 数}。
     */
    int[] issueItems(AmusementFaultCompensationBatchRecord batch, Instant validUntil, Instant now) {
        int extend = 0;
        int reissue = 0;
        for (AmusementFaultCompensationItemRecord item : repository.listItems(batch.id())) {
            if (!"pending".equals(item.status())) {
                continue;
            }
            if (AmusementFaultCompensationItemRecord.KIND_EXTEND.equals(item.kind())) {
                ticketingRepository.extendIssuedEntitlementValidTo(item.entitlementId(), validUntil, now);
                Instant newValidTo = item.originalValidTo() != null && item.originalValidTo().isAfter(validUntil)
                        ? item.originalValidTo() : validUntil;
                repository.markItemIssued(item.id(), newValidTo, List.of(), now);
                extend++;
            } else {
                List<String> ids = reissueFullTicketFace(item, validUntil, now);
                repository.markItemIssued(item.id(), validUntil, ids, now);
                reissue++;
            }
        }
        return new int[] {extend, reissue};
    }

    private List<String> reissueFullTicketFace(AmusementFaultCompensationItemRecord item, Instant validUntil, Instant now) {
        IssuedEntitlementRecord original = ticketingRepository.findIssuedEntitlementById(item.entitlementId())
                .orElseThrow(() -> ApiException.notFound("ENTITLEMENT_NOT_FOUND", "原票权益不存在：" + item.entitlementId()));
        List<TicketTypeEntitlementRecord> bindings = ticketingRepository.listTicketTypeEntitlements(original.ticketTypeId());
        List<String> ids = new ArrayList<>();
        if (bindings.isEmpty()) {
            // 票种无绑定（直发/历史手造）：至少补发原权益本身一次。
            ids.add(issueCompensationEntitlement(original, original.entitlementTemplateId(), original.entitlementType(),
                    original.merchantId(), original.venueId(), original.pointId(), original.valueAmountCent(),
                    original.entitlementKind(), original.agreementPriceCent(), original.consumptionWindow(),
                    original.exclusiveMerchantId(), item.id(), validUntil, now));
            return ids;
        }
        for (TicketTypeEntitlementRecord binding : bindings) {
            EntitlementTemplateRecord template = ticketingRepository.findEntitlementTemplateById(binding.entitlementTemplateId())
                    .orElse(null);
            if (template == null) {
                continue;
            }
            String merchantId = template.merchantId() != null ? template.merchantId() : original.merchantId();
            ids.add(issueCompensationEntitlement(original, template.id(), template.entitlementType(), merchantId,
                    template.venueId(), template.pointId(), binding.valueAmountCent(), template.entitlementKind(),
                    template.agreementPriceCent(), template.consumptionWindow(), template.exclusiveMerchantId(),
                    item.id(), validUntil, now));
        }
        return ids;
    }

    private String issueCompensationEntitlement(IssuedEntitlementRecord original, String templateId, String entitlementType,
            String merchantId, String venueId, String pointId, Long valueAmountCent, String entitlementKind,
            Long agreementPriceCent, String consumptionWindow, String exclusiveMerchantId, String itemId,
            Instant validUntil, Instant now) {
        String id = "ent_" + UUID.randomUUID().toString().replace("-", "");
        String code = "CP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        ticketingRepository.saveIssuedEntitlement(new IssuedEntitlementRecord(id, code, original.ticketTypeId(), original.orderId(),
                templateId, entitlementType, merchantId, venueId, pointId, 1, 0, valueAmountCent, "active", now, validUntil,
                now, now, 1, 0L, entitlementKind, agreementPriceCent, consumptionWindow, exclusiveMerchantId,
                original.parentTicketId(), original.slotIndex(), original.barId(), null, itemId));
        return id;
    }

    // ---------- helpers ----------

    private AmusementParkWideFaultRecord requireFault(String faultId) {
        AmusementParkWideFaultRecord fault = faultRepository.findById(faultId)
                .orElseThrow(() -> ApiException.notFound("PARK_WIDE_FAULT_NOT_FOUND", "全园故障状态记录不存在"));
        requireProjectInScope(fault.projectId());
        return fault;
    }

    private void requireProjectInScope(String projectId) {
        String scope = authorizationService.currentProjectScopeQuietly().orElse(null);
        if (scope != null && !scope.equals(projectId)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能操作当前综合体项目的全园故障补偿");
        }
    }

    private String requireProject(String projectId) {
        String scope = authorizationService.currentProjectScopeQuietly().orElse(null);
        String requested = trimToNull(projectId);
        if (scope != null) {
            if (requested != null && !requested.equals(scope) && !AmusementCompensationPolicyRecord.GLOBAL_SCOPE.equals(requested)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "只能操作当前综合体项目的补偿参数");
            }
            return requested == null ? scope : requested;
        }
        if (requested == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定综合体项目");
        }
        return requested;
    }

    private static String requireText(String value, String message) {
        String v = trimToNull(value);
        if (v == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", message);
        }
        return v;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String codeOf(RuntimeException exception) {
        return exception instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }
}
