-- 文件版本：V0.01
-- V619（第十八批 AH 段第 218 条 218-R1／218-R2）：高频出入场预警阈值，按项目可配。
-- 依据：BSM:800「对高频出入场的须调看出入录像核查，以防止将票借给他人使用：一晚出入次数或连续两日出入合计次数达到后台配置的阈值即触发核查
--   （业务参数，按项目可配，出厂默认一晚 4 次、连续两日合计 7 次，须同时配置上下限）……由系统自动预警，楼宇保安团队据预警调阅录像」。
-- '__global__' 为出厂回落行（一晚 4 次、连续两日 7 次；上下限 HTML 未给数值留空）；项目行阈值与上下限须同时填写。表结构与 CHECK 同 V537
-- customer_restriction_settings。
-- 同一迁移另含第 219 条补充（见文末）。

CREATE TABLE customer_frequent_entry_alert_settings (
    project_id VARCHAR(64) PRIMARY KEY,
    night_threshold INTEGER NOT NULL,
    night_min INTEGER,
    night_max INTEGER,
    two_day_threshold INTEGER NOT NULL,
    two_day_min INTEGER,
    two_day_max INTEGER,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_customer_frequent_entry_night_bounds CHECK (
        (night_min IS NULL AND night_max IS NULL)
        OR (night_min >= 1 AND night_min <= night_threshold AND night_threshold <= night_max)),
    CONSTRAINT chk_customer_frequent_entry_two_day_bounds CHECK (
        (two_day_min IS NULL AND two_day_max IS NULL)
        OR (two_day_min >= 1 AND two_day_min <= two_day_threshold AND two_day_threshold <= two_day_max))
);
INSERT INTO customer_frequent_entry_alert_settings (project_id, night_threshold, two_day_threshold) VALUES ('__global__', 4, 7);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON customer_frequent_entry_alert_settings TO complex_app';
    END IF;
END $$;

-- 第十八批 AH 段第 219 条补充：同一权益同一场次可有多条未释放的成功入场。BSM:780「按可用次数逐次核销（每用一次扣一次，剩余为 0 才算用完）」、
-- BSM:800「该窗口内票面附带的各项权益……按完整一份配足、完整可用」、ENUM:144「含一人多次票当天多次入场、当天不限次数一类表述」：多次票、
-- 当天无限在同一开放场次里再次入场照常（每次入场在玩人数加一，由出场自动扣减、场次结束与营业日结束释放，BSM:1263）。出场感应未接时入场记录
-- 留到场次或营业日结束才释放，V234 的这条唯一约束会让第二次入场整笔回滚，故去掉；并发扣次仍由权益行锁与原子扣次守住。
DROP INDEX IF EXISTS uk_amusement_verifications_active_entry;
