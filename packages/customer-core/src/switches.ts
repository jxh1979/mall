// 文件版本：V0.01
// 客人端开关隐藏（三端共用）。依据 PGM「关闭后菜单不可见或不可点，接口也要拒绝业务动作」「可以按全局、综合体、商户、业务系统、
// 模块、版本/设备、灰度开关」，与后端 GET /api/v1/customer/terminal-switches/effective（只返回已关闭的行）。
// 口径同后台菜单闸 apps/web/src/admin/security/terminalGate.ts：只认 terminal.enabled 行——
// 全局行、等于本端版本的版本行、等于当前综合体的综合体行 → 本端暂停服务；模块行 → 隐藏该模块；拉取失败（rows=null）→ 暂停服务。
// 接口侧仍由后端终端总开关按请求头（X-Client-Version、X-Module-Code、X-Project-Id）独立拒绝。
import type { TerminalSwitchRow } from "./types";

/** 客人端模块键（请求头 X-Module-Code 与开关行 scope_id 同值）。 */
export const CUSTOMER_MODULES = [
  "ticketing",
  "bar",
  "points",
  "wallet",
  "vouchers",
  "transfer",
  "saved_bottles",
  "parking",
  "complaints",
  "account",
] as const;
export type CustomerModule = (typeof CUSTOMER_MODULES)[number];

export type CustomerSwitchState = { paused: boolean; hiddenModules: ReadonlySet<string> };

const MASTER = "terminal.enabled";

export function customerSwitchState(
  rows: readonly TerminalSwitchRow[] | null,
  context: { version: string; projectId?: string | null },
): CustomerSwitchState {
  if (rows === null) return { paused: true, hiddenModules: new Set() };
  const hidden = new Set<string>();
  for (const row of rows) {
    if (row.capabilityCode !== MASTER) continue;
    if (
      row.scopeType === "global" ||
      (row.scopeType === "version" && row.scopeId === context.version) ||
      (row.scopeType === "project" && Boolean(context.projectId) && row.scopeId === context.projectId)
    ) {
      return { paused: true, hiddenModules: new Set() };
    }
    if (row.scopeType === "module") hidden.add(row.scopeId);
  }
  return { paused: false, hiddenModules: hidden };
}
