-- 文件版本：V0.01
-- HP-C02 到期终止结算接入统一审批。依据：BSM L1352「退租、提前终止、合同到期等各种终止结算，都要经过多级审批（涉及财务、运营等多个部门共同审核）……
-- 审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」；ENUM L255「合同审批链按新建、到期终止、变更三类分别配置流程模板……到期不得无审批自动终止」。
-- 到期结算单挂「到期终止」类模板复核单（V494 contract_expiry_termination 策略，object_type＝contract_settlement），与 contracts/contract_changes.review_id 同形。
ALTER TABLE contract_settlements ADD COLUMN IF NOT EXISTS review_id VARCHAR(80) REFERENCES supervisor_review_decisions(review_id);
