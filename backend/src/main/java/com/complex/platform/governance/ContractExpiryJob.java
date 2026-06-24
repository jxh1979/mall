package com.complex.platform.governance;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ② 履约闭环 S3：合同到期调度（@Scheduled 薄包装，默认关）。
 * 仅在 contract.expiry_job.enabled=true 时注册；调 {@link PlatformService#generateExpiredContractTransitions}
 *（其内再受 contract.renewal_termination.enabled 门控），把过 expiry_date 仍 effective 的合同转 expired + 释放空间。
 * 单构造器（无多构造 @Autowired 坑）；到期逻辑直接在 PlatformService 单测，不依赖定时触发。
 */
@Component
public class ContractExpiryJob {

    private final PlatformService service;

    public ContractExpiryJob(PlatformService service) {
        this.service = service;
    }

    @ConditionalOnProperty(name = "contract.expiry_job.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${contract.expiry_job.fixed-delay-ms:3600000}")
    public void scheduledRun() {
        service.generateExpiredContractTransitions(LocalDate.now(ZoneOffset.UTC));
    }
}
