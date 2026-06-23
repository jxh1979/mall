-- V155 订卡邀请函（扩展卡座预订；不另起独立实体，邀请函=预订上的列 + 成员为预订子表）：
-- 业务（用户 2026-06-22 口述 + 2026-06-23 拍板两步确认）：订卡客人无需买通票、只进定卡酒吧；持函人可把邀请函
--   （二维码，绑酒吧/卡座/卡座可进人数）转发给朋友，朋友在客人端「确认接受邀请」先占一个名额（登记是谁来），
--   到店再扫码核验入场；「已确认 + 已入场」总人数 ≤ 卡座可进人数（capacity）。
-- ① bar_reservations 增邀请函列：token（不可猜随机串，唯一）+ signature（HMAC 防伪，范式同门票 TicketCredentialSigner）
--    + capacity（签发时 bar_tables.capacity 快照，作总人数上限）+ status（none/active/revoked）+ issued_at。
ALTER TABLE bar_reservations
    ADD COLUMN IF NOT EXISTS invitation_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS invitation_signature VARCHAR(256),
    ADD COLUMN IF NOT EXISTS invitation_capacity INTEGER,
    ADD COLUMN IF NOT EXISTS invitation_status VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS invitation_issued_at TIMESTAMPTZ;

ALTER TABLE bar_reservations
    DROP CONSTRAINT IF EXISTS chk_bar_reservations_invitation_status;
ALTER TABLE bar_reservations
    ADD CONSTRAINT chk_bar_reservations_invitation_status
    CHECK (invitation_status IN ('none', 'active', 'revoked'));

ALTER TABLE bar_reservations
    DROP CONSTRAINT IF EXISTS chk_bar_reservations_invitation_capacity;
ALTER TABLE bar_reservations
    ADD CONSTRAINT chk_bar_reservations_invitation_capacity
    CHECK (invitation_capacity IS NULL OR invitation_capacity >= 0);

-- token 唯一（仅对已签发行；NULL 不参与唯一），供 confirm/entry 按 token 反查预订。
CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_reservations_invitation_token
    ON bar_reservations (invitation_token)
    WHERE invitation_token IS NOT NULL;

-- ② 邀请函成员（预订子表）：每个确认的朋友 + 持函人各一行。confirmed→entered 状态机；
--    UNIQUE(reservation_id, member_identifier) 保证同一朋友确认幂等、不重复占名额。
CREATE TABLE IF NOT EXISTS bar_reservation_invitation_members (
    id VARCHAR(64) PRIMARY KEY,
    reservation_id VARCHAR(64) NOT NULL REFERENCES bar_reservations(id),
    bar_id VARCHAR(64) NOT NULL,
    member_identifier VARCHAR(128) NOT NULL,
    member_customer_id VARCHAR(64),
    is_holder BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'confirmed',
    confirmed_at TIMESTAMPTZ NOT NULL,
    entered_at TIMESTAMPTZ,
    device_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_bar_invitation_member UNIQUE (reservation_id, member_identifier),
    CONSTRAINT chk_bar_invitation_member_status CHECK (status IN ('confirmed', 'entered', 'revoked'))
);

CREATE INDEX IF NOT EXISTS idx_bar_invitation_members_reservation
    ON bar_reservation_invitation_members (reservation_id);

-- ③ 权限 seed：邀请函签发/吊销（staff 高危）。朋友确认走客人端自助（X-Customer-Token 解析 customer，
--    能力 "not_applicable"，不经 requirePermission，对齐 createBarReservation），故无需 customer 权限码。
--    入场核验复用既有 bar.table.guest_checkin。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.reservation.invitation.manage', 'Issue or revoke a card-seat reservation invitation (QR with capacity)', 'bar', 'high', FALSE, 'active')
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
        'bar.reservation.invitation.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
