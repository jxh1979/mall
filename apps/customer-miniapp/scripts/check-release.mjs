// 文件版本：V0.01
// 小程序上传／发布前检查（开发者工具上传前由 project.config.json 的 beforeUpload 调用；用 miniprogram-ci 上传时先跑 npm run check-release）：
// 正式 AppID、正式接口域名与客人链接域名属外部资料，未到位时逐项列出中文原因并非零退出，不出带开发值的正式版本：
// 游客 touristappid、http 接口、私网或本机地址、本端版本与 package.json 不一致一律拒绝。
import fs from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";

const PRIVATE_OR_LOCAL = /^(localhost|127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|169\.254\.|\[?::1\]?)/i;

export function releaseProblems({ appid, apiBaseUrl, linkHost, clientVersion, packageVersion }) {
  const problems = [];
  if (!appid || appid === "touristappid") problems.push("AppID 未配置（仍为游客 touristappid），不能上传");
  const apiHost = /^https:\/\/([^/?#]+)/i.exec(apiBaseUrl ?? "")?.[1];
  if (!apiHost) problems.push(`接口地址必须是 https：${apiBaseUrl || "（空）"}`);
  else if (PRIVATE_OR_LOCAL.test(apiHost)) problems.push(`接口地址是私网或本机地址：${apiBaseUrl}`);
  if (!linkHost || PRIVATE_OR_LOCAL.test(linkHost)) problems.push(`客人链接主机未配置或是私网、本机地址：${linkHost || "（空）"}`);
  if (clientVersion !== packageVersion) problems.push(`config.js 的本端版本 ${clientVersion} 与 package.json 的 version ${packageVersion} 不一致`);
  return problems;
}

/** 读本工程现行配置：project.private.config.json 的 appid 优先（开发者工具同此口径）。 */
export function currentReleaseInput(root) {
  const require = createRequire(import.meta.url);
  const readJson = (name) => {
    const file = path.join(root, name);
    return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, "utf8")) : {};
  };
  const config = require(path.join(root, "miniprogram", "config.js"));
  return {
    appid: readJson("project.private.config.json").appid ?? readJson("project.config.json").appid,
    apiBaseUrl: config.apiBaseUrl,
    linkHost: config.linkHost,
    clientVersion: config.clientVersion,
    packageVersion: readJson("package.json").version,
  };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
  const problems = releaseProblems(currentReleaseInput(root));
  if (problems.length) {
    console.error(`[customer-miniapp] 发布检查不通过（${problems.length} 项），不能上传：`);
    for (const problem of problems) console.error(`  - ${problem}`);
    process.exit(1);
  }
  console.log("[customer-miniapp] 发布检查通过");
}
