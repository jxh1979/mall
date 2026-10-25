// 文件版本：V0.01
// OBS-14-02：后台结果区中文展示（PGM「全局原则 · 界面全中文（所有终端零英文）」）。
// 页面经 setActionResult 放进来的对象，按「中文键名：可读值」逐行展示：键名查 i18n/fieldLabels（与后端 FieldLabels 同口径），
// 枚举取值先按键查 KEY_VALUE_LABELS（同码不同键含义不同），再查 formatBusinessStatus 状态码表，再查 VALUE_LABELS（动作码、页面状态码、
// 一词多义的后端状态）；金额（*_cent、*_cents 及其 _min/_max 上下限）分转元，ISO 时间转本地中文时间，
// 嵌套对象缩进、数组逐项；字符串结果照旧。字典缺键、缺取值由 tools/verify-admin-result-labels.mjs 门禁拦。
import { FIELD_LABELS, KEY_VALUE_LABELS, VALUE_LABELS } from "../i18n/fieldLabels";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { cnDate } from "./RowDetailDrawer";

// 蛇形与驼峰同查：order_id / orderId 都落到 orderid。
const compact = (key: string) => key.replace(/_/g, "").toLowerCase();
const LABELS = new Map(Object.entries(FIELD_LABELS).map(([key, label]) => [compact(key), label]));
const KEY_VALUES = new Map(Object.entries(KEY_VALUE_LABELS).map(([key, values]) => [compact(key), values]));
const MONEY = /(^|_)cents?(_(min|max))?$|Cents?(Min|Max)?$/;
const pad = (n: number) => String(n).padStart(2, "0");

function label(key: string): string {
  if (/[一-鿿]/.test(key)) return key;
  const known = LABELS.get(compact(key));
  if (known) return known;
  // 字段名形状的英文键不上屏；编号作键的（如按商户编号汇总的金额）原样，属单据号/内部标识。
  return /^[a-z][A-Za-z0-9_]*$/.test(key) ? "其他信息" : key;
}

function scalar(key: string, value: unknown): string {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "number") return MONEY.test(key) ? `${(value / 100).toFixed(2)} 元` : String(value);
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(text)) {
    const d = new Date(text);
    if (!Number.isNaN(d.getTime())) return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return cnDate(text);
  const byKey = KEY_VALUES.get(compact(key))?.[text];
  if (byKey) return byKey;
  const status = formatBusinessStatus(text);
  return status !== text ? status : VALUE_LABELS[text] ?? text;
}

function field(name: string, key: string, value: unknown, indent: string): string[] {
  if (Array.isArray(value)) {
    if (value.length === 0) return [`${indent}${name}：无`];
    if (value.every((item) => item === null || typeof item !== "object")) return [`${indent}${name}：${value.map((item) => scalar(key, item)).join("、")}`];
    return [`${indent}${name}：`, ...value.flatMap((item, i) => field(`第${i + 1}项`, key, item, `${indent}  `))];
  }
  if (value !== null && typeof value === "object") return [`${indent}${name}：`, ...entries(value, `${indent}  `)];
  return [`${indent}${name}：${scalar(key, value)}`];
}

function entries(value: object, indent: string): string[] {
  const list = Object.entries(value);
  return list.length === 0 ? [`${indent}无`] : list.flatMap(([key, item]) => field(label(key), key, item, indent));
}

export function formatActionResult(value: unknown): string {
  if (typeof value === "string") return value;
  if (Array.isArray(value)) return value.flatMap((item, i) => field(`第${i + 1}项`, "", item, "")).join("\n");
  if (value !== null && typeof value === "object") return entries(value, "").join("\n");
  return scalar("", value);
}

export function ActionResultView({ value }: { value: unknown }) {
  return <pre className="admin-result" role="status" aria-live="polite" data-testid="admin-action-result">{formatActionResult(value)}</pre>;
}
