import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AdminDataState } from "./AdminDataState";

describe("AdminDataState", () => {
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
});
