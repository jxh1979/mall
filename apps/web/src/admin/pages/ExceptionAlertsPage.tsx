// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { TablePager, usePagedRows } from "../components/TablePager";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 统一风控预警队列的一条（后端 RiskAlertRecord，adminClient 已转驼峰）。
type ExceptionAlert = {
  alertId: string;
  sourceRef: string | null;
  alertType: string;
  riskLevel: string;
  status: string;
  summary: string;
  firstHandlerId: string | null;
  resolutionConclusion: string | null;
  projectId: string | null;
  createdAt: string;
};
type AlertAction = { actionType: string; actorId: string | null; reason: string | null; createdAt: string };
type AlertDetail = { alert: ExceptionAlert; actions: AlertAction[] };

/**
 * 异常预警页汇集的来源（总控驾驶舱未处理条数卡同口径）：平台（同一手机号注销注册超限）、票务（高频出入场）、停车（逃费、超长滞留、人工放行）。
 * 经营分析、企微、库存、游乐四类是各自原页的双写镜像，不收。
 */
export const EXCEPTION_ALERT_SOURCES = ["platform", "ticketing", "parking"];
const BASE = "/api/v1/evidence/risk-alerts";
// ponytail: 只取最近 200 条，多了按状态筛。
const LIST = `${BASE}?source_domain=${EXCEPTION_ALERT_SOURCES.join(",")}&limit=200`;
const STATUS_LABELS: Record<string, string> = { open: "待处理", handling: "处理中", resolved: "已处置" };
const ACTION_LABELS: Record<string, string> = { report: "上报", claim: "认领", transfer: "转办", resolve: "处置" };
const riskLevel = (code: string) => KEY_VALUE_LABELS.risk_level?.[code] ?? formatBusinessStatus(code);
const statusLabel = (code: string) => STATUS_LABELS[code] ?? formatBusinessStatus(code);
const actorLabel = (id: string | null) => (id?.startsWith("system") ? "系统" : id ?? "—");
const errorText = (error: unknown, fallback: string) => (error instanceof Error ? error.message : fallback);
// 18BC 收尾复核修正（PGM:287；与 369-R5 停车人工放行摘要的北京时间同口径）：上报时间、处置记录时间按北京时间显示。
const bjTime = (iso: string) => new Date(iso).toLocaleString("zh-CN",
  { timeZone: "Asia/Shanghai", year: "numeric", month: "long", day: "numeric", hour: "2-digit", minute: "2-digit", hour12: false });

/**
 * 异常预警（ADM-RISK-002）。第十八批 BC 段第 369 条 369-R2（BSM:1657「新增「异常预警」页面，集中汇集账户与出入场行为异常……供风控岗与管理层
 * 查看与处置。预警只作提示与线索汇集，不自动阻断业务；每条预警的查看、转办与处置结论均须留痕。」；BSM:1438、BSM:1448 停车人工放行、滞留与逃费
 * 进异常队列；第十二批裁决 14「由人在异常队列里按审批结果处理关闭」）：读统一风控预警队列，按状态认领、转办（须先认领）、填写处置结论关闭。
 * 风险等级按 ENUM:217 四级名显示（369-R7 同表）。
 */
