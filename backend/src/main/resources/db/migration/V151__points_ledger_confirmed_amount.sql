-- V151 积分按核销张数精确计分（F-TXN-03 收尾）：customer_points_ledger 增 confirmed_amount，
-- 记录该 earn 行「已确认份额」，支撑多次/多人票部分核销时按张数比例确认、冲正按份回滚。
-- 背景：原入账引擎 confirmPoints 把订单 pending earn「首次核销即整单 100% confirmed」；多次票(total_times>1)
--   部分核销应只按 Σused/Σtotal 比例确认，冲正一张应只回滚 1/Σtotal。改为「set-confirmed-to-target」：
--   target = floor(points_delta × 已核销张数 / 订单总张数)，每次核销/冲正把 confirmed_amount 对齐 target、
--   按差额双向增减余额（自然幂等）。本列即承载该 target。
-- 口径：仅对 change_type='earn' 行有意义（已确认份额 ⊆ points_delta）；非 earn 行恒 0。
-- 兼容：列默认 0；历史「已 100% confirmed 的 earn 行」回填 confirmed_amount = points_delta（保持既有余额口径）。

ALTER TABLE customer_points_ledger
    ADD COLUMN IF NOT EXISTS confirmed_amount BIGINT NOT NULL DEFAULT 0;

-- 历史回填：旧逻辑下 status='confirmed' 的正向 earn 行即「整单已确认」，份额=全额 points_delta。
UPDATE customer_points_ledger
   SET confirmed_amount = points_delta
 WHERE change_type = 'earn'
   AND status = 'confirmed'
   AND points_delta > 0
   AND confirmed_amount = 0;
