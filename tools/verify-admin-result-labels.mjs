#!/usr/bin/env node
// 文件版本：V0.01
// 门禁：后台结果区（AdminApp「admin-action-result」）展示的键名与取值必须都有中文名。
// 依据：PGM「全局原则 · 界面全中文（所有终端零英文）」条；OBS-14-02（结果区曾把结果对象原样 JSON、英文键名上屏）。
// 结果区由 admin/components/ActionResultView 按 admin/i18n/fieldLabels.ts 翻译；本门禁扫全仓实际会出现的键与取值，
// 字典里缺一个就红，防止以后新增英文键名、动作码、枚举取值漏翻：
//   ① 前端：apps/web/src 非测试文件里 setActionResult(...) / makeResult 返回 / runAction 请求体里的对象字面量键，
//      以及带字面量动作码（action: "xxx_saved"）的对象字面量的键（同文件内 const 变量与展开会顺着找）；
//   ② 后端：Controller 方法返回 ApiResponse<...> 里出现的 record 及其嵌套 record 的全部组件名（JSON 蛇形）；
//      Controller 文件里、以及 main 下全部 Java 文件中返回 Map<String, Object>（含其列表）的方法里 Map.of / Map.entry / put 的字面量键；
//      queryForList / queryForMap 直接 SELECT * / RETURNING * 整行返回的表，取迁移里该表的全部列名；
//   ③ 取值：上述结果对象里每个键的字面量取值（字面量、三元、?? / || 两支、同作用域 const 与 useState 初值、对象属性、
//      函数参数的字面量联合类型），以及页面上绑定到同名键的下拉（select / Select / Radio.Group 的 option 与 options）取值，
//      要在该键的按键取值表（KEY_VALUE_LABELS）、VALUE_LABELS 或 STATUS_LABELS 里；动作码（action）同理；
//      模板字符串动作码的插值能追到字面量或联合类型的逐一展开核对，追不到的退回「字典里至少一个能匹配」；
//      后端迁移里 status / *_status 列 CHECK (… IN (…)) 闭集的每个取值，要在 STATUS_LABELS 或 VALUE_LABELS 里；
//   ④ 口径：字典与后端 common/api/FieldLabels.java 同键的中文名必须一致。
// ponytail: 后端只认上面三类来源；返回类型不是 Map 的方法里拼的 Map（如审计载荷）、非 status 列的枚举取值扫不到，
//           运行时兜底显示「其他信息」或原码，新增时按 PRD 口径补字典。
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ts = createRequire(path.join(ROOT, "package.json"))("typescript");
const DICT = path.join(ROOT, "apps/web/src/admin/i18n/fieldLabels.ts");
const STATUS = path.join(ROOT, "packages/customer-core/src/statusLabels.ts");
const BACKEND_LABELS = path.join(ROOT, "backend/src/main/java/com/complex/platform/common/api/FieldLabels.java");
const MIGRATIONS = path.join(ROOT, "backend/src/main/resources/db/migration");

const compact = (key) => key.replace(/_/g, "").toLowerCase();
const CJK = /[一-鿿]/;
const CODE = /^[a-z][a-z0-9]*(?:[_.-][a-z0-9]+)+$/;
const VALUE = /^[a-z][a-z0-9_]*$/;
const ID_KEY = /(?:Id|_id|Ids|_ids|Code|_code)$/;
const SELECT_TAG = /^(select|Select|Radio\.Group|Segmented|Checkbox\.Group)$/;
const OPTION_TAG = /^(option|Option|Select\.Option|Radio|Radio\.Button)$/;

function walk(dir, test, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p, test, out);
    else if (test(p)) out.push(p);
  }
  return out;
}

function parseTs(file) {
  const text = fs.readFileSync(file, "utf8");
  return ts.createSourceFile(file, text, ts.ScriptTarget.Latest, true, file.endsWith("x") ? ts.ScriptKind.TSX : ts.ScriptKind.TS);
}

