import { Tooltip } from "antd";
import type { ReactNode } from "react";

// 表格行内单动作用图标按钮 + 悬停提示（tooltip），比文字按钮更紧凑。
// 触摸友好：命中区 36×36（见 styles.css .admin-icon-btn）；aria-label 保留可访问性。
export function IconButton({
  icon,
  tip,
  onClick,
  danger = false,
  disabled = false,
  testId,
}: {
  icon: ReactNode;
  /** 悬停提示文案（同时作为 aria-label），务必用业务语言说明动作。 */
  tip: string;
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
  testId?: string;
}) {
  return (
    <Tooltip title={tip}>
      <button
        type="button"
        className={`admin-icon-btn${danger ? " danger" : ""}`}
        aria-label={tip}
        data-testid={testId}
        disabled={disabled}
        onClick={onClick}
      >
        {icon}
      </button>
    </Tooltip>
  );
}
