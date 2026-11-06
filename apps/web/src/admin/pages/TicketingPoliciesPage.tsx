// 文件版本：V0.06
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { CnDatePicker } from "../components/CnDatePicker";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { Tabs } from "antd";
import { RowActions, type RowAction } from "../components/RowActions";
import { BarArrivalReminderSection, TicketExpiryReminderSection } from "./CustomerFacingSettingsSections";

// 3.10 票务策略配置化：票种/通票、权益模板、票种↔权益组装（通票）、派生价规则全部后台可配，
// 替代旧骨架页（名称写死、派生规则只是文案）。请求字段对齐后端真实契约（snake_case）。
// 组合式配置策略（用户 2026-07-06 拍板）：一张通票 = 组合多个权益绑定（跨酒吧/游乐/餐饮），
// 前端一次性编排既有端点完成：createEntitlementTemplate → bindTicketTypeEntitlement，
// 游乐额外 POST /amusement/projects/{id}/entitlements（收紧核销绑定）。零新增后端端点/迁移。
// 餐饮结算（用户 2026-07-06 拍板）：餐饮权益 entitlement_type='dining'，钱归餐厅商户——核销按【内部结算价】
//   (agreement_price_cent) 全额直付餐厅(exclusive_merchant_id，走 §6.4 不分摊)，价格可 0、折扣仅售卖口径不影响结算。
//   不再把折扣率/面值当结算价乱记（此前 discount_rate/dining_exchange 会被通用路当面值记账，故 #16 曾移除）。
type TicketTypeRow = {
  id: string;
  ticketName: string;
  weekdayPriceCent: number | null;
  weekendPriceCent: number | null;
  holidayPriceCent: number | null;
  status: string;
  stockQuantity: number | null;
  // V260 存档/删除范式：archivedAt 非空=已存档；deleteStatus none|pending|approved。
  archivedAt?: string | null;
  deleteStatus?: string;
  // AUD-004/022：有效期/可退/单店酒吧在列表与详情贯通展示。
  validityDays?: number | null;
  refundable?: boolean;
  barId?: string | null;
};
type EntitlementTemplateRow = {
  id: string;
  name: string;
  entitlementType: string;
  status: string;
  // 配置中心批5：签约价（=内部结算价）与结算类别贯通到列表/详情，改价前运营先看得见现值。
  entitlementKind?: string | null;
  agreementPriceCent?: number | null;
};
type PriceRuleRow = {
  id: string;
  ticketTypeId: string;
  variantType: string;
  priceMode: string;
  discountRate: number | null;
  weekdayPriceCent: number | null;
  status: string;
  archivedAt?: string | null;
  deleteStatus?: string;
};
type MerchantRow = { id: string; merchantName: string; merchantType: string | null };
type AmusementProjectRow = { id: string; projectName: string };

// V230：票种资格/定价档分类的中文名（全终端零英文）。
const TICKET_CATEGORY_LABELS: Record<string, string> = { standard: "标准票", new_person: "新人票", student: "学生票" };

const MANAGE = ["ticket.product.manage"];
// #172(2026-07-19 拍板)：全场统一动态码刷新间隔——5 秒为单位、最长 60 秒。
const QR_REFRESH_OPTIONS = [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60];
const VARIANT_TYPES = ["new_customer", "student", "group", "channel", "member"];
const VARIANT_LABELS: Record<string, string> = {
  new_customer: "新人票",
  student: "学生票",
  group: "多人票",
  channel: "渠道价",
  member: "会员价",
};
// 第十八批 W 段 177-R1（控制席并入）：通用「新建模板」不含赠饮——赠饮须选赠送商品，在「组合式套票配置」酒吧行建（该处可选任一票种含单店票、选该吧商品与成本承担方）。
const ENTITLEMENT_TYPES = ["entry", "ride", "bar", "amusement"];

// 组合式配置策略：酒吧权益种类（对应 entitlement_type）。gift_drink=酒水兑换，admission=入场资格。
const BAR_ENTITLEMENT_KINDS = [
  { value: "gift_drink", label: "酒水兑换" },
  { value: "admission", label: "入场资格" },
];

// V376（2026-08-01 拍板）：赠送权益的成本承担方必选——漏标后端拒保存/拒发布，前端先拦。
// 平台出资型=平台按内部结算价与提供商户结算（计入赠送权益预估成本）；商户让利型=平台不承担成本、不与商户结算。
const COST_BEARER_OPTIONS = [
  { value: "platform_funded", label: "平台出资型（平台按内部结算价与商户结算）" },
  { value: "merchant_concession", label: "商户让利型（平台不结算）" },
];

// 组合表单里的酒吧权益行。
// barItemRef：酒水兑换必填——指向该酒吧的商品，成本按这件商品的内部结算价与这家酒吧结算。
// costBearer：酒水兑换为赠送（票面价 0）时必选——这杯酒的成本谁出（V376）。
type BarLine = { merchantId: string; kind: string; totalTimes: string; valueYuan: string; barItemRef: string; costBearer: string };
// 提供方商品（下拉数据源：该酒吧自己的商品）。
type BarProductRow = { id: string; barId: string; productName: string };
// 票面赠酒清单行（按酒吧分组，售票时写明票面）。
type GiftCoverageRow = {
  barId: string;
  barName: string;
  giftName: string;
  quantity: number;
  cups: number;
  barTotalCups: number;
};
// 餐饮权益行（用户2026-07-06 拍板）：选餐厅商户 + 价格(可0) + 折扣 + 内部结算价。
//   结算按【内部结算价】全额归餐厅商户(exclusive_merchant_id + agreement_price_cent，走 §6.4 不分摊直付)；
//   价格(面值,可0)与折扣仅为售卖/展示口径，不参与结算(核销结算价=内部结算价，不再把折扣当面值乱记)。
// costBearer：价格 0（纯赠送）时必选——这份赠品的成本谁出（V376）。
type DiningLine = { merchantId: string; valueYuan: string; discount: string; catalogCode: string; costBearer: string; totalTimes: string };

