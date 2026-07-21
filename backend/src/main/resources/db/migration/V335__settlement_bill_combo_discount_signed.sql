-- V335 套票折扣跨月冲正:放开 settlement_bills.combo_discount_cent 的 >=0 约束(改为带符号)。
--
-- 背景(套票真上线前必修):项目承担折扣(discount_bearer=platform_absorb/split_by_ratio)在核销时为正(平台吸收、
-- 商户按满额分),但当这笔核销在<b>后续已过账的月份</b>被冲正时,冲正红冲行落到当月、该月折扣净额为负(平台<b>追回</b>
-- 先前吸收的折扣)。原 V331 `chk_sb_combo_discount CHECK (combo_discount_cent >= 0)` 会拒绝这个合法的负向追回,
-- 而收入侧 verified_amount_cent 是带符号跨月净掉的 → 出现「收入 -R 追回了、折扣 -D 没追回」的不对称,项目多付该笔折扣。
--
-- combo_discount_cent 是月度<b>净额聚合</b>(Σ 项目承担折扣,正=本月吸收/负=本月追回),无独立非负不变式,
-- 放开为带符号即与收入侧一致。存量行默认 0、放开约束不违反任何存量,可独立回滚(回滚即恢复 >=0,仅在有负值行时受阻)。
ALTER TABLE settlement_bills DROP CONSTRAINT IF EXISTS chk_sb_combo_discount;
