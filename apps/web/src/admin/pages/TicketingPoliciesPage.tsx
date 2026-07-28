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
const ENTITLEMENT_TYPES = ["entry", "ride", "gift_drink", "bar", "amusement"];

// 组合式配置策略：酒吧权益种类（对应 entitlement_type）。gift_drink=酒水兑换，admission=入场资格。
const BAR_ENTITLEMENT_KINDS = [
  { value: "gift_drink", label: "酒水兑换" },
  { value: "admission", label: "入场资格" },
];

// 组合表单里的酒吧权益行。
type BarLine = { merchantId: string; kind: string; totalTimes: string; valueYuan: string };
// 餐饮权益行（用户2026-07-06 拍板）：选餐厅商户 + 价格(可0) + 折扣 + 内部结算价。
//   结算按【内部结算价】全额归餐厅商户(exclusive_merchant_id + agreement_price_cent，走 §6.4 不分摊直付)；
//   价格(面值,可0)与折扣仅为售卖/展示口径，不参与结算(核销结算价=内部结算价，不再把折扣当面值乱记)。
type DiningLine = { merchantId: string; valueYuan: string; discount: string; internalYuan: string };

function toRows<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  const items = (data as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

function yuan(cent: number | null): string {
  return cent == null ? "—" : (cent / 100).toFixed(2);
}

export function TicketingPoliciesPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId, navigateTo }: AdminPageProps) {
  const [tickets, setTickets] = React.useState<TicketTypeRow[]>([]);
  const [templates, setTemplates] = React.useState<EntitlementTemplateRow[]>([]);
  const [rules, setRules] = React.useState<PriceRuleRow[]>([]);
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
  const [barId, setBarId] = React.useState("");
  // AUD-015：建票=草稿→绑权益→带理由发布，一个向导内完成；createKey 固定一次，失败重试幂等复用同一草稿。
  const [createTemplateId, setCreateTemplateId] = React.useState("");
  const [publishReason, setPublishReason] = React.useState("");
  const [createKey, setCreateKey] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  // 引导式流程 phase-2：新建票种收起为主按钮，点击弹出分步引导（GuidedFlowModal）。
  const [createOpen, setCreateOpen] = React.useState(false);

  // 区2 新建权益模板
  const [tplForm, setTplForm] = React.useState({ name: "", entitlementType: "entry", totalTimes: "1", valueYuan: "" });
  // 区3 通票组装（票种绑权益）
  const [bindForm, setBindForm] = React.useState({ ticketTypeId: "", templateId: "", quantity: "1", totalTimes: "1", numPeople: "1" });
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
  const [diningLines, setDiningLines] = React.useState<DiningLine[]>([]);
  const [selectedProjects, setSelectedProjects] = React.useState<Record<string, boolean>>({});
  const [projectTimes, setProjectTimes] = React.useState("1");
  // 游乐权益也必须归属商户（后端 createEntitlementTemplate 强制 merchant_id 非空，否则结算成孤儿）。
  // /amusement/projects 不返回项目所属商户（merchantId 被 @JsonIgnore），故此处让运营选一个归属商户，全部游乐权益用它。
  const [amusementMerchantId, setAmusementMerchantId] = React.useState("");
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
        base_calendar_price_mode: "calendar",
        weekday_price_cent: Math.round(prices.平日价 * 100),
        weekend_price_cent: Math.round(prices.周末价 * 100),
        holiday_price_cent: Math.round(prices.节假日价 * 100),
        stock_quantity: stock.trim() ? Number(stock) : null,
        validity_days: Number(validityDays) || 1,
        refundable,
        bar_id: barId || null,
      });
      const ticketId = ticket.id ?? "ticket-1";
      await api.post(`/api/v1/ticketing/ticket-types/${ticketId}/entitlements`, {
        idempotency_key: `${createKey}-bind-${createTemplateId}`,
        entitlement_template_id: createTemplateId,
        quantity: 1,
        total_times: 1,
        num_people: 1,
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
    await api.post("/api/v1/ticketing/entitlement-templates", {
      idempotency_key: crypto.randomUUID(),
      name: tplForm.name.trim(),
      entitlement_type: tplForm.entitlementType,
      default_total_times: Number(tplForm.totalTimes) || 1,
      default_value_cent: tplForm.valueYuan.trim() ? Math.round(Number(tplForm.valueYuan) * 100) : null,
      status: "active",
    });
    setActionResult({ action: "entitlement_template_created", name: tplForm.name.trim() });
    setTplForm({ name: "", entitlementType: "entry", totalTimes: "1", valueYuan: "" });
    reload();
  }

  async function bindEntitlement() {
    if (!bindForm.ticketTypeId || !bindForm.templateId) {
      setGlobalError("请选择票种与权益模板");
      return;
    }
    await api.post(`/api/v1/ticketing/ticket-types/${bindForm.ticketTypeId}/entitlements`, {
      idempotency_key: crypto.randomUUID(),
      entitlement_template_id: bindForm.templateId,
      quantity: Number(bindForm.quantity) || 1,
      total_times: Number(bindForm.totalTimes) || 1,
      num_people: Number(bindForm.numPeople) || 1,
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
  function addBarLine() {
    setBarLines([...barLines, { merchantId: merchants[0]?.id ?? "", kind: "gift_drink", totalTimes: "1", valueYuan: "" }]);
  }
  function addDiningLine() {
    setDiningLines([...diningLines, { merchantId: merchants[0]?.id ?? "", valueYuan: "", discount: "", internalYuan: "" }]);
  }

  // 创建单条权益模板并绑定到目标通票。merchantId 必填（后端强制）。
  async function createAndBind(payload: {
    name: string;
    entitlementType: string;
    totalTimes: number;
    valueCent: number | null;
    merchantId: string;
    // 餐饮结算(用户2026-07-06)：内部结算价 + 专属餐厅商户(可选，仅餐饮传)。核销按内部结算价全额归餐厅。
    agreementPriceCent?: number | null;
    exclusiveMerchantId?: string | null;
  }, ticketTypeId: string) {
    const tpl = await api.post<{ id: string }>("/api/v1/ticketing/entitlement-templates", {
      idempotency_key: crypto.randomUUID(),
      name: payload.name,
      entitlement_type: payload.entitlementType,
      default_total_times: payload.totalTimes,
      default_value_cent: payload.valueCent,
      status: "active",
      merchant_id: payload.merchantId,
      agreement_price_cent: payload.agreementPriceCent ?? null,
      exclusive_merchant_id: payload.exclusiveMerchantId ?? null,
    });
    await api.post(`/api/v1/ticketing/ticket-types/${ticketTypeId}/entitlements`, {
      idempotency_key: crypto.randomUUID(),
      entitlement_template_id: tpl.id,
      quantity: 1,
      total_times: payload.totalTimes,
      num_people: 1,
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
          totalTimes: 1,
          // 价格(面值)可为 0：留空=0（纯赠送/兑换）。
          valueCent: line.valueYuan.trim() ? Math.round(Number(line.valueYuan) * 100) : 0,
          merchantId: line.merchantId,
          // 内部结算价(可空)：核销按此价结算归餐厅；留空则回退面值。
          agreementPriceCent: line.internalYuan.trim() ? Math.round(Number(line.internalYuan) * 100) : null,
          // 专属餐厅商户：使核销结算全额归该餐厅、不进池分摊。
          exclusiveMerchantId: line.merchantId,
        }, comboTicketId);
      }
      setActionResult({
        action: "pass_config_strategy_composed",
        ticketTypeId: comboTicketId,
        bars: barLines.length,
        amusementProjects: chosenProjects.length,
        dining: diningLines.length,
      });
      setBarLines([]);
      setSelectedProjects({});
      setDiningLines([]);
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
                <label>限量<input data-testid="ticket-stock" type="number" min={0} value={stock} onChange={(event) => setStock(event.target.value)} placeholder="限量（可空=不限）" aria-label="限量" /></label>
                <label>有效期（天）
                  <select data-testid="ticket-validity-days" aria-label="有效期（天）" value={validityDays} onChange={(event) => setValidityDays(event.target.value)}>
                    <option value="1">当日（1天）</option>
                    <option value="7">周票（7天）</option>
                    <option value="30">月票（30天）</option>
                    <option value="365">年票（365天）</option>
                  </select>
                </label>
                <label>
                  <input data-testid="ticket-refundable" type="checkbox" checked={refundable} onChange={(event) => setRefundable(event.target.checked)} aria-label="可退票" />
                  可退票（客人可申请整票退款、到期未用自动退；不勾=不可退）
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
        <select aria-label="配置通票" data-testid="combo-ticket-select" value={comboTicketId} onChange={(event) => setComboTicketId(event.target.value)}>
          <option value="">选择要配置的通票</option>
          {tickets.map((t) => (
            <option key={t.id} value={t.id}>{t.ticketName}</option>
          ))}
        </select>
      </div>

      <h4>酒吧权益（多选）</h4>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="combo-bar-table">
          <thead>
            <tr><th>酒吧/商户</th><th>权益</th><th>次数</th><th>面值（元）</th><th>操作</th></tr>
          </thead>
          <tbody>
            {barLines.map((line, index) => (
              <tr key={index} data-testid={`combo-bar-line-${index}`}>
                <td>
                  <select aria-label="酒吧" value={line.merchantId} onChange={(event) => setBarLines(barLines.map((l, i) => i === index ? { ...l, merchantId: event.target.value } : l))}>
                    <option value="">选择商户</option>
                    {merchants.map((m) => (<option key={m.id} value={m.id}>{m.merchantName}</option>))}
                  </select>
                </td>
                <td>
                  <select aria-label="权益" value={line.kind} onChange={(event) => setBarLines(barLines.map((l, i) => i === index ? { ...l, kind: event.target.value } : l))}>
                    {BAR_ENTITLEMENT_KINDS.map((k) => (<option key={k.value} value={k.value}>{k.label}</option>))}
                  </select>
                </td>
                <td><input aria-label="次数" type="number" min={1} value={line.totalTimes} onChange={(event) => setBarLines(barLines.map((l, i) => i === index ? { ...l, totalTimes: event.target.value } : l))} /></td>
                <td><input aria-label="面值（元）" type="number" min={0} step="0.01" value={line.valueYuan} placeholder="可空" onChange={(event) => setBarLines(barLines.map((l, i) => i === index ? { ...l, valueYuan: event.target.value } : l))} /></td>
                <td><button type="button" className="admin-secondary-button" onClick={() => setBarLines(barLines.filter((_, i) => i !== index))}>移除</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <button type="button" className="admin-secondary-button" data-testid="combo-add-bar" onClick={addBarLine} disabled={merchants.length === 0}>+ 添加酒吧权益</button>

      <h4>游乐设施（多选）</h4>
      <div className="admin-inline-form" data-testid="combo-amusement-list">
        {projects.length === 0 ? <span>暂无游乐设施</span> : projects.map((p) => (
          <label key={p.id} className="admin-checkbox" data-testid={`combo-project-${p.id}`}>
            <input type="checkbox" checked={!!selectedProjects[p.id]} onChange={(event) => setSelectedProjects({ ...selectedProjects, [p.id]: event.target.checked })} />
            {p.projectName}
          </label>
        ))}
        <input aria-label="游乐次数" type="number" min={1} value={projectTimes} onChange={(event) => setProjectTimes(event.target.value)} placeholder="游乐次数（每设施）" />
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
            <tr><th>餐厅商户</th><th>价格（元，可 0）</th><th>折扣（如 8=八折，选填）</th><th>内部结算价（元）</th><th>操作</th></tr>
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
                  <input aria-label="内部结算价（元）" type="number" min={0} step="0.01" value={line.internalYuan} placeholder="归餐厅的结算价" onChange={(event) => setDiningLines(diningLines.map((l, i) => i === index ? { ...l, internalYuan: event.target.value } : l))} />
                </td>
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
                <td>{formatBusinessStatus(row.status)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看权益模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="admin-inline-form" data-testid="entitlement-template-form">
        <input aria-label="模板名称" placeholder="模板名称（如 入园1次）" value={tplForm.name} onChange={(event) => setTplForm({ ...tplForm, name: event.target.value })} />
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
