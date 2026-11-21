package com.complex.platform.leasing;

// 文件版本：V0.01

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.governance.PlatformService;
import org.springframework.stereotype.Service;

/**
 * 第十八批 BW 段第 385 条 385-R2（PGM:502「签约转商户自动带入联系人与联系方式、意向业态/面积/位置、报价租金条款和跟进历史，……」）：
 * 商户档案详情按来源线索读出招商跟进历史（线索本身加该线索各意向的跟进），不复制到商户侧。读者与商户档案同口径
 * （PlatformService.requireMerchant：商户不存在 404；非平台级账号须该商户经营项目在 merchant.profile.view 可触及范围内，否则 403）。
 */
@Service
public class LeasingMerchantFollowUpService {

    private final LeasingRepository repository;
    private final PlatformService platformService;

    public LeasingMerchantFollowUpService(LeasingRepository repository, PlatformService platformService) {
        this.repository = repository;
        this.platformService = platformService;
    }

    public PageResponse<LeasingFollowUpRecord> listForMerchant(String merchantId, int page, int size) {
        platformService.requireMerchant(merchantId, "merchant.profile.view");
        return repository.listFollowUpsForMerchantPaged(merchantId, page, size);
    }
}
