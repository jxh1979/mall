package com.complex.platform.voucher;

// 文件版本：V0.01

import java.time.Instant;

/**
 * "全部券"聚合只读视图行（读层并入，不搬表）：
 * source=unified（统一台账券实例，剩余次数 0/1）| pass_partner（通票第三方合作券＝V282 配置的权益实例，
 * 计次模型，remainingTimes=总次数-已用次数；状态为权益侧原状态，兼容映射不硬套）。
 */
public record VoucherOverviewRecord(
        String id,
        String source,
        String name,
        String voucherKind,
        String customerId,
        String status,
        Integer remainingTimes,
        Instant expiresAt,
        // 面额/折扣（客人端展示；统一券取定义面额，通票第三方券为 null）与核销方式（客人端「核销方式说明」）。
        // 内部结算价（成本）刻意不入此聚合视图——客人/店员天然不可见。
        Long faceValueCent,
        String redeemMethod,
        // V312/#29：统一台账券实例的固定券码（员工端台账查券用；通票第三方券为 null）。ROOT-03-R32（BSM 第 585 行）起客人端不再下发，
        // 客人出示的是 GET /customer/vouchers/{id}/redeem-code 的动态码，见 {@link #withoutCode()}。
        String code,
        // ROOT-03-R30（BSM 第 865 行「已失效详情显示原因以及是否退回积分……可用券只有在券种允许时才显示转赠入口」）：
        // 失效原因＝作废原因；退回积分＝积分兑换券作废后实际退回的积分（无退回流水为 0，非积分兑换券为 null）；可转赠＝券种允许且券仍可用。
        String invalidReason,
        Long pointsRefunded,
        boolean transferable
) {
    /** 客人端投影：去掉固定券码。 */
    public VoucherOverviewRecord withoutCode() {
        return new VoucherOverviewRecord(id, source, name, voucherKind, customerId, status, remainingTimes, expiresAt, faceValueCent,
                redeemMethod, null, invalidReason, pointsRefunded, transferable);
    }
}
