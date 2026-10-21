-- 文件版本：V0.01
-- V524 TK-14 票务核销异常复核（ENUM「核销记录状态」：「平台已记录核销成功并扣减次数，但现场未放行或实际权益交付存在争议、需要主管核实时，
-- 进入异常复核并保留原成功记录与扣次事实……不自动退款」；ENUM「异常状态的统一出口」：主管选「更正后恢复」或「作废并冲正」之一处置，
-- 填写原因并留痕，异常未处置完毕所属日结不得确认锁定；CG #45）。
-- 提起：新增一条 status=anomaly 的核销记录指向原成功记录（原记录与扣次不动）；同一原记录同时只能有一条待处置异常。
-- 处置：anomaly_resolution 取 restored（更正后恢复）/ voided_reversed（作废并冲正），连同处置人、时间、原因写在异常记录上。
ALTER TABLE ticket_verifications
    ADD COLUMN IF NOT EXISTS anomaly_of_verification_id VARCHAR(64) REFERENCES ticket_verifications(id),
    ADD COLUMN IF NOT EXISTS anomaly_resolution VARCHAR(32),
    ADD COLUMN IF NOT EXISTS anomaly_resolved_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS anomaly_resolved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS anomaly_resolution_reason VARCHAR(1000);

ALTER TABLE ticket_verifications
    ADD CONSTRAINT chk_ticket_verifications_anomaly_resolution
    CHECK (anomaly_resolution IS NULL OR anomaly_resolution IN ('restored', 'voided_reversed'));

ALTER TABLE ticket_verifications
    ADD CONSTRAINT chk_ticket_verifications_anomaly_source
    CHECK (status <> 'anomaly' OR anomaly_of_verification_id IS NOT NULL);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_verifications_open_anomaly
    ON ticket_verifications (anomaly_of_verification_id)
    WHERE status = 'anomaly' AND anomaly_resolution IS NULL;

-- 主管处置权限（高风险，与 ticket.verify.reverse 同级）；授予已持有冲正权限的角色与权限模板，基线 V261 同授平台管理员。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('ticket.verify.anomaly.resolve', '处置票务核销异常', 'ticketing', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'ticket.verify.anomaly.resolve'
  FROM role_permissions
 WHERE permission_code = 'ticket.verify.reverse'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'ticket.verify.anomaly.resolve')
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT template_id, 'ticket.verify.anomaly.resolve'
  FROM permission_template_permissions
 WHERE permission_code = 'ticket.verify.reverse'
ON CONFLICT DO NOTHING;
