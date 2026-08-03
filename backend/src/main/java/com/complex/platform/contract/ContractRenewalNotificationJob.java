package com.complex.platform.contract;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.NotificationWindowConfig;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 合同续约短信提醒定时作业（同 TicketOrderExpiryJob/SavedBottleNotificationJob 范式）：
 * 业务逻辑在可直接调用的 {@link ContractService#processMerchantFeeRenewalNotifications(LocalDate, int)}，
 * 此处仅薄 @Scheduled 包装，便于单测直接调方法对拍通知日志，不依赖定时触发。
 */
@Component
public class ContractRenewalNotificationJob {

    // V219:续约提醒窗天数迁入 notification_windows(key=contract.renewal),缺配置兜底 30(行为不变)。
    static final int RENEWAL_NOTICE_DAYS = 30;
    static final String WINDOW_KEY = "contract.renewal";

    private final ContractService service;
    private final Clock clock;
    private final boolean scheduledEnabled;
    private final NotificationWindowConfig windowConfig;

    @Autowired
    public ContractRenewalNotificationJob(
            ContractService service,
            @Value("${contract.renewal.notification.scheduled.enabled:false}") boolean scheduledEnabled,
            NotificationWindowConfig windowConfig
    ) {
        this(service, Clock.systemUTC(), scheduledEnabled, windowConfig);
    }

    ContractRenewalNotificationJob(ContractService service, Clock clock) {
        this(service, clock, true, new NotificationWindowConfig());
    }

    ContractRenewalNotificationJob(ContractService service, Clock clock, boolean scheduledEnabled) {
        this(service, clock, scheduledEnabled, new NotificationWindowConfig());
    }

    ContractRenewalNotificationJob(ContractService service, Clock clock, boolean scheduledEnabled,
            NotificationWindowConfig windowConfig) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
        this.windowConfig = windowConfig;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 contract.renewal.notification.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "contract.renewal.notification.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${contract.renewal.notification.scheduled.fixed-delay-ms:86400000}")
    public void scheduledNotifyRenewals() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(LocalDate.now(clock.withZone(LocalZone.ID)));
    }

    public void runOnce(LocalDate asOf) {
        service.processMerchantFeeRenewalNotifications(asOf, windowConfig.days(WINDOW_KEY, RENEWAL_NOTICE_DAYS));
    }
}
