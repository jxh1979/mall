// 3.8 业务枚举中文化：展示层把后端状态码翻成中文业务口径；提交/请求仍用原码。
// 未知码原样返回（不阻断展示），新增码逐步补此表即可。
const STATUS_LABELS: Record<string, string> = {
  // 数据清理作业（Tier-D 集中清理工具）
  dry_run: "已预览",
  pending_approval: "待审批",
  executed: "已执行",
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
  platform_admin: "平台管理员",
  outsourced_vendor: "外包账号",
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
  abnormal: "异常",
  // 工单类型（物业/商户工单共用）
  repair: "报修",
  cleaning: "保洁",
  complaint: "投诉",
  rectification: "整改",
  other: "其他",
  service: "服务",
  // 工单优先级
  urgent: "紧急",
  critical: "紧急",
  // 巡检类型
  security: "安防",
  fire: "消防",
  facility: "设施",
  environment: "环境",
  // 巡检/排程 模式
  template: "按模板",
  route: "按路线",
  // 巡检异常处理模式
  light: "轻度告警",
  to_work_order: "转工单",
  // 设备告警来源
  iot: "物联网",
  // 抄表方式
  remote: "远程抄表",
  // SLA 验收方
  handler: "处理人",
  reporter: "报告人",
  // 设备状态（台账扩展）
  alarm: "告警",
  fault: "故障",
  maintenance: "维保中",
  retired: "报废",
  // 工单流转（商户工单目标状态）
  pending_merchant: "商户待处理",
  // 风险预警类型
  staff_behavior: "员工行为",
  customer_risk: "客人风险",
  third_party_exception: "第三方异常",
  production_risk: "出品风险",
  // 风险等级
  suspicious: "可疑",
  // 主管复核动作类型
  contract_violation: "合同违规",
  base_definition_violation: "基础定义违规",
  billing_discount: "账单折扣",
  manual_release: "人工放行",
  // 审计结果
  success: "成功",
  failure: "失败",
  // 授权申请类型
  temporary_permission: "临时权限",
  user_role: "用户角色",
  user_position: "用户职位",
  // 售票渠道
  web: "网页端",
  miniapp: "小程序",
  channel: "第三方渠道",
  // 经营主体角色（platform 域 entity_role）
  operation: "运营主体",
  // 商户合作形式（platform 域 cooperation_form）
  fixed_rent_plus_commission: "固定租金+抽成",
  revenue_share: "收入分成",
  fixed_rent: "固定租金",
  // —— KPI 前置批全局补充（2026-07-05）——
  // 商户类型（platform 域 merchant_type；service 已有）
  tenant: "租赁商户",
  retail: "零售",
  catering: "餐饮",
  entertainment: "娱乐",
  // 业务域 / 退款域（refund_domain 等）
  ticket: "票务",
  bar: "酒吧",
  amusement: "游乐",
  finance: "财务",
  all: "全部",
  // 经营分析统计口径（segment_type；merchant 多义不进全局表）
  platform: "平台",
  project: "项目",
  // 票务权益类型（entitlement_type）
  entry: "入园",
  ride: "游乐项目",
  gift_drink: "赠饮",
  drink: "酒水",
  admission: "入场资格",
  dining: "餐饮",
  // 退款自动规则类型
  expired_unused: "到期未用",
  unused: "未使用",
  // 游乐 KPI 标签用词（reserved 已有"已预订"，游乐"已占用"义留页级）
  bookings: "预约",
  sessions: "场次",
  capacity: "容量",
  locked: "已锁定",
  projects: "项目",
  // 设施工单流转（accepted/suspended 多义留页级）
  pending_review: "待复核",
  pending_acceptance: "待验收",
  overtime: "已超时",
  // 巡检整改
  rectifying: "整改中",
  rechecked: "已复查",
  rectification_submitted: "整改已提交",
  // 能耗抄表复核
  recorded: "已记录",
  reviewed: "已复核",
  // 进场施工
  submitted: "已提交",
  // 账单账龄
  overdue: "已逾期",
  // 对账 / 集成事件
  processed: "已处理",
  matched: "已匹配",
  ignored: "已忽略",
  // 风险预警处置
  dismissed: "已忽略",
  // 集成适配器类型 / 就绪状态（retail/catering 与商户类型同词同义）
  payment: "支付",
  ticket_channel: "票务渠道",
  face_age: "人脸年龄核验",
  wecom: "企业微信",
  robot: "机器人",
  video: "视频",
  device: "设备",
  not_ready: "未就绪",
  // 支付渠道
  cash: "现金",
  pos: "POS刷卡",
};

export function formatBusinessStatus(code: string | null | undefined): string {
  if (!code) {
    return "—";
  }
  return STATUS_LABELS[code] ?? code;
}
