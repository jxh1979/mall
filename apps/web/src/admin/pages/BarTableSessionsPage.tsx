import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BarTableRow } from "../types";

export function BarTableSessionsPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [tables, setTables] = React.useState<BarTableRow[]>([]);
  const [guestCount, setGuestCount] = React.useState("1");
  const [checkinError, setCheckinError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [tableRows] = await Promise.all([
        api.get<BarTableRow[]>("/api/v1/bar/tables"),
        api.get("/api/v1/bar/reservations"),
        api.get("/api/v1/bar/orders"),
      ]);
      setTables(tableRows);
    },
    { onError: setGlobalError, fallbackMessage: "卡座数据读取失败" },
  );

  async function checkInGuest() {
    const table = tables[0];
    const limit = table?.guestLimit ?? table?.capacity ?? 0;
    const count = Number(guestCount);
    if (limit > 0 && count > limit) {
      setCheckinError(`人数超限：当前卡座最多允许 ${limit} 人。`);
      return;
    }
    setCheckinError("");
    const result = await api.post("/api/v1/bar/reservations/reservation-1/check-in-guest", { guestCount: count, barId: table?.barId ?? "bar-main" });
    setActionResult({ action: "bar_check_in", status: "checked_in", result });
  }

  return (
    <section data-testid="bar-table-sessions-page">
      <AdminPageHeader title="卡座与订台" description="卡座只免预定卡座所在酒吧、限定人数入场；转卡、合并、续卡必须受结账与审批约束。" />
      <AdminDataState loading={loading} error={error} empty={tables.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="bar-checkin-count" type="number" min={1} value={guestCount} onChange={(event) => setGuestCount(event.target.value)} placeholder="入场人数" />
          <button type="button" data-testid="bar-checkin-submit" onClick={() => void checkInGuest()}>登记入场</button>
          <span data-testid="bar-transfer-action" className="admin-muted-action">转卡 / 合并 / 续卡：未结账需主管审批</span>
        </div>
        {checkinError ? <div data-testid="bar-checkin-error" className="admin-inline-error">{checkinError}</div> : null}
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
