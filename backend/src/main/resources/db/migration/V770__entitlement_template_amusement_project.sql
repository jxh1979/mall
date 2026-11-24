-- 文件版本：V0.01
-- V770：第十八批 CC 段第 399 条 399-R1（游乐权益模板指定游乐项目，提供方按项目经营商户绑定）。
-- 依据：BSM:784「入场、游乐（指定项目/次数/场次）、酒水（赠饮或兑换券）、卡座免通票入场名额、餐饮券、零售券、服务权益、积分兑换虚拟券。」；
--   BSM:1033「通票中的酒水、小食和其他权益必须分别绑定实际提供方」；BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」
-- 研发阶段无存量（UD-07），不回填。
ALTER TABLE entitlement_templates ADD COLUMN IF NOT EXISTS amusement_project_id VARCHAR(64) REFERENCES amusement_projects(id);

COMMENT ON COLUMN entitlement_templates.amusement_project_id IS
    '游乐权益指定的游乐项目（BSM:784「游乐（指定项目/次数/场次）」）：游乐类权益必填，核销只认该项目；该项目已登记经营商户的，模板提供方商户须为该商户';
