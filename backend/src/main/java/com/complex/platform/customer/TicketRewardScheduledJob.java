package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketRewardScheduledJob {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(TicketRewardScheduledJob.class);
    private final TicketRewardService rewards;
    private final boolean enabled;
    public TicketRewardScheduledJob(TicketRewardService rewards,@Value("${customer.loyalty.scheduled.enabled:false}")boolean enabled){this.rewards=rewards;this.enabled=enabled;}
    @Scheduled(fixedDelayString="${customer.ticket-rewards.scan-delay-ms:60000}")
    public void run(){
        if(!enabled)return;
        Instant now=Instant.now();
        for(String id:rewards.due(now)){
            try{rewards.release(id);}catch(RuntimeException failure){LOG.error("门票积分确认失败，保留待确认记录 {}",id,failure);}
        }
    }
}
