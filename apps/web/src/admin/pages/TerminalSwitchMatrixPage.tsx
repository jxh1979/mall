import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { TERMINAL_LABELS } from "../i18n/fieldLabels";

// 3.19 终端十端开关统一矩阵（业务HTML「终端开关」节）：十类终端总开关 + 各能力开关按端分组，
// 一页统管。关闭后运营可见可控；「接口拒绝业务动作 + 拉取失败兜底关闭」的逐请求后端强制为后续专项。
type SwitchRow = {
  id: string;
  terminalType: string;
  capabilityCode: string;
  scopeType: string;
  scopeId: string;
  enabled: boolean;
  reason: string | null;
};

// R-10（2026-07-22 用户裁决）：灰度层 = 求值链最细的一档，只能收窄不能放宽。
// 某「端 × 能力」开了灰度试点后：白名单内的对象照常，白名单外一律关闭；
// 上面任一层（全局/综合体/商户/业务系统/模块/版本设备）关闭仍然压倒灰度白名单。
type CanaryTargetRow = {
  id: string;
  rolloutId: string;
  targetType: string;
  targetId: string;
  reason: string | null;
  archivedAt: string | null;
};

type CanaryRolloutRow = {
  id: string;
  terminalType: string;
  capabilityCode: string;
  active: boolean;
  reason: string | null;
};

type CanaryRolloutView = {
  rollout: CanaryRolloutRow;
  targets: CanaryTargetRow[];
};

const MANAGE = ["terminal.switch.manage"];

/** 试点对象可用维度（与后端 V342 chk_tcct_target_type 一致，不含全局）。 */
const TARGET_TYPE_LABELS: Record<string, string> = {
  project: "综合体项目",
  merchant: "商户",
  business_system: "业务系统",
  module: "模块",
  version: "端版本",
  device: "设备",
};

