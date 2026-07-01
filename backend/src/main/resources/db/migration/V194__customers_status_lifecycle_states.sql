-- 3.5（用户最终拍板）：客人账号四态生命周期——正常 active / 封禁 suspended（临时）/
-- 黑名单 blacklisted（永久）/ 注销 deregistered（客人自助，历史数据保留但客人不可见）。
-- 本轮仅落校验闸；封禁到期自动恢复+期内下单拦截、客人自助注销+注销后拦登录隐藏数据、
-- 黑名单永久拦截 均作后续单独功能。V193(active/frozen/closed)已推送不可原地改，故新迁移
-- drop+recreate；frozen/closed 从无写入流程、无存量行使用，去除安全。
ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_status;
ALTER TABLE customers ADD CONSTRAINT chk_customers_status
    CHECK (status IN ('active', 'suspended', 'blacklisted', 'deregistered'));
