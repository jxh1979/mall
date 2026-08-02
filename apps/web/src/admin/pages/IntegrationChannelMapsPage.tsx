import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import { useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

// 批6 渠道映射维护：第三方渠道（美团/大众点评/抖音…）的「商品→本地票种」「门店→本地商户」两张翻译表。
// 票种映射：渠道团购卖出的通票回流时按它落成本地哪种票（未配置 → 回流拒收，票出不来）。
// 门店映射：餐饮/零售渠道回流按门店号归属到哪个商户（未配置 → 回流落「未归属」，进不了店级抽成/租售比）。

const TICKET_MAP_ENDPOINT = "/api/v1/integration/channel-ticket-type-maps";
const STORE_MAP_ENDPOINT = "/api/v1/integration/channel-store-maps";
const MANAGE_PERMISSION = "integration.channel_store_map.manage";

const CHANNELS = [
  { value: "meituan", label: "美团" },
  { value: "dianping", label: "大众点评" },
  { value: "douyin", label: "抖音" },
  { value: "koubei", label: "口碑" },
  { value: "eleme", label: "饿了么" },
  { value: "alipay", label: "支付宝" },
  { value: "wechat", label: "微信" },
] as const;

function channelLabel(value?: string): string {
  return CHANNELS.find((channel) => channel.value === value)?.label ?? (value ?? "—");
}

type TicketMapRow = {
  id: string;
  providerCode: string;
  channel: string;
  externalProductId: string;
  ticketTypeId: string;
  priceRuleId?: string | null;
  status: string;
  updatedAt?: string;
};

type StoreMapRow = {
  id: string;
  providerCode: string;
  channel: string;
  externalStoreId: string;
  merchantId: string;
  status: string;
  updatedAt?: string;
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

export function IntegrationChannelMapsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const canManage = useCan(effectivePermissions, [MANAGE_PERMISSION]);
  const [ticketMaps, setTicketMaps] = React.useState<TicketMapRow[]>([]);
  const [storeMaps, setStoreMaps] = React.useState<StoreMapRow[]>([]);
  const [formError, setFormError] = React.useState("");
  const [busy, setBusy] = React.useState("");

  // 票种映射录入表单。
  const [ticketForm, setTicketForm] = React.useState({
    providerCode: "", channel: "meituan", externalProductId: "", ticketTypeId: "", priceRuleId: "",
  });
  // 门店映射录入表单。
  const [storeForm, setStoreForm] = React.useState({
    providerCode: "", channel: "meituan", externalStoreId: "", merchantId: "",
  });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [tickets, stores] = await Promise.all([
        api.get<TicketMapRow[]>(TICKET_MAP_ENDPOINT),
        api.get<StoreMapRow[]>(STORE_MAP_ENDPOINT),
      ]);
      setTicketMaps(tickets ?? []);
      setStoreMaps(stores ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "渠道映射读取失败" },
  );

  async function runAction(action: string, request: Record<string, unknown>, call: () => Promise<unknown>) {
    setBusy(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await call();
      setActionResult({ action, request, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "渠道映射操作失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, status: "failed", message });
    } finally {
      setBusy("");
    }
  }

  async function saveTicketMap() {
    if (!ticketForm.providerCode.trim() || !ticketForm.externalProductId.trim() || !ticketForm.ticketTypeId.trim()) {
      setFormError("供应商、渠道商品编号、本地票种为必填。");
      return;
    }
    const request = {
      idempotency_key: nextKey("ticket-map"),
      provider_code: ticketForm.providerCode.trim(),
      channel: ticketForm.channel,
      external_product_id: ticketForm.externalProductId.trim(),
      ticket_type_id: ticketForm.ticketTypeId.trim(),
      price_rule_id: ticketForm.priceRuleId.trim() || null,
    };
    await runAction("channel_ticket_type_map_saved", request, () => api.post(TICKET_MAP_ENDPOINT, request));
    setTicketForm((current) => ({ ...current, externalProductId: "", ticketTypeId: "", priceRuleId: "" }));
  }

  async function saveStoreMap() {
    if (!storeForm.providerCode.trim() || !storeForm.externalStoreId.trim() || !storeForm.merchantId.trim()) {
      setFormError("供应商、渠道门店号、本地商户为必填。");
      return;
    }
    const request = {
      idempotency_key: nextKey("store-map"),
      provider_code: storeForm.providerCode.trim(),
      channel: storeForm.channel,
      external_store_id: storeForm.externalStoreId.trim(),
      merchant_id: storeForm.merchantId.trim(),
    };
    await runAction("channel_store_map_saved", request, () => api.post(STORE_MAP_ENDPOINT, request));
    setStoreForm((current) => ({ ...current, externalStoreId: "", merchantId: "" }));
  }

  async function archive(endpoint: string, action: string, id: string) {
    const request = {
      idempotency_key: nextKey("map-archive"),
      reason: "后台停用渠道映射（停用后该渠道对应回流按未配置处理）",
    };
    await runAction(action, { id, ...request }, () => api.post(`${endpoint}/${id}/archive`, request));
  }

  return (
    <section data-testid="integration-channel-maps-page">
      <AdminPageHeader
        title="渠道映射维护"
        description="第三方渠道（美团/大众点评/抖音等）回流的两张翻译表：票种映射决定渠道团购卖出的通票落成本地哪种票（未配置则通票回流拒收）；门店映射决定餐饮/零售回流归属到哪个商户（未配置则落未归属，进不了店级抽成与租售比）。由招商签约时录入、运营后台补录。"
      />
      {formError ? <p className="admin-form-error" data-testid="channel-maps-error">{formError}</p> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>渠道票种映射（第三方商品 → 本地票种）</h3>
        <div className="admin-form-row" data-testid="ticket-map-form">
          <input
            placeholder="供应商编码（如 meituan-groupbuy）"
            value={ticketForm.providerCode}
            onChange={(event) => setTicketForm({ ...ticketForm, providerCode: event.target.value })}
            data-testid="ticket-map-provider"
          />
          <select
            value={ticketForm.channel}
            onChange={(event) => setTicketForm({ ...ticketForm, channel: event.target.value })}
            data-testid="ticket-map-channel"
          >
            {CHANNELS.map((channel) => (
              <option key={channel.value} value={channel.value}>{channel.label}</option>
            ))}
          </select>
          <input
            placeholder="渠道商品编号（团购套餐 id）"
            value={ticketForm.externalProductId}
            onChange={(event) => setTicketForm({ ...ticketForm, externalProductId: event.target.value })}
            data-testid="ticket-map-product"
          />
          <input
            placeholder="本地票种 id"
            value={ticketForm.ticketTypeId}
            onChange={(event) => setTicketForm({ ...ticketForm, ticketTypeId: event.target.value })}
            data-testid="ticket-map-ticket-type"
          />
          <input
            placeholder="价格规则 id（选填=渠道团购价）"
            value={ticketForm.priceRuleId}
            onChange={(event) => setTicketForm({ ...ticketForm, priceRuleId: event.target.value })}
            data-testid="ticket-map-price-rule"
          />
          <button
            type="button"
            disabled={!canManage || busy !== ""}
            onClick={() => void saveTicketMap()}
            data-testid="ticket-map-save"
            title={canManage ? undefined : "当前账号无渠道映射维护权限"}
          >
            登记/改绑
          </button>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-map-table">
            <thead>
              <tr>
                <th>渠道</th>
                <th>供应商</th>
                <th>渠道商品编号</th>
                <th>本地票种</th>
                <th>价格规则</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {ticketMaps.length === 0 ? (
                <tr><td colSpan={7}>暂无票种映射。渠道售出的通票回流前必须先在此登记映射，否则回流拒收。</td></tr>
              ) : ticketMaps.map((row) => (
                <tr key={row.id} data-testid={`ticket-map-row-${row.externalProductId}`}>
                  <td>{channelLabel(row.channel)}</td>
                  <td>{row.providerCode}</td>
                  <td>{row.externalProductId}</td>
                  <td>{row.ticketTypeId}</td>
                  <td>{row.priceRuleId ?? "门市价"}</td>
                  <td>{row.status === "active" ? "生效" : "已停用"}</td>
                  <td>
                    {row.status === "active" ? (
                      <button
                        type="button"
                        disabled={!canManage || busy !== ""}
                        onClick={() => void archive(TICKET_MAP_ENDPOINT, "channel_ticket_type_map_archived", row.id)}
                        data-testid={`ticket-map-archive-${row.externalProductId}`}
                      >
                        停用
                      </button>
                    ) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>渠道门店映射（第三方门店 → 本地商户）</h3>
        <div className="admin-form-row" data-testid="store-map-form">
          <input
            placeholder="供应商编码"
            value={storeForm.providerCode}
            onChange={(event) => setStoreForm({ ...storeForm, providerCode: event.target.value })}
            data-testid="store-map-provider"
          />
          <select
            value={storeForm.channel}
            onChange={(event) => setStoreForm({ ...storeForm, channel: event.target.value })}
            data-testid="store-map-channel"
          >
            {CHANNELS.map((channel) => (
              <option key={channel.value} value={channel.value}>{channel.label}</option>
            ))}
          </select>
          <input
            placeholder="渠道门店号"
            value={storeForm.externalStoreId}
            onChange={(event) => setStoreForm({ ...storeForm, externalStoreId: event.target.value })}
            data-testid="store-map-store"
          />
          <input
            placeholder="本地商户 id"
            value={storeForm.merchantId}
            onChange={(event) => setStoreForm({ ...storeForm, merchantId: event.target.value })}
            data-testid="store-map-merchant"
          />
          <button
            type="button"
            disabled={!canManage || busy !== ""}
            onClick={() => void saveStoreMap()}
            data-testid="store-map-save"
            title={canManage ? undefined : "当前账号无渠道映射维护权限"}
          >
            登记/改绑
          </button>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="store-map-table">
            <thead>
              <tr>
                <th>渠道</th>
                <th>供应商</th>
                <th>渠道门店号</th>
                <th>本地商户</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {storeMaps.length === 0 ? (
                <tr><td colSpan={6}>暂无门店映射。第三方回流带门店号时按此表归属商户，未配置则落「未归属」待人工补录。</td></tr>
              ) : storeMaps.map((row) => (
                <tr key={row.id} data-testid={`store-map-row-${row.externalStoreId}`}>
                  <td>{channelLabel(row.channel)}</td>
                  <td>{row.providerCode}</td>
                  <td>{row.externalStoreId}</td>
                  <td>{row.merchantId}</td>
                  <td>{row.status === "active" ? "生效" : "已停用"}</td>
                  <td>
                    {row.status === "active" ? (
                      <button
                        type="button"
                        disabled={!canManage || busy !== ""}
                        onClick={() => void archive(STORE_MAP_ENDPOINT, "channel_store_map_archived", row.id)}
                        data-testid={`store-map-archive-${row.externalStoreId}`}
                      >
                        停用
                      </button>
                    ) : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
