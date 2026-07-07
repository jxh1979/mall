import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useAdminResource } from "../hooks/useAdminResource";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps, BarProductRow, BarProductCategoryRow } from "../types";
import { PermissionButton } from "../security/Can";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { BarVenueSelect } from "../components/BarVenueSelect";

// 商品类别码 → 中文（零英文强制；不进共享码表，就近页级映射）。未知码原样返回。
// 说明：这是既有的 product_type 兼容字段（粗类别）；V256 起「可配置分类」走 categoryId，两者并存展示。
const CATEGORY_LABELS: Record<string, string> = {
  spirit: "烈酒",
  wine: "葡萄酒",
  beer: "啤酒",
  cocktail: "鸡尾酒",
  soft_drink: "软饮",
  mixer: "调饮",
  tea: "茶饮",
  snack: "小吃",
};
function categoryLabel(code: string | null | undefined): string {
  if (!code) return "—";
  return CATEGORY_LABELS[code] ?? code;
}

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

type CategoryForm = { barId: string; categoryName: string; sortOrder: string };
const EMPTY_CATEGORY_FORM: CategoryForm = { barId: "", categoryName: "", sortOrder: "0" };

/**
 * 酒吧商品管理页（ADM-BAR-005，V256 图形化点单 POS 阶段1扩展）：
 *   ① 分类可配置（后台自建分类表 bar_product_categories，商品挂 categoryId，供 POS 按分类分屏列菜）；
 *   ② 给商品挂分类、上传图片（本地存储，同 image_url 先例）；
 *   ③ 录入售价/内部结算价（原有能力保留）。
 * product_type（粗类别，历史兼容）保留展示；categoryId 为新的可配置精分类。
 * 查看需 bar.product.view；商品录入/改需 bar.product.manage；分类维护需 bar.category.manage。
 */
