import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type SwitchRow = { id: string; terminalType: string; capabilityCode: string; scopeId: string | null; enabled: boolean };

// §6 业务短信通知 两级开关目录（总开关 + 域 + 事件）。
const SMS_CATALOG: Record<string, { label: string; level: string }> = {
  "sms.notify": { label: "短信通知总开关", level: "总" },
  "sms.notify.stored_wine": { label: "存酒（域开关）", level: "域" },
  "sms.notify.stored_wine.deposit": { label: "存酒成功", level: "事件" },
  "sms.notify.stored_wine.claim": { label: "取酒成功", level: "事件" },
  "sms.notify.stored_wine.expiring": { label: "存酒即将到期", level: "事件" },
  "sms.notify.stored_wine.expired": { label: "存酒已过期", level: "事件" },
  "sms.notify.stored_value": { label: "储值（域开关）", level: "域" },
  "sms.notify.stored_value.topup": { label: "充值到账", level: "事件" },
  "sms.notify.stored_value.consume": { label: "消费扣款", level: "事件" },
  "sms.notify.merchant_fee": { label: "商户费用（域开关）", level: "域" },
  "sms.notify.merchant_fee.rent": { label: "房租账单/催缴", level: "事件" },
  "sms.notify.merchant_fee.utilities": { label: "水电费账单/催缴", level: "事件" },
  "sms.notify.merchant_fee.property": { label: "物业费账单/催缴", level: "事件" },
  "sms.notify.merchant_fee.renewal": { label: "合同续约提醒", level: "事件" },
};
const MANAGE = ["terminal.switch.manage"];

/**
 * 业务短信通知开关配置页（ADM-INT-004，条目2 §6）：后台两级可配——总开关 + 各域 + 各事件，决定哪些业务事件发短信。
 * 复用终端能力开关机制（capability-switches）；总开关默认关。存酒/储值/商户费用/续约触发点已接，开关控制是否实际发送。
 */
export function SmsNotificationSwitchesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SwitchRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<SwitchRow[]>("/api/v1/terminals/capability-switches");
      setRows((items ?? []).filter((row) => SMS_CATALOG[row.capabilityCode] !== undefined));
    },
    { onError: setGlobalError, fallbackMessage: "短信通知开关读取失败" },
  );

  async function toggle(row: SwitchRow) {
    const updated = await api.put<SwitchRow>(`/api/v1/terminals/capability-switches/${row.id}`, {
      enabled: !row.enabled, terminalType: row.terminalType, capabilityCode: row.capabilityCode, scopeId: row.scopeId,
    });
    setRows((current) => current.map((item) => (item.id === row.id ? { ...item, enabled: updated.enabled } : item)));
    setActionResult({ action: "sms_switch_toggled", capabilityCode: row.capabilityCode, enabled: updated.enabled });
  }

  return (
    <section data-testid="sms-notification-switches-page">
      <AdminPageHeader
        title="短信通知开关"
        description="两级可配：总开关 + 各域（存酒/储值/商户费用）+ 各事件，决定哪些业务事件经第三方短信平台通知对方。总开关默认关；触发点已接，开关控制是否实际发送。储值不含积分。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="sms-switches-table">
            <thead><tr><th>通知项</th><th>层级</th><th>能力码</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {rows.map((row) => {
                const entry = SMS_CATALOG[row.capabilityCode];
                return (
                  <tr key={row.id} data-testid={`sms-switch-${row.capabilityCode}`}>
                    <td>{entry?.label ?? row.capabilityCode}</td>
                    <td>{entry?.level ?? "—"}</td>
                    <td><small>{row.capabilityCode}</small></td>
                    <td data-testid={`sms-switch-state-${row.capabilityCode}`}>{row.enabled ? "开启" : "关闭"}</td>
                    <td>
                      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
                        data-testid={`sms-switch-toggle-${row.capabilityCode}`} onClick={() => void toggle(row)}>
                        {row.enabled ? "关闭" : "开启"}
                      </PermissionButton>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
