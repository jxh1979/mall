package com.complex.platform.compliance;

import java.util.List;
import java.util.Optional;

public interface ComplianceRepository {

    List<ContractKeyDateRecord> findAllContractKeyDates();

    List<ContractKeyDateRecord> findContractKeyDatesByMerchant(String merchantId);

    ContractKeyDateRecord saveContractKeyDate(ContractKeyDateRecord record);

    List<ComplianceExpiryItemRecord> findAllExpiryItems();

    List<ComplianceExpiryItemRecord> findExpiryItemsByMerchant(String merchantId);

    ComplianceExpiryItemRecord saveExpiryItem(ComplianceExpiryItemRecord record);

    List<ComplianceReminderRecord> findAllReminders();

    List<ComplianceReminderRecord> findRemindersByMerchant(String merchantId);

    ComplianceReminderRecord saveReminder(ComplianceReminderRecord record);
}
