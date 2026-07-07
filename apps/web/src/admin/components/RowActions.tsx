import { Button, Dropdown } from "antd";
import { hasPermissions } from "../security/PermissionGate";
import type { EffectivePermissionsState } from "../types";

export type RowAction = {
  /** 唯一 key。 */
  key: string;
  /** 中文动作名。 */
  label: string;
  onClick: () => void;
  /** 权限码（可选）：无权限则该动作不出现。 */
  permissions?: string[];
  /** 危险动作（删除/驳回等），红色。 */
  danger?: boolean;
  /** 沿用原按钮 data-testid，保持 e2e/门禁兼容。 */
  testId?: string;
};

// 「操作▾」行内动作（2026-07-07 用户拍板：表格每行多个动作收进一个下拉，表格更清爽）。
// 规则：按权限过滤后——0 个=灰显「—」；1 个=直接小按钮（单动作不必套下拉）；2 个以上=「操作▾」下拉。
// 桌面后台为主；沿用 antd Button/Dropdown；各动作 data-testid 原样保留。
export function RowActions({ actions, permissionsState, testId, triggerLabel = "操作" }: {
  actions: RowAction[];
  permissionsState: EffectivePermissionsState;
  /** 触发器 data-testid（下拉时用）。 */
  testId: string;
  triggerLabel?: string;
}) {
  const allowed = actions.filter((a) => !a.permissions || hasPermissions(permissionsState, a.permissions));
  if (allowed.length === 0) {
    return <span className="admin-muted-action" data-testid={`${testId}-none`}>—</span>;
  }
  if (allowed.length === 1) {
    const a = allowed[0]!;
    return (
      <Button size="small" danger={a.danger} data-testid={a.testId} onClick={a.onClick}>{a.label}</Button>
    );
  }
  const items = allowed.map((a) => ({
    key: a.key,
    danger: a.danger,
    label: <span data-testid={a.testId}>{a.label}</span>,
  }));
  return (
    <Dropdown
      trigger={["click"]}
      menu={{ items, onClick: ({ key }) => allowed.find((a) => a.key === key)?.onClick() }}
    >
      <Button size="small" data-testid={testId}>{triggerLabel} ▾</Button>
    </Dropdown>
  );
}
