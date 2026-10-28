// 文件版本：V0.02
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps } from "../types";

type Option = { id: string; name?: string; itemName?: string; entityName?: string; supplierName?: string; merchantName?: string; unit?: string; projectId?: string; merchantId?: string; businessEntityId?: string; status?: string };
type Contract = { id: string; contractNo: string; supplierId: string; projectId: string; merchantId?: string; contractMode: string; purchasableScope: string[]; approvalStatus: string; status: string; validFrom: string; validTo: string; summary?: string };
type Terms = { purchaserEntityId: string; settlementMethod: string; priceBasis: string; unitPrices?: Record<string, number>; dueAnchor: string; termDays?: number; fixedDueDate?: string; signedOn?: string; prepaymentAmountCent?: number; consignmentSaleBasis?: string; prepaymentRemainderTerms?: string };
type Purchase = { id: string; requestNo: string; supplierId: string; itemId: string; quantity: number; status: string; supplyContractId: string; expectedArrivalDate?: string };
type Receipt = { id: string; arrivedQuantity: number; acceptedQuantity: number; rejectedQuantity: number; receivedAt: string; warehouseId: string; locationId: string; unitCostCent?: number; payableId?: string };
type Resolution = { id: string; resolutionType: string; quantity: number; reason: string; status: string; reviewId?: string; expectedArrivalDate?: string };
type Sale = { id: string; receiptId: string; quantity: number; status: string; requestedBy: string; sourceReference: string; soldAt: string; payableId?: string };
type Return = { id: string; receiptId: string; quantity: number; status: string; accountingStatus: string; reviewId?: string; requestedBy: string; reason: string; responsibilityTerms: string; confirmedAmountCent?: number; actualCostCent?: number; costDifferenceCent?: number; payableReductionCent?: number; supplierRefundItemId?: string };
type Payable = { id: string; itemNo: string; sourceType: string; sourceId: string; direction: string; isCurrent: boolean; lockedDayCloseId?: string; amountCent: number; paidAmountCent: number; unpaidAmountCent: number; dueDate?: string; status: string; adjustmentId?: string };
type Allocation = { id: string; sourcePaymentId: string; targetItemId: string; amountCent: number; allocatedAt: string };
type PrepaymentResolution = { id: string; resolutionType: string; sourcePaymentId?: string; amountCent: number; refundDueDate?: string; contractBasis: string; reason: string; reviewId?: string; status: string; requestedBy: string; postedAt?: string; supplierRefundItemId?: string; originalItemId?: string; reversalItemId?: string; replacementItemId?: string };
type Payment = { id: string; amountCent: number; bankFlowRef?: string };
type Detail = { purchase: Purchase; projectId: string; reviewId?: string; acceptedQuantity: number; terminatedQuantity: number; remainingQuantity: number; receipts: Receipt[]; resolutions: Resolution[]; consignmentSales: Sale[]; returns: Return[]; payables?: Payable[]; allocations?: Allocation[]; settlementTerms?: Terms; settlementMethod?: string; consignmentSaleBasis?: string; prepaymentCashPaidCent?: number; prepaymentAllocatedCent?: number; prepaymentResolutions?: PrepaymentResolution[]; prepaymentRefundReservedCent?: number; prepaymentPayments?: Payment[] };
// 第十八批第 33 条：智能采购建议（GET /api/v1/inventory/purchase-suggestions，只读即时计算）。
type Suggestion = { projectId: string; merchantId?: string | null; itemId: string; itemName: string; suggestedQuantity: number; reason: string };
type Options = { projects: Option[]; entities: Option[]; merchants: Option[]; suppliers: Option[]; inventoryItems: Option[]; warehouses: Option[]; contracts: Contract[] };
type Action = "contract-create" | "terms" | "contract-approve" | "purchase-create" | "approve" | "order" | "arrive" | "receive" | "resolution" | "sale" | "sale-confirm" | "allocate" | "return" | "handover" | "finance-review" | "prepayment-resolution" | "prepayment-post";
const VIEW = "inventory.purchase_request.view", MANAGE = "inventory.purchase_request.manage", CONTRACT = "inventory.supply_contract.manage", FINANCE = "finance.ar_ap.manage", EVIDENCE = "evidence.attachment.create";
const actionNames: Record<Action, string> = { "contract-create": "新建供货合同", terms: "登记合同执行条款", "contract-approve": "发起合同审批", "purchase-create": "新建采购申请", approve: "发起采购审批", order: "登记已向供应商下单", arrive: "登记到货", receive: "登记本次验收", resolution: "处理未交及拒收余量", sale: "登记寄售实际售出", "sale-confirm": "复核寄售售出", allocate: "分配已付预付款", return: "申请退供应商", handover: "登记退供实际交接", "finance-review": "复核退供账款", "prepayment-resolution": "处理未用预付余额", "prepayment-post": "登记已批准预付调整" };
const statusNames: Record<string, string> = { created: "待提交", submitted: "审批中", approved: "已批准", rejected: "已驳回", ordered: "已下单", expected: "待到货", arrived: "交付处理中", received: "数量处理完成", draft: "草稿", active: "有效", inactive: "停用", pending_approval: "审批中", recorded: "已记录", completed: "已完成", pending_confirmation: "待不同人员复核", confirmed: "已确认", handed_over: "实物已交接", pending_review: "待财务复核", posted: "账款已登记", settled: "已结清", partial_paid: "部分结清", pending: "未结清", open: "未结清", overdue: "已逾期", unpaid: "未结清" };
const methods: Record<string, string> = { cash_on_delivery: "货到付款", monthly: "月结", credit_term: "合同账期", prepaid: "预付款", consignment: "寄售" };
const anchors: Record<string, string> = { contract_signed: "合同签约", ordered: "采购下单", each_receipt: "每次合格接收", all_fulfilled: "全部数量处理完", month_end: "月末", fixed_date: "合同固定日期", confirmed_sale: "寄售售出确认" };
const sourceNames: Record<string, string> = { purchase_prepayment: "采购预付款", purchase_inbound: "合格入库货款", purchase_consignment: "寄售结算", purchase_return_refund: "供应商应退货款", purchase_prepayment_refund: "供应商应退预付款" };
const emptyOptions: Options = { projects: [], entities: [], merchants: [], suppliers: [], inventoryItems: [], warehouses: [], contracts: [] };
const money = (v?: number | null) => typeof v === "number" ? `¥${(v / 100).toFixed(2)}` : "未提供金额或无查看权限";
const status = (v: string) => statusNames[v] ?? "状态待核实";
const date = (v?: string) => v ? new Date(v).toLocaleString("zh-CN") : "尚未记录";
const name = (v: Option) => v.name ?? v.itemName ?? v.entityName ?? v.supplierName ?? v.merchantName ?? v.id;
const enc = encodeURIComponent;

