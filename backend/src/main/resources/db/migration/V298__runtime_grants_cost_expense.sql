-- V298: 补授 V296/V297 新表的生产受限运行时权限(部署后生产实测 500 暴露)。
-- 根因:V140 的默认授权按建表角色生效,V296/V297 新表未吃到,且两迁移漏了 V277 以来
-- "新表显式 GRANT 给 complex_app" 的惯例;测试库以 owner 连接故未暴露。
-- 口径:成本状态行需移动加权滚动更新 = SELECT/INSERT/UPDATE(无 DELETE);
--       费用台账仅追加 = SELECT/INSERT(与 V297 REVOKE UPDATE, DELETE 口径一致)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON inventory_item_cost_state TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON merchant_expense_entries TO complex_app';
    END IF;
END $$;
