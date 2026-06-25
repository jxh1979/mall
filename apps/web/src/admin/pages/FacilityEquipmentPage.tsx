import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type EquipmentRow = {
  id: string; name: string; equipmentType: string; spaceId?: string; vendor?: string;
  status: string; maintenanceCycleDays?: number;
};

const MANAGE = ["facility.equipment.manage"];
const TYPES = ["hvac", "elevator", "fire", "power", "water", "weak_current", "other"];
const STATUSES = ["active", "maintenance", "retired"];

/**
 * 楼宇设施设备台账（ADM-FAC-001，③ 楼宇运营 S1）：楼宇机电设备主数据——维保工单/巡检/能耗的引用地基。
 * 功能标志 facility.equipment.enabled 默认关（关时写被拒）。
 */
export function FacilityEquipmentPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<EquipmentRow[]>([]);
  const [form, setForm] = React.useState({ name: "", equipmentType: "hvac", spaceId: "", vendor: "", cycleDays: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows((await api.get<EquipmentRow[]>("/api/v1/facility/equipment")) ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "设施设备台账读取失败", onStart: () => setGlobalError(null) },
  );

  async function createEquipment() {
    if (!form.name.trim()) {
      setGlobalError("请填写设备名称");
      return;
    }
    const cycle = Number(form.cycleDays);
    await api.post("/api/v1/facility/equipment", {
      name: form.name.trim(),
      equipmentType: form.equipmentType,
      spaceId: form.spaceId.trim() || null,
      vendor: form.vendor.trim() || null,
      maintenanceCycleDays: Number.isFinite(cycle) && cycle > 0 ? cycle : null,
    });
    setActionResult({ action: "facility_equipment_created", name: form.name.trim() });
    setForm({ ...form, name: "", vendor: "" });
    reload();
  }

  async function setStatus(eq: EquipmentRow, status: string) {
    await api.post(`/api/v1/facility/equipment/${eq.id}/status`, { status });
    setActionResult({ action: "facility_equipment_status", equipmentId: eq.id, status });
    reload();
  }

  return (
    <section data-testid="facility-equipment-page">
      <AdminPageHeader
        title="设施设备台账"
        description="楼宇机电设施设备主数据（暖通/电梯/消防/给排水/配电/弱电）。维保工单、设施巡检、能耗计量均引用本台账。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-equipment-table">
            <thead><tr><th>id</th><th>名称</th><th>类型</th><th>位置</th><th>厂商</th><th>维保周期(天)</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`facility-equipment-${row.id}`}>
                  <td>{row.id}</td><td>{row.name}</td><td>{row.equipmentType}</td><td>{row.spaceId ?? "-"}</td>
                  <td>{row.vendor ?? "-"}</td><td>{row.maintenanceCycleDays ?? "-"}</td><td>{row.status}</td>
                  <td>
                    <select aria-label="状态" data-testid={`facility-equipment-status-${row.id}`} value={row.status}
                      onChange={(e) => void setStatus(row, e.target.value)}>
                      {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-equipment-form">
          <input aria-label="设备名称" placeholder="设备名称" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <select aria-label="类型" value={form.equipmentType} onChange={(e) => setForm({ ...form, equipmentType: e.target.value })}>
            {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <input aria-label="位置空间 id" placeholder="位置空间 id" value={form.spaceId} onChange={(e) => setForm({ ...form, spaceId: e.target.value })} />
          <input aria-label="厂商" placeholder="厂商" value={form.vendor} onChange={(e) => setForm({ ...form, vendor: e.target.value })} />
          <input aria-label="维保周期天" type="number" min="1" placeholder="维保周期(天)" value={form.cycleDays} onChange={(e) => setForm({ ...form, cycleDays: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="facility-equipment-create" onClick={() => void createEquipment()}>建档</PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
