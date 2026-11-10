-- 文件版本：V0.01
-- 第十八批 Z 段第 186 条 186-R1：锁定后补录加一种「收付款补录」（kind='payment'），上级审批通过后登记收付款，不新建对象。
-- 依据：ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额：补录金额归入当期，并在补录记录上标明其所属的原营业日；……」；
--   BSM:2023「已拍板：日结锁定后不能直接补录影响金额，必须上级审批通过后补录。」；第七批裁决 19「人工登记收款仍受锁定拦截。」
-- 既有补录一律是金额调整（amount）；收付款补录的金额为正、收付方式取线下四种（与线下确认收款同一闭集）。
-- 186-R2（控制席并入，同根；BSM:1323「提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补」）：再加一种「退款补录」（kind='refund'），
--   上级审批通过后按原收款走现有原路退款路径；金额为正，收付方式照原收款记录。
ALTER TABLE ar_ap_adjustments ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'amount';
ALTER TABLE ar_ap_adjustments ADD COLUMN payment_method VARCHAR(32);
ALTER TABLE ar_ap_adjustments ADD CONSTRAINT chk_ar_ap_adj_kind CHECK (kind IN ('amount','payment','refund'));
ALTER TABLE ar_ap_adjustments ADD CONSTRAINT chk_ar_ap_adj_payment CHECK (
  (kind = 'amount' AND payment_method IS NULL)
  OR (kind = 'payment' AND adjustment_amount_cent > 0 AND payment_method IN ('cash','bank_transfer','pos','other'))
  OR (kind = 'refund' AND adjustment_amount_cent > 0 AND payment_method IS NOT NULL));
