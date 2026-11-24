#!/usr/bin/env node
// 文件版本：V0.02
// 门禁：操作留痕（audit_events）写入的业务类型与事件名都要在 backend/src/main/resources/audit/audit-labels.json 里有中文名
// （业务类型在 business_types，事件名在 event_names）。
// 依据：PGM:287「全局原则 · 界面全中文（所有终端零英文）……字段名、状态、枚举取值……必须全部为中文……」；第十八批 AN 段第 227 条
// 227-R2（后台审计事件页操作日志视图按该字典显示业务类型中文名，查不到的显示「其他」）、227-R3（「操作」列与明细按该字典显示事件名
// 中文名，查不到的显示「其他操作」）。
// 来源（backend/src/main/java，AuditEventService 本身除外）：
//   ① xxx.recordSuccess / recordFailure / recordSuccessIndependently(，以及 AuditedOperationExecutor 的 recordingFailure / recordDetail(
//     的第一个参数（事件名）与第三个参数（业务类型）；
//   ② AuditedOperation.of( 的第一、二个参数（成功、失败事件名）与第四个参数（业务类型）；
//   ③ implements AuditedOperation 的枚举常量的第一、二个参数（事件名）与第四个参数（业务类型）；
//   ④ 事件名另收 docs/openapi/openapi-p0.json 各操作的 x-audit-event-success / x-audit-event-failure（not_applicable 除外）；
//   ⑤ 业务类型、操作人类型取自变量、配置组码、请求或路径、跨文件传参（下面的追溯取不到或只取到一部分）的写入点，在 DECLARED 按
//     来源文件:行逐个登记取值，同样要在字典里（第十八批 AN 段收尾复核修正）。
// 参数取值：字面量、同文件 static final String 常量、「类名.常量」直接取；「a ? b : c」两支都取；「x + y」逐段取值后拼出全部组合；
// 是所在方法的形参时，取同文件里调用该方法处对应位置的实参，是同方法里 String 局部变量时取其赋值，再照此解析（最多追三层）。
// 其余跨文件传参、方法返回值（如 d.reviewStatus()）追不到就跳过（业务类型的这类写入点登记在 ⑤）。另核：字典每个中文名不得含英文字母。
// ponytail: 正则扫描、不是完整 Java 解析；追不到的业务类型与事件名在页面显示「其他」「其他操作」，需要时再加追踪规则。
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const JAVA = path.join(ROOT, "backend/src/main/java");
const DICT = path.join(ROOT, "backend/src/main/resources/audit/audit-labels.json");
const OPENAPI = path.join(ROOT, "docs/openapi/openapi-p0.json");

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p, out);
    else if (p.endsWith(".java")) out.push(p);
  }
  return out;
}

// 从 open（左括号下标）起按顶层逗号切实参，返回实参与右括号下标；字符串里的括号逗号不算，尖括号只在 angle 为真时（形参列表里的泛型）计层。
function splitArgs(text, open, angle = false) {
  const args = [];
  let depth = 0;
  let cur = "";
  for (let i = open; i < text.length; i += 1) {
    const c = text[i];
    if (c === '"') {
      let k = i + 1;
      while (k < text.length && text[k] !== '"') k += text[k] === "\\" ? 2 : 1;
      cur += text.slice(i, k + 1);
      i = k;
      continue;
    }
    if ("([{".includes(c) || (angle && c === "<")) {
      depth += 1;
      if (depth === 1) continue;
    }
    if (")]}".includes(c) || (angle && c === ">" && text[i - 1] !== "-")) {
      depth -= 1;
      if (depth === 0) {
        args.push(cur.trim());
        return { args, end: i };
      }
    }
    if (c === "," && depth === 1) {
      args.push(cur.trim());
      cur = "";
    } else cur += c;
  }
  return { args, end: text.length };
}

// 表达式顶层（括号与字符串外）按单个字符切。
function splitTop(expr, sep) {
  const parts = [];
  let depth = 0;
  let cur = "";
  for (let i = 0; i < expr.length; i += 1) {
    const c = expr[i];
    if (c === '"') {
      let k = i + 1;
      while (k < expr.length && expr[k] !== '"') k += expr[k] === "\\" ? 2 : 1;
      cur += expr.slice(i, k + 1);
      i = k;
      continue;
    }
    if ("([{".includes(c)) depth += 1;
    if (")]}".includes(c)) depth -= 1;
    if (c === sep && depth === 0) {
      parts.push(cur.trim());
      cur = "";
    } else cur += c;
  }
  parts.push(cur.trim());
  return parts;
}

