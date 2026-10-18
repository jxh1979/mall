package com.complex.platform.energy;

// 文件版本：V0.01
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 到期产生定向能耗主管待办；未核实异常不会因超期转为可出账。 */
@Component
public class EnergyAnomalyEscalationJob {
    private final EnergyWorkflowRepository repository;
    public EnergyAnomalyEscalationJob(EnergyWorkflowRepository repository){this.repository=repository;}
    @Scheduled(fixedDelayString="${energy.anomaly.escalation.delay-ms:60000}")
    @Transactional public void escalate(){repository.escalateOverdue();}
}
