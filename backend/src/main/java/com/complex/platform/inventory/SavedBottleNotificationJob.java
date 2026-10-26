package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 存酒生命周期短信通知定时作业（同 TicketOrderExpiryJob 范式）：
 * 业务逻辑在可直接调用的 {@link InventoryService#processSavedBottleNotifications(Instant)}，
 * 此处仅薄 @Scheduled 包装，便于单测直接调方法对拍业务表，不依赖定时触发。
 * 到期提醒按项目配置的提醒节点发（saved_bottle_reminder_policy，BSM L1072），不再读单一提醒窗。
 */
@Component
public class SavedBottleNotificationJob {

    private final InventoryService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SavedBottleNotificationJob(
            InventoryService service,
            @Value("${inventory.saved-bottle.notification.scheduled.enabled:false}") boolean scheduledEnabled
    ) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    SavedBottleNotificationJob(InventoryService service, Clock clock) {
        this(service, clock, true);
    }

    SavedBottleNotificationJob(InventoryService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
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
        service.processSavedBottleNotifications(now);
    }
}
