package com.complex.platform.contract;

import com.complex.platform.common.LocalZone;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 合同到期批转定时作业（同 ContractRenewalNotificationJob/TicketOrderExpiryJob 范式）：
 * 业务逻辑在可直接调用的 {@link ContractService#generateExpiredTransitions(LocalDate)}（含审计留痕
 * contract.expired_batch），此处仅薄 @Scheduled 包装，便于单测直接调方法对拍状态，不依赖定时触发。
 *
 * <p>接线背景（2026-08-02）：旧 governance ContractExpiryJob 删除后调用方一直缺位——合同过
 * expiry_date 仍停在 effective，变更/结算等按 effective 判定的入口照常放行。
 * 本作业补上日度批转 effective → expired。
 */
@Component
public class ContractExpiryJob {

    private final ContractService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public ContractExpiryJob(ContractService service,
            @Value("${contract.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    ContractExpiryJob(ContractService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 contract.expiry.scheduled.enabled=true 时执行。 */
    @ConditionalOnProperty(name = "contract.expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${contract.expiry.scheduled.fixed-delay-ms:86400000}")
    public void scheduledExpireContracts() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(LocalDate.now(clock.withZone(LocalZone.ID)));
    }

    public int runOnce(LocalDate asOf) {
        return service.generateExpiredTransitions(asOf);
    }
}
