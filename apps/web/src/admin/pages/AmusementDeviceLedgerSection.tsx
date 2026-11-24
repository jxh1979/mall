// 文件版本：V0.01
import React from "react";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import { formatBusinessStatus } from "../i18n/statusLabels";
import type { AdminPageProps } from "../types";

// 第十八批 CF 段第 407 条 407-R3（BSM:1271「每个游乐项目可以关联多台设备，并区分「关键安全设备」与「辅助设备」……每台关键安全设备属于哪一种，须在设备台账中
// 登记……登记内容变更须经工程或安全审批。项目上线后要把某台关键安全设备改为辅助设备……只能在该项目停运后生效」「判断结论必须留痕，记下是谁判的、什么时间判的、
// 依据了哪几项条件」）：安全巡检页的游乐设备台账——选项目后列设备与分级（复核中提示）、新建设备（分级与理由随建档进工程或安全复核）、申请变更分级、分级历史、
// 停用设备与辅助设备「能否继续运行」判定。分级、依赖关系、判定结论与停用原因的中文名放本组件（共用状态码表的 critical 已是「紧急」）。

type Props = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions"> & {
  projects: Array<{ id: string; projectName?: string }>;
};
type Device = { id: string; deviceCode?: string; name?: string | null; safetyCheckIntervalHours?: number | null; safetyClass?: string | null;
  dependency?: string | null; lastPassInspectionAt?: string | null };
type Change = { id: string; deviceId?: string; fromClass?: string | null; fromDependency?: string | null; toClass?: string; toDependency?: string | null;
  reason?: string; status?: string; requestedBy?: string; requestedAt?: string; decidedBy?: string | null; decidedAt?: string | null };
type Judgement = { conclusion?: string; conditions?: string[]; judgedBy?: string; judgedAt?: string };
type Stop = { deviceId: string; deviceCode?: string; name?: string | null; safetyClass?: string | null; dependency?: string | null; reasons?: string[]; stoppedAt?: string;
  latestJudgement?: Judgement | null };
type ClassForm = { safetyClass: string; dependency: string; reason: string; finalApproverId: string };

const MANAGE = "amusement.device.manage";
const JUDGE = "amusement.device.continue_judge";
const LABELS: Record<string, string> = {
  critical: "关键安全设备", auxiliary: "辅助设备", dependent: "依赖（必经环节）", independent: "不依赖（可顶替或可绕过）",
  continue: "可继续运行", stop: "停止运行", expired: "巡检超期", inspection_failed: "巡检不合格",
};
const EMPTY_CLASS: ClassForm = { safetyClass: "auxiliary", dependency: "", reason: "", finalApproverId: "" };

function label(code?: string | null): string {
  return code ? LABELS[code] ?? "—" : "—";
}

function classText(safetyClass?: string | null, dependency?: string | null): string {
  if (!safetyClass) return "未分级";
  return safetyClass === "critical" ? `${label(safetyClass)}（${label(dependency)}）` : label(safetyClass);
}

/** 分级选择：关键安全设备时再选依赖关系。 */
function ClassFields({ value, onChange, testid }: { value: ClassForm; onChange: (next: ClassForm) => void; testid: string }) {
  return (
    <>
      <span>
        {(["critical", "auxiliary"] as const).map((code) => (
          <label key={code}><input type="radio" name={`${testid}-class`} data-testid={`${testid}-class-${code}`} checked={value.safetyClass === code}
            onChange={() => onChange({ ...value, safetyClass: code, dependency: code === "critical" ? value.dependency : "" })} />{LABELS[code]}</label>
        ))}
      </span>
      {value.safetyClass === "critical" ? (
        <label>依赖关系<select data-testid={`${testid}-dependency`} value={value.dependency} onChange={(e) => onChange({ ...value, dependency: e.target.value })}>
          <option value="">请选择</option>
          <option value="dependent">{LABELS.dependent}</option>
          <option value="independent">{LABELS.independent}</option>
        </select></label>
      ) : null}
      <label>理由<input data-testid={`${testid}-reason`} value={value.reason} onChange={(e) => onChange({ ...value, reason: e.target.value })} /></label>
      <label>最终审批人（可空）<input data-testid={`${testid}-approver`} value={value.finalApproverId}
        onChange={(e) => onChange({ ...value, finalApproverId: e.target.value })} /></label>
    </>
  );
}

