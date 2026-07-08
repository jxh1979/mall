import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { shortId } from "../format/ids";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { IconButton } from "../components/IconButton";
import { SelectOutlined } from "@ant-design/icons";
import type { AdminPageProps } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { Tabs, Switch } from "antd";

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
// 本页枚举中文兜底（value 不变）：表计类型/归集对象复用上方元组，读数方式单列，
// 能源费分摊口径（合同 4 模式）在全局码表外，页内单列。
const ENERGY_FEE_BASIS_LABELS: Record<string, string> = {
  none: "无能源费", by_contract: "合同包干", by_area: "按面积分摊", by_usage: "按用量分摊",
};
const LABELS: Record<string, string> = {
  ...Object.fromEntries(METER_TYPES), ...Object.fromEntries(GATHER_OBJECTS),
  remote: "远程", manual: "人工", ...ENERGY_FEE_BASIS_LABELS,
};
const zh = (v?: string | null) => (v == null || v === "" ? "-" : (LABELS[v] ?? v));

// 新建仪表的空表单默认值（引导式流程 phase-4：主创建流=新增仪表）。
const EMPTY_METER_FORM = {
  id: "", name: "", meterType: "electricity", parentMeterId: "", gatherObject: "public_area",
  spaceId: "", merchantId: "", projectId: "", readMode: "remote", active: true,
};

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

