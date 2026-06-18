import React from "react";

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
  ...rest
}: HighRiskConfirmProps) {
  const testId = rest["data-testid"];

  function handleClick() {
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
    <button type="button" className={className} disabled={disabled} data-testid={testId} onClick={handleClick}>
      {label}
    </button>
  );
}
