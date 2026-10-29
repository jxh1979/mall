import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, BarComboRow, BarProductRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowActions, type RowAction } from "../components/RowActions";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { BarVenueSelect } from "../components/BarVenueSelect";

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
  catalogItemCode: string;
  items: ComboItemForm[];
};
const EMPTY_COMBO_FORM: ComboForm = { barId: "", comboName: "", priceYuan: "", catalogItemCode: "", items: [{ productId: "", quantity: "1" }] };

/**
 * 酒吧套餐管理页（ADM-BAR-006，V257 图形化点单 POS 阶段1）：
 *   后台从既有 SKU（bar_products）挑若干件组成套餐，定套餐整体售价与内部结算价。供平板图形化点单直接下套餐。
 * 查看需 bar.combo.view；维护需 bar.combo.manage（含内部结算价，敏感）。
 */
export function BarCombosPage({ api, setActionResult, setGlobalError, effectivePermissions, reportViewedObjects }: AdminPageProps) {
  const [rows, setRows] = React.useState<BarComboRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("bar_combo", rows), [rows, reportViewedObjects]);
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
      let reason: string | undefined;
      if (archive) {
        const r = window.prompt("存档（下线）理由：");
        if (r === null) return;
        if (!r.trim()) { setGlobalError("存档理由必填"); return; }
        reason = r.trim();
      }
      await api.post(`/api/v1/bar/catalog/combos/${id}/${archive ? "archive" : "unarchive"}`, reason ? { reason } : {});
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
    const actions: RowAction[] = row.deleteStatus === "pending"
      ? [
          { key: "approve", label: "批准删除", permissions: ["bar.combo.approve"], onClick: () => void approveDeleteCombo(row.id, "approved"), testId: `bar-combo-delete-approve-${row.id}` },
          { key: "reject", label: "驳回", permissions: ["bar.combo.approve"], danger: true, onClick: () => void approveDeleteCombo(row.id, "rejected"), testId: `bar-combo-delete-reject-${row.id}` },
        ]
      : row.archivedAt
      ? [
          { key: "unarchive", label: "取消存档", permissions: ["bar.combo.manage"], onClick: () => void archiveCombo(row.id, false), testId: `bar-combo-unarchive-${row.id}` },
          { key: "delete-request", label: "申请删除", permissions: ["bar.combo.manage"], danger: true, onClick: () => void requestDeleteCombo(row.id), testId: `bar-combo-delete-request-${row.id}` },
        ]
      : [
          { key: "archive", label: "存档", permissions: ["bar.combo.manage"], onClick: () => void archiveCombo(row.id, true), testId: `bar-combo-archive-${row.id}` },
        ];
    return <RowActions actions={actions} permissionsState={effectivePermissions} testId={`bar-combo-actions-${row.id}`} />;
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
    // 批10/E：内部结算价唯一可编辑处 = 内部结算 SKU 目录；套餐侧只挂目录条目引用（更新留空 = 保留既有引用）。
    const catalogItemCode = form.catalogItemCode.trim();
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
        catalogItemCode: catalogItemCode || null,
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
                <label><span>所属酒吧</span><BarVenueSelect api={api} testId="bar-combo-bar-id" value={form.barId} onChange={(v) => setField("barId", v)} setGlobalError={setGlobalError} /></label>
                <label><span>套餐名</span><input data-testid="bar-combo-name" value={form.comboName} onChange={(e) => setField("comboName", e.target.value)} /></label>
              </>
            ),
          },
          {
            title: "定价与商品",
            description: "售价必填（非负）。内部结算价已收编进「内部结算价目录」：此处只填目录条目编码建立引用（可空），改价一律回目录走草案复核；从既有商品挑至少一件组成套餐，数量为正整数。",
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
                <label><span>内部结算价目录条目编码（可空）</span><input data-testid="bar-combo-catalog-code" value={form.catalogItemCode} onChange={(e) => setField("catalogItemCode", e.target.value)} placeholder="在「结算 → 内部结算价目录」页新建/查询条目编码" /></label>
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
          { label: "内部结算价目录条目", value: form.catalogItemCode.trim() || "不引用" },
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
              <th>详情</th></tr>
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
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看套餐详情" onClick={(event) => { event.stopPropagation(); openDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
