-- V277: 库存跨主体流转 P1 基础 schema(2026-07-08 用户拍板"供货公司=新增组织层")。
-- 缘由:平台级供货公司(酒水/中央餐饮)向商户送货+结算;商户A的货可借调给B+A-B结算;货带溯源码全程跟随。
-- 详见 docs/proposals/库存跨主体流转-平台供货借调-结算与溯源-设计草案.md。

-- 1) 供货主体(平台级组织层·非商户):拥有自己的供货库存,向商户供货并结算。
CREATE TABLE supply_entities (
    id                 varchar(64)  PRIMARY KEY,
    name               varchar(200) NOT NULL,
    entity_type        varchar(40)  NOT NULL,                 -- liquor / central_kitchen / ...
    level              varchar(20)  NOT NULL DEFAULT 'platform',
    status             varchar(20)  NOT NULL DEFAULT 'active',
    settlement_account varchar(200),
    created_by         varchar(64),
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT supply_entities_status_check CHECK (status IN ('active', 'suspended', 'archived')),
    CONSTRAINT supply_entities_level_check CHECK (level IN ('platform'))
);

-- 2) 溯源码归属泛化:merchant_id → (owner_type, owner_id)。保留 merchant_id 向后兼容,新对为权威。
ALTER TABLE inventory_trace_codes ADD COLUMN owner_type varchar(20) NOT NULL DEFAULT 'merchant';
ALTER TABLE inventory_trace_codes ADD COLUMN owner_id   varchar(64);
UPDATE inventory_trace_codes SET owner_id = merchant_id WHERE owner_id IS NULL;

-- 3) 跨主体流转单:调拨(供货→商户·商户付货款)/借调(商户A→B·借=归还/调=买断)。
CREATE TABLE inventory_transfer_orders (
    id               varchar(64)  PRIMARY KEY,
    transfer_type    varchar(20)  NOT NULL,                   -- supply / lending
    lending_mode     varchar(20),                             -- borrow / purchase(仅 lending)
    from_owner_type  varchar(20)  NOT NULL,                   -- supply_entity / merchant
    from_owner_id    varchar(64)  NOT NULL,
    to_owner_type    varchar(20)  NOT NULL,                   -- merchant
    to_owner_id      varchar(64)  NOT NULL,
    from_location_id varchar(64),
    to_location_id   varchar(64),
    status           varchar(20)  NOT NULL DEFAULT 'created',
    operator_note    varchar(500),
    created_by       varchar(64),
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT inv_transfer_type_check CHECK (transfer_type IN ('supply', 'lending')),
    CONSTRAINT inv_transfer_status_check CHECK (status IN ('created', 'shipped', 'received', 'settled', 'returned', 'cancelled')),
    CONSTRAINT inv_transfer_lending_mode_check CHECK (lending_mode IS NULL OR lending_mode IN ('borrow', 'purchase'))
);

-- 4) 流转明细:物料+数量+随货溯源码+结算单价。
CREATE TABLE inventory_transfer_lines (
    id                varchar(64)  PRIMARY KEY,
    transfer_order_id varchar(64)  NOT NULL REFERENCES inventory_transfer_orders(id),
    item_id           varchar(64)  NOT NULL,
    quantity          integer      NOT NULL CHECK (quantity > 0),
    trace_codes       jsonb        NOT NULL DEFAULT '[]',
    unit_price_cent   bigint       NOT NULL DEFAULT 0,
    created_at        timestamptz  NOT NULL DEFAULT now()
);

-- 5) 溯源码归属变更历史(append-only·防调包+结算对账)。
CREATE TABLE inventory_trace_ownership_history (
    id                varchar(64)  PRIMARY KEY,
    trace_code        varchar(64)  NOT NULL,
    from_owner_type   varchar(20),
    from_owner_id     varchar(64),
    to_owner_type     varchar(20)  NOT NULL,
    to_owner_id       varchar(64)  NOT NULL,
    transfer_order_id varchar(64),
    changed_at        timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_transfer_lines_order ON inventory_transfer_lines(transfer_order_id);
CREATE INDEX idx_transfer_orders_to ON inventory_transfer_orders(to_owner_type, to_owner_id);
CREATE INDEX idx_trace_ownership_hist_code ON inventory_trace_ownership_history(trace_code);
CREATE INDEX idx_trace_codes_owner ON inventory_trace_codes(owner_type, owner_id);

-- 6) 受限运行时角色 complex_app 授权(事务表需 UPDATE·台账 append-only 只 INSERT/SELECT)。
GRANT SELECT, INSERT, UPDATE ON supply_entities TO complex_app;
GRANT SELECT, INSERT, UPDATE ON inventory_transfer_orders TO complex_app;
GRANT SELECT, INSERT, UPDATE ON inventory_transfer_lines TO complex_app;
GRANT SELECT, INSERT ON inventory_trace_ownership_history TO complex_app;
GRANT UPDATE ON inventory_trace_codes TO complex_app;
