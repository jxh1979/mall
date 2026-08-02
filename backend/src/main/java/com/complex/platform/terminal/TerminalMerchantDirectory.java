package com.complex.platform.terminal;

import com.complex.platform.governance.MerchantRecord;
import java.util.Optional;

/**
 * K16 设备商户归属写入面所需的最小商户读接口：按 id 取商户（判存在 / 存档状态 / 取名称）。
 *
 * <p>照 ticketing 的 {@code BarMerchantDirectory} 范式刻意收窄到一个方法而非注入整张
 * {@code PlatformRepository}——terminal 只需“这家商户存不存在/有没有归档”这一次读；
 * 生产由 {@link PlatformTerminalMerchantDirectory} 适配到 PlatformRepository，单测传 lambda 即可。
 */
@FunctionalInterface
public interface TerminalMerchantDirectory {

    Optional<MerchantRecord> findMerchant(String merchantId);
}