export function PurchaseWorkbenchPage({ api, user, effectivePermissions, setActionResult, setGlobalError, navigateTo, focusObjectId }: AdminPageProps) {
  const can = (permissions: string[]) => hasPermissions(effectivePermissions, permissions);
  const canView = can([VIEW]), canContract = can([CONTRACT]);
  const [options, setOptions] = React.useState<Options>(emptyOptions);
  const [purchases, setPurchases] = React.useState<Purchase[]>([]);
  const [detail, setDetail] = React.useState<Detail | null>(null);
  const [contract, setContract] = React.useState<{ contract: Contract; reviewId?: string; terms?: Terms } | null>(null);
  const [payments, setPayments] = React.useState<Payment[]>([]);
  const [loadErrors, setLoadErrors] = React.useState<Record<string, string>>({});
  const [error, setError] = React.useState("");
  const [notice, setNotice] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const busyRef = React.useRef(false), selection = React.useRef(0);
  const [action, setAction] = React.useState<Action | null>(null);
  const [target, setTarget] = React.useState("");
  const [form, setForm] = React.useState<Record<string, string>>({});
  const [scope, setScope] = React.useState<string[]>([]);
  const [prices, setPrices] = React.useState<Record<string, string>>({});
  const [proofs, setProofs] = React.useState<{ id: string; name: string }[]>([]);
  const [suggestionProject, setSuggestionProject] = React.useState("");
  const [suggestions, setSuggestions] = React.useState<Suggestion[]>([]);
  const request = React.useRef<{ fingerprint: string; key: string } | null>(null);
  const set = (field: string, value: string) => setForm(v => ({ ...v, [field]: value }));
  const itemName = (id: string) => { const row = options.inventoryItems.find(x => x.id === id); return row ? `${name(row)}（${row.unit ?? "未配置单位"}）` : id; };
  const supplierName = (id: string) => options.suppliers.find(x => x.id === id)?.supplierName ?? options.suppliers.find(x => x.id === id)?.name ?? id;
  const approvalLink = (id?: string) => id && <button type="button" disabled={!navigateTo || busy} onClick={() => navigateTo?.("ADM-GOV-009", id)}>查看审批原件</button>;
  const financeLink = (id: string) => <button type="button" disabled={!navigateTo || !can(["finance.ar_ap.view"])} onClick={() => navigateTo?.("ADM-FIN-001", id)}>查看并办理账款</button>;

  const load = React.useCallback(async () => {
    const errors: Record<string, string> = {};
    await Promise.all([
      api.get<Options>("/api/v1/inventory/purchase-workbench-options").then(v => setOptions({ ...emptyOptions, ...v })).catch(e => { setOptions(emptyOptions); errors.options = `可选合同、商品与仓库读取失败：${e.message}`; }),
      canView ? api.get<Purchase[]>("/api/v1/inventory/purchase-requests").then(setPurchases).catch(e => { setPurchases([]); errors.purchases = `采购记录读取失败：${e.message}`; }) : Promise.resolve(),
    ]);
    setLoadErrors(errors);
  }, [api, canView]);
  React.useEffect(() => { void load(); }, [load]);
  async function readPurchase(id: string, clearForm = true) {
    const seq = ++selection.current; if (clearForm) { setAction(null); setProofs([]); } setDetail(null); setPayments([]); setError("");
    try {
      const result = await api.get<Detail>(`/api/v1/inventory/purchase-requests/${enc(id)}`);
      if (seq === selection.current) setDetail(result);
    } catch (e) { if (seq === selection.current) setError(e instanceof Error ? e.message : "采购明细读取失败"); }
  }
  React.useEffect(() => { if (focusObjectId && canView) void readPurchase(focusObjectId); }, [focusObjectId, canView]);
  async function readContract(id: string) {
    const seq = ++selection.current; setAction(null); setProofs([]); setContract(null); setError("");
    try { const result = await api.get<{ contract: Contract; reviewId?: string; terms?: Terms }>(`/api/v1/inventory/supply-contracts/${enc(id)}/terms`); if (seq === selection.current) setContract(result); }
    catch (e) { if (seq === selection.current) setError(e instanceof Error ? e.message : "合同原件读取失败"); }
  }
  async function readSuggestions(id: string) {
    setSuggestionProject(id); setSuggestions([]); setError("");
    if (!id) return;
    try { setSuggestions(await api.get<Suggestion[]>(`/api/v1/inventory/purchase-suggestions?project_id=${enc(id)}`)); }
    catch (e) { setError(`采购建议读取失败：${e instanceof Error ? e.message : "请稍后重试"}`); }
  }
  // BSM:1211「生成建议采购数量及理由，由采购人员人工调整并按审批链批准后生成采购单」：只预填现有建单表单，由采购人员改后提交，不自动建单或下单。
  function purchaseFromSuggestion(s: Suggestion, c: Contract) {
    open("purchase-create");
    setForm(v => ({ ...v, supplyContractId: c.id, itemId: s.itemId, quantity: String(s.suggestedQuantity), reason: s.reason }));
  }
  function permissions(a: Action): string[] {
    if (a.startsWith("contract") || a === "terms") return [CONTRACT];
    if (a === "sale-confirm") return ["inventory.purchase.approve", FINANCE];
    return [a === "allocate" || a === "finance-review" || a === "prepayment-post" ? FINANCE : MANAGE];
  }
  function open(a: Action, id = "") {
    ++selection.current;
    setAction(a); setTarget(id); setError(""); setNotice(""); setProofs([]); request.current = null; setPayments([]);
    const initial: Record<string, string> = {};
    if (a === "contract-create") initial.contractMode = "platform_self_use";
    if (a === "terms" && contract?.terms) {
      Object.entries(contract.terms).forEach(([k, v]) => { if (typeof v === "string" || typeof v === "number") initial[k] = String(v); });
      setPrices(Object.fromEntries(Object.entries(contract.terms.unitPrices ?? {}).map(([k, v]) => [k, String(v)])));
    } else setPrices({});
    if (a === "terms") setScope(contract?.contract.purchasableScope ?? []); else setScope([]);
    if (a === "resolution") initial.resolutionType = "replenishment";
    if (a === "prepayment-resolution") initial.resolutionType = "refund_unused";
    if (a === "sale") initial.saleBasis = detail?.consignmentSaleBasis ?? "";
    if (a === "sale-confirm") initial.decision = "confirmed";
    setForm(initial);
    if (a === "allocate" || a === "prepayment-resolution") setPayments(detail?.prepaymentPayments ?? []);
  }
  const actionButton = (a: Action, id = "", disabled = false) => <PermissionButton permissionsState={effectivePermissions} permissions={permissions(a)} disabled={busy || disabled} onClick={() => open(a, id)}>{actionNames[a]}</PermissionButton>;
  const proofType = action === "prepayment-post" ? "purchase_prepayment_resolution" : action === "terms" ? "supply_contract" : action === "handover" || action === "finance-review" ? "purchase_return" : "inventory_purchase_request";
  const proofId = action === "prepayment-post" ? target : action === "terms" ? contract?.contract.id : action === "handover" || action === "finance-review" ? target : detail?.purchase.id;
  const needsProof = action !== null && !["contract-create", "contract-approve", "purchase-create", "order", "arrive"].includes(action);
  async function upload(files: FileList | null) {
    if (!files?.length || busyRef.current) return;
    if (!proofId || !api.postForm || !action || !can([...permissions(action), EVIDENCE])) { setError("当前无真实凭证上传权限，或尚未选择业务原件"); return; }
    busyRef.current = true; setBusy(true); setError("");
    try {
      for (const file of Array.from(files)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", proofType); data.append("business_id", proofId); data.append("attachment_role", "other");
        const result = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setProofs(v => [...v, { id: result.id, name: file.name }]);
      }
    } catch (e) { setError(e instanceof Error ? e.message : "凭证上传失败，已上传成功的文件已保留"); }
    finally { busyRef.current = false; setBusy(false); }
  }
  function required(key: string, label: string) { const v = form[key]?.trim(); if (!v) throw new Error(`请填写${label}`); return v; }
  function integer(key: string, label: string, minimum = 0) { const n = Number(required(key, label)); if (!Number.isSafeInteger(n) || n < minimum) throw new Error(`${label}须为不小于 ${minimum} 的整数`); return n; }
  function instant(key: string, label: string) { const v = new Date(required(key, label)); if (!Number.isFinite(v.getTime())) throw new Error(`${label}无效`); return v.toISOString(); }
  function approval(lines: string[] = []) { return { finalApproverId: required("finalApproverId", "主线最终审批人"), beneficiaryId: form.beneficiaryId?.trim() || null, parallelLines: lines.map(lineCode => ({ lineCode, finalApproverId: required(lineCode, `${lineCode === "business_review" ? "业务" : lineCode === "finance_review" ? "财务" : "法务"}线最终审批人`) })) }; }
  async function submit() {
    if (!action || busyRef.current || !can(permissions(action))) return;
    setError(""); setNotice("");
    try {
      const purchaseId = detail?.purchase.id, purchasePath = `/api/v1/inventory/purchase-requests/${enc(purchaseId ?? "")}`;
      let path = "", body: Record<string, unknown> = {};
      if (needsProof && !proofs.length) throw new Error("请先上传本次真实凭证");
      switch (action) {
        case "contract-create":
          if (!scope.length) throw new Error("请勾选合同允许采购的商品");
          path = "/api/v1/inventory/supply-contracts"; body = { supplierId: required("supplierId", "合同供应商"), projectId: required("projectId", "合同综合体"), merchantId: form.contractMode === "merchant_self_purchase" ? required("merchantId", "自采商户") : null, contractMode: form.contractMode, supplyScope: required("supplyScope", "供货范围"), priceTerms: required("priceTerms", "原合同价格约定"), validFrom: required("validFrom", "合同开始日"), validTo: required("validTo", "合同结束日"), summary: required("summary", "合同摘要"), purchasableScope: scope }; break;
        case "terms": {
          const terms: Terms = { purchaserEntityId: required("purchaserEntityId", "我方采购主体"), settlementMethod: required("settlementMethod", "结算方式"), priceBasis: required("priceBasis", "计价依据"), dueAnchor: required("dueAnchor", "到期起算事件") };
          if (terms.priceBasis === "contract_unit_price") terms.unitPrices = Object.fromEntries(scope.map(id => { const raw = prices[id]; const v = Number(raw); if (!raw?.trim() || !Number.isSafeInteger(v) || v < 0) throw new Error(`请完整填写${itemName(id)}的合同单价（整数分）`); return [id, v]; }));
          if (terms.dueAnchor === "fixed_date") terms.fixedDueDate = required("fixedDueDate", "合同付款日期"); else terms.termDays = integer("termDays", "起算后天数");
          if (terms.dueAnchor === "contract_signed") terms.signedOn = required("signedOn", "实际签约日");
          if (terms.settlementMethod === "prepaid") { terms.prepaymentAmountCent = integer("prepaymentAmountCent", "本采购预付金额", 1); terms.prepaymentRemainderTerms = required("prepaymentRemainderTerms", "未用预付余额处理约定"); }
          if (terms.settlementMethod === "consignment") terms.consignmentSaleBasis = required("consignmentSaleBasis", "售出确认依据");
          path = `/api/v1/inventory/supply-contracts/${enc(contract!.contract.id)}/terms`; body = { terms }; break;
        }
        case "contract-approve": path = `/api/v1/inventory/supply-contracts/${enc(contract!.contract.id)}/approve`; body = { decision: "approved", reason: required("reason", "申请依据"), ...approval(["business_review", "finance_review", "legal_review"]) }; break;
        case "purchase-create": {
          const c = options.contracts.find(c => c.id === required("supplyContractId", "已批准供货合同"));
          const itemId = required("itemId", "采购商品"); if (!c || !c.purchasableScope.includes(itemId)) throw new Error("请选择合同允许采购的商品");
          path = "/api/v1/inventory/purchase-requests"; body = { supplierId: c.supplierId, supplyContractId: c.id, items: [{ item_id: itemId, quantity: integer("quantity", "采购数量", 1) }], expectedArrivalDate: required("expectedArrivalDate", "预计到货日"), operatorNote: required("reason", "申请依据") }; break;
        }
        case "approve": path = `${purchasePath}/approve`; body = { decision: "approved", supplyContractId: detail!.purchase.supplyContractId, note: required("reason", "申请依据"), ...approval() }; break;
        case "order": case "arrive": path = `${purchasePath}/${action}`; body = { note: required("reason", "实际进度说明") }; break;
        case "receive": {
          const arrivedQuantity = integer("arrivedQuantity", "本次实到数量", 1), acceptedQuantity = integer("acceptedQuantity", "本次合格接收数量"), rejectedQuantity = integer("rejectedQuantity", "本次拒收数量");
          if (acceptedQuantity + rejectedQuantity !== arrivedQuantity) throw new Error("合格接收数量与拒收数量之和必须等于本次实到数量");
          if (arrivedQuantity > detail!.remainingQuantity) throw new Error("本次实到数量不能超过尚未处理余量");
          path = `${purchasePath}/receive`; body = { arrivedQuantity, acceptedQuantity, rejectedQuantity, unitCostCent: integer("unitCostCent", "实际到货单价"), warehouseId: required("warehouseId", "接收仓库"), locationId: required("locationId", "实际库位编号"), receivedAt: instant("receivedAt", "实际验收时间"), note: required("reason", "验收说明") }; break;
        }
        case "resolution": path = `${purchasePath}/resolutions`; body = { resolutionType: form.resolutionType, quantity: integer("quantity", "余量处理数量", 1), reason: required("reason", "处理依据"), ...(form.resolutionType === "terminate" ? approval() : { expectedArrivalDate: required("expectedArrivalDate", "补货预计到货日") }) }; break;
        case "sale": path = `${purchasePath}/consignment-sales`; body = { receiptId: required("receiptId", "合格入库来源"), quantity: integer("quantity", "实际售出数量", 1), saleBasis: required("saleBasis", "合同售出确认依据"), sourceReference: required("sourceReference", "实际售出原始单据编号"), soldAt: instant("soldAt", "实际售出时间") }; break;
        case "sale-confirm": path = `/api/v1/inventory/purchase-consignment-sales/${enc(target)}/confirm`; body = { decision: form.decision, reason: required("reason", "复核依据") }; break;
        case "allocate": path = `${purchasePath}/prepayment-allocations`; body = { sourcePaymentId: required("sourcePaymentId", "实际预付款流水"), targetItemId: required("targetItemId", "本采购待付入库款"), amountCent: integer("amountCent", "本次冲抵金额", 1) }; break;
        case "prepayment-resolution": path = `${purchasePath}/prepayment-resolutions`; body = { resolutionType: form.resolutionType, amountCent: integer("amountCent", "预付余额处理金额", 1), contractBasis: required("contractBasis", "锁定合同余额处理依据"), reason: required("reason", "处理原因"), sourcePaymentId: form.resolutionType === "refund_unused" ? required("sourcePaymentId", "实际预付款流水") : null, refundDueDate: form.resolutionType === "refund_unused" ? required("refundDueDate", "双方确认的预付款退款到期日") : null, ...approval(["finance_review"]) }; break;
        case "prepayment-post": path = `/api/v1/inventory/purchase-prepayment-resolutions/${enc(target)}/post`; body = { reason: required("reason", "预付调整复核依据") }; break;
        case "return": path = `${purchasePath}/returns`; body = { receiptId: required("receiptId", "合格入库来源"), quantity: integer("quantity", "退供数量", 1), returnUnitPriceCent: integer("returnUnitPriceCent", "双方确认退货单价"), responsibilityTerms: required("responsibilityTerms", "责任及费用约定"), reason: required("reason", "退供原因"), consignmentSaleId: form.consignmentSaleId || null, ...approval(["finance_review"]) }; break;
        case "handover": path = `/api/v1/inventory/purchase-returns/${enc(target)}/handover`; body = { handedOverAt: instant("handedOverAt", "实际交接时间") }; break;
        case "finance-review": path = `/api/v1/inventory/purchase-returns/${enc(target)}/finance-review`; body = { supplierRefundDueDate: form.supplierRefundDueDate || null, reason: required("reason", "财务复核依据") }; break;
      }
      if (needsProof) body.evidenceAttachmentIds = proofs.map(p => p.id);
      const fingerprint = JSON.stringify([path, body]);
      if (request.current?.fingerprint !== fingerprint) request.current = { fingerprint, key: crypto.randomUUID() };
      busyRef.current = true; setBusy(true);
      const result = await api.post<{ id?: string }>(path, { ...body, idempotencyKey: request.current.key });
      setActionResult(result); setGlobalError(null); setAction(null); setProofs([]);
      setNotice(actionNames[action] + "已记录；以刷新后的实际业务状态为准。");
      await load();
      if (action === "contract-create" && result.id) await readContract(result.id);
      else if (action === "terms" || action === "contract-approve") await readContract(contract!.contract.id);
      else if (action === "purchase-create" && result.id) await readPurchase(result.id);
      else if (purchaseId) await readPurchase(purchaseId);
    } catch (e) { const message = e instanceof Error ? e.message : "操作失败，请核实原单据后重试"; setError(message); setGlobalError(message); }
    finally { busyRef.current = false; setBusy(false); }
  }

  const input = (key: string, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} step={type === "datetime-local" ? "1" : type === "number" ? "1" : undefined} value={form[key] ?? ""} onChange={e => set(key, e.target.value)} /></label>;
  const select = (key: string, label: string, rows: { id: string; label: string }[], onChange?: (id: string) => void) => <label>{label}<select aria-label={label} value={form[key] ?? ""} onChange={e => { set(key, e.target.value); onChange?.(e.target.value); }}><option value="">请选择</option>{rows.map(r => <option key={r.id} value={r.id}>{r.label}</option>)}</select>{!rows.length && <small>当前无可选项，请核对资料状态和操作范围。</small>}</label>;
  const choices = (rows: Option[]) => rows.map(x => ({ id: x.id, label: name(x) }));
  const enumSelect = (key: string, label: string, names: Record<string, string>) => select(key, label, Object.entries(names).map(([id, label]) => ({ id, label })));
  const approvalFields = (lines: string[] = []) => <>{input("finalApproverId", "主线最终审批人")}{lines.map(line => <React.Fragment key={line}>{input(line, `${line === "business_review" ? "业务" : line === "finance_review" ? "财务" : "法务"}线最终审批人`)}</React.Fragment>)}{input("beneficiaryId", "受益员工账号（如有）")}<p>填写真实账号；系统核验岗位、组织关系与各审批线，提交不代表审批通过。</p></>;
  const receiptSelect = () => select("receiptId", "合格入库来源", (detail?.receipts ?? []).filter(r => r.acceptedQuantity > 0).map(r => ({ id: r.id, label: `${date(r.receivedAt)}，合格 ${r.acceptedQuantity}，${r.id}` })));
  const selectedContract = options.contracts.find(c => c.id === form.supplyContractId);
  const usableContracts = options.contracts.filter(c => c.status === "active" && c.approvalStatus === "approved");
  const activeWarehouses = options.warehouses.filter(w => w.status === "active" && w.projectId === detail?.projectId);
  const draft = contract?.contract.approvalStatus === "draft" && !contract?.reviewId;
  const pendingDelivery = detail && ["ordered", "expected", "arrived"].includes(detail.purchase.status);

  return <section data-testid="purchase-workbench-page">
    <AdminPageHeader title="采购与退供工作台" description="按批准合同采购，逐次记录实到、合格和拒收；数量处理、实际付款和退供退款分别追踪。" />
    {Object.entries(loadErrors).map(([key, message]) => <p key={key} role="alert">{message}</p>)}{error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
    <button type="button" disabled={busy} onClick={() => void load()}>刷新采购目录</button>
    <section className="admin-panel"><h3>供货合同</h3>{actionButton("contract-create")}
      {!canContract && <p>当前账号不能维护合同；新建采购仍可选择授权范围内已批准合同。</p>}
      <table className="admin-table"><thead><tr><th>合同</th><th>供应商</th><th>有效期</th><th>状态</th><th>原件</th></tr></thead><tbody>{options.contracts.map(c => <tr key={c.id}><td>{c.contractNo}</td><td>{supplierName(c.supplierId)}</td><td>{c.validFrom} 至 {c.validTo}</td><td>{status(c.approvalStatus)}／{status(c.status)}</td><td><button type="button" disabled={!canContract || busy} onClick={() => void readContract(c.id)}>查看合同</button></td></tr>)}</tbody></table>
      {contract && <div><h4>合同 {contract.contract.contractNo}</h4><p>{contract.contract.summary}；可采购：{contract.contract.purchasableScope.map(itemName).join("、")}</p>{approvalLink(contract.reviewId)}
        {contract.terms ? <><p>结算：{methods[contract.terms.settlementMethod] ?? "待核实"}；到期：{anchors[contract.terms.dueAnchor] ?? "待核实"}{contract.terms.dueAnchor === "fixed_date" ? ` ${contract.terms.fixedDueDate}` : `后 ${contract.terms.termDays} 天`}；计价：{contract.terms.priceBasis === "contract_unit_price" ? "合同固定单价" : "合格接收实际单价"}</p>{Object.entries(contract.terms.unitPrices ?? {}).map(([id, value]) => <p key={id}>{itemName(id)}：{money(value)}</p>)}{contract.terms.prepaymentRemainderTerms && <p>预付余额约定：{contract.terms.prepaymentRemainderTerms}</p>}{contract.terms.consignmentSaleBasis && <p>寄售确认依据：{contract.terms.consignmentSaleBasis}</p>}</> : <p>执行条款尚未登记或当前无价格查看权限。</p>}
        {actionButton("terms", "", !draft)}{actionButton("contract-approve", "", !draft)}
      </div>}
    </section>
    {can([MANAGE]) && <section className="admin-panel"><h3>采购建议</h3>
      <label>采购建议综合体<select aria-label="采购建议综合体" value={suggestionProject} disabled={busy} onChange={e => void readSuggestions(e.target.value)}><option value="">请选择</option>{options.projects.map(p => <option key={p.id} value={p.id}>{name(p)}</option>)}</select></label>
      {suggestionProject && <table className="admin-table"><thead><tr><th>商品</th><th>货权</th><th>建议采购量</th><th>理由</th><th>操作</th></tr></thead><tbody>{suggestions.map(s => {
        const c = usableContracts.find(c => c.projectId === s.projectId && (c.merchantId ?? null) === (s.merchantId ?? null) && c.purchasableScope.includes(s.itemId));
        return <tr key={`${s.merchantId ?? ""}:${s.itemId}`}><td>{s.itemName}</td><td>{s.merchantId ? options.merchants.find(m => m.id === s.merchantId)?.merchantName ?? s.merchantId : "平台"}</td><td>{s.suggestedQuantity}</td><td>{s.reason}</td><td>{c ? <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} disabled={busy} onClick={() => purchaseFromSuggestion(s, c)}>按建议发起采购</PermissionButton> : "无可用已批准合同"}</td></tr>;
      })}</tbody></table>}
    </section>}
    <section className="admin-panel"><h3>采购申请与履约</h3>{actionButton("purchase-create")}{!canView && <p>无采购查看权限；不能读取采购原件。</p>}
      <table className="admin-table"><thead><tr><th>采购单</th><th>商品</th><th>供应商</th><th>原采购量</th><th>进度</th><th>操作</th></tr></thead><tbody>{purchases.map(p => <tr key={p.id}><td>{p.requestNo}</td><td>{itemName(p.itemId)}</td><td>{supplierName(p.supplierId)}</td><td>{p.quantity}</td><td>{status(p.status)}</td><td><button type="button" disabled={busy} onClick={() => void readPurchase(p.id)}>查看采购</button></td></tr>)}</tbody></table>
      {detail && <div><h4>采购 {detail.purchase.requestNo}</h4><p>原采购 {detail.purchase.quantity}；合格接收 {detail.acceptedQuantity}；批准终止 {detail.terminatedQuantity}；尚待处理 {detail.remainingQuantity}。数量处理完成不代表货款结清。</p>{approvalLink(detail.reviewId)}
        {actionButton("approve", "", !["created", "submitted"].includes(detail.purchase.status) || Boolean(detail.reviewId))}{actionButton("order", "", detail.purchase.status !== "approved")}{actionButton("arrive", "", !["ordered", "expected"].includes(detail.purchase.status))}{actionButton("receive", "", !pendingDelivery)}{actionButton("resolution", "", !pendingDelivery || detail.remainingQuantity <= 0)}
        <h4>逐次验收</h4><table className="admin-table"><thead><tr><th>实际时间</th><th>实到</th><th>合格接收</th><th>拒收</th><th>仓库／库位</th><th>实际到货单价</th></tr></thead><tbody>{detail.receipts.map(r => <tr key={r.id}><td>{date(r.receivedAt)}</td><td>{r.arrivedQuantity}</td><td>{r.acceptedQuantity}</td><td>{r.rejectedQuantity}</td><td>{options.warehouses.find(w => w.id === r.warehouseId)?.name ?? r.warehouseId}／{r.locationId}</td><td>{money(r.unitCostCent)}</td></tr>)}</tbody></table>
        <h4>余量处理</h4>{detail.resolutions.map(r => <p key={r.id}>{r.resolutionType === "terminate" ? "终止余量" : "跟进补货"} {r.quantity}；{status(r.status)}；{r.reason}{r.expectedArrivalDate && `；预计 ${r.expectedArrivalDate}`}{approvalLink(r.reviewId)}</p>)}
        <h4>寄售售出确认</h4>{actionButton("sale", "", detail.settlementMethod !== "consignment")}{!detail.settlementMethod && <p>结算条款未提供；如需登记寄售，请先核对合同与查看权限。</p>}
        {detail.consignmentSales.map(s => <div key={s.id}><p>原始单据 {s.sourceReference}；实际售出 {s.quantity}；{date(s.soldAt)}；{status(s.status)}</p>{actionButton("sale-confirm", s.id, s.status !== "pending_confirmation" || s.requestedBy === user.id)}{s.requestedBy === user.id && s.status === "pending_confirmation" && <small>须由不同人员复核。</small>}</div>)}
        <h4>退供应商</h4>{actionButton("return", "", !detail.receipts.some(r => r.acceptedQuantity > 0))}
        {detail.returns.map(r => <div key={r.id}><p>退供 {r.quantity}；实物：{status(r.status)}；财务：{status(r.accountingStatus)}。{r.reason}；责任约定：{r.responsibilityTerms}</p><p>确认退货金额 {money(r.confirmedAmountCent)}；实际出库成本 {money(r.actualCostCent)}；差额 {money(r.costDifferenceCent)}；冲减未付货款 {money(r.payableReductionCent)}</p>{approvalLink(r.reviewId)}{actionButton("handover", r.id, r.status !== "approved")}{actionButton("finance-review", r.id, r.status !== "handed_over" || r.accountingStatus !== "pending_review" || r.requestedBy === user.id)}{r.supplierRefundItemId && <p>供应商退款仍按实际回款结清。{financeLink(r.supplierRefundItemId)}</p>}</div>)}
        <h4>采购款项</h4>{actionButton("prepayment-resolution", "", detail.settlementMethod !== "prepaid" || !["ordered", "expected", "arrived", "received"].includes(detail.purchase.status))}{detail.payables ? <><p>预付款实际现金支付：{money(detail.prepaymentCashPaidCent)}；已分配冲抵：{money(detail.prepaymentAllocatedCent)}；退款方案占用：{money(detail.prepaymentRefundReservedCent)}。冲抵为非现金记录，不重复登记付款。</p>{actionButton("allocate", "", detail.settlementMethod !== "prepaid")}
          <table className="admin-table"><thead><tr><th>单据与来源</th><th>金额</th><th>已结（含冲抵）</th><th>未结</th><th>到期</th><th>状态</th></tr></thead><tbody>{detail.payables.map(p => <tr key={p.id}><td>{p.itemNo}／{sourceNames[p.sourceType] ?? "采购调整账款"}{financeLink(p.id)}</td><td>{money(p.amountCent)}</td><td>{money(p.paidAmountCent)}</td><td>{money(p.unpaidAmountCent)}</td><td>{p.dueDate ?? "按合同等待起算事实"}</td><td>{status(p.status)}{p.amountCent < 0 && "（红冲记录）"}</td></tr>)}</tbody></table>
          {detail.prepaymentResolutions?.map(r => <div key={r.id}><p>{r.resolutionType === "refund_unused" ? "收回未用实付预款" : "终止未付预付款"} {money(r.amountCent)}；{status(r.status)}；合同依据：{r.contractBasis}；{r.reason}{r.refundDueDate && `；退款到期日 ${r.refundDueDate}`}</p>{approvalLink(r.reviewId)}{actionButton("prepayment-post", r.id, r.status !== "approved" || r.requestedBy === user.id)}{r.supplierRefundItemId && <p>应收退款已登记，仍须核实实际到账。{financeLink(r.supplierRefundItemId)}</p>}{r.replacementItemId && <p>调整后的预付款账款：{financeLink(r.replacementItemId)}</p>}</div>)}
          {detail.allocations?.map(a => <p key={a.id}>非现金冲抵 {money(a.amountCent)}；原实付流水 {a.sourcePaymentId} → 应付 {a.targetItemId}；{date(a.allocatedAt)}</p>)}
        </> : <p>当前无本采购财务查看权限，款项并非零元或已结清。</p>}
      </div>}
    </section>
    {action && <fieldset className="admin-panel" disabled={busy}><legend>{actionNames[action]}</legend>
      {action === "contract-create" && <>{select("projectId", "合同综合体", choices(options.projects), () => set("merchantId", ""))}{select("supplierId", "合同供应商", choices(options.suppliers))}{enumSelect("contractMode", "采购模式", { platform_self_use: "平台自用", merchant_self_purchase: "商户自采", platform_purchase_distribution: "平台采购配送" })}{form.contractMode === "merchant_self_purchase" && select("merchantId", "自采商户", choices(options.merchants.filter(m => m.projectId === form.projectId)))}{input("validFrom", "合同开始日", "date")}{input("validTo", "合同结束日", "date")}{input("supplyScope", "供货范围")}{input("priceTerms", "原合同价格约定")}{input("summary", "合同摘要")}<fieldset><legend>合同允许采购商品</legend>{options.inventoryItems.map(item => <label key={item.id}><input type="checkbox" checked={scope.includes(item.id)} onChange={e => setScope(v => e.target.checked ? [...v, item.id] : v.filter(id => id !== item.id))} />{itemName(item.id)}</label>)}</fieldset><p>先创建真实合同编号，再上传原件和登记执行条款；本步骤不会自动批准合同。</p></>}
      {action === "terms" && <>{select("purchaserEntityId", "我方采购主体", choices(options.entities.filter(e => !e.projectId || e.projectId === contract?.contract.projectId)))}{enumSelect("settlementMethod", "结算方式", methods)}{enumSelect("priceBasis", "计价依据", { contract_unit_price: "合同固定单价", accepted_actual_unit_price: "合格接收实际单价" })}{form.priceBasis === "contract_unit_price" && scope.map(id => <label key={id}>{itemName(id)}合同单价（分）<input aria-label={`${itemName(id)}合同单价（分）`} type="number" min="0" step="1" value={prices[id] ?? ""} onChange={e => setPrices(v => ({ ...v, [id]: e.target.value }))} /></label>)}{enumSelect("dueAnchor", "到期起算事件", anchors)}{form.dueAnchor === "fixed_date" ? input("fixedDueDate", "合同付款日期", "date") : input("termDays", "起算后天数", "number")}{form.dueAnchor === "contract_signed" && input("signedOn", "实际签约日", "date")}{form.settlementMethod === "prepaid" && <>{input("prepaymentAmountCent", "本采购预付金额（分）", "number")}{input("prepaymentRemainderTerms", "未用预付余额处理约定")}</>}{form.settlementMethod === "consignment" && input("consignmentSaleBasis", "售出确认依据")}</>}
      {action === "contract-approve" && <>{input("reason", "申请依据")}{approvalFields(["business_review", "finance_review", "legal_review"])}</>}
      {action === "purchase-create" && <>{select("supplyContractId", "已批准供货合同", usableContracts.map(c => ({ id: c.id, label: `${c.contractNo}／${supplierName(c.supplierId)}` })), () => set("itemId", ""))}{selectedContract && <p>供应商：{supplierName(selectedContract.supplierId)}；合同有效期 {selectedContract.validFrom} 至 {selectedContract.validTo}</p>}{select("itemId", "采购商品", options.inventoryItems.filter(i => selectedContract?.purchasableScope.includes(i.id)).map(i => ({ id: i.id, label: itemName(i.id) })))}{input("quantity", "采购数量", "number")}{input("expectedArrivalDate", "预计到货日", "date")}{input("reason", "申请依据")}<p>一份采购申请只包含一个商品；其他商品分别建单。</p></>}
      {action === "approve" && <>{input("reason", "申请依据")}{approvalFields()}</>}
      {(action === "order" || action === "arrive") && input("reason", "实际进度说明")}
      {action === "receive" && <>{input("arrivedQuantity", "本次实到数量", "number")}{input("acceptedQuantity", "本次合格接收数量", "number")}{input("rejectedQuantity", "本次拒收数量", "number")}{input("unitCostCent", "实际到货单价（分）", "number")}{select("warehouseId", "接收仓库", choices(activeWarehouses))}{input("locationId", "实际库位编号")}{input("receivedAt", "实际验收时间（当前设备时区）", "datetime-local")}{input("reason", "验收说明")}<p>只有合格接收部分进入可用库存；拒收及未交数量继续保留为未完成余量。</p></>}
      {action === "resolution" && <>{enumSelect("resolutionType", "余量处理方式", { replenishment: "跟进补货", terminate: "提交终止审批" })}{input("quantity", "余量处理数量", "number")}{input("reason", "处理依据")}{form.resolutionType === "replenishment" ? input("expectedArrivalDate", "补货预计到货日", "date") : approvalFields()}</>}
      {action === "sale" && <>{receiptSelect()}{input("quantity", "实际售出数量", "number")}{input("saleBasis", "合同售出确认依据")}{input("sourceReference", "实际售出原始单据编号")}{input("soldAt", "实际售出时间（当前设备时区）", "datetime-local")}<p>本次只记录事实，另由具备采购复核及财务权限的不同人员确认后形成应付。</p></>}
      {action === "sale-confirm" && <>{enumSelect("decision", "售出复核结果", { confirmed: "确认实际售出", rejected: "驳回" })}{input("reason", "复核依据")}</>}
      {action === "allocate" && <>{payments.length ? select("sourcePaymentId", "实际预付款流水", payments.map(p => ({ id: p.id, label: `${p.bankFlowRef ?? p.id}／${money(p.amountCent)}` }))) : <>{input("sourcePaymentId", "实际预付款流水")}<small>未取得付款流水选项，请从真实应付付款凭证核对编号；系统仍核验原支付归属和剩余额度。</small></>}{select("targetItemId", "本采购待付入库款", (detail?.payables ?? []).filter(p => p.sourceType === "purchase_inbound" && p.direction === "payable" && p.isCurrent && !p.lockedDayCloseId && p.amountCent > 0 && p.unpaidAmountCent > 0 && detail?.receipts.some(r => r.id === p.sourceId)).map(p => ({ id: p.id, label: `${p.itemNo}／未付 ${money(p.unpaidAmountCent)}` })))}{input("amountCent", "本次冲抵金额（分）", "number")}</>}
      {action === "prepayment-resolution" && <>{enumSelect("resolutionType", "预付余额处理方式", { refund_unused: "收回已实际支付但未使用的预付款", reduce_unpaid: "终止尚未支付的预付款" })}{form.resolutionType === "refund_unused" && <>{payments.length ? select("sourcePaymentId", "实际预付款流水", payments.map(p => ({ id: p.id, label: `${p.bankFlowRef ?? p.id}／${money(p.amountCent)}` }))) : input("sourcePaymentId", "实际预付款流水")}{input("refundDueDate", "双方确认的预付款退款到期日", "date")}</>}{input("amountCent", "预付余额处理金额（分）", "number")}{input("contractBasis", "锁定合同余额处理依据")}{input("reason", "处理原因")}{approvalFields(["finance_review"])}<p>按锁定合同及双方确认依据提出具体金额；系统核实原付款未用余额或现有未付义务，提交后等待主线与财务审批，不按未交数量自动推减。</p></>}
      {action === "prepayment-post" && <>{input("reason", "预付调整复核依据")}<p>仅登记已批准的账款调整或供应商应收退款，不表示已收到现金；须由方案申请人以外的财务人员办理。</p></>}
      {action === "return" && <>{receiptSelect()}{input("quantity", "退供数量", "number")}{input("returnUnitPriceCent", "双方确认退货单价（分）", "number")}{input("responsibilityTerms", "责任及费用约定")}{input("reason", "退供原因")}{detail?.settlementMethod === "consignment" && <>{select("consignmentSaleId", "已确认寄售结算（未售退回留空）", detail.consignmentSales.filter(s => s.receiptId === form.receiptId && s.status === "confirmed").map(s => ({ id: s.id, label: `${s.sourceReference}／${s.quantity}` })))}<p>未售退回留空，不产生已售货款退款；已售退回须关联原确认结算。</p></>}{approvalFields(["finance_review"])}</>}
      {action === "handover" && <>{input("handedOverAt", "实际交接时间（当前设备时区）", "datetime-local")}<p>请上传供应商实际收回凭证；成功后才扣减原批次库存，财务仍需单独复核。</p></>}
      {action === "finance-review" && <>{input("supplierRefundDueDate", "双方确认的供应商退款到期日（产生应收时必填）", "date")}{input("reason", "财务复核依据")}<p>复核确认金额、实际出库成本和差额；先冲减原未付货款，已付部分登记应收退款，实际到账后才结清。</p></>}
      {needsProof && <><label>本次真实凭证<input aria-label="本次真实凭证" type="file" multiple disabled={!can([...permissions(action), EVIDENCE])} onChange={e => { void upload(e.target.files); e.target.value = ""; }} /></label>{!can([EVIDENCE]) && <p>当前无附件上传权限，不能提交需要原始凭证的操作。</p>}<ul>{proofs.map(p => <li key={p.id}>{p.name}（已上传）</li>)}</ul></>}
      <PermissionButton permissionsState={effectivePermissions} permissions={[...permissions(action), ...(needsProof ? [EVIDENCE] : [])]} disabled={busy} onClick={() => void submit()}>确认{actionNames[action]}</PermissionButton><button type="button" onClick={() => { setAction(null); setProofs([]); }}>取消填写</button>
    </fieldset>}
  </section>;
}
