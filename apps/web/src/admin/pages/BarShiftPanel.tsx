// 文件版本：V0.02
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
// 第十八批 R 段第 142 条 142-R3（BSM:1081「当班收银员与接班人双方确认签字（电子确认同效；营业日最后一班没有接班人的，由财务跟班代替接班人确认签字），
// 财务跟班见证后归档」；BSM:1848「财务看退款、差异、日结和结算依据。」；PGM:664「差异认定……由该酒吧对应的财务 BP 负责」）：本区块在订单页给当班收银员
// 与接班人办交班单签字；reviewOnly 时放在财务对账页，只给持日结管理码的财务复核平账与见证归档，不出开班、班结与签字。
// 第十八批 R 段第 142 条 142-R4（BSM:1081「单人当班或能够明确定位到具体收银员时，由该收银员补足；多人共同使用同一钱箱时，由整个当班组共同补足。」；
// BSM:1489、ENUM:283）：同酒吧他人开班中、本人没有开班中班次时出「加入本钱箱」；成员同样班结交班与签字；复核有成员的短款时选责任人或当班组共同补足。
type Shift = {
  id: string; barId?: string; cashierUserId?: string; status?: string; openedAt?: string | null; closedAt?: string | null;
  openingCashCent?: number; declaredCashCent?: number | null; systemCashCent?: number; systemOnlineCent?: number; systemPosCent?: number;
  systemRefundCent?: number; systemTipCent?: number; cashDiffCent?: number | null; needsReview?: boolean; handoverToUserId?: string | null; note?: string | null;
  resolvedByUserId?: string | null; resolvedAt?: string | null; overageCause?: string | null; cashierSignedAt?: string | null; successorSignedAt?: string | null;
  witnessedByUserId?: string | null; witnessedAt?: string | null; responsibleUserId?: string | null;
  members?: { userId: string; joinedAt?: string | null; signedAt?: string | null }[];
};
type Props = Pick<AdminPageProps, "api" | "user" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & { reviewOnly?: boolean };

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
const members = (row: Shift) => row.members ?? [];
// 当班收银员＝开班人或已加入本钱箱的成员。
const onShift = (row: Shift, userId: string) => row.cashierUserId === userId || members(row).some((m) => m.userId === userId);
function sheetLabel(row: Shift) {
  if (row.status !== "closed") return "—";
  if (row.witnessedAt) return "已归档";
  if (row.needsReview) return "待复核平账";
  if (!row.cashierSignedAt || members(row).some((m) => !m.signedAt)) return "待收银员签字";
  if (row.handoverToUserId && !row.successorSignedAt) return "待接班人签字";
  return "待财务见证";
}

