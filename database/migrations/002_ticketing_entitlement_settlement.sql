-- 002_ticketing_entitlement_settlement.sql
-- Ticketing, entitlement, verification, payment, refund, settlement and reconciliation tables.
-- Target database: PostgreSQL.

create extension if not exists pgcrypto;

create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create table if not exists ticket_products (
  id uuid primary key default gen_random_uuid(),
  product_code varchar(64) not null unique,
  product_name varchar(160) not null,
  product_type varchar(40) not null,
  owner_merchant_id uuid,
  sales_start_at timestamptz,
  sales_end_at timestamptz,
  valid_type varchar(40) not null,
  valid_start_at timestamptz,
  valid_end_at timestamptz,
  price_amount bigint not null default 0,
  currency varchar(12) not null default 'CNY',
  status varchar(32) not null default 'draft',
  description text,
  created_by uuid,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_ticket_products_price check (price_amount >= 0),
  constraint chk_ticket_products_type check (
    product_type in (
      'platform_ticket',
      'bar_ticket',
      'amusement_ticket',
      'event_ticket',
      'package',
      'group_ticket',
      'member_benefit',
      'gift_ticket'
    )
  ),
  constraint chk_ticket_products_valid_type check (
    valid_type in ('fixed_date', 'date_range', 'after_purchase', 'session')
  ),
  constraint chk_ticket_products_status check (
    status in ('draft', 'active', 'paused', 'sold_out', 'archived')
  )
);

create index if not exists idx_ticket_products_owner_merchant on ticket_products(owner_merchant_id);
create index if not exists idx_ticket_products_status on ticket_products(status);
create index if not exists idx_ticket_products_sales_window on ticket_products(sales_start_at, sales_end_at);

create trigger trg_ticket_products_updated_at
before update on ticket_products
for each row execute function set_updated_at();

create table if not exists entitlement_templates (
  id uuid primary key default gen_random_uuid(),
  merchant_id uuid,
  venue_id uuid,
  project_id uuid,
  entitlement_type varchar(48) not null,
  name varchar(160) not null,
  default_total_times integer not null default 1,
  default_value_amount bigint not null default 0,
  settlement_rule_id uuid,
  requires_reservation boolean not null default false,
  requires_video_evidence boolean not null default false,
  requires_staff_confirmation boolean not null default false,
  status varchar(32) not null default 'active',
  description text,
  created_by uuid,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_entitlement_templates_times check (default_total_times > 0),
  constraint chk_entitlement_templates_value check (default_value_amount >= 0),
  constraint chk_entitlement_templates_type check (
    entitlement_type in (
      'entry',
      'bar_entry',
      'drink_redeem',
      'bar_table',
      'amusement_once',
      'amusement_pass',
      'fast_pass',
      'catering_coupon',
      'retail_coupon',
      'parking',
      'other'
    )
  ),
  constraint chk_entitlement_templates_status check (status in ('active', 'inactive'))
);

create index if not exists idx_entitlement_templates_merchant on entitlement_templates(merchant_id);
create index if not exists idx_entitlement_templates_venue on entitlement_templates(venue_id);
create index if not exists idx_entitlement_templates_project on entitlement_templates(project_id);
create index if not exists idx_entitlement_templates_type on entitlement_templates(entitlement_type);

create trigger trg_entitlement_templates_updated_at
before update on entitlement_templates
for each row execute function set_updated_at();

create table if not exists product_entitlement_templates (
  id uuid primary key default gen_random_uuid(),
  ticket_product_id uuid not null references ticket_products(id),
  entitlement_template_id uuid not null references entitlement_templates(id),
  quantity integer not null default 1,
  total_times integer not null default 1,
  value_amount bigint not null default 0,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_product_entitlements_quantity check (quantity > 0),
  constraint chk_product_entitlements_times check (total_times > 0),
  constraint chk_product_entitlements_value check (value_amount >= 0)
);

create unique index if not exists uq_product_entitlement_template
on product_entitlement_templates(ticket_product_id, entitlement_template_id)
where deleted_at is null;

