import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type NotificationWindow = { windowKey: string; days: number; description?: string };

const MANAGE = ["platform.notification_window.manage"];

/**
 * 提醒与留存天数配置页(ADM-ANA-011, V219/V353):维护「按天计」的运营参数——
 * 提醒类(续约提醒/存酒临期/招商跟进)与留存类(游乐票人脸照留存天数,V353);
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
        description="维护按天计的运营参数:提醒类(续约提醒/存酒临期/招商跟进)的提前天数,与留存类(游乐票人脸照)的留存天数。改配置不改代码;仅平台管理员可改,每次改动留审计。人脸照留存天数涉个人信息保护——调小=照片提前销毁核销无据可对,调大=生物识别信息超期留存,请按合规口径设定。"
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
    </section>
  );
}
