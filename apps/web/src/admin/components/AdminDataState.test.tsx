import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminApi } from "../client/adminClient";
import { useAdminResource } from "../hooks/useAdminResource";
import { AdminDataState } from "./AdminDataState";

// 第 193 条 193-R2：真实客户端读取失败 → useAdminResource → 本组件。
function ReadFailure() {
  const { loading, error } = useAdminResource(async () => {
    await new AdminApi().get("/api/v1/evidence/packages");
  }, { onError: () => undefined, fallbackMessage: "\u8bfb\u53d6\u5931\u8d25" });
  return <AdminDataState loading={loading} error={error}>ready</AdminDataState>;
}

describe("AdminDataState", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders loading, empty and children states", () => {
    const { rerender } = render(<AdminDataState loading error={null}>ready</AdminDataState>);
    expect(screen.getByText("\u52a0\u8f7d\u4e2d...")).not.toBeNull();
    expect(screen.getByTestId("admin-loading")).not.toBeNull();

    rerender(<AdminDataState loading={false} error={null} empty>ready</AdminDataState>);
    expect(screen.getByText("\u6682\u65e0\u6570\u636e")).not.toBeNull();
    expect(screen.getByTestId("admin-empty")).not.toBeNull();

    rerender(<AdminDataState loading={false} error={null}><span>ready content</span></AdminDataState>);
    expect(screen.getByText("ready content")).not.toBeNull();
  });

  it("renders retryable error state", () => {
    const onRetry = vi.fn();
    render(<AdminDataState loading={false} error="backend unavailable" onRetry={onRetry}>ready</AdminDataState>);

    expect(screen.getByText("\u8bfb\u53d6\u5931\u8d25")).not.toBeNull();
    expect(screen.getByTestId("admin-error")).not.toBeNull();
    expect(screen.getByText("backend unavailable")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "\u91cd\u8bd5" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("renders business message plus collapsed technical detail when error carries detail", () => {
    render(
      <AdminDataState loading={false} error={{ message: "\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5", detail: "\u72b6\u6001\u7801 500" }}>
        ready
      </AdminDataState>,
    );

    expect(screen.getByText("\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")).not.toBeNull();
    const detail = screen.getByTestId("admin-error-detail");
    expect(detail.textContent).toContain("\u6280\u672f\u8be6\u60c5");
    expect(detail.textContent).toContain("\u72b6\u6001\u7801 500");
    // \u9ed8\u8ba4\u6298\u53e0
    expect((detail as HTMLDetailsElement).open).toBe(false);
  });

  // 第 193 条 193-R2（PGM:287「可以不用中文的只有四样，没有第五样」）：技术详情只留状态码，接口路径、request_id、原始报文不上屏。
  it("193-R2 读取失败的技术详情只留状态码，接口路径、request_id、原始报文不上屏", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response('{"code":"SERVER_ERROR","message":"mock server error","request_id":"req-9"}', { status: 500 })));
    render(<ReadFailure />);
    await waitFor(() => expect(screen.getByTestId("admin-error-detail").textContent).toContain("500"));
    expect(screen.getByTestId("admin-error-detail").textContent).toBe("\u6280\u672f\u8be6\u60c5\u72b6\u6001\u7801 500");
    expect(screen.getByTestId("admin-error").textContent).not.toMatch(/[A-Za-z]/);
  });
});
