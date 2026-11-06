// 文件版本：V0.21
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { Tabs } from "antd";
import { loadSecondary, loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, BusinessEntityRow, GovernanceProjectRow } from "../types";
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

// 后端 ContractRecord / ContractApprovalTemplateRecord / SpaceExpiryColorRuleRecord（客户端 unwrap 已 camel 化）。
type ContractRow = {
  id: string; merchantId: string; lifecycleStatus: string;
  effectiveDate?: string; expiryDate?: string; commissionMode?: string | null; projectId?: string | null;
  rentTerms?: { businessScope?: string; allowedCategories?: string[]; rentMode?: string; rentBillingMode?: string; baseRentCent?: number; perSqmRentCent?: number; parkingMonthlyCent?: number; depositAdjustment?: DepositAdjustment | null } | null;
  reviewId?: string | null;   // HP-C01：提交审批开的统一审批复核单（空=未提交）
  depositCent?: number | null;   // HP-C03：押金由后端按「收取月数×月租」计算，前端只读
};
// HP-C03-3（BSM L1353「子合同写明目标金额、补退差额及办理期限」）：后端 rent_terms.depositAdjustment（面积/位置变更子合同、续租合同）。
type DepositAdjustment = { targetDepositCent?: number; deltaCent?: number; direction?: string; deadline?: string; itemNo?: string | null };
// V494：审批流程模板按新建／到期终止／变更三类、按综合体可配（BSM L1349）。
// V496（HP-C01-R5）：主线层级数上下限（都空＝未配置不校验）。
type FlowTemplate = {
  id?: string; contractType: string; name: string; requiredLineCodes: string[]; defaultFinalApproverId?: string | null;
  minMainLevels?: number | null; maxMainLevels?: number | null;
};
const CONTRACT_TYPES = ["new", "expiry_termination", "change"];
const CONTRACT_TYPE_LABELS: Record<string, string> = { new: "新建", expiry_termination: "到期终止", change: "变更" };
const LINE_LABELS: Record<string, string> = { business_review: "业务审核", finance_review: "财务审核", legal_review: "法务审核", tax_review: "税务审核" };
type ColorRule = { id: string; categoryName: string; color: string; thresholdDays: number };
// HP-C03（BSM L1347）：押金收取月数（全平台统一；出厂 3 个月，上下限未配置为空）。
type DepositMonths = { value: number; minValue?: number | null; maxValue?: number | null };
// HP-C06（BSM L1352）：押金退还等待期（按项目；项目未配置时返回出厂行 projectId=__global__、30 日、上下限为空）。
type RefundWait = { projectId?: string | null; value: number; minValue?: number | null; maxValue?: number | null };
// HP-C02-R6：退租/提前终止变更载荷带退租日 moveOutDate。
type ChangeRow = {
  id: string; contractId: string; changeType: string; approvalStatus: string; reviewId?: string | null; payload?: { moveOutDate?: string } | null;
  newSnapshot?: { rentTerms?: { depositAdjustment?: DepositAdjustment | null } | null } | null;   // 已批变更的新快照（HP-C03-3 押金补退）
};
const TERMINATION_CHANGES = ["move_out", "early_termination"];
/** 第三轮裁决第 1 条「纯提成合同的押金由招商按合同约定金额直接录入，不走月数公式……这类合同变更/续租时押金不按公式重算」。 */
const TURNOVER_DEPOSIT_HINT = "纯提成合同押金沿用约定金额，不重算、不登记差额";
type SettlementRow = {
  id: string; contractId: string; triggerType: string; status: string; settlementItems?: Array<Record<string, unknown>>;
  // HP-C02 齐单核对事实（V495）：核对人/时间/备注，空＝尚未核对。
  documentsCheckedBy?: string | null; documentsCheckedAt?: string | null; documentsCheckNote?: string | null;
  // HP-C04（V498）：异议理由（含商户「不接受裁决」理由，追加留痕）与主管裁决结论/诉讼仲裁结果。
  disputeReason?: string | null; disputeResolution?: string | null;
  // HP-C02（V500）：到期结算挂的「到期终止」类审批复核单（退租/提前终止的审批挂在退租变更上，此处为空）。
  reviewId?: string | null;
};
type SpaceRental = { spaceId: string; spaceName: string; rentalStatus: string; color?: string | null; daysToExpiry?: number | null };

const DRAFT = ["contract.draft.manage"];
const ACTIVATE = ["contract.lifecycle.activate"];
const RENT_AR = ["finance.ar_ap.manage"];
const SETTLE = ["contract.settlement.manage"];
const FLOW_CFG = ["contract.approval.flow.manage"];
const COLOR_CFG = ["contract.space.color.manage"];
const DEPOSIT_CFG = ["contract.deposit_months.manage"];
const REFUND_WAIT_CFG = ["contract.deposit_refund_wait.manage"];
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
const RENT_BILLING_MODES = [
  { v: "fixed_only", l: "纯月租（只收月租）" }, { v: "turnover_only", l: "纯提成（只按营业额提成）" },
  { v: "higher_of", l: "取高（月租与提成谁高收谁）" }, { v: "sum", l: "叠加（月租 + 提成都收）" },
];
type RentTermsForm = { rentMode: string; rentBillingMode: string; baseRentYuan: string; perSqmYuan: string; parkingMonthlyYuan?: string };
// 起草与草稿编辑共用：租金口径＋收租方式＋月租或每平米单价（元→分，空/0 不带）；
// HP-P01（BSM L1437「商户月租车位按合同约定、按月出账，账单类型为「车位月租」」）：约定的车位月租（元/月→分，空/0 不带）。
function rentTermsOf(f: RentTermsForm): Record<string, unknown> {
  const rentTerms: Record<string, unknown> = { rentMode: f.rentMode, rentBillingMode: f.rentBillingMode };
  const cent = Math.round(Number(f.rentMode === "per_sqm" ? f.perSqmYuan : f.baseRentYuan) * 100);
  if (Number.isFinite(cent) && cent > 0) rentTerms[f.rentMode === "per_sqm" ? "perSqmRentCent" : "baseRentCent"] = cent;
  const parking = Math.round(Number(f.parkingMonthlyYuan ?? "") * 100);
  if (Number.isFinite(parking) && parking > 0) rentTerms.parkingMonthlyCent = parking;
  return rentTerms;
}
// 第三轮控制席约定：齐单核对与「终审自动落账」只作用于落账前的「待结算」；押金抵扣中/欠费追缴中已落账。
const OPEN_SETTLEMENT = ["pending_settlement"];

function yuan(v: unknown): string {
  const n = Number(v);
  return Number.isFinite(n)
    ? new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(n / 100)
    : "—";
}

