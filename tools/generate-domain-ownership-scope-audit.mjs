// 文件版本：V0.12
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
  { pattern: /^amusement_session_followups$/, source: "ending_id", path: "ending_id -> amusement_session_endings -> actual complex_id; source entitlement/order and merchant scope also retained" },
  { pattern: /^purchase_quantity_resolutions$/, source: "purchase_id", path: "purchase_id -> inventory_purchase_requests -> project_id/merchant_id" },
  { pattern: /^purchase_(consignment_sales|returns)$/, source: "receipt_id", path: "receipt_id -> purchase_receipts -> original purchase project_id/merchant_id" },
  { pattern: /^purchase_prepayment_resolutions$/, source: "purchase_id/source_payment_id/original_item_id", path: "purchase -> project_id/merchant_id; original payment or payable and supplier refund must match the same procurement ownership" },
  { pattern: /^purchase_prepayment_allocations$/, source: "purchase_id/source_payment_id/target_item_id", path: "purchase -> project_id/merchant_id; original payment and target payable must match supplier, project and ownership" },
  { pattern: /^purchase_payable_adjustments$/, source: "return_id/original_item_id", path: "return -> receipt -> purchase project_id/merchant_id; reversal and replacement preserve original payable ownership" },
  { pattern: /^energy_effective_meter_readings$/, source: "meter_id", path: "view preserves energy_meter_readings rows; meter_id -> energy_meters -> project_id/merchant_id; approved verification changes values only" },
  { pattern: /^dealer_return_freight_adjustments$/, source: "order_id/return_id", path: "original order -> locked contract warehouse -> project_id; return must belong to that order" },
  { pattern: /^energy_(meter_openings|meter_parent_transfers|meter_retirements)$/, source: "meter_id", path: "meter_id -> energy_meters -> project_id/merchant_id" },
  { pattern: /^energy_meter_replacements$/, source: "old_meter_id/new_meter_id", path: "old/new meter -> energy_meters -> same project and merchant" },
  { pattern: /^energy_(reading_evidence|reading_verifications|anomaly_escalations|verifier_assignments)$/, source: "reading_id", path: "reading_id -> energy_meter_readings -> energy_meters -> project_id/merchant_id" },
  { pattern: /^customer_points_recovery_waivers$/, source: "customer_id/lines", path: "all customer recovery lines -> original reward project_id/merchant_id; actor must hold both finance view and waive scope for every source" },
  { pattern: /^dealer_order_overpayment_allocations$/, source: "overpayment_id/order_id", path: "overpayment_id -> dealer_order_overpayments -> original order contract warehouse -> project_id; target order project and same dealer/sales entity checked separately" },
  { pattern: /^dealer_order_overpayments$/, source: "order_id", path: "order_id -> locked dealer contract warehouse -> project_id; limited dealer project must match; no default project" },
  { pattern: /^dealer_order_refunds$/, source: "payment_id/order_id", path: "original payment order -> locked contract warehouse -> project_id; allocation target order scope also required" },
  { pattern: /^dealer_admission_submissions$/, source: "dealer_id", path: "dealer_id -> dealers -> project_id; reads and writes require authorized dealer scope" },
  { pattern: /^finance_refund_attempts$/, source: "refund_id", path: "refund_id -> finance_refunds -> project_id/merchant_id; every read and write inherits the parent refund scope" },
  { pattern: /^staff_shift_revisions$/, source: "review_id/shift_id", path: "review_id -> supervisor_reviews; published shift_id -> staff_shifts -> staff_profiles company/merchant scope" },
  { pattern: /^staff_locker_records$/, source: "staff_profile_id", path: "staff_profile_id -> staff_profiles -> current authenticated employee; cabinet and slot identify storage location" },
  { pattern: /^staff_locker_scan_tokens$/, source: "staff_profile_id", path: "staff_profile_id -> staff_profiles -> employee company/merchant scope; one-time scan token presented to the kiosk (U05-006)" },
  { pattern: /^staff_shift_covers$/, source: "shift_id", path: "shift_id -> staff_shifts -> staff_profiles company/merchant scope; cover_user_id must be in the same scope (U06-004)" },
  { pattern: /^business_process_step_records$/, source: "process_instance_id", path: "process_instance_id -> business_process_instances -> merchant_id/project_id" },
  { pattern: /^bar_order_items$/, source: "order_id", path: "order_id -> bar_orders -> table_id/bar_id -> venue/project scope" },
  { pattern: /^bar_bill/, source: "bill/order linkage", path: "bill/order linkage -> bar_orders -> venue/project scope" },
  { pattern: /^bar_production_ticket/, source: "order_id", path: "order_id -> bar_orders -> venue/project scope" },
  { pattern: /^bar_table_guest_checkins$/, source: "reservation/table linkage", path: "reservation_id/table_id -> bar_reservations/bar_tables -> venue/project scope" },
  { pattern: /^bar_commission_calculations$/, source: "bill_id", path: "bill_id -> bar_bills -> bar_orders -> venue/project scope" },
  { pattern: /^saved_bottle_claims$/, source: "saved_bottle_id", path: "saved_bottle_id -> saved_bottles -> merchant/customer scope" },
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
  { pattern: /^role_viewable_projects$/, source: "role_id", path: "role_id -> roles -> organization scope; viewable_id 是勾选的可查看目标，不是行归属" },
  { pattern: /^role_viewable_project_holds$/, source: "user_id/role_id", path: "user_id/role_id -> access_users/roles -> organization scope; viewable_id 是待重新审批的可查看目标，不是行归属" },
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
  "staff_penalty_cap_revisions",
  "payroll_tax_versions",
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
    // 本批核实读数视图继承原抄表行归属，纳入与基础表相同的隔离检查。
    const effectiveReadingsView = /CREATE VIEW energy_effective_meter_readings AS\s+([\s\S]*?);/i.exec(migration.text);
    if (effectiveReadingsView) definitions.set("energy_effective_meter_readings", effectiveReadingsView[1]);
    const alterPattern = /ALTER TABLE(?: IF EXISTS)?\s+([a-zA-Z0-9_]+)\s+ADD COLUMN[\s\S]*?;/gi;
    while ((match = alterPattern.exec(migration.text)) !== null) {
      const table = match[1];
      definitions.set(table, `${definitions.get(table) ?? ""}\n-- ${migration.name}\n${match[0]}`);
    }
    // 审计 P1-1:识别 DROP TABLE,从最终 schema 排除已删表(迁移已按版本号排序,DROP 在 CREATE 之后生效)。
    const dropPattern = /DROP TABLE(?: IF EXISTS)?\s+([a-zA-Z0-9_]+)/gi;
    while ((match = dropPattern.exec(migration.text)) !== null) {
      definitions.delete(match[1]);
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
  // 第十八批第 2 条：通票池逐份分账的引擎内部表，merchant_id 是受益酒吧而非租户归属，按父表的项目归属读写。
  if (table === "pass_pool_unit_results") return {
    scope_type: "inherited", scope_source: "bill_id",
    scope_path: "bill_id -> settlement_bills -> project_id; merchant_id is the beneficiary bar of each settlement unit, not the row owner",
    multi_complex_note: "must join through scope path before multi-complex rollout",
  };
  if (table === "pass_pool_snapshot_first_segment") return {
    scope_type: "inherited", scope_source: "ticket_instance_id",
    scope_path: "ticket_instance_id -> pass_pool_snapshots -> project_id; merchant_id is the first-segment bar frozen at sale, not the row owner",
    multi_complex_note: "must join through scope path before multi-complex rollout",
  };
  if (table === "entitlement_business_day_activations") return {
    scope_type: "direct", scope_source: "complex_id/entitlement_id",
    scope_path: "actual locked complex_id; entitlement_id -> original issued entitlement and order ownership; first admission and ticket verification must match",
    multi_complex_note: "original business-day window remains immutable; later configuration cannot move the activation or authorize a different complex",
  };
  if (["amusement_sessions", "amusement_verifications", "amusement_concurrent_reset_log", "amusement_session_endings"].includes(table)) return {
    scope_type: "direct", scope_source: "complex_id",
    scope_path: "complex_id is the locked actual projects.id; project_id denotes an amusement ride, not a complex; preserve original merchant scope",
    multi_complex_note: "business-day version and interval locked at the real event; missing ownership is not filled from actor or global defaults",
  };
  if (table === "wecom_cleanup_previews") return {
    scope_type: "global_reference", scope_source: "current master authority",
    scope_path: "platform cleanup evidence; current master identity required; individual preview actions also require creator match",
    multi_complex_note: "permanent exact message scope and execution evidence; never a tenant-readable or generic cleanup table",
  };
  if (table === "staff_business_handovers") return {
    scope_type: "direct", scope_source: "project_id",
    scope_path: "actual source project_id/merchant_id plus staff profile scope; nullable project only for verified platform-global source",
    multi_complex_note: "actor and recipient require original business permissions and scope; unknown source cannot be assigned",
  };
  if (table === "staff_policy_settings") return {
    scope_type: "direct", scope_source: "scope_id",
    scope_path: "shift_visible_days: scope_id = employer business entity; payroll_penalty_cap_bps: scope_id = global",
    multi_complex_note: "employer visibility settings scoped at source; global cap changes require HR, finance, legal and top authority approval",
  };
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
      file_version: "V0.07",
      status: "covered",
    };
  });
  const headers = ["table_name", "scope_type", "scope_source", "scope_path", "multi_complex_note", "file_version", "status"];
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
