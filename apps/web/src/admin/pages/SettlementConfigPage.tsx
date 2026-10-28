// 文件版本：V0.04
import React from "react";
import { Tabs } from "antd";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal, type GuidedStep, type GuidedSummaryItem } from "../components/GuidedFlowModal";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps, BusinessEntityRow } from "../types";
import { InternalPayDaysPanel } from "./InternalPayDaysPanel";
import { DistributionSchemePanel } from "./DistributionSchemePanel";

// 结算配置中心（新立项）总控页 —— PRD settlement-rebuild §9（ADM-STL-001）。
// 批0：①「看全局」当前生效配置一览；②草案待复核队列 + 复核转正。
// 批1（本文件）：加「通票分账」栏目（antd Tabs 外层）——8 类配置块，每块=当前生效值 + GuidedFlowModal「改」落草案；
//   + 集中的待复核草案队列（转正泛化 /{configType}/{id}/activate；软删仅 merchant_rate/revenue_stream_attribution）。
// 一切写动作走 settlement.config.manage + idempotency_key；转正/软删走后端「差人复核」（自己起草的不能自己转正）。
const MANAGE = ["settlement.config.manage"];
const BASE = "/api/v1/settlement/config";

type ConfigItem = { group: string; key: string; label: string; value: string };
type DraftItem = { id: string; configGroup: string; summary: string };
type Overview = { platformDefaults: ConfigItem[]; pendingDraftCount: number; pendingDrafts: DraftItem[] };

// 通票分账配置块 + V340 提成/积分 2 类（configGroup 即转正泛化 configType，rank_tiers 例外走 /rank-tiers/activate）。
// 第十八批第 2 条：原「排名档位分账梯子」「商户每客单价 / 固定额」两块已合并为通票分账方案（DistributionSchemePanel），引擎不再读旧表。
type EditKey =
  | "distribution_method"
  | "comprehensive_service_fee"
  | "pass_gift_cost"
  | "combo_discount"
  | "channel_fee"
  | "revenue_stream_attribution"
  | "commission_rate"
  | "points_accrual_rate";

type BlockDef = { key: EditKey; title: string; defaultKey?: string; note?: string; endpoint: string };

// 各块：标题 + 当前生效值取自 overview.platformDefaults 的哪个 key（无则块内提示按域/按商户维护）。
const BLOCKS: BlockDef[] = [
  { key: "distribution_method", title: "各酒吧分配方式（第二段）", defaultKey: "distribution_method", endpoint: "distribution-method", note: "两段分配：第一段名单内的酒吧每份票按固定金额或固定单价先拿（不再参与第二段）；其余覆盖酒吧按下方通票分账方案的参与家数档位表分第二段。此处配分配方式与短期经营激励单次有效期上限（按项目可配，出厂 3 个月，须同时配置上下限）。" },
  { key: "comprehensive_service_fee", title: "门票综合服务费", defaultKey: "comprehensive_service_fee", endpoint: "comprehensive-service-fee", note: "按「项目 × 商户 × 票种」配置（商户 / 票种留空 = 通用）。即娱乐平台公司留存 / 项目管理费——与原「项目公司留存比例」为同一项、只扣一次，原留存卡片已并入本费率。" },
  { key: "pass_gift_cost", title: "赠送权益预估成本", defaultKey: "pass_gift_cost", endpoint: "pass-gift-cost", note: "通票分账在进入可分配池之前预扣的一笔「赠送权益预估成本」，归娱乐平台公司。按「项目 × 票种」配置（票种留空 = 通用）：按件计 = 票面各赠送权益数量 × 提供方内部结算价之和（金额由票面推导，无需手填）；按票计 = 该票种赠送部分的售价整额。商户让利型权益（在票种配置里标明）不进预扣。改动落草案、复核转正后生效。" },
  { key: "combo_discount", title: "套票折扣归属 + 通票凑整允许误差", defaultKey: "combo_discount_bearer", endpoint: "combo-discount", note: "套票整体打折时，折扣由项目公司还是商户承担；同处再配「通票凑整允许误差」——运营给通票填的售价与系统按各业态价格×折扣算出的售价，差额超过这个数的票一律不许开卖（凑整多出/少收的那点钱走的正是上面这条折扣承担口径）。" },
  { key: "channel_fee", title: "渠道费用（通路费／销售渠道费）", endpoint: "channel-fee", note: "两层分别配、不可混用：通路费按支付通路（微信、支付宝、聚合支付、银联及第三方回款通路，填通路码）配比例，所有付款都扣、官方渠道同样扣；销售渠道费按第三方销售渠道（美团、抖音等，填渠道码，留空＝第三方通用）配比例或每张票固定金额，只有第三方渠道售出的票才扣、官方渠道不扣。两层都在拆分各业态之前从整单扣、由各业态按份额共担。改动落草案。" },
  { key: "revenue_stream_attribution", title: "收入流业态归因", endpoint: "revenue-stream-attribution", note: "六业态收入流 → 管理公司 / 业态主体，改动落草案；可对草案「下线」。" },
];

// V340（P0-D）：提成与积分两类原本写死在代码里的口径，收进本页统一维护（「业务策略一律可配、零硬编码全后台可改」）。
const COMMISSION_POINTS_BLOCKS: BlockDef[] = [
  {
    key: "commission_rate",
    title: "卡座销售提成费率",
    endpoint: "commission-rate",
    note: "按「项目 × 商户 × 角色档」配置提成率（万分比）；商户留空 = 该项目通用。出厂默认：销售人员 15%（1500）、非销售人员 10%（1000）。",
  },
  {
    key: "points_accrual_rate",
    title: "积分累计率",
    endpoint: "points-accrual-rate",
    note: "按「项目 × 商户」配置每积 1 分所需的消费金额（分）；商户留空 = 通用。出厂默认 100 = 每消费 1 元积 1 分（会员等级倍率、生日倍率另在会员配置叠加）。",
  },
];

const ALL_BLOCKS: BlockDef[] = [...BLOCKS, ...COMMISSION_POINTS_BLOCKS];

const ROLE_TYPE_OPTIONS: [string, string][] = [
  ["sales", "销售人员"],
  ["non_sales", "非销售人员"],
];

