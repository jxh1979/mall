import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { formatBusinessStatus } from "../i18n/statusLabels";
import type { AdminPageProps, AmusementGroupBookingRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 后端写权限码（AmusementController：/group-bookings/{id}/release）
const GROUP_BOOKING_MANAGE_PERMISSION = "amusement.group_booking.manage";

const TEXT = {
  title: "团体/包场",
  description: "查看团体预约和包场资源占用，支持释放容量并保留操作原因。",
  evidence: "接口证据：读取 /api/v1/amusement/group-bookings；释放调用 /api/v1/amusement/group-bookings/{id}/release。",
  boundary: "团体/包场属于容量占用增强能力；释放容量必须写明原因，不得静默覆盖预约和核销证据。",
  loadFailed: "团体/包场读取失败",
  reasonRequired: "释放原因不能为空。",
  select: "选择",
  focusClear: "清除筛选，返回全部",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

// KPI 聚焦视图（照样板）：单表页，只有一张预约表；?kpi=bookings[&status=approved]。
// 团体预约/容量占用卡→全部预约，已审批卡→status=approved 客户端过滤。选中预约卡无计数、不可点。
type KpiFocus = { status: string | null };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "bookings") return null;
  return { status: params.get("status") };
}

export function AmusementGroupBookingsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [bookings, setBookings] = React.useState<AmusementGroupBookingRow[]>([]);
  const { setSelectedId, selected } = useSelectedRow(bookings);
  const [releaseReason, setReleaseReason] = React.useState("完整后台验收释放");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该团体/包场预约完整字段（类型/状态中文·时间中文·引用可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(status: string | null = null) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "bookings");
    if (status) params.set("status", status);
    else params.delete("status");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ status });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("status");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      // AUD-013: 后端已改真分页(默认 50)。本页仍客户端分页 → 显式取一页上限(后端 size 封顶 200)。
      // ponytail: 200 上限, 超量需改 useServerPagedRows+getPaged 服务端分页(并加汇总端点还原 KPI 全量口径)。
      setBookings(await api.get<AmusementGroupBookingRow[]>("/api/v1/amusement/group-bookings?size=200"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  function validateRelease() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要释放的团体/包场预约。");
      return false;
    }
    if (!releaseReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return false;
    }
    setFormError("");
    return true;
  }

  async function releaseBooking() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要释放的团体/包场预约。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const request = { idempotencyKey: `group-booking-release-${selected.id}-${Date.now()}`, reason: releaseReason, evidenceAttachmentIds: ["att-group-booking-release"] };
    try {
      const result = await api.post<unknown>(`/api/v1/amusement/group-bookings/${encodeURIComponent(selected.id)}/release`, request);
      setActionResult({ action: "group_booking_released", bookingId: selected.id, evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "团体/包场释放失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "group_booking_released", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  const reservedCapacity = bookings.reduce((sum, row) => sum + Number(row.reservedCapacity ?? 0), 0);

  // 已审批 KPI 聚焦时对预约表做客户端筛选：仅过滤已加载内存数组，不发后端查询。
  const visibleBookings = kpiFocus?.status ? bookings.filter((row) => row.bookingStatus === kpiFocus.status) : bookings;

  const pagedBookings = usePagedRows(visibleBookings, 10, `${kpiFocus?.status ?? ""}`);

  function kpiFocusTitle(): string {
    if (kpiFocus?.status === "approved") return "已审批预约";
    return kpiFocus?.status ? `团体预约（${formatBusinessStatus(kpiFocus.status)}）` : "团体预约";
  }

  function openBookingDetail(row: AmusementGroupBookingRow) {
    setDetail({
      title: `团体/包场预约明细 · ${row.customerName ?? row.id}`,
      fields: [
        { label: "预约编号", value: row.id },
        { label: "类型", value: formatBusinessStatus(row.bookingType) },
        { label: "客户", value: display(row.customerName) },
        { label: "人数", value: display(row.partySize) },
        { label: "容量占用", value: display(row.reservedCapacity) },
        {
          label: "项目",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink>,
        },
        { label: "场次", value: display(row.sessionId) },
        { label: "状态", value: formatBusinessStatus(row.bookingStatus) },
        { label: "下单时间", value: cnDate(row.createdAt) },
        { label: "确认时间", value: cnDate(row.paidAt) },
      ],
    });
  }

  const bookingsSection = (
    <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-group-bookings-table"><thead><tr><th>{TEXT.select}</th><th>类型</th><th>客户</th><th>人数</th><th>容量占用</th><th>项目</th><th>场次</th><th>状态</th><th>下单时间</th><th>确认时间</th><th>详情</th></tr></thead><tbody>{pagedBookings.pageRows.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-group-booking-row-${row.id}`} onClick={() => openBookingDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBookingDetail(row); } }}><td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此团体预订 ${row.id}`} data-testid={`amusement-group-booking-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td><td>{formatBusinessStatus(row.bookingType)}</td><td>{display(row.customerName)}</td><td>{display(row.partySize)}</td><td>{display(row.reservedCapacity)}</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink></td><td>{display(row.sessionId)}</td><td>{formatBusinessStatus(row.bookingStatus)}</td><td>{cnDate(row.createdAt)}</td><td data-testid={`amusement-group-booking-paid-at-${row.id}`}>{cnDate(row.paidAt)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看团体/包场预约详情" onClick={(event) => { event.stopPropagation(); openBookingDetail(row); }}>查看详情</button></td></tr>)}</tbody></table><TablePager testid="amusement-group-bookings-pager" page={pagedBookings.page} pageCount={pagedBookings.pageCount} total={pagedBookings.total} onPrev={pagedBookings.prev} onNext={pagedBookings.next} /></div>
  );

  return (
    <section data-testid="amusement-group-bookings-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="amusement-group-bookings-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="amusement_group_booking.viewed / amusement_group_booking.released" />
      {formError ? <div className="admin-inline-error" data-testid="amusement-group-bookings-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={bookings.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="amusement-group-bookings-kpi-focus-banner">
              正在查看：{kpiFocusTitle()}（{visibleBookings.length}）
              <button type="button" data-testid="amusement-group-bookings-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {bookingsSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="amusement-group-bookings-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>团体预约</span><button type="button" className="admin-kpi-value" data-testid="amusement-group-bookings-kpi-total" onClick={() => enterKpiFocus()}>{bookings.length}</button><small>预约数</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>容量占用</span><button type="button" className="admin-kpi-value" data-testid="amusement-group-bookings-kpi-reserved" onClick={() => enterKpiFocus()}>{reservedCapacity}</button><small>占用容量</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已审批</span><button type="button" className="admin-kpi-value" data-testid="amusement-group-bookings-kpi-approved" onClick={() => enterKpiFocus("approved")}>{bookings.filter((row) => row.bookingStatus === "approved").length}</button><small>已审批数</small></div>
              <div className="admin-kpi-card"><span>选中预约</span><strong className="admin-kpi-value--text" title={selected?.customerName ?? selected?.id ?? "-"}>{selected?.customerName ?? selected?.id ?? "-"}</strong><small>{formatBusinessStatus(selected?.bookingStatus)}</small></div>
            </div>
            <div className="admin-form-grid"><label>释放原因<input data-testid="amusement-group-booking-release-reason" value={releaseReason} onChange={(event) => setReleaseReason(event.target.value)} /></label></div>
            <div className="admin-form-row">
              <HighRiskConfirm
                label="释放容量"
                impact="将释放选中团体/包场预约占用的容量并落库留痕，释放后该容量重新可售，不可静默覆盖既有预约与核销证据。"
                disabled={submitting}
                precheck={validateRelease}
                onConfirm={() => void releaseBooking()}
                permissionsState={effectivePermissions}
                permissions={[GROUP_BOOKING_MANAGE_PERMISSION]}
                data-testid="amusement-group-booking-release"
              />
              <span className="admin-muted-action" data-testid="amusement-group-bookings-boundary">{TEXT.boundary}</span>
            </div>
            {bookingsSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
