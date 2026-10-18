// 文件版本：V0.01
import { render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

afterEach(() => {
  vi.unstubAllGlobals();
  window.localStorage.clear();
  window.history.replaceState({}, "", "/");
});

function open(grants: string[], denies: string[] = []) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    const data = url.endsWith("/access/effective-permissions") ? { grants, denies, wildcard: false }
      : url.endsWith("/purchase-workbench-options") ? { projects: [], entities: [], suppliers: [], inventory_items: [], warehouses: [], contracts: [] }
      : { items: [], page: 1, size: 20, total: 0 };
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  window.localStorage.setItem("admin.session.v1", JSON.stringify({
    user: { id: "buyer", username: "采购经办", accountType: "staff" }, sessionId: "purchase-session"
  }));
  window.history.replaceState({}, "", "/admin/inventory/purchases");
  render(<AdminApp />);
}

it.each([["inventory.purchase_request.view"], ["inventory.supply_contract.manage"]])("持%s可进入采购页，各区仍独立校权", async (permission) => {
  open([permission]);
  expect(await screen.findByTestId("purchase-workbench-page")).toBeTruthy();
});

it("没有任一采购入口权限时拒绝直链", async () => {
  open(["inventory.stock.view"]);
  expect(await screen.findByTestId("admin-permission-denied")).toBeTruthy();
  expect(screen.queryByTestId("purchase-workbench-page")).toBeNull();
});

it("唯一的合同管理权限被显式拒绝时不能进入", async () => {
  open(["inventory.supply_contract.manage"], ["inventory.supply_contract.manage"]);
  expect(await screen.findByTestId("admin-permission-denied")).toBeTruthy();
  expect(screen.queryByTestId("purchase-workbench-page")).toBeNull();
});