// S8 两段分配（2026-07-28）：可分配池先由「商户每客单价 / 固定额」卡片里配了固定规则的酒吧按绝对金额拿第一段，
//   余额由其余酒吧按排名档位分尽第二段——「按每客单价」「按固定额」已是商户级固定规则，不再是项目级方式选项，
//   本下拉只剩第二段的分配方式。
const METHOD_OPTIONS: [string, string][] = [
  ["by_rank_tier", "按销售排名档位（第二段余额分尽）"],
];
// V368：短期经营激励有效期的单位。运营配「三天的短期激励」和「三个月的短期激励」是同一个动作——
//   同一个输入框填数值，旁边选单位，不用心算 90 天。
const TERM_UNIT_OPTIONS: [string, string][] = [
  ["day", "天"],
  ["month", "月"],
];
// S6：赠送权益预扣两模式（业务系统地图「通票收入分账全流程」原文口径）。
const PREDEDUCT_MODE_OPTIONS: [string, string][] = [
  ["by_ticket_amount", "按票计（该票种赠送部分的售价整额）"],
  ["by_item", "按件计（票面各权益数量 × 内部结算价之和）"],
];
// 18B 复核后裁决第 11 条「对，下线两个选项（推荐）」：预扣与实付的差额一律留娱乐平台公司（ENUM 第 52 行），不再可选。
const VARIANCE_FIXED_LABEL = "差额留娱乐平台公司";
// 第十八批第 3 条（BSM 第 749 行「渠道费用分两层、性质不同不可混用」）。
const FEE_LAYER_OPTIONS: [string, string][] = [
  ["route", "通路费（按支付通路）"],
  ["sales", "销售渠道费（按第三方销售渠道）"],
];
const BEARER_OPTIONS: [string, string][] = [
  ["platform_absorb", "平台承担"],
  ["merchant_absorb", "商户承担"],
  ["split_by_ratio", "按比例分摊"],
];
// V355：收入流 6 → 9，名称按业务速查表「六业态收入归属」与系统地图「通票收入分账全流程」逐字对齐。
const REVENUE_STREAM_OPTIONS: [string, string][] = [
  ["all_pass_ticket", "通票收入"],
  ["ticket_service_fee", "门票综合服务费"],
  ["pass_gift_cost", "通票赠送权益预估成本"],
  ["table_dining", "订台低消 / 二次消费 / 额外酒水"],
  ["bar_service_fee", "非散台服务费（包厢 / 卡座）"],
  ["supply_goods", "向商户供货的货款"],
  ["amusement_ticket", "游乐票与游乐核销收入"],
  ["performance_ticket", "演出票"],
  ["venue_rent", "场地租金 / 商业租金"],
];
// 每条收入流「按业务约定该归谁」的口径提示（照抄业务口径，防止运营配反了钱的去向）。
const REVENUE_STREAM_HINT: Record<string, string> = {
  all_pass_ticket: "业务约定：归「娱乐平台公司」。",
  ticket_service_fee: "业务约定：归「娱乐平台公司」（即项目管理费 / 娱乐平台公司留存，同一项只扣一次）。",
  pass_gift_cost: "业务约定：归「娱乐平台公司」（票里加赠权益的预估成本，先扣后与提供商户结算）。",
  table_dining: "业务约定：归「各酒吧商户自己」→ 收款方类型请选「各商户自己」。",
  bar_service_fee: "业务约定：归「各酒吧商户自己」→ 收款方类型请选「各商户自己」。",
  supply_goods: "业务约定：归「中央餐饮公司」（供应链主体，向商户供货收货款）。",
  amusement_ticket: "业务约定：归「游乐公司」（游乐票与游乐核销收入统一归此主体，不分给各商户）。",
  performance_ticket: "业务约定：归「传媒公司」。",
  venue_rent: "业务约定：归「长沙项目公司」（资产公司即长沙项目公司，资产业态不单设主体）。",
};
// 收款方类型：有些收入流没有单一收款主体，钱归产生它的各商户自己（订台低消、非散台服务费）。
const BENEFICIARY_KIND_OPTIONS: [string, string][] = [
  ["entity", "某个经营主体"],
  ["per_merchant", "各商户自己"],
];
// configGroup → 中文（草案队列展示用；未列出的原样显示）。
const GROUP_LABELS: Record<string, string> = {
  distribution_method: "分配方式",
  rank_tiers: "排名档位",
  merchant_rate: "商户每客/固定额",
  comprehensive_service_fee: "综合服务费",
  pass_gift_cost: "赠送权益预估成本",
  combo_discount: "套票折扣归属",
  channel_fee: "渠道费用",
  revenue_stream_attribution: "收入流归因",
  attribution_rule: "收入归因规则",
  commission_rate: "卡座提成费率",
  points_accrual_rate: "积分累计率",
  // V350：全平台「高价值物品」判定阈值（退酒/报损/盘点差异/存酒作废共用一个值，比的是物品进货价）。
  high_value_threshold: "高价值判定阈值(进货价)",
  // V346（P0-E）：营业额抽成规则在「合同工作台」填写并落草案，在此复核转正后才对结算生效。
  turnover_rent_rule: "营业额抽成规则",
  // 批10/E：内部结算 SKU 目录条目在「内部结算价目录」页维护并落草案，此队列可直接复核转正。
  internal_price_item: "内部结算价目录条目",
};
// 支持「下线」（软删）的草案类型，对齐后端 archive 白名单。
// V340 提成/积分：商户级特例条目须可下线，否则该商户被特例永久遮挡、回落不到项目/全局默认口径。
// V350 高价值判定阈值：商户级特例条目同理可下线（回落项目/全局默认），全局默认种子由后端守卫拦住。
// V346 抽成规则：合同终止/换规则时须可下线，下线后该合同不再产生抽成应收。
// S6 赠送权益预扣：项目/票种特例条目可下线（回落上层默认），全局默认种子由后端守卫拦住。
const ARCHIVABLE = new Set([
  "merchant_rate",
  "revenue_stream_attribution",
  "commission_rate",
  "points_accrual_rate",
  "high_value_threshold",
  "turnover_rent_rule",
  "pass_gift_cost",
  // 批10/E：目录条目草案可直接下线（弃草案）；生效条目的下线归档在「内部结算价目录」页做。
  "internal_price_item",
]);

