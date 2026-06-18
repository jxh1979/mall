package com.complex.platform.merchantbilling;

import java.util.List;
import java.util.Optional;

public interface MerchantBillingRepository {
    List<MerchantBillRecord> findAllBills();

    List<MerchantBillRecord> findBillsByMerchantId(String merchantId);

    Optional<MerchantBillRecord> findBillById(String id);

    MerchantBillRecord saveBill(MerchantBillRecord record);

    MerchantBillDisputeRecord saveDispute(MerchantBillDisputeRecord record);

    List<MerchantBillDisputeRecord> findDisputesByBillId(String billId);

    MerchantBillDunningRecord saveDunningRecord(MerchantBillDunningRecord record);

    List<MerchantBillDunningRecord> findDunningRecordsByBillId(String billId);
}
