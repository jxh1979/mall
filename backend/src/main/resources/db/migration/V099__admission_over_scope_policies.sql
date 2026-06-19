-- P0 准入闸：启用 over_contract_scope / over_space 策略（消除"无 over_* 策略=判定引擎空转"的 fail-open）。
--
-- 背景：判定引擎（AdmissionGuard + JdbcAdmissionFactRepository，over_contract_scope / over_space SQL）
--   及数据模型（V080 allowed_business_scope + contract_spaces、V084 对象→空间映射）均已就绪，
--   但 V068 的 seed 无一条 trigger_mode=over_contract_scope/over_space → 任何业务动作都命中不到 over_*
--   策略，"违反合同范围/超空间"永不被卡住。本迁移补 over_* 策略行启用该判定路径。
--
-- 业务口径（业务 HTML）：违反合同/超空间 → 先卡住（block，进 supervisor_review），
--   再按上一级 / 上两级 / 最高权限放行。outcome 一律 block（V068 chk_srp_outcome 仅 allow/warn/block；
--   "block_until_review" 即 block + SupervisorReviewService.blockForAdmission 建 pending 复核）。
--
-- 匹配约束（重要）：JdbcAdmissionPolicyRepository.findEnabledPolicy 按 (action_type, object_type)
--   命中且 ORDER BY policy_code LIMIT 1，即每个 (action_type, object_type) 实际只生效一条策略。
--   故本迁移仅对 V068 未占用的 (action_type, object_type) 对补 over_* 策略，避免与既有 always/
--   qualification_abnormal/warn/allow 行相互遮蔽，且同一对只补一条（over_space 或 over_contract_scope 二选一）。
--   V068 已占用、本迁移不再补的对：
--     bar.bill_waiver/bar_bill、amusement.reopen/inspection、finance.locked_backfill/ar_ap_item、
--     ticketing.special_release/verification。
--
-- over_space vs over_contract_scope 分配口径：
--   over_space 要求对象能解析到 space（JdbcAdmissionFactRepository.resolveSpaceId 支持的 objectType：
--     bar_bill / amusement_maintenance_record / amusement_incident / amusement_group_booking 等），
--     故对这些"空间可解析"的对象用 over_space（物理越界，风险更高 → 上两级 two_levels_up）。
--   其余对象（票务/财务/库存等无空间归属）用 over_contract_scope（经营范围越界 → 上一级 one_level_up，
--     财务类 → 最高权限 top_authority）。
--
-- 覆盖：票务下单/出票（退款审批/动态定价发布/核销冲正）、酒吧开台/结算（开台重开/班结/整单作废）、
--   游乐入场（团单审批/维保确认/事故关闭）、库存出库（出库调整/采购到货/寄存认领）、财务结算
--   （退款/线下到账确认/锁账回填审批）等关键 action_type/object_type。
--
-- approval_level / required_level 取 V068 既有枚举（one_level_up / two_levels_up / top_authority）。

INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, enabled)
VALUES
    -- ── over_space：超出合同租赁空间（对象可解析到 space）→ 先卡住，上两级放行 ──────
    ('bar_bill_reopen_over_space',          'bar.bill_reopen',                 'bar_bill',                          'over_space',          'block', 'two_levels_up', 3, true),
    ('amusement_maint_confirm_over_space',  'amusement.reopen',                'amusement_maintenance_record',      'over_space',          'block', 'two_levels_up', 3, true),
    ('amusement_incident_close_over_space', 'amusement.incident_close',        'amusement_incident',                'over_space',          'block', 'two_levels_up', 3, true),
    ('amusement_group_booking_over_space',  'amusement.group_booking_approve', 'amusement_group_booking',           'over_space',          'block', 'two_levels_up', 3, true),

    -- ── over_contract_scope：违反合同经营范围 → 先卡住，上一级放行 ────────────────
    -- 酒吧
    ('bar_order_item_void_over_scope',      'bar.order_item_void',             'bar_order_item',                    'over_contract_scope', 'block', 'one_level_up',  2, true),
    ('bar_shift_close_over_scope',          'bar.shift_close',                 'bar_shift',                         'over_contract_scope', 'block', 'one_level_up',  2, true),
    -- 票务
    ('ticketing_refund_approve_over_scope', 'ticketing.refund_approve',        'ticket_refund',                     'over_contract_scope', 'block', 'one_level_up',  2, true),
    ('ticketing_dyn_pricing_over_scope',    'ticketing.dynamic_pricing_publish','ticket_dynamic_pricing_rule',      'over_contract_scope', 'block', 'one_level_up',  2, true),
    ('ticketing_verify_reverse_over_scope', 'ticketing.verification_reverse',  'verification',                      'over_contract_scope', 'block', 'one_level_up',  2, true),
    -- 游乐（manual_override 对象无空间解析、走经营范围）
    ('amusement_manual_override_over_scope','amusement.manual_override',       'amusement_manual_override',         'over_contract_scope', 'block', 'one_level_up',  2, true),
    -- 库存出库
    ('inventory_stock_adjust_over_scope',   'inventory.stock_adjust',          'inventory_stock_adjustment',        'over_contract_scope', 'block', 'one_level_up',  2, true),
    ('inventory_purchase_recv_over_scope',  'inventory.purchase_receive',      'inventory_purchase_request',        'over_contract_scope', 'block', 'one_level_up',  2, true),
    ('inventory_saved_bottle_over_scope',   'inventory.saved_bottle_claim',    'saved_bottle',                      'over_contract_scope', 'block', 'one_level_up',  2, true),
    -- 财务结算 → 最高权限
    ('finance_refund_over_scope',           'finance.refund',                  'finance_refund',                    'over_contract_scope', 'block', 'top_authority', 4, true),
    ('finance_offline_confirm_over_scope',  'finance.offline_payment_confirm', 'finance_offline_payment_confirmation','over_contract_scope','block', 'top_authority', 4, true),
    ('finance_locked_approve_over_scope',   'finance.locked_backfill_approve', 'ar_ap_adjustment',                  'over_contract_scope', 'block', 'top_authority', 4, true)
ON CONFLICT (policy_code) DO NOTHING;
