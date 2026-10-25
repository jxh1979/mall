// @vitest-environment jsdom
// 文件版本：V0.01
import { afterEach, describe, expect, it } from "vitest";
import { selectShell, stripBase, withBase } from "./basePath";

const realLocation = window.location;
afterEach(() => {
  Object.defineProperty(window, "location", { configurable: true, value: realLocation });
});
function setPath(pathname: string): void {
  Object.defineProperty(window, "location", { configurable: true, value: { pathname } });
}

describe("selectShell（入口壳选择，入参已 stripBase）", () => {
  it("/admin* → 后台", () => {
    expect(selectShell("/admin")).toBe("admin");
    expect(selectShell("/admin/customers")).toBe("admin");
  });
  it("/dealer* → 独立经销商门户（第十一端）", () => {
    expect(selectShell("/dealer")).toBe("dealer");
    expect(selectShell("/dealer/orders")).toBe("dealer");
    expect(selectShell(stripBase("/complex/dealer"))).toBe("dealer");
  });
  // PGM「Web 前端（业务人员，客人无网页端）」：根路径、原客人入口与浏览器里打开的客人链接只给客人引导提示页。
  it("rootAndUnknownPathsRenderCustomerNoticeNotCustomerApp", () => {
    for (const path of ["/", "/customer", "/ticketing", "/bar", "/amusement", "/c/table", "/anything"]) {
      expect(selectShell(path), path).toBe("notice");
    }
  });
});

describe("stripBase（纯函数自检前缀）", () => {
  it("/complex/admin → /admin → 后台", () => {
    expect(stripBase("/complex/admin")).toBe("/admin");
    expect(selectShell(stripBase("/complex/admin"))).toBe("admin");
  });
  it("/complex/ → / → 客人引导提示页", () => {
    expect(stripBase("/complex/")).toBe("/");
    expect(selectShell(stripBase("/complex/"))).toBe("notice");
  });
  it("dev 根路径 /admin 原样", () => {
    expect(stripBase("/admin")).toBe("/admin");
  });
});

describe("withBase（按当前部署补前缀）", () => {
  it("生产 /complex 下补前缀", () => {
    setPath("/complex/admin");
    expect(withBase("/admin/customers")).toBe("/complex/admin/customers");
    expect(withBase("/?page=buy")).toBe("/complex/?page=buy");
  });
  it("dev 根路径原样返回", () => {
    setPath("/admin");
    expect(withBase("/admin")).toBe("/admin");
  });
});