function classBody(form: ClassForm) {
  return {
    safetyClass: form.safetyClass,
    ...(form.safetyClass === "critical" ? { dependency: form.dependency } : {}),
    ...(form.finalApproverId.trim() ? { finalApproverId: form.finalApproverId.trim() } : {}),
  };
}

export function AmusementDeviceLedgerSection({ api, setActionResult, setGlobalError, effectivePermissions, projects }: Props) {
  const [projectId, setProjectId] = React.useState("");
  const [devices, setDevices] = React.useState<Device[]>([]);
  const [changes, setChanges] = React.useState<Change[]>([]);
  const [stops, setStops] = React.useState<Stop[]>([]);
  const [create, setCreate] = React.useState({ deviceCode: "", name: "", interval: "", ...EMPTY_CLASS });
  const [changeTarget, setChangeTarget] = React.useState<Device | null>(null);
  const [change, setChange] = React.useState<ClassForm>(EMPTY_CLASS);
  const [judgements, setJudgements] = React.useState<Record<string, { conclusion: string; conditions: string }>>({});

  const base = (id: string) => `/api/v1/amusement/projects/${encodeURIComponent(id)}/devices`;

  async function load(id: string) {
    setProjectId(id);
    setDevices([]); setChanges([]); setStops([]); setChangeTarget(null);
    if (!id) return;
    try {
      const [deviceRows, changeRows, stopRows] = await Promise.all([
        api.get<Device[]>(base(id)), api.get<Change[]>(`${base(id)}/classification-changes`), api.get<Stop[]>(`${base(id)}/stops`)]);
      setDevices(Array.isArray(deviceRows) ? deviceRows : []);
      setChanges(Array.isArray(changeRows) ? changeRows : []);
      setStops(Array.isArray(stopRows) ? stopRows : []);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "游乐设备台账读取失败");
    }
  }

  function classInvalid(form: ClassForm): string | null {
    if (form.safetyClass === "critical" && !form.dependency) return "关键安全设备须选择依赖关系";
    return form.reason.trim() ? null : "请填写分级理由";
  }

  async function submitCreate() {
    if (!projectId) { setGlobalError("请先选择游乐项目"); return; }
    const interval = Number(create.interval);
    const invalid = !create.deviceCode.trim() ? "请填写设备码" : !Number.isInteger(interval) || interval < 1 ? "安全检查周期须为正整数（小时）" : classInvalid(create);
    if (invalid) { setGlobalError(invalid); return; }
    try {
      await api.post(base(projectId), { deviceCode: create.deviceCode.trim(), name: create.name.trim() || undefined, status: "active",
        safetyCheckIntervalHours: interval, operatorNote: create.reason.trim(), ...classBody(create) });
      setActionResult(`已建档设备 ${create.deviceCode.trim()}，分级「${classText(create.safetyClass, create.dependency)}」待工程／安全复核`);
      setCreate({ deviceCode: "", name: "", interval: "", ...EMPTY_CLASS });
      void load(projectId);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "游乐设备建档失败");
    }
  }

  async function submitChange() {
    if (!changeTarget) return;
    const invalid = classInvalid(change);
    if (invalid) { setGlobalError(invalid); return; }
    try {
      await api.post(`${base(projectId)}/${encodeURIComponent(changeTarget.id)}/classification-changes`, { reason: change.reason.trim(), ...classBody(change) });
      setActionResult(`已申请把 ${changeTarget.deviceCode ?? changeTarget.id} 改为「${classText(change.safetyClass, change.dependency)}」，待工程／安全复核`);
      setChangeTarget(null);
      void load(projectId);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "分级变更申请失败");
    }
  }

  async function submitJudgement(stop: Stop) {
    const form = judgements[stop.deviceId] ?? { conclusion: "continue", conditions: "" };
    const conditions = form.conditions.split("\n").map((line) => line.trim()).filter((line) => line !== "");
    if (conditions.length === 0) { setGlobalError("请至少填写一项判定依据的条件（一行一项）"); return; }
    try {
      await api.post(`${base(projectId)}/${encodeURIComponent(stop.deviceId)}/continue-judgements`, { conclusion: form.conclusion, conditions });
      setActionResult(`已留痕：${stop.deviceCode ?? stop.deviceId} 停用后项目${label(form.conclusion)}（依据 ${conditions.length} 项）`);
      void load(projectId);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "继续运行判定提交失败");
    }
  }

  const pending = (deviceId: string) => changes.some((row) => row.deviceId === deviceId && row.status === "pending_review");
  const deviceCode = (deviceId?: string) => devices.find((row) => row.id === deviceId)?.deviceCode ?? deviceId ?? "—";
  const downgrade = changeTarget?.safetyClass === "critical" && change.safetyClass === "auxiliary";

  return (
    <div className="admin-preview" data-testid="amusement-device-ledger">
      <h3>游乐设备台账</h3>
      <label>游乐项目<select data-testid="amusement-device-ledger-project" value={projectId} onChange={(e) => void load(e.target.value)}>
        <option value="">选择项目</option>
        {projects.map((p) => (<option key={p.id} value={p.id}>{p.projectName ?? p.id}</option>))}
      </select></label>
      {projectId ? (
        <>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="amusement-device-table">
              <thead><tr><th>设备码</th><th>名称</th><th>安全检查周期（小时）</th><th>分级</th><th>依赖关系</th><th>分级状态</th><th>最近合格巡检</th><th>操作</th></tr></thead>
              <tbody>
                {devices.map((row) => (
                  <tr key={row.id} data-testid={`amusement-device-row-${row.id}`}>
                    <td>{row.deviceCode ?? "—"}</td><td>{row.name ?? "—"}</td><td>{row.safetyCheckIntervalHours ?? "按项目缺省"}</td>
                    <td>{row.safetyClass ? label(row.safetyClass) : "未分级"}</td><td>{row.safetyClass === "critical" ? label(row.dependency) : "—"}</td>
                    <td>{pending(row.id) ? "分级待工程／安全复核" : row.safetyClass ? "已生效" : "未分级"}</td>
                    <td>{cnDate(row.lastPassInspectionAt ?? undefined)}</td>
                    <td><PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} data-testid={`amusement-device-change-open-${row.id}`}
                      disabled={pending(row.id)} onClick={() => { setChangeTarget(row); setChange(EMPTY_CLASS); }}>申请变更分级</PermissionButton></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {changeTarget ? (
            <div className="admin-form-grid" data-testid="amusement-device-change-form">
              <span>申请变更 {changeTarget.deviceCode ?? changeTarget.id} 的分级（现为「{classText(changeTarget.safetyClass, changeTarget.dependency)}」，须经工程或安全复核）</span>
              <ClassFields value={change} onChange={setChange} testid="amusement-device-change" />
              {downgrade ? <span className="admin-muted-action" data-testid="amusement-device-change-downgrade">关键安全设备改为辅助设备：项目停运后审批才能生效</span> : null}
              <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} data-testid="amusement-device-change-submit"
                onClick={() => void submitChange()}>提交分级变更</PermissionButton>
              <button type="button" onClick={() => setChangeTarget(null)}>取消</button>
            </div>
          ) : null}
          <div className="admin-form-grid" data-testid="amusement-device-create" style={{ marginTop: 8 }}>
            <span>新建设备（分级随建档进工程或安全复核，通过后才生效）</span>
            <label>设备码<input data-testid="amusement-device-create-code" value={create.deviceCode} onChange={(e) => setCreate({ ...create, deviceCode: e.target.value })} /></label>
            <label>名称<input data-testid="amusement-device-create-name" value={create.name} onChange={(e) => setCreate({ ...create, name: e.target.value })} /></label>
            <label>安全检查周期（小时）<input data-testid="amusement-device-create-interval" type="number" min={1} value={create.interval}
              onChange={(e) => setCreate({ ...create, interval: e.target.value })} /></label>
            <ClassFields value={create} onChange={(next) => setCreate({ ...create, ...next })} testid="amusement-device-create" />
            <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE]} data-testid="amusement-device-create-submit"
              onClick={() => void submitCreate()}>建档</PermissionButton>
          </div>
          <h4>分级历史</h4>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="amusement-device-changes">
              <thead><tr><th>设备</th><th>原分级</th><th>申请分级</th><th>理由</th><th>申请人</th><th>申请时间</th><th>状态</th><th>复核人</th><th>生效时间</th></tr></thead>
              <tbody>
                {changes.map((row) => (
                  <tr key={row.id} data-testid={`amusement-device-change-${row.id}`}>
                    <td>{deviceCode(row.deviceId)}</td><td>{classText(row.fromClass, row.fromDependency)}</td><td>{classText(row.toClass, row.toDependency)}</td>
                    <td>{row.reason ?? "—"}</td><td>{row.requestedBy ?? "—"}</td><td>{cnDate(row.requestedAt)}</td><td>{formatBusinessStatus(row.status)}</td>
                    <td>{row.decidedBy ?? "—"}</td><td>{row.status === "approved" ? cnDate(row.decidedAt ?? undefined) : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <h4>停用设备</h4>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="amusement-device-stops">
              <thead><tr><th>设备</th><th>分级</th><th>停用原因</th><th>停用起点</th><th>最近判定</th><th>能否继续运行（工程或安全岗位判定）</th></tr></thead>
              <tbody>
                {stops.map((row) => {
                  const form = judgements[row.deviceId] ?? { conclusion: "continue", conditions: "" };
                  const judged = row.latestJudgement;
                  return (
                    <tr key={row.deviceId} data-testid={`amusement-device-stop-${row.deviceId}`}>
                      <td>{row.deviceCode ?? row.deviceId}{row.name ? `（${row.name}）` : ""}</td><td>{classText(row.safetyClass, row.dependency)}</td>
                      <td>{(row.reasons ?? []).map(label).join("、")}</td><td>{cnDate(row.stoppedAt)}</td>
                      <td>{row.safetyClass !== "auxiliary" ? "—" : judged ? `${label(judged.conclusion)}：${(judged.conditions ?? []).join("；")}（${judged.judgedBy ?? "—"}，${cnDate(judged.judgedAt)}）` : "未判定（按停止运行处理）"}</td>
                      <td>{row.safetyClass === "auxiliary" ? (
                        <span className="admin-form-row">
                          <select data-testid={`amusement-device-judge-conclusion-${row.deviceId}`} value={form.conclusion}
                            onChange={(e) => setJudgements({ ...judgements, [row.deviceId]: { ...form, conclusion: e.target.value } })}>
                            <option value="continue">{LABELS.continue}</option>
                            <option value="stop">{LABELS.stop}</option>
                          </select>
                          <textarea data-testid={`amusement-device-judge-conditions-${row.deviceId}`} rows={2} placeholder="依据的条件，一行一项" value={form.conditions}
                            onChange={(e) => setJudgements({ ...judgements, [row.deviceId]: { ...form, conditions: e.target.value } })} />
                          <PermissionButton permissionsState={effectivePermissions} permissions={[JUDGE]} data-testid={`amusement-device-judge-submit-${row.deviceId}`}
                            onClick={() => void submitJudgement(row)}>提交判定</PermissionButton>
                        </span>
                      ) : "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </div>
  );
}
