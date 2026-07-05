import React from "react";
import { cnDate } from "./RowDetailDrawer";

// 自定义中文日期选择器：原生 <input type="date">/type="month" 的显示格式由浏览器 locale 决定
// （mm/dd/yyyy、July 2026），HTML 改不动 → 自研零依赖组件替换，落实「所有终端零英文」。
// 对外契约与原生一致：value=YYYY-MM-DD（月模式 YYYY-MM），onChange 回传同格式串，调用点最小改动。
// 样式见 styles.css .cn-date-*，复用 --adm 变量。

type Mode = "date" | "month";

type Props = {
  value: string;
  onChange: (value: string) => void;
  mode?: Mode;
  disabled?: boolean;
  placeholder?: string;
  "data-testid"?: string;
  "aria-label"?: string;
  /**
   * 视图锚点（YYYY-MM-DD）。仅用于「无 value 时日历默认打开到哪个月」。
   * 不传且 value 为空时打开到 FALLBACK_VIEW 且不高亮今天——
   * 刻意不在组件内读环境时钟（禁用 Date.now()/无参 new Date()，保证可测/确定）。
   */
  today?: string;
};

const WEEKDAYS = ["日", "一", "二", "三", "四", "五", "六"];
const MONTHS = ["1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"];
// ponytail: 无 value 且无 today 时的兜底视图年月，仅决定日历初始翻到哪一页，不参与取值。
const FALLBACK_VIEW = { year: 2025, month: 1 };

/** 从 YYYY-MM(-DD) 串取 {year, month, day}，非法返回 null。纯字符串解析，不构造 Date。 */
function parseParts(value?: string): { year: number; month: number; day: number | null } | null {
  if (!value) return null;
  const m = /^(\d{4})-(\d{2})(?:-(\d{2}))?/.exec(value);
  if (!m) return null;
  return { year: Number(m[1]), month: Number(m[2]), day: m[3] ? Number(m[3]) : null };
}

const pad = (n: number) => String(n).padStart(2, "0");

/** 某年月的天数。用显式数字构造 UTC Date（非无参构造，确定性）取「下月0日」。 */
function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/** 某年月1号是星期几(0=日)。显式数字构造，无环境时钟。 */
function firstWeekday(year: number, month: number): number {
  return new Date(Date.UTC(year, month - 1, 1)).getUTCDay();
}

function displayText(value: string, mode: Mode, placeholder: string): string {
  const parts = parseParts(value);
  if (!parts) return placeholder;
  if (mode === "month") return `${parts.year}年${parts.month}月`;
  return cnDate(value); // 复用既有中文格式化：2026年7月5日
}

export function CnDatePicker({
  value,
  onChange,
  mode = "date",
  disabled,
  placeholder,
  today,
  ...rest
}: Props) {
  const testId = rest["data-testid"];
  const ariaLabel = rest["aria-label"];
  const [open, setOpen] = React.useState(false);
  const rootRef = React.useRef<HTMLDivElement>(null);

  const anchor = parseParts(value) ?? parseParts(today) ?? { ...FALLBACK_VIEW, day: null };
  const [view, setView] = React.useState({ year: anchor.year, month: anchor.month });

  // 打开时把视图对齐到当前值/锚点。
  React.useEffect(() => {
    if (open) setView({ year: anchor.year, month: anchor.month });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 点外部关闭 + Esc 关闭。
  React.useEffect(() => {
    if (!open) return;
    const onDocClick = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const selected = parseParts(value);
  const todayParts = parseParts(today);

  const shiftMonth = (delta: number) => {
    setView((v) => {
      const total = v.year * 12 + (v.month - 1) + delta;
      return { year: Math.floor(total / 12), month: (total % 12) + 1 };
    });
  };

  const pickDay = (day: number) => {
    onChange(`${view.year}-${pad(view.month)}-${pad(day)}`);
    setOpen(false);
  };

  const pickMonth = (month: number) => {
    onChange(`${view.year}-${pad(month)}`);
    setOpen(false);
  };

  const toggle = () => {
    if (!disabled) setOpen((o) => !o);
  };

  const onTriggerKey = (e: React.KeyboardEvent) => {
    if (disabled) return;
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      setOpen((o) => !o);
    }
  };

  const text = displayText(value, mode, placeholder ?? (mode === "month" ? "选择月份" : "选择日期"));
  const isPlaceholder = !parseParts(value);

  return (
    <div className="cn-date" ref={rootRef}>
      <button
        type="button"
        className="cn-date-trigger"
        data-testid={testId}
        aria-label={ariaLabel}
        aria-haspopup="dialog"
        aria-expanded={open}
        disabled={disabled}
        onClick={toggle}
        onKeyDown={onTriggerKey}
      >
        <span className={isPlaceholder ? "cn-date-placeholder" : undefined}>{text}</span>
        <span className="cn-date-caret" aria-hidden="true">▾</span>
      </button>

      {open && (
        <div className="cn-date-pop" role="dialog" aria-label={ariaLabel ?? (mode === "month" ? "选择月份" : "选择日期")}>
          {mode === "month" ? (
            <>
              <div className="cn-date-head">
                <button type="button" className="cn-date-nav" aria-label="上一年" onClick={() => setView((v) => ({ ...v, year: v.year - 1 }))}>◀</button>
                <span className="cn-date-title">{view.year}年</span>
                <button type="button" className="cn-date-nav" aria-label="下一年" onClick={() => setView((v) => ({ ...v, year: v.year + 1 }))}>▶</button>
              </div>
              <div className="cn-date-months">
                {MONTHS.map((label, i) => {
                  const month = i + 1;
                  const isSel = selected?.year === view.year && selected?.month === month;
                  return (
                    <button
                      key={month}
                      type="button"
                      className={`cn-date-cell${isSel ? " is-selected" : ""}`}
                      aria-pressed={isSel}
                      onClick={() => pickMonth(month)}
                    >
                      {label}
                    </button>
                  );
                })}
              </div>
            </>
          ) : (
            <>
              <div className="cn-date-head">
                <button type="button" className="cn-date-nav" aria-label="上个月" onClick={() => shiftMonth(-1)}>◀</button>
                <span className="cn-date-title">{view.year}年{view.month}月</span>
                <button type="button" className="cn-date-nav" aria-label="下个月" onClick={() => shiftMonth(1)}>▶</button>
              </div>
              <div className="cn-date-week">
                {WEEKDAYS.map((w) => (
                  <span key={w} className="cn-date-wd">{w}</span>
                ))}
              </div>
              <div className="cn-date-grid">
                {Array.from({ length: firstWeekday(view.year, view.month) }).map((_, i) => (
                  <span key={`blank-${i}`} className="cn-date-blank" />
                ))}
                {Array.from({ length: daysInMonth(view.year, view.month) }).map((_, i) => {
                  const day = i + 1;
                  const isSel = selected?.year === view.year && selected?.month === view.month && selected?.day === day;
                  const isToday = todayParts?.year === view.year && todayParts?.month === view.month && todayParts?.day === day;
                  return (
                    <button
                      key={day}
                      type="button"
                      className={`cn-date-cell${isSel ? " is-selected" : ""}${isToday ? " is-today" : ""}`}
                      aria-pressed={isSel}
                      onClick={() => pickDay(day)}
                    >
                      {day}
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
