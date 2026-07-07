import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { readdirSync } from "node:fs";
import { join } from "node:path";

const migrationDir = "backend/src/main/resources/db/migration";
const outputPath = "docs/domain-ownership-scope-audit-v0.1.csv";
const check = process.argv.includes("--check");

const directColumns = [
  "project_id",
  "venue_id",
  "merchant_id",
  "organization_id",
  "business_entity_id",
  "business_object_id",
  "settlement_entity_id",
  "contract_entity_id",
  "operation_entity_id",
];

const inheritedRules = [
  { pattern: /^bar_order_items$/, source: "order_id", path: "order_id -> bar_orders -> table_id/bar_id -> venue/project scope" },
  { pattern: /^bar_bill/, source: "bill/order linkage", path: "bill/order linkage -> bar_orders -> venue/project scope" },
  { pattern: /^bar_production_ticket/, source: "order_id", path: "order_id -> bar_orders -> venue/project scope" },
  { pattern: /^bar_table_guest_checkins$/, source: "reservation/table linkage", path: "reservation_id/table_id -> bar_reservations/bar_tables -> venue/project scope" },
  { pattern: /^bar_commission_calculations$/, source: "bill_id", path: "bill_id -> bar_bills -> bar_orders -> venue/project scope" },
  { pattern: /^saved_bottle_claims$/, source: "saved_bottle_id", path: "saved_bottle_id -> saved_bottles -> merchant/customer scope" },
  { pattern: /^ticket_order_items$/, source: "order_id", path: "order_id -> ticket_orders -> ticket_type/project scope" },
  { pattern: /^ticket_credentials$/, source: "order_id/entitlement_id", path: "order_id/entitlement_id -> ticket_orders/issued_entitlements -> ticket_type/project scope" },
  { pattern: /^ticket_payment_intents$/, source: "order_id", path: "order_id -> ticket_orders -> ticket_type/project scope" },
  { pattern: /^ticket_refund_lines$/, source: "refund_id", path: "refund_id -> ticket_refunds -> order_id -> ticket_orders" },
  { pattern: /^ticket_type_entitlements$/, source: "ticket_type_id", path: "ticket_type_id -> ticket_types -> project scope" },
  { pattern: /^customer_points_/, source: "customer_id", path: "customer_id -> customers -> project/customer scope" },
  { pattern: /^customer_sessions$/, source: "customer_id", path: "customer_id -> customers -> project/customer scope" },
  { pattern: /^customer_consents$/, source: "customer_id", path: "customer_id -> customers -> project/customer scope" },
  { pattern: /^amusement_.*actions$/, source: "inspection/incident id", path: "inspection/incident id -> amusement project/session scope" },
  { pattern: /^amusement_waiver_acknowledgements$/, source: "reservation/customer linkage", path: "reservation/customer linkage -> amusement project/session scope" },
  { pattern: /^merchant_bill_/, source: "bill_id", path: "bill_id -> merchant_bills -> merchant_id" },
  { pattern: /^merchant_service_work_order_actions$/, source: "work_order_id", path: "work_order_id -> merchant_service_work_orders -> merchant_id/space_id" },
  { pattern: /^ar_ap_item_sources$/, source: "ar_ap_item_id", path: "ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope" },
  { pattern: /^ar_ap_payments$/, source: "ar_ap_item_id", path: "ar_ap_item_id -> ar_ap_items/finance_ar_ap_items -> merchant/entity scope" },
  { pattern: /^finance_ar_ap_payments$/, source: "ar_ap_item_id", path: "ar_ap_item_id -> finance_ar_ap_items -> merchant/entity scope" },
  { pattern: /^finance_reconciliation_differences$/, source: "batch_id", path: "batch_id -> finance_reconciliation_batches -> project/entity scope" },
  { pattern: /^inventory_stock_/, source: "item_id/location", path: "item_id/location -> inventory_items/location -> project/merchant scope" },
  { pattern: /^inventory_trace_handovers$/, source: "movement/batch linkage", path: "movement/batch linkage -> inventory_stock_movements/batches" },
  { pattern: /^permission_template_/, source: "template_id", path: "template_id -> permission_templates -> organization/global scope" },
  { pattern: /^role_permissions$/, source: "role_id/permission_id", path: "role_id/permission_id -> roles/permissions" },
  { pattern: /^user_roles$/, source: "user_id/role_id", path: "user_id/role_id -> access_users/roles -> organization scope" },
  { pattern: /^user_permission_overrides$/, source: "user_id", path: "user_id -> access_users -> organization scope" },
  { pattern: /^terminal_/, source: "terminal/device id", path: "terminal/device id -> venue/project deployment scope" },
  { pattern: /^open_/, source: "open app/endpoint key", path: "open app/endpoint key -> provider/project integration scope" },
  { pattern: /^integration_/, source: "provider_code/project linkage", path: "provider_code/project linkage -> integration_provider_adapters" },
  { pattern: /^evidence_/, source: "source_type/source_id/trace_id", path: "source_type/source_id/trace_id -> audited business object" },
  { pattern: /^audit_events$/, source: "source_type/source_id/trace_id", path: "source_type/source_id/trace_id -> audited business object" },
  { pattern: /^auth_/, source: "user_id/session", path: "user_id/session -> access_users -> organization scope" },
  { pattern: /^idempotency_records$/, source: "operation_key", path: "operation_key -> request route + actor + business operation" },
  { pattern: /^production_readiness_/, source: "owner/blocker code", path: "owner/blocker code -> production evidence scope" },
  { pattern: /^analytics_/, source: "dimension/project/merchant", path: "dimension/project/merchant fields or derived source table" },
  { pattern: /^compliance_/, source: "subject/contract linkage", path: "subject/contract linkage -> merchant/contract scope" },
  { pattern: /^floor_plan_/, source: "floor_plan_id/task_id", path: "floor_plan_id/task_id -> mall_floor_plans -> project/venue scope" },
];

