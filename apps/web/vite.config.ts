import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8080";

export default defineConfig(({ mode }) => ({
  // 子路径部署：构建产物落在 /complex/ 前缀下（门户 portal.py 把 /complex/ 代理到 preview）；
  // dev（mode!=='serve'）仍在根路径，方便本地直接调试。
  base: mode === "production" ? "/complex/" : "/",
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    // 真实分包已完成关键优化：admin 32 个页面经 React.lazy 路由级切分，
    // AdminApp 从 ~224kB 降到 ~19kB，首屏不再一次性加载全部页面代码。
    // 剩余最大单块 vendor-antd（antd 框架本体）约 586kB，是框架固有体积；
    // 实测在 rolldown 的 manualChunks 下进一步剥离 rc-*/dayjs 会触发反向合并、
    // 生成更大的单块（~1MB），属退化，故不强拆。
    // 阈值设为 650kB：覆盖 antd 框架块、消除噪音警告，又能在未来出现新的
    // 真实回归（如某业务块异常膨胀）时仍然报警。
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      input: {
        main: "index.html",
        ticketing: "ticketing/index.html",
        bar: "bar/index.html",
        amusement: "amusement/index.html",
      },
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/react") || id.includes("node_modules/react-dom")) return "vendor-react";
          if (id.includes("node_modules/@ant-design/icons")) return "vendor-antd-icons";
          if (id.includes("node_modules/antd") || id.includes("node_modules/@ant-design")) return "vendor-antd";
          if (id.includes("node_modules")) return "vendor";
        },
      },
    },
  },
}));