// 读数状态徽标：颜色照旧，文案零英文（abnormal/recorded/reviewed 已在全局码表）。
function statusBadge(status?: string | null) {
  const value = status || "-";
  const kind = value === "abnormal" ? "is-danger" : value === "reviewed" ? "is-ok" : value === "recorded" ? "is-info" : "";
  return <span className={`admin-badge ${kind}`}>{value === "-" ? "-" : formatBusinessStatus(value)}</span>;
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
  // 行明细抽屉：整行可点 → 该行完整字段（枚举中文·金额¥·时间中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-4：主创建流=新增仪表，收起为主按钮 + 分步引导弹窗（抄表/复核/抹差/结算及既有选中编辑表单不变）。
  const [meterCreateOpen, setMeterCreateOpen] = React.useState(false);
  const [meterCreateError, setMeterCreateError] = React.useState<string | null>(null);

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

  function openMeterDetail(row: EnergyMeterRow) {
    setDetail({
      title: `表计明细 · ${row.name || row.id}`,
      fields: [
        { label: "表计码", value: row.id },
        { label: "名称", value: row.name || "-" },
        { label: "类型", value: zh(row.meterType) },
        { label: "归集对象", value: zh(row.gatherObject) },
        { label: "上级总表", value: row.parentMeterId ?? "-" },
        { label: "空间", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.spaceId}>{row.spaceId ?? "-"}</TraceLink> },
        { label: "商户", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> },
        { label: "项目", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId ?? "-"}</TraceLink> },
        { label: "读数方式", value: zh(row.readMode) },
        { label: "状态", value: row.active === false ? "停用" : "启用" },
      ],
    });
  }

  function openReadingDetail(row: EnergyReadingRow) {
    setDetail({
      title: `抄表明细 · ${row.id}`,
      fields: [
        { label: "读数码", value: row.id },
        { label: "表计", value: row.meterId },
        { label: "期间", value: row.period },
        { label: "本期读数", value: numberText(row.readingValue) },
        { label: "上期读数", value: numberText(row.prevReadingValue) },
        { label: "本期用量", value: numberText(row.consumption) },
        { label: "状态", value: row.status ? formatBusinessStatus(row.status) : "-" },
        { label: "异常原因", value: row.abnormalReason ?? "-" },
        { label: "抄表方式", value: zh(row.readMode) },
        { label: "抄表人", value: row.readerId ?? "-" },
        { label: "抄表时间", value: cnDate(row.readAt) },
        { label: "复核人", value: row.reviewedBy ?? "-" },
        { label: "复核时间", value: cnDate(row.reviewedAt) },
      ],
    });
  }

  function openSettlementDetail(row: EnergySettlementRow) {
    setDetail({
      title: `能源费结算明细 · ${row.id}`,
      fields: [
        { label: "结算码", value: row.id },
        { label: "合同", value: <TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> },
        { label: "商户", value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> },
        { label: "期间", value: row.period },
        { label: "分摊口径", value: zh(row.energyFeeBasis) },
        { label: "金额", value: `¥${yuanFromCent(row.amountCent)}` },
        { label: "应收单", value: row.arApItemId ?? "-" },
        { label: "状态", value: formatBusinessStatus(row.status) },
      ],
    });
  }

  async function saveMeter() {
    const creating = !meterForm.id;
    if (creating) setMeterCreateError(null);
    if (!meterForm.name.trim()) {
      const message = "表计名称不能为空";
      if (creating) setMeterCreateError(message); else setGlobalError(message);
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
    try {
      const saved = meterForm.id
        ? await api.post<EnergyMeterRow>(`/api/v1/energy/meters/${encodeURIComponent(meterForm.id)}`, body)
        : await api.post<EnergyMeterRow>("/api/v1/energy/meters", body);
      setActionResult({ action: "energy_meter_saved", meterId: saved.id });
      setMeterForm({ ...meterForm, id: "", name: "" });
      if (creating) setMeterCreateOpen(false);
      await reload();
    } catch (e) {
      const message = e instanceof Error ? e.message : "表计保存失败";
      if (creating) setMeterCreateError(message); else setGlobalError(message);
    }
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

        <div className="admin-preview" data-testid="energy-workbench-guide">
          <strong>这一页做什么：</strong>能源管理三块——① 表计台账（4 类 / 总分表建档）② 抄表与抹差（录抄表、复核异常、总分表抹差）③ 能源费结算。先在上方「按期读取」选期间，再切标签操作。
        </div>
        <Tabs
          data-testid="energy-workbench-tabs"
          items={[
            { key: "meters", label: "① 表计台账", forceRender: true, children: (<>
        <h3>表计台账（4 类 / 总分表）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-meters-table">
            <thead><tr><th>表计码</th><th>名称</th><th>类型</th><th>归集对象</th><th>上级总表</th><th>空间/商户/项目</th><th>读数方式</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>{meters.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`energy-meter-${row.id}`}
                onClick={() => openMeterDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMeterDetail(row); } }}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.name}</td><td>{zh(row.meterType)}</td><td>{zh(row.gatherObject)}</td><td className="admin-id-cell" title={row.parentMeterId ?? ""}>{row.parentMeterId ? shortId(row.parentMeterId) : "-"}</td>
                <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-007" objectId={row.spaceId}>{row.spaceId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectId ?? "-"}</TraceLink></td><td>{zh(row.readMode)}</td><td>{row.active === false ? "停用" : "启用"}</td>
                <td onClick={(event) => event.stopPropagation()}><IconButton icon={<SelectOutlined />} tip="选中该表计填入表单" onClick={() => selectMeter(row)} testId={`energy-meter-select-${row.id}`} /></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        {/* 引导式流程 phase-4：新增仪表入口按钮（权限门在入口），点击弹出分步引导。下方内联表单保留用于选中后编辑既有仪表。 */}
        <div className="admin-form-row" data-testid="energy-meter-create-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={METER} data-testid="energy-meter-create-open" onClick={() => { setMeterCreateError(null); setMeterForm({ ...EMPTY_METER_FORM }); setMeterCreateOpen(true); }}>＋ 新增仪表</PermissionButton>
        </div>
        <GuidedFlowModal
          open={meterCreateOpen}
          title="新增仪表"
          testId="energy-meter-guided"
          submitTestId="energy-meter-save"
          submitLabel="保存表计"
          error={meterCreateError ?? undefined}
          onClose={() => setMeterCreateOpen(false)}
          onSubmit={() => void saveMeter()}
          steps={[
            {
              title: "表计基本信息",
              description: "填写表计名称、类型与归集对象。",
              validate: () => (meterForm.name.trim() ? null : "表计名称不能为空"),
              content: (
                <>
                  <label><span>表计名称</span><input data-testid="energy-meter-guided-name" value={meterForm.name} onChange={(event) => setMeterForm({ ...meterForm, name: event.target.value })} /></label>
                  <label><span>表计类型</span><select data-testid="energy-meter-guided-type" value={meterForm.meterType} onChange={(event) => setMeterForm({ ...meterForm, meterType: event.target.value })}>{METER_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <label><span>归集对象</span><select data-testid="energy-meter-guided-gather" value={meterForm.gatherObject} onChange={(event) => setMeterForm({ ...meterForm, gatherObject: event.target.value })}>{GATHER_OBJECTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                </>
              ),
            },
            {
              title: "归属与读数方式",
              description: "可选填上级总表、空间/商户/项目归属，选择读数方式与启用状态。",
              content: (
                <>
                  <label><span>上级总表 id（可空）</span><input data-testid="energy-meter-guided-parent" value={meterForm.parentMeterId} onChange={(event) => setMeterForm({ ...meterForm, parentMeterId: event.target.value })} /></label>
                  <label><span>空间 id（可空）</span><input data-testid="energy-meter-guided-space" value={meterForm.spaceId} onChange={(event) => setMeterForm({ ...meterForm, spaceId: event.target.value })} /></label>
                  <label><span>商户 id（可空）</span><input data-testid="energy-meter-guided-merchant" value={meterForm.merchantId} onChange={(event) => setMeterForm({ ...meterForm, merchantId: event.target.value })} /></label>
                  <label><span>项目 id（可空）</span><input data-testid="energy-meter-guided-project" value={meterForm.projectId} onChange={(event) => setMeterForm({ ...meterForm, projectId: event.target.value })} /></label>
                  <label><span>读数方式</span><select data-testid="energy-meter-guided-readmode" value={meterForm.readMode} onChange={(event) => setMeterForm({ ...meterForm, readMode: event.target.value })}><option value="remote">远程</option><option value="manual">人工</option></select></label>
                  <label className="admin-inline-check"><Switch size="default" data-testid="energy-meter-guided-active" checked={meterForm.active} onChange={(checked) => setMeterForm({ ...meterForm, active: checked })} /><span>启用</span></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "表计名称", value: meterForm.name.trim() || "未填写" },
            { label: "表计类型", value: zh(meterForm.meterType) },
            { label: "归集对象", value: zh(meterForm.gatherObject) },
            { label: "上级总表", value: meterForm.parentMeterId.trim() || "无" },
            { label: "读数方式", value: zh(meterForm.readMode) },
            { label: "状态", value: meterForm.active ? "启用" : "停用" },
          ]}
        />
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
          <label className="admin-inline-check"><Switch size="default" checked={meterForm.active} onChange={(checked) => setMeterForm({ ...meterForm, active: checked })} /><span>启用</span></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={METER} data-testid="energy-meter-update" onClick={() => void saveMeter()}>保存表计</PermissionButton>
        </div>

          </>) },
          { key: "readings", label: "② 抄表与抹差", forceRender: true, children: (<>
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
            <thead><tr><th>读数码</th><th>表计</th><th>期间</th><th>读数/上期/用量</th><th>状态</th><th>异常</th><th>抄表/复核</th><th>操作</th></tr></thead>
            <tbody>{readings.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`energy-reading-${row.id}`}
                onClick={() => openReadingDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openReadingDetail(row); } }}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td className="admin-id-cell" title={row.meterId}>{shortId(row.meterId)}</td><td>{row.period}</td>
                <td>{numberText(row.readingValue)} / {numberText(row.prevReadingValue)} / {numberText(row.consumption)}</td>
                <td>{statusBadge(row.status)}</td><td>{row.abnormalReason ?? "-"}</td>
                <td>{row.readerId ?? "-"} / {row.reviewedBy ?? "-"}</td>
                <td onClick={(event) => event.stopPropagation()}>{row.status === "abnormal" ? <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid={`energy-reading-review-${row.id}`} onClick={() => void reviewReading(row)}>复核</PermissionButton> : null}</td>
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

          </>) },
          { key: "settlement", label: "③ 能源费结算", forceRender: true, children: (<>
        <h3>能源费结算</h3>
        <div className="admin-inline-form" data-testid="energy-settlement-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={READING} data-testid="energy-fee-settle" onClick={() => void settleEnergyFee()}>结算本期能源费</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="energy-settlements-table">
            <thead><tr><th>结算码</th><th>合同/商户</th><th>期间</th><th>分摊口径</th><th>金额(元)</th><th>应收</th><th>状态</th></tr></thead>
            <tbody>{settlements.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`energy-settlement-${row.id}`}
                onClick={() => openSettlementDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSettlementDetail(row); } }}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td>
                <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-CONT-001" objectId={row.contractId}>{row.contractId ?? "-"}</TraceLink> / <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink></td>
                <td>{row.period}</td><td>{zh(row.energyFeeBasis)}</td><td>{yuanFromCent(row.amountCent)}</td><td>{row.arApItemId ?? "-"}</td><td>{formatBusinessStatus(row.status)}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
          </>) },
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
