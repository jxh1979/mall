// 文件版本：V0.09
import React from "react";
import { payrollDetailFields } from "./StaffPayrollDetail";
import type { PayrollStatementDetail } from "./staffPayrollTypes";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { StaffPayrollBasisPanel } from "./StaffPayrollBasisPanel";
import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps } from "../types";
import { RowActions, type RowAction } from "../components/RowActions";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

type PayrollRow = { id: string; userId?: string; month?: string; baseCent?: number; commissionCent?: number; bonusCent?: number; penaltyCent?: number; leaveDeductionCent?: number; taxCent?: number; totalCent?: number; payerCompanyId?: string | null; taxAssessmentId?: string | null; taxVersionId?: string | null };
// CG-07 提成月结拨付（bar_commission_settlements）：酒吧 → 人员所属主体 → 月，一组一条内部结算应付。
type CommissionSettlementRow = { id: string; month?: string; merchantId?: string | null; companyId?: string | null; amountCent?: number; calculationCount?: number; staffCount?: number; arApItemId?: string | null; dueDate?: string | null };
const cnDay = (value?: string | null) => (value ? value.replace(/^(\d{4})-(\d{2})-(\d{2})$/, "$1年$2月$3日") : "—");
// 2026-10-18 操作员裁决：销假=本人申请（revoke_requested，revokeRequestReason 为员工申请理由）＋主管批准（revoked）。
type LeaveRow = { id: string; userId?: string; leaveType?: string; startDate?: string; endDate?: string; reason?: string; status?: string; reviewNote?: string; revokeRequestReason?: string };
// CG-12：员工对已生效扣罚的申诉（同链裁定：撤销＝冲回原调整、维持）。
type AppealRow = { id: string; adjustmentId?: string; userId?: string; month?: string; amountCent?: number; reason?: string; reviewId?: string; status?: string; reversalAdjustmentId?: string | null; createdAt?: string; decidedAt?: string | null };
const APPEAL_STATUS: Record<string, string> = { pending: "裁定中", upheld: "维持", revoked: "已撤销" };

const TEXT = {
  title: "薪酬与请假",
  description: "依据已批准薪资方案、计薪日历和期间事实逐项核算。普通现金处罚仅按基础工资上限处理，绩效少发不占额度；同一事假影响只扣一次。工资定格后员工可查，银行实际支付另行记录。已发工资撤罚保留原单，真实少发差额并入下一次应发工资；离职依法结清。",
  loadFailed: "薪酬请假读取失败",
};

const LEAVE_LABELS: Record<string, string> = { personal: "事假", sick: "病假", comp: "调休", annual: "年假" };
const STATUS_LABELS: Record<string, string> = { pending: "待审", approved: "已通过", rejected: "已驳回", revoke_requested: "待销假审批", revoked: "已销假" };

function money(cent?: number) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

// 月份 "2026-07" → 中文 "2026年7月"（不把 ISO 原文直出）。
function cnMonth(value?: string) {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})/.exec(value);
  return m ? `${m[1]}年${Number(m[2])}月` : value;
}

