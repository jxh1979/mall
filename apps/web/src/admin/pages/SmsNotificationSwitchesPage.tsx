// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionSwitch } from "../components/PermissionSwitch";

type SwitchRow = { id: string; terminalType: string; capabilityCode: string; scopeId: string | null; enabled: boolean };

// §6 业务短信通知 两级开关目录（总开关 + 域 + 事件）。mandatory：BSM 第 1598 行不可关闭的三类（OBS-13-04），后端拒绝关闭、发送不看开关。
const SMS_CATALOG: Record<string, { label: string; level: string; mandatory?: boolean }> = {
  "sms_notification": { label: "短信通知总开关", level: "总" },
  "sms.notify.stored_wine": { label: "存酒（域开关）", level: "域" },
  "sms.notify.stored_wine.deposit": { label: "存酒成功", level: "事件" },
  "sms.notify.stored_wine.claim": { label: "取酒成功", level: "事件" },
  "sms.notify.stored_wine.expiring": { label: "存酒即将到期", level: "事件", mandatory: true },
  "sms.notify.stored_wine.expired": { label: "存酒已过期", level: "事件" },
  "sms.notify.stored_wine.exit_notice": { label: "酒吧退出存酒告知", level: "事件" },
  "sms.notify.stored_wine.handover": { label: "存酒移交平台保管告知", level: "事件" },
  "sms.notify.stored_value": { label: "储值（域开关）", level: "域" },
  "sms.notify.stored_value.topup": { label: "充值到账", level: "事件", mandatory: true },
  "sms.notify.stored_value.consume": { label: "消费扣款", level: "事件", mandatory: true },
  "sms.notify.stored_value.pending_refund": { label: "储值待退款提醒", level: "事件" },
  "sms.notify.merchant_fee": { label: "商户费用（域开关）", level: "域" },
  "sms.notify.merchant_fee.rent": { label: "房租账单", level: "事件", mandatory: true },
  "sms.notify.merchant_fee.utilities": { label: "水电费账单", level: "事件", mandatory: true },
  "sms.notify.merchant_fee.property": { label: "物业费账单", level: "事件", mandatory: true },
  "sms.notify.merchant_fee.rent_dunning": { label: "房租催缴", level: "事件" },
  "sms.notify.merchant_fee.utilities_dunning": { label: "水电费催缴", level: "事件" },
  "sms.notify.merchant_fee.property_dunning": { label: "物业费催缴", level: "事件" },
  "sms.notify.merchant_fee.renewal": { label: "合同续约提醒", level: "事件" },
  // 第十四批 OBS-13-03（BSM 第 1596 行给客人的订台类短信）。
  "sms.notify.bar_reservation": { label: "订台（域开关）", level: "域" },
  "sms.notify.bar_reservation.confirmed": { label: "订台成功确认", level: "事件" },
  "sms.notify.bar_reservation.arrival_reminder": { label: "最晚到店前提醒", level: "事件" },
  "sms.notify.bar_reservation.timeout_released": { label: "超时释放台位", level: "事件" },
  "sms.notify.bar_reservation.cancelled": { label: "订台取消确认", level: "事件" },
  "sms.notify.bar_reservation.deposit_forfeited": { label: "定金没收", level: "事件", mandatory: true },
  "sms.notify.bar_reservation.deposit_refunded": { label: "定金原路退回", level: "事件", mandatory: true },
  // 第十八批第 6 条（PGM 第 742 行缺行即关）：已接触发点、V554 补了显式开启行的域与事件。
  "sms.notify.service": { label: "服务单（域开关）", level: "域" },
  "sms.notify.service.catalog_fee_changed": { label: "服务项目管理费变更", level: "事件" },
  "sms.notify.facility": { label: "设施（域开关）", level: "域" },
  "sms.notify.facility.alarm_takeover": { label: "设施告警接管", level: "事件" },
  "sms.notify.ticketing": { label: "票务（域开关）", level: "域" },
  "sms.notify.ticketing.ticket_transfer_created": { label: "门票转赠领取", level: "事件" },
  "sms.notify.voucher": { label: "券（域开关）", level: "域" },
  "sms.notify.voucher.voucher_transfer_created": { label: "券转赠领取", level: "事件" },
  "sms.notify.bar": { label: "酒吧（域开关）", level: "域" },
  "sms.notify.bar.pending_clear_overdue": { label: "待清台超时提醒", level: "事件" },
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
        description="两级可配：总开关 + 各域（存酒/储值/商户费用/订台/服务单/设施/票务/券/酒吧）+ 各事件，管理员可决定哪些提醒经第三方短信平台发短信。总开关默认关；触发点已接，开关控制是否实际发送。涉及商户应收应付金额变化、涉及客人金额变化的通知与存酒到期提醒不可关闭，不受总开关与域开关影响。储值不含积分。"
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
                    <td data-testid={`sms-switch-state-${row.capabilityCode}`}>
                      {entry?.mandatory ? "开启（不可关闭）" : row.enabled ? "开启" : "关闭"}
                    </td>
                    <td>
                      {entry?.mandatory ? "—" : (
                        <PermissionSwitch permissionsState={effectivePermissions} permissions={MANAGE}
                          checked={row.enabled} onChange={() => void toggle(row)} testId={`sms-switch-toggle-${row.capabilityCode}`} />
                      )}
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
