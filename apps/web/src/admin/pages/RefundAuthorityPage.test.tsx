import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { RefundAuthorityPage } from "./RefundAuthorityPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const LIMITS = [
  { id: "lim-1", subjectType: "staff", subjectId: "u-9", refundDomain: "ticket", limitAmountCent: 50000, status: "active" },
];
const RULES = [
  { id: "rule-1", refundDomain: "bar", ruleType: "expired_unused", enabled: true, status: "active" },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/refund-authority/limits")) return Promise.resolve(LIMITS);
    if (path.includes("/refund-authority/rules")) return Promise.resolve(RULES);
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver()): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient = makeApi()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<RefundAuthorityPage {...props} />);
}

describe("RefundAuthorityPage 零英文枚举", () => {
  afterEach(() => vi.restoreAllMocks());

  it("额度表主体/退款域渲染中文，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("refund-limit-lim-1")).toBeTruthy());
    const row = screen.getByTestId("refund-limit-lim-1").textContent ?? "";
    expect(row).toContain("普通员工");
    expect(row).toContain("票务");
    expect(row).not.toContain("staff");
    expect(row).not.toContain("ticket");
  });

  it("规则表退款域/规则类型渲染中文，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("refund-rule-rule-1")).toBeTruthy());
    const row = screen.getByTestId("refund-rule-rule-1").textContent ?? "";
    expect(row).toContain("酒吧");
    expect(row).toContain("到期未用");
    expect(row).not.toContain("bar");
    expect(row).not.toContain("expired_unused");
  });
});

describe("RefundAuthorityPage 行明细抽屉（整行可点）", () => {
  afterEach(() => vi.restoreAllMocks());

  it("点额度行 -> 抽屉含员工 id + 金额¥ + 中文主体/退款域；Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("refund-limit-lim-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("refund-limit-lim-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("u-9");
    expect(drawer).toContain("¥500.00");
    expect(drawer).toContain("普通员工");
    expect(drawer).toContain("票务");
    expect(drawer).not.toContain("staff");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点规则行 -> 抽屉含中文退款域/规则类型，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("refund-rule-rule-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("refund-rule-rule-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("酒吧");
    expect(drawer).toContain("到期未用");
    expect(drawer).not.toContain("expired_unused");
  });

  it("规则行点停用/启用按钮触发切换、不弹抽屉（stopPropagation）", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("refund-rule-toggle-rule-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("refund-rule-toggle-rule-1"));
    expect(api.post).toHaveBeenCalledWith(expect.stringContaining("/refund-authority/rules"), expect.objectContaining({ enabled: false }));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
