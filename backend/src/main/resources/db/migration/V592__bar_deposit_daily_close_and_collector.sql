-- 文件版本：V0.01
-- 第十八批 O 段第 126 条：订台定金进日结与收银班结。
-- 126-R1 依据：BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」；
-- BSM:1003「连接支付和财务，一体机产生的订单必须进入日结、对账和经营看板。」。
-- 日结多源汇总来源闭集加订台定金 bar_deposit：各渠道当日到账的定金计金额与笔数、当日实际退回成功的定金计退款，按各酒吧营业日窗；
-- 定金不抵扣消费、不计入最低消费累计（BSM:889），不并入酒吧源。
ALTER TABLE finance_daily_close_summaries DROP CONSTRAINT chk_fdcs_source;
ALTER TABLE finance_daily_close_summaries ADD CONSTRAINT chk_fdcs_source CHECK (source_stream IN
  ('official_ticket','channel_ticket','bar','amusement','supply_cost','thirdparty_reflow','bar_deposit'));

-- 126-R2 依据：BSM:1029「现场必须允许现金和 POS 机支付，但要进入账单、日结、员工责任链和财务对账。」；
-- BSM:1081「……班结按本班次汇总各支付方式应收（现金/扫码/POS/退款/小费），录入实点现金算长短款差异……」。
-- 员工登记定金（pay-deposit）记登记员工，宽度同 bar_bill_settlements.cashier_staff_id（V062）。
ALTER TABLE bar_reservations ADD COLUMN deposit_collector_staff_id VARCHAR(64);
COMMENT ON COLUMN bar_reservations.deposit_collector_staff_id IS '员工登记定金（pay-deposit）的登记员工，班结按其归入现金/POS 应收';
