package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dividend.DividendRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 CL 段 422-R1（第十八批裁决 79，BSM:833「平台方（城市项目公司及娱乐平台公司、游乐公司、传媒公司、中央餐饮公司）按工商登记合计持股过半的商户，
 * 不得发行商户级卡，只能使用综合体通用卡。」；ENUM:297「酒吧的参股方为娱乐平台公司，引进参股的游乐项目的参股方为引进该项目的域主管公司」「必须以获批登记版本
 * 生效」）：持股取该商户当日生效的已批准分红方案版本的平台方（参股方）比例，严格大于 50% 即不得发卡；没有生效版本按平台方未持股。
 */
@Service
public class MerchantCardIssuanceEligibility {

    private static final int HALF_BPS = 5000;

    private final DividendRepository dividends;

    public MerchantCardIssuanceEligibility(DividendRepository dividends) {
        this.dividends = dividends;
    }

    /** 打开发卡与客人给商户级卡充值（开卡即发卡）共用。 */
    public void requireIssuable(String merchantId) {
        dividends.findPlanInForce(merchantId, LocalDate.now(LocalZone.ID))
                .filter(plan -> plan.platformShareBps() > HALF_BPS)
                .ifPresent(plan -> {
                    throw ApiException.conflict("MERCHANT_CARD_PLATFORM_MAJORITY",
                            "平台方合计持股过半的商户不得发行商户级卡，只能使用综合体通用卡");
                });
    }
}
