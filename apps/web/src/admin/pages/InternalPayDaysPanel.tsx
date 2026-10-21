// 文件版本：V0.03
import React from "react";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

// FS-021-R3（BSM L1673「三个付账日为业务参数（全平台统一可配，初始为每月 5 日、15 日、25 日），各笔普通内部结算应付只在配置的付账日
// 中选定……系统在保存配置时校验：三个付账日中至少须有一个能满足该顺序」；BSM L1683「每一笔应付具体落在哪一个付账日，由财务人员按其
// 工作负荷统一制定规则，规则一经确定至少一年内不得变更」；BSM L1597 非积分类商户账单异议期按项目可配、须同时配置上下限）。
// 提交后须另一位有权限的同事审批（提交人不得自审）。
const MANAGE = ["settlement.config.manage"];
const BASE = "/api/v1/settlement/internal-pay-days";
const ROUTE_LABELS: Record<string, string> = {
  ticket_distribution: "通票分账应付",
  ticket_distribution_recovery: "通票分账追收",
  platform_incentive_subsidy: "平台激励补贴",
  single_bar_service_fee: "单店门票综合服务费",
  compensation_fulfillment: "补偿新增履约结算",
  compensation_fulfillment_recovery: "补偿应付已付追收",
  parking_credit_cost: "停车抵免成本",
};
const STATUS_LABELS: Record<string, string> = { pending: "待审批", approved: "已批准", rejected: "已驳回" };

type Rule = {
  id: string; kind: string; route?: string | null; firstPayDay?: number | null; secondPayDay?: number | null; thirdPayDay?: number | null;
  statementDay?: number | null; paySlot?: number | null; effectiveFrom?: string; status: string; reason?: string; submittedBy?: string;
};

function ruleText(rule: Rule): string {
  if (rule.kind === "pay_days") return `三个付账日：每月 ${rule.firstPayDay}/${rule.secondPayDay}/${rule.thirdPayDay} 日`;
  return `${ROUTE_LABELS[rule.route ?? ""] ?? rule.route}：次月 ${rule.statementDay} 日出账，指定第 ${rule.paySlot} 个付账日`;
}

