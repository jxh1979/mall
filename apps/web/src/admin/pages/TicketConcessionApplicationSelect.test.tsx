// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ConcessionApplicationSelect } from "./TicketConcessionApplicationSelect";
import type { AdminApiClient } from "../types";

// 第十八批 BV 段 264-R4（第十八批裁决 78「运营配票种时只能从已确认的申请里选商户让利型」）。直接 mock api.get（绕过 snake→camel），按页面读取的驼峰键给。
const APPLICATIONS = [
  { id: "tca-own", merchantId: "mch-1", entitlementContent: "赠送小食一份", confirmedAt: "2026-11-20T02:00:00Z", bindingId: null },
  { id: "tca-other", merchantId: "mch-2", entitlementContent: "别家赠饮一杯", confirmedAt: "2026-11-20T02:00:00Z", bindingId: null },
  { id: "tca-used", merchantId: "mch-1", entitlementContent: "已用的赠饮", confirmedAt: "2026-11-19T02:00:00Z", bindingId: "ticket_link_x" },
];

function makeApi(rows: unknown[]): AdminApiClient {
  return { get: vi.fn(async () => ({ items: rows }) as unknown) } as unknown as AdminApiClient;
}

describe("ConcessionApplicationSelect 让利申请下拉", () => {
  it("只列该商户为本票确认、尚未用于其他权益的申请", async () => {
    const api = makeApi(APPLICATIONS);
    render(<ConcessionApplicationSelect api={api} ticketTypeId="tt-1" merchantId="mch-1" value="" onChange={vi.fn()} testId="sel" />);
    await waitFor(() => expect(screen.queryByTestId("sel")?.querySelectorAll("option").length ?? 0).toBe(2));
    expect(api.get).toHaveBeenCalledWith("/api/v1/ticketing/ticket-types/tt-1/concession-applications");
    const text = screen.getByTestId("sel").textContent ?? "";
    expect(text).toContain("赠送小食一份");
    expect(text).toContain("确认");
    expect(text).not.toContain("别家赠饮一杯");
    expect(text).not.toContain("已用的赠饮");
  });

  it("所选是别家的申请时清空", async () => {
    const onChange = vi.fn();
    render(<ConcessionApplicationSelect api={makeApi(APPLICATIONS)} ticketTypeId="tt-1" merchantId="mch-1" value="tca-other" onChange={onChange} testId="sel" />);
    await waitFor(() => expect(onChange).toHaveBeenCalledWith(""));
  });

  it("没有可选申请时提示须由商户在商户专区提交", async () => {
    render(<ConcessionApplicationSelect api={makeApi([])} ticketTypeId="tt-1" merchantId="mch-1" value="" onChange={vi.fn()} testId="sel" />);
    await waitFor(() => expect(screen.queryByTestId("sel-empty")?.textContent ?? "").toContain("商户专区「让利申请」"));
    expect(screen.queryByTestId("sel")).toBeNull();
  });
});
