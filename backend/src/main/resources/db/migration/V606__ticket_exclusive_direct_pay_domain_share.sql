-- 文件版本：V0.01
-- 第十八批 X 段第 176 条 176-R1：有价的餐饮、零售专属直付核销行记本次域份额，并按域份额分摊本单固化的通路费。
-- 依据：BSM:749「跨域组合通票扣完上述通路费和适用的销售渠道费后，再按各业态价格占比分到各域——各业态价格在售票时刻随订单一并固化、事后不得变更。
-- 零元免费权益不参与域间分配」「两者一律在拆分给各域之前从整单扣除且只扣一次，由各域按分配份额自然共担」；ENUM:51 同义。
ALTER TABLE settlement_transactions ADD COLUMN domain_share_cent BIGINT;
COMMENT ON COLUMN settlement_transactions.domain_share_cent IS '有价的专属直付（按内部结算价）核销行按售票固化的业态价格占比拆得的本次域份额增量；冲正行取原行之反；NULL＝非专属直付行或零元权益，零元权益不参与域间分配';

-- 第十八批 X 段第 176 条 176-R2：业态份额行记本业态有价专属直付按内部结算价应直付额，净份额与它之差即差额。
-- 依据：BSM:751「哪个域主管公司将权益对客价格或域内分配价格定得低于内部结算价，哪个主体承担差额；剩余也归该责任主体。」；
-- ENUM:53「哪个主体按内部结算价向实际提供方足额结算并自行承担差额；定得高于内部结算价时，高出内部结算价的部分也归该责任主体。」
ALTER TABLE settlement_bill_format_shares ADD COLUMN internal_settled_cent BIGINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN settlement_bill_format_shares.internal_settled_cent IS '本业态有价专属直付按内部结算价应直付给实际提供商户的合计；净份额 net_revenue_cent 与它之差即 ENUM:53 的差额，为负由归属主体承担、为正归归属主体';
