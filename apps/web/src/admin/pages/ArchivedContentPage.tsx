// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { cnDate } from "../components/RowDetailDrawer";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 BE 段第 343 条 343-R1（BSM:569「后台另设「已存档内容」页，进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。」；
// BSM:571「所有存档与删除的数据都按分类（票种、商品、商户、员工、设备、工单、预订等）分门别类归拢，便于按类查阅、恢复与清理。」）：
// 本页不另设接口——每个分类读该类对象已有的「含已存档」列表、恢复走该类对象已有的「取消存档」接口，
// 查看范围与恢复权限都由各接口照原样把关（能在原页面看到、恢复的，在这里才看得到、恢复得了）。

type Row = Record<string, unknown> & { id: string };

type Category = {
  key: string;
  label: string;
  /** 查看该类已存档对象所需权限（同原页面）。 */
  view: string[];
  /** 恢复（取消存档）所需权限（同原页面）。 */
  restore: string[];
  load: (api: AdminApiClient) => Promise<Row[]>;
  restorePath: (id: string) => string;
  /** 默认按 archivedAt 判；个别对象用状态判。 */
  isArchived?: (row: Row) => boolean;
};

function rowsOf(value: unknown): Row[] {
  if (Array.isArray(value)) return value as Row[];
  if (value && typeof value === "object" && Array.isArray((value as { items?: unknown }).items)) return (value as { items: Row[] }).items;
  return [];
}

const listOf = (path: string) => async (api: AdminApiClient) => rowsOf(await api.get<unknown>(path));

// 商户列表分页返回：逐页取完，免得已存档的落在后面几页看不到。
async function loadMerchants(api: AdminApiClient): Promise<Row[]> {
  const size = 100;
  const all: Row[] = [];
  for (let page = 1; ; page += 1) {
    const result = await api.getPaged<Row>(`/api/v1/platform/merchants?includeArchived=true&page=${page}&size=${size}`);
    all.push(...result.items);
    if (result.items.length < size) return all;
  }
}

export const ARCHIVED_CATEGORIES: Category[] = [
  { key: "merchants", label: "商户档案", view: ["merchant.profile.view"], restore: ["merchant.profile.manage"],
    load: loadMerchants, restorePath: (id) => `/api/v1/platform/merchants/${id}/unarchive` },
  { key: "ticket-types", label: "票种", view: ["ticket.product.view"], restore: ["ticket.product.manage"],
    load: listOf("/api/v1/ticketing/ticket-types?includeArchived=true"), restorePath: (id) => `/api/v1/ticketing/ticket-types/${id}/unarchive` },
  { key: "price-rules", label: "票价规则", view: ["ticket.price_variant.view"], restore: ["ticket.price_variant.manage"],
    load: listOf("/api/v1/ticketing/price-rules?includeArchived=true"), restorePath: (id) => `/api/v1/ticketing/price-rules/${id}/unarchive` },
  { key: "entitlement-templates", label: "通票权益模板", view: ["ticket.entitlement.view"], restore: ["ticket.entitlement.manage"],
    load: listOf("/api/v1/ticketing/entitlement-templates?includeArchived=true"), restorePath: (id) => `/api/v1/ticketing/entitlement-templates/${id}/unarchive` },
  { key: "bar-products", label: "酒吧商品", view: ["bar.product.view"], restore: ["bar.product.manage"],
    load: listOf("/api/v1/bar/products?includeArchived=true"), restorePath: (id) => `/api/v1/bar/catalog/products/${id}/unarchive` },
  { key: "bar-categories", label: "酒吧商品分类", view: ["bar.category.view"], restore: ["bar.category.manage"],
    load: listOf("/api/v1/bar/catalog/categories?includeArchived=true"), restorePath: (id) => `/api/v1/bar/catalog/categories/${id}/unarchive` },
  { key: "bar-combos", label: "酒吧套餐", view: ["bar.combo.view"], restore: ["bar.combo.manage"],
    load: listOf("/api/v1/bar/catalog/combos?includeArchived=true"), restorePath: (id) => `/api/v1/bar/catalog/combos/${id}/unarchive` },
  { key: "bar-reservations", label: "卡座预订", view: ["bar.reservation.view"], restore: ["bar.reservation.manage"],
    load: listOf("/api/v1/bar/reservations?include_archived=true"), restorePath: (id) => `/api/v1/bar/reservations/${id}/unarchive` },
  { key: "staff-profiles", label: "员工档案", view: ["staff.profile.view"], restore: ["staff.profile.submit"],
    load: listOf("/api/v1/staff/profiles?include_archived=true"), restorePath: (id) => `/api/v1/staff/profiles/${id}/unarchive` },
  { key: "facility-equipment", label: "设施设备", view: ["facility.equipment.manage"], restore: ["facility.equipment.manage"],
    load: listOf("/api/v1/facility/equipment?include_archived=true"), restorePath: (id) => `/api/v1/facility/equipment/${id}/unarchive` },
  { key: "merchant-work-orders", label: "商户服务工单", view: ["merchant.service_work_order.view"], restore: ["merchant.service_work_order.close"],
    load: listOf("/api/v1/merchant-service/work-orders?include_archived=true"), restorePath: (id) => `/api/v1/merchant-service/work-orders/${id}/unarchive` },
  { key: "service-catalog", label: "服务项目", view: ["service.catalog.manage"], restore: ["service.catalog.manage"],
    load: listOf("/api/v1/services/catalog/all"), restorePath: (id) => `/api/v1/services/catalog/${id}/unarchive` },
  { key: "space-types", label: "空间类型", view: ["governance.space.view"], restore: ["governance.space.manage"],
    load: listOf("/api/v1/platform/space-types"), restorePath: (id) => `/api/v1/platform/space-types/${id}/unarchive`,
    isArchived: (row) => row.status === "archived" },
];

