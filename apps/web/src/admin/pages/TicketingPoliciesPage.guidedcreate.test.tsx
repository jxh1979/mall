import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-2：新建票种改为分步弹窗（基本信息→定价→确认）。
// 覆盖入口按钮 → 步进 → 定价步校验 → 确认页发布 的关键路径，原 testid（ticket-name/三档价/ticket-publish）仍在弹窗内。
function makeApi(postSpy: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async () => ({ items: [] }) as unknown),
    post: postSpy,
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>) {
  const props: AdminPageProps = {
    api: makeApi(postSpy),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
}

describe("TicketingPoliciesPage 引导式新建票种", () => {
  afterEach(() => vi.clearAllMocks());

  it("入口按钮打开弹窗 → 名称默认→定价缺节假日报错→补齐→确认发布", async () => {
    const postSpy = vi.fn(async (path: string) => {
      if (path.includes("/ticket-types") && !path.includes("/publish")) return { id: "tt-new" } as unknown;
      return {} as unknown;
    });
    renderPage(postSpy);

    await waitFor(() => expect(screen.getByTestId("ticket-create-open")).toBeTruthy());
    // 未打开时弹窗不渲染。
    expect(screen.queryByTestId("ticket-name")).toBeNull();

    fireEvent.click(screen.getByTestId("ticket-create-open"));
    // 步1 基本信息（名称有默认值）→ 下一步。
    expect(screen.getByTestId("ticket-name")).toBeTruthy();
    fireEvent.click(screen.getByTestId("ticket-policy-next"));

    // 步2 定价：只填平日/周末，缺节假日 → 下一步触发校验，弹窗留开显错。
    fireEvent.change(screen.getByTestId("ticket-weekday-price"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("ticket-weekend-price"), { target: { value: "120" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    expect(screen.getByTestId("ticket-policy-error").textContent).toContain("节假日");

    // 补齐节假日 → 进确认页 → 发布。
    fireEvent.change(screen.getByTestId("ticket-holiday-price"), { target: { value: "150" } });
    fireEvent.click(screen.getByTestId("ticket-policy-next"));
    fireEvent.click(screen.getByTestId("ticket-publish"));

    await waitFor(() => {
      const posted = postSpy.mock.calls.map((c) => String(c[0]));
      expect(posted.some((p) => p.endsWith("/ticketing/ticket-types"))).toBe(true);
      expect(posted.some((p) => p.includes("/publish"))).toBe(true);
    });
    // 发布成功后弹窗关闭。
    await waitFor(() => expect(screen.queryByTestId("ticket-name")).toBeNull());
  });
});
