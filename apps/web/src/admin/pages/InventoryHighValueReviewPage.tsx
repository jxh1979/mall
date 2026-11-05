// 文件版本：V0.02
import React from "react";
import { withBase } from "../../basePath";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import { useAdminResource } from "../hooks/useAdminResource";
import { cnDate } from "../components/RowDetailDrawer";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";

// 第十八批第 24 条 24-R2（ENUM:164「报损/盘点差异/存酒作废须照片+视频双证据+主管复核」；BSM:1658「主管复核强制项」含
// 「高价值报损/盘点差异/存酒作废」；决策台账第十四批第 4 条「门店主管与财务审批」）：库存高价值复核后台审批页（ADM-INV-004）。
// 主管复核码批报损、盘点差异、存酒作废，以及存酒库房存货处置的门店主管一道；财务应收应付管理码只批存酒库房存货处置的财务一道。
// 申请人不得审批自己的申请（PGM:529），由后端拦截，页面显示中文原因。
const SUPERVISOR = "inventory.high_value.review";
const FINANCE = "finance.ar_ap.manage";
const BASE = "/api/v1/inventory/high-value-reviews";

type SubjectSummary = {
  itemName?: string | null;
  quantity?: number | null;
  unit?: string | null;
  reason?: string | null;
  savedBottleId?: string | null;
};
type ReviewRow = {
  id: string;
  subjectType: string;
  subjectId: string;
  photoAttachmentIds?: string[];
  videoAttachmentIds?: string[];
  reviewStatus: string;
  createdBy?: string | null;
  createdAt?: string;
  supervisorApprovedBy?: string | null;
  financeApprovedBy?: string | null;
  subjectSummary?: SubjectSummary | null;
};

const SUBJECT_LABELS: Record<string, string> = {
  damage_report: "高价值报损",
  stock_variance: "高价值盘点差异",
  saved_bottle_void: "高价值存酒作废",
  saved_bottle_stock_disposal: "存酒库房存货处置",
  // 第十八批 T 段第 157 条 157-R4（BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求」）：复核对象为酒吧订单。
  bar_order_bad_debt: "高价值挂起订单转坏账",
};
const ERROR_LABELS: Record<string, string> = {
  HIGH_VALUE_REVIEW_SELF_APPROVAL: "审批人不得是发起人。",
  HIGH_VALUE_REVIEW_SAME_APPROVER: "门店主管与财务两道审批不得由同一人完成。",
  HIGH_VALUE_REVIEW_NOT_PENDING: "该复核单已处理，请刷新。",
  CROSS_MERCHANT_FORBIDDEN: "该复核单不属于当前商户。",
};

function subjectText(row: ReviewRow): string {
  if (row.subjectType === "bar_order_bad_debt") return `挂起订单 ${row.subjectId}`;
  const s = row.subjectSummary;
  if (!s) return row.subjectId;
  const head = row.subjectType === "saved_bottle_void" ? `存酒 ${s.savedBottleId ?? row.subjectId} · ` : "";
  return `${head}${s.itemName ?? "-"} × ${s.quantity ?? "-"} ${s.unit ?? ""}${s.reason ? `（${s.reason}）` : ""}`;
}

function evidenceLinks(ids: string[] | undefined, label: string) {
  return (ids ?? []).map((id, index) => (
    <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">
      {label}{index + 1}{" "}
    </a>
  ));
}