create index if not exists idx_product_entitlements_product on product_entitlement_templates(ticket_product_id);
create index if not exists idx_product_entitlements_template on product_entitlement_templates(entitlement_template_id);

create trigger trg_product_entitlements_updated_at
before update on product_entitlement_templates
for each row execute function set_updated_at();

create table if not exists ticket_inventories (
  id uuid primary key default gen_random_uuid(),
  ticket_product_id uuid not null references ticket_products(id),
  inventory_date date not null,
  total_quantity integer not null default 0,
  sold_quantity integer not null default 0,
  locked_quantity integer not null default 0,
  refunded_quantity integer not null default 0,
  status varchar(32) not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_ticket_inventory_nonnegative check (
    total_quantity >= 0 and sold_quantity >= 0 and locked_quantity >= 0 and refunded_quantity >= 0
  ),
  constraint chk_ticket_inventory_status check (status in ('active', 'closed'))
);

create unique index if not exists uq_ticket_inventory_product_date
on ticket_inventories(ticket_product_id, inventory_date);

create trigger trg_ticket_inventory_updated_at
before update on ticket_inventories
for each row execute function set_updated_at();

create table if not exists sales_channels (
  id uuid primary key default gen_random_uuid(),
  channel_code varchar(64) not null unique,
  channel_name varchar(120) not null,
  channel_type varchar(40) not null,
  merchant_id uuid,
  open_app_id uuid,
  status varchar(32) not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_sales_channels_type check (
    channel_type in (
      'self_miniapp',
      'official_web',
      'offline_counter',
      'douyin',
      'meituan',
      'ctrip',
      'travel_agency',
      'open_api',
      'other'
    )
  ),
  constraint chk_sales_channels_status check (status in ('active', 'inactive'))
);

create index if not exists idx_sales_channels_merchant on sales_channels(merchant_id);
create index if not exists idx_sales_channels_open_app on sales_channels(open_app_id);

create trigger trg_sales_channels_updated_at
before update on sales_channels
for each row execute function set_updated_at();

create table if not exists ticket_orders (
  id uuid primary key default gen_random_uuid(),
  order_no varchar(64) not null unique,
  customer_id uuid,
  channel_id uuid references sales_channels(id),
  total_amount bigint not null default 0,
  discount_amount bigint not null default 0,
  payable_amount bigint not null default 0,
  paid_amount bigint not null default 0,
  refund_amount bigint not null default 0,
  currency varchar(12) not null default 'CNY',
  status varchar(40) not null default 'pending_payment',
  external_order_no varchar(128),
  buyer_name varchar(120),
  buyer_phone_masked varchar(40),
  remark text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  paid_at timestamptz,
  closed_at timestamptz,
  constraint chk_ticket_order_amounts check (
    total_amount >= 0 and discount_amount >= 0 and payable_amount >= 0 and paid_amount >= 0 and refund_amount >= 0
  ),
  constraint chk_ticket_order_status check (
    status in (
      'pending_payment',
      'paid',
      'ticket_issued',
      'partially_verified',
      'completed',
      'refunding',
      'partially_refunded',
      'refunded',
      'cancelled',
      'closed'
    )
  )
);

create index if not exists idx_ticket_orders_customer on ticket_orders(customer_id);
create index if not exists idx_ticket_orders_channel on ticket_orders(channel_id);
create index if not exists idx_ticket_orders_status on ticket_orders(status);
create index if not exists idx_ticket_orders_created_at on ticket_orders(created_at);
create index if not exists idx_ticket_orders_external_order_no on ticket_orders(external_order_no);

create trigger trg_ticket_orders_updated_at
before update on ticket_orders
for each row execute function set_updated_at();

create table if not exists order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid not null references ticket_orders(id),
  ticket_product_id uuid not null references ticket_products(id),
  quantity integer not null,
  unit_price_amount bigint not null default 0,
  discount_amount bigint not null default 0,
  payable_amount bigint not null default 0,
  status varchar(32) not null default 'normal',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_order_items_quantity check (quantity > 0),
  constraint chk_order_items_amounts check (
    unit_price_amount >= 0 and discount_amount >= 0 and payable_amount >= 0
  ),
  constraint chk_order_items_status check (status in ('normal', 'refunded', 'cancelled'))
);

