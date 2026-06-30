import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { TraceLink } from "./TraceLink";

describe("TraceLink", () => {
  it("renders a clickable link and navigates with pageId + objectId on click", () => {
    const navigateTo = vi.fn();
    render(
      <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId="merchant-1">
        商户 merchant-1
      </TraceLink>,
    );
    fireEvent.click(screen.getByTestId("trace-link-ADM-GOV-002-merchant-1"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-GOV-002", "merchant-1");
  });

  it("degrades to plain text when navigateTo missing", () => {
    render(<TraceLink pageId="ADM-GOV-002" objectId="m1">纯文本</TraceLink>);
    expect(screen.queryByRole("button")).toBeNull();
    expect(screen.getByText("纯文本")).toBeTruthy();
  });

  it("degrades to plain text when objectId is null/empty", () => {
    render(<TraceLink navigateTo={vi.fn()} pageId="ADM-GOV-002" objectId={null}>纯文本</TraceLink>);
    expect(screen.queryByRole("button")).toBeNull();
  });
});
