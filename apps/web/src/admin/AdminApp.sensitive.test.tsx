// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Modal } from "antd";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

vi.mock("./pages/OverviewPage", () => ({ OverviewPage: () => <section data-testid="overview-stub">总控</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); delete document.body.dataset.sensitive; });

type Body = Record<string, unknown>;

// 客人账号管理页（ADM-CUS-001）载入时读的黑名单待批申请：一行，客人 cus-7。
const REQUEST = { id: "blr-1", customer_id: "cus-7", project_id: "prj-1", action: "list", reason: "闹事", status: "pending", requested_at: "2026-10-29T01:00:00Z" };

function mount(path: string) {
  const pageViews: Body[] = [];
  let openFirst: (() => void) | null = null;
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    let data: unknown = [];
    if (url.endsWith("/api/v1/access/effective-permissions")) data = { grants: [], denies: [], wildcard: true };
    if (url.includes("/api/v1/customer-blacklist-requests")) data = [REQUEST];
    if (url.includes("/api/v1/customer-churn-alert-setting")) data = { threshold: 3, min_threshold: 1, max_threshold: 9 };
    if (url.includes("/api/v1/customer-restriction-settings/")) data = { duration_days: 90, min_days: 1, max_days: 365 };
    if (url.endsWith("/api/v1/security/page-views")) {
      pageViews.push(JSON.parse(String(init?.body)));
      data = { code: `CODE000${pageViews.length}`, opened_at: "2026-10-29T10:00:00Z" };
      // 第一次登记（打开页面）先挂起，看页面内容是否等它。
      if (pageViews.length === 1) await new Promise<void>((resolve) => { openFirst = resolve; });
    }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  }));
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "ops-1", username: "ops", accountType: "platform_admin", organizationId: "prj-1" }, sessionId: "session-1" }));
  window.history.replaceState({}, "", path);
  render(<AdminApp />);
  return { pageViews, open: () => openFirst?.() };
}

function blocked(target: EventTarget, event: Event) {
  target.dispatchEvent(event);
  return event.defaultPrevented;
}

// PGM:687「敏感页上的文字不能被选中、不能被复制，也不提供页面另存与打印……打开敏感页这件事本身也要留痕，记下是谁打开的、什么时间打开的、
// 打开的是哪一个页面、这一次看到的是哪些对象（哪个客人、哪个商户、哪些单据）；这条留痕不提供关闭开关」。
describe("敏感页", () => {
  it("先登记打开再显示内容，禁复制、剪切、右键、拖拽与另存打印快捷键（含弹窗内），载入后按已显示的行报看到的对象", async () => {
    const { pageViews, open } = mount("/admin/customers/accounts");

    await waitFor(() => expect(pageViews).toHaveLength(1));
    expect(pageViews[0]).toMatchObject({ page_id: "ADM-CUS-001", sensitive: true, object_refs: [] });
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByTestId("customer-accounts-page")).toBeNull();

    open();
    await waitFor(() => expect(screen.queryByTestId("customer-blacklist-request-blr-1")).not.toBeNull(), { timeout: 3000 });
    expect(document.body.dataset.sensitive).toBe("true");
    const text = screen.getByTestId("customer-accounts-page");
    for (const type of ["copy", "cut", "contextmenu", "dragstart"]) {
      expect(blocked(text, new Event(type, { bubbles: true, cancelable: true }))).toBe(true);
    }
    expect(blocked(text, new KeyboardEvent("keydown", { key: "p", ctrlKey: true, bubbles: true, cancelable: true }))).toBe(true);
    expect(blocked(text, new KeyboardEvent("keydown", { key: "s", metaKey: true, bubbles: true, cancelable: true }))).toBe(true);
    expect(blocked(text, new KeyboardEvent("keydown", { key: "a", bubbles: true, cancelable: true }))).toBe(false);
    // antd 弹窗渲染在 body 下、不在主内容区里，同样拦住。
    render(<Modal open title="明细"><p data-testid="modal-text">13800000000</p></Modal>);
    expect(blocked(screen.getByTestId("modal-text"), new Event("copy", { bubbles: true, cancelable: true }))).toBe(true);

    await waitFor(() => expect(pageViews.some((body) => (body.object_refs as unknown[]).length > 0)).toBe(true));
    const viewed = pageViews.find((body) => (body.object_refs as unknown[]).length > 0)!;
    expect(viewed).toMatchObject({ page_id: "ADM-CUS-001", sensitive: true });
    expect(viewed.object_refs).toEqual(expect.arrayContaining([
      { object_type: "customer_blacklist_request", object_id: "blr-1" },
      { object_type: "customer", object_id: "cus-7" },
    ]));
    expect(viewed.object_refs).toHaveLength(2);
    // 报看到的对象拿到的码不替换已显示的暗纹。
    expect(screen.getByTestId("admin-watermark").textContent).toContain("CODE0001");
  });

  it("登记打开失败时不显示敏感页内容", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/v1/security/page-views")) {
        return new Response(JSON.stringify({ success: false, code: "DEVICE_NOT_REGISTERED", message: "请重新登录后再打开" }), { status: 409, headers: { "content-type": "application/json" } });
      }
      const data = url.endsWith("/api/v1/access/effective-permissions") ? { grants: [], denies: [], wildcard: true } : [];
      return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
    }));
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "ops-1", username: "ops", accountType: "platform_admin" }, sessionId: "session-1" }));
    window.history.replaceState({}, "", "/admin/customers/accounts");
    render(<AdminApp />);

    await waitFor(() => expect(screen.queryByTestId("global-error")).not.toBeNull());
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByTestId("customer-accounts-page")).toBeNull();
  });

  it("普通页不打敏感标记、不拦复制", async () => {
    const { pageViews, open } = mount("/admin/overview");
    await waitFor(() => expect(pageViews).toHaveLength(1));
    open();
    await waitFor(() => expect(screen.queryByTestId("overview-stub")).not.toBeNull());
    expect(pageViews[0]).toMatchObject({ page_id: "ADM-OV-001", sensitive: false });
    expect(document.body.dataset.sensitive).toBeUndefined();
    expect(blocked(screen.getByTestId("overview-stub"), new Event("copy", { bubbles: true, cancelable: true }))).toBe(false);
  });
});
