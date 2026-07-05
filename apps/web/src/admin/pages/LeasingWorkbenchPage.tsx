import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 招商前漏斗行类型（后端 leasing.* 记录的前端视图，camelCase 由 adminClient 统一转换）。
interface LeasingLeadRow {
  id: string;
  contactName?: string;
  contactPhone?: string;
  source?: string;
  intendedBusinessType?: string;
  intendedSpaceId?: string;
  brandId?: string;
  ownerUserId?: string;
  status?: string;
  lastFollowUpAt?: string;
}
interface LeasingIntentRow {
  id: string;
  leadId?: string;
  stage?: string;
  intendedSpaceId?: string;
  lastStageChangeAt?: string;
}
interface LeasingBrandRow {
  id: string;
  brandName?: string;
  brandType?: string;
  businessType?: string;
  blacklistReason?: string;
}
interface LeasingFollowUpRow {
  id: string;
  refType?: string;
  refId?: string;
  method?: string;
  content?: string;
  nextPlanAt?: string;
}

const TEXT = {
  title: "招商工作台",
  description: "招商获客前漏斗：线索建档 → 跟进 → 租赁意向推进 → 签约转商户合同草稿；品牌库与黑名单维护。所有写动作带幂等键，签约直接生成合同草稿进入合同生命周期。",
  evidence: "接口证据：线索 /api/v1/leasing/leads（含 assign/status/follow-up-due）；意向 /api/v1/leasing/intents（advance/sign→合同草稿）；品牌 /api/v1/leasing/brands（blacklist）；跟进 /api/v1/leasing/follow-ups。",
  loadFailed: "招商前漏斗读取失败",
  actionFailed: "招商前漏斗操作失败",
  noLead: "请先选中一条线索。",
  noIntent: "请先选中一条意向。",
  noBrand: "请先选中一个品牌。",
  contactRequired: "创建线索必须填写联系人。",
  leadRequired: "创建意向必须先选中一条线索。",
  focusClear: "清除筛选，返回全部",
};

// 招商域内枚举中文（全局状态表未含，页内映射；表单下拉 option 已是中文，此处补表格展示列）。
const SOURCE_LABELS: Record<string, string> = { outbound: "外拓", online_inquiry: "线上咨询", referral: "转介绍", exhibition: "展会" };
// 线索 7 态（后端 LEAD_STATUSES，全局状态表未含，页内映射；表格+抽屉状态列共用）。
const LEAD_STATUS_LABELS: Record<string, string> = { pending_follow_up: "待跟进", following_up: "跟进中", converted_to_intent: "已转意向", negotiating: "谈判中", signed: "已签约", lost: "流失", on_hold: "暂缓" };
const STAGE_LABELS: Record<string, string> = { requirement_confirmed: "需求确认", proposal_quoted: "方案报价", negotiation: "商务谈判", pending_signing: "待签约" };
const BRAND_TYPE_LABELS: Record<string, string> = { chain: "连锁", single: "单店", flagship: "旗舰", franchise: "加盟" };
const BUSINESS_TYPE_LABELS: Record<string, string> = { retail: "零售", catering: "餐饮", entertainment: "娱乐", service: "服务" };
const METHOD_LABELS: Record<string, string> = { phone: "电话", visit: "拜访", wechat: "微信" };
const REF_TYPE_LABELS: Record<string, string> = { lead: "线索", intent: "意向" };
function label(map: Record<string, string>, code: string | null | undefined): string {
  if (!code) return "-";
  return map[code] ?? code;
}

// KPI 聚焦（照样板）：?kpi=<leads|intents|brands>[&subset=following]。
// 线索总数→leads，跟进中→leads subset=following（未签约/未流失客户端过滤），意向数→intents，品牌库→brands。
type KpiTable = "leads" | "intents" | "brands";
type KpiFocus = { table: KpiTable; subset: string | null };

