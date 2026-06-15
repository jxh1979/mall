export const FOUNDATION_MODULES = [
  "多商户与组织",
  "权限、账号与岗位",
  "开放平台与安全鉴权",
  "全链路事件留痕",
] as const;

export type FoundationModule = (typeof FOUNDATION_MODULES)[number];

export const OPEN_APP_LOCK_THRESHOLD = 3;
