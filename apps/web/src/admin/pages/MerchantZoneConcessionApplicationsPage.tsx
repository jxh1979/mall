// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 后端 /api/v1/merchant-portal/concession-ticket-types 与 /concession-applications 只出本商户可申请的票与本商户的申请（客户端已 snake→camel）。
type TicketOption = { id: string; ticketName?: string };
type ConcessionApplication = { id: string; ticketName?: string; entitlementContent?: string; confirmedAt?: string };

const APPLY = ["merchant.portal.concession.apply"];

function asRows<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  const items = (value as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/**
 * ADM-MER-011 让利申请（第十八批 BV 段 264-R3；第十八批裁决 78「商户用自己的账号提交让利申请（哪张票、哪项权益）并在线确认，运营配票种时只能从已确认的申请里选
 * 商户让利型。」；BSM:1033「只有商户主动申请并书面确认让利的权益才记为商户让利、不结算」）：选票、写明让利的是哪项权益，勾选确认后提交即在线确认。
 */
export function MerchantZoneConcessionApplicationsPage({ api, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const [tickets, setTickets] = React.useState<TicketOption[]>([]);
  const [items, setItems] = React.useState<ConcessionApplication[]>([]);
  const [ticketTypeId, setTicketTypeId] = React.useState("");
  const [content, setContent] = React.useState("");
  const [confirmed, setConfirmed] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [ticketData, itemData] = await Promise.all([
        api.get<unknown>("/api/v1/merchant-portal/concession-ticket-types"),
        api.get<unknown>("/api/v1/merchant-portal/concession-applications"),
      ]);
      setTickets(asRows<TicketOption>(ticketData));
      setItems(asRows<ConcessionApplication>(itemData));
    },
    { onError: setGlobalError, fallbackMessage: "让利申请读取失败", onStart: () => setGlobalError(null) },
  );

  async function submit() {
    const ticketName = tickets.find((ticket) => ticket.id === ticketTypeId)?.ticketName;
    const entitlementContent = content.trim();
    setSubmitting(true);
    try {
      await api.post("/api/v1/merchant-portal/concession-applications", {
        idempotency_key: crypto.randomUUID(),
        ticket_type_id: ticketTypeId,
        entitlement_content: entitlementContent,
        confirmed: true,
      });
      setActionResult({ action: "merchant_concession_application_submitted", ticketName, entitlementContent });
      setTicketTypeId("");
      setContent("");
      setConfirmed(false);
      reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "让利申请提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="merchant-zone-concession-page">
      <AdminPageHeader
        title="让利申请"
        description="只有商户主动申请并书面确认让利的权益，平台才记为商户让利，该项权益核销后不与本商户结算。在这里选票、写明让利的是哪项权益，确认后提交；运营配票时只能从已确认的申请里选商户让利。"
      />
      <div className="admin-inline-form" data-testid="merchant-concession-form">
        <select aria-label="票种" data-testid="merchant-concession-ticket" value={ticketTypeId} onChange={(event) => setTicketTypeId(event.target.value)}>
          <option value="">选择票种</option>
          {tickets.map((ticket) => (<option key={ticket.id} value={ticket.id}>{ticket.ticketName ?? "-"}</option>))}
        </select>
        <textarea aria-label="让利权益内容" data-testid="merchant-concession-content" maxLength={200} value={content}
          placeholder="写明让利的是哪项权益，如：赠送招牌鸡尾酒一杯" onChange={(event) => setContent(event.target.value)} />
        <label>
          <input type="checkbox" data-testid="merchant-concession-confirm" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} />
          本商户主动申请并确认：所填权益在所选票中记为商户让利，客人核销后平台不就该项权益与本商户结算。
        </label>
        <PermissionButton permissionsState={effectivePermissions} permissions={APPLY} data-testid="merchant-concession-submit"
          disabled={submitting || !ticketTypeId || !content.trim() || !confirmed} onClick={() => void submit()}>
          确认并提交
        </PermissionButton>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-concession-table">
            <thead><tr><th>票种</th><th>让利权益内容</th><th>确认时间</th></tr></thead>
            <tbody>
              {items.length === 0 ? <tr><td colSpan={3}>还没有让利申请</td></tr> : items.map((item) => (
                <tr key={item.id}>
                  <td>{item.ticketName ?? "-"}</td>
                  <td>{item.entitlementContent ?? "-"}</td>
                  <td>{cnDate(item.confirmedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
