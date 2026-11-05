// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { saveDownloadedFile } from "../client/adminClient";
import { cnDate } from "../components/RowDetailDrawer";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";

// 第十八批 T 段第 157 条 157-R7（BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和未结清数量。」；
// BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕」）：后台「订单/出品/账单」页的挂起订单风险看板。持查看订单码可看，
// 导出要单独授权的审计导出码（单次授权，导一次扣一次），只下载服务端生成的带密码 PDF。不设任何额度配置。
type Row = {
  orderId: string; billNo?: string; barId?: string; barName?: string | null; tableId?: string; tableName?: string | null; status: string;
  amountCent?: number; unpaidCent?: number; suspendedAt?: string; ageMinutes?: number;
  // 18T 复核修复④（BSM:1110）：本单已出品明细出品时刻的主责服务员逐人列出，userId 为空＝门店损耗；空列表＝尚无已出品明细。
  responsibleUsers?: { userId?: string | null; username?: string | null }[]; suspendedBy?: string;
};
type Board = { summary?: { unsettledCount?: number; unsettledAmountCent?: number; oldestUnsettledAgeMinutes?: number }; orders?: Row[] };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError">;

const yuan = (cent?: number) => `¥${((cent ?? 0) / 100).toFixed(2)}`;
function age(minutes?: number) {
  const value = minutes ?? 0;
  return value >= 60 ? `${Math.floor(value / 60)} 小时 ${value % 60} 分` : `${value} 分`;
}

export function BarSuspendedOrdersBoard({ api, effectivePermissions, setActionResult, setGlobalError }: Props) {
  const [board, setBoard] = React.useState<Board | null>(null);
  const [error, setError] = React.useState("");
  const [exporting, setExporting] = React.useState(false);
  const visible = hasPermissions(effectivePermissions, ["bar.order.view"]);
  React.useEffect(() => {
    if (!visible) return;
    // ponytail: 只取最近 100 单；汇总由后端按全部范围算，超过 100 单时列表要翻页再加。
    api.get<Board>("/api/v1/bar/suspended-orders?size=100").then(setBoard, (e) => setError(e instanceof Error ? e.message : "挂起订单风险看板读取失败"));
  }, [visible, api]);
  if (!visible) return null;

  async function exportBoard() {
    if (!api.getFile) return;
    setExporting(true);
    setError("");
    setGlobalError(null);
    try {
      saveDownloadedFile(await api.getFile("/api/v1/bar/suspended-orders/export"));
      setActionResult({ action: "bar_suspended_orders_export", status: "exported", message: "已导出（带密码的 PDF），打开密码请到「我的导出」查看" });
    } catch (exportError) {
      const message = exportError instanceof Error ? exportError.message : "挂起订单导出失败";
      setError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_suspended_orders_export", status: "failed", message });
    } finally {
      setExporting(false);
    }
  }

  const rows = board?.orders ?? [];
  const summary = board?.summary;
  return (
    <section data-testid="bar-suspended-orders-board" style={{ minWidth: 0 }}>
      <h3>挂起订单风险看板</h3>
      <div className="admin-kpi-grid">
        <div className="admin-kpi-card"><span>未结清数量</span><strong data-testid="bar-suspended-kpi-count">{summary?.unsettledCount ?? 0}</strong><small>已挂起待付款、取消审批中、坏账损耗处理中</small></div>
        <div className="admin-kpi-card"><span>未结清金额</span><strong data-testid="bar-suspended-kpi-amount">{yuan(summary?.unsettledAmountCent)}</strong><small>未结清订单的未收金额合计</small></div>
        <div className="admin-kpi-card"><span>最长账龄</span><strong data-testid="bar-suspended-kpi-age">{age(summary?.oldestUnsettledAgeMinutes)}</strong><small>未结清订单自挂起起算</small></div>
      </div>
      <div className="admin-form-row">
        <PermissionButton permissionsState={effectivePermissions} permissions={["audit.operation_log.export"]} data-testid="bar-suspended-export" disabled={exporting} deniedHint="导出须单独授权（审计导出）" onClick={() => void exportBoard()}>导出（带密码的 PDF）</PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="bar-suspended-error">{error}</div> : null}
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="bar-suspended-table">
          <thead><tr><th>账单号</th><th>酒吧</th><th>桌台</th><th>状态</th><th>挂起金额</th><th>未收金额</th><th>挂起时间</th><th>账龄</th><th>责任人</th><th>挂起人</th></tr></thead>
          <tbody>{rows.map((row) => (
            <tr key={row.orderId} data-testid={`bar-suspended-row-${row.orderId}`}>
              <td>{row.billNo ?? "—"}</td>
              <td>{row.barName ?? row.barId ?? "—"}</td>
              <td>{row.tableName ?? row.tableId ?? "—"}</td>
              <td>{KEY_VALUE_LABELS.suspension_status?.[row.status] ?? row.status}</td>
              <td>{yuan(row.amountCent)}</td>
              <td>{yuan(row.unpaidCent)}</td>
              <td>{cnDate(row.suspendedAt)}</td>
              <td>{age(row.ageMinutes)}</td>
              <td data-testid={`bar-suspended-responsible-${row.orderId}`}>{(row.responsibleUsers ?? []).map((r) => (r.userId ? (r.username ?? r.userId) : "门店损耗")).join("、")}</td>
              <td>{row.suspendedBy ?? "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
    </section>
  );
}
