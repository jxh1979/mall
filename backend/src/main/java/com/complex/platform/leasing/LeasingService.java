package com.complex.platform.leasing;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.ContractRecord;
import com.complex.platform.governance.PlatformService;
import com.complex.platform.governance.dto.ContractDraftRequest;
import com.complex.platform.leasing.dto.LeasingConvertRequest;
import com.complex.platform.leasing.dto.LeasingLeadRequest;
import com.complex.platform.leasing.dto.LeasingOfferRequest;
import com.complex.platform.leasing.dto.LeasingStatusRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * ② 履约闭环 S4：招商线索→租赁报价→转签约。链条最前端，accepted 报价 convertOfferToContract 喂入既有合同环
 *（PlatformService.createContractDraft）。写均 requirePlatformScope + 落审计；convert 幂等（已转拒重复建）。
 */
@Service
public class LeasingService {

    private static final Set<String> LEAD_STATUSES = Set.of("new", "contacted", "qualified", "converted", "lost");
    private static final Set<String> OFFER_STATUSES = Set.of("draft", "sent", "accepted", "rejected", "expired", "converted");

    private final LeasingRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final PlatformService platformService;

    public LeasingService(LeasingRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, PlatformService platformService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.platformService = platformService;
    }

    public PageResponse<LeasingLeadRecord> listLeads() {
        List<LeasingLeadRecord> items = repository.listLeads();
        auditEventService.recordSuccess("leasing.lead.viewed", "leasing.lead.manage", "leasing_lead", "all", null, null,
                Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public LeasingLeadRecord createLead(LeasingLeadRequest request) {
        authorizationService.requirePlatformScope();
        LeasingLeadRequest actual = requireRequest(request);
        Instant now = Instant.now();
        LeasingLeadRecord saved = repository.saveLead(new LeasingLeadRecord(
                "lead_" + UUID.randomUUID(), requireText(actual.contactName(), "contact_name"),
                trimToNull(actual.intendedBusinessType()), trimToNull(actual.intendedSpaceId()),
                trimToNull(actual.source()), "new", null, now, now));
        auditEventService.recordSuccess("leasing.lead.created", "leasing.lead.manage", "leasing_lead", saved.id(),
                null, actual.idempotencyKey(), Map.of());
        return saved;
    }

    public LeasingLeadRecord updateLeadStatus(String leadId, LeasingStatusRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(leadId, "lead_id");
        LeasingStatusRequest actual = requireRequest(request);
        String status = requireEnum(actual.status(), LEAD_STATUSES, "status");
        LeasingLeadRecord ex = repository.findLeadById(id)
                .orElseThrow(() -> ApiException.notFound("LEASING_LEAD_NOT_FOUND", "lead not found"));
        LeasingLeadRecord saved = repository.saveLead(new LeasingLeadRecord(ex.id(), ex.contactName(),
                ex.intendedBusinessType(), ex.intendedSpaceId(), ex.source(), status, ex.projectId(),
                ex.createdAt(), Instant.now()));
        auditEventService.recordSuccess("leasing.lead.status_changed", "leasing.lead.manage", "leasing_lead", id,
                null, actual.idempotencyKey(), Map.of("status", status));
        return saved;
    }

    public PageResponse<LeasingOfferRecord> listOffers() {
        List<LeasingOfferRecord> items = repository.listOffers();
        auditEventService.recordSuccess("leasing.offer.viewed", "leasing.offer.manage", "leasing_offer", "all", null, null,
                Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public LeasingOfferRecord createOffer(LeasingOfferRequest request) {
        authorizationService.requirePlatformScope();
        LeasingOfferRequest actual = requireRequest(request);
        String leadId = requireText(actual.leadId(), "lead_id");
        repository.findLeadById(leadId)
                .orElseThrow(() -> ApiException.notFound("LEASING_LEAD_NOT_FOUND", "lead not found"));
        Instant now = Instant.now();
        LeasingOfferRecord saved = repository.saveOffer(new LeasingOfferRecord(
                "offer_" + UUID.randomUUID(), leadId, trimToNull(actual.spaceId()), trimToNull(actual.merchantId()),
                actual.effectiveDate(), actual.expiryDate(), actual.rentTerms() == null ? Map.of() : actual.rentTerms(),
                actual.validUntil(), "draft", null, null, now, now));
        auditEventService.recordSuccess("leasing.offer.created", "leasing.offer.manage", "leasing_offer", saved.id(),
                null, actual.idempotencyKey(), Map.of("lead_id", leadId));
        return saved;
    }

    public LeasingOfferRecord updateOfferStatus(String offerId, LeasingStatusRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(offerId, "offer_id");
        LeasingStatusRequest actual = requireRequest(request);
        String status = requireEnum(actual.status(), OFFER_STATUSES, "status");
        LeasingOfferRecord ex = repository.findOfferById(id)
                .orElseThrow(() -> ApiException.notFound("LEASING_OFFER_NOT_FOUND", "offer not found"));
        LeasingOfferRecord saved = repository.saveOffer(withOfferStatus(ex, status, ex.convertedContractId()));
        auditEventService.recordSuccess("leasing.offer.status_changed", "leasing.offer.manage", "leasing_offer", id,
                null, actual.idempotencyKey(), Map.of("status", status));
        return saved;
    }

    /** accepted 报价 → 生成合同 draft（喂入既有合同环）+ 标 offer/lead converted。幂等：已转拒重复建。 */
    public ContractRecord convertOfferToContract(String offerId, LeasingConvertRequest request) {
        authorizationService.requirePlatformScope();
        String id = requireText(offerId, "offer_id");
        LeasingConvertRequest actual = requireRequest(request);
        LeasingOfferRecord offer = repository.findOfferById(id)
                .orElseThrow(() -> ApiException.notFound("LEASING_OFFER_NOT_FOUND", "offer not found"));
        // 幂等守卫优先：已转过（status=converted / convertedContractId 已写）→ 拒重复建，给出明确码。
        if (offer.convertedContractId() != null || "converted".equals(offer.status())) {
            throw ApiException.conflict("LEASING_OFFER_ALREADY_CONVERTED", "offer already converted to contract");
        }
        if (!"accepted".equals(offer.status())) {
            throw ApiException.conflict("LEASING_OFFER_NOT_ACCEPTED", "only accepted offer can convert to contract");
        }
        // createContractDraft 的幂等键确定性派生（同 offer→同合同），叠加上方 already-converted 守卫，防重复建合同。
        ContractRecord contract = platformService.createContractDraft(new ContractDraftRequest(
                "lease-convert-" + id, "converted from leasing offer " + id, List.of(),
                requireText(offer.merchantId(), "merchant_id"),
                requireText(actual.contractSubjectId(), "contract_subject_id"),
                trimToNull(actual.operationSubjectId()),
                requireText(actual.settlementSubjectId(), "settlement_subject_id"),
                offer.effectiveDate(), offer.expiryDate(), offer.rentTerms(), null, List.of()));
        repository.saveOffer(withOfferStatus(offer, "converted", contract.id()));
        repository.findLeadById(offer.leadId()).ifPresent(lead -> repository.saveLead(new LeasingLeadRecord(
                lead.id(), lead.contactName(), lead.intendedBusinessType(), lead.intendedSpaceId(), lead.source(),
                "converted", lead.projectId(), lead.createdAt(), Instant.now())));
        auditEventService.recordSuccess("leasing.offer.converted", "leasing.offer.manage", "leasing_offer", id,
                null, actual.idempotencyKey(), Map.of("contract_id", contract.id()));
        return contract;
    }

    private LeasingOfferRecord withOfferStatus(LeasingOfferRecord o, String status, String convertedContractId) {
        return new LeasingOfferRecord(o.id(), o.leadId(), o.spaceId(), o.merchantId(), o.effectiveDate(), o.expiryDate(),
                o.rentTerms(), o.validUntil(), status, convertedContractId, o.projectId(), o.createdAt(), Instant.now());
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        String t = value == null ? "" : value.trim();
        if (t.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return t;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String t = requireText(value, field);
        if (!allowed.contains(t)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return t;
    }

    private String trimToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }
}
