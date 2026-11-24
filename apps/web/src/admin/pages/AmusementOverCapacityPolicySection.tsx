// 文件版本：V0.01
import React from "react";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 第十八批 CF 段第 409 条 409-R2（BSM:1267「达到运营容量（软上限）时的处置方式可在后台配置为两种之一：「记录异常事件」（超过软上限即生成异常事件留痕，
// 出厂默认）或「仅提示」（只在现场提示、不生成异常事件）；两种方式都不改变硬安全容量的阻断与自动停运」；BSM:1295；ENUM:145）：「项目与场次」页的
// 软上限处置方式——选游乐项目后读当前运营容量与处置方式，改后保存。两种方式的中文名放本组件（block_hard、warn_only 是实现别名）。

type Props = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions"> & {
  projects: Array<{ id: string; projectName?: string }>;
};
type Policy = { projectId?: string; operatingCapacity?: number; disposalMode?: string; enabled?: boolean };

const MODES = [{ code: "block_hard", label: "记录异常事件（出厂默认）" }, { code: "warn_only", label: "仅提示" }] as const;
const CONFIGURE = "amusement.over_capacity_policy.configure";

function modeLabel(code?: string): string {
  return MODES.find((mode) => mode.code === code)?.label ?? MODES[0].label;
}

export function AmusementOverCapacityPolicySection({ api, setActionResult, setGlobalError, effectivePermissions, projects }: Props) {
  const [projectId, setProjectId] = React.useState("");
  const [policy, setPolicy] = React.useState<Policy | null>(null);
  const [capacity, setCapacity] = React.useState("");
  const [mode, setMode] = React.useState<string>("block_hard");

  const path = (id: string) => `/api/v1/amusement/projects/${encodeURIComponent(id)}/over-capacity-policy`;

  function show(next: Policy | null) {
    setPolicy(next);
    setCapacity(next?.enabled && (next.operatingCapacity ?? 0) > 0 ? String(next.operatingCapacity) : "");
    setMode(next?.disposalMode === "warn_only" ? "warn_only" : "block_hard");
  }

  async function load(id: string) {
    setProjectId(id);
    show(null);
    if (!id) return;
    try {
      show(await api.get<Policy>(path(id)));
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "软上限处置方式读取失败");
    }
  }

  async function save() {
    if (!projectId) { setGlobalError("请先选择游乐项目"); return; }
    const value = Number(capacity);
    if (capacity.trim() === "" || !Number.isInteger(value) || value < 1) { setGlobalError("运营容量（软上限）须为正整数"); return; }
    try {
      show(await api.put<Policy>(path(projectId), { operatingCapacity: value, disposalMode: mode }));
      setActionResult(`已保存软上限处置方式：运营容量 ${value} 人，达到时${modeLabel(mode)}`);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "软上限处置方式保存失败");
    }
  }

  const configured = policy?.enabled && (policy.operatingCapacity ?? 0) > 0;
  return (
    <div className="admin-form-grid" data-testid="amusement-overcap" style={{ marginTop: 8 }}>
      <span>运营容量（软上限）与处置方式（达到时只告警、仍放行；硬安全容量照旧阻断并自动停运）</span>
      <label>游乐项目<select data-testid="amusement-overcap-project" value={projectId} onChange={(e) => void load(e.target.value)}>
        <option value="">选择项目</option>
        {projects.map((p) => (<option key={p.id} value={p.id}>{p.projectName ?? p.id}</option>))}
      </select></label>
      <span className="admin-muted-action" data-testid="amusement-overcap-current">
        {!projectId || !policy ? "未查询" : configured ? `当前：运营容量 ${policy.operatingCapacity} 人，达到时${modeLabel(policy.disposalMode)}`
          : "未配置运营容量（只按硬安全容量阻断）"}
      </span>
      <label>运营容量（人）<input data-testid="amusement-overcap-capacity" type="number" min={1} value={capacity} onChange={(e) => setCapacity(e.target.value)} /></label>
      <span>
        {MODES.map((item) => (
          <label key={item.code}><input type="radio" name="amusement-overcap-mode" data-testid={`amusement-overcap-mode-${item.code}`}
            checked={mode === item.code} onChange={() => setMode(item.code)} />{item.label}</label>
        ))}
      </span>
      <PermissionButton permissionsState={effectivePermissions} permissions={[CONFIGURE]} data-testid="amusement-overcap-save"
        onClick={() => void save()}>保存处置方式</PermissionButton>
    </div>
  );
}
