-- AUD-066 父票实例与人员权益组基础模型（票务权益地基）。
-- 订单里每买 1 张票 = 1 行父票实例（四人票买 2 张 = 2 行）；多人票内每个人一个 slot。
-- 方案2（整票退款）/方案3（转赠）的共同地基：退款锚 first_used_at/status，转赠锚 current_holder_customer_id。
-- 用户口径：测试环境上线前全删，不做任何存量回填；本迁移只建表加列，无回填 SQL。
CREATE TABLE ticket_instances (
    id                          VARCHAR(64) PRIMARY KEY,
    ticket_no                   VARCHAR(64) NOT NULL UNIQUE,
    order_id                    VARCHAR(64) NOT NULL,
    ticket_type_id              VARCHAR(64) NOT NULL,
    seq_in_order                INTEGER     NOT NULL,
    num_people                  INTEGER     NOT NULL DEFAULT 1,
    total_times                 INTEGER     NOT NULL DEFAULT 1,
    buyer_customer_id           VARCHAR(64),
    current_holder_customer_id  VARCHAR(64),
    status                      VARCHAR(32) NOT NULL DEFAULT 'active',
    first_used_at               TIMESTAMPTZ,
    first_session_key           VARCHAR(128),
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_instances_status CHECK (status IN ('active','refunded','forfeited')),
    CONSTRAINT uq_ticket_instances_order_seq UNIQUE (order_id, seq_in_order)
);
CREATE INDEX idx_ticket_instances_order  ON ticket_instances (order_id);
CREATE INDEX idx_ticket_instances_holder ON ticket_instances (current_holder_customer_id);

-- 权益行归父归组：parent_ticket_id 可空（新发放必填，存量无=null，无历史回填），slot_index 1..N（用户口径无 slot 0）。
ALTER TABLE issued_entitlements
    ADD COLUMN parent_ticket_id VARCHAR(64),
    ADD COLUMN slot_index INTEGER NOT NULL DEFAULT 0 CHECK (slot_index >= 0);
CREATE INDEX idx_issued_entitlements_parent ON issued_entitlements (parent_ticket_id);
