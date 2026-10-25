-- 文件版本：V0.01
-- V546：OBS-13-04 商户费用催缴短信独立事件开关（第十四批）。
-- 依据：BSM L1597「给商户：房租、水电费、物业费的账单与催缴……本条所定的异议期适用于非积分类的商户账单（房租、水电、物业、……）」；
--   BSM L1598「管理员可在后台自行决定哪些提醒发短信。但下列三类不在可关闭范围内」。账单出单（rent/utilities/property）不可关闭，
--   催缴不改变应收应付金额，改用 *_dunning 事件键，管理员仍可关闭。照 V177 写法。

INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_sms_notify_merchant_fee_rent_dunning', '*', 'sms.notify.merchant_fee.rent_dunning', 'global', 'global', TRUE,
     '房租催缴短信事件开关', now(), now()),
    ('tcs_sms_notify_merchant_fee_utilities_dunning', '*', 'sms.notify.merchant_fee.utilities_dunning', 'global', 'global', TRUE,
     '水电费催缴短信事件开关', now(), now()),
    ('tcs_sms_notify_merchant_fee_property_dunning', '*', 'sms.notify.merchant_fee.property_dunning', 'global', 'global', TRUE,
     '物业费催缴短信事件开关', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;
