package com.complex.platform.evidence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 统一风险预警仓储（主表 risk_alerts + 流水 risk_alert_actions）。
 */
public interface RiskAlertRepository {

    /** 新建预警主记录（report）。 */
    RiskAlertRecord saveAlert(RiskAlertRecord alert);

    /** 更新预警主记录（状态机推进）。 */
    RiskAlertRecord updateAlert(RiskAlertRecord alert);

    Optional<RiskAlertRecord> findById(String alertId);

    /** 追加一条责任链动作流水（append-only）。 */
    RiskAlertActionRecord appendAction(RiskAlertActionRecord action);

    List<RiskAlertActionRecord> listActions(String alertId);

    /**
     * 按条件查询（scope 隔离）。任意过滤项为 null 表示不约束；sourceDomains 为空不按来源过滤。
     * merchantScope 非 null 时强制按商户隔离（商户用户只能看自己的）；projectScope 非 null 时只取所属项目在其中的（不带项目的不在内）。
     */
    List<RiskAlertRecord> search(
            List<String> sourceDomains,
            String status,
            String projectId,
            String merchantScope,
            Collection<String> projectScope,
            int limit
    );

    /** 第十八批 BC 段 369-R1：未处理（待处理、处理中）条数，来源、商户与项目范围同 {@link #search}。 */
    long countUnfinished(List<String> sourceDomains, String merchantScope, Collection<String> projectScope);
}
