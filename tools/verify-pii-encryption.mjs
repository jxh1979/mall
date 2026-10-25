#!/usr/bin/env node
// 文件版本：V0.03
// PII 落库加密门禁(2026-07-05 立,防"buyer_phone 明文"类回归复发;2026-07-27 扩到全部主体)。
// 口径:登记表列出所有"自然人手机号/PII 必须密文落库"的列 + 其写入文件;
// 断言每处 addValue("<col>", ...) 都被 piiCipher.encrypt(...) 包裹。明文即 FAIL。
// 这是"少信 markdown、多信可执行门禁"的落点:审计发现的加密口径由此持续强制。
import { readFileSync } from "node:fs";

// PII 列登记(新增任何自然人手机号/证件等落库列都须登记于此)。
// 2026-07-27 扩容依据:v7 综合审计 P0-A / ★GAP-07 / SEC-04——原登记只覆盖"消费者"三列,
// 注释还写着"商业联系人不在此列",而实证 merchants 59/59 明文入库。PIPL 不按消费者/商业伙伴划线,
// 商户/供应商/招商线索/费用通知/进场报备联系人同为自然人手机号,一并纳入(迁移 V339)。
const REGISTRY = [
  { table: "staff_profiles", column: "name", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  { table: "staff_profiles", column: "phone", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  { table: "staff_profiles", column: "id_doc", writeFile: "backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java" },
  // ── 消费者侧(V188/V247/V248)──
  { table: "customers", column: "phone", writeFile: "backend/src/main/java/com/complex/platform/customer/JdbcCustomerRepository.java" },
  { table: "ticket_orders", column: "buyer_phone", writeFile: "backend/src/main/java/com/complex/platform/ticketing/JdbcTicketingParameterSource.java" },
  { table: "amusement_group_bookings", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/amusement/JdbcAmusementRepository.java" },
  // ── 多主体联系人侧(V339,P0-A/GAP-07)──
  { table: "merchants", column: "contact_name", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
  { table: "merchants", column: "secondary_contact_name", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
  { table: "merchants", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
  { table: "merchants", column: "secondary_contact_phone", writeFile: "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java" },
  { table: "inventory_suppliers", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java" },
  { table: "leasing_leads", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/leasing/JdbcLeasingRepository.java" },
  { table: "merchant_fee_notification_recipient", column: "recipient_phone", writeFile: "backend/src/main/java/com/complex/platform/merchantbilling/JdbcMerchantFeeNotificationRecipientRepository.java" },
  { table: "site_entry_reports", column: "contact_phone", writeFile: "backend/src/main/java/com/complex/platform/facility/JdbcSiteEntryReportRepository.java" },
  // ── 存酒侧(V061/V102；审计 20260902T183912Z M009 A1-02 补登记：写入点在仓储层，此前漏登记且无信封 CHECK)──
  { table: "saved_bottles", column: "customer_phone", writeFile: "backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java" },
  { table: "saved_bottles", column: "transfer_to_phone", writeFile: "backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java" },
  { table: "saved_bottle_transfers", column: "to_phone", writeFile: "backend/src/main/java/com/complex/platform/inventory/JdbcInventoryRepository.java" },
  // ── 订台侧(V545；第十四批 OBS-13-03：员工代订录入的客人手机号，只供订台短信)──
  { table: "bar_reservations", column: "customer_phone", writeFile: "backend/src/main/java/com/complex/platform/bar/JdbcBarRepository.java" },
  // ── 短信发送留痕(V547；第十四批复核：各域短信收件人手机号，含员工代订录入的非会员手机号)──
  { table: "notification_send_log", column: "recipient", writeFile: "backend/src/main/java/com/complex/platform/notification/JdbcNotificationRepository.java" },
  // ── 幂等重放响应体(V399；审计 20260902T183912Z XM-013)：服务层返回对象原样序列化落库，含订单 / 预订 / 客人档案的手机号，
  //    此前为 jsonb 明文且表无过期清理；现只存 enc:v1: 信封（库上 NOT VALID CHECK 守新写入，存量启动期回填）──
  { table: "idempotency_records", column: "response_body", writeFile: "backend/src/main/java/com/complex/platform/common/idempotency/JdbcIdempotencyRepository.java" },
  // ticket_transfers.receiver_phone / voucher_transfers.receiver_phone 在服务层加密(TicketTransferService /
  // VoucherTransferService)，仓储层 addValue 不带 piiCipher.encrypt，本登记表的正则守不住——由 V396 库上信封 CHECK 守。
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
  console.error("PII_ENCRYPTION_FAIL: PII 列存在明文落库或登记漂移");
  for (const f of failures) console.error(`- ${f}`);
  process.exit(1);
}
console.log(`PII_ENCRYPTION_PASS: ${REGISTRY.length} 个 PII 列均经 piiCipher.encrypt 落库`);
