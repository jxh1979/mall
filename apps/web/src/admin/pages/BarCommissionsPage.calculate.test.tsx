// 文件版本：V0.01
// 第十八批 AE 段第 198 条 198-R3⑤（BSM:1070「销售提成一张账单的提成只计一次」）：计算提成的账单号取路径，
// 服务端不读 include_only_table_sales、recalculate；网页请求体为空，幂等键由通用请求层注入。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarCommissionsPage } from "./BarCommissionsPage";
import type { AdminApiClient, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: BarOrderRow[] = [
  { id: "ord-1", tableId: "T-1", salesStaffId: "staff_sales_1", totalAmount: 20000, status: "paid", createdAt: "2026-07-05T13:20:00Z", bills: [{ id: "bill-1", status: "settled", paidAt: "2026-07-05T14:00:00Z" }] },
];

describe("BarCommissionsPage 计算提成请求体", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/commissions");
  });

  it("198-R3⑤：计算提成调 /bills/{账单}/commissions/calculate，请求体不含 include_only_table_sales", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = {
      get: vi.fn().mockResolvedValue(ORDERS),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
    render(<BarCommissionsPage api={api} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect((screen.getByTestId("bar-commission-select-ord-1") as HTMLInputElement).checked).toBe(true));
    fireEvent.click(screen.getByTestId("bar-commission-calculate"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0]?.[0]).toBe("/api/v1/bar/bills/bill-1/commissions/calculate");
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("includeOnlyTableSales");
    expect(post.mock.calls[0]?.[1]).toEqual({});
  });
});
