package com.complex.platform.merchantbilling;

import java.util.List;
import java.util.Optional;

public interface MerchantFeeNotificationRecipientRepository {

    List<MerchantFeeNotificationRecipientRecord> listFeeNotificationRecipients(String merchantId);

    Optional<MerchantFeeNotificationRecipientRecord> findFeeNotificationRecipientById(String id);

    Optional<MerchantFeeNotificationRecipientRecord> findActiveFeeNotificationRecipient(
            String merchantId,
            String feeEvent
    );

    MerchantFeeNotificationRecipientRecord upsertFeeNotificationRecipient(
            MerchantFeeNotificationRecipientRecord record
    );

    Optional<MerchantFeeNotificationRecipientRecord> deactivateFeeNotificationRecipient(String id);
}
