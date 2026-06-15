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
    },
  },
});
