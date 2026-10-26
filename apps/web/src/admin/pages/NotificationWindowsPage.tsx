// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";

type NotificationWindow = { windowKey: string; days: number; description?: string };

const MANAGE = ["platform.notification_window.manage"];
const REMINDER_MANAGE = ["bar.saved_bottle.reminder_policy.manage"];
const REMINDER_PATH = "/api/v1/inventory/saved-bottles/reminder-policy";
type ReminderPolicyRow = { scopeId: string; reminderDays: number[] };
type SectionProps = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions">;

/**
 * 存酒到期提醒节点（BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，出厂默认到期前第 14 天、第 7 天、
 * 第 3 天、第 1 天）」）：列出出厂行与各项目行，按项目改节点；没单独配置的项目沿用出厂行。
 */
function SavedBottleReminderSection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [rows, setRows] = React.useState<ReminderPolicyRow[]>([]);
  const [projectId, setProjectId] = React.useState("");
  const [days, setDays] = React.useState("");

  const load = React.useCallback(async () => {
    try {
      setRows((await api.get<ReminderPolicyRow[]>(REMINDER_PATH)) ?? []);
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "存酒到期提醒节点读取失败");
    }
  }, [api, setGlobalError]);
  React.useEffect(() => { void load(); }, [load]);

  async function save() {
    const list = days.split(/[,，、\s]+/).filter(Boolean).map(Number);
    if (!projectId.trim()) {
      setGlobalError("请先填写综合体项目 id");
      return;
    }
    if (list.length === 0 || list.some((d) => !Number.isInteger(d) || d < 1) || new Set(list).size !== list.length) {
      setGlobalError("提醒节点须为到期前第 N 天（N 为不小于 1 的整数、不重复），至少一个，用逗号分隔");
      return;
    }
    try {
      const saved = await api.put(REMINDER_PATH, { idempotencyKey: `saved-bottle-reminder-${Date.now()}`, scopeId: `project:${projectId.trim()}`, reminderDays: list });
      setActionResult({ action: "saved_bottle_reminder_policy_saved", result: saved });
      setDays("");
      void load();
    } catch (err) {
      setGlobalError(err instanceof Error ? err.message : "存酒到期提醒节点保存失败");
    }
  }

  return (
    <div className="admin-preview" data-testid="saved-bottle-reminder-policy">
      <strong>存酒到期提醒节点（按项目；出厂到期前第 14、7、3、1 天）：</strong>
      到期前按节点各发一次短信提醒，超出本次存酒实际有效期的节点自动跳过；没单独配置的项目沿用出厂节点。
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="saved-bottle-reminder-policy-table">
          <thead><tr><th>范围</th><th>提醒节点</th></tr></thead>
          <tbody>{rows.map((row) => (
            <tr key={row.scopeId}>
              <td>{row.scopeId === "global" ? "出厂默认（未单独配置的项目）" : `项目 ${row.scopeId.replace(/^project:/, "")}`}</td>
              <td>到期前第 {row.reminderDays.join("、")} 天</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input aria-label="存酒提醒项目 id" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <label>提醒节点（到期前第 N 天，逗号分隔）<input aria-label="存酒提醒节点" placeholder="14,7,3,1" value={days} onChange={(e) => setDays(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={REMINDER_MANAGE}
          data-testid="saved-bottle-reminder-policy-save" onClick={() => void save()}>保存提醒节点</PermissionButton>
      </div>
    </div>
  );
}

/**
 * 提醒与留存天数配置页(ADM-ANA-011, V219/V353):维护「按天计」的运营参数——
 * 提醒类(续约提醒/招商跟进)等(存酒到期提醒改按项目提醒节点配置,V548,本页下方「存酒到期提醒节点」区);游乐票人脸照留存天数已由 V525 撤销(TK-06:人脸在该票用完或到期后删除,不再多留 N 天);
 * 改配置不改代码;仅 platform.notification_window.manage 可改,每次改动写审计。
 */
export function NotificationWindowsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<NotificationWindow[]>([]);
  const [edits, setEdits] = React.useState<Record<string, string>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<NotificationWindow[]>("/api/v1/analytics/notification-windows");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "提醒与留存天数配置读取失败" },
  );

  async function saveWindow(windowKey: string) {
    const raw = edits[windowKey];
    const days = Number(raw);
    if (raw === undefined || !Number.isInteger(days) || days <= 0 || days > 3650) {
      setGlobalError("天数必须是 1 到 3650 之间的整数");
      return;
    }
    await api.post("/api/v1/analytics/notification-windows", { windowKey, days });
    setActionResult({ action: "notification_window_saved", windowKey, days });
    setEdits((prev) => ({ ...prev, [windowKey]: "" }));
    reload();
  }

  return (
    <section data-testid="notification-windows-page">
      <AdminPageHeader
        title="提醒与留存天数配置"
        description="维护按天计的运营参数:提醒类(续约提醒/招商跟进)的提前天数等。改配置不改代码;仅平台管理员可改,每次改动留审计。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="notification-windows-table">
            <thead>
              <tr><th>配置项</th><th>当前天数</th><th>改为</th><th>操作</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.windowKey} data-testid={`notification-window-${row.windowKey}`}>
                  <td>{row.description ?? row.windowKey}</td>
                  <td>{row.days}</td>
                  <td>
                    <input
                      aria-label={`${row.windowKey} 天数`}
                      type="number"
                      min="1"
                      max="3650"
                      value={edits[row.windowKey] ?? ""}
                      onChange={(e) => setEdits((prev) => ({ ...prev, [row.windowKey]: e.target.value }))}
                    />
                  </td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`notification-window-save-${row.windowKey}`} onClick={() => void saveWindow(row.windowKey)}>
                      保存
                    </PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      {hasPermissions(effectivePermissions, REMINDER_MANAGE) ? (
        <SavedBottleReminderSection api={api} setActionResult={setActionResult} setGlobalError={setGlobalError} effectivePermissions={effectivePermissions} />
      ) : null}
    </section>
  );
}
