-- 条目3 空间类型字典（2026-06-30 拍板：硬校验 + 后台可由管理员维护 + 前端下拉）。
-- 平台级参考数据（无 project_id，全综合体共享）。createSpace 硬校验 space_type ∈ 本表 active 的 type_code。
CREATE TABLE IF NOT EXISTS space_type_dictionary (
    id VARCHAR(64) PRIMARY KEY,
    type_code VARCHAR(64) NOT NULL UNIQUE,
    type_name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active/disabled
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- 种子默认 8 类（管理员后续可在后台增改/停用）。
INSERT INTO space_type_dictionary (id, type_code, type_name, sort_order, status, created_at, updated_at)
VALUES
    ('sptype_building',    'building',    '楼栋',       10,  'active', now(), now()),
    ('sptype_floor',       'floor',       '楼层',       20,  'active', now(), now()),
    ('sptype_zone',        'zone',        '区块/区域',  30,  'active', now(), now()),
    ('sptype_shop',        'shop',        '商铺/铺位',  40,  'active', now(), now()),
    ('sptype_public_area', 'public_area', '公共区域',   50,  'active', now(), now()),
    ('sptype_parking',     'parking',     '车位/车库',  60,  'active', now(), now()),
    ('sptype_booth',       'booth',       '摊位/展位',  70,  'active', now(), now()),
    ('sptype_other',       'other',       '其他',       999, 'active', now(), now())
ON CONFLICT (type_code) DO NOTHING;