export function InventoryHighValueReviewPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<ReviewRow[]>([]);
  const [opinion, setOpinion] = React.useState("");
  const [busy, setBusy] = React.useState<string | null>(null);
  const [actionError, setActionError] = React.useState("");
  const canSupervise = hasPermissions(effectivePermissions, [SUPERVISOR]);

  const { loading, error, reload } = useAdminResource(
    async () => { setRows(await api.get<ReviewRow[]>(BASE)); },
    { onError: setGlobalError, fallbackMessage: "高价值复核单读取失败", onStart: () => setGlobalError(null) },
  );

  async function decide(row: ReviewRow, decision: "approved" | "rejected", role?: "supervisor" | "finance") {
    const key = `${row.id}-${decision}-${role ?? "review"}`;
    const requestBody = {
      idempotency_key: `admin-hvr-${key}-${Date.now()}`,
      decision,
      reason: opinion.trim() || undefined,
      ...(role ? { role } : {}),
    };
    setBusy(key);
    setActionError("");
    try {
      const result = await api.post(`${BASE}/${encodeURIComponent(row.id)}/review`, requestBody);
      setActionResult({ action: "inventory_high_value_review_decided", requestBody, result });
      await reload();
    } catch (e) {
      const code = e instanceof ApiError ? e.detailCode : undefined;
      setActionError((code && ERROR_LABELS[code]) || (e as Error)?.message || "复核操作失败");
    } finally {
      setBusy(null);
    }
  }

  function actions(row: ReviewRow) {
    if (row.reviewStatus !== "pending") return "-";
    if (row.subjectType !== "saved_bottle_stock_disposal") {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={[SUPERVISOR]} disabled={busy !== null}
            data-testid={`hvr-approve-${row.id}`} onClick={() => void decide(row, "approved")}>批准</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={[SUPERVISOR]} disabled={busy !== null}
            data-testid={`hvr-reject-${row.id}`} onClick={() => void decide(row, "rejected")}>驳回</PermissionButton>
        </>
      );
    }
    // 存酒库房存货处置：门店主管与财务两道，任一道驳回即驳回。
    const rejectRole = canSupervise ? "supervisor" : "finance";
    return (
      <>
        {row.supervisorApprovedBy ? null : (
          <PermissionButton permissionsState={effectivePermissions} permissions={[SUPERVISOR]} disabled={busy !== null}
            data-testid={`hvr-approve-sup-${row.id}`} onClick={() => void decide(row, "approved", "supervisor")}>门店主管批准</PermissionButton>
        )}
        {row.financeApprovedBy ? null : (
          <PermissionButton permissionsState={effectivePermissions} permissions={[FINANCE]} disabled={busy !== null}
            data-testid={`hvr-approve-fin-${row.id}`} onClick={() => void decide(row, "approved", "finance")}>财务批准</PermissionButton>
        )}
        <PermissionButton permissionsState={effectivePermissions} permissions={[rejectRole === "supervisor" ? SUPERVISOR : FINANCE]}
          disabled={busy !== null} data-testid={`hvr-reject-${row.id}`} onClick={() => void decide(row, "rejected", rejectRole)}>驳回</PermissionButton>
      </>
    );
  }

  return (
    <section data-testid="inventory-high-value-review-page">
      <AdminPageHeader
        title="高价值复核"
        description="高价值报损、盘点差异、存酒作废须照片＋视频并经主管复核才生效；存酒库房存货处置须门店主管与财务两道审批。发起人不得审批自己的申请。"
      />
      <label>审批意见（可选）
        <input data-testid="hvr-opinion" value={opinion} onChange={(event) => setOpinion(event.target.value)} />
      </label>
      {actionError ? <div className="admin-inline-error" data-testid="hvr-error">{actionError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="hvr-table">
            <thead><tr><th>对象类型</th><th>物品与数量</th><th>发起人与时间</th><th>照片／视频</th><th>状态</th><th>已批人</th><th>操作</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`hvr-row-${row.id}`}>
                  <td>{SUBJECT_LABELS[row.subjectType] ?? row.subjectType}</td>
                  <td>{subjectText(row)}</td>
                  <td>{row.createdBy ?? "-"} · {cnDate(row.createdAt)}</td>
                  <td>{evidenceLinks(row.photoAttachmentIds, "照片")}{evidenceLinks(row.videoAttachmentIds, "视频")}</td>
                  <td>{formatBusinessStatus(row.reviewStatus)}</td>
                  <td>
                    {row.supervisorApprovedBy ? `门店主管 ${row.supervisorApprovedBy}` : ""}
                    {row.financeApprovedBy ? ` 财务 ${row.financeApprovedBy}` : ""}
                  </td>
                  <td style={{ display: "flex", gap: 8 }}>{actions(row)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
