import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, DataCleanupJobRow } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { cnDate } from "../components/RowDetailDrawer";

// 集中数据清理工具（数据删除四层之第四层：Tier-D 集中清理入口）。
// 用户反馈"只看到存档没看到删除"——本页给运营一个集中入口批量清理旧数据，但保留全部安全闸：
//  ① 选清理范围+条件 → ② 预览（试运行，只统计不删）→ ③ 提交清理申请 → ④ 另一账号批准并执行（禁自审）/驳回。
// 后端权限：发起+预览 data.cleanup.request；审批+执行 data.cleanup.approve（职责分离）。
const REQUEST_PERMISSION = "data.cleanup.request";
const APPROVE_PERMISSION = "data.cleanup.approve";

// 支持的清理范围（与后端 DataCleanupScope 枚举一一对应）。全部只清理"已存档超 N 天"的配置类实体，绝不删活数据。
const SCOPE_OPTIONS: { value: string; label: string }[] = [
  { value: "archived_entitlement_templates", label: "已存档的通票权益模板" },
  { value: "archived_merchants", label: "已存档的商户档案" },
  { value: "archived_ticket_types", label: "已存档的票种" },
  { value: "archived_ticket_price_rules", label: "已存档的票价规则" },
  { value: "archived_bar_products", label: "已存档的酒吧商品" },
  { value: "archived_bar_product_categories", label: "已存档的酒吧商品分类" },
  { value: "archived_bar_combos", label: "已存档的酒吧套餐" },
];

const DEFAULT_SCOPE = "archived_bar_products";

function scopeLabel(code: string): string {
  return SCOPE_OPTIONS.find((o) => o.value === code)?.label ?? code;
}

