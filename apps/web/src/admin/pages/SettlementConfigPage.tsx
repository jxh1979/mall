import React from "react";
import { Tabs } from "antd";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal, type GuidedStep, type GuidedSummaryItem } from "../components/GuidedFlowModal";
import { PermissionButton } from "../security/Can";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";

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

// 8 类通票分账配置 + V340 提成/积分 2 类（configGroup 即转正泛化 configType，rank_tiers 例外走 /rank-tiers/activate）。
type EditKey =
  | "retention"
  | "distribution_method"
  | "rank_tiers"
  | "merchant_rate"
  | "comprehensive_service_fee"
  | "combo_discount"
  | "channel_fee"
  | "revenue_stream_attribution"
  | "commission_rate"
  | "points_accrual_rate";

type BlockDef = { key: EditKey; title: string; defaultKey?: string; note?: string; endpoint: string };

// 各块：标题 + 当前生效值取自 overview.platformDefaults 的哪个 key（无则块内提示按域/按商户维护）。
const BLOCKS: BlockDef[] = [
  { key: "retention", title: "项目公司留存比例", defaultKey: "platform_retention_bps", endpoint: "retention" },
  { key: "distribution_method", title: "各酒吧分配方式", defaultKey: "distribution_method", endpoint: "distribution-method" },
  { key: "rank_tiers", title: "排名档位分账梯子", endpoint: "rank-tiers", note: "按销售排名分档分账；当前生效梯子按项目维护，改动落草案、复核转正后生效。" },
  { key: "merchant_rate", title: "商户每客单价 / 固定额", endpoint: "merchant-rate", note: "按「项目 × 商户」配置，改动落草案；可对草案「下线」。" },
  { key: "comprehensive_service_fee", title: "门票综合服务费", defaultKey: "comprehensive_service_fee", endpoint: "comprehensive-service-fee", note: "按「项目 × 商户 × 票种」配置（商户 / 票种留空 = 通用）。" },
  { key: "combo_discount", title: "套票折扣归属", defaultKey: "combo_discount_bearer", endpoint: "combo-discount" },
  { key: "channel_fee", title: "渠道抽成费率", endpoint: "channel-fee", note: "按项目配置通用渠道抽成率（万分比），改动落草案。" },
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

const METHOD_OPTIONS: [string, string][] = [
  ["by_venue_per_guest", "按场地每客单价"],
  ["by_rank_tier", "按销售排名档位"],
  ["by_fixed_amount", "按固定额"],
];
const BEARER_OPTIONS: [string, string][] = [
  ["platform_absorb", "平台承担"],
  ["merchant_absorb", "商户承担"],
  ["split_by_ratio", "按比例分摊"],
];
const REVENUE_STREAM_OPTIONS: [string, string][] = [
  ["all_pass_ticket", "通票"],
  ["table_dining", "餐饮"],
  ["bar_service_fee", "酒吧服务费"],
  ["amusement_ticket", "游乐门票"],
  ["performance_ticket", "演出门票"],
  ["venue_rent", "场地租金"],
];
// configGroup → 中文（草案队列展示用；未列出的原样显示）。
const GROUP_LABELS: Record<string, string> = {
  retention: "留存比例",
  distribution_method: "分配方式",
  rank_tiers: "排名档位",
  merchant_rate: "商户每客/固定额",
  comprehensive_service_fee: "综合服务费",
  combo_discount: "套票折扣归属",
  channel_fee: "渠道抽成",
  revenue_stream_attribution: "收入流归因",
  attribution_rule: "收入归因规则",
  commission_rate: "卡座提成费率",
  points_accrual_rate: "积分累计率",
  // V350：全平台「高价值物品」判定阈值（退酒/报损/盘点差异/存酒作废共用一个值，比的是物品进货价）。
  high_value_threshold: "高价值判定阈值(进货价)",
  // V346（P0-E）：营业额抽成规则在「合同工作台」填写并落草案，在此复核转正后才对结算生效。
  turnover_rent_rule: "营业额抽成规则",
};
// 支持「下线」（软删）的草案类型，对齐后端 archive 白名单。
// V340 提成/积分：商户级特例条目须可下线，否则该商户被特例永久遮挡、回落不到项目/全局默认口径。
// V350 高价值判定阈值：商户级特例条目同理可下线（回落项目/全局默认），全局默认种子由后端守卫拦住。
// V346 抽成规则：合同终止/换规则时须可下线，下线后该合同不再产生抽成应收。
const ARCHIVABLE = new Set([
  "merchant_rate",
  "revenue_stream_attribution",
  "commission_rate",
  "points_accrual_rate",
  "high_value_threshold",
  "turnover_rent_rule",
]);

type TierRow = { rankFrom: string; rankTo: string; shareBps: string };
type Form = {
  projectId: string;
  retentionBps: string;
  methodType: string;
  specialApprovalTermMonths: string;
  merchantId: string;
  perGuestCent: string;
  fixedAmountCent: string;
  monthlyCapAmountCent: string;
  entitlementType: string;
  feeBps: string;
  discountBearer: string;
  splitPlatformBps: string;
  revenueStream: string;
  managementEntityId: string;
  roleType: string;
  rateBp: string;
  amountCentPerPoint: string;
};
const EMPTY_FORM: Form = {
  projectId: "", retentionBps: "", methodType: "by_venue_per_guest", specialApprovalTermMonths: "3", merchantId: "",
  perGuestCent: "", fixedAmountCent: "", monthlyCapAmountCent: "", entitlementType: "", feeBps: "",
  discountBearer: "platform_absorb", splitPlatformBps: "0", revenueStream: "all_pass_ticket", managementEntityId: "",
  roleType: "sales", rateBp: "", amountCentPerPoint: "",
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

// 排名档位区间覆盖客户端校验（镜像后端 validateRankTierCoverage：连续、首档 from=1、Σ((to-from+1)×share)=10000）。
function rankTierHint(tiers: TierRow[]): { ok: boolean; msg: string } {
  const parsed = tiers.map((t) => ({ from: Number(t.rankFrom), to: Number(t.rankTo), share: Number(t.shareBps) }));
  for (const t of parsed) {
    if (!Number.isInteger(t.from) || !Number.isInteger(t.to) || !Number.isInteger(t.share)) {
      return { ok: false, msg: "每档的起始名次、结束名次、分账万分比都必须是整数。" };
    }
    if (t.from < 1 || t.to < t.from) return { ok: false, msg: "每档需满足 1 ≤ 起始名次 ≤ 结束名次。" };
    if (t.share < 0 || t.share > 10000) return { ok: false, msg: "分账万分比需在 0~10000 之间。" };
  }
  const sorted = [...parsed].sort((a, b) => a.from - b.from);
  let expected = 1;
  let sum = 0;
  for (const t of sorted) {
    if (t.from !== expected) return { ok: false, msg: `名次区间需连续无缺口/重叠：期望下一档从第 ${expected} 名开始。` };
    sum += (t.to - t.from + 1) * t.share;
    expected = t.to + 1;
  }
  if (sum !== 10000) return { ok: false, msg: `全部名次的分账万分比合计需正好 10000，当前为 ${sum}。` };
  return { ok: true, msg: "区间连续、合计 10000，校验通过。" };
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
  const [tiers, setTiers] = React.useState<TierRow[]>([{ rankFrom: "1", rankTo: "", shareBps: "" }]);
  const [formError, setFormError] = React.useState<string>("");

  const reload = React.useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Overview>(`${BASE}/overview`)
      .then((d) => { setData(d); setLoading(false); })
      .catch((e: unknown) => { setError((e as Error)?.message ?? "结算配置总览读取失败"); setLoading(false); });
  }, [api]);

  React.useEffect(() => { reload(); }, [reload]);

  function openEdit(key: EditKey) {
    setForm(EMPTY_FORM);
    setTiers([{ rankFrom: "1", rankTo: "", shareBps: "" }]);
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
      body = buildBody(editing, form, tiers, idempotencyKey);
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
                  <td>{d.summary}</td>
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
        通票分账 8 类配置：改动一律先落草案，经另一位有权限的同事「复核转正」后才生效（草案不即时生效）。
      </p>
      {renderBlocks(BLOCKS)}
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
          steps={editSteps(editing, form, setField, tiers, setTiers)}
          summary={editSummary(editing, form, tiers)}
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
  tiers: TierRow[],
  setTiers: React.Dispatch<React.SetStateAction<TierRow[]>>,
): GuidedStep[] {
  const projectField = (
    <label>项目（留空 = 全平台默认）
      <input data-testid="settlement-config-field-projectId" value={form.projectId} onChange={setField("projectId")} placeholder="项目公司 id，空=默认" />
    </label>
  );
  let content: React.ReactNode;
  switch (key) {
    case "retention":
      content = (<>{projectField}
        <label>留存万分比（0~10000）
          <input data-testid="settlement-config-field-retentionBps" type="number" min={0} max={10000} value={form.retentionBps} onChange={setField("retentionBps")} />
        </label></>);
      break;
    case "distribution_method":
      content = (<>{projectField}
        <label>分配方式
          <select data-testid="settlement-config-field-methodType" value={form.methodType} onChange={setField("methodType")}>
            {METHOD_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>超池特批有效期（月，默认 3）
          <input data-testid="settlement-config-field-specialApprovalTermMonths" type="number" min={1}
                 value={form.specialApprovalTermMonths} onChange={setField("specialApprovalTermMonths")} />
        </label></>);
      break;
    case "merchant_rate":
      content = (<>{projectField}
        <label>商户 id<input data-testid="settlement-config-field-merchantId" value={form.merchantId} onChange={setField("merchantId")} /></label>
        <label>每客单价（分，可留空）<input data-testid="settlement-config-field-perGuestCent" type="number" min={0} value={form.perGuestCent} onChange={setField("perGuestCent")} /></label>
        <label>固定额（分，可留空）<input data-testid="settlement-config-field-fixedAmountCent" type="number" min={0} value={form.fixedAmountCent} onChange={setField("fixedAmountCent")} /></label></>);
      break;
    case "comprehensive_service_fee":
      content = (<>{projectField}
        <label>商户 id（留空 = 通用）<input data-testid="settlement-config-field-merchantId" value={form.merchantId} onChange={setField("merchantId")} /></label>
        <label>票种（留空 = 通用）<input data-testid="settlement-config-field-entitlementType" value={form.entitlementType} onChange={setField("entitlementType")} /></label>
        <label>费率万分比（0~10000，配固定额时须 0）<input data-testid="settlement-config-field-feeBps" type="number" min={0} max={10000} value={form.feeBps} onChange={setField("feeBps")} /></label>
        <label>固定额（分，可留空；与费率二选一）<input data-testid="settlement-config-field-fixedAmountCent" type="number" min={0} value={form.fixedAmountCent} onChange={setField("fixedAmountCent")} /></label>
        <label>月度封顶（分，可留空 = 不封顶；比例/固定额都受此上限）<input data-testid="settlement-config-field-monthlyCapAmountCent" type="number" min={0} value={form.monthlyCapAmountCent} onChange={setField("monthlyCapAmountCent")} /></label></>);
      break;
    case "combo_discount":
      content = (<>{projectField}
        <label>折扣归属
          <select data-testid="settlement-config-field-discountBearer" value={form.discountBearer} onChange={setField("discountBearer")}>
            {BEARER_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>平台分摊万分比（0~10000，仅按比例分摊时有意义）<input data-testid="settlement-config-field-splitPlatformBps" type="number" min={0} max={10000} value={form.splitPlatformBps} onChange={setField("splitPlatformBps")} /></label></>);
      break;
    case "channel_fee":
      content = (<>{projectField}
        <label>通用渠道抽成万分比（0~10000）<input data-testid="settlement-config-field-feeBps" type="number" min={0} max={10000} value={form.feeBps} onChange={setField("feeBps")} /></label></>);
      break;
    case "revenue_stream_attribution":
      content = (<>{projectField}
        <label>收入流业态
          <select data-testid="settlement-config-field-revenueStream" value={form.revenueStream} onChange={setField("revenueStream")}>
            {REVENUE_STREAM_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </label>
        <label>归因管理公司 / 业态主体 id<input data-testid="settlement-config-field-managementEntityId" value={form.managementEntityId} onChange={setField("managementEntityId")} /></label></>);
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
    case "rank_tiers": {
      const hint = rankTierHint(tiers);
      content = (<>{projectField}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-config-rank-tier-table">
            <thead><tr><th>起始名次</th><th>结束名次</th><th>分账万分比</th><th>操作</th></tr></thead>
            <tbody>
              {tiers.map((t, i) => (
                <tr key={i}>
                  <td><input aria-label={`档${i + 1}起始名次`} type="number" min={1} value={t.rankFrom} onChange={(e) => setTiers(tiers.map((x, j) => (j === i ? { ...x, rankFrom: e.target.value } : x)))} /></td>
                  <td><input aria-label={`档${i + 1}结束名次`} type="number" min={1} value={t.rankTo} onChange={(e) => setTiers(tiers.map((x, j) => (j === i ? { ...x, rankTo: e.target.value } : x)))} /></td>
                  <td><input aria-label={`档${i + 1}分账万分比`} type="number" min={0} max={10000} value={t.shareBps} onChange={(e) => setTiers(tiers.map((x, j) => (j === i ? { ...x, shareBps: e.target.value } : x)))} /></td>
                  <td><button type="button" onClick={() => setTiers(tiers.filter((_, j) => j !== i))} disabled={tiers.length <= 1}>删</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <button type="button" data-testid="settlement-config-rank-tier-add" onClick={() => setTiers([...tiers, { rankFrom: "", rankTo: "", shareBps: "" }])}>＋ 加一档</button>
        <div data-testid="settlement-config-rank-tier-hint" style={{ color: hint.ok ? "var(--adm-ok, #1a7f37)" : "var(--adm-danger, #b3261e)" }}>{hint.msg}</div>
      </>);
      break;
    }
  }
  return [{ title: "配置内容", validate: () => validate(key, form, tiers), content }];
}

function editSummary(key: EditKey, form: Form, tiers: TierRow[]): GuidedSummaryItem[] {
  const scope: GuidedSummaryItem = { label: "项目", value: form.projectId.trim() || "全平台默认" };
  switch (key) {
    case "retention": return [scope, { label: "留存万分比", value: form.retentionBps || "—" }];
    case "distribution_method": return [scope, { label: "分配方式", value: METHOD_OPTIONS.find(([v]) => v === form.methodType)?.[1] ?? form.methodType }, { label: "超池特批有效期（月）", value: form.specialApprovalTermMonths || "3" }];
    case "merchant_rate": return [scope, { label: "商户", value: form.merchantId || "—" }, { label: "每客单价（分）", value: form.perGuestCent || "—" }, { label: "固定额（分）", value: form.fixedAmountCent || "—" }];
    case "comprehensive_service_fee": return [scope, { label: "商户", value: form.merchantId.trim() || "通用" }, { label: "票种", value: form.entitlementType.trim() || "通用" }, { label: "费率万分比", value: form.feeBps || "0" }, { label: "固定额（分）", value: form.fixedAmountCent.trim() || "—" }, { label: "月度封顶（分）", value: form.monthlyCapAmountCent.trim() || "不封顶" }];
    case "combo_discount": return [scope, { label: "折扣归属", value: BEARER_OPTIONS.find(([v]) => v === form.discountBearer)?.[1] ?? form.discountBearer }, { label: "平台分摊万分比", value: form.splitPlatformBps || "0" }];
    case "channel_fee": return [scope, { label: "通用渠道抽成万分比", value: form.feeBps || "—" }];
    case "revenue_stream_attribution": return [scope, { label: "收入流业态", value: REVENUE_STREAM_OPTIONS.find(([v]) => v === form.revenueStream)?.[1] ?? form.revenueStream }, { label: "归因主体", value: form.managementEntityId || "—" }];
    case "rank_tiers": return [scope, { label: "档位数", value: String(tiers.length) }, { label: "区间校验", value: rankTierHint(tiers).msg }];
    case "commission_rate": return [scope, { label: "商户", value: form.merchantId.trim() || "该项目通用" }, { label: "角色档", value: ROLE_TYPE_OPTIONS.find(([v]) => v === form.roleType)?.[1] ?? form.roleType }, { label: "提成率万分比", value: form.rateBp || "—" }];
    case "points_accrual_rate": return [scope, { label: "商户", value: form.merchantId.trim() || "通用" }, { label: "每积1分所需消费（分）", value: form.amountCentPerPoint || "—" }];
  }
}

// 单屏校验（返回中文错误则阻止提交，null 放行）。
function validate(key: EditKey, form: Form, tiers: TierRow[]): string | null {
  switch (key) {
    case "retention":
      return isIntInRange(form.retentionBps, 0, 10000) ? null : "留存万分比需为 0~10000 的整数。";
    case "distribution_method":
      // S9：超池特批有效期（月）——业务口径「短期激励，默认不超过 3 个月」，留空按 3 落库。
      return isIntInRange(form.specialApprovalTermMonths || "3", 1, Number.MAX_SAFE_INTEGER)
        ? null : "超池特批有效期需为不小于 1 的整数（月）。";
    case "merchant_rate":
      if (orUndef(form.merchantId) === undefined) return "商户 id 必填。";
      if (orUndef(form.perGuestCent) === undefined && orUndef(form.fixedAmountCent) === undefined) return "每客单价与固定额至少填一项。";
      if (orUndef(form.perGuestCent) !== undefined && !isIntInRange(form.perGuestCent, 0, Number.MAX_SAFE_INTEGER)) return "每客单价需为不小于 0 的整数（分）。";
      if (orUndef(form.fixedAmountCent) !== undefined && !isIntInRange(form.fixedAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "固定额需为不小于 0 的整数（分）。";
      return null;
    case "comprehensive_service_fee": {
      const feeStr = form.feeBps.trim() === "" ? "0" : form.feeBps;
      if (!isIntInRange(feeStr, 0, 10000)) return "费率万分比需为 0~10000 的整数。";
      const hasFixed = orUndef(form.fixedAmountCent) !== undefined;
      if (hasFixed && !isIntInRange(form.fixedAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "固定额需为不小于 0 的整数（分）。";
      if (hasFixed && Number(feeStr) !== 0) return "配固定额时费率万分比须为 0（两者二选一）。";
      if (orUndef(form.monthlyCapAmountCent) !== undefined && !isIntInRange(form.monthlyCapAmountCent, 0, Number.MAX_SAFE_INTEGER)) return "月度封顶需为不小于 0 的整数（分），留空 = 不封顶。";
      return null;
    }
    case "combo_discount":
      return isIntInRange(form.splitPlatformBps || "0", 0, 10000) ? null : "平台分摊万分比需为 0~10000 的整数。";
    case "channel_fee":
      return isIntInRange(form.feeBps, 0, 10000) ? null : "渠道抽成万分比需为 0~10000 的整数。";
    case "revenue_stream_attribution":
      return orUndef(form.managementEntityId) === undefined ? "归因管理公司 / 业态主体 id 必填。" : null;
    case "rank_tiers":
      return rankTierHint(tiers).ok ? null : rankTierHint(tiers).msg;
    case "commission_rate":
      return isIntInRange(form.rateBp, 0, 10000) ? null : "提成率万分比需为 0~10000 的整数（1500 = 15%）。";
    case "points_accrual_rate":
      return isIntInRange(form.amountCentPerPoint, 1, Number.MAX_SAFE_INTEGER)
        ? null : "每积 1 分所需消费金额需为大于 0 的整数（分）；100 = 每 1 元积 1 分。";
  }
}

// 构造落草案请求体（camelCase；adminClient 会归一为 snake_case 并注入 idempotency_key，此处仍显式带上）。
// 抛错=校验未过（提交前 GuidedFlowModal 已跑 validate，这里是二次防线）。
function buildBody(key: EditKey, form: Form, tiers: TierRow[], idempotencyKey: string): Record<string, unknown> {
  const invalid = validate(key, form, tiers);
  if (invalid) throw new Error(invalid);
  const project = orUndef(form.projectId);
  switch (key) {
    case "retention":
      return { idempotencyKey, projectId: project, retentionBps: Number(form.retentionBps) };
    case "distribution_method":
      return {
        idempotencyKey, projectId: project, methodType: form.methodType,
        specialApprovalTermMonths: Number(form.specialApprovalTermMonths || "3"),
      };
    case "merchant_rate":
      return {
        idempotencyKey, projectId: project, merchantId: form.merchantId.trim(),
        perGuestCent: orUndef(form.perGuestCent) === undefined ? null : Number(form.perGuestCent),
        fixedAmountCent: orUndef(form.fixedAmountCent) === undefined ? null : Number(form.fixedAmountCent),
      };
    case "comprehensive_service_fee":
      return {
        idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), entitlementType: orUndef(form.entitlementType),
        feeBps: form.feeBps.trim() === "" ? 0 : Number(form.feeBps),
        fixedAmountCent: orUndef(form.fixedAmountCent) === undefined ? null : Number(form.fixedAmountCent),
        monthlyCapAmountCent: orUndef(form.monthlyCapAmountCent) === undefined ? null : Number(form.monthlyCapAmountCent),
      };
    case "combo_discount":
      return { idempotencyKey, projectId: project, discountBearer: form.discountBearer, splitPlatformBps: Number(form.splitPlatformBps || "0") };
    case "channel_fee":
      return { idempotencyKey, projectId: project, channel: null, feeBps: Number(form.feeBps) };
    case "revenue_stream_attribution":
      return { idempotencyKey, projectId: project, revenueStream: form.revenueStream, managementEntityId: form.managementEntityId.trim() };
    case "rank_tiers":
      return { idempotencyKey, projectId: project, tiers: tiers.map((t) => ({ rankFrom: Number(t.rankFrom), rankTo: Number(t.rankTo), shareBps: Number(t.shareBps) })) };
    case "commission_rate":
      return { idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), roleType: form.roleType, rateBp: Number(form.rateBp) };
    case "points_accrual_rate":
      return { idempotencyKey, projectId: project, merchantId: orUndef(form.merchantId), amountCentPerPoint: Number(form.amountCentPerPoint) };
  }
}
