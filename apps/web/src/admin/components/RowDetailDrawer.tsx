import React from "react";

// 整行可点 → 行明细抽屉（全站样板，条目5「数据可溯源、全链可点击」）：
// 表格每行整行可点，弹出该行完整字段，让行内每个数字/枚举/日期都能溯源到源单。
// 强制零英文（业务HTML新规）：抽屉只负责渲染，一切中文化在调用方构造 fields 时完成——
// 枚举经 formatBusinessStatus、日期经 cnDate、金额带 ¥、引用经 TraceLink 传入 value。

export type RowDetailField = {
  /** 中文字段名（复用现有表头文案）。 */
  label: string;
  /** 已格式化的中文值 / TraceLink 节点 / 机读标识（单号/ID 原样但 label 中文）。 */
  value: React.ReactNode;
};

type Props = {
  open: boolean;
  title: string;
  fields: RowDetailField[];
  onClose: () => void;
};

/**
 * ISO 日期/时间 → 中文展示（禁止 mm/dd/yyyy 或 ISO 原文直出给管理层）：
 * "2026-07-05" → "2026年7月5日"；"2026-07-05T13:20:00Z" → "2026年7月5日 13:20"。
 * 非日期串原样返回（调用方保证已是中文/机读标识）。
 */
export function cnDate(value?: string | null): string {
  if (!value) return "—";
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2}))?/.exec(value);
  if (!m) return value;
  const [, y, mo, d, hh, mm] = m;
  const day = `${y}年${Number(mo)}月${Number(d)}日`;
  return hh && mm ? `${day} ${hh}:${mm}` : day;
}

/**
 * 右侧行明细抽屉。基础无障碍：role=dialog + aria-modal，Esc 关闭，点遮罩关闭，
 * 打开时焦点落到关闭按钮（焦点可达）。样式见 styles.css .admin-drawer*。
 */
export function RowDetailDrawer({ open, title, fields, onClose }: Props) {
  const closeRef = React.useRef<HTMLButtonElement>(null);

  React.useEffect(() => {
    if (!open) return;
    closeRef.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="admin-drawer-overlay" data-testid="row-detail-overlay" onClick={onClose}>
      <aside
        className="admin-drawer"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        data-testid="row-detail-drawer"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="admin-drawer-head">
          <h3 className="admin-drawer-title" data-testid="row-detail-title">{title}</h3>
          <button type="button" ref={closeRef} className="admin-drawer-close" data-testid="row-detail-close" aria-label="关闭" onClick={onClose}>×</button>
        </header>
        <dl className="admin-drawer-fields">
          {fields.map((field, index) => (
            <div className="admin-drawer-field" key={`${field.label}-${index}`}>
              <dt className="admin-drawer-label">{field.label}</dt>
              <dd className="admin-drawer-value">{field.value}</dd>
            </div>
          ))}
        </dl>
      </aside>
    </div>
  );
}
