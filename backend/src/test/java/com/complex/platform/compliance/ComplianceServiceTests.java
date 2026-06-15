package com.complex.platform.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.compliance.dto.ComplianceExpiryItemRequest;
import com.complex.platform.compliance.dto.ComplianceReminderScanRequest;
import com.complex.platform.compliance.dto.ContractKeyDateRequest;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComplianceServiceTests {

    private InMemoryComplianceRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private ComplianceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryComplianceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new ComplianceService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
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

    private static final class InMemoryComplianceRepository implements ComplianceRepository {
        private final Map<String, ContractKeyDateRecord> contractKeyDates = new LinkedHashMap<>();
        private final Map<String, ComplianceExpiryItemRecord> expiryItems = new LinkedHashMap<>();
        private final List<ComplianceReminderRecord> reminders = new ArrayList<>();
        private int contractDateSaveCount;

        @Override
        public List<ContractKeyDateRecord> findAllContractKeyDates() {
            return new ArrayList<>(contractKeyDates.values());
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
        public ComplianceReminderRecord saveReminder(ComplianceReminderRecord record) {
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
