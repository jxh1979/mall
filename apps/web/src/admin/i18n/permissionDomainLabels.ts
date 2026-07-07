// 权限域(permission.domain)中文标签——权限模板/字典页按域分组的大标题用。
// 机读 domain 值保持不变(用于分组/接口),仅显示层中文化。遵循"所有终端零英文全中文"铁律。
const PERMISSION_DOMAIN_LABELS: Record<string, string> = {
  access: "访问控制",
  amusement: "游乐",
  analytics: "经营分析",
  audit: "审计",
  bar: "酒吧",
  compliance: "合规",
  customer: "客户",
  evidence: "证据留痕",
  facility: "设施",
  finance: "财务",
  floor_plan: "平面图",
  governance: "治理",
  integration: "集成对接",
  inventory: "库存",
  merchant_billing: "商户账单",
  merchant_portal: "商户专区",
  merchant_service: "商户服务",
  "open-platform": "开放平台",
  ops: "运维",
  partner: "合作方",
  platform: "平台",
  service: "服务点单",
  staff: "员工",
  terminal: "终端设备",
  ticketing: "票务",
  verification: "核验",
  other: "其他",
};

/** 权限域机读值 → 中文显示名;未知域回退原值(不丢信息)。 */
export function formatPermissionDomain(domain: string | null | undefined): string {
  if (!domain) return "其他";
  return PERMISSION_DOMAIN_LABELS[domain] ?? domain;
}
