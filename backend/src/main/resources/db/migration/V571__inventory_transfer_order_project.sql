-- 文件版本：V0.01
-- 第十八批 G 段 82-R18a（第十八批裁决 18 所选「对，按业务发生地（推荐）」：「要货单、借用单建单时记所属项目（在 m 的经营项目里选，
-- 只有一个时自动带），之后办理、赔偿应付、逾期工单都按单上的项目。」）：要货单、借用单记所属项目（接收方商户的经营项目之一，
-- 即主项目或 merchant_projects 已批准的经营项目），由建单写入。
ALTER TABLE inventory_transfer_orders ADD COLUMN project_id VARCHAR(64) REFERENCES projects(id);
COMMENT ON COLUMN inventory_transfer_orders.project_id IS '所属项目（建单时在接收方商户的经营项目里选，只有一个时自动带）；办理、赔偿应付、逾期工单按此项目';