function isFollowing(row: LeasingLeadRow) {
  return !["signed", "lost", "closed"].includes(row.status ?? "");
}

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  const table = params.get("kpi");
  if (table !== "leads" && table !== "intents" && table !== "brands") return null;
  return { table, subset: params.get("subset") };
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function LeasingWorkbenchPage({ api, setActionResult, setGlobalError, navigateTo }: AdminPageProps) {
  const [leads, setLeads] = React.useState<LeasingLeadRow[]>([]);
  const [intents, setIntents] = React.useState<LeasingIntentRow[]>([]);
  const [brands, setBrands] = React.useState<LeasingBrandRow[]>([]);
  const [followUps, setFollowUps] = React.useState<LeasingFollowUpRow[]>([]);
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·日期中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const lead = useSelectedRow(leads);
  const intent = useSelectedRow(intents);
  const brand = useSelectedRow(brands);

  // 线索表单
  const [contactName, setContactName] = React.useState("张经理");
  const [contactPhone, setContactPhone] = React.useState("13800000000");
  const [source, setSource] = React.useState("exhibition");
  const [intendedBusinessType, setIntendedBusinessType] = React.useState("retail");
  const [intendedSpaceId, setIntendedSpaceId] = React.useState("space-1");
  const [ownerUserId, setOwnerUserId] = React.useState("staff_sales");
  const [leadStatus, setLeadStatus] = React.useState("following_up");
  // 意向表单
  const [intentSpaceId, setIntentSpaceId] = React.useState("space-1");
  const [targetStage, setTargetStage] = React.useState("negotiation");
  const [signMerchantId, setSignMerchantId] = React.useState("merchant-1");
  const [signEffectiveDate, setSignEffectiveDate] = React.useState("2026-07-01");
  const [signExpiryDate, setSignExpiryDate] = React.useState("2027-06-30");
  // 品牌表单
  const [brandName, setBrandName] = React.useState("示例品牌");
  const [brandType, setBrandType] = React.useState("chain");
  const [brandBusinessType, setBrandBusinessType] = React.useState("retail");
  const [blacklistReason, setBlacklistReason] = React.useState("资质异常");
  // 跟进表单
  const [followMethod, setFollowMethod] = React.useState("phone");
  const [followContent, setFollowContent] = React.useState("已电话沟通，客户意向较强");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTable, subset: string | null = null) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    if (subset) params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table, subset });
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
      const [l, i, b, f] = await Promise.all([
        api.get<LeasingLeadRow[]>("/api/v1/leasing/leads"),
        api.get<LeasingIntentRow[]>("/api/v1/leasing/intents"),
        api.get<LeasingBrandRow[]>("/api/v1/leasing/brands"),
        api.get<LeasingFollowUpRow[]>("/api/v1/leasing/follow-ups"),
      ]);
      setLeads(l);
      setIntents(i);
      setBrands(b);
      setFollowUps(f);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  // 聚焦时线索客户端子集过滤（跟进中）。搜索/排序在完整视图内保留原逻辑。
  const focusLeads = kpiFocus?.table === "leads" && kpiFocus.subset === "following" ? leads.filter(isFollowing) : leads;
  const filteredLeads = focusLeads.filter((row) => rowMatchesKeyword(row, query));
  const leadSort = useSortedRows(filteredLeads, {
    contactName: (row) => row.contactName ?? row.id,
    source: (row) => row.source,
    status: (row) => row.status,
    owner: (row) => row.ownerUserId,
  });
  const pagedLeads = usePagedRows(leadSort.sorted, 10, `${kpiFocus?.subset ?? ""}:${query}:${leadSort.key}:${leadSort.direction}`);

  const followingCount = leads.filter(isFollowing).length;

  function openLeadDetail(row: LeasingLeadRow) {
    setDetail({
      title: `线索明细 · ${row.contactName || row.id}`,
      fields: [
        { label: "线索号", value: row.id },
        { label: "联系人", value: row.contactName ?? "-" },
        { label: "电话", value: row.contactPhone ?? "-" },
        { label: "来源", value: label(SOURCE_LABELS, row.source) },
        { label: "意向业态", value: label(BUSINESS_TYPE_LABELS, row.intendedBusinessType) },
        { label: "意向空间", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.intendedSpaceId}>{row.intendedSpaceId ?? "-"}</TraceLink> },
        { label: "关联品牌", value: row.brandId ?? "-" },
        { label: "负责人", value: row.ownerUserId ?? "-" },
        { label: "状态", value: label(LEAD_STATUS_LABELS, row.status) },
        { label: "最近跟进", value: cnDate(row.lastFollowUpAt) },
      ],
    });
  }

  function openIntentDetail(row: LeasingIntentRow) {
    setDetail({
      title: `租赁意向明细 · ${row.id}`,
      fields: [
        { label: "意向号", value: row.id },
        { label: "来源线索", value: row.leadId ?? "-" },
        { label: "阶段", value: label(STAGE_LABELS, row.stage) },
        { label: "意向空间", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.intendedSpaceId}>{row.intendedSpaceId ?? "-"}</TraceLink> },
        { label: "最近阶段变更", value: cnDate(row.lastStageChangeAt) },
      ],
    });
  }

  function openBrandDetail(row: LeasingBrandRow) {
    setDetail({
      title: `品牌明细 · ${row.brandName || row.id}`,
      fields: [
        { label: "品牌号", value: row.id },
        { label: "品牌", value: row.brandName ?? "-" },
        { label: "类型", value: label(BRAND_TYPE_LABELS, row.brandType) },
        { label: "业态", value: label(BUSINESS_TYPE_LABELS, row.businessType) },
        { label: "黑名单原因", value: row.blacklistReason || "-" },
      ],
    });
  }

  function openFollowUpDetail(row: LeasingFollowUpRow) {
    setDetail({
      title: `跟进明细 · ${row.id}`,
      fields: [
        { label: "跟进号", value: row.id },
        { label: "对象类型", value: label(REF_TYPE_LABELS, row.refType) },
        { label: "对象", value: row.refId ?? "-" },
        { label: "方式", value: label(METHOD_LABELS, row.method) },
        { label: "内容", value: row.content ?? "-" },
        { label: "下次计划", value: cnDate(row.nextPlanAt) },
      ],
    });
  }

  async function createLead() {
    if (!contactName.trim()) {
      setFormError(TEXT.contactRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("leasing-lead-create"),
      contactName,
      contactPhone,
      source,
      intendedBusinessType,
      intendedSpaceId,
      brandId: brand.selected?.id ?? null,
    };
    await runAction("leasing_lead_created", request, () => api.post("/api/v1/leasing/leads", request));
  }

  async function assignLead() {
    if (!lead.selected) {
      setFormError(TEXT.noLead);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-lead-assign"), ownerUserId };
    await runAction("leasing_lead_assigned", request, () => api.post(`/api/v1/leasing/leads/${lead.selected!.id}/assign`, request));
  }

  async function updateLeadStatus() {
    if (!lead.selected) {
      setFormError(TEXT.noLead);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-lead-status"), status: leadStatus, reason: "完整后台推进线索状态" };
    await runAction("leasing_lead_status_changed", request, () => api.post(`/api/v1/leasing/leads/${lead.selected!.id}/status`, request));
  }

  async function addFollowUp() {
    if (!lead.selected) {
      setFormError(TEXT.noLead);
      return;
    }
    const request = {
      idempotencyKey: nextKey("leasing-follow-up"),
      refType: "lead",
      refId: lead.selected.id,
      method: followMethod,
      content: followContent,
    };
    await runAction("leasing_follow_up_added", request, () => api.post("/api/v1/leasing/follow-ups", request));
  }

  async function createIntent() {
    if (!lead.selected) {
      setFormError(TEXT.leadRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-intent-create"), leadId: lead.selected.id, intendedSpaceId: intentSpaceId };
    await runAction("leasing_intent_created", request, () => api.post("/api/v1/leasing/intents", request));
  }

  async function advanceIntent() {
    if (!intent.selected) {
      setFormError(TEXT.noIntent);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-intent-advance"), targetStage, supervisorUserId: "staff_supervisor" };
    await runAction("leasing_intent_advanced", request, () => api.post(`/api/v1/leasing/intents/${intent.selected!.id}/advance`, request));
  }

  async function signIntent() {
    if (!intent.selected) {
      setFormError(TEXT.noIntent);
      return;
    }
    const request = {
      idempotencyKey: nextKey("leasing-intent-sign"),
      merchantId: signMerchantId,
      contractSubjectId: signMerchantId,
      operationSubjectId: signMerchantId,
      settlementSubjectId: signMerchantId,
      effectiveDate: signEffectiveDate,
      expiryDate: signExpiryDate,
    };
    await runAction("leasing_intent_signed", request, () => api.post(`/api/v1/leasing/intents/${intent.selected!.id}/sign`, request));
  }

  async function createBrand() {
    const request = { idempotencyKey: nextKey("leasing-brand-create"), brandName, brandType, businessType: brandBusinessType };
    await runAction("leasing_brand_created", request, () => api.post("/api/v1/leasing/brands", request));
  }

  async function blacklistBrand() {
    if (!brand.selected) {
      setFormError(TEXT.noBrand);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-brand-blacklist"), blacklist: true, reason: blacklistReason };
    await runAction("leasing_brand_blacklisted", request, () => api.post(`/api/v1/leasing/brands/${brand.selected!.id}/blacklist`, request));
  }

  const leadsTable = (
    <>
      <div className="admin-list-toolbar" data-testid="leasing-leads-filter">
        <input data-testid="leasing-leads-search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索联系人 / 来源 / 状态 / 负责人" />
        <span className="admin-muted-action" data-testid="leasing-leads-filter-count">匹配 {filteredLeads.length} / {focusLeads.length} 条</span>
      </div>
      {filteredLeads.length === 0 ? (
        <div className="admin-state admin-empty" data-testid="leasing-leads-empty">无匹配线索</div>
      ) : (
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="leasing-leads-table">
          <thead><tr><th>选择</th><th className="admin-sort-th" data-testid="leasing-leads-sort-contactName" onClick={() => leadSort.toggle("contactName")} {...leadSort.thProps("contactName")}>联系人{leadSort.indicator("contactName")}</th><th>电话</th><th className="admin-sort-th" data-testid="leasing-leads-sort-source" onClick={() => leadSort.toggle("source")} {...leadSort.thProps("source")}>来源{leadSort.indicator("source")}</th><th>意向业态</th><th className="admin-sort-th" data-testid="leasing-leads-sort-status" onClick={() => leadSort.toggle("status")} {...leadSort.thProps("status")}>状态{leadSort.indicator("status")}</th><th className="admin-sort-th" data-testid="leasing-leads-sort-owner" onClick={() => leadSort.toggle("owner")} {...leadSort.thProps("owner")}>负责人{leadSort.indicator("owner")}</th></tr></thead>
          <tbody>
            {pagedLeads.pageRows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-lead-row-${row.id}`}
                onClick={() => openLeadDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openLeadDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" data-testid={`leasing-lead-select-${row.id}`} checked={(lead.selectedId ?? "") === row.id} onChange={() => lead.setSelectedId(row.id)} /></td>
                <td>{row.contactName ?? row.id}</td>
                <td>{row.contactPhone ?? "-"}</td>
                <td>{label(SOURCE_LABELS, row.source)}</td>
                <td>{label(BUSINESS_TYPE_LABELS, row.intendedBusinessType)}</td>
                <td>{label(LEAD_STATUS_LABELS, row.status)}</td>
                <td>{row.ownerUserId ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <TablePager testid="leasing-leads-pager" page={pagedLeads.page} pageCount={pagedLeads.pageCount} total={pagedLeads.total} onPrev={pagedLeads.prev} onNext={pagedLeads.next} />
      </div>
      )}
    </>
  );

  const intentsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="leasing-intents-table">
        <thead><tr><th>选择</th><th>意向号</th><th>来源线索</th><th>阶段</th><th>意向空间</th></tr></thead>
        <tbody>
          {intents.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-intent-row-${row.id}`}
              onClick={() => openIntentDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openIntentDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" data-testid={`leasing-intent-select-${row.id}`} checked={(intent.selectedId ?? "") === row.id} onChange={() => intent.setSelectedId(row.id)} /></td>
              <td>{row.id}</td>
              <td>{row.leadId ?? "-"}</td>
              <td>{label(STAGE_LABELS, row.stage)}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.intendedSpaceId}>{row.intendedSpaceId ?? "-"}</TraceLink></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const brandsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="leasing-brands-table">
        <thead><tr><th>选择</th><th>品牌</th><th>类型</th><th>业态</th><th>黑名单原因</th></tr></thead>
        <tbody>
          {brands.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-brand-row-${row.id}`}
              onClick={() => openBrandDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBrandDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" data-testid={`leasing-brand-select-${row.id}`} checked={(brand.selectedId ?? "") === row.id} onChange={() => brand.setSelectedId(row.id)} /></td>
              <td>{row.brandName ?? row.id}</td>
              <td>{label(BRAND_TYPE_LABELS, row.brandType)}</td>
              <td>{label(BUSINESS_TYPE_LABELS, row.businessType)}</td>
              <td>{row.blacklistReason || "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  function kpiFocusTitle(): string {
    if (!kpiFocus) return "";
    if (kpiFocus.table === "leads") return kpiFocus.subset === "following" ? "跟进中线索" : "线索总数";
    if (kpiFocus.table === "intents") return "租赁意向";
    return "品牌库";
  }
  const focusCount = kpiFocus
    ? kpiFocus.table === "leads"
      ? focusLeads.length
      : kpiFocus.table === "intents"
        ? intents.length
        : brands.length
    : 0;

  return (
    <section data-testid="leasing-workbench-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="leasing-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="leasing.lead.created / assigned / status_changed · follow_up.added · intent.created / advanced / signed · brand.created / blacklisted" />
      <AdminDataState loading={loading} error={error} empty={leads.length === 0 && intents.length === 0 && brands.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="leasing-kpi-focus-banner">
              正在查看：{kpiFocusTitle()}（{focusCount}）
              <button type="button" data-testid="leasing-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "leads" ? leadsTable : kpiFocus.table === "intents" ? intentsTable : brandsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="leasing-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>线索总数</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-leads" onClick={() => enterKpiFocus("leads")}>{leads.length}</button><small>招商前漏斗</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>跟进中</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-following" onClick={() => enterKpiFocus("leads", "following")}>{followingCount}</button><small>未签约/未流失</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>意向数</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-intents" onClick={() => enterKpiFocus("intents")}>{intents.length}</button><small>租赁意向</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>品牌库</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-brands" onClick={() => enterKpiFocus("brands")}>{brands.length}</button><small>含黑名单</small></div>
            </div>

            {/* 线索建档 */}
            <div className="admin-form-grid" data-testid="leasing-lead-create-form">
              <label>联系人<input data-testid="leasing-lead-contact-name" value={contactName} onChange={(e) => setContactName(e.target.value)} /></label>
              <label>联系电话<input data-testid="leasing-lead-contact-phone" value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} /></label>
              <label>来源<select data-testid="leasing-lead-source" value={source} onChange={(e) => setSource(e.target.value)}><option value="outbound">外拓</option><option value="online_inquiry">线上咨询</option><option value="referral">转介绍</option><option value="exhibition">展会</option></select></label>
              <label>意向业态<input data-testid="leasing-lead-business-type" value={intendedBusinessType} onChange={(e) => setIntendedBusinessType(e.target.value)} /></label>
              <label>意向空间<input data-testid="leasing-lead-space" value={intendedSpaceId} onChange={(e) => setIntendedSpaceId(e.target.value)} /></label>
            </div>
            <div className="admin-form-row">
              <button type="button" data-testid="leasing-lead-create" disabled={busyAction !== ""} onClick={() => void createLead()}>创建线索</button>
              <span className="admin-muted-action" data-testid="leasing-boundary">招商前漏斗为后台主数据；签约直接生成合同草稿，进入合同生命周期工作台审批。</span>
            </div>

            {/* 线索动作 */}
            <div className="admin-form-grid" data-testid="leasing-lead-action-form">
              <label>负责人<input data-testid="leasing-lead-owner" value={ownerUserId} onChange={(e) => setOwnerUserId(e.target.value)} /></label>
              <label>线索状态<select data-testid="leasing-lead-status" value={leadStatus} onChange={(e) => setLeadStatus(e.target.value)}><option value="following_up">跟进中</option><option value="converted_to_intent">已转意向</option><option value="negotiating">谈判中</option><option value="lost">流失</option><option value="on_hold">暂缓</option></select></label>
              <label>跟进方式<select data-testid="leasing-follow-method" value={followMethod} onChange={(e) => setFollowMethod(e.target.value)}><option value="phone">电话</option><option value="visit">拜访</option><option value="wechat">微信</option></select></label>
              <label>跟进内容<input data-testid="leasing-follow-content" value={followContent} onChange={(e) => setFollowContent(e.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="leasing-lead-actions">
              <button type="button" data-testid="leasing-lead-assign" disabled={busyAction !== ""} onClick={() => void assignLead()}>指派</button>
              <button type="button" data-testid="leasing-lead-status-update" disabled={busyAction !== ""} onClick={() => void updateLeadStatus()}>更新状态</button>
              <button type="button" data-testid="leasing-follow-add" disabled={busyAction !== ""} onClick={() => void addFollowUp()}>添加跟进</button>
            </div>

            {/* 意向 + 签约 */}
            <div className="admin-form-grid" data-testid="leasing-intent-form">
              <label>意向空间<input data-testid="leasing-intent-space" value={intentSpaceId} onChange={(e) => setIntentSpaceId(e.target.value)} /></label>
              <label>推进阶段<select data-testid="leasing-intent-stage" value={targetStage} onChange={(e) => setTargetStage(e.target.value)}><option value="requirement_confirmed">需求确认</option><option value="proposal_quoted">方案报价</option><option value="negotiation">商务谈判</option><option value="pending_signing">待签约</option></select></label>
              <label>签约商户<input data-testid="leasing-sign-merchant" value={signMerchantId} onChange={(e) => setSignMerchantId(e.target.value)} /></label>
              <label>合同生效日<input type="date" data-testid="leasing-sign-effective-date" value={signEffectiveDate} onChange={(e) => setSignEffectiveDate(e.target.value)} /></label>
              <label>合同到期日<input type="date" data-testid="leasing-sign-expiry-date" value={signExpiryDate} onChange={(e) => setSignExpiryDate(e.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="leasing-intent-actions">
              <button type="button" data-testid="leasing-intent-create" disabled={busyAction !== ""} onClick={() => void createIntent()}>从线索建意向</button>
              <button type="button" data-testid="leasing-intent-advance" disabled={busyAction !== ""} onClick={() => void advanceIntent()}>推进意向</button>
              <button type="button" data-testid="leasing-intent-sign" disabled={busyAction !== ""} onClick={() => void signIntent()}>签约→合同草稿</button>
            </div>

            {/* 品牌库 */}
            <div className="admin-form-grid" data-testid="leasing-brand-form">
              <label>品牌名称<input data-testid="leasing-brand-name" value={brandName} onChange={(e) => setBrandName(e.target.value)} /></label>
              <label>品牌类型<input data-testid="leasing-brand-type" value={brandType} onChange={(e) => setBrandType(e.target.value)} /></label>
              <label>业态<input data-testid="leasing-brand-business" value={brandBusinessType} onChange={(e) => setBrandBusinessType(e.target.value)} /></label>
              <label>拉黑原因<input data-testid="leasing-brand-blacklist-reason" value={blacklistReason} onChange={(e) => setBlacklistReason(e.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="leasing-brand-actions">
              <button type="button" data-testid="leasing-brand-create" disabled={busyAction !== ""} onClick={() => void createBrand()}>录入品牌</button>
              <button type="button" data-testid="leasing-brand-blacklist" disabled={busyAction !== ""} onClick={() => void blacklistBrand()}>拉入黑名单</button>
            </div>
            {formError ? <div data-testid="leasing-error" className="admin-inline-error">{formError}</div> : null}

            {leadsTable}
            {intentsTable}
            {brandsTable}
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="leasing-follow-ups-table">
                <thead><tr><th>对象</th><th>方式</th><th>内容</th><th>下次计划</th></tr></thead>
                <tbody>
                  {followUps.map((row) => (
                    <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-follow-up-row-${row.id}`}
                      onClick={() => openFollowUpDetail(row)}
                      onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openFollowUpDetail(row); } }}>
                      <td>{label(REF_TYPE_LABELS, row.refType)}:{row.refId ?? "-"}</td>
                      <td>{label(METHOD_LABELS, row.method)}</td>
                      <td>{row.content ?? "-"}</td>
                      <td>{cnDate(row.nextPlanAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