// 去注释（字符串里的 // 不算）。
function stripComments(s) {
  let out = "";
  for (let i = 0; i < s.length; i += 1) {
    if (s[i] === '"') {
      let k = i + 1;
      while (k < s.length && s[k] !== '"') k += s[k] === "\\" ? 2 : 1;
      out += s.slice(i, k + 1);
      i = k;
    } else if (s.startsWith("//", i)) {
      while (i < s.length && s[i] !== "\n") i += 1;
    } else if (s.startsWith("/*", i)) {
      const close = s.indexOf("*/", i + 2);
      i = close < 0 ? s.length : close + 1;
    } else out += s[i];
  }
  return out.trim();
}
function unwrap(s) {
  let t = s;
  while (t.startsWith("(") && splitArgs(t, 0).end === t.length - 1) t = t.slice(1, -1).trim();
  return t;
}

const CONST = /static\s+final\s+String\s+(\w+)\s*=\s*"([^"]*)"\s*;/g;
const KEYWORDS = new Set(["if", "for", "while", "switch", "catch", "synchronized", "try", "return", "new", "else", "do", "throw", "case", "yield"]);
const files = walk(JAVA).map((file) => ({ file, rel: path.relative(ROOT, file).replace(/\\/g, "/"), text: fs.readFileSync(file, "utf8") }));
const classConsts = new Map(files.map((f) => [path.basename(f.file, ".java"), new Map([...f.text.matchAll(CONST)].map((m) => [m[1], m[2]]))]));

const found = { business: new Map(), event: new Map(), actor: new Map() };
const note = (kind, code, where) => { if (!found[kind].has(code)) found[kind].set(code, where); };