const propName = (p) => (p.name && (ts.isIdentifier(p.name) || ts.isStringLiteral(p.name)) ? p.name.text : null);

// 读 TS 文件里 `const NAME ... = { ... }` 对象字面量的键与字符串值（nested 时值为内层对象的键集）。
function objectLiteral(file, name, nested = false) {
  const map = new Map();
  const visit = (node) => {
    if (ts.isVariableDeclaration(node) && ts.isIdentifier(node.name) && node.name.text === name && node.initializer && ts.isObjectLiteralExpression(node.initializer)) {
      for (const p of node.initializer.properties) {
        if (!ts.isPropertyAssignment(p) || !propName(p)) continue;
        if (nested) map.set(compact(propName(p)), new Set(ts.isObjectLiteralExpression(p.initializer) ? p.initializer.properties.map(propName).filter(Boolean) : []));
        else map.set(propName(p), ts.isStringLiteral(p.initializer) ? p.initializer.text : "");
      }
    }
    ts.forEachChild(node, visit);
  };
  visit(parseTs(file));
  return map;
}

const unwrap = (e) => {
  while (e && (ts.isParenthesizedExpression(e) || ts.isAsExpression(e) || ts.isNonNullExpression(e) || ts.isAwaitExpression(e) || ts.isSatisfiesExpression(e))) e = e.expression;
  return e;
};
const callName = (call) => {
  const c = call.expression;
  return ts.isIdentifier(c) ? c.text : ts.isPropertyAccessExpression(c) ? c.name.text : "";
};
const isUseState = (e) => { const c = unwrap(e); return c && ts.isCallExpression(c) && callName(c) === "useState"; };
// 取值表达式的「末端名」：dutyForm.dutyRole → dutyRole；fundOwnership → fundOwnership。
const terminal = (expr) => {
  const e = unwrap(expr);
  if (!e) return null;
  if (ts.isIdentifier(e)) return e.text;
  if (ts.isPropertyAccessExpression(e)) return e.name.text;
  if (ts.isElementAccessExpression(e) && ts.isStringLiteral(e.argumentExpression)) return e.argumentExpression.text;
  return null;
};

