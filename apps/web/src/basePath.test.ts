// @vitest-environment jsdom
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
  it("根路径与客人入口 → 客人端", () => {
    expect(selectShell("/")).toBe("customer");
    expect(selectShell("/customer")).toBe("customer");
    expect(selectShell("/ticketing")).toBe("customer");
  });
});

describe("stripBase（纯函数自检前缀）", () => {
  it("/complex/admin → /admin → 后台", () => {
    expect(stripBase("/complex/admin")).toBe("/admin");
    expect(selectShell(stripBase("/complex/admin"))).toBe("admin");
  });
  it("/complex/ → / → 客人端", () => {
    expect(stripBase("/complex/")).toBe("/");
    expect(selectShell(stripBase("/complex/"))).toBe("customer");
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
