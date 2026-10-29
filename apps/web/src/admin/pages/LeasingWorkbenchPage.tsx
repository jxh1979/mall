// 文件版本：V0.06
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { CnDatePicker } from "../components/CnDatePicker";
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
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { hasPermissions } from "../security/PermissionGate";
import { shortId } from "../format/ids";
import { ApiError } from "../../shared/apiError";
import type { GovernanceSpaceRow, BusinessEntityRow, AccessUserRow, GovernanceProjectRow } from "../types";

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
  rentTerms?: { rentBillingMode?: string } | null;
  pendingStage?: string | null;
}
interface LeasingBrandRow {
  id: string;
  brandName?: string;
  brandType?: string;
  businessType?: string;
  blacklistReason?: string;
}
// 第十八批第 30 条 黑名单例外放行单（PGM L502：记录操作人、审批人、理由、时间和关联线索或意向）。
interface LeasingBlacklistExceptionRow {
  id: string;
  brandId?: string;
  brandName?: string;
  targetType?: string;
  leadId?: string | null;
  reason?: string;
  requestedBy?: string;
  requestedAt?: string;
  status?: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
  usedLeadId?: string | null;
  usedIntentId?: string | null;
}
type ExceptionDraft = { targetType: "lead_create" | "intent_create"; brandId?: string; leadId?: string; label: string };
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
  description: "招商获客前漏斗：线索建档 → 跟进 → 租赁意向推进 → 签约转商户合同草稿；品牌库与黑名单维护。超过约定天数没跟进的线索在页首直接提醒（天数在「提醒与留存天数配置」页可改）。所有写动作带幂等键，签约直接生成合同草稿进入合同生命周期。",
  evidence: "接口证据：线索 /api/v1/leasing/leads（含 assign/status/follow-up-due）；意向 /api/v1/leasing/intents（advance/sign→合同草稿）；品牌 /api/v1/leasing/brands（blacklist）；跟进 /api/v1/leasing/follow-ups。",
  loadFailed: "招商前漏斗读取失败",
  actionFailed: "招商前漏斗操作失败",
  noLead: "请先选中一条线索。",
  noIntent: "请先选中一条意向。",
  noBrand: "请先选中一个品牌。",
  contactRequired: "创建线索必须填写联系人。",
  leadRequired: "创建意向必须先选中一条线索。",
  brandNameRequired: "录入品牌必须填写品牌名称。",
  focusClear: "清除筛选，返回全部",
  phoneReveal: "查看完整号码",
  phoneRevealFailed: "查看完整号码失败，请稍后重试",
};

// 招商域内枚举中文（全局状态表未含，页内映射；表单下拉 option 已是中文，此处补表格展示列）。
const SOURCE_LABELS: Record<string, string> = { outbound: "外拓", online_inquiry: "线上咨询", referral: "转介绍", exhibition: "展会" };
// 线索 7 态（后端 LEAD_STATUSES，全局状态表未含，页内映射；表格+抽屉状态列共用）；中文按 ENUM:248「招商线索状态」原文。
const LEAD_STATUS_LABELS: Record<string, string> = { pending_follow_up: "待跟进", following_up: "跟进中", converted_to_intent: "已转意向", negotiating: "洽谈中", signed: "已签约", lost: "已流失", on_hold: "已搁置" };
// 意向 5 阶段按 ENUM:249「招商意向阶段」原文；线索转意向起始即 initial_contact（后端 LeasingService 转意向）。
const STAGE_LABELS: Record<string, string> = { initial_contact: "初步接触", requirement_confirmed: "需求确认", proposal_quoted: "方案报价", negotiation: "商务谈判", pending_signing: "待签约" };
const BRAND_TYPE_LABELS: Record<string, string> = { intentional: "意向品牌", prospective: "潜在品牌", partner: "合作品牌", blacklist: "黑名单" };
// V348 业态五值（真源 HTML §商户管理）：线索意向业态签约时直接带入商户业态，故与商户业态同一词表。
// V348+V384 六值：与 merchants.merchant_type 同词表（签约自动建档要落进那张表的 CHECK）。
const BUSINESS_TYPES = ["bar", "catering", "retail", "amusement", "media", "other"];
const BUSINESS_TYPE_LABELS: Record<string, string> = { bar: "酒吧", catering: "餐饮", retail: "零售", amusement: "游乐", media: "传媒", other: "其他" };
const METHOD_LABELS: Record<string, string> = { phone: "电话", visit: "拜访", wechat: "微信" };
const REF_TYPE_LABELS: Record<string, string> = { lead: "线索", intent: "意向" };
// 第十八批第 30 条：意向须逐级推进（PGM L500），选中意向时推进阶段默认取下一阶段；进入方案报价、商务谈判须主管确认（PGM L502）。
const NEXT_STAGE: Record<string, string> = { initial_contact: "requirement_confirmed", requirement_confirmed: "proposal_quoted", proposal_quoted: "negotiation", negotiation: "pending_signing" };
const KEY_STAGES = ["proposal_quoted", "negotiation"];
const EXCEPTION_TARGET_LABELS: Record<string, string> = { lead_create: "新建线索", intent_create: "由线索新建意向" };
const EXCEPTION_STATUS_LABELS: Record<string, string> = { pending_approval: "待审批", approved: "已批准", rejected: "已驳回", used: "已使用" };
function isBlacklisted(error: unknown) {
  return error instanceof ApiError && error.detailCode === "LEASING_BRAND_BLACKLISTED";
}
function label(map: Record<string, string>, code: string | null | undefined): string {
  if (!code) return "-";
  return map[code] ?? code;
}