for (const { file, rel, text } of files) {
  if (rel.endsWith("evidence/AuditEventService.java")) continue;
  const consts = classConsts.get(path.basename(file, ".java"));
  // 方法声明：「返回类型 名字(形参) {」或「… throws … {」；名字前须是类型（词、> 或 ]），不是 return、new 等。
  const decls = [];
  for (const m of text.matchAll(/(?<![\w$])(\w+)\s*\(/g)) {
    const before = /(\S+)\s*$/.exec(text.slice(Math.max(0, m.index - 60), m.index))?.[1] ?? "";
    if (KEYWORDS.has(m[1]) || KEYWORDS.has(before) || !/[\w>\]]$/.test(before)) continue;
    const { args, end } = splitArgs(text, m.index + m[0].length - 1, true);
    if (!/^\s*(throws\s[\w.,\s]+)?\{/.test(text.slice(end + 1, end + 200))) continue;
    decls.push({ index: m.index, name: m[1], params: args.filter(Boolean).map((p) => p.split(/\s+/).at(-1)) });
  }
  const declAt = new Set(decls.map((d) => d.index));
  // 表达式的全部可能取值；一个也取不到返回 null。
  const values = (raw, at, depth = 0) => {
    const expr = unwrap(stripComments(raw));
    const question = splitTop(expr, "?");
    if (question.length === 2) {
      const branches = splitTop(question[1], ":");
      if (branches.length === 2) {
        const both = [values(branches[0], at, depth), values(branches[1], at, depth)].filter(Boolean).flat();
        return both.length ? both : null;
      }
    }
    const plus = splitTop(expr, "+");
    if (plus.length > 1) {
      let acc = [""];
      for (const part of plus) {
        const v = values(part, at, depth);
        if (!v) return null;
        acc = acc.flatMap((a) => v.map((b) => a + b));
      }
      return acc;
    }
    const literal = /^"([^"]*)"$/.exec(expr);
    if (literal) return [literal[1]];
    if (consts.has(expr)) return [consts.get(expr)];
    const qualified = /^(\w+)\.(\w+)$/.exec(expr);
    if (qualified && classConsts.get(qualified[1])?.has(qualified[2])) return [classConsts.get(qualified[1]).get(qualified[2])];
    if (!/^\w+$/.test(expr) || depth > 2) return null;
    const decl = decls.filter((d) => d.index < at).at(-1);
    if (!decl) return null;
    const out = [];
    const local = [...text.slice(decl.index, at).matchAll(new RegExp(`\\bString\\s+${expr}\\s*=\\s*([^;]+);`, "g"))].at(-1);
    if (local) out.push(...(values(local[1], decl.index + local.index, depth + 1) ?? []));
    const k = decl.params.indexOf(expr);
    if (k >= 0) {
      for (const call of text.matchAll(new RegExp(`(?<![\\w.:$])(?:this\\.)?${decl.name}\\s*\\(`, "g"))) {
        if (declAt.has(call.index)) continue;
        const { args } = splitArgs(text, call.index + call[0].length - 1);
        if (args.length === decl.params.length) out.push(...(values(args[k], call.index, depth + 1) ?? []));
      }
    }
    return out.length ? out : null;
  };
  const take = (kind, args, idx, at) => {
    if (args.length <= idx) return;
    const line = text.slice(0, at).split("\n").length;
    for (const value of values(args[idx], at) ?? []) note(kind, value, `${rel}:${line}`);
  };
  for (const m of text.matchAll(/(?<![\w$])(\w+)\s*\.\s*record(?:Success|Failure|SuccessIndependently|ingFailure|Detail)\s*\(/g)) {
    if (m[1] === "this") continue;
    const { args } = splitArgs(text, m.index + m[0].length - 1);
    take("event", args, 0, m.index);
    take("business", args, 2, m.index);
  }
  const operation = (args, at) => {
    take("event", args, 0, at);
    take("event", args, 1, at);
    take("business", args, 3, at);
  };
  for (const m of text.matchAll(/AuditedOperation\.of\s*\(/g)) operation(splitArgs(text, m.index + m[0].length - 1).args, m.index);
  if (/implements\s+AuditedOperation\b/.test(text)) {
    for (const m of text.matchAll(/^\s*[A-Z][A-Z0-9_]*\s*\(/gm)) operation(splitArgs(text, m.index + m[0].length - 1).args, m.index);
  }
}

// ⑤ 追不到的写入点：[类别, 来源文件:行（取值从哪来）, 取值]。
const J = "backend/src/main/java/com/complex/platform/";
const DECLARED = [
  ["business", `${J}settlement/SettlementConfigWriteService.java:575（activateDraft 默认分支 auditActivated(type)，type 为页面传来的单行配置组码）`,
    ["distribution_method", "merchant_rate", "comprehensive_service_fee", "combo_discount", "channel_fee", "commission_rate", "points_accrual_rate",
      "turnover_rent_rule", "pass_gift_cost"]],
  ["business", `${J}settlement/SettlementConfigWriteService.java:648（archive 下线留痕，table 为 :616 不带初值的局部变量，取下线配置的表名）`,
    ["ticket_distribution_merchant_rates", "revenue_stream_attribution", "bar_commission_rate_configs", "points_accrual_rate_configs",
      "turnover_rent_rules", "pass_gift_cost_configs", "internal_price_catalog"]],
  ["business", `${J}settlement/SettlementConfigController.java:327、:340（转正、下线失败留痕，业务类型为路径上的配置组码，即待转正队列各组）`,
    ["distribution_method", "merchant_rate", "comprehensive_service_fee", "combo_discount", "channel_fee", "commission_rate", "points_accrual_rate",
      "high_value_threshold", "revenue_stream_attribution", "attribution_rule", "pass_gift_cost", "turnover_rent_rule", "internal_price_item"]],
  ["business", `${J}access/staff/StaffHandoverService.java:145（业务交接留痕，业务类型为 StaffHandoverRepository.Kind 的 name()）`,
    ["approval_node", "facility_work_order", "facility_inspection", "merchant_work_order", "service_order", "leasing_lead", "integration_difference",
      "bar_table_session", "risk_alert"]],
  ["business", `${J}inventory/PurchaseWorkflowService.java:806（write 留痕，业务类型由 PurchasePrepaymentService.java:35、:82 跨文件传入）`,
    ["inventory_purchase_request", "purchase_prepayment_resolution"]],
  ["business", `${J}evidence/EvidenceEventService.java:143（附件上传失败留痕，业务类型取请求 business_type：后台、经销商门户、安卓员工端各上传框所传，`
    + "DealerPortalService、SiteEntryReportController 转调）",
    ["amusement_business_day_config", "amusement_safety_inspection", "amusement_session", "bar_bill", "bar_business_day_config",
      "bar_order_bad_debt", "bar_order_item_void", "bar_payroll_rule", "dealer_admission", "dealer_delivery", "dealer_invoice",
      "dealer_overpayment", "dealer_return", "energy_meter",
      "facility_equipment_scrap", "facility_site_entry_report", "finance_locked_backfill", "finance_reconciliation_difference", "finance_refund",
      "inventory_purchase_request", "inventory_quarantine", "inventory_transfer_order", "manual_backfill", "payroll_tax_assessment",
      "payroll_tax_version", "purchase_prepayment_resolution", "purchase_return", "saved_bottle_registration", "staff_payroll_calendar",
      "staff_payroll_payment", "staff_payroll_period", "staff_payroll_scheme", "staff_performance_rule", "supply_contract",
      "ticket_sales_contract"]],
  ["business", `${J}evidence/EvidenceEventService.java:452、:463（证据时间线查看留痕，业务类型取路径 object_type：工资依据、工资付款、个税页所传）`,
    ["staff_payroll_scheme", "staff_payroll_calendar", "staff_payroll_period", "staff_performance_rule", "staff_payroll_payment",
      "payroll_tax_version", "payroll_tax_assessment"]],
  ["business", `${J}integration/IntegrationService.java:548、:552（开放平台读取留痕，业务类型由 OpenPlatformController.java:39、:52 跨文件传入）`,
    ["parking_config", "parking_payment"]],
  ["business", `${J}facility/WorkOrderMaterialService.java:264（承担方争议判定留痕，业务类型取争议行，即两类工单的 BUSINESS_TYPE）`,
    ["facility_work_order", "merchant_service_work_order"]],
  ["business", `${J}access/staff/StaffPayrollConfigurationService.java:432（success 留痕，业务类型为同行声明的 SCHEME、CALENDAR、PERIOD，`
    + ":304 审批对象类型，StaffPayrollService.java:82、:114 跨文件传入）",
    ["staff_payroll_scheme", "staff_payroll_calendar", "staff_payroll_period", "staff_performance_rule", "staff_performance_score",
      "staff_payroll_calculation", "staff_payroll"]],
  ["business", `${J}access/staff/PayrollTaxService.java:469、:470（审批结果留痕，:236、:237 业务类型为审批对象类型 VERSION、ASSESSMENT）`,
    ["payroll_tax_version", "payroll_tax_assessment"]],
  ["business", `${J}bar/BarPayrollRuleService.java:188（业务类型为同行声明的 OBJECT 常量）`, ["bar_payroll_rule"]],
  ["actor", `${J}evidence/AuditEventService.java（record、anonymousRequestActorType 与 kioskDeviceId：没有认证信息的非请求线程记 system；未登录请求按路径记，`
    + "/api/v1/terminals/kiosks/ 柜机上报记 device、/api/open/ 记 third_party、/api/v1/customer/ 记 customer、其余记 anonymous；第十八批 CH 段第 412 条 412-R1）",
    ["system", "device", "third_party", "customer", "anonymous"]],
  ["actor", `${J}auth/ProductionAuthService.java:413（最高权限账号 account_type＝SessionIssuer.MASTER_ACCOUNT_TYPE，`
    + "evidence/AuditEventService.java:129 按 actor.accountType() 记 actor_type）", ["master_authority"]],
];
for (const [kind, where, codes] of DECLARED) for (const code of codes) note(kind, code, where);

const spec = JSON.parse(fs.readFileSync(OPENAPI, "utf8"));
for (const [route, item] of Object.entries(spec.paths ?? {})) {
  for (const [method, op] of Object.entries(item)) {
    for (const key of ["x-audit-event-success", "x-audit-event-failure"]) {
      const event = op?.[key];
      if (typeof event === "string" && event && event !== "not_applicable") note("event", event, `openapi ${method.toUpperCase()} ${route}`);
    }
  }
}

const dict = JSON.parse(fs.readFileSync(DICT, "utf8"));
const businessTypes = dict.business_types ?? {};
const eventNames = dict.event_names ?? {};
const missing = (kind, section) => [...found[kind]].filter(([code]) => !(code in section)).map(([code, where]) => `${code}（${where}）`);
const missingBusiness = missing("business", businessTypes);
const missingEvents = missing("event", eventNames);
const missingActors = missing("actor", dict.actor_types ?? {});
const badNames = Object.entries(dict).flatMap(([section, entries]) =>
  Object.entries(entries ?? {}).filter(([, name]) => typeof name !== "string" || !name || /[A-Za-z]/.test(name)).map(([code, name]) => `${section}.${code}：「${name}」`));

console.log(`[audit-labels] 代码与契约里的留痕业务类型 ${found.business.size} 个、事件名 ${found.event.size} 个；字典业务类型 ${Object.keys(businessTypes).length}、操作人类型 ${Object.keys(dict.actor_types ?? {}).length}、事件名 ${Object.keys(eventNames).length}、权限表外权限 ${Object.keys(dict.extra_permissions ?? {}).length}`);
const report = (title, list) => { if (list.length) console.log(`${title} ${list.length} 条：\n  ${list.join("\n  ")}`); };
report("字典缺业务类型", missingBusiness);
report("字典缺事件名", missingEvents);
report("字典缺操作人类型", missingActors);
report("中文名为空或含英文字母", badNames);
const red = missingBusiness.length + missingEvents.length + missingActors.length + badNames.length;
console.log(red ? `[audit-labels] 红：${red} 处` : "[audit-labels] 绿：0 处缺失");
process.exit(red ? 1 : 0);
