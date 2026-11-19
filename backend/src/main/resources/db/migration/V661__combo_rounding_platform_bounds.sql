-- 文件版本：V0.01
-- V661（第十八批 AW 段第 293 条 293-R1、第 339 条 339-R1）：通票凑整参数表去掉套票折扣归属、只留全平台一行、凑整允许误差加上下限。
-- 依据：BSM:754「凑整产生的差额按各业态价格比例，与折扣一并分摊到各域，由各域主管公司按份额各自承担或取得」
--   「允许误差可配（业务参数，全平台统一可配，出厂默认 5 元，须同时配置上下限）」；ENUM:193 同句。
-- 折扣与凑整差额一律按出票时按业态价格占比拆出的实付份额由各域承担，不再按折扣归属加回；
-- 上下限出厂留空（HTML 未给出厂上下限），首次调整须同填（同 V566 高价值判定阈值）。研发阶段无存量（UD-07），项目行直接删。
DELETE FROM ticket_combo_discount_configs WHERE project_id IS NOT NULL;

ALTER TABLE ticket_combo_discount_configs
    DROP COLUMN discount_bearer,
    DROP COLUMN split_platform_bps,
    ADD COLUMN rounding_tolerance_min_cent BIGINT,
    ADD COLUMN rounding_tolerance_max_cent BIGINT,
    ADD CONSTRAINT chk_tcdc_platform_only CHECK (project_id IS NULL),
    ADD CONSTRAINT chk_tcdc_rounding_tolerance_bounds CHECK (
        (rounding_tolerance_min_cent IS NULL) = (rounding_tolerance_max_cent IS NULL)
        AND (rounding_tolerance_min_cent IS NULL
             OR (rounding_tolerance_min_cent >= 0
                 AND rounding_tolerance_min_cent <= rounding_tolerance_cent
                 AND rounding_tolerance_cent <= rounding_tolerance_max_cent)));

COMMENT ON COLUMN ticket_combo_discount_configs.rounding_tolerance_min_cent IS '通票凑整允许误差下限（分）：BSM:754「全平台统一可配……须同时配置上下限」；与上限同空同填，出厂留空';
COMMENT ON COLUMN ticket_combo_discount_configs.rounding_tolerance_max_cent IS '通票凑整允许误差上限（分）：与下限同空同填，0 ≤ 下限 ≤ 允许误差 ≤ 上限';
