-- 文件版本：V0.01
-- Flyway owner 与应用运行角色分离时，显式保证复核转正可取得序号。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT USAGE, SELECT ON SEQUENCE turnover_rent_rule_activation_seq TO complex_app;
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END;
$$;
