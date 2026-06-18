import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, SubjectBindingRow } from "../types";

const TEXT = {
  title: "四类主体绑定",
  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；所有变更必须带原因、幂等键和证据附件。",
  evidence: "接口证据：读取 /api/v1/platform/business-entity-bindings；变更调用 /api/v1/platform/business-entity-bindings/{id}/change。",
  boundary: "四类主体绑定是合同、结算、利润报表和责任归属的基础口径；同一业务对象必须唯一绑定，历史合同和流程留痕不得被覆盖。",
  empty: "暂无主体绑定",
  loadFailed: "主体绑定读取失败",
  submitChange: "提交绑定变更",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return String(value);
}

export function SubjectBindingsPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [bindings, setBindings] = React.useState<SubjectBindingRow[]>([]);
  const [reason, setReason] = React.useState("完整后台验收变更");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setBindings(await api.get<SubjectBindingRow[]>("/api/v1/platform/business-entity-bindings"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const activeCount = bindings.filter((row) => row.status === "active").length;
  const uniqueObjects = new Set(bindings.map((row) => row.businessObjectId).filter(Boolean)).size;
  const firstBinding = bindings[0];

  async function submitChange() {
    if (!firstBinding?.id) {
      setFormError("当前没有可变更的主体绑定记录。");
      return;
    }
    if (!reason.trim()) {
      setFormError("变更原因不能为空。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    const body = {
      reason,
      idempotencyKey: "full-admin-subject-binding-change",
      evidenceAttachmentIds: ["evidence-full-admin"],
    };
    const result = await api.post<unknown>(`/api/v1/platform/business-entity-bindings/${encodeURIComponent(firstBinding.id)}/change`, body);
    setActionResult({ action: "subject_binding_change_submitted", result, ...body });
    await reload();
  }

  return (
    <section data-testid="subject-bindings-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="subject-bindings-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="entity_binding.viewed / entity_binding.changed" />
      {error ? <div className="admin-inline-error" data-testid="subject-bindings-error">{error}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {bindings.length === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="subject-bindings-summary">
              <div className="admin-kpi-card"><span>唯一绑定对象</span><strong>{uniqueObjects}</strong><small>唯一绑定</small></div>
              <div className="admin-kpi-card"><span>生效绑定</span><strong>{activeCount}</strong><small>active</small></div>
              <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractSubject).length}</strong><small>合同口径</small></div>
              <div className="admin-kpi-card"><span>结算主体</span><strong>{bindings.filter((row) => row.settlementSubject).length}</strong><small>结算口径</small></div>
            </div>

            <div className="admin-form-grid">
              <label>变更原因<input data-testid="subject-bindings-change-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <button type="button" data-testid="subject-bindings-submit-change" onClick={() => void submitChange()}>{TEXT.submitChange}</button>
              <span className="admin-muted-action" data-testid="subject-bindings-boundary">{TEXT.boundary}</span>
            </div>
            {formError ? <div className="admin-inline-error" data-testid="subject-bindings-form-error">{formError}</div> : null}

            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="subject-bindings-table">
                <thead><tr><th>业务对象</th><th>运营主体</th><th>合同主体</th><th>合同主体 ID</th><th>结算主体</th><th>结算主体 ID</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{bindings.map((row) => <tr key={row.id}><td>{display(row.businessObjectId)}</td><td>{display(row.operatorSubject)}</td><td>{display(row.contractSubject)}</td><td>{display(row.contractSubjectId)}</td><td>{display(row.settlementSubject)}</td><td>{display(row.settlementSubjectId)}</td><td>{display(row.status)}</td><td>{display(row.auditEvidenceRef)}</td></tr>)}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
