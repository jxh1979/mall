import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";

type EnergyMeterRow = {
  id: string; name: string; meterType: string; parentMeterId?: string | null; gatherObject: string;
  spaceId?: string | null; merchantId?: string | null; projectId?: string | null; readMode?: string | null; active?: boolean;
};
type EnergyReadingRow = {
  id: string; meterId: string; period: string; readingValue: number | string; prevReadingValue?: number | string | null;
  consumption?: number | string | null; status: string; abnormalReason?: string | null; readMode?: string | null;
  readerId?: string | null; readAt?: string | null; reviewedBy?: string | null; reviewedAt?: string | null;
};
type EnergyDifferenceRow = {
  parentMeterId: string; period: string; parentConsumption?: number | string | null; childConsumption?: number | string | null; difference?: number | string | null;
};
type EnergySettlementRow = {
  id: string; contractId?: string | null; merchantId?: string | null; projectId?: string | null; period: string;
  energyFeeBasis?: string | null; amountCent: number; status?: string | null; arApItemId?: string | null;
};

const METER = ["energy.meter.manage"];
const READING = ["energy.reading.manage"];
const METER_TYPES = [
  ["electricity", "电"], ["water", "水"], ["gas", "燃气"], ["heat_cold", "冷热量"],
];
const GATHER_OBJECTS = [
  ["public_area", "公区"], ["merchant", "商户"], ["equipment_circuit", "设备回路"],
];
// 本页枚举中文兜底（value 不变）：表计类型/归集对象复用上方元组，读数方式单列。
const LABELS: Record<string, string> = {
  ...Object.fromEntries(METER_TYPES), ...Object.fromEntries(GATHER_OBJECTS),
  remote: "远程", manual: "人工",
};
const zh = (v?: string | null) => (v == null || v === "" ? "-" : (LABELS[v] ?? v));

function numberText(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  const parsed = Number(value);
  return Number.isFinite(parsed) ? String(parsed) : String(value);
}

