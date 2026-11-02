-- 文件版本：V0.01
-- V588（第十八批 M 段第 119 条 119-R1）：库位主数据。BSM:1936「供应链连基础管理｜因为供应商、供应合同、库房、库位、采购人员、库管和审批权限
-- 都要先在基础管理中定义清楚。」；BSM:1162「管理库房和库位……」；PGM:480「供应链库存要知道库房和库位。」
-- 库位须先在库存台账「库房」区块按库房建档；采购验收、门店要货签收、借调买断收货只收所选仓库下已建档的库位。
-- id 即库位编号（余额、批次、流水里的 location_id，长度同 inventory_stock_balances.location_id），全平台唯一；项目与货权经库房带出。
-- 谁建的由审计事件 inventory.warehouse_location.created 记录（warehouses 表同样不设 created_by）。不预置行、不回填既有余额的库位（UD-07）。

CREATE TABLE IF NOT EXISTS warehouse_locations (
    id VARCHAR(64) PRIMARY KEY,
    warehouse_id VARCHAR(64) NOT NULL REFERENCES warehouses(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE warehouse_locations IS '库位主数据：先在基础管理中按库房建档，入库只收所选仓库下已建档的库位（BSM:1936；第十八批 M 段第 119 条 119-R1）';
