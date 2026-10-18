-- 文件版本：V0.01
-- 由根代理编号；不修改已应用 V469。全国配置及日历审批事实、出账事实与正式往来分别留存。
CREATE TABLE ticket_points_billing_rules (
 id varchar(64) PRIMARY KEY, version varchar(32) NOT NULL UNIQUE,
 effective_month date NOT NULL UNIQUE CHECK (extract(day from effective_month)=1),
 statement_day integer NOT NULL CHECK(statement_day BETWEEN 1 AND 31),
 objection_working_days integer NOT NULL, minimum_days integer NOT NULL, maximum_days integer NOT NULL,
 pay_days integer[] NOT NULL, status varchar(32) NOT NULL CHECK(status IN ('pending_review','approved','rejected')),
 review_id varchar(128) UNIQUE, created_by varchar(64) NOT NULL, created_at timestamptz NOT NULL, approved_at timestamptz,
 CHECK(minimum_days>=0 AND objection_working_days BETWEEN minimum_days AND maximum_days),
 CHECK(array_length(pay_days,1)=3 AND pay_days[1]>=1 AND pay_days[1]<pay_days[2] AND pay_days[2]<pay_days[3] AND pay_days[3]<=28)
);
CREATE TABLE ticket_points_calendar_cases (
 id varchar(64) PRIMARY KEY, days jsonb NOT NULL, source_note text NOT NULL,
 status varchar(32) NOT NULL CHECK(status IN ('pending_review','approved','rejected')), review_id varchar(128) UNIQUE,
 created_by varchar(64) NOT NULL, created_at timestamptz NOT NULL, approved_at timestamptz
);
CREATE TABLE ticket_points_calendar_days (
 calendar_date date PRIMARY KEY, working_day boolean NOT NULL, bank_working_day boolean NOT NULL,
 case_id varchar(64) NOT NULL REFERENCES ticket_points_calendar_cases(id)
);
CREATE TABLE ticket_points_portal_outages (
 id varchar(64) PRIMARY KEY, project_id varchar(64) NOT NULL REFERENCES projects(id),
 unavailable_from timestamptz NOT NULL, unavailable_to timestamptz NOT NULL, reason text NOT NULL,
 status varchar(32) NOT NULL CHECK(status IN ('pending_review','approved','rejected')), review_id varchar(128) UNIQUE,
 created_by varchar(64) NOT NULL, created_at timestamptz NOT NULL, approved_at timestamptz,
 CHECK(unavailable_to>unavailable_from)
);
CREATE TABLE ticket_points_monthly_bills (
 id varchar(64) PRIMARY KEY, billing_month date NOT NULL, billing_rule_id varchar(64) NOT NULL REFERENCES ticket_points_billing_rules(id),
 seller_entity_id varchar(64) NOT NULL REFERENCES business_entities(id), seller_merchant_id varchar(64) REFERENCES merchants(id),
 receiver_entity_id varchar(64) NOT NULL REFERENCES business_entities(id), project_id varchar(64) NOT NULL REFERENCES projects(id),
 amount_cent bigint NOT NULL CHECK(amount_cent>=0), created_at timestamptz NOT NULL, visible_at timestamptz,
 objection_deadline date, due_date date, ar_ap_item_id varchar(64) UNIQUE REFERENCES ar_ap_items(id),
 CHECK((due_date IS NULL)=(objection_deadline IS NULL))
);
CREATE TABLE ticket_points_closed_months (
 billing_month date PRIMARY KEY CHECK(extract(day from billing_month)=1), closed_at timestamptz NOT NULL
);
CREATE TRIGGER ticket_points_closed_month_immutable BEFORE UPDATE OR DELETE ON ticket_points_closed_months FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE OR REPLACE FUNCTION ticket_points_fee_month_guard() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE fee_month date;
BEGIN
 fee_month:=date_trunc('month',NEW.confirmed_at AT TIME ZONE 'Asia/Shanghai')::date;
 PERFORM pg_advisory_xact_lock(hashtextextended('points-month:'||fee_month::text,0));
 IF EXISTS(SELECT 1 FROM ticket_points_closed_months WHERE billing_month=fee_month) THEN
  RAISE EXCEPTION 'confirmed issuance fee month is already closed; confirmation must use its actual new time';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER ticket_points_fee_month_closed BEFORE INSERT ON ticket_points_issuance_fees FOR EACH ROW EXECUTE FUNCTION ticket_points_fee_month_guard();
