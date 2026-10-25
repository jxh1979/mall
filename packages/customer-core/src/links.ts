// 文件版本：V0.01
// 客人深链（分享与扫码共用，三端同一格式）：https://<客人链接主机>/c/<路由>?参数。
// 只认配置的链接主机、https 与 /c/ 前缀；路由在白名单内且参数逐个按格式校验通过才返回，其余一律 null——
// 不把扫到或收到的任意地址当作可跳转地址。链接里只放业务参数，令牌、会话一律不进链接。
// 不用 URL 类：小程序运行环境没有它。

export type CustomerLinkRoute = "parking" | "bar" | "table" | "ticket-claim" | "voucher-claim" | "invitation";
export type CustomerLink = { route: CustomerLinkRoute; params: Record<string, string> };

type ParamRule = { pattern: RegExp; required: boolean };

const ID = /^[A-Za-z0-9_.:-]{1,128}$/;
const need = (pattern: RegExp): ParamRule => ({ pattern, required: true });
const may = (pattern: RegExp): ParamRule => ({ pattern, required: false });

const ROUTES: Record<CustomerLinkRoute, Record<string, ParamRule>> = {
  // 停车出口码：项目＋车牌（省份简称开头）或无牌车临时码。
  parking: { project_id: need(ID), code: may(/^[一-龥A-Za-z0-9]{1,16}$/) },
  // 酒吧专属购票入口（AUD-076）。
  bar: { bar_id: need(ID) },
  // 散台固定桌台码（扫码点单）。
  table: { table_id: need(ID) },
  "ticket-claim": { claim_code: need(ID) },
  "voucher-claim": { claim_code: need(ID) },
  // 订卡邀请函：接受时后端核验防伪标记、卡座与可进人数（ROOT-03-R25），四项缺一不可。
  invitation: { token: need(ID), sig: need(ID), table_id: need(ID), capacity: need(/^[1-9][0-9]{0,2}$/) },
};

const LINK = /^https:\/\/([^/?#]+)\/c\/([a-z-]+)\/?(?:\?([^#]*))?(?:#.*)?$/;

function decode(part: string): string | null {
  try {
    return decodeURIComponent(part.replace(/\+/g, " "));
  } catch {
    return null;
  }
}

function routeOf(name: string): CustomerLinkRoute | null {
  return Object.prototype.hasOwnProperty.call(ROUTES, name) ? (name as CustomerLinkRoute) : null;
}

/** linkHost：客人链接主机（含端口时写全，如 c.example.com）；未配置（空串）时一律不认。 */
export function parseCustomerLink(raw: string, linkHost: string): CustomerLink | null {
  if (!linkHost) return null;
  const match = LINK.exec(raw.trim());
  if (!match || (match[1] ?? "").toLowerCase() !== linkHost.toLowerCase()) return null;
  const route = routeOf(match[2] ?? "");
  if (!route) return null;
  const query = new Map<string, string>();
  for (const part of (match[3] ?? "").split("&")) {
    if (!part) continue;
    const eq = part.indexOf("=");
    const key = decode(eq < 0 ? part : part.slice(0, eq));
    const value = decode(eq < 0 ? "" : part.slice(eq + 1));
    if (key === null || value === null) return null;
    query.set(key, value);
  }
  const params: Record<string, string> = {};
  for (const [key, rule] of Object.entries(ROUTES[route])) {
    const value = query.get(key);
    if (!value) {
      if (rule.required) return null;
      continue;
    }
    if (!rule.pattern.test(value)) return null;
    params[key] = value;
  }
  return { route, params };
}

/** 生成分享用深链：只写该路由认的参数；参数不合格式返回 null（不生成别人打不开的链接）。 */
export function buildCustomerLink(linkHost: string, route: CustomerLinkRoute, params: Record<string, string>): string | null {
  if (!linkHost) return null;
  const pairs: string[] = [];
  for (const [key, rule] of Object.entries(ROUTES[route])) {
    const value = params[key];
    if (!value) {
      if (rule.required) return null;
      continue;
    }
    if (!rule.pattern.test(value)) return null;
    pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
  }
  return `https://${linkHost}/c/${route}${pairs.length ? `?${pairs.join("&")}` : ""}`;
}
