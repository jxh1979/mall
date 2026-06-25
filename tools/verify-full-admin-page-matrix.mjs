import { readFileSync } from "node:fs";

const matrixPath = "docs/full-admin-page-permission-field-matrix-v0.1.csv";
const routesPath = "apps/web/src/admin/adminRoutes.ts";
const mvpTestPath = "tests/e2e/full-admin-mvp.spec.ts";
const remainingTestPath = "tests/e2e/full-admin-remaining.spec.ts";

const bespokePages = new Set([
  "ADM-OV-001",
  "ADM-ACC-001",
  "ADM-ACC-002",
  "ADM-TKT-001",
  "ADM-TKT-002",
  "ADM-TKT-003",
  "ADM-BAR-001",
  "ADM-BAR-002",
  "ADM-BAR-003",
  "ADM-BAR-004",
  "ADM-BAR-005",
  "ADM-INV-001",
  "ADM-AMU-003",
  "ADM-AMU-004",
  "ADM-GOV-003",
  "ADM-GOV-004",
  "ADM-GOV-005",
  "ADM-RISK-001",
  "ADM-GOV-006",
  "ADM-FIN-001",
  "ADM-FIN-002",
  "ADM-FIN-003",
  "ADM-MER-001",
  "ADM-MER-002",
  "ADM-AMU-001",
  "ADM-AMU-002",
  "ADM-INT-001",
  "ADM-INT-002",
  "ADM-INT-003",
  "ADM-EVD-001",
  "ADM-ANA-001",
  "ADM-ANA-002",
  "ADM-ANA-003",
  "ADM-LEASE-001",
  "ADM-FAC-001",
  "ADM-FAC-002",
  "ADM-FAC-003",
  "ADM-OPS-001",
  "ADM-GOV-001",
  "ADM-GOV-002",
]);

function read(path) {
  return readFileSync(path, "utf8");
}

function stripBom(value) {
  return value.replace(/^\uFEFF/, "");
}

function parseCsvLine(line) {
  const cells = [];
  let cell = "";
  let quoted = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    const next = line[i + 1];
    if (char === '"' && quoted && next === '"') {
      cell += '"';
      i += 1;
      continue;
    }
    if (char === '"') {
      quoted = !quoted;
      continue;
    }
    if (char === "," && !quoted) {
      cells.push(cell.trim());
      cell = "";
      continue;
    }
    cell += char;
  }
  cells.push(cell.trim());
  return cells;
}

function parseCsv(content) {
  const lines = stripBom(content).split(/\r?\n/).filter((line) => line.trim().length > 0);
  const headers = parseCsvLine(lines[0]).map(stripBom);
  return lines.slice(1).map((line) => {
    const cells = parseCsvLine(line);
    const row = {};
    headers.forEach((header, index) => {
      row[header] = cells[index] ?? "";
    });
    return row;
  });
}

function parseRoutes(source) {
  const routes = new Map();
  const routePattern = /\{\s*pageId:\s*"([^"]+)",\s*label:\s*"([^"]+)",\s*module:\s*"([^"]+)",\s*(?:group:\s*"[^"]+",\s*)?route:\s*"([^"]+)",\s*requiredPermissions:\s*\[([^\]]*)\]\s*\}/g;
  let match;
  while ((match = routePattern.exec(source)) !== null) {
    const [, pageId, label, moduleName, route, permissionsSource] = match;
    const permissions = [...permissionsSource.matchAll(/"([^"]+)"/g)].map((item) => item[1]);
    routes.set(pageId, { pageId, label, module: moduleName, route, permissions });
  }
  return routes;
}

function expectedRoutePermission(row) {
  const permissions = row.permissions.split(";").map((item) => item.trim()).filter(Boolean);
  return permissions.find((permission) => permission.endsWith(".read")) ?? permissions[0] ?? "";
}

function reportMismatch(errors, pageId, field, expected, actual) {
  errors.push(`${pageId} ${field} 不一致：期望=${expected || "<?>"} 实际=${actual || "<?>"}`);
}

const matrixRows = parseCsv(read(matrixPath));
const routes = parseRoutes(read(routesPath));
const testsSource = `${read(mvpTestPath)}\n${read(remainingTestPath)}`;
const errors = [];

for (const row of matrixRows) {
  const pageId = row.page_id;
  const route = routes.get(pageId);
  if (!route) {
    errors.push(`${pageId} ?? adminRoutes.ts ??`);
    continue;
  }

  if (route.label !== row.page_name) reportMismatch(errors, pageId, "页面名称", row.page_name, route.label);
  if (route.module !== row.module) reportMismatch(errors, pageId, "模块", row.module, route.module);
  if (route.route !== row.route) reportMismatch(errors, pageId, "路由", row.route, route.route);

  const expectedPermission = expectedRoutePermission(row);
  const actualPermission = route.permissions[0] ?? "";
  if (actualPermission !== expectedPermission) {
    reportMismatch(errors, pageId, "首个读权限", expectedPermission, actualPermission);
  }

  if (!bespokePages.has(pageId)) {
    errors.push(`${pageId} ?? 专属页面缺失（不在 bespokePages 内）??`);
  }

  if (!testsSource.includes(pageId)) {
    errors.push(`${pageId} 缺少 full-admin Playwright 验收覆盖`);
  }
}

for (const pageId of routes.keys()) {
  if (!matrixRows.some((row) => row.page_id === pageId)) {
    errors.push(`${pageId} 存在路由但缺少页面矩阵行`);
  }
}

if (errors.length > 0) {
  console.error("完整后台页面矩阵校验失败");
  for (const error of errors) console.error(`- ${error}`);
  process.exit(1);
}

console.log(`完整后台页面矩阵校验通过：${matrixRows.length} 个页面均有路由、权限、UI 与测试覆盖`);
