-- 文件版本：V0.01
-- V549（第十六批 G4：待开发清单第 34 条）：供应商适配器接入状态改为 ENUM 七态。
-- 依据 ENUM L211「接入状态 待接入、联调中、已启用、暂停、异常、已停用、归档」；码沿用 third-party-integration-development-design
-- 接入关系业务状态的 pending（待接入）、testing（联调中）、active（已启用）、paused（暂停）、abnormal（异常）、disabled（已停用）、archived（归档）。
-- 存量按语义一一映射（第三方接入 PRD §5：待接入＝已规划但未启用、联调中＝正在确认回流口径、已启用＝可正常回流业务结果）：
--   draft、mock_ready（模拟就绪，未接真实供应商）→ pending；test_ready → testing；production_ready → active；disabled 不变。
-- 生产就绪标记（production_ready 列）只认 active。
ALTER TABLE integration_provider_adapters DROP CONSTRAINT chk_integration_provider_adapters_status;
ALTER TABLE integration_provider_adapters DROP CONSTRAINT chk_integration_provider_adapters_ready;
UPDATE integration_provider_adapters
   SET status = CASE status
                    WHEN 'draft' THEN 'pending'
                    WHEN 'mock_ready' THEN 'pending'
                    WHEN 'test_ready' THEN 'testing'
                    WHEN 'production_ready' THEN 'active'
                    ELSE status
                END;
ALTER TABLE integration_provider_adapters ADD CONSTRAINT chk_integration_provider_adapters_status
    CHECK (status IN ('pending', 'testing', 'active', 'paused', 'abnormal', 'disabled', 'archived'));
ALTER TABLE integration_provider_adapters ADD CONSTRAINT chk_integration_provider_adapters_ready CHECK (
    production_ready = FALSE
    OR (
        status = 'active'
        AND environment = 'production'
        AND webhook_enabled = TRUE
        AND polling_check_enabled = TRUE
        AND credential_secret_ref IS NOT NULL
        AND jsonb_array_length(capabilities) > 0
    )
);
