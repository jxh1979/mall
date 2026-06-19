package com.complex.platform.evidence;

import java.util.List;

/**
 * 风险预警详情（主记录 + 责任链动作流水），供详情查询返回。
 */
public record RiskAlertDetail(
        RiskAlertRecord alert,
        List<RiskAlertActionRecord> actions
) {
}
