import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, SubjectBindingRow } from "../types";

// 后端写权限码（PlatformController：/business-entity-bindings/{id}/change）
const BINDING_MANAGE_PERMISSION = "entity.binding.manage";

const TEXT = {
  title: "四类主体绑定",
  description: "展示经营对象、运营主体、合同主体和结算主体的唯一绑定关系；所有变更必须带原因、幂等键和证据附件。",
  evidence: "接口证据：读取 /api/v1/platform/business-entity-bindings；变更调用 /api/v1/platform/business-entity-bindings/{id}/change。",
  boundary: "四类主体绑定是合同、结算、利润报表和责任归属的基础口径；同一业务对象必须唯一绑定，历史合同和流程留痕不得被覆盖。",
  empty: "暂无主体绑定",
  loadFailed: "主体绑定读取失败",
  submitChange: "提交绑定变更",
  select: "选择",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return String(value);
}

export function SubjectBindingsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [bindings, setBindings] = React.useState<SubjectBindingRow[]>([]);
  const { setSelectedId, selected } = useSelectedRow(bindings);
  const [reason, setReason] = React.useState("完整后台验收变更");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      setBindings(await api.get<SubjectBindingRow[]>("/api/v1/platform/business-entity-bindings"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const activeCount = bindings.filter((row) => row.status === "active").length;
  const uniqueObjects = new Set(bindings.map((row) => row.businessObjectId).filter(Boolean)).size;

  function validateChange() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要变更的主体绑定记录。");
      return false;
    }
    if (!reason.trim()) {
      setFormError("变更原因不能为空。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function submitChange() {
    if (!selected?.id) {
      setFormError("请先在表格中选择要变更的主体绑定记录。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const body = {
      reason,
      idempotencyKey: `full-admin-subject-binding-change-${selected.id}-${Date.now()}`,
      evidenceAttachmentIds: ["evidence-full-admin"],
    };
    try {
      const result = await api.post<unknown>(`/api/v1/platform/business-entity-bindings/${encodeURIComponent(selected.id)}/change`, body);
      setActionResult({ action: "subject_binding_change_submitted", bindingId: selected.id, result, ...body });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "主体绑定变更失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "subject_binding_change_submitted", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="subject-bindings-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="subject-bindings-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="entity_binding.viewed / entity_binding.changed" />
      {error ? <div className="admin-inline-error" data-testid="subject-bindings-error">{error.message}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {bindings.length === 0 ? <div className="admin-state admin-empty" data-testid="subject-bindings-empty">{TEXT.empty}</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="subject-bindings-summary">
              <div className="admin-kpi-card"><span>唯一绑定对象</span><strong>{uniqueObjects}</strong><small>唯一绑定</small></div>
              <div className="admin-kpi-card"><span>生效绑定</span><strong>{activeCount}</strong><small>active</small></div>
              <div className="admin-kpi-card"><span>合同主体</span><strong>{bindings.filter((row) => row.contractSubject).length}</strong><small>合同口径</small></div>
              <div className="admin-kpi-card"><span>选中绑定</span><strong>{selected?.businessObjectId ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
            </div>

            <div className="admin-form-grid">
              <label>变更原因<input data-testid="subject-bindings-change-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <HighRiskConfirm
                label={TEXT.submitChange}
                impact="将变更选中业务对象的四类主体绑定并落库留痕，影响合同、结算与利润报表口径，历史绑定不可被静默覆盖。"
                disabled={submitting}
                precheck={validateChange}
                onConfirm={() => void submitChange()}
                permissionsState={effectivePermissions}
                permissions={[BINDING_MANAGE_PERMISSION]}
                data-testid="subject-bindings-submit-change"
              />
              <span className="admin-muted-action" data-testid="subject-bindings-boundary">{TEXT.boundary}</span>
            </div>
            {formError ? <div className="admin-inline-error" data-testid="subject-bindings-form-error">{formError}</div> : null}

            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="subject-bindings-table">
                <thead><tr><th>{TEXT.select}</th><th>业务对象</th><th>运营主体</th><th>合同主体</th><th>合同主体 ID</th><th>结算主体</th><th>结算主体 ID</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{bindings.map((row) => (
                  <tr key={row.id}>
                    <td><input type="radio" data-testid={`subject-bindings-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                    <td>{display(row.businessObjectId)}</td>
                    <td>{display(row.operatorSubject)}</td>
                    <td>{display(row.contractSubject)}</td>
                    <td>{display(row.contractSubjectId)}</td>
                    <td>{display(row.settlementSubject)}</td>
                    <td>{display(row.settlementSubjectId)}</td>
                    <td>{display(row.status)}</td>
                    <td>{display(row.auditEvidenceRef)}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
