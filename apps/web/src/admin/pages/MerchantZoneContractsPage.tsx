import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, MerchantZoneContractRow } from "../types";

const TEXT = {
  title: "我的合同",
  description: "本商户合同的关键信息(租期 / 空间 / 状态 / 保证金)只读;改价、变更与审批仍走平台侧权威流程。",
  loadFailed: "合同读取失败",
  focusClear: "清除筛选，返回全部",
};

const STATUS_LABELS: Record<string, string> = {
  draft: "起草中",
  pending_approval: "审批中",
  pending_confirmation: "待确认",
  effective: "履约中",
  expired: "已到期",
  pending_settlement: "结算中",
  terminated: "已终止",
  renewed: "已续签",
};

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

function money(cent: number | null | undefined) {
  if (cent === undefined || cent === null) return "-";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} CNY`;
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染合同表对应子集。
// URL 约定：?kpi=contracts[&subset=effective]。TraceLink（合同/空间下钻）在聚焦表内保留。
type KpiFocus = { subset: "all" | "effective" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "contracts") return null;
  return { subset: params.get("subset") === "effective" ? "effective" : "all" };
}

export function MerchantZoneContractsPage({ api, setGlobalError, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneContractRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该合同完整字段（日期中文·金额¥·状态中文·合同/空间引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openContractDetail(row: MerchantZoneContractRow) {
    setDetail({
      title: `合同明细 · ${shortId(row.id)}`,
      fields: [
        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.id}>{row.id}</TraceLink> },
        { label: "租期", value: `${cnDate(row.effectiveDate)} ~ ${cnDate(row.expiryDate)}` },
        {
          label: "空间",
          value: (row.spaceIds ?? []).length > 0
            ? (row.spaceIds ?? []).map((sid, i) => <React.Fragment key={sid}>{i > 0 ? "、" : ""}<TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={sid}>{sid}</TraceLink></React.Fragment>)
            : "—",
        },
        { label: "状态", value: STATUS_LABELS[row.lifecycleStatus ?? ""] ?? row.lifecycleStatus ?? "—" },
        { label: "保证金", value: money(row.depositCent) },
        {
          label: "续签自",
          value: row.renewedFromContractId
            ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.renewedFromContractId}>{row.renewedFromContractId}</TraceLink>
            : "—",
        },
      ],
    });
  }

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: "all" | "effective") {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "contracts");
    if (subset === "effective") params.set("subset", "effective");
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneContractRow[]>("/api/v1/merchant-portal/contracts"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const effectiveRows = rows.filter((row) => row.lifecycleStatus === "effective");
  const activeCount = effectiveRows.length;
  const focusRows = kpiFocus?.subset === "effective" ? effectiveRows : rows;
  const paged = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus ? kpiFocus.subset : "");

  const contractsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-contracts-table">
        <thead>
          <tr><th>合同</th><th>租期</th><th>空间</th><th>状态</th><th>保证金</th><th>续签自</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-contract-row-${row.id}`}
              onClick={() => openContractDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}>
              <td className="admin-id-cell" title={row.id} onClick={(event) => event.stopPropagation()}>
                <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.id}>{shortId(row.id)}</TraceLink>
              </td>
              <td>{cnDate(row.effectiveDate)} ~ {cnDate(row.expiryDate)}</td>
              <td onClick={(event) => event.stopPropagation()}>{(row.spaceIds ?? []).length > 0
                ? (row.spaceIds ?? []).map((sid, i) => <React.Fragment key={sid}>{i > 0 ? "、" : ""}<TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={sid}>{sid}</TraceLink></React.Fragment>)
                : "-"}</td>
              <td>{STATUS_LABELS[row.lifecycleStatus ?? ""] ?? row.lifecycleStatus ?? "-"}</td>
              <td className="admin-num">{money(row.depositCent)}</td>
              <td className="admin-id-cell" title={row.renewedFromContractId ?? undefined} onClick={(event) => event.stopPropagation()}>
                {row.renewedFromContractId
                  ? <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.renewedFromContractId}>{shortId(row.renewedFromContractId)}</TraceLink>
                  : shortId(row.renewedFromContractId)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <TablePager testid="merchant-zone-contracts-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );

  return (
    <section data-testid="merchant-zone-contracts-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_portal.contract.viewed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="merchant-zone-contracts-kpi-focus-banner">
              正在查看：{kpiFocus.subset === "effective" ? "履约中合同" : "合同总数"}（{focusRows.length}）
              <button type="button" data-testid="merchant-zone-contracts-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {contractsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-zone-contracts-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>合同总数</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-contracts-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>履约中</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-contracts-kpi-effective" onClick={() => enterKpiFocus("effective")}>{activeCount}</button><small>当前有效</small></div>
            </div>
            {contractsTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
