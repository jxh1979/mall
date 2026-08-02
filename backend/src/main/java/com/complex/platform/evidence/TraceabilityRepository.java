package com.complex.platform.evidence;

import java.util.List;
import java.util.Optional;

public interface TraceabilityRepository {

    TraceabilityProcessInstanceRecord saveProcessInstance(TraceabilityProcessInstanceRecord record);

    /**
     * 按综合体（project）scope 列出业务流程实例（business_process_instances.project_id = token scope）。
     * 商户面/综合体面数据：按 project_id 行级过滤。默认空，JdbcTraceabilityRepository 用 SQL 谓词下推。
     * 隔离闸口（AC-ISO-1）：提供 project_id 行级 scoped 读通道（与 CSV scope_source=project_id 对齐）。
     * 勿删:本方法零调用是常态(多综合体隔离通道,建成待激活),但 verify:tenant-isolation 门禁
     * 以「该表存在 scoped 查询」为隔离能力判据,删除会导致门禁 fail-closed 阻断部署。
     */
    default List<TraceabilityProcessInstanceRecord> listProcessInstancesByProject(String projectId) {
        return List.of();
    }

    Optional<TraceabilityProcessInstanceRecord> findProcessInstanceById(String id);

    TraceabilityStepRecord saveStep(TraceabilityStepRecord record);

    List<TraceabilityStepRecord> listStepsByInstance(String processInstanceId);

    default TraceabilityAutoCaptureRate autoCaptureRate(String merchantScope, int thresholdPercent) {
        return new TraceabilityAutoCaptureRate(0, 0, 100.0, thresholdPercent, true);
    }

    TraceabilityResponsibilityRecord saveResponsibility(TraceabilityResponsibilityRecord record);

    List<TraceabilityResponsibilityRecord> listResponsibilities(String merchantScope);
}
