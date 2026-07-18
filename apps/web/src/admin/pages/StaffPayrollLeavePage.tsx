import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { RowActions, type RowAction } from "../components/RowActions";

type PayrollRow = { id: string; userId?: string; month?: string; baseCent?: number; commissionCent?: number; bonusCent?: number; penaltyCent?: number; leaveDeductionCent?: number; taxCent?: number; totalCent?: number };
type LeaveRow = { id: string; userId?: string; leaveType?: string; startDate?: string; endDate?: string; reason?: string; status?: string; reviewNote?: string };

const TEXT = {
  title: "薪酬与请假",
  description: "薪资四块:固定工资(员工档案)+提成(业务自动归集)+奖金/处罚(手录必须带事由,审计留痕);人事「月结」后员工才可见,补录后可重结覆盖。请假由排班主管审批,通过区间考勤记「请假」;事假(无薪假)按日工资(月薪÷21.75)×天数自动扣薪,病假/年假/调休不扣。",
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

// 月份 "2026-07" → 中文 "2026年7月"（不把 ISO 原文直出）。
function cnMonth(value?: string) {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})/.exec(value);
  return m ? `${m[1]}年${Number(m[2])}月` : value;
}

export function StaffPayrollLeavePage({ api, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const [month, setMonth] = React.useState(currentMonth);
  const [payroll, setPayroll] = React.useState<PayrollRow[]>([]);
  const [leaves, setLeaves] = React.useState<LeaveRow[]>([]);
  const [baseUser, setBaseUser] = React.useState("");
  const [baseSalary, setBaseSalary] = React.useState("");
  // 引导式流程 phase-5：薪资基数录入收起为主按钮 + 分步引导弹窗（奖惩/月结/请假审批不动）。
  const [baseOpen, setBaseOpen] = React.useState(false);
  const [baseError, setBaseError] = React.useState("");
  const [baseSubmitting, setBaseSubmitting] = React.useState(false);
  const [adjUser, setAdjUser] = React.useState("");
  const [adjType, setAdjType] = React.useState("bonus");
  const [adjAmount, setAdjAmount] = React.useState("");
  const [adjReason, setAdjReason] = React.useState("");
  // 行明细抽屉：整行可点 → 展示该行完整字段（全部中文 label + ¥ 金额 + 中文日期/枚举）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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

  // 基数校验（步内 + 提交双保险）：员工 id 非空 + 月薪非负数。
  function baseInvalid() {
    const cent = Math.round(Number(baseSalary) * 100);
    return !baseUser.trim() || !Number.isFinite(cent) || cent < 0;
  }

  const saveBase = async () => {
    const cent = Math.round(Number(baseSalary) * 100);
    if (baseInvalid()) {
      setBaseError("请填 员工id 与 非负月薪(元)");
      return;
    }
    setBaseSubmitting(true);
    try {
      await api.post("/api/v1/staff/payroll/base", { userId: baseUser.trim(), monthlySalaryCent: cent });
      setActionResult({ action: "payroll_base_saved", userId: baseUser.trim() });
      setBaseUser(""); setBaseSalary("");
      setBaseError("");
      setBaseOpen(false);
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "薪资基数录入失败";
      setBaseError(message);
      setGlobalError(message);
    } finally {
      setBaseSubmitting(false);
    }
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

  // 薪资单行 → 该员工该月全部字段（label 用表头中文，金额带 ¥）。
  function openPayrollDetail(row: PayrollRow) {
    setDetail({
      title: `薪资单明细 · ${row.userId ?? "—"}`,
      fields: [
        { label: "员工", value: row.userId ?? "—" },
        { label: "所属月份", value: cnMonth(row.month ?? month) },
        { label: "固定工资", value: money(row.baseCent) },
        { label: "提成", value: money(row.commissionCent) },
        { label: "奖金", value: money(row.bonusCent) },
        { label: "处罚", value: money(row.penaltyCent) },
        { label: "请假扣薪", value: money(row.leaveDeductionCent) },
        { label: "代扣个税", value: money(row.taxCent) },
        { label: "实发合计", value: money(row.totalCent) },
        { label: "薪资单号", value: row.id },
      ],
    });
  }

  // 请假行 → 该申请完整字段（类型/状态枚举中文、起止日期中文）。
  function openLeaveDetail(row: LeaveRow) {
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
        { label: "申请单号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="staff-payroll-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="staff.payroll.month_closed / staff.payroll.adjustment_added / staff.leave.reviewed" />
      <div className="admin-toolbar">
        <label>月份:<CnDatePicker mode="month" data-testid="payroll-month" value={month} onChange={(v) => setMonth(v || currentMonth())} /></label>
        <button type="button" data-testid="payroll-close" onClick={() => void closeMonth()}>执行月结(重结=覆盖)</button>
      </div>
      <h3>固定工资设定(员工档案)</h3>
      <div className="admin-form-row" data-testid="payroll-base-editor">
        <button type="button" data-testid="payroll-base-create-open" onClick={() => { setBaseError(""); setBaseOpen(true); }}>＋ 薪资基数录入</button>
        <span className="admin-muted-action">固定工资录入后进入员工档案，作为月结固定工资项基数。</span>
      </div>
      <GuidedFlowModal
        open={baseOpen}
        title="薪资基数录入"
        testId="payroll-base-guided"
        submitTestId="base-save"
        submitLabel="保存"
        submitting={baseSubmitting}
        error={baseError || undefined}
        onClose={() => setBaseOpen(false)}
        onSubmit={() => void saveBase()}
        steps={[
          {
            title: "固定工资",
            description: "填写员工与月薪(元)。",
            validate: () => (baseInvalid() ? "请填 员工id 与 非负月薪(元)" : null),
            content: (
              <>
                <label>员工 user_id<input data-testid="base-user-input" value={baseUser} onChange={(event) => setBaseUser(event.target.value)} /></label>
                <label>月薪(元)<input data-testid="base-salary-input" value={baseSalary} onChange={(event) => setBaseSalary(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "员工 user_id", value: baseUser.trim() || "未填写" },
          { label: "月薪(元)", value: baseSalary.trim() || "未填写" },
        ]}
      />
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
            <thead><tr><th>员工</th><th>固定工资</th><th>提成</th><th>奖金</th><th>处罚</th><th>请假扣薪</th><th>代扣个税</th><th>实发合计</th><th>详情</th></tr></thead>
            <tbody>
              {payroll.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`payroll-row-${row.id}`}
                  onClick={() => openPayrollDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPayrollDetail(row); } }}>
                  <td>{row.userId ?? "—"}</td>
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
        <h3>待审请假(排班主管审批)</h3>
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
                  <td>{row.reason ?? "—"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = [
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
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
