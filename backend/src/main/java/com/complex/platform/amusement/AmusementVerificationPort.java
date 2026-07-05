package com.complex.platform.amusement;

import java.util.Optional;

/**
 * 票务核销 → 游乐正路的分流端口（阶段1，2026-07-05 用户拍板）。
 *
 * <p>依赖方向 ticketing → amusement（与 {@link AmusementEntitlementGateway} 相反的另一条单向边：
 * ticketing 调本端口把游乐类权益核销分流进游乐正路；amusement 仍不依赖 ticketing 任何 Service）。
 *
 * <p>由 {@link AmusementService} 实现。ticketing 侧检出游乐类权益后调 {@link #verifyAmusementEntry}，
 * 该方法在【调用方事务内】完成：point/device→项目定位、当前 open 场次定位、项目 open+巡检准入、
 * 三维容量原子扣减、消费票务权益（issued_entitlements.used_times+1，容量失败则补偿回补）、
 * 写 amusement_verifications（project+session+verified_at+verified_people）。返回落库的核销记录 id
 * 供 ticketing 侧写交叉引用（ticket_verifications.amusement_verification_id）。
 */
public interface AmusementVerificationPort {

    /**
     * 把一张游乐类权益核销分流进游乐正路。
     *
     * <p>返回语义：
     * <ul>
     *   <li>{@code Optional.empty()}：本核销点/设备【不属于任何游乐项目】——即本核销并非在游乐入场闸发起，
     *       不应走游乐正路。调用方据此【回退】通用核销旧路（保持 unlimited_in_day 等既有 amusement_type 消耗语义、
     *       向后兼容）。此为「点位非游乐闸」而非「配置缺失」，故不报错。</li>
     *   <li>{@code Optional.of(id)}：定位到游乐项目并成功核销，返回落库的 amusement_verifications.id
     *       供 ticketing 侧写交叉引用。</li>
     * </ul>
     *
     * <p>一旦定位到游乐项目（即本核销确在游乐闸），后续任一步失败即抛
     * {@link com.complex.platform.common.api.ApiException}（项目未开、当前无进行中场次、安全闸拦截、本场满等），
     * 且【不净消费权益】（容量失败已补偿回补），由 ticketing 侧原样上抛。
     *
     * @param request 核销上下文（凭证 payload、point/device、staff、幂等键、now）
     */
    Optional<String> verifyAmusementEntry(AmusementEntryRequest request);
}