function toIso(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

function yuanFromCent(value?: number | null) {
  if (value === null || value === undefined) return "-";
  return (value / 100).toFixed(2);
}

function statusBadge(status?: string | null) {
  const value = status || "-";
  const kind = value === "abnormal" ? "is-danger" : value === "reviewed" ? "is-ok" : value === "recorded" ? "is-info" : "";
  return <span className={`admin-badge ${kind}`}>{value}</span>;
}

export function EnergyWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [meters, setMeters] = React.useState<EnergyMeterRow[]>([]);
  const [readings, setReadings] = React.useState<EnergyReadingRow[]>([]);
  const [difference, setDifference] = React.useState<EnergyDifferenceRow | null>(null);
  const [settlements, setSettlements] = React.useState<EnergySettlementRow[]>([]);
  const [period, setPeriod] = React.useState("2026-06");
  const [meterForm, setMeterForm] = React.useState({
    id: "", name: "", meterType: "electricity", parentMeterId: "", gatherObject: "public_area",
    spaceId: "", merchantId: "", projectId: "", readMode: "remote", active: true,
  });
  const [readingForm, setReadingForm] = React.useState({ meterId: "", readingValue: "", readMode: "manual", readerId: "", readAt: "" });
  const [reviewedBy, setReviewedBy] = React.useState("");
  const [differenceMeterId, setDifferenceMeterId] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const query = period.trim() ? `?period=${encodeURIComponent(period.trim())}` : "";
      const [meterRows, readingRows] = await Promise.all([
        api.get<EnergyMeterRow[]>("/api/v1/energy/meters"),
        api.get<EnergyReadingRow[]>(`/api/v1/energy/readings${query}`),
      ]);
      setMeters(meterRows ?? []);
      setReadings(readingRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "能源抄表数据读取失败", onStart: () => setGlobalError(null) },
  );

  function selectMeter(row: EnergyMeterRow) {
    setMeterForm({
      id: row.id, name: row.name ?? "", meterType: row.meterType ?? "electricity",
      parentMeterId: row.parentMeterId ?? "", gatherObject: row.gatherObject ?? "public_area",
      spaceId: row.spaceId ?? "", merchantId: row.merchantId ?? "", projectId: row.projectId ?? "",
      readMode: row.readMode ?? "remote", active: row.active ?? true,
    });
    setReadingForm((current) => ({ ...current, meterId: row.id }));
    setDifferenceMeterId(row.id);
  }

  async function saveMeter() {
    if (!meterForm.name.trim()) {
      setGlobalError("表计名称不能为空");
      return;
    }
    const body = {
      name: meterForm.name.trim(),
      meterType: meterForm.meterType,
      parentMeterId: meterForm.parentMeterId.trim() || undefined,
      gatherObject: meterForm.gatherObject,
      spaceId: meterForm.spaceId.trim() || undefined,
      merchantId: meterForm.merchantId.trim() || undefined,
      projectId: meterForm.projectId.trim() || undefined,
      readMode: meterForm.readMode,
      active: meterForm.active,
    };
    const saved = meterForm.id
      ? await api.post<EnergyMeterRow>(`/api/v1/energy/meters/${encodeURIComponent(meterForm.id)}`, body)
      : await api.post<EnergyMeterRow>("/api/v1/energy/meters", body);
    setActionResult({ action: "energy_meter_saved", meterId: saved.id });
    setMeterForm({ ...meterForm, id: "", name: "" });
    await reload();
  }

  async function recordReading() {
    const readingValue = Number(readingForm.readingValue);
    if (!readingForm.meterId.trim() || !period.trim() || !Number.isFinite(readingValue)) {
      setGlobalError("抄表需填写表计 id、期间和有效读数");
      return;
    }
    const saved = await api.post<EnergyReadingRow>("/api/v1/energy/readings", {
      meterId: readingForm.meterId.trim(),
      period: period.trim(),
      readingValue,
      readMode: readingForm.readMode,
      readerId: readingForm.readerId.trim() || undefined,
      readAt: toIso(readingForm.readAt),
    });
    setActionResult({ action: "energy_reading_recorded", readingId: saved.id, status: saved.status });
    setReadingForm({ ...readingForm, readingValue: "" });
    await reload();
  }

  async function reviewReading(row: EnergyReadingRow) {
    if (!reviewedBy.trim()) {
      setGlobalError("复核异常读数需填写复核人 id");
      return;
    }
    await api.post(`/api/v1/energy/readings/${encodeURIComponent(row.id)}/review`, { reviewedBy: reviewedBy.trim() });
    setActionResult({ action: "energy_reading_reviewed", readingId: row.id });
    await reload();
  }

  async function loadDifference() {
    if (!differenceMeterId.trim() || !period.trim()) {
      setGlobalError("总分表抹差需填写总表 id 与期间");
      return;
    }
    setDifference(await api.get<EnergyDifferenceRow>(`/api/v1/energy/meters/${encodeURIComponent(differenceMeterId.trim())}/difference?period=${encodeURIComponent(period.trim())}`));
  }

  async function settleEnergyFee() {
    if (!period.trim()) {
      setGlobalError("能源费结算需填写期间");
      return;
    }
    const rows = await api.post<EnergySettlementRow[]>("/api/v1/energy/settle-energy-fee", { period: period.trim() });
    setSettlements(rows ?? []);
    setActionResult({ action: "energy_fee_settled", period: period.trim(), count: rows?.length ?? 0 });
  }

  return (
    <section data-testid="energy-workbench-page">
      <AdminPageHeader title="能源工作台" description="表计台账、抄表异常复核、总分表抹差和能源费结算；请求体沿用后台 snake_case 自动转换。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-inline-form" data-testid="energy-period-form">
          <input aria-label="抄表期间" placeholder="期间 YYYY-MM" value={period} onChange={(event) => setPeriod(event.target.value)} />
          <button type="button" data-testid="energy-period-load" onClick={() => void reload()}>按期读取</button>
        </div>

        <h3>表计台账（4 类 / 总分表）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-meters-table">
            <thead><tr><th>id</th><th>名称</th><th>类型</th><th>归集对象</th><th>上级总表</th><th>空间/商户/项目</th><th>读数方式</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>{meters.map((row) => (
              <tr key={row.id} data-testid={`energy-meter-${row.id}`}>
                <td>{row.id}</td><td>{row.name}</td><td>{zh(row.meterType)}</td><td>{zh(row.gatherObject)}</td><td>{row.parentMeterId ?? "-"}</td>
                <td><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.spaceId}>{row.spaceId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId ?? "-"}</TraceLink></td><td>{zh(row.readMode)}</td><td>{row.active === false ? "停用" : "启用"}</td>
                <td><button type="button" data-testid={`energy-meter-select-${row.id}`} onClick={() => selectMeter(row)}>选中</button></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="energy-meter-form">
          <input aria-label="表计 id" placeholder="表计 id(选中后更新)" value={meterForm.id} onChange={(event) => setMeterForm({ ...meterForm, id: event.target.value })} />
          <input aria-label="表计名称" placeholder="表计名称" value={meterForm.name} onChange={(event) => setMeterForm({ ...meterForm, name: event.target.value })} />
          <select aria-label="表计类型" value={meterForm.meterType} onChange={(event) => setMeterForm({ ...meterForm, meterType: event.target.value })}>{METER_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input aria-label="上级总表 id" placeholder="上级总表 id" value={meterForm.parentMeterId} onChange={(event) => setMeterForm({ ...meterForm, parentMeterId: event.target.value })} />
          <select aria-label="归集对象" value={meterForm.gatherObject} onChange={(event) => setMeterForm({ ...meterForm, gatherObject: event.target.value })}>{GATHER_OBJECTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input aria-label="空间 id" placeholder="空间 id" value={meterForm.spaceId} onChange={(event) => setMeterForm({ ...meterForm, spaceId: event.target.value })} />
          <input aria-label="商户 id" placeholder="商户 id" value={meterForm.merchantId} onChange={(event) => setMeterForm({ ...meterForm, merchantId: event.target.value })} />
          <input aria-label="项目 id" placeholder="项目 id" value={meterForm.projectId} onChange={(event) => setMeterForm({ ...meterForm, projectId: event.target.value })} />
          <select aria-label="读数方式" value={meterForm.readMode} onChange={(event) => setMeterForm({ ...meterForm, readMode: event.target.value })}><option value="remote">远程</option><option value="manual">人工</option></select>
          <label><input type="checkbox" checked={meterForm.active} onChange={(event) => setMeterForm({ ...meterForm, active: event.target.checked })} />启用</label>
          <PermissionButton permissionsState={effectivePermissions} permissions={METER} data-testid="energy-meter-save" onClick={() => void saveMeter()}>保存表计</PermissionButton>
        </div>

        <h3>抄表与异常复核</h3>
        <div className="admin-inline-form" data-testid="energy-reading-form">
          <input aria-label="读数表计 id" placeholder="表计 id" value={readingForm.meterId} onChange={(event) => setReadingForm({ ...readingForm, meterId: event.target.value })} />
          <input aria-label="本期读数" type="number" placeholder="本期读数" value={readingForm.readingValue} onChange={(event) => setReadingForm({ ...readingForm, readingValue: event.target.value })} />
          <select aria-label="抄表方式" value={readingForm.readMode} onChange={(event) => setReadingForm({ ...readingForm, readMode: event.target.value })}><option value="manual">人工</option><option value="remote">远程</option></select>
          <input aria-label="抄表人" placeholder="抄表人 id" value={readingForm.readerId} onChange={(event) => setReadingForm({ ...readingForm, readerId: event.target.value })} />
          <input aria-label="抄表时间" type="datetime-local" value={readingForm.readAt} onChange={(event) => setReadingForm({ ...readingForm, readAt: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid="energy-reading-record" onClick={() => void recordReading()}>录入读数</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="energy-reading-review-form">
          <input aria-label="复核人" placeholder="复核人 id" value={reviewedBy} onChange={(event) => setReviewedBy(event.target.value)} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-readings-table">
            <thead><tr><th>id</th><th>表计</th><th>期间</th><th>读数/上期/用量</th><th>状态</th><th>异常</th><th>抄表/复核</th><th>操作</th></tr></thead>
            <tbody>{readings.map((row) => (
              <tr key={row.id} data-testid={`energy-reading-${row.id}`}>
                <td>{row.id}</td><td>{row.meterId}</td><td>{row.period}</td>
                <td>{numberText(row.readingValue)} / {numberText(row.prevReadingValue)} / {numberText(row.consumption)}</td>
                <td>{statusBadge(row.status)}</td><td>{row.abnormalReason ?? "-"}</td>
                <td>{row.readerId ?? "-"} / {row.reviewedBy ?? "-"}</td>
                <td>{row.status === "abnormal" ? <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid={`energy-reading-review-${row.id}`} onClick={() => void reviewReading(row)}>复核</PermissionButton> : null}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>

        <h3>总分表抹差</h3>
        <div className="admin-inline-form" data-testid="energy-difference-form">
          <input aria-label="总表 id" placeholder="总表 id" value={differenceMeterId} onChange={(event) => setDifferenceMeterId(event.target.value)} />
          <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid="energy-difference-load" onClick={() => void loadDifference()}>计算抹差</PermissionButton>
        </div>
        {difference ? (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="energy-difference-table">
              <thead><tr><th>总表</th><th>期间</th><th>总表用量</th><th>分表合计</th><th>差额</th></tr></thead>
              <tbody><tr><td>{difference.parentMeterId}</td><td>{difference.period}</td><td>{numberText(difference.parentConsumption)}</td><td>{numberText(difference.childConsumption)}</td><td>{numberText(difference.difference)}</td></tr></tbody>
            </table>
          </div>
        ) : null}

        <h3>能源费结算</h3>
        <div className="admin-inline-form" data-testid="energy-settlement-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid="energy-fee-settle" onClick={() => void settleEnergyFee()}>settle-energy-fee</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-settlements-table">
            <thead><tr><th>结算</th><th>合同/商户</th><th>期间</th><th>分摊口径</th><th>金额(元)</th><th>应收</th><th>状态</th></tr></thead>
            <tbody>{settlements.map((row) => <tr key={row.id} data-testid={`energy-settlement-${row.id}`}><td>{row.id}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink></td><td>{row.period}</td><td>{row.energyFeeBasis ?? "-"}</td><td>{yuanFromCent(row.amountCent)}</td><td>{row.arApItemId ?? "-"}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