function scanFrontend() {
  const keys = new Map();
  const values = new Map();
  const templates = new Map();
  const note = (map, k, file) => { if (!map.has(k)) map.set(k, path.relative(ROOT, file).replace(/\\/g, "/")); };
  const files = walk(path.join(ROOT, "apps/web/src"), (p) => /\.tsx?$/.test(p) && !/\.test\.tsx?$/.test(p));
  for (const file of files) {
    if (!/setActionResult|makeResult|runAction/.test(fs.readFileSync(file, "utf8"))) continue;
    const sf = parseTs(file);
    const decls = new Map();
    const aliases = new Map();
    const collectDecls = (n) => {
      if (ts.isVariableDeclaration(n) && ts.isIdentifier(n.name) && n.initializer) {
        if (!decls.has(n.name.text)) decls.set(n.name.text, []);
        decls.get(n.name.text).push(n.initializer);
      }
      if (ts.isTypeAliasDeclaration(n)) aliases.set(n.name.text, n.type);
      ts.forEachChild(n, collectDecls);
    };
    collectDecls(sf);

    // 标识符按词法作用域就近找声明：外层函数的参数（取其字面量联合类型）、所在块里的 const 与 useState 初值；找不到再退回同文件同名声明。
    const lookup = (id) => {
      const name = id.text;
      for (let n = id.parent; n; n = n.parent) {
        if (ts.isFunctionLike(n)) {
          for (const p of n.parameters) if (ts.isIdentifier(p.name) && p.name.text === name) return { types: p.type ? [p.type] : [], inits: p.initializer ? [p.initializer] : [] };
        }
        const stmts = ts.isBlock(n) || ts.isSourceFile(n) || ts.isCaseClause(n) || ts.isDefaultClause(n) ? n.statements : null;
        if (!stmts) continue;
        const inits = [];
        for (const s of stmts) {
          if (!ts.isVariableStatement(s)) continue;
          for (const d of s.declarationList.declarations) {
            if (!d.initializer) continue;
            if (ts.isIdentifier(d.name) && d.name.text === name) inits.push(d.initializer);
            const first = ts.isArrayBindingPattern(d.name) ? d.name.elements[0] : null;
            if (first && ts.isBindingElement(first) && ts.isIdentifier(first.name) && first.name.text === name && isUseState(d.initializer) && unwrap(d.initializer).arguments[0]) inits.push(unwrap(d.initializer).arguments[0]);
          }
        }
        if (inits.length) return { types: [], inits };
      }
      return { types: [], inits: decls.get(name) ?? [] };
    };
    const typeLiterals = (t, out, depth = 0) => {
      if (!t || depth > 5) return out;
      if (ts.isUnionTypeNode(t)) t.types.forEach((x) => typeLiterals(x, out, depth + 1));
      else if (ts.isParenthesizedTypeNode(t)) typeLiterals(t.type, out, depth + 1);
      else if (ts.isLiteralTypeNode(t) && ts.isStringLiteral(t.literal)) out.add(t.literal.text);
      else if (ts.isTypeReferenceNode(t) && ts.isIdentifier(t.typeName) && aliases.has(t.typeName.text)) typeLiterals(aliases.get(t.typeName.text), out, depth + 1);
      return out;
    };
    const prop = (obj, name) => {
      for (const p of obj.properties) {
        if (ts.isPropertyAssignment(p) && propName(p) === name) return p.initializer;
        if (ts.isShorthandPropertyAssignment(p) && p.name.text === name) return p.name;
      }
      return null;
    };
    const objects = (expr, depth = 0) => {
      const e = unwrap(expr);
      if (!e || depth > 6) return [];
      if (ts.isObjectLiteralExpression(e)) return [e];
      if (ts.isIdentifier(e)) return lookup(e).inits.flatMap((init) => objects(init, depth + 1));
      if (ts.isPropertyAccessExpression(e)) return objects(e.expression, depth + 1).flatMap((o) => { const v = prop(o, e.name.text); return v ? objects(v, depth + 1) : []; });
      if (ts.isConditionalExpression(e)) return [...objects(e.whenTrue, depth + 1), ...objects(e.whenFalse, depth + 1)];
      return [];
    };
    // 模板字符串逐一展开：每个插值都追得到字面量才展开；追不到的交给 sink（动作码退回「至少一个匹配」）。
    const expand = (tpl, depth) => {
      let acc = [tpl.head.text];
      for (const span of tpl.templateSpans) {
        const lits = [...literals(span.expression, new Set(), depth + 1)];
        if (!lits.length) return null;
        acc = acc.flatMap((a) => lits.map((l) => a + l + span.literal.text));
      }
      return acc;
    };
    const literals = (expr, out = new Set(), depth = 0, sink = null) => {
      const e = unwrap(expr);
      if (!e || depth > 6) return out;
      if (ts.isStringLiteral(e) || ts.isNoSubstitutionTemplateLiteral(e)) out.add(e.text);
      else if (ts.isTemplateExpression(e)) { const codes = expand(e, depth); if (codes) codes.forEach((c) => out.add(c)); else sink?.(e); }
      else if (ts.isConditionalExpression(e)) { literals(e.whenTrue, out, depth + 1, sink); literals(e.whenFalse, out, depth + 1, sink); }
      else if (ts.isBinaryExpression(e) && [ts.SyntaxKind.QuestionQuestionToken, ts.SyntaxKind.BarBarToken].includes(e.operatorToken.kind)) { literals(e.left, out, depth + 1, sink); literals(e.right, out, depth + 1, sink); }
      else if (ts.isIdentifier(e)) { const found = lookup(e); found.inits.forEach((init) => literals(init, out, depth + 1, sink)); found.types.forEach((t) => typeLiterals(t, out)); }
      else if (ts.isPropertyAccessExpression(e)) for (const o of objects(e.expression, depth + 1)) { const v = prop(o, e.name.text); if (v) literals(v, out, depth + 1, sink); }
      return out;
    };
    // 下拉选项取值：数组字面量（字符串、二元组首项、对象 value）、同名常量、ARR.map(...)、Object.entries/keys(对象)。
    const optionValues = (expr, out = new Set(), depth = 0) => {
      const e = unwrap(expr);
      if (!e || depth > 5) return out;
      if (ts.isArrayLiteralExpression(e)) {
        for (const el of e.elements) {
          const x = unwrap(el);
          if (ts.isStringLiteral(x)) out.add(x.text);
          else if (ts.isArrayLiteralExpression(x) && x.elements[0]) literals(x.elements[0], out);
          else if (ts.isObjectLiteralExpression(x)) { const v = prop(x, "value") ?? prop(x, "v") ?? prop(x, "code"); if (v) literals(v, out); }
        }
      } else if (ts.isIdentifier(e)) lookup(e).inits.forEach((init) => optionValues(init, out, depth + 1));
      else if (ts.isCallExpression(e) && ts.isPropertyAccessExpression(e.expression)) {
        const method = e.expression.name.text;
        const target = unwrap(e.expression.expression);
        if (["map", "filter", "slice", "concat"].includes(method)) optionValues(target, out, depth + 1);
        if (["entries", "keys"].includes(method) && ts.isIdentifier(target) && target.text === "Object") {
          for (const o of objects(e.arguments[0])) for (const p of o.properties) if (propName(p)) out.add(propName(p));
        }
      }
      return out;
    };
    // 绑定到某键的下拉：value / defaultValue 的末端名 → 选项取值。
    const bound = new Map();
    const scanSelects = (n) => {
      if (ts.isJsxElement(n) || ts.isJsxSelfClosingElement(n)) {
        const open = ts.isJsxElement(n) ? n.openingElement : n;
        if (SELECT_TAG.test(open.tagName.getText())) {
          const names = [];
          const opts = new Set();
          for (const a of open.attributes.properties) {
            if (!ts.isJsxAttribute(a) || !a.initializer || !ts.isJsxExpression(a.initializer) || !a.initializer.expression) continue;
            const attr = a.name.getText();
            if (attr === "value" || attr === "defaultValue") { const t = terminal(a.initializer.expression); if (t) names.push(t); }
            if (attr === "options") optionValues(a.initializer.expression, opts);
          }
          if (ts.isJsxElement(n)) {
            for (const child of n.children) {
              if (ts.isJsxExpression(child) && child.expression) optionValues(child.expression, opts);
              const el = ts.isJsxElement(child) ? child.openingElement : ts.isJsxSelfClosingElement(child) ? child : null;
              if (el && OPTION_TAG.test(el.tagName.getText())) {
                for (const a of el.attributes.properties) {
                  if (!ts.isJsxAttribute(a) || a.name.getText() !== "value" || !a.initializer) continue;
                  if (ts.isStringLiteral(a.initializer)) opts.add(a.initializer.text);
                  else if (ts.isJsxExpression(a.initializer) && a.initializer.expression) literals(a.initializer.expression, opts);
                }
              }
            }
          }
          for (const name of names) { if (!bound.has(name)) bound.set(name, new Set()); opts.forEach((v) => bound.get(name).add(v)); }
        }
      }
      ts.forEachChild(n, scanSelects);
    };
    scanSelects(sf);

    const pairs = [];
    let seen = new Set();
    const collect = (expr, depth = 0) => {
      const e = unwrap(expr);
      if (!e || depth > 6) return;
      if (ts.isObjectLiteralExpression(e)) {
        for (const p of e.properties) {
          if (ts.isPropertyAssignment(p) || ts.isShorthandPropertyAssignment(p)) {
            const name = propName(p);
            const value = ts.isPropertyAssignment(p) ? p.initializer : p.name;
            if (name && !CJK.test(name)) { note(keys, name, file); pairs.push([name, value]); }
            collect(value, depth + 1);
          } else if (ts.isSpreadAssignment(p)) collect(p.expression, depth + 1);
        }
      } else if (ts.isArrayLiteralExpression(e)) e.elements.forEach((x) => collect(x, depth + 1));
      else if (ts.isConditionalExpression(e)) { collect(e.whenTrue, depth + 1); collect(e.whenFalse, depth + 1); }
      else if (ts.isBinaryExpression(e)) { collect(e.left, depth + 1); collect(e.right, depth + 1); }
      else if (ts.isIdentifier(e)) {
        if (seen.has(e.text)) return;
        seen.add(e.text);
        for (const init of decls.get(e.text) ?? []) collect(init, depth + 1);
      } else if (ts.isCallExpression(e) && callName(e) === "makeResult") { pairs.push(["action", e.arguments[0]]); collect(e.arguments[1], depth + 1); }
    };
    // 页面自带的动作助手（如 runOrderAction(action, ...)）：参数一路传进 setActionResult 的 action，调用处的实参才是动作码。
    const forwarders = new Map([["runAction", 0], ["makeResult", 0]]);
    const findForwarders = (n) => {
      const fn = ts.isFunctionDeclaration(n) ? n : ts.isVariableDeclaration(n) && n.initializer && (ts.isArrowFunction(n.initializer) || ts.isFunctionExpression(n.initializer)) ? n.initializer : null;
      const fnName = ts.isFunctionDeclaration(n) ? n.name?.text : ts.isVariableDeclaration(n) && ts.isIdentifier(n.name) ? n.name.text : undefined;
      if (fn?.body && fnName) {
        const params = fn.parameters.map((p) => (ts.isIdentifier(p.name) ? p.name.text : ""));
        const scan = (m) => {
          if (ts.isCallExpression(m)) {
            const callee = callName(m);
            const first = unwrap(m.arguments[0]);
            let arg;
            if (callee === "setActionResult" && first && ts.isObjectLiteralExpression(first)) {
              for (const p of first.properties) if ((ts.isPropertyAssignment(p) || ts.isShorthandPropertyAssignment(p)) && p.name.getText() === "action") arg = ts.isPropertyAssignment(p) ? unwrap(p.initializer) : p.name;
            } else if (forwarders.has(callee) && callee !== fnName) arg = unwrap(m.arguments[forwarders.get(callee)]);
            if (arg && ts.isIdentifier(arg) && params.includes(arg.text)) forwarders.set(fnName, params.indexOf(arg.text));
          }
          ts.forEachChild(m, scan);
        };
        scan(fn.body);
      }
      ts.forEachChild(n, findForwarders);
    };
    // 助手套助手时多扫几遍，直到不再新增。
    for (let size = -1; size !== forwarders.size;) { size = forwarders.size; findForwarders(sf); }
    const visit = (n) => {
      if (ts.isCallExpression(n)) {
        const name = callName(n);
        if (name === "setActionResult") { seen = new Set(); collect(n.arguments[0]); }
        if (forwarders.has(name)) pairs.push(["action", n.arguments[forwarders.get(name)]]);
        if (name === "runAction" || name === "makeResult") { seen = new Set(); collect(n.arguments[1]); }
      }
      // 先拼好再交给 setActionResult 的结果对象（如回调 return { action: "xxx_saved", ... }）：带字面量动作码的对象一律按结果对象扫。
      if (ts.isObjectLiteralExpression(n) && n.properties.some((p) => ts.isPropertyAssignment(p) && p.name.getText() === "action" && ts.isStringLiteral(p.initializer) && CODE.test(p.initializer.text))) { seen = new Set(); collect(n); }
      if (ts.isFunctionDeclaration(n) && n.name?.text === "makeResult" && n.body) {
        const ret = (r) => { if (ts.isReturnStatement(r) && r.expression) { seen = new Set(); collect(r.expression); } ts.forEachChild(r, ret); };
        ret(n.body);
      }
      ts.forEachChild(n, visit);
    };
    visit(sf);

    for (const [key, expr] of pairs) {
      const e = unwrap(expr);
      // 编号、编码类键（*Id / *_id / *Code）的取值是单据号或系统内部标识符（PGM 同条允许不译的四样之一），不核。
      if (!e || ID_KEY.test(key)) continue;
      const isAction = key === "action";
      const found = literals(e, new Set(), 0, isAction ? (tpl) => note(templates, tpl.getText(), file) : null);
      if (!isAction) for (const name of new Set([key, terminal(e)])) bound.get(name)?.forEach((v) => found.add(v));
      for (const v of found) if ((isAction ? CODE : VALUE).test(v)) note(values, `${isAction ? "action" : key}\u0000${v}`, file);
    }
  }
  return { keys, values, templates };
}

