// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { CnDatePicker } from "../components/CnDatePicker";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { cnDate } from "../components/RowDetailDrawer";
import { hasPermissions } from "../security/PermissionGate";

// 第十八批 Z 段第 188 条 188-R2（BSM:1803「封账由财务部门执行，条件是账目已算清楚、经确认无误、四流一致。」；PGM:664「跨主体与平台层面的财务工作——
// 三方对账、分账复核、封账、开票、付款执行、守恒校验不通过时的追查——由财务中心负责。」；第十八批裁决 41「财务中心按综合体＋月份在平台办理；只放行该月
// 每天五类数据都已最终、日结都已锁定的月份；四流一致由财务核对后勾选确认留痕；封账不能撤销，不另加拦截」）：后台「财务 → 对账差异」页的封账区块——
// 填综合体编号、选月份、勾选已核对四流一致，二次确认后封账；下列已封账月份。持 finance.period_close.execute 才显示；被拒时原样显示后端中文提示。
type PeriodClose = { id: string; projectId?: string; periodMonth?: string; closedBy?: string; closedAt?: string };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions">;

const IMPACT = "封账由财务部门执行，条件是账目已算清楚、经确认无误、四流一致。";
const monthLabel = (value?: string) => (value ?? "—").replace(/^(\d{4})-(\d{2})$/, (_, y: string, m: string) => `${y}年${Number(m)}月`);

export function FinancePeriodClosePanel(props: Props) {
  return hasPermissions(props.effectivePermissions, ["finance.period_close.execute"]) ? <PeriodClosePanel {...props} /> : null;
}

function PeriodClosePanel({ api }: Props) {
  const [projectId, setProjectId] = React.useState("");
  const [month, setMonth] = React.useState("");
  const [fourFlows, setFourFlows] = React.useState(false);
  const [rows, setRows] = React.useState<PeriodClose[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [refresh, setRefresh] = React.useState(0);

  React.useEffect(() => {
    let alive = true;
    api.get<PeriodClose[]>("/api/v1/finance/period-closes")
      .then((items) => { if (alive) setRows(Array.isArray(items) ? items : []); })
      .catch((e: unknown) => { if (alive) setError(e instanceof Error ? e.message : "封账记录读取失败"); });
    return () => { alive = false; };
  }, [api, refresh]);

  function ready() {
    if (projectId.trim() && month && fourFlows) return true;
    setError("请填写综合体编号、选择月份，并勾选已核对四流一致"); return false;
  }
  async function closePeriod() {
    setBusy(true); setError("");
    try {
      await api.post("/api/v1/finance/period-closes", { projectId: projectId.trim(), periodMonth: month, fourFlowsConfirmed: fourFlows });
      setFourFlows(false); setRefresh((n) => n + 1);
    } catch (e) { setError(e instanceof Error ? e.message : "封账失败"); }
    finally { setBusy(false); }
  }

  return <section data-testid="finance-period-close-panel">
    <div className="admin-section-title">封账</div>
    <p className="admin-muted-action">按综合体＋月份封账：只放行该月每天五类数据都已最终、日结都已锁定的月份；四流一致由财务核对后勾选确认；封账不能撤销。</p>
    <div className="admin-form-row">
      <label>综合体编号<input aria-label="综合体编号" data-testid="finance-period-close-project" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
      <label>月份<CnDatePicker mode="month" aria-label="封账月份" data-testid="finance-period-close-month" value={month} onChange={setMonth} /></label>
      <label><input type="checkbox" data-testid="finance-period-close-four-flows" checked={fourFlows} onChange={(e) => setFourFlows(e.target.checked)} />已核对四流一致</label>
      <HighRiskConfirm label="封账" impact={IMPACT} disabled={busy} precheck={ready} onConfirm={() => void closePeriod()} data-testid="finance-period-close-submit" />
    </div>
    {error ? <p role="alert" data-testid="finance-period-close-error">{error}</p> : null}
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="finance-period-close-table">
        <thead><tr><th>月份</th><th>综合体</th><th>封账人</th><th>封账时间</th></tr></thead>
        <tbody>{rows.map((row) => <tr key={row.id} data-testid={`finance-period-close-row-${row.id}`}>
          <td>{monthLabel(row.periodMonth)}</td><td>{row.projectId ?? "—"}</td><td>{row.closedBy ?? "—"}</td><td>{cnDate(row.closedAt)}</td>
        </tr>)}</tbody>
      </table>
    </div>
  </section>;
}
