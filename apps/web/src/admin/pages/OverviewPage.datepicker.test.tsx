import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope({ items: [], total: 0 }))));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<OverviewPage {...props} />);
}

describe("OverviewPage 日期控件零英文（CnDatePicker）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("起始/结束用中文 CnDatePicker，无原生 type=date 残留", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("overview-date-from")).toBeTruthy());
    const from = screen.getByTestId("overview-date-from");
    const to = screen.getByTestId("overview-date-to");
    expect(from.tagName).toBe("BUTTON");
    expect(to.tagName).toBe("BUTTON");
    expect(document.querySelector('input[type="date"]')).toBeNull();
    expect(document.querySelector('input[type="month"]')).toBeNull();
  });
});
