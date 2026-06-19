import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, BarReservationRow, BarTableRow } from "../types";

// 后端写权限码（BarController：/reservations/{id}/check-in-guest）
const BAR_CHECKIN_PERMISSION = "bar.table.guest_checkin";

export function BarTableSessionsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [tables, setTables] = React.useState<BarTableRow[]>([]);
  const [reservations, setReservations] = React.useState<BarReservationRow[]>([]);
  const { setSelectedId, selected: selectedReservation } = useSelectedRow(reservations);
  const [guestCount, setGuestCount] = React.useState("1");
  const [checkinError, setCheckinError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [tableRows, reservationRows] = await Promise.all([
        api.get<BarTableRow[]>("/api/v1/bar/tables"),
        api.get<BarReservationRow[]>("/api/v1/bar/reservations"),
        api.get("/api/v1/bar/orders"),
      ]);
      setTables(tableRows);
      setReservations(reservationRows);
    },
    { onError: setGlobalError, fallbackMessage: "卡座数据读取失败" },
  );

  // 选中预定所属卡座的人数上限，用于入场人数校验。
  // 预定未关联具体卡座时回退到首张卡座的限额，避免上限校验失效。
  const selectedTable = (selectedReservation
    ? tables.find((table) => table.id === selectedReservation.tableId)
    : undefined) ?? tables[0];

  function validateCheckIn() {
    if (!selectedReservation?.id) {
      setCheckinError("请先在预定列表中选择要登记入场的订台。");
      return false;
    }
    const limit = selectedTable?.guestLimit ?? selectedTable?.capacity ?? 0;
    const count = Number(guestCount);
    if (!Number.isFinite(count) || count <= 0) {
      setCheckinError("入场人数必须大于 0。");
      return false;
    }
    if (limit > 0 && count > limit) {
      setCheckinError(`人数超限：当前卡座最多允许 ${limit} 人。`);
      return false;
    }
    setCheckinError("");
    return true;
  }

  async function checkInGuest() {
    if (!selectedReservation?.id) {
      setCheckinError("请先在预定列表中选择要登记入场的订台。");
      return;
    }
    const count = Number(guestCount);
    setSubmitting(true);
    setCheckinError("");
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
      setCheckinError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_check_in", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="bar-table-sessions-page">
      <AdminPageHeader title="卡座与订台" description="卡座只免预定卡座所在酒吧、限定人数入场；转卡、合并、续卡必须受结账与审批约束。" />
      <AdminDataState loading={loading} error={error} empty={tables.length === 0 && reservations.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="bar-checkin-count" type="number" min={1} value={guestCount} onChange={(event) => setGuestCount(event.target.value)} placeholder="入场人数" />
          <HighRiskConfirm
            label="登记入场"
            impact="将对选中订台登记客人入场并落库，受卡座人数上限约束；转卡/合并/续卡仍需结账与主管审批。"
            disabled={submitting}
            precheck={validateCheckIn}
            onConfirm={() => void checkInGuest()}
            permissionsState={effectivePermissions}
            permissions={[BAR_CHECKIN_PERMISSION]}
            irreversible={false}
            data-testid="bar-checkin-submit"
          />
          <span data-testid="bar-transfer-action" className="admin-muted-action">转卡 / 合并 / 续卡：未结账需主管审批</span>
        </div>
        {checkinError ? <div data-testid="bar-checkin-error" className="admin-inline-error">{checkinError}</div> : null}

        <h3>订台预定</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-reservations-table">
            <thead><tr><th>选择</th><th>预定号</th><th>卡座</th><th>人数</th><th>定金状态</th><th>状态</th></tr></thead>
            <tbody>
              {reservations.length === 0 ? (
                <tr><td colSpan={6} className="admin-muted-action">暂无订台预定</td></tr>
              ) : reservations.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`bar-reservation-select-${row.id}`} checked={(selectedReservation?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.id}</td>
                  <td>{row.tableId ?? "-"}</td>
                  <td>{row.guestCount ?? "-"}</td>
                  <td>{row.depositStatus ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>卡座</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-table-sessions-table">
            <thead><tr><th>卡座</th><th>容量</th><th>状态</th></tr></thead>
            <tbody>{tables.map((row) => <tr key={row.id}><td>{row.tableCode ?? row.tableName}</td><td>{row.capacity ?? row.guestLimit} 人</td><td>{row.status}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
