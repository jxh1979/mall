import React from "react";
import type { EffectivePermissionsState } from "../types";
import { useCan } from "../security/Can";

export type HighRiskConfirmProps = {
  /** 按钮文案，同时作为确认提示中的动作名 */
  label: string;
  /** 影响说明：本次操作会带来的后果 */
  impact: string;
  /** 确认后才会执行的高危写动作 */
  onConfirm: () => void;
  /** 点击时的前置校验：返回 false 则不弹确认框、不发请求（用于表单校验失败时） */
  precheck?: () => boolean;
  /** 禁用按钮（例如正在提交时） */
  disabled?: boolean;
  /** 稳定选择器 */
  "data-testid"?: string;
  /** 是否提示不可逆，默认 true */
  irreversible?: boolean;
  /** 按钮样式类，默认复用 admin-secondary-button */
  className?: string;
  /**
   * 按钮级权限门控（可选）。传入后复用 useCan：无权限时按钮禁用且点击直接拦截，
   * 不弹确认框、不发请求（fail-safe）。与 disabled 合并。
   */
  permissionsState?: EffectivePermissionsState;
  /** 该动作所需权限码（与后端 requirePermission 对齐）。 */
  permissions?: string[];
  /** 无权限时的提示，默认“当前账号无此操作权限”。 */
  deniedHint?: string;
};

const IRREVERSIBLE_HINT = "此操作不可逆，请确认证据齐备后再继续。";

/**
 * 高危写动作二次确认按钮。
 *
 * 点击后弹出二次确认（window.confirm），提示动作名、影响和不可逆说明；
 * 仅当用户确认后才调用 onConfirm 发起请求，取消则不发任何请求。
 */
export function HighRiskConfirm({
  label,
  impact,
  onConfirm,
  precheck,
  disabled = false,
  irreversible = true,
  className = "admin-secondary-button",
  permissionsState,
  permissions = [],
  deniedHint = "当前账号无此操作权限",
  ...rest
}: HighRiskConfirmProps) {
  const testId = rest["data-testid"];
  // 未传 permissionsState 时不做按钮级权限门控（保持既有调用方零行为变化）。
  const allowed = useCan(permissionsState ?? { status: "loading" }, permissions);
  const gated = permissionsState !== undefined;
  const denied = gated && !allowed;
  const blocked = disabled || denied;

  function handleClick() {
    if (denied) {
      return; // fail-safe：无权限直接拦截，不弹确认框、不发请求
    }
    if (precheck && !precheck()) {
      return;
    }
    const lines = [`确认执行高危操作：${label}？`, `影响：${impact}`];
    if (irreversible) {
      lines.push(IRREVERSIBLE_HINT);
    }
    if (window.confirm(lines.join("\n"))) {
      onConfirm();
    }
  }

  return (
    <button
      type="button"
      className={className}
      disabled={blocked}
      aria-disabled={blocked}
      title={denied ? deniedHint : undefined}
      data-testid={testId}
      onClick={handleClick}
    >
      {label}
    </button>
  );
}
