// 库位码 / 库位类型 → 业务库位名（展示层翻译，接口仍用原码）。
// 未知码原样返回，新增库位逐步补此表（照 metricLabels 样板）。
const LOCATION_LABELS: Record<string, string> = {
  "bar-main": "酒吧主库",
  "warehouse-main": "中央仓库",
  "admin-main": "后勤主库",
};

// 库位类型（余额表 locationType 常给类型而非具体码）。
const LOCATION_TYPE_LABELS: Record<string, string> = {
  warehouse: "仓库",
  bar: "酒吧",
  admin: "后勤",
  kitchen: "后厨",
};

// 展示层常见库位码，供登记表单 datalist 中文提示（仍允许自由输入其它码）。
export const KNOWN_LOCATIONS: Array<{ code: string; name: string }> = Object.entries(
  LOCATION_LABELS,
).map(([code, name]) => ({ code, name }));

export function formatLocation(code: string | null | undefined): string {
  if (!code) return "—";
  return LOCATION_LABELS[code] ?? LOCATION_TYPE_LABELS[code] ?? code;
}
