// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

vi.mock("./pages/OverviewPage", () => ({ OverviewPage: () => <section data-testid="overview-stub">总控</section> }));
vi.mock("./pages/FinanceArApLedgerPage", () => ({ FinanceArApLedgerPage: () => <section data-testid="ledger-stub">台账</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

type Call = { url: string; body: Record<string, unknown> | null };

const traces: FormData[] = [];

function mount(codes: string[], path = "/admin/overview") {
  const pageViews: Call[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    let data: unknown = [];
    if (url.endsWith("/api/v1/access/effective-permissions")) data = { grants: [], denies: [], wildcard: true };
    if (url.endsWith("/api/v1/security/watermarks/trace")) {
      traces.push(init?.body as FormData);
      data = { code: "K7MX3PQR", account_id: "usr-7", username: "张三账号", device_no: "WBABCDEFGHJKMN", network: "10.0.0.8", kind: "page",
        page_id: "ADM-FIN-001", export_file_id: null, opened_at: "2026-10-29T02:03:04Z" };
    }
    if (url.endsWith("/api/v1/security/page-views")) {
      pageViews.push({ url, body: init?.body ? JSON.parse(String(init.body)) : null });
      data = { code: codes[pageViews.length - 1], opened_at: "2026-10-29T10:00:00Z" };
    }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  }));
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "ops-1", username: "ops", accountType: "platform_admin" }, sessionId: "session-1" }));
  window.history.replaceState({}, "", path);
  render(<AdminApp />);
  return pageViews;
}

// PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹；暗纹可反向推算……哪个账号、在哪一台设备上、于什么时间打开的」。
describe("在线页面暗纹", () => {
  it("进入页面即登记打开并平铺暗纹码，叠在最上层且不挡操作；切路由按新页面再登记一次", async () => {
    const pageViews = mount(["K7MX3PQR", "Z2W9HNTB"]);

    await waitFor(() => expect(screen.queryByTestId("admin-watermark")?.textContent).toContain("K7MX3PQR"));
    expect(pageViews[0]!.body).toMatchObject({ page_id: "ADM-OV-001", sensitive: false, object_refs: [] });
    expect(pageViews[0]!.body!.idempotency_key).toBeTruthy();
    const overlay = screen.getByTestId("admin-watermark");
    expect(overlay.parentElement).toBe(document.body);
    expect(overlay.style.pointerEvents).toBe("none");
    expect(Number(overlay.style.zIndex)).toBeGreaterThan(1000);

    // jsdom 的元素没有 scrollTo；主壳换页时会调用。
    if (typeof HTMLElement.prototype.scrollTo !== "function") HTMLElement.prototype.scrollTo = () => undefined;
    fireEvent.click(screen.getByTestId("admin-nav-ADM-FIN-001"));

    await waitFor(() => expect(screen.queryByTestId("admin-watermark")?.textContent).toContain("Z2W9HNTB"));
    expect(pageViews).toHaveLength(2);
    expect(pageViews[1]!.body).toMatchObject({ page_id: "ADM-FIN-001", sensitive: false });
  });

  it("拿不到暗纹码时页面照常显示、不叠暗纹", async () => {
    const pageViews = mount([]);
    await waitFor(() => expect(pageViews).toHaveLength(1));
    await waitFor(() => expect(screen.queryByTestId("overview-stub")).not.toBeNull());
    expect(screen.queryByTestId("admin-watermark")).toBeNull();
  });

  it("暗纹反查页：上传截屏后显示打开该页面的账号、设备、网络与北京时间", async () => {
    mount(["K7MX3PQR"], "/admin/security/watermark-trace");

    // 懒加载页面，整套并跑时首个 chunk 编译较慢。
    await waitFor(() => expect(screen.queryByTestId("watermark-trace-page")).not.toBeNull(), { timeout: 3000 });
    const shot = new File([new Uint8Array([137, 80, 78, 71])], "shot.png", { type: "image/png" });
    fireEvent.change(screen.getByTestId("watermark-trace-file"), { target: { files: [shot] } });
    fireEvent.click(screen.getByTestId("watermark-trace-submit"));

    await waitFor(() => expect(screen.queryByTestId("watermark-trace-result")).not.toBeNull());
    expect((traces.at(-1)!.get("file") as File).name).toBe("shot.png");
    const result = screen.getByTestId("watermark-trace-result").textContent;
    expect(result).toContain("张三账号");
    expect(result).toContain("WBABCDEFGHJKMN");
    expect(result).toContain("10.0.0.8");
    expect(result).toContain("应收应付台账");
    expect(result).toContain("10:03:04");
  });
});
