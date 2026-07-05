import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, MerchantZoneFeeRecipientRow } from "../types";

const MANAGE_PERMS = ["merchant.portal.fee_recipient.manage"];

const TEXT = {
  title: "费用通知",
  description: "自助维护本商户费用短信 / 站内通知的接收人:按费用事件(租金 / 能耗 / 物业 / 续约)各设一位接收人,停用后回落商户首要联系人。",
  loadFailed: "接收人读取失败",
  actionFailed: "接收人保存失败",
  nameRequired: "接收人姓名必填。",
  phoneRequired: "接收人手机号必填。",
  save: "保存接收人",
  deactivate: "停用",
  focusClear: "清除筛选，返回全部",
};

const FEE_EVENT_LABELS: Record<string, string> = {
  rent: "租金",
  utilities: "能耗",
  property: "物业",
  renewal: "续约",
};

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染接收人表对应子集。
// URL 约定：?kpi=recipients[&subset=active]。表内停用操作按钮保留。
type KpiFocus = { subset: "all" | "active" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "recipients") return null;
  return { subset: params.get("subset") === "active" ? "active" : "all" };
}

export function MerchantZoneFeeRecipientsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantZoneFeeRecipientRow[]>([]);
  const [feeEvent, setFeeEvent] = React.useState("rent");
  const [name, setName] = React.useState("");
  const [phone, setPhone] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: "all" | "active") {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "recipients");
    if (subset === "active") params.set("subset", "active");
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<MerchantZoneFeeRecipientRow[]>("/api/v1/merchant-billing/fee-notification-recipients"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function saveRecipient() {
    if (!name.trim()) {
      setFormError(TEXT.nameRequired);
      return;
    }
    if (!phone.trim()) {
      setFormError(TEXT.phoneRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-zone-fee-recipient"), feeEvent, recipientName: name, recipientPhone: phone };
    await runAction("merchant_zone_fee_recipient_saved", request,
      () => api.post("/api/v1/merchant-billing/fee-notification-recipients", request));
    setName("");
    setPhone("");
  }

  async function deactivateRecipient(id: string) {
    const request = { idempotencyKey: nextKey("merchant-zone-fee-recipient-off"), id };
    await runAction("merchant_zone_fee_recipient_deactivated", request,
      () => api.post(`/api/v1/merchant-billing/fee-notification-recipients/${id}/deactivate`, {}));
  }

  const activeRows = rows.filter((row) => row.status === "active");
  const focusRows = kpiFocus?.subset === "active" ? activeRows : rows;

  const recipientsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="merchant-zone-fee-recipients-table">
        <thead>
          <tr><th>费用事件</th><th>接收人</th><th>手机号</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          {(kpiFocus ? focusRows : rows).map((row) => (
            <tr key={row.id}>
              <td>{FEE_EVENT_LABELS[row.feeEvent ?? ""] ?? row.feeEvent ?? "-"}</td>
              <td>{row.recipientName ?? "-"}</td>
              <td>{row.recipientPhone ?? "-"}</td>
              <td>{row.status === "active" ? "生效" : "已停用"}</td>
              <td>
                {row.status === "active" ? (
                  <PermissionButton
                    permissionsState={effectivePermissions}
                    permissions={MANAGE_PERMS}
                    data-testid={`merchant-zone-fee-recipient-off-${row.id}`}
                    disabled={busyAction === "merchant_zone_fee_recipient_deactivated"}
                    onClick={() => deactivateRecipient(row.id)}
                  >{TEXT.deactivate}</PermissionButton>
                ) : "-"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <section data-testid="merchant-zone-fee-recipients-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="merchant_billing.fee_recipient.upserted / merchant_billing.fee_recipient.deactivated" />
      <div className="admin-form-row" data-testid="merchant-zone-fee-recipient-form">
        <label>费用事件<select data-testid="merchant-zone-fee-recipient-event" value={feeEvent} onChange={(event) => setFeeEvent(event.target.value)}>
          {Object.entries(FEE_EVENT_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select></label>
        <label>接收人姓名<input data-testid="merchant-zone-fee-recipient-name" value={name} onChange={(event) => setName(event.target.value)} placeholder="如:王店长" /></label>
        <label>接收手机号<input data-testid="merchant-zone-fee-recipient-phone" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="11 位手机号" /></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="merchant-zone-fee-recipient-save"
          disabled={busyAction === "merchant_zone_fee_recipient_saved"}
          onClick={saveRecipient}
        >{TEXT.save}</PermissionButton>
      </div>
      {formError ? <div className="admin-form-error" data-testid="merchant-zone-fee-recipient-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="merchant-zone-fee-recipients-kpi-focus-banner">
              正在查看：{kpiFocus.subset === "active" ? "生效中接收人" : "接收人配置"}（{focusRows.length}）
              <button type="button" data-testid="merchant-zone-fee-recipients-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {recipientsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-zone-fee-recipients-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>接收人配置</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-fee-recipients-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>本商户</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>生效中</span><button type="button" className="admin-kpi-value" data-testid="merchant-zone-fee-recipients-kpi-active" onClick={() => enterKpiFocus("active")}>{activeRows.length}</button><small>按费用事件</small></div>
            </div>
            {recipientsTable}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