export function InternalPayDaysPanel({ api, effectivePermissions }: Pick<AdminPageProps, "api" | "effectivePermissions">) {
  const [rules, setRules] = React.useState<Rule[]>([]);
  const [kind, setKind] = React.useState("route");
  const [route, setRoute] = React.useState("ticket_distribution");
  const [statementDay, setStatementDay] = React.useState("");
  const [slot, setSlot] = React.useState("1");
  const [effectiveFrom, setEffectiveFrom] = React.useState("");
  const [days, setDays] = React.useState(["", "", ""]);
  const [reason, setReason] = React.useState("");
  const [policy, setPolicy] = React.useState({ projectId: "", workingDays: "", minDays: "", maxDays: "" });
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");

  const reload = React.useCallback(async () => {
    const data = await api.get<Rule[]>(`${BASE}/rules`);
    setRules(Array.isArray(data) ? data : []);
  }, [api]);
  React.useEffect(() => { void reload().catch((e) => setError(e instanceof Error ? e.message : "付账日配置读取失败")); }, [reload]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError("");
    try {
      await action();
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "提交失败");
    } finally {
      setBusy(false);
    }
  }

  function submitRule() {
    if (!reason.trim()) { setError("原因必填。"); return; }
    const body = kind === "route"
      ? { kind, route, statementDay: Number(statementDay), paySlot: Number(slot), effectiveFrom, reason: reason.trim() }
      : { kind, firstPayDay: Number(days[0]), secondPayDay: Number(days[1]), thirdPayDay: Number(days[2]), reason: reason.trim() };
    void run(() => api.post(`${BASE}/rules`, body));
  }

  function savePolicy() {
    void run(() => api.post(`${BASE}/objection-period`, {
      projectId: policy.projectId.trim() || undefined, workingDays: Number(policy.workingDays),
      minDays: Number(policy.minDays), maxDays: Number(policy.maxDays),
    }));
  }

  return (
    <div data-testid="internal-pay-days-panel">
      <p className="admin-muted-action">
        普通内部结算应付按「月结定格 → 账单出账 → 异议期届满 → 付账日」顺序付款：财务按工作负荷为各钱路指定落在第几个付账日，规则一年内不得变更；
        指定日不满足顺序的顺延至本账期下一个满足的付账日，本账期均不满足的顺延至下一账期第一个满足的付账日。提交后须另一位同事审批。
      </p>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="internal-pay-day-rules">
          <thead><tr><th>配置</th><th>生效起始日</th><th>状态</th><th>原因</th><th>审批</th></tr></thead>
          <tbody>{rules.map((rule) => (
            <tr key={rule.id} data-testid={`internal-pay-day-rule-${rule.id}`}>
              <td>{ruleText(rule)}</td>
              <td>{rule.kind === "pay_days" && rule.status === "pending" ? "审批之日" : cnDate(rule.effectiveFrom)}</td>
              <td>{STATUS_LABELS[rule.status] ?? rule.status}</td>
              <td>{rule.reason ?? "—"}</td>
              <td>{rule.status === "pending" ? (<>
                <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`internal-pay-day-approve-${rule.id}`}
                  disabled={busy} onClick={() => void run(() => api.post(`${BASE}/rules/${encodeURIComponent(rule.id)}/decision`, { decision: "approve" }))}>批准</PermissionButton>
                <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`internal-pay-day-reject-${rule.id}`}
                  disabled={busy} onClick={() => void run(() => api.post(`${BASE}/rules/${encodeURIComponent(rule.id)}/decision`, { decision: "reject" }))}>驳回</PermissionButton>
              </>) : "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-row" data-testid="internal-pay-day-form">
        <label>类别<select data-testid="internal-pay-day-kind" value={kind} onChange={(e) => setKind(e.target.value)}>
          <option value="route">钱路指派规则</option>
          <option value="pay_days">三个付账日</option>
        </select></label>
        {kind === "route" ? (<>
          <label>钱路<select data-testid="internal-pay-day-route" value={route} onChange={(e) => setRoute(e.target.value)}>
            {Object.entries(ROUTE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label>次月出账日<input data-testid="internal-pay-day-statement-day" type="number" min={1} max={28} value={statementDay} onChange={(e) => setStatementDay(e.target.value)} /></label>
          <label>指定付账日<select data-testid="internal-pay-day-slot" value={slot} onChange={(e) => setSlot(e.target.value)}>
            <option value="1">第 1 个</option><option value="2">第 2 个</option><option value="3">第 3 个</option>
          </select></label>
          <label>生效起始日<input data-testid="internal-pay-day-effective-from" type="date" value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} /></label>
        </>) : days.map((day, index) => (
          <label key={index}>第 {index + 1} 个付账日<input data-testid={`internal-pay-day-day-${index + 1}`} type="number" min={1} max={28} value={day}
            onChange={(e) => setDays((prev) => prev.map((v, i) => (i === index ? e.target.value : v)))} /></label>
        ))}
        <label>原因<input data-testid="internal-pay-day-reason" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="internal-pay-day-submit" disabled={busy} onClick={submitRule}>提交审批</PermissionButton>
      </div>
      <h4>非积分商户账单异议期（按项目）</h4>
      <div className="admin-form-row" data-testid="internal-pay-day-objection-form">
        <label>项目<input data-testid="internal-pay-day-objection-project" value={policy.projectId} onChange={(e) => setPolicy({ ...policy, projectId: e.target.value })} placeholder="项目账号可留空＝本项目" /></label>
        <label>工作日数<input data-testid="internal-pay-day-objection-days" type="number" min={0} value={policy.workingDays} onChange={(e) => setPolicy({ ...policy, workingDays: e.target.value })} /></label>
        <label>下限<input data-testid="internal-pay-day-objection-min" type="number" min={0} value={policy.minDays} onChange={(e) => setPolicy({ ...policy, minDays: e.target.value })} /></label>
        <label>上限<input data-testid="internal-pay-day-objection-max" type="number" min={0} value={policy.maxDays} onChange={(e) => setPolicy({ ...policy, maxDays: e.target.value })} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="internal-pay-day-objection-save" disabled={busy} onClick={savePolicy}>保存异议期</PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="internal-pay-day-error">{error}</div> : null}
    </div>
  );
}
