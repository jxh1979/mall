package com.complex.platform.ops.productionreadiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckRequest;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemUpdateRequest;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductionReadinessServiceTests {

    private InMemoryProductionReadinessRepository readinessRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private ProductionReadinessService service;

    @BeforeEach
    void setUp() {
        readinessRepository = new InMemoryProductionReadinessRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        IdempotencyService idempotencyService = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
        AuthorizationService authorizationService = new AuthorizationService(mock(AccessRepository.class));
        service = new ProductionReadinessService(
                readinessRepository,
                idempotencyService,
                new AuditEventService(auditEventRepository),
                authorizationService
        );
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void listItemsReturnsDefaultTwelveItemsAndWritesAuditEvent() {
        var page = service.listItems();

        assertThat(page.items()).hasSize(12);
        assertThat(page.total()).isEqualTo(12);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("production_readiness.viewed");
    }

    @Test
    void updateMissingItemFailsWithBusinessCodeAndAuditEvent() {
        assertThatThrownBy(() -> service.updateItem("missing", updateRequest("missing-key", ProductionReadinessStatus.PASSED, null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PRODUCTION_READINESS_ITEM_NOT_FOUND"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("production_readiness.update_failed");
    }

    @Test
    void updateRejectsPlaintextSecretLikeCredentialRef() {
        ProductionReadinessItemResponse item = service.listItems().items().getFirst();

        assertThatThrownBy(() -> service.updateItem(item.id(), updateRequest("secret-key", ProductionReadinessStatus.PASSED, "sk-live-plain-secret")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("SECRET_VALUE_REJECTED"));
    }

    @Test
    void checkAllReturnsBlockedWhenAnyBlockingItemIsNotPassed() {
        service.listItems();

        var response = service.createCheck(new ProductionReadinessCheckRequest("check-blocked", "all", "preflight"));

        assertThat(response.result()).isEqualTo("blocked");
        assertThat(response.blockerCount()).isGreaterThan(0);
        assertThat(response.blockers())
                .extracting(ProductionReadinessItemResponse::status)
                .contains(ProductionReadinessStatus.PENDING_INFO);
    }

    @Test
    void checkAllReturnsPassedWhenCriticalItemsArePassedWaivedOrNotApplicable() {
        List<ProductionReadinessItemResponse> items = service.listItems().items();
        for (int index = 0; index < items.size(); index++) {
            ProductionReadinessItemResponse item = items.get(index);
            ProductionReadinessStatus status = switch (index % 3) {
                case 0 -> ProductionReadinessStatus.PASSED;
                case 1 -> ProductionReadinessStatus.WAIVED;
                default -> ProductionReadinessStatus.NOT_APPLICABLE;
            };
            service.updateItem(item.id(), updateRequest("pass-" + index, status, null));
        }

        var response = service.createCheck(new ProductionReadinessCheckRequest("check-passed", "all", "ready"));

        assertThat(response.result()).isEqualTo("passed");
        assertThat(response.blockerCount()).isZero();
        assertThat(response.blockers()).isEmpty();
    }

    @Test
    void duplicateIdempotencyKeyWithSameRequestReturnsFirstResult() {
        ProductionReadinessItemResponse item = service.listItems().items().getFirst();
        ProductionReadinessItemUpdateRequest request = updateRequest("same-key", ProductionReadinessStatus.PASSED, "vault://complex/payment");

        ProductionReadinessItemResponse first = service.updateItem(item.id(), request);
        ProductionReadinessItemResponse second = service.updateItem(item.id(), request);

        assertThat(second).isEqualTo(first);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "production_readiness.updated".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void duplicateIdempotencyKeyWithDifferentRequestReturnsConflict() {
        ProductionReadinessItemResponse item = service.listItems().items().getFirst();
        service.updateItem(item.id(), updateRequest("conflict-key", ProductionReadinessStatus.PASSED, null));

        assertThatThrownBy(() -> service.updateItem(item.id(), updateRequest("conflict-key", ProductionReadinessStatus.FAILED, null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void updateItemRecordsActualActorAsUpdatedBy() {
        ProductionReadinessItemResponse item = service.listItems().items().getFirst();
        ProductionReadinessItemResponse updated = service.updateItem(item.id(),
                updateRequest("actor-test-key", ProductionReadinessStatus.PASSED, null));

        assertThat(updated.updatedBy()).isNotEqualTo("staff-token");
        assertThat(updated.updatedBy()).isEqualTo("usr_admin");
    }

    @Test
    void rejectSecretLikeCredentialRefWithEmbeddedNewline() {
        service.listItems();
        ProductionReadinessItemResponse item = service.listItems().items().getFirst();

        assertThatThrownBy(() -> service.updateItem(item.id(),
                updateRequest("newline-secret-key", ProductionReadinessStatus.PASSED, "password=\nsecret123")))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.code()).isEqualTo("SECRET_VALUE_REJECTED"));
    }

    private ProductionReadinessItemUpdateRequest updateRequest(
            String idempotencyKey,
            ProductionReadinessStatus status,
            String credentialRef
    ) {
        return new ProductionReadinessItemUpdateRequest(
                idempotencyKey,
                status,
                "IT",
                "ops-owner",
                "supplier",
                LocalDate.parse("2026-06-30"),
                "runbook://evidence",
                Instant.parse("2026-06-13T00:00:00Z"),
                "production readiness",
                "mock only",
                credentialRef,
                "runbook://production-readiness",
                "finish verification",
                "test update",
                List.of("att-1")
        );
    }

    private static final class InMemoryProductionReadinessRepository implements ProductionReadinessRepository {
        private final Map<String, ProductionReadinessItem> items = new LinkedHashMap<>();
        private final List<ProductionReadinessCheck> checks = new ArrayList<>();

        @Override
        public List<ProductionReadinessItem> findAllItems() {
            return new ArrayList<>(items.values());
        }

        @Override
        public List<ProductionReadinessItem> findItemsByCategory(String category) {
            return items.values().stream()
                    .filter(item -> item.category().equals(category))
                    .toList();
        }

        @Override
        public Optional<ProductionReadinessItem> findItemById(String id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public void saveDefaultItems(List<ProductionReadinessItem> defaultItems) {
            defaultItems.forEach(item -> items.putIfAbsent(item.id(), item));
        }

        @Override
        public ProductionReadinessItem saveItem(ProductionReadinessItem item) {
            items.put(item.id(), item);
            return item;
        }

        @Override
        public ProductionReadinessCheck saveCheck(ProductionReadinessCheck check, List<ProductionReadinessItem> blockers) {
            checks.add(check);
            return check;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