/** 本页入口权限：持任一分类的查看权限即可进入（AdminApp 按任一即可判）。 */
export const ARCHIVED_CONTENT_VIEW_PERMISSIONS = Array.from(new Set(ARCHIVED_CATEGORIES.flatMap((category) => category.view)));

const NAME_KEYS = ["merchantName", "ticketName", "productName", "categoryName", "comboName", "typeName", "name", "title", "ruleName", "customerName"];

function text(value: unknown): string {
  return typeof value === "string" && value.trim() ? value : typeof value === "number" ? String(value) : "";
}

function displayName(row: Row): string {
  for (const key of NAME_KEYS) {
    const value = text(row[key]);
    if (value) return value;
  }
  return "—";
}

export function ArchivedContentPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const visible = React.useMemo(
    () => ARCHIVED_CATEGORIES.filter((category) => hasPermissions(effectivePermissions, category.view)),
    [effectivePermissions],
  );
  const [selected, setSelected] = React.useState("");
  const [rows, setRows] = React.useState<Row[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");
  const [restoringId, setRestoringId] = React.useState("");
  const category = visible.find((item) => item.key === selected) ?? visible[0];
  const categoryKey = category?.key ?? "";

  const load = React.useCallback(async () => {
    const current = ARCHIVED_CATEGORIES.find((item) => item.key === categoryKey);
    if (!current) return;
    setLoading(true);
    setError("");
    try {
      const all = await current.load(api);
      setRows(all.filter(current.isArchived ?? ((row) => Boolean(row.archivedAt))));
    } catch (cause) {
      setRows([]);
      setError(cause instanceof Error ? cause.message : "读取已存档内容失败");
    } finally {
      setLoading(false);
    }
  }, [api, categoryKey]);

  React.useEffect(() => { void load(); }, [load]);

  async function restore(row: Row) {
    if (!category) return;
    setRestoringId(row.id);
    try {
      await api.post(category.restorePath(row.id), {});
      setActionResult({ 结果: "已恢复（取消存档）", 分类: category.label, 编号: row.id });
      await load();
    } catch (cause) {
      setGlobalError(cause instanceof Error ? cause.message : "恢复失败");
    } finally {
      setRestoringId("");
    }
  }

  return (
    <section data-testid="archived-content-page">
      <AdminPageHeader title="已存档内容" description="按分类查看全部被存档的对象，可逐条恢复。存档只是先下线：不再被新业务选用，历史照样能查。" />
      {visible.length === 0 ? <p data-testid="archived-content-no-category">当前账号没有可查看的存档分类。</p> : (
        <>
          <div className="admin-form-grid">
            <label>分类
              <select data-testid="archived-content-category" value={categoryKey} onChange={(event) => setSelected(event.target.value)}>
                {visible.map((item) => <option key={item.key} value={item.key}>{item.label}</option>)}
              </select>
            </label>
          </div>
          {error ? <p role="alert" data-testid="archived-content-error">{error}</p> : null}
          {loading ? <p data-testid="archived-content-loading">正在读取…</p> : rows.length === 0 && !error ? (
            <p data-testid="archived-content-empty">「{category?.label}」没有已存档的对象。</p>
          ) : (
            <table className="admin-table" data-testid="archived-content-table">
              <thead><tr><th>名称</th><th>编号</th><th>存档时间</th><th>存档人</th><th>存档理由</th><th>操作</th></tr></thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id} data-testid={`archived-content-row-${row.id}`}>
                    <td>{displayName(row)}</td>
                    <td>{row.id}</td>
                    <td>{cnDate(text(row.archivedAt) || null)}</td>
                    <td>{text(row.archivedBy) || "—"}</td>
                    <td>{text(row.archiveReason) || "—"}</td>
                    <td>
                      <PermissionButton permissionsState={effectivePermissions} permissions={category?.restore ?? []} disabled={restoringId === row.id}
                        onClick={() => void restore(row)} data-testid={`archived-content-restore-${row.id}`}>恢复</PermissionButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}
