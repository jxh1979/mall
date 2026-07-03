import { TechDetails } from "./TechDetails";

// 技术段落折叠（用户裁决）：留痕事件名默认收进「技术详情」，文字原样保留。
export function AuditTrailHint({ event }: { event: string }) {
  return (
    <TechDetails>
      <p className="admin-audit-hint">操作留痕：{event}</p>
    </TechDetails>
  );
}
