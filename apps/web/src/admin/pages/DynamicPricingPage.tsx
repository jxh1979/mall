import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { CnDatePicker } from "../components/CnDatePicker";
import type { AdminPageProps, TicketDynamicPricingConditionRule, TicketDynamicPricingRuleRow, TicketDynamicPricingSimulationResult } from "../types";

// 后端写权限码（TicketingController：/dynamic-pricing-rules/{id}/publish）
const DYNAMIC_PUBLISH_PERMISSION = "ticket.dynamic_pricing.publish";

const TEXT = {
  title: "\u52a8\u6001\u5b9a\u4ef7",
  // 第十八批 AW 段 340-R1（PGM:287「必须全部为中文，不得出现任何英文」）：去掉 P2、P0。
  description: "动态定价用于规则草稿、价格仿真和发布审计；仍以平日/周末/节假日基础价为主。",
  loadFailed: "\u52a8\u6001\u5b9a\u4ef7\u89c4\u5219\u8bfb\u53d6\u5931\u8d25",
  actionFailed: "\u52a8\u6001\u5b9a\u4ef7\u64cd\u4f5c\u5931\u8d25",
  ruleRequired: "\u8bf7\u5148\u9009\u62e9\u8981\u53d1\u5e03\u7684\u89c4\u5219\u3002",
  nameRequired: "\u89c4\u5219\u540d\u79f0\u5fc5\u586b\u3002",
  ruleName: "\u89c4\u5219\u540d\u79f0",
  channel: "\u6e20\u9053",
  adjustmentRate: "调整比例（%，正数加价、负数减价）",
  adjustmentType: "调价方式",
  adjustmentAmount: "调整金额（元，正数加价、负数减价）",
  fixedPrice: "固定价（元）",
  visitDate: "\u5230\u8bbf\u65e5\u671f",
  createDraft: "\u521b\u5efa\u8349\u7a3f",
  simulate: "\u4ef7\u683c\u4eff\u771f",
  publish: "\u53d1\u5e03\u89c4\u5219",
  boundary: "\u4eff\u771f\u53ea\u8fd4\u56de\u8ba1\u7b97\u7ed3\u679c\uff0c\u4e0d\u521b\u5efa\u8ba2\u5355\u3001\u4e0d\u4fee\u6539\u5df2\u552e\u7968\u3001\u4e0d\u6539\u5199\u5e73\u65e5/\u5468\u672b/\u8282\u5047\u65e5\u57fa\u7840\u4ef7\u3002",
  ruleCount: "\u89c4\u5219\u6570",
  draftCount: "\u8349\u7a3f",
  publishedCount: "\u5df2\u53d1\u5e03",
  selectedRule: "\u9009\u4e2d\u89c4\u5219",
  finalPrice: "\u4eff\u771f\u5230\u624b\u4ef7",
  noSimulation: "\u672a\u4eff\u771f",
  select: "\u9009\u62e9",
  priority: "\u4f18\u5148\u7ea7",
  adjustment: "\u8c03\u6574",
  status: "\u72b6\u6001",
  effective: "\u751f\u6548\u671f",
  publishReason: "\u540e\u53f0\u53d1\u5e03",
  ticketType: "\u7968\u79cd",
  ticketTypeRequired: "\u8bf7\u9009\u62e9\u7968\u79cd\u3002",
  effectiveFrom: "\u751f\u6548\u5f00\u59cb",
  effectiveTo: "\u751f\u6548\u7ed3\u675f",
  ruleCountHelp: "\u89c4\u5219\u603b\u6570",
  draftHelp: "\u8349\u7a3f\u89c4\u5219",
  publishedHelp: "\u5df2\u53d1\u5e03\u89c4\u5219",
  focusClear: "\u6e05\u9664\u7b5b\u9009\uff0c\u8fd4\u56de\u5168\u90e8",
};

// KPI \u805a\u7126\u89c6\u56fe\uff08\u7167 OrganizationMerchantPage \u6837\u677f\uff09\uff1a\u70b9 KPI \u6570\u5b57=\u5e26 ?kpi= \u53c2\u6570\u91cd\u8fdb\u672c\u9875\uff0c\u53ea\u6e32\u67d3\u89c4\u5219\u8868\u5e76\u6309\u72b6\u6001\u5b50\u96c6\u8fc7\u6ee4\u3002
type KpiTarget = "draft" | "published";

