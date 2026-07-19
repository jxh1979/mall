import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { loadSecondary, loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { CnDatePicker } from "../components/CnDatePicker";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { TraceLink } from "../components/TraceLink";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import type {
  AdminPageProps,
  AmusementProjectRow,
  AmusementSessionRow,
  AmusementVerificationRow,
  AmusementCapacitySnapshotRow,
} from "../types";

// 游乐核销记录（阶段3）：入场/出场记录 + 在场容量看板 + 交叉对账，三视图一页。
// 铁律：零英文全中文（方向/结果中文 label）、整行可点开明细、时间 cnDate、日期筛选 CnDatePicker、数字可点下钻。

const TEXT = {
  title: "游乐核销记录",
  description: "游乐入场/出场核销记录、在场容量看板与通票交叉对账，全部只读。",
  evidence:
    "接口证据：读取 /api/v1/amusement/verifications（支持 projectId/sessionId/direction/dateFrom/dateTo 筛选，含关联通票号）、/api/v1/amusement/capacity-snapshots、/api/v1/amusement/projects、/api/v1/amusement/sessions。",
  loadFailed: "游乐核销记录读取失败",
};

// 方向枚举中文（direction=entry/exit）。注意：不可复用 formatBusinessStatus——
// 全局码表里 entry 已被票务权益类型占用为“入园”，此处语义是核销方向“入场”，故页级独立 label。
const DIRECTION_LABEL: Record<string, string> = { entry: "入场", exit: "出场" };
function directionLabel(value?: string): string {
  if (!value) return "—";
  return DIRECTION_LABEL[value] ?? value;
}

// 容量维度中文（dimension）。point_device=自由进出型（并发在场/上限），project/session=场次批次型（已用/容量）。
const DIMENSION_LABEL: Record<string, string> = {
  point_device: "自由进出（并发在场）",
  project: "项目容量",
  session: "场次容量",
};
function dimensionLabel(value?: string): string {
  if (!value) return "—";
  return DIMENSION_LABEL[value] ?? value;
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "number" || typeof value === "string") return String(value);
  return JSON.stringify(value);
}

function num(value: unknown): number {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n : 0;
}

type TabKey = "records" | "capacity" | "reconcile";

