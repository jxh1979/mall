-- 文件版本：V0.01
-- 第十三批 B 段 ROOT-03（B-identity）：客人终端开关口径、注册与注销计数预警、会员身份状态。

-- R33 退役 V097 种的 customer.native_client.enabled 行：无任何代码读取、关着也不拒任何请求，原因写「关时走 Web 客户端代偿」，
-- 与 PGM 第 745 行「Web 前端（业务人员，客人无网页端）」、第 737 行「关闭后菜单不可见或不可点，接口也要拒绝业务动作」相反。
-- 客人两端开关以 V197 的 terminal.enabled（app、miniapp）为唯一口径。前向删除，不改 V097 原文。
DELETE FROM terminal_capability_switches
 WHERE id = 'tcs_native_client_off' AND capability_code = 'customer.native_client.enabled';

-- R22 ENUM 第 68 行「同一手机号在一年内注销与注册合计超过后台配置的次数上限的，触发异常预警交风控查看处置（业务参数，
-- 全平台统一可配，出厂默认一年内合计 3 次，须同时配置上下限）；预警只作提示，不自动阻断注册」。
-- 出厂只定值 3；上下限 HTML 未给数值，出厂留空，后台改值时值与上下限须同时填写。
CREATE TABLE customer_churn_alert_settings (
    id VARCHAR(16) PRIMARY KEY,
    threshold INTEGER NOT NULL,
    min_threshold INTEGER,
    max_threshold INTEGER,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_customer_churn_alert_settings_id CHECK (id = 'global'),
    CONSTRAINT chk_customer_churn_alert_settings_bounds CHECK (
        (min_threshold IS NULL AND max_threshold IS NULL)
        OR (min_threshold >= 1 AND min_threshold <= threshold AND threshold <= max_threshold))
);
INSERT INTO customer_churn_alert_settings (id, threshold) VALUES ('global', 3);

-- ENUM 第 68 行「为完成上述计数及注销后重新注册的新人资格判定，系统仅留存注销事件与该手机号的对应记录至一年统计窗口届满；
-- 该记录仅用于上述两项目的、不与新账户建立任何关联」：只存手机号盲索引与两个时点，不存客人编号。
CREATE TABLE customer_deregistration_marks (
    id VARCHAR(64) PRIMARY KEY,
    phone_hash CHAR(64) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    deregistered_at TIMESTAMPTZ NOT NULL,
    purge_after TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_customer_deregistration_marks_phone ON customer_deregistration_marks (phone_hash, deregistered_at);
CREATE INDEX idx_customer_deregistration_marks_purge ON customer_deregistration_marks (purge_after);

-- R23 BSM 第 819 行「受限满后台配置时长（业务参数，按项目可配，出厂默认 90 天，须同时配置上下限）自动恢复有效」。
-- '__global__' 为出厂回落行（只定 90 天，上下限 HTML 未给数值留空）；项目行值与上下限须同时填写。
CREATE TABLE customer_restriction_settings (
    project_id VARCHAR(64) PRIMARY KEY,
    duration_days INTEGER NOT NULL,
    min_days INTEGER,
    max_days INTEGER,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_customer_restriction_settings_bounds CHECK (
        (min_days IS NULL AND max_days IS NULL)
        OR (min_days >= 1 AND min_days <= duration_days AND duration_days <= max_days))
);
INSERT INTO customer_restriction_settings (project_id, duration_days) VALUES ('__global__', 90);

-- R23 ENUM 第 68 行「将客人列入黑名单即为关闭其账户，黑名单全平台通用，由保安团队申请、夜班值班经理批准、娱乐平台负责人批准」；
-- BSM 第 819 行「解除黑名单须按列入时同级审批链（夜班值班经理批准、娱乐平台负责人批准）复核，填原因留痕；
-- 解除后原账户保持已关闭，该手机号自解除时起允许重新注册」。两级都批准才执行；同一客人同时只有一笔待批申请。
CREATE TABLE customer_blacklist_requests (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    project_id VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    night_manager_approved_by VARCHAR(64),
    night_manager_approved_at TIMESTAMPTZ,
    platform_head_approved_by VARCHAR(64),
    platform_head_approved_at TIMESTAMPTZ,
    rejected_by VARCHAR(64),
    rejected_at TIMESTAMPTZ,
    CONSTRAINT chk_customer_blacklist_requests_action CHECK (action IN ('list', 'release')),
    CONSTRAINT chk_customer_blacklist_requests_status CHECK (status IN ('pending', 'approved', 'rejected'))
);
CREATE UNIQUE INDEX uq_customer_blacklist_requests_pending
    ON customer_blacklist_requests (customer_id) WHERE status = 'pending';
CREATE INDEX idx_customer_blacklist_requests_status ON customer_blacklist_requests (status, requested_at DESC);

-- 解除黑名单后手机号自解除时起允许重新注册：原账户保持已关闭，手机号三列置空以释放唯一名额。
ALTER TABLE customers ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE customers ALTER COLUMN masked_phone DROP NOT NULL;

-- 权限点只入字典（中文名），不预绑岗位模板：仓内无「保安团队」「娱乐平台负责人」模板，由既有授权流程授予。
-- 第一级批准人按排班判定（申请所在项目此刻在班的夜班值班经理），不设权限点。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('customer.blacklist.request', '申请将客人列入或解除黑名单（保安团队）', 'customer', 'high', FALSE, 'active'),
    ('customer.blacklist.approve', '终审客人列入或解除黑名单（娱乐平台负责人）', 'customer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON customer_churn_alert_settings, customer_deregistration_marks,
            customer_restriction_settings, customer_blacklist_requests TO complex_app';
    END IF;
END $$;
