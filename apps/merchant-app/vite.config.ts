// 文件版本：V0.01
// 商户手机应用的构建：入口 src/main.tsx 挂 apps/web 的后台外壳（不复制页面），产物 dist/ 由原生壳本地加载；不设远程网页地址。
// base 用 /（不用 ./）：后台页面里有整页链接 <a href="/admin/…">，WebView 整页加载这类地址时原生壳本地服务回 index.html，
// 资源按 ./ 会解析到 /admin/…/assets/ 下取不到而白屏。开发服务器的 /api 代理只供浏览器调试，打包产物不含代理，接口基址取 VITE_API_BASE_URL。
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
import { defineConfig } from "vitest/config";

const { version } = JSON.parse(readFileSync(new URL("./package.json", import.meta.url), "utf8")) as { version: string };
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8080";

export default defineConfig({
  base: "/",
  plugins: [react()],
  // 本端版本随每个请求带 X-Client-Version，终端开关可按版本关闭。
  define: { "import.meta.env.VITE_MERCHANT_APP_VERSION": JSON.stringify(version) },
  server: {
    proxy: {
      "/api": { target: apiProxyTarget, changeOrigin: true },
    },
  },
  build: {
    outDir: "dist",
    // 商户包由原生壳从本地加载、不经网络下载；阈值只用来发现异常膨胀。
    chunkSizeWarningLimit: 1500,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    globals: false,
    restoreMocks: true,
  },
});