const globalTables = new Set([
  "permissions",
  "roles",
  "platform_migration_baseline",
  // 集中数据清理台账（Tier-D）：平台级操作台账，记谁发起/审批/删了几条；非租户数据边界。
  "data_cleanup_jobs",
]);

function readMigrations() {
  return readdirSync(migrationDir)
    .filter((name) => name.endsWith(".sql"))
    .sort()
    .map((name) => ({ name, text: readFileSync(join(migrationDir, name), "utf8") }));
}

function collectDefinitions(migrations) {
  const definitions = new Map();
  for (const migration of migrations) {
    const createPattern = /CREATE TABLE(?: IF NOT EXISTS)?\s+([a-zA-Z0-9_]+)\s*\(([\s\S]*?)\);/gi;
    let match;
    while ((match = createPattern.exec(migration.text)) !== null) {
      const table = match[1];
      definitions.set(table, `${definitions.get(table) ?? ""}\n-- ${migration.name}\n${match[2]}`);
    }
    const alterPattern = /ALTER TABLE(?: IF EXISTS)?\s+([a-zA-Z0-9_]+)\s+ADD COLUMN[\s\S]*?;/gi;
    while ((match = alterPattern.exec(migration.text)) !== null) {
      const table = match[1];
      definitions.set(table, `${definitions.get(table) ?? ""}\n-- ${migration.name}\n${match[0]}`);
    }
  }
  return definitions;
}

function findColumn(definition, columns) {
  const definitionTokens = new Set(definition.toLowerCase().match(/[a-z0-9_]+/g) ?? []);
  return columns.find((column) => definitionTokens.has(column.toLowerCase()));
}

