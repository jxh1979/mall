// 文件版本：V0.02
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AdminDataState } from "../components/AdminDataState";
import { cnDate } from "../components/RowDetailDrawer";
import { PermissionButton } from "../security/Can";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { SiteEntryHistory, type SiteEntryHistoryRow } from "./SiteEntryHistory";
import type { AdminPageProps } from "../types";

type Report = { id: string; workScope: string; projectId: string; vendorUserId: string; status: string; spaceId?: string; plannedStartDate?: string; plannedEndDate?: string; crewCount?: number; contactName?: string; contactPhone?: string; reviewComment?: string; acceptanceComment?: string };

export function SiteEntryPropertyPage({ api, setActionResult, setGlobalError, effectivePermissions, stage }: AdminPageProps & { stage: "review" | "accept" }) {
  const [rows, setRows] = React.useState<Report[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [note, setNote] = React.useState("");
  const [history, setHistory] = React.useState<SiteEntryHistoryRow[] | null>(null);
  const [historyError, setHistoryError] = React.useState("");
  const [epoch, setEpoch] = React.useState(0);
  const selected = rows.find(row => row.id === selectedId) ?? rows[0];
  const { loading, error, reload } = useAdminResource(async () => {
    setRows(await api.get<Report[]>("/api/v1/facility/site-entry-reports"));
  }, { onError: () => {}, fallbackMessage: "进场报备读取失败" });
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload, setGlobalError, setActionResult, fallbackMessage: "进场报备办理失败",
    makeResult: (action, request, result) => ({ action, idempotencyKey: request.idempotencyKey, result }),
  });
  React.useEffect(() => {
    let active = true;
    setHistory(null); setHistoryError("");
    if (selected?.id) api.get<SiteEntryHistoryRow[]>(`/api/v1/facility/site-entry-reports/${encodeURIComponent(selected.id)}/history`)
      .then(items => { if (active) setHistory(items); })
      .catch(() => { if (active) setHistoryError("材料历史读取失败，请重试"); });
    return () => { active = false; };
  }, [api, selected?.id, epoch]);
  const latest = history?.at(-1);
  const canAct = selected && (stage === "review" ? selected.status === "submitted"
    : selected.status === "in_progress" && latest?.action === "completion");

  async function decide(result: string) {
    if (!selected || !canAct) return;
    if (!note.trim()) { setFormError("请填写办理意见"); return; }
    const request = stage === "review"
      ? { idempotencyKey: crypto.randomUUID(), decision: result, reviewComment: note.trim() }
      : { idempotencyKey: crypto.randomUUID(), result, acceptanceComment: note.trim(), completionSequence: latest!.sequence };
    const ok = await runAction("site_entry_" + stage, request,
      () => api.post(`/api/v1/facility/site-entry-reports/${encodeURIComponent(selected.id)}/${stage}`, request));
    if (ok) { setNote(""); setEpoch(value => value + 1); }
  }

  return <section data-testid="site-entry-property-page">
    <AdminPageHeader title={stage === "review" ? "进场审批" : "施工验收"}
      description="查看报备范围与每轮实际材料，办理意见保留在原报备。" />
    <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
      <label>进场报备<select aria-label="进场报备" value={selected?.id ?? ""} disabled={busyAction !== ""}
        onChange={event => { setSelectedId(event.target.value); setNote(""); }}>
        {rows.map(row => <option key={row.id} value={row.id}>{row.workScope} · {formatBusinessStatus(row.status)}</option>)}
      </select></label>
      {selected ? <div aria-label="报备详情">
        <p>{selected.workScope} · 项目：{selected.projectId} · 供应商：{selected.vendorUserId}</p>
        <p>施工地点：{selected.spaceId ?? "未填写"}</p>
        <p>计划开始：{cnDate(selected.plannedStartDate)}</p>
        <p>计划结束：{cnDate(selected.plannedEndDate)}</p>
        <p>施工人数：{selected.crewCount ?? "未填写"}</p>
        <p>联系人：{selected.contactName ?? "未填写"} · {selected.contactPhone ?? "未填写"}</p>
        {selected.reviewComment ? <p>进场审批意见：{selected.reviewComment}</p> : null}
        {selected.acceptanceComment ? <p>最近验收意见：{selected.acceptanceComment}</p> : null}
      </div> : null}
      {historyError ? <p role="alert">{historyError}<button onClick={() => setEpoch(value => value + 1)}>重试</button></p>
        : history === null ? <p>正在读取材料历史…</p> : selected ? <SiteEntryHistory reportId={selected.id} items={history} /> : null}
      <label>办理意见<textarea aria-label="办理意见" value={note} onChange={event => setNote(event.target.value)} /></label>
      <div className="admin-form-row">
        <PermissionButton permissionsState={effectivePermissions} permissions={["facility.site_entry." + stage]}
          disabled={busyAction !== "" || !canAct} onClick={() => void decide(stage === "review" ? "approve" : "pass")}>
          {stage === "review" ? "批准进场" : "验收通过"}
        </PermissionButton>
        <PermissionButton permissionsState={effectivePermissions} permissions={["facility.site_entry." + stage]}
          disabled={busyAction !== "" || !canAct} onClick={() => void decide(stage === "review" ? "reject" : "rectify")}>
          {stage === "review" ? "驳回报备" : "要求整改"}
        </PermissionButton>
      </div>
      {stage === "accept" && !canAct ? <p>等待供应商提交新的完工材料后方可验收。</p> : null}
      {formError ? <p role="alert">{formError}</p> : null}
    </AdminDataState>
  </section>;
}
