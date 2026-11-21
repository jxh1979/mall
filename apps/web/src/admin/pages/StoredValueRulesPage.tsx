// 文件版本：V0.04
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { accountFormOf } from "../adminRoutes";
import { ExitNoticePolicySection } from "./CustomerFacingSettingsSections";

// 储值规则配置（会员忠诚度 PRD §7 储值）。后端既有端点（商户级卡三个只限发卡商户本商户账号，第十八批 CL 段 423-R1）：
//   GET  /api/v1/loyalty/stored-value/merchant-refund-rules/{merchant_id}（合并返回发卡+资金+扣款顺序+退款规则）
//   POST /api/v1/loyalty/stored-value/merchant-card-config（发卡资格 + 资金模式 + 扣款顺序）
//   POST /api/v1/loyalty/stored-value/merchant-refund-rules（可退 + 退款比例）
// 第十八批第 11 条：通用卡与商户级卡充值赠送比例在「门票积分、成长值与销售责任」页提交审批（BSM L521 涉钱配置版本化），本页不再直接保存。
type MerchantCardRule = {
  merchantId?: string | null;
  refundAllowed?: boolean;
  refundRatioBps?: number;
  cardIssuanceEnabled?: boolean;
  fundOwnership?: string;
  // CG-06a：商户级卡消费扣款顺序（BSM「支付方式」：由发卡商户自定、由发卡商户在商户后台自行设置）。
  consumeOrder?: string;
};
const CONSUME_ORDERS: Array<[string, string]> = [
  ["principal_first", "先扣本金、后扣赠送金"],
  ["bonus_first", "先扣赠送金、后扣本金"],
];
function consumeOrderLabel(code?: string | null): string {
  return CONSUME_ORDERS.find(([c]) => c === code)?.[1] ?? "先扣本金、后扣赠送金";
}

const MANAGE = ["customer.stored_value.refund"];
// 18CL 收尾复核（PGM:291「用户看到的每一处文字都是中文，含……提示、报错」）：保存被拒（如 422-R1 持股过半不得发卡 409）报出后端中文提示，照 StoredValueRefundPage。
const errorText = (error: unknown, fallback: string) => (error instanceof Error ? error.message : fallback);

// 第十八批第 11 条（BSM L833「商户级卡的充值资金归发卡商户预收，平台不代收」；DEC-CFG-2026-08-03-11 关闭平台代收开关）。
const FUND_OWNERSHIPS: Array<[string, string]> = [
  ["merchant_prepaid", "商户预收（商户自收，客人充值直接进商户账户）"],
];
function fundLabel(code?: string | null): string {
  return FUND_OWNERSHIPS.find(([c]) => c === code)?.[1] ?? code ?? "—";
}

/**
 * 储值规则配置页（ADM-CUS-005）：储值卡两类——① 综合体通用卡：平台缺省账户，全场通用，无商户级配置；
 * ② 商户级卡：发卡商户设发卡资格、资金模式（充值入账归属）、扣款顺序与可退规则。
 * 第十八批 CL 段 423-R2（第十八批裁决 79：ENUM:86、ENUM:107、BSM:1066「由发卡商户在商户后台自行设置」；BSM:833「平台各岗位（含最高权限）一律看不到
 * 商户级卡……」）：商户账号打开即读、设本商户的卡；平台账号只留通用卡说明与退出公示天数，不再出现商户级卡配置。
 * 查看/维护均需储值退款维护权限（customer.stored_value.refund）。
 */