// Java 去注释（保留字符串与文本块原样，免得把 "http://" 当注释）。
const stripJava = (s) => s.replace(/("""[\s\S]*?""")|("(?:\\.|[^"\\\n])*")|\/\*[\s\S]*?\*\/|\/\/[^\n]*/g, (m, block, str) => block ?? str ?? "");

// 按顶层逗号切分；Java 泛型要把尖括号算作括号，SQL 不能（CHECK(x>0) 里的 > 是比较号）。
function splitTop(body, opens = "(<[{", closes = ")>]}") {
  const parts = [];
  let depth = 0;
  let cur = "";
  for (const c of body) {
    if (opens.includes(c)) depth++;
    if (closes.includes(c)) depth--;
    if (c === "," && depth === 0) { parts.push(cur); cur = ""; } else cur += c;
  }
  if (cur.trim()) parts.push(cur);
  return parts;
}

function balanced(s, start, open, close) {
  let depth = 1;
  let i = start;
  while (i < s.length && depth > 0) {
    if (s[i] === open) depth++;
    else if (s[i] === close) depth--;
    i++;
  }
  return s.slice(start, i - 1);
}

const mapKeys = (s) => {
  const out = [...s.matchAll(/(?:Map\.entry|\.put|\.putIfAbsent)\(\s*"([a-z][A-Za-z0-9_]*)"/g)].map((m) => m[1]);
  for (const m of s.matchAll(/Map\.of\(/g)) {
    const args = splitTop(balanced(s, m.index + m[0].length, "(", ")")).map((a) => a.trim());
    for (let i = 0; i < args.length; i += 2) { const k = /^"([a-z][A-Za-z0-9_]*)"$/.exec(args[i]); if (k) out.push(k[1]); }
  }
  return out;
};

// 迁移里各表的列（CREATE TABLE 列定义 + ALTER TABLE ADD/DROP/RENAME COLUMN，按迁移号顺序）与 status 类列的 CHECK 闭集取值。
function scanMigrations() {
  const columns = new Map();
  const statusValues = new Map();
  const NOT_COLUMN = /^(constraint|primary|unique|check|foreign|exclude|like)\b/i;
  const files = fs.readdirSync(MIGRATIONS).filter((f) => /^V\d+__.*\.sql$/.test(f)).sort((a, b) => Number(/^V(\d+)/.exec(a)[1]) - Number(/^V(\d+)/.exec(b)[1]));
  for (const f of files) {
    const s = fs.readFileSync(path.join(MIGRATIONS, f), "utf8").replace(/--[^\n]*/g, "");
    for (const m of s.matchAll(/create\s+table\s+(?:if\s+not\s+exists\s+)?(?:public\.)?([a-z_][a-z0-9_]*)\s*\(/gi)) {
      const cols = new Set();
      for (const part of splitTop(balanced(s, m.index + m[0].length, "(", ")"), "([", ")]")) {
        const def = part.trim();
        const name = /^"?([a-z_][a-z0-9_]*)"?\s/i.exec(def);
        if (name && !NOT_COLUMN.test(def)) cols.add(name[1].toLowerCase());
      }
      columns.set(m[1].toLowerCase(), cols);
    }
    for (const m of s.matchAll(/alter\s+table\s+(?:if\s+exists\s+)?(?:only\s+)?(?:public\.)?([a-z_][a-z0-9_]*)\s+([^;]*);/gi)) {
      const cols = columns.get(m[1].toLowerCase());
      if (!cols) continue;
      for (const a of m[2].matchAll(/add\s+column\s+(?:if\s+not\s+exists\s+)?"?([a-z_][a-z0-9_]*)/gi)) cols.add(a[1].toLowerCase());
      for (const a of m[2].matchAll(/drop\s+column\s+(?:if\s+exists\s+)?"?([a-z_][a-z0-9_]*)/gi)) cols.delete(a[1].toLowerCase());
      for (const a of m[2].matchAll(/rename\s+column\s+"?([a-z_][a-z0-9_]*)"?\s+to\s+"?([a-z_][a-z0-9_]*)/gi)) { cols.delete(a[1].toLowerCase()); cols.add(a[2].toLowerCase()); }
    }
    for (const m of s.matchAll(/check\s*\(\s*\(?\s*([a-z_]*status)\s*\)?\s+in\s*\(([^)]*)\)/gi)) {
      for (const v of m[2].matchAll(/'([^']+)'/g)) if (!statusValues.has(v[1])) statusValues.set(v[1], `${f} ${m[1]}`);
    }
  }
  return { columns, statusValues };
}

function scanBackend(migrations) {
  const files = walk(path.join(ROOT, "backend/src/main/java"), (p) => p.endsWith(".java"));
  const records = new Map();
  const roots = [];
  const keys = new Map();
  const note = (k, where) => { const snake = k.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase(); if (!keys.has(snake)) keys.set(snake, where); };
  for (const file of files) {
    const raw = fs.readFileSync(file, "utf8");
    const s = stripJava(raw);
    const where = path.basename(file);
    for (const m of s.matchAll(/\brecord\s+(\w+)\s*(?:<[^>(]*>)?\s*\(/g)) {
      const comps = splitTop(balanced(s, m.index + m[0].length, "(", ")")).map((rawComp) => {
        const prop = /@JsonProperty\(\s*(?:value\s*=\s*)?"([^"]+)"/.exec(rawComp);
        if (/@JsonIgnore\b/.test(rawComp)) return null;
        const t = rawComp.replace(/@[\w.]+(\([^)]*\))?/g, "").trim().replace(/\s+/g, " ");
        const name = t.slice(t.lastIndexOf(" ") + 1);
        return /^\w+$/.test(name) ? { name: prop ? prop[1] : name, type: t.slice(0, t.lastIndexOf(" ")) } : null;
      }).filter(Boolean);
      records.set(m[1], [...(records.get(m[1]) ?? []), ...comps]);
    }
    // 返回 Map<String, Object>（或其列表）的方法：方法体里的 Map 字面量键就是响应键。
    for (const m of s.matchAll(/(?:List<\s*)?Map<String,\s*(?:Object|\?)>\s*>?\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w.,\s]+)?\{/g)) {
      mapKeys(balanced(s, m.index + m[0].length, "{", "}")).forEach((k) => note(k, `${where} ${m[1]}()`));
    }
    // queryForList / queryForMap 直接整行返回的表：迁移里该表的全部列都会成为响应键。
    for (const m of raw.matchAll(/queryFor(?:List|Map)\(\s*(?:"""|")\s*(?:select\s+(?:\w+\.)?\*\s+from\s+([a-z_][a-z0-9_]*)|[^;]*?(?:insert\s+into|update)\s+([a-z_][a-z0-9_]*)[^;]*?returning\s+\*)/gi)) {
      const table = (m[1] ?? m[2]).toLowerCase();
      for (const c of migrations.columns.get(table) ?? []) note(c, `${where} ${table}.*`);
    }
    if (!file.endsWith("Controller.java")) continue;
    for (const m of s.matchAll(/\bApiResponse</g)) roots.push(...(balanced(s, m.index + m[0].length, "<", ">").match(/[A-Z]\w*/g) ?? []));
    mapKeys(s).forEach((k) => note(k, where));
  }
  const done = new Set();
  while (roots.length) {
    const id = roots.pop();
    if (done.has(id)) continue;
    done.add(id);
    for (const c of records.get(id) ?? []) {
      note(c.name, id);
      roots.push(...(c.type.match(/[A-Z]\w*/g) ?? []));
    }
  }
  return keys;
}

const backendLabels = new Map([...fs.readFileSync(BACKEND_LABELS, "utf8").matchAll(/\bp\("([^"]+)",\s*"([^"]+)"\)/g)].map((m) => [m[1], m[2]]));
const fieldLabels = objectLiteral(DICT, "FIELD_LABELS");
const valueLabels = objectLiteral(DICT, "VALUE_LABELS");
const keyValueLabels = objectLiteral(DICT, "KEY_VALUE_LABELS", true);
const statusCodes = new Set(objectLiteral(STATUS, "STATUS_LABELS").keys());
const known = new Set([...fieldLabels.keys()].map(compact));
const labeled = (key, v) => keyValueLabels.get(compact(key))?.has(v) || valueLabels.has(v) || statusCodes.has(v);

const migrations = scanMigrations();
const front = scanFrontend();
const back = scanBackend(migrations);
const missingKeys = new Map();
for (const [k, where] of [...front.keys, ...back]) if (!known.has(compact(k)) && !missingKeys.has(compact(k))) missingKeys.set(compact(k), `${k}（${where}）`);
const missingValues = [...front.values].map(([pair, where]) => [...pair.split("\u0000"), where]).filter(([key, v]) => !labeled(key, v)).map(([key, v, where]) => `${key}=${v}（${where}）`);
const missingStatus = [...migrations.statusValues].filter(([v]) => !labeled("status", v)).map(([v, where]) => `${v}（${where}）`);
const missingTemplates = [...front.templates].filter(([tpl]) => {
  const re = new RegExp(`^${tpl.slice(1, -1).split(/\$\{[^}]*\}/).map((x) => x.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("[a-z0-9_-]+")}$`);
  return ![...valueLabels.keys()].some((code) => re.test(code));
}).map(([tpl, where]) => `${tpl}（${where}）`);
const mismatched = [...fieldLabels].filter(([k, v]) => backendLabels.has(k) && backendLabels.get(k) !== v).map(([k, v]) => `${k}：前端「${v}」≠ 后端「${backendLabels.get(k)}」`);

console.log(`[admin-result-labels] 前端键 ${front.keys.size}，后端响应键 ${back.size}，前端取值 ${front.values.size}（模板 ${front.templates.size}），后端状态取值 ${migrations.statusValues.size}，字典 ${fieldLabels.size} 键 / ${valueLabels.size} 取值 / ${keyValueLabels.size} 按键取值表`);
const report = (title, list) => { if (list.length) console.log(`${title} ${list.length} 条：\n  ${list.join("\n  ")}`); };
report("缺字段中文名", [...missingKeys.values()]);
report("缺取值中文名", missingValues);
report("缺后端状态中文名", missingStatus);
report("模板动作码无匹配", missingTemplates);
report("与后端 FieldLabels 口径不一致", mismatched);
const red = missingKeys.size + missingValues.length + missingStatus.length + missingTemplates.length + mismatched.length;
console.log(red ? `[admin-result-labels] 红：${red} 处` : "[admin-result-labels] 绿：0 处缺失");
process.exit(red ? 1 : 0);
