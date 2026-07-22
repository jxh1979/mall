-- V338 补授权(2026-07-21):V327 `channel_fee_configs` 与 V334 `integration_channel_store_map` 建表时
--   owner=complex_owner(非 complex),而 V140 的 `ALTER DEFAULT PRIVILEGES` 自动授权只对 complex 建的表生效,
--   故这两表【漏授权给受限运行时 complex_app】,也漏了「新表一律 GRANT complex_app 兜底」铁律(V296/V298 教训)。
-- 后果(2026-07-21 过账 go-live 现场暴露):受限运行时 `settleMonth` 读 `channel_fee_configs`(findChannelFeeBps)报
--   `permission denied for table channel_fee_configs` → 月度分账作业自 batch1(V327)上线起就静默权限报错、
--   从未在生产落过一张账单(过账管道等于一直空转)。补授权后作业首跑即成功落账并过账(实证)。
-- 幂等:GRANT 可重复;运行时角色缺失(部分测试环境)时跳过(与 V140 同守卫)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON channel_fee_configs TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON integration_channel_store_map TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;
