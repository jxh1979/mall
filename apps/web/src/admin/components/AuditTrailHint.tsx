export function AuditTrailHint({ event }: { event: string }) {
  return <p className="admin-audit-hint">操作留痕：{event}</p>;
}
