import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type NotificationWindow = { windowKey: string; days: number; description?: string };

const MANAGE = ["platform.notification_window.manage"];

/**
 * 提醒窗天数配置页(ADM-ANA-011, V219):维护各提醒窗提前天数——
 * 续约提醒/存酒临期/招商跟进等,运营调窗口改配置不改代码;仅 platform.notification_window.manage 可改。
 */
export function NotificationWindowsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<NotificationWindow[]>([]);
  const [edits, setEdits] = React.useState<Record<string, string>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<NotificationWindow[]>("/api/v1/analytics/notification-windows");
      setRows(items ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "提醒窗配置读取失败" },
  );

  async function saveWindow(windowKey: string) {
    const raw = edits[windowKey];
    const days = Number(raw);
    if (raw === undefined || !Number.isInteger(days) || days <= 0) {
      setGlobalError("提醒提前天数必须是大于 0 的整数");
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
        title="提醒窗配置"
        description="维护各类提醒的提前天数(续约提醒/存酒临期/招商跟进等)。调整提醒时机改配置不改代码;仅平台管理员可改。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="notification-windows-table">
            <thead>
              <tr><th>提醒项</th><th>当前提前(天)</th><th>改为</th><th>操作</th></tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.windowKey} data-testid={`notification-window-${row.windowKey}`}>
                  <td>{row.description ?? row.windowKey}</td>
                  <td>{row.days}</td>
                  <td>
                    <input
                      aria-label={`${row.windowKey} 提前天数`}
                      type="number"
                      min="1"
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
    </section>
  );
}