// 显式 analytics 表 scope 覆盖（逐表实证定性，出处见 SEC-ANALYTICS-ISOLATION-REGISTER）。
// 这 4 张表 /^analytics_/ 会被 inheritedRules 统包成 inherited（信息性盲区），但静态列检测无法表达其真实语义，
// 故在此按 Repository 读写路径 + Controller 门控的实证结论精确覆盖，消除盲区：
//   - analytics_business_metric_snapshots: 无单一 scope 列，但 (segment_type,segment_id) 为多态维度自持
//     scoped 读通道（JdbcAnalyticsRepository readMetricSnapshots/readProjectMetricSnapshots:
//     'segment_type=:.. AND segment_id=:..'；商户端 Controller /dashboards/* 走 currentMerchantScope 行级隔离）
//     → direct(segment_id)，验证器可对其 'segment_id = :' scoped 谓词做 F1 校验。
//   - analytics_anomaly_config: 单例 id='default' 的平台级异常阈值配置（repo WHERE id='default'；
//     Controller /anomaly-config 强制 requirePlatformScope）→ 有意平台级全局。
//   - analytics_ai_quality_tasks/results: 平台级 AI 质检任务/结果，全表读无租户过滤
//     （Controller /ai-quality/* 强制 requirePlatformScope）→ 有意平台级全局。
const ANALYTICS_SCOPE_OVERRIDES = new Map([
  ["analytics_business_metric_snapshots", {
    scope_type: "direct",
    scope_source: "segment_id",
    scope_path: "polymorphic (segment_type, segment_id) self-owned scoped-read channel on analytics_business_metric_snapshots",
    multi_complex_note: "segment_type in {platform(NULL),merchant,project}; merchant/project rows scoped by segment_id (AC-ISO-1)",
  }],
  ["analytics_anomaly_config", {
    scope_type: "global_reference",
    scope_source: "global_reference",
    scope_path: "platform-global anomaly threshold config (singleton id='default'); read/write gated by requirePlatformScope",
    multi_complex_note: "intentional platform-global; per-tenant thresholds would require a separate scoped table",
  }],
  ["analytics_ai_quality_tasks", {
    scope_type: "global_reference",
    scope_source: "global_reference",
    scope_path: "platform-global AI quality task ledger; unfiltered read gated by requirePlatformScope",
    multi_complex_note: "intentional platform-global AI governance output; not a tenant access boundary",
  }],
  ["analytics_ai_quality_results", {
    scope_type: "global_reference",
    scope_source: "global_reference",
    scope_path: "platform-global AI quality result ledger; unfiltered read gated by requirePlatformScope",
    multi_complex_note: "intentional platform-global AI governance output; not a tenant access boundary",
  }],
]);

function classify(table, definition) {
  const overridden = ANALYTICS_SCOPE_OVERRIDES.get(table);
  if (overridden) return overridden;
  const direct = findColumn(definition, directColumns);
  if (direct) {
    return {
      scope_type: "direct",
      scope_source: direct,
      scope_path: `${direct} column on ${table}`,
      multi_complex_note: "ready for project/merchant scoped query enforcement",
    };
  }
  if (globalTables.has(table)) {
    return {
      scope_type: "global_reference",
      scope_source: "global_reference",
      scope_path: "global reference/configuration table",
      multi_complex_note: "safe as global reference; tenant-specific overrides require separate scoped table",
    };
  }
  const inherited = inheritedRules.find((rule) => rule.pattern.test(table));
  if (inherited) {
    return {
      scope_type: "inherited",
      scope_source: inherited.source,
      scope_path: inherited.path,
      multi_complex_note: "must join through scope path before multi-complex rollout",
    };
  }
  return {
    scope_type: "single_complex_default",
    scope_source: "P0_SINGLE_COMPLEX",
    scope_path: "single-complex default scope; no direct scope column detected",
    multi_complex_note: "before multi-complex rollout, add direct project_id/merchant_id or document parent join",
  };
}

function csvEscape(value) {
  const text = String(value ?? "");
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

function buildCsv() {
  const definitions = collectDefinitions(readMigrations());
  const rows = [...definitions.keys()].sort().map((table) => {
    const classification = classify(table, definitions.get(table) ?? "");
    return {
      table_name: table,
      ...classification,
      status: "covered",
    };
  });
  const headers = ["table_name", "scope_type", "scope_source", "scope_path", "multi_complex_note", "status"];
  return `${headers.join(",")}\n${rows.map((row) => headers.map((header) => csvEscape(row[header])).join(",")).join("\n")}\n`;
}

const next = buildCsv();
if (check) {
  if (!existsSync(outputPath)) {
    console.error(`${outputPath} is missing; run npm run docs:domain-ownership-scope`);
    process.exit(1);
  }
  const current = readFileSync(outputPath, "utf8");
  if (current !== next) {
    console.error(`${outputPath} is out of sync with Flyway migrations; run npm run docs:domain-ownership-scope`);
    process.exit(1);
  }
  const rows = next.trim().split(/\n/).slice(1);
  const uncovered = rows.filter((line) => !line.endsWith(",covered"));
  if (uncovered.length > 0) {
    console.error(`domain ownership scope audit has ${uncovered.length} uncovered rows`);
    process.exit(1);
  }
  console.log(`DOMAIN_OWNERSHIP_SCOPE_PASS: ${rows.length} tables classified`);
} else {
  writeFileSync(outputPath, next);
  console.log(`generated ${outputPath}`);
}
