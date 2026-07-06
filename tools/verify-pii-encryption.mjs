#!/usr/bin/env node
// 消费者 PII 落库加密门禁(2026-07-05,防"buyer_phone 明文"类回归复发)。
// 口径:登记表列出所有"消费者手机号/PII 必须密文落库"的列 + 其写入文件;
// 断言每处 addValue("<col>", ...) 都被 piiCipher.encrypt(...) 包裹。明文即 FAIL。
// 这是"少信 markdown、多信可执行门禁"的落点:审计发现的加密口径由此持续强制。
import { readFileSync } from "node:fs";

// 消费者 PII 列登记(新增消费者手机号/证件等落库列须登记于此;商业联系人不在此列)。
const REGISTRY = [
  { table: "customers", column: "phone", writeFile: "backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java" },
  { table: "ticket_orders", column: "buyer_phone", writeFile: "backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingParameterSource.java" },
  { table: "amusement_group_bookings", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java" },
];

const failures = [];
for (const { table, column, writeFile } of REGISTRY) {
  let src;
  try {
    src = readFileSync(writeFile, "utf8");
  } catch {
    failures.push(`${table}.${column}: 写入文件缺失 ${writeFile}(登记表与代码漂移,请更新登记表）`);
    continue;
  }
  // 匹配该列的 addValue 绑定行:.addValue("<col>", <expr>)
  const re = new RegExp(`addValue\\(\\s*"${column}"\\s*,\\s*([^\\n]*?)\\)\\s*(?:\\.|;|$)`, "g");
  let m;
  let seen = 0;
  while ((m = re.exec(src)) !== null) {
    seen += 1;
    const expr = m[1];
    if (!/piiCipher\.encrypt\s*\(/.test(expr)) {
      failures.push(`${table}.${column}: addValue 未经 piiCipher.encrypt 包裹（明文落库风险）→ ${writeFile}: addValue("${column}", ${expr.trim()})`);
    }
  }
  if (seen === 0) {
    failures.push(`${table}.${column}: 在 ${writeFile} 未找到 addValue("${column}", ...) 写入点（登记表与代码漂移，请核对）`);
  }
}

if (failures.length > 0) {
  console.error("PII_ENCRYPTION_FAIL: 消费者 PII 列存在明文落库或登记漂移");
  for (const f of failures) console.error(`- ${f}`);
  process.exit(1);
}
console.log(`PII_ENCRYPTION_PASS: ${REGISTRY.length} 个消费者 PII 列均经 piiCipher.encrypt 落库`);