export function ExceptionAlertsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<ExceptionAlert[]>([]);
  const [status, setStatus] = React.useState("");
  // 筛选改了立刻重取：取数函数读这里（state 要到下一次渲染才更新）。
  const statusRef = React.useRef("");
  const [selectedId, setSelectedId] = React.useState<string | null>(null);
  const [target, setTarget] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [conclusion, setConclusion] = React.useState("");
  const [detail, setDetail] = React.useState<AlertDetail | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => setRows(await api.get<ExceptionAlert[]>(`${LIST}${statusRef.current ? `&status=${statusRef.current}` : ""}`)),
    { onError: setGlobalError, fallbackMessage: "异常预警读取失败", onStart: () => setGlobalError(null) },
  );
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    // 18BC 收尾复核修正（PGM:287）：结果区只写动作名与预警编号，整条记录的来源、首报人、状态码不上结果区（页内表已有中文）。
    makeResult: (action, _request, result) => ({ action, 预警编号: (result as ExceptionAlert).alertId }),
    fallbackMessage: "异常预警操作失败",
  });
  const paged = usePagedRows(rows, 10, status);
  const selected = rows.find((row) => row.alertId === selectedId) ?? null;

  // 375-R1：每打开一次明细，后端按该条预警记查看留痕。
  async function openDetail(row: ExceptionAlert) {
    try {
      setDetail(await api.get<AlertDetail>(`${BASE}/${encodeURIComponent(row.alertId)}`));
    } catch (loadError) {
      setGlobalError(errorText(loadError, "异常预警明细读取失败"));
    }
  }

  async function act(action: string, path: string, request: Record<string, string>) {
    if (!selected) return;
    const ok = await runAction(action, request, () => api.post(`${BASE}/${encodeURIComponent(selected.alertId)}/${path}`, request));
    if (ok) {
      setTarget("");
      setReason("");
      setConclusion("");
    }
  }

  function transfer() {
    if (!target.trim()) {
      setFormError("请填写转办给哪位员工（员工编号）");
      return;
    }
    void act("exception_alert_transferred", "transfer", { targetHandlerId: target.trim(), reason: reason.trim() });
  }

  function requireConclusion() {
    if (conclusion.trim()) return true;
    setFormError("请填写处置结论");
    return false;
  }

  const actionable = selected !== null && ["open", "handling"].includes(selected.status);
  const fields: RowDetailField[] = detail ? [
    { label: "预警类型", value: formatBusinessStatus(detail.alert.alertType) },
    { label: "风险等级", value: riskLevel(detail.alert.riskLevel) },
    { label: "所属综合体", value: detail.alert.projectId ?? "—" },
    { label: "关联编号", value: detail.alert.sourceRef ?? "—" },
    { label: "摘要", value: detail.alert.summary },
    { label: "状态", value: statusLabel(detail.alert.status) },
    { label: "处理人", value: detail.alert.firstHandlerId ?? "—" },
    { label: "处置结论", value: detail.alert.resolutionConclusion ?? "—" },
    { label: "上报时间", value: bjTime(detail.alert.createdAt) },
    { label: "预警编号", value: detail.alert.alertId },
    { label: "处置记录", value: <ul>{detail.actions.map((item, index) => (
      <li key={index}>{[bjTime(item.createdAt), ACTION_LABELS[item.actionType] ?? formatBusinessStatus(item.actionType), actorLabel(item.actorId), item.reason]
        .filter(Boolean).join(" · ")}</li>
    ))}</ul> },
  ] : [];

  return (
    <section data-testid="exception-alerts-page">
      <AdminPageHeader
        title="异常预警"
        description="集中查看客人账户与出入场行为异常（同一手机号注销注册超限、高频出入场、停车逃费与超长滞留、停车人工放行），只作提示、不阻断业务；查看、转办与处置结论均留痕。"
      />
      <div className="admin-form-row" data-testid="exception-alerts-filter">
        <label>状态<select aria-label="状态" value={status} onChange={(event) => {
          statusRef.current = event.target.value;
          setStatus(event.target.value);
          void reload();
        }}>
          <option value="">全部</option>
          {Object.entries(STATUS_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
        </select></label>
      </div>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="exception-alerts-table">
            <thead><tr><th>选择</th><th>预警类型</th><th>风险等级</th><th>摘要</th><th>状态</th><th>处理人</th><th>上报时间</th><th>详情</th></tr></thead>
            <tbody>{paged.pageRows.map((row) => (
              <tr key={row.alertId} className="admin-row-clickable" tabIndex={0} data-testid={`exception-alert-row-${row.alertId}`}
                onClick={() => void openDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); void openDetail(row); } }}>
                <td onClick={(event) => event.stopPropagation()}><button type="button" data-testid={`exception-alert-select-${row.alertId}`}
                  onClick={() => { setSelectedId(row.alertId); setFormError(""); }}>选择</button></td>
                <td>{formatBusinessStatus(row.alertType)}</td>
                <td>{riskLevel(row.riskLevel)}</td>
                <td>{row.summary}</td>
                <td>{statusLabel(row.status)}</td>
                <td>{row.firstHandlerId ?? "—"}</td>
                <td>{bjTime(row.createdAt)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看异常预警详情"
                  onClick={(event) => { event.stopPropagation(); void openDetail(row); }}>查看详情</button></td>
              </tr>
            ))}</tbody>
          </table>
          <TablePager testid="exception-alerts-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
      {actionable && selected ? (
        <div className="admin-form-row" data-testid="exception-alert-actions">
          <span>选中预警：{selected.summary}</span>
          {selected.status === "open" ? (
            <PermissionButton permissionsState={effectivePermissions} permissions={["risk_alert.claim"]} disabled={busyAction !== ""}
              data-testid="exception-alert-claim" onClick={() => void act("exception_alert_claimed", "claim", {})}>认领</PermissionButton>
          ) : (
            <>
              <label>转办给（员工编号）<input aria-label="转办给（员工编号）" value={target} onChange={(event) => setTarget(event.target.value)} /></label>
              <label>转办原因<input aria-label="转办原因" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
              <PermissionButton permissionsState={effectivePermissions} permissions={["risk_alert.transfer"]} disabled={busyAction !== ""}
                data-testid="exception-alert-transfer" onClick={transfer}>转办</PermissionButton>
            </>
          )}
          <label>处置结论<input aria-label="处置结论" value={conclusion} onChange={(event) => setConclusion(event.target.value)} /></label>
          <HighRiskConfirm
            label="处置"
            impact="预警按填写的处置结论关闭，结论与处置人进留痕，关闭后不能再认领或转办；误报也写在结论里。"
            disabled={busyAction !== ""}
            precheck={requireConclusion}
            onConfirm={() => void act("exception_alert_resolved", "resolve", { resolutionConclusion: conclusion.trim() })}
            permissionsState={effectivePermissions}
            permissions={["risk_alert.resolve"]}
            data-testid="exception-alert-resolve"
          />
        </div>
      ) : null}
      {formError ? <div className="admin-inline-error" data-testid="exception-alert-error">{formError}</div> : null}
      <RowDetailDrawer open={detail !== null} title={`异常预警明细 · ${detail?.alert.summary ?? ""}`} fields={fields} onClose={() => setDetail(null)} />
    </section>
  );
}
