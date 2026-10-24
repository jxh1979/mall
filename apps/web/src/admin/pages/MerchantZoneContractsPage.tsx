// 文件版本：V0.03
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, MerchantZoneContractRow } from "../types";
import { MerchantCardExitNoticeSection } from "./CustomerFacingSettingsSections";

const TEXT = {
  title: "我的合同",
  description: "本商户合同的租期、到期情况和状态只读；改价、变更、关键条款与审批仍走平台侧权威流程。",
  loadFailed: "合同读取失败",
  focusClear: "清除筛选，返回全部",
};

const STATUS_LABELS: Record<string, string> = {
  drafting: "草拟",
  pending_confirmation: "待确认",
  pending_effective: "待生效",
  effective: "已生效",
  renewing: "续签中",
  expired: "已到期",
  terminated: "已终止",
  archived: "归档",
};

// M023-003：未知码给中文占位，不把英文原码显示给用户。
const UNKNOWN_STATUS = "未知状态";

function statusLabel(code: string | null | undefined): string {
  if (!code) return "-";
  return STATUS_LABELS[code] ?? UNKNOWN_STATUS;
}

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

function money(cent: number | null | undefined) {
  if (cent === undefined || cent === null) return "-";
  return `${new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(cent / 100)} 元`;
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染合同表对应子集。
// URL 约定：?kpi=contracts[&subset=effective]。
// M023-005：本页只对商户账号开放，合同工作台/空间页需平台权限、商户点了没反应，故合同号/空间/续签自一律纯文本（整行点开明细抽屉）。
type KpiFocus = { subset: "all" | "effective" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "contracts") return null;
  return { subset: params.get("subset") === "effective" ? "effective" : "all" };
}

export function MerchantZoneContractsPage({ api, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneContractRow[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该合同完整字段（日期中文·金额元·状态中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openContractDetail(row: MerchantZoneContractRow) {
    setDetail({
      title: `合同明细 · ${shortId(row.id)}`,
      fields: [
        { label: "合同", value: row.id },
        { label: "租期", value: `${cnDate(row.effectiveDate)} ~ ${cnDate(row.expiryDate)}` },
        {
          label: "空间",
          value: (row.spaceIds ?? []).length > 0 ? (row.spaceIds ?? []).join("、") : "—",
        },
        { label: "状态", value: statusLabel(row.lifecycleStatus) },
        { label: "押金", value: money(row.depositCent) },
        {
          label: "续签自",
          value: row.renewedFromContractId ?? "—",
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
          <tr><th>合同</th><th>租期</th><th>空间</th><th>状态</th><th>押金</th><th>续签自</th><th>详情</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-contract-row-${row.id}`}
              onClick={() => openContractDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}>
              <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td>
              <td>{cnDate(row.effectiveDate)} ~ {cnDate(row.expiryDate)}</td>
              <td>{(row.spaceIds ?? []).length > 0 ? (row.spaceIds ?? []).join("、") : "-"}</td>
              <td>{statusLabel(row.lifecycleStatus)}</td>
              <td className="admin-num">{money(row.depositCent)}</td>
              <td className="admin-id-cell" title={row.renewedFromContractId ?? undefined}>{shortId(row.renewedFromContractId)}</td>
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看合同详情" onClick={(event) => { event.stopPropagation(); openContractDetail(row); }}>查看详情</button></td></tr>
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
              正在查看：{kpiFocus.subset === "effective" ? "已生效合同" : "合同总数"}（{focusRows.length}）
              <button type="button" data-testid="merchant-zone-contracts-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {contractsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-zone-contracts-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>合同总数</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-contracts-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已生效</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-contracts-kpi-effective" onClick={() => enterKpiFocus("effective")}>{activeCount}</button><small>当前有效</small></div>
            </div>
            {contractsTable}
          </>
        )}
      </AdminDataState>
      <MerchantCardExitNoticeSection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
