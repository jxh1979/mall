// 文件版本：V0.02
import React from "react";
import { withBase } from "../../basePath";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";

// 第十八批 BF 段第 351 条 351-R1（BSM:1164「记录入库、出库、调拨、盘点、报损、报溢和库存调整。」；ENUM:164「报损/盘点差异/存酒作废须照片+视频
// 双证据+主管复核」；BSM:1687「核对采购入库、库存消耗、报损、盘点差异和高价值酒水异常。」）：后台「库存盘点」页（ADM-INV-006）。
// 建盘点单、登记差异、差异复核、差异过账都走已有接口，范围、高价值证据、录入人不得复核、过账方向等规则由接口照原样把关：
//   GET/POST /api/v1/inventory/stocktakes、GET /api/v1/inventory/stocktakes/{id}、
//   POST /api/v1/inventory/stocktakes/{id}/variances、POST /api/v1/inventory/stocktakes/{id}/variances/{vid}/review、
//   POST /api/v1/finance/inventory-variances/post。
// 第十八批 BK 段修复项 F01：建盘点单后该库房的入库等库存变动被冻结（接口报「仓库正在盘点中，盘点结束前库存变动已冻结」），
// 盘点中的单加「结束盘点」（POST /api/v1/inventory/stocktakes/{id}/close，建单权限，高危确认）；还有待复核的差异时接口不许结束。
// 第十八批 BK 段修复项 F02（BSM:1165「商品成本按「当前平均进货价」算」）：单位成本可不填，接口按该物料当前平均进货价估算差异金额；
// F03：复核、过账请求只带接口会读的字段（复核结论；差异编号与往来单位），不设「复核意见」「过账说明」输入框；
// F04：库存按整瓶记、复核纠账按整数，实盘数量只收不小于 0 的整数。
// 复核修复（PGM:660「系统自动使用某个价格完成计算，不代表当前操作人取得查看或修改权限。」）：不持 inventory.cost.view 的读者接口隐去差异金额
// （显示「—」），「过账」按接口给的 has_variance_amount（差异金额不为 0，不含价格）出现。
// 账面数量由系统按该库房现有库存取，不由页面填。复核与过账首次提交可能被准入闸拦下送主管复核（ADMISSION_BLOCKED），
// 主管批准后按原内容再提交才生效，所以被拦下时不清空已填内容。
const CREATE = "inventory.stocktake.create";
const RECORD = "inventory.stocktake.variance_record";
const REVIEW = "inventory.stocktake.variance_review";
const POST = "finance.inventory_variance.post";
const BASE = "/api/v1/inventory/stocktakes";

type Stocktake = { id: string; stocktakeNo: string; warehouseId: string; businessDate?: string | null; status: string; remark?: string | null; createdAt?: string | null };
type Variance = {
  id: string; itemId: string; batchNo?: string | null; bookQty?: number | null; countedQty?: number | null; varianceQty?: number | null;
  varianceAmountCent?: number | null; hasVarianceAmount?: boolean | null; reason: string; supplierId?: string | null; reviewStatus: string; reviewedBy?: string | null;
  reviewedAt?: string | null; postedArApItemId?: string | null; recordedBy?: string | null;
  photoAttachmentIds?: string[] | null; videoAttachmentIds?: string[] | null;
};
type Warehouse = { id: string; name?: string | null };
type Item = { id: string; itemName?: string | null; unit?: string | null };
type EvidenceFile = { id: string; fileName: string; video: boolean };

const STATUS_LABELS: Record<string, string> = { counting: "盘点中", reviewing: "复核中", closed: "已关闭" };
const REVIEW_LABELS: Record<string, string> = { pending: "待复核", approved: "已通过", rejected: "已驳回" };
export const VARIANCE_REASON_LABELS: Record<string, string> = { damage: "报损", shortage: "盘亏", overage: "盘盈", supplier_fault: "供应商责任" };
const BLOCKED_NOTE = "已提交主管审批：主管在「主管复核」页批准后，请保持所填内容不变，原样再点一次。";
const CLOSE_IMPACT = "结束后该库房恢复入库、出库、调拨等库存变动；已关闭的盘点单不能再登记差异。";
const isOpen = (status: string) => status === "counting" || status === "reviewing";

const yuan = (cent: number | null | undefined) => (cent == null ? "—" : (cent / 100).toFixed(2));
const qty = (value: number | null | undefined) => (value == null ? "—" : String(value));

