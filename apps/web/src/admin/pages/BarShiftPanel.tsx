// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { BarVenueSelect } from "../components/BarVenueSelect";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { cnDate } from "../components/RowDetailDrawer";
import { hasPermissions } from "../security/PermissionGate";

// 第十八批 P 段第 133 条 133-R1（BSM:1081「收银员开班记备用金，班结按本班次汇总各支付方式应收（现金/扫码/POS/退款/小费），录入实点现金算长短款差异。
// 收银不应出现差额，出现任何金额的差额一律进复核、定位责任，不设容差」「由当班收银员完成现金清点与盘收银，接班人与其对接完成交接」；
// BSM:940 收银按该次登录会话定位到操作人；ENUM:115 收银交班状态；DEC-R4-2026-08-04-26）：后台「订单/出品/账单」页的收银交班对账区块。
// 收银员取登录人、不发容差（后端按 0 算）；商户侧账号的酒吧就是本账号商户；只有当班收银员本人能对自己开班中的班次做班结交班。
type Shift = {
  id: string; barId?: string; cashierUserId?: string; status?: string; openedAt?: string | null; closedAt?: string | null;
  openingCashCent?: number; declaredCashCent?: number | null; systemCashCent?: number; systemOnlineCent?: number; systemPosCent?: number;
  systemRefundCent?: number; systemTipCent?: number; cashDiffCent?: number | null; needsReview?: boolean; handoverToUserId?: string | null; note?: string | null;
};
type Props = Pick<AdminPageProps, "api" | "user" | "effectivePermissions" | "setActionResult" | "setGlobalError">;

const yuan = (cent?: number | null) => (cent == null ? "—" : (cent / 100).toFixed(2));
function cents(raw: string, name: string) {
  if (!/^\d+(\.\d{1,2})?$/.test(raw.trim())) throw new Error(`${name}须填写非负金额（元），最多两位小数`);
  const value = Math.round(Number(raw) * 100);
  if (!Number.isSafeInteger(value)) throw new Error("金额超出可办理范围");
  return value;
}
function stateLabel(row: Shift) {
  if (row.status === "open") return "开班";
  if (row.status === "closed") return row.needsReview ? "有差异待复核" : "已平账";
  return row.status ?? "—";
}
function diffLabel(cent?: number | null) {
  if (cent == null) return "—";
  return cent > 0 ? `长款 ${yuan(cent)} 元` : cent < 0 ? `短款 ${yuan(-cent)} 元` : "平账";
}

