import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

// 3.10 票务策略配置化：票种/通票、权益模板、票种↔权益组装（通票）、派生价规则全部后台可配，
// 替代旧骨架页（名称写死、派生规则只是文案）。请求字段对齐后端真实契约（snake_case）。
type TicketTypeRow = {
  id: string;
  ticketName: string;
  weekdayPriceCent: number | null;
  weekendPriceCent: number | null;
  holidayPriceCent: number | null;
  status: string;
  stockQuantity: number | null;
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
};

const MANAGE = ["ticket.product.manage"];
const VARIANT_TYPES = ["new_customer", "student", "group", "channel", "member"];
const VARIANT_LABELS: Record<string, string> = {
  new_customer: "新人票",
  student: "学生票",
  group: "多人票",
  channel: "渠道价",
  member: "会员价",
};
const ENTITLEMENT_TYPES = ["entry", "ride", "gift_drink", "bar", "amusement"];

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

  // 区1 新建票种（保留既有 e2e 锚点：三档价 + ticket-publish + ticket-policy-error）
  const [ticketName, setTicketName] = React.useState("后台通票策略");
  const [weekday, setWeekday] = React.useState("");
  const [weekend, setWeekend] = React.useState("");
  const [holiday, setHoliday] = React.useState("");
  const [stock, setStock] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  // 区2 新建权益模板
  const [tplForm, setTplForm] = React.useState({ name: "", entitlementType: "entry", totalTimes: "1", valueYuan: "" });
  // 区3 通票组装（票种绑权益）
  const [bindForm, setBindForm] = React.useState({ ticketTypeId: "", templateId: "", quantity: "1", totalTimes: "1", numPeople: "1" });
  // 区4 派生价规则
  const [ruleForm, setRuleForm] = React.useState({ ticketTypeId: "", variantType: "new_customer", priceMode: "derive_from_calendar_price", discount: "0.8", fixedYuan: "" });

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
      const [ticketRows, templateRows, ruleRows] = await Promise.all([
        api.get<unknown>("/api/v1/ticketing/ticket-types"),
        api.get<unknown>("/api/v1/ticketing/entitlement-templates"),
        api.get<unknown>("/api/v1/ticketing/price-rules"),
      ]);
      setTickets(toRows<TicketTypeRow>(ticketRows));
      setTemplates(toRows<EntitlementTemplateRow>(templateRows));
      setRules(toRows<PriceRuleRow>(ruleRows));
    },
    { onError: setGlobalError, fallbackMessage: "票务策略配置读取失败" },
  );

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
    setError("");
    setBusy(true);
    try {
      const ticket = await api.post<{ id: string }>("/api/v1/ticketing/ticket-types", {
        idempotency_key: crypto.randomUUID(),
        ticket_name: ticketName.trim(),
        base_calendar_price_mode: "calendar",
        weekday_price_cent: Math.round(prices.平日价 * 100),
        weekend_price_cent: Math.round(prices.周末价 * 100),
        holiday_price_cent: Math.round(prices.节假日价 * 100),
        stock_quantity: stock.trim() ? Number(stock) : null,
        status: "active",
      });
      const published = await api.post(`/api/v1/ticketing/ticket-types/${ticket.id ?? "ticket-1"}/publish`, {});
      setActionResult({ action: "ticket_policy_publish", status: "published", published });
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

  return (
    <section data-testid="ticketing-policies-page">
      <AdminPageHeader
        title="票务策略"
        description="票种/通票、权益、适用范围与派生价规则全部在此配置：先建票种并发布，再建权益模板并组装成通票，最后配派生价（新人/学生/多人/渠道/会员）。"
      />

      {focusObjectId && (
        <div className="admin-state" data-testid="ticketing-policies-focus">已从下钻定位到票种：{focusObjectId}（下表高亮行）</div>
      )}
      <h3>票种 / 通票</h3>
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
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {tickets.map((row) => (
                <tr key={row.id} data-testid={`ticket-type-${row.id}`}
                  className={`admin-row-clickable${focusObjectId === row.id ? " admin-row-focus" : ""}`} tabIndex={0}
                  onClick={() => openTicketTypeDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTicketTypeDetail(row); } }}>
                  <td>{row.ticketName}</td>
                  <td>{yuan(row.weekdayPriceCent)}</td>
                  <td>{yuan(row.weekendPriceCent)}</td>
                  <td>{yuan(row.holidayPriceCent)}</td>
                  <td>{row.stockQuantity ?? "不限"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <div className="admin-form-row">
        <input data-testid="ticket-name" value={ticketName} onChange={(event) => setTicketName(event.target.value)} placeholder="票种名称" aria-label="票种名称" />
        <input data-testid="ticket-weekday-price" type="number" min={0} step="0.01" value={weekday} onChange={(event) => setWeekday(event.target.value)} placeholder="平日价" />
        <input data-testid="ticket-weekend-price" type="number" min={0} step="0.01" value={weekend} onChange={(event) => setWeekend(event.target.value)} placeholder="周末价" />
        <input data-testid="ticket-holiday-price" type="number" min={0} step="0.01" value={holiday} onChange={(event) => setHoliday(event.target.value)} placeholder="节假日价" />
        <input data-testid="ticket-stock" type="number" min={0} value={stock} onChange={(event) => setStock(event.target.value)} placeholder="限量（可空=不限）" aria-label="限量" />
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ticket-publish" onClick={() => void publish()} disabled={busy}>
          发布策略
        </PermissionButton>
      </div>
      <div data-testid="ticket-derived-preview" className="admin-preview">
        新人票、学生票、多人票、渠道价、会员价在下方「派生价规则」区配置：按基础价折扣派生或固定价。
      </div>
      {error ? <div data-testid="ticket-policy-error" className="admin-inline-error">{error}</div> : null}

      <h3>权益模板</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="entitlement-templates-table">
          <thead>
            <tr>
              <th>模板名称</th>
              <th>权益类型</th>
              <th>状态</th>
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
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
