// 文件版本：V0.04
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
  "revenue.thirdparty": "第三方回流收入",
  "revenue.net": "净收入",
  "revenue.per_sqm": "坪效",
  // 客流 / 核销
  "traffic.amusement": "游乐客流",
  "verify.done": "票务核销",
  "verify.redemption_rate": "核销率",
  "verify.rejected": "核销拒绝数",
  "verify.issued": "权益发放数",
  // 会员
  "member.new": "新增会员",
  "member.checked_in": "会员签到",
  "member.level_changed": "会员升降级",
  // 风险
  "risk.pending_review": "待复核",
  "risk.manual_backfill": "补录数量",
  // 出租 / 面积
  "occupancy.rate": "出租率",
  "occupancy.leased_area": "已租面积",
  "occupancy.rentable_area": "可租面积",
  "occupancy.total_area": "铺位总面积",
  "occupancy.signed_pending_area": "已签待入驻面积",
  // HP-P01：occupancy.parking_rate 是车位出租率（月租与员工车位占用计入），车位使用率是停车场另列的指标。
  "occupancy.parking_rate": "车位出租率",
  // 停车场（BSM「停车场」章「连接经营看板」，与车位出租率并列）
  "parking.utilization_rate": "车位使用率",
  "parking.daily_turnover": "日周转次数",
  "parking.revenue": "停车收入",
  "parking.credit_share": "抵免占比",
  "parking.avg_stay_minutes": "平均停留时长",
  // 招商漏斗 / 租售比(V220)
  "leasing.lead_count": "新增招商线索",
  "leasing.intent_conversion_rate": "线索转意向率",
  "leasing.signed_conversion_rate": "意向转签约率",
  "leasing.rent_to_sales": "租售比",
  // 能耗
  "energy.total_electricity": "总用电量",
  "energy.total_water": "总用水量",
  "energy.total_gas": "总用气量",
  "energy.total_heat_cold": "总冷热量",
  "energy.electricity_per_sqm": "单位面积用电量",
  // 财务应收账龄 / 供应链 / 合同 / 工单积压（T3-A、经营分析实际出现的机读键）
  "ar.aging_0_30": "应收账龄 0-30 天",
  "supply.low_stock": "缺货预警",
  "contract.expiring_90d": "90 天内到期合同",
  "mwo.backlog": "商户工单积压",
  // 财务应收（账龄 / 余额 / 回款）
  "ar.balance": "欠费总额",
  "ar.aging_31_60": "应收账龄 31-60 天",
  "ar.aging_61_90": "应收账龄 61-90 天",
  "ar.aging_90p": "应收账龄 90 天以上",
  "ar.collected_mtd": "本月已收",
  "ar.receivable_mtd": "本月应收",
  "ar.collection_rate_mtd": "本月回款率",
  // 能耗异常
  "energy.abnormal_count": "能耗异常数",
  // 供应链
  "supply.balance": "库存结余",
  "supply.received": "采购到货",
  // 物业工单时效
  "wo.response_hours_p50": "工单响应时长中位",
  "wo.handle_hours_p50": "工单处理时长中位",
  "wo.overdue_rate": "工单超时率",
  "wo.backlog": "工单积压",
  // 商户服务工单时效
  "mwo.handle_hours_p50": "商户工单处理时长中位",
  "mwo.overdue_rate": "商户工单逾期率",
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
  customer: "客户",
  inventory: "库存",
  governance: "治理",
  facility: "设施",
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