function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target === "draft" || target === "published" ? target : null;
}

function formatAmount(amountCent: number | undefined) {
  if (amountCent === undefined) return TEXT.noSimulation;
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent) / 100);
}

function ruleLabel(rule: TicketDynamicPricingRuleRow | undefined) {
  return rule?.ruleName ?? rule?.id ?? "-";
}

// 第十八批 AT 段 335-R1（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生」）：调价方式页内映射，
//   键同后端取值（TicketPricingService：percent_delta、fixed_delta、fixed_price），属定价域专有词，不进全局码表。
//   第十八批 AW 段 338-R3：加「固定价」（BSM:782「固定额」，与派生价固定价同口径）。
const ADJUSTMENT_TYPE_LABELS: Record<string, string> = {
  percent_delta: "按比例",
  fixed_delta: "按固定金额",
  fixed_price: "固定价",
};

// 第十八批 AW 段 340-R1（PGM:287「必须全部为中文，不得出现任何英文」）：渠道改下拉，取值同后端 TicketPricingService.SALES_CHANNELS，
//   选项文字一律经 formatBusinessStatus 显示中文。
const SALES_CHANNEL_CODES = ["web", "miniapp", "app", "kiosk", "admin", "channel"];

// 动态价的调整：按比例写「加价/减价 x%」，按固定金额写「加价/减价 ¥x」（同后端复核单摘要）；票务策略页规则版本表共用。
export function dynamicAdjustment(d: { adjustmentType?: string; adjustmentRate?: number | string | null; adjustmentAmountCent?: number | null }): string {
  if (d.adjustmentType === "fixed_price") return `固定价 ¥${((d.adjustmentAmountCent ?? 0) / 100).toFixed(2)}`;
  if (d.adjustmentType === "fixed_delta") {
    const amount = d.adjustmentAmountCent ?? 0;
    return `${amount < 0 ? "减价" : "加价"} ¥${(Math.abs(amount) / 100).toFixed(2)}`;
  }
  const rate = Number(d.adjustmentRate ?? 0);
  return `${rate < 0 ? "减价" : "加价"} ${Math.abs(rate)}%`;
}

// 第十八批 BP 段第 359 条 359-R2、359-R3（BSM:782「动态价按时段、容量等规则配置」；第十八批裁决 75「到访当天已售满多少张起」）：条件文字同后端复核单摘要
//   （TicketDynamicPricingConditions.describe）——「时段 20:00～22:00」、跨零点「时段 22:00～次日 02:00」、「到访当天已售满 50 张起」，无条件为空串；
//   票务策略页规则版本表共用。
export function dynamicConditions(c?: TicketDynamicPricingConditionRule | null): string {
  const parts: string[] = [];
  if (c?.timeFrom && c.timeTo) parts.push(`时段 ${c.timeFrom}～${c.timeTo < c.timeFrom ? "次日 " : ""}${c.timeTo}`);
  if (c?.daySoldMin) parts.push(`到访当天已售满 ${c.daySoldMin} 张起`);
  return parts.join(" · ");
}

// 第 229 条 229-R7：票种下拉项（GET /api/v1/ticketing/ticket-types 的元素，本页只用 id 与票名）。
type TicketTypeOption = { id: string; ticketName?: string };

