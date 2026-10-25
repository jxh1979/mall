// 文件版本：V0.01
// 客人业务模块是纯 TS：单测跑在 node 环境，不提供 DOM。
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
    globals: false,
    restoreMocks: true,
  },
});
