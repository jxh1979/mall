-- AUD-001（P1）：酒吧储值结账渠道白名单补 stored_value。
-- 背景：服务层词表 PaymentChannelPolicy.SETTLEMENT_CHANNELS 与 BarBillingService 均已支持 stored_value
--（AUD-061 储值支付扣卡），但 V062 的 chk_bbs_channel CHECK 未含该值——任何储值结账 INSERT 必触发
-- CHECK 失败并整单回滚，方案5核心路径不可用（此前仅内存仓测试绕过了真实约束）。
ALTER TABLE bar_bill_settlements DROP CONSTRAINT chk_bbs_channel;
ALTER TABLE bar_bill_settlements ADD CONSTRAINT chk_bbs_channel
    CHECK (settle_channel IN ('aggregate_pay', 'cash', 'pos', 'bank_transfer', 'other', 'stored_value'));
