package com.complex.platform.ticketing;

import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * AUD-076 生产适配：把 {@link BarMerchantDirectory} 的“按 id 取商户”接到既有 PlatformRepository.findMerchantById
 * （Jdbc 为主键查询）。ticketing→governance 的跨域读与 contract/merchantbilling 既有做法一致。
 */
@Component
class PlatformBarMerchantDirectory implements BarMerchantDirectory {

    private final PlatformRepository platformRepository;

    PlatformBarMerchantDirectory(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    @Override
    public Optional<MerchantRecord> findMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        return platformRepository.findMerchantById(merchantId);
    }
}
