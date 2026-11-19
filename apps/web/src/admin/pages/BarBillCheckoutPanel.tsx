// 文件版本：V0.02
import React from "react";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BarBillRow, BarOrderRow } from "../types";

// 第十八批 AU 段第 282 条 282-R2（BSM:1077「小费是客人在服务费之外额外给的钱，结账可录（按额或按比例），计入账单并归属该桌台的主责服务提供人」；
// BSM:1079「未支付、未结算账单可按金额拆出子账单供客人分别付款，也可将同一酒吧内另一张未支付、未结算账单合入主账单……合单时，销售提成归订台人员、
// 小费归服务人员，依据各付款客人各自所属的台位分别确定」）与第 283 条 283-R2（BSM:1064「不足最低消费的要补足」；BSM:1079「各结果台显示差额……结账补足」）：
// 后台订单页对所选账单办理小费、拆单、合单与补足低消差额。每个动作一个请求编号：同内容重提沿用、成功即清。

type Operation = { operationType?: string; primaryBillId?: string; secondaryBillId?: string };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & {
  order?: BarOrderRow;
  bill?: BarBillRow;
  orders: BarOrderRow[];
  onChanged: () => Promise<unknown>;
};

const yuan = (cent?: number | null) => `¥${((cent ?? 0) / 100).toFixed(2)}`;
function cents(raw: string, label: string) {
  if (!/^\d+(\.\d{1,2})?$/.test(raw.trim()) || Number(raw) <= 0) throw new Error(`${label}须为正数，最多两位小数`);
  const value = Math.round(Number(raw) * 100);
  if (!Number.isSafeInteger(value)) throw new Error(`${label}超出可办理范围`);
  return value;
}