export function DataCleanupPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [jobs, setJobs] = React.useState<DataCleanupJobRow[]>([]);
  const [scopeType, setScopeType] = React.useState(DEFAULT_SCOPE);
  const [olderThanDays, setOlderThanDays] = React.useState("30");
  const [preview, setPreview] = React.useState<DataCleanupJobRow | null>(null);
  const [reason, setReason] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const rows = await api.get<DataCleanupJobRow[]>("/api/v1/ops/data-cleanup/jobs");
      setJobs(rows);
    },
    { onError: setGlobalError, fallbackMessage: "清理作业读取失败" },
  );

  // ② 预览（试运行）：只统计将删条数+样本，不删任何数据。
  async function runPreview() {
    const days = Number(olderThanDays);
    if (!Number.isInteger(days) || days < 0) {
      setFormError("请填写有效的存档天数（0 或正整数）。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setBusy(true);
    try {
      const job = await api.post<DataCleanupJobRow>("/api/v1/ops/data-cleanup/preview", { scopeType, olderThanDays: days });
      setPreview(job);
      setActionResult({ action: "data_cleanup_preview", scopeType, previewCount: job.previewCount });
      await reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "预览失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setBusy(false);
    }
  }

  // ③ 提交清理申请：dry_run → pending_approval（待另一账号审批）。
  async function submitForApproval() {
    if (!preview?.id) {
      setFormError("请先点「预览（试运行）」生成待清理项，再提交申请。");
      return;
    }
    setFormError("");
    setGlobalError(null);
    setBusy(true);
    try {
      const job = await api.post<DataCleanupJobRow>(`/api/v1/ops/data-cleanup/jobs/${preview.id}/submit`, { reason });
      setActionResult({ action: "data_cleanup_submit", jobId: job.id, status: job.status });
      setPreview(null);
      setReason("");
      await reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "提交清理申请失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setBusy(false);
    }
  }

  // ④ 审批：批准并执行真删（禁自审由后端强制）或驳回。
  async function decide(jobId: string, decision: "approved" | "rejected") {
    setGlobalError(null);
    setBusy(true);
    try {
      const job = await api.post<DataCleanupJobRow>(`/api/v1/ops/data-cleanup/jobs/${jobId}/approve`, { decision });
      setActionResult({ action: "data_cleanup_decide", jobId, decision, deletedCount: job.deletedCount ?? 0 });
      await reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "审批失败";
      setGlobalError(message);
      setActionResult({ action: "data_cleanup_decide", jobId, decision, status: "failed", message });
    } finally {
      setBusy(false);
    }
  }

  const pending = jobs.filter((j) => j.status === "pending_approval");
  const history = jobs.filter((j) => j.status === "executed" || j.status === "rejected" || j.status === "cancelled");

  return (
    <div data-testid="data-cleanup-page">
      <AdminPageHeader
        title="数据清理工具"
        description="集中清理旧的、已存档的配置数据。只能清理「已存档超过 N 天」的数据，绝不删除在用数据；每次清理都要先预览、再由另一位有权限的同事审批后才真正删除，全程留痕可追溯。"
      />

      {/* ①②③ 发起区：选范围 + 条件 → 预览 → 提交申请 */}
      <section className="admin-section-highlight" data-testid="data-cleanup-request">
        <h2>发起清理</h2>
        <div className="admin-form-grid">
          <label>
            清理范围
            <select
              data-testid="data-cleanup-scope"
              value={scopeType}
              onChange={(e) => {
                setScopeType(e.target.value);
                setPreview(null);
              }}
            >
              {SCOPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            已存档超过（天）
            <input
              data-testid="data-cleanup-older-than-days"
              type="number"
              min={0}
              value={olderThanDays}
              onChange={(e) => {
                setOlderThanDays(e.target.value);
                setPreview(null);
              }}
            />
          </label>
        </div>

        <div className="admin-form-row">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[REQUEST_PERMISSION]}
            disabled={busy}
            onClick={runPreview}
            data-testid="data-cleanup-preview-button"
          >
            预览（试运行）
          </PermissionButton>
        </div>

        {preview ? (
          <div className="admin-section-highlight" data-testid="data-cleanup-preview-result">
            <p>
              <strong>仅预览，尚未删除。</strong>
              本次将清理「{scopeLabel(preview.scopeType)}」中 <strong data-testid="data-cleanup-preview-count">{preview.previewCount}</strong> 条已存档数据。
            </p>
            {preview.previewSample && preview.previewSample.length > 0 ? (
              <ul data-testid="data-cleanup-preview-sample">
                {preview.previewSample.slice(0, 10).map((s, i) => (
                  <li key={`${s.id}-${i}`}>
                    {s.id}
                    {s.archivedAt ? `（存档于 ${cnDate(s.archivedAt)}）` : ""}
                  </li>
                ))}
              </ul>
            ) : (
              <p>没有符合条件的已存档数据，无需清理。</p>
            )}
            <label>
              清理事由（便于审批人核对）
              <input
                data-testid="data-cleanup-reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="例如：清理半年前测试期录入的废弃配置"
              />
            </label>
            <div className="admin-form-row">
              <PermissionButton
                permissionsState={effectivePermissions}
                permissions={[REQUEST_PERMISSION]}
                disabled={busy || preview.previewCount === 0}
                onClick={submitForApproval}
                data-testid="data-cleanup-submit-button"
              >
                提交清理申请
              </PermissionButton>
            </div>
          </div>
        ) : null}

        {formError ? (
          <div className="admin-inline-error" data-testid="data-cleanup-error">
            {formError}
          </div>
        ) : null}
      </section>

      {/* ④ 待审批区：另一账号批准并执行 / 驳回（禁自审由后端强制） */}
      <section className="admin-section-highlight" data-testid="data-cleanup-pending">
        <h2>待审批的清理申请</h2>
        <AdminDataState loading={loading} error={error} empty={pending.length === 0} onRetry={reload}>
          <table className="admin-table">
            <thead>
              <tr>
                <th>清理范围</th>
                <th>将删条数</th>
                <th>发起人</th>
                <th>事由</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {pending.map((job) => (
                <tr key={job.id} data-testid={`data-cleanup-pending-row-${job.id}`}>
                  <td>{scopeLabel(job.scopeType)}</td>
                  <td>{job.previewCount}</td>
                  <td>{job.requestedBy ?? "—"}</td>
                  <td>{job.reason ?? "—"}</td>
                  <td>
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={[APPROVE_PERMISSION]}
                      disabled={busy}
                      onClick={() => decide(job.id, "approved")}
                      data-testid={`data-cleanup-approve-${job.id}`}
                    >
                      批准并执行
                    </PermissionButton>
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={[APPROVE_PERMISSION]}
                      disabled={busy}
                      onClick={() => decide(job.id, "rejected")}
                      data-testid={`data-cleanup-reject-${job.id}`}
                    >
                      驳回
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </AdminDataState>
      </section>

      {/* 历史区：已执行/已驳回作业留痕（删了几条可追溯） */}
      <section className="admin-section-highlight" data-testid="data-cleanup-history">
        <h2>清理历史</h2>
        <table className="admin-table">
          <thead>
            <tr>
              <th>清理范围</th>
              <th>状态</th>
              <th>删除条数</th>
              <th>发起人</th>
              <th>审批人</th>
            </tr>
          </thead>
          <tbody>
            {history.map((job) => (
              <tr key={job.id} data-testid={`data-cleanup-history-row-${job.id}`}>
                <td>{scopeLabel(job.scopeType)}</td>
                <td>{formatBusinessStatus(job.status)}</td>
                <td>{job.deletedCount ?? "—"}</td>
                <td>{job.requestedBy ?? "—"}</td>
                <td>{job.approvedBy ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
