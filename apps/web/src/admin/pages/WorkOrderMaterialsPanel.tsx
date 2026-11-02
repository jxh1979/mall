// 文件版本：V0.01
// 第十八批 M 段第 117 条（BSM:1380「派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上
// 标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。
// 承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。」）：工单「用料」面板——用料明细与合计、领用出库、标明费用承担方、承担方争议
// 与争议记录。楼宇运营工作台的设施工单（117-R3）与商户服务工单页（117-R5；争议按第十八批裁决 29 停在原状态转上级判定）共用；打开才请求。
import React from "react";
import { Button, Modal } from "antd";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminApiClient } from "../types";

type Line = {
  movementId: string; itemName?: string | null; warehouseName?: string | null; locationId?: string | null; quantity: number;
  unitCostCent?: number | null; totalCostCent?: number | null;
};
type Bearer = { costBearer: string; merchantId?: string | null; merchantName?: string | null };
type Dispute = {
  id: string; fromCostBearer: string; fromMerchantId?: string | null; claimedCostBearer: string; claimedMerchantId?: string | null;
  reason: string; status: string; raisedAt?: string | null; ruledBy?: string | null; ruledAt?: string | null; rulingBasis?: string | null;
};
type StockOption = { warehouseId: string; warehouseName: string; locationId: string; itemId: string; itemName: string; availableQuantity: number };
type MerchantOption = { merchantId: string; merchantName: string };
type Materials = { lines: Line[]; totalCostCent: number; bearer?: Bearer | null; disputes: Dispute[]; options: { stock: StockOption[]; merchants: MerchantOption[] } };
type MaterialAction = "work_order_material_issued" | "work_order_cost_bearer_marked" | "work_order_bearer_disputed";

const BEARER_LABELS: Record<string, string> = { property: "物业承担", merchant: "商户承担" };
// 争议状态中文只在本面板用：不放进全局 status 键，免得改到所有结果对象的状态显示。
const DISPUTE_STATUS_LABELS: Record<string, string> = { pending: "待上级判定", approved: "按主张改定", rejected: "维持原承担方" };
const yuan = (cent?: number | null) => (cent == null ? "-" : (cent / 100).toFixed(2));
const stockKey = (row: StockOption) => `${row.itemId}::${row.locationId}`;

