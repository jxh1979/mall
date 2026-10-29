// 文件版本：V0.09
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

// CG-07 派驻记录（staff_dispatch_records）：员工、所属主体（登记时取自员工档案）、派驻酒吧、派驻起止时间。
type StaffDispatchRow = {
  id: string;
  userId?: string | null;
  companyId?: string | null;
  merchantId?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  status?: string;
  note?: string | null;
};

type HandoverOverview = {
  businessAccessStopped: boolean;
  revisions: { reviewId: string; beforeStatus: string; afterStatus: string; reason: string; reviewStatus: string; effectiveAt: string; appliedAt?: string | null }[];
  pending: { businessType: string; businessId: string; businessName: string; status: string; projectId?: string | null; merchantId?: string | null; assignable: boolean; holdReason?: string | null }[];
  history: { id: string; businessType: string; businessId: string; previousUserId: string; targetUserId: string; reason: string; assignedBy: string; assignedAt: string; responsibilityStartedAt?: string | null }[];
};

const EMPLOYMENT = { pending_entry: "待入职", employed: "在职", trial: "试岗", suspended: "停岗", departed: "离职" };
const DISPATCH_STATUS: Record<string, string> = { active: "进行中", ended: "已结束" };
const todayIso = () => new Date().toISOString().slice(0, 10);
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
export function StaffProfilesPage({ api, user, setActionResult, setGlobalError, effectivePermissions, reportViewedObjects }: AdminPageProps) {
  const canManage = useCan(effectivePermissions, MANAGE);
  const canDispatch = canManage && !user.merchantId && !["merchant", "merchant_admin", "outsourced_vendor"].includes(user.accountType);
  const [rows, setRows] = React.useState<StaffProfileRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("staff_profile", rows), [rows, reportViewedObjects]);
  const [reason, setReason] = React.useState("");
  const [effectiveAt, setEffectiveAt] = React.useState("");
  const [handoverProfile, setHandoverProfile] = React.useState<StaffProfileRow | null>(null);
  const [handover, setHandover] = React.useState<HandoverOverview | null>(null);
  const [handoverTarget, setHandoverTarget] = React.useState("");
  const [handoverReason, setHandoverReason] = React.useState("");
  const [handoverLoading, setHandoverLoading] = React.useState(false);
  const [initialPassword, setInitialPassword] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submittingId, setSubmittingId] = React.useState("");
  // CG-07 派驻记录登记/结束（BSM「派驻人员的成本与报酬」：所属主体、派驻酒吧、派驻起止时间须分别记录）。
  const [dispatches, setDispatches] = React.useState<StaffDispatchRow[]>([]);
  const [dispatchUser, setDispatchUser] = React.useState("");
  const [dispatchMerchant, setDispatchMerchant] = React.useState("");
  const [dispatchStart, setDispatchStart] = React.useState(todayIso);
  const [dispatchNote, setDispatchNote] = React.useState("");
  const [dispatchEndDate, setDispatchEndDate] = React.useState(todayIso);
  // U03-008：默认不把已存档档案（含脱敏手机号）下到浏览器；勾选后才带 include_archived=true 重新拉取，不在客户端过滤。
  const [includeArchived, setIncludeArchived] = React.useState(false);
  const includeArchivedRef = React.useRef(false);
  const visibleRows = rows;
  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [items, dispatchItems] = await Promise.all([
        api.get<StaffProfileRow[]>(includeArchivedRef.current ? "/api/v1/staff/profiles?include_archived=true" : "/api/v1/staff/profiles"),
        api.get<StaffDispatchRow[]>("/api/v1/staff/dispatches"),
      ]);
      setRows(items ?? []);
      setDispatches(Array.isArray(dispatchItems) ? dispatchItems : []);
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
      const review = await api.post<{ reviewId?: string }>("/api/v1/staff/profiles/" + encodeURIComponent(row.id) + "/employment-status",
        { employmentStatus, reason: reason.trim(), ...(effectiveAt ? { effectiveAt: new Date(effectiveAt).toISOString() } : {}) });
      setActionResult({ 结果: "已发起员工状态变更复核", 员工档案: row.id, 目标状态: EMPLOYMENT[employmentStatus as keyof typeof EMPLOYMENT] ?? employmentStatus,
        复核单: review?.reviewId ?? "—", 说明: "人力会签按员工部门自动确定，直属起共三级；配置缺失时保持待处理，补齐后重新生成审批链，全部通过且到生效时间才变更；停离职到点撤权，未交接不延后" });
      setReason("");
      await reload();
    } catch (error) { setFormError(error instanceof Error ? error.message : "员工状态变更复核发起失败"); }
    finally { setSubmittingId(""); }
  }

  async function openHandover(row: StaffProfileRow) {
    setHandoverProfile(row); setHandover(null); setHandoverLoading(true); setFormError("");
    try { setHandover(await api.get<HandoverOverview>(`/api/v1/staff/profiles/${encodeURIComponent(row.id)}/handover`)); }
    catch (error) { setFormError(error instanceof Error ? error.message : "员工交接读取失败"); }
    finally { setHandoverLoading(false); }
  }

  async function assignHandover(item: HandoverOverview["pending"][number]) {
    if (!handoverProfile || !handoverTarget.trim() || !handoverReason.trim()) { setFormError("交接须填写接手员工账号和交接原因。"); return; }
    setFormError(""); setSubmittingId(item.businessType + ":" + item.businessId);
    try {
      await api.post(`/api/v1/staff/profiles/${encodeURIComponent(handoverProfile.id)}/handover`, {
        businessType: item.businessType, businessId: item.businessId, targetUserId: handoverTarget.trim(), reason: handoverReason.trim(),
      });
      setActionResult({ 结果: "原业务责任已交接", 业务: item.businessName, 业务编号: item.businessId, 接手员工: handoverTarget.trim() });
      await openHandover(handoverProfile);
    } catch (error) { setFormError(error instanceof Error ? error.message : "业务交接失败"); }
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

  async function createDispatch() {
    if (!dispatchUser.trim() || !dispatchMerchant.trim() || !dispatchStart) {
      setFormError("登记派驻须填写员工账号、派驻酒吧与开始日期。");
      return;
    }
    setFormError("");
    setSubmittingId("dispatch-create");
    try {
      await api.post("/api/v1/staff/dispatches", {
        userId: dispatchUser.trim(), merchantId: dispatchMerchant.trim(), startDate: dispatchStart, note: dispatchNote.trim() || undefined,
      });
      setActionResult({ 结果: "派驻记录已登记", 员工账号: dispatchUser.trim(), 派驻酒吧: dispatchMerchant.trim(), 开始日期: dispatchStart });
      setDispatchUser(""); setDispatchMerchant(""); setDispatchNote("");
      await reload();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "派驻登记失败");
    } finally { setSubmittingId(""); }
  }

  async function endDispatch(row: StaffDispatchRow) {
    if (!reason.trim()) { setFormError("结束派驻须填写改动事由。"); return; }
    setFormError("");
    setSubmittingId(row.id);
    try {
      await api.post(`/api/v1/staff/dispatches/${encodeURIComponent(row.id)}/end`, { endDate: dispatchEndDate, reason: reason.trim() });
      setActionResult({ 结果: "派驻记录已结束", 派驻记录: row.id, 结束日期: dispatchEndDate });
      setReason("");
      await reload();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "结束派驻失败");
    } finally { setSubmittingId(""); }
  }

  return (
    <section data-testid="staff-profiles-page">
      <AdminPageHeader
        title="员工档案"
        description="办理员工注册审批、档案存档及删除。员工状态（待入职/在职/试岗/停岗/离职）的任何变更都只是发起复核：须经 人力会签与该员工直属起共三个组织层级全部通过且到生效时间才生效。停岗、离职生效即停止业务权限与原会话，未交接事项继续由有权人员承接；设备交接、工资结清继续办理。销售岗员工卖出卡座按销售人员费率计提提成，其余按非销售人员费率；两档费率在「结算配置中心 → 提成与积分」维护。销售岗改动只影响此后新开的账单，不回算历史提成。"
      />
      <AuditTrailHint event="staff.profile.employment_status_change_requested / staff.profile.employment_status_changed / staff.profile.sales_position_changed" />
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
          <label>员工状态生效时间（本地时间，留空为批准后生效）
            <input type="datetime-local" aria-label="员工状态生效时间" value={effectiveAt} onChange={(event) => setEffectiveAt(event.target.value)} />
          </label>
          <span className="admin-muted-action">销售岗改动直接影响提成计提档，事由必填并留审计；员工状态变更仅限直属领导发起；人力会签按员工部门自动确定，直属起共三级。配置缺失时保持待处理，补齐后重新生成审批链。</span>
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
                    {row.userId ? <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                      data-testid={`staff-profile-handover-${row.id}`} disabled={!!submittingId || handoverLoading}
                      onClick={() => void openHandover(row)}>生效与业务交接</PermissionButton> : null}
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
      {handoverProfile ? <section aria-label="员工生效与业务交接" data-testid="staff-handover-panel">
        <h3>{handoverProfile.name ?? handoverProfile.userId}：生效与业务交接</h3>
        <button type="button" onClick={() => { setHandoverProfile(null); setHandover(null); }}>关闭交接</button>
        {handoverLoading ? <p>正在读取原业务待办…</p> : handover ? <>
          <p>{handover.businessAccessStopped ? "业务权限已停止，未结事项继续交接。" : "当前业务权限未因停离职停止；批准且到生效时间后才能办理停离职交接。"}</p>
          <table className="admin-table"><caption>状态变更审批与生效</caption><thead><tr><th>目标状态</th><th>审批状态</th><th>计划生效</th><th>落地时间</th></tr></thead><tbody>
            {handover.revisions.map((row) => <tr key={row.reviewId}><td>{EMPLOYMENT[row.afterStatus as keyof typeof EMPLOYMENT] ?? row.afterStatus}</td><td>{formatBusinessStatus(row.reviewStatus)}</td><td>{new Date(row.effectiveAt).toLocaleString()}</td><td>{row.appliedAt ? new Date(row.appliedAt).toLocaleString() : "尚未落地"}</td></tr>)}
          </tbody></table>
          <div className="admin-form-row">
            <label>接手员工账号<input aria-label="接手员工账号" value={handoverTarget} onChange={(event) => setHandoverTarget(event.target.value)} /></label>
            <label>交接原因<input aria-label="交接原因" value={handoverReason} onChange={(event) => setHandoverReason(event.target.value)} /></label>
          </div>
          <p>接手人须在岗并具备原业务权限及项目、商户范围。历史销售、审批和收入记录保留，卡座保留原负责时段。</p>
          <table className="admin-table"><caption>原业务待交接事项</caption><thead><tr><th>业务</th><th>编号</th><th>状态</th><th>项目／商户</th><th>操作</th></tr></thead><tbody>
            {handover.pending.length === 0 ? <tr><td colSpan={5}>暂无待交接事项</td></tr> : handover.pending.map((item) => <tr key={item.businessType + ":" + item.businessId}>
              <td>{item.businessName}</td><td>{item.businessId}</td><td>{formatBusinessStatus(item.status)}</td><td>{item.projectId ?? "全平台或待核实"}／{item.merchantId ?? "—"}</td>
              <td><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={!handover.businessAccessStopped || !item.assignable || !!submittingId}
                data-testid={`staff-handover-assign-${item.businessId}`} onClick={() => void assignHandover(item)}>确认交接</PermissionButton>{item.holdReason ? <span>{item.holdReason}</span> : null}</td>
            </tr>)}
          </tbody></table>
          <table className="admin-table"><caption>已完成交接记录</caption><thead><tr><th>业务编号</th><th>原责任人</th><th>接手人</th><th>时间</th><th>原因</th></tr></thead><tbody>
            {handover.history.map((item) => <tr key={item.id}><td>{item.businessId}</td><td>{item.previousUserId}</td><td>{item.targetUserId}</td><td>{new Date(item.assignedAt).toLocaleString()}</td><td>{item.reason}</td></tr>)}
          </tbody></table>
        </> : null}
      </section> : null}
      <h3>派驻记录（所属主体 → 派驻酒吧 · 起止时间）</h3>
      <p className="admin-muted-action">
        派驻期间在该酒吧促成订单的提成，由该酒吧按月拨付给人员所属主体，列入本人工资单由所属主体统一发放并代扣个人所得税（拨付走内部结算应付，按三个付账日结清）。
        仅平台侧获授权人员可登记及结束派驻，员工和目标酒吧均须在授权范围内。所属主体取自员工档案，项目取目标酒吧；结束派驻须填写上方改动事由。
      </p>
      <div className="admin-form-row" data-testid="staff-dispatch-editor">
        <label>员工账号<input aria-label="派驻员工账号" data-testid="staff-dispatch-user" value={dispatchUser} onChange={(event) => setDispatchUser(event.target.value)} /></label>
        <label>派驻酒吧（商户编号）<input aria-label="派驻酒吧" data-testid="staff-dispatch-merchant" value={dispatchMerchant} onChange={(event) => setDispatchMerchant(event.target.value)} /></label>
        <label>开始日期<input type="date" aria-label="派驻开始日期" data-testid="staff-dispatch-start" value={dispatchStart} onChange={(event) => setDispatchStart(event.target.value)} /></label>
        <label>备注<input aria-label="派驻备注" data-testid="staff-dispatch-note" value={dispatchNote} onChange={(event) => setDispatchNote(event.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="staff-dispatch-create"
          disabled={!canDispatch || submittingId === "dispatch-create"} onClick={() => void createDispatch()}>登记派驻</PermissionButton>
        <label>结束日期<input type="date" aria-label="派驻结束日期" data-testid="staff-dispatch-end-date" value={dispatchEndDate} onChange={(event) => setDispatchEndDate(event.target.value)} /></label>
      </div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="staff-dispatch-table">
          <thead>
            <tr><th>员工账号</th><th>所属主体</th><th>派驻酒吧</th><th>开始日期</th><th>结束日期</th><th>状态</th><th>备注</th><th>操作</th></tr>
          </thead>
          <tbody>
            {dispatches.length === 0 ? <tr><td colSpan={8}>暂无派驻记录</td></tr> : dispatches.map((row) => (
              <tr key={row.id} data-testid={`staff-dispatch-row-${row.id}`}>
                <td className="admin-id-cell" title={row.userId ?? ""}>{row.userId ?? "—"}</td>
                <td>{row.companyId ?? "—"}</td>
                <td>{row.merchantId ?? "—"}</td>
                <td>{row.startDate ?? "—"}</td>
                <td>{row.endDate ?? "—"}</td>
                <td data-testid={`staff-dispatch-status-${row.id}`}>{DISPATCH_STATUS[row.status ?? ""] ?? formatBusinessStatus(row.status)}</td>
                <td>{row.note ?? "—"}</td>
                <td>
                  {row.status === "active" ? <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                    data-testid={`staff-dispatch-end-${row.id}`} disabled={!canDispatch || submittingId === row.id}
                    onClick={() => void endDispatch(row)}>结束派驻</PermissionButton> : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