// S2：票种下拉项（/api/v1/ticketing/ticket-types 分页信封的 items 元素，本页只用 id + 票名）。
type TicketTypeOption = { id: string; ticketName?: string; status?: string };
type Form = {
  projectId: string;
  methodType: string;
  specialApprovalTermValue: string;
  specialApprovalTermUnit: string;
  // 第 75 条：有效期上限这一参数的下限、上限（同一单位），与数值、单位四项同时必填。
  specialApprovalTermMinValue: string;
  specialApprovalTermMaxValue: string;
  merchantId: string;
  perGuestCent: string;
  fixedAmountCent: string;
  monthlyCapAmountCent: string;
  ticketTypeId: string;
  // 渠道费用：费用层（route 通路费 / sales 销售渠道费）+ 渠道码（支付通路码或第三方销售渠道码）。
  feeLayer: string;
  channel: string;
  feeBps: string;
  discountBearer: string;
  splitPlatformBps: string;
  // V369：综合／联合通票凑整允许误差（元，提交时换算成分）。
  roundingToleranceYuan: string;
  revenueStream: string;
  managementEntityId: string;
  beneficiaryKind: string;
  roleType: string;
  rateBp: string;
  amountCentPerPoint: string;
  // S6 赠送权益预估成本：预扣模式 + 按票计整额（元，提交时换算成分）。
  predeductMode: string;
  ticketGiftAmountYuan: string;
};
const EMPTY_FORM: Form = {
  projectId: "", methodType: "by_rank_tier",
  specialApprovalTermValue: "3", specialApprovalTermUnit: "month", specialApprovalTermMinValue: "", specialApprovalTermMaxValue: "",
  merchantId: "",
  perGuestCent: "", fixedAmountCent: "", monthlyCapAmountCent: "", ticketTypeId: "", feeLayer: "route", channel: "", feeBps: "",
  discountBearer: "platform_absorb", splitPlatformBps: "0", roundingToleranceYuan: "5", revenueStream: "all_pass_ticket", managementEntityId: "",
  beneficiaryKind: "entity",
  roleType: "sales", rateBp: "", amountCentPerPoint: "",
  predeductMode: "by_ticket_amount", ticketGiftAmountYuan: "",
};

function nextKey(prefix: string) {
  return `stl-${prefix}-${Date.now()}`;
}
// 空 = 全局默认（后端 project_id 须为 NULL 才与种子默认行同 scope；空串会当成独立 scope，故省略字段）。
function orUndef(value: string): string | undefined {
  const t = value.trim();
  return t === "" ? undefined : t;
}
function isIntInRange(value: string, min: number, max: number): boolean {
  const t = value.trim();
  if (!/^-?\d+$/.test(t)) return false;
  const n = Number(t);
  return n >= min && n <= max;
}

// 草案 summary 里解析 project（rank_tiers 转正按 project 换梯，overview 未单列 projectId，只能从 summary 取）。
// ponytail: 依赖后端 summary 文案格式（'project=<id> ...'），后端改文案需同步；若 DraftItem 加 projectId 字段可去掉此解析。
function projectFromSummary(summary: string): string | undefined {
  const m = /project=(\S+)/.exec(summary);
  if (!m || m[1] === "__DEFAULT__") return undefined;
  return m[1];
}