export function DynamicPricingPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rules, setRules] = React.useState<TicketDynamicPricingRuleRow[]>([]);
  const { selectedId: selectedRuleId, setSelectedId: setSelectedRuleId, selected: selectedRule } = useSelectedRow(rules);
  const [ruleName, setRuleName] = React.useState("周末上浮");
  const [salesChannel, setSalesChannel] = React.useState("web");
  // 340-R1：试算渠道单独一个值——后端试算渠道必填，新建规则的渠道可空（空＝全部渠道），两者不共用。
  const [simulateChannel, setSimulateChannel] = React.useState("web");
  const [adjustmentRate, setAdjustmentRate] = React.useState("10");
  const [adjustmentType, setAdjustmentType] = React.useState("percent_delta");
  const [adjustmentYuan, setAdjustmentYuan] = React.useState("");
  // 按固定金额与按固定价都只带金额（338-R3）。
  const fixedAmount = adjustmentType === "fixed_delta" || adjustmentType === "fixed_price";
  const [visitDate, setVisitDate] = React.useState("");
  const [ticketTypes, setTicketTypes] = React.useState<TicketTypeOption[]>([]);
  const [ticketTypeId, setTicketTypeId] = React.useState("");
  const [effectiveFrom, setEffectiveFrom] = React.useState("");
  const [effectiveTo, setEffectiveTo] = React.useState("");
  // 359-R2、359-R3：条件都可空（时段空＝全天，已售空＝不论）；只在填了时随建规则发出。试算可指定试算时刻与当天已售张数（空＝此刻、按实际统计）。
  const [timeFrom, setTimeFrom] = React.useState("");
  const [timeTo, setTimeTo] = React.useState("");
  const [daySoldMin, setDaySoldMin] = React.useState("");
  const [simulateTime, setSimulateTime] = React.useState("");
  const [simulateDaySold, setSimulateDaySold] = React.useState("");
  const conditionRule: TicketDynamicPricingConditionRule = {
    ...(timeFrom && timeTo ? { timeFrom, timeTo } : {}),
    ...(daySoldMin.trim() ? { daySoldMin: Number(daySoldMin) } : {}),
  };
  const [simulation, setSimulation] = React.useState<TicketDynamicPricingSimulationResult | null>(null);
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该动态定价规则完整字段（渠道/状态中文·生效期中文日期）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-3：新建定价规则（创建草稿）收起为主按钮，点击弹出分步引导；仿真/发布仍留原表单区（对已存在规则的操作，非创建流）。
  const [createOpen, setCreateOpen] = React.useState(false);

  function openRuleDetail(rule: TicketDynamicPricingRuleRow) {
    setDetail({
      title: `动态定价规则明细 · ${rule.ruleName ?? rule.id}`,
      fields: [
        { label: "规则名称", value: rule.ruleName ?? rule.id },
        { label: "渠道", value: formatBusinessStatus(rule.salesChannel) },
        { label: "优先级", value: rule.priority ?? "—" },
        { label: "调整", value: dynamicAdjustment(rule) },
        { label: "状态", value: formatBusinessStatus(rule.status) },
        { label: "生效期", value: `${cnDate(rule.effectiveFrom)} ~ ${cnDate(rule.effectiveTo)}` },
        { label: "条件", value: dynamicConditions(rule.conditionRule) || "无（全天、不论已售张数）" },
        { label: "规则编号", value: rule.id },
      ],
    });
  }

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(target);
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

  const { loading, error, reload: reloadResource } = useAdminResource(
    async () => {
      // 票种与规则同读；票种读失败回空列表，不挡规则列表（真实接口回分页信封，两种形状都认）。
      const [ruleRows, typeRows] = await Promise.all([
        api.get<TicketDynamicPricingRuleRow[]>("/api/v1/ticketing/dynamic-pricing-rules"),
        api.get<TicketTypeOption[] | { items?: TicketTypeOption[] }>("/api/v1/ticketing/ticket-types?includeArchived=false").catch(() => []),
      ]);
      setRules(ruleRows);
      setTicketTypes(Array.isArray(typeRows) ? typeRows : typeRows?.items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const draftCount = rules.filter((rule) => rule.status === "draft").length;
  const publishedCount = rules.filter((rule) => rule.status === "published").length;
  // 聚焦时对规则表做客户端筛选：仅过滤已加载的内存数组，不发起后端查询。
  const visibleRules = kpiFocus ? rules.filter((rule) => rule.status === kpiFocus) : rules;

  async function runAction(action: string, request: () => Promise<unknown>, reload = true) {
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await request();
      setActionResult({ action, result });
      if (reload) await reloadResource();
      return result;
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, status: "failed", message });
      return null;
    } finally {
      setBusyAction("");
    }
  }

  async function createDraft() {
    if (!ruleName.trim()) {
      setFormError(TEXT.nameRequired);
      return;
    }
    const result = await runAction("ticket_dynamic_rule_created", () => api.post("/api/v1/ticketing/dynamic-pricing-rules", {
      ticketTypeId,
      ruleName,
      priority: 10,
      ...(effectiveFrom ? { effectiveFrom } : {}),
      ...(effectiveTo ? { effectiveTo } : {}),
      salesChannel,
      adjustmentType,
      ...(fixedAmount ? { adjustmentAmountCent: Math.round(Number(adjustmentYuan) * 100) } : { adjustmentRate: Number(adjustmentRate) }),
      ...(Object.keys(conditionRule).length ? { conditionRule } : {}),
    }));
    if (result) setCreateOpen(false);
  }

  async function simulate() {
    const context = { ...(simulateTime ? { atTime: simulateTime } : {}), ...(simulateDaySold.trim() ? { daySold: Number(simulateDaySold) } : {}) };
    const result = await runAction("ticket_dynamic_pricing_simulated", () => api.post<TicketDynamicPricingSimulationResult>("/api/v1/ticketing/dynamic-pricing/simulations", {
      ticketTypeId: selectedRule?.ticketTypeId,
      ...(visitDate ? { visitDate } : {}),
      salesChannel: simulateChannel,
      ...(Object.keys(context).length ? { context } : {}),
    }), false);
    if (result) setSimulation(result as TicketDynamicPricingSimulationResult);
  }

  function validatePublish() {
    if (!selectedRule) {
      setFormError(TEXT.ruleRequired);
      return false;
    }
    return true;
  }

  async function publishRule() {
    if (!selectedRule) {
      setFormError(TEXT.ruleRequired);
      return;
    }
    const result = await runAction("ticket_dynamic_rule_published", () => api.post(`/api/v1/ticketing/dynamic-pricing-rules/${selectedRule.id}/publish`, {
      action: "publish",
      reason: TEXT.publishReason,
    }));
    // 第十八批 AS 段 297-R4（第十八批裁决 63、61）：已上架跨域组合通票的动态价只存进规则草稿版（后端回 draft），提交审批获批后生效。
    if ((result as { status?: string } | null)?.status === "draft") {
      setActionResult({ action: "ticket_dynamic_rule_staged",
        note: "这张票是已上架的未挂酒吧的票：动态价已存入规则草稿版，旧版照常售卖；请到票务策略② 组合式套票配置提交审批，获批后生效" });
    }
  }

  return (
    <section data-testid="dynamic-pricing-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AdminDataState loading={loading} error={error} empty={rules.length === 0} onRetry={reloadResource}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="dynamic-pricing-kpi-focus-banner">
            正在查看：{formatBusinessStatus(kpiFocus)}（{visibleRules.length}）
            <button type="button" data-testid="dynamic-pricing-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="dynamic-pricing-summary">
          <div className="admin-kpi-card"><span>{TEXT.ruleCount}</span><strong>{rules.length}</strong><small>{TEXT.ruleCountHelp}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.draftCount}</span><button type="button" className="admin-kpi-value" data-testid="dynamic-pricing-kpi-draft" onClick={() => enterKpiFocus("draft")}>{draftCount}</button><small>{TEXT.draftHelp}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.publishedCount}</span><button type="button" className="admin-kpi-value" data-testid="dynamic-pricing-kpi-published" onClick={() => enterKpiFocus("published")}>{publishedCount}</button><small>{TEXT.publishedHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedRule}</span><strong className="admin-kpi-value--text" title={ruleLabel(selectedRule)}>{ruleLabel(selectedRule)}</strong><small>{formatBusinessStatus(selectedRule?.salesChannel ?? salesChannel)}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.finalPrice}</span><strong>{formatAmount(simulation?.finalPriceCent)}</strong><small>{simulation?.matchedRuleId ?? TEXT.noSimulation}</small></div>
        </div>

        <div className="admin-form-row" data-testid="dynamic-pricing-form">
          {/* 引导式流程 phase-3：新建定价规则入口按钮（权限门仍由发布环节的 HighRiskConfirm 把守；创建草稿为低风险操作）。 */}
          <button type="button" data-testid="dynamic-pricing-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ 新建定价规则</button>
          <select data-testid="dynamic-pricing-channel" aria-label={TEXT.channel} value={simulateChannel} onChange={(event) => setSimulateChannel(event.target.value)}>{SALES_CHANNEL_CODES.map((code) => <option key={code} value={code}>{formatBusinessStatus(code)}</option>)}</select>
          <input data-testid="dynamic-pricing-visit-date" value={visitDate} onChange={(event) => setVisitDate(event.target.value)} placeholder={TEXT.visitDate} />
          <label><span>试算时刻（空＝此刻）</span><input type="time" data-testid="dynamic-pricing-simulate-time" aria-label="试算时刻" title="试算时刻（空＝此刻）" value={simulateTime} onChange={(event) => setSimulateTime(event.target.value)} /></label>
          <input type="number" min={0} step={1} data-testid="dynamic-pricing-simulate-day-sold" aria-label="试算当天已售张数" value={simulateDaySold} onChange={(event) => setSimulateDaySold(event.target.value)} placeholder="试算当天已售张数（空＝按实际）" />
          <button type="button" data-testid="dynamic-pricing-simulate" disabled={busyAction !== ""} onClick={() => void simulate()}>{TEXT.simulate}</button>
          <HighRiskConfirm
            label={TEXT.publish}
            impact="将发布选中动态定价规则并写入审计留痕，规则生效后影响对应渠道/日期的售价计算。"
            disabled={busyAction !== ""}
            precheck={validatePublish}
            onConfirm={() => void publishRule()}
            permissionsState={effectivePermissions}
            permissions={[DYNAMIC_PUBLISH_PERMISSION]}
            data-testid="dynamic-pricing-publish"
          />
        </div>
        <GuidedFlowModal
          open={createOpen}
          title="新建定价规则"
          testId="dynamic-pricing-guided"
          submitTestId="dynamic-pricing-create"
          submitLabel={TEXT.createDraft}
          submitting={busyAction !== ""}
          error={formError}
          onClose={() => setCreateOpen(false)}
          onSubmit={() => void createDraft()}
          steps={[
            {
              title: "基本信息",
              description: "选择票种，填写规则名称与适用渠道。",
              validate: () => (!ticketTypeId ? TEXT.ticketTypeRequired : ruleName.trim() ? null : TEXT.nameRequired),
              content: (
                <>
                  <label><span>{TEXT.ticketType}</span><select data-testid="dynamic-pricing-ticket-type" value={ticketTypeId} onChange={(event) => setTicketTypeId(event.target.value)}><option value="">请选择票种</option>{ticketTypes.map((type) => <option key={type.id} value={type.id}>{type.ticketName ?? type.id}</option>)}</select></label>
                  <label><span>{TEXT.ruleName}</span><input data-testid="dynamic-pricing-rule-name" value={ruleName} onChange={(event) => setRuleName(event.target.value)} placeholder={TEXT.ruleName} /></label>
                  <label><span>{TEXT.channel}</span><select data-testid="dynamic-pricing-guided-channel" value={salesChannel} onChange={(event) => setSalesChannel(event.target.value)}><option value="">全部渠道</option>{SALES_CHANNEL_CODES.map((code) => <option key={code} value={code}>{formatBusinessStatus(code)}</option>)}</select></label>
                </>
              ),
            },
            {
              title: "调价",
              description: "按比例或按固定金额调整（正数加价、负数减价），或按固定价（命中即卖这个价）；生效期可空，不填开始为当天，不填结束同开始日。"
                + "时段可空（空＝全天），结束早于开始表示跨零点（如 22:00～02:00），按客人下单时刻判断；到访当天已售满多少张起可空，按该单到访日已卖出的张数判断。",
              validate: () => (fixedAmount
                ? (adjustmentYuan.trim() && Number.isFinite(Number(adjustmentYuan)) ? null : "调整金额必须为数字。")
                : (Number.isFinite(Number(adjustmentRate)) ? null : "调整比例必须为数字。"))
                ?? ((timeFrom === "") !== (timeTo === "") ? "时段开始与结束须同时填写。" : null),
              content: (
                <>
                  <label><span>{TEXT.adjustmentType}</span><select data-testid="dynamic-pricing-adjustment-type" value={adjustmentType} onChange={(event) => setAdjustmentType(event.target.value)}>{Object.entries(ADJUSTMENT_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  {fixedAmount
                    ? <label><span>{adjustmentType === "fixed_price" ? TEXT.fixedPrice : TEXT.adjustmentAmount}</span><input data-testid="dynamic-pricing-adjustment-amount" value={adjustmentYuan} onChange={(event) => setAdjustmentYuan(event.target.value)} placeholder={adjustmentType === "fixed_price" ? TEXT.fixedPrice : TEXT.adjustmentAmount} /></label>
                    : <label><span>{TEXT.adjustmentRate}</span><input data-testid="dynamic-pricing-adjustment-rate" value={adjustmentRate} onChange={(event) => setAdjustmentRate(event.target.value)} placeholder={TEXT.adjustmentRate} /></label>}
                  <label><span>{TEXT.effectiveFrom}</span><CnDatePicker data-testid="dynamic-pricing-effective-from" value={effectiveFrom} onChange={setEffectiveFrom} /></label>
                  <label><span>{TEXT.effectiveTo}</span><CnDatePicker data-testid="dynamic-pricing-effective-to" value={effectiveTo} onChange={setEffectiveTo} /></label>
                  <label><span>时段开始</span><input type="time" data-testid="dynamic-pricing-time-from" value={timeFrom} onChange={(event) => setTimeFrom(event.target.value)} /></label>
                  <label><span>时段结束</span><input type="time" data-testid="dynamic-pricing-time-to" value={timeTo} onChange={(event) => setTimeTo(event.target.value)} /></label>
                  <label><span>到访当天已售满（张）起</span><input type="number" min={1} step={1} data-testid="dynamic-pricing-day-sold-min" value={daySoldMin} onChange={(event) => setDaySoldMin(event.target.value)} placeholder="可空" /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: TEXT.ticketType, value: ticketTypes.find((type) => type.id === ticketTypeId)?.ticketName ?? (ticketTypeId || "未填写") },
            { label: TEXT.ruleName, value: ruleName.trim() || "未填写" },
            { label: TEXT.channel, value: salesChannel ? formatBusinessStatus(salesChannel) : "全部渠道" },
            { label: TEXT.adjustment, value: dynamicAdjustment({ adjustmentType, adjustmentRate, adjustmentAmountCent: Math.round(Number(adjustmentYuan) * 100) }) },
            { label: TEXT.effectiveFrom, value: effectiveFrom ? cnDate(effectiveFrom) : "未填写" },
            { label: TEXT.effectiveTo, value: effectiveTo ? cnDate(effectiveTo) : "未填写" },
            { label: "条件", value: dynamicConditions(conditionRule) || "无（全天、不论已售张数）" },
          ]}
        />
        {formError ? <div data-testid="dynamic-pricing-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="dynamic-pricing-table">
            <thead>
              <tr><th>{TEXT.select}</th><th>{TEXT.ruleName}</th><th>{TEXT.channel}</th><th>{TEXT.priority}</th><th>{TEXT.adjustment}</th><th>{TEXT.status}</th><th>{TEXT.effective}</th><th>详情</th></tr>
            </thead>
            <tbody>
              {visibleRules.map((rule) => (
                <tr key={rule.id} className="admin-row-clickable" tabIndex={0} data-testid={`dynamic-pricing-row-${rule.id}`}
                  onClick={() => openRuleDetail(rule)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRuleDetail(rule); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此定价规则 ${rule.id}`} data-testid={`dynamic-pricing-select-${rule.id}`} checked={(selectedRule?.id ?? "") === rule.id} onChange={() => setSelectedRuleId(rule.id)} /></td>
                  <td>{rule.ruleName ?? rule.id}</td>
                  <td>{formatBusinessStatus(rule.salesChannel)}</td>
                  <td>{rule.priority ?? "-"}</td>
                  <td>{dynamicAdjustment(rule)}</td>
                  <td>{formatBusinessStatus(rule.status)}</td>
                  <td>{cnDate(rule.effectiveFrom)} ~ {cnDate(rule.effectiveTo)}{dynamicConditions(rule.conditionRule) ? ` · ${dynamicConditions(rule.conditionRule)}` : ""}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看动态定价规则详情" onClick={(event) => { event.stopPropagation(); openRuleDetail(rule); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
