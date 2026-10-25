// 文件版本：V0.01
// 小程序构建：把三端共用的客人业务模块 packages/customer-core（纯 TS）编译成小程序可直接 require 的 CommonJS（miniprogram/core/），
// 并把纯 JS 的二维码编码库 qrcode-generator 拷到 miniprogram/vendor/。小程序不用 web-view、不打包网页，页面资源全在本工程里。
// 上传前与开发者工具编译前都先跑本脚本（project.config.json 的 scripts）。
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const coreDir = path.join(root, "miniprogram", "core");
const vendorDir = path.join(root, "miniprogram", "vendor");

fs.rmSync(coreDir, { recursive: true, force: true });
execFileSync(process.execPath, [require.resolve("typescript/bin/tsc"), "-p", path.join(root, "tsconfig.core.json")], { cwd: root, stdio: "inherit" });
fs.mkdirSync(vendorDir, { recursive: true });
fs.copyFileSync(require.resolve("qrcode-generator"), path.join(vendorDir, "qrcode.js"));
console.log("[customer-miniapp] 已编译 miniprogram/core/ 与 miniprogram/vendor/qrcode.js");
