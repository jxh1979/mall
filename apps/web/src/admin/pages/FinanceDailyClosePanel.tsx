// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { BarVenueSelect } from "../components/BarVenueSelect";
import { CnDatePicker } from "../components/CnDatePicker";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { cnDate } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { hasPermissions } from "../security/PermissionGate";

// 第十八批 P 段第 133 条 133-R3（BSM:1668「……最后形成日结和结算依据。」；ENUM:188 对账口径含「按日结」；BSM:1682「按营业日生成日结批次。」；
// BSM:1848「财务看退款、差异、日结和结算依据。」；BSM:2023「已拍板：日结锁定后不能直接补录影响金额，必须上级审批通过后补录。」）：
// 后台「财务 → 对账差异」页的日结区块——按营业日倒序查日结批次与七源汇总，按营业日与范围锁定日结。
// 商户侧账号的范围对象就是本账号商户（平台商户列表接口对商户账号 403）；不发强制日结（三份 HTML 与裁决都没有）。
type DailyClose = { id: string; businessDate?: string; scopeType?: string; scopeId?: string; status?: string; lockedAt?: string | null };
type Summary = { id: string; sourceStream?: string; amountCent?: number; refundCent?: number; orderCount?: number };
type Props = Pick<AdminPageProps, "api" | "user" | "effectivePermissions" | "setGlobalError">;

const PAGE_SIZE = 20;
const PLATFORM_SCOPES = ["project", "merchant", "bar", "ticketing", "amusement"];
const MERCHANT_SCOPES = ["merchant", "bar"];
const LOCK_IMPACT = "日结锁定后不能直接补录影响金额，必须上级审批通过后补录。";
const yuan = (cent?: number) => ((cent ?? 0) / 100).toFixed(2);
const scopeLabel = (code?: string) => KEY_VALUE_LABELS.scope_type?.[code ?? ""] ?? code ?? "—";
const sourceLabel = (code?: string) => KEY_VALUE_LABELS.source_stream?.[code ?? ""] ?? code ?? "—";

export function FinanceDailyClosePanel(props: Props) {
  return hasPermissions(props.effectivePermissions, ["finance.daily_close.manage"]) ? <DailyClosePanel {...props} /> : null;
}

