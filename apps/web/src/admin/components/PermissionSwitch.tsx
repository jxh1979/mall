import { Switch } from "antd";
import { useCan } from "../security/Can";
import type { EffectivePermissionsState } from "../types";

// 布尔开关设置（第三方开关 / 通知开关等）用 antd Switch 替代「开启/关闭」按钮：
// 直观显示当前态 + 点击即切换；无权限则禁用（fail-safe，不触发 onChange）。
export function PermissionSwitch({
  permissionsState,
  permissions,
  checked,
  onChange,
  disabled = false,
  loading = false,
  testId,
  deniedHint = "当前账号无此操作权限",
}: {
  permissionsState: EffectivePermissionsState;
  /** 该开关所需权限码（与后端 requirePermission 对齐）。 */
  permissions: string[];
  checked: boolean;
  /** 切换回调（切到相反态）。 */
  onChange: () => void;
  disabled?: boolean;
  loading?: boolean;
  testId?: string;
  deniedHint?: string;
}) {
  const allowed = useCan(permissionsState, permissions);
  const blocked = disabled || !allowed;
  return (
    <Switch
      checked={checked}
      disabled={blocked}
      loading={loading}
      data-testid={testId}
      title={!allowed ? deniedHint : undefined}
      onChange={() => {
        if (!allowed) return; // fail-safe：无权限直接拦截
        onChange();
      }}
    />
  );
}
