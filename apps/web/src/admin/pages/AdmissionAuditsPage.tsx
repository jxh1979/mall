import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, ContractRow, MerchantAdmissionRow, SubjectBindingRow } from "../types";

const EVIDENCE_CREATE = ["evidence.event.create"];

const TEXT = {
  title: "商户准入审批流水",
  description: "把商户档案、四类主体绑定、合同草稿/审批三类准入证据放在同一条审计链路里，支撑总控、法务、财务和运营共同验收。",
  evidence: "接口证据：读取 /api/v1/platform/merchants、/api/v1/platform/business-entity-bindings、/api/v1/contracts；导出写入 /api/v1/evidence/events。",
  boundary: "商户准入必须同时核验商户档案、四类主体绑定、合同审批与附件归档；任何一段缺失都不能视为准入闭环完成。",
  loadFailed: "准入流水读取失败",
  exportReasonRequired: "导出原因不能为空。",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

function byMerchant<T extends { merchantId?: string; id?: string }>(rows: T[], merchantId: string) {
  return rows.filter((row) => row.merchantId === merchantId || row.id === merchantId);
}

function contractAttachments(row: ContractRow) {
  return row.attachmentIds?.length ? row.attachmentIds.join(" / ") : row.evidenceAttachmentIds?.join(" / ") ?? "-";
}

export function AdmissionAuditsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [merchants, setMerchants] = React.useState<MerchantAdmissionRow[]>([]);
  const [bindings, setBindings] = React.useState<SubjectBindingRow[]>([]);
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [exportReason, setExportReason] = React.useState("完整后台准入流水导出");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [nextMerchants, nextBindings, nextContracts] = await Promise.all([
        api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants"),
        api.get<SubjectBindingRow[]>("/api/v1/platform/business-entity-bindings"),
        api.get<{ items?: ContractRow[] } | ContractRow[]>("/api/v1/contracts").then((data) => (Array.isArray(data) ? data : data?.items ?? [])),
      ]);
      setMerchants(nextMerchants);
      setBindings(nextBindings);
      setContracts(nextContracts);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const merchantIds = new Set(merchants.map((row) => row.id).filter(Boolean));
  const bindingMerchantIds = new Set(bindings.map((row) => row.businessObjectId).filter(Boolean));
  const contractMerchantIds = new Set(contracts.map((row) => row.merchantId).filter(Boolean));
  const closedLoopCount = [...merchantIds].filter((id) => bindingMerchantIds.has(id) && contractMerchantIds.has(id)).length;
  const draftCount = contracts.filter((row) => ["draft", "pending", "returned"].includes(row.lifecycleStatus ?? row.status ?? "")).length;

  async function exportAdmissionAudit() {
    if (!exportReason.trim()) {
      setFormError(TEXT.exportReasonRequired);
      return;
    }
    setFormError("");
    setGlobalError(null);
    const evidenceAttachmentIds = ["att-admission-audit-export"];
    const payload = {
      idempotencyKey: `admission-audit-export-${Date.now()}`,
      operatorNote: exportReason,
      evidenceAttachmentIds,
      deviceCode: "web-admin",
      staffId: "admin-ui",
      events: [{
        eventType: "merchant.admission.audit_exported",
        eventCategory: "governance",
        result: "success",
        summary: exportReason,
        payload: {
          merchantCount: merchants.length,
          bindingCount: bindings.length,
          contractCount: contracts.length,
          closedLoopCount,
        },
        occurredAt: new Date().toISOString(),
      }],
    };
    const result = await api.post<unknown>("/api/v1/evidence/events", payload);
    setActionResult({
      action: "admission_audit_exported",
      audit: "merchant_admission_trace_ready",
      evidenceAttachmentIds,
      request: payload,
      result,
    });
  }

  return (
    <section data-testid="admission-audits-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="admission-audits-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="merchant_admission.viewed / merchant_admission.audit_exported" />
      {formError ? <div className="admin-inline-error" data-testid="admission-audits-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {merchants.length === 0 && bindings.length === 0 && contracts.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="admission-audits-empty">暂无商户准入流水</div>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="admission-audits-summary">
              <div className="admin-kpi-card"><span>商户档案</span><strong>{merchants.length}</strong><small>档案证据</small></div>
              <div className="admin-kpi-card"><span>主体绑定</span><strong>{bindings.length}</strong><small>四类主体</small></div>
              <div className="admin-kpi-card"><span>合同审批</span><strong>{contracts.length}</strong><small>{draftCount} 个草稿</small></div>
              <div className="admin-kpi-card"><span>准入闭环</span><strong>{closedLoopCount}</strong><small>档案+绑定+合同</small></div>
            </div>

            <div className="admin-form-grid">
              <label>导出原因<input data-testid="admission-audits-export-reason" value={exportReason} onChange={(event) => setExportReason(event.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <PermissionButton permissionsState={effectivePermissions} permissions={EVIDENCE_CREATE} data-testid="admission-audits-export" onClick={() => void exportAdmissionAudit()}>导出准入审计片段</PermissionButton>
              <span className="admin-muted-action" data-testid="admission-audits-boundary">{TEXT.boundary}</span>
            </div>

            <div className="admin-section-title">商户档案</div>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="admission-audits-merchants-table">
                <thead><tr><th>商户</th><th>公司名</th><th>区块</th><th>面积</th><th>合作形式</th><th>风险</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{merchants.map((row) => (
                  <tr key={row.id}><td>{display(row.displayName)}</td><td>{display(row.companyName)}</td><td>{display(row.mallBlockCode)}</td><td>{display(row.areaSquareMeter)}</td><td>{display(row.cooperationForm)}</td><td>{display(row.riskLevel)}</td><td>{display(row.status)}</td><td>{display(row.auditEvidenceRef)}</td></tr>
                ))}</tbody>
              </table>
            </div>

            <div className="admin-section-title">主体绑定</div>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="admission-audits-bindings-table">
                <thead><tr><th>业务对象</th><th>运营主体</th><th>合同主体</th><th>结算主体</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{bindings.map((row) => (
                  <tr key={row.id}><td>{display(row.businessObjectId)}</td><td>{display(row.operatorSubject)}</td><td>{display(row.contractSubject)}</td><td>{display(row.settlementSubject)}</td><td>{display(row.status)}</td><td>{display(row.auditEvidenceRef)}</td></tr>
                ))}</tbody>
              </table>
            </div>

            <div className="admin-section-title">合同草稿/审批</div>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="admission-audits-contracts-table">
                <thead><tr><th>商户</th><th>合同主体</th><th>运营主体</th><th>结算主体</th><th>生效日</th><th>到期日</th><th>附件</th><th>状态</th><th>留痕证据</th></tr></thead>
                <tbody>{contracts.map((row) => (
                  <tr key={row.id}><td>{display(row.merchantId)}</td><td>{display(row.contractSubjectId)}</td><td>{display(row.operationSubjectId)}</td><td>{display(row.settlementSubjectId)}</td><td>{display(row.effectiveDate)}</td><td>{display(row.expiryDate)}</td><td>{contractAttachments(row)}</td><td>{display(row.lifecycleStatus ?? row.status)}</td><td>{display(row.auditEvidenceRef)}</td></tr>
                ))}</tbody>
              </table>
            </div>

            <div className="admin-muted-action" data-testid="admission-audits-cross-check">
              已核验商户 {merchants.map((row) => `${row.id}:${byMerchant(contracts, row.id).length} 份合同`).join("；") || "-"}
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
