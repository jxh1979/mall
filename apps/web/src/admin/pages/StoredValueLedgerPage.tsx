// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { CnDatePicker } from "../components/CnDatePicker";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 储值台账一行（后端 StoredValueLedgerService.LedgerRow，unwrap 已 camel 化）；手机号只给掩码。
type LedgerRow = {
  txnId: string;
  createdAt: string;
  txnType: string;
  amountCent: number;
  principalCent: number;
  bonusCent: number;
  balanceAfterCent: number;
  principalAfterCent: number;
  bonusAfterCent: number;
  accountId: string;
  cardType: string;
  projectId: string | null;
  issuerMerchantId: string | null;
  issuerMerchantName: string | null;
  customerId: string;
  maskedPhone: string | null;
  paymentChannel: string | null;
  consumeMerchantId: string | null;
  consumeMerchantName: string | null;
  originalTxnId: string | null;
  reason: string | null;
};

const QUERY = "/api/v1/loyalty/stored-value/ledger/query";
const VIEW = ["customer.stored_value.view"];
const SIZE = 20;
const TXN_LABELS: Record<string, string> = { topup: "充值", consume: "消费", consume_reversal: "退款回冲", refund: "退卡", adjust: "调整" };
const yuan = (cent: number) => (cent / 100).toFixed(2);
const errorText = (error: unknown, fallback: string) => (error instanceof Error ? error.message : fallback);
// 时间按北京时间显示（同 ExceptionAlertsPage）。
const bjTime = (iso: string) => new Date(iso).toLocaleString("zh-CN",
  { timeZone: "Asia/Shanghai", year: "numeric", month: "long", day: "numeric", hour: "2-digit", minute: "2-digit", hour12: false });
const bjDate = (d: Date) => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(d);

/**
 * 储值台账（ADM-CUS-007）。第十八批 BD 段第 364 条 364-R2（BSM:833「运营端提供储值台账，可按客人、商户、期间查询逐笔明细与余额构成。」）：
 * 按客人完整手机号、商户编号、起止日期（北京时间，含首尾，缺省本月 1 日至今天）查逐笔，列本金与赠送金变动及变动后的本金、赠送金与余额；
 * 范围由后端按当前账号定（同退卡查卡）。打开页面不自动查询。页上有客人掩码手机号，属敏感页（PGM:687）。
 */
export function StoredValueLedgerPage({ api, setGlobalError, effectivePermissions, reportViewedObjects }: AdminPageProps) {
  const today = bjDate(new Date());
  const [phone, setPhone] = React.useState("");
  const [merchantId, setMerchantId] = React.useState("");
  const [dateFrom, setDateFrom] = React.useState(today.slice(0, 8) + "01");
  const [dateTo, setDateTo] = React.useState(today);
  // 后台客户端把分页信封拍平成本页各行（adminClient.unwrap），翻页按本页是否满一页判断。
  const [result, setResult] = React.useState<LedgerRow[] | null>(null);
  const [page, setPage] = React.useState(1);
  const [loading, setLoading] = React.useState(false);
  const rows = result ?? [];
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」），按卡去重。
  React.useEffect(() => {
    const cards = new Map((result ?? []).map((row) =>
      [row.accountId, { id: row.accountId, customerId: row.customerId, merchantId: row.issuerMerchantId }]));
    reportViewedObjects?.("customer_stored_value", [...cards.values()]);
  }, [result, reportViewedObjects]);

  async function search(target: number) {
    setLoading(true);
    try {
      // AUD-020：手机号属个人信息，走请求体，不进 URL。
      const body = {
        ...(phone.trim() ? { phone: phone.trim() } : {}),
        ...(merchantId.trim() ? { merchantId: merchantId.trim() } : {}),
        ...(dateFrom ? { dateFrom } : {}),
        ...(dateTo ? { dateTo } : {}),
        page: target,
        size: SIZE,
      };
      setResult((await api.post<LedgerRow[]>(QUERY, body)) ?? []);
      setPage(target);
    } catch (error) {
      setGlobalError(errorText(error, "储值台账查询失败"));
    } finally {
      setLoading(false);
    }
  }

  const consumeRow = (row: LedgerRow) => row.txnType === "consume" || row.txnType === "consume_reversal";

  return (
    <section data-testid="stored-value-ledger-page">
      <AdminPageHeader
        title="储值台账"
        description="运营端储值台账：按客人（完整手机号）、商户、期间查询储值逐笔明细；每笔列出本金与赠送金变动及变动后的本金、赠送金与余额。"
      />
      <div className="admin-form-row" style={{ display: "flex", gap: 12, alignItems: "flex-end", flexWrap: "wrap" }}>
        <label>客人手机号（选填，完整精确匹配）
          <input aria-label="客人手机号" type="tel" autoComplete="off" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </label>
        <label>商户编号（选填，发卡或扣款商户）
          <input aria-label="商户编号" value={merchantId} onChange={(e) => setMerchantId(e.target.value)} />
        </label>
        <label>起始日期
          <CnDatePicker value={dateFrom} onChange={setDateFrom} data-testid="stored-value-ledger-from" aria-label="起始日期" today={today} />
        </label>
        <label>结束日期
          <CnDatePicker value={dateTo} onChange={setDateTo} data-testid="stored-value-ledger-to" aria-label="结束日期" today={today} />
        </label>
        <PermissionButton permissionsState={effectivePermissions} permissions={VIEW} data-testid="stored-value-ledger-query"
          disabled={loading} onClick={() => void search(1)}>{loading ? "查询中…" : "查询"}</PermissionButton>
      </div>
      {result !== null && rows.length === 0 && <p data-testid="stored-value-ledger-empty">无符合条件的流水</p>}
      {rows.length > 0 && (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="stored-value-ledger-table">
            <thead>
              <tr><th>时间</th><th>客人</th><th>卡</th><th>类型</th><th>金额（元）</th><th>本金变动</th><th>赠送金变动</th>
                <th>变动后本金</th><th>变动后赠送金</th><th>变动后余额</th><th>扣款商户</th><th>支付通路</th><th>说明</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.txnId}>
                  <td>{bjTime(row.createdAt)}</td>
                  <td>{row.maskedPhone ?? "—"}</td>
                  <td>{row.cardType === "general" ? "综合体通用卡" : `商户卡·${row.issuerMerchantName ?? row.issuerMerchantId ?? ""}`}</td>
                  <td>{TXN_LABELS[row.txnType] ?? formatBusinessStatus(row.txnType)}</td>
                  <td>{yuan(row.amountCent)}</td>
                  <td>{yuan(row.principalCent)}</td>
                  <td>{yuan(row.bonusCent)}</td>
                  <td>{yuan(row.principalAfterCent)}</td>
                  <td>{yuan(row.bonusAfterCent)}</td>
                  <td>{yuan(row.balanceAfterCent)}</td>
                  <td>{consumeRow(row) ? row.consumeMerchantName ?? row.consumeMerchantId ?? "综合体" : "—"}</td>
                  <td>{row.txnType === "topup" && row.paymentChannel ? formatBusinessStatus(row.paymentChannel) : "—"}</td>
                  <td>{row.reason ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {result !== null && (
        <div className="admin-form-row">
          <span>第 {page} 页</span>
          <button type="button" data-testid="stored-value-ledger-prev" disabled={loading || page <= 1} onClick={() => void search(page - 1)}>上一页</button>
          <button type="button" data-testid="stored-value-ledger-next" disabled={loading || rows.length < SIZE} onClick={() => void search(page + 1)}>下一页</button>
        </div>
      )}
    </section>
  );
}
