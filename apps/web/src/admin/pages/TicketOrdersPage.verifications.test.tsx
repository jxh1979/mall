// 文件版本：V0.02
import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 P 段 134-R3（PGM:287 所有终端零英文；ENUM:43 核销记录状态「拒绝（规则不允许）」）：
// 核销表的状态与结果说明显示中文；员工录入的冲正原因原样显示。
const VERIFICATIONS = [
  { id: "v-rej", verificationNo: "V-REJ", status: "rejected", resultReason: "entitlement_expired" },
  { id: "v-fail", verificationNo: "V-FAIL", status: "failed", resultReason: "window_expired" },
  // 第十八批 Q 段 145-R1（ENUM:43「明确的设备或网络失败仍记「失败」」）：在线核销抛错的设备技术失败也落 failed 记录。
  { id: "v-dev", verificationNo: "V-DEV", status: "failed", resultReason: "verify_device_merchant_unresolved" },
  { id: "v-rev", verificationNo: "V-REV", status: "reversed", resultReason: "客人误扫，现场确认冲正" },
  // 第十八批 Q 段 148-R1（ENUM:55「未先核入场即核酒水一律拒绝」）：未先核这家酒吧入场即核赠饮落 rejected。
  { id: "v-bar", verificationNo: "V-BAR", status: "rejected", resultReason: "bar_entry_not_verified" },
];

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function renderPage() {
  const api = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("/ticketing/orders")) return Promise.resolve([{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }]);
      if (url.includes("/ticketing/verifications")) return Promise.resolve(VERIFICATIONS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "staff", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: perms(["verification.view"]),
  };
  render(<TicketOrdersPage {...props} />);
}

function cells(rowId: string) {
  return within(screen.getByTestId(`ticket-verification-row-${rowId}`)).getAllByRole("cell").map((cell) => cell.textContent ?? "");
}

afterEach(() => vi.restoreAllMocks());

describe("TicketOrdersPage 核销表中文显示（134-R3）", () => {
  it("拒绝行状态为「拒绝」、结果为中文；失败行结果为中文；冲正行原因原样；全表无英文机器码", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-v-rev")).not.toBeNull());

    const rejected = cells("v-rej");
    expect(rejected[1]).toBe("拒绝");
    expect(rejected[2]).toBe("该权益已过期");
    const failed = cells("v-fail");
    expect(failed[2]).not.toBe("window_expired");
    expect(failed[2]).toMatch(/[一-龥]/);
    expect(cells("v-rev")[2]).toBe("客人误扫，现场确认冲正");
    expect(screen.getByTestId("ticket-verifications-table").textContent ?? "").not.toMatch(/[a-z]+_[a-z_]+/);
  });

  it("145-R1：设备技术失败行状态为「已失败」、结果说明为中文；全表无英文机器码", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-v-dev")).not.toBeNull());

    const deviceFailed = cells("v-dev");
    // 状态格 failed 沿用 formatBusinessStatus 的现有中文（不改）。
    expect(deviceFailed[1]).toBe("已失败");
    expect(deviceFailed[2]).toBe("核销设备未绑定商户，无法判定本次核销发生在哪家门店");
    expect(screen.getByTestId("ticket-verifications-table").textContent ?? "").not.toMatch(/[a-z]+_[a-z_]+/);
  });

  it("148-R1：未先核这家酒吧入场即核赠饮的拒绝行，状态为「拒绝」、结果说明为中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("ticket-verification-row-v-bar")).not.toBeNull());

    const barEntry = cells("v-bar");
    expect(barEntry[1]).toBe("拒绝");
    expect(barEntry[2]).toBe("须先核销这家酒吧的入场权益，再核销酒水（赠饮）权益");
  });
});
