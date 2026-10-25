// 文件版本：V0.01
// 官方客人 App 的客人专用构建：只打客人代码（入口 src/main.tsx 只挂 CustomerApp），产物 dist/ 由原生壳本地加载，
// base 用相对路径；不设远程网页地址。开发服务器的 /api 代理只供浏览器调试与 e2e，打包产物不含代理，接口基址取 VITE_CUSTOMER_API_BASE_URL。
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";
import { defineConfig } from "vitest/config";

const { version } = JSON.parse(readFileSync(new URL("./package.json", import.meta.url), "utf8")) as { version: string };
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8080";

export default defineConfig({
  base: "./",
  plugins: [react()],
  // 本端版本随每个请求带 X-Client-Version，终端开关可按版本关闭。
  define: { "import.meta.env.VITE_CUSTOMER_APP_VERSION": JSON.stringify(version) },
  server: {
    proxy: {
      "/api": { target: apiProxyTarget, changeOrigin: true },
    },
  },
  build: {
    outDir: "dist",
    // 客人包由原生壳从本地加载、不经网络下载，单块约 1.1MB 不影响首屏；阈值只用来发现异常膨胀。
    chunkSizeWarningLimit: 1500,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    globals: false,
    restoreMocks: true,
    // 单测用的客人链接主机（正式域名未到位，打包产物不带此值）。
    env: { VITE_CUSTOMER_LINK_HOST: "c.complex.test" },
  },
});
