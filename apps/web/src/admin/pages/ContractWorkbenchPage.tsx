import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { Tabs } from "antd";
import { loadSecondary, loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, BusinessEntityRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowActions, type RowAction } from "../components/RowActions";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { auditTraceField } from "../components/AuditTrailHint";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

// ponytail: ID 展示统一截尾 8 位 + title 全量；等宽由 admin-id-cell 类保证。
function shortId(id: string | null | undefined): string {
  if (!id) return "-";
  return id.length > 8 ? `…${id.slice(-8)}` : id;
}

// 后端 ContractRecord / ContractApprovalFlowStepRecord / SpaceExpiryColorRuleRecord（客户端 unwrap 已 camel 化）。
type ContractRow = {
  id: string; merchantId: string; lifecycleStatus: string;
  effectiveDate?: string; expiryDate?: string; commissionMode?: string | null; projectId?: string | null;
  rentTerms?: { businessScope?: string; allowedCategories?: string[] } | null;
};
type FlowStep = { stageSeq: number; stageName: string; approverPermissionCode: string };
type ColorRule = { id: string; categoryName: string; color: string; thresholdDays: number };
type ChangeRow = { id: string; contractId: string; changeType: string; approvalStatus: string };
type SettlementRow = { id: string; contractId: string; triggerType: string; status: string; settlementItems?: Array<Record<string, unknown>> };
type SpaceRental = { spaceId: string; spaceName: string; rentalStatus: string; color?: string | null; daysToExpiry?: number | null };

const DRAFT = ["contract.draft.manage"];
const ACTIVATE = ["contract.lifecycle.activate"];
const RENT_AR = ["finance.ar_ap.manage"];
const SETTLE = ["contract.settlement.manage"];
const FLOW_CFG = ["contract.approval.flow.manage"];
const COLOR_CFG = ["contract.space.color.manage"];
const CHANGE_MANAGE = ["contract.change.manage"];
const DISPUTE_RESOLVE = ["contract.settlement.dispute.resolve"];
const CHANGE_TYPES = [
  { v: "renewal", l: "续租" }, { v: "move_out", l: "退租" }, { v: "area_change", l: "面积变更" },
  { v: "location_change", l: "经营位置变更" },
  { v: "rent_change", l: "租金变更" }, { v: "business_scope_change", l: "经营范围变更" }, { v: "early_termination", l: "提前终止" },
];
// 经营范围词表（2026-07-18 拍板：复用既有业务类目，禁止发明新枚举）——
// 域级 bar/ticketing/amusement=V103 风险告警 source_domain CHECK 消费域；catering/retail/service/entertainment=
// merchant_type 既有词表（OrganizationMerchantPage MERCHANT_TYPES）；其余 8 项=酒吧商品粗类别（BarProductsPage CATEGORY_LABELS）。
// 勾选后存 rent_terms.allowedCategories，下单侧（酒吧/票务）拦超范围经营；不勾=不拦截（存量口径）。
const SCOPE_CATEGORIES: { v: string; l: string }[] = [
  { v: "bar", l: "酒吧" }, { v: "ticketing", l: "门票销售" }, { v: "amusement", l: "游乐" },
  { v: "catering", l: "餐饮" }, { v: "retail", l: "零售" }, { v: "service", l: "服务" }, { v: "entertainment", l: "娱乐" },
  { v: "spirit", l: "烈酒" }, { v: "wine", l: "葡萄酒" }, { v: "beer", l: "啤酒" }, { v: "cocktail", l: "鸡尾酒" },
  { v: "soft_drink", l: "软饮" }, { v: "mixer", l: "调饮" }, { v: "tea", l: "茶饮" }, { v: "snack", l: "小吃" },
];
const scopeLabel = (v: string) => SCOPE_CATEGORIES.find((c) => c.v === v)?.l ?? v;
const RENTAL_LABEL: Record<string, string> = {
  vacant: "空置", negotiating: "洽谈中", let: "已租", expiring: "即将到期", moved_out: "已退租", unavailable: "不可租",
};
const RENTAL_COLOR: Record<string, string> = {
  vacant: "#9ca3af", negotiating: "#3b82f6", let: "#10b981", expiring: "#F59E0B", moved_out: "#6b7280", unavailable: "#ef4444",
};
const OPEN_SETTLEMENT = ["pending_settlement", "deposit_offsetting", "arrears_collecting"];

// 分组并行审批：默认四岗，按 stageSeq 串行。审批权限码即并行槽 id。
const APPROVE_ROLES: { code: string; label: string }[] = [
  { code: "contract.approve.business", label: "业务过" },
  { code: "contract.approve.finance", label: "财务过" },
  { code: "contract.approve.legal", label: "法务过" },
  { code: "contract.approve.gm", label: "总经理过" },
];
// 审批链表格「审批岗位」列显名：权限码 → 岗位业务名。
const ROLE_LABELS: Record<string, string> = {
  "contract.approve.business": "业务岗",
  "contract.approve.finance": "财务岗",
  "contract.approve.legal": "法务岗",
  "contract.approve.gm": "总经理",
};

/**
 * 合同生命周期工作台（ADM-CONT-001，V162 重建）：起草 → 分组并行审批 → 生效固化 → 退租结算 + 审批链/到期着色配置。
 * 覆盖验收 ADM-CONT-001..008：起草/四级并行审批/固化快照/铺位着色配置/变更入口/退租结算/审批链配置。
 */
