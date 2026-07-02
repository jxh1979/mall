-- H1 节假日日历(2026-07-02 拍板):节假日对比(今年春节 vs 去年春节)+异常检测节假日免报。
-- 农历假期与调休每年按国务院公布,程序不可推算:2025 为已公布确定值预填种子;
-- 2026 及以后由经营参数页 URL 抓取(holiday-cn JSON 格式)或手动录入,不预填未核实数据。
CREATE TABLE IF NOT EXISTS holiday_calendar (
    id VARCHAR(64) PRIMARY KEY,
    holiday_name VARCHAR(64) NOT NULL,
    holiday_year INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'manual',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_name_year UNIQUE (holiday_name, holiday_year),
    CONSTRAINT chk_holiday_range CHECK (end_date >= start_date),
    CONSTRAINT chk_holiday_source CHECK (source IN ('seed', 'fetched', 'manual'))
);
CREATE INDEX IF NOT EXISTS idx_holiday_calendar_range ON holiday_calendar (start_date, end_date);

INSERT INTO holiday_calendar (id, holiday_name, holiday_year, start_date, end_date, source)
VALUES
    ('hol_2025_yuandan',  '元旦',   2025, '2025-01-01', '2025-01-01', 'seed'),
    ('hol_2025_chunjie',  '春节',   2025, '2025-01-28', '2025-02-04', 'seed'),
    ('hol_2025_qingming', '清明',   2025, '2025-04-04', '2025-04-06', 'seed'),
    ('hol_2025_laodong',  '劳动节', 2025, '2025-05-01', '2025-05-05', 'seed'),
    ('hol_2025_duanwu',   '端午',   2025, '2025-05-31', '2025-06-02', 'seed'),
    ('hol_2025_guoqing',  '国庆',   2025, '2025-10-01', '2025-10-08', 'seed')
ON CONFLICT (holiday_name, holiday_year) DO NOTHING;
