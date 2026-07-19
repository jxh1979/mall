import { describe, expect, it, vi } from "vitest";
import { loadSecondary, loadSecondaryIfPermitted } from "./useAdminResource";
import { ApiError } from "../../shared/apiError";

const apiError = (status: number) =>
  new ApiError("x", { detail: { path: "/p", status, requestId: null, raw: "" } });

describe("loadSecondaryIfPermitted (根因修：无权限不请求)", () => {
  it("无权限 → 不调用 load、无提示、回退 fallback", async () => {
    const load = vi.fn(async () => ["should-not-run"]);
    const res = await loadSecondaryIfPermitted(false, load, [] as string[]);
    expect(load).not.toHaveBeenCalled();
    expect(res).toEqual({ data: [], notice: null });
  });

  it("有权限且成功 → 走 loadSecondary，返回数据、无提示", async () => {
    const res = await loadSecondaryIfPermitted(true, async () => ["ok"], [] as string[]);
    expect(res).toEqual({ data: ["ok"], notice: null });
  });

  it("有权限但真失败（500）→ 保留区块级降级提示（不吞成静默）", async () => {
    const res = await loadSecondaryIfPermitted(true, async () => { throw apiError(500); }, [] as string[]);
    expect(res.data).toEqual([]);
    expect(res.notice).toBe("该数据加载失败，可稍后重试，不影响本页其他内容");
  });
});

describe("loadSecondary", () => {
  it("403 → 无权提示", async () => {
    const res = await loadSecondary(async () => { throw apiError(403); }, [] as string[]);
    expect(res.notice).toBe("当前账号无权查看该数据");
  });

  it("null 结果回退 fallback", async () => {
    const res = await loadSecondary(async () => null as unknown as string[], ["fallback"]);
    expect(res).toEqual({ data: ["fallback"], notice: null });
  });
});
