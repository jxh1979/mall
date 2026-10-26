-- 文件版本：V0.01
-- V554：能力开关缺行默认关闭（opt-in）改造的显式开启行（第十八批第 6 条）。
-- 依据：PGM L742「任何能力在后台没有显式开关配置行时一律默认关闭（opt-in），要启用必须有人显式打开；开关缺行不得视为开启」；
--   BSM L2030 同句；改造口径 docs/operation-traceability-and-terminal-switch-prd-v0.1.md「默认值反转与全量能力 seed 核查两件事不得分批上线」。
-- 同批代码：TerminalSwitchGuard 缺行即拒绝（reason=not_configured），NotificationSwitchResolver 域/事件开关缺行即不发。
-- 本迁移只给已发布在用、此前靠「缺行放行」运行的能力补跨端（'*'）全局显式开启行；已有显式行的（受控/预留能力的 FALSE 行、
-- 十一端专端 terminal.enabled 总开关、已有的 sms.notify.* 行）不动。不可关闭的短信（MANDATORY_EVENTS）发送不看开关，不补行。

-- 1) 全部 @GuardedAction 能力码（字面量与常量引用解析后共 113 个，见 CapabilitySwitchOptInJdbcIntegrationTests）
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
SELECT 'tcs_optin_' || c, '*', c, 'global', 'global', TRUE, '已发布在用能力显式开启（PGM L742 opt-in）', now(), now()
  FROM (VALUES
    ('amusement.capacity_config.manage'), ('amusement.device.manage'), ('amusement.entitlement_session.claim'),
    ('amusement.entitlement_session.initiate'), ('amusement.group_booking.approve'), ('amusement.group_booking.manage'),
    ('amusement.incident.close'), ('amusement.incident.create'), ('amusement.incident.process'),
    ('amusement.maintenance.confirm'), ('amusement.maintenance.submit'), ('amusement.over_capacity_policy.configure'),
    ('amusement.project.manage'), ('amusement.project_entitlement.approve'), ('amusement.project_entitlement.manage'),
    ('amusement.queue.manage'), ('amusement.reservation.manage'), ('amusement.safety_inspection.approve_reopen'),
    ('amusement.safety_inspection.create'), ('amusement.safety_inspection.recheck'), ('amusement.safety_inspection.rectify'),
    ('amusement.safety_inspection.submit'), ('amusement.safety_notice.manage'), ('amusement.safety_template.manage'),
    ('amusement.session.end'), ('amusement.session.manage'), ('amusement.verification.execute'),
    ('amusement.waiver.acknowledge'),
    ('bar.bill.adjust'), ('bar.bill.anomaly.resolve'), ('bar.bill.reopen'), ('bar.bill.split_merge'), ('bar.bill.tip'),
    ('bar.deposit_rule.manage'), ('bar.entitlement.redeem'), ('bar.order.create'), ('bar.order_item.void'),
    ('bar.payroll.manage'), ('bar.product.sold_out'), ('bar.production_ticket.manage'), ('bar.reservation.approve'),
    ('bar.reservation.create'), ('bar.reservation.invitation.manage'), ('bar.reservation.manage'),
    ('bar.saved_bottle.manage'), ('bar.shift.close'), ('bar.shift.manage'), ('bar.table.guest_checkin'),
    ('bar.table.manage'), ('bar.table.open_close'), ('bar.table_commission.calculate'), ('bar.table_session.create'),
    ('bar.table_session.merge'), ('bar.table_session.renew'), ('bar.table_session.split'), ('bar.table_session.transfer'),
    ('customer.loyalty.evaluate'), ('customer.points.freeze'), ('customer.stored_value.consume'),
    ('customer.stored_value.freeze'), ('customer.stored_value.refund'), ('customer.stored_value.topup'),
    ('finance.ar_ap.manage'), ('finance.ar_ap.view'), ('finance.daily_close.lock'), ('finance.deposit.manage'),
    ('finance.inventory_variance.post'), ('finance.locked_backfill.approve'), ('finance.locked_backfill.request'),
    ('finance.payment.register'), ('finance.payment_intent.create'), ('finance.payroll.payment.register'),
    ('finance.points_recovery.waive'), ('finance.property_fee.manage'), ('finance.reconciliation.difference.create'),
    ('finance.refund.create'), ('finance.turnover_rent.bill'),
    ('inventory.handover.review'), ('inventory.handover.scan'), ('inventory.high_value.review'),
    ('inventory.purchase.approve'), ('inventory.purchase_request.manage'), ('inventory.risk.scan'),
    ('inventory.stock.adjust'), ('inventory.stock.batch.manage'), ('inventory.stock.move'), ('inventory.stocktake.create'),
    ('inventory.stocktake.variance_record'), ('inventory.stocktake.variance_review'), ('inventory.supplier.manage'),
    ('inventory.supplier.reconcile'), ('inventory.supply_contract.manage'), ('inventory.trace_code.print'),
    ('inventory.trace_code.register'), ('inventory.warehouse.manage'),
    ('staff.payroll.manage'), ('staff.payroll.view_mine'), ('terminal.device.bind'),
    ('ticket.dynamic_pricing.manage'), ('ticket.dynamic_pricing.publish'), ('ticket.entitlement.approve'),
    ('ticket.entitlement.manage'), ('ticket.order.create'), ('ticket.price_rule.approve'), ('ticket.price_variant.manage'),
    ('ticket.product.manage'), ('ticket.refund.approve'), ('ticket.refund.request'), ('ticket.ticket.issue'),
    ('ticket.type.approve'), ('ticket.verify.anomaly.resolve'), ('ticket.verify.reverse'), ('verification.execute')
  ) AS v(c)
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 2) 已接触发点、此前无行靠缺行放行的短信域与事件开关（照 V177/V546 写法；总开关 sms_notification 仍为 V177 的 FALSE 行不动）
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_sms_notify_stored_value_pending_refund', '*', 'sms.notify.stored_value.pending_refund', 'global', 'global', TRUE,
     '储值待退款提醒短信事件开关', now(), now()),
    ('tcs_sms_notify_stored_wine_exit_notice', '*', 'sms.notify.stored_wine.exit_notice', 'global', 'global', TRUE,
     '酒吧退出存酒告知短信事件开关', now(), now()),
    ('tcs_sms_notify_stored_wine_handover', '*', 'sms.notify.stored_wine.handover', 'global', 'global', TRUE,
     '存酒移交平台保管告知短信事件开关', now(), now()),
    ('tcs_sms_notify_service', '*', 'sms.notify.service', 'global', 'global', TRUE,
     '服务单短信域开关', now(), now()),
    ('tcs_sms_notify_service_catalog_fee_changed', '*', 'sms.notify.service.catalog_fee_changed', 'global', 'global', TRUE,
     '服务项目管理费变更短信事件开关', now(), now()),
    ('tcs_sms_notify_facility', '*', 'sms.notify.facility', 'global', 'global', TRUE,
     '设施短信域开关', now(), now()),
    ('tcs_sms_notify_facility_alarm_takeover', '*', 'sms.notify.facility.alarm_takeover', 'global', 'global', TRUE,
     '设施告警接管短信事件开关', now(), now()),
    ('tcs_sms_notify_ticketing', '*', 'sms.notify.ticketing', 'global', 'global', TRUE,
     '票务短信域开关', now(), now()),
    ('tcs_sms_notify_ticketing_ticket_transfer_created', '*', 'sms.notify.ticketing.ticket_transfer_created', 'global', 'global', TRUE,
     '门票转赠领取短信事件开关', now(), now()),
    ('tcs_sms_notify_voucher', '*', 'sms.notify.voucher', 'global', 'global', TRUE,
     '券短信域开关', now(), now()),
    ('tcs_sms_notify_voucher_voucher_transfer_created', '*', 'sms.notify.voucher.voucher_transfer_created', 'global', 'global', TRUE,
     '券转赠领取短信事件开关', now(), now()),
    ('tcs_sms_notify_bar', '*', 'sms.notify.bar', 'global', 'global', TRUE,
     '酒吧短信域开关', now(), now()),
    ('tcs_sms_notify_bar_pending_clear_overdue', '*', 'sms.notify.bar.pending_clear_overdue', 'global', 'global', TRUE,
     '待清台超时提醒短信事件开关', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 3) 跨端（'*'）终端总开关：没有请求上下文的后台调用取不到终端类型、按 '*' 求值（如线上支付结果定时补结
--    CustomerOnlinePaymentSettlementJob 确认停车缴费时按所属综合体判总开关）。十一端各有专端总开关行，同层专端行优先于通配行，
--    本行不改变任何一端的开关。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES ('tcs_term_wildcard', '*', 'terminal.enabled', 'global', 'global', TRUE, '无终端上下文的后台调用总开关（跨端通配）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;
