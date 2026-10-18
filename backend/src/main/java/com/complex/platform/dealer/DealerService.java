package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerDeactivateRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerQuoteDecisionRequest;
import com.complex.platform.dealer.dto.DealerQuoteRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CG-11a 外部经销商准入、价格与库存（BSM:1216-1222）。
 * <ul>
 *   <li>经销商五态（ENUM:170）：待提交 → 审核中 → 已通过 / 已驳回；已通过 → 已停用。企业资料、合同、开票、银行资料四项都审核通过才可
 *       启用独立经销商门户（BSM:1217）；停用后新询价、新目录一律禁止（BSM:1246）。</li>
 *   <li>价格目录四态（ENUM:172）：每份合同独立、版本递增；编辑人与审批人不得同人（ENUM:178）；新版本生效即旧版本失效，同一时刻只有一版生效。</li>
 *   <li>询价四态（ENUM:171）：已定合同价的商品不得询价（BSM:1220）；报价人与审批人不得同人。</li>
 *   <li>库存外显（BSM:1221 / ENUM:175）：按合同中心仓只给 有货 / 紧张 / 缺货 / 预计交期，不带数量。</li>
 * </ul>
 */
@Service
public class DealerService {

    static final String VIEW = "dealer.view";
    static final String MANAGE = "dealer.manage";
    static final String REVIEW = "dealer.review";
    static final String CATALOG_EDIT = "dealer.price_catalog.edit";
    static final String CATALOG_APPROVE = "dealer.price_catalog.approve";
    static final String INQUIRY_MANAGE = "dealer.inquiry.manage";
    static final String INQUIRY_QUOTE = "dealer.inquiry.quote";
    static final String INQUIRY_APPROVE = "dealer.inquiry.approve";

    static final String PENDING_SUBMISSION = "pending_submission";
    static final String UNDER_REVIEW = "under_review";
    static final String APPROVED = "approved";
    static final String REJECTED = "rejected";
    static final String DEACTIVATED = "deactivated";

