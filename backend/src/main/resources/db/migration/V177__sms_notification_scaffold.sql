-- A1 SMS notification sending scaffold + two-level configurable switches.
-- Reuses terminal_capability_switches as the platform capability-switch source.
-- Main SMS switch is explicitly seeded OFF because resolveEffectiveSwitch defaults to enabled when no row exists.

INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_sms_notification_off', '*', 'sms_notification', 'global', 'global', FALSE,
     'SMS notification master switch: default off until provider credentials, templates, and operational runbook are accepted', now(), now()),
    ('tcs_sms_notify_stored_wine_domain', '*', 'sms.notify.stored_wine', 'global', 'global', TRUE,
     'SMS notification domain switch for stored wine lifecycle notices; gated by sms_notification master switch', now(), now()),
    ('tcs_sms_notify_stored_value_domain', '*', 'sms.notify.stored_value', 'global', 'global', TRUE,
     'SMS notification domain switch for stored value balance notices; gated by sms_notification master switch', now(), now()),
    ('tcs_sms_notify_merchant_fee_domain', '*', 'sms.notify.merchant_fee', 'global', 'global', TRUE,
     'SMS notification domain switch for merchant fee billing notices; gated by sms_notification master switch', now(), now()),
    ('tcs_sms_notify_stored_wine_deposit', '*', 'sms.notify.stored_wine.deposit', 'global', 'global', TRUE,
     'SMS notification event switch for stored wine deposit confirmation notices', now(), now()),
    ('tcs_sms_notify_stored_wine_claim', '*', 'sms.notify.stored_wine.claim', 'global', 'global', TRUE,
     'SMS notification event switch for stored wine claim pickup notices', now(), now()),
    ('tcs_sms_notify_stored_wine_expiring', '*', 'sms.notify.stored_wine.expiring', 'global', 'global', TRUE,
     'SMS notification event switch for stored wine expiring reminder notices', now(), now()),
    ('tcs_sms_notify_stored_wine_expired', '*', 'sms.notify.stored_wine.expired', 'global', 'global', TRUE,
     'SMS notification event switch for stored wine expired notices', now(), now()),
    ('tcs_sms_notify_stored_value_topup', '*', 'sms.notify.stored_value.topup', 'global', 'global', TRUE,
     'SMS notification event switch for stored value top-up notices', now(), now()),
    ('tcs_sms_notify_stored_value_consume', '*', 'sms.notify.stored_value.consume', 'global', 'global', TRUE,
     'SMS notification event switch for stored value consumption notices', now(), now()),
    ('tcs_sms_notify_merchant_fee_rent', '*', 'sms.notify.merchant_fee.rent', 'global', 'global', TRUE,
     'SMS notification event switch for merchant rent fee notices', now(), now()),
    ('tcs_sms_notify_merchant_fee_utilities', '*', 'sms.notify.merchant_fee.utilities', 'global', 'global', TRUE,
     'SMS notification event switch for merchant utilities fee notices', now(), now()),
    ('tcs_sms_notify_merchant_fee_property', '*', 'sms.notify.merchant_fee.property', 'global', 'global', TRUE,
     'SMS notification event switch for merchant property fee notices', now(), now()),
    ('tcs_sms_notify_merchant_fee_renewal', '*', 'sms.notify.merchant_fee.renewal', 'global', 'global', TRUE,
     'SMS notification event switch for merchant renewal fee notices', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS notification_send_log (
    id            VARCHAR(64) PRIMARY KEY,
    request_id    VARCHAR(128) NOT NULL UNIQUE,
    channel       VARCHAR(32) NOT NULL,
    domain        VARCHAR(64) NOT NULL,
    event_key     VARCHAR(64) NOT NULL,
    recipient     VARCHAR(128) NOT NULL,
    template_code VARCHAR(128) NOT NULL,
    params        JSONB NOT NULL DEFAULT '{}'::jsonb,
    status        VARCHAR(32) NOT NULL,
    reason        TEXT,
    provider_ref  VARCHAR(128),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_notification_send_log_channel CHECK (channel IN ('sms')),
    CONSTRAINT chk_notification_send_log_status CHECK (status IN (
        'pending', 'sent', 'failed', 'throttled', 'skipped_switch_off'
    ))
);

CREATE INDEX IF NOT EXISTS idx_notification_send_log_domain_event
    ON notification_send_log (domain, event_key, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_send_log_status
    ON notification_send_log (status, created_at DESC);