function toRows<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  const items = (data as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

function yuan(cent: number | null): string {
  return cent == null ? "—" : (cent / 100).toFixed(2);
}

// V369 票种定价核算（后端 GET /ticketing/ticket-types/{id}/price-check，adminClient 已 snake→camel）。
type PriceCheckRow = {
  ticketTypeId: string;
  componentTotalCent: number;
  comboDiscountRate: number | string;
  computedPriceCent: number;
  filledPriceCent: number | null;
  diffCent: number | null;
  toleranceCent: number;
  withinTolerance: boolean;
  roundingSuggested: number;
};

export function TicketingPoliciesPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId, navigateTo, reportViewedObjects }: AdminPageProps) {
  const [tickets, setTickets] = React.useState<TicketTypeRow[]>([]);
  const [templates, setTemplates] = React.useState<EntitlementTemplateRow[]>([]);
  const [rules, setRules] = React.useState<PriceRuleRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("ticket_type", tickets), [tickets, reportViewedObjects]);
  React.useEffect(() => reportViewedObjects?.("ticket_price_rule", rules), [rules, reportViewedObjects]);
  const [merchants, setMerchants] = React.useState<MerchantRow[]>([]);
  const [projects, setProjects] = React.useState<AmusementProjectRow[]>([]);
  // V260 存档/删除范式：显示已存档票种/价规开关（后端按 includeArchived 过滤）。
  const [showArchived, setShowArchived] = React.useState(false);

  // 区1 新建票种（保留既有 e2e 锚点：三档价 + ticket-publish + ticket-policy-error）
  const [ticketName, setTicketName] = React.useState("后台通票策略");
  const [weekday, setWeekday] = React.useState("");
  const [weekend, setWeekend] = React.useState("");
  const [holiday, setHoliday] = React.useState("");
  const [stock, setStock] = React.useState("");
  // AUD-004/022：有效期天数/动态码刷新/可退票/单店酒吧 建票直配。
  const [validityDays, setValidityDays] = React.useState("1");
  // #172：全场统一动态码刷新间隔（秒）——页级配置，不再按票种各配一套。
  const [qrRefresh, setQrRefresh] = React.useState<number>(15);
  const [refundable, setRefundable] = React.useState(false);
  // 结算重构批4（V230）：票种资格/定价档分类（标准/新人/学生）。仅分类与报表口径，不碰定价。
  const [ticketCategory, setTicketCategory] = React.useState("standard");
  const [barId, setBarId] = React.useState("");
  // AUD-015：建票=草稿→绑权益→带理由发布，一个向导内完成；createKey 固定一次，失败重试幂等复用同一草稿。
  const [createTemplateId, setCreateTemplateId] = React.useState("");
  // V369 建票时的票种折扣（百分数，100=不打折）。系统按「各业态票面原价合计 × 折扣」算出售价。
  const [createDiscount, setCreateDiscount] = React.useState("100");
  // V369 组合配置页的定价核算：系统算出售价 vs 运营填的售价，差多少、超没超。
  const [priceCheck, setPriceCheck] = React.useState<PriceCheckRow | null>(null);
  const [pricingDiscount, setPricingDiscount] = React.useState("100");
  const [pricingWeekday, setPricingWeekday] = React.useState("");
  const [pricingWeekend, setPricingWeekend] = React.useState("");
  const [pricingHoliday, setPricingHoliday] = React.useState("");
  const [pricingReason, setPricingReason] = React.useState("");
  const [pricingError, setPricingError] = React.useState("");
  const [pricingBusy, setPricingBusy] = React.useState(false);
  const [publishReason, setPublishReason] = React.useState("");
  const [createKey, setCreateKey] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  // 引导式流程 phase-2：新建票种收起为主按钮，点击弹出分步引导（GuidedFlowModal）。
  const [createOpen, setCreateOpen] = React.useState(false);

  // 区2 新建权益模板
  // 第十八批 W 段 177-R1（BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」）：提供方商户必选。
  const [tplForm, setTplForm] = React.useState({ name: "", entitlementType: "entry", totalTimes: "1", valueYuan: "", merchantId: "" });
  // 配置中心批5：改虚拟权益签约价（=内部结算价）。资金口径，理由必填。
  // 区3 通票组装（票种绑权益）
  const [bindForm, setBindForm] = React.useState({ ticketTypeId: "", templateId: "", quantity: "1", totalTimes: "1", numPeople: "1", componentPriceYuan: "", costBearer: "" });
  // 区4 派生价规则
  const [ruleForm, setRuleForm] = React.useState({ ticketTypeId: "", variantType: "new_customer", priceMode: "derive_from_calendar_price", discount: "0.8", fixedYuan: "" });
  // 批3 第三方券:把权益模板配成第三方券 + 对账文件核销回流导入。
  const [pvConfig, setPvConfig] = React.useState({ templateId: "", settlementMode: "free_marketing", partnerKind: "merchant", partnerId: "", internalYuan: "" });
  const [pvRedeem, setPvRedeem] = React.useState({ code: "", redeemedDate: "", externalRef: "" });
  const submitPvConfig = async () => {
    if (!pvConfig.templateId || !pvConfig.partnerId.trim()) { setGlobalError("第三方券配置需选权益模板并填第三方"); return; }
    if (pvConfig.settlementMode === "internal_purchased" && !pvConfig.internalYuan.trim()) { setGlobalError("内部价采购券需填内部结算价"); return; }
    try {
      await api.post("/api/v1/ticketing/partner-vouchers/configs", {
        entitlementTemplateId: pvConfig.templateId,
        settlementMode: pvConfig.settlementMode,
        partnerKind: pvConfig.partnerKind,
        partnerId: pvConfig.partnerId.trim(),
        internalPriceCent: pvConfig.settlementMode === "internal_purchased" ? Math.round(Number(pvConfig.internalYuan) * 100) : null,
      });
      setActionResult({ action: "partner_voucher_config_created", templateId: pvConfig.templateId });
    } catch (e) { setGlobalError(e instanceof Error ? e.message : String(e)); }
  };
  const submitPvRedeem = async () => {
    if (!pvRedeem.code.trim() || !pvRedeem.externalRef.trim()) { setGlobalError("核销回流需填券码和外部核销单号"); return; }
    try {
      await api.post("/api/v1/ticketing/partner-vouchers/redemptions", {
        entitlementCode: pvRedeem.code.trim(),
        redeemedAt: pvRedeem.redeemedDate ? `${pvRedeem.redeemedDate}T00:00:00Z` : null,
        externalRedemptionRef: pvRedeem.externalRef.trim(),
        source: "reconciliation_file",
      });
      setActionResult({ action: "partner_voucher_redeemed", code: pvRedeem.code.trim() });
      setPvRedeem({ code: "", redeemedDate: "", externalRef: "" });
    } catch (e) { setGlobalError(e instanceof Error ? e.message : String(e)); }
  };

  // 组合式配置策略（新）：选一张通票 → 多选酒吧（每个配权益：兑换/次数/面值）+ 多选游乐设施 → 一次性组合创建。
  const [comboTicketId, setComboTicketId] = React.useState("");
  const [barLines, setBarLines] = React.useState<BarLine[]>([]);
  // 各酒吧的商品（按商户 id 缓存，选中酒吧时按需拉一次）。
  const [barProducts, setBarProducts] = React.useState<Record<string, BarProductRow[]>>({});
  // 票面赠酒清单（各酒吧分别赠几杯）。
  const [giftCoverage, setGiftCoverage] = React.useState<GiftCoverageRow[]>([]);
  const [diningLines, setDiningLines] = React.useState<DiningLine[]>([]);
  const [selectedProjects, setSelectedProjects] = React.useState<Record<string, boolean>>({});
  const [projectTimes, setProjectTimes] = React.useState("1");
  // 游乐权益也必须归属商户（后端 createEntitlementTemplate 强制 merchant_id 非空，否则结算成孤儿）。
  // /amusement/projects 不返回项目所属商户（merchantId 被 @JsonIgnore），故此处让运营选一个归属商户，全部游乐权益用它。
  const [amusementMerchantId, setAmusementMerchantId] = React.useState("");
  // S11：游乐权益的票面原价（元）。整票的钱按各业态票面原价占比拆，游乐没填价就分不到钱。
  const [amusementPriceYuan, setAmusementPriceYuan] = React.useState("");
  const [comboBusy, setComboBusy] = React.useState(false);
  const [comboError, setComboError] = React.useState("");

  // 行明细抽屉：三张业务表（票种/权益模板/派生价规则）整行可点 → 该行完整字段（金额¥·枚举中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openTicketTypeDetail(row: TicketTypeRow) {
    setDetail({
      title: `票种明细 · ${row.ticketName}`,
      fields: [
        { label: "票种名称", value: row.ticketName },
        { label: "平日价", value: `¥${yuan(row.weekdayPriceCent)}` },
        { label: "周末价", value: `¥${yuan(row.weekendPriceCent)}` },
        { label: "节假日价", value: `¥${yuan(row.holidayPriceCent)}` },
        { label: "限量", value: row.stockQuantity ?? "不限" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "票种编号", value: row.id },
      ],
    });
  }

  function openTemplateDetail(row: EntitlementTemplateRow) {
    setDetail({
      title: `权益模板明细 · ${row.name}`,
      fields: [
        { label: "模板名称", value: row.name },
        { label: "权益类型", value: formatBusinessStatus(row.entitlementType) },
        { label: "签约价", value: row.agreementPriceCent != null ? `¥${yuan(row.agreementPriceCent)}` : "未设（按面值结算）" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "模板编号", value: row.id },
      ],
    });
  }

  function openRuleDetail(row: PriceRuleRow) {
    setDetail({
      title: `派生价规则明细 · ${VARIANT_LABELS[row.variantType] ?? row.variantType}`,
      fields: [
        { label: "票种", value: <TraceLink navigateTo={navigateTo} pageId="ADM-TKT-001" objectId={row.ticketTypeId}>{tickets.find((t) => t.id === row.ticketTypeId)?.ticketName ?? row.ticketTypeId}</TraceLink> },
        { label: "人群/渠道", value: VARIANT_LABELS[row.variantType] ?? row.variantType },
        { label: "定价方式", value: row.priceMode === "fixed_price" ? "固定价" : "按基础价折扣" },
        { label: "折扣/价格", value: row.priceMode === "fixed_price" ? `¥${yuan(row.weekdayPriceCent)}` : row.discountRate != null ? `${Math.round(Number(row.discountRate) * 100)}%` : "—" },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "规则编号", value: row.id },
      ],
    });
  }

  const { loading, error: loadError, reload } = useAdminResource(
    async () => {
      const [ticketRows, templateRows, ruleRows, merchantRows, projectRows, qrConfig] = await Promise.all([
        api.get<unknown>(`/api/v1/ticketing/ticket-types?includeArchived=${showArchived}`),
        api.get<unknown>("/api/v1/ticketing/entitlement-templates"),
        api.get<unknown>(`/api/v1/ticketing/price-rules?includeArchived=${showArchived}`),
        // 数据源（组合式配置多选）：商户（酒吧）+ 游乐设施。读失败不阻断本页主体，降级为空列表。
        api.get<unknown>("/api/v1/platform/merchants").catch(() => ({ items: [] })),
        api.get<unknown>("/api/v1/amusement/projects").catch(() => ({ items: [] })),
        // #172：全场统一动态码刷新间隔（读失败不阻断本页，回退默认 15）。
        api.get<{ refreshIntervalSeconds?: number }>("/api/v1/ticketing/qr-refresh-config").catch(() => ({})),
      ]);
      setTickets(toRows<TicketTypeRow>(ticketRows));
      setTemplates(toRows<EntitlementTemplateRow>(templateRows));
      setRules(toRows<PriceRuleRow>(ruleRows));
      setMerchants(toRows<MerchantRow>(merchantRows));
      setProjects(toRows<AmusementProjectRow>(projectRows));
      setQrRefresh(Number((qrConfig as { refreshIntervalSeconds?: number })?.refreshIntervalSeconds) || 15);
    },
    { onError: setGlobalError, fallbackMessage: "票务策略配置读取失败" },
  );

  // 切换「显示已存档」后重新拉取（后端按 includeArchived 过滤），首次挂载不触发。
  const firstArchivedToggle = React.useRef(true);
  React.useEffect(() => {
    if (firstArchivedToggle.current) {
      firstArchivedToggle.current = false;
      return;
    }
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showArchived]);

  async function archiveCatalog(kind: "ticket-types" | "price-rules", id: string, archive: boolean) {
    setGlobalError(null);
    let reason: string | undefined;
    if (archive) {
      const r = window.prompt("存档（下线）理由：");
      if (r === null) return;
      if (!r.trim()) { setGlobalError("存档理由必填"); return; }
      reason = r.trim();
    }
    await api.post(`/api/v1/ticketing/${kind}/${id}/${archive ? "archive" : "unarchive"}`, {
      idempotencyKey: `${kind}-${archive ? "archive" : "unarchive"}-${id}-${Date.now()}`,
      ...(reason ? { reason } : {}),
    });
    setActionResult({ action: `${kind === "ticket-types" ? "ticket_type" : "price_rule"}_${archive ? "archived" : "unarchived"}`, id });
    await reload();
  }

  async function requestDeleteCatalog(kind: "ticket-types" | "price-rules", id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null) {
      return; // 用户点「取消」= 中止申请，不提交空理由。
    }
    setGlobalError(null);
    await api.post(`/api/v1/ticketing/${kind}/${id}/delete-request`, {
      idempotencyKey: `${kind}-delete-request-${id}-${Date.now()}`,
      reason,
    });
    setActionResult({ action: `${kind === "ticket-types" ? "ticket_type" : "price_rule"}_delete_requested`, id });
    await reload();
  }

  async function approveDeleteCatalog(kind: "ticket-types" | "price-rules", id: string, decision: "approved" | "rejected") {
    setGlobalError(null);
    const decided = await api.post<{ deleted?: boolean }>(`/api/v1/ticketing/${kind}/${id}/delete-approve`, {
      idempotencyKey: `${kind}-delete-approve-${id}-${decision}-${Date.now()}`,
      decision,
    });
    setActionResult({ action: `${kind === "ticket-types" ? "ticket_type" : "price_rule"}_delete_${decision}`, id, deleted: decided?.deleted ?? false });
    await reload();
  }

  function archiveLabel(row: { archivedAt?: string | null; deleteStatus?: string }): string {
    if (row.deleteStatus === "pending") return "待审批删除";
    if (row.archivedAt) return "已存档";
    return "生效中";
  }

  function catalogActions(kind: "ticket-types" | "price-rules", row: { id: string; archivedAt?: string | null; deleteStatus?: string }) {
    const manage = kind === "ticket-types" ? MANAGE : ["ticket.price_variant.manage"];
    const approve = kind === "ticket-types" ? ["ticket.type.approve"] : ["ticket.price_rule.approve"];
    const prefix = kind === "ticket-types" ? "ticket-type" : "price-rule";
    const actions: RowAction[] = row.deleteStatus === "pending"
      ? [
          { key: "approve", label: "批准删除", permissions: approve, onClick: () => void approveDeleteCatalog(kind, row.id, "approved"), testId: `${prefix}-delete-approve-${row.id}` },
          { key: "reject", label: "驳回", permissions: approve, danger: true, onClick: () => void approveDeleteCatalog(kind, row.id, "rejected"), testId: `${prefix}-delete-reject-${row.id}` },
        ]
      : row.archivedAt
      ? [
          { key: "unarchive", label: "取消存档", permissions: manage, onClick: () => void archiveCatalog(kind, row.id, false), testId: `${prefix}-unarchive-${row.id}` },
          { key: "delete-request", label: "申请删除", permissions: manage, danger: true, onClick: () => void requestDeleteCatalog(kind, row.id), testId: `${prefix}-delete-request-${row.id}` },
        ]
      : [
          { key: "archive", label: "存档", permissions: manage, onClick: () => void archiveCatalog(kind, row.id, true), testId: `${prefix}-archive-${row.id}` },
        ];
    return <RowActions actions={actions} permissionsState={effectivePermissions} testId={`${prefix}-actions-${row.id}`} />;
  }

  async function publish() {
    if (!weekday || !weekend || !holiday) {
      setError("平日、周末、节假日基础价必须全部填写，缺少节假日价不可发布。");
      return;
    }
    const prices = { 平日价: Number(weekday), 周末价: Number(weekend), 节假日价: Number(holiday) };
    const invalid = Object.entries(prices).find(([, value]) => !Number.isFinite(value) || value <= 0);
    if (invalid) {
      setError(`${invalid[0]}必须是大于 0 的数字。`);
      return;
    }
    if (!ticketName.trim()) {
      setError("票种名称必填。");
      return;
    }
    if (!createTemplateId) {
      setError("必须绑定一个权益模板——公开可售的票必须能出票。");
      return;
    }
    if (!publishReason.trim()) {
      setError("发布理由必填。");
      return;
    }
    setError("");
    setBusy(true);
    try {
      // AUD-015：草稿建票 → 绑权益 → 带理由发布（服务端发布闸校验≥1个绑定）。createKey 全程复用，失败重试不产生重复草稿。
      const ticket = await api.post<{ id: string }>("/api/v1/ticketing/ticket-types", {
        idempotency_key: createKey,
        ticket_name: ticketName.trim(),
        base_calendar_price_mode: "weekday_weekend_holiday",
        weekday_price_cent: Math.round(prices.平日价 * 100),
        weekend_price_cent: Math.round(prices.周末价 * 100),
        holiday_price_cent: Math.round(prices.节假日价 * 100),
        stock_quantity: stock.trim() ? Number(stock) : null,
        // V369 票种折扣：系统按「各业态票面原价合计 × 折扣」算出售价，运营填的价与它差太多不许开卖。
        combo_discount_rate: Number(createDiscount || "100"),
        validity_days: Number(validityDays) || 1,
        refundable,
        bar_id: barId || null,
        // V230：资格/定价档分类（新人票/学生票等）。仅分类与报表用，价照样在本页三档价与派生价规则里配。
        ticket_category: ticketCategory,
      });
      const ticketId = ticket.id ?? "ticket-1";
      await api.post(`/api/v1/ticketing/ticket-types/${ticketId}/entitlements`, {
        idempotency_key: `${createKey}-bind-${createTemplateId}`,
        entitlement_template_id: createTemplateId,
        quantity: 1,
        total_times: 1,
        num_people: 1,
        // S11 票面原价：这张票只有这一条权益，它的票面原价就是这张票的平日价（占比 100%，钱全归这条权益的业态）。
        component_price_cent: Math.round(prices.平日价 * 100),
      });
      const published = await api.post(`/api/v1/ticketing/ticket-types/${ticketId}/publish`, {
        idempotency_key: `${createKey}-publish`,
        reason: publishReason.trim(),
      });
      setActionResult({ action: "ticket_policy_publish", status: "published", published });
      setCreateOpen(false);
      reload();
    } catch (publishError) {
      setError(publishError instanceof Error ? publishError.message : "发布失败");
    } finally {
      setBusy(false);
    }
  }

  async function createTemplate() {
    if (!tplForm.name.trim()) {
      setGlobalError("权益模板名称必填");
      return;
    }
    if (!tplForm.merchantId) {
      setGlobalError("请选择权益的提供方商户");
      return;
    }
    try {
      await api.post("/api/v1/ticketing/entitlement-templates", {
        idempotency_key: crypto.randomUUID(),
        name: tplForm.name.trim(),
        entitlement_type: tplForm.entitlementType,
        default_total_times: Number(tplForm.totalTimes) || 1,
        default_value_cent: tplForm.valueYuan.trim() ? Math.round(Number(tplForm.valueYuan) * 100) : null,
        status: "active",
        merchant_id: tplForm.merchantId,
      });
      setActionResult({ action: "entitlement_template_created", name: tplForm.name.trim() });
      setTplForm({ name: "", entitlementType: "entry", totalTimes: "1", valueYuan: "", merchantId: "" });
      reload();
    } catch (e) { setGlobalError(e instanceof Error ? e.message : "新建权益模板失败"); }
  }

  async function bindEntitlement() {
    if (!bindForm.ticketTypeId || !bindForm.templateId) {
      setGlobalError("请选择票种与权益模板");
      return;
    }
    // V376 前置拦：赠送权益（票面原价 0/未填 且 赠饮/实物类模板）必须标成本承担方，漏标后端也会拒。
    //   模板列表拿不到「专属商户」信息的（如餐饮专属直付类），由后端兜底点名报错。
    const boundTemplate = templates.find((t) => t.id === bindForm.templateId);
    const looksLikeGift = boundTemplate
      && (boundTemplate.entitlementType === "gift_drink" || boundTemplate.entitlementKind === "physical")
      && !(Number(bindForm.componentPriceYuan) > 0);
    if (looksLikeGift && !bindForm.costBearer) {
      setGlobalError(`「${boundTemplate.name}」是赠送权益（各业态价格 0），请选择成本承担方（商户让利型/平台出资型）。`);
      return;
    }
    await api.post(`/api/v1/ticketing/ticket-types/${bindForm.ticketTypeId}/entitlements`, {
      idempotency_key: crypto.randomUUID(),
      entitlement_template_id: bindForm.templateId,
      quantity: Number(bindForm.quantity) || 1,
      total_times: Number(bindForm.totalTimes) || 1,
      num_people: Number(bindForm.numPeople) || 1,
      // S11：票面原价（元→分）。这份权益按票面值多少钱，决定客人这笔钱有多少分给它所属的业态。
      //   免费的填 0（0=免费=不分钱）；留空=还没定价，票种发布时会被拦下不许开卖。
      component_price_cent: bindForm.componentPriceYuan.trim() === ""
        ? null
        : Math.round(Number(bindForm.componentPriceYuan) * 100),
      // V376：成本承担方（赠送权益必选；非赠送类可空）。
      cost_bearer: bindForm.costBearer || null,
    });
    setActionResult({ action: "ticket_entitlement_bound", ticketTypeId: bindForm.ticketTypeId });
    reload();
  }

  async function createRule() {
    if (!ruleForm.ticketTypeId) {
      setGlobalError("请选择派生价所属票种");
      return;
    }
    const fixed = ruleForm.priceMode === "fixed_price";
    if (fixed && !ruleForm.fixedYuan.trim()) {
      setGlobalError("固定价模式必须填价格（元）");
      return;
    }
    await api.post("/api/v1/ticketing/price-rules", {
      idempotency_key: crypto.randomUUID(),
      ticket_type_id: ruleForm.ticketTypeId,
      variant_type: ruleForm.variantType,
      price_mode: ruleForm.priceMode,
      discount_rate: fixed ? null : Number(ruleForm.discount) || null,
      weekday_price_cent: fixed ? Math.round(Number(ruleForm.fixedYuan) * 100) : null,
      weekend_price_cent: fixed ? Math.round(Number(ruleForm.fixedYuan) * 100) : null,
      holiday_price_cent: fixed ? Math.round(Number(ruleForm.fixedYuan) * 100) : null,
    });
    setActionResult({ action: "ticket_price_rule_created", variantType: ruleForm.variantType });
    reload();
  }

  // ---- 组合式配置策略编排 ----
  function merchantName(id: string): string {
    return merchants.find((m) => m.id === id)?.merchantName ?? id;
  }
  // 选中酒吧后按需拉该吧商品（一家拉一次；读失败降级为空，不阻断本页）。
  const loadBarProducts = React.useCallback(async (merchantId: string) => {
    if (!merchantId || barProducts[merchantId]) return;
    const rows = await api
      .get<unknown>(`/api/v1/bar/catalog/products?bar_id=${encodeURIComponent(merchantId)}`)
      .catch(() => []);
    setBarProducts((prev) => ({ ...prev, [merchantId]: toRows<BarProductRow>(rows) }));
  }, [api, barProducts]);

  // 票面赠酒清单：选中通票后拉一次，配置完再刷新。
  const loadGiftCoverage = React.useCallback(async (ticketTypeId: string) => {
    if (!ticketTypeId) { setGiftCoverage([]); return; }
    const rows = await api
      .get<unknown>(`/api/v1/ticketing/ticket-types/${encodeURIComponent(ticketTypeId)}/gift-coverage`)
      .catch(() => ({ items: [] }));
    setGiftCoverage(toRows<GiftCoverageRow>(rows));
  }, [api]);

  // V369 定价核算：选中通票后拉一次，配完权益/改完价再刷新。运营看到的和发布闸判的是同一份计算。
  const loadPriceCheck = React.useCallback(async (ticketTypeId: string) => {
    if (!ticketTypeId) { setPriceCheck(null); return; }
    const row = await api
      .get<PriceCheckRow>(`/api/v1/ticketing/ticket-types/${encodeURIComponent(ticketTypeId)}/price-check`)
      .catch(() => null);
    setPriceCheck(row);
    if (row) {
      setPricingDiscount(String(row.comboDiscountRate ?? 100));
      setPricingWeekday(row.filledPriceCent == null ? "" : (row.filledPriceCent / 100).toFixed(2));
    }
  }, [api]);

  // 运营边填边看：填的这个数离系统算出的售价差多少、超没超允许误差（提交前就知道，不用等报错）。
  const pricingLiveDiffCent = React.useMemo(() => {
    if (!priceCheck || pricingWeekday.trim() === "") return null;
    const filled = Math.round(Number(pricingWeekday) * 100);
    if (!Number.isFinite(filled)) return null;
    const rate = Number(pricingDiscount);
    const computed = Number.isFinite(rate)
      ? Math.round(priceCheck.componentTotalCent * rate / 100)
      : priceCheck.computedPriceCent;
    return { filled, computed, diff: filled - computed };
  }, [priceCheck, pricingWeekday, pricingDiscount]);

  // V369 改售价／折扣：保存当场比对允许误差，超出后端直接拒（前端提示与后端同一口径）。
  async function savePricing() {
    if (!comboTicketId) { setPricingError("请先选择要配置的通票。"); return; }
    if (!pricingReason.trim()) { setPricingError("改价理由必填（事后要能查是谁为什么改成这个数）。"); return; }
    setPricingError("");
    setPricingBusy(true);
    try {
      await api.put(`/api/v1/ticketing/ticket-types/${encodeURIComponent(comboTicketId)}/pricing`, {
        idempotency_key: crypto.randomUUID(),
        reason: pricingReason.trim(),
        combo_discount_rate: Number(pricingDiscount || "100"),
        weekday_price_cent: pricingWeekday.trim() ? Math.round(Number(pricingWeekday) * 100) : null,
        weekend_price_cent: pricingWeekend.trim() ? Math.round(Number(pricingWeekend) * 100) : null,
        holiday_price_cent: pricingHoliday.trim() ? Math.round(Number(pricingHoliday) * 100) : null,
      });
      setActionResult({ action: "ticket_type_pricing_updated", ticketTypeId: comboTicketId });
      await loadPriceCheck(comboTicketId);
      reload();
    } catch (err) {
      setPricingError(err instanceof Error ? err.message : "改售价失败");
    } finally {
      setPricingBusy(false);
    }
  }

  function updateBarLine(index: number, patch: Partial<BarLine>) {
    setBarLines(barLines.map((l, i) => (i === index ? { ...l, ...patch } : l)));
    if (patch.merchantId) void loadBarProducts(patch.merchantId);
  }

  function addBarLine() {
    const merchantId = merchants[0]?.id ?? "";
    setBarLines([...barLines, { merchantId, kind: "gift_drink", totalTimes: "1", valueYuan: "", barItemRef: "", costBearer: "" }]);
    if (merchantId) void loadBarProducts(merchantId);
  }
  function addDiningLine() {
    setDiningLines([...diningLines, { merchantId: merchants[0]?.id ?? "", valueYuan: "", discount: "", catalogCode: "", costBearer: "", totalTimes: "1" }]);
  }

  // 创建单条权益模板并绑定到目标通票。merchantId 必填（后端强制）。
  async function createAndBind(payload: {
    name: string;
    entitlementType: string;
    totalTimes: number;
    valueCent: number | null;
    merchantId: string;
    // 批10/E：内部结算价收编进 SKU 目录——此处只挂目录条目引用；专属餐厅商户(可选，仅餐饮传)。核销按目录生效价全额归餐厅。
    catalogItemCode?: string | null;
    exclusiveMerchantId?: string | null;
    // 酒水兑换：提供方商品（成本按该商品的内部结算价与这家酒吧结算）。
    barItemRef?: string | null;
    // S11 票面原价（分）：这份权益在票面上值多少钱，决定客人这笔钱按什么比例分给它所属的业态。
    //   免费/赠送填 0（不分钱，成本另按内部结算价与提供方结算）。与内部结算价是两条价线，不可互相顶替。
    componentPriceCent: number;
    // V376 成本承担方：赠送权益（票面价 0 的提供方兑付类）必填，漏标后端拒保存；非赠送类传 null。
    costBearer?: string | null;
  }, ticketTypeId: string) {
    const tpl = await api.post<{ id: string }>("/api/v1/ticketing/entitlement-templates", {
      idempotency_key: crypto.randomUUID(),
      name: payload.name,
      entitlement_type: payload.entitlementType,
      default_total_times: payload.totalTimes,
      default_value_cent: payload.valueCent,
      status: "active",
      merchant_id: payload.merchantId,
      catalog_item_code: payload.catalogItemCode ?? null,
      exclusive_merchant_id: payload.exclusiveMerchantId ?? null,
      bar_item_ref: payload.barItemRef ?? null,
    });
    await api.post(`/api/v1/ticketing/ticket-types/${ticketTypeId}/entitlements`, {
      idempotency_key: crypto.randomUUID(),
      entitlement_template_id: tpl.id,
      quantity: 1,
      total_times: payload.totalTimes,
      num_people: 1,
      component_price_cent: payload.componentPriceCent,
      // V376：赠送权益必须标成本承担方（商户让利型/平台出资型），漏标后端拒保存/拒发布。
      cost_bearer: payload.costBearer ?? null,
    });
  }

  async function submitCombo() {
    if (!comboTicketId) {
      setComboError("请先选择要配置的通票。");
      return;
    }
    const chosenProjects = projects.filter((p) => selectedProjects[p.id]);
    if (barLines.length === 0 && chosenProjects.length === 0 && diningLines.length === 0) {
      setComboError("至少选择一个酒吧权益、游乐设施或餐饮项。");
      return;
    }
    // 游乐权益也须归属商户（后端强制），选了游乐就必须选归属商户。
    if (chosenProjects.length > 0 && !amusementMerchantId) {
      setComboError("已勾选游乐设施，请选择游乐权益的归属商户。");
      return;
    }
    // 酒水兑换必须指明是哪件商品：成本按该商品的内部结算价与这家酒吧结算，不指明就没法结账。
    const missingProduct = barLines.find((line) => line.merchantId && line.kind === "gift_drink" && !line.barItemRef);
    if (missingProduct) {
      setComboError(`${merchantName(missingProduct.merchantId)} 的酒水兑换请选择赠送的商品。`);
      return;
    }
    // V376：赠送权益（票面价 0）必须标成本承担方——这杯酒/这份赠品的成本谁出，漏标后端也会拒。
    const missingBarBearer = barLines.find((line) =>
      line.merchantId && line.kind === "gift_drink" && !(Number(line.valueYuan) > 0) && !line.costBearer);
    if (missingBarBearer) {
      setComboError(`${merchantName(missingBarBearer.merchantId)} 的酒水兑换是赠送权益，请选择成本承担方（商户让利型/平台出资型）。`);
      return;
    }
    const missingDiningBearer = diningLines.find((line) =>
      line.merchantId && !(Number(line.valueYuan) > 0) && !line.costBearer);
    if (missingDiningBearer) {
      setComboError(`${merchantName(missingDiningBearer.merchantId)} 的餐饮项是纯赠送（价格 0），请选择成本承担方（商户让利型/平台出资型）。`);
      return;
    }
    setComboError("");
    setComboBusy(true);
    try {
      // 酒吧权益：每行一条模板（gift_drink=酒水兑换 / admission=入场资格），绑定该商户。
      for (const line of barLines) {
        if (!line.merchantId) continue;
        const kindLabel = BAR_ENTITLEMENT_KINDS.find((k) => k.value === line.kind)?.label ?? line.kind;
        await createAndBind({
          name: `${merchantName(line.merchantId)} · ${kindLabel}`,
          entitlementType: line.kind,
          totalTimes: Number(line.totalTimes) || 1,
          valueCent: line.valueYuan.trim() ? Math.round(Number(line.valueYuan) * 100) : null,
          merchantId: line.merchantId,
          barItemRef: line.kind === "gift_drink" ? line.barItemRef : null,
          // 票面原价 = 这一行填的价格；赠送/免费的留空即 0（不参与分钱）。
          componentPriceCent: line.valueYuan.trim() ? Math.round(Number(line.valueYuan) * 100) : 0,
          // V376：酒水兑换的成本承担方（赠送时必选，上方已前置拦）。入场资格无此概念。
          costBearer: line.kind === "gift_drink" ? (line.costBearer || null) : null,
        }, comboTicketId);
      }
      // 游乐设施：每个项目一条 amusement 类权益模板 + 绑定通票 + 收紧项目核销绑定。归属商户用统一选择的 amusementMerchantId。
      for (const project of chosenProjects) {
        await createAndBind({
          name: `${project.projectName} · 游乐`,
          entitlementType: "amusement",
          totalTimes: Number(projectTimes) || 1,
          valueCent: null,
          merchantId: amusementMerchantId,
          // 游乐票面原价：游乐份额就是按它在整票里的占比分给游乐公司的，留空即 0=不分。
          componentPriceCent: amusementPriceYuan.trim() ? Math.round(Number(amusementPriceYuan) * 100) : 0,
        }, comboTicketId);
        // 项目↔权益类型绑定（V082）：配置后该项目核销收紧为「仅接受 amusement 类权益」。
        await api.post(`/api/v1/amusement/projects/${project.id}/entitlements`, {
          idempotency_key: crypto.randomUUID(),
          entitlement_type: "amusement",
        }).catch(() => undefined); // 已绑定（唯一键冲突）时忽略。
      }
      // 餐饮权益：每行一条 dining 权益模板（entitlement_type='dining'）。
      //   价格(面值,可0)→default_value_cent；内部结算价→agreement_price_cent；专属餐厅→exclusive_merchant_id。
      //   核销结算全额按内部结算价归该餐厅(§6.4 不分摊)，不把折扣/面值当结算价乱记。折扣仅记入名称备注(售卖口径，不影响结算)。
      for (const line of diningLines) {
        if (!line.merchantId) continue;
        const discountNote = line.discount.trim() ? ` · ${line.discount}折` : "";
        await createAndBind({
          name: `${merchantName(line.merchantId)} · 餐饮${discountNote}`,
          entitlementType: "dining",
          totalTimes: Number(line.totalTimes) || 1,
          // 价格(面值)可为 0：留空=0（纯赠送/兑换）。
          valueCent: line.valueYuan.trim() ? Math.round(Number(line.valueYuan) * 100) : 0,
          merchantId: line.merchantId,
          // 批10/E：内部结算价唯一可编辑处 = 内部结算 SKU 目录；此处只挂目录条目引用（留空=未配价）。
          catalogItemCode: line.catalogCode.trim() || null,
          // 专属餐厅商户：使核销结算全额归该餐厅、不进池分摊。
          exclusiveMerchantId: line.merchantId,
          // 票面原价 = 这一行填的价格（0=纯赠送，不参与分钱）。内部结算价是另一条线，不能拿来当票面原价。
          componentPriceCent: line.valueYuan.trim() ? Math.round(Number(line.valueYuan) * 100) : 0,
          // V376：纯赠送（价格 0）的餐饮项必须标成本承担方（上方已前置拦）；有价格的选填。
          costBearer: line.costBearer || null,
        }, comboTicketId);
      }
      setActionResult({
        action: "pass_config_strategy_composed",
        ticketTypeId: comboTicketId,
        bars: barLines.length,
        amusementProjects: chosenProjects.length,
        dining: diningLines.length,
        // V369：改了票的组成，这张票会退回草稿——售价要按新的业态构成重新核对再发布。
        note: "已上架的票改了组成会退回草稿，请在下方「定价核算」核对售价后重新发布。",
      });
      setBarLines([]);
      setSelectedProjects({});
      setDiningLines([]);
      void loadGiftCoverage(comboTicketId);
      void loadPriceCheck(comboTicketId);
      reload();
    } catch (composeError) {
      setComboError(composeError instanceof Error ? composeError.message : "组合配置失败");
    } finally {
      setComboBusy(false);
    }
  }

  // #172(2026-07-19 拍板)：保存全场统一动态码刷新间隔（出示验证类二维码全场一套设置）。
  async function saveQrRefresh() {
    setGlobalError(null);
    try {
      const saved = await api.put<{ refreshIntervalSeconds?: number }>(
        "/api/v1/ticketing/qr-refresh-config", { refreshIntervalSeconds: qrRefresh });
      setQrRefresh(Number(saved?.refreshIntervalSeconds) || qrRefresh);
      setActionResult({ action: "qr_refresh_config_updated", refreshIntervalSeconds: qrRefresh });
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "动态码刷新间隔保存失败");
    }
  }

  return (
    <section data-testid="ticketing-policies-page">
      <AdminPageHeader
        title="票务策略"
        description="票种/通票、权益、适用范围与派生价规则全部在此配置：先建票种并发布，再用「组合式配置策略」一次性给通票配好跨酒吧/游乐/餐饮的权益，最后配派生价（新人/学生/多人/渠道/会员）。"
      />

      {focusObjectId && (
        <div className="admin-state" data-testid="ticketing-policies-focus">已从下钻定位到票种：{focusObjectId}（下表高亮行）</div>
      )}
      <div className="admin-preview" data-testid="ticketing-workflow-guide">
        <strong>怎么用这一页（票务策略配置流程）：</strong>① 先在「票种 / 通票」建好票种并发布 → ② 建「权益模板」（入园 / 酒水兑换 / 游乐等可复用的权益）→ ③ 用「通票组装」把票种绑上权益，或用「组合式套票配置」一次性给一张通票配好跨酒吧 / 游乐 / 餐饮的套票权益 → ④ 最后配「派生价规则」（新人 / 学生 / 多人 / 渠道 / 会员的折扣或固定价）。切换下方标签逐步配置。
      </div>
      <div className="admin-preview" data-testid="ticketing-qr-refresh-config">
        <strong>动态验证码刷新间隔（全场统一）：</strong>
        凡「出示验证类」二维码（门票核销码 / 权益核销码 / 入场码 / 付款出示码）都按此间隔自动刷新——全场一套统一设置、以 5 秒为单位、最长 60 秒；截图 / 转发的旧码过间隔即作废。（纯「扫码打开链接」类二维码不受影响。）
        <div style={{ marginTop: 8 }}>
          <label>刷新间隔（秒）
            <select data-testid="ticket-qr-refresh-config-select" aria-label="动态验证码刷新间隔（秒）"
              value={String(qrRefresh)} onChange={(event) => setQrRefresh(Number(event.target.value))}>
              {QR_REFRESH_OPTIONS.map((s) => (
                <option key={s} value={s}>{s} 秒{s === 15 ? "（默认）" : ""}</option>
              ))}
            </select>
          </label>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ticket-qr-refresh-config-save" onClick={() => void saveQrRefresh()}>
            保存刷新间隔
          </PermissionButton>
        </div>
      </div>
      <TicketExpiryReminderSection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      <BarArrivalReminderSection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      <Tabs
        type="card"
        data-testid="ticketing-tabs"
        items={[
          { key: "types", label: "① 票种 / 通票", forceRender: true, children: (<>
      <h3>票种 / 通票</h3>
      <label className="admin-inline-check" data-testid="ticket-catalog-show-archived">
        <input type="checkbox" checked={showArchived} onChange={(event) => setShowArchived(event.target.checked)} />
        显示已存档票种与派生价规则
      </label>
      <AdminDataState loading={loading} error={loadError} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-types-table">
            <thead>
              <tr>
                <th>票种名称</th>
                <th>平日（元）</th>
                <th>周末（元）</th>
                <th>节假日（元）</th>
                <th>限量</th>
                <th>有效期</th>
                <th>可退</th>
                <th>单店酒吧</th>
                <th>状态</th>
                <th>存档状态</th>
                <th>操作</th>
                <th>详情</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((row) => (
                <tr key={row.id} data-testid={`ticket-type-${row.id}`}
                  className={`admin-row-clickable${focusObjectId === row.id ? " admin-row-selected" : ""}`} tabIndex={0}
                  onClick={() => openTicketTypeDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTicketTypeDetail(row); } }}>
                  <td>{row.ticketName}</td>
                  <td>{yuan(row.weekdayPriceCent)}</td>
                  <td>{yuan(row.weekendPriceCent)}</td>
                  <td>{yuan(row.holidayPriceCent)}</td>
                  <td>{row.stockQuantity ?? "不限"}</td>
                  <td>{row.validityDays ?? 1} 天</td>
                  <td>{row.refundable ? "可退" : "不可退"}</td>
                  <td>{row.barId ? (merchants.find((m) => m.id === row.barId)?.merchantName ?? row.barId) : "通票"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td>{archiveLabel(row)}</td>
                  <td onClick={(event) => event.stopPropagation()}>{catalogActions("ticket-types", row)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看票种详情" onClick={(event) => { event.stopPropagation(); openTicketTypeDetail(row); }}>查看详情</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      {/* 引导式流程 phase-2：新建票种入口按钮（权限门在入口），点击弹出分步引导。 */}
      <div className="admin-form-row">
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ticket-create-open" onClick={() => { setError(""); setCreateKey(crypto.randomUUID()); setCreateOpen(true); }}>
          ＋ 新建票种
        </PermissionButton>
      </div>
      <GuidedFlowModal
        open={createOpen}
        title="新建票种"
        testId="ticket-policy"
        submitTestId="ticket-publish"
        submitLabel="发布策略"
        submitting={busy}
        error={error}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void publish()}
        steps={[
          {
            title: "填写票种信息",
            description: "票种名称与平日/周末/节假日基础价必填（价格大于 0），限量可空表示不限。填完点下一步核对信息再发布。",
            validate: () => {
              if (!ticketName.trim()) return "票种名称必填。";
              if (!weekday || !weekend || !holiday) return "平日、周末、节假日基础价必须全部填写，缺少节假日价不可发布。";
              const prices: Record<string, number> = { 平日价: Number(weekday), 周末价: Number(weekend), 节假日价: Number(holiday) };
              const invalid = Object.entries(prices).find(([, value]) => !Number.isFinite(value) || value <= 0);
              return invalid ? `${invalid[0]}必须是大于 0 的数字。` : null;
            },
            content: (
              <>
                <label>票种名称<input data-testid="ticket-name" value={ticketName} onChange={(event) => setTicketName(event.target.value)} placeholder="票种名称" aria-label="票种名称" /></label>
                <label>平日价（元）<input data-testid="ticket-weekday-price" type="number" min={0} step="0.01" value={weekday} onChange={(event) => setWeekday(event.target.value)} placeholder="平日价" /></label>
                <label>周末价（元）<input data-testid="ticket-weekend-price" type="number" min={0} step="0.01" value={weekend} onChange={(event) => setWeekend(event.target.value)} placeholder="周末价" /></label>
                <label>节假日价（元）<input data-testid="ticket-holiday-price" type="number" min={0} step="0.01" value={holiday} onChange={(event) => setHoliday(event.target.value)} placeholder="节假日价" /></label>
                <label>票种折扣（%，100=不打折）<input data-testid="ticket-combo-discount" type="number" min={0} max={100} step="0.01" value={createDiscount} onChange={(event) => setCreateDiscount(event.target.value)} placeholder="100" aria-label="票种折扣" /></label>
                <label>限量<input data-testid="ticket-stock" type="number" min={0} value={stock} onChange={(event) => setStock(event.target.value)} placeholder="限量（可空=不限）" aria-label="限量" /></label>
                <label>有效期（天）
                  <select data-testid="ticket-validity-days" aria-label="有效期（天）" value={validityDays} onChange={(event) => setValidityDays(event.target.value)}>
                    <option value="1">当日（1天）</option>
                    <option value="7">周票（7天）</option>
                    <option value="30">月票（30天）</option>
                    <option value="365">年票（365天）</option>
                  </select>
                </label>
                <label>票种分类
                  <select data-testid="ticket-category" aria-label="票种分类" value={ticketCategory} onChange={(event) => setTicketCategory(event.target.value)}>
                    <option value="standard">标准票</option>
                    <option value="new_person">新人票</option>
                    <option value="student">学生票</option>
                  </select>
                </label>
                <label>
                  <input data-testid="ticket-refundable" type="checkbox" checked={refundable} onChange={(event) => setRefundable(event.target.checked)} aria-label="可退票" />
                  可退票（勾选=客人可申请整票退款；不勾=不可退。到期未核销一律作废，任何票种都不自动退）
                </label>
                <label>单店票绑定酒吧（可空=通票）
                  <select data-testid="ticket-bar-select" aria-label="单店票绑定酒吧" value={barId} onChange={(event) => setBarId(event.target.value)}>
                    <option value="">不绑定（通票）</option>
                    {merchants.filter((m) => m.merchantType === "bar").map((m) => (
                      <option key={m.id} value={m.id}>{m.merchantName}</option>
                    ))}
                  </select>
                </label>
                <div data-testid="ticket-derived-preview" className="admin-preview">
                  新人票、学生票、多人票、渠道价、会员价在下方「派生价规则」区配置：按基础价折扣派生或固定价。
                </div>
              </>
            ),
          },
          {
            title: "绑定权益并发布",
            description: "公开可售的票必须能出票：至少绑定一个权益模板，填写发布理由后上架。更多分项权益可发布后在「组合式套票配置」补充。",
            validate: () => {
              if (!createTemplateId) return "必须选择一个权益模板。";
              if (!publishReason.trim()) return "发布理由必填。";
              return null;
            },
            content: (
              <>
                <label>权益模板（必选）
                  <select data-testid="ticket-create-template" aria-label="权益模板" value={createTemplateId} onChange={(event) => setCreateTemplateId(event.target.value)}>
                    <option value="">选择权益模板</option>
                    {templates.map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </label>
                <label>发布理由（必填）<input data-testid="ticket-publish-reason" value={publishReason} onChange={(event) => setPublishReason(event.target.value)} placeholder="如：暑期档新票上架" aria-label="发布理由" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "票种名称", value: ticketName.trim() || "未填写" },
          { label: "平日价（元）", value: weekday || "未填写" },
          { label: "周末价（元）", value: weekend || "未填写" },
          { label: "节假日价（元）", value: holiday || "未填写" },
          { label: "限量", value: stock.trim() || "不限" },
          { label: "有效期（天）", value: validityDays },
          { label: "票种分类", value: TICKET_CATEGORY_LABELS[ticketCategory] ?? "标准票" },
          { label: "可退票", value: refundable ? "可退" : "不可退" },
          { label: "单店酒吧", value: barId ? (merchants.find((m) => m.id === barId)?.merchantName ?? barId) : "通票" },
          { label: "权益模板", value: createTemplateId ? (templates.find((t) => t.id === createTemplateId)?.name ?? createTemplateId) : "未选择" },
          { label: "发布理由", value: publishReason.trim() || "未填写" },
        ]}
      />

          </>) },
          { key: "combo", label: "② 组合式套票配置", forceRender: true, children: (<>
      <h3>组合式配置策略（一次性给通票配跨酒吧/游乐/餐饮的权益）</h3>
      <div className="admin-preview">
        选一张通票，勾选多个酒吧并为每个配权益（酒水兑换/次数/面值）、多选游乐设施、加餐饮项（餐厅/价格/折扣/内部结算价），点「组合配置到通票」一次性写入。
      </div>
      <div className="admin-inline-form" data-testid="pass-combo-form">
        <select aria-label="配置通票" data-testid="combo-ticket-select" value={comboTicketId} onChange={(event) => { setComboTicketId(event.target.value); void loadGiftCoverage(event.target.value); void loadPriceCheck(event.target.value); }}>
          <option value="">选择要配置的通票</option>
          {tickets.map((t) => (
            <option key={t.id} value={t.id}>{t.ticketName}</option>
          ))}
        </select>
      </div>

      <h4>定价核算（系统算出售价 vs 你填的售价）</h4>
      <div className="admin-preview">
        通票的售价不用自己拍脑袋：各业态价格加起来、再按票种折扣打一下，系统就算出该卖多少。
        你可以在这个数上下调一点凑个好卖的整数（尾数 8/9/0），但差得太多就是填错了——超过后台配的允许误差，这张票不许开卖。
        免费权益（各业态价格 0）不计入价值。允许误差在「结算配置中心 → 通票分账」维护。
      </div>
      {!comboTicketId ? (
        <div className="admin-preview" data-testid="combo-price-check-empty">请先选择要配置的通票。</div>
      ) : !priceCheck ? (
        <div className="admin-preview" data-testid="combo-price-check-empty">定价核算读取中或暂不可用。</div>
      ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="combo-price-check-table">
            <tbody>
              <tr><th>各业态价格合计</th><td data-testid="price-check-component-total">¥{yuan(priceCheck.componentTotalCent)}</td></tr>
              <tr><th>票种折扣</th><td data-testid="price-check-discount">{String(priceCheck.comboDiscountRate ?? 100)}%</td></tr>
              <tr><th>系统算出售价</th><td data-testid="price-check-computed">¥{yuan(priceCheck.computedPriceCent)}</td></tr>
              <tr><th>当前填的售价（平日价）</th><td data-testid="price-check-filled">{priceCheck.filledPriceCent == null ? "还没填" : `¥${yuan(priceCheck.filledPriceCent)}`}</td></tr>
              <tr><th>允许误差</th><td data-testid="price-check-tolerance">¥{yuan(priceCheck.toleranceCent)}</td></tr>
              <tr><th>凑整建议</th><td data-testid="price-check-suggested">¥{yuan(priceCheck.roundingSuggested)}（整元、尾数 8/9/0）</td></tr>
            </tbody>
          </table>
        </div>
      )}
      <div className="admin-inline-form" data-testid="combo-pricing-form">
        <label>票种折扣（%，100=不打折）
          <input aria-label="票种折扣" data-testid="combo-pricing-discount" type="number" min={0} max={100} step="0.01"
            value={pricingDiscount} onChange={(event) => setPricingDiscount(event.target.value)} />
        </label>
        <label>平日售价（元，凑整后的近似售价）
          <input aria-label="平日售价" data-testid="combo-pricing-weekday" type="number" min={0} step="0.01"
            value={pricingWeekday} onChange={(event) => setPricingWeekday(event.target.value)} />
        </label>
        <label>周末售价（元，可空）
          <input aria-label="周末售价" data-testid="combo-pricing-weekend" type="number" min={0} step="0.01"
            value={pricingWeekend} onChange={(event) => setPricingWeekend(event.target.value)} />
        </label>
        <label>节假日售价（元，可空）
          <input aria-label="节假日售价" data-testid="combo-pricing-holiday" type="number" min={0} step="0.01"
            value={pricingHoliday} onChange={(event) => setPricingHoliday(event.target.value)} />
        </label>
        <label>改价理由
          <input aria-label="改价理由" data-testid="combo-pricing-reason" value={pricingReason}
            onChange={(event) => setPricingReason(event.target.value)} placeholder="例如：按新的业态构成重新定价" />
        </label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="combo-pricing-save"
          disabled={pricingBusy} onClick={() => void savePricing()}>保存售价</PermissionButton>
      </div>
      {priceCheck && pricingLiveDiffCent ? (
        <div className="admin-preview" data-testid="combo-pricing-live-hint">
          {Math.abs(pricingLiveDiffCent.diff) <= priceCheck.toleranceCent
            ? `没问题：系统算出 ¥${yuan(pricingLiveDiffCent.computed)}，你填 ¥${yuan(pricingLiveDiffCent.filled)}，`
              + `${pricingLiveDiffCent.diff >= 0 ? "上调" : "下调"} ¥${yuan(Math.abs(pricingLiveDiffCent.diff))}，`
              + `在允许误差 ¥${yuan(priceCheck.toleranceCent)} 之内。`
            : `超了：系统算出 ¥${yuan(pricingLiveDiffCent.computed)}，你填 ¥${yuan(pricingLiveDiffCent.filled)}，`
              + `${pricingLiveDiffCent.diff >= 0 ? "上调" : "下调"} ¥${yuan(Math.abs(pricingLiveDiffCent.diff))}，`
              + `超出允许误差 ¥${yuan(priceCheck.toleranceCent)} 达 ¥${yuan(Math.abs(pricingLiveDiffCent.diff) - priceCheck.toleranceCent)}，`
              + `这张票发不出去。凑整可填 ¥${yuan(priceCheck.roundingSuggested)}。`}
        </div>
      ) : null}
      {pricingError ? <div className="admin-error" data-testid="combo-pricing-error">{pricingError}</div> : null}

      <h4>酒吧权益（多选）</h4>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="combo-bar-table">
          <thead>
            <tr><th>酒吧/商户</th><th>权益</th><th>赠送商品</th><th>成本承担方</th><th>次数</th><th>面值（元）</th><th>操作</th></tr>
          </thead>
          <tbody>
            {barLines.map((line, index) => (
              <tr key={index} data-testid={`combo-bar-line-${index}`}>
                <td>
                  <select aria-label="酒吧" value={line.merchantId} onChange={(event) => updateBarLine(index, { merchantId: event.target.value, barItemRef: "" })}>
                    <option value="">选择商户</option>
                    {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
                  </select>
                </td>
                <td>
                  <select aria-label="权益" value={line.kind} onChange={(event) => updateBarLine(index, { kind: event.target.value })}>
                    {BAR_ENTITLEMENT_KINDS.map((k) => (<option key={k.value} value={k.value}>{k.label}</option>))}
                  </select>
                </td>
                <td>
                  {line.kind === "gift_drink" ? (
                    <select aria-label="赠送商品" data-testid={`combo-bar-product-${index}`} value={line.barItemRef} onChange={(event) => updateBarLine(index, { barItemRef: event.target.value })}>
                      <option value="">选择该酒吧的商品</option>
                      {(barProducts[line.merchantId] ?? []).map((product) => (
                        <option key={product.id} value={product.id}>{product.productName}</option>
                      ))}
                    </select>
                  ) : <span>—</span>}
                </td>
                <td>
                  {line.kind === "gift_drink" ? (
                    <select aria-label="成本承担方" data-testid={`combo-bar-cost-bearer-${index}`} value={line.costBearer} onChange={(event) => updateBarLine(index, { costBearer: event.target.value })}>
                      <option value="">这杯酒的成本谁出（必选）</option>
                      {COST_BEARER_OPTIONS.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
                    </select>
                  ) : <span>—</span>}
                </td>
                <td><input aria-label="次数" type="number" min={1} value={line.totalTimes} onChange={(event) => updateBarLine(index, { totalTimes: event.target.value })} /></td>
                <td><input aria-label="面值（元）" type="number" min={0} step="0.01" value={line.valueYuan} placeholder="可空" onChange={(event) => updateBarLine(index, { valueYuan: event.target.value })} /></td>
                <td><button type="button" className="admin-secondary-button" onClick={() => setBarLines(barLines.filter((_, i) => i !== index))}>移除</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <button type="button" className="admin-secondary-button" data-testid="combo-add-bar" onClick={addBarLine} disabled={merchants.length === 0}>+ 添加酒吧权益</button>
      <div className="admin-preview">
        酒水兑换要指明赠送的是哪件商品：客人只能到这家酒吧核销，平台按这件商品的内部结算价与这家酒吧结算。
        这张票让客人能进的每个酒吧都要至少赠一杯，各吧几杯写在票面上（下方清单即票面写明的内容）。
      </div>

      <h4>票面赠酒清单（各酒吧分别赠几杯）</h4>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="combo-gift-coverage-table">
          <thead>
            <tr><th>酒吧</th><th>赠品</th><th>份数</th><th>杯数</th><th>该吧合计</th></tr>
          </thead>
          <tbody>
            {giftCoverage.length === 0 ? (
              <tr><td colSpan={5}>{comboTicketId ? "该票种还没有赠酒权益" : "请先选择要配置的通票"}</td></tr>
            ) : giftCoverage.map((row) => (
              <tr key={`${row.barId}-${row.giftName}`}>
                <td>{row.barName}</td>
                <td>{row.giftName}</td>
                <td>{row.quantity}</td>
                <td>{row.cups}</td>
                <td>{row.barTotalCups}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h4>游乐设施（多选）</h4>
      <div className="admin-inline-form" data-testid="combo-amusement-list">
        {projects.length === 0 ? <span>暂无游乐设施</span> : projects.map((p) => (
          <label key={p.id} className="admin-checkbox" data-testid={`combo-project-${p.id}`}>
            <input type="checkbox" checked={!!selectedProjects[p.id]} onChange={(event) => setSelectedProjects({ ...selectedProjects, [p.id]: event.target.checked })} />
            {p.projectName}
          </label>
        ))}
        <input aria-label="游乐次数" type="number" min={1} value={projectTimes} onChange={(event) => setProjectTimes(event.target.value)} placeholder="游乐次数（每设施）" />
        <input aria-label="游乐业态价格（元）" type="number" min={0} step="0.01" value={amusementPriceYuan} data-testid="combo-amusement-price" onChange={(event) => setAmusementPriceYuan(event.target.value)} placeholder="游乐业态价格（元，每设施）" />
        <select aria-label="游乐权益归属商户" data-testid="combo-amusement-merchant" value={amusementMerchantId} onChange={(event) => setAmusementMerchantId(event.target.value)}>
          <option value="">游乐权益归属商户</option>
          {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
        </select>
      </div>

      <h4>餐饮（多选）</h4>
      <div className="admin-preview">
        餐饮的钱归餐厅商户所有：核销时按「内部结算价」全额结算给该餐厅（不进池、不分摊）。价格可设为 0（纯赠送/兑换）；也可同时配价格、折扣与内部结算价。折扣仅为售卖口径，不影响结算金额。
      </div>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="combo-dining-table">
          <thead>
            <tr><th>餐厅商户</th><th>价格（元，可 0）</th><th>折扣（如 8=八折，选填）</th><th>内部结算价（元）</th><th>成本承担方（价格 0 必选）</th><th>次数</th><th>操作</th></tr>
          </thead>
          <tbody>
            {diningLines.map((line, index) => (
              <tr key={index} data-testid={`combo-dining-line-${index}`}>
                <td>
                  <select aria-label="餐厅商户" value={line.merchantId} onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, merchantId: event.target.value } : l))}>
                    <option value="">选择餐厅商户</option>
                    {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
                  </select>
                </td>
                <td>
                  <input aria-label="价格（元）" type="number" min={0} step="0.01" value={line.valueYuan} placeholder="0=纯赠送" onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, valueYuan: event.target.value } : l))} />
                </td>
                <td>
                  <input aria-label="折扣" type="number" min={0} max={10} step="0.5" value={line.discount} placeholder="选填，如 8" onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, discount: event.target.value } : l))} />
                </td>
                <td>
                  <input aria-label="内部结算价目录条目编码" value={line.catalogCode} placeholder="目录条目编码（结算→内部结算价目录）" onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, catalogCode: event.target.value } : l))} />
                </td>
                <td>
                  <select aria-label="餐饮成本承担方" data-testid={`combo-dining-cost-bearer-${index}`} value={line.costBearer} onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, costBearer: event.target.value } : l))}>
                    <option value="">纯赠送时必选</option>
                    {COST_BEARER_OPTIONS.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
                  </select>
                </td>
                <td><input aria-label="餐饮次数" type="number" min={1} value={line.totalTimes} onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, totalTimes: event.target.value } : l))} /></td>
                <td><button type="button" className="admin-secondary-button" onClick={() => setDiningLines(diningLines.filter((_, i) => i !== index))}>移除</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <button type="button" className="admin-secondary-button" data-testid="combo-add-dining" onClick={addDiningLine} disabled={merchants.length === 0}>+ 添加餐饮项</button>

      <div className="admin-form-row">
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="combo-submit" onClick={() => void submitCombo()} disabled={comboBusy}>
          组合配置到通票
        </PermissionButton>
      </div>
      {comboError ? <div data-testid="combo-error" className="admin-inline-error">{comboError}</div> : null}

          </>) },
          { key: "templates", label: "③ 权益模板", forceRender: true, children: (<>
      <h3>权益模板</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="entitlement-templates-table">
          <thead>
            <tr>
              <th>模板名称</th>
              <th>权益类型</th>
              <th>签约价</th>
              <th>状态</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            {templates.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`entitlement-template-${row.id}`}
                onClick={() => openTemplateDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
                <td>{row.name}</td>
                <td>{formatBusinessStatus(row.entitlementType)}</td>
                <td>{row.agreementPriceCent != null ? `¥${yuan(row.agreementPriceCent)}` : "未设（按面值结算）"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看权益模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="admin-inline-form" data-testid="entitlement-template-form">
        <input aria-label="模板名称" placeholder="模板名称（如 入园1次）" value={tplForm.name} onChange={(event) => setTplForm({ ...tplForm, name: event.target.value })} />
        <select aria-label="提供方商户" data-testid="entitlement-template-merchant" value={tplForm.merchantId} onChange={(event) => setTplForm({ ...tplForm, merchantId: event.target.value })}>
          <option value="">选择提供方商户</option>
          {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
        </select>
        <select aria-label="权益类型" value={tplForm.entitlementType} onChange={(event) => setTplForm({ ...tplForm, entitlementType: event.target.value })}>
          {ENTITLEMENT_TYPES.map((t) => (
            <option key={t} value={t}>{formatBusinessStatus(t)}</option>
          ))}
        </select>
        <input aria-label="默认次数" type="number" min={1} value={tplForm.totalTimes} onChange={(event) => setTplForm({ ...tplForm, totalTimes: event.target.value })} placeholder="默认次数" />
        <input aria-label="默认面值（元）" type="number" min={0} step="0.01" value={tplForm.valueYuan} onChange={(event) => setTplForm({ ...tplForm, valueYuan: event.target.value })} placeholder="默认面值（元，可空）" />
        <PermissionButton permissionsState={effectivePermissions} permissions={["ticket.entitlement.manage"]} data-testid="entitlement-template-create" onClick={() => void createTemplate()}>
          新建模板
        </PermissionButton>
      </div>

      <h3>签约价（虚拟权益 · 内部结算价）</h3>
      <p style={{ color: "var(--adm-text-3)", fontSize: 13 }} data-testid="agreement-price-scope-hint">
        签约价（内部结算价）已收编进「结算 → 内部结算价目录」，那里是唯一可改价处：改价先存草案、由具备高敏权限的他人复核转正后才对<strong>之后的核销/结算</strong>生效；已核销的账按核销当时固化的价，一分不会被改动。本页列表里的签约价是目录价快照，只读展示。
      </p>

      <h3>第三方券（餐厅券 · 通票包含）</h3>
      <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>把某个权益模板配成第三方券：免费营销券核销不结算；内部价采购券每核销一张按内部结算价给第三方挂应付。客人买含该权益的通票即附赠此券，到餐厅由餐厅自己系统核销，再经对账文件回流。</p>
      <div className="admin-inline-form" data-testid="partner-voucher-config-form">
        <select aria-label="第三方券-权益模板" value={pvConfig.templateId} onChange={(e) => setPvConfig({ ...pvConfig, templateId: e.target.value })}>
          <option value="">选择权益模板</option>
          {templates.map((t) => (<option key={t.id} value={t.id}>{t.name}</option>))}
        </select>
        <select aria-label="结算模式" value={pvConfig.settlementMode} onChange={(e) => setPvConfig({ ...pvConfig, settlementMode: e.target.value })}>
          <option value="free_marketing">免费营销券（核销不结算）</option>
          <option value="internal_purchased">内部价采购券（核销挂应付）</option>
        </select>
        <select aria-label="第三方类型" value={pvConfig.partnerKind} onChange={(e) => setPvConfig({ ...pvConfig, partnerKind: e.target.value, partnerId: "" })}>
          <option value="merchant">入驻商户</option>
          <option value="external_partner">外部合作餐厅</option>
        </select>
        {pvConfig.partnerKind === "merchant" ? (
          <select aria-label="第三方商户" value={pvConfig.partnerId} onChange={(e) => setPvConfig({ ...pvConfig, partnerId: e.target.value })}>
            <option value="">选择商户</option>
            {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
          </select>
        ) : (
          <input aria-label="外部餐厅id" placeholder="外部合作餐厅 id" value={pvConfig.partnerId} onChange={(e) => setPvConfig({ ...pvConfig, partnerId: e.target.value })} />
        )}
        {pvConfig.settlementMode === "internal_purchased" ? (
          <input aria-label="内部结算价（元）" type="number" min={0} step="0.01" placeholder="内部结算价（元）" value={pvConfig.internalYuan} onChange={(e) => setPvConfig({ ...pvConfig, internalYuan: e.target.value })} />
        ) : null}
        <PermissionButton permissionsState={effectivePermissions} permissions={["ticket.entitlement.manage"]} data-testid="partner-voucher-config-submit" onClick={() => void submitPvConfig()}>
          配为第三方券
        </PermissionButton>
      </div>
      <h4>核销回流导入（餐厅对账文件）</h4>
      <div className="admin-inline-form" data-testid="partner-voucher-redeem-form">
        <input aria-label="券码" placeholder="券码" value={pvRedeem.code} onChange={(e) => setPvRedeem({ ...pvRedeem, code: e.target.value })} />
        <label>核销日期<CnDatePicker aria-label="核销日期" value={pvRedeem.redeemedDate} onChange={(v) => setPvRedeem({ ...pvRedeem, redeemedDate: v })} /></label>
        <input aria-label="外部核销单号" placeholder="餐厅侧核销单号" value={pvRedeem.externalRef} onChange={(e) => setPvRedeem({ ...pvRedeem, externalRef: e.target.value })} />
        <PermissionButton permissionsState={effectivePermissions} permissions={["verification.execute"]} data-testid="partner-voucher-redeem-submit" onClick={() => void submitPvRedeem()}>
          导入核销
        </PermissionButton>
      </div>

          </>) },
          { key: "assemble", label: "④ 通票组装", forceRender: true, children: (<>
      <h3>通票组装（票种绑权益）</h3>
      <div className="admin-inline-form" data-testid="ticket-bind-form">
        <select aria-label="票种" value={bindForm.ticketTypeId} onChange={(event) => setBindForm({ ...bindForm, ticketTypeId: event.target.value })}>
          <option value="">选择票种</option>
          {tickets.map((t) => (
            <option key={t.id} value={t.id}>{t.ticketName}</option>
          ))}
        </select>
        <select aria-label="权益模板" value={bindForm.templateId} onChange={(event) => setBindForm({ ...bindForm, templateId: event.target.value })}>
          <option value="">选择权益模板</option>
          {templates.map((t) => (
            <option key={t.id} value={t.id}>{t.name}</option>
          ))}
        </select>
        <input aria-label="份数" type="number" min={1} value={bindForm.quantity} onChange={(event) => setBindForm({ ...bindForm, quantity: event.target.value })} placeholder="份数" />
        <input aria-label="每份次数" type="number" min={1} value={bindForm.totalTimes} onChange={(event) => setBindForm({ ...bindForm, totalTimes: event.target.value })} placeholder="每份次数" />
        <input aria-label="每份核销人数" type="number" min={1} value={bindForm.numPeople} onChange={(event) => setBindForm({ ...bindForm, numPeople: event.target.value })} placeholder="每份人数（多人票>1）" />
        <input aria-label="各业态价格（元）" type="number" min={0} step="0.01" value={bindForm.componentPriceYuan} onChange={(event) => setBindForm({ ...bindForm, componentPriceYuan: event.target.value })} placeholder="各业态价格（元，免费填0）" data-testid="ticket-bind-component-price" />
        <select aria-label="成本承担方" data-testid="ticket-bind-cost-bearer" value={bindForm.costBearer} onChange={(event) => setBindForm({ ...bindForm, costBearer: event.target.value })}>
          <option value="">成本承担方（赠送权益必选）</option>
          {COST_BEARER_OPTIONS.map((o) => (<option key={o.value} value={o.value}>{o.label}</option>))}
        </select>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ticket-bind-submit" onClick={() => void bindEntitlement()}>
          绑定权益
        </PermissionButton>
      </div>

          </>) },
          { key: "pricing", label: "⑤ 派生价规则", forceRender: true, children: (<>
      <h3>派生价规则</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="price-rules-table">
          <thead>
            <tr>
              <th>票种</th>
              <th>人群/渠道</th>
              <th>定价方式</th>
              <th>折扣/价格</th>
              <th>状态</th>
              <th>存档状态</th>
              <th>操作</th>
              <th>详情</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`price-rule-${row.id}`}
                onClick={() => openRuleDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRuleDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-TKT-001" objectId={row.ticketTypeId}>{tickets.find((t) => t.id === row.ticketTypeId)?.ticketName ?? row.ticketTypeId}</TraceLink></td>
                <td>{VARIANT_LABELS[row.variantType] ?? row.variantType}</td>
                <td>{row.priceMode === "fixed_price" ? "固定价" : "按基础价折扣"}</td>
                <td>{row.priceMode === "fixed_price" ? `¥${yuan(row.weekdayPriceCent)}` : row.discountRate != null ? `${Math.round(Number(row.discountRate) * 100)}%` : "—"}</td>
                <td>{formatBusinessStatus(row.status)}</td>
                <td>{archiveLabel(row)}</td>
                <td onClick={(event) => event.stopPropagation()}>{catalogActions("price-rules", row)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看派生价规则详情" onClick={(event) => { event.stopPropagation(); openRuleDetail(row); }}>查看详情</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="admin-inline-form" data-testid="price-rule-form">
        <select aria-label="派生票种" value={ruleForm.ticketTypeId} onChange={(event) => setRuleForm({ ...ruleForm, ticketTypeId: event.target.value })}>
          <option value="">选择票种</option>
          {tickets.map((t) => (
            <option key={t.id} value={t.id}>{t.ticketName}</option>
          ))}
        </select>
        <select aria-label="人群/渠道" value={ruleForm.variantType} onChange={(event) => setRuleForm({ ...ruleForm, variantType: event.target.value })}>
          {VARIANT_TYPES.map((v) => (
            <option key={v} value={v}>{VARIANT_LABELS[v]}</option>
          ))}
        </select>
        <select aria-label="定价方式" value={ruleForm.priceMode} onChange={(event) => setRuleForm({ ...ruleForm, priceMode: event.target.value })}>
          <option value="derive_from_calendar_price">按基础价折扣</option>
          <option value="fixed_price">固定价</option>
        </select>
        {ruleForm.priceMode === "fixed_price" ? (
          <input aria-label="固定价（元）" type="number" min={0} step="0.01" value={ruleForm.fixedYuan} onChange={(event) => setRuleForm({ ...ruleForm, fixedYuan: event.target.value })} placeholder="固定价（元）" />
        ) : (
          <input aria-label="折扣率" type="number" min={0} max={1} step="0.05" value={ruleForm.discount} onChange={(event) => setRuleForm({ ...ruleForm, discount: event.target.value })} placeholder="折扣率（0.8=8折）" />
        )}
        <PermissionButton permissionsState={effectivePermissions} permissions={["ticket.price_variant.manage"]} data-testid="price-rule-create" onClick={() => void createRule()}>
          新建派生价
        </PermissionButton>
      </div>
          </>) },
        ]}
      />
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
