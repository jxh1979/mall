import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { BarVenueSelect } from "../components/BarVenueSelect";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminPageProps, BarReservationRow, BarTableRow, BarTableSessionRow } from "../types";

const BAR_CHECKIN_PERMISSION = "bar.table.guest_checkin";
const BAR_TRANSFER_PERMISSION = "bar.table_session.transfer";
const BAR_MERGE_PERMISSION = "bar.table_session.merge";
const BAR_RENEW_PERMISSION = "bar.table_session.renew";
const BAR_INVITATION_PERMISSION = "bar.reservation.invitation.manage";
const BAR_TABLE_MANAGE_PERMISSION = "bar.table.manage";
const BAR_RESERVATION_CREATE_PERMISSION = "bar.reservation.create";
const BAR_SESSION_CREATE_PERMISSION = "bar.table_session.create";

function todayBusinessDate() {
  return new Date().toISOString().slice(0, 10);
}

function nextRenewedUntil(minutes: number) {
  return new Date(Date.now() + minutes * 60_000).toISOString();
}

export function BarTableSessionsPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [tables, setTables] = React.useState<BarTableRow[]>([]);
  const [reservations, setReservations] = React.useState<BarReservationRow[]>([]);
  const [sessions, setSessions] = React.useState<BarTableSessionRow[]>([]);
  const { setSelectedId, selected: selectedReservation } = useSelectedRow(reservations);
  const [selectedSessionId, setSelectedSessionId] = React.useState("");
  const [guestCount, setGuestCount] = React.useState("1");
  // dev 假值预填已去掉：目标卡座/目标会话必须由操作人显式填写，防误转误合。
  const [targetTableId, setTargetTableId] = React.useState("");
  const [mergedSessionId, setMergedSessionId] = React.useState("");
  const [renewMinutes, setRenewMinutes] = React.useState("60");
  const [actionReason, setActionReason] = React.useState("主管审批通过");
  const [invitationMemberId, setInvitationMemberId] = React.useState("");
  const [barError, setBarError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  // 引导式流程 phase-5：新建卡座收起为主按钮 + 分步引导弹窗（订台/开台/转合续等操作流不动）。
  const [tableCreateOpen, setTableCreateOpen] = React.useState(false);
  const [tableCreateError, setTableCreateError] = React.useState("");
  // 从零起单三表单：新建卡座 / 新建订台（代客）/ 开台。全站零英文，受权限按钮约束。
  const [newTable, setNewTable] = React.useState({ barId: "", tableName: "", tableType: "card_seat", capacity: "4", allowedFreeEntryCount: "0", minimumSpendYuan: "0" });
  const [newReservation, setNewReservation] = React.useState({ barId: "", tableId: "", guestCount: "2", salespersonUserId: "" });
  const [newSession, setNewSession] = React.useState({ barId: "", tableId: "", businessDate: todayBusinessDate(), guestCount: "2", salesStaffId: "", serviceStaffId: "" });
  // 行明细抽屉：三张业务表整行可点 → 完整字段（状态中文·开台/关台/预定时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openSessionDetail(row: BarTableSessionRow) {
    setDetail({
      title: `开台会话明细 · ${row.id}`,
      fields: [
        { label: "会话", value: row.id },
        { label: "卡座", value: row.tableId ?? "—" },
        { label: "所属酒吧", value: row.barId ?? "—" },
        { label: "营业日", value: cnDate(row.businessDate) },
        { label: "人数", value: row.guestCount ?? "—" },
        { label: "销售", value: row.salesStaffId ?? "—" },
        { label: "服务员", value: row.serviceStaffId ?? "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "版本", value: row.sessionVersion ?? "—" },
        { label: "开台时间", value: cnDate(row.openedAt) },
        { label: "关台时间", value: cnDate(row.closedAt) },
      ],
    });
  }

  function openReservationDetail(row: BarReservationRow) {
    setDetail({
      title: `订台预定明细 · ${row.id}`,
      fields: [
        { label: "预定号", value: row.id },
        { label: "卡座", value: row.tableId ?? "—" },
        { label: "所属酒吧", value: row.barId ?? "—" },
        { label: "客户", value: row.customerId ?? "—" },
        { label: "人数", value: row.guestCount ?? "—" },
        { label: "免通票入场人数", value: row.allowedPassFreeGuestCount ?? "—" },
        { label: "定金状态", value: formatBusinessStatus(row.depositStatus) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "预定时间", value: cnDate(row.reservedAt) },
        { label: "创建时间", value: cnDate(row.createdAt) },
      ],
    });
  }

  function openTableDetail(row: BarTableRow) {
    setDetail({
      title: `卡座明细 · ${row.tableCode ?? row.tableName ?? row.id}`,
      fields: [
        { label: "卡座编号", value: row.id },
        { label: "卡座", value: row.tableCode ?? row.tableName ?? "—" },
        { label: "所属酒吧", value: row.barId ?? "—" },
        { label: "容量", value: row.capacity ?? row.guestLimit ? `${row.capacity ?? row.guestLimit} 人` : "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [tableRows, reservationRows, sessionRows] = await Promise.all([
        api.get<BarTableRow[]>("/api/v1/bar/tables"),
        api.get<BarReservationRow[]>("/api/v1/bar/reservations"),
        api.get<BarTableSessionRow[]>("/api/v1/bar/table-sessions"),
        api.get("/api/v1/bar/orders"),
      ]);
      setTables(tableRows);
      setReservations(reservationRows);
      setSessions(sessionRows);
      setSelectedSessionId((current) => current || sessionRows[0]?.id || "");
    },
    { onError: setGlobalError, fallbackMessage: "卡座数据读取失败" },
  );

  const selectedTable = (selectedReservation
    ? tables.find((table) => table.id === selectedReservation.tableId)
    : undefined) ?? tables[0];
  const selectedSession = sessions.find((session) => session.id === selectedSessionId) ?? sessions[0];

  function fail(message: string) {
    setBarError(message);
    return false;
  }

  function validateCheckIn() {
    if (!selectedReservation?.id) return fail("请先在预定列表中选择要登记入场的订台。");
    const limit = selectedTable?.guestLimit ?? selectedTable?.capacity ?? 0;
    const count = Number(guestCount);
    if (!Number.isFinite(count) || count <= 0) return fail("入场人数必须大于 0。");
    if (limit > 0 && count > limit) return fail(`人数超限：当前卡座最多允许 ${limit} 人。`);
    setBarError("");
    return true;
  }

  function validateSessionAction(kind: "transfer" | "merge" | "renew") {
    if (!selectedSession?.id) return fail("请先选择要操作的开台会话。");
    if (!actionReason.trim()) return fail("转卡、合并、续卡必须填写原因。 ");
    if (kind === "transfer") {
      if (!targetTableId.trim()) return fail("请输入目标卡座 ID。");
      if (targetTableId.trim() === selectedSession.tableId) return fail("目标卡座不能与当前卡座相同。");
    }
    if (kind === "merge") {
      if (!mergedSessionId.trim()) return fail("请输入要合并的目标会话 ID。");
      if (mergedSessionId.trim() === selectedSession.id) return fail("不能把会话合并到自身。");
    }
    if (kind === "renew") {
      const minutes = Number(renewMinutes);
      if (!Number.isFinite(minutes) || minutes <= 0) return fail("续卡分钟数必须大于 0。");
    }
    setBarError("");
    return true;
  }

  async function checkInGuest() {
    if (!validateCheckIn() || !selectedReservation?.id) return;
    const count = Number(guestCount);
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/bar/reservations/${encodeURIComponent(selectedReservation.id)}/check-in-guest`, {
        guestCount: count,
        barId: selectedReservation.barId ?? selectedTable?.barId ?? "bar-main",
      });
      setActionResult({ action: "bar_check_in", status: "checked_in", reservationId: selectedReservation.id, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "入场登记失败";
      setBarError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_check_in", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  async function runSessionAction(kind: "transfer" | "merge" | "renew") {
    if (!validateSessionAction(kind) || !selectedSession?.id) return;
    setSubmitting(true);
    setGlobalError(null);
    try {
      const body = kind === "transfer"
        ? { targetTableId: targetTableId.trim(), reason: actionReason.trim(), requestedByUserId: "admin-web", sessionVersion: selectedSession.sessionVersion ?? 1 }
        : kind === "merge"
          ? { mergedSessionId: mergedSessionId.trim(), reason: actionReason.trim(), requestedByUserId: "admin-web", primarySessionVersion: selectedSession.sessionVersion ?? 1, mergedSessionVersion: 1 }
          : { renewedUntil: nextRenewedUntil(Number(renewMinutes)), additionalMinConsumptionCent: 0, additionalDepositAmountCent: 0, additionalServiceFeeCent: 0, reason: actionReason.trim(), requestedByUserId: "admin-web", sessionVersion: selectedSession.sessionVersion ?? 1 };
      const result = await api.post(`/api/v1/bar/table-sessions/${encodeURIComponent(selectedSession.id)}/${kind}`, body);
      setActionResult({ action: `bar_table_session_${kind}`, status: kind === "transfer" ? "transferred" : kind === "merge" ? "merged" : "renewed", sessionId: selectedSession.id, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "卡座会话操作失败";
      setBarError(message);
      setGlobalError(message);
      setActionResult({ action: `bar_table_session_${kind}`, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 新建卡座：可进人数（容量）独立于免通票入场人数；低消按元填、后端按分存。
  // 返回中文错误文案则校验不过，null=放行（供弹窗步内校验 + 提交双保险共用）。
  function newTableValidationMessage(): string | null {
    if (!newTable.barId.trim()) return "新建卡座请填写所属酒吧。";
    if (!newTable.tableName.trim()) return "新建卡座请填写卡座名称。";
    const cap = Number(newTable.capacity);
    if (!Number.isFinite(cap) || cap <= 0) return "可进人数必须大于 0。";
    const free = Number(newTable.allowedFreeEntryCount);
    if (!Number.isFinite(free) || free < 0) return "免通票入场人数不能为负。";
    const spend = Number(newTable.minimumSpendYuan);
    if (!Number.isFinite(spend) || spend < 0) return "低消金额不能为负。";
    return null;
  }

  async function createTable() {
    const message = newTableValidationMessage();
    if (message) {
      setTableCreateError(message);
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/bar/tables", {
        barId: newTable.barId.trim(),
        tableName: newTable.tableName.trim(),
        tableType: newTable.tableType,
        capacity: Number(newTable.capacity),
        allowedFreeEntryCount: Number(newTable.allowedFreeEntryCount),
        minimumSpendCent: Math.round(Number(newTable.minimumSpendYuan) * 100),
      });
      setActionResult({ action: "bar_table_create", status: "created", result });
      setTableCreateError("");
      setNewTable((prev) => ({ ...prev, tableName: "" }));
      setTableCreateOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新建卡座失败";
      setTableCreateError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_table_create", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 新建订台（代客）：order_source 由后端自动置为 staff（代客下单），前端只填卡座与人数。
  function validateNewReservation() {
    if (!newReservation.barId.trim()) return fail("新建订台请填写所属酒吧。");
    if (!newReservation.tableId.trim()) return fail("新建订台请填写卡座 ID。");
    const count = Number(newReservation.guestCount);
    if (!Number.isFinite(count) || count <= 0) return fail("订台人数必须大于 0。");
    setBarError("");
    return true;
  }

  async function createReservation() {
    if (!validateNewReservation()) return;
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/bar/reservations", {
        barId: newReservation.barId.trim(),
        tableId: newReservation.tableId.trim(),
        reservationTime: new Date().toISOString(),
        guestCount: Number(newReservation.guestCount),
        salespersonUserId: newReservation.salespersonUserId.trim() || undefined,
      });
      setActionResult({ action: "bar_reservation_create", status: "created", result });
      setBarError("");
      setNewReservation((prev) => ({ ...prev, tableId: "" }));
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新建订台失败";
      setBarError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_reservation_create", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 开台：为选定卡座新建开台会话（可选带营业日、销售/服务员）。
  function validateNewSession() {
    if (!newSession.barId.trim()) return fail("开台请填写所属酒吧。");
    if (!newSession.tableId.trim()) return fail("开台请填写卡座 ID。");
    if (!newSession.businessDate.trim()) return fail("开台请填写营业日。");
    const count = Number(newSession.guestCount);
    if (!Number.isFinite(count) || count <= 0) return fail("开台人数必须大于 0。");
    setBarError("");
    return true;
  }

  async function createSession() {
    if (!validateNewSession()) return;
    setSubmitting(true);
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/bar/table-sessions", {
        barId: newSession.barId.trim(),
        tableId: newSession.tableId.trim(),
        businessDate: newSession.businessDate,
        guestCount: Number(newSession.guestCount),
        salesStaffId: newSession.salesStaffId.trim() || undefined,
        serviceStaffId: newSession.serviceStaffId.trim() || undefined,
      });
      setActionResult({ action: "bar_table_session_create", status: "opened", result });
      setBarError("");
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "开台失败";
      setBarError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_table_session_create", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  // 邀请函签发/吊销前置校验：未选订台则不弹二次确认、不发请求（与 runInvitationAction 内守卫同口径）。
  function requireSelectedReservation(): boolean {
    if (!selectedReservation?.id) {
      setBarError("请先在预定列表中选择订台。");
      return false;
    }
    return true;
  }

  // V155 订卡邀请函：签发 / 吊销（staff）+ 门口入场核验（复用 guest_checkin 权限）。
  async function runInvitationAction(kind: "issue" | "revoke" | "entry") {
    if (!selectedReservation?.id) {
      setBarError("请先在预定列表中选择订台。");
      return;
    }
    if (kind === "entry" && !invitationMemberId.trim()) {
      setBarError("入场核验请先输入成员标识（朋友的客人 ID 或持函人标识）。");
      return;
    }
    setSubmitting(true);
    setGlobalError(null);
    try {
      const reservationId = selectedReservation.id;
      const barId = selectedReservation.barId ?? selectedTable?.barId ?? "bar-main";
      const path = kind === "issue"
        ? `/api/v1/bar/reservations/${encodeURIComponent(reservationId)}/invitation`
        : kind === "revoke"
          ? `/api/v1/bar/reservations/${encodeURIComponent(reservationId)}/invitation/revoke`
          : `/api/v1/bar/reservations/${encodeURIComponent(reservationId)}/invitation/entry-verify`;
      const body = kind === "entry"
        ? { credentialPayload: invitationMemberId.trim(), pointId: barId, businessContext: { barId } }
        : {};
      const result = await api.post(path, body);
      setActionResult({ action: `bar_invitation_${kind}`, reservationId, result });
      setBarError("");
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "邀请函操作失败";
      setBarError(message);
      setGlobalError(message);
      setActionResult({ action: `bar_invitation_${kind}`, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  const pagedSessions = usePagedRows(sessions, 10);
  const pagedReservations = usePagedRows(reservations, 10);

  return (
    <section data-testid="bar-table-sessions-page">
      {focusObjectId && (
        <div className="admin-state" data-testid="bar-table-sessions-focus">已从下钻定位到桌位：{focusObjectId}</div>
      )}
      <AdminPageHeader title="卡座与订台" description="卡座只免预定卡座所在酒吧、限定人数入场；转卡、合并、续卡必须受结账与审批约束。" />

      {/* 从零起单入口：即便零数据也常驻可见（放在空态短路之外），支持新建卡座 / 新建订台（代客）/ 开台。 */}
      <h3>从零起单</h3>
      <div className="admin-form-row" data-testid="bar-create-table-form">
        <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_TABLE_MANAGE_PERMISSION]} disabled={submitting} onClick={() => { setTableCreateError(""); setTableCreateOpen(true); }} data-testid="bar-new-table-open">＋ 新建卡座</PermissionButton>
        <span className="admin-muted-action">卡座为配置主数据：可进人数独立于免通票入场人数，低消按元填。</span>
      </div>
      <GuidedFlowModal
        open={tableCreateOpen}
        title="新建卡座"
        testId="bar-new-table-guided"
        submitTestId="bar-new-table-submit"
        submitLabel="新建卡座"
        submitting={submitting}
        error={tableCreateError || undefined}
        onClose={() => setTableCreateOpen(false)}
        onSubmit={() => void createTable()}
        steps={[
          {
            title: "卡座信息",
            description: "填写所属酒吧与卡座名称。",
            validate: () => {
              if (!newTable.barId.trim()) return "新建卡座请填写所属酒吧。";
              if (!newTable.tableName.trim()) return "新建卡座请填写卡座名称。";
              return null;
            },
            content: (
              <>
                <label>所属酒吧<BarVenueSelect api={api} testId="bar-new-table-bar" value={newTable.barId} onChange={(v) => setNewTable((p) => ({ ...p, barId: v }))} autoSelectFirst setGlobalError={setGlobalError} /></label>
                <label>卡座名称<input data-testid="bar-new-table-name" value={newTable.tableName} onChange={(event) => setNewTable((p) => ({ ...p, tableName: event.target.value }))} placeholder="如 VIP-01" /></label>
                <label>桌台类型
                  <select data-testid="bar-new-table-type" aria-label="桌台类型" value={newTable.tableType} onChange={(event) => setNewTable((p) => ({ ...p, tableType: event.target.value }))}>
                    <option value="card_seat">卡座（出品走接单→制作→备好→送达流转）</option>
                    <option value="vip">VIP 包坐（同卡座流转）</option>
                    <option value="bar_counter">吧台位（直点即点即取，不走出品流转）</option>
                  </select>
                </label>
              </>
            ),
          },
          {
            title: "容量与低消",
            description: "填写可进人数、免通票入场人数与低消金额。",
            validate: () => newTableValidationMessage(),
            content: (
              <>
                <label>可进人数<input data-testid="bar-new-table-capacity" type="number" min={1} value={newTable.capacity} onChange={(event) => setNewTable((p) => ({ ...p, capacity: event.target.value }))} placeholder="容量" /></label>
                <label>免通票入场人数<input data-testid="bar-new-table-free" type="number" min={0} value={newTable.allowedFreeEntryCount} onChange={(event) => setNewTable((p) => ({ ...p, allowedFreeEntryCount: event.target.value }))} placeholder="免票人数" /></label>
                <label>低消（元）<input data-testid="bar-new-table-spend" type="number" min={0} value={newTable.minimumSpendYuan} onChange={(event) => setNewTable((p) => ({ ...p, minimumSpendYuan: event.target.value }))} placeholder="低消" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "所属酒吧", value: newTable.barId.trim() || "未填写" },
          { label: "卡座名称", value: newTable.tableName.trim() || "未填写" },
          { label: "桌台类型", value: newTable.tableType === "bar_counter" ? "吧台位（不走出品流转）" : newTable.tableType === "vip" ? "VIP 包坐" : "卡座" },
          { label: "可进人数", value: newTable.capacity.trim() || "未填写" },
          { label: "免通票入场人数", value: newTable.allowedFreeEntryCount.trim() || "0" },
          { label: "低消（元）", value: newTable.minimumSpendYuan.trim() || "0" },
        ]}
      />
      <div className="admin-form-row" data-testid="bar-create-reservation-form">
        <label>所属酒吧<BarVenueSelect api={api} testId="bar-new-reservation-bar" value={newReservation.barId} onChange={(v) => setNewReservation((p) => ({ ...p, barId: v }))} autoSelectFirst setGlobalError={setGlobalError} /></label>
        <label>卡座 ID<input data-testid="bar-new-reservation-table" value={newReservation.tableId} onChange={(event) => setNewReservation((p) => ({ ...p, tableId: event.target.value }))} placeholder="要订的卡座 ID" /></label>
        <label>人数<input data-testid="bar-new-reservation-guests" type="number" min={1} value={newReservation.guestCount} onChange={(event) => setNewReservation((p) => ({ ...p, guestCount: event.target.value }))} placeholder="人数" /></label>
        <label>销售员（选填）<input data-testid="bar-new-reservation-sales" value={newReservation.salespersonUserId} onChange={(event) => setNewReservation((p) => ({ ...p, salespersonUserId: event.target.value }))} placeholder="销售员 ID" /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_RESERVATION_CREATE_PERMISSION]} disabled={submitting} onClick={() => void createReservation()} data-testid="bar-new-reservation-submit">新建订台（代客）</PermissionButton>
      </div>
      <div className="admin-form-row" data-testid="bar-create-session-form">
        <label>所属酒吧<BarVenueSelect api={api} testId="bar-new-session-bar" value={newSession.barId} onChange={(v) => setNewSession((p) => ({ ...p, barId: v }))} autoSelectFirst setGlobalError={setGlobalError} /></label>
        <label>卡座 ID<input data-testid="bar-new-session-table" value={newSession.tableId} onChange={(event) => setNewSession((p) => ({ ...p, tableId: event.target.value }))} placeholder="要开台的卡座 ID" /></label>
        <label>营业日<input data-testid="bar-new-session-date" type="date" value={newSession.businessDate} onChange={(event) => setNewSession((p) => ({ ...p, businessDate: event.target.value }))} /></label>
        <label>人数<input data-testid="bar-new-session-guests" type="number" min={1} value={newSession.guestCount} onChange={(event) => setNewSession((p) => ({ ...p, guestCount: event.target.value }))} placeholder="人数" /></label>
        <label>销售（选填）<input data-testid="bar-new-session-sales" value={newSession.salesStaffId} onChange={(event) => setNewSession((p) => ({ ...p, salesStaffId: event.target.value }))} placeholder="销售 ID" /></label>
        <label>服务员（选填）<input data-testid="bar-new-session-service" value={newSession.serviceStaffId} onChange={(event) => setNewSession((p) => ({ ...p, serviceStaffId: event.target.value }))} placeholder="服务员 ID" /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_SESSION_CREATE_PERMISSION]} disabled={submitting} onClick={() => void createSession()} data-testid="bar-new-session-submit">开台</PermissionButton>
      </div>
      {barError ? <div data-testid="bar-create-error" className="admin-inline-error">{barError}</div> : null}

      <AdminDataState loading={loading} error={error} empty={tables.length === 0 && reservations.length === 0 && sessions.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <label>入场人数（人）<input data-testid="bar-checkin-count" type="number" min={1} value={guestCount} onChange={(event) => setGuestCount(event.target.value)} placeholder="入场人数" /></label>
          <HighRiskConfirm
            label="登记入场"
            impact="将对选中订台登记客人入场并落库，受卡座人数上限约束。"
            disabled={submitting}
            precheck={validateCheckIn}
            onConfirm={() => void checkInGuest()}
            permissionsState={effectivePermissions}
            permissions={[BAR_CHECKIN_PERMISSION]}
            irreversible={false}
            data-testid="bar-checkin-submit"
          />
          <label>转/合/续原因<input data-testid="bar-session-reason" value={actionReason} onChange={(event) => setActionReason(event.target.value)} placeholder="必填，留痕用" /></label>
          <label>目标卡座<input data-testid="bar-transfer-target-table" value={targetTableId} onChange={(event) => setTargetTableId(event.target.value)} placeholder="转入的卡座 ID" /></label>
          <HighRiskConfirm label="转卡" impact="将把选中开台会话转移到目标卡座，必须确认未结账约束和主管审批。" disabled={submitting} precheck={() => validateSessionAction("transfer")} onConfirm={() => void runSessionAction("transfer")} permissionsState={effectivePermissions} permissions={[BAR_TRANSFER_PERMISSION]} data-testid="bar-transfer-submit" />
          <label>合并目标会话<input data-testid="bar-merge-target-session" value={mergedSessionId} onChange={(event) => setMergedSessionId(event.target.value)} placeholder="被并入的会话 ID" /></label>
          <HighRiskConfirm label="合并" impact="将把选中会话与目标会话合并，账单和责任链必须留痕。" disabled={submitting} precheck={() => validateSessionAction("merge")} onConfirm={() => void runSessionAction("merge")} permissionsState={effectivePermissions} permissions={[BAR_MERGE_PERMISSION]} data-testid="bar-merge-submit" />
          <label>续卡时长（分钟）<input data-testid="bar-renew-minutes" type="number" min={1} value={renewMinutes} onChange={(event) => setRenewMinutes(event.target.value)} placeholder="续卡分钟" /></label>
          <HighRiskConfirm label="续卡" impact="将延长选中会话并记录追加低消/服务费快照。" disabled={submitting} precheck={() => validateSessionAction("renew")} onConfirm={() => void runSessionAction("renew")} permissionsState={effectivePermissions} permissions={[BAR_RENEW_PERMISSION]} data-testid="bar-renew-submit" />
        </div>
        {barError ? <div data-testid="bar-checkin-error" className="admin-inline-error">{barError}</div> : null}

        <h3>订卡邀请函</h3>
        <p className="admin-muted-action">对选中订台签发邀请函（持函人转发给朋友，朋友客人端确认占名额）；门口扫码核验成员入场，总人数受卡座可进人数约束。</p>
        <div className="admin-form-row">
          <HighRiskConfirm label="签发邀请函" impact="将对选中订台签发订卡邀请函，持函人可转发给朋友占用入场名额。" disabled={submitting} precheck={() => requireSelectedReservation()} onConfirm={() => void runInvitationAction("issue")} permissionsState={effectivePermissions} permissions={[BAR_INVITATION_PERMISSION]} irreversible={false} data-testid="bar-invitation-issue" />
          <HighRiskConfirm label="吊销邀请函" impact="将吊销选中订台的邀请函，已转发链接立即失效、未入场的朋友名额被收回。" disabled={submitting} precheck={() => requireSelectedReservation()} onConfirm={() => void runInvitationAction("revoke")} permissionsState={effectivePermissions} permissions={[BAR_INVITATION_PERMISSION]} data-testid="bar-invitation-revoke" />
          <input data-testid="bar-invitation-member" value={invitationMemberId} onChange={(event) => setInvitationMemberId(event.target.value)} placeholder="入场成员标识（朋友客人ID/持函人标识）" />
          <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_CHECKIN_PERMISSION]} disabled={submitting} onClick={() => void runInvitationAction("entry")} data-testid="bar-invitation-entry">门口入场核验</PermissionButton>
        </div>

        <h3>开台会话</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-table-sessions-table">
            <thead><tr><th>选择</th><th>会话</th><th>卡座</th><th>人数</th><th>状态</th><th>版本</th><th>详情</th></tr></thead>
            <tbody>{sessions.length === 0 ? (
              <tr><td colSpan={7} className="admin-muted-action">暂无开台会话</td></tr>
            ) : pagedSessions.pageRows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-session-row-${row.id}`}
                onClick={() => openSessionDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSessionDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择会话 ${row.id}`} data-testid={`bar-session-select-${row.id}`} checked={(selectedSession?.id ?? "") === row.id} onChange={() => setSelectedSessionId(row.id)} /></td>
                <td>{row.id}</td>
                <td>{row.tableId ?? "-"}</td>
                <td>{row.guestCount ?? "-"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td>{row.sessionVersion ?? "-"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看开台会话详情" onClick={(event) => { event.stopPropagation(); openSessionDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="bar-sessions-pager" page={pagedSessions.page} pageCount={pagedSessions.pageCount} total={pagedSessions.total} onPrev={pagedSessions.prev} onNext={pagedSessions.next} />
        </div>

        <h3>订台预定</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-reservations-table">
            <thead><tr><th>选择</th><th>预定号</th><th>卡座</th><th>人数</th><th>定金状态</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{reservations.length === 0 ? (
              <tr><td colSpan={7} className="admin-muted-action">暂无订台预定</td></tr>
            ) : pagedReservations.pageRows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-reservation-row-${row.id}`}
                onClick={() => openReservationDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openReservationDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择预定 ${row.id}`} data-testid={`bar-reservation-select-${row.id}`} checked={(selectedReservation?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{row.id}</td>
                <td>{row.tableId ?? "-"}</td>
                <td>{row.guestCount ?? "-"}</td>
                <td>{formatBusinessStatus(row.depositStatus)}</td>
                <td>{formatBusinessStatus(row.status)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看订台预定详情" onClick={(event) => { event.stopPropagation(); openReservationDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
          <TablePager testid="bar-reservations-pager" page={pagedReservations.page} pageCount={pagedReservations.pageCount} total={pagedReservations.total} onPrev={pagedReservations.prev} onNext={pagedReservations.next} />
        </div>

        <h3>卡座</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-tables-table">
            <thead><tr><th>卡座</th><th>容量</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{tables.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-table-row-${row.id}`} onClick={() => openTableDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTableDetail(row); } }}><td>{row.tableCode ?? row.tableName}</td><td>{row.capacity ?? row.guestLimit} 人</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看卡座详情" onClick={(event) => { event.stopPropagation(); openTableDetail(row); }}>查看详情</button></td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
