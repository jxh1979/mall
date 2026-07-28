-- V351 退酒原因码字典（用户 2026-07-28 批准出厂八条，后台可增删改）。
--
-- 【为什么】V345 把「结构化原因码」落成了**正则形状校验**（^[a-z][a-z0-9_]{1,31}$）：
--   店员打 `asdf` 照样过闸，事后按原因码聚合退酒（哪家店质量问题多、哪个班次上错单多）全是垃圾数据；
--   而且前端是**自由文本框、要店员手打英文短码**，直接撞「所有终端零英文全中文」铁律。
--   本迁移建字典表，服务层改为「必须命中字典里生效的码」，前端改中文下拉——店员只选中文，不打英文。
--
-- 【为什么不复用通用字典】仓里没有通用字典设施（只有 V184 单用途的 space_type_dictionary）。
--   照 space_type_dictionary 的范式建一张单用途表，不为一处需求造通用字典框架。
--
-- 【软删铁律】下线走 status='inactive' + archive_reason，不物理删——历史退酒单还引用着这些码。

CREATE TABLE IF NOT EXISTS bar_void_reason_codes (
    code           VARCHAR(64) PRIMARY KEY,          -- 机读标识（保留英文短码，仅用于聚合/对账，不上屏）
    label_zh       VARCHAR(64)  NOT NULL,            -- 上屏中文（下拉里店员看到的就是这个）
    sort_order     INTEGER      NOT NULL DEFAULT 0,  -- 下拉排序
    status         VARCHAR(16)  NOT NULL DEFAULT 'active',
    archive_reason TEXT,                             -- 软删铁律：下线留理由
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_bvrc_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT chk_bvrc_code   CHECK (code ~ '^[a-z][a-z0-9_]{1,63}$')
);

CREATE INDEX IF NOT EXISTS idx_bar_void_reason_codes_active
    ON bar_void_reason_codes (sort_order) WHERE status = 'active';

-- 出厂八条（用户 2026-07-28 逐条批准）。other 的中文标签自带「必须同时填写说明」提示，
-- 落地上由服务层既有的 reason 必填保证（退酒本来就必填自由文本说明），不另加列。
INSERT INTO bar_void_reason_codes (code, label_zh, sort_order) VALUES
    ('quality_defect',     '酒品质量问题',           10),
    ('wrong_item',         '上错酒 / 出错单',        20),
    ('customer_change',    '客人改单',               30),
    ('service_delay',      '出品超时',               40),
    ('damaged_in_service', '服务中损坏',             50),
    ('system_error',       '系统/录单错误',          60),
    ('goodwill',           '客诉安抚',               70),
    ('other',              '其他（必须同时填写说明）', 80)
ON CONFLICT (code) DO NOTHING;

-- 退酒流水的原因码收进字典：DB 层兜底，服务层已先拦「必须是生效码」。
-- 开发库该字段从未填过（无存量），直接加外键；软删的码仍在表内，历史单据引用不断。
ALTER TABLE bar_order_item_voids
    DROP CONSTRAINT IF EXISTS fk_bar_order_item_voids_reason_code;
ALTER TABLE bar_order_item_voids
    ADD CONSTRAINT fk_bar_order_item_voids_reason_code
    FOREIGN KEY (reason_code) REFERENCES bar_void_reason_codes (code);

-- 新表一律授权受限运行时（V296/V298/V338 教训：漏 GRANT → 运行时静默 permission denied）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON bar_void_reason_codes TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;
