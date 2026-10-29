package com.complex.platform.settlement;

// 文件版本：V0.02

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.settlement.MediaPerformanceRecords.FulfillmentRecord;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowDetail;
import com.complex.platform.settlement.MediaPerformanceRecords.ShowRecord;
import com.complex.platform.settlement.MediaPerformanceRequests.ActionRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.DisputeRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.FulfillmentRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.ResolveRequest;
import com.complex.platform.settlement.MediaPerformanceRequests.ShowRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TK-29-R1/R2：传媒逐场演出合同记录（三种情形、基数模板、版本锁定）与实际履约登记 / 确认 / 争议 / 裁决。
 *
 * <p>依据 BSM L1145：「按演出合同可以同时存在三种情形，也可以只有其中一种或两种」「在后台配置页面逐场填写、随合同固化版本」
 * 「每场演出适用哪几种情形、各自的数值与合同版本，必须逐场记录」「由传媒经办人在现有逐场合同记录中登记实际履约时间、已完成和未完成内容及依据；
 * 到酒吧演出的，由酒吧指定现场责任人确认；传媒自办场次，由传媒指定主管确认，确认过程按原留痕规则记录」「传媒自办场次……不因本条确认新增酒吧应付」。
 * 操作员裁决（2026-10-20）：逐场合同记录里填「本场每张门票加价金额」并指定适用的酒吧门票票种，随合同固化。
 *
 * <p>范围：项目范围按操作者项目（平台管理员不受约束，D-032）；「酒吧指定现场责任人」＝持确认权限码且属该酒吧商户范围的员工，
 * 「传媒指定主管」＝持确认权限码且属该传媒商户范围、且不是登记人本人的员工。争议裁决照 ContractService.resolveDispute 范式（结论必填、裁决人/时留痕）。
 *
 * <p>复核修复：各写口按 OpenAPI x-idempotency-required 接 {@link IdempotencyService#execute}（同号同体返回第一次结果、同号异体 409，PGM L303）；
 * 酒吧 / 传媒商户须属该场项目；已有出账结算的场次不得新建或锁定合同版本，改走结算调整（BSM L1145「关联原记录……不覆盖原结果」）。
 */
@Service
public class MediaPerformanceService {

    static final String PERM_VIEW = "media.performance.view";
    static final String PERM_CONTRACT = "media.performance.contract.manage";
    static final String PERM_RECORD = "media.performance.fulfillment.record";
    static final String PERM_CONFIRM = "media.performance.fulfillment.confirm";
    static final String PERM_RESOLVE = "media.performance.dispute.resolve";
    static final String BUSINESS_TYPE = "media_performance_show";

    private static final Set<String> SHOW_KINDS = Set.of("bar_show", "self_hosted");
    private static final Set<String> TICKET_MODES = Set.of("none", "ratio", "fixed");
    private static final Set<String> DRINK_TEMPLATES = Set.of("show_window", "whole_store_session");

    private final JdbcMediaPerformanceRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;
    private final IdempotencyService idempotencyService;

    public MediaPerformanceService(JdbcMediaPerformanceRepository repository, AuthorizationService authorizationService,
                                   AuditEventService auditEventService, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
        this.idempotencyService = idempotencyService;
    }

    // ================================================================ R1 逐场合同记录

    @Transactional
    public ShowRecord createShow(ShowRequest request) {
        authorizationService.requirePermission(PERM_CONTRACT);
        AuthorizationService.ProjectReach reach = reach(PERM_CONTRACT);
        ShowRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_show.created", "media_show.create_failed", PERM_CONTRACT, null, key, actual, ShowRecord.class,
                () -> requireProjectInReach(actual.projectId(), reach), () -> {
            AccessActor actor = authorizationService.currentActor();
            ShowRecord show = validate(actual, "ms_" + UUID.randomUUID(), null, 1, actor.id(), Instant.now());
            requireScope(show, reach);
            return repository.insertShow(show);
        });
    }

    /** 草案可改；已锁定的合同数值不可改（409 MEDIA_SHOW_LOCKED），改动须走 {@link #createVersion}。 */
    @Transactional
    public ShowRecord updateShow(String showId, ShowRequest request) {
        authorizationService.requirePermission(PERM_CONTRACT);
        AuthorizationService.ProjectReach reach = reach(PERM_CONTRACT);
        ShowRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_show.updated", "media_show.update_failed", PERM_CONTRACT, showId, key, actual, ShowRecord.class,
                () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord existing = requireShowForUpdate(showId);
            requireScope(existing, reach);
            if (!"draft".equals(existing.status())) {
                throw ApiException.conflict("MEDIA_SHOW_LOCKED", "该场合同已锁定版本，数值不可修改；如需变更请新建合同版本");
            }
            ShowRecord updated = validate(actual, existing.id(), existing.showGroupId(), existing.contractVersion(),
                    existing.createdBy(), existing.createdAt());
            requireScope(updated, reach);
            return repository.updateShowValues(updated);
        });
    }

    /** 锁定版本：随合同固化，结算只读 locked 行；同组旧的 locked 版本置 superseded（旧行保留）。重复锁定幂等。 */
    @Transactional
    public ShowRecord lockShow(String showId, ActionRequest request) {
        authorizationService.requirePermission(PERM_CONTRACT);
        AuthorizationService.ProjectReach reach = reach(PERM_CONTRACT);
        String key = requireText(requireRequest(request).idempotencyKey(), "idempotency_key");
        return audited("media_show.locked", "media_show.lock_failed", PERM_CONTRACT, showId, key, request, ShowRecord.class,
                () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord existing = requireShowForUpdate(showId);
            requireScope(existing, reach);
            if ("locked".equals(existing.status())) {
                return existing;
            }
            if (!"draft".equals(existing.status())) {
                throw ApiException.conflict("MEDIA_SHOW_SUPERSEDED", "该合同版本已被更新版本取代，不能再锁定");
            }
            requireNoPostedSettlement(existing.showGroupId());
            for (ShowRecord sibling : repository.listVersions(existing.showGroupId())) {
                if (!sibling.id().equals(existing.id()) && "locked".equals(sibling.status())) {
                    repository.updateShowStatus(sibling.id(), "superseded", null, null);
                }
            }
            Instant now = Instant.now();
            String by = authorizationService.currentActor().id();
            repository.updateShowStatus(existing.id(), "locked", now, by);
            return repository.findShow(existing.id()).orElseThrow();
        });
    }

    /** 从已锁定版本新建草案版本（contract_version+1，旧行保留）；项目 / 场次类型 / 传媒商户沿用原版本。 */
    @Transactional
    public ShowRecord createVersion(String showId, ShowRequest request) {
        authorizationService.requirePermission(PERM_CONTRACT);
        AuthorizationService.ProjectReach reach = reach(PERM_CONTRACT);
        ShowRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_show.version_created", "media_show.version_create_failed", PERM_CONTRACT, showId, key, actual,
                ShowRecord.class, () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord source = requireShowForUpdate(showId);
            requireScope(source, reach);
            if ("draft".equals(source.status())) {
                throw ApiException.conflict("MEDIA_SHOW_NOT_LOCKED", "草案直接修改即可，不必新建版本");
            }
            requireNoPostedSettlement(source.showGroupId());
            List<ShowRecord> versions = repository.listVersions(source.showGroupId());
            if (versions.stream().anyMatch(v -> "draft".equals(v.status()))) {
                throw ApiException.conflict("MEDIA_SHOW_VERSION_DRAFT_EXISTS", "该场已有未锁定的新版本草案");
            }
            int nextVersion = versions.stream().mapToInt(ShowRecord::contractVersion).max().orElse(0) + 1;
            ShowRequest pinned = new ShowRequest(actual.idempotencyKey(), source.projectId(), actual.barMerchantId(),
                    source.mediaMerchantId(), source.showKind(), actual.showDate(), actual.plannedStartAt(), actual.plannedEndAt(),
                    actual.contractRef(), actual.ticketShareMode(), actual.ticketShareBps(), actual.ticketShareAmountCent(),
                    actual.ticketSurchargePerTicketCent(), actual.ticketTypeId(), actual.fixedFeeCent(),
                    actual.drinkCommissionBps(), actual.drinkBaseTemplate());
            ShowRecord created = validate(pinned, "ms_" + UUID.randomUUID(), source.showGroupId(), nextVersion,
                    authorizationService.currentActor().id(), Instant.now());
            requireScope(created, reach);
            return repository.insertShow(created);
        });
    }

    /** OpenAPI 声明 media_show.listed / media_show.list_failed。 */
    public List<ShowRecord> listShows(String projectId, String merchantId) {
        try {
            authorizationService.requirePermission(PERM_VIEW);
            String merchant = authorizationService.currentMerchantIdQuietly().orElse(null);
            String requested = trimToNull(projectId);
            List<ShowRecord> shows;
            if (merchant != null || authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
                // 商户账号照旧只看本人项目里本商户是酒吧方或传媒方的场次；平台管理员照旧
                shows = repository.listShows(authorizationService.currentProjectScopeQuietly().orElse(requested),
                        merchant != null ? merchant : trimToNull(merchantId));
            } else {
                // 第十八批 G 段 82b-R5：其余按 media.performance.view 可触及的项目取；带项目的须可触及
                AuthorizationService.ProjectReach reach = reach(PERM_VIEW);
                if (requested != null && !reach.covers(requested)) {
                    throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "所选项目不在本权限可触及的项目范围内");
                }
                shows = requested != null ? repository.listShows(requested, trimToNull(merchantId))
                        : repository.listShowsIn(reach.organizationIds(), trimToNull(merchantId));
            }
            auditEventService.recordSuccess("media_show.listed", PERM_VIEW, BUSINESS_TYPE, null, null, null, Map.of("count", shows.size()));
            return shows;
        } catch (ApiException failure) {
            auditEventService.recordFailure("media_show.list_failed", PERM_VIEW, BUSINESS_TYPE, null, null, null, failure.code());
            throw failure;
        }
    }

    /** OpenAPI 声明 media_show.viewed / media_show.view_failed。 */
    public ShowDetail getDetail(String showId) {
        try {
            authorizationService.requirePermission(PERM_VIEW);
            ShowRecord show = requireShow(showId);
            requireScope(show, reach(PERM_VIEW));
            ShowDetail detail = new ShowDetail(show, repository.findFulfillmentByShow(show.id()).orElse(null),
                    repository.listSettlements(show.id()));
            auditEventService.recordSuccess("media_show.viewed", PERM_VIEW, BUSINESS_TYPE, show.id(), null, null, Map.of());
            return detail;
        } catch (ApiException failure) {
            auditEventService.recordFailure("media_show.view_failed", PERM_VIEW, BUSINESS_TYPE, showId, null, null, failure.code());
            throw failure;
        }
    }

    // ================================================================ R2 实际履约登记与确认

    /** 传媒经办登记实际履约：合同须已锁定；登记人须属该传媒商户范围；一场一条。 */
    @Transactional
    public FulfillmentRecord recordFulfillment(String showId, FulfillmentRequest request) {
        authorizationService.requirePermission(PERM_RECORD);
        AuthorizationService.ProjectReach reach = reach(PERM_RECORD);
        FulfillmentRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_fulfillment.recorded", "media_fulfillment.record_failed", PERM_RECORD, showId, key, actual,
                FulfillmentRecord.class, () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord show = requireShowForUpdate(showId);
            requireScope(show, reach);
            if (!"locked".equals(show.status())) {
                throw ApiException.conflict("MEDIA_SHOW_NOT_LOCKED", "合同版本尚未锁定，不能登记实际履约");
            }
            AccessActor actor = authorizationService.currentActor();
            requireMerchant(actor, show.mediaMerchantId(), "实际履约须由该传媒公司经办人登记");
            if (repository.findFulfillmentByShow(show.id()).isPresent()) {
                throw ApiException.conflict("MEDIA_FULFILLMENT_EXISTS", "该场已登记实际履约；如有出入请走争议处理");
            }
            Instant start = actual.actualStartAt();
            Instant end = actual.actualEndAt();
            if (start == null || end == null || !end.isAfter(start)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "实际履约时间须填写且结束晚于开始");
            }
            Instant now = Instant.now();
            return repository.insertFulfillment(new FulfillmentRecord("mf_" + UUID.randomUUID(), show.id(), start, end,
                    requireText(actual.completedContent(), "completed_content"), trimToNull(actual.uncompletedContent()),
                    requireText(actual.basis(), "basis"), actor.id(), now, "recorded",
                    null, null, null, null, null, null, null, null));
        });
    }

    /** 确认：到酒吧演出由该酒吧范围的现场责任人确认；自办场次由该传媒范围、非登记人本人的主管确认。 */
    @Transactional
    public FulfillmentRecord confirmFulfillment(String showId, ActionRequest request) {
        authorizationService.requirePermission(PERM_CONFIRM);
        AuthorizationService.ProjectReach reach = reach(PERM_CONFIRM);
        String key = requireText(requireRequest(request).idempotencyKey(), "idempotency_key");
        return audited("media_fulfillment.confirmed", "media_fulfillment.confirm_failed", PERM_CONFIRM, showId, key, request,
                FulfillmentRecord.class, () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord show = requireShowForUpdate(showId);
            FulfillmentRecord f = requireRecorded(show);
            AccessActor actor = requireConfirmer(show, f, reach);
            Instant now = Instant.now();
            return repository.updateFulfillment(new FulfillmentRecord(f.id(), f.showId(), f.actualStartAt(), f.actualEndAt(),
                    f.completedContent(), f.uncompletedContent(), f.basis(), f.recordedBy(), f.recordedAt(), "confirmed",
                    actor.id(), now, f.disputeReason(), f.disputedBy(), f.disputedAt(), f.resolutionNote(), f.resolvedBy(), f.resolvedAt()));
        });
    }

    /** 提出争议（确认方同一范围）：挂起 disputed，相关结算保持待处理。 */
    @Transactional
    public FulfillmentRecord disputeFulfillment(String showId, DisputeRequest request) {
        authorizationService.requirePermission(PERM_CONFIRM);
        AuthorizationService.ProjectReach reach = reach(PERM_CONFIRM);
        DisputeRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_fulfillment.disputed", "media_fulfillment.dispute_failed", PERM_CONFIRM, showId, key, actual,
                FulfillmentRecord.class, () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord show = requireShowForUpdate(showId);
            FulfillmentRecord f = requireRecorded(show);
            AccessActor actor = requireConfirmer(show, f, reach);
            String reason = requireText(actual.disputeReason(), "dispute_reason");
            Instant now = Instant.now();
            return repository.updateFulfillment(new FulfillmentRecord(f.id(), f.showId(), f.actualStartAt(), f.actualEndAt(),
                    f.completedContent(), f.uncompletedContent(), f.basis(), f.recordedBy(), f.recordedAt(), "disputed",
                    null, null, reason, actor.id(), now, null, null, null));
        });
    }

    /** 主管裁决争议（结论必填、裁决人 / 时间留痕）→ resolved，事实回到可结算态。 */
    @Transactional
    public FulfillmentRecord resolveDispute(String showId, ResolveRequest request) {
        authorizationService.requirePermission(PERM_RESOLVE);
        AuthorizationService.ProjectReach reach = reach(PERM_RESOLVE);
        ResolveRequest actual = requireRequest(request);
        String key = requireText(actual.idempotencyKey(), "idempotency_key");
        return audited("media_fulfillment.dispute_resolved", "media_fulfillment.dispute_resolve_failed", PERM_RESOLVE, showId, key,
                actual, FulfillmentRecord.class, () -> requireScope(requireShow(showId), reach), () -> {
            ShowRecord show = requireShowForUpdate(showId);
            requireScope(show, reach);
            FulfillmentRecord f = repository.findFulfillmentByShow(show.id())
                    .orElseThrow(() -> ApiException.notFound("MEDIA_FULFILLMENT_NOT_FOUND", "该场尚未登记实际履约"));
            if (!"disputed".equals(f.status())) {
                throw ApiException.conflict("MEDIA_FULFILLMENT_NOT_DISPUTED", "该场履约记录不在争议状态");
            }
            String note = requireText(actual.resolutionNote(), "resolution_note");
            Instant now = Instant.now();
            return repository.updateFulfillment(new FulfillmentRecord(f.id(), f.showId(), f.actualStartAt(), f.actualEndAt(),
                    f.completedContent(), f.uncompletedContent(), f.basis(), f.recordedBy(), f.recordedAt(), "resolved",
                    f.confirmedBy(), f.confirmedAt(), f.disputeReason(), f.disputedBy(), f.disputedAt(), note,
                    authorizationService.currentActor().id(), now));
        });
    }

    // ================================================================ 内部

    private FulfillmentRecord requireRecorded(ShowRecord show) {
        FulfillmentRecord f = repository.findFulfillmentByShow(show.id())
                .orElseThrow(() -> ApiException.notFound("MEDIA_FULFILLMENT_NOT_FOUND", "该场尚未登记实际履约"));
        if (!"recorded".equals(f.status())) {
            throw ApiException.conflict("MEDIA_FULFILLMENT_NOT_RECORDED", "该场履约记录当前状态为 " + f.status() + "，不能再确认或提出争议");
        }
        return f;
    }

    /** 确认 / 争议的主体规则：bar_show → 酒吧范围；self_hosted → 传媒范围且非登记人本人。 */
    private AccessActor requireConfirmer(ShowRecord show, FulfillmentRecord f, AuthorizationService.ProjectReach reach) {
        AccessActor actor = authorizationService.currentActor();
        requireScope(show, reach);
        if (show.selfHosted()) {
            requireMerchant(actor, show.mediaMerchantId(), "自办场次须由该传媒公司指定主管确认");
            if (actor.id().equals(f.recordedBy())) {
                throw ApiException.conflict("MEDIA_FULFILLMENT_SELF_CONFIRM", "登记人本人不能确认自己登记的履约事实，须由传媒指定主管确认");
            }
        } else {
            requireMerchant(actor, show.barMerchantId(), "到酒吧演出须由该酒吧指定的现场责任人确认");
        }
        return actor;
    }

    /** 商户范围：操作者带商户归属时必须就是该商户；平台管理员与无商户归属的平台员工不受约束（D-032）。 */
    private void requireMerchant(AccessActor actor, String merchantId, String message) {
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        String own = actor.merchantId();
        if (own == null || own.isBlank() || !own.equals(merchantId)) {
            throw ApiException.forbidden("MEDIA_PERFORMANCE_SCOPE_DENIED", message);
        }
    }

    /** 复核 money I5：该场（同组任一版本）已有出账结算时不得新建或锁定合同版本，须走结算调整。 */
    private void requireNoPostedSettlement(String showGroupId) {
        for (ShowRecord version : repository.listVersions(showGroupId)) {
            if (repository.listSettlements(version.id()).stream()
                    .anyMatch(s -> "posted".equals(s.status()) || "adjusted".equals(s.status()))) {
                throw ApiException.conflict("MEDIA_SHOW_SETTLED_USE_ADJUST", "该场已按合同出账结算，合同变更须走结算调整（关联原记录），不能新建或锁定合同版本");
            }
        }
    }

    /** 第十八批 G 段 82b-R5：入口权限码可触及的项目范围（含可跨项目角色勾选的项目），在幂等与取锁之前算好；结算域复用。 */
    AuthorizationService.ProjectReach reach(String permission) {
        return authorizationService.projectReach(authorizationService.currentActor(), permission);
    }

    /**
     * 项目范围 + 商户范围（商户用户只能碰自己是酒吧方或传媒方的场次）；结算域复用（复核 authz B1）。
     * 第十八批 G 段 82b-R5：项目按入口已算好的 reach 判（商户账号的 reach 只到本人项目，与原先一致）。
     */
    void requireScope(ShowRecord show, AuthorizationService.ProjectReach reach) {
        if (reach.platformAdmin()) {
            return;
        }
        if (!reach.covers(show.projectId())) {
            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "该演出场次不在本权限可触及的项目范围内");
        }
        AccessActor actor = authorizationService.currentActor();
        String own = actor.merchantId();
        if (own != null && !own.isBlank() && !own.equals(show.barMerchantId()) && !own.equals(show.mediaMerchantId())) {
            throw ApiException.forbidden("MEDIA_PERFORMANCE_SCOPE_DENIED", "该演出场次不属于当前商户范围");
        }
    }

    private ShowRecord validate(ShowRequest r, String id, String groupId, int version, String createdBy, Instant createdAt) {
        String projectId = requireText(r.projectId(), "project_id");
        String kind = requireText(r.showKind(), "show_kind");
        if (!SHOW_KINDS.contains(kind)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "show_kind 只能是 bar_show 或 self_hosted");
        }
        if (r.showDate() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "show_date 必填");
        }
        if (r.plannedStartAt() != null && r.plannedEndAt() != null && !r.plannedEndAt().isAfter(r.plannedStartAt())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "计划结束时间须晚于开始时间");
        }
        String media = requireText(r.mediaMerchantId(), "media_merchant_id");
        if (!"media".equals(repository.merchantType(media).orElse(null))) {
            throw ApiException.badRequest("MEDIA_SHOW_MEDIA_MERCHANT_INVALID", "media_merchant_id 必须是业态为演出传媒（media）的商户");
        }
        requireMerchantInProject(media, projectId);
        String bar = trimToNull(r.barMerchantId());
        String mode = trimToNull(r.ticketShareMode()) == null ? "none" : r.ticketShareMode().trim();
        if (!TICKET_MODES.contains(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "ticket_share_mode 只能是 none / ratio / fixed");
        }
        boolean selfHosted = "self_hosted".equals(kind);
        if (selfHosted) {
            if (r.fixedFeeCent() != null || r.drinkCommissionBps() != null || trimToNull(r.drinkBaseTemplate()) != null
                    || !"none".equals(mode)) {
                throw ApiException.badRequest("MEDIA_SHOW_SELF_HOSTED_NO_BAR_PAYABLE",
                        "传媒自办场次按原售票收款与收入归属规则办理，不得约定固定演出费、酒水提成或酒吧门票加价分成");
            }
            if (bar != null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "自办场次不填 bar_merchant_id");
            }
        } else {
            bar = requireText(bar, "bar_merchant_id");
            if (!"bar".equals(repository.merchantType(bar).orElse(null))) {
                throw ApiException.badRequest("MEDIA_SHOW_BAR_MERCHANT_INVALID", "bar_merchant_id 必须是业态为酒吧（bar）的商户");
            }
            requireMerchantInProject(bar, projectId);
        }
        Integer bps = null;
        Long amount = null;
        Long surcharge = null;
        String ticketType = null;
        if (!"none".equals(mode)) {
            surcharge = r.ticketSurchargePerTicketCent();
            if (surcharge == null || surcharge <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "本场每张门票加价金额（分）须大于 0");
            }
            ticketType = requireText(r.ticketTypeId(), "ticket_type_id");
            String ticketBar = repository.ticketTypeBar(ticketType)
                    .orElseThrow(() -> ApiException.badRequest("VALIDATION_FAILED", "适用票种不存在"));
            if (!bar.equals(ticketBar)) {
                throw ApiException.badRequest("MEDIA_SHOW_TICKET_TYPE_NOT_BAR", "适用票种必须是该酒吧的门票票种");
            }
            if ("ratio".equals(mode)) {
                bps = r.ticketShareBps();
                if (bps == null || bps < 1 || bps > 10000) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "门票加价分成比例（万分比）须在 1～10000");
                }
            } else {
                amount = r.ticketShareAmountCent();
                if (amount == null || amount <= 0) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "门票加价分成金额（分）须大于 0");
                }
            }
        }
        if (r.fixedFeeCent() != null && r.fixedFeeCent() <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "固定演出费（分）须大于 0");
        }
        String template = trimToNull(r.drinkBaseTemplate());
        if (r.drinkCommissionBps() != null) {
            if (r.drinkCommissionBps() < 1 || r.drinkCommissionBps() > 10000) {
                throw ApiException.badRequest("VALIDATION_FAILED", "酒水提成比例（万分比）须在 1～10000");
            }
            if (template == null || !DRINK_TEMPLATES.contains(template)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "酒水提成须选择基数模板：show_window（演出时段）或 whole_store_session（当场全店）");
            }
        } else if (template != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "未约定酒水提成时不填基数模板");
        }
        return new ShowRecord(id, groupId == null ? id : groupId, version, projectId, bar, media, kind, r.showDate(),
                r.plannedStartAt(), r.plannedEndAt(), trimToNull(r.contractRef()), mode, bps, amount, surcharge, ticketType,
                r.fixedFeeCent(), r.drinkCommissionBps(), template, "draft", null, null, createdBy, createdAt, Instant.now());
    }

    /** 复核 authz M2：场次的酒吧 / 传媒商户须属于该场项目；第十八批 G 段 82b-R8：以该场项目为主项目或已批准的经营项目均可（PGM:659）。 */
    private void requireMerchantInProject(String merchantId, String projectId) {
        if (!repository.merchantOperatesIn(merchantId, projectId)) {
            throw ApiException.badRequest("MEDIA_SHOW_MERCHANT_PROJECT_MISMATCH", "商户 " + merchantId + " 不属于该场项目");
        }
    }

    private ShowRecord requireShow(String showId) {
        return repository.findShow(requireText(showId, "show_id"))
                .orElseThrow(() -> ApiException.notFound("MEDIA_SHOW_NOT_FOUND", "演出场次不存在"));
    }

    private ShowRecord requireShowForUpdate(String showId) {
        return repository.findShowForUpdate(requireText(showId, "show_id"))
                .orElseThrow(() -> ApiException.notFound("MEDIA_SHOW_NOT_FOUND", "演出场次不存在"));
    }

    /** 复核：新建按请求 project_id 先判可触及（没带项目由体内校验报 400）。 */
    private static void requireProjectInReach(String projectId, AuthorizationService.ProjectReach reach) {
        String project = trimToNull(projectId);
        if (project != null && !reach.covers(project)) {
            throw ApiException.forbidden("PROJECT_SCOPE_MISMATCH", "该演出场次不在本权限可触及的项目范围内");
        }
    }

    /**
     * 成功事件随事务提交；失败事件独立事务留痕后原样抛出。复核 authz I1：主体包在 {@link IdempotencyService#execute} 里——
     * 同号同体返回第一次结果（不重复执行、不重复记成功），同号异体 409。复核（范围闸在幂等之前）：gate 在幂等之前跑，同号重放也先过它；
     * 体内按锁重读后照旧再核。
     */
    private <T> T audited(String successEvent, String failureEvent, String permission, String businessId, String key,
                          Object request, Class<T> responseType, Runnable gate, Supplier<T> body) {
        try {
            gate.run();
            return idempotencyService.execute(successEvent + ":" + (businessId == null ? "" : businessId), key, request, () -> {
                T result = body.get();
                String id = businessId != null ? businessId : idOf(result);
                auditEventService.recordSuccess(successEvent, permission, BUSINESS_TYPE, id, null, key, Map.of());
                return result;
            }, responseType);
        } catch (ApiException failure) {
            auditEventService.recordFailure(failureEvent, permission, BUSINESS_TYPE, businessId, null, key, failure.code());
            throw failure;
        }
    }

    private static String idOf(Object result) {
        if (result instanceof ShowRecord show) {
            return show.id();
        }
        if (result instanceof FulfillmentRecord f) {
            return f.showId();
        }
        return null;
    }

    private static <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求体不能为空");
        }
        return request;
    }
}
