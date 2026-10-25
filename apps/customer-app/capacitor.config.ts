// 文件版本：V0.01
// 官方客人 App 原生壳（Capacitor）配置：页面资源从本地 dist/ 打进安装包、由原生壳本地加载；不设 server.url（不加载远程网页）。
// appId／appName 是开发值（调试包包名 com.complex.customer.dev）；发行包的正式包名与中文名由 android/app/build.gradle
// 按 CUSTOMER_ANDROID_APPLICATION_ID／CUSTOMER_ANDROID_APP_NAME 覆盖，缺失或仍为开发值即拒绝构建。
// CapacitorHttp：页面里的 fetch 走原生请求通道（证书按 network_security_config：调试包只多信任 160 自签证书，发行包只认系统证书、不许 http）。
// loggingBehavior none：原生桥不把插件调用参数（含会话令牌、请求头）打进系统日志。
import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.complex.customer.dev",
  appName: "综合体客人端（开发）",
  webDir: "dist",
  loggingBehavior: "none",
  plugins: {
    CapacitorHttp: { enabled: true },
  },
};

export default config;
