-- V226 结算重构批3-2(酒吧入场资格「场次规则」PRD §6.2):入场资格权益按**场次**核销。
--   口径:入场资格核销单位=场次(一个"开业→歇业"周期)。客人当场次入过场后,该场次内持续进出、不重复消耗;
--         即使门票有效期未到,一旦过了首入场场次(下一个开歇业周期)即作废;场次按各酒吧营业时间表自动划。
--   本迁移:①entitlement_templates/issued_entitlements 加 consumption_window(消耗单位判别位);
--          ②venue_business_hours(酒吧营业时间表)、venue_sessions(场次实例)、entitlement_entry_records(入场记录)三表。
--   核销时的场次解析(惰性开场)与入场判定(放行不消耗/过场作废/首入消耗)在配套代码(批3-2)。
--   仅加列+建表,不改现有逻辑;consumption_window 允许 NULL,回填 NULL→per_use(现状逐次扣不变)。

-- ① 消耗单位:per_use(逐次扣,现状)|per_session(按场次)|per_day(按天)|unlimited_in_day(当天无限),后两值留批3-3。
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS consumption_window VARCHAR(16);
ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS consumption_window VARCHAR(16);

-- 回填现状:NULL→per_use(逐次扣,行为不变)。新发放从模板复制;模板未显式配置亦回退 per_use。
UPDATE entitlement_templates SET consumption_window = 'per_use' WHERE consumption_window IS NULL;
UPDATE issued_entitlements   SET consumption_window = 'per_use' WHERE consumption_window IS NULL;

-- 取值约束(允许 NULL 向后兼容;判定层对 NULL/其余值走现状 per_use 逐次扣)。
ALTER TABLE entitlement_templates
    ADD CONSTRAINT chk_et_consumption_window
    CHECK (consumption_window IS NULL OR consumption_window IN ('per_use','per_session','per_day','unlimited_in_day'));
ALTER TABLE issued_entitlements
    ADD CONSTRAINT chk_ie_consumption_window
    CHECK (consumption_window IS NULL OR consumption_window IN ('per_use','per_session','per_day','unlimited_in_day'));

-- ② 酒吧营业时间表:按 venue_id + 星期几 配开/歇业时刻。场次按此惰性划(核销时判当前是否营业)。
--    跨零点营业(如 20:00→次日 02:00):close_time < open_time 视为次日歇业,场次解析在代码侧处理。
--    is_draft/effective_from 留草稿态与生效锚点(运营编辑先存草稿再发布);唯一键(venue_id,day_of_week)。
CREATE TABLE IF NOT EXISTS venue_business_hours (
    id             VARCHAR(64)  PRIMARY KEY,
    venue_id       VARCHAR(64)  NOT NULL,
    day_of_week    SMALLINT     NOT NULL CHECK (day_of_week BETWEEN 0 AND 6), -- 0=周日..6=周六(Java DayOfWeek 归一在代码侧)
    open_time      TIME         NOT NULL,
    close_time     TIME         NOT NULL,
    is_draft       BOOLEAN      NOT NULL DEFAULT FALSE,
    effective_from DATE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vbh_venue_dow UNIQUE (venue_id, day_of_week)
);

-- ③ 场次实例:一个"开业→歇业"周期。惰性开场(核销时无当日 open 场次则 INSERT);歇业关场留后续 job。
--    唯一键(venue_id,session_date)=每 venue 每自然日一场(跨零点场归属其 open 当日)。
CREATE TABLE IF NOT EXISTS venue_sessions (
    id           VARCHAR(64)  PRIMARY KEY,
    venue_id     VARCHAR(64)  NOT NULL,
    session_date DATE         NOT NULL,
    opened_at    TIMESTAMPTZ  NOT NULL,
    closed_at    TIMESTAMPTZ,
    status       VARCHAR(16)  NOT NULL DEFAULT 'open' CHECK (status IN ('open','closed')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_vs_venue_date UNIQUE (venue_id, session_date)
);

-- ④ 入场记录:某权益在某场次的首次入场留痕。唯一键(entitlement_id,session_id)=同权益同场次仅一条
--    (并发重复入场靠此唯一键防重);查该权益是否曾在别的场次入场→判定"过场作废"。
CREATE TABLE IF NOT EXISTS entitlement_entry_records (
    id             VARCHAR(64)  PRIMARY KEY,
    entitlement_id VARCHAR(64)  NOT NULL,
    session_id     VARCHAR(64)  NOT NULL,
    first_entry_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_eer_ent_session UNIQUE (entitlement_id, session_id)
);
CREATE INDEX IF NOT EXISTS idx_eer_entitlement ON entitlement_entry_records (entitlement_id);