create index if not exists idx_order_items_order on order_items(order_id);
create index if not exists idx_order_items_product on order_items(ticket_product_id);

create trigger trg_order_items_updated_at
before update on order_items
for each row execute function set_updated_at();

create table if not exists issued_tickets (
  id uuid primary key default gen_random_uuid(),
  ticket_no varchar(64) not null unique,
  order_id uuid not null references ticket_orders(id),
  order_item_id uuid not null references order_items(id),
  ticket_product_id uuid not null references ticket_products(id),
  customer_id uuid,
  qr_code_hash varchar(128),
  nfc_binding_id uuid,
  valid_start_at timestamptz,
  valid_end_at timestamptz,
  status varchar(40) not null default 'unused',
  issued_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_issued_tickets_status check (
    status in ('unused', 'partially_used', 'used', 'expired', 'frozen', 'refunded', 'voided')
  )
);

create index if not exists idx_issued_tickets_order on issued_tickets(order_id);
create index if not exists idx_issued_tickets_customer on issued_tickets(customer_id);
create index if not exists idx_issued_tickets_status on issued_tickets(status);
create index if not exists idx_issued_tickets_qr_hash on issued_tickets(qr_code_hash);

create trigger trg_issued_tickets_updated_at
before update on issued_tickets
for each row execute function set_updated_at();

create table if not exists issued_entitlements (
  id uuid primary key default gen_random_uuid(),
  issued_ticket_id uuid not null references issued_tickets(id),
  entitlement_template_id uuid not null references entitlement_templates(id),
  merchant_id uuid,
  venue_id uuid,
  project_id uuid,
  entitlement_type varchar(48) not null,
  total_times integer not null default 1,
  used_times integer not null default 0,
  remaining_times integer not null default 1,
  value_amount bigint not null default 0,
  valid_start_at timestamptz,
  valid_end_at timestamptz,
  status varchar(40) not null default 'unused',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_issued_entitlements_times check (
    total_times > 0 and used_times >= 0 and remaining_times >= 0 and used_times + remaining_times <= total_times
  ),
  constraint chk_issued_entitlements_value check (value_amount >= 0),
  constraint chk_issued_entitlements_status check (
    status in ('unused', 'partially_used', 'used', 'expired', 'frozen', 'refunded', 'voided')
  )
);

create index if not exists idx_issued_entitlements_ticket on issued_entitlements(issued_ticket_id);
create index if not exists idx_issued_entitlements_template on issued_entitlements(entitlement_template_id);
create index if not exists idx_issued_entitlements_merchant on issued_entitlements(merchant_id);
create index if not exists idx_issued_entitlements_status on issued_entitlements(status);
create index if not exists idx_issued_entitlements_valid_window on issued_entitlements(valid_start_at, valid_end_at);

create trigger trg_issued_entitlements_updated_at
before update on issued_entitlements
for each row execute function set_updated_at();

create table if not exists verification_records (
  id uuid primary key default gen_random_uuid(),
  verification_no varchar(64) not null unique,
  issued_entitlement_id uuid not null references issued_entitlements(id),
  issued_ticket_id uuid not null references issued_tickets(id),
  merchant_id uuid,
  venue_id uuid,
  project_id uuid,
  location_point_id uuid,
  staff_id uuid,
  device_id uuid,
  open_app_id uuid,
  verification_method varchar(40) not null,
  verify_times integer not null default 1,
  result varchar(32) not null,
  failure_reason text,
  verified_at timestamptz not null default now(),
  reversed_at timestamptz,
  reverse_reason text,
  created_at timestamptz not null default now(),
  constraint chk_verification_times check (verify_times > 0),
  constraint chk_verification_method check (
    verification_method in ('qr', 'nfc', 'gate', 'manual', 'third_party', 'offline_sync')
  ),
  constraint chk_verification_result check (result in ('success', 'failed', 'reversed'))
);

