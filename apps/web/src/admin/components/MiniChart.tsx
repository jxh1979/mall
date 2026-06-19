import React from "react";

/**
 * 轻量内联 SVG 趋势图：不引入 recharts 等图表依赖（离线无网），用纯 SVG/CSS
 * 渲染 sparkline 折线与迷你柱状图，替代看板上的纯文字趋势。
 *
 * - 输入空数组时返回 null（由调用方决定占位文案）。
 * - 数值统一做 min/max 归一化映射到视口高度，单点时居中绘制。
 * - 仅展示趋势形态，不承担精确读数；精确值仍由 KPI 卡片/表格给出。
 */

function toFinite(value: unknown): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function Sparkline({
  values,
  width = 120,
  height = 32,
  testid,
  ariaLabel,
}: {
  values: number[];
  width?: number;
  height?: number;
  testid?: string;
  ariaLabel?: string;
}): React.ReactElement | null {
  const points = values.map(toFinite);
  if (points.length === 0) return null;
  const max = Math.max(...points);
  const min = Math.min(...points);
  const span = max - min || 1;
  const pad = 2;
  const innerH = height - pad * 2;
  const step = points.length > 1 ? width / (points.length - 1) : 0;
  const coords = points.map((value, index) => {
    const x = points.length > 1 ? index * step : width / 2;
    const y = pad + innerH - ((value - min) / span) * innerH;
    return { x, y };
  });
  const path = coords.map((c, i) => `${i === 0 ? "M" : "L"}${c.x.toFixed(2)},${c.y.toFixed(2)}`).join(" ");
  const last = coords[coords.length - 1] ?? { x: width / 2, y: height / 2 };
  const first = points[0] ?? 0;
  const tail = points[points.length - 1] ?? first;
  const rising = tail >= first;
  const stroke = rising ? "#16a34a" : "#dc2626";
  return (
    <svg
      className="admin-sparkline"
      data-testid={testid}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      role="img"
      aria-label={ariaLabel ?? "趋势"}
    >
      <path d={path} fill="none" stroke={stroke} strokeWidth={1.5} strokeLinejoin="round" strokeLinecap="round" />
      <circle cx={last.x} cy={last.y} r={2.2} fill={stroke} />
    </svg>
  );
}

export function MiniBars({
  values,
  width = 120,
  height = 32,
  testid,
  ariaLabel,
}: {
  values: number[];
  width?: number;
  height?: number;
  testid?: string;
  ariaLabel?: string;
}): React.ReactElement | null {
  const points = values.map(toFinite);
  if (points.length === 0) return null;
  const max = Math.max(...points, 0);
  const span = max || 1;
  const gap = 2;
  const barWidth = Math.max(2, (width - gap * (points.length - 1)) / points.length);
  return (
    <svg
      className="admin-mini-bars"
      data-testid={testid}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      role="img"
      aria-label={ariaLabel ?? "分布"}
    >
      {points.map((value, index) => {
        const barHeight = Math.max(1, (Math.max(0, value) / span) * height);
        const x = index * (barWidth + gap);
        return <rect key={index} x={x} y={height - barHeight} width={barWidth} height={barHeight} rx={1} fill="#2563eb" />;
      })}
    </svg>
  );
}
