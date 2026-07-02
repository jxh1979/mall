import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

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

const MANAGE = ["terminal.switch.manage"];

const TERMINAL_LABELS: Record<string, string> = {
  app: "客人 App",
  miniapp: "小程序",
  web: "Web 前端",
  tablet: "工作人员平板",
  staff_work_phone: "员工工作手机",
  staff_private_phone: "员工私人手机",
  merchant_portal: "商户门户",
  merchant_app: "商户 App",
  kiosk: "触摸屏一体机",
  mobile_admin: "移动管理端",
  global: "全局能力",
  integration: "第三方集成",
};

function toRows(data: unknown): SwitchRow[] {
  if (Array.isArray(data)) return data as SwitchRow[];
  const items = (data as { items?: SwitchRow[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

export function TerminalSwitchMatrixPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<SwitchRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const data = await api.get<unknown>("/api/v1/terminals/capability-switches");
      setRows(toRows(data));
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
        description="十类终端的总开关与各能力开关一页统管：全局>综合体>商户>模块>设备逐级压制，高层级关闭压倒低层级打开；开关关闭后端或模块不能用，打开后仍要看员工权限、商户范围和设备状态。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <h3>十端总开关</h3>
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

        <h3>各端能力开关</h3>
        {[...grouped.entries()].map(([terminalType, list]) => (
          <div key={terminalType} data-testid={`terminal-capability-group-${terminalType}`}>
            <h4>{TERMINAL_LABELS[terminalType] ?? terminalType}</h4>
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
      </AdminDataState>
    </section>
  );
}
