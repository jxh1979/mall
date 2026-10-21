// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// HP-P01 R7（BSM L1437「商户月租车位按合同约定、按月出账，账单类型为「车位月租」」）：草稿租金条款可录车位月租（元/月→分）。
const DRAFT = { id: "contract-park1111", merchant_id: "m-1", lifecycle_status: "drafting", effective_date: "2027-03-01",
  expiry_date: "2028-02-29", commission_mode: "fixed_ratio", project_id: "proj-1",
  rent_terms: { rentMode: "monthly_fixed", rentBillingMode: "fixed_only", baseRentCent: 100000 } };

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function props(): AdminPageProps {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/api/v1/contracts")) return Promise.resolve(envelope([DRAFT]));
    return Promise.resolve(envelope([]));
  }));
  const perms: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  return { api: new AdminApi("t"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(),
    setGlobalError: vi.fn(), effectivePermissions: perms, navigateTo: vi.fn() };
}

function postBody(path: string): Record<string, unknown> | null {
  const calls = (globalThis.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls;
  const call = calls.find((c) => String(c[0]).includes(path) && (c[1] as { method?: string })?.method === "POST");
  return call ? (JSON.parse(String((call[1] as { body: string }).body)) as Record<string, unknown>) : null;
}

describe("ContractWorkbenchPage 车位月租", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("draftCarriesParkingMonthlyCent：草稿编辑填车位月租 → rent_terms.parking_monthly_cent 按分提交", async () => {
    render(<ContractWorkbenchPage {...props()} />);
    await waitFor(() => expect(screen.queryByTestId("contract-contract-park1111")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-actions-contract-park1111"));
    await waitFor(() => expect(screen.queryByTestId("contract-edit-terms-contract-park1111")).not.toBeNull());
    fireEvent.click(screen.getByTestId("contract-edit-terms-contract-park1111"));
    await waitFor(() => expect(screen.queryByLabelText("编辑-车位月租（元/月）")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("编辑-车位月租（元/月）"), { target: { value: "300" } });
    fireEvent.click(screen.getByTestId("contract-edit-terms-save"));
    await waitFor(() => expect(postBody("/contracts/contract-park1111/rent-terms")).not.toBeNull());
    expect(postBody("/contracts/contract-park1111/rent-terms")!.rent_terms).toMatchObject({ base_rent_cent: 100000,
      parking_monthly_cent: 30000 });
  });
});
