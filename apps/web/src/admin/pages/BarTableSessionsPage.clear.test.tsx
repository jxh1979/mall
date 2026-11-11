// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十批 TK-27-R2（BSM L1058「关台成功后台位自动进入待清台；由负责该台位的服务员完成清理后在系统登记恢复空闲」
// 「待清台超过后台配置时限……未恢复空闲的，系统提醒当班主管」）：待清台行出现「登记恢复空闲」并 POST /tables/{id}/clear，超时行高亮。
describe("卡座待清台登记恢复空闲", () => {
  afterEach(() => vi.restoreAllMocks());
  it("待清台行有登记恢复空闲按钮并提交清台；超时行标出超时", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn(async (url: string) => url === "/api/v1/bar/tables"
        ? [
          { id: "t1", tableCode: "A1", capacity: 4, status: "pending_clear", pendingClearSince: "2026-07-05T10:00:00Z", pendingClearOverdue: true },
          { id: "t2", tableCode: "A2", capacity: 4, status: "opened" },
        ]
        : []),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = { api, user: { id: "operator", username: "服务员", accountType: "staff" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-table-clear-t1")).toBeTruthy());
    expect(screen.queryByTestId("bar-table-clear-t2")).toBeNull();
    expect(screen.getByTestId("bar-table-row-t1").textContent).toContain("超时");

    fireEvent.click(screen.getByTestId("bar-table-clear-t1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/tables/t1/clear", expect.anything()));
    // 第 198 条 198-R3③：清台只登记恢复空闲，请求体为空（幂等键由通用请求层注入），不再带写死的 operator_note。
    expect(post.mock.calls[0]?.[1]).toEqual({});
  });
});
