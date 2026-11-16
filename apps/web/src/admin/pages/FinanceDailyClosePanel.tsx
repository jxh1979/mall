// 文件版本：V0.07
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
import { withBase } from "../../basePath";
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
// 列出「本日结锁定的台账」（逐行申请补录，二次确认文案为 BSM:2023 原句）与「锁定后补录」。
// 第十八批 S 段第 161 条 161-R3（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：票务、游乐由综合体的项目范围日结覆盖，
// 不再单独出日结，平台侧范围类型只有项目、商户、酒吧。
// 第十八批 Z 段第 186 条 186-R1（ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额：补录金额归入当期……」；BSM:2023；
// 第七批裁决 19「人工登记收款仍受锁定拦截。」）：「本日结锁定的台账」逐行另可「申请收付款补录」（金额、收付方式、原因），上级批准后登记收付款；
// 「锁定后补录」表列出类型（金额调整／收付款补录）与收付方式。
// 186-R2（控制席并入，同根；BSM:1323「提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补」）：应收台账逐行另可「申请退款补录」
// （金额、原因；收付方式由后端照原收款记录），上级批准后按原收款原路退款；退款只退应收（同退款路径），其余方向不出。
// 第十八批 AL 段第 245 条 245-R2（ENUM:190「日结锁定后的补录须上级审批」；BSM:1642「补录一律进入主管复核队列，复核通过才生效」；PGM:528「发起单据时只需
// 指定该事项的最终审批人」）：补录提交后进主管复核，审批通过才生效，本区块不再批准、驳回；每行可填最终审批人（不填默认直接上级），「锁定后补录」表复核单号
// 链到审批链页。第 253 条（BSM:1642「必须写原因和责任人……补录必须附原始记录凭证」；第十八批裁决 56「三种锁账补录提交时必须上传原始凭证（银行回单、转账截图、
// 异议处理结论等）并写责任人」）：每行三种申请共用一组原始凭证上传与责任人，缺一不弹确认、不提交；须同时持补录申请与凭证上传权限才出申请；补录表列出责任人与凭证。
type LockedItem = { id: string; itemNo?: string; direction?: string; amountCent?: number; dueDate?: string };
type Adjustment = { id: string; itemNo?: string; adjustmentAmountCent?: number; reason?: string; requestedBy?: string; createdAt?: string;
  approvalStatus?: string; approvedBy?: string | null; decidedAt?: string | null; originalBusinessDate?: string | null;
  kind?: string; paymentMethod?: string | null; reviewId?: string | null; evidenceAttachmentIds?: string[]; responsibleStaffId?: string | null };
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
// 186-R1：补录类型与收付方式中文（收付方式闭集同后端 FinanceService.OFFLINE_PAYMENT_METHODS）。
const kindLabel = (code?: string) => KEY_VALUE_LABELS.kind?.[code ?? ""] ?? code ?? "—";
const methodLabel = (code?: string | null) => (code ? KEY_VALUE_LABELS.payment_method?.[code] ?? code : "—");
const PAYMENT_METHODS = Object.entries(KEY_VALUE_LABELS.payment_method ?? {});
const EMPTY_PAY = { amount: "", method: "", reason: "" };
// 245-R2、253：每行三种申请共用的原始凭证（已上传的附件编号与文件名）、责任人与最终审批人。
const EMPTY_SHARED = { proofs: [] as { id: string; name: string }[], responsible: "", finalApprover: "" };
const PROOF_ACCEPT = "image/*,.pdf";
const PROOF_HINT = "请上传原始凭证（银行回单、转账截图、异议处理结论等）";

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
  const canRequest = hasPermissions(effectivePermissions, ["finance.locked_backfill.request", "evidence.attachment.create"]);
  const [lockedItems, setLockedItems] = React.useState<LockedItem[]>([]);
  const [adjustments, setAdjustments] = React.useState<Adjustment[]>([]);
  const [drafts, setDrafts] = React.useState<Record<string, { amount: string; reason: string }>>({});
  const [payDrafts, setPayDrafts] = React.useState<Record<string, typeof EMPTY_PAY>>({});
  const [refundDrafts, setRefundDrafts] = React.useState<Record<string, { amount: string; reason: string }>>({});
  const [shared, setShared] = React.useState<Record<string, typeof EMPTY_SHARED>>({});
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
      const body = { businessDate, scopeType, scopeId: chosenScope };
      const slot = `generate:${JSON.stringify(body)}`;
      const saved = await api.post<DailyClose>("/api/v1/finance/daily-closes", { ...body, idempotencyKey: keyFor(slot) });
      delete keys.current[slot]; list.reload(); setRefresh((n) => n + 1); onGenerated?.();
      await loadSummaries(saved.id);
    });
  }
  // 143-R4：申请补录（调整额按元填、可为负，原因必填）；第十八批 AL 段 245-R2 起审批在主管复核。
  function backfillReady(item: LockedItem) {
    const draft = drafts[item.id];
    const cent = Math.round(Number(draft?.amount) * 100);
    if (draft?.reason.trim() && Number.isFinite(cent) && cent !== 0) return proofReady(item);
    setError("请填写调整额（元，不为零，可为负）与补录原因"); return false;
  }
  // 253：三种申请都须已上传原始凭证并写责任人（第十八批裁决 56）。
  function proofReady(item: LockedItem) {
    const block = shared[item.id] ?? EMPTY_SHARED;
    if (!block.proofs.length) { setError(PROOF_HINT); return false; }
    if (!block.responsible.trim()) { setError("请填写责任人"); return false; }
    return true;
  }
  function editShared(id: string, patch: (current: typeof EMPTY_SHARED) => Partial<typeof EMPTY_SHARED>) {
    setShared((all) => { const current = all[id] ?? EMPTY_SHARED; return { ...all, [id]: { ...current, ...patch(current) } }; });
  }
  // 253-R1：凭证走证据上传接口（业务类型 finance_locked_backfill），每传成一份即记一份。
  async function uploadProofs(item: LockedItem, files: FileList | null) {
    const picked = Array.from(files ?? []);
    if (!picked.length) return;
    await run(async () => {
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      for (const file of picked) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", "finance_locked_backfill");
        const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
        editShared(item.id, (current) => ({ proofs: [...current.proofs, { id: saved.id, name: saved.fileName ?? file.name }] }));
      }
    });
  }
  // 245-R2：提交即进主管复核；最终审批人填了才带。253：带原始凭证与责任人。成功后清空该行。
  async function submitBackfill(item: LockedItem, body: Record<string, unknown>, reset: () => void) {
    const block = shared[item.id] ?? EMPTY_SHARED;
    const full = { ...body, evidenceAttachmentIds: block.proofs.map((proof) => proof.id), responsibleStaffId: block.responsible.trim(),
      ...(block.finalApprover.trim() ? { finalApproverId: block.finalApprover.trim() } : {}) };
    await run(async () => {
      const slot = `backfill:${JSON.stringify(full)}`;
      await api.post("/api/v1/finance/ar-ap-adjustments", { ...full, idempotencyKey: keyFor(slot) });
      delete keys.current[slot]; reset(); setShared((all) => ({ ...all, [item.id]: EMPTY_SHARED })); setBackfillRefresh((n) => n + 1);
    });
  }
  async function requestBackfill(item: LockedItem) {
    const draft = drafts[item.id] ?? { amount: "", reason: "" };
    await submitBackfill(item, { arApItemId: item.id, adjustmentAmountCent: Math.round(Number(draft.amount) * 100), reason: draft.reason.trim() },
      () => setDrafts((all) => ({ ...all, [item.id]: { amount: "", reason: "" } })));
  }
  // 186-R1：申请收付款补录（金额按元填、须大于零，收付方式与原因必填）。
  function paymentBackfillReady(item: LockedItem) {
    const draft = payDrafts[item.id];
    const cent = Math.round(Number(draft?.amount) * 100);
    if (draft?.reason.trim() && draft.method && Number.isFinite(cent) && cent > 0) return proofReady(item);
    setError("请填写收付金额（元，大于零）、收付方式与补录原因"); return false;
  }
  async function requestPaymentBackfill(item: LockedItem) {
    const draft = payDrafts[item.id] ?? EMPTY_PAY;
    await submitBackfill(item, { kind: "payment", arApItemId: item.id, adjustmentAmountCent: Math.round(Number(draft.amount) * 100),
      paymentMethod: draft.method, reason: draft.reason.trim() }, () => setPayDrafts((all) => ({ ...all, [item.id]: EMPTY_PAY })));
  }
  // 186-R2：申请退款补录（金额按元填、须大于零，原因必填）。
  function refundBackfillReady(item: LockedItem) {
    const draft = refundDrafts[item.id];
    const cent = Math.round(Number(draft?.amount) * 100);
    if (draft?.reason.trim() && Number.isFinite(cent) && cent > 0) return proofReady(item);
    setError("请填写退款金额（元，大于零）与补录原因"); return false;
  }
  async function requestRefundBackfill(item: LockedItem) {
    const draft = refundDrafts[item.id] ?? { amount: "", reason: "" };
    await submitBackfill(item, { kind: "refund", arApItemId: item.id, adjustmentAmountCent: Math.round(Number(draft.amount) * 100),
      reason: draft.reason.trim() }, () => setRefundDrafts((all) => ({ ...all, [item.id]: { amount: "", reason: "" } })));
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
        <p className="admin-muted-action">提交后进主管复核，审批通过才生效。</p>
        <table className="admin-table" data-testid="daily-close-locked-items">
          <thead><tr><th>单号</th><th>方向</th><th>金额（元）</th><th>到期日</th>{canRequest ? <><th>原始凭证与责任人</th><th>申请补录</th><th>申请收付款补录</th><th>申请退款补录</th></> : null}</tr></thead>
          <tbody>{lockedItems.map((item) => {
            const draft = drafts[item.id] ?? { amount: "", reason: "" };
            const edit = (patch: Partial<typeof draft>) => setDrafts((all) => ({ ...all, [item.id]: { ...draft, ...patch } }));
            const pay = payDrafts[item.id] ?? EMPTY_PAY;
            const editPay = (patch: Partial<typeof pay>) => setPayDrafts((all) => ({ ...all, [item.id]: { ...pay, ...patch } }));
            const back = refundDrafts[item.id] ?? { amount: "", reason: "" };
            const editBack = (patch: Partial<typeof back>) => setRefundDrafts((all) => ({ ...all, [item.id]: { ...back, ...patch } }));
            const block = shared[item.id] ?? EMPTY_SHARED;
            return <tr key={item.id}>
              <td>{item.itemNo ?? item.id}</td><td>{directionLabel(item.direction)}</td><td>{yuan(item.amountCent)}</td><td>{cnDate(item.dueDate)}</td>
              {canRequest ? <td>
                <label>原始凭证（银行回单、转账截图、异议处理结论等）<input type="file" multiple accept={PROOF_ACCEPT} disabled={busy}
                  data-testid={`daily-close-backfill-proof-${item.id}`} onChange={(e) => { void uploadProofs(item, e.target.files); e.target.value = ""; }} /></label>
                <ul data-testid={`daily-close-backfill-proofs-${item.id}`}>{block.proofs.map((proof) => <li key={proof.id}>{proof.name}（已上传）</li>)}</ul>
                <input aria-label="责任人账号" data-testid={`daily-close-backfill-responsible-${item.id}`} value={block.responsible}
                  onChange={(e) => { const value = e.target.value; editShared(item.id, () => ({ responsible: value })); }} />
                <input aria-label="最终审批人（可不填，默认直接上级）" data-testid={`daily-close-backfill-final-approver-${item.id}`} value={block.finalApprover}
                  onChange={(e) => { const value = e.target.value; editShared(item.id, () => ({ finalApprover: value })); }} />
              </td> : null}
              {canRequest ? <td>
                <input aria-label="调整额（元）" data-testid={`daily-close-backfill-amount-${item.id}`} value={draft.amount} onChange={(e) => edit({ amount: e.target.value })} />
                <input aria-label="补录原因" data-testid={`daily-close-backfill-reason-${item.id}`} value={draft.reason} onChange={(e) => edit({ reason: e.target.value })} />
                <HighRiskConfirm label="申请补录" impact={LOCK_IMPACT} disabled={busy} precheck={() => backfillReady(item)}
                  onConfirm={() => void requestBackfill(item)} data-testid={`daily-close-backfill-${item.id}`} />
              </td> : null}
              {canRequest ? <td>
                <input aria-label="收付金额（元）" data-testid={`daily-close-payment-backfill-amount-${item.id}`} value={pay.amount} onChange={(e) => editPay({ amount: e.target.value })} />
                <select aria-label="收付方式" data-testid={`daily-close-payment-backfill-method-${item.id}`} value={pay.method} onChange={(e) => editPay({ method: e.target.value })}>
                  <option value="">请选择收付方式</option>
                  {PAYMENT_METHODS.map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                </select>
                <input aria-label="收付款补录原因" data-testid={`daily-close-payment-backfill-reason-${item.id}`} value={pay.reason} onChange={(e) => editPay({ reason: e.target.value })} />
                <HighRiskConfirm label="申请收付款补录" impact={LOCK_IMPACT} disabled={busy} precheck={() => paymentBackfillReady(item)}
                  onConfirm={() => void requestPaymentBackfill(item)} data-testid={`daily-close-payment-backfill-${item.id}`} />
              </td> : null}
              {canRequest ? <td>{item.direction === "receivable" ? <>
                <input aria-label="退款金额（元）" data-testid={`daily-close-refund-backfill-amount-${item.id}`} value={back.amount} onChange={(e) => editBack({ amount: e.target.value })} />
                <input aria-label="退款补录原因" data-testid={`daily-close-refund-backfill-reason-${item.id}`} value={back.reason} onChange={(e) => editBack({ reason: e.target.value })} />
                <HighRiskConfirm label="申请退款补录" impact={LOCK_IMPACT} disabled={busy} precheck={() => refundBackfillReady(item)}
                  onConfirm={() => void requestRefundBackfill(item)} data-testid={`daily-close-refund-backfill-${item.id}`} />
              </> : "—"}</td> : null}
            </tr>;
          })}</tbody>
        </table>
      </> : null}
      <div className="admin-section-title">锁定后补录：{lockedOpen}</div>
      <table className="admin-table" data-testid="daily-close-adjustments">
        <thead><tr><th>台账单号</th><th>类型</th><th>调整额（元）</th><th>收付方式</th><th>原因</th><th>补录人</th><th>申请时间</th><th>状态</th><th>批准人</th><th>批准时间</th><th>原营业日</th><th>责任人</th><th>凭证</th><th>复核单号</th></tr></thead>
        <tbody>{adjustments.map((row) => <tr key={row.id}>
          <td>{row.itemNo ?? "—"}</td><td>{kindLabel(row.kind)}</td><td>{yuan(row.adjustmentAmountCent)}</td><td>{methodLabel(row.paymentMethod)}</td><td>{row.reason ?? "—"}</td><td>{row.requestedBy ?? "—"}</td>
          <td>{cnDate(row.createdAt)}</td><td>{formatBusinessStatus(row.approvalStatus)}</td><td>{row.approvedBy ?? "—"}</td>
          <td>{cnDate(row.decidedAt)}</td><td>{cnDate(row.originalBusinessDate)}</td><td>{row.responsibleStaffId ?? "—"}</td>
          <td>{row.evidenceAttachmentIds?.length ? row.evidenceAttachmentIds.map((id, index) => <a key={id} data-testid={`daily-close-adjustment-proof-${row.id}-${index}`}
            href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">凭证{index + 1} </a>) : "—"}</td>
          <td>{row.reviewId ? <a data-testid={`daily-close-adjustment-review-${row.id}`}
            href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>{row.reviewId}</a> : "—"}</td>
        </tr>)}</tbody>
      </table>
    </div> : null}
  </section>;
}