export function WorkOrderMaterialsPanel({ api, basePath, title, canIssue, canMarkBearer, canDispute, allowMerchantPick, defaultMerchantId,
  disputeHint, setActionResult, onChanged, onClose }: {
  api: AdminApiClient;
  /** 工单接口前缀，如 /api/v1/facility/work-orders/{id}。 */
  basePath: string;
  title: string;
  /** 可领用出库（设施工单处理中；商户服务工单已派单、处理中、待商户确认）。 */
  canIssue: boolean;
  /** 可标明承担方（工单未终结）。 */
  canMarkBearer: boolean;
  /** 可提承担方争议（进行中的工单）。 */
  canDispute: boolean;
  /** 商户承担可选商户（设施工单）；商户服务工单的承担商户就是提单商户，不另选。 */
  allowMerchantPick: boolean;
  defaultMerchantId?: string | null;
  /** 提出争议成功后的提示。 */
  disputeHint: string;
  setActionResult: (result: unknown) => void;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [data, setData] = React.useState<Materials | null>(null);
  const [warehouseId, setWarehouseId] = React.useState("");
  const [stock, setStock] = React.useState("");
  const [quantity, setQuantity] = React.useState("1");
  const [costBearer, setCostBearer] = React.useState("property");
  const [bearerMerchantId, setBearerMerchantId] = React.useState("");
  const [claimed, setClaimed] = React.useState("merchant");
  const [claimedMerchantId, setClaimedMerchantId] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [finalApproverId, setFinalApproverId] = React.useState("");
  const [error, setError] = React.useState("");
  const [notice, setNotice] = React.useState("");
  const [busy, setBusy] = React.useState("");

  async function load() {
    try {
      const next = await api.get<Materials>(`${basePath}/materials`);
      setData(next);
      setCostBearer(next.bearer?.costBearer ?? "property");
      setBearerMerchantId(next.bearer?.merchantId ?? defaultMerchantId ?? "");
      setClaimed(next.bearer?.costBearer === "merchant" ? "property" : "merchant");
      setClaimedMerchantId(defaultMerchantId ?? "");
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "用料读取失败");
    }
  }

  React.useEffect(() => {
    void load();
    // 只在打开（换工单）时读一次。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [basePath]);

  async function submit(action: MaterialAction, path: string, requestBody: Record<string, unknown>) {
    setBusy(action);
    setError("");
    setNotice("");
    try {
      const result = await api.post(`${basePath}/${path}`, requestBody);
      setActionResult({ action, requestBody, result });
      await load();
      onChanged();
      return true;
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "用料操作失败");
      return false;
    } finally {
      setBusy("");
    }
  }

  function issue() {
    const picked = data?.options.stock.find((row) => stockKey(row) === stock);
    const count = Number(quantity);
    if (!picked) return setError("请选择物料与库位");
    if (!Number.isInteger(count) || count <= 0) return setError("数量须为正整数");
    void submit("work_order_material_issued", "materials", { locationId: picked.locationId, itemId: picked.itemId, quantity: count });
  }

  function markBearer() {
    if (allowMerchantPick && costBearer === "merchant" && !bearerMerchantId) return setError("请选择承担的商户");
    void submit("work_order_cost_bearer_marked", "cost-bearer",
      allowMerchantPick && costBearer === "merchant" ? { costBearer, merchantId: bearerMerchantId } : { costBearer });
  }

  async function dispute() {
    if (!reason.trim()) return setError("请填写争议原因");
    if (allowMerchantPick && claimed === "merchant" && !claimedMerchantId) return setError("请选择主张承担的商户");
    const done = await submit("work_order_bearer_disputed", "bearer-dispute", {
      claimedCostBearer: claimed,
      claimedMerchantId: allowMerchantPick && claimed === "merchant" ? claimedMerchantId : undefined,
      reason: reason.trim(),
      finalApproverId: finalApproverId.trim() || undefined,
    });
    if (done) {
      setReason("");
      setNotice(disputeHint);
    }
  }

  const merchantName = (id?: string | null) => data?.options.merchants.find((row) => row.merchantId === id)?.merchantName
    ?? (data?.bearer?.merchantId === id ? data?.bearer?.merchantName : null) ?? id ?? "";
  const bearerText = (bearer?: string | null, merchantId?: string | null) =>
    !bearer ? "未标明" : bearer === "merchant" ? `${BEARER_LABELS.merchant}（${merchantName(merchantId)}）` : BEARER_LABELS[bearer] ?? bearer;
  const warehouses = Array.from(new Map((data?.options.stock ?? []).map((row) => [row.warehouseId, row.warehouseName])).entries());
  const pending = (data?.disputes ?? []).some((row) => row.status === "pending");
  const merchantSelect = (testId: string, value: string, onChange: (value: string) => void) => (
    <label><span>商户</span><select data-testid={testId} value={value} onChange={(event) => onChange(event.target.value)}>
      <option value="">请选择商户</option>
      {(data?.options.merchants ?? []).map((row) => <option key={row.merchantId} value={row.merchantId}>{row.merchantName}</option>)}
    </select></label>
  );
  const bearerRadios = (name: string, value: string, onChange: (value: string) => void) => (
    <span>{Object.entries(BEARER_LABELS).map(([code, label]) => (
      <label key={code}><input type="radio" name={name} data-testid={`${name}-${code}`} checked={value === code} onChange={() => onChange(code)} />{label}</label>
    ))}</span>
  );

  return (
    <Modal open title={title} onCancel={onClose} footer={<Button data-testid="work-order-materials-close" onClick={onClose}>关闭</Button>} width={760}>
      <div data-testid="work-order-materials">
        <p>费用承担方：{bearerText(data?.bearer?.costBearer, data?.bearer?.merchantId)}</p>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="work-order-materials-lines">
            <thead><tr><th>物料</th><th>库房</th><th>库位</th><th>数量</th><th>单价（当前平均进货价，元）</th><th>金额（元）</th></tr></thead>
            <tbody>{(data?.lines ?? []).map((row) => (
              <tr key={row.movementId}><td>{row.itemName ?? "-"}</td><td>{row.warehouseName ?? "-"}</td><td>{row.locationId ?? "-"}</td><td>{row.quantity}</td>
                <td>{yuan(row.unitCostCent)}</td><td>{yuan(row.totalCostCent)}</td></tr>
            ))}</tbody>
            <tfoot><tr><td colSpan={5}>合计</td><td data-testid="work-order-materials-total">{yuan(data?.totalCostCent ?? 0)}</td></tr></tfoot>
          </table>
        </div>
        {data && canIssue ? (
          <div className="admin-form-grid" data-testid="work-order-materials-issue">
            <h4>领用出库</h4>
            <label><span>库房</span><select data-testid="work-order-materials-warehouse" value={warehouseId} onChange={(event) => { setWarehouseId(event.target.value); setStock(""); }}>
              <option value="">请选择库房</option>
              {warehouses.map(([id, name]) => <option key={id} value={id}>{name}</option>)}
            </select></label>
            <label><span>物料</span><select data-testid="work-order-materials-stock" value={stock} onChange={(event) => setStock(event.target.value)}>
              <option value="">请选择物料</option>
              {data.options.stock.filter((row) => row.warehouseId === warehouseId).map((row) => (
                <option key={stockKey(row)} value={stockKey(row)}>{`${row.itemName} · 库位 ${row.locationId} · 可用 ${row.availableQuantity}`}</option>
              ))}
            </select></label>
            <label><span>数量</span><input data-testid="work-order-materials-quantity" type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label>
            <Button data-testid="work-order-materials-issue-submit" loading={busy === "work_order_material_issued"} disabled={busy !== ""} onClick={issue}>领用出库</Button>
          </div>
        ) : null}
        {data && canMarkBearer && !pending ? (
          <div className="admin-form-grid" data-testid="work-order-materials-bearer">
            <h4>费用承担方</h4>
            <p className="admin-muted-action">公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担。</p>
            {bearerRadios("work-order-materials-bearer", costBearer, setCostBearer)}
            {allowMerchantPick && costBearer === "merchant" ? merchantSelect("work-order-materials-bearer-merchant-id", bearerMerchantId, setBearerMerchantId) : null}
            <Button data-testid="work-order-materials-bearer-submit" loading={busy === "work_order_cost_bearer_marked"} disabled={busy !== ""} onClick={markBearer}>标明费用承担方</Button>
          </div>
        ) : null}
        {data?.bearer && canDispute && !pending ? (
          <div className="admin-form-grid" data-testid="work-order-materials-dispute">
            <h4>承担方有争议</h4>
            {bearerRadios("work-order-materials-claimed", claimed, setClaimed)}
            {allowMerchantPick && claimed === "merchant" ? merchantSelect("work-order-materials-claimed-merchant-id", claimedMerchantId, setClaimedMerchantId) : null}
            <label><span>争议原因</span><textarea data-testid="work-order-materials-dispute-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
            <label><span>最终审批人</span><input data-testid="work-order-materials-final-approver" placeholder="不填按直接上级" value={finalApproverId} onChange={(event) => setFinalApproverId(event.target.value)} /></label>
            <Button data-testid="work-order-materials-dispute-submit" danger loading={busy === "work_order_bearer_disputed"} disabled={busy !== ""} onClick={() => void dispute()}>转上级判定</Button>
          </div>
        ) : null}
        {notice ? <div className="admin-state" data-testid="work-order-materials-notice">{notice}</div> : null}
        {error ? <div className="admin-inline-error" data-testid="work-order-materials-error">{error}</div> : null}
        {(data?.disputes ?? []).length ? (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="work-order-materials-disputes">
              <thead><tr><th>提出时间</th><th>原承担方</th><th>主张承担方</th><th>原因</th><th>状态</th><th>判定人</th><th>判定依据</th><th>判定时间</th></tr></thead>
              <tbody>{data!.disputes.map((row) => (
                <tr key={row.id}><td>{cnDate(row.raisedAt)}</td><td>{bearerText(row.fromCostBearer, row.fromMerchantId)}</td>
                  <td>{bearerText(row.claimedCostBearer, row.claimedMerchantId)}</td><td>{row.reason}</td><td>{DISPUTE_STATUS_LABELS[row.status] ?? row.status}</td>
                  <td>{row.ruledBy ?? "-"}</td><td>{row.rulingBasis ?? "-"}</td><td>{cnDate(row.ruledAt)}</td></tr>
              ))}</tbody>
            </table>
          </div>
        ) : null}
      </div>
    </Modal>
  );
}
