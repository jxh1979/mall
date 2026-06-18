import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, AmusementGroupBookingRow } from "../types";

const TEXT = {
  title: "团体/包场",
  description: "查看团体预约和包场资源占用，支持释放容量并保留操作原因。",
  evidence: "接口证据：读取 /api/v1/amusement/group-bookings；释放调用 /api/v1/amusement/group-bookings/{id}/release。",
  boundary: "团体/包场属于容量占用增强能力；释放容量必须写明原因，不得静默覆盖预约和核销证据。",
  loadFailed: "团体/包场读取失败",
  reasonRequired: "释放原因不能为空。",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

export function AmusementGroupBookingsPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [bookings, setBookings] = React.useState<AmusementGroupBookingRow[]>([]);
  const [releaseReason, setReleaseReason] = React.useState("完整后台验收释放");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setBookings(await api.get<AmusementGroupBookingRow[]>("/api/v1/amusement/group-bookings"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  async function releaseBooking() {
    const first = bookings[0];
    if (!first?.id) {
      setFormError("当前没有可释放的团体/包场预约。");
      return;
    }
    if (!releaseReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    setFormError("");
    setGlobalError(null);
    const request = { idempotencyKey: `group-booking-release-${Date.now()}`, reason: releaseReason, evidenceAttachmentIds: ["att-group-booking-release"] };
    const result = await api.post<unknown>(`/api/v1/amusement/group-bookings/${encodeURIComponent(first.id)}/release`, request);
    setActionResult({ action: "group_booking_released", evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
    await reload();
  }

  const reservedCapacity = bookings.reduce((sum, row) => sum + Number(row.reservedCapacity ?? 0), 0);

  return (
    <section data-testid="amusement-group-bookings-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="amusement-group-bookings-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="amusement_group_booking.viewed / amusement_group_booking.released" />
      {formError ? <div className="admin-inline-error" data-testid="amusement-group-bookings-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={bookings.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="amusement-group-bookings-summary">
          <div className="admin-kpi-card"><span>团体预约</span><strong>{bookings.length}</strong><small>bookings</small></div>
          <div className="admin-kpi-card"><span>容量占用</span><strong>{reservedCapacity}</strong><small>reserved</small></div>
          <div className="admin-kpi-card"><span>已审批</span><strong>{bookings.filter((row) => row.bookingStatus === "approved").length}</strong><small>approved</small></div>
          <div className="admin-kpi-card"><span>包场</span><strong>{bookings.filter((row) => row.bookingType === "private").length}</strong><small>private</small></div>
        </div>
        <div className="admin-form-grid"><label>释放原因<input data-testid="amusement-group-booking-release-reason" value={releaseReason} onChange={(event) => setReleaseReason(event.target.value)} /></label></div>
        <div className="admin-form-row"><button type="button" data-testid="amusement-group-booking-release" onClick={() => void releaseBooking()}>释放容量</button><span className="admin-muted-action" data-testid="amusement-group-bookings-boundary">{TEXT.boundary}</span></div>
        <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-group-bookings-table"><thead><tr><th>类型</th><th>客户</th><th>人数</th><th>容量占用</th><th>项目</th><th>场次</th><th>状态</th></tr></thead><tbody>{bookings.map((row) => <tr key={row.id}><td>{display(row.bookingType)}</td><td>{display(row.customerName)}</td><td>{display(row.partySize)}</td><td>{display(row.reservedCapacity)}</td><td>{display(row.projectId)}</td><td>{display(row.sessionId)}</td><td>{display(row.bookingStatus)}</td></tr>)}</tbody></table></div>
      </AdminDataState>
    </section>
  );
}
