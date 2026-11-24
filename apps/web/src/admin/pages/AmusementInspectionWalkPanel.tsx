// 文件版本：V0.02
import React from "react";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminApiClient } from "../types";

// 第十八批 BS 段第 373 条 373-R3、373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、
// 不得标记为已完成」；第十八批裁决 76「巡检员用工作手机按顺序扫码后记结果」）：安全巡检页新建弹窗里只读显示所选游乐项目的走点进度，
// 点位在工作手机上逐个扫码记录；项目配了点位时检查项取走完的点位，不再手填检查项说明。
// 第十八批 CF 段第 404 条 404-R2（BSM:1642「补录记录与原始记录并列展示，标明“人工补录”」）：主管复核通过的点位补录落的走点行（带补录单编号）标「人工补录」。

export type InspectionPoint = { seq: number; pointName: string; pointCode?: string };
export type InspectionWalkPoint = { seq: number; pointName: string; result: string; note?: string | null; checkedBy?: string; checkedAt?: string;
  backfillId?: string | null };
export type InspectionPointsView = { projectId?: string; points: InspectionPoint[]; walk: InspectionWalkPoint[] };

export function inspectionPointsPath(projectId: string): string {
  return `/api/v1/amusement/projects/${encodeURIComponent(projectId)}/inspection-points`;
}

/** 读该游乐项目的点位清单与本轮走点；没选项目、没配点位或读取失败为 null（新建表单照旧填检查项）。 */
export async function readInspectionWalk(api: AdminApiClient, projectId: string): Promise<InspectionPointsView | null> {
  if (!projectId) return null;
  try {
    const view = await api.get<InspectionPointsView>(inspectionPointsPath(projectId));
    if (!view || !Array.isArray(view.points) || view.points.length === 0) return null;
    return { ...view, walk: Array.isArray(view.walk) ? view.walk : [] };
  } catch {
    return null;
  }
}

export function walkComplete(view: InspectionPointsView): boolean {
  return view.points.every((point) => view.walk.some((row) => row.seq === point.seq));
}

export function AmusementInspectionWalkPanel({ view }: { view: InspectionPointsView }) {
  const done = new Map(view.walk.map((row) => [row.seq, row]));
  return (
    <div className="admin-preview" data-testid="amusement-inspection-walk">
      <span>本项目巡检须按点位顺序用工作手机逐个扫码记录，全部点位走完才能创建巡检（已记录 {view.walk.length}/{view.points.length}）。</span>
      <table className="admin-table">
        <thead><tr><th>序号</th><th>点位</th><th>结果</th><th>记录时间</th></tr></thead>
        <tbody>
          {view.points.map((point) => {
            const row = done.get(point.seq);
            return (
              <tr key={point.seq} data-testid={`amusement-inspection-walk-row-${point.seq}`}>
                <td>{point.seq}</td>
                <td>{point.pointName}</td>
                <td>{row ? `${row.result === "pass" ? "通过" : "不通过"}${row.backfillId ? "（人工补录）" : ""}` : "未记录"}</td>
                <td>{row ? cnDate(row.checkedAt) : "—"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
