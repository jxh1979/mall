-- AUD-058 储值充值支付意图 ↔ 卡范围绑定。
-- 根因：settleTopup 用请求体的 project/merchant 解析入账账户，与充值时的卡范围无绑定——
-- 通用卡充值可在入账时被指名入商户卡/跨商户/跨项目（结算口径错位、项目隔离失守）。
-- 收口：topup 创建支付意图时把 (customer, project, merchant) 绑定落本表，settleTopup 只认绑定范围入账。
CREATE TABLE stored_value_topup_intents (
    payment_intent_id VARCHAR(64) PRIMARY KEY,
    customer_id       VARCHAR(64) NOT NULL,
    project_id        VARCHAR(64),
    merchant_id       VARCHAR(64),
    created_at        TIMESTAMPTZ NOT NULL
);
