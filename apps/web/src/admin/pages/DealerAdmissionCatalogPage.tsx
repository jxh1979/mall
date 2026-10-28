// 文件版本：V0.03
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton, useCan } from "../security/Can";
import { shortId } from "../format/ids";
import type { AccessUserRow, AdminPageProps } from "../types";

/**
 * 经销商准入与价格目录（ADM-DLR-001，CG-11a）。
 * BSM:1216-1222：经销商五态准入（企业资料、合同、开票、银行资料四项审核通过才启用门户）；每份合同独立、可版本化的合同销售价格目录
 * （编辑与审批不得同人）；未定价商品只能询价（报价审批通过后才能订购）；库存外显只给 有货 / 紧张 / 缺货 / 预计交期。
 */
type DealerRow = { id: string; dealerName?: string; status?: string; portalEnabled?: boolean; contactName?: string; contactPhone?: string; reviewNote?: string; unifiedSocialCreditCode?: string; createdBy?: string };
type Admission = { dealerId: string; companyAttachmentIds: string[]; contractAttachmentIds: string[]; invoiceAttachmentIds: string[]; bankAttachmentIds: string[]; submittedBy?: string };
const DOCUMENTS = [["companyAttachmentIds", "company", "企业资料"], ["contractAttachmentIds", "contract", "合同"], ["invoiceAttachmentIds", "invoice", "开票资料"], ["bankAttachmentIds", "bank", "银行资料"]] as const;
type Documents = Pick<Admission, "companyAttachmentIds" | "contractAttachmentIds" | "invoiceAttachmentIds" | "bankAttachmentIds">;
const emptyDocuments = (): Documents => ({ companyAttachmentIds: [], contractAttachmentIds: [], invoiceAttachmentIds: [], bankAttachmentIds: [] });
type ContractRow = { id: string; contractNo?: string; salesEntityId?: string; centralWarehouseId?: string; validFrom?: string; validTo?: string; status?: string; designatedHandlerUserId?: string | null };
type CatalogRow = { id: string; version?: number; status?: string; editedBy?: string; submittedBy?: string; approvedBy?: string; note?: string };
type CatalogLine = { id: string; itemId?: string; spec?: string; unitPriceCent?: number; taxRateBps?: number | null; expectedRestockDate?: string | null };
type InquiryRow = { id: string; itemId?: string; spec?: string; quantity?: number; status?: string; quotedPriceCent?: number | null; quotedBy?: string | null; quoteValidUntil?: string | null; note?: string };
type PortalAccountRow = { userId: string; username?: string; status?: string; createdAt?: string | null; revokedAt?: string | null };
type StockRow = { itemId: string; itemName?: string; spec?: string; unitPriceCent?: number; availability?: string; expectedRestockDate?: string | null };

const DEALER_STATUS: Record<string, string> = { pending_submission: "待提交", under_review: "审核中", approved: "已通过", rejected: "已驳回", deactivated: "已停用" };
const CATALOG_STATUS: Record<string, string> = { draft: "草稿", pending_approval: "待审批", effective: "已生效", expired: "已失效" };
const INQUIRY_STATUS: Record<string, string> = { pending_quote: "待报价", quote_approval: "报价审批中", quoted: "已报价", expired: "已失效" };
const AVAILABILITY: Record<string, string> = { in_stock: "有货", low: "紧张", out_of_stock: "缺货", expected_restock: "预计交期" };
const CONTRACT_STATUS: Record<string, string> = { active: "有效", expired: "已到期", terminated: "已终止" };

function money(cent?: number | null) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}
function label(map: Record<string, string>, value?: string | null) {
  return value ? (map[value] ?? value) : "—";
}
function cnDate(value?: string | null) {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  return m ? `${m[1]}年${Number(m[2])}月${Number(m[3])}日` : value;
}

