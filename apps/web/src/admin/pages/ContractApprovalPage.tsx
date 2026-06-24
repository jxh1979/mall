import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, ContractRow } from "../types";

const TEXT = {
  title: "合同审批与附件归档",
  description: "覆盖合同草稿、主体绑定、附件归档和审批流。合同主体、运营主体、结算主体可以不同，但必须唯一绑定并在审批留痕中统一保存。",
  evidence: "接口证据：读取/创建 /api/v1/platform/contracts；审批 POST /api/v1/platform/contracts/{contract_id}/approve。",
  loadFailed: "合同数据读取失败",
  actionFailed: "合同操作失败",
  noContract: "当前没有可审批合同。",
  draftTitleRequired: "合同草稿说明不能为空。",
  approvalCommentRequired: "审批意见不能为空。",
  total: "合同总数",
  archivedAttachments: "附件归档",
  draftCount: "待审批草稿",
  selected: "选中合同",
  boundary: "合同审批只做审批流和附件归档；股东比例只用于利润报表，不参与结算；合同/运营/结算主体必须在合同留痕中唯一绑定。",
  draftTitle: "草稿说明",
  approvalComment: "审批意见",
  createDraft: "创建草稿并归档附件",
  approve: "审批通过",
  select: "选择",
  merchant: "商户",
  contractSubject: "合同主体",
  operationSubject: "运营主体",
  settlementSubject: "结算主体",
  effectiveDate: "生效日",
  expiryDate: "到期日",
  rentTerms: "租金条款",
  shareholderRatio: "股东比例(报表)",
  attachments: "附件",
  status: "审批状态",
  // ② 履约闭环 S1：合同生效 + 空间绑定
  spaceIds: "租赁空间 id（逗号分隔）",
  activate: "生效并绑定空间",
  activateImpact: "合同将由 approved 进入 effective（履约中），并绑定所列租赁空间——准入闸 over_space 据此判定。需开启功能标志 contract.lifecycle_effective.enabled。",
  spaceIdsRequired: "请填写至少一个租赁空间 id（逗号分隔）。",
  viewSpaces: "查看已绑空间",
  boundSpaces: "已绑租赁空间",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(", ") : "-";
  return JSON.stringify(value);
}

function attachmentList(row: ContractRow) {
  return (row.attachmentIds ?? row.evidenceAttachmentIds ?? []).join(", ") || "-";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds, attachmentIds: request.attachmentIds, result };
}

// 后端写权限码（PlatformController：/contracts/{id}/approve）
const CONTRACT_APPROVE_PERMISSION = "contract.lifecycle.approve";
// ② 履约闭环 S1：合同生效写权限（/contracts/{id}/activate）
const CONTRACT_ACTIVATE_PERMISSION = "contract.lifecycle.activate";

