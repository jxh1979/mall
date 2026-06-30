import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 空间类型字典行（后端 SpaceTypeRecord，客户端 unwrap 已 camel 化）。
type SpaceTypeRow = {
  id: string;
  typeCode: string;
  typeName: string;
  sortOrder: number;
  status: string;
};

const MANAGE = ["governance.space.manage"];

/**
 * 空间类型字典管理页（ADM-GOV-007，条目3b）：平台管理员维护空间类型（楼栋/楼层/区块/铺位/公共区域…），
 * 作为空间建档 space_type 的下拉数据源与硬校验依据。新增/启用后即可用于空间 space_type 校验。
 */
export function SpaceTypesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SpaceTypeRow[]>([]);
  const [form, setForm] = React.useState({ typeCode: "", typeName: "", sortOrder: "100", status: "active" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<SpaceTypeRow[]>("/api/v1/platform/space-types");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "空间类型字典读取失败" },
  );

  async function saveType() {
    const code = form.typeCode.trim();
    const name = form.typeName.trim();
    if (!code || !name) {
      setGlobalError("请填写类型编码与显示名");
      return;
    }
    await api.post("/api/v1/platform/space-types", {
      idempotencyKey: `space-type-${code}-${form.sortOrder}`,
      typeCode: code,
      typeName: name,
      sortOrder: Number(form.sortOrder) || 100,
      status: form.status,
    });
    setActionResult({ action: "space_type_saved", typeCode: code });
    setForm({ typeCode: "", typeName: "", sortOrder: "100", status: "active" });
    reload();
  }

  return (
    <section data-testid="space-types-page">
      <AdminPageHeader
        title="空间类型字典"
        description="维护空间类型（楼栋/楼层/区块/铺位/公共区域…）。空间建档时 space_type 从本字典下拉选择并硬校验；管理员可新增或停用类型。仅 governance.space.manage 可改。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="space-types-table">
            <thead>
              <tr><th>编码</th><th>名称</th><th>排序</th><th>状态</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.typeCode} data-testid={`space-type-${row.typeCode}`}>
                  <td>{row.typeCode}</td><td>{row.typeName}</td><td>{row.sortOrder}</td><td>{row.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="space-types-form">
          <input aria-label="类型编码" placeholder="类型编码（如 kiosk）" value={form.typeCode}
            onChange={(event) => setForm({ ...form, typeCode: event.target.value })} />
          <input aria-label="显示名" placeholder="显示名（如 一体机位）" value={form.typeName}
            onChange={(event) => setForm({ ...form, typeName: event.target.value })} />
          <input aria-label="排序" type="number" min="0" placeholder="排序" value={form.sortOrder}
            onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} />
          <select aria-label="状态" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
            <option value="active">启用</option>
            <option value="disabled">停用</option>
          </select>
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="space-types-save" onClick={() => void saveType()}>
            保存类型
          </PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
