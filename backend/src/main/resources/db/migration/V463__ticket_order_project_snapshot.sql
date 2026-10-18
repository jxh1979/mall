-- 文件版本：V0.01
-- 在售票时保存已核实的综合体归属，后续付款、拒收退款与核销沿用原事实。
ALTER TABLE ticket_orders
    ADD COLUMN project_id VARCHAR(64) REFERENCES projects(id);
