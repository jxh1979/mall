CREATE TABLE IF NOT EXISTS merchant_fee_notification_recipient (
    id              VARCHAR(80) PRIMARY KEY,
    merchant_id     VARCHAR(80) NOT NULL,
    fee_event       VARCHAR(32),
    recipient_name  VARCHAR(120) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    created_by      VARCHAR(80) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uk_merchant_fee_notification_recipient_event
        UNIQUE NULLS NOT DISTINCT (merchant_id, fee_event),
    CONSTRAINT chk_merchant_fee_notification_recipient_event
        CHECK (fee_event IS NULL OR fee_event IN ('rent', 'utilities', 'property', 'renewal')),
    CONSTRAINT chk_merchant_fee_notification_recipient_status
        CHECK (status IN ('active', 'inactive'))
);

CREATE INDEX IF NOT EXISTS idx_merchant_fee_notification_recipient_active
    ON merchant_fee_notification_recipient (merchant_id, fee_event)
 WHERE status = 'active'
   AND deleted_at IS NULL;
