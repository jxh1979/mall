import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneFeeRecipientsPage } from "./MerchantZoneFeeRecipientsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const RECIPIENTS = [
  { id: "fr-1", fee_event: "rent", recipient_name: "王店长", recipient_phone: "13800000001", status: "active" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function okEnvelope(): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { id: "fr-new" } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let fetchMock: ReturnType<typeof vi.fn>;

function makeApi(): AdminApi {
  fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (init?.method === "POST") return Promise.resolve(okEnvelope());
    if (url.includes("fee-notification-recipients")) return Promise.resolve(envelope(RECIPIENTS));
    return Promise.resolve(envelope([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(): void {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MerchantZoneFeeRecipientsPage {...props} />);
}

// 引导式流程 phase-6：新增接收人从内联表单改为「＋新增接收人」按钮 → 分步引导弹窗。
describe("MerchantZoneFeeRecipientsPage 引导式新增接收人", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/fee-recipients");
  });

  it("入口→步进→提交携带费用事件/姓名/手机号", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipient-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipient-create-open"));
    fireEvent.change(screen.getByTestId("merchant-zone-fee-recipient-event"), { target: { value: "utilities" } });
    fireEvent.change(screen.getByTestId("merchant-zone-fee-recipient-name"), { target: { value: "李工" } });
    fireEvent.change(screen.getByTestId("merchant-zone-fee-recipient-phone"), { target: { value: "13800000002" } });
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipient-guided-next"));
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipient-save"));
    await waitFor(() => {
      const post = fetchMock.mock.calls.find((c) => c[1]?.method === "POST" && String(c[0]).includes("fee-notification-recipients"));
      expect(post).toBeTruthy();
      const body = JSON.parse(String(post?.[1]?.body));
      expect(body).toMatchObject({ fee_event: "utilities", recipient_name: "李工", recipient_phone: "13800000002" });
    });
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-fee-recipient-guided")).toBeNull());
  });

  it("缺姓名 → 步内校验拦截，不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipient-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipient-create-open"));
    // 只填手机号，不填姓名
    fireEvent.change(screen.getByTestId("merchant-zone-fee-recipient-phone"), { target: { value: "13800000002" } });
    fireEvent.click(screen.getByTestId("merchant-zone-fee-recipient-guided-next"));
    await waitFor(() => expect(screen.getByTestId("merchant-zone-fee-recipient-guided-error").textContent).toContain("姓名"));
    expect(screen.queryByTestId("merchant-zone-fee-recipient-save")).toBeNull();
    const posted = fetchMock.mock.calls.some((c) => c[1]?.method === "POST" && String(c[0]).includes("fee-notification-recipients"));
    expect(posted).toBe(false);
  });
});