export function SettlementConfigPage({ api, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<Overview | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState<string | null>(null);

  const [editing, setEditing] = React.useState<EditKey | null>(null);
  const [form, setForm] = React.useState<Form>(EMPTY_FORM);
  const [formError, setFormError] = React.useState<string>("");

  // 归因主体下拉数据源。该端点要 entity.binding.view（与本页 settlement.config.manage 是两个权限），
  // 缺权限就别请求（403 会污染整页），降级成手填主体 id —— 与改造前行为一致。
  const [entities, setEntities] = React.useState<BusinessEntityRow[]>([]);
  const canEntityView = hasPermissions(effectivePermissions, ["entity.binding.view"]);

  // S2：综合服务费费率按【票种】配置，票种下拉数据源。该端点要 ticket.product.view（与本页 settlement.config.manage
  // 是两个权限），缺权限就别请求（403 会污染整页），降级成手填票种 id —— 同上面主体下拉的处理。
  const [ticketTypes, setTicketTypes] = React.useState<TicketTypeOption[]>([]);
  const canTicketTypeView = hasPermissions(effectivePermissions, ["ticket.product.view"]);

  const reload = React.useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Overview>(`${BASE}/overview`)
      .then((d) => { setData(d); setLoading(false); })
      .catch((e: unknown) => { setError((e as Error)?.message ?? "结算配置总览读取失败"); setLoading(false); });
  }, [api]);

  React.useEffect(() => { reload(); }, [reload]);
  React.useEffect(() => {
    if (!canEntityView) return;
    api.get<BusinessEntityRow[]>("/api/v1/platform/business-entities")
      .then((rows) => setEntities(Array.isArray(rows) ? rows.filter((r) => r.status !== "inactive") : []))
      .catch(() => setEntities([]));  // 主体列表拿不到不阻断配置页，回落手填
  }, [api, canEntityView]);
  React.useEffect(() => {
    if (!canTicketTypeView) return;
    api.get<{ items?: TicketTypeOption[] }>("/api/v1/ticketing/ticket-types?includeArchived=false")
      .then((page) => setTicketTypes(Array.isArray(page?.items) ? page.items : []))
      .catch(() => setTicketTypes([]));  // 票种列表拿不到不阻断配置页，回落手填
  }, [api, canTicketTypeView]);

  function openEdit(key: EditKey) {
    setForm(EMPTY_FORM);
    setFormError("");
    setEditing(key);
  }
  const setField = (k: keyof Form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((prev) => ({ ...prev, [k]: e.target.value }));

  // 差人转正/软删失败 → 友好提示（后端 SELF_ACTIVATION_FORBIDDEN 走 detail_code）。
  function friendlyError(e: unknown, fallback: string): string {
    if (e instanceof ApiError && e.detailCode === "SELF_ACTIVATION_FORBIDDEN") {
      return "该草案由你起草，需由另一位拥有结算配置管理权限的同事复核转正/下线（同一人不能自审）。";
    }
    return (e as Error)?.message ?? fallback;
  }

  async function submitEdit() {
    if (!editing) return;
    setFormError("");
    const block = ALL_BLOCKS.find((b) => b.key === editing)!;
    const idempotencyKey = nextKey(editing);
    let body: Record<string, unknown>;
    try {
      body = buildBody(editing, form, idempotencyKey);
    } catch (validationError) {
      setFormError((validationError as Error).message);
      return;
    }
    setBusy(`edit-${editing}`);
    try {
      await api.post(`${BASE}/${block.endpoint}`, body);
      setEditing(null);
      reload();
    } catch (e: unknown) {
      setFormError((e as Error)?.message ?? "保存草案失败");
    } finally {
      setBusy(null);
    }
  }

  async function activate(draft: DraftItem) {
    setBusy(`activate-${draft.id}`);
    try {
      if (draft.configGroup === "rank_tiers") {
        await api.post(`${BASE}/rank-tiers/activate`, { projectId: projectFromSummary(draft.summary) });
      } else {
        await api.post(`${BASE}/${draft.configGroup}/${draft.id}/activate`);
      }
      reload();
    } catch (e: unknown) {
      setGlobalError(friendlyError(e, "草案转正失败"));
    } finally {
      setBusy(null);
    }
  }

  async function archive(draft: DraftItem) {
    const reason = window.prompt("请填写下线原因（软删留痕，原则上需高权限复核）：");
    if (reason == null) return; // 取消
    if (reason.trim() === "") { setGlobalError("下线原因必填。"); return; }
    setBusy(`archive-${draft.id}`);
    try {
      await api.post(`${BASE}/${draft.configGroup}/${draft.id}/archive`, {
        idempotencyKey: nextKey(`archive-${draft.configGroup}`),
        archiveReason: reason.trim(),
      });
      reload();
    } catch (e: unknown) {
      setGlobalError(friendlyError(e, "草案下线失败"));
    } finally {
      setBusy(null);
    }
  }

  const defaults = data?.platformDefaults ?? [];
  const defaultsByKey = new Map(defaults.map((it) => [it.key, it]));
  const drafts = data?.pendingDrafts ?? [];

  const overviewTab = (
    <>
      <h3>当前生效配置（平台默认口径）</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="settlement-config-defaults-table">
          <thead><tr><th>分组</th><th>配置项</th><th>当前生效值</th></tr></thead>
          <tbody>
            {defaults.map((it) => (
              <tr key={it.key} data-testid={`settlement-config-default-${it.key}`}>
                <td>{it.group}</td>
                <td>{it.label}</td>
                <td className="admin-num" style={{ textAlign: "right" }}>{it.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="admin-muted-action">
        税费、地产费用、分红、内部结算价等各组的可视化维护将随后续批次接入本页。
      </p>
    </>
  );

  // 配置块渲染（通票分账 / 提成与积分 两个栏目共用）。
  const renderBlocks = (blocks: BlockDef[]) => blocks.map((block) => {
    const current = block.defaultKey ? defaultsByKey.get(block.defaultKey) : undefined;
    return (
      <div key={block.key} className="admin-muted-action" data-testid={`settlement-config-block-${block.key}`}
        style={{ borderTop: "1px solid var(--adm-border, #eee)", padding: "10px 0" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
          <div>
            <div style={{ fontWeight: 600 }}>{block.title}</div>
            <div data-testid={`settlement-config-current-${block.key}`}>
              当前生效值：{current ? current.value : "按项目/商户维度维护，见下方草案队列与生效口径"}
            </div>
            {block.note ? <div style={{ fontSize: 12 }}>{block.note}</div> : null}
          </div>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid={`settlement-config-edit-${block.key}`}
            onClick={() => openEdit(block.key)}
          >改</PermissionButton>
        </div>
      </div>
    );
  });

  // 待复核草案队列（两个栏目共用同一队列，配置组已在行内标明）。
  const draftQueue = (
    <>
      <h3>待复核草案（{data?.pendingDraftCount ?? 0}）</h3>
      {drafts.length === 0 ? (
        <p className="admin-muted-action" data-testid="settlement-config-drafts-empty">
          当前无待复核草案。配置改动会先落草案、在此复核转正后方生效。
        </p>
      ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-config-drafts-table">
            <thead><tr><th>配置组</th><th>草案内容</th><th>复核</th></tr></thead>
            <tbody>
              {drafts.map((d) => (
                <tr key={d.id} data-testid={`settlement-config-draft-${d.id}`}>
                  <td>{GROUP_LABELS[d.configGroup] ?? d.configGroup}</td>
                  <td>
                    {d.summary}
                    {d.configGroup === "turnover_rent_rule" ? (
                      <div className="admin-muted-action">
                        初始规则按填写日期生效；已有规则变更统一按实际批准次月1日生效，批准当月仍按旧规则，手填起日不决定变更生效。
                      </div>
                    ) : null}
                  </td>
                  <td style={{ display: "flex", gap: 8 }}>
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={MANAGE}
                      data-testid={`settlement-config-activate-${d.id}`}
                      disabled={busy === `activate-${d.id}`}
                      onClick={() => void activate(d)}
                    >{busy === `activate-${d.id}` ? "转正中…" : "复核转正"}</PermissionButton>
                    {ARCHIVABLE.has(d.configGroup) ? (
                      <PermissionButton
                        permissionsState={effectivePermissions}
                        permissions={MANAGE}
                        data-testid={`settlement-config-archive-${d.id}`}
                        disabled={busy === `archive-${d.id}`}
                        onClick={() => void archive(d)}
                      >{busy === `archive-${d.id}` ? "下线中…" : "下线"}</PermissionButton>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );

  const ticketDistributionTab = (
    <div data-testid="settlement-config-ticket-distribution">
      <p className="admin-muted-action">
        通票分账配置：改动一律先落草案，经另一位有权限的同事「复核转正」后才生效（草案不即时生效）。
      </p>
      {renderBlocks(BLOCKS)}
      <DistributionSchemePanel api={api} effectivePermissions={effectivePermissions} />
    </div>
  );

  // V340（P0-D）：提成率、积分累计率原写死在代码里，现收进本页；同样走「改→落草案→差人复核转正」。
  const commissionPointsTab = (
    <div data-testid="settlement-config-commission-points">
      <p className="admin-muted-action">
        卡座销售提成与会员积分累计的口径，改动一律先落草案，经另一位有权限的同事「复核转正」后才生效。
        商户级特例可「下线」，下线后该商户回落到项目 / 全平台默认口径；全平台默认口径不可下线，只能改数值。
      </p>
      {renderBlocks(COMMISSION_POINTS_BLOCKS)}
    </div>
  );

  return (
    <section data-testid="settlement-config-page">
      <AdminPageHeader
        title="结算配置中心"
        description="统一查看结算相关配置的当前生效口径，并复核转正配置草案。配置改动一律先落草案、经复核转正后才生效（草案不即时生效）。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <Tabs
          type="card"
          data-testid="settlement-config-tabs"
          defaultActiveKey="overview"
          items={[
            { key: "overview", label: "概览", children: overviewTab },
            { key: "ticket-distribution", label: "通票分账", forceRender: true, children: ticketDistributionTab },
            { key: "commission-points", label: "提成与积分", forceRender: true, children: commissionPointsTab },
            // FS-021-R3：三个付账日、钱路指派规则与非积分商户账单异议期。
            { key: "internal-pay-days", label: "付账日与钱路指派", forceRender: true,
              children: <InternalPayDaysPanel api={api} effectivePermissions={effectivePermissions} /> },
          ]}
        />
        {/* 待复核草案队列各栏目共用一份（testid 唯一），放在页签下方常驻，任一栏目改动落草案后都在此复核转正。 */}
        {draftQueue}
      </AdminDataState>

      {editing ? (
        <GuidedFlowModal
          open
          title={`修改配置 · ${ALL_BLOCKS.find((b) => b.key === editing)!.title}`}
          testId="settlement-config-guided"
          submitTestId="settlement-config-submit"
          submitLabel="保存草案"
          submitting={busy === `edit-${editing}`}
          error={formError || undefined}
          onClose={() => setEditing(null)}
          onSubmit={() => void submitEdit()}
          steps={editSteps(editing, form, setField, entities, canEntityView, ticketTypes, canTicketTypeView)}
          summary={editSummary(editing, form, ticketTypes)}
        />
      ) : null}
    </section>
  );
}

// ── 每类配置的编辑字段（GuidedFlowModal 单屏 content） ─────────────────────
function editSteps(
  key: EditKey,
  form: Form,
  setField: (k: keyof Form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void,
  entities: BusinessEntityRow[],
  canEntityView: boolean,
  ticketTypes: TicketTypeOption[],
  canTicketTypeView: boolean,
): GuidedStep[] {
  const projectField = (
    <label>项目（留空 = 全平台默认）
      <input data-testid="settlement-config-field-projectId" value={form.projectId} onChange={setField("projectId")} placeholder="项目公司 id，空=默认" />
    </label>
  );
  let content: React.ReactNode;
  switch (key) {
    case "distribution_method":
      content = (<>{projectField}
        <label>分配方式
          <select data-testid="settlement-config-field-methodType" value={form.methodType} onChange={setField("methodType")}>
            {METHOD_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>短期经营激励单次有效期上限（出厂 3 个月；可配到天，如 3 天）
          <input data-testid="settlement-config-field-specialApprovalTermValue" type="number" min={1}
                 value={form.specialApprovalTermValue} onChange={setField("specialApprovalTermValue")} />
        </label>
        <label>上限参数的下限
          <input data-testid="settlement-config-field-specialApprovalTermMinValue" type="number" min={1}
                 value={form.specialApprovalTermMinValue} onChange={setField("specialApprovalTermMinValue")} />
        </label>
        <label>上限参数的上限
          <input data-testid="settlement-config-field-specialApprovalTermMaxValue" type="number" min={1}
                 value={form.specialApprovalTermMaxValue} onChange={setField("specialApprovalTermMaxValue")} />
        </label>
        <label>有效期单位
          <select data-testid="settlement-config-field-specialApprovalTermUnit"
                  value={form.specialApprovalTermUnit} onChange={setField("specialApprovalTermUnit")}>
            {TERM_UNIT_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label></>);
      break;
    case "comprehensive_service_fee":
      content = (<>{projectField}
        <label>商户 id（留空 = 通用）<input data-testid="settlement-config-field-merchantId" value={form.merchantId} onChange={setField("merchantId")} /></label>
        {canTicketTypeView ? (
          <label>票种（留空 = 该范围内所有票种通用）
            <select data-testid="settlement-config-field-ticketTypeId" value={form.ticketTypeId} onChange={setField("ticketTypeId")}>
              <option value="">全部票种通用</option>
              {ticketTypes.map((t) => (
                <option key={t.id} value={t.id}>{t.ticketName ?? t.id}</option>
              ))}
            </select>
          </label>
        ) : (
          <label>票种 id（无票种查看权限，暂按 id 手填；留空 = 全部票种通用）
            <input data-testid="settlement-config-field-ticketTypeId" value={form.ticketTypeId} onChange={setField("ticketTypeId")} />
          </label>
        )}
        <label>费率万分比（0~10000，配固定额时须 0）<input data-testid="settlement-config-field-feeBps" type="number" min={0} max={10000} value={form.feeBps} onChange={setField("feeBps")} /></label>
        <label>固定额（分，可留空；与费率二选一）<input data-testid="settlement-config-field-fixedAmountCent" type="number" min={0} value={form.fixedAmountCent} onChange={setField("fixedAmountCent")} /></label>
        <label>月度封顶（分，可留空 = 不封顶；比例/固定额都受此上限）<input data-testid="settlement-config-field-monthlyCapAmountCent" type="number" min={0} value={form.monthlyCapAmountCent} onChange={setField("monthlyCapAmountCent")} /></label></>);
      break;
    case "pass_gift_cost":
      content = (<>{projectField}
        {canTicketTypeView ? (
          <label>票种（留空 = 该范围内所有票种通用）
            <select data-testid="settlement-config-field-ticketTypeId" value={form.ticketTypeId} onChange={setField("ticketTypeId")}>
              <option value="">全部票种通用</option>
              {ticketTypes.map((t) => (
                <option key={t.id} value={t.id}>{t.ticketName ?? t.id}</option>
              ))}
            </select>
          </label>
        ) : (
          <label>票种 id（无票种查看权限，暂按 id 手填；留空 = 全部票种通用）
            <input data-testid="settlement-config-field-ticketTypeId" value={form.ticketTypeId} onChange={setField("ticketTypeId")} />
          </label>
        )}
        <label>预扣模式
          <select data-testid="settlement-config-field-predeductMode" value={form.predeductMode} onChange={setField("predeductMode")}>
            {PREDEDUCT_MODE_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        {form.predeductMode === "by_ticket_amount" ? (
          <label>该票种赠送部分的售价整额（元；0 = 预扣关闭）
            <input data-testid="settlement-config-field-ticketGiftAmountYuan" type="number" min={0} step="0.01" value={form.ticketGiftAmountYuan} onChange={setField("ticketGiftAmountYuan")} />
          </label>
        ) : (
          <div style={{ fontSize: 12 }} data-testid="settlement-config-by-item-hint">
            按件计的预扣金额由票面推导（各赠送权益数量 × 提供方内部结算价之和），无需手填；商户让利型权益不进预扣。
          </div>
        )}
        <label>每月封顶（分，可留空 = 不封顶）
          <input data-testid="settlement-config-field-monthlyCapAmountCent" type="number" min={0} value={form.monthlyCapAmountCent} onChange={setField("monthlyCapAmountCent")} />
        </label>
        <div style={{ fontSize: 12 }} data-testid="settlement-config-variance-fixed">预扣与实付差额：{VARIANCE_FIXED_LABEL}</div></>);
      break;
    case "combo_discount":
      content = (<>{projectField}
        <label>折扣归属
          <select data-testid="settlement-config-field-discountBearer" value={form.discountBearer} onChange={setField("discountBearer")}>
            {BEARER_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>平台分摊万分比（0~10000，仅按比例分摊时有意义）<input data-testid="settlement-config-field-splitPlatformBps" type="number" min={0} max={10000} value={form.splitPlatformBps} onChange={setField("splitPlatformBps")} /></label>
        <label>通票凑整允许误差（元）<input data-testid="settlement-config-field-roundingToleranceYuan" type="number" min={0} step="0.01" value={form.roundingToleranceYuan} onChange={setField("roundingToleranceYuan")} /></label>
        <div style={{ fontSize: 12 }} data-testid="settlement-config-rounding-tolerance-hint">
          运营为了好卖会把系统算出的 197.50 填成 199（上调下调都行），这属于凑整；填成 179 就是填错了。
          这个数就是「差多少还算凑整」的界线，超出的票直接拒绝发布。填 5 元够凑到最近的尾数 8/9/0，又拦得住一次真正的填错。
        </div></>);
      break;
    case "channel_fee":
      content = (<>{projectField}
        <label>费用层
          <select data-testid="settlement-config-field-feeLayer" value={form.feeLayer} onChange={setField("feeLayer")}>
            {FEE_LAYER_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>{form.feeLayer === "route"
          ? "支付通路码（必填，如 wechat、alipay、aggregate_pay、unionpay；第三方回款通路填其渠道码）"
          : "第三方销售渠道码（如 meituan、douyin；留空 = 第三方通用）"}
          <input data-testid="settlement-config-field-channel" value={form.channel} onChange={setField("channel")} />
        </label>
        <label>{form.feeLayer === "route" ? "通路费率万分比（0~10000）" : "销售渠道费率万分比（0~10000，配固定金额时须 0）"}<input data-testid="settlement-config-field-feeBps" type="number" min={0} max={10000} value={form.feeBps} onChange={setField("feeBps")} /></label>
        {form.feeLayer === "sales" ? (
          <label>每张票固定金额（分，可留空；与比例二选一）<input data-testid="settlement-config-field-fixedAmountCent" type="number" min={0} value={form.fixedAmountCent} onChange={setField("fixedAmountCent")} /></label>
        ) : null}</>);
      break;
    case "revenue_stream_attribution":
      content = (<>{projectField}
        <label>收入流业态
          <select data-testid="settlement-config-field-revenueStream" value={form.revenueStream} onChange={setField("revenueStream")}>
            {REVENUE_STREAM_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <div style={{ fontSize: 12 }} data-testid="settlement-config-revenue-stream-hint">{REVENUE_STREAM_HINT[form.revenueStream] ?? ""}</div>
        <label>收款方类型
          <select data-testid="settlement-config-field-beneficiaryKind" value={form.beneficiaryKind} onChange={setField("beneficiaryKind")}>
            {BENEFICIARY_KIND_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        {form.beneficiaryKind === "per_merchant" ? (
          <div style={{ fontSize: 12 }} data-testid="settlement-config-per-merchant-hint">
            这笔收入归产生它的各商户自己，没有单一收款主体，无需选主体。
          </div>
        ) : canEntityView ? (
          <label>收款主体
            <select data-testid="settlement-config-field-managementEntityId" value={form.managementEntityId} onChange={setField("managementEntityId")}>
              <option value="">请选择收款主体</option>
              {entities.map((e) => (
                <option key={e.id} value={e.id}>
                  {(e.entityName ?? e.id) + (e.entityRole ? `（${formatBusinessStatus(e.entityRole)}）` : "")}
                </option>
              ))}
            </select>
          </label>
        ) : (
          <label>收款主体 id（无经营主体查看权限，暂按 id 手填）
            <input data-testid="settlement-config-field-managementEntityId" value={form.managementEntityId} onChange={setField("managementEntityId")} />
          </label>
        )}</>);
      break;
    case "commission_rate":
      content = (<>{projectField}
        <label>商户 id（留空 = 该项目通用）<input data-testid="settlement-config-field-merchantId" value={form.merchantId} onChange={setField("merchantId")} /></label>
        <label>提成角色档
          <select data-testid="settlement-config-field-roleType" value={form.roleType} onChange={setField("roleType")}>
            {ROLE_TYPE_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>提成率万分比（0~10000；1500 = 15%）<input data-testid="settlement-config-field-rateBp" type="number" min={0} max={10000} value={form.rateBp} onChange={setField("rateBp")} /></label></>);
      break;
    case "points_accrual_rate":
      content = (<>{projectField}
        <label>商户 id（留空 = 通用）<input data-testid="settlement-config-field-merchantId" value={form.merchantId} onChange={setField("merchantId")} /></label>
        <label>每积 1 分所需消费金额（分，须大于 0；100 = 每 1 元积 1 分）<input data-testid="settlement-config-field-amountCentPerPoint" type="number" min={1} value={form.amountCentPerPoint} onChange={setField("amountCentPerPoint")} /></label></>);
      break;
  }
  return [{ title: "配置内容", validate: () => validate(key, form), content }];
}

function editSummary(key: EditKey, form: Form, ticketTypes: TicketTypeOption[]): GuidedSummaryItem[] {
  const scope: GuidedSummaryItem = { label: "项目", value: form.projectId.trim() || "全平台默认" };
  // 确认页给票名而不是 id —— 复核的人要看得出这条费率影响的是哪个票。
  const ticketTypeLabel = (id: string) => {
    const t = id.trim();
    if (t === "") return "全部票种通用";
    return ticketTypes.find((x) => x.id === t)?.ticketName ?? t;
  };
  switch (key) {
    case "distribution_method": return [scope, { label: "分配方式", value: METHOD_OPTIONS.find(([v]) => v === form.methodType)?.[1] ?? form.methodType }, { label: "短期经营激励单次有效期上限", value: `${form.specialApprovalTermValue} ${form.specialApprovalTermUnit === "day" ? "天" : "个月"}（下限 ${form.specialApprovalTermMinValue}，上限 ${form.specialApprovalTermMaxValue}）` }];
    case "comprehensive_service_fee": return [scope, { label: "商户", value: form.merchantId.trim() || "通用" }, { label: "票种", value: ticketTypeLabel(form.ticketTypeId) }, { label: "费率万分比", value: form.feeBps || "0" }, { label: "固定额（分）", value: form.fixedAmountCent.trim() || "—" }, { label: "月度封顶（分）", value: form.monthlyCapAmountCent.trim() || "不封顶" }];
    case "pass_gift_cost": return [scope, { label: "票种", value: ticketTypeLabel(form.ticketTypeId) },
      { label: "预扣模式", value: PREDEDUCT_MODE_OPTIONS.find(([v]) => v === form.predeductMode)?.[1] ?? form.predeductMode },
      { label: "按票计整额", value: form.predeductMode === "by_ticket_amount" ? `${form.ticketGiftAmountYuan || "0"} 元` : "由票面推导" },
      { label: "每月封顶（分）", value: form.monthlyCapAmountCent.trim() || "不封顶" },
      { label: "差额处理", value: VARIANCE_FIXED_LABEL }];
    case "combo_discount": return [scope, { label: "折扣归属", value: BEARER_OPTIONS.find(([v]) => v === form.discountBearer)?.[1] ?? form.discountBearer }, { label: "平台分摊万分比", value: form.splitPlatformBps || "0" }, { label: "通票凑整允许误差", value: `${form.roundingToleranceYuan || "0"} 元` }];
    case "channel_fee": return [scope, { label: "费用层", value: FEE_LAYER_OPTIONS.find(([v]) => v === form.feeLayer)?.[1] ?? form.feeLayer },
      { label: form.feeLayer === "route" ? "支付通路" : "销售渠道", value: form.channel.trim() || (form.feeLayer === "route" ? "—" : "第三方通用") },
      { label: "费率万分比", value: form.feeBps || "—" },
      ...(form.feeLayer === "sales" ? [{ label: "每张票固定金额（分）", value: form.fixedAmountCent.trim() || "—" }] : [])];
    case "revenue_stream_attribution": return [scope, { label: "收入流业态", value: REVENUE_STREAM_OPTIONS.find(([v]) => v === form.revenueStream)?.[1] ?? form.revenueStream }, { label: "收款方类型", value: BENEFICIARY_KIND_OPTIONS.find(([v]) => v === form.beneficiaryKind)?.[1] ?? form.beneficiaryKind }, { label: "收款主体", value: form.beneficiaryKind === "per_merchant" ? "各商户自己" : (form.managementEntityId || "—") }];
    case "commission_rate": return [scope, { label: "商户", value: form.merchantId.trim() || "该项目通用" }, { label: "角色档", value: ROLE_TYPE_OPTIONS.find(([v]) => v === form.roleType)?.[1] ?? form.roleType }, { label: "提成率万分比", value: form.rateBp || "—" }];
    case "points_accrual_rate": return [scope, { label: "商户", value: form.merchantId.trim() || "通用" }, { label: "每积1分所需消费（分）", value: form.amountCentPerPoint || "—" }];
  }
}

// 单屏校验（返回中文错误则阻止提交，null 放行）。
function validate(key: EditKey, form: Form): string | null {
  switch (key) {
    case "distribution_method":
      // 第 75 条（ENUM 第 52 行「初始有效期不超过后台配置的上限（业务参数，按项目可配，出厂默认三个月，须同时配置上下限）」）：
      //   数值、单位、下限、上限四项同时必填，1 ≤ 下限 ≤ 数值 ≤ 上限（按所选单位计，天或月）。
      return isIntInRange(form.specialApprovalTermMinValue, 1, Number.MAX_SAFE_INTEGER)
        && isIntInRange(form.specialApprovalTermMaxValue, 1, Number.MAX_SAFE_INTEGER)
        && isIntInRange(form.specialApprovalTermValue, Number(form.specialApprovalTermMinValue), Number(form.specialApprovalTermMaxValue))
        ? null : "短期经营激励单次有效期上限须同时填写数值、下限与上限（不小于 1 的整数），且下限 ≤ 数值 ≤ 上限。";
    case "comprehensive_service_fee": {
      const feeStr = form.feeBps.trim() === "" ? "0" : form.feeBps;
      if (!isIntInRange(feeStr, 0, 10000)) return "费率万分比需为 0~10000 的整数。";
      const hasFixed = orUndef(form.fixedAmountCent) !== undefined;
      if (hasFixed && !isIntInRange(form.fixedAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "固定额需为不小于 0 的整数（分）。";
      if (hasFixed && Number(feeStr) !== 0) return "配固定额时费率万分比须为 0（两者二选一）。";
      if (orUndef(form.monthlyCapAmountCent) !== undefined && !isIntInRange(form.monthlyCapAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "月度封顶需为不小于 0 的整数（分），留空 = 不封顶。";
      return null;
    }
    case "pass_gift_cost": {
      if (form.predeductMode === "by_ticket_amount") {
        const yuan = Number(form.ticketGiftAmountYuan);
        if (form.ticketGiftAmountYuan.trim() === "" || !Number.isFinite(yuan) || yuan < 0) {
          return "按票计模式必须填该票种赠送部分的售价整额（元，可为 0 = 预扣关闭）。";
        }
      }
      if (orUndef(form.monthlyCapAmountCent) !== undefined && !isIntInRange(form.monthlyCapAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "每月封顶需为不小于 0 的整数（分），留空 = 不封顶。";
      return null;
    }
    case "combo_discount":
      if (!isIntInRange(form.splitPlatformBps || "0", 0, 10000)) return "平台分摊万分比需为 0~10000 的整数。";
      return Number(form.roundingToleranceYuan) >= 0 && Number.isFinite(Number(form.roundingToleranceYuan))
        ? null : "通票凑整允许误差需为不小于 0 的金额（元）。";
    case "channel_fee": {
      if (form.feeLayer === "route") {
        if (orUndef(form.channel) === undefined) return "通路费须填写支付通路码。";
        return isIntInRange(form.feeBps, 0, 10000) ? null : "通路费率万分比需为 0~10000 的整数。";
      }
      const chFixed = orUndef(form.fixedAmountCent) !== undefined;
      if (chFixed && !isIntInRange(form.fixedAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "每张票固定金额需为不小于 0 的整数（分）。";
      const chFee = chFixed && form.feeBps.trim() === "" ? "0" : form.feeBps;
      if (!isIntInRange(chFee, 0, 10000)) return "销售渠道费率万分比需为 0~10000 的整数。";
      if (chFixed && Number(chFee) !== 0) return "比例与每张票固定金额二选一：配了固定金额，费率万分比须为 0。";
      return null;
    }
    case "revenue_stream_attribution":
      if (form.beneficiaryKind === "per_merchant") return null;  // 归各商户自己，无单一主体
      return orUndef(form.managementEntityId) === undefined ? "收款主体必选。" : null;
    case "commission_rate":
      return isIntInRange(form.rateBp, 0, 10000) ? null : "提成率万分比需为 0~10000 的整数（1500 = 15%）。";
    case "points_accrual_rate":
      return isIntInRange(form.amountCentPerPoint, 1, Number.MAX_SAFE_INTEGER)
        ? null : "每积 1 分所需消费金额需为大于 0 的整数（分）；100 = 每 1 元积 1 分。";
  }
}

// 构造落草案请求体（camelCase；adminClient 会归一为 snake_case 并注入 idempotency_key，此处仍显式带上）。
// 抛错=校验未过（提交前 GuidedFlowModal 已跑 validate，这里是二次防线）。
function buildBody(key: EditKey, form: Form, idempotencyKey: string): Record<string, unknown> {
  const invalid = validate(key, form);
  if (invalid) throw new Error(invalid);
  const project = orUndef(form.projectId);
  switch (key) {
    case "distribution_method":
      return {
        idempotencyKey, projectId: project, methodType: form.methodType,
        specialApprovalTermValue: Number(form.specialApprovalTermValue),
        specialApprovalTermUnit: form.specialApprovalTermUnit || "month",
        specialApprovalTermMinValue: Number(form.specialApprovalTermMinValue),
        specialApprovalTermMaxValue: Number(form.specialApprovalTermMaxValue),
      };
    case "comprehensive_service_fee":
      return {
        idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), ticketTypeId: orUndef(form.ticketTypeId),
        feeBps: form.feeBps.trim() === "" ? 0 : Number(form.feeBps),
        fixedAmountCent: orUndef(form.fixedAmountCent) === undefined ? null : Number(form.fixedAmountCent),
        monthlyCapAmountCent: orUndef(form.monthlyCapAmountCent) === undefined ? null : Number(form.monthlyCapAmountCent),
      };
    case "pass_gift_cost":
      return {
        idempotencyKey, projectId: project, ticketTypeId: orUndef(form.ticketTypeId),
        predeductMode: form.predeductMode,
        // 按件计不许带整额（后端与 DB CHECK 同锁）；按票计元→分。
        ticketGiftAmountCent: form.predeductMode === "by_ticket_amount"
          ? Math.round(Number(form.ticketGiftAmountYuan || "0") * 100) : null,
        monthlyCapAmountCent: orUndef(form.monthlyCapAmountCent) === undefined ? null : Number(form.monthlyCapAmountCent),
      };
    case "combo_discount":
      return { idempotencyKey, projectId: project, discountBearer: form.discountBearer, splitPlatformBps: Number(form.splitPlatformBps || "0"),
        roundingToleranceCent: Math.round(Number(form.roundingToleranceYuan || "0") * 100) };
    case "channel_fee":
      return {
        idempotencyKey, projectId: project, feeLayer: form.feeLayer, channel: orUndef(form.channel) ?? null,
        feeBps: form.feeBps.trim() === "" ? 0 : Number(form.feeBps),
        fixedAmountCent: form.feeLayer === "route" || orUndef(form.fixedAmountCent) === undefined ? null : Number(form.fixedAmountCent),
      };
    case "revenue_stream_attribution":
      return {
        idempotencyKey, projectId: project, revenueStream: form.revenueStream,
        beneficiaryKind: form.beneficiaryKind,
        // 归各商户自己时不得带主体 id（后端 CHECK 强制主体列为空）。
        managementEntityId: form.beneficiaryKind === "per_merchant" ? undefined : form.managementEntityId.trim(),
      };
    case "commission_rate":
      return { idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), roleType: form.roleType, rateBp: Number(form.rateBp) };
    case "points_accrual_rate":
      return { idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), amountCentPerPoint: Number(form.amountCentPerPoint) };
  }
}
