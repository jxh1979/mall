import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, BarComboRow, BarProductRow } from "../types";
import { PermissionButton } from "../security/Can";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

function yuan(cent: number | null | undefined): string {
  if (cent === null || cent === undefined) return "—";
  return (cent / 100).toFixed(2);
}
function toCent(input: string): number | null {
  const trimmed = input.trim();
  if (trimmed === "") return null;
  const value = Number(trimmed);
  if (!Number.isFinite(value)) return null;
  return Math.round(value * 100);
}

type ComboItemForm = { productId: string; quantity: string };
type ComboForm = {
  barId: string;
  comboName: string;
  priceYuan: string;
  internalPriceYuan: string;
  items: ComboItemForm[];
};
const EMPTY_COMBO_FORM: ComboForm = { barId: "", comboName: "", priceYuan: "", internalPriceYuan: "", items: [{ productId: "", quantity: "1" }] };

/**
 * 酒吧套餐管理页（ADM-BAR-006，V257 图形化点单 POS 阶段1）：
 *   后台从既有 SKU（bar_products）挑若干件组成套餐，定套餐整体售价与内部结算价。供平板图形化点单直接下套餐。
 * 查看需 bar.combo.view；维护需 bar.combo.manage（含内部结算价，敏感）。
 */
export function BarCombosPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<BarComboRow[]>([]);
  const [products, setProducts] = React.useState<BarProductRow[]>([]);
  const [form, setForm] = React.useState<ComboForm>(EMPTY_COMBO_FORM);
  const [formError, setFormError] = React.useState<string | null>(null);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // V262 存档/删除范式：默认列表不含已存档，勾选后带出。
  const [showArchived, setShowArchived] = React.useState(false);
  // 引导式流程 phase-3：新建套餐收起为主按钮，点击弹出分步引导（GuidedFlowModal）。
  const [createOpen, setCreateOpen] = React.useState(false);

  const productName = React.useCallback(
    (id: string): string => products.find((p) => p.id === id)?.productName ?? id,
    [products],
  );

  const { loading, error, reload } = useAdminResource(
    async () => {
      const combos = await api.get<BarComboRow[]>(`/api/v1/bar/catalog/combos?includeArchived=${showArchived}`);
      setRows(Array.isArray(combos) ? combos : []);
      try {
        const prods = await api.get<BarProductRow[]>("/api/v1/bar/products");
        setProducts(Array.isArray(prods) ? prods : []);
      } catch {
        setProducts([]);
      }
    },
    { onError: setGlobalError, fallbackMessage: "套餐列表读取失败" },
  );


  // V262 存档/删除范式:切换「显示已存档」后重新拉取,首次挂载不触发。
  const firstArchivedToggle = React.useRef(true);
  React.useEffect(() => {
    if (firstArchivedToggle.current) {
      firstArchivedToggle.current = false;
      return;
    }
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showArchived]);

  function archiveLabel(row: { archivedAt?: string | null; deleteStatus?: string }): string {
    if (row.deleteStatus === "pending") return "待审批删除";
    if (row.archivedAt) return "已存档";
    return "生效中";
  }

  async function archiveCombo(id: string, archive: boolean) {
    try {
      await api.post(`/api/v1/bar/catalog/combos/${id}/${archive ? "archive" : "unarchive"}`, {});
      setActionResult({ action: `bar_combo_${archive ? "archived" : "unarchived"}`, id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "存档操作失败");
    }
  }

  async function requestDeleteCombo(id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null) {
      return; // 用户点「取消」= 中止申请。
    }
    try {
      await api.post(`/api/v1/bar/catalog/combos/${id}/delete-request`, { reason });
      setActionResult({ action: "bar_combo_delete_requested", id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除申请失败");
    }
  }

  async function approveDeleteCombo(id: string, decision: "approved" | "rejected") {
    try {
      const decided = await api.post<{ deleted?: boolean }>(`/api/v1/bar/catalog/combos/${id}/delete-approve`, { decision });
      setActionResult({ action: `bar_combo_delete_${decision}`, id, deleted: decided?.deleted ?? false });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除审批失败");
    }
  }

  function comboActions(row: BarComboRow) {
    if (row.deleteStatus === "pending") {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.approve"]} data-testid={`bar-combo-delete-approve-${row.id}`} onClick={() => void approveDeleteCombo(row.id, "approved")}>批准删除</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.approve"]} data-testid={`bar-combo-delete-reject-${row.id}`} onClick={() => void approveDeleteCombo(row.id, "rejected")}>驳回</PermissionButton>
        </>
      );
    }
    if (row.archivedAt) {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.manage"]} data-testid={`bar-combo-unarchive-${row.id}`} onClick={() => void archiveCombo(row.id, false)}>取消存档</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.manage"]} data-testid={`bar-combo-delete-request-${row.id}`} onClick={() => void requestDeleteCombo(row.id)}>申请删除</PermissionButton>
        </>
      );
    }
    return (
      <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.manage"]} data-testid={`bar-combo-archive-${row.id}`} onClick={() => void archiveCombo(row.id, true)}>存档</PermissionButton>
    );
  }

  function openDetail(row: BarComboRow) {
    setDetail({
      title: `套餐明细 · ${row.comboName ?? row.id}`,
      fields: [
        { label: "套餐编号", value: row.id },
        { label: "套餐名", value: row.comboName ?? "—" },
        { label: "所属酒吧", value: row.barId ?? "—" },
        { label: "售价（元）", value: `¥${yuan(row.priceCent)}` },
        { label: "内部结算价（元）", value: row.internalSettlementPriceCent === null || row.internalSettlementPriceCent === undefined ? "未录入" : `¥${yuan(row.internalSettlementPriceCent)}` },
        { label: "含商品", value: (row.items ?? []).map((it) => `${it.productName ?? it.productId} ×${it.quantity}`).join("、") || "—" },
        { label: "图片", value: row.imageUrl ? row.imageUrl : "未上传" },
        { label: "状态", value: formatBusinessStatus(row.status) },
      ],
    });
  }

  function setField(key: keyof Omit<ComboForm, "items">, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }
  function setItem(index: number, key: keyof ComboItemForm, value: string) {
    setForm((current) => ({
      ...current,
      items: current.items.map((it, i) => (i === index ? { ...it, [key]: value } : it)),
    }));
  }
  function addItem() {
    setForm((current) => ({ ...current, items: [...current.items, { productId: "", quantity: "1" }] }));
  }
  function removeItem(index: number) {
    setForm((current) => ({ ...current, items: current.items.filter((_, i) => i !== index) }));
  }

  async function submit() {
    setFormError(null);
    const barId = form.barId.trim();
    const comboName = form.comboName.trim();
    const priceCent = toCent(form.priceYuan);
    const internalSettlementPriceCent = toCent(form.internalPriceYuan);
    if (!barId || !comboName) {
      setFormError("酒吧、套餐名必填");
      return;
    }
    if (priceCent === null || priceCent < 0) {
      setFormError("售价必须为非负数字");
      return;
    }
    const items = form.items
      .map((it) => ({ productId: it.productId.trim(), quantity: Number(it.quantity.trim() || "0") }))
      .filter((it) => it.productId !== "");
    if (items.length === 0) {
      setFormError("套餐至少包含一个商品");
      return;
    }
    if (items.some((it) => !Number.isInteger(it.quantity) || it.quantity <= 0)) {
      setFormError("每个组合项数量必须为正整数");
      return;
    }
    try {
      const created = await api.post<BarComboRow>("/api/v1/bar/catalog/combos", {
        barId,
        comboName,
        priceCent,
        internalSettlementPriceCent,
        status: "active",
        items,
      });
      setForm(EMPTY_COMBO_FORM);
      setCreateOpen(false);
      setActionResult({ action: "bar_combo_created", comboId: created?.id, comboName });
      await reload();
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : "套餐保存失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  return (
    <section data-testid="bar-combos-page">
      <AdminPageHeader
        title="套餐管理"
        description="酒吧套餐目录：从既有商品挑若干件组成套餐并定整体售价。套餐供平板图形化点单直接下单。内部结算价是平台↔酒吧成本结算价基，可空。"
      />

      {/* 引导式流程 phase-3：新建套餐入口按钮（权限门在入口），点击弹出分步引导。 */}
      <div className="admin-form" data-testid="bar-combo-create-form">
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.combo.manage"]} data-testid="bar-combo-create-open" onClick={() => { setFormError(null); setCreateOpen(true); }}>
            ＋ 新建套餐
          </PermissionButton>
        </div>
      </div>
      <GuidedFlowModal
        open={createOpen}
        title="新建套餐"
        testId="bar-combo-guided"
        submitTestId="bar-combo-submit"
        submitLabel="保存套餐"
        error={formError ?? undefined}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void submit()}
        steps={[
          {
            title: "套餐信息",
            description: "填写所属酒吧与套餐名。",
            validate: () => (form.barId.trim() && form.comboName.trim() ? null : "酒吧、套餐名必填"),
            content: (
              <>
                <label><span>酒吧 ID</span><input data-testid="bar-combo-bar-id" value={form.barId} onChange={(e) => setField("barId", e.target.value)} /></label>
                <label><span>套餐名</span><input data-testid="bar-combo-name" value={form.comboName} onChange={(e) => setField("comboName", e.target.value)} /></label>
              </>
            ),
          },
          {
            title: "定价与商品",
            description: "售价必填（非负），内部结算价可空；从既有商品挑至少一件组成套餐，数量为正整数。",
            validate: () => {
              const priceCent = toCent(form.priceYuan);
              if (priceCent === null || priceCent < 0) return "售价必须为非负数字";
              const items = form.items
                .map((it) => ({ productId: it.productId.trim(), quantity: Number(it.quantity.trim() || "0") }))
                .filter((it) => it.productId !== "");
              if (items.length === 0) return "套餐至少包含一个商品";
              if (items.some((it) => !Number.isInteger(it.quantity) || it.quantity <= 0)) return "每个组合项数量必须为正整数";
              return null;
            },
            content: (
              <>
                <label><span>售价（元）</span><input data-testid="bar-combo-price" inputMode="decimal" value={form.priceYuan} onChange={(e) => setField("priceYuan", e.target.value)} /></label>
                <label><span>内部结算价（元，可空）</span><input data-testid="bar-combo-internal-price" inputMode="decimal" value={form.internalPriceYuan} onChange={(e) => setField("internalPriceYuan", e.target.value)} /></label>
                <div data-testid="bar-combo-items">
                  <span>组合商品</span>
                  {form.items.map((it, index) => (
                    <div key={index} className="admin-form-row" data-testid={`bar-combo-item-${index}`}>
                      <select data-testid={`bar-combo-item-product-${index}`} value={it.productId} onChange={(e) => setItem(index, "productId", e.target.value)}>
                        <option value="">选择商品</option>
                        {products.map((p) => (
                          <option key={p.id} value={p.id}>{p.productName ?? p.id}</option>
                        ))}
                      </select>
                      <input data-testid={`bar-combo-item-qty-${index}`} inputMode="numeric" value={it.quantity} onChange={(e) => setItem(index, "quantity", e.target.value)} />
                      {form.items.length > 1 ? (
                        <button type="button" data-testid={`bar-combo-item-remove-${index}`} onClick={() => removeItem(index)}>移除</button>
                      ) : null}
                    </div>
                  ))}
                  <button type="button" data-testid="bar-combo-item-add" onClick={addItem}>新增组合项</button>
                </div>
              </>
            ),
          },
        ]}
        summary={[
          { label: "酒吧 ID", value: form.barId.trim() || "未填写" },
          { label: "套餐名", value: form.comboName.trim() || "未填写" },
          { label: "售价（元）", value: form.priceYuan.trim() || "未填写" },
          { label: "内部结算价（元）", value: form.internalPriceYuan.trim() || "不录入" },
          { label: "组合商品数", value: String(form.items.filter((it) => it.productId.trim() !== "").length) },
        ]}
      />

      <label className="admin-inline-check" data-testid="bar-combos-show-archived">
        <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
        显示已存档套餐
      </label>

      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-combos-table">
            <thead>
              <tr>
                <th>套餐</th>
                <th>售价（元）</th>
                <th>内部结算价（元）</th>
                <th>含商品</th>
                <th>状态</th>
                <th>存档状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-combo-row-${row.id}`}
                  onClick={() => openDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(row); } }}>
                  <td>
                    {row.comboName ?? row.id}
                    <br />
                    <small>{row.id}</small>
                  </td>
                  <td>{yuan(row.priceCent)}</td>
                  <td>{yuan(row.internalSettlementPriceCent)}</td>
                  <td>{(row.items ?? []).map((it) => `${it.productName ?? productName(it.productId)} ×${it.quantity}`).join("、") || "—"}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td data-testid={`bar-combo-archive-label-${row.id}`}>{archiveLabel(row)}</td>
                  <td onClick={(e) => e.stopPropagation()}>{comboActions(row)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