function evidenceLinks(ids: string[] | null | undefined, label: string) {
  return (ids ?? []).map((id, index) => (
    <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">
      {label}{index + 1}{" "}
    </a>
  ));
}

export function InventoryStocktakePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [stocktakes, setStocktakes] = React.useState<Stocktake[]>([]);
  const [warehouses, setWarehouses] = React.useState<Warehouse[]>([]);
  const [items, setItems] = React.useState<Item[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  const [note, setNote] = React.useState("");
  const [createForm, setCreateForm] = React.useState({ warehouseId: "", remark: "" });
  const [selectedId, setSelectedId] = React.useState("");
  const [variances, setVariances] = React.useState<Variance[]>([]);
  const [varianceForm, setVarianceForm] = React.useState({ itemId: "", batchNo: "", countedQty: "", reason: "", supplierId: "", unitCostYuan: "" });
  const [evidence, setEvidence] = React.useState<EvidenceFile[]>([]);
  const [uploading, setUploading] = React.useState(false);
  const [postCounterpartyId, setPostCounterpartyId] = React.useState("");

  const selected = stocktakes.find((row) => row.id === selectedId) ?? null;
  const warehouseName = (id: string) => warehouses.find((row) => row.id === id)?.name ?? id;
  const itemName = (id: string) => items.find((row) => row.id === id)?.itemName ?? id;

  const loadList = React.useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setStocktakes(await api.get<Stocktake[]>(BASE));
    } catch (cause) {
      setStocktakes([]);
      setError(cause instanceof Error ? cause.message : "盘点单读取失败");
    } finally {
      setLoading(false);
    }
  }, [api]);

  const loadDetail = React.useCallback(async (id: string) => {
    if (!id) { setVariances([]); return; }
    try {
      const detail = await api.get<{ variances?: Variance[] }>(`${BASE}/${encodeURIComponent(id)}`);
      setVariances(detail.variances ?? []);
    } catch (cause) {
      setVariances([]);
      setFormError(cause instanceof Error ? cause.message : "盘点差异读取失败");
    }
  }, [api]);

  React.useEffect(() => {
    void loadList();
    // 库房与物料只用来给下拉显示名称；读不到（没有对应查看权限）时改为手填编号。
    void api.get<Warehouse[]>("/api/v1/inventory/warehouses").then(setWarehouses).catch(() => setWarehouses([]));
    void api.get<Item[]>("/api/v1/inventory/items").then(setItems).catch(() => setItems([]));
  }, [api, loadList]);

  function selectStocktake(id: string) {
    setSelectedId(id);
    setFormError("");
    setNote("");
    void loadDetail(id);
  }

  /** 统一提交：成功后写结果区并刷新；被准入闸拦下时给出提示、保留所填内容。返回是否办成。 */
  async function submit(action: string, path: string, body: Record<string, unknown>, result: Record<string, string>): Promise<boolean> {
    setBusy(true);
    setFormError("");
    setNote("");
    try {
      await api.post(path, body);
      setActionResult({ 结果: action, ...result });
      return true;
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : `${action}失败`;
      if (cause instanceof ApiError && cause.detailCode === "ADMISSION_BLOCKED") {
        setNote(BLOCKED_NOTE);
        setGlobalError(message);
      } else {
        setFormError(message);
      }
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function createStocktake() {
    const warehouseId = createForm.warehouseId.trim();
    if (!warehouseId) { setFormError("请先选择要盘点的库房。"); return; }
    const ok = await submit("盘点单已建立", BASE, { warehouseId, remark: createForm.remark.trim() || undefined }, { 库房: warehouseName(warehouseId) });
    if (ok) {
      setCreateForm({ warehouseId: "", remark: "" });
      await loadList();
    }
  }

  async function closeStocktake(row: Stocktake) {
    const ok = await submit("盘点已结束", `${BASE}/${encodeURIComponent(row.id)}/close`, {}, { 盘点单号: row.stocktakeNo, 库房: warehouseName(row.warehouseId) });
    if (ok) await loadList();
  }

  /** 现场拍的照片、视频直接上传，取回真实附件编号。 */
  async function uploadEvidence(files: FileList | null) {
    if (!files || files.length === 0) return;
    setUploading(true);
    setFormError("");
    try {
      const uploaded: EvidenceFile[] = [];
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", "inventory_variance");
        form.append("business_id", selectedId); // 第 384 条 384-R3：带所选盘点单编号，后端按盘点单范围判
        form.append("attachment_role", file.type.startsWith("video") ? "video" : "photo");
        if (!api.postForm) throw new Error("当前客户端不支持文件上传。");
        const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
        uploaded.push({ id: saved.id, fileName: saved.fileName ?? file.name, video: file.type.startsWith("video") });
      }
      setEvidence((prev) => [...prev, ...uploaded]);
    } catch (cause) {
      setFormError(cause instanceof Error ? cause.message : "证据上传失败。");
    } finally {
      setUploading(false);
    }
  }

  async function recordVariance() {
    if (!selected) return;
    const itemId = varianceForm.itemId.trim();
    const counted = Number(varianceForm.countedQty);
    if (!itemId) { setFormError("请选择盘点的物料。"); return; }
    if (varianceForm.countedQty.trim() === "" || !Number.isFinite(counted) || counted < 0) { setFormError("请填写实盘数量（不小于 0）。"); return; }
    if (!Number.isInteger(counted)) { setFormError("实盘数量须为整数。"); return; }
    if (!varianceForm.reason) { setFormError("请选择差异原因。"); return; }
    const unitCost = varianceForm.unitCostYuan.trim() === "" ? undefined : Math.round(Number(varianceForm.unitCostYuan) * 100);
    if (unitCost !== undefined && (!Number.isFinite(unitCost) || unitCost < 0)) { setFormError("单位成本填写不正确。"); return; }
    const photos = evidence.filter((file) => !file.video).map((file) => file.id);
    const videos = evidence.filter((file) => file.video).map((file) => file.id);
    const ok = await submit("盘点差异已登记", `${BASE}/${encodeURIComponent(selected.id)}/variances`, {
      itemId,
      batchNo: varianceForm.batchNo.trim() || undefined,
      countedQty: counted,
      reason: varianceForm.reason,
      supplierId: varianceForm.reason === "supplier_fault" ? varianceForm.supplierId.trim() || undefined : undefined,
      unitCostCents: unitCost,
      photoAttachmentIds: photos.length ? photos : undefined,
      videoAttachmentIds: videos.length ? videos : undefined,
    }, { 盘点单号: selected.stocktakeNo, 物料: itemName(itemId) });
    if (ok) {
      setVarianceForm({ itemId: "", batchNo: "", countedQty: "", reason: "", supplierId: "", unitCostYuan: "" });
      setEvidence([]);
      await loadDetail(selected.id);
    }
  }

  async function reviewVariance(row: Variance, decision: "approved" | "rejected") {
    if (!selected) return;
    const ok = await submit(decision === "approved" ? "盘点差异已复核通过" : "盘点差异已驳回",
      `${BASE}/${encodeURIComponent(selected.id)}/variances/${encodeURIComponent(row.id)}/review`,
      { decision }, { 盘点单号: selected.stocktakeNo, 物料: itemName(row.itemId) });
    if (ok) await loadDetail(selected.id);
  }

  async function postVariance(row: Variance) {
    if (!selected) return;
    const counterpartyId = postCounterpartyId.trim() || (row.reason === "supplier_fault" ? row.supplierId ?? "" : "");
    if (!counterpartyId) { setFormError("过账须填写往来单位编号（供应商责任的差异不填时取登记的责任供应商）。"); return; }
    const ok = await submit("盘点差异已过账", "/api/v1/finance/inventory-variances/post",
      { inventoryVarianceId: row.id, counterpartyId },
      { 盘点单号: selected.stocktakeNo, 物料: itemName(row.itemId), 往来单位: counterpartyId });
    if (ok) {
      setPostCounterpartyId("");
      await loadDetail(selected.id);
    }
  }

  function varianceActions(row: Variance) {
    if (row.reviewStatus === "pending") {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={[REVIEW]} disabled={busy}
            data-testid={`stocktake-variance-approve-${row.id}`} onClick={() => void reviewVariance(row, "approved")}>通过</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={[REVIEW]} disabled={busy}
            data-testid={`stocktake-variance-reject-${row.id}`} onClick={() => void reviewVariance(row, "rejected")}>驳回</PermissionButton>
        </>
      );
    }
    if (row.reviewStatus === "approved" && !row.postedArApItemId && row.hasVarianceAmount) {
      return (
        <PermissionButton permissionsState={effectivePermissions} permissions={[POST]} disabled={busy}
          data-testid={`stocktake-variance-post-${row.id}`} onClick={() => void postVariance(row)}>过账</PermissionButton>
      );
    }
    return row.postedArApItemId ? "已过账" : "—";
  }

  const open = selected != null && isOpen(selected.status);

  return (
    <section data-testid="inventory-stocktake-page">
      <AdminPageHeader
        title="库存盘点"
        description="建盘点单、登记盘点差异、差异复核与过账。账面数量由系统按该库房现有库存取；高价值物品的差异须上传照片和视频；登记差异的人不能复核自己登记的差异；复核通过后库存按实盘数量纠正。"
      />
      {formError ? <div className="admin-inline-error" role="alert" data-testid="stocktake-error">{formError}</div> : null}
      {note ? <p data-testid="stocktake-note">{note}</p> : null}

      <h3>新建盘点单</h3>
      <div className="admin-form-grid">
        <label>库房
          {warehouses.length > 0 ? (
            <select data-testid="stocktake-create-warehouse" value={createForm.warehouseId} onChange={(event) => setCreateForm({ ...createForm, warehouseId: event.target.value })}>
              <option value="">请选择</option>
              {warehouses.map((row) => <option key={row.id} value={row.id}>{row.name ?? row.id}</option>)}
            </select>
          ) : (
            <input data-testid="stocktake-create-warehouse" value={createForm.warehouseId} placeholder="库房编号" onChange={(event) => setCreateForm({ ...createForm, warehouseId: event.target.value })} />
          )}
        </label>
        <label>备注（可选）
          <input data-testid="stocktake-create-remark" value={createForm.remark} onChange={(event) => setCreateForm({ ...createForm, remark: event.target.value })} />
        </label>
        <PermissionButton permissionsState={effectivePermissions} permissions={[CREATE]} disabled={busy} onClick={() => void createStocktake()} data-testid="stocktake-create-submit">新建盘点单</PermissionButton>
      </div>

      <h3>盘点单</h3>
      {error ? <p role="alert" data-testid="stocktake-list-error">{error}</p> : null}
      {loading ? <p data-testid="stocktake-loading">正在读取…</p> : stocktakes.length === 0 && !error ? <p data-testid="stocktake-empty">还没有盘点单。</p> : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="stocktake-table">
            <thead><tr><th>盘点单号</th><th>库房</th><th>盘点营业日</th><th>状态</th><th>备注</th><th>建单时间</th><th>操作</th></tr></thead>
            <tbody>
              {stocktakes.map((row) => (
                <tr key={row.id} data-testid={`stocktake-row-${row.id}`} aria-selected={row.id === selectedId}>
                  <td>{row.stocktakeNo}</td>
                  <td>{warehouseName(row.warehouseId)}</td>
                  <td>{cnDate(row.businessDate ?? null)}</td>
                  <td>{STATUS_LABELS[row.status] ?? row.status}</td>
                  <td>{row.remark ?? "—"}</td>
                  <td>{cnDate(row.createdAt ?? null)}</td>
                  <td style={{ display: "flex", gap: 8 }}>
                    <button type="button" data-testid={`stocktake-select-${row.id}`} onClick={() => selectStocktake(row.id)}>查看差异</button>
                    {isOpen(row.status) ? (
                      <HighRiskConfirm label="结束盘点" impact={CLOSE_IMPACT} irreversible={false} disabled={busy}
                        permissionsState={effectivePermissions} permissions={[CREATE]}
                        data-testid={`stocktake-close-${row.id}`} onConfirm={() => void closeStocktake(row)} />
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected ? (
        <div data-testid="stocktake-detail">
          <h3>盘点单 {selected.stocktakeNo} 的差异</h3>
          {open ? (
            <div className="admin-form-grid">
              <label>物料
                {items.length > 0 ? (
                  <select data-testid="stocktake-variance-item" value={varianceForm.itemId} onChange={(event) => setVarianceForm({ ...varianceForm, itemId: event.target.value })}>
                    <option value="">请选择</option>
                    {items.map((row) => <option key={row.id} value={row.id}>{row.itemName ?? row.id}{row.unit ? `（${row.unit}）` : ""}</option>)}
                  </select>
                ) : (
                  <input data-testid="stocktake-variance-item" value={varianceForm.itemId} placeholder="物料编号" onChange={(event) => setVarianceForm({ ...varianceForm, itemId: event.target.value })} />
                )}
              </label>
              <label>批次号（可选）
                <input data-testid="stocktake-variance-batch" value={varianceForm.batchNo} onChange={(event) => setVarianceForm({ ...varianceForm, batchNo: event.target.value })} />
              </label>
              <label>实盘数量
                <input data-testid="stocktake-variance-counted" inputMode="numeric" value={varianceForm.countedQty} onChange={(event) => setVarianceForm({ ...varianceForm, countedQty: event.target.value })} />
              </label>
              <label>差异原因
                <select data-testid="stocktake-variance-reason" value={varianceForm.reason} onChange={(event) => setVarianceForm({ ...varianceForm, reason: event.target.value })}>
                  <option value="">请选择</option>
                  {Object.entries(VARIANCE_REASON_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </label>
              {varianceForm.reason === "supplier_fault" ? (
                <label>责任供应商编号
                  <input data-testid="stocktake-variance-supplier" value={varianceForm.supplierId} onChange={(event) => setVarianceForm({ ...varianceForm, supplierId: event.target.value })} />
                </label>
              ) : null}
              <label>单位成本（元，不填按当前平均进货价估算）
                <input data-testid="stocktake-variance-unit-cost" inputMode="decimal" value={varianceForm.unitCostYuan} onChange={(event) => setVarianceForm({ ...varianceForm, unitCostYuan: event.target.value })} />
              </label>
              <label>照片、视频（高价值物品必传）
                <input type="file" multiple accept="image/*,video/*" data-testid="stocktake-variance-evidence" disabled={uploading} onChange={(event) => void uploadEvidence(event.target.files)} />
              </label>
              {evidence.length > 0 ? <p data-testid="stocktake-variance-evidence-list">已上传：{evidence.map((file) => `${file.video ? "视频" : "照片"} ${file.fileName}`).join("、")}</p> : null}
              <PermissionButton permissionsState={effectivePermissions} permissions={[RECORD]} disabled={busy || uploading} onClick={() => void recordVariance()} data-testid="stocktake-variance-submit">登记差异</PermissionButton>
            </div>
          ) : <p data-testid="stocktake-closed-hint">该盘点单已关闭，不能再登记差异。</p>}

          <div className="admin-form-grid">
            <label>过账往来单位编号
              <input data-testid="stocktake-post-counterparty" value={postCounterpartyId} onChange={(event) => setPostCounterpartyId(event.target.value)} />
            </label>
          </div>

          {variances.length === 0 ? <p data-testid="stocktake-variance-empty">该盘点单还没有登记差异。</p> : (
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="stocktake-variance-table">
                <thead><tr><th>物料</th><th>批次号</th><th>账面数量</th><th>实盘数量</th><th>差异数量</th><th>差异金额（元）</th><th>原因</th><th>责任供应商</th><th>照片／视频</th><th>登记人</th><th>复核状态</th><th>复核人与时间</th><th>操作</th></tr></thead>
                <tbody>
                  {variances.map((row) => (
                    <tr key={row.id} data-testid={`stocktake-variance-row-${row.id}`}>
                      <td>{itemName(row.itemId)}</td>
                      <td>{row.batchNo ?? "—"}</td>
                      <td>{qty(row.bookQty)}</td>
                      <td>{qty(row.countedQty)}</td>
                      <td>{qty(row.varianceQty)}</td>
                      <td>{yuan(row.varianceAmountCent)}</td>
                      <td>{VARIANCE_REASON_LABELS[row.reason] ?? row.reason}</td>
                      <td>{row.supplierId ?? "—"}</td>
                      <td>{evidenceLinks(row.photoAttachmentIds, "照片")}{evidenceLinks(row.videoAttachmentIds, "视频")}</td>
                      <td>{row.recordedBy ?? "—"}</td>
                      <td>{REVIEW_LABELS[row.reviewStatus] ?? row.reviewStatus}</td>
                      <td>{row.reviewedBy ? `${row.reviewedBy} · ${cnDate(row.reviewedAt ?? null)}` : "—"}</td>
                      <td style={{ display: "flex", gap: 8 }}>{varianceActions(row)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
}
