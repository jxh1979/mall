package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="customer.loyalty.scheduled.enabled",havingValue="true")
public class TicketPointsBillingJob {
    private static final Logger log=LoggerFactory.getLogger(TicketPointsBillingJob.class);
    private final TicketPointsBillingService bills;
    public TicketPointsBillingJob(TicketPointsBillingService bills){this.bills=bills;}
    @Scheduled(fixedDelayString="${customer.ticket-points-billing.scan-delay-ms:60000}")
    public void run(){
        Instant now=Instant.now();
        try{bills.closeReadyMonths(now);}catch(RuntimeException e){log.error("门票积分月结未完成，保留原发行费事实待处理",e);}
        for(String id:bills.unfinished())try{bills.finalizeDue(id,now);}catch(RuntimeException e){log.error("积分账单 {} 未形成应收，保留未结状态",id,e);}
        try{bills.notifyPublishedBills();}catch(RuntimeException e){log.error("积分账单通知未完成，账单可见时间与异议期保持",e);}
    }
}
