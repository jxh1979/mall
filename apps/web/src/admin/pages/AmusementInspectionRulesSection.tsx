// 文件版本：V0.01
import React from "react";
import { QRCodeSVG } from "qrcode.react";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import { ApiError } from "../../shared/apiError";
import type { AdminPageProps } from "../types";
import { inspectionPointsPath, type InspectionPoint, type InspectionPointsView } from "./AmusementInspectionWalkPanel";

// 第十八批 BS 段第 373 条 373-R3、373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过……」「营业中抽检有最低频次要求，
// 每隔多久至少抽检一次由后台配置（安全阈值，按项目可配，须同时配置上限与下限，出厂默认每 2 小时一次），达不到最低频次的视为当班未抽检」；
// 第十八批裁决 76「每个点位生成一张码贴在现场」）：安全巡检页的三块规则——①游乐项目巡检点位清单（一行一个，从上到下即顺序；保存后逐个显示点位码
// 二维码供打印张贴）；②营业中抽检间隔（按综合体项目，三项同填，调整须最高权限本人办理，后端拒绝文案原样显示）；③当班未抽检记录。

type Props = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions"> & {
  projects: Array<{ id: string; projectName?: string }>;
};
type SpotPolicy = { projectId?: string; intervalMinutes?: number; minMinutes?: number | null; maxMinutes?: number | null };
type SpotMiss = { id: string; projectId?: string; projectName?: string; anchorAt?: string; firstAdmissionAt?: string; intervalMinutes?: number; detectedAt?: string };

const MANAGE = "amusement.safety_template.manage";
const SPOT_FIELDS = ["intervalMinutes", "minMinutes", "maxMinutes"] as const;
type SpotField = (typeof SPOT_FIELDS)[number];

function whole(value: string): number | null {
  const n = Number(value);
  return value.trim() !== "" && Number.isInteger(n) && n >= 1 ? n : null;
}

// 公共翻译层把 403 一律译成「无权」；最高权限闸的拒绝要让人看懂找谁办（同 DistributionSchemePanel 按细码给句子）。
function errorText(err: unknown, fallback: string): string {
  if (err instanceof ApiError && err.detailCode === "MASTER_APPROVAL_REQUIRED") return "营业中抽检间隔是安全阈值，调整须由最高权限本人办理";
  return err instanceof Error ? err.message : fallback;
}

