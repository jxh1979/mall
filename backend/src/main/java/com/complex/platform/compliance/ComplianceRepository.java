package com.complex.platform.compliance;

import java.util.List;
import java.util.Optional;

public interface ComplianceRepository {

    List<ContractKeyDateRecord> findAllContractKeyDates();

    Optional<ContractKeyDateRecord> findContractKeyDateById(String id);

    ContractKeyDateRecord saveContractKeyDate(ContractKeyDateRecord record);

    List<ComplianceExpiryItemRecord> findAllExpiryItems();

    Optional<ComplianceExpiryItemRecord> findExpiryItemById(String id);

    ComplianceExpiryItemRecord saveExpiryItem(ComplianceExpiryItemRecord record);

    List<ComplianceReminderRecord> findAllReminders();

    ComplianceReminderRecord saveReminder(ComplianceReminderRecord record);
}