CREATE UNIQUE INDEX ticket_points_bill_scope ON ticket_points_monthly_bills(billing_month,seller_entity_id,COALESCE(seller_merchant_id,''),receiver_entity_id,project_id);
CREATE TABLE ticket_points_monthly_bill_items (
 reward_id varchar(64) PRIMARY KEY REFERENCES ticket_points_issuance_fees(reward_id),
 bill_id varchar(64) NOT NULL REFERENCES ticket_points_monthly_bills(id)
);
CREATE TRIGGER ticket_points_bill_item_immutable BEFORE UPDATE OR DELETE ON ticket_points_monthly_bill_items FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TABLE ticket_points_bill_objections (
 id varchar(64) PRIMARY KEY, bill_id varchar(64) NOT NULL REFERENCES ticket_points_monthly_bills(id),
 reason text NOT NULL, submitted_by varchar(64) NOT NULL, submitted_at timestamptz NOT NULL,
 resolution text, resolved_by varchar(64), resolved_at timestamptz,
 status varchar(32) NOT NULL CHECK(status IN ('open','resolved')), review_id varchar(128) UNIQUE
);
CREATE OR REPLACE FUNCTION ticket_points_billing_rule_protect() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-'status'-'review_id'-'approved_at') IS DISTINCT FROM (to_jsonb(NEW)-'status'-'review_id'-'approved_at') OR OLD.status <> 'pending_review' THEN
  RAISE EXCEPTION 'billing rule facts are immutable';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER ticket_points_billing_rule_immutable BEFORE UPDATE OR DELETE ON ticket_points_billing_rules FOR EACH ROW EXECUTE FUNCTION ticket_points_billing_rule_protect();
CREATE TRIGGER ticket_points_calendar_case_immutable BEFORE UPDATE OR DELETE ON ticket_points_calendar_cases FOR EACH ROW EXECUTE FUNCTION ticket_points_billing_rule_protect();
CREATE TRIGGER ticket_points_outage_immutable BEFORE UPDATE OR DELETE ON ticket_points_portal_outages FOR EACH ROW EXECUTE FUNCTION ticket_points_billing_rule_protect();
CREATE OR REPLACE FUNCTION ticket_points_bill_protect() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR (to_jsonb(OLD)-'visible_at'-'objection_deadline'-'due_date'-'ar_ap_item_id') IS DISTINCT FROM (to_jsonb(NEW)-'visible_at'-'objection_deadline'-'due_date'-'ar_ap_item_id') THEN
  RAISE EXCEPTION 'published points bill facts are immutable';
 END IF;
 IF OLD.visible_at IS NOT NULL AND NEW.visible_at IS DISTINCT FROM OLD.visible_at THEN RAISE EXCEPTION 'bill publication time is immutable'; END IF;
 IF OLD.ar_ap_item_id IS NOT NULL AND NEW IS DISTINCT FROM OLD THEN
  RAISE EXCEPTION 'points bill accounting deadline is already locked';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER ticket_points_bill_immutable BEFORE UPDATE OR DELETE ON ticket_points_monthly_bills FOR EACH ROW EXECUTE FUNCTION ticket_points_bill_protect();
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status) VALUES
 ('ticket.points.billing.view','查看门票积分结算账单','finance','medium',false,'active'),
 ('ticket.points.billing.manage','办理门票积分全国结算','finance','high',true,'active') ON CONFLICT DO NOTHING;