export function ContractApprovalPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<ContractRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [draftTitle, setDraftTitle] = React.useState("主力店补充协议");
  const [approvalComment, setApprovalComment] = React.useState("主体、附件和日期均已核验");
  const [spaceIdsInput, setSpaceIdsInput] = React.useState("");
  const [boundSpaces, setBoundSpaces] = React.useState<string[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<ContractRow[]>("/api/v1/platform/contracts"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const attachmentCount = rows.reduce((count, row) => count + (row.attachmentIds?.length ?? row.evidenceAttachmentIds?.length ?? 0), 0);
  const draftCount = rows.filter((row) => ["draft", "pending", "returned"].includes(row.lifecycleStatus ?? row.status ?? "")).length;

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function createDraft() {
    if (!draftTitle.trim()) {
      setFormError(TEXT.draftTitleRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("contract-draft"),
      operatorNote: draftTitle,
      evidenceAttachmentIds: ["att-contract-draft"],
      merchantId: "merchant-1",
      contractSubjectId: "contract-subject-1",
      operationSubjectId: "operator-subject-1",
      settlementSubjectId: "settlement-subject-1",
      effectiveDate: "2026-07-01",
      expiryDate: "2027-06-30",
      rentTerms: { billingCycle: "monthly", baseRent: 12000 },
      shareholderRatioForReport: 25,
      attachmentIds: ["att-contract-draft"],
    };
    await runAction("contract_draft_created", request, () => api.post("/api/v1/platform/contracts", request));
  }

  function validateApprove() {
    if (!selected) {
      setFormError(TEXT.noContract);
      return false;
    }
    if (!approvalComment.trim()) {
      setFormError(TEXT.approvalCommentRequired);
      return false;
    }
    return true;
  }

  async function approveContract() {
    if (!selected) {
      setFormError(TEXT.noContract);
      return;
    }
    const request = {
      idempotencyKey: nextKey("contract-approve"),
      operatorNote: "完整后台合同审批",
      evidenceAttachmentIds: ["att-contract-approve"],
      decision: "approve",
      comment: approvalComment,
    };
    await runAction("contract_approved", request, () => api.post(`/api/v1/platform/contracts/${selected.id}/approve`, request));
  }

  // ② 履约闭环 S1：合同生效（approved→effective）+ 绑定租赁空间。
  function validateActivate() {
    if (!selected) {
      setFormError(TEXT.noContract);
      return false;
    }
    if (spaceIdsInput.split(",").map((s) => s.trim()).filter(Boolean).length === 0) {
      setFormError(TEXT.spaceIdsRequired);
      return false;
    }
    return true;
  }

  async function activateContract() {
    if (!selected) {
      setFormError(TEXT.noContract);
      return;
    }
    const spaceIds = spaceIdsInput.split(",").map((s) => s.trim()).filter(Boolean);
    const request = { idempotencyKey: nextKey("contract-activate"), spaceIds };
    await runAction("contract_activated", request, () => api.post(`/api/v1/platform/contracts/${selected.id}/activate`, request));
  }

  async function loadBoundSpaces() {
    if (!selected) {
      setFormError(TEXT.noContract);
      return;
    }
    setGlobalError(null);
    const spaces = await api.get<Array<{ spaceId: string }>>(`/api/v1/platform/contracts/${selected.id}/spaces`);
    setBoundSpaces((spaces ?? []).map((s) => s.spaceId));
  }

  return (
    <section data-testid="contract-approval-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="contract-approval-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="contract.viewed / contract.draft_saved / contract.lifecycle_changed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="contract-approval-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.archivedAttachments}</span><strong>{attachmentCount}</strong><small>{TEXT.attachments}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.draftCount}</span><strong>{draftCount}</strong><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected?.id ?? "-"}</strong><small>{selected?.lifecycleStatus ?? selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="contract-approval-form">
          <label>{TEXT.draftTitle}<input data-testid="contract-approval-draft-title" value={draftTitle} onChange={(event) => setDraftTitle(event.target.value)} /></label>
          <label>{TEXT.approvalComment}<input data-testid="contract-approval-comment" value={approvalComment} onChange={(event) => setApprovalComment(event.target.value)} /></label>
          <label>{TEXT.spaceIds}<input data-testid="contract-activate-space-ids" placeholder="sp_1, sp_2" value={spaceIdsInput} onChange={(event) => setSpaceIdsInput(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="contract-approval-actions">
          <button type="button" data-testid="contract-approval-create-draft" disabled={busyAction !== ""} onClick={() => void createDraft()}>{TEXT.createDraft}</button>
          <HighRiskConfirm
            label={TEXT.approve}
            impact="合同将进入已审批状态并写入审计留痕，下游结算/绑定按此合同生效。"
            disabled={busyAction !== ""}
            precheck={validateApprove}
            onConfirm={() => void approveContract()}
            permissionsState={effectivePermissions}
            permissions={[CONTRACT_APPROVE_PERMISSION]}
            data-testid="contract-approval-approve"
          />
          <HighRiskConfirm
            label={TEXT.activate}
            impact={TEXT.activateImpact}
            disabled={busyAction !== ""}
            precheck={validateActivate}
            onConfirm={() => void activateContract()}
            permissionsState={effectivePermissions}
            permissions={[CONTRACT_ACTIVATE_PERMISSION]}
            data-testid="contract-activate"
          />
          <button type="button" data-testid="contract-view-spaces" disabled={busyAction !== ""} onClick={() => void loadBoundSpaces()}>{TEXT.viewSpaces}</button>
        </div>
        <div className="admin-muted-action" data-testid="contract-approval-boundary">{TEXT.boundary}</div>
        {boundSpaces.length > 0 ? <div className="admin-muted-action" data-testid="contract-bound-spaces">{TEXT.boundSpaces}：{boundSpaces.join(", ")}</div> : null}
        {formError ? <div data-testid="contract-approval-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-approval-table">
            <thead><tr><th>{TEXT.select}</th><th>{TEXT.merchant}</th><th>{TEXT.contractSubject}</th><th>{TEXT.operationSubject}</th><th>{TEXT.settlementSubject}</th><th>{TEXT.effectiveDate}</th><th>{TEXT.expiryDate}</th><th>{TEXT.rentTerms}</th><th>{TEXT.shareholderRatio}</th><th>{TEXT.attachments}</th><th>{TEXT.status}</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id}>
                <td><button type="button" data-testid={`contract-approval-select-${row.id}`} onClick={() => setSelectedId(row.id)}>{TEXT.select}</button></td>
                <td>{row.merchantId ?? "-"}</td>
                <td>{row.contractSubjectId ?? "-"}</td>
                <td>{row.operationSubjectId ?? "-"}</td>
                <td>{row.settlementSubjectId ?? "-"}</td>
                <td>{row.effectiveDate ?? "-"}</td>
                <td>{row.expiryDate ?? "-"}</td>
                <td>{displayValue(row.rentTerms)}</td>
                <td>{displayValue(row.shareholderRatioForReport)}</td>
                <td>{attachmentList(row)}</td>
                <td>{row.lifecycleStatus ?? row.status ?? "-"}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
