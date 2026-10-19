-- 文件版本：V0.01
-- HP-C04 退租结算争议裁决留痕与「有争议，诉讼/仲裁」状态。依据：BSM L1352「有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，
-- 则进「有争议，诉讼/仲裁」状态无限期挂起，直至诉讼或仲裁有结果后再行处理」；ENUM L252 退租结算状态「待结算、押金抵扣中、欠费追缴中、已结清、有争议、
-- 「有争议，诉讼/仲裁」」；2026-10-18 操作员裁决第 5、6 条（主管裁决写结论留痕；商户不接受裁决进诉讼/仲裁挂起，有结果后由主管录结果继续）。
-- dispute_resolution 追加记主管裁决结论与诉讼/仲裁结果（resolved_by/resolved_at 记最近一次录入人/时）。
ALTER TABLE contract_settlements ADD COLUMN IF NOT EXISTS dispute_resolution TEXT;
ALTER TABLE contract_settlements DROP CONSTRAINT IF EXISTS chk_contract_settlements_status;
ALTER TABLE contract_settlements ADD CONSTRAINT chk_contract_settlements_status
    CHECK (status IN ('pending_settlement', 'deposit_offsetting', 'arrears_collecting', 'settled', 'disputed',
                      'disputed_litigation', 'voided'));
