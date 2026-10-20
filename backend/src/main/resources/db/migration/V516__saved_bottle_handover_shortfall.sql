-- 文件版本：V0.01
-- V516：FS-017 存酒交平台接收时部分短少按差额（第九批复核遗留，2026-10-20 操作员裁决「按差额」：补存/损失/员工赔偿数量＝登记剩余量−实收量；
--   实收的部分继续以原瓶由平台保管、客人照常取）。依据：BSM L1072 ④「接收时已存在的毁损、短少……仍由原门店承担」。
-- 逐瓶交接明细另记短少量（登记剩余量−实收量），非 short 为 0。
ALTER TABLE saved_bottle_handover_items
    ADD COLUMN shortfall_quantity INTEGER NOT NULL DEFAULT 0
    CONSTRAINT chk_saved_bottle_handover_items_shortfall CHECK (shortfall_quantity >= 0);