export function BarShiftPanel({ api, user, effectivePermissions, setActionResult, setGlobalError }: Props) {
  const [rows, setRows] = React.useState<Shift[]>([]);
  const [barId, setBarId] = React.useState(user.merchantId ?? "");
  const [openingCash, setOpeningCash] = React.useState("");
  const [openNote, setOpenNote] = React.useState("");
  const [closingId, setClosingId] = React.useState("");
  const [declaredCash, setDeclaredCash] = React.useState("");
  const [handoverTo, setHandoverTo] = React.useState("");
  const [closeNote, setCloseNote] = React.useState("");
  const [closed, setClosed] = React.useState<Shift | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const requestKey = React.useRef<{ body: string; key: string } | null>(null);
  const canManage = hasPermissions(effectivePermissions, ["bar.shift.manage"]);
  const canClose = hasPermissions(effectivePermissions, ["bar.shift.close"]);

  async function run(work: () => Promise<void>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { await work(); } catch (e) { const message = e instanceof Error ? e.message : "收银交班操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function load() {
    const data = await api.get<Shift[]>("/api/v1/bar/shifts");
    setRows(Array.isArray(data) ? data : []);
  }
  // 请求体不变就重用同一个幂等键（响应丢失后重试不会重复开班或交班）。
  function keyFor(path: string, body: Record<string, unknown>) {
    const serialized = JSON.stringify({ path, ...body });
    if (requestKey.current?.body !== serialized) requestKey.current = { body: serialized, key: crypto.randomUUID() };
    return requestKey.current.key;
  }
  React.useEffect(() => {
    if (canManage) load().catch((e) => setError(e instanceof Error ? e.message : "收银交班记录读取失败"));
  }, [canManage, api]); // eslint-disable-line react-hooks/exhaustive-deps

  async function openShift() {
    await run(async () => {
      if (!barId) throw new Error("请先选择酒吧");
      const path = "/api/v1/bar/shifts/open";
      const body = { barId, cashierUserId: user.id, openingCashCent: cents(openingCash, "备用金"), note: openNote.trim() || undefined };
      await api.post(path, { ...body, idempotencyKey: keyFor(path, body) });
      requestKey.current = null; setOpeningCash(""); setOpenNote(""); await load();
    });
  }
  async function closeShift() {
    await run(async () => {
      const path = `/api/v1/bar/shifts/${encodeURIComponent(closingId)}/close`;
      const body = { declaredCashCent: cents(declaredCash, "实点现金"), handoverToUserId: handoverTo.trim() || undefined, note: closeNote.trim() || undefined };
      const result = await api.post<Shift>(path, { ...body, idempotencyKey: keyFor(path, body) });
      requestKey.current = null; setClosed(result); setClosingId(""); setDeclaredCash(""); setHandoverTo(""); setCloseNote("");
      setActionResult({ id: result.id, systemCashCent: result.systemCashCent, systemPosCent: result.systemPosCent, systemOnlineCent: result.systemOnlineCent,
        systemRefundCent: result.systemRefundCent, systemTipCent: result.systemTipCent, declaredCashCent: result.declaredCashCent,
        cashDiffCent: result.cashDiffCent, needsReview: result.needsReview });
      await load();
    });
  }

  if (!canManage) return null;
  // minWidth 0：本区块是页面 grid 的子项，不设时 16 列交班表的最小宽度会把整列撑过手机视口（表格改在 admin-table-scroll 内横向滚动）。
  return <section data-testid="bar-shift-panel" style={{ minWidth: 0 }}>
    <div className="admin-section-title">收银交班对账</div>
    <p className="admin-muted-action">开班登记备用金；班结按本班次汇总现金、扫码、POS、退款、小费应收，录入实点现金（含备用金）算长短款。收银不设容差，出现任何金额的差额一律进复核、定位责任。收银员按当前登录账号记录。</p>
    {error ? <p role="alert">{error}</p> : null}
    <div className="admin-form-row" data-testid="bar-shift-open-form">
      {user.merchantId ? <span>酒吧：{user.merchantId}</span>
        : <label>酒吧<BarVenueSelect api={api} value={barId} onChange={setBarId} testId="bar-shift-open-bar" autoSelectFirst setGlobalError={setGlobalError} /></label>}
      <label>备用金（元）<input aria-label="备用金" inputMode="decimal" value={openingCash} onChange={(e) => setOpeningCash(e.target.value)} /></label>
      <label>开班备注<input aria-label="开班备注" value={openNote} onChange={(e) => setOpenNote(e.target.value)} /></label>
      <button type="button" data-testid="bar-shift-open-submit" disabled={busy} onClick={() => void openShift()}>开班</button>
    </div>
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="bar-shift-table">
        <thead><tr><th>酒吧</th><th>收银员</th><th>状态</th><th>开班时间</th><th>交班时间</th><th>备用金（元）</th><th>实点现金（元）</th><th>系统现金（元）</th><th>扫码（元）</th><th>POS（元）</th><th>退款（元）</th><th>小费（元）</th><th>长短款</th><th>交接给</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>{rows.map((row) => <tr key={row.id} data-testid={`bar-shift-row-${row.id}`}>
          <td>{row.barId ?? "—"}</td><td>{row.cashierUserId ?? "—"}</td><td>{stateLabel(row)}</td><td>{cnDate(row.openedAt)}</td><td>{cnDate(row.closedAt)}</td>
          <td>{yuan(row.openingCashCent)}</td><td>{yuan(row.declaredCashCent)}</td><td>{yuan(row.systemCashCent)}</td><td>{yuan(row.systemOnlineCent)}</td><td>{yuan(row.systemPosCent)}</td>
          <td>{yuan(row.systemRefundCent)}</td><td>{yuan(row.systemTipCent)}</td><td>{row.status === "closed" ? diffLabel(row.cashDiffCent) : "—"}</td><td>{row.handoverToUserId ?? "—"}</td><td>{row.note ?? "—"}</td>
          <td>{row.status === "open" && row.cashierUserId === user.id && canClose
            ? <button type="button" data-testid={`bar-shift-close-${row.id}`} disabled={busy} onClick={() => { setClosingId(row.id); setClosed(null); }}>班结交班</button> : null}</td>
        </tr>)}</tbody>
      </table>
    </div>
    {closingId ? <div className="admin-form-row" data-testid="bar-shift-close-form">
      <span>班结交班：{closingId}</span>
      <label>实点现金（元，含备用金）<input aria-label="实点现金" inputMode="decimal" value={declaredCash} onChange={(e) => setDeclaredCash(e.target.value)} /></label>
      <label>接班人（选填）<input aria-label="接班人" value={handoverTo} onChange={(e) => setHandoverTo(e.target.value)} /></label>
      <label>交班备注<input aria-label="交班备注" value={closeNote} onChange={(e) => setCloseNote(e.target.value)} /></label>
      <HighRiskConfirm label="确认班结交班" impact="按本班次汇总各支付方式应收并与实点现金比对，出现任何金额的差额一律进复核、定位责任；交班后本班次不能再次班结。"
        disabled={busy} onConfirm={() => void closeShift()} data-testid="bar-shift-close-submit" />
    </div> : null}
    {closed ? <p role="status" data-testid="bar-shift-close-result">
      本班次已交班：现金应收 {yuan(closed.systemCashCent)} 元，扫码 {yuan(closed.systemOnlineCent)} 元，POS {yuan(closed.systemPosCent)} 元，退款 {yuan(closed.systemRefundCent)} 元，小费 {yuan(closed.systemTipCent)} 元；
      实点现金 {yuan(closed.declaredCashCent)} 元；{diffLabel(closed.cashDiffCent)}。{closed.needsReview ? "有差异，已进复核。" : ""}
    </p> : null}
  </section>;
}