function DailyClosePanel({ api, user, effectivePermissions, setGlobalError }: Props) {
  const merchant = user.merchantId ?? "";
  const scopes = merchant ? MERCHANT_SCOPES : PLATFORM_SCOPES;
  const list = useServerPagedRows<DailyClose>((page, size) => api.getPaged(`/api/v1/finance/daily-closes?page=${page}&size=${size}`), PAGE_SIZE);
  const [openId, setOpenId] = React.useState("");
  const [summaries, setSummaries] = React.useState<Summary[]>([]);
  const [businessDate, setBusinessDate] = React.useState("");
  const [scopeType, setScopeType] = React.useState(merchant ? "merchant" : "project");
  const [scopeId, setScopeId] = React.useState(merchant);
  const [note, setNote] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const requestKey = React.useRef<{ body: string; key: string } | null>(null);
  const canLock = hasPermissions(effectivePermissions, ["finance.daily_close.lock"]);

  async function run(work: () => Promise<void>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { await work(); } catch (e) { const message = e instanceof Error ? e.message : "日结操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function loadSummaries(id: string) {
    const rows = await api.get<Summary[]>(`/api/v1/finance/daily-closes/${encodeURIComponent(id)}/summaries`);
    setSummaries(Array.isArray(rows) ? rows : []); setOpenId(id);
  }
  async function lock() {
    await run(async () => {
      if (!businessDate) throw new Error("请选择营业日");
      if (!scopeId.trim()) throw new Error("请填写范围编号");
      const body = { businessDate, scopeType, scopeId: scopeId.trim(), operatorNote: note.trim() || undefined };
      // 请求体不变就重用同一个幂等键（响应丢失后重试不会另生成一张日结）。
      const serialized = JSON.stringify(body);
      if (requestKey.current?.body !== serialized) requestKey.current = { body: serialized, key: crypto.randomUUID() };
      const saved = await api.post<DailyClose>("/api/v1/finance/daily-closes", { ...body, idempotencyKey: requestKey.current.key });
      requestKey.current = null; setNote(""); list.reload();
      await loadSummaries(saved.id);
    });
  }

  return <section data-testid="finance-daily-close-panel">
    <div className="admin-section-title">日结</div>
    <p className="admin-muted-action">按营业日生成日结批次：锁定时按官方售票、渠道售票、酒吧、游乐、供应链成本、第三方回流、订台定金七源汇总当日金额、退款与笔数。{LOCK_IMPACT}</p>
    {error || list.error ? <p role="alert">{error || list.error?.message}</p> : null}
    {canLock ? <div className="admin-form-row" data-testid="daily-close-lock-form">
      <label>营业日<CnDatePicker aria-label="营业日" data-testid="daily-close-date" value={businessDate} onChange={setBusinessDate} /></label>
      <label>范围类型<select data-testid="daily-close-scope-type" value={scopeType}
        onChange={(e) => { setScopeType(e.target.value); if (!merchant) setScopeId(""); }}>
        {scopes.map((code) => <option key={code} value={code}>{scopeLabel(code)}</option>)}
      </select></label>
      {merchant ? <span>范围对象：{merchant}</span>
        : scopeType === "bar" ? <label>酒吧<BarVenueSelect api={api} value={scopeId} onChange={setScopeId} testId="daily-close-scope-bar" autoSelectFirst setGlobalError={setGlobalError} /></label>
        : <label>{scopeType === "merchant" ? "商户编号" : "项目编号"}<input aria-label="范围编号" value={scopeId} onChange={(e) => setScopeId(e.target.value)} /></label>}
      <label>备注<input aria-label="日结备注" value={note} onChange={(e) => setNote(e.target.value)} /></label>
      <HighRiskConfirm label="日结锁定" impact={LOCK_IMPACT} disabled={busy} onConfirm={() => void lock()} data-testid="daily-close-lock-submit" />
    </div> : null}
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="daily-close-table">
        <thead><tr><th>营业日</th><th>范围类型</th><th>范围编号</th><th>状态</th><th>锁定时间</th><th>汇总</th></tr></thead>
        <tbody>{list.rows.map((row) => <tr key={row.id} data-testid={`daily-close-row-${row.id}`}>
          <td>{cnDate(row.businessDate)}</td><td>{scopeLabel(row.scopeType)}</td><td>{row.scopeId ?? "—"}</td>
          <td>{formatBusinessStatus(row.status)}</td><td>{cnDate(row.lockedAt)}</td>
          <td><button type="button" data-testid={`daily-close-open-${row.id}`} disabled={busy} onClick={() => void run(() => loadSummaries(row.id))}>查看汇总</button></td>
        </tr>)}</tbody>
      </table>
      <TablePager testid="daily-close-pager" page={list.page} pageCount={list.pageCount} total={list.total} onPrev={list.prev} onNext={list.next} />
    </div>
    {openId ? <div className="admin-table-scroll">
      <div className="admin-section-title">日结七源汇总：{openId}</div>
      <table className="admin-table" data-testid="daily-close-summary-table">
        <thead><tr><th>来源</th><th>金额（元）</th><th>退款（元）</th><th>笔数</th></tr></thead>
        <tbody>{summaries.map((row) => <tr key={row.id}>
          <td>{sourceLabel(row.sourceStream)}</td><td>{yuan(row.amountCent)}</td><td>{yuan(row.refundCent)}</td><td>{row.orderCount ?? 0}</td>
        </tr>)}</tbody>
      </table>
    </div> : null}
  </section>;
}
