// 文件版本：V0.01
// 客人入口与员工端分离的构建断言（PGM「受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）」）：
// - apps/web/dist：不得再含客人接口路径（客人壳已移出 apps/web）；
// - apps/customer-app/dist：不得含后台、经销商门户代码与员工侧收款接口（客人付款只走本人订单／订台／扫码点单／充值的在线付款）；
//   页面引用的脚本与样式只许本地相对路径（不加载远程网页资源）。
// 先构建再跑；dist 不存在即失败（没构建不算通过）。
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const findings = [];

const checks = [
  { dist: "apps/web/dist", forbidden: ["/api/v1/customer/"] },
  { dist: "apps/customer-app/dist", forbidden: ["admin-shell", "dealer-portal", "AdminApp", "DealerPortalApp", "/api/v1/auth/login", "/api/v1/finance/payment-intents"] },
];

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full, out);
    else out.push(full);
  }
  return out;
}

for (const { dist, forbidden } of checks) {
  const dir = path.join(root, dist);
  if (!fs.existsSync(dir)) {
    findings.push(`${dist}: 不存在，请先构建`);
    continue;
  }
  for (const file of walk(dir).filter((f) => /\.(js|html|css)$/.test(f))) {
    const rel = path.relative(root, file).replaceAll(path.sep, "/");
    const text = fs.readFileSync(file, "utf8");
    for (const marker of forbidden) {
      if (rel.includes(marker) || text.includes(marker)) findings.push(`${rel}: 含 ${marker}`);
    }
  }
}

const customerIndex = path.join(root, "apps/customer-app/dist/index.html");
if (fs.existsSync(customerIndex)) {
  const html = fs.readFileSync(customerIndex, "utf8");
  for (const match of html.matchAll(/<(?:script|link)\b[^>]*\b(?:src|href)="([^"]+)"/g)) {
    const ref = match[1];
    if (/^(?:[a-z]+:)?\/\//i.test(ref) || ref.startsWith("/")) findings.push(`apps/customer-app/dist/index.html: 非本地相对资源 ${ref}`);
  }
}

if (findings.length) {
  console.error(`[customer-shell-separation] FAIL: ${findings.length} 处`);
  for (const finding of findings) console.error(`  ${finding}`);
  process.exit(1);
}
console.log("[customer-shell-separation] PASS: apps/web 产物无客人壳；客人包无后台与经销商门户代码，页面资源均为本地相对路径。");
