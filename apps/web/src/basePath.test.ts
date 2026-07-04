import { afterEach, describe, expect, it, vi } from "vitest";
import { selectShell, stripBase, withBase } from "./basePath";

afterEach(() => {
  vi.unstubAllEnvs();
});

describe("selectShell（入口壳选择，入参已 stripBase）", () => {
  it("/admin* → 后台", () => {
    expect(selectShell("/admin")).toBe("admin");
    expect(selectShell("/admin/customers")).toBe("admin");
  });
  it("根路径与客人入口 → 客人端", () => {
    expect(selectShell("/")).toBe("customer");
    expect(selectShell("/customer")).toBe("customer");
    expect(selectShell("/ticketing")).toBe("customer");
    expect(selectShell("/bar")).toBe("customer");
    expect(selectShell("/amusement")).toBe("customer");
  });
});

describe("子路径部署（BASE_URL=/complex/）", () => {
  it("/complex/admin → stripBase → /admin → 后台", () => {
    vi.stubEnv("BASE_URL", "/complex/");
    expect(stripBase("/complex/admin")).toBe("/admin");
    expect(selectShell(stripBase("/complex/admin"))).toBe("admin");
  });
  it("/complex/ → stripBase → / → 客人端", () => {
    vi.stubEnv("BASE_URL", "/complex/");
    expect(stripBase("/complex/")).toBe("/");
    expect(selectShell(stripBase("/complex/"))).toBe("customer");
  });
  it("withBase 补前缀（pushState 不跳出 /complex）", () => {
    vi.stubEnv("BASE_URL", "/complex/");
    expect(withBase("/admin/customers")).toBe("/complex/admin/customers");
    expect(withBase("/?page=buy")).toBe("/complex/?page=buy");
  });
});

describe("dev 根路径部署（BASE_URL=/，直接访问无前缀）", () => {
  it("/admin 原样，选后台", () => {
    vi.stubEnv("BASE_URL", "/");
    expect(stripBase("/admin")).toBe("/admin");
    expect(selectShell(stripBase("/admin"))).toBe("admin");
  });
  it("withBase 原样返回（无前缀）", () => {
    vi.stubEnv("BASE_URL", "/");
    expect(withBase("/admin")).toBe("/admin");
  });
});
