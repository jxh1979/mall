package com.complex.platform.finance;

// 文件版本：V0.01

/**
 * 第十四批 OBS-13-03：订台定金退款实际成功（BSM L1062「只有实际退款成功才记为已退」）——达标原路退款应付实付结清，或订台定金原路待退
 * （finance_refunds.source_type=bar_deposit_rejection）经核实成功。同一事务内通知酒吧域回写已退还并发定金原路退回通知（BSM L1596）。
 * 放在 finance 包，免得财务反向依赖酒吧域。
 */
public record BarDepositRefunded(String reservationId) {
}
