// 文件版本：V0.04
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
// 第十八批 R 段第 143 条 143-R1（ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」；PGM:664「单店层面的财务工作——日结确认与锁定……
// 由该酒吧对应的财务 BP 负责」）：先「生成日结」（待确认），再逐行「确认」「锁定」；所选营业日与范围查不到日结即「待生成」（不入库）。
// 143-R2（ENUM:190「需要逐类标记为最终的数据集有五类……由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」）：列表显示五类已标数，
// 展开某行列出五类，待确认、有差异的行逐类「标记为最终」（标记后不可撤销）。
// 143-R5（第十八批裁决 33「锁定后，由该店持日结锁定权限的财务 BP 点「归档」；归档后只读可查，锁定后补录照常走上级审批。」）：
// 已锁定的行出「归档」二次确认；已归档的行只读（照常看七源汇总与五类标记）。
// 143-R4（ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额：补录金额归入当期，并在补录记录上标明其所属的原营业日；同时在原营业日的
// 日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查。」；财务 PRD §5.4 日结页面直接展示补录人、补录原因、批准人含时间）：展开已锁定、已归档的行
// 列出「本日结锁定的台账」（逐行申请补录，二次确认文案为 BSM:2023 原句）与「锁定后补录」（待审批的可批准、驳回）。
// 第十八批 S 段第 161 条 161-R3（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：票务、游乐由综合体的项目范围日结覆盖，
// 不再单独出日结，平台侧范围类型只有项目、商户、酒吧。
type LockedItem = { id: string; itemNo?: string; direction?: string; amountCent?: number; dueDate?: string };
type Adjustment = { id: string; itemNo?: string; adjustmentAmountCent?: number; reason?: string; requestedBy?: string; createdAt?: string;
  approvalStatus?: string; approvedBy?: string | null; decidedAt?: string | null; originalBusinessDate?: string | null };
type DailyClose = { id: string; businessDate?: string; scopeType?: string; scopeId?: string; status?: string; confirmedAt?: string | null; lockedAt?: string | null; archivedAt?: string | null; finalMarkCategories?: string[] };
type Summary = { id: string; sourceStream?: string; amountCent?: number; refundCent?: number; orderCount?: number };
// 第十八批 Y 段复核修复（BSM:1682「按营业日生成日结批次。」）：180-R1 起对账批次随日结生成，生成成功后回调所在页面（对账差异页据此重读对账批次与差异）。
type Props = Pick<AdminPageProps, "api" | "user" | "effectivePermissions" | "setGlobalError"> & { onGenerated?: () => void };

const PAGE_SIZE = 20;
const PLATFORM_SCOPES = ["project", "merchant", "bar"];
const MERCHANT_SCOPES = ["merchant", "bar"];
const LOCK_IMPACT = "日结锁定后不能直接补录影响金额，必须上级审批通过后补录。";
const ARCHIVE_IMPACT = "归档后只读可查，锁定后补录照常走上级审批。";
// 143-R2：五类数据按 ENUM:190 原句顺序；标记前二次确认文案取 ENUM:190 原句。
const FINAL_MARK_CATEGORIES = ["orders_verifications", "refunds_reversals", "production_handover", "attendance", "anomaly_resolutions"];
const FINAL_MARK_IMPACT = "标记后不可撤销，事后发现错误只能走冲正，不得改回未最终。";
const yuan = (cent?: number) => ((cent ?? 0) / 100).toFixed(2);
// 台账方向：与后端 FinanceService.AR_AP_DIRECTION_LABELS 同名（142-R1 长款营业外收入台账为内部核算 internal_accounting）。
const DIRECTION_LABELS: Record<string, string> = { receivable: "应收", payable: "应付", internal_accounting: "内部核算" };
const directionLabel = (code?: string) => DIRECTION_LABELS[code ?? ""] ?? code ?? "—";
const scopeLabel = (code?: string) => KEY_VALUE_LABELS.scope_type?.[code ?? ""] ?? code ?? "—";
const sourceLabel = (code?: string) => KEY_VALUE_LABELS.source_stream?.[code ?? ""] ?? code ?? "—";
const statusLabel = (code?: string) => KEY_VALUE_LABELS.daily_close_status?.[code ?? ""] ?? code ?? "—";
const finalMarkLabel = (code: string) => KEY_VALUE_LABELS.final_mark_category?.[code] ?? code;

export function FinanceDailyClosePanel(props: Props) {
  return hasPermissions(props.effectivePermissions, ["finance.daily_close.manage"]) ? <DailyClosePanel {...props} /> : null;
}

