package com.complex.platform.voucher;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * V312 券预占过期释放定时作业（SavedBottleNotificationJob 同款薄包装范式）：
 * 业务逻辑在可直接调用的 {@link VoucherService#expireReservedVouchers(Instant, int)}，此处仅 @Scheduled 包装，
 * 便于单测直接调方法对拍业务表、不依赖定时触发。
 *
 * <p>惰性过期口径（{@link VoucherStateMachine}）下，过期未核销的店内实物券若无人再触碰，其库存预占会一直占死；
 * 本作业周期性扫描 issued+已预占+已过期的券，逐条 CAS 过期并释放预占。默认关闭，仅在
 * voucher.reservation.expiry.scheduled.enabled=true 时注册。
 */
@Component
public class VoucherReservationExpiryJob {

    static final int DEFAULT_BATCH_LIMIT = 500;

    private final VoucherService service;
    private final Clock clock;
    private final boolean scheduledEnabled;
    private final int batchLimit;

    @Autowired
    public VoucherReservationExpiryJob(
            VoucherService service,
            @Value("${voucher.reservation.expiry.scheduled.enabled:false}") boolean scheduledEnabled,
            @Value("${voucher.reservation.expiry.scheduled.batch-limit:500}") int batchLimit) {
        this(service, Clock.systemUTC(), scheduledEnabled, batchLimit);
    }

    VoucherReservationExpiryJob(VoucherService service, Clock clock) {
        this(service, clock, true, DEFAULT_BATCH_LIMIT);
    }

    VoucherReservationExpiryJob(VoucherService service, Clock clock, boolean scheduledEnabled, int batchLimit) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
        this.batchLimit = batchLimit <= 0 ? DEFAULT_BATCH_LIMIT : batchLimit;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 voucher.reservation.expiry.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "voucher.reservation.expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${voucher.reservation.expiry.scheduled.fixed-delay-ms:3600000}")
    public void scheduledReleaseExpired() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(Instant.now(clock));
    }

    /** 单次执行（可直接单测调用）：返回本轮释放预占的券数。 */
    public int runOnce(Instant now) {
        return service.expireReservedVouchers(now, batchLimit);
    }
}
