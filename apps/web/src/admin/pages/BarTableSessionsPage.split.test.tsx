// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十批 TK-27-R3（BSM L1079 拆台：拆回两侧 / 任选一台 / 一次全拆；「无法确认订单归属时，须先人工明确归属并留痕，未完成前不得拆台」）：
// 会话操作区的「拆台」按拆台方式与人工订单归属 POST /table-sessions/{id}/split。
function mount() {
  const post = vi.fn().mockResolvedValue({});
  const api = {
    get: vi.fn(async (url: string) => url === "/api/v1/bar/table-sessions"
      ? [{ id: "session-1", tableId: "table-a", barId: "bar-1", status: "open", sessionVersion: 2 }]
      : []),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = { api, user: { id: "operator", username: "主管", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } } };
  vi.spyOn(window, "confirm").mockReturnValue(true);
  render(<BarTableSessionsPage {...props} />);
  return { post, props };
}

describe("卡座会话拆台", () => {
  afterEach(() => vi.restoreAllMocks());
  it("按拆台方式与订单归属提交拆台请求", async () => {
    const { post, props } = mount();

    await waitFor(() => expect(screen.getByTestId("bar-split-mode")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "客人分开坐" } });
    fireEvent.change(screen.getByTestId("bar-split-mode"), { target: { value: "single" } });
    fireEvent.click(screen.getByTestId("bar-split-submit"));
    expect(screen.getByTestId("bar-checkin-error").textContent).toContain("拆出的原台会话");
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("bar-split-session"), { target: { value: "session-2" } });
    fireEvent.change(screen.getByTestId("bar-split-assignments"), { target: { value: "ord-9=session-2\n\nord-10 = session-1" } });
    fireEvent.click(screen.getByTestId("bar-split-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/session-1/split",
      expect.objectContaining({
        mode: "single", splitSessionId: "session-2", primarySessionVersion: 2,
        orderAssignments: [{ orderId: "ord-9", sessionId: "session-2" }, { orderId: "ord-10", sessionId: "session-1" }],
      })));
    // 第十八批 AF 段第 198 条 198-R5（BarTableSessionService 发起人取登录主体，不读 requested_by_user_id）：拆台请求体不再带写死的 requestedByUserId。
    expect(Object.keys(post.mock.calls[0]![1] as object)).not.toContain("requestedByUserId");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_table_session_split", status: "split" })));
  });

  // 第十八批 T 段第 167 条 167-R3b（BSM:1079「并台期间新发出的邀请，以及由这些邀请形成的已接受成员、已入场成员，在首次拆台前由授权人员
  // 逐一分配到当前合并台所包含的某一张原台并留痕」；ENUM:114 同句）：拆台表单的邀请归属、成员归属随拆台请求提交。
  it("按邀请归属与成员归属提交拆台请求，格式不对不发请求", async () => {
    const { post } = mount();

    await waitFor(() => expect(screen.queryByTestId("bar-split-member-assignments")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("bar-session-select-session-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "客人分开坐" } });
    fireEvent.change(screen.getByTestId("bar-split-invitation-assignments"), { target: { value: "rsv-1=session-2" } });
    fireEvent.change(screen.getByTestId("bar-split-member-assignments"), { target: { value: "invm-1=session-1\ninvm-2" } });
    fireEvent.click(screen.getByTestId("bar-split-submit"));
    expect(screen.getByTestId("bar-checkin-error").textContent).toContain("成员归属每行须为「成员ID=会话ID」");
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("bar-split-member-assignments"), { target: { value: "invm-1=session-1\n\ninvm-2 = session-2" } });
    fireEvent.click(screen.getByTestId("bar-split-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/session-1/split",
      expect.objectContaining({
        invitationAssignments: [{ reservationId: "rsv-1", sessionId: "session-2" }],
        memberAssignments: [{ memberId: "invm-1", sessionId: "session-1" }, { memberId: "invm-2", sessionId: "session-2" }],
      })));
  });

  // 第十八批 K 段第 98 条（BSM L1079「平台不提供续卡、续时类产品。」，裁决 XM-M725-O）：会话操作区不再有续卡入口。
  it("会话操作区没有续卡时长与续卡按钮", async () => {
    mount();
    await waitFor(() => expect(screen.getByTestId("bar-split-mode")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    expect(screen.queryByTestId("bar-renew-submit")).toBeNull();
    expect(screen.queryByTestId("bar-renew-minutes")).toBeNull();
    expect(document.body.textContent).not.toContain("续卡");
    expect(document.body.textContent).not.toContain("续时");
  });
});

// 第十八批 AF 段第 198 条 198-R5（BarTableSessionService.mergeTableSession 发起人取登录主体 currentActor，不读 requested_by_user_id）：
// 并台请求体不再带写死的 requestedByUserId。
describe("卡座会话并台", () => {
  afterEach(() => vi.restoreAllMocks());
  it("并台请求体不含 requestedByUserId", async () => {
    const { post } = mount();

    await waitFor(() => expect(screen.queryByTestId("bar-session-select-session-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-session-select-session-1"));
    fireEvent.change(screen.getByTestId("bar-session-reason"), { target: { value: "两桌客人合坐" } });
    fireEvent.change(screen.getByTestId("bar-merge-target-session"), { target: { value: "session-2" } });
    await waitFor(() => expect((screen.getByTestId("bar-merge-target-session") as HTMLInputElement).value).toBe("session-2"));
    fireEvent.click(screen.getByTestId("bar-merge-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/table-sessions/session-1/merge",
      expect.objectContaining({ mergedSessionId: "session-2", reason: "两桌客人合坐", primarySessionVersion: 2 })));
    expect(Object.keys(post.mock.calls[0]![1] as object)).not.toContain("requestedByUserId");
  });
});
