-- 文件版本：V0.01
-- V625（第十八批 AK 段第 221 条 221-R1）：酒吧商品沽清分两档，沽清与解除各经主管审批。
-- 依据：BSM:1069「反结（重开已结账单）、沽清（卖完、临时下架）均需主管审批留痕」；
--   ENUM:111「沽清分两档：临时沽清（本营业日有效，营业日切换时点自动恢复在售）与长期下架（须主管审批解除后才恢复）；
--   沽清与解除同样须审批留痕，作用范围为本酒吧。」
-- ① 沽清档位 sold_out_type：temporary（临时沽清）、long_term（长期下架），未沽清为空；
--    临时沽清到期时间 sold_out_until＝该酒吧本营业日结束时刻（营业日切换自动恢复作业据此恢复在售），长期下架与未沽清为空。
-- ② 三条准入策略恒阻断，档位同同句高危动作反结（V612 bar_bill_reopen_block）。临时沽清、长期下架、解除分成三个动作：
--    复核单只带动作类型与对象，主管从动作名看出批的是哪一种，批准也只放行所批的那一种。
ALTER TABLE bar_products ADD COLUMN sold_out_type VARCHAR(16);
ALTER TABLE bar_products ADD COLUMN sold_out_until TIMESTAMPTZ;
ALTER TABLE bar_products ADD CONSTRAINT chk_bar_products_sold_out_type
    CHECK (sold_out_type IS NULL OR (sold_out AND sold_out_type IN ('temporary', 'long_term')));

INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, enabled)
VALUES ('bar_product_sold_out_block', 'bar.product_sold_out', 'bar_product', 'always', 'block', 'one_level_up', 2, true),
       ('bar_product_delist_block', 'bar.product_delist', 'bar_product', 'always', 'block', 'one_level_up', 2, true),
       ('bar_product_sold_out_release_block', 'bar.product_sold_out_release', 'bar_product', 'always', 'block', 'one_level_up', 2, true)
ON CONFLICT (policy_code) DO NOTHING;