export function StoredValueRulesPage({ api, user, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const merchantId = accountFormOf(user) === "merchant" ? user.merchantId ?? "" : "";
  const [rule, setRule] = React.useState<MerchantCardRule | null>(null);
  const [cardEnabled, setCardEnabled] = React.useState(false);
  const [fundOwnership, setFundOwnership] = React.useState("merchant_prepaid");
  const [refundAllowed, setRefundAllowed] = React.useState(true);
  const [refundPct, setRefundPct] = React.useState("100");
  const [consumeOrder, setConsumeOrder] = React.useState("principal_first");

  const { loading, error, reload } = useAdminResource(
    async () => {
      if (!merchantId) { setRule(null); return; }
      const r = await api.get<MerchantCardRule>(`/api/v1/loyalty/stored-value/merchant-refund-rules/${encodeURIComponent(merchantId)}`);
      setRule(r ?? null);
      setCardEnabled(Boolean(r?.cardIssuanceEnabled));
      setFundOwnership(r?.fundOwnership ?? "merchant_prepaid");
      setRefundAllowed(r?.refundAllowed ?? true);
      setRefundPct(r?.refundRatioBps == null ? "100" : String(r.refundRatioBps / 100));
      setConsumeOrder(r?.consumeOrder ?? "principal_first");
    },
    { onError: setGlobalError, fallbackMessage: "商户储值卡规则读取失败" },
  );

  async function saveCardConfig() {
    try {
      await api.post("/api/v1/loyalty/stored-value/merchant-card-config", {
        merchantId: merchantId.trim(), cardIssuanceEnabled: cardEnabled, fundOwnership, consumeOrder,
      });
    } catch (error) {
      setGlobalError(errorText(error, "发卡设置保存失败"));
      return;
    }
    setActionResult({ action: "stored_value_card_config_saved", merchantId: merchantId.trim(), cardIssuanceEnabled: cardEnabled, fundOwnership, consumeOrder });
    reload();
  }

  async function saveRefundRule() {
    const pct = Number(refundPct);
    if (!Number.isFinite(pct) || pct < 0 || pct > 100) { setGlobalError("可退比例按百分比填写（0-100）"); return; }
    try {
      await api.post("/api/v1/loyalty/stored-value/merchant-refund-rules", {
        merchantId: merchantId.trim(), refundAllowed, refundRatioBps: Math.round(pct * 100),
      });
    } catch (error) {
      setGlobalError(errorText(error, "退款规则保存失败"));
      return;
    }
    setActionResult({ action: "stored_value_refund_rule_saved", merchantId: merchantId.trim(), refundAllowed, refundRatioBps: Math.round(pct * 100) });
    reload();
  }

  return (
    <section data-testid="stored-value-rules-page">
      <AdminPageHeader
        title="储值规则配置"
        description="储值卡两类：① 综合体通用卡为平台缺省账户、全场通用、无商户级配置；② 商户级卡由发卡商户在商户后台自行设置发卡资格、资金模式（充值入账归属）、扣款顺序与可退规则，不经平台。充值必须支付成功才入账，账户可冻结/解冻。"
      />
      {!merchantId ? (
        <>
          <p className="admin-muted-action">综合体通用卡充值赠送比例在「门票积分、成长值与销售责任」页提交审批（按项目可配，须同时配置上下限；通用卡扣款顺序写死先本金后赠送金）。</p>
          <ExitNoticePolicySection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
        </>
      ) : (
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
          <>
            <div className="admin-kpi-grid" data-testid="stored-value-current">
              <div className="admin-kpi-card"><span>发卡资格</span><strong>{rule?.cardIssuanceEnabled ? "已开通" : "未开通"}</strong></div>
              <div className="admin-kpi-card"><span>资金模式</span><strong>{fundLabel(rule?.fundOwnership)}</strong></div>
              <div className="admin-kpi-card"><span>可退</span><strong>{rule?.refundAllowed === false ? "拒退" : "允许"}</strong></div>
              <div className="admin-kpi-card"><span>可退比例</span><strong>{rule?.refundRatioBps == null ? "—" : `${(rule.refundRatioBps / 100).toFixed(0)}%`}</strong></div>
              <div className="admin-kpi-card"><span>扣款顺序</span><strong>{consumeOrderLabel(rule?.consumeOrder)}</strong></div>
            </div>

            <h3>发卡与消费扣款顺序（由本商户自行设置；资金只可商户预收，平台不代收）</h3>
            <p className="admin-muted-action">本商户储值卡充值赠送比例在「门票积分、成长值与销售责任」页由本商户提交，经本商户有审批权的人转正后生效（赠送金由本商户承担）。</p>
            <div className="admin-form-grid" data-testid="stored-value-card-config-form">
              <label>是否发行本商户储值卡
                <select aria-label="发卡资格" value={cardEnabled ? "on" : "off"} onChange={(e) => setCardEnabled(e.target.value === "on")}>
                  <option value="off">未开通（客人不能给本商户卡充值）</option>
                  <option value="on">已开通（可发卡充值）</option>
                </select>
              </label>
              <label>资金模式
                <select aria-label="资金模式" value={fundOwnership} onChange={(e) => setFundOwnership(e.target.value)}>
                  {FUND_OWNERSHIPS.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                </select>
              </label>
              <label>消费扣款顺序
                <select aria-label="消费扣款顺序" value={consumeOrder} onChange={(e) => setConsumeOrder(e.target.value)}>
                  {CONSUME_ORDERS.map(([c, l]) => <option key={c} value={c}>{l}</option>)}
                </select>
              </label>
              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="stored-value-card-config-save" onClick={() => void saveCardConfig()}>保存发卡、资金模式与扣款顺序</PermissionButton>
            </div>

            <h3>退款规则（发卡商户可设可否退与退回比例，余下为商户留存）</h3>
            <div className="admin-form-grid" data-testid="stored-value-refund-rule-form">
              <label>是否可退
                <select aria-label="是否可退" value={refundAllowed ? "on" : "off"} onChange={(e) => setRefundAllowed(e.target.value === "on")}>
                  <option value="on">允许退款</option>
                  <option value="off">拒绝退款</option>
                </select>
              </label>
              <label>可退比例(%)<input aria-label="可退比例" type="number" min="0" max="100" placeholder="100" value={refundPct} onChange={(e) => setRefundPct(e.target.value)} disabled={!refundAllowed} /></label>
              <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="stored-value-refund-rule-save" onClick={() => void saveRefundRule()}>保存退款规则</PermissionButton>
            </div>
          </>
      </AdminDataState>
      )}
    </section>
  );
}
