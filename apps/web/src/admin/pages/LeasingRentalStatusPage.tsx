import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";

type RentalRow = { spaceId: string; spaceName: string; rentalStatus: string; color: string | null; daysToExpiry: number | null };

const STATUS_LABEL: Record<string, string> = { available: "可租", let: "已租", expiring: "即将到期" };

/**
 * 租赁状态看板（ADM-LEASE-002，P2-5）：按铺位展示 可租/已租/即将到期 + 距到期天数 + 出租率汇总。
 * 数据源 GET /api/v1/contracts/space-rental-status（即将到期套用着色分类色）。铺位可下钻到空间页。
 */
export function LeasingRentalStatusPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<RentalRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<RentalRow[]>("/api/v1/contracts/space-rental-status");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "租赁状态读取失败" },
  );

  const let_ = rows.filter((r) => r.rentalStatus === "let" || r.rentalStatus === "expiring").length;
  const total = rows.length;
  const occupancy = total === 0 ? "—" : `${((let_ / total) * 100).toFixed(1)}%`;

  return (
    <section data-testid="leasing-rental-status-page">
      <AdminPageHeader
        title="租赁状态看板"
        description="按铺位展示租赁状态（可租/已租/即将到期）与距到期天数，并汇总出租率。即将到期按着色分类高亮，便于招商续租跟进。"
      />
      <AdminDataState loading={loading} error={error} empty={total === 0} onRetry={reload}>
        <div className="admin-summary" data-testid="leasing-occupancy-summary">
          出租率：<strong>{occupancy}</strong>（已租/即将到期 {let_} / 总铺位 {total}）
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-rental-status-table">
            <thead><tr><th>铺位</th><th>状态</th><th>距到期（天）</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.spaceId} data-testid={`rental-status-${row.spaceId}`}
                    style={row.color ? { background: row.color + "22" } : undefined}>
                  <td><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.spaceId}>{row.spaceName || row.spaceId}</TraceLink></td>
                  <td data-testid={`rental-status-state-${row.spaceId}`}>{STATUS_LABEL[row.rentalStatus] ?? row.rentalStatus}</td>
                  <td>{row.daysToExpiry == null ? "-" : row.daysToExpiry}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
