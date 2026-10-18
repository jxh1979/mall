package com.complex.platform.amusement;

// 文件版本：V0.01

import java.time.Instant;
import java.util.Optional;

/**
 * 游乐域→票务域的单向只读+消费端口（防循环依赖）。
 *
 * <p>设计 §12.1：游乐系统只消费票务系统生成的游乐权益实例，本身不售票、不生成权益。
 * 该端口由票务域实现（ticketing 依赖 amusement 的本接口；amusement 不依赖 ticketing 任何 Service，
 * 仅依赖 ticketing 的 DTO），核销时游乐域据此识别票/权益实例（§6.4 步骤 2）、校验并消费权益（§6.4 步骤 4）。
 */
public interface AmusementEntitlementGateway {

    /**
     * 按核销凭证 payload（动态二维码 token 或权益码）查询票务权益实例摘要。
     * 不存在时返回 empty（由调用方落核销失败码 ticket_not_found）。
     */
    Optional<AmusementEntitlementView> findEntitlementByCredential(String credentialPayload);

    /**
     * 原子消费一次权益（issued_entitlements.used_times+1，仅当剩余次数>0）。
     * 成功返回 true；剩余次数耗尽（并发耗尽）返回 false。
     */
    boolean consumeEntitlement(String entitlementId);

    enum Consumption { CONSUMED, DAY_REPLAY, UNAVAILABLE }

    /** 消费与营业日重入在同一权益行锁内判定；生产实现必须校验实际日窗。 */
    default Consumption consumeEntitlement(String entitlementId, AmusementBusinessDayService.Window window, Instant occurredAt) {
        return consumeEntitlement(entitlementId) ? Consumption.CONSUMED : Consumption.UNAVAILABLE;
    }

    /** 实际入场保存后，在原事务内生成对应票务事实；失败使整次入场回滚。 */
    default void recordAdmission(AmusementVerificationRecord admission, AmusementBusinessDayService.Window window,
            Consumption consumption) {}

    /** 只撤销本次新消费，既有营业日激活的重入不调用此方法。 */
    default void reverseEntitlement(String entitlementId, AmusementBusinessDayService.Window window, Instant occurredAt) {
        reverseEntitlement(entitlementId);
    }

    /**
     * 回补一次已消费权益（used_times-1，下限 0），用于权益已扣但后续容量扣减失败时的补偿回滚。
     */
    void reverseEntitlement(String entitlementId);
}
