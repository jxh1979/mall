-- V172 积分会员间转赠下线清理。
-- 依据：业务 HTML 明确「积分不支持会员间转赠」+ 2026-06-24 用户裁决「积分转赠下线」。
-- 背景：V090 曾建 customer_points_transfers / customer_points_transfer_limits 表 + 转赠权限，
--   但服务层从未接线上线（无活路由），Java 记录类 / DTO / repo 方法已随本次清理移除（孤儿死代码）。
--   本迁移删除遗留表与权限种子，收口转赠下线。
-- 安全性：两表为叶子表（无下游 FK 引用、单综合体下无写入路径，数据为空）；
--   permissions / role_permissions 非 append-only 保护表（见 data-isolation spec §9），可安全 DELETE。

-- 1) 先撤销角色授权（role_permissions 引用 permissions，先删子行）
DELETE FROM role_permissions
 WHERE permission_code IN ('customer.points.transfer', 'customer.points.transfer_limit');

-- 2) 删除转赠权限点
DELETE FROM permissions
 WHERE permission_code IN ('customer.points.transfer', 'customer.points.transfer_limit');

-- 3) 删除转赠遗留表
DROP TABLE IF EXISTS customer_points_transfers;
DROP TABLE IF EXISTS customer_points_transfer_limits;
