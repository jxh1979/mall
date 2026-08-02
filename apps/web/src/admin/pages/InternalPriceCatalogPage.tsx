import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 内部结算价目录（内部结算 SKU 目录，ADM-STL-003）—— 批10/E（V379/V380），业务 HTML「内部结算价目录」节。
// 平台所有内部结算价集中在这里维护，是成本侧唯一真源、唯一可改价处：
//   条目硬绑提供商家（业态随商家带出）+ 归入品类，商家/业态/品类三维筛选；
//   新建/改价先落草案 → 具备高敏权限的他人复核转正 → 只对之后的核销/结算生效，转正同时同步四处业务快照；
//   核销/结算发生时当时价固化到流水，此后改价不影响已发生的账；条目可下线归档，不影响历史引用。
// 敏感：内部结算价仅授权后台人员可见（读端权限闸在后端，无权限直接 403）。
const BASE = "/api/v1/settlement/config";
const MANAGE = ["settlement.config.manage"];

type CatalogRow = {
  id: string;
  itemCode: string;
  merchantId: string;
  businessFormat?: string | null;
  category: string;
  itemName: string;
  priceCent: number;
  status: string;
  isDraft: boolean;
  updatedBy?: string | null;
  updatedAt?: string | null;
};

const FORMAT_LABELS: Record<string, string> = {
  bar: "酒吧",
  catering: "餐饮",
  retail: "零售",
  amusement: "游乐",
  other: "其他",
};

function yuan(cent: number | null | undefined): string {
  if (cent === null || cent === undefined) return "—";
  return (cent / 100).toFixed(2);
}

type Form = { itemCode: string; merchantId: string; category: string; itemName: string; priceYuan: string };
const EMPTY_FORM: Form = { itemCode: "", merchantId: "", category: "", itemName: "", priceYuan: "" };

