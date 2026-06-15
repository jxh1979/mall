create table if not exists TicketProduct (
  id text primary key,
  productCode text not null unique,
  productName text not null,
  productType text not null,
  priceAmount integer not null default 0,
  currency text not null default 'CNY',
  status text not null default 'draft',
  description text,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp,
  deletedAt datetime
);

create table if not exists EntitlementTemplate (
  id text primary key,
  name text not null,
  entitlementType text not null,
  merchantId text,
  venueId text,
  projectId text,
  defaultTotalTimes integer not null default 1,
  defaultValueAmount integer not null default 0,
  status text not null default 'active',
  description text,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp,
  deletedAt datetime
);

create table if not exists ProductEntitlement (
  id text primary key,
  ticketProductId text not null,
  entitlementTemplateId text not null,
  quantity integer not null default 1,
  totalTimes integer not null default 1,
  valueAmount integer not null default 0,
  sortOrder integer not null default 0
);

create table if not exists IssuedTicket (
  id text primary key,
  ticketCode text not null unique,
  ticketProductId text not null,
  purchaserName text,
  purchaserPhone text,
  status text not null default 'active',
  issuedAt datetime not null default current_timestamp,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists IssuedEntitlement (
  id text primary key,
  entitlementCode text not null unique,
  issuedTicketId text not null,
  entitlementTemplateId text not null,
  entitlementType text not null,
  merchantId text,
  venueId text,
  projectId text,
  totalTimes integer not null default 1,
  usedTimes integer not null default 0,
  valueAmount integer not null default 0,
  status text not null default 'active',
  validFrom datetime,
  validTo datetime,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists VerificationRecord (
  id text primary key,
  verificationNo text not null unique,
  issuedEntitlementId text not null,
  merchantId text,
  venueId text,
  projectId text,
  staffId text,
  deviceId text,
  source text not null default 'backend',
  status text not null,
  resultReason text,
  createdAt datetime not null default current_timestamp
);

create table if not exists SettlementItem (
  id text primary key,
  verificationRecordId text not null,
  merchantId text,
  amount integer not null default 0,
  status text not null default 'pending',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists SettlementBill (
  id text primary key,
  billNo text not null unique,
  merchantId text,
  totalAmount integer not null default 0,
  status text not null default 'draft',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists BarTable (
  id text primary key,
  merchantId text,
  venueId text,
  tableCode text not null unique,
  tableName text not null,
  capacity integer,
  minSpendAmount integer not null default 0,
  status text not null default 'idle',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists BarProduct (
  id text primary key,
  merchantId text,
  venueId text,
  category text not null,
  productName text not null,
  priceAmount integer not null default 0,
  status text not null default 'active',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists BarOrder (
  id text primary key,
  orderNo text not null unique,
  tableId text not null,
  merchantId text,
  venueId text,
  status text not null default 'open',
  totalAmount integer not null default 0,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists BarOrderItem (
  id text primary key,
  orderId text not null,
  productId text not null,
  quantity integer not null default 1,
  unitPrice integer not null default 0,
  lineAmount integer not null default 0,
  status text not null default 'ordered',
  createdAt datetime not null default current_timestamp
);

create table if not exists BarBill (
  id text primary key,
  billNo text not null unique,
  orderId text not null,
  tableId text not null,
  totalAmount integer not null default 0,
  paidAmount integer not null default 0,
  paymentMethod text,
  status text not null default 'unpaid',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists AmusementProject (
  id text primary key,
  merchantId text,
  venueId text,
  projectCode text not null unique,
  projectName text not null,
  capacity integer not null default 0,
  status text not null default 'active',
  description text,
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists AmusementProjectRule (
  id text primary key,
  projectId text not null,
  ruleType text not null,
  ruleValue text not null,
  status text not null default 'active',
  createdAt datetime not null default current_timestamp
);

create table if not exists AmusementSession (
  id text primary key,
  projectId text not null,
  sessionName text not null,
  startsAt datetime not null,
  endsAt datetime not null,
  capacity integer not null default 0,
  usedCapacity integer not null default 0,
  status text not null default 'scheduled',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists AmusementVerification (
  id text primary key,
  verificationNo text not null unique,
  projectId text not null,
  sessionId text not null,
  issuedEntitlementId text,
  staffId text,
  status text not null,
  resultReason text,
  createdAt datetime not null default current_timestamp
);

create table if not exists MetricSnapshot (
  id text primary key,
  metricCode text not null,
  metricName text not null,
  metricValue integer not null default 0,
  dimensionType text not null default 'platform',
  dimensionId text,
  snapshotAt datetime not null default current_timestamp,
  createdAt datetime not null default current_timestamp
);

create table if not exists RiskRule (
  id text primary key,
  ruleCode text not null unique,
  ruleName text not null,
  ruleType text not null,
  threshold integer not null default 0,
  status text not null default 'active',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);

create table if not exists RiskAlert (
  id text primary key,
  ruleId text,
  alertType text not null,
  riskLevel text not null default 'normal',
  objectType text,
  objectId text,
  summary text not null,
  status text not null default 'open',
  createdAt datetime not null default current_timestamp,
  updatedAt datetime not null default current_timestamp
);
