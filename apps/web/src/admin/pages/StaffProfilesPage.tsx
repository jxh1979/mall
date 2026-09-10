// 文件版本：V0.05
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton, useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

// 员工档案（staff_profiles）。客户端 unwrap 已 camel 化。
type StaffProfileRow = {
  id: string;
  name?: string;
  merchantId?: string | null;
  userId?: string | null;
  registrationStatus?: string;
  status?: string;
  employmentStatus?: string;
  salesPosition?: boolean;
  phone?: string | null;
  archivedAt?: string | null;
  deleteStatus?: string;
};

const EMPLOYMENT = { pending_entry: "待入职", employed: "在职", trial: "试岗", suspended: "停岗", departed: "离职" };
const MANAGE = ["staff.profile.review"];

/**
 * 员工档案页（ADM-STF-004，V349）。
 *
 * 办理员工注册审批、档案存档/删除，并维护员工是否为「销售岗」。
 * 这一个勾直接决定该员工卖出的卡座按哪档费率计提提成（销售岗按销售人员费率、其余按非销售人员费率，
 * 两档费率在「结算配置中心 → 提成与积分」维护）。所以：
 *   · 改动必须填事由，落审计留痕，对账时能回溯是谁哪天为什么改的；
 *   · 改岗位只影响此后新开的账单，不回算已开账单的历史提成——否则改一次岗位就悄悄改写旧账。
 */
