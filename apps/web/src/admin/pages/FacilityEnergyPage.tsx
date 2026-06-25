import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type MeterRow = { id: string; name: string; meterType: string; spaceId?: string; unit: string; status: string };
type ReadingRow = { id: string; meterId: string; period: string; readingValue: number; consumption?: number | null };

const MANAGE = ["facility.energy.manage"];
const TYPES = ["electricity", "water", "gas"];

/**
 * 楼宇能耗抄表（ADM-FAC-004，③ 楼宇运营 S4）：电/水/气表台账 + 抄表录入，系统按上期表底算本期用量。
 * 功能标志 facility.energy.enabled 默认关（关时写被拒）。能耗费起账留二期。
 */
export function FacilityEnergyPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [meters, setMeters] = React.useState<MeterRow[]>([]);
  const [readings, setReadings] = React.useState<ReadingRow[]>([]);
  const [meterForm, setMeterForm] = React.useState({ name: "", meterType: "electricity", unit: "", spaceId: "" });
  const [readingForm, setReadingForm] = React.useState({ meterId: "", period: "", readingValue: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      setMeters((await api.get<MeterRow[]>("/api/v1/facility/energy/meters")) ?? []);
      setReadings((await api.get<ReadingRow[]>("/api/v1/facility/energy/readings")) ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "能耗抄表读取失败", onStart: () => setGlobalError(null) },
  );

  async function createMeter() {
    if (!meterForm.name.trim()) {
      setGlobalError("请填写表名称");
      return;
    }
    await api.post("/api/v1/facility/energy/meters", {
      name: meterForm.name.trim(),
      meterType: meterForm.meterType,
      unit: meterForm.unit.trim() || null,
      spaceId: meterForm.spaceId.trim() || null,
    });
    setActionResult({ action: "energy_meter_created", name: meterForm.name.trim() });
    setMeterForm({ ...meterForm, name: "", spaceId: "" });
    reload();
  }

  async function recordReading() {
    if (!readingForm.meterId || !readingForm.period.trim() || !readingForm.readingValue.trim()) {
      setGlobalError("请选择表、填写期次与读数");
      return;
    }
    await api.post(`/api/v1/facility/energy/meters/${readingForm.meterId}/readings`, {
      period: readingForm.period.trim(),
      readingValue: Number(readingForm.readingValue),
    });
    setActionResult({ action: "energy_reading_recorded", meterId: readingForm.meterId, period: readingForm.period.trim() });
    setReadingForm({ ...readingForm, period: "", readingValue: "" });
    reload();
  }

  return (
    <section data-testid="facility-energy-page">
      <AdminPageHeader
        title="楼宇能耗抄表"
        description="电/水/气表台账与抄表录入；系统按上期表底自动算本期用量（本期-上期）。能耗费起账（用量×单价）留二期。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <AdminPageHeader title="能耗表台账" description="" />
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-meter-table">
            <thead><tr><th>id</th><th>名称</th><th>类型</th><th>单位</th><th>位置</th><th>状态</th></tr></thead>
            <tbody>
              {meters.map((m) => (
                <tr key={m.id} data-testid={`energy-meter-${m.id}`}>
                  <td>{m.id}</td><td>{m.name}</td><td>{m.meterType}</td><td>{m.unit}</td><td>{m.spaceId ?? "-"}</td><td>{m.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="energy-meter-form">
          <input aria-label="表名称" placeholder="表名称" value={meterForm.name} onChange={(e) => setMeterForm({ ...meterForm, name: e.target.value })} />
          <select aria-label="表类型" value={meterForm.meterType} onChange={(e) => setMeterForm({ ...meterForm, meterType: e.target.value })}>
            {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <input aria-label="单位" placeholder="单位（默认 kWh）" value={meterForm.unit} onChange={(e) => setMeterForm({ ...meterForm, unit: e.target.value })} />
          <input aria-label="位置空间 id" placeholder="位置空间 id" value={meterForm.spaceId} onChange={(e) => setMeterForm({ ...meterForm, spaceId: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="energy-meter-create" onClick={() => void createMeter()}>建表</PermissionButton>
        </div>

        <AdminPageHeader title="抄表录入" description="选表、填期次（如 2026-06）与表底读数。" />
        <div className="admin-inline-form" data-testid="energy-reading-form">
          <select aria-label="选择表" value={readingForm.meterId} onChange={(e) => setReadingForm({ ...readingForm, meterId: e.target.value })}>
            <option value="">选择能耗表…</option>
            {meters.map((m) => <option key={m.id} value={m.id}>{m.name}（{m.meterType}）</option>)}
          </select>
          <input aria-label="期次" placeholder="期次 2026-06" value={readingForm.period} onChange={(e) => setReadingForm({ ...readingForm, period: e.target.value })} />
          <input aria-label="表底读数" type="number" min="0" step="0.0001" placeholder="表底读数" value={readingForm.readingValue} onChange={(e) => setReadingForm({ ...readingForm, readingValue: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="energy-reading-record" onClick={() => void recordReading()}>录入</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-reading-table">
            <thead><tr><th>id</th><th>表</th><th>期次</th><th>表底读数</th><th>本期用量</th></tr></thead>
            <tbody>
              {readings.map((r) => (
                <tr key={r.id} data-testid={`energy-reading-${r.id}`}>
                  <td>{r.id}</td><td>{r.meterId}</td><td>{r.period}</td><td>{r.readingValue}</td>
                  <td>{r.consumption ?? "首读"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
