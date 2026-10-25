// 文件版本：V0.01
// 上传前检查：正式 AppID、正式接口域名与客人链接域名未到位时拒绝上传（游客 touristappid、http、私网或本机地址、本端版本不一致）。
import fs from "node:fs";
import path from "node:path";
import { expect, test } from "vitest";

import { currentReleaseInput, releaseProblems } from "../scripts/check-release.mjs";
import { APP_ROOT } from "./helpers.js";

test("releaseCheckRejectsTouristAppIdAndDevValues", () => {
  const committed = JSON.parse(fs.readFileSync(path.join(APP_ROOT, "project.config.json"), "utf8"));
  const problems = releaseProblems({ ...currentReleaseInput(APP_ROOT), appid: committed.appid }).join("\n");
  expect(problems).toContain("touristappid");
  expect(problems).toContain("接口地址是私网或本机地址：https://192.168.88.160");
  expect(problems).toContain("客人链接主机未配置或是私网、本机地址：192.168.88.160");
  const http = releaseProblems({ appid: "wx0123456789abcdef", apiBaseUrl: "http://api.example.com", linkHost: "localhost", clientVersion: "0.1.0", packageVersion: "0.2.0" });
  expect(http.join("\n")).toContain("接口地址必须是 https");
  expect(http.join("\n")).toContain("localhost");
  expect(http.join("\n")).toContain("不一致");
});

test("releaseCheckPassesFormalValues", () => {
  expect(releaseProblems({ appid: "wx0123456789abcdef", apiBaseUrl: "https://api.example.com", linkHost: "c.example.com", clientVersion: "0.1.0",
    packageVersion: "0.1.0" })).toEqual([]);
});
