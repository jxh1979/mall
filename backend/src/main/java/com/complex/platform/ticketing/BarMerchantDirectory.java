package com.complex.platform.ticketing;

import com.complex.platform.governance.MerchantRecord;
import java.util.Optional;

/**
 * AUD-076 单店票挂酒吧所需的最小商户读接口：按 id 取商户（判业态 bar / 存档状态 / 取名称）。
 *
 * <p>刻意收窄到一个方法而非注入整张 {@code PlatformRepository}——ticketing 只需“这家酒吧存不存在/是不是 bar/
 * 有没有下架/叫什么名”这一次读；生产由 {@link PlatformBarMerchantDirectory} 适配到 PlatformRepository，
 * 单测传 lambda 即可（免造 20 方法的仓库假实现）。为 null（部分既有单测未接商户目录）时视同“查不到”。
 */
@FunctionalInterface
public interface BarMerchantDirectory {

    Optional<MerchantRecord> findMerchant(String merchantId);
}
