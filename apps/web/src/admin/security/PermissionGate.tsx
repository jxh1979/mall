import React from "react";
import type { EffectivePermissionsState } from "../types";

// DENY 优先 + wildcard + fail-safe
export function hasPermissions(
  state: EffectivePermissionsState,
  required: string[],
): boolean {
  if (state.status !== "ready") return false; // loading/error → fail-safe 隐藏
  const { grants, denies, wildcard } = state.value;
  if (required.some((permission) => denies.includes(permission))) return false; // DENY 优先（含 wildcard 也拒）
  if (wildcard) return true;
  return required.every((permission) => grants.includes(permission));
}

export function PermissionGate({
  permissionsState,
  permissions,
  children,
}: {
  permissionsState: EffectivePermissionsState;
  permissions: string[];
  children: React.ReactNode;
}) {
  if (permissionsState.status === "loading") {
    return (
      <div className="admin-loading" data-testid="admin-permission-loading">
        权限加载中…
      </div>
    );
  }
  if (!hasPermissions(permissionsState, permissions)) {
    return (
      <div className="admin-denied" data-testid="admin-permission-denied">
        权限不足：当前账号不能访问该后台页面。
      </div>
    );
  }
  return <>{children}</>;
}