function toRows(data: unknown): SwitchRow[] {
  if (Array.isArray(data)) return data as SwitchRow[];
  const items = (data as { items?: SwitchRow[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

function toCanaryViews(data: unknown): CanaryRolloutView[] {
  if (Array.isArray(data)) return data as CanaryRolloutView[];
  const items = (data as { items?: CanaryRolloutView[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

const EMPTY_ROLLOUT_FORM = { terminalType: "kiosk", capabilityCode: "", reason: "" };
const EMPTY_TARGET_FORM = { targetType: "device", targetId: "", reason: "" };

export function TerminalSwitchMatrixPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SwitchRow[]>([]);
  const [canary, setCanary] = React.useState<CanaryRolloutView[]>([]);
  const [rolloutForm, setRolloutForm] = React.useState(EMPTY_ROLLOUT_FORM);
  const [targetForms, setTargetForms] = React.useState<Record<string, typeof EMPTY_TARGET_FORM>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      const data = await api.get<unknown>("/api/v1/terminals/capability-switches");
      setRows(toRows(data));
      const canaryData = await api.get<unknown>("/api/v1/terminals/canary-rollouts");
      setCanary(toCanaryViews(canaryData));
    },
    { onError: setGlobalError, fallbackMessage: "终端开关矩阵读取失败" },
  );

  async function toggle(row: SwitchRow) {
    try {
      await api.put(`/api/v1/terminals/capability-switches/${row.id}`, {
        enabled: !row.enabled,
        reason: `${row.enabled ? "后台关闭" : "后台打开"}：${TERMINAL_LABELS[row.terminalType] ?? row.terminalType} / ${row.capabilityCode}`,
      });
      setActionResult({ action: "terminal_switch_toggled", id: row.id, enabled: !row.enabled });
      reload();
    } catch (toggleError) {
      setGlobalError(toggleError instanceof Error ? toggleError.message : "开关切换失败");
    }
  }

  function targetForm(rolloutId: string) {
    return targetForms[rolloutId] ?? EMPTY_TARGET_FORM;
  }

  function setTargetForm(rolloutId: string, patch: Partial<typeof EMPTY_TARGET_FORM>) {
    setTargetForms((current) => ({ ...current, [rolloutId]: { ...targetForm(rolloutId), ...patch } }));
  }

  /** 开/关某「端 × 能力」的灰度试点。开=进入试点期（白名单外一律关闭）。 */
  async function saveRollout(terminalType: string, capabilityCode: string, active: boolean, reason: string) {
    if (!capabilityCode.trim() || !reason.trim()) {
      setGlobalError("能力码和理由都要填写");
      return;
    }
    try {
      await api.put("/api/v1/terminals/canary-rollouts", {
        terminalType,
        capabilityCode: capabilityCode.trim(),
        active,
        reason: reason.trim(),
      });
      setActionResult({ action: "terminal_canary_rollout_saved", capabilityCode, active });
      setRolloutForm(EMPTY_ROLLOUT_FORM);
      reload();
    } catch (saveError) {
      setGlobalError(saveError instanceof Error ? saveError.message : "灰度试点保存失败");
    }
  }

  /** 把一个试点对象加进白名单。 */
  async function addTarget(rolloutId: string) {
    const form = targetForm(rolloutId);
    if (!form.targetId.trim() || !form.reason.trim()) {
      setGlobalError("试点对象和理由都要填写");
      return;
    }
    try {
      await api.post(`/api/v1/terminals/canary-rollouts/${rolloutId}/targets`, {
        targetType: form.targetType,
        targetId: form.targetId.trim(),
        reason: form.reason.trim(),
      });
      setActionResult({ action: "terminal_canary_target_added", rolloutId, targetId: form.targetId.trim() });
      setTargetForms((current) => ({ ...current, [rolloutId]: EMPTY_TARGET_FORM }));
      reload();
    } catch (addError) {
      setGlobalError(addError instanceof Error ? addError.message : "试点对象加入失败");
    }
  }

  /** 移出白名单（软删除，行保留可恢复）。注意方向：移出=收紧，该对象随即被灰度关闭。 */
  async function archiveTarget(target: CanaryTargetRow) {
    const reason = window.prompt("请填写移出白名单的理由（留痕必填）");
    if (reason === null || !reason.trim()) {
      return;
    }
    try {
      await api.post(`/api/v1/terminals/canary-targets/${target.id}/archive`, { reason: reason.trim() });
      setActionResult({ action: "terminal_canary_target_archived", id: target.id });
      reload();
    } catch (archiveError) {
      setGlobalError(archiveError instanceof Error ? archiveError.message : "试点对象移出失败");
    }
  }

  const masters = rows.filter((row) => row.capabilityCode === "terminal.enabled");
  const grouped = new Map<string, SwitchRow[]>();
  for (const row of rows.filter((r) => r.capabilityCode !== "terminal.enabled")) {
    const list = grouped.get(row.terminalType) ?? [];
    list.push(row);
    grouped.set(row.terminalType, list);
  }

  return (
    <section data-testid="terminal-switch-matrix-page">
      <AdminPageHeader
        title="终端开关矩阵"
        description="十类终端的总开关、各能力开关与灰度试点一页统管。开关优先级：全局 > 综合体 > 商户 > 业务系统 > 模块 > 版本/设备 > 灰度，高层级关闭压倒低层级打开——任何一层关闭，最终就是关闭。开关关闭后端或模块不能用，打开后仍要看员工权限、商户范围和设备状态。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {/* 标题层级：页 h1 之下用 h2，杜绝 h1→h3 跳级。 */}
        <h2>十端总开关</h2>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="terminal-master-switch-table">
            <thead>
              <tr>
                <th>终端</th>
                <th>状态</th>
                <th>说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {masters.map((row) => (
                <tr key={row.id} data-testid={`terminal-master-${row.terminalType}`}>
                  <td>{TERMINAL_LABELS[row.terminalType] ?? row.terminalType}</td>
                  <td data-testid={`terminal-master-status-${row.terminalType}`}>{row.enabled ? "开" : "关"}</td>
                  <td>{row.reason ?? "—"}</td>
                  <td>
                    <PermissionButton
                      permissionsState={effectivePermissions}
                      permissions={MANAGE}
                      data-testid={`terminal-master-toggle-${row.terminalType}`}
                      onClick={() => void toggle(row)}
                    >
                      {row.enabled ? "关闭该端" : "打开该端"}
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h2>各端能力开关</h2>
        {[...grouped.entries()].map(([terminalType, list]) => (
          <div key={terminalType} data-testid={`terminal-capability-group-${terminalType}`}>
            <h3>{TERMINAL_LABELS[terminalType] ?? terminalType}</h3>
            <div className="admin-table-scroll">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>能力</th>
                    <th>范围</th>
                    <th>状态</th>
                    <th>说明</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((row) => (
                    <tr key={row.id}>
                      <td>{row.capabilityCode}</td>
                      <td>{row.scopeType === "global" ? "全局" : `${row.scopeType}:${row.scopeId}`}</td>
                      <td>{row.enabled ? "开" : "关"}</td>
                      <td>{row.reason ?? "—"}</td>
                      <td>
                        <PermissionButton
                          permissionsState={effectivePermissions}
                          permissions={MANAGE}
                          data-testid={`terminal-capability-toggle-${row.id}`}
                          onClick={() => void toggle(row)}
                        >
                          {row.enabled ? "关闭" : "打开"}
                        </PermissionButton>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}

        {/* R-10 灰度层：求值链最细的一档，只能收窄不能放宽。 */}
        <h2>灰度试点（最细一档）</h2>
        <p>
          灰度只对少数试点对象先开放，确认没问题再逐步全面放开。某「端 × 能力」的灰度试点打开后：
          白名单内的对象照常使用，<strong>白名单外一律关闭</strong>；上面任何一层（全局、综合体、商户、业务系统、模块、版本/设备）
          关闭时，灰度白名单也<strong>不能</strong>把它重新打开。灰度试点关闭＝不做灰度，行为回到普通开关。
        </p>
        <div className="admin-form-row" data-testid="terminal-canary-rollout-form">
          <label>
            终端
            <select
              value={rolloutForm.terminalType}
              data-testid="terminal-canary-terminal-type"
              onChange={(event) => setRolloutForm({ ...rolloutForm, terminalType: event.target.value })}
            >
              {Object.entries(TERMINAL_LABELS)
                .filter(([key]) => key !== "global" && key !== "integration")
                .map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
            </select>
          </label>
          <label>
            能力码
            <input
              value={rolloutForm.capabilityCode}
              data-testid="terminal-canary-capability-code"
              onChange={(event) => setRolloutForm({ ...rolloutForm, capabilityCode: event.target.value })}
            />
          </label>
          <label>
            理由
            <input
              value={rolloutForm.reason}
              data-testid="terminal-canary-rollout-reason"
              onChange={(event) => setRolloutForm({ ...rolloutForm, reason: event.target.value })}
            />
          </label>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={MANAGE}
            data-testid="terminal-canary-rollout-open"
            onClick={() => void saveRollout(rolloutForm.terminalType, rolloutForm.capabilityCode, true, rolloutForm.reason)}
          >
            开启灰度试点
          </PermissionButton>
        </div>

        {canary.length === 0 ? (
          <p data-testid="terminal-canary-empty">暂无灰度试点：所有能力都按普通开关求值。</p>
        ) : (
          canary.map((view) => (
            <div key={view.rollout.id} data-testid={`terminal-canary-rollout-${view.rollout.id}`}>
              <h3>
                {TERMINAL_LABELS[view.rollout.terminalType] ?? view.rollout.terminalType} / {view.rollout.capabilityCode}
                （{view.rollout.active ? "试点中" : "未启用"}）
              </h3>
              <p>{view.rollout.reason ?? "—"}</p>
              <PermissionButton
                permissionsState={effectivePermissions}
                permissions={MANAGE}
                data-testid={`terminal-canary-toggle-${view.rollout.id}`}
                onClick={() =>
                  void saveRollout(
                    view.rollout.terminalType,
                    view.rollout.capabilityCode,
                    !view.rollout.active,
                    view.rollout.active ? "后台结束灰度试点" : "后台开启灰度试点",
                  )
                }
              >
                {view.rollout.active ? "结束灰度试点" : "开启灰度试点"}
              </PermissionButton>
              <div className="admin-table-scroll">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>试点维度</th>
                      <th>试点对象</th>
                      <th>理由</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {view.targets.length === 0 ? (
                      <tr>
                        <td colSpan={4}>白名单为空：试点中该端该能力对所有对象都是关闭的。</td>
                      </tr>
                    ) : (
                      view.targets.map((target) => (
                        <tr key={target.id} data-testid={`terminal-canary-target-${target.id}`}>
                          <td>{TARGET_TYPE_LABELS[target.targetType] ?? target.targetType}</td>
                          <td>{target.targetId}</td>
                          <td>{target.reason ?? "—"}</td>
                          <td>
                            <PermissionButton
                              permissionsState={effectivePermissions}
                              permissions={MANAGE}
                              data-testid={`terminal-canary-target-archive-${target.id}`}
                              onClick={() => void archiveTarget(target)}
                            >
                              移出白名单
                            </PermissionButton>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
              <div className="admin-form-row" data-testid={`terminal-canary-target-form-${view.rollout.id}`}>
                <label>
                  试点维度
                  <select
                    value={targetForm(view.rollout.id).targetType}
                    data-testid={`terminal-canary-target-type-${view.rollout.id}`}
                    onChange={(event) => setTargetForm(view.rollout.id, { targetType: event.target.value })}
                  >
                    {Object.entries(TARGET_TYPE_LABELS).map(([key, label]) => (
                      <option key={key} value={key}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  试点对象
                  <input
                    value={targetForm(view.rollout.id).targetId}
                    data-testid={`terminal-canary-target-id-${view.rollout.id}`}
                    onChange={(event) => setTargetForm(view.rollout.id, { targetId: event.target.value })}
                  />
                </label>
                <label>
                  理由
                  <input
                    value={targetForm(view.rollout.id).reason}
                    data-testid={`terminal-canary-target-reason-${view.rollout.id}`}
                    onChange={(event) => setTargetForm(view.rollout.id, { reason: event.target.value })}
                  />
                </label>
                <PermissionButton
                  permissionsState={effectivePermissions}
                  permissions={MANAGE}
                  data-testid={`terminal-canary-target-add-${view.rollout.id}`}
                  onClick={() => void addTarget(view.rollout.id)}
                >
                  加入白名单
                </PermissionButton>
              </div>
            </div>
          ))
        )}
      </AdminDataState>
    </section>
  );
}
