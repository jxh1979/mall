// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 员工档案（staff_profiles）。客户端 unwrap 已 camel 化。
type StaffProfileRow = {
  id: string;
  name?: string;
  merchantId?: string | null;
  userId?: string | null;
  registrationStatus?: string;
  status?: string;
  salesPosition?: boolean;
};

const MANAGE = ["staff.profile.review"];

/**
 * 员工档案页（ADM-STF-004，V349）。
 *
 * 本页的业务用途只有一个且很重：维护每个员工是不是「销售岗」。
 * 这一个勾直接决定该员工卖出的卡座按哪档费率计提提成（销售岗按销售人员费率、其余按非销售人员费率，
 * 两档费率在「结算配置中心 → 提成与积分」维护）。所以：
 *   · 改动必须填事由，落审计留痕，对账时能回溯是谁哪天为什么改的；
 *   · 改岗位只影响此后新开的账单，不回算已开账单的历史提成——否则改一次岗位就悄悄改写旧账。
 */
export function StaffProfilesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<StaffProfileRow[]>([]);
  const [reason, setReason] = React.useState("");
  const [initialPassword, setInitialPassword] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submittingId, setSubmittingId] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<StaffProfileRow[]>("/api/v1/staff/profiles");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "员工档案读取失败" },
  );

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
      setActionResult({ action: decision === "approve" ? "员工注册审批通过" : "员工注册已驳回", staffProfileId: row.id });
      setInitialPassword("");
      reload();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "注册审批失败");
    } finally { setSubmittingId(""); }
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
      setActionResult({ action: "staff_sales_position_changed", staffProfileId: row.id, salesPosition, reason: trimmedReason });
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
        description="维护员工的「销售岗」标识。设有销售岗的员工卖出卡座按销售人员费率计提提成，其余按非销售人员费率；两档费率在「结算配置中心 → 提成与积分」维护。岗位在本页维护，不按单次是否指派销售人员临时判断；改动只影响此后新开的账单，不回算历史提成。"
      />
      <AuditTrailHint event="staff.profile.sales_position_changed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
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
              <tr><th>姓名</th><th>所属商户</th><th>员工账号</th><th>注册状态</th><th>状态</th><th>销售岗</th><th>操作</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`staff-profile-row-${row.id}`}>
                  <td>{row.name ?? "—"}</td>
                  <td>{row.merchantId ?? "平台"}</td>
                  <td className="admin-id-cell" title={row.userId ?? ""}>{row.userId ?? "—"}</td>
                  <td>{formatBusinessStatus(row.registrationStatus)}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td data-testid={`staff-profile-sales-position-${row.id}`}>{row.salesPosition ? "是" : "否"}</td>
                  <td>
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