export function BarProductsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<BarProductRow[]>([]);
  const [categories, setCategories] = React.useState<BarProductCategoryRow[]>([]);
  const [form, setForm] = React.useState<ProductForm>(EMPTY_FORM);
  const [categoryForm, setCategoryForm] = React.useState<CategoryForm>(EMPTY_CATEGORY_FORM);
  const [formError, setFormError] = React.useState<string | null>(null);
  const [categoryError, setCategoryError] = React.useState<string | null>(null);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // V262 存档/删除范式：默认列表不含已存档，勾选后带出。
  const [showArchived, setShowArchived] = React.useState(false);
  // 引导式流程 phase-3：新建商品/新建分类收起为主按钮，点击弹出分步引导（GuidedFlowModal）。
  const [productOpen, setProductOpen] = React.useState(false);
  const [categoryOpen, setCategoryOpen] = React.useState(false);

  const categoryName = React.useCallback(
    (id: string | null | undefined): string => {
      if (!id) return "未分类";
      return categories.find((c) => c.id === id)?.categoryName ?? id;
    },
    [categories],
  );

  function openProductDetail(row: BarProductRow) {
    setDetail({
      title: `商品明细 · ${row.productName ?? row.id}`,
      fields: [
        { label: "商品编号", value: row.id },
        { label: "商品", value: row.productName ?? "—" },
        { label: "所属酒吧", value: row.barId ?? "—" },
        { label: "类别", value: categoryLabel(row.productType) },
        { label: "分类", value: categoryName(row.categoryId) },
        { label: "售价（元）", value: `¥${yuan(row.priceCent)}` },
        { label: "内部结算价（元）", value: row.internalSettlementPriceCent === null || row.internalSettlementPriceCent === undefined ? "未录入" : `¥${yuan(row.internalSettlementPriceCent)}` },
        { label: "图片", value: row.imageUrl ? row.imageUrl : "未上传" },
        { label: "状态", value: row.soldOut ? "沽清" : formatBusinessStatus(row.status) },
      ],
    });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<BarProductRow[]>(`/api/v1/bar/products?includeArchived=${showArchived}`);
      setRows(items ?? []);
      try {
        const cats = await api.get<BarProductCategoryRow[]>(`/api/v1/bar/catalog/categories?includeArchived=${showArchived}`);
        setCategories(Array.isArray(cats) ? cats : []);
      } catch {
        setCategories([]);
      }
    },
    { onError: setGlobalError, fallbackMessage: "商品列表读取失败" },
  );


  // V262 存档/删除范式:切换「显示已存档」后重新拉取(后端按 includeArchived 过滤),首次挂载不触发。
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

  async function archiveEntity(kind: "products" | "categories", id: string, archive: boolean) {
    try {
      await api.post(`/api/v1/bar/catalog/${kind}/${id}/${archive ? "archive" : "unarchive"}`, {});
      setActionResult({ action: `bar_${kind}_${archive ? "archived" : "unarchived"}`, id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "存档操作失败");
    }
  }

  async function requestDeleteEntity(kind: "products" | "categories", id: string) {
    const reason = window.prompt("删除理由（提交后需上级审批）：");
    if (reason === null) {
      return; // 用户点「取消」= 中止申请。
    }
    try {
      await api.post(`/api/v1/bar/catalog/${kind}/${id}/delete-request`, { reason });
      setActionResult({ action: `bar_${kind}_delete_requested`, id });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除申请失败");
    }
  }

  async function approveDeleteEntity(kind: "products" | "categories", id: string, decision: "approved" | "rejected") {
    try {
      const decided = await api.post<{ deleted?: boolean }>(`/api/v1/bar/catalog/${kind}/${id}/delete-approve`, { decision });
      setActionResult({ action: `bar_${kind}_delete_${decision}`, id, deleted: decided?.deleted ?? false });
      await reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除审批失败");
    }
  }

  function catalogActions(kind: "products" | "categories", row: { id: string; archivedAt?: string | null; deleteStatus?: string }) {
    const manage = kind === "products" ? ["bar.product.manage"] : ["bar.category.manage"];
    const approve = kind === "products" ? ["bar.product.approve"] : ["bar.category.approve"];
    const prefix = kind === "products" ? "bar-product" : "bar-category";
    if (row.deleteStatus === "pending") {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={approve} data-testid={`${prefix}-delete-approve-${row.id}`} onClick={() => void approveDeleteEntity(kind, row.id, "approved")}>批准删除</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={approve} data-testid={`${prefix}-delete-reject-${row.id}`} onClick={() => void approveDeleteEntity(kind, row.id, "rejected")}>驳回</PermissionButton>
        </>
      );
    }
    if (row.archivedAt) {
      return (
        <>
          <PermissionButton permissionsState={effectivePermissions} permissions={manage} data-testid={`${prefix}-unarchive-${row.id}`} onClick={() => void archiveEntity(kind, row.id, false)}>取消存档</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={manage} data-testid={`${prefix}-delete-request-${row.id}`} onClick={() => void requestDeleteEntity(kind, row.id)}>申请删除</PermissionButton>
        </>
      );
    }
    return (
      <PermissionButton permissionsState={effectivePermissions} permissions={manage} data-testid={`${prefix}-archive-${row.id}`} onClick={() => void archiveEntity(kind, row.id, true)}>存档</PermissionButton>
    );
  }

  function setField(key: keyof ProductForm, value: string) {
    setForm((current) => ({ ...current, [key]: value }));
  }
  function setCategoryField(key: keyof CategoryForm, value: string) {
    setCategoryForm((current) => ({ ...current, [key]: value }));
  }

  async function submitCategory() {
    setCategoryError(null);
    const barId = categoryForm.barId.trim();
    const name = categoryForm.categoryName.trim();
    if (!barId || !name) {
      setCategoryError("酒吧、分类名必填");
      return;
    }
    const sortOrder = Number(categoryForm.sortOrder.trim() || "0");
    try {
      await api.post("/api/v1/bar/catalog/categories", { barId, categoryName: name, sortOrder });
      setCategoryForm({ ...EMPTY_CATEGORY_FORM, barId });
      setCategoryOpen(false);
      setActionResult({ action: "bar_category_created", categoryName: name });
      await reload();
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : "分类保存失败";
      setCategoryError(message);
      setGlobalError(message);
    }
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
      setProductOpen(false);
      setActionResult({ action: "bar_product_created", productId: created?.id, productName });
      await reload();
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : "商品录入失败";
      setFormError(message);
      setGlobalError(message);
    }
  }

  async function changeCategory(row: BarProductRow, categoryId: string) {
    try {
      await api.post(`/api/v1/bar/catalog/products/${row.id}/category`, { categoryId: categoryId || null });
      setActionResult({ action: "bar_product_category_assigned", productId: row.id, categoryId: categoryId || null });
      await reload();
    } catch (assignError) {
      setGlobalError(assignError instanceof Error ? assignError.message : "设分类失败");
    }
  }

  async function uploadImage(row: BarProductRow, file: File) {
    try {
      if (!api.postForm) {
        setGlobalError("当前环境不支持图片上传");
        return;
      }
      const fd = new FormData();
      fd.append("file", file);
      fd.append("product_id", row.id);
      await api.postForm("/api/v1/bar/catalog/images", fd);
      setActionResult({ action: "bar_product_image_uploaded", productId: row.id });
      await reload();
    } catch (uploadError) {
      setGlobalError(uploadError instanceof Error ? uploadError.message : "图片上传失败");
    }
  }

  // 可指派分类：启用、未存档、非待删（引用侧守卫的前端同口径；后端 BAR_CATEGORY_NOT_ASSIGNABLE 兜底）。
  const activeCategories = categories.filter((c) => c.status !== "archived" && !c.archivedAt && (c.deleteStatus ?? "none") === "none");

  return (
    <section data-testid="bar-products-page">
      <AdminPageHeader
        title="商品管理"
        description="酒吧商品目录：维护可配置分类、给商品挂分类与上传图片、录入售价与内部结算价。分类供平板图形化点单按分类分屏列菜；图片存服务器本地。内部结算价是平台↔酒吧成本结算价基，留空表示不参与结算/提成扣减。"
      />

      <div className="admin-form" data-testid="bar-category-create-form">
        <h3>分类维护（供图形化点单分屏）</h3>
        {/* 引导式流程 phase-3：新建分类入口按钮（权限门在入口），点击弹出分步引导。 */}
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.category.manage"]} data-testid="bar-category-create-open" onClick={() => { setCategoryError(null); setCategoryOpen(true); }}>
            ＋ 新建分类
          </PermissionButton>
        </div>
        <GuidedFlowModal
          open={categoryOpen}
          title="新建分类"
          testId="bar-category-guided"
          submitTestId="bar-category-submit"
          submitLabel="保存分类"
          error={categoryError ?? undefined}
          onClose={() => setCategoryOpen(false)}
          onSubmit={() => void submitCategory()}
          steps={[
            {
              title: "分类信息",
              description: "填写所属酒吧与分类名，排序小的在前。",
              validate: () => (categoryForm.barId.trim() && categoryForm.categoryName.trim() ? null : "酒吧、分类名必填"),
              content: (
                <>
                  <label><span>所属酒吧</span><BarVenueSelect api={api} testId="bar-category-bar-id" value={categoryForm.barId} onChange={(v) => setCategoryField("barId", v)} setGlobalError={setGlobalError} /></label>
                  <label><span>分类名</span><input data-testid="bar-category-name" value={categoryForm.categoryName} onChange={(e) => setCategoryField("categoryName", e.target.value)} /></label>
                  <label><span>排序（小在前）</span><input data-testid="bar-category-sort" inputMode="numeric" value={categoryForm.sortOrder} onChange={(e) => setCategoryField("sortOrder", e.target.value)} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "酒吧 ID", value: categoryForm.barId.trim() || "未填写" },
            { label: "分类名", value: categoryForm.categoryName.trim() || "未填写" },
            { label: "排序", value: categoryForm.sortOrder.trim() || "0" },
          ]}
        />
        {categories.length > 0 ? (
          <div className="admin-table-scroll" data-testid="bar-category-list">
            <table className="admin-table" data-testid="bar-categories-table">
              <thead>
                <tr>
                  <th>分类</th>
                  <th>排序</th>
                  <th>状态</th>
                  <th>存档状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((c) => (
                  <tr key={c.id} data-testid={`bar-category-row-${c.id}`}>
                    <td>{c.categoryName ?? c.id}<br /><small>{c.id}</small></td>
                    <td>{c.sortOrder ?? 0}</td>
                    <td>{c.status === "archived" ? "停用" : "启用"}</td>
                    <td data-testid={`bar-category-archive-label-${c.id}`}>{archiveLabel(c)}</td>
                    <td>{catalogActions("categories", c)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>

      <div className="admin-form" data-testid="bar-product-create-form">
        <h3>商品录入</h3>
        {/* 引导式流程 phase-3：新建商品入口按钮（权限门在入口），点击弹出分步引导。图片上传仍在下方商品列表行内逐件上传（后端按 product_id 挂图）。 */}
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.product.manage"]} data-testid="bar-product-create-open" onClick={() => { setFormError(null); setProductOpen(true); }}>
            ＋ 新建商品
          </PermissionButton>
        </div>
        <GuidedFlowModal
          open={productOpen}
          title="新建商品"
          testId="bar-product-guided"
          submitTestId="bar-product-submit"
          submitLabel="录入商品"
          error={formError ?? undefined}
          onClose={() => setProductOpen(false)}
          onSubmit={() => void submit()}
          steps={[
            {
              title: "商品信息",
              description: "填写所属酒吧、商品名与类别。",
              validate: () => (form.barId.trim() && form.productName.trim() && form.category.trim() ? null : "酒吧、商品名、类别必填"),
              content: (
                <>
                  <label><span>所属酒吧</span><BarVenueSelect api={api} testId="bar-product-bar-id" value={form.barId} onChange={(v) => setField("barId", v)} setGlobalError={setGlobalError} /></label>
                  <label><span>商品名</span><input data-testid="bar-product-name" value={form.productName} onChange={(e) => setField("productName", e.target.value)} /></label>
                  <label><span>类别</span><input data-testid="bar-product-category" value={form.category} onChange={(e) => setField("category", e.target.value)} /></label>
                </>
              ),
            },
            {
              title: "定价与图片",
              description: "售价必填（非负），内部结算价可空（留空=不参与结算/提成扣减）；商品图片在录入后于下方列表按行上传。",
              validate: () => {
                const priceCent = toCent(form.priceYuan);
                if (priceCent === null || priceCent < 0) return "售价必须为非负数字";
                const internal = toCent(form.internalPriceYuan);
                if (form.internalPriceYuan.trim() !== "" && (internal === null || internal < 0)) return "内部结算价必须为非负数字（留空=不录入）";
                return null;
              },
              content: (
                <>
                  <label><span>售价（元）</span><input data-testid="bar-product-price" inputMode="decimal" value={form.priceYuan} onChange={(e) => setField("priceYuan", e.target.value)} /></label>
                  <label><span>内部结算价（元，可空）</span><input data-testid="bar-product-internal-price" inputMode="decimal" value={form.internalPriceYuan} onChange={(e) => setField("internalPriceYuan", e.target.value)} /></label>
                  <div className="admin-preview" data-testid="bar-product-image-hint">图片：商品录入后在下方列表对应行上传（按商品挂图）。</div>
                </>
              ),
            },
          ]}
          summary={[
            { label: "酒吧 ID", value: form.barId.trim() || "未填写" },
            { label: "商品名", value: form.productName.trim() || "未填写" },
            { label: "类别", value: form.category.trim() || "未填写" },
            { label: "售价（元）", value: form.priceYuan.trim() || "未填写" },
            { label: "内部结算价（元）", value: form.internalPriceYuan.trim() || "不录入" },
          ]}
        />
      </div>

      <label className="admin-inline-check" data-testid="bar-catalog-show-archived">
        <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
        显示已存档分类与商品
      </label>

      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-products-table">
            <thead>
              <tr>
                <th>商品</th>
                <th>类别</th>
                <th>分类（可配置）</th>
                <th>售价（元）</th>
                <th>内部结算价（元）</th>
                <th>图片</th>
                <th>状态</th>
                <th>存档状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-product-row-${row.id}`}
                  onClick={() => openProductDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProductDetail(row); } }}>
                  <td>
                    {row.productName ?? row.id}
                    <br />
                    <small>{row.id}</small>
                  </td>
                  <td>{categoryLabel(row.productType)}</td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <select
                      data-testid={`bar-product-category-select-${row.id}`}
                      value={row.categoryId ?? ""}
                      onChange={(e) => void changeCategory(row, e.target.value)}
                    >
                      <option value="">未分类</option>
                      {activeCategories.map((c) => (
                        <option key={c.id} value={c.id}>{c.categoryName ?? c.id}</option>
                      ))}
                    </select>
                  </td>
                  <td>{yuan(row.priceCent)}</td>
                  <td data-testid={`bar-product-internal-price-${row.id}`}>{yuan(row.internalSettlementPriceCent)}</td>
                  <td data-testid={`bar-product-image-${row.id}`} onClick={(e) => e.stopPropagation()}>
                    {row.imageUrl ? <small>{row.imageUrl}</small> : <small>未上传</small>}
                    <br />
                    <input
                      type="file"
                      accept="image/*"
                      data-testid={`bar-product-image-input-${row.id}`}
                      onChange={(e) => { const f = e.target.files?.[0]; if (f) void uploadImage(row, f); }}
                    />
                  </td>
                  <td>{row.soldOut ? "沽清" : formatBusinessStatus(row.status)}</td>
                  <td data-testid={`bar-product-archive-label-${row.id}`}>{archiveLabel(row)}</td>
                  <td onClick={(e) => e.stopPropagation()}>{catalogActions("products", row)}</td>
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
