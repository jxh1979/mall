// 文件版本：V0.01
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

// M023-001：行类型按后端真实返回收口（MerchantWorkOrderRecord / MerchantBillRecord），不读后端不存在的字段。
type WorkOrderRow = { id: string; merchantId?: string; workOrderType?: string; priority?: string; status?: string; title?: string };
type BillRow = { id: string; merchantId?: string; billNo?: string; totalAmountCent?: number | null; status?: string };

const TEXT = {
  title: "商户授权工作台",
  description: "商户侧只查看已授权业务范围内的工单和账单；平台治理、财务总账、第三方配置等跨主体能力继续由权限网关拦截。",
  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.service_work_order.view、merchant.billing.view 控制。",
  boundary: "商户角色不得访问平台治理、财务总账、第三方配置和其它商户数据；真实多商户隔离仍以后端权限和数据范围策略为准，本页只展示当前授权范围。",
  empty: "暂无授权商户数据",
  loadFailed: "商户授权数据读取失败",
  scopeMissing: "当前账号缺少商户授权范围，无法展示任何商户数据，请联系管理员配置。",
  focusClear: "清除筛选，返回全部",
};

// M023-003：账单状态用页内字典（与商户专区「我的账单」同词）；未知码给中文占位，不回显英文原码。
const BILL_STATUS_LABELS: Record<string, string> = {
  issued: "已出账",
  viewed: "已查看",
  confirmed: "已确认",
  disputed: "有异议",
  overdue: "已逾期",
  closed: "已结清",
};
const UNKNOWN = "未知状态";

function billStatus(code: string | null | undefined): string {
  if (!code) return "-";
  return BILL_STATUS_LABELS[code] ?? UNKNOWN;
}

function zh(code: string | null | undefined): string {
  if (!code) return "-";
  const label = formatBusinessStatus(code);
  return label === code ? UNKNOWN : label;
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<workOrders|bills>；点 KPI 数字=带参重进本页只渲染对应表。
// 账单金额是汇总口径，无独立承载表，其 KPI 数字保持纯文本不可点。
type KpiTarget = "workOrders" | "bills";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "workOrders" && table !== "bills") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "bills" ? "商户账单" : "商户工单";
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  return String(value);
}

function moneyCent(cent: number | null | undefined) {
  if (cent === null || cent === undefined || !Number.isFinite(cent)) return "-";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
}

function matchesMerchant(merchantId: string | null | undefined, rowMerchantId: string | undefined) {
  if (!merchantId) return false;
  return rowMerchantId === merchantId;
}

export function MerchantScopePage({ api, user, setGlobalError }: AdminPageProps) {
  const [workOrders, setWorkOrders] = React.useState<WorkOrderRow[]>([]);
  const [bills, setBills] = React.useState<BillRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该工单/账单完整字段（枚举中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
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

  const scopeMissing = !user.merchantId;

  const { loading, error, reload } = useAdminResource(
    async () => {
      if (scopeMissing) {
        setWorkOrders([]);
        setBills([]);
        throw new Error(TEXT.scopeMissing);
      }
      const [workOrderRows, billRows] = await Promise.all([
        api.get<WorkOrderRow[]>("/api/v1/merchant-service/work-orders"),
        api.get<BillRow[]>("/api/v1/merchant-billing/bills"),
      ]);
      setWorkOrders(workOrderRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
      setBills(billRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const totalBillCent = bills.reduce((sum, row) => sum + Number(row.totalAmountCent ?? 0), 0);
  const hasRows = workOrders.length > 0 || bills.length > 0;

  function openWorkOrderDetail(row: WorkOrderRow) {
    setDetail({
      title: `授权工单明细 · ${display(row.title)}`,
      fields: [
        { label: "商户", value: display(row.merchantId) },
        { label: "类型", value: zh(row.workOrderType) },
        { label: "标题", value: display(row.title) },
        { label: "优先级", value: zh(row.priority) },
        { label: "状态", value: zh(row.status) },
      ],
    });
  }

  function openBillDetail(row: BillRow) {
    setDetail({
      title: `授权账单明细 · ${display(row.billNo)}`,
      fields: [
        { label: "商户", value: display(row.merchantId) },
        { label: "账单号", value: display(row.billNo) },
        { label: "金额", value: moneyCent(row.totalAmountCent) },
        { label: "状态", value: billStatus(row.status) },
      ],
    });
  }

  const workOrdersSection = (
    <>
      <h3>授权工单</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="merchant-scope-work-orders-table">
          <thead><tr><th>商户</th><th>类型</th><th>标题</th><th>优先级</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{workOrders.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-work-order-row-${row.id}`} onClick={() => openWorkOrderDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}><td>{display(row.merchantId)}</td><td>{zh(row.workOrderType)}</td><td>{display(row.title)}</td><td>{zh(row.priority)}</td><td>{zh(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  const billsSection = (
    <>
      <h3>授权账单</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="merchant-scope-bills-table">
          <thead><tr><th>商户</th><th>账单号</th><th>金额</th><th>状态</th><th>详情</th></tr></thead>
          <tbody>{bills.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-scope-bill-row-${row.id}`} onClick={() => openBillDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBillDetail(row); } }}><td>{display(row.merchantId)}</td><td>{display(row.billNo)}</td><td className="admin-num">{moneyCent(row.totalAmountCent)}</td><td>{billStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看授权账单详情" onClick={(event) => { event.stopPropagation(); openBillDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "bills" ? bills.length : workOrders.length) : 0;

  return (
    <section data-testid="merchant-scope-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="merchant-scope-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="merchant_service.work_order.viewed / merchant_billing.bill.viewed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {!hasRows ? <div className="admin-state admin-empty" data-testid="merchant-scope-empty">{TEXT.empty}</div> : kpiFocus ? (
          <>
            <div className="admin-state" data-testid="merchant-scope-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{focusCount}）
              <button type="button" data-testid="merchant-scope-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "bills" ? billsSection : workOrdersSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-scope-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户工单</span><button type="button" className="admin-kpi-value" data-testid="merchant-scope-kpi-work-orders" onClick={() => enterKpiFocus("workOrders")}>{workOrders.length}</button><small>当前商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>商户账单</span><button type="button" className="admin-kpi-value" data-testid="merchant-scope-kpi-bills" onClick={() => enterKpiFocus("bills")}>{bills.length}</button><small>授权账单</small></div>
              <div className="admin-kpi-card"><span>账单金额</span><strong data-testid="merchant-scope-kpi-bill-amount">{moneyCent(totalBillCent)}</strong><small>只读汇总</small></div>
            </div>

            <div className="admin-form-row">
              <span className="admin-muted-action" data-testid="merchant-scope-boundary">{TEXT.boundary}</span>
            </div>

            {workOrdersSection}
            {billsSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
