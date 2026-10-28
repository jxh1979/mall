-- 文件版本：V0.01
-- V559：第十八批 C 段第 15 条 员工代订免收定金、爽约记经手员工、爽约率上限、自动停用代订权限与店长复核恢复。
-- 依据：BSM 第 1062 行「员工代客订台不收定金……员工代订的记一笔爽约到经手员工名下」；ENUM 第 103 行订台定金状态「免收（员工代订）」；
--   BSM 第 1468 行「客人超过最晚到店时间未到店的，系统自动释放该台位，并记一笔爽约到经手员工名下……经手员工的爽约率超过后台配置的上限时
--   （业务参数，按酒吧可配，须同时配置上下限），自动停用其代订权限，由店长复核后恢复」；BSM 第 819 行；第十八批裁决 3「按自然月：每个自然月单独统计」。
-- 铁律 8：不为存量数据回填；只扩闭集、加列与新表。

-- 1) 订台定金状态闭集加「免收（员工代订）」staff_waived（其余照 V542）。
ALTER TABLE bar_reservations DROP CONSTRAINT IF EXISTS chk_bar_reservations_deposit_status;
ALTER TABLE bar_reservations ADD CONSTRAINT chk_bar_reservations_deposit_status
    CHECK (deposit_status IN ('not_required', 'pending', 'paid', 'settled', 'timeout_released', 'staff_waived'));

-- 2) 员工代订爽约率上限：按酒吧可配（酒吧＝merchant_type='bar' 的商户，V267），值与上下限同配（范式同 V521 待清台提醒时限），整数百分比；
--    HTML 未给出厂默认：未配置＝三列皆空＝不自动停用（BSM 第 521 行「未填写上下限的参数不得上线」）。
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS staff_booking_no_show_limit_pct INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS staff_booking_no_show_limit_min_pct INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS staff_booking_no_show_limit_max_pct INTEGER;
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_staff_booking_no_show_limit;
ALTER TABLE merchants ADD CONSTRAINT chk_merchants_staff_booking_no_show_limit CHECK (
    (staff_booking_no_show_limit_pct IS NULL AND staff_booking_no_show_limit_min_pct IS NULL AND staff_booking_no_show_limit_max_pct IS NULL)
    OR (staff_booking_no_show_limit_pct IS NOT NULL AND staff_booking_no_show_limit_min_pct IS NOT NULL AND staff_booking_no_show_limit_max_pct IS NOT NULL
        AND staff_booking_no_show_limit_min_pct >= 0
        AND staff_booking_no_show_limit_min_pct <= staff_booking_no_show_limit_pct
        AND staff_booking_no_show_limit_pct <= staff_booking_no_show_limit_max_pct
        AND staff_booking_no_show_limit_max_pct <= 100)
);
COMMENT ON COLUMN merchants.staff_booking_no_show_limit_pct IS '员工代订爽约率上限（%）：经手员工本自然月爽约率超过即自动停用其代订权限（未配置＝空，不自动停用；与上下限同配）';
COMMENT ON COLUMN merchants.staff_booking_no_show_limit_min_pct IS '员工代订爽约率上限已配置下限（%，与值、上限同配）';
COMMENT ON COLUMN merchants.staff_booking_no_show_limit_max_pct IS '员工代订爽约率上限已配置上限（%，与值、下限同配）';

-- 3) 爽约留痕：超时释放的员工代订记一笔爽约到经手员工（sales_staff_id）名下的时刻；只由超时释放作业写。
ALTER TABLE bar_reservations ADD COLUMN IF NOT EXISTS no_show_at TIMESTAMPTZ;
COMMENT ON COLUMN bar_reservations.no_show_at IS '员工代订超过最晚到店时间未到店、系统自动释放时记爽约的时刻（计入经手员工爽约率；客人自助订台不记）';

-- 4) 员工代订权限停用：经手员工本自然月爽约率超上限自动停用（一人一酒吧同时至多一条停用中），由店长复核后恢复。
CREATE TABLE IF NOT EXISTS bar_staff_booking_suspensions (
    id VARCHAR(128) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    staff_user_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    stat_month VARCHAR(7) NOT NULL,
    no_show_count INTEGER NOT NULL,
    booking_count INTEGER NOT NULL,
    limit_pct INTEGER NOT NULL,
    trigger_reservation_id VARCHAR(64) NOT NULL,
    suspended_at TIMESTAMPTZ NOT NULL,
    restored_by VARCHAR(64),
    restored_at TIMESTAMPTZ,
    CONSTRAINT chk_bar_staff_booking_suspensions_status CHECK (status IN ('suspended', 'restored'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_staff_booking_suspensions_active
    ON bar_staff_booking_suspensions (merchant_id, staff_user_id) WHERE status = 'suspended';
COMMENT ON TABLE bar_staff_booking_suspensions IS '员工代订权限停用记录（merchant_id＝酒吧；stat_month＝触发时统计的营业月 yyyy-MM）';

-- 5) 店长复核恢复权限（范式同 V521；由运营分配给店长角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.staff_booking.restore', '复核恢复员工代订权限', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.staff_booking.restore')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON bar_staff_booking_suspensions TO complex_app';
    END IF;
END $$;
