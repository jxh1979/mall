-- Group governance S2: high-risk access grant requests.

CREATE TABLE IF NOT EXISTS access_grant_requests (
    id VARCHAR(64) PRIMARY KEY,
    request_type VARCHAR(32) NOT NULL,
    target_user_id VARCHAR(64),
    target_role_id VARCHAR(64),
    template_id VARCHAR(64),
    position_code VARCHAR(64),
    staff_level_id VARCHAR(64),
    base_salary_amount_cent BIGINT,
    salary_currency VARCHAR(8),
    salary_detail TEXT,
    permission_code VARCHAR(128),
    permission_codes TEXT,
    valid_from TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    single_use BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    review_id VARCHAR(64),
    reason VARCHAR(1000),
    requested_by VARCHAR(64),
    decided_by VARCHAR(64),
    decided_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE access_grant_requests
    ADD COLUMN IF NOT EXISTS target_role_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS template_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS position_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS staff_level_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS base_salary_amount_cent BIGINT,
    ADD COLUMN IF NOT EXISTS salary_currency VARCHAR(8),
    ADD COLUMN IF NOT EXISTS salary_detail TEXT,
    ADD COLUMN IF NOT EXISTS permission_code VARCHAR(128),
    ADD COLUMN IF NOT EXISTS permission_codes TEXT,
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS single_use BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE access_grant_requests
    ALTER COLUMN target_role_id DROP NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT now();

UPDATE access_grant_requests
   SET permission_codes = permission_code
 WHERE permission_codes IS NULL
   AND permission_code IS NOT NULL;

UPDATE access_grant_requests
   SET updated_at = created_at
 WHERE updated_at IS NULL;

ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_agr_type;
ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_agr_status;
ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_access_grant_requests_type;
ALTER TABLE access_grant_requests DROP CONSTRAINT IF EXISTS chk_access_grant_requests_status;

ALTER TABLE access_grant_requests
    ADD CONSTRAINT chk_access_grant_requests_type
    CHECK (request_type IN ('user_role', 'user_position', 'temporary_permission', 'role_permission'));

ALTER TABLE access_grant_requests
    ADD CONSTRAINT chk_access_grant_requests_status
    CHECK (status IN ('pending', 'applied', 'rejected'));

CREATE INDEX IF NOT EXISTS idx_access_grant_requests_status
    ON access_grant_requests (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_grant_requests_requested_by
    ON access_grant_requests (requested_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_grant_requests_target_user
    ON access_grant_requests (target_user_id, created_at DESC);

ALTER TABLE temporary_permission_grants
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'active',
    ADD COLUMN IF NOT EXISTS valid_from TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS single_use BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consumed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS consumed_at TIMESTAMPTZ;

UPDATE temporary_permission_grants
   SET valid_from = granted_at
 WHERE valid_from IS NULL;

ALTER TABLE temporary_permission_grants
    ALTER COLUMN valid_from SET NOT NULL;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('access.grant_request.view', 'View access grant requests', 'access', 'medium', FALSE, 'active'),
    ('access.grant_request.apply', 'Apply for high-risk access grants', 'access', 'high', FALSE, 'active'),
    ('access.grant_request.review', 'Review high-risk access grant requests', 'access', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'access.grant_request.view',
        'access.grant_request.apply',
        'access.grant_request.review'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

WITH high_risk(permission_code, risk_level) AS (
    VALUES
        -- Funds, write-off, refund, adjustment.
        ('finance.ar_ap.manage', 'high'),
        ('finance.payment.register', 'high'),
        ('finance.daily_close.lock', 'high'),
        ('finance.payment_intent.create', 'high'),
        ('finance.refund.create', 'high'),
        ('finance.daily_close.manage', 'high'),
        ('finance.locked_backfill.request', 'high'),
        ('finance.locked_backfill.approve', 'high'),
        ('finance.inventory_variance.post', 'high'),
        ('finance.payment_gateway.manage', 'high'),
        ('finance.turnover_rent.bill', 'high'),
        ('finance.deposit.manage', 'high'),
        ('finance.cam.allocate', 'high'),
        ('finance.cam.trueup', 'high'),
        ('finance.property_fee.manage', 'high'),
        ('refund.authority.manage', 'high'),
        ('bar.bill.adjust', 'high'),
        ('bar.bill.refund', 'high'),
        ('customer.stored_value.topup', 'high'),
        ('customer.stored_value.refund', 'high'),
        ('customer.stored_value.freeze', 'high'),
        ('merchant.billing.manage', 'high'),
        ('merchant.billing.dunning.record', 'high'),
        ('inventory.stock.adjust', 'high'),
        ('inventory.stocktake.variance_review', 'high'),
        -- Privilege escalation, cross-merchant, cross-complex governance.
        ('access.permission_template.manage', 'high'),
        ('access.permission_template.apply', 'high'),
        ('access.permission_template.revoke', 'high'),
        ('access.role.manage', 'high'),
        ('access.role.assign', 'high'),
        ('access.temporary_grant.create', 'high'),
        ('access.temporary_grant.revoke', 'high'),
        ('access.user.manage', 'high'),
        ('access.staff_level.manage', 'high'),
        ('access.grant_request.review', 'critical'),
        ('governance.supervisor_review.manage', 'high'),
        ('governance.supervisor_review.approve', 'critical'),
        ('entity.binding.manage', 'high'),
        ('merchant.profile.manage', 'high'),
        ('governance.project.manage', 'high'),
        ('governance.space.manage', 'high'),
        ('contract.approval.flow.manage', 'high'),
        ('contract.approve.gm', 'high'),
        ('contract.settlement.manage', 'high'),
        ('contract.settlement.dispute.resolve', 'high'),
        ('leasing.brand.blacklist', 'high'),
        ('leasing.convert', 'high'),
        -- Sensitive cost/internal settlement price data.
        ('bar.product.view', 'high'),
        ('bar.product.manage', 'high'),
        -- High-risk operations: sold-out, reverse settlement, saved bottle void, manual release.
        ('bar.product.sold_out', 'high'),
        ('bar.bill.reopen', 'high'),
        ('bar.order_item.void', 'high'),
        ('bar.order_item.void_decide', 'high'),
        ('bar.saved_bottle.manage', 'high'),
        ('ticket.verify.reverse', 'high'),
        ('amusement.verification.override', 'high'),
        ('amusement.over_capacity_policy.configure', 'high')
)
UPDATE permissions p
   SET risk_level = high_risk.risk_level,
       requires_approval = TRUE,
       status = 'active'
  FROM high_risk
 WHERE p.permission_code = high_risk.permission_code;