export function InternalPriceCatalogPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<CatalogRow[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState<string | null>(null);

  // 三维筛选：商家（硬绑定）/ 业态（随商家带出）/ 品类（独立标签）；含归档档查看开关。
  const [filterMerchant, setFilterMerchant] = React.useState("");
  const [filterFormat, setFilterFormat] = React.useState("");
  const [filterCategory, setFilterCategory] = React.useState("");
  const [showArchived, setShowArchived] = React.useState(false);

  const [form, setForm] = React.useState<Form>(EMPTY_FORM);
  const [formError, setFormError] = React.useState<string | null>(null);
  const [archiveReason, setArchiveReason] = React.useState<Record<string, string>>({});

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("include_drafts", "true");
      if (showArchived) params.set("include_archived", "true");
      if (filterMerchant.trim()) params.set("merchant_id", filterMerchant.trim());
      if (filterFormat) params.set("business_format", filterFormat);
      if (filterCategory.trim()) params.set("category", filterCategory.trim());
      const data = await api.get<CatalogRow[]>(`${BASE}/internal-price-items?${params.toString()}`);
      setRows(Array.isArray(data) ? data : []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "目录加载失败");
    } finally {
      setLoading(false);
    }
  }, [api, filterMerchant, filterFormat, filterCategory, showArchived]);

  React.useEffect(() => {
    void load();
  }, [load]);

  const drafts = rows.filter((row) => row.isDraft && row.status === "active");
  const actives = rows.filter((row) => !row.isDraft);

  async function submitDraft() {
    setFormError(null);
    const merchantId = form.merchantId.trim();
    const category = form.category.trim();
    const itemName = form.itemName.trim();
    const priceYuan = form.priceYuan.trim();
    if (!merchantId || !category || !itemName || !priceYuan) {
      setFormError("提供商家、品类、条目名称、内部结算价都必填（条目编码留空=新建）");
      return;
    }
    const priceCent = Math.round(Number(priceYuan) * 100);
    if (!Number.isFinite(priceCent) || priceCent < 0) {
      setFormError("内部结算价必须为非负数字（0=商户让利型合法价）");
      return;
    }
    setBusy("save");
    try {
      const itemCode = await api.post<string>(`${BASE}/internal-price-item`, {
        idempotencyKey: `ipc-${Date.now()}`,
        itemCode: form.itemCode.trim() || null,
        merchantId,
        category,
        itemName,
        priceCent,
      });
      setActionResult({ action: "internal_price_item_draft_saved", itemCode });
      setForm(EMPTY_FORM);
      await load();
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : "草案保存失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setBusy(null);
    }
  }

  async function activate(row: CatalogRow) {
    setBusy(row.id);
    try {
      await api.post(`${BASE}/internal_price_item/${row.id}/activate`);
      setActionResult({ action: "internal_price_item_activated", itemCode: row.itemCode });
      await load();
    } catch (activateError) {
      setGlobalError(activateError instanceof Error ? activateError.message : "复核转正失败");
    } finally {
      setBusy(null);
    }
  }

  async function archive(row: CatalogRow) {
    const reason = (archiveReason[row.id] ?? "").trim();
    if (!reason) {
      setGlobalError("下线归档必须填写理由（软删留库，可追溯）");
      return;
    }
    setBusy(row.id);
    try {
      await api.post(`${BASE}/internal_price_item/${row.id}/archive`, {
        idempotencyKey: `ipc-arch-${Date.now()}`,
        archiveReason: reason,
      });
      setActionResult({ action: "internal_price_item_archived", itemCode: row.itemCode });
      await load();
    } catch (archiveError) {
      setGlobalError(archiveError instanceof Error ? archiveError.message : "下线归档失败");
    } finally {
      setBusy(null);
    }
  }

  return (
    <section className="admin-page" data-testid="internal-price-catalog-page">
      <AdminPageHeader
        title="内部结算价目录（内部结算 SKU 目录）"
        description="平台所有内部结算价集中在本目录维护，是成本侧唯一真源、唯一可改价处：条目必须绑定提供它的商家（业态随商家带出）并归入品类，商家/业态/品类三维可筛。酒吧商品、酒吧套餐、票务权益模板、券在各自页面引用条目编码即可复用；新建/改价先存草案，须由具备高敏权限的他人复核转正后才对之后的核销/结算生效；核销/结算发生时当时价固化到该笔流水，此后目录价怎么变都不影响已发生的账。条目可下线归档，不影响历史引用。"
      />

      <div className="admin-inline-form" data-testid="ipc-filters">
        <label><span>提供商家</span><input data-testid="ipc-filter-merchant" value={filterMerchant} onChange={(e) => setFilterMerchant(e.target.value)} placeholder="商户 ID" /></label>
        <label><span>业态（随商家带出）</span>
          <select data-testid="ipc-filter-format" value={filterFormat} onChange={(e) => setFilterFormat(e.target.value)}>
            <option value="">全部业态</option>
            {Object.entries(FORMAT_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
          </select>
        </label>
        <label><span>品类</span><input data-testid="ipc-filter-category" value={filterCategory} onChange={(e) => setFilterCategory(e.target.value)} placeholder="如 spirit / combo / dining" /></label>
        <label className="admin-inline-check"><input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />显示已归档</label>
      </div>

      <h3>新建条目 / 对既有条目改价（先落草案）</h3>
      <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>
        条目编码留空＝新建（保存后返回编码，业务页「就地新建」拿它建立引用）；填既有编码＝对该条目改价/改名/改类。
        草案不生效：须由另一位具备高敏权限的人在下方草案队列「复核转正」，才对之后的核销/结算生效，同时系统把四处业务快照同步为新价。绑定商户不可更改——要换提供方请新建条目。
      </p>
      <div className="admin-inline-form" data-testid="ipc-create-form">
        <label><span>条目编码（留空=新建）</span><input data-testid="ipc-form-code" value={form.itemCode} onChange={(e) => setForm({ ...form, itemCode: e.target.value })} /></label>
        <label><span>提供商家</span><input data-testid="ipc-form-merchant" value={form.merchantId} onChange={(e) => setForm({ ...form, merchantId: e.target.value })} placeholder="商户 ID（必填）" /></label>
        <label><span>品类</span><input data-testid="ipc-form-category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} placeholder="如 spirit / combo / dining" /></label>
        <label><span>条目名称</span><input data-testid="ipc-form-name" value={form.itemName} onChange={(e) => setForm({ ...form, itemName: e.target.value })} /></label>
        <label><span>内部结算价（元）</span><input data-testid="ipc-form-price" inputMode="decimal" value={form.priceYuan} onChange={(e) => setForm({ ...form, priceYuan: e.target.value })} placeholder="0=商户让利型" /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="ipc-form-submit" disabled={busy === "save"} onClick={() => void submitDraft()}>
          保存草案
        </PermissionButton>
      </div>
      {formError ? <p className="admin-error" data-testid="ipc-form-error">{formError}</p> : null}

      <AdminDataState loading={loading} error={error} onRetry={() => void load()}>
        <h3>草案复核队列（{drafts.length}）</h3>
        <p style={{ color: "var(--adm-text-3)", fontSize: 13 }}>草案本身不生效；不能自行转正自己起草的改动，须另一位具备高敏权限的人复核批准。</p>
        <table className="admin-table" data-testid="ipc-draft-table">
          <thead><tr><th>条目编码</th><th>提供商家</th><th>业态</th><th>品类</th><th>名称</th><th>草案价（元）</th><th>起草人</th><th>操作</th></tr></thead>
          <tbody>
            {drafts.length === 0 ? (
              <tr><td colSpan={8} data-testid="ipc-draft-empty">暂无待复核草案</td></tr>
            ) : drafts.map((row) => (
              <tr key={row.id} data-testid={`ipc-draft-${row.itemCode}`}>
                <td>{row.itemCode}</td>
                <td>{row.merchantId}</td>
                <td>{FORMAT_LABELS[row.businessFormat ?? ""] ?? row.businessFormat ?? "—"}</td>
                <td>{row.category}</td>
                <td>{row.itemName}</td>
                <td className="admin-num">¥{yuan(row.priceCent)}</td>
                <td>{row.updatedBy ?? "—"}</td>
                <td>
                  <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`ipc-activate-${row.itemCode}`} disabled={busy === row.id} onClick={() => void activate(row)}>
                    复核转正
                  </PermissionButton>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <h3>生效条目（{actives.filter((row) => row.status === "active").length}）</h3>
        <table className="admin-table" data-testid="ipc-active-table">
          <thead><tr><th>条目编码</th><th>提供商家</th><th>业态</th><th>品类</th><th>名称</th><th>生效价（元）</th><th>状态</th><th>下线归档</th></tr></thead>
          <tbody>
            {actives.length === 0 ? (
              <tr><td colSpan={8} data-testid="ipc-active-empty">目录暂无条目：新建条目或在业务页「就地新建」后，这里就是全平台内部结算价的唯一真源。</td></tr>
            ) : actives.map((row) => (
              <tr key={row.id} data-testid={`ipc-active-${row.itemCode}`}>
                <td>{row.itemCode}</td>
                <td>{row.merchantId}</td>
                <td>{FORMAT_LABELS[row.businessFormat ?? ""] ?? row.businessFormat ?? "—"}</td>
                <td>{row.category}</td>
                <td>{row.itemName}</td>
                <td className="admin-num" data-testid={`ipc-price-${row.itemCode}`}>¥{yuan(row.priceCent)}</td>
                <td>{row.status === "active" ? "生效中" : "已下线（历史留档）"}</td>
                <td>
                  {row.status === "active" ? (
                    <span className="admin-form-row">
                      <input data-testid={`ipc-archive-reason-${row.itemCode}`} value={archiveReason[row.id] ?? ""} placeholder="下线理由（必填）" onChange={(e) => setArchiveReason({ ...archiveReason, [row.id]: e.target.value })} />
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`ipc-archive-${row.itemCode}`} disabled={busy === row.id} onClick={() => void archive(row)}>
                        下线归档
                      </PermissionButton>
                    </span>
                  ) : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </AdminDataState>
    </section>
  );
}