export function BarBillCheckoutPanel({ api, effectivePermissions, setActionResult, setGlobalError, order, bill, orders, onChanged }: Props) {
  const [tipAmount, setTipAmount] = React.useState("");
  const [tipRatio, setTipRatio] = React.useState("");
  const [tipSource, setTipSource] = React.useState("");
  const [mergedSources, setMergedSources] = React.useState<string[]>([]);
  const [splitAmount, setSplitAmount] = React.useState("");
  const [splitReason, setSplitReason] = React.useState("");
  const [mergeBillId, setMergeBillId] = React.useState("");
  const [mergeReason, setMergeReason] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [note, setNote] = React.useState("");
  const keys = React.useRef<Record<string, { body: string; key: string }>>({});
  const billId = bill?.id ?? "";

  React.useEffect(() => {
    setTipAmount(""); setTipRatio(""); setTipSource(""); setMergedSources([]); setSplitAmount(""); setSplitReason("");
    setMergeBillId(""); setMergeReason(""); setError(""); setNote(""); keys.current = {};
    if (!billId) return;
    let alive = true;
    // 合单后的收款账单：小费须选客人原账单（后端 BAR_TIP_SOURCE_REQUIRED）。读不到（无拆单合单权限）不显示，由后端中文提示兜住。
    api.get<Operation[]>(`/api/v1/bar/bills/${encodeURIComponent(billId)}/operations`)
      .then((rows) => {
        if (alive) setMergedSources(rows.filter((row) => row.operationType === "merge" && row.primaryBillId === billId && row.secondaryBillId)
          .map((row) => row.secondaryBillId!));
      })
      .catch(() => undefined);
    return () => { alive = false; };
  }, [api, billId]);

  const billLabel = (id: string) => orders.flatMap((row) => row.bills ?? []).find((row) => row.id === id)?.billNo ?? id;
  const mergeCandidates = orders.filter((row) => order?.barId && row.barId === order.barId)
    .flatMap((row) => (row.bills ?? []).map((candidate) => ({ row, candidate })))
    .filter(({ candidate }) => candidate.id && candidate.id !== billId && candidate.status === "unpaid");
  const tippable = bill?.status === "unpaid" || bill?.status === "partial_paid";
  const movable = bill?.status === "unpaid";
  const gap = bill?.minSpendGapCent ?? 0;

  function fail(message: string) {
    setError(message);
    return false;
  }

  async function submit(action: string, path: string, body: Record<string, unknown>, done: () => void) {
    const serialized = JSON.stringify({ path, ...body });
    if (keys.current[action]?.body !== serialized) keys.current[action] = { body: serialized, key: crypto.randomUUID() };
    setBusy(true); setError(""); setNote(""); setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/bar/bills/${encodeURIComponent(billId)}/${path}`, { ...body, idempotencyKey: keys.current[action]!.key });
      delete keys.current[action];
      done();
      // 第十八批 AY 段第 347 条 347-R1：合单、拆单、补足低消返回的服务端快照 snapshot_json 由结果区组件统一不展示。
      setActionResult(result);
      // 第十八批 AY 段第 342 条 342-R2：账单已结清的，补足由后端另开一张补足账单（返回的 billId 不是所选账单），提示到「办理账单」里选中结账。
      if (action === "topUp" && (result as { billId?: unknown }).billId !== billId) setNote("已另开补足低消账单，请在「办理账单」下拉中选中后结账");
      await onChanged();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "账单操作失败";
      setError(message);
      setGlobalError(message);
    } finally {
      setBusy(false);
    }
  }

  async function addTip() {
    setError("");
    if (!billId) return void fail("请先选择已生成账单的订单。");
    if (Boolean(tipAmount.trim()) === Boolean(tipRatio.trim())) return void fail("小费按金额或按比例二选一填写。");
    let tipBody: Record<string, unknown>;
    try {
      tipBody = tipAmount.trim() ? { tipAmountCent: cents(tipAmount, "小费金额") } : { tipRatioBps: cents(tipRatio, "小费比例") };
    } catch (invalid) {
      return void fail((invalid as Error).message);
    }
    if (mergedSources.length > 0 && !tipSource) return void fail("合单后的账单加小费须选择客人原账单。");
    await submit("tip", "tip", { ...tipBody, ...(tipSource ? { sourceBillId: tipSource } : {}) }, () => { setTipAmount(""); setTipRatio(""); setTipSource(""); });
  }

  function validateSplit() {
    setError("");
    if (!billId) return fail("请先选择已生成账单的订单。");
    let amount: number;
    try {
      amount = cents(splitAmount, "拆单金额");
    } catch (invalid) {
      return fail((invalid as Error).message);
    }
    if (amount >= (bill?.payableAmountCent ?? 0)) return fail("拆单金额必须小于本账单应付金额。");
    if (!splitReason.trim()) return fail("拆单须填写原因。");
    return true;
  }

  function validateMerge() {
    setError("");
    if (!billId) return fail("请先选择已生成账单的订单。");
    if (!mergeBillId) return fail("请选择要合入的账单。");
    if (!mergeReason.trim()) return fail("合单须填写原因。");
    return true;
  }

  return (
    <section data-testid="bar-bill-checkout-panel">
      <div className="admin-section-title">账单操作</div>
      <p className="admin-muted-action">所选账单：{bill?.id ? `${bill.billNo ?? bill.id} · ${formatBusinessStatus(bill.status)} · 应付 ${yuan(bill.payableAmountCent)} · 已付 ${yuan(bill.paidAmountCent)} · 小费 ${yuan(bill.tipAmountCent)}` : "未生成"}</p>
      {gap > 0 ? (
        <div className="admin-form-row" data-testid="bar-bill-min-spend-form">
          <span data-testid="bar-bill-min-spend-gap">本台低消差额 {yuan(gap)}，结账或关台前须补足</span>
          <HighRiskConfirm
            label="补足低消差额"
            impact="按普通消费计入本台消费：未结账单直接加到应付；账单已结清的另开一张补足账单，结清后方可关台。"
            disabled={busy}
            onConfirm={() => void submit("topUp", "adjustments", { adjustmentType: "min_spend_top_up", amountCent: gap, reason: "结账补足低消差额" }, () => undefined)}
            permissionsState={effectivePermissions}
            permissions={["bar.bill.adjust"]}
            data-testid="bar-bill-min-spend-top-up"
          />
        </div>
      ) : null}
      <div className="admin-form-row" data-testid="bar-bill-tip-form">
        <label>小费金额（元）<input aria-label="小费金额" inputMode="decimal" value={tipAmount} onChange={(e) => setTipAmount(e.target.value)} /></label>
        <label>小费比例（%）<input aria-label="小费比例" inputMode="decimal" value={tipRatio} onChange={(e) => setTipRatio(e.target.value)} /></label>
        {mergedSources.length > 0 ? (
          <label>客人原账单<select data-testid="bar-bill-tip-source" value={tipSource} onChange={(e) => setTipSource(e.target.value)}>
            <option value="">请选择</option>
            {[billId, ...mergedSources].map((id) => <option key={id} value={id}>{billLabel(id)}</option>)}
          </select></label>
        ) : null}
        <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.tip"]} data-testid="bar-bill-tip-submit" disabled={busy || !tippable} onClick={() => void addTip()}>录入小费</PermissionButton>
      </div>
      <div className="admin-form-row" data-testid="bar-bill-split-form">
        <label>拆出金额（元）<input aria-label="拆出金额" inputMode="decimal" value={splitAmount} onChange={(e) => setSplitAmount(e.target.value)} /></label>
        <label>拆单原因<input aria-label="拆单原因" value={splitReason} onChange={(e) => setSplitReason(e.target.value)} placeholder="例如：客人分开付" /></label>
        <HighRiskConfirm
          label="拆单"
          impact="按金额从所选账单拆出一张子账单供客人分别付款，只拆商品消费，服务费及小费留在原账单；销售提成与小费仍归原账单责任人。"
          disabled={busy || !movable}
          precheck={validateSplit}
          onConfirm={() => void submit("split", "split", { splitAmountCent: Math.round(Number(splitAmount) * 100), reason: splitReason.trim() },
            () => { setSplitAmount(""); setSplitReason(""); })}
          permissionsState={effectivePermissions}
          permissions={["bar.bill.split_merge"]}
          data-testid="bar-bill-split"
        />
      </div>
      <div className="admin-form-row" data-testid="bar-bill-merge-form">
        <label>合入账单<select data-testid="bar-bill-merge-target" value={mergeBillId} onChange={(e) => setMergeBillId(e.target.value)}>
          <option value="">请选择同一酒吧未付款的账单</option>
          {mergeCandidates.map(({ row, candidate }) => (
            <option key={candidate.id} value={candidate.id}>{`${candidate.billNo ?? candidate.id} · ${row.id} · ${row.tableId ?? "—"} · 应付 ${yuan(candidate.payableAmountCent)}`}</option>
          ))}
        </select></label>
        <label>合单原因<input aria-label="合单原因" value={mergeReason} onChange={(e) => setMergeReason(e.target.value)} placeholder="例如：同行客人一起付" /></label>
        <HighRiskConfirm
          label="合单"
          impact="合入后副单变为已合并，提成按订台人员、小费按各自台位服务人员分别记。"
          disabled={busy || !movable}
          precheck={validateMerge}
          onConfirm={() => void submit("merge", "merge", { mergedBillId: mergeBillId, reason: mergeReason.trim() }, () => { setMergeBillId(""); setMergeReason(""); })}
          permissionsState={effectivePermissions}
          permissions={["bar.bill.split_merge"]}
          data-testid="bar-bill-merge"
        />
      </div>
      {note ? <p data-testid="bar-bill-checkout-note">{note}</p> : null}
      {error ? <p role="alert" data-testid="bar-bill-checkout-error">{error}</p> : null}
    </section>
  );
}
