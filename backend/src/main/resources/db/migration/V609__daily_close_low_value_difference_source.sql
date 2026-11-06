-- 文件版本：V0.01
-- 第十八批 Y 段第 182 条 182-R1：日结多源汇总来源闭集加低价值未关闭差异 low_value_difference（含上期带入）。
-- 依据：ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定；低价值差异单独列示带账进入下期。对账差异的高低价值判定：
--   以该笔差异所涉金额与「高价值判定」条所配的同一金额阈值比较，高于阈值即高价值，其余为低价值；阈值未配置一律按高价值处理。」——
--   营业日不晚于本日、本范围仍未关闭的低价值对账差异计笔数与金额合计，单列一行，关闭即不再列示；退款记 0。
ALTER TABLE finance_daily_close_summaries DROP CONSTRAINT chk_fdcs_source;
ALTER TABLE finance_daily_close_summaries ADD CONSTRAINT chk_fdcs_source CHECK (source_stream IN
  ('official_ticket','channel_ticket','bar','amusement','supply_cost','thirdparty_reflow','bar_deposit','low_value_difference'));
