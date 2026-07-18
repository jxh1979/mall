import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";

// 巡检到期待办（V143 amusement_inspection_tasks，由 AmusementInspectionTaskJob 按设备安全检查周期预生成；camelCase 化后口径）。
type InspectionTaskRow = {
  id: string;
  projectId: string;
  deviceId: string | null;
  dueAt: string;
  status: string;
};

// 状态中文：逾期高亮，其余（待巡检等）走全局码表兜底，杜绝英文枚举直出。
function statusLabel(status: string): string {
  return status === "pending" ? "待巡检" : formatBusinessStatus(status);
}

/**
 * ADM-AMU-004 巡检到期任务（T3.4，人身安全）：只读列出游乐设备巡检到期/逾期待办（按到期升序，逾期高亮）。
 * 任务由系统按设备安全检查周期自动生成；具体巡检处置走「安全巡检/事故」页的巡检创建流程。
 */
export function AmusementInspectionTasksPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<InspectionTaskRow[]>([]);
  // 行明细抽屉：整行可点 → 该巡检待办完整字段（时间中文·状态中文·引用可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<InspectionTaskRow[]>("/api/v1/amusement/inspection-tasks");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "巡检到期任务读取失败" },
  );

  function openTaskDetail(row: InspectionTaskRow) {
    setDetail({
      title: `巡检到期任务明细 · ${row.id}`,
      fields: [
        { label: "任务编号", value: row.id },
        {
          label: "项目",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId}</TraceLink>,
        },
        { label: "设备", value: row.deviceId ?? "（项目级）" },
        { label: "到期时间", value: cnDate(row.dueAt) },
        { label: "状态", value: statusLabel(row.status) },
      ],
    });
  }

  return (
    <section data-testid="amusement-inspection-tasks-page">
      <AdminPageHeader
        title="巡检到期任务"
        description="游乐设备安全巡检到期/逾期待办，系统按各设备安全检查周期自动生成。逾期项须尽快安排巡检——人身安全优先；巡检处置在「安全巡检/事故」页发起。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inspection-tasks-table">
            <thead>
              <tr>
                <th>项目</th>
                <th>设备</th>
                <th>到期时间</th>
                <th>状态</th>
              <th>详情</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`inspection-task-${row.id}`}
                  onClick={() => openTaskDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTaskDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId}</TraceLink></td>
                  <td>{row.deviceId ?? "（项目级）"}</td>
                  <td>{cnDate(row.dueAt)}</td>
                  <td>
                    {row.status === "overdue" ? (
                      <strong style={{ color: "#c0392b" }}>逾期</strong>
                    ) : (
                      statusLabel(row.status)
                    )}
                  </td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检到期任务详情" onClick={(event) => { event.stopPropagation(); openTaskDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
