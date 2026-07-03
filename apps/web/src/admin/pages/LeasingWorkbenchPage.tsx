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
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function LeasingWorkbenchPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [leads, setLeads] = React.useState<LeasingLeadRow[]>([]);
  const [intents, setIntents] = React.useState<LeasingIntentRow[]>([]);
  const [brands, setBrands] = React.useState<LeasingBrandRow[]>([]);
  const [followUps, setFollowUps] = React.useState<LeasingFollowUpRow[]>([]);

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

  const filteredLeads = leads.filter((row) => rowMatchesKeyword(row, query));
  const leadSort = useSortedRows(filteredLeads, {
    contactName: (row) => row.contactName ?? row.id,
    source: (row) => row.source,
    status: (row) => row.status,
    owner: (row) => row.ownerUserId,
  });
  const pagedLeads = usePagedRows(leadSort.sorted, 10, `${query}:${leadSort.key}:${leadSort.direction}`);

  const followingCount = leads.filter((row) => !["signed", "lost", "closed"].includes(row.status ?? "")).length;

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

  return (
    <section data-testid="leasing-workbench-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="leasing-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="leasing.lead.created / assigned / status_changed · follow_up.added · intent.created / advanced / signed · brand.created / blacklisted" />
      <AdminDataState loading={loading} error={error} empty={leads.length === 0 && intents.length === 0 && brands.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="leasing-summary">
          <div className="admin-kpi-card"><span>线索总数</span><strong>{leads.length}</strong><small>招商前漏斗</small></div>
          <div className="admin-kpi-card"><span>跟进中</span><strong>{followingCount}</strong><small>未签约/未流失</small></div>
          <div className="admin-kpi-card"><span>意向数</span><strong>{intents.length}</strong><small>租赁意向</small></div>
          <div className="admin-kpi-card"><span>品牌库</span><strong>{brands.length}</strong><small>含黑名单</small></div>
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

        {/* 线索列表 */}
        <div className="admin-list-toolbar" data-testid="leasing-leads-filter">
          <input data-testid="leasing-leads-search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索联系人 / 来源 / 状态 / 负责人" />
          <span className="admin-muted-action" data-testid="leasing-leads-filter-count">匹配 {filteredLeads.length} / {leads.length} 条</span>
        </div>
        {filteredLeads.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="leasing-leads-empty">无匹配线索</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-leads-table">
            <thead><tr><th>选择</th><th className="admin-sort-th" data-testid="leasing-leads-sort-contactName" onClick={() => leadSort.toggle("contactName")} {...leadSort.thProps("contactName")}>联系人{leadSort.indicator("contactName")}</th><th>电话</th><th className="admin-sort-th" data-testid="leasing-leads-sort-source" onClick={() => leadSort.toggle("source")} {...leadSort.thProps("source")}>来源{leadSort.indicator("source")}</th><th>意向业态</th><th className="admin-sort-th" data-testid="leasing-leads-sort-status" onClick={() => leadSort.toggle("status")} {...leadSort.thProps("status")}>状态{leadSort.indicator("status")}</th><th className="admin-sort-th" data-testid="leasing-leads-sort-owner" onClick={() => leadSort.toggle("owner")} {...leadSort.thProps("owner")}>负责人{leadSort.indicator("owner")}</th></tr></thead>
            <tbody>
              {pagedLeads.pageRows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`leasing-lead-select-${row.id}`} checked={(lead.selectedId ?? "") === row.id} onChange={() => lead.setSelectedId(row.id)} /></td>
                  <td>{row.contactName ?? row.id}</td>
                  <td>{row.contactPhone ?? "-"}</td>
                  <td>{row.source ?? "-"}</td>
                  <td>{row.intendedBusinessType ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.ownerUserId ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="leasing-leads-pager" page={pagedLeads.page} pageCount={pagedLeads.pageCount} total={pagedLeads.total} onPrev={pagedLeads.prev} onNext={pagedLeads.next} />
        </div>
        )}

        {/* 意向列表 */}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-intents-table">
            <thead><tr><th>选择</th><th>意向 id</th><th>来源线索</th><th>阶段</th><th>意向空间</th></tr></thead>
            <tbody>
              {intents.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`leasing-intent-select-${row.id}`} checked={(intent.selectedId ?? "") === row.id} onChange={() => intent.setSelectedId(row.id)} /></td>
                  <td>{row.id}</td>
                  <td>{row.leadId ?? "-"}</td>
                  <td>{row.stage ?? "-"}</td>
                  <td>{row.intendedSpaceId ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* 品牌 + 跟进列表 */}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-brands-table">
            <thead><tr><th>选择</th><th>品牌</th><th>类型</th><th>业态</th><th>黑名单原因</th></tr></thead>
            <tbody>
              {brands.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`leasing-brand-select-${row.id}`} checked={(brand.selectedId ?? "") === row.id} onChange={() => brand.setSelectedId(row.id)} /></td>
                  <td>{row.brandName ?? row.id}</td>
                  <td>{row.brandType ?? "-"}</td>
                  <td>{row.businessType ?? "-"}</td>
                  <td>{row.blacklistReason || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-follow-ups-table">
            <thead><tr><th>对象</th><th>方式</th><th>内容</th><th>下次计划</th></tr></thead>
            <tbody>
              {followUps.map((row) => (
                <tr key={row.id}>
                  <td>{row.refType ?? "-"}:{row.refId ?? "-"}</td>
                  <td>{row.method ?? "-"}</td>
                  <td>{row.content ?? "-"}</td>
                  <td>{row.nextPlanAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
