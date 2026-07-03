// 3.8 业务枚举中文化：展示层把后端状态码翻成中文业务口径；提交/请求仍用原码。
// 未知码原样返回（不阻断展示），新增码逐步补此表即可。
const STATUS_LABELS: Record<string, string> = {
  // 支付
  paid: "已支付",
  unpaid: "未支付",
  partial_paid: "部分支付",
  refunded: "已退款",
  refunding: "退款中",
  // 出票
  issued: "已出票",
  not_issued: "未出票",
  // 通用 / 账户 / 会员 / 票 / 权益
  active: "有效",
  inactive: "未激活",
  expired: "已过期",
  frozen: "已冻结",
  suspended: "已封禁",
  blacklisted: "黑名单",
  deregistered: "已注销",
  // 订单 / 预定 / 邀请
  created: "已创建",
  pending: "待处理",
  confirmed: "已确认",
  completed: "已完成",
  cancelled: "已取消",
  closed: "已关闭",
  // 积分 / 兑换
  redeemed: "已兑换",
  processing: "处理中",
  succeeded: "已成功",
  failed: "已失败",
  // fix/ui-pages 批量业务化补充 ——
  // 票种 / 商品上下架
  published: "已上架",
  draft: "草稿",
  offline: "已下架",
  // 应收应付台账（方向 + 状态）
  receivable: "应收",
  payable: "应付",
  pending_collection: "待收款",
  pending_payment: "待付款",
  settled: "已结清",
  // 合同生命周期 / 退租结算
  drafting: "起草中",
  pending_confirmation: "审批中",
  pending_effective: "待生效",
  effective: "生效中",
  terminated: "已终止",
  pending_settlement: "待结算",
  deposit_offsetting: "保证金抵扣中",
  arrears_collecting: "欠费追缴中",
  disputed: "有争议",
  // 合同抽成模式 / 变更类型 / 结算触发
  fixed_ratio: "固定比例",
  guarantee_or_commission_higher: "保底与抽成取高",
  tiered_ratio: "阶梯比例",
  guarantee_plus_commission: "保底+抽成叠加",
  renewal: "续租",
  move_out: "退租",
  area_change: "面积变更",
  rent_change: "租金变更",
  business_scope_change: "经营范围变更",
  early_termination: "提前终止",
  expiry: "到期",
  // 卡座 / 订台
  reserved: "已预订",
  available: "可用",
  occupied: "使用中",
  open: "进行中",
  not_required: "无需定金",
  // 员工账号类型 / 账号状态
  staff: "普通员工",
  sales: "销售人员",
  supervisor: "主管",
  merchant_admin: "商户管理员",
  merchant: "商户员工",
  invited: "待激活",
  disabled: "已禁用",
  enabled: "已启用",
  // 审批 / 就绪 / 通用流转
  approved: "已通过",
  rejected: "已驳回",
  ready: "已就绪",
  blocked: "受阻",
  in_progress: "进行中",
  done: "已完成",
  verified: "已核验",
  // 风险等级
  high: "高",
  medium: "中",
  low: "低",
  // 会员等级
  base: "普通会员",
  // 存酒
  stored: "存酒中",
  consumed: "已取用完",
  forfeited: "过期已处置",
  // 工单派转 / 巡检
  pending_assign: "待派单",
  assigned: "已派单",
  scheduled: "已排期",
  resolved: "已解决",
  // 库存
  normal: "正常",
};

export function formatBusinessStatus(code: string | null | undefined): string {
  if (!code) {
    return "—";
  }
  return STATUS_LABELS[code] ?? code;
}
