-- V442 招商线索「已签约」改到合同生效时（CG-01）。
--
-- 【业务口径】(platform-governance-map「招商运营细则」)
--   「招商线索在已签约前，可从待跟进、跟进中、已转意向或洽谈中转为已流失或已搁置；转换时必须填写原因，
--    合同审批全部通过、生效后转为已签约，已签约后不得回退至未签约状态」
--
-- 【为什么要加列】签约起草的合同 id 由幂等键 lease-sign-<意向id> 经 SHA-256 派生（ContractService.createDraft），
--   只能由意向算出合同，不能由合同反查意向。合同生效（ContractService.activate）要反查来源线索再置已签约，
--   故在意向上记下签约起草的合同 id（LeasingService.signIntent 写入）。不新增线索/合同状态，不回填存量（铁律 8）。
ALTER TABLE leasing_intents ADD COLUMN IF NOT EXISTS contract_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_leasing_intents_contract
    ON leasing_intents (contract_id)
 WHERE contract_id IS NOT NULL;

COMMENT ON COLUMN leasing_intents.contract_id IS
    '签约起草的合同 id（幂等键 lease-sign-<意向id> 派生）；合同生效后据此反查来源线索并置已签约。';
