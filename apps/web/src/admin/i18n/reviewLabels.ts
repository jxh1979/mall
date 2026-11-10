// 文件版本：V0.04
// 第十八批 I 段第 87 条（PGM:287「界面全中文（所有终端零英文）」）：主管复核的动作码、对象码、档位码与证据类型中文名，
// 主管复核页与审批链页（ADM-GOV-009）共用（展示层翻译，提交仍原码）。原表与 formatReviewValue 自 SupervisorReviewPage.tsx 搬入。
import { formatBusinessStatus } from "./statusLabels";

export const REVIEW_LABELS: Record<string, string> = {
  "staff.shift.publish": "发布排班", "staff.payroll.penalty_cap.adjust": "调整工资扣罚上限",
  bill_waiver: "免单", "bar.bill_waiver": "免单", refund: "退款", ar_ap_adjustment: "调账",
  permission_grant_l1: "一级权限授权", permission_grant_l2: "二级权限授权", permission_grant_l3: "三级权限授权",
  "finance.tax_rule.adjust": "调整税费规则",
  reason: "原因说明", before_after: "变更前后资料", operator_note: "操作说明",
  evidence_attachment: "证据附件", violation_summary: "违规说明",
  contract_snapshot: "合同快照", definition_snapshot: "基础定义快照",
  one_level_up: "一级上级审批", two_levels_up: "两级上级审批",
  top_authority: "最高权限审批", specific_role: "指定岗位审批",
  hr_three_levels_up: "人力会签＋直属领导向上三级复核",
  "staff.profile.employment_status.change": "员工状态变更", "staff.payroll.adjustment.add": "员工奖惩入账",
  "staff.payroll.adjustment.appeal": "员工扣罚申诉", staff_profile: "员工档案", staff_payroll_adjustment: "员工奖惩记录",
  staff_payroll_adjustment_appeal: "员工扣罚申诉单",
  contract: "合同", bar_bill: "酒吧账单", access_grant_request: "权限授权申请",
  "facility.equipment.scrap": "楼宇设备报废", facility_equipment_scrap: "楼宇设备报废单",
  // 第十八批 Y 段第 181 条 181-R1（ENUM:191「复核环节属业务审批」）：对账差异关闭复核。
  "finance.reconciliation_difference.close": "对账差异关闭复核", finance_reconciliation_difference: "对账差异",
  // 第十八批 M 段第 117 条（BSM:1380「承担方有争议的，工单挂起转上级判定」）：设施工单与商户服务工单共用。
  "work_order.bearer_dispute": "工单费用承担方争议", work_order_bearer_dispute: "工单费用承担方争议单",
  // 第十八批 AB 段第 190 条 190-R2（BSM:1079「达到上限后须主管审批才能继续转台」）：超限转台申请。
  "bar.table_session.transfer_over_limit": "转台超过次数上限", bar_table_transfer_limit_approval: "超限转台申请",
  "parking.release.waive": "停车免费放行与减免", parking_manual_release: "停车人工放行",
  "leasing.blacklist_exception": "黑名单例外放行", leasing_blacklist_exception: "黑名单例外放行单",
  "leasing.intent.key_stage": "招商意向关键阶段确认", leasing_intent: "招商意向",
  "merchant.project.add": "商户加经营项目", merchant_project_request: "加经营项目申请",
  "compliance.expiry_item.grace": "证照宽限期申请", compliance_expiry_item: "证照",
  // 第十八批 H 段第 23 条 23-R4（PGM:687）：导出文件可编辑版申请。
  "security.export.editable_copy": "可编辑版导出申请", export_file: "导出文件",
  // 第十八批 F 段第 14 条 14-R8（BSM 第 1642 行「补录一律进入主管复核队列」）：人工补录三类复核动作与补录单。
  "inventory.trace.scan_backfill": "交接扫码补录", "bar.sales.backfill": "销售归属补录", "bar.service.backfill": "服务责任补录",
  manual_backfill: "人工补录单",
  // 第十八批 I 段第 87 条 87-R1：会建复核单的其余动作码。码闭集三路：① evaluate（SupervisorReviewService 策略表）；
  // ② 准入闸（V068/V099/V541 中 outcome=block 且有 @GuardedAction(admissionAction) 的行）；③ AccessService 直接建单。
  "staff.payroll.scheme.change": "工资方案变更", "staff.payroll.period.confirm": "工资期间确认", "staff.performance.points.deduct": "绩效扣分",
  "staff.performance.points.appeal": "绩效扣分申诉", "staff.payroll.calendar.publish": "计薪日历发布", "staff.performance.rule.publish": "绩效规则发布",
  "bar.payroll_rule.adjust": "酒吧薪酬规则调整", "amusement.business_day.configure": "游乐营业日配置", "bar.business_day.configure": "酒吧营业日配置",
  "settlement.high_value_threshold.configure": "高价值判定阈值调整", "inventory.purchase_request": "采购申请", "inventory.purchase_terminate": "采购余量终止",
  "inventory.supply_contract": "供货合同审批", "inventory.purchase_return": "采购退货", "inventory.purchase_prepayment_resolution": "预付款余额处理",
  "contract.new": "新建合同", "contract.expiry_termination": "合同到期终止", "contract.change": "合同变更", "energy.reading_anomaly_confirm": "能耗异常读数核实",
  "energy.review_settings": "能耗核实期限设置", "dealer.delivery_resolution": "经销交付差异处理", "inventory.quarantine_disposition": "隔离实物处置",
  "inventory.transfer_order.cancel": "要货未出库取消", "inventory.transfer_order.unreturnable": "借用物无法归还", "ticket.reward.rule": "积分规则",
  "ticket.reward.national": "全国积分事项", "ticket.reward.sales_contract": "积分销售合同", "staff.attendance.receipt.review": "领取时刻补证",
  "amusement.reopen": "游乐复开", "bar.bill_reopen": "酒吧账单重开", "amusement.incident_close": "游乐事故关闭", "amusement.group_booking_approve": "游乐团体预订审批",
  "bar.order_item_void": "酒吧订单明细作废", "bar.shift_close": "酒吧交班", "ticketing.refund_approve": "门票退款审批", "ticketing.dynamic_pricing_publish": "动态定价发布",
  "ticketing.verification_reverse": "核销冲正", "inventory.stock_adjust": "库存调整", "inventory.purchase_receive": "采购入库验收",
  "inventory.saved_bottle_claim": "存酒领取", "finance.refund": "财务退款", "finance.offline_payment_confirm": "线下收款确认",
  "finance.locked_backfill_approve": "锁账后补录审批", "customer.home_content.publish": "首页内容发布", "access.position_role_template.delete": "删除岗位模板",
  "access.scope_change": "员工任职范围变更", "access.scope_change.money": "员工任职范围变更（涉钱）", "access.grant_request": "权限授权申请",
  "access.grant_request.money": "权限授权申请（涉钱）",
  // 87-R2：三路调用处传入的对象码（不放进全局 STATUS_LABELS：inspection 在合规提醒页另有「复检」之意）。
  contract_change: "合同变更单", contract_settlement: "合同到期结算单", ticket_reward_config: "积分规则版本", ticket_points_billing_rule: "积分结算规则",
  ticket_points_calendar: "积分结算日历", ticket_points_portal_outage: "积分平台故障登记", ticket_points_objection_resolution: "积分账单异议处理",
  purchase_prepayment_resolution: "预付款余额处理单", inventory_transfer_order: "流转单", inventory_quarantine_disposition: "隔离处置单", supply_contract: "供货合同",
  inventory_purchase_request: "采购申请单", purchase_quantity_resolution: "采购余量处理单", purchase_return: "采购退货单", amusement_business_day_config: "游乐营业日配置",
  energy_reading_verification: "能耗读数核实单", energy_review_settings: "能耗核实设置", high_value_threshold: "高价值判定阈值", dealer_return_freight: "经销首程运费调减",
  dealer_return: "经销退货单", dealer_order_refund: "经销订单退款", dealer_delivery_resolution: "经销交付差异处理单", staff_performance_score: "绩效扣分记录",
  staff_payroll_scheme: "工资方案", staff_payroll_calendar: "计薪日历", staff_payroll_period: "工资期间", staff_performance_rule: "绩效规则",
  staff_attendance_receipt_review: "领取时刻补证单", payroll_tax_version: "税表版本", payroll_tax_assessment: "专业税务认定", staff_shift_batch: "排班发布批次",
  staff_penalty_cap: "工资扣罚上限", bar_payroll_rule: "酒吧薪酬规则", bar_business_day_config: "酒吧营业日配置", inspection: "游乐安全检查",
  amusement_maintenance_record: "游乐维修记录", amusement_incident: "游乐事故", amusement_group_booking: "游乐团体预订", bar_order_item: "酒吧订单明细", bar_shift: "酒吧班次",
  ticket_refund: "门票退款单", ticket_dynamic_pricing_rule: "动态定价规则", verification: "核销记录", inventory_stock_adjustment: "库存调整单", saved_bottle: "存酒",
  finance_refund: "财务退款单", finance_offline_payment_confirmation: "线下收款确认单", customer_home_content: "首页内容", position_role_template: "岗位模板",
};

export function formatReviewValue(value?: string | null) {
  if (!value) return "-";
  const label = REVIEW_LABELS[value] ?? formatBusinessStatus(value);
  return /[a-zA-Z]/.test(label) ? "未识别类型" : label;
}
