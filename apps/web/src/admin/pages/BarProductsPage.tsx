import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, BarProductRow } from "../types";
import { PermissionButton } from "../security/Can";

/** 分 → 元展示（两位小数）；空/未录入显示占位。 */
function yuan(cent: number | null | undefined): string {
  if (cent === null || cent === undefined) return "—";
  return (cent / 100).toFixed(2);
}

/** 元输入 → 分（四舍五入到整数分）；空串返回 null。 */
function toCent(input: string): number | null {
  const trimmed = input.trim();
  if (trimmed === "") return null;
  const value = Number(trimmed);
  if (!Number.isFinite(value)) return null;
  return Math.round(value * 100);
}

type ProductForm = {
  barId: string;
  productName: string;
  category: string;
  priceYuan: string;
  internalPriceYuan: string;
};

const EMPTY_FORM: ProductForm = { barId: "", productName: "", category: "spirit", priceYuan: "", internalPriceYuan: "" };

/**
 * 酒吧商品管理页（ADM-BAR-005）：列商品 + 录入售价/内部结算价。
 * 内部结算价（V152 internal_settlement_price_cent）是平台↔酒吧的成本结算价基，属敏感数据：
 *   ① 通票免费酒水核销驱动平台↔酒吧结算按此价；② 吧台随单赠送项按此价 × 数量从销售提成基数扣除（F-TXN-05）。
 * 查看需 bar.product.view；录入/修改需高危权限 bar.product.manage（与 /bar/products 接口守卫一致）。
 */
export function BarProductsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<BarProductRow[]>([]);
  const [form, setForm] = React.useState<ProductForm>(EMPTY_FORM);
  const [formError, setFormError] = React.useState<string | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<BarProductRow[]>("/api/v1/bar/products");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "商品列表读取失败" },
  );

  function setField(key: keyof ProductForm, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function submit() {
    setFormError(null);
    const barId = form.barId.trim();
    const productName = form.productName.trim();
    const category = form.category.trim();
    const priceCent = toCent(form.priceYuan);
    const internalSettlementPriceCent = toCent(form.internalPriceYuan);
    if (!barId || !productName || !category) {
      setFormError("酒吧、商品名、类别必填");
      return;
    }
    if (priceCent === null || priceCent < 0) {
      setFormError("售价必须为非负数字");
      return;
    }
    if (form.internalPriceYuan.trim() !== "" && (internalSettlementPriceCent === null || internalSettlementPriceCent < 0)) {
      setFormError("内部结算价必须为非负数字（留空=不录入，该商品不参与结算/提成扣减）");
      return;
    }
    try {
      const created = await api.post<BarProductRow>("/api/v1/bar/products", {
        barId,
        productName,
        category,
        priceCent,
        status: "active",
        internalSettlementPriceCent,
      });
      setForm(EMPTY_FORM);
      setActionResult({ action: "bar_product_created", productId: created?.id, productName });
      await reload();
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : "商品录入失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  return (
    <section data-testid="bar-products-page">
      <AdminPageHeader
        title="商品管理"
        description="酒吧商品目录：录入售价与内部结算价。内部结算价是平台↔酒吧的成本结算价基，用于通票免费酒水平台↔酒吧结算与吧台赠送项按内部价扣减销售提成；留空表示该商品不参与结算/提成扣减。"
      />

      <div className="admin-form" data-testid="bar-product-create-form">
        <label><span>酒吧 ID</span><input data-testid="bar-product-bar-id" value={form.barId} onChange={(e) => setField("barId", e.target.value)} /></label>
        <label><span>商品名</span><input data-testid="bar-product-name" value={form.productName} onChange={(e) => setField("productName", e.target.value)} /></label>
        <label><span>类别</span><input data-testid="bar-product-category" value={form.category} onChange={(e) => setField("category", e.target.value)} /></label>
        <label><span>售价（元）</span><input data-testid="bar-product-price" inputMode="decimal" value={form.priceYuan} onChange={(e) => setField("priceYuan", e.target.value)} /></label>
        <label><span>内部结算价（元，可空）</span><input data-testid="bar-product-internal-price" inputMode="decimal" value={form.internalPriceYuan} onChange={(e) => setField("internalPriceYuan", e.target.value)} /></label>
        {formError ? <div className="admin-inline-error" data-testid="bar-product-form-error">{formError}</div> : null}
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={["bar.product.manage"]}
          data-testid="bar-product-submit"
          onClick={() => void submit()}
        >
          录入商品
        </PermissionButton>
      </div>

      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-products-table">
            <thead>
              <tr>
                <th>商品</th>
                <th>类别</th>
                <th>售价（元）</th>
                <th>内部结算价（元）</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`bar-product-row-${row.id}`}>
                  <td>
                    {row.productName ?? row.id}
                    <br />
                    <small>{row.id}</small>
                  </td>
                  <td>{row.productType ?? "—"}</td>
                  <td>{yuan(row.priceCent)}</td>
                  <td data-testid={`bar-product-internal-price-${row.id}`}>{yuan(row.internalSettlementPriceCent)}</td>
                  <td>{row.soldOut ? "沽清" : row.status ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
