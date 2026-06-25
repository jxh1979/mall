import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
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

function avgText(data: GroupOverviewPayload | null, code: string): string {
  const metric = data?.groupAverages?.[code];
  if (!metric || metric.value === null || metric.value === undefined) return "—";
  const parsed = Number(metric.value);
  return Number.isFinite(parsed) ? String(parsed) : "—";
}

export function GroupOverviewPage({ api, setGlobalError }: AdminPageProps) {
  const [data, setData] = React.useState<GroupOverviewPayload | null>(null);
  const [groupBy, setGroupBy] = React.useState<"project" | "entity">("project");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setData(await api.get<GroupOverviewPayload>(`/api/v1/analytics/group-overview?groupBy=${groupBy}`));
    },
    { onError: setGlobalError, fallbackMessage: "集团总览读取失败", onStart: () => setGlobalError(null) },
  );

  // 切换汇总维度时重拉（useAdminResource 仅挂载时拉一次；首渲染由其负责，故跳过）。
  const firstRender = React.useRef(true);
  React.useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return;
    }
    void reload();
  }, [groupBy, reload]);

  const projects = data?.projects ?? [];

  return (
    <section data-testid="admin-group-overview">
      <AdminPageHeader
        title="集团总览"
        description="按项目（综合体）或法律主体维度汇总收入、客流、核销、风险与商户数；低于集团均值的指标标红点（对标）。"
      />
      <div className="admin-inline-form" data-testid="group-overview-groupby">
        <label>汇总维度：</label>
        <select aria-label="汇总维度" data-testid="group-overview-groupby-select" value={groupBy}
          onChange={(e) => setGroupBy(e.target.value === "entity" ? "entity" : "project")}>
          <option value="project">按项目</option>
          <option value="entity">按法律主体</option>
        </select>
      </div>
      <AdminDataState loading={loading} error={error} empty={projects.length === 0} onRetry={reload}>
        {data ? <p className="admin-muted-action" data-testid="group-overview-business-date">经营日：{data.businessDate}</p> : null}
        <table className="admin-table" data-testid="group-overview-table">
          <thead>
            <tr>
              <th>{groupBy === "entity" ? "法律主体" : "项目（综合体）"}</th>
              <th>商户数</th>
              {METRIC_COLUMNS.map((col) => (
                <th key={col.code} title={col.hint}>{col.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {projects.map((project) => {
              const below = new Set(project.belowAverageMetrics ?? []);
              return (
                <tr key={project.projectId} data-testid={`group-overview-row-${project.projectId}`}>
                  <td>{project.projectName}</td>
                  <td data-testid={`group-overview-merchants-${project.projectId}`}>{project.merchantCount}</td>
                  {METRIC_COLUMNS.map((col) => {
                    const note = metricNote(project, col.code);
                    const isBelow = below.has(col.code);
                    return (
                      <td key={col.code} title={note ?? (isBelow ? "低于集团均值" : undefined)}
                        style={isBelow ? { color: "#c00" } : undefined}>
                        {metricText(project, col.code)}
                        {isBelow ? <span data-testid={`group-overview-below-${project.projectId}-${col.code}`}> ●</span> : null}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
            {projects.length > 1 ? (
              <tr data-testid="group-overview-average-row">
                <td>集团均值</td><td>—</td>
                {METRIC_COLUMNS.map((col) => <td key={col.code}>{avgText(data, col.code)}</td>)}
              </tr>
            ) : null}
          </tbody>
        </table>
      </AdminDataState>
    </section>
  );
}
