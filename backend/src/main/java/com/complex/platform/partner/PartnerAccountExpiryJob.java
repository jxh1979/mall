package com.complex.platform.partner;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 外部机构账号时效到期停用定时任务（默认关，partner.account.expiry.scheduled.enabled=true 时注册）。 */
@Component
public class PartnerAccountExpiryJob {

    private final PartnerService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    public PartnerAccountExpiryJob(PartnerService service,
            @Value("${partner.account.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.service = service;
        this.clock = Clock.systemUTC();
        this.scheduledEnabled = scheduledEnabled;
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void run() {
        if (scheduledEnabled) {
            service.expireOverdueAccounts(Instant.now(clock));
        }
    }
}
