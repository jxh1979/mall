import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": "http://localhost:4000",
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