export function StaffProfilesPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const canManage = useCan(effectivePermissions, MANAGE);
  const [rows, setRows] = React.useState<StaffProfileRow[]>([]);
  const [reason, setReason] = React.useState("");
  const [initialPassword, setInitialPassword] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submittingId, setSubmittingId] = React.useState("");
  // U03-008：默认不把已存档档案（含脱敏手机号）下到浏览器；勾选后才带 include_archived=true 重新拉取，不在客户端过滤。
  const [includeArchived, setIncludeArchived] = React.useState(false);
  const includeArchivedRef = React.useRef(false);
  const visibleRows = rows;
  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<StaffProfileRow[]>(includeArchivedRef.current ? "/api/v1/staff/profiles?include_archived=true" : "/api/v1/staff/profiles");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "员工档案读取失败" },
  );

  async function changeArchive(row: StaffProfileRow, action: "archive" | "unarchive" | "delete-request" | "delete-approve") {
    if ((action === "archive" || action === "delete-request") && !reason.trim()) {
      setFormError("请先填写改动事由。");
      return;
    }
    setFormError("");
    setSubmittingId(row.id);
    try {
      await api.post("/api/v1/staff/profiles/" + encodeURIComponent(row.id) + "/" + action,
        action === "delete-approve" ? { decision: "approved", reason: reason.trim() } : { reason: reason.trim() });
      setActionResult({ 结果: action === "archive" ? "员工档案已存档" : action === "unarchive" ? "员工档案已恢复"
        : action === "delete-request" && !isMaster ? "删除申请已提交，等待最高权限审批" : "员工档案已删除", 员工档案: row.id });
      setReason("");
      await reload();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "员工档案操作失败");
    } finally { setSubmittingId(""); }
  }

  async function reviewRegistration(row: StaffProfileRow, decision: "approve" | "reject") {
    if (decision === "approve" && !initialPassword.trim()) { setFormError("审批通过须填写初始密码。"); return; }
    if (decision === "reject" && !reason.trim()) { setFormError("驳回须填写理由。"); return; }
    setFormError("");
    setSubmittingId(row.id);
    try {
      await api.post(`/api/v1/staff/registrations/${encodeURIComponent(row.id)}/review`, {
        decision, initialPassword: decision === "approve" ? initialPassword : undefined,
        rejectReason: decision === "reject" ? reason.trim() : undefined,
        operatorNote: reason.trim() || "公司复核员工注册",
      });
      setActionResult({ 结果: decision === "approve" ? "员工注册审批通过" : "员工注册已驳回", 员工档案: row.id });
      setInitialPassword("");
      reload();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "注册审批失败");
    } finally { setSubmittingId(""); }
  }

  async function changeEmployment(row: StaffProfileRow, employmentStatus: string) {
    if (!reason.trim()) { setFormError("请先填写改动事由。"); return; }
    setFormError("");
    setSubmittingId(row.id);
    try {
      await api.post("/api/v1/staff/profiles/" + encodeURIComponent(row.id) + "/employment-status", { employmentStatus, reason: reason.trim() });
      setActionResult({ 结果: "员工状态已更新", 员工档案: row.id });
      setReason("");
      await reload();
    } catch (error) { setFormError(error instanceof Error ? error.message : "员工状态更新失败"); }
    finally { setSubmittingId(""); }
  }

  async function toggleSalesPosition(row: StaffProfileRow) {
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setFormError("请先填写改动事由：销售岗决定提成按哪档费率发，必须留痕。");
      return;
    }
    const salesPosition = !row.salesPosition;
    setFormError("");
    setGlobalError(null);
    setSubmittingId(row.id);
    try {
      await api.post(`/api/v1/staff/profiles/${row.id}/sales-position`, { salesPosition, reason: trimmedReason });
      setActionResult({ 结果: "员工销售岗已更新", 员工档案: row.id, 销售岗: salesPosition ? "是" : "否", 事由: trimmedReason });
      setReason("");
      reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "销售岗设置失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setSubmittingId("");
    }
  }

  return (
    <section data-testid="staff-profiles-page">
      <AdminPageHeader
        title="员工档案"
        description="办理员工注册审批、档案存档及删除。销售岗员工卖出卡座按销售人员费率计提提成，其余按非销售人员费率；两档费率在「结算配置中心 → 提成与积分」维护。销售岗改动只影响此后新开的账单，不回算历史提成。"
      />
      <AuditTrailHint event="staff.profile.sales_position_changed" />
      <label><input type="checkbox" data-testid="staff-profiles-include-archived" checked={includeArchived} onChange={(event) => { includeArchivedRef.current = event.target.checked; setIncludeArchived(event.target.checked); void reload(); }} />显示已存档</label>
      <AdminDataState loading={loading} error={error} empty={visibleRows.length === 0} onRetry={reload}>
        {rows.some((row) => row.registrationStatus === "submitted") ? <label>注册审批初始密码
          <input type="password" data-testid="staff-registration-password" value={initialPassword} onChange={(event) => setInitialPassword(event.target.value)} />
        </label> : null}
        <div className="admin-form-row" data-testid="staff-profiles-reason-row">
          <label>改动事由
            <input
              aria-label="改动事由"
              placeholder="例：转岗销售，2026-07 起承接卡座销售"
              data-testid="staff-profiles-reason"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
          <span className="admin-muted-action">销售岗改动直接影响提成计提档，事由必填并留审计。</span>
        </div>
        {formError ? <div data-testid="staff-profiles-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="staff-profiles-table">
            <thead>
              <tr><th>姓名</th><th>手机号</th><th>所属商户</th><th>员工账号</th><th>注册状态</th><th>员工状态</th><th>销售岗</th><th>操作</th></tr>
            </thead>
            <tbody>
              {visibleRows.map((row) => (
                <tr key={row.id} data-testid={`staff-profile-row-${row.id}`}>
                  <td>{row.name ?? "—"}</td>
                  <td>{row.phone ?? "—"}</td>
                  <td>{row.merchantId ?? "平台"}</td>
                  <td className="admin-id-cell" title={row.userId ?? ""}>{row.userId ?? "—"}</td>
                  <td>{formatBusinessStatus(row.registrationStatus)}</td>
                  <td>
                    <select aria-label={"员工状态-" + row.id} value={row.employmentStatus ?? "pending_entry"}
                      disabled={submittingId === row.id || !canManage}
                      onChange={(event) => void changeEmployment(row, event.target.value)}>
                      {Object.entries(EMPLOYMENT).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </td>
                  <td data-testid={`staff-profile-sales-position-${row.id}`}>{row.salesPosition ? "是" : "否"}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["staff.profile.submit"]}
                      data-testid={`staff-profile-${row.archivedAt ? "unarchive" : "archive"}-${row.id}`}
                      disabled={submittingId === row.id} onClick={() => void changeArchive(row, row.archivedAt ? "unarchive" : "archive")}>
                      {row.archivedAt ? "恢复档案" : "存档"}
                    </PermissionButton>
                    {row.archivedAt && row.deleteStatus !== "pending" ? <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                      data-testid={`staff-profile-delete-request-${row.id}`} disabled={submittingId === row.id}
                      onClick={() => void changeArchive(row, "delete-request")}>{isMaster ? "直接删除" : "申请删除"}</PermissionButton> : null}
                    {row.deleteStatus === "pending" ? isMaster ? <PermissionButton permissionsState={effectivePermissions} permissions={["staff.profile.approve"]}
                      data-testid={`staff-profile-delete-approve-${row.id}`} disabled={submittingId === row.id}
                      onClick={() => void changeArchive(row, "delete-approve")}>批准删除</PermissionButton> : <span>待最高权限审批</span> : null}
                    {row.registrationStatus === "submitted" ? <>
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`staff-registration-approve-${row.id}`} disabled={submittingId === row.id} onClick={() => void reviewRegistration(row, "approve")}>批准注册</PermissionButton>
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`staff-registration-reject-${row.id}`} disabled={submittingId === row.id} onClick={() => void reviewRegistration(row, "reject")}>驳回注册</PermissionButton>
                    </> : null}
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={MANAGE}
                      data-testid={`staff-profile-toggle-sales-${row.id}`}
                      disabled={submittingId === row.id}
                      onClick={() => void toggleSalesPosition(row)}
                    >
                      {row.salesPosition ? "取消销售岗" : "设为销售岗"}
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
