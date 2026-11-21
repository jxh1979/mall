import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

type RentalRow = {
  spaceId: string; spaceName: string; rentalStatus: string; color: string | null; daysToExpiry: number | null;
  projectId?: string | null; spaceType?: string | null; areaSqm?: number | string | null;
  signedPendingContractId?: string | null; signedPendingEffectiveDate?: string | null;
};

// 六种铺位租赁状态（ENUM:259；PGM:287 展示一律中文）。
const STATUS_LABEL: Record<string, string> = {
  vacant: "空置", negotiating: "洽谈中", let: "已租", expiring: "即将到期", moved_out: "已退租", unavailable: "不可租",
};
const cnStatus = (code: string) => STATUS_LABEL[code] ?? code;

function sqm(value: number) {
  return `${new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value)} ㎡`;
}

/** BSM:1829 铺位出租率＝已租铺位面积 ÷ 本项目拥有的铺位总面积：按项目分组，只算商铺和摊位，分母含不可租（同经营分析口径）。 */
function occupancyByProject(rows: RentalRow[]) {
  const byProject = new Map<string, { leased: number; total: number }>();
  for (const row of rows) {
    if (row.spaceType !== "shop" && row.spaceType !== "booth") continue;
    const key = row.projectId ?? "";
    const sum = byProject.get(key) ?? { leased: 0, total: 0 };
    const area = Number(row.areaSqm ?? 0) || 0;
    sum.total += area;
    if (row.rentalStatus === "let" || row.rentalStatus === "expiring") sum.leased += area;
    byProject.set(key, sum);
  }
  return [...byProject.entries()].sort(([a], [b]) => a.localeCompare(b));
}

/**
 * 租赁状态看板（ADM-LEASE-002，P2-5）：按铺位展示六种租赁状态（空置/洽谈中/已租/即将到期/已退租/不可租）+ 距到期天数
 * + 「已签待交」标注（关联合同与约定起租日）+ 按项目、按面积的出租率（第十八批 CE 段 403-R2）。
 * 数据源 GET /api/v1/contracts/space-rental-status（即将到期套用着色分类色）。铺位可下钻到空间页，已签待交合同可下钻到合同工作台。
 * 整行可点 → 行明细抽屉（业务表，非配置）：铺位/状态/距到期/已签待交完整字段可溯源。
 */
export function LeasingRentalStatusPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<RentalRow[]>([]);
  // 行明细抽屉：整行可点 → 该铺位租赁状态完整字段（状态中文·距到期天数）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<RentalRow[]>("/api/v1/contracts/space-rental-status");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "租赁状态读取失败" },
  );

  const total = rows.length;
  const projects = occupancyByProject(rows);

  // ENUM:259「另加「已签待交」标注并关联合同与交付依据」；BSM:1347「交付日（即起租日）」。
  function signedPending(row: RentalRow) {
    if (!row.signedPendingContractId) return "—";
    return (
      <>
        已签待交 · 合同 <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.signedPendingContractId}>{row.signedPendingContractId}</TraceLink>
        {" · "}约定起租 {cnDate(row.signedPendingEffectiveDate)}
      </>
    );
  }

  function openRentalDetail(row: RentalRow) {
    setDetail({
      title: `铺位租赁明细 · ${row.spaceName || row.spaceId}`,
      fields: [
        {
          label: "铺位",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.spaceId}>{row.spaceName || row.spaceId}</TraceLink>,
        },
        { label: "状态", value: cnStatus(row.rentalStatus) },
        { label: "距到期（天）", value: row.daysToExpiry == null ? "—" : `${row.daysToExpiry} 天` },
        { label: "已签待交", value: signedPending(row) },
        { label: "铺位编号", value: row.spaceId },
      ],
    });
  }

  return (
    <section data-testid="leasing-rental-status-page">
      <AdminPageHeader
        title="租赁状态看板"
        description="按铺位展示租赁状态（空置、洽谈中、已租、即将到期、已退租、不可租）与距到期天数，已签约未交付的铺位单列「已签待交」并关联合同与约定起租日；出租率按项目、按面积计算（已租铺位面积 ÷ 本项目全部商铺和摊位面积，含不可租）。即将到期按着色分类高亮，便于招商续租跟进。"
      />
      <AdminDataState loading={loading} error={error} empty={total === 0} onRetry={reload}>
        <div className="admin-summary" data-testid="leasing-occupancy-summary">
          {projects.length === 0 ? <>出租率：<strong>—</strong></> : projects.map(([projectId, sum]) => (
            <div key={projectId} data-testid={`leasing-occupancy-${projectId}`}>
              {projects.length > 1 ? `项目 ${projectId} · ` : ""}出租率：<strong>{sum.total === 0 ? "—" : `${((sum.leased / sum.total) * 100).toFixed(1)}%`}</strong>
              （已租面积 {sqm(sum.leased)} / 铺位总面积 {sqm(sum.total)}）
            </div>
          ))}
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-rental-status-table">
            <thead><tr><th>铺位</th><th>状态</th><th>距到期（天）</th><th>已签待交</th><th>详情</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.spaceId} className="admin-row-clickable" tabIndex={0} data-testid={`rental-status-${row.spaceId}`}
                    style={row.color ? { background: row.color + "22" } : undefined}
                    onClick={() => openRentalDetail(row)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRentalDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.spaceId}>{row.spaceName || row.spaceId}</TraceLink></td>
                  <td data-testid={`rental-status-state-${row.spaceId}`}>{cnStatus(row.rentalStatus)}</td>
                  <td>{row.daysToExpiry == null ? "-" : row.daysToExpiry}</td>
                  <td data-testid={`rental-signed-pending-${row.spaceId}`} onClick={(event) => event.stopPropagation()}>{signedPending(row)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看铺位租赁详情" onClick={(event) => { event.stopPropagation(); openRentalDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
