import React from "react";
import { TraceLink } from "./TraceLink";
import type { RowDetailField } from "./RowDetailDrawer";

// 审计事件页 pageId（操作留痕跨对象查询入口）。
const AUDIT_PAGE_ID = "ADM-EVD-001";

// 反查焦点参数：以 `业务类型:业务id` 组合传给审计页，页内解析为 business_type/business_id 服务端过滤。
export function auditFocusToken(businessType: string, businessId: string) {
  return `${businessType}:${businessId}`;
}

// 对象详情抽屉里的「操作审计」反查字段：点击 → 跳审计页并按该对象过滤真操作日志（audit_events）。
// navigateTo 或 businessId 缺失时降级为纯文本占位，不破坏既有展示。
export function auditTraceField(
  navigateTo: ((pageId: string, objectId?: string) => void) | undefined,
  businessType: string,
  businessId?: string | null,
  label = "操作审计",
): RowDetailField {
  const value = businessId
    ? (
      <TraceLink navigateTo={navigateTo} pageId={AUDIT_PAGE_ID} objectId={auditFocusToken(businessType, businessId)}>
        查看该对象操作审计
      </TraceLink>
    )
    : "—";
  return { label, value };
}
