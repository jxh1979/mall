import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ORDERS = [{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }];
const ENTITLEMENTS = [{ id: "ent-1", entitlementCode: "ENT-0001" }];
const PHOTO_BASE64 = "aGVsbG8=";

function makeApi(getImpl: (url: string) => Promise<unknown>, post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn().mockImplementation(getImpl),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient, setActionResult = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<TicketOrdersPage {...props} />);
  return setActionResult;
}

function baseGet(url: string): Promise<unknown> {
  if (url.includes("/ticketing/orders")) return Promise.resolve(ORDERS);
  if (url.includes("/issued-entitlements")) return Promise.resolve(ENTITLEMENTS);
  if (url.includes("/verifications")) return Promise.resolve([]);
  return Promise.resolve([]);
}

describe("TicketOrdersPage 购票人脸照比对(C2)", () => {
  it("核销预检扫到权益后取脸照并渲染成可比对的图像", async () => {
    const post = vi.fn().mockResolvedValue({ allowed: true, entitlement: { id: "ent-1" } });
    const get = vi.fn().mockImplementation((url: string) =>
      url.includes("/face-photo")
        ? Promise.resolve({ photoBase64: PHOTO_BASE64, contentType: "image/jpeg" })
        : baseGet(url));
    const api = makeApi(get, post);
    const setActionResult = renderPage(api);

    await screen.findByTestId("ticket-verification-preview");
    await userEvent.click(screen.getByTestId("ticket-verification-preview"));

    await waitFor(() =>
      expect(get).toHaveBeenCalledWith("/api/v1/ticketing/issued-entitlements/ent-1/face-photo"));
    const image = await screen.findByTestId("ticket-face-photo-image");
    expect(image.style.backgroundImage).toContain(`data:image/jpeg;base64,${PHOTO_BASE64}`);

    // 照片不得混进操作结果（那是会被展示/记录的东西）。
    for (const call of setActionResult.mock.calls) {
      expect(JSON.stringify(call[0])).not.toContain(PHOTO_BASE64);
    }
    // 也不得落任何浏览器存储。
    expect(JSON.stringify(window.localStorage)).not.toContain(PHOTO_BASE64);
    expect(JSON.stringify(window.sessionStorage)).not.toContain(PHOTO_BASE64);

    await userEvent.click(screen.getByTestId("ticket-face-photo-clear"));
    await waitFor(() => expect(screen.queryByTestId("ticket-face-photo-image")).toBeNull());
  });

  it("本票没有脸照时给出中文说明，不打断核销", async () => {
    const post = vi.fn().mockResolvedValue({ allowed: true, entitlement: { id: "ent-1" } });
    const get = vi.fn().mockImplementation((url: string) =>
      url.includes("/face-photo") ? Promise.reject(new Error("FACE_PHOTO_NOT_FOUND")) : baseGet(url));
    renderPage(makeApi(get, post));

    await screen.findByTestId("ticket-verification-preview");
    await userEvent.click(screen.getByTestId("ticket-verification-preview"));

    const missing = await screen.findByTestId("ticket-face-photo-missing");
    expect(missing.textContent).toContain("没有留存购票人脸照");
  });
});
