// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 R 段 152-R4（第十八批裁决 34「销售可以通过微信等发送邀请函，客户点击后确定自己是订台人还是参与者」；
// BSM:1036「对订台签发邀请函二维码（注明酒吧、卡座、可进人数）……邀请函可转发给朋友」）：
// 签发后在「卡座与订台」页显示可转发的邀请链接与二维码，可一键复制，由销售通过微信等发给订台客人。
function renderPage() {
  const api = {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/bar/tables") return [{ id: "table_1", barId: "bar1", tableName: "A01 卡座", capacity: 4 }];
      if (url === "/api/v1/bar/reservations") return [
        { id: "resv_active", barId: "bar1", tableId: "table_1", guestCount: 3, status: "reserved", depositStatus: "waived",
          invitationStatus: "active", invitationToken: "inv_abc123", invitationSignature: "0f1e2d3c", invitationCapacity: 3 },
        { id: "resv_none", barId: "bar1", tableId: "table_1", guestCount: 2, status: "reserved", depositStatus: "waived", invitationStatus: "none" },
      ];
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = { api, user: { id: "sales", username: "销售", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
  render(<BarTableSessionsPage {...props} />);
}

describe("卡座与订台页：签发后复制邀请链接", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    Reflect.deleteProperty(navigator, "clipboard");
  });

  it("已签发的订台显示邀请链接、二维码与「可进 3 人」，复制按钮把同一链接写进剪贴板", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { value: { writeText }, configurable: true });
    renderPage();

    await waitFor(() => expect(screen.queryByTestId("bar-reservation-row-resv_active")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-reservation-select-resv_active"));
    const expected = `https://${window.location.host}/c/invitation?token=inv_abc123&sig=0f1e2d3c&table_id=table_1&capacity=3`;
    await waitFor(() => expect(screen.queryByTestId("bar-invitation-link")?.textContent).toBe(expected));
    const panel = screen.getByTestId("bar-invitation-share").textContent ?? "";
    expect(panel).toContain("A01 卡座");
    expect(panel).toContain("可进 3 人");
    expect(screen.getByTestId("bar-invitation-share").querySelector("svg")).toBeTruthy();

    fireEvent.click(screen.getByTestId("bar-invitation-copy"));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(expected));
    await waitFor(() => expect(screen.queryByText("已复制，可通过微信等发给订台客人")).toBeTruthy());
  });

  it("未签发的订台不出链接，提示签发后可复制", async () => {
    renderPage();

    await waitFor(() => expect(screen.queryByTestId("bar-reservation-row-resv_none")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-reservation-select-resv_none"));
    await waitFor(() => expect(screen.queryByText("签发后可在此复制邀请链接")).toBeTruthy());
    expect(screen.queryByTestId("bar-invitation-link")).toBeNull();
    expect(screen.queryByTestId("bar-invitation-copy")).toBeNull();
  });
});
