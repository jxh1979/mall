import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import type { AdminPageProps } from "../types";

export function TicketingPoliciesPage({ api, setActionResult }: AdminPageProps) {
  const [weekday, setWeekday] = React.useState("");
  const [weekend, setWeekend] = React.useState("");
  const [holiday, setHoliday] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  async function publish() {
    if (!weekday || !weekend || !holiday) {
      setError("平日、周末、节假日基础价必须全部填写，缺少节假日价不可发布。");
      return;
    }
    const prices = { 平日价: Number(weekday), 周末价: Number(weekend), 节假日价: Number(holiday) };
    const invalid = Object.entries(prices).find(([, value]) => !Number.isFinite(value) || value <= 0);
    if (invalid) {
      setError(`${invalid[0]}必须是大于 0 的数字。`);
      return;
    }
    setError("");
    setBusy(true);
    try {
      const ticket = await api.post<{ id: string }>("/api/v1/ticketing/ticket-types", {
        productCode: "ADMIN-PASS",
        productName: "后台通票策略",
        productType: "pass",
        priceAmount: Math.round(prices.平日价 * 100),
        weekdayPrice: Math.round(prices.平日价 * 100),
        weekendPrice: Math.round(prices.周末价 * 100),
        holidayPrice: Math.round(prices.节假日价 * 100),
      });
      const published = await api.post(`/api/v1/ticketing/ticket-types/${ticket.id ?? "ticket-1"}/publish`, {});
      setActionResult({ action: "ticket_policy_publish", status: "published", published });
    } catch (publishError) {
      setError(publishError instanceof Error ? publishError.message : "发布失败");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section data-testid="ticketing-policies-page">
      <AdminPageHeader title="票务策略" description="最高优先级为平日/周末/节假日基础价，多人、新人、学生、渠道价基于基础价派生或固定价。" />
      <div className="admin-form-row">
        <input data-testid="ticket-weekday-price" type="number" min={0} step="0.01" value={weekday} onChange={(event) => setWeekday(event.target.value)} placeholder="平日价" />
        <input data-testid="ticket-weekend-price" type="number" min={0} step="0.01" value={weekend} onChange={(event) => setWeekend(event.target.value)} placeholder="周末价" />
        <input data-testid="ticket-holiday-price" type="number" min={0} step="0.01" value={holiday} onChange={(event) => setHoliday(event.target.value)} placeholder="节假日价" />
        <button type="button" data-testid="ticket-publish" onClick={() => void publish()} disabled={busy}>发布策略</button>
      </div>
      <div data-testid="ticket-derived-preview" className="admin-preview">新人票按基础价 8 折派生；学生票按基础价 7 折派生；多人票和渠道价可改为固定价。</div>
      {error ? <div data-testid="ticket-policy-error" className="admin-inline-error">{error}</div> : null}
    </section>
  );
}