// KPI 聚焦（照样板）：?kpi=<leads|intents|brands>[&subset=following|due]。
// 线索总数→leads，跟进中→leads subset=following（未签约/未流失客户端过滤），
// 待跟进→leads subset=due（后端 /leads/follow-up-due 按可配天数判定，不在前端算），意向数→intents，品牌库→brands。
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

// 第 37 条（ENUM:248「招商线索状态」）：线索类结果的 status 先按页内中文映射再交给结果区——lost 与终端设备「已丢失」共用码，不能改全局表。
function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  const lead = action.startsWith("leasing_lead_") && result && typeof result === "object" ? (result as LeasingLeadRow) : null;
  return { action, idempotencyKey: request.idempotencyKey, result: lead ? { ...lead, status: label(LEAD_STATUS_LABELS, lead.status) } : result };
}

export function LeasingWorkbenchPage({ api, user, setActionResult, setGlobalError, navigateTo, effectivePermissions, refreshEffectivePermissions }: AdminPageProps) {
  const [leads, setLeads] = React.useState<LeasingLeadRow[]>([]);
  const [intents, setIntents] = React.useState<LeasingIntentRow[]>([]);
  const [brands, setBrands] = React.useState<LeasingBrandRow[]>([]);
  const [followUps, setFollowUps] = React.useState<LeasingFollowUpRow[]>([]);
  const [exceptions, setExceptions] = React.useState<LeasingBlacklistExceptionRow[]>([]);
  // 黑名单例外放行：被拦或在黑名单品牌上发起的申请草稿；按批准新建线索时用的放行单。
  const [exceptionDraft, setExceptionDraft] = React.useState<ExceptionDraft | null>(null);
  const [exceptionReason, setExceptionReason] = React.useState("");
  const [exceptionApprover, setExceptionApprover] = React.useState("");
  const [leadException, setLeadException] = React.useState<LeasingBlacklistExceptionRow | null>(null);
  // 到期待跟进线索（V219 提醒窗天数后台可配，判定在后端 /leads/follow-up-due，前端只展示）。
  const [dueLeads, setDueLeads] = React.useState<LeasingLeadRow[]>([]);
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·日期中文·引用 TraceLink）。
  // 线索明细带 lead：字段在渲染时现算（「电话」字段要跟着下面的 phoneReveal 变）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[]; lead?: LeasingLeadRow } | null>(null);
  // M724（治理地图第 871 行，操作员 2026-10-07 确认：「招商线索联系人的手机号另按本句处理：线索列表和操作结果一律只显示脱敏号码；
  // 经单独授权且查看权限有效的人员，可以在某一条线索上点“查看完整号码”，只显示这一条的完整号码，每点一次算查看一次并留痕，
  // 单次授权看一次即用掉。」）：点出来的完整号码只放这里（按线索 id），不并进 leads／dueLeads（搜索框对整行做匹配）、
  // 不进页面底部操作结果区；关抽屉或重新拉数即清。
  const [phoneReveal, setPhoneReveal] = React.useState<{ leadId: string; busy?: boolean; phone?: string; error?: string } | null>(null);
  const canViewFullPhone = hasPermissions(effectivePermissions, ["pii.contact_phone.view"]);

  const lead = useSelectedRow(leads);
  const intent = useSelectedRow(intents);
  const brand = useSelectedRow(brands);

  // 线索表单
  const [contactName, setContactName] = React.useState("张经理");
  const [contactPhone, setContactPhone] = React.useState("13800000000");
  const [source, setSource] = React.useState("exhibition");
  const [intendedBusinessType, setIntendedBusinessType] = React.useState("retail");
  const [intendedSpaceId, setIntendedSpaceId] = React.useState("");
  const [ownerUserId, setOwnerUserId] = React.useState("");
  const [leadStatus, setLeadStatus] = React.useState("following_up");
  // 意向表单
  const [intentSpaceId, setIntentSpaceId] = React.useState("");
  const [targetStage, setTargetStage] = React.useState("requirement_confirmed");
  const [intentApprover, setIntentApprover] = React.useState("");
  const [intentNote, setIntentNote] = React.useState("");
  const [signMerchantId, setSignMerchantId] = React.useState("");
  const [signSubjectId, setSignSubjectId] = React.useState("");
  const [spaces, setSpaces] = React.useState<GovernanceSpaceRow[]>([]);
  // 第十八批 E 段第 19 条（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」）：
  // 新建线索、录入品牌可选所属项目；能看到本人项目以外的项目就显示（18G 同根补扫：角色只勾了 B、只看得到 B 一个时也显示），
  // 不选＝后端按意向空间所在项目或本人所在项目落。
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const pickProject = projects.some((x) => x.id !== user?.organizationId);
  const [leadProjectId, setLeadProjectId] = React.useState("");
  const [brandProjectId, setBrandProjectId] = React.useState("");
  const [merchantList, setMerchantList] = React.useState<Array<{ id: string; displayName?: string; companyName?: string; merchantName?: string }>>([]);
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const [salesUsers, setSalesUsers] = React.useState<AccessUserRow[]>([]);
  const [signEffectiveDate, setSignEffectiveDate] = React.useState("2026-07-01");
  const [signExpiryDate, setSignExpiryDate] = React.useState("2027-06-30");
  // 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入」：选中纯提成意向时签约表单录入约定押金（元）。
  const [signDepositYuan, setSignDepositYuan] = React.useState("");
  // 品牌表单
  const [brandName, setBrandName] = React.useState("");
  const [brandType, setBrandType] = React.useState("intentional");
  const [brandBusinessType, setBrandBusinessType] = React.useState("retail");
  const [blacklistReason, setBlacklistReason] = React.useState("资质异常");
  // 跟进表单
  const [followMethod, setFollowMethod] = React.useState("phone");
  const [followContent, setFollowContent] = React.useState("已电话沟通，客户意向较强");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 引导式创建流（一期范式）：新建线索收起为主按钮 → 分步弹窗 → 确认创建。
  const [leadFlowOpen, setLeadFlowOpen] = React.useState(false);
  // 引导式流程 phase-5：新增品牌收起为主按钮 → 分步弹窗（线索/意向/跟进/拉黑表单不动）。
  const [brandFlowOpen, setBrandFlowOpen] = React.useState(false);

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
      const [l, i, b, f, d] = await Promise.all([
        api.get<LeasingLeadRow[]>("/api/v1/leasing/leads"),
        api.get<LeasingIntentRow[]>("/api/v1/leasing/intents"),
        api.get<LeasingBrandRow[]>("/api/v1/leasing/brands"),
        api.get<LeasingFollowUpRow[]>("/api/v1/leasing/follow-ups"),
        api.get<LeasingLeadRow[]>("/api/v1/leasing/leads/follow-up-due"),
      ]);
      setLeads(l);
      setIntents(i);
      setBrands(b);
      setFollowUps(f);
      setDueLeads(d ?? []);
      try {
        setExceptions((await api.get<LeasingBlacklistExceptionRow[]>("/api/v1/leasing/blacklist-exceptions")) ?? []);
      } catch { setExceptions([]); }
      try {
        const [sp, mc, en, us] = await Promise.all([
          api.get<GovernanceSpaceRow[]>("/api/v1/platform/spaces"),
          api.get<Array<{ id: string; displayName?: string; companyName?: string; merchantName?: string }>>("/api/v1/platform/merchants"),
          api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities"),
          api.get<AccessUserRow[]>("/api/v1/access/users"),
        ]);
        setSpaces(sp ?? []); setMerchantList(mc ?? []); setEntities(en ?? []); setSalesUsers(us ?? []);
      } catch { /* 无权限→保持空字典 */ }
      try {
        setProjects((await api.get<GovernanceProjectRow[]>("/api/v1/platform/projects")) ?? []);
      } catch { setProjects([]); }
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => { setGlobalError(null); setPhoneReveal(null); } },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  // 聚焦时线索子集过滤：跟进中=客户端按状态过滤；待跟进=后端 /leads/follow-up-due 的结果（天数口径在后端，
  // 前端不复刻判定，改后台提醒窗天数即刻改变这张单子）。搜索/排序在完整视图内保留原逻辑。
  const focusLeads = kpiFocus?.table === "leads" && kpiFocus.subset === "following"
    ? leads.filter(isFollowing)
    : kpiFocus?.table === "leads" && kpiFocus.subset === "due"
      ? dueLeads
      : leads;
  const filteredLeads = focusLeads.filter((row) => rowMatchesKeyword(row, query));
  const leadSort = useSortedRows(filteredLeads, {
    contactName: (row) => row.contactName ?? row.id,
    source: (row) => row.source,
    status: (row) => row.status,
    owner: (row) => row.ownerUserId,
  });
  const pagedLeads = usePagedRows(leadSort.sorted, 10, `${kpiFocus?.subset ?? ""}:${query}:${leadSort.key}:${leadSort.direction}`);

  const followingCount = leads.filter(isFollowing).length;

  const selectedIntentStage = intent.selected?.stage;
  React.useEffect(() => {
    const next = NEXT_STAGE[selectedIntentStage ?? ""];
    if (next) setTargetStage(next);
  }, [intent.selected?.id, selectedIntentStage]);

  function openLeadDetail(row: LeasingLeadRow) {
    setPhoneReveal(null);
    setDetail({ title: `线索明细 · ${row.contactName || row.id}`, fields: [], lead: row });
  }

  // 引用保持不变：抽屉按 onClose 重挂键盘监听并把焦点移到关闭按钮，点「查看完整号码」引起的重渲染不应触发它。
  const closeDetail = React.useCallback(() => {
    setDetail(null);
    setPhoneReveal(null);
  }, []);

  // M724：查看这一条线索的完整号码。每点一次后端算查看一次并留痕；请求在途时按钮禁用（连点第二下若被拒会盖掉已取回的号码）；
  // 抽屉已关或已重新拉数（phoneReveal 被清）时丢弃迟到的结果。失败只在抽屉里报错，不走整页报错。
  async function revealLeadPhone(leadId: string) {
    const settle = (next: { phone?: string; error?: string }) =>
      setPhoneReveal((current) => (current?.leadId === leadId && current?.busy ? { leadId, ...next } : current));
    setPhoneReveal({ leadId, busy: true });
    try {
      const result = await api.get<{ contactPhone?: string | null }>(`/api/v1/leasing/leads/${leadId}/contact-phone`);
      settle({ phone: result?.contactPhone ?? "" });
    } catch (revealError) {
      settle({ error: revealError instanceof Error ? revealError.message : TEXT.phoneRevealFailed });
      return;
    }
    // 单次授权看一次即用掉：重读本人有效权限，按钮随之收起（先例 AccessGovernancePage M002-F49）。重读失败不影响已显示的号码。
    if (refreshEffectivePermissions) await refreshEffectivePermissions().catch(() => undefined);
  }

  function leadDetailFields(row: LeasingLeadRow): RowDetailField[] {
    const reveal = phoneReveal?.leadId === row.id ? phoneReveal : null;
    return [
      { label: "线索号", value: row.id },
      { label: "联系人", value: row.contactName ?? "-" },
      {
        label: "电话",
        value: (
          <>
            <span data-testid="leasing-lead-phone-value">{reveal?.phone || row.contactPhone || "-"}</span>
            {canViewFullPhone && !reveal?.phone ? (
              <button type="button" data-testid="leasing-lead-phone-reveal" disabled={reveal?.busy === true} onClick={() => void revealLeadPhone(row.id)}>{TEXT.phoneReveal}</button>
            ) : null}
            {reveal?.error ? <div data-testid="leasing-lead-phone-error" className="admin-inline-error">{reveal.error}</div> : null}
          </>
        ),
      },
      { label: "来源", value: label(SOURCE_LABELS, row.source) },
      { label: "意向业态", value: label(BUSINESS_TYPE_LABELS, row.intendedBusinessType) },
      { label: "意向空间", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.intendedSpaceId}>{row.intendedSpaceId ?? "-"}</TraceLink> },
      { label: "关联品牌", value: row.brandId ?? "-" },
      { label: "负责人", value: row.ownerUserId ?? "-" },
      { label: "状态", value: label(LEAD_STATUS_LABELS, row.status) },
      { label: "最近跟进", value: cnDate(row.lastFollowUpAt) },
    ];
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
      brandId: leadException?.brandId ?? brand.selected?.id ?? null,
      ...(leadException ? { blacklistExceptionId: leadException.id } : {}),
      ...(leadProjectId ? { projectId: leadProjectId } : {}),
    };
    const brandId = request.brandId;
    await runAction("leasing_lead_created", request, () => api.post("/api/v1/leasing/leads", request).catch((error: unknown) => {
      if (isBlacklisted(error) && brandId) setExceptionDraft({ targetType: "lead_create", brandId, label: `品牌 ${brand.selected?.brandName ?? brandId} 新建线索` });
      throw error;
    }));
    setLeadException(null);
    // 成功或失败都收起弹窗：字段是页面态不丢失，失败信息在页面 leasing-error 区展示。
    setLeadFlowOpen(false);
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
    if (lead.selected.status === "signed") {
      setFormError("已签约的线索不能回退状态。");
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
    if (lead.selected.status === "signed") {
      setFormError("已签约的线索不能再次创建意向。");
      return;
    }
    const leadId = lead.selected.id;
    const request = { idempotencyKey: nextKey("leasing-intent-create"), leadId, intendedSpaceId: intentSpaceId };
    await runAction("leasing_intent_created", request, () => api.post("/api/v1/leasing/intents", request).catch((error: unknown) => {
      if (isBlacklisted(error)) setExceptionDraft({ targetType: "intent_create", leadId, label: `由线索 ${lead.selected?.contactName ?? leadId} 新建意向` });
      throw error;
    }));
  }

  // 第十八批第 30 条：申请黑名单例外放行（理由必填、最终审批人可选），审批走主管复核待办；批准后由申请人本人按批准新建线索或转意向。
  async function requestBlacklistException() {
    if (!exceptionDraft) return;
    if (!exceptionReason.trim()) { setFormError("申请黑名单例外放行必须填写理由。"); return; }
    const request = {
      idempotencyKey: nextKey("leasing-blacklist-exception"),
      targetType: exceptionDraft.targetType,
      brandId: exceptionDraft.brandId,
      leadId: exceptionDraft.leadId,
      reason: exceptionReason.trim(),
      finalApproverId: exceptionApprover || undefined,
    };
    const ok = await runAction("leasing_blacklist_exception_requested", request, () => api.post("/api/v1/leasing/blacklist-exceptions", request));
    if (ok) { setExceptionDraft(null); setExceptionReason(""); }
  }

  async function proceedWithException(row: LeasingBlacklistExceptionRow) {
    if (row.targetType === "lead_create") {
      setLeadException(row);
      setFormError("");
      setLeadFlowOpen(true);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-intent-create"), leadId: row.leadId, intendedSpaceId: intentSpaceId, blacklistExceptionId: row.id };
    await runAction("leasing_intent_created", request, () => api.post("/api/v1/leasing/intents", request));
  }

  async function advanceIntent() {
    if (!intent.selected) {
      setFormError(TEXT.noIntent);
      return;
    }
    const keyStage = KEY_STAGES.includes(targetStage);
    const request = { idempotencyKey: nextKey("leasing-intent-advance"), targetStage, ...(keyStage && intentApprover ? { finalApproverId: intentApprover } : {}) };
    setIntentNote("");
    const ok = await runAction("leasing_intent_advanced", request, () => api.post(`/api/v1/leasing/intents/${intent.selected!.id}/advance`, request));
    if (ok && keyStage) setIntentNote(`已提交主管确认，批准后进入「${label(STAGE_LABELS, targetStage)}」。审批在「主管复核待办」办理。`);
  }

  const signTurnoverOnly = intent.selected?.rentTerms?.rentBillingMode === "turnover_only";
  async function signIntent() {
    if (!intent.selected) {
      setFormError(TEXT.noIntent);
      return;
    }
    if (!signMerchantId) { setFormError("请选择签约商户。"); return; }
    if (!signSubjectId) { setFormError("请选择合同主体（经营主体），不能用商户 id 顶替。"); return; }
    if (signTurnoverOnly && !(Number(signDepositYuan) > 0)) { setFormError("纯提成合同须填写合同约定押金（元），不得为零。"); return; }
    const request = {
      idempotencyKey: nextKey("leasing-intent-sign"),
      merchantId: signMerchantId,
      contractSubjectId: signSubjectId,
      operationSubjectId: signSubjectId,
      settlementSubjectId: signSubjectId,
      effectiveDate: signEffectiveDate,
      expiryDate: signExpiryDate,
      depositCent: signTurnoverOnly ? Math.round(Number(signDepositYuan) * 100) : undefined,
    };
    await runAction("leasing_intent_signed", request, () => api.post(`/api/v1/leasing/intents/${intent.selected!.id}/sign`, request));
  }

  async function createBrand() {
    if (!brandName.trim()) {
      setFormError(TEXT.brandNameRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("leasing-brand-create"), brandName, brandType, businessType: brandBusinessType,
      ...(brandProjectId ? { projectId: brandProjectId } : {}) };
    const ok = await runAction("leasing_brand_created", request, () => api.post("/api/v1/leasing/brands", request));
    if (ok) setBrandFlowOpen(false);
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
        <input data-testid="leasing-leads-search" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="搜索联系人 / 来源 / 状态 / 负责人" placeholder="搜索联系人 / 来源 / 状态 / 负责人" />
        <span className="admin-muted-action" data-testid="leasing-leads-filter-count">匹配 {filteredLeads.length} / {focusLeads.length} 条</span>
      </div>
      {filteredLeads.length === 0 ? (
        <div className="admin-state admin-empty" data-testid="leasing-leads-empty">无匹配线索</div>
      ) : (
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="leasing-leads-table">
          <thead><tr><th>选择</th><th className="admin-sort-th" data-testid="leasing-leads-sort-contactName" onClick={() => leadSort.toggle("contactName")} {...leadSort.thProps("contactName")}>联系人{leadSort.indicator("contactName")}</th><th>电话</th><th className="admin-sort-th" data-testid="leasing-leads-sort-source" onClick={() => leadSort.toggle("source")} {...leadSort.thProps("source")}>来源{leadSort.indicator("source")}</th><th>意向业态</th><th className="admin-sort-th" data-testid="leasing-leads-sort-status" onClick={() => leadSort.toggle("status")} {...leadSort.thProps("status")}>状态{leadSort.indicator("status")}</th><th className="admin-sort-th" data-testid="leasing-leads-sort-owner" onClick={() => leadSort.toggle("owner")} {...leadSort.thProps("owner")}>负责人{leadSort.indicator("owner")}</th><th>详情</th></tr></thead>
          <tbody>
            {pagedLeads.pageRows.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-lead-row-${row.id}`}
                onClick={() => openLeadDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openLeadDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此招商线索 ${row.id}`} data-testid={`leasing-lead-select-${row.id}`} checked={(lead.selectedId ?? "") === row.id} onChange={() => lead.setSelectedId(row.id)} /></td>
                <td>{row.contactName ?? row.id}</td>
                <td>{row.contactPhone ?? "-"}</td>
                <td>{label(SOURCE_LABELS, row.source)}</td>
                <td>{label(BUSINESS_TYPE_LABELS, row.intendedBusinessType)}</td>
                <td>{label(LEAD_STATUS_LABELS, row.status)}</td>
                <td>{row.ownerUserId ?? "-"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看线索详情" onClick={(event) => { event.stopPropagation(); openLeadDetail(row); }}>查看详情</button></td></tr>
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
        <thead><tr><th>选择</th><th>意向号</th><th>来源线索</th><th>阶段</th><th>待确认阶段</th><th>意向空间</th><th>详情</th></tr></thead>
        <tbody>
          {intents.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-intent-row-${row.id}`}
              onClick={() => openIntentDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openIntentDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此招商意向 ${row.id}`} data-testid={`leasing-intent-select-${row.id}`} checked={(intent.selectedId ?? "") === row.id} onChange={() => intent.setSelectedId(row.id)} /></td>
              <td>{row.id}</td>
              <td>{row.leadId ?? "-"}</td>
              <td>{label(STAGE_LABELS, row.stage)}</td>
              <td>{row.pendingStage ? `${label(STAGE_LABELS, row.pendingStage)}（待主管确认）` : "-"}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.intendedSpaceId}>{row.intendedSpaceId ?? "-"}</TraceLink></td>
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看租赁意向详情" onClick={(event) => { event.stopPropagation(); openIntentDetail(row); }}>查看详情</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const brandsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="leasing-brands-table">
        <thead><tr><th>选择</th><th>品牌</th><th>类型</th><th>业态</th><th>黑名单原因</th><th>详情</th></tr></thead>
        <tbody>
          {brands.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-brand-row-${row.id}`}
              onClick={() => openBrandDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBrandDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此品牌 ${row.id}`} data-testid={`leasing-brand-select-${row.id}`} checked={(brand.selectedId ?? "") === row.id} onChange={() => brand.setSelectedId(row.id)} /></td>
              <td>{row.brandName ?? row.id}</td>
              <td>{label(BRAND_TYPE_LABELS, row.brandType)}</td>
              <td>{label(BUSINESS_TYPE_LABELS, row.businessType)}</td>
              <td>{row.blacklistReason || "-"}</td>
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看品牌详情" onClick={(event) => { event.stopPropagation(); openBrandDetail(row); }}>查看详情</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  const exceptionsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="leasing-blacklist-exceptions-table">
        <thead><tr><th>放行事项</th><th>品牌</th><th>关联线索或意向</th><th>理由</th><th>申请人</th><th>申请时间</th><th>审批人</th><th>批准时间</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          {exceptions.map((row) => (
            <tr key={row.id} data-testid={`leasing-blacklist-exception-row-${row.id}`}>
              <td>{label(EXCEPTION_TARGET_LABELS, row.targetType)}</td>
              <td>{row.brandName ?? row.brandId ?? "-"}</td>
              <td>{[row.leadId || row.usedLeadId ? `线索 ${row.leadId ?? row.usedLeadId}` : "", row.usedIntentId ? `意向 ${row.usedIntentId}` : ""].filter(Boolean).join("；") || "-"}</td>
              <td>{row.reason ?? "-"}</td>
              <td>{row.requestedBy ?? "-"}</td>
              <td>{cnDate(row.requestedAt)}</td>
              <td>{row.approvedBy ?? "-"}</td>
              <td>{cnDate(row.approvedAt)}</td>
              <td>{label(EXCEPTION_STATUS_LABELS, row.status)}</td>
              <td>{row.status === "approved" ? (
                <button type="button" data-testid={`leasing-blacklist-exception-use-${row.id}`} disabled={busyAction !== ""} onClick={() => void proceedWithException(row)}>
                  {row.targetType === "lead_create" ? "按批准新建线索" : "按批准转意向"}
                </button>
              ) : "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  function kpiFocusTitle(): string {
    if (!kpiFocus) return "";
    if (kpiFocus.table === "leads") {
      if (kpiFocus.subset === "following") return "跟进中线索";
      if (kpiFocus.subset === "due") return "该跟进的线索";
      return "线索总数";
    }
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
            {/* 到期跟进提醒：招商人员一进工作台就先看见「该跟进几条」，点进去直接是那几条线索。
                判定天数在后台「提醒与留存天数配置」可改（leasing.lead_follow_up），前端不复刻口径。 */}
            {dueLeads.length > 0 && (
              <div className="admin-state" data-testid="leasing-follow-up-due-banner">
                有 {dueLeads.length} 条线索超过约定天数没跟进，请尽快联系。
                <button type="button" data-testid="leasing-follow-up-due-open" onClick={() => enterKpiFocus("leads", "due")}>查看该跟进的线索</button>
              </div>
            )}
            <div className="admin-kpi-grid" data-testid="leasing-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>线索总数</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-leads" onClick={() => enterKpiFocus("leads")}>{leads.length}</button><small>招商前漏斗</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>跟进中</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-following" onClick={() => enterKpiFocus("leads", "following")}>{followingCount}</button><small>未签约/未流失</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>该跟进</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-due" onClick={() => enterKpiFocus("leads", "due")}>{dueLeads.length}</button><small>超过约定天数没跟进</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>意向数</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-intents" onClick={() => enterKpiFocus("intents")}>{intents.length}</button><small>租赁意向</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>品牌库</span><button type="button" className="admin-kpi-value" data-testid="leasing-kpi-brands" onClick={() => enterKpiFocus("brands")}>{brands.length}</button><small>含黑名单</small></div>
            </div>

            {/* 线索建档：引导式流程一期，内联填表收起为主按钮 → 分步弹窗（GuidedFlowModal）。 */}
            <div className="admin-form-row">
              <button type="button" data-testid="leasing-lead-create-open" onClick={() => { setFormError(""); setLeadFlowOpen(true); }}>＋ 新建线索</button>
              <span className="admin-muted-action" data-testid="leasing-boundary">招商前漏斗为后台主数据；签约直接生成合同草稿，进入合同生命周期工作台审批。</span>
            </div>

            {/* 线索动作 */}
            <div className="admin-form-grid" data-testid="leasing-lead-action-form">
              <label>负责人<select data-testid="leasing-lead-owner" value={ownerUserId} onChange={(e) => setOwnerUserId(e.target.value)}><option value="">请选择负责人（可空）</option>{salesUsers.map((x) => <option key={x.id} value={x.id}>{(x.username ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
              <label>线索状态<select data-testid="leasing-lead-status" value={leadStatus} onChange={(e) => setLeadStatus(e.target.value)}><option value="following_up">跟进中</option><option value="converted_to_intent">已转意向</option><option value="negotiating">洽谈中</option><option value="lost">已流失</option><option value="on_hold">已搁置</option></select></label>
              <label>跟进方式<select data-testid="leasing-follow-method" value={followMethod} onChange={(e) => setFollowMethod(e.target.value)}><option value="phone">电话</option><option value="visit">拜访</option><option value="wechat">微信</option></select></label>
              <label>跟进内容<input data-testid="leasing-follow-content" value={followContent} onChange={(e) => setFollowContent(e.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="leasing-lead-actions">
              <button type="button" data-testid="leasing-lead-assign" disabled={busyAction !== ""} onClick={() => void assignLead()}>指派</button>
              <button type="button" data-testid="leasing-lead-status-update" disabled={busyAction !== "" || lead.selected?.status === "signed"} title={lead.selected?.status === "signed" ? "已签约后不能回退状态" : undefined} onClick={() => void updateLeadStatus()}>更新状态</button>
              <button type="button" data-testid="leasing-follow-add" disabled={busyAction !== ""} onClick={() => void addFollowUp()}>添加跟进</button>
            </div>

            {/* 意向 + 签约 */}
            <div className="admin-form-grid" data-testid="leasing-intent-form">
              <label>意向空间<select data-testid="leasing-intent-space" value={intentSpaceId} onChange={(e) => setIntentSpaceId(e.target.value)}><option value="">请选择空间</option>{spaces.map((x) => <option key={x.id} value={x.id}>{(x.spaceName ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
              <label>推进阶段<select data-testid="leasing-intent-stage" value={targetStage} onChange={(e) => setTargetStage(e.target.value)}><option value="requirement_confirmed">需求确认</option><option value="proposal_quoted">方案报价</option><option value="negotiation">商务谈判</option><option value="pending_signing">待签约</option></select></label>
              {KEY_STAGES.includes(targetStage) ? <label>最终审批人<select data-testid="leasing-intent-final-approver" value={intentApprover} onChange={(e) => setIntentApprover(e.target.value)}><option value="">不指定（直接上级确认）</option>{salesUsers.map((x) => <option key={x.id} value={x.id}>{(x.username ?? x.id)}（{shortId(x.id)}）</option>)}</select></label> : null}
              <label>签约商户<select data-testid="leasing-sign-merchant" value={signMerchantId} onChange={(e) => setSignMerchantId(e.target.value)}><option value="">请选择商户</option>{merchantList.map((x) => <option key={x.id} value={x.id}>{(x.displayName ?? x.companyName ?? x.merchantName ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
              <label>合同主体<select data-testid="leasing-sign-subject" value={signSubjectId} onChange={(e) => setSignSubjectId(e.target.value)}><option value="">请选择经营主体</option>{entities.map((x) => <option key={x.id} value={x.id}>{(x.entityName ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
              <label>合同生效日<CnDatePicker data-testid="leasing-sign-effective-date" value={signEffectiveDate} onChange={setSignEffectiveDate} /></label>
              <label>合同到期日<CnDatePicker data-testid="leasing-sign-expiry-date" value={signExpiryDate} onChange={setSignExpiryDate} /></label>
              {signTurnoverOnly ? <label>押金（元）<input data-testid="leasing-sign-deposit" type="number" min="0" placeholder="纯提成合同约定押金（元）" value={signDepositYuan} onChange={(e) => setSignDepositYuan(e.target.value)} /></label> : null}
            </div>
            <div className="admin-form-row" data-testid="leasing-intent-actions">
              <button type="button" data-testid="leasing-intent-create" disabled={busyAction !== "" || lead.selected?.status === "signed"} title={lead.selected?.status === "signed" ? "已签约的线索不能再次创建意向" : undefined} onClick={() => void createIntent()}>从线索建意向</button>
              <button type="button" data-testid="leasing-intent-advance" disabled={busyAction !== ""} onClick={() => void advanceIntent()}>推进意向</button>
              <button type="button" data-testid="leasing-intent-sign" disabled={busyAction !== ""} onClick={() => void signIntent()}>签约→合同草稿</button>
            </div>
            {intentNote ? <div className="admin-state" data-testid="leasing-intent-pending-note">{intentNote}</div> : null}

            {/* 品牌库：录入品牌走引导式弹窗；拉黑原因 + 拉入黑名单为品牌处置动作，原样保留。 */}
            <div className="admin-form-row" data-testid="leasing-brand-create-entry">
              <button type="button" data-testid="leasing-brand-create-open" onClick={() => { setFormError(""); setBrandFlowOpen(true); }}>＋ 录入品牌</button>
              <span className="admin-muted-action">品牌库为招商后台主数据；拉黑需先在下方品牌表选中目标品牌。</span>
            </div>
            <div className="admin-form-grid" data-testid="leasing-brand-form">
              <label>拉黑原因<input data-testid="leasing-brand-blacklist-reason" value={blacklistReason} onChange={(e) => setBlacklistReason(e.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="leasing-brand-actions">
              <button type="button" data-testid="leasing-brand-blacklist" disabled={busyAction !== ""} onClick={() => void blacklistBrand()}>拉入黑名单</button>
              {brand.selected?.brandType === "blacklist" ? (
                <button type="button" data-testid="leasing-brand-blacklist-exception-open" onClick={() => setExceptionDraft({ targetType: "lead_create", brandId: brand.selected!.id, label: `品牌 ${brand.selected!.brandName ?? brand.selected!.id} 新建线索` })}>申请黑名单例外放行（新建线索）</button>
              ) : null}
            </div>
            {exceptionDraft ? (
              <div className="admin-form-grid" data-testid="leasing-blacklist-exception-form">
                <span className="admin-muted-action">申请黑名单例外放行：{exceptionDraft.label}。须持单独授予的黑名单例外放行权限，按层级经主管审批，本人不能审批；批准后由本人按批准办理一次。</span>
                <label>理由<input data-testid="leasing-blacklist-exception-reason" value={exceptionReason} onChange={(e) => setExceptionReason(e.target.value)} placeholder="必填" /></label>
                <label>最终审批人<select data-testid="leasing-blacklist-exception-approver" value={exceptionApprover} onChange={(e) => setExceptionApprover(e.target.value)}><option value="">不指定（直接上级审批）</option>{salesUsers.map((x) => <option key={x.id} value={x.id}>{(x.username ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
                <div className="admin-form-row">
                  <button type="button" data-testid="leasing-blacklist-exception-submit" disabled={busyAction !== ""} onClick={() => void requestBlacklistException()}>提交例外放行申请</button>
                  <button type="button" data-testid="leasing-blacklist-exception-cancel" onClick={() => setExceptionDraft(null)}>取消</button>
                </div>
              </div>
            ) : null}
            {formError ? <div data-testid="leasing-error" className="admin-inline-error">{formError}</div> : null}

            {leadsTable}
            {intentsTable}
            {brandsTable}
            {exceptionsTable}
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="leasing-follow-ups-table">
                <thead><tr><th>对象</th><th>方式</th><th>内容</th><th>下次计划</th><th>详情</th></tr></thead>
                <tbody>
                  {followUps.map((row) => (
                    <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`leasing-follow-up-row-${row.id}`}
                      onClick={() => openFollowUpDetail(row)}
                      onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openFollowUpDetail(row); } }}>
                      <td>{label(REF_TYPE_LABELS, row.refType)}:{row.refId ?? "-"}</td>
                      <td>{label(METHOD_LABELS, row.method)}</td>
                      <td>{row.content ?? "-"}</td>
                      <td>{cnDate(row.nextPlanAt)}</td>
                    <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看跟进详情" onClick={(event) => { event.stopPropagation(); openFollowUpDetail(row); }}>查看详情</button></td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
      <GuidedFlowModal
        open={leadFlowOpen}
        title="新建招商线索"
        testId="leasing-lead-flow"
        submitTestId="leasing-lead-create"
        submitLabel="创建线索"
        submitting={busyAction !== ""}
        error={formError}
        onClose={() => { setLeadFlowOpen(false); setLeadException(null); }}
        onSubmit={() => void createLead()}
        steps={[
          {
            title: "联系人信息",
            description: "填写线索联系人及联系电话。",
            validate: () => (contactName.trim() ? null : TEXT.contactRequired),
            content: (
              <>
                <label>联系人<input data-testid="leasing-lead-contact-name" value={contactName} onChange={(e) => setContactName(e.target.value)} /></label>
                <label>联系电话<input data-testid="leasing-lead-contact-phone" value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} /></label>
              </>
            ),
          },
          {
            title: "来源与意向",
            description: "记录线索来源、意向业态与意向空间。",
            content: (
              <>
                <label>来源<select data-testid="leasing-lead-source" value={source} onChange={(e) => setSource(e.target.value)}><option value="outbound">外拓</option><option value="online_inquiry">线上咨询</option><option value="referral">转介绍</option><option value="exhibition">展会</option></select></label>
                <label>意向业态<select data-testid="leasing-lead-business-type" value={intendedBusinessType} onChange={(e) => setIntendedBusinessType(e.target.value)}>{BUSINESS_TYPES.map((t) => <option key={t} value={t}>{BUSINESS_TYPE_LABELS[t]}</option>)}</select></label>
                <label>意向空间<select data-testid="leasing-lead-space" value={intendedSpaceId} onChange={(e) => setIntendedSpaceId(e.target.value)}><option value="">请选择空间（可空）</option>{spaces.map((x) => <option key={x.id} value={x.id}>{(x.spaceName ?? x.id)}（{shortId(x.id)}）</option>)}</select></label>
                {pickProject ? <label>所属项目<select data-testid="leasing-lead-project" value={leadProjectId} onChange={(e) => setLeadProjectId(e.target.value)}><option value="">{intendedSpaceId ? "按意向空间所在项目" : "本人所在项目"}</option>{projects.map((x) => <option key={x.id} value={x.id}>{x.name ?? x.id}</option>)}</select></label> : null}
              </>
            ),
          },
        ]}
        summary={[
          { label: "联系人", value: contactName },
          { label: "联系电话", value: contactPhone },
          { label: "来源", value: label(SOURCE_LABELS, source) },
          { label: "意向业态", value: label(BUSINESS_TYPE_LABELS, intendedBusinessType) },
          { label: "意向空间", value: intendedSpaceId },
          { label: "关联品牌", value: leadException ? `${leadException.brandName ?? leadException.brandId}（按黑名单例外放行）` : brand.selected?.brandName ?? "未关联" },
        ]}
      />
      <GuidedFlowModal
        open={brandFlowOpen}
        title="录入品牌"
        testId="leasing-brand-flow"
        submitTestId="leasing-brand-create"
        submitLabel="录入品牌"
        submitting={busyAction !== ""}
        error={formError}
        onClose={() => setBrandFlowOpen(false)}
        onSubmit={() => void createBrand()}
        steps={[
          {
            title: "品牌信息",
            description: "填写品牌名称、类型与业态。",
            validate: () => (brandName.trim() ? null : TEXT.brandNameRequired),
            content: (
              <>
                <label>品牌名称<input data-testid="leasing-brand-name" value={brandName} onChange={(e) => setBrandName(e.target.value)} /></label>
                <label>品牌类型<select data-testid="leasing-brand-type" value={brandType} onChange={(e) => setBrandType(e.target.value)}><option value="intentional">意向品牌</option><option value="prospective">潜在品牌</option><option value="partner">合作品牌</option></select></label>
                <label>业态<select data-testid="leasing-brand-business" value={brandBusinessType} onChange={(e) => setBrandBusinessType(e.target.value)}>{BUSINESS_TYPES.map((t) => <option key={t} value={t}>{BUSINESS_TYPE_LABELS[t]}</option>)}</select></label>
                {pickProject ? <label>所属项目<select data-testid="leasing-brand-project" value={brandProjectId} onChange={(e) => setBrandProjectId(e.target.value)}><option value="">本人所在项目</option>{projects.map((x) => <option key={x.id} value={x.id}>{x.name ?? x.id}</option>)}</select></label> : null}
              </>
            ),
          },
        ]}
        summary={[
          { label: "品牌名称", value: brandName.trim() || "未填写" },
          { label: "品牌类型", value: brandType.trim() || "未填写" },
          { label: "业态", value: brandBusinessType.trim() || "未填写" },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.lead ? leadDetailFields(detail.lead) : detail?.fields ?? []} onClose={closeDetail} />
    </section>
  );
}
