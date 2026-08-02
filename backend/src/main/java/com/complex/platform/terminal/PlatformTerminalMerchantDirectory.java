package com.complex.platform.terminal;

import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.governance.PlatformRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 生产适配：把 {@link TerminalMerchantDirectory} 的“按 id 取商户”接到既有
 * PlatformRepository.findMerchantById（Jdbc 为主键查询）。terminal→governance 的跨域读与
 * ticketing 的 {@code PlatformBarMerchantDirectory} 既有做法一致。
 */
@Component
class PlatformTerminalMerchantDirectory implements TerminalMerchantDirectory {

    private final PlatformRepository platformRepository;

    PlatformTerminalMerchantDirectory(PlatformRepository platformRepository) {
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
