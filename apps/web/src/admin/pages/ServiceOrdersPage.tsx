// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint, auditTraceField } from "../components/AuditTrailHint";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminPageProps } from "../types";
import { RowActions, type RowAction } from "../components/RowActions";

type CatalogRow = { id: string; name?: string; priceCent?: number; durationMinutes?: number; active?: boolean; archivedAt?: string | null; deleteStatus?: string | null; mgmtFeeMode?: string; mgmtFeeValue?: number; feeVersion?: number; nextMgmtFeeMode?: string | null; nextMgmtFeeValue?: number | null; nextEffectiveFrom?: string | null; nextRequestedBy?: string | null; nextApprovedBy?: string | null };
type OrderRow = { id: string; itemName?: string; priceCent?: number; tableId?: string; performerUserId?: string; orderedBy?: string; status?: string; cancelReason?: string; paidAt?: string; mgmtFeeCent?: number; dancerShareCent?: number; createdAt?: string };

const TEXT = {
  title: "服务点单",
  description: "公司统一账户代收(2026-07-02最终拍板):客人扫公司收款码付款→服务员确认收款→舞者上台→完成时分账(管理费归公司,余额进舞者薪资提成,月结代扣个税)。履约前可取消,完成后不可退;管理费变更须提交草案、他人复核并指定生效日。",
  loadFailed: "服务点单读取失败",
  orderCount: "服务单数",
  paidCount: "已收款",
  completedCount: "已完成",
  orderCountHelp: "当日服务单总数",
  paidHelp: "已收款待履约",
  completedHelp: "已完成分账",
  focusClear: "清除筛选，返回全部",
};

const STATUS_LABELS: Record<string, string> = { created: "待收款", paid: "已收款", pending_accept: "待接单(旧)", accepted: "已接单(旧)", completed: "已完成", cancelled: "已取消" };
const FEE_MODE_LABELS: Record<string, string> = { none: "免收", percent: "比例", fixed: "固定" };
function feeText(mode?: string | null, value?: number | null) {
  if (!mode || mode === "none") return "免收";
  if (mode === "percent") return `比例 ${((value ?? 0) / 100).toLocaleString("zh-CN")}%`;
  return `固定 ${money(value ?? 0)}`;
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带 ?kpi= 参数重进本页，只渲染服务单表并按状态子集过滤。
type KpiTarget = "orders" | "paid" | "completed";
function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target === "orders" || target === "paid" || target === "completed" ? target : null;
}
function kpiTitle(target: KpiTarget): string {
  if (target === "paid") return TEXT.paidCount;
  if (target === "completed") return TEXT.completedCount;
  return TEXT.orderCount;
}

