import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";

type CatalogRow = { id: string; name?: string; priceCent?: number; durationMinutes?: number; active?: boolean };
type OrderRow = { id: string; itemName?: string; priceCent?: number; tableId?: string; performerUserId?: string; orderedBy?: string; status?: string; cancelReason?: string; paidAt?: string; mgmtFeeCent?: number; dancerShareCent?: number; createdAt?: string };

const TEXT = {
  title: "服务点单",
  description: "公司统一账户代收(2026-07-02最终拍板):客人扫公司收款码付款→服务员确认收款→舞者上台→完成时分账(管理费归公司,余额进舞者薪资提成,月结代扣个税)。履约前可取消(已收款的标待退款),完成后不可退。",
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

export function ServiceOrdersPage({ api, setGlobalError, setActionResult, navigateTo }: AdminPageProps) {
  const [date, setDate] = React.useState(today);
  const [catalog, setCatalog] = React.useState<CatalogRow[]>([]);
  const [orders, setOrders] = React.useState<OrderRow[]>([]);
  const [itemName, setItemName] = React.useState("");
  const [itemPrice, setItemPrice] = React.useState("");
  const [itemMinutes, setItemMinutes] = React.useState("");
  const [feeMode, setFeeMode] = React.useState("none");
  const [feeValue, setFeeValue] = React.useState("");
  const [companyQr, setCompanyQr] = React.useState("");
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

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [catalogRows, orderRows] = await Promise.all([
        api.get<CatalogRow[]>("/api/v1/services/catalog/all"),
        api.get<OrderRow[]>(`/api/v1/services/orders?date=${date}`),
      ]);
      setCatalog(catalogRows ?? []);
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

  const saveItem = async () => {
    const cent = Math.round(Number(itemPrice) * 100);
    if (!itemName.trim() || !Number.isFinite(cent) || cent < 0) {
      setGlobalError("请填 服务名与非负参考价(元)");
      return;
    }
    await api.post("/api/v1/services/catalog", {
      name: itemName.trim(), priceCent: cent,
      durationMinutes: itemMinutes ? Number(itemMinutes) : null, active: true,
      mgmtFeeMode: feeMode,
      mgmtFeeValue: feeMode === "percent" ? Math.round(Number(feeValue) * 100) : feeMode === "fixed" ? Math.round(Number(feeValue) * 100) : 0,
    });
    setActionResult({ action: "service_catalog_saved", name: itemName.trim() });
    setItemName(""); setItemPrice(""); setItemMinutes("");
    await reload();
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
      ],
    });
  }

  return (
    <section data-testid="service-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="service.catalog.saved / service.order.created / service.order.cancelled" />
      <h3>服务目录(固定参考价)</h3>
      <div className="admin-form-grid" data-testid="service-catalog-editor">
        <label>服务名<input data-testid="svc-name-input" value={itemName} onChange={(event) => setItemName(event.target.value)} placeholder="点舞" /></label>
        <label>参考价(元)<input data-testid="svc-price-input" value={itemPrice} onChange={(event) => setItemPrice(event.target.value)} /></label>
        <label>时长(分钟)<input data-testid="svc-minutes-input" value={itemMinutes} onChange={(event) => setItemMinutes(event.target.value)} /></label>
        <label>管理费<select data-testid="svc-fee-mode" value={feeMode} onChange={(event) => setFeeMode(event.target.value)}><option value="none">免收</option><option value="percent">比例(%)</option><option value="fixed">固定(元)</option></select></label>
        <label>管理费值<input data-testid="svc-fee-value" value={feeValue} onChange={(event) => setFeeValue(event.target.value)} placeholder="10 或 30" /></label>
        <button type="button" data-testid="svc-save" onClick={() => void saveItem()}>保存目录项</button>
      </div>
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
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="service-catalog-table">
            <thead><tr><th>服务</th><th>参考价</th><th>时长</th><th>状态</th></tr></thead>
            <tbody>
              {catalog.map((row) => (
                <tr key={row.id}>
                  <td>{row.name ?? "—"}</td>
                  <td>{money(row.priceCent)}</td>
                  <td>{row.durationMinutes ? `${row.durationMinutes} 分钟` : "—"}</td>
                  <td>{row.active ? "上架" : "下架"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-toolbar">
          <label>服务单日期:<input data-testid="svc-date" type="date" value={date} onChange={(event) => setDate(event.target.value || today())} /></label>
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
            <thead><tr><th>单号</th><th>服务</th><th>价格</th><th>桌台</th><th>履约人</th><th>管理费</th><th>舞者应得</th><th>状态</th><th>操作</th></tr></thead>
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