export function AmusementVerificationRecordsPage({ api, setGlobalError, navigateTo, effectivePermissions }: AdminPageProps) {
  const [records, setRecords] = React.useState<AmusementVerificationRow[]>([]);
  const [snapshots, setSnapshots] = React.useState<AmusementCapacitySnapshotRow[]>([]);
  const [projects, setProjects] = React.useState<AmusementProjectRow[]>([]);
  const [sessions, setSessions] = React.useState<AmusementSessionRow[]>([]);
  // 容量快照/项目/场次仅供筛选下拉、显名与超容小卡，属副资源，任一失败不拖垮核销记录主表。
  const [auxNotice, setAuxNotice] = React.useState<string | null>(null);

  const [tab, setTab] = React.useState<TabKey>("records");
  // 筛选：项目/场次/方向/日期起止。空=不加谓词（后端同款语义）。
  const [projectId, setProjectId] = React.useState("");
  const [sessionId, setSessionId] = React.useState("");
  const [direction, setDirection] = React.useState("");
  const [dateFrom, setDateFrom] = React.useState("");
  const [dateTo, setDateTo] = React.useState("");
  // 交叉对账筛选：全部 / 仅有通票号 / 仅无通票号。
  const [reconcile, setReconcile] = React.useState<"all" | "linked" | "unlinked">("all");

  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const query = React.useMemo(() => {
    const params = new URLSearchParams();
    if (projectId) params.set("projectId", projectId);
    if (sessionId) params.set("sessionId", sessionId);
    if (direction) params.set("direction", direction);
    if (dateFrom) params.set("dateFrom", dateFrom);
    if (dateTo) params.set("dateTo", dateTo);
    const qs = params.toString();
    return qs ? `?${qs}` : "";
  }, [projectId, sessionId, direction, dateFrom, dateTo]);

  // 无权限副资源不请求（根因修）：项目/场次端点需 amusement.session.view，页面基础权限 amusement.verification.view 不含之。
  const canAmusementSession = hasPermissions(effectivePermissions, ["amusement.session.view"]);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // 副资源容错（外包页范式）：容量快照/项目/场次为筛选与显名辅助数据，可能因权限/500 单独失败——
      // 各自降级为空并合并一条区块提示，不拖垮核销记录主表。
      const [nextRecords, snapRes, projRes, sessRes] = await Promise.all([
        api.get<AmusementVerificationRow[]>(`/api/v1/amusement/verifications${query}`),
        loadSecondary(() => api.get<AmusementCapacitySnapshotRow[]>("/api/v1/amusement/capacity-snapshots"), [] as AmusementCapacitySnapshotRow[]),
        loadSecondaryIfPermitted(canAmusementSession, () => api.get<AmusementProjectRow[]>("/api/v1/amusement/projects"), [] as AmusementProjectRow[]),
        loadSecondaryIfPermitted(canAmusementSession, () => api.get<AmusementSessionRow[]>("/api/v1/amusement/sessions"), [] as AmusementSessionRow[]),
      ]);
      setRecords(nextRecords);
      setSnapshots(snapRes.data);
      setProjects(projRes.data);
      setSessions(sessRes.data);
      setAuxNotice(snapRes.notice ?? projRes.notice ?? sessRes.notice);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  // 筛选变化即重查（后端按 query 过滤）。reload 身份稳定、经 ref 读最新 fetcher 闭包，安全依赖。
  React.useEffect(() => {
    void reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const projectNameById = React.useMemo(() => {
    const map = new Map<string, string>();
    for (const p of projects) map.set(p.id, String(p.projectName ?? p.id));
    return map;
  }, [projects]);
  const sessionNameById = React.useMemo(() => {
    const map = new Map<string, string>();
    for (const s of sessions) map.set(s.id, String(s.sessionName ?? s.id));
    return map;
  }, [sessions]);

  const reconciledRecords = React.useMemo(() => {
    if (reconcile === "linked") return records.filter((r) => Boolean(r.linkedTicketNo));
    if (reconcile === "unlinked") return records.filter((r) => !r.linkedTicketNo);
    return records;
  }, [records, reconcile]);

  const shownRecords = tab === "reconcile" ? reconciledRecords : records;
  const paged = usePagedRows(shownRecords, 12);

  function openRecordDetail(row: AmusementVerificationRow) {
    setDetail({
      title: `核销明细 · ${directionLabel(row.direction)} · ${row.id}`,
      fields: [
        { label: "方向", value: directionLabel(row.direction) },
        { label: "结果", value: formatBusinessStatus(row.verificationResult) },
        { label: "核销时间", value: cnDate(row.createdAt) },
        { label: "释放时间", value: row.releasedAt ? cnDate(row.releasedAt) : "仍在场 / 无" },
        {
          label: "项目",
          value: (
            <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>
              {display(projectNameById.get(String(row.projectId ?? "")) ?? row.projectId)}
            </TraceLink>
          ),
        },
        { label: "场次", value: display(sessionNameById.get(String(row.sessionId ?? "")) ?? row.sessionId) },
        { label: "核销人数", value: display(row.verifiedPeople) },
        { label: "设备", value: display(row.deviceId) },
        { label: "核销点位", value: display(row.locationPointId) },
        {
          label: "关联通票号",
          value: row.linkedTicketNo ? (
            <TraceLink navigateTo={navigateTo} pageId="ADM-TKT-003" objectId={row.linkedTicketNo}>
              {row.linkedTicketNo}
            </TraceLink>
          ) : (
            "无对应通票核销"
          ),
        },
        { label: "入场记录", value: display(row.entryVerificationId) },
        { label: "权益实例", value: display(row.entitlementId) },
        { label: "核销编号", value: row.id },
      ],
    });
  }

  function openSnapshotDetail(row: AmusementCapacitySnapshotRow) {
    const rate = num(row.capacityLimit) > 0 ? Math.round((num(row.occupiedCount) / num(row.capacityLimit)) * 100) : 0;
    setDetail({
      title: `容量明细 · ${dimensionLabel(row.dimension)}`,
      fields: [
        { label: "维度", value: dimensionLabel(row.dimension) },
        {
          label: "项目",
          value: (
            <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>
              {display(projectNameById.get(String(row.projectId ?? "")) ?? row.projectId)}
            </TraceLink>
          ),
        },
        { label: "场次", value: display(row.sessionId ? (sessionNameById.get(String(row.sessionId)) ?? row.sessionId) : "—") },
        { label: "核销点位", value: display(row.locationPointId) },
        { label: "设备", value: display(row.deviceId) },
        { label: "当前在场 / 已用", value: display(row.occupiedCount) },
        { label: "上限", value: display(row.capacityLimit) },
        { label: "占用率", value: `${rate}%` },
        { label: "更新时间", value: cnDate(row.updatedAt) },
      ],
    });
  }

  const entryCount = records.filter((r) => (r.direction ?? "entry") === "entry").length;
  const exitCount = records.filter((r) => r.direction === "exit").length;
  const linkedCount = records.filter((r) => Boolean(r.linkedTicketNo)).length;
  const overCapacityCount = snapshots.filter((s) => num(s.occupiedCount) > num(s.capacityLimit)).length;
  // KPI 改走统一明细端点（数字=明细合计同源）。统一明细为平台全量口径，仅在页面无筛选（query 为空）时口径对齐——
  // 有筛选时禁用统一取数、回退本地筛选后计数并隐藏「完整明细」入口（不误把全量当筛选值）；失败/无权限同样回退。
  const unfiltered = query === "";
  const entryKpi = useMetricKpiValue(api, "amusement.entry_count", { enabled: unfiltered });
  const exitKpi = useMetricKpiValue(api, "amusement.exit_count", { enabled: unfiltered });
  const entryDisplay = entryKpi.status === "ready" && entryKpi.value !== null ? entryKpi.value : entryCount;
  const exitDisplay = exitKpi.status === "ready" && exitKpi.value !== null ? exitKpi.value : exitCount;

  const filterBar = (
    <div className="admin-form-grid" data-testid="amu-verif-filters">
      <label>
        项目
        <select data-testid="amu-verif-filter-project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
          <option value="">全部项目</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>{p.projectName ?? p.id}</option>
          ))}
        </select>
      </label>
      <label>
        场次
        <select data-testid="amu-verif-filter-session" value={sessionId} onChange={(e) => setSessionId(e.target.value)}>
          <option value="">全部场次</option>
          {sessions.map((s) => (
            <option key={s.id} value={s.id}>{s.sessionName ?? s.id}</option>
          ))}
        </select>
      </label>
      <label>
        方向
        <select data-testid="amu-verif-filter-direction" value={direction} onChange={(e) => setDirection(e.target.value)}>
          <option value="">全部方向</option>
          <option value="entry">入场</option>
          <option value="exit">出场</option>
        </select>
      </label>
      <label>
        起始日期
        <CnDatePicker data-testid="amu-verif-filter-date-from" value={dateFrom} onChange={setDateFrom} placeholder="不限" />
      </label>
      <label>
        截止日期
        <CnDatePicker data-testid="amu-verif-filter-date-to" value={dateTo} onChange={setDateTo} placeholder="不限" />
      </label>
    </div>
  );

  const recordsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="amu-verif-records-table">
        <thead>
          <tr><th>方向</th><th>核销时间</th><th>项目</th><th>场次</th><th>结果</th><th>人数</th><th>关联通票号</th><th>详情</th></tr>
        </thead>
        <tbody>
          {paged.pageRows.length === 0 ? (
            <tr data-testid="amu-verif-records-empty"><td colSpan={8}>暂无核销记录</td></tr>
          ) : (
            paged.pageRows.map((row) => (
              <tr
                key={row.id}
                className="admin-row-clickable"
                tabIndex={0}
                data-testid={`amu-verif-row-${row.id}`}
                onClick={() => openRecordDetail(row)}
                onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openRecordDetail(row); } }}
              >
                <td>{directionLabel(row.direction)}</td>
                <td>{cnDate(row.createdAt)}</td>
                <td>{display(projectNameById.get(String(row.projectId ?? "")) ?? row.projectId)}</td>
                <td>{display(sessionNameById.get(String(row.sessionId ?? "")) ?? row.sessionId)}</td>
                <td>{formatBusinessStatus(row.verificationResult)}</td>
                <td>{display(row.verifiedPeople)}</td>
                <td>{row.linkedTicketNo ?? "—"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看核销详情" onClick={(event) => { event.stopPropagation(); openRecordDetail(row); }}>查看详情</button></td></tr>
            ))
          )}
        </tbody>
      </table>
      <TablePager testid="amu-verif-records-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
    </div>
  );

  const capacityTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="amu-verif-capacity-table">
        <thead>
          <tr><th>维度</th><th>项目</th><th>场次</th><th>点位/设备</th><th>在场/已用</th><th>上限</th><th>占用率</th><th>详情</th></tr>
        </thead>
        <tbody>
          {snapshots.length === 0 ? (
            <tr data-testid="amu-verif-capacity-empty"><td colSpan={8}>暂无在场容量快照</td></tr>
          ) : (
            snapshots.map((row) => {
              const over = num(row.occupiedCount) > num(row.capacityLimit);
              const rate = num(row.capacityLimit) > 0 ? Math.round((num(row.occupiedCount) / num(row.capacityLimit)) * 100) : 0;
              return (
                <tr
                  key={row.id}
                  className={`admin-row-clickable${over ? " admin-row-danger" : ""}`}
                  tabIndex={0}
                  data-testid={`amu-capacity-row-${row.id}`}
                  onClick={() => openSnapshotDetail(row)}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); openSnapshotDetail(row); } }}
                >
                  <td>{dimensionLabel(row.dimension)}</td>
                  <td>{display(projectNameById.get(String(row.projectId ?? "")) ?? row.projectId)}</td>
                  <td>{display(row.sessionId ? (sessionNameById.get(String(row.sessionId)) ?? row.sessionId) : "—")}</td>
                  <td>{display(row.locationPointId ?? row.deviceId)}</td>
                  <td>{display(row.occupiedCount)}</td>
                  <td>{display(row.capacityLimit)}</td>
                  <td style={over ? { color: "var(--adm-danger, #c0392b)", fontWeight: 600 } : undefined}>{rate}%{over ? " · 超容" : ""}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看容量详情" onClick={(event) => { event.stopPropagation(); openSnapshotDetail(row); }}>查看详情</button></td></tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );

  return (
    <section data-testid="amusement-verification-records-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="amu-verif-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="amusement.verification.viewed" />

      <div className="admin-tabs" data-testid="amu-verif-tabs" role="tablist">
        <button type="button" role="tab" aria-selected={tab === "records"} data-testid="amu-verif-tab-records" className={tab === "records" ? "is-active" : ""} onClick={() => setTab("records")}>核销记录</button>
        <button type="button" role="tab" aria-selected={tab === "capacity"} data-testid="amu-verif-tab-capacity" className={tab === "capacity" ? "is-active" : ""} onClick={() => setTab("capacity")}>在场容量</button>
        <button type="button" role="tab" aria-selected={tab === "reconcile"} data-testid="amu-verif-tab-reconcile" className={tab === "reconcile" ? "is-active" : ""} onClick={() => setTab("reconcile")}>交叉对账</button>
      </div>

      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {auxNotice ? (
          <div className="admin-state" data-testid="amu-verif-aux-degraded">辅助数据（筛选项/容量快照）{auxNotice}</div>
        ) : null}
        <div className="admin-kpi-grid" data-testid="amu-verif-summary">
          <div className="admin-kpi-card"><span>入场</span><strong className="admin-kpi-value" data-testid="amu-verif-kpi-entry">{entryDisplay}</strong><small>入场核销数{entryKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="amusement.entry_count" testid="amu-verif-metric-detail-entry" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>出场</span><strong className="admin-kpi-value" data-testid="amu-verif-kpi-exit">{exitDisplay}</strong><small>出场核销数{exitKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="amusement.exit_count" testid="amu-verif-metric-detail-exit" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>已关联通票</span><strong className="admin-kpi-value" data-testid="amu-verif-kpi-linked">{linkedCount}</strong><small>有通票交叉引用</small></div>
          <div className="admin-kpi-card"><span>超容点位</span><strong className="admin-kpi-value" data-testid="amu-verif-kpi-over">{overCapacityCount}</strong><small>在场超上限</small></div>
        </div>

        {tab === "capacity" ? (
          capacityTable
        ) : (
          <>
            {filterBar}
            {tab === "reconcile" ? (
              <div className="admin-form-row" data-testid="amu-verif-reconcile-bar">
                <label>
                  对账口径
                  <select data-testid="amu-verif-reconcile-select" value={reconcile} onChange={(e) => setReconcile(e.target.value as "all" | "linked" | "unlinked")}>
                    <option value="all">全部核销</option>
                    <option value="linked">仅有通票号（已匹配）</option>
                    <option value="unlinked">仅无通票号（待核对）</option>
                  </select>
                </label>
                <span className="admin-muted-action">对账：通票游乐核销应有交叉引用；无匹配即两侧不一致，需核对。</span>
              </div>
            ) : null}
            {recordsTable}
          </>
        )}
      </AdminDataState>

      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
