-- BI S0(2026-07-02 用户拍板):经营日切分按业务域可配——不同业务的「一天」从不同时点切。
-- 酒吧=06:00(晚6点到次日早6点算一天)、零售/餐饮商户=10:00、其余=04:00;
-- platform 行=平台跨域汇总与财务日结的对齐基准(revenue.net 等跨域相加指标一律用它,防混窗)。
-- 后台经营参数页(ADM-ANA-003)可改,BusinessDayResolver 60s 缓存内生效。
CREATE TABLE IF NOT EXISTS business_day_cutoff_config (
    domain VARCHAR(32) PRIMARY KEY,
    cutoff_hour INT NOT NULL CHECK (cutoff_hour BETWEEN 0 AND 23),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by VARCHAR(64)
);

INSERT INTO business_day_cutoff_config (domain, cutoff_hour) VALUES
    ('platform', 4),
    ('finance', 4),
    ('ticketing', 4),
    ('amusement', 4),
    ('customer', 4),
    ('inventory', 4),
    ('governance', 4),
    ('bar', 6),
    ('merchant', 10)
ON CONFLICT (domain) DO NOTHING;
