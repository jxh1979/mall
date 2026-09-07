-- V393：M006 失效重审（2026-09-07）R6-01。
-- PGM-1.10.1「以下五类权限授出前须先审批才生效……敏感经营数据的查看 / 导出 / 修改」「查看内部结算价……不因“平台管理员”身份自动获得，
-- 必须逐项单独授予并留痕」；BSM-1.3.4「敏感数据查看与导出（内部结算价、成本价、分账底价……）必须逐项单独授予并留痕」。
--   四个含内部结算价 / 成本价的权限码此前以 requires_approval=FALSE 播种（V258 / V296 / V301）；其中 bar.combo.view 为 medium，
--   被当作日常权限随平台管理员日常桥即时生效，其余三个 high 码可随角色 / 模板 / 定岗免审直发。与 V388 同法改为须审批：
--   不再进入平台管理员日常桥，授出须走高危授权审批（既有持有者不受影响，审批仅约束新的授出）。
UPDATE permissions SET requires_approval = TRUE
 WHERE permission_code IN ('bar.combo.view', 'bar.combo.manage', 'inventory.cost.view', 'voucher.internal_price.view');
