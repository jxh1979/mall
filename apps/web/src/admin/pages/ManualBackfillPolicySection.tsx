// 文件版本：V0.01
import React from "react";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 第十八批 F 段第 14 条 14-R8（BSM 第 1642 行「补录时限为业务发生后的配置天数内（安全阈值，按项目可配，调整须最高权限审批并留痕，
// 出厂默认 3 天，须同时配置上下限）」「录音起始时间必须早于客人到店时间、且早于客人开卡时间达到后台配置的时长（业务参数，按项目可配，
// 出厂默认 30 分钟，须同时配置上下限）」）：主管复核页内的人工补录参数块。两组分开提交（带补录时限组须最高权限本人，后端另闸），
// 组内值与上下限同填；只提交本组三项，另一组沿用现生效值。
type Props = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions">;
type Policy = {
  projectId?: string; windowDays?: number; windowMinDays?: number | null; windowMaxDays?: number | null;
  recordingLeadMinutes?: number; leadMinMinutes?: number | null; leadMaxMinutes?: number | null;
};
type Field = "windowDays" | "windowMinDays" | "windowMaxDays" | "recordingLeadMinutes" | "leadMinMinutes" | "leadMaxMinutes";

const PERMISSION = "evidence.manual_backfill.policy.manage";
type Group = "window" | "lead";
const GROUPS: Record<Group, { fields: [Field, Field, Field]; floor: number; unit: string; name: string }> = {
  window: { fields: ["windowDays", "windowMinDays", "windowMaxDays"], floor: 1, unit: "天", name: "补录时限" },
  lead: { fields: ["recordingLeadMinutes", "leadMinMinutes", "leadMaxMinutes"], floor: 0, unit: "分钟", name: "录音提前量" },
};
const EMPTY: Record<Field, string> = { windowDays: "", windowMinDays: "", windowMaxDays: "", recordingLeadMinutes: "", leadMinMinutes: "", leadMaxMinutes: "" };
const FIELDS = Object.keys(EMPTY) as Field[];

function whole(value: string, floor: number): number | null {
  const n = Number(value);
  return value.trim() !== "" && Number.isInteger(n) && n >= floor ? n : null;
}

function bounds(min: number | null | undefined, max: number | null | undefined, unit: string) {
  return min == null ? "（上下限未配置）" : `（下限 ${min}、上限 ${max} ${unit}）`;
}

export function ManualBackfillPolicySection({ api, setActionResult, setGlobalError, effectivePermissions }: Props) {
  const [projectId, setProjectId] = React.useState("");
  const [current, setCurrent] = React.useState<Policy | null>(null);
  const [form, setForm] = React.useState(EMPTY);

  function path() {
    return `/api/v1/evidence/manual-backfill-policies/${encodeURIComponent(projectId.trim())}`;
  }

  async function load() {
    if (!projectId.trim()) { setGlobalError("请先填写综合体项目 id"); return; }
    try {
      const policy = await api.get<Policy | null>(path());
      setCurrent(policy);
      const values: Policy = policy ?? {};
      setForm(Object.fromEntries(FIELDS.map((key) => [key, values[key] == null ? "" : String(values[key])])) as Record<Field, string>);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "人工补录参数读取失败");
    }
  }

  async function save(group: Group) {
    const g = GROUPS[group];
    const [valueKey, minKey, maxKey] = g.fields;
    const value = whole(form[valueKey], g.floor), min = whole(form[minKey], g.floor), max = whole(form[maxKey], g.floor);
    if (!projectId.trim()) { setGlobalError("请先填写综合体项目 id"); return; }
    if (value === null || min === null || max === null || min > value || value > max) {
      setGlobalError(`${g.name}须同时填写${g.unit}数及上下限（整数，下限不小于 ${g.floor}），${g.unit}数须在上下限内`);
      return;
    }
    try {
      await api.put(path(), { [valueKey]: value, [minKey]: min, [maxKey]: max });
      setActionResult(`已保存人工补录参数：项目 ${projectId.trim()}，${group === "window" ? "补录时限" : "录音提前"} ${value} ${g.unit}（下限 ${min}、上限 ${max} ${g.unit}）`);
      void load();
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "人工补录参数保存失败");
    }
  }

  function input(key: Field, label: string, floor: number, testId: string) {
    return <label>{label}<input data-testid={testId} type="number" min={floor} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} /></label>;
  }

  return (
    <div className="admin-preview" data-testid="manual-backfill-policy">
      <strong>人工补录参数（按项目；出厂补录时限 3 天、录音提前 30 分钟；须同时配置上下限）：</strong>
      超过补录时限不得补录；以通话录音为凭证的，录音起始须早于到店与开卡达录音提前量，否则不予采信。
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input data-testid="manual-backfill-policy-project" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <button type="button" data-testid="manual-backfill-policy-query" onClick={() => void load()}>查询</button>
        <span className="admin-muted-action" data-testid="manual-backfill-policy-current">
          {current ? `当前${current.projectId === "__global__" ? "（出厂值）" : ""}：补录时限 ${current.windowDays} 天${bounds(current.windowMinDays, current.windowMaxDays, "天")}；录音提前 ${current.recordingLeadMinutes} 分钟${bounds(current.leadMinMinutes, current.leadMaxMinutes, "分钟")}` : "未查询"}
        </span>
      </div>
      <div className="admin-form-grid" data-testid="manual-backfill-window" style={{ marginTop: 8 }}>
        <span>补录时限（安全阈值，调整须最高权限本人办理）</span>
        {input("windowDays", "补录时限（天）", 1, "manual-backfill-window-days")}
        {input("windowMinDays", "下限（天）", 1, "manual-backfill-window-min")}
        {input("windowMaxDays", "上限（天）", 1, "manual-backfill-window-max")}
        <PermissionButton permissionsState={effectivePermissions} permissions={[PERMISSION]}
          data-testid="manual-backfill-window-save" onClick={() => void save("window")}>保存补录时限</PermissionButton>
      </div>
      <div className="admin-form-grid" data-testid="manual-backfill-lead" style={{ marginTop: 8 }}>
        <span>录音提前量（业务参数）</span>
        {input("recordingLeadMinutes", "录音提前分钟数", 0, "manual-backfill-lead-minutes")}
        {input("leadMinMinutes", "下限（分钟）", 0, "manual-backfill-lead-min")}
        {input("leadMaxMinutes", "上限（分钟）", 0, "manual-backfill-lead-max")}
        <PermissionButton permissionsState={effectivePermissions} permissions={[PERMISSION]}
          data-testid="manual-backfill-lead-save" onClick={() => void save("lead")}>保存录音提前量</PermissionButton>
      </div>
    </div>
  );
}
