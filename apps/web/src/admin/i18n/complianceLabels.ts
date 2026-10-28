// 文件版本：V0.01
// 第十八批 E 段第 22 条：合规证照类型与到期提醒类型中文名。证照类型＝后端九类闭集（V028 chk_compliance_expiry_item_type、
// ComplianceService.EXPIRY_ITEM_TYPES），提醒类型＝V568 chk_compliance_reminder_type。ADM-COMP-001 与商户专区「我的证照」共用。
export const COMPLIANCE_ITEM_TYPE_LABELS: Record<string, string> = {
  business_license: "营业执照",
  food_license: "食品经营许可证",
  alcohol_license: "酒类经营许可证",
  fire_certificate: "消防安全证照",
  special_equipment: "特种设备使用证",
  insurance: "保险",
  supplier_qualification: "供应商资质",
  inspection_certificate: "检验检测证书",
  custom: "其他证照",
};

export const COMPLIANCE_REMINDER_TYPE_LABELS: Record<string, string> = {
  upcoming: "到期预警",
  expired: "已到期",
  grace: "宽限期提醒",
};
