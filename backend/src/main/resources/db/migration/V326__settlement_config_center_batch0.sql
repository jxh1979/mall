-- 结算配置中心 · 新立项批0 骨架(2026-07-20 用户拍板)。
-- 依据 settlement-rebuild-prd-v1.0 §9(配置中心 = 配置表族 + ADM-STL 页 + 权限点;PRD 原「批5」从未实现,本立项落地)。
-- 批0 迁移内容:①结算配置权限点;②启用「草案不生效」复核闸 —— 现存配置种子一次性转正(is_draft TRUE→FALSE)。
-- 现存种子 = 当前线上生效值 = 视同已批准(2026-07-19 用户拍板);此后新改动写草案(is_draft=TRUE),须经复核转正后方生效。
-- 本迁移仅加权限 + 就地转正,不改表结构,可独立回滚。

-- ========== 1) 结算配置权限点(PRD §9)==========
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('settlement.config.view',         '查看结算配置中心',                        'settlement', 'low',  FALSE, 'active'),
    ('settlement.config.manage',       '维护结算配置(改动写草案·复核转正后生效)',  'settlement', 'high', TRUE,  'active'),
    ('settlement.distribution.manage', '维护分账/分红配置(高敏·复核转正)',        'settlement', 'high', TRUE,  'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 授予:查看 → 平台管理员 + 运营角色;维护/分账(高敏 requires_approval)→ 仅最高权限(沿用现状高危授权约定)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES
    ('role_platform_admin', 'settlement.config.view'),
    ('role_platform_admin', 'settlement.config.manage'),
    ('role_platform_admin', 'settlement.distribution.manage'),
    ('role_ops_manager',    'settlement.config.view')
) AS v(role_id, permission_code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- ========== 2) 启用「草案不生效」复核闸:现存种子一次性转正(is_draft TRUE→FALSE)==========
-- 背景:此前读取侧只过滤 status='active'、不区分 is_draft,导致「草案即生效」;
-- 本立项批0 起读取侧改为「只读 is_draft=FALSE 的正式口径」。为保证现有线上分账/归因口径【零变化】,
-- 先把全部现存种子(均为建表默认 is_draft=TRUE)一次性转正为 FALSE(视同已批准的当前生效值)。
UPDATE merchant_revenue_attribution_rules SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE platform_retention_rules           SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE giveaway_cost_configs              SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE ticket_distribution_configs        SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE ticket_distribution_rank_tiers     SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE ticket_distribution_merchant_rates SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE settlement_tax_configs             SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE comprehensive_service_fee_configs  SET is_draft = FALSE WHERE is_draft = TRUE;
UPDATE ticket_combo_discount_configs      SET is_draft = FALSE WHERE is_draft = TRUE;
