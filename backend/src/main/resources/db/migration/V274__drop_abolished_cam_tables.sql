-- V274: 彻底下线 CAM 权重分摊 + 年度 true-up。
-- F-OPS-08(用户 2026-06 裁决)CAM 已由物业费每平米固定计费(不退不补)取代;
-- 本次同步移除全部持久化/服务/控制器/DTO/Record 代码(无外部调用方),故 drop 三张空表。
-- 依赖顺序:lines → runs → allocations(CASCADE 兜底残留外键)。

DROP TABLE IF EXISTS cam_trueup_lines CASCADE;
DROP TABLE IF EXISTS cam_trueup_runs CASCADE;
DROP TABLE IF EXISTS cam_allocations CASCADE;
