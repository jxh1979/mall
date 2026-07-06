-- P2 审计项 B-3：给订单/权益 status 列加 CHECK 约束，防脏数据落库（应用层脏值写入被库拒绝）。
-- 枚举来源为生产写路径穷尽追溯（非猜测），每列合法集合见下方注释。
-- 参照 V224 settlement_bills 的 chk_sb_status 风格：CONSTRAINT chk_<table>_status CHECK (status IN (...))。

-- ticket_orders.status 完整生产枚举 {created, cancelled}：
--   created   —— 下单创建：TicketOrderService.createOrder（TicketOrderRecord.java 构造，JdbcTicketingRepository INSERT :status）。
--   cancelled —— 取消/超时未支付：JdbcTicketingRepository.atomicCancelUnpaidOrder（UPDATE ... SET status='cancelled'）。
--   注意：pay_status / issue_status 是独立列（paid/unpaid、issued/not_issued/issuing），不并入本约束。
ALTER TABLE ticket_orders
    ADD CONSTRAINT chk_ticket_orders_status CHECK (status IN ('created', 'cancelled'));

-- issued_entitlements.status 完整生产枚举 {active, refunded, forfeited}：
--   active    —— 出票发放 / 冲正恢复：TicketOrderService.issueEntitlements、JdbcTicketingRepository.atomicReverseEntitlement（CASE WHEN ... 'active'）。
--   refunded  —— 退款审批冻结：JdbcTicketingRepository.freezeEntitlementForRefund（UPDATE ... SET status='refunded'）。
--   forfeited —— 到期未用作废：JdbcTicketingRepository.forfeitEntitlement（UPDATE ... SET status='forfeited'）。
ALTER TABLE issued_entitlements
    ADD CONSTRAINT chk_issued_entitlements_status CHECK (status IN ('active', 'refunded', 'forfeited'));