export function StaffPayrollLeavePage({ api, user, setGlobalError, setActionResult, effectivePermissions, navigateTo }: AdminPageProps) {
  const [cap, setCap] = React.useState<{ value: string; minValue: string; maxValue: string } | null>(null);
  const [capReason, setCapReason] = React.useState("");
  const [capReviewers, setCapReviewers] = React.useState({ hr_review: "", finance_review: "", legal_review: "" });
  const [capMessage, setCapMessage] = React.useState("");
  const [capSaving, setCapSaving] = React.useState(false);
  const [month, setMonth] = React.useState(currentMonth);
  const [payroll, setPayroll] = React.useState<PayrollRow[]>([]);
  const [leaves, setLeaves] = React.useState<LeaveRow[]>([]);
  const [settlements, setSettlements] = React.useState<CommissionSettlementRow[]>([]);
  const [leaveNotice, setLeaveNotice] = React.useState<string | null>(null);
  const [adjUser, setAdjUser] = React.useState("");
  const [adjType, setAdjType] = React.useState("bonus");
  const [adjAmount, setAdjAmount] = React.useState("");
  const [adjReason, setAdjReason] = React.useState("");
  const [appealAdjId, setAppealAdjId] = React.useState("");
  const [appealReason, setAppealReason] = React.useState("");
  const [appealUser, setAppealUser] = React.useState("");
  const [appeals, setAppeals] = React.useState<AppealRow[]>([]);
  const [appealNotice, setAppealNotice] = React.useState("");
  // 行明细抽屉：整行可点 → 展示该行完整字段（全部中文 label + ¥ 金额 + 中文日期/枚举）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  // 无权限副资源不请求（根因修）：待审请假端点需 staff.shift.manage，页面基础权限 staff.payroll.manage 不含之。
  const canShiftManage = hasPermissions(effectivePermissions, ["staff.shift.manage"]);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：待审请假归排班主管审批，薪资账号可能无权（403）——降级为区块提示，
      // 不拖垮当月薪资明细主内容。
      const [payRows, leaveRes, settlementRows] = await Promise.all([
        api.get<PayrollRow[]>(`/api/v1/staff/payroll?month=${month}`),
        // 待审请假 + 待销假审批（员工本人已申请销假、等主管批准/驳回）一并列出。
        loadSecondaryIfPermitted(canShiftManage, async () => [
          ...(await api.get<LeaveRow[]>("/api/v1/staff/leaves?status=pending")),
          ...(await api.get<LeaveRow[]>("/api/v1/staff/leaves?status=revoke_requested")),
        ], [] as LeaveRow[]),
        api.get<CommissionSettlementRow[]>(`/api/v1/staff/commission-settlements?month=${month}`),
      ]);
      setPayroll(payRows ?? []);
      setSettlements(Array.isArray(settlementRows) ? settlementRows : []);
      setLeaves(leaveRes.data);
      setLeaveNotice(leaveRes.notice);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const isFirstMonthChange = React.useRef(true);
  React.useEffect(() => {
    if (isFirstMonthChange.current) {
      isFirstMonthChange.current = false;
      return;
    }
    void reload();
  }, [month, reload]);

  const saveAdjustment = async () => {
    const cent = Math.round(Number(adjAmount) * 100);
    if (!adjUser.trim() || !Number.isFinite(cent) || cent <= 0 || !adjReason.trim()) {
      setGlobalError("奖惩必须填 员工id/正金额(元)/事由(留痕硬要求)");
      return;
    }
    const review = await api.post<{ reviewId?: string }>("/api/v1/staff/payroll/adjustments", {
      userId: adjUser.trim(), month, adjType, amountCent: cent, reason: adjReason.trim(),
    });
    setActionResult({ action: "payroll_adjustment_requested", adjType, reviewId: review?.reviewId, note: "已发起复核，人力会签与直属起共三个组织层级全部通过后入账" });
    setAdjUser(""); setAdjAmount(""); setAdjReason("");
  };

  // CG-12：申诉——只能由被扣罚员工本人提出；清单按员工查看（人事）或看本人。
  const loadAppeals = async () => {
    setAppealNotice("");
    try {
      const rows = await api.get<AppealRow[]>("/api/v1/staff/payroll/adjustments/appeals" + (appealUser.trim() ? `?user_id=${encodeURIComponent(appealUser.trim())}` : ""));
      setAppeals(rows ?? []);
      if (!rows || rows.length === 0) setAppealNotice("暂无申诉记录");
    } catch (error) {
      setAppealNotice(error instanceof Error ? error.message : "申诉清单读取失败");
    }
  };

  const submitAppeal = async () => {
    if (!appealAdjId.trim() || !appealReason.trim()) {
      setGlobalError("申诉必须填 扣罚记录编号/申诉事由");
      return;
    }
    const appeal = await api.post<{ id?: string; reviewId?: string }>(`/api/v1/staff/payroll/adjustments/${encodeURIComponent(appealAdjId.trim())}/appeals`, {
      reason: appealReason.trim(),
    });
    setActionResult({ action: "payroll_adjustment_appeal_submitted", appealId: appeal?.id, reviewId: appeal?.reviewId, note: "申诉已报至人力部门与直属起共三个组织层级裁定" });
    setAppealAdjId(""); setAppealReason("");
    await loadAppeals();
  };

  // CG-07：按（酒吧 → 人员所属主体 → 月）汇总派驻人员提成，生成内部结算应付；重复执行不重复挂账。
  // 第十八批 V 段 159-R8：派驻酒吧当月营业日数据未标最终时后端 409 拒绝，报错经全局提示显示后端中文原文。
  const closeCommissionSettlement = async () => {
    try {
      const result = await api.post<{ settlements?: number }>("/api/v1/staff/commission-settlements/close", { month });
      setActionResult({ action: "commission_settlement_closed", month, settlements: result?.settlements });
      await reload();
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "提成月结拨付失败");
    }
  };

  const review = async (id: string, decision: "approve" | "reject") => {
    await api.post(`/api/v1/staff/leaves/${id}/review`, { decision, reviewNote: decision === "approve" ? "准" : "驳回" });
    setActionResult({ action: "leave_reviewed", id, decision });
    await reload();
  };

  // 2026-10-18 操作员裁决：销假=本人申请＋主管批准。主管在此批准（→ 已销假，BSM L1481 冲正/不扣薪）或驳回（假仍有效）。
  const reviewRevoke = async (id: string, decision: "approve" | "reject") => {
    await api.post(`/api/v1/staff/leaves/${id}/revoke`, { decision, reviewNote: decision === "approve" ? "准予销假" : "不予销假" });
    setActionResult({ action: "leave_revoke_reviewed", id, decision });
    await reload();
  };

  const detailRequest = React.useRef(0);
  React.useEffect(() => { detailRequest.current++; setDetail(null); }, [month]);
  async function openPayrollDetail(row: PayrollRow) {
    const request = ++detailRequest.current;
    setDetail(null); setGlobalError(null);
    try {
      const result = await api.get<PayrollStatementDetail>(`/api/v1/staff/payroll/statements/${encodeURIComponent(row.id)}`);
      if (request === detailRequest.current) setDetail({ title: `薪资单明细 · ${result.statement.userId ?? "—"}`, fields: payrollDetailFields(result) });
    } catch (error) {
      if (request === detailRequest.current) setGlobalError(error instanceof Error ? error.message : "工资单详情读取失败");
    }
  }

  // 请假行 → 该申请完整字段（类型/状态枚举中文、起止日期中文）。
  function openLeaveDetail(row: LeaveRow) {
    detailRequest.current += 1;
    setDetail({
      title: `请假申请明细 · ${row.userId ?? "—"}`,
      fields: [
        { label: "员工", value: row.userId ?? "—" },
        { label: "请假类型", value: LEAVE_LABELS[row.leaveType ?? ""] ?? row.leaveType ?? "—" },
        { label: "开始日期", value: cnDate(row.startDate) },
        { label: "结束日期", value: cnDate(row.endDate) },
        { label: "事由", value: row.reason ?? "—" },
        { label: "审批状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
        { label: "审批意见", value: row.reviewNote ?? "—" },
        { label: "销假申请理由", value: row.revokeRequestReason ?? "—" },
        { label: "申请单号", value: row.id },
      ],
    });
  }

  // 第五批（人事域）KPI 接线：应发净额合计/薪资单数按所选月（from=to=月首日）、待审请假为当前 pending 计数；平台口径同源。
  const monthDay = `${month}-01`;
  const payrollTotalKpi = useMetricKpiValue(api, "staff.payroll_net_total", { from: monthDay, to: monthDay });
  const payrollCountKpi = useMetricKpiValue(api, "staff.payroll_statement_count", { from: monthDay, to: monthDay });
  const pendingLeaveKpi = useMetricKpiValue(api, "staff.pending_leave_count", {});

  return (
    <section data-testid="staff-payroll-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      {hasPermissions(effectivePermissions, ["staff.payroll.manage"]) ? <>
        <button type="button" onClick={async () => {
          try {
            const current = await api.get<{ value: number; minValue: number; maxValue: number }>("/api/v1/staff/settings/payroll-penalty-cap");
            setCap({ value: String(current.value / 100), minValue: String(current.minValue / 100), maxValue: String(current.maxValue / 100) });
            setCapMessage("");
          } catch (error) { setCapMessage((error as Error).message); }
        }}>普通扣除上限</button>
        {cap ? <fieldset><legend>全平台普通扣除上限（百分比）</legend>
          <p>现金处罚仅按基础工资计算上限，绩效减少不占额度。上限修改须经人力、财务、法务审核，再由顶层平台最终确认；已月结单据沿用原上限。</p>
          {([['value', '上限比例（%）'], ['minValue', '允许下限（%）'], ['maxValue', '允许上限（%）']] as const).map(([key, label]) => <label key={key}>{label}
            <input aria-label={label} type="number" min={0} max={100} step="0.01" value={cap[key]} onChange={(event) => setCap({ ...cap, [key]: event.target.value })} />
          </label>)}
          {([['hr_review', '人力最终审批人'], ['finance_review', '财务最终审批人'], ['legal_review', '法务最终审批人']] as const).map(([key,label]) => <label key={key}>{label}
            <input aria-label={label} value={capReviewers[key]} onChange={(event) => setCapReviewers({ ...capReviewers, [key]: event.target.value })} placeholder="员工账号编号" />
          </label>)}
          <label>调整事由<input aria-label="上限调整事由" value={capReason} onChange={(event) => setCapReason(event.target.value)} /></label>
          <button type="button" disabled={capSaving} onClick={async () => {
            if ([...Object.values(cap), ...Object.values(capReviewers), capReason].some((value) => !value.trim())) { setCapMessage("请填写比例、上下限、三条线的最终审批人及事由。"); return; }
            setCapSaving(true);
            try {
              const review = await api.post<{ reviewId: string }>("/api/v1/staff/settings/payroll-penalty-cap", {
                value: Math.round(Number(cap.value) * 100), minValue: Math.round(Number(cap.minValue) * 100), maxValue: Math.round(Number(cap.maxValue) * 100),
                reason: capReason.trim(), parallelLines: Object.entries(capReviewers).map(([lineCode, finalApproverId]) => ({ lineCode, finalApproverId: finalApproverId.trim() })),
              });
              setCapMessage("申请已提交，审批完成前现行上限不变。审批单：" + review.reviewId);
            } catch (error) { setCapMessage((error as Error).message); }
            finally { setCapSaving(false); }
          }}>提交上限调整审批</button>
        </fieldset> : null}
      </> : null}
      {capMessage ? <p role="status">{capMessage}</p> : null}
      <AuditTrailHint event="staff.payroll.settle / staff.payroll.adjustment_added / staff.leave.reviewed" />
      <div className="admin-toolbar">
        <label>月份:<CnDatePicker mode="month" data-testid="payroll-month" value={month} onChange={(v) => setMonth(v || currentMonth())} /></label>
        <a href="#payroll-approved-basis">按批准依据核算工资</a>
        <button type="button" data-testid="payroll-commission-close" onClick={() => void closeCommissionSettlement()}>提成月结拨付(酒吧→所属主体)</button>
      </div>
      <p>工资须逐员工依据已批准期间事实生成税前原件，完成专业税额审批后定格；未结周期和未分配项在工资详情持续跟进。</p>
      <div className="admin-kpi-grid" data-testid="staff-payroll-summary">
        <div className="admin-kpi-card"><span>应发净额合计（元）</span><strong className="admin-kpi-value--text" data-testid="staff-payroll-kpi-total">{payrollTotalKpi.status === "ready" && payrollTotalKpi.value !== null ? money(payrollTotalKpi.value) : "—"}</strong><small>平台口径·{cnMonth(month)}{payrollTotalKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.payroll_net_total" from={monthDay} to={monthDay} testid="staff-payroll-metric-detail-total" /> : null}</small></div>
        <div className="admin-kpi-card"><span>薪资单数</span><strong className="admin-kpi-value--text" data-testid="staff-payroll-kpi-count">{payrollCountKpi.status === "ready" && payrollCountKpi.value !== null ? payrollCountKpi.value : "—"}</strong><small>平台口径·{cnMonth(month)}{payrollCountKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.payroll_statement_count" from={monthDay} to={monthDay} testid="staff-payroll-metric-detail-count" /> : null}</small></div>
        <div className="admin-kpi-card"><span>待审请假数</span><strong className="admin-kpi-value--text" data-testid="staff-leave-kpi-pending">{pendingLeaveKpi.status === "ready" && pendingLeaveKpi.value !== null ? pendingLeaveKpi.value : "—"}</strong><small>平台口径·当前{pendingLeaveKpi.status === "ready" ? <MetricDetailLink navigateTo={navigateTo} metric="staff.pending_leave_count" testid="staff-leave-metric-detail-pending" /> : null}</small></div>
      </div>
      <div id="payroll-approved-basis"><StaffPayrollBasisPanel api={api} user={user} effectivePermissions={effectivePermissions} setActionResult={setActionResult} setGlobalError={setGlobalError} month={month} onSettled={result => { detailRequest.current++; setDetail({ title: `薪资单明细 · ${result.statement.userId ?? "—"}`, fields: payrollDetailFields(result) }); void reload(); }} /></div>
      <h3>奖金 / 处罚手录(必须带事由；人力会签＋直属起共三个组织层级复核通过后入账)</h3>
      <div className="admin-form-grid" data-testid="payroll-adj-editor">
        <label>员工 user_id<input data-testid="adj-user-input" value={adjUser} onChange={(event) => setAdjUser(event.target.value)} /></label>
        <label>类型<select data-testid="adj-type-select" value={adjType} onChange={(event) => setAdjType(event.target.value)}><option value="bonus">激励奖金</option><option value="penalty">处罚扣款</option></select></label>
        <label>金额(元)<input data-testid="adj-amount-input" value={adjAmount} onChange={(event) => setAdjAmount(event.target.value)} /></label>
        <label>事由<input data-testid="adj-reason-input" value={adjReason} onChange={(event) => setAdjReason(event.target.value)} /></label>
        <p>人力会签按员工部门自动确定，直属起共三级；配置缺失时保持待处理，补齐后重新生成审批链。</p>
        <button type="button" data-testid="adj-save" onClick={() => void saveAdjustment()}>发起复核</button>
      </div>
      <h3>扣罚申诉(员工本人对已生效扣罚提出；同链裁定，撤销保留原件，已发工资另列实际补差)</h3>
      <div className="admin-form-grid" data-testid="payroll-appeal-editor">
        <label>扣罚记录编号<input data-testid="appeal-adj-input" value={appealAdjId} onChange={(event) => setAppealAdjId(event.target.value)} /></label>
        <label>申诉事由<input data-testid="appeal-reason-input" value={appealReason} onChange={(event) => setAppealReason(event.target.value)} /></label>
        <p>申诉沿员工所属部门自动确定人力会签人，不由申诉人指定。</p>
        <button type="button" data-testid="appeal-submit" onClick={() => void submitAppeal()}>提出申诉</button>
        <label>按员工查看申诉<input data-testid="appeal-user-input" placeholder="留空＝本人" value={appealUser} onChange={(event) => setAppealUser(event.target.value)} /></label>
        <button type="button" data-testid="appeal-load" onClick={() => void loadAppeals()}>查看申诉清单</button>
      </div>
      {appealNotice ? <div className="admin-state" data-testid="appeal-notice">{appealNotice}</div> : null}
      {appeals.length > 0 ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="appeal-table">
            <thead><tr><th>申诉编号</th><th>扣罚记录</th><th>员工</th><th>月份</th><th>金额</th><th>申诉事由</th><th>结论</th><th>冲回记录</th><th>复核单</th><th>提交时间</th><th>裁定时间</th></tr></thead>
            <tbody>
              {appeals.map((row) => (
                <tr key={row.id} data-testid={`appeal-row-${row.id}`}>
                  <td>{row.id}</td><td>{row.adjustmentId ?? "—"}</td><td>{row.userId ?? "—"}</td><td>{cnMonth(row.month)}</td><td>{money(row.amountCent)}</td>
                  <td>{row.reason ?? "—"}</td><td data-testid={`appeal-status-${row.id}`}>{APPEAL_STATUS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td>{row.reversalAdjustmentId ?? "—"}</td><td>{row.reviewId ?? "—"}</td><td>{cnDate(row.createdAt)}</td><td>{row.decidedAt ? cnDate(row.decidedAt) : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>当月薪资单（专业税额批准并定格后生成；员工端仅见已定格单据）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="payroll-table">
            <thead><tr><th>员工</th><th>发放主体</th><th>固定工资</th><th>提成</th><th>奖金</th><th>处罚</th><th>请假扣薪</th><th>代扣个税</th><th>应发净额</th><th>详情</th></tr></thead>
            <tbody>
              {payroll.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`payroll-row-${row.id}`}
                  onClick={() => openPayrollDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPayrollDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
                  <td data-testid={`payroll-payer-${row.id}`}>{row.payerCompanyId ?? "—"}</td>
                  <td>{money(row.baseCent)}</td>
                  <td>{money(row.commissionCent)}</td>
                  <td>{money(row.bonusCent)}</td>
                  <td>{money(row.penaltyCent)}</td>
                  <td>{money(row.leaveDeductionCent)}</td>
                  <td>{money(row.taxCent)}</td>
                  <td>{money(row.totalCent)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看薪资单详情" onClick={(event) => { event.stopPropagation(); openPayrollDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3>提成月结拨付(酒吧 → 人员所属主体，按三个付账日结清)</h3>
        <p className="admin-muted-action">派驻/营销人员在酒吧促成订单的提成，由该酒吧按月拨付给人员所属主体，列入本人工资单由所属主体统一发放并代扣个人所得税；拨付为内部结算应付，总额收付、不轧差。</p>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="commission-settlement-table">
            <thead><tr><th>酒吧</th><th>所属主体</th><th>拨付金额</th><th>计提笔数</th><th>人数</th><th>到期付账日</th><th>应付台账项</th></tr></thead>
            <tbody>
              {settlements.length === 0 ? <tr><td colSpan={7}>本月尚未执行提成月结拨付</td></tr> : settlements.map((row) => (
                <tr key={row.id} data-testid={`commission-settlement-row-${row.id}`}>
                  <td>{row.merchantId ?? "—"}</td>
                  <td>{row.companyId ?? "—"}</td>
                  <td>{money(row.amountCent)}</td>
                  <td>{row.calculationCount ?? 0}</td>
                  <td>{row.staffCount ?? 0}</td>
                  <td>{cnDay(row.dueDate)}</td>
                  <td className="admin-id-cell" title={row.arApItemId ?? ""}>{row.arApItemId ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3>待审请假与销假申请(排班主管审批)</h3>
        {leaveNotice ? (
          <div className="admin-state" data-testid="leave-degraded">{leaveNotice}</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leave-table">
            <thead><tr><th>员工</th><th>类型</th><th>起止</th><th>事由</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>
              {leaves.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leave-row-${row.id}`}
                  onClick={() => openLeaveDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openLeaveDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{LEAVE_LABELS[row.leaveType ?? ""] ?? row.leaveType ?? "—"}</td>
                  <td>{cnDate(row.startDate)} ~ {cnDate(row.endDate)}</td>
                  <td>{row.reason ?? "—"}{row.revokeRequestReason ? `；销假理由：${row.revokeRequestReason}` : ""}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = row.status === "revoke_requested" ? [
                        { key: "revoke-approve", label: "批准销假", onClick: () => void reviewRevoke(row.id, "approve"), testId: `leave-revoke-approve-${row.id}` },
                        { key: "revoke-reject", label: "驳回销假", danger: true, onClick: () => void reviewRevoke(row.id, "reject"), testId: `leave-revoke-reject-${row.id}` },
                      ] : [
                        { key: "approve", label: "通过", onClick: () => void review(row.id, "approve"), testId: `leave-approve-${row.id}` },
                        { key: "reject", label: "驳回", danger: true, onClick: () => void review(row.id, "reject"), testId: `leave-reject-${row.id}` },
                      ];
                      return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`leave-actions-${row.id}`} />;
                    })()}
                  </td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看请假申请详情" onClick={(event) => { event.stopPropagation(); openLeaveDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => { detailRequest.current += 1; setDetail(null); }} />
    </section>
  );
}