function DailyClosePanel({ api, user, effectivePermissions, setGlobalError, onGenerated }: Props) {
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
  // 所选营业日与范围的日结：undefined＝还没选好，null＝待生成。
  const [current, setCurrent] = React.useState<DailyClose | null | undefined>(undefined);
  const [refresh, setRefresh] = React.useState(0);
  const keys = React.useRef<Record<string, string>>({});
  const canLock = hasPermissions(effectivePermissions, ["finance.daily_close.lock"]);
  const chosenScope = scopeId.trim();
  const opened = list.rows.find((row) => row.id === openId);
  // 143-R4：展开的日结已锁定或已归档时列被它锁定的台账与锁定后补录。
  const lockedOpen = opened && (opened.status === "locked" || opened.status === "archived") ? opened.id : "";
  const canViewArAp = hasPermissions(effectivePermissions, ["finance.ar_ap.view"]);
  const canRequest = hasPermissions(effectivePermissions, ["finance.locked_backfill.request"]);
  const canApprove = hasPermissions(effectivePermissions, ["finance.locked_backfill.approve"]);
  const [lockedItems, setLockedItems] = React.useState<LockedItem[]>([]);
  const [adjustments, setAdjustments] = React.useState<Adjustment[]>([]);
  const [drafts, setDrafts] = React.useState<Record<string, { amount: string; reason: string }>>({});
  const [backfillRefresh, setBackfillRefresh] = React.useState(0);

  React.useEffect(() => {
    if (!lockedOpen) return;
    let alive = true;
    const id = encodeURIComponent(lockedOpen);
    // ponytail: 被锁台账只取第一页 200 条（接口每页上限）；一日一范围超过 200 条再加翻页。
    Promise.all([
      canViewArAp ? api.getPaged<LockedItem>(`/api/v1/finance/ar-ap-items?locked_day_close_id=${id}&page=1&size=200`).then((page) => page.items)
        : Promise.resolve([] as LockedItem[]),
      api.get<Adjustment[]>(`/api/v1/finance/daily-closes/${id}/adjustments`),
    ]).then(([items, rows]) => { if (alive) { setLockedItems(items); setAdjustments(Array.isArray(rows) ? rows : []); } })
      .catch((e: unknown) => { if (alive) setError(e instanceof Error ? e.message : "锁定后补录读取失败"); });
    return () => { alive = false; };
  }, [api, lockedOpen, canViewArAp, backfillRefresh]);

  React.useEffect(() => {
    if (!businessDate || !chosenScope) { setCurrent(undefined); return; }
    let alive = true;
    const query = `business_date=${businessDate}&scope_type=${encodeURIComponent(scopeType)}&scope_id=${encodeURIComponent(chosenScope)}`;
    api.getPaged<DailyClose>(`/api/v1/finance/daily-closes?${query}&page=1&size=1`)
      .then((page) => { if (alive) setCurrent(page.items[0] ?? null); })
      .catch((e: unknown) => { if (alive) setError(e instanceof Error ? e.message : "日结状态读取失败"); });
    return () => { alive = false; };
  }, [api, businessDate, scopeType, chosenScope, refresh]);

  async function run(work: () => Promise<void>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { await work(); } catch (e) { const message = e instanceof Error ? e.message : "日结操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function loadSummaries(id: string) {
    const rows = await api.get<Summary[]>(`/api/v1/finance/daily-closes/${encodeURIComponent(id)}/summaries`);
    setSummaries(Array.isArray(rows) ? rows : []); setOpenId(id);
  }
  // 同一请求（同一请求体、同一行同一动作）重用同一个幂等键：响应丢失后重试不会另生成一张日结或重复办理。
  function keyFor(slot: string) { return (keys.current[slot] ??= crypto.randomUUID()); }
  async function generate() {
    await run(async () => {
      if (!businessDate) throw new Error("请选择营业日");
      if (!chosenScope) throw new Error("请填写范围编号");
      const body = { businessDate, scopeType, scopeId: chosenScope, operatorNote: note.trim() || undefined };
      const slot = `generate:${JSON.stringify(body)}`;
      const saved = await api.post<DailyClose>("/api/v1/finance/daily-closes", { ...body, idempotencyKey: keyFor(slot) });
      delete keys.current[slot]; setNote(""); list.reload(); setRefresh((n) => n + 1); onGenerated?.();
      await loadSummaries(saved.id);
    });
  }
  // 143-R4：申请补录（调整额按元填、可为负，原因必填）与审批补录。
  function backfillReady(item: LockedItem) {
    const draft = drafts[item.id];
    const cent = Math.round(Number(draft?.amount) * 100);
    if (draft?.reason.trim() && Number.isFinite(cent) && cent !== 0) return true;
    setError("请填写调整额（元，不为零，可为负）与补录原因"); return false;
  }
  async function requestBackfill(item: LockedItem) {
    const draft = drafts[item.id] ?? { amount: "", reason: "" };
    await run(async () => {
      const body = { arApItemId: item.id, adjustmentAmountCent: Math.round(Number(draft.amount) * 100), reason: draft.reason.trim() };
      const slot = `backfill:${JSON.stringify(body)}`;
      await api.post("/api/v1/finance/ar-ap-adjustments", { ...body, idempotencyKey: keyFor(slot) });
      delete keys.current[slot]; setDrafts((all) => ({ ...all, [item.id]: { amount: "", reason: "" } })); setBackfillRefresh((n) => n + 1);
    });
  }
  async function decide(id: string, decision: "approve" | "reject") {
    await run(async () => {
      const slot = `decide:${id}:${decision}`;
      await api.post(`/api/v1/finance/ar-ap-adjustments/${encodeURIComponent(id)}/approve`, { idempotencyKey: keyFor(slot), decision });
      delete keys.current[slot]; setBackfillRefresh((n) => n + 1);
    });
  }
  async function act(id: string, action: "confirm" | "lock" | "archive" | "final-marks", category?: string) {
    await run(async () => {
      const slot = [action, id, category].join(":");
      await api.post<DailyClose>(`/api/v1/finance/daily-closes/${encodeURIComponent(id)}/${action}`,
        { idempotencyKey: keyFor(slot), ...(category ? { category } : {}) });
      delete keys.current[slot]; list.reload(); setRefresh((n) => n + 1);
      // 确认时后端按当前数据重算七源汇总：办理的是已展开的行就重读，锁定前看到的是确认时的金额。
      if (id === openId) await loadSummaries(id);
    });
  }

  return <section data-testid="finance-daily-close-panel">
    <div className="admin-section-title">日结</div>
    <p className="admin-muted-action">按营业日生成日结批次：生成时按官方售票、渠道售票、酒吧、游乐、供应链成本、第三方回流、订台定金七源汇总当日金额、退款与笔数，确认时按当前数据重算；先确认、再锁定。{LOCK_IMPACT}</p>
    {error || list.error ? <p role="alert">{error || list.error?.message}</p> : null}
    {canLock ? <div className="admin-form-row" data-testid="daily-close-generate-form">
      <label>营业日<CnDatePicker aria-label="营业日" data-testid="daily-close-date" value={businessDate} onChange={setBusinessDate} /></label>
      <label>范围类型<select data-testid="daily-close-scope-type" value={scopeType}
        onChange={(e) => { setScopeType(e.target.value); if (!merchant) setScopeId(""); }}>
        {scopes.map((code) => <option key={code} value={code}>{scopeLabel(code)}</option>)}
      </select></label>
      {merchant ? <span>范围对象：{merchant}</span>
        : scopeType === "bar" ? <label>酒吧<BarVenueSelect api={api} value={scopeId} onChange={setScopeId} testId="daily-close-scope-bar" autoSelectFirst setGlobalError={setGlobalError} /></label>
        : <label>{scopeType === "merchant" ? "商户编号" : "项目编号"}<input aria-label="范围编号" value={scopeId} onChange={(e) => setScopeId(e.target.value)} /></label>}
      <label>备注<input aria-label="日结备注" value={note} onChange={(e) => setNote(e.target.value)} /></label>
      <button type="button" data-testid="daily-close-generate-submit" disabled={busy} onClick={() => void generate()}>生成日结</button>
    </div> : null}
    {canLock && current !== undefined ? <p data-testid="daily-close-current-status">所选营业日与范围的日结状态：{current ? statusLabel(current.status) : "待生成"}</p> : null}
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="daily-close-table">
        <thead><tr><th>营业日</th><th>范围类型</th><th>范围编号</th><th>状态</th><th>五类标记</th><th>确认时间</th><th>锁定时间</th><th>归档时间</th><th>汇总</th>{canLock ? <th>操作</th> : null}</tr></thead>
        <tbody>{list.rows.map((row) => <tr key={row.id} data-testid={`daily-close-row-${row.id}`}>
          <td>{cnDate(row.businessDate)}</td><td>{scopeLabel(row.scopeType)}</td><td>{row.scopeId ?? "—"}</td>
          <td>{statusLabel(row.status)}</td><td>{(row.finalMarkCategories ?? []).length}/5</td><td>{cnDate(row.confirmedAt)}</td><td>{cnDate(row.lockedAt)}</td><td>{cnDate(row.archivedAt)}</td>
          <td><button type="button" data-testid={`daily-close-open-${row.id}`} disabled={busy} onClick={() => void run(() => loadSummaries(row.id))}>查看汇总</button></td>
          {canLock ? <td>{row.status === "pending_confirm"
            ? <button type="button" data-testid={`daily-close-confirm-${row.id}`} disabled={busy} onClick={() => void act(row.id, "confirm")}>确认</button>
            : row.status === "confirmed"
              ? <HighRiskConfirm label="锁定" impact={LOCK_IMPACT} disabled={busy} onConfirm={() => void act(row.id, "lock")} data-testid={`daily-close-lock-${row.id}`} />
              : row.status === "locked"
                ? <HighRiskConfirm label="归档" impact={ARCHIVE_IMPACT} disabled={busy} onConfirm={() => void act(row.id, "archive")} data-testid={`daily-close-archive-${row.id}`} />
                : null}</td> : null}
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
    {opened ? <div className="admin-table-scroll">
      <div className="admin-section-title">五类数据最终标记：{opened.id}</div>
      <table className="admin-table" data-testid="daily-close-final-marks">
        <thead><tr><th>数据类别</th><th>标记</th></tr></thead>
        <tbody>{FINAL_MARK_CATEGORIES.map((code) => <tr key={code}>
          <td>{finalMarkLabel(code)}</td>
          <td>{(opened.finalMarkCategories ?? []).includes(code) ? "已标记为最终"
            : canLock && (opened.status === "pending_confirm" || opened.status === "has_difference")
              ? <HighRiskConfirm label="标记为最终" impact={FINAL_MARK_IMPACT} disabled={busy}
                onConfirm={() => void act(opened.id, "final-marks", code)} data-testid={`daily-close-final-mark-${code}`} />
              : "未标记"}</td>
        </tr>)}</tbody>
      </table>
    </div> : null}
    {lockedOpen ? <div className="admin-table-scroll">
      {canViewArAp ? <>
        <div className="admin-section-title">本日结锁定的台账：{lockedOpen}</div>
        <table className="admin-table" data-testid="daily-close-locked-items">
          <thead><tr><th>单号</th><th>方向</th><th>金额（元）</th><th>到期日</th>{canRequest ? <th>申请补录</th> : null}</tr></thead>
          <tbody>{lockedItems.map((item) => {
            const draft = drafts[item.id] ?? { amount: "", reason: "" };
            const edit = (patch: Partial<typeof draft>) => setDrafts((all) => ({ ...all, [item.id]: { ...draft, ...patch } }));
            return <tr key={item.id}>
              <td>{item.itemNo ?? item.id}</td><td>{directionLabel(item.direction)}</td><td>{yuan(item.amountCent)}</td><td>{cnDate(item.dueDate)}</td>
              {canRequest ? <td>
                <input aria-label="调整额（元）" data-testid={`daily-close-backfill-amount-${item.id}`} value={draft.amount} onChange={(e) => edit({ amount: e.target.value })} />
                <input aria-label="补录原因" data-testid={`daily-close-backfill-reason-${item.id}`} value={draft.reason} onChange={(e) => edit({ reason: e.target.value })} />
                <HighRiskConfirm label="申请补录" impact={LOCK_IMPACT} disabled={busy} precheck={() => backfillReady(item)}
                  onConfirm={() => void requestBackfill(item)} data-testid={`daily-close-backfill-${item.id}`} />
              </td> : null}
            </tr>;
          })}</tbody>
        </table>
      </> : null}
      <div className="admin-section-title">锁定后补录：{lockedOpen}</div>
      <table className="admin-table" data-testid="daily-close-adjustments">
        <thead><tr><th>台账单号</th><th>调整额（元）</th><th>原因</th><th>补录人</th><th>申请时间</th><th>状态</th><th>批准人</th><th>批准时间</th><th>原营业日</th>{canApprove ? <th>审批</th> : null}</tr></thead>
        <tbody>{adjustments.map((row) => <tr key={row.id}>
          <td>{row.itemNo ?? "—"}</td><td>{yuan(row.adjustmentAmountCent)}</td><td>{row.reason ?? "—"}</td><td>{row.requestedBy ?? "—"}</td>
          <td>{cnDate(row.createdAt)}</td><td>{formatBusinessStatus(row.approvalStatus)}</td><td>{row.approvedBy ?? "—"}</td>
          <td>{cnDate(row.decidedAt)}</td><td>{cnDate(row.originalBusinessDate)}</td>
          {canApprove ? <td>{row.approvalStatus === "pending" ? <>
            <button type="button" data-testid={`daily-close-adjustment-approve-${row.id}`} disabled={busy} onClick={() => void decide(row.id, "approve")}>批准</button>
            <button type="button" data-testid={`daily-close-adjustment-reject-${row.id}`} disabled={busy} onClick={() => void decide(row.id, "reject")}>驳回</button>
          </> : null}</td> : null}
        </tr>)}</tbody>
      </table>
    </div> : null}
  </section>;
}
