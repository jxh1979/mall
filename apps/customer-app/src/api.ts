// 文件版本：V0.01
// 客人 App 的请求适配：transport 用 fetch，不带 cookie——客人会话只走 X-Customer-Token 头（令牌在内存与本端安全会话里）。
// 原生壳里的 fetch 由原生请求通道承接（C4/C5 接入），接口与本文件不变。
import { CustomerApi, CustomerService, type CustomerRequestContext, type CustomerTransport } from "@complex/customer-core";

export const CUSTOMER_APP_VERSION: string = import.meta.env.VITE_CUSTOMER_APP_VERSION ?? "0.0.0";
/** 接口基址：开发服务器同源走 /api 代理（空串）；打包给原生壳时由 VITE_CUSTOMER_API_BASE_URL 指定。 */
export const CUSTOMER_API_BASE_URL: string = String(import.meta.env.VITE_CUSTOMER_API_BASE_URL ?? "").replace(/\/$/, "");
/** H5 支付页允许的主机（逗号分隔，支付供应商资料到位后配置）；未配置时 H5 支付一律「支付暂不可用，未扣款」。 */
export const CUSTOMER_PAY_HOSTS: readonly string[] = String(import.meta.env.VITE_CUSTOMER_PAY_HOSTS ?? "")
  .split(",").map((host) => host.trim().toLowerCase()).filter(Boolean);
/** 客人深链主机（正式域名未到位时为空，深链一律不认）。 */
export const CUSTOMER_LINK_HOST: string = String(import.meta.env.VITE_CUSTOMER_LINK_HOST ?? "");

export const fetchTransport: CustomerTransport = async ({ method, url, headers, body, form }) => {
  // 带文件的提交（投诉附件、学生证件照片）组 FormData；分隔符由运行环境写进 content-type。
  let payload: BodyInit | undefined = body;
  if (form) {
    const data = new FormData();
    for (const [key, value] of Object.entries(form.fields)) data.append(key, value);
    for (const item of form.files) data.append(item.field, item.file as Blob, item.fileName);
    payload = data;
  }
  const response = await fetch(url, { method, headers: new Headers(headers), body: payload, credentials: "omit" });
  return { status: response.status, text: await response.text() };
};

export function createCustomerService(
  getToken: () => string | null = () => null,
  getContext: () => CustomerRequestContext = () => ({}),
): CustomerService {
  return new CustomerService(new CustomerApi({
    baseUrl: CUSTOMER_API_BASE_URL,
    terminalType: "app",
    clientVersion: CUSTOMER_APP_VERSION,
    transport: fetchTransport,
    getToken,
    getContext,
  }));
}
