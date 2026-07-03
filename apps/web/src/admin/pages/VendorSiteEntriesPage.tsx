import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type VendorSiteEntryRow = {
  id: string;
  projectId?: string;
  spaceId?: string;
  workScope?: string;
  plannedStartDate?: string;
  plannedEndDate?: string;
  crewCount?: number;
  contactName?: string;
  contactPhone?: string;
  vendorUserId?: string;
  status?: string;
  reviewComment?: string | null;
  acceptanceComment?: string | null;
  completionAttachmentIds?: string[];
  completionNote?: string | null;
};

const CREATE = ["facility.site_entry.create_assigned"];
const SUBMIT = ["facility.site_entry.submit_assigned"];
const UPLOAD_COMPLETION = ["facility.site_entry.upload_completion_assigned"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

function splitAttachmentIds(value: string) {
  return value.split(/[\n,，;；]+/).map((item) => item.trim()).filter(Boolean);
}

export function VendorSiteEntriesPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorSiteEntryRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [form, setForm] = React.useState({
    projectId: "venue-1",
    spaceId: "area-1",
    workScope: "店铺装修围挡施工",
    plannedStartDate: "2026-06-20",
    plannedEndDate: "2026-06-22",
    crewCount: "4",
    contactName: "王工",
    contactPhone: "13800000000",
  });
  const [submitNote, setSubmitNote] = React.useState("进场施工");
  const [completionNote, setCompletionNote] = React.useState("施工已完工，材料已上传");
  const [completionAttachments, setCompletionAttachments] = React.useState("att-site-completion");

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;
  const canUploadCompletion = selected?.status === "in_progress" && (user.accountType !== "outsourced_vendor" || selected.vendorUserId === user.id);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const entries = await api.get<VendorSiteEntryRow[]>("/api/v1/facility/site-entry-reports");
      setRows(entries ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "外包进场报备读取失败", onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: "外包进场报备操作失败",
  });

  function setField(key: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function createEntry() {
    if (!form.projectId.trim() || !form.spaceId.trim() || !form.workScope.trim()) {
      setFormError("项目、空间和施工范围不能为空");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-site-entry-create"),
      projectId: form.projectId.trim(),
      spaceId: form.spaceId.trim(),
      workScope: form.workScope.trim(),
      plannedStartDate: form.plannedStartDate,
      plannedEndDate: form.plannedEndDate,
      crewCount: Number(form.crewCount || 1),
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone.trim(),
    };
    await runAction("vendor_site_entry_created", request, () => api.post("/api/v1/facility/site-entry-reports", request));
  }

  async function submitEntry() {
    if (!selected) {
      setFormError("请选择一条进场报备");
      return;
    }
    const request = { idempotencyKey: nextKey("vendor-site-entry-submit"), note: submitNote };
    await runAction("vendor_site_entry_submitted", request, () => api.post(`/api/v1/facility/site-entry-reports/${encodeURIComponent(selected.id)}/submit`, request));
  }

  async function uploadCompletion() {
    if (!selected) {
      setFormError("请选择一条进场报备");
      return;
    }
    if (!canUploadCompletion) {
      setFormError("仅 in_progress 且归属当前外包账号的报备可上传完工材料");
      return;
    }
    const attachmentIds = splitAttachmentIds(completionAttachments);
    if (!completionNote.trim() || attachmentIds.length === 0) {
      setFormError("完工说明和附件引用不能为空");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-site-entry-completion"),
      note: completionNote.trim(),
      attachmentIds,
    };
    await runAction("vendor_site_entry_completion_uploaded", request, () => api.post(`/api/v1/facility/site-entry-reports/${encodeURIComponent(selected.id)}/completion`, request));
  }

  return (
    <section data-testid="vendor-site-entries-page">
      <AdminPageHeader title="外包进场报备" description="施工/装修队只能查看、创建、提交自己的进场报备，并在施工中报备上传完工材料；审核验收由平台角色处理。" />
      <TechDetails><div className="admin-generic-evidence" data-testid="vendor-site-entries-evidence">接口证据：GET/POST /api/v1/facility/site-entry-reports；POST /api/v1/facility/site-entry-reports/{"{report_id}"}/submit；POST /api/v1/facility/site-entry-reports/{"{report_id}"}/completion。</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="vendor-site-entries-summary">
          <div className="admin-kpi-card"><span>我的报备</span><strong>{rows.length}</strong><small>{user.username}</small></div>
          <div className="admin-kpi-card"><span>可进场</span><strong>{rows.filter((row) => ["approved", "submitted", "in_progress"].includes(row.status ?? "")).length}</strong><small>status</small></div>
          <div className="admin-kpi-card"><span>当前报备</span><strong>{selected?.workScope ?? selected?.id ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="vendor-site-entry-create-form">
          <label>项目 id<input data-testid="vendor-site-entry-project" value={form.projectId} onChange={(event) => setField("projectId", event.target.value)} /></label>
          <label>空间 id<input data-testid="vendor-site-entry-space" value={form.spaceId} onChange={(event) => setField("spaceId", event.target.value)} /></label>
          <label>施工范围<input data-testid="vendor-site-entry-scope" value={form.workScope} onChange={(event) => setField("workScope", event.target.value)} /></label>
          <label>开始日期<input data-testid="vendor-site-entry-start" type="date" value={form.plannedStartDate} onChange={(event) => setField("plannedStartDate", event.target.value)} /></label>
          <label>结束日期<input data-testid="vendor-site-entry-end" type="date" value={form.plannedEndDate} onChange={(event) => setField("plannedEndDate", event.target.value)} /></label>
          <label>人数<input data-testid="vendor-site-entry-crew" value={form.crewCount} onChange={(event) => setField("crewCount", event.target.value)} /></label>
          <label>联系人<input data-testid="vendor-site-entry-contact" value={form.contactName} onChange={(event) => setField("contactName", event.target.value)} /></label>
          <label>电话<input data-testid="vendor-site-entry-phone" value={form.contactPhone} onChange={(event) => setField("contactPhone", event.target.value)} /></label>
          <label>提交备注<input data-testid="vendor-site-entry-submit-note" value={submitNote} onChange={(event) => setSubmitNote(event.target.value)} /></label>
          <label>完工说明<input data-testid="vendor-site-entry-completion-note" value={completionNote} onChange={(event) => setCompletionNote(event.target.value)} /></label>
          <label>完工附件引用<input data-testid="vendor-site-entry-completion-attachments" value={completionAttachments} onChange={(event) => setCompletionAttachments(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="vendor-site-entry-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={CREATE} data-testid="vendor-site-entry-create" disabled={busyAction !== ""} onClick={() => void createEntry()}>创建</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={SUBMIT} data-testid="vendor-site-entry-submit" disabled={busyAction !== ""} onClick={() => void submitEntry()}>提交</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={UPLOAD_COMPLETION} data-testid="vendor-site-entry-completion-upload" disabled={busyAction !== "" || !canUploadCompletion} onClick={() => void uploadCompletion()}>完工材料上传</PermissionButton>
        </div>
        {formError ? <div data-testid="vendor-site-entry-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-site-entries-table">
            <thead><tr><th>选择</th><th>施工范围</th><th>项目/空间</th><th>计划日期</th><th>人数</th><th>联系人</th><th>外包账号</th><th>状态</th><th>审核/验收</th><th>完工材料</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`vendor-site-entry-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.workScope ?? row.id}</td>
                  <td>{row.projectId ?? "-"} / {row.spaceId ?? "-"}</td>
                  <td>{row.plannedStartDate ?? "-"} ~ {row.plannedEndDate ?? "-"}</td>
                  <td>{row.crewCount ?? "-"}</td>
                  <td>{row.contactName ?? "-"} / {row.contactPhone ?? "-"}</td>
                  <td>{row.vendorUserId ?? "-"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td>{row.reviewComment ?? "-"} / {row.acceptanceComment ?? "-"}</td>
                  <td>{row.completionNote ?? "-"} / {(row.completionAttachmentIds ?? []).join("、") || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
