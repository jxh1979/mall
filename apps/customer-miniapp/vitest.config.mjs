// 文件版本：V0.01
// 小程序页面与组件单测：miniprogram-simulate（j-component）在 jsdom 里加载 WXML/JS 跑交互，wx 全局用测试替身（test/helpers.js）。
// 只表示源码逻辑与模板可执行，不等于微信开发者工具编译通过或真机预览。
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    include: ["test/**/*.test.js"],
    globals: false,
    restoreMocks: true,
  },
});
