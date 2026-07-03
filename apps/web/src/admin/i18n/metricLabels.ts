// fix/ui-pages 经营指标码 → 业务指标名（展示层翻译，接口仍用原码）。
// 未知码原样返回，新增指标逐步补此表。
const METRIC_LABELS: Record<string, string> = {
  // 收入
  "revenue.ticket": "票务收入",
  "revenue.ticket_refund": "票务退款",
  "revenue.ticket_bar_share": "通票酒吧分成",
  "revenue.ticket_amusement_share": "通票游乐分成",
  "revenue.bar": "酒吧收入",
  "revenue.bar_refund": "酒吧退款",
  "revenue.contract_rent": "合同租金",
  "revenue.thirdparty": "第三方收入",
  "revenue.net": "净收入",
  "revenue.per_sqm": "坪效",
  // 客流 / 核销
  "traffic.amusement": "游乐客流",
  "verify.done": "票务核销",
  "verify.redemption_rate": "核销率",
  // 会员
  "member.new": "新增会员",
  "member.checked_in": "会员签到",
  "member.level_changed": "会员升降级",
  // 风险
  "risk.pending_review": "待复核",
  "risk.manual_backfill": "人工补录",
  // 出租 / 面积
  "occupancy.rate": "出租率",
  "occupancy.leased_area": "已租面积",
  "occupancy.rentable_area": "可租面积",
  "occupancy.signed_pending_area": "已签待入驻面积",
  "occupancy.parking_rate": "车位使用率",
  // 能耗
  "energy.total_electricity": "总用电量",
  "energy.total_water": "总用水量",
  "energy.total_gas": "总用气量",
  "energy.total_heat_cold": "总冷热量",
};

// 业务域码 → 业务域名（经营分析筛选与表格列用）。
const DOMAIN_LABELS: Record<string, string> = {
  ticketing: "票务",
  ticket: "票务",
  bar: "酒吧",
  amusement: "游乐",
  member: "会员",
  membership: "会员",
  finance: "财务",
  risk: "风控",
  merchant: "商户",
  energy: "能耗",
  leasing: "租赁",
  occupancy: "出租",
  unknown: "未分域",
};

export function formatMetricName(code: string | null | undefined): string {
  if (!code) return "—";
  return METRIC_LABELS[code] ?? code;
}

export function formatMetricDomain(code: string | null | undefined): string {
  if (!code) return "—";
  return DOMAIN_LABELS[code] ?? code;
}
