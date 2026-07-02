import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";

type PayrollRow = { id: string; userId?: string; month?: string; baseCent?: number; commissionCent?: number; bonusCent?: number; penaltyCent?: number; totalCent?: number };
type LeaveRow = { id: string; userId?: string; leaveType?: string; startDate?: string; endDate?: string; reason?: string; status?: string; reviewNote?: string };

const TEXT = {
  title: "薪酬与请假",
  description: "薪资四块:固定工资(员工档案)+提成(业务自动归集)+奖金/处罚(手录必须带事由,审计留痕);人事「月结」后员工才可见,补录后可重结覆盖。请假由排班主管审批,通过区间考勤记「请假」。",
  loadFailed: "薪酬请假读取失败",
};

const LEAVE_LABELS: Record<string, string> = { personal: "事假", sick: "病假", comp: "调休", annual: "年假" };
const STATUS_LABELS: Record<string, string> = { pending: "待审", approved: "已通过", rejected: "已驳回" };

function money(cent?: number) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

export function StaffPayrollLeavePage({ api, setGlobalError, setActionResult }: AdminPageProps) {
  const [month, setMonth] = React.useState(currentMonth);
  const [payroll, setPayroll] = React.useState<PayrollRow[]>([]);
  const [leaves, setLeaves] = React.useState<LeaveRow[]>([]);
  const [baseUser, setBaseUser] = React.useState("");
  const [baseSalary, setBaseSalary] = React.useState("");
  const [adjUser, setAdjUser] = React.useState("");
  const [adjType, setAdjType] = React.useState("bonus");
  const [adjAmount, setAdjAmount] = React.useState("");
  const [adjReason, setAdjReason] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [payRows, leaveRows] = await Promise.all([
        api.get<PayrollRow[]>(`/api/v1/staff/payroll?month=${month}`),
        api.get<LeaveRow[]>("/api/v1/staff/leaves?status=pending"),
      ]);
      setPayroll(payRows ?? []);
      setLeaves(leaveRows ?? []);
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

  const saveBase = async () => {
    const cent = Math.round(Number(baseSalary) * 100);
    if (!baseUser.trim() || !Number.isFinite(cent) || cent < 0) {
      setGlobalError("请填 员工id 与 非负月薪(元)");
      return;
    }
    await api.post("/api/v1/staff/payroll/base", { userId: baseUser.trim(), monthlySalaryCent: cent });
    setActionResult({ action: "payroll_base_saved", userId: baseUser.trim() });
    setBaseUser(""); setBaseSalary("");
  };

  const saveAdjustment = async () => {
    const cent = Math.round(Number(adjAmount) * 100);
    if (!adjUser.trim() || !Number.isFinite(cent) || cent <= 0 || !adjReason.trim()) {
      setGlobalError("奖惩必须填 员工id/正金额(元)/事由(留痕硬要求)");
      return;
    }
    await api.post("/api/v1/staff/payroll/adjustments", {
      userId: adjUser.trim(), month, adjType, amountCent: cent, reason: adjReason.trim(),
    });
    setActionResult({ action: "payroll_adjustment_added", adjType });
    setAdjUser(""); setAdjAmount(""); setAdjReason("");
  };

  const closeMonth = async () => {
    const result = await api.post<{ statements?: number }>("/api/v1/staff/payroll/close", { month });
    setActionResult({ action: "payroll_month_closed", month, statements: result?.statements });
    await reload();
  };

  const review = async (id: string, decision: "approve" | "reject") => {
    await api.post(`/api/v1/staff/leaves/${id}/review`, { decision, reviewNote: decision === "approve" ? "准" : "驳回" });
    setActionResult({ action: "leave_reviewed", id, decision });
    await reload();
  };

  return (
    <section data-testid="staff-payroll-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="staff.payroll.month_closed / staff.payroll.adjustment_added / staff.leave.reviewed" />
      <div className="admin-toolbar">
        <label>月份:<input data-testid="payroll-month" type="month" value={month} onChange={(event) => setMonth(event.target.value || currentMonth())} /></label>
        <button type="button" data-testid="payroll-close" onClick={() => void closeMonth()}>执行月结(重结=覆盖)</button>
      </div>
      <h3>固定工资设定(员工档案)</h3>
      <div className="admin-form-grid" data-testid="payroll-base-editor">
        <label>员工 user_id<input data-testid="base-user-input" value={baseUser} onChange={(event) => setBaseUser(event.target.value)} /></label>
        <label>月薪(元)<input data-testid="base-salary-input" value={baseSalary} onChange={(event) => setBaseSalary(event.target.value)} /></label>
        <button type="button" data-testid="base-save" onClick={() => void saveBase()}>保存</button>
      </div>
      <h3>奖金 / 处罚手录(必须带事由)</h3>
      <div className="admin-form-grid" data-testid="payroll-adj-editor">
        <label>员工 user_id<input data-testid="adj-user-input" value={adjUser} onChange={(event) => setAdjUser(event.target.value)} /></label>
        <label>类型<select data-testid="adj-type-select" value={adjType} onChange={(event) => setAdjType(event.target.value)}><option value="bonus">激励奖金</option><option value="penalty">处罚扣款</option></select></label>
        <label>金额(元)<input data-testid="adj-amount-input" value={adjAmount} onChange={(event) => setAdjAmount(event.target.value)} /></label>
        <label>事由<input data-testid="adj-reason-input" value={adjReason} onChange={(event) => setAdjReason(event.target.value)} /></label>
        <button type="button" data-testid="adj-save" onClick={() => void saveAdjustment()}>录入</button>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>当月薪资单(月结后生成;员工端仅见已定格月份)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="payroll-table">
            <thead><tr><th>员工</th><th>固定工资</th><th>提成</th><th>奖金</th><th>处罚</th><th>实发合计</th></tr></thead>
            <tbody>
              {payroll.map((row) => (
                <tr key={row.id}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{money(row.baseCent)}</td>
                  <td>{money(row.commissionCent)}</td>
                  <td>{money(row.bonusCent)}</td>
                  <td>{money(row.penaltyCent)}</td>
                  <td>{money(row.totalCent)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <h3>待审请假(排班主管审批)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leave-table">
            <thead><tr><th>员工</th><th>类型</th><th>起止</th><th>事由</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {leaves.map((row) => (
                <tr key={row.id}>
                  <td>{row.userId ?? "—"}</td>
                  <td>{LEAVE_LABELS[row.leaveType ?? ""] ?? row.leaveType ?? "—"}</td>
                  <td>{row.startDate} ~ {row.endDate}</td>
                  <td>{row.reason ?? "—"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td>
                    <button type="button" data-testid={`leave-approve-${row.id}`} onClick={() => void review(row.id, "approve")}>通过</button>
                    <button type="button" data-testid={`leave-reject-${row.id}`} onClick={() => void review(row.id, "reject")}>驳回</button>
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