export function ContractWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [flow, setFlow] = React.useState<FlowStep[]>([]);
  const [colorRules, setColorRules] = React.useState<ColorRule[]>([]);
  // 审批链/到期着色/铺位租赁状态为配置与辅助区块，任一失败不拖垮合同主列表。
  const [configNotice, setConfigNotice] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState({
    merchantId: "", contractSubjectId: "", settlementSubjectId: "", effectiveDate: "", expiryDate: "",
    baseRentYuan: "", perSqmYuan: "", rentMode: "monthly_fixed", rentBillingMode: "fixed_only",
    commissionMode: "fixed_ratio", energyFeeBasis: "by_area",
    businessScope: "", allowedCategories: [] as string[],
  });
  const [spaceId, setSpaceId] = React.useState("");
  const [billPeriod, setBillPeriod] = React.useState("");
  const [colorForm, setColorForm] = React.useState({ categoryName: "", color: "#F59E0B", thresholdDays: "30" });
  // 批2b: 提成规则(阶梯超额累进)配置——填合同条款时建，挂 contract_id。
  const [turnoverForm, setTurnoverForm] = React.useState({ contractId: "", merchantId: "", ruleName: "", minYuan: "", effectiveFrom: "", effectiveTo: "" });
  const [turnoverTiers, setTurnoverTiers] = React.useState<Array<{ upperWan: string; ratePct: string }>>([{ upperWan: "", ratePct: "" }]);
  const submitTurnoverRule = async () => {
    if (!turnoverForm.contractId.trim() || !turnoverForm.merchantId.trim() || !turnoverForm.ruleName.trim() || !turnoverForm.effectiveFrom) {
      setGlobalError("建提成规则需填：合同 id、商户 id、规则名、生效日期");
      return;
    }
    const tiers = turnoverTiers.map((t, i) => ({
      upperBoundCent: i === turnoverTiers.length - 1 ? null : Math.round(Number(t.upperWan) * 1_000_000),
      rateBps: Math.round(Number(t.ratePct) * 100),
    }));
    try {
      await api.post("/api/v1/finance/turnover-rent/rules", {
        contractId: turnoverForm.contractId.trim(),
        merchantId: turnoverForm.merchantId.trim(),
        ruleName: turnoverForm.ruleName.trim(),
        minAmountCent: turnoverForm.minYuan.trim() ? Math.round(Number(turnoverForm.minYuan) * 100) : null,
        effectiveFrom: turnoverForm.effectiveFrom,
        effectiveTo: turnoverForm.effectiveTo || null,
        tiers,
      });
      // P0-E：本提交只落草案，须由另一位同事在「结算配置中心」复核转正后才对结算生效。
      setActionResult({ action: "turnover_rule_draft_saved", contractId: turnoverForm.contractId.trim() });
      setTurnoverTiers([{ upperWan: "", ratePct: "" }]);
      setTurnoverForm({ ...turnoverForm, ruleName: "", minYuan: "" });
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  };
  const [selectedId, setSelectedId] = React.useState("");
  const [changes, setChanges] = React.useState<ChangeRow[]>([]);
  const [settlements, setSettlements] = React.useState<SettlementRow[]>([]);
  const [spaceRental, setSpaceRental] = React.useState<SpaceRental[]>([]);
  const [changeForm, setChangeForm] = React.useState({ changeType: "rent_change", note: "", allowedCategories: [] as string[], clearScope: false, newDepositYuan: "", spaceIds: "" });
  // #30 超范围下单经营申请（拦下后可申请，走既有审批链）
  const [scopeReqForm, setScopeReqForm] = React.useState({ category: "spirit", reason: "", grantType: "permanent", validUntil: "" });
  const [disputeReason, setDisputeReason] = React.useState("");
  const [settleTrigger, setSettleTrigger] = React.useState("move_out");
  // 商户显名字典（页面级拉取，失败降级回 ID）。
  const [merchantNames, setMerchantNames] = React.useState<Record<string, string>>({});
  const [merchants, setMerchants] = React.useState<Array<{ id: string; displayName?: string; companyName?: string }>>([]);
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·日期中文·引用 TraceLink）。合同/变更/退租结算三张业务表接入。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-2：起草合同收起为主按钮，点击弹出分步引导（GuidedFlowModal）。
  const [createOpen, setCreateOpen] = React.useState(false);
  const [draftError, setDraftError] = React.useState("");

  // 无权限副资源不请求（根因修）：审批链配置需 contract.approval.flow.manage、着色规则需 contract.space.color.manage，页面基础权限 contract.draft.manage 不含之。
  const canApprovalFlow = hasPermissions(effectivePermissions, ["contract.approval.flow.manage"]);
  const canColorRules = hasPermissions(effectivePermissions, ["contract.space.color.manage"]);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：审批链/到期着色规则/铺位租赁状态为配置与辅助区块，可能因权限/500
      // 单独失败——各自降级为空并合并一条区块提示，不拖垮合同主列表（合同为主资源，读失败才整页报错）。
      const [rows, stepsRes, rulesRes, spacesRes] = await Promise.all([
        api.get<ContractRow[]>("/api/v1/contracts"),
        loadSecondaryIfPermitted(canApprovalFlow, () => api.get<FlowStep[]>("/api/v1/contracts/approval-flow"), [] as FlowStep[]),
        loadSecondaryIfPermitted(canColorRules, () => api.get<ColorRule[]>("/api/v1/contracts/expiry-color-rules"), [] as ColorRule[]),
        loadSecondary(() => api.get<SpaceRental[]>("/api/v1/contracts/space-rental-status"), [] as SpaceRental[]),
      ]);
      setContracts(rows ?? []);
      setFlow(stepsRes.data);
      setColorRules(rulesRes.data);
      setSpaceRental(spacesRes.data);
      setConfigNotice(stepsRes.notice ?? rulesRes.notice ?? spacesRes.notice);
      // 显名字典拉取失败不阻断主流程，表格降级显示 ID。
      try {
        const merchants = await api.get<Array<{ id: string; displayName?: string; companyName?: string }>>("/api/v1/platform/merchants");
        setMerchantNames(Object.fromEntries((merchants ?? []).filter((m) => m.displayName || m.companyName).map((m) => [m.id, (m.displayName ?? m.companyName) as string])));
        setMerchants(merchants ?? []);
        const ents = await api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities");
        setEntities(ents ?? []);
      } catch {
        /* 无权限或接口失败 → 保持空字典 */
      }
    },
    { onError: setGlobalError, fallbackMessage: "合同数据读取失败", onStart: () => setGlobalError(null) },
  );

  // 下钻定位：从别的页点合同号进来（?focus=合同id）→ 选中该合同并滚动高亮其行；无 focus 时行为不变。
  React.useEffect(() => {
    if (!focusObjectId || contracts.length === 0) return;
    if (!contracts.some((row) => row.id === focusObjectId)) return;
    void selectContract(focusObjectId);
    const el = typeof document !== "undefined" ? document.querySelector(`[data-testid="contract-${focusObjectId}"]`) : null;
    if (el && typeof (el as HTMLElement).scrollIntoView === "function") {
      (el as HTMLElement).scrollIntoView({ block: "center" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusObjectId, contracts.length]);

  async function createDraft() {
    // 双保险：步内已校验，提交前再挡一次（起草需填商户/合同主体/生效/到期日期）。
    if (!draft.merchantId.trim() || !draft.contractSubjectId.trim() || !draft.effectiveDate || !draft.expiryDate) {
      setDraftError("起草需填写商户、合同主体、生效/到期日期");
      return;
    }
    const cent = Math.round(Number(draft.baseRentYuan) * 100);
    const perSqmCent = Math.round(Number(draft.perSqmYuan) * 100);
    const rentTerms: Record<string, unknown> = { rentMode: draft.rentMode, rentBillingMode: draft.rentBillingMode };
    if (draft.rentMode === "per_sqm") {
      if (Number.isFinite(perSqmCent) && perSqmCent > 0) rentTerms.perSqmRentCent = perSqmCent;
    } else if (Number.isFinite(cent) && cent > 0) {
      rentTerms.baseRentCent = cent;
    }
    try {
      await api.post("/api/v1/contracts", {
        merchantId: draft.merchantId.trim(),
        contractSubjectId: draft.contractSubjectId.trim(),
        settlementSubjectId: (draft.settlementSubjectId.trim() || draft.contractSubjectId.trim()),
        effectiveDate: draft.effectiveDate,
        expiryDate: draft.expiryDate,
        rentTerms,
        commissionMode: draft.commissionMode,
        energyFeeBasis: draft.energyFeeBasis,
        businessScope: draft.businessScope.trim() || undefined,
        allowedCategories: draft.allowedCategories.length ? draft.allowedCategories : undefined,
      });
      setActionResult({ action: "contract_drafted", merchantId: draft.merchantId.trim() });
      setDraft({ ...draft, merchantId: "", baseRentYuan: "", perSqmYuan: "", businessScope: "", allowedCategories: [] });
      setCreateOpen(false);
      reload();
    } catch (createError) {
      setDraftError(createError instanceof Error ? createError.message : "起草合同失败");
    }
  }

  async function submit(row: ContractRow) {
    await api.post(`/api/v1/contracts/${row.id}/submit`, {});
    setActionResult({ action: "contract_submitted", contractId: row.id });
    reload();
  }

  async function approve(row: ContractRow, code: string) {
    await api.post(`/api/v1/contracts/${row.id}/approve`, { approverPermissionCode: code, decision: "approve" });
    setActionResult({ action: "contract_approved", contractId: row.id, role: code });
    reload();
  }

  async function activate(row: ContractRow) {
    if (!spaceId.trim()) {
      setGlobalError("生效需填写所租空间 id");
      return;
    }
    await api.post(`/api/v1/contracts/${row.id}/activate`, { spaceIds: [spaceId.trim()] });
    setActionResult({ action: "contract_activated", contractId: row.id });
    reload();
  }

  // P1-B 收入闭环：手工补排某账期租金/物业/能源 AR（生效已自动排整租期，此处用于补排/重排，幂等已挂账则跳过）。
  async function generatePeriodBills(row: ContractRow) {
    if (!billPeriod) {
      setGlobalError("排租金AR需先选账期（年-月）");
      return;
    }
    const bills = await api.post<unknown[]>(`/api/v1/contracts/${row.id}/period-bills`, { period: billPeriod });
    setActionResult({ action: "contract_period_bills_generated", contractId: row.id, period: billPeriod, count: (bills ?? []).length });
    reload();
  }

  async function settle(row: ContractRow, forceTrigger?: string) {
    // 退租/到期开账 → 待确认退押金（填争议原因则挂起有争议）。开账只算不落账，押金退还须在「确认退押金」时才落账。
    // I-3 到期入口强制 expiry（按合同生命周期派生，不吃共享下拉默认 move_out，否则到期合同会被误作废、押金静默不退）。
    await api.post(`/api/v1/contracts/${row.id}/settle`, {
      triggerType: forceTrigger ?? settleTrigger, disputeReason: disputeReason.trim() || null,
    });
    setActionResult({ action: "contract_settle_opened", contractId: row.id });
    setDisputeReason("");
    await selectContract(row.id);
    reload();
  }

  async function selectContract(id: string) {
    setSelectedId(id);
    const [chs, sts] = await Promise.all([
      api.get<ChangeRow[]>(`/api/v1/contracts/${id}/changes`),
      api.get<SettlementRow[]>(`/api/v1/contracts/${id}/settlements`),
    ]);
    setChanges(chs ?? []);
    setSettlements(sts ?? []);
  }

  async function raiseChange() {
    if (!selectedId) {
      setGlobalError("先点合同行『明细』选一份合同");
      return;
    }
    const payload: Record<string, unknown> = {};
    if (changeForm.note.trim()) payload.note = changeForm.note.trim();
    if (changeForm.changeType === "business_scope_change") {
      // S-5: 显式清空 → 发空数组（审批通过后 rent_terms.allowedCategories=[]，下单不再拦截）；
      // 未勾任何类目且未勾清空 → 不发键，审批通过后既有范围原样不动。
      if (changeForm.clearScope) payload.allowedCategories = [];
      else if (changeForm.allowedCategories.length) payload.allowedCategories = changeForm.allowedCategories;
    }
    if (changeForm.changeType === "area_change" || changeForm.changeType === "location_change") {
      // #31 变更子合同：新保证金（元→分）+ 经营位置变更的新铺位（逗号分隔）。
      if (changeForm.newDepositYuan.trim()) payload.newDepositCent = Math.round(Number(changeForm.newDepositYuan.trim()) * 100);
      const spaces = changeForm.spaceIds.split(/[,，\s]+/).map((x) => x.trim()).filter(Boolean);
      if (spaces.length) payload.spaceIds = spaces;
    }
    await api.post(`/api/v1/contracts/${selectedId}/changes`, {
      changeType: changeForm.changeType, payload,
    });
    setActionResult({ action: "contract_change_raised", contractId: selectedId, type: changeForm.changeType });
    void selectContract(selectedId);
  }

  async function requestScopeGrant() {
    if (!selectedId) { setGlobalError("先点合同行『明细』选一份合同"); return; }
    if (!scopeReqForm.reason.trim()) { setGlobalError("超范围经营申请须填原因"); return; }
    const body: Record<string, unknown> = {
      category: scopeReqForm.category, reason: scopeReqForm.reason.trim(), grantType: scopeReqForm.grantType,
    };
    if (scopeReqForm.grantType === "temporary" && scopeReqForm.validUntil) body.validUntil = scopeReqForm.validUntil;
    await api.post(`/api/v1/contracts/${selectedId}/scope-requests`, body);
    setActionResult({ action: "contract_scope_request_raised", contractId: selectedId, category: scopeReqForm.category });
    setScopeReqForm({ ...scopeReqForm, reason: "" });
    void selectContract(selectedId);
  }

  async function approveChange(changeId: string, code: string) {
    await api.post(`/api/v1/contracts/changes/${changeId}/approve`, { approverPermissionCode: code, decision: "approve" });
    setActionResult({ action: "contract_change_approved", changeId, role: code });
    void selectContract(selectedId);
  }

  async function settlementAction(sid: string, action: "resolve-dispute") {
    await api.post(`/api/v1/contracts/settlements/${sid}/${action}`, {});
    setActionResult({ action: "contract_settlement_" + action, settlementId: sid });
    await selectContract(selectedId);
    reload();
  }

  // 确认退押金（资金动作二次确认）：结清 → 落 ar_ap+deposit_ledger、终止/到期合同；若合同已被别的退租落账终止则本单作废。
  async function confirmSettlement(st: SettlementRow) {
    const dep = (st.settlementItems ?? []).find((i) => i.item === "deposit_disposal");
    const refundYuan = (Number(dep?.netRefundCent ?? 0) / 100).toFixed(2);
    if (!window.confirm(`确认退押金（约 ¥${refundYuan}，最终以确认时最新欠费重算为准）并落账？将挂应付给商户并扣减押金台账，同时结清并终止/到期合同，不可撤销。`)) return;
    await api.post(`/api/v1/contracts/settlements/${st.id}/complete`, {});
    setActionResult({ action: "contract_settlement_complete", settlementId: st.id });
    await selectContract(selectedId);
    reload();
  }

  async function resetFlowToDefault() {
    await api.post("/api/v1/contracts/approval-flow", {
      name: "平台默认合同审批链",
      steps: [
        { stageSeq: 1, stageName: "业务审核", approverPermissionCode: "contract.approve.business" },
        { stageSeq: 1, stageName: "财务审核", approverPermissionCode: "contract.approve.finance" },
        { stageSeq: 1, stageName: "法务审核", approverPermissionCode: "contract.approve.legal" },
        { stageSeq: 2, stageName: "总经理终审", approverPermissionCode: "contract.approve.gm" },
      ],
    });
    setActionResult({ action: "contract_flow_saved" });
    reload();
  }

  async function addColorRule() {
    const days = Number(colorForm.thresholdDays);
    if (!colorForm.categoryName.trim() || !Number.isFinite(days) || days <= 0) {
      setGlobalError("着色分类需填写分类名与正整数提前天数");
      return;
    }
    await api.post("/api/v1/contracts/expiry-color-rules", {
      categoryName: colorForm.categoryName.trim(), color: colorForm.color, thresholdDays: days,
    });
    setActionResult({ action: "contract_color_rule_added", category: colorForm.categoryName.trim() });
    setColorForm({ ...colorForm, categoryName: "" });
    reload();
  }

  // 合同整行明细：全字段（含生效/到期，抽成模式，商户下钻）。
  function openContractDetail(row: ContractRow) {
    setDetail({
      title: `合同明细 · ${shortId(row.id)}`,
      fields: [
        { label: "合同号", value: row.id },
        {
          label: "商户",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{merchantNames[row.merchantId] ?? row.merchantId}</TraceLink>,
        },
        { label: "生命周期状态", value: formatBusinessStatus(row.lifecycleStatus) },
        { label: "生效日期", value: cnDate(row.effectiveDate) },
        { label: "到期日期", value: cnDate(row.expiryDate) },
        { label: "抽成模式", value: formatBusinessStatus(row.commissionMode) },
        {
          label: "经营范围",
          value: (row.rentTerms?.allowedCategories?.length ?? 0) > 0
            ? (row.rentTerms?.allowedCategories ?? []).map(scopeLabel).join("、")
            : "未配置（下单不拦截）",
        },
        { label: "经营范围描述", value: row.rentTerms?.businessScope ?? "—" },
        { label: "所属综合体", value: row.projectId ?? "—" },
        auditTraceField(navigateTo, "contract", row.id),
      ],
    });
  }

  // 合同变更整行明细：变更号/类型/审批状态（均中文），并挂本合同下钻。
  function openChangeDetail(ch: ChangeRow) {
    setDetail({
      title: `合同变更明细 · ${shortId(ch.id)}`,
      fields: [
        { label: "变更号", value: ch.id },
        {
          label: "所属合同",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={ch.contractId}>{shortId(ch.contractId)}</TraceLink>,
        },
        { label: "变更类型", value: formatBusinessStatus(ch.changeType) },
        { label: "审批状态", value: formatBusinessStatus(ch.approvalStatus) },
      ],
    });
  }

  // 退租结算整行明细：结算单号/触发/状态（均中文），并挂本合同下钻。
  function openSettlementDetail(st: SettlementRow) {
    const yuan = (v: unknown): string => {
      const n = Number(v);
      return Number.isFinite(n)
        ? new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n / 100)
        : "—";
    };
    const fields: RowDetailField[] = [
      { label: "结算单号", value: st.id },
      {
        label: "所属合同",
        value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={st.contractId}>{shortId(st.contractId)}</TraceLink>,
      },
      { label: "触发方式", value: formatBusinessStatus(st.triggerType) },
      { label: "结算状态", value: formatBusinessStatus(st.status) },
    ];
    // 退租结算清单逐项：未清清算 / 押金处置（抵扣·净额或仍欠） / 提前退租违约金。
    for (const raw of Array.isArray(st.settlementItems) ? st.settlementItems : []) {
      const it = raw as Record<string, unknown>;
      const key = String(it.item ?? "");
      if (key === "unpaid_receivable_summary") {
        fields.push({ label: "未清应收合计", value: `¥${yuan(it.amountCent)}（已在应收台账，不重复挂账）` });
      } else if (key === "deposit_disposal") {
        fields.push({ label: "押金", value: `¥${yuan(it.depositCent)}` });
        fields.push({ label: "抵扣未清", value: `¥${yuan(it.offsetCent)}` });
        if (Number(it.netRefundCent ?? 0) > 0) {
          fields.push({ label: "应退押金净额", value: `¥${yuan(it.netRefundCent)}（挂应付给商户）` });
        } else {
          fields.push({ label: "商户仍欠差额", value: `¥${yuan(it.merchantStillOwesCent)}（不挂应付，列示）` });
        }
      } else if (key === "termination_penalty") {
        fields.push({ label: "提前退租违约金", value: `¥${yuan(it.amountCent)}（挂应收）` });
        if (it.reason) fields.push({ label: "违约金理由", value: String(it.reason) });
      }
    }
    setDetail({ title: `退租结算明细 · ${shortId(st.id)}`, fields });
  }

  return (
    <section data-testid="contract-workbench-page">
      <AdminPageHeader
        title="合同生命周期工作台"
        description="起草 → 分组并行审批（业务/财务/法务并行 → 总经理终审，按综合体可配）→ 生效固化收益快照 + 绑铺位 → 变更/退租逐例结算。转租不支持。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {configNotice ? (
          <div className="admin-state" data-testid="contract-config-degraded">审批链/到期着色/铺位租赁状态等配置{configNotice}</div>
        ) : null}
        <div className="admin-preview" data-testid="contract-workbench-guide">
          <strong>这一页做什么：</strong>合同全生命周期——① 合同（起草 / 审批 / 生效 / 排租金 AR）② 审批与配置（审批链、铺位到期着色）③ 变更与结算（合同变更、退租结算，点合同「明细」联动）④ 铺位平面图。切换标签操作。
        </div>
        <Tabs
          data-testid="contract-workbench-tabs"
          items={[
            { key: "contracts", label: "① 合同", forceRender: true, children: (<>
        <h3>合同</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contracts-table">
            <thead><tr><th>合同号</th><th>商户</th><th>状态</th><th>租期</th><th>抽成</th><th className="admin-op-col">操作</th><th>详情</th></tr></thead>
            <tbody>
              {contracts.map((row) => (
                <tr key={row.id} data-testid={`contract-${row.id}`}
                  className={["admin-row-clickable", row.id === focusObjectId ? "admin-row-selected" : ""].filter(Boolean).join(" ")}
                  tabIndex={0}
                  onClick={() => openContractDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openContractDetail(row); } }}>
                  <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td>
                  <td title={row.merchantId} onClick={(event) => event.stopPropagation()}>
                    <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>
                      {merchantNames[row.merchantId] ?? <span className="admin-id-cell">{shortId(row.merchantId)}</span>}
                    </TraceLink>
                  </td>
                  <td>{formatBusinessStatus(row.lifecycleStatus)}</td>
                  <td>{cnDate(row.effectiveDate) + " → " + cnDate(row.expiryDate)}</td>
                  <td>{formatBusinessStatus(row.commissionMode)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = [];
                      if (row.lifecycleStatus === "drafting") acts.push({ key: "submit", label: "提交审批", permissions: DRAFT, onClick: () => void submit(row), testId: `contract-submit-${row.id}` });
                      if (row.lifecycleStatus === "pending_confirmation") APPROVE_ROLES.forEach((r) => acts.push({ key: `approve-${r.code}`, label: r.label, permissions: [r.code], onClick: () => void approve(row, r.code), testId: `contract-approve-${r.code}-${row.id}` }));
                      if (row.lifecycleStatus === "pending_effective") acts.push({ key: "activate", label: "生效(绑铺位)", permissions: ACTIVATE, onClick: () => void activate(row), testId: `contract-activate-${row.id}` });
                      if (row.lifecycleStatus === "effective") {
                        acts.push({ key: "period-bills", label: "排租金AR", permissions: RENT_AR, onClick: () => void generatePeriodBills(row), testId: `contract-period-bills-${row.id}` });
                        acts.push({ key: "settle", label: "退租开账", permissions: SETTLE, onClick: () => void settle(row), testId: `contract-settle-${row.id}` });
                      }
                      if (row.lifecycleStatus === "expired") {
                        // 到期合同经带确认的结算入口落押金退还（选「到期」类型），堵住到期押金悬空。
                        acts.push({ key: "settle", label: "结算开账", permissions: SETTLE, onClick: () => void settle(row, "expiry"), testId: `contract-settle-${row.id}` });
                      }
                      acts.push({ key: "detail", label: "明细", onClick: () => void selectContract(row.id), testId: `contract-detail-${row.id}` });
                      return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`contract-actions-${row.id}`} />;
                    })()}
                  </td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看合同详情" onClick={(event) => { event.stopPropagation(); openContractDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
        {/* 引导式流程 phase-2：起草合同入口按钮（权限门在入口），点击弹出分步引导。 */}
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={DRAFT}
            data-testid="contract-draft-open" onClick={() => { setDraftError(""); setCreateOpen(true); }}>＋ 起草合同</PermissionButton>
        </div>
        <GuidedFlowModal
          open={createOpen}
          title="起草合同"
          testId="contract-draft"
          submitTestId="contract-draft-create"
          submitLabel="起草合同"
          error={draftError}
          onClose={() => setCreateOpen(false)}
          onSubmit={() => void createDraft()}
          steps={[
            {
              title: "合同双方",
              description: "填写商户与合同/结算主体（结算主体缺省时取合同主体）。",
              validate: () => {
                if (!draft.merchantId.trim()) return "商户 id 不能为空。";
                if (!draft.contractSubjectId.trim()) return "合同主体 id 不能为空。";
                return null;
              },
              content: (
                <>
                  <label>商户<select aria-label="商户 id" value={draft.merchantId} onChange={(e) => setDraft({ ...draft, merchantId: e.target.value })}>
                    <option value="">请选择商户</option>
                    {merchants.map((m) => <option key={m.id} value={m.id}>{(m.displayName ?? m.companyName ?? m.id)}（{shortId(m.id)}）</option>)}
                  </select></label>
                  <label>合同主体<select aria-label="合同主体 id" value={draft.contractSubjectId} onChange={(e) => setDraft({ ...draft, contractSubjectId: e.target.value })}>
                    <option value="">请选择合同主体</option>
                    {entities.map((en) => <option key={en.id} value={en.id}>{(en.entityName ?? en.id)}（{shortId(en.id)}）</option>)}
                  </select></label>
                  <label>结算主体<select aria-label="结算主体 id" value={draft.settlementSubjectId} onChange={(e) => setDraft({ ...draft, settlementSubjectId: e.target.value })}>
                    <option value="">缺省 = 合同主体</option>
                    {entities.map((en) => <option key={en.id} value={en.id}>{(en.entityName ?? en.id)}（{shortId(en.id)}）</option>)}
                  </select></label>
                </>
              ),
            },
            {
              title: "租期与租金",
              description: "选择生效/到期日期，填写月租金（可空）。",
              validate: () => {
                if (!draft.effectiveDate) return "请选择生效日期。";
                if (!draft.expiryDate) return "请选择到期日期。";
                return null;
              },
              content: (
                <>
                  <label>生效日期<CnDatePicker aria-label="生效日期" value={draft.effectiveDate} onChange={(v) => setDraft({ ...draft, effectiveDate: v })} /></label>
                  <label>到期日期<CnDatePicker aria-label="到期日期" value={draft.expiryDate} onChange={(v) => setDraft({ ...draft, expiryDate: v })} /></label>
                  <label>租金口径
                    <select aria-label="租金口径" value={draft.rentMode} onChange={(e) => setDraft({ ...draft, rentMode: e.target.value })}>
                      <option value="monthly_fixed">按月租金</option>
                      <option value="per_sqm">按每平米单价</option>
                    </select>
                  </label>
                  {draft.rentMode === "monthly_fixed" ? (
                    <label>月租金（元）<input aria-label="月租金（元）" type="number" min="0" placeholder="月租金（元）" value={draft.baseRentYuan} onChange={(e) => setDraft({ ...draft, baseRentYuan: e.target.value })} /></label>
                  ) : (
                    <label>每平米单价（元/㎡）<input aria-label="每平米单价（元/㎡）" type="number" min="0" placeholder="每平米单价（元/㎡）" value={draft.perSqmYuan} onChange={(e) => setDraft({ ...draft, perSqmYuan: e.target.value })} /></label>
                  )}
                  {draft.rentMode === "per_sqm" ? <small style={{ color: "var(--adm-text-3)" }}>月租金 = 每平米单价 × 铺位登记面积，合同生效绑定铺位时自动固化。</small> : null}
                  <label>收租方式
                    <select aria-label="收租方式" value={draft.rentBillingMode} onChange={(e) => setDraft({ ...draft, rentBillingMode: e.target.value })}>
                      <option value="fixed_only">纯月租（只收月租）</option>
                      <option value="turnover_only">纯提成（只按营业额提成）</option>
                      <option value="higher_of">取高（月租与提成谁高收谁）</option>
                      <option value="sum">叠加（月租 + 提成都收）</option>
                    </select>
                  </label>
                  {draft.rentBillingMode !== "fixed_only" ? <small style={{ color: "var(--adm-text-3)" }}>提成规则（阶梯档位）在合同生效后于「审批与配置」页按合同配置。</small> : null}
                </>
              ),
            },
            {
              title: "结算方式",
              description: "选择抽成模式与能源费分摊口径。",
              content: (
                <>
                  <label>抽成模式<select aria-label="抽成模式" value={draft.commissionMode} onChange={(e) => setDraft({ ...draft, commissionMode: e.target.value })}>
                    <option value="fixed_ratio">固定比例</option>
                    <option value="guarantee_or_commission_higher">保底与抽成取高</option>
                    <option value="tiered_ratio">阶梯比例</option>
                    <option value="guarantee_plus_commission">保底+抽成叠加</option>
                  </select></label>
                  <label>能源费分摊<select aria-label="能源费分摊" value={draft.energyFeeBasis} onChange={(e) => setDraft({ ...draft, energyFeeBasis: e.target.value })}>
                    <option value="by_area">按面积</option>
                    <option value="by_usage">按用量</option>
                    <option value="by_contract">按合同约定</option>
                    <option value="none">不分摊</option>
                  </select></label>
                </>
              ),
            },
            {
              title: "经营范围",
              description: "勾选合同允许经营的业务类目（可多选，可空）。勾选后商户在酒吧/票务下单超出范围会被拦截；不勾选则不启用拦截（存量口径）。",
              content: (
                <>
                  <div role="group" aria-label="经营范围类目" data-testid="contract-scope-categories"
                    style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px" }}>
                    {SCOPE_CATEGORIES.map((c) => (
                      <label key={c.v} style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
                        <input type="checkbox" aria-label={`经营范围-${c.l}`}
                          checked={draft.allowedCategories.includes(c.v)}
                          onChange={(e) => setDraft({
                            ...draft,
                            allowedCategories: e.target.checked
                              ? [...draft.allowedCategories, c.v]
                              : draft.allowedCategories.filter((x) => x !== c.v),
                          })} />
                        {c.l}
                      </label>
                    ))}
                  </div>
                  <label>经营范围文字描述（可空，随勾选并存）<input aria-label="经营范围描述"
                    placeholder="如：酒吧经营，含啤酒与小吃" value={draft.businessScope}
                    onChange={(e) => setDraft({ ...draft, businessScope: e.target.value })} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "商户 id", value: draft.merchantId.trim() || "未填写" },
            { label: "合同主体 id", value: draft.contractSubjectId.trim() || "未填写" },
            { label: "结算主体 id", value: draft.settlementSubjectId.trim() || "缺省取合同主体" },
            { label: "生效日期", value: draft.effectiveDate || "未选择" },
            { label: "到期日期", value: draft.expiryDate || "未选择" },
            { label: "月租金（元）", value: draft.baseRentYuan.trim() || "未填写" },
            { label: "抽成模式", value: formatBusinessStatus(draft.commissionMode) },
            { label: "经营范围", value: draft.allowedCategories.length ? draft.allowedCategories.map(scopeLabel).join("、") : "未勾选（不拦截）" },
          ]}
        />
        <div className="admin-inline-form" data-testid="contract-activate-form">
          <input aria-label="生效绑定空间 id" placeholder="生效绑定空间 id" value={spaceId} onChange={(e) => setSpaceId(e.target.value)} />
          <CnDatePicker mode="month" aria-label="排租金AR账期" data-testid="contract-bill-period" value={billPeriod} onChange={setBillPeriod} />
        </div>

          </>) },
          { key: "config", label: "② 审批与配置", forceRender: true, children: (<>
        <h3>审批链（分组并行，按综合体可配）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-flow-table">
            <thead><tr><th>阶段</th><th>阶段名</th><th>审批岗位</th></tr></thead>
            <tbody>
              {flow.map((s, i) => (
                <tr key={i} data-testid={`contract-flow-step-${i}`}>
                  <td>{s.stageSeq}</td><td>{s.stageName}</td><td title={s.approverPermissionCode}>{ROLE_LABELS[s.approverPermissionCode] ?? s.approverPermissionCode}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={FLOW_CFG}
            data-testid="contract-flow-reset" onClick={() => void resetFlowToDefault()}>重置为默认链(①业务/财务/法务并行→②总经理)</PermissionButton>
        </div>

        <h3>提成租金规则（阶梯超额累进，挂合同）</h3>
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>按月销售额分档，每档只对落在本档区间的销售额用本档费率（像个税）；保底=提成下限（可空），无封顶。合同「收租方式」选纯提成/取高/叠加时按此规则出账。</p>
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }} data-testid="turnover-rule-draft-notice">
          提交后<strong>先存为草案，不会马上影响结算</strong>：须由另一位有结算配置管理权限的同事在「结算配置中心 → 待复核草案」里复核转正，转正后才对后续结算生效（同合同同商户的旧规则在转正时自动下线留档）。起草人不能自己转正。
        </p>
        <div className="admin-inline-form">
          <input aria-label="提成-合同id" placeholder="合同 id" value={turnoverForm.contractId} onChange={(e) => setTurnoverForm({ ...turnoverForm, contractId: e.target.value })} />
          <input aria-label="提成-商户id" placeholder="商户 id" value={turnoverForm.merchantId} onChange={(e) => setTurnoverForm({ ...turnoverForm, merchantId: e.target.value })} />
          <input aria-label="提成-规则名" placeholder="规则名" value={turnoverForm.ruleName} onChange={(e) => setTurnoverForm({ ...turnoverForm, ruleName: e.target.value })} />
          <input aria-label="提成-保底额（元，可空）" type="number" min="0" placeholder="保底额（元，可空）" value={turnoverForm.minYuan} onChange={(e) => setTurnoverForm({ ...turnoverForm, minYuan: e.target.value })} />
          <label>生效起<CnDatePicker aria-label="提成生效起" value={turnoverForm.effectiveFrom} onChange={(v) => setTurnoverForm({ ...turnoverForm, effectiveFrom: v })} /></label>
          <label>生效止（可空）<CnDatePicker aria-label="提成生效止" value={turnoverForm.effectiveTo} onChange={(v) => setTurnoverForm({ ...turnoverForm, effectiveTo: v })} /></label>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="turnover-tier-table">
            <thead><tr><th>档序</th><th>销售额上限（万元，末档留空=无上限）</th><th>该段提成率（%）</th><th>操作</th></tr></thead>
            <tbody>
              {turnoverTiers.map((t, i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td><input aria-label={`提成档${i + 1}上限万元`} type="number" min="0" placeholder={i === turnoverTiers.length - 1 ? "末档无上限" : "上限（万元）"} value={t.upperWan} disabled={i === turnoverTiers.length - 1} onChange={(e) => setTurnoverTiers(turnoverTiers.map((x, j) => (j === i ? { ...x, upperWan: e.target.value } : x)))} /></td>
                  <td><input aria-label={`提成档${i + 1}费率`} type="number" min="0" placeholder="提成率（%）" value={t.ratePct} onChange={(e) => setTurnoverTiers(turnoverTiers.map((x, j) => (j === i ? { ...x, ratePct: e.target.value } : x)))} /></td>
                  <td><button type="button" onClick={() => setTurnoverTiers(turnoverTiers.filter((_, j) => j !== i))} disabled={turnoverTiers.length <= 1}>删</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form">
          <button type="button" onClick={() => setTurnoverTiers([...turnoverTiers, { upperWan: "", ratePct: "" }])}>＋ 加一档</button>
          <PermissionButton permissionsState={effectivePermissions} permissions={["finance.turnover_rent.bill"]}
            data-testid="turnover-rule-create" onClick={() => void submitTurnoverRule()}>存为草案（待复核转正）</PermissionButton>
        </div>

        <h3>铺位到期着色分类</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-color-table">
            <thead><tr><th>分类</th><th>颜色</th><th>提前天数</th></tr></thead>
            <tbody>
              {colorRules.map((r) => (
                <tr key={r.id} data-testid={`contract-color-${r.id}`}>
                  <td>{r.categoryName}</td>
                  <td><span style={{ background: r.color, padding: "0 12px" }}>{r.color}</span></td>
                  <td>{r.thresholdDays}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="contract-color-form">
          <input aria-label="分类名" placeholder="分类名(如 临期红)" value={colorForm.categoryName} onChange={(e) => setColorForm({ ...colorForm, categoryName: e.target.value })} />
          <input aria-label="颜色" type="color" value={colorForm.color} onChange={(e) => setColorForm({ ...colorForm, color: e.target.value })} />
          <input aria-label="提前天数" type="number" min="1" placeholder="提前天数" value={colorForm.thresholdDays} onChange={(e) => setColorForm({ ...colorForm, thresholdDays: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={COLOR_CFG}
            data-testid="contract-color-add" onClick={() => void addColorRule()}>新增着色分类</PermissionButton>
        </div>

          </>) },
          { key: "changes", label: "③ 变更与结算", forceRender: true, children: (<>
        <h3>合同变更（含变更子合同/超范围申请，走同一审批链）{selectedId ? <span className="admin-id-cell" title={selectedId}> · {shortId(selectedId)}</span> : "（点合同『明细』选）"}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-changes-table">
            <thead><tr><th>变更号</th><th>类型</th><th>审批</th><th className="admin-op-col">操作</th><th>详情</th></tr></thead>
            <tbody>
              {changes.map((ch) => (
                <tr key={ch.id} data-testid={`contract-change-${ch.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openChangeDetail(ch)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openChangeDetail(ch); } }}>
                  <td className="admin-id-cell" title={ch.id}>{shortId(ch.id)}</td><td>{formatBusinessStatus(ch.changeType)}</td><td>{formatBusinessStatus(ch.approvalStatus)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>{(() => {
                    const acts: RowAction[] = [];
                    if (ch.approvalStatus === "pending") APPROVE_ROLES.forEach((r) => acts.push({ key: `approve-${r.code}`, label: r.label, permissions: [r.code], onClick: () => void approveChange(ch.id, r.code), testId: `change-approve-${r.code}-${ch.id}` }));
                    return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`change-actions-${ch.id}`} />;
                  })()}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看合同变更详情" onClick={(event) => { event.stopPropagation(); openChangeDetail(ch); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="contract-change-form">
          <select aria-label="变更类型" value={changeForm.changeType} onChange={(e) => setChangeForm({ ...changeForm, changeType: e.target.value })}>
            {CHANGE_TYPES.map((t) => <option key={t.v} value={t.v}>{t.l}</option>)}
          </select>
          <input aria-label="变更说明" placeholder="变更说明 / payload" value={changeForm.note} onChange={(e) => setChangeForm({ ...changeForm, note: e.target.value })} />
          {changeForm.changeType === "business_scope_change" ? (
            <div role="group" aria-label="变更后经营范围类目" data-testid="contract-change-scope-categories"
              style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", width: "100%" }}>
              {SCOPE_CATEGORIES.map((c) => (
                <label key={c.v} style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
                  <input type="checkbox" aria-label={`变更经营范围-${c.l}`} disabled={changeForm.clearScope}
                    checked={changeForm.allowedCategories.includes(c.v)}
                    onChange={(e) => setChangeForm({
                      ...changeForm,
                      allowedCategories: e.target.checked
                        ? [...changeForm.allowedCategories, c.v]
                        : changeForm.allowedCategories.filter((x) => x !== c.v),
                    })} />
                  {c.l}
                </label>
              ))}
              <label style={{ display: "inline-flex", alignItems: "center", gap: 4, fontWeight: 600 }}>
                <input type="checkbox" aria-label="清空经营范围" checked={changeForm.clearScope}
                  onChange={(e) => setChangeForm({
                    ...changeForm, clearScope: e.target.checked,
                    allowedCategories: e.target.checked ? [] : changeForm.allowedCategories,
                  })} />
                清空经营范围（审批通过后下单不再拦截）
              </label>
            </div>
          ) : null}
          {(changeForm.changeType === "area_change" || changeForm.changeType === "location_change") ? (
            <div data-testid="contract-amendment-fields" style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", width: "100%", alignItems: "center" }}>
              <span style={{ fontWeight: 600 }}>变更子合同：基于原合同派生（继承主体/收费口径，原合同置已变更）</span>
              <input aria-label="新押金（元）" placeholder="新押金（元，补/退差额走押金台账）" inputMode="decimal"
                value={changeForm.newDepositYuan} onChange={(e) => setChangeForm({ ...changeForm, newDepositYuan: e.target.value })} />
              {changeForm.changeType === "location_change" ? (
                <input aria-label="新经营位置铺位" placeholder="新铺位ID（逗号分隔；不填=沿用原铺位）"
                  value={changeForm.spaceIds} onChange={(e) => setChangeForm({ ...changeForm, spaceIds: e.target.value })} />
              ) : null}
            </div>
          ) : null}
          <PermissionButton permissionsState={effectivePermissions} permissions={CHANGE_MANAGE}
            data-testid="contract-change-raise" onClick={() => void raiseChange()}>发起变更</PermissionButton>
        </div>

        <h3>超范围下单经营申请（#30：下单被经营范围拦下后可申请，走同一审批链，通过则放行）</h3>
        <div className="admin-inline-form" data-testid="contract-scope-request-form">
          <select aria-label="申请经营品类" value={scopeReqForm.category} onChange={(e) => setScopeReqForm({ ...scopeReqForm, category: e.target.value })}>
            {SCOPE_CATEGORIES.map((cc) => <option key={cc.v} value={cc.v}>{cc.l}</option>)}
          </select>
          <select aria-label="许可类型" value={scopeReqForm.grantType} onChange={(e) => setScopeReqForm({ ...scopeReqForm, grantType: e.target.value })}>
            <option value="permanent">永久许可</option>
            <option value="temporary">临时许可</option>
          </select>
          {scopeReqForm.grantType === "temporary" ? (
            <input type="date" aria-label="临时许可有效期至" value={scopeReqForm.validUntil}
              onChange={(e) => setScopeReqForm({ ...scopeReqForm, validUntil: e.target.value })} />
          ) : null}
          <input aria-label="申请原因" placeholder="申请原因（必填）" value={scopeReqForm.reason}
            onChange={(e) => setScopeReqForm({ ...scopeReqForm, reason: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={["contract.scope_request.raise"]}
            data-testid="contract-scope-request-raise" onClick={() => void requestScopeGrant()}>提交超范围申请</PermissionButton>
        </div>

        <h3>退租/到期结算（开账待确认退押金 → 确认退押金落账已结清 / 有争议 / 悬挂作废）</h3>
        <div className="admin-inline-form" data-testid="contract-dispute-form">
          <label>结算类型<select aria-label="结算类型" data-testid="contract-settle-trigger" value={settleTrigger} onChange={(e) => setSettleTrigger(e.target.value)}>
            <option value="move_out">退租</option>
            <option value="expiry">到期</option>
            <option value="early_termination">提前终止</option>
          </select></label>
          <input aria-label="争议原因" placeholder="争议原因（退租开账时填则挂起『有争议』）" value={disputeReason} onChange={(e) => setDisputeReason(e.target.value)} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-settlements-table">
            <thead><tr><th>结算单号</th><th>触发</th><th>状态</th><th className="admin-op-col">操作</th><th>详情</th></tr></thead>
            <tbody>
              {settlements.map((st) => (
                <tr key={st.id} data-testid={`contract-settlement-${st.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openSettlementDetail(st)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(st); } }}>
                  <td className="admin-id-cell" title={st.id}>{shortId(st.id)}</td><td>{formatBusinessStatus(st.triggerType)}</td><td>{formatBusinessStatus(st.status)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = [];
                      if (st.status === "disputed") acts.push({ key: "resolve", label: "裁决→待结算", permissions: DISPUTE_RESOLVE, onClick: () => void settlementAction(st.id, "resolve-dispute"), testId: `settlement-resolve-${st.id}` });
                      if (OPEN_SETTLEMENT.includes(st.status)) {
                        acts.push({ key: "complete", label: "确认退押金并落账", permissions: SETTLE, onClick: () => void confirmSettlement(st), testId: `settlement-complete-${st.id}` });
                      }
                      return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`settlement-actions-${st.id}`} />;
                    })()}
                  </td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看退租结算详情" onClick={(event) => { event.stopPropagation(); openSettlementDetail(st); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>

          </>) },
          { key: "floorplan", label: "④ 铺位平面图", forceRender: true, children: (<>
        <h3>铺位平面图（6 态着色）</h3>
        <div data-testid="contract-floorplan" style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {spaceRental.map((sp) => (
            <div key={sp.spaceId} data-testid={`space-${sp.spaceId}`}
              title={`${RENTAL_LABEL[sp.rentalStatus] ?? sp.rentalStatus}${sp.daysToExpiry != null ? ` · ${sp.daysToExpiry} 天到期` : ""}`}
              style={{ minWidth: 96, padding: "8px 10px", borderRadius: 6, color: "#fff",
                background: sp.color ?? RENTAL_COLOR[sp.rentalStatus] ?? "#9ca3af" }}>
              <div style={{ fontWeight: 600 }}>{sp.spaceName}</div>
              <div style={{ fontSize: 12 }}>{RENTAL_LABEL[sp.rentalStatus] ?? sp.rentalStatus}</div>
            </div>
          ))}
          {spaceRental.length === 0 && <span className="admin-muted">暂无铺位</span>}
        </div>
          </>) },
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