function money(cent?: number) {
  return cent === undefined || cent === null ? "—" : `¥${(cent / 100).toLocaleString("zh-CN")}`;
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function ServiceOrdersPage({ api, setGlobalError, setActionResult, navigateTo, effectivePermissions }: AdminPageProps) {
  const [date, setDate] = React.useState(today);
  const [catalog, setCatalog] = React.useState<CatalogRow[]>([]);
  const [catalogNotice, setCatalogNotice] = React.useState<string | null>(null);
  const [orders, setOrders] = React.useState<OrderRow[]>([]);
  const [itemName, setItemName] = React.useState("");
  const [itemPrice, setItemPrice] = React.useState("");
  const [itemMinutes, setItemMinutes] = React.useState("");
  const [feeMode, setFeeMode] = React.useState("none");
  const [feeValue, setFeeValue] = React.useState("");
  const [companyQr, setCompanyQr] = React.useState("");
  // 引导式流程 phase-5：新增服务目录收起为主按钮 + 分步引导弹窗（收款码配置表单不动）。
  const [catalogOpen, setCatalogOpen] = React.useState(false);
  const [catalogError, setCatalogError] = React.useState("");
  const [catalogSubmitting, setCatalogSubmitting] = React.useState(false);
  // TK-23-R3：已有目录项的管理费改为「拟改费率 + 生效日」草案区（提交后由他人复核转正），不再直接保存生效。
  const [feeDraftFor, setFeeDraftFor] = React.useState<string | null>(null);
  const [draftMode, setDraftMode] = React.useState("percent");
  const [draftValue, setDraftValue] = React.useState("");
  const [draftFrom, setDraftFrom] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该服务单完整字段（枚举中文·金额¥·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(target);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  async function submitFeeDraft(id: string) {
    const value = draftMode === "none" ? 0 : Math.round(Number(draftValue) * 100);
    if (!draftFrom) { setGlobalError("请选择生效日"); return; }
    if (!Number.isFinite(value) || value < 0) { setGlobalError("管理费值不正确"); return; }
    try {
      await api.post(`/api/v1/services/catalog/${id}/fee-draft`, { mgmtFeeMode: draftMode, mgmtFeeValue: value, effectiveFrom: draftFrom });
      setActionResult({ action: "service_catalog_fee_drafted", id });
      setFeeDraftFor(null); setDraftValue(""); setDraftFrom("");
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "提交草案失败"); }
  }
  async function activateFeeDraft(id: string) {
    try {
      await api.post(`/api/v1/services/catalog/${id}/fee-activate`, {});
      setActionResult({ action: "service_catalog_fee_activated", id });
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "复核转正失败"); }
  }

  async function catalogArchive(id: string, archive: boolean) {
    let reason: string | undefined;
    if (archive) {
      const r = window.prompt("存档（下线）理由：");
      if (r === null) return;
      if (!r.trim()) { setGlobalError("存档理由不能为空"); return; }
      reason = r.trim();
    }
    try {
      await api.post(`/api/v1/services/catalog/${id}/${archive ? "archive" : "unarchive"}`, reason ? { reason } : {});
      setActionResult({ action: archive ? "service_catalog_archived" : "service_catalog_unarchived", id });
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "操作失败"); }
  }
  async function catalogRequestDelete(id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null) return;
    try {
      await api.post(`/api/v1/services/catalog/${id}/delete-request`, { reason });
      setActionResult({ action: "service_catalog_delete_requested", id });
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "操作失败"); }
  }
  async function catalogApproveDelete(id: string, decision: "approved" | "rejected") {
    try {
      await api.post(`/api/v1/services/catalog/${id}/delete-approve`, { decision });
      setActionResult({ action: "service_catalog_delete_" + decision, id });
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "操作失败"); }
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：服务目录另有 service.catalog 权限，仅可看服务单的账号可能无权（403）——
      // 目录降级为区块提示，不拖垮当日服务单主内容。
      const [catalogRes, orderRows] = await Promise.all([
        loadSecondary(() => api.get<CatalogRow[]>("/api/v1/services/catalog/all"), [] as CatalogRow[]),
        api.get<OrderRow[]>(`/api/v1/services/orders?date=${date}`),
      ]);
      setCatalog(catalogRes.data);
      setCatalogNotice(catalogRes.notice);
      setOrders(orderRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const isFirstDateChange = React.useRef(true);
  React.useEffect(() => {
    if (isFirstDateChange.current) {
      isFirstDateChange.current = false;
      return;
    }
    void reload();
  }, [date, reload]);

  // 参考价校验（步内 + 提交双保险）：服务名非空 + 参考价非负数。
  function catalogPriceInvalid() {
    const cent = Math.round(Number(itemPrice) * 100);
    return !itemName.trim() || !Number.isFinite(cent) || cent < 0;
  }

  // TK-23-R1（ENUM「固定金额必须在 0 至服务售价（含边界）之间；任一越界值在保存时直接拒绝」）：固定(元)≤参考价、比例 0..100%。
  function catalogFeeError(): string | null {
    const cent = Math.round(Number(itemPrice) * 100);
    const fee = feeMode === "none" ? 0 : Math.round(Number(feeValue) * 100);
    if (feeMode === "none") return null;
    if (!Number.isFinite(fee) || fee < 0) return "管理费值须为非负数";
    if (feeMode === "percent" && fee > 10000) return "比例管理费不得超过 100%";
    if (feeMode === "fixed" && fee > cent) return "固定管理费(元)不得超过参考价(元)";
    return null;
  }

  const saveItem = async () => {
    const cent = Math.round(Number(itemPrice) * 100);
    if (catalogPriceInvalid()) {
      setCatalogError("请填 服务名与非负参考价(元)");
      return;
    }
    const feeError = catalogFeeError();
    if (feeError) {
      setCatalogError(feeError);
      return;
    }
    setCatalogSubmitting(true);
    try {
      await api.post("/api/v1/services/catalog", {
        name: itemName.trim(), priceCent: cent,
        durationMinutes: itemMinutes ? Number(itemMinutes) : null, active: true,
        mgmtFeeMode: feeMode,
        mgmtFeeValue: feeMode === "percent" ? Math.round(Number(feeValue) * 100) : feeMode === "fixed" ? Math.round(Number(feeValue) * 100) : 0,
      });
      setActionResult({ action: "service_catalog_saved", name: itemName.trim() });
      setItemName(""); setItemPrice(""); setItemMinutes("");
      setCatalogError("");
      setCatalogOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新增服务目录失败";
      setCatalogError(message);
      setGlobalError(message);
    } finally {
      setCatalogSubmitting(false);
    }
  };

  const cancelOrder = async (id: string) => {
    await api.post(`/api/v1/services/orders/${id}/cancel`, { reason: "后台取消" });
    setActionResult({ action: "service_order_cancelled", id });
    await reload();
  };

  const paidCount = orders.filter((row) => row.status === "paid").length;
  const completedCount = orders.filter((row) => row.status === "completed").length;
  // 聚焦时对服务单表做客户端筛选：仅过滤已加载内存数组，不发起后端查询。
  const visibleOrders = kpiFocus === "paid"
    ? orders.filter((row) => row.status === "paid")
    : kpiFocus === "completed"
      ? orders.filter((row) => row.status === "completed")
      : orders;

  function openCatalogDetail(row: CatalogRow) {
    setDetail({
      title: `服务项目明细 · ${row.name ?? row.id}`,
      fields: [
        { label: "服务名", value: row.name ?? "—" },
        { label: "参考价", value: money(row.priceCent) },
        { label: "时长", value: row.durationMinutes ? `${row.durationMinutes} 分钟` : "—" },
        { label: "状态", value: row.active ? "上架" : "下架" },
        { label: "管理费(现行)", value: `${feeText(row.mgmtFeeMode, row.mgmtFeeValue)}（V${row.feeVersion ?? 1}）` },
        { label: "管理费草案", value: row.nextRequestedBy ? `${feeText(row.nextMgmtFeeMode, row.nextMgmtFeeValue)} 自 ${row.nextEffectiveFrom ?? "—"} 生效 · ${row.nextApprovedBy ? "已复核" : "待复核"}` : "—" },
        { label: "服务编号", value: row.id },
      ],
    });
  }

  function openOrderDetail(row: OrderRow) {
    setDetail({
      title: `服务单明细 · ${row.id}`,
      fields: [
        { label: "服务单号", value: row.id },
        { label: "服务项目", value: row.itemName ?? "—" },
        { label: "价格", value: money(row.priceCent) },
        { label: "桌台", value: row.tableId ?? "—" },
        {
          label: "履约人",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.performerUserId}>{row.performerUserId ?? "—"}</TraceLink>,
        },
        { label: "下单人", value: row.orderedBy ?? "—" },
        { label: "管理费", value: money(row.mgmtFeeCent) },
        { label: "舞者应得", value: money(row.dancerShareCent) },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
        { label: "取消原因", value: row.cancelReason ?? "—" },
        { label: "下单时间", value: cnDate(row.createdAt) },
        { label: "收款时间", value: cnDate(row.paidAt) },
        auditTraceField(navigateTo, "service_order", row.id),
      ],
    });
  }

  return (
    <section data-testid="service-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="service.catalog.saved / service.order.created / service.order.cancelled" />
      <h3>服务目录(固定参考价)</h3>
      <div className="admin-form-row" data-testid="service-catalog-editor">
        <button type="button" data-testid="svc-catalog-create-open" onClick={() => { setCatalogError(""); setCatalogOpen(true); }}>＋ 新增服务目录</button>
        <span className="admin-muted-action">服务目录=固定参考价项目；管理费模式决定完成分账时归公司比例。</span>
      </div>
      <GuidedFlowModal
        open={catalogOpen}
        title="新增服务目录"
        testId="svc-catalog-guided"
        submitTestId="svc-save"
        submitLabel="保存目录项"
        submitting={catalogSubmitting}
        error={catalogError || undefined}
        onClose={() => setCatalogOpen(false)}
        onSubmit={() => void saveItem()}
        steps={[
          {
            title: "服务信息",
            description: "填写服务名、参考价与时长。",
            validate: () => (catalogPriceInvalid() ? "请填 服务名与非负参考价(元)" : null),
            content: (
              <>
                <label>服务名<input data-testid="svc-name-input" value={itemName} onChange={(event) => setItemName(event.target.value)} placeholder="点舞" /></label>
                <label>参考价(元)<input data-testid="svc-price-input" value={itemPrice} onChange={(event) => setItemPrice(event.target.value)} /></label>
                <label>时长(分钟)<input data-testid="svc-minutes-input" value={itemMinutes} onChange={(event) => setItemMinutes(event.target.value)} /></label>
              </>
            ),
          },
          {
            title: "管理费",
            description: "选择管理费收取方式与数值。固定金额不得超过参考价，比例不得超过 100%。",
            validate: () => catalogFeeError(),
            content: (
              <>
                <label>管理费<select data-testid="svc-fee-mode" value={feeMode} onChange={(event) => setFeeMode(event.target.value)}><option value="none">免收</option><option value="percent">比例(%)</option><option value="fixed">固定(元)</option></select></label>
                <label>管理费值<input data-testid="svc-fee-value" value={feeValue} onChange={(event) => setFeeValue(event.target.value)} placeholder="10 或 30" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "服务名", value: itemName.trim() || "未填写" },
          { label: "参考价(元)", value: itemPrice.trim() || "未填写" },
          { label: "时长(分钟)", value: itemMinutes.trim() || "未填写" },
          { label: "管理费", value: feeMode === "percent" ? "比例(%)" : feeMode === "fixed" ? "固定(元)" : "免收" },
          { label: "管理费值", value: feeValue.trim() || "0" },
        ]}
      />
      <h3>公司收款码(统一代收;客人扫此码付款)</h3>
      <div className="admin-form-grid" data-testid="svc-payconfig-editor">
        <label>收款码内容<input data-testid="svc-payqr-input" value={companyQr} onChange={(event) => setCompanyQr(event.target.value)} placeholder="https://qr.alipay.com/..." /></label>
        <button type="button" data-testid="svc-payqr-save" onClick={() => void (async () => {
          if (!companyQr.trim()) { setGlobalError("请填收款码内容"); return; }
          await api.post("/api/v1/services/pay-config", { qrContent: companyQr.trim() });
          setActionResult({ action: "service_pay_config_saved" });
          setCompanyQr("");
        })()}>保存公司收款码</button>
      </div>
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {catalogNotice ? (
          <div className="admin-state" data-testid="service-catalog-degraded">{catalogNotice}</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="service-catalog-table">
            <thead><tr><th>服务</th><th>参考价</th><th>时长</th><th>管理费</th><th>状态</th><th>存档</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>
              {catalog.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`service-catalog-row-${row.id}`}
                  onClick={() => openCatalogDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCatalogDetail(row); } }}>
                  <td>{row.name ?? "—"}</td>
                  <td>{money(row.priceCent)}</td>
                  <td>{row.durationMinutes ? `${row.durationMinutes} 分钟` : "—"}</td>
                  <td data-testid={`svc-catalog-fee-${row.id}`}>{feeText(row.mgmtFeeMode, row.mgmtFeeValue)}{row.nextRequestedBy ? `（草案 ${feeText(row.nextMgmtFeeMode, row.nextMgmtFeeValue)} 自 ${row.nextEffectiveFrom ?? "—"}，${row.nextApprovedBy ? "已复核" : "待复核"}）` : ""}</td>
                  <td>{row.active ? "上架" : "下架"}</td>
                  <td>{row.deleteStatus === "pending" ? "待审批删除" : (row.archivedAt ? "已存档" : "生效中")}</td>
                  <td onClick={(event) => event.stopPropagation()}>{(() => {
                    const acts: RowAction[] = [];
                    if (row.deleteStatus === "pending") {
                      acts.push({ key: "approve", label: "批准删除", permissions: ["service.catalog.approve"], onClick: () => void catalogApproveDelete(row.id, "approved"), testId: `svc-catalog-delete-approve-${row.id}` });
                      acts.push({ key: "reject", label: "驳回", permissions: ["service.catalog.approve"], danger: true, onClick: () => void catalogApproveDelete(row.id, "rejected"), testId: `svc-catalog-delete-reject-${row.id}` });
                    } else if (row.archivedAt) {
                      acts.push({ key: "unarchive", label: "取消存档", permissions: ["service.catalog.manage"], onClick: () => void catalogArchive(row.id, false), testId: `svc-catalog-unarchive-${row.id}` });
                      acts.push({ key: "delete-request", label: "申请删除", permissions: ["service.catalog.manage"], danger: true, onClick: () => void catalogRequestDelete(row.id), testId: `svc-catalog-delete-request-${row.id}` });
                    } else {
                      // TK-23-R3：已有目录项的管理费只能走「提交草案 → 他人复核转正 → 指定生效日版本」，没有直接保存入口。
                      acts.push({ key: "fee-draft", label: "提交管理费草案", permissions: ["service.catalog.manage"], onClick: () => { setFeeDraftFor(row.id); setDraftMode(row.mgmtFeeMode ?? "percent"); }, testId: `svc-fee-draft-${row.id}` });
                      if (row.nextRequestedBy && !row.nextApprovedBy) {
                        acts.push({ key: "fee-activate", label: "复核转正", permissions: ["service.catalog.approve"], onClick: () => void activateFeeDraft(row.id), testId: `svc-fee-activate-${row.id}` });
                      }
                      acts.push({ key: "archive", label: "存档", permissions: ["service.catalog.manage"], onClick: () => void catalogArchive(row.id, true), testId: `svc-catalog-archive-${row.id}` });
                    }
                    return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`svc-catalog-actions-${row.id}`} />;
                  })()}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看服务项目详情" onClick={(event) => { event.stopPropagation(); openCatalogDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          {feeDraftFor ? (
            <div className="admin-form-row" data-testid="svc-fee-draft-form">
              <span>管理费草案（{feeDraftFor}）：</span>
              <label>模式<select data-testid="svc-fee-draft-mode" value={draftMode} onChange={(event) => setDraftMode(event.target.value)}><option value="none">免收</option><option value="percent">比例(%)</option><option value="fixed">固定(元)</option></select></label>
              <label>数值<input data-testid="svc-fee-draft-value" value={draftValue} onChange={(event) => setDraftValue(event.target.value)} placeholder="10 或 30" /></label>
              <label>生效日<CnDatePicker data-testid="svc-fee-draft-from" value={draftFrom} onChange={(v) => setDraftFrom(v)} /></label>
              <button type="button" data-testid="svc-fee-draft-submit" onClick={() => void submitFeeDraft(feeDraftFor)}>提交草案</button>
              <button type="button" data-testid="svc-fee-draft-cancel" onClick={() => setFeeDraftFor(null)}>取消</button>
            </div>
          ) : null}
        </div>
        )}
        <div className="admin-toolbar">
          <label>服务单日期:<CnDatePicker data-testid="svc-date" value={date} onChange={(v) => setDate(v || today())} /></label>
        </div>
        <h3>服务单(公司代收;完成时分账)</h3>
        {kpiFocus ? (
          <div className="admin-state" data-testid="service-orders-kpi-focus-banner">
            正在查看：{kpiTitle(kpiFocus)}（{visibleOrders.length}）
            <button type="button" data-testid="service-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
          <div className="admin-kpi-grid" data-testid="service-orders-summary">
            <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.orderCount}</span><button type="button" className="admin-kpi-value" data-testid="service-orders-kpi-orders" onClick={() => enterKpiFocus("orders")}>{orders.length}</button><small>{TEXT.orderCountHelp}</small></div>
            <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.paidCount}</span><button type="button" className="admin-kpi-value" data-testid="service-orders-kpi-paid" onClick={() => enterKpiFocus("paid")}>{paidCount}</button><small>{TEXT.paidHelp}</small></div>
            <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.completedCount}</span><button type="button" className="admin-kpi-value" data-testid="service-orders-kpi-completed" onClick={() => enterKpiFocus("completed")}>{completedCount}</button><small>{TEXT.completedHelp}</small></div>
          </div>
        )}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="service-orders-table">
            <thead><tr><th>单号</th><th>服务</th><th>价格</th><th>桌台</th><th>履约人</th><th>管理费</th><th>舞者应得</th><th>状态</th><th>操作</th><th>详情</th></tr></thead>
            <tbody>
              {visibleOrders.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`service-order-row-${row.id}`}
                  onClick={() => openOrderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOrderDetail(row); } }}>
                  <td>{row.id}</td>
                  <td>{row.itemName ?? "—"}</td>
                  <td>{money(row.priceCent)}</td>
                  <td>{row.tableId ?? "—"}</td>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.performerUserId}>{row.performerUserId ?? "—"}</TraceLink></td>
                  <td>{money(row.mgmtFeeCent)}</td>
                  <td>{money(row.dancerShareCent)}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}{row.cancelReason ? `(${row.cancelReason})` : ""}</td>
                  <td onClick={(event) => event.stopPropagation()}>
                    {row.status === "created" || row.status === "paid" || row.status === "pending_accept" || row.status === "accepted" ? (
                      <button type="button" data-testid={`svc-cancel-${row.id}`} onClick={() => void cancelOrder(row.id)}>取消</button>
                    ) : "—"}
                  </td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看服务单详情" onClick={(event) => { event.stopPropagation(); openOrderDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
