package com.complex.platform.inventory;

import java.time.Clock;
import java.time.Instant;
import com.complex.platform.common.NotificationWindowConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 存酒生命周期短信通知定时作业（同 TicketOrderExpiryJob 范式）：
 * 业务逻辑在可直接调用的 {@link InventoryService#processSavedBottleNotifications(Instant, int)}，
 * 此处仅薄 @Scheduled 包装，便于单测直接调方法对拍业务表，不依赖定时触发。
 */
@Component
public class SavedBottleNotificationJob {

    // V219:存酒临期提醒窗天数迁入 notification_windows(key=inventory.saved_bottle),缺配置兜底 7(行为不变)。
    static final int EXPIRING_NOTICE_DAYS = 7;
    static final String WINDOW_KEY = "inventory.saved_bottle";

    private final InventoryService service;
    private final Clock clock;
    private final boolean scheduledEnabled;
    private final NotificationWindowConfig windowConfig;

    @Autowired
    public SavedBottleNotificationJob(
            InventoryService service,
            @Value("${inventory.saved-bottle.notification.scheduled.enabled:false}") boolean scheduledEnabled,
            NotificationWindowConfig windowConfig
    ) {
        this(service, Clock.systemUTC(), scheduledEnabled, windowConfig);
    }

    SavedBottleNotificationJob(InventoryService service, Clock clock) {
        this(service, clock, true, new NotificationWindowConfig());
    }

    SavedBottleNotificationJob(InventoryService service, Clock clock, boolean scheduledEnabled) {
        this(service, clock, scheduledEnabled, new NotificationWindowConfig());
    }

    SavedBottleNotificationJob(InventoryService service, Clock clock, boolean scheduledEnabled,
            NotificationWindowConfig windowConfig) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
        this.windowConfig = windowConfig;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 inventory.saved-bottle.notification.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "inventory.saved-bottle.notification.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${inventory.saved-bottle.notification.scheduled.fixed-delay-ms:86400000}")
    public void scheduledNotifyLifecycle() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(Instant.now(clock));
    }

    public void runOnce(Instant now) {
        service.processSavedBottleNotifications(now, windowConfig.days(WINDOW_KEY, EXPIRING_NOTICE_DAYS));
    }
}
