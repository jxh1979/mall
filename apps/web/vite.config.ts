import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8080";

export default defineConfig({
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
});
