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
};

export function formatBusinessStatus(code: string | null | undefined): string {
  if (!code) {
    return "—";
  }
  return STATUS_LABELS[code] ?? code;
}
