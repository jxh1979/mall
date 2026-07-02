import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BarReservationRow, BarTableRow, BarTableSessionRow } from "../types";

const BAR_CHECKIN_PERMISSION = "bar.table.guest_checkin";
const BAR_TRANSFER_PERMISSION = "bar.table_session.transfer";
const BAR_MERGE_PERMISSION = "bar.table_session.merge";
const BAR_RENEW_PERMISSION = "bar.table_session.renew";
const BAR_INVITATION_PERMISSION = "bar.reservation.invitation.manage";

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
  const [targetTableId, setTargetTableId] = React.useState("table-2");
  const [mergedSessionId, setMergedSessionId] = React.useState("session-2");
  const [renewMinutes, setRenewMinutes] = React.useState("60");
  const [actionReason, setActionReason] = React.useState("主管审批通过");
  const [invitationMemberId, setInvitationMemberId] = React.useState("");
  const [barError, setBarError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

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
      <AdminDataState loading={loading} error={error} empty={tables.length === 0 && reservations.length === 0 && sessions.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="bar-checkin-count" type="number" min={1} value={guestCount} onChange={(event) => setGuestCount(event.target.value)} placeholder="入场人数" />
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
          <input data-testid="bar-session-reason" value={actionReason} onChange={(event) => setActionReason(event.target.value)} placeholder="转/合/续原因" />
          <input data-testid="bar-transfer-target-table" value={targetTableId} onChange={(event) => setTargetTableId(event.target.value)} placeholder="目标卡座 ID" />
          <HighRiskConfirm label="转卡" impact="将把选中开台会话转移到目标卡座，必须确认未结账约束和主管审批。" disabled={submitting} precheck={() => validateSessionAction("transfer")} onConfirm={() => void runSessionAction("transfer")} permissionsState={effectivePermissions} permissions={[BAR_TRANSFER_PERMISSION]} data-testid="bar-transfer-submit" />
          <input data-testid="bar-merge-target-session" value={mergedSessionId} onChange={(event) => setMergedSessionId(event.target.value)} placeholder="合并目标会话 ID" />
          <HighRiskConfirm label="合并" impact="将把选中会话与目标会话合并，账单和责任链必须留痕。" disabled={submitting} precheck={() => validateSessionAction("merge")} onConfirm={() => void runSessionAction("merge")} permissionsState={effectivePermissions} permissions={[BAR_MERGE_PERMISSION]} data-testid="bar-merge-submit" />
          <input data-testid="bar-renew-minutes" type="number" min={1} value={renewMinutes} onChange={(event) => setRenewMinutes(event.target.value)} placeholder="续卡分钟" />
          <HighRiskConfirm label="续卡" impact="将延长选中会话并记录追加低消/服务费快照。" disabled={submitting} precheck={() => validateSessionAction("renew")} onConfirm={() => void runSessionAction("renew")} permissionsState={effectivePermissions} permissions={[BAR_RENEW_PERMISSION]} data-testid="bar-renew-submit" />
        </div>
        {barError ? <div data-testid="bar-checkin-error" className="admin-inline-error">{barError}</div> : null}

        <h3>订卡邀请函</h3>
        <p className="admin-muted-action">对选中订台签发邀请函（持函人转发给朋友，朋友客人端确认占名额）；门口扫码核验成员入场，总人数受卡座可进人数约束。</p>
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_INVITATION_PERMISSION]} disabled={submitting} onClick={() => void runInvitationAction("issue")} data-testid="bar-invitation-issue">签发邀请函</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_INVITATION_PERMISSION]} disabled={submitting} onClick={() => void runInvitationAction("revoke")} data-testid="bar-invitation-revoke">吊销邀请函</PermissionButton>
          <input data-testid="bar-invitation-member" value={invitationMemberId} onChange={(event) => setInvitationMemberId(event.target.value)} placeholder="入场成员标识（朋友客人ID/持函人标识）" />
          <PermissionButton permissionsState={effectivePermissions} permissions={[BAR_CHECKIN_PERMISSION]} disabled={submitting} onClick={() => void runInvitationAction("entry")} data-testid="bar-invitation-entry">门口入场核验</PermissionButton>
        </div>

        <h3>开台会话</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-table-sessions-table">
            <thead><tr><th>选择</th><th>会话</th><th>卡座</th><th>人数</th><th>状态</th><th>版本</th></tr></thead>
            <tbody>{sessions.length === 0 ? (
              <tr><td colSpan={6} className="admin-muted-action">暂无开台会话</td></tr>
            ) : pagedSessions.pageRows.map((row) => (
              <tr key={row.id}>
                <td><input type="radio" data-testid={`bar-session-select-${row.id}`} checked={(selectedSession?.id ?? "") === row.id} onChange={() => setSelectedSessionId(row.id)} /></td>
                <td>{row.id}</td>
                <td>{row.tableId ?? "-"}</td>
                <td>{row.guestCount ?? "-"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td>{row.sessionVersion ?? "-"}</td>
              </tr>
            ))}</tbody>
          </table>
          <TablePager testid="bar-sessions-pager" page={pagedSessions.page} pageCount={pagedSessions.pageCount} total={pagedSessions.total} onPrev={pagedSessions.prev} onNext={pagedSessions.next} />
        </div>

        <h3>订台预定</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-reservations-table">
            <thead><tr><th>选择</th><th>预定号</th><th>卡座</th><th>人数</th><th>定金状态</th><th>状态</th></tr></thead>
            <tbody>{reservations.length === 0 ? (
              <tr><td colSpan={6} className="admin-muted-action">暂无订台预定</td></tr>
            ) : pagedReservations.pageRows.map((row) => (
              <tr key={row.id}>
                <td><input type="radio" data-testid={`bar-reservation-select-${row.id}`} checked={(selectedReservation?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{row.id}</td>
                <td>{row.tableId ?? "-"}</td>
                <td>{row.guestCount ?? "-"}</td>
                <td>{row.depositStatus ?? "-"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
              </tr>
            ))}</tbody>
          </table>
          <TablePager testid="bar-reservations-pager" page={pagedReservations.page} pageCount={pagedReservations.pageCount} total={pagedReservations.total} onPrev={pagedReservations.prev} onNext={pagedReservations.next} />
        </div>

        <h3>卡座</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-tables-table">
            <thead><tr><th>卡座</th><th>容量</th><th>状态</th></tr></thead>
            <tbody>{tables.map((row) => <tr key={row.id}><td>{row.tableCode ?? row.tableName}</td><td>{row.capacity ?? row.guestLimit} 人</td><td>{row.status}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
