package com.complex.platform.voucher;

import com.complex.platform.customer.CustomerRedemptionCatalogItemRecord;
import java.time.Instant;
import java.util.List;

/**
 * 积分兑换→统一台账发券端口（CustomerService.redeemPoints 调用，VoucherService 实现）。
 * 兑换只发券不动库存；实物出库/内部结算移到统一核销阶段。
 */
public interface VoucherIssuePort {

    /**
     * 按兑换目录项发券：目录显式挂接的券定义优先，未挂接的按 vdef_cat_&lt;目录id&gt; 派生
     * （get-or-create）。实例 id 由兑换记录 id 派生，重放天然幂等。
     */
    List<VoucherInstanceRecord> issueForPointsRedemption(
            CustomerRedemptionCatalogItemRecord catalogItem,
            String customerId,
            String redemptionId,
            int quantity,
            Instant now);
}
