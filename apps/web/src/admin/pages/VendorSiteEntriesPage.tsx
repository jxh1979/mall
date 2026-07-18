import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
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
const ENTERABLE_STATUSES = ["approved", "submitted", "in_progress"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

function splitAttachmentIds(value: string) {
  return value.split(/[\n,，;；]+/).map((item) => item.trim()).filter(Boolean);
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染报备表。
// URL 约定：?kpi=site-entries[&subset=enterable]。
type KpiFocus = { subset: "all" | "enterable" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "site-entries") return null;
  return { subset: params.get("subset") === "enterable" ? "enterable" : "all" };
}

export function VendorSiteEntriesPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorSiteEntryRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  // 引导式「新建进场报备」弹窗：必填项（项目/空间/施工范围）默认留空，避免默认值让缺字段拦截失效。
  const [form, setForm] = React.useState({
    projectId: "",
    spaceId: "",
    workScope: "",
    plannedStartDate: "2026-06-20",
    plannedEndDate: "2026-06-22",
    crewCount: "4",
    contactName: "王工",
    contactPhone: "13800000000",
  });
  const [createOpen, setCreateOpen] = React.useState(false);
  const [submitNote, setSubmitNote] = React.useState("进场施工");
  const [completionNote, setCompletionNote] = React.useState("施工已完工，材料已上传");
  const [completionAttachments, setCompletionAttachments] = React.useState("att-site-completion");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该进场报备完整字段（枚举中文·计划日期中文·完工材料展开）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: "all" | "enterable") {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "site-entries");
    if (subset === "enterable") params.set("subset", "enterable");
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;
  const canUploadCompletion = selected?.status === "in_progress" && (user.accountType !== "outsourced_vendor" || selected.vendorUserId === user.id);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const entries = await api.get<VendorSiteEntryRow[]>("/api/v1/facility/site-entry-reports");
      setRows(entries ?? []);
    },
    { onError: () => {}, fallbackMessage: "外包进场报备读取失败", onStart: () => setGlobalError(null) },
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

  // 提交在确认步触发；runAction 成功返回 true 才关弹窗，失败留弹窗内显错（formError）。原接口/请求体/双保险校验保留。
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
    const ok = await runAction("vendor_site_entry_created", request, () => api.post("/api/v1/facility/site-entry-reports", request));
    if (ok) setCreateOpen(false);
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
      setFormError("仅施工中且归属当前外包账号的报备可上传完工材料");
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

  const enterableRows = rows.filter((row) => ENTERABLE_STATUSES.includes(row.status ?? ""));
  const focusRows = kpiFocus?.subset === "enterable" ? enterableRows : rows;

  function openEntryDetail(row: VendorSiteEntryRow) {
    setDetail({
      title: `进场报备明细 · ${row.workScope ?? row.id}`,
      fields: [
        { label: "施工范围", value: row.workScope ?? row.id },
        { label: "项目", value: row.projectId ?? "—" },
        { label: "空间", value: row.spaceId ?? "—" },
        { label: "计划开始", value: cnDate(row.plannedStartDate) },
        { label: "计划结束", value: cnDate(row.plannedEndDate) },
        { label: "施工人数", value: row.crewCount ?? "—" },
        { label: "联系人", value: row.contactName ?? "—" },
        { label: "联系电话", value: row.contactPhone ?? "—" },
        { label: "外包账号", value: row.vendorUserId ?? "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "审核意见", value: row.reviewComment ?? "—" },
        { label: "验收意见", value: row.acceptanceComment ?? "—" },
        { label: "完工说明", value: row.completionNote ?? "—" },
        { label: "完工材料", value: (row.completionAttachmentIds ?? []).join("、") || "—" },
        { label: "报备编号", value: row.id },
      ],
    });
  }

  const entriesTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="vendor-site-entries-table">
        <thead><tr><th>选择</th><th>施工范围</th><th>项目/空间</th><th>计划日期</th><th>人数</th><th>联系人</th><th>外包账号</th><th>状态</th><th>审核/验收</th><th>完工材料</th></tr></thead>
        <tbody>
          {(kpiFocus ? focusRows : rows).map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-site-entry-row-${row.id}`}
              onClick={() => openEntryDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEntryDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此进场报备 ${row.id}`} data-testid={`vendor-site-entry-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
              <td>{row.workScope ?? row.id}</td>
              <td>{row.projectId ?? "-"} / {row.spaceId ?? "-"}</td>
              <td>{cnDate(row.plannedStartDate)} ~ {cnDate(row.plannedEndDate)}</td>
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
  );

  return (
    <section data-testid="vendor-site-entries-page">
      <AdminPageHeader title="外包进场报备" description="施工/装修队只能查看、创建、提交自己的进场报备，并在施工中报备上传完工材料；审核验收由平台角色处理。" />
      <TechDetails><div className="admin-generic-evidence" data-testid="vendor-site-entries-evidence">接口证据：GET/POST /api/v1/facility/site-entry-reports；POST /api/v1/facility/site-entry-reports/{"{report_id}"}/submit；POST /api/v1/facility/site-entry-reports/{"{report_id}"}/completion。</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="vendor-site-entries-kpi-focus-banner">
              正在查看：{kpiFocus.subset === "enterable" ? "可进场报备" : "我的报备"}（{focusRows.length}）
              <button type="button" data-testid="vendor-site-entries-kpi-focus-clear" onClick={exitKpiFocus}>清除筛选，返回全部</button>
            </div>
            {entriesTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="vendor-site-entries-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>我的报备</span><button type="button" className="admin-kpi-value" data-testid="vendor-site-entries-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>{user.username}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>可进场</span><button type="button" className="admin-kpi-value" data-testid="vendor-site-entries-kpi-enterable" onClick={() => enterKpiFocus("enterable")}>{enterableRows.length}</button><small>已通过/已提交/施工中</small></div>
              <div className="admin-kpi-card"><span>当前报备</span><strong className="admin-kpi-value--text" title={selected?.workScope ?? selected?.id ?? "-"}>{selected?.workScope ?? selected?.id ?? "-"}</strong><small>{formatBusinessStatus(selected?.status)}</small></div>
            </div>

            <div className="admin-form-row" data-testid="vendor-site-entry-create-form">
              <PermissionButton permissionsState={effectivePermissions} permissions={CREATE} data-testid="vendor-site-entry-create-open" disabled={busyAction !== ""}
                onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ 新建进场报备</PermissionButton>
              <span className="admin-muted-action">选中下方一条报备后，可提交或上传完工材料。</span>
            </div>
            <div className="admin-form-grid" data-testid="vendor-site-entry-followup-form">
              <label>提交备注<input data-testid="vendor-site-entry-submit-note" value={submitNote} onChange={(event) => setSubmitNote(event.target.value)} /></label>
              <label>完工说明<input data-testid="vendor-site-entry-completion-note" value={completionNote} onChange={(event) => setCompletionNote(event.target.value)} /></label>
              <label>完工附件引用<input data-testid="vendor-site-entry-completion-attachments" value={completionAttachments} onChange={(event) => setCompletionAttachments(event.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="vendor-site-entry-actions">
              <PermissionButton permissionsState={effectivePermissions} permissions={SUBMIT} data-testid="vendor-site-entry-submit" disabled={busyAction !== ""} onClick={() => void submitEntry()}>提交</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={UPLOAD_COMPLETION} data-testid="vendor-site-entry-completion-upload" disabled={busyAction !== "" || !canUploadCompletion} onClick={() => void uploadCompletion()}>完工材料上传</PermissionButton>
            </div>
            {formError ? <div data-testid="vendor-site-entry-error" className="admin-inline-error">{formError}</div> : null}

            {entriesTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={createOpen}
        title="新建进场报备"
        testId="vendor-site-entry-create-guided"
        submitTestId="vendor-site-entry-create"
        submitLabel="创建"
        submitting={busyAction !== ""}
        error={createOpen ? (formError || undefined) : undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void createEntry()}
        steps={[
          {
            title: "施工基本信息",
            description: "项目、空间和施工范围必填；只能创建归属自己的进场报备，审核验收由平台角色处理。",
            validate: () => (form.projectId.trim() && form.spaceId.trim() && form.workScope.trim() ? null : "项目、空间和施工范围不能为空"),
            content: (
              <>
                <label>项目 id<input data-testid="vendor-site-entry-project" value={form.projectId} onChange={(event) => setField("projectId", event.target.value)} /></label>
                <label>空间 id<input data-testid="vendor-site-entry-space" value={form.spaceId} onChange={(event) => setField("spaceId", event.target.value)} /></label>
                <label>施工范围<input data-testid="vendor-site-entry-scope" value={form.workScope} onChange={(event) => setField("workScope", event.target.value)} /></label>
              </>
            ),
          },
          {
            title: "计划与联系人",
            description: "计划日期、人数与现场联系人。",
            content: (
              <>
                <label>开始日期<CnDatePicker data-testid="vendor-site-entry-start" value={form.plannedStartDate} onChange={(v) => setField("plannedStartDate", v)} /></label>
                <label>结束日期<CnDatePicker data-testid="vendor-site-entry-end" value={form.plannedEndDate} onChange={(v) => setField("plannedEndDate", v)} /></label>
                <label>人数<input data-testid="vendor-site-entry-crew" value={form.crewCount} onChange={(event) => setField("crewCount", event.target.value)} /></label>
                <label>联系人<input data-testid="vendor-site-entry-contact" value={form.contactName} onChange={(event) => setField("contactName", event.target.value)} /></label>
                <label>电话<input data-testid="vendor-site-entry-phone" value={form.contactPhone} onChange={(event) => setField("contactPhone", event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "项目", value: form.projectId.trim() || "未填写" },
          { label: "空间", value: form.spaceId.trim() || "未填写" },
          { label: "施工范围", value: form.workScope.trim() || "未填写" },
          { label: "计划日期", value: `${cnDate(form.plannedStartDate)} ~ ${cnDate(form.plannedEndDate)}` },
          { label: "人数", value: form.crewCount || "1" },
          { label: "联系人", value: `${form.contactName.trim() || "—"} / ${form.contactPhone.trim() || "—"}` },
        ]}
      />
    </section>
  );
}
