import React from "react";
import { TechDetails } from "./TechDetails";
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

type Props = {
  event: string;
  // 传入 navigateTo + 业务类型/对象 id 时，「操作留痕」追加可点反查链接：跳审计页并按该对象过滤真操作日志。
  navigateTo?: (pageId: string, objectId?: string) => void;
  businessType?: string;
  businessId?: string | null;
};

// 技术段落折叠（用户裁决）：留痕事件名默认收进「技术详情」，文字原样保留。
// 反查升级（条目5 全链可点击）：给定对象引用则追加「查看该对象操作审计」可点入口，复用 TraceLink/navigateTo。
export function AuditTrailHint({ event, navigateTo, businessType, businessId }: Props) {
  const canTrace = Boolean(navigateTo && businessType && businessId);
  return (
    <TechDetails label="操作留痕">
      <p className="admin-audit-hint">操作留痕：{event}</p>
      {canTrace ? (
        <p className="admin-audit-hint">
          <TraceLink navigateTo={navigateTo} pageId={AUDIT_PAGE_ID} objectId={auditFocusToken(businessType!, businessId!)}>
            查看该对象操作审计
          </TraceLink>
        </p>
      ) : null}
    </TechDetails>
  );
}