    private final JdbcDealerRepository repository;
    private final AuthorizationService authorization;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public DealerService(JdbcDealerRepository repository, AuthorizationService authorization, IdempotencyService idempotency, AuditEventService audit) {
        this.repository = repository;
        this.authorization = authorization;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    // ── 经销商准入 ──

    public PageResponse<DealerRecord> listDealers() {
        authorization.requirePermission(VIEW);
        List<DealerRecord> items = repository.listDealers(projectScope());
        audit.recordSuccess("dealer.viewed", VIEW, "dealer", "list", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    public DealerRecord getDealer(String id) {
        authorization.requirePermission(VIEW);
        return requireDealer(id);
    }

    @Transactional
    public DealerRecord createDealer(DealerCreateRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            if (request == null || request.dealerName() == null || request.dealerName().isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "经销商名称必填");
            }
            return idempotency.execute("dealerCreate", request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerRecord dealer = new DealerRecord("dlr_" + UUID.randomUUID(), projectScope(), request.dealerName().trim(),
                        trim(request.unifiedSocialCreditCode()), trim(request.legalRepresentative()), trim(request.contactName()), trim(request.contactPhone()),
                        PENDING_SUBMISSION, false, false, false, false, false, null, null, null, null, null, null, actorId(), now, now);
                repository.saveDealer(dealer);
                audit.recordSuccess("dealer.created", MANAGE, "dealer", dealer.id(), null, request.idempotencyKey(),
                        Map.of("dealer_name", dealer.dealerName(), "status", dealer.status()));
                return dealer;
            }, DealerRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.create_failed", MANAGE, "dealer", null, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 待提交 / 已驳回 → 审核中。 */
    @Transactional
    public DealerRecord submitDealer(String id, String idempotencyKey) {
        authorization.requirePermission(MANAGE);
        try {
            DealerRecord dealer = requireDealer(id);
            if (!PENDING_SUBMISSION.equals(dealer.status()) && !REJECTED.equals(dealer.status())) {
                throw ApiException.conflict("DEALER_STATUS_INVALID", "只有待提交或已驳回的经销商可以提交审核");
            }
            return idempotency.execute("dealerSubmit:" + id, idempotencyKey, Map.of("id", id), () -> {
                DealerRecord saved = withStatus(dealer, UNDER_REVIEW, false, false, false, false, false, null, null, null);
                repository.saveDealer(saved);
                audit.recordSuccess("dealer.submitted", MANAGE, "dealer", id, null, idempotencyKey, Map.of("from", dealer.status(), "to", UNDER_REVIEW));
                return saved;
            }, DealerRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.submit_failed", MANAGE, "dealer", id, null, idempotencyKey, codeOf(e));
            throw e;
        }
    }

    /** 审核中 → 已通过（四项资料全部审核通过，启用门户）/ 已驳回。 */
    @Transactional
    public DealerRecord reviewDealer(String id, DealerReviewRequest request) {
        authorization.requirePermission(REVIEW);
        try {
            DealerRecord dealer = requireDealer(id);
            if (!UNDER_REVIEW.equals(dealer.status())) {
                throw ApiException.conflict("DEALER_STATUS_INVALID", "只有审核中的经销商可以审核");
            }
            String decision = requireText(request == null ? null : request.decision(), "decision");
            if (!List.of("approve", "reject").contains(decision)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "审核结论须为通过（approve）或驳回（reject）");
            }
            boolean cp = Boolean.TRUE.equals(request.companyProfileReviewed());
            boolean ct = Boolean.TRUE.equals(request.contractReviewed());
            boolean inv = Boolean.TRUE.equals(request.invoiceReviewed());
            boolean bank = Boolean.TRUE.equals(request.bankReviewed());
            if ("approve".equals(decision) && !(cp && ct && inv && bank)) {
                throw ApiException.conflict("DEALER_REVIEW_INCOMPLETE", "企业资料、合同、开票和银行资料须全部审核通过后才能通过准入并启用门户");
            }
            if ("reject".equals(decision) && (request.note() == null || request.note().isBlank())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "驳回须填写原因");
            }
            return idempotency.execute("dealerReview:" + id, request.idempotencyKey(), request, () -> {
                boolean approve = "approve".equals(decision);
                DealerRecord saved = withStatus(dealer, approve ? APPROVED : REJECTED, cp, ct, inv, bank, approve, trim(request.note()), actorId(), Instant.now());
                repository.saveDealer(saved);
                audit.recordSuccess("dealer.reviewed", REVIEW, "dealer", id, null, request.idempotencyKey(),
                        Map.of("decision", decision, "status", saved.status(), "portal_enabled", saved.portalEnabled(), "note", nz(request.note())));
                return saved;
            }, DealerRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.review_failed", REVIEW, "dealer", id, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 已通过 → 已停用：立即撤销门户资格（ENUM:181）；已完成记录保留；新询价、新目录禁止。 */
    @Transactional
    public DealerRecord deactivateDealer(String id, DealerDeactivateRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            DealerRecord dealer = requireDealer(id);
            if (!APPROVED.equals(dealer.status())) {
                throw ApiException.conflict("DEALER_STATUS_INVALID", "只有已通过的经销商可以停用");
            }
            String reason = requireText(request == null ? null : request.reason(), "reason");
            return idempotency.execute("dealerDeactivate:" + id, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerRecord saved = new DealerRecord(dealer.id(), dealer.projectId(), dealer.dealerName(), dealer.unifiedSocialCreditCode(),
                        dealer.legalRepresentative(), dealer.contactName(), dealer.contactPhone(), DEACTIVATED, dealer.companyProfileReviewed(),
                        dealer.contractReviewed(), dealer.invoiceReviewed(), dealer.bankReviewed(), false, dealer.reviewNote(), dealer.reviewedBy(),
                        dealer.reviewedAt(), reason.trim(), actorId(), now, dealer.createdBy(), dealer.createdAt(), now, dealer.creditSuspended());
                repository.saveDealer(saved);
                audit.recordSuccess("dealer.deactivated", MANAGE, "dealer", id, null, request.idempotencyKey(), Map.of("reason", reason.trim()));
                return saved;
            }, DealerRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.deactivate_failed", MANAGE, "dealer", id, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 合同 ──

    public PageResponse<DealerContractRecord> listContracts(String dealerId) {
        authorization.requirePermission(VIEW);
        requireDealer(dealerId);
        List<DealerContractRecord> items = repository.listContracts(dealerId);
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    @Transactional
    public DealerContractRecord createContract(String dealerId, DealerContractRequest request) {
        authorization.requirePermission(MANAGE);
        try {
            DealerRecord dealer = requireDealer(dealerId);
            if (!APPROVED.equals(dealer.status())) {
                throw ApiException.conflict("DEALER_NOT_APPROVED", "只有准入已通过的经销商可以登记经销合同");
            }
            String no = requireText(request == null ? null : request.contractNo(), "contract_no");
            String entity = requireText(request.salesEntityId(), "sales_entity_id");
            String warehouse = requireText(request.centralWarehouseId(), "central_warehouse_id");
            if (request.validFrom() == null || request.validTo() == null || request.validTo().isBefore(request.validFrom())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "合同起止日期必填且结束不得早于开始");
            }
            if (!repository.businessEntityExists(entity)) {
                throw ApiException.notFound("SALES_ENTITY_NOT_FOUND", "销售主体不存在");
            }
            if (!repository.warehouseExists(warehouse)) {
                throw ApiException.notFound("WAREHOUSE_NOT_FOUND", "中心仓不存在或已归档");
            }
            return idempotency.execute("dealerContractCreate:" + dealerId, request.idempotencyKey(), request, () -> {
                DealerContractRecord contract = new DealerContractRecord("dct_" + UUID.randomUUID(), dealerId, no.trim(), entity, warehouse,
                        request.validFrom(), request.validTo(), "active", actorId(), Instant.now());
                repository.saveContract(contract);
                audit.recordSuccess("dealer.contract_created", MANAGE, "dealer_contract", contract.id(), null, request.idempotencyKey(),
                        Map.of("dealer_id", dealerId, "contract_no", contract.contractNo(), "sales_entity_id", entity, "central_warehouse_id", warehouse));
                return contract;
            }, DealerContractRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.contract_create_failed", MANAGE, "dealer_contract", dealerId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 价格目录 ──

    public PageResponse<DealerPriceCatalogRecord> listCatalogs(String contractId) {
        authorization.requirePermission(VIEW);
        requireContract(contractId);
        List<DealerPriceCatalogRecord> items = repository.listCatalogs(contractId);
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    public PageResponse<DealerPriceCatalogLineRecord> listCatalogLines(String catalogId) {
        authorization.requirePermission(VIEW);
        requireCatalog(catalogId);
        List<DealerPriceCatalogLineRecord> items = repository.listCatalogLines(catalogId);
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    /** 新建草稿版本（版本号 = 该合同最大版本 + 1）；明细为合同单价，不读取内部结算价。 */
    @Transactional
    public DealerPriceCatalogRecord createCatalogDraft(String contractId, DealerCatalogDraftRequest request) {
        authorization.requirePermission(CATALOG_EDIT);
        try {
            DealerContractRecord contract = requireContract(contractId);
            requireActiveDealer(contract.dealerId());
            if (request == null || request.lines() == null || request.lines().isEmpty()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "价格目录至少包含一条商品价格");
            }
            for (DealerCatalogLineRequest line : request.lines()) {
                if (line == null || line.itemId() == null || line.itemId().isBlank() || line.unitPriceCent() == null || line.unitPriceCent() <= 0) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "每条价格须填写商品与大于 0 的合同单价（分）");
                }
                if (repository.findItem(line.itemId()).isEmpty()) {
                    throw ApiException.notFound("ITEM_NOT_FOUND", "库存商品不存在：" + line.itemId());
                }
            }
            return idempotency.execute("dealerCatalogDraft:" + contractId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerPriceCatalogRecord catalog = new DealerPriceCatalogRecord("dpc_" + UUID.randomUUID(), contractId, repository.nextCatalogVersion(contractId),
                        "draft", actorId(), null, null, null, null, null, null, trim(request.note()), now, now);
                repository.saveCatalog(catalog);
                for (DealerCatalogLineRequest line : request.lines()) {
                    repository.saveCatalogLine(new DealerPriceCatalogLineRecord("dpl_" + UUID.randomUUID(), catalog.id(), line.itemId().trim(),
                            line.spec() == null ? "" : line.spec().trim(), line.unitPriceCent(), line.taxRateBps(), line.expectedRestockDate()));
                }
                audit.recordSuccess("dealer.price_catalog.drafted", CATALOG_EDIT, "dealer_price_catalog", catalog.id(), null, request.idempotencyKey(),
                        Map.of("contract_id", contractId, "version", catalog.version(), "line_count", request.lines().size()));
                return catalog;
            }, DealerPriceCatalogRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.price_catalog.draft_failed", CATALOG_EDIT, "dealer_price_catalog", contractId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 草稿 → 待审批。 */
    @Transactional
    public DealerPriceCatalogRecord submitCatalog(String catalogId, String idempotencyKey) {
        authorization.requirePermission(CATALOG_EDIT);
        try {
            DealerPriceCatalogRecord catalog = requireCatalog(catalogId);
            if (!"draft".equals(catalog.status())) {
                throw ApiException.conflict("CATALOG_STATUS_INVALID", "只有草稿目录可以提交审批");
            }
            return idempotency.execute("dealerCatalogSubmit:" + catalogId, idempotencyKey, Map.of("id", catalogId), () -> {
                Instant now = Instant.now();
                DealerPriceCatalogRecord saved = new DealerPriceCatalogRecord(catalog.id(), catalog.dealerContractId(), catalog.version(), "pending_approval",
                        catalog.editedBy(), actorId(), now, null, null, null, null, catalog.note(), catalog.createdAt(), now);
                repository.saveCatalog(saved);
                audit.recordSuccess("dealer.price_catalog.submitted", CATALOG_EDIT, "dealer_price_catalog", catalogId, null, idempotencyKey,
                        Map.of("version", catalog.version()));
                return saved;
            }, DealerPriceCatalogRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.price_catalog.submit_failed", CATALOG_EDIT, "dealer_price_catalog", catalogId, null, idempotencyKey, codeOf(e));
            throw e;
        }
    }

    /** 待审批 → 已生效（审批人不得为编辑人或提交人；旧版本同时失效）/ 草稿（驳回）。 */
    @Transactional
    public DealerPriceCatalogRecord decideCatalog(String catalogId, DealerCatalogDecisionRequest request) {
        authorization.requirePermission(CATALOG_APPROVE);
        try {
            DealerPriceCatalogRecord catalog = requireCatalog(catalogId);
            if (!"pending_approval".equals(catalog.status())) {
                throw ApiException.conflict("CATALOG_STATUS_INVALID", "只有待审批目录可以审批");
            }
            String decision = requireDecision(request == null ? null : request.decision());
            String actor = actorId();
            if (Objects.equals(actor, catalog.editedBy()) || Objects.equals(actor, catalog.submittedBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "合同价格编辑人与审批人不得为同一人");
            }
            return idempotency.execute("dealerCatalogDecide:" + catalogId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerPriceCatalogRecord saved;
                if ("approve".equals(decision)) {
                    Optional<DealerPriceCatalogRecord> current = repository.findEffectiveCatalog(catalog.dealerContractId());
                    current.ifPresent(prev -> repository.saveCatalog(new DealerPriceCatalogRecord(prev.id(), prev.dealerContractId(), prev.version(), "expired",
                            prev.editedBy(), prev.submittedBy(), prev.submittedAt(), prev.approvedBy(), prev.approvedAt(), prev.effectiveAt(), now, prev.note(),
                            prev.createdAt(), now)));
                    saved = new DealerPriceCatalogRecord(catalog.id(), catalog.dealerContractId(), catalog.version(), "effective", catalog.editedBy(),
                            catalog.submittedBy(), catalog.submittedAt(), actor, now, now, null, catalog.note(), catalog.createdAt(), now);
                } else {
                    saved = new DealerPriceCatalogRecord(catalog.id(), catalog.dealerContractId(), catalog.version(), "draft", catalog.editedBy(),
                            null, null, null, null, null, null, catalog.note(), catalog.createdAt(), now);
                }
                repository.saveCatalog(saved);
                audit.recordSuccess("dealer.price_catalog.decided", CATALOG_APPROVE, "dealer_price_catalog", catalogId, null, request.idempotencyKey(),
                        Map.of("decision", decision, "status", saved.status(), "version", saved.version(), "note", nz(request.note())));
                return saved;
            }, DealerPriceCatalogRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.price_catalog.decide_failed", CATALOG_APPROVE, "dealer_price_catalog", catalogId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    // ── 询价 ──

    public PageResponse<DealerInquiryRecord> listInquiries(String dealerId) {
        authorization.requirePermission(VIEW);
        requireDealer(dealerId);
        List<DealerInquiryRecord> items = repository.listInquiries(dealerId);
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    /** 未定价商品才能询价（BSM:1220）；经销商须已通过且未停用。 */
    @Transactional
    public DealerInquiryRecord createInquiry(String dealerId, DealerInquiryRequest request) {
        authorization.requirePermission(INQUIRY_MANAGE);
        try {
            requireActiveDealer(dealerId);
            String contractId = requireText(request == null ? null : request.dealerContractId(), "dealer_contract_id");
            DealerContractRecord contract = requireContract(contractId);
            if (!contract.dealerId().equals(dealerId)) {
                throw ApiException.badRequest("CONTRACT_DEALER_MISMATCH", "合同不属于该经销商");
            }
            String itemId = requireText(request.itemId(), "item_id");
            if (request.quantity() == null || request.quantity() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "询价数量须大于 0");
            }
            if (repository.findItem(itemId).isEmpty()) {
                throw ApiException.notFound("ITEM_NOT_FOUND", "库存商品不存在");
            }
            String spec = request.spec() == null ? "" : request.spec().trim();
            if (repository.effectivePriceExists(contractId, itemId, spec)) {
                throw ApiException.conflict("ITEM_ALREADY_PRICED", "该商品已定合同价，可直接下单，无需询价");
            }
            return idempotency.execute("dealerInquiryCreate:" + dealerId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerInquiryRecord inquiry = new DealerInquiryRecord("dqi_" + UUID.randomUUID(), dealerId, contractId, itemId, spec, request.quantity(),
                        "pending_quote", null, null, null, null, null, null, trim(request.note()), actorId(), now, now);
                repository.saveInquiry(inquiry);
                audit.recordSuccess("dealer.inquiry.created", INQUIRY_MANAGE, "dealer_inquiry", inquiry.id(), null, request.idempotencyKey(),
                        Map.of("dealer_id", dealerId, "contract_id", contractId, "item_id", itemId, "quantity", request.quantity()));
                return inquiry;
            }, DealerInquiryRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.inquiry.create_failed", INQUIRY_MANAGE, "dealer_inquiry", dealerId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 待报价 → 报价审批中。 */
    @Transactional
    public DealerInquiryRecord quoteInquiry(String inquiryId, DealerQuoteRequest request) {
        authorization.requirePermission(INQUIRY_QUOTE);
        try {
            DealerInquiryRecord inquiry = requireInquiry(inquiryId);
            if (!"pending_quote".equals(inquiry.status())) {
                throw ApiException.conflict("INQUIRY_STATUS_INVALID", "只有待报价的询价可以报价");
            }
            if (request == null || request.quotedPriceCent() == null || request.quotedPriceCent() <= 0 || request.quoteValidUntil() == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "报价须填写大于 0 的单价（分）与报价有效期");
            }
            return idempotency.execute("dealerInquiryQuote:" + inquiryId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                DealerInquiryRecord saved = new DealerInquiryRecord(inquiry.id(), inquiry.dealerId(), inquiry.dealerContractId(), inquiry.itemId(), inquiry.spec(),
                        inquiry.quantity(), "quote_approval", request.quotedPriceCent(), actorId(), now, request.quoteValidUntil(), null, null,
                        request.note() == null ? inquiry.note() : request.note().trim(), inquiry.createdBy(), inquiry.createdAt(), now);
                repository.saveInquiry(saved);
                audit.recordSuccess("dealer.inquiry.quoted", INQUIRY_QUOTE, "dealer_inquiry", inquiryId, null, request.idempotencyKey(),
                        Map.of("quoted_price_cent", request.quotedPriceCent(), "quote_valid_until", request.quoteValidUntil().toString()));
                return saved;
            }, DealerInquiryRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.inquiry.quote_failed", INQUIRY_QUOTE, "dealer_inquiry", inquiryId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 报价审批中 → 已报价（审批人不得为报价人）/ 待报价（驳回）。 */
    @Transactional
    public DealerInquiryRecord decideQuote(String inquiryId, DealerQuoteDecisionRequest request) {
        authorization.requirePermission(INQUIRY_APPROVE);
        try {
            DealerInquiryRecord inquiry = requireInquiry(inquiryId);
            if (!"quote_approval".equals(inquiry.status())) {
                throw ApiException.conflict("INQUIRY_STATUS_INVALID", "只有报价审批中的询价可以审批");
            }
            String decision = requireDecision(request == null ? null : request.decision());
            String actor = actorId();
            if (Objects.equals(actor, inquiry.quotedBy())) {
                throw ApiException.forbidden("DEALER_SOD_SAME_PERSON", "报价人与报价审批人不得为同一人");
            }
            return idempotency.execute("dealerInquiryDecide:" + inquiryId, request.idempotencyKey(), request, () -> {
                Instant now = Instant.now();
                boolean approve = "approve".equals(decision);
                DealerInquiryRecord saved = new DealerInquiryRecord(inquiry.id(), inquiry.dealerId(), inquiry.dealerContractId(), inquiry.itemId(), inquiry.spec(),
                        inquiry.quantity(), approve ? "quoted" : "pending_quote", approve ? inquiry.quotedPriceCent() : null, approve ? inquiry.quotedBy() : null,
                        approve ? inquiry.quotedAt() : null, approve ? inquiry.quoteValidUntil() : null, approve ? actor : null, approve ? now : null,
                        request.note() == null ? inquiry.note() : request.note().trim(), inquiry.createdBy(), inquiry.createdAt(), now);
                repository.saveInquiry(saved);
                audit.recordSuccess("dealer.inquiry.decided", INQUIRY_APPROVE, "dealer_inquiry", inquiryId, null, request.idempotencyKey(),
                        Map.of("decision", decision, "status", saved.status(), "note", nz(request.note())));
                return saved;
            }, DealerInquiryRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.inquiry.decide_failed", INQUIRY_APPROVE, "dealer_inquiry", inquiryId, null, request == null ? null : request.idempotencyKey(), codeOf(e));
            throw e;
        }
    }

    /** 已报价 → 已失效（报价过期或撤回）。 */
    @Transactional
    public DealerInquiryRecord expireInquiry(String inquiryId, String idempotencyKey) {
        authorization.requirePermission(INQUIRY_QUOTE);
        try {
            DealerInquiryRecord inquiry = requireInquiry(inquiryId);
            if (!"quoted".equals(inquiry.status())) {
                throw ApiException.conflict("INQUIRY_STATUS_INVALID", "只有已报价的询价可以置为已失效");
            }
            return idempotency.execute("dealerInquiryExpire:" + inquiryId, idempotencyKey, Map.of("id", inquiryId), () -> {
                Instant now = Instant.now();
                DealerInquiryRecord saved = new DealerInquiryRecord(inquiry.id(), inquiry.dealerId(), inquiry.dealerContractId(), inquiry.itemId(), inquiry.spec(),
                        inquiry.quantity(), "expired", inquiry.quotedPriceCent(), inquiry.quotedBy(), inquiry.quotedAt(), inquiry.quoteValidUntil(),
                        inquiry.approvedBy(), inquiry.approvedAt(), inquiry.note(), inquiry.createdBy(), inquiry.createdAt(), now);
                repository.saveInquiry(saved);
                audit.recordSuccess("dealer.inquiry.expired", INQUIRY_QUOTE, "dealer_inquiry", inquiryId, null, idempotencyKey, Map.of());
                return saved;
            }, DealerInquiryRecord.class);
        } catch (RuntimeException e) {
            audit.recordFailure("dealer.inquiry.expire_failed", INQUIRY_QUOTE, "dealer_inquiry", inquiryId, null, idempotencyKey, codeOf(e));
            throw e;
        }
    }

    // ── 库存外显 ──

    /**
     * BSM:1221「外部可以看到允许销售的库存商品及有货、紧张、缺货、预计交期，不得看到精确数量、仓库总库存或内部调拨信息」：
     * 允许销售 = 现行生效目录上的商品；按合同中心仓可用量分档（技术决定：紧张 = 可用量 > 0 且不高于物品档案最低库存 min_quantity；
     * 缺货且目录给了预计交期 → 预计交期）。
     */
    public PageResponse<DealerExternalStockRecord> externalStock(String contractId) {
        authorization.requirePermission(VIEW);
        DealerContractRecord contract = requireContract(contractId);
        List<DealerExternalStockRecord> rows = new ArrayList<>();
        Optional<DealerPriceCatalogRecord> effective = repository.findEffectiveCatalog(contractId);
        if (effective.isPresent()) {
            for (DealerPriceCatalogLineRecord line : repository.listCatalogLines(effective.get().id())) {
                JdbcDealerRepository.ItemInfo item = repository.findItem(line.itemId()).orElse(new JdbcDealerRepository.ItemInfo(line.itemId(), line.itemId(), 0));
                int available = repository.availableInWarehouse(line.itemId(), contract.centralWarehouseId());
                String availability;
                if (available <= 0) {
                    availability = line.expectedRestockDate() != null ? DealerExternalStockRecord.EXPECTED_RESTOCK : DealerExternalStockRecord.OUT_OF_STOCK;
                } else if (available <= item.minQuantity()) {
                    availability = DealerExternalStockRecord.LOW;
                } else {
                    availability = DealerExternalStockRecord.IN_STOCK;
                }
                rows.add(new DealerExternalStockRecord(line.itemId(), item.name(), line.spec(), line.unitPriceCent(), availability,
                        DealerExternalStockRecord.EXPECTED_RESTOCK.equals(availability) ? line.expectedRestockDate() : null));
            }
        }
        audit.recordSuccess("dealer.external_stock.viewed", VIEW, "dealer_contract", contractId, null, null, Map.of("count", rows.size()));
        return new PageResponse<>(rows, 1, Math.max(rows.size(), 1), rows.size());
    }

    // ── helpers ──

    private DealerRecord requireDealer(String id) {
        DealerRecord dealer = repository.findDealer(requireText(id, "dealer_id"))
                .orElseThrow(() -> ApiException.notFound("DEALER_NOT_FOUND", "经销商不存在"));
        String scope = projectScope();
        if (scope != null && dealer.projectId() != null && !scope.equals(dealer.projectId())) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "无权访问其他项目的经销商");
        }
        return dealer;
    }

    private DealerRecord requireActiveDealer(String id) {
        DealerRecord dealer = requireDealer(id);
        if (DEACTIVATED.equals(dealer.status())) {
            throw ApiException.conflict("DEALER_DEACTIVATED", "经销商已停用，禁止新询价、新目录与新交易");
        }
        if (!APPROVED.equals(dealer.status())) {
            throw ApiException.conflict("DEALER_NOT_APPROVED", "经销商准入尚未通过");
        }
        return dealer;
    }

    private DealerContractRecord requireContract(String id) {
        DealerContractRecord contract = repository.findContract(requireText(id, "contract_id"))
                .orElseThrow(() -> ApiException.notFound("DEALER_CONTRACT_NOT_FOUND", "经销合同不存在"));
        requireDealer(contract.dealerId());
        return contract;
    }

    private DealerPriceCatalogRecord requireCatalog(String id) {
        DealerPriceCatalogRecord catalog = repository.findCatalog(requireText(id, "catalog_id"))
                .orElseThrow(() -> ApiException.notFound("PRICE_CATALOG_NOT_FOUND", "价格目录不存在"));
        requireContract(catalog.dealerContractId());
        return catalog;
    }

    private DealerInquiryRecord requireInquiry(String id) {
        DealerInquiryRecord inquiry = repository.findInquiry(requireText(id, "inquiry_id"))
                .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND", "询价不存在"));
        requireDealer(inquiry.dealerId());
        return inquiry;
    }

    private static DealerRecord withStatus(DealerRecord d, String status, boolean cp, boolean ct, boolean inv, boolean bank, boolean portal,
            String note, String reviewedBy, Instant reviewedAt) {
        return new DealerRecord(d.id(), d.projectId(), d.dealerName(), d.unifiedSocialCreditCode(), d.legalRepresentative(), d.contactName(), d.contactPhone(),
                status, cp, ct, inv, bank, portal, note, reviewedBy, reviewedAt, d.deactivationReason(), d.deactivatedBy(), d.deactivatedAt(),
                d.createdBy(), d.createdAt(), Instant.now(), d.creditSuspended());
    }

    private static String requireDecision(String decision) {
        String value = requireText(decision, "decision");
        if (!List.of("approve", "reject").contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论须为通过（approve）或驳回（reject）");
        }
        return value;
    }

    private String projectScope() {
        return authorization.currentProjectScopeQuietly().orElse(null);
    }

    private String actorId() {
        AccessActor actor = authorization.currentActor();
        return actor == null ? null : actor.id();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String codeOf(RuntimeException e) {
        return e instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
    }

    static Map<String, Object> meta(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    static LocalDate today() {
        return LocalDate.now();
    }
}
