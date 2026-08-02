package com.complex.platform.contract;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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

    // 判定基准日按所在地(北京)日历日取,不按 UTC(2026-08-02 用户拍板)。
    // 业务人员按北京时间日历日理解"合同到期日";服务器时区 America/Los_Angeles,
    // 旧的 UTC 取法会让批转比北京日历日晚最多 8 小时(北京 8/3 上午看到的仍是"生效中")。
    // 合同域无门店维度,故固定 Asia/Shanghai;考勤域若改为按门店所在地动态取,两者取法不同属预期。
    private static final ZoneId LOCAL_ZONE = ZoneId.of("Asia/Shanghai");

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
        runOnce(LocalDate.now(clock.withZone(LOCAL_ZONE)));
    }

    public int runOnce(LocalDate asOf) {
        return service.generateExpiredTransitions(asOf);
    }
}
