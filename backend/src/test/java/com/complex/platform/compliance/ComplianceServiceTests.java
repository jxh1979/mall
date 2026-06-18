package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
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
import org.mockito.Mockito;

class ComplianceServiceTests {

    private InMemoryComplianceRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private ComplianceService service;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asMerchantUser("merchant_1");
        repository = new InMemoryComplianceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new ComplianceService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                new AuthorizationService(Mockito.mock(AccessRepository.class))
        );
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void createContractKeyDateRequiresCoreFieldsAndWritesFailureAudit() {
        assertThatThrownBy(() -> service.createContractKeyDate(new ContractKeyDateRequest(
                "contract-date-missing",
                "missing merchant",
                List.of(),
                "contract_1",
                " ",
                "contract_end",
                "Contract end",
                LocalDate.parse("2026-07-01"),
                List.of(90, 30, 7),
                "active",
                "staff_legal",
                "manual",
                true
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("compliance.contract_key_date.create_failed");
    }

    @Test
    void createContractKeyDatePersistsReminderDaysEvidenceAndAudit() {
        ContractKeyDateRecord created = service.createContractKeyDate(contractDateRequest("contract-date-create"));

        assertThat(created.id()).startsWith("ckd_");
        assertThat(created.contractId()).isEqualTo("contract_1");
        assertThat(created.merchantId()).isEqualTo("merchant_1");
        assertThat(created.dateType()).isEqualTo("contract_end");
        assertThat(created.reminderDays()).containsExactly(90, 30, 7);
        assertThat(created.evidenceAttachmentIds()).containsExactly("att-contract");
        assertThat(created.humanConfirmed()).isTrue();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("compliance.contract_key_date.created");
    }

    @Test
    void createExpiryItemAndScanGenerateUpcomingAndExpiredBlockingReminders() {
        service.createContractKeyDate(contractDateRequest("contract-date-scan"));
        ComplianceExpiryItemRecord expiredLicense = service.createExpiryItem(new ComplianceExpiryItemRequest(
                "expiry-item-create",
                "record expired license",
                List.of("att-license"),
                "merchant",
                "merchant_1",
                "merchant_1",
                "alcohol_license",
                "Alcohol license",
                "LIC-001",
                "Market Authority",
                LocalDate.parse("2025-06-01"),
                LocalDate.parse("2026-06-10"),
                List.of(60, 30, 7),
                "block_operation",
                "active",
                "staff_compliance",
                "manual",
                true
        ));

        ComplianceReminderScanResult result = service.scanReminders(new ComplianceReminderScanRequest(
                "scan-20260614",
                "daily scan",
                LocalDate.parse("2026-06-14"),
                30,
                "staff_ops"
        ));

        assertThat(result.generatedCount()).isEqualTo(2);
        assertThat(result.reminders())
                .extracting(ComplianceReminderRecord::sourceType, ComplianceReminderRecord::reminderType)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("contract_key_date", "upcoming"),
                        org.assertj.core.groups.Tuple.tuple("compliance_expiry_item", "expired")
                );
        assertThat(result.reminders())
                .filteredOn(reminder -> reminder.sourceId().equals(expiredLicense.id()))
                .singleElement()
                .satisfies(reminder -> {
                    assertThat(reminder.severity()).isEqualTo("critical");
                    assertThat(reminder.blockingSuggested()).isTrue();
                    assertThat(reminder.merchantId()).isEqualTo("merchant_1");
                });
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("compliance.reminder.scan_completed");
    }

    @Test
    void duplicateIdempotencyKeyWithSameRequestReturnsFirstResult() {
        ContractKeyDateRequest request = contractDateRequest("contract-date-once");

        ContractKeyDateRecord first = service.createContractKeyDate(request);
        ContractKeyDateRecord second = service.createContractKeyDate(request);

        assertThat(second).isEqualTo(first);
        assertThat(repository.contractDateSaveCount).isEqualTo(1);
    }

    // P0: tenant isolation — list queries must only return current merchant's data
    @Test
    void listContractKeyDatesFiltersToCurrentMerchantScope() {
        service.createContractKeyDate(contractDateRequest("date-scope-m1"));

        PageResponse<ContractKeyDateRecord> forMerchant1 = service.listContractKeyDates();
        assertThat(forMerchant1.items()).hasSize(1);

        TestSecurityActors.asMerchantUser("other_merchant");
        PageResponse<ContractKeyDateRecord> forOther = service.listContractKeyDates();
        assertThat(forOther.items()).isEmpty();
    }

    @Test
    void listExpiryItemsFiltersToCurrentMerchantScope() {
        service.createExpiryItem(expiryItemRequest("expiry-scope-m1"));

        PageResponse<ComplianceExpiryItemRecord> forMerchant1 = service.listExpiryItems();
        assertThat(forMerchant1.items()).hasSize(1);

        TestSecurityActors.asMerchantUser("other_merchant");
        PageResponse<ComplianceExpiryItemRecord> forOther = service.listExpiryItems();
        assertThat(forOther.items()).isEmpty();
    }

    // P1: merchant ownership must come from caller scope, not the request body
    @Test
    void createContractKeyDateRejectsBodyMerchantIdDifferentFromScope() {
        // caller is merchant_1 (setUp); planting a record under merchant_2 via the body must be rejected
        assertThatThrownBy(() -> service.createContractKeyDate(new ContractKeyDateRequest(
                        "date-xmerchant",
                        "record contract end date",
                        List.of("att-contract"),
                        "contract_1",
                        "merchant_2",
                        "contract_end",
                        "Contract end",
                        LocalDate.parse("2026-06-20"),
                        List.of(30),
                        "active",
                        "staff_legal",
                        "manual",
                        true)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // P0/P1: slug collision — different keys that produce same slug must generate different IDs
    @Test
    void createContractKeyDateWithSlugCollidingKeysUsesUniqueIds() {
        // "create-1" and "create!1" both slug to "create_1" with the old code
        ContractKeyDateRecord first = service.createContractKeyDate(contractDateRequest("create-1"));
        ContractKeyDateRecord second = service.createContractKeyDate(contractDateRequest("create!1"));

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(repository.findContractKeyDateById(first.id())).isPresent();
        assertThat(repository.findContractKeyDateById(second.id())).isPresent();
    }

    // P1: scan must only touch current merchant's records
    @Test
    void scanRemindersOnlyScansCurrentMerchantRecords() {
        // Record belongs to merchant_1
        service.createContractKeyDate(contractDateRequest("date-scan-scope"));

        // Switch to a different merchant
        TestSecurityActors.asMerchantUser("other_merchant");

        ComplianceReminderScanResult result = service.scanReminders(new ComplianceReminderScanRequest(
                "scan-scope-other", "scan other merchant", LocalDate.parse("2026-06-20"), 30, "staff_ops"
        ));

        assertThat(result.generatedCount()).isEqualTo(0);
    }

    // P1: repeated scans with different idempotency keys must not accumulate duplicate reminders
    @Test
    void scanRemindersDeduplicatesOnRepeatScanWithDifferentIdempotencyKey() {
        service.createContractKeyDate(contractDateRequest("date-dedup"));
        LocalDate scanDate = LocalDate.parse("2026-06-20");

        ComplianceReminderScanResult first = service.scanReminders(new ComplianceReminderScanRequest(
                "scan-dedup-1", "first scan", scanDate, 30, "staff_ops"
        ));

        service.scanReminders(new ComplianceReminderScanRequest(
                "scan-dedup-2", "second scan", scanDate, 30, "staff_ops"
        ));

        // Total reminders must not exceed first scan count — second scan upserted, not inserted
        assertThat(repository.findAllReminders()).hasSameSizeAs(first.reminders());
    }

    private ContractKeyDateRequest contractDateRequest(String idempotencyKey) {
        return new ContractKeyDateRequest(
                idempotencyKey,
                "record contract end date",
                List.of("att-contract"),
                "contract_1",
                "merchant_1",
                "contract_end",
                "Contract end",
                LocalDate.parse("2026-06-20"),
                List.of(90, 30, 7),
                "active",
                "staff_legal",
                "manual",
                true
        );
    }

    private ComplianceExpiryItemRequest expiryItemRequest(String idempotencyKey) {
        return new ComplianceExpiryItemRequest(
                idempotencyKey,
                "record expiry",
                List.of(),
                "merchant",
                "merchant_1",
                "merchant_1",
                "business_license",
                "Business License",
                null,
                null,
                null,
                LocalDate.parse("2027-01-01"),
                List.of(30),
                "remind_only",
                "active",
                "staff_compliance",
                "manual",
                true
        );
    }

    static final class InMemoryComplianceRepository implements ComplianceRepository {
        private final Map<String, ContractKeyDateRecord> contractKeyDates = new LinkedHashMap<>();
        private final Map<String, ComplianceExpiryItemRecord> expiryItems = new LinkedHashMap<>();
        private final List<ComplianceReminderRecord> reminders = new ArrayList<>();
        int contractDateSaveCount;

        @Override
        public List<ContractKeyDateRecord> findAllContractKeyDates() {
            return new ArrayList<>(contractKeyDates.values());
        }

        @Override
        public List<ContractKeyDateRecord> findContractKeyDatesByMerchant(String merchantId) {
            return contractKeyDates.values().stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public Optional<ContractKeyDateRecord> findContractKeyDateById(String id) {
            return Optional.ofNullable(contractKeyDates.get(id));
        }

        @Override
        public ContractKeyDateRecord saveContractKeyDate(ContractKeyDateRecord record) {
            contractKeyDates.put(record.id(), record);
            contractDateSaveCount++;
            return record;
        }

        @Override
        public List<ComplianceExpiryItemRecord> findAllExpiryItems() {
            return new ArrayList<>(expiryItems.values());
        }

        @Override
        public List<ComplianceExpiryItemRecord> findExpiryItemsByMerchant(String merchantId) {
            return expiryItems.values().stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public Optional<ComplianceExpiryItemRecord> findExpiryItemById(String id) {
            return Optional.ofNullable(expiryItems.get(id));
        }

        @Override
        public ComplianceExpiryItemRecord saveExpiryItem(ComplianceExpiryItemRecord record) {
            expiryItems.put(record.id(), record);
            return record;
        }

        @Override
        public List<ComplianceReminderRecord> findAllReminders() {
            return new ArrayList<>(reminders);
        }

        @Override
        public List<ComplianceReminderRecord> findRemindersByMerchant(String merchantId) {
            return reminders.stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public ComplianceReminderRecord saveReminder(ComplianceReminderRecord record) {
            // Simulate ON CONFLICT (source_type, source_id, reminder_type, due_date) DO UPDATE
            reminders.removeIf(r ->
                    r.sourceType().equals(record.sourceType()) &&
                    r.sourceId().equals(record.sourceId()) &&
                    r.reminderType().equals(record.reminderType()) &&
                    r.dueDate().equals(record.dueDate()));
            reminders.add(record);
            return record;
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
