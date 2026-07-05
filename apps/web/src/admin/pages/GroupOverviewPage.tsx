import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, GroupOverviewPayload, GroupProjectRollup } from "../types";

// P2 集团/跨项目汇总（R6-D13）：按 project 维 rollup 关键指标（收入/客流/核销/风险/商户数）。
// 当前单综合体——平台口径 ETL 快照即该 project 的跨商户汇总，故通常仅一行 project（不伪造多 project）。
// 真实拉 /api/v1/analytics/group-overview，空态诚实（无 ETL 快照时指标显示 — 并带 note）。
const METRIC_COLUMNS: Array<{ code: string; label: string; hint: string }> = [
  { code: "revenue.net", label: "净收入", hint: "售票+酒吧−退款（分）" },
  { code: "revenue.ticket", label: "票务收入", hint: "已支付订单实付（分）" },
  { code: "revenue.bar", label: "酒吧收入", hint: "已结算账单实收（分）" },
  { code: "traffic.amusement", label: "游乐客流", hint: "成功核销人次" },
  { code: "verify.done", label: "票务核销", hint: "成功核销数" },
  { code: "risk.pending_review", label: "待复核", hint: "复核 + 风险预警" },
];

function metricText(project: GroupProjectRollup, code: string): string {
  const metric = project.metrics?.[code];
  if (!metric || metric.value === null || metric.value === undefined) return "—";
  const parsed = Number(metric.value);
  return Number.isFinite(parsed) ? String(parsed) : "—";
}

function metricNote(project: GroupProjectRollup, code: string): string | null {
  const metric = project.metrics?.[code];
  if (!metric || metric.value !== null) return null;
  return metric.note ?? null;
}

function belowBenchmark(project: GroupProjectRollup, code: string): boolean {
  const dimension = project.metrics?.[code]?.dimension;
  return Boolean(project.belowBenchmarkMetrics?.includes(code) || dimension?.belowBenchmark || dimension?.below_benchmark);
}

export function GroupOverviewPage({ api, setGlobalError }: AdminPageProps) {
  const [data, setData] = React.useState<GroupOverviewPayload | null>(null);
  const [groupBy, setGroupBy] = React.useState<"project" | "entity">("project");
  // 行明细抽屉：整行可点 → 该项目全部汇总指标（列名中文·低于基准标注·商户数）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      setData(await api.get<GroupOverviewPayload>(`/api/v1/analytics/group-overview?groupBy=${groupBy}`));
    },
    { onError: setGlobalError, fallbackMessage: "集团总览读取失败", onStart: () => setGlobalError(null) },
  );

  const projects = data?.projects ?? [];

  function openProjectDetail(project: GroupProjectRollup) {
    setDetail({
      title: `项目汇总明细 · ${project.projectName}`,
      fields: [
        { label: "项目（综合体）", value: project.projectName },
        { label: "商户数", value: project.merchantCount },
        ...METRIC_COLUMNS.map((col) => ({
          label: col.label,
          value: `${metricText(project, col.code)}${belowBenchmark(project, col.code) ? "（低于集团基准）" : ""}`,
        })),
      ],
    });
  }

  return (
    <section data-testid="admin-group-overview">
      <AdminPageHeader
        title="集团总览"
        description="按项目或主体维度下钻汇总收入、客流、核销、风险与商户数；低于集团基准的指标用红点标识。"
      />
      <AdminDataState loading={loading} error={error} empty={projects.length === 0} onRetry={reload}>
        <div className="admin-inline-form" data-testid="group-overview-groupby-form">
          <select aria-label="集团总览维度" data-testid="group-overview-groupby" value={groupBy} onChange={(event) => setGroupBy(event.target.value as "project" | "entity")}>
            <option value="project">按项目</option>
            <option value="entity">按主体</option>
          </select>
          <button type="button" data-testid="group-overview-refresh" onClick={() => void reload()}>切换</button>
          <span className="admin-muted-action" data-testid="group-overview-current-groupby">当前：{data?.groupBy ?? groupBy}</span>
        </div>
        {data ? <p className="admin-muted-action" data-testid="group-overview-business-date">经营日：{data.businessDate}</p> : null}
        <table className="admin-table" data-testid="group-overview-table">
          <thead>
            <tr>
              <th>项目（综合体）</th>
              <th>商户数</th>
              {METRIC_COLUMNS.map((col) => (
                <th key={col.code} title={col.hint}>{col.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {projects.map((project) => (
              <tr key={project.projectId} className="admin-row-clickable" tabIndex={0} data-testid={`group-overview-row-${project.projectId}`}
                onClick={() => openProjectDetail(project)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(project); } }}>
                <td>{project.projectName}</td>
                <td data-testid={`group-overview-merchants-${project.projectId}`}>{project.merchantCount}</td>
                {METRIC_COLUMNS.map((col) => {
                  const note = metricNote(project, col.code);
                  return (
                    <td key={col.code} title={note ?? undefined}>
                      {belowBenchmark(project, col.code) ? <span className="admin-badge is-danger" data-testid={`group-overview-below-${project.projectId}-${col.code}`}>●</span> : null}
                      {metricText(project, col.code)}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
