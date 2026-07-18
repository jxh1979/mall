import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
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
  // 引导式流程 phase-6：新增接收人收起为主按钮 + 分步引导弹窗（停用是行内动作不动）。
  const [recipientOpen, setRecipientOpen] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该接收人完整字段（费用事件/状态中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openRecipientDetail(row: MerchantZoneFeeRecipientRow) {
    setDetail({
      title: `费用接收人明细 · ${row.recipientName ?? row.id}`,
      fields: [
        { label: "费用事件", value: FEE_EVENT_LABELS[row.feeEvent ?? ""] ?? row.feeEvent ?? "—" },
        { label: "接收人", value: row.recipientName ?? "—" },
        { label: "手机号", value: row.recipientPhone ?? "—" },
        { label: "状态", value: row.status === "active" ? "生效" : "已停用" },
        { label: "接收人编号", value: row.id },
      ],
    });
  }

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
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
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

  // 接收人校验（步内 + 提交双保险）：姓名 + 手机号都必填。
  function recipientInvalid() {
    if (!name.trim()) return TEXT.nameRequired;
    if (!phone.trim()) return TEXT.phoneRequired;
    return null;
  }

  async function saveRecipient() {
    const invalid = recipientInvalid();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-zone-fee-recipient"), feeEvent, recipientName: name, recipientPhone: phone };
    const ok = await runAction("merchant_zone_fee_recipient_saved", request,
      () => api.post("/api/v1/merchant-billing/fee-notification-recipients", request));
    if (ok) {
      setName("");
      setPhone("");
      setRecipientOpen(false);
    }
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
          <tr><th>费用事件</th><th>接收人</th><th>手机号</th><th>状态</th><th>操作</th><th>详情</th></tr>
        </thead>
        <tbody>
          {(kpiFocus ? focusRows : rows).map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-zone-fee-recipient-row-${row.id}`}
              onClick={() => openRecipientDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openRecipientDetail(row); } }}>
              <td>{FEE_EVENT_LABELS[row.feeEvent ?? ""] ?? row.feeEvent ?? "-"}</td>
              <td>{row.recipientName ?? "-"}</td>
              <td>{row.recipientPhone ?? "-"}</td>
              <td>{row.status === "active" ? "生效" : "已停用"}</td>
              <td onClick={(event) => event.stopPropagation()}>
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
            <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看费用接收人详情" onClick={(event) => { event.stopPropagation(); openRecipientDetail(row); }}>查看详情</button></td></tr>
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
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="merchant-zone-fee-recipient-create-open"
          onClick={() => { setFormError(""); setRecipientOpen(true); }}
        >＋ 新增接收人</PermissionButton>
        <span className="admin-muted-action">按费用事件（租金 / 能耗 / 物业 / 续约）各设一位接收人，停用后回落商户首要联系人。</span>
      </div>
      <GuidedFlowModal
        open={recipientOpen}
        title="新增费用通知接收人"
        testId="merchant-zone-fee-recipient-guided"
        submitTestId="merchant-zone-fee-recipient-save"
        submitLabel={TEXT.save}
        submitting={busyAction === "merchant_zone_fee_recipient_saved"}
        error={formError || undefined}
        onClose={() => setRecipientOpen(false)}
        onSubmit={() => void saveRecipient()}
        steps={[
          {
            title: "接收人信息",
            description: "选择费用事件并填写接收人姓名与手机号。",
            validate: () => recipientInvalid(),
            content: (
              <>
                <label>费用事件<select data-testid="merchant-zone-fee-recipient-event" value={feeEvent} onChange={(event) => setFeeEvent(event.target.value)}>
                  {Object.entries(FEE_EVENT_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>接收人姓名<input data-testid="merchant-zone-fee-recipient-name" value={name} onChange={(event) => setName(event.target.value)} placeholder="如:王店长" /></label>
                <label>接收手机号<input data-testid="merchant-zone-fee-recipient-phone" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="11 位手机号" /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "费用事件", value: FEE_EVENT_LABELS[feeEvent] ?? feeEvent },
          { label: "接收人姓名", value: name.trim() || "未填写" },
          { label: "接收手机号", value: phone.trim() || "未填写" },
        ]}
      />
      {formError && !recipientOpen ? <div className="admin-form-error" data-testid="merchant-zone-fee-recipient-form-error">{formError}</div> : null}
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
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