// HP-C03-3：押金补退只读四项（目标押金、补缴/退还差额、办理期限、台账项号），差额>0 应收补缴、<0 应付退还。
function depositAdjustmentFields(adj?: DepositAdjustment | null): RowDetailField[] {
  if (!adj) return [];
  const refund = adj.direction === "refund";
  return [
    { label: "目标押金", value: `¥${yuan(adj.targetDepositCent)}` },
    { label: refund ? "退还差额" : "补缴差额", value: `¥${yuan(Math.abs(Number(adj.deltaCent ?? 0)))}（${refund ? "应付" : "应收"}）` },
    { label: "押金补退办理期限", value: cnDate(adj.deadline) },
    { label: "台账项号", value: adj.itemNo ?? "-" },
  ];
}

// HP-C01：合同三类审批走全平台统一审批模型——提交/变更/超范围申请时指定主线最终审批人（空=模板默认总经理）与模板各平行线的
// 最终审批人，审批人在「审批链」页逐节点裁决；读不到模板（无配置权限）时按平台默认三线。
const DEFAULT_LINES = ["business_review", "finance_review", "legal_review"];

/**
 * 合同生命周期工作台（ADM-CONT-001，V162 重建）：起草 → 分组并行审批 → 生效固化 → 退租结算 + 审批链/到期着色配置。
 * 覆盖验收 ADM-CONT-001..008：起草/四级并行审批/固化快照/铺位着色配置/变更入口/退租结算/审批链配置。
 */
