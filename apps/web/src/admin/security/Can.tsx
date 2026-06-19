import React from "react";
import type { EffectivePermissionsState } from "../types";
import { hasPermissions } from "./PermissionGate";

/**
 * 按钮级权限判定。复用 PermissionGate 的 hasPermissions：DENY 优先、wildcard、
 * loading/error 一律 fail-safe 拒绝。permissions 为空数组时视为无需权限（放行）。
 *
 * 与页面级 PermissionGate 区别：Gate 控制整页可见性，useCan/Can/PermissionButton
 * 控制单个写动作按钮是否可用，弥补此前“按钮级权限 0 实现”的缺口。
 */
export function useCan(
  permissionsState: EffectivePermissionsState,
  permissions: string[],
): boolean {
  return React.useMemo(
    () => hasPermissions(permissionsState, permissions),
    [permissionsState, permissions],
  );
}

export function Can({
  permissionsState,
  permissions,
  fallback = null,
  children,
}: {
  permissionsState: EffectivePermissionsState;
  permissions: string[];
  /** 无权限时渲染的内容，默认隐藏。 */
  fallback?: React.ReactNode;
  children: React.ReactNode;
}) {
  const allowed = useCan(permissionsState, permissions);
  return <>{allowed ? children : fallback}</>;
}

export type PermissionButtonProps = {
  permissionsState: EffectivePermissionsState;
  /** 该写动作所需权限码（与后端 requirePermission 对齐）。 */
  permissions: string[];
  onClick: () => void;
  children: React.ReactNode;
  /** 其它业务态禁用（如正在提交）。无权限会与之合并。 */
  disabled?: boolean;
  className?: string;
  "data-testid"?: string;
  /** 无权限时按钮上的 title 提示。 */
  deniedHint?: string;
};

/**
 * 权限受控按钮：无权限时禁用并拦截 onClick（fail-safe，不止视觉禁用）。
 * 用于高风险写动作，确保未授权账号即使看到按钮也无法触发请求。
 */
export function PermissionButton({
  permissionsState,
  permissions,
  onClick,
  children,
  disabled = false,
  className,
  deniedHint = "当前账号无此操作权限",
  ...rest
}: PermissionButtonProps) {
  const allowed = useCan(permissionsState, permissions);
  const blocked = disabled || !allowed;
  return (
    <button
      type="button"
      className={className}
      disabled={blocked}
      aria-disabled={blocked}
      title={!allowed ? deniedHint : undefined}
      data-testid={rest["data-testid"]}
      onClick={() => {
        if (!allowed) return; // fail-safe：无权限直接拦截，不发请求
        onClick();
      }}
    >
      {children}
    </button>
  );
}
