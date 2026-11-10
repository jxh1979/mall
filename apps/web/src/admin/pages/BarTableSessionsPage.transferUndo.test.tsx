// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { formatActionResult } from "../components/ActionResultView";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

// 第十八批 AB 段第 190 条 190-R3（BSM:1079「撤销转台……按转台前快照恢复原台位、邀请关系、成员归属、可邀请人数、最低消费额和消费进度，并完整留痕」
// 「每次拆单、合单、转台、并台、拆台和撤销转台均须留痕。记录内容包括操作类型、原因、操作人、时间……」）：
// 卡座与订台页「撤销最近一次转台」带「转/合/拆原因」与当前场次版本发 POST …/transfer-undo。
describe("撤销最近一次转台", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("点「撤销最近一次转台」带原因与场次版本发撤销请求", async () => {
    const envelope = (data: unknown) => new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
    const list = (items: unknown[]) => envelope({ items, page: 1, page_size: 50, total: items.length });
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        return Promise.resolve(envelope({ id: "adjustment-1", session_id: "session-1", adjustment_type: "transfer_undo" }));
      }
      if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
      if (url.includes("/bar/table-sessions")) {
        return Promise.resolve(list([{ id: "session-1", table_id: "new-table", bar_id: "bar-1", status: "open", session_version: 4 }]));
      }
      return Promise.resolve(list([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "店员", accountType: "platform_admin" },
      setActionResult, setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("bar-session-select-session-1")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "转错台，回原台" } });
    await waitFor(() => expect(screen.queryByTestId("bar-transfer-undo-submit")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-transfer-undo-submit")!);
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("撤销最近一次转台"));
    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([, init]) => (init?.method ?? "GET").toUpperCase() === "POST");
      expect(String(call?.[0])).toContain("/api/v1/bar/table-sessions/session-1/transfer-undo");
      expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ reason: "转错台，回原台", session_version: 4 });
    }, { timeout: 5000 });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "bar_table_session_transfer_undo", status: "transfer_undone", sessionId: "session-1" })));
    // 第十八批 AB 段复核意见（第 190 条；PGM:287「界面全中文」）：结果区「调整类型」显示「撤销转台」，不出现 transfer_undo。
    const text = formatActionResult(setActionResult.mock.calls.at(-1)![0]);
    expect(text).toContain("调整类型：撤销转台");
    expect(text).not.toContain("transfer_undo");
  });
});
