-- 文件版本：V0.01
-- V622（第十八批 AI 段第 237 条 237-R1、第 214 条 214-R1、214-R2）：业态份额行拆出娱乐平台公司代收、应归归属主体的域间份额，
--   另记原月已过账后撤销在本月追收的部分与到期未核销归属，月结过账据此挂娱乐平台公司与各域主管公司之间的应收应付。
-- 依据：ENUM:32「票款由娱乐平台公司统一收款」「其余各域份额自收款之日起即为应付给对应域主管公司的款项」「纯游乐票和纯游乐通票由游乐公司负责」；
--   BSM:1720「与娱乐平台公司无关」；BSM:1144「由传媒公司自行定价、售票、收款」；ENUM:49「其一，到期未核销票款」；BSM:804「再冲回域间份额」；
--   第十八批裁决 51「当月核销形成的份额挂在核销所在月；从未核销、到期的部分在到期所在月另挂一笔」；决策台账 FS-021「单独列追收、按原月重算」。

ALTER TABLE settlement_bill_format_shares
    ADD COLUMN inter_entity_net_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN recovery_net_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN recovery_internal_settled_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN unverified_net_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_bill_format_shares.inter_entity_net_cent IS
    '域间份额（分）：本业态净份额中由娱乐平台公司代收、应归归属主体的部分——游乐、传媒行扣除归属主体自收的纯游乐票、纯游乐通票与传媒自办演出票，餐饮零售行即净份额，酒吧与「其他」为 0（ENUM:32「其余各域份额自收款之日起即为应付给对应域主管公司的款项」；BSM:1720）';
COMMENT ON COLUMN settlement_bill_format_shares.recovery_net_cent IS
    '原月已过账后撤销、在本月追收的域间份额（分，负＝向归属主体追回；决策台账 FS-021「单独列追收、按原月重算」；BSM:804「再冲回域间份额」）';
COMMENT ON COLUMN settlement_bill_format_shares.recovery_internal_settled_cent IS
    '原月已过账后撤销、在本月追收的有价专属直付按内部结算价应直付额（分；决策台账 FS-021「单独列追收、按原月重算」）';
COMMENT ON COLUMN settlement_bill_format_shares.unverified_net_cent IS
    '本月到期未核销、归本业态归属主体的组合通票票款（分，未核销额扣实际发生的通路费与销售渠道费；ENUM:49「其一，到期未核销票款」；第十八批裁决 51「从未核销、到期的部分在到期所在月另挂一笔」）';
