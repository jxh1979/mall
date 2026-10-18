-- 文件版本：V0.01
-- 驳回不阻止补正重申；真实门户故障只延期未付款额，原账单与付款保留。
ALTER TABLE ticket_points_billing_rules DROP CONSTRAINT ticket_points_billing_rules_effective_month_key;
CREATE UNIQUE INDEX ticket_points_billing_rule_live_month ON ticket_points_billing_rules(effective_month) WHERE status IN ('pending_review','approved');

CREATE TABLE ticket_points_bill_schedule_adjustments (
 id varchar(64) PRIMARY KEY,
 bill_id varchar(64) NOT NULL REFERENCES ticket_points_monthly_bills(id),
 outage_id varchar(64) NOT NULL REFERENCES ticket_points_portal_outages(id),
 previous_objection_deadline date NOT NULL, previous_due_date date NOT NULL,
 objection_deadline date NOT NULL, due_date date NOT NULL,
 unpaid_amount_cent bigint NOT NULL CHECK(unpaid_amount_cent>0),
 created_at timestamptz NOT NULL,
 UNIQUE(bill_id,outage_id),
 CHECK(objection_deadline>previous_objection_deadline AND due_date>=previous_due_date AND due_date>=objection_deadline)
);
CREATE TRIGGER ticket_points_bill_schedule_adjustment_immutable BEFORE UPDATE OR DELETE ON ticket_points_bill_schedule_adjustments FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
