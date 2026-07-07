-- V267 纠正 V263：酒吧门店＝业态为「酒吧」的商户，而非独立 bars 门店主数据。
-- 用户拍板(2026-07-07)："酒吧门店就是商户本身"——酒吧＝merchant_type=bar 的商户，通过招商/商户准入正路引进，
-- 不该凭空新建独立门店。故：① 把 V263 建的 bars 行落成酒吧商户（保留友好名，不丢历史）；
-- ② 兜底把运营表里出现过但不在 bars/merchants 的 bar_id 也落成酒吧商户；③ DROP bars；④ 删 bar.venue.* 权限。
-- 幂等可重跑（IF EXISTS / NOT IN）；向后兼容，可独立回滚（bars 已无代码引用）。

-- ① bars 现有行 -> 酒吧商户（保留 bar_name 友好名）。
-- merchants 仅 5 列 NOT NULL：merchant_name/merchant_type/status/created_at/updated_at；其余可空，安全 seed。
INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
SELECT id, bar_name, 'bar', 'active', now(), now()
  FROM bars
 WHERE id NOT IN (SELECT id FROM merchants);

-- ② 兜底：运营表里出现过、但既不在 bars 也不在 merchants 的 DISTINCT bar_id，也落成酒吧商户
--    （name 用 bar_id 占位，避免选择器/历史单据成孤儿）。
INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
SELECT DISTINCT bar_id, bar_id, 'bar', 'active', now(), now()
  FROM (
        SELECT bar_id FROM bar_tables
        UNION SELECT bar_id FROM bar_products
        UNION SELECT bar_id FROM bar_combos
        UNION SELECT bar_id FROM bar_reservations
  ) t
 WHERE bar_id IS NOT NULL
   AND bar_id <> ''
   AND bar_id NOT IN (SELECT id FROM merchants);

-- ③ 删除独立门店主数据表（连带其索引、CHECK 约束一并随表消失）。已无代码引用。
DROP TABLE IF EXISTS bars CASCADE;

-- ④ 删除 V263 引入的酒吧门店权限（bar.venue.*）。先解绑角色再删权限本体。
DELETE FROM role_permissions WHERE permission_code LIKE 'bar.venue.%';
DELETE FROM permissions WHERE permission_code LIKE 'bar.venue.%';
