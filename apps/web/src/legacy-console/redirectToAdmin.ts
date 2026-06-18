import { message } from "antd";

// 终端铁律：Web（legacy-console）不可直接执行会改变核销结果/资金/库存的写动作
// （门票/权益核销、人工放行、酒吧结账支付、存酒/取酒等）。
// 这些动作统一改为：给出提示并跳转到管理后台对应页面，由管理后台/现场端执行。

// 管理后台相关页面路由（与 admin/adminRoutes.ts 保持一致）。
export const adminRoutes = {
  ticketingVerification: "/admin/ticketing/orders",
  amusementSessions: "/admin/amusement/sessions",
  barOrders: "/admin/bar/orders",
  barStoredWine: "/admin/bar/stored-wine",
} as const;

// 提示该操作须在管理后台/现场端执行，并跳转到对应的管理后台页面。
export function redirectToAdmin(actionLabel: string, route: string) {
  message.warning(`${actionLabel}须在管理后台/现场端执行，正在跳转到管理后台…`);
  window.location.assign(route);
}
