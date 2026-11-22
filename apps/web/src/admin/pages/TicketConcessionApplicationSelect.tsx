// 文件版本：V0.01
import React from "react";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminApiClient } from "../types";

// 后端 GET /api/v1/ticketing/ticket-types/{id}/concession-applications（客户端已 snake→camel）：该票各商户已确认的让利申请，bindingId 非空＝已用于本票某项权益。
type ConcessionApplication = { id: string; merchantId?: string; entitlementContent?: string; confirmedAt?: string; bindingId?: string | null };

function toRows(data: unknown): ConcessionApplication[] {
  if (Array.isArray(data)) return data as ConcessionApplication[];
  const items = (data as { items?: ConcessionApplication[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/**
 * 第十八批 BV 段 264-R4（第十八批裁决 78「运营配票种时只能从已确认的申请里选商户让利型」；BSM:1033「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」）：
 * 选了商户让利型的权益，只能从该商户为本票提交并确认、尚未用于本票其他权益的让利申请里选；换了商户或票、所选已不可选时清空。
 */
export function ConcessionApplicationSelect({ api, ticketTypeId, merchantId, value, onChange, testId }: {
  api: AdminApiClient;
  ticketTypeId: string;
  merchantId: string;
  value: string;
  onChange: (applicationId: string) => void;
  testId: string;
}) {
  const [rows, setRows] = React.useState<ConcessionApplication[] | null>(null);
  React.useEffect(() => {
    setRows(null);
    if (!ticketTypeId) return undefined;
    let live = true;
    api.get<unknown>(`/api/v1/ticketing/ticket-types/${encodeURIComponent(ticketTypeId)}/concession-applications`)
      .then((data) => { if (live) setRows(toRows(data)); })
      .catch(() => { if (live) setRows([]); });
    return () => { live = false; };
  }, [api, ticketTypeId]);
  const options = (rows ?? []).filter((row) => row.merchantId === merchantId && !row.bindingId);
  const stale = rows !== null && value !== "" && !options.some((row) => row.id === value);
  React.useEffect(() => {
    if (stale) onChange("");
  }, [stale, onChange]);

  if (!ticketTypeId) return <span>请先选择票种</span>;
  if (rows !== null && options.length === 0) {
    return <span data-testid={`${testId}-empty`}>该商户还没有为这张票提交并确认让利申请，须由商户在商户专区「让利申请」提交</span>;
  }
  return (
    <select aria-label="让利申请" data-testid={testId} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">选择该商户已确认的让利申请</option>
      {options.map((row) => (
        <option key={row.id} value={row.id}>{`${row.entitlementContent ?? ""}（${cnDate(row.confirmedAt)} 确认）`}</option>
      ))}
    </select>
  );
}