export function AmusementInspectionRulesSection({ api, setActionResult, setGlobalError, effectivePermissions, projects }: Props) {
  const [pointProject, setPointProject] = React.useState("");
  const [pointText, setPointText] = React.useState("");
  const [points, setPoints] = React.useState<InspectionPoint[]>([]);
  const [spotProject, setSpotProject] = React.useState("");
  const [spotPolicy, setSpotPolicy] = React.useState<SpotPolicy | null>(null);
  const [spotForm, setSpotForm] = React.useState<Record<SpotField, string>>({ intervalMinutes: "", minMinutes: "", maxMinutes: "" });
  const [misses, setMisses] = React.useState<SpotMiss[]>([]);
  const [missError, setMissError] = React.useState("");

  React.useEffect(() => {
    let alive = true;
    api.get<SpotMiss[]>("/api/v1/amusement/spot-check-misses")
      .then((rows) => { if (alive) setMisses(Array.isArray(rows) ? rows : []); })
      .catch(() => { if (alive) setMissError("当班未抽检记录读取失败"); });
    return () => { alive = false; };
  }, [api]);

  function showPoints(view: InspectionPointsView | null) {
    const list = view && Array.isArray(view.points) ? view.points : [];
    setPoints(list);
    setPointText(list.map((point) => point.pointName).join("\n"));
  }

  async function loadPoints(projectId: string) {
    setPointProject(projectId);
    showPoints(null);
    if (!projectId) return;
    try {
      showPoints(await api.get<InspectionPointsView>(inspectionPointsPath(projectId)));
    } catch (err) {
      setGlobalError(errorText(err, "巡检点位清单读取失败"));
    }
  }

  async function savePoints() {
    if (!pointProject) { setGlobalError("请先选择游乐项目"); return; }
    const names = pointText.split("\n").map((line) => line.trim()).filter((line) => line !== "");
    try {
      showPoints(await api.put<InspectionPointsView>(inspectionPointsPath(pointProject), { points: names }));
      setActionResult(`已保存巡检点位清单：${names.length} 个点位（${names.join("、") || "已清空"}）`);
    } catch (err) {
      setGlobalError(errorText(err, "巡检点位清单保存失败"));
    }
  }

  function spotPath() {
    return `/api/v1/amusement/spot-check-policies/${encodeURIComponent(spotProject.trim())}`;
  }

  async function loadSpot() {
    if (!spotProject.trim()) { setGlobalError("请先填写综合体项目编号"); return; }
    try {
      const policy = await api.get<SpotPolicy | null>(spotPath());
      setSpotPolicy(policy);
      const values: SpotPolicy = policy ?? {};
      setSpotForm({
        intervalMinutes: values.intervalMinutes == null ? "" : String(values.intervalMinutes),
        minMinutes: values.minMinutes == null ? "" : String(values.minMinutes),
        maxMinutes: values.maxMinutes == null ? "" : String(values.maxMinutes),
      });
    } catch (err) {
      setGlobalError(errorText(err, "营业中抽检间隔读取失败"));
    }
  }

  async function saveSpot() {
    if (!spotProject.trim()) { setGlobalError("请先填写综合体项目编号"); return; }
    const value = whole(spotForm.intervalMinutes), min = whole(spotForm.minMinutes), max = whole(spotForm.maxMinutes);
    if (value === null || min === null || max === null || min > value || value > max) {
      setGlobalError("抽检间隔须同时填写分钟数及上下限（整数，下限不小于 1），间隔须在上下限内");
      return;
    }
    try {
      await api.put(spotPath(), { intervalMinutes: value, minMinutes: min, maxMinutes: max });
      setActionResult(`已保存营业中抽检间隔：项目 ${spotProject.trim()}，每 ${value} 分钟至少抽检一次（下限 ${min}、上限 ${max} 分钟）`);
      void loadSpot();
    } catch (err) {
      setGlobalError(errorText(err, "营业中抽检间隔保存失败"));
    }
  }

  return (
    <div className="admin-preview" data-testid="amusement-inspection-rules">
      <h3>巡检点位与营业中抽检</h3>
      <div className="admin-form-grid" data-testid="amusement-rules-points">
        <span>巡检点位清单（按游乐项目；一行一个点位，从上到下即走点顺序；保存后打印下方点位码贴在现场，巡检员用工作手机按顺序扫码）</span>
        <label>游乐项目<select data-testid="amusement-rules-points-project" value={pointProject} onChange={(e) => void loadPoints(e.target.value)}>
          <option value="">选择项目</option>
          {projects.map((p) => (<option key={p.id} value={p.id}>{p.projectName ?? p.id}</option>))}
        </select></label>
        <label>点位（一行一个）<textarea data-testid="amusement-rules-points-text" rows={5} value={pointText} onChange={(e) => setPointText(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} data-testid="amusement-rules-points-save"
          onClick={() => void savePoints()}>保存点位清单</PermissionButton>
      </div>
      {points.length > 0 ? (
        <div className="admin-form-row" data-testid="amusement-rules-point-codes">
          {points.map((point) => (
            <figure key={point.seq} data-testid={`amusement-rules-point-qr-${point.seq}`}>
              {point.pointCode ? <QRCodeSVG value={point.pointCode} size={120} level="M" title={`第 ${point.seq} 个点位码`} /> : null}
              <figcaption>{point.seq}. {point.pointName}（{point.pointCode ?? "—"}）</figcaption>
            </figure>
          ))}
        </div>
      ) : null}
      <div className="admin-form-grid" data-testid="amusement-rules-spot" style={{ marginTop: 8 }}>
        <span>营业中抽检间隔（安全阈值，按综合体项目，调整须最高权限本人办理；出厂每 120 分钟一次）</span>
        <label>综合体项目编号<input data-testid="amusement-rules-spot-project" value={spotProject} onChange={(e) => setSpotProject(e.target.value)} /></label>
        <button type="button" data-testid="amusement-rules-spot-query" onClick={() => void loadSpot()}>查询</button>
        <span className="admin-muted-action" data-testid="amusement-rules-spot-current">
          {spotPolicy ? `当前${spotPolicy.projectId === "__global__" ? "（出厂值）" : ""}：每 ${spotPolicy.intervalMinutes} 分钟至少抽检一次${spotPolicy.minMinutes == null ? "（上下限未配置）" : `（下限 ${spotPolicy.minMinutes}、上限 ${spotPolicy.maxMinutes} 分钟）`}` : "未查询"}
        </span>
        <label>间隔（分钟）<input data-testid="amusement-rules-spot-interval" type="number" min={1} value={spotForm.intervalMinutes} onChange={(e) => setSpotForm({ ...spotForm, intervalMinutes: e.target.value })} /></label>
        <label>下限（分钟）<input data-testid="amusement-rules-spot-min" type="number" min={1} value={spotForm.minMinutes} onChange={(e) => setSpotForm({ ...spotForm, minMinutes: e.target.value })} /></label>
        <label>上限（分钟）<input data-testid="amusement-rules-spot-max" type="number" min={1} value={spotForm.maxMinutes} onChange={(e) => setSpotForm({ ...spotForm, maxMinutes: e.target.value })} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} data-testid="amusement-rules-spot-save"
          onClick={() => void saveSpot()}>保存抽检间隔</PermissionButton>
      </div>
      <h4>当班未抽检记录</h4>
      {missError ? <div className="admin-inline-error" data-testid="amusement-rules-misses-error">{missError}</div> : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="amusement-rules-misses">
            <thead><tr><th>项目</th><th>上次巡检时间</th><th>超间隔后首次入场时间</th><th>当时间隔（分钟）</th><th>判定时间</th></tr></thead>
            <tbody>
              {misses.map((row) => (
                <tr key={row.id} data-testid={`amusement-rules-miss-${row.id}`}>
                  <td>{row.projectName ?? row.projectId ?? "—"}</td>
                  <td>{cnDate(row.anchorAt)}</td>
                  <td>{cnDate(row.firstAdmissionAt)}</td>
                  <td>{row.intervalMinutes ?? "—"}</td>
                  <td>{cnDate(row.detectedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
