package com.complex.platform.evidence;

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
     * 按条件查询（scope 隔离）。任意过滤项为 null 表示不约束。
     * merchantScope 非 null 时强制按商户隔离（商户用户只能看自己的）。
     */
    List<RiskAlertRecord> search(
            String sourceDomain,
            String status,
            String projectId,
            String merchantScope,
            int limit
    );
}