create index if not exists idx_verifications_entitlement on verification_records(issued_entitlement_id);
create index if not exists idx_verifications_ticket on verification_records(issued_ticket_id);
create index if not exists idx_verifications_merchant on verification_records(merchant_id);
create index if not exists idx_verifications_verified_at on verification_records(verified_at);
create index if not exists idx_verifications_open_app on verification_records(open_app_id);

create table if not exists payment_records (
  id uuid primary key default gen_random_uuid(),
  payment_no varchar(64) not null unique,
  order_id uuid not null references ticket_orders(id),
  payment_channel varchar(40) not null,
  external_payment_no varchar(128),
  amount bigint not null,
  currency varchar(12) not null default 'CNY',
  status varchar(32) not null default 'pending',
  paid_at timestamptz,
  raw_payload jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_payment_amount check (amount >= 0),
  constraint chk_payment_channel check (
    payment_channel in ('wechat', 'alipay', 'bank_card', 'cash', 'transfer', 'stored_value', 'other')
  ),
  constraint chk_payment_status check (
    status in ('pending', 'processing', 'success', 'failed', 'cancelled', 'reversed')
  )
);

create index if not exists idx_payments_order on payment_records(order_id);
create index if not exists idx_payments_external_no on payment_records(external_payment_no);
create index if not exists idx_payments_status on payment_records(status);

create trigger trg_payment_records_updated_at
before update on payment_records
for each row execute function set_updated_at();

create table if not exists refund_orders (
  id uuid primary key default gen_random_uuid(),
  refund_no varchar(64) not null unique,
  order_id uuid not null references ticket_orders(id),
  payment_record_id uuid references payment_records(id),
  refund_type varchar(32) not null,
  refund_amount bigint not null,
  currency varchar(12) not null default 'CNY',
  reason text,
  approval_status varchar(32) not null default 'none',
  status varchar(32) not null default 'pending',
  requested_by uuid,
  approved_by uuid,
  refunded_at timestamptz,
  raw_payload jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_refund_amount check (refund_amount >= 0),
  constraint chk_refund_type check (refund_type in ('full', 'partial', 'entitlement')),
  constraint chk_refund_approval_status check (approval_status in ('none', 'pending', 'approved', 'rejected')),
  constraint chk_refund_status check (
    status in ('pending', 'processing', 'success', 'failed', 'cancelled')
  )
);

create index if not exists idx_refunds_order on refund_orders(order_id);
create index if not exists idx_refunds_payment on refund_orders(payment_record_id);
create index if not exists idx_refunds_status on refund_orders(status);

create trigger trg_refund_orders_updated_at
before update on refund_orders
for each row execute function set_updated_at();

create table if not exists settlement_rules (
  id uuid primary key default gen_random_uuid(),
  merchant_id uuid not null,
  rule_name varchar(160) not null,
  rule_type varchar(40) not null,
  fixed_amount bigint,
  percentage numeric(8, 4),
  settlement_cycle varchar(32) not null default 'monthly',
  effective_start_at timestamptz,
  effective_end_at timestamptz,
  status varchar(32) not null default 'active',
  created_by uuid,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint chk_settlement_rule_amount check (fixed_amount is null or fixed_amount >= 0),
  constraint chk_settlement_rule_percentage check (percentage is null or (percentage >= 0 and percentage <= 1)),
  constraint chk_settlement_rule_type check (
    rule_type in ('fixed_amount', 'percentage', 'by_verification', 'guarantee_plus_share', 'subsidy')
  ),
  constraint chk_settlement_cycle check (
    settlement_cycle in ('daily', 'weekly', 'monthly', 'event_based')
  ),
  constraint chk_settlement_rule_status check (status in ('active', 'inactive'))
);

create index if not exists idx_settlement_rules_merchant on settlement_rules(merchant_id);
create index if not exists idx_settlement_rules_status on settlement_rules(status);