export function ContractWorkbenchPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [contracts, setContracts] = React.useState<ContractRow[]>([]);
  const [flow, setFlow] = React.useState<FlowTemplate[]>([]);
  const [colorRules, setColorRules] = React.useState<ColorRule[]>([]);
  // 审批链/到期着色/铺位租赁状态为配置与辅助区块，任一失败不拖垮合同主列表。
  const [configNotice, setConfigNotice] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState({
    merchantId: "", contractSubjectId: "", settlementSubjectId: "", effectiveDate: "", expiryDate: "",
    baseRentYuan: "", perSqmYuan: "", parkingMonthlyYuan: "", rentMode: "monthly_fixed", rentBillingMode: "fixed_only",
    commissionMode: "fixed_ratio", energyFeeBasis: "none",
    businessScope: "", allowedCategories: [] as string[],
    depositYuan: "",   // 第三轮裁决第 1 条：纯提成合同录入的合同约定押金（元）
    projectId: "",   // 第十八批 G 段 82b-R3：所属项目（不选＝后端按意向铺位所在项目或本人所在项目落）
  });
  // 第十八批 G 段 82b-R3（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」）：
  // 起草可选所属项目；能看到本人项目以外的项目就显示（复核：角色只勾了 B、只看得到 B 一个时也显示）。
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
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
  // HP-C03-1：面积/位置变更录变更后月租（每平米计租不填）与押金补退办理期限；押金由后端按原合同押金月数×变更后月租计算。
  // HP-C03-4（裁决第 4 条）：续租录续租后月租（每平米计租录单价）、新到期日与押金办理期限（depositDeadline 与面积/位置变更共用）。
  const [changeForm, setChangeForm] = React.useState({ changeType: "rent_change", note: "", allowedCategories: [] as string[], clearScope: false, newRentYuan: "", depositDeadline: "", spaceIds: "", moveOutDate: "", newExpiryDate: "" });
  const [depositMonths, setDepositMonths] = React.useState<DepositMonths | null>(null);
  const [depositForm, setDepositForm] = React.useState({ value: "", min: "", max: "" });
  const [refundWait, setRefundWait] = React.useState<RefundWait | null>(null);
  const [refundWaitForm, setRefundWaitForm] = React.useState({ projectId: "", value: "", min: "", max: "" });
  // HP-C01-R5：各类模板主线层级数上下限的编辑值（按合同类型）。
  const [levelsForm, setLevelsForm] = React.useState<Record<string, { min: string; max: string }>>({});
  // #30 超范围下单经营申请（拦下后可申请，走既有审批链）
  const [scopeReqForm, setScopeReqForm] = React.useState({ category: "spirit", reason: "", grantType: "permanent", validUntil: "" });
  // HP-C01：统一审批的发起入参（提交/变更/超范围申请三个入口共用一套值）。
  const [approval, setApproval] = React.useState<{ finalApproverId: string; reason: string; lines: Record<string, string> }>({ finalApproverId: "", reason: "", lines: {} });
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
  // 第四轮裁决第 4 条「合同草稿可编辑补录」：草稿行补录/修改租金条款（字段沿用起草表单），纯提成录约定押金。
  const [termsEdit, setTermsEdit] = React.useState<(RentTermsForm & { id: string; depositYuan: string }) | null>(null);

  // 无权限副资源不请求（根因修）：审批链配置需 contract.approval.flow.manage、着色规则需 contract.space.color.manage，页面基础权限 contract.draft.manage 不含之。
  const canApprovalFlow = hasPermissions(effectivePermissions, ["contract.approval.flow.manage"]);
  const canColorRules = hasPermissions(effectivePermissions, ["contract.space.color.manage"]);
  const canDepositMonths = hasPermissions(effectivePermissions, DEPOSIT_CFG);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：审批链/到期着色规则/铺位租赁状态为配置与辅助区块，可能因权限/500
      // 单独失败——各自降级为空并合并一条区块提示，不拖垮合同主列表（合同为主资源，读失败才整页报错）。
      const [rows, stepsRes, rulesRes, spacesRes, depositRes] = await Promise.all([
        api.get<ContractRow[]>("/api/v1/contracts"),
        loadSecondaryIfPermitted(canApprovalFlow, () => api.get<FlowTemplate[]>("/api/v1/contracts/approval-flow"), [] as FlowTemplate[]),
        loadSecondaryIfPermitted(canColorRules, () => api.get<ColorRule[]>("/api/v1/contracts/expiry-color-rules"), [] as ColorRule[]),
        loadSecondary(() => api.get<SpaceRental[]>("/api/v1/contracts/space-rental-status"), [] as SpaceRental[]),
        loadSecondaryIfPermitted(canDepositMonths, () => api.get<DepositMonths>("/api/v1/contracts/deposit-months"), null as DepositMonths | null),
      ]);
      setContracts(rows ?? []);
      setFlow(stepsRes.data);
      setColorRules(rulesRes.data);
      setSpaceRental(spacesRes.data);
      setDepositMonths(depositRes.data);
      setConfigNotice(stepsRes.notice ?? rulesRes.notice ?? spacesRes.notice ?? depositRes.notice);
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
      try {
        setProjects(hasPermissions(effectivePermissions, ["governance.project.view"])
          ? (await api.get<GovernanceProjectRow[]>("/api/v1/platform/projects")) ?? [] : []);
      } catch { setProjects([]); }
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
    const rentTerms = rentTermsOf(draft);
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
        // 第三轮裁决第 1 条：纯提成合同押金按合同约定金额录入；其余收租方式不传，押金由后端按「收取月数×签约月租」计算。
        depositCent: draft.rentBillingMode === "turnover_only" ? Math.round(Number(draft.depositYuan) * 100) : undefined,
        ...(draft.projectId ? { projectId: draft.projectId } : {}),
      });
      setActionResult({ action: "contract_drafted", merchantId: draft.merchantId.trim() });
      setDraft({ ...draft, merchantId: "", baseRentYuan: "", perSqmYuan: "", parkingMonthlyYuan: "", businessScope: "", allowedCategories: [], depositYuan: "" });
      setCreateOpen(false);
      reload();
    } catch (createError) {
      setDraftError(createError instanceof Error ? createError.message : "起草合同失败");
    }
  }

  // HP-C01：按合同类型取模板要求的平行线，逐线取最终审批人；主线最终审批人空=后端取模板默认（总经理）。
  function templateLines(contractType: string): string[] {
    return flow.find((t) => t.contractType === contractType)?.requiredLineCodes ?? DEFAULT_LINES;
  }
  function approvalBody(contractType: string) {
    const missing = templateLines(contractType).filter((lineCode) => !(approval.lines[lineCode] ?? "").trim());
    if (missing.length) {
      setGlobalError("须为平行审批线指定该部门的最终审批人：" + missing.map((c) => LINE_LABELS[c] ?? c).join("、"));
      return null;
    }
    return {
      finalApproverId: approval.finalApproverId.trim() || null,
      parallelLines: templateLines(contractType).map((lineCode) => ({ lineCode, finalApproverId: (approval.lines[lineCode] ?? "").trim() })),
      reason: approval.reason.trim() || null,
    };
  }
  const approvalFields = (contractType: string, prefix: string) => (
    <div data-testid={`${prefix}-approval-fields`} style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", width: "100%" }}>
      <input aria-label={`${prefix}-主线最终审批人`} data-testid={`${prefix}-final-approver`} placeholder="主线最终审批人账号（空=模板默认总经理）"
        value={approval.finalApproverId} onChange={(e) => setApproval({ ...approval, finalApproverId: e.target.value })} />
      {templateLines(contractType).map((line) => (
        <input key={line} aria-label={`${prefix}-${LINE_LABELS[line] ?? line}线最终审批人`} data-testid={`${prefix}-line-${line}`}
          placeholder={`${LINE_LABELS[line] ?? line}线最终审批人账号`} value={approval.lines[line] ?? ""}
          onChange={(e) => setApproval({ ...approval, lines: { ...approval.lines, [line]: e.target.value } })} />
      ))}
      <input aria-label={`${prefix}-申请说明`} data-testid={`${prefix}-reason`} placeholder="申请说明（留痕）"
        value={approval.reason} onChange={(e) => setApproval({ ...approval, reason: e.target.value })} />
    </div>
  );

  async function submit(row: ContractRow) {
    const body = approvalBody("new");
    if (!body) return;
    try {
      await api.post(`/api/v1/contracts/${row.id}/submit`, body);
      setActionResult({ action: "contract_submitted", contractId: row.id });
      reload();
    } catch (e) {
      // 第四轮裁决第 4 条：非纯提成合同未补录月租 → 后端 400「请先在草稿上补录月租」，原样提示。
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  // 第四轮裁决第 4 条「合同草稿可编辑补录」：只开放租金条款与纯提成约定押金；有月租的押金由后端按「收取月数×月租」重算。
  function openTermsEdit(row: ContractRow) {
    const rt = row.rentTerms ?? {};
    setTermsEdit({
      id: row.id, rentMode: rt.rentMode ?? "monthly_fixed", rentBillingMode: rt.rentBillingMode ?? "fixed_only",
      baseRentYuan: rt.baseRentCent ? String(rt.baseRentCent / 100) : "", perSqmYuan: rt.perSqmRentCent ? String(rt.perSqmRentCent / 100) : "",
      parkingMonthlyYuan: rt.parkingMonthlyCent ? String(rt.parkingMonthlyCent / 100) : "",
      depositYuan: rt.rentBillingMode === "turnover_only" && row.depositCent ? String(row.depositCent / 100) : "",
    });
  }
  async function saveTermsEdit() {
    if (!termsEdit) return;
    try {
      await api.post(`/api/v1/contracts/${termsEdit.id}/rent-terms`, {
        rentTerms: rentTermsOf(termsEdit),
        depositCent: termsEdit.rentBillingMode === "turnover_only" ? Math.round(Number(termsEdit.depositYuan) * 100) : undefined,
      });
      setActionResult({ action: "contract_draft_rent_terms_saved", contractId: termsEdit.id });
      setTermsEdit(null);
      reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
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
    try {
      const bills = await api.post<unknown[]>(`/api/v1/contracts/${row.id}/period-bills`, { period: billPeriod });
      setActionResult({ action: "contract_period_bills_generated", contractId: row.id, period: billPeriod, count: (bills ?? []).length });
      reload();
    } catch (e) {
      // 第十八批 V 段第 159 条 159-R4：账期内营业日数据未标记为最终（409 TURNOVER_DATA_NOT_FINAL）等拒绝原样提示。
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  async function settle(row: ContractRow, forceTrigger?: string) {
    // 退租/到期开账（填争议原因则挂起有争议）。开账只算不落账。
    // I-3 到期入口强制 expiry（按合同生命周期派生，不吃共享下拉默认 move_out，否则到期合同会被误作废、押金静默不退）。
    // HP-C02（BSM「退租、提前终止、合同到期等各种终止结算，都要经过多级审批」「审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」）：
    // 到期结算开账即按「到期终止」类模板发起审批（主线最终审批人＋各平行线最终审批人），终审通过后端自动落账。
    const triggerType = forceTrigger ?? settleTrigger;
    const approvalFor = triggerType === "expiry" ? approvalBody("expiry_termination") : {};
    if (!approvalFor) return;
    await api.post(`/api/v1/contracts/${row.id}/settle`, {
      triggerType, disputeReason: disputeReason.trim() || null, ...approvalFor,
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

  const selectedPerSqm = contracts.find((c) => c.id === selectedId)?.rentTerms?.rentMode === "per_sqm";
  // 第三轮裁决第 1 条「这类合同变更/续租时押金不按公式重算」：纯提成合同没有月租，不录变更/续租后月租与押金补退办理期限。
  const selectedTurnoverOnly = contracts.find((c) => c.id === selectedId)?.rentTerms?.rentBillingMode === "turnover_only";

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
      // #31 变更子合同：变更后月租（元→分；每平米计租不填）、押金补退办理期限 + 经营位置变更的新铺位（逗号分隔）。
      // HP-C03-1：目标押金＝原合同押金月数×变更后月租由后端计算，缺办理期限/月租由后端 VALIDATION_FAILED 拒绝。
      if (!selectedTurnoverOnly && changeForm.newRentYuan.trim()) payload.baseRentCent = Math.round(Number(changeForm.newRentYuan.trim()) * 100);
      if (!selectedTurnoverOnly && changeForm.depositDeadline) payload.depositDeadline = changeForm.depositDeadline;
      const spaces = changeForm.spaceIds.split(/[,，\s]+/).map((x) => x.trim()).filter(Boolean);
      if (spaces.length) payload.spaceIds = spaces;
    }
    // HP-C02-R6（裁决「发起退租/提前终止变更时必填「退租日」」）：退租日随载荷提交，缺失由后端 VALIDATION_FAILED 拒绝。
    if (TERMINATION_CHANGES.includes(changeForm.changeType) && changeForm.moveOutDate) payload.moveOutDate = changeForm.moveOutDate;
    if (changeForm.changeType === "renewal") {
      // HP-C03-4：每平米计租给续租后单价（后端按铺位面积算月租），否则给续租后月租；缺项/到期日不晚于现到期日由后端 VALIDATION_FAILED 拒绝。
      const cent = !selectedTurnoverOnly && changeForm.newRentYuan.trim() ? Math.round(Number(changeForm.newRentYuan.trim()) * 100) : null;
      if (cent != null) payload[selectedPerSqm ? "perSqmRentCent" : "baseRentCent"] = cent;
      if (changeForm.newExpiryDate) payload.newExpiryDate = changeForm.newExpiryDate;
      if (!selectedTurnoverOnly && changeForm.depositDeadline) payload.depositDeadline = changeForm.depositDeadline;
    }
    // 第五轮裁决第 2 条「租金调整生效时……按调整后月租重排剩余租期」：租金调整给调整后月租（每平米计租给调整后单价，后端按铺位面积算月租）；缺项由后端 VALIDATION_FAILED 拒绝。
    if (changeForm.changeType === "rent_change" && !selectedTurnoverOnly && changeForm.newRentYuan.trim()) {
      payload[selectedPerSqm ? "perSqmRentCent" : "baseRentCent"] = Math.round(Number(changeForm.newRentYuan.trim()) * 100);
    }
    const approvalFor = approvalBody("change");
    if (!approvalFor) return;
    await api.post(`/api/v1/contracts/${selectedId}/changes`, {
      changeType: changeForm.changeType, payload, ...approvalFor,
    });
    setActionResult({ action: "contract_change_raised", contractId: selectedId, type: changeForm.changeType });
    void selectContract(selectedId);
  }

  async function requestScopeGrant() {
    if (!selectedId) { setGlobalError("先点合同行『明细』选一份合同"); return; }
    if (!scopeReqForm.reason.trim()) { setGlobalError("超范围经营申请须填原因"); return; }
    const approvalFor = approvalBody("change");
    if (!approvalFor) return;
    const body: Record<string, unknown> = {
      ...approvalFor,
      category: scopeReqForm.category, reason: scopeReqForm.reason.trim(), grantType: scopeReqForm.grantType,
    };
    if (scopeReqForm.grantType === "temporary" && scopeReqForm.validUntil) body.validUntil = scopeReqForm.validUntil;
    await api.post(`/api/v1/contracts/${selectedId}/scope-requests`, body);
    setActionResult({ action: "contract_scope_request_raised", contractId: selectedId, category: scopeReqForm.category });
    setScopeReqForm({ ...scopeReqForm, reason: "" });
    void selectContract(selectedId);
  }

  // HP-C04（裁决第 5、6 条「主管裁决（写结论留痕）后回待结算继续审批」「诉讼/仲裁有结果后，由有裁决权的主管在后台录入结果」）：结论必填。
  async function resolveSettlementDispute(sid: string, promptLabel: string) {
    const note = window.prompt(promptLabel)?.trim();
    if (!note) return;
    await api.post(`/api/v1/contracts/settlements/${sid}/resolve-dispute`, { resolutionNote: note });
    setActionResult({ action: "contract_settlement_resolve-dispute", settlementId: sid });
    await selectContract(selectedId);
    reload();
  }

  // HP-C02 齐单核对（BSM「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」）：
  // 缺项 409 直接把缺单明细（yyyy-MM:fee_type）展示出来，结算单保持原态。
  async function checkSettlementDocuments(st: SettlementRow) {
    try {
      await api.post(`/api/v1/contracts/settlements/${st.id}/documents-check`, {});
      setActionResult({ action: "contract_settlement_documents_checked", settlementId: st.id });
      await selectContract(selectedId);
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  async function resetFlowToDefault(contractType: string) {
    // 该类平台默认模板重置为出厂三线（业务/财务/法务，V494 种子同形），默认最终审批人留空=发起时指定。
    await api.post("/api/v1/contracts/approval-flow", {
      contractType,
      name: `平台默认·${CONTRACT_TYPE_LABELS[contractType] ?? contractType}`,
      requiredLineCodes: DEFAULT_LINES,
      defaultFinalApproverId: null,
    });
    setActionResult({ action: "contract_flow_saved", contractType });
    reload();
  }

  // HP-C01-R5（BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，须同时配置上下限）」）：保存该类模板主线层级数上下限，
  // 平行线/默认最终审批人沿用现值；都留空＝不校验，只填一项或越界由后端 VALIDATION_FAILED 拒绝。
  async function saveFlowLevels(contractType: string) {
    const t = flow.find((x) => x.contractType === contractType);
    const form = levelsForm[contractType] ?? { min: "", max: "" };
    try {
      await api.post("/api/v1/contracts/approval-flow", {
        contractType,
        name: t?.name ?? `平台默认·${CONTRACT_TYPE_LABELS[contractType] ?? contractType}`,
        requiredLineCodes: t?.requiredLineCodes ?? DEFAULT_LINES,
        defaultFinalApproverId: t?.defaultFinalApproverId ?? null,
        minMainLevels: form.min.trim() ? Number(form.min) : null,
        maxMainLevels: form.max.trim() ? Number(form.max) : null,
      });
      setActionResult({ action: "contract_flow_saved", contractType });
      reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  // HP-C03（BSM L1347「须同时配置上下限；下限不得低于三个月，配置校验拒绝更低的值」）：值与上下限同时提交，校验由后端 VALIDATION_FAILED 拒绝。
  async function saveDepositMonths() {
    try {
      await api.post("/api/v1/contracts/deposit-months", {
        value: Number(depositForm.value), minValue: Number(depositForm.min), maxValue: Number(depositForm.max),
      });
      setActionResult({ action: "contract_deposit_months_saved", value: Number(depositForm.value) });
      reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  // HP-C06（BSM L1352「业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」）：按项目查询/保存，校验由后端 VALIDATION_FAILED 拒绝。
  async function queryRefundWait() {
    try {
      setRefundWait(await api.get<RefundWait>(`/api/v1/contracts/deposit-refund-wait?project_id=${encodeURIComponent(refundWaitForm.projectId.trim())}`));
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
  }

  async function saveRefundWait() {
    try {
      await api.post("/api/v1/contracts/deposit-refund-wait", {
        projectId: refundWaitForm.projectId.trim(), value: Number(refundWaitForm.value),
        minValue: Number(refundWaitForm.min), maxValue: Number(refundWaitForm.max),
      });
      setActionResult({ action: "contract_deposit_refund_wait_saved", projectId: refundWaitForm.projectId.trim(), value: Number(refundWaitForm.value) });
      void queryRefundWait();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : String(e));
    }
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
        { label: "审批复核单", value: row.reviewId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-009" objectId={row.reviewId}>{row.reviewId}</TraceLink> : "-" },
        { label: "生效日期", value: cnDate(row.effectiveDate) },
        { label: "到期日期", value: cnDate(row.expiryDate) },
        { label: "抽成模式", value: formatBusinessStatus(row.commissionMode) },
        { label: "押金", value: row.depositCent != null ? `¥${yuan(row.depositCent)}` : "-" },
        ...depositAdjustmentFields(row.rentTerms?.depositAdjustment),
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
        { label: "审批复核单", value: ch.reviewId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-009" objectId={ch.reviewId}>{ch.reviewId}</TraceLink> : "-" },
        ...(TERMINATION_CHANGES.includes(ch.changeType) ? [{ label: "退租日", value: cnDate(ch.payload?.moveOutDate) }] : []),
        ...depositAdjustmentFields(ch.newSnapshot?.rentTerms?.depositAdjustment),
      ],
    });
  }

  // 退租结算整行明细：结算单号/触发/状态（均中文），并挂本合同下钻。
  function openSettlementDetail(st: SettlementRow) {
    const fields: RowDetailField[] = [
      { label: "结算单号", value: st.id },
      {
        label: "所属合同",
        value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={st.contractId}>{shortId(st.contractId)}</TraceLink>,
      },
      { label: "触发方式", value: formatBusinessStatus(st.triggerType) },
      { label: "结算状态", value: formatBusinessStatus(st.status) },
      {
        label: "齐单核对",
        value: st.documentsCheckedBy
          ? `${st.documentsCheckedBy} · ${cnDate(st.documentsCheckedAt)}${st.documentsCheckNote ? ` · ${st.documentsCheckNote}` : ""}`
          : "未核对（缺单时保持待处理）",
      },
      ...(st.reviewId ? [{ label: "审批复核单", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-009" objectId={st.reviewId}>{st.reviewId}</TraceLink> }] : []),
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
        description="起草 → 提交统一审批（指定主线最终审批人，默认总经理；业务/财务/法务为平行部门线各指定该线最终审批人，按综合体模板可配；审批人在「审批链」页逐节点裁决，全部通过才生效、任一驳回退回起草）→ 生效固化收益快照 + 绑铺位 → 变更/退租逐例结算。转租不支持。"
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
        <div className="admin-inline-form" data-testid="contract-submit-approval-form">
          <span style={{ fontWeight: 600 }}>提交审批（统一审批模型）：</span>
          {approvalFields("new", "contract-submit")}
        </div>
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
                  <td onClick={(event) => event.stopPropagation()}>
                    {formatBusinessStatus(row.lifecycleStatus)}
                    {row.reviewId ? <> · <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-009" objectId={row.reviewId}>审批链 {shortId(row.reviewId)}</TraceLink></> : null}
                  </td>
                  <td>{cnDate(row.effectiveDate) + " → " + cnDate(row.expiryDate)}</td>
                  <td>{formatBusinessStatus(row.commissionMode)}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = [];
                      if (row.lifecycleStatus === "drafting") {
                        acts.push({ key: "submit", label: "提交审批", permissions: DRAFT, onClick: () => void submit(row), testId: `contract-submit-${row.id}` });
                        acts.push({ key: "edit-terms", label: "编辑租金条款", permissions: DRAFT, onClick: () => openTermsEdit(row), testId: `contract-edit-terms-${row.id}` });
                      }
                      if (row.lifecycleStatus === "pending_effective") acts.push({ key: "activate", label: "生效(绑铺位)", permissions: ACTIVATE, onClick: () => void activate(row), testId: `contract-activate-${row.id}` });
                      if (row.lifecycleStatus === "effective") {
                        acts.push({ key: "period-bills", label: "排租金AR", permissions: RENT_AR, onClick: () => void generatePeriodBills(row), testId: `contract-period-bills-${row.id}` });
                        acts.push({ key: "settle", label: "退租开账", permissions: SETTLE, onClick: () => void settle(row), testId: `contract-settle-${row.id}` });
                      }
                      if (row.lifecycleStatus === "expired") {
                        // 第十八批 Q 段第 138 条复核修正：营业额抽成类合同的最后一期在到期、该期月结定格后出，已到期合同仍可排到期日所在月及以前的账期。
                        acts.push({ key: "period-bills", label: "排租金AR", permissions: RENT_AR, onClick: () => void generatePeriodBills(row), testId: `contract-period-bills-${row.id}` });
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
        {termsEdit ? (
          <div className="admin-inline-form" data-testid="contract-edit-terms-form">
            <span style={{ fontWeight: 600 }}>编辑租金条款（草稿 {shortId(termsEdit.id)}）：</span>
            <label>租金口径<select aria-label="编辑-租金口径" value={termsEdit.rentMode} onChange={(e) => setTermsEdit({ ...termsEdit, rentMode: e.target.value })}>
              <option value="monthly_fixed">按月租金</option>
              <option value="per_sqm">按每平米单价</option>
            </select></label>
            {termsEdit.rentMode === "per_sqm" ? (
              <label>每平米单价（元/㎡）<input aria-label="编辑-每平米单价（元/㎡）" type="number" min="0" value={termsEdit.perSqmYuan} onChange={(e) => setTermsEdit({ ...termsEdit, perSqmYuan: e.target.value })} /></label>
            ) : (
              <label>月租金（元）<input aria-label="编辑-月租金（元）" type="number" min="0" value={termsEdit.baseRentYuan} onChange={(e) => setTermsEdit({ ...termsEdit, baseRentYuan: e.target.value })} /></label>
            )}
            <label>收租方式<select aria-label="编辑-收租方式" value={termsEdit.rentBillingMode} onChange={(e) => setTermsEdit({ ...termsEdit, rentBillingMode: e.target.value })}>
              {RENT_BILLING_MODES.map((m) => <option key={m.v} value={m.v}>{m.l}</option>)}
            </select></label>
            <label>车位月租（元/月）<input aria-label="编辑-车位月租（元/月）" type="number" min="0" placeholder="未约定月租车位留空" value={termsEdit.parkingMonthlyYuan ?? ""} onChange={(e) => setTermsEdit({ ...termsEdit, parkingMonthlyYuan: e.target.value })} /></label>
            {termsEdit.rentBillingMode === "turnover_only" ? (
              <label>押金（元）<input aria-label="编辑-押金（元）" type="number" min="0" placeholder="合同约定押金（元）" value={termsEdit.depositYuan} onChange={(e) => setTermsEdit({ ...termsEdit, depositYuan: e.target.value })} /></label>
            ) : <small style={{ color: "var(--adm-text-3)" }}>押金＝收取月数×月租，保存后由系统按公式重算。</small>}
            <PermissionButton permissionsState={effectivePermissions} permissions={DRAFT}
              data-testid="contract-edit-terms-save" onClick={() => void saveTermsEdit()}>保存租金条款</PermissionButton>
            <button type="button" onClick={() => setTermsEdit(null)}>取消</button>
          </div>
        ) : null}
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
                  {projects.some((x) => x.id !== user?.organizationId) ? <label>所属项目<select data-testid="contract-draft-project" aria-label="所属项目" value={draft.projectId} onChange={(e) => setDraft({ ...draft, projectId: e.target.value })}><option value="">本人所在项目</option>{projects.map((x) => <option key={x.id} value={x.id}>{x.name ?? x.id}</option>)}</select></label> : null}
                </>
              ),
            },
            {
              title: "租期与租金",
              description: "选择生效/到期日期，填写月租金（可空）。",
              validate: () => {
                if (!draft.effectiveDate) return "请选择生效日期。";
                if (!draft.expiryDate) return "请选择到期日期。";
                if (draft.rentBillingMode === "turnover_only" && !(Number(draft.depositYuan) > 0)) return "纯提成合同须填写合同约定押金（元），不得为零。";
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
                      {RENT_BILLING_MODES.map((m) => <option key={m.v} value={m.v}>{m.l}</option>)}
                    </select>
                  </label>
                  {draft.rentBillingMode !== "fixed_only" ? <small style={{ color: "var(--adm-text-3)" }}>提成规则（阶梯档位）在合同生效后于「审批与配置」页按合同配置。</small> : null}
                  <label>车位月租（元/月）<input aria-label="车位月租（元/月）" type="number" min="0" placeholder="未约定月租车位留空" value={draft.parkingMonthlyYuan} onChange={(e) => setDraft({ ...draft, parkingMonthlyYuan: e.target.value })} /></label>
                  {/* 第三轮裁决第 1 条：纯提成合同没有月租，押金按合同约定金额录入（不走月数公式），必填且不得为零。 */}
                  {draft.rentBillingMode === "turnover_only" ? (
                    <label>押金（元）<input aria-label="押金（元）" type="number" min="0" placeholder="合同约定押金（元）" value={draft.depositYuan} onChange={(e) => setDraft({ ...draft, depositYuan: e.target.value })} /></label>
                  ) : null}
                </>
              ),
            },
            {
              title: "结算方式",
              description: "选择抽成模式；公区及水电损包含物业费，独表按实际费用代收。",
              content: (
                <>
                  <label>抽成模式<select aria-label="抽成模式" value={draft.commissionMode} onChange={(e) => setDraft({ ...draft, commissionMode: e.target.value })}>
                    <option value="fixed_ratio">固定比例</option>
                    <option value="guarantee_or_commission_higher">保底与抽成取高</option>
                    <option value="tiered_ratio">阶梯比例</option>
                    <option value="guarantee_plus_commission">保底+抽成叠加</option>
                  </select></label>
                  <label>能源费分摊<select aria-label="能源费分摊" value={draft.energyFeeBasis} onChange={(e) => setDraft({ ...draft, energyFeeBasis: e.target.value })}>
                    <option value="by_contract">独表实际费用代收</option>
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
        <h3>审批流程模板（新建／到期终止／变更三类，按综合体可配；各模板的阶段与岗位可以不同）</h3>
        <Tabs data-testid="contract-flow-tabs" items={CONTRACT_TYPES.map((ct) => {
          const t = flow.find((x) => x.contractType === ct);
          return { key: ct, label: CONTRACT_TYPE_LABELS[ct], forceRender: true, children: (
            <div data-testid={`contract-flow-template-${ct}`}>
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid={`contract-flow-table-${ct}`}>
                  <thead><tr><th>模板名</th><th>平行审批线（必需）</th><th>默认最终审批人</th><th>主线层级数（最少～最多）</th></tr></thead>
                  <tbody>
                    {t ? (
                      <tr><td>{t.name}</td><td>{(t.requiredLineCodes ?? []).map((c) => LINE_LABELS[c] ?? c).join("、")}</td><td>{t.defaultFinalApproverId ?? "发起时指定"}</td>
                        <td>{t.minMainLevels != null ? `${t.minMainLevels}～${t.maxMainLevels}` : "未配置（不校验）"}</td></tr>
                    ) : (
                      <tr><td colSpan={4}>未配置（提交时按平台默认三线：业务/财务/法务）</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
              <div className="admin-inline-form">
                <PermissionButton permissionsState={effectivePermissions} permissions={FLOW_CFG}
                  data-testid={`contract-flow-reset-${ct}`} onClick={() => void resetFlowToDefault(ct)}>重置「{CONTRACT_TYPE_LABELS[ct]}」模板为出厂三线(业务/财务/法务)</PermissionButton>
              </div>
              <div className="admin-inline-form" data-testid={`contract-flow-levels-form-${ct}`}>
                <input aria-label={`${CONTRACT_TYPE_LABELS[ct]}-最少主线层级`} data-testid={`contract-flow-min-levels-${ct}`} type="number" min="1"
                  placeholder="最少主线层级（与最多同时填写）" value={levelsForm[ct]?.min ?? ""}
                  onChange={(e) => setLevelsForm({ ...levelsForm, [ct]: { min: e.target.value, max: levelsForm[ct]?.max ?? "" } })} />
                <input aria-label={`${CONTRACT_TYPE_LABELS[ct]}-最多主线层级`} data-testid={`contract-flow-max-levels-${ct}`} type="number" min="1"
                  placeholder="最多主线层级（都空＝不校验）" value={levelsForm[ct]?.max ?? ""}
                  onChange={(e) => setLevelsForm({ ...levelsForm, [ct]: { min: levelsForm[ct]?.min ?? "", max: e.target.value } })} />
                <PermissionButton permissionsState={effectivePermissions} permissions={FLOW_CFG}
                  data-testid={`contract-flow-save-levels-${ct}`} onClick={() => void saveFlowLevels(ct)}>保存主线层级数上下限</PermissionButton>
              </div>
            </div>
          ) };
        })} />

        <h3>提成租金规则（阶梯超额累进，挂合同）</h3>
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>按月销售额分档，每档只对落在本档区间的销售额用本档费率（像个税）；保底=提成下限（可空），无封顶。合同「收租方式」选纯提成/取高/叠加时按此规则出账。</p>
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }} data-testid="turnover-rule-draft-notice">
          提交后<strong>先存为草案，不会马上影响结算</strong>：须由另一位有结算配置管理权限的同事在「结算配置中心 → 待复核草案」里复核转正。初始规则按填写日期生效；已有规则变更统一按实际批准次月1日生效，批准当月仍按旧规则，手填起日不决定变更生效。起草人不能自己转正。
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

        <h3>押金收取月数（全平台统一；押金＝收取月数×签约月租，只乘月租）</h3>
        <p data-testid="contract-deposit-months-current">
          {depositMonths ? `当前 ${depositMonths.value} 个月；${depositMonths.minValue != null
            ? `上下限 ${depositMonths.minValue}～${depositMonths.maxValue} 个月`
            : "上下限未配置（首次配置须同时填写，下限不低于 3 个月）"}` : "-"}
        </p>
        <div className="admin-inline-form" data-testid="contract-deposit-months-form">
          <input aria-label="押金收取月数" data-testid="contract-deposit-months-value" type="number" min="3" placeholder="收取月数"
            value={depositForm.value} onChange={(e) => setDepositForm({ ...depositForm, value: e.target.value })} />
          <input aria-label="押金收取月数下限" data-testid="contract-deposit-months-min" type="number" min="3" placeholder="下限（不低于 3）"
            value={depositForm.min} onChange={(e) => setDepositForm({ ...depositForm, min: e.target.value })} />
          <input aria-label="押金收取月数上限" data-testid="contract-deposit-months-max" type="number" min="3" placeholder="上限"
            value={depositForm.max} onChange={(e) => setDepositForm({ ...depositForm, max: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={DEPOSIT_CFG}
            data-testid="contract-deposit-months-save" onClick={() => void saveDepositMonths()}>保存押金收取月数及上下限</PermissionButton>
        </div>

        <h3>押金退还等待期（按项目；自最后一个审批完成之日起算，算账晚于等待期完成的顺延至算账完成之日支付）</h3>
        <div className="admin-inline-form" data-testid="contract-refund-wait-form">
          <input aria-label="等待期项目 id" data-testid="contract-refund-wait-project" placeholder="综合体项目 id"
            value={refundWaitForm.projectId} onChange={(e) => setRefundWaitForm({ ...refundWaitForm, projectId: e.target.value })} />
          <button type="button" data-testid="contract-refund-wait-query" onClick={() => void queryRefundWait()}>查询等待期</button>
          <span className="admin-muted-action" data-testid="contract-refund-wait-current">
            {refundWait ? `当前 ${refundWait.value} 日（${refundWait.projectId === "__global__" ? "出厂值，本项目未配置" : "本项目配置"}）；${refundWait.minValue != null
              ? `上下限 ${refundWait.minValue}～${refundWait.maxValue} 日`
              : "上下限未配置（首次配置须同时填写）"}` : "未查询"}
          </span>
          <input aria-label="押金退还等待期日数" data-testid="contract-refund-wait-value" type="number" min="0" placeholder="等待期（日）"
            value={refundWaitForm.value} onChange={(e) => setRefundWaitForm({ ...refundWaitForm, value: e.target.value })} />
          <input aria-label="押金退还等待期下限" data-testid="contract-refund-wait-min" type="number" min="0" placeholder="下限（日）"
            value={refundWaitForm.min} onChange={(e) => setRefundWaitForm({ ...refundWaitForm, min: e.target.value })} />
          <input aria-label="押金退还等待期上限" data-testid="contract-refund-wait-max" type="number" min="0" placeholder="上限（日）"
            value={refundWaitForm.max} onChange={(e) => setRefundWaitForm({ ...refundWaitForm, max: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={REFUND_WAIT_CFG}
            data-testid="contract-refund-wait-save" onClick={() => void saveRefundWait()}>保存等待期及上下限</PermissionButton>
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
                  <td className="admin-id-cell" title={ch.id}>{shortId(ch.id)}</td><td>{formatBusinessStatus(ch.changeType)}</td>
                  <td onClick={(event) => event.stopPropagation()}>
                    {formatBusinessStatus(ch.approvalStatus)}
                    {ch.reviewId ? <> · <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-009" objectId={ch.reviewId}>审批链 {shortId(ch.reviewId)}</TraceLink></> : null}
                  </td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>{(() => {
                    const acts: RowAction[] = [];   // HP-C01：变更节点在「审批链」页裁决，此处无逐岗审批按钮
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
          {TERMINATION_CHANGES.includes(changeForm.changeType) ? (
            <label>退租日<input type="date" aria-label="退租日" data-testid="contract-change-move-out-date" required
              value={changeForm.moveOutDate} onChange={(e) => setChangeForm({ ...changeForm, moveOutDate: e.target.value })} /></label>
          ) : null}
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
              {selectedTurnoverOnly ? <span className="admin-muted">{TURNOVER_DEPOSIT_HINT}</span> : (
                <>
                  <input aria-label="变更后月租（元）" data-testid="contract-change-new-rent" inputMode="decimal"
                    placeholder="变更后月租（元；按每平米计租的合同不填，按新铺位面积重算）"
                    value={changeForm.newRentYuan} onChange={(e) => setChangeForm({ ...changeForm, newRentYuan: e.target.value })} />
                  <label>押金补退办理期限<input type="date" aria-label="押金补退办理期限" data-testid="contract-change-deposit-deadline" required
                    value={changeForm.depositDeadline} onChange={(e) => setChangeForm({ ...changeForm, depositDeadline: e.target.value })} /></label>
                  <span className="admin-muted">目标押金＝原押金月数×变更后月租，差额自动登记应收/应退</span>
                </>
              )}
              {changeForm.changeType === "location_change" ? (
                <input aria-label="新经营位置铺位" placeholder="新铺位ID（逗号分隔；不填=沿用原铺位）"
                  value={changeForm.spaceIds} onChange={(e) => setChangeForm({ ...changeForm, spaceIds: e.target.value })} />
              ) : null}
            </div>
          ) : null}
          {changeForm.changeType === "rent_change" && !selectedTurnoverOnly ? (
            <div data-testid="contract-rent-change-fields" style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", width: "100%", alignItems: "center" }}>
              {selectedPerSqm ? (
                <input aria-label="调整后每平米单价（元/㎡）" data-testid="contract-change-adjusted-per-sqm" inputMode="decimal"
                  placeholder="调整后每平米单价（元/㎡；月租按铺位面积计算）"
                  value={changeForm.newRentYuan} onChange={(e) => setChangeForm({ ...changeForm, newRentYuan: e.target.value })} />
              ) : (
                <input aria-label="调整后月租（元）" data-testid="contract-change-adjusted-rent" inputMode="decimal" placeholder="调整后月租（元）"
                  value={changeForm.newRentYuan} onChange={(e) => setChangeForm({ ...changeForm, newRentYuan: e.target.value })} />
              )}
              <span className="admin-muted">生效后剩余租期按新月租重排</span>
            </div>
          ) : null}
          {changeForm.changeType === "renewal" ? (
            <div data-testid="contract-renewal-fields" style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", width: "100%", alignItems: "center" }}>
              <span style={{ fontWeight: 600 }}>续租：同一合同延期（终审通过后延长到期日、换月租、补排延长期账单）</span>
              {selectedTurnoverOnly ? null : selectedPerSqm ? (
                <input aria-label="续租后每平米单价（元/㎡）" data-testid="contract-change-renewal-per-sqm" inputMode="decimal"
                  placeholder="续租后每平米单价（元/㎡；月租按铺位面积计算）"
                  value={changeForm.newRentYuan} onChange={(e) => setChangeForm({ ...changeForm, newRentYuan: e.target.value })} />
              ) : (
                <input aria-label="续租后月租（元）" data-testid="contract-change-renewal-rent" inputMode="decimal" placeholder="续租后月租（元）"
                  value={changeForm.newRentYuan} onChange={(e) => setChangeForm({ ...changeForm, newRentYuan: e.target.value })} />
              )}
              <label>续租后到期日<input type="date" aria-label="续租后到期日" data-testid="contract-change-renewal-expiry" required
                value={changeForm.newExpiryDate} onChange={(e) => setChangeForm({ ...changeForm, newExpiryDate: e.target.value })} /></label>
              {selectedTurnoverOnly ? <span className="admin-muted">{TURNOVER_DEPOSIT_HINT}</span> : (
                <>
                  <label>押金补退办理期限<input type="date" aria-label="押金补退办理期限" data-testid="contract-change-deposit-deadline" required
                    value={changeForm.depositDeadline} onChange={(e) => setChangeForm({ ...changeForm, depositDeadline: e.target.value })} /></label>
                  <span className="admin-muted">目标押金＝原押金月数×续租后月租，差额自动登记应收/应退</span>
                </>
              )}
            </div>
          ) : null}
          {approvalFields("change", "contract-change")}
          <PermissionButton permissionsState={effectivePermissions} permissions={CHANGE_MANAGE}
            data-testid="contract-change-raise" onClick={() => void raiseChange()}>发起变更</PermissionButton>
        </div>

        <h3>超范围下单经营申请（#30：下单被经营范围拦下后可申请，按「变更」类模板走统一审批，通过则放行）</h3>
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
          {approvalFields("change", "contract-scope-request")}
          <PermissionButton permissionsState={effectivePermissions} permissions={["contract.scope_request.raise"]}
            data-testid="contract-scope-request-raise" onClick={() => void requestScopeGrant()}>提交超范围申请</PermissionButton>
        </div>

        <h3>退租/到期结算（退租/提前终止：齐单核对 → 变更终审通过自动落账；到期：开账即发起到期终止审批 → 齐单核对 → 终审通过自动落账 / 有争议挂起）</h3>
        <div className="admin-inline-form" data-testid="contract-dispute-form">
          <label>结算类型<select aria-label="结算类型" data-testid="contract-settle-trigger" value={settleTrigger} onChange={(e) => setSettleTrigger(e.target.value)}>
            <option value="move_out">退租</option>
            <option value="expiry">到期</option>
            <option value="early_termination">提前终止</option>
          </select></label>
          <input aria-label="争议原因" placeholder="争议原因（退租开账时填则挂起『有争议』）" value={disputeReason} onChange={(e) => setDisputeReason(e.target.value)} />
          {approvalFields("expiry_termination", "contract-settle")}
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="contract-settlements-table">
            <thead><tr><th>结算单号</th><th>触发</th><th>状态</th><th>异议理由</th><th>处理结论</th><th className="admin-op-col">操作</th><th>详情</th></tr></thead>
            <tbody>
              {settlements.map((st) => (
                <tr key={st.id} data-testid={`contract-settlement-${st.id}`} className="admin-row-clickable" tabIndex={0}
                  onClick={() => openSettlementDetail(st)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(st); } }}>
                  <td className="admin-id-cell" title={st.id}>{shortId(st.id)}</td><td>{formatBusinessStatus(st.triggerType)}</td><td>{formatBusinessStatus(st.status)}</td>
                  <td style={{ whiteSpace: "pre-line" }}>{st.disputeReason ?? "-"}</td>
                  <td style={{ whiteSpace: "pre-line" }}>{st.disputeResolution ?? "-"}</td>
                  <td className="admin-op-col" onClick={(event) => event.stopPropagation()}>
                    {(() => {
                      const acts: RowAction[] = [];
                      if (st.status === "disputed") acts.push({ key: "resolve", label: "裁决→待结算", permissions: DISPUTE_RESOLVE, onClick: () => void resolveSettlementDispute(st.id, "主管裁决结论（必填）："), testId: `settlement-resolve-${st.id}` });
                      if (st.status === "disputed_litigation") acts.push({ key: "litigation", label: "录入诉讼/仲裁结果", permissions: DISPUTE_RESOLVE, onClick: () => void resolveSettlementDispute(st.id, "诉讼/仲裁结果（必填）："), testId: `settlement-litigation-${st.id}` });
                      const open = OPEN_SETTLEMENT.includes(st.status);
                      if (open) {
                        acts.push({ key: "docs-check", label: "齐单核对完成", permissions: SETTLE, onClick: () => void checkSettlementDocuments(st), testId: `settlement-docs-check-${st.id}` });
                      }
                      return (<>
                        <RowActions actions={acts} permissionsState={effectivePermissions} testId={`settlement-actions-${st.id}`} />
                        {open ? <span className="admin-muted" data-testid={`settlement-auto-post-${st.id}`}>{st.triggerType === "expiry" ? "由到期终止审批终审自动落账" : "由退租审批终审自动落账"}</span> : null}
                      </>);
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