export function BarShiftPanel({ api, user, effectivePermissions, setActionResult, setGlobalError, reviewOnly = false }: Props) {
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
  const [causes, setCauses] = React.useState<Record<string, string>>({});
  const [responsibles, setResponsibles] = React.useState<Record<string, string>>({});
  const requestKey = React.useRef<{ body: string; key: string } | null>(null);
  const canManage = hasPermissions(effectivePermissions, ["bar.shift.manage"]);
  const canClose = hasPermissions(effectivePermissions, ["bar.shift.close"]);
  const visible = reviewOnly ? hasPermissions(effectivePermissions, ["finance.daily_close.manage"]) : canManage;

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
    if (visible) load().catch((e) => setError(e instanceof Error ? e.message : "收银交班记录读取失败"));
  }, [visible, api]); // eslint-disable-line react-hooks/exhaustive-deps

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

  // 交班单签字（sign）、复核平账（resolve）、见证归档（witness）、加入本钱箱（join）：成功后只重读列表。
  async function act(id: string, action: "sign" | "resolve" | "witness" | "join", extra: Record<string, unknown> = {}) {
    await run(async () => {
      const path = `/api/v1/bar/shifts/${encodeURIComponent(id)}/${action}`;
      await api.post(path, { ...extra, idempotencyKey: keyFor(path, extra) });
      requestKey.current = null; await load();
    });
  }
  function canSign(row: Shift) {
    return row.status === "closed" && !row.needsReview && !row.witnessedAt
      && ((row.cashierUserId === user.id && !row.cashierSignedAt) || (row.handoverToUserId === user.id && !row.successorSignedAt)
        || members(row).some((m) => m.userId === user.id && !m.signedAt));
  }
  function canJoin(row: Shift) {
    return row.status === "open" && !rows.some((r) => r.status === "open" && r.barId === row.barId && onShift(r, user.id));
  }
  function reviewCell(row: Shift) {
    if (row.status !== "closed" || row.witnessedAt) return null;
    if (row.needsReview && (row.cashDiffCent ?? 0) > 0) {
      const cause = (causes[row.id] ?? "").trim();
      return <>
        <input aria-label={`录像查明的原因 ${row.id}`} placeholder="录像查明的原因" value={causes[row.id] ?? ""} onChange={(e) => setCauses({ ...causes, [row.id]: e.target.value })} />
        <HighRiskConfirm label="复核平账" impact="长款当期计入该商户营业外收入并单独留痕可查，不得留存钱箱、不得用于冲抵其他班次的短款。" disabled={busy}
          precheck={() => { if (cause) return true; setError("长款须填写录像查明的原因"); return false; }}
          onConfirm={() => void act(row.id, "resolve", { overageCause: cause })} data-testid={`bar-shift-resolve-${row.id}`} />
      </>;
    }
    if (row.needsReview) {
      const responsible = responsibles[row.id] ?? "";
      return <>
        {members(row).length ? <select aria-label={`责任人 ${row.id}`} value={responsible} onChange={(e) => setResponsibles({ ...responsibles, [row.id]: e.target.value })}>
          <option value="">当班组共同补足</option>
          {row.cashierUserId ? <option value={row.cashierUserId}>{row.cashierUserId}</option> : null}
          {members(row).map((m) => <option key={m.userId} value={m.userId}>{m.userId}</option>)}
        </select> : null}
        <HighRiskConfirm label="复核平账" impact="短款补足即为处理完毕，不另计处罚扣款。" disabled={busy}
          onConfirm={() => void act(row.id, "resolve", responsible ? { responsibleUserId: responsible } : {})} data-testid={`bar-shift-resolve-${row.id}`} />
      </>;
    }
    if (!row.cashierSignedAt || (row.handoverToUserId && !row.successorSignedAt) || members(row).some((m) => !m.signedAt)) return null;
    return <HighRiskConfirm label={row.handoverToUserId ? "见证归档" : "代接班人签字并见证归档"} disabled={busy}
      impact={row.handoverToUserId ? "财务跟班见证后归档；交班单按留痕通则永久保存。"
        : "营业日最后一班没有接班人的，由财务跟班代替接班人确认签字，财务跟班见证后归档；交班单按留痕通则永久保存。"}
      onConfirm={() => void act(row.id, "witness")} data-testid={`bar-shift-witness-${row.id}`} />;
  }

  if (!visible) return null;
  // minWidth 0：本区块是页面 grid 的子项，不设时 18 列交班表的最小宽度会把整列撑过手机视口（表格改在 admin-table-scroll 内横向滚动）。
  return <section data-testid="bar-shift-panel" style={{ minWidth: 0 }}>
    <div className="admin-section-title">{reviewOnly ? "收银交班复核与见证" : "收银交班对账"}</div>
    <p className="admin-muted-action">{reviewOnly
      ? "收银出现任何金额的差额一律进复核、定位责任：短款补足即为处理完毕，不另计处罚扣款；长款须登记录像查明的原因，当期计入该商户营业外收入。当班收银员与接班人双方确认签字后由财务跟班见证归档；营业日最后一班没有接班人的，由财务跟班代替接班人确认签字。多人共同使用同一钱箱的短款，能明确定位到具体收银员的选该收银员补足，否则由整个当班组共同补足。"
      : "开班登记备用金；班结按本班次汇总现金、扫码、POS、退款、小费应收，录入实点现金（含备用金）算长短款。收银不设容差，出现任何金额的差额一律进复核、定位责任。收银员按当前登录账号记录。平账后当班收银员与接班人各自办交班单签字（电子确认同效）。多人共同使用同一钱箱的，其他收银员对他人开班中的班次「加入本钱箱」，加入后其收款一并计入本班次，交班单由当班组成员各自签字。"}</p>
    {error ? <p role="alert">{error}</p> : null}
    {reviewOnly ? null : <div className="admin-form-row" data-testid="bar-shift-open-form">
      {user.merchantId ? <span>酒吧：{user.merchantId}</span>
        : <label>酒吧<BarVenueSelect api={api} value={barId} onChange={setBarId} testId="bar-shift-open-bar" autoSelectFirst setGlobalError={setGlobalError} /></label>}
      <label>备用金（元）<input aria-label="备用金" inputMode="decimal" value={openingCash} onChange={(e) => setOpeningCash(e.target.value)} /></label>
      <label>开班备注<input aria-label="开班备注" value={openNote} onChange={(e) => setOpenNote(e.target.value)} /></label>
      <button type="button" data-testid="bar-shift-open-submit" disabled={busy} onClick={() => void openShift()}>开班</button>
    </div>}
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="bar-shift-table">
        <thead><tr><th>酒吧</th><th>收银员</th><th>当班组成员</th><th>状态</th><th>开班时间</th><th>交班时间</th><th>备用金（元）</th><th>实点现金（元）</th><th>系统现金（元）</th><th>扫码（元）</th><th>POS（元）</th><th>退款（元）</th><th>小费（元）</th><th>长短款</th><th>交接给</th><th>备注</th><th>交班单</th><th>操作</th></tr></thead>
        <tbody>{rows.map((row) => <tr key={row.id} data-testid={`bar-shift-row-${row.id}`}>
          <td>{row.barId ?? "—"}</td><td>{row.cashierUserId ?? "—"}</td><td>{members(row).map((m) => m.userId).join("、") || "—"}</td><td>{stateLabel(row)}</td><td>{cnDate(row.openedAt)}</td><td>{cnDate(row.closedAt)}</td>
          <td>{yuan(row.openingCashCent)}</td><td>{yuan(row.declaredCashCent)}</td><td>{yuan(row.systemCashCent)}</td><td>{yuan(row.systemOnlineCent)}</td><td>{yuan(row.systemPosCent)}</td>
          <td>{yuan(row.systemRefundCent)}</td><td>{yuan(row.systemTipCent)}</td><td>{row.status === "closed" ? diffLabel(row.cashDiffCent) : "—"}</td><td>{row.handoverToUserId ?? "—"}</td><td>{row.note ?? "—"}</td><td>{sheetLabel(row)}</td>
          <td>{reviewOnly ? reviewCell(row) : <>
            {canJoin(row) ? <button type="button" data-testid={`bar-shift-join-${row.id}`} disabled={busy} onClick={() => void act(row.id, "join")}>加入本钱箱</button> : null}
            {row.status === "open" && onShift(row, user.id) && canClose
              ? <button type="button" data-testid={`bar-shift-close-${row.id}`} disabled={busy} onClick={() => { setClosingId(row.id); setClosed(null); }}>班结交班</button> : null}
            {canSign(row) ? <HighRiskConfirm label="交班单签字" impact="当班收银员与接班人双方确认签字（电子确认同效）。" disabled={busy}
              onConfirm={() => void act(row.id, "sign")} data-testid={`bar-shift-sign-${row.id}`} /> : null}
          </>}</td>
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
