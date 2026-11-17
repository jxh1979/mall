// 文件版本：V0.01
// 第十八批 AP 段第 254 条 254-R6（BSM:1171「管理存酒取酒、转让、作废、争议和异常处理。」；第十八批裁决 58「门店主管核实后二选一：
// 争议不成立，恢复为存酒中、照常可取；争议成立，走现有存酒作废……」）：存酒中可登记争议（原因必填、每次登记幂等键不同）；
// 争议中且持门店主管码可点「争议不成立，恢复存酒」发 dispute-dismissal；无门店主管码时恢复与作废都不可点。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
  { id: "d-1", savedNo: "SW-0002", customerName: "客户乙", itemName: "红酒", remainingQuantity: 1, status: "disputed", createdAt: "2026-07-02" },
];

const SUPERVISOR = ["bar.saved_bottle.manage", "inventory.high_value.review"];

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") || path.endsWith("/renewals") ? [] : ROWS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ id: "w-1", status: "disputed" })),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, grants: string[]) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "店长", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: perms(grants),
  };
  render(<StoredWinePage {...props} />);
}

// 本页单选框回落首行：先等首行补存记录请求发出（选中已落定），再点争议中那一行并等它的被动 effect 落定。
async function selectDisputed(api: AdminApiClient) {
  await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
  fireEvent.click(screen.getByTestId("stored-wine-select-d-1"));
  await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/d-1/remedies"));
}

function postsTo(api: AdminApiClient, path: string) {
  return vi.mocked(api.post).mock.calls.filter(([called]) => called === path);
}

describe("StoredWinePage 存酒争议（第十八批 AP 段 254-R6）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("存酒中：争议原因为空不发请求并提示；填写后 POST …/dispute 只带 idempotency_key 与 reason，两次登记幂等键不同", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    let now = 1_000;
    vi.spyOn(Date, "now").mockImplementation(() => (now += 1));
    const api = makeApi();
    renderPage(api, SUPERVISOR);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-table")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("stored-wine-dispute-form")).not.toBeNull());

    fireEvent.click(screen.getByTestId("stored-wine-dispute"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-dispute-error").textContent).toContain("登记争议须填写争议原因"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/dispute")).toHaveLength(0);

    fireEvent.change(screen.getByTestId("stored-wine-dispute-reason"), { target: { value: "客人质疑剩余量" } });
    fireEvent.click(screen.getByTestId("stored-wine-dispute"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/dispute")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/dispute")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-dispute-w-1-/),
      reason: "客人质疑剩余量",
    });

    await waitFor(() => expect((screen.getByTestId("stored-wine-dispute") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.change(screen.getByTestId("stored-wine-dispute-reason"), { target: { value: "客人称瓶底标签被改" } });
    fireEvent.click(screen.getByTestId("stored-wine-dispute"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/w-1/dispute")).toHaveLength(2));
    const [first, second] = postsTo(api, "/api/v1/inventory/saved-bottles/w-1/dispute").map(([, body]) => (body as { idempotency_key: string }).idempotency_key);
    expect(second).not.toBe(first);
  });

  it("争议中且持门店主管码：出现「争议不成立，恢复存酒」，点后 POST …/dispute-dismissal 只带 idempotency_key；不出登记争议块", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api, SUPERVISOR);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-table")).not.toBeNull());
    await selectDisputed(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-dispute-dismiss")).not.toBeNull());
    expect(screen.queryByTestId("stored-wine-dispute-form")).toBeNull();
    expect((screen.getByTestId("stored-wine-cancel") as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(screen.getByTestId("stored-wine-dispute-dismiss"));
    await waitFor(() => expect(postsTo(api, "/api/v1/inventory/saved-bottles/d-1/dispute-dismissal")).toHaveLength(1));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/d-1/dispute-dismissal")[0]?.[1]).toEqual({
      idempotency_key: expect.stringMatching(/^admin-saved-bottle-dispute-dismissal-d-1-/),
    });
  });

  it("争议中但无门店主管码：恢复与作废按钮都不可点", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api, ["bar.saved_bottle.manage"]);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-table")).not.toBeNull());
    await selectDisputed(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-dispute-dismiss")).not.toBeNull());
    expect((screen.getByTestId("stored-wine-dispute-dismiss") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("stored-wine-cancel") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("stored-wine-dispute-dismiss"));
    expect(postsTo(api, "/api/v1/inventory/saved-bottles/d-1/dispute-dismissal")).toHaveLength(0);
  });
});
