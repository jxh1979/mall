package com.complex.platform.ticketing;

/**
 * 删除审批结果。approved 时模板已物理删除（templateId 仅供留痕），deleted=true；
 * rejected 时模板回到 none 态、未删除。
 */
public record EntitlementTemplateDeleteDecision(String templateId, String decision, boolean deleted) {
}
