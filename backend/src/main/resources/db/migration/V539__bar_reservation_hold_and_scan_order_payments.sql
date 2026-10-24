-- 文件版本：V0.01
-- 第十三批 B 段 ROOT-03（B-bar）：订台排他预留与最晚到店时间、客人扫码点单待付单。
--
-- ① ROOT-03-R19 订台排他预留与最晚到店时间。依据：ENUM 第 100 行「客人自助订台支付定金成功、或员工代订提交成功时，该台位即进入已预留。
--    一个台位同一时刻只能存在一笔有效预留（排他锁，不是展示标记）；台位未被占用——既无有效预留也未开台——时即可再次接受预订……
--    冲突时先支付定金者得；员工代订不得覆盖已付定金的自助订单；两笔员工代订相撞先提交者得。超时自动释放后台位回到空闲……
--    已预留只能经取消订台、超时释放、到店开台三条路径退出」；BSM 第 1062 行「无论是否支付定金，每一笔订台都必须填写最晚到店时间；
--    超过最晚到店时间未到店的，系统自动释放该台位——已付定金的按爽约规则没收定金，未付定金的直接作废」。
--    hold_status：held＝该笔订台正占着台位；released＝已经取消、超时或到店开台退出；NULL＝尚未占台（客人定金未付）。
--    排他按台位（不按营业日）：部分唯一索引保证一个台位同一时刻只一笔 held。
ALTER TABLE bar_reservations
    ADD COLUMN IF NOT EXISTS latest_arrival_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS hold_status VARCHAR(16);

ALTER TABLE bar_reservations DROP CONSTRAINT IF EXISTS chk_bar_reservations_hold_status;
ALTER TABLE bar_reservations ADD CONSTRAINT chk_bar_reservations_hold_status
    CHECK (hold_status IS NULL OR hold_status IN ('held', 'released'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_reservation_active_hold
    ON bar_reservations (table_id)
    WHERE hold_status = 'held';

COMMENT ON COLUMN bar_reservations.latest_arrival_at IS '最晚到店时间（BSM 第 1062 行，每笔订台必填；超时由系统自动释放）';
COMMENT ON COLUMN bar_reservations.hold_status IS '台位排他预留：held 占台中 / released 已退出（ENUM 第 100 行）';

-- ② ROOT-03-R7 客人扫码点单待付单。依据：BSM 第 1025 行「扫码点单只适用于散台……下单时即时付款、付款成功订单才成立，不挂账；
--    酒水必须先完成付款才能出品上台。付款成功后所点酒水计入该桌位当前账单」；ENUM 第 129 行「非会员可以浏览、点单和付款」。
--    客人下单只落本表与在线支付单，付款确认成功才建酒吧订单（bar_order_id）；客人身份可空（非会员）。
CREATE TABLE IF NOT EXISTS bar_scan_order_payments (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    table_id VARCHAR(64) NOT NULL REFERENCES bar_tables(id),
    session_id VARCHAR(64) NOT NULL REFERENCES bar_table_sessions(id),
    customer_id VARCHAR(64),
    items JSONB NOT NULL,
    amount_cent BIGINT NOT NULL,
    payment_intent_id VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    bar_order_id VARCHAR(64),
    created_terminal VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    CONSTRAINT chk_bar_scan_order_payments_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_bar_scan_order_payments_status CHECK (status IN ('pending', 'paid', 'refunding')),
    CONSTRAINT chk_bar_scan_order_payments_terminal CHECK (created_terminal IN ('app', 'miniapp'))
);

CREATE INDEX IF NOT EXISTS idx_bar_scan_order_payments_session
    ON bar_scan_order_payments (session_id, status);

-- ③ 已收客人在线款但成不了的两种情形，登记原路待退（与停售拒单同一原路退款登记）：订台定金到账时台位已被先付者占
--    （bar_deposit_rejection，ENUM 第 100 行「冲突时先支付定金者得」）；扫码点单付款确认时该桌已结束接待（bar_scan_order_rejection）。
ALTER TABLE finance_refunds DROP CONSTRAINT chk_finance_refund_source;
ALTER TABLE finance_refunds ADD CONSTRAINT chk_finance_refund_source CHECK (
 source_type IN ('ar_ap_payment','stored_value_account','ticket_payment_rejection','channel_ticket_rejection','dealer_order_refund',
                 'bar_deposit_rejection','bar_scan_order_rejection')
);
