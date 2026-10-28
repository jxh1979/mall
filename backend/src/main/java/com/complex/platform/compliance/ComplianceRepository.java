package com.complex.platform.compliance;

import java.time.LocalDate;
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

    // ---- 第十八批 E 段第 22 条：按证照类型配置、宽限申请、续期登记 ----

    List<ComplianceItemTypeConfigRecord> findItemTypeConfigs();

    void saveItemTypeConfig(ComplianceItemTypeConfigRecord record, String actorId);

    /** 按编号读证照条目；在事务内即锁住该行。 */
    Optional<ComplianceExpiryItemRecord> findExpiryItemForUpdate(String id);

    void markGraceRequested(String id, LocalDate graceUntil, String reviewId);

    /** 只在条目仍挂着这张审批单、且宽限在审时回写；返回改动行数。 */
    int decideGrace(String id, String reviewId, String decision);

    /** 就地更新有效期等并清空宽限申请。 */
    void renewExpiryItem(String id, LocalDate validFrom, LocalDate validTo, String documentNo, List<String> evidenceAttachmentIds);
}
