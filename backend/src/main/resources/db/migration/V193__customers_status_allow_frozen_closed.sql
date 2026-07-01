-- 3.5 修订（用户 2026-07-01 改选 active+frozen+closed）：customers.status 合法值放开到
-- active/frozen/closed，为封禁(frozen)+注销(closed)预留（暂无写入流程，日后建流程时启用）。
-- 替换 V192 仅 active 的 CHECK；权衡由用户拍板（最省未来迁移，代价=两个暂无流程支撑的状态值）。
ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_status;
ALTER TABLE customers ADD CONSTRAINT chk_customers_status CHECK (status IN ('active', 'frozen', 'closed'));
