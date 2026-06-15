package com.complex.platform.ops.productionreadiness;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckRequest;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemUpdateRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ProductionReadinessService {

    private static final String BUSINESS_TYPE = "production_readiness_item";
    private static final String UPDATED_BY = "staff-token";
    private static final Set<String> CHECK_SCOPES = Set.of(
            "all",
            "mdm",
            "payment",
            "age_verification",
            "nas_backup",
            "credential_rotation",
            "wecom",
            "third_party",
            "server_160",
            "https_domain"
    );
    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i).*(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)\\s*[:=].+"),
            Pattern.compile("(?i).*(sk-live|sk_test|sk-proj|AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----).*"),
            Pattern.compile("(?i).*bearer\\s+[a-z0-9._\\-]{20,}.*")
    );

    private final ProductionReadinessRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public ProductionReadinessService(
            ProductionReadinessRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public PageResponse<ProductionReadinessItemResponse> listItems() {
        try {
            ensureDefaultItems();
            List<ProductionReadinessItemResponse> items = repository.findAllItems().stream()
                    .map(this::toResponse)
                    .toList();
            auditEventService.recordSuccess(
                    "production_readiness.viewed",
                    "ops.production_readiness.view",
                    BUSINESS_TYPE,
                    "all",
                    requestId(),
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, 1, 50, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "production_readiness.view_failed",
                    "ops.production_readiness.view",
                    BUSINESS_TYPE,
                    "all",
                    requestId(),
                    null,
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public ProductionReadinessItemResponse updateItem(String id, ProductionReadinessItemUpdateRequest request) {
        try {
            return idempotencyService.execute(
                    "updateProductionReadinessItem",
                    request == null ? null : request.idempotencyKey(),
                    new UpdateFingerprint(id, request),
                    () -> updateItemOnce(id, request),
                    ProductionReadinessItemResponse.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "production_readiness.update_failed",
                    "ops.production_readiness.manage",
                    BUSINESS_TYPE,
                    id,
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    public ProductionReadinessCheckResponse createCheck(ProductionReadinessCheckRequest request) {
        try {
            return idempotencyService.execute(
                    "createProductionReadinessCheck",
                    request == null ? null : request.idempotencyKey(),
                    request,
                    () -> createCheckOnce(request),
                    ProductionReadinessCheckResponse.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "production_readiness.check_failed",
                    "ops.production_readiness.check",
                    "production_readiness_check",
                    request == null ? null : request.checkScope(),
                    requestId(),
                    request == null ? null : request.idempotencyKey(),
                    errorCode(exception)
            );
            throw exception;
        }
    }

    private ProductionReadinessItemResponse updateItemOnce(String id, ProductionReadinessItemUpdateRequest request) {
        validateUpdateRequest(request);
        ensureDefaultItems();
        ProductionReadinessItem existing = repository.findItemById(id)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCTION_READINESS_ITEM_NOT_FOUND",
                        "production readiness item not found"
                ));
        rejectSecretLikeCredentialRef(request.credentialRef());

        ProductionReadinessItem saved = repository.saveItem(existing.updateFrom(request, Instant.now(), UPDATED_BY));
        auditEventService.recordSuccess(
                "production_readiness.updated",
                "ops.production_readiness.manage",
                BUSINESS_TYPE,
                id,
                requestId(),
                request.idempotencyKey(),
                Map.of("status", saved.status().code())
        );
        return toResponse(saved);
    }

    private ProductionReadinessCheckResponse createCheckOnce(ProductionReadinessCheckRequest request) {
        validateCheckRequest(request);
        ensureDefaultItems();
        List<ProductionReadinessItem> checkedItems = "all".equals(request.checkScope())
                ? repository.findAllItems()
                : repository.findItemsByCategory(request.checkScope());
        List<ProductionReadinessItem> blockers = checkedItems.stream()
                .filter(this::isBlockingForCodingAcceptance)
                .toList();
        ProductionReadinessCheck check = new ProductionReadinessCheck(
                UUID.randomUUID(),
                request.checkScope(),
                blockers.isEmpty() ? "passed" : "blocked",
                blockers.size(),
                request.operatorNote(),
                Instant.now(),
                UPDATED_BY,
                request.idempotencyKey()
        );
        repository.saveCheck(check, blockers);
        auditEventService.recordSuccess(
                "production_readiness.checked",
                "ops.production_readiness.check",
                "production_readiness_check",
                request.checkScope(),
                requestId(),
                request.idempotencyKey(),
                Map.of("result", check.result(), "blocker_count", check.blockerCount())
        );
        return toCheckResponse(check, blockers);
    }

    private void validateUpdateRequest(ProductionReadinessItemUpdateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        if (request.status() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "status is required");
        }
        if (request.ownerDepartment() == null || request.ownerDepartment().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "owner_department is required");
        }
        if (request.ownerPerson() == null || request.ownerPerson().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "owner_person is required");
        }
        if (request.targetDate() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "target_date is required");
        }
        if (request.blockingScope() == null || request.blockingScope().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "blocking_scope is required");
        }
        if (request.fallbackMode() == null || request.fallbackMode().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "fallback_mode is required");
        }
    }

    private void validateCheckRequest(ProductionReadinessCheckRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        if (request.checkScope() == null || !CHECK_SCOPES.contains(request.checkScope())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "check_scope is invalid");
        }
    }

    private void rejectSecretLikeCredentialRef(String credentialRef) {
        if (credentialRef == null || credentialRef.isBlank()) {
            return;
        }
        for (Pattern pattern : SECRET_PATTERNS) {
            if (pattern.matcher(credentialRef).matches()) {
                throw ApiException.badRequest("SECRET_VALUE_REJECTED", "credential_ref must be a reference, not a secret value");
            }
        }
    }

    private boolean isBlockingForCodingAcceptance(ProductionReadinessItem item) {
        if ("https_domain".equals(item.category())) {
            return false;
        }
        return item.status().isBlocking();
    }

    private void ensureDefaultItems() {
        if (repository.findAllItems().isEmpty()) {
            repository.saveDefaultItems(defaultItems());
        }
    }

    private List<ProductionReadinessItem> defaultItems() {
        Instant now = Instant.now();
        LocalDate targetDate = LocalDate.of(2026, 6, 30);
        return List.of(
                defaultItem("mdm-device-enrollment", "mdm", "MDM device enrollment", "Use local/test terminal flow until Android Enterprise data is complete", targetDate, now),
                defaultItem("mdm-policy-push", "mdm", "MDM policy push", "Record supplier prerequisites and keep smoke devices on controlled local profile", targetDate, now),
                defaultItem("payment-channel-contract", "payment", "Payment channel contract", "Use mock/offline registration until production payment credentials are approved", targetDate, now),
                defaultItem("payment-callback-reconciliation", "payment", "Payment callback reconciliation", "Keep callback and reconciliation in mock mode until provider callback data is available", targetDate, now),
                defaultItem("age-verification-policy", "age_verification", "Age verification policy", "Block real age-gated flow until human review and supplier capability are confirmed", targetDate, now),
                defaultItem("nas-backup-plan", "nas_backup", "NAS backup plan", "Keep documented backup runbook and block production recovery claims until NAS is validated", targetDate, now),
                defaultItem("nas-restore-drill", "nas_backup", "NAS restore drill", "Run restore drill before claiming production recoverability", targetDate, now),
                defaultItem("credential-rotation-runbook", "credential_rotation", "Credential rotation runbook", "Store credential references only and rotate real secrets outside the API", targetDate, now),
                defaultItem("wecom-notification", "wecom", "WeCom notification", "Use internal mock notification until enterprise app material is complete", targetDate, now),
                defaultItem("third-party-provider-mock", "third_party", "Third-party provider mock", "Keep SDK/API calls mocked until supplier contracts and sandboxes are confirmed", targetDate, now),
                defaultItem("server-160-runtime", "server_160", "Server 160 runtime", "Use 160 smoke and environment scripts as coding acceptance gate", targetDate, now),
                defaultItem("https-domain-binding", "https_domain", "HTTPS domain binding", "Track public production HTTPS binding as a production reminder, not an internal coding blocker", targetDate, now)
        );
    }

    private ProductionReadinessItem defaultItem(
            String id,
            String category,
            String itemName,
            String defaultSolution,
            LocalDate targetDate,
            Instant now
    ) {
        return new ProductionReadinessItem(
                id,
                category,
                itemName,
                defaultSolution,
                "IT",
                null,
                null,
                ProductionReadinessStatus.PENDING_INFO,
                targetDate,
                null,
                null,
                defaultSolution,
                "mock only",
                null,
                "runbook://p0-production-readiness",
                "collect supplier/environment evidence",
                now,
                "system"
        );
    }

    private ProductionReadinessItemResponse toResponse(ProductionReadinessItem item) {
        return new ProductionReadinessItemResponse(
                item.id(),
                item.category(),
                item.itemName(),
                item.defaultSolution(),
                item.ownerDepartment(),
                item.ownerPerson(),
                item.supplierName(),
                item.status(),
                item.targetDate(),
                item.evidenceLink(),
                item.lastTestTime(),
                item.blockingScope(),
                item.fallbackMode(),
                item.credentialRef(),
                item.runbookRef(),
                item.nextAction(),
                item.updatedAt(),
                item.updatedBy()
        );
    }

    private ProductionReadinessCheckResponse toCheckResponse(
            ProductionReadinessCheck check,
            List<ProductionReadinessItem> blockers
    ) {
        return new ProductionReadinessCheckResponse(
                check.id().toString(),
                check.checkScope(),
                check.result(),
                check.blockerCount(),
                blockers.stream().map(this::toResponse).toList(),
                check.checkedAt()
        );
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }

    private String requestId() {
        return "req_" + UUID.randomUUID();
    }

    private record UpdateFingerprint(String id, ProductionReadinessItemUpdateRequest request) {
    }
}