create trigger trg_settlement_rules_updated_at
before update on settlement_rules
for each row execute function set_updated_at();

create table if not exists settlement_items (
  id uuid primary key default gen_random_uuid(),
  merchant_id uuid not null,
  settlement_rule_id uuid references settlement_rules(id),
  order_id uuid references ticket_orders(id),
  issued_entitlement_id uuid references issued_entitlements(id),
  verification_record_id uuid references verification_records(id),
  refund_order_id uuid references refund_orders(id),
  source_type varchar(32) not null,
  amount bigint not null,
  currency varchar(12) not null default 'CNY',
  status varchar(32) not null default 'pending',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_settlement_item_source check (
    source_type in ('sale', 'verification', 'refund', 'adjustment', 'subsidy')
  ),
  constraint chk_settlement_item_status check (
    status in ('pending', 'included', 'reversed', 'cancelled')
  )
);

create index if not exists idx_settlement_items_merchant on settlement_items(merchant_id);
create index if not exists idx_settlement_items_status on settlement_items(status);
create index if not exists idx_settlement_items_order on settlement_items(order_id);
create index if not exists idx_settlement_items_verification on settlement_items(verification_record_id);

create trigger trg_settlement_items_updated_at
before update on settlement_items
for each row execute function set_updated_at();

create table if not exists settlement_bills (
  id uuid primary key default gen_random_uuid(),
  bill_no varchar(64) not null unique,
  merchant_id uuid not null,
  period_start timestamptz not null,
  period_end timestamptz not null,
  gross_amount bigint not null default 0,
  refund_deduct_amount bigint not null default 0,
  adjustment_amount bigint not null default 0,
  payable_amount bigint not null default 0,
  currency varchar(12) not null default 'CNY',
  status varchar(32) not null default 'draft',
  confirmed_by uuid,
  confirmed_at timestamptz,
  paid_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_settlement_bill_period check (period_end > period_start),
  constraint chk_settlement_bill_amounts check (
    gross_amount >= 0 and refund_deduct_amount >= 0
  ),
  constraint chk_settlement_bill_status check (
    status in ('draft', 'pending_confirm', 'confirmed', 'paying', 'paid', 'disputed', 'adjusted', 'cancelled')
  )
);

create index if not exists idx_settlement_bills_merchant on settlement_bills(merchant_id);
create index if not exists idx_settlement_bills_period on settlement_bills(period_start, period_end);
create index if not exists idx_settlement_bills_status on settlement_bills(status);

create trigger trg_settlement_bills_updated_at
before update on settlement_bills
for each row execute function set_updated_at();

create table if not exists settlement_bill_items (
  id uuid primary key default gen_random_uuid(),
  settlement_bill_id uuid not null references settlement_bills(id),
  settlement_item_id uuid not null references settlement_items(id),
  created_at timestamptz not null default now()
);

create unique index if not exists uq_settlement_bill_item
on settlement_bill_items(settlement_bill_id, settlement_item_id);

create table if not exists reconciliation_records (
  id uuid primary key default gen_random_uuid(),
  reconciliation_date date not null,
  source_type varchar(40) not null,
  source_id varchar(128),
  system_amount bigint not null default 0,
  external_amount bigint not null default 0,
  difference_amount bigint not null default 0,
  result varchar(40) not null,
  status varchar(32) not null default 'pending',
  detail jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chk_reconciliation_source check (
    source_type in ('payment', 'channel', 'third_party_pos', 'settlement')
  ),
  constraint chk_reconciliation_result check (
    result in ('matched', 'different', 'missing_internal', 'missing_external')
  ),
  constraint chk_reconciliation_status check (
    status in ('pending', 'confirmed', 'adjusted', 'ignored')
  )
);

create index if not exists idx_reconciliation_date on reconciliation_records(reconciliation_date);
create index if not exists idx_reconciliation_result on reconciliation_records(result);
create index if not exists idx_reconciliation_status on reconciliation_records(status);

create trigger trg_reconciliation_records_updated_at
before update on reconciliation_records
for each row execute function set_updated_at();
