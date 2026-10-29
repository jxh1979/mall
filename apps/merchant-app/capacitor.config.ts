// 文件版本：V0.01
// 商户手机应用原生壳（Capacitor，第 25 条）配置：页面资源从本地 dist/ 打进安装包、由原生壳本地加载；不设 server.url（不加载远程网页）。
// appId／appName 是开发值（调试包包名 com.complex.merchant.dev）；正式包名、中文名、签名与上架属外部资料，到位前只出调试包。
// CapacitorHttp：页面里的 fetch 走原生请求通道（证书按 network_security_config：调试包只多信任 160 自签证书，发行包只认系统证书、不许 http）。
// loggingBehavior none：原生桥不把插件调用参数（含会话令牌、请求头）打进系统日志。
import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.complex.merchant.dev",
  appName: "综合体商户端（开发）",
  webDir: "dist",
  loggingBehavior: "none",
  plugins: {
    CapacitorHttp: { enabled: true },
  },
};

export default config;