export function DealerAdmissionCatalogPage({ api, user, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const canUpload = useCan(effectivePermissions, ["dealer.manage", "evidence.attachment.create"]);
  const canReadEvidence = useCan(effectivePermissions, ["evidence.timeline.view"]);
  const [dealers, setDealers] = React.useState<DealerRow[]>([]);
  const [selected, setSelected] = React.useState<DealerRow | null>(null);
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [contractId, setContractId] = React.useState("");
  const [catalogs, setCatalogs] = React.useState<CatalogRow[]>([]);
  const [lines, setLines] = React.useState<{ catalogId: string; rows: CatalogLine[] } | null>(null);
  const [inquiries, setInquiries] = React.useState<InquiryRow[]>([]);
  const [stock, setStock] = React.useState<StockRow[]>([]);
  const [portalAccounts, setPortalAccounts] = React.useState<PortalAccountRow[]>([]);
  const [staffUsers, setStaffUsers] = React.useState<AccessUserRow[]>([]);
  const [handlerUserId, setHandlerUserId] = React.useState("");
  const [admission, setAdmission] = React.useState<Admission | null>(null);
  const [documents, setDocuments] = React.useState<Documents>(emptyDocuments);
  const [uploading, setUploading] = React.useState(false);
  const selectedId = React.useRef<string | null>(null);
  const [formError, setFormError] = React.useState("");
  // 门户账号（CG-11d）
  const [portalUsername, setPortalUsername] = React.useState("");
  const [portalPassword, setPortalPassword] = React.useState("");
  const [portalContact, setPortalContact] = React.useState("");
  // 建档
  const [dealerName, setDealerName] = React.useState("");
  const [uscc, setUscc] = React.useState("");
  const [contactName, setContactName] = React.useState("");
  const [contactPhone, setContactPhone] = React.useState("");
  // 审核
  const [reviewFlags, setReviewFlags] = React.useState({ companyProfileReviewed: false, contractReviewed: false, invoiceReviewed: false, bankReviewed: false });
  const [reviewNote, setReviewNote] = React.useState("");
  // 合同
  const [contractNo, setContractNo] = React.useState("");
  const [salesEntityId, setSalesEntityId] = React.useState("");
  const [warehouseId, setWarehouseId] = React.useState("");
  const [validFrom, setValidFrom] = React.useState("");
  const [validTo, setValidTo] = React.useState("");
  // 目录草稿（一行一条：商品id,规格,单价元,税率%,预计到货日）
  const [draftLines, setDraftLines] = React.useState("");
  const [draftNote, setDraftNote] = React.useState("");
  // 询价
  const [inqItem, setInqItem] = React.useState("");
  const [inqSpec, setInqSpec] = React.useState("");
  const [inqQty, setInqQty] = React.useState("");
  const [quotePrice, setQuotePrice] = React.useState("");
  const [quoteValidUntil, setQuoteValidUntil] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const rows = await api.get<DealerRow[]>("/api/v1/dealers");
      setDealers(rows ?? []);
      try { setStaffUsers((await api.get<AccessUserRow[]>("/api/v1/access/users")) ?? []); } catch { setStaffUsers([]); }
    },
    { onError: setGlobalError, fallbackMessage: "经销商列表读取失败", onStart: () => setGlobalError(null) },
  );

  const key = () => `dlr-${Date.now()}-${Math.random().toString(16).slice(2)}`;

  async function run(action: string, fn: () => Promise<unknown>) {
    setFormError("");
    try {
      const result = await fn();
      setActionResult({ action, ...(result && typeof result === "object" ? (result as Record<string, unknown>) : {}) });
      return true;
    } catch (e) {
      const message = e instanceof Error ? e.message : "操作失败";
      setFormError(message);
      setGlobalError(message);
      return false;
    }
  }

  async function selectDealer(row: DealerRow) {
    setSelected(row);
    selectedId.current = row.id;
    setAdmission(null); setDocuments(emptyDocuments());
    setReviewFlags({ companyProfileReviewed: false, contractReviewed: false, invoiceReviewed: false, bankReviewed: false });
    setReviewNote("");
    setContractId("");
    setCatalogs([]); setLines(null); setStock([]);
    await loadDealerData(row);
  }

  // 刷新当前经销商的合同与询价，不清空已选合同（询价 / 报价 / 审批后目录与外显区块保持可见）。
  async function loadDealerData(row: DealerRow) {
    try {
      const [cs, qs, pa, submitted] = await Promise.all([
        api.get<ContractRow[]>(`/api/v1/dealers/${encodeURIComponent(row.id)}/contracts`),
        api.get<InquiryRow[]>(`/api/v1/dealers/${encodeURIComponent(row.id)}/inquiries`),
        api.get<PortalAccountRow[]>(`/api/v1/dealers/${encodeURIComponent(row.id)}/portal-accounts`),
        api.get<Admission | null>(`/api/v1/dealers/${encodeURIComponent(row.id)}/admission`),
      ]);
      if (selectedId.current !== row.id) return;
      setAdmission(submitted);
      setDocuments(submitted ?? emptyDocuments());
      setContracts(cs ?? []);
      setInquiries(qs ?? []);
      setPortalAccounts(pa ?? []);
    } catch (e) {
      setFormError(e instanceof Error ? e.message : "经销商资料读取失败");
    }
  }

  async function selectContract(id: string) {
    setContractId(id);
    setLines(null);
    try {
      const [cats, st] = await Promise.all([
        api.get<CatalogRow[]>(`/api/v1/dealers/contracts/${encodeURIComponent(id)}/price-catalogs`),
        api.get<StockRow[]>(`/api/v1/dealers/contracts/${encodeURIComponent(id)}/external-stock`),
      ]);
      setCatalogs(cats ?? []);
      setStock(st ?? []);
    } catch (e) {
      setFormError(e instanceof Error ? e.message : "合同资料读取失败");
    }
  }

  async function refreshSelected() {
    await reload();
    if (selected) {
      const fresh = await api.get<DealerRow[]>("/api/v1/dealers");
      const row = (fresh ?? []).find((d) => d.id === selected.id) ?? selected;
      await selectDealer(row);
      if (contractId) await selectContract(contractId);
    }
  }

  const createDealer = () => run("dealer_created", async () => {
    if (!dealerName.trim()) throw new Error("经销商名称必填");
    const d = await api.post<DealerRow>("/api/v1/dealers", { idempotencyKey: key(), dealerName: dealerName.trim(), unifiedSocialCreditCode: uscc.trim() || undefined, contactName: contactName.trim() || undefined, contactPhone: contactPhone.trim() || undefined });
    setDealerName(""); setUscc(""); setContactName(""); setContactPhone("");
    await reload();
    return { 经销商: d?.id, 状态: "待提交" };
  });

  const submitDealer = (row: DealerRow) => run("dealer_submitted", async () => {
    if (selected?.id !== row.id || DOCUMENTS.some(([field]) => !documents[field]?.length)) throw new Error("请选中该经销商并上传企业、合同、开票、银行四类资料后提交");
    if (uploading) throw new Error("资料仍在上传，请等待完成");
    await api.post(`/api/v1/dealers/${encodeURIComponent(row.id)}/submit`, { idempotencyKey: key(), ...documents });
    await refreshSelected();
    return { 经销商: row.id, 状态: "审核中" };
  });

  const reviewDealer = (row: DealerRow, decision: "approve" | "reject") => run("dealer_reviewed", async () => {
    if (row.createdBy === user.id || admission?.submittedBy === user.id) throw new Error("准入建档人和提交人不得自审");
    if (decision === "approve" && (DOCUMENTS.some(([field]) => !admission?.[field]?.length) || Object.values(reviewFlags).some((flag) => !flag))) throw new Error("须查看四类提交资料并逐项核实通过");
    if (decision === "reject" && !reviewNote.trim()) throw new Error("驳回须填写原因");
    const d = await api.post<DealerRow>(`/api/v1/dealers/${encodeURIComponent(row.id)}/review`, { idempotencyKey: key(), decision, ...reviewFlags, note: reviewNote.trim() || undefined });
    await refreshSelected();
    return { 经销商: row.id, 结论: decision === "approve" ? "已通过" : "已驳回", 门户: d?.portalEnabled ? "已启用" : "未启用" };
  });

  const deactivateDealer = (row: DealerRow) => run("dealer_deactivated", async () => {
    const reason = window.prompt("停用原因（必填）：") ?? "";
    if (!reason.trim()) throw new Error("停用须填写原因");
    await api.post(`/api/v1/dealers/${encodeURIComponent(row.id)}/deactivate`, { idempotencyKey: key(), reason: reason.trim() });
    await refreshSelected();
    return { 经销商: row.id, 状态: "已停用", 说明: "门户与接口权限已撤销，已完成记录保留" };
  });

  const createPortalAccount = () => run("dealer_portal_account_created", async () => {
    if (!selected) throw new Error("请先选中经销商");
    const account = await api.post<PortalAccountRow>(`/api/v1/dealers/${encodeURIComponent(selected.id)}/portal-accounts`, {
      idempotencyKey: `dpa-${Date.now()}`, username: portalUsername.trim(), password: portalPassword, contactName: portalContact.trim() || undefined,
    });
    setPortalUsername(""); setPortalPassword(""); setPortalContact("");
    await loadDealerData(selected);
    return { 门户账号: account?.username ?? portalUsername, 经销商: selected.dealerName, 状态: account?.status === "active" ? "已启用" : "预建禁用，四项审核通过后才可登录" };
  });

  async function uploadDocuments(field: keyof Documents, role: string, files: FileList | null) {
    if (!selected || !files?.length || uploading || !canUpload) return;
    const dealerId = selected.id;
    setUploading(true); setFormError("");
    try {
      if (!api.postForm) throw new Error("当前客户端不支持资料上传");
      for (const file of Array.from(files)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "dealer_admission");
        form.append("business_id", dealerId); form.append("attachment_role", role);
        const attachment = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
        if (selectedId.current === dealerId) setDocuments((previous) => ({ ...previous, [field]: [...previous[field], attachment.id] }));
      }
    } catch (error) { setFormError(error instanceof Error ? error.message : "资料上传失败"); }
    finally { setUploading(false); }
  }

  const createContract = () => run("dealer_contract_created", async () => {
    if (!selected) throw new Error("请先选择经销商");
    if (!contractNo.trim() || !salesEntityId.trim() || !warehouseId.trim() || !validFrom || !validTo) throw new Error("合同编号、销售主体、中心仓、起止日期必填");
    const c = await api.post<ContractRow>(`/api/v1/dealers/${encodeURIComponent(selected.id)}/contracts`, { idempotencyKey: key(), contractNo: contractNo.trim(), salesEntityId: salesEntityId.trim(), centralWarehouseId: warehouseId.trim(), validFrom, validTo });
    setContractNo("");
    await loadDealerData(selected);
    return { 经销合同: c?.id, 合同编号: c?.contractNo };
  });

  // 第十八批第 31 条（BSM:1247「合同销售主体指定经办人」）：经办人从员工列表选、提交用户编号（后端按用户编号认经办人本人）；改派直接覆盖。
  const userLabel = (id: string) => { const u = staffUsers.find((x) => x.id === id); return u ? `${u.username}（${shortId(u.id)}）` : id; };
  const designateHandler = (contract: ContractRow) => run("dealer_contract_handler_designated", async () => {
    if (!selected) throw new Error("请先选择经销商");
    if (!handlerUserId) throw new Error("请先在「指定经办人」下拉中选择员工");
    await api.post<ContractRow>(`/api/v1/dealers/contracts/${encodeURIComponent(contract.id)}/designated-handler`, { idempotencyKey: key(), handlerUserId });
    await loadDealerData(selected);
    return { 合同编号: contract.contractNo, 指定经办人: userLabel(handlerUserId) };
  });

  const createDraft = () => run("dealer_price_catalog_drafted", async () => {
    if (!contractId) throw new Error("请先选择合同");
    const parsed = draftLines.split("\n").map((l) => l.trim()).filter(Boolean).map((l) => {
      const [itemId, spec, price, tax, restock] = l.split(",").map((s) => s.trim());
      const cent = Math.round(Number(price) * 100);
      if (!itemId || !Number.isFinite(cent) || cent <= 0) throw new Error(`价格行格式应为「商品id,规格,单价(元),税率%,预计到货日」：${l}`);
      return { itemId, spec: spec || "", unitPriceCent: cent, taxRateBps: tax ? Math.round(Number(tax) * 100) : undefined, expectedRestockDate: restock || undefined };
    });
    if (parsed.length === 0) throw new Error("至少一条商品价格");
    const c = await api.post<CatalogRow>(`/api/v1/dealers/contracts/${encodeURIComponent(contractId)}/price-catalogs`, { idempotencyKey: key(), note: draftNote.trim() || undefined, lines: parsed });
    setDraftLines(""); setDraftNote("");
    await selectContract(contractId);
    return { 价格目录: c?.id, 版本: c?.version, 状态: "草稿" };
  });

  const submitCatalog = (c: CatalogRow) => run("dealer_price_catalog_submitted", async () => {
    await api.post(`/api/v1/dealers/price-catalogs/${encodeURIComponent(c.id)}/submit`, { idempotencyKey: key() });
    await selectContract(contractId);
    return { 价格目录: c.id, 状态: "待审批" };
  });

  const decideCatalog = (c: CatalogRow, decision: "approve" | "reject") => run("dealer_price_catalog_decided", async () => {
    const note = window.prompt(decision === "approve" ? "审批意见（可空）：" : "驳回原因：") ?? "";
    await api.post(`/api/v1/dealers/price-catalogs/${encodeURIComponent(c.id)}/decision`, { idempotencyKey: key(), decision, note: note.trim() || undefined });
    await selectContract(contractId);
    return { 价格目录: c.id, 结论: decision === "approve" ? "已生效（旧版本已失效）" : "退回草稿" };
  });

  const loadLines = async (c: CatalogRow) => {
    try {
      const rows = await api.get<CatalogLine[]>(`/api/v1/dealers/price-catalogs/${encodeURIComponent(c.id)}/lines`);
      setLines({ catalogId: c.id, rows: rows ?? [] });
    } catch (e) { setFormError(e instanceof Error ? e.message : "目录明细读取失败"); }
  };

  const createInquiry = () => run("dealer_inquiry_created", async () => {
    if (!selected || !contractId) throw new Error("请先选择经销商与合同");
    const qty = Number(inqQty);
    if (!inqItem.trim() || !Number.isFinite(qty) || qty <= 0) throw new Error("商品 id 与大于 0 的数量必填");
    const q = await api.post<InquiryRow>(`/api/v1/dealers/${encodeURIComponent(selected.id)}/inquiries`, { idempotencyKey: key(), dealerContractId: contractId, itemId: inqItem.trim(), spec: inqSpec.trim() || undefined, quantity: qty });
    setInqItem(""); setInqSpec(""); setInqQty("");
    await loadDealerData(selected);
    return { 询价: q?.id, 状态: "待报价" };
  });

  const quoteInquiry = (q: InquiryRow) => run("dealer_inquiry_quoted", async () => {
    const cent = Math.round(Number(quotePrice) * 100);
    if (!Number.isFinite(cent) || cent <= 0 || !quoteValidUntil) throw new Error("报价单价（元）与报价有效期必填");
    await api.post(`/api/v1/dealers/inquiries/${encodeURIComponent(q.id)}/quote`, { idempotencyKey: key(), quotedPriceCent: cent, quoteValidUntil });
    if (selected) await loadDealerData(selected);
    return { 询价: q.id, 状态: "报价审批中" };
  });

  const decideQuote = (q: InquiryRow, decision: "approve" | "reject") => run("dealer_inquiry_decided", async () => {
    await api.post(`/api/v1/dealers/inquiries/${encodeURIComponent(q.id)}/decision`, { idempotencyKey: key(), decision });
    if (selected) await loadDealerData(selected);
    return { 询价: q.id, 结论: decision === "approve" ? "已报价（可订购）" : "退回待报价" };
  });

  const expireInquiry = (q: InquiryRow) => run("dealer_inquiry_expired", async () => {
    await api.post(`/api/v1/dealers/inquiries/${encodeURIComponent(q.id)}/expire`, { idempotencyKey: key() });
    if (selected) await loadDealerData(selected);
    return { 询价: q.id, 状态: "已失效" };
  });

  return (
    <section data-testid="dealer-admission-page">
      <AdminPageHeader
        title="经销商准入与价格目录"
        description="经销商准入五态：待提交、审核中、已通过、已驳回、已停用；企业资料、合同、开票和银行资料四项审核通过后才启用独立经销商门户。每份经销合同使用独立、可版本化的合同销售价格目录，不复用内部结算价，编辑人与审批人不得同人；已定合同价商品可下单，未定价商品只能询价，报价审批通过后才能订购。对外库存只显示有货、紧张、缺货、预计交期。"
      />
      <AuditTrailHint event="dealer.created / dealer.reviewed / dealer.deactivated / dealer.price_catalog.decided / dealer.inquiry.decided" />
      <p className="admin-muted-action" data-testid="dealer-evidence">接口证据：GET/POST /api/v1/dealers；POST /api/v1/dealers/{"{id}"}/review；POST /api/v1/dealers/contracts/{"{id}"}/price-catalogs；GET /api/v1/dealers/contracts/{"{id}"}/external-stock。</p>
      {formError ? <div data-testid="dealer-error" className="admin-inline-error">{formError}</div> : null}

      <h3>经销商建档</h3>
      <p className="admin-muted-action">请通过角色管理为指定销售经办配置建档、提交和资料上传权限，为运营复核人配置准入审核和资料查看权限；建档人及本次提交人不能审核本人申请。</p>
      <div className="admin-form-grid" data-testid="dealer-create-form">
        <label>经销商名称<input data-testid="dealer-name-input" value={dealerName} onChange={(e) => setDealerName(e.target.value)} /></label>
        <label>统一社会信用代码<input data-testid="dealer-uscc-input" value={uscc} onChange={(e) => setUscc(e.target.value)} /></label>
        <label>联系人<input value={contactName} onChange={(e) => setContactName(e.target.value)} /></label>
        <label>联系电话<input value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid="dealer-create" onClick={() => void createDealer()}>建档（待提交）</PermissionButton>
      </div>

      <AdminDataState loading={loading} error={error} empty={dealers.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dealer-table">
            <thead><tr><th>经销商</th><th>信用代码</th><th>状态</th><th>门户</th><th>联系人</th><th>审核意见</th><th>操作</th></tr></thead>
            <tbody>
              {dealers.map((row) => (
                <tr key={row.id} data-testid={`dealer-row-${row.id}`} className={selected?.id === row.id ? "admin-row-selected" : undefined}>
                  <td>{row.dealerName ?? row.id}</td>
                  <td>{row.unifiedSocialCreditCode ?? "—"}</td>
                  <td data-testid={`dealer-status-${row.id}`}>{label(DEALER_STATUS, row.status)}</td>
                  <td>{row.portalEnabled ? "已启用" : "未启用"}</td>
                  <td>{row.contactName ?? "—"}</td>
                  <td>{row.reviewNote ?? "—"}</td>
                  <td>
                    <button type="button" data-testid={`dealer-select-${row.id}`} onClick={() => void selectDealer(row)}>选中</button>
                    {row.status === "pending_submission" || row.status === "rejected" ? (
                      <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid={`dealer-submit-${row.id}`} onClick={() => void submitDealer(row)}>提交审核</PermissionButton>
                    ) : null}
                    {row.status === "approved" ? (
                      <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid={`dealer-deactivate-${row.id}`} onClick={() => void deactivateDealer(row)}>停用</PermissionButton>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>

      {selected ? (
        <>
          <div className="admin-form-grid" data-testid="dealer-admission-documents">
            <h3>四类准入资料：{selected.dealerName}</h3>
            {DOCUMENTS.map(([field, role, title]) => <div key={field}>
              <label>{title}扫描件{["pending_submission", "rejected"].includes(selected.status ?? "") ?
                <input type="file" multiple accept=".jpg,.jpeg,.png,.webp,.heic" disabled={uploading || !canUpload} data-testid={`admission-upload-${role}`}
                  onChange={(event) => { void uploadDocuments(field, role, event.target.files); event.target.value = ""; }} /> : null}</label>
              {(documents[field] ?? []).map((id, index) => <span key={id}>
                {canReadEvidence ? <a href={`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`} target="_blank" rel="noreferrer">查看{title} {index + 1}</a> : <span>{title}附件 {index + 1}</span>}
                {["pending_submission", "rejected"].includes(selected.status ?? "") ? <button type="button" disabled={uploading} onClick={() => setDocuments((previous) => ({ ...previous, [field]: previous[field].filter((value) => value !== id) }))}>移出本次提交</button> : null}
              </span>)}
              {!documents[field]?.length ? <span>尚未提交</span> : null}
            </div>)}
            <p>附件须真实核验，上传和勾选不能代替核验；建档人、提交人不得自审。</p>
          </div>
          {selected.status === "under_review" ? (
            <div className="admin-form-grid" data-testid="dealer-review-form">
              <h3>准入审核：{selected.dealerName}</h3>
              {([["companyProfileReviewed", "企业资料审核通过"], ["contractReviewed", "合同审核通过"], ["invoiceReviewed", "开票资料审核通过"], ["bankReviewed", "银行资料审核通过"]] as const).map(([k, text]) => (
                <label key={k}><input type="checkbox" data-testid={`dealer-review-${k}`} checked={reviewFlags[k]} onChange={(e) => setReviewFlags({ ...reviewFlags, [k]: e.target.checked })} />{text}</label>
              ))}
              <label>审核意见<input data-testid="dealer-review-note" value={reviewNote} onChange={(e) => setReviewNote(e.target.value)} /></label>
              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.review"]} data-testid="dealer-review-approve" disabled={uploading || selected.createdBy === user.id || admission?.submittedBy === user.id} onClick={() => void reviewDealer(selected, "approve")}>通过并启用门户</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.review"]} data-testid="dealer-review-reject" disabled={selected.createdBy === user.id || admission?.submittedBy === user.id} onClick={() => void reviewDealer(selected, "reject")}>驳回</PermissionButton>
              <span className="admin-muted-action">四项资料全部审核通过后才能通过准入并启用独立经销商门户。</span>
            </div>
          ) : null}

          <h3>门户账号（独立经销商 Web 门户，第十一端）：{selected.dealerName}</h3>
          <div className="admin-form-grid" data-testid="dealer-portal-account-form">
            <label>登录名<input data-testid="portal-username-input" value={portalUsername} onChange={(e) => setPortalUsername(e.target.value)} /></label>
            <label>初始密码<input data-testid="portal-password-input" type="password" value={portalPassword} onChange={(e) => setPortalPassword(e.target.value)} /></label>
            <label>企业联系人<input data-testid="portal-contact-input" value={portalContact} onChange={(e) => setPortalContact(e.target.value)} /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.portal_account.manage"]} data-testid="portal-account-create" onClick={() => void createPortalAccount()}>预建或开通门户账号</PermissionButton>
            <span className="admin-muted-action">未审核通过的账号保持禁用、不能登录；门户账号与员工端、商户门户隔离，只见本企业数据；停用经销商时立即终止其全部门户会话。</span>
          </div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="dealer-portal-account-table">
              <thead><tr><th>登录名</th><th>状态</th><th>开通时间</th><th>撤权时间</th></tr></thead>
              <tbody>
                {portalAccounts.map((a) => (
                  <tr key={a.userId}><td>{a.username ?? a.userId}</td><td data-testid={`portal-account-status-${a.userId}`}>{a.status === "revoked" ? "已撤权" : a.status === "disabled" ? "禁用待准入" : "有效"}</td><td>{cnDate(a.createdAt)}</td><td>{cnDate(a.revokedAt)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>

          <h3>经销合同：{selected.dealerName}</h3>
          <div className="admin-form-grid" data-testid="dealer-contract-form">
            <label>合同编号<input data-testid="contract-no-input" value={contractNo} onChange={(e) => setContractNo(e.target.value)} /></label>
            <label>销售主体 id<input data-testid="contract-entity-input" value={salesEntityId} onChange={(e) => setSalesEntityId(e.target.value)} /></label>
            <label>中心仓 id<input data-testid="contract-warehouse-input" value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)} /></label>
            <label>生效日期<input type="date" data-testid="contract-from-input" value={validFrom} onChange={(e) => setValidFrom(e.target.value)} /></label>
            <label>到期日期<input type="date" data-testid="contract-to-input" value={validTo} onChange={(e) => setValidTo(e.target.value)} /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid="contract-create" onClick={() => void createContract()}>登记合同</PermissionButton>
          </div>
          <div className="admin-form-grid" data-testid="dealer-contract-handler-form">
            <label>指定经办人<select data-testid="contract-handler-user" value={handlerUserId} onChange={(e) => setHandlerUserId(e.target.value)}><option value="">请选择员工</option>{staffUsers.map((x) => <option key={x.id} value={x.id}>{x.username}（{shortId(x.id)}）</option>)}</select></label>
            <span className="admin-muted-action">选好员工后在合同行点「指定经办人」。</span>
          </div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="dealer-contract-table">
              <thead><tr><th>合同编号</th><th>销售主体</th><th>中心仓</th><th>有效期</th><th>状态</th><th>指定经办人</th><th>操作</th></tr></thead>
              <tbody>
                {contracts.map((c) => (
                  <tr key={c.id} className={contractId === c.id ? "admin-row-selected" : undefined}>
                    <td>{c.contractNo ?? c.id}</td><td>{c.salesEntityId ?? "—"}</td><td>{c.centralWarehouseId ?? "—"}</td>
                    <td>{cnDate(c.validFrom)} 至 {cnDate(c.validTo)}</td><td>{label(CONTRACT_STATUS, c.status)}</td>
                    <td data-testid={`contract-handler-${c.id}`}>{c.designatedHandlerUserId ? userLabel(c.designatedHandlerUserId) : "未指定"}</td>
                    <td><button type="button" data-testid={`contract-select-${c.id}`} onClick={() => void selectContract(c.id)}>查看目录与外显</button>
                      <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.manage"]} data-testid={`contract-handler-designate-${c.id}`} onClick={() => void designateHandler(c)}>指定经办人</PermissionButton></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {contractId ? (
            <>
              <h3>合同销售价格目录（版本只影响生效后的订单）</h3>
              <div className="admin-form-grid" data-testid="catalog-draft-form">
                <label>价格明细（每行：商品id,规格,单价元,税率%,预计到货日）
                  <textarea data-testid="catalog-lines-input" value={draftLines} onChange={(e) => setDraftLines(e.target.value)} rows={4} />
                </label>
                <label>说明<input data-testid="catalog-note-input" value={draftNote} onChange={(e) => setDraftNote(e.target.value)} /></label>
                <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.price_catalog.edit"]} data-testid="catalog-draft-create" onClick={() => void createDraft()}>新建草稿版本</PermissionButton>
              </div>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="catalog-table">
                  <thead><tr><th>版本</th><th>状态</th><th>编辑人</th><th>提交人</th><th>审批人</th><th>说明</th><th>操作</th></tr></thead>
                  <tbody>
                    {catalogs.map((c) => (
                      <tr key={c.id} data-testid={`catalog-row-${c.id}`}>
                        <td>V{c.version}</td><td data-testid={`catalog-status-${c.id}`}>{label(CATALOG_STATUS, c.status)}</td>
                        <td>{c.editedBy ?? "—"}</td><td>{c.submittedBy ?? "—"}</td><td>{c.approvedBy ?? "—"}</td><td>{c.note ?? "—"}</td>
                        <td>
                          <button type="button" data-testid={`catalog-lines-${c.id}`} onClick={() => void loadLines(c)}>明细</button>
                          {c.status === "draft" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.price_catalog.edit"]} data-testid={`catalog-submit-${c.id}`} onClick={() => void submitCatalog(c)}>提交审批</PermissionButton> : null}
                          {c.status === "pending_approval" ? (
                            <>
                              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.price_catalog.approve"]} data-testid={`catalog-approve-${c.id}`} onClick={() => void decideCatalog(c, "approve")}>审批生效</PermissionButton>
                              <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.price_catalog.approve"]} data-testid={`catalog-reject-${c.id}`} onClick={() => void decideCatalog(c, "reject")}>驳回</PermissionButton>
                            </>
                          ) : null}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {lines ? (
                <div className="admin-table-scroll">
                  <table className="admin-table" data-testid="catalog-lines-table">
                    <thead><tr><th>商品</th><th>规格</th><th>合同单价</th><th>税率快照</th><th>预计到货日</th></tr></thead>
                    <tbody>
                      {lines.rows.map((l) => (
                        <tr key={l.id}><td>{l.itemId}</td><td>{l.spec || "—"}</td><td>{money(l.unitPriceCent)}</td><td>{l.taxRateBps === null || l.taxRateBps === undefined ? "—" : `${l.taxRateBps / 100}%`}</td><td>{cnDate(l.expectedRestockDate)}</td></tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}

              <h3>库存外显（经销商可见口径：只给有货 / 紧张 / 缺货 / 预计交期）</h3>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="external-stock-table">
                  <thead><tr><th>商品</th><th>规格</th><th>合同单价</th><th>库存外显</th><th>预计交期</th></tr></thead>
                  <tbody>
                    {stock.map((s) => (
                      <tr key={`${s.itemId}-${s.spec ?? ""}`} data-testid={`stock-row-${s.itemId}`}>
                        <td>{s.itemName ?? s.itemId}</td><td>{s.spec || "—"}</td><td>{money(s.unitPriceCent)}</td>
                        <td data-testid={`stock-availability-${s.itemId}`}>{label(AVAILABILITY, s.availability)}</td><td>{cnDate(s.expectedRestockDate)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <h3>询价（未定价商品）</h3>
              <div className="admin-form-grid" data-testid="inquiry-form">
                <label>商品 id<input data-testid="inquiry-item-input" value={inqItem} onChange={(e) => setInqItem(e.target.value)} /></label>
                <label>规格<input data-testid="inquiry-spec-input" value={inqSpec} onChange={(e) => setInqSpec(e.target.value)} /></label>
                <label>数量<input data-testid="inquiry-qty-input" value={inqQty} onChange={(e) => setInqQty(e.target.value)} /></label>
                <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.inquiry.manage"]} data-testid="inquiry-create" onClick={() => void createInquiry()}>登记询价</PermissionButton>
                <label>报价单价（元）<input data-testid="quote-price-input" value={quotePrice} onChange={(e) => setQuotePrice(e.target.value)} /></label>
                <label>报价有效期<input type="date" data-testid="quote-valid-input" value={quoteValidUntil} onChange={(e) => setQuoteValidUntil(e.target.value)} /></label>
              </div>
            </>
          ) : null}

          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="inquiry-table">
              <thead><tr><th>询价</th><th>商品</th><th>规格</th><th>数量</th><th>状态</th><th>报价</th><th>报价人</th><th>有效期</th><th>操作</th></tr></thead>
              <tbody>
                {inquiries.map((q) => (
                  <tr key={q.id} data-testid={`inquiry-row-${q.id}`}>
                    <td>{q.id}</td><td>{q.itemId}</td><td>{q.spec || "—"}</td><td>{q.quantity ?? "—"}</td>
                    <td data-testid={`inquiry-status-${q.id}`}>{label(INQUIRY_STATUS, q.status)}</td>
                    <td>{money(q.quotedPriceCent)}</td><td>{q.quotedBy ?? "—"}</td><td>{cnDate(q.quoteValidUntil)}</td>
                    <td>
                      {q.status === "pending_quote" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.inquiry.quote"]} data-testid={`inquiry-quote-${q.id}`} onClick={() => void quoteInquiry(q)}>报价</PermissionButton> : null}
                      {q.status === "quote_approval" ? (
                        <>
                          <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.inquiry.approve"]} data-testid={`inquiry-approve-${q.id}`} onClick={() => void decideQuote(q, "approve")}>审批通过</PermissionButton>
                          <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.inquiry.approve"]} data-testid={`inquiry-reject-${q.id}`} onClick={() => void decideQuote(q, "reject")}>驳回</PermissionButton>
                        </>
                      ) : null}
                      {q.status === "quoted" ? <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.inquiry.quote"]} data-testid={`inquiry-expire-${q.id}`} onClick={() => void expireInquiry(q)}>置为已失效</PermissionButton> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </section>
  );
}
